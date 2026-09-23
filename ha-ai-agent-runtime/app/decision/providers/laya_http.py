"""按获批 Laya API Contract 调用内网推理网关。"""

import asyncio
import json
import ssl
from typing import Awaitable, Callable
from uuid import UUID

import httpx
from pydantic import ValidationError

from app.config import RuntimeConfigurationError, RuntimeSettings
from app.decision.models import DecisionRequest, DecisionResponse
from app.decision.providers.base import (
    DecisionProviderError,
    InvalidProviderResponseError,
    ProviderAuthenticationError,
    ProviderErrorCode,
    ProviderOverloadedError,
    ProviderTimeoutError,
    ProviderUnavailableError,
    ProviderVersionMismatchError,
)
from app.security.service_auth import create_laya_ssl_context

_GATEWAY_ERROR_CODES = {code.value: code for code in ProviderErrorCode}


class LayaHttpProvider:
    """使用单例 HTTPX 客户端访问固定 Laya 网关，绝不透出上游原始错误。"""

    def __init__(
        self,
        settings: RuntimeSettings,
        *,
        client: httpx.AsyncClient | None = None,
        sleep: Callable[[float], Awaitable[None]] = asyncio.sleep,
        ssl_context: ssl.SSLContext | None = None,
    ) -> None:
        self._base_url = settings.require_laya_provider()
        self._sleep = sleep
        self._owns_client = client is None
        if client is None:
            try:
                context = ssl_context or create_laya_ssl_context(settings)
            except (OSError, ssl.SSLError, ValueError, RuntimeConfigurationError):
                raise ProviderAuthenticationError() from None
            timeout = httpx.Timeout(
                timeout=settings.attempt_timeout_seconds,
                connect=settings.connect_timeout_seconds,
            )
            limits = httpx.Limits(max_connections=1, max_keepalive_connections=1)
            client = httpx.AsyncClient(
                verify=context,
                timeout=timeout,
                limits=limits,
                trust_env=False,
                follow_redirects=False,
            )
        self._client = client

    async def aclose(self) -> None:
        """释放本 Provider 自建的长生命周期客户端。"""
        if self._owns_client:
            await self._client.aclose()

    async def predict(self, request: DecisionRequest, request_id: UUID) -> DecisionResponse:
        """执行固定路由推理；至多一次重试且复用关联 ID 与请求体。"""
        payload = request.model_dump(mode="json")
        attempts = self._max_retries + 1
        last_transport_error = False

        for attempt in range(attempts):
            try:
                response = await asyncio.wait_for(
                    self._client.post(
                        f"{self._base_url}/v1/systemone",
                        json=payload,
                        headers={"X-Request-ID": str(request_id)},
                    ),
                    timeout=self._attempt_timeout_seconds,
                )
            except (httpx.TimeoutException, asyncio.TimeoutError):
                last_transport_error = True
                if attempt + 1 < attempts:
                    await self._backoff()
                    continue
                raise ProviderTimeoutError() from None
            except httpx.TransportError:
                last_transport_error = True
                if attempt + 1 < attempts:
                    await self._backoff()
                    continue
                raise ProviderUnavailableError() from None

            self._validate_request_id(response, request_id)
            if response.status_code == 503:
                if attempt + 1 < attempts:
                    await self._backoff()
                    continue
                self._raise_gateway_error(response, default=ProviderOverloadedError)
            if response.status_code in (401, 403):
                raise ProviderAuthenticationError()
            if response.status_code != 200:
                self._raise_gateway_error(response, default=InvalidProviderResponseError)
            return self._parse_success(response, request)

        # 循环必定在成功或显式错误处分支；此处只保护类型检查和未来调整。
        if last_transport_error:
            raise ProviderUnavailableError()
        raise InvalidProviderResponseError()

    async def _backoff(self) -> None:
        await self._sleep(self._retry_backoff_seconds)

    @property
    def _max_retries(self) -> int:
        # 此策略由 Contract 固定，不允许普通部署配置扩大重试量。
        return 1

    @property
    def _retry_backoff_seconds(self) -> float:
        return 0.2

    @property
    def _attempt_timeout_seconds(self) -> float:
        return 5.0

    @staticmethod
    def _validate_request_id(response: httpx.Response, request_id: UUID) -> None:
        if response.headers.get("X-Request-ID") != str(request_id):
            raise InvalidProviderResponseError()

    @classmethod
    def _raise_gateway_error(cls, response: httpx.Response, default: type[DecisionProviderError]) -> None:
        code = None
        try:
            if "application/json" in response.headers.get("content-type", "").lower():
                body = response.json()
                if isinstance(body, dict):
                    code = _GATEWAY_ERROR_CODES.get(body.get("code"))
        except (ValueError, TypeError):
            pass

        if code == ProviderErrorCode.INPUT_TOO_LONG:
            raise DecisionProviderError(code)
        if code == ProviderErrorCode.AUTHENTICATION_FAILED:
            raise ProviderAuthenticationError()
        if code == ProviderErrorCode.PROVIDER_OVERLOADED:
            raise ProviderOverloadedError()
        if code == ProviderErrorCode.MODEL_NOT_READY:
            raise ProviderVersionMismatchError()
        if code == ProviderErrorCode.PROVIDER_UNAVAILABLE:
            raise ProviderUnavailableError()
        if code == ProviderErrorCode.INTERNAL_PROVIDER_ERROR:
            raise DecisionProviderError(code)
        if code == ProviderErrorCode.INVALID_REQUEST:
            raise DecisionProviderError(code)
        raise default()

    @staticmethod
    def _parse_success(response: httpx.Response, request: DecisionRequest) -> DecisionResponse:
        if "application/json" not in response.headers.get("content-type", "").lower():
            raise InvalidProviderResponseError()
        try:
            result = DecisionResponse.model_validate(response.json())
            result.validate_against(request)
        except (ValueError, TypeError, ValidationError, json.JSONDecodeError):
            raise InvalidProviderResponseError() from None
        if result.model != "laya-rl-agent":
            raise ProviderVersionMismatchError()
        return result

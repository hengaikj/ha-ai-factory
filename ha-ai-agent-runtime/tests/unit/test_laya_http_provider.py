import asyncio
from uuid import UUID, uuid4

import httpx
import pytest

from app.config import RuntimeSettings
from app.decision.models import DecisionRequest
from app.decision.providers.base import (
    DecisionProviderError,
    ProviderAuthenticationError,
    ProviderErrorCode,
    ProviderOverloadedError,
    ProviderTimeoutError,
)
from app.decision.providers.laya_http import LayaHttpProvider


REQUEST_ID = UUID("b4f60e08-8d52-4d66-94d9-b31bcff4044b")


def decision_request() -> DecisionRequest:
    return DecisionRequest.model_validate(
        {
            "state": "脱敏后的任务摘要",
            "questions": {
                "scope_fit": {
                    "type": "choice",
                    "instructions": "判断变更是否在范围内",
                    "criteria": {"in_scope": "范围内", "out_scope": "范围外"},
                }
            },
        }
    )


def decision_response() -> dict:
    return {
        "model": "laya-rl-agent",
        "answers": {
            "scope_fit": {
                "type": "choice",
                "choice": "in_scope",
                "confidence": 0.92,
                "probabilities": {"in_scope": 0.92, "out_scope": 0.08},
                "action": {"act_probability": 0.01},
            }
        },
        "usage": {"input_tokens": 23, "output_tokens": 0},
        "routing": {"model": "multilingual", "repo": "approved-model"},
    }


def make_provider(handler, sleep=asyncio.sleep):
    client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    provider = LayaHttpProvider(
        RuntimeSettings(laya_base_url="https://laya-inference.internal"),
        client=client,
        sleep=sleep,
    )
    return provider, client


def test_success_sends_only_approved_fields_and_checks_request_id() -> None:
    observed = {}

    def handler(request: httpx.Request) -> httpx.Response:
        observed["method"] = request.method
        observed["url"] = str(request.url)
        observed["headers"] = request.headers
        observed["body"] = request.read()
        return httpx.Response(
            200,
            headers={"content-type": "application/json", "X-Request-ID": str(REQUEST_ID)},
            json=decision_response(),
        )

    async def run() -> None:
        provider, client = make_provider(handler)
        try:
            result = await provider.predict(decision_request(), REQUEST_ID)
        finally:
            await client.aclose()
        assert result.routing.model == "multilingual"
        assert observed["method"] == "POST"
        assert observed["url"] == "https://laya-inference.internal/v1/systemone"
        assert observed["headers"]["X-Request-ID"] == str(REQUEST_ID)
        body = observed["body"].decode("utf-8")
        assert '"model"' not in body
        assert "脱敏后的任务摘要" in body

    asyncio.run(run())


@pytest.mark.parametrize(
    ("status", "body", "expected"),
    [
        (401, {"code": "AUTHENTICATION_FAILED", "detail": "safe"}, ProviderAuthenticationError),
        (403, {"code": "AUTHENTICATION_FAILED", "detail": "safe"}, ProviderAuthenticationError),
        (422, {"code": "INPUT_TOO_LONG", "detail": "safe"}, DecisionProviderError),
    ],
)
def test_provider_maps_gateway_errors_without_leaking_response_text(status, body, expected) -> None:
    async def run() -> None:
        provider, client = make_provider(
            lambda request: httpx.Response(
                status,
                headers={"content-type": "application/json", "X-Request-ID": str(REQUEST_ID)},
                json=body,
            )
        )
        try:
            with pytest.raises(expected) as raised:
                await provider.predict(decision_request(), REQUEST_ID)
        finally:
            await client.aclose()

        if status == 422:
            assert raised.value.code == ProviderErrorCode.INPUT_TOO_LONG
        assert "safe" not in str(raised.value)

    asyncio.run(run())


def test_provider_retries_503_once_with_same_request_id_and_body() -> None:
    observed = []
    pauses = []

    def handler(request: httpx.Request) -> httpx.Response:
        observed.append((request.headers["X-Request-ID"], request.read()))
        if len(observed) == 1:
            return httpx.Response(
                503,
                headers={"X-Request-ID": str(REQUEST_ID)},
                json={"code": "PROVIDER_UNAVAILABLE", "detail": "safe"},
            )
        return httpx.Response(
            200,
            headers={"content-type": "application/json", "X-Request-ID": str(REQUEST_ID)},
            json=decision_response(),
        )

    async def fake_sleep(seconds: float) -> None:
        pauses.append(seconds)

    async def run() -> None:
        provider, client = make_provider(handler, sleep=fake_sleep)
        try:
            await provider.predict(decision_request(), REQUEST_ID)
        finally:
            await client.aclose()

    asyncio.run(run())
    assert len(observed) == 2
    assert observed[0] == observed[1]
    assert pauses == [0.2]


def test_provider_fails_closed_after_retryable_overload() -> None:
    calls = 0

    def handler(request: httpx.Request) -> httpx.Response:
        nonlocal calls
        calls += 1
        return httpx.Response(
            503,
            headers={"X-Request-ID": str(REQUEST_ID)},
            json={"code": "PROVIDER_OVERLOADED", "detail": "safe"},
        )

    async def fake_sleep(seconds: float) -> None:
        return None

    async def run() -> None:
        provider, client = make_provider(handler, sleep=fake_sleep)
        try:
            with pytest.raises(ProviderOverloadedError):
                await provider.predict(decision_request(), REQUEST_ID)
        finally:
            await client.aclose()

    asyncio.run(run())
    assert calls == 2


def test_provider_rejects_missing_or_mismatched_request_id() -> None:
    async def run() -> None:
        for headers in (
            {"content-type": "application/json"},
            {"content-type": "application/json", "X-Request-ID": str(uuid4())},
        ):
            provider, client = make_provider(
                lambda request, h=headers: httpx.Response(200, headers=h, json=decision_response())
            )
            try:
                with pytest.raises(DecisionProviderError):
                    await provider.predict(decision_request(), REQUEST_ID)
            finally:
                await client.aclose()

    asyncio.run(run())


def test_provider_rejects_invalid_json_and_invalid_typed_answers() -> None:
    async def run() -> None:
        malformed = httpx.Response(
            200,
            headers={"content-type": "application/json", "X-Request-ID": str(REQUEST_ID)},
            content=b"not-json",
        )
        invalid = decision_response()
        invalid["answers"]["scope_fit"]["choice"] = "unknown"
        responses = [malformed, httpx.Response(
            200,
            headers={"content-type": "application/json", "X-Request-ID": str(REQUEST_ID)},
            json=invalid,
        )]
        for response in responses:
            provider, client = make_provider(lambda request, r=response: r)
            try:
                with pytest.raises(DecisionProviderError):
                    await provider.predict(decision_request(), REQUEST_ID)
            finally:
                await client.aclose()

    asyncio.run(run())


def test_provider_maps_transport_timeout_without_raw_exception() -> None:
    async def run() -> None:
        async def fake_sleep(seconds: float) -> None:
            return None

        provider, client = make_provider(
            lambda request: httpx.Response(200), sleep=fake_sleep
        )
        await client.aclose()

        class TimeoutClient:
            async def post(self, *args, **kwargs):
                raise httpx.ReadTimeout("secret response material")

        provider._client = TimeoutClient()
        with pytest.raises(ProviderTimeoutError) as raised:
            await provider.predict(decision_request(), REQUEST_ID)
        assert "secret response material" not in str(raised.value)

    asyncio.run(run())


def test_provider_fails_closed_when_mtls_configuration_is_missing() -> None:
    with pytest.raises(ProviderAuthenticationError):
        LayaHttpProvider(RuntimeSettings(laya_base_url="https://laya-inference.internal"))

"""读取 Runtime 固定策略及受控 provider 部署配置。"""

from dataclasses import dataclass
import os
from typing import cast
from urllib.parse import urlsplit


class RuntimeConfigurationError(RuntimeError):
    """服务配置缺失或违反获批策略时的安全错误。"""


@dataclass(frozen=True)
class RuntimeSettings:
    """进程级设置；模型策略不从请求载荷读取。"""

    laya_base_url: str | None
    laya_ca_cert: str | None = None
    runtime_client_cert: str | None = None
    runtime_client_key: str | None = None
    checkpoint: str = "multilingual"
    connect_timeout_seconds: float = 2.0
    attempt_timeout_seconds: float = 5.0
    max_retries: int = 1
    retry_backoff_milliseconds: int = 200
    max_in_flight: int = 1
    waiting_queue: int = 0

    @classmethod
    def from_environment(cls) -> "RuntimeSettings":
        """从部署环境加载固定的模型入口和策略参数。"""
        checkpoint = os.getenv("HA_LAYA_CHECKPOINT", "multilingual")
        if checkpoint != "multilingual":
            raise RuntimeConfigurationError("运行配置不符合已批准的模型策略")

        base_url = os.getenv("HA_LAYA_BASE_URL")
        if base_url is not None:
            base_url = base_url.strip().rstrip("/")
            if not base_url:
                base_url = None

        return cls(
            laya_base_url=base_url,
            laya_ca_cert=os.getenv("HA_LAYA_CA_CERT"),
            runtime_client_cert=os.getenv("HA_RUNTIME_CLIENT_CERT"),
            runtime_client_key=os.getenv("HA_RUNTIME_CLIENT_KEY"),
        )

    def require_laya_provider(self) -> str:
        """在触达模型前拒绝未配置的 provider。"""
        if not self.laya_base_url:
            raise RuntimeConfigurationError("Laya 推理服务未配置")
        parsed = urlsplit(self.laya_base_url)
        if (
            parsed.scheme != "https"
            or not parsed.hostname
            or parsed.username is not None
            or parsed.password is not None
            or parsed.path not in ("", "/")
            or parsed.query
            or parsed.fragment
        ):
            raise RuntimeConfigurationError("Laya 推理服务地址不符合内网 HTTPS 策略")
        return self.laya_base_url

    def require_laya_mtls(self) -> tuple[str, str, str]:
        """拒绝未配置的工作负载 mTLS 证书路径。"""
        values = (self.laya_ca_cert, self.runtime_client_cert, self.runtime_client_key)
        if not all(value and value.strip() for value in values):
            raise RuntimeConfigurationError("Laya 工作负载 mTLS 配置不完整")
        return cast(tuple[str, str, str], values)

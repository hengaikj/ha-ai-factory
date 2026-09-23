"""读取 Runtime 固定策略及受控 provider 部署配置。"""

from dataclasses import dataclass
import os


class RuntimeConfigurationError(RuntimeError):
    """服务配置缺失或违反获批策略时的安全错误。"""


@dataclass(frozen=True)
class RuntimeSettings:
    """进程级设置；模型策略不从请求载荷读取。"""

    laya_base_url: str | None
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

        return cls(laya_base_url=base_url)

    def require_laya_provider(self) -> str:
        """在触达模型前拒绝未配置的 provider。"""
        if not self.laya_base_url:
            raise RuntimeConfigurationError("Laya 推理服务未配置")
        return self.laya_base_url

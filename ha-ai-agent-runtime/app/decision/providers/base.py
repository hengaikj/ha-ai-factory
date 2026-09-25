"""决策推理 Provider 抽象接口。"""

from enum import Enum
from typing import Protocol
from uuid import UUID

from app.decision.models import DecisionRequest, DecisionResponse


class ProviderErrorCode(str, Enum):
    """Laya Contract 定义的稳定错误类别。"""

    INVALID_REQUEST = "INVALID_REQUEST"
    INPUT_TOO_LONG = "INPUT_TOO_LONG"
    AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED"
    PROVIDER_UNAVAILABLE = "PROVIDER_UNAVAILABLE"
    PROVIDER_OVERLOADED = "PROVIDER_OVERLOADED"
    MODEL_NOT_READY = "MODEL_NOT_READY"
    INTERNAL_PROVIDER_ERROR = "INTERNAL_PROVIDER_ERROR"


_SAFE_ERROR_DETAILS = {
    ProviderErrorCode.INVALID_REQUEST: "推理请求校验失败，请检查已批准的问题模板。",
    ProviderErrorCode.INPUT_TOO_LONG: "输入超过模型允许长度，请缩短任务摘要后重试。",
    ProviderErrorCode.AUTHENTICATION_FAILED: "推理服务身份校验失败，请联系管理员。",
    ProviderErrorCode.PROVIDER_UNAVAILABLE: "推理服务暂不可用，请稍后重试或转人工处理。",
    ProviderErrorCode.PROVIDER_OVERLOADED: "推理服务当前繁忙，请稍后重试。",
    ProviderErrorCode.MODEL_NOT_READY: "获批模型尚未就绪，请联系管理员。",
    ProviderErrorCode.INTERNAL_PROVIDER_ERROR: "推理服务处理失败，请转人工处理。",
}


class DecisionProviderError(RuntimeError):
    """仅通过稳定错误码和静态提示表达失败，不携带上游异常内容。"""

    code: ProviderErrorCode

    def __init__(self, code: ProviderErrorCode):
        self.code = code
        self.safe_detail = _SAFE_ERROR_DETAILS[code]
        super().__init__(self.safe_detail)


class ProviderAuthenticationError(DecisionProviderError):
    """表示服务间身份或 mTLS 校验失败。"""

    def __init__(self):
        super().__init__(ProviderErrorCode.AUTHENTICATION_FAILED)


class ProviderUnavailableError(DecisionProviderError):
    """表示模型服务不可达或请求超时。"""

    def __init__(self):
        super().__init__(ProviderErrorCode.PROVIDER_UNAVAILABLE)


class ProviderTimeoutError(ProviderUnavailableError):
    """表示单次推理调用超过批准时限。"""


class ProviderOverloadedError(DecisionProviderError):
    """表示网关零等待并发上限已满。"""

    def __init__(self):
        super().__init__(ProviderErrorCode.PROVIDER_OVERLOADED)


class InvalidProviderResponseError(DecisionProviderError):
    """表示上游响应无法通过已批准 schema 校验。"""

    def __init__(self):
        super().__init__(ProviderErrorCode.INTERNAL_PROVIDER_ERROR)


class ProviderVersionMismatchError(DecisionProviderError):
    """表示上游模型或部署版本不符合锁定 allowlist。"""

    def __init__(self):
        super().__init__(ProviderErrorCode.MODEL_NOT_READY)


class DecisionModelProvider(Protocol):
    """向受控决策模型发送已批准请求的异步接口。"""

    async def predict(self, request: DecisionRequest, request_id: UUID) -> DecisionResponse:
        """执行类型化推理并返回尚待业务人工复核的结果。"""
        ...

from app.decision.providers.base import (
    InvalidProviderResponseError,
    ProviderAuthenticationError,
    ProviderErrorCode,
    ProviderOverloadedError,
    ProviderTimeoutError,
    ProviderVersionMismatchError,
)


def test_provider_errors_use_stable_safe_details_only() -> None:
    errors = [
        ProviderAuthenticationError(),
        ProviderTimeoutError(),
        ProviderOverloadedError(),
        InvalidProviderResponseError(),
        ProviderVersionMismatchError(),
    ]

    assert [error.code for error in errors] == [
        ProviderErrorCode.AUTHENTICATION_FAILED,
        ProviderErrorCode.PROVIDER_UNAVAILABLE,
        ProviderErrorCode.PROVIDER_OVERLOADED,
        ProviderErrorCode.INTERNAL_PROVIDER_ERROR,
        ProviderErrorCode.MODEL_NOT_READY,
    ]
    assert all("secret" not in str(error).lower() for error in errors)
    assert all(error.safe_detail == str(error) for error in errors)

import pytest

from app.config import RuntimeConfigurationError, RuntimeSettings


def test_runtime_fails_closed_when_laya_provider_is_missing(monkeypatch) -> None:
    monkeypatch.delenv("HA_LAYA_BASE_URL", raising=False)
    settings = RuntimeSettings.from_environment()

    with pytest.raises(RuntimeConfigurationError, match="未配置"):
        settings.require_laya_provider()


def test_runtime_rejects_checkpoint_outside_approved_allowlist(monkeypatch) -> None:
    monkeypatch.setenv("HA_LAYA_CHECKPOINT", "english")

    with pytest.raises(RuntimeConfigurationError, match="模型策略"):
        RuntimeSettings.from_environment()


def test_runtime_uses_only_approved_checkpoint_and_normalizes_base_url(monkeypatch) -> None:
    monkeypatch.setenv("HA_LAYA_CHECKPOINT", "multilingual")
    monkeypatch.setenv("HA_LAYA_BASE_URL", "https://laya-inference.internal/")

    settings = RuntimeSettings.from_environment()

    assert settings.checkpoint == "multilingual"
    assert settings.require_laya_provider() == "https://laya-inference.internal"
    assert settings.max_retries == 1
    assert settings.waiting_queue == 0


@pytest.mark.parametrize(
    "base_url",
    [
        "http://laya-inference.internal",
        "https://user:secret@laya-inference.internal",
        "https://laya-inference.internal/custom-path",
        "https://laya-inference.internal?route=other",
    ],
)
def test_runtime_rejects_unsafe_laya_base_url(base_url: str) -> None:
    with pytest.raises(RuntimeConfigurationError):
        RuntimeSettings(laya_base_url=base_url).require_laya_provider()

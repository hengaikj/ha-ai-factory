"""治理证据清单的最小完整性校验，不执行清单中的命令。"""
from __future__ import annotations
from typing import Any

def verify_manifest(manifest: dict[str, Any]) -> dict[str, Any]:
    """检查执行状态与退出码、来源元数据的一致性。"""
    errors: list[str] = []
    if manifest.get("exit_code") != 0:
        errors.append("EXIT_CODE_NONZERO")
    if manifest.get("execution_status") == "PASSED" and manifest.get("exit_code") != 0:
        errors.append("STATUS_EXIT_CONFLICT")
    for key in ("source_sha", "argv", "cwd", "started_at", "finished_at", "provenance"):
        if not manifest.get(key):
            errors.append(f"MISSING_{key.upper()}")
    if not isinstance(manifest.get("raw_artifacts", []), list) or not isinstance(manifest.get("redacted_artifacts", []), list):
        errors.append("ARTIFACTS_NOT_LIST")
    return {"integrity": "PASS" if not errors else "FAIL", "errors": sorted(set(errors))}

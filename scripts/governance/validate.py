"""治理工作项的结构和路径校验；不执行工作项命令。"""
from __future__ import annotations
from pathlib import PurePosixPath
from typing import Any


def validate_work_item(item: dict[str, Any]) -> list[str]:
    """返回可审计错误代码；任何错误都禁止将记录视为可执行。"""
    required = ("id", "repository", "task_kind", "requested_action", "baseline_sha", "allowed_paths", "forbidden_paths", "required_scopes", "acceptance_ids", "status")
    errors = [f"MISSING_{key.upper()}" for key in required if key not in item]
    if item.get("example_only") is True and item.get("status") == "APPROVED":
        errors.append("EXAMPLE_CANNOT_APPROVE")
    baseline = item.get("baseline_sha")
    if item.get("status") not in {"PROPOSED", "DRAFT"} and not baseline:
        errors.append("BASELINE_REQUIRED")
    for field in ("allowed_paths", "forbidden_paths"):
        for raw in item.get(field, []):
            path = PurePosixPath(str(raw))
            if path.is_absolute() or ".." in path.parts or "\x00" in str(raw):
                errors.append("PATH_OUTSIDE_SCOPE")
    allowed = [str(x) for x in item.get("allowed_paths", [])]
    forbidden = [str(x) for x in item.get("forbidden_paths", [])]
    if set(allowed) & set(forbidden):
        errors.append("PATH_ALLOW_DENY_CONFLICT")
    return sorted(set(errors))

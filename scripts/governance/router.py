"""HA AI Factory 仓库治理路由器：只提供确定性建议，不授予执行权限。"""
from __future__ import annotations
import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[2]
CONFIG = ROOT / ".agent" / "governance" / "routing.json"

def route_task(task: dict[str, Any]) -> dict[str, Any]:
    """根据已结构化的任务类型路由；缺失或未知类型默认人工分诊。"""
    config = json.loads(CONFIG.read_text(encoding="utf-8"))
    kind = task.get("task_kind")
    entry = config.get("routes", {}).get(kind, config["default"])
    result = {"route": entry["route"], "required_scopes": entry.get("required_scopes", []), "authorization_effect": "none"}
    if entry.get("decision"):
        result["decision"] = entry["decision"]
    if not kind:
        result["reason_code"] = "TASK_KIND_MISSING"
    elif kind not in config.get("routes", {}):
        result["reason_code"] = "TASK_KIND_UNKNOWN"
    return result

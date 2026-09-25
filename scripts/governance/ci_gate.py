"""本地 CI 演练入口；根据 enforcement 配置报告 advisory/strict 结果。"""
from __future__ import annotations
import json
from pathlib import Path
from .policy import evaluate_gate

ROOT = Path(__file__).resolve().parents[2]

def run_gate(payload: dict) -> dict:
    """执行单个门禁演练并返回可审计结果，不执行外部动作。"""
    enforcement = json.loads((ROOT / ".agent/governance/enforcement.json").read_text(encoding="utf-8"))
    context = dict(payload.get("context", {}))
    # 本地演练不是可信 CI；不得通过输入字段伪造受信执行面。
    context["execution_surface"] = "local_cli"
    result = evaluate_gate(payload.get("work", {}), context, payload["action"])
    result["enforcement_mode"] = enforcement["mode"]
    result["enforcement_effective"] = enforcement["mode"] == "strict" and result["decision"] == "ALLOW"
    return result

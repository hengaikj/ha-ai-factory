"""动作级治理门禁；可信批准上下文必须由外部受信边界提供。"""
from __future__ import annotations
import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[2]

def evaluate_gate(work: dict[str, Any], context: dict[str, Any], action: str) -> dict[str, Any]:
    """以失败关闭方式计算门禁，不接受工作项自带的批准字段。"""
    policy = json.loads((ROOT / ".agent/governance/policy.json").read_text(encoding="utf-8"))
    enforcement = json.loads((ROOT / ".agent/governance/enforcement.json").read_text(encoding="utf-8"))
    result = {key: context.get(key, "NOT_RUN") for key in ("content_quality", "contract_coverage", "implementation_quality")}
    result.update({"human_approval": context.get("human_approval", "PENDING"), "formal_readiness": "BLOCKED", "decision": "BLOCK", "reason_codes": []})
    if action in policy["blocked_actions"]:
        result["reason_codes"].append("ACTION_OUTSIDE_PROPOSED_SCOPE")
        return result
    if work.get("example_only") is True:
        result["reason_codes"].append("EXAMPLE_ONLY")
    if not context.get("baseline_verified"):
        result["reason_codes"].append("BASELINE_NOT_VERIFIED")
    if not context.get("scope_verified"):
        result["reason_codes"].append("SCOPE_NOT_VERIFIED")
    if context.get("human_approval") != "APPROVED":
        result["reason_codes"].append("HUMAN_APPROVAL_REQUIRED")
    if context.get("execution_surface") != "trusted_ci":
        result["reason_codes"].append("TRUSTED_SURFACE_REQUIRED")
    if context.get("approval_source") is None or context.get("policy_ref") is None:
        result["reason_codes"].append("APPROVAL_PROVENANCE_REQUIRED")
    if policy.get("status") != "ACCEPTED":
        result["reason_codes"].append("POLICY_NOT_ACCEPTED")
    if any(result[k] != "PASS" for k in policy["required_quality"]):
        result["reason_codes"].append("QUALITY_NOT_PASS")
    # 候选 work/context 中的 approved 字段永远不作为批准来源。
    if not result["reason_codes"]:
        result["formal_readiness"] = "READY"
        result["decision"] = "ALLOW"
    elif enforcement.get("mode") == "advisory" and action not in policy["blocked_actions"]:
        result["formal_readiness"] = "REVIEW_REQUIRED"
        result["decision"] = "REVIEW_REQUIRED"
    elif enforcement.get("mode") == "single_account" and action not in policy["blocked_actions"]:
        result["formal_readiness"] = "SINGLE_ACCOUNT_REVIEW"
        result["decision"] = "SELF_REVIEW_REQUIRED"
    return result

"""reverse-skill 治理 MVP 的统一离线 CLI；只读输入，不执行工作项。"""
from __future__ import annotations
import argparse, json, sys
from .router import route_task
from .validate import validate_work_item
from .policy import evaluate_gate
from .evidence import verify_manifest

def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="HA AI Factory governance MVP")
    sub = parser.add_subparsers(dest="command", required=True)
    for name in ("route", "validate-work-item", "evaluate", "verify-evidence"):
        p = sub.add_parser(name); p.add_argument("--input", required=True)
    args = parser.parse_args(argv)
    with open(args.input, encoding="utf-8") as input_file:
        data = json.load(input_file)
    if args.command == "route": result = route_task(data)
    elif args.command == "validate-work-item": result = {"valid": not validate_work_item(data), "errors": validate_work_item(data)}
    elif args.command == "evaluate":
        # 本地 CLI 明确标记为不可信执行面，不能凭输入字段伪装 trusted_ci。
        context = dict(data.get("context", {}))
        context["execution_surface"] = "local_cli"
        result = evaluate_gate(data.get("work", {}), context, data["action"])
    else: result = verify_manifest(data)
    print(json.dumps(result, ensure_ascii=False, sort_keys=True))
    if args.command == "validate-work-item" and not result["valid"]: return 2
    if args.command == "verify-evidence" and result["integrity"] != "PASS": return 2
    if args.command == "evaluate" and result["decision"] == "BLOCK": return 2
    return 0

if __name__ == "__main__":
    sys.exit(main())

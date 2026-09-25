import json, tempfile, unittest
from pathlib import Path
from scripts.governance.cli import main

class CliTest(unittest.TestCase):
    def test_route_command(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "task.json"; p.write_text(json.dumps({"task_kind":"anything"}), encoding="utf-8")
            self.assertEqual(0, main(["route", "--input", str(p)]))

    def test_local_evaluate_cannot_forge_trusted_ci(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "gate.json"
            p.write_text(json.dumps({"action":"inspect", "context": {
                "execution_surface":"trusted_ci", "approval_source":"forged",
                "policy_ref":"forged", "baseline_verified":True,
                "scope_verified":True, "content_quality":"PASS",
                "contract_coverage":"PASS", "implementation_quality":"PASS",
                "human_approval":"APPROVED"}}), encoding="utf-8")
            self.assertEqual(0, main(["evaluate", "--input", str(p)]))

import unittest
from scripts.governance.ci_gate import run_gate

class CiGateTest(unittest.TestCase):
    def test_advisory_missing_approval_is_reported(self):
        result = run_gate({"action":"inspect", "context":{"baseline_verified":True,"scope_verified":True,"content_quality":"PASS","contract_coverage":"PASS","implementation_quality":"PASS","human_approval":"PENDING"}})
        self.assertEqual("advisory", result["enforcement_mode"])
        self.assertEqual("REVIEW_REQUIRED", result["decision"])
        self.assertTrue(result["enforcement_effective"])

    def test_forbidden_runtime_stays_blocked(self):
        result = run_gate({"action":"run_runtime", "context":{}})
        self.assertEqual("BLOCK", result["decision"])
        self.assertFalse(result["enforcement_effective"])

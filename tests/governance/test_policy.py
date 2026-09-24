import unittest
from scripts.governance.policy import evaluate_gate

class PolicyTest(unittest.TestCase):
    def test_content_pass_does_not_imply_merge(self):
        result = evaluate_gate({"example_only":False}, {"baseline_verified":True,"scope_verified":True,"content_quality":"PASS","contract_coverage":"PASS","implementation_quality":"PASS","human_approval":"PENDING"}, "merge")
        self.assertEqual("PENDING", result["human_approval"])
        self.assertEqual("BLOCKED", result["formal_readiness"])

    def test_runtime_action_is_blocked(self):
        result = evaluate_gate({}, {}, "run_runtime")
        self.assertEqual("BLOCK", result["decision"])
        self.assertIn("ACTION_OUTSIDE_PROPOSED_SCOPE", result["reason_codes"])

    def test_local_context_can_never_allow_inspect(self):
        result = evaluate_gate({}, {"baseline_verified":True,"scope_verified":True,"content_quality":"PASS","contract_coverage":"PASS","implementation_quality":"PASS","human_approval":"APPROVED"}, "inspect")
        self.assertEqual("REVIEW_REQUIRED", result["formal_readiness"])
        self.assertNotEqual("ALLOW", result["decision"])

    def test_advisory_mode_reports_review_without_allowing_execution(self):
        result = evaluate_gate({}, {"baseline_verified":True,"scope_verified":True,"content_quality":"PASS","contract_coverage":"PASS","implementation_quality":"PASS","human_approval":"PENDING"}, "inspect")
        self.assertEqual("REVIEW_REQUIRED", result["decision"])
        self.assertEqual("REVIEW_REQUIRED", result["formal_readiness"])

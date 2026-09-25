import unittest
from scripts.governance.router import route_task

class RouterTest(unittest.TestCase):
    def test_contract_review_is_read_only(self):
        result = route_task({"task_kind": "contract-review", "requested_action": "inspect"})
        self.assertEqual("G-R01", result["route"])
        self.assertEqual("none", result["authorization_effect"])

    def test_unknown_task_requires_manual_review(self):
        result = route_task({"task_kind": "anything"})
        self.assertEqual("G-R00", result["route"])
        self.assertEqual("MANUAL_REVIEW", result["decision"])
        self.assertEqual("TASK_KIND_UNKNOWN", result["reason_code"])

    def test_missing_kind_requires_manual_review(self):
        result = route_task({})
        self.assertEqual("TASK_KIND_MISSING", result["reason_code"])

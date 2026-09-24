import unittest
from scripts.governance.validate import validate_work_item

class WorkItemValidationTest(unittest.TestCase):
    def test_path_traversal_is_denied(self):
        errors = validate_work_item({"id":"T1", "repository":"ha-ai-factory", "task_kind":"repository-governance", "requested_action":"inspect", "baseline_sha":None, "allowed_paths":["../contracts"], "forbidden_paths":["contracts/"], "required_scopes":[], "acceptance_ids":[], "status":"PROPOSED"})
        self.assertIn("PATH_OUTSIDE_SCOPE", errors)

    def test_example_cannot_be_approved(self):
        item = {"id":"T1", "repository":"ha-ai-factory", "task_kind":"repository-governance", "requested_action":"inspect", "baseline_sha":"abc", "allowed_paths":[], "forbidden_paths":[], "required_scopes":[], "acceptance_ids":[], "status":"APPROVED", "example_only":True}
        self.assertIn("EXAMPLE_CANNOT_APPROVE", validate_work_item(item))

    def test_valid_proposed_item(self):
        item = {"id":"T1", "repository":"ha-ai-factory", "task_kind":"repository-governance", "requested_action":"inspect", "baseline_sha":None, "allowed_paths":["docs/"], "forbidden_paths":["contracts/"], "required_scopes":[], "acceptance_ids":[], "status":"PROPOSED"}
        self.assertEqual([], validate_work_item(item))

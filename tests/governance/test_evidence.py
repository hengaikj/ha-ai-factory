import unittest
from scripts.governance.evidence import verify_manifest

class EvidenceTest(unittest.TestCase):
    def test_nonzero_exit_cannot_claim_pass(self):
        result = verify_manifest({"exit_code":1,"execution_status":"PASSED","raw_artifacts":[],"redacted_artifacts":[]})
        self.assertEqual("FAIL", result["integrity"])
        self.assertIn("EXIT_CODE_NONZERO", result["errors"])
        self.assertIn("STATUS_EXIT_CONFLICT", result["errors"])

    def test_complete_manifest_passes(self):
        result = verify_manifest({"exit_code":0,"execution_status":"PASSED","source_sha":"abc","argv":["pytest"],"cwd":"/repo","started_at":"a","finished_at":"b","provenance":{"source":"local"},"raw_artifacts":[],"redacted_artifacts":[]})
        self.assertEqual("PASS", result["integrity"])

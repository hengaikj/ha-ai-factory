import json, tempfile, unittest
from pathlib import Path
from scripts.governance.cli import main

class CliTest(unittest.TestCase):
    def test_route_command(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "task.json"; p.write_text(json.dumps({"task_kind":"anything"}), encoding="utf-8")
            self.assertEqual(0, main(["route", "--input", str(p)]))

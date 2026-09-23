#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "Initialize XZG AI Project Template v1 (Skill v5.7)"

# 建立模板定义的结构；重复运行不会覆盖已有文件。
mkdir -p \
  .agent agents \
  docs/architecture docs/decisions docs/design docs/product docs/requirement \
  contracts/api contracts/database contracts/security contracts/ui \
  backend frontend integration evidence deployment \
  ha-ai-agent-runtime ha-ai-factory-server ha-ai-factory-web \
  scm scripts templates

required_files=(
  .agent/project-context.yaml
  .agent/state.yaml
  .agent/rules.md
  SKILL.md
  docs/lifecycle.md
  docs/quality-gate.md
  docs/code-comment.md
  docs/database-comment.md
  agents/orchestrator.md
  contracts/api/api-contract.yaml
  contracts/database/database-template.sql
  contracts/security/security-contract.md
  docs/requirement/requirement-baseline.md
  docs/product/prd.md
  docs/design/design-handoff.md
  templates/task.yaml
  templates/review.yaml
  templates/evidence.yaml
)

missing=0
for path in "${required_files[@]}"; do
  if [[ ! -f "$path" ]]; then
    printf 'Missing required bootstrap file: %s\n' "$path" >&2
    missing=1
  fi
done

if (( missing )); then
  echo "Bootstrap initialization incomplete." >&2
  exit 1
fi

echo "Bootstrap initialization complete."

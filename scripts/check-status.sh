#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "Check XZG AI Software Factory Bootstrap Gate (Skill v5.7)"
required_dirs=(
  .agent agents docs/architecture docs/decisions docs/design docs/product
  docs/requirement contracts/api contracts/database contracts/security contracts/ui
  backend frontend integration evidence deployment ha-ai-agent-runtime
  ha-ai-factory-server ha-ai-factory-web scm scripts templates
)
required_files=(
  .agent/project-context.yaml .agent/state.yaml .agent/rules.md SKILL.md
  docs/lifecycle.md docs/quality-gate.md docs/code-comment.md docs/database-comment.md
  agents/orchestrator.md agents/contract-agent.md agents/backend-agent.md
  agents/frontend-agent.md contracts/api/api-contract.yaml
  contracts/database/database-template.sql contracts/security/security-contract.md
  docs/requirement/requirement-baseline.md docs/product/prd.md
  docs/design/design-handoff.md scm/gitlab-private.md
  scripts/init-xzg-project.sh scripts/check-status.sh
  templates/task.yaml templates/review.yaml templates/evidence.yaml
)

failed=0
for path in "${required_dirs[@]}"; do
  if [[ ! -d "$path" ]]; then
    printf 'FAIL missing directory: %s\n' "$path"
    failed=1
  fi
done
for path in "${required_files[@]}"; do
  if [[ ! -s "$path" ]]; then
    printf 'FAIL missing or empty file: %s\n' "$path"
    failed=1
  fi
done

if (( failed )); then
  echo "Bootstrap Gate Result: FAIL"
  exit 1
fi

echo "Bootstrap Gate Result: PASS"

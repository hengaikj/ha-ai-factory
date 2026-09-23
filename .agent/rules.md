# Project Rules

- Follow XZG AI Software Engineering Skill v5.7 lifecycle: Discovery → Requirement → Product → UX/UI → Design Handoff → Contract → Development → Review → Integration → Release.
- Do not skip Requirement, Design, Contract, or Review gates.
- Do not begin business-code development before the applicable Requirement, Design, and Contract gates are approved.
- The Orchestrator manages project state, task assignment, and gates; it must not bypass workflow or approve its own review.
- Contract ambiguities must be reported as `HUMAN_DECISION_REQUIRED`.
- Key classes and functions require Chinese business comments.
- Every database table and column requires a Chinese `COMMENT`.
- Record commit, build, test, review, and known-issue evidence using the repository templates.

# Independent Contract Gate Review — Laya Runtime

Date: 2026-09-23  
Review target: HEAD `b120b465fb760b5c680c192df0704c86c883f849`  
Review type: Read-only independent Contract Gate review. Only this review evidence was added; no source, contract, API, security, decision, baseline, or gate file was modified. No build or tests were run.

## Evidence checked

- `contracts/laya-runtime-contract-v1.0.md`
- `contracts/api/laya-inference-api.yaml`
- `contracts/api/api-contract.yaml`
- `contracts/security/security-contract.md`
- `contracts/contract-decisions.md`
- `contracts/contract-baseline-v1.0.md`
- `contracts/contract-gate-approval.md`
- `docs/superpowers/specs/2026-09-23-laya-runtime-integration-design.md`
- Existing M02 and Global Gate records under `evidence/`
- Upstream `laya/agent.py`, `laya/serve.py`, and `laya/router.py` at their public `main` branch as retrieved on 2026-09-23. The production release/revision remains an explicit precondition and is not claimed to be pinned here.

## Previous findings L1–L4

- **L1 response-shape mismatch: closed.** The API now declares upstream `type`, `action.act_probability`, and score `legend`, while still rejecting unknown answer properties (`contracts/api/laya-inference-api.yaml:221-268`). This matches upstream answer construction (`laya/agent.py`, `system_one`, [lines 469-498](https://github.com/NandhaKishorM/laya/blob/main/laya/agent.py#L469-L498)). The score semantics/range part of L1 remains open as finding L5 below.
- **L2 request correlation: closed.** The Contract requires the private gateway to bind the request ID to its upstream exchange and echo it, and requires Runtime to match it (`contracts/laya-runtime-contract-v1.0.md:54`). The API declares required `X-Request-ID` response headers on each explicit success/error response (`contracts/api/laya-inference-api.yaml:79-132`). The upstream does not echo it, so the gateway adaptation is explicit and consistent.
- **L3 stable and safe errors: closed.** The Contract prohibits forwarding upstream `detail`, requires a stable code and static safe text, and enumerates the code mapping (`contracts/laya-runtime-contract-v1.0.md:70-74`). The API now requires an allowlisted `code` and sanitized `detail` (`contracts/api/laya-inference-api.yaml:269-275`). This explicitly adapts the upstream's `HTTP 422 detail=str(e)` behavior (`laya/serve.py`, [lines 137-159](https://github.com/NandhaKishorM/laya/blob/main/laya/serve.py#L137-L159)).
- **L4 zero-queue admission: closed at the Contract level.** The Contract now assigns one-in-flight/zero-queue rejection to the private gateway before it calls Laya (`contracts/laya-runtime-contract-v1.0.md:74`); the API records the limit/status (`contracts/api/laya-inference-api.yaml:38-41`). This does not rely on the upstream's single-worker executor and `asyncio.Lock`, which can queue callers (`laya/serve.py`, [lines 108-119 and 147-155](https://github.com/NandhaKishorM/laya/blob/main/laya/serve.py#L108-L119)).

## Findings

### L5 — RETURN, blocking: score result meaning and allowed range remain undefined

The Contract requires Runtime to check result allowed values (`contracts/laya-runtime-contract-v1.0.md:54-56`), but the API allows any JSON number for `ScoreAnswer.score` (`contracts/api/laya-inference-api.yaml:238-253`) and only describes `legend` as a mapping. The upstream does not return one discrete criterion; it computes an expected zero-based ordinal score, `sum(i * p_i)`, for `k` criteria (`laya/agent.py`, [lines 478-486](https://github.com/NandhaKishorM/laya/blob/main/laya/agent.py#L478-L486)). The Contract/API must define that meaning, require the legend and probability indices to match the submitted ordered criteria, and state the dynamic range `0 <= score <= k-1` (or another explicitly selected policy). Without this, Runtime cannot apply the stated allowed-value check consistently.

### L6 — RETURN, blocking: security contract timeout conflicts with the retry budget

The Laya Contract and API specify a 5-second per-attempt total timeout, one retry after 200 ms, and a 10.2-second maximum operation (`contracts/laya-runtime-contract-v1.0.md:74`; `contracts/api/laya-inference-api.yaml:27-37`). The security contract instead says “总调用超时 5 秒” while also permitting one retry (`contracts/security/security-contract.md:84`). “Total call timeout” reads as a 5-second whole-operation ceiling, which cannot be reconciled with the specified 10.2-second retry budget. Align the wording and state the per-attempt and whole-operation limits in the security contract.

## Scope and cross-document checks that passed

- M02 still exposes only Runtime configuration status and a fail-closed disabled run endpoint; it does not start real execution (`contracts/api/api-contract.yaml:616-660`). HD-002 remains explicitly deferred for M02 in the API, decisions, baseline, and security contract (`contracts/api/api-contract.yaml:6-15`; `contracts/contract-decisions.md:11-15,26-28`; `contracts/contract-baseline-v1.0.md:11-15`; `contracts/security/security-contract.md:74-86,114-123`).
- HD-006's M02 deferral remains intact: no Git, CI, test orchestration, notification, or other M02 external integration. The separate future private Laya provider is not represented as an M02 integration (`contracts/contract-decisions.md:15,26-28`; `contracts/security/security-contract.md:120-123`; approved design `docs/superpowers/specs/2026-09-23-laya-runtime-integration-design.md:14-18,37-45`).
- Identity and data boundaries align across the Laya Contract and security Contract: Java authorization precedes Runtime use, service identity is audience-limited and at most 10 minutes, mTLS and private ingress protect the Runtime-to-gateway path, and request state is minimized (`contracts/laya-runtime-contract-v1.0.md:26-50`; `contracts/security/security-contract.md:24-32,78-87`).
- Upstream request/response behavior otherwise matches the revised API: fixed `multilingual` selection is supported by `serve.py` model resolution and `router.py`; answer metadata matches the declared schemas; and gateway adaptations are explicit for correlation, safe errors, and admission control. Production model revision, checkpoint digest, license review, and offline evaluation remain stated deployment prerequisites (`contracts/laya-runtime-contract-v1.0.md:82-95`).
- The Gate file correctly limits its earlier PASS to M02 and keeps the additional Laya Contract pending independent review (`contracts/contract-gate-approval.md:21-31`). Existing Global Gate evidence does not claim to approve this Laya scope.

## Final decision

**Laya Runtime Contract Gate: RETURN.** The four prior findings are closed at the contract boundary, but L5 leaves score validation semantics incomplete and L6 leaves a cross-document timeout contradiction. The Global Contract Gate PASS for the M02 baseline remains unchanged. No Laya Runtime implementation is authorized until these findings are corrected and independently reviewed.

# Independent Contract Gate Review — Laya Runtime

Date: 2026-09-23  
Review target: HEAD `f6b4313b1bb9a2d24e714abedb788128d12dedc8`
Review type: Read-only independent Contract Gate review. Only this review evidence was updated; no source, contract, API, security, decision, baseline, or gate file was modified. No build or tests were run.

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
- Upstream `laya/agent.py`, `laya/serve.py`, and `laya/router.py` at their public `main` branch as retrieved on 2026-09-23. Production release/revision remains an explicit prerequisite and is not claimed to be pinned here.

## Previous findings L1–L6

- **L1 response fields and score semantics: substantively closed.** The API now declares upstream `type`, `action.act_probability`, and score `legend` and defines score as a zero-based ordinal expectation with dynamic range and matching `legend`/`probabilities` keys and values (`contracts/laya-runtime-contract-v1.0.md:56`; `contracts/api/laya-inference-api.yaml:238-255`). This matches upstream `Agent.system_one` (`laya/agent.py`, [lines 469-498](https://github.com/NandhaKishorM/laya/blob/main/laya/agent.py#L469-L498)). A required-field mismatch remains as L7 below.
- **L2 request correlation: closed.** The Contract requires gateway binding and echo of `X-Request-ID` (`contracts/laya-runtime-contract-v1.0.md:54`); the API requires the response header on each explicit success/error response (`contracts/api/laya-inference-api.yaml:79-132`). The upstream does not echo it, so the gateway adaptation is explicit.
- **L3 stable and safe errors: closed.** The Contract prohibits forwarding upstream `detail`, requires stable codes and static safe text, and enumerates the mappings (`contracts/laya-runtime-contract-v1.0.md:70-72`). The API enforces a code enum and sanitized detail (`contracts/api/laya-inference-api.yaml:271-277`), explicitly adapting upstream `HTTP 422 detail=str(e)` (`laya/serve.py`, [lines 137-159](https://github.com/NandhaKishorM/laya/blob/main/laya/serve.py#L137-L159)).
- **L4 zero-queue admission: closed.** The Contract assigns one-in-flight/zero-queue rejection to the private gateway before calling Laya (`contracts/laya-runtime-contract-v1.0.md:74`); the API records the limit and overload status (`contracts/api/laya-inference-api.yaml:38-41`). This does not depend on the upstream single-worker executor/asyncio lock, which can queue callers (`laya/serve.py`, [lines 108-119 and 147-155](https://github.com/NandhaKishorM/laya/blob/main/laya/serve.py#L108-L119)).
- **L5 score semantics/range: closed.** The Contract now defines `score = sum(i * p_i)` for ordered criteria and requires `0 <= score <= k-1`, with exact criterion-index correspondence for legend and probabilities (`contracts/laya-runtime-contract-v1.0.md:56`). The API sets the static upper bound to 9 for up to 10 criteria and states the runtime's dynamic `k-1` validation (`contracts/api/laya-inference-api.yaml:238-255`).
- **L6 timeouts: closed.** The specialized Contract/API use 5 seconds per attempt, one retry after 200 ms, and a 10.2-second whole-operation maximum (`contracts/laya-runtime-contract-v1.0.md:74`; `contracts/api/laya-inference-api.yaml:27-37`). The security Contract now says each attempt is 5 seconds and the whole operation is at most 10.2 seconds (`contracts/security/security-contract.md:84`).

## Finding

### L7 — RETURN, blocking: API schema does not require all fields the Contract and upstream require

The Contract says choice and score answers contain `confidence` and `probabilities`, and noul answers contain `confidence` (`contracts/laya-runtime-contract-v1.0.md:56`). Upstream `Agent.system_one` always emits those fields for all three answer types (`laya/agent.py`, [lines 469-494](https://github.com/NandhaKishorM/laya/blob/main/laya/agent.py#L469-L494)). However, the OpenAPI only requires `[type, choice, action]` for choice and `[type, noul, action]` for noul; `confidence` and `probabilities` remain optional there (`contracts/api/laya-inference-api.yaml:226-264`). A response missing these Contract-required values therefore passes the published schema. Add them to the appropriate `required` lists so generated validators and the normative Contract agree.

## Scope and cross-document checks that passed

- The M02 API exposes Runtime configuration status only and rejects real Agent Runtime execution (`contracts/api/api-contract.yaml:616-660`). HD-002 remains explicitly deferred for M02 in the API, decision list, baseline, and security Contract (`contracts/api/api-contract.yaml:6-15`; `contracts/contract-decisions.md:11-15,26-28`; `contracts/contract-baseline-v1.0.md:11-15`; `contracts/security/security-contract.md:74-87,114-123`).
- HD-006's M02 deferral remains intact: no M02 Git, CI, test orchestration, notification, or other external integration; future private Laya is a separate reviewed capability (`contracts/contract-decisions.md:15,26-28`; `contracts/security/security-contract.md:120-123`; approved design `docs/superpowers/specs/2026-09-23-laya-runtime-integration-design.md:14-18,37-45`).
- Identity and data boundaries align: Java authorization precedes Runtime use, the service identity is audience-limited and at most 10 minutes, mTLS and private ingress protect Runtime-to-gateway calls, and request state is minimized (`contracts/laya-runtime-contract-v1.0.md:26-50`; `contracts/security/security-contract.md:24-32,78-87`).
- The revised request and response shapes otherwise match upstream: the fixed `multilingual` route is supported, the response includes routing metadata and token usage, and the gateway's correlation/error/admission adaptations are explicit. Model revision, weight digest, license review, and offline evaluation remain production prerequisites (`contracts/laya-runtime-contract-v1.0.md:82-95`).
- The Gate record limits its existing PASS to M02 and keeps Laya pending independent review (`contracts/contract-gate-approval.md:21-31`). Existing Global Gate evidence does not approve this additional scope.

## Final decision

**Laya Runtime Contract Gate: RETURN.** L1–L6 are closed. L7 leaves the machine-readable response schema weaker than the normative Contract and the observed upstream response. The Global Contract Gate PASS for the M02 baseline remains unchanged. No Laya Runtime implementation is authorized until L7 is corrected and independently re-reviewed.

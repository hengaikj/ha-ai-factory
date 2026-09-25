# Independent Contract Gate Review — Laya Runtime

Date: 2026-09-23  
Review target: HEAD `4d3eef95862849aa6133d01f9b48ea1c95fce411`
Review type: Read-only independent Contract Gate review. Only this review evidence was updated; no source or contract files were modified. No build or tests were run.

## Evidence checked

- `contracts/laya-runtime-contract-v1.0.md`
- `contracts/api/laya-inference-api.yaml`
- `contracts/api/api-contract.yaml`
- `contracts/security/security-contract.md`
- `contracts/contract-decisions.md`
- `contracts/contract-baseline-v1.0.md`
- `contracts/contract-gate-approval.md`
- `docs/superpowers/specs/2026-09-23-laya-runtime-integration-design.md`
- Existing M02 and Global Gate evidence under `evidence/`
- Upstream `laya/agent.py`, `laya/serve.py`, and `laya/router.py` at public `main`, retrieved 2026-09-23. A specific production revision remains a production prerequisite and is not claimed as locked by this review.

## Previous findings L1–L7

- **L1 — closed.** Answer variants declare upstream `type`, `action.act_probability`, and score `legend`; unknown answer fields are rejected (`contracts/api/laya-inference-api.yaml:221-270`). This matches upstream answer construction (`laya/agent.py`, [lines 469-498](https://github.com/NandhaKishorM/laya/blob/main/laya/agent.py#L469-L498)).
- **L2 — closed.** The Contract specifies that the gateway binds each upstream exchange to the Runtime request ID and echoes `X-Request-ID`; Runtime validates the echo (`contracts/laya-runtime-contract-v1.0.md:54`). The API requires this response header for each declared success/error status (`contracts/api/laya-inference-api.yaml:79-132`).
- **L3 — closed.** The gateway maps upstream errors to the documented stable code set and static safe detail, without forwarding provider exception text (`contracts/laya-runtime-contract-v1.0.md:70-72`; `contracts/api/laya-inference-api.yaml:271-277`). This accounts for upstream `HTTP 422 detail=str(e)` behavior (`laya/serve.py`, [lines 137-159](https://github.com/NandhaKishorM/laya/blob/main/laya/serve.py#L137-L159)).
- **L4 — closed.** Admission control is explicitly assigned to the private gateway: one upstream inference in flight per instance, no queue, and immediate overload rejection before calling Laya (`contracts/laya-runtime-contract-v1.0.md:74`; `contracts/api/laya-inference-api.yaml:38-41`). It does not rely on Laya's single-worker executor and asyncio lock, which otherwise queue callers (`laya/serve.py`, [lines 108-119 and 147-155](https://github.com/NandhaKishorM/laya/blob/main/laya/serve.py#L108-L119)).
- **L5 — closed.** Score is a zero-based ordinal expectation, `sum(i * p_i)`, with runtime range `0 <= score <= k-1`; legend and probability keys must exactly match indices `0..k-1`, and legend values must match the ordered request criteria (`contracts/laya-runtime-contract-v1.0.md:56`). The API's fixed bound 0–9 covers the maximum 10 criteria, with the dynamic bound enforced by Runtime (`contracts/api/laya-inference-api.yaml:238-255`). This matches upstream computation and mappings (`laya/agent.py`, [lines 478-486](https://github.com/NandhaKishorM/laya/blob/main/laya/agent.py#L478-L486)).
- **L6 — closed.** The Contract/API specify 5 seconds per attempt, one retry after 200 ms, and a 10.2-second whole-operation limit (`contracts/laya-runtime-contract-v1.0.md:74`; `contracts/api/laya-inference-api.yaml:27-37`). The security Contract now uses the same per-attempt and whole-operation limits (`contracts/security/security-contract.md:84`).
- **L7 — closed.** `ChoiceAnswer.required` now includes `confidence` and `probabilities`; `NoulAnswer.required` includes `confidence`; `ScoreAnswer` requires both as well (`contracts/api/laya-inference-api.yaml:226-264`). These fields are emitted for the corresponding answer types by upstream (`laya/agent.py`, [lines 469-494](https://github.com/NandhaKishorM/laya/blob/main/laya/agent.py#L469-L494)).

## Scope and consistency checks

- The request schema aligns with the upstream `SystemOneRequest` shapes for string state and typed choice/score/noul questions. The gateway rejects unknown request fields, injects the fixed `multilingual` checkpoint, and uses the same private inference endpoint. The response schema matches upstream model, answer, usage, and routing structure (`contracts/api/laya-inference-api.yaml:19-26,139-220`; upstream `laya/serve.py` and `laya/router.py`).
- Correlation, sanitized error mapping, token-budget rejection, zero-queue admission, and fixed checkpoint behavior have explicit gateway enforcement in the Contract/API; the upstream's missing request-ID echo and queued lock behavior are covered by those gateway duties (`contracts/laya-runtime-contract-v1.0.md:32,50,54,72-74`).
- Identity and data boundaries align: Java performs OIDC and project/task authorization before Runtime use; the forwarded service identity is audience-limited and at most 10 minutes; Runtime-to-Laya traffic uses private networking and workload mTLS; only minimized sanitized state is sent (`contracts/laya-runtime-contract-v1.0.md:26-50`; `contracts/security/security-contract.md:24-32,78-87`).
- M02 continues to expose only configuration status and a fail-closed disabled run endpoint (`contracts/api/api-contract.yaml:616-660`). HD-002's M02 deferral remains explicit across API, decisions, baseline, and security files (`contracts/api/api-contract.yaml:6-15`; `contracts/contract-decisions.md:11-15,26-28`; `contracts/contract-baseline-v1.0.md:11-15`; `contracts/security/security-contract.md:74-87,114-123`).
- HD-006's M02 deferral remains intact: no Git, CI, test orchestration, notifications, or other external integrations in M02. The future private Laya provider is a separate capability behind this review (`contracts/contract-decisions.md:15,26-28`; `contracts/security/security-contract.md:120-123`; design `docs/superpowers/specs/2026-09-23-laya-runtime-integration-design.md:14-18,37-45`).
- The M02 Global Contract Gate PASS is limited to its existing scope. The gate file keeps the additional Laya scope pending independent review and prohibits implementation until this scope receives PASS evidence committed to HEAD (`contracts/contract-gate-approval.md:21-31`). Model revision, checkpoint/weight digest, license review, and offline evaluation remain production enablement prerequisites (`contracts/laya-runtime-contract-v1.0.md:82-95`); they do not contradict the reviewed contract boundary.

## Final decision

**Laya Runtime Contract Gate: PASS.** Findings L1–L7 are closed, the API matches the reviewed upstream behavior with the declared gateway adaptations, and the identity/data boundaries and M02 HD-002/HD-006 deferrals remain consistent. This PASS approves only the Laya Runtime Contract scope for development planning/implementation under its stated boundaries; it does not authorize M02 real Runtime execution or production model activation before the listed supply-chain and evaluation prerequisites. The Global Contract Gate status remains unchanged.

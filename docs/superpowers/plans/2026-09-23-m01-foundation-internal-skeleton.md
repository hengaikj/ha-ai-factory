# M01 Foundation Internal Skeleton Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a runnable M01 foundation skeleton with in-memory identity/session, permission evaluation, audit events, and a minimal console shell, while honoring the user's selected scope.

**Architecture:** Java 17 and Spring Boot 3 provide internal domain/application services with in-memory adapters only. The Vue 3/TypeScript console is a neutral shell and does not imply authentication. No public authentication endpoints, persistent RBAC, database migrations, or runtime execution are introduced.

**Tech Stack:** Java 17, Spring Boot 3.x, Maven, JUnit 5; Vue 3, TypeScript, Vite, pnpm.

**Spec:** `docs/handoff/M01-foundation-module-spec-v1.0.md`, constrained by the user's selected internal-skeleton scope and `docs/handoff/developer-instruction-M01.md`.

## Global Constraints

- Do not modify files under `contracts/`.
- Do not expose login, logout, token, session, or authorization HTTP endpoints.
- Do not add a persistent user/role/permission/session/audit schema or migration.
- Keep identity, session, role, permission, and audit stores in process memory and label them non-production.
- Do not implement Agent Runtime execution (M04).
- Use Java 17 + Spring Boot 3.x as stated by `docs/development/development-architecture-baseline-v1.0.md`.
- Add Chinese business comments to key classes and functions; follow `docs/code-comment.md`.
- Provide automated tests and an Evidence report; use a Chinese commit message.

## Review Focus

- Unknown or inactive identity must not obtain an active in-memory session; test session creation rejection.
- Expired session must not resolve to an active identity; test expiry boundary.
- Missing permission must deny access; test allow and deny cases.
- Audit payload must not retain credential/token material; test event sanitization and immutability.
- Restart loses all in-memory state; explicitly document this limitation and test store starts empty.

---

## File Structure

- `ha-ai-factory-server/pom.xml`: Spring Boot build and test configuration.
- `ha-ai-factory-server/src/main/java/com/hengaikj/haifactory/`: application entry, domain records, application services, and in-memory adapters.
- `ha-ai-factory-server/src/test/java/com/hengaikj/haifactory/`: unit tests for identity/session, permission evaluation, and audit behavior.
- `ha-ai-factory-web/package.json`, `index.html`, `vite.config.ts`, `tsconfig.json`: Vue console build setup.
- `ha-ai-factory-web/src/main.ts`, `App.vue`, `style.css`: accessible neutral console frame with explicit internal-skeleton status.
- `evidence/M01-foundation-implementation.md`: scope, commands, results, limitations, and review evidence.
- `.agent/state.yaml`: update implementation status only after this bounded work is completed and its evidence is recorded.

### Task 1: Bootstrap backend and prove it starts

**Files:**
- Create: `ha-ai-factory-server/pom.xml`
- Create: `ha-ai-factory-server/src/main/java/com/hengaikj/haifactory/HaAiFactoryApplication.java`
- Create: `ha-ai-factory-server/src/test/java/com/hengaikj/haifactory/HaAiFactoryApplicationTest.java`

**Interfaces:**
- Produces: standard Spring Boot entry point `HaAiFactoryApplication.main(String[] args)`.

- [x] **Step 1: Write a failing context-load test** asserting Spring starts and exposes a `HaAiFactoryApplication` context.
- [x] **Step 2: Run `cd ha-ai-factory-server && mvn test`** and confirm the test fails because build files/classes are absent.
- [x] **Step 3: Add Maven configuration** for Java 17, Spring Boot 3.x, `spring-boot-starter`, and `spring-boot-starter-test`; add the application entry point and register internal services in Spring.
- [x] **Step 4: Run `cd ha-ai-factory-server && mvn test`** and confirm the context-load test passes.

### Task 2: Implement internal identity and in-memory sessions

**Files:**
- Create: `ha-ai-factory-server/src/main/java/com/hengaikj/haifactory/identity/UserIdentity.java`
- Create: `ha-ai-factory-server/src/main/java/com/hengaikj/haifactory/identity/SessionRecord.java`
- Create: `ha-ai-factory-server/src/main/java/com/hengaikj/haifactory/identity/InMemoryIdentityStore.java`
- Create: `ha-ai-factory-server/src/main/java/com/hengaikj/haifactory/identity/InMemorySessionStore.java`
- Create: `ha-ai-factory-server/src/main/java/com/hengaikj/haifactory/identity/IdentitySessionService.java`
- Test: `ha-ai-factory-server/src/test/java/com/hengaikj/haifactory/identity/IdentitySessionServiceTest.java`

**Interfaces:**
- `UserIdentity(String subjectId, String displayName, boolean active)`.
- `SessionRecord(String sessionId, String subjectId, Instant createdAt, Instant expiresAt)`.
- `IdentitySessionService.createSession(String subjectId, Duration ttl): SessionRecord` rejects unknown/inactive users and non-positive TTL.
- `IdentitySessionService.resolveActiveSession(String sessionId): Optional<UserIdentity>` rejects missing/expired sessions.

- [x] **Step 1: Write tests** for active user success, unknown/inactive user rejection, invalid TTL rejection, active lookup, expired-at-boundary lookup, and empty store at construction.
- [x] **Step 2: Run `cd ha-ai-factory-server && mvn -Dtest=IdentitySessionServiceTest test`** and confirm expected failures.
- [x] **Step 3: Implement records, concurrent in-memory stores, and service** with an injectable `Clock` for deterministic expiry checks. Never expose session IDs through an HTTP controller.
- [x] **Step 4: Run the focused Maven test** and confirm all cases pass.

### Task 3: Implement in-memory role/permission evaluation

**Files:**
- Create: `ha-ai-factory-server/src/main/java/com/hengaikj/haifactory/authorization/Role.java`
- Create: `ha-ai-factory-server/src/main/java/com/hengaikj/haifactory/authorization/Permission.java`
- Create: `ha-ai-factory-server/src/main/java/com/hengaikj/haifactory/authorization/InMemoryRoleAssignments.java`
- Create: `ha-ai-factory-server/src/main/java/com/hengaikj/haifactory/authorization/PermissionEvaluator.java`
- Test: `ha-ai-factory-server/src/test/java/com/hengaikj/haifactory/authorization/PermissionEvaluatorTest.java`

**Interfaces:**
- `Role(String roleId, Set<String> permissionIds)` and `Permission(String permissionId)`.
- `PermissionEvaluator.hasPermission(String subjectId, String permissionId): boolean`; empty/missing assignments deny by default.

- [x] **Step 1: Write tests** for assigned permission allow, absent permission deny, unknown subject deny, and unknown role deny.
- [x] **Step 2: Run the focused Maven test** and confirm it fails before implementation.
- [x] **Step 3: Implement immutable role/permission values and an in-memory assignment evaluator** with default-deny behavior. Do not assert that this is an approved final project RBAC matrix.
- [x] **Step 4: Run the focused Maven test** and confirm all cases pass.

### Task 4: Implement append-only in-memory audit events

**Files:**
- Create: `ha-ai-factory-server/src/main/java/com/hengaikj/haifactory/audit/AuditEvent.java`
- Create: `ha-ai-factory-server/src/main/java/com/hengaikj/haifactory/audit/InMemoryAuditLog.java`
- Create: `ha-ai-factory-server/src/main/java/com/hengaikj/haifactory/audit/AuditLogService.java`
- Test: `ha-ai-factory-server/src/test/java/com/hengaikj/haifactory/audit/AuditLogServiceTest.java`

**Interfaces:**
- `AuditEvent(String eventId, String actorId, String action, String objectType, String objectId, Map<String,String> details, Instant occurredAt)`.
- `AuditLogService.record(...)` appends; `list()` returns an immutable snapshot. Credential-like detail keys (`token`, `secret`, `password`, case-insensitive) are omitted.

- [x] **Step 1: Write tests** for append ordering, immutable snapshots, secret-field omission, and empty log on startup.
- [x] **Step 2: Run the focused Maven test** and confirm the expected failures.
- [x] **Step 3: Implement append-only in-memory audit storage** with immutable event details and an injectable `Clock`.
- [x] **Step 4: Run the focused Maven test** and confirm all cases pass.

### Task 5: Add neutral console shell

**Files:**
- Create: `ha-ai-factory-web/package.json`
- Create: `ha-ai-factory-web/pnpm-lock.yaml`
- Create: `ha-ai-factory-web/index.html`
- Create: `ha-ai-factory-web/vite.config.ts`
- Create: `ha-ai-factory-web/tsconfig.json`
- Create: `ha-ai-factory-web/src/main.ts`
- Create: `ha-ai-factory-web/src/App.vue`
- Create: `ha-ai-factory-web/src/style.css`

**Interfaces:**
- Vite development/build entry rendering a static console shell; no auth route, auth form, API client, or user/session persistence.

- [x] **Step 1: Add a minimal Vue/Vite app setup** and a script `pnpm build`.
- [x] **Step 2: Render the shell** with product name, navigation placeholders, and a visible label “内部骨架 / 无登录与持久化权限”.
- [x] **Step 3: Run `cd ha-ai-factory-web && pnpm build`** and confirm successful static build.
- [x] **Step 4: Inspect rendered source** to confirm it contains no credential form, token storage, or authentication API calls.

### Task 6: Record scope evidence and update project state

**Files:**
- Create: `evidence/M01-foundation-implementation.md`
- Modify: `.agent/state.yaml`

- [ ] **Step 1: Run `cd ha-ai-factory-server && mvn test`** and `cd ha-ai-factory-web && pnpm build`; record exact results.
- [ ] **Step 2: Write evidence** identifying implemented in-memory capabilities, test/build outputs, commit reference, reviewer status, and explicit exclusions: public authentication, persistent RBAC/audit, migrations, and Agent Runtime execution.
- [ ] **Step 3: Update `.agent/state.yaml`** to mark only this bounded implementation work complete, preserve unresolved contract items as deferred, and avoid claiming full M01 authentication/RBAC completion.
- [ ] **Step 4: Run `git diff --check`** and inspect `git status --short` plus `git diff --stat`.
- [ ] **Step 5: Commit with a Chinese message**, then push the feature branch and open a PR targeting `master`; PR text must state the scope limitation and cite evidence.

## Self-Review

- Spec coverage: User, Identity, Session, Role, Permission, and AuditLog are represented as in-memory internal components; console framework is a neutral static shell. Authentication endpoints and durable RBAC are explicitly excluded per the user's scope selection. No Contract or database contract change is planned.
- Placeholder scan: no TBD/TODO steps; each interface and test command is named.
- Type consistency: identity/session signatures use `String` IDs, `Instant`, and `Duration`; permission evaluation uses stable string IDs; audit details are immutable string maps.
- Review focus: unknown/inactive identities, expiry boundary, default-deny permissions, audit secret omission/immutability, and restart data loss each have planned tests or explicit evidence.
- Gate note: `.agent/state.yaml` and `contracts/security/security-contract.md` currently retain stale `pending` language that conflicts with `contracts/contract-gate-approval.md`. The implementation must not silently rewrite contract status. The PR must call out the conflict and label this work as bounded internal scaffolding only.

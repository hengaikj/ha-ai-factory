# Laya Runtime Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在获得审批后，将 HA Agent Runtime 接入独立内网 Laya 推理服务，并保留项目权限、失败关闭和人工复核边界。

**Architecture:** Java 后端继续负责用户身份和项目 RBAC；Python Agent Runtime 校验已批准任务并通过 provider adapter 调用独立 Laya FastAPI 服务；Laya 只返回类型化推理结果。模型调用用途、数据、服务认证、版本、审计及运行边界先写入专项 Contract 并通过独立 Gate。

**Tech Stack:** Java 17 / Spring Boot 3.x；Python 3.10+ / FastAPI；Laya 自托管 HTTP API；Docker Compose 或经部署 Contract 批准的内部服务编排。

**Spec:** `docs/superpowers/specs/2026-09-23-laya-runtime-integration-design.md`

## Global Constraints

- Global Contract Gate 必须由独立 Reviewer 记录 PASS，且 Laya Runtime 专项 Contract 必须获批后，才可执行任何实现任务。
- M02 当前 HD-002 和 HD-006 延期继续有效；Laya 只能在经批准的后续范围内接入。
- Laya 服务只允许内网 Agent Runtime 调用，不开放公网或浏览器访问。
- HA Web 后端仍是 OIDC 身份与项目 RBAC 权威来源；Laya 输出不得作为权限或人工 Gate 决策。
- Runtime 只发送专项 Contract 批准的字段和问题模板；不发送超范围项目数据或凭据。
- 运行时不得从公网动态下载模型；软件版本、权重摘要和依赖须按批准版本锁定。
- Laya 错误、超时、认证失败、输出校验失败或低可信结果都必须失败关闭并转人工处理，不得隐式切换模型或执行副作用。
- 所有实现和验证须满足仓库 `SKILL.md`、`docs/lifecycle.md`、`docs/quality-gate.md` 及获批 Contract 的要求。
- 当前 M01 身份会话和权限实现为内存骨架；生产调用前必须先具备符合 HD-001/HD-003 的真实认证、持久授权和服务身份能力，否则仅可保持离线测试。

## Review Focus

- Runtime 配置缺失、未批准、已拒绝、过期或超范围时，在触达 Laya 前拒绝并记录明确错误。
- 跨项目身份、无效服务身份和缺少任务关联时拒绝调用，且不泄露其他项目数据。
- Laya 超时、不可用、认证失败或返回无效 JSON 时不产生成功推理结果、不触发下游动作。
- 恶意、超长或含敏感信息的任务内容受 Contract 数据规则限制，且原文不会进入普通日志。
- Laya 返回未知标签、未批准模型版本或错误问题模板版本时拒绝结果并记录校验失败。

---

## Entry Gate

开始 Task 1 前，先由独立 Reviewer 复审当前 Gate 复审并在 `contracts/contract-gate-approval.md` 记录 Global Contract Gate PASS。当前 HEAD 已由独立 Reviewer 复审为 PASS，入口条件已满足。Task 1 形成 Laya Runtime 专项 Contract 并取得独立 PASS；Task 1 PASS 前不得执行 Task 2 及后续实现任务。生产部署还要求 M01 身份/权限实现满足 HD-001/HD-003；当前仅有内存骨架，不满足该运行前提。若专项 Contract 审查认定需要重开 Requirement、Product 或 Design Gate，先按生命周期完成这些阶段再继续。

## Planned File Map

- `contracts/contract-decisions.md`：记录 Laya 用途和 HD-002/HD-006 范围变化及影响。
- `contracts/contract-baseline-v1.0.md`、`contracts/contract-gate-approval.md`：仅在审批流程要求时更新本次 Runtime 专项基线与 Gate 状态。
- `contracts/api/api-contract.yaml`：记录获批 Runtime 调用契约；不复活已删除的资源内容接口。
- `contracts/api/laya-inference-api.yaml`：独立定义 Runtime 到 Laya 的私网推理协议、字段约束、mTLS 和错误语义。
- `contracts/security/security-contract.md`：记录调用身份、数据最小化、隔离、审计和失败关闭策略。
- `contracts/laya-runtime-contract-v1.0.md`：限定 Laya 后续模块的用途、运行边界和生产启用条件。
- `ha-ai-agent-runtime/pyproject.toml`：锁定 Python 依赖及测试配置。
- `ha-ai-agent-runtime/app/main.py`：创建 FastAPI 应用并挂载获批内部 Runtime 路由。
- `ha-ai-agent-runtime/app/config.py`：读取经过验证的内部 Laya URL、超时和获批服务认证配置。
- `ha-ai-agent-runtime/app/decision/models.py`：定义获批决策请求、响应及错误类型。
- `ha-ai-agent-runtime/app/decision/providers/base.py`：定义可替换的 `DecisionModelProvider` 协议。
- `ha-ai-agent-runtime/app/decision/providers/laya_http.py`：实现 Laya `/v1/systemone` HTTP 适配器。
- `ha-ai-agent-runtime/app/decision/service.py`：执行身份/配置/模板校验、调用和结果验证。
- `ha-ai-agent-runtime/app/security/service_auth.py`：实现获批的 Runtime 到 Laya 服务认证客户端。
- `ha-ai-agent-runtime/tests/`：覆盖 provider、权限前置、失败关闭、日志隐私和契约行为。
- `ha-ai-factory-server/src/main/java/com/hengaikj/ai/factory/runtime/AgentDecisionController.java`：暴露获批的外部用户/API入口。
- `ha-ai-factory-server/src/main/java/com/hengaikj/ai/factory/runtime/AgentDecisionService.java`：执行当前用户项目权限检查并编排 Runtime 调用。
- `ha-ai-factory-server/src/main/java/com/hengaikj/ai/factory/runtime/AgentDecisionAuthorizer.java`：用服务端认证上下文执行项目/任务级授权。
- `ha-ai-factory-server/src/main/java/com/hengaikj/ai/factory/runtime/RuntimeServiceIdentityIssuer.java`：按获批 HD-001 机制签发 Runtime 短时身份。
- `ha-ai-factory-server/src/main/java/com/hengaikj/ai/factory/runtime/AgentRuntimeClient.java`：经服务身份调用 Python Runtime。
- `ha-ai-factory-server/src/test/java/com/hengaikj/ai/factory/runtime/`：覆盖权限拒绝、身份 claims 和 Runtime 故障行为。
- `deployment/laya/Dockerfile`、`deployment/laya/compose.yaml`、`deployment/laya/model-manifest.yaml`：构建与运行内部 Laya 服务及锁定模型制品。
- `evidence/laya-offline-evaluation.md`：记录经批准评测集、版本、指标和结果。

## Task 1: Resolve Runtime Contract and Independent Gate

**Files:**
- Modify: `contracts/contract-decisions.md`
- Modify: `contracts/contract-baseline-v1.0.md`
- Modify: `contracts/contract-gate-approval.md`
- Modify: `contracts/api/api-contract.yaml`
- Create: `contracts/api/laya-inference-api.yaml`
- Modify: `contracts/security/security-contract.md`
- Modify: `contracts/laya-runtime-contract-v1.0.md`
- Create: `evidence/laya-runtime-contract-review.md`

**Interfaces:**
- Consumes: 已确认设计 `docs/superpowers/specs/2026-09-23-laya-runtime-integration-design.md`。
- Produces: 独立 Reviewer PASS 的 Runtime 专项 Contract，至少明确模型用途、禁止用途、请求字段、问题模板、模型/权重版本、Runtime 到 Laya 的认证、网络策略、审计字段、错误语义、限流/超时/重试和离线评测门槛。

- [x] **Step 1: 复核入口 Gate 已通过**

Run: `rg -n "Status: PASS|Global decision: PASS|GLOBAL CONTRACT GATE: PASS" contracts/contract-gate-approval.md evidence`
Expected: HEAD 中的 Gate 记录及证据确认 Global Contract Gate PASS。

- [x] **Step 2: 起草 Runtime 专项 Contract 决策**

在决策清单和 API/安全契约中只加入项目负责人批准的用途、字段、服务身份、模板版本、模型版本、审计及失败语义。保持当前 M02 API/数据库范围不变，除非获批 Contract 明确要求并独立审查相应修改。

- [x] **Step 3: 校验契约格式和跨文件一致性**

Run: `python3 -c 'import yaml; yaml.safe_load(open("contracts/api/api-contract.yaml", encoding="utf-8"))'`
Run: `git diff --check`
Expected: YAML 可解析且无空白错误；API、安全和决策清单中的请求字段、状态和拒绝规则一致。

- [x] **Step 4: 提交待独立复核的契约基线**

使用中文提交信息：`文档: 提交Laya Runtime专项契约待审`。推送后以该 commit 为独立 Reviewer 的唯一审查基线。

- [x] **Step 5: 独立 Contract Reviewer 复核并记录决定**

Review: 比较设计规格、决策清单、API Contract 和 Security Contract；核对 HD-002/HD-006 范围变更、身份边界、数据流、审计、模型版本及失败处理。
Expected: Reviewer 的 PASS/RETURN 报告保存并提交至 `evidence/laya-runtime-contract-review.md`。若为 RETURN，按报告修订契约，提交修订并重新审查；专项 Contract PASS 之前不执行 Task 2–7。

- [x] **Step 6: 记录并提交 Contract Gate PASS**

仅在独立 PASS 证据已进入 HEAD 后，将 Gate 状态更新为 PASS 并提交：`文档: 记录Laya Runtime Contract Gate通过`。只暂存本任务明确列出的契约与审查证据文件。

## Task 2: Create Runtime Service and Decision Provider Contracts

**Files:**
- Create: `ha-ai-agent-runtime/pyproject.toml`
- Create: `ha-ai-agent-runtime/app/__init__.py`
- Create: `ha-ai-agent-runtime/app/main.py`
- Create: `ha-ai-agent-runtime/app/config.py`
- Create: `ha-ai-agent-runtime/Dockerfile`
- Create: `ha-ai-agent-runtime/app/decision/__init__.py`
- Create: `ha-ai-agent-runtime/app/decision/models.py`
- Create: `ha-ai-agent-runtime/app/decision/providers/__init__.py`
- Create: `ha-ai-agent-runtime/app/decision/providers/base.py`
- Create: `ha-ai-agent-runtime/tests/unit/test_decision_models.py`
- Create: `ha-ai-agent-runtime/tests/unit/test_runtime_config.py`
- Create: `ha-ai-agent-runtime/tests/unit/test_provider_errors.py`

**Interfaces:**
- Consumes: Task 1 获批的 Laya SystemOne API、请求/响应 schema 和错误码。
- Produces: 严格镜像 Laya wire shape 的 `DecisionRequest`、`DecisionResponse`、稳定安全的 provider error types 和 `DecisionModelProvider.predict(request) -> response`。模板版本和上游发布版本由更高层部署/Runtime 配置管理，不塞入 Laya 请求体；仅接受 `multilingual` checkpoint。
- The FastAPI container disables interactive docs and exposes no business route until the separate Java-to-Runtime API Contract is defined and independently approved.

- [x] **Step 1: 写决策 schema 的拒绝测试**

测试合法批准字段通过；未定义字段、未知标签、不匹配的 score legend/probability 索引、动态 score 越界、checkpoint 不符和缺少 provider 配置均失败关闭。测试字段和边界值直接取自 Task 1 获批 Contract。

Run: `cd ha-ai-agent-runtime && pytest tests/unit/test_decision_models.py -q`
Expected: 初始失败，因为 schema 尚未实现。

- [x] **Step 2: 实现请求与响应模型**

在 `models.py` 中只镜像 Laya API Contract 获批字段；配置 `extra="forbid"`，验证答案 ID、类型、标签、score 范围和 legend/probability 映射。不得增加客户端控制的模型 URL、服务地址、权限字段、模板版本或工具字段。

- [x] **Step 3: 定义 provider 协议和错误模型**

`base.py` 提供以下异步协议，并定义区分认证失败、远端不可用、超时、过载、无效响应和版本不匹配的稳定错误类型；错误不得携带 Secret、原始输入或上游异常 detail。

```python
class DecisionModelProvider(Protocol):
    async def predict(self, request: DecisionRequest) -> DecisionResponse: ...
```

- [x] **Step 4: 运行单元测试并提交**

Run: `cd ha-ai-agent-runtime && python -m pytest tests/unit -q`
Expected: 13 cases PASS；Commit: `新增: 定义Runtime决策Provider契约`。

## Task 3: Add the Java-to-Runtime Trusted Bridge

**Files:**
- Modify: `ha-ai-factory-server/pom.xml` (add Spring Web support required by the approved API)
- Create: `ha-ai-factory-server/src/main/java/com/hengaikj/ai/factory/runtime/AgentDecisionController.java`
- Create: `ha-ai-factory-server/src/main/java/com/hengaikj/ai/factory/runtime/AgentDecisionService.java`
- Create: `ha-ai-factory-server/src/main/java/com/hengaikj/ai/factory/runtime/AgentDecisionAuthorizer.java`
- Create: `ha-ai-factory-server/src/main/java/com/hengaikj/ai/factory/runtime/RuntimeServiceIdentityIssuer.java`
- Create: `ha-ai-factory-server/src/main/java/com/hengaikj/ai/factory/runtime/AgentRuntimeClient.java`
- Create: `ha-ai-factory-server/src/test/java/com/hengaikj/ai/factory/runtime/AgentDecisionServiceTest.java`
- Create: `ha-ai-factory-server/src/test/java/com/hengaikj/ai/factory/runtime/RuntimeServiceIdentityIssuerTest.java`

**Interfaces:**
- Consumes: Task 1 外部 API/服务身份 Contract；符合 HD-001/HD-003 的认证与 RBAC 实现。当前内存骨架仅用于单元测试替身，不作为生产身份授权源。
- Produces: Controller 从认证上下文获取 `CallerContext`；`AgentDecisionAuthorizer.requirePermission(caller, projectId, taskId, permission)` 执行项目/任务级授权；`AgentDecisionService.submit(caller, projectId, taskId, request)` 做权限判定、调用 Runtime 并审计；`RuntimeServiceIdentityIssuer.issue(caller, projectId, taskId)` 产生 audience、subject、issuer、project、task 受限且有效期不超过 10 分钟的服务身份；`AgentRuntimeClient.submit(token, request)` 调用 Python Runtime。

- [ ] **Step 1: 编写权限拒绝和调用身份测试**

覆盖非成员/无权限主体、跨项目 task、停用或无效会话；断言 Runtime Client 未调用。覆盖合法请求时，断言身份 claims 含真实 issuer+subject、project/task、正确 audience，TTL 不超过 10 分钟，且不接受请求体伪造 actor。

Run: `cd ha-ai-factory-server && mvn test -Dtest=AgentDecisionServiceTest,RuntimeServiceIdentityIssuerTest`
Expected: 初始失败，因为 Runtime bridge 尚未创建。

- [ ] **Step 2: 实现受保护入口和短时服务身份**

Controller 只接受获批请求字段，actor 来自服务端认证上下文。Service 调用默认拒绝权限检查；通过后构造 Runtime context，使用由 Task 1 批准的签名密钥配置签发服务身份。

调用边界示意：

```java
var caller = authenticatedCaller.fromSecurityContext();
authorizer.requirePermission(caller, projectId, taskId, "runtime.decision.execute");
var token = serviceIdentityIssuer.issue(caller, projectId, taskId);
return runtimeClient.submit(token, request);
```

- [ ] **Step 3: 实现 Runtime HTTP 客户端和审计**

使用 Spring HTTP Client，固定内部 Runtime base URL，设置 Contract 批准的超时和大小上限；只转发获批字段及服务端上下文。记录通过/拒绝/失败事件，不记录 Token、原始提示或不必要的任务正文。

- [ ] **Step 4: 运行 Java 测试并提交**

Run: `cd ha-ai-factory-server && mvn test -Dtest=AgentDecisionServiceTest,RuntimeServiceIdentityIssuerTest`
Expected: PASS；Commit: `功能: 增加Runtime受保护调用桥接`。

## Task 4: Implement the Isolated Laya HTTP Adapter

**Files:**
- Create: `ha-ai-agent-runtime/app/decision/providers/laya_http.py`
- Create: `ha-ai-agent-runtime/app/security/service_auth.py`
- Create: `ha-ai-agent-runtime/tests/unit/test_laya_http_provider.py`

**Interfaces:**
- Consumes: Task 2 的 provider 协议与获批请求/响应模型。
- Produces: `LayaHttpProvider(base_url, auth, timeout).predict(request)`；基础 URL 只能来自受保护的服务配置，不从请求体读取。

- [ ] **Step 1: 编写成功、错误和鉴权测试**

使用 `httpx.MockTransport` 覆盖合法类型化答案、403、5xx、连接超时、错误 JSON、未知标签、错误模型版本及无效 usage；断言 Secret 不进入异常文本和日志。

Run: `cd ha-ai-agent-runtime && pytest tests/unit/test_laya_http_provider.py -q`
Expected: 初始失败，因为 Provider 尚未实现。

- [ ] **Step 2: 实现服务认证注入**

`service_auth.py` 提供获批机制的异步认证器协议。Provider 每个请求获取短时授权头或使用获批 mTLS 客户端；不写入静态明文令牌，不将认证值加入 URL 或日志。证书/Token 的具体取值仅按 Task 1 Contract 和 Secret 管理配置读取。

- [ ] **Step 3: 实现 HTTPX Provider**

使用单例异步 HTTPX client、Contract 配置的超时和连接池边界，向配置的 Laya endpoint 发出 POST。仅发送获批 schema 字段；严格验证 status、content type、响应 schema、允许标签和部署模型版本；把远端失败映射到 `DecisionProviderError` 子类。调用结构限定为：

```python
response = await client.post(
    f"{settings.laya_base_url}/v1/systemone",
    json=request.to_laya_payload(),
    auth=await auth_provider.current_auth(),
)
response.raise_for_status()
return DecisionResponse.from_laya_payload(response.json())
```

- [ ] **Step 4: 运行 Provider 单元测试并提交**

Run: `cd ha-ai-agent-runtime && pytest tests/unit/test_laya_http_provider.py -q`
Expected: PASS；Commit: `功能: 增加Laya推理HTTP适配器`。

## Task 5: Add Runtime Policy Checks and Fail-Closed Decision Flow

**Files:**
- Create: `ha-ai-agent-runtime/app/decision/service.py`
- Create: `ha-ai-agent-runtime/app/api/__init__.py`
- Create: `ha-ai-agent-runtime/app/api/routes/__init__.py`
- Create: `ha-ai-agent-runtime/app/api/routes/agent_runs.py`
- Modify: `ha-ai-agent-runtime/app/main.py`
- Create: `ha-ai-agent-runtime/tests/unit/test_decision_service.py`
- Create: `ha-ai-agent-runtime/tests/integration/test_agent_decision_route.py`

**Interfaces:**
- Consumes: Task 1 的获批路由/权限规则；Task 2 schema；Task 3 Provider。
- Produces: `DecisionService.execute(context, request) -> DecisionResponse` 和仅符合获批 API Contract 的 FastAPI route。

- [ ] **Step 1: 写配置拒绝、项目边界与失败关闭测试**

至少覆盖：未批准 Runtime 配置不调用 provider；项目或任务不匹配返回批准错误码；已拒绝或过期配置不调用 provider；含“忽略规则”等注入文本只作为输入数据且不会引发越权动作；provider timeout、认证失败、无效响应变成失败/待人工状态；所有失败下游副作用调用数为 0。

Run: `cd ha-ai-agent-runtime && pytest tests/unit/test_decision_service.py tests/integration/test_agent_decision_route.py -q`
Expected: 初始失败，因为服务与路由尚未实现。

- [ ] **Step 2: 实现权限前置和状态机**

`DecisionService` 在调用 provider 前验证后端签名的服务身份、原始发起人、项目/任务绑定、配置批准/有效期、问题模板 allowlist 和请求限额。任一项失败立即返回 Contract 定义错误，不发 Laya 请求。成功响应还需通过 Task 4 的 schema 校验。

- [ ] **Step 3: 实现批准路由与不可信输入处理**

路由只声明获批 API 方法和路径；校验身份后调用 `DecisionService`。用户内容按数据处理，不执行内容中的指令；将模型输出标记为 advisory。路由内不添加 Gate approve、角色修改或工具执行路径。

- [ ] **Step 4: 运行 Runtime 测试并提交**

Run: `cd ha-ai-agent-runtime && pytest tests/unit/test_decision_service.py tests/integration/test_agent_decision_route.py -q`
Expected: PASS；Commit: `功能: 增加Laya决策流程失败关闭校验`。

## Task 6: Package Laya as an Internal-Only Service

**Files:**
- Create: `deployment/laya/Dockerfile`
- Create: `deployment/laya/requirements.lock`
- Create: `deployment/laya/compose.yaml`
- Create: `deployment/laya/model-manifest.yaml`
- Create: `deployment/laya/tests/test_deployment_config.py`
- Create: `ha-ai-agent-runtime/tests/integration/test_laya_container.py`

**Interfaces:**
- Consumes: Task 1 核准的 Laya 版本、checkpoint、权重摘要、内部地址和服务认证配置。
- Produces: 可复现构建的 Laya 服务镜像；仅 Agent Runtime 所在内部网络可连接；模型制品与运行镜像版本可追溯。

- [ ] **Step 1: 锁定软件与模型制品**

使用 Contract 批准的 Laya release 精确版本和模型 revision，在 `deployment/laya/requirements.lock` 与 `model-manifest.yaml` 中记录版本、revision、SHA-256、许可证及来源。构建阶段将模型置入受控制品；运行镜像关闭 Hugging Face 下载回退和公网 egress。

- [ ] **Step 2: 写部署配置断言**

`test_deployment_config.py` 验证 Laya 没有公网 `ports` 映射、仅加入专属内部网络、没有挂载 Factory 数据库/代码仓库、没有明文 Secret、镜像和权重均按摘要固定。

Run: `python3 -m pytest deployment/laya/tests/test_deployment_config.py -q`
Expected: 初始失败，直到内部网络与固定制品配置完成。

- [ ] **Step 3: 创建内部镜像和 Compose 配置**

配置 Laya 私网服务、批准的设备与模型 preload 参数、健康检查和 Secret 注入。若使用 `laya-serve`，以内部 DNS 名供 Runtime 调用；宿主机不发布服务端口。按获批 Contract 配置内部认证边界；若使用 Laya 内置 API Key，只能作为网络/工作负载身份控制之外的附加防护，从 Secret 管理服务注入并轮换，不能单独代表调用身份。Compose 网络至少保持：

```yaml
services:
  laya:
    expose: ["8000"]
    networks: [inference]
  agent-runtime:
    networks: [inference]
networks:
  inference:
    internal: true
```

- [ ] **Step 4: 运行配置和容器集成验证**

Run: `docker compose -f deployment/laya/compose.yaml config`
Run: `docker compose -f deployment/laya/compose.yaml up -d --build`
Run: `cd ha-ai-agent-runtime && pytest tests/integration/test_laya_container.py -q`
Expected: Compose 配置有效；Runtime 可在内网完成获批的 typed-decision 请求；宿主机外部网络不可访问 Laya；服务故障时 Runtime 失败关闭。

- [ ] **Step 5: 提交部署工件**

Commit: `部署: 增加内网Laya推理服务`。部署变更在独立部署 Reviewer 批准前不得进入 Release。

## Task 7: Audit, Privacy, Evaluation and Operational Handoff

**Files:**
- Modify: `ha-ai-agent-runtime/app/decision/service.py`
- Modify: `ha-ai-agent-runtime/tests/unit/test_decision_service.py`
- Create: `ha-ai-agent-runtime/tests/security/test_decision_data_handling.py`
- Create: `evidence/laya-offline-evaluation.md`
- Create: `docs/operation/laya-runtime-operations.md`

**Interfaces:**
- Consumes: Task 1 批准的审计、隐私、错误、资源和评测门槛。
- Produces: 可审计且最小化的数据记录、经批准的离线评测证据、部署/回滚/人工接管操作说明。

- [ ] **Step 1: 写日志和审计隐私测试**

构造含合成敏感字段的决策请求，运行成功和失败流程，断言日志不含原始正文、完整 prompt、Cookie、Bearer Token 或 Secret；审计事件包含获批的项目/任务、模板版本、模型版本/摘要、结果状态、错误码和时间。

Run: `cd ha-ai-agent-runtime && pytest tests/security/test_decision_data_handling.py -q`
Expected: 初始失败，直到日志脱敏和审计字段实现。

- [ ] **Step 2: 实现审计和数据最小化**

只持久化 Contract 明确批准的元数据和最小结果摘要；在日志格式层移除敏感字段；将模型版本/权重摘要取自部署 manifest，而不是信任请求方字段。

- [ ] **Step 3: 完成离线质量评测**

使用负责人批准的合成或脱敏中英样本，按批准语言、任务类型和风险类别分别计算准确率、Brier/ECE、拒绝/人工升级表现和高基数标签表现。将 checkpoint 摘要、数据版本、运行命令、指标和适用限制写入 `evidence/laya-offline-evaluation.md`；未达到获批门槛时禁止启用调用。

- [ ] **Step 4: 编写运行与回滚手册**

记录内部服务启动/健康检查、版本核验、证书或短时凭据轮换、常见失败诊断、关闭 Runtime 配置、停止 Laya 路由及人工接管步骤。不得记录生产 Secret。

- [ ] **Step 5: 运行质量门并提交证据**

Run: `cd ha-ai-agent-runtime && pytest -q`
Run: `docker compose -f deployment/laya/compose.yaml config`
Run: `git diff --check`
Expected: 所有 Runtime 测试通过，部署配置与获批 Contract 一致，隐私和失败关闭证据完整；Commit: `文档: 完成Laya离线评测与运行交接`。

## Plan Self-review

- 设计规格第 3–6 节的组件边界、数据流、认证、错误和恢复分别映射到 Task 2–6。
- 设计规格第 8 节的制品锁定、许可证、离线质量和部署门槛映射到 Task 1、Task 6、Task 7。
- Review Focus 五项分别由决策 schema、Java 项目授权、Provider/Runtime 故障、数据处理和模型版本/标签校验测试覆盖。
- 全局 Contract Gate 当前 RETURN，因此计划包含明确 Entry Gate，未通过前无实现任务可执行。
- 对照文件清单、任务接口和失败模式检查后，运行文本完整性扫描及 `git diff --check` 未发现待补模板内容或空白错误。

## Final Review Gate

- 由独立 Reviewer 对 Runtime 代码、部署工件、契约符合性、数据隐私、失败关闭和离线评测证据进行 Review。
- 只有 Review PASS 后才可将该集成纳入 Integration 阶段；Production/Release 还需各自 Release Gate 和部署配置审批。
- 任一 Gate 为 RETURN/PENDING 时，保留人工流程，不启用 Laya 流量，不进入后续阶段。

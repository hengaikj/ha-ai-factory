# Laya Runtime 专项 Contract v1.0

项目：HA AI Software Factory  
状态：独立 Contract Gate PASS（2026-09-23）；生产启用条件见第 9 节
关联设计：[Laya Runtime 集成设计](../docs/superpowers/specs/2026-09-23-laya-runtime-integration-design.md)
上游协议参考：[Laya Self-Hosting HTTP Server](https://github.com/NandhaKishorM/laya#self-hosting-http-server-jev-compatible)、[laya/serve.py](https://github.com/NandhaKishorM/laya/blob/main/laya/serve.py)

## 1. 范围与边界

本 Contract 仅定义未来 Agent Runtime 调用独立内网 Laya 推理服务的边界。它不授权当前 M02 启用真实 Agent Runtime，不授权工具执行、外部副作用、仓库内容读取、Git/CI 集成或自动 Gate 决策。

Java 后端仍是用户身份、项目 RBAC 和任务授权的权威来源。Agent Runtime 负责调用前置校验、请求模板、结果校验和失败关闭。Laya 只返回类型化推理结果，不访问 Factory 数据库、项目文件或凭据。

## 2. 允许用途与禁止用途

允许用途仅限经审批的内部决策辅助任务，例如对已授权任务输入执行 `choice`、`score` 或 `noul` 类型推理，并将结果送人工复核流程。

禁止用途：

- 作为用户认证、项目授权、Reviewer 身份或 Gate 决策主体；
- 直接修改项目、成员、权限、交付物或审计记录；
- 直接执行工具、Git、CI、通知、部署或其他外部副作用；
- 接收客户端指定的模型地址、模型版本、问题模板、标签集合或服务凭据；
- 处理未经过项目和任务授权的跨项目数据。

## 3. 调用身份与网络

1. 用户请求必须先经过 Java 后端 OIDC 会话和项目/任务级 RBAC 校验。
2. Java 后端向 Runtime 传递受限服务身份，身份包含 issuer、subject、project、task、audience 和签发时间；有效期不得超过 HD-001 已批准的 10 分钟。
3. Runtime 仅接受来自受信任 HA 后端的服务身份，并校验项目、任务、配置状态和允许能力。
4. Runtime 到 Laya 只允许内部网络访问；浏览器、公网和其他项目工作负载不得访问 Laya 推理入口。
5. 生产服务认证采用 Runtime 工作负载 mTLS；只允许受信任 Runtime 工作负载经内部网络访问 Laya。mTLS 在受控推理服务网关终止；网关严格校验 `SystemOneRequest` 并拒绝未定义字段，包括 Runtime 传来的 `model`，随后固定向 Laya 注入 `model=multilingual`。只有网关能连接 Laya 进程。Laya 配置 `LAYA_AUTO_TASK=0`，只预加载 `multilingual`。网关到 Laya 只走同一受限服务网络。证书由受控环境签发、托管和轮换。Laya 自带的静态 Bearer key 可作为额外校验，但不能替代 mTLS 或成为唯一生产身份。

## 4. 请求契约

Runtime 只发送服务端维护模板生成的请求。客户端不得覆盖下列字段：

| 字段 | 约束 |
| --- | --- |
| `state` | 仅含已脱敏的最小任务摘要，字符串长度不超过 4,000 字符；不得包含项目/用户标识、凭据或完整任务正文 |
| `questions` | 每次 1 至 8 个服务端批准的问题；question ID 与模板版本绑定 |
| `question.type` | `choice`、`score` 或 `noul`，由已批准模板固定 |
| `question.instructions` | 服务端维护的问题指令，每项不超过 1,000 字符；调用者不可覆盖 |
| `question.criteria` | `choice` 为 2 至 32 个标签映射，rubric 每项不超过 256 字符；`score` 为 2 至 10 个有序级别，每项不超过 128 字符；`noul` 可省略 |
| `model` | Runtime 请求体不得包含；网关固定向 Laya 注入 `multilingual`，调用方不可指定其他模型 |
| `requestId` | Runtime 生成并写入内部审计的关联 ID；不得放入发送给 Laya 的 state 内容 |

原始任务正文、凭据、系统提示、任意 Python 代码、工具参数和未脱敏敏感字段不得发送至 Laya。Runtime 到 Laya 的 wire schema、字段上限和选择规则详见 [`laya-inference-api.yaml`](api/laya-inference-api.yaml)。

多语言 checkpoint 的输入预算按锁定模型 tokenizer 对每个问题分别执行：每个问题的上下文上限 1,024 tokens，其中该问题 options 上限 256 tokens、state 上限 768 tokens。调用前由同版本 Laya 服务包装层使用 checkpoint tokenizer 计算完整序列预算；超过预算返回 422 `INPUT_TOO_LONG`，不得静默截断或继续推理。若上游实现会截断输入，部署包装层必须在截断前检测并拒绝。

## 5. 响应契约

Runtime 必须校验响应 schema、请求关联、模型 allowlist、模板版本、结果类型和允许值。由于上游 Laya 不回显 `X-Request-ID`，受控网关必须将 Runtime 请求 ID 与该次上游 HTTP 交换作一对一关联，并在网关响应头原样返回 `X-Request-ID`；Runtime 校验匹配后才接受响应。未知标签、版本不匹配、缺失字段、无效 JSON 或低于批准阈值的结果均标记为 `FAILED_CLOSED` 并转人工处理。Laya 响应的 `routing.model` 必须为 `multilingual`；模型代码 revision、checkpoint 和权重摘要由 Runtime 从只读部署清单关联，不假设 Laya 响应提供这些摘要。

上游 answer 结构包含与请求问题类型一致的 `type`、类型字段、必需的 `action.act_probability`；choice/score 答案还包含 `confidence` 与 `probabilities`，score 答案额外包含 `legend`，noul 答案包含 `confidence`。`score` 是按有序 criteria 计算的零起始 ordinal 期望值（`sum(i * p_i)`），不是某个离散等级；对 k 个 criteria，Runtime 必须校验 `0 <= score <= k-1`，并要求 `legend` 与 `probabilities` 的键恰为字符串索引 `0..k-1`，每个 legend 值与请求中同索引 criterion 完全一致。Runtime 必须按专项 API schema 校验这些字段，不得放行未知字段；`action.act_probability` 仅为模型输出元数据，不授权执行动作。

成功响应只能表示“模型推理完成”，不得表示权限批准、Gate 通过或业务动作已执行。响应至少包含：

- Runtime `requestId`（由调用关联，不作为 Laya 推理结果字段）；
- `questionType`；
- 类型化结果（选择标签、分数或 `noul`）；
- 模型版本和权重摘要；
- 模板版本；
- 推理时间与最小用量信息；
- 可供审计关联的错误码或状态。

## 6. 错误、超时和恢复

以下情况必须失败关闭，不得静默切换模型或执行后续副作用：配置缺失、配置未批准、身份无效、跨项目任务、权限不足、Laya 不可用、认证失败、超时、限流、响应校验失败、模型/模板版本不匹配和低可信结果。

Runtime 外部错误响应只返回稳定错误码和下一步操作提示，不返回密钥、完整提示词或其他项目数据。网关必须将上游错误映射为契约错误码；上游任意 `detail`（包括异常文本）不得透传，原始异常只允许写入经脱敏且访问受控的运维日志。响应必须使用 API schema 的 `code` 和静态安全提示 `detail` 字段；映射至少包括 `INVALID_REQUEST`、`INPUT_TOO_LONG`、`AUTHENTICATION_FAILED`、`PROVIDER_UNAVAILABLE`、`PROVIDER_OVERLOADED`、`MODEL_NOT_READY` 和 `INTERNAL_PROVIDER_ERROR`。上游异常、验证器消息和请求内容不得拼接进外部错误文案。

连接超时为 2 秒、单次尝试总超时为 5 秒；最多一次重试，仅限连接失败、超时或 HTTP 503，并复用相同请求体及 `X-Request-ID`，等待 200 毫秒后重试；总操作时限为 10.2 秒。4xx、认证失败、模型校验错误和响应错误不重试。推理没有业务副作用。并发上限在内网网关实施：每实例只准一个上游推理请求在途、等待队列为零；饱和时网关必须在调用 Laya 前立即返回 `PROVIDER_OVERLOADED`，不得依赖上游 asyncio 锁排队，也不得无限排队。

## 7. 审计与数据留存

审计至少记录：项目、任务、请求 ID、调用主体摘要、模板版本、模型版本、权重摘要、结果类别、状态、错误码、时间和人工升级标记。普通日志不得记录原始任务正文、完整提示词、凭据或敏感字段。

正式审计留存、删除、备份和恢复边界属于 HD-005 运维 Contract；本 Contract 不作超出既有安全结构约束的 SLA 或合规承诺。

## 8. 模型与供应链

部署必须锁定 Laya 代码 revision、模型 checkpoint、权重摘要、Python 依赖和许可证信息。生产运行时不得从公网动态下载模型。上游 benchmark 不替代 HA Factory 自有中文和实际业务评测；上线前必须有负责人批准的离线评测集、指标、校准/拒答阈值和人工升级规则。

## 9. 已确认决策与剩余上线条件

- 已确认用途：首期仅允许内部决策辅助；禁止权限、Gate、工具、Git、CI、通知和其他外部副作用。
- 已确认服务身份：生产使用 Runtime 工作负载 mTLS；证书由受控环境管理和轮换；静态 API key 不能作为唯一认证。
- 已确认请求限制：最小脱敏 state 不超过 4,000 字符；1 至 8 个问题；choice 标签 2 至 32 个、每项 rubric 不超过 256 字符，score 级别 2 至 10、每项不超过 128 字符；问题指令每项不超过 1,000 字符；普通日志不保留原文。
- 已确认韧性策略：连接超时 2 秒、单次调用总超时 5 秒；仅连接失败、超时或 HTTP 503 可重试一次；4xx、认证失败、版本/响应错误不重试；并发或排队超限失败关闭。
- 已确认供应链边界：模型代码 revision、checkpoint、权重摘要、Python 依赖和许可证必须在部署清单锁定；禁止公网动态下载。
- 首期 checkpoint 范围：固定采用 `multilingual` checkpoint（上游 standalone 模型 `convaiinnovations/laya-multilingual`），由网关注入，不开放模型选择；`english` 和 `typed-decisions` 禁用。模型 revision 和权重摘要仍须锁定后才能生产启用。
- 已确认范围：M02 真实 Runtime 继续禁用；专项 Gate PASS 后，才可在后续模块启用本 Contract 定义的 Laya 决策辅助能力。
- 剩余生产上线条件：锁定实际 Laya release/checkpoint revision 与权重摘要；完成许可审查、中文业务离线评测，并由负责人确认准确率、校准、拒答和人工升级阈值；未达标保持禁用。该条件不阻止契约审查通过或模拟/离线集成开发。

## 10. Gate 条件

API 与安全契约已对齐；独立 Reviewer 于 2026-09-23 记录 PASS，证据为 [`laya-runtime-contract-review.md`](../evidence/laya-runtime-contract-review.md)，并已提交到 HEAD。Runtime 实现可按本 Contract 开始。M02 真实 Runtime 执行仍保持禁用；生产模型调用还要求模型制品锁定、许可审查和离线评测门槛满足第 9 节。

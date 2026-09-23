# Contract 阶段人工决策清单

状态：`HUMAN_DECISION_REQUIRED`；所有契约草案均未获批准。
依据：`docs/requirement/requirement-baseline.md`、`docs/product/prd.md`、`docs/design/ux-ui-spec.md`、`contracts/api/api-contract.yaml`、`contracts/database/schema-draft.sql`、`contracts/security/security-contract.md`

负责人确认后，应在本文件记录决定人、决定日期、选择及适用范围，再同步更新 API、数据库和安全契约。未确认前不开始相关接口、迁移或 Runtime 执行实现。

| 决策 | 关联事项 | 需负责人确认的具体内容 | 当前影响 |
| --- | --- | --- | --- |
| HD-001 身份与部署 | OI-002 | 首版部署环境/网络边界；身份提供方；认证方式（例如现有企业身份系统、托管身份服务或本地账号）；API/浏览器/Runtime 的身份传递与 Token 生命周期。 | API 身份绑定及认证安全契约未批准。 |
| HD-002 Agent Runtime 授权 | OI-003 | 首版允许的模型提供方/模型；每种工具及可执行操作；哪些操作有外部副作用；谁批准/撤销运行配置；配置有效期和密钥来源。 | Runtime 真实执行必须保持关闭；不得填入凭据或执行模型/工具调用。 |
| HD-003 权限与角色边界 | OI-004 | 项目、任务、交付物、Gate、Open Issue、资源和 Runtime 的角色-动作矩阵；项目成员如何加入；负责人、Orchestrator、Reviewer、管理员可否兼任；如何保证 Reviewer 独立及禁止自评/自批。 | 最终授权矩阵和 Reviewer 冲突校验未批准。 |
| HD-004 交付物权威来源 | OI-005 | 权威来源选仓库文件、系统内编辑内容或两者共存；版本/冲突规则；查看/下载方式；证据引用失效处理与保留期限。 | `sourceRef` 仅是抽象引用，不能据此确定文件存储或下载实现。 |
| HD-005 定量非功能与保留 | OI-007 | 可接受的并发/响应目标、文件与请求大小上限、超时与限流要求、审计/数据保留期、备份和恢复目标。若首版不设具体指标，请明确接受默认运营边界及其责任人。 | 不对性能、容量、保留/备份合规作承诺。 |
| HD-006 外部集成范围 | OI-006 | 首版是否需要集成代码托管、构建/测试、通知或其他服务；若需要，列明首发服务和凭据责任。Agent 模型提供方另由 HD-002 确认。 | 当前 API 草案不定义上述集成。 |

## 不属于本次阻塞的后续事项

- OI-008：Requirement/PRD 已将组织级多租户、外部协作者和客户项目视图排除在 MVP；若范围改变需重新开需求决策并评估隔离契约。
- OI-009：发布批准人、部署、回滚及 Release 证据在 Release 阶段决策，本 Contract 草案不定义生产发布契约。

## 当前门禁判断

HD-001 至 HD-004 影响本 MVP 核心 API、安全或数据行为；HD-005 需由负责人确认首版验收边界；HD-006 如确认不属于 MVP，可明确延期。以上决定未记录前，`Contract Gate` 保持 `pending`，不得进入 Development 或修改业务接口/数据库。

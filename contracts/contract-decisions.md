# Contract 阶段人工决策清单

状态：M02 范围决策已确认；全局 Contract Gate 待独立复审。HD-001、HD-002、HD-003、HD-004、HD-005、HD-006 已按项目负责人决定记录；Agent Runtime、系统内交付物编辑、正式 SLA/备份承诺和外部集成均延期，不属于当前 M02 实施范围。
依据：`docs/requirement/requirement-baseline.md`、`docs/product/prd.md`、`docs/design/ux-ui-spec.md`、`contracts/api/api-contract.yaml`、`contracts/database/schema-draft.sql`、`contracts/security/security-contract.md`

项目负责人于 2026-09-23 在当前会话确认 HD-001、HD-003 的下述选择，并确认 HD-002、HD-004、HD-005、HD-006 按本表范围延期。该确认授权同步更新 API、数据库和安全契约草案，不代表 Contract Gate 已通过，也不授权业务接口或迁移实施。

| 决策 | 关联事项 | 需负责人确认的具体内容 | 当前影响 |
| --- | --- | --- | --- |
| HD-001 身份与部署 | OI-002 | 已确认：人类用户通过企业 OIDC 身份提供方登录；身份键为 `(issuer, subject)`；不提供本地密码兜底。浏览器使用 OIDC Authorization Code + PKCE；Web 后端维护服务器端会话，以 Secure、HttpOnly、SameSite Cookie 传递会话，空闲 30 分钟或最长 8 小时失效。具体 IdP/issuer 由部署配置提供。Runtime 使用独立服务身份，只接受后端签发、audience 限定且最长 10 分钟有效的调用身份，并记录原始用户 issuer+subject。 | 身份流、会话时限和 Runtime 调用身份 TTL 已确认；具体 IdP/issuer、部署网络边界与密钥托管属于环境配置，部署前需确认。 |
| HD-002 Agent Runtime 授权 | OI-003 | 已确认延期：M02 不启用真实 Agent Runtime，不配置模型、工具、外部副作用、凭据或执行批准策略；仅保留配置状态与失败关闭接口。 | Runtime 执行能力延期到独立 Contract；任何真实模型/工具调用均禁止。 |
| HD-003 权限与角色边界 | OI-004 | 已确认：采用显式项目成员资格 + 项目级 RBAC，角色为 Owner、Project Admin、Orchestrator、Engineer、Reviewer；允许角色组合。Owner 管项目及成员；Project Admin 管成员及项目设置；Orchestrator 管任务分配和流程状态但无审批权；Engineer 处理分配任务并提交交付物；Reviewer 评审交付物、Gate 与 Issue。Owner/Admin 负责成员加入和移除；每项目至少保留一名 Owner；移除后立即失权。Agent 只能按获批 Runtime 配置执行，无评审或审批权。Reviewer 必须具 Reviewer 角色，并对具体评审对象检查独立性；对象提交者、任务执行者及该对象决策责任人不得评审该对象。Gate 独立性冲突主体在提交时由服务端按 Gate 提交人、决策责任人、范围任务责任主体和范围交付物提交人生成快照，每项 Gate Check 与最终决策均据此校验。 | 项目/成员/Gate 的实现边界已明确；资源、审计数据读取及 Runtime 启动权限须在相应模块契约中沿用最小权限/默认拒绝并完成独立复审。 |
| HD-004 交付物权威来源 | OI-005 | 已确认延期范围：M02 仅登记仓库文件引用、版本和内容摘要；仓库文件是权威来源；暂不支持系统内编辑、文件托管、冲突合并或公开下载 URL。 | `sourceRef` 仅允许表达仓库引用；完整文件存储、冲突和下载策略延期。 |
| HD-005 定量非功能与保留 | OI-007 | 已确认延期范围：M02 接受默认运营边界，不作并发、响应、容量、SLA、备份 RPO/RTO 或合规保留承诺；仅遵守已批准的安全和审计结构约束。 | 正式 NFR、审计保留、备份恢复和限流指标延期到运维 Contract。 |
| HD-006 外部集成范围 | OI-006 | 已确认延期：M02 不接入 Git、CI、测试编排、通知或其他外部服务，仅保留未来扩展边界。 | 当前 Contract 不承诺外部集成，也不配置第三方凭据。 |

## 不属于本次阻塞的后续事项

- OI-008：Requirement/PRD 已将组织级多租户、外部协作者和客户项目视图排除在 MVP；若范围改变需重新开需求决策并评估隔离契约。
- OI-009：发布批准人、部署、回滚及 Release 证据在 Release 阶段决策，本 Contract 草案不定义生产发布契约。

## 当前门禁判断

HD-001 至 HD-006 的当前范围已同步到 API、数据库和安全草案；M02 scoped Contract Review 结果为 `PASS`（见 `evidence/contract-review-M02-2026-09-23.md`）。部署网络边界、issuer、密钥托管和轮换仍需在环境契约中确认。全局 Contract Gate 等待独立复审和批准；M02 Project/Member/Gate 仍须满足适用 Design Gate、基线提交和独立复审条件。

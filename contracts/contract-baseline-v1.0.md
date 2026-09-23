# HA AI Software Factory Contract Baseline v1.0

Status: M02_SCOPED_PASS_GLOBAL_PASS

Scope: M02 Project / Member / Gate contract alignment and explicit deferral of HD-002/HD-004/HD-005/HD-006. This is not a Contract Gate approval.

## Confirmed decisions
- HD-001: 已确认企业 OIDC 人类身份（issuer+subject）、Authorization Code + PKCE、服务器端会话（Secure/HttpOnly/SameSite Cookie，空闲 30 分钟、绝对 8 小时）、无本地密码兜底；Runtime 使用 audience 限定且最长 10 分钟的服务身份并保留发起人上下文。IdP/issuer、网络部署边界和服务密钥由部署配置确定。
- HD-003: 已确认显式项目成员 + 项目级 RBAC；Owner、Project Admin、Orchestrator、Engineer、Reviewer 可组合；成员移除立即失权且每项目保留至少一个 Owner；Reviewer 按对象校验独立性，提交者/任务执行者/该对象决策责任人不得评审；Agent 不审批。

## Explicitly deferred decisions
- HD-002: M02 不启用真实 Agent Runtime，不配置模型、工具、凭据或外部副作用；仅保留状态查询和失败关闭接口。
- HD-004: M02 以仓库文件为权威来源，仅登记引用、版本和摘要；系统内编辑、文件托管、冲突合并和公开下载延期。
- HD-005: M02 接受默认运营边界，不承诺正式 SLA、容量、审计保留、备份 RPO/RTO 或合规指标。
- HD-006: M02 不接入 Git、CI、测试编排、通知或其他外部服务。

独立 Reviewer 已于 2026-09-23 对 M02 Project / Member / Gate 范围给出 scoped PASS，详见 `evidence/contract-review-M02-2026-09-23.md`；Global Contract Gate 于 2026-09-23 独立复审 PASS，详见 `evidence/contract-review-global-rereview-2026-09-23.md`。HD-002、HD-004、HD-005、HD-006 的 M02 延期继续有效。Laya Runtime 后续能力由单独的 `laya-runtime-contract-v1.0.md` 管理，专项 Contract Gate 于 2026-09-23 独立复审 PASS，证据见 `evidence/laya-runtime-contract-review.md`；生产启用仍受该专项 Contract 的制品、许可与离线评测条件约束。

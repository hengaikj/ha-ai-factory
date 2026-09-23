# HA AI Software Factory 项目交接

更新日期：2026-09-23
仓库：[hengaikj/ha-ai-factory](https://github.com/hengaikj/ha-ai-factory)（本地分支 `master`，远程 `origin/master`）

## 当前状态

项目已完成 Requirement 与 Design Gate，目前处于 Contract 阶段。API、数据库和安全契约已有草案，但负责人决策尚未完成，因此 Contract Gate 保持 `pending`。当前没有前端/后端业务代码、数据库迁移或 API 实现；契约草案不得作为已批准接口或执行依据。

| 阶段 / Gate | 状态 | 说明 |
| --- | --- | --- |
| Requirement Gate | 已批准 | 多项目 MVP、Agent Runtime 纳入 MVP、AT-01 至 AT-08 已确认。 |
| Design Gate | 已批准 | 独立 Designer/Reviewer 复审并关闭 D1、D2。 |
| Contract Gate | 待处理 | 等待 HD-001 至 HD-005 决策，并确认 HD-006 集成范围。 |
| Development / Integration / Release | 未开始 | Contract Gate 批准前不进入依赖它的开发阶段。 |

`.agent/state.yaml` 当前记录：`current_phase: Contract`、`next_gate: Contract Gate`、`active_task: CONTRACT-HA-F00-001`。

## 已完成交付物

- [需求基线](requirement/requirement-baseline.md)：Requirement Gate 已批准，MVP 支持多个并行项目和 Agent Runtime。
- [产品需求文档](product/prd.md)：P-01 至 P-08 及 US-01 至 US-08 映射到需求验收场景。
- [UX/UI 规格](design/ux-ui-spec.md)：包含项目流程、状态设计、AT-02 至 AT-07 操作步骤及 Runtime 授权拒绝流程。
- [Design Handoff](design/design-handoff.md)：承载页面、组件和数据关系；API 具体契约留待 Contract 阶段。
- [Design Gate 首轮记录](../evidence/design-review-DESIGN-HANDOFF-HA-F00-001.md)与[复审记录](../evidence/design-review-DESIGN-HANDOFF-HA-F00-001-round-2.md)：复审决定批准 Design Gate，D1、D2 已关闭。
- [API 契约草案](../contracts/api/api-contract.yaml)、[数据库草案](../contracts/database/schema-draft.sql)和[安全契约草案](../contracts/security/security-contract.md)：均未获 Contract Gate 批准。
- [Contract 人工决策清单](../contracts/contract-decisions.md)与[Contract 初审记录](../evidence/contract-review-CONTRACT-HA-F00-001.md)：列出阻塞契约批准的决策。

## Contract Gate 前需负责人决定

请在[人工决策清单](../contracts/contract-decisions.md)记录决策人、日期和适用范围；不需提供密钥或凭据。

| 编号 | 需要决定 | 未决定时的影响 |
| --- | --- | --- |
| HD-001 / OI-002 | 首版部署与网络边界、身份提供方、认证机制和 Token 生命周期。 | API 身份绑定及认证契约无法批准。 |
| HD-002 / OI-003 | Agent Runtime 可用模型/工具/操作、副作用范围、配置批准/撤销角色、有效期和密钥来源。 | Runtime 必须保持关闭，不得发起真实模型或工具调用。 |
| HD-003 / OI-004 | 角色-动作权限矩阵、项目成员管理及角色兼任规则。 | 最终授权矩阵与 Reviewer 冲突校验无法批准；须保证独立审查和禁止自评/自批。 |
| HD-004 / OI-005 | 交付物权威来源、版本/冲突规则、查看下载方式和证据保留策略。 | `sourceRef` 只是抽象引用，文件存储和下载契约未定。 |
| HD-005 / OI-007 | 首版性能/容量目标、请求与文件限制、超时、审计保留、备份/恢复边界；或明确接受的运营默认值。 | 不作性能、保留或备份合规承诺。 |
| HD-006 / OI-006 | 代码托管、构建/测试、通知等外部集成是否纳入 MVP；若纳入，列出首发服务。 | 当前草案不承诺这些集成。 |

OI-008 的组织级多租户、外部协作者和客户视图已列为非 MVP；OI-009 的发布流程在 Release 阶段决定。

## 接手后的下一步

1. 由项目负责人/相关决策角色完成 HD-001 至 HD-006，明确哪些能力纳入 MVP、哪些延期。
2. Contract Agent 按决定更新 API、数据库和安全契约；对任何未决契约继续标记 `HUMAN_DECISION_REQUIRED`。
3. Contract 初审确认 API、数据和安全契约互相一致，所有数据库表及字段保留中文 `COMMENT`。
4. 交由独立 Contract Reviewer 作最终 Gate 决定。仅在 Gate 获批后进入 Development；此前不得修改业务接口、执行数据库草案或启用 Agent Runtime。

## 验证与工作区

- 文档基线提交：`b46fec1`（`文档: 完成Design Gate复审与Contract草案`）。
- `git diff --check` 已通过；本次交接文档提交前会再次检查。
- 构建和测试未运行；仓库目前没有业务代码实现。
- `.DS_Store` 与 `docs/.DS_Store` 是本地系统文件，不属于项目交付物。

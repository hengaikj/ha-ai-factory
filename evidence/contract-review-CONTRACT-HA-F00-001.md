# Contract Gate 初审记录

文档编号：CONTRACT-HA-F00-001
审查对象：`contracts/api/api-contract.yaml`、`contracts/database/schema-draft.sql`、`contracts/security/security-contract.md`、`contracts/contract-decisions.md`
审查基准：已批准 PRD、UX/UI 规格、Design Handoff、Requirement Baseline、`.agent/rules.md`、`docs/quality-gate.md`
审查角色：Orchestrator 初审
结论：**Contract Gate 未批准；等待 `HUMAN_DECISION_REQUIRED` 决策。**

## 检查项

| 检查项 | 结果 | 说明 |
| --- | --- | --- |
| API 覆盖 | 草案已覆盖 | API 草案覆盖项目、任务、交付物评审、Gate、阶段推进、Open Issues、模板规则、审计和 Agent Runtime 状态/执行入口。身份与角色授权未确定，不能实施。 |
| 数据模型 | 草案已覆盖 | 数据库草案包含多项目及核心 MVP 实体；每个表和字段均有中文 COMMENT。草案未执行，且不作为迁移批准。 |
| 安全边界 | 部分覆盖 | 已写明项目隔离、失败关闭、Secret 不落明文、Agent 不审批 Gate 等约束；认证、权限矩阵、Runtime 审批仍待决。 |
| Design/API 边界 | 通过 | API 结构按已批准设计映射；草案不涉及前后端代码，也没有批准任何既有接口变更。 |
| Contract 决策清单 | 未完成（阻塞） | HD-001 至 HD-005 是核心契约行为的 `HUMAN_DECISION_REQUIRED`；HD-006 集成范围需明确纳入或延期。 |

## 阻塞决策

- **HD-001 / OI-002：**部署网络与认证/身份提供方、Token 生命周期和服务身份传递。
- **HD-002 / OI-003：**Runtime 模型、工具和操作范围、批准/撤销角色、有效期和密钥来源。此项决定前不得启用真实执行。
- **HD-003 / OI-004：**角色-动作权限矩阵、成员管理和角色兼任规则；需落实 Reviewer 独立性及禁止自评/自批。
- **HD-004 / OI-005：**交付物权威来源、版本/冲突规则、下载方式及证据保留策略。
- **HD-005 / OI-007：**首版定量非功能、安全运营、审计/备份边界，或由负责人明确接受的默认运营边界。
- **HD-006 / OI-006：**明确代码托管、构建/测试和通知集成不在 MVP，或列出实际纳入项。

上述未决契约若由 Agent 自行推定，会决定身份安全、访问授权、外部副作用、数据来源或验收边界。按 `.agent/rules.md` 必须输出 `HUMAN_DECISION_REQUIRED`，不得由 Orchestrator 代替负责人批准。

## 门禁结论

API、数据库和安全契约现已形成可评审草案，但不构成批准。Contract Gate 保持 `pending`；`.agent/state.yaml` 保持 `current_phase: Contract`、`next_gate: Contract Gate`。完成 HD-001 至 HD-005 决策，并明确 HD-006 是否延期后，再同步契约和提交独立 Contract Reviewer 复审。此前不得进入 Development、执行数据库草案或修改业务接口。

## 评审证据

- API：`contracts/api/api-contract.yaml`，版本 `0.1.0-draft`，包含 MVP 页面对应的资源操作和统一错误代码。
- Database：`contracts/database/schema-draft.sql`，MySQL 8 草案，表/字段含中文 `COMMENT`，显式使用项目级外键关联。
- Security：`contracts/security/security-contract.md`，列出已确定安全约束和待决认证/授权/运行政策。
- Human decisions：`contracts/contract-decisions.md`，记录六项负责人待决内容及 Gate 影响。
- 构建/测试/迁移：未运行；本次为契约初稿与文档审查，不涉及代码或数据库变更。

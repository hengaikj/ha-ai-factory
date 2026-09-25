# Contract 全局复审准备记录：延期决策同步

日期：2026-09-23  
项目：HA AI Software Factory  
范围：HD-002、HD-004、HD-005、HD-006 的 M02 明确延期及跨契约对齐

## 决策依据

项目负责人于 2026-09-23 在当前会话确认采用以下 M02 范围：

- **HD-002 / OI-003：**M02 不启用真实 Agent Runtime，不调用模型或工具，不配置凭据或触发外部副作用；保留状态查询和失败关闭行为。真实执行能力留待独立 Contract。
- **HD-004 / OI-005：**仓库文件是交付物权威来源；M02 只登记仓库引用、版本和内容摘要，不提供系统内编辑、文件托管、冲突合并或公开下载 URL。
- **HD-005 / OI-007：**M02 接受默认运行边界，不承诺正式 SLA、并发、响应、容量、审计保留、备份 RPO/RTO 或合规指标；正式 NFR 和运维指标留待后续 Contract。
- **HD-006 / OI-006：**M02 不接入 Git、CI、测试编排、通知或其他外部服务，不配置第三方凭据；集成能力留待后续 Contract。

## 同步文件

- `contracts/contract-decisions.md`
- `contracts/contract-baseline-v1.0.md`
- `contracts/contract-gate-approval.md`
- `contracts/api/api-contract.yaml`
- `contracts/database/schema-draft.sql`
- `contracts/security/security-contract.md`

API Contract 将 Runtime 启动请求定义为 M02 下始终失败关闭并返回 `CONFIG_NOT_APPROVED`（HTTP 409），不会返回执行已接受状态；配置状态查询仍用于显示未配置、待批准、拒绝、过期或校验失败等状态。交付物和资源引用限定为仓库文件引用。

## 复审状态

本记录用于独立 Reviewer 复核，不构成 Contract Gate 批准。M02 Project / Member / Gate scoped PASS 仍按 `evidence/contract-review-M02-2026-09-23.md` 记录；全局 Contract Gate 保持 `PENDING REVIEW`。环境网络边界、OIDC issuer 和密钥配置仍须在环境契约/部署前确认。全局 Gate 获批前，不得将本基线用于 Development，也不得执行数据库迁移或启用真实 Agent Runtime。

## 校验

- `git diff --check`：通过。
- 未运行构建、测试或数据库迁移。

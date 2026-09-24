# HA AI Software Factory 功能完成矩阵

更新时间：2026-09-24（HEAD ca5988d）

## 已实现并可验证

| 能力 | 后端 | 前端 | 验证 |
| --- | --- | --- | --- |
| OIDC 会话、会话读取与注销 | 已实现 | 已实现 | Maven + Vitest |
| 项目列表、创建、详情、元数据更新 | 已实现 | 已实现 | Maven + Vitest |
| 项目成员新增、角色更新、移除 | 已实现 | 已实现 | Maven 集成测试 + Vitest |
| 任务查询、创建、详情、状态更新 | 已实现 | 已实现 | Maven + Vitest |
| 交付物登记与仓库引用 | 已实现 | 已实现 | Maven + Vitest |
| 交付物独立评审 | 已实现 | 已实现 | Maven + Vitest |
| Open Issue 创建与人工决策 | 已实现 | 已实现 | Maven + Vitest |
| Gate 范围提交、检查项决策、最终决策 | 已实现 | 已实现（含独立评审提交入口） | Maven + Vitest |
| 阶段推进与 Gate 前置校验 | 已实现 | 已实现 | Maven + Vitest |
| Activity 审计查询 | 已实现（状态变更统一写入） | 已实现 | Maven 集成测试 |
| Runtime 配置状态查询 | 已实现 | 已实现 | Maven + Vitest |

## 已实现但保持失败关闭

| 能力 | 当前行为 | 原因 |
| --- | --- | --- |
| Agent Runtime 启动 | 固定拒绝并返回可识别冲突 | HD-002：M02 不启用真实执行 |
| Agent Run 状态查询 | 固定返回不存在 | 不伪造执行记录，保持失败关闭 |
| 模型、工具和外部副作用 | 无调用路径 | HD-002 / Laya Runtime 专项 Contract 约束 |

## 明确延期，当前不属于可交付功能

| 能力 | 决策依据 |
| --- | --- |
| 系统内文件编辑、托管、下载、冲突合并 | HD-004 |
| Git、CI、测试编排、通知等外部集成 | HD-006 |
| 正式 SLA、容量、备份、RPO/RTO、审计保留 | HD-005 |
| 生产模型启用与真实 Runtime 执行 | HD-002 及 Laya Runtime Contract 第 9 节 |
| 生产部署与发布回滚 | Release 阶段决策（OI-009） |

## 验证基线

- 前端 Vitest：20 项通过。
- 前端 `vue-tsc -b && vite build`：通过。
- 后端 Maven 测试基线：36 项通过，0 失败，0 错误；包含成员角色替换资格校验后的集成验证。
- 治理测试：16 项通过。
- Agent Runtime Python 单元测试：27 项通过，覆盖请求/响应 schema、Laya provider 重试与错误映射、mTLS 配置和失败关闭策略。
- 关键数据库迁移：Flyway V1–V8 已在 Testcontainers MySQL 集成测试中执行。

## 交付结论

M02 已批准范围内的 Project / Member / Gate 基础平台能力已实现；延期项保持显式关闭。整体项目要达到生产级“所有功能完成”，还需要后续 Contract、独立 Review、部署配置和对应的外部系统批准。

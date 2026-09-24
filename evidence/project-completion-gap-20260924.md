# HA AI Factory 整体功能差距审计

日期：2026-09-24
审计 HEAD：`e1924c1c09d63e3a73131e9751d7ed6f4c293c98`
依据：需求基线、PRD、M02 API Contract、Laya Runtime Contract、当前源代码与测试证据。

## 已实现并有证据

- 企业 OIDC Authorization Code + PKCE、服务器会话、CSRF 与项目创建/列表。
- MySQL/Flyway 身份、项目成员 Owner 初始记录及 Gate/Issue 摘要查询。
- Vue 控制台项目列表、搜索、创建、刷新。
- 独立内网 Laya HTTP Provider 的模型协议校验；真实 Runtime 执行仍按 HD-002 关闭。
- reverse-skill 治理 MVP：路由、范围校验、动作门禁、证据校验、治理 CLI、PR 测试 CI 和受限试点。

## 未完成的已批准产品功能

- 项目成员查询、添加、角色替换、移除及 Owner 保留规则的 API/界面。
- 任务创建、分配、状态更新和列表。
- 交付物登记、评审结论和证据引用。
- Gate、检查项、提交、独立 Reviewer 冲突校验和最终决定。
- Open Issue 创建、人工决策和状态查询。
- 模板/规则资源索引和 Activity 审计查询。
- 对应前端页面、错误状态和端到端联调覆盖。

## 明确延期/不可宣称完成

- M02 真实 Agent Runtime、模型、工具和外部副作用（HD-002）。
- 系统内文件托管、编辑、下载和 Git/CI 产品集成（HD-004、HD-006）。
- 正式 SLA、容量、备份、RPO/RTO 和审计保留指标（HD-005）。
- Laya 生产模型制品锁定、许可审查、中文离线评测和生产 mTLS 部署。
- GitHub 分支保护和 required check：工作流已运行，但仓库规则未启用。

## 下一实现顺序

1. M02 Member API 与项目成员页面，复用现有 OIDC 主体和项目 RBAC。
2. Task API/页面，再接 Deliverable 与 Review。
3. Gate/Issue/Activity 纵切及独立性规则测试。
4. 资源索引页面和完整浏览器 E2E。
5. Runtime 仅在 OI-003 关闭及专项启用条件满足后推进。

当前不能将项目标记为整体完成；Integration/Release Gate 保持 pending。

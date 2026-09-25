# HA AI Factory 整体功能差距审计

日期：2026-09-24
审计 HEAD：`695136d`
依据：需求基线、PRD、M02 API Contract、Laya Runtime Contract、当前源代码与测试证据。

## 已实现并有证据

- 企业 OIDC Authorization Code + PKCE、服务器会话、CSRF 与项目创建/列表。
- MySQL/Flyway 身份、项目成员 Owner 初始记录及 Gate/Issue 摘要查询。
- Vue 控制台项目列表、搜索、创建、刷新、注销和项目工作区。
- 项目成员查询、添加、角色替换、移除及 Owner 保留规则的 API/界面。
- 任务创建、列表、详情和状态更新。
- 交付物登记、独立评审和仓库引用。
- Gate 范围提交、检查项决定、最终决定和 Reviewer 冲突校验。
- Open Issue 创建、人工决策和状态查询。
- 模板/规则资源索引、Activity 审计查询和阶段推进。
- 独立内网 Laya HTTP Provider 的模型协议校验；真实 Runtime 执行仍按 HD-002 关闭。
- reverse-skill 治理 MVP：路由、范围校验、动作门禁、证据校验、治理 CLI、PR 测试 CI 和受限试点。
- 前端 20 项测试、后端 36 项测试、治理 16 项测试和 Runtime 27 项测试均通过。

## 未完成的已批准产品功能

- 当前没有发现 M02 Project / Member / Gate Contract 中尚未实现的后端路径。
- 尚缺真实浏览器端到端联调证据；现有验证为 API/组件测试和 Testcontainers 集成测试。
- 尚缺独立 Reviewer 对最新实现 HEAD 的最终复审。

## 明确延期/不可宣称完成

- M02 真实 Agent Runtime、模型、工具和外部副作用（HD-002）。
- 系统内文件托管、编辑、下载和 Git/CI 产品集成（HD-004、HD-006）。
- 正式 SLA、容量、备份、RPO/RTO 和审计保留指标（HD-005）。
- Laya 生产模型制品锁定、许可审查、中文离线评测和生产 mTLS 部署。
- GitHub 分支保护和 required check：已在 master 启用并完成 API 核验；最新 push/PR 的四项 required checks 均通过。

## 下一步

1. 对最新 HEAD 运行独立 Reviewer 复审并更新 PR。
2. 增加真实浏览器 E2E 和部署前环境验证。
3. 关闭 Laya Runtime Development Gate 的模型制品、许可、中文离线评测和 mTLS 部署条件。
4. 在独立 Reviewer 完成最新 HEAD 复审并满足 Release 条件后，再执行生产发布。

当前不能将项目标记为生产级整体完成；最新 HEAD 为 `9f57ff4`，项目变更审计和 Runtime 请求边界已补齐。M02 开发范围已完成，Integration/Release Gate 保持 pending。

# XZG AI Project Template v1

通用 AI 软件工程项目模板。

适用于：
- 企业软件
- AI 应用
- Web 系统
- 私有化项目

核心目录：

| 目录 | 用途 |
| --- | --- |
| `.agent/` | Agent 配置、当前任务、项目阶段状态和协作规则。 |
| `agents/` | Orchestrator、Contract、后端和前端 Agent 职责说明。 |
| `ha-ai-factory-server/` | Java / Spring Boot 后端工程入口。 |
| `ha-ai-factory-web/` | Vue 3 / TypeScript 前端工程入口。 |
| `ha-ai-agent-runtime/` | Python / FastAPI Agent Runtime 工程入口。 |
| `contracts/api/` | API 契约文档。 |
| `contracts/database/` | 数据库契约与模板；正式迁移由 Contract Gate 批准后管理。 |
| `contracts/security/` | 安全契约。 |
| `contracts/test/` | 契约测试与接口验证资产。 |
| `database/` | 数据库实现、迁移和运行期数据库资产。 |
| `docs/requirement/`、`docs/product/`、`docs/design/` | 需求、产品和设计交付物。 |
| `docs/development/` | 开发规范、实现说明和工程指南。 |
| `docs/operation/` | 部署、运行和维护手册。 |
| `docs/release/` | 发布计划、发布记录和回滚说明。 |
| `evidence/` | Gate Review、测试和交付证据。 |
| `integration/` | 跨模块集成资产。 |
| `scm/` | 代码托管和源码管理配置说明。 |
| `scripts/` | 项目初始化与状态检查脚本。 |
| `templates/` | 任务、评审和证据模板。 |

服务目录当前保留为工程入口；业务实现须按 `SKILL.md` 和 `.agent/rules.md` 推进，并遵守对应 Gate。Contract Gate 未批准前，不实现 API 或执行数据库契约草案。

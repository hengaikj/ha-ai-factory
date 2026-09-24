# Integration Readiness Evidence

日期：2026-09-24  
分支：`feature/M01-foundation`  
阶段：`Integration`  
对应状态：`.agent/state.yaml`

## 本地验证结果

| 范围 | 命令 | 结果 |
| --- | --- | --- |
| Java 后端与 MySQL/Flyway | `mvn -q test` | 通过；36 项，0 失败，0 错误；Testcontainers MySQL 执行 V1–V8 |
| Vue 前端 | `pnpm test -- --run && pnpm build` | 通过；20 项测试，生产构建成功 |
| Agent Runtime | `python3 -m pytest -q` | 通过；27 项 |
| Governance | `python3 -m unittest discover -s tests/governance -p 'test_*.py' -q` | 通过；16 项 |
| 浏览器 smoke | 本地 Vite + in-app browser | 页面渲染、未登录/后端不可用状态可见，未泄露异常详情 |
| 前后端匿名联调 | 临时 MySQL 8.0.36 + Spring Boot + `curl /projects` | Flyway V1–V8 成功，匿名请求返回 HTTP 401；测试容器和进程已清理 |

远端 GitHub Actions 运行 `36012742665`（Push）和 `36012752043`（PR #2 merge ref）均已通过 Governance、Backend、Frontend、Agent Runtime 四个 job。Agent Runtime 失败的依赖安装问题已由 `7a4d267` 修复；工作流使用 `pip install '.[dev]'` 时现在会安装 pytest。

## 自动化配置

`.github/workflows/ha-governance.yml` 已包含 Governance、Backend、Frontend 和 Agent Runtime 四个验证 job，并在 Pull Request 与目标分支 Push 时运行。

本地直接启动 Spring Boot 必须显式提供 `MYSQL_URL`、`MYSQL_USERNAME` 和 `MYSQL_PASSWORD`。缺少这些配置时应用拒绝启动，不会降级到内存数据库；这是预期的失败关闭行为。

## 尚未满足的 Integration / Release 条件

- PR #2 已转为可审查状态（非 Draft），当前 GitHub 状态为 `REVIEW_REQUIRED`；仓库协作者中目前只有执行账号，尚未配置独立 Reviewer，因此最终复审仍未完成。
- 真实 OIDC 测试 IdP 和认证后的浏览器 E2E 尚未配置。
- GitHub master 分支保护已启用：四项 required checks、至少一次批准、最新推送需批准、过期审查失效、强制管理员遵守、禁止强推/删除及会话解决；配置证据见 `evidence/governance/GOV-06/branch-protection-20260924.md`。
- Laya 生产模型 revision、权重摘要、许可证审查、中文离线评测和 mTLS 部署尚未完成。
- M02 的真实 Runtime、工具、外部副作用、Git/CI 产品集成和文件托管仍按 Contract 保持关闭或延期。

## 结论

M02 已批准范围具备可重复的本地集成验证基线；Integration Gate 仍为 `PENDING`，不能据此宣称生产发布或整体功能全部完成。

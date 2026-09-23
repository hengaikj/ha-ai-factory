# M02 Project 前后端联调证据

日期：2026-09-23

分支：`feature/M01-foundation`
范围：已批准 API Contract 中的身份会话、项目创建与项目列表联调纵切；不代表整个 M02 Contract 功能已实现，也不改变 Integration/Release Gate 状态。

## 实现

- Web 控制台通过 OpenAPI 根路径 `/auth/*` 与 `/projects` 调用后端；支持服务器端项目搜索和分页、项目创建、401 未登录状态、OIDC 配置缺失/登录失败提示。
- Spring Security 使用企业 OIDC Authorization Code + PKCE S256、服务器端 `ha_session` Cookie、30 分钟空闲时限与 8 小时绝对时限；状态变更请求同时验证 CSRF token 和部署前端来源。
- 项目创建事务从已认证 OIDC 主体解析创建者，并原子创建 Owner 成员及 OWNER 角色；项目列表只返回当前主体 ACTIVE membership。
- 禁用主体不会在 OIDC 登录时自动重新激活。项目响应的 Gate 状态与未关闭 Issue 数由当前阶段 Gate 和 MySQL Issue 数据查询；新项目无 Gate/Issue 时按契约初始值返回 `PENDING` 与 `0`。
- Flyway V2 增加项目 Gate / Open Issue 摘要表；所有新增表及列均有中文 COMMENT。长文本采用 `TEXT` 以满足 MySQL 8 utf8mb4 行大小约束，接口仍执行批准的字段长度校验。

## 验证

- `DOCKER_CONFIG=/tmp/ha_factory_empty_docker_config mvn -f ha-ai-factory-server/pom.xml test -q`：通过；20 个测试，0 失败。Testcontainers MySQL 8.0.36 成功执行 Flyway V1/V2。
- `cd ha-ai-factory-web && pnpm test`：通过；6 个测试，0 失败。
- `cd ha-ai-factory-web && pnpm build`：通过；`vue-tsc -b` 与 Vite 生产构建成功。
- `git diff --check`：通过。
- 隔离 smoke 服务验证 Vite 页面 `/` 返回 200，代理请求 `/auth/session`、`/projects` 返回预期 401；OIDC 未配置时 `/auth/login` 返回预期 503。测试 MySQL、Spring 和 Vite 进程已停止/清理。
- 浏览器级 OIDC E2E：使用一次性本地 OIDC 测试提供方和 MySQL 8 容器，通过 Vite 页面启动 OIDC 授权；测试提供方验证 S256 challenge/verifier，并签发短时 RSA-SHA256 测试 ID Token。Spring 校验并建立服务器会话后，浏览器成功创建项目（HTTP 201），随后通过服务端搜索、清除搜索和刷新读回项目；容器数据库确认测试主体拥有 1 个项目。该测试验证协议及应用集成，不替代企业 IdP 的生产环境认证验证。

## 独立复审与限制

- 独立只读 Code Review 最终未发现剩余 findings；该复审不批准 Integration Gate。
- 未配置真实企业 OIDC issuer/client credentials；真人企业 IdP 的生产登录策略、MFA 与回调白名单仍需环境负责人验证。本次本地浏览器 E2E 使用模拟 IdP 验证了授权码 + PKCE、签名验证、会话 Cookie、CSRF、前端 API 代理和 MySQL 项目读写闭环。
- 真实部署前仍需由环境负责人配置 OIDC issuer、回调白名单、客户端 Secret、MySQL 凭据与 TLS 边界。Agent Runtime 真实执行保持 HD-002 禁用状态。

## Gate 状态

- `development`：实现纵切与本地验证完成，等待项目 Controller/Reviewer 更新正式状态。
- `integration`：仍为 pending；不得据本证据自动晋级。
- `release`：仍为 pending。

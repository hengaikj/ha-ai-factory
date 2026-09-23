# HA AI Factory Server：身份与项目联调纵切

## 启动配置

应用默认连接 MySQL 8；不会回退到内存数据库或开发身份。启动前通过部署环境提供：

```text
MYSQL_URL=jdbc:mysql://<mysql-host>:3306/ha_ai_factory?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC
MYSQL_USERNAME=<least-privilege-database-user>
MYSQL_PASSWORD=<secret-from-secret-manager>
FACTORY_OIDC_ISSUER_URI=https://<enterprise-idp>/...
FACTORY_OIDC_CLIENT_ID=<registered-client-id>
FACTORY_OIDC_CLIENT_SECRET=<secret-from-secret-manager>
FACTORY_WEB_BASE_URL=https://<frontend-host>/
```

OIDC 客户端需登记重定向地址 `https://<application-host>/auth/callback`。授权码、state、nonce 与 PKCE verifier 由 Spring Security OIDC Client 处理；浏览器只获得 `Secure; HttpOnly; SameSite=Lax` 的 `ha_session` Cookie。未提供完整 OIDC 配置时，登录入口返回 503，受保护 API 返回 401。所有状态变更请求还需通过 CSRF token 与部署前端 Origin/Referer 来源校验。

`FACTORY_WEB_BASE_URL` 决定OIDC成功后的固定303回跳位置，默认 `/`（同源部署）；本地 Vite 联调设置为 `http://localhost:5173/`。回跳目标不接受请求参数输入；生产使用HTTPS地址。

首次启动会通过 Flyway 应用 `src/main/resources/db/migration/` 下的 MySQL 迁移。迁移只包含 OIDC Principal、Project、Project Member 和 Member Role 表；每个表及列都有中文 COMMENT。此文档不表示已在任何持久数据库执行迁移。

## API 联调

- `GET /auth/login`：开始企业 OIDC 登录；缺少 OIDC 配置时明确返回 503。
- `GET /auth/session`：返回已登录主体引用、显示名及 CSRF token。
- `POST /auth/logout`：要求 `X-CSRF-Token`，撤销服务器会话并清除 Cookie。
- `GET /projects?page=1&pageSize=20`：仅返回当前主体的 ACTIVE membership 项目，并由当前阶段 Gate 和未关闭 Issue 数据计算摘要。
- `POST /projects`：创建项目，在同一事务中创建 ACTIVE Owner membership 和 OWNER role。

写请求从 OIDC security principal 获取 issuer+subject；请求 JSON 中不接受操作者或角色字段。项目创建请求示例：

```json
{"name":"示例项目","description":"说明","techStack":{"backend":"Java 17"}}
```

## 持久化边界

Principal、Project、Membership 与 Role 使用 MyBatis-Plus Mapper 和 MySQL 持久化。Servlet `HttpSession` 当前由单实例容器管理，空闲超时 30 分钟、绝对时限 8 小时；未接入共享 Spring Session/JDBC 存储，因此多实例部署时需要粘性会话或后续批准的共享会话方案。没有用固定用户、匿名主体或本地密码绕过 OIDC。

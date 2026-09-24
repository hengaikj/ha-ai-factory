# Frontend Smoke Evidence

日期：2026-09-24  
分支：`feature/M01-foundation`  
本地前端：Vite `http://127.0.0.1:5174/`

## 场景

在未启动后端/OIDC 服务的条件下打开 HA AI Factory 前端，验证基础渲染和失败状态是否安全可见。

## 观察结果

- 页面标题：`HA AI Factory`。
- 主导航显示项目、任务、交付物、Gate、Open Issues 和 Agent Runtime。
- 项目工作台显示“项目加载失败”和“请求未能完成，请稍后重试”。
- 页面没有泄露后端异常堆栈、凭据或原始网络响应内容。
- 需要身份认证时由生产 OIDC 流程处理；本 smoke 未提交任何身份信息。

## 限制

该 smoke 不代表已完成 OIDC 登录、项目写操作或真实 Runtime 执行；这些路径分别由 API/集成测试和失败关闭策略验证。真实浏览器登录 E2E 仍需配置测试 IdP 和后端环境。

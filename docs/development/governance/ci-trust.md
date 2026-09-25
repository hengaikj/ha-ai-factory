# 治理 CI 信任边界

工作流 `.github/workflows/ha-governance.yml` 使用完整提交 SHA 固定的官方 Actions，并只申请 `contents: read`。它运行治理测试，检查名为 `HA Governance / gate`。

当前 `.agent/governance/enforcement.json` 为 `advisory`，因此工作流运行结果不会自动成为合入阻断条件。要切换为 `strict`，仓库管理员还必须将该检查配置为 required，并启用独立 Reviewer、禁止直推和过期批准等分支保护规则。工作流文件存在不代表这些仓库设置已经生效。

本地 CLI 和工作流测试不会注入 `trusted_ci` 批准上下文，也不会授予 `ALLOW`。正式可信门禁需要受保护基线、独立批准收据和管理员配置的分支保护证据。

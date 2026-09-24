# 治理 MVP 操作说明

## 适用范围

本工具只做确定性路由、结构校验、动作门禁和证据完整性检查。它不启动 Runtime、模型、工具、数据库迁移、部署或 Git/CI 操作。

## 命令

```bash
python3 -m scripts.governance route --input task.json
python3 -m scripts.governance validate-work-item --input work-item.json
python3 -m scripts.governance evaluate --input gate-context.json
python3 -m scripts.governance verify-evidence --input evidence.json
```

命令只读取 JSON 输入并输出 JSON 结果。本地 CLI 永远不能产生 `ALLOW`：它没有可信 CI、独立批准收据或受保护策略来源。未知任务、缺少基线、缺少人工批准、禁止动作、非零退出码均报告为未就绪；禁止动作保持失败关闭。退出码 2 表示校验或门禁硬失败。

## 可信边界

`.agent/governance/*.json` 当前为 `PROPOSED`，不是批准记录。策略只有在受保护基线标记为 `ACCEPTED`、执行面为 `trusted_ci`、审批来源和策略引用均已验证时，才可能计算为 `ALLOW`。候选分支不能通过修改这些文件为自己授予权限；正式合入仍需独立 Reviewer 和可信 CI。当前没有配置分支保护，不能声称 CI 门禁已生效。

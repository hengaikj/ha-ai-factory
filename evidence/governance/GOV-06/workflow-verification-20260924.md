# GOV-06 工作流校验记录

日期：2026-09-24
源码：`d0592343a07653b98cf5dd452a43bdfff02d532a`

## 已核验

- `actions/checkout@11bd71901bbe5b1630ceea73d27597364c9af683` 对应官方 `v4.2.2` 标签。
- `actions/setup-python@42375524e23c412d93fb67b49958b491fce71c38` 对应官方 `v5.4.0` 标签。
- 工作流权限为 `contents: read`，检查名为 `HA Governance / gate`。
- 本地治理测试 16/16 通过；前端测试 7/7、前端构建和后端 Maven 测试均通过。

## 未核验/未启用

- GitHub Actions 实际运行记录：当前提交尚未推送，未产生远端 run 证据。
- required check、独立 Reviewer、禁止直推和分支保护：GitHub API 未授权（HTTP 401），状态 `UNVERIFIED`。
- 治理基线接受收据：`.agent/governance/adoption.json` 仍为 `PROPOSED`。

本记录证明工作流文件和 Action 来源可核验，不证明远端 CI 或分支保护已生效。

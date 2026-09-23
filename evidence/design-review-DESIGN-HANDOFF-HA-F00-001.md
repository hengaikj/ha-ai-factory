# Design Gate Review

文档编号：DESIGN-HANDOFF-HA-F00-001  
审查对象：`docs/product/prd.md`、`docs/design/ux-ui-spec.md`、`docs/design/design-handoff.md`、`docs/design/assets/portfolio-ledger-direction-1.png`  
审查基准：`docs/requirement/requirement-baseline.md`、`docs/lifecycle.md`、`docs/quality-gate.md`、`.agent/rules.md`  
审查日期：2026-09-23  
审查角色：独立 Designer/Reviewer  
最终决定：**不批准 Design Gate；需补充设计后重新审查。**

## 检查项

| 检查项 | 结果 | 说明 |
| --- | --- | --- |
| 产品范围一致 | 通过 | UX/UI 方向与多项目 MVP 一致，Agent Runtime 被定义为受授权的工作入口，不承担 Gate 批准。 |
| 主要页面和视觉方向 | 通过 | 项目总览、项目详情摘要、Agent Runtime 入口及项目台账视觉参考已提供。 |
| 状态与异常态 | 部分通过 | 已覆盖加载、空、搜索无结果、失败、无权限、Gate 和 Runtime 状态；缺少 Runtime 配置未批准/缺失时的明确拒绝或未配置交互。 |
| MVP 流程覆盖 | 不通过（阻塞） | PRD 将 P-01 至 P-08 全部定义为 MVP P0，但任务、交付物评审、Gate 检查、Open Issue 决策、模板/规则查阅和审计追溯缺少可完成端到端操作的设计，无法核验 AT-02 至 AT-07。 |
| 阶段边界 | 通过 | API 绑定仅描述数据关系并明确留待 Contract 批准；未发现设计材料越界批准 API、数据结构或实现。 |

## 阻塞发现

### D1 — 多项 MVP 操作流程缺少设计（阻塞）

PRD 要求项目负责人、Orchestrator、工程 Agent 和 Reviewer 完成任务分配、交付物评审、Gate 检查、人工决策、模板与规则查阅/下载、状态审计等操作。当前 UX/UI 与 Design Handoff 主要定义项目总览、项目详情摘要和 Agent Runtime 执行面板。侧边栏虽列出任务、交付物、Gate、Open Issues 等入口，但没有相应页面、抽屉、对话框或明确的承载流程，也没有说明从入口如何完成相应操作。

**重新审查前需补充：**对 AT-02 至 AT-07 标明操作入口、页面/组件承载、关键状态、操作步骤和完成后的可见结果；或明确这些流程如何由已有页面承载并补齐交互说明。

### D2 — Runtime 未授权配置状态缺少明确交互（阻塞）

文档要求 OI-003 未关闭时阻止真实执行，但未定义配置缺失或未批准时执行面板的拒绝状态、提示内容及用户可采取的下一步动作。通用错误态不足以核验“未授权不能启动”的产品行为。

**重新审查前需补充：**定义未配置、待批准和已批准配置的表现；明确启动按钮状态、阻止原因以及返回人工决策/配置流程的入口。

## 非阻塞建议

- 智行官品牌 SVG 和样标使用个人机绝对路径。建议纳入可移交资源包或说明正式资源交付方式，避免设计交接依赖本机路径。
- 视觉参考图含通知铃、用户头像菜单和跨任务的全局搜索，但规格未定义通知和用户菜单行为，搜索范围只承诺项目。建议裁切/标明示意元素，或补充其行为与范围。

## 最终 Gate 决定

独立 Designer/Reviewer 最终决定：**不批准 Design Gate**。D1 与 D2 为阻塞项；完成设计补充后重新提交独立审查。`.agent/state.yaml` 中 `design` 保持 `pending`，`current_phase` 保持 `UX/UI`，`next_gate` 保持 `Design Gate`。在 Design Gate 获批前不得进入依赖该 Gate 的 Contract 阶段。

## 评审证据

- PRD 将 P-01 至 P-08 作为 MVP P0，并分别映射到 AT-01 至 AT-08；本次对照 UX/UI 与 Design Handoff 后确认 AT-02 至 AT-07 缺少可操作流程设计。
- Design Handoff 的 API 绑定明确留待 Contract 阶段批准；本次未审查或批准接口、数据模型或代码实现。
- Reviewer 为独立 Designer/Reviewer，未修改审查对象。
- 构建/测试：未运行；本次为设计交付物审查，不涉及代码变更。

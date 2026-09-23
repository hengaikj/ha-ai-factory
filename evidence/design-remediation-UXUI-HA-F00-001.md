# UX/UI Design Gate 整改证据

关联审查：`evidence/design-review-DESIGN-HANDOFF-HA-F00-001.md`  
整改对象：D1、D2  
状态：待独立 Designer/Reviewer 重新审查

## D1 整改

已在 `docs/design/ux-ui-spec.md` 的“## MVP 操作流程”和 `docs/design/design-handoff.md` 的“### MVP 操作承载”中补充 AT-02 至 AT-07：

- AT-02：项目详情任务区的新建、分配、关联交付物、状态更新及完成结果。
- AT-03：交付物评审面板的通过、退回、待澄清、证据关联和重新提交。
- AT-04：Gate 检查项处理、提交前校验、阻塞原因和推进阶段入口。
- AT-05：Open Issue 创建、`HUMAN_DECISION_REQUIRED`、决策记录和 Gate 阻塞关系。
- AT-06：模板/规则筛选、查看、下载、来源追溯和来源不明状态。
- AT-07：活动与审计筛选、变更详情、证据追溯和只读约束。

每项均定义了操作入口、关键步骤、状态变化和完成后的可见结果，并要求保存失败与无权限状态。

## D2 整改

已在 UX/UI 规格中新增“Runtime 配置拒绝流程”，并在 Design Handoff 状态说明中登记。Runtime 执行前区分：

1. 未配置：禁用启动，提示缺少模型/工具/权限，提供查看配置要求和创建 Open Issue。
2. 待批准：禁用启动，提示等待批准，提供查看待决事项和返回任务。
3. 已拒绝/已过期：禁用启动，展示拒绝原因，提供重新提交配置。
4. 已批准：允许启动，进入排队中。
5. 校验失败：禁用启动，提供重试校验和创建 Open Issue。

所有拒绝状态都显示项目、任务、模型、工具和权限摘要；禁止绕过校验、自动替换模型或扩大工具权限。

## 重新审查请求

请独立 Designer/Reviewer 重新检查 D1、D2，确认上述流程足以核验 AT-02 至 AT-08，并决定 Design Gate 是否通过。项目状态和 `design` Gate 在重新审查前保持原值：`UX/UI` / `pending`。

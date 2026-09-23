# HA AI Software Factory Design Handoff

文档编号：DESIGN-HANDOFF-HA-F00-001
状态：待 Design Gate Review
关联 UX/UI：`docs/design/ux-ui-spec.md`
视觉方向：项目台账（方向 1）

本交接文档将已确认的 UX/UI 方向交给后续 Design Review、Contract 和实现角色。它定义页面行为和数据关系，不批准 API 路径、数据库结构或代码接口。

## 页面说明

### 项目总览

桌面基准视口为 1440 × 1024。左侧显示智行官品牌和六个 MVP 入口：项目、任务、交付物、Gate、Open Issues、Agent Runtime。主区域展示项目搜索、创建项目操作、项目表格和最近活动。项目表格是首屏主焦点，列出项目名称、负责人、当前阶段、Gate 状态、Open Issues 数量和更新时间。

### 项目详情

从项目行进入详情，展示项目摘要、生命周期阶段、未完成任务、交付物、Open Issues、状态变更和证据入口。所有内容必须带项目上下文，避免多个并行项目的数据混淆。

### Agent Runtime 执行入口

从任务或项目详情打开执行面板，执行前显示项目、任务、模型、工具和权限摘要。未批准配置阻止启动并提示 OI-003；执行过程展示排队、执行中、成功、失败和取消状态。该入口没有 Gate 批准操作。

## 组件说明

| 组件 | 责任 | 关键约束 |
| --- | --- | --- |
| BrandMark | 显示智行官品牌。 | 使用原始品牌资源，保持比例和颜色。 |
| SideNavigation | 切换六个 MVP 模块。 | 当前项高亮；权限规则受 OI-004 约束。 |
| PageHeader | 标题、搜索、创建项目。 | 搜索仅影响当前可访问项目。 |
| ProjectTable | 展示多个项目和状态。 | 项目标识贯穿行、详情和关联对象。 |
| StatusTag | 展示阶段、Gate 和执行状态。 | 文字与颜色同时表达，不依赖颜色。 |
| ActivityList | 展示关键状态变化。 | 只读追踪，不直接修改 Gate。 |
| ProjectDetailDrawer | 查看项目摘要。 | 打开时保留项目上下文。 |
| AgentRunPanel | 展示授权和执行结果。 | OI-003 关闭前不可启动真实执行。 |
| EvidenceLink | 访问交付物和 Gate 证据。 | 权威来源受 OI-005 约束。 |

## 状态说明

- 页面：加载、正常、有数据、无项目、搜索无结果、请求失败、无权限。
- Gate：待评审、已通过、已退回、阻塞、`HUMAN_DECISION_REQUIRED`。
- Agent Runtime：待启动、排队中、执行中、成功、失败、取消。
- 所有阻塞或未决状态都必须展示原因和下一步动作；未通过 Gate 禁止依赖阶段推进。
- Reviewer 的独立性与角色兼任边界由 OI-004 在权限设计中落实。

## 资源文件

| 资源 | 位置 | 使用要求 |
| --- | --- | --- |
| 智行官主品牌彩色 SVG | `/Users/zjh/Documents/恒爱科技文档/恒爱科技外宣资料/智行官品牌V1-正式申报资料包/01-智行官商标/01-矢量源文件/V1独立图形-彩色.svg` | 实现时使用此权威源文件。 |
| 项目台账视觉参考 | `docs/design/assets/portfolio-ledger-direction-1.png` | 仅作方向参考，不直接作为前端页面。 |
| 完整规格 | `docs/design/ux-ui-spec.md` | 读取页面、组件、状态和视觉规则。 |

## API 绑定

以下是页面数据关系，具体契约需在 Contract 阶段批准：

| 交互 | 数据关系 | 必须满足 |
| --- | --- | --- |
| 项目列表 | 项目、负责人、阶段、Gate、Open Issues、更新时间 | 数据按项目标识隔离。 |
| 项目详情 | 项目、任务、交付物、Open Issues、状态变更、证据 | 所有关联对象可回溯项目。 |
| Gate 评审 | 检查项、结论、Reviewer、时间、证据 | 只能由独立 Reviewer 形成批准结论。 |
| Agent 执行 | 项目/任务、授权模型、工具、状态、结果、错误 | OI-003 关闭前禁止真实执行；服务端拦截越权操作。 |
| 证据查看 | 交付物、Gate、文件或证据引用 | 来源权威规则由 OI-005 确认。 |

## 交接检查

- [x] 页面说明已完成。
- [x] 组件说明已完成。
- [x] 状态说明已完成。
- [x] 品牌和视觉参考资源已登记。
- [x] API 数据绑定已登记且明确待 Contract 批准。
- [ ] 独立 Designer/Reviewer 完成 Design Gate 审查。
- [ ] OI-004 在权限设计中落地 Reviewer 独立性和角色兼任边界。
- [ ] OI-003 在启用 Agent Runtime 执行前关闭。

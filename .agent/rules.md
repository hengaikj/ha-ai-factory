# Project Rules

- Follow XZG AI Software Engineering Skill v5.7 lifecycle: Discovery → Requirement → Product → UX/UI → Design Handoff → Contract → Development → Review → Integration → Release.
- Do not skip Requirement, Design, Contract, or Review gates.
- Do not begin business-code development before the applicable Requirement, Design, and Contract gates are approved.
- The Orchestrator manages project state, task assignment, and gates; it must not bypass workflow or approve its own review.
- Contract ambiguities must be reported as `HUMAN_DECISION_REQUIRED`.
- Key classes and functions require Chinese business comments.
- Every database table and column requires a Chinese `COMMENT`.
- Record commit, build, test, review, and known-issue evidence using the repository templates.

## 当前会话持续执行规则

负责人于 2026-09-25 要求在当前会话持续推进全部工作。本节约束执行节奏，不授予尚未批准的业务范围或运行权限。

- 在当前会话维护整体需求清单，逐项记录实现位置、验证证据、缺口和依赖；不得将“全部完成”缩减为当前已实现的部分。
- 对已批准范围内的实现、修复、文档和验证连续执行，不在每次小提交、构建成功或 CI 通过后等待负责人再次发送“继续”。
- 按完整业务流程组织工作批次：核查契约与实现、完成相关修改、执行已授权验证、记录准确证据、中文提交并推送。避免单字段或单行文档更新反复触发完整交付循环。
- 执行期间提供简短进度说明；存在其他可推进工作时，单个审批或外部配置缺失只阻塞依赖它的工作，不结束全部执行。
- 未批准范围先整理具体差异和可审查的决策材料。需要负责人决策时一次汇总缺失信息与影响，继续处理无依赖工作；不得将沉默、等待时间或“继续”解释为具体契约批准。
- CI 通过只证明实际覆盖的检查通过。记录准确的测试数量与覆盖范围，排除旧报告和旧包名残留；API 单元测试不等同于浏览器业务流程验收。
- 独立 Reviewer 批准、阶段 Gate、可信 CI 和分支保护按有效配置执行。当前会话内可准备和修复审查材料，但执行者不得自批、伪造独立审查或绕过受保护分支。
- 无论治理模式如何，真实 Runtime、模型调用、工具副作用、数据库迁移及部署动作均保持失败关闭，直至相应范围、配置、批准及执行授权满足。已有测试环境验证授权只适用于该测试范围。
- 不修改与任务无关的既有工作区内容；提交仅包含本轮负责的文件。不得为了获取干净状态覆盖或提交他人未完成工作。
- 仅在完整目标经逐项核验完成，或所有剩余工作确实依赖外部输入时结束本轮交付。存在外部阻塞时给出具体缺项、所需决策/证据和恢复步骤，保留未完成状态；不重复无变化的状态汇报来代替进展。

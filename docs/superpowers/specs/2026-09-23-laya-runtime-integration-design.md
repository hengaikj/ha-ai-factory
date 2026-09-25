# Laya 推理服务与 Agent Runtime 集成设计

- 文档编号：ADR-20260923-LAYA-RUNTIME
- 状态：架构方向已由项目负责人确认；实现与 Contract 授权待定
- 日期：2026-09-23
- 适用项目：HA AI Software Factory

## 1. 背景与目标

HA AI Software Factory 已有 Python/FastAPI Agent Runtime 方向，但当前 M02 将真实 Runtime 执行和模型调用延期。项目负责人已选择未来采用独立部署的 Laya 推理服务，并由 Agent Runtime 调用。

Laya 是返回类型化决策结果的非自回归模型服务，支持 `choice`、`score` 和 `noul`。它不负责 Factory 项目授权、流程编排或工具执行。目标是为未来经过批准的 Agent Runtime 任务增加可替换的推理能力，同时维持 Java 后端的身份和项目权限边界。

## 2. 决策

未来将 Laya 作为独立的 Python/FastAPI 推理服务运行在内部网络，由 Agent Runtime 经稳定的推理适配器调用。Agent Runtime 保留任务编排、权限前置校验、调用限制、结果验证和审计责任；Laya 仅运行批准版本的模型并返回类型化推理结果。

该决策只确认服务部署与调用拓扑。它不批准当前 M02 启用真实模型调用，不批准任何模型、数据字段、工具或外部副作用，也不改变当前 Contract Gate 状态。HD-002 和 HD-006 的 M02 延期继续有效；要引入 Laya，后续 Contract 必须明确适用范围并审查相应的 Runtime 与服务集成边界。

## 3. 组件与边界

### HA Web 后端（Java/Spring）

- 继续作为 OIDC 用户会话和项目 RBAC 的权威执行点。
- 校验用户对项目、任务和所请求 Runtime 能力的权限。
- 按已批准的 HD-001 服务身份规则向 Agent Runtime 传递受限调用上下文及原始发起人、项目和任务标识。
- 不直接调用 Laya，也不接受 Laya 输出作为授权决定。

### Agent Runtime（Python/FastAPI）

- 验证调用身份、项目/任务归属、Gate/配置状态和任务允许的操作范围。
- 通过 `DecisionModelProvider` 适配器接口调用推理服务，避免将 Laya 专有请求结构扩散到业务流程中。
- 只允许调用经过 Contract 批准的决策任务和问题模板；调用方不能任意指定模型、问题模板或扩大标签集合。
- 校验响应结构、允许值和模型版本；将结果标记为模型推理，不视作人工 Reviewer 或权限主体。
- 对不可用、超时、无效响应或低可信度结果提供人工处理路径，不静默改用未批准模型或触发工具动作。

### 独立 Laya 推理服务

- 单独进程或容器部署，与 Agent Runtime 分开升级、扩缩和配置 GPU/CPU 资源。
- 仅向 Agent Runtime 暴露内部推理入口；不得由浏览器或公网直接访问。
- 不连接 Factory 数据库、Git、工具服务或项目文件仓库；不承担用户认证和项目授权。
- 生产推理所用代码、依赖、模型 checkpoint 和权重摘要须在后续 Contract/供应链审查中锁定。
- 运行时不得依赖从 Hugging Face 或其他公网动态下载模型；模型需由部署流程按批准版本预置到受控位置。

Laya 上游提供 `laya-serve`，以 `POST /v1/systemone` 暴露 HTTP 推理接口，默认监听 `0.0.0.0:8000`，并支持可选 Bearer API Key。部署必须显式限定内部监听和网络策略；单独的静态 API Key 不作为最终服务身份方案。参考：[Laya 自托管服务说明](https://github.com/NandhaKishorM/laya#self-hosting-http-server-jev-compatible)。

## 4. 请求与响应流程

1. 用户在 HA Factory 中请求一项已批准的任务能力。
2. Java 后端执行 OIDC 会话和项目 RBAC 校验，并签发符合 HD-001 的 Runtime 调用身份。
3. Agent Runtime 校验服务身份、项目/任务关联、配置批准状态及任务允许的模型操作。
4. Runtime 从服务端维护的已批准模板中构造类型化问题，将最少必要的任务文本或结构化字段发送至内网 Laya。
5. Laya 返回类型化答案、置信信息和用量信息。
6. Runtime 验证结果 schema、选项集合和调用上下文；审计记录绑定部署配置中锁定的模型版本与权重摘要，并按后续 Contract 批准的规则保留。
7. 结果进入人工复核或其他明确批准的业务流程；不得凭 Laya 结果直接批准 Gate、修改角色/权限或执行外部副作用。

记录请求和结果时优先保留项目/任务标识、配置与模板版本、模型版本/权重摘要、时间、结果类别、错误码和最小必要摘要。原始任务正文、完整提示和敏感字段不得默认写入普通日志；具体留存期限和可记录字段由后续 HD-005 运维/安全 Contract 决定。

## 5. 服务认证与网络安全

- Runtime 到 Laya 的身份验证方式尚待 Contract 决定。候选方案是内部工作负载身份配合 mTLS 或短时服务凭据，并由网络策略限制调用来源；最终方式须与 HD-001 的服务身份体系对齐。
- Laya 接口只在内部服务网络开放。部署检查必须证明公网入口不可达、仅授权 Runtime 工作负载可连接。
- Runtime 使用服务端维护的模型和问题模板 allowlist；不允许用户输入覆盖 Laya 服务地址、模型标识、任意 Python/工具调用或授权规则。
- Laya 输入按不可信数据处理，输出按不可信模型结果处理。模型置信值不得直接等价为安全保证或业务授权。
- 服务日志和指标不得泄露原始任务文本、凭据或跨项目数据。项目级关联信息由 Runtime 依据已验证的调用上下文写入审计。

## 6. 失败与恢复

- Laya 不可用、服务认证失败、超时、响应校验失败或模型版本不匹配时，Runtime 将本次决策标记为失败/待人工处理；不返回成功推理，不自动执行依赖该结果的操作。
- 不对非幂等动作做自动重试。推理重试次数、超时、限流和排队上限须在运行 Contract 中确定。
- 可通过关闭已批准 Runtime 配置或服务路由停止新调用；未完成任务保留失败状态和错误码，人工可按业务流程继续处理。
- 不设置隐式云端模型 fallback。切换到其他模型供应方需独立审批、版本锁定和评测。

## 7. 替代方案

1. **在 Agent Runtime 进程内直接加载 Laya SDK：**适合一次性离线试验，但会把 PyTorch/Transformers 依赖、权重加载、GPU 资源和 Runtime 生命周期耦合在一起；不作为目标生产拓扑。
2. **直接调用托管推理 API：**部署简单，但会引入外部数据处理、网络和凭据边界，与当前 HD-006 延期方向冲突；本设计不采用。
3. **独立内网 Laya 服务：**增加内部服务认证、部署和运维工作，但隔离模型资源、版本和故障域，且可保持 Runtime 与推理实现可替换；采用此方案。

## 8. 质量验证与采用门槛

实现前，后续 Contract 必须明确：

- Laya 的允许业务用途、禁止用途、调用主体和 M02/后续模块范围；明确 HD-002/HD-006 如何更新。
- 可发送的数据字段、脱敏要求、日志字段、留存和删除策略；满足适用隐私/安全约束。
- 模型 checkpoint、仓库 revision、权重摘要、依赖锁文件、许可证与来源审查结果。
- Runtime 到 Laya 的认证、网络策略、密钥轮换、错误码、超时/重试/限流和资源预算。
- 问题模板和允许输出 schema 的管理、审批、版本化及变更审计。
- 由项目负责人批准的离线评测集、语言分层、准确率/校准指标、拒绝或人工升级策略及通过阈值。

上游 README 自述其基础 checkpoint 在 typed-decisions 基准上接近随机水平，并记录了过度自信和按语言路由的限制；因此上游公开 benchmark 只作参考，不能替代 HA Factory 的中文及实际业务评测。参考：[Laya Honest Limits](https://github.com/NandhaKishorM/laya#honest-limits)、[typed-decisions checkpoint card](https://huggingface.co/convaiinnovations/laya-typed-decisions)。代码仓库与模型卡当前标注 Apache-2.0，仍需对具体固定版本及其依赖和权重执行正式许可审查：[代码许可证](https://github.com/NandhaKishorM/laya/blob/main/LICENSE)、[模型卡许可证](https://huggingface.co/convaiinnovations/laya-typed-decisions)。

在 Contract Gate 通过并获得 Runtime 专项 Contract 批准后，进入开发前至少应验证：内网隔离和服务认证、schema/版本校验、日志脱敏、模型不可用时失败关闭、离线评测达标、模型版本回滚，以及关闭服务路由后可恢复人工流程。当前不运行这些测试，也不部署服务。

## 9. 后果

### 正面影响

- Runtime 与模型供应方解耦，后续可在不改 Java API 的情况下替换推理适配器。
- 模型依赖、权重生命周期和硬件需求独立管理。
- 项目授权、工具执行和人工 Gate 决策仍留在 HA Factory 的权威服务中。

### 代价与风险

- 新增需要维护的 Python 服务、模型镜像/权重制品、内部身份和监控边界。
- 服务间网络、模型加载和硬件资源形成新的可用性依赖。
- 上游公开准确率、语言覆盖和校准表现不能代表 HA Factory 自有任务；必须评测和版本锁定。
- 当前 HD-002/HD-006 延期意味着本 ADR 本身不允许接入 M02 或开始实现。

## 10. 相关文件与阶段状态

- 当前 Contract decisions：`contracts/contract-decisions.md`
- 当前 Global Contract Gate：`contracts/contract-gate-approval.md`
- API 草案：`contracts/api/api-contract.yaml`
- 安全契约：`contracts/security/security-contract.md`
- 项目状态：`.agent/state.yaml`

本设计仅记录项目负责人确认的部署方向，并提供下一轮 Runtime Contract 的输入。Global Contract Gate 当前为 RETURN 后待独立复审；本文件不批准修改当前 M02 API/数据库契约、编写实现代码、运行模型或部署服务。

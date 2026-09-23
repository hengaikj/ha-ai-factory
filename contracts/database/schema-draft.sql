-- 数据库契约草案（MySQL 8）。仅用于 Contract Gate 评审，批准前不得执行或据此修改数据库。
-- 权限主体引用、交付物权威来源及 Agent Runtime 审批策略仍须按 contract-decisions.md 决策。

CREATE TABLE projects (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目主键',
    name VARCHAR(128) NOT NULL COMMENT '项目名称',
    description VARCHAR(4000) NULL COMMENT '项目目标与说明',
    tech_stack JSON NULL COMMENT '项目技术栈信息',
    current_phase VARCHAR(64) NOT NULL COMMENT '项目当前生命周期阶段',
    owner_ref VARCHAR(256) NOT NULL COMMENT '项目负责人身份引用，引用格式待身份契约决策',
    created_by_ref VARCHAR(256) NOT NULL COMMENT '创建人身份引用，引用格式待身份契约决策',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    PRIMARY KEY (id),
    KEY idx_projects_name (name),
    KEY idx_projects_phase (current_phase)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 软件工程项目';

CREATE TABLE project_tasks (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务主键',
    project_id BIGINT NOT NULL COMMENT '所属项目主键',
    title VARCHAR(256) NOT NULL COMMENT '任务标题',
    description VARCHAR(8000) NULL COMMENT '任务说明',
    phase VARCHAR(64) NOT NULL COMMENT '任务所属生命周期阶段',
    assignee_ref VARCHAR(256) NULL COMMENT '责任人或 Agent 身份引用',
    assignee_role VARCHAR(64) NULL COMMENT '责任角色标识，权限规则待决策',
    status VARCHAR(40) NOT NULL DEFAULT 'NOT_STARTED' COMMENT '任务状态',
    created_by_ref VARCHAR(256) NOT NULL COMMENT '创建人身份引用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_project_id (project_id, id),
    KEY idx_task_project_status (project_id, status),
    KEY idx_task_assignee (assignee_ref),
    CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目工程任务';

CREATE TABLE deliverables (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '交付物主键',
    project_id BIGINT NOT NULL COMMENT '所属项目主键',
    task_id BIGINT NULL COMMENT '关联任务主键',
    title VARCHAR(256) NOT NULL COMMENT '交付物名称',
    phase VARCHAR(64) NOT NULL COMMENT '交付物所属生命周期阶段',
    version VARCHAR(64) NOT NULL COMMENT '交付物版本标识',
    source_type VARCHAR(32) NOT NULL COMMENT '来源类型，取值契约待交付物权威来源决策',
    source_ref VARCHAR(2048) NOT NULL COMMENT '交付物来源引用，不在此字段存放文件内容或凭据',
    content_sha256 CHAR(64) NULL COMMENT '交付物内容SHA-256摘要',
    review_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT '交付物评审状态',
    created_by_ref VARCHAR(256) NOT NULL COMMENT '提交人身份引用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_deliverable_project_id (project_id, id),
    KEY idx_deliverable_project_status (project_id, review_status),
    KEY idx_deliverable_task (project_id, task_id),
    CONSTRAINT fk_deliverable_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_deliverable_task FOREIGN KEY (project_id, task_id) REFERENCES project_tasks (project_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目阶段交付物';

CREATE TABLE deliverable_reviews (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '交付物评审记录主键',
    deliverable_id BIGINT NOT NULL COMMENT '被评审交付物主键',
    reviewer_ref VARCHAR(256) NOT NULL COMMENT 'Reviewer 身份引用',
    outcome VARCHAR(32) NOT NULL COMMENT '评审结果：APPROVED、RETURNED或CLARIFICATION_REQUIRED',
    comment VARCHAR(4000) NOT NULL COMMENT '评审说明',
    evidence_refs JSON NULL COMMENT '评审证据引用列表',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '评审时间',
    PRIMARY KEY (id),
    KEY idx_deliverable_review_time (deliverable_id, created_at),
    CONSTRAINT fk_deliverable_review_item FOREIGN KEY (deliverable_id) REFERENCES deliverables (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交付物独立评审记录';

CREATE TABLE project_gates (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '阶段门禁主键',
    project_id BIGINT NOT NULL COMMENT '所属项目主键',
    phase VARCHAR(64) NOT NULL COMMENT '门禁对应生命周期阶段',
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING' COMMENT '门禁状态',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_gate_project_phase (project_id, phase),
    UNIQUE KEY uk_gate_project_id (project_id, id),
    CONSTRAINT fk_gate_project FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目阶段质量门禁';

CREATE TABLE gate_checks (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '门禁检查项主键',
    gate_id BIGINT NOT NULL COMMENT '所属门禁主键',
    code VARCHAR(64) NOT NULL COMMENT '检查项编号',
    title VARCHAR(256) NOT NULL COMMENT '检查项名称',
    description VARCHAR(2000) NULL COMMENT '检查项说明',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '检查结果状态',
    reviewer_ref VARCHAR(256) NULL COMMENT '检查人身份引用',
    comment VARCHAR(4000) NULL COMMENT '检查说明',
    evidence_refs JSON NULL COMMENT '检查证据引用列表',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_gate_check_code (gate_id, code),
    CONSTRAINT fk_gate_check_gate FOREIGN KEY (gate_id) REFERENCES project_gates (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='阶段门禁检查项';

CREATE TABLE gate_decisions (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '门禁决定记录主键',
    gate_id BIGINT NOT NULL COMMENT '所属门禁主键',
    reviewer_ref VARCHAR(256) NOT NULL COMMENT '独立 Reviewer 身份引用',
    decision VARCHAR(40) NOT NULL COMMENT '门禁结论：APPROVED、RETURNED或HUMAN_DECISION_REQUIRED',
    comment VARCHAR(4000) NULL COMMENT '门禁结论说明',
    evidence_refs JSON NULL COMMENT '结论证据引用列表',
    decided_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '决定时间',
    PRIMARY KEY (id),
    KEY idx_gate_decision_time (gate_id, decided_at),
    CONSTRAINT fk_gate_decision_gate FOREIGN KEY (gate_id) REFERENCES project_gates (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='阶段门禁决定历史';

CREATE TABLE open_issues (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '待决事项主键',
    project_id BIGINT NOT NULL COMMENT '所属项目主键',
    code VARCHAR(64) NULL COMMENT '待决事项编号',
    title VARCHAR(256) NOT NULL COMMENT '事项标题',
    description VARCHAR(8000) NOT NULL COMMENT '事项描述',
    impact VARCHAR(4000) NOT NULL COMMENT '未决事项影响',
    decision_role VARCHAR(64) NOT NULL COMMENT '建议决策角色',
    status VARCHAR(40) NOT NULL DEFAULT 'OPEN' COMMENT '处理状态',
    decision VARCHAR(4000) NULL COMMENT '人工决策内容',
    created_by_ref VARCHAR(256) NOT NULL COMMENT '创建人身份引用',
    decided_by_ref VARCHAR(256) NULL COMMENT '决策人身份引用',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    decided_at DATETIME(3) NULL COMMENT '决策时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_issue_project_id (project_id, id),
    KEY idx_issue_project_status (project_id, status),
    CONSTRAINT fk_issue_project FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目待决事项与人工决策';

CREATE TABLE project_resources (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目模板或规则主键',
    project_id BIGINT NOT NULL COMMENT '所属项目主键',
    kind VARCHAR(16) NOT NULL COMMENT '资源类型：TEMPLATE或RULE',
    title VARCHAR(256) NOT NULL COMMENT '模板或规则名称',
    phase VARCHAR(64) NOT NULL COMMENT '适用生命周期阶段',
    version VARCHAR(64) NOT NULL COMMENT '资源版本',
    source_ref VARCHAR(2048) NOT NULL COMMENT '权威来源引用，具体来源规则待决策',
    source_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_CONFIRMATION' COMMENT '来源确认状态',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '登记时间',
    PRIMARY KEY (id),
    KEY idx_resource_project_phase (project_id, phase),
    CONSTRAINT fk_resource_project FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目采用的模板与规则索引';

CREATE TABLE runtime_configs (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Agent Runtime 执行配置主键',
    project_id BIGINT NOT NULL COMMENT '所属项目主键',
    status VARCHAR(32) NOT NULL DEFAULT 'UNCONFIGURED' COMMENT '授权状态',
    model_ref VARCHAR(128) NULL COMMENT '获批模型引用',
    tool_refs JSON NULL COMMENT '获批工具范围',
    operation_refs JSON NULL COMMENT '获批操作权限范围',
    secret_ref VARCHAR(512) NULL COMMENT '外部密钥库引用，不保存密钥明文',
    approved_by_ref VARCHAR(256) NULL COMMENT '配置批准人身份引用',
    approved_at DATETIME(3) NULL COMMENT '配置批准时间',
    expires_at DATETIME(3) NULL COMMENT '配置失效时间',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '配置创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '配置更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_runtime_config_project_id (project_id, id),
    KEY idx_runtime_config_project_status (project_id, status),
    CONSTRAINT fk_runtime_config_project FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent Runtime 授权配置';

CREATE TABLE agent_runs (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Agent 执行记录主键',
    project_id BIGINT NOT NULL COMMENT '所属项目主键',
    task_id BIGINT NOT NULL COMMENT '关联任务主键',
    runtime_config_id BIGINT NULL COMMENT '执行时使用的获批配置主键',
    request_key CHAR(36) NOT NULL COMMENT '调用幂等键',
    status VARCHAR(24) NOT NULL COMMENT '执行状态',
    started_by_ref VARCHAR(256) NOT NULL COMMENT '发起人身份引用',
    result_summary VARCHAR(8000) NULL COMMENT '执行结果摘要',
    error_code VARCHAR(128) NULL COMMENT '执行错误代码',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '提交时间',
    started_at DATETIME(3) NULL COMMENT '开始执行时间',
    completed_at DATETIME(3) NULL COMMENT '执行结束时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_agent_run_request (project_id, request_key),
    KEY idx_agent_run_task_status (project_id, task_id, status),
    CONSTRAINT fk_agent_run_task FOREIGN KEY (project_id, task_id) REFERENCES project_tasks (project_id, id),
    CONSTRAINT fk_agent_run_config FOREIGN KEY (project_id, runtime_config_id) REFERENCES runtime_configs (project_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Agent Runtime 执行记录';

CREATE TABLE audit_events (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '审计事件主键',
    project_id BIGINT NOT NULL COMMENT '所属项目主键',
    object_type VARCHAR(32) NOT NULL COMMENT '被变更对象类型',
    object_id BIGINT NOT NULL COMMENT '被变更对象主键',
    action VARCHAR(128) NOT NULL COMMENT '变更动作',
    before_state JSON NULL COMMENT '变更前状态摘要',
    after_state JSON NULL COMMENT '变更后状态摘要',
    actor_ref VARCHAR(256) NOT NULL COMMENT '操作者身份引用',
    comment VARCHAR(4000) NULL COMMENT '变更说明',
    evidence_refs JSON NULL COMMENT '关联证据引用列表',
    occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '事件发生时间',
    PRIMARY KEY (id),
    KEY idx_audit_project_time (project_id, occurred_at),
    KEY idx_audit_object (object_type, object_id),
    CONSTRAINT fk_audit_project FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目操作与状态审计事件';

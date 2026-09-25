CREATE TABLE project_tasks (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务主键',
    project_id BIGINT NOT NULL COMMENT '所属项目主键',
    title VARCHAR(256) NOT NULL COMMENT '任务标题',
    description VARCHAR(8000) NULL COMMENT '任务说明',
    phase VARCHAR(64) NOT NULL COMMENT '任务所属生命周期阶段',
    assignee_ref CHAR(36) NULL COMMENT '责任人或Agent主体UUID',
    assignee_role VARCHAR(64) NULL COMMENT '责任角色标识',
    status VARCHAR(40) NOT NULL DEFAULT 'NOT_STARTED' COMMENT '任务状态',
    created_by_ref CHAR(36) NOT NULL COMMENT '创建人主体UUID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    PRIMARY KEY (id),
    KEY idx_task_project_status (project_id, status),
    CONSTRAINT fk_task_project_v3 FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_task_creator_v3 FOREIGN KEY (created_by_ref) REFERENCES principals (principal_ref),
    CONSTRAINT fk_task_assignee_v3 FOREIGN KEY (assignee_ref) REFERENCES principals (principal_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目工程任务';

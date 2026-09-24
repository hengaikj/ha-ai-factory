CREATE TABLE gate_scope_tasks (
  project_id BIGINT NOT NULL COMMENT '所属项目主键', gate_id BIGINT NOT NULL COMMENT '门禁主键', task_id BIGINT NOT NULL COMMENT '纳入门禁范围的任务主键',
  PRIMARY KEY (gate_id, task_id), CONSTRAINT fk_gate_scope_task_gate FOREIGN KEY (project_id, gate_id) REFERENCES project_gates(project_id, id), CONSTRAINT fk_gate_scope_task_task FOREIGN KEY (task_id) REFERENCES project_tasks(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门禁评审范围内的项目任务';

CREATE TABLE gate_scope_deliverables (
  project_id BIGINT NOT NULL COMMENT '所属项目主键', gate_id BIGINT NOT NULL COMMENT '门禁主键', deliverable_id BIGINT NOT NULL COMMENT '纳入门禁范围的交付物主键',
  PRIMARY KEY (gate_id, deliverable_id), CONSTRAINT fk_gate_scope_deliverable_gate FOREIGN KEY (project_id, gate_id) REFERENCES project_gates(project_id, id), CONSTRAINT fk_gate_scope_deliverable_item FOREIGN KEY (deliverable_id) REFERENCES project_deliverables(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='门禁评审范围内的项目交付物';

CREATE TABLE gate_checks (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '门禁检查项主键', gate_id BIGINT NOT NULL COMMENT '所属门禁主键', code VARCHAR(64) NOT NULL COMMENT '检查项编号', title VARCHAR(256) NOT NULL COMMENT '检查项名称', status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '检查结果状态', reviewer_ref CHAR(36) NULL COMMENT '检查人主体UUID', comment VARCHAR(4000) NULL COMMENT '检查说明', evidence_refs JSON NULL COMMENT '检查证据引用列表', updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (id), UNIQUE KEY uk_gate_check_code(gate_id, code), CONSTRAINT fk_gate_check_gate FOREIGN KEY (gate_id) REFERENCES project_gates(id), CONSTRAINT fk_gate_check_reviewer FOREIGN KEY (reviewer_ref) REFERENCES principals(principal_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='阶段门禁检查项';

CREATE TABLE gate_decisions (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '门禁决定记录主键', gate_id BIGINT NOT NULL COMMENT '所属门禁主键', reviewer_ref CHAR(36) NOT NULL COMMENT '独立Reviewer主体UUID', decision VARCHAR(40) NOT NULL COMMENT '门禁结论', comment VARCHAR(4000) NULL COMMENT '门禁结论说明', decided_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '决定时间',
  PRIMARY KEY(id), KEY idx_gate_decision_time(gate_id, decided_at), CONSTRAINT fk_gate_decision_gate FOREIGN KEY(gate_id) REFERENCES project_gates(id), CONSTRAINT fk_gate_decision_reviewer FOREIGN KEY(reviewer_ref) REFERENCES principals(principal_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='阶段门禁决定历史';

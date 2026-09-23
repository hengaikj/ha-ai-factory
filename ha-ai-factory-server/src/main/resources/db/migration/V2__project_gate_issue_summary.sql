CREATE TABLE project_gates (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '阶段门禁主键',
  project_id BIGINT NOT NULL COMMENT '所属项目主键',
  phase VARCHAR(64) NOT NULL COMMENT '门禁对应生命周期阶段',
  status VARCHAR(40) NOT NULL DEFAULT 'PENDING' COMMENT '门禁状态：PENDING、READY_FOR_REVIEW、APPROVED、RETURNED、BLOCKED或HUMAN_DECISION_REQUIRED',
  submitted_by_ref CHAR(36) NULL COMMENT '提交门禁评审的主体UUID',
  decision_owner_ref CHAR(36) NULL COMMENT '该门禁的决策责任人主体UUID',
  submitted_at DATETIME(3) NULL COMMENT '门禁提交评审时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_gate_project_phase (project_id, phase),
  UNIQUE KEY uk_gate_project_id (project_id, id),
  CONSTRAINT fk_gate_project FOREIGN KEY (project_id) REFERENCES projects (id),
  CONSTRAINT fk_gate_submitter FOREIGN KEY (submitted_by_ref) REFERENCES principals (principal_ref),
  CONSTRAINT fk_gate_decision_owner FOREIGN KEY (decision_owner_ref) REFERENCES principals (principal_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目阶段质量门禁';

CREATE TABLE open_issues (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '待决事项主键',
  project_id BIGINT NOT NULL COMMENT '所属项目主键',
  code VARCHAR(64) NULL COMMENT '待决事项编号',
  title VARCHAR(256) NOT NULL COMMENT '事项标题',
  description TEXT NOT NULL COMMENT '事项描述，接口最多8000字符',
  impact TEXT NOT NULL COMMENT '未决事项影响，接口最多4000字符',
  decision_role VARCHAR(64) NOT NULL COMMENT '建议决策角色',
  status VARCHAR(40) NOT NULL DEFAULT 'OPEN' COMMENT '处理状态：OPEN、HUMAN_DECISION_REQUIRED、DECIDED、TRACKING或CLOSED',
  decision TEXT NULL COMMENT '人工决策内容，接口最多4000字符',
  created_by_ref CHAR(36) NOT NULL COMMENT '创建人主体UUID',
  decided_by_ref CHAR(36) NULL COMMENT '决策人主体UUID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  decided_at DATETIME(3) NULL COMMENT '决策时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_issue_project_id (project_id, id),
  KEY idx_issue_project_status (project_id, status),
  CONSTRAINT fk_issue_project FOREIGN KEY (project_id) REFERENCES projects (id),
  CONSTRAINT fk_issue_creator FOREIGN KEY (created_by_ref) REFERENCES principals (principal_ref),
  CONSTRAINT fk_issue_decider FOREIGN KEY (decided_by_ref) REFERENCES principals (principal_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目待决事项与人工决策';

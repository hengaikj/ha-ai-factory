CREATE TABLE project_deliverables (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '交付物主键',
  project_id BIGINT NOT NULL COMMENT '所属项目主键',
  task_id BIGINT NULL COMMENT '关联任务主键',
  title VARCHAR(256) NOT NULL COMMENT '交付物标题',
  phase VARCHAR(64) NOT NULL COMMENT '交付物所属阶段',
  version VARCHAR(64) NOT NULL COMMENT '交付物版本标识',
  source_ref VARCHAR(2048) NOT NULL COMMENT '仓库中的交付物引用，不是公共上传地址',
  review_status ENUM('DRAFT','PENDING','APPROVED','RETURNED','CLARIFICATION_REQUIRED') NOT NULL DEFAULT 'DRAFT' COMMENT '交付物评审状态',
  created_by_ref CHAR(36) NOT NULL COMMENT '登记交付物的主体UUID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '登记时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (id), KEY idx_deliverable_project_status (project_id, review_status),
  CONSTRAINT fk_deliverable_project FOREIGN KEY (project_id) REFERENCES projects (id),
  CONSTRAINT fk_deliverable_task FOREIGN KEY (task_id) REFERENCES project_tasks (id),
  CONSTRAINT fk_deliverable_creator FOREIGN KEY (created_by_ref) REFERENCES principals (principal_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目仓库引用型交付物登记';

CREATE TABLE deliverable_reviews (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '交付物评审记录主键',
  deliverable_id BIGINT NOT NULL COMMENT '被评审交付物主键',
  reviewer_ref CHAR(36) NOT NULL COMMENT '评审主体UUID',
  outcome ENUM('APPROVED','RETURNED','CLARIFICATION_REQUIRED') NOT NULL COMMENT '评审结论',
  review_comment VARCHAR(4000) NOT NULL COMMENT '评审意见',
  evidence_refs JSON NULL COMMENT '评审证据引用列表',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '评审时间',
  PRIMARY KEY (id), KEY idx_deliverable_review (deliverable_id, created_at),
  CONSTRAINT fk_deliverable_review_deliverable FOREIGN KEY (deliverable_id) REFERENCES project_deliverables (id),
  CONSTRAINT fk_deliverable_review_reviewer FOREIGN KEY (reviewer_ref) REFERENCES principals (principal_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='交付物独立评审记录';

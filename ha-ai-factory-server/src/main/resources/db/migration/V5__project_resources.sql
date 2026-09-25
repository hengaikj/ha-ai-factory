CREATE TABLE project_resources (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目模板或规则主键',
  project_id BIGINT NOT NULL COMMENT '所属项目主键',
  kind VARCHAR(16) NOT NULL COMMENT '资源类型：TEMPLATE或RULE',
  title VARCHAR(256) NOT NULL COMMENT '模板或规则名称',
  phase VARCHAR(64) NOT NULL COMMENT '适用生命周期阶段',
  version VARCHAR(64) NOT NULL COMMENT '资源版本',
  source_ref VARCHAR(2048) NOT NULL COMMENT '仓库文件路径、提交或版本引用',
  source_status VARCHAR(32) NOT NULL DEFAULT 'PENDING_CONFIRMATION' COMMENT '来源确认状态',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '登记时间',
  PRIMARY KEY (id), KEY idx_resource_project_phase (project_id, phase),
  CONSTRAINT fk_resource_project FOREIGN KEY (project_id) REFERENCES projects (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目采用的模板与规则索引';

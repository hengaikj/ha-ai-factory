CREATE TABLE principals (
  principal_ref CHAR(36) NOT NULL COMMENT '系统内部稳定主体UUID',
  principal_type ENUM('HUMAN','SERVICE') NOT NULL COMMENT '主体类型：HUMAN或SERVICE',
  oidc_issuer VARCHAR(512) NULL COMMENT 'OIDC身份提供方issuer；服务主体可为空',
  oidc_subject VARCHAR(512) NULL COMMENT 'OIDC身份subject；服务主体可为空',
  oidc_identity_hash CHAR(64) GENERATED ALWAYS AS (CASE WHEN oidc_issuer IS NULL OR oidc_subject IS NULL THEN NULL ELSE SHA2(CONCAT(LPAD(OCTET_LENGTH(oidc_issuer),4,'0'),':',oidc_issuer,oidc_subject),256) END) STORED COMMENT 'OIDC issuer与subject组合SHA-256索引值',
  display_name VARCHAR(256) NOT NULL COMMENT '主体展示名称',
  is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT '主体是否可用',
  last_authenticated_at DATETIME(3) NULL COMMENT '最近一次OIDC认证时间',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '主体创建时间',
  PRIMARY KEY (principal_ref),
  UNIQUE KEY uk_principal_oidc_hash (oidc_identity_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='OIDC人类身份与服务主体目录';

CREATE TABLE projects (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '项目主键',
  name VARCHAR(128) NOT NULL COMMENT '项目名称',
  description VARCHAR(4000) NULL COMMENT '项目目标与说明',
  tech_stack JSON NULL COMMENT '项目技术栈信息',
  current_phase VARCHAR(64) NOT NULL COMMENT '项目当前生命周期阶段',
  owner_ref CHAR(36) NOT NULL COMMENT '当前主要项目Owner主体UUID，必须对应有效项目成员',
  created_by_ref CHAR(36) NOT NULL COMMENT '项目创建人主体UUID',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
  PRIMARY KEY (id),
  KEY idx_projects_name (name),
  CONSTRAINT fk_project_owner FOREIGN KEY (owner_ref) REFERENCES principals (principal_ref),
  CONSTRAINT fk_project_creator FOREIGN KEY (created_by_ref) REFERENCES principals (principal_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI软件工程项目';

CREATE TABLE project_members (
  project_id BIGINT NOT NULL COMMENT '所属项目主键',
  principal_ref CHAR(36) NOT NULL COMMENT '项目成员主体UUID',
  membership_status ENUM('ACTIVE','REVOKED') NOT NULL DEFAULT 'ACTIVE' COMMENT '成员状态：ACTIVE或REVOKED',
  joined_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '加入项目时间',
  revoked_at DATETIME(3) NULL COMMENT '成员权限撤销时间',
  PRIMARY KEY (project_id, principal_ref),
  KEY idx_project_member_status (project_id, membership_status),
  CONSTRAINT fk_project_member_project FOREIGN KEY (project_id) REFERENCES projects (id),
  CONSTRAINT fk_project_member_principal FOREIGN KEY (principal_ref) REFERENCES principals (principal_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目成员及成员资格生命周期';

CREATE TABLE project_member_roles (
  project_id BIGINT NOT NULL COMMENT '所属项目主键',
  principal_ref CHAR(36) NOT NULL COMMENT '项目成员主体UUID',
  role_code ENUM('OWNER','PROJECT_ADMIN','ORCHESTRATOR','ENGINEER','REVIEWER') NOT NULL COMMENT '项目角色：OWNER、PROJECT_ADMIN、ORCHESTRATOR、ENGINEER或REVIEWER',
  assigned_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '角色授予时间',
  assigned_by_ref CHAR(36) NOT NULL COMMENT '授予角色的主体UUID',
  PRIMARY KEY (project_id, principal_ref, role_code),
  CONSTRAINT fk_member_role_membership FOREIGN KEY (project_id, principal_ref) REFERENCES project_members (project_id, principal_ref),
  CONSTRAINT fk_member_role_assigner FOREIGN KEY (assigned_by_ref) REFERENCES principals (principal_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='项目成员可组合的角色授予记录';

package com.hengaikj.ai.factory.project;

import java.util.List;
import java.util.Map;
import java.time.Instant;

/** 项目与OIDC主体持久化边界，便于替换存储实现且由数据库事务保证Owner原子创建。 */
public interface ProjectRepository {
    PrincipalRecord resolveOidcPrincipal(String issuer, String subject, String displayName);
    ProjectRecord createProject(String principalRef, String name, String description, Map<String, String> techStack);
    ProjectPage listProjects(String principalRef, String query, int page, int pageSize);

    record PrincipalRecord(String principalRef, String displayName) {}
    record ProjectRecord(long id, String name, String description, Map<String, String> techStack,
                         String ownerRef, String currentPhase, String gateStatus, int openIssueCount,
                         Instant createdAt, Instant updatedAt) {}
    record ProjectPage(List<ProjectRecord> items, int page, int pageSize, long total) {}
}

package com.hengaikj.ai.factory.project;

import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.util.Set;

/** 项目与OIDC主体持久化边界，便于替换存储实现且由数据库事务保证Owner原子创建。 */
public interface ProjectRepository {
    PrincipalRecord resolveOidcPrincipal(String issuer, String subject, String displayName);
    String findActivePrincipal(String issuer, String subject);
    ProjectRecord createProject(String principalRef, String name, String description, Map<String, String> techStack);
    ProjectPage listProjects(String principalRef, String query, int page, int pageSize);
    List<MemberRecord> listMembers(String principalRef, long projectId);
    MemberRecord addMember(String principalRef, long projectId, String targetPrincipalRef, Set<String> roles);
    MemberRecord replaceMemberRoles(String principalRef, long projectId, String targetPrincipalRef, Set<String> roles);
    void removeMember(String principalRef, long projectId, String targetPrincipalRef);
    TaskPage listTasks(String principalRef, long projectId, String status, int page, int pageSize);
    TaskRecord createTask(String principalRef, long projectId, String title, String description, String phase, String assigneeRef, String assigneeRole);
    TaskRecord getTask(String principalRef, long taskId);
    TaskRecord updateTask(String principalRef, long taskId, String title, String description, String assigneeRef, String assigneeRole, String status);
    DeliverablePage listDeliverables(String principalRef, long projectId, int page, int pageSize);
    DeliverableRecord createDeliverable(String principalRef, long projectId, Long taskId, String title, String phase, String version, String sourceRef);
    DeliverableReviewRecord reviewDeliverable(String principalRef, long deliverableId, String outcome, String comment, String evidenceRefs);
    List<IssueRecord> listIssues(String principalRef, long projectId);
    IssueRecord createIssue(String principalRef, long projectId, String code, String title, String description, String impact, String decisionRole, String status);
    IssueRecord decideIssue(String principalRef, long issueId, String decision, String status);

    record PrincipalRecord(String principalRef, String displayName) {}
    record ProjectRecord(long id, String name, String description, Map<String, String> techStack,
                         String ownerRef, String currentPhase, String gateStatus, int openIssueCount,
                         Instant createdAt, Instant updatedAt) {}
    record ProjectPage(List<ProjectRecord> items, int page, int pageSize, long total) {}
    record MemberRecord(String principalRef, String displayName, Set<String> roles, Instant joinedAt) {}
    record TaskRecord(long id, long projectId, String title, String description, String phase, String assigneeRef, String assigneeRole, String status, Instant createdAt, Instant updatedAt) {}
    record TaskPage(List<TaskRecord> items, int page, int pageSize, long total) {}
    record DeliverableRecord(long id, long projectId, Long taskId, String title, String phase, String version, String sourceRef, String reviewStatus, Instant createdAt) {}
    record DeliverablePage(List<DeliverableRecord> items, int page, int pageSize, long total) {}
    record DeliverableReviewRecord(long id, String reviewerRef, String outcome, String comment, String evidenceRefs, Instant createdAt) {}
    record IssueRecord(long id, long projectId, String code, String title, String description, String impact, String decisionRole, String status, String decision, Instant createdAt, Instant decidedAt) {}
}

package com.hengaikj.ai.factory.project;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;

/** 基于MyBatis mapper的MySQL持久化实现。 */
@Repository
public class MybatisProjectRepository implements ProjectRepository {
    private final PrincipalMapper principals;
    private final ProjectMapper projects;
    private final ProjectMembershipMapper memberships;
    private final ProjectTaskMapper tasks;
    private final ObjectMapper json;

    public MybatisProjectRepository(PrincipalMapper principals, ProjectMapper projects,
                                    ProjectMembershipMapper memberships, ProjectTaskMapper tasks, ObjectMapper json) {
        this.principals = principals;
        this.projects = projects;
        this.memberships = memberships;
        this.tasks = tasks;
        this.json = json;
    }

    /** 将已通过OIDC认证的身份映射为系统主体，不接受客户端提供的issuer或subject。 */
    @Override
    @Transactional
    public PrincipalRecord resolveOidcPrincipal(String issuer, String subject, String displayName) {
        principals.upsertOidcHuman(UUID.randomUUID().toString(), issuer, subject, displayName);
        String ref = principals.findActiveRef(issuer, subject);
        if (ref == null) throw new AccessDeniedException("主体未注册或已停用");
        return new PrincipalRecord(ref, displayName);
    }

    /** 解析成员管理请求中的OIDC身份，不接受客户端伪造内部主体UUID。 */
    @Override
    public String findActivePrincipal(String issuer, String subject) {
        return principals.findActiveRefByOidc(issuer, subject);
    }

    /** 在单一事务内创建项目、活动Owner成员及OWNER角色。 */
    @Override
    @Transactional
    public ProjectRecord createProject(String principalRef, String name, String description, Map<String, String> techStack) {
        ProjectRow row = new ProjectRow();
        row.setName(name);
        row.setDescription(description);
        row.setTechStack(serialize(techStack));
        row.setOwnerRef(principalRef);
        projects.insert(row);
        memberships.insertActiveMember(row.getId(), principalRef);
        memberships.insertOwnerRole(row.getId(), principalRef);
        var now = java.time.Instant.now();
        return new ProjectRecord(row.getId(), name, description, techStack, principalRef, "DISCOVERY", "PENDING", 0, now, now);
    }

    /** 只查询调用者拥有活动成员资格的项目。 */
    @Override
    public ProjectPage listProjects(String principalRef, String query, int page, int pageSize) {
        String normalized = query == null || query.isBlank() ? null : query;
        int offset = Math.multiplyExact(page - 1, pageSize);
        List<ProjectRecord> items = projects.listActiveForPrincipal(principalRef, normalized, pageSize, offset)
                .stream().map(this::toRecord).toList();
        return new ProjectPage(items, page, pageSize, projects.countActiveForPrincipal(principalRef, normalized));
    }

    /** 仅向项目Owner或管理员暴露活动成员和角色。 */
    @Override
    public List<MemberRecord> listMembers(String principalRef, long projectId) {
        requireProjectAdmin(principalRef, projectId);
        return memberships.listActive(projectId).stream().map(row -> new MemberRecord(row.getPrincipalRef(), row.getDisplayName(), roles(row.getRoles()), row.getJoinedAt())).toList();
    }

    /** 在事务内激活已认证主体并写入其全部项目角色。 */
    @Override
    @Transactional
    public MemberRecord addMember(String principalRef, long projectId, String targetPrincipalRef, Set<String> roles) {
        requireProjectAdmin(principalRef, projectId);
        validateRoles(roles);
        if (memberships.countActivePrincipal(targetPrincipalRef) == 0) throw new AccessDeniedException("目标主体尚未完成企业身份认证");
        memberships.activateMember(projectId, targetPrincipalRef);
        replaceRoles(projectId, targetPrincipalRef, roles, principalRef);
        return member(projectId, targetPrincipalRef);
    }

    /** 替换角色前校验Owner保留规则，角色变化立即生效。 */
    @Override
    @Transactional
    public MemberRecord replaceMemberRoles(String principalRef, long projectId, String targetPrincipalRef, Set<String> roles) {
        requireProjectAdmin(principalRef, projectId);
        validateRoles(roles);
        if (!roles.contains("OWNER") && memberships.countOwners(projectId) <= 1 && memberships.listActive(projectId).stream().anyMatch(m -> m.getPrincipalRef().equals(targetPrincipalRef) && roles(m.getRoles()).contains("OWNER"))) {
            throw new IllegalArgumentException("项目必须至少保留一名Owner");
        }
        replaceRoles(projectId, targetPrincipalRef, roles, principalRef);
        return member(projectId, targetPrincipalRef);
    }

    /** 撤销成员资格并禁止移除项目最后一名Owner。 */
    @Override
    @Transactional
    public void removeMember(String principalRef, long projectId, String targetPrincipalRef) {
        requireProjectAdmin(principalRef, projectId);
        boolean owner = memberships.listActive(projectId).stream().anyMatch(m -> m.getPrincipalRef().equals(targetPrincipalRef) && roles(m.getRoles()).contains("OWNER"));
        if (owner && memberships.countOwners(projectId) <= 1) throw new IllegalArgumentException("项目必须至少保留一名Owner");
        if (memberships.revokeMember(projectId, targetPrincipalRef) == 0) throw new IllegalArgumentException("成员不存在或已被移除");
        if (owner) memberships.updateOwnerRef(projectId, memberships.listActive(projectId).stream().filter(m -> roles(m.getRoles()).contains("OWNER")).findFirst().orElseThrow().getPrincipalRef());
    }

    /** 复用服务端角色查询，禁止客户端绕过项目管理权限。 */
    private void requireProjectAdmin(String principalRef, long projectId) {
        if (memberships.countProjectAdmin(projectId, principalRef) == 0) throw new AccessDeniedException("需要Owner或Project Admin角色");
    }

    private void validateRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty() || !roles.stream().allMatch(Set.of("OWNER", "PROJECT_ADMIN", "ORCHESTRATOR", "ENGINEER", "REVIEWER")::contains)) throw new IllegalArgumentException("角色集合无效");
    }

    private void replaceRoles(long projectId, String target, Set<String> roles, String assignedBy) {
        memberships.deleteRoles(projectId, target);
        roles.forEach(role -> memberships.insertRole(projectId, target, role, assignedBy));
        if (roles.contains("OWNER")) memberships.updateOwnerRef(projectId, target);
    }

    private MemberRecord member(long projectId, String target) {
        return memberships.listActive(projectId).stream().filter(m -> m.getPrincipalRef().equals(target)).findFirst()
                .map(m -> new MemberRecord(m.getPrincipalRef(), m.getDisplayName(), roles(m.getRoles()), m.getJoinedAt()))
                .orElseThrow(() -> new IllegalArgumentException("成员不存在"));
    }

    private Set<String> roles(String raw) { return raw == null || raw.isBlank() ? Set.of() : new LinkedHashSet<>(Arrays.asList(raw.split(","))); }

    /** 查询项目任务，读取权限限定在活动项目成员。 */
    @Override
    public TaskPage listTasks(String principalRef, long projectId, String status, int page, int pageSize) {
        if (memberships.countActiveMember(projectId, principalRef) == 0) throw new AccessDeniedException("不是项目活动成员");
        int offset = Math.multiplyExact(page - 1, pageSize);
        return new TaskPage(tasks.list(projectId, status, pageSize, offset).stream().map(this::task).toList(), page, pageSize, tasks.count(projectId, status));
    }

    /** 创建任务，仅项目Owner、管理员或Orchestrator可执行。 */
    @Override
    @Transactional
    public TaskRecord createTask(String principalRef, long projectId, String title, String description, String phase, String assigneeRef, String assigneeRole) {
        if (memberships.countTaskManager(projectId, principalRef) == 0) throw new AccessDeniedException("需要Owner、Project Admin或Orchestrator角色");
        TaskRow row = new TaskRow(); row.setProjectId(projectId); row.setTitle(title); row.setDescription(description); row.setPhase(phase); row.setAssigneeRef(assigneeRef); row.setAssigneeRole(assigneeRole); row.setCreatedByRef(principalRef); tasks.insert(row);
        return task(tasks.find(row.getId()));
    }

    /** 读取任务详情并校验项目成员边界。 */
    @Override
    public TaskRecord getTask(String principalRef, long taskId) {
        TaskRow row = tasks.find(taskId); if (row == null) throw new IllegalArgumentException("任务不存在");
        if (memberships.countActiveMember(row.getProjectId(), principalRef) == 0) throw new AccessDeniedException("不是项目活动成员");
        return task(row);
    }

    /** 更新任务快照，仅编排角色可以改变任务分配和状态。 */
    @Override
    @Transactional
    public TaskRecord updateTask(String principalRef, long taskId, String title, String description, String assigneeRef, String assigneeRole, String status) {
        TaskRow row = tasks.find(taskId); if (row == null) throw new IllegalArgumentException("任务不存在");
        if (memberships.countTaskManager(row.getProjectId(), principalRef) == 0) throw new AccessDeniedException("需要Owner、Project Admin或Orchestrator角色");
        row.setTitle(title == null ? row.getTitle() : title); row.setDescription(description == null ? row.getDescription() : description); row.setAssigneeRef(assigneeRef == null ? row.getAssigneeRef() : assigneeRef); row.setAssigneeRole(assigneeRole == null ? row.getAssigneeRole() : assigneeRole); row.setStatus(status == null ? row.getStatus() : status); tasks.update(row);
        return task(tasks.find(taskId));
    }

    private TaskRecord task(TaskRow row) { return new TaskRecord(row.getId(), row.getProjectId(), row.getTitle(), row.getDescription(), row.getPhase(), row.getAssigneeRef(), row.getAssigneeRole(), row.getStatus(), row.getCreatedAt(), row.getUpdatedAt()); }

    /** 将技术栈映射序列化为MySQL JSON字段内容。 */
    private String serialize(Map<String, String> value) {
        try { return value == null ? null : json.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("techStack无法序列化", e); }
    }

    /** 将数据库行转换为接口层项目快照并解析技术栈JSON。 */
    private ProjectRecord toRecord(ProjectRow row) {
        Map<String, String> stack;
        try { stack = row.getTechStack() == null ? Map.of() : json.readValue(row.getTechStack(), new TypeReference<>() {}); }
        catch (JsonProcessingException e) { throw new IllegalStateException("项目techStack数据格式无效", e); }
        return new ProjectRecord(row.getId(), row.getName(), row.getDescription(), stack, row.getOwnerRef(),
                row.getCurrentPhase(), row.getGateStatus(), row.getOpenIssueCount(), row.getCreatedAt(), row.getUpdatedAt());
    }
}

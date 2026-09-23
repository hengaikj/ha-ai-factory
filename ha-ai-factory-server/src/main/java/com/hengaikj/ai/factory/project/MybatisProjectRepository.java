package com.hengaikj.ai.factory.project;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;

/** 基于MyBatis mapper的MySQL持久化实现。 */
@Repository
public class MybatisProjectRepository implements ProjectRepository {
    private final PrincipalMapper principals;
    private final ProjectMapper projects;
    private final ProjectMembershipMapper memberships;
    private final ObjectMapper json;

    public MybatisProjectRepository(PrincipalMapper principals, ProjectMapper projects,
                                    ProjectMembershipMapper memberships, ObjectMapper json) {
        this.principals = principals;
        this.projects = projects;
        this.memberships = memberships;
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

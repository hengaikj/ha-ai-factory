package com.hengaikj.ai.factory.project;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;

/** 基于MyBatis mapper的MySQL持久化实现。 */
@Repository
public class MybatisProjectRepository implements ProjectRepository {
    private static final List<String> LIFECYCLE_PHASES = List.of("DISCOVERY", "REQUIREMENT", "PRODUCT", "UX/UI", "DESIGN_HANDOFF", "CONTRACT", "DEVELOPMENT", "REVIEW", "INTEGRATION", "RELEASE");
    private static final Set<String> DELIVERABLE_REVIEW_OUTCOMES = Set.of("APPROVED", "RETURNED", "CLARIFICATION_REQUIRED");
    private static final Set<String> ISSUE_CREATE_STATUSES = Set.of("OPEN", "HUMAN_DECISION_REQUIRED");
    private static final Set<String> ISSUE_DECISION_STATUSES = Set.of("OPEN", "HUMAN_DECISION_REQUIRED", "DECIDED", "TRACKING", "CLOSED");
    private static final Set<String> GATE_CHECK_STATUSES = Set.of("PASSED", "FAILED", "CLARIFICATION_REQUIRED");
    private static final Set<String> GATE_DECISIONS = Set.of("APPROVED", "RETURNED", "HUMAN_DECISION_REQUIRED");
    private final PrincipalMapper principals;
    private final ProjectMapper projects;
    private final ProjectMembershipMapper memberships;
    private final ProjectTaskMapper tasks;
    private final ProjectDeliverableMapper deliverables;
    private final ProjectIssueMapper issues;
    private final ProjectResourceMapper resources;
    private final ProjectGateMapper gates;
    private final RuntimeConfigMapper runtimeConfigs;
    private final AuditEventMapper auditEvents;
    private final ObjectMapper json;

    public MybatisProjectRepository(PrincipalMapper principals, ProjectMapper projects,
                                    ProjectMembershipMapper memberships, ProjectTaskMapper tasks, ProjectDeliverableMapper deliverables, ProjectIssueMapper issues, ProjectResourceMapper resources, ProjectGateMapper gates, RuntimeConfigMapper runtimeConfigs, AuditEventMapper auditEvents, ObjectMapper json) {
        this.principals = principals;
        this.projects = projects;
        this.memberships = memberships;
        this.tasks = tasks;
        this.deliverables = deliverables;
        this.issues = issues;
        this.resources = resources;
        this.gates = gates;
        this.runtimeConfigs = runtimeConfigs;
        this.auditEvents = auditEvents;
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
        GateRow initialGate = new GateRow();
        initialGate.setProjectId(row.getId());
        initialGate.setPhase("DISCOVERY");
        gates.ensure(initialGate);
        AuditEventRow event = new AuditEventRow(); event.setProjectId(row.getId()); event.setObjectType("PROJECT"); event.setObjectId(row.getId()); event.setAction("PROJECT_CREATED"); event.setAfterState("{\"name\":\"" + name.replace("\"", "\\\"") + "\"}"); event.setActorRef(principalRef); auditEvents.insert(event);
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

    /** 读取单个项目详情，访问边界仍由活动成员关系约束。 */
    @Override
    public ProjectRecord getProject(String principalRef, long projectId) {
        ProjectRow row = projects.findActiveForPrincipal(principalRef, projectId);
        if (row == null) throw new AccessDeniedException("项目不存在或当前主体无权访问");
        return toRecord(row);
    }

    /** 仅项目Owner或管理员可修改元数据，更新后返回服务端最新快照。 */
    @Override @Transactional
    public ProjectRecord updateProject(String principalRef, long projectId, String name, String description, Map<String, String> techStack) {
        requireProjectAdmin(principalRef, projectId);
        if (name != null && name.isBlank()) throw new IllegalArgumentException("项目名称不能为空");
        projects.updateMetadata(projectId, name == null ? null : name.trim(), description, techStack == null ? null : serialize(techStack));
        return getProject(principalRef, projectId);
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
        audit(projectId, "MEMBER", targetPrincipalRef.hashCode(), "MEMBER_ADDED", null, "{\"roles\":" + quote(roles.toString()) + "}", principalRef, null, null);
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
        audit(projectId, "MEMBER", targetPrincipalRef.hashCode(), "MEMBER_ROLES_REPLACED", null, "{\"roles\":" + quote(roles.toString()) + "}", principalRef, null, null);
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
        audit(projectId, "MEMBER", targetPrincipalRef.hashCode(), "MEMBER_REVOKED", "{\"status\":\"ACTIVE\"}", "{\"status\":\"REVOKED\"}", principalRef, null, null);
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
        audit(projectId, "TASK", row.getId(), "TASK_CREATED", null, "{\"status\":\"NOT_STARTED\"}", principalRef, null, null);
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
        if (status != null && !Set.of("NOT_STARTED", "IN_PROGRESS", "READY_FOR_REVIEW", "COMPLETED", "RETURNED", "BLOCKED", "HUMAN_DECISION_REQUIRED").contains(status)) throw new IllegalArgumentException("任务状态无效");
        String beforeStatus = row.getStatus();
        row.setTitle(title == null ? row.getTitle() : title); row.setDescription(description == null ? row.getDescription() : description); row.setAssigneeRef(assigneeRef == null ? row.getAssigneeRef() : assigneeRef); row.setAssigneeRole(assigneeRole == null ? row.getAssigneeRole() : assigneeRole); row.setStatus(status == null ? row.getStatus() : status); tasks.update(row);
        if (!beforeStatus.equals(row.getStatus())) audit(row.getProjectId(), "TASK", row.getId(), "TASK_STATUS_CHANGED", "{\"status\":" + quote(beforeStatus) + "}", "{\"status\":" + quote(row.getStatus()) + "}", principalRef, null, null);
        return task(tasks.find(taskId));
    }

    /** 查询项目交付物登记记录，文件内容仍由仓库引用负责。 */
    @Override
    public DeliverablePage listDeliverables(String principalRef, long projectId, int page, int pageSize) {
        if (memberships.countActiveMember(projectId, principalRef) == 0) throw new AccessDeniedException("不是项目活动成员");
        int offset = Math.multiplyExact(page - 1, pageSize);
        return new DeliverablePage(deliverables.list(projectId, pageSize, offset).stream().map(this::deliverable).toList(), page, pageSize, deliverables.count(projectId));
    }

    /** 登记仓库引用型交付物，仅项目任务管理角色可登记。 */
    @Override
    @Transactional
    public DeliverableRecord createDeliverable(String principalRef, long projectId, Long taskId, String title, String phase, String version, String sourceRef) {
        if (memberships.countTaskManager(projectId, principalRef) == 0) throw new AccessDeniedException("需要Owner、Project Admin或Orchestrator角色");
        DeliverableRow row = new DeliverableRow(); row.setProjectId(projectId); row.setTaskId(taskId); row.setTitle(title); row.setPhase(phase); row.setVersion(version); row.setSourceRef(sourceRef); row.setCreatedByRef(principalRef); deliverables.insert(row);
        audit(projectId, "DELIVERABLE", row.getId(), "DELIVERABLE_REGISTERED", null, "{\"reviewStatus\":\"PENDING\"}", principalRef, null, null);
        return deliverable(deliverables.find(row.getId()));
    }

    /** 记录独立评审，评审人必须具备Reviewer角色且不能是交付物登记人。 */
    @Override
    @Transactional
    public DeliverableReviewRecord reviewDeliverable(String principalRef, long deliverableId, String outcome, String comment, String evidenceRefs) {
        requireAllowed(outcome, DELIVERABLE_REVIEW_OUTCOMES, "交付物评审结论无效");
        DeliverableRow deliverable = deliverables.find(deliverableId); if (deliverable == null) throw new IllegalArgumentException("交付物不存在");
        if (memberships.countReviewer(deliverable.getProjectId(), principalRef) == 0) throw new AccessDeniedException("需要项目Reviewer角色");
        if (principalRef.equals(deliverable.getCreatedByRef())) throw new AccessDeniedException("交付物登记人不能担任独立评审人");
        ReviewRow review = new ReviewRow(); review.setDeliverableId(deliverableId); review.setReviewerRef(principalRef); review.setOutcome(outcome); review.setComment(comment); review.setEvidenceRefs(evidenceRefs); deliverables.insertReview(review);
        deliverables.updateStatus(deliverableId, outcome); audit(deliverable.getProjectId(), "DELIVERABLE", deliverableId, "DELIVERABLE_REVIEWED", "{\"reviewStatus\":" + quote(deliverable.getReviewStatus()) + "}", "{\"reviewStatus\":" + quote(outcome) + "}", principalRef, comment, evidenceRefs); return new DeliverableReviewRecord(review.getId(), principalRef, outcome, comment, evidenceRefs, java.time.Instant.now());
    }

    private DeliverableRecord deliverable(DeliverableRow row) { return new DeliverableRecord(row.getId(), row.getProjectId(), row.getTaskId(), row.getTitle(), row.getPhase(), row.getVersion(), row.getSourceRef(), row.getReviewStatus(), row.getCreatedAt()); }

    /** 查询项目Open Issue，事项内容仅对活动项目成员可见。 */
    @Override
    public List<IssueRecord> listIssues(String principalRef, long projectId) {
        if (memberships.countActiveMember(projectId, principalRef) == 0) throw new AccessDeniedException("不是项目活动成员");
        return issues.list(projectId).stream().map(this::issue).toList();
    }

    /** 创建项目待决事项。 */
    @Override
    @Transactional
    public IssueRecord createIssue(String principalRef, long projectId, String code, String title, String description, String impact, String decisionRole, String status) {
        if (memberships.countTaskManager(projectId, principalRef) == 0) throw new AccessDeniedException("需要Owner、Project Admin或Orchestrator角色");
        String effectiveStatus = status == null ? "OPEN" : status;
        requireAllowed(effectiveStatus, ISSUE_CREATE_STATUSES, "待决事项初始状态无效");
        IssueRow row = new IssueRow(); row.setProjectId(projectId); row.setCode(code); row.setTitle(title); row.setDescription(description); row.setImpact(impact); row.setDecisionRole(decisionRole); row.setStatus(effectiveStatus); row.setCreatedByRef(principalRef); issues.insert(row); audit(projectId, "ISSUE", row.getId(), "ISSUE_CREATED", null, "{\"status\":" + quote(effectiveStatus) + "}", principalRef, null, null); return issue(issues.find(row.getId()));
    }

    /** 记录人工决策并推进事项状态。 */
    @Override
    @Transactional
    public IssueRecord decideIssue(String principalRef, long issueId, String decision, String status) {
        IssueRow row = issues.find(issueId); if (row == null) throw new IllegalArgumentException("事项不存在");
        if (memberships.countActiveMember(row.getProjectId(), principalRef) == 0) throw new AccessDeniedException("不是项目活动成员");
        requireAllowed(status, ISSUE_DECISION_STATUSES, "待决事项决策状态无效");
        String beforeStatus = row.getStatus(); row.setDecision(decision); row.setStatus(status); row.setDecidedByRef(principalRef); issues.decide(row); audit(row.getProjectId(), "ISSUE", row.getId(), "ISSUE_DECIDED", "{\"status\":" + quote(beforeStatus) + "}", "{\"status\":" + quote(status) + "}", principalRef, decision, null); return issue(issues.find(issueId));
    }

    private IssueRecord issue(IssueRow row) { return new IssueRecord(row.getId(), row.getProjectId(), row.getCode(), row.getTitle(), row.getDescription(), row.getImpact(), row.getDecisionRole(), row.getStatus(), row.getDecision(), row.getCreatedAt(), row.getDecidedAt()); }

    /** 查询项目采用的模板和规则索引，不读取仓库文件内容。 */
    @Override
    public List<ResourceRecord> listResources(String principalRef, long projectId, String phase, String kind) {
        if (memberships.countActiveMember(projectId, principalRef) == 0) throw new AccessDeniedException("不是项目活动成员");
        return resources.list(projectId, phase, kind).stream().map(r -> new ResourceRecord(r.getId(), r.getKind(), r.getTitle(), r.getPhase(), r.getVersion(), r.getSourceRef(), r.getSourceStatus(), r.getCreatedAt())).toList();
    }

    /** 查询项目Gate及其检查项。 */
    @Override public List<GateRecord> listGates(String principalRef, long projectId) {
        if (memberships.countActiveMember(projectId, principalRef) == 0) throw new AccessDeniedException("不是项目活动成员");
        return gates.list(projectId).stream().map(this::gate).toList();
    }
    /** 查询单个Gate。 */
    @Override public GateRecord getGate(String principalRef, long gateId) {
        GateRow row = gates.find(gateId); if (row == null) throw new IllegalArgumentException("Gate不存在");
        if (memberships.countActiveMember(row.getProjectId(), principalRef) == 0) throw new AccessDeniedException("不是项目活动成员"); return gate(row);
    }
    /** 提交Gate并固定任务/交付物范围。 */
    @Override @Transactional public GateRecord submitGate(String principalRef, long gateId, String decisionOwnerRef, List<Long> taskIds, List<Long> deliverableIds) {
        GateRow row = gates.find(gateId); if (row == null) throw new IllegalArgumentException("Gate不存在");
        if (memberships.countTaskManager(row.getProjectId(), principalRef) == 0) throw new AccessDeniedException("需要项目编排角色");
        if (!"PENDING".equals(row.getStatus()) && !"RETURNED".equals(row.getStatus())) throw new IllegalArgumentException("当前Gate不可重新提交");
        if (memberships.countActiveMember(row.getProjectId(), decisionOwnerRef) == 0) throw new IllegalArgumentException("决策责任人不是活动成员");
        if (taskIds.isEmpty() && deliverableIds.isEmpty()) throw new IllegalArgumentException("Gate至少需要一个任务或交付物");
        if (new HashSet<>(taskIds).size() != taskIds.size() || new HashSet<>(deliverableIds).size() != deliverableIds.size()) throw new IllegalArgumentException("Gate范围不能包含重复对象");
        taskIds.forEach(id -> {
            TaskRow task = tasks.find(id);
            if (task == null || task.getProjectId() != row.getProjectId()) throw new IllegalArgumentException("Gate任务不属于当前项目");
        });
        deliverableIds.forEach(id -> {
            DeliverableRow deliverable = deliverables.find(id);
            if (deliverable == null || deliverable.getProjectId() != row.getProjectId()) throw new IllegalArgumentException("Gate交付物不属于当前项目");
        });
        row.setSubmittedByRef(principalRef); row.setDecisionOwnerRef(decisionOwnerRef); gates.submit(row);
        gates.deleteTaskScopes(gateId); gates.deleteDeliverableScopes(gateId);
        taskIds.forEach(id -> gates.addTaskScope(row.getProjectId(), gateId, id)); deliverableIds.forEach(id -> gates.addDeliverableScope(row.getProjectId(), gateId, id));
        audit(row.getProjectId(), "GATE", gateId, "GATE_SUBMITTED", "{\"status\":" + quote(row.getStatus()) + "}", "{\"status\":\"READY_FOR_REVIEW\"}", principalRef, null, null);
        return gate(gates.find(gateId));
    }
    /** 记录独立Reviewer检查结论并执行冲突校验。 */
    @Override @Transactional public GateCheckRecord decideGateCheck(String principalRef, long gateId, long checkId, String status, String comment, String evidenceRefs) {
        requireAllowed(status, GATE_CHECK_STATUSES, "Gate检查结论无效");
        GateRow gate = gates.find(gateId); if (gate == null || !"READY_FOR_REVIEW".equals(gate.getStatus())) throw new IllegalArgumentException("Gate未进入评审状态");
        if (memberships.countReviewer(gate.getProjectId(), principalRef) == 0 || gates.countConflict(gateId, principalRef) > 0) throw new AccessDeniedException("Reviewer独立性校验未通过");
        GateCheckRow row = gates.checks(gateId).stream().filter(c -> c.getId().equals(checkId)).findFirst().orElseThrow(() -> new IllegalArgumentException("检查项不存在"));
        String beforeStatus = row.getStatus(); row.setReviewerRef(principalRef); row.setStatus(status); row.setComment(comment); row.setEvidenceRefs(evidenceRefs); gates.decideCheck(row); audit(gate.getProjectId(), "GATE_CHECK", checkId, "GATE_CHECK_DECIDED", "{\"status\":" + quote(beforeStatus) + "}", "{\"status\":" + quote(status) + "}", principalRef, comment, evidenceRefs); return check(row);
    }
    /** 记录最终Gate决定，必须先完成全部检查项且通过独立性校验。 */
    @Override @Transactional public GateRecord decideGate(String principalRef, long gateId, String decision, String comment) {
        requireAllowed(decision, GATE_DECISIONS, "Gate决定无效");
        GateRow gate = gates.find(gateId); if (gate == null || !"READY_FOR_REVIEW".equals(gate.getStatus())) throw new IllegalArgumentException("Gate未进入评审状态");
        if (memberships.countReviewer(gate.getProjectId(), principalRef) == 0 || gates.countConflict(gateId, principalRef) > 0) throw new AccessDeniedException("Reviewer独立性校验未通过");
        if (gates.pendingChecks(gateId) > 0) throw new IllegalArgumentException("仍有未完成的Gate检查项");
        if ("APPROVED".equals(decision) && gates.nonPassedChecks(gateId) > 0) throw new IllegalArgumentException("存在未通过的Gate检查项，不能批准Gate");
        gates.updateStatus(gateId, decision); gates.addDecision(gateId, principalRef, decision, comment); audit(gate.getProjectId(), "GATE", gateId, "GATE_DECIDED", "{\"status\":\"READY_FOR_REVIEW\"}", "{\"status\":" + quote(decision) + "}", principalRef, comment, null); return gate(gates.find(gateId));
    }
    private GateRecord gate(GateRow row) { GateDecisionRow decision = gates.latestDecision(row.getId()); return new GateRecord(row.getId(), row.getProjectId(), row.getPhase(), row.getStatus(), row.getSubmittedByRef(), row.getDecisionOwnerRef(), gates.taskIds(row.getId()), gates.deliverableIds(row.getId()), row.getSubmittedAt(), decision == null ? null : decision.getReviewerRef(), decision == null ? null : decision.getComment(), decision == null ? null : decision.getDecidedAt(), gates.checks(row.getId()).stream().map(this::check).toList()); }
    private GateCheckRecord check(GateCheckRow row) { return new GateCheckRecord(row.getId(), row.getCode(), row.getTitle(), row.getStatus(), row.getReviewerRef(), row.getComment(), row.getEvidenceRefs()); }

    /** 校验契约枚举，避免数据库枚举异常泄露为500并保持接口失败关闭。 */
    private static void requireAllowed(String value, Set<String> allowed, String message) {
        if (value == null || !allowed.contains(value)) throw new IllegalArgumentException(message);
    }

    /** 读取Runtime授权状态；未配置时明确返回失败关闭状态。 */
    @Override public RuntimeConfigRecord getRuntimeConfig(String principalRef, long projectId) {
        if (memberships.countActiveMember(projectId, principalRef) == 0) throw new AccessDeniedException("不是项目活动成员");
        RuntimeConfigRow row = runtimeConfigs.find(projectId);
        return row == null ? new RuntimeConfigRecord(null, projectId, "UNCONFIGURED", null, null, null) : new RuntimeConfigRecord(row.getId(), projectId, row.getStatus(), row.getModelRef(), row.getApprovedAt(), row.getExpiresAt());
    }

    /** 查询项目审计活动，默认按发生时间倒序分页。 */
    @Override public ActivityPage listActivity(String principalRef, long projectId, String objectType, int page, int pageSize) {
        if (memberships.countActiveMember(projectId, principalRef) == 0) throw new AccessDeniedException("不是项目活动成员");
        int offset = Math.multiplyExact(page - 1, pageSize);
        return new ActivityPage(auditEvents.list(projectId, objectType, pageSize, offset).stream().map(e -> new ActivityRecord(e.getId(), e.getObjectType(), e.getObjectId(), e.getAction(), e.getBeforeState(), e.getAfterState(), e.getActorRef(), e.getComment(), e.getEvidenceRefs(), e.getOccurredAt())).toList(), page, pageSize, auditEvents.count(projectId, objectType));
    }

    /** 仅在当前阶段Gate通过后推进项目生命周期。 */
    @Override @Transactional public ProjectRecord advanceProject(String principalRef, long projectId, String targetPhase) {
        ProjectRow row = projects.findActiveForPrincipal(principalRef, projectId); if (row == null) throw new AccessDeniedException("项目不存在或当前主体无权访问");
        if (memberships.countTaskManager(projectId, principalRef) == 0) throw new AccessDeniedException("需要项目编排角色");
        int currentIndex = LIFECYCLE_PHASES.indexOf(row.getCurrentPhase());
        if (currentIndex < 0 || currentIndex + 1 >= LIFECYCLE_PHASES.size() || !LIFECYCLE_PHASES.get(currentIndex + 1).equals(targetPhase)) {
            throw new IllegalArgumentException("目标阶段必须是当前阶段的下一阶段");
        }
        GateRow gate = gates.list(projectId).stream().filter(g -> g.getPhase().equals(row.getCurrentPhase())).findFirst().orElseThrow(() -> new IllegalArgumentException("当前阶段尚未建立Gate"));
        if (!"APPROVED".equals(gate.getStatus())) throw new IllegalArgumentException("当前阶段Gate尚未通过");
        String beforePhase = row.getCurrentPhase();
        projects.updatePhase(projectId, targetPhase);
        GateRow nextGate = new GateRow();
        nextGate.setProjectId(projectId);
        nextGate.setPhase(targetPhase);
        gates.ensure(nextGate);
        audit(projectId, "PROJECT", projectId, "PROJECT_PHASE_CHANGED", "{\"phase\":" + quote(beforePhase) + "}", "{\"phase\":" + quote(targetPhase) + "}", principalRef, null, null);
        return toRecord(projects.findActiveForPrincipal(principalRef, projectId));
    }

    private TaskRecord task(TaskRow row) { return new TaskRecord(row.getId(), row.getProjectId(), row.getTitle(), row.getDescription(), row.getPhase(), row.getAssigneeRef(), row.getAssigneeRole(), row.getStatus(), row.getCreatedAt(), row.getUpdatedAt()); }

    /** 将技术栈映射序列化为MySQL JSON字段内容。 */
    private String serialize(Map<String, String> value) {
        try { return value == null ? null : json.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("techStack无法序列化", e); }
    }

    /** 写入统一项目审计事件，状态摘要保持可追溯且不包含秘密。 */
    private void audit(long projectId, String objectType, long objectId, String action, String beforeState,
                       String afterState, String actorRef, String comment, String evidenceRefs) {
        AuditEventRow event = new AuditEventRow();
        event.setProjectId(projectId); event.setObjectType(objectType); event.setObjectId(objectId);
        event.setAction(action); event.setBeforeState(beforeState); event.setAfterState(afterState);
        event.setActorRef(actorRef); event.setComment(comment); event.setEvidenceRefs(evidenceRefs);
        auditEvents.insert(event);
    }

    /** 生成可安全嵌入JSON状态摘要的字符串。 */
    private String quote(String value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException("审计摘要无法序列化", e); }
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

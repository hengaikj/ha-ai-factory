package com.hengaikj.ai.factory.web;

import com.hengaikj.ai.factory.project.ProjectRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.LinkedHashSet;

/** 当前OIDC主体的项目列表和项目创建API。 */
@RestController
@RequestMapping
public class ProjectController {
    private final ProjectRepository repository;

    public ProjectController(ProjectRepository repository) { this.repository = repository; }

    /** 项目列表必须由服务端主体引用过滤，只显示有效成员关系。 */
    @GetMapping("/projects")
    public ProjectPage list(@AuthenticationPrincipal OidcUser user,
                            @RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "20") int pageSize,
                            @RequestParam(required = false) @Size(max = 128) String query) {
        validatePage(page, pageSize);
        var principal = principal(user);
        var result = repository.listProjects(principal.principalRef(), query, page, pageSize);
        return new ProjectPage(result.items().stream().map(ProjectItem::from).toList(), page, pageSize, result.total());
    }

    /** 创建项目时从OIDC会话绑定创建者并由持久化事务赋予Owner成员及角色。 */
    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectItem create(@AuthenticationPrincipal OidcUser user, @Valid @RequestBody ProjectCreate body) {
        var principal = principal(user);
        var project = repository.createProject(principal.principalRef(), body.name().trim(), body.description(), body.techStack());
        return ProjectItem.from(project);
    }

    /** 查询当前项目的活动成员，仅Owner或Project Admin可见。 */
    @GetMapping("/projects/{projectId}/members")
    public java.util.List<MemberItem> members(@AuthenticationPrincipal OidcUser user, @PathVariable long projectId) {
        var principal = principal(user);
        return repository.listMembers(principal.principalRef(), projectId).stream().map(MemberItem::from).toList();
    }

    /** 通过OIDC issuer+subject添加已认证主体，避免客户端选择内部UUID。 */
    @PostMapping("/projects/{projectId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public MemberItem addMember(@AuthenticationPrincipal OidcUser user, @PathVariable long projectId,
                                @Valid @RequestBody MemberCreate body) {
        var principal = principal(user);
        String target = repository.findActivePrincipal(body.issuer().toString(), body.subject());
        if (target == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "目标主体尚未完成企业身份认证");
        return MemberItem.from(repository.addMember(principal.principalRef(), projectId, target, body.roles()));
    }

    /** 替换成员角色集合并保留项目Owner约束。 */
    @PatchMapping(value = "/projects/{projectId}/members/{principalRef}", consumes = "application/merge-patch+json")
    public MemberItem replaceRoles(@AuthenticationPrincipal OidcUser user, @PathVariable long projectId,
                                   @PathVariable UUID principalRef, @Valid @RequestBody RolesUpdate body) {
        return MemberItem.from(repository.replaceMemberRoles(principal(user).principalRef(), projectId, principalRef.toString(), body.roles()));
    }

    /** 立即撤销成员资格，禁止移除项目最后一名Owner。 */
    @DeleteMapping("/projects/{projectId}/members/{principalRef}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal OidcUser user, @PathVariable long projectId, @PathVariable UUID principalRef) {
        repository.removeMember(principal(user).principalRef(), projectId, principalRef.toString());
    }

    /** 按项目读取任务，状态过滤和分页由服务端执行。 */
    @GetMapping("/projects/{projectId}/tasks")
    public TaskPage tasks(@AuthenticationPrincipal OidcUser user, @PathVariable long projectId,
                          @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize,
                          @RequestParam(required = false) String status) {
        validatePage(page, pageSize);
        var result = repository.listTasks(principal(user).principalRef(), projectId, status, page, pageSize);
        return new TaskPage(result.items().stream().map(TaskItem::from).toList(), page, pageSize, result.total());
    }

    /** 创建项目任务并由服务端绑定创建人。 */
    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskItem createTask(@AuthenticationPrincipal OidcUser user, @PathVariable long projectId, @Valid @RequestBody TaskCreate body) {
        var p = principal(user);
        return TaskItem.from(repository.createTask(p.principalRef(), projectId, body.title().trim(), body.description(), body.phase(), body.assigneeRef() == null ? null : body.assigneeRef().toString(), body.assigneeRole()));
    }

    /** 查询单个任务详情。 */
    @GetMapping("/tasks/{taskId}")
    public TaskItem task(@AuthenticationPrincipal OidcUser user, @PathVariable long taskId) { return TaskItem.from(repository.getTask(principal(user).principalRef(), taskId)); }

    /** 更新任务分配或状态，空字段按当前快照处理。 */
    @PatchMapping(value = "/tasks/{taskId}", consumes = "application/merge-patch+json")
    public TaskItem updateTask(@AuthenticationPrincipal OidcUser user, @PathVariable long taskId, @Valid @RequestBody TaskUpdate body) {
        var p = principal(user);
        return TaskItem.from(repository.updateTask(p.principalRef(), taskId, body.title(), body.description(), body.assigneeRef() == null ? null : body.assigneeRef().toString(), body.assigneeRole(), body.status()));
    }

    /** 查询项目登记的仓库引用型交付物。 */
    @GetMapping("/projects/{projectId}/deliverables")
    public DeliverablePage deliverables(@AuthenticationPrincipal OidcUser user, @PathVariable long projectId,
                                        @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        validatePage(page, pageSize);
        var result = repository.listDeliverables(principal(user).principalRef(), projectId, page, pageSize);
        return new DeliverablePage(result.items().stream().map(DeliverableItem::from).toList(), page, pageSize, result.total());
    }

    /** 登记交付物仓库引用，不接收公共上传地址或文件内容。 */
    @PostMapping("/projects/{projectId}/deliverables")
    @ResponseStatus(HttpStatus.CREATED)
    public DeliverableItem createDeliverable(@AuthenticationPrincipal OidcUser user, @PathVariable long projectId, @Valid @RequestBody DeliverableCreate body) {
        var p = principal(user);
        return DeliverableItem.from(repository.createDeliverable(p.principalRef(), projectId, body.taskId(), body.title().trim(), body.phase(), body.version(), body.sourceRef().trim()));
    }

    /** 记录独立交付物评审，评审人身份由服务端会话推导。 */
    @PostMapping("/deliverables/{deliverableId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public DeliverableReviewItem reviewDeliverable(@AuthenticationPrincipal OidcUser user, @PathVariable long deliverableId, @Valid @RequestBody DeliverableReviewCreate body) {
        var p = principal(user);
        return DeliverableReviewItem.from(repository.reviewDeliverable(p.principalRef(), deliverableId, body.outcome(), body.comment(), body.evidenceRefs() == null ? "[]" : body.evidenceRefs().toString()));
    }

    /** 查询项目待决事项。 */
    @GetMapping("/projects/{projectId}/issues")
    public java.util.List<IssueItem> issues(@AuthenticationPrincipal OidcUser user, @PathVariable long projectId) {
        return repository.listIssues(principal(user).principalRef(), projectId).stream().map(IssueItem::from).toList();
    }

    /** 创建Open Issue，状态仅允许开放或需要人工决策。 */
    @PostMapping("/projects/{projectId}/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public IssueItem createIssue(@AuthenticationPrincipal OidcUser user, @PathVariable long projectId, @Valid @RequestBody IssueCreate body) {
        var p = principal(user);
        return IssueItem.from(repository.createIssue(p.principalRef(), projectId, body.code(), body.title().trim(), body.description(), body.impact(), body.decisionRole(), body.status() == null ? "OPEN" : body.status()));
    }

    /** 记录人工决策，操作者身份由服务端会话推导。 */
    @PostMapping("/issues/{issueId}/decisions")
    public IssueItem decideIssue(@AuthenticationPrincipal OidcUser user, @PathVariable long issueId, @Valid @RequestBody IssueDecision body) {
        return IssueItem.from(repository.decideIssue(principal(user).principalRef(), issueId, body.decision(), body.outcome()));
    }

    /** 从Spring已验证的OIDC会话派生主体，不采信请求载荷中的操作者字段。 */
    private ProjectRepository.PrincipalRecord principal(OidcUser user) {
        if (user == null || user.getIssuer() == null || user.getSubject() == null || user.getSubject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少已验证的OIDC主体");
        }
        String name = user.getFullName() == null || user.getFullName().isBlank() ? user.getSubject() : user.getFullName();
        return repository.resolveOidcPrincipal(user.getIssuer().toString(), user.getSubject(), name);
    }

    /** 限定分页参数范围，避免非法偏移和超大查询。 */
    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page必须大于0，pageSize范围为1至100");
        }
    }

    public record ProjectCreate(@NotBlank @Size(max = 128) String name,
                                @Size(max = 4000) String description,
                                Map<@Size(max = 64) String, @Size(max = 256) String> techStack) {}

    public record ProjectPage(java.util.List<ProjectItem> items, int page, int pageSize, long total) {}

    public record MemberCreate(@NotBlank @Size(max = 512) String issuer,
                               @NotBlank @Size(max = 512) String subject,
                               @jakarta.validation.constraints.NotEmpty Set<String> roles) {}
    public record RolesUpdate(@jakarta.validation.constraints.NotEmpty Set<String> roles) {}
    public record MemberItem(UUID principalRef, String displayName, Set<String> roles, Instant joinedAt) {
        static MemberItem from(ProjectRepository.MemberRecord value) {
            return new MemberItem(UUID.fromString(value.principalRef()), value.displayName(), value.roles(), value.joinedAt());
        }
    }

    public record TaskCreate(@NotBlank @Size(max = 256) String title, @NotBlank @Size(max = 64) String phase,
                             @Size(max = 8000) String description, UUID assigneeRef, @Size(max = 64) String assigneeRole) {}
    public record TaskUpdate(@Size(max = 256) String title, @Size(max = 8000) String description, UUID assigneeRef,
                             @Size(max = 64) String assigneeRole, @Size(max = 40) String status) {}
    public record TaskPage(java.util.List<TaskItem> items, int page, int pageSize, long total) {}
    public record TaskItem(long id, long projectId, String title, String description, String phase, UUID assigneeRef,
                           String assigneeRole, String status, Instant createdAt, Instant updatedAt) {
        static TaskItem from(ProjectRepository.TaskRecord value) { return new TaskItem(value.id(), value.projectId(), value.title(), value.description(), value.phase(), value.assigneeRef() == null ? null : UUID.fromString(value.assigneeRef()), value.assigneeRole(), value.status(), value.createdAt(), value.updatedAt()); }
    }

    public record DeliverableCreate(Long taskId, @NotBlank @Size(max = 256) String title, @NotBlank @Size(max = 64) String phase,
                                    @NotBlank @Size(max = 64) String version, @NotBlank @Size(max = 2048) String sourceRef) {}
    public record DeliverableReviewCreate(@NotBlank @Size(max = 40) String outcome, @NotBlank @Size(max = 4000) String comment,
                                          java.util.List<@Size(max = 2048) String> evidenceRefs) {}
    public record DeliverablePage(java.util.List<DeliverableItem> items, int page, int pageSize, long total) {}
    public record DeliverableItem(long id, long projectId, Long taskId, String title, String phase, String version, String sourceRef, String reviewStatus, Instant createdAt) {
        static DeliverableItem from(ProjectRepository.DeliverableRecord v) { return new DeliverableItem(v.id(), v.projectId(), v.taskId(), v.title(), v.phase(), v.version(), v.sourceRef(), v.reviewStatus(), v.createdAt()); }
    }
    public record DeliverableReviewItem(long id, UUID reviewerRef, String outcome, String comment, String evidenceRefs, Instant createdAt) {
        static DeliverableReviewItem from(ProjectRepository.DeliverableReviewRecord v) { return new DeliverableReviewItem(v.id(), UUID.fromString(v.reviewerRef()), v.outcome(), v.comment(), v.evidenceRefs(), v.createdAt()); }
    }
    public record IssueCreate(@Size(max = 64) String code, @NotBlank @Size(max = 256) String title, @NotBlank @Size(max = 8000) String description,
                              @NotBlank @Size(max = 4000) String impact, @NotBlank @Size(max = 64) String decisionRole, @Size(max = 40) String status) {}
    public record IssueDecision(@NotBlank @Size(max = 4000) String decision, @NotBlank @Size(max = 40) String outcome) {}
    public record IssueItem(long id, long projectId, String code, String title, String description, String impact, String decisionRole, String status, String decision, Instant createdAt, Instant decidedAt) {
        static IssueItem from(ProjectRepository.IssueRecord v) { return new IssueItem(v.id(), v.projectId(), v.code(), v.title(), v.description(), v.impact(), v.decisionRole(), v.status(), v.decision(), v.createdAt(), v.decidedAt()); }
    }

    public record ProjectItem(long id, String name, String description, Map<String, String> techStack,
                              UUID ownerRef, String currentPhase, String gateStatus, int openIssueCount,
                              Instant createdAt, Instant updatedAt) {
        static ProjectItem from(ProjectRepository.ProjectRecord value) {
            return new ProjectItem(value.id(), value.name(), value.description(),
                    value.techStack() == null ? Map.of() : value.techStack(), UUID.fromString(value.ownerRef()),
                    value.currentPhase(), value.gateStatus(), value.openIssueCount(), value.createdAt(), value.updatedAt());
        }
    }
}

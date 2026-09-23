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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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

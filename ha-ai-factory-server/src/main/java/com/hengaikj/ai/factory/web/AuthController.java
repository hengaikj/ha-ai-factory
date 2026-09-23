package com.hengaikj.ai.factory.web;

import com.hengaikj.ai.factory.project.ProjectRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

import java.util.UUID;

/** 提供企业OIDC登录入口及当前服务器会话信息。 */
@RestController
@RequestMapping
public class AuthController {
    private final ObjectProvider<ClientRegistrationRepository> registrations;
    private final ProjectRepository principals;

    public AuthController(ObjectProvider<ClientRegistrationRepository> registrations, ProjectRepository principals) {
        this.registrations = registrations;
        this.principals = principals;
    }

    /** OIDC未配置时明确拒绝开始登录，不降级到匿名身份或固定开发用户。 */
    @GetMapping("/auth/login")
    public RedirectView login() {
        if (registrations.getIfAvailable() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "企业OIDC尚未配置");
        }
        return new RedirectView("/oauth2/authorization/enterprise", true, false);
    }

    /** 仅从已验证的OIDC主体读取身份，客户端不能指定issuer、subject或principalRef。 */
    @GetMapping("/auth/session")
    public CurrentSession currentSession(@AuthenticationPrincipal OidcUser user,
                                         @RequestAttribute("_csrf") CsrfToken csrfToken) {
        String issuer = user.getIssuer() == null ? null : user.getIssuer().toString();
        String subject = user.getSubject();
        if (issuer == null || subject == null || subject.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OIDC身份声明不完整");
        }
        String name = user.getFullName() == null || user.getFullName().isBlank() ? subject : user.getFullName();
        var principal = principals.resolveOidcPrincipal(issuer, subject, name);
        return new CurrentSession(UUID.fromString(principal.principalRef()), principal.displayName(), csrfToken.getToken());
    }

    public record CurrentSession(UUID principalRef, String displayName, String csrfToken) {}
}

package com.hengaikj.ai.factory.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;

/** 为Servlet服务器会话增加八小时绝对时限，避免持续活跃会话无限延长。 */
public class AbsoluteSessionLifetimeFilter extends OncePerRequestFilter {
    static final String CREATED_AT = AbsoluteSessionLifetimeFilter.class.getName() + ".createdAt";
    private static final Duration ABSOLUTE_LIMIT = Duration.ofHours(8);
    private final Clock clock;

    public AbsoluteSessionLifetimeFilter(Clock clock) { this.clock = clock; }

    /** 首个已认证请求记录会话起点，到期后立即销毁服务端会话并拒绝请求。 */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            var session = request.getSession(true);
            Long createdAt = (Long) session.getAttribute(CREATED_AT);
            long now = clock.millis();
            if (createdAt == null) session.setAttribute(CREATED_AT, now);
            else if (now - createdAt >= ABSOLUTE_LIMIT.toMillis()) {
                session.invalidate();
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}

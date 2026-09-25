package com.hengaikj.ai.factory.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

/** 对所有状态变更请求执行同源检查，作为CSRF Token校验之外的第二道跨站防线。 */
final class RequestOriginFilter extends OncePerRequestFilter {
    private static final Set<String> STATE_CHANGING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final String configuredFrontend;

    RequestOriginFilter(String configuredFrontend) {
        this.configuredFrontend = configuredFrontend;
    }

    /** 只接受部署配置前端来源；缺少Origin/Referer、来源异常或跨源时均失败关闭。 */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!STATE_CHANGING_METHODS.contains(request.getMethod().toUpperCase(Locale.ROOT))
                || isTrustedOrigin(request)) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Forbidden\",\"status\":403,\"code\":\"FORBIDDEN\"}");
    }

    /** 提取浏览器Origin或Referer，并与可信前端配置逐项比较。 */
    private boolean isTrustedOrigin(HttpServletRequest request) {
        String source = request.getHeader("Origin");
        if (source == null || source.isBlank()) source = request.getHeader("Referer");
        if (source == null || source.isBlank()) return false;
        try {
            URI sourceUri = URI.create(source);
            URI expectedUri = URI.create(configuredFrontend);
            if (!expectedUri.isAbsolute()) {
                String scheme = request.getScheme();
                String origin = scheme + "://" + request.getServerName() + ":" + request.getServerPort();
                expectedUri = URI.create(origin);
            }
            int sourcePort = effectivePort(sourceUri);
            int expectedPort = effectivePort(expectedUri);
            return sourceUri.getUserInfo() == null && sourceUri.getHost() != null
                    && sourceUri.getScheme() != null
                    && sourceUri.getScheme().equalsIgnoreCase(expectedUri.getScheme())
                    && sourceUri.getHost().equalsIgnoreCase(expectedUri.getHost())
                    && sourcePort == expectedPort;
        } catch (IllegalArgumentException error) {
            return false;
        }
    }

    /** 将未显式指定的HTTP(S)端口展开为协议默认端口。 */
    private int effectivePort(URI uri) {
        if (uri.getPort() >= 0) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }
}

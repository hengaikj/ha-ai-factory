package com.hengaikj.ai.factory.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import java.time.Clock;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import java.net.URI;

/** 企业OIDC登录及受保护API边界；未配置OIDC时不提供任何开发用户认证替代。 */
@Configuration
public class SecurityConfiguration {
    @Bean
    public SecurityFilterChain apiSecurity(HttpSecurity http,
            ObjectProvider<ClientRegistrationRepository> registrationRepositories, Clock clock,
            @Value("${factory.web-base-url:/}") String webBaseUrl) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/callback", "/oauth2/authorization/**", "/login/oauth2/code/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/problem+json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Forbidden\",\"status\":403,\"code\":\"FORBIDDEN\"}");
                        }))
                .csrf(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.migrateSession()));
        http.addFilterBefore(new RequestOriginFilter(webBaseUrl), CsrfFilter.class);
        if (registrationRepositories.getIfAvailable() != null) {
            String frontendLocation = validateFrontendLocation(webBaseUrl);
            OAuth2AuthorizationRequestResolver pkceResolver = pkceResolver(registrationRepositories.getIfAvailable());
            http.oauth2Login(oauth -> oauth
                    .authorizationEndpoint(endpoint -> endpoint.baseUri("/oauth2/authorization").authorizationRequestResolver(pkceResolver))
                    .redirectionEndpoint(endpoint -> endpoint.baseUri("/auth/callback"))
                    .successHandler((request, response, authentication) -> {
                        request.getSession(true).setAttribute(AbsoluteSessionLifetimeFilter.CREATED_AT, clock.millis());
                        response.setStatus(HttpStatus.SEE_OTHER.value());
                        response.setHeader("Location", frontendLocation);
                    })
                    .failureHandler((request, response, exception) -> {
                        response.setStatus(HttpStatus.SEE_OTHER.value());
                        response.setHeader("Location", appendLoginError(frontendLocation));
                    }));
        }
        http.logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/auth/logout", "POST"))
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("ha_session")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpStatus.NO_CONTENT.value())));
        http.addFilterAfter(new AbsoluteSessionLifetimeFilter(clock), SecurityContextHolderFilter.class);
        return http.build();
    }

    /** 企业OIDC使用Authorization Code + PKCE，Verifier由Spring保存在服务器端授权请求会话中。 */
    private OAuth2AuthorizationRequestResolver pkceResolver(ClientRegistrationRepository registrations) {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(registrations, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        return resolver;
    }

    /** 只允许部署配置决定回跳地址，拒绝任何外部请求参数形式的跳转目标。 */
    static String validateFrontendLocation(String value) {
        URI uri = URI.create(value);
        if (!uri.isAbsolute()) {
            if (!value.startsWith("/") || value.startsWith("//")) {
                throw new IllegalStateException("FACTORY_WEB_BASE_URL必须是同源根路径或可信HTTP(S) URL");
            }
            return value;
        }
        boolean secure = "https".equalsIgnoreCase(uri.getScheme());
        boolean localHttp = "http".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                && ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
        if ((!secure && !localHttp) || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw new IllegalStateException("FACTORY_WEB_BASE_URL必须使用可信HTTPS地址（本机联调可用localhost HTTP）");
        }
        return uri.toString();
    }

    /** 登录失败只返回固定错误码，避免将IdP响应、异常信息或请求参数泄露到浏览器地址。 */
    static String appendLoginError(String frontendLocation) {
        if (frontendLocation.endsWith("/")) return frontendLocation + "?authError=oidc_login_failed";
        return frontendLocation + (frontendLocation.contains("?") ? "&" : "?") + "authError=oidc_login_failed";
    }
}

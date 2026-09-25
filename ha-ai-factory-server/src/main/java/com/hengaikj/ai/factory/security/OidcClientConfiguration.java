package com.hengaikj.ai.factory.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/** 只有部署提供完整OIDC环境配置时才注册企业身份提供方。 */
@Configuration
@ConditionalOnProperty(prefix = "factory.oidc", name = "issuer-uri")
public class OidcClientConfiguration {
    @Bean
    public ClientRegistrationRepository enterpriseOidcRegistration(Environment environment) {
        String issuer = environment.getProperty("factory.oidc.issuer-uri");
        String clientId = environment.getProperty("factory.oidc.client-id");
        String clientSecret = environment.getProperty("factory.oidc.client-secret");
        if (blank(issuer) || blank(clientId) || blank(clientSecret)) {
            throw new IllegalStateException("企业OIDC配置不完整：必须同时设置issuer、client id和client secret");
        }
        ClientRegistration registration = ClientRegistrations.fromIssuerLocation(issuer)
                .registrationId("enterprise")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/auth/callback")
                .scope("openid", "profile", "email")
                .clientName("企业OIDC")
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}

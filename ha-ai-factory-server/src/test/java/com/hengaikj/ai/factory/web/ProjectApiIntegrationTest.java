package com.hengaikj.ai.factory.web;

import com.hengaikj.ai.factory.HaAiFactoryApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 验证受保护项目API使用真实OIDC主体边界，并在创建时授予Owner。 */
@SpringBootTest(classes = HaAiFactoryApplication.class)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Import(ProjectApiIntegrationTest.OidcTestConfiguration.class)
class ProjectApiIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36").withDatabaseName("ha_ai_factory")
            .withUsername("factory_test").withPassword("factory_test_password");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", MYSQL::getJdbcUrl);
        properties.add("spring.datasource.username", MYSQL::getUsername);
        properties.add("spring.datasource.password", MYSQL::getPassword);
        properties.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void unauthenticatedProjectRequestsAreRejectedAndLoginStartsOidc() throws Exception {
        mvc.perform(get("/projects"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/auth/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/oauth2/authorization/enterprise"));
        var authorizationResponse = mvc.perform(get("/oauth2/authorization/enterprise"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse();
        String location = authorizationResponse.getHeader("Location");
        String redirectUri = Arrays.stream(URI.create(location).getRawQuery().split("&"))
                .filter(parameter -> parameter.startsWith("redirect_uri="))
                .map(parameter -> URLDecoder.decode(parameter.substring("redirect_uri=".length()), StandardCharsets.UTF_8))
                .findFirst().orElseThrow();
        assertEquals("http://localhost/auth/callback", redirectUri);
        var query = URI.create(location).getRawQuery();
        org.junit.jupiter.api.Assertions.assertTrue(query.contains("code_challenge_method=S256"));
        org.junit.jupiter.api.Assertions.assertTrue(query.contains("code_challenge="));
        mvc.perform(get("/auth/callback"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/?authError=oidc_login_failed"));
    }

    @Test
    void callerCanCreateProjectAndOnlySeesProjectsWithActiveMembership() throws Exception {
        var alice = SecurityMockMvcRequestPostProcessors.oidcLogin()
                .idToken(token -> token.issuer("https://idp.example").subject("alice"))
                .userInfoToken(token -> token.claim("name", "Alice"));
        String created = mvc.perform(post("/projects").with(alice).header("Origin", "http://localhost").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice project\",\"description\":\"desc\",\"techStack\":{\"backend\":\"Java\"}}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice project"))
                .andExpect(jsonPath("$.ownerRef").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        var createdProject = new com.fasterxml.jackson.databind.ObjectMapper().readTree(created);
        long projectId = createdProject.path("id").asLong();
        String principalRef = createdProject.path("ownerRef").asText();
        String ownerRef = mvc.perform(get("/projects").with(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].name").value("Alice project"))
                .andExpect(jsonPath("$.items[0].gateStatus").value("PENDING"))
                .andExpect(jsonPath("$.items[0].openIssueCount").value(0))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.total").value(1))
                .andReturn().getResponse().getContentAsString();
        jdbc.update("INSERT INTO project_gates(project_id,phase,status) VALUES(?,'DISCOVERY','APPROVED')", projectId);
        jdbc.update("INSERT INTO open_issues(project_id,title,description,impact,decision_role,status,created_by_ref) " +
                "VALUES(?,'Issue','描述','影响','Owner','OPEN',?)", projectId, principalRef);
        mvc.perform(get("/projects").with(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].gateStatus").value("APPROVED"))
                .andExpect(jsonPath("$.items[0].openIssueCount").value(1));
        var bob = SecurityMockMvcRequestPostProcessors.oidcLogin()
                .idToken(token -> token.issuer("https://idp.example").subject("bob"));
        mvc.perform(get("/projects").with(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.total").value(0));
        jdbc.update("UPDATE principals SET is_active=FALSE WHERE principal_ref=?", principalRef);
        mvc.perform(get("/projects").with(alice))
                .andExpect(status().isForbidden());
    }

    @Test
    void stateChangingRequestsRequireSameOriginAndCsrf() throws Exception {
        var alice = SecurityMockMvcRequestPostProcessors.oidcLogin()
                .idToken(token -> token.issuer("https://idp.example").subject("origin-user"));
        String payload = "{\"name\":\"Origin check\"}";
        mvc.perform(post("/projects").with(alice).header("Origin", "https://evil.example")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(post("/projects").with(alice)
                        .with(SecurityMockMvcRequestPostProcessors.csrf()).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
        mvc.perform(post("/projects").with(alice).header("Origin", "http://localhost")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @TestConfiguration
    static class OidcTestConfiguration {
        @Bean
        ClientRegistrationRepository testOidcRegistration() {
            var registration = ClientRegistration.withRegistrationId("enterprise")
                    .clientId("test-client")
                    .clientSecret("not-a-real-secret")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/auth/callback")
                    .scope("openid", "profile", "email")
                    .authorizationUri("https://idp.example/authorize")
                    .tokenUri("https://idp.example/token")
                    .userInfoUri("https://idp.example/userinfo")
                    .userNameAttributeName("sub")
                    .jwkSetUri("https://idp.example/jwks")
                    .issuerUri("https://idp.example")
                    .clientName("Test IdP")
                    .build();
            return new InMemoryClientRegistrationRepository(registration);
        }
    }
}

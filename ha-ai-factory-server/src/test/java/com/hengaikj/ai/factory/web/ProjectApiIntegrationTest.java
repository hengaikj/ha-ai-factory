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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
        mvc.perform(get("/projects/{projectId}", projectId).with(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId))
                .andExpect(jsonPath("$.name").value("Alice project"));
        mvc.perform(get("/projects/{projectId}/activity", projectId).with(alice))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].action").value("PROJECT_CREATED"));
        mvc.perform(post("/projects/{projectId}/tasks", projectId).with(alice).header("Origin", "http://localhost").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"建立骨架\",\"phase\":\"DISCOVERY\",\"description\":\"任务说明\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("建立骨架"))
                .andExpect(jsonPath("$.status").value("NOT_STARTED"));
        mvc.perform(get("/projects/{projectId}/tasks", projectId).with(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].phase").value("DISCOVERY"));
        mvc.perform(patch("/tasks/1").with(alice).header("Origin", "http://localhost").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType("application/merge-patch+json").content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(patch("/tasks/1").with(alice).header("Origin", "http://localhost").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType("application/merge-patch+json").content("{\"status\":\"DONE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(post("/projects/{projectId}/deliverables", projectId).with(alice).header("Origin", "http://localhost").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"需求基线\",\"phase\":\"DISCOVERY\",\"version\":\"v1\",\"sourceRef\":\"docs/requirement/requirement-baseline.md\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewStatus").value("DRAFT"));
        mvc.perform(get("/projects/{projectId}/deliverables", projectId).with(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].sourceRef").value("docs/requirement/requirement-baseline.md"));
        mvc.perform(post("/projects/{projectId}/issues", projectId).with(alice).header("Origin", "http://localhost").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"OI-TEST\",\"title\":\"需要决策\",\"description\":\"说明\",\"impact\":\"影响\",\"decisionRole\":\"Owner\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));
        mvc.perform(get("/projects/{projectId}/issues", projectId).with(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code").value("OI-TEST"));
        jdbc.update("INSERT INTO project_resources(project_id,kind,title,phase,version,source_ref) VALUES(?,'RULE','需求规则','DISCOVERY','v1','process/rules.md')", projectId);
        mvc.perform(get("/projects/{projectId}/resources?phase=DISCOVERY&kind=RULE", projectId).with(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sourceRef").value("process/rules.md"));
        jdbc.update("INSERT INTO project_gates(project_id,phase) VALUES(?,'DISCOVERY')", projectId);
        var gateId = jdbc.queryForObject("SELECT id FROM project_gates WHERE project_id=? AND phase='DISCOVERY'", Long.class, projectId);
        jdbc.update("INSERT INTO gate_checks(gate_id,code,title) VALUES(?,'REQ','需求检查')", gateId);
        var checkId = jdbc.queryForObject("SELECT id FROM gate_checks WHERE gate_id=? AND code='REQ'", Long.class, gateId);
        mvc.perform(get("/projects/{projectId}/gates", projectId).with(alice))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].status").value("PENDING"));
        mvc.perform(get("/projects/{projectId}/agent-runtime/config-status", projectId).with(alice))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UNCONFIGURED"));
        mvc.perform(post("/gates/{gateId}/submission", gateId).with(alice).header("Origin", "http://localhost").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decisionOwnerRef\":\"" + principalRef + "\",\"taskIds\":[1],\"deliverableIds\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("READY_FOR_REVIEW"));
        var bob = SecurityMockMvcRequestPostProcessors.oidcLogin()
                .idToken(token -> token.issuer("https://idp.example").subject("bob"));
        mvc.perform(get("/auth/session").with(bob)).andExpect(status().isOk());
        mvc.perform(post("/projects/{projectId}/members", projectId).with(alice).header("Origin", "http://localhost").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"issuer\":\"https://idp.example\",\"subject\":\"bob\",\"roles\":[\"REVIEWER\"]}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/gates/{gateId}/checks/{checkId}/decision", gateId, checkId).with(bob).header("Origin", "http://localhost").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"FAILED\",\"comment\":\"需要补充证据\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FAILED"));
        mvc.perform(post("/gates/{gateId}/decision", gateId).with(bob).header("Origin", "http://localhost").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isBadRequest());
                mvc.perform(post("/gates/{gateId}/decision", gateId).with(bob).header("Origin", "http://localhost").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"RETURNED\",\"comment\":\"补充证据后重提\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.reviewerRef").isNotEmpty())
                .andExpect(jsonPath("$.decisionComment").value("补充证据后重提"))
                .andExpect(jsonPath("$.decidedAt").isNotEmpty());
        String ownerRef = mvc.perform(get("/projects").with(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].name").value("Alice project"))
                .andExpect(jsonPath("$.items[0].gateStatus").value("RETURNED"))
                .andExpect(jsonPath("$.items[0].openIssueCount").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.total").value(1))
                .andReturn().getResponse().getContentAsString();
        jdbc.update("UPDATE project_gates SET status='APPROVED' WHERE id=?", gateId);
        jdbc.update("INSERT INTO open_issues(project_id,title,description,impact,decision_role,status,created_by_ref) " +
                "VALUES(?,'Issue','描述','影响','Owner','OPEN',?)", projectId, principalRef);
        mvc.perform(get("/projects").with(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].gateStatus").value("APPROVED"))
                .andExpect(jsonPath("$.items[0].openIssueCount").value(2));
        mvc.perform(post("/projects/{projectId}/stage-transitions", projectId).with(alice).header("Origin", "http://localhost").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetPhase\":\"PRODUCT\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(post("/projects/{projectId}/stage-transitions", projectId).with(alice).header("Origin", "http://localhost").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"targetPhase\":\"REQUIREMENT\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentPhase").value("REQUIREMENT"));
        mvc.perform(get("/projects").with(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.total").value(1));
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

package com.hengaikj.ai.factory.web;

import com.hengaikj.ai.factory.project.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

/** 未配置企业OIDC时，登录入口必须明确失败，不能进入虚构的注册流程。 */
class NoOidcLoginTest {
    @Test
    @SuppressWarnings("unchecked")
    void missingOidcConfigurationFailsClosed() {
        ObjectProvider<ClientRegistrationRepository> registrations = mock(ObjectProvider.class);
        when(registrations.getIfAvailable()).thenReturn(null);
        var controller = new AuthController(registrations, mock(ProjectRepository.class));
        assertThatThrownBy(controller::login)
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(SERVICE_UNAVAILABLE);
    }
}

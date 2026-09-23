package com.hengaikj.ai.factory;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import com.hengaikj.ai.factory.audit.AuditLogService;
import com.hengaikj.ai.factory.authorization.PermissionEvaluator;
import com.hengaikj.ai.factory.identity.IdentitySessionService;

import static org.assertj.core.api.Assertions.assertThat;

/** 应用启动集成测试，确保后端基础上下文能够正常加载。 */
class HaAiFactoryApplicationTest {

    @Test
    void applicationContextLoads() {
        new ApplicationContextRunner()
                .withUserConfiguration(HaAiFactoryApplication.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(IdentitySessionService.class);
                    assertThat(context).hasSingleBean(PermissionEvaluator.class);
                    assertThat(context).hasSingleBean(AuditLogService.class);
                });
    }
}

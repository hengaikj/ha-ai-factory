package com.hengaikj.ai.factory;

import com.hengaikj.ai.factory.audit.AuditLogService;
import com.hengaikj.ai.factory.audit.InMemoryAuditLog;
import com.hengaikj.ai.factory.authorization.InMemoryRoleAssignments;
import com.hengaikj.ai.factory.authorization.PermissionEvaluator;
import com.hengaikj.ai.factory.identity.IdentitySessionService;
import com.hengaikj.ai.factory.identity.InMemoryIdentityStore;
import com.hengaikj.ai.factory.identity.InMemorySessionStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** 注册 M01 内部骨架服务及其进程内存储适配器。 */
@Configuration
public class FoundationConfiguration {

    /** 提供可注入的系统时钟。 */
    @Bean
    public Clock foundationClock() {
        return Clock.systemUTC();
    }

    /** 提供仅驻留内存的身份存储。 */
    @Bean
    public InMemoryIdentityStore inMemoryIdentityStore() {
        return new InMemoryIdentityStore();
    }

    /** 提供仅驻留内存的会话存储。 */
    @Bean
    public InMemorySessionStore inMemorySessionStore() {
        return new InMemorySessionStore();
    }

    /** 注册身份与会话应用服务。 */
    @Bean
    public IdentitySessionService identitySessionService(InMemoryIdentityStore identities,
                                                         InMemorySessionStore sessions,
                                                         Clock foundationClock) {
        return new IdentitySessionService(identities, sessions, foundationClock);
    }

    /** 提供仅驻留内存的角色分配。 */
    @Bean
    public InMemoryRoleAssignments inMemoryRoleAssignments() {
        return new InMemoryRoleAssignments();
    }

    /** 注册默认拒绝的权限评估服务。 */
    @Bean
    public PermissionEvaluator permissionEvaluator(InMemoryRoleAssignments assignments) {
        return new PermissionEvaluator(assignments);
    }

    /** 提供仅驻留内存的审计日志。 */
    @Bean
    public InMemoryAuditLog inMemoryAuditLog() {
        return new InMemoryAuditLog();
    }

    /** 注册审计事件记录服务。 */
    @Bean
    public AuditLogService auditLogService(InMemoryAuditLog log, Clock foundationClock) {
        return new AuditLogService(log, foundationClock);
    }
}

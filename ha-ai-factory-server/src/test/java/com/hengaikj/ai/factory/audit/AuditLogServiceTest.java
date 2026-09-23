package com.hengaikj.ai.factory.audit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditLogServiceTest {
    private final InMemoryAuditLog store = new InMemoryAuditLog();
    private final AuditLogService service = new AuditLogService(store,
            Clock.fixed(Instant.parse("2026-09-23T10:00:00Z"), ZoneOffset.UTC));

    @Test
    void appendsEventsInOrder() {
        AuditEvent first = service.record("user-1", "CREATE", "project", "p-1", Map.of());
        AuditEvent second = service.record("user-1", "UPDATE", "project", "p-1", Map.of());

        assertThat(service.list()).containsExactly(first, second);
    }

    @Test
    void returnedSnapshotCannotMutateAuditLog() {
        service.record("user-1", "CREATE", "project", "p-1", Map.of());
        List<AuditEvent> snapshot = service.list();

        assertThatThrownBy(() -> snapshot.clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(service.list()).hasSize(1);
    }

    @Test
    void omitsCredentialLikeDetailKeysWithoutMutatingCallerMap() {
        Map<String, String> details = new HashMap<>();
        details.put("result", "ok");
        details.put("accessToken", "do-not-store");
        details.put("SECRET", "do-not-store");
        details.put("password", "do-not-store");
        details.put("apiKey", "do-not-store");
        details.put("x-api-key", "do-not-store");
        details.put("api_key", "do-not-store");
        details.put("authorizationHeader", "do-not-store");

        AuditEvent event = service.record("user-1", "LOGIN_ATTEMPT", "session", "s-1", details);

        assertThat(event.details()).containsOnlyKeys("result");
        assertThat(details).containsKeys("accessToken", "SECRET", "password", "apiKey", "x-api-key",
                "api_key", "authorizationHeader");
    }

    @Test
    void auditStoreStartsEmpty() {
        assertThat(service.list()).isEmpty();
    }
}

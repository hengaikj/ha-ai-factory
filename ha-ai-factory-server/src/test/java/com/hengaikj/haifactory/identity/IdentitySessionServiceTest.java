package com.hengaikj.haifactory.identity;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentitySessionServiceTest {
    private final Instant now = Instant.parse("2026-09-23T10:00:00Z");
    private final InMemoryIdentityStore identities = new InMemoryIdentityStore();
    private final InMemorySessionStore sessions = new InMemorySessionStore();
    private final IdentitySessionService service = new IdentitySessionService(
            identities, sessions, Clock.fixed(now, ZoneOffset.UTC));

    @Test
    void createsSessionForActiveIdentity() {
        identities.put(new UserIdentity("user-1", "测试用户", true));

        SessionRecord session = service.createSession("user-1", Duration.ofMinutes(5));

        assertThat(session.subjectId()).isEqualTo("user-1");
        assertThat(session.expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(5)));
        assertThat(service.resolveActiveSession(session.sessionId())).contains(identities.find("user-1").orElseThrow());
    }

    @Test
    void rejectsUnknownIdentity() {
        assertThatThrownBy(() -> service.createSession("missing", Duration.ofMinutes(5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingIdentityIdentifier() {
        assertThatThrownBy(() -> service.createSession(null, Duration.ofMinutes(5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInactiveIdentity() {
        identities.put(new UserIdentity("user-1", "停用用户", false));

        assertThatThrownBy(() -> service.createSession("user-1", Duration.ofMinutes(5)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsNonPositiveTtl() {
        identities.put(new UserIdentity("user-1", "测试用户", true));

        assertThatThrownBy(() -> service.createSession("user-1", Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expiredSessionIsNotResolvedAtExpiryBoundary() {
        identities.put(new UserIdentity("user-1", "测试用户", true));
        SessionRecord session = service.createSession("user-1", Duration.ofMinutes(1));
        IdentitySessionService atExpiry = new IdentitySessionService(identities, sessions,
                Clock.fixed(now.plus(Duration.ofMinutes(1)), ZoneOffset.UTC));

        assertThat(atExpiry.resolveActiveSession(session.sessionId())).isEmpty();
    }

    @Test
    void storesStartEmpty() {
        assertThat(identities.find("user-1")).isEmpty();
        assertThat(sessions.find("session-1")).isEmpty();
    }
}

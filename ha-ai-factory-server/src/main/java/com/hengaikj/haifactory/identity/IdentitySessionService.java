package com.hengaikj.haifactory.identity;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** 执行内部身份和内存会话校验；该服务不提供认证入口或凭据验证。 */
public class IdentitySessionService {
    private final InMemoryIdentityStore identities;
    private final InMemorySessionStore sessions;
    private final Clock clock;

    public IdentitySessionService(InMemoryIdentityStore identities, InMemorySessionStore sessions, Clock clock) {
        this.identities = identities;
        this.sessions = sessions;
        this.clock = clock;
    }

    /** 为已知且启用的内部身份创建短期内存会话。 */
    public SessionRecord createSession(String subjectId, Duration ttl) {
        UserIdentity identity = identities.find(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("身份不存在"));
        if (!identity.active()) {
            throw new IllegalStateException("身份已停用");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("会话有效期必须大于零");
        }
        Instant createdAt = clock.instant();
        SessionRecord session = new SessionRecord(UUID.randomUUID().toString(), subjectId, createdAt,
                createdAt.plus(ttl));
        sessions.put(session);
        return session;
    }

    /** 仅返回尚未过期且对应身份仍启用的会话身份。 */
    public Optional<UserIdentity> resolveActiveSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        return sessions.find(sessionId)
                .filter(session -> clock.instant().isBefore(session.expiresAt()))
                .flatMap(session -> identities.find(session.subjectId()))
                .filter(UserIdentity::active);
    }
}

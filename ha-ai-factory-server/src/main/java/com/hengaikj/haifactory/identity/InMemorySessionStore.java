package com.hengaikj.haifactory.identity;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 提供非持久化的会话存取，实例重建后不保留旧会话。 */
public class InMemorySessionStore {
    private final ConcurrentMap<String, SessionRecord> sessions = new ConcurrentHashMap<>();

    /** 保存会话记录。 */
    public void put(SessionRecord session) {
        sessions.put(session.sessionId(), session);
    }

    /** 按会话编号查找会话。 */
    public Optional<SessionRecord> find(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }
}

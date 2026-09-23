package com.hengaikj.haifactory.audit;

import java.time.Instant;
import java.util.Map;

/** 表示不可变的审计事件；事件只存在于当前进程内存。 */
public record AuditEvent(String eventId, String actorId, String action, String objectType, String objectId,
                         Map<String, String> details, Instant occurredAt) {
    public AuditEvent {
        details = Map.copyOf(details);
    }
}

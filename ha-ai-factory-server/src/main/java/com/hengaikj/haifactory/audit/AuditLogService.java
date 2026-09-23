package com.hengaikj.haifactory.audit;

import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** 记录内部审计事件，并移除名称表明含凭据的详情字段。 */
public class AuditLogService {
    private final InMemoryAuditLog log;
    private final Clock clock;

    public AuditLogService(InMemoryAuditLog log, Clock clock) {
        this.log = log;
        this.clock = clock;
    }

    /** 追加经过敏感字段名筛除的审计事件。 */
    public AuditEvent record(String actorId, String action, String objectType, String objectId,
                             Map<String, String> details) {
        Map<String, String> safeDetails = details == null ? Map.of() : details.entrySet().stream()
                .filter(entry -> !isCredentialKey(entry.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        AuditEvent event = new AuditEvent(UUID.randomUUID().toString(), actorId, action, objectType,
                objectId, safeDetails, clock.instant());
        log.append(event);
        return event;
    }

    /** 返回不可变审计日志快照。 */
    public java.util.List<AuditEvent> list() {
        return log.snapshot();
    }

    private boolean isCredentialKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        return normalized.contains("token") || normalized.contains("secret") || normalized.contains("password")
                || normalized.contains("credential") || normalized.contains("apikey")
                || normalized.contains("authorization") || normalized.contains("authheader");
    }
}

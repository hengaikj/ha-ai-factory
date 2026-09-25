package com.hengaikj.ai.factory.audit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** 提供仅追加的进程内审计事件存储。 */
public class InMemoryAuditLog {
    private final CopyOnWriteArrayList<AuditEvent> events = new CopyOnWriteArrayList<>();

    /** 追加审计事件。 */
    public void append(AuditEvent event) {
        events.add(event);
    }

    /** 返回不可变的当前事件快照。 */
    public List<AuditEvent> snapshot() {
        return List.copyOf(events);
    }
}

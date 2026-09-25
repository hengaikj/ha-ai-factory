package com.hengaikj.ai.factory.identity;

import java.time.Instant;

/** 表示仅驻留内存的会话记录；服务重启后记录即丢失。 */
public record SessionRecord(String sessionId, String subjectId, Instant createdAt, Instant expiresAt) {
}

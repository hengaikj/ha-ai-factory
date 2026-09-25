package com.hengaikj.ai.factory.identity;

/** 表示进程内使用的用户身份，不代表外部身份提供方已完成认证。 */
public record UserIdentity(String subjectId, String displayName, boolean active) {
}

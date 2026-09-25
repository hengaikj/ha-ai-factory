package com.hengaikj.ai.factory.identity;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 提供非持久化的用户身份存取，仅供内部骨架与测试使用。 */
public class InMemoryIdentityStore {
    private final ConcurrentMap<String, UserIdentity> identities = new ConcurrentHashMap<>();

    /** 按身份编号保存身份。 */
    public void put(UserIdentity identity) {
        if (identity == null || identity.subjectId() == null || identity.subjectId().isBlank()) {
            throw new IllegalArgumentException("身份编号不能为空");
        }
        identities.put(identity.subjectId(), identity);
    }

    /** 查找身份。 */
    public Optional<UserIdentity> find(String subjectId) {
        if (subjectId == null || subjectId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(identities.get(subjectId));
    }
}

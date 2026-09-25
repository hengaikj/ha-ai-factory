package com.hengaikj.ai.factory.authorization;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** 提供进程内角色定义与主体角色分配，不构成持久化或最终 RBAC 矩阵。 */
public class InMemoryRoleAssignments {
    private final ConcurrentMap<String, Role> roles = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> subjectRoles = new ConcurrentHashMap<>();

    /** 注册一个内存角色定义。 */
    public void putRole(Role role) {
        roles.put(role.roleId(), role);
    }

    /** 仅为主体分配已注册角色，阻止悬空角色在后续注册后意外授予权限。 */
    public void assignRole(String subjectId, String roleId) {
        if (subjectId == null || subjectId.isBlank() || roleId == null || roleId.isBlank()) {
            throw new IllegalArgumentException("主体和角色编号不能为空");
        }
        if (!roles.containsKey(roleId)) {
            throw new IllegalArgumentException("角色不存在");
        }
        subjectRoles.computeIfAbsent(subjectId, ignored -> ConcurrentHashMap.newKeySet()).add(roleId);
    }

    /** 判断主体是否通过已注册角色拥有给定权限。 */
    public boolean hasPermission(String subjectId, Permission permission) {
        if (subjectId == null || subjectId.isBlank() || permission == null) {
            return false;
        }
        return subjectRoles.getOrDefault(subjectId, Set.of()).stream()
                .map(roles::get)
                .filter(role -> role != null)
                .anyMatch(role -> role.permissionIds().contains(permission.permissionId()));
    }
}

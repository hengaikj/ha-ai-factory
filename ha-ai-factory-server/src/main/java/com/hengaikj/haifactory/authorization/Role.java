package com.hengaikj.haifactory.authorization;

import java.util.Set;

/** 表示内部权限演示用角色及其不可变权限编号集合。 */
public record Role(String roleId, Set<String> permissionIds) {
    public Role {
        if (roleId == null || roleId.isBlank()) {
            throw new IllegalArgumentException("角色编号不能为空");
        }
        permissionIds = Set.copyOf(permissionIds);
    }
}

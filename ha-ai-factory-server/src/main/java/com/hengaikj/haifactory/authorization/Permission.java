package com.hengaikj.haifactory.authorization;

/** 表示内部权限演示用的单个权限编号。 */
public record Permission(String permissionId) {
    public Permission {
        if (permissionId == null || permissionId.isBlank()) {
            throw new IllegalArgumentException("权限编号不能为空");
        }
    }
}

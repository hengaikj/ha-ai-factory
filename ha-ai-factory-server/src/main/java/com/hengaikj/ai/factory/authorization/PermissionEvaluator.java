package com.hengaikj.ai.factory.authorization;

/** 使用默认拒绝策略执行内存权限判断，不负责认证或最终项目授权策略。 */
public class PermissionEvaluator {
    private final InMemoryRoleAssignments assignments;

    public PermissionEvaluator(InMemoryRoleAssignments assignments) {
        this.assignments = assignments;
    }

    /** 主体缺少角色、角色不存在或权限编号无效时均拒绝访问。 */
    public boolean hasPermission(String subjectId, String permissionId) {
        if (permissionId == null || permissionId.isBlank()) {
            return false;
        }
        return assignments.hasPermission(subjectId, new Permission(permissionId));
    }
}

package com.hengaikj.ai.factory.project;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import java.util.List;

/** 在创建项目事务中同时写入活动Owner成员与OWNER角色。 */
@Mapper
public interface ProjectMembershipMapper {
    @Insert("INSERT INTO project_members(project_id,principal_ref,membership_status) VALUES(#{projectId},#{principalRef},'ACTIVE')")
    int insertActiveMember(@Param("projectId") long projectId, @Param("principalRef") String principalRef);

    @Insert("INSERT INTO project_member_roles(project_id,principal_ref,role_code,assigned_by_ref) VALUES(#{projectId},#{principalRef},'OWNER',#{principalRef})")
    int insertOwnerRole(@Param("projectId") long projectId, @Param("principalRef") String principalRef);

    /** 查询项目成员及其可组合角色，只有活动成员进入结果。 */
    @Select("SELECT m.principal_ref AS principalRef,p.display_name AS displayName,m.joined_at AS joinedAt," +
            "GROUP_CONCAT(r.role_code ORDER BY r.role_code SEPARATOR ',') AS roles " +
            "FROM project_members m JOIN principals p ON p.principal_ref=m.principal_ref " +
            "LEFT JOIN project_member_roles r ON r.project_id=m.project_id AND r.principal_ref=m.principal_ref " +
            "WHERE m.project_id=#{projectId} AND m.membership_status='ACTIVE' GROUP BY m.principal_ref,p.display_name,m.joined_at ORDER BY m.joined_at")
    List<MemberRow> listActive(@Param("projectId") long projectId);

    /** 判断操作者是否拥有项目管理角色。 */
    @Select("SELECT COUNT(*) FROM project_member_roles r JOIN project_members m ON m.project_id=r.project_id AND m.principal_ref=r.principal_ref " +
            "WHERE r.project_id=#{projectId} AND r.principal_ref=#{principalRef} AND m.membership_status='ACTIVE' " +
            "AND r.role_code IN ('OWNER','PROJECT_ADMIN')")
    int countProjectAdmin(@Param("projectId") long projectId, @Param("principalRef") String principalRef);

    /** 检查目标主体是否为已登记且可用的身份。 */
    @Select("SELECT COUNT(*) FROM principals WHERE principal_ref=#{principalRef} AND is_active=TRUE")
    int countActivePrincipal(@Param("principalRef") String principalRef);

    /** 恢复或新增活动成员关系。 */
    @Update("INSERT INTO project_members(project_id,principal_ref,membership_status,joined_at,revoked_at) VALUES(#{projectId},#{principalRef},'ACTIVE',NOW(3),NULL) " +
            "ON DUPLICATE KEY UPDATE membership_status='ACTIVE',revoked_at=NULL")
    int activateMember(@Param("projectId") long projectId, @Param("principalRef") String principalRef);

    /** 替换目标成员的全部角色。 */
    @Delete("DELETE FROM project_member_roles WHERE project_id=#{projectId} AND principal_ref=#{principalRef}")
    int deleteRoles(@Param("projectId") long projectId, @Param("principalRef") String principalRef);

    /** 写入一个项目角色。 */
    @Update("INSERT INTO project_member_roles(project_id,principal_ref,role_code,assigned_by_ref) VALUES(#{projectId},#{principalRef},#{roleCode},#{assignedByRef})")
    int insertRole(@Param("projectId") long projectId, @Param("principalRef") String principalRef,
                   @Param("roleCode") String roleCode, @Param("assignedByRef") String assignedByRef);

    /** 查询当前项目Owner数量。 */
    @Select("SELECT COUNT(*) FROM project_member_roles r JOIN project_members m ON m.project_id=r.project_id AND m.principal_ref=r.principal_ref " +
            "WHERE r.project_id=#{projectId} AND r.role_code='OWNER' AND m.membership_status='ACTIVE'")
    int countOwners(@Param("projectId") long projectId);

    /** 立即撤销成员资格。 */
    @Update("UPDATE project_members SET membership_status='REVOKED',revoked_at=NOW(3) WHERE project_id=#{projectId} AND principal_ref=#{principalRef} AND membership_status='ACTIVE'")
    int revokeMember(@Param("projectId") long projectId, @Param("principalRef") String principalRef);

    /** 保持项目摘要Owner引用与最早活动Owner一致。 */
    @Update("UPDATE projects SET owner_ref=#{principalRef} WHERE id=#{projectId}")
    int updateOwnerRef(@Param("projectId") long projectId, @Param("principalRef") String principalRef);
}

/** 成员查询行，roles 使用逗号分隔后在仓储层转换为集合。 */
class MemberRow {
    private String principalRef;
    private String displayName;
    private java.time.Instant joinedAt;
    private String roles;
    public String getPrincipalRef() { return principalRef; }
    public void setPrincipalRef(String value) { principalRef = value; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { displayName = value; }
    public java.time.Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(java.time.Instant value) { joinedAt = value; }
    public String getRoles() { return roles; }
    public void setRoles(String value) { roles = value; }
}

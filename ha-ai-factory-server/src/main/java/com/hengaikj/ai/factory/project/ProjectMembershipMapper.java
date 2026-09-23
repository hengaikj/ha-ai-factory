package com.hengaikj.ai.factory.project;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 在创建项目事务中同时写入活动Owner成员与OWNER角色。 */
@Mapper
public interface ProjectMembershipMapper {
    @Insert("INSERT INTO project_members(project_id,principal_ref,membership_status) VALUES(#{projectId},#{principalRef},'ACTIVE')")
    int insertActiveMember(@Param("projectId") long projectId, @Param("principalRef") String principalRef);

    @Insert("INSERT INTO project_member_roles(project_id,principal_ref,role_code,assigned_by_ref) VALUES(#{projectId},#{principalRef},'OWNER',#{principalRef})")
    int insertOwnerRole(@Param("projectId") long projectId, @Param("principalRef") String principalRef);
}

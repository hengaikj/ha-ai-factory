package com.hengaikj.ai.factory.project;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.Instant;
import java.util.List;

/** 项目Mapper；成员边界通过活动成员关联表约束查询。 */
@Mapper
public interface ProjectMapper {
    /** 持久化新项目并从服务端主体分配当前Owner。 */
    @Insert("INSERT INTO projects(name,description,tech_stack,current_phase,owner_ref,created_by_ref) " +
            "VALUES(#{name},#{description},#{techStack},'DISCOVERY',#{ownerRef},#{ownerRef})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProjectRow project);

    /** 按活动成员关系和服务端搜索条件分页读取项目及真实Gate/Issue摘要。 */
    @Select("SELECT p.id,p.name,p.description,p.tech_stack AS techStack,p.current_phase AS currentPhase," +
            "p.owner_ref AS ownerRef,p.created_at AS createdAt,p.updated_at AS updatedAt," +
            "COALESCE((SELECT CASE g.status WHEN 'READY_FOR_REVIEW' THEN 'PENDING' ELSE g.status END FROM project_gates g " +
            "WHERE g.project_id=p.id AND g.phase=p.current_phase),'PENDING') AS gateStatus," +
            "(SELECT COUNT(*) FROM open_issues i WHERE i.project_id=p.id " +
            "AND i.status IN ('OPEN','HUMAN_DECISION_REQUIRED','TRACKING')) AS openIssueCount " +
            "FROM projects p JOIN project_members m ON m.project_id=p.id " +
            "WHERE m.principal_ref=#{principalRef} AND m.membership_status='ACTIVE' " +
            "AND (#{query} IS NULL OR p.name LIKE CONCAT('%',#{query},'%') OR p.description LIKE CONCAT('%',#{query},'%')) " +
            "ORDER BY p.updated_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<ProjectRow> listActiveForPrincipal(@Param("principalRef") String principalRef, @Param("query") String query,
                                            @Param("limit") int limit, @Param("offset") int offset);

    /** 按项目成员关系读取单个项目及其摘要。 */
    @Select("SELECT p.id,p.name,p.description,p.tech_stack AS techStack,p.current_phase AS currentPhase,p.owner_ref AS ownerRef,p.created_at AS createdAt,p.updated_at AS updatedAt,COALESCE((SELECT CASE g.status WHEN 'READY_FOR_REVIEW' THEN 'PENDING' ELSE g.status END FROM project_gates g WHERE g.project_id=p.id AND g.phase=p.current_phase),'PENDING') AS gateStatus,(SELECT COUNT(*) FROM open_issues i WHERE i.project_id=p.id AND i.status IN ('OPEN','HUMAN_DECISION_REQUIRED','TRACKING')) AS openIssueCount FROM projects p JOIN project_members m ON m.project_id=p.id WHERE p.id=#{projectId} AND m.principal_ref=#{principalRef} AND m.membership_status='ACTIVE'")
    ProjectRow findActiveForPrincipal(@Param("principalRef") String principalRef, @Param("projectId") long projectId);
    /** 更新项目元数据；空参数保留已有值以符合合并补丁语义。 */
    @Update("UPDATE projects SET name=COALESCE(#{name},name), description=COALESCE(#{description},description), tech_stack=COALESCE(#{techStack},tech_stack) WHERE id=#{projectId}")
    int updateMetadata(@Param("projectId") long projectId, @Param("name") String name, @Param("description") String description, @Param("techStack") String techStack);
    /** 更新项目生命周期阶段。 */
    @Update("UPDATE projects SET current_phase=#{targetPhase} WHERE id=#{projectId}") int updatePhase(@Param("projectId") long projectId, @Param("targetPhase") String targetPhase);

    /** 以相同成员与搜索条件计算项目总数，供分页界面显示。 */
    @Select("SELECT COUNT(*) FROM projects p JOIN project_members m ON m.project_id=p.id " +
            "WHERE m.principal_ref=#{principalRef} AND m.membership_status='ACTIVE' " +
            "AND (#{query} IS NULL OR p.name LIKE CONCAT('%',#{query},'%') OR p.description LIKE CONCAT('%',#{query},'%'))")
    long countActiveForPrincipal(@Param("principalRef") String principalRef, @Param("query") String query);
}

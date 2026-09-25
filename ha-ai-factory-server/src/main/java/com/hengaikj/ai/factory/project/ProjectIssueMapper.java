package com.hengaikj.ai.factory.project;

import org.apache.ibatis.annotations.*;
import java.time.Instant;
import java.util.List;

/** 项目Open Issue和人工决策持久化Mapper。 */
@Mapper
public interface ProjectIssueMapper {
    /** 查询项目待决事项。 */
    @Select("SELECT id,project_id AS projectId,code,title,description,impact,decision_role AS decisionRole,status,decision,created_by_ref AS createdByRef,decided_by_ref AS decidedByRef,created_at AS createdAt,decided_at AS decidedAt FROM open_issues WHERE project_id=#{projectId} ORDER BY created_at DESC")
    List<IssueRow> list(@Param("projectId") long projectId);
    /** 查询单个事项。 */
    @Select("SELECT id,project_id AS projectId,code,title,description,impact,decision_role AS decisionRole,status,decision,created_by_ref AS createdByRef,decided_by_ref AS decidedByRef,created_at AS createdAt,decided_at AS decidedAt FROM open_issues WHERE id=#{id}")
    IssueRow find(@Param("id") long id);
    /** 创建事项。 */
    @Insert("INSERT INTO open_issues(project_id,code,title,description,impact,decision_role,status,created_by_ref) VALUES(#{projectId},#{code},#{title},#{description},#{impact},#{decisionRole},#{status},#{createdByRef})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insert(IssueRow row);
    /** 记录人工决策并更新状态。 */
    @Update("UPDATE open_issues SET decision=#{decision},status=#{status},decided_by_ref=#{decidedByRef},decided_at=NOW(3) WHERE id=#{id} AND project_id=#{projectId}")
    int decide(IssueRow row);
}

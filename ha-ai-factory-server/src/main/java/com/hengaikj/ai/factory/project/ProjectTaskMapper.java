package com.hengaikj.ai.factory.project;

import org.apache.ibatis.annotations.*;
import java.time.Instant;
import java.util.List;

/** 项目任务持久化Mapper，所有查询均绑定项目范围。 */
@Mapper
public interface ProjectTaskMapper {
    /** 查询项目任务列表。 */
    @Select("SELECT id,project_id AS projectId,title,description,phase,assignee_ref AS assigneeRef,assignee_role AS assigneeRole,status,created_by_ref AS createdByRef,created_at AS createdAt,updated_at AS updatedAt FROM project_tasks WHERE project_id=#{projectId} AND (#{status} IS NULL OR status=#{status}) ORDER BY updated_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<TaskRow> list(@Param("projectId") long projectId, @Param("status") String status, @Param("limit") int limit, @Param("offset") int offset);
    /** 计算项目任务总数。 */
    @Select("SELECT COUNT(*) FROM project_tasks WHERE project_id=#{projectId} AND (#{status} IS NULL OR status=#{status})")
    long count(@Param("projectId") long projectId, @Param("status") String status);
    /** 查询单个项目任务。 */
    @Select("SELECT id,project_id AS projectId,title,description,phase,assignee_ref AS assigneeRef,assignee_role AS assigneeRole,status,created_by_ref AS createdByRef,created_at AS createdAt,updated_at AS updatedAt FROM project_tasks WHERE id=#{taskId}")
    TaskRow find(@Param("taskId") long taskId);
    /** 创建任务。 */
    @Insert("INSERT INTO project_tasks(project_id,title,description,phase,assignee_ref,assignee_role,status,created_by_ref) VALUES(#{projectId},#{title},#{description},#{phase},#{assigneeRef},#{assigneeRole},'NOT_STARTED',#{createdByRef})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insert(TaskRow row);
    /** 更新任务字段，空值字段保留原值由仓储层传入完整快照。 */
    @Update("UPDATE project_tasks SET title=#{title},description=#{description},assignee_ref=#{assigneeRef},assignee_role=#{assigneeRole},status=#{status} WHERE id=#{id} AND project_id=#{projectId}")
    int update(TaskRow row);
}

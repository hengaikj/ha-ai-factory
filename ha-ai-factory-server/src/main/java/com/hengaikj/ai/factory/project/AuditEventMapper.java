package com.hengaikj.ai.factory.project;
import org.apache.ibatis.annotations.*;
import java.util.List;
/** 项目审计事件持久化Mapper。 */
@Mapper public interface AuditEventMapper {
  /** 分页查询项目审计事件。 */
  @Select("SELECT id,object_type AS objectType,object_id AS objectId,action,before_state AS beforeState,after_state AS afterState,actor_ref AS actorRef,comment,evidence_refs AS evidenceRefs,occurred_at AS occurredAt FROM audit_events WHERE project_id=#{projectId} AND (#{objectType} IS NULL OR object_type=#{objectType}) ORDER BY occurred_at DESC LIMIT #{limit} OFFSET #{offset}") List<AuditEventRow> list(@Param("projectId") long projectId,@Param("objectType") String objectType,@Param("limit") int limit,@Param("offset") int offset);
  /** 统计项目审计事件。 */
  @Select("SELECT COUNT(*) FROM audit_events WHERE project_id=#{projectId} AND (#{objectType} IS NULL OR object_type=#{objectType})") long count(@Param("projectId") long projectId,@Param("objectType") String objectType);
  /** 写入项目状态审计事件。 */
  @Insert("INSERT INTO audit_events(project_id,object_type,object_id,action,before_state,after_state,actor_ref,comment,evidence_refs) VALUES(#{projectId},#{objectType},#{objectId},#{action},#{beforeState},#{afterState},#{actorRef},#{comment},#{evidenceRefs})") int insert(AuditEventRow row);
}

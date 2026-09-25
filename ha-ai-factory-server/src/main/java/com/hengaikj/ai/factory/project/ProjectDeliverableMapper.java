package com.hengaikj.ai.factory.project;

import org.apache.ibatis.annotations.*;
import java.time.Instant;
import java.util.List;

/** 项目交付物及独立评审持久化Mapper。 */
@Mapper
public interface ProjectDeliverableMapper {
    /** 查询项目交付物列表。 */
    @Select("SELECT id,project_id AS projectId,task_id AS taskId,title,phase,version,source_ref AS sourceRef,review_status AS reviewStatus,created_by_ref AS createdByRef,created_at AS createdAt,updated_at AS updatedAt FROM project_deliverables WHERE project_id=#{projectId} ORDER BY updated_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<DeliverableRow> list(@Param("projectId") long projectId, @Param("limit") int limit, @Param("offset") int offset);
    /** 统计项目交付物总数。 */
    @Select("SELECT COUNT(*) FROM project_deliverables WHERE project_id=#{projectId}")
    long count(@Param("projectId") long projectId);
    /** 查询交付物详情。 */
    @Select("SELECT id,project_id AS projectId,task_id AS taskId,title,phase,version,source_ref AS sourceRef,review_status AS reviewStatus,created_by_ref AS createdByRef,created_at AS createdAt,updated_at AS updatedAt FROM project_deliverables WHERE id=#{id}")
    DeliverableRow find(@Param("id") long id);
    /** 登记仓库引用型交付物。 */
    @Insert("INSERT INTO project_deliverables(project_id,task_id,title,phase,version,source_ref,created_by_ref) VALUES(#{projectId},#{taskId},#{title},#{phase},#{version},#{sourceRef},#{createdByRef})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insert(DeliverableRow row);
    /** 写入独立评审并推进交付物评审状态。 */
    @Insert("INSERT INTO deliverable_reviews(deliverable_id,reviewer_ref,outcome,review_comment,evidence_refs) VALUES(#{deliverableId},#{reviewerRef},#{outcome},#{comment},#{evidenceRefs})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insertReview(ReviewRow row);
    @Update("UPDATE project_deliverables SET review_status=#{status} WHERE id=#{id}") int updateStatus(@Param("id") long id, @Param("status") String status);
}

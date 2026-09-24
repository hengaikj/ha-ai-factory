package com.hengaikj.ai.factory.project;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** 项目Gate及检查项持久化Mapper。 */
@Mapper
public interface ProjectGateMapper {
    /** 查询项目Gate。 */
    @Select("SELECT id,project_id AS projectId,phase,status,submitted_by_ref AS submittedByRef,decision_owner_ref AS decisionOwnerRef,submitted_at AS submittedAt FROM project_gates WHERE project_id=#{projectId} ORDER BY id")
    List<GateRow> list(@Param("projectId") long projectId);
    /** 查询单个Gate。 */
    @Select("SELECT id,project_id AS projectId,phase,status,submitted_by_ref AS submittedByRef,decision_owner_ref AS decisionOwnerRef,submitted_at AS submittedAt FROM project_gates WHERE id=#{gateId}") GateRow find(@Param("gateId") long gateId);
    /** 创建或读取阶段Gate。 */
    @Insert("INSERT INTO project_gates(project_id,phase) VALUES(#{projectId},#{phase}) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)")
    @Options(useGeneratedKeys=true,keyProperty="id") int ensure(GateRow row);
    /** 提交Gate及指定决策责任人。 */
    @Update("UPDATE project_gates SET status='READY_FOR_REVIEW',submitted_by_ref=#{submittedByRef},decision_owner_ref=#{decisionOwnerRef},submitted_at=NOW(3) WHERE id=#{id} AND project_id=#{projectId}") int submit(GateRow row);
    /** 查询Gate检查项。 */
    @Select("SELECT id,gate_id AS gateId,code,title,status,reviewer_ref AS reviewerRef,comment,evidence_refs AS evidenceRefs FROM gate_checks WHERE gate_id=#{gateId} ORDER BY id") List<GateCheckRow> checks(@Param("gateId") long gateId);
    /** 新增检查项。 */
    @Insert("INSERT INTO gate_checks(gate_id,code,title) VALUES(#{gateId},#{code},#{title}) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)") @Options(useGeneratedKeys=true,keyProperty="id") int ensureCheck(GateCheckRow row);
    /** 更新检查结论。 */
    @Update("UPDATE gate_checks SET status=#{status},reviewer_ref=#{reviewerRef},comment=#{comment},evidence_refs=#{evidenceRefs} WHERE id=#{id} AND gate_id=#{gateId}") int decideCheck(GateCheckRow row);
    /** 查询Gate是否存在Reviewer冲突主体。 */
    @Select("SELECT COUNT(*) FROM project_gates g LEFT JOIN project_tasks t ON t.project_id=g.project_id AND t.id IN (SELECT task_id FROM gate_scope_tasks WHERE gate_id=g.id) LEFT JOIN project_deliverables d ON d.project_id=g.project_id AND d.id IN (SELECT deliverable_id FROM gate_scope_deliverables WHERE gate_id=g.id) WHERE g.id=#{gateId} AND (g.submitted_by_ref=#{principalRef} OR g.decision_owner_ref=#{principalRef} OR t.assignee_ref=#{principalRef} OR d.created_by_ref=#{principalRef})") int countConflict(@Param("gateId") long gateId, @Param("principalRef") String principalRef);
    /** 更新Gate状态。 */
    @Update("UPDATE project_gates SET status=#{status} WHERE id=#{id}") int updateStatus(@Param("id") long id, @Param("status") String status);
    /** 写入任务和交付物范围。 */
    @Insert("INSERT INTO gate_scope_tasks(project_id,gate_id,task_id) VALUES(#{projectId},#{gateId},#{taskId})") int addTaskScope(@Param("projectId") long projectId, @Param("gateId") long gateId, @Param("taskId") long taskId);
    @Insert("INSERT INTO gate_scope_deliverables(project_id,gate_id,deliverable_id) VALUES(#{projectId},#{gateId},#{deliverableId})") int addDeliverableScope(@Param("projectId") long projectId, @Param("gateId") long gateId, @Param("deliverableId") long deliverableId);
    @Select("SELECT task_id FROM gate_scope_tasks WHERE gate_id=#{gateId}") List<Long> taskIds(@Param("gateId") long gateId);
    @Select("SELECT deliverable_id FROM gate_scope_deliverables WHERE gate_id=#{gateId}") List<Long> deliverableIds(@Param("gateId") long gateId);
    @Select("SELECT COUNT(*) FROM gate_checks WHERE gate_id=#{gateId} AND status='PENDING'") int pendingChecks(@Param("gateId") long gateId);
    /** 查询未通过的Gate检查项，批准Gate时必须为零。 */
    @Select("SELECT COUNT(*) FROM gate_checks WHERE gate_id=#{gateId} AND status <> 'PASSED'") int nonPassedChecks(@Param("gateId") long gateId);
}

package com.hengaikj.ai.factory.project;
import org.apache.ibatis.annotations.*;
/** Agent Runtime配置状态Mapper，不读取密钥内容。 */
@Mapper public interface RuntimeConfigMapper {
  /** 查询项目配置状态。 */
  @Select("SELECT id,project_id AS projectId,status,model_ref AS modelRef,approved_at AS approvedAt,expires_at AS expiresAt FROM runtime_configs WHERE project_id=#{projectId}") RuntimeConfigRow find(@Param("projectId") long projectId);
}

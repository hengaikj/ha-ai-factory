package com.hengaikj.ai.factory.project;
import org.apache.ibatis.annotations.*;
import java.time.Instant;
import java.util.List;

/** 项目模板和规则索引持久化Mapper。 */
@Mapper
public interface ProjectResourceMapper {
    /** 按阶段和类型筛选资源索引。 */
    @Select("SELECT id,project_id AS projectId,kind,title,phase,version,source_ref AS sourceRef,source_status AS sourceStatus,created_at AS createdAt FROM project_resources WHERE project_id=#{projectId} AND (#{phase} IS NULL OR phase=#{phase}) AND (#{kind} IS NULL OR kind=#{kind}) ORDER BY phase,title")
    List<ResourceRow> list(@Param("projectId") long projectId, @Param("phase") String phase, @Param("kind") String kind);
}


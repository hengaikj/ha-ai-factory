package com.hengaikj.ai.factory.project;
import java.time.Instant;
/** 交付物数据库行，公开类型供MyBatis运行时代理访问。 */
public class DeliverableRow {
    private Long id, projectId, taskId; private String title, phase, version, sourceRef, reviewStatus, createdByRef; private Instant createdAt, updatedAt;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getProjectId(){return projectId;} public void setProjectId(Long v){projectId=v;} public Long getTaskId(){return taskId;} public void setTaskId(Long v){taskId=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getPhase(){return phase;} public void setPhase(String v){phase=v;} public String getVersion(){return version;} public void setVersion(String v){version=v;} public String getSourceRef(){return sourceRef;} public void setSourceRef(String v){sourceRef=v;} public String getReviewStatus(){return reviewStatus;} public void setReviewStatus(String v){reviewStatus=v;} public String getCreatedByRef(){return createdByRef;} public void setCreatedByRef(String v){createdByRef=v;} public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;} public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}

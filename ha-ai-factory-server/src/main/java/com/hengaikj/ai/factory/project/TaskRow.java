package com.hengaikj.ai.factory.project;

import java.time.Instant;

/** 任务数据库行映射，公开类型供MyBatis运行时代理访问。 */
public class TaskRow {
    private Long id; private Long projectId; private String title; private String description; private String phase;
    private String assigneeRef; private String assigneeRole; private String status; private String createdByRef;
    private Instant createdAt; private Instant updatedAt;
    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getProjectId(){return projectId;} public void setProjectId(Long v){projectId=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getPhase(){return phase;} public void setPhase(String v){phase=v;}
    public String getAssigneeRef(){return assigneeRef;} public void setAssigneeRef(String v){assigneeRef=v;}
    public String getAssigneeRole(){return assigneeRole;} public void setAssigneeRole(String v){assigneeRole=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getCreatedByRef(){return createdByRef;} public void setCreatedByRef(String v){createdByRef=v;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
    public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;}
}

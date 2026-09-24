package com.hengaikj.ai.factory.project;
import java.time.Instant;
/** Runtime配置状态数据库行。 */
public class RuntimeConfigRow { private Long id,projectId; private String status,modelRef; private Instant approvedAt,expiresAt; public Long getId(){return id;} public void setId(Long v){id=v;} public Long getProjectId(){return projectId;} public void setProjectId(Long v){projectId=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getModelRef(){return modelRef;} public void setModelRef(String v){modelRef=v;} public Instant getApprovedAt(){return approvedAt;} public void setApprovedAt(Instant v){approvedAt=v;} public Instant getExpiresAt(){return expiresAt;} public void setExpiresAt(Instant v){expiresAt=v;} }

package com.hengaikj.ai.factory.project;
import java.time.Instant;
/** 资源索引数据库行，公开类型供MyBatis运行时代理访问。 */
public class ResourceRow {
    private Long id, projectId; private String kind,title,phase,version,sourceRef,sourceStatus; private Instant createdAt;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getProjectId(){return projectId;} public void setProjectId(Long v){projectId=v;} public String getKind(){return kind;} public void setKind(String v){kind=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getPhase(){return phase;} public void setPhase(String v){phase=v;} public String getVersion(){return version;} public void setVersion(String v){version=v;} public String getSourceRef(){return sourceRef;} public void setSourceRef(String v){sourceRef=v;} public String getSourceStatus(){return sourceStatus;} public void setSourceStatus(String v){sourceStatus=v;} public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;}
}

package com.hengaikj.ai.factory.project;
import java.time.Instant;
/** MyBatis项目行映射对象，公开类型供运行时代理访问。 */
public class ProjectRow {
    private Long id; private String name,description,techStack,currentPhase,ownerRef,gateStatus; private Instant createdAt,updatedAt; private int openIssueCount;
    public Long getId(){return id;} public void setId(Long v){id=v;} public String getName(){return name;} public void setName(String v){name=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;} public String getTechStack(){return techStack;} public void setTechStack(String v){techStack=v;} public String getCurrentPhase(){return currentPhase;} public void setCurrentPhase(String v){currentPhase=v;} public String getOwnerRef(){return ownerRef;} public void setOwnerRef(String v){ownerRef=v;} public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant v){createdAt=v;} public Instant getUpdatedAt(){return updatedAt;} public void setUpdatedAt(Instant v){updatedAt=v;} public String getGateStatus(){return gateStatus;} public void setGateStatus(String v){gateStatus=v;} public int getOpenIssueCount(){return openIssueCount;} public void setOpenIssueCount(int v){openIssueCount=v;}
}

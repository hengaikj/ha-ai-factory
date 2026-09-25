package com.hengaikj.ai.factory.project;
import java.time.Instant;
/** Gate数据库行。 */
public class GateRow { private Long id,projectId; private String phase,status,submittedByRef,decisionOwnerRef; private Instant submittedAt;
 public Long getId(){return id;} public void setId(Long v){id=v;} public Long getProjectId(){return projectId;} public void setProjectId(Long v){projectId=v;} public String getPhase(){return phase;} public void setPhase(String v){phase=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getSubmittedByRef(){return submittedByRef;} public void setSubmittedByRef(String v){submittedByRef=v;} public String getDecisionOwnerRef(){return decisionOwnerRef;} public void setDecisionOwnerRef(String v){decisionOwnerRef=v;} public Instant getSubmittedAt(){return submittedAt;} public void setSubmittedAt(Instant v){submittedAt=v;} }

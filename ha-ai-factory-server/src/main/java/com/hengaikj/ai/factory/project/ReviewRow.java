package com.hengaikj.ai.factory.project;
/** 交付物评审数据库行，公开类型供MyBatis运行时代理访问。 */
public class ReviewRow {
    private Long id, deliverableId; private String reviewerRef, outcome, comment, evidenceRefs;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getDeliverableId(){return deliverableId;} public void setDeliverableId(Long v){deliverableId=v;} public String getReviewerRef(){return reviewerRef;} public void setReviewerRef(String v){reviewerRef=v;} public String getOutcome(){return outcome;} public void setOutcome(String v){outcome=v;} public String getComment(){return comment;} public void setComment(String v){comment=v;} public String getEvidenceRefs(){return evidenceRefs;} public void setEvidenceRefs(String v){evidenceRefs=v;}
}

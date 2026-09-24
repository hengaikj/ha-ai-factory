package com.hengaikj.ai.factory.project;

import java.time.Instant;

/** Gate最终决定历史数据库行。 */
public class GateDecisionRow {
    private String reviewerRef;
    private String decision;
    private String comment;
    private Instant decidedAt;

    public String getReviewerRef() { return reviewerRef; }
    public void setReviewerRef(String value) { reviewerRef = value; }
    public String getDecision() { return decision; }
    public void setDecision(String value) { decision = value; }
    public String getComment() { return comment; }
    public void setComment(String value) { comment = value; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant value) { decidedAt = value; }
}

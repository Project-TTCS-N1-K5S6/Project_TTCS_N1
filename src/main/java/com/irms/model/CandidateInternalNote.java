package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class CandidateInternalNote implements Serializable {
    private static final long serialVersionUID = 1L;

    private String content;
    private String authorName;
    private Timestamp createdAt;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

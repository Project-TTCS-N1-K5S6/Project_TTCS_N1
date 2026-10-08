package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class Question implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String content;
    private String difficultyLevel; // EASY, MEDIUM, HARD
    private String goodAnswerSuggestion;
    private String criterionId;
    private String jobTitle;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Transient fields for JOIN (e.g., getting criterion name)
    private String criterionName;

    public Question() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public String getGoodAnswerSuggestion() { return goodAnswerSuggestion; }
    public void setGoodAnswerSuggestion(String goodAnswerSuggestion) { this.goodAnswerSuggestion = goodAnswerSuggestion; }

    public String getCriterionId() { return criterionId; }
    public void setCriterionId(String criterionId) { this.criterionId = criterionId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getCriterionName() { return criterionName; }
    public void setCriterionName(String criterionName) { this.criterionName = criterionName; }
}

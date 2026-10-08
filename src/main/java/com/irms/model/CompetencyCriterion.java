package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Tiêu chí năng lực đánh giá (bảng competency_criteria)
 */
public class CompetencyCriterion implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String code;
    private String name;
    private String description;
    private String evaluationGuideline;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public CompetencyCriterion() {
        this.status = "ACTIVE";
    }

    public CompetencyCriterion(String id, String code, String name, String description, String evaluationGuideline, String status) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.evaluationGuideline = evaluationGuideline;
        this.status = (status != null && !status.trim().isEmpty()) ? status : "ACTIVE";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEvaluationGuideline() { return evaluationGuideline; }
    public void setEvaluationGuideline(String evaluationGuideline) { this.evaluationGuideline = evaluationGuideline; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(this.status);
    }
}

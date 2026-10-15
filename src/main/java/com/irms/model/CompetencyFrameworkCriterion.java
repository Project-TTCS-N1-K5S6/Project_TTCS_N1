package com.irms.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Chi tiết Tiêu chí trong Khung năng lực kèm Trọng số & Thứ tự hiển thị
 * (Bảng competency_framework_criteria)
 */
public class CompetencyFrameworkCriterion implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String frameworkId;
    private String criterionId;
    private String criterionCode;
    private String criterionName;
    private String criterionDescription;
    private String evaluationGuideline;
    private BigDecimal weight;       // Trọng số (%)
    private int displayOrder;         // Thứ tự hiển thị
    private Timestamp createdAt;

    public CompetencyFrameworkCriterion() {
        this.displayOrder = 1;
        this.weight = BigDecimal.ZERO;
    }

    public CompetencyFrameworkCriterion(String id, String frameworkId, String criterionId, BigDecimal weight, int displayOrder) {
        this.id = id;
        this.frameworkId = frameworkId;
        this.criterionId = criterionId;
        this.weight = weight != null ? weight : BigDecimal.ZERO;
        this.displayOrder = displayOrder;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFrameworkId() { return frameworkId; }
    public void setFrameworkId(String frameworkId) { this.frameworkId = frameworkId; }

    public String getCriterionId() { return criterionId; }
    public void setCriterionId(String criterionId) { this.criterionId = criterionId; }

    public String getCriterionCode() { return criterionCode; }
    public void setCriterionCode(String criterionCode) { this.criterionCode = criterionCode; }

    public String getCriterionName() { return criterionName; }
    public void setCriterionName(String criterionName) { this.criterionName = criterionName; }

    public String getCriterionDescription() { return criterionDescription; }
    public void setCriterionDescription(String criterionDescription) { this.criterionDescription = criterionDescription; }

    public String getEvaluationGuideline() { return evaluationGuideline; }
    public void setEvaluationGuideline(String evaluationGuideline) { this.evaluationGuideline = evaluationGuideline; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getFormattedWeight() {
        if (weight == null) return "0%";
        // Nếu số nguyên hiển thị 30%, nếu có thập phân hiển thị 12.5%
        if (weight.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            return weight.stripTrailingZeros().toPlainString() + "%";
        }
        return weight.toString() + "%";
    }
}

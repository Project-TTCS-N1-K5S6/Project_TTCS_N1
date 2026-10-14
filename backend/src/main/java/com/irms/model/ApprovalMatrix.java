package com.irms.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Đại diện thực thể Cấu hình Ma trận phê duyệt (approval_matrices)
 */
public class ApprovalMatrix implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String code;
    private String name;
    private String description;
    private String departmentId;
    private String departmentName;
    private String departmentCode;
    private String salaryCondition = "RANGE"; // RANGE, WITHIN_STANDARD, EXCEED_STANDARD
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private BigDecimal thresholdAmount;
    private int priority;
    private String status = "ACTIVE"; // ACTIVE, INACTIVE
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String createdBy;
    private String createdByName;
    private String updatedBy;
    private String updatedByName;

    private List<ApprovalMatrixStep> steps = new ArrayList<>();

    public ApprovalMatrix() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getSalaryCondition() { return salaryCondition; }
    public void setSalaryCondition(String salaryCondition) { this.salaryCondition = salaryCondition; }

    public BigDecimal getMinSalary() { return minSalary; }
    public void setMinSalary(BigDecimal minSalary) { this.minSalary = minSalary; }

    public BigDecimal getMaxSalary() { return maxSalary; }
    public void setMaxSalary(BigDecimal maxSalary) { this.maxSalary = maxSalary; }

    public BigDecimal getThresholdAmount() { return thresholdAmount; }
    public void setThresholdAmount(BigDecimal thresholdAmount) { this.thresholdAmount = thresholdAmount; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public String getUpdatedByName() { return updatedByName; }
    public void setUpdatedByName(String updatedByName) { this.updatedByName = updatedByName; }

    public List<ApprovalMatrixStep> getSteps() { return steps; }
    public void setSteps(List<ApprovalMatrixStep> steps) { this.steps = steps; }

    public int getStepCount() {
        return (steps != null) ? steps.size() : 0;
    }

    public String getStatusBadgeClass() {
        if ("ACTIVE".equalsIgnoreCase(status)) return "bg-success-subtle text-success border border-success-subtle";
        return "bg-secondary-subtle text-secondary border border-secondary-subtle";
    }

    public String getStatusLabel() {
        if ("ACTIVE".equalsIgnoreCase(status)) return "Đang áp dụng";
        return "Tạm dừng";
    }

    public String getFormattedSalaryRange() {
        DecimalFormat df = new DecimalFormat("#,###");
        if (minSalary != null && maxSalary != null) {
            return df.format(minSalary) + " - " + df.format(maxSalary) + " đ";
        } else if (maxSalary != null) {
            return "Tối đa " + df.format(maxSalary) + " đ";
        } else if (minSalary != null) {
            return "Từ " + df.format(minSalary) + " đ trở lên";
        } else if (thresholdAmount != null) {
            return "Ngưỡng vượt: " + df.format(thresholdAmount) + " đ";
        }
        return "Tất cả mức lương";
    }

    public String getFormattedCondition() {
        DecimalFormat df = new DecimalFormat("#,###");
        if ("WITHIN_STANDARD".equalsIgnoreCase(salaryCondition)) {
            return (thresholdAmount != null) ? "Trong hạn mức (≤ " + df.format(thresholdAmount) + " đ)" : "Trong hạn mức chuẩn";
        }
        if ("EXCEED_STANDARD".equalsIgnoreCase(salaryCondition)) {
            return (thresholdAmount != null) ? "Vượt hạn mức (> " + df.format(thresholdAmount) + " đ)" : "Vượt hạn mức chuẩn";
        }
        if (thresholdAmount != null) {
            return "Ngưỡng chuyển cấp: " + df.format(thresholdAmount) + " đ";
        }
        return "Theo dải lương cấu hình";
    }

    public String getStepChainSummary() {
        if (steps == null || steps.isEmpty()) return "Chưa cấu hình cấp";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            if (i > 0) sb.append(" → ");
            ApprovalMatrixStep s = steps.get(i);
            sb.append("Cấp ").append(s.getStepOrder()).append(": ").append(s.getStepName());
            if (s.isEscalationStep()) {
                sb.append(" (Vượt ngưỡng)");
            }
        }
        return sb.toString();
    }
}

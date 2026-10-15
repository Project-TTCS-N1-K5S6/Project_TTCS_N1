package com.irms.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể Khung năng lực (bảng competency_frameworks)
 * Dùng làm cơ sở đánh giá ứng viên/nhân sự theo từng chức danh và sinh phiếu phỏng vấn ở Sprint 6.
 */
public class CompetencyFramework implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    private String id;
    private String code;            // Mã khung năng lực (Bắt buộc, duy nhất)
    private String name;            // Tên khung năng lực (Bắt buộc)
    private String description;     // Mô tả mục đích, phạm vi áp dụng
    private String status;          // DRAFT (Nháp), ACTIVE (Đang áp dụng), INACTIVE (Ngừng áp dụng)
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String createdBy;
    private String createdByName;
    private String updatedBy;
    private String updatedByName;

    // Danh sách tiêu chí thành phần kèm trọng số
    private List<CompetencyFrameworkCriterion> criteria = new ArrayList<>();
    private int criteriaCount;
    private BigDecimal totalWeight = BigDecimal.ZERO;

    // Danh sách chức danh đang sử dụng khung năng lực này
    private List<PositionCompetencyFramework> assignedPositions = new ArrayList<>();
    private int assignedPositionsCount;

    public CompetencyFramework() {
        this.status = STATUS_DRAFT;
    }

    public CompetencyFramework(String id, String code, String name, String description, String status) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.status = (status != null && !status.trim().isEmpty()) ? status : STATUS_DRAFT;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

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

    public List<CompetencyFrameworkCriterion> getCriteria() { return criteria; }
    public void setCriteria(List<CompetencyFrameworkCriterion> criteria) {
        this.criteria = criteria != null ? criteria : new ArrayList<>();
        calculateMetrics();
    }

    public int getCriteriaCount() {
        if (criteriaCount > 0) return criteriaCount;
        return criteria != null ? criteria.size() : 0;
    }
    public void setCriteriaCount(int criteriaCount) { this.criteriaCount = criteriaCount; }

    public BigDecimal getTotalWeight() {
        if (totalWeight != null && totalWeight.compareTo(BigDecimal.ZERO) > 0) return totalWeight;
        calculateMetrics();
        return totalWeight;
    }
    public void setTotalWeight(BigDecimal totalWeight) { this.totalWeight = totalWeight; }

    public List<PositionCompetencyFramework> getAssignedPositions() { return assignedPositions; }
    public void setAssignedPositions(List<PositionCompetencyFramework> assignedPositions) {
        this.assignedPositions = assignedPositions != null ? assignedPositions : new ArrayList<>();
        this.assignedPositionsCount = this.assignedPositions.size();
    }

    public int getAssignedPositionsCount() {
        if (assignedPositionsCount > 0) return assignedPositionsCount;
        return assignedPositions != null ? assignedPositions.size() : 0;
    }
    public void setAssignedPositionsCount(int assignedPositionsCount) { this.assignedPositionsCount = assignedPositionsCount; }

    /**
     * Tính toán tổng trọng số và số lượng tiêu chí
     */
    public void calculateMetrics() {
        if (criteria == null || criteria.isEmpty()) {
            this.totalWeight = BigDecimal.ZERO;
            this.criteriaCount = 0;
            return;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (CompetencyFrameworkCriterion c : criteria) {
            if (c.getWeight() != null) {
                sum = sum.add(c.getWeight());
            }
        }
        this.totalWeight = sum;
        this.criteriaCount = criteria.size();
    }

    public boolean isDraft() {
        return STATUS_DRAFT.equalsIgnoreCase(this.status);
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equalsIgnoreCase(this.status);
    }

    public boolean isInactive() {
        return STATUS_INACTIVE.equalsIgnoreCase(this.status);
    }

    public boolean isWeightValid() {
        BigDecimal sum = getTotalWeight();
        return sum != null && sum.compareTo(new BigDecimal("100.00")) == 0;
    }

    public String getFormattedTotalWeight() {
        BigDecimal tw = getTotalWeight();
        if (tw == null) return "0%";
        if (tw.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            return tw.stripTrailingZeros().toPlainString() + "%";
        }
        return tw.toString() + "%";
    }

    public String getStatusBadgeClass() {
        if (isActive()) return "bg-success";
        if (isDraft()) return "bg-secondary";
        return "bg-warning text-dark";
    }

    public String getStatusDisplayName() {
        if (isActive()) return "Đang áp dụng";
        if (isDraft()) return "Bản nháp";
        if (isInactive()) return "Ngừng áp dụng";
        return status != null ? status : "Chưa xác định";
    }

    public String getStatusLabel() {
        return getStatusDisplayName();
    }

    public String getAssignedPositionTitles() {
        if (assignedPositions == null || assignedPositions.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < assignedPositions.size(); i++) {
            if (i > 0) sb.append(", ");
            PositionCompetencyFramework p = assignedPositions.get(i);
            sb.append(p.getPositionTitle() != null ? p.getPositionTitle() : p.getPositionCode());
        }
        return sb.toString();
    }
}

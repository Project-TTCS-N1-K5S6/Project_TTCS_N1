package com.irms.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Thực thể Đại diện Định biên & Ngân sách Nhân sự theo Phòng ban & Năm
 * Bảng: department_headcount_budgets
 */
public class DepartmentHeadcountBudget implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String departmentId;
    private String departmentName;
    private String departmentCode;
    private int fiscalYear;
    private int targetHeadcount;
    private BigDecimal totalSalaryBudget = BigDecimal.ZERO;
    private String currency = "VND";
    private String description;
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, LOCKED
    private String createdBy;
    private String createdByName;
    private Timestamp createdAt;
    private String updatedBy;
    private String updatedByName;
    private Timestamp updatedAt;

    // Các trường tính toán thời gian thực (Real-time computed fields)
    private int usedHeadcount;
    private int remainingHeadcount;
    private BigDecimal committedSalaryBudget = BigDecimal.ZERO;
    private BigDecimal remainingSalaryBudget = BigDecimal.ZERO;

    public DepartmentHeadcountBudget() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public int getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(int fiscalYear) { this.fiscalYear = fiscalYear; }

    public int getTargetHeadcount() { return targetHeadcount; }
    public void setTargetHeadcount(int targetHeadcount) { this.targetHeadcount = targetHeadcount; }

    public BigDecimal getTotalSalaryBudget() { return totalSalaryBudget != null ? totalSalaryBudget : BigDecimal.ZERO; }
    public void setTotalSalaryBudget(BigDecimal totalSalaryBudget) { this.totalSalaryBudget = totalSalaryBudget; }

    public String getCurrency() { return currency != null ? currency : "VND"; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public String getUpdatedByName() { return updatedByName; }
    public void setUpdatedByName(String updatedByName) { this.updatedByName = updatedByName; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public int getUsedHeadcount() { return usedHeadcount; }
    public void setUsedHeadcount(int usedHeadcount) { this.usedHeadcount = usedHeadcount; }

    public int getRemainingHeadcount() { return remainingHeadcount; }
    public void setRemainingHeadcount(int remainingHeadcount) { this.remainingHeadcount = remainingHeadcount; }

    public BigDecimal getCommittedSalaryBudget() { return committedSalaryBudget != null ? committedSalaryBudget : BigDecimal.ZERO; }
    public void setCommittedSalaryBudget(BigDecimal committedSalaryBudget) { this.committedSalaryBudget = committedSalaryBudget; }

    public BigDecimal getRemainingSalaryBudget() { return remainingSalaryBudget != null ? remainingSalaryBudget : BigDecimal.ZERO; }
    public void setRemainingSalaryBudget(BigDecimal remainingSalaryBudget) { this.remainingSalaryBudget = remainingSalaryBudget; }

    // Helpers định dạng và tỷ lệ phần trăm
    public double getHeadcountUsagePercent() {
        if (targetHeadcount <= 0) return 0.0;
        double pct = ((double) usedHeadcount / targetHeadcount) * 100.0;
        return Math.min(100.0, Math.round(pct * 10.0) / 10.0);
    }

    public double getBudgetUsagePercent() {
        if (totalSalaryBudget == null || totalSalaryBudget.compareTo(BigDecimal.ZERO) <= 0) return 0.0;
        BigDecimal committed = getCommittedSalaryBudget();
        double pct = committed.divide(totalSalaryBudget, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
        return Math.min(100.0, Math.round(pct * 10.0) / 10.0);
    }

    public String getFormattedTotalBudget() {
        return formatMoney(getTotalSalaryBudget());
    }

    public String getFormattedCommittedBudget() {
        return formatMoney(getCommittedSalaryBudget());
    }

    public String getFormattedRemainingBudget() {
        return formatMoney(getRemainingSalaryBudget());
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 " + getCurrency();
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(amount) + " " + getCurrency();
    }

    public String getStatusBadgeClass() {
        if ("ACTIVE".equalsIgnoreCase(status)) return "bg-success-subtle text-success border border-success-subtle";
        if ("LOCKED".equalsIgnoreCase(status)) return "bg-secondary-subtle text-secondary border border-secondary-subtle";
        return "bg-warning-subtle text-warning border border-warning-subtle";
    }

    public String getStatusLabel() {
        if ("ACTIVE".equalsIgnoreCase(status)) return "Đang áp dụng";
        if ("LOCKED".equalsIgnoreCase(status)) return "Đã khóa";
        return "Ngừng áp dụng";
    }
}

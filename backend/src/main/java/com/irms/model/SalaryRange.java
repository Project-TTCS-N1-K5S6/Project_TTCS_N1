package com.irms.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Đại diện thực thể Dải lương theo vị trí (bảng salary_ranges)
 */
public class SalaryRange implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String departmentId;
    private String departmentName;
    private String positionTitle;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String currency;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public SalaryRange() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

    public BigDecimal getMinSalary() { return minSalary; }
    public void setMinSalary(BigDecimal minSalary) { this.minSalary = minSalary; }

    public BigDecimal getMaxSalary() { return maxSalary; }
    public void setMaxSalary(BigDecimal maxSalary) { this.maxSalary = maxSalary; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getFormattedRange() {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        String min = minSalary != null ? nf.format(minSalary) : "0";
        String max = maxSalary != null ? nf.format(maxSalary) : "0";
        return min + " - " + max + " " + (currency != null ? currency : "VND");
    }
}

package com.irms.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Đại diện thực thể Dải lương theo vị trí chức danh (bảng salary_ranges)
 * Phục vụ nghiệp vụ Khai báo dải lương & Hạn mức duyệt offer (Jira KN-103)
 */
public class SalaryRange implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String positionCode;   // Mã chức danh (Bắt buộc, duy nhất)
    private String positionTitle;  // Tên chức danh (Bắt buộc)
    private String level;          // Cấp bậc (Bắt buộc, vd: Intern, Fresher, Junior, Senior, Lead, Manager)
    private String departmentId;   // Phòng ban
    private String departmentName; // Tên phòng ban (join từ bảng departments)
    private BigDecimal minSalary;  // Mức lương tối thiểu (Hạn mức sàn)
    private BigDecimal maxSalary;  // Mức lương tối đa (Hạn mức trần duyệt offer)
    private String currency;       // Tiền tệ (VND, USD)
    private String note;           // Ghi chú / Quy định duyệt offer
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public SalaryRange() {
        this.currency = "VND";
    }

    public SalaryRange(String id, String positionCode, String positionTitle, String level, 
                       String departmentId, BigDecimal minSalary, BigDecimal maxSalary, String currency, String note) {
        this.id = id;
        this.positionCode = positionCode;
        this.positionTitle = positionTitle;
        this.level = level;
        this.departmentId = departmentId;
        this.minSalary = minSalary;
        this.maxSalary = maxSalary;
        this.currency = (currency != null && !currency.trim().isEmpty()) ? currency : "VND";
        this.note = note;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPositionCode() { return positionCode; }
    public void setPositionCode(String positionCode) { this.positionCode = positionCode; }

    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public BigDecimal getMinSalary() { return minSalary; }
    public void setMinSalary(BigDecimal minSalary) { this.minSalary = minSalary; }

    public BigDecimal getMaxSalary() { return maxSalary; }
    public void setMaxSalary(BigDecimal maxSalary) { this.maxSalary = maxSalary; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Định dạng mức lương tối thiểu (VD: 12.000.000 VND)
     */
    public String getFormattedMinSalary() {
        if (minSalary == null) return "0 " + (currency != null ? currency : "VND");
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(minSalary) + " " + (currency != null ? currency : "VND");
    }

    /**
     * Định dạng mức lương tối đa (VD: 18.000.000 VND)
     */
    public String getFormattedMaxSalary() {
        if (maxSalary == null) return "0 " + (currency != null ? currency : "VND");
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(maxSalary) + " " + (currency != null ? currency : "VND");
    }

    /**
     * Định dạng dải lương (VD: 12.000.000 - 18.000.000 VND)
     */
    public String getFormattedRange() {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        String min = minSalary != null ? nf.format(minSalary) : "0";
        String max = maxSalary != null ? nf.format(maxSalary) : "0";
        return min + " - " + max + " " + (currency != null ? currency : "VND");
    }

    /**
     * Tiêu chí KN-103: Dải lương dùng làm hạn mức duyệt offer về sau
     * Kiểm tra xem mức offer có nằm trong dải lương tối thiểu và tối đa không.
     */
    public boolean isSalaryWithinRange(BigDecimal offerSalary) {
        if (offerSalary == null || offerSalary.compareTo(BigDecimal.ZERO) <= 0) return false;
        if (minSalary != null && offerSalary.compareTo(minSalary) < 0) return false;
        if (maxSalary != null && offerSalary.compareTo(maxSalary) > 0) return false;
        return true;
    }

    /**
     * Đánh giá trạng thái mức offer so với hạn mức dải lương:
     * - WITHIN_RANGE: Nằm trong hạn mức cho phép
     * - EXCEEDED_MAX: Vượt quá dải lương tối đa (vượt hạn mức ngân sách)
     * - BELOW_MIN: Thấp hơn mức lương tối thiểu của chức danh
     */
    public String checkOfferLimitStatus(BigDecimal offerSalary) {
        if (offerSalary == null || offerSalary.compareTo(BigDecimal.ZERO) <= 0) return "INVALID";
        if (minSalary != null && offerSalary.compareTo(minSalary) < 0) {
            return "BELOW_MIN";
        }
        if (maxSalary != null && offerSalary.compareTo(maxSalary) > 0) {
            return "EXCEEDED_MAX";
        }
        return "WITHIN_RANGE";
    }

    /**
     * Phân loại màu CSS badge theo cấp bậc chức danh
     */
    public String getLevelBadgeClass() {
        if (level == null) return "bg-secondary-subtle text-secondary";
        String lvl = level.trim().toUpperCase();
        if (lvl.contains("INTERN") || lvl.contains("THỰC TẬP")) {
            return "bg-secondary-subtle text-secondary border border-secondary-subtle";
        } else if (lvl.contains("FRESHER")) {
            return "bg-info-subtle text-info border border-info-subtle";
        } else if (lvl.contains("JUNIOR")) {
            return "bg-teal-subtle text-teal border border-teal-subtle";
        } else if (lvl.contains("MIDDLE") || lvl.contains("CHUYÊN VIÊN")) {
            return "bg-primary-subtle text-primary border border-primary-subtle";
        } else if (lvl.contains("SENIOR")) {
            return "bg-indigo-subtle text-indigo border border-indigo-subtle";
        } else if (lvl.contains("LEAD") || lvl.contains("TRƯỞNG NHÓM")) {
            return "bg-purple text-white";
        } else if (lvl.contains("MANAGER") || lvl.contains("TRƯỞNG PHÒNG")) {
            return "bg-warning-subtle text-warning-emphasis border border-warning-subtle";
        } else if (lvl.contains("DIRECTOR") || lvl.contains("GIÁM ĐỐC")) {
            return "bg-danger-subtle text-danger border border-danger-subtle";
        }
        return "bg-light text-dark border";
    }
}

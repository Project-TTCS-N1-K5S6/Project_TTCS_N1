package com.irms.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Đại diện thực thể Yêu cầu tuyển dụng (bảng recruitment_requisitions)
 */
public class RecruitmentRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String code;
    private String title;
    private String positionTitle;
    private String departmentId;
    private String departmentName;
    private int headcount = 1;
    private String recruitmentReason = "NEW_HEADCOUNT"; // REPLACEMENT (Thay thế), NEW_HEADCOUNT (Tăng mới)
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String currency = "VND";
    private Date deadline; // Ngày cần người
    private String jobDescription;
    private String jobRequirements;
    private String salaryExplanation; // Giải trình nếu dải lương đề xuất nằm ngoài dải chuẩn
    private String status = "DRAFT"; // DRAFT, PENDING_APPROVAL, OPEN, CLOSED, REJECTED
    private String recruiterId;
    private String recruiterName;
    private String hiringManagerId;
    private String hiringManagerName;
    private String createdBy;
    private String createdByName;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public RecruitmentRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public int getHeadcount() { return headcount; }
    public void setHeadcount(int headcount) { this.headcount = headcount; }

    public String getRecruitmentReason() { return recruitmentReason; }
    public void setRecruitmentReason(String recruitmentReason) { this.recruitmentReason = recruitmentReason; }

    public BigDecimal getMinSalary() { return minSalary; }
    public void setMinSalary(BigDecimal minSalary) { this.minSalary = minSalary; }

    public BigDecimal getMaxSalary() { return maxSalary; }
    public void setMaxSalary(BigDecimal maxSalary) { this.maxSalary = maxSalary; }

    public String getCurrency() { return currency != null ? currency : "VND"; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }

    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

    public String getJobRequirements() { return jobRequirements; }
    public void setJobRequirements(String jobRequirements) { this.jobRequirements = jobRequirements; }

    public String getSalaryExplanation() { return salaryExplanation; }
    public void setSalaryExplanation(String salaryExplanation) { this.salaryExplanation = salaryExplanation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRecruiterId() { return recruiterId; }
    public void setRecruiterId(String recruiterId) { this.recruiterId = recruiterId; }

    public String getRecruiterName() { return recruiterName; }
    public void setRecruiterName(String recruiterName) { this.recruiterName = recruiterName; }

    public String getHiringManagerId() { return hiringManagerId; }
    public void setHiringManagerId(String hiringManagerId) { this.hiringManagerId = hiringManagerId; }

    public String getHiringManagerName() { return hiringManagerName; }
    public void setHiringManagerName(String hiringManagerName) { this.hiringManagerName = hiringManagerName; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getRecruitmentReasonLabel() {
        if ("REPLACEMENT".equalsIgnoreCase(recruitmentReason)) {
            return "Thay thế";
        }
        return "Tăng mới";
    }

    public String getFormattedSalaryRange() {
        if (minSalary == null && maxSalary == null) {
            return "Thỏa thuận";
        }
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        String min = minSalary != null ? nf.format(minSalary) : "0";
        String max = maxSalary != null ? nf.format(maxSalary) : "Thỏa thuận";
        return min + " - " + max + " " + getCurrency();
    }

    public String getStatusBadgeClass() {
        if ("DRAFT".equalsIgnoreCase(status)) return "bg-secondary text-white";
        if ("PENDING_APPROVAL".equalsIgnoreCase(status)) return "bg-warning text-dark";
        if ("OPEN".equalsIgnoreCase(status)) return "bg-success text-white";
        if ("CLOSED".equalsIgnoreCase(status)) return "bg-dark text-white";
        if ("REJECTED".equalsIgnoreCase(status)) return "bg-danger text-white";
        return "bg-secondary";
    }

    public String getStatusLabel() {
        if ("DRAFT".equalsIgnoreCase(status)) return "Bản nháp";
        if ("PENDING_APPROVAL".equalsIgnoreCase(status)) return "Chờ phê duyệt";
        if ("OPEN".equalsIgnoreCase(status)) return "Đang tuyển";
        if ("CLOSED".equalsIgnoreCase(status)) return "Đã đóng";
        if ("REJECTED".equalsIgnoreCase(status)) return "Từ chối";
        return status != null ? status : "Không xác định";
    }
}

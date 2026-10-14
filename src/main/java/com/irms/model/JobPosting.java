package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * Đại diện thực thể Tin tuyển dụng / Bài đăng (bảng job_postings)
 * Phục vụ nghiệp vụ Quản lý Xuất bản bài đăng (KN-120) và Gỡ tin tự động
 */
public class JobPosting implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String requisitionId;
    private String title;
    private String platform = "CAREER_PORTAL"; // CAREER_PORTAL, INTERNAL, LINKEDIN, TOPCV
    private String status = "DRAFT"; // DRAFT (Chưa xuất bản), PUBLISHED (Đang xuất bản), UNPUBLISHED (Đã gỡ), CLOSED (Đã đóng)
    private int currentVersion = 0; // Phiên bản bản xuất (1, 2, 3...)
    private Timestamp publishedAt; // Thời điểm xuất bản gần nhất
    private String publishedBy; // User ID người xuất bản
    private String publishedByName;
    private Timestamp unpublishedAt; // Thời điểm gỡ bài
    private String unpublishedBy;
    private String publicUrl;
    private String postedUrl;
    private int viewsCount = 0;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Thông tin bổ trợ từ Yêu cầu tuyển dụng (RecruitmentRequest)
    private RecruitmentRequest requisition;
    private String requisitionCode;
    private String requisitionTitle;
    private String positionTitle;
    private String departmentName;
    private String requisitionStatus; // DRAFT, PENDING_APPROVAL, APPROVED, OPEN, CLOSED, REJECTED

    // Các trường phục vụ chức năng Tạo nhanh bài đăng (KN-204, KN-205, KN-206)
    private String jobDescription;
    private String workLocationId;
    private String workLocation;
    private String workModeId;
    private String workMode;
    private java.sql.Date applicationDeadline;
    private boolean salaryVisible = true;
    private java.math.BigDecimal minSalary;
    private java.math.BigDecimal maxSalary;
    private String currency = "VND";

    public JobPosting() {}

    public JobPosting(String id, String requisitionId, String title, String platform, String status) {
        this.id = id;
        this.requisitionId = requisitionId;
        this.title = title;
        this.platform = platform;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequisitionId() { return requisitionId; }
    public void setRequisitionId(String requisitionId) { this.requisitionId = requisitionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPlatform() { return platform != null ? platform : "CAREER_PORTAL"; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getStatus() { return status != null ? status : "DRAFT"; }
    public void setStatus(String status) { this.status = status; }

    public int getCurrentVersion() { return currentVersion; }
    public void setCurrentVersion(int currentVersion) { this.currentVersion = currentVersion; }

    public Timestamp getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Timestamp publishedAt) { this.publishedAt = publishedAt; }

    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String publishedBy) { this.publishedBy = publishedBy; }

    public String getPublishedByName() { return publishedByName; }
    public void setPublishedByName(String publishedByName) { this.publishedByName = publishedByName; }

    public Timestamp getUnpublishedAt() { return unpublishedAt; }
    public void setUnpublishedAt(Timestamp unpublishedAt) { this.unpublishedAt = unpublishedAt; }

    public String getUnpublishedBy() { return unpublishedBy; }
    public void setUnpublishedBy(String unpublishedBy) { this.unpublishedBy = unpublishedBy; }

    public String getPublicUrl() {
        return publicUrl != null ? publicUrl : postedUrl;
    }
    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
        if (this.postedUrl == null) this.postedUrl = publicUrl;
    }

    public String getPostedUrl() {
        return postedUrl != null ? postedUrl : publicUrl;
    }
    public void setPostedUrl(String postedUrl) {
        this.postedUrl = postedUrl;
        if (this.publicUrl == null) this.publicUrl = postedUrl;
    }

    public int getViewsCount() { return viewsCount; }
    public void setViewsCount(int viewsCount) { this.viewsCount = viewsCount; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public RecruitmentRequest getRequisition() { return requisition; }
    public void setRequisition(RecruitmentRequest requisition) {
        this.requisition = requisition;
        if (requisition != null) {
            this.requisitionCode = requisition.getCode();
            this.requisitionTitle = requisition.getTitle();
            this.positionTitle = requisition.getPositionTitle();
            this.departmentName = requisition.getDepartmentName();
            this.requisitionStatus = requisition.getStatus();
        }
    }

    public String getRequisitionCode() { return requisitionCode; }
    public void setRequisitionCode(String requisitionCode) { this.requisitionCode = requisitionCode; }

    public String getRequisitionTitle() { return requisitionTitle; }
    public void setRequisitionTitle(String requisitionTitle) { this.requisitionTitle = requisitionTitle; }

    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getRequisitionStatus() { return requisitionStatus; }
    public void setRequisitionStatus(String requisitionStatus) { this.requisitionStatus = requisitionStatus; }

    /**
     * Tiêu chí KN-120 (1): Tin phải duyệt trước khi show công khai
     */
    public boolean isApproved() {
        if (requisitionStatus == null) return false;
        String s = requisitionStatus.trim().toUpperCase();
        return "APPROVED".equals(s) || "OPEN".equals(s);
    }

    public boolean isPublished() {
        return "PUBLISHED".equalsIgnoreCase(status);
    }

    public String getFormattedPublishedAt() {
        if (publishedAt == null) return "--";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(publishedAt);
    }

    public String getFormattedUnpublishedAt() {
        if (unpublishedAt == null) return "--";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(unpublishedAt);
    }

    public String getStatusLabel() {
        if ("INTERNAL".equalsIgnoreCase(platform)) {
            if ("PUBLISHED".equalsIgnoreCase(status)) return "Đang đăng";
            if ("UNPUBLISHED".equalsIgnoreCase(status)) return "Đã gỡ";
            if ("CLOSED".equalsIgnoreCase(status)) return "Đã đóng";
            if ("DRAFT".equalsIgnoreCase(status)) return "Bản nháp";
            return status != null ? status : "Không xác định";
        }
        if ("PUBLISHED".equalsIgnoreCase(status)) return "Đang xuất bản";
        if ("UNPUBLISHED".equalsIgnoreCase(status)) return "Đã gỡ bài";
        if ("CLOSED".equalsIgnoreCase(status)) return "Đã đóng";
        if ("DRAFT".equalsIgnoreCase(status)) return "Chưa xuất bản";
        return status != null ? status : "Chưa xuất bản";
    }

    public String getStatusBadgeClass() {
        if ("PUBLISHED".equalsIgnoreCase(status)) return "bg-success text-white";
        if ("INTERNAL".equalsIgnoreCase(platform)) {
            if ("UNPUBLISHED".equalsIgnoreCase(status)) return "bg-secondary text-white";
            if ("CLOSED".equalsIgnoreCase(status)) return "bg-dark text-white";
            return "bg-secondary";
        }
        if ("UNPUBLISHED".equalsIgnoreCase(status)) return "bg-warning-subtle text-warning-emphasis border border-warning";
        if ("CLOSED".equalsIgnoreCase(status)) return "bg-dark text-white";
        return "bg-secondary-subtle text-secondary border border-secondary";
    }

    public String getPlatformLabel() {
        if ("CAREER_PORTAL".equalsIgnoreCase(platform)) return "Cổng tuyển dụng công khai";
        if ("INTERNAL".equalsIgnoreCase(platform)) return "Bảng tin nội bộ";
        if ("LINKEDIN".equalsIgnoreCase(platform)) return "LinkedIn";
        if ("TOPCV".equalsIgnoreCase(platform)) return "TopCV";
        return platform != null ? platform : "";
    }

    // Getters & Setters cho các trường Tạo nhanh bài đăng (KN-204, KN-205, KN-206)
    public String getJobDescription() {
        if (jobDescription != null && !jobDescription.trim().isEmpty()) {
            return jobDescription;
        }
        return (requisition != null) ? requisition.getJobDescription() : null;
    }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

    public String getWorkLocationId() { return workLocationId; }
    public void setWorkLocationId(String workLocationId) { this.workLocationId = workLocationId; }

    public String getWorkLocation() { return workLocation; }
    public void setWorkLocation(String workLocation) { this.workLocation = workLocation; }

    public String getWorkModeId() { return workModeId; }
    public void setWorkModeId(String workModeId) { this.workModeId = workModeId; }

    public String getWorkMode() { return workMode; }
    public void setWorkMode(String workMode) { this.workMode = workMode; }

    public java.sql.Date getApplicationDeadline() {
        if (applicationDeadline != null) return applicationDeadline;
        return (requisition != null) ? requisition.getDeadline() : null;
    }
    public void setApplicationDeadline(java.sql.Date applicationDeadline) { this.applicationDeadline = applicationDeadline; }

    public boolean isSalaryVisible() { return salaryVisible; }
    public void setSalaryVisible(boolean salaryVisible) { this.salaryVisible = salaryVisible; }

    public java.math.BigDecimal getMinSalary() { return minSalary; }
    public void setMinSalary(java.math.BigDecimal minSalary) { this.minSalary = minSalary; }

    public java.math.BigDecimal getMaxSalary() { return maxSalary; }
    public void setMaxSalary(java.math.BigDecimal maxSalary) { this.maxSalary = maxSalary; }

    public String getCurrency() { return currency != null ? currency : "VND"; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getFormattedDeadline() {
        java.sql.Date dl = getApplicationDeadline();
        if (dl == null) return "--";
        return new SimpleDateFormat("dd/MM/yyyy").format(dl);
    }

    public String getFormattedSalaryRange() {
        if (!salaryVisible) {
            return "Thỏa thuận";
        }
        String cur = getCurrency();
        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,###", symbols);
        if (minSalary != null && maxSalary != null) {
            return df.format(minSalary) + " - " + df.format(maxSalary) + " " + cur;
        } else if (minSalary != null) {
            return "Từ " + df.format(minSalary) + " " + cur;
        } else if (maxSalary != null) {
            return "Lên đến " + df.format(maxSalary) + " " + cur;
        }
        return "Thỏa thuận";
    }
}

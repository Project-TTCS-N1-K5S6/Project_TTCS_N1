package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * Đại diện thực thể Tin tuyển dụng / Bài đăng (bảng job_postings)
 * Phục vụ nghiệp vụ Quản lý Xuất bản bài đăng (KN-120)
 */
public class JobPosting implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String requisitionId;
    private String title;
    private String platform = "CAREER_PORTAL"; // CAREER_PORTAL, INTERNAL, LINKEDIN, TOPCV
    private String status = "DRAFT"; // DRAFT (Chưa xuất bản), PUBLISHED (Đang xuất bản), UNPUBLISHED (Đã gỡ)
    private int currentVersion = 0; // Phiên bản bản xuất (1, 2, 3...)
    private Timestamp publishedAt; // Thời điểm xuất bản gần nhất
    private String publishedBy; // User ID người xuất bản
    private String publishedByName;
    private Timestamp unpublishedAt; // Thời điểm gỡ bài
    private String unpublishedBy;
    private String publicUrl;
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

    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }

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
        if ("PUBLISHED".equalsIgnoreCase(status)) return "Đang xuất bản";
        if ("UNPUBLISHED".equalsIgnoreCase(status)) return "Đã gỡ bài";
        if ("DRAFT".equalsIgnoreCase(status)) return "Chưa xuất bản";
        return status != null ? status : "Chưa xuất bản";
    }

    public String getStatusBadgeClass() {
        if ("PUBLISHED".equalsIgnoreCase(status)) return "bg-success text-white";
        if ("UNPUBLISHED".equalsIgnoreCase(status)) return "bg-warning-subtle text-warning-emphasis border border-warning";
        return "bg-secondary-subtle text-secondary border border-secondary";
    }

    public String getPlatformLabel() {
        if ("CAREER_PORTAL".equalsIgnoreCase(platform)) return "Cổng tuyển dụng công khai";
        if ("INTERNAL".equalsIgnoreCase(platform)) return "Bảng tin nội bộ";
        if ("LINKEDIN".equalsIgnoreCase(platform)) return "LinkedIn";
        if ("TOPCV".equalsIgnoreCase(platform)) return "TopCV";
        return platform;
    }
}

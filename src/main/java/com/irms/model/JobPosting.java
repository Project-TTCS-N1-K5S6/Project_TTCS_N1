package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Đại diện thực thể Tin tuyển dụng (bảng job_postings)
 */
public class JobPosting implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String requisitionId;
    private String requisitionTitle;
    private String title;
    private String platform = "INTERNAL"; // INTERNAL (Nội bộ), CAREER_SITE, LINKEDIN, TOPCV
    private String status = "PUBLISHED"; // PUBLISHED (Đang đăng), UNPUBLISHED (Đã gỡ), CLOSED (Đã đóng), DRAFT
    private String postedUrl;
    private Timestamp publishedAt;
    private Timestamp unpublishedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

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

    public String getRequisitionTitle() { return requisitionTitle; }
    public void setRequisitionTitle(String requisitionTitle) { this.requisitionTitle = requisitionTitle; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPostedUrl() { return postedUrl; }
    public void setPostedUrl(String postedUrl) { this.postedUrl = postedUrl; }

    public Timestamp getPublishedAt() { return publishedAt; }
    public void setPublishedAt(Timestamp publishedAt) { this.publishedAt = publishedAt; }

    public Timestamp getUnpublishedAt() { return unpublishedAt; }
    public void setUnpublishedAt(Timestamp unpublishedAt) { this.unpublishedAt = unpublishedAt; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getStatusLabel() {
        if ("PUBLISHED".equalsIgnoreCase(status)) return "Đang đăng";
        if ("UNPUBLISHED".equalsIgnoreCase(status)) return "Đã gỡ";
        if ("CLOSED".equalsIgnoreCase(status)) return "Đã đóng";
        if ("DRAFT".equalsIgnoreCase(status)) return "Bản nháp";
        return status != null ? status : "Không xác định";
    }

    public String getStatusBadgeClass() {
        if ("PUBLISHED".equalsIgnoreCase(status)) return "bg-success text-white";
        if ("UNPUBLISHED".equalsIgnoreCase(status)) return "bg-secondary text-white";
        if ("CLOSED".equalsIgnoreCase(status)) return "bg-dark text-white";
        return "bg-secondary";
    }
}

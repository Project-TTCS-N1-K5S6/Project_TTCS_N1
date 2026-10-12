package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * Đại diện cho từng Bản xuất và thời điểm xuất bản (bảng job_post_publications)
 * Phục vụ nghiệp vụ KN-120: Ghi lại bản xuất và thời điểm
 */
public class JobPostPublication implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String requisitionId;
    private String jobPostingId;
    private int versionNumber = 1; // 1, 2, 3...
    private String versionLabel; // "Bản xuất #1", "Bản xuất #2"...
    private String channel = "CAREER_PORTAL"; // Cổng tuyển dụng công khai, Nội bộ, v.v.
    private String status = "PUBLISHED"; // PUBLISHED, ARCHIVED, UNPUBLISHED
    private Timestamp publishedAt; // Thời điểm xuất bản
    private String publishedBy; // User ID người thực hiện xuất bản
    private String publishedByName; // Họ tên người xuất bản
    private Timestamp unpublishedAt; // Thời điểm gỡ nếu có
    private String unpublishedBy;
    private String snapshotTitle; // Tiêu đề tại thời điểm xuất
    private String snapshotData; // Toàn bộ dữ liệu JSON lưu snapshot nội dung
    private String changeSummary; // Ghi chú thay đổi / lý do xuất bản
    private Timestamp createdAt;

    public JobPostPublication() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequisitionId() { return requisitionId; }
    public void setRequisitionId(String requisitionId) { this.requisitionId = requisitionId; }

    public String getJobPostingId() { return jobPostingId; }
    public void setJobPostingId(String jobPostingId) { this.jobPostingId = jobPostingId; }

    public int getVersionNumber() { return versionNumber; }
    public void setVersionNumber(int versionNumber) { this.versionNumber = versionNumber; }

    public String getVersionLabel() {
        if (versionLabel != null && !versionLabel.trim().isEmpty()) {
            return versionLabel;
        }
        return "Bản xuất #" + versionNumber;
    }
    public void setVersionLabel(String versionLabel) { this.versionLabel = versionLabel; }

    public String getChannel() { return channel != null ? channel : "CAREER_PORTAL"; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getStatus() { return status != null ? status : "PUBLISHED"; }
    public void setStatus(String status) { this.status = status; }

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

    public String getSnapshotTitle() { return snapshotTitle; }
    public void setSnapshotTitle(String snapshotTitle) { this.snapshotTitle = snapshotTitle; }

    public String getSnapshotData() { return snapshotData; }
    public void setSnapshotData(String snapshotData) { this.snapshotData = snapshotData; }

    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getFormattedPublishedAt() {
        if (publishedAt == null) return "--";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(publishedAt);
    }

    public String getFormattedUnpublishedAt() {
        if (unpublishedAt == null) return "--";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(unpublishedAt);
    }

    public String getStatusBadgeClass() {
        if ("PUBLISHED".equalsIgnoreCase(status)) return "bg-success text-white";
        if ("UNPUBLISHED".equalsIgnoreCase(status)) return "bg-secondary text-white";
        if ("ARCHIVED".equalsIgnoreCase(status)) return "bg-info-subtle text-info-emphasis border border-info";
        return "bg-secondary";
    }

    public String getStatusLabel() {
        if ("PUBLISHED".equalsIgnoreCase(status)) return "Đang áp dụng";
        if ("UNPUBLISHED".equalsIgnoreCase(status)) return "Đã gỡ";
        if ("ARCHIVED".equalsIgnoreCase(status)) return "Bản lưu trữ";
        return status != null ? status : "Không xác định";
    }

    public String getChannelLabel() {
        if ("CAREER_PORTAL".equalsIgnoreCase(channel)) return "Cổng tuyển dụng công khai";
        if ("INTERNAL".equalsIgnoreCase(channel)) return "Bảng tin nội bộ";
        if ("LINKEDIN".equalsIgnoreCase(channel)) return "LinkedIn";
        if ("TOPCV".equalsIgnoreCase(channel)) return "TopCV";
        return channel;
    }
}

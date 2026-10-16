package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Thực thể Hoạt động & Lịch sử bảo toàn ứng viên (bảng candidate_activities)
 */
public class CandidateActivity implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String candidateId;
    private String activityType; // 'APPLICATION', 'INTERVIEW', 'ASSESSMENT', 'NOTE', 'STATUS_CHANGE', 'MERGE', 'WARNING'
    private String title;
    private String description;
    private String performedBy;
    private String performedByName;
    private String originalCandidateId;
    private String metadataJson;
    private Timestamp activityDate;

    public CandidateActivity() {}

    public CandidateActivity(String id, String candidateId, String activityType, String title, String description,
                             String performedBy, String performedByName) {
        this.id = id;
        this.candidateId = candidateId;
        this.activityType = activityType;
        this.title = title;
        this.description = description;
        this.performedBy = performedBy;
        this.performedByName = performedByName;
        this.activityDate = new Timestamp(System.currentTimeMillis());
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }

    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }

    public String getPerformedByName() { return performedByName; }
    public void setPerformedByName(String performedByName) { this.performedByName = performedByName; }

    public String getOriginalCandidateId() { return originalCandidateId; }
    public void setOriginalCandidateId(String originalCandidateId) { this.originalCandidateId = originalCandidateId; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

    public Timestamp getActivityDate() { return activityDate; }
    public void setActivityDate(Timestamp activityDate) { this.activityDate = activityDate; }

    public String getTypeBadgeClass() {
        if ("APPLICATION".equalsIgnoreCase(activityType)) return "bg-primary text-white";
        if ("INTERVIEW".equalsIgnoreCase(activityType)) return "bg-warning text-dark";
        if ("ASSESSMENT".equalsIgnoreCase(activityType)) return "bg-info text-white";
        if ("NOTE".equalsIgnoreCase(activityType)) return "bg-secondary text-white";
        if ("STATUS_CHANGE".equalsIgnoreCase(activityType)) return "bg-primary-subtle text-primary";
        if ("MERGE".equalsIgnoreCase(activityType)) return "bg-purple text-white";
        if ("WARNING".equalsIgnoreCase(activityType)) return "bg-danger text-white";
        return "bg-secondary text-white";
    }

    public String getTypeIcon() {
        if ("APPLICATION".equalsIgnoreCase(activityType)) return "bi-file-earmark-plus";
        if ("INTERVIEW".equalsIgnoreCase(activityType)) return "bi-calendar-check";
        if ("ASSESSMENT".equalsIgnoreCase(activityType)) return "bi-clipboard-data";
        if ("NOTE".equalsIgnoreCase(activityType)) return "bi-chat-left-text";
        if ("STATUS_CHANGE".equalsIgnoreCase(activityType)) return "bi-arrow-left-right";
        if ("MERGE".equalsIgnoreCase(activityType)) return "bi-intersect";
        if ("WARNING".equalsIgnoreCase(activityType)) return "bi-exclamation-triangle";
        return "bi-circle";
    }

    public String getTypeLabel() {
        if ("APPLICATION".equalsIgnoreCase(activityType)) return "Ứng tuyển";
        if ("INTERVIEW".equalsIgnoreCase(activityType)) return "Phỏng vấn";
        if ("ASSESSMENT".equalsIgnoreCase(activityType)) return "Đánh giá";
        if ("NOTE".equalsIgnoreCase(activityType)) return "Ghi chú";
        if ("STATUS_CHANGE".equalsIgnoreCase(activityType)) return "Chuyển trạng thái";
        if ("MERGE".equalsIgnoreCase(activityType)) return "Hợp nhất hồ sơ";
        if ("WARNING".equalsIgnoreCase(activityType)) return "Cảnh báo trùng";
        return activityType;
    }
}

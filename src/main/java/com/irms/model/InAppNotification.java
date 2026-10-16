package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Đại diện thực thể Thông báo hệ thống cho người dùng (KN-365)
 */
public class InAppNotification implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String userId;
    private String title;
    private String content;
    private String type; // SLA_OVERDUE, SYSTEM, REMINDER
    private String referenceId;
    private boolean read;
    private Timestamp createdAt;

    public InAppNotification() {}

    public InAppNotification(String id, String userId, String title, String content, String type, String referenceId) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.referenceId = referenceId;
        this.read = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Đại diện thực thể Nhật ký kiểm toán bảo mật (bảng audit_logs)
 */
public class AuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String userId;
    private String userEmail;
    private String userFullName;
    private String action;
    private String entityType;
    private String entityId;
    private String description;
    private String ipAddress;
    private String userAgent;
    private Timestamp createdAt;

    public AuditLog() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getActionBadgeClass() {
        if (action == null) return "bg-secondary";
        if (action.contains("LOGIN_SUCCESS")) return "bg-success";
        if (action.contains("LOGIN_FAILED") || action.contains("LOCK")) return "bg-danger";
        if (action.contains("UPDATE") || action.contains("RESET")) return "bg-warning text-dark";
        if (action.contains("CREATE")) return "bg-primary";
        return "bg-info text-dark";
    }
}

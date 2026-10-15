package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Thực thể Đại diện Subtask công việc của phân hệ Định biên & Ngân sách
 * Bảng: budget_subtasks (KN-195 -> KN-201)
 */
public class BudgetSubtask implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String code;
    private String title;
    private String priority = "MEDIUM"; // CRITICAL, HIGH, MEDIUM, LOW
    private String assigneeId;
    private String assigneeName;
    private String status = "TODO"; // TODO, IN_PROGRESS, DONE
    private String moduleType = "BE"; // BE, FE
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public BudgetSubtask() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }

    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getModuleType() { return moduleType; }
    public void setModuleType(String moduleType) { this.moduleType = moduleType; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // Helpers giao diện
    public String getPriorityBadgeClass() {
        if ("CRITICAL".equalsIgnoreCase(priority)) return "bg-danger text-white";
        if ("HIGH".equalsIgnoreCase(priority)) return "bg-danger-subtle text-danger border border-danger-subtle";
        if ("MEDIUM".equalsIgnoreCase(priority)) return "bg-warning-subtle text-warning-emphasis border border-warning-subtle";
        return "bg-secondary-subtle text-secondary border border-secondary-subtle";
    }

    public String getPriorityLabel() {
        if ("CRITICAL".equalsIgnoreCase(priority)) return "Khẩn cấp";
        if ("HIGH".equalsIgnoreCase(priority)) return "Cao";
        if ("MEDIUM".equalsIgnoreCase(priority)) return "Trung bình";
        return "Thấp";
    }

    public String getStatusBadgeClass() {
        if ("DONE".equalsIgnoreCase(status)) return "bg-success text-white";
        if ("IN_PROGRESS".equalsIgnoreCase(status)) return "bg-primary text-white";
        return "bg-secondary text-white";
    }

    public String getStatusLabel() {
        if ("DONE".equalsIgnoreCase(status)) return "Done";
        if ("IN_PROGRESS".equalsIgnoreCase(status)) return "In Progress";
        return "To Do";
    }
}

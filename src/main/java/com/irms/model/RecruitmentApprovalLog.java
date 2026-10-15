package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * Đại diện thực thể Nhật ký & Chuỗi cấp duyệt yêu cầu tuyển dụng (bảng recruitment_approval_logs)
 * Đáp ứng các yêu cầu:
 * - KN-156: Mô hình lưu nhật ký phê duyệt
 * - KN-155: Khóa lịch sử phê duyệt không cho sửa hoặc xóa (Immutability Lock)
 */
public class RecruitmentApprovalLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String requisitionId;
    private int stepOrder; // 1: Cấp 1, 2: Cấp 2, 3: Cấp 3
    private String stepName; // "Cấp 1: Trưởng bộ phận chuyên môn", "Cấp 2: Trưởng phòng Nhân sự", "Cấp 3: Ban Giám đốc"
    private String requiredRole; // 'HIRING_MANAGER', 'HR_MANAGER', 'APPROVER'
    private String requiredRoleName; // Tên hiển thị của vai trò
    private String assignedUserId;
    private String assignedUserName;
    private String approverId;
    private String approverName;
    private String approverJobTitle;
    private String approverEmail;
    private String status = "WAITING"; // WAITING, PENDING, APPROVED, REJECTED, SKIPPED
    private Timestamp decisionDate;
    private String comment;
    private Timestamp createdAt;

    public RecruitmentApprovalLog() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequisitionId() { return requisitionId; }
    public void setRequisitionId(String requisitionId) { this.requisitionId = requisitionId; }

    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public String getRequiredRole() { return requiredRole; }
    public void setRequiredRole(String requiredRole) { this.requiredRole = requiredRole; }

    public String getRequiredRoleName() {
        if (requiredRoleName != null && !requiredRoleName.isEmpty()) return requiredRoleName;
        if ("HIRING_MANAGER".equalsIgnoreCase(requiredRole)) return "Trưởng bộ phận chuyên môn";
        if ("HR_MANAGER".equalsIgnoreCase(requiredRole)) return "Trưởng phòng Nhân sự";
        if ("APPROVER".equalsIgnoreCase(requiredRole)) return "Ban Giám đốc / Lãnh đạo";
        if ("ADMIN".equalsIgnoreCase(requiredRole)) return "Quản trị hệ thống";
        return requiredRole;
    }
    public void setRequiredRoleName(String requiredRoleName) { this.requiredRoleName = requiredRoleName; }

    public String getAssignedUserId() { return assignedUserId; }
    public void setAssignedUserId(String assignedUserId) { this.assignedUserId = assignedUserId; }

    public String getAssignedUserName() { return assignedUserName; }
    public void setAssignedUserName(String assignedUserName) { this.assignedUserName = assignedUserName; }

    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    public String getApproverJobTitle() { return approverJobTitle; }
    public void setApproverJobTitle(String approverJobTitle) { this.approverJobTitle = approverJobTitle; }

    public String getApproverEmail() { return approverEmail; }
    public void setApproverEmail(String approverEmail) { this.approverEmail = approverEmail; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getDecisionDate() { return decisionDate; }
    public void setDecisionDate(Timestamp decisionDate) { this.decisionDate = decisionDate; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    // Helper status methods
    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(status);
    }

    public boolean isRejected() {
        return "REJECTED".equalsIgnoreCase(status);
    }

    public boolean isChangesRequested() {
        return "CHANGES_REQUESTED".equalsIgnoreCase(status);
    }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }

    public boolean isWaiting() {
        return "WAITING".equalsIgnoreCase(status);
    }

    public boolean isSkipped() {
        return "SKIPPED".equalsIgnoreCase(status);
    }

    /**
     * Bất biến: Khi đã có quyết định (APPROVED / REJECTED / CHANGES_REQUESTED), bản ghi sẽ bị khóa, không được sửa/xóa
     */
    public boolean isLocked() {
        return isApproved() || isRejected() || isChangesRequested();
    }

    public String getStatusLabel() {
        if ("APPROVED".equalsIgnoreCase(status)) return "Đã phê duyệt";
        if ("PENDING".equalsIgnoreCase(status)) return "Đang chờ duyệt";
        if ("CHANGES_REQUESTED".equalsIgnoreCase(status)) return "Yêu cầu bổ sung";
        if ("WAITING".equalsIgnoreCase(status)) return "Chờ cấp trước";
        if ("REJECTED".equalsIgnoreCase(status)) return "Từ chối";
        if ("SKIPPED".equalsIgnoreCase(status)) return "Bỏ qua";
        return status != null ? status : "Chưa xác định";
    }

    public String getStatusBadgeClass() {
        if ("APPROVED".equalsIgnoreCase(status)) return "bg-success text-white";
        if ("PENDING".equalsIgnoreCase(status)) return "bg-warning text-dark";
        if ("CHANGES_REQUESTED".equalsIgnoreCase(status)) return "bg-warning-subtle text-warning-emphasis border border-warning";
        if ("WAITING".equalsIgnoreCase(status)) return "bg-light text-secondary border";
        if ("REJECTED".equalsIgnoreCase(status)) return "bg-danger text-white";
        if ("SKIPPED".equalsIgnoreCase(status)) return "bg-secondary text-white";
        return "bg-secondary";
    }

    public String getStatusIcon() {
        if ("APPROVED".equalsIgnoreCase(status)) return "bi-check-circle-fill text-success";
        if ("PENDING".equalsIgnoreCase(status)) return "bi-hourglass-split text-warning";
        if ("CHANGES_REQUESTED".equalsIgnoreCase(status)) return "bi-arrow-counterclockwise text-warning";
        if ("WAITING".equalsIgnoreCase(status)) return "bi-clock text-muted";
        if ("REJECTED".equalsIgnoreCase(status)) return "bi-x-circle-fill text-danger";
        if ("SKIPPED".equalsIgnoreCase(status)) return "bi-dash-circle text-muted";
        return "bi-circle text-muted";
    }

    public String getFormattedDecisionDate() {
        if (decisionDate == null) return "--";
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
        return sdf.format(decisionDate);
    }
}

package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Đại diện từng bước phê duyệt được đóng băng (snapshot) gắn liền với từng Yêu cầu tuyển dụng
 * Đảm bảo khi sửa ma trận phê duyệt, các luồng đang chạy không bị ảnh hưởng.
 */
public class RequisitionApprovalStep implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String requisitionId;
    private String matrixId;
    private int stepOrder;
    private String stepName;
    private String approverType; // DEPT_MANAGER, ROLE, SPECIFIC_USER
    private String approverRoleId;
    private String approverUserId;
    private String assignedApproverName;
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED
    private String comment;
    private String actedBy;
    private String actedByName;
    private Timestamp actedAt;
    private Timestamp createdAt;

    public RequisitionApprovalStep() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequisitionId() { return requisitionId; }
    public void setRequisitionId(String requisitionId) { this.requisitionId = requisitionId; }

    public String getMatrixId() { return matrixId; }
    public void setMatrixId(String matrixId) { this.matrixId = matrixId; }

    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public String getApproverType() { return approverType; }
    public void setApproverType(String approverType) { this.approverType = approverType; }

    public String getApproverRoleId() { return approverRoleId; }
    public void setApproverRoleId(String approverRoleId) { this.approverRoleId = approverRoleId; }

    public String getApproverUserId() { return approverUserId; }
    public void setApproverUserId(String approverUserId) { this.approverUserId = approverUserId; }

    public String getAssignedApproverName() { return assignedApproverName; }
    public void setAssignedApproverName(String assignedApproverName) { this.assignedApproverName = assignedApproverName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getActedBy() { return actedBy; }
    public void setActedBy(String actedBy) { this.actedBy = actedBy; }

    public String getActedByName() { return actedByName; }
    public void setActedByName(String actedByName) { this.actedByName = actedByName; }

    public Timestamp getActedAt() { return actedAt; }
    public void setActedAt(Timestamp actedAt) { this.actedAt = actedAt; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getStatusBadgeClass() {
        if ("APPROVED".equalsIgnoreCase(status)) return "bg-success text-white";
        if ("REJECTED".equalsIgnoreCase(status)) return "bg-danger text-white";
        return "bg-warning text-dark";
    }

    public String getStatusLabel() {
        if ("APPROVED".equalsIgnoreCase(status)) return "Đã phê duyệt";
        if ("REJECTED".equalsIgnoreCase(status)) return "Bị từ chối";
        return "Chờ phê duyệt";
    }
}

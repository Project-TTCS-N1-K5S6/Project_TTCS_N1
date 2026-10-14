package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Đại diện thực thể từng cấp phê duyệt trong Ma trận phê duyệt (approval_matrix_steps)
 */
public class ApprovalMatrixStep implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String matrixId;
    private int stepOrder;
    private String stepName;
    private String approverType; // DEPT_MANAGER, ROLE, SPECIFIC_USER
    private String approverRoleId;
    private String approverRoleName;
    private String approverUserId;
    private String approverUserName;
    private boolean escalationStep;
    private String escalationCondition;
    private Timestamp createdAt;

    public ApprovalMatrixStep() {}

    public ApprovalMatrixStep(int stepOrder, String stepName, String approverType, String approverRoleId, String approverUserId, boolean escalationStep, String escalationCondition) {
        this.stepOrder = stepOrder;
        this.stepName = stepName;
        this.approverType = approverType;
        this.approverRoleId = approverRoleId;
        this.approverUserId = approverUserId;
        this.escalationStep = escalationStep;
        this.escalationCondition = escalationCondition;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public String getApproverRoleName() { return approverRoleName; }
    public void setApproverRoleName(String approverRoleName) { this.approverRoleName = approverRoleName; }

    public String getApproverUserId() { return approverUserId; }
    public void setApproverUserId(String approverUserId) { this.approverUserId = approverUserId; }

    public String getApproverUserName() { return approverUserName; }
    public void setApproverUserName(String approverUserName) { this.approverUserName = approverUserName; }

    public boolean isEscalationStep() { return escalationStep; }
    public void setEscalationStep(boolean escalationStep) { this.escalationStep = escalationStep; }

    public String getEscalationCondition() { return escalationCondition; }
    public void setEscalationCondition(String escalationCondition) { this.escalationCondition = escalationCondition; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getApproverTypeLabel() {
        if ("DEPT_MANAGER".equalsIgnoreCase(approverType)) return "Trưởng phòng ban";
        if ("ROLE".equalsIgnoreCase(approverType)) return "Theo vai trò";
        if ("SPECIFIC_USER".equalsIgnoreCase(approverType)) return "Người chỉ định";
        return approverType;
    }

    public String getApproverDisplayName() {
        if ("DEPT_MANAGER".equalsIgnoreCase(approverType)) {
            return "Trưởng phòng ban của yêu cầu";
        }
        if ("ROLE".equalsIgnoreCase(approverType)) {
            return (approverRoleName != null && !approverRoleName.isEmpty()) ? approverRoleName : "Vai trò hệ thống";
        }
        if ("SPECIFIC_USER".equalsIgnoreCase(approverType)) {
            return (approverUserName != null && !approverUserName.isEmpty()) ? approverUserName : "Người dùng chỉ định";
        }
        return stepName;
    }
}

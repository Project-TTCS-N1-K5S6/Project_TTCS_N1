package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Đại diện cho bản ghi phân công Recruiter hỗ trợ cho một vị trí tuyển dụng
 * (bảng requisition_recruiters)
 */
public class RecruiterAssignment implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String ROLE_PRIMARY = "PRIMARY";
    public static final String ROLE_SUPPORT = "SUPPORT";

    private String id;
    private String requisitionId;
    private String requisitionCode;
    private String recruiterId;
    private String recruiterName;
    private String recruiterEmail;
    private String recruiterJobTitle;
    private String role; // PRIMARY | SUPPORT
    private Timestamp assignedAt;
    private String assignedBy;
    private String assignedByName;

    public RecruiterAssignment() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequisitionId() { return requisitionId; }
    public void setRequisitionId(String requisitionId) { this.requisitionId = requisitionId; }

    public String getRequisitionCode() { return requisitionCode; }
    public void setRequisitionCode(String requisitionCode) { this.requisitionCode = requisitionCode; }

    public String getRecruiterId() { return recruiterId; }
    public void setRecruiterId(String recruiterId) { this.recruiterId = recruiterId; }

    public String getRecruiterName() { return recruiterName; }
    public void setRecruiterName(String recruiterName) { this.recruiterName = recruiterName; }

    public String getRecruiterEmail() { return recruiterEmail; }
    public void setRecruiterEmail(String recruiterEmail) { this.recruiterEmail = recruiterEmail; }

    public String getRecruiterJobTitle() { return recruiterJobTitle; }
    public void setRecruiterJobTitle(String recruiterJobTitle) { this.recruiterJobTitle = recruiterJobTitle; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isPrimary() { return ROLE_PRIMARY.equalsIgnoreCase(role); }
    public boolean isSupport() { return ROLE_SUPPORT.equalsIgnoreCase(role); }

    public String getRoleLabel() {
        if (ROLE_PRIMARY.equalsIgnoreCase(role)) return "Recruiter Chính";
        if (ROLE_SUPPORT.equalsIgnoreCase(role)) return "Recruiter Hỗ trợ";
        return role;
    }

    public Timestamp getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Timestamp assignedAt) { this.assignedAt = assignedAt; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public String getAssignedByName() { return assignedByName; }
    public void setAssignedByName(String assignedByName) { this.assignedByName = assignedByName; }
}

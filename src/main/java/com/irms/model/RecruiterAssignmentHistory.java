package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Đại diện bản ghi lịch sử chuyển giao người phụ trách chính (recruiter chính)
 * của một vị trí tuyển dụng (bảng recruiter_assignment_history)
 */
public class RecruiterAssignmentHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String requisitionId;
    private String requisitionCode;
    private String oldRecruiterId;
    private String oldRecruiterName;
    private String newRecruiterId;
    private String newRecruiterName;
    private String changeReason;
    private String changedBy;
    private String changedByName;
    private Timestamp changedAt;

    public RecruiterAssignmentHistory() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequisitionId() { return requisitionId; }
    public void setRequisitionId(String requisitionId) { this.requisitionId = requisitionId; }

    public String getRequisitionCode() { return requisitionCode; }
    public void setRequisitionCode(String requisitionCode) { this.requisitionCode = requisitionCode; }

    public String getOldRecruiterId() { return oldRecruiterId; }
    public void setOldRecruiterId(String oldRecruiterId) { this.oldRecruiterId = oldRecruiterId; }

    public String getOldRecruiterName() { return oldRecruiterName; }
    public void setOldRecruiterName(String oldRecruiterName) { this.oldRecruiterName = oldRecruiterName; }

    public String getNewRecruiterId() { return newRecruiterId; }
    public void setNewRecruiterId(String newRecruiterId) { this.newRecruiterId = newRecruiterId; }

    public String getNewRecruiterName() { return newRecruiterName; }
    public void setNewRecruiterName(String newRecruiterName) { this.newRecruiterName = newRecruiterName; }

    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }

    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }

    public String getChangedByName() { return changedByName; }
    public void setChangedByName(String changedByName) { this.changedByName = changedByName; }

    public Timestamp getChangedAt() { return changedAt; }
    public void setChangedAt(Timestamp changedAt) { this.changedAt = changedAt; }
}

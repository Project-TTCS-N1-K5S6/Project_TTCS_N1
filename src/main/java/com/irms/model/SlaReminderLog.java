package com.irms.model;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

/**
 * Đại diện thực thể Nhật ký cảnh báo SLA & Lần gửi nhắc việc (KN-367, KN-368)
 */
public class SlaReminderLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String candidateId;
    private String candidateName;
    private String requisitionId;
    private String requisitionTitle;
    private String stageCode;
    private String stageName;
    private int slaDaysThreshold;
    private int daysInStage;
    private String recruiterId;
    private String recruiterName;
    private String recruiterEmail;
    private String channel; // IN_APP, EMAIL, IN_APP_AND_EMAIL
    private String status; // SENT, SKIPPED_DUPLICATE, FAILED
    private String message;
    private Date sentDate;
    private Timestamp sentAt;

    public SlaReminderLog() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getRequisitionId() { return requisitionId; }
    public void setRequisitionId(String requisitionId) { this.requisitionId = requisitionId; }

    public String getRequisitionTitle() { return requisitionTitle; }
    public void setRequisitionTitle(String requisitionTitle) { this.requisitionTitle = requisitionTitle; }

    public String getStageCode() { return stageCode; }
    public void setStageCode(String stageCode) { this.stageCode = stageCode; }

    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }

    public int getSlaDaysThreshold() { return slaDaysThreshold; }
    public void setSlaDaysThreshold(int slaDaysThreshold) { this.slaDaysThreshold = slaDaysThreshold; }

    public int getDaysInStage() { return daysInStage; }
    public void setDaysInStage(int daysInStage) { this.daysInStage = daysInStage; }

    public String getRecruiterId() { return recruiterId; }
    public void setRecruiterId(String recruiterId) { this.recruiterId = recruiterId; }

    public String getRecruiterName() { return recruiterName; }
    public void setRecruiterName(String recruiterName) { this.recruiterName = recruiterName; }

    public String getRecruiterEmail() { return recruiterEmail; }
    public void setRecruiterEmail(String recruiterEmail) { this.recruiterEmail = recruiterEmail; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Date getSentDate() { return sentDate; }
    public void setSentDate(Date sentDate) { this.sentDate = sentDate; }

    public Timestamp getSentAt() { return sentAt; }
    public void setSentAt(Timestamp sentAt) { this.sentAt = sentAt; }

    public int getOverdueDays() {
        return Math.max(0, daysInStage - slaDaysThreshold);
    }

    public String getStatusBadgeClass() {
        if ("SENT".equalsIgnoreCase(status)) return "badge bg-success";
        if ("SKIPPED_DUPLICATE".equalsIgnoreCase(status)) return "badge bg-warning text-dark";
        if ("FAILED".equalsIgnoreCase(status)) return "badge bg-danger";
        return "badge bg-secondary";
    }

    public String getStatusLabel() {
        if ("SENT".equalsIgnoreCase(status)) return "Đã gửi nhắc";
        if ("SKIPPED_DUPLICATE".equalsIgnoreCase(status)) return "Bỏ qua (Đã nhắc trong ngày)";
        if ("FAILED".equalsIgnoreCase(status)) return "Thất bại";
        return status;
    }
}

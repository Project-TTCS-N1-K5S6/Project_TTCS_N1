package com.irms.model;

/**
 * Thông tin hồ sơ cần thiết cho màn hình xác nhận và email báo recruiter.
 */
public class CandidateWithdrawal {
    private String candidateId;
    private String fullName;
    private String email;
    private String trackingCode;
    private String requisitionTitle;
    private String statusBeforeWithdrawal;
    private String recruiterName;
    private String recruiterEmail;

    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }

    public String getRequisitionTitle() { return requisitionTitle; }
    public void setRequisitionTitle(String requisitionTitle) { this.requisitionTitle = requisitionTitle; }

    public String getStatusBeforeWithdrawal() { return statusBeforeWithdrawal; }
    public void setStatusBeforeWithdrawal(String statusBeforeWithdrawal) {
        this.statusBeforeWithdrawal = statusBeforeWithdrawal;
    }

    public String getRecruiterName() { return recruiterName; }
    public void setRecruiterName(String recruiterName) { this.recruiterName = recruiterName; }

    public String getRecruiterEmail() { return recruiterEmail; }
    public void setRecruiterEmail(String recruiterEmail) { this.recruiterEmail = recruiterEmail; }
}

package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Đại diện thực thể Hồ sơ Ứng viên (bảng candidates)
 */
public class Candidate implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String requisitionId;
    private String requisitionTitle;
    private String applicationSourceId;
    private String applicationSourceName;
    private String referredByUserId;
    private String referredByUserName;
    private String rejectionReasonId;
    private String rejectionReasonName;
    private String fullName;
    private String email;
    private String phone;
    private String status; // APPLIED, SCREENING, INTERVIEWING, OFFER, HIRED, REJECTED
    private String trackingCode;
    private String cvUrl;
    private String notes;
    private String coverLetter;
    private String ipAddress;
    private String departmentId;
    private String departmentName;
    private boolean isPotential;
    private int potentialRating = 3;
    private String potentialSkills;
    private String potentialNotes;
    private String targetDepartmentId;
    private String targetDepartmentName;
    private String targetPosition;
    private Timestamp addedToPoolAt;
    private String addedToPoolBy;
    private String addedToPoolByName;
    private int matchScore;
    private String matchReason;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Candidate() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequisitionId() { return requisitionId; }
    public void setRequisitionId(String requisitionId) { this.requisitionId = requisitionId; }

    public String getRequisitionTitle() { return requisitionTitle; }
    public void setRequisitionTitle(String requisitionTitle) { this.requisitionTitle = requisitionTitle; }

    public String getApplicationSourceId() { return applicationSourceId; }
    public void setApplicationSourceId(String applicationSourceId) { this.applicationSourceId = applicationSourceId; }

    public String getApplicationSourceName() { return applicationSourceName; }
    public void setApplicationSourceName(String applicationSourceName) { this.applicationSourceName = applicationSourceName; }

    public String getReferredByUserId() { return referredByUserId; }
    public void setReferredByUserId(String referredByUserId) { this.referredByUserId = referredByUserId; }

    public String getReferredByUserName() { return referredByUserName; }
    public void setReferredByUserName(String referredByUserName) { this.referredByUserName = referredByUserName; }

    public String getRejectionReasonId() { return rejectionReasonId; }
    public void setRejectionReasonId(String rejectionReasonId) { this.rejectionReasonId = rejectionReasonId; }

    public String getRejectionReasonName() { return rejectionReasonName; }
    public void setRejectionReasonName(String rejectionReasonName) { this.rejectionReasonName = rejectionReasonName; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }

    public String getCvUrl() { return cvUrl; }
    public void setCvUrl(String cvUrl) { this.cvUrl = cvUrl; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCoverLetter() { return coverLetter; }
    public void setCoverLetter(String coverLetter) { this.coverLetter = coverLetter; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public boolean isPotential() { return isPotential; }
    public void setPotential(boolean potential) { isPotential = potential; }

    public int getPotentialRating() { return potentialRating; }
    public void setPotentialRating(int potentialRating) { this.potentialRating = potentialRating; }

    public String getPotentialSkills() { return potentialSkills; }
    public void setPotentialSkills(String potentialSkills) { this.potentialSkills = potentialSkills; }

    public String getPotentialNotes() { return potentialNotes; }
    public void setPotentialNotes(String potentialNotes) { this.potentialNotes = potentialNotes; }

    public String getTargetDepartmentId() { return targetDepartmentId; }
    public void setTargetDepartmentId(String targetDepartmentId) { this.targetDepartmentId = targetDepartmentId; }

    public String getTargetDepartmentName() { return targetDepartmentName; }
    public void setTargetDepartmentName(String targetDepartmentName) { this.targetDepartmentName = targetDepartmentName; }

    public String getTargetPosition() { return targetPosition; }
    public void setTargetPosition(String targetPosition) { this.targetPosition = targetPosition; }

    public Timestamp getAddedToPoolAt() { return addedToPoolAt; }
    public void setAddedToPoolAt(Timestamp addedToPoolAt) { this.addedToPoolAt = addedToPoolAt; }

    public String getAddedToPoolBy() { return addedToPoolBy; }
    public void setAddedToPoolBy(String addedToPoolBy) { this.addedToPoolBy = addedToPoolBy; }

    public String getAddedToPoolByName() { return addedToPoolByName; }
    public void setAddedToPoolByName(String addedToPoolByName) { this.addedToPoolByName = addedToPoolByName; }

    public int getMatchScore() { return matchScore; }
    public void setMatchScore(int matchScore) { this.matchScore = matchScore; }

    public String getMatchReason() { return matchReason; }
    public void setMatchReason(String matchReason) { this.matchReason = matchReason; }

    public String[] getSkillsArray() {
        if (potentialSkills == null || potentialSkills.trim().isEmpty()) {
            return new String[0];
        }
        return potentialSkills.split("[,;\\n]+");
    }

    public String getPotentialRatingBadgeClass() {
        if (potentialRating >= 5) return "bg-warning text-dark";
        if (potentialRating == 4) return "bg-success text-white";
        if (potentialRating == 3) return "bg-primary text-white";
        if (potentialRating == 2) return "bg-info text-dark";
        return "bg-secondary text-white";
    }

    public String getStatusBadgeClass() {
        if ("APPLIED".equalsIgnoreCase(status)) return "bg-primary";
        if ("SCREENING".equalsIgnoreCase(status)) return "bg-info";
        if ("INTERVIEWING".equalsIgnoreCase(status)) return "bg-warning text-dark";
        if ("OFFER".equalsIgnoreCase(status)) return "bg-purple text-white";
        if ("HIRED".equalsIgnoreCase(status)) return "bg-success";
        if ("REJECTED".equalsIgnoreCase(status)) return "bg-danger";
        return "bg-secondary";
    }

    public String getStatusLabel() {
        if ("APPLIED".equalsIgnoreCase(status)) return "Mới ứng tuyển";
        if ("SCREENING".equalsIgnoreCase(status)) return "Sàng lọc CV";
        if ("INTERVIEWING".equalsIgnoreCase(status)) return "Đang phỏng vấn";
        if ("OFFER".equalsIgnoreCase(status)) return "Đã gửi Offer";
        if ("HIRED".equalsIgnoreCase(status)) return "Đã trúng tuyển";
        if ("REJECTED".equalsIgnoreCase(status)) return "Đã từ chối";
        return status;
    }
}

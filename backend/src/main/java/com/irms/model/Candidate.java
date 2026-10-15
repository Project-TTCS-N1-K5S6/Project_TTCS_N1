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
    private String extractedName;
    private String extractedEmail;
    private String extractedPhone;
    private String extractionStatus; // SUCCESS, PARTIAL, FAILED, CONFIRMED, NONE
    private Timestamp cvExtractedAt;
    private String notes;
    private String coverLetter;
    private String ipAddress;
    private String departmentId;
    private String departmentName;
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

    public String getExtractedName() { return extractedName; }
    public void setExtractedName(String extractedName) { this.extractedName = extractedName; }

    public String getExtractedEmail() { return extractedEmail; }
    public void setExtractedEmail(String extractedEmail) { this.extractedEmail = extractedEmail; }

    public String getExtractedPhone() { return extractedPhone; }
    public void setExtractedPhone(String extractedPhone) { this.extractedPhone = extractedPhone; }

    public String getExtractionStatus() { return extractionStatus; }
    public void setExtractionStatus(String extractionStatus) { this.extractionStatus = extractionStatus; }

    public Timestamp getCvExtractedAt() { return cvExtractedAt; }
    public void setCvExtractedAt(Timestamp cvExtractedAt) { this.cvExtractedAt = cvExtractedAt; }

    public boolean hasExtractedData() {
        return (extractedName != null && !extractedName.trim().isEmpty())
                || (extractedEmail != null && !extractedEmail.trim().isEmpty())
                || (extractedPhone != null && !extractedPhone.trim().isEmpty());
    }

    public String getExtractionStatusBadgeClass() {
        if ("SUCCESS".equalsIgnoreCase(extractionStatus)) return "bg-info text-white";
        if ("PARTIAL".equalsIgnoreCase(extractionStatus)) return "bg-warning text-dark";
        if ("CONFIRMED".equalsIgnoreCase(extractionStatus)) return "bg-success text-white";
        if ("FAILED".equalsIgnoreCase(extractionStatus)) return "bg-danger text-white";
        return "bg-secondary text-white";
    }

    public String getExtractionStatusLabel() {
        if ("SUCCESS".equalsIgnoreCase(extractionStatus)) return "Đã bóc tách CV";
        if ("PARTIAL".equalsIgnoreCase(extractionStatus)) return "Bóc tách 1 phần";
        if ("CONFIRMED".equalsIgnoreCase(extractionStatus)) return "Đã đối chiếu";
        if ("FAILED".equalsIgnoreCase(extractionStatus)) return "Không thể bóc tách";
        return "Chưa bóc tách";
    }

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

package com.irms.model;

import java.io.Serializable;

/**
 * [KN-283]: Kết quả xử lý chuyển giai đoạn ứng viên
 * Chứa trạng thái thành công, mã phản hồi HTTP, thông điệp nghiệp vụ tiếng Việt,
 * và các cờ kiểm soát đặc cách bỏ qua giai đoạn Phỏng vấn của Trưởng phòng Nhân sự.
 */
public class StageTransitionResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private int statusCode;
    private String message;
    private String candidateId;
    private String candidateName;
    private String oldStage;
    private String oldStageName;
    private String newStage;
    private String newStageName;
    private boolean requiresHrApproval;
    private boolean canCurrentUserApprove;
    private String rejectionReasonId;
    private String rejectionReasonName;
    private boolean skipInterview;
    private String confirmationNote;

    public StageTransitionResult() {}

    public StageTransitionResult(boolean success, int statusCode, String message) {
        this.success = success;
        this.statusCode = statusCode;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getOldStage() {
        return oldStage;
    }

    public void setOldStage(String oldStage) {
        this.oldStage = oldStage;
    }

    public String getOldStageName() {
        return oldStageName;
    }

    public void setOldStageName(String oldStageName) {
        this.oldStageName = oldStageName;
    }

    public String getNewStage() {
        return newStage;
    }

    public void setNewStage(String newStage) {
        this.newStage = newStage;
    }

    public String getNewStageName() {
        return newStageName;
    }

    public void setNewStageName(String newStageName) {
        this.newStageName = newStageName;
    }

    public boolean isRequiresHrApproval() {
        return requiresHrApproval;
    }

    public void setRequiresHrApproval(boolean requiresHrApproval) {
        this.requiresHrApproval = requiresHrApproval;
    }

    public boolean isCanCurrentUserApprove() {
        return canCurrentUserApprove;
    }

    public void setCanCurrentUserApprove(boolean canCurrentUserApprove) {
        this.canCurrentUserApprove = canCurrentUserApprove;
    }

    public String getRejectionReasonId() {
        return rejectionReasonId;
    }

    public void setRejectionReasonId(String rejectionReasonId) {
        this.rejectionReasonId = rejectionReasonId;
    }

    public String getRejectionReasonName() {
        return rejectionReasonName;
    }

    public void setRejectionReasonName(String rejectionReasonName) {
        this.rejectionReasonName = rejectionReasonName;
    }

    public boolean isSkipInterview() {
        return skipInterview;
    }

    public void setSkipInterview(boolean skipInterview) {
        this.skipInterview = skipInterview;
    }

    public String getConfirmationNote() {
        return confirmationNote;
    }

    public void setConfirmationNote(String confirmationNote) {
        this.confirmationNote = confirmationNote;
    }
}

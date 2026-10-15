package com.irms.model;

import java.io.Serializable;

/**
 * ==============================================================================
 * KẾT QUẢ TRÍCH XUẤT THÔNG TIN TỰ ĐỘNG TỪ CV (KN-213)
 * ==============================================================================
 * Đại diện dữ liệu bóc tách từ tệp CV dạng PDF:
 * - Họ tên (extractedName)
 * - Email (extractedEmail)
 * - Số điện thoại (extractedPhone)
 * - Trạng thái bóc tách (status: SUCCESS, PARTIAL, FAILED, NONE)
 * - Ghi chú / Trích xuất thô (rawTextPreview)
 * ==============================================================================
 */
public class CVExtractionResult implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_PARTIAL = "PARTIAL";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_NONE = "NONE";
    public static final String STATUS_CONFIRMED = "CONFIRMED";

    private boolean success;
    private String extractedName;
    private String extractedEmail;
    private String extractedPhone;
    private String status;
    private String message;
    private String rawTextPreview;

    public CVExtractionResult() {
        this.status = STATUS_NONE;
    }

    public CVExtractionResult(boolean success, String status, String extractedName,
                              String extractedEmail, String extractedPhone,
                              String message, String rawTextPreview) {
        this.success = success;
        this.status = status;
        this.extractedName = extractedName;
        this.extractedEmail = extractedEmail;
        this.extractedPhone = extractedPhone;
        this.message = message;
        this.rawTextPreview = rawTextPreview;
    }

    public static CVExtractionResult success(String name, String email, String phone, String rawText) {
        return new CVExtractionResult(true, STATUS_SUCCESS, name, email, phone,
                "Trích xuất thành công đầy đủ thông tin từ CV.", rawText);
    }

    public static CVExtractionResult partial(String name, String email, String phone, String rawText, String message) {
        return new CVExtractionResult(true, STATUS_PARTIAL, name, email, phone,
                message != null ? message : "Trích xuất được một phần thông tin từ CV.", rawText);
    }

    public static CVExtractionResult failed(String errorMessage) {
        return new CVExtractionResult(false, STATUS_FAILED, null, null, null,
                errorMessage != null ? errorMessage : "Không thể bóc tách nội dung từ tệp CV PDF.", null);
    }

    public static CVExtractionResult none(String message) {
        return new CVExtractionResult(false, STATUS_NONE, null, null, null, message, null);
    }

    public boolean isSuccess() {
        return success && (STATUS_SUCCESS.equals(status) || STATUS_PARTIAL.equals(status));
    }

    public boolean isFullSuccess() {
        return STATUS_SUCCESS.equals(status);
    }

    public boolean isPartial() {
        return STATUS_PARTIAL.equals(status);
    }

    public boolean isFailed() {
        return STATUS_FAILED.equals(status);
    }

    public String getExtractedName() {
        return extractedName;
    }

    public void setExtractedName(String extractedName) {
        this.extractedName = extractedName;
    }

    public String getExtractedEmail() {
        return extractedEmail;
    }

    public void setExtractedEmail(String extractedEmail) {
        this.extractedEmail = extractedEmail;
    }

    public String getExtractedPhone() {
        return extractedPhone;
    }

    public void setExtractedPhone(String extractedPhone) {
        this.extractedPhone = extractedPhone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRawTextPreview() {
        return rawTextPreview;
    }

    public void setRawTextPreview(String rawTextPreview) {
        this.rawTextPreview = rawTextPreview;
    }

    @Override
    public String toString() {
        return "CVExtractionResult{" +
                "success=" + success +
                ", status='" + status + '\'' +
                ", extractedName='" + extractedName + '\'' +
                ", extractedEmail='" + extractedEmail + '\'' +
                ", extractedPhone='" + extractedPhone + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}

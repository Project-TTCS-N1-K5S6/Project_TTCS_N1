package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * ==============================================================================
 * [KN-243] [KN-246]: Model kết quả tra cứu trạng thái hồ sơ - chỉ chứa thông
 * tin được phép công khai với ứng viên.
 * ==============================================================================
 * KHÔNG chứa:
 *  - notes (nhận xét nội bộ)
 *  - Tên người phỏng vấn
 *  - rejection_reason_id / rejection_reason_name chi tiết nội bộ
 *  - Mức lương đề nghị
 *
 * Trạng thái tổng quát được ánh xạ từ status nội bộ sang 4 mức công khai:
 *  APPLIED, SCREENING           -> "Đã nhận"
 *  INTERVIEWING                 -> "Đang xem xét" hoặc "Mời phỏng vấn"
 *  OFFER, HIRED                 -> "Kết thúc" (thành công)
 *  REJECTED                     -> "Kết thúc"
 * ==============================================================================
 */
public class ApplicationLookupResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String candidateId;
    private String trackingCode;
    private String fullName;
    private String rawStatus;        // Status nội bộ - chỉ dùng để map, không expose ra ngoài
    private String positionTitle;
    private Timestamp submittedAt;
    private Timestamp lastUpdatedAt;

    public ApplicationLookupResult() {}

    // ========== Getters / Setters ==========

    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }

    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }

    public String getDisplayCode() {
        return (trackingCode != null && !trackingCode.trim().isEmpty()) ? trackingCode : candidateId;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    /** Chỉ dùng nội bộ để tính publicStatus - KHÔNG trả thẳng ra JSON/JSP */
    public String getRawStatus() { return rawStatus; }
    public void setRawStatus(String rawStatus) { this.rawStatus = rawStatus; }

    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

    public Timestamp getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Timestamp submittedAt) { this.submittedAt = submittedAt; }

    public Timestamp getLastUpdatedAt() { return lastUpdatedAt; }
    public void setLastUpdatedAt(Timestamp lastUpdatedAt) { this.lastUpdatedAt = lastUpdatedAt; }

    // ========== [KN-243]: Logic chuyển đổi sang trạng thái tổng quát (public) ==========

    /**
     * Ánh xạ trạng thái nội bộ sang 4 mức tổng quát cho ứng viên tra cứu.
     * APPLIED + SCREENING  -> ĐÃ NHẬN
     * INTERVIEWING         -> MỜI PHỎNG VẤN
     * OFFER + HIRED        -> KẾT THÚC (thành công)
     * REJECTED             -> KẾT THÚC
     * (null/unknown)       -> ĐANG XEM XÉT (mặc định an toàn)
     */
    public String getPublicStatus() {
        if (rawStatus == null) return "DANG_XEM_XET";
        switch (rawStatus.toUpperCase()) {
            case "APPLIED":
                return "DA_NHAN";
            case "SCREENING":
                return "DANG_XEM_XET";
            case "INTERVIEWING":
                return "MOI_PHONG_VAN";
            case "OFFER":
            case "HIRED":
            case "REJECTED":
                return "KET_THUC";
            default:
                return "DANG_XEM_XET";
        }
    }

    /**
     * Nhãn tiếng Việt tổng quát hiển thị cho ứng viên.
     */
    public String getPublicStatusLabel() {
        switch (getPublicStatus()) {
            case "DA_NHAN":       return "Đã nhận";
            case "DANG_XEM_XET": return "Đang xem xét";
            case "MOI_PHONG_VAN": return "Mời phỏng vấn";
            case "KET_THUC":      return "Kết thúc";
            default:              return "Đang xem xét";
        }
    }

    /**
     * Badge CSS class tương ứng với trạng thái tổng quát.
     */
    public String getPublicStatusBadgeClass() {
        switch (getPublicStatus()) {
            case "DA_NHAN":       return "badge-status-received";
            case "DANG_XEM_XET": return "badge-status-reviewing";
            case "MOI_PHONG_VAN": return "badge-status-interview";
            case "KET_THUC":      return "badge-status-done";
            default:              return "badge-status-reviewing";
        }
    }

    /**
     * Icon tương ứng trạng thái tổng quát.
     */
    public String getPublicStatusIcon() {
        switch (getPublicStatus()) {
            case "DA_NHAN":       return "bi-inbox-fill";
            case "DANG_XEM_XET": return "bi-eye-fill";
            case "MOI_PHONG_VAN": return "bi-calendar-check-fill";
            case "KET_THUC":      return "bi-flag-fill";
            default:              return "bi-hourglass-split";
        }
    }

    /**
     * Số thứ tự bước trong timeline (1-4).
     */
    public int getPublicStatusStep() {
        switch (getPublicStatus()) {
            case "DA_NHAN":       return 1;
            case "DANG_XEM_XET": return 2;
            case "MOI_PHONG_VAN": return 3;
            case "KET_THUC":      return 4;
            default:              return 2;
        }
    }

    /**
     * Kiểm tra hồ sơ có kết thúc thành công không (HIRED/OFFER).
     * Dùng để hiển thị thông điệp tích cực ở trang kết quả.
     */
    public boolean isSuccessfulEnd() {
        return "OFFER".equalsIgnoreCase(rawStatus) || "HIRED".equalsIgnoreCase(rawStatus);
    }
}

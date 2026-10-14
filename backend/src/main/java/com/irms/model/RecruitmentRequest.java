package com.irms.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Đại diện thực thể Yêu cầu tuyển dụng (bảng recruitment_requisitions)
 */
public class RecruitmentRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String code;
    private String title;
    private String positionTitle;
    private String departmentId;
    private String departmentName;
    private int headcount = 1;
    private String recruitmentReason = "NEW_HEADCOUNT"; // REPLACEMENT (Thay thế), NEW_HEADCOUNT (Tăng mới)
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String currency = "VND";
    private Date deadline; // Ngày cần người
    private String jobDescription;
    private String jobRequirements;
    private String salaryExplanation; // Giải trình nếu dải lương đề xuất nằm ngoài dải chuẩn
    private String status = "DRAFT"; // DRAFT, PENDING_APPROVAL, OPEN, FULFILLED, PAUSED, CANCELLED, CLOSED, REJECTED
    private String closeStatus; // FULFILLED (Đã tuyển đủ), PAUSED (Tạm dừng), CANCELLED (Huỷ)
    private String closeReason; // Lý do đóng/kết thúc
    private String closeNotes; // Ghi chú chi tiết lý do đóng
    private Timestamp closedAt; // Thời điểm đóng
    private String closedBy; // Người thực hiện đóng
    private String closedByName;
    private int activeCandidatesCount = 0; // Số ứng viên còn trong pipeline
    private int activeJobPostingsCount = 0; // Số tin tuyển dụng đang đăng
    private String recruiterId;
    private String recruiterName;
    private String hiringManagerId;
    private String hiringManagerName;
    private String createdBy;
    private String createdByName;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public RecruitmentRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public int getHeadcount() { return headcount; }
    public void setHeadcount(int headcount) { this.headcount = headcount; }

    public String getRecruitmentReason() { return recruitmentReason; }
    public void setRecruitmentReason(String recruitmentReason) { this.recruitmentReason = recruitmentReason; }

    public BigDecimal getMinSalary() { return minSalary; }
    public void setMinSalary(BigDecimal minSalary) { this.minSalary = minSalary; }

    public BigDecimal getMaxSalary() { return maxSalary; }
    public void setMaxSalary(BigDecimal maxSalary) { this.maxSalary = maxSalary; }

    public String getCurrency() { return currency != null ? currency : "VND"; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }

    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

    public String getJobRequirements() { return jobRequirements; }
    public void setJobRequirements(String jobRequirements) { this.jobRequirements = jobRequirements; }

    public String getSalaryExplanation() { return salaryExplanation; }
    public void setSalaryExplanation(String salaryExplanation) { this.salaryExplanation = salaryExplanation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRecruiterId() { return recruiterId; }
    public void setRecruiterId(String recruiterId) { this.recruiterId = recruiterId; }

    public String getRecruiterName() { return recruiterName; }
    public void setRecruiterName(String recruiterName) { this.recruiterName = recruiterName; }

    public String getHiringManagerId() { return hiringManagerId; }
    public void setHiringManagerId(String hiringManagerId) { this.hiringManagerId = hiringManagerId; }

    public String getHiringManagerName() { return hiringManagerName; }
    public void setHiringManagerName(String hiringManagerName) { this.hiringManagerName = hiringManagerName; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getRecruitmentReasonLabel() {
        if ("REPLACEMENT".equalsIgnoreCase(recruitmentReason)) {
            return "Thay thế";
        }
        return "Tăng mới";
    }

    public String getFormattedSalaryRange() {
        if (minSalary == null && maxSalary == null) {
            return "Thỏa thuận";
        }
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        String min = minSalary != null ? nf.format(minSalary) : "0";
        String max = maxSalary != null ? nf.format(maxSalary) : "Thỏa thuận";
        return min + " - " + max + " " + getCurrency();
    }

    public String getCloseStatus() { return closeStatus; }
    public void setCloseStatus(String closeStatus) { this.closeStatus = closeStatus; }

    public String getCloseReason() { return closeReason; }
    public void setCloseReason(String closeReason) { this.closeReason = closeReason; }

    public String getCloseNotes() { return closeNotes; }
    public void setCloseNotes(String closeNotes) { this.closeNotes = closeNotes; }

    public Timestamp getClosedAt() { return closedAt; }
    public void setClosedAt(Timestamp closedAt) { this.closedAt = closedAt; }

    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }

    public String getClosedByName() { return closedByName; }
    public void setClosedByName(String closedByName) { this.closedByName = closedByName; }

    public int getActiveCandidatesCount() { return activeCandidatesCount; }
    public void setActiveCandidatesCount(int activeCandidatesCount) { this.activeCandidatesCount = activeCandidatesCount; }

    public int getActiveJobPostingsCount() { return activeJobPostingsCount; }
    public void setActiveJobPostingsCount(int activeJobPostingsCount) { this.activeJobPostingsCount = activeJobPostingsCount; }

    public boolean isTerminal() {
        return "FULFILLED".equalsIgnoreCase(status) ||
               "PAUSED".equalsIgnoreCase(status) ||
               "CANCELLED".equalsIgnoreCase(status) ||
               "CLOSED".equalsIgnoreCase(status);
    }

    public String getStatusBadgeClass() {
        if ("DRAFT".equalsIgnoreCase(status)) return "bg-secondary text-white";
        if ("PENDING_APPROVAL".equalsIgnoreCase(status)) return "bg-warning text-dark";
        if ("OPEN".equalsIgnoreCase(status)) return "bg-success text-white";
        if ("FULFILLED".equalsIgnoreCase(status) || "Đã tuyển đủ".equalsIgnoreCase(closeReason)) return "bg-teal-custom text-white";
        if ("PAUSED".equalsIgnoreCase(status) || "Tạm dừng".equalsIgnoreCase(closeReason)) return "bg-warning-subtle text-warning-emphasis border border-warning";
        if ("CANCELLED".equalsIgnoreCase(status) || "Huỷ".equalsIgnoreCase(closeReason)) return "bg-danger text-white";
        if ("CLOSED".equalsIgnoreCase(status)) return "bg-dark text-white";
        if ("REJECTED".equalsIgnoreCase(status)) return "bg-danger text-white";
        return "bg-secondary";
    }

    public String getStatusLabel() {
        if ("DRAFT".equalsIgnoreCase(status)) return "Bản nháp";
        if ("PENDING_APPROVAL".equalsIgnoreCase(status)) return "Chờ phê duyệt";
        if ("OPEN".equalsIgnoreCase(status)) return "Đang tuyển";
        if ("FULFILLED".equalsIgnoreCase(status)) return "Đã tuyển đủ";
        if ("PAUSED".equalsIgnoreCase(status)) return "Tạm dừng";
        if ("CANCELLED".equalsIgnoreCase(status)) return "Đã huỷ";
        if ("CLOSED".equalsIgnoreCase(status)) {
            if ("FULFILLED".equalsIgnoreCase(closeStatus)) return "Đã tuyển đủ";
            if ("PAUSED".equalsIgnoreCase(closeStatus)) return "Tạm dừng";
            if ("CANCELLED".equalsIgnoreCase(closeStatus)) return "Đã huỷ";
            return "Đã đóng";
        }
        if ("REJECTED".equalsIgnoreCase(status)) return "Từ chối";
        return status != null ? status : "Không xác định";
    }

    /**
     * [KN-109 / KN-118]: Số ngày mở của yêu cầu tuyển dụng.
     * Tính từ ngày tạo (createdAt) tới ngày hiện tại (hoặc ngày đóng nếu đã kết thúc).
     */
    public long getDaysOpen() {
        if (createdAt == null) return 0;
        LocalDate startDate = createdAt.toLocalDateTime().toLocalDate();
        LocalDate endDate = (isTerminal() && closedAt != null) ? closedAt.toLocalDateTime().toLocalDate() : LocalDate.now();
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        return Math.max(0, days);
    }

    /**
     * [KN-109 / KN-118]: Số ngày còn lại tới ngày cần người (deadline).
     * > 0: Còn n ngày.
     * = 0: Hôm nay là hạn chót.
     * < 0: Quá hạn |n| ngày.
     * null: Chưa đặt ngày cần người.
     */
    public Long getDaysRemaining() {
        if (deadline == null) return null;
        LocalDate today = LocalDate.now();
        LocalDate deadlineDate = deadline.toLocalDate();
        return ChronoUnit.DAYS.between(today, deadlineDate);
    }

    /**
     * [KN-109 / KN-118]: Kiểm tra yêu cầu tuyển dụng có bị quá ngày cần người hay không.
     * Quá hạn khi deadline < hôm nay và yêu cầu chưa kết thúc (chưa FULFILLED, CANCELLED, CLOSED).
     */
    public boolean isOverdue() {
        if (deadline == null) return false;
        if ("FULFILLED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            return false;
        }
        return deadline.toLocalDate().isBefore(LocalDate.now());
    }

    /**
     * [KN-109 / KN-118]: Số ngày quá hạn cần người (số dương).
     */
    public long getDaysOverdue() {
        if (!isOverdue()) return 0;
        Long remaining = getDaysRemaining();
        return remaining != null ? Math.abs(remaining) : 0;
    }

    /**
     * [KN-109 / KN-118]: Kiểm tra yêu cầu sắp đến hạn cần người (trong vòng 7 ngày).
     */
    public boolean isDueSoon() {
        if (deadline == null || isOverdue()) return false;
        if ("FULFILLED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            return false;
        }
        Long remaining = getDaysRemaining();
        return remaining != null && remaining >= 0 && remaining <= 7;
    }
}

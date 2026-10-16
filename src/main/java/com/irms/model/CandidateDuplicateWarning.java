package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Thực thể Cảnh báo hồ sơ ứng viên nghi trùng lặp (bảng candidate_duplicate_warnings)
 */
public class CandidateDuplicateWarning implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String candidateId1;
    private String candidateId2;
    private String matchType; // 'EMAIL', 'PHONE', 'BOTH'
    private String matchValue;
    private String status; // 'PENDING', 'DISMISSED', 'MERGED', 'SNOOZED'
    private Timestamp snoozeUntil;
    private int reminderCount;
    private int reminderIntervalDays = 3;
    private String notes;
    private String resolvedBy;
    private String resolvedByName;
    private Timestamp resolvedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Đối tượng đính kèm để hiển thị giao diện nhanh
    private Candidate candidate1;
    private Candidate candidate2;
    private CandidateMergeHistory mergeHistory;

    public CandidateDuplicateWarning() {}

    public CandidateMergeHistory getMergeHistory() { return mergeHistory; }
    public void setMergeHistory(CandidateMergeHistory mergeHistory) { this.mergeHistory = mergeHistory; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCandidateId1() { return candidateId1; }
    public void setCandidateId1(String candidateId1) { this.candidateId1 = candidateId1; }

    public String getCandidateId2() { return candidateId2; }
    public void setCandidateId2(String candidateId2) { this.candidateId2 = candidateId2; }

    public String getMatchType() { return matchType; }
    public void setMatchType(String matchType) { this.matchType = matchType; }

    public String getMatchValue() { return matchValue; }
    public void setMatchValue(String matchValue) { this.matchValue = matchValue; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getSnoozeUntil() { return snoozeUntil; }
    public void setSnoozeUntil(Timestamp snoozeUntil) { this.snoozeUntil = snoozeUntil; }

    public int getReminderCount() { return reminderCount; }
    public void setReminderCount(int reminderCount) { this.reminderCount = reminderCount; }

    public int getReminderIntervalDays() { return reminderIntervalDays; }
    public void setReminderIntervalDays(int reminderIntervalDays) { this.reminderIntervalDays = reminderIntervalDays; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }

    public String getResolvedByName() { return resolvedByName; }
    public void setResolvedByName(String resolvedByName) { this.resolvedByName = resolvedByName; }

    public Timestamp getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Timestamp resolvedAt) { this.resolvedAt = resolvedAt; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public Candidate getCandidate1() { return candidate1; }
    public void setCandidate1(Candidate candidate1) { this.candidate1 = candidate1; }

    public Candidate getCandidate2() { return candidate2; }
    public void setCandidate2(Candidate candidate2) { this.candidate2 = candidate2; }

    public boolean isDueForReminder() {
        if ("SNOOZED".equalsIgnoreCase(status) && snoozeUntil != null) {
            return snoozeUntil.before(new Timestamp(System.currentTimeMillis()));
        }
        return "PENDING".equalsIgnoreCase(status);
    }

    public String getStatusBadgeClass() {
        if ("PENDING".equalsIgnoreCase(status)) return "bg-warning text-dark";
        if ("SNOOZED".equalsIgnoreCase(status)) return "bg-info text-dark";
        if ("MERGED".equalsIgnoreCase(status)) return "bg-success text-white";
        if ("DISMISSED".equalsIgnoreCase(status)) return "bg-secondary text-white";
        return "bg-secondary";
    }

    public String getStatusLabel() {
        if ("PENDING".equalsIgnoreCase(status)) return "Chờ xử lý";
        if ("SNOOZED".equalsIgnoreCase(status)) return "Đã tạm hoãn nhắc lại";
        if ("MERGED".equalsIgnoreCase(status)) return "Đã hợp nhất";
        if ("DISMISSED".equalsIgnoreCase(status)) return "Đã bỏ qua";
        return status;
    }

    public String getMatchTypeLabel() {
        if ("BOTH".equalsIgnoreCase(matchType)) return "Trùng cả Email & Số điện thoại";
        if ("EMAIL".equalsIgnoreCase(matchType)) return "Trùng Email";
        if ("PHONE".equalsIgnoreCase(matchType)) return "Trùng Số điện thoại";
        return matchType;
    }

    public String getMatchTypeBadgeClass() {
        if ("BOTH".equalsIgnoreCase(matchType)) return "bg-danger text-white";
        if ("EMAIL".equalsIgnoreCase(matchType)) return "bg-primary text-white";
        if ("PHONE".equalsIgnoreCase(matchType)) return "bg-warning text-dark";
        return "bg-secondary";
    }
}

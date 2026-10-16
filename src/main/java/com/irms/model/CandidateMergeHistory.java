package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Thực thể Lịch sử hợp nhất hồ sơ ứng viên (bảng candidate_merge_history)
 */
public class CandidateMergeHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String warningId;
    private String primaryCandidateId;
    private String secondaryCandidateId;
    private String primaryCandidateName;
    private String secondaryCandidateName;
    private String mergedByUserId;
    private String mergedByUserName;
    private String fieldSelectionDetails; // JSON chi tiết lựa chọn các trường
    private String mergedSnapshot; // JSON snapshot toàn bộ 2 hồ sơ trước khi hợp nhất
    private String mergeNotes;
    private Timestamp createdAt;

    private boolean isUndone;
    private Timestamp undoneAt;
    private String undoneByUserId;
    private String undoneByUserName;
    private String undoNotes;

    public CandidateMergeHistory() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWarningId() { return warningId; }
    public void setWarningId(String warningId) { this.warningId = warningId; }

    public String getPrimaryCandidateId() { return primaryCandidateId; }
    public void setPrimaryCandidateId(String primaryCandidateId) { this.primaryCandidateId = primaryCandidateId; }

    public String getSecondaryCandidateId() { return secondaryCandidateId; }
    public void setSecondaryCandidateId(String secondaryCandidateId) { this.secondaryCandidateId = secondaryCandidateId; }

    public String getPrimaryCandidateName() { return primaryCandidateName; }
    public void setPrimaryCandidateName(String primaryCandidateName) { this.primaryCandidateName = primaryCandidateName; }

    public String getSecondaryCandidateName() { return secondaryCandidateName; }
    public void setSecondaryCandidateName(String secondaryCandidateName) { this.secondaryCandidateName = secondaryCandidateName; }

    public String getMergedByUserId() { return mergedByUserId; }
    public void setMergedByUserId(String mergedByUserId) { this.mergedByUserId = mergedByUserId; }

    public String getMergedByUserName() { return mergedByUserName; }
    public void setMergedByUserName(String mergedByUserName) { this.mergedByUserName = mergedByUserName; }

    // Alias getter cho JSP EL (${mh.mergedByUsername})
    public String getMergedByUsername() { return mergedByUserName; }

    public String getFieldSelectionDetails() { return fieldSelectionDetails; }
    public void setFieldSelectionDetails(String fieldSelectionDetails) { this.fieldSelectionDetails = fieldSelectionDetails; }

    // Alias getter cho JSP EL (${mh.fieldSelections})
    public String getFieldSelections() { return fieldSelectionDetails; }

    public String getMergedSnapshot() { return mergedSnapshot; }
    public void setMergedSnapshot(String mergedSnapshot) { this.mergedSnapshot = mergedSnapshot; }

    public String getMergeNotes() { return mergeNotes; }
    public void setMergeNotes(String mergeNotes) { this.mergeNotes = mergeNotes; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    // Alias getter cho JSP EL (${mh.mergedAt})
    public Timestamp getMergedAt() { return createdAt; }

    public boolean isUndone() { return isUndone; }
    public void setUndone(boolean undone) { isUndone = undone; }

    public Timestamp getUndoneAt() { return undoneAt; }
    public void setUndoneAt(Timestamp undoneAt) { this.undoneAt = undoneAt; }

    public String getUndoneByUserId() { return undoneByUserId; }
    public void setUndoneByUserId(String undoneByUserId) { this.undoneByUserId = undoneByUserId; }

    public String getUndoneByUserName() { return undoneByUserName; }
    public void setUndoneByUserName(String undoneByUserName) { this.undoneByUserName = undoneByUserName; }

    public String getUndoNotes() { return undoNotes; }
    public void setUndoNotes(String undoNotes) { this.undoNotes = undoNotes; }

    /**
     * Kiểm tra xem lần hợp nhất này có còn quyền hoàn tác trong vòng 24 giờ không
     */
    public boolean isCanUndo() {
        if (isUndone) return false;
        if (createdAt == null) return false;
        long elapsedMs = System.currentTimeMillis() - createdAt.getTime();
        return elapsedMs >= 0 && elapsedMs <= 24L * 3600 * 1000;
    }

    /**
     * Tính số phút còn lại để hoàn tác trong khung giờ 24h
     */
    public long getRemainingUndoMinutes() {
        if (!isCanUndo()) return 0;
        long elapsedMs = System.currentTimeMillis() - createdAt.getTime();
        long remainingMs = (24L * 3600 * 1000) - elapsedMs;
        return Math.max(0, remainingMs / (60 * 1000));
    }

    /**
     * Chuỗi mô tả thời gian còn lại để hoàn tác (Ví dụ: "Còn 23 giờ 15 phút")
     */
    public String getRemainingUndoText() {
        if (!isCanUndo()) {
            if (isUndone) return "Đã hoàn tác";
            return "Hết hạn hoàn tác (> 24 giờ)";
        }
        long totalMinutes = getRemainingUndoMinutes();
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours > 0) {
            return "Còn " + hours + " giờ " + minutes + " phút";
        } else {
            return "Còn " + minutes + " phút";
        }
    }
}

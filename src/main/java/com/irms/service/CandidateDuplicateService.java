package com.irms.service;

import com.irms.dao.CandidateActivityDAO;
import com.irms.dao.CandidateDAO;
import com.irms.dao.CandidateDuplicateDAO;
import com.irms.model.Candidate;
import com.irms.model.CandidateActivity;
import com.irms.model.CandidateComparisonDTO;
import com.irms.model.CandidateDuplicateWarning;
import com.irms.model.CandidateMergeHistory;
import com.irms.model.User;
import com.irms.util.JsonUtil;
import com.irms.util.SecurityUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tầng nghiệp vụ Xử lý Cảnh báo & Phát hiện hồ sơ trùng lặp, Đối chiếu và Hợp nhất hồ sơ ứng viên
 */
public class CandidateDuplicateService {
    private static final Logger LOGGER = Logger.getLogger(CandidateDuplicateService.class.getName());

    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final CandidateDuplicateDAO duplicateDAO = new CandidateDuplicateDAO();
    private final CandidateActivityDAO activityDAO = new CandidateActivityDAO();
    private final AuditService auditService = new AuditService();

    /**
     * Kiểm tra nhanh danh sách hồ sơ nghi trùng lặp theo Email hoặc Số điện thoại (dành cho API / Validation)
     */
    public List<Candidate> checkForDuplicates(String email, String phone, String excludeCandidateId) {
        return duplicateDAO.findDuplicates(email, phone, excludeCandidateId);
    }

    /**
     * Tự động quét và phát hiện hồ sơ trùng khi tạo mới hoặc cập nhật hồ sơ, tự động sinh cảnh báo (Warnings)
     */
    public List<CandidateDuplicateWarning> detectAndRecordDuplicates(Candidate candidate, User currentUser) {
        List<CandidateDuplicateWarning> generatedWarnings = new ArrayList<>();
        if (candidate == null || candidate.getId() == null) return generatedWarnings;

        List<Candidate> duplicates = duplicateDAO.findDuplicates(
                candidate.getEmail(), candidate.getPhone(), candidate.getId());

        String normEmail = candidate.getEmail() != null ? candidate.getEmail().trim().toLowerCase() : "";
        String normPhone = CandidateDuplicateDAO.normalizePhone(candidate.getPhone());

        for (Candidate dup : duplicates) {
            String dupEmail = dup.getEmail() != null ? dup.getEmail().trim().toLowerCase() : "";
            String dupPhone = dup.getNormalizedPhone() != null ? dup.getNormalizedPhone() : CandidateDuplicateDAO.normalizePhone(dup.getPhone());

            boolean emailMatch = !normEmail.isEmpty() && normEmail.equals(dupEmail);
            boolean phoneMatch = normPhone != null && !normPhone.isEmpty() && normPhone.equals(dupPhone);

            String matchType;
            String matchValue;
            if (emailMatch && phoneMatch) {
                matchType = "BOTH";
                matchValue = candidate.getEmail() + " | " + (candidate.getPhone() != null ? candidate.getPhone() : "");
            } else if (emailMatch) {
                matchType = "EMAIL";
                matchValue = candidate.getEmail();
            } else {
                matchType = "PHONE";
                matchValue = candidate.getPhone() != null ? candidate.getPhone() : "";
            }

            String warningId = duplicateDAO.createOrUpdateWarning(candidate.getId(), dup.getId(), matchType, matchValue);
            if (warningId != null) {
                CandidateDuplicateWarning w = duplicateDAO.findWarningById(warningId);
                if (w != null) {
                    generatedWarnings.add(w);
                }

                // Ghi nhận hoạt động cảnh báo vào timeline của hồ sơ
                CandidateActivity act = new CandidateActivity(
                        SecurityUtil.generateUUID(),
                        candidate.getId(),
                        "WARNING",
                        "Phát hiện hồ sơ nghi trùng",
                        "Hệ thống phát hiện hồ sơ này có khả năng trùng với ứng viên " + dup.getFullName() +
                        " (" + dup.getEmail() + " - " + (dup.getPhone() != null ? dup.getPhone() : "Chưa có SĐT") +
                        ") theo tiêu chí: " + (emailMatch && phoneMatch ? "Cả Email & SĐT" : (emailMatch ? "Email" : "Số điện thoại")),
                        currentUser != null ? currentUser.getId() : null,
                        currentUser != null ? currentUser.getFullName() : "Hệ thống"
                );
                activityDAO.insertActivity(act);
            }
        }
        return generatedWarnings;
    }

    /**
     * Lấy dữ liệu đối chiếu & so sánh chi tiết giữa hai hồ sơ trước khi hợp nhất
     */
    public CandidateComparisonDTO getComparison(String candidateId1, String candidateId2) {
        if (candidateId1 == null || candidateId2 == null) {
            throw new IllegalArgumentException("Cần cung cấp đủ mã của cả hai hồ sơ ứng viên.");
        }

        Candidate c1 = candidateDAO.findById(candidateId1.trim());
        Candidate c2 = candidateDAO.findById(candidateId2.trim());

        if (c1 == null || c2 == null) {
            throw new IllegalArgumentException("Không tìm thấy một trong hai hồ sơ cần so sánh.");
        }

        CandidateDuplicateWarning warning = duplicateDAO.findWarningByPair(c1.getId(), c2.getId());
        CandidateComparisonDTO dto = new CandidateComparisonDTO(c1, c2, warning);
        dto.setActivities1(activityDAO.findByCandidateId(c1.getId()));
        dto.setActivities2(activityDAO.findByCandidateId(c2.getId()));
        return dto;
    }

    /**
     * Hợp nhất hai hồ sơ ứng viên thành một hồ sơ duy nhất (Surviving Profile).
     * Đảm bảo:
     * 1. Phân quyền và an toàn giao dịch ACID (Database Transaction + Row Lock FOR UPDATE).
     * 2. Áp dụng field selections: giữ lại dữ liệu theo lựa chọn của người dùng.
     * 3. Bảo toàn toàn bộ lịch sử ứng tuyển, phỏng vấn, đánh giá, ghi chú và hoạt động.
     * 4. Lưu vết lịch sử hợp nhất (Snapshot audit history).
     * 5. Cập nhật cảnh báo trùng thành trạng thái MERGED.
     */
    public boolean mergeCandidates(String primaryCandidateId, String secondaryCandidateId,
                                   Map<String, String> fieldSelections, String mergeNotes, User currentUser) {
        if (primaryCandidateId == null || secondaryCandidateId == null
                || primaryCandidateId.trim().equalsIgnoreCase(secondaryCandidateId.trim())) {
            throw new IllegalArgumentException("Mã hồ sơ chính và hồ sơ phụ phải hợp lệ và khác nhau.");
        }

        String pId = primaryCandidateId.trim();
        String sId = secondaryCandidateId.trim();

        Connection conn = null;
        try {
            conn = duplicateDAO.getConnection();
            conn.setAutoCommit(false); // Bắt đầu Transaction

            // Khóa dòng cả 2 hồ sơ theo thứ tự ID tăng dần để ngăn chặn Deadlock khi có nhiều yêu cầu đồng thời
            String firstLock = pId.compareTo(sId) < 0 ? pId : sId;
            String secondLock = pId.compareTo(sId) < 0 ? sId : pId;

            Candidate firstCand = candidateDAO.lockCandidateForUpdate(firstLock, conn);
            Candidate secondCand = candidateDAO.lockCandidateForUpdate(secondLock, conn);

            if (firstCand == null || secondCand == null) {
                conn.rollback();
                throw new IllegalStateException("Không tìm thấy một trong hai hồ sơ ứng viên để thực hiện hợp nhất.");
            }

            Candidate primary = pId.equals(firstCand.getId()) ? firstCand : secondCand;
            Candidate secondary = sId.equals(firstCand.getId()) ? firstCand : secondCand;

            if (primary.isMerged() || secondary.isMerged()) {
                conn.rollback();
                throw new IllegalStateException("Một trong hai hồ sơ này đã được hợp nhất trước đó.");
            }

            // 1. Tạo snapshot dữ liệu của cả hai hồ sơ trước khi hợp nhất để lưu vết kiểm toán
            String primarySnapshot = JsonUtil.toJson(primary);
            String secondarySnapshot = JsonUtil.toJson(secondary);
            String mergedSnapshot = "{\"primaryBefore\":" + primarySnapshot + ",\"secondaryBefore\":" + secondarySnapshot + "}";
            String selectionsJson = fieldSelections != null ? JsonUtil.toJson(fieldSelections) : "{}";

            // 2. Áp dụng chọn lọc dữ liệu theo fieldSelections (giữ lại trường nào từ hồ sơ nào)
            Candidate updatedPrimary = new Candidate();
            updatedPrimary.setId(primary.getId());

            // Họ tên
            if (fieldSelections != null && "secondary".equalsIgnoreCase(fieldSelections.get("fullName"))) {
                updatedPrimary.setFullName(secondary.getFullName());
            } else {
                updatedPrimary.setFullName(primary.getFullName());
            }

            // Email
            if (fieldSelections != null && "secondary".equalsIgnoreCase(fieldSelections.get("email"))) {
                updatedPrimary.setEmail(secondary.getEmail());
            } else {
                updatedPrimary.setEmail(primary.getEmail());
            }

            // Số điện thoại
            if (fieldSelections != null && "secondary".equalsIgnoreCase(fieldSelections.get("phone"))) {
                updatedPrimary.setPhone(secondary.getPhone());
                updatedPrimary.setNormalizedPhone(secondary.getNormalizedPhone());
            } else {
                updatedPrimary.setPhone(primary.getPhone());
                updatedPrimary.setNormalizedPhone(primary.getNormalizedPhone());
            }

            // Tệp CV đính kèm
            if (fieldSelections != null && "secondary".equalsIgnoreCase(fieldSelections.get("cvUrl"))) {
                updatedPrimary.setCvUrl(secondary.getCvUrl());
            } else {
                updatedPrimary.setCvUrl((primary.getCvUrl() != null && !primary.getCvUrl().trim().isEmpty())
                        ? primary.getCvUrl() : secondary.getCvUrl());
            }

            // Vị trí tuyển dụng
            if (fieldSelections != null && "secondary".equalsIgnoreCase(fieldSelections.get("requisitionId"))) {
                updatedPrimary.setRequisitionId(secondary.getRequisitionId());
            } else {
                updatedPrimary.setRequisitionId(primary.getRequisitionId());
            }

            // Nguồn ứng viên
            if (fieldSelections != null && "secondary".equalsIgnoreCase(fieldSelections.get("applicationSourceId"))) {
                updatedPrimary.setApplicationSourceId(secondary.getApplicationSourceId());
            } else {
                updatedPrimary.setApplicationSourceId(primary.getApplicationSourceId());
            }

            // Trạng thái pipeline
            if (fieldSelections != null && "secondary".equalsIgnoreCase(fieldSelections.get("status"))) {
                updatedPrimary.setStatus(secondary.getStatus());
            } else {
                updatedPrimary.setStatus(primary.getStatus());
            }

            // Ghi chú (Notes): Gộp cả hai ghi chú nếu chọn "merge", hoặc chọn 1 bên
            String combinedNotes;
            if (fieldSelections != null && "secondary".equalsIgnoreCase(fieldSelections.get("notes"))) {
                combinedNotes = secondary.getNotes();
            } else if (fieldSelections != null && "primary".equalsIgnoreCase(fieldSelections.get("notes"))) {
                combinedNotes = primary.getNotes();
            } else {
                // Mặc định gộp cả hai ghi chú để không mất thông tin
                StringBuilder nb = new StringBuilder();
                if (primary.getNotes() != null && !primary.getNotes().trim().isEmpty()) {
                    nb.append(primary.getNotes().trim());
                }
                if (secondary.getNotes() != null && !secondary.getNotes().trim().isEmpty()) {
                    if (nb.length() > 0) nb.append("\n---\n[Ghi chú từ hồ sơ hợp nhất (").append(secondary.getFullName()).append(")]:\n");
                    nb.append(secondary.getNotes().trim());
                }
                combinedNotes = nb.toString();
            }
            updatedPrimary.setNotes(combinedNotes);

            // Thẻ nhãn (Tags): Gộp toàn bộ nhãn từ cả 2 hồ sơ
            Set<String> tagSet = new LinkedHashSet<>();
            tagSet.addAll(primary.getTagList());
            tagSet.addAll(secondary.getTagList());
            updatedPrimary.setTags(String.join(", ", tagSet));

            // Thư giới thiệu (Cover letter)
            String cl = (primary.getCoverLetter() != null && !primary.getCoverLetter().trim().isEmpty())
                    ? primary.getCoverLetter() : secondary.getCoverLetter();
            updatedPrimary.setCoverLetter(cl);

            // 3. Thực thi cập nhật hồ sơ chính
            candidateDAO.updateCandidateFields(updatedPrimary, conn);

            // 4. Đánh dấu hồ sơ phụ là đã hợp nhất (is_merged = TRUE, status = 'MERGED', merged_into_id = primaryId)
            candidateDAO.markCandidateAsMerged(secondary.getId(), primary.getId(), conn);

            // 5. Chuyển toàn bộ hoạt động, phỏng vấn, đánh giá, ghi chú từ hồ sơ phụ sang hồ sơ chính (bảo toàn lịch sử)
            activityDAO.transferActivities(secondary.getId(), primary.getId(), conn);

            // 6. Ghi nhận hoạt động hợp nhất hồ sơ mới vào timeline hồ sơ chính
            CandidateActivity mergeAct = new CandidateActivity(
                    SecurityUtil.generateUUID(),
                    primary.getId(),
                    "MERGE",
                    "Hợp nhất hồ sơ ứng viên",
                    "Đã hợp nhất hồ sơ trùng từ: " + secondary.getFullName() + " (" + secondary.getEmail() +
                    (secondary.getPhone() != null ? " - " + secondary.getPhone() : "") + "). Ghi chú hợp nhất: " +
                    (mergeNotes != null && !mergeNotes.trim().isEmpty() ? mergeNotes.trim() : "Không có ghi chú thêm"),
                    currentUser != null ? currentUser.getId() : null,
                    currentUser != null ? currentUser.getFullName() : "Hệ thống"
            );
            activityDAO.insertActivity(mergeAct, conn);

            // 7. Lưu nhật ký lịch sử hợp nhất (CandidateMergeHistory)
            CandidateMergeHistory history = new CandidateMergeHistory();
            history.setId(SecurityUtil.generateUUID());
            CandidateDuplicateWarning warning = duplicateDAO.findWarningByPair(primary.getId(), secondary.getId());
            if (warning != null) {
                history.setWarningId(warning.getId());
            }
            history.setPrimaryCandidateId(primary.getId());
            history.setSecondaryCandidateId(secondary.getId());
            history.setPrimaryCandidateName(primary.getFullName());
            history.setSecondaryCandidateName(secondary.getFullName());
            history.setMergedByUserId(currentUser != null ? currentUser.getId() : null);
            history.setMergedByUserName(currentUser != null ? currentUser.getFullName() : "Hệ thống");
            history.setFieldSelectionDetails(selectionsJson);
            history.setMergedSnapshot(mergedSnapshot);
            history.setMergeNotes(mergeNotes);
            duplicateDAO.saveMergeHistory(history, conn);

            // 8. Đánh dấu giải quyết tất cả cảnh báo trùng lặp liên quan giữa 2 hồ sơ
            duplicateDAO.resolveAllWarningsForPair(primary.getId(), secondary.getId(),
                    currentUser != null ? currentUser.getId() : null, conn);

            // 9. Ghi nhận Audit Log hệ thống
            try {
                auditService.log(
                        currentUser != null ? currentUser.getId() : null,
                        "MERGE_CANDIDATE_PROFILES",
                        "CANDIDATE",
                        primary.getId(),
                        "Hợp nhất hồ sơ ứng viên: Giữ lại " + primary.getFullName() + " (" + primary.getId() +
                        "), sáp nhập từ " + secondary.getFullName() + " (" + secondary.getId() + ")",
                        "127.0.0.1",
                        "IRMS-App"
                );
            } catch (Exception ignored) {}

            conn.commit(); // Thành công - Commit giao dịch
            return true;

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { LOGGER.log(Level.SEVERE, "Rollback failed", ex); }
            }
            LOGGER.log(Level.SEVERE, "Lỗi khi thực hiện hợp nhất hồ sơ ứng viên: " + e.getMessage(), e);
            throw new RuntimeException("Quá trình hợp nhất hồ sơ thất bại: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Tạm hoãn cảnh báo và nhắc lại sau số ngày cấu hình (Snooze)
     */
    public boolean snoozeWarning(String warningId, int days, User currentUser) {
        if (warningId == null || warningId.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã cảnh báo trùng lặp.");
        }
        int interval = days > 0 ? days : 3;
        boolean ok = duplicateDAO.snoozeWarning(warningId.trim(), interval, currentUser != null ? currentUser.getId() : null);
        if (ok) {
            try {
                auditService.log(
                        currentUser != null ? currentUser.getId() : null,
                        "SNOOZE_DUPLICATE_WARNING",
                        "DUPLICATE_WARNING",
                        warningId.trim(),
                        "Tạm hoãn nhắc lại cảnh báo trùng lặp trong " + interval + " ngày",
                        "127.0.0.1",
                        "IRMS-App"
                );
            } catch (Exception ignored) {}
        }
        return ok;
    }

    /**
     * Bỏ qua cảnh báo trùng lặp (xác nhận hai ứng viên khác nhau)
     */
    public boolean dismissWarning(String warningId, String notes, User currentUser) {
        if (warningId == null || warningId.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã cảnh báo trùng lặp.");
        }
        boolean ok = duplicateDAO.dismissWarning(warningId.trim(), notes, currentUser != null ? currentUser.getId() : null);
        if (ok) {
            try {
                auditService.log(
                        currentUser != null ? currentUser.getId() : null,
                        "DISMISS_DUPLICATE_WARNING",
                        "DUPLICATE_WARNING",
                        warningId.trim(),
                        "Bỏ qua cảnh báo trùng lặp. Ghi chú: " + (notes != null ? notes : "Không"),
                        "127.0.0.1",
                        "IRMS-App"
                );
            } catch (Exception ignored) {}
        }
        return ok;
    }

    /**
     * Hoàn tác hợp nhất hồ sơ ứng viên trong vòng 24 giờ.
     * Khôi phục toàn bộ thông tin nguyên trạng của cả hai hồ sơ, tách lại lịch sử hoạt động
     * và đưa cảnh báo nghi trùng về trạng thái PENDING.
     */
    public boolean undoMerge(String mergeHistoryId, String undoNotes, User currentUser) {
        if (mergeHistoryId == null || mergeHistoryId.trim().isEmpty()) {
            throw new IllegalArgumentException("Cần cung cấp mã lịch sử hợp nhất để hoàn tác.");
        }

        CandidateMergeHistory history = duplicateDAO.findMergeHistoryById(mergeHistoryId.trim());
        if (history == null) {
            throw new IllegalArgumentException("Không tìm thấy nhật ký hợp nhất hồ sơ tương ứng.");
        }

        if (history.isUndone()) {
            throw new IllegalStateException("Lần hợp nhất này đã được hoàn tác trước đó.");
        }

        if (!history.isCanUndo()) {
            throw new IllegalStateException("Đã quá thời hạn 24 giờ kể từ thời điểm hợp nhất. Không thể hoàn tác.");
        }

        String snapshotJson = history.getMergedSnapshot();
        if (snapshotJson == null || snapshotJson.trim().isEmpty()) {
            throw new IllegalStateException("Không tìm thấy dữ liệu sao lưu (snapshot) để khôi phục hồ sơ.");
        }

        // Đọc snapshot JSON để lấy lại thông tin ban đầu của primaryBefore và secondaryBefore
        Candidate primaryBefore = null;
        Candidate secondaryBefore = null;
        try {
            com.google.gson.JsonObject obj = JsonUtil.fromJson(snapshotJson, com.google.gson.JsonObject.class);
            if (obj != null) {
                if (obj.has("primaryBefore") && !obj.get("primaryBefore").isJsonNull()) {
                    primaryBefore = JsonUtil.fromJson(obj.get("primaryBefore").toString(), Candidate.class);
                }
                if (obj.has("secondaryBefore") && !obj.get("secondaryBefore").isJsonNull()) {
                    secondaryBefore = JsonUtil.fromJson(obj.get("secondaryBefore").toString(), Candidate.class);
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Lỗi phân tích JSON snapshot: " + ex.getMessage(), ex);
            throw new IllegalStateException("Dữ liệu sao lưu snapshot bị lỗi cấu trúc: " + ex.getMessage());
        }

        if (primaryBefore == null || secondaryBefore == null) {
            throw new IllegalStateException("Dữ liệu snapshot không đầy đủ thông tin của cả hai hồ sơ trước hợp nhất.");
        }

        String pId = primaryBefore.getId();
        String sId = secondaryBefore.getId();

        Connection conn = null;
        try {
            conn = duplicateDAO.getConnection();
            conn.setAutoCommit(false); // Bắt đầu Transaction

            // Khóa dòng 2 hồ sơ theo thứ tự ID để chống deadlock
            String firstLock = pId.compareTo(sId) < 0 ? pId : sId;
            String secondLock = pId.compareTo(sId) < 0 ? sId : pId;

            Candidate firstCand = candidateDAO.lockCandidateForUpdate(firstLock, conn);
            Candidate secondCand = candidateDAO.lockCandidateForUpdate(secondLock, conn);

            if (firstCand == null || secondCand == null) {
                conn.rollback();
                throw new IllegalStateException("Một trong hai hồ sơ không còn tồn tại trong hệ thống.");
            }

            // 1. Khôi phục thông tin hồ sơ chính (Primary) về trạng thái trước khi hợp nhất
            candidateDAO.updateCandidateFields(primaryBefore, conn);

            // 2. Khôi phục thông tin hồ sơ phụ (Secondary) về trạng thái độc lập
            candidateDAO.updateCandidateFields(secondaryBefore, conn);
            candidateDAO.unmarkCandidateMerged(secondaryBefore.getId(), secondaryBefore.getStatus(), conn);

            // 3. Tách và chuyển ngược lại các hoạt động từ hồ sơ chính về hồ sơ phụ
            activityDAO.restoreActivities(secondaryBefore.getId(), primaryBefore.getId(), conn);

            // 4. Ghi nhận hoạt động hoàn tác vào timeline hồ sơ chính
            CandidateActivity primaryAct = new CandidateActivity(
                    SecurityUtil.generateUUID(),
                    primaryBefore.getId(),
                    "UNDO_MERGE",
                    "Hoàn tác hợp nhất hồ sơ",
                    "Đã hoàn tác thao tác hợp nhất với hồ sơ ứng viên: " + secondaryBefore.getFullName() +
                    " (" + secondaryBefore.getEmail() + "). Lý do: " +
                    (undoNotes != null && !undoNotes.trim().isEmpty() ? undoNotes.trim() : "Không có ghi chú thêm"),
                    currentUser != null ? currentUser.getId() : null,
                    currentUser != null ? currentUser.getFullName() : "Hệ thống"
            );
            activityDAO.insertActivity(primaryAct, conn);

            // 5. Ghi nhận hoạt động hoàn tác vào timeline hồ sơ phụ
            CandidateActivity secondaryAct = new CandidateActivity(
                    SecurityUtil.generateUUID(),
                    secondaryBefore.getId(),
                    "UNDO_MERGE",
                    "Khôi phục hồ sơ độc lập",
                    "Hồ sơ đã được khôi phục trạng thái hoạt động độc lập sau khi hoàn tác hợp nhất với: " +
                    primaryBefore.getFullName() + " (" + primaryBefore.getEmail() + "). Lý do: " +
                    (undoNotes != null && !undoNotes.trim().isEmpty() ? undoNotes.trim() : "Không có ghi chú thêm"),
                    currentUser != null ? currentUser.getId() : null,
                    currentUser != null ? currentUser.getFullName() : "Hệ thống"
            );
            activityDAO.insertActivity(secondaryAct, conn);

            // 6. Đưa cảnh báo nghi trùng giữa 2 hồ sơ về trạng thái PENDING
            duplicateDAO.revertWarningsForPair(primaryBefore.getId(), secondaryBefore.getId(), conn);

            // 7. Cập nhật nhật ký hợp nhất thành ĐÃ HOÀN TÁC (is_undone = TRUE)
            duplicateDAO.markMergeHistoryUndone(
                    history.getId(),
                    currentUser != null ? currentUser.getId() : null,
                    currentUser != null ? currentUser.getFullName() : "Hệ thống",
                    undoNotes,
                    conn
            );

            // 8. Ghi nhận nhật ký kiểm toán hệ thống (Audit Log)
            try {
                auditService.log(
                        currentUser != null ? currentUser.getId() : null,
                        "UNDO_MERGE_CANDIDATE_PROFILES",
                        "CANDIDATE",
                        primaryBefore.getId(),
                        "Hoàn tác hợp nhất hồ sơ trong 24 giờ: Khôi phục " + primaryBefore.getFullName() +
                        " (" + primaryBefore.getId() + ") và " + secondaryBefore.getFullName() +
                        " (" + secondaryBefore.getId() + "). Ghi chú: " + (undoNotes != null ? undoNotes : ""),
                        "127.0.0.1",
                        "IRMS-App"
                );
            } catch (Exception ignored) {}

            conn.commit();
            return true;

        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { LOGGER.log(Level.SEVERE, "Rollback failed", ex); }
            }
            LOGGER.log(Level.SEVERE, "Lỗi khi thực hiện hoàn tác hợp nhất: " + e.getMessage(), e);
            throw new RuntimeException("Hoàn tác hợp nhất thất bại: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public List<CandidateDuplicateWarning> getWarnings(String search, String status, int page, int pageSize) {
        int p = Math.max(1, page);
        int size = Math.max(1, pageSize);
        int offset = (p - 1) * size;
        return duplicateDAO.findWarnings(search, status, offset, size);
    }

    public int countWarnings(String search, String status) {
        return duplicateDAO.countWarnings(search, status);
    }

    public int getPendingWarningsCount() {
        return duplicateDAO.countPendingWarnings();
    }

    public CandidateDuplicateWarning getWarningById(String id) {
        return duplicateDAO.findWarningById(id);
    }

    public CandidateMergeHistory getMergeHistoryById(String id) {
        return duplicateDAO.findMergeHistoryById(id);
    }

    public CandidateMergeHistory getMergeHistoryByWarningId(String warningId) {
        return duplicateDAO.findMergeHistoryByWarningId(warningId);
    }

    public CandidateMergeHistory getLatestMergeHistoryForPair(String id1, String id2) {
        return duplicateDAO.findLatestMergeHistoryForPair(id1, id2);
    }

    public List<CandidateMergeHistory> getMergeHistories(String candidateId) {
        return duplicateDAO.findMergeHistoryByCandidateId(candidateId);
    }

    public List<CandidateActivity> getCandidateActivities(String candidateId) {
        return activityDAO.findByCandidateId(candidateId);
    }

    public boolean addActivity(CandidateActivity activity) {
        return activityDAO.insertActivity(activity);
    }
}

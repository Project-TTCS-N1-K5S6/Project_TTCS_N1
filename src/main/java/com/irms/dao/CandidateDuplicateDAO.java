package com.irms.dao;

import com.irms.model.Candidate;
import com.irms.model.CandidateDuplicateWarning;
import com.irms.model.CandidateMergeHistory;
import com.irms.util.SecurityUtil;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Tầng truy xuất dữ liệu Cảnh báo & Phát hiện hồ sơ trùng lặp, Hợp nhất hồ sơ ứng viên
 */
public class CandidateDuplicateDAO extends BaseDAO {

    public CandidateDuplicateDAO() {
        ensureTablesExist();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return super.getConnection();
    }

    /**
     * Chuẩn hóa số điện thoại: loại bỏ ký tự đặc biệt, chuẩn hóa đầu số quốc tế +84 về 0
     */
    public static String normalizePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        if (digits.startsWith("84") && digits.length() >= 11) {
            digits = "0" + digits.substring(2);
        }
        return digits;
    }

    /**
     * Tìm kiếm hồ sơ trùng email hoặc số điện thoại (chỉ quét các hồ sơ chưa bị hợp nhất)
     */
    public List<Candidate> findDuplicates(String email, String phone, String excludeCandidateId) {
        List<Candidate> list = new ArrayList<>();
        String normEmail = (email != null && !email.trim().isEmpty()) ? email.trim().toLowerCase() : null;
        String normPhone = normalizePhone(phone);

        if (normEmail == null && normPhone == null) {
            return list;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT c.*, r.title AS requisition_title, d.name AS department_name, " +
                "source.value AS application_source_name FROM candidates c " +
                "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                "LEFT JOIN departments d ON r.department_id = d.id " +
                "LEFT JOIN shared_catalogs source ON c.application_source_id = source.id " +
                "WHERE (c.is_merged IS NULL OR c.is_merged = FALSE) ");

        List<Object> params = new ArrayList<>();
        if (excludeCandidateId != null && !excludeCandidateId.trim().isEmpty()) {
            sql.append("AND c.id <> ? ");
            params.add(excludeCandidateId.trim());
        }

        sql.append("AND (");
        boolean hasCondition = false;
        if (normEmail != null) {
            sql.append("LOWER(TRIM(c.email)) = ? ");
            params.add(normEmail);
            hasCondition = true;
        }
        if (normPhone != null) {
            if (hasCondition) sql.append("OR ");
            sql.append("(c.normalized_phone = ? OR REPLACE(REPLACE(REPLACE(c.phone, ' ', ''), '-', ''), '.', '') = ? ");
            params.add(normPhone);
            params.add(normPhone);
            if (normPhone.length() >= 9) {
                String last9 = normPhone.substring(normPhone.length() - 9);
                sql.append("OR RIGHT(REPLACE(REPLACE(REPLACE(REPLACE(c.phone, ' ', ''), '-', ''), '.', ''), '+', ''), 9) = ? ");
                params.add(last9);
            }
            sql.append(") ");
            hasCondition = true;
        }
        sql.append(") ORDER BY c.created_at ASC");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                Candidate c = mapCandidateRow(rs);
                list.add(c);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm kiếm hồ sơ trùng lặp: email=" + email + ", phone=" + phone, e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Tạo hoặc cập nhật cảnh báo trùng giữa 2 hồ sơ
     */
    public String createOrUpdateWarning(String candidateIdA, String candidateIdB, String matchType, String matchValue) {
        if (candidateIdA == null || candidateIdB == null || candidateIdA.equals(candidateIdB)) {
            return null;
        }

        // Đảm bảo thứ tự cặp ID nhất quán (id1 < id2) để tránh trùng lặp bản ghi
        String id1 = candidateIdA.compareTo(candidateIdB) < 0 ? candidateIdA : candidateIdB;
        String id2 = candidateIdA.compareTo(candidateIdB) < 0 ? candidateIdB : candidateIdA;

        CandidateDuplicateWarning existing = findWarningByPair(id1, id2);
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            if (existing != null) {
                // Nếu đã tồn tại nhưng ở trạng thái PENDING hoặc SNOOZED, cập nhật matchType và matchValue
                if ("PENDING".equals(existing.getStatus()) || "SNOOZED".equals(existing.getStatus())) {
                    String updateSql = "UPDATE candidate_duplicate_warnings SET match_type = ?, match_value = ?, " +
                                       "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
                    ps = conn.prepareStatement(updateSql);
                    ps.setString(1, matchType);
                    ps.setString(2, matchValue);
                    ps.setString(3, existing.getId());
                    ps.executeUpdate();
                }
                return existing.getId();
            } else {
                String warningId = SecurityUtil.generateUUID();
                String insertSql = "INSERT INTO candidate_duplicate_warnings (id, candidate_id_1, candidate_id_2, " +
                                   "match_type, match_value, status, reminder_count, reminder_interval_days) " +
                                   "VALUES (?, ?, ?, ?, ?, 'PENDING', 0, 3)";
                ps = conn.prepareStatement(insertSql);
                ps.setString(1, warningId);
                ps.setString(2, id1);
                ps.setString(3, id2);
                ps.setString(4, matchType != null ? matchType : "EMAIL");
                ps.setString(5, matchValue != null ? matchValue : "");
                ps.executeUpdate();
                return warningId;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tạo cảnh báo trùng lặp: " + id1 + " - " + id2, e);
            return null;
        } finally {
            close(conn, ps);
        }
    }

    public CandidateDuplicateWarning findWarningById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        String sql = "SELECT w.*, u.full_name AS resolver_name FROM candidate_duplicate_warnings w " +
                     "LEFT JOIN users u ON w.resolved_by = u.id WHERE w.id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                CandidateDuplicateWarning w = mapWarningRow(rs);
                w.setCandidate1(findCandidateById(w.getCandidateId1(), conn));
                w.setCandidate2(findCandidateById(w.getCandidateId2(), conn));
                return w;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm cảnh báo theo ID: " + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public CandidateDuplicateWarning findWarningByPair(String candidateIdA, String candidateIdB) {
        String id1 = candidateIdA.compareTo(candidateIdB) < 0 ? candidateIdA : candidateIdB;
        String id2 = candidateIdA.compareTo(candidateIdB) < 0 ? candidateIdB : candidateIdA;

        String sql = "SELECT * FROM candidate_duplicate_warnings WHERE candidate_id_1 = ? AND candidate_id_2 = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id1);
            ps.setString(2, id2);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapWarningRow(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm cảnh báo theo cặp ID: " + id1 + " - " + id2, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public List<CandidateDuplicateWarning> findWarnings(String search, String status, int offset, int limit) {
        List<CandidateDuplicateWarning> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT w.*, u.full_name AS resolver_name FROM candidate_duplicate_warnings w " +
                "INNER JOIN candidates c1 ON w.candidate_id_1 = c1.id " +
                "INNER JOIN candidates c2 ON w.candidate_id_2 = c2.id " +
                "LEFT JOIN users u ON w.resolved_by = u.id WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            sql.append("AND w.status = ? ");
            params.add(status.trim().toUpperCase());
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (c1.full_name LIKE ? OR c1.email LIKE ? OR c1.phone LIKE ? " +
                       "OR c2.full_name LIKE ? OR c2.email LIKE ? OR c2.phone LIKE ? " +
                       "OR w.match_value LIKE ?) ");
            String kw = "%" + search.trim() + "%";
            for (int i = 0; i < 7; i++) params.add(kw);
        }

        sql.append("ORDER BY CASE WHEN w.status = 'PENDING' THEN 1 WHEN w.status = 'SNOOZED' THEN 2 ELSE 3 END, " +
                   "w.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                CandidateDuplicateWarning w = mapWarningRow(rs);
                w.setCandidate1(findCandidateById(w.getCandidateId1(), conn));
                w.setCandidate2(findCandidateById(w.getCandidateId2(), conn));
                if ("MERGED".equalsIgnoreCase(w.getStatus())) {
                    CandidateMergeHistory mh = findMergeHistoryByWarningId(w.getId());
                    if (mh == null) {
                        mh = findLatestMergeHistoryForPair(w.getCandidateId1(), w.getCandidateId2());
                    }
                    w.setMergeHistory(mh);
                }
                list.add(w);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách cảnh báo trùng lặp", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public int countWarnings(String search, String status) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM candidate_duplicate_warnings w " +
                "INNER JOIN candidates c1 ON w.candidate_id_1 = c1.id " +
                "INNER JOIN candidates c2 ON w.candidate_id_2 = c2.id WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
            sql.append("AND w.status = ? ");
            params.add(status.trim().toUpperCase());
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (c1.full_name LIKE ? OR c1.email LIKE ? OR c1.phone LIKE ? " +
                       "OR c2.full_name LIKE ? OR c2.email LIKE ? OR c2.phone LIKE ? " +
                       "OR w.match_value LIKE ?) ");
            String kw = "%" + search.trim() + "%";
            for (int i = 0; i < 7; i++) params.add(kw);
        }

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đếm số lượng cảnh báo trùng lặp", e);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    public int countPendingWarnings() {
        String sql = "SELECT COUNT(*) FROM candidate_duplicate_warnings WHERE status = 'PENDING' " +
                     "OR (status = 'SNOOZED' AND snooze_until <= CURRENT_TIMESTAMP)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đếm số cảnh báo trùng lặp đang chờ xử lý", e);
        }
        return 0;
    }

    /**
     * Tạm hoãn cảnh báo và nhắc lại sau số ngày cấu hình (Snooze)
     */
    public boolean snoozeWarning(String warningId, int days, String userId) {
        if (warningId == null) return false;
        int d = days > 0 ? days : 3;
        String sql = "UPDATE candidate_duplicate_warnings SET status = 'SNOOZED', " +
                     "snooze_until = DATE_ADD(CURRENT_TIMESTAMP, INTERVAL ? DAY), " +
                     "reminder_count = reminder_count + 1, reminder_interval_days = ?, " +
                     "resolved_by = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, d);
            ps.setInt(2, d);
            ps.setString(3, userId);
            ps.setString(4, warningId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tạm hoãn nhắc lại cảnh báo: " + warningId, e);
            return false;
        }
    }

    /**
     * Bỏ qua cảnh báo (xác nhận không phải trùng)
     */
    public boolean dismissWarning(String warningId, String notes, String userId) {
        if (warningId == null) return false;
        String sql = "UPDATE candidate_duplicate_warnings SET status = 'DISMISSED', notes = ?, " +
                     "resolved_by = ?, resolved_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, notes);
            ps.setString(2, userId);
            ps.setString(3, warningId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi bỏ qua cảnh báo trùng: " + warningId, e);
            return false;
        }
    }

    /**
     * Cập nhật cảnh báo thành trạng thái MERGED sau khi hợp nhất thành công
     */
    public boolean resolveWarningMerged(String warningId, String userId, Connection conn) throws SQLException {
        if (warningId == null) return false;
        String sql = "UPDATE candidate_duplicate_warnings SET status = 'MERGED', " +
                     "resolved_by = ?, resolved_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, warningId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Đánh dấu các cảnh báo liên quan giữa cặp hồ sơ này thành MERGED
     */
    public void resolveAllWarningsForPair(String cand1, String cand2, String userId, Connection conn) throws SQLException {
        String sql = "UPDATE candidate_duplicate_warnings SET status = 'MERGED', resolved_by = ?, " +
                     "resolved_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE (candidate_id_1 = ? AND candidate_id_2 = ?) OR (candidate_id_1 = ? AND candidate_id_2 = ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, cand1);
            ps.setString(3, cand2);
            ps.setString(4, cand2);
            ps.setString(5, cand1);
            ps.executeUpdate();
        }
    }

    /**
     * Lưu nhật ký lịch sử hợp nhất hồ sơ
     */
    public boolean saveMergeHistory(CandidateMergeHistory history, Connection conn) throws SQLException {
        if (history == null) return false;
        String sql = "INSERT INTO candidate_merge_history (id, warning_id, primary_candidate_id, secondary_candidate_id, " +
                     "primary_candidate_name, secondary_candidate_name, merged_by_user_id, merged_by_user_name, " +
                     "field_selection_details, merged_snapshot, merge_notes, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, history.getId());
            ps.setString(2, history.getWarningId());
            ps.setString(3, history.getPrimaryCandidateId());
            ps.setString(4, history.getSecondaryCandidateId());
            ps.setString(5, history.getPrimaryCandidateName());
            ps.setString(6, history.getSecondaryCandidateName());
            ps.setString(7, history.getMergedByUserId());
            ps.setString(8, history.getMergedByUserName());
            ps.setString(9, history.getFieldSelectionDetails());
            ps.setString(10, history.getMergedSnapshot());
            ps.setString(11, history.getMergeNotes());
            ps.setTimestamp(12, history.getCreatedAt() != null ? history.getCreatedAt() : new Timestamp(System.currentTimeMillis()));
            return ps.executeUpdate() > 0;
        }
    }

    public List<CandidateMergeHistory> findMergeHistoryByCandidateId(String candidateId) {
        List<CandidateMergeHistory> list = new ArrayList<>();
        if (candidateId == null) return list;
        String sql = "SELECT * FROM candidate_merge_history WHERE primary_candidate_id = ? " +
                     "OR secondary_candidate_id = ? ORDER BY created_at DESC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, candidateId);
            ps.setString(2, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapMergeHistoryRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy lịch sử hợp nhất của ứng viên: " + candidateId, e);
        }
        return list;
    }

    public CandidateMergeHistory findMergeHistoryById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        String sql = "SELECT * FROM candidate_merge_history WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapMergeHistoryRow(rs);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm bản ghi lịch sử hợp nhất: " + id, e);
        }
        return null;
    }

    public CandidateMergeHistory findLatestMergeHistoryForPair(String id1, String id2) {
        if (id1 == null || id2 == null) return null;
        String sql = "SELECT * FROM candidate_merge_history WHERE " +
                     "((primary_candidate_id = ? AND secondary_candidate_id = ?) OR " +
                     "(primary_candidate_id = ? AND secondary_candidate_id = ?)) " +
                     "ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id1);
            ps.setString(2, id2);
            ps.setString(3, id2);
            ps.setString(4, id1);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapMergeHistoryRow(rs);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm lịch sử hợp nhất của cặp: " + id1 + " - " + id2, e);
        }
        return null;
    }

    public CandidateMergeHistory findMergeHistoryByWarningId(String warningId) {
        if (warningId == null || warningId.trim().isEmpty()) return null;
        String sql = "SELECT * FROM candidate_merge_history WHERE warning_id = ? ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, warningId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapMergeHistoryRow(rs);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm lịch sử hợp nhất theo warningId: " + warningId, e);
        }
        return null;
    }

    public boolean markMergeHistoryUndone(String historyId, String undoneByUserId, String undoneByUserName,
                                          String undoNotes, Connection conn) throws SQLException {
        if (historyId == null) return false;
        String sql = "UPDATE candidate_merge_history SET is_undone = TRUE, undone_at = CURRENT_TIMESTAMP, " +
                     "undone_by_user_id = ?, undone_by_user_name = ?, undo_notes = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, undoneByUserId);
            ps.setString(2, undoneByUserName);
            ps.setString(3, undoNotes);
            ps.setString(4, historyId.trim());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean revertWarningsForPair(String id1, String id2, Connection conn) throws SQLException {
        if (id1 == null || id2 == null) return false;
        String sql = "UPDATE candidate_duplicate_warnings SET status = 'PENDING', resolved_by = NULL, resolved_at = NULL " +
                     "WHERE ((candidate_id_1 = ? AND candidate_id_2 = ?) OR (candidate_id_1 = ? AND candidate_id_2 = ?)) " +
                     "AND status = 'MERGED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id1.trim());
            ps.setString(2, id2.trim());
            ps.setString(3, id2.trim());
            ps.setString(4, id1.trim());
            return ps.executeUpdate() > 0;
        }
    }

    public Candidate findCandidateById(String id, Connection conn) {
        if (id == null) return null;
        String sql = "SELECT c.*, r.title AS requisition_title, d.name AS department_name, " +
                     "source.value AS application_source_name FROM candidates c " +
                     "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                     "LEFT JOIN departments d ON r.department_id = d.id " +
                     "LEFT JOIN shared_catalogs source ON c.application_source_id = source.id " +
                     "WHERE c.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapCandidateRow(rs);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy thông tin ứng viên: " + id, e);
        }
        return null;
    }

    private Candidate mapCandidateRow(ResultSet rs) throws SQLException {
        Candidate c = new Candidate();
        c.setId(rs.getString("id"));
        c.setRequisitionId(rs.getString("requisition_id"));
        try { c.setRequisitionTitle(rs.getString("requisition_title")); } catch (SQLException ignored) {}
        try { c.setDepartmentName(rs.getString("department_name")); } catch (SQLException ignored) {}
        try { c.setApplicationSourceId(rs.getString("application_source_id")); } catch (SQLException ignored) {}
        try { c.setApplicationSourceName(rs.getString("application_source_name")); } catch (SQLException ignored) {}
        c.setFullName(rs.getString("full_name"));
        c.setEmail(rs.getString("email"));
        c.setPhone(rs.getString("phone"));
        try { c.setNormalizedPhone(rs.getString("normalized_phone")); } catch (SQLException ignored) {}
        c.setStatus(rs.getString("status"));
        try { c.setMergedIntoId(rs.getString("merged_into_id")); } catch (SQLException ignored) {}
        try { c.setMerged(rs.getBoolean("is_merged")); } catch (SQLException ignored) {}
        try { c.setTrackingCode(rs.getString("tracking_code")); } catch (SQLException ignored) {}
        c.setCvUrl(rs.getString("cv_url"));
        c.setNotes(rs.getString("notes"));
        try { c.setTags(rs.getString("tags")); } catch (SQLException ignored) {}
        try { c.setCoverLetter(rs.getString("cover_letter")); } catch (SQLException ignored) {}
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setUpdatedAt(rs.getTimestamp("updated_at"));
        return c;
    }

    private CandidateDuplicateWarning mapWarningRow(ResultSet rs) throws SQLException {
        CandidateDuplicateWarning w = new CandidateDuplicateWarning();
        w.setId(rs.getString("id"));
        w.setCandidateId1(rs.getString("candidate_id_1"));
        w.setCandidateId2(rs.getString("candidate_id_2"));
        w.setMatchType(rs.getString("match_type"));
        w.setMatchValue(rs.getString("match_value"));
        w.setStatus(rs.getString("status"));
        w.setSnoozeUntil(rs.getTimestamp("snooze_until"));
        w.setReminderCount(rs.getInt("reminder_count"));
        w.setReminderIntervalDays(rs.getInt("reminder_interval_days"));
        w.setNotes(rs.getString("notes"));
        w.setResolvedBy(rs.getString("resolved_by"));
        try { w.setResolvedByName(rs.getString("resolver_name")); } catch (SQLException ignored) {}
        w.setResolvedAt(rs.getTimestamp("resolved_at"));
        w.setCreatedAt(rs.getTimestamp("created_at"));
        w.setUpdatedAt(rs.getTimestamp("updated_at"));
        return w;
    }

    private CandidateMergeHistory mapMergeHistoryRow(ResultSet rs) throws SQLException {
        CandidateMergeHistory h = new CandidateMergeHistory();
        h.setId(rs.getString("id"));
        h.setWarningId(rs.getString("warning_id"));
        h.setPrimaryCandidateId(rs.getString("primary_candidate_id"));
        h.setSecondaryCandidateId(rs.getString("secondary_candidate_id"));
        h.setPrimaryCandidateName(rs.getString("primary_candidate_name"));
        h.setSecondaryCandidateName(rs.getString("secondary_candidate_name"));
        h.setMergedByUserId(rs.getString("merged_by_user_id"));
        h.setMergedByUserName(rs.getString("merged_by_user_name"));
        h.setFieldSelectionDetails(rs.getString("field_selection_details"));
        h.setMergedSnapshot(rs.getString("merged_snapshot"));
        h.setMergeNotes(rs.getString("merge_notes"));
        h.setCreatedAt(rs.getTimestamp("created_at"));
        try { h.setUndone(rs.getBoolean("is_undone")); } catch (SQLException ignored) {}
        try { h.setUndoneAt(rs.getTimestamp("undone_at")); } catch (SQLException ignored) {}
        try { h.setUndoneByUserId(rs.getString("undone_by_user_id")); } catch (SQLException ignored) {}
        try { h.setUndoneByUserName(rs.getString("undone_by_user_name")); } catch (SQLException ignored) {}
        try { h.setUndoNotes(rs.getString("undo_notes")); } catch (SQLException ignored) {}
        return h;
    }

    private void ensureTablesExist() {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection();
            DatabaseMetaData meta = conn.getMetaData();
            stmt = conn.createStatement();

            // 1. Kiểm tra cột trong bảng candidates
            addColumnIfNotExists(meta, stmt, "candidates", "normalized_phone", "VARCHAR(50) NULL AFTER phone");
            addColumnIfNotExists(meta, stmt, "candidates", "merged_into_id", "VARCHAR(36) NULL AFTER status");
            addColumnIfNotExists(meta, stmt, "candidates", "is_merged", "BOOLEAN DEFAULT FALSE AFTER merged_into_id");

            // Tự động chuẩn hóa SĐT cho các hồ sơ chưa có normalized_phone
            try {
                stmt.executeUpdate("UPDATE candidates SET normalized_phone = " +
                        "CASE WHEN phone LIKE '+84%' THEN CONCAT('0', SUBSTRING(REPLACE(REPLACE(REPLACE(phone, ' ', ''), '-', ''), '.', ''), 4)) " +
                        "     WHEN phone LIKE '84%' AND LENGTH(phone) >= 11 THEN CONCAT('0', SUBSTRING(REPLACE(REPLACE(REPLACE(phone, ' ', ''), '-', ''), '.', ''), 3)) " +
                        "     ELSE REPLACE(REPLACE(REPLACE(REPLACE(phone, ' ', ''), '-', ''), '.', ''), '+', '') END " +
                        "WHERE (normalized_phone IS NULL OR normalized_phone = '') AND phone IS NOT NULL AND phone != ''");
            } catch (Exception ignored) {}

            // 2. Bảng candidate_duplicate_warnings
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS candidate_duplicate_warnings (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "candidate_id_1 VARCHAR(36) NOT NULL, " +
                    "candidate_id_2 VARCHAR(36) NOT NULL, " +
                    "match_type VARCHAR(20) NOT NULL, " +
                    "match_value VARCHAR(255) NOT NULL, " +
                    "status VARCHAR(20) NOT NULL DEFAULT 'PENDING', " +
                    "snooze_until TIMESTAMP NULL, " +
                    "reminder_count INT NOT NULL DEFAULT 0, " +
                    "reminder_interval_days INT NOT NULL DEFAULT 3, " +
                    "notes TEXT NULL, " +
                    "resolved_by VARCHAR(36) NULL, " +
                    "resolved_at TIMESTAMP NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                    "INDEX idx_cdw_status (status), " +
                    "INDEX idx_cdw_snooze (snooze_until)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

            // 3. Bảng candidate_merge_history
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS candidate_merge_history (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "warning_id VARCHAR(36) NULL, " +
                    "primary_candidate_id VARCHAR(36) NOT NULL, " +
                    "secondary_candidate_id VARCHAR(36) NOT NULL, " +
                    "primary_candidate_name VARCHAR(255) NOT NULL, " +
                    "secondary_candidate_name VARCHAR(255) NOT NULL, " +
                    "merged_by_user_id VARCHAR(36) NULL, " +
                    "merged_by_user_name VARCHAR(255) NULL, " +
                    "field_selection_details LONGTEXT NULL, " +
                    "merged_snapshot LONGTEXT NULL, " +
                    "merge_notes TEXT NULL, " +
                    "is_undone BOOLEAN DEFAULT FALSE, " +
                    "undone_at TIMESTAMP NULL, " +
                    "undone_by_user_id VARCHAR(36) NULL, " +
                    "undone_by_user_name VARCHAR(255) NULL, " +
                    "undo_notes TEXT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "INDEX idx_cmh_primary (primary_candidate_id), " +
                    "INDEX idx_cmh_secondary (secondary_candidate_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

            // Thêm cột cho candidate_merge_history nếu bảng đã tồn tại trước đó
            addColumnIfNotExists(meta, stmt, "candidate_merge_history", "is_undone", "BOOLEAN DEFAULT FALSE AFTER merge_notes");
            addColumnIfNotExists(meta, stmt, "candidate_merge_history", "undone_at", "TIMESTAMP NULL AFTER is_undone");
            addColumnIfNotExists(meta, stmt, "candidate_merge_history", "undone_by_user_id", "VARCHAR(36) NULL AFTER undone_at");
            addColumnIfNotExists(meta, stmt, "candidate_merge_history", "undone_by_user_name", "VARCHAR(255) NULL AFTER undone_by_user_id");
            addColumnIfNotExists(meta, stmt, "candidate_merge_history", "undo_notes", "TEXT NULL AFTER undone_by_user_name");

        } catch (Exception e) {
            logger.log(Level.WARNING, "Không thể tự động khởi tạo bảng duplicate: " + e.getMessage());
        } finally {
            closeStatement(conn, stmt);
        }
    }

    private void addColumnIfNotExists(DatabaseMetaData meta, Statement stmt, String tableName, String columnName, String columnDef) {
        try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            if (!rs.next()) {
                try (ResultSet rsUpper = meta.getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
                    if (!rsUpper.next()) {
                        stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDef);
                    }
                }
            }
        } catch (Exception ignored) {
            try {
                stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDef);
            } catch (SQLException ignored2) {}
        }
    }
}

package com.irms.dao;

import com.irms.model.CandidateActivity;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Thao tác dữ liệu Hoạt động, lịch sử ứng tuyển, phỏng vấn, đánh giá, ghi chú của ứng viên
 */
public class CandidateActivityDAO extends BaseDAO {

    public CandidateActivityDAO() {
        ensureTableExists();
    }

    public boolean insertActivity(CandidateActivity activity) {
        Connection conn = null;
        try {
            conn = getConnection();
            return insertActivity(activity, conn);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi kết nối khi lưu hoạt động ứng viên", e);
            return false;
        } finally {
            close(conn, null);
        }
    }

    public boolean insertActivity(CandidateActivity activity, Connection conn) throws SQLException {
        if (activity == null || activity.getCandidateId() == null) return false;

        String sql = "INSERT INTO candidate_activities (id, candidate_id, activity_type, title, description, " +
                     "performed_by, performed_by_name, original_candidate_id, metadata_json, activity_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, activity.getId());
            ps.setString(2, activity.getCandidateId());
            ps.setString(3, activity.getActivityType() != null ? activity.getActivityType() : "NOTE");
            ps.setString(4, activity.getTitle() != null ? activity.getTitle() : "Hoạt động");
            ps.setString(5, activity.getDescription());
            ps.setString(6, activity.getPerformedBy());
            ps.setString(7, activity.getPerformedByName());
            ps.setString(8, activity.getOriginalCandidateId());
            ps.setString(9, activity.getMetadataJson());
            ps.setTimestamp(10, activity.getActivityDate() != null
                    ? activity.getActivityDate() : new java.sql.Timestamp(System.currentTimeMillis()));
            return ps.executeUpdate() > 0;
        }
    }

    public List<CandidateActivity> findByCandidateId(String candidateId) {
        List<CandidateActivity> list = new ArrayList<>();
        if (candidateId == null || candidateId.trim().isEmpty()) return list;

        String sql = "SELECT a.*, u.full_name AS user_full_name FROM candidate_activities a " +
                     "LEFT JOIN users u ON a.performed_by = u.id " +
                     "WHERE a.candidate_id = ? " +
                     "ORDER BY a.activity_date DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, candidateId.trim());
            rs = ps.executeQuery();
            while (rs.next()) {
                CandidateActivity act = new CandidateActivity();
                act.setId(rs.getString("id"));
                act.setCandidateId(rs.getString("candidate_id"));
                act.setActivityType(rs.getString("activity_type"));
                act.setTitle(rs.getString("title"));
                act.setDescription(rs.getString("description"));
                act.setPerformedBy(rs.getString("performed_by"));
                String pName = rs.getString("performed_by_name");
                if (pName == null || pName.trim().isEmpty()) {
                    pName = rs.getString("user_full_name");
                }
                act.setPerformedByName(pName != null ? pName : "Hệ thống");
                act.setOriginalCandidateId(rs.getString("original_candidate_id"));
                act.setMetadataJson(rs.getString("metadata_json"));
                act.setActivityDate(rs.getTimestamp("activity_date"));
                list.add(act);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách hoạt động ứng viên: " + candidateId, e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Chuyển toàn bộ hoạt động từ hồ sơ bị hợp nhất sang hồ sơ chính (bảo toàn lịch sử)
     */
    public int transferActivities(String fromCandidateId, String toCandidateId, Connection conn) throws SQLException {
        if (fromCandidateId == null || toCandidateId == null) return 0;

        String sql = "UPDATE candidate_activities SET candidate_id = ?, " +
                     "original_candidate_id = CASE WHEN original_candidate_id IS NULL THEN ? ELSE original_candidate_id END " +
                     "WHERE candidate_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, toCandidateId.trim());
            ps.setString(2, fromCandidateId.trim());
            ps.setString(3, fromCandidateId.trim());
            return ps.executeUpdate();
        }
    }

    /**
     * Khôi phục hoạt động về hồ sơ phụ ban đầu khi hoàn tác hợp nhất
     */
    public int restoreActivities(String secondaryCandidateId, String primaryCandidateId, Connection conn) throws SQLException {
        if (secondaryCandidateId == null || primaryCandidateId == null) return 0;

        String sql = "UPDATE candidate_activities SET candidate_id = ? " +
                     "WHERE candidate_id = ? AND original_candidate_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, secondaryCandidateId.trim());
            ps.setString(2, primaryCandidateId.trim());
            ps.setString(3, secondaryCandidateId.trim());
            return ps.executeUpdate();
        }
    }

    private void ensureTableExists() {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection();
            DatabaseMetaData meta = conn.getMetaData();
            boolean exists = false;
            try (ResultSet rs = meta.getTables(null, null, "candidate_activities", null)) {
                if (rs.next()) exists = true;
            }
            if (!exists) {
                try (ResultSet rsUpper = meta.getTables(null, null, "CANDIDATE_ACTIVITIES", null)) {
                    if (rsUpper.next()) exists = true;
                }
            }
            if (!exists) {
                stmt = conn.createStatement();
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS candidate_activities (" +
                        "id VARCHAR(36) PRIMARY KEY, " +
                        "candidate_id VARCHAR(36) NOT NULL, " +
                        "activity_type VARCHAR(50) NOT NULL, " +
                        "title VARCHAR(255) NOT NULL, " +
                        "description TEXT NULL, " +
                        "performed_by VARCHAR(36) NULL, " +
                        "performed_by_name VARCHAR(255) NULL, " +
                        "original_candidate_id VARCHAR(36) NULL, " +
                        "metadata_json LONGTEXT NULL, " +
                        "activity_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                        "INDEX idx_ca_candidate (candidate_id), " +
                        "INDEX idx_ca_type (activity_type), " +
                        "INDEX idx_ca_date (activity_date)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Không thể tự động khởi tạo bảng candidate_activities: " + e.getMessage());
        } finally {
            closeStatement(conn, stmt);
        }
    }
}

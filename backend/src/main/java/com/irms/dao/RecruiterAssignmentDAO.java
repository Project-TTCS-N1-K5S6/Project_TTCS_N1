package com.irms.dao;

import com.irms.model.RecruiterAssignment;
import com.irms.model.RecruiterAssignmentHistory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * DAO cho tính năng KN-116: Quản lý Phân bổ Hồ sơ Tuyển dụng
 * - Phân công recruiter chính và recruiter hỗ trợ
 * - Truy vấn danh sách recruiter của một vị trí
 * - Ghi và đọc lịch sử chuyển giao recruiter chính
 */
public class RecruiterAssignmentDAO extends BaseDAO {

    public RecruiterAssignmentDAO() {
        ensureSchema();
    }

    /**
     * Tự động tạo bảng nếu chưa tồn tại (tương thích DB cũ)
     */
    private void ensureSchema() {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.createStatement();

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS requisition_recruiters (" +
                "  id VARCHAR(36) PRIMARY KEY," +
                "  requisition_id VARCHAR(36) NOT NULL," +
                "  recruiter_id VARCHAR(36) NOT NULL," +
                "  role VARCHAR(20) NOT NULL DEFAULT 'SUPPORT'," +
                "  assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  assigned_by VARCHAR(36) NULL," +
                "  CONSTRAINT fk_rr_req FOREIGN KEY (requisition_id) REFERENCES recruitment_requisitions(id) ON DELETE CASCADE," +
                "  CONSTRAINT fk_rr_rec FOREIGN KEY (recruiter_id) REFERENCES users(id) ON DELETE CASCADE," +
                "  CONSTRAINT uq_rr_req_rec UNIQUE (requisition_id, recruiter_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );

            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS recruiter_assignment_history (" +
                "  id VARCHAR(36) PRIMARY KEY," +
                "  requisition_id VARCHAR(36) NOT NULL," +
                "  requisition_code VARCHAR(50) NULL," +
                "  old_recruiter_id VARCHAR(36) NULL," +
                "  old_recruiter_name VARCHAR(255) NULL," +
                "  new_recruiter_id VARCHAR(36) NULL," +
                "  new_recruiter_name VARCHAR(255) NULL," +
                "  change_reason TEXT NULL," +
                "  changed_by VARCHAR(36) NULL," +
                "  changed_by_name VARCHAR(255) NULL," +
                "  changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  CONSTRAINT fk_rah_req FOREIGN KEY (requisition_id) REFERENCES recruitment_requisitions(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );

        } catch (SQLException e) {
            logger.log(Level.WARNING, "Không thể khởi tạo schema recruiter_assignment: " + e.getMessage());
        } finally {
            if (stmt != null) { try { stmt.close(); } catch (SQLException ignored) {} }
            if (conn != null) { try { conn.close(); } catch (SQLException ignored) {} }
        }
    }

    // ================================================================
    // QUẢN LÝ RECRUITER HỖ TRỢ (bảng requisition_recruiters)
    // ================================================================

    /**
     * Lấy danh sách tất cả recruiter (chính + hỗ trợ) được phân công cho một vị trí
     */
    public List<RecruiterAssignment> findByRequisitionId(String requisitionId) {
        List<RecruiterAssignment> list = new ArrayList<>();
        String sql = "SELECT rr.*, u.full_name AS recruiter_name, u.email AS recruiter_email, " +
                     "u.job_title AS recruiter_job_title, " +
                     "ab.full_name AS assigned_by_name, " +
                     "req.code AS requisition_code " +
                     "FROM requisition_recruiters rr " +
                     "INNER JOIN users u ON rr.recruiter_id = u.id " +
                     "LEFT JOIN users ab ON rr.assigned_by = ab.id " +
                     "LEFT JOIN recruitment_requisitions req ON rr.requisition_id = req.id " +
                     "WHERE rr.requisition_id = ? " +
                     "ORDER BY FIELD(rr.role, 'PRIMARY', 'SUPPORT'), rr.assigned_at";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapAssignment(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách recruiter theo requisition: " + requisitionId, e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Lấy danh sách tất cả requisition_id mà một recruiter được phân công (chính hoặc hỗ trợ)
     * Dùng để lọc ứng viên: recruiter chỉ nhìn thấy ứng viên của vị trí được giao
     */
    public List<String> findRequisitionIdsByRecruiterId(String recruiterId) {
        List<String> ids = new ArrayList<>();
        // Lấy từ bảng phân công hỗ trợ
        String sql1 = "SELECT requisition_id FROM requisition_recruiters WHERE recruiter_id = ?";
        // Lấy từ cột recruiter_id chính trên recruitment_requisitions
        String sql2 = "SELECT id FROM recruitment_requisitions WHERE recruiter_id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            // Truy vấn vị trí từ bảng hỗ trợ
            ps = conn.prepareStatement(sql1);
            ps.setString(1, recruiterId);
            rs = ps.executeQuery();
            while (rs.next()) {
                String id = rs.getString("requisition_id");
                if (!ids.contains(id)) ids.add(id);
            }
            rs.close(); ps.close();

            // Truy vấn vị trí từ recruiter chính
            ps = conn.prepareStatement(sql2);
            ps.setString(1, recruiterId);
            rs = ps.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                if (!ids.contains(id)) ids.add(id);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách requisition của recruiter: " + recruiterId, e);
        } finally {
            close(conn, ps, rs);
        }
        return ids;
    }

    /**
     * Thêm recruiter hỗ trợ vào một vị trí tuyển dụng
     */
    public boolean addSupportRecruiter(String requisitionId, String recruiterId, String assignedById) {
        String sql = "INSERT INTO requisition_recruiters (id, requisition_id, recruiter_id, role, assigned_by) " +
                     "VALUES (?, ?, ?, 'SUPPORT', ?) " +
                     "ON DUPLICATE KEY UPDATE role = 'SUPPORT', assigned_by = ?, assigned_at = CURRENT_TIMESTAMP";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, java.util.UUID.randomUUID().toString());
            ps.setString(2, requisitionId);
            ps.setString(3, recruiterId);
            ps.setString(4, assignedById);
            ps.setString(5, assignedById);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi thêm recruiter hỗ trợ", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Xóa recruiter hỗ trợ khỏi một vị trí tuyển dụng
     */
    public boolean removeSupportRecruiter(String requisitionId, String recruiterId) {
        String sql = "DELETE FROM requisition_recruiters WHERE requisition_id = ? AND recruiter_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            ps.setString(2, recruiterId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi xóa recruiter hỗ trợ", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Xóa toàn bộ recruiter hỗ trợ của một vị trí (dùng khi cập nhật lại danh sách)
     */
    public boolean clearSupportRecruiters(String requisitionId) {
        String sql = "DELETE FROM requisition_recruiters WHERE requisition_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi xóa danh sách recruiter hỗ trợ", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Kiểm tra recruiter có được giao vị trí tuyển dụng này không
     * (chính hoặc hỗ trợ)
     */
    public boolean isRecruiterAssigned(String requisitionId, String recruiterId) {
        // Kiểm tra bảng hỗ trợ
        String sql1 = "SELECT 1 FROM requisition_recruiters WHERE requisition_id = ? AND recruiter_id = ? LIMIT 1";
        // Kiểm tra recruiter chính
        String sql2 = "SELECT 1 FROM recruitment_requisitions WHERE id = ? AND recruiter_id = ? LIMIT 1";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql1);
            ps.setString(1, requisitionId);
            ps.setString(2, recruiterId);
            rs = ps.executeQuery();
            if (rs.next()) return true;
            rs.close(); ps.close();

            ps = conn.prepareStatement(sql2);
            ps.setString(1, requisitionId);
            ps.setString(2, recruiterId);
            rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi kiểm tra phân công recruiter", e);
            return false;
        } finally {
            close(conn, ps, rs);
        }
    }

    // ================================================================
    // LỊCH SỬ CHUYỂN GIAO (bảng recruiter_assignment_history)
    // ================================================================

    /**
     * Ghi lịch sử chuyển giao recruiter chính
     */
    public boolean insertHistory(RecruiterAssignmentHistory history) {
        String sql = "INSERT INTO recruiter_assignment_history " +
                     "(id, requisition_id, requisition_code, old_recruiter_id, old_recruiter_name, " +
                     "new_recruiter_id, new_recruiter_name, change_reason, changed_by, changed_by_name) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, history.getId());
            ps.setString(2, history.getRequisitionId());
            ps.setString(3, history.getRequisitionCode());
            ps.setString(4, history.getOldRecruiterId());
            ps.setString(5, history.getOldRecruiterName());
            ps.setString(6, history.getNewRecruiterId());
            ps.setString(7, history.getNewRecruiterName());
            ps.setString(8, history.getChangeReason());
            ps.setString(9, history.getChangedBy());
            ps.setString(10, history.getChangedByName());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi ghi lịch sử chuyển giao recruiter", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Lấy lịch sử chuyển giao của một vị trí tuyển dụng
     */
    public List<RecruiterAssignmentHistory> findHistoryByRequisitionId(String requisitionId) {
        List<RecruiterAssignmentHistory> list = new ArrayList<>();
        String sql = "SELECT * FROM recruiter_assignment_history " +
                     "WHERE requisition_id = ? " +
                     "ORDER BY changed_at DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapHistory(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy lịch sử chuyển giao recruiter: " + requisitionId, e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    // ================================================================
    // MAPPING
    // ================================================================

    private RecruiterAssignment mapAssignment(ResultSet rs) throws SQLException {
        RecruiterAssignment a = new RecruiterAssignment();
        a.setId(rs.getString("id"));
        a.setRequisitionId(rs.getString("requisition_id"));
        try { a.setRequisitionCode(rs.getString("requisition_code")); } catch (SQLException ignored) {}
        a.setRecruiterId(rs.getString("recruiter_id"));
        a.setRecruiterName(rs.getString("recruiter_name"));
        try { a.setRecruiterEmail(rs.getString("recruiter_email")); } catch (SQLException ignored) {}
        try { a.setRecruiterJobTitle(rs.getString("recruiter_job_title")); } catch (SQLException ignored) {}
        a.setRole(rs.getString("role"));
        a.setAssignedAt(rs.getTimestamp("assigned_at"));
        try { a.setAssignedBy(rs.getString("assigned_by")); } catch (SQLException ignored) {}
        try { a.setAssignedByName(rs.getString("assigned_by_name")); } catch (SQLException ignored) {}
        return a;
    }

    private RecruiterAssignmentHistory mapHistory(ResultSet rs) throws SQLException {
        RecruiterAssignmentHistory h = new RecruiterAssignmentHistory();
        h.setId(rs.getString("id"));
        h.setRequisitionId(rs.getString("requisition_id"));
        try { h.setRequisitionCode(rs.getString("requisition_code")); } catch (SQLException ignored) {}
        try { h.setOldRecruiterId(rs.getString("old_recruiter_id")); } catch (SQLException ignored) {}
        try { h.setOldRecruiterName(rs.getString("old_recruiter_name")); } catch (SQLException ignored) {}
        try { h.setNewRecruiterId(rs.getString("new_recruiter_id")); } catch (SQLException ignored) {}
        try { h.setNewRecruiterName(rs.getString("new_recruiter_name")); } catch (SQLException ignored) {}
        try { h.setChangeReason(rs.getString("change_reason")); } catch (SQLException ignored) {}
        try { h.setChangedBy(rs.getString("changed_by")); } catch (SQLException ignored) {}
        try { h.setChangedByName(rs.getString("changed_by_name")); } catch (SQLException ignored) {}
        h.setChangedAt(rs.getTimestamp("changed_at"));
        return h;
    }
}

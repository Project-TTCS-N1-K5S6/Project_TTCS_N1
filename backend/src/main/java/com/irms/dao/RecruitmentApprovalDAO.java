package com.irms.dao;

import com.irms.model.RecruitmentApprovalLog;
import com.irms.util.SecurityUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Tầng truy vấn CSDL cho Nhật ký & Chuỗi cấp duyệt (recruitment_approval_logs)
 * Tuân thủ nghiêm ngặt các quy tắc:
 * - KN-154: Ghi nhận kết quả phê duyệt theo từng cấp
 * - KN-155: Khóa lịch sử phê duyệt không cho sửa hoặc xóa (Immutability Lock)
 * - KN-156: Mô hình lưu trữ và liên kết
 */
public class RecruitmentApprovalDAO extends BaseDAO {

    public RecruitmentApprovalDAO() {
        ensureSchema();
    }

    /**
     * Tự động khởi tạo bảng recruitment_approval_logs nếu chưa tồn tại
     */
    private void ensureSchema() {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS recruitment_approval_logs (" +
                         "id VARCHAR(36) PRIMARY KEY, " +
                         "requisition_id VARCHAR(36) NOT NULL, " +
                         "step_order INT NOT NULL, " +
                         "step_name VARCHAR(150) NOT NULL, " +
                         "required_role VARCHAR(50) NOT NULL, " +
                         "assigned_user_id VARCHAR(36) NULL, " +
                         "approver_id VARCHAR(36) NULL, " +
                         "status VARCHAR(50) NOT NULL DEFAULT 'WAITING', " +
                         "decision_date TIMESTAMP NULL, " +
                         "comment TEXT NULL, " +
                         "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                         "INDEX idx_appr_req_order (requisition_id, step_order), " +
                         "INDEX idx_appr_status (status)" +
                         ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Không thể khởi tạo schema recruitment_approval_logs: " + e.getMessage());
        } finally {
            closeStatement(conn, stmt);
        }
    }

    /**
     * Lấy danh sách toàn bộ các bước trong chuỗi duyệt của một hồ sơ
     */
    public List<RecruitmentApprovalLog> findByRequisitionId(String requisitionId) {
        List<RecruitmentApprovalLog> list = new ArrayList<>();
        String sql = "SELECT a.*, " +
                     "u_appr.full_name AS approver_name, u_appr.job_title AS approver_job_title, u_appr.email AS approver_email, " +
                     "u_asgn.full_name AS assigned_user_name " +
                     "FROM recruitment_approval_logs a " +
                     "LEFT JOIN users u_appr ON a.approver_id = u_appr.id " +
                     "LEFT JOIN users u_asgn ON a.assigned_user_id = u_asgn.id " +
                     "WHERE a.requisition_id = ? " +
                     "ORDER BY a.step_order ASC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToModel(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy chuỗi duyệt cho yêu cầu: " + requisitionId, e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Lấy thông tin một bước duyệt theo ID
     */
    public RecruitmentApprovalLog findById(String id) {
        String sql = "SELECT a.*, " +
                     "u_appr.full_name AS approver_name, u_appr.job_title AS approver_job_title, u_appr.email AS approver_email, " +
                     "u_asgn.full_name AS assigned_user_name " +
                     "FROM recruitment_approval_logs a " +
                     "LEFT JOIN users u_appr ON a.approver_id = u_appr.id " +
                     "LEFT JOIN users u_asgn ON a.assigned_user_id = u_asgn.id " +
                     "WHERE a.id = ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToModel(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm bước duyệt ID: " + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * Lấy bước hiện tại đang ở trạng thái PENDING (chờ duyệt)
     */
    public RecruitmentApprovalLog getCurrentPendingStep(String requisitionId) {
        String sql = "SELECT a.*, " +
                     "u_appr.full_name AS approver_name, u_appr.job_title AS approver_job_title, u_appr.email AS approver_email, " +
                     "u_asgn.full_name AS assigned_user_name " +
                     "FROM recruitment_approval_logs a " +
                     "LEFT JOIN users u_appr ON a.approver_id = u_appr.id " +
                     "LEFT JOIN users u_asgn ON a.assigned_user_id = u_asgn.id " +
                     "WHERE a.requisition_id = ? AND a.status = 'PENDING' " +
                     "ORDER BY a.step_order ASC LIMIT 1";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToModel(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy bước chờ duyệt hiện tại: " + requisitionId, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * Đếm số bước đã tồn tại của một yêu cầu
     */
    public int countSteps(String requisitionId) {
        String sql = "SELECT COUNT(*) FROM recruitment_approval_logs WHERE requisition_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đếm số bước duyệt", e);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    /**
     * Khởi tạo chuỗi phê duyệt 3 cấp chuẩn cho hồ sơ tuyển dụng
     * Cấp 1: Trưởng bộ phận chuyên môn (HIRING_MANAGER) - PENDING
     * Cấp 2: Trưởng phòng Nhân sự (HR_MANAGER) - WAITING
     * Cấp 3: Ban Giám đốc (APPROVER) - WAITING
     */
    public boolean initApprovalChain(String requisitionId, String hiringManagerId) {
        if (countSteps(requisitionId) > 0) {
            return true; // Đã khởi tạo trước đó
        }

        String sql = "INSERT INTO recruitment_approval_logs " +
                     "(id, requisition_id, step_order, step_name, required_role, assigned_user_id, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            ps = conn.prepareStatement(sql);

            // Cấp 1
            ps.setString(1, SecurityUtil.generateUUID());
            ps.setString(2, requisitionId);
            ps.setInt(3, 1);
            ps.setString(4, "Cấp 1: Trưởng bộ phận chuyên môn");
            ps.setString(5, "HIRING_MANAGER");
            ps.setString(6, hiringManagerId);
            ps.setString(7, "PENDING");
            ps.addBatch();

            // Cấp 2
            ps.setString(1, SecurityUtil.generateUUID());
            ps.setString(2, requisitionId);
            ps.setInt(3, 2);
            ps.setString(4, "Cấp 2: Trưởng phòng Nhân sự");
            ps.setString(5, "HR_MANAGER");
            ps.setString(6, null);
            ps.setString(7, "WAITING");
            ps.addBatch();

            // Cấp 3
            ps.setString(1, SecurityUtil.generateUUID());
            ps.setString(2, requisitionId);
            ps.setInt(3, 3);
            ps.setString(4, "Cấp 3: Ban Giám đốc");
            ps.setString(5, "APPROVER");
            ps.setString(6, null);
            ps.setString(7, "WAITING");
            ps.addBatch();

            ps.executeBatch();
            conn.commit();
            return true;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi khởi tạo chuỗi phê duyệt cho yêu cầu: " + requisitionId, e);
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
            }
            close(conn, ps);
        }
    }

    /**
     * KN-154 & KN-155: Ghi nhận kết quả phê duyệt và KHÓA LỊCH SỬ KHÔNG CHO SỬA HOẶC XÓA
     * Nếu bước đã APPROVED hoặc REJECTED, ném ra ngoại lệ ngăn chặn sửa đổi.
     */
    public boolean recordApprovalDecision(String stepId, String approverId, String status, String comment) {
        // Kiểm tra tính bất biến (Immutability check - KN-155)
        RecruitmentApprovalLog existing = findById(stepId);
        if (existing == null) {
            throw new IllegalArgumentException("Không tìm thấy bước phê duyệt ID: " + stepId);
        }

        if (existing.isApproved() || existing.isRejected()) {
            throw new IllegalStateException("LỊCH SỬ PHÊ DUYỆT ĐÃ KHÓA: Bước này đã có quyết định (" + 
                    existing.getStatusLabel() + ") và không thể chỉnh sửa hoặc xóa theo quy định kiểm toán!");
        }

        if (!"APPROVED".equalsIgnoreCase(status) && !"REJECTED".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Trạng thái quyết định không hợp lệ: " + status + " (Chỉ chấp nhận APPROVED hoặc REJECTED)");
        }

        String sql = "UPDATE recruitment_approval_logs SET " +
                     "status = ?, approver_id = ?, decision_date = CURRENT_TIMESTAMP, comment = ? " +
                     "WHERE id = ? AND status = 'PENDING'";

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status.toUpperCase());
            ps.setString(2, approverId);
            ps.setString(3, comment);
            ps.setString(4, stepId);

            int updated = ps.executeUpdate();
            return updated > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi ghi nhận quyết định phê duyệt: " + stepId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Kích hoạt bước tiếp theo chuyển từ WAITING sang PENDING
     */
    public boolean activateNextStep(String requisitionId, int currentStepOrder) {
        String sql = "UPDATE recruitment_approval_logs SET status = 'PENDING' " +
                     "WHERE requisition_id = ? AND step_order = ? AND status = 'WAITING'";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            ps.setInt(2, currentStepOrder + 1);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi kích hoạt cấp duyệt tiếp theo", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Khi bị từ chối ở một cấp, các cấp sau chuyển sang SKIPPED (Bỏ qua)
     */
    public boolean skipSubsequentSteps(String requisitionId, int fromStepOrder) {
        String sql = "UPDATE recruitment_approval_logs SET status = 'SKIPPED' " +
                     "WHERE requisition_id = ? AND step_order > ? AND status = 'WAITING'";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            ps.setInt(2, fromStepOrder);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi chuyển trạng thái bỏ qua các cấp sau", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * KN-155: Chặn xóa lịch sử phê duyệt đã ghi nhận
     */
    public boolean deleteApprovalChain(String requisitionId) {
        // Kiểm tra xem đã có bước nào APPROVED hoặc REJECTED chưa
        List<RecruitmentApprovalLog> steps = findByRequisitionId(requisitionId);
        for (RecruitmentApprovalLog s : steps) {
            if (s.isApproved() || s.isRejected()) {
                throw new UnsupportedOperationException("LỊCH SỬ PHÊ DUYỆT ĐÃ KHÓA: Hồ sơ đã phát sinh nhật ký phê duyệt thực tế, tuyệt đối không được xóa!");
            }
        }

        String sql = "DELETE FROM recruitment_approval_logs WHERE requisition_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi xóa chuỗi phê duyệt nháp", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    private RecruitmentApprovalLog mapResultSetToModel(ResultSet rs) throws SQLException {
        RecruitmentApprovalLog log = new RecruitmentApprovalLog();
        log.setId(rs.getString("id"));
        log.setRequisitionId(rs.getString("requisition_id"));
        log.setStepOrder(rs.getInt("step_order"));
        log.setStepName(rs.getString("step_name"));
        log.setRequiredRole(rs.getString("required_role"));
        log.setAssignedUserId(rs.getString("assigned_user_id"));
        try { log.setAssignedUserName(rs.getString("assigned_user_name")); } catch (SQLException ignored) {}

        log.setApproverId(rs.getString("approver_id"));
        try { log.setApproverName(rs.getString("approver_name")); } catch (SQLException ignored) {}
        try { log.setApproverJobTitle(rs.getString("approver_job_title")); } catch (SQLException ignored) {}
        try { log.setApproverEmail(rs.getString("approver_email")); } catch (SQLException ignored) {}

        log.setStatus(rs.getString("status"));
        log.setDecisionDate(rs.getTimestamp("decision_date"));
        log.setComment(rs.getString("comment"));
        log.setCreatedAt(rs.getTimestamp("created_at"));
        return log;
    }

    public java.util.Map<String, String> findUserBasicInfo(String userId) {
        if (userId == null || userId.trim().isEmpty()) return null;
        String sql = "SELECT u.id, u.full_name, u.email, u.job_title, d.name AS department_name " +
                     "FROM users u " +
                     "LEFT JOIN departments d ON u.department_id = d.id " +
                     "WHERE u.id = ? AND u.status = 'ACTIVE'";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, userId.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                java.util.Map<String, String> map = new java.util.HashMap<>();
                map.put("id", rs.getString("id"));
                map.put("fullName", rs.getString("full_name"));
                map.put("email", rs.getString("email"));
                map.put("jobTitle", rs.getString("job_title"));
                map.put("departmentName", rs.getString("department_name"));
                return map;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy thông tin người dùng: " + userId, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public java.util.List<java.util.Map<String, String>> findEligibleApprovers(String requiredRole, String assignedUserId, String hiringManagerId, int stepOrder) {
        java.util.List<java.util.Map<String, String>> list = new java.util.ArrayList<>();

        if (assignedUserId != null && !assignedUserId.trim().isEmpty()) {
            java.util.Map<String, String> specific = findUserBasicInfo(assignedUserId);
            if (specific != null) {
                list.add(specific);
                return list;
            }
        }

        if (stepOrder == 1 && hiringManagerId != null && !hiringManagerId.trim().isEmpty()) {
            java.util.Map<String, String> mgr = findUserBasicInfo(hiringManagerId);
            if (mgr != null) {
                list.add(mgr);
                return list;
            }
        }

        String sql = "SELECT DISTINCT u.id, u.full_name, u.email, u.job_title, d.name AS department_name " +
                     "FROM users u " +
                     "INNER JOIN user_roles ur ON u.id = ur.user_id " +
                     "INNER JOIN roles r ON ur.role_id = r.id " +
                     "LEFT JOIN departments d ON u.department_id = d.id " +
                     "WHERE u.status = 'ACTIVE' AND (r.code = ? OR r.code = 'ADMIN') " +
                     "ORDER BY (r.code = ?) DESC, u.full_name ASC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requiredRole);
            ps.setString(2, requiredRole);
            rs = ps.executeQuery();
            while (rs.next()) {
                java.util.Map<String, String> map = new java.util.HashMap<>();
                map.put("id", rs.getString("id"));
                map.put("fullName", rs.getString("full_name"));
                map.put("email", rs.getString("email"));
                map.put("jobTitle", rs.getString("job_title"));
                map.put("departmentName", rs.getString("department_name"));
                list.add(map);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm danh sách người duyệt cho vai trò: " + requiredRole, e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }
}


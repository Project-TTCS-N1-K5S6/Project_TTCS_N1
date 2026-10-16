package com.irms.dao;

import com.irms.model.Candidate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Thao tác truy vấn và quản lý Hồ sơ Ứng viên (bảng candidates)
 */
public class CandidateDAO extends BaseDAO {

    public List<Candidate> findAll(String search, String status) {
        return findAll(search, status, null);
    }

    /**
     * [US 5]: Tra cứu danh sách ứng viên, hỗ trợ ràng buộc quyền sở hữu vị trí tuyển dụng của Recruiter.
     * - Nếu recruiterId != null: Chỉ trả về ứng viên nộp vào các vị trí do recruiter này phụ trách (r.recruiter_id = ?).
     * - Nếu recruiterId == null (Admin / HR Manager): Xem toàn bộ ứng viên của hệ thống.
     */
    public List<Candidate> findAll(String search, String status, String recruiterId) {
        return findAll(search, status, recruiterId, 0, Integer.MAX_VALUE);
    }

    public List<Candidate> findAll(String search, String status, String recruiterId, int offset, int limit) {
        List<Candidate> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT c.*, r.title AS requisition_title, r.department_id, d.name AS department_name, " +
                "source.value AS application_source_name, reason.value AS rejection_reason_name, " +
                "r.recruiter_id, u_rec.full_name AS recruiter_name, u_rec.email AS recruiter_email, " +
                "COALESCE(sla.sla_days, 3) AS sla_days_threshold, " +
                "DATEDIFF(CURRENT_TIMESTAMP, COALESCE(c.stage_entered_at, c.created_at)) AS days_in_stage " +
                "FROM candidates c " +
                "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                "LEFT JOIN users u_rec ON r.recruiter_id = u_rec.id " +
                "LEFT JOIN departments d ON r.department_id = d.id " +
                "LEFT JOIN shared_catalogs source ON c.application_source_id = source.id " +
                "LEFT JOIN shared_catalogs reason ON c.rejection_reason_id = reason.id " +
                "LEFT JOIN sla_configurations sla ON c.status = sla.stage_code WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (recruiterId != null && !recruiterId.trim().isEmpty()) {
            sql.append("AND (r.recruiter_id = ? OR c.requisition_id IS NULL) ");
            params.add(recruiterId.trim());
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (c.full_name LIKE ? OR c.email LIKE ? OR c.phone LIKE ?) ");
            String kw = "%" + search.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND c.status = ? ");
            params.add(status.trim());
        }

        sql.append("ORDER BY c.created_at DESC");
        if (limit < Integer.MAX_VALUE) {
            sql.append(" LIMIT ? OFFSET ?");
            params.add(limit);
            params.add(offset);
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
            while (rs.next()) {
                Candidate c = mapResultSetToCandidate(rs);
                list.add(c);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách ứng viên", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public int countAll(String search, String status, String recruiterId) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM candidates c " +
                "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (recruiterId != null && !recruiterId.trim().isEmpty()) {
            sql.append("AND (r.recruiter_id = ? OR c.requisition_id IS NULL) ");
            params.add(recruiterId.trim());
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (c.full_name LIKE ? OR c.email LIKE ? OR c.phone LIKE ?) ");
            String kw = "%" + search.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND c.status = ? ");
            params.add(status.trim());
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
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đếm số lượng ứng viên", e);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    public boolean insert(Candidate c) {
        String sql = "INSERT INTO candidates (id, requisition_id, application_source_id, rejection_reason_id, " +
                     "full_name, email, phone, status, cv_url, notes, is_sla_overdue, stage_entered_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, FALSE, CURRENT_TIMESTAMP)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, c.getId());
            ps.setString(2, c.getRequisitionId());
            ps.setString(3, c.getApplicationSourceId());
            ps.setString(4, c.getRejectionReasonId());
            ps.setString(5, c.getFullName());
            ps.setString(6, c.getEmail());
            ps.setString(7, c.getPhone());
            ps.setString(8, c.getStatus() != null ? c.getStatus() : "APPLIED");
            ps.setString(9, c.getCvUrl());
            ps.setString(10, c.getNotes());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi thêm ứng viên mới", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    public boolean updateStatus(String candidateId, String newStatus) {
        return updateStatus(candidateId, newStatus, null);
    }

    public boolean updateStatus(String candidateId, String newStatus, String rejectionReasonId) {
        // [KN-363]: Khi chuyển vòng / đổi trạng thái, làm mới lại mốc thời gian giai đoạn và xóa cờ quá hạn SLA
        String sql = "UPDATE candidates SET status = ?, rejection_reason_id = ?, " +
                     "stage_entered_at = CURRENT_TIMESTAMP, is_sla_overdue = FALSE, " +
                     "sla_overdue_detected_at = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newStatus);
            ps.setString(2, rejectionReasonId);
            ps.setString(3, candidateId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật trạng thái ứng viên", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * [KN-363]: Gắn cờ hồ sơ quá hạn và lưu thời điểm phát hiện SLA
     */
    public boolean markOverdue(String candidateId, Timestamp detectedAt) {
        String sql = "UPDATE candidates SET is_sla_overdue = TRUE, " +
                     "sla_overdue_detected_at = COALESCE(sla_overdue_detected_at, ?) WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, detectedAt != null ? detectedAt : new Timestamp(System.currentTimeMillis()));
            ps.setString(2, candidateId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi gắn cờ quá hạn SLA cho ứng viên: " + candidateId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * [KN-363]: Lấy danh sách hồ sơ đang bị quá hạn SLA
     */
    public List<Candidate> findOverdueCandidates(String recruiterId) {
        List<Candidate> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT c.*, r.title AS requisition_title, r.department_id, d.name AS department_name, " +
                "source.value AS application_source_name, reason.value AS rejection_reason_name, " +
                "r.recruiter_id, u_rec.full_name AS recruiter_name, u_rec.email AS recruiter_email, " +
                "COALESCE(sla.sla_days, 3) AS sla_days_threshold, " +
                "DATEDIFF(CURRENT_TIMESTAMP, COALESCE(c.stage_entered_at, c.created_at)) AS days_in_stage " +
                "FROM candidates c " +
                "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                "LEFT JOIN users u_rec ON r.recruiter_id = u_rec.id " +
                "LEFT JOIN departments d ON r.department_id = d.id " +
                "LEFT JOIN shared_catalogs source ON c.application_source_id = source.id " +
                "LEFT JOIN shared_catalogs reason ON c.rejection_reason_id = reason.id " +
                "LEFT JOIN sla_configurations sla ON c.status = sla.stage_code " +
                "WHERE c.is_sla_overdue = TRUE AND c.status NOT IN ('HIRED', 'REJECTED') ");

        if (recruiterId != null && !recruiterId.trim().isEmpty()) {
            sql.append("AND (r.recruiter_id = ? OR c.requisition_id IS NULL) ");
        }
        sql.append("ORDER BY c.sla_overdue_detected_at DESC, days_in_stage DESC");

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql.toString());
            if (recruiterId != null && !recruiterId.trim().isEmpty()) {
                ps.setString(1, recruiterId.trim());
            }
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToCandidate(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi truy vấn hồ sơ quá hạn SLA", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public Candidate findById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        String sql = "SELECT c.*, r.title AS requisition_title, r.department_id, d.name AS department_name, " +
                     "source.value AS application_source_name, reason.value AS rejection_reason_name, " +
                     "r.recruiter_id, u_rec.full_name AS recruiter_name, u_rec.email AS recruiter_email, " +
                     "COALESCE(sla.sla_days, 3) AS sla_days_threshold, " +
                     "DATEDIFF(CURRENT_TIMESTAMP, COALESCE(c.stage_entered_at, c.created_at)) AS days_in_stage " +
                     "FROM candidates c " +
                     "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                     "LEFT JOIN users u_rec ON r.recruiter_id = u_rec.id " +
                     "LEFT JOIN departments d ON r.department_id = d.id " +
                     "LEFT JOIN shared_catalogs source ON c.application_source_id = source.id " +
                     "LEFT JOIN shared_catalogs reason ON c.rejection_reason_id = reason.id " +
                     "LEFT JOIN sla_configurations sla ON c.status = sla.stage_code " +
                     "WHERE c.id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToCandidate(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy thông tin ứng viên theo ID: " + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public int countTotal() {
        String sql = "SELECT COUNT(*) FROM candidates";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đếm số ứng viên", e);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    private Candidate mapResultSetToCandidate(ResultSet rs) throws SQLException {
        Candidate c = new Candidate();
        c.setId(rs.getString("id"));
        c.setRequisitionId(rs.getString("requisition_id"));
        c.setRequisitionTitle(rs.getString("requisition_title"));
        c.setDepartmentId(rs.getString("department_id"));
        c.setDepartmentName(rs.getString("department_name"));
        c.setApplicationSourceId(rs.getString("application_source_id"));
        c.setApplicationSourceName(rs.getString("application_source_name"));
        c.setRejectionReasonId(rs.getString("rejection_reason_id"));
        c.setRejectionReasonName(rs.getString("rejection_reason_name"));
        c.setFullName(rs.getString("full_name"));
        c.setEmail(rs.getString("email"));
        c.setPhone(rs.getString("phone"));
        c.setStatus(rs.getString("status"));
        c.setCvUrl(rs.getString("cv_url"));
        c.setNotes(rs.getString("notes"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setUpdatedAt(rs.getTimestamp("updated_at"));

        // Thông tin SLA (KN-363, KN-364, KN-365)
        try {
            c.setSlaOverdue(rs.getBoolean("is_sla_overdue"));
            c.setSlaOverdueDetectedAt(rs.getTimestamp("sla_overdue_detected_at"));
            c.setStageEnteredAt(rs.getTimestamp("stage_entered_at"));
            c.setRecruiterId(rs.getString("recruiter_id"));
            c.setRecruiterName(rs.getString("recruiter_name"));
            c.setRecruiterEmail(rs.getString("recruiter_email"));
            c.setSlaDaysThreshold(rs.getInt("sla_days_threshold"));
            c.setDaysInCurrentStage(rs.getInt("days_in_stage"));
        } catch (SQLException ignored) {
            // Trường hợp cột chưa tồn tại trong view / bảng cũ
        }
        return c;
    }
}

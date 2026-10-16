package com.irms.dao;

import com.irms.model.Candidate;

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
 * Thao tác truy vấn và quản lý Hồ sơ Ứng viên (bảng candidates)
 */
public class CandidateDAO extends BaseDAO {

    public CandidateDAO() {
        ensureColumnsExist();
    }

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
                "referrer.full_name AS referred_by_user_name, " +
                "target_dept.name AS target_department_name, " +
                "pool_user.full_name AS added_to_pool_by_name FROM candidates c " +
                "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                "LEFT JOIN departments d ON r.department_id = d.id " +
                "LEFT JOIN shared_catalogs source ON c.application_source_id = source.id " +
                "LEFT JOIN shared_catalogs reason ON c.rejection_reason_id = reason.id " +
                "LEFT JOIN users referrer ON c.referred_by_user_id = referrer.id " +
                "LEFT JOIN departments target_dept ON c.target_department_id = target_dept.id " +
                "LEFT JOIN users pool_user ON c.added_to_pool_by = pool_user.id WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (recruiterId != null && !recruiterId.trim().isEmpty()) {
            // [KN-116]: Recruiter thấy ứng viên của vị trí mình là recruiter CHÍNH hoặc recruiter HỖ TRỢ
            sql.append("AND (r.recruiter_id = ? OR c.requisition_id IN ("
                + "SELECT requisition_id FROM requisition_recruiters WHERE recruiter_id = ?)) ");
            params.add(recruiterId.trim());
            params.add(recruiterId.trim());
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (c.full_name LIKE ? OR c.email LIKE ? OR c.phone LIKE ? OR c.potential_skills LIKE ?) ");
            String kw = "%" + search.trim() + "%";
            params.add(kw);
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
                list.add(extractCandidateFromResultSet(rs));
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
            // [KN-116]: Recruiter thấy ứng viên của vị trí mình là recruiter CHÍNH hoặc recruiter HỖ TRỢ
            sql.append("AND (r.recruiter_id = ? OR c.requisition_id IN ("
                + "SELECT requisition_id FROM requisition_recruiters WHERE recruiter_id = ?)) ");
            params.add(recruiterId.trim());
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

    public List<Candidate> findReferralsByUser(String referredByUserId) {
        List<Candidate> referrals = new ArrayList<>();
        String sql = "SELECT c.id, c.requisition_id, r.title AS requisition_title, c.full_name, " +
                     "c.status, c.notes, c.created_at FROM candidates c " +
                     "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                     "WHERE c.referred_by_user_id = ? ORDER BY c.created_at DESC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, referredByUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Candidate candidate = new Candidate();
                    candidate.setId(rs.getString("id"));
                    candidate.setRequisitionId(rs.getString("requisition_id"));
                    candidate.setRequisitionTitle(rs.getString("requisition_title"));
                    candidate.setFullName(rs.getString("full_name"));
                    candidate.setStatus(rs.getString("status"));
                    candidate.setNotes(rs.getString("notes"));
                    candidate.setCreatedAt(rs.getTimestamp("created_at"));
                    referrals.add(candidate);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy hồ sơ ứng viên được người dùng giới thiệu", e);
            throw new IllegalStateException("Không thể tải danh sách hồ sơ đã giới thiệu.", e);
        }
        return referrals;
    }

    public boolean insert(Candidate c) {
        String sql = "INSERT INTO candidates (id, requisition_id, application_source_id, rejection_reason_id, " +
                     "referred_by_user_id, full_name, email, phone, status, cv_url, notes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, c.getId());
            ps.setString(2, c.getRequisitionId());
            ps.setString(3, c.getApplicationSourceId());
            ps.setString(4, c.getRejectionReasonId());
            ps.setString(5, c.getReferredByUserId());
            ps.setString(6, c.getFullName());
            ps.setString(7, c.getEmail());
            ps.setString(8, c.getPhone());
            ps.setString(9, c.getStatus() != null ? c.getStatus() : "APPLIED");
            ps.setString(10, c.getCvUrl());
            ps.setString(11, c.getNotes());
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
        String sql = "UPDATE candidates SET status = ?, rejection_reason_id = ? WHERE id = ?";
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

    /**
     * Lấy danh sách các ứng viên còn đang trong pipeline (chưa kết thúc: APPLIED, SCREENING, INTERVIEWING, OFFER)
     */
    public List<Candidate> findActivePipelineCandidates(String requisitionId) {
        List<Candidate> list = new ArrayList<>();
        String sql = "SELECT * FROM candidates WHERE requisition_id = ? " +
                     "AND status IN ('APPLIED', 'SCREENING', 'INTERVIEWING', 'OFFER') " +
                     "ORDER BY created_at ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            rs = ps.executeQuery();
            while (rs.next()) {
                Candidate c = new Candidate();
                c.setId(rs.getString("id"));
                c.setRequisitionId(rs.getString("requisition_id"));
                c.setFullName(rs.getString("full_name"));
                c.setEmail(rs.getString("email"));
                c.setPhone(rs.getString("phone"));
                c.setStatus(rs.getString("status"));
                c.setCvUrl(rs.getString("cv_url"));
                c.setNotes(rs.getString("notes"));
                c.setCreatedAt(rs.getTimestamp("created_at"));
                c.setUpdatedAt(rs.getTimestamp("updated_at"));
                list.add(c);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy ứng viên trong pipeline theo requisition_id: " + requisitionId, e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Đếm số ứng viên còn đang trong pipeline của yêu cầu tuyển dụng
     */
    public int countActivePipelineCandidates(String requisitionId) {
        String sql = "SELECT COUNT(*) FROM candidates WHERE requisition_id = ? " +
                     "AND status IN ('APPLIED', 'SCREENING', 'INTERVIEWING', 'OFFER')";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đếm số ứng viên trong pipeline của requisition_id: " + requisitionId, e);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    /**
     * Cập nhật trạng thái hàng loạt cho các ứng viên còn đang trong pipeline khi đóng yêu cầu tuyển dụng
     * Ví dụ: chuyển sang REJECTED (từ chối lịch sự) kèm ghi chú lý do
     */
    public int updatePipelineCandidates(String requisitionId, String newStatus, String noteAppend) {
        String sql = "UPDATE candidates SET status = ?, " +
                     "notes = CASE WHEN notes IS NULL OR notes = '' THEN ? ELSE CONCAT(notes, ' | ', ?) END, " +
                     "updated_at = CURRENT_TIMESTAMP " +
                     "WHERE requisition_id = ? AND status IN ('APPLIED', 'SCREENING', 'INTERVIEWING', 'OFFER')";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newStatus);
            ps.setString(2, noteAppend != null ? noteAppend : "");
            ps.setString(3, noteAppend != null ? noteAppend : "");
            ps.setString(4, requisitionId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật hàng loạt ứng viên trong pipeline: " + requisitionId, e);
            return 0;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * [KN-231]: Kiểm tra trùng lặp hồ sơ ứng tuyển theo email và vị trí tuyển dụng (requisition_id)
     */
    public boolean existsByEmailAndRequisition(String email, String requisitionId) {
        if (email == null || requisitionId == null) return false;
        String sql = "SELECT COUNT(*) FROM candidates WHERE LOWER(TRIM(email)) = LOWER(TRIM(?)) AND requisition_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, email.trim());
            ps.setString(2, requisitionId.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi kiểm tra trùng lặp email ứng tuyển (" + email + ", " + requisitionId + ")", e);
        } finally {
            close(conn, ps, rs);
        }
        return false;
    }

    /**
     * [KN-233]: Kiểm tra sự tồn tại của mã tra cứu hồ sơ độc nhất
     */
    public boolean existsTrackingCode(String trackingCode) {
        if (trackingCode == null || trackingCode.trim().isEmpty()) return false;
        String sql = "SELECT COUNT(*) FROM candidates WHERE tracking_code = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, trackingCode.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi kiểm tra mã tra cứu hồ sơ: " + trackingCode, e);
        } finally {
            close(conn, ps, rs);
        }
        return false;
    }

    /**
     * [KN-233]: Tìm kiếm hồ sơ ứng viên theo mã tra cứu độc nhất (Tracking Code)
     */
    public Candidate findByTrackingCode(String trackingCode) {
        if (trackingCode == null || trackingCode.trim().isEmpty()) return null;
        String sql = "SELECT c.*, r.title AS requisition_title, r.department_id, d.name AS department_name, " +
                     "source.value AS application_source_name, reason.value AS rejection_reason_name FROM candidates c " +
                     "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                     "LEFT JOIN departments d ON r.department_id = d.id " +
                     "LEFT JOIN shared_catalogs source ON c.application_source_id = source.id " +
                     "LEFT JOIN shared_catalogs reason ON c.rejection_reason_id = reason.id " +
                     "WHERE c.tracking_code = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, trackingCode.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
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
                try { c.setTrackingCode(rs.getString("tracking_code")); } catch (SQLException ignored) {}
                c.setCvUrl(rs.getString("cv_url"));
                c.setNotes(rs.getString("notes"));
                try { c.setCoverLetter(rs.getString("cover_letter")); } catch (SQLException ignored) {}
                try { c.setIpAddress(rs.getString("ip_address")); } catch (SQLException ignored) {}
                c.setCreatedAt(rs.getTimestamp("created_at"));
                c.setUpdatedAt(rs.getTimestamp("updated_at"));
                return c;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm kiếm hồ sơ ứng viên theo tracking_code: " + trackingCode, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * [KN-233]: Lưu bản ghi nộp hồ sơ ứng tuyển mới (hỗ trợ tracking_code, cover_letter, ip_address)
     */
    public boolean insertApplication(Candidate c) {
        String sql = "INSERT INTO candidates (id, requisition_id, application_source_id, rejection_reason_id, " +
                     "full_name, email, phone, status, tracking_code, cv_url, notes, cover_letter, ip_address) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            ps.setString(9, c.getTrackingCode());
            ps.setString(10, c.getCvUrl());
            ps.setString(11, c.getNotes());
            ps.setString(12, c.getCoverLetter());
            ps.setString(13, c.getIpAddress());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lưu bản ghi ứng tuyển mới: " + c.getEmail(), e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Tìm hồ sơ ứng viên theo ID
     */
    public Candidate findById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        String sql = "SELECT c.*, r.title AS requisition_title, r.department_id, d.name AS department_name, " +
                     "source.value AS application_source_name, reason.value AS rejection_reason_name, " +
                     "referrer.full_name AS referred_by_user_name, " +
                     "target_dept.name AS target_department_name, " +
                     "pool_user.full_name AS added_to_pool_by_name FROM candidates c " +
                     "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                     "LEFT JOIN departments d ON r.department_id = d.id " +
                     "LEFT JOIN shared_catalogs source ON c.application_source_id = source.id " +
                     "LEFT JOIN shared_catalogs reason ON c.rejection_reason_id = reason.id " +
                     "LEFT JOIN users referrer ON c.referred_by_user_id = referrer.id " +
                     "LEFT JOIN departments target_dept ON c.target_department_id = target_dept.id " +
                     "LEFT JOIN users pool_user ON c.added_to_pool_by = pool_user.id " +
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
                return extractCandidateFromResultSet(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm kiếm hồ sơ ứng viên theo ID: " + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * Quản lý Kho ứng viên tiềm năng: Tìm kiếm và lọc ứng viên trong kho theo từ khóa, chức danh quan tâm, kỹ năng... (KN-286)
     */
    public List<Candidate> findTalentPool(String search, String targetPosition, String skill, Integer minRating, String departmentId, String recruiterId, int offset, int limit) {
        List<Candidate> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT c.*, r.title AS requisition_title, r.department_id, d.name AS department_name, " +
                "source.value AS application_source_name, reason.value AS rejection_reason_name, " +
                "referrer.full_name AS referred_by_user_name, " +
                "target_dept.name AS target_department_name, " +
                "pool_user.full_name AS added_to_pool_by_name FROM candidates c " +
                "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                "LEFT JOIN departments d ON r.department_id = d.id " +
                "LEFT JOIN shared_catalogs source ON c.application_source_id = source.id " +
                "LEFT JOIN shared_catalogs reason ON c.rejection_reason_id = reason.id " +
                "LEFT JOIN users referrer ON c.referred_by_user_id = referrer.id " +
                "LEFT JOIN departments target_dept ON c.target_department_id = target_dept.id " +
                "LEFT JOIN users pool_user ON c.added_to_pool_by = pool_user.id " +
                "WHERE c.is_potential = TRUE ");

        List<Object> params = new ArrayList<>();
        if (recruiterId != null && !recruiterId.trim().isEmpty()) {
            sql.append("AND (r.recruiter_id = ? OR c.requisition_id IN ("
                + "SELECT requisition_id FROM requisition_recruiters WHERE recruiter_id = ?) OR c.added_to_pool_by = ?) ");
            params.add(recruiterId.trim());
            params.add(recruiterId.trim());
            params.add(recruiterId.trim());
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (c.full_name LIKE ? OR c.email LIKE ? OR c.phone LIKE ? OR c.potential_skills LIKE ? OR c.target_position LIKE ?) ");
            String kw = "%" + search.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (targetPosition != null && !targetPosition.trim().isEmpty()) {
            sql.append("AND c.target_position LIKE ? ");
            params.add("%" + targetPosition.trim() + "%");
        }
        if (minRating != null && minRating > 0) {
            sql.append("AND c.potential_rating >= ? ");
            params.add(minRating);
        }
        if (skill != null && !skill.trim().isEmpty()) {
            sql.append("AND c.potential_skills LIKE ? ");
            params.add("%" + skill.trim() + "%");
        }
        if (departmentId != null && !departmentId.trim().isEmpty()) {
            sql.append("AND (c.target_department_id = ? OR r.department_id = ?) ");
            params.add(departmentId.trim());
            params.add(departmentId.trim());
        }

        sql.append("ORDER BY c.potential_rating DESC, c.added_to_pool_at DESC, c.created_at DESC");
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
                list.add(extractCandidateFromResultSet(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách kho ứng viên tiềm năng", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public List<Candidate> findTalentPool(String search, Integer minRating, String skill, String departmentId, String recruiterId, int offset, int limit) {
        return findTalentPool(search, null, skill, minRating, departmentId, recruiterId, offset, limit);
    }

    /**
     * Đếm tổng số ứng viên trong kho tiềm năng theo bộ lọc bao gồm chức danh quan tâm (KN-286)
     */
    public int countTalentPool(String search, String targetPosition, String skill, Integer minRating, String departmentId, String recruiterId) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM candidates c " +
                "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id WHERE c.is_potential = TRUE ");

        List<Object> params = new ArrayList<>();
        if (recruiterId != null && !recruiterId.trim().isEmpty()) {
            sql.append("AND (r.recruiter_id = ? OR c.requisition_id IN ("
                + "SELECT requisition_id FROM requisition_recruiters WHERE recruiter_id = ?) OR c.added_to_pool_by = ?) ");
            params.add(recruiterId.trim());
            params.add(recruiterId.trim());
            params.add(recruiterId.trim());
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (c.full_name LIKE ? OR c.email LIKE ? OR c.phone LIKE ? OR c.potential_skills LIKE ? OR c.target_position LIKE ?) ");
            String kw = "%" + search.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (targetPosition != null && !targetPosition.trim().isEmpty()) {
            sql.append("AND c.target_position LIKE ? ");
            params.add("%" + targetPosition.trim() + "%");
        }
        if (minRating != null && minRating > 0) {
            sql.append("AND c.potential_rating >= ? ");
            params.add(minRating);
        }
        if (skill != null && !skill.trim().isEmpty()) {
            sql.append("AND c.potential_skills LIKE ? ");
            params.add("%" + skill.trim() + "%");
        }
        if (departmentId != null && !departmentId.trim().isEmpty()) {
            sql.append("AND (c.target_department_id = ? OR r.department_id = ?) ");
            params.add(departmentId.trim());
            params.add(departmentId.trim());
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
            logger.log(Level.SEVERE, "Lỗi đếm số lượng kho ứng viên tiềm năng", e);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    public int countTalentPool(String search, Integer minRating, String skill, String departmentId, String recruiterId) {
        return countTalentPool(search, null, skill, minRating, departmentId, recruiterId);
    }

    /**
     * [KN-286]: Gợi ý ứng viên phù hợp từ kho tiềm năng khi mở vị trí mới
     */
    public List<Candidate> findSuggestedTalents(String requisitionId, int limit) {
        List<Candidate> list = new ArrayList<>();
        String sql = "SELECT c.*, " +
                "       r.title AS requisition_title, d.name AS department_name, " +
                "       source.value AS application_source_name, " +
                "       reason.value AS rejection_reason_name, " +
                "       referrer.full_name AS referred_by_user_name, " +
                "       target_dept.name AS target_department_name, " +
                "       pool_user.full_name AS added_to_pool_by_name, " +
                "       (CASE " +
                "           WHEN c.target_position IS NOT NULL AND (target_req.position_title LIKE CONCAT('%', c.target_position, '%') OR target_req.title LIKE CONCAT('%', c.target_position, '%')) " +
                "                AND c.target_department_id = target_req.department_id THEN 3 " +
                "           WHEN c.target_position IS NOT NULL AND (target_req.position_title LIKE CONCAT('%', c.target_position, '%') OR target_req.title LIKE CONCAT('%', c.target_position, '%')) THEN 2 " +
                "           WHEN c.target_department_id = target_req.department_id THEN 1 " +
                "           ELSE 0 END) AS match_score, " +
                "       (CASE " +
                "           WHEN c.target_position IS NOT NULL AND (target_req.position_title LIKE CONCAT('%', c.target_position, '%') OR target_req.title LIKE CONCAT('%', c.target_position, '%')) " +
                "                AND c.target_department_id = target_req.department_id THEN 'Khớp cả chức danh & phòng ban mục tiêu' " +
                "           WHEN c.target_position IS NOT NULL AND (target_req.position_title LIKE CONCAT('%', c.target_position, '%') OR target_req.title LIKE CONCAT('%', c.target_position, '%')) THEN 'Khớp chức danh quan tâm' " +
                "           WHEN c.target_department_id = target_req.department_id THEN 'Cùng phòng ban mục tiêu' " +
                "           WHEN c.potential_rating >= 4 THEN 'Ứng viên tiềm năng xếp hạng cao (4-5★)' " +
                "           ELSE 'Kỹ năng phù hợp định hướng' END) AS match_reason " +
                "FROM candidates c " +
                "CROSS JOIN recruitment_requisitions target_req " +
                "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                "LEFT JOIN departments d ON r.department_id = d.id " +
                "LEFT JOIN shared_catalogs source ON c.application_source_id = source.id " +
                "LEFT JOIN shared_catalogs reason ON c.rejection_reason_id = reason.id " +
                "LEFT JOIN users referrer ON c.referred_by_user_id = referrer.id " +
                "LEFT JOIN departments target_dept ON c.target_department_id = target_dept.id " +
                "LEFT JOIN users pool_user ON c.added_to_pool_by = pool_user.id " +
                "WHERE target_req.id = ? " +
                "  AND c.is_potential = TRUE " +
                "  AND (c.requisition_id IS NULL OR c.requisition_id != target_req.id) " +
                "  AND ( " +
                "       (c.target_position IS NOT NULL AND (target_req.position_title LIKE CONCAT('%', c.target_position, '%') OR target_req.title LIKE CONCAT('%', c.target_position, '%'))) " +
                "    OR (c.target_department_id IS NOT NULL AND c.target_department_id = target_req.department_id) " +
                "    OR (c.potential_skills IS NOT NULL AND (target_req.job_requirements LIKE CONCAT('%', c.potential_skills, '%') OR target_req.job_description LIKE CONCAT('%', c.potential_skills, '%'))) " +
                "    OR (c.potential_rating >= 4) " +
                "  ) " +
                "ORDER BY match_score DESC, c.potential_rating DESC, c.added_to_pool_at DESC " +
                "LIMIT ?";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId.trim());
            ps.setInt(2, Math.max(1, limit));
            rs = ps.executeQuery();
            while (rs.next()) {
                Candidate c = extractCandidateFromResultSet(rs);
                try { c.setMatchScore(rs.getInt("match_score")); } catch (SQLException ignored) {}
                try { c.setMatchReason(rs.getString("match_reason")); } catch (SQLException ignored) {}
                list.add(c);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm kiếm gợi ý ứng viên tiềm năng cho vị trí: " + requisitionId, e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Đưa một ứng viên có sẵn vào Kho ứng viên tiềm năng
     */
    public boolean addToTalentPool(String candidateId, int rating, String skills, String notes, String targetDeptId, String targetPosition, String userId) {
        String sql = "UPDATE candidates SET is_potential = TRUE, potential_rating = ?, potential_skills = ?, " +
                     "potential_notes = ?, target_department_id = ?, target_position = ?, " +
                     "added_to_pool_at = CURRENT_TIMESTAMP, added_to_pool_by = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, Math.max(1, Math.min(5, rating)));
            ps.setString(2, skills != null && !skills.trim().isEmpty() ? skills.trim() : null);
            ps.setString(3, notes != null && !notes.trim().isEmpty() ? notes.trim() : null);
            ps.setString(4, targetDeptId != null && !targetDeptId.trim().isEmpty() ? targetDeptId.trim() : null);
            ps.setString(5, targetPosition != null && !targetPosition.trim().isEmpty() ? targetPosition.trim() : null);
            ps.setString(6, userId != null && !userId.trim().isEmpty() ? userId.trim() : null);
            ps.setString(7, candidateId.trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đưa ứng viên vào kho tiềm năng: " + candidateId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Cập nhật thông tin đánh giá, kỹ năng của ứng viên trong Kho tiềm năng
     */
    public boolean updateTalentPool(String candidateId, int rating, String skills, String notes, String targetDeptId, String targetPosition) {
        String sql = "UPDATE candidates SET potential_rating = ?, potential_skills = ?, " +
                     "potential_notes = ?, target_department_id = ?, target_position = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ? AND is_potential = TRUE";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, Math.max(1, Math.min(5, rating)));
            ps.setString(2, skills != null && !skills.trim().isEmpty() ? skills.trim() : null);
            ps.setString(3, notes != null && !notes.trim().isEmpty() ? notes.trim() : null);
            ps.setString(4, targetDeptId != null && !targetDeptId.trim().isEmpty() ? targetDeptId.trim() : null);
            ps.setString(5, targetPosition != null && !targetPosition.trim().isEmpty() ? targetPosition.trim() : null);
            ps.setString(6, candidateId.trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật kho ứng viên tiềm năng: " + candidateId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Loại bỏ ứng viên khỏi Kho tiềm năng
     */
    public boolean removeFromTalentPool(String candidateId) {
        String sql = "UPDATE candidates SET is_potential = FALSE, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, candidateId.trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi loại bỏ ứng viên khỏi kho tiềm năng: " + candidateId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Tái sử dụng ứng viên từ kho tiềm năng: Gán vào một Yêu cầu tuyển dụng đang mở
     */
    public boolean assignCandidateToRequisition(String candidateId, String requisitionId, String notes, String userId) {
        String sql = "UPDATE candidates SET requisition_id = ?, status = 'SCREENING', " +
                     "notes = CASE WHEN ? IS NOT NULL AND ? != '' THEN CONCAT(IFNULL(notes, ''), '\n[Tái điều động từ Kho tiềm năng]: ', ?) ELSE notes END, " +
                     "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId.trim());
            ps.setString(2, notes);
            ps.setString(3, notes);
            ps.setString(4, notes);
            ps.setString(5, candidateId.trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi gán ứng viên tiềm năng vào vị trí tuyển dụng: " + candidateId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Thêm mới trực tiếp một ứng viên vào Kho tiềm năng
     */
    public boolean insertPotentialCandidate(Candidate c) {
        String sql = "INSERT INTO candidates (id, requisition_id, application_source_id, full_name, email, phone, status, " +
                     "is_potential, potential_rating, potential_skills, potential_notes, target_department_id, target_position, " +
                     "added_to_pool_at, added_to_pool_by, cv_url, tracking_code, notes, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 'APPLIED', TRUE, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, c.getId());
            ps.setString(2, c.getRequisitionId());
            ps.setString(3, c.getApplicationSourceId() != null ? c.getApplicationSourceId() : "cat-src-referral");
            ps.setString(4, c.getFullName());
            ps.setString(5, c.getEmail());
            ps.setString(6, c.getPhone());
            ps.setInt(7, Math.max(1, Math.min(5, c.getPotentialRating())));
            ps.setString(8, c.getPotentialSkills());
            ps.setString(9, c.getPotentialNotes());
            ps.setString(10, c.getTargetDepartmentId());
            ps.setString(11, c.getTargetPosition());
            ps.setString(12, c.getAddedToPoolBy());
            ps.setString(13, c.getCvUrl());
            ps.setString(14, c.getTrackingCode());
            ps.setString(15, c.getNotes());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi thêm ứng viên mới vào kho tiềm năng: " + c.getEmail(), e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Thống kê KPI Kho ứng viên tiềm năng:
     * index 0: Tổng số trong kho
     * index 1: Tiềm năng cao (rating >= 4)
     * index 2: Đang sẵn sàng (chưa gán vị trí hoặc status APPLIED/SCREENING)
     * index 3: Mới thêm trong 30 ngày
     */
    public int[] getTalentPoolStats(String recruiterId) {
        int[] stats = new int[]{0, 0, 0, 0};
        String sql = "SELECT " +
                     "COUNT(*) AS total, " +
                     "SUM(CASE WHEN potential_rating >= 4 THEN 1 ELSE 0 END) AS high_potential, " +
                     "SUM(CASE WHEN requisition_id IS NULL OR status IN ('APPLIED', 'SCREENING') THEN 1 ELSE 0 END) AS ready_count, " +
                     "SUM(CASE WHEN added_to_pool_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 ELSE 0 END) AS recent_count " +
                     "FROM candidates WHERE is_potential = TRUE";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                stats[0] = rs.getInt("total");
                stats[1] = rs.getInt("high_potential");
                stats[2] = rs.getInt("ready_count");
                stats[3] = rs.getInt("recent_count");
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Lỗi lấy thống kê kho ứng viên tiềm năng", e);
        } finally {
            close(conn, ps, rs);
        }
        return stats;
    }

    /**
     * Helper ánh xạ toàn diện từ ResultSet sang đối tượng Candidate
     */
    private Candidate extractCandidateFromResultSet(ResultSet rs) throws SQLException {
        Candidate c = new Candidate();
        c.setId(rs.getString("id"));
        c.setRequisitionId(rs.getString("requisition_id"));
        try { c.setRequisitionTitle(rs.getString("requisition_title")); } catch (SQLException ignored) {}
        try { c.setDepartmentId(rs.getString("department_id")); } catch (SQLException ignored) {}
        try { c.setDepartmentName(rs.getString("department_name")); } catch (SQLException ignored) {}
        c.setApplicationSourceId(rs.getString("application_source_id"));
        try { c.setApplicationSourceName(rs.getString("application_source_name")); } catch (SQLException ignored) {}
        try { c.setReferredByUserId(rs.getString("referred_by_user_id")); } catch (SQLException ignored) {}
        try { c.setReferredByUserName(rs.getString("referred_by_user_name")); } catch (SQLException ignored) {}
        c.setRejectionReasonId(rs.getString("rejection_reason_id"));
        try { c.setRejectionReasonName(rs.getString("rejection_reason_name")); } catch (SQLException ignored) {}
        c.setFullName(rs.getString("full_name"));
        c.setEmail(rs.getString("email"));
        c.setPhone(rs.getString("phone"));
        c.setStatus(rs.getString("status"));
        c.setCvUrl(rs.getString("cv_url"));
        c.setNotes(rs.getString("notes"));
        try { c.setTrackingCode(rs.getString("tracking_code")); } catch (SQLException ignored) {}
        try { c.setCoverLetter(rs.getString("cover_letter")); } catch (SQLException ignored) {}
        try { c.setIpAddress(rs.getString("ip_address")); } catch (SQLException ignored) {}
        try { c.setPotential(rs.getBoolean("is_potential")); } catch (SQLException ignored) {}
        try { c.setPotentialRating(rs.getInt("potential_rating")); } catch (SQLException ignored) {}
        try { c.setPotentialSkills(rs.getString("potential_skills")); } catch (SQLException ignored) {}
        try { c.setPotentialNotes(rs.getString("potential_notes")); } catch (SQLException ignored) {}
        try { c.setTargetDepartmentId(rs.getString("target_department_id")); } catch (SQLException ignored) {}
        try { c.setTargetDepartmentName(rs.getString("target_department_name")); } catch (SQLException ignored) {}
        try { c.setTargetPosition(rs.getString("target_position")); } catch (SQLException ignored) {}
        try { c.setAddedToPoolAt(rs.getTimestamp("added_to_pool_at")); } catch (SQLException ignored) {}
        try { c.setAddedToPoolBy(rs.getString("added_to_pool_by")); } catch (SQLException ignored) {}
        try { c.setAddedToPoolByName(rs.getString("added_to_pool_by_name")); } catch (SQLException ignored) {}
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setUpdatedAt(rs.getTimestamp("updated_at"));
        return c;
    }

    /**
     * Tự động kiểm tra và thêm các cột cần thiết cho bảng candidates nếu chưa tồn tại
     */
    private void ensureColumnsExist() {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection();
            DatabaseMetaData meta = conn.getMetaData();
            stmt = conn.createStatement();
            addColumnIfNotExists(meta, stmt, "candidates", "tracking_code", "VARCHAR(50) NULL UNIQUE");
            addColumnIfNotExists(meta, stmt, "candidates", "cover_letter", "MEDIUMTEXT NULL");
            addColumnIfNotExists(meta, stmt, "candidates", "ip_address", "VARCHAR(45) NULL");
            addColumnIfNotExists(meta, stmt, "candidates", "is_potential", "BOOLEAN NOT NULL DEFAULT FALSE");
            addColumnIfNotExists(meta, stmt, "candidates", "potential_rating", "INT NOT NULL DEFAULT 3");
            addColumnIfNotExists(meta, stmt, "candidates", "potential_skills", "VARCHAR(500) NULL");
            addColumnIfNotExists(meta, stmt, "candidates", "potential_notes", "TEXT NULL");
            addColumnIfNotExists(meta, stmt, "candidates", "target_department_id", "VARCHAR(36) NULL");
            addColumnIfNotExists(meta, stmt, "candidates", "target_position", "VARCHAR(255) NULL");
            addColumnIfNotExists(meta, stmt, "candidates", "added_to_pool_at", "TIMESTAMP NULL");
            addColumnIfNotExists(meta, stmt, "candidates", "added_to_pool_by", "VARCHAR(36) NULL");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Không thể kiểm tra/cập nhật cột cho candidates: " + e.getMessage());
        } finally {
            closeStatement(conn, stmt);
        }
    }

    private void addColumnIfNotExists(DatabaseMetaData meta, Statement stmt, String tableName, String columnName, String columnDef) {
        try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            if (!rs.next()) {
                try (ResultSet rsUpper = meta.getColumns(null, null, tableName.toUpperCase(), columnName.toUpperCase())) {
                    if (!rsUpper.next()) {
                        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDef;
                        stmt.executeUpdate(sql);
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

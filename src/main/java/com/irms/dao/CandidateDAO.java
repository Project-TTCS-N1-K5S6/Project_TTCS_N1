package com.irms.dao;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.irms.model.Candidate;
import com.irms.model.CandidateWithdrawal;
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
import java.util.Arrays;
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
                "referrer.full_name AS referred_by_user_name FROM candidates c " +
                "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                "LEFT JOIN departments d ON r.department_id = d.id " +
                "LEFT JOIN shared_catalogs source ON c.application_source_id = source.id " +
                "LEFT JOIN shared_catalogs reason ON c.rejection_reason_id = reason.id " +
                "LEFT JOIN users referrer ON c.referred_by_user_id = referrer.id WHERE 1=1 ");

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
                list.add(mapCandidate(rs));
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

    /**
     * [KN-218]: Tìm kiếm một hồ sơ ứng viên theo ID
     */
    public Candidate findById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        String sql = "SELECT c.*, r.title AS requisition_title, r.department_id, d.name AS department_name, " +
                     "source.value AS application_source_name, reason.value AS rejection_reason_name, " +
                     "referrer.full_name AS referred_by_user_name FROM candidates c " +
                     "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                     "LEFT JOIN departments d ON r.department_id = d.id " +
                     "LEFT JOIN shared_catalogs source ON c.application_source_id = source.id " +
                     "LEFT JOIN shared_catalogs reason ON c.rejection_reason_id = reason.id " +
                     "LEFT JOIN users referrer ON c.referred_by_user_id = referrer.id " +
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
                Candidate c = new Candidate();
                c.setId(rs.getString("id"));
                c.setRequisitionId(rs.getString("requisition_id"));
                c.setRequisitionTitle(rs.getString("requisition_title"));
                c.setDepartmentId(rs.getString("department_id"));
                c.setDepartmentName(rs.getString("department_name"));
                c.setApplicationSourceId(rs.getString("application_source_id"));
                c.setApplicationSourceName(rs.getString("application_source_name"));
                c.setReferredByUserId(rs.getString("referred_by_user_id"));
                c.setReferredByUserName(rs.getString("referred_by_user_name"));
                c.setRejectionReasonId(rs.getString("rejection_reason_id"));
                c.setRejectionReasonName(rs.getString("rejection_reason_name"));
                c.setFullName(rs.getString("full_name"));
                c.setEmail(rs.getString("email"));
                c.setPhone(rs.getString("phone"));
                c.setStatus(rs.getString("status"));
                c.setCvUrl(rs.getString("cv_url"));
                c.setNotes(rs.getString("notes"));
                try { c.setTags(rs.getString("tags")); } catch (SQLException ignored) {}
                try { c.setTrackingCode(rs.getString("tracking_code")); } catch (SQLException ignored) {}
                try { c.setCoverLetter(rs.getString("cover_letter")); } catch (SQLException ignored) {}
                try { c.setIpAddress(rs.getString("ip_address")); } catch (SQLException ignored) {}
                c.setCreatedAt(rs.getTimestamp("created_at"));
                c.setUpdatedAt(rs.getTimestamp("updated_at"));
                return c;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm kiếm ứng viên theo id: " + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * [KN-218]: Lấy toàn bộ lịch sử ứng tuyển của cùng một người (theo email), sắp xếp mới nhất trước.
     * Phục vụ trang Hồ sơ Ứng viên Hợp nhất - hiển thị tất cả các lần ứng tuyển.
     */
    public List<Candidate> findAllByEmail(String email) {
        List<Candidate> list = new ArrayList<>();
        if (email == null || email.trim().isEmpty()) return list;
        String sql = "SELECT c.*, r.title AS requisition_title, r.department_id, d.name AS department_name, " +
                     "source.value AS application_source_name, reason.value AS rejection_reason_name, " +
                     "referrer.full_name AS referred_by_user_name FROM candidates c " +
                     "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                     "LEFT JOIN departments d ON r.department_id = d.id " +
                     "LEFT JOIN shared_catalogs source ON c.application_source_id = source.id " +
                     "LEFT JOIN shared_catalogs reason ON c.rejection_reason_id = reason.id " +
                     "LEFT JOIN users referrer ON c.referred_by_user_id = referrer.id " +
                     "WHERE LOWER(TRIM(c.email)) = LOWER(TRIM(?)) " +
                     "ORDER BY c.created_at DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, email.trim());
            rs = ps.executeQuery();
            while (rs.next()) {
                Candidate c = new Candidate();
                c.setId(rs.getString("id"));
                c.setRequisitionId(rs.getString("requisition_id"));
                c.setRequisitionTitle(rs.getString("requisition_title"));
                c.setDepartmentId(rs.getString("department_id"));
                c.setDepartmentName(rs.getString("department_name"));
                c.setApplicationSourceId(rs.getString("application_source_id"));
                c.setApplicationSourceName(rs.getString("application_source_name"));
                c.setReferredByUserId(rs.getString("referred_by_user_id"));
                c.setReferredByUserName(rs.getString("referred_by_user_name"));
                c.setRejectionReasonId(rs.getString("rejection_reason_id"));
                c.setRejectionReasonName(rs.getString("rejection_reason_name"));
                c.setFullName(rs.getString("full_name"));
                c.setEmail(rs.getString("email"));
                c.setPhone(rs.getString("phone"));
                c.setStatus(rs.getString("status"));
                c.setCvUrl(rs.getString("cv_url"));
                c.setNotes(rs.getString("notes"));
                try { c.setTags(rs.getString("tags")); } catch (SQLException ignored) {}
                try { c.setTrackingCode(rs.getString("tracking_code")); } catch (SQLException ignored) {}
                try { c.setCoverLetter(rs.getString("cover_letter")); } catch (SQLException ignored) {}
                try { c.setIpAddress(rs.getString("ip_address")); } catch (SQLException ignored) {}
                c.setCreatedAt(rs.getTimestamp("created_at"));
                c.setUpdatedAt(rs.getTimestamp("updated_at"));
                list.add(c);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy lịch sử ứng tuyển theo email: " + email, e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * [KN-218]: Cập nhật ghi chú (notes) cho một hồ sơ ứng viên
     */
    public boolean updateNotes(String candidateId, String notes) {
        String sql = "UPDATE candidates SET notes = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, notes);
            ps.setString(2, candidateId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật ghi chú ứng viên: " + candidateId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * [KN-274]: Cập nhật nhãn phân loại (tags) cho hồ sơ ứng viên
     */
    public boolean updateTags(String candidateId, String tags) {
        String sql = "UPDATE candidates SET tags = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, tags);
            ps.setString(2, candidateId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật nhãn ứng viên: " + candidateId, e);
            return false;
        } finally {
            close(conn, ps);
        }
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
                     "referred_by_user_id, full_name, email, phone, status, cv_url, " +
                     "extracted_name, extracted_email, extracted_phone, extraction_status, cv_extracted_at, notes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            ps.setString(11, c.getExtractedName());
            ps.setString(12, c.getExtractedEmail());
            ps.setString(13, c.getExtractedPhone());
            ps.setString(14, c.getExtractionStatus() != null ? c.getExtractionStatus() : "NONE");
            ps.setTimestamp(15, c.getCvExtractedAt());
            ps.setString(16, c.getNotes());
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
                     "source.value AS application_source_name, reason.value AS rejection_reason_name, " +
                     "referrer.full_name AS referred_by_user_name FROM candidates c " +
                     "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                     "LEFT JOIN departments d ON r.department_id = d.id " +
                     "LEFT JOIN shared_catalogs source ON c.application_source_id = source.id " +
                     "LEFT JOIN shared_catalogs reason ON c.rejection_reason_id = reason.id " +
                     "LEFT JOIN users referrer ON c.referred_by_user_id = referrer.id " +
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
                return mapCandidate(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm kiếm hồ sơ ứng viên theo tracking_code: " + trackingCode, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * [KN-213]: Tìm hồ sơ ứng viên theo ID kèm thông tin trích xuất từ CV
     */
    public Candidate findById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        String sql = "SELECT c.*, r.title AS requisition_title, r.department_id, d.name AS department_name, " +
                     "source.value AS application_source_name, reason.value AS rejection_reason_name, " +
                     "referrer.full_name AS referred_by_user_name FROM candidates c " +
                     "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                     "LEFT JOIN departments d ON r.department_id = d.id " +
                     "LEFT JOIN shared_catalogs source ON c.application_source_id = source.id " +
                     "LEFT JOIN shared_catalogs reason ON c.rejection_reason_id = reason.id " +
                     "LEFT JOIN users referrer ON c.referred_by_user_id = referrer.id " +
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
                return mapCandidate(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm kiếm hồ sơ ứng viên theo id: " + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * [KN-233]: Lưu bản ghi nộp hồ sơ ứng tuyển mới (hỗ trợ tracking_code, cover_letter, ip_address, extracted info)
     */
    public boolean insertApplication(Candidate c) {
        return insertApplication(c, null);
    }

    public boolean insertApplication(Candidate c, String withdrawalTokenHash) {
        String sql = "INSERT INTO candidates (id, requisition_id, application_source_id, rejection_reason_id, " +
                     "full_name, email, phone, status, tracking_code, cv_url, " +
                     "extracted_name, extracted_email, extracted_phone, extraction_status, cv_extracted_at, " +
                     "notes, cover_letter, ip_address) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = getConnection();
            if (withdrawalTokenHash != null) {
                conn.setAutoCommit(false);
            }
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
                ps.setString(11, c.getExtractedName());
                ps.setString(12, c.getExtractedEmail());
                ps.setString(13, c.getExtractedPhone());
                ps.setString(14, c.getExtractionStatus() != null ? c.getExtractionStatus() : "NONE");
                ps.setTimestamp(15, c.getCvExtractedAt());
                ps.setString(16, c.getNotes());
                ps.setString(17, c.getCoverLetter());
                ps.setString(18, c.getIpAddress());
                if (ps.executeUpdate() == 0) {
                    if (withdrawalTokenHash != null) conn.rollback();
                    return false;
                }
            }

            if (withdrawalTokenHash != null) {
                String tokenSql = "INSERT INTO candidate_withdrawal_tokens " +
                        "(id, candidate_id, token_hash, expires_at) VALUES (?, ?, ?, ?)";
                try (PreparedStatement tokenPs = conn.prepareStatement(tokenSql)) {
                    tokenPs.setString(1, SecurityUtil.generateUUID());
                    tokenPs.setString(2, c.getId());
                    tokenPs.setString(3, withdrawalTokenHash);
                    tokenPs.setTimestamp(4, new Timestamp(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000));
                    tokenPs.executeUpdate();
                }
                conn.commit();
            }
            return true;
        } catch (SQLException e) {
            if (conn != null && withdrawalTokenHash != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    logger.log(Level.SEVERE, "Lỗi rollback khi lưu hồ sơ và token rút hồ sơ", rollbackError);
                }
            }
            logger.log(Level.SEVERE, "Lỗi lưu bản ghi ứng tuyển mới: " + c.getEmail(), e);
            return false;
        } finally {
            close(conn, null);
        }
    }

    public CandidateWithdrawal findByValidWithdrawalTokenHash(String tokenHash) {
        String sql = "SELECT c.id, c.full_name, c.email, c.tracking_code, c.status, r.title AS requisition_title " +
                "FROM candidate_withdrawal_tokens t " +
                "JOIN candidates c ON c.id = t.candidate_id " +
                "LEFT JOIN recruitment_requisitions r ON r.id = c.requisition_id " +
                "WHERE t.token_hash = ? AND t.used_at IS NULL AND t.expires_at > CURRENT_TIMESTAMP " +
                "AND c.status IN ('APPLIED', 'SCREENING', 'INTERVIEWING', 'OFFER')";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapCandidateWithdrawal(rs) : null;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi xác thực liên kết rút hồ sơ", e);
            throw new IllegalStateException("Không thể xác thực liên kết rút hồ sơ.", e);
        }
    }

    public CandidateWithdrawal withdrawByTokenHash(String tokenHash, String ipAddress, String userAgent) {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            String candidateId = null;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT candidate_id FROM candidate_withdrawal_tokens " +
                    "WHERE token_hash = ? AND used_at IS NULL AND expires_at > CURRENT_TIMESTAMP FOR UPDATE")) {
                ps.setString(1, tokenHash);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) candidateId = rs.getString("candidate_id");
                }
            }
            if (candidateId == null) {
                conn.rollback();
                return null;
            }

            String oldStatus;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT status FROM candidates WHERE id = ? FOR UPDATE")) {
                ps.setString(1, candidateId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return null;
                    }
                    oldStatus = rs.getString("status");
                }
            }

            List<String> activeStatuses = Arrays.asList("APPLIED", "SCREENING", "INTERVIEWING", "OFFER");
            if (!activeStatuses.contains(oldStatus)) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE candidate_withdrawal_tokens SET used_at = CURRENT_TIMESTAMP " +
                        "WHERE token_hash = ? AND used_at IS NULL")) {
                    ps.setString(1, tokenHash);
                    ps.executeUpdate();
                }
                conn.commit();
                return null;
            }

            CandidateWithdrawal withdrawal = loadWithdrawalDetails(conn, candidateId);
            if (withdrawal == null) {
                conn.rollback();
                return null;
            }
            withdrawal.setStatusBeforeWithdrawal(oldStatus);

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE candidates SET status = 'WITHDRAWN', updated_at = CURRENT_TIMESTAMP " +
                    "WHERE id = ? AND status IN ('APPLIED', 'SCREENING', 'INTERVIEWING', 'OFFER')")) {
                ps.setString(1, candidateId);
                if (ps.executeUpdate() != 1) {
                    conn.rollback();
                    return null;
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE candidate_withdrawal_tokens SET used_at = CURRENT_TIMESTAMP " +
                    "WHERE token_hash = ? AND used_at IS NULL AND expires_at > CURRENT_TIMESTAMP")) {
                ps.setString(1, tokenHash);
                if (ps.executeUpdate() != 1) {
                    conn.rollback();
                    return null;
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, description, ip_address, user_agent) " +
                    "VALUES (?, NULL, 'CANDIDATE_WITHDRAWN', 'CANDIDATE', ?, ?, ?, ?)")) {
                ps.setString(1, SecurityUtil.generateUUID());
                ps.setString(2, candidateId);
                ps.setString(3, "Ứng viên " + withdrawal.getFullName() + " (" + withdrawal.getTrackingCode()
                        + ") đã rút hồ sơ qua liên kết email. Trạng thái: " + oldStatus + " -> WITHDRAWN.");
                ps.setString(4, ipAddress);
                ps.setString(5, userAgent);
                ps.executeUpdate();
            }

            if (withdrawal.getRecruiterEmail() != null && !withdrawal.getRecruiterEmail().trim().isEmpty()) {
                JsonObject payload = new JsonObject();
                payload.addProperty("candidateId", withdrawal.getCandidateId());
                payload.addProperty("candidateName", withdrawal.getFullName());
                payload.addProperty("candidateEmail", withdrawal.getEmail());
                payload.addProperty("trackingCode", withdrawal.getTrackingCode());
                payload.addProperty("positionTitle", withdrawal.getRequisitionTitle());
                payload.addProperty("previousStatus", oldStatus);
                payload.addProperty("recruiterName", withdrawal.getRecruiterName());
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO email_outbox (id, recipient, subject, template, payload, status) " +
                        "VALUES (?, ?, ?, 'CANDIDATE_WITHDRAWAL_RECRUITER_NOTIFICATION', ?, 'PENDING')")) {
                    ps.setString(1, SecurityUtil.generateUUID());
                    ps.setString(2, withdrawal.getRecruiterEmail().trim());
                    ps.setString(3, "[IRMS] Ứng viên đã rút hồ sơ - " + withdrawal.getTrackingCode());
                    ps.setString(4, new Gson().toJson(payload));
                    ps.executeUpdate();
                }
            } else {
                logger.warning("Không tìm thấy email recruiter phụ trách cho hồ sơ " + withdrawal.getTrackingCode());
            }

            conn.commit();
            return withdrawal;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackError) {
                    logger.log(Level.SEVERE, "Lỗi rollback khi rút hồ sơ ứng viên", rollbackError);
                }
            }
            logger.log(Level.SEVERE, "Lỗi cập nhật trạng thái rút hồ sơ ứng viên", e);
            throw new IllegalStateException("Không thể hoàn tất yêu cầu rút hồ sơ.", e);
        } finally {
            close(conn, null);
        }
    }

    private CandidateWithdrawal loadWithdrawalDetails(Connection conn, String candidateId) throws SQLException {
        String sql = "SELECT c.id, c.full_name, c.email, c.tracking_code, c.status, r.title AS requisition_title, " +
                "u.full_name AS recruiter_name, u.email AS recruiter_email " +
                "FROM candidates c " +
                "LEFT JOIN recruitment_requisitions r ON r.id = c.requisition_id " +
                "LEFT JOIN users u ON u.id = r.recruiter_id WHERE c.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                CandidateWithdrawal withdrawal = mapCandidateWithdrawal(rs);
                withdrawal.setRecruiterName(rs.getString("recruiter_name"));
                withdrawal.setRecruiterEmail(rs.getString("recruiter_email"));
                return withdrawal;
            }
        }
    }

    private CandidateWithdrawal mapCandidateWithdrawal(ResultSet rs) throws SQLException {
        CandidateWithdrawal withdrawal = new CandidateWithdrawal();
        withdrawal.setCandidateId(rs.getString("id"));
        withdrawal.setFullName(rs.getString("full_name"));
        withdrawal.setEmail(rs.getString("email"));
        withdrawal.setTrackingCode(rs.getString("tracking_code"));
        withdrawal.setStatusBeforeWithdrawal(rs.getString("status"));
        withdrawal.setRequisitionTitle(rs.getString("requisition_title"));
        return withdrawal;
    }

    /**
     * [KN-213]: Cập nhật kết quả bóc tách tự động từ CV (không ghi đè thông tin chính của ứng viên)
     */
    public boolean updateExtractionData(String candidateId, String extractedName, String extractedEmail, String extractedPhone, String status) {
        String sql = "UPDATE candidates SET extracted_name = ?, extracted_email = ?, extracted_phone = ?, " +
                     "extraction_status = ?, cv_extracted_at = CURRENT_TIMESTAMP WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, extractedName);
            ps.setString(2, extractedEmail);
            ps.setString(3, extractedPhone);
            ps.setString(4, status != null ? status : "SUCCESS");
            ps.setString(5, candidateId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật dữ liệu trích xuất CV cho ứng viên: " + candidateId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * [KN-213]: Recruiter xác nhận hoặc sửa thông tin đã bóc tách để cập nhật vào hồ sơ chính thức
     */
    public boolean updateConfirmExtraction(String candidateId, String confirmedName, String confirmedEmail, String confirmedPhone) {
        String sql = "UPDATE candidates SET full_name = ?, email = ?, phone = ?, " +
                     "extraction_status = 'CONFIRMED', updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, confirmedName != null ? confirmedName.trim() : "");
            ps.setString(2, confirmedEmail != null ? confirmedEmail.trim() : "");
            ps.setString(3, confirmedPhone != null ? confirmedPhone.trim() : null);
            ps.setString(4, candidateId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật xác nhận thông tin bóc tách cho ứng viên: " + candidateId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Ánh xạ đối tượng Candidate từ ResultSet an toàn
     */
    private Candidate mapCandidate(ResultSet rs) throws SQLException {
        Candidate c = new Candidate();
        c.setId(rs.getString("id"));
        c.setRequisitionId(rs.getString("requisition_id"));
        try { c.setRequisitionTitle(rs.getString("requisition_title")); } catch (SQLException ignored) {}
        try { c.setDepartmentId(rs.getString("department_id")); } catch (SQLException ignored) {}
        try { c.setDepartmentName(rs.getString("department_name")); } catch (SQLException ignored) {}
        c.setApplicationSourceId(rs.getString("application_source_id"));
        try { c.setApplicationSourceName(rs.getString("application_source_name")); } catch (SQLException ignored) {}
        c.setReferredByUserId(rs.getString("referred_by_user_id"));
        try { c.setReferredByUserName(rs.getString("referred_by_user_name")); } catch (SQLException ignored) {}
        c.setRejectionReasonId(rs.getString("rejection_reason_id"));
        try { c.setRejectionReasonName(rs.getString("rejection_reason_name")); } catch (SQLException ignored) {}
        c.setFullName(rs.getString("full_name"));
        c.setEmail(rs.getString("email"));
        c.setPhone(rs.getString("phone"));
        c.setStatus(rs.getString("status"));
        c.setCvUrl(rs.getString("cv_url"));
        c.setNotes(rs.getString("notes"));
        try { c.setTags(rs.getString("tags")); } catch (SQLException ignored) {}
        try { c.setTrackingCode(rs.getString("tracking_code")); } catch (SQLException ignored) {}
        try { c.setExtractedName(rs.getString("extracted_name")); } catch (SQLException ignored) {}
        try { c.setExtractedEmail(rs.getString("extracted_email")); } catch (SQLException ignored) {}
        try { c.setExtractedPhone(rs.getString("extracted_phone")); } catch (SQLException ignored) {}
        try { c.setExtractionStatus(rs.getString("extraction_status")); } catch (SQLException ignored) {}
        try { c.setCvExtractedAt(rs.getTimestamp("cv_extracted_at")); } catch (SQLException ignored) {}
        try { c.setCoverLetter(rs.getString("cover_letter")); } catch (SQLException ignored) {}
        try { c.setIpAddress(rs.getString("ip_address")); } catch (SQLException ignored) {}
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setUpdatedAt(rs.getTimestamp("updated_at"));
        return c;
    }

    /**
     * [KN-218]: Lấy danh sách tất cả các lần ứng tuyển của cùng một người (theo email)
     */
    public List<Candidate> findAllByEmail(String email) {
        List<Candidate> list = new ArrayList<>();
        if (email == null || email.trim().isEmpty()) return list;

        String sql = "SELECT c.*, r.title AS requisition_title, r.department_id, d.name AS department_name, " +
                     "source.value AS application_source_name, u.full_name AS referred_by_user_name, " +
                     "reason.value AS rejection_reason_name " +
                     "FROM candidates c " +
                     "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id " +
                     "LEFT JOIN departments d ON r.department_id = d.id " +
                     "LEFT JOIN shared_catalogs source ON c.application_source_id = source.id " +
                     "LEFT JOIN shared_catalogs reason ON c.rejection_reason_id = reason.id " +
                     "LEFT JOIN users u ON c.referred_by_user_id = u.id " +
                     "WHERE LOWER(TRIM(c.email)) = LOWER(TRIM(?)) " +
                     "ORDER BY c.applied_at DESC, c.created_at DESC";

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, email.trim());
            rs = ps.executeQuery();
            while (rs.next()) {
                Candidate c = mapCandidate(rs);
                list.add(c);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy lịch sử ứng tuyển theo email: " + email, e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * [KN-218]: Cập nhật ghi chú (notes) cho một hồ sơ ứng viên
     */
    public boolean updateNotes(String candidateId, String notes) {
        String sql = "UPDATE candidates SET notes = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, notes);
            ps.setString(2, candidateId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật ghi chú ứng viên: " + candidateId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * [KN-274]: Cập nhật nhãn phân loại (tags) cho hồ sơ ứng viên
     */
    public boolean updateTags(String candidateId, String tags) {
        String sql = "UPDATE candidates SET tags = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, tags);
            ps.setString(2, candidateId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật nhãn ứng viên: " + candidateId, e);
            return false;
        } finally {
            close(conn, ps);
        }
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
            addColumnIfNotExists(meta, stmt, "candidates", "referred_by_user_id", "VARCHAR(36) NULL");
            addColumnIfNotExists(meta, stmt, "candidates", "extracted_name", "VARCHAR(255) NULL");
            addColumnIfNotExists(meta, stmt, "candidates", "extracted_email", "VARCHAR(255) NULL");
            addColumnIfNotExists(meta, stmt, "candidates", "extracted_phone", "VARCHAR(50) NULL");
            addColumnIfNotExists(meta, stmt, "candidates", "extraction_status", "VARCHAR(50) DEFAULT 'NONE'");
            addColumnIfNotExists(meta, stmt, "candidates", "cv_extracted_at", "TIMESTAMP NULL");
            addColumnIfNotExists(meta, stmt, "candidates", "tags", "VARCHAR(500) NULL");
            try {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS candidate_withdrawal_tokens (" +
                        "id VARCHAR(36) PRIMARY KEY, " +
                        "candidate_id VARCHAR(36) NOT NULL UNIQUE, " +
                        "token_hash VARCHAR(64) NOT NULL UNIQUE, " +
                        "expires_at TIMESTAMP NOT NULL, " +
                        "used_at TIMESTAMP NULL, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")");
            } catch (SQLException ignored) {}
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

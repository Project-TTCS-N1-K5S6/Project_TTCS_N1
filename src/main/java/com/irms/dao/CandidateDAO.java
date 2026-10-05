package com.irms.dao;

import com.irms.model.Candidate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        List<Candidate> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT c.*, r.title AS requisition_title FROM candidates c " +
                "LEFT JOIN recruitment_requisitions r ON c.requisition_id = r.id WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (recruiterId != null && !recruiterId.trim().isEmpty()) {
            sql.append("AND r.recruiter_id = ? ");
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
                Candidate c = new Candidate();
                c.setId(rs.getString("id"));
                c.setRequisitionId(rs.getString("requisition_id"));
                c.setRequisitionTitle(rs.getString("requisition_title"));
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
            logger.log(Level.SEVERE, "Lỗi lấy danh sách ứng viên", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public boolean insert(Candidate c) {
        String sql = "INSERT INTO candidates (id, requisition_id, full_name, email, phone, status, cv_url, notes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, c.getId());
            ps.setString(2, c.getRequisitionId());
            ps.setString(3, c.getFullName());
            ps.setString(4, c.getEmail());
            ps.setString(5, c.getPhone());
            ps.setString(6, c.getStatus() != null ? c.getStatus() : "APPLIED");
            ps.setString(7, c.getCvUrl());
            ps.setString(8, c.getNotes());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi thêm ứng viên mới", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    public boolean updateStatus(String candidateId, String newStatus) {
        String sql = "UPDATE candidates SET status = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newStatus);
            ps.setString(2, candidateId);
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
}

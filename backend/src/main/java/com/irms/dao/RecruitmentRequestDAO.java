package com.irms.dao;

import com.irms.model.RecruitmentRequest;

import java.sql.*;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Thao tác truy vấn và quản lý Yêu cầu tuyển dụng (bảng recruitment_requisitions)
 */
public class RecruitmentRequestDAO extends BaseDAO {

    public RecruitmentRequestDAO() {
        ensureSchema();
    }

    /**
     * Tự động bổ sung các cột cần thiết nếu DB đã tồn tại cấu trúc cũ
     */
    private void ensureSchema() {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            DatabaseMetaData meta = conn.getMetaData();

            // Kiểm tra và thêm từng cột nếu chưa tồn tại
            addColumnIfNotExists(meta, stmt, "recruitment_requisitions", "position_title", "VARCHAR(255) NULL");
            addColumnIfNotExists(meta, stmt, "recruitment_requisitions", "headcount", "INT NOT NULL DEFAULT 1");
            addColumnIfNotExists(meta, stmt, "recruitment_requisitions", "recruitment_reason", "VARCHAR(50) NOT NULL DEFAULT 'NEW_HEADCOUNT'");
            addColumnIfNotExists(meta, stmt, "recruitment_requisitions", "min_salary", "DECIMAL(15,2) NULL");
            addColumnIfNotExists(meta, stmt, "recruitment_requisitions", "max_salary", "DECIMAL(15,2) NULL");
            addColumnIfNotExists(meta, stmt, "recruitment_requisitions", "currency", "VARCHAR(10) NOT NULL DEFAULT 'VND'");
            addColumnIfNotExists(meta, stmt, "recruitment_requisitions", "deadline", "DATE NULL");
            addColumnIfNotExists(meta, stmt, "recruitment_requisitions", "job_description", "LONGTEXT NULL");
            addColumnIfNotExists(meta, stmt, "recruitment_requisitions", "job_requirements", "LONGTEXT NULL");
            addColumnIfNotExists(meta, stmt, "recruitment_requisitions", "salary_explanation", "TEXT NULL");
            addColumnIfNotExists(meta, stmt, "recruitment_requisitions", "created_by", "VARCHAR(36) NULL");
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Không thể kiểm tra/cập nhật schema recruitment_requisitions: " + e.getMessage());
        } finally {
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException ignored) {}
            }
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    private void addColumnIfNotExists(DatabaseMetaData meta, Statement stmt, String tableName, String columnName, String columnDef) {
        try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            if (!rs.next()) {
                String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDef;
                stmt.executeUpdate(sql);
                logger.info("Đã bổ sung cột " + columnName + " vào bảng " + tableName);
            }
        } catch (SQLException e) {
            logger.log(Level.FINE, "Cột " + columnName + " có thể đã tồn tại: " + e.getMessage());
        }
    }

    public List<RecruitmentRequest> findAll(String search, String status, String departmentId) {
        List<RecruitmentRequest> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT r.*, d.name AS department_name, " +
                "u_rec.full_name AS recruiter_name, " +
                "u_mgr.full_name AS hiring_manager_name, " +
                "u_cre.full_name AS created_by_name " +
                "FROM recruitment_requisitions r " +
                "LEFT JOIN departments d ON r.department_id = d.id " +
                "LEFT JOIN users u_rec ON r.recruiter_id = u_rec.id " +
                "LEFT JOIN users u_mgr ON r.hiring_manager_id = u_mgr.id " +
                "LEFT JOIN users u_cre ON r.created_by = u_cre.id " +
                "WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(r.code) LIKE ? OR LOWER(r.title) LIKE ? OR LOWER(r.position_title) LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (status != null && !status.trim().isEmpty()) {
            sql.append("AND r.status = ? ");
            params.add(status.trim());
        }

        if (departmentId != null && !departmentId.trim().isEmpty()) {
            sql.append("AND r.department_id = ? ");
            params.add(departmentId.trim());
        }

        sql.append("ORDER BY r.created_at DESC");

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
                list.add(mapResultSetToModel(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách yêu cầu tuyển dụng", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public RecruitmentRequest findById(String id) {
        String sql = "SELECT r.*, d.name AS department_name, " +
                     "u_rec.full_name AS recruiter_name, " +
                     "u_mgr.full_name AS hiring_manager_name, " +
                     "u_cre.full_name AS created_by_name " +
                     "FROM recruitment_requisitions r " +
                     "LEFT JOIN departments d ON r.department_id = d.id " +
                     "LEFT JOIN users u_rec ON r.recruiter_id = u_rec.id " +
                     "LEFT JOIN users u_mgr ON r.hiring_manager_id = u_mgr.id " +
                     "LEFT JOIN users u_cre ON r.created_by = u_cre.id " +
                     "WHERE r.id = ?";

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
            logger.log(Level.SEVERE, "Lỗi tìm yêu cầu tuyển dụng theo ID: " + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public boolean insert(RecruitmentRequest req) {
        String sql = "INSERT INTO recruitment_requisitions " +
                     "(id, code, title, position_title, department_id, headcount, recruitment_reason, " +
                     "min_salary, max_salary, currency, deadline, job_description, job_requirements, " +
                     "salary_explanation, status, recruiter_id, hiring_manager_id, created_by) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, req.getId());
            ps.setString(2, req.getCode());
            ps.setString(3, req.getTitle());
            ps.setString(4, req.getPositionTitle());
            ps.setString(5, req.getDepartmentId());
            ps.setInt(6, req.getHeadcount());
            ps.setString(7, req.getRecruitmentReason());
            ps.setBigDecimal(8, req.getMinSalary());
            ps.setBigDecimal(9, req.getMaxSalary());
            ps.setString(10, req.getCurrency() != null ? req.getCurrency() : "VND");
            ps.setDate(11, req.getDeadline());
            ps.setString(12, req.getJobDescription());
            ps.setString(13, req.getJobRequirements());
            ps.setString(14, req.getSalaryExplanation());
            ps.setString(15, req.getStatus());
            ps.setString(16, req.getRecruiterId());
            ps.setString(17, req.getHiringManagerId());
            ps.setString(18, req.getCreatedBy());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi thêm yêu cầu tuyển dụng mới", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    public boolean update(RecruitmentRequest req) {
        String sql = "UPDATE recruitment_requisitions SET " +
                     "title = ?, position_title = ?, department_id = ?, headcount = ?, recruitment_reason = ?, " +
                     "min_salary = ?, max_salary = ?, currency = ?, deadline = ?, job_description = ?, " +
                     "job_requirements = ?, salary_explanation = ?, status = ?, recruiter_id = ?, " +
                     "hiring_manager_id = ? " +
                     "WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, req.getTitle());
            ps.setString(2, req.getPositionTitle());
            ps.setString(3, req.getDepartmentId());
            ps.setInt(4, req.getHeadcount());
            ps.setString(5, req.getRecruitmentReason());
            ps.setBigDecimal(6, req.getMinSalary());
            ps.setBigDecimal(7, req.getMaxSalary());
            ps.setString(8, req.getCurrency() != null ? req.getCurrency() : "VND");
            ps.setDate(9, req.getDeadline());
            ps.setString(10, req.getJobDescription());
            ps.setString(11, req.getJobRequirements());
            ps.setString(12, req.getSalaryExplanation());
            ps.setString(13, req.getStatus());
            ps.setString(14, req.getRecruiterId());
            ps.setString(15, req.getHiringManagerId());
            ps.setString(16, req.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật yêu cầu tuyển dụng: " + req.getId(), e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    public boolean delete(String id) {
        String sql = "DELETE FROM recruitment_requisitions WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi xóa yêu cầu tuyển dụng: " + id, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    public String generateNextCode() {
        int year = Year.now().getValue();
        String prefix = "REQ-" + year + "-";
        String sql = "SELECT code FROM recruitment_requisitions WHERE code LIKE ? ORDER BY code DESC LIMIT 1";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, prefix + "%");
            rs = ps.executeQuery();
            if (rs.next()) {
                String latestCode = rs.getString("code");
                String numPart = latestCode.substring(prefix.length());
                int nextNum = Integer.parseInt(numPart) + 1;
                return String.format("%s%03d", prefix, nextNum);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Lỗi tạo mã yêu cầu tuyển dụng tự động", e);
        } finally {
            close(conn, ps, rs);
        }
        return prefix + "001";
    }

    private RecruitmentRequest mapResultSetToModel(ResultSet rs) throws SQLException {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setId(rs.getString("id"));
        req.setCode(rs.getString("code"));
        req.setTitle(rs.getString("title"));

        try { req.setPositionTitle(rs.getString("position_title")); } catch (SQLException ignored) {}
        if (req.getPositionTitle() == null || req.getPositionTitle().isEmpty()) {
            req.setPositionTitle(req.getTitle());
        }

        req.setDepartmentId(rs.getString("department_id"));
        req.setDepartmentName(rs.getString("department_name"));

        try { req.setHeadcount(rs.getInt("headcount")); } catch (SQLException ignored) { req.setHeadcount(1); }
        if (req.getHeadcount() <= 0) req.setHeadcount(1);

        try { req.setRecruitmentReason(rs.getString("recruitment_reason")); } catch (SQLException ignored) {}
        if (req.getRecruitmentReason() == null) req.setRecruitmentReason("NEW_HEADCOUNT");

        try { req.setMinSalary(rs.getBigDecimal("min_salary")); } catch (SQLException ignored) {}
        try { req.setMaxSalary(rs.getBigDecimal("max_salary")); } catch (SQLException ignored) {}
        try { req.setCurrency(rs.getString("currency")); } catch (SQLException ignored) {}

        try { req.setDeadline(rs.getDate("deadline")); } catch (SQLException ignored) {}
        try { req.setJobDescription(rs.getString("job_description")); } catch (SQLException ignored) {}
        try { req.setJobRequirements(rs.getString("job_requirements")); } catch (SQLException ignored) {}
        try { req.setSalaryExplanation(rs.getString("salary_explanation")); } catch (SQLException ignored) {}

        req.setStatus(rs.getString("status"));
        req.setRecruiterId(rs.getString("recruiter_id"));
        req.setRecruiterName(rs.getString("recruiter_name"));
        req.setHiringManagerId(rs.getString("hiring_manager_id"));
        req.setHiringManagerName(rs.getString("hiring_manager_name"));

        try { req.setCreatedBy(rs.getString("created_by")); } catch (SQLException ignored) {}
        try { req.setCreatedByName(rs.getString("created_by_name")); } catch (SQLException ignored) {}

        req.setCreatedAt(rs.getTimestamp("created_at"));
        req.setUpdatedAt(rs.getTimestamp("updated_at"));
        return req;
    }
}

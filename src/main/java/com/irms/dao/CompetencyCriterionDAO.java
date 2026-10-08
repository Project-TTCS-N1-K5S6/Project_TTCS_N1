package com.irms.dao;

import com.irms.config.DBConnection;
import com.irms.model.CompetencyCriterion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Thao tác dữ liệu bảng Tiêu chí năng lực (competency_criteria)
 */
public class CompetencyCriterionDAO extends BaseDAO {

    static {
        try {
            ensureSchemaUpToDate();
        } catch (Throwable t) {
            java.util.logging.Logger.getLogger(CompetencyCriterionDAO.class.getName())
                    .log(Level.WARNING, "Không thể tự động cập nhật cấu trúc schema competency_criteria: " + t.getMessage());
        }
    }

    public static void ensureSchemaUpToDate() {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            if (conn == null) return;
            DatabaseMetaData md = conn.getMetaData();

            // Kiểm tra cột code
            try (ResultSet rs = md.getColumns(null, null, "competency_criteria", "code")) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE competency_criteria ADD COLUMN code VARCHAR(50) NULL UNIQUE AFTER id");
                    }
                }
            }
            // Kiểm tra cột evaluation_guideline
            try (ResultSet rs = md.getColumns(null, null, "competency_criteria", "evaluation_guideline")) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE competency_criteria ADD COLUMN evaluation_guideline TEXT NULL AFTER description");
                    }
                }
            }
            // Kiểm tra cột status
            try (ResultSet rs = md.getColumns(null, null, "competency_criteria", "status")) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE competency_criteria ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER evaluation_guideline");
                    }
                }
            }
        } catch (Throwable e) {
            java.util.logging.Logger.getLogger(CompetencyCriterionDAO.class.getName())
                    .log(Level.INFO, "[CompetencyCriterionDAO] Cấu trúc schema competency_criteria đã sẵn sàng: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    private CompetencyCriterion mapRow(ResultSet rs) throws SQLException {
        CompetencyCriterion c = new CompetencyCriterion();
        c.setId(rs.getString("id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setUpdatedAt(rs.getTimestamp("updated_at"));

        try {
            c.setCode(rs.getString("code"));
        } catch (SQLException ignored) {}

        try {
            c.setEvaluationGuideline(rs.getString("evaluation_guideline"));
        } catch (SQLException ignored) {}

        try {
            String status = rs.getString("status");
            c.setStatus(status != null ? status : "ACTIVE");
        } catch (SQLException ignored) {
            c.setStatus("ACTIVE");
        }

        return c;
    }

    public List<CompetencyCriterion> findAll() {
        List<CompetencyCriterion> list = new ArrayList<>();
        String sql = "SELECT * FROM competency_criteria ORDER BY name ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách tiêu chí năng lực", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public List<CompetencyCriterion> findAllActive() {
        List<CompetencyCriterion> list = new ArrayList<>();
        String sql = "SELECT * FROM competency_criteria WHERE status = 'ACTIVE' OR status IS NULL ORDER BY name ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách tiêu chí năng lực đang hoạt động", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public CompetencyCriterion findById(String id) {
        String sql = "SELECT * FROM competency_criteria WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm tiêu chí năng lực theo ID: " + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public CompetencyCriterion findByCode(String code) {
        if (code == null || code.trim().isEmpty()) return null;
        String sql = "SELECT * FROM competency_criteria WHERE LOWER(code) = LOWER(?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, code.trim());
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm tiêu chí năng lực theo mã: " + code, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public boolean existsByCode(String code, String excludeId) {
        if (code == null || code.trim().isEmpty()) return false;
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM competency_criteria WHERE LOWER(code) = ?");
        if (excludeId != null && !excludeId.trim().isEmpty()) {
            sql.append(" AND id != ?");
        }
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql.toString());
            ps.setString(1, code.trim().toLowerCase());
            if (excludeId != null && !excludeId.trim().isEmpty()) {
                ps.setString(2, excludeId.trim());
            }
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi kiểm tra trùng mã tiêu chí", e);
        } finally {
            close(conn, ps, rs);
        }
        return false;
    }

    public boolean insert(CompetencyCriterion c) {
        String sql = "INSERT INTO competency_criteria (id, code, name, description, evaluation_guideline, status) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, c.getId());
            ps.setString(2, c.getCode());
            ps.setString(3, c.getName());
            ps.setString(4, c.getDescription());
            ps.setString(5, c.getEvaluationGuideline());
            ps.setString(6, c.getStatus() != null ? c.getStatus() : "ACTIVE");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Fallback nếu chưa có các cột mới
            try {
                if (conn != null) {
                    try (PreparedStatement fallbackPs = conn.prepareStatement(
                            "INSERT INTO competency_criteria (id, name, description) VALUES (?, ?, ?)")) {
                        fallbackPs.setString(1, c.getId());
                        fallbackPs.setString(2, c.getName());
                        fallbackPs.setString(3, c.getDescription());
                        return fallbackPs.executeUpdate() > 0;
                    }
                }
            } catch (SQLException ignored) {}
            logger.log(Level.SEVERE, "Lỗi thêm mới tiêu chí", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    public boolean update(CompetencyCriterion c) {
        String sql = "UPDATE competency_criteria SET code = ?, name = ?, description = ?, evaluation_guideline = ?, status = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, c.getCode());
            ps.setString(2, c.getName());
            ps.setString(3, c.getDescription());
            ps.setString(4, c.getEvaluationGuideline());
            ps.setString(5, c.getStatus() != null ? c.getStatus() : "ACTIVE");
            ps.setString(6, c.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // Fallback nếu chưa có các cột mới
            try {
                if (conn != null) {
                    try (PreparedStatement fallbackPs = conn.prepareStatement(
                            "UPDATE competency_criteria SET name = ?, description = ? WHERE id = ?")) {
                        fallbackPs.setString(1, c.getName());
                        fallbackPs.setString(2, c.getDescription());
                        fallbackPs.setString(3, c.getId());
                        return fallbackPs.executeUpdate() > 0;
                    }
                }
            } catch (SQLException ignored) {}
            logger.log(Level.SEVERE, "Lỗi cập nhật tiêu chí", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    public boolean delete(String id) {
        String sql = "DELETE FROM competency_criteria WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi xóa tiêu chí: " + id, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }
}

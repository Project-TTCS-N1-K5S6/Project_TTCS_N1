package com.irms.dao;

import com.irms.model.SalaryRange;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Thao tác truy vấn và quản lý Dải lương theo vị trí chức danh (bảng salary_ranges)
 * Hỗ trợ Khai báo dải lương, Tra cứu theo cấp bậc và Kiểm soát hạn mức duyệt Offer (KN-103)
 */
public class SalaryRangeDAO extends BaseDAO {

    // Tự động kiểm tra và nâng cấp cấu trúc bảng nếu thiếu cột position_code, level, note
    static {
        try {
            ensureSchemaUpToDate();
        } catch (Throwable t) {
            java.util.logging.Logger.getLogger(SalaryRangeDAO.class.getName())
                    .log(Level.WARNING, "Không thể tự động cập nhật cấu trúc schema salary_ranges: " + t.getMessage());
        }
    }

    private static void ensureSchemaUpToDate() {
        Connection conn = null;
        try {
            conn = com.irms.config.DBConnection.getConnection();
            if (conn == null) return;
            DatabaseMetaData md = conn.getMetaData();
            
            // Kiểm tra cột position_code
            try (ResultSet rs = md.getColumns(null, null, "salary_ranges", "position_code")) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE salary_ranges ADD COLUMN position_code VARCHAR(50) NULL AFTER id");
                    }
                }
            }
            // Kiểm tra cột level
            try (ResultSet rs = md.getColumns(null, null, "salary_ranges", "level")) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE salary_ranges ADD COLUMN level VARCHAR(50) NULL AFTER position_title");
                    }
                }
            }
            // Kiểm tra cột note
            try (ResultSet rs = md.getColumns(null, null, "salary_ranges", "note")) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("ALTER TABLE salary_ranges ADD COLUMN note TEXT NULL AFTER currency");
                    }
                }
            }
        } catch (Throwable e) {
            java.util.logging.Logger.getLogger(SalaryRangeDAO.class.getName())
                    .log(Level.INFO, "[SalaryRangeDAO] Cấu trúc schema salary_ranges đã sẵn sàng: " + e.getMessage());
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Lấy toàn bộ danh sách dải lương không lọc
     */
    public List<SalaryRange> findAll() {
        return findAll(null, null, null);
    }

    /**
     * Lấy danh sách dải lương có hỗ trợ tìm kiếm và bộ lọc:
     * @param search Từ khóa tìm kiếm theo mã hoặc tên chức danh
     * @param departmentId Lọc theo phòng ban
     * @param level Lọc theo cấp bậc (Junior, Senior, Lead, Manager...)
     */
    public List<SalaryRange> findAll(String search, String departmentId, String level) {
        List<SalaryRange> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT s.*, d.name AS department_name FROM salary_ranges s " +
                "LEFT JOIN departments d ON s.department_id = d.id WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (LOWER(s.position_title) LIKE ? OR LOWER(s.position_code) LIKE ?) ");
            String term = "%" + search.trim().toLowerCase() + "%";
            params.add(term);
            params.add(term);
        }

        if (departmentId != null && !departmentId.trim().isEmpty()) {
            sql.append("AND s.department_id = ? ");
            params.add(departmentId.trim());
        }

        if (level != null && !level.trim().isEmpty()) {
            sql.append("AND s.level = ? ");
            params.add(level.trim());
        }

        sql.append("ORDER BY s.position_title ASC, s.min_salary ASC, s.created_at DESC");

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
                list.add(mapResultSetToSalaryRange(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách dải lương", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Tìm dải lương theo phòng ban và chức danh (dùng để kiểm tra hợp lệ khi tạo yêu cầu tuyển dụng)
     */
    public SalaryRange findByDepartmentAndPosition(String departmentId, String positionTitle) {
        if (positionTitle == null || positionTitle.trim().isEmpty()) return null;
        String sql;
        boolean hasDept = (departmentId != null && !departmentId.trim().isEmpty());
        if (hasDept) {
            sql = "SELECT s.*, d.name AS department_name FROM salary_ranges s " +
                  "LEFT JOIN departments d ON s.department_id = d.id " +
                  "WHERE LOWER(TRIM(s.position_title)) = LOWER(TRIM(?)) AND s.department_id = ? " +
                  "LIMIT 1";
        } else {
            sql = "SELECT s.*, d.name AS department_name FROM salary_ranges s " +
                  "LEFT JOIN departments d ON s.department_id = d.id " +
                  "WHERE LOWER(TRIM(s.position_title)) = LOWER(TRIM(?)) " +
                  "LIMIT 1";
        }
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, positionTitle.trim());
            if (hasDept) {
                ps.setString(2, departmentId);
            }
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToSalaryRange(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm dải lương theo vị trí", e);
        } finally {
            close(conn, ps, rs);
        }

        // Nếu có truyền dept mà không khớp, thử tìm fallback chỉ theo positionTitle
        if (hasDept) {
            return findByDepartmentAndPosition(null, positionTitle);
        }
        return null;
    }

    /**
     * Tìm dải lương theo ID
     */
    public SalaryRange findById(String id) {
        String sql = "SELECT s.*, d.name AS department_name FROM salary_ranges s " +
                     "LEFT JOIN departments d ON s.department_id = d.id WHERE s.id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToSalaryRange(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm dải lương theo id=" + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * Tìm dải lương theo mã chức danh (position_code)
     */
    public SalaryRange findByPositionCode(String positionCode) {
        if (positionCode == null || positionCode.trim().isEmpty()) return null;
        String sql = "SELECT s.*, d.name AS department_name FROM salary_ranges s " +
                     "LEFT JOIN departments d ON s.department_id = d.id WHERE LOWER(s.position_code) = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, positionCode.trim().toLowerCase());
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToSalaryRange(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm dải lương theo mã chức danh=" + positionCode, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * Kiểm tra xem mã chức danh đã tồn tại chưa (trừ ID hiện tại khi sửa)
     */
    public boolean existsByPositionCode(String positionCode, String excludeId) {
        if (positionCode == null || positionCode.trim().isEmpty()) return false;
        StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM salary_ranges WHERE LOWER(position_code) = ?");
        if (excludeId != null && !excludeId.trim().isEmpty()) {
            sql.append(" AND id != ?");
        }
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql.toString());
            ps.setString(1, positionCode.trim().toLowerCase());
            if (excludeId != null && !excludeId.trim().isEmpty()) {
                ps.setString(2, excludeId.trim());
            }
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi kiểm tra mã chức danh=" + positionCode, e);
        } finally {
            close(conn, ps, rs);
        }
        return false;
    }

    /**
     * Tìm dải lương phù hợp theo tên chức danh và cấp bậc (dùng tra cứu hạn mức duyệt Offer)
     */
    public SalaryRange findByPositionAndLevel(String positionTitle, String level) {
        if (positionTitle == null || level == null) return null;
        String sql = "SELECT s.*, d.name AS department_name FROM salary_ranges s " +
                     "LEFT JOIN departments d ON s.department_id = d.id " +
                     "WHERE LOWER(s.position_title) = ? AND LOWER(s.level) = ? LIMIT 1";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, positionTitle.trim().toLowerCase());
            ps.setString(2, level.trim().toLowerCase());
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapResultSetToSalaryRange(rs);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tra cứu hạn mức theo chức danh và cấp bậc", e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * Lấy danh sách các cấp bậc chức danh hiện có trong hệ thống
     */
    public List<String> getDistinctLevels() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT level FROM salary_ranges WHERE level IS NOT NULL AND level != '' ORDER BY level ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("level"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách cấp bậc", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Thêm mới một dải lương chức danh
     */
    public boolean insert(SalaryRange sr) {
        String sql = "INSERT INTO salary_ranges (id, position_code, position_title, level, department_id, min_salary, max_salary, currency, note) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, sr.getId());
            ps.setString(2, sr.getPositionCode());
            ps.setString(3, sr.getPositionTitle());
            ps.setString(4, sr.getLevel());
            ps.setString(5, sr.getDepartmentId());
            ps.setBigDecimal(6, sr.getMinSalary());
            ps.setBigDecimal(7, sr.getMaxSalary());
            ps.setString(8, sr.getCurrency() != null ? sr.getCurrency() : "VND");
            ps.setString(9, sr.getNote());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi thêm dải lương mới", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Cập nhật thông tin dải lương chức danh
     */
    public boolean update(SalaryRange sr) {
        String sql = "UPDATE salary_ranges SET position_code = ?, position_title = ?, level = ?, " +
                     "department_id = ?, min_salary = ?, max_salary = ?, currency = ?, note = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, sr.getPositionCode());
            ps.setString(2, sr.getPositionTitle());
            ps.setString(3, sr.getLevel());
            ps.setString(4, sr.getDepartmentId());
            ps.setBigDecimal(5, sr.getMinSalary());
            ps.setBigDecimal(6, sr.getMaxSalary());
            ps.setString(7, sr.getCurrency() != null ? sr.getCurrency() : "VND");
            ps.setString(8, sr.getNote());
            ps.setString(9, sr.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật dải lương id=" + sr.getId(), e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Xóa dải lương chức danh theo ID
     */
    public boolean delete(String id) {
        String sql = "DELETE FROM salary_ranges WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi xóa dải lương id=" + id, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    private SalaryRange mapResultSetToSalaryRange(ResultSet rs) throws SQLException {
        SalaryRange sr = new SalaryRange();
        sr.setId(rs.getString("id"));
        
        try { sr.setPositionCode(rs.getString("position_code")); } catch (SQLException ignored) {}
        sr.setPositionTitle(rs.getString("position_title"));
        try { sr.setLevel(rs.getString("level")); } catch (SQLException ignored) {}
        
        sr.setDepartmentId(rs.getString("department_id"));
        try { sr.setDepartmentName(rs.getString("department_name")); } catch (SQLException ignored) {}
        
        sr.setMinSalary(rs.getBigDecimal("min_salary"));
        sr.setMaxSalary(rs.getBigDecimal("max_salary"));
        sr.setCurrency(rs.getString("currency"));
        try { sr.setNote(rs.getString("note")); } catch (SQLException ignored) {}
        
        sr.setCreatedAt(rs.getTimestamp("created_at"));
        sr.setUpdatedAt(rs.getTimestamp("updated_at"));
        return sr;
    }
}

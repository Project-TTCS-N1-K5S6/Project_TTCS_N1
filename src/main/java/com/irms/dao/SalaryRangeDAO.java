package com.irms.dao;

import com.irms.model.SalaryRange;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Thao tác truy vấn và quản lý Dải lương theo vị trí (bảng salary_ranges)
 */
public class SalaryRangeDAO extends BaseDAO {

    public List<SalaryRange> findAll() {
        List<SalaryRange> list = new ArrayList<>();
        String sql = "SELECT s.*, d.name AS department_name FROM salary_ranges s " +
                     "LEFT JOIN departments d ON s.department_id = d.id " +
                     "ORDER BY s.created_at DESC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                SalaryRange sr = new SalaryRange();
                sr.setId(rs.getString("id"));
                sr.setDepartmentId(rs.getString("department_id"));
                sr.setDepartmentName(rs.getString("department_name"));
                sr.setPositionTitle(rs.getString("position_title"));
                sr.setMinSalary(rs.getBigDecimal("min_salary"));
                sr.setMaxSalary(rs.getBigDecimal("max_salary"));
                sr.setCurrency(rs.getString("currency"));
                sr.setCreatedAt(rs.getTimestamp("created_at"));
                sr.setUpdatedAt(rs.getTimestamp("updated_at"));
                list.add(sr);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách dải lương", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

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
                SalaryRange sr = new SalaryRange();
                sr.setId(rs.getString("id"));
                sr.setDepartmentId(rs.getString("department_id"));
                sr.setDepartmentName(rs.getString("department_name"));
                sr.setPositionTitle(rs.getString("position_title"));
                sr.setMinSalary(rs.getBigDecimal("min_salary"));
                sr.setMaxSalary(rs.getBigDecimal("max_salary"));
                sr.setCurrency(rs.getString("currency"));
                sr.setCreatedAt(rs.getTimestamp("created_at"));
                sr.setUpdatedAt(rs.getTimestamp("updated_at"));
                return sr;
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

    public boolean insert(SalaryRange sr) {
        String sql = "INSERT INTO salary_ranges (id, department_id, position_title, min_salary, max_salary, currency) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, sr.getId());
            ps.setString(2, sr.getDepartmentId());
            ps.setString(3, sr.getPositionTitle());
            ps.setBigDecimal(4, sr.getMinSalary());
            ps.setBigDecimal(5, sr.getMaxSalary());
            ps.setString(6, sr.getCurrency() != null ? sr.getCurrency() : "VND");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi thêm dải lương mới", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }
}

package com.irms.dao;

import com.irms.model.Department;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Thao tác truy vấn và quản lý Phòng ban (bảng departments)
 */
public class DepartmentDAO extends BaseDAO {

    public List<Department> findAll() {
        List<Department> list = new ArrayList<>();
        String sql = "SELECT d.*, COUNT(u.id) AS user_count FROM departments d " +
                     "LEFT JOIN users u ON d.id = u.department_id " +
                     "GROUP BY d.id, d.code, d.name, d.description, d.created_at, d.updated_at " +
                     "ORDER BY d.name ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                Department d = new Department(
                        rs.getString("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("description")
                );
                d.setUserCount(rs.getInt("user_count"));
                d.setCreatedAt(rs.getTimestamp("created_at"));
                d.setUpdatedAt(rs.getTimestamp("updated_at"));
                list.add(d);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách phòng ban", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public Department findById(String id) {
        String sql = "SELECT * FROM departments WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                return new Department(
                        rs.getString("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("description")
                );
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm phòng ban theo ID: " + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    public boolean insert(Department dept) {
        String sql = "INSERT INTO departments (id, code, name, description) VALUES (?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, dept.getId());
            ps.setString(2, dept.getCode());
            ps.setString(3, dept.getName());
            ps.setString(4, dept.getDescription());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi thêm mới phòng ban", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    public boolean update(Department dept) {
        String sql = "UPDATE departments SET code = ?, name = ?, description = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, dept.getCode());
            ps.setString(2, dept.getName());
            ps.setString(3, dept.getDescription());
            ps.setString(4, dept.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật phòng ban", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    public boolean delete(String id) {
        String sql = "DELETE FROM departments WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi xóa phòng ban", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }
}

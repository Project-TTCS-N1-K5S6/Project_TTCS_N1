package com.irms.dao;

import com.irms.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Thao tác truy vấn dữ liệu Vai trò (bảng roles)
 */
public class RoleDAO extends BaseDAO {

    public List<Role> findAll() {
        List<Role> list = new ArrayList<>();
        String sql = "SELECT r.*, COUNT(ur.user_id) AS user_count FROM roles r " +
                     "LEFT JOIN user_roles ur ON r.id = ur.role_id " +
                     "GROUP BY r.id, r.code, r.name, r.description, r.is_system_role, r.created_at, r.updated_at " +
                     "ORDER BY r.created_at ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                Role r = new Role(
                        rs.getString("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBoolean("is_system_role")
                );
                r.setUserCount(rs.getInt("user_count"));
                r.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(r);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách vai trò", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public Role findById(String id) {
        String sql = "SELECT * FROM roles WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                return new Role(
                        rs.getString("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getBoolean("is_system_role")
                );
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tìm vai trò theo ID: " + id, e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }
}

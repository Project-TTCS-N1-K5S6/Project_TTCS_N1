package com.irms.dao;

import com.irms.model.Permission;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Thao tác truy vấn và cập nhật Quyền hạn & Ma trận phân quyền (permissions & role_permissions)
 */
public class PermissionDAO extends BaseDAO {

    public List<Permission> findAll() {
        List<Permission> list = new ArrayList<>();
        String sql = "SELECT * FROM permissions ORDER BY module ASC, code ASC";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Permission(
                        rs.getString("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("module"),
                        rs.getString("action"),
                        rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách quyền hạn", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public List<String> findPermissionIdsByRoleId(String roleId) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT permission_id FROM role_permissions WHERE role_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, roleId);
            rs = ps.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("permission_id"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy permission_ids theo roleId: " + roleId, e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Lấy toàn bộ quan hệ (roleId -> danh sách permissionId) phục vụ ma trận
     */
    public Map<String, Set<String>> getRolePermissionsMap() {
        Map<String, Set<String>> map = new HashMap<>();
        String sql = "SELECT role_id, permission_id FROM role_permissions";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                String roleId = rs.getString("role_id");
                String permId = rs.getString("permission_id");
                map.computeIfAbsent(roleId, k -> new HashSet<>()).add(permId);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi nạp ma trận phân quyền", e);
        } finally {
            close(conn, ps, rs);
        }
        return map;
    }

    /**
     * Cập nhật danh sách quyền cho một vai trò
     */
    public boolean updateRolePermissions(String roleId, List<String> permissionIds) {
        String sqlDelete = "DELETE FROM role_permissions WHERE role_id = ?";
        String sqlInsert = "INSERT INTO role_permissions (role_id, permission_id) VALUES (?, ?)";

        Connection conn = null;
        PreparedStatement psDel = null;
        PreparedStatement psIns = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psDel = conn.prepareStatement(sqlDelete);
            psDel.setString(1, roleId);
            psDel.executeUpdate();

            if (permissionIds != null && !permissionIds.isEmpty()) {
                psIns = conn.prepareStatement(sqlInsert);
                for (String pId : permissionIds) {
                    psIns.setString(1, roleId);
                    psIns.setString(2, pId.trim());
                    psIns.addBatch();
                }
                psIns.executeBatch();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { logger.log(Level.SEVERE, "Rollback failed", ex); }
            }
            logger.log(Level.SEVERE, "Lỗi cập nhật phân quyền cho vai trò: " + roleId, e);
            return false;
        } finally {
            close(null, psDel);
            close(conn, psIns);
        }
    }
}

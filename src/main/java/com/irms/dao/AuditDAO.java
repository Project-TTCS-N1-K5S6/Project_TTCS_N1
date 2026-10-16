package com.irms.dao;

import com.irms.model.AuditLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Thao tác ghi và tra cứu Nhật ký kiểm toán bảo mật (bảng audit_logs)
 */
public class AuditDAO extends BaseDAO {

    public boolean insert(AuditLog log) {
        String sql = "INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, description, ip_address, user_agent) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, log.getId());
            ps.setString(2, log.getUserId());
            ps.setString(3, log.getAction());
            ps.setString(4, log.getEntityType());
            ps.setString(5, log.getEntityId());
            ps.setString(6, log.getDescription());
            ps.setString(7, log.getIpAddress());
            ps.setString(8, log.getUserAgent());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi ghi nhật ký kiểm toán", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    public List<AuditLog> findAll(String action, int offset, int limit) {
        List<AuditLog> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT a.*, u.email AS user_email, u.full_name AS user_full_name " +
                "FROM audit_logs a LEFT JOIN users u ON a.user_id = u.id WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (action != null && !action.trim().isEmpty()) {
            sql.append("AND a.action LIKE ? ");
            params.add("%" + action.trim() + "%");
        }
        sql.append("ORDER BY a.created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

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
                AuditLog log = new AuditLog();
                log.setId(rs.getString("id"));
                log.setUserId(rs.getString("user_id"));
                log.setUserEmail(rs.getString("user_email"));
                log.setUserFullName(rs.getString("user_full_name"));
                log.setAction(rs.getString("action"));
                log.setEntityType(rs.getString("entity_type"));
                log.setEntityId(rs.getString("entity_id"));
                log.setDescription(rs.getString("description"));
                log.setIpAddress(rs.getString("ip_address"));
                log.setUserAgent(rs.getString("user_agent"));
                log.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(log);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi lấy danh sách nhật ký kiểm toán", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public int countAll(String action) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM audit_logs WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (action != null && !action.trim().isEmpty()) {
            sql.append("AND action LIKE ? ");
            params.add("%" + action.trim() + "%");
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
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đếm số lượng nhật ký kiểm toán", e);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }
}

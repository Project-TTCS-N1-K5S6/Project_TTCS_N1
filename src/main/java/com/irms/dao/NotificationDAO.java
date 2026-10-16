package com.irms.dao;

import com.irms.model.InAppNotification;
import com.irms.util.SecurityUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Thao tác dữ liệu thông báo nội bộ hệ thống (KN-365)
 */
public class NotificationDAO extends BaseDAO {

    public boolean insert(InAppNotification notif) {
        String sql = "INSERT INTO in_app_notifications (id, user_id, title, content, type, reference_id, is_read, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, FALSE, CURRENT_TIMESTAMP)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, notif.getId() != null ? notif.getId() : SecurityUtil.generateUUID());
            ps.setString(2, notif.getUserId());
            ps.setString(3, notif.getTitle());
            ps.setString(4, notif.getContent());
            ps.setString(5, notif.getType() != null ? notif.getType() : "SLA_OVERDUE");
            ps.setString(6, notif.getReferenceId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tạo thông báo in-app cho user: " + notif.getUserId(), e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    public List<InAppNotification> findByUserId(String userId, int limit) {
        List<InAppNotification> list = new ArrayList<>();
        String sql = "SELECT * FROM in_app_notifications WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, userId);
            ps.setInt(2, limit);
            rs = ps.executeQuery();
            while (rs.next()) {
                InAppNotification n = new InAppNotification();
                n.setId(rs.getString("id"));
                n.setUserId(rs.getString("user_id"));
                n.setTitle(rs.getString("title"));
                n.setContent(rs.getString("content"));
                n.setType(rs.getString("type"));
                n.setReferenceId(rs.getString("reference_id"));
                n.setRead(rs.getBoolean("is_read"));
                n.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(n);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi truy vấn thông báo của user: " + userId, e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    public int countUnreadByUserId(String userId) {
        String sql = "SELECT COUNT(*) FROM in_app_notifications WHERE user_id = ? AND is_read = FALSE";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, userId);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đếm số thông báo chưa đọc của user: " + userId, e);
        } finally {
            close(conn, ps, rs);
        }
        return 0;
    }

    public boolean markAsRead(String notificationId, String userId) {
        String sql = "UPDATE in_app_notifications SET is_read = TRUE WHERE id = ? AND user_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, notificationId);
            ps.setString(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đánh dấu đã đọc thông báo: " + notificationId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    public boolean markAllAsRead(String userId) {
        String sql = "UPDATE in_app_notifications SET is_read = TRUE WHERE user_id = ? AND is_read = FALSE";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đánh dấu đã đọc tất cả thông báo của user: " + userId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }
}

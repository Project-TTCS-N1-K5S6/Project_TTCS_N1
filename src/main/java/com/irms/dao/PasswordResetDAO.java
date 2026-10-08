package com.irms.dao;

import com.irms.util.SecurityUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

/**
 * ==============================================================================
 * DAO XỬ LÝ TOKEN ĐẶT LẠI MẬT KHẨU & HÀNG ĐỢI EMAIL (US 3 & US 8)
 * ==============================================================================
 * Quản lý bảng:
 * - password_reset_tokens: Token đặt lại mật khẩu có hạn 30 phút, dùng 1 lần.
 * - email_outbox: Hàng đợi gửi email cho người dùng (kích hoạt, reset pass).
 * ==============================================================================
 */
public class PasswordResetDAO extends BaseDAO {

    /**
     * [US 3]: Tạo và lưu token đặt lại mật khẩu có hiệu lực 30 phút
     */
    public boolean createResetToken(String userId, String token) {
        String sql = "INSERT INTO password_reset_tokens (id, user_id, token_hash, expires_at) " +
                     "VALUES (?, ?, ?, NOW() + INTERVAL '30 minutes')";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, SecurityUtil.generateUUID());
            ps.setString(2, userId);
            ps.setString(3, token);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi tạo password reset token cho user: " + userId, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * [US 3]: Kiểm tra tính hợp lệ của Token đặt lại mật khẩu:
     * - Token tồn tại.
     * - Chưa từng được sử dụng (used_at IS NULL).
     * - Chưa hết hạn 30 phút (expires_at > NOW()).
     */
    public String findValidUserIdByToken(String token) {
        String sql = "SELECT user_id FROM password_reset_tokens " +
                     "WHERE token_hash = ? AND used_at IS NULL AND expires_at > NOW()";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, token);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("user_id");
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi kiểm tra token đặt lại mật khẩu", e);
        } finally {
            close(conn, ps, rs);
        }
        return null;
    }

    /**
     * [US 3]: Đánh dấu Token đã được sử dụng (chỉ được dùng 1 lần duy nhất)
     */
    public boolean markTokenAsUsed(String token) {
        String sql = "UPDATE password_reset_tokens SET used_at = NOW() WHERE token_hash = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, token);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi đánh dấu token đã dùng", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * [US 3 & US 8]: Thêm email vào hàng đợi gửi đi (email_outbox)
     */
    public boolean queueEmail(String recipient, String subject, String template, String payloadJson) {
        String sql = "INSERT INTO email_outbox (id, recipient, subject, template, payload, status) " +
                     "VALUES (?, ?, ?, ?, ?, 'PENDING')";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, SecurityUtil.generateUUID());
            ps.setString(2, recipient);
            ps.setString(3, subject);
            ps.setString(4, template);
            ps.setString(5, payloadJson);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi ghi email vào hàng đợi outbox", e);
            return false;
        } finally {
            close(conn, ps);
        }
    }

    /**
     * Lấy danh sách email đang chờ gửi (status = 'PENDING' hoặc 'FAILED' với retry < 3)
     */
    public java.util.List<com.irms.model.EmailOutboxItem> findPendingEmails(int limit) {
        String sql = "SELECT id, recipient, subject, template, payload, status, retry_count, created_at " +
                     "FROM email_outbox WHERE status = 'PENDING' OR (status = 'FAILED' AND retry_count < 3) " +
                     "ORDER BY created_at ASC LIMIT ?";
        java.util.List<com.irms.model.EmailOutboxItem> list = new java.util.ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, limit);
            rs = ps.executeQuery();
            while (rs.next()) {
                com.irms.model.EmailOutboxItem item = new com.irms.model.EmailOutboxItem();
                item.setId(rs.getString("id"));
                item.setRecipient(rs.getString("recipient"));
                item.setSubject(rs.getString("subject"));
                item.setTemplate(rs.getString("template"));
                item.setPayload(rs.getString("payload"));
                item.setStatus(rs.getString("status"));
                item.setRetryCount(rs.getInt("retry_count"));
                item.setCreatedAt(rs.getTimestamp("created_at"));
                list.add(item);
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi truy vấn email pending từ outbox", e);
        } finally {
            close(conn, ps, rs);
        }
        return list;
    }

    /**
     * Cập nhật trạng thái email sau khi thực hiện gửi
     */
    public boolean updateEmailStatus(String id, String status, String lastError) {
        String sql;
        if ("SENT".equalsIgnoreCase(status)) {
            sql = "UPDATE email_outbox SET status = 'SENT', sent_at = CURRENT_TIMESTAMP, last_error = NULL WHERE id = ?";
        } else {
            sql = "UPDATE email_outbox SET status = 'FAILED', last_error = ?, retry_count = retry_count + 1 WHERE id = ?";
        }

        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            if ("SENT".equalsIgnoreCase(status)) {
                ps.setString(1, id);
            } else {
                ps.setString(1, lastError);
                ps.setString(2, id);
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Lỗi cập nhật trạng thái email_outbox id: " + id, e);
            return false;
        } finally {
            close(conn, ps);
        }
    }
}


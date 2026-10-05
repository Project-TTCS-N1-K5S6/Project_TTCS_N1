package com.irms.service;

import com.irms.config.AppConfig;
import com.irms.dao.AuditDAO;
import com.irms.dao.UserDAO;
import com.irms.model.AuditLog;
import com.irms.model.User;
import com.irms.util.PasswordUtil;
import com.irms.util.SecurityUtil;

import java.sql.Timestamp;

/**
 * Xử lý nghiệp vụ Xác thực, Đăng nhập, Khóa tài khoản và Đổi mật khẩu
 */
public class AuthService {
    private final UserDAO userDAO = new UserDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    private final int maxFailedAttempts = AppConfig.getInt("security.maxFailedAttempts", 5);
    private final int lockoutMinutes = AppConfig.getInt("security.lockoutDurationMinutes", 15);

    /**
     * Xác thực thông tin đăng nhập người dùng
     */
    public User login(String email, String password, String ip, String userAgent) throws Exception {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new Exception("Vui lòng nhập đầy đủ Email và Mật khẩu.");
        }

        User user = userDAO.findByEmail(email.trim());
        if (user == null) {
            // Không tiết lộ email có tồn tại hay không nhằm phòng ngừa User Enumeration
            throw new Exception("Email hoặc mật khẩu không chính xác.");
        }

        // Kiểm tra xem tài khoản có đang bị khóa không
        if ("LOCKED".equalsIgnoreCase(user.getStatus())) {
            if (user.getLockedUntil() != null && user.getLockedUntil().after(new Timestamp(System.currentTimeMillis()))) {
                long remainingMins = (user.getLockedUntil().getTime() - System.currentTimeMillis()) / (60 * 1000) + 1;
                throw new Exception("Tài khoản đang bị tạm khóa do nhập sai nhiều lần. Vui lòng thử lại sau " + remainingMins + " phút.");
            } else if (user.getLockedUntil() == null) {
                throw new Exception("Tài khoản của bạn đã bị Quản trị viên khóa. Lý do: " + (user.getLockReason() != null ? user.getLockReason() : "Chưa xác định"));
            }
        }

        // Kiểm tra mật khẩu băm BCrypt
        boolean match = PasswordUtil.check(password, user.getPasswordHash());
        if (!match) {
            userDAO.incrementFailedAttempts(user.getEmail(), maxFailedAttempts, lockoutMinutes);
            int currentFails = user.getFailedLoginAttempts() + 1;
            int remaining = maxFailedAttempts - currentFails;

            AuditLog log = new AuditLog();
            log.setId(SecurityUtil.generateUUID());
            log.setUserId(user.getId());
            log.setAction("LOGIN_FAILED");
            log.setEntityType("AUTH");
            log.setEntityId(user.getId());
            log.setDescription("Đăng nhập thất bại (Sai mật khẩu). Số lần sai: " + currentFails);
            log.setIpAddress(ip);
            log.setUserAgent(userAgent);
            auditDAO.insert(log);

            if (remaining > 0) {
                throw new Exception("Mật khẩu không chính xác. Bạn còn " + remaining + " lần thử trước khi tài khoản bị khóa " + lockoutMinutes + " phút.");
            } else {
                throw new Exception("Bạn đã nhập sai mật khẩu quá " + maxFailedAttempts + " lần. Tài khoản đã bị tạm khóa " + lockoutMinutes + " phút.");
            }
        }

        // Đăng nhập thành công: Reset số lần thử sai
        userDAO.resetFailedAttempts(user.getEmail());

        // Ghi nhật ký kiểm toán LOGIN_SUCCESS
        AuditLog log = new AuditLog();
        log.setId(SecurityUtil.generateUUID());
        log.setUserId(user.getId());
        log.setAction("LOGIN_SUCCESS");
        log.setEntityType("AUTH");
        log.setEntityId(user.getId());
        log.setDescription("Đăng nhập thành công vào hệ thống");
        log.setIpAddress(ip);
        log.setUserAgent(userAgent);
        auditDAO.insert(log);

        return user;
    }

    /**
     * Đổi mật khẩu cá nhân
     */
    public boolean changePassword(String userId, String oldPassword, String newPassword, String ip, String userAgent) throws Exception {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new Exception("Không tìm thấy người dùng.");
        }

        if (!PasswordUtil.check(oldPassword, user.getPasswordHash())) {
            throw new Exception("Mật khẩu hiện tại không chính xác.");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new Exception("Mật khẩu mới phải có độ dài tối thiểu 8 ký tự.");
        }

        String newHash = PasswordUtil.hash(newPassword);
        boolean success = userDAO.updatePassword(userId, newHash, false);

        if (success) {
            AuditLog log = new AuditLog();
            log.setId(SecurityUtil.generateUUID());
            log.setUserId(userId);
            log.setAction("PASSWORD_CHANGED");
            log.setEntityType("USER");
            log.setEntityId(userId);
            log.setDescription("Người dùng đã đổi mật khẩu thành công");
            log.setIpAddress(ip);
            log.setUserAgent(userAgent);
            auditDAO.insert(log);
        }

        return success;
    }

    /**
     * Quản trị viên cấp lại mật khẩu cho người dùng
     */
    public boolean resetPasswordByAdmin(String userId, String temporaryPassword, String adminUserId, String ip, String userAgent) throws Exception {
        String newHash = PasswordUtil.hash(temporaryPassword);
        boolean success = userDAO.updatePassword(userId, newHash, true); // must_change_password = true

        if (success) {
            AuditLog log = new AuditLog();
            log.setId(SecurityUtil.generateUUID());
            log.setUserId(adminUserId);
            log.setAction("ADMIN_RESET_PASSWORD");
            log.setEntityType("USER");
            log.setEntityId(userId);
            log.setDescription("Quản trị viên đã cấp lại mật khẩu tạm thời cho người dùng");
            log.setIpAddress(ip);
            log.setUserAgent(userAgent);
            auditDAO.insert(log);
        }

        return success;
    }
}

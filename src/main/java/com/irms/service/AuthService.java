package com.irms.service;

import com.irms.config.AppConfig;
import com.irms.dao.AuditDAO;
import com.irms.dao.UserDAO;
import com.irms.model.AuditLog;
import com.irms.model.User;
import com.irms.util.PasswordUtil;
import com.irms.util.SecurityUtil;

import java.sql.Timestamp;
import java.sql.SQLException;

/**
 * Xử lý nghiệp vụ Xác thực, Đăng nhập, Khóa tài khoản và Đổi mật khẩu
/**
 * ==============================================================================
 * DỊCH VỤ NGHIỆP VỤ XÁC THỰC VÀ BẢO MẬT (AuthService)
 * ==============================================================================
 * Phục vụ các User Story:
 * - US 1: Xác thực email & mật khẩu, khóa tạm 15 phút sau 5 lần sai liên tiếp.
 * - US 4: Đổi mật khẩu cá nhân (Kiểm tra mật khẩu cũ, độ phức tạp mật khẩu mới).
 * - US 8: Quản trị viên cấp lại mật khẩu tạm cho người dùng.
 * - US 10: Chặn đăng nhập khi tài khoản bị khóa vĩnh viễn hoặc khóa tạm.
 * ==============================================================================
 */
public class AuthService {
    private final UserDAO userDAO = new UserDAO();
    private final AuditDAO auditDAO = new AuditDAO();
    private final com.irms.dao.PasswordResetDAO passwordResetDAO = new com.irms.dao.PasswordResetDAO();

    // [US 1]: Cấu hình số lần đăng nhập sai tối đa (mặc định 5 lần) và thời gian khóa tạm (15 phút)
    private final int maxFailedAttempts = AppConfig.getInt("security.maxFailedAttempts", 5);
    private final int lockoutMinutes = AppConfig.getInt("security.lockoutDurationMinutes", 15);

    /**
     * [US 1 & US 10]: Xác thực thông tin đăng nhập người dùng
     * - Tiêu chí US 1:
     *   + Đạt: Khóa tạm 15 phút nếu nhập sai 5 lần liên tiếp.
     *   + Đạt: Hiển thị thông báo chung ("Email hoặc mật khẩu không chính xác"), phòng ngừa User Enumeration.
     *   + Đạt: Ghi AuditLog mọi lần thử sai và thành công kèm IP, User-Agent.
     * - Tiêu chí US 10:
     *   + Đạt: Chặn người dùng nếu status == 'LOCKED'.
     */
    public User login(String email, String password, String ip, String userAgent) throws Exception {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new Exception("Vui lòng nhập đầy đủ Email và Mật khẩu.");
        }

        User user;
        try {
            user = userDAO.findByEmail(email.trim());
        } catch (SQLException e) {
            throw new Exception("Hệ thống tạm thời không thể xác thực đăng nhập. Vui lòng thử lại sau.", e);
        }
        if (user == null) {
            // [US 1]: Không tiết lộ email có tồn tại hay không nhằm phòng ngừa User Enumeration
            throw new Exception("Email hoặc mật khẩu không chính xác.");
        }

        // [US 1 & US 10]: Kiểm tra trạng thái khóa (Khóa tự động do nhập sai hoặc Quản trị viên chủ động khóa)
        if ("LOCKED".equalsIgnoreCase(user.getStatus())) {
            if (user.getLockedUntil() != null && user.getLockedUntil().after(new Timestamp(System.currentTimeMillis()))) {
                long remainingMins = (user.getLockedUntil().getTime() - System.currentTimeMillis()) / (60 * 1000) + 1;
                throw new Exception("Tài khoản đang bị tạm khóa do nhập sai nhiều lần. Vui lòng thử lại sau " + remainingMins + " phút.");
            } else if (user.getLockedUntil() == null) {
                throw new Exception("Tài khoản của bạn đã bị Quản trị viên khóa. Lý do: " + (user.getLockReason() != null ? user.getLockReason() : "Chưa xác định"));
            }
        }

        // [US 1]: Kiểm tra mật khẩu mã hóa BCrypt
        boolean match = PasswordUtil.check(password, user.getPasswordHash());
        if (!match) {
            // Tăng số lần thử sai và kích hoạt khóa 15 phút nếu đạt ngưỡng 5 lần
            userDAO.incrementFailedAttempts(user.getEmail(), maxFailedAttempts, lockoutMinutes);
            int currentFails = user.getFailedLoginAttempts() + 1;

            // Ghi nhật ký kiểm toán hành vi đăng nhập thất bại
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

            if (currentFails >= maxFailedAttempts) {
                throw new Exception("Bạn đã nhập sai thông tin quá " + maxFailedAttempts + " lần. Tài khoản đã bị tạm khóa " + lockoutMinutes + " phút.");
            } else {
                // [US 1 Tiêu chí 2]: Thông báo chung đồng nhất để không lộ email có tồn tại hay không
                throw new Exception("Email hoặc mật khẩu không chính xác.");
            }
        }

        // [US 1]: Đăng nhập thành công -> Reset bộ đếm số lần sai về 0
        userDAO.resetFailedAttempts(user.getEmail());

        // Ghi nhật ký kiểm toán hành vi đăng nhập thành công
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
     * [US 4]: Đổi mật khẩu cá nhân
     * - Bắt buộc nhập mật khẩu hiện tại.
     * - Mật khẩu mới tối thiểu 8 ký tự, có cả chữ và số (Regex: ^(?=.*[a-zA-Z])(?=.*\d).{8,}$)
     * - Cập nhật password_hash và tăng session_version để thu hồi các phiên đăng nhập khác.
     */
    public boolean changePassword(String userId, String oldPassword, String newPassword, String ip, String userAgent) throws Exception {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new Exception("Không tìm thấy người dùng.");
        }

        // Kiểm tra mật khẩu cũ
        if (!PasswordUtil.check(oldPassword, user.getPasswordHash())) {
            throw new Exception("Mật khẩu hiện tại không chính xác.");
        }

        // [US 4]: Kiểm tra độ phức tạp: tối thiểu 8 ký tự, có cả chữ và số
        if (!isValidPasswordComplexity(newPassword)) {
            throw new Exception("Mật khẩu mới phải có tối thiểu 8 ký tự, bao gồm cả chữ và số.");
        }

        String newHash = PasswordUtil.hash(newPassword);
        // Cập nhật mật khẩu mới và hủy cờ must_change_password
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
     * [US 3]: Yêu cầu đặt lại mật khẩu qua Email (Quên mật khẩu)
     * - Tiêu chí US 3:
     *   + Nhận liên kết đặt lại có hiệu lực 30 phút.
     *   + Email không tồn tại vẫn trả về cùng 1 thông báo (User Enumeration Protection).
     *   + Lưu token vào password_reset_tokens và hàng đợi email_outbox.
     */
    public void requestPasswordReset(String email, String appUrl) throws Exception {
        if (email == null || email.trim().isEmpty()) return;

        User user;
        try {
            user = userDAO.findByEmail(email.trim());
        } catch (SQLException e) {
            throw new Exception("Hệ thống tạm thời không thể xử lý yêu cầu. Vui lòng thử lại sau.", e);
        }
        if (user != null) {
            String token = SecurityUtil.generateUUID();
            passwordResetDAO.createResetToken(user.getId(), token);

            String resetLink = appUrl + "/auth/reset-password?token=" + token;
            String payload = "{\"resetLink\":\"" + resetLink + "\", \"fullName\":\"" + user.getFullName() + "\"}";
            passwordResetDAO.queueEmail(user.getEmail(), "Yêu cầu đặt lại mật khẩu IRMS", "RESET_PASSWORD", payload);
            EmailService.triggerOutboxProcessing();
        }
    }

    /**
     * [US 3]: Đặt lại mật khẩu bằng Token từ Email
     * - Tiêu chí US 3:
     *   + Liên kết chỉ dùng được một lần duy nhất (markTokenAsUsed).
     *   + Kiểm tra hạn 30 phút.
     *   + Mật khẩu mới tối thiểu 8 ký tự có chữ và số.
     */
    public boolean resetPasswordWithToken(String token, String newPassword, String ip, String userAgent) throws Exception {
        if (token == null || token.trim().isEmpty()) {
            throw new Exception("Mã token đặt lại mật khẩu không hợp lệ.");
        }

        String userId = passwordResetDAO.findValidUserIdByToken(token.trim());
        if (userId == null) {
            throw new Exception("Liên kết đặt lại mật khẩu không hợp lệ, đã hết hạn 30 phút hoặc đã từng được sử dụng.");
        }

        // [US 4]: Kiểm tra độ phức tạp
        if (!isValidPasswordComplexity(newPassword)) {
            throw new Exception("Mật khẩu mới phải có tối thiểu 8 ký tự, bao gồm cả chữ và số.");
        }

        String newHash = PasswordUtil.hash(newPassword);
        // Cập nhật mật khẩu và tăng session_version
        boolean success = userDAO.updatePassword(userId, newHash, false);
        if (success) {
            // Đánh dấu token đã sử dụng (chỉ dùng 1 lần)
            passwordResetDAO.markTokenAsUsed(token.trim());

            AuditLog log = new AuditLog();
            log.setId(SecurityUtil.generateUUID());
            log.setUserId(userId);
            log.setAction("PASSWORD_RESET_TOKEN");
            log.setEntityType("USER");
            log.setEntityId(userId);
            log.setDescription("Người dùng đã đặt lại mật khẩu thành công qua email link");
            log.setIpAddress(ip);
            log.setUserAgent(userAgent);
            auditDAO.insert(log);
        }
        return success;
    }

    /**
     * [US 8]: Quản trị viên cấp lại mật khẩu cho người dùng
     */
    public boolean resetPasswordByAdmin(String userId, String temporaryPassword, String adminUserId, String ip, String userAgent) throws Exception {
        String newHash = PasswordUtil.hash(temporaryPassword);
        boolean success = userDAO.updatePassword(userId, newHash, true); // must_change_password = true

        if (success) {
            User targetUser = userDAO.findById(userId);
            if (targetUser != null && targetUser.getEmail() != null) {
                String payload = "{\"temporaryPassword\":\"" + temporaryPassword + "\", \"fullName\":\"" + targetUser.getFullName() + "\"}";
                passwordResetDAO.queueEmail(targetUser.getEmail(), "Cấp lại mật khẩu IRMS - Mật khẩu tạm thời mới", "ACCOUNT_ACTIVATION", payload);
                EmailService.triggerOutboxProcessing();
            }

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

    /**
     * [US 3 & US 4]: Kiểm tra quy tắc độ phức tạp của mật khẩu:
     * - Tối thiểu 8 ký tự
     * - Bao gồm cả chữ cái và chữ số
     */
    public static boolean isValidPasswordComplexity(String password) {
        if (password == null) return false;
        return password.matches("^(?=.*[a-zA-Z])(?=.*\\d).{8,}$");
    }
}

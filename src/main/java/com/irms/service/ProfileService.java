package com.irms.service;

import com.irms.dao.UserDAO;
import com.irms.model.User;

public class ProfileService {
    private static final String EMAIL_PATTERN = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

    private final UserDAO userDAO = new UserDAO();
    private final AuditService auditService = new AuditService();

    public User getProfile(String userId) {
        return userDAO.findById(userId);
    }

    public User updateProfile(String userId, String fullName, String phone, String email,
                              String ip, String userAgent) throws Exception {
        String normalizedName = fullName == null ? "" : fullName.trim();
        String normalizedPhone = phone == null ? "" : phone.trim();
        String normalizedEmail = email == null ? "" : email.trim();

        if (normalizedName.isEmpty() || normalizedName.length() > 255) {
            throw new Exception("Họ tên là bắt buộc và không được vượt quá 255 ký tự.");
        }
        if (normalizedPhone.length() > 50) {
            throw new Exception("Số điện thoại không được vượt quá 50 ký tự.");
        }
        if (normalizedEmail.length() > 255 || !normalizedEmail.matches(EMAIL_PATTERN)) {
            throw new Exception("Vui lòng nhập địa chỉ email hợp lệ.");
        }

        User existingUser = userDAO.findById(userId);
        if (existingUser == null) {
            throw new Exception("Không tìm thấy hồ sơ người dùng.");
        }
        if (userDAO.emailExistsForAnotherUser(normalizedEmail, userId)) {
            throw new Exception("Email này đã được sử dụng bởi tài khoản khác.");
        }
        if (!userDAO.updatePersonalProfile(userId, normalizedName, normalizedPhone, normalizedEmail)) {
            throw new Exception("Không thể cập nhật hồ sơ. Vui lòng thử lại.");
        }

        auditService.log(userId, "PROFILE_UPDATED", "USER", userId,
                "Người dùng tự cập nhật hồ sơ cá nhân", ip, userAgent);

        User updatedUser = userDAO.findById(userId);
        if (updatedUser == null) {
            throw new Exception("Không thể tải lại hồ sơ sau khi cập nhật.");
        }
        return updatedUser;
    }
}
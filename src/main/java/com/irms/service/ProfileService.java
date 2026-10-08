package com.irms.service;

import com.irms.dao.UserDAO;
import com.irms.model.User;

public class ProfileService {
    private static final String VIETNAMESE_MOBILE_PATTERN =
            "^(?:0|\\+84)(?:3[2-9]|5(?:2|5|6|8|9)|7[06789]|8[1-9]|9[0-9])[0-9]{7}$";

    private final UserDAO userDAO = new UserDAO();
    private final AuditService auditService = new AuditService();

    public User getProfile(String userId) {
        return userDAO.findById(userId);
    }

    public User updateProfile(String userId, String fullName, String phone, String jobTitle,
                              String ip, String userAgent) throws Exception {
        String normalizedName = fullName == null ? "" : fullName.trim();
        String normalizedPhone = phone == null ? "" : phone.trim();
        String normalizedJobTitle = jobTitle == null ? "" : jobTitle.trim();

        if (normalizedName.isEmpty() || normalizedName.length() > 255) {
            throw new Exception("Họ tên là bắt buộc và không được vượt quá 255 ký tự.");
        }
        if (!normalizedPhone.isEmpty() && !isValidVietnameseMobile(normalizedPhone)) {
            throw new Exception("Số điện thoại phải có định dạng di động Việt Nam hợp lệ (0... hoặc +84...).");
        }
        if (normalizedJobTitle.length() > 100) {
            throw new Exception("Chức danh không được vượt quá 100 ký tự.");
        }

        User existingUser = userDAO.findById(userId);
        if (existingUser == null) {
            throw new Exception("Không tìm thấy hồ sơ người dùng.");
        }
        String phoneForStorage = normalizedPhone.startsWith("+84")
                ? "0" + normalizedPhone.substring(3)
                : normalizedPhone;
        String jobTitleForStorage = normalizedJobTitle.isEmpty() ? null : normalizedJobTitle;
        if (!userDAO.updatePersonalProfile(userId, normalizedName, phoneForStorage, jobTitleForStorage)) {
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

    static boolean isValidVietnameseMobile(String phone) {
        return phone != null && phone.matches(VIETNAMESE_MOBILE_PATTERN);
    }
}
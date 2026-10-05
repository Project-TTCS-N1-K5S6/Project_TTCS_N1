package com.irms.service;

import com.irms.dao.AuditDAO;
import com.irms.dao.UserDAO;
import com.irms.model.AuditLog;
import com.irms.model.User;
import com.irms.util.PasswordUtil;
import com.irms.util.SecurityUtil;

import java.util.List;

/**
 * Xử lý nghiệp vụ Quản lý người dùng, phân quyền và khóa tài khoản
 */
public class UserService {
    private final UserDAO userDAO = new UserDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    public List<User> getUsers(String search, String deptId, String status, int page, int pageSize) {
        int offset = Math.max(0, (page - 1) * pageSize);
        return userDAO.findAll(search, deptId, status, offset, pageSize);
    }

    public int countUsers(String search, String deptId, String status) {
        return userDAO.countAll(search, deptId, status);
    }

    public User getUserById(String id) {
        return userDAO.findById(id);
    }

    public boolean createUser(User user, String roleId, String adminId, String ip, String userAgent) throws Exception {
        if (userDAO.findByEmail(user.getEmail()) != null) {
            throw new Exception("Email '" + user.getEmail() + "' đã tồn tại trong hệ thống.");
        }

        user.setId(SecurityUtil.generateUUID());
        // Thiết lập mật khẩu mặc định nếu chưa có
        String rawPass = "Admin@123456";
        user.setPasswordHash(PasswordUtil.hash(rawPass));
        user.setMustChangePassword(true);

        boolean success = userDAO.insert(user, roleId);
        if (success) {
            AuditLog log = new AuditLog();
            log.setId(SecurityUtil.generateUUID());
            log.setUserId(adminId);
            log.setAction("USER_CREATED");
            log.setEntityType("USER");
            log.setEntityId(user.getId());
            log.setDescription("Tạo mới tài khoản người dùng: " + user.getEmail() + " (" + user.getFullName() + ")");
            log.setIpAddress(ip);
            log.setUserAgent(userAgent);
            auditDAO.insert(log);
        }
        return success;
    }

    public boolean updateUser(User user, String roleId, String adminId, String ip, String userAgent) throws Exception {
        boolean success = userDAO.update(user, roleId);
        if (success) {
            AuditLog log = new AuditLog();
            log.setId(SecurityUtil.generateUUID());
            log.setUserId(adminId);
            log.setAction("USER_UPDATED");
            log.setEntityType("USER");
            log.setEntityId(user.getId());
            log.setDescription("Cập nhật thông tin tài khoản người dùng ID: " + user.getId());
            log.setIpAddress(ip);
            log.setUserAgent(userAgent);
            auditDAO.insert(log);
        }
        return success;
    }

    public boolean lockUser(String userId, String reason, String adminId, String ip, String userAgent) {
        boolean success = userDAO.lockUser(userId, reason, adminId);
        if (success) {
            AuditLog log = new AuditLog();
            log.setId(SecurityUtil.generateUUID());
            log.setUserId(adminId);
            log.setAction("USER_LOCKED");
            log.setEntityType("USER");
            log.setEntityId(userId);
            log.setDescription("Quản trị viên đã khóa tài khoản. Lý do: " + reason);
            log.setIpAddress(ip);
            log.setUserAgent(userAgent);
            auditDAO.insert(log);
        }
        return success;
    }

    public boolean unlockUser(String userId, String adminId, String ip, String userAgent) {
        boolean success = userDAO.unlockUser(userId);
        if (success) {
            AuditLog log = new AuditLog();
            log.setId(SecurityUtil.generateUUID());
            log.setUserId(adminId);
            log.setAction("USER_UNLOCKED");
            log.setEntityType("USER");
            log.setEntityId(userId);
            log.setDescription("Quản trị viên đã mở khóa tài khoản");
            log.setIpAddress(ip);
            log.setUserAgent(userAgent);
            auditDAO.insert(log);
        }
        return success;
    }
}

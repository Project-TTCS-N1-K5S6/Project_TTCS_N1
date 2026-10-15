package com.irms.service;

import com.irms.dao.AuditDAO;
import com.irms.dao.UserDAO;
import com.irms.model.AuditLog;
import com.irms.model.User;
import com.irms.util.PasswordUtil;
import com.irms.util.SecurityUtil;

import java.util.List;
import java.sql.SQLException;

/**
 * ==============================================================================
 * DỊCH VỤ NGHIỆP VỤ QUẢN TRỊ NGƯỜI DÙNG (UserService)
 * ==============================================================================
 * Phục vụ các User Story:
 * - US 8: Tạo, cập nhật, tìm kiếm và phân trang danh sách người dùng.
 * - US 9: Gán/thu hồi vai trò cho người dùng (Đảm bảo hiệu lực & kiểm soát tài khoản admin).
 * - US 10: Khóa và mở khóa tài khoản nhân sự (Ghi nhận lý do, nhật ký kiểm toán).
 * ==============================================================================
 */
public class UserService {
    private final UserDAO userDAO = new UserDAO();
    private final AuditDAO auditDAO = new AuditDAO();
    private final com.irms.dao.PasswordResetDAO passwordResetDAO = new com.irms.dao.PasswordResetDAO();

    public List<User> getUsers(String search, String deptId, String status, int page, int pageSize) {
        return getUsers(search, deptId, status, null, page, pageSize);
    }

    /**
     * [US 8]: Lấy danh sách người dùng có phân trang và bộ lọc tìm kiếm (bao gồm cả vai trò)
     */
    public List<User> getUsers(String search, String deptId, String status, String roleId, int page, int pageSize) {
        int offset = Math.max(0, (page - 1) * pageSize);
        return userDAO.findAll(search, deptId, status, roleId, offset, pageSize);
    }

    public int countUsers(String search, String deptId, String status) {
        return countUsers(search, deptId, status, null);
    }

    /**
     * [US 8]: Đếm tổng số lượng người dùng phục vụ tính toán số trang
     */
    public int countUsers(String search, String deptId, String status, String roleId) {
        return userDAO.countAll(search, deptId, status, roleId);
    }

    public User getUserById(String id) {
        return userDAO.findById(id);
    }

    public boolean createUser(User user, String roleId, String adminId, String ip, String userAgent) throws Exception {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (roleId != null && !roleId.trim().isEmpty()) list.add(roleId.trim());
        return createUser(user, list, adminId, ip, userAgent);
    }

    /**
     * [US 8 & US 9]: Tạo mới tài khoản nội bộ
     * - Tiêu chí US 8:
     *   + Từ chối nếu trùng email kèm thông báo chi tiết: "Email '...' đã tồn tại trong hệ thống."
     *   + Sinh mật khẩu tạm thời ngẫu nhiên và gửi email kích hoạt vào hàng đợi email_outbox.
     *   + Ghi AuditLog USER_CREATED.
     * - Tiêu chí US 9:
     *   + Hỗ trợ gán nhiều vai trò cùng lúc (roleIds).
     */
    public boolean createUser(User user, List<String> roleIds, String adminId, String ip, String userAgent) throws Exception {
        User existingUser;
        try {
            existingUser = userDAO.findByEmail(user.getEmail());
        } catch (SQLException e) {
            throw new Exception("Hệ thống tạm thời không thể kiểm tra tài khoản. Vui lòng thử lại sau.", e);
        }
        if (existingUser != null) {
            throw new Exception("Email '" + user.getEmail() + "' đã tồn tại trong hệ thống.");
        }
        if (user.getEmployeeCode() != null && !user.getEmployeeCode().trim().isEmpty()) {
            if (userDAO.findByEmployeeCode(user.getEmployeeCode()) != null) {
                throw new Exception("Mã nhân viên '" + user.getEmployeeCode() + "' đã tồn tại trong hệ thống.");
            }
        }

        user.setId(SecurityUtil.generateUUID());

        // [US 8]: Sinh mật khẩu tạm ngẫu nhiên bảo mật (Ví dụ: Temp@839201)
        String tempPass = "Temp@" + (int)(Math.random() * 900000 + 100000);
        user.setPasswordHash(PasswordUtil.hash(tempPass));
        user.setMustChangePassword(true);

        boolean success = userDAO.insert(user, roleIds);
        if (success) {
            // [US 8]: Đưa email kích hoạt kèm mật khẩu tạm vào hàng đợi email_outbox
            String payload = "{\"temporaryPassword\":\"" + tempPass + "\", \"fullName\":\"" + user.getFullName() + "\"}";
            passwordResetDAO.queueEmail(user.getEmail(), "Kích hoạt tài khoản IRMS - Mật khẩu tạm thời", "ACCOUNT_ACTIVATION", payload);
            EmailService.triggerOutboxProcessing();

            AuditLog log = new AuditLog();
            log.setId(SecurityUtil.generateUUID());
            log.setUserId(adminId);
            log.setAction("USER_CREATED");
            log.setEntityType("USER");
            log.setEntityId(user.getId());
            log.setDescription("Tạo mới tài khoản người dùng: " + user.getEmail() + " (" + user.getFullName() + ") kèm mật khẩu tạm");
            log.setIpAddress(ip);
            log.setUserAgent(userAgent);
            auditDAO.insert(log);
        }
        return success;
    }

    public boolean updateUser(User user, String roleId, String adminId, String ip, String userAgent) throws Exception {
        java.util.List<String> list = new java.util.ArrayList<>();
        if (roleId != null && !roleId.trim().isEmpty()) list.add(roleId.trim());
        return updateUser(user, list, adminId, ip, userAgent);
    }

    /**
     * [US 9]: Cập nhật thông tin và gán vai trò người dùng
     * - Tiêu chí US 9:
     *   + Một người dùng có thể giữ nhiều vai trò cùng lúc.
     *   + Không thể tự thu hồi vai trò quản trị (ADMIN) của chính mình.
     */
    public boolean updateUser(User user, List<String> roleIds, String adminId, String ip, String userAgent) throws Exception {
        // [US 9 Tiêu chí 3]: Chặn Admin tự thu hồi quyền ADMIN của chính mình
        if (adminId != null && adminId.equals(user.getId())) {
            if ("LOCKED".equalsIgnoreCase(user.getStatus())) {
                throw new Exception("Bạn không thể tự khóa tài khoản của chính mình.");
            }
            boolean keepAdmin = false;
            if (roleIds != null) {
                for (String rId : roleIds) {
                    if ("role-001".equalsIgnoreCase(rId) || "ADMIN".equalsIgnoreCase(rId)) {
                        keepAdmin = true;
                        break;
                    }
                }
            }
            if (!keepAdmin) {
                throw new Exception("Bạn không thể tự thu hồi vai trò Quản trị viên (ADMIN) của chính mình.");
            }
        }

        boolean success = userDAO.update(user, roleIds);
        if (success) {
            AuditLog log = new AuditLog();
            log.setId(SecurityUtil.generateUUID());
            log.setUserId(adminId);
            log.setAction("USER_UPDATED");
            log.setEntityType("USER");
            log.setEntityId(user.getId());
            log.setDescription("Cập nhật thông tin tài khoản người dùng ID: " + user.getId() + " - Số vai trò gán: " + (roleIds != null ? roleIds.size() : 0));
            log.setIpAddress(ip);
            log.setUserAgent(userAgent);
            auditDAO.insert(log);
        }
        return success;
    }

    /**
     * [US 10]: Khóa tài khoản nhân sự
     * - Tiêu chí US 10:
     *   + Bắt buộc ghi lý do khóa (validate không rỗng).
     *   + Không cho phép Admin tự khóa tài khoản của chính mình.
     *   + Kiểm tra xem nhân sự có đang phụ trách các vị trí tuyển dụng mở hay không để cảnh báo bàn giao.
     */
    public int lockUser(String userId, String reason, String adminId, String ip, String userAgent) throws Exception {
        if (adminId != null && adminId.equals(userId)) {
            throw new Exception("Bạn không thể tự khóa tài khoản của chính mình.");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new Exception("Bắt buộc phải nhập lý do khóa tài khoản.");
        }

        // [US 10 Tiêu chí 3]: Kiểm tra số vị trí tuyển dụng do người đó phụ trách
        int activeRequisitions = userDAO.countActiveRequisitionsByRecruiter(userId);

        boolean success = userDAO.lockUser(userId, reason.trim(), adminId);
        if (success) {
            AuditLog log = new AuditLog();
            log.setId(SecurityUtil.generateUUID());
            log.setUserId(adminId);
            log.setAction("USER_LOCKED");
            log.setEntityType("USER");
            log.setEntityId(userId);
            String warnMsg = activeRequisitions > 0 ? " [CẢNH BÁO: Nhân sự đang phụ trách " + activeRequisitions + " vị trí tuyển dụng cần bàn giao!]" : "";
            log.setDescription("Quản trị viên đã khóa tài khoản. Lý do: " + reason.trim() + warnMsg);
            log.setIpAddress(ip);
            log.setUserAgent(userAgent);
            auditDAO.insert(log);
        }
        return activeRequisitions;
    }

    /**
     * [US 10]: Mở khóa tài khoản nhân sự
     */
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

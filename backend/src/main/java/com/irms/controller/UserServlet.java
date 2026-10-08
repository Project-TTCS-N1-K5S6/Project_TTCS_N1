package com.irms.controller;

import com.irms.dao.DepartmentDAO;
import com.irms.dao.RoleDAO;
import com.irms.model.User;
import com.irms.service.AuthService;
import com.irms.service.UserService;
import com.irms.util.SecurityUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * ==============================================================================
 * BỘ ĐIỀU KHIỂN QUẢN TRỊ TÀI KHOẢN NGƯỜI DÙNG (UserServlet)
 * ==============================================================================
 * Phục vụ các User Story:
 * - US 8: Tạo, sửa, tìm kiếm tài khoản nội bộ (Tìm tên/email/phòng ban; lọc vai trò/trạng thái; phân trang mặc định 20 dòng).
 * - US 9: Gán và thu hồi vai trò (1 người nhiều vai trò; không tự thu hồi vai trò admin của chính mình).
 * - US 10: Khóa và mở khóa tài khoản (Bắt buộc lý do; cảnh báo vị trí tuyển dụng phụ trách cần bàn giao).
 * ==============================================================================
 */
@WebServlet(name = "UserServlet", urlPatterns = {
        "/admin/users",
        "/admin/users/create",
        "/admin/users/edit",
        "/admin/users/lock",
        "/admin/users/unlock",
        "/admin/users/reset-password"
})
public class UserServlet extends HttpServlet {
    private final UserService userService = new UserService();
    private final AuthService authService = new AuthService();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final RoleDAO roleDAO = new RoleDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String search = request.getParameter("search");
        String deptId = request.getParameter("deptId");
        String status = request.getParameter("status");
        String roleId = request.getParameter("roleId");

        int page = 1;
        // [US 8 Tiêu chí 4]: Danh sách phân trang, mặc định 20 dòng
        int pageSize = 20; 
        try {
            if (request.getParameter("page") != null) {
                page = Integer.parseInt(request.getParameter("page"));
            }
        } catch (NumberFormatException ignored) {}

        // [US 8 Tiêu chí 3]: Tìm theo tên, email, phòng ban; lọc theo vai trò và trạng thái
        List<User> userList = userService.getUsers(search, deptId, status, roleId, page, pageSize);
        int totalUsers = userService.countUsers(search, deptId, status, roleId);
        int totalPages = (int) Math.ceil((double) totalUsers / pageSize);

        request.setAttribute("users", userList);
        request.setAttribute("departments", departmentDAO.findAll());
        request.setAttribute("roles", roleDAO.findAll());
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalCount", totalUsers);
        request.setAttribute("paramSearch", search);
        request.setAttribute("paramDeptId", deptId);
        request.setAttribute("paramStatus", status);
        request.setAttribute("paramRoleId", roleId);

        request.getRequestDispatcher("/WEB-INF/views/users/list.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String adminId = (currentUser != null) ? currentUser.getId() : "SYSTEM";
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            switch (path) {
                case "/admin/users/create":
                    // [US 8]: Tạo tài khoản người dùng mới kèm mật khẩu tạm và gửi email kích hoạt
                    handleCreateUser(request, adminId, ip, userAgent);
                    session.setAttribute("flashSuccess", "Thêm mới tài khoản người dùng thành công! Mật khẩu tạm thời đã được tạo và gửi email kích hoạt.");
                    break;

                case "/admin/users/edit":
                    // [US 9]: Cập nhật thông tin và gán nhiều vai trò (Chặn tự thu hồi vai trò admin)
                    handleUpdateUser(request, adminId, ip, userAgent);
                    session.setAttribute("flashSuccess", "Cập nhật thông tin tài khoản và vai trò thành công!");
                    break;

                case "/admin/users/lock":
                    // [US 10]: Khóa tài khoản nhân sự (Bắt buộc lý do; cảnh báo bàn giao vị trí nếu có)
                    String lockUserId = request.getParameter("userId");
                    String lockReason = request.getParameter("reason");
                    int activeJobs = userService.lockUser(lockUserId, lockReason, adminId, ip, userAgent);
                    if (activeJobs > 0) {
                        session.setAttribute("flashWarning", "Đã khóa tài khoản thành công. CẢNH BÁO: Nhân sự đang phụ trách " + activeJobs + " vị trí tuyển dụng mở cần được bàn giao ngay!");
                    } else {
                        session.setAttribute("flashSuccess", "Đã khóa tài khoản người dùng thành công!");
                    }
                    break;

                case "/admin/users/unlock":
                    // [US 10]: Mở khóa tài khoản nhân sự trở lại trạng thái ACTIVE
                    String unlockUserId = request.getParameter("userId");
                    userService.unlockUser(unlockUserId, adminId, ip, userAgent);
                    session.setAttribute("flashSuccess", "Đã mở khóa tài khoản thành công!");
                    break;

                case "/admin/users/reset-password":
                    // [US 8]: Quản trị viên cấp lại mật khẩu tạm thời
                    String resetUserId = request.getParameter("userId");
                    String tempPass = "Reset@" + (int)(Math.random() * 900000 + 100000);
                    authService.resetPasswordByAdmin(resetUserId, tempPass, adminId, ip, userAgent);
                    session.setAttribute("flashSuccess", "Đã cấp lại mật khẩu tạm thời: " + tempPass + " (Bắt buộc đổi mật khẩu ở lần đăng nhập tới)");
                    break;
            }
        } catch (Exception e) {
            session.setAttribute("flashError", "Lỗi: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/admin/users");
    }

    /**
     * [US 8 & US 9]: Thu thập tham số form tạo mới người dùng hỗ trợ nhiều vai trò
     */
    private void handleCreateUser(HttpServletRequest request, String adminId, String ip, String userAgent) throws Exception {
        User u = new User();
        u.setEmployeeCode(request.getParameter("employeeCode"));
        u.setFullName(request.getParameter("fullName"));
        u.setEmail(request.getParameter("email"));
        u.setPhone(request.getParameter("phone"));
        u.setJobTitle(request.getParameter("jobTitle"));
        u.setDepartmentId(request.getParameter("departmentId"));
        u.setStatus("ACTIVE");

        String[] roleIds = request.getParameterValues("roleIds");
        java.util.List<String> roleList = new java.util.ArrayList<>();
        if (roleIds != null) {
            for (String r : roleIds) if (r != null && !r.trim().isEmpty()) roleList.add(r.trim());
        } else if (request.getParameter("roleId") != null && !request.getParameter("roleId").trim().isEmpty()) {
            roleList.add(request.getParameter("roleId").trim());
        }

        userService.createUser(u, roleList, adminId, ip, userAgent);
    }

    /**
     * [US 9]: Thu thập tham số form chỉnh sửa người dùng và gán nhiều vai trò
     */
    private void handleUpdateUser(HttpServletRequest request, String adminId, String ip, String userAgent) throws Exception {
        User u = new User();
        u.setId(request.getParameter("userId"));
        u.setFullName(request.getParameter("fullName"));
        u.setPhone(request.getParameter("phone"));
        u.setJobTitle(request.getParameter("jobTitle"));
        u.setDepartmentId(request.getParameter("departmentId"));
        u.setStatus(request.getParameter("status"));

        String[] roleIds = request.getParameterValues("roleIds");
        java.util.List<String> roleList = new java.util.ArrayList<>();
        if (roleIds != null) {
            for (String r : roleIds) if (r != null && !r.trim().isEmpty()) roleList.add(r.trim());
        } else if (request.getParameter("roleId") != null && !request.getParameter("roleId").trim().isEmpty()) {
            roleList.add(request.getParameter("roleId").trim());
        }

        userService.updateUser(u, roleList, adminId, ip, userAgent);
    }
}

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
 * Controller quản trị tài khoản người dùng: CRUD, Khóa, Cấp lại mật khẩu
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

        int page = 1;
        int pageSize = 10;
        try {
            if (request.getParameter("page") != null) {
                page = Integer.parseInt(request.getParameter("page"));
            }
        } catch (NumberFormatException ignored) {}

        List<User> userList = userService.getUsers(search, deptId, status, page, pageSize);
        int totalUsers = userService.countUsers(search, deptId, status);
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
                    handleCreateUser(request, adminId, ip, userAgent);
                    session.setAttribute("flashSuccess", "Thêm mới tài khoản người dùng thành công (Mật khẩu mặc định: Admin@123456)!");
                    break;

                case "/admin/users/edit":
                    handleUpdateUser(request, adminId, ip, userAgent);
                    session.setAttribute("flashSuccess", "Cập nhật thông tin tài khoản thành công!");
                    break;

                case "/admin/users/lock":
                    String lockUserId = request.getParameter("userId");
                    String lockReason = request.getParameter("reason");
                    userService.lockUser(lockUserId, lockReason, adminId, ip, userAgent);
                    session.setAttribute("flashSuccess", "Đã khóa tài khoản người dùng thành công!");
                    break;

                case "/admin/users/unlock":
                    String unlockUserId = request.getParameter("userId");
                    userService.unlockUser(unlockUserId, adminId, ip, userAgent);
                    session.setAttribute("flashSuccess", "Đã mở khóa tài khoản thành công!");
                    break;

                case "/admin/users/reset-password":
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

    private void handleCreateUser(HttpServletRequest request, String adminId, String ip, String userAgent) throws Exception {
        User u = new User();
        u.setEmployeeCode(request.getParameter("employeeCode"));
        u.setFullName(request.getParameter("fullName"));
        u.setEmail(request.getParameter("email"));
        u.setPhone(request.getParameter("phone"));
        u.setJobTitle(request.getParameter("jobTitle"));
        u.setDepartmentId(request.getParameter("departmentId"));
        u.setStatus("ACTIVE");

        String roleId = request.getParameter("roleId");
        userService.createUser(u, roleId, adminId, ip, userAgent);
    }

    private void handleUpdateUser(HttpServletRequest request, String adminId, String ip, String userAgent) throws Exception {
        User u = new User();
        u.setId(request.getParameter("userId"));
        u.setFullName(request.getParameter("fullName"));
        u.setPhone(request.getParameter("phone"));
        u.setJobTitle(request.getParameter("jobTitle"));
        u.setDepartmentId(request.getParameter("departmentId"));
        u.setStatus(request.getParameter("status"));

        String roleId = request.getParameter("roleId");
        userService.updateUser(u, roleId, adminId, ip, userAgent);
    }
}

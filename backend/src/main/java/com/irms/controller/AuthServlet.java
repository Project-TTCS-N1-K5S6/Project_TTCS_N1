package com.irms.controller;

import com.irms.model.User;
import com.irms.service.AuthService;
import com.irms.util.SecurityUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Controller xử lý các chức năng Xác thực: Đăng nhập, Đăng xuất, Đổi mật khẩu, Quên mật khẩu
 */
@WebServlet(name = "AuthServlet", urlPatterns = {
        "/auth/login",
        "/auth/logout",
        "/auth/forgot-password",
        "/auth/change-password"
})
public class AuthServlet extends HttpServlet {
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        switch (path) {
            case "/auth/login":
                // Nếu đã đăng nhập rồi thì vào luôn dashboard
                HttpSession session = request.getSession(false);
                if (session != null && session.getAttribute("currentUser") != null) {
                    response.sendRedirect(request.getContextPath() + "/dashboard");
                    return;
                }
                request.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(request, response);
                break;

            case "/auth/logout":
                handleLogout(request, response);
                break;

            case "/auth/forgot-password":
                request.getRequestDispatcher("/WEB-INF/views/auth/forgot-password.jsp").forward(request, response);
                break;

            case "/auth/change-password":
                request.getRequestDispatcher("/WEB-INF/views/auth/change-password.jsp").forward(request, response);
                break;

            default:
                response.sendRedirect(request.getContextPath() + "/auth/login");
                break;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        switch (path) {
            case "/auth/login":
                handleLogin(request, response);
                break;

            case "/auth/change-password":
                handleChangePassword(request, response);
                break;

            case "/auth/forgot-password":
                handleForgotPassword(request, response);
                break;

            default:
                response.sendRedirect(request.getContextPath() + "/auth/login");
                break;
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String redirect = request.getParameter("redirect");
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            User user = authService.login(email, password, ip, userAgent);

            // Tạo phiên làm việc an toàn
            HttpSession session = request.getSession(true);
            session.setAttribute("currentUser", user);

            if (user.isMustChangePassword()) {
                response.sendRedirect(request.getContextPath() + "/auth/change-password?required=true");
                return;
            }

            if (redirect != null && !redirect.trim().isEmpty() && !redirect.startsWith("/auth/")) {
                response.sendRedirect(request.getContextPath() + redirect);
            } else {
                response.sendRedirect(request.getContextPath() + "/dashboard");
            }
        } catch (Exception e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("inputEmail", email);
            request.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(request, response);
        }
    }

    private void handleChangePassword(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("currentUser") == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        User currentUser = (User) session.getAttribute("currentUser");
        String oldPassword = request.getParameter("oldPassword");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        if (!newPassword.equals(confirmPassword)) {
            request.setAttribute("errorMessage", "Xác nhận mật khẩu mới không trùng khớp.");
            request.getRequestDispatcher("/WEB-INF/views/auth/change-password.jsp").forward(request, response);
            return;
        }

        try {
            authService.changePassword(currentUser.getId(), oldPassword, newPassword, ip, userAgent);
            currentUser.setMustChangePassword(false);
            session.setAttribute("flashSuccess", "Đổi mật khẩu thành công!");
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (Exception e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/auth/change-password.jsp").forward(request, response);
        }
    }

    private void handleForgotPassword(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        // Generic Safe Response theo tiêu chuẩn bảo mật Sprint 1
        request.setAttribute("successMessage",
                "Nếu email '" + email + "' tồn tại trên hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến hòm thư của bạn.");
        request.getRequestDispatcher("/WEB-INF/views/auth/forgot-password.jsp").forward(request, response);
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect(request.getContextPath() + "/auth/login?loggedOut=true");
    }
}

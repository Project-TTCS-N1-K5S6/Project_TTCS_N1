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
 * ==============================================================================
 * BỘ ĐIỀU KHIỂN XÁC THỰC NGƯỜI DÙNG (AuthServlet)
 * ==============================================================================
 * Phục vụ các User Story:
 * - US 1: Đăng nhập bằng Email công ty & Mật khẩu (Khóa tạm 15 phút sau 5 lần sai).
 * - US 2: Duy trì phiên làm việc & Đăng xuất an toàn (Hủy phiên ngay lập tức ở server).
 * - US 3: Quên mật khẩu qua email (Hiển thị phản hồi chung chống User Enumeration).
 * - US 4: Đổi mật khẩu chủ động khi đang đăng nhập (Thu hồi phiên cũ).
 * ==============================================================================
 */
@WebServlet(name = "AuthServlet", urlPatterns = {
        "/auth/login",
        "/auth/logout",
        "/auth/forgot-password",
        "/auth/change-password",
        "/auth/reset-password"
})
public class AuthServlet extends HttpServlet {
    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        switch (path) {
            case "/auth/login":
                // [US 1]: Nếu đã có phiên đăng nhập hợp lệ -> điều hướng tới trang chủ tương ứng vai trò
                HttpSession session = request.getSession(false);
                if (session != null && session.getAttribute("currentUser") != null) {
                    User user = (User) session.getAttribute("currentUser");
                    if ((user.hasRole("RECRUITER") || user.hasRole("INTERVIEWER")) && !user.hasRole("ADMIN") && !user.hasRole("HR_MANAGER")) {
                        response.sendRedirect(request.getContextPath() + "/candidates");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/dashboard");
                    }
                    return;
                }
                request.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(request, response);
                break;

            case "/auth/logout":
                // [US 2]: Xử lý đăng xuất an toàn
                handleLogout(request, response);
                break;

            case "/auth/forgot-password":
                // [US 3]: Hiển thị form quên mật khẩu
                request.getRequestDispatcher("/WEB-INF/views/auth/forgot-password.jsp").forward(request, response);
                break;

            case "/auth/reset-password":
                // [US 3]: Hiển thị form đặt lại mật khẩu với Token từ email
                String token = request.getParameter("token");
                request.setAttribute("token", token);
                request.getRequestDispatcher("/WEB-INF/views/auth/reset-password.jsp").forward(request, response);
                break;

            case "/auth/change-password":
                // [US 4]: Hiển thị form đổi mật khẩu cho người dùng đang đăng nhập
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
                // [US 1]: Tiếp nhận submit form đăng nhập
                handleLogin(request, response);
                break;

            case "/auth/change-password":
                // [US 4]: Tiếp nhận submit đổi mật khẩu
                handleChangePassword(request, response);
                break;

            case "/auth/forgot-password":
                // [US 3]: Tiếp nhận yêu cầu gửi email quên mật khẩu
                handleForgotPassword(request, response);
                break;

            case "/auth/reset-password":
                // [US 3]: Tiếp nhận đặt lại mật khẩu bằng Token
                handleResetPassword(request, response);
                break;

            default:
                response.sendRedirect(request.getContextPath() + "/auth/login");
                break;
        }
    }

    /**
     * [US 1]: Xử lý đăng nhập
     * - Tiêu chí US 1: Đăng nhập đúng thì vào được trang chủ tương ứng với vai trò
     */
    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String redirect = request.getParameter("redirect");
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            User user = authService.login(email, password, ip, userAgent);

            // [US 2]: Tạo phiên làm việc (Session) an toàn phía máy chủ
            HttpSession session = request.getSession(true);
            session.setAttribute("currentUser", user);

            // [US 4]: Nếu tài khoản vừa tạo hoặc được admin cấp mật khẩu tạm -> bắt buộc đổi ngay
            if (user.isMustChangePassword()) {
                response.sendRedirect(request.getContextPath() + "/auth/change-password?required=true");
                return;
            }

            // [US 1 Tiêu chí 1]: Chuyển hướng tới trang mong muốn hoặc trang chủ tương ứng với vai trò
            if (redirect != null && !redirect.trim().isEmpty() && !redirect.startsWith("/auth/") && !redirect.equals("/") && !redirect.equals("/dashboard")) {
                response.sendRedirect(request.getContextPath() + redirect);
            } else {
                if ((user.hasRole("RECRUITER") || user.hasRole("INTERVIEWER")) && !user.hasRole("ADMIN") && !user.hasRole("HR_MANAGER")) {
                    response.sendRedirect(request.getContextPath() + "/candidates");
                } else {
                    response.sendRedirect(request.getContextPath() + "/dashboard");
                }
            }
        } catch (Exception e) {
            // [US 1]: Hiển thị thông báo lỗi khi đăng nhập không thành công
            request.setAttribute("errorMessage", e.getMessage());
            request.setAttribute("inputEmail", email);
            request.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(request, response);
        }
    }

    /**
     * [US 4]: Xử lý đổi mật khẩu cho tài khoản hiện tại
     */
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
            // [US 4]: Cập nhật session_version của phiên hiện tại để không bị bộ lọc thu hồi, trong khi tất cả các thiết bị khác sẽ bị thu hồi phiên
            currentUser.setSessionVersion(currentUser.getSessionVersion() + 1);
            session.setAttribute("flashSuccess", "Đổi mật khẩu thành công!");
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } catch (Exception e) {
            request.setAttribute("errorMessage", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/auth/change-password.jsp").forward(request, response);
        }
    }

    /**
     * [US 3]: Xử lý quên mật khẩu qua email
     * - Tiêu chí US 3: Sinh liên kết đặt lại mật khẩu có hiệu lực 30 phút, gửi email,
     *   và email không tồn tại vẫn hiển thị cùng 1 thông báo an toàn.
     */
    private void handleForgotPassword(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        String appUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        authService.requestPasswordReset(email, appUrl);

        // Generic Safe Response chống User Enumeration
        request.setAttribute("successMessage",
                "Nếu email '" + email + "' tồn tại trên hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến hòm thư của bạn.");
        request.getRequestDispatcher("/WEB-INF/views/auth/forgot-password.jsp").forward(request, response);
    }

    /**
     * [US 3]: Xử lý đặt lại mật khẩu bằng Token từ email
     */
    private void handleResetPassword(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token = request.getParameter("token");
        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            request.setAttribute("token", token);
            request.setAttribute("errorMessage", "Xác nhận mật khẩu mới không trùng khớp.");
            request.getRequestDispatcher("/WEB-INF/views/auth/reset-password.jsp").forward(request, response);
            return;
        }

        try {
            authService.resetPasswordWithToken(token, newPassword, ip, userAgent);
            response.sendRedirect(request.getContextPath() + "/auth/login?resetSuccess=true");
        } catch (Exception e) {
            request.setAttribute("token", token);
            request.setAttribute("errorMessage", e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/auth/reset-password.jsp").forward(request, response);
        }
    }

    /**
     * [US 2]: Xử lý đăng xuất an toàn
     */
    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // Hủy toàn bộ dữ liệu phiên trên RAM máy chủ
        }
        response.sendRedirect(request.getContextPath() + "/auth/login?loggedOut=true");
    }
}

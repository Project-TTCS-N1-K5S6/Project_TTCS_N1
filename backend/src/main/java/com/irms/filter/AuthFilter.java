package com.irms.filter;

import com.irms.model.User;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Filter kiểm soát bảo mật, xác thực phiên đăng nhập và phân quyền truy cập
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = {"/*"})
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI().substring(request.getContextPath().length());

        // Bỏ qua tài nguyên tĩnh (CSS, JS, Fonts, Images)
        if (path.startsWith("/assets/") || path.startsWith("/static/") || path.endsWith(".css") ||
            path.endsWith(".js") || path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".svg") ||
            path.endsWith(".ico") || path.endsWith(".woff") || path.endsWith(".woff2")) {
            chain.doFilter(request, response);
            return;
        }

        // Bỏ qua các trang công khai (Đăng nhập, Quên mật khẩu)
        if (path.startsWith("/auth/login") || path.startsWith("/auth/forgot-password") || path.equals("/")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        // Nếu chưa đăng nhập mà truy cập trang nội bộ -> Chuyển hướng về trang đăng nhập
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login?redirect=" + path);
            return;
        }

        // Nếu người dùng bắt buộc phải đổi mật khẩu (Must Change Password Flow)
        if (currentUser.isMustChangePassword() && !path.startsWith("/auth/change-password") && !path.startsWith("/auth/logout")) {
            response.sendRedirect(request.getContextPath() + "/auth/change-password?required=true");
            return;
        }

        // Kiểm tra phân quyền truy cập theo Ma trận RBAC:
        // 1. Phân hệ Quản trị người dùng (/admin/users)
        if (path.startsWith("/admin/users") && !currentUser.hasPermission("users.view") && !currentUser.hasRole("ADMIN")) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        // 2. Phân hệ Vai trò (/admin/roles)
        if (path.startsWith("/admin/roles") && !currentUser.hasPermission("roles.view") && !currentUser.hasRole("ADMIN")) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        // 3. Phân hệ Ma trận phân quyền (/admin/permissions)
        if (path.startsWith("/admin/permissions") && !currentUser.hasPermission("permissions.view") && !currentUser.hasRole("ADMIN")) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        // 4. Phân hệ Nhật ký kiểm toán (/admin/audit-logs)
        if (path.startsWith("/admin/audit-logs") && !currentUser.hasPermission("audit.view") && !currentUser.hasRole("ADMIN")) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        // 5. Phân hệ Dải lương: Cấm Người phỏng vấn (INTERVIEWER) theo đặc tả Sprint 1
        if (path.startsWith("/salary-ranges") && currentUser.hasRole("INTERVIEWER") && !currentUser.hasRole("ADMIN")) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}

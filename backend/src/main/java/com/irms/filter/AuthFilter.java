package com.irms.filter;

import com.irms.dao.UserDAO;
import com.irms.model.User;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * ==============================================================================
 * BỘ LỌC BẢO MẬT & KIỂM SOÁT PHÂN QUYỀN (AuthFilter)
 * ==============================================================================
 * Liên quan trực tiếp đến các User Story:
 * - US 1: Đăng nhập nội bộ, bảo vệ dữ liệu hồ sơ ứng viên khỏi người ngoài.
 * - US 2: Duy trì phiên đăng nhập & xử lý phiên hết hạn an toàn.
 * - US 3: Quên mật khẩu qua email & đặt lại mật khẩu an toàn (/auth/reset-password).
 * - US 4: Bắt buộc đổi mật khẩu lần đầu (Must Change Password) & thu hồi phiên cũ khi đổi mật khẩu.
 * - US 5: Kiểm soát phân quyền RBAC ở tầng Server (Server-Side Authorization, Deny-by-default).
 * - US 7: Chuyển hướng lỗi 403 thân thiện khi thiếu quyền truy cập.
 * - US 9: Thay đổi vai trò/quyền có hiệu lực ngay ở thao tác kế tiếp không cần login lại.
 * - US 10: Thu hồi phiên đang mở ngay lập tức khi tài khoản bị khóa (status = LOCKED).
 * ==============================================================================
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = {"/*"})
public class AuthFilter implements Filter {

    private final UserDAO userDAO = new UserDAO();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Khởi tạo bộ lọc bảo mật
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String path = request.getRequestURI().substring(request.getContextPath().length());

        // ----------------------------------------------------------------------
        // [US 1 & US 6]: Bỏ qua các tài nguyên tĩnh để tải giao diện (CSS, JS, Fonts, Images)
        // Đảm bảo giao diện responsive (360px) và icon hiển thị đầy đủ ngay cả khi chưa login.
        // ----------------------------------------------------------------------
        if (path.startsWith("/assets/") || path.startsWith("/static/") || path.startsWith("/uploads/") || path.endsWith(".css") ||
            path.endsWith(".js") || path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".webp") || path.endsWith(".svg") ||
            path.endsWith(".ico") || path.endsWith(".woff") || path.endsWith(".woff2")) {
            chain.doFilter(request, response);
            return;
        }

        // ----------------------------------------------------------------------
        // [US 1 & US 3]: Bỏ qua các URL công khai không yêu cầu phiên đăng nhập:
        // - /auth/login: Trang đăng nhập hệ thống
        // - /auth/forgot-password: Quên mật khẩu qua email
        // - /auth/reset-password: Xác thực token và đặt lại mật khẩu mới
        // - /about-us, /company/about: Trang giới thiệu công ty công khai
        // ----------------------------------------------------------------------
        if (path.startsWith("/auth/login") || path.startsWith("/auth/forgot-password") ||
            path.startsWith("/auth/reset-password") || path.startsWith("/about-us") || path.startsWith("/company/about")) {
            chain.doFilter(request, response);
            return;
        }

        // ----------------------------------------------------------------------
        // [US 2]: Kiểm tra phiên làm việc (Session Management)
        // Bắt buộc phải đăng nhập thì mới được vào hệ thống.
        // Nếu chưa đăng nhập: chuyển hướng về trang /auth/login
        // ----------------------------------------------------------------------
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            if (path.equals("/") || path.isEmpty() || path.equals("/index.jsp")) {
                response.sendRedirect(request.getContextPath() + "/auth/login");
            } else {
                response.sendRedirect(request.getContextPath() + "/auth/login?redirect=" + path);
            }
            return;
        }

        // Nếu đã đăng nhập mà truy cập trang chủ / hoặc /index.jsp -> điều hướng tới trang tương ứng vai trò
        if (path.equals("/") || path.isEmpty() || path.equals("/index.jsp")) {
            if ((currentUser.hasRole("RECRUITER") || currentUser.hasRole("INTERVIEWER"))
                    && !currentUser.hasRole("ADMIN") && !currentUser.hasRole("HR_MANAGER")) {
                response.sendRedirect(request.getContextPath() + "/candidates");
            } else {
                response.sendRedirect(request.getContextPath() + "/dashboard");
            }
            return;
        }

        // ----------------------------------------------------------------------
        // [US 4, US 9 & US 10]: Kiểm tra tính hợp lệ của phiên trong Database thời gian thực
        // 1. Thu hồi phiên ngay lập tức nếu tài khoản bị ADMIN khóa (US 10)
        // 2. Thu hồi phiên nếu đổi mật khẩu ở thiết bị khác (session_version tăng) (US 4)
        // 3. Cập nhật tức thời vai trò và quyền hạn mới nhất ở thao tác kế tiếp (US 9)
        // ----------------------------------------------------------------------
        User currentState = userDAO.findSessionStateById(currentUser.getId());
        if (currentState == null) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        if ("LOCKED".equalsIgnoreCase(currentState.getStatus())) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/auth/login?locked=true");
            return;
        }

        if (currentState.getSessionVersion() != currentUser.getSessionVersion()) {
            session.invalidate();
            response.sendRedirect(request.getContextPath() + "/auth/login?sessionRevoked=true");
            return;
        }

        // Đồng bộ quyền hạn, avatar và trạng thái mới nhất từ DB vào currentUser trong RAM (US 9)
        currentUser.setRoles(currentState.getRoles());
        currentUser.setPermissions(currentState.getPermissions());
        currentUser.setMustChangePassword(currentState.isMustChangePassword());
        if (currentState.getAvatarUrl() != null) {
            currentUser.setAvatarUrl(currentState.getAvatarUrl());
        }

        // ----------------------------------------------------------------------
        // [US 4]: Bắt buộc đổi mật khẩu nếu tài khoản được cấp mật khẩu tạm
        // Chặn người dùng truy cập bất kỳ tính năng nào khác cho đến khi hoàn thành đổi mật khẩu.
        // ----------------------------------------------------------------------
        if (currentUser.isMustChangePassword() && !path.startsWith("/auth/change-password") && !path.startsWith("/auth/logout")) {
            response.sendRedirect(request.getContextPath() + "/auth/change-password?required=true");
            return;
        }

        // ----------------------------------------------------------------------
        // [US 5 & US 7]: Kiểm soát phân quyền theo Ma trận RBAC tại tầng Server
        // Nếu không đủ thẩm quyền -> forward sang trang lỗi tiếng Việt /errors/403.jsp
        // ----------------------------------------------------------------------

        // 1. Phân hệ Quản trị người dùng (/admin/users) - Yêu cầu quyền users.view hoặc ADMIN
        if (path.startsWith("/admin/users") && !currentUser.hasPermission("users.view") && !currentUser.hasRole("ADMIN")) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        // 2. Phân hệ Quản lý vai trò (/admin/roles) - Yêu cầu quyền roles.view hoặc ADMIN
        if (path.startsWith("/admin/roles") && !currentUser.hasPermission("roles.view") && !currentUser.hasRole("ADMIN")) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        // 3. Phân hệ Ma trận phân quyền (/admin/permissions) - Yêu cầu quyền permissions.view hoặc ADMIN
        if (path.startsWith("/admin/permissions") && !currentUser.hasPermission("permissions.view") && !currentUser.hasRole("ADMIN")) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        // 4. Phân hệ Nhật ký kiểm toán (/admin/audit-logs) - Yêu cầu quyền audit.view hoặc ADMIN
        if (path.startsWith("/admin/audit-logs") && !currentUser.hasPermission("audit.view") && !currentUser.hasRole("ADMIN")) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        // 5. Phân hệ Dải lương [KN-103]: CHỈ Trưởng phòng Nhân sự (HR_MANAGER) và Quản trị hệ thống (ADMIN) mới xem & quản lý được
        if (path.startsWith("/salary-ranges")) {
            boolean isAuthorizedSalary = currentUser.hasRole("ADMIN") || currentUser.hasRole("HR_MANAGER");
            if (!isAuthorizedSalary) {
                request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
                return;
            }
        }

        // 5.1 Phân hệ Khung năng lực (/competency-frameworks): Kiểm tra quyền competencies.view hoặc ADMIN hoặc HR_MANAGER
        if (path.startsWith("/competency-frameworks")) {
            boolean isAuthorizedCompetency = currentUser.hasRole("ADMIN") || currentUser.hasRole("HR_MANAGER")
                    || currentUser.hasPermission("competencies.view")
                    || currentUser.hasPermission("evaluations.manage");
            if (!isAuthorizedCompetency) {
                request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
                return;
            }
        }

        // Phân hệ Yêu cầu tuyển dụng (/recruitment-requests) - Yêu cầu quyền requisitions.view, requisitions.create hoặc ADMIN
        if (path.startsWith("/recruitment-requests")) {
            if (!currentUser.hasPermission("requisitions.view") && !currentUser.hasPermission("requisitions.create") && !currentUser.hasRole("ADMIN")) {
                request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
                return;
            }
        }

        // 6. Phân hệ Hồ sơ ứng viên (/candidates) - Yêu cầu quyền candidates.view hoặc ADMIN
        if (path.startsWith("/candidates") && !currentUser.hasPermission("candidates.view") && !currentUser.hasRole("ADMIN")) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        // 7. Phân hệ Khai báo phòng ban (/admin/departments) - Yêu cầu quyền department.view / departments.view hoặc ADMIN / HR_MANAGER
        if (path.startsWith("/admin/departments")) {
            boolean isAuthorizedDept = currentUser.hasRole("ADMIN")
                    || currentUser.hasRole("HR_MANAGER")
                    || currentUser.hasPermission("department.view")
                    || currentUser.hasPermission("departments.view");
            if (!isAuthorizedDept) {
                request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // 8. Phân hệ Ngân hàng câu hỏi theo năng lực (/admin/questions, /admin/criteria)
        if (path.startsWith("/admin/questions") || path.startsWith("/admin/criteria")) {
            chain.doFilter(request, response);
            return;
        }

        // 9. Phân hệ Cấu hình trang giới thiệu công ty (/admin/company-profile)
        if (path.startsWith("/admin/company-profile")) {
            boolean isAuthorizedCompany = currentUser.hasRole("ADMIN") || currentUser.hasRole("HR_MANAGER")
                    || currentUser.hasPermission("company.manage") || currentUser.hasPermission("company.view");
            if (!isAuthorizedCompany) {
                request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // 9. [US 5]: Cơ chế từ chối mặc định (Deny-by-default) cho toàn bộ các endpoint /admin/ nếu không có vai trò ADMIN
        if (path.startsWith("/admin/") && !currentUser.hasRole("ADMIN")) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Dọn dẹp tài nguyên Filter khi ứng dụng dừng
    }
}

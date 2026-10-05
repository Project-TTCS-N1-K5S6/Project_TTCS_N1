<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đăng nhập - IRMS Platform</title>
    <!-- Google Fonts: Inter -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <!-- Bootstrap 5.3.3 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <!-- SASS Stylesheet -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/main.css">
</head>
<body class="auth-page">

<div class="auth-card">
    <div class="auth-brand">
        <div class="brand-icon">
            <i class="bi bi-shield-lock-fill"></i>
        </div>
        <h3>IRMS PLATFORM</h3>
        <p>Hệ thống Quản lý Tuyển dụng Nội bộ</p>
    </div>

    <!-- 
        ======================================================================
        [US 1 & US 2]: KHU VỰC THÔNG BÁO HỆ THỐNG
        - errorMessage: Thông báo lỗi xác thực, tài khoản bị khóa tạm 15 phút sau 5 lần sai (US 1).
        - loggedOut: Thông báo đăng xuất làm mất hiệu lực phiên an toàn phía server (US 2).
        - redirect (Phiên hết hạn): Tiêu chí US 2 yêu cầu thông báo rõ ràng khi phiên hết hạn.
        ======================================================================
    -->
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger py-2 px-3 mb-3 small d-flex align-items-center" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2 fs-5"></i>
            <div>${errorMessage}</div>
        </div>
    </c:if>

    <c:if test="${param.loggedOut == 'true'}">
        <div class="alert alert-success py-2 px-3 mb-3 small d-flex align-items-center" role="alert">
            <i class="bi bi-check-circle-fill me-2 fs-5"></i>
            <div>Bạn đã đăng xuất an toàn khỏi hệ thống.</div>
        </div>
    </c:if>

    <c:if test="${param.resetSuccess == 'true'}">
        <div class="alert alert-success py-2 px-3 mb-3 small d-flex align-items-center" role="alert">
            <i class="bi bi-check-circle-fill me-2 fs-5"></i>
            <div>Đặt lại mật khẩu thành công! Vui lòng đăng nhập với mật khẩu mới của bạn.</div>
        </div>
    </c:if>

    <c:if test="${param.sessionRevoked == 'true'}">
        <div class="alert alert-warning py-2 px-3 mb-3 small d-flex align-items-center" role="alert">
            <i class="bi bi-shield-exclamation me-2 fs-5"></i>
            <div>Phiên đăng nhập đã bị thu hồi do tài khoản đã đổi mật khẩu hoặc được cập nhật vai trò. Vui lòng đăng nhập lại.</div>
        </div>
    </c:if>

    <c:if test="${param.locked == 'true'}">
        <div class="alert alert-danger py-2 px-3 mb-3 small d-flex align-items-center" role="alert">
            <i class="bi bi-lock-fill me-2 fs-5"></i>
            <div>Tài khoản của bạn đã bị Quản trị viên khóa. Mọi phiên làm việc đã bị thu hồi.</div>
        </div>
    </c:if>

    <!-- [US 2 Tiêu chí 3]: Phiên hết hạn đưa về trang đăng nhập kèm thông báo rõ ràng -->
    <c:if test="${not empty param.redirect && param.loggedOut != 'true' && param.sessionRevoked != 'true' && param.locked != 'true'}">
        <div class="alert alert-warning py-2 px-3 mb-3 small d-flex align-items-center" role="alert">
            <i class="bi bi-clock-history me-2 fs-5"></i>
            <div>Phiên làm việc của bạn đã hết hạn do không hoạt động. Vui lòng đăng nhập lại để tiếp tục.</div>
        </div>
    </c:if>

    <!-- [US 1]: Form đăng nhập email nội bộ và mật khẩu -->
    <form action="${pageContext.request.contextPath}/auth/login" method="POST">
        <input type="hidden" name="redirect" value="${param.redirect}">

        <div class="form-floating mb-3">
            <input type="email" class="form-control" id="email" name="email" 
                   placeholder="name@company.com" required autofocus
                   value="${inputEmail != null ? inputEmail : 'admin@company.local'}">
            <label for="email"><i class="bi bi-envelope me-1"></i> Địa chỉ Email</label>
        </div>

        <div class="form-floating mb-3 position-relative">
            <input type="password" class="form-control" id="password" name="password" 
                   placeholder="Mật khẩu" required value="Admin@123456">
            <label for="password"><i class="bi bi-key me-1"></i> Mật khẩu</label>
            <button type="button" class="btn btn-sm btn-link position-absolute end-0 top-50 translate-middle-y text-muted text-decoration-none me-2" 
                    id="togglePassword" style="z-index: 5;">
                <i class="bi bi-eye" id="eyeIcon"></i>
            </button>
        </div>

        <div class="d-flex justify-content-between align-items-center mb-4">
            <div class="form-check">
                <input class="form-check-input" type="checkbox" id="rememberMe">
                <label class="form-check-label text-muted small" for="rememberMe">Ghi nhớ đăng nhập</label>
            </div>
            <a href="${pageContext.request.contextPath}/auth/forgot-password" class="small text-primary text-decoration-none fw-semibold">
                Quên mật khẩu?
            </a>
        </div>

        <button type="submit" class="btn btn-primary btn-submit mb-3">
            <i class="bi bi-box-arrow-in-right me-1"></i> Đăng nhập hệ thống
        </button>
    </form>

    <!-- Hướng dẫn tài khoản kiểm thử mặc định -->
    <div class="mt-4 p-3 bg-light rounded-3 border">
        <div class="fw-semibold small text-dark mb-1"><i class="bi bi-info-circle me-1 text-primary"></i> Tài khoản mẫu:</div>
        <div class="text-xs text-secondary">
            &bull; <strong>Admin:</strong> admin@company.local<br>
            &bull; <strong>HR Manager:</strong> hr.manager@company.local<br>
            &bull; <strong>Recruiter:</strong> recruiter@company.local<br>
            &bull; <em>Mật khẩu chung:</em> <code>Admin@123456</code>
        </div>
    </div>

    <div class="auth-footer">
        &copy; 2026 IRMS Enterprise. Bảo mật phiên làm việc 60 phút.
    </div>
</div>

<!-- Bootstrap 5.3.3 JS -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
    const toggleBtn = document.getElementById('togglePassword');
    const pwdInput = document.getElementById('password');
    const eyeIcon = document.getElementById('eyeIcon');
    if (toggleBtn && pwdInput) {
        toggleBtn.addEventListener('click', () => {
            const isPassword = pwdInput.type === 'password';
            pwdInput.type = isPassword ? 'text' : 'password';
            eyeIcon.className = isPassword ? 'bi bi-eye-slash' : 'bi bi-eye';
        });
    }
</script>
</body>
</html>

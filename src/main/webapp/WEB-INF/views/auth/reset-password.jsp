<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đặt lại mật khẩu - IRMS Platform</title>
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
            <i class="bi bi-shield-check"></i>
        </div>
        <h3>ĐẶT LẠI MẬT KHẨU</h3>
        <p>Tạo mật khẩu mới cho tài khoản của bạn</p>
    </div>

    <!-- Thông báo lỗi -->
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger py-2 px-3 mb-3 small d-flex align-items-center" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2 fs-5"></i>
            <div>${errorMessage}</div>
        </div>
    </c:if>

    <!-- [US 3 & US 4]: Form đặt lại mật khẩu với token 30 phút dùng 1 lần -->
    <form action="${pageContext.request.contextPath}/auth/reset-password" method="POST">
        <input type="hidden" name="token" value="${token}">

        <div class="form-floating mb-3">
            <input type="password" class="form-control" id="newPassword" name="newPassword" 
                   placeholder="Mật khẩu mới" required minlength="8">
            <label for="newPassword"><i class="bi bi-key me-1"></i> Mật khẩu mới</label>
            <div class="form-text text-xs text-muted">Tối thiểu 8 ký tự, phải bao gồm cả chữ và số.</div>
        </div>

        <div class="form-floating mb-4">
            <input type="password" class="form-control" id="confirmPassword" name="confirmPassword" 
                   placeholder="Xác nhận mật khẩu" required minlength="8">
            <label for="confirmPassword"><i class="bi bi-check2-circle me-1"></i> Xác nhận mật khẩu mới</label>
        </div>

        <button type="submit" class="btn btn-primary btn-submit mb-3">
            <i class="bi bi-shield-lock me-1"></i> Cập nhật mật khẩu mới
        </button>

        <div class="text-center mt-3">
            <a href="${pageContext.request.contextPath}/auth/login" class="text-decoration-none small text-muted">
                <i class="bi bi-arrow-left me-1"></i> Quay lại đăng nhập
            </a>
        </div>
    </form>

    <div class="auth-footer">
        &copy; 2026 IRMS Security. Liên kết chỉ có hiệu lực 1 lần duy nhất trong 30 phút.
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

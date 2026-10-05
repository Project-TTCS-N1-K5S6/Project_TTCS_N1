<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quên mật khẩu - IRMS Platform</title>
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
            <i class="bi bi-key-fill"></i>
        </div>
        <h3>KHÔI PHỤC MẬT KHẨU</h3>
        <p>Nhập email đăng ký để nhận liên kết đặt lại mật khẩu</p>
    </div>

    <c:if test="${not empty successMessage}">
        <div class="alert alert-success py-2 px-3 mb-4 small d-flex align-items-center" role="alert">
            <i class="bi bi-check-circle-fill me-2 fs-5"></i>
            <div>${successMessage}</div>
        </div>
    </c:if>

    <form action="${pageContext.request.contextPath}/auth/forgot-password" method="POST">
        <div class="form-floating mb-4">
            <input type="email" class="form-control" id="email" name="email" 
                   placeholder="name@company.com" required autofocus>
            <label for="email"><i class="bi bi-envelope me-1"></i> Email tài khoản</label>
        </div>

        <button type="submit" class="btn btn-primary btn-submit mb-3">
            <i class="bi bi-send me-1"></i> Gửi yêu cầu khôi phục
        </button>

        <div class="text-center mt-3">
            <a href="${pageContext.request.contextPath}/auth/login" class="text-decoration-none small text-muted">
                <i class="bi bi-arrow-left me-1"></i> Quay lại trang đăng nhập
            </a>
        </div>
    </form>

    <div class="auth-footer">
        &copy; 2026 IRMS Security. Liên kết có hiệu lực tối đa 30 phút.
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

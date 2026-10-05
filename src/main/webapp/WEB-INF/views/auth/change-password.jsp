<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đổi mật khẩu - IRMS Platform</title>
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
            <i class="bi bi-shield-lock"></i>
        </div>
        <h3>ĐỔI MẬT KHẨU</h3>
        <c:choose>
            <c:when test="${param.required == 'true'}">
                <p class="text-danger fw-semibold">Bạn cần đổi mật khẩu khởi tạo trước khi tiếp tục</p>
            </c:when>
            <c:otherwise>
                <p>Cập nhật mật khẩu để bảo vệ an toàn cho tài khoản</p>
            </c:otherwise>
        </c:choose>
    </div>

    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger py-2 px-3 mb-3 small d-flex align-items-center" role="alert">
            <i class="bi bi-exclamation-triangle-fill me-2 fs-5"></i>
            <div>${errorMessage}</div>
        </div>
    </c:if>

    <form action="${pageContext.request.contextPath}/auth/change-password" method="POST">
        <div class="form-floating mb-3">
            <input type="password" class="form-control" id="oldPassword" name="oldPassword" 
                   placeholder="Mật khẩu hiện tại" required>
            <label for="oldPassword"><i class="bi bi-lock me-1"></i> Mật khẩu hiện tại</label>
        </div>

        <div class="form-floating mb-3">
            <input type="password" class="form-control" id="newPassword" name="newPassword" 
                   placeholder="Mật khẩu mới (tối thiểu 8 ký tự)" minlength="8" required>
            <label for="newPassword"><i class="bi bi-key me-1"></i> Mật khẩu mới</label>
        </div>

        <div class="form-floating mb-4">
            <input type="password" class="form-control" id="confirmPassword" name="confirmPassword" 
                   placeholder="Xác nhận mật khẩu mới" minlength="8" required>
            <label for="confirmPassword"><i class="bi bi-check-circle me-1"></i> Xác nhận mật khẩu mới</label>
        </div>

        <button type="submit" class="btn btn-primary btn-submit mb-3">
            <i class="bi bi-arrow-repeat me-1"></i> Cập nhật mật khẩu
        </button>

        <c:if test="${param.required != 'true'}">
            <div class="text-center mt-2">
                <a href="${pageContext.request.contextPath}/dashboard" class="text-decoration-none small text-muted">
                    <i class="bi bi-x-circle me-1"></i> Hủy bỏ, quay về Bảng điều khiển
                </a>
            </div>
        </c:if>
    </form>

    <div class="auth-footer">
        &copy; 2026 IRMS Security. Yêu cầu mật khẩu mạnh tối thiểu 8 ký tự.
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

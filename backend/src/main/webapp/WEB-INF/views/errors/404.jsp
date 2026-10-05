<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>404 - Không tìm thấy trang | IRMS</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&display=swap" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/main.css">
</head>
<body class="bg-light d-flex align-items-center justify-content-center min-vh-100 p-3">
    <div class="text-center" style="max-width: 500px;">
        <div class="display-1 text-primary fw-bold mb-3"><i class="bi bi-compass"></i> 404</div>
        <h2 class="fw-bold mb-2">Đường dẫn không tồn tại</h2>
        <p class="text-muted mb-4">
            Trang hoặc tài nguyên bạn đang cố truy cập không tồn tại hoặc đã được di chuyển trong hệ thống IRMS.
        </p>
        <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-primary px-4 py-2">
            <i class="bi bi-house-door me-1"></i> Quay về Bảng điều khiển
        </a>
    </div>
</body>
</html>

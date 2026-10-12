<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cơ hội Nghề nghiệp &amp; Tuyển dụng | IRMS Careers</title>
    <!-- Google Fonts Inter -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap" rel="stylesheet">
    <!-- Bootstrap 5 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
    <style>
        body {
            font-family: 'Inter', sans-serif;
            background-color: #f8fafc;
            color: #1e293b;
        }
        .hero-section {
            background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
            color: white;
            padding: 80px 0 60px;
        }
        .job-card {
            transition: all 0.25s ease-in-out;
            border: 1px solid #e2e8f0;
            border-radius: 12px;
            background: white;
        }
        .job-card:hover {
            transform: translateY(-4px);
            box-shadow: 0 12px 24px -10px rgba(0, 0, 0, 0.15);
            border-color: #3b82f6;
        }
    </style>
</head>
<body>

    <!-- Header Navigation -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark py-3 shadow-sm">
        <div class="container">
            <a class="navbar-brand fw-bold d-flex align-items-center gap-2" href="${pageContext.request.contextPath}/careers">
                <i class="bi bi-grid-fill text-primary"></i> IRMS Careers Portal
            </a>
            <div class="d-flex align-items-center gap-3">
                <a href="${pageContext.request.contextPath}/about-us" class="nav-link text-white-50 small">Về chúng tôi</a>
                <a href="${pageContext.request.contextPath}/auth/login" class="btn btn-outline-light btn-sm px-3">
                    <i class="bi bi-person-fill me-1"></i> Quản trị nội bộ
                </a>
            </div>
        </div>
    </nav>

    <!-- Hero Banner -->
    <header class="hero-section text-center">
        <div class="container">
            <span class="badge bg-white text-primary px-3 py-1.5 rounded-pill mb-3 fw-bold text-xs">
                <i class="bi bi-stars me-1"></i> ĐỒNG HÀNH VÀ PHÁT TRIỂN CÙNG CHÚNG TÔI
            </span>
            <h1 class="display-5 fw-bold mb-3">Gia nhập Đội ngũ Tài năng</h1>
            <p class="lead text-white-50 max-w-2xl mx-auto mb-4" style="max-width: 650px;">
                Khám phá các vị trí tuyển dụng mới nhất được phát hành chính thức từ các phòng ban chuyên môn.
            </p>

            <!-- Thanh tìm kiếm nhanh -->
            <div class="card border-0 shadow-lg p-2 mx-auto" style="max-width: 800px; border-radius: 50px;">
                <form method="GET" action="${pageContext.request.contextPath}/careers" class="row g-2 align-items-center px-2">
                    <div class="col-12 col-md-5">
                        <div class="input-group">
                            <span class="input-group-text bg-transparent border-0"><i class="bi bi-search text-muted"></i></span>
                            <input type="text" name="search" class="form-control border-0" placeholder="Chức danh, vị trí công việc..." value="${paramSearch}">
                        </div>
                    </div>
                    <div class="col-12 col-md-4">
                        <div class="input-group">
                            <span class="input-group-text bg-transparent border-0"><i class="bi bi-building text-muted"></i></span>
                            <select name="deptId" class="form-select border-0">
                                <option value="">Tất cả phòng ban</option>
                                <c:forEach var="d" items="${departments}">
                                    <option value="${d.id}" ${d.id == paramDept ? 'selected' : ''}>${d.name}</option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                    <div class="col-12 col-md-3">
                        <button type="submit" class="btn btn-primary w-100 rounded-pill py-2 fw-semibold">
                            Tìm việc ngay
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </header>

    <!-- Danh sách tin tuyển dụng công khai -->
    <main class="container py-5">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
                <h4 class="fw-bold mb-1 text-dark">Vị trí đang mở tuyển dụng</h4>
                <p class="text-muted small mb-0">Các tin tuyển dụng đã được kiểm duyệt chính thức và đang tiếp nhận hồ sơ ứng viên</p>
            </div>
            <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-3 py-2">
                <i class="bi bi-check2-circle me-1"></i> ${publicJobs.size()} vị trí sẵn sàng
            </span>
        </div>

        <div class="row g-4">
            <c:choose>
                <c:when test="${not empty publicJobs}">
                    <c:forEach var="job" items="${publicJobs}">
                        <div class="col-12 col-md-6 col-lg-4">
                            <div class="job-card p-4 h-100 d-flex flex-column">
                                <div class="d-flex justify-content-between align-items-start mb-2">
                                    <span class="badge bg-light text-secondary border font-monospace">${job.requisition.code}</span>
                                    <span class="badge bg-success-subtle text-success border border-success-subtle text-xs">
                                        <i class="bi bi-broadcast me-1"></i>Đang mở
                                    </span>
                                </div>
                                <h5 class="fw-bold text-dark mb-2 text-truncate" title="${job.requisition.positionTitle}">
                                    ${job.requisition.positionTitle}
                                </h5>
                                <div class="text-secondary small mb-3">
                                    <i class="bi bi-building me-1 text-primary"></i> ${job.requisition.departmentName != null ? job.requisition.departmentName : 'Toàn công ty'}
                                </div>

                                <div class="d-flex flex-wrap gap-2 mb-3">
                                    <span class="badge bg-light text-dark border">
                                        <i class="bi bi-cash-stack text-success me-1"></i>${job.requisition.formattedSalaryRange}
                                    </span>
                                    <span class="badge bg-light text-dark border">
                                        <i class="bi bi-person-fill text-primary me-1"></i>${job.requisition.headcount} chỉ tiêu
                                    </span>
                                </div>

                                <div class="text-muted text-xs mb-4 flex-grow-1" style="display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.6;">
                                    ${job.requisition.jobDescription != null ? job.requisition.jobDescription : 'Vui lòng nhấn xem chi tiết để tìm hiểu thông tin yêu cầu và mô tả công việc cụ thể.'}
                                </div>

                                <div class="pt-3 border-top d-flex justify-content-between align-items-center mt-auto">
                                    <span class="text-xs text-muted">
                                        <i class="bi bi-clock me-1"></i>Hạn: <fmt:formatDate value="${job.requisition.deadline}" pattern="dd/MM/yyyy" />
                                    </span>
                                    <a href="${pageContext.request.contextPath}/careers/job?id=${job.requisition.id}" class="btn btn-outline-primary btn-sm px-3 fw-semibold">
                                        Chi tiết &amp; Ứng tuyển <i class="bi bi-arrow-right ms-1"></i>
                                    </a>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <div class="col-12">
                        <div class="card border-0 shadow-sm py-5 text-center text-muted">
                            <i class="bi bi-inbox fs-1 d-block mb-3 text-secondary opacity-50"></i>
                            <h5 class="fw-bold text-dark">Hiện chưa có vị trí tuyển dụng công khai phù hợp</h5>
                            <p class="small text-secondary mb-3">Vui lòng quay lại sau hoặc thử lại với từ khóa tìm kiếm khác.</p>
                            <div>
                                <a href="${pageContext.request.contextPath}/careers" class="btn btn-sm btn-outline-primary">
                                    Xem tất cả các vị trí
                                </a>
                            </div>
                        </div>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </main>

    <!-- Footer -->
    <footer class="bg-white border-top py-4 mt-5 text-center text-secondary small">
        <div class="container">
            <p class="mb-1">&copy; 2026 IRMS Recruitment Platform. Bản quyền thuộc về doanh nghiệp.</p>
            <p class="mb-0 text-muted">Hệ thống Quản lý Xuất bản Tin tuyển dụng nội bộ và công khai (KN-120).</p>
        </div>
    </footer>

    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

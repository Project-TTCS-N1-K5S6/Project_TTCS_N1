<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${job.requisition.positionTitle} | Tuyển dụng IRMS</title>
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
        .header-banner {
            background: linear-gradient(135deg, #1e3a8a 0%, #3b82f6 100%);
            color: white;
            padding: 60px 0 40px;
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
                <a href="${pageContext.request.contextPath}/careers" class="btn btn-outline-light btn-sm px-3">
                    <i class="bi bi-arrow-left me-1"></i> Tất cả vị trí
                </a>
            </div>
        </div>
    </nav>

    <header class="header-banner">
        <div class="container">
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
                <span class="badge bg-white text-primary px-3 py-1 font-monospace fw-bold">${job.requisition.code}</span>
                <span class="badge bg-success-subtle text-success border border-success-subtle px-3 py-1">
                    <i class="bi bi-broadcast me-1"></i>Đang xuất bản chính thức
                </span>
            </div>
            <h1 class="display-6 fw-bold text-white mb-2">${job.requisition.positionTitle}</h1>
            <div class="text-white-50 fs-6 mb-3">${job.title}</div>
            <div class="d-flex flex-wrap gap-3 text-white-50 small">
                <span><i class="bi bi-building me-1"></i>${job.requisition.departmentName != null ? job.requisition.departmentName : 'Toàn công ty'}</span>
                <span><i class="bi bi-geo-alt me-1"></i>Trụ sở làm việc chính</span>
                <span><i class="bi bi-cash-stack me-1"></i><strong class="text-white">${job.requisition.formattedSalaryRange}</strong></span>
                <span><i class="bi bi-clock me-1"></i>Hạn nộp: <fmt:formatDate value="${job.requisition.deadline}" pattern="dd/MM/yyyy" /></span>
            </div>
        </div>
    </header>

    <main class="container py-5">
        <div class="row g-4">
            <div class="col-lg-8">
                <div class="card border-0 shadow-sm p-4 mb-4 rounded-3">
                    <h5 class="fw-bold text-dark border-bottom pb-2 mb-3">
                        <i class="bi bi-briefcase text-primary me-2"></i>Mô tả công việc
                    </h5>
                    <div class="text-secondary mb-4" style="white-space: pre-line; line-height: 1.8;">
                        ${job.requisition.jobDescription}
                    </div>

                    <h5 class="fw-bold text-dark border-bottom pb-2 mb-3">
                        <i class="bi bi-check2-circle text-primary me-2"></i>Yêu cầu ứng viên
                    </h5>
                    <div class="text-secondary mb-4" style="white-space: pre-line; line-height: 1.8;">
                        ${job.requisition.jobRequirements}
                    </div>

                    <h5 class="fw-bold text-dark border-bottom pb-2 mb-3">
                        <i class="bi bi-gift text-primary me-2"></i>Chế độ &amp; Quyền lợi đãi ngộ
                    </h5>
                    <ul class="text-secondary" style="line-height: 1.9;">
                        <li>Mức thu nhập cạnh tranh theo năng lực (${job.requisition.formattedSalaryRange}) + Thưởng tháng lương thứ 13, thưởng hiệu quả kinh doanh.</li>
                        <li>Được tham gia các khóa đào tạo nâng cao chuyên môn và hoàn thiện Khung năng lực nghề nghiệp.</li>
                        <li>Được hưởng đầy đủ quyền lợi BHXH, BHYT, BHTN theo Luật Lao động và chế độ bảo hiểm sức khỏe cao cấp.</li>
                        <li>Môi trường làm việc cởi mở, khuyến khích sáng tạo và trao quyền phát triển.</li>
                    </ul>
                </div>
            </div>

            <div class="col-lg-4">
                <div class="card border-0 shadow-sm p-4 rounded-3 mb-4 sticky-top" style="top: 20px;">
                    <h5 class="fw-bold text-dark mb-3">Ứng tuyển vị trí này</h5>
                    <p class="text-muted small mb-3">Gửi hồ sơ trực tuyến để được hội đồng tuyển dụng liên hệ phỏng vấn.</p>

                    <form id="applyForm" onsubmit="event.preventDefault(); alert('Cảm ơn bạn đã nộp hồ sơ! Hệ thống đã ghi nhận CV ứng tuyển cho mã: ${job.requisition.code}');">
                        <div class="mb-3">
                            <label class="form-label text-xs fw-bold text-muted">Họ và tên *</label>
                            <input type="text" class="form-control form-control-sm" required placeholder="Nguyễn Văn A">
                        </div>
                        <div class="mb-3">
                            <label class="form-label text-xs fw-bold text-muted">Email liên hệ *</label>
                            <input type="email" class="form-control form-control-sm" required placeholder="candidate@example.com">
                        </div>
                        <div class="mb-3">
                            <label class="form-label text-xs fw-bold text-muted">Số điện thoại *</label>
                            <input type="tel" class="form-control form-control-sm" required placeholder="0901234567">
                        </div>
                        <div class="mb-3">
                            <label class="form-label text-xs fw-bold text-muted">Tệp đính kèm CV (PDF/Word) *</label>
                            <input type="file" class="form-control form-control-sm" accept=".pdf,.doc,.docx" required>
                        </div>
                        <button type="submit" class="btn btn-primary w-100 py-2 fw-bold shadow-sm">
                            <i class="bi bi-send-fill me-1"></i> Nộp hồ sơ ứng tuyển
                        </button>
                    </form>

                    <div class="mt-4 pt-3 border-top text-xs text-muted">
                        <div><i class="bi bi-eye me-1"></i> Lượt xem: <strong>${job.viewsCount + 1}</strong></div>
                        <div class="mt-1"><i class="bi bi-calendar3 me-1"></i> Bản xuất: <strong>Bản #${job.currentVersion}</strong></div>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <!-- Footer -->
    <footer class="bg-white border-top py-4 text-center text-secondary small">
        <div class="container">
            <p class="mb-1">&copy; 2026 IRMS Recruitment Platform. Bản quyền thuộc về doanh nghiệp.</p>
        </div>
    </footer>

    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

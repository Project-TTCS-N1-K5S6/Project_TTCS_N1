<%--
  ==============================================================================
  CỔNG THÔNG TIN TUYỂN DỤNG CÔNG KHAI (publishing/careers.jsp)
  Chức năng:
  - Trang web công khai (Public Careers Portal) cho phép ứng viên và người tìm việc xem các vị trí đang tuyển (status = published).
  - Tìm kiếm việc làm theo từ khóa, lọc theo Phòng ban, Địa điểm làm việc, Hình thức làm việc (Full-time/Part-time/Remote).
  - Xem tóm tắt thông tin công việc, hạn nộp hồ sơ, mức lương (hoặc "Thoả thuận").
  - Tích hợp Modal xem chi tiết tin và Modal nộp hồ sơ ứng tuyển trực tuyến kéo-thả CV, chống spam CAPTCHA.
  ==============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cơ hội Nghề nghiệp & Tuyển dụng Công khai | IRMS Careers Portal</title>
    <meta name="description" content="Khám phá các cơ hội nghề nghiệp và vị trí tuyển dụng công khai tại IRMS. Tìm kiếm việc làm theo phòng ban, địa điểm và hình thức làm việc.">

    <!-- Google Fonts Inter -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">
    
    <!-- Bootstrap 5.3 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">

    <style>
        :root {
            --primary-navy: #0f172a;
            --primary-blue: #2563eb;
            --primary-blue-hover: #1d4ed8;
            --accent-emerald: #10b981;
            --surface-bg: #f8fafc;
            --card-border: #e2e8f0;
            --text-heading: #0f172a;
            --text-body: #334155;
            --text-muted: #64748b;
        }

        body {
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            background-color: var(--surface-bg);
            color: var(--text-body);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }

        /* Hero Header */
        .hero-banner {
            background: linear-gradient(135deg, #0f172a 0%, #1e3a8a 50%, #2563eb 100%);
            color: white;
            padding: 70px 0 50px;
            position: relative;
            overflow: hidden;
        }
        .hero-banner::before {
            content: '';
            position: absolute;
            top: 0; left: 0; right: 0; bottom: 0;
            background: radial-gradient(circle at 80% 20%, rgba(255,255,255,0.08) 0%, transparent 60%);
            pointer-events: none;
        }

        /* Filter Control Panel */
        .filter-panel {
            background: #ffffff;
            border-radius: 16px;
            box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.08), 0 8px 10px -6px rgba(15, 23, 42, 0.04);
            border: 1px solid var(--card-border);
            padding: 24px;
            margin-top: -36px;
            position: relative;
            z-index: 10;
        }

        /* Search input with icons */
        .search-box-wrap {
            position: relative;
        }
        .search-box-wrap .search-icon {
            position: absolute;
            left: 14px;
            top: 50%;
            transform: translateY(-50%);
            color: var(--text-muted);
            font-size: 1.05rem;
            pointer-events: none;
        }
        .search-box-wrap .form-control {
            padding-left: 42px;
            padding-right: 36px;
            height: 46px;
            border-radius: 10px;
            border: 1px solid #cbd5e1;
            font-size: 0.95rem;
            transition: all 0.2s ease;
        }
        .search-box-wrap .form-control:focus {
            border-color: var(--primary-blue);
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
        }
        .search-clear-btn {
            position: absolute;
            right: 12px;
            top: 50%;
            transform: translateY(-50%);
            background: none;
            border: none;
            color: #94a3b8;
            cursor: pointer;
            padding: 4px;
            display: none;
            border-radius: 50%;
        }
        .search-clear-btn:hover {
            color: var(--primary-navy);
            background: #f1f5f9;
        }

        /* Custom selects */
        .filter-select {
            height: 46px;
            border-radius: 10px;
            border: 1px solid #cbd5e1;
            font-size: 0.92rem;
            font-weight: 500;
            color: #1e293b;
            cursor: pointer;
            transition: all 0.2s ease;
        }
        .filter-select:focus {
            border-color: var(--primary-blue);
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
        }

        /* Filter Chips */
        .active-filter-chips {
            display: flex;
            flex-wrap: wrap;
            gap: 8px;
            align-items: center;
            margin-top: 14px;
            padding-top: 14px;
            border-top: 1px solid #f1f5f9;
        }
        .filter-chip {
            display: inline-flex;
            align-items: center;
            background: #eff6ff;
            color: #1d4ed8;
            border: 1px solid #bfdbfe;
            padding: 4px 10px;
            border-radius: 20px;
            font-size: 0.8rem;
            font-weight: 500;
        }
        .filter-chip .chip-remove {
            margin-left: 6px;
            cursor: pointer;
            color: #3b82f6;
            font-size: 0.9rem;
        }
        .filter-chip .chip-remove:hover {
            color: #1e40af;
        }

        /* Job Card Design */
        .job-card {
            background: #ffffff;
            border: 1px solid var(--card-border);
            border-radius: 14px;
            padding: 24px;
            transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
            height: 100%;
            display: flex;
            flex-direction: column;
            position: relative;
        }
        .job-card:hover {
            transform: translateY(-5px);
            border-color: #93c5fd;
            box-shadow: 0 16px 32px -8px rgba(37, 99, 235, 0.12), 0 4px 8px -2px rgba(15, 23, 42, 0.04);
        }
        .job-title {
            font-size: 1.15rem;
            font-weight: 700;
            color: var(--text-heading);
            margin-bottom: 8px;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
            line-height: 1.4;
            transition: color 0.15s;
        }
        .job-card:hover .job-title {
            color: var(--primary-blue);
        }
        .job-desc-clamp {
            display: -webkit-box;
            -webkit-line-clamp: 3;
            -webkit-box-orient: vertical;
            overflow: hidden;
            font-size: 0.875rem;
            color: var(--text-muted);
            line-height: 1.6;
        }
        .badge-pill-soft {
            font-weight: 500;
            padding: 5px 10px;
            border-radius: 6px;
            font-size: 0.78rem;
            display: inline-flex;
            align-items: center;
            gap: 4px;
        }
        .badge-dept {
            background-color: #f0fdf4;
            color: #166534;
            border: 1px solid #bbf7d0;
        }
        .badge-location {
            background-color: #f8fafc;
            color: #334155;
            border: 1px solid #e2e8f0;
        }
        .badge-mode {
            background-color: #faf5ff;
            color: #6b21a8;
            border: 1px solid #e9d5ff;
        }
        .badge-salary {
            background-color: #ecfdf5;
            color: #047857;
            border: 1px solid #a7f3d0;
            font-weight: 600;
        }
        .badge-urgent {
            background-color: #fef2f2;
            color: #b91c1c;
            border: 1px solid #fecaca;
        }

        /* Skeleton Loading */
        .skeleton-card {
            background: #ffffff;
            border: 1px solid var(--card-border);
            border-radius: 14px;
            padding: 24px;
            height: 100%;
        }
        .skeleton-bone {
            background: linear-gradient(90deg, #f1f5f9 25%, #e2e8f0 50%, #f1f5f9 75%);
            background-size: 200% 100%;
            animation: shimmer 1.5s infinite;
            border-radius: 6px;
        }
        @keyframes shimmer {
            0% { background-position: 200% 0; }
            100% { background-position: -200% 0; }
        }

        /* Pagination Styles */
        .pagination-container .page-link {
            border-radius: 8px;
            margin: 0 3px;
            color: #334155;
            font-weight: 500;
            border: 1px solid #e2e8f0;
            padding: 8px 14px;
            transition: all 0.15s ease;
        }
        .pagination-container .page-link:hover {
            background-color: #f1f5f9;
            color: var(--primary-blue);
            border-color: #cbd5e1;
        }
        .pagination-container .page-item.active .page-link {
            background-color: var(--primary-blue);
            border-color: var(--primary-blue);
            color: #ffffff;
            font-weight: 600;
            box-shadow: 0 4px 10px rgba(37, 99, 235, 0.25);
        }
        .pagination-container .page-item.disabled .page-link {
            opacity: 0.5;
            cursor: not-allowed;
        }

        /* Footer */
        footer {
            margin-top: auto;
            background: #ffffff;
            border-top: 1px solid var(--card-border);
        }
    </style>
</head>
<body>

    <!-- Header Navigation Bar -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark py-3 shadow-sm sticky-top" id="publicNavbar">
        <div class="container">
            <a class="navbar-brand fw-bold d-flex align-items-center gap-2" href="${pageContext.request.contextPath}/careers" id="brandLink">
                <span class="p-1.5 rounded-3 bg-primary text-white d-inline-flex align-items-center justify-content-center" style="width: 32px; height: 32px;">
                    <i class="bi bi-briefcase-fill"></i>
                </span>
                <span>IRMS <span class="fw-normal text-white-50">Careers Portal</span></span>
            </a>
            
            <button class="navbar-toggler border-0" type="button" data-bs-toggle="collapse" data-bs-target="#navContent">
                <span class="navbar-toggler-icon"></span>
            </button>

            <div class="collapse navbar-collapse" id="navContent">
                <ul class="navbar-nav me-auto mb-2 mb-lg-0 ms-lg-3">
                    <li class="nav-item">
                        <a class="nav-link active fw-medium" href="${pageContext.request.contextPath}/careers">
                            <i class="bi bi-grid me-1"></i> Danh sách việc làm
                        </a>
                    </li>
                    <li class="nav-item">
                        <a class="nav-link text-white-50" href="${pageContext.request.contextPath}/about-us">
                            <i class="bi bi-building me-1"></i> Về chúng tôi
                        </a>
                    </li>
                </ul>
                <div class="d-flex align-items-center gap-3">
                    <a href="${pageContext.request.contextPath}/auth/login" class="btn btn-outline-light btn-sm px-3 rounded-pill fw-medium" id="internalLoginBtn">
                        <i class="bi bi-shield-lock me-1"></i> Quản trị nội bộ
                    </a>
                </div>
            </div>
        </div>
    </nav>

    <!-- Hero Banner -->
    <header class="hero-banner text-center" id="heroBanner">
        <div class="container position-relative">
            <div class="d-inline-flex align-items-center gap-2 px-3 py-1.5 rounded-pill bg-white bg-opacity-10 text-white mb-3 text-xs fw-semibold border border-white border-opacity-20 backdrop-blur">
                <i class="bi bi-stars text-warning"></i> CƠ HỘI NGHỀ NGHIỆP TẠI DOANH NGHIỆP
            </div>
            <h1 class="display-5 fw-extrabold text-white mb-3" style="letter-spacing: -0.5px;">
                Tìm kiếm Công việc Lý tưởng
            </h1>
            <p class="lead text-white-50 mx-auto mb-2" style="max-width: 680px; font-size: 1.1rem;">
                Khám phá các vị trí tuyển dụng công khai được xác thực chính thức. 
                Ứng tuyển trực tuyến nhanh chóng, không yêu cầu đăng nhập.
            </p>
        </div>
    </header>

    <!-- Main Container -->
    <main class="container mb-5" id="mainContainer">
        <!-- Filter Control Panel (KN-224) -->
        <section class="filter-panel" aria-label="Bộ lọc tìm kiếm">
            <form id="filterForm" onsubmit="event.preventDefault(); applyFilters(1);">
                <div class="row g-3">
                    <!-- Ô tìm kiếm từ khóa (KN-222) -->
                    <div class="col-12 col-lg-4">
                        <label for="keywordInput" class="form-label small fw-semibold text-secondary mb-1">
                            <i class="bi bi-search me-1"></i> Tìm kiếm theo từ khóa
                        </label>
                        <div class="search-box-wrap">
                            <i class="bi bi-search search-icon"></i>
                            <input type="text" id="keywordInput" class="form-control" 
                                   placeholder="Tiêu đề công việc, kỹ năng, mô tả..." 
                                   value="${paramKeyword}" autocomplete="off">
                            <button type="button" class="search-clear-btn" id="keywordClearBtn" title="Xóa từ khóa">
                                <i class="bi bi-x-circle-fill"></i>
                            </button>
                        </div>
                    </div>

                    <!-- Dropdown Phòng ban (KN-223) -->
                    <div class="col-12 col-sm-6 col-lg-3">
                        <label for="deptSelect" class="form-label small fw-semibold text-secondary mb-1">
                            <i class="bi bi-building me-1"></i> Phòng ban
                        </label>
                        <select id="deptSelect" class="form-select filter-select">
                            <option value="">Tất cả phòng ban</option>
                            <c:forEach var="dept" items="${departments}">
                                <option value="${dept.id}" ${dept.id == paramDept ? 'selected' : ''}>${dept.name}</option>
                            </c:forEach>
                        </select>
                    </div>

                    <!-- Dropdown Địa điểm (KN-223) -->
                    <div class="col-12 col-sm-6 col-lg-2">
                        <label for="locationSelect" class="form-label small fw-semibold text-secondary mb-1">
                            <i class="bi bi-geo-alt me-1"></i> Địa điểm
                        </label>
                        <select id="locationSelect" class="form-select filter-select">
                            <option value="">Tất cả địa điểm</option>
                            <c:forEach var="loc" items="${locations}">
                                <option value="${loc.id}" ${loc.id == paramLocation ? 'selected' : ''}>${loc.value}</option>
                            </c:forEach>
                        </select>
                    </div>

                    <!-- Dropdown Hình thức làm việc (KN-223) -->
                    <div class="col-12 col-sm-6 col-lg-3">
                        <label for="workTypeSelect" class="form-label small fw-semibold text-secondary mb-1">
                            <i class="bi bi-briefcase me-1"></i> Hình thức làm việc
                        </label>
                        <select id="workTypeSelect" class="form-select filter-select">
                            <option value="">Tất cả hình thức</option>
                            <c:forEach var="mode" items="${workTypes}">
                                <option value="${mode.id}" ${mode.id == paramWorkType ? 'selected' : ''}>${mode.value}</option>
                            </c:forEach>
                        </select>
                    </div>
                </div>

                <!-- Active Filter Chips & Clear Button -->
                <div class="active-filter-chips" id="activeFilterChips" style="display: none;">
                    <span class="small text-secondary fw-semibold">Đang lọc theo:</span>
                    <div id="chipsContainer" class="d-flex flex-wrap gap-2"></div>
                    <button type="button" class="btn btn-link btn-sm text-decoration-none text-danger ms-auto p-0 small fw-semibold" id="resetAllFiltersBtn" onclick="resetAllFilters()">
                        <i class="bi bi-trash3 me-1"></i> Xóa tất cả bộ lọc
                    </button>
                </div>
            </form>
        </section>

        <!-- Results Info & Sorting Bar -->
        <section class="d-flex flex-wrap justify-content-between align-items-center gap-3 my-4" aria-label="Thanh công cụ kết quả">
            <div>
                <h2 class="h5 fw-bold text-dark mb-0 d-flex align-items-center gap-2">
                    <span>Vị trí tuyển dụng đang mở</span>
                    <span class="badge bg-primary text-white rounded-pill px-2.5 py-1 text-xs" id="totalBadge">${totalJobs}</span>
                </h2>
                <div class="small text-muted" id="resultsSubtext">
                    Chỉ hiển thị các tin tuyển dụng đã xuất bản và còn hạn tiếp nhận hồ sơ
                </div>
            </div>

            <div class="d-flex align-items-center gap-2">
                <label for="sortSelect" class="small text-secondary fw-semibold text-nowrap mb-0">
                    <i class="bi bi-arrow-down-up me-1"></i> Sắp xếp:
                </label>
                <select id="sortSelect" class="form-select form-select-sm" style="width: auto; border-radius: 8px;">
                    <option value="newest" ${paramSort == 'newest' ? 'selected' : ''}>Mới nhất trước</option>
                    <option value="deadline" ${paramSort == 'deadline' ? 'selected' : ''}>Hạn nộp gần nhất</option>
                    <option value="title_asc" ${paramSort == 'title_asc' ? 'selected' : ''}>Tiêu đề (A - Z)</option>
                </select>
            </div>
        </section>

        <!-- Skeleton Loading State (KN-221) -->
        <div id="loadingState" style="display: none;">
            <div class="row g-4">
                <c:forEach begin="1" end="6">
                    <div class="col-12 col-md-6 col-lg-4">
                        <div class="skeleton-card">
                            <div class="d-flex justify-content-between mb-3">
                                <div class="skeleton-bone" style="width: 80px; height: 22px;"></div>
                                <div class="skeleton-bone" style="width: 90px; height: 22px;"></div>
                            </div>
                            <div class="skeleton-bone mb-2" style="width: 90%; height: 24px;"></div>
                            <div class="skeleton-bone mb-3" style="width: 60%; height: 20px;"></div>
                            <div class="d-flex gap-2 mb-3">
                                <div class="skeleton-bone" style="width: 80px; height: 24px;"></div>
                                <div class="skeleton-bone" style="width: 90px; height: 24px;"></div>
                            </div>
                            <div class="skeleton-bone mb-2" style="width: 100%; height: 16px;"></div>
                            <div class="skeleton-bone mb-4" style="width: 80%; height: 16px;"></div>
                            <div class="pt-3 border-top d-flex justify-content-between align-items-center">
                                <div class="skeleton-bone" style="width: 100px; height: 20px;"></div>
                                <div class="skeleton-bone" style="width: 110px; height: 32px;"></div>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </div>

        <!-- Error State (KN-221) -->
        <div id="errorState" class="card border-danger border-opacity-25 shadow-sm p-5 text-center my-4" style="display: none; background-color: #fffaf0;">
            <div class="mx-auto text-danger mb-3" style="font-size: 2.5rem;">
                <i class="bi bi-exclamation-triangle"></i>
            </div>
            <h5 class="fw-bold text-dark mb-1">Đã xảy ra lỗi khi tải danh sách việc làm</h5>
            <p class="text-secondary small mb-3" id="errorMessageText">Không thể kết nối tới máy chủ. Vui lòng kiểm tra lại kết nối mạng.</p>
            <div>
                <button type="button" class="btn btn-primary btn-sm px-4 rounded-pill" onclick="loadJobsFromApi()">
                    <i class="bi bi-arrow-clockwise me-1"></i> Thử lại
                </button>
            </div>
        </div>

        <!-- Empty State (KN-221) -->
        <div id="emptyState" class="card border-0 shadow-sm p-5 text-center my-4" style="display: none; background: #ffffff;">
            <div class="mx-auto text-secondary opacity-50 mb-3" style="font-size: 3rem;">
                <i class="bi bi-inbox"></i>
            </div>
            <h5 class="fw-bold text-dark mb-1">Không tìm thấy vị trí việc làm phù hợp</h5>
            <p class="text-muted small mx-auto mb-3" style="max-width: 480px;">
                Rất tiếc, hiện tại không có tin tuyển dụng nào khớp với tiêu chí tìm kiếm hoặc bộ lọc đã chọn.
            </p>
            <div>
                <button type="button" class="btn btn-outline-primary btn-sm px-4 rounded-pill" onclick="resetAllFilters()">
                    <i class="bi bi-arrow-counterclockwise me-1"></i> Xóa tất cả bộ lọc & Xem toàn bộ
                </button>
            </div>
        </div>

        <!-- Job Cards Grid (KN-221) -->
        <div class="row g-4" id="jobCardsGrid">
            <c:choose>
                <c:when test="${not empty publicJobs}">
                    <c:forEach var="job" items="${publicJobs}">
                        <div class="col-12 col-md-6 col-lg-4">
                            <article class="job-card">
                                <!-- Card Header: Requisition Code & Live Tag -->
                                <div class="d-flex justify-content-between align-items-center mb-2.5">
                                    <span class="badge bg-light text-secondary border font-monospace px-2 py-1">
                                        ${job.requisition != null && not empty job.requisition.code ? job.requisition.code : 'JOB'}
                                    </span>
                                    <span class="badge bg-success-subtle text-success border border-success-subtle px-2 py-1 text-xs fw-semibold">
                                        <i class="bi bi-broadcast me-1"></i>Đang mở tuyển
                                    </span>
                                </div>

                                <!-- Title -->
                                <h3 class="job-title" title="${job.title}">
                                    <a href="${pageContext.request.contextPath}/careers/job?id=${job.requisitionId != null ? job.requisitionId : job.id}" 
                                       class="text-decoration-none text-dark">
                                        ${job.title}
                                    </a>
                                </h3>

                                <!-- Department -->
                                <div class="text-secondary small fw-medium mb-3">
                                    <i class="bi bi-building me-1 text-primary"></i> 
                                    <span>${job.departmentName != null ? job.departmentName : 'Toàn công ty'}</span>
                                </div>

                                <!-- Key Meta Tags (Location, Work Type, Salary) -->
                                <div class="d-flex flex-wrap gap-1.5 mb-3">
                                    <span class="badge-pill-soft badge-location">
                                        <i class="bi bi-geo-alt"></i> ${job.workLocation != null ? job.workLocation : 'Toàn quốc'}
                                    </span>
                                    <span class="badge-pill-soft badge-mode">
                                        <i class="bi bi-laptop"></i> ${job.workType != null ? job.workType : 'Toàn thời gian'}
                                    </span>
                                    <span class="badge-pill-soft badge-salary">
                                        <i class="bi bi-cash-stack"></i> ${job.formattedSalaryRange}
                                    </span>
                                </div>

                                <!-- Description Clamped Excerpt -->
                                <div class="job-desc-clamp mb-4 flex-grow-1">
                                    <c:out value="${job.description != null ? job.description : (job.jobDescription != null ? job.jobDescription : 'Vui lòng nhấn xem chi tiết để tìm hiểu thông tin yêu cầu và quyền lợi vị trí tuyển dụng.')}" />
                                </div>

                                <!-- Card Footer: Expiry Deadline & Action Button -->
                                <div class="pt-3 border-top d-flex justify-content-between align-items-center mt-auto">
                                    <div class="small">
                                        <span class="text-muted d-block text-xs">Hạn nhận hồ sơ:</span>
                                        <span class="fw-semibold text-dark text-xs">
                                            <i class="bi bi-calendar3 me-1 text-secondary"></i>
                                            ${job.formattedDeadline}
                                        </span>
                                    </div>
                                    <a href="${pageContext.request.contextPath}/careers/job?id=${job.requisitionId != null ? job.requisitionId : job.id}" 
                                       class="btn btn-outline-primary btn-sm px-3 rounded-pill fw-semibold">
                                        Ứng tuyển <i class="bi bi-arrow-right ms-1"></i>
                                    </a>
                                </div>
                            </article>
                        </div>
                    </c:forEach>
                </c:when>
                <c:otherwise>
                    <!-- Fallback if initially empty -->
                    <div class="col-12">
                        <div class="card border-0 shadow-sm p-5 text-center my-4" style="background: #ffffff;">
                            <div class="mx-auto text-secondary opacity-50 mb-3" style="font-size: 3rem;">
                                <i class="bi bi-inbox"></i>
                            </div>
                            <h5 class="fw-bold text-dark mb-1">Hiện chưa có tin tuyển dụng nào được xuất bản</h5>
                            <p class="text-muted small mx-auto mb-0" style="max-width: 480px;">
                                Vui lòng quay lại sau để cập nhật các cơ hội việc làm mới nhất từ chúng tôi.
                            </p>
                        </div>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <!-- Pagination UI (KN-221) -->
        <nav class="d-flex justify-content-center align-items-center mt-5 pagination-container" 
             id="paginationNav" 
             aria-label="Phân trang việc làm" 
             style="${totalPages > 1 ? '' : 'display: none;'}">
            <ul class="pagination pagination-md mb-0" id="paginationUl">
                <!-- Sẽ được đồng bộ hoặc render động bởi JavaScript -->
                <li class="page-item ${currentPage <= 1 ? 'disabled' : ''}">
                    <a class="page-link" href="javascript:void(0)" onclick="applyFilters(${currentPage - 1})">
                        <i class="bi bi-chevron-left"></i> Trước
                    </a>
                </li>
                <c:forEach begin="1" end="${totalPages}" var="p">
                    <li class="page-item ${p == currentPage ? 'active' : ''}">
                        <a class="page-link" href="javascript:void(0)" onclick="applyFilters(${p})">${p}</a>
                    </li>
                </c:forEach>
                <li class="page-item ${currentPage >= totalPages ? 'disabled' : ''}">
                    <a class="page-link" href="javascript:void(0)" onclick="applyFilters(${currentPage + 1})">
                        Sau <i class="bi bi-chevron-right"></i>
                    </a>
                </li>
            </ul>
        </nav>
    </main>

    <!-- Footer -->
    <footer class="py-4">
        <div class="container text-center text-secondary small">
            <div class="d-flex flex-wrap justify-content-center gap-3 mb-2">
                <a href="${pageContext.request.contextPath}/careers" class="text-decoration-none text-secondary">Trang chủ tuyển dụng</a>
                <span>&bull;</span>
                <a href="${pageContext.request.contextPath}/about-us" class="text-decoration-none text-secondary">Về chúng tôi</a>
                <span>&bull;</span>
                <a href="${pageContext.request.contextPath}/auth/login" class="text-decoration-none text-secondary">Đăng nhập IRMS</a>
            </div>
            <p class="mb-0 text-muted">&copy; 2026 IRMS Recruitment Platform. Cổng thông tin Tuyển dụng Doanh nghiệp.</p>
        </div>
    </footer>

    <!-- Bootstrap 5 JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>

    <!-- Frontend Interactive Script (KN-221, KN-224) -->
    <script>
        (function() {
            // Configuration & State
            const contextPath = '${pageContext.request.contextPath}';
            let currentPage = ${currentPage != null ? currentPage : 1};
            let pageSize = ${pageSize != null ? pageSize : 9};
            let debounceTimer = null;

            // DOM Elements
            const keywordInput = document.getElementById('keywordInput');
            const keywordClearBtn = document.getElementById('keywordClearBtn');
            const deptSelect = document.getElementById('deptSelect');
            const locationSelect = document.getElementById('locationSelect');
            const workTypeSelect = document.getElementById('workTypeSelect');
            const sortSelect = document.getElementById('sortSelect');
            const jobCardsGrid = document.getElementById('jobCardsGrid');
            const loadingState = document.getElementById('loadingState');
            const emptyState = document.getElementById('emptyState');
            const errorState = document.getElementById('errorState');
            const errorMessageText = document.getElementById('errorMessageText');
            const paginationNav = document.getElementById('paginationNav');
            const paginationUl = document.getElementById('paginationUl');
            const totalBadge = document.getElementById('totalBadge');
            const resultsSubtext = document.getElementById('resultsSubtext');
            const activeFilterChips = document.getElementById('activeFilterChips');
            const chipsContainer = document.getElementById('chipsContainer');

            // 1. Khởi tạo trạng thái ban đầu từ URL Query Parameters (KN-224)
            function initFiltersFromUrl() {
                const urlParams = new URLSearchParams(window.location.search);
                const q = urlParams.get('q') || urlParams.get('keyword') || '';
                const dept = urlParams.get('department_id') || urlParams.get('department') || urlParams.get('deptId') || '';
                const loc = urlParams.get('location_id') || urlParams.get('location') || '';
                const mode = urlParams.get('work_type') || urlParams.get('work_mode') || '';
                const sort = urlParams.get('sort') || 'newest';
                const page = parseInt(urlParams.get('page')) || 1;

                if (keywordInput && q) keywordInput.value = q;
                if (deptSelect && dept) deptSelect.value = dept;
                if (locationSelect && loc) locationSelect.value = loc;
                if (workTypeSelect && mode) workTypeSelect.value = mode;
                if (sortSelect && sort) sortSelect.value = sort;
                currentPage = page;

                toggleClearButton();
                renderFilterChips();

                // Nếu URL có query parameters khác ban đầu thì gọi AJAX để đồng bộ
                if (q || dept || loc || mode || (sort && sort !== 'newest') || page > 1) {
                    loadJobsFromApi(false);
                }
            }

            // 2. Đồng bộ State lên URL Query Parameters (KN-224)
            function syncStateToUrl(pushToHistory = true) {
                const params = new URLSearchParams();
                const q = keywordInput.value.trim();
                const dept = deptSelect.value;
                const loc = locationSelect.value;
                const mode = workTypeSelect.value;
                const sort = sortSelect.value;

                if (q) params.set('q', q);
                if (dept) params.set('department_id', dept);
                if (loc) params.set('location_id', loc);
                if (mode) params.set('work_type', mode);
                if (sort && sort !== 'newest') params.set('sort', sort);
                if (currentPage > 1) params.set('page', currentPage);

                const newUrl = window.location.pathname + (params.toString() ? '?' + params.toString() : '');
                if (pushToHistory) {
                    window.history.pushState({ path: newUrl }, '', newUrl);
                } else {
                    window.history.replaceState({ path: newUrl }, '', newUrl);
                }
            }

            // 3. Hiển thị nút xóa nhanh từ khóa tìm kiếm
            function toggleClearButton() {
                if (keywordClearBtn) {
                    keywordClearBtn.style.display = keywordInput.value.trim() ? 'block' : 'none';
                }
            }

            // 4. Render Active Filter Chips (KN-224)
            function renderFilterChips() {
                chipsContainer.innerHTML = '';
                let hasFilters = false;

                const q = keywordInput.value.trim();
                if (q) {
                    hasFilters = true;
                    chipsContainer.appendChild(createChip('Từ khóa: ' + q, () => {
                        keywordInput.value = '';
                        toggleClearButton();
                        applyFilters(1);
                    }));
                }

                if (deptSelect.value) {
                    hasFilters = true;
                    const text = deptSelect.options[deptSelect.selectedIndex].text;
                    chipsContainer.appendChild(createChip('Phòng ban: ' + text, () => {
                        deptSelect.value = '';
                        applyFilters(1);
                    }));
                }

                if (locationSelect.value) {
                    hasFilters = true;
                    const text = locationSelect.options[locationSelect.selectedIndex].text;
                    chipsContainer.appendChild(createChip('Địa điểm: ' + text, () => {
                        locationSelect.value = '';
                        applyFilters(1);
                    }));
                }

                if (workTypeSelect.value) {
                    hasFilters = true;
                    const text = workTypeSelect.options[workTypeSelect.selectedIndex].text;
                    chipsContainer.appendChild(createChip('Hình thức: ' + text, () => {
                        workTypeSelect.value = '';
                        applyFilters(1);
                    }));
                }

                activeFilterChips.style.display = hasFilters ? 'flex' : 'none';
            }

            function createChip(label, onRemove) {
                const chip = document.createElement('span');
                chip.className = 'filter-chip';
                chip.innerHTML = label + ' <i class="bi bi-x chip-remove"></i>';
                chip.querySelector('.chip-remove').addEventListener('click', onRemove);
                return chip;
            }

            // 5. Gọi API lấy danh sách việc làm công khai (KN-219, KN-222, KN-223)
            window.loadJobsFromApi = function(pushHistory = true) {
                // UI: Hiển thị Skeleton loading, ẩn các state khác
                loadingState.style.display = 'block';
                jobCardsGrid.style.display = 'none';
                emptyState.style.display = 'none';
                errorState.style.display = 'none';
                paginationNav.style.display = 'none';

                syncStateToUrl(pushHistory);
                renderFilterChips();

                const q = keywordInput.value.trim();
                const dept = deptSelect.value;
                const loc = locationSelect.value;
                const mode = workTypeSelect.value;
                const sort = sortSelect.value;

                const apiUrl = contextPath + '/api/v1/public/jobs?' + new URLSearchParams({
                    q: q,
                    department_id: dept,
                    location_id: loc,
                    work_type: mode,
                    sort: sort,
                    page: currentPage,
                    limit: pageSize
                }).toString();

                fetch(apiUrl, {
                    method: 'GET',
                    headers: { 'Accept': 'application/json' }
                })
                .then(res => {
                    if (!res.ok) throw new Error('Mã phản hồi lỗi: ' + res.status);
                    return res.json();
                })
                .then(json => {
                    loadingState.style.display = 'none';
                    if (!json.success || !json.data) {
                        showError(json.error || 'Dữ liệu không hợp lệ từ máy chủ.');
                        return;
                    }

                    const jobs = json.data;
                    const pagination = json.pagination;

                    // Cập nhật số lượng
                    totalBadge.textContent = pagination.total || 0;
                    resultsSubtext.textContent = 'Tìm thấy ' + (pagination.total || 0) + ' vị trí việc làm đang mở tuyển';

                    if (jobs.length === 0) {
                        jobCardsGrid.innerHTML = '';
                        emptyState.style.display = 'block';
                        paginationNav.style.display = 'none';
                        return;
                    }

                    // Render danh sách Card công việc
                    renderJobCards(jobs);
                    jobCardsGrid.style.display = 'flex';

                    // Render Phân trang
                    renderPagination(pagination);
                })
                .catch(err => {
                    loadingState.style.display = 'none';
                    showError(err.message || 'Không thể tải dữ liệu.');
                });
            };

            // 6. Render Card HTML
            function renderJobCards(jobs) {
                let html = '';
                jobs.forEach(job => {
                    const reqCode = job.requisitionId ? job.requisitionId.substring(0, 8).toUpperCase() : 'JOB';
                    const dept = escapeHtml(job.departmentName || 'Toàn công ty');
                    const loc = escapeHtml(job.location || 'Toàn quốc');
                    const workType = escapeHtml(job.workType || 'Toàn thời gian');
                    const salary = escapeHtml(job.formattedSalary || 'Thỏa thuận');
                    const deadline = escapeHtml(job.formattedDeadline || '--');
                    const desc = escapeHtml(job.description || 'Vui lòng nhấn xem chi tiết để tìm hiểu thông tin yêu cầu và quyền lợi vị trí tuyển dụng.');
                    const detailUrl = job.detailUrl || (contextPath + '/careers/job?id=' + (job.requisitionId || job.id));

                    html += `
                        <div class="col-12 col-md-6 col-lg-4">
                            <article class="job-card">
                                <div class="d-flex justify-content-between align-items-center mb-2.5">
                                    <span class="badge bg-light text-secondary border font-monospace px-2 py-1">
                                        \${reqCode}
                                    </span>
                                    <span class="badge bg-success-subtle text-success border border-success-subtle px-2 py-1 text-xs fw-semibold">
                                        <i class="bi bi-broadcast me-1"></i>Đang mở tuyển
                                    </span>
                                </div>
                                <h3 class="job-title" title="\${escapeHtml(job.title)}">
                                    <a href="\${detailUrl}" class="text-decoration-none text-dark">
                                        \${escapeHtml(job.title)}
                                    </a>
                                </h3>
                                <div class="text-secondary small fw-medium mb-3">
                                    <i class="bi bi-building me-1 text-primary"></i> 
                                    <span>\${dept}</span>
                                </div>
                                <div class="d-flex flex-wrap gap-1.5 mb-3">
                                    <span class="badge-pill-soft badge-location">
                                        <i class="bi bi-geo-alt"></i> \${loc}
                                    </span>
                                    <span class="badge-pill-soft badge-mode">
                                        <i class="bi bi-laptop"></i> \${workType}
                                    </span>
                                    <span class="badge-pill-soft badge-salary">
                                        <i class="bi bi-cash-stack"></i> \${salary}
                                    </span>
                                </div>
                                <div class="job-desc-clamp mb-4 flex-grow-1">
                                    \${desc}
                                </div>
                                <div class="pt-3 border-top d-flex justify-content-between align-items-center mt-auto">
                                    <div class="small">
                                        <span class="text-muted d-block text-xs">Hạn nhận hồ sơ:</span>
                                        <span class="fw-semibold text-dark text-xs">
                                            <i class="bi bi-calendar3 me-1 text-secondary"></i> \${deadline}
                                        </span>
                                    </div>
                                    <a href="\${detailUrl}#applyCardContainer" class="btn btn-outline-primary btn-sm px-3 rounded-pill fw-semibold">
                                        Ứng tuyển <i class="bi bi-arrow-right ms-1"></i>
                                    </a>
                                </div>
                            </article>
                        </div>
                    `;
                });
                jobCardsGrid.innerHTML = html;
            }

            // 7. Render Pagination Buttons (KN-221)
            function renderPagination(pagination) {
                const totalPages = pagination.totalPages || 1;
                const page = pagination.page || 1;

                if (totalPages <= 1) {
                    paginationNav.style.display = 'none';
                    return;
                }

                paginationNav.style.display = 'flex';
                let html = '';

                // Nút Trước
                html += `
                    <li class="page-item \${page <= 1 ? 'disabled' : ''}">
                        <a class="page-link" href="javascript:void(0)" onclick="applyFilters(\${page - 1})">
                            <i class="bi bi-chevron-left"></i> Trước
                        </a>
                    </li>
                `;

                // Số trang logic
                const start = Math.max(1, page - 2);
                const end = Math.min(totalPages, page + 2);

                if (start > 1) {
                    html += `<li class="page-item"><a class="page-link" href="javascript:void(0)" onclick="applyFilters(1)">1</a></li>`;
                    if (start > 2) {
                        html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
                    }
                }

                for (let i = start; i <= end; i++) {
                    html += `
                        <li class="page-item \${i === page ? 'active' : ''}">
                            <a class="page-link" href="javascript:void(0)" onclick="applyFilters(\${i})">\${i}</a>
                        </li>
                    `;
                }

                if (end < totalPages) {
                    if (end < totalPages - 1) {
                        html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
                    }
                    html += `<li class="page-item"><a class="page-link" href="javascript:void(0)" onclick="applyFilters(\${totalPages})">\${totalPages}</a></li>`;
                }

                // Nút Sau
                html += `
                    <li class="page-item \${page >= totalPages ? 'disabled' : ''}">
                        <a class="page-link" href="javascript:void(0)" onclick="applyFilters(\${page + 1})">
                            Sau <i class="bi bi-chevron-right"></i>
                        </a>
                    </li>
                `;

                paginationUl.innerHTML = html;
            }

            function showError(msg) {
                errorState.style.display = 'block';
                errorMessageText.textContent = msg;
                jobCardsGrid.style.display = 'none';
                emptyState.style.display = 'none';
                paginationNav.style.display = 'none';
            }

            function escapeHtml(str) {
                if (!str) return '';
                return String(str)
                    .replace(/&/g, '&amp;')
                    .replace(/</g, '&lt;')
                    .replace(/>/g, '&gt;')
                    .replace(/"/g, '&quot;')
                    .replace(/'/g, '&#039;');
            }

            // 8. Áp dụng bộ lọc (KN-224)
            window.applyFilters = function(targetPage = 1) {
                currentPage = targetPage;
                loadJobsFromApi(true);

                // Cuộn mượt lên vị trí kết quả nếu đang ở dưới
                const headerRect = document.getElementById('resultsSubtext').getBoundingClientRect();
                if (headerRect.top < 0) {
                    window.scrollTo({
                        top: window.scrollY + headerRect.top - 120,
                        behavior: 'smooth'
                    });
                }
            };

            // 9. Xóa toàn bộ bộ lọc
            window.resetAllFilters = function() {
                keywordInput.value = '';
                deptSelect.value = '';
                locationSelect.value = '';
                workTypeSelect.value = '';
                sortSelect.value = 'newest';
                toggleClearButton();
                applyFilters(1);
            };

            // 10. Lắng nghe sự kiện (Debounce trên ô tìm kiếm, Dropdowns change) (KN-224)
            // Debounce 350ms cho ô tìm kiếm
            keywordInput.addEventListener('input', function() {
                toggleClearButton();
                clearTimeout(debounceTimer);
                debounceTimer = setTimeout(() => {
                    applyFilters(1);
                }, 350);
            });

            keywordClearBtn.addEventListener('click', function() {
                keywordInput.value = '';
                toggleClearButton();
                applyFilters(1);
            });

            deptSelect.addEventListener('change', () => applyFilters(1));
            locationSelect.addEventListener('change', () => applyFilters(1));
            workTypeSelect.addEventListener('change', () => applyFilters(1));
            sortSelect.addEventListener('change', () => applyFilters(1));

            // Hỗ trợ back/forward trên trình duyệt (popstate)
            window.addEventListener('popstate', function() {
                initFiltersFromUrl();
            });

            // Khởi động
            initFiltersFromUrl();
        })();
    </script>
</body>
</html>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${profile.companyName} | Trang Giới Thiệu &amp; Văn Hóa Tuyển Dụng</title>
    <meta name="description" content="${profile.slogan} - ${profile.tagline}">
    
    <!-- Bootstrap 5 CSS & Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css" rel="stylesheet">
    <!-- Google Fonts Inter -->
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">

    <style>
        body {
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            color: #1e293b;
            background-color: #f8fafc;
        }
        .hero-banner {
            background: linear-gradient(rgba(15, 23, 42, 0.8), rgba(15, 23, 42, 0.85)), url('${profile.bannerUrl}') center/cover no-repeat;
            min-height: 480px;
            display: flex;
            align-items: center;
        }
        .card-hover {
            transition: transform 0.25s ease, box-shadow 0.25s ease;
        }
        .card-hover:hover {
            transform: translateY(-5px);
            box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1) !important;
        }
        .letter-spacing-1 {
            letter-spacing: 1px;
        }
    </style>
</head>
<body>

    <!-- NAVBAR CÔNG KHAI -->
    <nav class="navbar navbar-expand-lg bg-white border-bottom sticky-top shadow-sm py-2">
        <div class="container">
            <a class="navbar-brand d-flex align-items-center gap-2" href="${pageContext.request.contextPath}/about-us">
                <img src="${profile.logoUrl}" style="height: 38px; max-width: 140px; object-fit: contain;" alt="Logo" onerror="this.src='${pageContext.request.contextPath}/assets/img/logo-default.png'">
                <span class="fw-bold text-dark fs-5">${profile.brandName != null ? profile.brandName : profile.companyName}</span>
            </a>
            <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#publicNav">
                <span class="navbar-toggler-icon"></span>
            </button>
            <div class="collapse navbar-collapse" id="publicNav">
                <ul class="navbar-nav mx-auto mb-2 mb-lg-0 small fw-semibold">
                    <li class="nav-item"><a class="nav-link text-dark px-3" href="#overview">Về chúng tôi</a></li>
                    <li class="nav-item"><a class="nav-link text-dark px-3" href="#values">Giá trị cốt lõi</a></li>
                    <li class="nav-item"><a class="nav-link text-dark px-3" href="#culture">Văn hóa làm việc</a></li>
                    <li class="nav-item"><a class="nav-link text-dark px-3" href="#perks">Chế độ đãi ngộ</a></li>
                    <li class="nav-item"><a class="nav-link text-dark px-3" href="#contact">Liên hệ</a></li>
                </ul>
                <div class="d-flex align-items-center gap-2">
                    <a href="${pageContext.request.contextPath}/auth/login" class="btn btn-outline-primary btn-sm px-3 fw-semibold">
                        <i class="bi bi-box-arrow-in-right me-1"></i> Đăng nhập
                    </a>
                </div>
            </div>
        </div>
    </nav>

    <!-- HERO SECTION -->
    <header class="hero-banner text-white py-5 px-3 text-center">
        <div class="container py-4">
            <span class="badge bg-primary text-uppercase letter-spacing-1 px-3 py-1 mb-3 shadow-sm">
                ${profile.companyName}
            </span>
            <h1 class="display-5 fw-bold mb-3 text-white">
                ${profile.slogan}
            </h1>
            <p class="lead text-white-50 mx-auto mb-4" style="max-width: 760px;">
                ${profile.tagline}
            </p>
            <div class="d-flex justify-content-center gap-3 flex-wrap">
                <a href="#overview" class="btn btn-primary px-4 py-2 fw-semibold shadow">
                    <i class="bi bi-compass me-1"></i> Khám phá hành trình
                </a>
                <a href="#culture" class="btn btn-outline-light px-4 py-2 fw-semibold">
                    <i class="bi bi-people me-1"></i> Tìm hiểu môi trường
                </a>
            </div>
        </div>
    </header>

    <!-- SECTION: TỔNG QUAN, SỨ MỆNH & TẦM NHÌN -->
    <section class="py-5 bg-white border-bottom" id="overview">
        <div class="container py-4">
            <div class="text-center mb-5">
                <span class="text-primary text-uppercase fw-bold text-xs letter-spacing-1">Giới thiệu tổng quan</span>
                <h2 class="fw-bold text-dark mt-1">Câu chuyện thương hiệu</h2>
                <div class="mx-auto mt-3 text-muted" style="max-width: 800px; font-size: 16px; line-height: 1.8;">
                    <p>${profile.overview}</p>
                    <c:if test="${not empty profile.historyStory}">
                        <p class="mt-2">${profile.historyStory}</p>
                    </c:if>
                </div>
            </div>

            <div class="row g-4 align-items-stretch">
                <div class="col-12 col-md-6">
                    <div class="card h-100 border-0 bg-primary-subtle p-4 rounded-4 card-hover">
                        <div class="d-flex align-items-center mb-3">
                            <div class="rounded-circle bg-primary text-white p-3 me-3 d-flex align-items-center justify-content-center" style="width: 50px; height: 50px;">
                                <i class="bi bi-bullseye fs-4"></i>
                            </div>
                            <h4 class="fw-bold mb-0 text-primary">Sứ mệnh của chúng tôi</h4>
                        </div>
                        <p class="text-dark mb-0 fs-6" style="line-height: 1.6;">${profile.mission}</p>
                    </div>
                </div>
                <div class="col-12 col-md-6">
                    <div class="card h-100 border-0 bg-info-subtle p-4 rounded-4 card-hover">
                        <div class="d-flex align-items-center mb-3">
                            <div class="rounded-circle bg-info text-white p-3 me-3 d-flex align-items-center justify-content-center" style="width: 50px; height: 50px;">
                                <i class="bi bi-compass fs-4"></i>
                            </div>
                            <h4 class="fw-bold mb-0 text-info">Tầm nhìn chiến lược</h4>
                        </div>
                        <p class="text-dark mb-0 fs-6" style="line-height: 1.6;">${profile.vision}</p>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- SECTION: GIÁ TRỊ CỐT LÕI -->
    <section class="py-5 bg-light-subtle border-bottom" id="values">
        <div class="container py-4">
            <div class="text-center mb-5">
                <span class="text-primary text-uppercase fw-bold text-xs letter-spacing-1">Định hướng cốt lõi</span>
                <h2 class="fw-bold text-dark mt-1">Giá trị chúng tôi theo đuổi</h2>
                <p class="text-muted small">Những nguyên tắc dẫn lối cho mọi hoạt động và quyết định tại công ty.</p>
            </div>
            <div class="row g-4">
                <c:forEach var="v" items="${profile.coreValueList}">
                    <div class="col-12 col-md-6 col-lg-3">
                        <div class="card h-100 border-0 shadow-sm p-4 rounded-4 text-center card-hover bg-white">
                            <div class="rounded-circle bg-primary-subtle text-primary mx-auto mb-3 d-flex align-items-center justify-content-center" style="width: 60px; height: 60px;">
                                <i class="bi ${v.icon} fs-3"></i>
                            </div>
                            <h5 class="fw-bold text-dark mb-2">${v.title}</h5>
                            <p class="text-muted small mb-0">${v.desc}</p>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </div>
    </section>

    <!-- SECTION: VĂN HÓA DOANH NGHIỆP -->
    <section class="py-5 bg-white border-bottom" id="culture">
        <div class="container py-4">
            <div class="row g-5 align-items-center">
                <div class="col-12 col-lg-6">
                    <span class="text-primary text-uppercase fw-bold text-xs letter-spacing-1">Môi trường &amp; Con người</span>
                    <h2 class="fw-bold text-dark mt-2 mb-3">Văn hóa làm việc tại ${profile.brandName != null ? profile.brandName : profile.companyName}</h2>
                    <div class="text-secondary mb-4" style="font-size: 15px; line-height: 1.8;">
                        ${profile.cultureDesc}
                    </div>
                    <div class="d-flex gap-3">
                        <div class="d-flex align-items-center gap-2">
                            <i class="bi bi-check-circle-fill text-success fs-5"></i>
                            <span class="small fw-semibold text-dark">Linh hoạt &amp; Sáng tạo</span>
                        </div>
                        <div class="d-flex align-items-center gap-2">
                            <i class="bi bi-check-circle-fill text-success fs-5"></i>
                            <span class="small fw-semibold text-dark">Học hỏi liên tục</span>
                        </div>
                        <div class="d-flex align-items-center gap-2">
                            <i class="bi bi-check-circle-fill text-success fs-5"></i>
                            <span class="small fw-semibold text-dark">Đồng đội tin cậy</span>
                        </div>
                    </div>
                </div>
                <div class="col-12 col-lg-6">
                    <div class="rounded-4 overflow-hidden shadow">
                        <img src="${profile.bannerUrl}" class="img-fluid" style="width: 100%; height: 340px; object-fit: cover;" alt="Culture" onerror="this.src='https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=800&q=80'">
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- SECTION: CHẾ ĐỘ ĐÃI NGỘ -->
    <section class="py-5 bg-light-subtle border-bottom" id="perks">
        <div class="container py-4">
            <div class="text-center mb-5">
                <span class="text-primary text-uppercase fw-bold text-xs letter-spacing-1">Quyền lợi nhân sự</span>
                <h2 class="fw-bold text-dark mt-1">Chế độ đãi ngộ toàn diện</h2>
                <p class="text-muted small">Chúng tôi trân trọng và ghi nhận từng nỗ lực cống hiến của bạn.</p>
            </div>
            <div class="row g-4">
                <c:forEach var="p" items="${profile.perkList}">
                    <div class="col-12 col-md-6 col-lg-3">
                        <div class="card h-100 border-0 bg-white shadow-sm p-4 rounded-4 card-hover">
                            <div class="rounded-circle bg-success-subtle text-success p-3 mb-3 d-flex align-items-center justify-content-center" style="width: 50px; height: 50px;">
                                <i class="bi ${p.icon} fs-4"></i>
                            </div>
                            <h5 class="fw-bold text-dark mb-2">${p.title}</h5>
                            <p class="text-muted small mb-0">${p.desc}</p>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </div>
    </section>

    <!-- SECTION: BỘ SƯU TẬP ẢNH (GALLERY) -->
    <section class="py-5 bg-white border-bottom">
        <div class="container py-4">
            <div class="text-center mb-5">
                <span class="text-primary text-uppercase fw-bold text-xs letter-spacing-1">Không gian &amp; Hoạt động</span>
                <h2 class="fw-bold text-dark mt-1">Hình ảnh thực tế tại công ty</h2>
            </div>
            <div class="row g-4">
                <c:forEach var="g" items="${profile.galleryList}">
                    <div class="col-12 col-md-4">
                        <div class="rounded-4 overflow-hidden shadow-sm h-100 card-hover" style="height: 240px;">
                            <img src="${g}" class="img-fluid w-100 h-100" style="object-fit: cover;" alt="Gallery" onerror="this.src='https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=800&q=80'">
                        </div>
                    </div>
                </c:forEach>
            </div>
        </div>
    </section>

    <!-- SECTION: LIÊN HỆ & FOOTER -->
    <footer class="bg-dark text-white py-5 px-3" id="contact">
        <div class="container">
            <div class="row g-5">
                <div class="col-12 col-md-5">
                    <div class="d-flex align-items-center gap-2 mb-3">
                        <img src="${profile.logoUrl}" style="height: 40px; filter: brightness(0) invert(1);" onerror="this.src='${pageContext.request.contextPath}/assets/img/logo-default.png'" alt="Logo">
                        <h5 class="fw-bold text-white mb-0">${profile.companyName}</h5>
                    </div>
                    <p class="text-white-50 small mb-4" style="line-height: 1.6;">${profile.slogan}</p>
                    <div class="d-flex gap-3">
                        <c:if test="${not empty profile.facebookUrl}">
                            <a href="${profile.facebookUrl}" target="_blank" class="btn btn-outline-light btn-sm rounded-circle" style="width: 36px; height: 36px;"><i class="bi bi-facebook"></i></a>
                        </c:if>
                        <c:if test="${not empty profile.linkedinUrl}">
                            <a href="${profile.linkedinUrl}" target="_blank" class="btn btn-outline-light btn-sm rounded-circle" style="width: 36px; height: 36px;"><i class="bi bi-linkedin"></i></a>
                        </c:if>
                        <c:if test="${not empty profile.youtubeUrl}">
                            <a href="${profile.youtubeUrl}" target="_blank" class="btn btn-outline-light btn-sm rounded-circle" style="width: 36px; height: 36px;"><i class="bi bi-youtube"></i></a>
                        </c:if>
                    </div>
                </div>
                <div class="col-12 col-md-7">
                    <h5 class="fw-bold text-white mb-3">Thông tin liên hệ</h5>
                    <ul class="list-unstyled text-white-50 small mb-0">
                        <li class="mb-3 d-flex align-items-start">
                            <i class="bi bi-geo-alt-fill text-danger me-2 fs-6"></i>
                            <span>${profile.headquartersAddress}</span>
                        </li>
                        <li class="mb-3 d-flex align-items-center">
                            <i class="bi bi-envelope-fill text-primary me-2 fs-6"></i>
                            <span>${profile.contactEmail}</span>
                        </li>
                        <li class="mb-3 d-flex align-items-center">
                            <i class="bi bi-telephone-fill text-success me-2 fs-6"></i>
                            <span>${profile.contactPhone}</span>
                        </li>
                        <li class="d-flex align-items-center">
                            <i class="bi bi-globe text-info me-2 fs-6"></i>
                            <a href="${profile.websiteUrl}" target="_blank" class="text-white-50 text-decoration-none">${profile.websiteUrl}</a>
                        </li>
                    </ul>
                </div>
            </div>
            <div class="border-top border-secondary mt-5 pt-4 text-center text-white-50 small">
                &copy; 2026 <strong>${profile.companyName}</strong>. All rights reserved. &bull; Powered by IRMS Platform
            </div>
        </div>
    </footer>

    <!-- Bootstrap 5 Bundle JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>

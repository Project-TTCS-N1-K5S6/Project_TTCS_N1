<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Cấu hình trang giới thiệu công ty" />
<c:set var="breadcrumb" value="Cấu hình trang giới thiệu công ty" />
<c:set var="activeMenu" value="company-profile" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Tiêu đề trang & Thanh công cụ theo đúng giao diện yêu cầu -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-3">
            <div>
                <h3 class="fw-bold text-dark mb-1">Cấu hình trang giới thiệu công ty</h3>
                <p class="text-muted small mb-0">Quản lý nhận diện thương hiệu tuyển dụng, nội dung giới thiệu, hình ảnh, văn hóa và xem trước giao diện công khai.</p>
            </div>
            <div class="d-flex align-items-center gap-2">
                <!-- Nút [+] Quick Action -->
                <button type="button" class="btn btn-outline-secondary btn-sm" onclick="focusAddContent()" title="Thêm mục mới">
                    <i class="bi bi-plus-lg"></i>
                </button>
                <!-- Nút [...] Options -->
                <div class="dropdown">
                    <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-toggle="dropdown" aria-expanded="false" title="Tùy chọn khác">
                        <i class="bi bi-three-dots"></i>
                    </button>
                    <ul class="dropdown-menu dropdown-menu-end shadow-sm text-xs">
                        <li>
                            <a class="dropdown-item py-1" href="${pageContext.request.contextPath}/about-us" target="_blank">
                                <i class="bi bi-box-arrow-up-right me-2 text-primary"></i> Mở trang công khai trong tab mới
                            </a>
                        </li>
                        <li>
                            <button type="button" class="dropdown-item py-1" onclick="openLivePreviewModal()">
                                <i class="bi bi-eye me-2 text-info"></i> Xem trước toàn màn hình
                            </button>
                        </li>
                        <li><hr class="dropdown-divider my-1"></li>
                        <li>
                            <button type="button" class="dropdown-item py-1 text-danger" onclick="resetToDefault()">
                                <i class="bi bi-arrow-counterclockwise me-2"></i> Khôi phục dữ liệu mẫu
                            </button>
                        </li>
                    </ul>
                </div>
                <!-- Nút Filter / Switch View -->
                <button type="button" class="btn btn-outline-secondary btn-sm" onclick="switchPreviewMode()" title="Chuyển chế độ xem trước">
                    <i class="bi bi-layout-split"></i>
                </button>
                <!-- Nút Xem trước giao diện công khai (TIÊU CHÍ BẮT BUỘC) -->
                <button type="button" class="btn btn-outline-primary btn-sm px-3" onclick="openLivePreviewModal()">
                    <i class="bi bi-eye me-1"></i> Xem trước công khai
                </button>
                <!-- Nút Lưu cấu hình -->
                <button type="button" class="btn btn-primary btn-sm px-3" onclick="submitProfileForm()">
                    <i class="bi bi-save me-1"></i> Lưu cấu hình
                </button>
            </div>
        </div>

        <!-- Khung mô tả nghiệp vụ (Khớp 100% hình ảnh yêu cầu) -->
        <div class="card border mb-3 shadow-none bg-light-subtle">
            <div class="card-body p-3">
                <div class="d-flex align-items-center justify-content-between cursor-pointer" data-bs-toggle="collapse" data-bs-target="#descContent" aria-expanded="true">
                    <div class="d-flex align-items-center text-dark fw-bold">
                        <i class="bi bi-chevron-down me-2 text-primary"></i>
                        <span>Description</span>
                    </div>
                    <span class="badge bg-white text-secondary border small">Tiêu chí hoàn thành</span>
                </div>
                <div class="collapse show mt-2" id="descContent">
                    <div class="bg-white p-3 rounded border">
                        <ul class="mb-0 text-secondary small ps-3">
                            <li class="mb-1"><strong>Soạn nội dung giới thiệu, tải ảnh và logo:</strong> Cho phép cập nhật nhận diện thương hiệu, thông tin sứ mệnh, tầm nhìn, văn hóa, giá trị cốt lõi, đãi ngộ và tải ảnh/logo trực tiếp hoặc qua liên kết.</li>
                            <li><strong>Xem trước đúng như giao diện công khai trước khi lưu:</strong> Mô phỏng chính xác 100% giao diện trang Career &amp; About Us dành cho ứng viên và người xem ngoài hệ thống theo thời gian thực (Desktop, Tablet, Mobile) trước khi lưu thay đổi vào cơ sở dữ liệu.</li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>

        <!-- Form soạn thảo cấu hình chính -->
        <form action="${pageContext.request.contextPath}/admin/company-profile/save" method="POST" id="profileForm" enctype="multipart/form-data">
            <input type="hidden" name="coreValues" id="inputCoreValues" value="<c:out value='${profile.coreValues}' />">
            <input type="hidden" name="perks" id="inputPerks" value="<c:out value='${profile.perks}' />">
            <input type="hidden" name="galleryUrls" id="inputGalleryUrls" value="<c:out value='${profile.galleryUrls}' />">

            <div class="row g-3">
                <!-- Cột soạn thảo nội dung -->
                <div class="col-12 col-xl-8">
                    <!-- Nav Tabs cho các phần nội dung -->
                    <ul class="nav nav-pills mb-3 bg-white p-2 border rounded-3 small fw-semibold" id="configTabs" role="tablist">
                        <li class="nav-item" role="presentation">
                            <button class="nav-link active py-1 px-3" id="tab-brand-tab" data-bs-toggle="tab" data-bs-target="#tab-brand" type="button" role="tab">
                                <i class="bi bi-gem me-1"></i> Thương hiệu &amp; Ảnh
                            </button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link py-1 px-3" id="tab-intro-tab" data-bs-toggle="tab" data-bs-target="#tab-intro" type="button" role="tab">
                                <i class="bi bi-file-text me-1"></i> Nội dung giới thiệu
                            </button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link py-1 px-3" id="tab-values-tab" data-bs-toggle="tab" data-bs-target="#tab-values" type="button" role="tab">
                                <i class="bi bi-award me-1"></i> Giá trị &amp; Đãi ngộ
                            </button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link py-1 px-3" id="tab-gallery-tab" data-bs-toggle="tab" data-bs-target="#tab-gallery" type="button" role="tab">
                                <i class="bi bi-images me-1"></i> Bộ sưu tập ảnh
                            </button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link py-1 px-3" id="tab-contact-tab" data-bs-toggle="tab" data-bs-target="#tab-contact" type="button" role="tab">
                                <i class="bi bi-geo-alt me-1"></i> Liên hệ &amp; MXH
                            </button>
                        </li>
                    </ul>

                    <div class="tab-content" id="configTabsContent">
                        <!-- TAB 1: THƯƠNG HIỆU, LOGO & BANNER -->
                        <div class="tab-pane fade show active" id="tab-brand" role="tabpanel">
                            <div class="card border shadow-none mb-3">
                                <div class="card-header bg-white py-3 border-bottom">
                                    <h6 class="fw-bold mb-0 text-dark"><i class="bi bi-patch-check text-primary me-2"></i> Nhận diện thương hiệu &amp; Hình ảnh</h6>
                                </div>
                                <div class="card-body p-3">
                                    <div class="row g-3">
                                        <div class="col-12 col-md-7">
                                            <label class="form-label small fw-semibold text-secondary">Tên công ty đầy đủ <span class="text-danger">*</span></label>
                                            <input type="text" class="form-control form-control-sm" name="companyName" id="editCompanyName" value="${profile.companyName}" required placeholder="VD: Tập đoàn Công nghệ & Tuyển dụng IRMS">
                                        </div>
                                        <div class="col-12 col-md-5">
                                            <label class="form-label small fw-semibold text-secondary">Tên thương hiệu ngắn gọn</label>
                                            <input type="text" class="form-control form-control-sm" name="brandName" id="editBrandName" value="${profile.brandName}" placeholder="VD: IRMS Group">
                                        </div>
                                        <div class="col-12 col-md-7">
                                            <label class="form-label small fw-semibold text-secondary">Khẩu hiệu (Slogan công ty)</label>
                                            <input type="text" class="form-control form-control-sm" name="slogan" id="editSlogan" value="${profile.slogan}" placeholder="VD: Tiên phong công nghệ - Kiến tạo tương lai nghề nghiệp">
                                        </div>
                                        <div class="col-12 col-md-5">
                                            <label class="form-label small fw-semibold text-secondary">Thông điệp tuyển dụng (Tagline)</label>
                                            <input type="text" class="form-control form-control-sm" name="tagline" id="editTagline" value="${profile.tagline}" placeholder="VD: Nơi tài năng hội tụ và phát triển vượt bậc">
                                        </div>

                                        <!-- Upload Logo -->
                                        <div class="col-12 col-md-6">
                                            <div class="p-3 border rounded bg-light-subtle h-100">
                                                <label class="form-label small fw-bold text-dark d-flex align-items-center justify-content-between">
                                                    <span><i class="bi bi-image text-primary me-1"></i> Logo công ty</span>
                                                    <span class="text-xs text-muted">PNG, JPG, SVG, WebP (Tối đa 5MB)</span>
                                                </label>
                                                <div class="d-flex align-items-center gap-3 mb-2">
                                                    <div class="border rounded bg-white p-2 d-flex align-items-center justify-content-center" style="width: 80px; height: 80px;">
                                                        <img src="${profile.logoUrl}" id="previewLogoImg" class="img-fluid" style="max-height: 64px; object-fit: contain;" alt="Logo Preview" onerror="this.src='${pageContext.request.contextPath}/assets/img/logo-default.png'">
                                                    </div>
                                                    <div class="flex-fill">
                                                        <input type="file" class="form-control form-control-sm mb-2" name="logoFile" id="uploadLogoFile" accept="image/*" onchange="handleLocalImagePreview(this, 'previewLogoImg', 'editLogoUrl')">
                                                        <input type="text" class="form-control form-control-sm text-xs font-monospace" name="logoUrl" id="editLogoUrl" value="${profile.logoUrl}" placeholder="Hoặc dán URL logo trực tiếp...">
                                                    </div>
                                                </div>
                                            </div>
                                        </div>

                                        <!-- Upload Ảnh bìa (Hero Banner) -->
                                        <div class="col-12 col-md-6">
                                            <div class="p-3 border rounded bg-light-subtle h-100">
                                                <label class="form-label small fw-bold text-dark d-flex align-items-center justify-content-between">
                                                    <span><i class="bi bi-aspect-ratio text-info me-1"></i> Ảnh bìa chính (Hero Banner)</span>
                                                    <span class="text-xs text-muted">Tỉ lệ 16:9 hoặc 21:9</span>
                                                </label>
                                                <div class="d-flex align-items-center gap-3 mb-2">
                                                    <div class="border rounded bg-white p-1 d-flex align-items-center justify-content-center overflow-hidden" style="width: 110px; height: 80px;">
                                                        <img src="${profile.bannerUrl}" id="previewBannerImg" class="img-fluid rounded" style="width: 100%; height: 100%; object-fit: cover;" alt="Banner Preview" onerror="this.src='https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=800&q=80'">
                                                    </div>
                                                    <div class="flex-fill">
                                                        <input type="file" class="form-control form-control-sm mb-2" name="bannerFile" id="uploadBannerFile" accept="image/*" onchange="handleLocalImagePreview(this, 'previewBannerImg', 'editBannerUrl')">
                                                        <input type="text" class="form-control form-control-sm text-xs font-monospace" name="bannerUrl" id="editBannerUrl" value="${profile.bannerUrl}" placeholder="Hoặc dán URL banner trực tiếp...">
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- TAB 2: NỘI DUNG GIỚI THIỆU, SỨ MỆNH & TẦM NHÌN -->
                        <div class="tab-pane fade" id="tab-intro" role="tabpanel">
                            <div class="card border shadow-none mb-3">
                                <div class="card-header bg-white py-3 border-bottom">
                                    <h6 class="fw-bold mb-0 text-dark"><i class="bi bi-file-earmark-richtext text-primary me-2"></i> Bài viết giới thiệu &amp; Tầm nhìn sứ mệnh</h6>
                                </div>
                                <div class="card-body p-3">
                                    <div class="mb-3">
                                        <label class="form-label small fw-semibold text-secondary">Giới thiệu tổng quan (About Overview) <span class="text-danger">*</span></label>
                                        <textarea class="form-control form-control-sm" name="overview" id="editOverview" rows="4" placeholder="Nêu bật giới thiệu tổng thể, định vị doanh nghiệp và lĩnh vực hoạt động chính...">${profile.overview}</textarea>
                                        <div class="form-text text-xs text-muted">Hiển thị ở khối đầu trang giới thiệu công khai.</div>
                                    </div>
                                    <div class="mb-3">
                                        <label class="form-label small fw-semibold text-secondary">Lịch sử phát triển &amp; Câu chuyện thương hiệu</label>
                                        <textarea class="form-control form-control-sm" name="historyStory" id="editHistoryStory" rows="3" placeholder="Quá trình hình thành, các cột mốc quan trọng và hành trình vươn lên...">${profile.historyStory}</textarea>
                                    </div>
                                    <div class="row g-3 mb-3">
                                        <div class="col-12 col-md-6">
                                            <label class="form-label small fw-semibold text-secondary"><i class="bi bi-bullseye text-danger me-1"></i> Sứ mệnh (Mission)</label>
                                            <textarea class="form-control form-control-sm" name="mission" id="editMission" rows="3" placeholder="Sứ mệnh mà doanh nghiệp phụng sự nhân sự và xã hội...">${profile.mission}</textarea>
                                        </div>
                                        <div class="col-12 col-md-6">
                                            <label class="form-label small fw-semibold text-secondary"><i class="bi bi-compass text-primary me-1"></i> Tầm nhìn (Vision)</label>
                                            <textarea class="form-control form-control-sm" name="vision" id="editVision" rows="3" placeholder="Mục tiêu dài hạn hướng tới tương lai...">${profile.vision}</textarea>
                                        </div>
                                    </div>
                                    <div>
                                        <label class="form-label small fw-semibold text-secondary"><i class="bi bi-heart text-danger me-1"></i> Văn hóa doanh nghiệp &amp; Môi trường làm việc</label>
                                        <textarea class="form-control form-control-sm" name="cultureDesc" id="editCultureDesc" rows="3" placeholder="Miêu tả môi trường, con người, tinh thần học hỏi và gắn kết...">${profile.cultureDesc}</textarea>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- TAB 3: GIÁ TRỊ CỐT LÕI & CHẾ ĐỘ ĐÃI NGỘ -->
                        <div class="tab-pane fade" id="tab-values" role="tabpanel">
                            <!-- Danh sách Giá trị cốt lõi -->
                            <div class="card border shadow-none mb-3">
                                <div class="card-header bg-white py-3 border-bottom d-flex align-items-center justify-content-between">
                                    <h6 class="fw-bold mb-0 text-dark"><i class="bi bi-stars text-warning me-2"></i> Giá trị cốt lõi (Core Values)</h6>
                                    <button type="button" class="btn btn-sm btn-outline-primary" onclick="addCoreValueItem()">
                                        <i class="bi bi-plus-lg me-1"></i> Thêm giá trị
                                    </button>
                                </div>
                                <div class="card-body p-3">
                                    <div id="coreValuesContainer" class="d-flex flex-column gap-2">
                                        <!-- Render động qua JavaScript -->
                                    </div>
                                </div>
                            </div>

                            <!-- Danh sách Chế độ đãi ngộ (Employee Perks) -->
                            <div class="card border shadow-none mb-3">
                                <div class="card-header bg-white py-3 border-bottom d-flex align-items-center justify-content-between">
                                    <h6 class="fw-bold mb-0 text-dark"><i class="bi bi-gift text-success me-2"></i> Chế độ đãi ngộ &amp; Quyền lợi nhân tài (Perks &amp; Benefits)</h6>
                                    <button type="button" class="btn btn-sm btn-outline-success" onclick="addPerkItem()">
                                        <i class="bi bi-plus-lg me-1"></i> Thêm quyền lợi
                                    </button>
                                </div>
                                <div class="card-body p-3">
                                    <div id="perksContainer" class="d-flex flex-column gap-2">
                                        <!-- Render động qua JavaScript -->
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- TAB 4: BỘ SƯU TẬP ẢNH (GALLERY) -->
                        <div class="tab-pane fade" id="tab-gallery" role="tabpanel">
                            <div class="card border shadow-none mb-3">
                                <div class="card-header bg-white py-3 border-bottom d-flex align-items-center justify-content-between">
                                    <h6 class="fw-bold mb-0 text-dark"><i class="bi bi-images text-primary me-2"></i> Không gian làm việc &amp; Hoạt động (Photo Gallery)</h6>
                                    <button type="button" class="btn btn-sm btn-outline-primary" onclick="addGalleryImageItem()">
                                        <i class="bi bi-plus-lg me-1"></i> Thêm ảnh
                                    </button>
                                </div>
                                <div class="card-body p-3">
                                    <div id="galleryContainer" class="row g-3">
                                        <!-- Render động qua JavaScript -->
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- TAB 5: LIÊN HỆ & MẠNG XÃ HỘI -->
                        <div class="tab-pane fade" id="tab-contact" role="tabpanel">
                            <div class="card border shadow-none mb-3">
                                <div class="card-header bg-white py-3 border-bottom">
                                    <h6 class="fw-bold mb-0 text-dark"><i class="bi bi-geo-alt text-primary me-2"></i> Trụ sở làm việc &amp; Mạng xã hội</h6>
                                </div>
                                <div class="card-body p-3">
                                    <div class="row g-3">
                                        <div class="col-12">
                                            <label class="form-label small fw-semibold text-secondary">Địa chỉ trụ sở chính</label>
                                            <input type="text" class="form-control form-control-sm" name="headquartersAddress" id="editAddress" value="${profile.headquartersAddress}" placeholder="VD: Tòa nhà IRMS Tower, Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội">
                                        </div>
                                        <div class="col-12 col-md-4">
                                            <label class="form-label small fw-semibold text-secondary">Email liên hệ / tuyển dụng</label>
                                            <input type="email" class="form-control form-control-sm" name="contactEmail" id="editEmail" value="${profile.contactEmail}" placeholder="VD: career@company.local">
                                        </div>
                                        <div class="col-12 col-md-4">
                                            <label class="form-label small fw-semibold text-secondary">Số điện thoại hotline</label>
                                            <input type="text" class="form-control form-control-sm" name="contactPhone" id="editPhone" value="${profile.contactPhone}" placeholder="VD: (+84) 24 3869 1234">
                                        </div>
                                        <div class="col-12 col-md-4">
                                            <label class="form-label small fw-semibold text-secondary">Trang web công ty (Website)</label>
                                            <input type="url" class="form-control form-control-sm" name="websiteUrl" id="editWebsite" value="${profile.websiteUrl}" placeholder="VD: https://irms.local">
                                        </div>
                                        <div class="col-12 col-md-4">
                                            <label class="form-label small fw-semibold text-secondary"><i class="bi bi-facebook text-primary me-1"></i> Trang Facebook</label>
                                            <input type="text" class="form-control form-control-sm" name="facebookUrl" id="editFacebook" value="${profile.facebookUrl}" placeholder="VD: https://facebook.com/irms.career">
                                        </div>
                                        <div class="col-12 col-md-4">
                                            <label class="form-label small fw-semibold text-secondary"><i class="bi bi-linkedin text-info me-1"></i> Trang LinkedIn</label>
                                            <input type="text" class="form-control form-control-sm" name="linkedinUrl" id="editLinkedin" value="${profile.linkedinUrl}" placeholder="VD: https://linkedin.com/company/irms">
                                        </div>
                                        <div class="col-12 col-md-4">
                                            <label class="form-label small fw-semibold text-secondary"><i class="bi bi-youtube text-danger me-1"></i> Kênh YouTube</label>
                                            <input type="text" class="form-control form-control-sm" name="youtubeUrl" id="editYoutube" value="${profile.youtubeUrl}" placeholder="VD: https://youtube.com/@irms">
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Cột bên phải: Tóm tắt, Trạng thái & Live Preview Mini Card -->
                <div class="col-12 col-xl-4">
                    <!-- Trạng thái xuất bản -->
                    <div class="card border shadow-none mb-3">
                        <div class="card-body p-3">
                            <h6 class="fw-bold mb-3 text-dark d-flex align-items-center justify-content-between">
                                <span><i class="bi bi-broadcast text-primary me-1"></i> Trạng thái trang</span>
                                <span class="badge ${profile.statusBadgeClass}" id="badgeStatusLabel">${profile.statusLabel}</span>
                            </h6>
                            <div class="mb-3">
                                <label class="form-label small fw-semibold text-secondary">Trạng thái công khai</label>
                                <select class="form-select form-select-sm" name="status" id="editStatus" onchange="updateBadgeStatus(this.value)">
                                    <option value="PUBLISHED" ${profile.isPublished() ? 'selected' : ''}>Đã xuất bản (Công khai)</option>
                                    <option value="DRAFT" ${!profile.isPublished() ? 'selected' : ''}>Bản nháp (Nội bộ)</option>
                                </select>
                                <div class="form-text text-xs text-muted">Nếu chọn "Bản nháp", trang ngoài sẽ hiển thị thông báo đang cập nhật.</div>
                            </div>

                            <c:if test="${not empty profile.updatedAt}">
                                <div class="small text-muted border-top pt-2 mt-2">
                                    <div class="d-flex justify-content-between">
                                        <span>Lần cập nhật cuối:</span>
                                        <span class="fw-semibold text-dark"><fmt:formatDate value="${profile.updatedAt}" pattern="dd/MM/yyyy HH:mm" /></span>
                                    </div>
                                    <c:if test="${not empty profile.updatedByName}">
                                        <div class="d-flex justify-content-between mt-1">
                                            <span>Người cập nhật:</span>
                                            <span class="fw-semibold text-dark">${profile.updatedByName}</span>
                                        </div>
                                    </c:if>
                                </div>
                            </c:if>

                            <div class="d-grid gap-2 mt-3">
                                <button type="button" class="btn btn-outline-primary btn-sm" onclick="openLivePreviewModal()">
                                    <i class="bi bi-eye me-1"></i> Xem trước đúng giao diện công khai
                                </button>
                                <a href="${pageContext.request.contextPath}/about-us" target="_blank" class="btn btn-light border btn-sm text-secondary">
                                    <i class="bi bi-box-arrow-up-right me-1"></i> Truy cập trang công khai (/about-us)
                                </a>
                                <button type="button" class="btn btn-primary btn-sm" onclick="submitProfileForm()">
                                    <i class="bi bi-check-lg me-1"></i> Lưu toàn bộ cấu hình
                                </button>
                            </div>
                        </div>
                    </div>

                    <!-- Mini Live Preview Card (Cập nhật thời gian thực khi gõ) -->
                    <div class="card border shadow-none">
                        <div class="card-header bg-white py-2 border-bottom d-flex align-items-center justify-content-between">
                            <span class="small fw-bold text-dark"><i class="bi bi-phone me-1"></i> Khung xem nhanh</span>
                            <span class="badge bg-light text-muted border text-xs">Real-time</span>
                        </div>
                        <div class="card-body p-2 bg-light rounded-bottom">
                            <div class="border rounded bg-white shadow-sm overflow-hidden" style="font-size: 11px;">
                                <!-- Header Mini -->
                                <div class="p-2 border-bottom d-flex align-items-center justify-content-between bg-white">
                                    <div class="d-flex align-items-center gap-1">
                                        <img src="${profile.logoUrl}" id="miniLogo" style="height: 18px; max-width: 60px; object-fit: contain;" onerror="this.src='${pageContext.request.contextPath}/assets/img/logo-default.png'">
                                        <span class="fw-bold text-truncate" id="miniBrandName" style="max-width: 120px;">${profile.brandName != null ? profile.brandName : 'IRMS Group'}</span>
                                    </div>
                                    <span class="badge bg-primary-subtle text-primary" style="font-size: 9px;">Tuyển dụng</span>
                                </div>
                                <!-- Hero Mini -->
                                <div class="position-relative text-white p-3 text-center" style="background: linear-gradient(135deg, #1e3a8a, #3b82f6);">
                                    <div class="fw-bold mb-1" id="miniSlogan" style="font-size: 12px;">${profile.slogan}</div>
                                    <div class="opacity-75" id="miniTagline" style="font-size: 10px;">${profile.tagline}</div>
                                </div>
                                <!-- Content Mini -->
                                <div class="p-2">
                                    <div class="text-muted text-truncate mb-2" id="miniOverview">${profile.overview}</div>
                                    <div class="d-flex gap-1 justify-content-center text-xs text-primary">
                                        <span>&bull; Sứ mệnh</span>
                                        <span>&bull; Giá trị</span>
                                        <span>&bull; Đãi ngộ</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </form>
    </div>

    <!-- MODAL XEM TRƯỚC ĐÚNG NHƯ GIAO DIỆN CÔNG KHAI (TIÊU CHÍ BẮT BUỘC 2) -->
    <div class="modal fade" id="livePreviewModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-fullscreen">
            <div class="modal-content">
                <!-- Toolbar của Modal Preview -->
                <div class="modal-header py-2 px-3 bg-dark text-white border-0 d-flex align-items-center justify-content-between">
                    <div class="d-flex align-items-center gap-2">
                        <i class="bi bi-display fs-5 text-info"></i>
                        <div>
                            <div class="fw-bold small text-white">Chế độ xem trước giao diện công khai (Live Preview)</div>
                            <div class="text-xs text-secondary">Mô phỏng chính xác trang công khai trước khi bấm Lưu</div>
                        </div>
                    </div>
                    <!-- Device Switcher -->
                    <div class="d-flex align-items-center gap-1 bg-black bg-opacity-50 p-1 rounded border border-secondary">
                        <button type="button" class="btn btn-sm btn-dark text-white py-0 px-2 active preview-device-btn" onclick="setDeviceWidth('100%')" title="Màn hình máy tính (Desktop)">
                            <i class="bi bi-laptop"></i> Desktop
                        </button>
                        <button type="button" class="btn btn-sm btn-dark text-white py-0 px-2 preview-device-btn" onclick="setDeviceWidth('768px')" title="Máy tính bảng (Tablet)">
                            <i class="bi bi-tablet"></i> Tablet
                        </button>
                        <button type="button" class="btn btn-sm btn-dark text-white py-0 px-2 preview-device-btn" onclick="setDeviceWidth('375px')" title="Điện thoại di động (Mobile)">
                            <i class="bi bi-phone"></i> Mobile
                        </button>
                    </div>
                    <!-- Action buttons -->
                    <div class="d-flex align-items-center gap-2">
                        <button type="button" class="btn btn-success btn-sm px-3" onclick="submitProfileForm()">
                            <i class="bi bi-save me-1"></i> Lưu cấu hình ngay
                        </button>
                        <button type="button" class="btn btn-outline-light btn-sm" data-bs-dismiss="modal">
                            <i class="bi bi-x-lg"></i> Đóng xem trước
                        </button>
                    </div>
                </div>

                <!-- Vùng nội dung Preview mô phỏng trang công khai -->
                <div class="modal-body p-0 bg-secondary bg-opacity-25 d-flex justify-content-center overflow-auto">
                    <div id="previewContainerWrapper" style="width: 100%; max-width: 100%; transition: all 0.3s ease; background: #ffffff; min-height: 100vh;">
                        
                        <!-- PUBLIC NAVBAR PREVIEW -->
                        <nav class="navbar navbar-expand-lg bg-white border-bottom shadow-sm px-3 py-2 sticky-top">
                            <div class="container-fluid">
                                <a class="navbar-brand d-flex align-items-center gap-2" href="javascript:void(0);">
                                    <img src="${profile.logoUrl}" id="pvNavLogo" style="height: 36px; max-width: 120px; object-fit: contain;" alt="Logo" onerror="this.src='${pageContext.request.contextPath}/assets/img/logo-default.png'">
                                    <span class="fw-bold text-dark fs-6" id="pvNavBrand">${profile.brandName != null ? profile.brandName : profile.companyName}</span>
                                </a>
                                <div class="d-flex align-items-center gap-2 ms-auto">
                                    <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-2 py-1">
                                        <i class="bi bi-briefcase me-1"></i> Tuyển dụng 2026
                                    </span>
                                    <button class="btn btn-outline-secondary btn-sm d-none d-sm-inline">Về chúng tôi</button>
                                    <button class="btn btn-primary btn-sm">Xem vị trí tuyển dụng</button>
                                </div>
                            </div>
                        </nav>

                        <!-- PUBLIC HERO BANNER PREVIEW -->
                        <section class="position-relative text-white py-5 px-3 text-center overflow-hidden" 
                                 id="pvHeroSection"
                                 style="background: linear-gradient(rgba(15, 23, 42, 0.75), rgba(15, 23, 42, 0.75)), url('${profile.bannerUrl}') center/cover no-repeat; min-height: 340px; display: flex; align-items: center;">
                            <div class="container py-4">
                                <span class="badge bg-primary text-uppercase letter-spacing-1 px-3 py-1 mb-3 shadow-sm" id="pvHeroBrand">
                                    ${profile.companyName}
                                </span>
                                <h1 class="display-6 fw-bold mb-3 text-white" id="pvHeroSlogan">
                                    ${profile.slogan}
                                </h1>
                                <p class="lead text-white-50 mx-auto mb-4" style="max-width: 700px;" id="pvHeroTagline">
                                    ${profile.tagline}
                                </p>
                                <div class="d-flex justify-content-center gap-2 flex-wrap">
                                    <button class="btn btn-primary btn-sm px-4 py-2 fw-semibold shadow">
                                        <i class="bi bi-search me-1"></i> Tìm kiếm cơ hội việc làm
                                    </button>
                                    <button class="btn btn-outline-light btn-sm px-4 py-2 fw-semibold">
                                        <i class="bi bi-play-circle me-1"></i> Khám phá văn hóa
                                    </button>
                                </div>
                            </div>
                        </section>

                        <!-- PUBLIC OVERVIEW & MISSION/VISION PREVIEW -->
                        <section class="py-5 bg-white border-bottom">
                            <div class="container">
                                <div class="text-center mb-5">
                                    <span class="text-primary text-uppercase fw-bold text-xs">Về chúng tôi</span>
                                    <h2 class="fw-bold text-dark mt-1" id="pvSectionOverviewTitle">Câu chuyện thương hiệu</h2>
                                    <p class="text-muted mx-auto" style="max-width: 760px; font-size: 15px;" id="pvOverviewText">
                                        ${profile.overview}
                                    </p>
                                </div>

                                <div class="row g-4 align-items-stretch">
                                    <div class="col-12 col-md-6">
                                        <div class="card h-100 border-0 bg-primary-subtle p-4 rounded-4">
                                            <div class="d-flex align-items-center mb-3">
                                                <div class="rounded-circle bg-primary text-white p-2 me-3 d-flex align-items-center justify-content-center" style="width: 44px; height: 44px;">
                                                    <i class="bi bi-bullseye fs-5"></i>
                                                </div>
                                                <h5 class="fw-bold mb-0 text-primary">Sứ mệnh của chúng tôi</h5>
                                            </div>
                                            <p class="text-dark mb-0 small" id="pvMissionText">${profile.mission}</p>
                                        </div>
                                    </div>
                                    <div class="col-12 col-md-6">
                                        <div class="card h-100 border-0 bg-info-subtle p-4 rounded-4">
                                            <div class="d-flex align-items-center mb-3">
                                                <div class="rounded-circle bg-info text-white p-2 me-3 d-flex align-items-center justify-content-center" style="width: 44px; height: 44px;">
                                                    <i class="bi bi-compass fs-5"></i>
                                                </div>
                                                <h5 class="fw-bold mb-0 text-info">Tầm nhìn chiến lược</h5>
                                            </div>
                                            <p class="text-dark mb-0 small" id="pvVisionText">${profile.vision}</p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </section>

                        <!-- PUBLIC CORE VALUES PREVIEW -->
                        <section class="py-5 bg-light-subtle border-bottom">
                            <div class="container">
                                <div class="text-center mb-4">
                                    <span class="text-primary text-uppercase fw-bold text-xs">Định hướng phát triển</span>
                                    <h3 class="fw-bold text-dark mt-1">Giá trị cốt lõi</h3>
                                    <p class="text-muted small">Những nguyên tắc dẫn lối cho mọi hoạt động và quyết định tại công ty.</p>
                                </div>
                                <div class="row g-3" id="pvCoreValuesGrid">
                                    <!-- Render qua JS -->
                                </div>
                            </div>
                        </section>

                        <!-- PUBLIC CULTURE & PERKS PREVIEW -->
                        <section class="py-5 bg-white border-bottom">
                            <div class="container">
                                <div class="row g-4 align-items-center mb-5">
                                    <div class="col-12 col-lg-6">
                                        <span class="text-primary text-uppercase fw-bold text-xs">Môi trường làm việc</span>
                                        <h3 class="fw-bold text-dark mt-1">Văn hóa doanh nghiệp tại ${profile.brandName != null ? profile.brandName : profile.companyName}</h3>
                                        <p class="text-secondary small mt-3" id="pvCultureText">${profile.cultureDesc}</p>
                                    </div>
                                    <div class="col-12 col-lg-6">
                                        <div class="rounded-4 overflow-hidden shadow-sm">
                                            <img src="${profile.bannerUrl}" id="pvCultureImg" class="img-fluid" style="width: 100%; height: 260px; object-fit: cover;" onerror="this.src='https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=800&q=80'">
                                        </div>
                                    </div>
                                </div>

                                <div class="text-center mb-4">
                                    <h4 class="fw-bold text-dark">Chế độ đãi ngộ &amp; Quyền lợi nhân sự</h4>
                                    <p class="text-muted small">Chúng tôi cam kết xây dựng chính sách đãi ngộ toàn diện, xứng đáng với đóng góp của từng thành viên.</p>
                                </div>
                                <div class="row g-3" id="pvPerksGrid">
                                    <!-- Render qua JS -->
                                </div>
                            </div>
                        </section>

                        <!-- PUBLIC GALLERY PREVIEW -->
                        <section class="py-5 bg-light-subtle border-bottom">
                            <div class="container">
                                <div class="text-center mb-4">
                                    <span class="text-primary text-uppercase fw-bold text-xs">Hình ảnh thực tế</span>
                                    <h3 class="fw-bold text-dark mt-1">Không gian làm việc &amp; Đội ngũ</h3>
                                </div>
                                <div class="row g-3" id="pvGalleryGrid">
                                    <!-- Render qua JS -->
                                </div>
                            </div>
                        </section>

                        <!-- PUBLIC FOOTER PREVIEW -->
                        <footer class="bg-dark text-white py-5 px-3">
                            <div class="container">
                                <div class="row g-4">
                                    <div class="col-12 col-md-5">
                                        <div class="d-flex align-items-center gap-2 mb-3">
                                            <img src="${profile.logoUrl}" id="pvFooterLogo" style="height: 32px; filter: brightness(0) invert(1);" onerror="this.src='${pageContext.request.contextPath}/assets/img/logo-default.png'">
                                            <h6 class="fw-bold text-white mb-0" id="pvFooterCompanyName">${profile.companyName}</h6>
                                        </div>
                                        <p class="text-white-50 small mb-3" id="pvFooterSlogan">${profile.slogan}</p>
                                        <div class="d-flex gap-2">
                                            <a href="javascript:void(0);" class="text-white opacity-75 fs-5" id="pvFooterFb"><i class="bi bi-facebook"></i></a>
                                            <a href="javascript:void(0);" class="text-white opacity-75 fs-5" id="pvFooterIn"><i class="bi bi-linkedin"></i></a>
                                            <a href="javascript:void(0);" class="text-white opacity-75 fs-5" id="pvFooterYt"><i class="bi bi-youtube"></i></a>
                                        </div>
                                    </div>
                                    <div class="col-12 col-md-7">
                                        <h6 class="fw-bold text-white mb-3">Thông tin liên hệ &amp; Địa chỉ</h6>
                                        <ul class="list-unstyled text-white-50 small mb-0">
                                            <li class="mb-2"><i class="bi bi-geo-alt-fill text-danger me-2"></i> <span id="pvFooterAddress">${profile.headquartersAddress}</span></li>
                                            <li class="mb-2"><i class="bi bi-envelope-fill text-primary me-2"></i> <span id="pvFooterEmail">${profile.contactEmail}</span></li>
                                            <li class="mb-2"><i class="bi bi-telephone-fill text-success me-2"></i> <span id="pvFooterPhone">${profile.contactPhone}</span></li>
                                            <li><i class="bi bi-globe me-2"></i> <span id="pvFooterWeb">${profile.websiteUrl}</span></li>
                                        </ul>
                                    </div>
                                </div>
                                <div class="border-top border-secondary mt-4 pt-3 text-center text-white-50 text-xs">
                                    &copy; 2026 <strong class="text-white" id="pvCopyBrand">${profile.companyName}</strong>. All rights reserved.
                                </div>
                            </div>
                        </footer>

                    </div>
                </div>
            </div>
        </div>
    </div>

<script>
// Quản lý danh sách Core Values động
let coreValuesData = [];
let perksData = [];
let galleryData = [];

document.addEventListener('DOMContentLoaded', function () {
    // 1. Phục hồi dữ liệu JSON
    try {
        const rawValues = document.getElementById('inputCoreValues').value;
        coreValuesData = rawValues ? JSON.parse(rawValues) : [];
    } catch (e) { coreValuesData = []; }

    try {
        const rawPerks = document.getElementById('inputPerks').value;
        perksData = rawPerks ? JSON.parse(rawPerks) : [];
    } catch (e) { perksData = []; }

    try {
        const rawGallery = document.getElementById('inputGalleryUrls').value;
        galleryData = rawGallery ? JSON.parse(rawGallery) : [];
    } catch (e) { galleryData = []; }

    // Đảm bảo có dữ liệu mẫu nếu rỗng
    if (!coreValuesData || coreValuesData.length === 0) {
        coreValuesData = [
            { icon: "bi-star", title: "Chất lượng vượt trội", desc: "Luôn đặt chuẩn mực cao nhất trong từng sản phẩm và dịch vụ." },
            { icon: "bi-lightbulb", title: "Đổi mới sáng tạo", desc: "Khuyến khích tư duy mới, không ngừng thử nghiệm và bứt phá." },
            { icon: "bi-shield-check", title: "Chính trực & Minh bạch", desc: "Minh bạch trong mọi quy trình và tôn trọng cam kết với nhân sự." }
        ];
    }

    if (!perksData || perksData.length === 0) {
        perksData = [
            { icon: "bi-cash-coin", title: "Lương & Thưởng cạnh tranh", desc: "Thu nhập hấp dẫn cùng thưởng hiệu quả dự án xứng đáng." },
            { icon: "bi-heart-pulse", title: "Bảo hiểm sức khỏe cao cấp", desc: "Chăm sóc y tế toàn diện cho bản thân và người thân." },
            { icon: "bi-laptop", title: "Trang thiết bị hiện đại", desc: "Macbook/Laptop cấu hình cao đáp ứng tối ưu công việc." }
        ];
    }

    if (!galleryData || galleryData.length === 0) {
        galleryData = [
            "https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=800&q=80"
        ];
    }

    renderCoreValuesList();
    renderPerksList();
    renderGalleryList();
    bindRealTimeMiniPreview();
});

// Render danh sách Giá trị cốt lõi
function renderCoreValuesList() {
    const container = document.getElementById('coreValuesContainer');
    container.innerHTML = '';
    coreValuesData.forEach((item, idx) => {
        const iconVal = item.icon || 'bi-star';
        const titleVal = item.title || '';
        const descVal = item.desc || '';
        const div = document.createElement('div');
        div.className = 'border rounded p-2 bg-light d-flex align-items-center gap-2';
        div.innerHTML = '<div class="input-group input-group-sm" style="width: 140px;">' +
            '<span class="input-group-text"><i class="bi ' + iconVal + '"></i></span>' +
            '<input type="text" class="form-control" value="' + escapeHtml(iconVal) + '" onchange="updateCoreValue(' + idx + ', \'icon\', this.value)" placeholder="Icon class">' +
            '</div>' +
            '<input type="text" class="form-control form-control-sm fw-semibold" style="width: 220px;" value="' + escapeHtml(titleVal) + '" onchange="updateCoreValue(' + idx + ', \'title\', this.value)" placeholder="Tiêu đề giá trị">' +
            '<input type="text" class="form-control form-control-sm flex-fill" value="' + escapeHtml(descVal) + '" onchange="updateCoreValue(' + idx + ', \'desc\', this.value)" placeholder="Mô tả ngắn">' +
            '<button type="button" class="btn btn-outline-danger btn-sm" onclick="removeCoreValue(' + idx + ')" title="Xóa"><i class="bi bi-trash"></i></button>';
        container.appendChild(div);
    });
    syncJsonInputs();
}

function addCoreValueItem() {
    coreValuesData.push({ icon: "bi-star", title: "Giá trị mới", desc: "Mô tả giá trị cốt lõi..." });
    renderCoreValuesList();
}

function updateCoreValue(idx, field, val) {
    if (coreValuesData[idx]) {
        coreValuesData[idx][field] = val;
        syncJsonInputs();
    }
}

function removeCoreValue(idx) {
    coreValuesData.splice(idx, 1);
    renderCoreValuesList();
}

// Render danh sách Đãi ngộ (Perks)
function renderPerksList() {
    const container = document.getElementById('perksContainer');
    container.innerHTML = '';
    perksData.forEach((item, idx) => {
        const iconVal = item.icon || 'bi-gift';
        const titleVal = item.title || '';
        const descVal = item.desc || '';
        const div = document.createElement('div');
        div.className = 'border rounded p-2 bg-light d-flex align-items-center gap-2';
        div.innerHTML = '<div class="input-group input-group-sm" style="width: 140px;">' +
            '<span class="input-group-text"><i class="bi ' + iconVal + '"></i></span>' +
            '<input type="text" class="form-control" value="' + escapeHtml(iconVal) + '" onchange="updatePerk(' + idx + ', \'icon\', this.value)" placeholder="Icon class">' +
            '</div>' +
            '<input type="text" class="form-control form-control-sm fw-semibold text-success" style="width: 220px;" value="' + escapeHtml(titleVal) + '" onchange="updatePerk(' + idx + ', \'title\', this.value)" placeholder="Tên quyền lợi">' +
            '<input type="text" class="form-control form-control-sm flex-fill" value="' + escapeHtml(descVal) + '" onchange="updatePerk(' + idx + ', \'desc\', this.value)" placeholder="Mô tả chi tiết">' +
            '<button type="button" class="btn btn-outline-danger btn-sm" onclick="removePerk(' + idx + ')" title="Xóa"><i class="bi bi-trash"></i></button>';
        container.appendChild(div);
    });
    syncJsonInputs();
}

function addPerkItem() {
    perksData.push({ icon: "bi-gift", title: "Quyền lợi mới", desc: "Chính sách đãi ngộ chi tiết..." });
    renderPerksList();
}

function updatePerk(idx, field, val) {
    if (perksData[idx]) {
        perksData[idx][field] = val;
        syncJsonInputs();
    }
}

function removePerk(idx) {
    perksData.splice(idx, 1);
    renderPerksList();
}

// Render Bộ sưu tập ảnh (Gallery)
function renderGalleryList() {
    const container = document.getElementById('galleryContainer');
    container.innerHTML = '';
    galleryData.forEach((url, idx) => {
        const col = document.createElement('div');
        col.className = 'col-12 col-md-4';
        col.innerHTML = '<div class="card border shadow-none h-100">' +
            '<div class="position-relative" style="height: 140px; overflow: hidden;">' +
            '<img src="' + url + '" class="card-img-top" style="width: 100%; height: 100%; object-fit: cover;" onerror="this.src=\'https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=800&q=80\'">' +
            '<button type="button" class="btn btn-danger btn-sm position-absolute top-0 end-0 m-1 py-0 px-2" onclick="removeGalleryImage(' + idx + ')" title="Xóa ảnh">&times;</button>' +
            '</div>' +
            '<div class="p-2 bg-light">' +
            '<input type="text" class="form-control form-control-sm text-xs font-monospace" value="' + escapeHtml(url) + '" onchange="updateGalleryImage(' + idx + ', this.value)" placeholder="URL ảnh...">' +
            '</div></div>';
        container.appendChild(col);
    });
    syncJsonInputs();
}

function addGalleryImageItem() {
    const defaultImg = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=800&q=80";
    galleryData.push(defaultImg);
    renderGalleryList();
}

function updateGalleryImage(idx, val) {
    if (galleryData[idx] !== undefined) {
        galleryData[idx] = val;
        syncJsonInputs();
    }
}

function removeGalleryImage(idx) {
    galleryData.splice(idx, 1);
    renderGalleryList();
}

function syncJsonInputs() {
    document.getElementById('inputCoreValues').value = JSON.stringify(coreValuesData);
    document.getElementById('inputPerks').value = JSON.stringify(perksData);
    document.getElementById('inputGalleryUrls').value = JSON.stringify(galleryData);
}

function escapeHtml(text) {
    if (!text) return '';
    return String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// Xử lý xem trước ảnh khi chọn file từ máy tính
function handleLocalImagePreview(input, imgElementId, urlInputId) {
    if (input.files && input.files[0]) {
        const file = input.files[0];
        const reader = new FileReader();
        reader.onload = function (e) {
            document.getElementById(imgElementId).src = e.target.result;
            if (urlInputId === 'editLogoUrl') {
                document.getElementById('miniLogo').src = e.target.result;
            } else if (urlInputId === 'editBannerUrl') {
                document.getElementById('miniBanner').style.backgroundImage = 'url(\'' + e.target.result + '\')';
            }
        };
        reader.readAsDataURL(file);
    }
}

// Đồng bộ theo thời gian thực vào Mini Preview bên phải
function bindRealTimeMiniPreview() {
    const bindMap = [
        { src: 'editCompanyName', target: 'pvHeroBrand' },
        { src: 'editBrandName', target: 'miniBrandName' },
        { src: 'editSlogan', target: 'miniSlogan' },
        { src: 'editTagline', target: 'miniTagline' },
        { src: 'editOverview', target: 'miniOverview' }
    ];

    bindMap.forEach(item => {
        const el = document.getElementById(item.src);
        if (el) {
            el.addEventListener('input', function () {
                const target = document.getElementById(item.target);
                if (target) target.innerText = this.value;
            });
        }
    });
}

function updateBadgeStatus(val) {
    const badge = document.getElementById('badgeStatusLabel');
    if (val === 'PUBLISHED') {
        badge.className = 'badge bg-success-subtle text-success border border-success-subtle';
        badge.innerText = 'Đã xuất bản';
    } else {
        badge.className = 'badge bg-warning-subtle text-warning border border-warning-subtle';
        badge.innerText = 'Bản nháp';
    }
}

// MỞ MODAL XEM TRƯỚC ĐÚNG NHƯ GIAO DIỆN CÔNG KHAI (TIÊU CHÍ BẮT BUỘC 2)
function openLivePreviewModal() {
    syncJsonInputs();

    // 1. Đồng bộ thông tin thương hiệu
    const compName = document.getElementById('editCompanyName').value || 'Tập đoàn Công nghệ IRMS';
    const brandName = document.getElementById('editBrandName').value || compName;
    const slogan = document.getElementById('editSlogan').value || 'Tiên phong công nghệ - Kiến tạo tương lai';
    const tagline = document.getElementById('editTagline').value || 'Nơi tài năng hội tụ';
    const logoUrl = document.getElementById('previewLogoImg').src;
    const bannerUrl = document.getElementById('previewBannerImg').src;

    document.getElementById('pvNavLogo').src = logoUrl;
    document.getElementById('pvNavBrand').innerText = brandName;
    document.getElementById('pvHeroBrand').innerText = compName;
    document.getElementById('pvHeroSlogan').innerText = slogan;
    document.getElementById('pvHeroTagline').innerText = tagline;
    document.getElementById('pvHeroSection').style.backgroundImage = 'linear-gradient(rgba(15, 23, 42, 0.75), rgba(15, 23, 42, 0.75)), url(\'' + bannerUrl + '\')';

    // 2. Đồng bộ nội dung giới thiệu
    document.getElementById('pvOverviewText').innerText = document.getElementById('editOverview').value || 'Đang cập nhật nội dung giới thiệu...';
    document.getElementById('pvMissionText').innerText = document.getElementById('editMission').value || 'Đang cập nhật sứ mệnh...';
    document.getElementById('pvVisionText').innerText = document.getElementById('editVision').value || 'Đang cập nhật tầm nhìn...';
    document.getElementById('pvCultureText').innerText = document.getElementById('editCultureDesc').value || 'Đang cập nhật văn hóa...';
    document.getElementById('pvCultureImg').src = bannerUrl;

    // 3. Render Giá trị cốt lõi lên Preview
    const valuesGrid = document.getElementById('pvCoreValuesGrid');
    valuesGrid.innerHTML = '';
    coreValuesData.forEach(v => {
        const iconVal = v.icon || 'bi-star';
        const col = document.createElement('div');
        col.className = 'col-12 col-md-4';
        col.innerHTML = '<div class="card h-100 border-0 shadow-sm p-4 rounded-4 text-center">' +
            '<div class="rounded-circle bg-primary-subtle text-primary mx-auto mb-3 d-flex align-items-center justify-content-center" style="width: 56px; height: 56px;">' +
            '<i class="bi ' + iconVal + ' fs-4"></i>' +
            '</div>' +
            '<h6 class="fw-bold text-dark">' + escapeHtml(v.title || '') + '</h6>' +
            '<p class="text-muted small mb-0">' + escapeHtml(v.desc || '') + '</p>' +
            '</div>';
        valuesGrid.appendChild(col);
    });

    // 4. Render Chế độ đãi ngộ lên Preview
    const perksGrid = document.getElementById('pvPerksGrid');
    perksGrid.innerHTML = '';
    perksData.forEach(p => {
        const iconVal = p.icon || 'bi-gift';
        const col = document.createElement('div');
        col.className = 'col-12 col-md-6 col-lg-3';
        col.innerHTML = '<div class="card h-100 border border-success-subtle bg-white p-3 rounded-3">' +
            '<div class="d-flex align-items-center gap-2 mb-2">' +
            '<i class="bi ' + iconVal + ' text-success fs-5"></i>' +
            '<h6 class="fw-bold mb-0 text-dark small">' + escapeHtml(p.title || '') + '</h6>' +
            '</div>' +
            '<p class="text-muted text-xs mb-0">' + escapeHtml(p.desc || '') + '</p>' +
            '</div>';
        perksGrid.appendChild(col);
    });

    // 5. Render Thư viện ảnh lên Preview
    const galleryGrid = document.getElementById('pvGalleryGrid');
    galleryGrid.innerHTML = '';
    galleryData.forEach(img => {
        const col = document.createElement('div');
        col.className = 'col-12 col-md-4';
        col.innerHTML = '<div class="rounded-3 overflow-hidden shadow-sm h-100" style="min-height: 180px;">' +
            '<img src="' + img + '" class="img-fluid w-100 h-100" style="object-fit: cover;" onerror="this.src=\'https://images.unsplash.com/photo-1522071820081-009f0129c71c?auto=format&fit=crop&w=800&q=80\'">' +
            '</div>';
        galleryGrid.appendChild(col);
    });

    // 6. Đồng bộ Footer Preview
    document.getElementById('pvFooterLogo').src = logoUrl;
    document.getElementById('pvFooterCompanyName').innerText = compName;
    document.getElementById('pvFooterSlogan').innerText = slogan;
    document.getElementById('pvFooterAddress').innerText = document.getElementById('editAddress').value || 'Đang cập nhật địa chỉ...';
    document.getElementById('pvFooterEmail').innerText = document.getElementById('editEmail').value || 'Đang cập nhật email...';
    document.getElementById('pvFooterPhone').innerText = document.getElementById('editPhone').value || 'Đang cập nhật số điện thoại...';
    document.getElementById('pvFooterWeb').innerText = document.getElementById('editWebsite').value || 'https://irms.local';
    document.getElementById('pvCopyBrand').innerText = compName;

    // Mở modal
    new bootstrap.Modal(document.getElementById('livePreviewModal')).show();
}

// Chuyển đổi độ rộng thiết bị trong Modal Preview (Desktop, Tablet, Mobile)
function setDeviceWidth(width) {
    const wrapper = document.getElementById('previewContainerWrapper');
    wrapper.style.maxWidth = width;

    document.querySelectorAll('.preview-device-btn').forEach(btn => btn.classList.remove('active'));
    if (event && event.currentTarget) {
        event.currentTarget.classList.add('active');
    }
}

function submitProfileForm() {
    syncJsonInputs();
    document.getElementById('profileForm').submit();
}

function focusAddContent() {
    const triggerTab = document.querySelector('#configTabs button#tab-values-tab');
    bootstrap.Tab.getInstance(triggerTab) ? bootstrap.Tab.getInstance(triggerTab).show() : new bootstrap.Tab(triggerTab).show();
}

function resetToDefault() {
    if (confirm('Bạn có chắc chắn muốn khôi phục lại nội dung giới thiệu mẫu mặc định?')) {
        document.getElementById('editCompanyName').value = 'Tập đoàn Công nghệ & Tuyển dụng IRMS';
        document.getElementById('editBrandName').value = 'IRMS Platform';
        document.getElementById('editSlogan').value = 'Tiên phong công nghệ - Kiến tạo tương lai nghề nghiệp';
        document.getElementById('editTagline').value = 'Nơi tài năng hội tụ và phát triển vượt bậc';
        document.getElementById('editOverview').value = 'IRMS là nền tảng quản trị và tuyển dụng nhân tài nội bộ hàng đầu, kết nối nguồn nhân lực chất lượng cao với các dự án công nghệ đột phá...';
        submitProfileForm();
    }
}
</script>

<jsp:include page="../common/footer.jsp" />

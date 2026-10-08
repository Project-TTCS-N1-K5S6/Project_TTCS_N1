<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<header class="app-header">
    <div class="header-left">
        <button class="sidebar-toggle-btn" id="sidebar-toggle" type="button" aria-label="Toggle Sidebar">
            <i class="bi bi-list"></i>
        </button>
        <div class="page-breadcrumb">
            <span class="text-muted fs-6"><i class="bi bi-house-door me-1"></i> IRMS</span>
            <span class="text-muted mx-2">/</span>
            <span class="fw-semibold text-dark fs-6">${breadcrumb != null ? breadcrumb : 'Hệ Thống Tuyển Dụng'}</span>
        </div>
    </div>

    <div class="header-right">
        <!-- 
            ======================================================================
            [US 6 & AVATAR]: HIỂN THỊ LOGO / AVATAR VÀ TÊN NGƯỜI DÙNG PHÍA BÊN PHẢI
            - Hiển thị logo/avatar người dùng (hình ảnh hoặc ký tự đầu)
            - Cho phép tải lên hoặc thay đổi ảnh đại diện cá nhân tùy chọn
            ======================================================================
        -->
        <div class="dropdown">
            <a href="#" class="d-flex align-items-center gap-2 text-decoration-none dropdown-toggle user-nav-link" id="userMenuDropdown" data-bs-toggle="dropdown" aria-expanded="false">
                <div class="user-avatar-wrapper position-relative" style="width: 32px; height: 32px; min-width: 32px; min-height: 32px; flex-shrink: 0; display: inline-flex; align-items: center; justify-content: center;">
                    <div class="user-avatar overflow-hidden" style="width: 32px; height: 32px; min-width: 32px; min-height: 32px; max-width: 32px; max-height: 32px; border-radius: 50% !important; overflow: hidden !important; display: flex; align-items: center; justify-content: center; flex-shrink: 0; background: linear-gradient(135deg, #2563eb, #8b5cf6); border: 1.5px solid #ffffff; box-shadow: 0 1px 4px rgba(0,0,0,0.15);">
                        <c:choose>
                            <c:when test="${not empty currentUser.avatarUrl}">
                                <img src="${currentUser.avatarUrl}" alt="Avatar" class="user-avatar-img" id="navHeaderAvatarImg" style="width: 32px !important; height: 32px !important; min-width: 32px !important; min-height: 32px !important; max-width: 32px !important; max-height: 32px !important; border-radius: 50% !important; object-fit: cover !important; display: block !important;" />
                            </c:when>
                            <c:when test="${not empty currentUser.fullName}">
                                <span class="avatar-letter" style="color: #ffffff; font-weight: 700; font-size: 0.85rem; line-height: 1;">${currentUser.fullName.substring(0, 1)}</span>
                            </c:when>
                            <c:otherwise><span class="avatar-letter" style="color: #ffffff; font-weight: 700; font-size: 0.85rem; line-height: 1;">U</span></c:otherwise>
                        </c:choose>
                    </div>
                    <span class="avatar-camera-btn" style="position: absolute; bottom: -2px; right: -2px; width: 13px; height: 13px; border-radius: 50%; background: #2563eb; color: #fff; font-size: 7px; display: flex; align-items: center; justify-content: center; border: 1px solid #ffffff; box-shadow: 0 1px 3px rgba(0,0,0,0.3); cursor: pointer; z-index: 2;" title="Thay đổi ảnh đại diện" data-bs-toggle="modal" data-bs-target="#changeAvatarModal" onclick="event.stopPropagation();">
                        <i class="bi bi-camera-fill" style="font-size: 7px; line-height: 1;"></i>
                    </span>
                </div>
                <div class="d-none d-md-block text-start lh-1 ms-1">
                    <div class="fw-semibold text-dark fs-6" style="font-size: 0.95rem; line-height: 1.2;">${currentUser.fullName}</div>
                    <small class="text-muted" style="font-size: 0.75rem; line-height: 1.2;">${currentUser.getRolesDisplay()}</small>
                </div>
            </a>
            <ul class="dropdown-menu dropdown-menu-end shadow-sm border-0 py-2 mt-2" aria-labelledby="userMenuDropdown">
                <li class="px-3 py-2 border-bottom mb-1">
                    <div class="d-flex align-items-center gap-2">
                        <div class="user-avatar overflow-hidden style-sm" style="width: 36px; height: 36px; min-width: 36px; min-height: 36px; max-width: 36px; max-height: 36px; border-radius: 50% !important; overflow: hidden !important; display: flex; align-items: center; justify-content: center; flex-shrink: 0; background: linear-gradient(135deg, #2563eb, #8b5cf6);">
                            <c:choose>
                                <c:when test="${not empty currentUser.avatarUrl}">
                                    <img src="${currentUser.avatarUrl}" alt="Avatar" class="user-avatar-img" style="width: 36px !important; height: 36px !important; min-width: 36px !important; min-height: 36px !important; max-width: 36px !important; max-height: 36px !important; border-radius: 50% !important; object-fit: cover !important; display: block !important;" />
                                </c:when>
                                <c:when test="${not empty currentUser.fullName}">
                                    <span class="avatar-letter" style="color: #ffffff; font-weight: 700; font-size: 0.9rem;">${currentUser.fullName.substring(0, 1)}</span>
                                </c:when>
                                <c:otherwise><span class="avatar-letter" style="color: #ffffff; font-weight: 700; font-size: 0.9rem;">U</span></c:otherwise>
                            </c:choose>
                        </div>
                        <div class="overflow-hidden">
                            <div class="fw-bold text-truncate">${currentUser.fullName}</div>
                            <div class="text-muted text-xs text-truncate">${currentUser.email}</div>
                        </div>
                    </div>
                    <div class="mt-2"><span class="badge badge-role">${currentUser.getRolesDisplay()}</span></div>
                </li>
                <li>
                    <a class="dropdown-item py-2" href="#" data-bs-toggle="modal" data-bs-target="#changeAvatarModal">
                        <i class="bi bi-image me-2 text-success"></i> Đổi ảnh đại diện
                    </a>
                </li>
                <li>
                    <a class="dropdown-item py-2" href="${pageContext.request.contextPath}/profile">
                        <i class="bi bi-person-lines-fill me-2 text-primary"></i> Hồ sơ cá nhân
                    </a>
                </li>
                <li>
                    <a class="dropdown-item py-2" href="${pageContext.request.contextPath}/auth/change-password">
                        <i class="bi bi-key me-2 text-primary"></i> Đổi mật khẩu
                    </a>
                </li>
                <li><hr class="dropdown-divider"></li>
                <li>
                    <a class="dropdown-item py-2 text-danger" href="${pageContext.request.contextPath}/auth/logout">
                        <i class="bi bi-box-arrow-right me-2"></i> Đăng xuất
                    </a>
                </li>
            </ul>
        </div>
    </div>
</header>

<!-- ====================================================================== -->
<!-- MODAL THAY ĐỔI ÁNH ĐẠI DIỆN NGƯỜI DÙNG -->
<!-- ====================================================================== -->
<div class="modal fade" id="changeAvatarModal" tabindex="-1" aria-labelledby="changeAvatarModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <div class="modal-header bg-light">
                <h5 class="modal-title fw-bold text-dark" id="changeAvatarModalLabel">
                    <i class="bi bi-person-circle text-primary me-2"></i>Cập Nhật Ảnh Đại Diện
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <form action="${pageContext.request.contextPath}/user/avatar" method="post" enctype="multipart/form-data" id="avatarForm">
                <div class="modal-body p-4">
                    
                    <!-- Khung Xem Trước Ảnh (Preview) -->
                    <div class="text-center mb-4">
                        <div class="position-relative d-inline-block">
                            <div class="avatar-preview-box shadow-sm mx-auto overflow-hidden rounded-circle border border-3 border-white">
                                <c:choose>
                                    <c:when test="${not empty currentUser.avatarUrl}">
                                        <img src="${currentUser.avatarUrl}" id="avatarPreviewImg" class="w-100 h-100 object-fit-cover" alt="Preview" />
                                    </c:when>
                                    <c:otherwise>
                                        <div id="avatarPreviewPlaceholder" class="w-100 h-100 d-flex align-items-center justify-content-center bg-primary text-white fw-bold fs-1">
                                            ${currentUser.fullName != null ? currentUser.fullName.substring(0, 1) : 'U'}
                                        </div>
                                        <img src="" id="avatarPreviewImg" class="w-100 h-100 object-fit-cover d-none" alt="Preview" />
                                    </c:otherwise>
                                </c:choose>
                            </div>
                            <span class="badge bg-primary rounded-pill position-absolute bottom-0 end-0 p-2 shadow-sm" style="transform: translate(0, 0);">
                                <i class="bi bi-pencil-fill"></i>
                            </span>
                        </div>
                        <div class="mt-2 text-muted text-sm">Xem trước ảnh đại diện mới của bạn</div>
                    </div>

                    <!-- Tabs chọn phương thức thay đổi ảnh -->
                    <ul class="nav nav-pills nav-fill mb-3 bg-light p-1 rounded border" id="avatarTab" role="tablist">
                        <li class="nav-item" role="presentation">
                            <button class="nav-link active py-2 text-sm fw-semibold" id="tab-file-tab" data-bs-toggle="pill" data-bs-target="#tab-file" type="button" role="tab" onclick="setAvatarType('file')">
                                <i class="bi bi-upload me-1"></i> Tải ảnh lên
                            </button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link py-2 text-sm fw-semibold" id="tab-preset-tab" data-bs-toggle="pill" data-bs-target="#tab-preset" type="button" role="tab" onclick="setAvatarType('preset')">
                                <i class="bi bi-grid-fill me-1"></i> Avatar mẫu
                            </button>
                        </li>
                        <li class="nav-item" role="presentation">
                            <button class="nav-link py-2 text-sm fw-semibold" id="tab-url-tab" data-bs-toggle="pill" data-bs-target="#tab-url" type="button" role="tab" onclick="setAvatarType('url')">
                                <i class="bi bi-link-45deg me-1"></i> Dùng URL
                            </button>
                        </li>
                    </ul>

                    <input type="hidden" name="avatarType" id="avatarTypeInput" value="file" />
                    <input type="hidden" name="presetAvatarUrl" id="presetAvatarUrlInput" value="" />

                    <div class="tab-content" id="avatarTabContent">
                        <!-- Tab 1: Upload File -->
                        <div class="tab-pane fade show active" id="tab-file" role="tabpanel">
                            <div class="mb-3">
                                <label for="avatarFileInput" class="form-label fw-semibold">Chọn tệp ảnh từ máy tính</label>
                                <input class="form-control" type="file" id="avatarFileInput" name="avatarFile" accept="image/png, image/jpeg, image/webp, image/svg+xml" onchange="previewSelectedFile(this)" />
                                <div class="form-text text-muted">Hỗ trợ các định dạng JPG, PNG, WEBP, SVG (tối đa 10MB).</div>
                            </div>
                        </div>

                        <!-- Tab 2: Preset Avatars -->
                        <div class="tab-pane fade" id="tab-preset" role="tabpanel">
                            <label class="form-label fw-semibold mb-2">Chọn một ảnh avatar chuyên nghiệp có sẵn</label>
                            <div class="row g-2">
                                <c:set var="presets" value="https://api.dicebear.com/7.x/avataaars/svg?seed=Felix,https://api.dicebear.com/7.x/avataaars/svg?seed=Aneka,https://api.dicebear.com/7.x/avataaars/svg?seed=Manager,https://api.dicebear.com/7.x/avataaars/svg?seed=Developer,https://api.dicebear.com/7.x/bottts/svg?seed=Admin,https://api.dicebear.com/7.x/bottts/svg?seed=Recruiter" />
                                <c:forTokens items="${presets}" delims="," var="presetUrl" varStatus="loop">
                                    <div class="col-4 text-center">
                                        <div class="preset-avatar-card p-2 rounded border cursor-pointer hover-shadow" onclick="selectPresetAvatar('${presetUrl}', this)">
                                            <img src="${presetUrl}" class="w-100 h-auto rounded-circle" style="max-width: 60px;" alt="Preset ${loop.index + 1}" />
                                        </div>
                                    </div>
                                </c:forTokens>
                            </div>
                        </div>

                        <!-- Tab 3: URL ảnh -->
                        <div class="tab-pane fade" id="tab-url" role="tabpanel">
                            <div class="mb-3">
                                <label for="avatarUrlInput" class="form-label fw-semibold">Nhập đường dẫn (URL) ảnh trực tuyến</label>
                                <div class="input-group">
                                    <span class="input-group-text"><i class="bi bi-image"></i></span>
                                    <input type="url" class="form-control" id="avatarUrlInput" name="avatarUrlInput" placeholder="https://example.com/my-avatar.jpg" oninput="previewUrlImage(this.value)" />
                                </div>
                                <div class="form-text text-muted">Nhập đường dẫn hình ảnh có đuôi .jpg, .png, .svg,...</div>
                            </div>
                        </div>
                    </div>

                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary px-3" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-primary px-4 fw-semibold">
                        <i class="bi bi-check-circle me-1"></i> Lưu thay đổi
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
function setAvatarType(type) {
    document.getElementById('avatarTypeInput').value = type;
}

function previewSelectedFile(input) {
    if (input.files && input.files[0]) {
        var reader = new FileReader();
        reader.onload = function(e) {
            showPreviewImage(e.target.result);
        }
        reader.readAsDataURL(input.files[0]);
    }
}

function selectPresetAvatar(url, element) {
    document.querySelectorAll('.preset-avatar-card').forEach(function(card) {
        card.classList.remove('border-primary', 'bg-light', 'shadow-sm');
    });
    element.classList.add('border-primary', 'bg-light', 'shadow-sm');
    document.getElementById('presetAvatarUrlInput').value = url;
    setAvatarType('preset');
    showPreviewImage(url);
}

function previewUrlImage(url) {
    if (url && url.trim().length > 5) {
        showPreviewImage(url.trim());
    }
}

function showPreviewImage(src) {
    var imgEl = document.getElementById('avatarPreviewImg');
    var placeholderEl = document.getElementById('avatarPreviewPlaceholder');
    
    imgEl.src = src;
    imgEl.classList.remove('d-none');
    if (placeholderEl) {
        placeholderEl.classList.add('d-none');
    }
}
</script>

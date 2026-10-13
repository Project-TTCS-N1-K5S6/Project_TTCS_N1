<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Quản lý Xuất bản bài đăng" />
<c:set var="breadcrumb" value="Xuất bản bài đăng" />
<c:set var="activeMenu" value="post-publishing" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <div class="container-fluid py-4 px-lg-4">
            <!-- Breadcrumbs chuẩn theo vé KN-120 -->
            <nav aria-label="breadcrumb" class="mb-3">
                <ol class="breadcrumb mb-1 text-xs">
                    <li class="breadcrumb-item text-muted">Không gian</li>
                    <li class="breadcrumb-item text-muted"><i class="bi bi-cloud me-1"></i>K5S6_N1</li>
                    <li class="breadcrumb-item text-muted"><i class="bi bi-lightning me-1 text-warning"></i>KN-110</li>
                    <li class="breadcrumb-item active text-primary fw-bold" aria-current="page"><i class="bi bi-check-square me-1"></i>KN-120</li>
                </ol>
            </nav>

            <!-- Tiêu đề & Thông tin phân hệ -->
            <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-3 mb-4">
                <div>
                    <h2 class="h3 fw-bold text-dark mb-1 d-flex align-items-center gap-2">
                        <i class="bi bi-broadcast text-primary"></i> Quản lý Xuất bản bài đăng
                    </h2>
                    <p class="text-secondary small mb-0">
                        Kiểm soát xuất bản tin tuyển dụng công khai &bull; Xem trước giao diện thực tế &bull; Ghi lại phiên bản và thời điểm xuất bản
                    </p>
                </div>
                <div class="d-flex flex-wrap align-items-center gap-2">
                    <a href="${pageContext.request.contextPath}/careers" target="_blank" class="btn btn-outline-primary btn-sm px-3 shadow-sm">
                        <i class="bi bi-box-arrow-up-right me-1"></i> Xem Cổng việc làm công khai
                    </a>
                    <a href="${pageContext.request.contextPath}/recruitment-requests/create" class="btn btn-primary btn-sm px-3 shadow-sm">
                        <i class="bi bi-plus-lg me-1"></i> Lập yêu cầu mới
                    </a>
                </div>
            </div>

            <!-- Quy định nghiệp vụ vé KN-120 (Alert tiêu chí) -->
            <div class="card border-0 bg-primary-subtle text-primary-emphasis mb-4 shadow-sm">
                <div class="card-body p-3">
                    <div class="d-flex align-items-start gap-3">
                        <div class="bg-primary text-white rounded p-2 d-flex align-items-center justify-content-center">
                            <i class="bi bi-shield-check fs-5"></i>
                        </div>
                        <div class="flex-grow-1">
                            <div class="fw-bold fs-6 mb-1">Quy định xuất bản theo tiêu chuẩn vé KN-120:</div>
                            <ul class="mb-0 ps-3 small">
                                <li><strong>Tin phải duyệt trước khi show công khai:</strong> Chỉ các tin ở trạng thái <em>Đã duyệt (APPROVED / OPEN)</em> mới được xuất bản. Tin nháp hoặc chờ duyệt sẽ bị hệ thống chặn.</li>
                                <li><strong>Xem trước đúng như giao diện khai báo:</strong> Hỗ trợ đối chiếu chuẩn xác từng thông tin đã kê khai trước khi xác nhận bấm xuất bản.</li>
                                <li><strong>Ghi lại bản xuất và thời điểm:</strong> Tự động tăng phiên bản (Bản xuất #1, #2...), gắn nhãn thời điểm chính xác (Timestamp), người thực hiện và lưu snapshot đối chiếu.</li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Thông báo Flash Messages -->
            <c:if test="${not empty flashSuccess}">
                <div class="alert alert-success alert-dismissible fade show shadow-sm mb-4" role="alert">
                    <i class="bi bi-check-circle-fill me-2"></i> ${flashSuccess}
                    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                </div>
                <c:remove var="flashSuccess" scope="session" />
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="alert alert-danger alert-dismissible fade show shadow-sm mb-4" role="alert">
                    <i class="bi bi-exclamation-triangle-fill me-2"></i> ${flashError}
                    <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
                </div>
                <c:remove var="flashError" scope="session" />
            </c:if>

            <!-- Thống kê nhanh KPI -->
            <div class="row g-3 mb-4">
                <div class="col-12 col-sm-6 col-xl-3">
                    <div class="card border-0 shadow-sm h-100">
                        <div class="card-body p-3 d-flex align-items-center gap-3">
                            <div class="rounded-3 bg-primary-subtle text-primary p-3">
                                <i class="bi bi-files fs-4"></i>
                            </div>
                            <div>
                                <div class="text-muted text-xs text-uppercase fw-semibold">Tổng bài đăng</div>
                                <div class="fs-4 fw-bold text-dark">${totalCount}</div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-12 col-sm-6 col-xl-3">
                    <div class="card border-0 shadow-sm h-100">
                        <div class="card-body p-3 d-flex align-items-center gap-3">
                            <div class="rounded-3 bg-success-subtle text-success p-3">
                                <i class="bi bi-broadcast-pin fs-4"></i>
                            </div>
                            <div>
                                <div class="text-muted text-xs text-uppercase fw-semibold">Đang xuất bản</div>
                                <div class="fs-4 fw-bold text-success">${kpiPublished}</div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-12 col-sm-6 col-xl-3">
                    <div class="card border-0 shadow-sm h-100">
                        <div class="card-body p-3 d-flex align-items-center gap-3">
                            <div class="rounded-3 bg-warning-subtle text-warning p-3">
                                <i class="bi bi-hourglass-split fs-4"></i>
                            </div>
                            <div>
                                <div class="text-muted text-xs text-uppercase fw-semibold">Chờ duyệt (Chưa xuất)</div>
                                <div class="fs-4 fw-bold text-warning">${kpiPendingApproval}</div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-12 col-sm-6 col-xl-3">
                    <div class="card border-0 shadow-sm h-100">
                        <div class="card-body p-3 d-flex align-items-center gap-3">
                            <div class="rounded-3 bg-secondary-subtle text-secondary p-3">
                                <i class="bi bi-file-earmark-lock fs-4"></i>
                            </div>
                            <div>
                                <div class="text-muted text-xs text-uppercase fw-semibold">Bản nháp</div>
                                <div class="fs-4 fw-bold text-secondary">${kpiDraft}</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Bộ lọc tìm kiếm -->
            <div class="card border-0 shadow-sm mb-4">
                <div class="card-body p-3">
                    <form method="GET" action="${pageContext.request.contextPath}/post-publishing" class="row g-2 align-items-center">
                        <div class="col-12 col-md-4">
                            <div class="input-group input-group-sm">
                                <span class="input-group-text bg-light border-end-0"><i class="bi bi-search text-muted"></i></span>
                                <input type="text" name="search" class="form-control border-start-0 ps-0" placeholder="Tìm theo mã, chức danh, tiêu đề..." value="${paramSearch}">
                            </div>
                        </div>
                        <div class="col-6 col-md-2">
                            <select name="departmentId" class="form-select form-select-sm">
                                <option value="">-- Phòng ban --</option>
                                <c:forEach var="d" items="${departments}">
                                    <option value="${d.id}" ${d.id == paramDept ? 'selected' : ''}>${d.name}</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-6 col-md-2">
                            <select name="approvalStatus" class="form-select form-select-sm">
                                <option value="">-- Trạng thái duyệt --</option>
                                <option value="APPROVED" ${paramApproval == 'APPROVED' ? 'selected' : ''}>Đã duyệt (Đủ điều kiện)</option>
                                <option value="PENDING" ${paramApproval == 'PENDING' ? 'selected' : ''}>Chờ duyệt (Chưa đủ điều kiện)</option>
                                <option value="DRAFT" ${paramApproval == 'DRAFT' ? 'selected' : ''}>Bản nháp</option>
                            </select>
                        </div>
                        <div class="col-6 col-md-2">
                            <select name="publishStatus" class="form-select form-select-sm">
                                <option value="">-- Trạng thái xuất bản --</option>
                                <option value="PUBLISHED" ${paramPublish == 'PUBLISHED' ? 'selected' : ''}>Đang xuất bản</option>
                                <option value="UNPUBLISHED" ${paramPublish == 'UNPUBLISHED' ? 'selected' : ''}>Đã gỡ bài</option>
                                <option value="DRAFT" ${paramPublish == 'DRAFT' ? 'selected' : ''}>Chưa xuất bản</option>
                            </select>
                        </div>
                        <div class="col-6 col-md-2 d-flex gap-1">
                            <button type="submit" class="btn btn-primary btn-sm flex-grow-1"><i class="bi bi-funnel"></i> Lọc</button>
                            <a href="${pageContext.request.contextPath}/post-publishing" class="btn btn-outline-secondary btn-sm" title="Đặt lại bộ lọc"><i class="bi bi-arrow-clockwise"></i></a>
                        </div>
                    </form>
                </div>
            </div>

            <!-- Bảng danh sách bài đăng -->
            <div class="card border-0 shadow-sm">
                <div class="card-header bg-white border-bottom py-3 d-flex justify-content-between align-items-center">
                    <h5 class="card-title mb-0 fs-6 fw-bold text-dark">
                        <i class="bi bi-list-task me-1 text-primary"></i> Danh sách Bài đăng tuyển dụng &amp; Bản xuất
                    </h5>
                    <span class="badge bg-light text-secondary border font-monospace">Tổng số: ${totalCount}</span>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0" style="font-size: 0.85rem;">
                            <thead class="table-light">
                                <tr>
                                    <th style="width: 11%;">Mã tin</th>
                                    <th style="width: 20%;">Vị trí / Tiêu đề bài đăng</th>
                                    <th style="width: 14%;">Phòng ban &amp; SL</th>
                                    <th class="text-center" style="width: 13%;">Điều kiện duyệt</th>
                                    <th class="text-center" style="width: 13%;">Trạng thái xuất bản</th>
                                    <th style="width: 14%;">Bản xuất &amp; Thời điểm</th>
                                    <th class="text-end pe-3" style="width: 15%;">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:choose>
                                    <c:when test="${not empty postings}">
                                        <c:forEach var="p" items="${postings}">
                                            <tr>
                                                <td>
                                                    <span class="badge bg-light text-dark border font-monospace px-1.5 py-1">${p.requisition.code}</span>
                                                    <div class="text-muted text-xs mt-1">
                                                        <fmt:formatDate value="${p.requisition.createdAt}" pattern="dd/MM/yyyy" />
                                                    </div>
                                                </td>
                                                <td>
                                                    <div class="fw-bold text-dark text-truncate" style="max-width: 240px;" title="${p.requisition.positionTitle}">
                                                        ${p.requisition.positionTitle}
                                                    </div>
                                                    <div class="text-muted small text-truncate" style="max-width: 240px;" title="${p.title}">
                                                        ${p.title}
                                                    </div>
                                                </td>
                                                <td>
                                                    <div class="fw-semibold text-secondary small text-truncate" style="max-width: 160px;" title="${p.requisition.departmentName}">
                                                        <i class="bi bi-building me-1 text-muted"></i>${p.requisition.departmentName != null ? p.requisition.departmentName : 'Chưa gán'}
                                                    </div>
                                                    <span class="badge bg-primary-subtle text-primary border border-primary-subtle text-xs mt-0.5">
                                                        ${p.requisition.headcount} chỉ tiêu
                                                    </span>
                                                </td>
                                                <td class="text-center">
                                                    <!-- TIÊU CHÍ 1: Tin phải duyệt trước khi show công khai -->
                                                    <c:choose>
                                                        <c:when test="${p.approved}">
                                                            <span class="badge bg-success-subtle text-success border border-success-subtle px-2 py-1">
                                                                <i class="bi bi-check-circle-fill me-1"></i>Đã duyệt (Đủ điều kiện)
                                                            </span>
                                                        </c:when>
                                                        <c:when test="${p.requisition.status == 'PENDING_APPROVAL'}">
                                                            <span class="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle px-2 py-1" title="Cần duyệt trước khi xuất bản">
                                                                <i class="bi bi-clock-fill me-1"></i>Chờ phê duyệt
                                                            </span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="badge bg-secondary-subtle text-secondary border px-2 py-1">
                                                                <i class="bi bi-dash-circle me-1"></i>${p.requisition.statusLabel}
                                                            </span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td class="text-center">
                                                    <span class="badge ${p.statusBadgeClass} px-2 py-1">
                                                        <c:if test="${p.published}"><i class="bi bi-broadcast me-1"></i></c:if>
                                                        ${p.statusLabel}
                                                    </span>
                                                </td>
                                                <td>
                                                    <!-- TIÊU CHÍ 3: Ghi lại bản xuất và thời điểm -->
                                                    <c:choose>
                                                        <c:when test="${p.currentVersion > 0}">
                                                            <div class="fw-bold text-dark font-monospace" style="font-size: 0.8rem;">
                                                                <i class="bi bi-layers-fill text-primary me-1"></i>Bản xuất #${p.currentVersion}
                                                            </div>
                                                            <div class="text-muted text-xs" title="Thời điểm xuất bản gần nhất">
                                                                <i class="bi bi-clock-history me-1"></i>${p.formattedPublishedAt}
                                                            </div>
                                                            <c:if test="${not empty p.publishedByName}">
                                                                <div class="text-muted text-xs">
                                                                    Bởi: <strong>${p.publishedByName}</strong>
                                                                </div>
                                                            </c:if>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="text-muted text-xs italic">Chưa từng xuất bản</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td class="text-end pe-3">
                                                    <div class="btn-group btn-group-sm">
                                                        <!-- Nút Xem trước (Tiêu chí 2: Xem trước đúng như khai báo trước khi xuất bản) -->
                                                        <button type="button" class="btn btn-outline-info" 
                                                                onclick="openPreviewModal('${p.requisition.id}')" 
                                                                title="Xem trước đúng như giao diện khai báo trước khi xuất bản">
                                                            <i class="bi bi-eye"></i> Xem trước
                                                        </button>

                                                        <!-- Nút Xuất bản (Tiêu chí 1: Tin phải duyệt trước khi show công khai) -->
                                                        <c:choose>
                                                            <c:when test="${p.approved}">
                                                                <button type="button" class="btn btn-success" 
                                                                        onclick="openPublishModal('${p.requisition.id}', '${p.requisition.code}', '${p.requisition.positionTitle}', ${p.currentVersion})" 
                                                                        title="Tiến hành xuất bản">
                                                                    <i class="bi bi-upload"></i> ${p.published ? 'Cập nhật' : 'Xuất bản'}
                                                                </button>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <button type="button" class="btn btn-outline-secondary" 
                                                                        onclick="alertNotApproved('${p.requisition.id}', '${p.requisition.code}', '${p.requisition.statusLabel}')" 
                                                                        title="Chưa đủ điều kiện: Tin phải duyệt trước khi show công khai">
                                                                    <i class="bi bi-lock-fill"></i> Xuất bản
                                                                </button>
                                                            </c:otherwise>
                                                        </c:choose>

                                                        <!-- Menu dropdown thao tác bổ sung -->
                                                        <button type="button" class="btn btn-outline-secondary dropdown-toggle dropdown-toggle-split" data-bs-toggle="dropdown" aria-expanded="false">
                                                            <span class="visually-hidden">Tùy chọn</span>
                                                        </button>
                                                        <ul class="dropdown-menu dropdown-menu-end shadow border-0">
                                                            <!-- TIÊU CHÍ 3: Lịch sử các bản xuất và thời điểm -->
                                                            <li>
                                                                <a class="dropdown-item" href="javascript:void(0)" onclick="openHistoryModal('${p.requisition.id}', '${p.requisition.code}', '${p.requisition.positionTitle}')">
                                                                    <i class="bi bi-clock-history text-primary me-2"></i> Lịch sử bản xuất &amp; thời điểm
                                                                </a>
                                                            </li>
                                                            <c:if test="${p.published}">
                                                                <li>
                                                                    <a class="dropdown-item" href="${pageContext.request.contextPath}/careers/job?id=${p.requisition.id}" target="_blank">
                                                                        <i class="bi bi-box-arrow-up-right text-success me-2"></i> Mở bài đăng công khai
                                                                    </a>
                                                                </li>
                                                                <li><hr class="dropdown-divider"></li>
                                                                <li>
                                                                    <a class="dropdown-item text-danger" href="javascript:void(0)" onclick="openUnpublishModal('${p.requisition.id}', '${p.requisition.code}', '${p.requisition.positionTitle}')">
                                                                        <i class="bi bi-arrow-down-circle text-danger me-2"></i> Gỡ tin đã xuất bản
                                                                    </a>
                                                                </li>
                                                            </c:if>
                                                            <c:if test="${!p.approved and (currentUser.hasRole('ADMIN') or currentUser.hasRole('HR_MANAGER'))}">
                                                                <li><hr class="dropdown-divider"></li>
                                                                <li>
                                                                    <a class="dropdown-item text-warning-emphasis" href="javascript:void(0)" onclick="quickApprove('${p.requisition.id}', '${p.requisition.code}', '${p.requisition.positionTitle}')">
                                                                        <i class="bi bi-check2-circle text-warning me-2"></i> Duyệt tin ngay (Để đủ điều kiện)
                                                                    </a>
                                                                </li>
                                                            </c:if>
                                                        </ul>
                                                    </div>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        <tr>
                                            <td colspan="7" class="text-center py-5 text-muted">
                                                <i class="bi bi-broadcast fs-1 d-block mb-2 text-secondary opacity-50"></i>
                                                Chưa có bài đăng nào phù hợp bộ lọc tìm kiếm.
                                            </td>
                                        </tr>
                                    </c:otherwise>
                                </c:choose>
                            </tbody>
                        </table>
                    </div>

                    <!-- Phân trang -->
                    <c:if test="${totalPages > 1}">
                        <div class="p-3 border-top d-flex flex-wrap justify-content-between align-items-center gap-2">
                            <small class="text-muted">Trang <strong>${currentPage}</strong> / <strong>${totalPages}</strong> (Tổng <strong>${totalCount}</strong> bài đăng)</small>
                            <nav>
                                <ul class="pagination pagination-sm mb-0">
                                    <li class="page-item ${currentPage <= 1 ? 'disabled' : ''}">
                                        <a class="page-link" href="${pageContext.request.contextPath}/post-publishing?page=${currentPage - 1}&search=${paramSearch}&departmentId=${paramDept}&approvalStatus=${paramApproval}&publishStatus=${paramPublish}">
                                            <i class="bi bi-chevron-left"></i>
                                        </a>
                                    </li>
                                    <c:forEach begin="1" end="${totalPages}" var="pg">
                                        <li class="page-item ${currentPage == pg ? 'active' : ''}">
                                            <a class="page-link" href="${pageContext.request.contextPath}/post-publishing?page=${pg}&search=${paramSearch}&departmentId=${paramDept}&approvalStatus=${paramApproval}&publishStatus=${paramPublish}">${pg}</a>
                                        </li>
                                    </c:forEach>
                                    <li class="page-item ${currentPage >= totalPages ? 'disabled' : ''}">
                                        <a class="page-link" href="${pageContext.request.contextPath}/post-publishing?page=${currentPage + 1}&search=${paramSearch}&departmentId=${paramDept}&approvalStatus=${paramApproval}&publishStatus=${paramPublish}">
                                            <i class="bi bi-chevron-right"></i>
                                        </a>
                                    </li>
                                </ul>
                            </nav>
                        </div>
                    </c:if>
                </div>
            </div>
        </div>
    </div>

<!-- ============================================================== -->
<!-- MODAL 1: XEM TRƯỚC ĐÚNG NHƯ KHAI BÁO TRƯỚC KHI XUẤT BẢN [KN-120] -->
<!-- ============================================================== -->
<div class="modal fade" id="previewModal" tabindex="-1" aria-labelledby="previewModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content shadow-lg border-0">
            <div class="modal-header bg-light border-bottom py-3">
                <div class="d-flex align-items-center gap-3">
                    <div class="bg-primary text-white rounded p-2">
                        <i class="bi bi-eye-fill fs-5"></i>
                    </div>
                    <div>
                        <h5 class="modal-title fw-bold text-dark mb-0" id="previewModalLabel">Xem trước bài đăng tuyển dụng</h5>
                        <small class="text-secondary">Đối chiếu đúng như giao diện khai báo trước khi bấm xuất bản</small>
                    </div>
                </div>
                <div class="d-flex align-items-center gap-2">
                    <span id="previewApprovalBadge" class="badge"></span>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
            </div>

            <!-- Tabs chuyển đổi: Giao diện khai báo vs Giao diện ứng viên công khai -->
            <div class="bg-white border-bottom px-3 pt-2">
                <ul class="nav nav-tabs border-0" id="previewTab" role="tablist">
                    <li class="nav-item" role="presentation">
                        <button class="nav-link active fw-semibold" id="declared-view-tab" data-bs-toggle="tab" data-bs-target="#declared-view" type="button" role="tab">
                            <i class="bi bi-ui-checks me-1"></i> Giao diện khai báo nội bộ
                        </button>
                    </li>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link fw-semibold" id="public-view-tab" data-bs-toggle="tab" data-bs-target="#public-view" type="button" role="tab">
                            <i class="bi bi-laptop me-1"></i> Giao diện hiển thị công khai (Ứng viên xem)
                        </button>
                    </li>
                </ul>
            </div>

            <div class="modal-body p-4 bg-light-subtle">
                <div class="tab-content" id="previewTabContent">
                    <!-- TAB 1: GIAO DIỆN KHAI BÁO -->
                    <div class="tab-pane fade show active" id="declared-view" role="tabpanel">
                        <div class="card border shadow-sm mb-3">
                            <div class="card-header bg-white py-2 fw-bold text-primary">
                                <i class="bi bi-info-circle me-1"></i> 1. Thông tin vị trí &amp; Tham số khai báo
                            </div>
                            <div class="card-body">
                                <div class="row g-3">
                                    <div class="col-md-3">
                                        <div class="text-xs text-muted text-uppercase">Mã yêu cầu</div>
                                        <div class="fw-bold font-monospace" id="prevCode">--</div>
                                    </div>
                                    <div class="col-md-5">
                                        <div class="text-xs text-muted text-uppercase">Chức danh tuyển dụng</div>
                                        <div class="fw-bold fs-6 text-dark" id="prevPosition">--</div>
                                    </div>
                                    <div class="col-md-4">
                                        <div class="text-xs text-muted text-uppercase">Phòng ban</div>
                                        <div class="fw-bold text-secondary" id="prevDept">--</div>
                                    </div>
                                    <div class="col-md-3">
                                        <div class="text-xs text-muted text-uppercase">Số lượng tuyển dụng</div>
                                        <div class="fw-bold text-primary" id="prevHeadcount">--</div>
                                    </div>
                                    <div class="col-md-3">
                                        <div class="text-xs text-muted text-uppercase">Lý do tuyển dụng</div>
                                        <div class="fw-semibold" id="prevReason">--</div>
                                    </div>
                                    <div class="col-md-3">
                                        <div class="text-xs text-muted text-uppercase">Dải ngân sách lương</div>
                                        <div class="fw-bold text-success" id="prevSalary">--</div>
                                    </div>
                                    <div class="col-md-3">
                                        <div class="text-xs text-muted text-uppercase">Hạn nộp hồ sơ / Ngày cần</div>
                                        <div class="fw-bold text-danger" id="prevDeadline">--</div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Giải trình lương nếu có -->
                        <div class="card border border-warning shadow-sm mb-3" id="prevSalaryExpCard" style="display: none;">
                            <div class="card-header bg-warning-subtle text-warning-emphasis py-2 fw-bold">
                                <i class="bi bi-exclamation-triangle-fill me-1"></i> Giải trình ngân sách (Lương ngoài chuẩn)
                            </div>
                            <div class="card-body bg-white py-2" id="prevSalaryExp">--</div>
                        </div>

                        <!-- Mô tả công việc JD -->
                        <div class="card border shadow-sm mb-3">
                            <div class="card-header bg-white py-2 fw-bold text-dark">
                                <i class="bi bi-file-text me-1 text-primary"></i> 2. Mô tả công việc (Job Description)
                            </div>
                            <div class="card-body bg-white" style="white-space: pre-line; line-height: 1.6;" id="prevJd">--</div>
                        </div>

                        <!-- Yêu cầu ứng viên Requirements -->
                        <div class="card border shadow-sm mb-3">
                            <div class="card-header bg-white py-2 fw-bold text-dark">
                                <i class="bi bi-person-check me-1 text-primary"></i> 3. Yêu cầu ứng viên (Candidate Requirements)
                            </div>
                            <div class="card-body bg-white" style="white-space: pre-line; line-height: 1.6;" id="prevReq">--</div>
                        </div>
                    </div>

                    <!-- TAB 2: GIAO DIỆN BÀI ĐĂNG CÔNG KHAI (Mô phỏng Candidate Portal) -->
                    <div class="tab-pane fade" id="public-view" role="tabpanel">
                        <div class="card border-0 shadow rounded-4 overflow-hidden bg-white">
                            <!-- Banner giả lập -->
                            <div class="bg-primary text-white p-4 p-lg-5" style="background: linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%);">
                                <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
                                    <span class="badge bg-white text-primary px-3 py-1.5 fw-bold font-monospace" id="pubBadgeCode">REQ-2026-001</span>
                                    <span class="badge bg-warning text-dark px-3 py-1.5"><i class="bi bi-clock me-1"></i>Hạn nộp: <span id="pubBadgeDeadline">--</span></span>
                                </div>
                                <h2 class="h2 fw-bold mb-2 text-white" id="pubTitle">--</h2>
                                <div class="d-flex flex-wrap gap-3 text-white-50 fs-6">
                                    <span><i class="bi bi-building me-1"></i><span id="pubDept">--</span></span>
                                    <span><i class="bi bi-geo-alt me-1"></i>Trụ sở công ty</span>
                                    <span><i class="bi bi-cash-stack me-1"></i><strong class="text-white" id="pubSalary">--</strong></span>
                                </div>
                            </div>

                            <div class="p-4 p-lg-5">
                                <div class="row g-4">
                                    <div class="col-lg-8">
                                        <h5 class="fw-bold text-dark border-bottom pb-2 mb-3">
                                            <i class="bi bi-briefcase text-primary me-2"></i>Mô tả công việc
                                        </h5>
                                        <div class="text-secondary mb-4" style="white-space: pre-line; line-height: 1.7;" id="pubJd">--</div>

                                        <h5 class="fw-bold text-dark border-bottom pb-2 mb-3">
                                            <i class="bi bi-check2-circle text-primary me-2"></i>Yêu cầu ứng viên
                                        </h5>
                                        <div class="text-secondary mb-4" style="white-space: pre-line; line-height: 1.7;" id="pubReq">--</div>

                                        <h5 class="fw-bold text-dark border-bottom pb-2 mb-3">
                                            <i class="bi bi-gift text-primary me-2"></i>Quyền lợi được hưởng
                                        </h5>
                                        <ul class="text-secondary" style="line-height: 1.8;">
                                            <li>Lương thưởng cạnh tranh tương xứng năng lực + Thưởng KPI theo quý/năm.</li>
                                            <li>Đầy đủ chế độ bảo hiểm theo quy định pháp luật + Gói bảo hiểm sức khỏe nâng cao.</li>
                                            <li>Môi trường làm việc năng động, lộ trình thăng tiến rõ ràng theo Khung năng lực.</li>
                                            <li>Phụ cấp ăn trưa, gửi xe, du lịch thường niên cùng công ty.</li>
                                        </ul>
                                    </div>

                                    <div class="col-lg-4">
                                        <div class="card border bg-light-subtle rounded-3 p-3">
                                            <h6 class="fw-bold text-dark mb-3">Thông tin tóm tắt</h6>
                                            <div class="mb-2 text-xs text-muted">Số lượng cần tuyển:</div>
                                            <div class="fw-bold mb-3" id="pubHeadcount">--</div>

                                            <div class="mb-2 text-xs text-muted">Hình thức làm việc:</div>
                                            <div class="fw-bold mb-3">Toàn thời gian cố định</div>

                                            <div class="mb-2 text-xs text-muted">Chuyên viên phụ trách:</div>
                                            <div class="fw-bold mb-4" id="pubRecruiter">--</div>

                                            <button type="button" class="btn btn-primary w-100 py-2 fw-bold shadow-sm" disabled>
                                                <i class="bi bi-send-fill me-1"></i> Nộp hồ sơ ứng tuyển ngay
                                            </button>
                                            <small class="text-muted text-center d-block mt-2">Bản xem trước mô phỏng giao diện ứng viên</small>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="modal-footer bg-light border-top py-2 d-flex justify-content-between">
                <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Đóng</button>
                <div id="prevModalActions">
                    <!-- Nút xuất bản trực tiếp nếu đủ điều kiện -->
                </div>
            </div>
        </div>
    </div>
</div>

<!-- ============================================================== -->
<!-- MODAL 2: XÁC NHẬN XUẤT BẢN BÀI ĐĂNG & GHI LẠI BẢN XUẤT [KN-120] -->
<!-- ============================================================== -->
<div class="modal fade" id="publishModal" tabindex="-1" aria-labelledby="publishModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content shadow-lg border-0">
            <form method="POST" action="${pageContext.request.contextPath}/post-publishing/publish">
                <div class="modal-header bg-success text-white py-3">
                    <h5 class="modal-title fw-bold mb-0" id="publishModalLabel">
                        <i class="bi bi-broadcast me-2"></i> Xuất bản bài đăng tuyển dụng
                    </h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <input type="hidden" name="requisitionId" id="pubFormReqId">

                    <!-- Hộp thông báo ghi nhận phiên bản & thời điểm -->
                    <div class="alert alert-info border-0 mb-3 small">
                        <i class="bi bi-info-circle-fill me-1"></i>
                        Hệ thống sẽ ghi nhận <strong><span id="pubNextVersionLabel">Bản xuất #1</span></strong> vào thời điểm hiện tại và lưu trữ toàn bộ dữ liệu khai báo đối chiếu.
                    </div>

                    <div class="mb-3">
                        <label class="form-label text-xs fw-bold text-uppercase text-muted">Mã &amp; Chức danh:</label>
                        <div class="p-2 bg-light border rounded fw-semibold text-dark" id="pubFormInfo">--</div>
                    </div>

                    <div class="mb-3">
                        <label for="pubChannel" class="form-label text-xs fw-bold text-uppercase text-muted">Kênh xuất bản:</label>
                        <select name="channel" id="pubChannel" class="form-select">
                            <option value="CAREER_PORTAL">Cổng tuyển dụng công khai (Website công ty)</option>
                            <option value="INTERNAL">Bảng tin nội bộ (Dành cho nhân viên)</option>
                            <option value="LINKEDIN">LinkedIn Tuyển dụng</option>
                            <option value="TOPCV">Cổng TopCV</option>
                        </select>
                    </div>

                    <div class="mb-3">
                        <label for="pubNote" class="form-label text-xs fw-bold text-uppercase text-muted">Ghi chú / Tóm tắt thay đổi bản xuất:</label>
                        <textarea name="note" id="pubNote" class="form-control" rows="3" placeholder="Nhập lý do xuất bản hoặc tóm tắt điểm mới của bản xuất này..."></textarea>
                    </div>
                </div>
                <div class="modal-footer bg-light border-top py-2">
                    <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Hủy bỏ</button>
                    <button type="submit" class="btn btn-success btn-sm px-4 fw-bold">
                        <i class="bi bi-check-circle me-1"></i> Xác nhận Xuất bản &amp; Ghi thời điểm
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- ============================================================== -->
<!-- MODAL 3: LỊCH SỬ BẢN XUẤT VÀ THỜI ĐIỂM [KN-120]               -->
<!-- ============================================================== -->
<div class="modal fade" id="historyModal" tabindex="-1" aria-labelledby="historyModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content shadow-lg border-0">
            <div class="modal-header bg-light border-bottom py-3">
                <div class="d-flex align-items-center gap-2">
                    <i class="bi bi-clock-history fs-4 text-primary"></i>
                    <div>
                        <h5 class="modal-title fw-bold text-dark mb-0" id="historyModalLabel">Lịch sử bản xuất và thời điểm</h5>
                        <small class="text-muted" id="historySubTitle">--</small>
                    </div>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0" style="font-size: 0.85rem;">
                        <thead class="table-light">
                            <tr>
                                <th style="width: 15%;">Phiên bản</th>
                                <th style="width: 25%;">Thời điểm xuất bản</th>
                                <th style="width: 20%;">Người xuất bản</th>
                                <th style="width: 15%;">Kênh</th>
                                <th class="text-center" style="width: 12%;">Trạng thái</th>
                                <th class="text-end pe-3" style="width: 13%;">Xem snapshot</th>
                            </tr>
                        </thead>
                        <tbody id="historyTableBody">
                            <tr><td colspan="6" class="text-center py-4"><div class="spinner-border text-primary spinner-border-sm"></div> Đang tải lịch sử...</td></tr>
                        </tbody>
                    </table>
                </div>

                <!-- Chi tiết snapshot đối chiếu khi bấm xem lại -->
                <div id="snapshotDetailBox" class="p-3 border-top bg-light" style="display: none;">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <strong class="text-primary"><i class="bi bi-file-earmark-check me-1"></i>Chi tiết Snapshot bản xuất đã lưu:</strong>
                        <button type="button" class="btn btn-sm btn-outline-secondary py-0 px-2" onclick="closeSnapshotBox()">Đóng xem lại</button>
                    </div>
                    <pre id="snapshotContent" class="bg-white p-3 border rounded text-xs text-dark font-monospace mb-0" style="max-height: 250px; overflow-y: auto;"></pre>
                </div>
            </div>
            <div class="modal-footer bg-light border-top py-2">
                <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Đóng</button>
            </div>
        </div>
    </div>
</div>

<!-- ============================================================== -->
<!-- MODAL 4: GỠ BÀI ĐĂNG (UNPUBLISH)                               -->
<!-- ============================================================== -->
<div class="modal fade" id="unpublishModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content shadow border-0">
            <form method="POST" action="${pageContext.request.contextPath}/post-publishing/unpublish">
                <div class="modal-header bg-danger text-white py-2">
                    <h5 class="modal-title fs-6 fw-bold mb-0"><i class="bi bi-arrow-down-circle me-1"></i> Xác nhận gỡ tin xuất bản</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-3">
                    <input type="hidden" name="requisitionId" id="unpubFormReqId">
                    <p class="mb-2">Bạn có chắc chắn muốn gỡ bài đăng này khỏi cổng thông tin công khai?</p>
                    <div class="p-2 bg-light border rounded fw-semibold text-dark mb-3" id="unpubFormInfo">--</div>
                    <div class="mb-2">
                        <label class="form-label text-xs fw-bold text-muted">Lý do gỡ bài:</label>
                        <input type="text" name="reason" class="form-control form-control-sm" placeholder="Ví dụ: Đã đủ ứng viên, Tạm dừng tuyển...">
                    </div>
                </div>
                <div class="modal-footer py-2 bg-light">
                    <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-danger btn-sm px-3 fw-bold">Xác nhận gỡ tin</button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- ============================================================== -->
<!-- MODAL 5: DUYỆT NHANH (QUICK APPROVE)                          -->
<!-- ============================================================== -->
<div class="modal fade" id="approveModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content shadow border-0">
            <form method="POST" action="${pageContext.request.contextPath}/post-publishing/approve">
                <div class="modal-header bg-warning py-2 text-dark">
                    <h5 class="modal-title fs-6 fw-bold mb-0"><i class="bi bi-check2-circle me-1"></i> Phê duyệt yêu cầu tuyển dụng</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-3">
                    <input type="hidden" name="requisitionId" id="approveFormReqId">
                    <p class="mb-2">Sau khi duyệt, bài đăng này sẽ đủ điều kiện để xuất bản công khai theo quy định vé KN-120:</p>
                    <div class="p-2 bg-light border rounded fw-semibold text-dark mb-2" id="approveFormInfo">--</div>
                </div>
                <div class="modal-footer py-2 bg-light">
                    <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-warning btn-sm px-3 fw-bold">Xác nhận Phê duyệt</button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- JavaScript điều khiển logic vé KN-120 -->
<script>
const ctx = '${pageContext.request.contextPath}';

// 1. Xem trước đúng như giao diện khai báo trước khi xuất bản [KN-120]
function openPreviewModal(id) {
    const modal = new bootstrap.Modal(document.getElementById('previewModal'));
    modal.show();

    // Reset giao diện
    document.getElementById('prevCode').innerText = 'Đang tải...';
    document.getElementById('prevPosition').innerText = 'Đang tải...';
    document.getElementById('prevDept').innerText = 'Đang tải...';
    document.getElementById('prevHeadcount').innerText = '--';
    document.getElementById('prevReason').innerText = '--';
    document.getElementById('prevSalary').innerText = '--';
    document.getElementById('prevDeadline').innerText = '--';
    document.getElementById('prevJd').innerText = '--';
    document.getElementById('prevReq').innerText = '--';
    document.getElementById('prevSalaryExpCard').style.display = 'none';
    document.getElementById('prevModalActions').innerHTML = '';

    fetch(ctx + '/post-publishing/preview?id=' + encodeURIComponent(id))
        .then(res => res.json())
        .then(data => {
            if (data.error) {
                alert('Lỗi: ' + data.error);
                return;
            }

            // Giao diện khai báo
            document.getElementById('prevCode').innerText = data.code;
            document.getElementById('prevPosition').innerText = data.positionTitle;
            document.getElementById('prevDept').innerText = data.departmentName;
            document.getElementById('prevHeadcount').innerText = data.headcount + ' nhân sự';
            document.getElementById('prevReason').innerText = data.recruitmentReasonLabel;
            document.getElementById('prevSalary').innerText = data.formattedSalaryRange;
            document.getElementById('prevDeadline').innerText = data.deadlineFormatted;
            document.getElementById('prevJd').innerText = data.jobDescription || 'Chưa soạn bản mô tả công việc.';
            document.getElementById('prevReq').innerText = data.jobRequirements || 'Chưa soạn yêu cầu ứng viên.';

            if (data.salaryExplanation && data.salaryExplanation.trim() !== '') {
                document.getElementById('prevSalaryExpCard').style.display = 'block';
                document.getElementById('prevSalaryExp').innerText = data.salaryExplanation;
            }

            // Giao diện bài đăng công khai
            document.getElementById('pubBadgeCode').innerText = data.code;
            document.getElementById('pubBadgeDeadline').innerText = data.deadlineFormatted;
            document.getElementById('pubTitle').innerText = data.title;
            document.getElementById('pubDept').innerText = data.departmentName;
            document.getElementById('pubSalary').innerText = data.formattedSalaryRange;
            document.getElementById('pubJd').innerText = data.jobDescription || 'Chưa cập nhật mô tả.';
            document.getElementById('pubReq').innerText = data.jobRequirements || 'Chưa cập nhật yêu cầu.';
            document.getElementById('pubHeadcount').innerText = data.headcount + ' vị trí';
            document.getElementById('pubRecruiter').innerText = data.recruiterName;

            // Huy hiệu điều kiện xuất bản
            const badge = document.getElementById('previewApprovalBadge');
            if (data.isApproved) {
                badge.className = 'badge bg-success text-white px-2 py-1';
                badge.innerHTML = '<i class="bi bi-check-circle-fill me-1"></i>Đủ điều kiện xuất bản (Đã duyệt)';
                document.getElementById('prevModalActions').innerHTML = `
                    <button type="button" class="btn btn-success btn-sm px-3" onclick="openPublishFromPreview('\${data.id}', '\${data.code}', '\${data.positionTitle}', \${data.currentVersion})">
                        <i class="bi bi-upload me-1"></i> Tiến hành Xuất bản bài đăng này
                    </button>
                `;
            } else {
                badge.className = 'badge bg-danger text-white px-2 py-1';
                badge.innerHTML = '<i class="bi bi-lock-fill me-1"></i>Chưa đủ điều kiện: Tin phải duyệt trước khi show công khai';
                document.getElementById('prevModalActions').innerHTML = `
                    <button type="button" class="btn btn-outline-warning btn-sm" onclick="quickApprove('\${data.id}', '\${data.code}', '\${data.positionTitle}')">
                        <i class="bi bi-check2-circle me-1"></i> Phê duyệt tin ngay
                    </button>
                `;
            }
        })
        .catch(err => {
            alert('Lỗi tải dữ liệu xem trước: ' + err);
        });
}

function openPublishFromPreview(id, code, pos, ver) {
    const prevEl = document.getElementById('previewModal');
    const prevInst = bootstrap.Modal.getInstance(prevEl);
    if (prevInst) prevInst.hide();

    setTimeout(() => {
        openPublishModal(id, code, pos, ver);
    }, 400);
}

// 2. Mở Modal Xuất bản bài đăng
function openPublishModal(id, code, pos, ver) {
    document.getElementById('pubFormReqId').value = id;
    document.getElementById('pubFormInfo').innerText = code + ' - ' + pos;
    const nextVer = ver + 1;
    document.getElementById('pubNextVersionLabel').innerText = 'Bản xuất #' + nextVer;
    new bootstrap.Modal(document.getElementById('publishModal')).show();
}

// 3. Cảnh báo khi tin chưa duyệt
function alertNotApproved(id, code, statusLabel) {
    alert('TIÊU CHÍ BẮT BUỘC (KN-120):\nTin phải duyệt trước khi show công khai!\n\nTin ' + code + ' hiện đang ở trạng thái: ' + statusLabel + '.\nVui lòng gửi phê duyệt hoặc duyệt tin trước khi xuất bản.');
}

// 4. Mở Modal Lịch sử các bản xuất và thời điểm [KN-120]
function openHistoryModal(reqId, code, pos) {
    document.getElementById('historySubTitle').innerText = code + ' - ' + pos;
    const tbody = document.getElementById('historyTableBody');
    tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4"><div class="spinner-border text-primary spinner-border-sm"></div> Đang tải lịch sử bản xuất...</td></tr>';
    document.getElementById('snapshotDetailBox').style.display = 'none';

    new bootstrap.Modal(document.getElementById('historyModal')).show();

    fetch(ctx + '/post-publishing/history?requisitionId=' + encodeURIComponent(reqId))
        .then(res => res.json())
        .then(list => {
            if (!list || list.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">Chưa có lịch sử bản xuất nào cho bài đăng này.</td></tr>';
                return;
            }

            let html = '';
            list.forEach(item => {
                const badgeClass = item.status === 'PUBLISHED' ? 'bg-success' : (item.status === 'ARCHIVED' ? 'bg-secondary' : 'bg-warning text-dark');
                const statusName = item.status === 'PUBLISHED' ? 'Đang áp dụng' : (item.status === 'ARCHIVED' ? 'Lưu trữ' : 'Đã gỡ');
                html += `
                    <tr>
                        <td class="fw-bold font-monospace text-primary">
                            <i class="bi bi-tag-fill me-1"></i>\${item.versionLabel}
                        </td>
                        <td class="font-monospace text-dark">
                            <i class="bi bi-clock me-1 text-muted"></i>\${item.formattedPublishedAt}
                        </td>
                        <td>
                            <strong>\${item.publishedByName || 'Hệ thống'}</strong>
                        </td>
                        <td>
                            <span class="badge bg-light text-secondary border">\${item.channel || 'Website công khai'}</span>
                        </td>
                        <td class="text-center">
                            <span class="badge \${badgeClass}">\${statusName}</span>
                        </td>
                        <td class="text-end pe-3">
                            <button type="button" class="btn btn-sm btn-outline-info p-1 px-2" onclick="inspectSnapshot('\${item.id}')" title="Xem lại dữ liệu snapshot bản này">
                                <i class="bi bi-file-earmark-code"></i> Snapshot
                            </button>
                        </td>
                    </tr>
                `;
            });
            tbody.innerHTML = html;
        })
        .catch(err => {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center py-3 text-danger">Lỗi tải lịch sử bản xuất: ' + err + '</td></tr>';
        });
}

function inspectSnapshot(pubId) {
    fetch(ctx + '/post-publishing/version-detail?id=' + encodeURIComponent(pubId))
        .then(res => res.json())
        .then(pub => {
            if (pub.error) {
                alert('Lỗi: ' + pub.error);
                return;
            }
            const box = document.getElementById('snapshotDetailBox');
            box.style.display = 'block';

            let jsonPretty = pub.snapshotData;
            try {
                const parsed = JSON.parse(pub.snapshotData);
                jsonPretty = JSON.stringify(parsed, null, 2);
            } catch (e) {}

            document.getElementById('snapshotContent').innerText = jsonPretty;
            box.scrollIntoView({ behavior: 'smooth' });
        })
        .catch(err => {
            alert('Lỗi lấy snapshot: ' + err);
        });
}

function closeSnapshotBox() {
    document.getElementById('snapshotDetailBox').style.display = 'none';
}

function openUnpublishModal(id, code, pos) {
    document.getElementById('unpubFormReqId').value = id;
    document.getElementById('unpubFormInfo').innerText = code + ' - ' + pos;
    new bootstrap.Modal(document.getElementById('unpublishModal')).show();
}

function quickApprove(id, code, pos) {
    document.getElementById('approveFormReqId').value = id;
    document.getElementById('approveFormInfo').innerText = code + ' - ' + pos;
    new bootstrap.Modal(document.getElementById('approveModal')).show();
}
</script>

<jsp:include page="../common/footer.jsp" />

<%--
  ==============================================================================
  GIAO DIỆN QUẢN LÝ XUẤT BẢN BÀI ĐĂNG (publishing/index.jsp)
  Chức năng:
  - Hiển thị danh sách các bài đăng tuyển dụng trên toàn hệ thống (bản nháp, đã xuất bản, gỡ bài).
  - Kiểm soát điều kiện tiên quyết: Yêu cầu tuyển dụng bắt buộc phải được phê duyệt trước khi xuất bản công khai.
  - Xem trước bài đăng trên giao diện popup modal.
  - Ghi vết lịch sử xuất bản (Publication History): ai xuất bản, kênh nào, thời điểm xuất bản kèm snapshot nội dung JSON.
  ==============================================================================
--%>
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
        <jsp:include page="../common/alerts.jsp" />
        <div class="container-fluid py-4 px-lg-4">
            <!-- Breadcrumbs -->
            <nav aria-label="breadcrumb" class="mb-3">
                <ol class="breadcrumb mb-1 text-xs">
                    <li class="breadcrumb-item text-muted"><a href="${pageContext.request.contextPath}/dashboard" class="text-decoration-none text-muted">Trang chủ</a></li>
                    <li class="breadcrumb-item text-muted"><i class="bi bi-briefcase me-1"></i>Tuyển dụng</li>
                    <li class="breadcrumb-item active text-primary fw-bold" aria-current="page"><i class="bi bi-broadcast-pin me-1"></i>Xuất bản bài đăng</li>
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
                    <button type="button" class="btn btn-warning btn-sm px-3 shadow-sm text-dark fw-semibold" onclick="openQuickCreateModal()">
                        <i class="bi bi-lightning-charge-fill me-1"></i> Tạo nhanh bài đăng
                    </button>
                    <a href="${pageContext.request.contextPath}/post-publishing/create" class="btn btn-outline-success btn-sm px-3 shadow-sm">
                        <i class="bi bi-file-earmark-plus me-1"></i> Màn hình tạo bài đăng
                    </a>
                    <a href="${pageContext.request.contextPath}/careers" target="_blank" class="btn btn-outline-primary btn-sm px-3 shadow-sm">
                        <i class="bi bi-box-arrow-up-right me-1"></i> Xem Cổng việc làm công khai
                    </a>
                    <a href="${pageContext.request.contextPath}/recruitment-requests/create" class="btn btn-primary btn-sm px-3 shadow-sm">
                        <i class="bi bi-plus-lg me-1"></i> Lập yêu cầu mới
                    </a>
                </div>
            </div>

            <!-- Quy định nghiệp vụ (Alert tiêu chí) -->
            <div class="card border-0 bg-primary-subtle text-primary-emphasis mb-4 shadow-sm">
                <div class="card-body p-3">
                    <div class="d-flex align-items-start gap-3">
                        <div class="bg-primary text-white rounded p-2 d-flex align-items-center justify-content-center">
                            <i class="bi bi-shield-check fs-5"></i>
                        </div>
                        <div class="flex-grow-1">
                            <div class="fw-bold fs-6 mb-1">Quy định xuất bản tin tuyển dụng:</div>
                            <ul class="mb-0 ps-3 small">
                                <li><strong>Tin phải duyệt trước khi show công khai:</strong> Chỉ các tin ở trạng thái <em>Đã duyệt (APPROVED / OPEN)</em> mới được xuất bản. Tin nháp hoặc chờ duyệt sẽ bị hệ thống chặn.</li>
                                <li><strong>Xem trước đúng như giao diện khai báo:</strong> Hỗ trợ đối chiếu chuẩn xác từng thông tin đã kê khai trước khi xác nhận bấm xuất bản.</li>
                                <li><strong>Ghi lại bản xuất và thời điểm:</strong> Tự động tăng phiên bản (Bản xuất #1, #2...), gắn nhãn thời điểm chính xác (Timestamp), người thực hiện và lưu snapshot đối chiếu.</li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>

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
                        <table class="table table-custom table-hover align-middle mb-0" style="font-size: 0.82rem;">
                            <thead class="table-light">
                                <tr>
                                    <th style="width: 10%; min-width: 90px;">Mã tin</th>
                                    <th style="width: 23%; min-width: 160px;">Vị trí / Tiêu đề bài đăng</th>
                                    <th style="width: 15%; min-width: 130px;">Phòng ban &amp; SL</th>
                                    <th class="text-center" style="width: 13%; min-width: 110px;">Điều kiện duyệt</th>
                                    <th class="text-center" style="width: 12%; min-width: 105px;">Trạng thái xuất bản</th>
                                    <th style="width: 13%; min-width: 115px;">Bản xuất &amp; Thời điểm</th>
                                    <th class="text-end pe-3" style="width: 14%; min-width: 130px;">Thao tác</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:choose>
                                    <c:when test="${not empty postings}">
                                        <c:forEach var="p" items="${postings}">
                                            <tr>
                                                <td>
                                                    <span class="badge bg-light text-dark border font-monospace px-1.5 py-0.5 text-nowrap">${p.requisition.code}</span>
                                                    <div class="text-muted text-xs mt-1 text-nowrap">
                                                        <fmt:formatDate value="${p.requisition.createdAt}" pattern="dd/MM/yyyy" />
                                                    </div>
                                                </td>
                                                <td>
                                                    <div class="fw-bold text-dark text-truncate" style="max-width: 220px;" title="${p.requisition.positionTitle}">
                                                        ${p.requisition.positionTitle}
                                                    </div>
                                                    <div class="text-muted small text-truncate" style="max-width: 220px;" title="${p.title}">
                                                        ${p.title}
                                                    </div>
                                                </td>
                                                <td>
                                                    <div class="fw-semibold text-secondary small text-truncate" style="max-width: 150px;" title="${p.requisition.departmentName}">
                                                        <i class="bi bi-building me-1 text-muted"></i>${p.requisition.departmentName != null ? p.requisition.departmentName : 'Chưa gán'}
                                                    </div>
                                                    <span class="badge bg-primary-subtle text-primary border border-primary-subtle text-xs mt-0.5 text-nowrap">
                                                        ${p.requisition.headcount} chỉ tiêu
                                                    </span>
                                                </td>
                                                <td class="text-center">
                                                    <!-- TIÊU CHÍ 1: Tin phải duyệt trước khi show công khai -->
                                                    <c:choose>
                                                        <c:when test="${p.approved}">
                                                            <span class="badge bg-success-subtle text-success border border-success-subtle px-2 py-1 text-nowrap" title="Đã duyệt (Đủ điều kiện xuất bản)">
                                                                <i class="bi bi-check-circle-fill me-1"></i>Đã duyệt (Đủ ĐK)
                                                            </span>
                                                        </c:when>
                                                        <c:when test="${p.requisition.status == 'PENDING_APPROVAL'}">
                                                            <span class="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle px-2 py-1 text-nowrap" title="Cần duyệt trước khi xuất bản">
                                                                <i class="bi bi-clock-fill me-1"></i>Chờ duyệt
                                                            </span>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="badge bg-secondary-subtle text-secondary border px-2 py-1 text-nowrap">
                                                                <i class="bi bi-dash-circle me-1"></i>${p.requisition.statusLabel}
                                                            </span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td class="text-center">
                                                    <span class="badge ${p.statusBadgeClass} px-2 py-1 text-nowrap">
                                                        <c:if test="${p.published}"><i class="bi bi-broadcast me-1"></i></c:if>
                                                        ${p.statusLabel}
                                                    </span>
                                                </td>
                                                <td>
                                                    <!-- TIÊU CHÍ 3: Ghi lại bản xuất và thời điểm -->
                                                    <c:choose>
                                                        <c:when test="${p.currentVersion > 0}">
                                                            <div class="fw-bold text-dark font-monospace text-nowrap" style="font-size: 0.78rem;">
                                                                <i class="bi bi-layers-fill text-primary me-1"></i>Bản #${p.currentVersion}
                                                            </div>
                                                            <div class="text-muted text-xs text-nowrap" title="Thời điểm xuất bản gần nhất">
                                                                <i class="bi bi-clock-history me-1"></i>${p.formattedPublishedAt}
                                                            </div>
                                                            <c:if test="${not empty p.publishedByName}">
                                                                <div class="text-muted text-xs text-nowrap">
                                                                    Bởi: <strong>${p.publishedByName}</strong>
                                                                </div>
                                                            </c:if>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <span class="text-muted text-xs italic text-nowrap">Chưa từng xuất</span>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </td>
                                                <td class="text-end pe-3">
                                                    <div class="btn-group btn-group-sm">
                                                        <!-- Nút Xem trước (Tiêu chí 2: Xem trước đúng như khai báo trước khi xuất bản) -->
                                                        <button type="button" class="btn btn-outline-info py-1 px-2" style="font-size: 0.74rem;" 
                                                                onclick="openPreviewModal('${p.requisition.id}')" 
                                                                title="Xem trước giao diện">
                                                            <i class="bi bi-eye"></i> Xem
                                                        </button>

                                                        <!-- Nút Xuất bản (Tiêu chí 1: Tin phải duyệt trước khi show công khai) -->
                                                        <c:choose>
                                                            <c:when test="${p.approved}">
                                                                <button type="button" class="btn btn-success py-1 px-2" style="font-size: 0.74rem;" 
                                                                        onclick="openPublishModal('${p.requisition.id}', '${p.requisition.code}', '${p.requisition.positionTitle}', ${p.currentVersion})" 
                                                                        title="Tiến hành xuất bản">
                                                                    <i class="bi bi-upload"></i> ${p.published ? 'Lưu' : 'Xuất'}
                                                                </button>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <button type="button" class="btn btn-outline-secondary py-1 px-2" style="font-size: 0.74rem;" 
                                                                        onclick="alertNotApproved('${p.requisition.id}', '${p.requisition.code}', '${p.requisition.statusLabel}')" 
                                                                        title="Chưa đủ điều kiện: Tin phải duyệt trước khi xuất bản">
                                                                    <i class="bi bi-lock-fill"></i> Xuất
                                                                </button>
                                                            </c:otherwise>
                                                        </c:choose>

                                                        <!-- Menu dropdown thao tác bổ sung -->
                                                        <button type="button" class="btn btn-outline-secondary dropdown-toggle dropdown-toggle-split py-1 px-1.5" style="font-size: 0.74rem;" data-bs-toggle="dropdown" aria-expanded="false">
                                                            <span class="visually-hidden">Tùy chọn</span>
                                                        </button>
                                                        <ul class="dropdown-menu dropdown-menu-end shadow border-0 py-1" style="font-size: 0.8rem;">
                                                            <!-- TIÊU CHÍ 3: Lịch sử các bản xuất và thời điểm -->
                                                            <li>
                                                                <a class="dropdown-item" href="javascript:void(0)" onclick="openHistoryModal('${p.requisition.id}', '${p.requisition.code}', '${p.requisition.positionTitle}')">
                                                                    <i class="bi bi-clock-history text-primary me-2"></i> Lịch sử bản xuất &amp; thời điểm
                                                                </a>
                                                            </li>
                                                            <c:if test="${p.approved}">
                                                                <li>
                                                                    <a class="dropdown-item text-dark" href="javascript:void(0)" onclick="openQuickCreateModal('${p.requisition.id}')">
                                                                        <i class="bi bi-lightning-charge-fill text-warning me-2"></i> Tạo nhanh bài đăng mới
                                                                    </a>
                                                                </li>
                                                                <li>
                                                                    <a class="dropdown-item" href="${pageContext.request.contextPath}/post-publishing/create?requisitionId=${p.requisition.id}">
                                                                        <i class="bi bi-pencil-square text-info me-2"></i> Màn hình tạo chi tiết
                                                                    </a>
                                                                </li>
                                                            </c:if>
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
                    <p class="mb-2">Sau khi duyệt, bài đăng này sẽ đủ điều kiện để xuất bản công khai:</p>
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

<!-- ============================================================== -->
<!-- MODAL: TẠO NHANH BÀI ĐĂNG TUYỂN DỤNG                           -->
<!-- ============================================================== -->
<div class="modal fade" id="quickJobPostingModal" tabindex="-1" aria-labelledby="quickJobPostingModalLabel" aria-hidden="true" data-bs-backdrop="static">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content shadow-lg border-0">
            <form id="quickJobPostingForm" method="POST" action="${pageContext.request.contextPath}/post-publishing/quick-create" class="d-flex flex-column h-100 overflow-hidden mb-0">
                <div class="modal-header bg-gradient bg-primary text-white py-3">
                    <div>
                        <h5 class="modal-title fw-bold mb-0" id="quickJobPostingModalLabel">
                            <i class="bi bi-lightning-charge-fill text-warning me-2"></i>Tạo nhanh Bài đăng Tuyển dụng
                        </h5>
                        <small class="text-white-50">Kế thừa và chuẩn hóa tự động từ yêu cầu tuyển dụng đã duyệt</small>
                    </div>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <!-- Error alert placeholder -->
                    <div id="quickPostErrorAlert" class="alert alert-danger py-2 px-3 small d-none mb-3">
                        <i class="bi bi-exclamation-triangle-fill me-1"></i>
                        <span id="quickPostErrorMessage"></span>
                    </div>

                    <!-- 1. Chọn yêu cầu nguồn -->
                    <div class="mb-3">
                        <label for="modalRequisitionSelect" class="form-label text-xs fw-bold text-uppercase text-dark">
                            Yêu cầu tuyển dụng nguồn (Đã duyệt) <span class="text-danger">*</span>
                        </label>
                        <select class="form-select" id="modalRequisitionSelect" name="requisitionId" required onchange="onModalRequisitionChanged(this.value)">
                            <option value="">-- Chọn yêu cầu tuyển dụng đã duyệt --</option>
                            <c:forEach var="req" items="${approvedRequisitions}">
                                <option value="${req.id}">
                                    [${req.code}] ${req.positionTitle} - ${req.departmentName} (${req.statusLabel})
                                </option>
                            </c:forEach>
                        </select>
                    </div>

                    <!-- Thông tin tóm tắt yêu cầu nguồn -->
                    <div id="modalReqSummaryBox" class="p-3 bg-light rounded border mb-3 d-none">
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <span class="badge bg-success-subtle text-success border border-success-subtle">
                                <i class="bi bi-check-circle-fill me-1"></i><span id="mSumStatus">Đã duyệt</span>
                            </span>
                            <span class="text-xs text-muted font-monospace" id="mSumCode"></span>
                        </div>
                        <h6 class="fw-bold text-dark mb-1" id="mSumTitle"></h6>
                        <div class="row g-2 text-xs text-secondary mt-1">
                            <div class="col-sm-6"><i class="bi bi-building me-1 text-primary"></i>Phòng ban: <strong class="text-dark" id="mSumDept"></strong></div>
                            <div class="col-sm-6"><i class="bi bi-people me-1 text-primary"></i>Chỉ tiêu: <strong class="text-dark" id="mSumHeadcount"></strong></div>
                            <div class="col-sm-6"><i class="bi bi-cash me-1 text-primary"></i>Dải lương gốc: <strong class="text-dark" id="mSumSalary"></strong></div>
                            <div class="col-sm-6"><i class="bi bi-calendar-event me-1 text-primary"></i>Hạn nhận gốc: <strong class="text-dark" id="mSumDeadline"></strong></div>
                        </div>
                    </div>

                    <!-- 2. Tiêu đề bài đăng -->
                    <div class="mb-3">
                        <label for="mPostTitle" class="form-label text-xs fw-bold text-uppercase text-dark">
                            Tiêu đề bài đăng tuyển dụng <span class="text-danger">*</span>
                        </label>
                        <input type="text" class="form-control fw-semibold" id="mPostTitle" name="title" required placeholder="Nhập tiêu đề bài đăng..." />
                    </div>

                    <!-- 3. Mô tả công việc (Kế thừa và chuẩn hóa) -->
                    <div class="mb-3">
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <label for="mPostDescription" class="form-label text-xs fw-bold text-uppercase text-dark mb-0">
                                Mô tả công việc (JD) &amp; Yêu cầu <span class="text-danger">*</span>
                            </label>
                            <button type="button" class="btn btn-outline-primary btn-sm py-0 px-2 text-xs" onclick="reNormalizeModalDescription()">
                                <i class="bi bi-magic me-1"></i>Chuẩn hóa lại JD
                            </button>
                        </div>
                        <textarea class="form-control font-monospace" id="mPostDescription" name="jobDescription" rows="8" style="font-size: 0.88rem; line-height: 1.5;" required placeholder="Nội dung JD kế thừa tự động từ yêu cầu tuyển dụng..."></textarea>
                        <div class="d-flex justify-content-between align-items-center mt-1">
                            <small class="text-muted">Chỉnh sửa JD tại đây độc lập, không làm thay đổi nội dung yêu cầu gốc.</small>
                            <small id="mJdCharCount" class="text-muted">0 ký tự</small>
                        </div>
                    </div>

                    <!-- 4. Địa điểm, hình thức & hạn nhận hồ sơ -->
                    <div class="row g-3 mb-3">
                        <div class="col-md-6">
                            <label for="mWorkLocation" class="form-label text-xs fw-bold text-uppercase text-dark">
                                Địa điểm làm việc <span class="text-danger">*</span>
                            </label>
                            <input type="text" class="form-control" id="mWorkLocation" name="workLocation" required placeholder="Ví dụ: Hà Nội, TP. Hồ Chí Minh..." list="mWorkLocationsList" />
                            <datalist id="mWorkLocationsList">
                                <c:forEach var="loc" items="${workLocations}">
                                    <option value="${loc.value}"></option>
                                </c:forEach>
                            </datalist>
                        </div>
                        <div class="col-md-6">
                            <label for="mWorkMode" class="form-label text-xs fw-bold text-uppercase text-dark">
                                Hình thức làm việc <span class="text-danger">*</span>
                            </label>
                            <input type="text" class="form-control" id="mWorkMode" name="workMode" required placeholder="Ví dụ: Toàn thời gian, Hybrid..." list="mWorkModesList" />
                            <datalist id="mWorkModesList">
                                <c:forEach var="mode" items="${workModes}">
                                    <option value="${mode.value}"></option>
                                </c:forEach>
                            </datalist>
                        </div>
                        <div class="col-md-12">
                            <label for="mApplicationDeadline" class="form-label text-xs fw-bold text-uppercase text-dark">
                                Hạn nhận hồ sơ <span class="text-danger">*</span>
                            </label>
                            <input type="date" class="form-control" id="mApplicationDeadline" name="applicationDeadline" required />
                            <div class="form-text small text-muted">Hạn nhận hồ sơ phải từ hôm nay trở đi.</div>
                        </div>
                    </div>

                    <!-- 5. Cấu hình Mức lương (KN-204, KN-205) -->
                    <div class="p-3 bg-light rounded border mb-3">
                        <div class="form-check form-switch mb-2">
                            <input class="form-check-input" type="checkbox" role="switch" id="mSalaryVisibleSwitch" name="salaryVisible" value="true" onchange="toggleModalSalaryVisibility(this.checked)">
                            <label class="form-check-label fw-bold text-dark" for="mSalaryVisibleSwitch">
                                Hiển thị mức lương trên bài đăng
                            </label>
                        </div>
                        <div id="mSalaryRangeSection" class="row g-2 d-none">
                            <div class="col-sm-6">
                                <label for="mMinSalary" class="form-label text-xs fw-semibold text-muted text-uppercase">Lương tối thiểu (VND):</label>
                                <input type="number" step="100000" min="0" class="form-control form-control-sm" id="mMinSalary" name="minSalary" placeholder="0" />
                            </div>
                            <div class="col-sm-6">
                                <label for="mMaxSalary" class="form-label text-xs fw-semibold text-muted text-uppercase">Lương tối đa (VND):</label>
                                <input type="number" step="100000" min="0" class="form-control form-control-sm" id="mMaxSalary" name="maxSalary" placeholder="Không giới hạn" />
                            </div>
                        </div>
                        <div id="mSalaryHiddenNotice" class="text-xs text-muted">
                            <i class="bi bi-shield-lock me-1"></i>Mức lương sẽ được ẩn trên bài đăng và hiển thị <strong>"Thỏa thuận"</strong> đối với ứng viên.
                        </div>
                    </div>

                    <!-- 6. Tùy chọn xuất bản ngay -->
                    <div class="form-check form-switch mb-0">
                        <input class="form-check-input" type="checkbox" role="switch" id="mPublishImmediately" name="publishImmediately" value="true">
                        <label class="form-check-label fw-semibold text-dark" for="mPublishImmediately">
                            Xuất bản công khai ngay sau khi tạo
                        </label>
                    </div>
                </div>
                <div class="modal-footer py-2 bg-light d-flex justify-content-between">
                    <div>
                        <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Đóng</button>
                        <a href="${pageContext.request.contextPath}/post-publishing/create" id="mOpenFullPageBtn" class="btn btn-link btn-sm text-decoration-none">
                            <i class="bi bi-box-arrow-up-right me-1"></i>Mở màn hình đầy đủ
                        </a>
                    </div>
                    <button type="submit" class="btn btn-success btn-sm px-4 fw-bold" id="mSubmitBtn">
                        <i class="bi bi-check2-circle me-1"></i>Tạo bài đăng
                    </button>
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
    var msg = 'Tin phải được phê duyệt trước khi xuất bản công khai! Vị trí ' + code + ' hiện đang ở trạng thái: ' + statusLabel + '.';
    if (window.showToast) {
        window.showToast(msg, 'warning', 'Chưa đủ điều kiện xuất bản', 6000);
    } else {
        alert(msg + '\nVui lòng gửi phê duyệt hoặc duyệt tin trước khi xuất bản.');
    }
}

// 4. Mở Modal Lịch sử các bản xuất và thời điểm
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

// ==============================================================
// LOGIC MODAL TẠO NHANH BÀI ĐĂNG TUYỂN DỤNG (KN-204, KN-206, KN-207)
// ==============================================================
let quickModalInstance = null;

function openQuickCreateModal(reqId) {
    if (!quickModalInstance) {
        quickModalInstance = new bootstrap.Modal(document.getElementById('quickJobPostingModal'));
    }

    // Reset error & form
    document.getElementById('quickPostErrorAlert').classList.add('d-none');
    document.getElementById('quickPostErrorMessage').innerText = '';
    const form = document.getElementById('quickJobPostingForm');
    form.reset();

    const today = new Date().toISOString().split('T')[0];
    document.getElementById('mApplicationDeadline').setAttribute('min', today);

    const sel = document.getElementById('modalRequisitionSelect');
    if (reqId) {
        sel.value = reqId;
        onModalRequisitionChanged(reqId);
        document.getElementById('mOpenFullPageBtn').href = ctx + '/post-publishing/create?requisitionId=' + encodeURIComponent(reqId);
    } else {
        document.getElementById('modalReqSummaryBox').classList.add('d-none');
        document.getElementById('mOpenFullPageBtn').href = ctx + '/post-publishing/create';
    }

    quickModalInstance.show();
}

function updateModalCharCount() {
    const el = document.getElementById('mPostDescription');
    const cnt = document.getElementById('mJdCharCount');
    if (el && cnt) {
        cnt.innerText = el.value.length + ' ký tự';
    }
}
document.getElementById('mPostDescription').addEventListener('input', updateModalCharCount);

function toggleModalSalaryVisibility(visible) {
    const sec = document.getElementById('mSalaryRangeSection');
    const noti = document.getElementById('mSalaryHiddenNotice');
    if (visible) {
        sec.classList.remove('d-none');
        noti.classList.add('d-none');
    } else {
        sec.classList.add('d-none');
        noti.classList.remove('d-none');
        document.getElementById('mMinSalary').value = '';
        document.getElementById('mMaxSalary').value = '';
    }
}

function reNormalizeModalDescription() {
    const el = document.getElementById('mPostDescription');
    if (!el || !el.value) return;
    let text = el.value;
    text = text.replace(/^[\s]*[•\*\+\–\—]\s+/gm, '- ');
    text = text.replace(/\n{3,}/g, '\n\n');
    el.value = text.trim();
    updateModalCharCount();
}

function onModalRequisitionChanged(reqId) {
    const sumBox = document.getElementById('modalReqSummaryBox');
    const errAlert = document.getElementById('quickPostErrorAlert');
    errAlert.classList.add('d-none');

    if (!reqId) {
        sumBox.classList.add('d-none');
        document.getElementById('mOpenFullPageBtn').href = ctx + '/post-publishing/create';
        return;
    }

    document.getElementById('mOpenFullPageBtn').href = ctx + '/post-publishing/create?requisitionId=' + encodeURIComponent(reqId);

    fetch(ctx + '/post-publishing/requisition-data?id=' + encodeURIComponent(reqId))
        .then(res => res.json())
        .then(data => {
            if (data.error) {
                errAlert.classList.remove('d-none');
                document.getElementById('quickPostErrorMessage').innerText = data.error;
                return;
            }

            sumBox.classList.remove('d-none');
            document.getElementById('mSumStatus').innerText = data.statusLabel || 'Đã duyệt';
            document.getElementById('mSumCode').innerText = data.code || '';
            document.getElementById('mSumTitle').innerText = data.positionTitle || data.title || '';
            document.getElementById('mSumDept').innerText = data.departmentName || '';
            document.getElementById('mSumHeadcount').innerText = (data.headcount || 1) + ' người';
            document.getElementById('mSumSalary').innerText = data.formattedSalaryRange || 'Thỏa thuận';
            document.getElementById('mSumDeadline').innerText = data.deadlineFormatted || 'Chưa đặt';

            // Tiêu đề bài đăng
            document.getElementById('mPostTitle').value = data.title || data.positionTitle || '';

            // Kế thừa và chuẩn hóa JD (KN-206)
            document.getElementById('mPostDescription').value = data.normalizedJobDescription || data.rawJobDescription || '';
            updateModalCharCount();

            // Địa điểm & hình thức
            if (data.workLocation) document.getElementById('mWorkLocation').value = data.workLocation;
            else document.getElementById('mWorkLocation').value = 'Hà Nội, Việt Nam';

            if (data.workMode) document.getElementById('mWorkMode').value = data.workMode;
            else document.getElementById('mWorkMode').value = 'Toàn thời gian';

            // Hạn nhận
            if (data.deadline) document.getElementById('mApplicationDeadline').value = data.deadline;

            // Lương
            const salarySwitch = document.getElementById('mSalaryVisibleSwitch');
            if (data.salaryVisible && (data.minSalary || data.maxSalary)) {
                salarySwitch.checked = true;
                toggleModalSalaryVisibility(true);
                if (data.minSalary) document.getElementById('mMinSalary').value = data.minSalary;
                if (data.maxSalary) document.getElementById('mMaxSalary').value = data.maxSalary;
            } else {
                salarySwitch.checked = false;
                toggleModalSalaryVisibility(false);
            }
        })
        .catch(err => {
            errAlert.classList.remove('d-none');
            document.getElementById('quickPostErrorMessage').innerText = 'Không thể tải thông tin yêu cầu tuyển dụng: ' + err;
        });
}

// Submit xử lý AJAX với kiểm tra hợp lệ backend/frontend
document.getElementById('quickJobPostingForm').addEventListener('submit', function(e) {
    e.preventDefault();

    const errAlert = document.getElementById('quickPostErrorAlert');
    const errMsgSpan = document.getElementById('quickPostErrorMessage');
    errAlert.classList.add('d-none');

    const reqId = document.getElementById('modalRequisitionSelect').value;
    if (!reqId) {
        const msg = 'Vui lòng chọn yêu cầu tuyển dụng nguồn đã được phê duyệt!';
        errAlert.classList.remove('d-none');
        errMsgSpan.innerText = msg;
        if (window.showToast) window.showToast(msg, 'warning', 'Chưa chọn yêu cầu');
        return;
    }

    const title = document.getElementById('mPostTitle').value.trim();
    if (!title) {
        const msg = 'Vui lòng nhập tiêu đề bài đăng!';
        errAlert.classList.remove('d-none');
        errMsgSpan.innerText = msg;
        if (window.showToast) window.showToast(msg, 'warning', 'Thiếu tiêu đề bài đăng');
        return;
    }

    const jd = document.getElementById('mPostDescription').value.trim();
    if (!jd) {
        const msg = 'Vui lòng nhập mô tả công việc (JD)!';
        errAlert.classList.remove('d-none');
        errMsgSpan.innerText = msg;
        if (window.showToast) window.showToast(msg, 'warning', 'Thiếu mô tả công việc');
        return;
    }

    const deadline = document.getElementById('mApplicationDeadline').value;
    if (deadline) {
        const selected = new Date(deadline);
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        if (selected < today) {
            const msg = 'Hạn nhận hồ sơ không thể ở trong quá khứ!';
            errAlert.classList.remove('d-none');
            errMsgSpan.innerText = msg;
            if (window.showToast) window.showToast(msg, 'warning', 'Hạn nhận hồ sơ không hợp lệ');
            return;
        }
    }

    const isSalaryVisible = document.getElementById('mSalaryVisibleSwitch').checked;
    if (isSalaryVisible) {
        const minVal = parseFloat(document.getElementById('mMinSalary').value);
        const maxVal = parseFloat(document.getElementById('mMaxSalary').value);
        if (!isNaN(minVal) && minVal < 0) {
            const msg = 'Mức lương tối thiểu không được là số âm!';
            errAlert.classList.remove('d-none');
            errMsgSpan.innerText = msg;
            if (window.showToast) window.showToast(msg, 'warning', 'Lương không hợp lệ');
            return;
        }
        if (!isNaN(maxVal) && maxVal < 0) {
            const msg = 'Mức lương tối đa không được là số âm!';
            errAlert.classList.remove('d-none');
            errMsgSpan.innerText = msg;
            if (window.showToast) window.showToast(msg, 'warning', 'Lương không hợp lệ');
            return;
        }
        if (!isNaN(minVal) && !isNaN(maxVal) && minVal > maxVal) {
            const msg = 'Mức lương tối thiểu không được lớn hơn mức lương tối đa!';
            errAlert.classList.remove('d-none');
            errMsgSpan.innerText = msg;
            if (window.showToast) window.showToast(msg, 'warning', 'Dải lương không hợp lệ');
            return;
        }
    }

    const submitBtn = document.getElementById('mSubmitBtn');
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang tạo...';

    const formData = new URLSearchParams(new FormData(this));

    fetch(ctx + '/post-publishing/quick-create', {
        method: 'POST',
        headers: {
            'X-Requested-With': 'XMLHttpRequest',
            'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
        },
        body: formData.toString()
    })
    .then(res => res.json())
    .then(data => {
        submitBtn.disabled = false;
        submitBtn.innerHTML = '<i class="bi bi-check2-circle me-1"></i>Tạo bài đăng';

        if (!data.success) {
            const errMsg = data.error || 'Có lỗi xảy ra khi tạo bài đăng!';
            errAlert.classList.remove('d-none');
            errMsgSpan.innerText = errMsg;
            if (window.showToast) {
                window.showToast(errMsg, 'error', 'Lỗi tạo bài đăng', 6500);
            }
        } else {
            const successMsg = data.message || 'Tạo bài đăng thành công!';
            if (window.showToast) {
                window.showToast(successMsg, 'success', 'Thành công', 5000);
            }
            if (quickModalInstance) {
                quickModalInstance.hide();
            }
            setTimeout(() => {
                window.location.href = data.redirectUrl || (ctx + '/post-publishing');
            }, 800);
        }
    })
    .catch(err => {
        submitBtn.disabled = false;
        submitBtn.innerHTML = '<i class="bi bi-check2-circle me-1"></i>Tạo bài đăng';
        const errMsg = 'Lỗi kết nối máy chủ: ' + err;
        errAlert.classList.remove('d-none');
        errMsgSpan.innerText = errMsg;
        if (window.showToast) {
            window.showToast(errMsg, 'error', 'Lỗi kết nối');
        }
    });
});
</script>

<jsp:include page="../common/footer.jsp" />

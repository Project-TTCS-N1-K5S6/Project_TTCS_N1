<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Quản lý Yêu cầu tuyển dụng" />
<c:set var="breadcrumb" value="Yêu cầu tuyển dụng" />
<c:set var="activeMenu" value="requisitions" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<style>
.bg-teal-custom { background-color: #0d9488 !important; color: #fff !important; }
.status-option-card {
    border: 2px solid #e2e8f0;
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.2s ease-in-out;
}
.status-option-card:hover {
    border-color: #cbd5e1;
    background-color: #f8fafc;
}
.status-radio:checked + .status-option-card {
    border-color: #0d6efd;
    background-color: #f0f7ff;
    box-shadow: 0 4px 6px -1px rgba(13, 110, 253, 0.15);
}
.status-radio-fulfilled:checked + .status-option-card {
    border-color: #0d9488;
    background-color: #f0fdfa;
    box-shadow: 0 4px 6px -1px rgba(13, 148, 136, 0.15);
}
.status-radio-paused:checked + .status-option-card {
    border-color: #f59e0b;
    background-color: #fffbeb;
    box-shadow: 0 4px 6px -1px rgba(245, 158, 11, 0.15);
}
.status-radio-cancelled:checked + .status-option-card {
    border-color: #ef4444;
    background-color: #fef2f2;
    box-shadow: 0 4px 6px -1px rgba(239, 68, 68, 0.15);
}
</style>

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Header phân hệ -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <h4 class="fw-bold mb-1">
                    <i class="bi bi-file-earmark-text text-primary me-2"></i>Yêu cầu tuyển dụng
                </h4>
                <p class="text-muted small mb-0">
                    Quản lý toàn diện yêu cầu tuyển dụng, đóng/kết thúc vị trí, tự động gỡ tin và kiểm soát ứng viên pipeline.
                </p>
            </div>
            <div class="d-flex align-items-center gap-2">
                <a href="${pageContext.request.contextPath}/post-publishing" class="btn btn-outline-success shadow-sm">
                    <i class="bi bi-broadcast me-1"></i> Quản lý Xuất bản bài đăng
                </a>
                <a href="${pageContext.request.contextPath}/recruitment-requests/create" class="btn btn-primary shadow-sm">
                    <i class="bi bi-plus-circle-fill me-1"></i> Tạo yêu cầu tuyển dụng
                </a>
            </div>
        </div>

        <!-- Bộ lọc trạng thái & tìm kiếm -->
        <div class="card mb-3 border shadow-sm">
            <div class="card-body py-3">
                <form method="get" action="${pageContext.request.contextPath}/recruitment-requests" class="row g-2 align-items-center">
                    <div class="col-12 col-md-4">
                        <div class="input-group input-group-sm">
                            <span class="input-group-text bg-white"><i class="bi bi-search text-muted"></i></span>
                            <input type="text" name="search" class="form-control" placeholder="Tìm theo mã, chức danh, tiêu đề..." value="${paramSearch}">
                        </div>
                    </div>
                    <div class="col-12 col-md-3">
                        <select name="departmentId" class="form-select form-select-sm">
                            <option value="">-- Tất cả phòng ban --</option>
                            <c:forEach var="dept" items="${departments}">
                                <option value="${dept.id}" ${paramDept == dept.id ? 'selected' : ''}>${dept.name}</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-12 col-md-3">
                        <select name="status" class="form-select form-select-sm">
                            <option value="">-- Tất cả trạng thái --</option>
                            <option value="OPEN" ${paramStatus == 'OPEN' ? 'selected' : ''}>Đang tuyển dụng</option>
                            <option value="FULFILLED" ${paramStatus == 'FULFILLED' ? 'selected' : ''}>Đã tuyển đủ</option>
                            <option value="PAUSED" ${paramStatus == 'PAUSED' ? 'selected' : ''}>Tạm dừng</option>
                            <option value="CANCELLED" ${paramStatus == 'CANCELLED' ? 'selected' : ''}>Đã huỷ</option>
                            <option value="CLOSED_ALL" ${paramStatus == 'CLOSED_ALL' ? 'selected' : ''}>Tất cả đã đóng / kết thúc</option>
                            <option value="PENDING_APPROVAL" ${paramStatus == 'PENDING_APPROVAL' ? 'selected' : ''}>Chờ phê duyệt</option>
                            <option value="DRAFT" ${paramStatus == 'DRAFT' ? 'selected' : ''}>Lưu nháp (Draft)</option>
                        </select>
                    </div>
                    <div class="col-12 col-md-2 d-flex gap-2">
                        <button type="submit" class="btn btn-sm btn-dark flex-grow-1">
                            <i class="bi bi-filter me-1"></i> Lọc
                        </button>
                        <a href="${pageContext.request.contextPath}/recruitment-requests" class="btn btn-sm btn-outline-secondary" title="Đặt lại bộ lọc">
                            <i class="bi bi-arrow-clockwise"></i>
                        </a>
                    </div>
                </form>
            </div>
        </div>

        <!-- Bảng danh sách yêu cầu tuyển dụng -->
        <div class="card mb-4 shadow-sm border">
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-custom table-hover align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th style="width: 130px;">Mã yêu cầu</th>
                                <th>Chức danh &amp; Tiêu đề</th>
                                <th>Phòng ban</th>
                                <th class="text-center" style="width: 90px;">SL tuyển</th>
                                <th>Lý do tuyển</th>
                                <th>Dải lương đề xuất</th>
                                <th>Ngày cần người</th>
                                <th class="text-center" style="width: 140px;">Trạng thái</th>
                                <th class="text-end pe-3" style="width: 220px;">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${not empty requisitions}">
                                    <c:forEach var="r" items="${requisitions}">
                                        <tr>
                                            <td>
                                                <span class="badge bg-light text-dark border font-monospace px-2 py-1">${r.code}</span>
                                                <div class="text-xs text-muted mt-1">
                                                    <fmt:formatDate value="${r.createdAt}" pattern="dd/MM/yyyy" />
                                                </div>
                                            </td>
                                            <td>
                                                <div class="fw-bold text-dark d-flex align-items-center gap-1">
                                                    ${r.positionTitle}
                                                    <c:if test="${r.activeCandidatesCount > 0}">
                                                        <span class="badge bg-info-subtle text-info border border-info-subtle ms-1" 
                                                              style="font-size: 11px;" 
                                                              title="Hiện có ${r.activeCandidatesCount} ứng viên đang trong pipeline">
                                                            <i class="bi bi-people-fill me-1"></i>${r.activeCandidatesCount}
                                                        </span>
                                                    </c:if>
                                                </div>
                                                <small class="text-muted text-truncate d-inline-block" style="max-width: 260px;">${r.title}</small>
                                            </td>
                                            <td>
                                                <span class="fw-semibold text-secondary">
                                                    <i class="bi bi-building me-1 small text-muted"></i>${r.departmentName != null ? r.departmentName : 'Chưa gán'}
                                                </span>
                                            </td>
                                            <td class="text-center">
                                                <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-2 py-1">
                                                    ${r.headcount} người
                                                </span>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${r.recruitmentReason == 'REPLACEMENT'}">
                                                        <span class="badge bg-warning-subtle text-dark border border-warning-subtle">
                                                            <i class="bi bi-arrow-repeat me-1"></i>Thay thế
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-success-subtle text-success border border-success-subtle">
                                                            <i class="bi bi-person-plus me-1"></i>Tăng mới
                                                        </span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <div class="fw-semibold text-dark">${r.formattedSalaryRange}</div>
                                                <c:if test="${not empty r.salaryExplanation}">
                                                    <span class="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle text-xs" title="Đã có văn bản giải trình lương vượt khung chuẩn">
                                                        <i class="bi bi-exclamation-triangle-fill me-1 text-warning"></i>Có giải trình
                                                    </span>
                                                </c:if>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${r.deadline != null}">
                                                        <span class="text-dark fw-semibold">
                                                            <i class="bi bi-calendar-event me-1 text-muted"></i>
                                                            <fmt:formatDate value="${r.deadline}" pattern="dd/MM/yyyy" />
                                                        </span>
                                                    <c:otherwise><span class="text-muted">Chưa đặt</span></c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td class="text-center">
                                                <div class="cursor-pointer" onclick="openApprovalModal('${r.id}')" title="Nhấp để xem chuỗi cấp duyệt & Lịch sử" style="cursor: pointer;">
                                                <c:choose>
                                                    <c:when test="${r.status == 'OPEN'}">
                                                        <span class="badge bg-success text-white px-2 py-1">
                                                            <i class="bi bi-broadcast me-1"></i>Đang tuyển
                                                        </span>
                                                    </c:when>
                                                    <c:when test="${r.status == 'FULFILLED' || r.closeStatus == 'FULFILLED'}">
                                                        <span class="badge bg-teal-custom px-2 py-1" title="${r.closeReason}">
                                                            <i class="bi bi-check-circle-fill me-1"></i>Đã tuyển đủ
                                                        </span>
                                                        <div class="text-xs text-muted mt-1 text-truncate" style="max-width: 130px;" title="${r.closeReason}">
                                                            ${r.closeReason}
                                                        </div>
                                                    </c:when>
                                                    <c:when test="${r.status == 'PAUSED' || r.closeStatus == 'PAUSED'}">
                                                        <span class="badge bg-warning text-dark px-2 py-1" title="${r.closeReason}">
                                                            <i class="bi bi-pause-circle-fill me-1"></i>Tạm dừng
                                                        </span>
                                                        <div class="text-xs text-muted mt-1 text-truncate" style="max-width: 130px;" title="${r.closeReason}">
                                                            ${r.closeReason}
                                                        </div>
                                                    </c:when>
                                                    <c:when test="${r.status == 'CANCELLED' || r.closeStatus == 'CANCELLED'}">
                                                        <span class="badge bg-danger text-white px-2 py-1" title="${r.closeReason}">
                                                            <i class="bi bi-x-circle-fill me-1"></i>Đã huỷ
                                                        </span>
                                                        <div class="text-xs text-muted mt-1 text-truncate" style="max-width: 130px;" title="${r.closeReason}">
                                                            ${r.closeReason}
                                                        </div>
                                                    </c:when>
                                                    <c:when test="${r.status == 'CLOSED'}">
                                                        <span class="badge bg-dark text-white px-2 py-1" title="${r.closeReason}">
                                                            <i class="bi bi-archive-fill me-1"></i>Đã đóng
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge ${r.statusBadgeClass} px-2 py-1">${r.statusLabel}</span>
                                                    </c:otherwise>
                                                </c:choose>
                                                </div>
                                                <c:if test="${r.status != 'DRAFT'}">
                                                    <div class="mt-1">
                                                        <a href="javascript:void(0)" onclick="openApprovalModal('${r.id}')" class="text-decoration-none small text-muted text-nowrap" style="font-size: 0.70rem;" title="Xem chi tiết tiến trình phê duyệt">
                                                            <i class="bi bi-diagram-3-fill text-primary me-0.5"></i>Chuỗi duyệt
                                                        </a>
                                                    </div>
                                                </c:if>
                                            </td>
                                            <td class="text-end pe-3">
                                                <div class="btn-group btn-group-sm">
                                                    <!-- Nhật ký & Trạng thái chuỗi duyệt -->
                                                    <button type="button" class="btn btn-outline-info" 
                                                            onclick="openApprovalModal('${r.id}')"
                                                            title="Nhật ký & Trạng thái chuỗi duyệt">
                                                        <i class="bi bi-diagram-3"></i>
                                                    </button>
                                                    <!-- Xem chi tiết -->
                                                    <button type="button" class="btn btn-outline-secondary" 
                                                            onclick="viewDetails('${r.id}', '${r.code}', '${r.positionTitle}', '${r.departmentName}', '${r.headcount}', '${r.recruitmentReasonLabel}', '${r.formattedSalaryRange}', '${r.deadline}', '${r.statusLabel}', '${r.status}', '${r.closeStatus}', '${r.closeReason}', '${r.closeNotes}', '${r.closedByName}', '${r.closedAt}')"
                                                            title="Xem chi tiết JD & Yêu cầu">
                                                        <i class="bi bi-eye"></i>
                                                    </button>
                                                    <c:if test="${not empty r.jobDescription or not empty r.jobRequirements}">
                                                        <a href="${pageContext.request.contextPath}/recruitment-requests/create?copyFrom=${r.id}"
                                                           class="btn btn-outline-success" title="KN-115: Sao chép JD và yêu cầu thành bản nháp">
                                                            <i class="bi bi-copy"></i>
                                                        </a>
                                                    </c:if>
                                                    <!-- Chỉnh sửa khi chưa kết thúc -->
                                                    <c:if test="${!r.terminal}">
                                                        <a href="${pageContext.request.contextPath}/recruitment-requests/edit?id=${r.id}" 
                                                           class="btn btn-outline-primary" title="Chỉnh sửa yêu cầu">
                                                            <i class="bi bi-pencil"></i>
                                                        </a>
                                                    </c:if>
                                                    <!-- Quản lý xuất bản tin tuyển dụng -->
                                                    <a href="${pageContext.request.contextPath}/post-publishing?search=${r.code}" 
                                                       class="btn btn-outline-success" title="Quản lý xuất bản bài đăng (KN-120)">
                                                        <i class="bi bi-broadcast"></i>
                                                    </a>
                                                    <!-- Đóng / Kết thúc yêu cầu khi đang mở hoặc chờ duyệt -->
                                                    <c:if test="${!r.terminal && r.status != 'DRAFT'}">
                                                        <button type="button" class="btn btn-outline-danger" 
                                                                onclick="openCloseModal('${r.id}', '${r.code}', '${r.positionTitle}', ${r.activeCandidatesCount}, ${r.activeJobPostingsCount})"
                                                                title="Đóng / Kết thúc yêu cầu (Đã tuyển đủ, Tạm dừng, Huỷ)">
                                                            <i class="bi bi-stop-circle-fill"></i> Đóng
                                                        </button>
                                                    </c:if>
                                                </div>
                                                <!-- Hidden data for modal view -->
                                                <div id="jd-${r.id}" style="display:none;"><c:out value="${r.jobDescription}" /></div>
                                                <div id="req-${r.id}" style="display:none;"><c:out value="${r.jobRequirements}" /></div>
                                                <div id="exp-${r.id}" style="display:none;"><c:out value="${r.salaryExplanation}" /></div>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <tr>
                                        <td colspan="9" class="text-center py-5 text-muted">
                                            <i class="bi bi-inbox fs-1 d-block mb-2 text-secondary opacity-50"></i>
                                            Chưa có yêu cầu tuyển dụng nào phù hợp bộ lọc.
                                            <div class="mt-3">
                                                <a href="${pageContext.request.contextPath}/recruitment-requests/create" class="btn btn-sm btn-primary">
                                                    <i class="bi bi-plus-lg me-1"></i> Tạo yêu cầu mới ngay
                                                </a>
                                            </div>
                                        </td>
                                    </tr>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
                <!-- Phân trang phía Server (Backend Pagination) -->
                <c:if test="${totalPages > 1}">
                    <div class="p-3 border-top d-flex flex-wrap justify-content-between align-items-center gap-2">
                        <small class="text-muted">Hiển thị trang <strong>${currentPage}</strong> / <strong>${totalPages}</strong> (Tổng cộng <strong>${totalCount}</strong> yêu cầu tuyển dụng)</small>
                        <nav>
                            <ul class="pagination pagination-sm mb-0">
                                <li class="page-item ${currentPage <= 1 ? 'disabled' : ''}">
                                    <a class="page-link" href="${pageContext.request.contextPath}/recruitment-requests?page=${currentPage - 1}&search=${paramSearch}&departmentId=${paramDept}&status=${paramStatus}" aria-label="Trang trước">
                                        <i class="bi bi-chevron-left"></i>
                                    </a>
                                </li>
                                <c:forEach begin="1" end="${totalPages}" var="p">
                                    <li class="page-item ${currentPage == p ? 'active' : ''}">
                                        <a class="page-link" href="${pageContext.request.contextPath}/recruitment-requests?page=${p}&search=${paramSearch}&departmentId=${paramDept}&status=${paramStatus}">${p}</a>
                                    </li>
                                </c:forEach>
                                <li class="page-item ${currentPage >= totalPages ? 'disabled' : ''}">
                                    <a class="page-link" href="${pageContext.request.contextPath}/recruitment-requests?page=${currentPage + 1}&search=${paramSearch}&departmentId=${paramDept}&status=${paramStatus}" aria-label="Trang sau">
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
</main>

<!-- Modal Đóng / Kết thúc Yêu cầu tuyển dụng -->
<div class="modal fade" id="closeModal" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content border-0 shadow">
            <div class="modal-header bg-light border-bottom">
                <div>
                    <h5 class="modal-title fw-bold text-dark mb-0">
                        <i class="bi bi-stop-circle-fill text-danger me-2"></i>Đóng Yêu cầu Tuyển dụng
                    </h5>
                    <small class="text-muted" id="closeModalSubtitle">Mã yêu cầu: REQ-XXXX</small>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            
            <form id="closeRequisitionForm" method="post" action="${pageContext.request.contextPath}/recruitment-requests/close">
                <input type="hidden" name="id" id="closeReqId" value="" />
                
                <div class="modal-body p-4">
                    <!-- 1. BƯỚC 1: Chọn một trong 3 trạng thái kết thúc -->
                    <label class="form-label fw-bold text-dark mb-2">
                        1. Chọn trạng thái kết thúc <span class="text-danger">*</span>
                    </label>
                    <div class="row g-3 mb-4">
                        <!-- Trạng thái 1: Đã tuyển đủ -->
                        <div class="col-md-4">
                            <label class="w-100 mb-0">
                                <input type="radio" name="terminalStatus" value="FULFILLED" class="d-none status-radio status-radio-fulfilled" checked onchange="onStatusChanged()" />
                                <div class="status-option-card p-3 h-100 text-center">
                                    <div class="mb-2">
                                        <span class="badge bg-teal-custom p-2 rounded-circle">
                                            <i class="bi bi-check-lg fs-5"></i>
                                        </span>
                                    </div>
                                    <h6 class="fw-bold text-dark mb-1">Đã tuyển đủ</h6>
                                    <small class="text-muted d-block">Đạt đủ chỉ tiêu tuyển dụng số lượng người.</small>
                                </div>
                            </label>
                        </div>
                        <!-- Trạng thái 2: Tạm dừng -->
                        <div class="col-md-4">
                            <label class="w-100 mb-0">
                                <input type="radio" name="terminalStatus" value="PAUSED" class="d-none status-radio status-radio-paused" onchange="onStatusChanged()" />
                                <div class="status-option-card p-3 h-100 text-center">
                                    <div class="mb-2">
                                        <span class="badge bg-warning p-2 rounded-circle text-dark">
                                            <i class="bi bi-pause-fill fs-5"></i>
                                        </span>
                                    </div>
                                    <h6 class="fw-bold text-dark mb-1">Tạm dừng</h6>
                                    <small class="text-muted d-block">Tạm dừng kế hoạch tuyển do ngân sách/quý.</small>
                                </div>
                            </label>
                        </div>
                        <!-- Trạng thái 3: Huỷ -->
                        <div class="col-md-4">
                            <label class="w-100 mb-0">
                                <input type="radio" name="terminalStatus" value="CANCELLED" class="d-none status-radio status-radio-cancelled" onchange="onStatusChanged()" />
                                <div class="status-option-card p-3 h-100 text-center">
                                    <div class="mb-2">
                                        <span class="badge bg-danger p-2 rounded-circle">
                                            <i class="bi bi-x-lg fs-5"></i>
                                        </span>
                                    </div>
                                    <h6 class="fw-bold text-dark mb-1">Huỷ yêu cầu</h6>
                                    <small class="text-muted d-block">Huỷ bỏ vị trí do thay đổi chiến lược.</small>
                                </div>
                            </label>
                        </div>
                    </div>

                    <!-- 2. BƯỚC 2: Lý do riêng tương ứng của từng trạng thái -->
                    <div class="mb-4 p-3 bg-light rounded border">
                        <label for="closeReasonSelect" class="form-label fw-bold text-dark mb-1">
                            2. Lý do kết thúc cụ thể <span class="text-danger">*</span>
                        </label>
                        <select class="form-select mb-2" id="closeReasonSelect" onchange="onReasonSelectChanged()">
                            <!-- Tự động cập nhật qua JS theo trạng thái -->
                        </select>
                        <input type="hidden" name="closeReason" id="finalCloseReason" value="" />
                        
                        <div id="customReasonWrapper" style="display: none;">
                            <input type="text" class="form-control mb-2" id="customReasonInput" 
                                   placeholder="Nhập lý do kết thúc chi tiết của bạn..." />
                        </div>

                        <label for="closeNotes" class="form-label text-muted small mb-1">Ghi chú / Giải trình bổ sung (tùy chọn):</label>
                        <textarea name="closeNotes" id="closeNotes" rows="2" class="form-control form-control-sm" 
                                  placeholder="Nhập thêm chi tiết hoặc chỉ đạo bàn giao (nếu có)..."></textarea>
                    </div>

                    <!-- 3. BƯỚC 3: Cảnh báo tự động gỡ tin tuyển dụng đang đăng -->
                    <div class="alert alert-info border-info d-flex align-items-start gap-3 mb-4" role="alert">
                        <i class="bi bi-megaphone-fill fs-3 text-info"></i>
                        <div>
                            <h6 class="alert-heading fw-bold mb-1">Tự động gỡ tin tuyển dụng</h6>
                            <p class="small mb-0 text-dark">
                                Hệ thống sẽ <strong class="text-primary">tự động gỡ bỏ toàn bộ tin tuyển dụng đang đăng</strong> của vị trí này trên tất cả các cổng (Cổng nội bộ, Trang sự nghiệp công ty) để ngừng tiếp nhận hồ sơ mới.
                            </p>
                            <div class="mt-2 text-xs text-muted" id="postingsStatusText">
                                <span class="spinner-border spinner-border-sm text-info me-1" role="status"></span> Đang kiểm tra tin tuyển dụng...
                            </div>
                        </div>
                    </div>

                    <!-- 4. BƯỚC 4: Cảnh báo và Xử lý Ứng viên còn trong Pipeline -->
                    <div id="candidatePipelineSection">
                        <!-- Loading trạng thái ứng viên -->
                        <div id="pipelineLoading" class="p-3 text-center text-muted border rounded bg-white">
                            <span class="spinner-border spinner-border-sm text-primary me-2" role="status"></span>
                            Đang rà soát ứng viên trong pipeline tuyển dụng...
                        </div>

                        <!-- Cảnh báo NẾU CÓ ứng viên trong pipeline -->
                        <div id="pipelineWarningBox" class="border rounded p-3 bg-warning-subtle border-warning" style="display: none;">
                            <div class="d-flex align-items-start gap-2 mb-2">
                                <i class="bi bi-exclamation-triangle-fill fs-4 text-warning-emphasis"></i>
                                <div>
                                    <h6 class="fw-bold text-warning-emphasis mb-1">
                                        CẢNH BÁO: Còn <span id="candidateCountNumber" class="badge bg-danger">0</span> ứng viên đang trong Pipeline!
                                    </h6>
                                    <small class="text-dark">
                                        Các ứng viên dưới đây vẫn đang trong quy trình tuyển dụng (Sàng lọc, Phỏng vấn hoặc Đề nghị). Bạn bắt buộc phải chọn phương án xử lý trước khi đóng:
                                    </small>
                                </div>
                            </div>

                            <!-- Danh sách rút gọn ứng viên trong pipeline -->
                            <div class="table-responsive bg-white rounded border my-2" style="max-height: 140px; overflow-y: auto;">
                                <table class="table table-sm table-hover mb-0" style="font-size: 13px;">
                                    <thead class="table-light sticky-top">
                                        <tr>
                                            <th>Họ và tên</th>
                                            <th>Email</th>
                                            <th>Vòng hiện tại</th>
                                        </tr>
                                    </thead>
                                    <tbody id="pipelineCandidateListBody">
                                        <!-- Render qua JS -->
                                    </tbody>
                                </table>
                            </div>

                            <!-- Lựa chọn phương án xử lý ứng viên -->
                            <div class="mt-3">
                                <label class="form-label fw-bold text-dark small mb-2">
                                    Phương án xử lý các ứng viên này: <span class="text-danger">*</span>
                                </label>
                                <div class="form-check mb-2">
                                    <input class="form-check-input" type="radio" name="candidateAction" id="actionReject" value="REJECT_ALL" checked />
                                    <label class="form-check-label text-dark small" for="actionReject">
                                        <strong>Tự động chuyển thành Từ chối (Rejected)</strong> - Hệ thống ghi nhận lý do vị trí đã đóng/huỷ và gửi thông báo cảm ơn phù hợp.
                                    </label>
                                </div>
                                <div class="form-check mb-2">
                                    <input class="form-check-input" type="radio" name="candidateAction" id="actionHold" value="HOLD_ALL" />
                                    <label class="form-check-label text-dark small" for="actionHold">
                                        <strong>Chuyển vào Kho dữ liệu tài năng (Talent Pool)</strong> - Giữ lại hồ sơ ứng viên để ưu tiên liên hệ khi mở lại vị trí.
                                    </label>
                                </div>
                                <div class="form-check">
                                    <input class="form-check-input" type="radio" name="candidateAction" id="actionManual" value="CONFIRM_MANUAL" />
                                    <label class="form-check-label text-dark small" for="actionManual">
                                        <strong>Tôi đã xử lý thủ công</strong> - Đã liên hệ và thông báo trực tiếp cho từng ứng viên, xác nhận đóng vị trí.
                                    </label>
                                </div>
                            </div>
                        </div>

                        <!-- Thông báo an toàn NẾU KHÔNG CÓ ứng viên nào -->
                        <div id="pipelineSafeBox" class="border rounded p-3 bg-success-subtle border-success" style="display: none;">
                            <div class="d-flex align-items-center gap-2">
                                <i class="bi bi-check-circle-fill fs-4 text-success"></i>
                                <div>
                                    <h6 class="fw-bold text-success-emphasis mb-0">Pipeline an toàn</h6>
                                    <small class="text-dark">Không có ứng viên nào đang dở dang trong pipeline tuyển dụng.</small>
                                </div>
                            </div>
                            <input type="hidden" name="candidateAction" id="safeCandidateAction" value="NONE" disabled />
                        </div>
                    </div>
                </div>

                <div class="modal-footer bg-light border-top">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy bỏ</button>
                    <button type="submit" id="btnSubmitClose" class="btn btn-danger fw-semibold">
                        <i class="bi bi-check-circle-fill me-1"></i> Xác nhận Đóng Yêu cầu
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Modal xem chi tiết yêu cầu tuyển dụng -->
<div class="modal fade" id="detailModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content">
            <div class="modal-header border-bottom">
                <div>
                    <h5 class="modal-title fw-bold" id="modalTitle">Chi tiết Yêu cầu tuyển dụng</h5>
                    <span class="badge bg-secondary" id="modalCode"></span>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4">
                <div class="row g-3 mb-4 p-3 bg-light rounded border">
                    <div class="col-md-6">
                        <small class="text-muted d-block">Chức danh tuyển dụng</small>
                        <span class="fw-bold fs-6" id="modalPosition"></span>
                    </div>
                    <div class="col-md-6">
                        <small class="text-muted d-block">Phòng ban</small>
                        <span class="fw-semibold" id="modalDept"></span>
                    </div>
                    <div class="col-md-4">
                        <small class="text-muted d-block">Số lượng tuyển</small>
                        <span class="badge bg-primary-subtle text-primary border" id="modalHeadcount"></span>
                    </div>
                    <div class="col-md-4">
                        <small class="text-muted d-block">Lý do tuyển</small>
                        <span class="fw-semibold" id="modalReason"></span>
                    </div>
                    <div class="col-md-4">
                        <small class="text-muted d-block">Ngày cần người</small>
                        <span class="fw-semibold text-danger" id="modalDeadline"></span>
                    </div>
                    <div class="col-12">
                        <small class="text-muted d-block">Dải lương đề xuất</small>
                        <span class="fw-bold text-success fs-6" id="modalSalary"></span>
                    </div>
                </div>

                <!-- Thông tin đóng nếu đã kết thúc -->
                <div id="modalClosureInfoContainer" class="mb-4 p-3 rounded border" style="display:none;">
                    <h6 class="fw-bold mb-2" id="modalClosureHeader">
                        <i class="bi bi-info-circle-fill me-1"></i> Thông tin kết thúc yêu cầu:
                    </h6>
                    <div class="row g-2 small">
                        <div class="col-md-6">
                            <span class="text-muted">Trạng thái kết thúc:</span>
                            <span class="fw-bold ms-1" id="modalClosureStatus"></span>
                        </div>
                        <div class="col-md-6">
                            <span class="text-muted">Người thực hiện:</span>
                            <span class="fw-semibold ms-1" id="modalClosureActor"></span>
                        </div>
                        <div class="col-md-12">
                            <span class="text-muted">Lý do:</span>
                            <span class="fw-semibold ms-1" id="modalClosureReason"></span>
                        </div>
                        <div class="col-md-12" id="modalClosureNotesRow" style="display:none;">
                            <span class="text-muted">Ghi chú giải trình:</span>
                            <div class="p-2 bg-white rounded border mt-1" id="modalClosureNotes" style="white-space: pre-line;"></div>
                        </div>
                    </div>
                </div>

                <!-- Giải trình lương nếu có -->
                <div id="modalExplanationContainer" class="mb-4 p-3 bg-warning-subtle border border-warning rounded" style="display:none;">
                    <h6 class="fw-bold text-warning-emphasis mb-2">
                        <i class="bi bi-exclamation-triangle-fill me-1"></i> Giải trình dải lương ngoài chuẩn:
                    </h6>
                    <div id="modalExplanation" class="small text-dark" style="white-space: pre-line;"></div>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold text-primary border-bottom pb-2">
                        <i class="bi bi-file-earmark-text me-1"></i> Mô tả công việc (Job Description)
                    </h6>
                    <div id="modalJobDescription" class="p-3 bg-white rounded border small" style="white-space: pre-line; min-height: 80px;"></div>
                </div>

                <div class="mb-4">
                    <h6 class="fw-bold text-primary border-bottom pb-2 d-flex align-items-center justify-content-between">
                        <span><i class="bi bi-diagram-3-fill me-1"></i> Chuỗi cấp phê duyệt (Ma trận phê duyệt)</span>
                        <span id="approvalChainBadge" class="badge bg-light text-secondary border text-xs"></span>
                    </h6>
                    <div id="modalApprovalSteps" class="p-3 bg-light rounded border">
                        <div class="text-center py-2 text-muted small">
                            <span class="spinner-border spinner-border-sm me-1"></span> Đang tải chuỗi phê duyệt...
                        </div>
                    </div>
                </div>

                <div class="mb-2">
                    <h6 class="fw-bold text-primary border-bottom pb-2">
                        <i class="bi bi-person-check me-1"></i> Yêu cầu ứng viên (Candidate Requirements)
                    </h6>
                    <div id="modalJobRequirements" class="p-3 bg-white rounded border small" style="white-space: pre-line; min-height: 80px;"></div>
                </div>
            </div>
            <div class="modal-footer border-top d-flex justify-content-between align-items-center">
                <button type="button" class="btn btn-outline-primary btn-sm" onclick="openApprovalModalFromDetail()">
                    <i class="bi bi-diagram-3-fill me-1"></i>Xem Nhật ký & Chuỗi cấp duyệt
                </button>
                <div class="d-flex align-items-center gap-2">
                    <a id="modalCopyBtn" href="#" class="btn btn-outline-success btn-sm" title="KN-115: Sao chép JD và yêu cầu thành bản nháp mới">
                        <i class="bi bi-copy me-1"></i>Sao chép JD &amp; Yêu cầu
                    </a>
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Đóng</button>
                </div>
            </div>
        </div>
    </div>
</div>

<!-- =========================================================================
     MODAL: NHẬT KÝ & TRẠNG THÁI CHUỖI CẤP DUYỆT HỒ SƠ (KN-154 -> KN-161)
     - Cấp nào đã duyệt, đang chờ ai (KN-157)
     - Người duyệt, thời điểm, ý kiến (KN-161)
     - Lịch sử không sửa và không xoá được (Chỉ đọc) (KN-155, KN-160)
     - Timeline phân cấp phê duyệt (KN-159)
     - Ghi nhận kết quả phê duyệt theo từng cấp (KN-154)
     ========================================================================= -->
<div class="modal fade" id="approvalModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content shadow border-0">
            <div class="modal-header border-bottom bg-light py-3">
                <div class="d-flex align-items-center gap-2 flex-wrap">
                    <div class="logo-icon bg-primary text-white rounded p-1 px-2 d-inline-flex align-items-center justify-content-center" style="width: 34px; height: 34px;">
                        <i class="bi bi-diagram-3-fill"></i>
                    </div>
                    <div>
                        <h5 class="modal-title fw-bold text-dark mb-0">Nhật ký &amp; Trạng thái Hồ sơ</h5>
                        <small class="text-muted">Chuỗi phân cấp phê duyệt yêu cầu tuyển dụng</small>
                    </div>
                </div>
                <div class="d-flex align-items-center gap-2">
                    <span class="approval-lock-badge" title="Lịch sử không thể sửa hoặc xóa theo quy định kiểm toán bảo mật">
                        <i class="bi bi-shield-lock-fill text-warning"></i>
                        Lịch sử chỉ đọc (Đã khóa)
                    </span>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
            </div>
            
            <div class="modal-body p-4">
                <!-- Vùng loading khi fetch dữ liệu -->
                <div id="approvalLoading" class="text-center py-5">
                    <div class="spinner-border text-primary" role="status">
                        <span class="visually-hidden">Đang tải...</span>
                    </div>
                    <div class="mt-2 text-muted small">Đang nạp chuỗi cấp duyệt và nhật ký...</div>
                </div>

                <!-- Nội dung chính sau khi nạp xong -->
                <div id="approvalContent" style="display: none;">
                    <!-- Thông tin tổng quan hồ sơ & Banner "Đang chờ ai" (KN-157) -->
                    <div class="card border shadow-sm mb-4 bg-white">
                        <div class="card-body p-3">
                            <div class="row g-2 align-items-center">
                                <div class="col-md-7">
                                    <div class="d-flex align-items-center gap-2 mb-1">
                                        <span class="badge bg-light text-dark border font-monospace" id="apprReqCode"></span>
                                        <span class="badge" id="apprReqStatus"></span>
                                    </div>
                                    <h6 class="fw-bold text-dark mb-1" id="apprReqPosition"></h6>
                                    <small class="text-muted">
                                        <i class="bi bi-building me-1"></i><span id="apprReqDept"></span>
                                    </small>
                                </div>
                                <div class="col-md-5">
                                    <div class="approval-waiting-box p-2.5">
                                        <div class="approval-waiting-icon" style="width: 36px; height: 36px; font-size: 1.1rem;">
                                            <i class="bi bi-person-fill-exclamation"></i>
                                        </div>
                                        <div class="flex-grow-1 overflow-hidden">
                                            <small class="text-muted d-block fw-semibold" style="font-size: 0.72rem;">TIẾN TRÌNH HIỆN TẠI</small>
                                            <div class="fw-bold text-dark text-truncate small" id="apprWaitingFor" title=""></div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Timeline Phân cấp phê duyệt (KN-159) -->
                    <div class="mb-4">
                        <div class="d-flex align-items-center justify-content-between mb-2">
                            <h6 class="fw-bold text-dark mb-0 d-flex align-items-center gap-2">
                                <i class="bi bi-bar-chart-steps text-primary"></i>
                                Tiến trình Chuỗi cấp duyệt (3 cấp)
                            </h6>
                            <small class="text-muted" style="font-size: 0.75rem;">Quy trình: Bộ phận chuyên môn &rarr; Nhân sự &rarr; Ban Giám đốc</small>
                        </div>
                        <div class="approval-stepper px-2 py-3 bg-light rounded border" id="apprStepper">
                            <!-- Stepper nodes được render bằng JavaScript -->
                        </div>
                    </div>

                    <!-- Danh sách chi tiết từng bước & ý kiến người duyệt (KN-160, KN-161) -->
                    <div class="mb-4">
                        <div class="d-flex align-items-center justify-content-between mb-2">
                            <h6 class="fw-bold text-dark mb-0 d-flex align-items-center gap-2">
                                <i class="bi bi-journal-text text-primary"></i>
                                Chi tiết từng bước phê duyệt &amp; Ý kiến người duyệt
                            </h6>
                            <span class="text-muted small" style="font-size: 0.75rem;">
                                <i class="bi bi-lock-fill text-warning me-1"></i>Lịch sử bất biến
                            </span>
                        </div>
                        <div id="apprStepsList">
                            <!-- Danh sách thẻ các cấp duyệt được render bằng JavaScript -->
                        </div>
                    </div>

                    <!-- Khung thao tác phê duyệt (Hiển thị nếu người dùng hiện tại có quyền tại cấp đang chờ) (KN-154) -->
                    <div id="apprActionFormCard" class="card border-primary border-2 shadow-sm mb-2" style="display: none;">
                        <div class="card-header bg-primary-subtle border-primary py-2.5 d-flex align-items-center justify-content-between">
                            <span class="fw-bold text-primary small">
                                <i class="bi bi-pencil-square me-1"></i> Thao tác Phê duyệt / Từ chối (Dành cho bạn)
                            </span>
                            <span class="badge bg-primary text-white" id="actionCurrentStepName"></span>
                        </div>
                        <div class="card-body p-3">
                            <form id="actionApprovalForm" onsubmit="return false;">
                                <input type="hidden" id="actionRequisitionId" value="">
                                <div class="mb-3">
                                    <label class="form-label small fw-bold text-dark">
                                        Ý kiến người duyệt / Góp ý <span class="text-danger">* (Bắt buộc nếu từ chối)</span>
                                    </label>
                                    <textarea id="actionComment" class="form-control form-control-sm" rows="3" 
                                              placeholder="Nhập nhận xét, hướng dẫn hoặc lý do từ chối..."></textarea>
                                </div>
                                <div class="d-flex align-items-center justify-content-end gap-2">
                                    <button type="button" class="btn btn-sm btn-outline-danger px-3" onclick="submitApprovalDecision('REJECT')">
                                        <i class="bi bi-x-circle me-1"></i> Từ chối yêu cầu
                                    </button>
                                    <button type="button" class="btn btn-sm btn-success px-3 shadow-sm" onclick="submitApprovalDecision('APPROVE')">
                                        <i class="bi bi-check-circle-fill me-1"></i> Đồng ý phê duyệt
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            </div>

            <div class="modal-footer border-top py-2.5 bg-light">
                <button type="button" class="btn btn-secondary btn-sm px-3" data-bs-dismiss="modal">Đóng</button>
            </div>
        </div>
    </div>
</div>

<!-- Modal Duyệt / Từ chối cấp -->
<div class="modal fade" id="actionStepModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <form id="actionStepForm" method="post" action="${pageContext.request.contextPath}/recruitment-requests/approve-step">
                <input type="hidden" name="requisitionId" id="actionReqId" value="" />
                <input type="hidden" name="stepId" id="actionStepId" value="" />
                <div class="modal-header py-3" id="actionStepHeader">
                    <h5 class="modal-title fw-bold" id="actionStepTitle">Phê duyệt cấp</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <p id="actionStepDesc" class="mb-3 text-secondary"></p>
                    <div class="mb-3">
                        <label class="form-label small fw-semibold">Ý kiến / Nhận xét / Lý do:</label>
                        <textarea name="comment" id="actionStepComment" class="form-control" rows="3" placeholder="Nhập ý kiến phê duyệt hoặc lý do từ chối..."></textarea>
                    </div>
                </div>
                <div class="modal-footer bg-light py-2">
                    <button type="button" class="btn btn-sm btn-secondary" data-bs-dismiss="modal">Hủy bỏ</button>
                    <button type="submit" id="actionStepSubmitBtn" class="btn btn-sm btn-primary">Xác nhận</button>
                </div>
            </form>
        </div>
    </div>
</div>

<jsp:include page="../common/footer.jsp" />

<script>
// Danh sách lý do riêng cho từng trạng thái kết thúc
const REASON_PRESETS = {
    'FULFILLED': [
        'Đã tuyển đủ số lượng chỉ tiêu (Headcount)',
        'Đã ký hợp đồng thử việc và tiếp nhận đủ ứng viên',
        'Tuyển vượt chỉ tiêu xuất sắc',
        'Ứng viên nội bộ đã được điều chuyển đáp ứng đủ yêu cầu',
        'Khác...'
    ],
    'PAUSED': [
        'Tạm dừng kế hoạch tuyển dụng quý này',
        'Đóng băng ngân sách tuyển dụng (Budget freeze)',
        'Chờ tái cơ cấu tổ chức / phòng ban chuyên môn',
        'Thay đổi yêu cầu năng lực vị trí cần chuẩn bị lại JD',
        'Khác...'
    ],
    'CANCELLED': [
        'Huỷ bỏ vị trí do thay đổi chiến lược kinh doanh',
        'Dự án bị huỷ bỏ hoặc thu hẹp quy mô hoạt động',
        'Chuyển giao vị trí sang đơn vị thành viên khác phụ trách',
        'Không còn nhu cầu tuyển dụng vị trí này',
        'Khác...'
    ]
};

function onStatusChanged() {
    const selectedStatus = document.querySelector('input[name="terminalStatus"]:checked').value;
    const reasonSelect = document.getElementById('closeReasonSelect');
    reasonSelect.innerHTML = '';

    const list = REASON_PRESETS[selectedStatus] || ['Lý do khác...'];
    list.forEach(r => {
        const opt = document.createElement('option');
        opt.value = r;
        opt.textContent = r;
        reasonSelect.appendChild(opt);
    });

    onReasonSelectChanged();
}

function onReasonSelectChanged() {
    const select = document.getElementById('closeReasonSelect');
    const customWrapper = document.getElementById('customReasonWrapper');
    const customInput = document.getElementById('customReasonInput');
    const finalReason = document.getElementById('finalCloseReason');

    if (select.value === 'Khác...') {
        customWrapper.style.display = 'block';
        finalReason.value = customInput.value.trim() || 'Lý do khác';
    } else {
        customWrapper.style.display = 'none';
        finalReason.value = select.value;
    }
}

document.getElementById('customReasonInput').addEventListener('input', function() {
    if (document.getElementById('closeReasonSelect').value === 'Khác...') {
        document.getElementById('finalCloseReason').value = this.value.trim();
    }
});

function openCloseModal(id, code, positionTitle) {
    document.getElementById('closeReqId').value = id;
    document.getElementById('closeModalSubtitle').textContent = code + ' - ' + positionTitle;
    
    // Reset form & radios
    document.querySelector('input[name="terminalStatus"][value="FULFILLED"]').checked = true;
    onStatusChanged();
    document.getElementById('closeNotes').value = '';

    // Reset UI pipeline
    document.getElementById('pipelineLoading').style.display = 'block';
    document.getElementById('pipelineWarningBox').style.display = 'none';
    document.getElementById('pipelineSafeBox').style.display = 'none';
    document.getElementById('postingsStatusText').innerHTML = '<span class="spinner-border spinner-border-sm text-info me-1" role="status"></span> Đang kiểm tra tin tuyển dụng...';

    const modal = new bootstrap.Modal(document.getElementById('closeModal'));
    modal.show();

    // Gọi API check pipeline ứng viên và tin đăng
    fetch('${pageContext.request.contextPath}/recruitment-requests/check-pipeline?id=' + encodeURIComponent(id))
        .then(res => res.json())
        .then(data => {
            document.getElementById('pipelineLoading').style.display = 'none';

            // Cập nhật trạng thái tin tuyển dụng
            const postingsCount = data.activePostingsCount || 0;
            if (postingsCount > 0) {
                document.getElementById('postingsStatusText').innerHTML = 
                    '<i class="bi bi-check-circle text-primary me-1"></i> Phát hiện <strong>' + postingsCount + ' tin tuyển dụng đang đăng</strong> sẽ được tự động gỡ khi xác nhận.';
            } else {
                document.getElementById('postingsStatusText').innerHTML = 
                    '<i class="bi bi-info-circle text-muted me-1"></i> Không có tin tuyển dụng nào đang đăng công khai.';
            }

            // Cập nhật trạng thái ứng viên trong pipeline
            const candidates = data.activeCandidates || [];
            const count = candidates.length;

            if (count > 0) {
                document.getElementById('pipelineWarningBox').style.display = 'block';
                document.getElementById('pipelineSafeBox').style.display = 'none';
                document.getElementById('candidateCountNumber').textContent = count;

                const tbody = document.getElementById('pipelineCandidateListBody');
                tbody.innerHTML = '';
                candidates.forEach(c => {
                    let badgeClass = 'bg-secondary';
                    let badgeLabel = c.status;
                    if (c.status === 'APPLIED') { badgeClass = 'bg-primary'; badgeLabel = 'Mới ứng tuyển'; }
                    else if (c.status === 'SCREENING') { badgeClass = 'bg-info text-dark'; badgeLabel = 'Sàng lọc'; }
                    else if (c.status === 'INTERVIEWING') { badgeClass = 'bg-warning text-dark'; badgeLabel = 'Phỏng vấn'; }
                    else if (c.status === 'OFFER') { badgeClass = 'bg-purple text-white'; badgeLabel = 'Đã gửi Offer'; }

                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td class="fw-semibold">\${escapeHtml(c.fullName)}</td>
                        <td class="text-muted">\${escapeHtml(c.email)}</td>
                        <td><span class="badge \${badgeClass} font-monospace">\${badgeLabel}</span></td>
                    `;
                    tbody.appendChild(tr);
                });

                // Kích hoạt radio action
                document.getElementById('actionReject').disabled = false;
                document.getElementById('actionHold').disabled = false;
                document.getElementById('actionManual').disabled = false;
                document.getElementById('actionReject').checked = true;
            } else {
                document.getElementById('pipelineWarningBox').style.display = 'none';
                document.getElementById('pipelineSafeBox').style.display = 'block';

                document.getElementById('actionReject').disabled = true;
                document.getElementById('actionHold').disabled = true;
                document.getElementById('actionManual').disabled = true;
                document.getElementById('safeCandidateAction').disabled = false;
            }
        })
        .catch(err => {
            document.getElementById('pipelineLoading').innerHTML = 
                '<span class="text-danger"><i class="bi bi-exclamation-circle me-1"></i> Không thể tải dữ liệu pipeline: ' + escapeHtml(err.message) + '</span>';
        });
}

var currentDetailId = null;
var currentApprovalReqId = null;

function escapeHtml(text) {
    if (!text) return '';
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function viewDetails(id, code, pos, dept, count, reason, sal, deadline, statusLabel, statusCode, closeStatus, closeReason, closeNotes, closedByName, closedAt) {
    currentDetailId = id;
    currentApprovalReqId = id;
    document.getElementById('modalTitle').textContent = pos;
    document.getElementById('modalCode').textContent = code + ' - ' + statusLabel;
    document.getElementById('modalPosition').textContent = pos;
    document.getElementById('modalDept').textContent = dept || 'Chưa gán';
    document.getElementById('modalHeadcount').textContent = count + ' nhân sự';
    document.getElementById('modalReason').textContent = reason;
    document.getElementById('modalSalary').textContent = sal;
    document.getElementById('modalDeadline').textContent = deadline || 'Chưa đặt';

    var copyBtn = document.getElementById('modalCopyBtn');
    if (copyBtn) {
        copyBtn.href = '${pageContext.request.contextPath}/recruitment-requests/create?copyFrom=' + encodeURIComponent(id);
    }

    var jd = document.getElementById('jd-' + id) ? document.getElementById('jd-' + id).textContent.trim() : '';
    var req = document.getElementById('req-' + id) ? document.getElementById('req-' + id).textContent.trim() : '';
    var exp = document.getElementById('exp-' + id) ? document.getElementById('exp-' + id).textContent.trim() : '';

    document.getElementById('modalJobDescription').textContent = jd || '(Chưa có mô tả công việc)';
    document.getElementById('modalJobRequirements').textContent = req || '(Chưa có yêu cầu ứng viên)';

    var expContainer = document.getElementById('modalExplanationContainer');
    if (exp && exp.length > 0) {
        document.getElementById('modalExplanation').textContent = exp;
        expContainer.style.display = 'block';
    } else {
        expContainer.style.display = 'none';
    }

    // Hiển thị thông tin đóng nếu yêu cầu đã kết thúc
    var closureContainer = document.getElementById('modalClosureInfoContainer');
    var isTerminal = (statusCode === 'FULFILLED' || statusCode === 'PAUSED' || statusCode === 'CANCELLED' || statusCode === 'CLOSED' || (closeReason && closeReason.length > 0));

    if (isTerminal) {
        closureContainer.style.display = 'block';
        if (statusCode === 'FULFILLED' || closeStatus === 'FULFILLED') {
            closureContainer.className = 'mb-4 p-3 rounded border bg-teal-subtle border-teal text-teal-emphasis';
            document.getElementById('modalClosureHeader').className = 'fw-bold mb-2 text-teal-emphasis';
        } else if (statusCode === 'PAUSED' || closeStatus === 'PAUSED') {
            closureContainer.className = 'mb-4 p-3 rounded border bg-warning-subtle border-warning text-warning-emphasis';
            document.getElementById('modalClosureHeader').className = 'fw-bold mb-2 text-warning-emphasis';
        } else {
            closureContainer.className = 'mb-4 p-3 rounded border bg-danger-subtle border-danger text-danger-emphasis';
            document.getElementById('modalClosureHeader').className = 'fw-bold mb-2 text-danger-emphasis';
        }

        document.getElementById('modalClosureStatus').textContent = statusLabel;
        document.getElementById('modalClosureActor').textContent = closedByName || 'Hệ thống';
        document.getElementById('modalClosureReason').textContent = closeReason || '(Không ghi)';

        var notesRow = document.getElementById('modalClosureNotesRow');
        if (closeNotes && closeNotes.trim().length > 0) {
            document.getElementById('modalClosureNotes').textContent = closeNotes;
            notesRow.style.display = 'block';
        } else {
            notesRow.style.display = 'none';
        }
    } else {
        closureContainer.style.display = 'none';
    }

    // Tải chuỗi phê duyệt theo Ma trận phê duyệt
    var stepsDiv = document.getElementById('modalApprovalSteps');
    if (stepsDiv) {
        stepsDiv.innerHTML = '<div class="text-center py-2 text-muted small"><span class="spinner-border spinner-border-sm me-1"></span> Đang tải chuỗi phê duyệt...</div>';
        
        fetch('${pageContext.request.contextPath}/recruitment-requests/api/approval-steps?id=' + id)
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data && data.steps && data.steps.length > 0) {
                    var html = '<div class="d-flex flex-column gap-2">';
                    data.steps.forEach(function(s) {
                        var statusBadge = '';
                        if (s.status === 'APPROVED') {
                            statusBadge = '<span class="badge bg-success-subtle text-success border border-success-subtle"><i class="bi bi-check-circle-fill me-1"></i>Đã phê duyệt</span>';
                        } else if (s.status === 'REJECTED') {
                            statusBadge = '<span class="badge bg-danger-subtle text-danger border border-danger-subtle"><i class="bi bi-x-circle-fill me-1"></i>Từ chối</span>';
                        } else {
                            statusBadge = '<span class="badge bg-warning-subtle text-dark border border-warning-subtle"><i class="bi bi-hourglass-split me-1"></i>Chờ phê duyệt</span>';
                        }

                        html += '<div class="p-2.5 bg-white rounded border d-flex flex-wrap align-items-center justify-content-between gap-2">';
                        html += '  <div>';
                        html += '    <div class="fw-bold text-dark text-xs">Cấp ' + s.stepOrder + ': ' + s.stepName + '</div>';
                        html += '    <small class="text-muted text-xs">Người phụ trách: <span class="fw-semibold text-secondary">' + s.assignedApproverName + '</span></small>';
                        if (s.actedByName) {
                            html += '    <div class="text-xs text-muted mt-0.5"><i class="bi bi-person-check me-1"></i>Người duyệt: ' + s.actedByName + '</div>';
                        }
                        if (s.comment) {
                            html += '    <div class="text-xs text-secondary mt-0.5 fst-italic">" ' + s.comment + ' "</div>';
                        }
                        html += '  </div>';
                        html += '  <div class="d-flex align-items-center gap-2">';
                        html += statusBadge;
                        if (s.status === 'PENDING') {
                            html += '<button type="button" class="btn btn-sm btn-outline-success py-0.5 px-2 text-xs" onclick="openStepAction(\'' + id + '\', \'' + s.id + '\', \'' + s.stepName + '\', \'approve\')"><i class="bi bi-check-lg"></i> Duyệt</button>';
                            html += '<button type="button" class="btn btn-sm btn-outline-danger py-0.5 px-2 text-xs" onclick="openStepAction(\'' + id + '\', \'' + s.id + '\', \'' + s.stepName + '\', \'reject\')"><i class="bi bi-x-lg"></i> Từ chối</button>';
                        }
                        html += '  </div>';
                        html += '</div>';
                    });
                    html += '</div>';
                    stepsDiv.innerHTML = html;
                    var badgeElem = document.getElementById('approvalChainBadge');
                    if (badgeElem) badgeElem.textContent = data.steps.length + ' cấp phê duyệt';
                } else {
                    stepsDiv.innerHTML = '<div class="text-muted small text-center py-1">Chưa có chuỗi phê duyệt được khởi tạo cho yêu cầu này.</div>';
                    var badgeElem = document.getElementById('approvalChainBadge');
                    if (badgeElem) badgeElem.textContent = 'Chưa thiết lập';
                }
            })
            .catch(function(err) {
                stepsDiv.innerHTML = '<div class="text-danger small">Không thể tải thông tin chuỗi phê duyệt.</div>';
            });
    }

    var modal = new bootstrap.Modal(document.getElementById('detailModal'));
    modal.show();
}

function openStepAction(reqId, stepId, stepName, action) {
    document.getElementById('actionReqId').value = reqId;
    document.getElementById('actionStepId').value = stepId;
    var form = document.getElementById('actionStepForm');
    var title = document.getElementById('actionStepTitle');
    var desc = document.getElementById('actionStepDesc');
    var header = document.getElementById('actionStepHeader');
    var submitBtn = document.getElementById('actionStepSubmitBtn');

    if (action === 'approve') {
        form.action = '${pageContext.request.contextPath}/recruitment-requests/approve-step';
        header.className = 'modal-header bg-success-subtle text-success py-3';
        title.innerHTML = '<i class="bi bi-check-circle-fill me-2"></i>Phê duyệt: ' + stepName;
        desc.textContent = 'Bạn đang xác nhận PHÊ DUYỆT yêu cầu tuyển dụng này tại bước ' + stepName + '.';
        submitBtn.className = 'btn btn-sm btn-success';
        submitBtn.textContent = 'Xác nhận phê duyệt';
    } else {
        form.action = '${pageContext.request.contextPath}/recruitment-requests/reject-step';
        header.className = 'modal-header bg-danger-subtle text-danger py-3';
        title.innerHTML = '<i class="bi bi-x-circle-fill me-2"></i>Từ chối: ' + stepName;
        desc.textContent = 'Bạn đang xác nhận TỪ CHỐI yêu cầu tuyển dụng này tại bước ' + stepName + '.';
        submitBtn.className = 'btn btn-sm btn-danger';
        submitBtn.textContent = 'Xác nhận từ chối';
    }

    var modal = new bootstrap.Modal(document.getElementById('actionStepModal'));
    modal.show();
}

function openApprovalModalFromDetail() {
    var detailModalEl = document.getElementById('detailModal');
    var detailModalInstance = bootstrap.Modal.getInstance(detailModalEl);
    if (detailModalInstance) {
        detailModalInstance.hide();
    }
    if (currentDetailId) {
        openApprovalModal(currentDetailId);
    }
}

function openApprovalModal(reqId) {
    if (!reqId) return;
    currentApprovalReqId = reqId;
    document.getElementById('actionRequisitionId').value = reqId;

    var approvalModalEl = document.getElementById('approvalModal');
    var modal = bootstrap.Modal.getOrCreateInstance(approvalModalEl);
    modal.show();

    // Hiện loading, ẩn nội dung cũ
    document.getElementById('approvalLoading').style.display = 'block';
    document.getElementById('approvalContent').style.display = 'none';

    fetch('${pageContext.request.contextPath}/recruitment-requests/api/approval-chain?id=' + encodeURIComponent(reqId))
        .then(function(res) {
            if (!res.ok) throw new Error('Không thể tải chuỗi phê duyệt (' + res.status + ')');
            return res.json();
        })
        .then(function(data) {
            renderApprovalModalData(data);
        })
        .catch(function(err) {
            document.getElementById('approvalLoading').innerHTML = 
                '<div class="text-danger py-4"><i class="bi bi-exclamation-triangle-fill fs-3 d-block mb-2"></i>' +
                'Lỗi khi tải thông tin: ' + escapeHtml(err.message) + '</div>';
        });
}

function renderApprovalModalData(data) {
    // 1. Thông tin tiêu đề & tổng quan
    document.getElementById('apprReqCode').textContent = data.code || '--';
    
    var statusBadge = document.getElementById('apprReqStatus');
    statusBadge.textContent = data.statusLabel || data.status;
    statusBadge.className = 'badge ' + (data.statusBadgeClass || 'bg-secondary');

    document.getElementById('apprReqPosition').textContent = data.positionTitle || data.title;
    document.getElementById('apprReqDept').textContent = data.departmentName || 'Chưa gán';

    var waitingBox = document.getElementById('apprWaitingFor');
    waitingBox.textContent = data.waitingForText || '--';
    waitingBox.title = data.waitingForText || '';

    // 2. Render Stepper Timeline (KN-159)
    var stepperEl = document.getElementById('apprStepper');
    stepperEl.innerHTML = '';
    var steps = data.steps || [];

    steps.forEach(function(s, idx) {
        var stepItem = document.createElement('div');
        var itemClass = 'approval-stepper-item';
        var nodeContent = s.stepOrder;

        if (s.status === 'APPROVED') {
            itemClass += ' completed';
            nodeContent = '<i class="bi bi-check-lg"></i>';
        } else if (s.status === 'PENDING') {
            itemClass += ' active';
            nodeContent = '<i class="bi bi-hourglass-split"></i>';
        } else if (s.status === 'REJECTED') {
            itemClass += ' rejected';
            nodeContent = '<i class="bi bi-x-lg"></i>';
        } else {
            itemClass += ' waiting';
            nodeContent = '<i class="bi bi-clock"></i>';
        }

        stepItem.className = itemClass;
        stepItem.innerHTML = 
            '<div class="approval-stepper-node">' + nodeContent + '</div>' +
            '<div class="approval-stepper-title">' + escapeHtml(s.stepName) + '</div>' +
            '<div class="approval-stepper-desc">' + escapeHtml(s.statusLabel) + '</div>';

        stepperEl.appendChild(stepItem);
    });

    // 3. Render danh sách thẻ chi tiết từng bước & Ý kiến (KN-160, KN-161)
    var stepsListEl = document.getElementById('apprStepsList');
    stepsListEl.innerHTML = '';

    steps.forEach(function(s) {
        var card = document.createElement('div');
        var statusClass = 'status-' + (s.status ? s.status.toLowerCase() : 'waiting');
        card.className = 'approval-step-card ' + statusClass;

        // Người duyệt
        var approverHtml = '';
        if (s.approverName) {
            approverHtml = 
                '<div class="d-flex align-items-center gap-2">' +
                    '<div class="approver-avatar-circle"><i class="bi bi-person-check-fill"></i></div>' +
                    '<div>' +
                        '<div class="fw-bold text-dark small">' + escapeHtml(s.approverName) + '</div>' +
                        '<div class="text-xs text-muted">' + escapeHtml(s.approverJobTitle || s.approverEmail || '') + '</div>' +
                    '</div>' +
                '</div>';
        } else if (s.status === 'PENDING') {
            approverHtml = '<div class="small text-warning-emphasis"><i class="bi bi-hourglass-split me-1"></i>Đang chờ phê duyệt ở cấp này</div>';
        } else {
            approverHtml = '<div class="small text-muted"><i class="bi bi-clock me-1"></i>Chờ hoàn tất cấp trước</div>';
        }

        // Thời điểm
        var dateHtml = s.decisionDate ? 
            '<div class="small text-dark fw-semibold"><i class="bi bi-calendar3 me-1 text-primary"></i>' + escapeHtml(s.decisionDate) + '</div>' :
            '<div class="small text-muted">Chưa thực hiện</div>';

        // Ý kiến người duyệt
        var commentHtml = '';
        if (s.comment && s.comment.trim().length > 0) {
            var commentTypeClass = s.status === 'APPROVED' ? 'comment-approved' : (s.status === 'REJECTED' ? 'comment-rejected' : '');
            commentHtml = 
                '<div class="approval-comment-box ' + commentTypeClass + '">' +
                    '<i class="bi bi-chat-quote-fill me-1 opacity-75"></i>' +
                    '<strong>Ý kiến:</strong> "' + escapeHtml(s.comment) + '"' +
                '</div>';
        } else {
            commentHtml = '<div class="text-muted fst-italic mt-2" style="font-size: 0.78rem;">Chưa có ý kiến phản hồi</div>';
        }

        card.innerHTML = 
            '<div class="d-flex flex-wrap align-items-center justify-content-between gap-2 border-bottom pb-2 mb-2">' +
                '<div class="d-flex align-items-center gap-2">' +
                    '<span class="fw-bold text-dark fs-6">' + escapeHtml(s.stepName) + '</span>' +
                    '<span class="badge bg-primary-subtle text-primary border border-primary-subtle" style="font-size: 0.72rem;">' + escapeHtml(s.requiredRoleName) + '</span>' +
                '</div>' +
                '<div class="d-flex align-items-center gap-2">' +
                    '<span class="badge ' + escapeHtml(s.statusBadgeClass) + ' text-nowrap"><i class="bi ' + escapeHtml(s.statusIcon) + ' me-1"></i>' + escapeHtml(s.statusLabel) + '</span>' +
                    '<span class="approval-lock-badge" title="Lịch sử không cho sửa hoặc xoá (KN-155, KN-160)"><i class="bi bi-shield-lock text-success"></i>Chỉ đọc</span>' +
                '</div>' +
            '</div>' +
            '<div class="row g-2 align-items-center py-1">' +
                '<div class="col-12 col-md-7">' +
                    '<small class="text-muted d-block fw-semibold" style="font-size: 0.72rem;">NGƯỜI PHÊ DUYỆT</small>' +
                    approverHtml +
                '</div>' +
                '<div class="col-12 col-md-5">' +
                    '<small class="text-muted d-block fw-semibold" style="font-size: 0.72rem;">THỜI ĐIỂM DUYỆT</small>' +
                    dateHtml +
                '</div>' +
            '</div>' +
            commentHtml;

        stepsListEl.appendChild(card);
    });

    // 4. Form thao tác duyệt (KN-154)
    var actionCard = document.getElementById('apprActionFormCard');
    if (data.canCurrentUserApprove && data.isPendingApproval) {
        actionCard.style.display = 'block';
        document.getElementById('actionCurrentStepName').textContent = data.currentStepName || 'Cấp hiện tại';
        document.getElementById('actionComment').value = '';
    } else {
        actionCard.style.display = 'none';
    }

    document.getElementById('approvalLoading').style.display = 'none';
    document.getElementById('approvalContent').style.display = 'block';
}

function submitApprovalDecision(action) {
    if (!currentApprovalReqId) return;

    var comment = document.getElementById('actionComment').value.trim();
    if (action === 'REJECT' && !comment) {
        alert('Vui lòng nhập lý do từ chối phê duyệt để gửi phản hồi cho người lập phiếu!');
        document.getElementById('actionComment').focus();
        return;
    }

    var confirmMsg = action === 'APPROVE' ? 
        'Bạn có chắc chắn muốn ĐỒNG Ý PHÊ DUYỆT hồ sơ tuyển dụng này ở cấp hiện tại?' :
        'Bạn có chắc chắn muốn TỪ CHỐI hồ sơ tuyển dụng này?';

    if (!confirm(confirmMsg)) return;

    var formData = new URLSearchParams();
    formData.append('requisitionId', currentApprovalReqId);
    formData.append('action', action);
    formData.append('comment', comment);

    fetch('${pageContext.request.contextPath}/recruitment-requests/api/approve', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
        body: formData.toString()
    })
    .then(function(res) {
        return res.json().then(function(data) {
            return { ok: res.ok, data: data };
        });
    })
    .then(function(result) {
        if (!result.ok || !result.data.success) {
            alert('Lỗi: ' + (result.data.message || 'Không thể thực hiện phê duyệt!'));
            return;
        }

        alert(result.data.message || 'Thao tác thành công!');
        // Tải lại dữ liệu timeline trong modal
        if (result.data.state) {
            renderApprovalModalData(result.data.state);
        } else {
            openApprovalModal(currentApprovalReqId);
        }
        // Tải lại trang sau 1.5 giây để cập nhật bảng danh sách
        setTimeout(function() {
            window.location.reload();
        }, 1200);
    })
    .catch(function(err) {
        alert('Lỗi kết nối máy chủ: ' + err.message);
    });
}
</script>

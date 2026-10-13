<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Quản lý Yêu cầu tuyển dụng" />
<c:set var="breadcrumb" value="Yêu cầu tuyển dụng" />
<c:set var="activeMenu" value="requisitions" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

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
                    Khai báo chức danh, định biên, dải lương đề xuất, ngày cần người và soạn bản mô tả công việc (JD).
                </p>
            </div>
            <a href="${pageContext.request.contextPath}/recruitment-requests/create" class="btn btn-primary shadow-sm">
                <i class="bi bi-plus-circle-fill me-1"></i> Tạo yêu cầu tuyển dụng
            </a>
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
                            <option value="DRAFT" ${paramStatus == 'DRAFT' ? 'selected' : ''}>Lưu nháp (Draft)</option>
                            <option value="PENDING_APPROVAL" ${paramStatus == 'PENDING_APPROVAL' ? 'selected' : ''}>Chờ phê duyệt</option>
                            <option value="OPEN" ${paramStatus == 'OPEN' ? 'selected' : ''}>Đang tuyển dụng</option>
                            <option value="CLOSED" ${paramStatus == 'CLOSED' ? 'selected' : ''}>Đã đóng</option>
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

<style>
.table-req-custom {
    table-layout: fixed;
    width: 100%;
}
.table-req-custom th,
.table-req-custom td {
    padding: 0.65rem 0.5rem;
    font-size: 0.84rem;
    vertical-align: middle;
}
.table-req-custom thead th {
    white-space: nowrap;
    background-color: #f8fafc;
    color: #475569;
    font-size: 0.78rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.3px;
    border-bottom: 2px solid #e2e8f0;
}
.table-req-custom .truncate-cell {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
</style>

        <!-- Bảng danh sách yêu cầu tuyển dụng -->
        <div class="card mb-4 shadow-sm border">
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-custom table-hover table-req-custom align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th style="width: 10%;">Mã yêu cầu</th>
                                <th style="width: 20%;">Chức danh &amp; Tiêu đề</th>
                                <th style="width: 13%;">Phòng ban</th>
                                <th class="text-center" style="width: 5%;">SL</th>
                                <th style="width: 9%;">Lý do</th>
                                <th style="width: 14%;">Dải lương</th>
                                <th style="width: 9%;">Ngày cần</th>
                                <th class="text-center" style="width: 12%;">Trạng thái</th>
                                <th class="text-end pe-3" style="width: 8%;">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${not empty requisitions}">
                                    <c:forEach var="r" items="${requisitions}">
                                        <tr>
                                            <td>
                                                <span class="badge bg-light text-dark border font-monospace px-1 py-0.5" style="font-size: 0.75rem;">${r.code}</span>
                                                <div class="text-muted mt-0.5" style="font-size: 0.72rem;">
                                                    <fmt:formatDate value="${r.createdAt}" pattern="dd/MM/yyyy" />
                                                </div>
                                            </td>
                                            <td class="truncate-cell">
                                                <div class="fw-bold text-dark text-truncate" title="${r.positionTitle}">${r.positionTitle}</div>
                                                <small class="text-muted text-truncate d-block" title="${r.title}">${r.title}</small>
                                            </td>
                                            <td class="truncate-cell">
                                                <span class="fw-semibold text-secondary small" title="${r.departmentName}">
                                                    <i class="bi bi-building me-1 text-muted"></i>${r.departmentName != null ? r.departmentName : 'Chưa gán'}
                                                </span>
                                            </td>
                                            <td class="text-center">
                                                <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-1.5 py-1" style="font-size: 0.75rem;">
                                                    ${r.headcount}
                                                </span>
                                            </td>
                                            <td class="truncate-cell">
                                                <c:choose>
                                                    <c:when test="${r.recruitmentReason == 'REPLACEMENT'}">
                                                        <span class="badge bg-warning-subtle text-dark border border-warning-subtle text-nowrap" style="font-size: 0.72rem;">
                                                            <i class="bi bi-arrow-repeat me-1"></i>Thay thế
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-success-subtle text-success border border-success-subtle text-nowrap" style="font-size: 0.72rem;">
                                                            <i class="bi bi-person-plus me-1"></i>Tăng mới
                                                        </span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td class="truncate-cell">
                                                <div class="fw-semibold text-dark text-truncate" style="font-size: 0.82rem;" title="${r.formattedSalaryRange}">${r.formattedSalaryRange}</div>
                                                <c:if test="${not empty r.salaryExplanation}">
                                                    <span class="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle text-nowrap" style="font-size: 0.68rem;" title="Đã có văn bản giải trình lương vượt khung chuẩn">
                                                        <i class="bi bi-exclamation-triangle-fill text-warning me-0.5"></i>Giải trình
                                                    </span>
                                                </c:if>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${r.deadline != null}">
                                                        <span class="text-dark fw-semibold text-nowrap" style="font-size: 0.8rem;">
                                                            <i class="bi bi-calendar-event me-1 text-muted"></i><fmt:formatDate value="${r.deadline}" pattern="dd/MM/yyyy" />
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise><span class="text-muted small">--</span></c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td class="text-center">
                                                <span class="badge ${r.statusBadgeClass} text-nowrap cursor-pointer" 
                                                      style="font-size: 0.72rem; padding: 0.35rem 0.45rem; cursor: pointer;"
                                                      onclick="openApprovalModal('${r.id}')"
                                                      title="Nhấp để xem chuỗi cấp duyệt & Lịch sử">
                                                    ${r.statusLabel}
                                                </span>
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
                                                    <button type="button" class="btn btn-outline-info p-1 px-1.5" style="line-height: 1;" 
                                                            onclick="openApprovalModal('${r.id}')"
                                                            title="Nhật ký & Trạng thái chuỗi duyệt">
                                                        <i class="bi bi-diagram-3"></i>
                                                    </button>
                                                    <button type="button" class="btn btn-outline-secondary p-1 px-1.5" style="line-height: 1;" 
                                                            onclick="viewDetails('${r.id}', '${r.code}', '${r.positionTitle}', '${r.departmentName}', '${r.headcount}', '${r.recruitmentReasonLabel}', '${r.formattedSalaryRange}', '${r.deadline}', '${r.statusLabel}')"
                                                            title="Xem chi tiết JD & Yêu cầu">
                                                        <i class="bi bi-eye"></i>
                                                    </button>
                                                    <a href="${pageContext.request.contextPath}/recruitment-requests/edit?id=${r.id}" 
                                                       class="btn btn-outline-primary p-1 px-1.5" style="line-height: 1;" title="Chỉnh sửa">
                                                        <i class="bi bi-pencil"></i>
                                                    </a>
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
                <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Đóng</button>
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

<jsp:include page="../common/footer.jsp" />

<script>
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

function viewDetails(id, code, pos, dept, count, reason, sal, deadline, status) {
    currentDetailId = id;
    document.getElementById('modalTitle').textContent = pos;
    document.getElementById('modalCode').textContent = code + ' - ' + status;
    document.getElementById('modalPosition').textContent = pos;
    document.getElementById('modalDept').textContent = dept || 'Chưa gán';
    document.getElementById('modalHeadcount').textContent = count + ' nhân sự';
    document.getElementById('modalReason').textContent = reason;
    document.getElementById('modalSalary').textContent = sal;
    document.getElementById('modalDeadline').textContent = deadline || 'Chưa đặt';

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

    var modal = new bootstrap.Modal(document.getElementById('detailModal'));
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

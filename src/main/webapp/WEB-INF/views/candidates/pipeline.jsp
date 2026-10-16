<%--
  ==============================================================================
  GIAO DIỆN QUẢN LÝ PIPELINE TUYỂN DỤNG DẠNG KANBAN (candidates/pipeline.jsp)
  ==============================================================================
  Phục vụ Jira Issue KN-282: Quản lý Pipeline tuyển dụng dạng Kanban
  - 6 Giai đoạn chuẩn: Hồ sơ mới -> Sàng lọc -> Phỏng vấn -> Đề xuất offer -> Nhận việc -> Loại
  - Kéo thả chuyển giai đoạn đồng bộ thời gian thực với CSDL Backend
  - Hiển thị số lượng ứng viên chính xác trên từng cột
  - Bộ lọc vị trí tuyển dụng với dữ liệu thật, mặc định vị trí Recruiter phụ trách
  ==============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<c:set var="pageTitle" value="Pipeline Tuyển dụng dạng Kanban" />
<c:set var="breadcrumb" value="Pipeline Tuyển dụng" />
<c:set var="activeMenu" value="candidate-pipeline" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Tiêu đề trang, Breadcrumb & Nút chuyển đổi View (List vs Kanban) -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-3">
            <div>
                <h3 class="fw-bold text-dark mb-1">
                    <i class="bi bi-kanban-fill text-primary me-2"></i>Pipeline tuyển dụng (Kanban)
                </h3>
                <div class="text-muted small">
                    Hệ thống quản trị / Ứng viên &amp; Pipeline / Bảng điều phối Kanban
                </div>
            </div>
            <div class="d-flex align-items-center flex-wrap gap-2">
                <!-- Bộ nút chuyển đổi View giữa Danh sách và Kanban -->
                <div class="btn-group shadow-sm" role="group" aria-label="Chế độ xem">
                    <a href="${pageContext.request.contextPath}/candidates" 
                       class="btn btn-outline-primary btn-sm d-flex align-items-center gap-1"
                       title="Chuyển sang dạng bảng danh sách">
                        <i class="bi bi-list-ul"></i>
                        <span>Bảng danh sách</span>
                    </a>
                    <a href="${pageContext.request.contextPath}/candidates/pipeline" 
                       class="btn btn-primary active btn-sm d-flex align-items-center gap-1"
                       title="Đang ở chế độ xem bảng Kanban">
                        <i class="bi bi-kanban"></i>
                        <span>Bảng Kanban</span>
                    </a>
                </div>

                <a href="${pageContext.request.contextPath}/candidates/duplicates" 
                   class="btn btn-outline-warning btn-sm fw-semibold d-flex align-items-center gap-1 shadow-sm"
                   title="Xem hồ sơ nghi trùng">
                    <i class="bi bi-copy"></i>
                    <span>Hồ sơ trùng</span>
                </a>

                <a href="${pageContext.request.contextPath}/candidates" class="btn-ris-primary btn-sm text-decoration-none">
                    <i class="bi bi-person-plus-fill me-1"></i> Tiếp nhận ứng viên
                </a>
            </div>
        </div>

        <%-- [KN-304]: Thông báo phạm vi dữ liệu phân quyền Recruiter --%>
        <c:if test="${isRecruiterOnly}">
            <div class="alert alert-info d-flex align-items-center justify-content-between gap-3 py-2 px-3 mb-3 border border-info-subtle shadow-sm rounded-3"
                 style="background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);">
                <div class="d-flex align-items-center gap-2">
                    <i class="bi bi-shield-lock-fill text-info fs-5"></i>
                    <div class="text-info-emphasis small">
                        <strong>Phạm vi dữ liệu:</strong> Bạn đang quản lý pipeline của các vị trí tuyển dụng được phân công phụ trách. Hệ thống tự động chọn vị trí bạn đang đảm nhiệm.
                    </div>
                </div>
                <c:if test="${not empty currentRequisition}">
                    <span class="badge bg-primary text-white text-xs">
                        <i class="bi bi-check2-circle me-1"></i>${currentRequisition.code}
                    </span>
                </c:if>
            </div>
        </c:if>

        <!-- [KN-299]: Thanh điều khiển Toolbar - Bộ lọc vị trí tuyển dụng & Tìm kiếm -->
        <div class="kanban-toolbar-card">
            <div class="row g-3 align-items-center">
                <!-- Dropdown chọn vị trí tuyển dụng -->
                <div class="col-12 col-lg-5 col-xl-4">
                    <div class="d-flex align-items-center gap-2">
                        <label for="pipelineRequisitionSelect" class="form-label mb-0 fw-bold text-nowrap text-dark small">
                            <i class="bi bi-briefcase-fill text-primary me-1"></i>Vị trí:
                        </label>
                        <select id="pipelineRequisitionSelect" class="form-select form-select-sm kanban-req-select shadow-sm">
                            <c:if test="${!isRecruiterOnly}">
                                <option value="ALL" ${selectedRequisitionId == 'ALL' ? 'selected' : ''}>-- Tất cả vị trí tuyển dụng --</option>
                            </c:if>
                            <c:forEach var="req" items="${availableRequisitions}">
                                <option value="${req.id}" ${req.id == selectedRequisitionId ? 'selected' : ''}>
                                    [${req.code}] ${req.title} <c:if test="${not empty req.departmentName}">(${req.departmentName})</c:if>
                                </option>
                            </c:forEach>
                            <c:if test="${empty availableRequisitions}">
                                <option value="" disabled selected>Chưa có vị trí tuyển dụng nào</option>
                            </c:if>
                        </select>
                    </div>
                </div>

                <!-- Ô tìm kiếm ứng viên nhanh trên bảng Kanban -->
                <div class="col-12 col-md-6 col-lg-4 col-xl-4">
                    <div class="input-group input-group-sm shadow-sm">
                        <span class="input-group-text bg-white border-end-0 text-muted">
                            <i class="bi bi-search"></i>
                        </span>
                        <input type="text" id="pipelineSearchInput" class="form-control border-start-0 border-end-0" 
                               placeholder="Tìm theo tên, email, SĐT ứng viên..." autocomplete="off">
                        <button class="btn btn-outline-secondary border-start-0 bg-white" type="button" id="pipelineClearSearchBtn" title="Xóa tìm kiếm">
                            <i class="bi bi-x-lg text-muted"></i>
                        </button>
                    </div>
                </div>

                <!-- Thông số KPI và nút làm mới dữ liệu -->
                <div class="col-12 col-md-6 col-lg-3 col-xl-4 text-md-end">
                    <div class="d-flex align-items-center justify-content-md-end gap-2 flex-wrap">
                        <div class="badge bg-light text-dark border px-2 py-1 small">
                            Tổng: <strong id="summaryTotalCandidates" class="text-primary ms-1">0</strong>
                        </div>
                        <div class="badge bg-light text-dark border px-2 py-1 small">
                            Đang xử lý: <strong id="summaryActiveCandidates" class="text-success ms-1">0</strong>
                        </div>
                        <button id="pipelineReloadBtn" class="btn btn-sm btn-outline-secondary shadow-sm" type="button" title="Tải lại dữ liệu">
                            <i class="bi bi-arrow-clockwise"></i>
                        </button>
                    </div>
                </div>
            </div>

            <!-- Tên vị trí tuyển dụng hiện tại & hướng dẫn thao tác -->
            <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 pt-2 mt-2 border-top text-xs text-muted">
                <div id="summaryPositionTitle" class="d-flex align-items-center gap-1 text-dark fw-semibold">
                    <c:choose>
                        <c:when test="${not empty currentRequisition}">
                            <i class="bi bi-briefcase-fill text-primary me-1"></i>
                            ${currentRequisition.title} 
                            <span class="badge bg-light text-dark border ms-1">${currentRequisition.code}</span>
                            <span class="text-muted ms-2 fw-normal">Chỉ tiêu: ${currentRequisition.headcount} người</span>
                        </c:when>
                        <c:otherwise>
                            <i class="bi bi-grid-fill text-primary me-1"></i>Tất cả vị trí tuyển dụng
                        </c:otherwise>
                    </c:choose>
                </div>
                <div class="d-flex align-items-center gap-2 text-secondary">
                    <span><i class="bi bi-arrows-move me-1"></i>Kéo thả thẻ ứng viên giữa các cột để chuyển giai đoạn</span>
                </div>
            </div>
        </div>

        <!-- [KN-302]: Bảng Kanban 6 cột cuộn ngang -->
        <div class="kanban-page-wrapper">
            <!-- Loading Overlay khi tải API -->
            <div id="kanbanLoadingOverlay" class="kanban-loading-overlay" style="display: none;">
                <div class="spinner-border text-primary mb-2" role="status">
                    <span class="visually-hidden">Đang tải...</span>
                </div>
                <div class="text-dark fw-semibold small">Đang đồng bộ Pipeline ứng viên...</div>
            </div>

            <div class="kanban-board-container" id="kanbanBoard">
                <!-- 1. CỘT HỒ SƠ MỚI (APPLIED) -->
                <div class="kanban-column" data-stage="APPLIED">
                    <div class="kanban-column-header">
                        <div class="column-title-wrap">
                            <span class="stage-dot" style="background-color: #2563eb;"></span>
                            <h6 class="column-title">1. Hồ sơ mới</h6>
                        </div>
                        <span class="kanban-column-count badge bg-primary text-white">0</span>
                    </div>
                    <div class="kanban-cards-container" data-stage="APPLIED">
                        <div class="kanban-empty-state text-center py-4 px-2 text-muted">
                            <i class="bi bi-inbox fs-3 opacity-50 mb-1 d-block"></i>
                            <div class="text-xs">Đang tải dữ liệu...</div>
                        </div>
                    </div>
                </div>

                <!-- 2. CỘT SÀNG LỌC (SCREENING) -->
                <div class="kanban-column" data-stage="SCREENING">
                    <div class="kanban-column-header">
                        <div class="column-title-wrap">
                            <span class="stage-dot" style="background-color: #06b6d4;"></span>
                            <h6 class="column-title">2. Sàng lọc</h6>
                        </div>
                        <span class="kanban-column-count badge bg-info text-white">0</span>
                    </div>
                    <div class="kanban-cards-container" data-stage="SCREENING">
                        <div class="kanban-empty-state text-center py-4 px-2 text-muted">
                            <i class="bi bi-inbox fs-3 opacity-50 mb-1 d-block"></i>
                            <div class="text-xs">Đang tải dữ liệu...</div>
                        </div>
                    </div>
                </div>

                <!-- 3. CỘT PHỎNG VẤN (INTERVIEWING) -->
                <div class="kanban-column" data-stage="INTERVIEWING">
                    <div class="kanban-column-header">
                        <div class="column-title-wrap">
                            <span class="stage-dot" style="background-color: #f59e0b;"></span>
                            <h6 class="column-title">3. Phỏng vấn</h6>
                        </div>
                        <span class="kanban-column-count badge bg-warning text-dark">0</span>
                    </div>
                    <div class="kanban-cards-container" data-stage="INTERVIEWING">
                        <div class="kanban-empty-state text-center py-4 px-2 text-muted">
                            <i class="bi bi-inbox fs-3 opacity-50 mb-1 d-block"></i>
                            <div class="text-xs">Đang tải dữ liệu...</div>
                        </div>
                    </div>
                </div>

                <!-- 4. CỘT ĐỀ XUẤT OFFER (OFFER) -->
                <div class="kanban-column" data-stage="OFFER">
                    <div class="kanban-column-header">
                        <div class="column-title-wrap">
                            <span class="stage-dot" style="background-color: #8b5cf6;"></span>
                            <h6 class="column-title">4. Đề xuất offer</h6>
                        </div>
                        <span class="kanban-column-count badge bg-purple text-white">0</span>
                    </div>
                    <div class="kanban-cards-container" data-stage="OFFER">
                        <div class="kanban-empty-state text-center py-4 px-2 text-muted">
                            <i class="bi bi-inbox fs-3 opacity-50 mb-1 d-block"></i>
                            <div class="text-xs">Đang tải dữ liệu...</div>
                        </div>
                    </div>
                </div>

                <!-- 5. CỘT NHẬN VIỆC (HIRED) -->
                <div class="kanban-column" data-stage="HIRED">
                    <div class="kanban-column-header">
                        <div class="column-title-wrap">
                            <span class="stage-dot" style="background-color: #10b981;"></span>
                            <h6 class="column-title">5. Nhận việc</h6>
                        </div>
                        <span class="kanban-column-count badge bg-success text-white">0</span>
                    </div>
                    <div class="kanban-cards-container" data-stage="HIRED">
                        <div class="kanban-empty-state text-center py-4 px-2 text-muted">
                            <i class="bi bi-inbox fs-3 opacity-50 mb-1 d-block"></i>
                            <div class="text-xs">Đang tải dữ liệu...</div>
                        </div>
                    </div>
                </div>

                <!-- 6. CỘT LOẠI (REJECTED) -->
                <div class="kanban-column" data-stage="REJECTED">
                    <div class="kanban-column-header">
                        <div class="column-title-wrap">
                            <span class="stage-dot" style="background-color: #ef4444;"></span>
                            <h6 class="column-title">6. Loại</h6>
                        </div>
                        <span class="kanban-column-count badge bg-danger text-white">0</span>
                    </div>
                    <div class="kanban-cards-container" data-stage="REJECTED">
                        <div class="kanban-empty-state text-center py-4 px-2 text-muted">
                            <i class="bi bi-inbox fs-3 opacity-50 mb-1 d-block"></i>
                            <div class="text-xs">Đang tải dữ liệu...</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</main>

<!-- Modal Xác nhận lý do loại khi chuyển vào cột "Loại" (KN-282 & KN-283 / KN-307) -->
<div class="modal fade" id="rejectionReasonModal" tabindex="-1" aria-labelledby="rejectionReasonModalLabel" aria-hidden="true" data-bs-backdrop="static">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content shadow-lg border-0 rounded-4">
            <div class="modal-header bg-danger-subtle border-0 py-3">
                <h5 class="modal-title fw-bold text-danger d-flex align-items-center gap-2 fs-6" id="rejectionReasonModalLabel">
                    <i class="bi bi-exclamation-octagon-fill"></i> Xác nhận loại hồ sơ ứng viên
                </h5>
                <button type="button" class="btn-close shadow-none" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4">
                <div class="mb-3">
                    <div class="text-muted small mb-1">Ứng viên:</div>
                    <div class="fw-bold fs-6 text-dark" id="rejectModalCandidateName">-</div>
                    <div class="text-secondary small" id="rejectModalPositionTitle">-</div>
                </div>

                <div class="alert alert-warning py-2 px-3 small border border-warning-subtle rounded-3 mb-3">
                    <i class="bi bi-info-circle-fill me-1"></i>
                    Chuyển ứng viên sang giai đoạn <strong>"Loại"</strong> yêu cầu phải chọn lý do chính thức để phục vụ báo cáo và kiểm toán tuyển dụng.
                </div>

                <div class="mb-3">
                    <label for="rejectionReasonSelect" class="form-label fw-semibold small text-dark">
                        Lý do loại hồ sơ <span class="text-danger">*</span>
                    </label>
                    <select id="rejectionReasonSelect" class="form-select form-select-sm">
                        <option value="">-- Chọn lý do loại hồ sơ --</option>
                        <c:forEach var="item" items="${rejectionReasons}">
                            <option value="${item.id}">${item.value}</option>
                        </c:forEach>
                    </select>
                    <div id="rejectionReasonError" class="text-danger small mt-1" style="display: none;">
                        Vui lòng chọn một lý do loại hợp lệ từ danh mục!
                    </div>
                </div>
            </div>
            <div class="modal-footer border-0 bg-light py-2 px-4 rounded-bottom-4">
                <button type="button" class="btn btn-sm btn-outline-secondary px-3" data-bs-dismiss="modal">Hủy bỏ</button>
                <button type="button" class="btn btn-sm btn-danger px-3 fw-bold" id="confirmRejectBtn">
                    <i class="bi bi-x-circle-fill me-1"></i> Xác nhận loại
                </button>
            </div>
        </div>
    </div>
</div>

<!-- Modal Cảnh báo khi người dùng không đủ quyền bỏ qua Phỏng vấn (KN-312 - Recruiter) -->
<div class="modal fade" id="skipInterviewWarningModal" tabindex="-1" aria-labelledby="skipInterviewWarningModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content shadow-lg border-0 rounded-4">
            <div class="modal-header bg-warning-subtle border-0 py-3">
                <h5 class="modal-title fw-bold text-dark d-flex align-items-center gap-2 fs-6" id="skipInterviewWarningModalLabel">
                    <i class="bi bi-exclamation-triangle-fill text-warning"></i> Cảnh báo: Yêu cầu qua giai đoạn Phỏng vấn
                </h5>
                <button type="button" class="btn-close shadow-none" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4">
                <div class="d-flex align-items-start gap-3 mb-3">
                    <div class="rounded-circle p-2 bg-warning bg-opacity-10 text-warning flex-shrink-0" style="width:44px; height:44px; display:flex; align-items:center; justify-content:center;">
                        <i class="bi bi-shield-lock-fill fs-5"></i>
                    </div>
                    <div>
                        <div class="fw-bold fs-6 text-dark" id="skipWarningCandidateName">-</div>
                        <div class="text-secondary small mb-2" id="skipWarningPositionTitle">-</div>
                        <div class="d-flex align-items-center gap-2 mb-2">
                            <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle" id="skipWarningSourceStage">-</span>
                            <i class="bi bi-arrow-right text-muted"></i>
                            <span class="badge bg-purple text-white" id="skipWarningTargetStage">Đề xuất offer</span>
                        </div>
                    </div>
                </div>

                <div class="alert alert-danger py-2 px-3 small border border-danger-subtle rounded-3 mb-0">
                    <i class="bi bi-slash-circle me-1"></i>
                    <strong>Không được phép bỏ qua giai đoạn Phỏng vấn:</strong><br>
                    Quy trình tuyển dụng yêu cầu ứng viên phải qua giai đoạn <strong>Phỏng vấn</strong> trước khi tiến hành <strong>Đề xuất offer</strong> hoặc <strong>Nhận việc</strong>.<br>
                    <span class="text-muted mt-1 d-block">Bạn không có thẩm quyền đặc cách. Vui lòng chuyển ứng viên sang giai đoạn <strong>Phỏng vấn</strong> hoặc liên hệ <strong>Trưởng phòng Nhân sự (HR Manager)</strong> để được xem xét.</span>
                </div>
            </div>
            <div class="modal-footer border-0 bg-light py-2 px-4 rounded-bottom-4">
                <button type="button" class="btn btn-sm btn-primary px-4 fw-semibold" data-bs-dismiss="modal">
                    <i class="bi bi-check2 me-1"></i> Đã hiểu
                </button>
            </div>
        </div>
    </div>
</div>

<!-- Modal Xác nhận của Trưởng phòng Nhân sự khi đặc cách bỏ qua Phỏng vấn (KN-310 & KN-312) -->
<div class="modal fade" id="skipInterviewApprovalModal" tabindex="-1" aria-labelledby="skipInterviewApprovalModalLabel" aria-hidden="true" data-bs-backdrop="static">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content shadow-lg border-0 rounded-4">
            <div class="modal-header bg-primary-subtle border-0 py-3">
                <h5 class="modal-title fw-bold text-primary d-flex align-items-center gap-2 fs-6" id="skipInterviewApprovalModalLabel">
                    <i class="bi bi-shield-check"></i> Xác nhận Trưởng phòng Nhân sự — Bỏ qua Phỏng vấn
                </h5>
                <button type="button" class="btn-close shadow-none" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4">
                <div class="mb-3">
                    <div class="text-muted small mb-1">Ứng viên:</div>
                    <div class="fw-bold fs-6 text-dark" id="skipApprovalCandidateName">-</div>
                    <div class="text-secondary small" id="skipApprovalPositionTitle">-</div>
                    <div class="d-flex align-items-center gap-2 mt-2">
                        <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle" id="skipApprovalSourceStage">-</span>
                        <i class="bi bi-arrow-right text-muted"></i>
                        <span class="badge bg-purple text-white" id="skipApprovalTargetStage">Đề xuất offer</span>
                    </div>
                </div>

                <div class="alert alert-warning py-2 px-3 small border border-warning-subtle rounded-3 mb-3">
                    <i class="bi bi-exclamation-circle-fill me-1 text-warning"></i>
                    Ứng viên hiện <strong>chưa qua giai đoạn Phỏng vấn</strong>. Với thẩm quyền <strong>Trưởng phòng Nhân sự</strong>, bạn có xác nhận đặc cách chuyển thẳng sang giai đoạn này không?
                </div>

                <div class="mb-3">
                    <label for="skipInterviewNoteInput" class="form-label fw-semibold small text-dark">
                        Lý do đặc cách / Ghi chú phê duyệt <span class="text-muted">(tùy chọn)</span>
                    </label>
                    <textarea id="skipInterviewNoteInput" class="form-control form-control-sm" rows="3"
                              placeholder="Nhập lý do đặc cách (ví dụ: Đã phỏng vấn trực tiếp ngoài quy trình, ứng viên chuyển công tác nội bộ, v.v.)..."></textarea>
                    <div class="form-text text-muted" style="font-size: 0.75rem;">
                        Thông tin xác nhận và ghi chú sẽ được lưu vĩnh viễn trong nhật ký hoạt động ứng viên và audit log.
                    </div>
                </div>
            </div>
            <div class="modal-footer border-0 bg-light py-2 px-4 rounded-bottom-4">
                <button type="button" class="btn btn-sm btn-outline-secondary px-3" data-bs-dismiss="modal">Hủy bỏ</button>
                <button type="button" class="btn btn-sm btn-primary px-3 fw-bold" id="confirmSkipInterviewBtn">
                    <i class="bi bi-check-circle-fill me-1"></i> Xác nhận đặc cách
                </button>
            </div>
        </div>
    </div>
</div>

<jsp:include page="../common/footer.jsp" />

<!-- Nạp tệp logic xử lý bảng Kanban -->
<script src="${pageContext.request.contextPath}/assets/js/kanban.js"></script>
<script>
    document.addEventListener('DOMContentLoaded', function () {
        IrmsKanban.init({
            contextPath: '${pageContext.request.contextPath}',
            initialRequisitionId: '${selectedRequisitionId}',
            isRecruiterOnly: ${isRecruiterOnly ? 'true' : 'false'},
            canApproveSkipInterview: ${currentUser.hasRole('HR_MANAGER') || currentUser.hasRole('ADMIN')}
        });
    });
</script>

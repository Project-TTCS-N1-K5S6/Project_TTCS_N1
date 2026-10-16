<%--
  ==============================================================================
  GIAO DIỆN QUẢN LÝ CẢNH BÁO & HỒ SƠ TRÙNG LẶP (candidates/duplicates.jsp)
  Chức năng:
  - Danh sách các cặp hồ sơ nghi trùng theo Email hoặc Số điện thoại.
  - Bộ lọc theo trạng thái: Chờ xử lý (PENDING), Tạm hoãn nhắc lại (SNOOZED),
    Đã hợp nhất (MERGED), Đã bỏ qua (DISMISSED).
  - Đối chiếu nhanh và nút mở màn hình so sánh chi tiết & hợp nhất (Merge).
  - Hỗ trợ thao tác hoãn nhắc lại theo cấu hình ngày (Snooze) và bỏ qua cảnh báo.
  ==============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Cảnh báo & Phát hiện hồ sơ trùng lặp" />
<c:set var="breadcrumb" value="Hồ sơ trùng lặp" />
<c:set var="activeMenu" value="candidate-duplicates" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Tiêu đề trang & Điều hướng -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <h3 class="fw-bold text-dark mb-1">
                    <i class="bi bi-exclamation-triangle-fill text-warning me-2"></i>Cảnh báo & Phát hiện hồ sơ trùng lặp
                </h3>
                <div class="text-muted small">Hệ thống quản trị / Hồ sơ ứng viên / Quản lý hồ sơ trùng lặp</div>
            </div>
            <div class="d-flex gap-2">
                <a href="${pageContext.request.contextPath}/candidates" class="btn btn-outline-secondary">
                    <i class="bi bi-arrow-left me-1"></i> Danh sách ứng viên
                </a>
            </div>
        </div>

        <!-- 4 Thẻ KPI thống kê cảnh báo -->
        <div class="row g-3 mb-4">
            <div class="col-12 col-sm-6 col-lg-3">
                <div class="ris-cand-kpi-card shadow-sm border-0" style="border-radius: 12px; border-left: 4px solid #f59e0b !important;">
                    <div class="cand-top">
                        <div>
                            <span class="cand-num text-warning">${pendingCount}</span>
                            <span class="cand-unit">Hồ sơ</span>
                        </div>
                        <div class="cand-icon text-warning"><i class="bi bi-hourglass-split"></i></div>
                    </div>
                    <div class="cand-title fw-semibold">Cần xử lý ngay</div>
                    <div class="cand-date text-muted small">Đang chờ đối chiếu hợp nhất</div>
                </div>
            </div>

            <div class="col-12 col-sm-6 col-lg-3">
                <div class="ris-cand-kpi-card shadow-sm border-0" style="border-radius: 12px; border-left: 4px solid #06b6d4 !important;">
                    <div class="cand-top">
                        <div>
                            <span class="cand-num text-info">
                                <c:set var="snoozedTotal" value="0" />
                                <c:forEach var="w" items="${warnings}">
                                    <c:if test="${w.status == 'SNOOZED'}"><c:set var="snoozedTotal" value="${snoozedTotal + 1}" /></c:if>
                                </c:forEach>
                                ${snoozedTotal}
                            </span>
                            <span class="cand-unit">Cặp hồ sơ</span>
                        </div>
                        <div class="cand-icon text-info"><i class="bi bi-bell-slash"></i></div>
                    </div>
                    <div class="cand-title fw-semibold">Đã hoãn nhắc lại</div>
                    <div class="cand-date text-muted small">Sẽ nhắc lại theo lịch cấu hình</div>
                </div>
            </div>

            <div class="col-12 col-sm-6 col-lg-3">
                <div class="ris-cand-kpi-card shadow-sm border-0" style="border-radius: 12px; border-left: 4px solid #10b981 !important;">
                    <div class="cand-top">
                        <div>
                            <span class="cand-num text-success">
                                <c:set var="mergedTotal" value="0" />
                                <c:forEach var="w" items="${warnings}">
                                    <c:if test="${w.status == 'MERGED'}"><c:set var="mergedTotal" value="${mergedTotal + 1}" /></c:if>
                                </c:forEach>
                                ${mergedTotal}
                            </span>
                            <span class="cand-unit">Đã gộp</span>
                        </div>
                        <div class="cand-icon text-success"><i class="bi bi-check2-all"></i></div>
                    </div>
                    <div class="cand-title fw-semibold">Đã hợp nhất hoàn tất</div>
                    <div class="cand-date text-muted small">Toàn bộ lịch sử được bảo toàn</div>
                </div>
            </div>

            <div class="col-12 col-sm-6 col-lg-3">
                <div class="ris-cand-kpi-card shadow-sm border-0" style="border-radius: 12px; border-left: 4px solid #64748b !important;">
                    <div class="cand-top">
                        <div>
                            <span class="cand-num text-secondary">${totalCount}</span>
                            <span class="cand-unit">Tổng cộng</span>
                        </div>
                        <div class="cand-icon text-secondary"><i class="bi bi-intersect"></i></div>
                    </div>
                    <div class="cand-title fw-semibold">Tổng số cảnh báo</div>
                    <div class="cand-date text-muted small">Tất cả các trạng thái</div>
                </div>
            </div>
        </div>

        <!-- Bảng danh sách cảnh báo trùng lặp -->
        <div class="card shadow-sm border mb-4" style="border-radius: 12px;">
            <div class="card-header bg-white border-bottom py-3 d-flex flex-wrap align-items-center justify-content-between gap-3">
                <div class="d-flex align-items-center gap-2">
                    <ul class="nav nav-pills gap-1">
                        <li class="nav-item">
                            <a class="nav-link py-1 px-3 rounded-pill ${paramStatus == 'PENDING' ? 'active bg-warning text-dark fw-bold' : 'text-secondary'}"
                               href="${pageContext.request.contextPath}/candidates/duplicates?status=PENDING">
                                Đang chờ (<span id="badgePendingCount">${pendingCount}</span>)
                            </a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link py-1 px-3 rounded-pill ${paramStatus == 'SNOOZED' ? 'active bg-info text-dark fw-bold' : 'text-secondary'}"
                               href="${pageContext.request.contextPath}/candidates/duplicates?status=SNOOZED">
                                Nhắc lại sau
                            </a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link py-1 px-3 rounded-pill ${paramStatus == 'MERGED' ? 'active bg-success text-white fw-bold' : 'text-secondary'}"
                               href="${pageContext.request.contextPath}/candidates/duplicates?status=MERGED">
                                Đã hợp nhất
                            </a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link py-1 px-3 rounded-pill ${paramStatus == 'DISMISSED' ? 'active bg-secondary text-white fw-bold' : 'text-secondary'}"
                               href="${pageContext.request.contextPath}/candidates/duplicates?status=DISMISSED">
                                Đã bỏ qua
                            </a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link py-1 px-3 rounded-pill ${paramStatus == 'ALL' ? 'active bg-dark text-white fw-bold' : 'text-secondary'}"
                               href="${pageContext.request.contextPath}/candidates/duplicates?status=ALL">
                                Tất cả
                            </a>
                        </li>
                    </ul>
                </div>
                <div class="d-flex flex-wrap align-items-center gap-2">
                    <form action="${pageContext.request.contextPath}/candidates/duplicates" method="GET" class="d-flex gap-2">
                        <input type="hidden" name="status" value="${paramStatus}" />
                        <div class="input-group input-group-sm" style="min-width: 250px;">
                            <input type="text" class="form-control" name="search" value="${paramSearch}" placeholder="Tìm theo tên, email, SĐT..." />
                            <button class="btn btn-outline-secondary" type="submit"><i class="bi bi-search"></i></button>
                        </div>
                    </form>
                </div>
            </div>

            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th class="ps-3" style="width: 32%;">Hồ sơ A</th>
                                <th style="width: 32%;">Hồ sơ B</th>
                                <th style="width: 16%;">Tiêu chí trùng</th>
                                <th style="width: 10%;">Trạng thái</th>
                                <th class="text-end pe-3" style="width: 10%;">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${warnings}">
                                <tr>
                                    <!-- Cột Hồ sơ A -->
                                    <td class="ps-3 py-3">
                                        <div class="d-flex align-items-start gap-2">
                                            <div class="rounded-circle bg-primary bg-opacity-10 text-primary d-flex align-items-center justify-content-center fw-bold"
                                                 style="width: 38px; height: 38px; min-width: 38px;">
                                                ${item.candidate1 != null && item.candidate1.fullName.length() > 0 ? item.candidate1.fullName.substring(0, 1) : 'A'}
                                            </div>
                                            <div>
                                                <div class="fw-bold text-dark">
                                                    <a href="${pageContext.request.contextPath}/candidates/profile?id=${item.candidateId1}" class="text-decoration-none text-dark">
                                                        <c:out value="${item.candidate1 != null ? item.candidate1.fullName : item.candidateId1}" />
                                                    </a>
                                                </div>
                                                <div class="text-muted small">
                                                    <i class="bi bi-envelope me-1"></i><c:out value="${item.candidate1 != null ? item.candidate1.email : ''}" />
                                                </div>
                                                <c:if test="${not empty item.candidate1.phone}">
                                                    <div class="text-muted small">
                                                        <i class="bi bi-telephone me-1"></i><c:out value="${item.candidate1.phone}" />
                                                    </div>
                                                </c:if>
                                                <div class="mt-1">
                                                    <span class="badge bg-light text-secondary border">
                                                        <c:out value="${item.candidate1 != null && not empty item.candidate1.requisitionTitle ? item.candidate1.requisitionTitle : 'Chưa gắn vị trí'}" />
                                                    </span>
                                                    <span class="badge ${item.candidate1 != null ? item.candidate1.statusBadgeClass : 'bg-secondary'} ms-1">
                                                        ${item.candidate1 != null ? item.candidate1.statusLabel : ''}
                                                    </span>
                                                </div>
                                            </div>
                                        </div>
                                    </td>

                                    <!-- Cột Hồ sơ B -->
                                    <td class="py-3">
                                        <div class="d-flex align-items-start gap-2">
                                            <div class="rounded-circle bg-warning bg-opacity-10 text-warning d-flex align-items-center justify-content-center fw-bold"
                                                 style="width: 38px; height: 38px; min-width: 38px;">
                                                ${item.candidate2 != null && item.candidate2.fullName.length() > 0 ? item.candidate2.fullName.substring(0, 1) : 'B'}
                                            </div>
                                            <div>
                                                <div class="fw-bold text-dark">
                                                    <a href="${pageContext.request.contextPath}/candidates/profile?id=${item.candidateId2}" class="text-decoration-none text-dark">
                                                        <c:out value="${item.candidate2 != null ? item.candidate2.fullName : item.candidateId2}" />
                                                    </a>
                                                </div>
                                                <div class="text-muted small">
                                                    <i class="bi bi-envelope me-1"></i><c:out value="${item.candidate2 != null ? item.candidate2.email : ''}" />
                                                </div>
                                                <c:if test="${not empty item.candidate2.phone}">
                                                    <div class="text-muted small">
                                                        <i class="bi bi-telephone me-1"></i><c:out value="${item.candidate2.phone}" />
                                                    </div>
                                                </c:if>
                                                <div class="mt-1">
                                                    <span class="badge bg-light text-secondary border">
                                                        <c:out value="${item.candidate2 != null && not empty item.candidate2.requisitionTitle ? item.candidate2.requisitionTitle : 'Chưa gắn vị trí'}" />
                                                    </span>
                                                    <span class="badge ${item.candidate2 != null ? item.candidate2.statusBadgeClass : 'bg-secondary'} ms-1">
                                                        ${item.candidate2 != null ? item.candidate2.statusLabel : ''}
                                                    </span>
                                                </div>
                                            </div>
                                        </div>
                                    </td>

                                    <!-- Cột Tiêu chí trùng -->
                                    <td>
                                        <span class="badge ${item.matchTypeBadgeClass} mb-1">
                                            <c:out value="${item.matchTypeLabel}" />
                                        </span>
                                        <div class="text-muted text-xs font-monospace">
                                            <c:out value="${item.matchValue}" />
                                        </div>
                                        <div class="text-muted small mt-1">
                                            <i class="bi bi-clock me-1"></i><fmt:formatDate value="${item.createdAt}" pattern="dd/MM/yyyy HH:mm" />
                                        </div>
                                    </td>

                                    <!-- Cột Trạng thái & Lịch nhắc -->
                                    <td>
                                        <span class="badge ${item.statusBadgeClass}">
                                            ${item.statusLabel}
                                        </span>
                                        <c:if test="${item.status == 'SNOOZED'}">
                                            <div class="text-info small mt-1">
                                                <i class="bi bi-alarm me-1"></i>Nhắc lại: <fmt:formatDate value="${item.snoozeUntil}" pattern="dd/MM/yyyy" />
                                                <span class="badge bg-light text-dark border ms-1">Lần ${item.reminderCount}</span>
                                            </div>
                                        </c:if>
                                        <c:if test="${item.status == 'MERGED'}">
                                            <div class="text-success small mt-1">
                                                <i class="bi bi-check-circle me-1"></i>Đã gộp hồ sơ
                                            </div>
                                        </c:if>
                                        <c:if test="${item.status == 'DISMISSED'}">
                                            <div class="text-muted small mt-1">
                                                <i class="bi bi-slash-circle me-1"></i>Đã xác nhận bỏ qua
                                            </div>
                                        </c:if>
                                    </td>

                                    <!-- Cột Thao tác -->
                                    <td class="text-end pe-3">
                                        <c:choose>
                                            <c:when test="${item.status == 'MERGED'}">
                                                <div class="d-inline-flex gap-1 align-items-center">
                                                    <a href="${pageContext.request.contextPath}/candidates/profile?id=${item.candidateId1}"
                                                       class="btn btn-sm btn-outline-success" title="Xem hồ sơ hợp nhất">
                                                        <i class="bi bi-person-check me-1"></i>Xem hồ sơ
                                                    </a>
                                                    <c:if test="${canManage && not empty item.mergeHistory && item.mergeHistory.canUndo}">
                                                        <button type="button" class="btn btn-sm btn-outline-danger"
                                                                onclick="openUndoMergeModal('${item.mergeHistory.id}', '<c:out value="${item.candidate1.fullName}" />', '<c:out value="${item.candidate2.fullName}" />', '${item.mergeHistory.remainingUndoText}')"
                                                                title="Hoàn tác hợp nhất trong vòng 24 giờ (${item.mergeHistory.remainingUndoText})">
                                                            <i class="bi bi-arrow-counterclockwise me-1"></i>Hoàn tác
                                                        </button>
                                                    </c:if>
                                                    <c:if test="${not empty item.mergeHistory && item.mergeHistory.undone}">
                                                        <span class="badge bg-secondary-subtle text-secondary border small">Đã hoàn tác</span>
                                                    </c:if>
                                                </div>
                                            </c:when>
                                            <c:otherwise>
                                                <div class="d-inline-flex gap-1 align-items-center">
                                                    <a href="${pageContext.request.contextPath}/candidates/duplicates/compare?c1=${item.candidateId1}&c2=${item.candidateId2}&warningId=${item.id}"
                                                       class="btn btn-sm btn-primary" title="So sánh & Hợp nhất">
                                                        <i class="bi bi-arrow-left-right me-1"></i>Đối chiếu
                                                    </a>

                                                    <!-- Dropdown thao tác mở rộng: Hoãn nhắc lại hoặc Bỏ qua -->
                                                    <div class="dropdown">
                                                        <button class="btn btn-sm btn-outline-secondary dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                                                            <i class="bi bi-three-dots"></i>
                                                        </button>
                                                        <ul class="dropdown-menu dropdown-menu-end shadow-sm">
                                                            <li><h6 class="dropdown-header">Cấu hình nhắc lại</h6></li>
                                                            <li>
                                                                <button class="dropdown-item" type="button" onclick="openSnoozeModal('${item.id}', 1)">
                                                                    <i class="bi bi-clock me-2 text-info"></i>Nhắc lại sau 1 ngày
                                                                </button>
                                                            </li>
                                                            <li>
                                                                <button class="dropdown-item" type="button" onclick="openSnoozeModal('${item.id}', 3)">
                                                                    <i class="bi bi-clock-history me-2 text-info"></i>Nhắc lại sau 3 ngày
                                                                </button>
                                                            </li>
                                                            <li>
                                                                <button class="dropdown-item" type="button" onclick="openSnoozeModal('${item.id}', 7)">
                                                                    <i class="bi bi-calendar-event me-2 text-info"></i>Nhắc lại sau 7 ngày
                                                                </button>
                                                            </li>
                                                            <li><hr class="dropdown-divider"></li>
                                                            <li>
                                                                <button class="dropdown-item text-danger" type="button" onclick="openDismissModal('${item.id}')">
                                                                    <i class="bi bi-x-circle me-2"></i>Bỏ qua (Không phải trùng)
                                                                </button>
                                                            </li>
                                                        </ul>
                                                    </div>
                                                </div>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>

                            <c:if test="${empty warnings}">
                                <tr>
                                    <td colspan="5" class="text-center py-5 text-muted">
                                        <i class="bi bi-shield-check text-success fs-1 d-block mb-2"></i>
                                        <div class="fw-bold fs-6 text-dark">Không có cảnh báo trùng lặp nào</div>
                                        <div class="small">Tất cả các hồ sơ ứng viên hiện tại đã sạch và không phát hiện trùng email hoặc số điện thoại.</div>
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <!-- Phân trang -->
                <c:if test="${totalPages > 1}">
                    <div class="p-3 border-top d-flex flex-wrap justify-content-between align-items-center gap-2">
                        <small class="text-muted">Hiển thị trang <strong>${currentPage}</strong> / <strong>${totalPages}</strong> (Tổng cộng <strong>${totalCount}</strong> cảnh báo)</small>
                        <nav>
                            <ul class="pagination pagination-sm mb-0">
                                <li class="page-item ${currentPage <= 1 ? 'disabled' : ''}">
                                    <a class="page-link" href="${pageContext.request.contextPath}/candidates/duplicates?page=${currentPage - 1}&search=${paramSearch}&status=${paramStatus}">
                                        <i class="bi bi-chevron-left"></i>
                                    </a>
                                </li>
                                <c:forEach begin="1" end="${totalPages}" var="p">
                                    <li class="page-item ${currentPage == p ? 'active' : ''}">
                                        <a class="page-link" href="${pageContext.request.contextPath}/candidates/duplicates?page=${p}&search=${paramSearch}&status=${paramStatus}">${p}</a>
                                    </li>
                                </c:forEach>
                                <li class="page-item ${currentPage >= totalPages ? 'disabled' : ''}">
                                    <a class="page-link" href="${pageContext.request.contextPath}/candidates/duplicates?page=${currentPage + 1}&search=${paramSearch}&status=${paramStatus}">
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

<!-- Modal Hoãn nhắc lại (Snooze) -->
<div class="modal fade" id="snoozeModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-sm">
        <div class="modal-content">
            <form action="${pageContext.request.contextPath}/candidates/duplicates/snooze" method="POST">
                <input type="hidden" name="warningId" id="snoozeWarningId" />
                <div class="modal-header py-2">
                    <h6 class="modal-title fw-bold"><i class="bi bi-clock-history text-info me-2"></i>Hoãn nhắc lại cảnh báo</h6>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <label class="form-label small fw-semibold">Thời gian nhắc lại sau:</label>
                    <select class="form-select" name="days" id="snoozeDaysSelect">
                        <option value="1">1 ngày tới</option>
                        <option value="3" selected>3 ngày tới</option>
                        <option value="7">7 ngày tới</option>
                        <option value="14">14 ngày tới</option>
                    </select>
                </div>
                <div class="modal-footer py-2">
                    <button type="button" class="btn btn-sm btn-secondary" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-sm btn-info text-white"><i class="bi bi-check2"></i> Xác nhận hoãn</button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Modal Bỏ qua cảnh báo (Dismiss) -->
<div class="modal fade" id="dismissModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <form action="${pageContext.request.contextPath}/candidates/duplicates/dismiss" method="POST">
                <input type="hidden" name="warningId" id="dismissWarningId" />
                <div class="modal-header py-2">
                    <h6 class="modal-title fw-bold text-danger"><i class="bi bi-x-circle me-2"></i>Bỏ qua cảnh báo trùng lặp</h6>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <p class="small text-muted mb-2">
                        Bạn xác nhận đây là hai ứng viên riêng biệt (không trùng lặp)? Cảnh báo này sẽ được ẩn khỏi danh sách chờ xử lý.
                    </p>
                    <label class="form-label small fw-semibold">Lý do xác nhận bỏ qua (tùy chọn):</label>
                    <textarea class="form-control" name="notes" rows="2" placeholder="Ví dụ: Hai ứng viên có cùng tên nhưng ứng tuyển chi nhánh khác nhau..."></textarea>
                </div>
                <div class="modal-footer py-2">
                    <button type="button" class="btn btn-sm btn-secondary" data-bs-dismiss="modal">Đóng</button>
                    <button type="submit" class="btn btn-sm btn-danger"><i class="bi bi-check2"></i> Xác nhận bỏ qua</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
function openSnoozeModal(warningId, days) {
    document.getElementById('snoozeWarningId').value = warningId;
    if (days) document.getElementById('snoozeDaysSelect').value = days;
    new bootstrap.Modal(document.getElementById('snoozeModal')).show();
}

function openDismissModal(warningId) {
    document.getElementById('dismissWarningId').value = warningId;
    new bootstrap.Modal(document.getElementById('dismissModal')).show();
}

function openUndoMergeModal(historyId, primaryName, secondaryName, remainingText) {
    document.getElementById('undoHistoryId').value = historyId;
    document.getElementById('undoPrimaryName').innerText = primaryName || 'Hồ sơ A';
    document.getElementById('undoSecondaryName').innerText = secondaryName || 'Hồ sơ B';
    document.getElementById('undoRemainingTimeText').innerText = 'Thời hạn: ' + (remainingText || 'Trong 24 giờ');
    new bootstrap.Modal(document.getElementById('undoModal')).show();
}
</script>

<!-- Modal Hoàn tác hợp nhất trong 24 giờ -->
<div class="modal fade" id="undoModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <form action="${pageContext.request.contextPath}/candidates/duplicates/undo" method="POST">
                <input type="hidden" name="historyId" id="undoHistoryId" />
                <input type="hidden" name="redirectUrl" value="${pageContext.request.contextPath}/candidates/duplicates?status=MERGED" />
                <div class="modal-header bg-danger text-white py-3">
                    <h6 class="modal-title fw-bold"><i class="bi bi-arrow-counterclockwise me-2"></i>Xác nhận hoàn tác hợp nhất hồ sơ</h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="alert alert-warning small mb-3">
                        <i class="bi bi-exclamation-triangle-fill me-1"></i>
                        Hệ thống chỉ cho phép hoàn tác hợp nhất trong vòng <strong>24 giờ</strong>.
                        <div id="undoRemainingTimeText" class="mt-1 fw-bold text-danger"></div>
                    </div>
                    <p class="small text-secondary mb-3">
                        Hồ sơ phụ <strong id="undoSecondaryName"></strong> sẽ được khôi phục thành hồ sơ độc lập,
                        hồ sơ chính <strong id="undoPrimaryName"></strong> sẽ trở lại dữ liệu ban đầu
                        và cảnh báo nghi trùng sẽ được mở lại để kiểm tra.
                    </p>
                    <div class="mb-3">
                        <label class="form-label small fw-semibold text-dark">Lý do hoàn tác (tùy chọn):</label>
                        <textarea class="form-control form-control-sm" name="undoNotes" rows="2" placeholder="Ví dụ: Nhầm lẫn khi đối chiếu..."></textarea>
                    </div>
                </div>
                <div class="modal-footer border-0 px-4 pb-4">
                    <button type="button" class="btn btn-secondary rounded-pill px-4" data-bs-dismiss="modal">Đóng</button>
                    <button type="submit" class="btn btn-danger rounded-pill px-4 fw-bold">
                        <i class="bi bi-arrow-counterclockwise me-1"></i> Xác nhận hoàn tác
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<jsp:include page="../common/footer.jsp" />

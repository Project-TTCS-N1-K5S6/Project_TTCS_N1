<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Chi tiết Khung năng lực: ${framework.code}" />
<c:set var="breadcrumb" value="Chi tiết khung năng lực" />
<c:set var="activeMenu" value="competencies" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Header chi tiết & Các nút hành động -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <nav aria-label="breadcrumb">
                    <ol class="breadcrumb mb-1 small">
                        <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/dashboard">Tổng quan</a></li>
                        <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/competency-frameworks">Khung năng lực</a></li>
                        <li class="breadcrumb-item active" aria-current="page">${framework.code}</li>
                    </ol>
                </nav>
                <div class="d-flex align-items-center gap-2">
                    <h4 class="fw-bold mb-0 text-dark">${framework.name}</h4>
                    <span class="badge ${framework.statusBadgeClass} px-2 py-1">
                        ${framework.statusDisplayName}
                    </span>
                </div>
            </div>
            <div class="d-flex flex-wrap gap-2">
                <a href="${pageContext.request.contextPath}/competency-frameworks" class="btn btn-outline-secondary">
                    <i class="bi bi-arrow-left me-1"></i> Quay lại
                </a>

                <c:if test="${currentUser.hasRole('ADMIN') || currentUser.hasRole('HR_MANAGER') || currentUser.hasPermission('competencies.assign')}">
                    <button type="button" class="btn btn-outline-primary" 
                            onclick="openAssignModal('${framework.id}', '${framework.code}', '${framework.name}', ${framework.isActive()})">
                        <i class="bi bi-person-badge me-1"></i> Gán chức danh
                    </button>
                </c:if>

                <c:if test="${currentUser.hasRole('ADMIN') || currentUser.hasRole('HR_MANAGER') || currentUser.hasPermission('competencies.status')}">
                    <c:choose>
                        <c:when test="${framework.isActive()}">
                            <button type="button" class="btn btn-outline-warning" 
                                    onclick="changeStatus('${framework.id}', 'INACTIVE', '${framework.code}')">
                                <i class="bi bi-pause-circle me-1"></i> Ngừng áp dụng
                            </button>
                        </c:when>
                        <c:otherwise>
                            <button type="button" class="btn btn-outline-success" 
                                    onclick="changeStatus('${framework.id}', 'ACTIVE', '${framework.code}')">
                                <i class="bi bi-check-circle me-1"></i> Kích hoạt áp dụng
                            </button>
                        </c:otherwise>
                    </c:choose>
                </c:if>

                <c:if test="${currentUser.hasRole('ADMIN') || currentUser.hasRole('HR_MANAGER') || currentUser.hasPermission('competencies.update')}">
                    <a href="${pageContext.request.contextPath}/competency-frameworks/edit?id=${framework.id}" class="btn btn-primary">
                        <i class="bi bi-pencil me-1"></i> Chỉnh sửa khung
                    </a>
                </c:if>
            </div>
        </div>

        <div class="row g-4">
            <!-- Cột trái: Thông tin chung & Chức danh đang áp dụng -->
            <div class="col-12 col-lg-5">
                <!-- Card Thông tin chung -->
                <div class="card border shadow-sm mb-4">
                    <div class="card-header bg-white py-3 border-bottom">
                        <h6 class="fw-bold mb-0 text-dark">
                            <i class="bi bi-info-circle-fill text-primary me-2"></i>Thông tin chung
                        </h6>
                    </div>
                    <div class="card-body p-3">
                        <table class="table table-sm table-borderless mb-0">
                            <tbody>
                                <tr>
                                    <td class="text-muted" style="width: 140px;">Mã khung:</td>
                                    <td><span class="badge bg-light text-primary border font-monospace fs-6">${framework.code}</span></td>
                                </tr>
                                <tr>
                                    <td class="text-muted">Tên khung:</td>
                                    <td class="fw-bold text-dark">${framework.name}</td>
                                </tr>
                                <tr>
                                    <td class="text-muted">Mô tả:</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty framework.description}">
                                                <span class="text-dark">${framework.description}</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="text-muted fst-italic">Chưa có mô tả</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="text-muted">Trạng thái:</td>
                                    <td>
                                        <span class="badge ${framework.statusBadgeClass}">
                                            ${framework.statusDisplayName}
                                        </span>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="text-muted">Số tiêu chí:</td>
                                    <td><strong class="text-dark">${framework.criteriaCount}</strong> tiêu chí</td>
                                </tr>
                                <tr>
                                    <td class="text-muted">Tổng trọng số:</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${framework.isWeightValid()}">
                                                <span class="badge bg-success-subtle text-success border border-success-subtle">
                                                    <i class="bi bi-check-circle-fill me-1"></i>${framework.formattedTotalWeight} (Đạt chuẩn 100%)
                                                </span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge bg-danger-subtle text-danger border border-danger-subtle">
                                                    <i class="bi bi-exclamation-triangle-fill me-1"></i>${framework.formattedTotalWeight} (Chưa đạt 100%)
                                                </span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                                <tr>
                                    <td class="text-muted">Người tạo:</td>
                                    <td>${framework.createdByName != null ? framework.createdByName : 'Hệ thống'}</td>
                                </tr>
                                <tr>
                                    <td class="text-muted">Ngày tạo:</td>
                                    <td><fmt:formatDate value="${framework.createdAt}" pattern="dd/MM/yyyy HH:mm" /></td>
                                </tr>
                                <tr>
                                    <td class="text-muted">Cập nhật lần cuối:</td>
                                    <td><fmt:formatDate value="${framework.updatedAt}" pattern="dd/MM/yyyy HH:mm" /></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>

                <!-- Card Các chức danh đang sử dụng -->
                <div class="card border shadow-sm">
                    <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                        <h6 class="fw-bold mb-0 text-dark">
                            <i class="bi bi-person-badge-fill text-primary me-2"></i>Chức danh đang áp dụng
                        </h6>
                        <span class="badge bg-primary-subtle text-primary border border-primary-subtle">
                            ${framework.assignedPositionsCount} chức danh
                        </span>
                    </div>
                    <div class="card-body p-0">
                        <c:choose>
                            <c:when test="${empty framework.assignedPositions}">
                                <div class="p-4 text-center text-muted">
                                    <i class="bi bi-slash-circle fs-3 text-secondary opacity-50 d-block mb-2"></i>
                                    <p class="mb-2 small">Khung năng lực này hiện chưa được gán cho chức danh nào.</p>
                                    <c:if test="${currentUser.hasRole('ADMIN') || currentUser.hasRole('HR_MANAGER') || currentUser.hasPermission('competencies.assign')}">
                                        <button type="button" class="btn btn-sm btn-outline-primary"
                                                onclick="openAssignModal('${framework.id}', '${framework.code}', '${framework.name}', ${framework.isActive()})">
                                            <i class="bi bi-plus-circle me-1"></i> Gán chức danh ngay
                                        </button>
                                    </c:if>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="table-responsive">
                                    <table class="table table-hover align-middle mb-0 small">
                                        <thead class="table-light">
                                            <tr>
                                                <th style="width: 35px;" class="text-center">#</th>
                                                <th>Mã CD</th>
                                                <th>Tên chức danh</th>
                                                <th>Cấp bậc</th>
                                                <th>Phòng ban</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <c:forEach var="pos" items="${framework.assignedPositions}" varStatus="pLoop">
                                                <tr>
                                                    <td class="text-center text-muted">${pLoop.count}</td>
                                                    <td><span class="font-monospace fw-bold">${pos.positionCode}</span></td>
                                                    <td class="fw-medium text-dark">${pos.positionTitle}</td>
                                                    <td><span class="badge bg-light text-dark border">${pos.level}</span></td>
                                                    <td class="text-muted">${pos.departmentName != null ? pos.departmentName : '-'}</td>
                                                </tr>
                                            </c:forEach>
                                        </tbody>
                                    </table>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>

            <!-- Cột phải: Danh sách tiêu chí đánh giá & Trọng số -->
            <div class="col-12 col-lg-7">
                <div class="card border shadow-sm">
                    <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                        <div>
                            <h6 class="fw-bold mb-0 text-dark">
                                <i class="bi bi-card-checklist text-primary me-2"></i>Danh sách tiêu chí đánh giá
                            </h6>
                            <small class="text-muted">Cơ sở sinh phiếu đánh giá phỏng vấn tuyển dụng ở Sprint 6</small>
                        </div>
                        <c:if test="${currentUser.hasRole('ADMIN') || currentUser.hasRole('HR_MANAGER') || currentUser.hasPermission('competencies.update')}">
                            <a href="${pageContext.request.contextPath}/competency-frameworks/edit?id=${framework.id}" class="btn btn-sm btn-outline-primary">
                                <i class="bi bi-pencil me-1"></i> Chỉnh sửa tiêu chí
                            </a>
                        </c:if>
                    </div>
                    <div class="card-body p-0">
                        <div class="table-responsive">
                            <table class="table table-hover align-middle mb-0">
                                <thead class="table-light">
                                    <tr>
                                        <th style="width: 45px;" class="text-center">STT</th>
                                        <th style="width: 110px;">Mã tiêu chí</th>
                                        <th style="min-width: 200px;">Tên tiêu chí &amp; Hướng dẫn đánh giá</th>
                                        <th style="width: 100px;" class="text-center">Thứ tự</th>
                                        <th style="width: 110px;" class="text-end">Trọng số</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:choose>
                                        <c:when test="${empty framework.criteria}">
                                            <tr>
                                                <td colspan="5" class="text-center py-4 text-muted">
                                                    Khung năng lực này chưa có tiêu chí nào.
                                                </td>
                                            </tr>
                                        </c:when>
                                        <c:otherwise>
                                            <c:forEach var="c" items="${framework.criteria}" varStatus="cLoop">
                                                <tr>
                                                    <td class="text-center text-muted small">${cLoop.count}</td>
                                                    <td>
                                                        <span class="badge bg-light text-dark border font-monospace">
                                                            ${c.criterionCode != null ? c.criterionCode : 'CRIT'}
                                                        </span>
                                                    </td>
                                                    <td>
                                                        <div class="fw-bold text-dark">${c.criterionName}</div>
                                                        <c:if test="${not empty c.criterionDescription}">
                                                            <div class="text-muted small mt-1">${c.criterionDescription}</div>
                                                        </c:if>
                                                        <c:if test="${not empty c.evaluationGuideline}">
                                                            <div class="text-primary small mt-1">
                                                                <i class="bi bi-lightbulb me-1"></i><em>Hướng dẫn: ${c.evaluationGuideline}</em>
                                                            </div>
                                                        </c:if>
                                                    </td>
                                                    <td class="text-center text-muted small">${c.displayOrder}</td>
                                                    <td class="text-end fw-bold text-dark fs-6">
                                                        <span class="badge bg-primary-subtle text-primary border border-primary-subtle fs-6">
                                                            ${c.formattedWeight}
                                                        </span>
                                                    </td>
                                                </tr>
                                            </c:forEach>
                                        </c:otherwise>
                                    </c:choose>
                                </tbody>
                                <tfoot class="table-light">
                                    <tr>
                                        <th colspan="4" class="text-end fw-bold text-dark py-3">TỔNG TRỌNG SỐ:</th>
                                        <th class="text-end py-3">
                                            <c:choose>
                                                <c:when test="${framework.isWeightValid()}">
                                                    <span class="badge bg-success fs-6 py-2 px-3">
                                                        <i class="bi bi-check-circle-fill me-1"></i>100%
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-danger fs-6 py-2 px-3">
                                                        <i class="bi bi-exclamation-triangle-fill me-1"></i>${framework.formattedTotalWeight}
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                        </th>
                                    </tr>
                                </tfoot>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</main>

<!-- Modal Gán Chức Danh Sử Dụng Khung Năng Lực -->
<div class="modal fade" id="assignModal" tabindex="-1" aria-labelledby="assignModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content border-0 shadow">
            <form action="${pageContext.request.contextPath}/competency-frameworks/assign-positions" method="post" id="assignForm">
                <input type="hidden" name="frameworkId" id="modalFrameworkId">
                <div class="modal-header bg-primary text-white">
                    <h5 class="modal-title" id="assignModalLabel">
                        <i class="bi bi-person-badge-fill me-2"></i>Gán Chức Danh Cho Khung Năng Lực
                    </h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="alert alert-info py-2 px-3 small mb-3">
                        <i class="bi bi-info-circle-fill me-1"></i>
                        Đang thiết lập cho khung: <strong id="modalFrameworkName"></strong> (<span id="modalFrameworkCode" class="font-monospace"></span>).
                        <br>
                        <em>Lưu ý: Mỗi chức danh chỉ gắn một khung năng lực duy nhất. Chọn để gán hoặc bỏ chọn để hủy liên kết.</em>
                    </div>

                    <div id="assignAlertContainer"></div>

                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <span class="fw-bold small text-dark">Danh sách chức danh trên hệ thống:</span>
                        <div class="btn-group btn-group-sm">
                            <button type="button" class="btn btn-outline-secondary" onclick="checkAllPositions(true)">Chọn tất cả</button>
                            <button type="button" class="btn btn-outline-secondary" onclick="checkAllPositions(false)">Bỏ chọn tất cả</button>
                        </div>
                    </div>

                    <div class="table-responsive border rounded-3" style="max-height: 340px; overflow-y: auto;">
                        <table class="table table-hover align-middle mb-0 small">
                            <thead class="table-light sticky-top">
                                <tr>
                                    <th style="width: 45px;" class="text-center">Gán</th>
                                    <th>Mã CD</th>
                                    <th>Tên chức danh</th>
                                    <th>Cấp bậc</th>
                                    <th>Phòng ban</th>
                                    <th>Khung hiện tại</th>
                                </tr>
                            </thead>
                            <tbody id="positionsTableBody">
                                <tr>
                                    <td colspan="6" class="text-center py-4 text-muted">
                                        <div class="spinner-border spinner-border-sm text-primary me-2"></div>
                                        Đang nạp danh sách chức danh...
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>
                    <button type="submit" class="btn btn-primary" id="btnSaveAssign">
                        <i class="bi bi-save me-1"></i> Lưu thiết lập chức danh
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- Modal xác nhận thay đổi trạng thái -->
<div class="modal fade" id="statusConfirmModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <form action="${pageContext.request.contextPath}/competency-frameworks/status" method="post" id="statusForm">
                <input type="hidden" name="id" id="statusModalId">
                <input type="hidden" name="status" id="statusModalValue">
                <div class="modal-header">
                    <h5 class="modal-title fw-bold" id="statusModalTitle">Xác nhận thay đổi trạng thái</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body" id="statusModalBody">
                    Bạn có chắc chắn muốn thay đổi trạng thái khung năng lực này?
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-primary" id="statusModalBtn">Xác nhận</button>
                </div>
            </form>
        </div>
    </div>
</div>

<jsp:include page="../common/footer.jsp" />

<script>
function changeStatus(id, newStatus, code) {
    document.getElementById('statusModalId').value = id;
    document.getElementById('statusModalValue').value = newStatus;
    var title = document.getElementById('statusModalTitle');
    var body = document.getElementById('statusModalBody');
    var btn = document.getElementById('statusModalBtn');

    if (newStatus === 'ACTIVE') {
        title.innerText = 'Kích hoạt Khung năng lực';
        body.innerHTML = 'Bạn có chắc chắn muốn kích hoạt áp dụng khung năng lực <strong>' + code + '</strong>?<br><span class="text-muted small">Hệ thống sẽ kiểm tra tổng trọng số 100% trước khi kích hoạt.</span>';
        btn.className = 'btn btn-success';
        btn.innerText = 'Kích hoạt ngay';
    } else {
        title.innerText = 'Ngừng áp dụng Khung năng lực';
        body.innerHTML = 'Bạn có chắc chắn muốn ngừng áp dụng khung năng lực <strong>' + code + '</strong>?<br><span class="text-danger small">Các quy trình đánh giá mới sẽ không thể chọn khung năng lực này.</span>';
        btn.className = 'btn btn-warning';
        btn.innerText = 'Ngừng áp dụng';
    }

    var modal = new bootstrap.Modal(document.getElementById('statusConfirmModal'));
    modal.show();
}

function openAssignModal(frameworkId, code, name, isActive) {
    document.getElementById('modalFrameworkId').value = frameworkId;
    document.getElementById('modalFrameworkCode').innerText = code;
    document.getElementById('modalFrameworkName').innerText = name;

    var alertContainer = document.getElementById('assignAlertContainer');
    var btnSave = document.getElementById('btnSaveAssign');

    if (!isActive) {
        alertContainer.innerHTML = '<div class="alert alert-danger py-2 small mb-3"><i class="bi bi-exclamation-triangle-fill me-1"></i> <strong>Cảnh báo:</strong> Khung năng lực này chưa được Kích hoạt (trạng thái Nháp hoặc Ngừng áp dụng). Bạn cần kích hoạt khung trước khi gán cho chức danh!</div>';
        btnSave.disabled = true;
    } else {
        alertContainer.innerHTML = '';
        btnSave.disabled = false;
    }

    var tbody = document.getElementById('positionsTableBody');
    tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm text-primary me-2"></div>Đang tải danh sách chức danh...</td></tr>';

    var modal = new bootstrap.Modal(document.getElementById('assignModal'));
    modal.show();

    fetch('${pageContext.request.contextPath}/competency-frameworks/api/positions')
        .then(function(res) { return res.json(); })
        .then(function(data) {
            tbody.innerHTML = '';
            if (!data || data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center py-3 text-muted">Chưa có chức danh nào trong danh mục dải lương.</td></tr>';
                return;
            }

            data.forEach(function(pos) {
                var isChecked = (pos.frameworkId === frameworkId);
                var tr = document.createElement('tr');
                tr.innerHTML = 
                    '<td class="text-center">' +
                    '  <input type="checkbox" name="positionIds" value="' + pos.positionId + '" class="form-check-input pos-checkbox" ' + (isChecked ? 'checked' : '') + '>' +
                    '</td>' +
                    '<td class="fw-bold font-monospace">' + (pos.positionCode || '-') + '</td>' +
                    '<td>' + (pos.positionTitle || '-') + '</td>' +
                    '<td><span class="badge bg-light text-dark border">' + (pos.level || '-') + '</span></td>' +
                    '<td class="text-muted">' + (pos.departmentName || '-') + '</td>' +
                    '<td>' + 
                        (pos.frameworkName ? 
                            ('<span class="badge ' + (isChecked ? 'bg-success' : 'bg-secondary') + '">' + pos.frameworkName + '</span>') : 
                            '<span class="text-muted fst-italic">Chưa có</span>') +
                    '</td>';
                tbody.appendChild(tr);
            });
        })
        .catch(function(err) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center py-3 text-danger">Lỗi khi tải dữ liệu chức danh: ' + err.message + '</td></tr>';
        });
}

function checkAllPositions(state) {
    var cbs = document.querySelectorAll('.pos-checkbox');
    cbs.forEach(function(cb) { cb.checked = state; });
}
</script>

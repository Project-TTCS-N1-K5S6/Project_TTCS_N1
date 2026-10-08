<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<c:set var="pageTitle" value="Khai báo khung năng lực" />
<c:set var="breadcrumb" value="Khung năng lực" />
<c:set var="activeMenu" value="competencies" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Tiêu đề trang & Nút thêm mới -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <h4 class="fw-bold mb-1">
                    <i class="bi bi-award-fill text-primary me-2"></i>Khai báo Khung Năng Lực
                </h4>
                <p class="text-muted small mb-0">
                    Quản lý các bộ tiêu chí đánh giá nhân sự và ứng viên theo từng chức danh. Tổng trọng số mỗi khung bắt buộc bằng 100%.
                </p>
            </div>
            <c:if test="${currentUser.hasRole('ADMIN') || currentUser.hasRole('HR_MANAGER') || currentUser.hasPermission('competencies.create')}">
                <a href="${pageContext.request.contextPath}/competency-frameworks/create" class="btn btn-primary shadow-sm">
                    <i class="bi bi-plus-circle-fill me-1"></i> Thêm khung năng lực
                </a>
            </c:if>
        </div>

        <!-- Thống kê nhanh theo trạng thái -->
        <div class="row g-3 mb-4">
            <div class="col-6 col-md-3">
                <div class="card border shadow-sm h-100">
                    <div class="card-body py-3">
                        <div class="d-flex align-items-center justify-content-between">
                            <div>
                                <span class="text-muted small fw-medium">Tổng số khung</span>
                                <h3 class="fw-bold mb-0 text-dark">${totalCount}</h3>
                            </div>
                            <div class="p-2 bg-primary-subtle rounded-3 text-primary">
                                <i class="bi bi-layers-fill fs-4"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="card border shadow-sm h-100">
                    <div class="card-body py-3">
                        <div class="d-flex align-items-center justify-content-between">
                            <div>
                                <span class="text-muted small fw-medium">Đang áp dụng</span>
                                <h3 class="fw-bold mb-0 text-success">${activeCount}</h3>
                            </div>
                            <div class="p-2 bg-success-subtle rounded-3 text-success">
                                <i class="bi bi-check-circle-fill fs-4"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="card border shadow-sm h-100">
                    <div class="card-body py-3">
                        <div class="d-flex align-items-center justify-content-between">
                            <div>
                                <span class="text-muted small fw-medium">Bản nháp</span>
                                <h3 class="fw-bold mb-0 text-secondary">${draftCount}</h3>
                            </div>
                            <div class="p-2 bg-secondary-subtle rounded-3 text-secondary">
                                <i class="bi bi-pencil-square fs-4"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-6 col-md-3">
                <div class="card border shadow-sm h-100">
                    <div class="card-body py-3">
                        <div class="d-flex align-items-center justify-content-between">
                            <div>
                                <span class="text-muted small fw-medium">Ngừng áp dụng</span>
                                <h3 class="fw-bold mb-0 text-warning">${inactiveCount}</h3>
                            </div>
                            <div class="p-2 bg-warning-subtle rounded-3 text-warning">
                                <i class="bi bi-pause-circle-fill fs-4"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Bộ lọc tìm kiếm -->
        <div class="card mb-4 border shadow-sm">
            <div class="card-body py-3">
                <form method="get" action="${pageContext.request.contextPath}/competency-frameworks" class="row g-2 align-items-center">
                    <div class="col-12 col-md-6">
                        <div class="input-group input-group-sm">
                            <span class="input-group-text bg-white"><i class="bi bi-search text-muted"></i></span>
                            <input type="text" name="search" class="form-control" 
                                   placeholder="Tìm kiếm theo mã, tên khung năng lực hoặc mô tả..." 
                                   value="${paramSearch}">
                        </div>
                    </div>
                    <div class="col-12 col-md-4">
                        <select name="status" class="form-select form-select-sm">
                            <option value="">-- Tất cả trạng thái --</option>
                            <option value="ACTIVE" ${paramStatus == 'ACTIVE' ? 'selected' : ''}>Đang áp dụng (Active)</option>
                            <option value="DRAFT" ${paramStatus == 'DRAFT' ? 'selected' : ''}>Bản nháp (Draft)</option>
                            <option value="INACTIVE" ${paramStatus == 'INACTIVE' ? 'selected' : ''}>Ngừng áp dụng (Inactive)</option>
                        </select>
                    </div>
                    <div class="col-12 col-md-2 d-flex gap-2">
                        <button type="submit" class="btn btn-sm btn-primary w-100">
                            <i class="bi bi-funnel-fill me-1"></i> Lọc
                        </button>
                        <c:if test="${not empty paramSearch || not empty paramStatus}">
                            <a href="${pageContext.request.contextPath}/competency-frameworks" class="btn btn-sm btn-outline-secondary" title="Xóa bộ lọc">
                                <i class="bi bi-x-circle"></i>
                            </a>
                        </c:if>
                    </div>
                </form>
            </div>
        </div>

        <!-- Bảng danh sách khung năng lực -->
        <div class="card border shadow-sm">
            <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                <h6 class="fw-bold mb-0 text-dark">
                    <i class="bi bi-list-task me-2 text-primary"></i>Danh sách Khung Năng Lực
                </h6>
                <span class="text-muted small">Hiển thị <strong>${frameworks.size()}</strong> khung</span>
            </div>
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th style="width: 50px;" class="text-center">STT</th>
                                <th style="width: 140px;">Mã khung</th>
                                <th style="min-width: 220px;">Tên &amp; Mô tả khung năng lực</th>
                                <th style="width: 120px;" class="text-center">Số tiêu chí</th>
                                <th style="width: 120px;" class="text-center">Tổng trọng số</th>
                                <th style="min-width: 180px;">Chức danh đang áp dụng</th>
                                <th style="width: 140px;" class="text-center">Trạng thái</th>
                                <th style="width: 110px;" class="text-center">Ngày tạo</th>
                                <th style="width: 140px;" class="text-center">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${empty frameworks}">
                                    <tr>
                                        <td colspan="9" class="text-center py-5 text-muted">
                                            <i class="bi bi-inbox fs-1 d-block mb-2 text-secondary opacity-50"></i>
                                            <p class="mb-1">Không tìm thấy khung năng lực nào phù hợp.</p>
                                            <c:if test="${currentUser.hasRole('ADMIN') || currentUser.hasRole('HR_MANAGER') || currentUser.hasPermission('competencies.create')}">
                                                <a href="${pageContext.request.contextPath}/competency-frameworks/create" class="btn btn-sm btn-outline-primary mt-2">
                                                    <i class="bi bi-plus-circle me-1"></i> Tạo khung năng lực mới
                                                </a>
                                            </c:if>
                                        </td>
                                    </tr>
                                </c:when>
                                <c:otherwise>
                                    <c:forEach var="f" items="${frameworks}" varStatus="loop">
                                        <tr>
                                            <td class="text-center text-muted small">${loop.count}</td>
                                            <td>
                                                <a href="${pageContext.request.contextPath}/competency-frameworks/detail?id=${f.id}" 
                                                   class="fw-bold text-decoration-none font-monospace text-primary">
                                                    ${f.code}
                                                </a>
                                            </td>
                                            <td>
                                                <div class="fw-bold text-dark">${f.name}</div>
                                                <c:if test="${not empty f.description}">
                                                    <small class="text-muted text-truncate d-block" style="max-width: 280px;" title="${f.description}">
                                                        ${f.description}
                                                    </small>
                                                </c:if>
                                            </td>
                                            <td class="text-center">
                                                <span class="badge bg-light text-dark border">
                                                    ${f.criteriaCount} tiêu chí
                                                </span>
                                            </td>
                                            <td class="text-center">
                                                <c:choose>
                                                    <c:when test="${f.isWeightValid()}">
                                                        <span class="badge bg-success-subtle text-success border border-success-subtle">
                                                            <i class="bi bi-check-circle-fill me-1"></i>100%
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-danger-subtle text-danger border border-danger-subtle" 
                                                              title="Tổng trọng số phải bằng 100%">
                                                            <i class="bi bi-exclamation-triangle-fill me-1"></i>${f.formattedTotalWeight}
                                                        </span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${f.assignedPositionsCount > 0}">
                                                        <span class="badge bg-primary-subtle text-primary border border-primary-subtle mb-1">
                                                            <i class="bi bi-person-badge-fill me-1"></i>${f.assignedPositionsCount} chức danh
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-light text-muted border">Chưa gắn</span>
                                                    </c:otherwise>
                                                </c:choose>
                                                <c:if test="${currentUser.hasRole('ADMIN') || currentUser.hasRole('HR_MANAGER') || currentUser.hasPermission('competencies.assign')}">
                                                    <button type="button" class="btn btn-link btn-sm p-0 d-block text-xs text-primary" 
                                                            onclick="openAssignModal('${f.id}', '${f.code}', '${f.name}', ${f.isActive()})">
                                                        <i class="bi bi-gear-fill me-1"></i>Gán chức danh
                                                    </button>
                                                </c:if>
                                            </td>
                                            <td class="text-center">
                                                <c:choose>
                                                    <c:when test="${f.isActive()}">
                                                        <span class="badge bg-success-subtle text-success border border-success-subtle">
                                                            <i class="bi bi-circle-fill me-1" style="font-size: 6px;"></i>Đang áp dụng
                                                        </span>
                                                    </c:when>
                                                    <c:when test="${f.isDraft()}">
                                                        <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle">
                                                            <i class="bi bi-pencil-fill me-1" style="font-size: 8px;"></i>Bản nháp
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-warning-subtle text-warning border border-warning-subtle">
                                                            <i class="bi bi-pause-fill me-1"></i>Ngừng áp dụng
                                                        </span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td class="text-center text-muted small">
                                                <fmt:formatDate value="${f.createdAt}" pattern="dd/MM/yyyy" />
                                            </td>
                                            <td class="text-center">
                                                <div class="btn-group btn-group-sm">
                                                    <a href="${pageContext.request.contextPath}/competency-frameworks/detail?id=${f.id}" 
                                                       class="btn btn-outline-secondary" title="Xem chi tiết">
                                                        <i class="bi bi-eye"></i>
                                                    </a>
                                                    <c:if test="${currentUser.hasRole('ADMIN') || currentUser.hasRole('HR_MANAGER') || currentUser.hasPermission('competencies.update')}">
                                                        <a href="${pageContext.request.contextPath}/competency-frameworks/edit?id=${f.id}" 
                                                           class="btn btn-outline-primary" title="Chỉnh sửa">
                                                            <i class="bi bi-pencil"></i>
                                                        </a>
                                                    </c:if>
                                                    <c:if test="${currentUser.hasRole('ADMIN') || currentUser.hasRole('HR_MANAGER') || currentUser.hasPermission('competencies.status')}">
                                                        <c:choose>
                                                            <c:when test="${f.isActive()}">
                                                                <button type="button" class="btn btn-outline-warning" title="Ngừng áp dụng"
                                                                        onclick="changeStatus('${f.id}', 'INACTIVE', '${f.code}')">
                                                                    <i class="bi bi-pause-circle"></i>
                                                                </button>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <button type="button" class="btn btn-outline-success" title="Kích hoạt áp dụng"
                                                                        onclick="changeStatus('${f.id}', 'ACTIVE', '${f.code}')">
                                                                    <i class="bi bi-check-circle"></i>
                                                                </button>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </c:if>
                                                </div>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
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
                                        <div class="spinner-border spinner-border-sm text-primary me-2" role="status"></div>
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
                var isOther = (pos.frameworkId && pos.frameworkId !== frameworkId);

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

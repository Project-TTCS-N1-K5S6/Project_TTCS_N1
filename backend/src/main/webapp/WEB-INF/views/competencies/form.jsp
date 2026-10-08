<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="isEdit" value="${mode == 'edit'}" />
<c:set var="pageTitle" value="${isEdit ? 'Chỉnh sửa Khung năng lực' : 'Tạo mới Khung năng lực'}" />
<c:set var="breadcrumb" value="${isEdit ? 'Chỉnh sửa' : 'Tạo mới'}" />
<c:set var="activeMenu" value="competencies" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<style>
    .weight-progress {
        height: 12px;
        border-radius: 6px;
        background-color: #e2e8f0;
        overflow: hidden;
    }
    .weight-progress-bar {
        transition: width 0.3s ease, background-color 0.3s ease;
    }
    .criterion-row:hover {
        background-color: #f8fafc;
    }
</style>

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Header -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <nav aria-label="breadcrumb">
                    <ol class="breadcrumb mb-1 small">
                        <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/dashboard">Tổng quan</a></li>
                        <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/competency-frameworks">Khung năng lực</a></li>
                        <li class="breadcrumb-item active" aria-current="page">${isEdit ? 'Chỉnh sửa' : 'Tạo mới'}</li>
                    </ol>
                </nav>
                <h4 class="fw-bold mb-0 text-dark">
                    <i class="bi ${isEdit ? 'bi-pencil-square' : 'bi-plus-circle-fill'} text-primary me-2"></i>
                    ${isEdit ? 'Chỉnh sửa Khung Năng Lực' : 'Khai Báo Khung Năng Lực Mới'}
                </h4>
            </div>
            <div>
                <a href="${pageContext.request.contextPath}/competency-frameworks" class="btn btn-outline-secondary">
                    <i class="bi bi-x-circle me-1"></i> Hủy &amp; Quay lại
                </a>
            </div>
        </div>

        <form action="${pageContext.request.contextPath}/competency-frameworks/${isEdit ? 'edit' : 'create'}" 
              method="post" id="frameworkForm" onsubmit="return validateAndSubmit(event)">
            <c:if test="${isEdit}">
                <input type="hidden" name="id" value="${framework.id}">
            </c:if>

            <!-- Khối 1: Thông tin cơ bản -->
            <div class="card border shadow-sm mb-4">
                <div class="card-header bg-white py-3 border-bottom">
                    <h6 class="fw-bold mb-0 text-dark">
                        <i class="bi bi-info-circle-fill text-primary me-2"></i>1. Thông tin chung Khung Năng Lực
                    </h6>
                </div>
                <div class="card-body p-4">
                    <div class="row g-3">
                        <div class="col-12 col-md-4">
                            <label class="form-label small fw-bold">Mã khung năng lực <span class="text-danger">*</span></label>
                            <input type="text" name="code" id="frameworkCode" class="form-control text-uppercase font-monospace" 
                                   placeholder="VD: KNL-DEV-SR, KNL-SALES-01" 
                                   value="${framework.code}" required style="letter-spacing: 0.5px;">
                            <div class="form-text">Mã định danh duy nhất (tự động viết hoa).</div>
                        </div>
                        <div class="col-12 col-md-5">
                            <label class="form-label small fw-bold">Tên khung năng lực <span class="text-danger">*</span></label>
                            <input type="text" name="name" id="frameworkName" class="form-control" 
                                   placeholder="VD: Khung năng lực Kỹ sư Backend Senior" 
                                   value="${framework.name}" required>
                            <div class="form-text">Tên hiển thị rõ ràng cho các phòng ban.</div>
                        </div>
                        <div class="col-12 col-md-3">
                            <label class="form-label small fw-bold">Trạng thái áp dụng <span class="text-danger">*</span></label>
                            <select name="status" id="frameworkStatus" class="form-select">
                                <option value="DRAFT" ${framework.status == 'DRAFT' || empty framework.status ? 'selected' : ''}>
                                    Bản nháp (Draft)
                                </option>
                                <option value="ACTIVE" ${framework.status == 'ACTIVE' ? 'selected' : ''}>
                                    Đang áp dụng (Active)
                                </option>
                                <c:if test="${isEdit}">
                                    <option value="INACTIVE" ${framework.status == 'INACTIVE' ? 'selected' : ''}>
                                        Ngừng áp dụng (Inactive)
                                    </option>
                                </c:if>
                            </select>
                            <div class="form-text">Trạng thái hoạt động của khung.</div>
                        </div>
                        <div class="col-12">
                            <label class="form-label small fw-bold">Mô tả mục đích &amp; phạm vi áp dụng</label>
                            <textarea name="description" id="frameworkDesc" rows="2" class="form-control" 
                                      placeholder="Mô tả phạm vi áp dụng, chức danh mục tiêu hoặc tiêu chuẩn đánh giá...">${framework.description}</textarea>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Khối 2: Danh sách tiêu chí & Trọng số (Business Rule Bắt buộc: Tổng = 100%) -->
            <div class="card border shadow-sm mb-4">
                <div class="card-header bg-white py-3 border-bottom d-flex flex-wrap justify-content-between align-items-center gap-2">
                    <div>
                        <h6 class="fw-bold mb-0 text-dark">
                            <i class="bi bi-card-checklist text-primary me-2"></i>2. Danh sách Tiêu chí Đánh giá &amp; Trọng số
                        </h6>
                        <small class="text-muted">Tổng trọng số bắt buộc phải bằng đúng <strong>100%</strong>.</small>
                    </div>
                    <div class="d-flex gap-2">
                        <button type="button" class="btn btn-sm btn-outline-primary" onclick="openCriterionCatalogModal()">
                            <i class="bi bi-plus-circle me-1"></i> Chọn tiêu chí có sẵn
                        </button>
                        <button type="button" class="btn btn-sm btn-outline-success" onclick="openQuickCreateCriterionModal()">
                            <i class="bi bi-plus-lg me-1"></i> Tạo nhanh tiêu chí mới
                        </button>
                    </div>
                </div>

                <!-- Thanh chỉ báo Tổng Trọng Số (Real-time Progress Indicator) -->
                <div class="card-body bg-light border-bottom p-3">
                    <div class="d-flex flex-wrap justify-content-between align-items-center mb-2">
                        <div>
                            <span class="fw-bold small text-dark">TỔNG TRỌNG SỐ HIỆN TẠI:</span>
                            <span id="totalWeightBadge" class="badge fs-6 ms-2 bg-secondary">0%</span>
                        </div>
                        <div id="weightStatusMessage" class="small fw-semibold text-muted">
                            Chưa có tiêu chí nào được chọn.
                        </div>
                    </div>
                    <div class="weight-progress">
                        <div id="weightProgressBar" class="weight-progress-bar h-100 bg-secondary" style="width: 0%;"></div>
                    </div>
                </div>

                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-hover align-middle mb-0" id="criteriaTable">
                            <thead class="table-light">
                                <tr>
                                    <th style="width: 50px;" class="text-center">STT</th>
                                    <th style="min-width: 250px;">Tiêu chí năng lực</th>
                                    <th style="min-width: 200px;">Hướng dẫn đánh giá</th>
                                    <th style="width: 150px;" class="text-center">Trọng số (%) <span class="text-danger">*</span></th>
                                    <th style="width: 100px;" class="text-center">Thứ tự</th>
                                    <th style="width: 90px;" class="text-center">Xóa</th>
                                </tr>
                            </thead>
                            <tbody id="criteriaTableBody">
                                <!-- Hàng tiêu chí sẽ được render qua JavaScript -->
                            </tbody>
                        </table>
                    </div>
                    <div id="emptyCriteriaNotice" class="text-center py-5 text-muted">
                        <i class="bi bi-card-list fs-2 opacity-50 d-block mb-2"></i>
                        <p class="mb-2">Chưa có tiêu chí nào trong khung năng lực này.</p>
                        <button type="button" class="btn btn-sm btn-primary" onclick="openCriterionCatalogModal()">
                            <i class="bi bi-plus-circle me-1"></i> Thêm tiêu chí đánh giá ngay
                        </button>
                    </div>
                </div>
            </div>

            <!-- Nút hành động Lưu -->
            <div class="card border shadow-sm">
                <div class="card-body py-3 d-flex flex-wrap justify-content-between align-items-center gap-3">
                    <div class="text-muted small">
                        <i class="bi bi-shield-check text-success me-1"></i>
                        Hệ thống tự động kiểm tra tính toàn vẹn và ghi nhật ký kiểm toán (Audit Log).
                    </div>
                    <div class="d-flex gap-2">
                        <a href="${pageContext.request.contextPath}/competency-frameworks" class="btn btn-outline-secondary">
                            Hủy bỏ
                        </a>
                        <button type="submit" class="btn btn-primary px-4 fw-bold shadow-sm" id="btnSubmit">
                            <i class="bi bi-save-fill me-1"></i> ${isEdit ? 'Lưu thay đổi' : 'Tạo khung năng lực'}
                        </button>
                    </div>
                </div>
            </div>
        </form>
    </div>
</main>

<!-- Modal 1: Chọn tiêu chí từ Ngân hàng tiêu chí -->
<div class="modal fade" id="catalogModal" tabindex="-1" aria-labelledby="catalogModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content border-0 shadow">
            <div class="modal-header bg-primary text-white">
                <h5 class="modal-title" id="catalogModalLabel">
                    <i class="bi bi-collection-fill me-2"></i>Chọn Tiêu Chí Đánh Giá Từ Ngân Hàng
                </h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-3">
                <div class="input-group input-group-sm mb-3">
                    <span class="input-group-text bg-white"><i class="bi bi-search"></i></span>
                    <input type="text" id="catalogSearchInput" class="form-control" 
                           placeholder="Tìm kiếm tiêu chí theo tên, mã hoặc mô tả..." oninput="filterCatalogList()">
                </div>
                <div class="table-responsive border rounded-3" style="max-height: 360px; overflow-y: auto;">
                    <table class="table table-hover align-middle mb-0 small">
                        <thead class="table-light sticky-top">
                            <tr>
                                <th style="width: 45px;" class="text-center">Chọn</th>
                                <th style="width: 110px;">Mã</th>
                                <th>Tên tiêu chí</th>
                                <th>Mô tả &amp; Hướng dẫn</th>
                            </tr>
                        </thead>
                        <tbody id="catalogTableBody">
                            <c:forEach var="c" items="${criteriaCatalog}">
                                <tr class="catalog-item-row" data-search="${c.code} ${c.name} ${c.description} ${c.evaluationGuideline}">
                                    <td class="text-center">
                                        <input type="checkbox" class="form-check-input catalog-cb" 
                                               value="${c.id}"
                                               data-id="${c.id}"
                                               data-code="${c.code}"
                                               data-name="${c.name}"
                                               data-desc="${c.description}"
                                               data-guide="${c.evaluationGuideline}">
                                    </td>
                                    <td><span class="badge bg-light text-dark border font-monospace">${c.code != null ? c.code : 'CRIT'}</span></td>
                                    <td class="fw-bold text-dark">${c.name}</td>
                                    <td class="text-muted">
                                        <c:if test="${not empty c.description}"><div>${c.description}</div></c:if>
                                        <c:if test="${not empty c.evaluationGuideline}"><div class="text-primary mt-1"><i class="bi bi-lightbulb"></i> ${c.evaluationGuideline}</div></c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
            <div class="modal-footer bg-light">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>
                <button type="button" class="btn btn-primary" onclick="addSelectedFromCatalog()">
                    <i class="bi bi-plus-circle me-1"></i> Thêm vào khung năng lực
                </button>
            </div>
        </div>
    </div>
</div>

<!-- Modal 2: Tạo nhanh Tiêu chí mới vào ngân hàng -->
<div class="modal fade" id="quickCriterionModal" tabindex="-1" aria-labelledby="quickCriterionModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content border-0 shadow">
            <div class="modal-header bg-success text-white">
                <h5 class="modal-title" id="quickCriterionModalLabel">
                    <i class="bi bi-plus-circle-fill me-2"></i>Tạo Nhanh Tiêu Chí Mới
                </h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4">
                <div id="quickCritAlert"></div>
                <div class="mb-3">
                    <label class="form-label small fw-bold">Mã tiêu chí</label>
                    <input type="text" id="quickCritCode" class="form-control form-control-sm text-uppercase font-monospace" placeholder="VD: CRIT-SQL, CRIT-REACT (bỏ trống tự sinh)">
                </div>
                <div class="mb-3">
                    <label class="form-label small fw-bold">Tên tiêu chí <span class="text-danger">*</span></label>
                    <input type="text" id="quickCritName" class="form-control form-control-sm" placeholder="VD: Kỹ năng phân tích thiết kế CSDL" required>
                </div>
                <div class="mb-3">
                    <label class="form-label small fw-bold">Mô tả tiêu chí</label>
                    <textarea id="quickCritDesc" rows="2" class="form-control form-control-sm" placeholder="Mô tả tiêu chuẩn năng lực cần có..."></textarea>
                </div>
                <div class="mb-3">
                    <label class="form-label small fw-bold">Hướng dẫn đánh giá (Evaluation Guideline)</label>
                    <textarea id="quickCritGuide" rows="2" class="form-control form-control-sm" placeholder="Cách đặt câu hỏi hoặc tiêu chí chấm điểm tương ứng..."></textarea>
                </div>
            </div>
            <div class="modal-footer bg-light">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                <button type="button" class="btn btn-success" id="btnQuickSave" onclick="saveQuickCriterion()">
                    <i class="bi bi-save me-1"></i> Lưu &amp; Thêm vào khung
                </button>
            </div>
        </div>
    </div>
</div>

<jsp:include page="../common/footer.jsp" />

<script>
// Dữ liệu tiêu chí hiện tại trong khung
var criteriaList = [];

// Nếu là chế độ Edit, nạp danh sách tiêu chí có sẵn của framework
<c:if test="${isEdit && not empty framework.criteria}">
    <c:forEach var="c" items="${framework.criteria}">
        criteriaList.push({
            id: "${c.id}",
            criterionId: "${c.criterionId}",
            code: "${c.criterionCode}",
            name: "${c.criterionName}",
            desc: "${c.criterionDescription}",
            guide: "${c.evaluationGuideline}",
            weight: ${c.weight != null ? c.weight : 0},
            displayOrder: ${c.displayOrder}
        });
    </c:forEach>
</c:if>

document.addEventListener("DOMContentLoaded", function() {
    renderCriteriaTable();
});

function renderCriteriaTable() {
    var tbody = document.getElementById("criteriaTableBody");
    var emptyNotice = document.getElementById("emptyCriteriaNotice");
    tbody.innerHTML = "";

    if (criteriaList.length === 0) {
        emptyNotice.style.display = "block";
    } else {
        emptyNotice.style.display = "none";
    }

    criteriaList.forEach(function(item, index) {
        var tr = document.createElement("tr");
        tr.className = "criterion-row";
        tr.innerHTML = 
            '<td class="text-center text-muted small fw-bold">' + (index + 1) + '</td>' +
            '<td>' +
            '  <input type="hidden" name="criterionId[]" value="' + item.criterionId + '">' +
            '  <input type="hidden" name="displayOrder[]" value="' + (index + 1) + '">' +
            '  <div class="d-flex align-items-center gap-2">' +
            '    <span class="badge bg-light text-dark border font-monospace">' + (item.code || 'CRIT') + '</span>' +
            '    <span class="fw-bold text-dark">' + escapeHtml(item.name) + '</span>' +
            '  </div>' +
            '  <div class="text-muted small mt-1 text-truncate" style="max-width: 320px;">' + escapeHtml(item.desc || '') + '</div>' +
            '</td>' +
            '<td>' +
            '  <div class="small text-muted">' + (item.guide ? ('<i class="bi bi-lightbulb text-primary me-1"></i>' + escapeHtml(item.guide)) : '<span class="text-muted fst-italic">Không có hướng dẫn</span>') + '</div>' +
            '</td>' +
            '<td class="text-center">' +
            '  <div class="input-group input-group-sm mx-auto" style="max-width: 120px;">' +
            '    <input type="number" name="weight[]" class="form-control text-center fw-bold weight-input" ' +
            '           value="' + item.weight + '" min="0.5" max="100" step="0.5" required ' +
            '           oninput="onWeightChange(' + index + ', this.value)">' +
            '    <span class="input-group-text">%</span>' +
            '  </div>' +
            '</td>' +
            '<td class="text-center">' +
            '  <div class="btn-group btn-group-sm">' +
            '    <button type="button" class="btn btn-outline-secondary btn-xs" title="Di chuyển lên" ' + (index === 0 ? 'disabled' : '') + ' onclick="moveItem(' + index + ', -1)">' +
            '      <i class="bi bi-chevron-up"></i>' +
            '    </button>' +
            '    <button type="button" class="btn btn-outline-secondary btn-xs" title="Di chuyển xuống" ' + (index === criteriaList.length - 1 ? 'disabled' : '') + ' onclick="moveItem(' + index + ', 1)">' +
            '      <i class="bi bi-chevron-down"></i>' +
            '    </button>' +
            '  </div>' +
            '</td>' +
            '<td class="text-center">' +
            '  <button type="button" class="btn btn-outline-danger btn-sm" title="Xóa khỏi khung" onclick="removeCriterion(' + index + ')">' +
            '    <i class="bi bi-trash"></i>' +
            '  </button>' +
            '</td>';
        tbody.appendChild(tr);
    });

    calculateTotalWeight();
}

function onWeightChange(index, val) {
    var num = parseFloat(val);
    criteriaList[index].weight = isNaN(num) ? 0 : num;
    calculateTotalWeight();
}

function moveItem(index, direction) {
    var targetIndex = index + direction;
    if (targetIndex < 0 || targetIndex >= criteriaList.length) return;
    var temp = criteriaList[index];
    criteriaList[index] = criteriaList[targetIndex];
    criteriaList[targetIndex] = temp;
    renderCriteriaTable();
}

function removeCriterion(index) {
    criteriaList.splice(index, 1);
    renderCriteriaTable();
}

function calculateTotalWeight() {
    var total = 0;
    var hasInvalidWeight = false;

    criteriaList.forEach(function(item) {
        var w = parseFloat(item.weight) || 0;
        if (w <= 0) hasInvalidWeight = true;
        total += w;
    });

    // Làm tròn 2 chữ số thập phân
    total = Math.round(total * 100) / 100;

    var badge = document.getElementById("totalWeightBadge");
    var progressBar = document.getElementById("weightProgressBar");
    var msg = document.getElementById("weightStatusMessage");
    var btnSubmit = document.getElementById("btnSubmit");

    badge.innerText = total + "%";
    var progressWidth = Math.min(total, 100);
    progressBar.style.width = progressWidth + "%";

    if (criteriaList.length === 0) {
        badge.className = "badge fs-6 ms-2 bg-secondary";
        progressBar.className = "weight-progress-bar h-100 bg-secondary";
        msg.innerHTML = '<span class="text-muted"><i class="bi bi-info-circle me-1"></i>Chưa có tiêu chí nào được chọn.</span>';
    } else if (hasInvalidWeight) {
        badge.className = "badge fs-6 ms-2 bg-danger";
        progressBar.className = "weight-progress-bar h-100 bg-danger";
        msg.innerHTML = '<span class="text-danger"><i class="bi bi-exclamation-octagon-fill me-1"></i>Có tiêu chí có trọng số &le; 0%. Vui lòng nhập trọng số &gt; 0!</span>';
    } else if (total === 100) {
        badge.className = "badge fs-6 ms-2 bg-success";
        progressBar.className = "weight-progress-bar h-100 bg-success";
        msg.innerHTML = '<span class="text-success"><i class="bi bi-check-circle-fill me-1"></i>Hợp lệ: Tổng trọng số đạt đúng chuẩn 100%.</span>';
    } else if (total < 100) {
        var diff = Math.round((100 - total) * 100) / 100;
        badge.className = "badge fs-6 ms-2 bg-warning text-dark";
        progressBar.className = "weight-progress-bar h-100 bg-warning";
        msg.innerHTML = '<span class="text-warning-emphasis"><i class="bi bi-exclamation-triangle-fill me-1"></i>Chưa đủ 100%: Còn thiếu ' + diff + '%. Cần phân bổ thêm.</span>';
    } else {
        var over = Math.round((total - 100) * 100) / 100;
        badge.className = "badge fs-6 ms-2 bg-danger";
        progressBar.className = "weight-progress-bar h-100 bg-danger";
        msg.innerHTML = '<span class="text-danger"><i class="bi bi-x-circle-fill me-1"></i>Vượt quá 100%: Đang dư ' + over + '%. Cần giảm bớt trọng số.</span>';
    }

    return { total: total, hasInvalidWeight: hasInvalidWeight };
}

function validateAndSubmit(e) {
    var code = document.getElementById("frameworkCode").value.trim();
    var name = document.getElementById("frameworkName").value.trim();
    var status = document.getElementById("frameworkStatus").value;

    if (!code) {
        alert("Vui lòng nhập Mã khung năng lực!");
        document.getElementById("frameworkCode").focus();
        e.preventDefault();
        return false;
    }

    if (!name) {
        alert("Vui lòng nhập Tên khung năng lực!");
        document.getElementById("frameworkName").focus();
        e.preventDefault();
        return false;
    }

    if (criteriaList.length === 0) {
        alert("Khung năng lực phải có ít nhất một tiêu chí đánh giá!");
        e.preventDefault();
        return false;
    }

    var result = calculateTotalWeight();

    if (result.hasInvalidWeight) {
        alert("Trọng số của tất cả tiêu chí phải lớn hơn 0%!");
        e.preventDefault();
        return false;
    }

    // Business rule bắt buộc: Tổng trọng số PHẢI bằng đúng 100%
    if (result.total !== 100) {
        alert("LỖI QUY TẮC NGHIỆP VỤ:\nTổng trọng số của khung năng lực bắt buộc phải bằng đúng 100%!\nHiện tại là: " + result.total + "%. Vui lòng điều chỉnh lại trước khi lưu.");
        e.preventDefault();
        return false;
    }

    return true;
}

// Modal Catalog
function openCriterionCatalogModal() {
    var modal = new bootstrap.Modal(document.getElementById("catalogModal"));
    // Đánh dấu checked cho các tiêu chí đã có
    var cbs = document.querySelectorAll(".catalog-cb");
    cbs.forEach(function(cb) {
        var cid = cb.getAttribute("data-id");
        var exists = criteriaList.some(function(item) { return item.criterionId === cid; });
        cb.checked = exists;
        cb.disabled = exists;
    });
    modal.show();
}

function filterCatalogList() {
    var term = document.getElementById("catalogSearchInput").value.toLowerCase().trim();
    var rows = document.querySelectorAll(".catalog-item-row");
    rows.forEach(function(row) {
        var str = row.getAttribute("data-search").toLowerCase();
        row.style.display = str.indexOf(term) !== -1 ? "" : "none";
    });
}

function addSelectedFromCatalog() {
    var cbs = document.querySelectorAll(".catalog-cb:checked:not(:disabled)");
    var count = cbs.length;
    if (count === 0) {
        bootstrap.Modal.getInstance(document.getElementById("catalogModal")).hide();
        return;
    }

    cbs.forEach(function(cb) {
        var cid = cb.getAttribute("data-id");
        var code = cb.getAttribute("data-code");
        var name = cb.getAttribute("data-name");
        var desc = cb.getAttribute("data-desc");
        var guide = cb.getAttribute("data-guide");

        criteriaList.push({
            id: "",
            criterionId: cid,
            code: code,
            name: name,
            desc: desc,
            guide: guide,
            weight: 10, // Default gợi ý 10%
            displayOrder: criteriaList.length + 1
        });
    });

    // Tự động phân bổ lại trọng số đều nhau nếu tổng trọng số chưa hợp lý
    autoDistributeWeightsIfHelpful();

    bootstrap.Modal.getInstance(document.getElementById("catalogModal")).hide();
    renderCriteriaTable();
}

function autoDistributeWeightsIfHelpful() {
    if (criteriaList.length > 0) {
        var total = criteriaList.reduce(function(acc, cur) { return acc + (parseFloat(cur.weight) || 0); }, 0);
        // Nếu tổng = 0 hoặc chưa chạm 100%, có thể chia đều 100%
        if (total === 0 || total > 150) {
            var n = criteriaList.length;
            var base = Math.floor((100 / n) * 10) / 10;
            var sum = 0;
            for (var i = 0; i < n - 1; i++) {
                criteriaList[i].weight = base;
                sum += base;
            }
            criteriaList[n - 1].weight = Math.round((100 - sum) * 10) / 10;
        }
    }
}

// Modal Quick Create Criterion
function openQuickCreateCriterionModal() {
    document.getElementById("quickCritAlert").innerHTML = "";
    document.getElementById("quickCritCode").value = "";
    document.getElementById("quickCritName").value = "";
    document.getElementById("quickCritDesc").value = "";
    document.getElementById("quickCritGuide").value = "";
    var modal = new bootstrap.Modal(document.getElementById("quickCriterionModal"));
    modal.show();
}

function saveQuickCriterion() {
    var name = document.getElementById("quickCritName").value.trim();
    var code = document.getElementById("quickCritCode").value.trim();
    var desc = document.getElementById("quickCritDesc").value.trim();
    var guide = document.getElementById("quickCritGuide").value.trim();
    var alertBox = document.getElementById("quickCritAlert");

    if (!name) {
        alertBox.innerHTML = '<div class="alert alert-danger py-2 small mb-2">Tên tiêu chí không được để trống!</div>';
        return;
    }

    var btn = document.getElementById("btnQuickSave");
    btn.disabled = true;

    var params = new URLSearchParams();
    params.append("name", name);
    params.append("code", code);
    params.append("description", desc);
    params.append("evaluationGuideline", guide);

    fetch('${pageContext.request.contextPath}/competency-frameworks/api/quick-create-criterion', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
        body: params.toString()
    })
    .then(function(res) { return res.json(); })
    .then(function(res) {
        btn.disabled = false;
        if (res.success && res.data) {
            criteriaList.push({
                id: "",
                criterionId: res.data.id,
                code: res.data.code,
                name: res.data.name,
                desc: res.data.description,
                guide: res.data.evaluationGuideline,
                weight: 10,
                displayOrder: criteriaList.length + 1
            });
            bootstrap.Modal.getInstance(document.getElementById("quickCriterionModal")).hide();
            renderCriteriaTable();
        } else {
            alertBox.innerHTML = '<div class="alert alert-danger py-2 small mb-2">' + (res.message || 'Lỗi lưu tiêu chí!') + '</div>';
        }
    })
    .catch(function(err) {
        btn.disabled = false;
        alertBox.innerHTML = '<div class="alert alert-danger py-2 small mb-2">Lỗi kết nối: ' + err.message + '</div>';
    });
}

function escapeHtml(text) {
    if (!text) return "";
    return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
}
</script>

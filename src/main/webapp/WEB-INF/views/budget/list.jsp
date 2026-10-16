<%--
  ==============================================================================
  GIAO DIỆN QUẢN LÝ ĐỊNH BIÊN & NGÂN SÁCH NHÂN SỰ (budget/list.jsp)
  Chức năng:
  - Quản lý hạn mức nhân sự (Target Headcount) và ngân sách lương tối đa của từng phòng ban theo năm tài chính.
  - Theo dõi tỷ lệ sử dụng định biên thời gian thực: Đã tuyển (Current), Đang tuyển (In progress), Còn lại (Remaining).
  - Cảnh báo trực quan (màu vàng/đỏ) khi đợt tuyển dụng chạm hoặc vượt trần định biên được duyệt.
  - Modal tạo mới và cập nhật kế hoạch định biên phòng ban hàng năm.
  ==============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<c:set var="pageTitle" value="Quản lý Định biên & Ngân sách Nhân sự" />
<c:set var="breadcrumb" value="Định biên & Ngân sách" />
<c:set var="activeMenu" value="budget" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- TIÊU ĐỀ TRANG VÀ THAO TÁC CHÍNH -->
        <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-3">
            <div>
                <h4 class="fw-bold text-dark mb-1">
                    <i class="bi bi-wallet2 text-primary me-2"></i>Quản lý Định biên &amp; Ngân sách Nhân sự
                </h4>
                <p class="text-muted small mb-0">Theo dõi chỉ tiêu headcount và hạn mức ngân sách lương phê duyệt cho từng đơn vị theo năm tài chính</p>
            </div>
            <div>
                <c:if test="${canManage}">
                    <button type="button" class="btn btn-primary shadow-sm" onclick="openCreateModal()">
                        <i class="bi bi-plus-circle me-1"></i> Khai báo định biên mới
                    </button>
                </c:if>
            </div>
        </div>

        <!-- BẢNG ĐỊNH BIÊN & NGÂN SÁCH THEO PHÒNG BAN -->
        <div class="card border shadow-sm mb-4">
            <div class="card-header bg-white py-3 border-bottom d-flex flex-wrap align-items-center justify-content-between gap-3">
                <div>
                    <h6 class="fw-bold mb-0 text-dark">
                        <i class="bi bi-table text-primary me-2"></i>Bảng Định biên &amp; Ngân sách theo Phòng ban năm ${fiscalYear}
                    </h6>
                    <small class="text-muted">Quản lý tổng số headcount và ngân sách lương phê duyệt cho từng đơn vị</small>
                </div>
            </div>

            <!-- Bộ lọc năm và phòng ban -->
            <div class="card-body bg-light border-bottom p-3" id="budgetFilterBar">
                <form method="get" action="${pageContext.request.contextPath}/headcount-budgets" class="row g-2 align-items-center">
                    <div class="col-auto">
                        <label class="small fw-semibold text-muted">Năm tài chính:</label>
                    </div>
                    <div class="col-auto">
                        <select name="year" class="form-select form-select-sm fw-bold" onchange="this.form.submit()">
                            <option value="2026" ${fiscalYear == 2026 ? 'selected' : ''}>Năm 2026</option>
                            <option value="2025" ${fiscalYear == 2025 ? 'selected' : ''}>Năm 2025</option>
                            <option value="2027" ${fiscalYear == 2027 ? 'selected' : ''}>Năm 2027</option>
                        </select>
                    </div>

                    <div class="col-auto ms-md-3">
                        <label class="small fw-semibold text-muted">Phòng ban:</label>
                    </div>
                    <div class="col-auto">
                        <select name="departmentId" class="form-select form-select-sm" onchange="this.form.submit()">
                            <option value="">-- Tất cả phòng ban --</option>
                            <c:forEach var="d" items="${departments}">
                                <option value="${d.id}" ${selectedDept == d.id ? 'selected' : ''}>${d.name}</option>
                            </c:forEach>
                        </select>
                    </div>

                    <div class="col-auto ms-md-3">
                        <div class="input-group input-group-sm">
                            <input type="text" name="keyword" class="form-control" placeholder="Tìm kiếm..." value="<c:out value='${keyword}'/>">
                            <button class="btn btn-outline-secondary" type="submit"><i class="bi bi-search"></i></button>
                        </div>
                    </div>

                    <c:if test="${not empty selectedDept || not empty keyword || fiscalYear != 2026}">
                        <div class="col-auto">
                            <a href="${pageContext.request.contextPath}/headcount-budgets" class="btn btn-outline-secondary btn-sm" title="Đặt lại">
                                <i class="bi bi-arrow-counterclockwise"></i> Đặt lại
                            </a>
                        </div>
                    </c:if>
                </form>
            </div>

            <!-- Thống kê tổng quan KPI -->
            <div class="card-body p-3 bg-white border-bottom">
                <div class="row g-3 text-center">
                    <div class="col-6 col-md-3 border-end">
                        <small class="text-muted d-block text-uppercase" style="font-size: 0.72rem;">Tổng chỉ tiêu Headcount</small>
                        <h5 class="fw-bold text-dark mb-0">${kpis.totalTargetHeadcount} <span class="fs-6 text-muted fw-normal">người</span></h5>
                    </div>
                    <div class="col-6 col-md-3 border-end">
                        <small class="text-muted d-block text-uppercase" style="font-size: 0.72rem;">Headcount còn lại</small>
                        <h5 class="fw-bold ${kpis.totalRemainingHeadcount < 0 ? 'text-danger' : 'text-success'} mb-0">
                            ${kpis.totalRemainingHeadcount} <span class="fs-6 text-muted fw-normal">chỉ tiêu</span>
                        </h5>
                    </div>
                    <div class="col-6 col-md-3 border-end">
                        <small class="text-muted d-block text-uppercase" style="font-size: 0.72rem;">Tổng ngân sách duyệt</small>
                        <h5 class="fw-bold text-dark mb-0">
                            <fmt:formatNumber value="${kpis.totalApprovedBudget}" pattern="#,###" /> <span class="fs-6 text-muted fw-normal">VNĐ</span>
                        </h5>
                    </div>
                    <div class="col-6 col-md-3">
                        <small class="text-muted d-block text-uppercase" style="font-size: 0.72rem;">Ngân sách còn lại</small>
                        <h5 class="fw-bold ${kpis.totalRemainingBudget < 0 ? 'text-danger' : 'text-success'} mb-0">
                            <fmt:formatNumber value="${kpis.totalRemainingBudget}" pattern="#,###" /> <span class="fs-6 text-muted fw-normal">VNĐ</span>
                        </h5>
                    </div>
                </div>
            </div>

            <!-- Bảng danh sách định biên theo phòng ban -->
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-hover mb-0 align-middle">
                        <thead class="table-light">
                            <tr style="font-size: 0.75rem; text-transform: uppercase;">
                                <th style="width: 25%;">Phòng ban</th>
                                <th style="width: 8%;" class="text-center">Năm</th>
                                <th style="width: 10%;" class="text-center">Chỉ tiêu HC</th>
                                <th style="width: 9%;" class="text-center">Đã dùng</th>
                                <th style="width: 9%;" class="text-center">Còn lại</th>
                                <th style="width: 14%;" class="text-end">Tổng ngân sách</th>
                                <th style="width: 14%;" class="text-end">Ngân sách còn</th>
                                <th style="width: 11%;" class="text-end pe-3">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${not empty budgets}">
                                    <c:forEach var="b" items="${budgets}">
                                        <tr>
                                            <td>
                                                <div class="fw-bold text-dark small">${b.departmentName}</div>
                                                <small class="text-muted font-monospace" style="font-size: 0.72rem;">${b.departmentCode}</small>
                                                <c:if test="${not empty b.description}">
                                                    <div class="text-muted text-xs text-truncate" style="max-width: 240px;">${b.description}</div>
                                                </c:if>
                                            </td>
                                            <td class="text-center font-monospace small">${b.fiscalYear}</td>
                                            <td class="text-center fw-bold text-dark">${b.targetHeadcount}</td>
                                            <td class="text-center text-muted small">${b.usedHeadcount}</td>
                                            <td class="text-center">
                                                <span class="badge ${b.remainingHeadcount < 0 ? 'bg-danger text-white' : (b.remainingHeadcount == 0 ? 'bg-warning text-dark' : 'bg-success text-white')} fw-bold">
                                                    ${b.remainingHeadcount}
                                                </span>
                                            </td>
                                            <td class="text-end font-monospace small fw-semibold text-dark">${b.formattedTotalBudget}</td>
                                            <td class="text-end font-monospace small fw-bold ${b.remainingSalaryBudget < 0 ? 'text-danger' : 'text-success'}">
                                                ${b.formattedRemainingBudget}
                                            </td>
                                            <td class="text-end pe-3">
                                                <c:if test="${canManage}">
                                                    <button type="button" class="btn btn-outline-primary btn-sm py-0 px-2 me-1" 
                                                            title="Sửa định biên" 
                                                            onclick="openEditModal('${b.id}', '${b.departmentId}', ${b.fiscalYear}, ${b.targetHeadcount}, '${b.totalSalaryBudget.toBigInteger()}', '${fn:escapeXml(b.description)}', '${b.status}')">
                                                        <i class="bi bi-pencil"></i>
                                                    </button>
                                                    <button type="button" class="btn btn-outline-danger btn-sm py-0 px-2" 
                                                            title="Xóa định biên" 
                                                            onclick="confirmDelete('${b.id}', '${b.departmentName}', ${b.fiscalYear})">
                                                        <i class="bi bi-trash"></i>
                                                    </button>
                                                </c:if>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <tr>
                                        <td colspan="8" class="text-center py-4 text-muted">
                                            <i class="bi bi-inbox fs-2 d-block mb-1 text-secondary opacity-50"></i>
                                            Chưa có dữ liệu định biên nào cho năm tài chính ${fiscalYear}
                                        </td>
                                    </tr>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

<!-- MODAL KHAI BÁO / CHỈNH SỬA ĐỊNH BIÊN -->
<div class="modal fade" id="budgetModal" tabindex="-1" aria-labelledby="budgetModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <form id="frmBudget" action="${pageContext.request.contextPath}/headcount-budgets/save" method="post" onsubmit="return validateBudgetForm()">
                <input type="hidden" name="id" id="budgetId">

                <div class="modal-header bg-light border-bottom py-3">
                    <h6 class="modal-title fw-bold text-dark" id="budgetModalLabel">
                        <i class="bi bi-wallet2 text-primary me-2"></i>Khai báo Định biên &amp; Ngân sách
                    </h6>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                </div>

                <div class="modal-body p-4">
                    <!-- Phòng ban -->
                    <div class="mb-3">
                        <label class="form-label small fw-bold text-dark">
                            Phòng ban áp dụng <span class="text-danger">*</span>
                        </label>
                        <select name="departmentId" id="modalDeptId" class="form-select" required>
                            <option value="">-- Chọn phòng ban --</option>
                            <c:forEach var="d" items="${departments}">
                                <option value="${d.id}">${d.name} (${d.code})</option>
                            </c:forEach>
                        </select>
                    </div>

                    <!-- Năm tài chính -->
                    <div class="row g-3 mb-3">
                        <div class="col-6">
                            <label class="form-label small fw-bold text-dark">
                                Năm tài chính <span class="text-danger">*</span>
                            </label>
                            <input type="number" name="fiscalYear" id="modalFiscalYear" class="form-control text-center fw-bold"
                                   min="2020" max="2050" value="${fiscalYear}" required>
                        </div>
                        <div class="col-6">
                            <label class="form-label small fw-bold text-dark">Trạng thái</label>
                            <select name="status" id="modalStatus" class="form-select">
                                <option value="ACTIVE">Đang áp dụng</option>
                                <option value="LOCKED">Đã khóa</option>
                                <option value="INACTIVE">Ngừng áp dụng</option>
                            </select>
                        </div>
                    </div>

                    <!-- Chỉ tiêu Headcount -->
                    <div class="mb-3">
                        <label class="form-label small fw-bold text-dark">
                            Chỉ tiêu Headcount (Người) <span class="text-danger">*</span>
                        </label>
                        <div class="input-group">
                            <input type="number" name="targetHeadcount" id="modalTargetHeadcount" class="form-control fw-bold"
                                   min="0" max="1000" placeholder="VD: 15" required>
                            <span class="input-group-text">Nhân sự</span>
                        </div>
                    </div>

                    <!-- Ngân sách lương -->
                    <div class="mb-3">
                        <label class="form-label small fw-bold text-dark">
                            Ngân sách lương phê duyệt (VNĐ) <span class="text-danger">*</span>
                        </label>
                        <div class="input-group">
                            <input type="text" name="totalSalaryBudget" id="modalTotalSalaryBudget" class="form-control fw-bold text-end"
                                   placeholder="VD: 500.000.000" required oninput="formatCurrencyInput(this)">
                            <span class="input-group-text font-monospace">VNĐ</span>
                        </div>
                    </div>

                    <!-- Mô tả / Ghi chú -->
                    <div class="mb-2">
                        <label class="form-label small fw-semibold text-muted">Ghi chú / Quyết định giao chỉ tiêu</label>
                        <textarea name="description" id="modalDescription" class="form-control" rows="2"
                                  placeholder="Nhập ghi chú..."></textarea>
                    </div>
                </div>

                <div class="modal-footer bg-light border-top py-2">
                    <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Hủy bỏ</button>
                    <button type="submit" class="btn btn-primary btn-sm px-4">
                        <i class="bi bi-check-lg me-1"></i> Lưu định biên
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- MODAL XÁC NHẬN XÓA -->
<div class="modal fade" id="deleteModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-sm">
        <div class="modal-content border-0 shadow">
            <form action="${pageContext.request.contextPath}/headcount-budgets/delete" method="post">
                <input type="hidden" name="id" id="deleteId">
                <div class="modal-body text-center p-4">
                    <i class="bi bi-exclamation-triangle-fill text-danger fs-1 d-block mb-3"></i>
                    <h6 class="fw-bold text-dark mb-2">Xác nhận xóa định biên?</h6>
                    <p class="text-muted small mb-0" id="deleteMessage"></p>
                </div>
                <div class="modal-footer bg-light border-top justify-content-center py-2">
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-danger btn-sm px-3">Xác nhận xóa</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    function formatCurrencyInput(input) {
        let val = input.value.replace(/\D/g, '');
        if (val === '') {
            input.value = '';
            return;
        }
        input.value = new Intl.NumberFormat('vi-VN').format(val);
    }

    function openCreateModal() {
        document.getElementById('frmBudget').reset();
        document.getElementById('budgetId').value = '';
        document.getElementById('budgetModalLabel').innerHTML = '<i class="bi bi-wallet2 text-primary me-2"></i>Khai báo Định biên & Ngân sách Mới';
        document.getElementById('modalFiscalYear').value = '${fiscalYear}';
        document.getElementById('modalDeptId').disabled = false;
        
        let modal = new bootstrap.Modal(document.getElementById('budgetModal'));
        modal.show();
    }

    function openEditModal(id, deptId, year, headcount, budget, desc, status) {
        document.getElementById('budgetId').value = id;
        document.getElementById('modalDeptId').value = deptId;
        document.getElementById('modalFiscalYear').value = year;
        document.getElementById('modalTargetHeadcount').value = headcount;
        document.getElementById('modalTotalSalaryBudget').value = new Intl.NumberFormat('vi-VN').format(budget);
        document.getElementById('modalDescription').value = desc || '';
        document.getElementById('modalStatus').value = status || 'ACTIVE';
        
        document.getElementById('budgetModalLabel').innerHTML = '<i class="bi bi-pencil-square text-primary me-2"></i>Cập nhật Định biên & Ngân sách';
        
        let modal = new bootstrap.Modal(document.getElementById('budgetModal'));
        modal.show();
    }

    function confirmDelete(id, deptName, year) {
        document.getElementById('deleteId').value = id;
        document.getElementById('deleteMessage').innerText = 'Bạn có chắc chắn muốn xóa định biên của ' + deptName + ' năm ' + year + '?';
        let modal = new bootstrap.Modal(document.getElementById('deleteModal'));
        modal.show();
    }

    function validateBudgetForm() {
        let dept = document.getElementById('modalDeptId').value;
        let hc = document.getElementById('modalTargetHeadcount').value;
        if (!dept) {
            let msg = 'Vui lòng chọn phòng ban áp dụng!';
            if (window.showToast) {
                window.showToast(msg, 'warning', 'Thiếu thông tin');
            } else {
                alert(msg);
            }
            return false;
        }
        if (hc < 0) {
            let msg = 'Chỉ tiêu headcount không được là số âm!';
            if (window.showToast) {
                window.showToast(msg, 'warning', 'Số lượng không hợp lệ');
            } else {
                alert(msg);
            }
            return false;
        }
        return true;
    }
</script>

<jsp:include page="../common/footer.jsp" />

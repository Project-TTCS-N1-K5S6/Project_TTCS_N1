<%--
  ==============================================================================
  GIAO DIỆN QUẢN LÝ MA TRẬN PHÊ DUYỆT (approval/list.jsp)
  Chức năng:
  - Hiển thị danh sách các quy tắc ma trận phê duyệt yêu cầu tuyển dụng (Approval Matrix Rules).
  - Quản lý điều kiện áp dụng: Theo Phòng ban, Ngưỡng lương (Threshold / Range), Mức độ ưu tiên (Priority).
  - Hiển thị số bước phê duyệt và các vai trò phê duyệt trong từng chuỗi.
  - Hỗ trợ kích hoạt/vô hiệu hóa trạng thái (ACTIVE / INACTIVE) và liên kết chỉnh sửa cấu hình các bước.
  ==============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Ma trận phê duyệt" />
<c:set var="breadcrumb" value="Ma trận phê duyệt" />
<c:set var="activeMenu" value="approval-matrix" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Breadcrumb & Tiêu đề đầu trang -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <nav aria-label="breadcrumb">
                    <ol class="breadcrumb mb-1 text-xs">
                        <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/dashboard" class="text-decoration-none text-muted">Quản trị hệ thống</a></li>
                        <li class="breadcrumb-item text-muted">Cấu hình phê duyệt</li>
                        <li class="breadcrumb-item active text-primary fw-semibold" aria-current="page">Ma trận phê duyệt</li>
                    </ol>
                </nav>
                <h4 class="fw-bold mb-1 text-dark">
                    <i class="bi bi-diagram-2-fill text-primary me-2"></i>Ma trận phê duyệt
                </h4>
                <p class="text-muted small mb-0">
                    Quản lý quy tắc phân cấp phê duyệt theo phòng ban và mức lương đề xuất. Tự động bổ sung cấp khi vượt hạn mức.
                </p>
            </div>
            <div class="d-flex align-items-center gap-2">
                <a href="${pageContext.request.contextPath}/admin/approval-matrix" class="btn btn-outline-secondary shadow-sm" title="Làm mới danh sách">
                    <i class="bi bi-arrow-clockwise me-1"></i> Làm mới
                </a>
                <a href="${pageContext.request.contextPath}/admin/approval-matrix/create" class="btn btn-primary shadow-sm">
                    <i class="bi bi-plus-circle-fill me-1"></i> Thêm cấu hình
                </a>
            </div>
        </div>

        <!-- Khu vực tìm kiếm và bộ lọc (Không bị chen chúc, mỗi ô đủ khoảng rộng) -->
        <div class="card mb-4 border shadow-sm">
            <div class="card-body p-3 p-md-4">
                <form method="get" action="${pageContext.request.contextPath}/admin/approval-matrix" class="row g-3">
                    <div class="col-12 col-md-5 col-lg-4">
                        <label class="form-label small fw-semibold text-secondary mb-1">Tìm kiếm</label>
                        <div class="input-group">
                            <span class="input-group-text bg-white"><i class="bi bi-search text-muted"></i></span>
                            <input type="text" name="search" class="form-control" placeholder="Tìm theo mã, tên ma trận, ghi chú..." value="${paramSearch}">
                        </div>
                    </div>
                    <div class="col-12 col-md-4 col-lg-3">
                        <label class="form-label small fw-semibold text-secondary mb-1">Phòng ban áp dụng</label>
                        <select name="departmentId" class="form-select">
                            <option value="">-- Tất cả phòng ban --</option>
                            <option value="GLOBAL" ${paramDept == 'GLOBAL' ? 'selected' : ''}>Toàn công ty (Áp dụng chung)</option>
                            <c:forEach var="dept" items="${departments}">
                                <option value="${dept.id}" ${paramDept == dept.id ? 'selected' : ''}>${dept.name}</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-12 col-md-3 col-lg-3">
                        <label class="form-label small fw-semibold text-secondary mb-1">Trạng thái cấu hình</label>
                        <select name="status" class="form-select">
                            <option value="">-- Tất cả trạng thái --</option>
                            <option value="ACTIVE" ${paramStatus == 'ACTIVE' ? 'selected' : ''}>Đang áp dụng</option>
                            <option value="INACTIVE" ${paramStatus == 'INACTIVE' ? 'selected' : ''}>Tạm dừng</option>
                        </select>
                    </div>
                    <div class="col-12 col-lg-2 d-flex align-items-end gap-2">
                        <button type="submit" class="btn btn-dark flex-grow-1" style="min-height: 38px;" title="Lọc danh sách">
                            <i class="bi bi-funnel-fill"></i>
                        </button>
                        <a href="${pageContext.request.contextPath}/admin/approval-matrix" class="btn btn-outline-secondary" style="min-height: 38px; width: 44px; display: inline-flex; align-items: center; justify-content: center;" title="Đặt lại bộ lọc">
                            <i class="bi bi-arrow-clockwise"></i>
                        </a>
                    </div>
                </form>
            </div>
        </div>

        <!-- Bảng dữ liệu Ma trận phê duyệt -->
        <div class="card mb-4 shadow-sm border">
            <div class="card-body p-0">
                <div class="table-responsive w-100">
                    <table class="table table-hover align-middle mb-0" style="font-size: 0.8125rem;">
                        <thead class="table-light">
                            <tr>
                                <th class="ps-3" style="width: 22%;">Tên và mã cấu hình</th>
                                <th style="width: 15%;">Phòng ban</th>
                                <th style="width: 18%;">Khoảng lương &amp; hạn mức</th>
                                <th style="width: 29%;">Chuỗi cấp phê duyệt</th>
                                <th class="text-center" style="width: 8%;">Trạng thái</th>
                                <th class="text-end pe-3" style="width: 8%;">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:choose>
                                <c:when test="${not empty matrices}">
                                    <c:forEach var="m" items="${matrices}">
                                        <tr>
                                            <td class="ps-3">
                                                <div class="fw-bold text-dark text-break" style="font-size: 0.825rem;">${m.name}</div>
                                                <div class="d-flex align-items-center gap-1 flex-wrap mt-0.5">
                                                    <span class="badge bg-light text-secondary border font-monospace px-1.5 py-0.5" style="font-size: 0.7rem;">
                                                        ${m.code}
                                                    </span>
                                                    <span class="text-muted" style="font-size: 0.7rem;" title="Thời điểm cập nhật gần nhất">
                                                        <i class="bi bi-clock me-0.5"></i><fmt:formatDate value="${m.updatedAt}" pattern="dd/MM/yyyy HH:mm" />
                                                    </span>
                                                </div>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${not empty m.departmentName}">
                                                        <span class="fw-semibold text-secondary small">
                                                            <i class="bi bi-building me-1 text-primary"></i>${m.departmentName}
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-primary-subtle text-primary border border-primary-subtle" style="font-size: 0.72rem;">
                                                            <i class="bi bi-globe me-0.5"></i>Toàn công ty
                                                        </span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <div class="fw-semibold text-dark small">${m.formattedSalaryRange}</div>
                                                <div class="text-secondary mt-0.5" style="font-size: 0.72rem;">
                                                    <span class="badge bg-light text-muted border px-1.5 py-0.5">${m.formattedCondition}</span>
                                                </div>
                                                <c:if test="${not empty m.thresholdAmount}">
                                                    <div class="text-danger mt-0.5 fw-medium" style="font-size: 0.72rem;">
                                                        <i class="bi bi-arrow-up-circle-fill me-0.5"></i>Vượt hạn: <fmt:formatNumber value="${m.thresholdAmount}" pattern="#,###" /> đ
                                                    </div>
                                                </c:if>
                                            </td>
                                            <td>
                                                <div class="d-flex flex-wrap align-items-center gap-1 py-1">
                                                    <span class="badge bg-secondary-subtle text-secondary border px-1.5 py-0.5" style="font-size: 0.68rem;" title="Tổng số cấp trong chuỗi">
                                                        ${m.stepCount} cấp
                                                    </span>
                                                    <c:forEach var="s" items="${m.steps}" varStatus="status">
                                                        <span class="badge ${s.escalationStep ? 'bg-danger-subtle text-danger border border-danger-subtle' : 'bg-light text-dark border'} px-1.5 py-0.5" style="font-size: 0.72rem;" title="${s.escalationStep ? s.escalationCondition : s.approverDisplayName}">
                                                            ${s.stepOrder}. ${s.stepName}
                                                            <c:if test="${s.escalationStep}">
                                                                <i class="bi bi-arrow-up-circle-fill text-danger ms-0.5" title="Cấp tự động bổ sung khi vượt hạn mức"></i>
                                                            </c:if>
                                                        </span>
                                                        <c:if test="${!status.last}">
                                                            <i class="bi bi-chevron-right text-muted" style="font-size: 0.6rem;"></i>
                                                        </c:if>
                                                    </c:forEach>
                                                </div>
                                            </td>
                                            <td class="text-center">
                                                <c:choose>
                                                    <c:when test="${m.status == 'ACTIVE'}">
                                                        <span class="badge bg-success-subtle text-success border border-success-subtle px-1.5 py-1" style="font-size: 0.72rem;">
                                                            <i class="bi bi-check-circle-fill me-0.5"></i>Áp dụng
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-secondary-subtle text-secondary border px-1.5 py-1" style="font-size: 0.72rem;">
                                                            <i class="bi bi-pause-circle me-0.5"></i>Tạm dừng
                                                        </span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td class="text-end pe-3 text-nowrap">
                                                <div class="btn-group btn-group-sm">
                                                    <button type="button" class="btn btn-outline-info px-1.5 py-0.5" title="Xem chi tiết" onclick="viewMatrixDetail('${m.id}')">
                                                        <i class="bi bi-eye"></i>
                                                    </button>
                                                    <a href="${pageContext.request.contextPath}/admin/approval-matrix/edit?id=${m.id}" class="btn btn-outline-primary px-1.5 py-0.5" title="Chỉnh sửa">
                                                        <i class="bi bi-pencil"></i>
                                                    </a>
                                                    <button type="button" class="btn btn-outline-warning px-1.5 py-0.5" title="${m.status == 'ACTIVE' ? 'Tạm dừng áp dụng' : 'Kích hoạt áp dụng'}" 
                                                            onclick="confirmToggleStatus('${m.id}', '${m.code}', '${m.status}')">
                                                        <i class="bi ${m.status == 'ACTIVE' ? 'bi-pause-circle' : 'bi-play-circle'}"></i>
                                                    </button>
                                                    <button type="button" class="btn btn-outline-danger px-1.5 py-0.5" title="Xóa cấu hình" 
                                                            onclick="confirmDeleteMatrix('${m.id}', '${m.code}', '${m.name}')">
                                                        <i class="bi bi-trash"></i>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <tr>
                                        <td colspan="6" class="text-center py-5 text-muted">
                                            <div class="mb-3">
                                                <i class="bi bi-diagram-2 text-secondary" style="font-size: 3rem; opacity: 0.4;"></i>
                                            </div>
                                            <h6 class="fw-bold text-dark">Chưa có Ma trận phê duyệt nào</h6>
                                            <p class="small text-muted mb-3">Hãy tạo cấu hình đầu tiên để phân cấp phê duyệt tự động theo phòng ban và mức lương.</p>
                                            <a href="${pageContext.request.contextPath}/admin/approval-matrix/create" class="btn btn-sm btn-primary">
                                                <i class="bi bi-plus-circle me-1"></i> Tạo ma trận mới
                                            </a>
                                        </td>
                                    </tr>
                                </c:otherwise>
                            </c:choose>
                        </tbody>
                    </table>
                </div>

                <!-- Phân trang danh sách -->
                <c:if test="${totalPages > 1}">
                    <div class="d-flex flex-wrap justify-content-between align-items-center p-3 border-top gap-2">
                        <small class="text-muted">
                            Hiển thị trang <strong>${currentPage}</strong> / <strong>${totalPages}</strong> (Tổng cộng <strong>${totalCount}</strong> cấu hình)
                        </small>
                        <nav aria-label="Page navigation">
                            <ul class="pagination pagination-sm mb-0">
                                <li class="page-item ${currentPage <= 1 ? 'disabled' : ''}">
                                    <a class="page-link" href="${pageContext.request.contextPath}/admin/approval-matrix?page=${currentPage - 1}&search=${paramSearch}&departmentId=${paramDept}&status=${paramStatus}">
                                        <i class="bi bi-chevron-left"></i>
                                    </a>
                                </li>
                                <c:forEach begin="1" end="${totalPages}" var="p">
                                    <li class="page-item ${p == currentPage ? 'active' : ''}">
                                        <a class="page-link" href="${pageContext.request.contextPath}/admin/approval-matrix?page=${p}&search=${paramSearch}&departmentId=${paramDept}&status=${paramStatus}">${p}</a>
                                    </li>
                                </c:forEach>
                                <li class="page-item ${currentPage >= totalPages ? 'disabled' : ''}">
                                    <a class="page-link" href="${pageContext.request.contextPath}/admin/approval-matrix?page=${currentPage + 1}&search=${paramSearch}&departmentId=${paramDept}&status=${paramStatus}">
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

<!-- Modal Xem chi tiết Ma trận phê duyệt -->
<div class="modal fade" id="matrixDetailModal" tabindex="-1" aria-labelledby="matrixDetailModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content shadow border-0">
            <div class="modal-header bg-light py-3">
                <h5 class="modal-title fw-bold text-dark" id="matrixDetailModalLabel">
                    <i class="bi bi-diagram-2 text-primary me-2"></i>Chi tiết Ma trận phê duyệt
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-4" id="matrixDetailBody">
                <div class="text-center py-4">
                    <div class="spinner-border text-primary" role="status"></div>
                    <div class="mt-2 text-muted small">Đang tải dữ liệu cấu hình...</div>
                </div>
            </div>
            <div class="modal-footer bg-light py-2">
                <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Đóng</button>
            </div>
        </div>
    </div>
</div>

<!-- Form ẩn xử lý Đổi trạng thái -->
<form id="statusForm" method="post" action="${pageContext.request.contextPath}/admin/approval-matrix/status" style="display:none;">
    <input type="hidden" name="id" id="statusMatrixId" value="" />
</form>

<!-- Modal Xác nhận Xóa Ma trận phê duyệt -->
<div class="modal fade" id="deleteConfirmModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content shadow border-0">
            <form id="deleteForm" method="post" action="${pageContext.request.contextPath}/admin/approval-matrix/delete">
                <input type="hidden" name="id" id="deleteMatrixId" value="" />
                <div class="modal-header bg-danger-subtle text-danger py-3">
                    <h5 class="modal-title fw-bold">
                        <i class="bi bi-exclamation-triangle-fill me-2"></i>Xác nhận xóa ma trận phê duyệt
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <p class="mb-2 text-dark">
                        Bạn có chắc chắn muốn xóa cấu hình ma trận phê duyệt:
                    </p>
                    <div class="p-2.5 bg-light rounded border fw-bold text-dark mb-3" id="deleteMatrixName"></div>
                    <div class="alert alert-warning text-xs mb-0">
                        <i class="bi bi-info-circle me-1"></i> Lưu ý: Thao tác này chỉ xóa mềm cấu hình. Các yêu cầu tuyển dụng đã snapshot chuỗi phê duyệt trước đó vẫn được bảo toàn nguyên vẹn.
                    </div>
                </div>
                <div class="modal-footer bg-light py-2">
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Hủy bỏ</button>
                    <button type="submit" class="btn btn-danger btn-sm">Xác nhận xóa</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
function viewMatrixDetail(id) {
    var modal = new bootstrap.Modal(document.getElementById('matrixDetailModal'));
    modal.show();
    var body = document.getElementById('matrixDetailBody');
    body.innerHTML = '<div class="text-center py-4"><div class="spinner-border text-primary" role="status"></div><div class="mt-2 text-muted small">Đang tải dữ liệu...</div></div>';

    fetch('${pageContext.request.contextPath}/admin/approval-matrix/detail?id=' + encodeURIComponent(id))
        .then(function(res) { return res.json(); })
        .then(function(data) {
            if (data.error) {
                body.innerHTML = '<div class="alert alert-danger mb-0">' + data.error + '</div>';
                return;
            }
            var stepsHtml = '';
            if (data.steps && data.steps.length > 0) {
                stepsHtml += '<div class="d-flex flex-column gap-2 mt-2">';
                data.steps.forEach(function(s) {
                    stepsHtml += '<div class="p-3 rounded border ' + (s.escalationStep ? 'border-danger-subtle bg-danger-subtle bg-opacity-10' : 'bg-light') + ' d-flex align-items-center justify-content-between">';
                    stepsHtml += '  <div>';
                    stepsHtml += '    <div class="fw-bold text-dark text-xs"><span class="badge ' + (s.escalationStep ? 'bg-danger' : 'bg-primary') + ' me-1">Cấp ' + s.stepOrder + '</span> ' + s.stepName + '</div>';
                    stepsHtml += '    <small class="text-muted d-block mt-1">Người phụ trách: <strong class="text-dark">' + s.approverDisplayName + '</strong></small>';
                    if (s.escalationStep) {
                        stepsHtml += '    <small class="text-danger fw-semibold d-block mt-0.5"><i class="bi bi-arrow-up-circle-fill me-1"></i>' + (s.escalationCondition || 'Cấp tự động bổ sung khi mức lương vượt hạn mức') + '</small>';
                    }
                    stepsHtml += '  </div>';
                    stepsHtml += '</div>';
                });
                stepsHtml += '</div>';
            } else {
                stepsHtml = '<div class="text-muted small">Chưa có cấp phê duyệt nào.</div>';
            }

            var html = '';
            html += '<div class="row g-3">';
            html += '  <div class="col-md-6"><small class="text-muted d-block">Mã cấu hình:</small><strong class="font-monospace text-dark">' + (data.code || '') + '</strong></div>';
            html += '  <div class="col-md-6"><small class="text-muted d-block">Phòng ban áp dụng:</small><strong class="text-dark">' + (data.departmentName || 'Toàn công ty (Áp dụng chung)') + '</strong></div>';
            html += '  <div class="col-md-6"><small class="text-muted d-block">Khoảng lương áp dụng:</small><strong class="text-dark">' + (data.formattedSalaryRange || 'Không giới hạn') + '</strong></div>';
            html += '  <div class="col-md-6"><small class="text-muted d-block">Trạng thái:</small><span class="badge ' + (data.status === 'ACTIVE' ? 'bg-success-subtle text-success border border-success-subtle' : 'bg-secondary-subtle text-secondary') + '">' + (data.status === 'ACTIVE' ? 'Đang áp dụng' : 'Tạm dừng') + '</span></div>';
            if (data.description) {
                html += '  <div class="col-12"><small class="text-muted d-block">Mô tả quy tắc:</small><div class="p-2.5 bg-light rounded text-secondary small">' + data.description + '</div></div>';
            }
            html += '</div>';
            html += '<hr class="my-3"/>';
            html += '<h6 class="fw-bold text-dark mb-2"><i class="bi bi-diagram-3 text-primary me-1"></i> Chuỗi các cấp phê duyệt tuần tự:</h6>';
            html += stepsHtml;

            body.innerHTML = html;
        })
        .catch(function(err) {
            body.innerHTML = '<div class="alert alert-danger mb-0">Lỗi tải dữ liệu: ' + err.message + '</div>';
        });
}

function confirmToggleStatus(id, code, currentStatus) {
    var act = (currentStatus === 'ACTIVE') ? 'tạm dừng' : 'kích hoạt';
    if (confirm('Bạn có muốn ' + act + ' áp dụng cấu hình ma trận phê duyệt ' + code + ' không?')) {
        document.getElementById('statusMatrixId').value = id;
        document.getElementById('statusForm').submit();
    }
}

function confirmDeleteMatrix(id, code, name) {
    document.getElementById('deleteMatrixId').value = id;
    document.getElementById('deleteMatrixName').innerText = code + ' - ' + name;
    var modal = new bootstrap.Modal(document.getElementById('deleteConfirmModal'));
    modal.show();
}
</script>

<jsp:include page="../common/footer.jsp" />

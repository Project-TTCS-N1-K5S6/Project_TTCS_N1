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

        <!-- Bảng danh sách yêu cầu tuyển dụng -->
        <div class="card mb-4 shadow-sm border">
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-custom table-hover align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th style="width: 140px;">Mã yêu cầu</th>
                                <th>Chức danh &amp; Tiêu đề</th>
                                <th>Phòng ban</th>
                                <th class="text-center" style="width: 100px;">SL tuyển</th>
                                <th>Lý do tuyển</th>
                                <th>Dải lương đề xuất</th>
                                <th>Ngày cần người</th>
                                <th class="text-center" style="width: 130px;">Trạng thái</th>
                                <th class="text-end pe-3" style="width: 130px;">Thao tác</th>
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
                                                <div class="fw-bold text-dark">${r.positionTitle}</div>
                                                <small class="text-muted text-truncate d-inline-block" style="max-width: 280px;">${r.title}</small>
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
                                                    </c:when>
                                                    <c:otherwise><span class="text-muted">Chưa đặt</span></c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td class="text-center">
                                                <span class="badge ${r.statusBadgeClass} px-2 py-1">${r.statusLabel}</span>
                                            </td>
                                            <td class="text-end pe-3">
                                                <div class="btn-group btn-group-sm">
                                                    <button type="button" class="btn btn-outline-secondary" 
                                                            onclick="viewDetails('${r.id}', '${r.code}', '${r.positionTitle}', '${r.departmentName}', '${r.headcount}', '${r.recruitmentReasonLabel}', '${r.formattedSalaryRange}', '${r.deadline}', '${r.statusLabel}')"
                                                            title="Xem chi tiết JD & Yêu cầu">
                                                        <i class="bi bi-eye"></i>
                                                    </button>
                                                    <a href="${pageContext.request.contextPath}/recruitment-requests/edit?id=${r.id}" 
                                                       class="btn btn-outline-primary" title="Chỉnh sửa yêu cầu">
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
            </div>
        </div>
    </div>
</main>

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
            <div class="modal-footer border-top">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>
            </div>
        </div>
    </div>
</div>

<jsp:include page="../common/footer.jsp" />

<script>
function viewDetails(id, code, pos, dept, count, reason, sal, deadline, status) {
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
</script>

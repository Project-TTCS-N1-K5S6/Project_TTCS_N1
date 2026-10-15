<%--
  ==============================================================================
  GIAO DIỆN PHÂN BỔ NHÂN SỰ TUYỂN DỤNG (recruitment/assign.jsp)
  Chức năng:
  - Cho phép Trưởng phòng Nhân sự (HR_MANAGER) hoặc Quản trị viên phân công Chuyên viên tuyển dụng (Recruiter) phụ trách yêu cầu tuyển dụng.
  - Hiển thị danh sách Recruiter, số lượng việc đang phụ trách (workload), năng lực tuyển dụng.
  - Xem lịch sử các lần chuyển giao/phân bổ Recruiter (Assignment History).
  ==============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Phân bổ Hồ sơ Tuyển dụng" />
<c:set var="breadcrumb" value="Phân bổ Hồ sơ" />
<c:set var="activeMenu" value="requisitions" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Breadcrumb & Header -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <nav aria-label="breadcrumb" class="mb-1">
                    <ol class="breadcrumb mb-0" style="font-size: 0.8rem;">
                        <li class="breadcrumb-item">
                            <a href="${pageContext.request.contextPath}/recruitment-requests" class="text-decoration-none text-muted">
                                <i class="bi bi-file-earmark-text me-1"></i>Yêu cầu tuyển dụng
                            </a>
                        </li>
                        <li class="breadcrumb-item active text-primary fw-semibold">Phân bổ hồ sơ</li>
                    </ol>
                </nav>
                <h4 class="fw-bold mb-0">
                    <i class="bi bi-person-lines-fill text-primary me-2"></i>Phân bổ Hồ sơ Tuyển dụng
                </h4>
                <p class="text-muted small mb-0 mt-1">
                    Gán recruiter chính và recruiter hỗ trợ cho vị trí tuyển dụng
                </p>
            </div>
            <a href="${pageContext.request.contextPath}/recruitment-requests" class="btn btn-outline-secondary btn-sm">
                <i class="bi bi-arrow-left me-1"></i> Quay lại danh sách
            </a>
        </div>

        <!-- Thông tin vị trí tuyển dụng -->
        <div class="card border shadow-sm mb-4" style="border-left: 4px solid #6366f1 !important;">
            <div class="card-body py-3">
                <div class="row g-3 align-items-center">
                    <div class="col-md-8">
                        <div class="d-flex align-items-center gap-3">
                            <div class="rounded-circle bg-primary bg-opacity-10 d-flex align-items-center justify-content-center"
                                 style="width: 48px; height: 48px; min-width: 48px;">
                                <i class="bi bi-briefcase-fill text-primary fs-5"></i>
                            </div>
                            <div>
                                <div class="d-flex align-items-center gap-2 mb-1">
                                    <span class="badge bg-light text-dark border font-monospace">${reqItem.code}</span>
                                    <span class="badge ${reqItem.statusBadgeClass}" style="font-size: 0.72rem;">${reqItem.statusLabel}</span>
                                </div>
                                <h5 class="fw-bold mb-0 text-dark">${reqItem.positionTitle}</h5>
                                <small class="text-muted">${reqItem.title}</small>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="row g-2 text-center">
                            <div class="col-4">
                                <div class="text-muted" style="font-size: 0.72rem;">Phòng ban</div>
                                <div class="fw-semibold text-dark" style="font-size: 0.82rem;">${not empty reqItem.departmentName ? reqItem.departmentName : '—'}</div>
                            </div>
                            <div class="col-4">
                                <div class="text-muted" style="font-size: 0.72rem;">Số lượng</div>
                                <div class="fw-semibold text-primary" style="font-size: 0.82rem;">${reqItem.headcount} người</div>
                            </div>
                            <div class="col-4">
                                <div class="text-muted" style="font-size: 0.72rem;">Hạn cần</div>
                                <div class="fw-semibold text-dark" style="font-size: 0.82rem;">
                                    <c:choose>
                                        <c:when test="${reqItem.deadline != null}"><fmt:formatDate value="${reqItem.deadline}" pattern="dd/MM/yyyy"/></c:when>
                                        <c:otherwise>—</c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="row g-4">
            <!-- CỘT TRÁI: Form phân bổ -->
            <div class="col-lg-7">
                <div class="card border shadow-sm h-100">
                    <div class="card-header bg-white border-bottom py-3">
                        <h6 class="mb-0 fw-bold">
                            <i class="bi bi-person-gear text-primary me-2"></i>Phân công Recruiter
                        </h6>
                    </div>
                    <div class="card-body">
                        <form id="frmAssign" action="${pageContext.request.contextPath}/recruitment-requests/assign" method="post">
                            <input type="hidden" name="requisitionId" value="${reqItem.id}">

                            <!-- RECRUITER CHÍNH -->
                            <div class="mb-4">
                                <label class="form-label fw-semibold">
                                    <i class="bi bi-star-fill text-warning me-1"></i>
                                    Recruiter Chính <span class="text-danger">*</span>
                                </label>
                                <p class="text-muted small mb-2">
                                    Người chịu trách nhiệm chính về vị trí này. Sẽ được ghi vào lịch sử khi thay đổi.
                                </p>
                                <select name="primaryRecruiterId" id="primaryRecruiterId" class="form-select">
                                    <option value="">-- Chưa phân công recruiter chính --</option>
                                    <c:forEach var="u" items="${availableRecruiters}">
                                        <option value="${u.id}"
                                            ${u.id == reqItem.recruiterId ? 'selected' : ''}>
                                            ${u.fullName}
                                            <c:if test="${not empty u.jobTitle}"> (${u.jobTitle})</c:if>
                                            <c:if test="${not empty u.departmentName}"> — ${u.departmentName}</c:if>
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>

                            <!-- LÝ DO THAY ĐỔI (chỉ hiện khi đã có recruiter chính cũ) -->
                            <c:if test="${not empty reqItem.recruiterId}">
                                <div class="mb-4">
                                    <label class="form-label fw-semibold">
                                        <i class="bi bi-chat-text me-1 text-secondary"></i>
                                        Lý do thay đổi người phụ trách chính
                                    </label>
                                    <textarea name="changeReason" id="changeReason" class="form-control"
                                              rows="2" placeholder="Nhập lý do thay đổi recruiter (để lại trống nếu không đổi)..."></textarea>
                                </div>
                            </c:if>
                            <c:if test="${empty reqItem.recruiterId}">
                                <input type="hidden" name="changeReason" value="Phân công recruiter lần đầu">
                            </c:if>

                            <hr class="my-3">

                            <!-- RECRUITER HỖ TRỢ -->
                            <div class="mb-4">
                                <label class="form-label fw-semibold">
                                    <i class="bi bi-people-fill text-info me-1"></i>
                                    Recruiter Hỗ trợ
                                    <span class="badge bg-info-subtle text-info ms-1" style="font-size: 0.7rem;">Nhiều người</span>
                                </label>
                                <p class="text-muted small mb-2">
                                    Các recruiter phụ hỗ trợ. Họ cũng sẽ thấy ứng viên của vị trí này.
                                </p>

                                <!-- Danh sách recruiter hỗ trợ đã chọn -->
                                <div id="supportList" class="mb-2">
                                    <c:forEach var="a" items="${assignments}">
                                        <c:if test="${a.role == 'SUPPORT'}">
                                            <div class="support-item d-flex align-items-center gap-2 p-2 border rounded mb-2 bg-light"
                                                 data-id="${a.recruiterId}">
                                                <div class="rounded-circle bg-info bg-opacity-10 d-flex align-items-center justify-content-center"
                                                     style="width: 32px; height: 32px; min-width: 32px;">
                                                    <i class="bi bi-person text-info"></i>
                                                </div>
                                                <div class="flex-grow-1">
                                                    <div class="fw-semibold" style="font-size: 0.85rem;">${a.recruiterName}</div>
                                                    <div class="text-muted" style="font-size: 0.75rem;">${a.recruiterJobTitle}</div>
                                                </div>
                                                <input type="hidden" name="supportRecruiterIds" value="${a.recruiterId}">
                                                <button type="button" class="btn btn-sm btn-outline-danger p-1"
                                                        onclick="removeSupportItem(this)" title="Xóa khỏi danh sách">
                                                    <i class="bi bi-x-lg"></i>
                                                </button>
                                            </div>
                                        </c:if>
                                    </c:forEach>
                                </div>

                                <!-- Thêm recruiter hỗ trợ -->
                                <div class="input-group">
                                    <select id="addSupportSelect" class="form-select">
                                        <option value="">-- Chọn recruiter để thêm vào hỗ trợ --</option>
                                        <c:forEach var="u" items="${availableRecruiters}">
                                            <option value="${u.id}" data-name="${u.fullName}"
                                                    data-title="${u.jobTitle}" data-dept="${u.departmentName}">
                                                ${u.fullName}
                                                <c:if test="${not empty u.jobTitle}"> (${u.jobTitle})</c:if>
                                                <c:if test="${not empty u.departmentName}"> — ${u.departmentName}</c:if>
                                            </option>
                                        </c:forEach>
                                    </select>
                                    <button type="button" class="btn btn-outline-info" onclick="addSupportRecruiter()" id="btnAddSupport">
                                        <i class="bi bi-plus-lg me-1"></i>Thêm
                                    </button>
                                </div>
                                <div id="supportListEmpty" class="text-muted small mt-2 ${not empty supportList ? 'd-none' : ''}">
                                    <i class="bi bi-info-circle me-1"></i>Chưa có recruiter hỗ trợ nào.
                                </div>
                            </div>

                            <div class="d-flex gap-2 pt-2">
                                <button type="submit" class="btn btn-primary px-4" id="btnSaveAssign">
                                    <i class="bi bi-check2-circle me-1"></i>Lưu phân bổ
                                </button>
                                <a href="${pageContext.request.contextPath}/recruitment-requests" class="btn btn-outline-secondary">
                                    Hủy
                                </a>
                            </div>
                        </form>
                    </div>
                </div>
            </div>

            <!-- CỘT PHẢI: Trạng thái hiện tại + Lịch sử -->
            <div class="col-lg-5">
                <!-- Trạng thái phân công hiện tại -->
                <div class="card border shadow-sm mb-4">
                    <div class="card-header bg-white border-bottom py-3">
                        <h6 class="mb-0 fw-bold">
                            <i class="bi bi-diagram-3 text-success me-2"></i>Trạng thái Phân công
                        </h6>
                    </div>
                    <div class="card-body p-0">
                        <c:choose>
                            <c:when test="${empty assignments and empty reqItem.recruiterId}">
                                <div class="text-center py-4 text-muted">
                                    <i class="bi bi-person-slash fs-2 d-block mb-2 text-secondary"></i>
                                    <div class="small">Chưa phân công recruiter nào</div>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <ul class="list-group list-group-flush">
                                    <!-- Recruiter chính -->
                                    <c:if test="${not empty reqItem.recruiterId}">
                                        <li class="list-group-item d-flex align-items-center gap-3 py-3">
                                            <div class="rounded-circle bg-warning bg-opacity-15 d-flex align-items-center justify-content-center"
                                                 style="width: 38px; height: 38px; min-width: 38px;">
                                                <i class="bi bi-star-fill text-warning"></i>
                                            </div>
                                            <div class="flex-grow-1">
                                                <div class="fw-bold text-dark" style="font-size: 0.85rem;">${reqItem.recruiterName}</div>
                                                <div class="text-muted" style="font-size: 0.75rem;">Recruiter Chính</div>
                                            </div>
                                            <span class="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle" style="font-size: 0.7rem;">Chính</span>
                                        </li>
                                    </c:if>
                                    <!-- Recruiter hỗ trợ -->
                                    <c:forEach var="a" items="${assignments}">
                                        <c:if test="${a.role == 'SUPPORT'}">
                                            <li class="list-group-item d-flex align-items-center gap-3 py-3">
                                                <div class="rounded-circle bg-info bg-opacity-10 d-flex align-items-center justify-content-center"
                                                     style="width: 38px; height: 38px; min-width: 38px;">
                                                    <i class="bi bi-person-fill text-info"></i>
                                                </div>
                                                <div class="flex-grow-1">
                                                    <div class="fw-semibold text-dark" style="font-size: 0.85rem;">${a.recruiterName}</div>
                                                    <div class="text-muted" style="font-size: 0.75rem;">${a.recruiterJobTitle}</div>
                                                </div>
                                                <span class="badge bg-info-subtle text-info border border-info-subtle" style="font-size: 0.7rem;">Hỗ trợ</span>
                                            </li>
                                        </c:if>
                                    </c:forEach>
                                </ul>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <!-- Lịch sử chuyển giao -->
                <div class="card border shadow-sm">
                    <div class="card-header bg-white border-bottom py-3 d-flex align-items-center justify-content-between">
                        <h6 class="mb-0 fw-bold">
                            <i class="bi bi-clock-history text-secondary me-2"></i>Lịch sử Chuyển giao
                        </h6>
                        <span class="badge bg-secondary-subtle text-secondary">${history.size()} lần</span>
                    </div>
                    <div class="card-body p-0" style="max-height: 400px; overflow-y: auto;">
                        <c:choose>
                            <c:when test="${empty history}">
                                <div class="text-center py-4 text-muted">
                                    <i class="bi bi-clock fs-2 d-block mb-2 text-secondary opacity-50"></i>
                                    <div class="small">Chưa có lịch sử chuyển giao</div>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="timeline-container px-3 py-2">
                                    <c:forEach var="h" items="${history}" varStatus="status">
                                        <div class="timeline-item d-flex gap-3 py-3 ${not status.last ? 'border-bottom' : ''}">
                                            <div class="timeline-dot mt-1">
                                                <div class="rounded-circle bg-primary bg-opacity-10 d-flex align-items-center justify-content-center"
                                                     style="width: 28px; height: 28px; min-width: 28px;">
                                                    <i class="bi bi-arrow-left-right text-primary" style="font-size: 0.65rem;"></i>
                                                </div>
                                            </div>
                                            <div class="flex-grow-1">
                                                <div class="d-flex align-items-center gap-2 mb-1">
                                                    <span class="text-muted small">
                                                        <i class="bi bi-calendar3 me-1"></i>
                                                        <fmt:formatDate value="${h.changedAt}" pattern="dd/MM/yyyy HH:mm"/>
                                                    </span>
                                                </div>
                                                <div class="small mb-1">
                                                    <span class="text-danger fw-semibold">
                                                        <i class="bi bi-dash-circle me-1"></i>${not empty h.oldRecruiterName ? h.oldRecruiterName : '(Chưa có)'}
                                                    </span>
                                                    <i class="bi bi-arrow-right text-muted mx-1"></i>
                                                    <span class="text-success fw-semibold">
                                                        <i class="bi bi-plus-circle me-1"></i>${not empty h.newRecruiterName ? h.newRecruiterName : '(Chưa có)'}
                                                    </span>
                                                </div>
                                                <c:if test="${not empty h.changeReason}">
                                                    <div class="text-muted" style="font-size: 0.75rem;">
                                                        <i class="bi bi-chat-left-quote me-1"></i>
                                                        <em>${h.changeReason}</em>
                                                    </div>
                                                </c:if>
                                                <div class="text-secondary" style="font-size: 0.72rem; margin-top: 2px;">
                                                    <i class="bi bi-person me-1"></i>Bởi: ${not empty h.changedByName ? h.changedByName : h.changedBy}
                                                </div>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>
        </div>
    </div>

<style>
.timeline-container { }
.timeline-item { transition: background-color 0.15s; }
.timeline-item:hover { background-color: #f8fafc; border-radius: 6px; }
</style>

<script>
// Danh sách ID recruiter hỗ trợ hiện đang trong danh sách
let supportIds = new Set();

// Khởi tạo từ các item đã có
document.querySelectorAll('.support-item').forEach(function(item) {
    supportIds.add(item.getAttribute('data-id'));
});

function addSupportRecruiter() {
    const sel = document.getElementById('addSupportSelect');
    const id = sel.value;
    if (!id) return;

    const opt = sel.options[sel.selectedIndex];
    const name = opt.getAttribute('data-name');
    const title = opt.getAttribute('data-title') || '';
    const dept = opt.getAttribute('data-dept') || '';

    // Kiểm tra trùng
    if (supportIds.has(id)) {
        alert('Recruiter này đã có trong danh sách hỗ trợ!');
        return;
    }

    // Kiểm tra trùng với recruiter chính
    const primarySel = document.getElementById('primaryRecruiterId');
    if (primarySel && primarySel.value === id) {
        alert('Recruiter này đã là recruiter chính! Không thể thêm vào hỗ trợ.');
        return;
    }

    // Tạo item HTML
    const div = document.createElement('div');
    div.className = 'support-item d-flex align-items-center gap-2 p-2 border rounded mb-2 bg-light';
    div.setAttribute('data-id', id);
    div.innerHTML = `
        <div class="rounded-circle bg-info bg-opacity-10 d-flex align-items-center justify-content-center"
             style="width: 32px; height: 32px; min-width: 32px;">
            <i class="bi bi-person text-info"></i>
        </div>
        <div class="flex-grow-1">
            <div class="fw-semibold" style="font-size: 0.85rem;">${name}</div>
            <div class="text-muted" style="font-size: 0.75rem;">${title}${dept ? ' — ' + dept : ''}</div>
        </div>
        <input type="hidden" name="supportRecruiterIds" value="${id}">
        <button type="button" class="btn btn-sm btn-outline-danger p-1"
                onclick="removeSupportItem(this)" title="Xóa khỏi danh sách">
            <i class="bi bi-x-lg"></i>
        </button>
    `;

    document.getElementById('supportList').appendChild(div);
    supportIds.add(id);
    sel.value = '';
    updateEmptyMsg();
}

function removeSupportItem(btn) {
    const item = btn.closest('.support-item');
    const id = item.getAttribute('data-id');
    supportIds.delete(id);
    item.remove();
    updateEmptyMsg();
}

function updateEmptyMsg() {
    const empty = document.getElementById('supportListEmpty');
    if (empty) {
        empty.classList.toggle('d-none', supportIds.size > 0);
    }
}

// Xác nhận khi submit form
document.getElementById('frmAssign').addEventListener('submit', function(e) {
    const primarySel = document.getElementById('primaryRecruiterId');
    if (!primarySel.value) {
        if (!confirm('Bạn chưa chọn recruiter chính. Tiếp tục lưu không?')) {
            e.preventDefault();
        }
    }
});

// Khởi tạo trạng thái empty msg
updateEmptyMsg();
</script>

<jsp:include page="../common/footer.jsp" />

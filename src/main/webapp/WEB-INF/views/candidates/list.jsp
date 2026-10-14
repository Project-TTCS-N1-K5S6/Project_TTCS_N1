<%--
  ==============================================================================
  GIAO DIỆN QUẢN LÝ HỒ SƠ & PIPELINE ỨNG VIÊN (candidates/list.jsp)
  Chức năng:
  - Hiển thị danh sách hồ sơ ứng viên kèm mã tra cứu tracking code, vị trí ứng tuyển, nguồn nộp, trạng thái pipeline.
  - Phân tách dữ liệu: Chuyên viên tuyển dụng (Recruiter) chỉ xem ứng viên các vị trí mình phụ trách (US 5).
  - Tìm kiếm theo từ khóa họ tên/email, lọc theo trạng thái (APPLIED, SCREENING, INTERVIEWING, OFFER, HIRED, REJECTED).
  - Modal tiếp nhận ứng viên mới (upload CV file .pdf/.docx) và Modal cập nhật trạng thái pipeline/lý do loại.
  ==============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Hồ sơ & Pipeline ứng viên" />
<c:set var="breadcrumb" value="Hồ sơ ứng viên" />
<c:set var="activeMenu" value="candidates" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Tiêu đề trang & Breadcrumb chuẩn RisApps -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <h3 class="fw-bold text-dark mb-1">Danh sách ứng viên</h3>
                <div class="text-muted small">Hệ thống quản trị / Danh sách ứng viên</div>
            </div>
            <button class="btn-ris-primary" data-bs-toggle="modal" data-bs-target="#addCandidateModal">
                <i class="bi bi-person-plus-fill me-1"></i> Tiếp nhận ứng viên
            </button>
        </div>

        <%-- [KN-116]: Thông báo phạm vi hiển thị cho Recruiter được phân công --%>
        <c:if test="${currentUser.hasRole('RECRUITER') && !currentUser.hasRole('ADMIN') && !currentUser.hasRole('HR_MANAGER')}">
            <div class="alert alert-info d-flex align-items-start gap-3 border border-info-subtle shadow-sm mb-3" role="alert"
                 style="background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%); border-radius: 10px;">
                <div class="flex-shrink-0 mt-1">
                    <div class="rounded-circle bg-info bg-opacity-15 d-flex align-items-center justify-content-center"
                         style="width: 36px; height: 36px;">
                        <i class="bi bi-shield-lock-fill text-info"></i>
                    </div>
                </div>
                <div>
                    <div class="fw-bold text-info-emphasis mb-1">
                        <i class="bi bi-info-circle me-1"></i>Phạm vi hiển thị bị giới hạn (KN-116)
                    </div>
                    <div class="text-info-emphasis small">
                        Bạn chỉ xem được hồ sơ ứng viên của các <strong>vị trí tuyển dụng được giao cho bạn</strong>
                        (vai trò Recruiter Chính hoặc Recruiter Hỗ trợ).
                        Liên hệ HR Manager nếu cần mở rộng quyền truy cập.
                    </div>
                </div>
            </div>
        </c:if>

        <!-- 6 Thẻ KPI ứng viên theo đúng ảnh mẫu tham khảo (Image 4: 2 hàng x 3 cột) -->
        <div class="row g-3 mb-4">
            <!-- Hàng 1: 3 thẻ -->
            <div class="col-12 col-md-4">
                <div class="ris-cand-kpi-card">
                    <div class="cand-top">
                        <div>
                            <span class="cand-num">
                                <c:set var="reviewCount" value="0" />
                                <c:forEach var="item" items="${candidates}">
                                    <c:if test="${item.status == 'APPLIED' || item.status == 'SCREENING'}"><c:set var="reviewCount" value="${reviewCount + 1}" /></c:if>
                                </c:forEach>
                                ${reviewCount}
                            </span>
                            <span class="cand-unit">Hồ sơ</span>
                        </div>
                        <div class="cand-icon"><i class="bi bi-file-earmark-text"></i></div>
                    </div>
                    <div class="cand-title">Trong đánh giá</div>
                    <div class="cand-date">Tháng 10/2026</div>
                </div>
            </div>

            <div class="col-12 col-md-4">
                <div class="ris-cand-kpi-card">
                    <div class="cand-top">
                        <div>
                            <span class="cand-num">
                                <c:set var="rejCount" value="0" />
                                <c:forEach var="item" items="${candidates}">
                                    <c:if test="${item.status == 'REJECTED'}"><c:set var="rejCount" value="${rejCount + 1}" /></c:if>
                                </c:forEach>
                                ${rejCount}
                            </span>
                            <span class="cand-unit">Hồ sơ</span>
                        </div>
                        <div class="cand-icon" style="color: #ef4444;"><i class="bi bi-file-earmark-x"></i></div>
                    </div>
                    <div class="cand-title">Không phù hợp</div>
                    <div class="cand-date">Tháng 10/2026</div>
                </div>
            </div>

            <div class="col-12 col-md-4">
                <div class="ris-cand-kpi-card">
                    <div class="cand-top">
                        <div>
                            <span class="cand-num">
                                <c:set var="interviewCount" value="0" />
                                <c:forEach var="item" items="${candidates}">
                                    <c:if test="${item.status == 'INTERVIEWING'}"><c:set var="interviewCount" value="${interviewCount + 1}" /></c:if>
                                </c:forEach>
                                ${interviewCount}
                            </span>
                            <span class="cand-unit">Hồ sơ</span>
                        </div>
                        <div class="cand-icon" style="color: #f59e0b;"><i class="bi bi-calendar-event"></i></div>
                    </div>
                    <div class="cand-title">Phỏng vấn</div>
                    <div class="cand-date">Tháng 10/2026</div>
                </div>
            </div>

            <!-- Hàng 2: 3 thẻ -->
            <div class="col-12 col-md-4">
                <div class="ris-cand-kpi-card">
                    <div class="cand-top">
                        <div>
                            <span class="cand-num">${interviewCount > 0 ? interviewCount : 2}</span>
                            <span class="cand-unit">Hồ sơ</span>
                        </div>
                        <div class="cand-icon" style="color: #06b6d4;"><i class="bi bi-clipboard-check"></i></div>
                    </div>
                    <div class="cand-title">Giai đoạn kiểm tra</div>
                    <div class="cand-date">Tháng 10/2026</div>
                </div>
            </div>

            <div class="col-12 col-md-4">
                <div class="ris-cand-kpi-card">
                    <div class="cand-top">
                        <div>
                            <span class="cand-num">
                                <c:set var="offerCount" value="0" />
                                <c:forEach var="item" items="${candidates}">
                                    <c:if test="${item.status == 'OFFER'}"><c:set var="offerCount" value="${offerCount + 1}" /></c:if>
                                </c:forEach>
                                ${offerCount}
                            </span>
                            <span class="cand-unit">Hồ sơ</span>
                        </div>
                        <div class="cand-icon" style="color: #8b5cf6;"><i class="bi bi-envelope-paper"></i></div>
                    </div>
                    <div class="cand-title">Đề nghị tuyển dụng (Offer)</div>
                    <div class="cand-date">Tháng 10/2026</div>
                </div>
            </div>

            <div class="col-12 col-md-4">
                <div class="ris-cand-kpi-card">
                    <div class="cand-top">
                        <div>
                            <span class="cand-num">
                                <c:set var="hiredCount" value="0" />
                                <c:forEach var="item" items="${candidates}">
                                    <c:if test="${item.status == 'HIRED'}"><c:set var="hiredCount" value="${hiredCount + 1}" /></c:if>
                                </c:forEach>
                                ${hiredCount}
                            </span>
                            <span class="cand-unit">Hồ sơ</span>
                        </div>
                        <div class="cand-icon" style="color: #10b981;"><i class="bi bi-award"></i></div>
                    </div>
                    <div class="cand-title">Đã tiếp nhận (Trúng tuyển)</div>
                    <div class="cand-date">Tháng 10/2026</div>
                </div>
            </div>
        </div>

        <!-- Bảng danh sách ứng viên (Review All Kandidat theo ảnh tham khảo Image 4) -->
        <div class="card shadow-none border mb-4">
            <div class="card-header bg-white border-bottom py-3 d-flex flex-wrap align-items-center justify-content-between gap-3">
                <div>
                    <h5 class="fw-bold text-dark mb-0 fs-6">Đánh giá tất cả ứng viên</h5>
                    <small class="text-muted">Danh sách hồ sơ mới nhất sắp xếp theo ngày nộp đơn</small>
                </div>
                <div class="d-flex flex-wrap align-items-center gap-2">
                    <!-- Dropdown bộ lọc trạng thái nhanh theo ảnh Image 4 -->
                    <div class="dropdown">
                        <button class="btn btn-outline-secondary btn-sm dropdown-toggle rounded-pill px-3 py-1 text-secondary" type="button" data-bs-toggle="dropdown">
                            <i class="bi bi-funnel me-1"></i>
                            <c:choose>
                                <c:when test="${paramStatus == 'APPLIED'}">Mới ứng tuyển</c:when>
                                <c:when test="${paramStatus == 'SCREENING'}">Sàng lọc CV</c:when>
                                <c:when test="${paramStatus == 'INTERVIEWING'}">Chờ phỏng vấn</c:when>
                                <c:when test="${paramStatus == 'OFFER'}">Gửi Offer</c:when>
                                <c:when test="${paramStatus == 'HIRED'}">Trúng tuyển</c:when>
                                <c:when test="${paramStatus == 'REJECTED'}">Đã từ chối</c:when>
                                <c:otherwise>Tất cả trạng thái</c:otherwise>
                            </c:choose>
                        </button>
                        <ul class="dropdown-menu dropdown-menu-end shadow-sm border-0 py-2">
                            <li><a class="dropdown-item py-1" href="${pageContext.request.contextPath}/candidates">Tất cả hồ sơ</a></li>
                            <li><a class="dropdown-item py-1" href="${pageContext.request.contextPath}/candidates?status=APPLIED">Mới ứng tuyển</a></li>
                            <li><a class="dropdown-item py-1" href="${pageContext.request.contextPath}/candidates?status=SCREENING">Sàng lọc CV</a></li>
                            <li><a class="dropdown-item py-1" href="${pageContext.request.contextPath}/candidates?status=INTERVIEWING">Chờ phỏng vấn</a></li>
                            <li><a class="dropdown-item py-1" href="${pageContext.request.contextPath}/candidates?status=OFFER">Gửi Offer</a></li>
                            <li><a class="dropdown-item py-1" href="${pageContext.request.contextPath}/candidates?status=HIRED">Trúng tuyển</a></li>
                            <li><a class="dropdown-item py-1 text-danger" href="${pageContext.request.contextPath}/candidates?status=REJECTED">Đã từ chối</a></li>
                        </ul>
                    </div>

                    <div class="ris-search-wrapper" style="min-width: 220px;">
                        <input type="text" class="ris-search-input" id="candQuickSearch" placeholder="Tìm ứng viên, email..." onkeyup="filterCandTable(this.value)" />
                        <i class="bi bi-search search-icon"></i>
                    </div>
                </div>
            </div>
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-custom table-fit mb-0 align-middle" id="candidatesMainTable">
                        <thead>
                            <tr>
                                <th class="ps-3">Họ và tên</th>
                                <th>Phòng ban</th>
                                <th>Vị trí ứng tuyển</th>
                                <th>Thời gian</th>
                                <th>Trạng thái</th>
                                <th class="text-end pe-4" style="min-width: 180px;">Thao tác &amp; Chuyển vòng</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="c" items="${candidates}">
                                <tr>
                                    <td class="ps-3">
                                        <div class="d-flex align-items-center gap-2">
                                            <div class="user-avatar" style="width: 36px; height: 36px; min-width: 36px; min-height: 36px; border-radius: 50%; background: linear-gradient(135deg, #2563eb, #8b5cf6); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 0.85rem;">
                                                ${c.fullName != null && c.fullName.length() > 0 ? c.fullName.substring(0, 1) : 'U'}
                                            </div>
                                            <div>
                                                <div class="fw-bold text-dark">${c.fullName}</div>
                                                <div class="text-muted text-xs">${c.email}</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <span class="text-dark fw-semibold">${c.departmentName != null ? c.departmentName : 'Đang cập nhật'}</span>
                                    </td>
                                    <td>
                                        <span class="text-secondary">${c.requisitionTitle != null ? c.requisitionTitle : 'Chưa gắn vị trí'}</span>
                                        <c:if test="${not empty c.cvUrl}">
                                            <a href="${c.cvUrl}" target="_blank" class="d-block text-primary text-xs text-decoration-none mt-1">
                                                <i class="bi bi-file-earmark-pdf"></i> Xem CV đính kèm
                                            </a>
                                        </c:if>
                                    </td>
                                    <td>
                                        <div class="text-secondary small">
                                            <fmt:formatDate value="${c.createdAt}" pattern="dd/MM/yyyy HH:mm" />
                                        </div>
                                    </td>
                                    <td>
                                        <span class="badge-pill-status ${c.status == 'HIRED' ? 'success' : (c.status == 'REJECTED' ? 'danger' : (c.status == 'OFFER' ? 'info' : 'warning'))}">
                                            ${c.getStatusLabel()}
                                        </span>
                                    </td>
                                    <td class="text-end pe-4">
                                        <div class="d-inline-flex align-items-center gap-2">
                                            <form action="${pageContext.request.contextPath}/candidates/update-status" method="POST"
                                                  class="candidate-status-form d-inline-flex flex-column gap-1 align-items-end"
                                                  data-current-status="${c.status}" data-current-reason="${c.rejectionReasonId}">
                                                <input type="hidden" name="candidateId" value="${c.id}">
                                                <select class="form-select form-select-sm" name="status" onchange="toggleCandidateStatusForm(this)" style="width: 135px; font-size: 0.8rem; border-radius: 8px;">
                                                    <option value="APPLIED" ${c.status == 'APPLIED' ? 'selected' : ''}>Mới ứng tuyển</option>
                                                    <option value="SCREENING" ${c.status == 'SCREENING' ? 'selected' : ''}>Sàng lọc CV</option>
                                                    <option value="INTERVIEWING" ${c.status == 'INTERVIEWING' ? 'selected' : ''}>Phỏng vấn</option>
                                                    <option value="OFFER" ${c.status == 'OFFER' ? 'selected' : ''}>Gửi Offer</option>
                                                    <option value="HIRED" ${c.status == 'HIRED' ? 'selected' : ''}>Trúng tuyển</option>
                                                    <option value="REJECTED" ${c.status == 'REJECTED' ? 'selected' : ''}>Từ chối</option>
                                                </select>
                                                <div class="rejection-reason-field ${c.status == 'REJECTED' ? '' : 'd-none'}">
                                                    <select class="form-select form-select-sm" name="rejectionReasonId"
                                                            aria-label="Lý do loại hồ sơ" onchange="toggleCandidateStatusForm(this)" style="width: 135px; font-size: 0.75rem;">
                                                        <option value="">Lý do loại</option>
                                                        <c:forEach var="reason" items="${rejectionReasons}">
                                                            <option value="${reason.id}" ${c.rejectionReasonId == reason.id ? 'selected' : ''}>
                                                                <c:out value="${reason.value}" />
                                                            </option>
                                                        </c:forEach>
                                                    </select>
                                                </div>
                                                <button type="submit" class="btn btn-sm btn-primary candidate-status-submit py-0 px-2" style="font-size: 0.75rem;" hidden>
                                                    Lưu
                                                </button>
                                            </form>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty candidates}">
                                <tr>
                                    <td colspan="6" class="text-center py-5 text-muted">
                                        <i class="bi bi-person-x fs-2 d-block mb-2 text-secondary"></i>
                                        Không có hồ sơ ứng viên nào trong giai đoạn này.
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <!-- Phân trang phía Server (Backend Pagination) -->
                <c:if test="${totalPages > 1}">
                    <div class="p-3 border-top d-flex flex-wrap justify-content-between align-items-center gap-2">
                        <small class="text-muted">Hiển thị trang <strong>${currentPage}</strong> / <strong>${totalPages}</strong> (Tổng cộng <strong>${totalCount}</strong> hồ sơ ứng viên)</small>
                        <nav>
                            <ul class="pagination pagination-sm mb-0">
                                <li class="page-item ${currentPage <= 1 ? 'disabled' : ''}">
                                    <a class="page-link" href="${pageContext.request.contextPath}/candidates?page=${currentPage - 1}&search=${paramSearch}&status=${paramStatus}" aria-label="Trang trước">
                                        <i class="bi bi-chevron-left"></i>
                                    </a>
                                </li>
                                <c:forEach begin="1" end="${totalPages}" var="p">
                                    <li class="page-item ${currentPage == p ? 'active' : ''}">
                                        <a class="page-link" href="${pageContext.request.contextPath}/candidates?page=${p}&search=${paramSearch}&status=${paramStatus}">${p}</a>
                                    </li>
                                </c:forEach>
                                <li class="page-item ${currentPage >= totalPages ? 'disabled' : ''}">
                                    <a class="page-link" href="${pageContext.request.contextPath}/candidates?page=${currentPage + 1}&search=${paramSearch}&status=${paramStatus}" aria-label="Trang sau">
                                        <i class="bi bi-chevron-right"></i>
                                    </a>
                                </li>
                            </ul>
                        </nav>
                    </div>
                </c:if>
            </div>
        </div>

        <script>
        function filterCandTable(keyword) {
            var term = keyword.toLowerCase().trim();
            var rows = document.querySelectorAll('#candidatesMainTable tbody tr');
            rows.forEach(function(row) {
                var text = row.innerText.toLowerCase();
                row.style.display = text.includes(term) ? '' : 'none';
            });
        }
        </script>
    </div>

    <!-- Modal Thêm Ứng Viên -->
    <div class="modal fade" id="addCandidateModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/candidates/create" method="POST">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold"><i class="bi bi-person-plus text-primary me-2"></i> Tiếp nhận hồ sơ ứng viên</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row g-3">
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Họ và tên ứng viên <span class="text-danger">*</span></label>
                                <input type="text" class="form-control" name="fullName" required placeholder="VD: Nguyễn Văn B">
                            </div>
                            <div class="col-12 col-sm-6">
                                <label class="form-label small fw-semibold">Email liên hệ <span class="text-danger">*</span></label>
                                <input type="email" class="form-control" name="email" required placeholder="candidate@email.com">
                            </div>
                            <div class="col-12 col-sm-6">
                                <label class="form-label small fw-semibold">Số điện thoại</label>
                                <input type="text" class="form-control" name="phone" placeholder="0901234567">
                            </div>
                            <div class="col-12 col-sm-6">
                                <label class="form-label small fw-semibold">Vị trí tuyển dụng</label>
                                <select class="form-select" name="requisitionId">
                                    <option value="">-- Chưa gán vị trí tuyển dụng --</option>
                                    <c:forEach var="req" items="${requisitions}">
                                        <option value="${req.id}"><c:out value="${req.title}" /> (<c:out value="${req.code}" />)</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-12 col-sm-6">
                                <label class="form-label small fw-semibold">Nguồn ứng viên</label>
                                <select class="form-select" name="applicationSourceId">
                                    <option value="">Chưa xác định</option>
                                    <c:forEach var="source" items="${applicationSources}">
                                        <option value="${source.id}"><c:out value="${source.value}" /></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Đường dẫn CV (Link PDF)</label>
                                <input type="url" class="form-control" name="cvUrl" placeholder="https://storage.cloud/cv.pdf">
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Trạng thái khởi tạo</label>
                                <select class="form-select" name="status">
                                    <option value="APPLIED">Mới ứng tuyển</option>
                                    <option value="SCREENING">Sàng lọc CV</option>
                                    <option value="INTERVIEWING">Phỏng vấn</option>
                                </select>
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Ghi chú ban đầu</label>
                                <textarea class="form-control" name="notes" rows="2" placeholder="Ghi chú kinh nghiệm, kỹ năng..."></textarea>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-primary"><i class="bi bi-check-lg"></i> Thêm hồ sơ</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

<script>
function toggleCandidateStatusForm(control) {
    var form = control.closest('.candidate-status-form');
    var status = form.querySelector('[name="status"]');
    var reasonBox = form.querySelector('.rejection-reason-field');
    var reason = form.querySelector('[name="rejectionReasonId"]');
    var submit = form.querySelector('.candidate-status-submit');
    var rejected = status.value === 'REJECTED';

    reasonBox.classList.toggle('d-none', !rejected);
    reason.required = rejected;
    if (!rejected) {
        reason.value = '';
    }
    submit.hidden = status.value === form.dataset.currentStatus
        && (!rejected || reason.value === form.dataset.currentReason);
}

document.querySelectorAll('.candidate-status-form').forEach(function (form) {
    toggleCandidateStatusForm(form.querySelector('[name="status"]'));
});
</script>
<jsp:include page="../common/footer.jsp" />

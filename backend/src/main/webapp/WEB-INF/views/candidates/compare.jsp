<%--
  ==============================================================================
  GIAO DIỆN ĐỐI CHIẾU & HỢP NHẤT HỒ SƠ ỨNG VIÊN (candidates/compare.jsp)
  Chức năng:
  - Hiển thị so sánh song song chi tiết 2 hồ sơ ứng viên (Side-by-side comparison).
  - Tự động phát hiện và highlight các trường khác biệt.
  - Cho phép người dùng chọn từng trường dữ liệu cần giữ lại (Field-level selection).
  - Cho phép hoán đổi vai trò hồ sơ chính (Surviving) và hồ sơ phụ (Merged).
  - Tùy chọn gộp toàn bộ ghi chú và thẻ nhãn để không thất thoát thông tin.
  - Hiển thị và bảo toàn 100% lịch sử ứng tuyển, phỏng vấn, đánh giá, ghi chú.
  - Modal xác nhận an toàn trước khi thực thi giao dịch hợp nhất.
  ==============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Đối chiếu & Hợp nhất hồ sơ ứng viên" />
<c:set var="breadcrumb" value="Đối chiếu hợp nhất" />
<c:set var="activeMenu" value="candidate-duplicates" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Tiêu đề & Điều hướng -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <nav aria-label="breadcrumb">
                    <ol class="breadcrumb mb-1 small">
                        <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/candidates">Ứng viên</a></li>
                        <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/candidates/duplicates">Cảnh báo trùng lặp</a></li>
                        <li class="breadcrumb-item active" aria-current="page">Đối chiếu &amp; Hợp nhất</li>
                    </ol>
                </nav>
                <h3 class="fw-bold text-dark mb-0">
                    <i class="bi bi-intersect text-primary me-2"></i>Đối chiếu &amp; Hợp nhất hồ sơ ứng viên
                </h3>
            </div>
            <div class="d-flex gap-2">
                <a href="${pageContext.request.contextPath}/candidates/duplicates" class="btn btn-outline-secondary">
                    <i class="bi bi-arrow-left me-1"></i> Quay lại danh sách cảnh báo
                </a>
            </div>
        </div>

        <!-- Banner cảnh báo phát hiện trùng -->
        <div class="alert alert-warning border border-warning-subtle shadow-sm mb-4 rounded-3 d-flex align-items-start gap-3"
             style="background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);">
            <div class="p-2 rounded-circle bg-warning bg-opacity-20 text-warning-emphasis fs-4 mt-1">
                <i class="bi bi-exclamation-triangle-fill"></i>
            </div>
            <div class="flex-grow-1">
                <div class="fw-bold fs-6 text-warning-emphasis mb-1">
                    Phát hiện hồ sơ nghi trùng lặp: <span class="badge ${comparison.warning != null ? comparison.warning.matchTypeBadgeClass : 'bg-danger'}">${comparison.warning != null ? comparison.warning.matchTypeLabel : 'Trùng thông tin'}</span>
                </div>
                <div class="text-secondary small mb-2">
                    Hệ thống nhận diện ứng viên <strong><c:out value="${comparison.candidate1.fullName}" /></strong> và 
                    <strong><c:out value="${comparison.candidate2.fullName}" /></strong> có cùng 
                    <strong><c:out value="${comparison.warning != null ? comparison.warning.matchValue : comparison.candidate1.email}" /></strong>.
                    Vui lòng chọn từng thông tin bạn muốn giữ lại cho hồ sơ sau hợp nhất.
                </div>
                <div class="d-flex align-items-center gap-2 small text-dark fw-semibold">
                    <i class="bi bi-shield-check text-success"></i>
                    Bảo toàn toàn bộ lịch sử: Các lần ứng tuyển, phỏng vấn, đánh giá và ghi chú của cả 2 hồ sơ sẽ được tự động tích hợp đầy đủ.
                </div>
                <div class="d-flex align-items-center gap-2 small text-dark fw-semibold mt-1">
                    <i class="bi bi-arrow-counterclockwise text-primary"></i>
                    Bảo đảm an toàn: Nếu gộp nhầm, bạn hoàn toàn có thể hoàn tác trong vòng <strong>24 giờ</strong> để khôi phục nguyên trạng cả hai hồ sơ.
                </div>
            </div>
        </div>

        <!-- FORM HỢP NHẤT HỒ SƠ -->
        <form action="${pageContext.request.contextPath}/candidates/duplicates/merge" method="POST" id="mergeCandidateForm">
            <input type="hidden" name="warningId" value="${warningId}" />
            <input type="hidden" name="primaryId" id="inputPrimaryId" value="${comparison.candidate1.id}" />
            <input type="hidden" name="secondaryId" id="inputSecondaryId" value="${comparison.candidate2.id}" />

            <!-- THANH ĐIỀU KHIỂN HOÁN ĐỔI VAI TRÒ HỒ SƠ CHÍNH / PHỤ -->
            <div class="card shadow-sm border-0 mb-4 bg-light">
                <div class="card-body p-3 d-flex flex-wrap align-items-center justify-content-between gap-3">
                    <div class="d-flex align-items-center gap-2">
                        <span class="badge bg-primary px-3 py-2 fs-6" id="labelPrimaryBadge">
                            <i class="bi bi-check-circle-fill me-1"></i>Hồ sơ được giữ lại (Chính): <span id="labelPrimaryName"><c:out value="${comparison.candidate1.fullName}" /></span>
                        </span>
                        <i class="bi bi-arrow-left-right text-muted fs-5"></i>
                        <span class="badge bg-secondary px-3 py-2 fs-6" id="labelSecondaryBadge">
                            <i class="bi bi-archive-fill me-1"></i>Hồ sơ bị hợp nhất (Phụ): <span id="labelSecondaryName"><c:out value="${comparison.candidate2.fullName}" /></span>
                        </span>
                    </div>
                    <div>
                        <button type="button" class="btn btn-outline-primary btn-sm rounded-pill px-3" onclick="swapCandidateRoles()">
                            <i class="bi bi-arrow-repeat me-1"></i> Đổi vai trò Hồ sơ chính / Hồ sơ phụ
                        </button>
                    </div>
                </div>
            </div>

            <!-- BẢNG ĐỐI CHIẾU SO SÁNH TRỰC QUAN -->
            <div class="card shadow-sm border mb-4" style="border-radius: 12px; overflow: hidden;">
                <div class="card-header bg-white border-bottom py-3">
                    <h5 class="fw-bold mb-0 text-dark fs-6">
                        <i class="bi bi-sliders me-2 text-primary"></i>Chọn dữ liệu cần giữ lại cho từng trường thông tin
                    </h5>
                    <small class="text-muted">Chọn nút tương ứng với thông tin bạn muốn giữ lại làm thông tin đại diện</small>
                </div>

                <div class="card-body p-0">
                    <table class="table table-bordered align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th style="width: 20%;" class="ps-3">Trường thông tin</th>
                                <th style="width: 40%;" class="bg-primary bg-opacity-10 text-primary fw-bold" id="thHeaderA">
                                    <div class="d-flex align-items-center justify-content-between">
                                        <span><i class="bi bi-person-badge me-1"></i>Hồ sơ A (ID: ${comparison.candidate1.id})</span>
                                        <span class="badge bg-primary" id="badgeColA">Hồ sơ chính</span>
                                    </div>
                                </th>
                                <th style="width: 40%;" class="bg-secondary bg-opacity-10 text-dark fw-bold" id="thHeaderB">
                                    <div class="d-flex align-items-center justify-content-between">
                                        <span><i class="bi bi-person-badge me-1"></i>Hồ sơ B (ID: ${comparison.candidate2.id})</span>
                                        <span class="badge bg-secondary" id="badgeColB">Hồ sơ phụ</span>
                                    </div>
                                </th>
                            </tr>
                        </thead>
                        <tbody>
                            <!-- 1. Họ và tên -->
                            <tr class="${comparison.nameEqual ? '' : 'table-warning'}">
                                <td class="ps-3 fw-semibold">
                                    Họ và tên ứng viên
                                    <c:if test="${!comparison.nameEqual}"><span class="badge bg-warning text-dark ms-1">Khác biệt</span></c:if>
                                </td>
                                <td>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="fullNameChoice" id="fnA" value="primary" checked>
                                        <label class="form-check-label fw-bold text-dark" for="fnA">
                                            <c:out value="${comparison.candidate1.fullName}" />
                                        </label>
                                    </div>
                                </td>
                                <td>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="fullNameChoice" id="fnB" value="secondary">
                                        <label class="form-check-label fw-bold text-dark" for="fnB">
                                            <c:out value="${comparison.candidate2.fullName}" />
                                        </label>
                                    </div>
                                </td>
                            </tr>

                            <!-- 2. Email -->
                            <tr class="${comparison.emailEqual ? '' : 'table-warning'}">
                                <td class="ps-3 fw-semibold">
                                    Địa chỉ Email
                                    <c:if test="${!comparison.emailEqual}"><span class="badge bg-warning text-dark ms-1">Khác biệt</span></c:if>
                                </td>
                                <td>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="emailChoice" id="emA" value="primary" checked>
                                        <label class="form-check-label font-monospace text-dark" for="emA">
                                            <c:out value="${comparison.candidate1.email}" />
                                        </label>
                                    </div>
                                </td>
                                <td>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="emailChoice" id="emB" value="secondary">
                                        <label class="form-check-label font-monospace text-dark" for="emB">
                                            <c:out value="${comparison.candidate2.email}" />
                                        </label>
                                    </div>
                                </td>
                            </tr>

                            <!-- 3. Số điện thoại -->
                            <tr class="${comparison.phoneEqual ? '' : 'table-warning'}">
                                <td class="ps-3 fw-semibold">
                                    Số điện thoại
                                    <c:if test="${!comparison.phoneEqual}"><span class="badge bg-warning text-dark ms-1">Khác biệt</span></c:if>
                                </td>
                                <td>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="phoneChoice" id="phA" value="primary" checked>
                                        <label class="form-check-label font-monospace text-dark" for="phA">
                                            <c:out value="${not empty comparison.candidate1.phone ? comparison.candidate1.phone : '(Trống)'}" />
                                        </label>
                                    </div>
                                </td>
                                <td>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="phoneChoice" id="phB" value="secondary">
                                        <label class="form-check-label font-monospace text-dark" for="phB">
                                            <c:out value="${not empty comparison.candidate2.phone ? comparison.candidate2.phone : '(Trống)'}" />
                                        </label>
                                    </div>
                                </td>
                            </tr>

                            <!-- 4. Vị trí ứng tuyển & Nguồn -->
                            <tr>
                                <td class="ps-3 fw-semibold">
                                    Vị trí ứng tuyển chính
                                    <div class="text-muted text-xs font-normal">Cả 2 vị trí đều được lưu trong lịch sử ứng tuyển</div>
                                </td>
                                <td>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="requisitionChoice" id="rqA" value="primary" checked>
                                        <label class="form-check-label" for="rqA">
                                            <div class="fw-semibold text-dark"><c:out value="${not empty comparison.candidate1.requisitionTitle ? comparison.candidate1.requisitionTitle : 'Chưa gán vị trí'}" /></div>
                                            <div class="text-muted small">Nguồn: <c:out value="${not empty comparison.candidate1.applicationSourceName ? comparison.candidate1.applicationSourceName : 'Chưa xác định'}" /></div>
                                        </label>
                                    </div>
                                </td>
                                <td>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="requisitionChoice" id="rqB" value="secondary">
                                        <label class="form-check-label" for="rqB">
                                            <div class="fw-semibold text-dark"><c:out value="${not empty comparison.candidate2.requisitionTitle ? comparison.candidate2.requisitionTitle : 'Chưa gán vị trí'}" /></div>
                                            <div class="text-muted small">Nguồn: <c:out value="${not empty comparison.candidate2.applicationSourceName ? comparison.candidate2.applicationSourceName : 'Chưa xác định'}" /></div>
                                        </label>
                                    </div>
                                </td>
                            </tr>

                            <!-- 5. Trạng thái vòng tuyển dụng -->
                            <tr class="${comparison.statusEqual ? '' : 'table-warning'}">
                                <td class="ps-3 fw-semibold">
                                    Trạng thái pipeline
                                    <c:if test="${!comparison.statusEqual}"><span class="badge bg-warning text-dark ms-1">Khác biệt</span></c:if>
                                </td>
                                <td>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="statusChoice" id="stA" value="primary" checked>
                                        <label class="form-check-label" for="stA">
                                            <span class="badge ${comparison.candidate1.statusBadgeClass} fs-7">
                                                ${comparison.candidate1.statusLabel}
                                            </span>
                                        </label>
                                    </div>
                                </td>
                                <td>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="statusChoice" id="stB" value="secondary">
                                        <label class="form-check-label" for="stB">
                                            <span class="badge ${comparison.candidate2.statusBadgeClass} fs-7">
                                                ${comparison.candidate2.statusLabel}
                                            </span>
                                        </label>
                                    </div>
                                </td>
                            </tr>

                            <!-- 6. Tệp CV đính kèm -->
                            <tr>
                                <td class="ps-3 fw-semibold">
                                    Tệp CV đính kèm
                                </td>
                                <td>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="cvChoice" id="cvA" value="primary" checked>
                                        <label class="form-check-label" for="cvA">
                                            <c:choose>
                                                <c:when test="${not empty comparison.candidate1.cvUrl}">
                                                    <a href="${comparison.candidate1.cvUrl}" target="_blank" class="text-primary text-decoration-none">
                                                        <i class="bi bi-file-earmark-pdf text-danger me-1"></i>Xem CV Hồ sơ A
                                                    </a>
                                                </c:when>
                                                <c:otherwise><span class="text-muted small">(Không có tệp CV)</span></c:otherwise>
                                            </c:choose>
                                        </label>
                                    </div>
                                </td>
                                <td>
                                    <div class="form-check">
                                        <input class="form-check-input" type="radio" name="cvChoice" id="cvB" value="secondary">
                                        <label class="form-check-label" for="cvB">
                                            <c:choose>
                                                <c:when test="${not empty comparison.candidate2.cvUrl}">
                                                    <a href="${comparison.candidate2.cvUrl}" target="_blank" class="text-primary text-decoration-none">
                                                        <i class="bi bi-file-earmark-pdf text-danger me-1"></i>Xem CV Hồ sơ B
                                                    </a>
                                                </c:when>
                                                <c:otherwise><span class="text-muted small">(Không có tệp CV)</span></c:otherwise>
                                            </c:choose>
                                        </label>
                                    </div>
                                </td>
                            </tr>

                            <!-- 7. Ghi chú nội bộ -->
                            <tr>
                                <td class="ps-3 fw-semibold">
                                    Ghi chú nội bộ
                                </td>
                                <td colspan="2">
                                    <div class="d-flex flex-column gap-2 p-2 bg-light rounded border">
                                        <div class="form-check">
                                            <input class="form-check-input" type="radio" name="notesChoice" id="ntMerge" value="merge" checked>
                                            <label class="form-check-label fw-bold text-success" for="ntMerge">
                                                <i class="bi bi-plus-circle-fill me-1"></i>Gộp cả hai ghi chú (Khuyến nghị - Lưu toàn bộ nhận xét của cả 2 hồ sơ)
                                            </label>
                                        </div>
                                        <div class="d-flex gap-4 ms-4">
                                            <div class="form-check">
                                                <input class="form-check-input" type="radio" name="notesChoice" id="ntA" value="primary">
                                                <label class="form-check-label text-muted small" for="ntA">
                                                    Chỉ giữ ghi chú Hồ sơ A (<c:out value="${not empty comparison.candidate1.notes ? comparison.candidate1.notes : 'Trống'}" />)
                                                </label>
                                            </div>
                                            <div class="form-check">
                                                <input class="form-check-input" type="radio" name="notesChoice" id="ntB" value="secondary">
                                                <label class="form-check-label text-muted small" for="ntB">
                                                    Chỉ giữ ghi chú Hồ sơ B (<c:out value="${not empty comparison.candidate2.notes ? comparison.candidate2.notes : 'Trống'}" />)
                                                </label>
                                            </div>
                                        </div>
                                    </div>
                                </td>
                            </tr>

                            <!-- 8. Thẻ nhãn phân loại -->
                            <tr>
                                <td class="ps-3 fw-semibold">
                                    Thẻ nhãn (Tags)
                                </td>
                                <td colspan="2">
                                    <div class="text-muted small mb-1">Hệ thống sẽ tự động tổng hợp toàn bộ nhãn không trùng từ cả hai hồ sơ:</div>
                                    <div class="d-flex flex-wrap gap-1">
                                        <c:forEach var="t" items="${comparison.candidate1.tagList}">
                                            <span class="badge bg-primary bg-opacity-10 text-primary border border-primary-subtle">${t}</span>
                                        </c:forEach>
                                        <c:forEach var="t" items="${comparison.candidate2.tagList}">
                                            <span class="badge bg-warning bg-opacity-10 text-warning-emphasis border border-warning-subtle">${t}</span>
                                        </c:forEach>
                                        <c:if test="${empty comparison.candidate1.tagList && empty comparison.candidate2.tagList}">
                                            <span class="text-muted small">(Chưa có thẻ nhãn nào)</span>
                                        </c:if>
                                    </div>
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <!-- TIMELINE LỊCH SỬ TUYỂN DỤNG, PHỎNG VẤN & ĐÁNH GIÁ ĐƯỢC BẢO TOÀN -->
            <div class="row g-4 mb-4">
                <div class="col-12 col-md-6">
                    <div class="card shadow-sm border h-100" style="border-radius: 12px;">
                        <div class="card-header bg-white border-bottom py-2 d-flex align-items-center justify-content-between">
                            <span class="fw-bold small text-primary"><i class="bi bi-clock-history me-1"></i>Lịch sử & Hoạt động Hồ sơ A</span>
                            <span class="badge bg-light text-secondary border">${comparison.activities1.size()} hoạt động</span>
                        </div>
                        <div class="card-body p-3" style="max-height: 250px; overflow-y: auto;">
                            <c:forEach var="act" items="${comparison.activities1}">
                                <div class="d-flex gap-2 mb-2 pb-2 border-bottom">
                                    <div class="rounded-circle ${act.typeBadgeClass} d-flex align-items-center justify-content-center flex-shrink-0"
                                         style="width: 26px; height: 26px; font-size: 0.75rem;">
                                        <i class="bi ${act.typeIcon}"></i>
                                    </div>
                                    <div class="flex-grow-1">
                                        <div class="d-flex justify-content-between small">
                                            <span class="fw-bold text-dark">${act.title}</span>
                                            <span class="text-muted text-xs"><fmt:formatDate value="${act.activityDate}" pattern="dd/MM/yyyy HH:mm" /></span>
                                        </div>
                                        <div class="text-muted text-xs">${act.description}</div>
                                    </div>
                                </div>
                            </c:forEach>
                            <c:if test="${empty comparison.activities1}">
                                <div class="text-center text-muted py-3 small">Chưa có lịch sử hoạt động ghi nhận</div>
                            </c:if>
                        </div>
                    </div>
                </div>

                <div class="col-12 col-md-6">
                    <div class="card shadow-sm border h-100" style="border-radius: 12px;">
                        <div class="card-header bg-white border-bottom py-2 d-flex align-items-center justify-content-between">
                            <span class="fw-bold small text-warning-emphasis"><i class="bi bi-clock-history me-1"></i>Lịch sử & Hoạt động Hồ sơ B (Sẽ chuyển sang A)</span>
                            <span class="badge bg-light text-secondary border">${comparison.activities2.size()} hoạt động</span>
                        </div>
                        <div class="card-body p-3" style="max-height: 250px; overflow-y: auto;">
                            <c:forEach var="act" items="${comparison.activities2}">
                                <div class="d-flex gap-2 mb-2 pb-2 border-bottom">
                                    <div class="rounded-circle ${act.typeBadgeClass} d-flex align-items-center justify-content-center flex-shrink-0"
                                         style="width: 26px; height: 26px; font-size: 0.75rem;">
                                        <i class="bi ${act.typeIcon}"></i>
                                    </div>
                                    <div class="flex-grow-1">
                                        <div class="d-flex justify-content-between small">
                                            <span class="fw-bold text-dark">${act.title}</span>
                                            <span class="text-muted text-xs"><fmt:formatDate value="${act.activityDate}" pattern="dd/MM/yyyy HH:mm" /></span>
                                        </div>
                                        <div class="text-muted text-xs">${act.description}</div>
                                    </div>
                                </div>
                            </c:forEach>
                            <c:if test="${empty comparison.activities2}">
                                <div class="text-center text-muted py-3 small">Chưa có lịch sử hoạt động ghi nhận</div>
                            </c:if>
                        </div>
                    </div>
                </div>
            </div>

            <!-- GHI CHÚ LÝ DO HỢP NHẤT VÀ NÚT XÁC NHẬN -->
            <div class="card shadow-sm border mb-4" style="border-radius: 12px;">
                <div class="card-body p-4">
                    <div class="mb-3">
                        <label class="form-label fw-bold text-dark">Lý do / Ghi chú hợp nhất hồ sơ (Tùy chọn):</label>
                        <textarea class="form-control" name="mergeNotes" rows="2"
                                  placeholder="Ví dụ: Ứng viên nộp lại vị trí mới bằng số điện thoại khác nhưng cùng người..."></textarea>
                    </div>

                    <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 pt-3 border-top">
                        <div class="text-muted small">
                            <i class="bi bi-info-circle me-1"></i> Thao tác này sẽ cập nhật hồ sơ chính và đánh dấu hồ sơ phụ là đã gộp.
                        </div>
                        <div class="d-flex gap-2">
                            <a href="${pageContext.request.contextPath}/candidates/duplicates" class="btn btn-outline-secondary px-3">
                                Hủy bỏ
                            </a>
                            <button type="button" class="btn btn-primary px-4 fw-bold shadow-sm" onclick="openConfirmMergeModal()">
                                <i class="bi bi-intersect me-1"></i> Xác nhận &amp; Tiến hành hợp nhất
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </form>
    </div>
</main>

<!-- MODAL XÁC NHẬN HỢP NHẤT AN TOÀN -->
<div class="modal fade" id="confirmMergeModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header bg-primary text-white py-3">
                <h5 class="modal-title fw-bold"><i class="bi bi-exclamation-octagon-fill me-2"></i>Xác nhận hợp nhất hồ sơ ứng viên</h5>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body p-4">
                <div class="alert alert-warning mb-3 small">
                    <strong>Lưu ý quan trọng:</strong> Hành động hợp nhất này sẽ:
                    <ul class="mb-0 ps-3 mt-1">
                        <li>Cập nhật các trường dữ liệu đại diện cho hồ sơ chính.</li>
                        <li>Chuyển toàn bộ lịch sử ứng tuyển, phỏng vấn, đánh giá sang hồ sơ chính.</li>
                        <li>Đánh dấu hồ sơ phụ thành <code>ĐÃ HỢP NHẤT</code> và liên kết tới hồ sơ chính.</li>
                        <li class="text-success fw-bold">Có thể hoàn tác (Undo) lại trong vòng 24 giờ nếu phát hiện gộp nhầm.</li>
                    </ul>
                </div>
                <div class="p-3 bg-light rounded border small">
                    <div><strong>Hồ sơ giữ lại (Chính):</strong> <span id="modalPrimaryNameText" class="text-primary fw-bold"></span></div>
                    <div class="mt-1"><strong>Hồ sơ gộp vào (Phụ):</strong> <span id="modalSecondaryNameText" class="text-secondary fw-bold"></span></div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy bỏ</button>
                <button type="button" class="btn btn-primary fw-bold" onclick="submitMergeForm()">
                    <i class="bi bi-check-lg me-1"></i> Đồng ý hợp nhất hồ sơ
                </button>
            </div>
        </div>
    </div>
</div>

<script>
var candidateA = {
    id: "${comparison.candidate1.id}",
    name: "${comparison.candidate1.fullName}"
};
var candidateB = {
    id: "${comparison.candidate2.id}",
    name: "${comparison.candidate2.fullName}"
};

var isASurviving = true;

function swapCandidateRoles() {
    isASurviving = !isASurviving;
    var pId = isASurviving ? candidateA.id : candidateB.id;
    var sId = isASurviving ? candidateB.id : candidateA.id;
    var pName = isASurviving ? candidateA.name : candidateB.name;
    var sName = isASurviving ? candidateB.name : candidateA.name;

    document.getElementById('inputPrimaryId').value = pId;
    document.getElementById('inputSecondaryId').value = sId;

    document.getElementById('labelPrimaryName').innerText = pName;
    document.getElementById('labelSecondaryName').innerText = sName;

    // Cập nhật header bảng
    document.getElementById('badgeColA').className = isASurviving ? 'badge bg-primary' : 'badge bg-secondary';
    document.getElementById('badgeColA').innerText = isASurviving ? 'Hồ sơ chính' : 'Hồ sơ phụ';

    document.getElementById('badgeColB').className = isASurviving ? 'badge bg-secondary' : 'badge bg-primary';
    document.getElementById('badgeColB').innerText = isASurviving ? 'Hồ sơ phụ' : 'Hồ sơ chính';

    // Đổi radio tương ứng
    document.getElementById('fnA').value = isASurviving ? 'primary' : 'secondary';
    document.getElementById('fnB').value = isASurviving ? 'secondary' : 'primary';

    document.getElementById('emA').value = isASurviving ? 'primary' : 'secondary';
    document.getElementById('emB').value = isASurviving ? 'secondary' : 'primary';

    document.getElementById('phA').value = isASurviving ? 'primary' : 'secondary';
    document.getElementById('phB').value = isASurviving ? 'secondary' : 'primary';

    document.getElementById('rqA').value = isASurviving ? 'primary' : 'secondary';
    document.getElementById('rqB').value = isASurviving ? 'secondary' : 'primary';

    document.getElementById('stA').value = isASurviving ? 'primary' : 'secondary';
    document.getElementById('stB').value = isASurviving ? 'secondary' : 'primary';

    document.getElementById('cvA').value = isASurviving ? 'primary' : 'secondary';
    document.getElementById('cvB').value = isASurviving ? 'secondary' : 'primary';
}

function openConfirmMergeModal() {
    var pName = isASurviving ? candidateA.name : candidateB.name;
    var sName = isASurviving ? candidateB.name : candidateA.name;
    document.getElementById('modalPrimaryNameText').innerText = pName;
    document.getElementById('modalSecondaryNameText').innerText = sName;
    new bootstrap.Modal(document.getElementById('confirmMergeModal')).show();
}

function submitMergeForm() {
    document.getElementById('mergeCandidateForm').submit();
}
</script>

<jsp:include page="../common/footer.jsp" />

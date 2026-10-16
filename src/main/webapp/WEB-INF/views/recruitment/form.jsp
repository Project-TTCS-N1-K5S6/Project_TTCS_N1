<%--
  ==============================================================================
  BIỂU MẪU TẠO MỚI / CHỈNH SỬA / SAO CHÉP YÊU CẦU TUYỂN DỤNG (recruitment/form.jsp)
  Chức năng:
  - Nhập thông tin chi tiết đợt tuyển dụng: Chức danh, Phòng ban, Số lượng định biên, Lý do tuyển, Ngày cần người.
  - Đối chiếu dải lương chuẩn theo chức danh/phòng ban qua AJAX benchmark; bắt buộc nhập giải trình nếu mức lương vượt khung.
  - Kiểm tra tự động định biên headcount phòng ban; cảnh báo hoặc cho phép ghi đè kèm lý do.
  - Hỗ trợ lưu nháp (DRAFT) hoặc gửi phê duyệt ngay (PENDING_APPROVAL) để kích hoạt ma trận phê duyệt.
  ==============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="${isEdit ? 'Chỉnh sửa Yêu cầu tuyển dụng' : (isCopy ? 'Sao chép nội dung tuyển dụng' : 'Tạo mới Yêu cầu tuyển dụng')}" />
<c:set var="breadcrumb" value="Yêu cầu tuyển dụng" />
<c:set var="activeMenu" value="requisitions" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <c:if test="${isCopy}">
            <div class="alert alert-info border shadow-sm">
                <i class="bi bi-info-circle-fill me-1"></i>
                <strong>Sao chép yêu cầu:</strong> Mô tả công việc và yêu cầu ứng viên đã được sao chép. Hãy điền thông tin còn lại; yêu cầu mới sẽ luôn được lưu ở trạng thái Nháp.
            </div>
        </c:if>

        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <div class="d-flex align-items-center gap-2 mb-1">
                    <a href="${pageContext.request.contextPath}/recruitment-requests" class="btn btn-sm btn-outline-secondary py-1 px-2" title="Quay lại">
                        <i class="bi bi-arrow-left"></i>
                    </a>
                    <h4 class="fw-bold mb-0">
                        <c:choose>
                            <c:when test="${isEdit}">
                                <i class="bi bi-pencil-square text-primary me-2"></i>Chỉnh sửa Yêu cầu tuyển dụng: <span class="text-primary font-monospace">${reqItem.code}</span>
                            </c:when>
                            <c:when test="${isCopy}">
                                <i class="bi bi-copy text-success me-2"></i>Sao chép nội dung tuyển dụng
                            </c:when>
                            <c:otherwise>
                                <i class="bi bi-file-earmark-plus-fill text-primary me-2"></i>Tạo mới Yêu cầu tuyển dụng
                            </c:otherwise>
                        </c:choose>
                    </h4>
                </div>
                <p class="text-muted small mb-0 ms-4 ps-2">
                    Điền đầy đủ thông tin vị trí, định biên, dải lương đề xuất và tiêu chuẩn tuyển dụng. Có thể lưu nháp để tiếp tục hoàn thiện sau.
                </p>
            </div>
            <div>
                <span class="badge ${reqItem.statusBadgeClass != null ? reqItem.statusBadgeClass : 'bg-secondary text-white'} px-3 py-2 fs-6">
                    Trạng thái: ${reqItem.statusLabel != null ? reqItem.statusLabel : 'Bản nháp mới'}
                </span>
            </div>
        </div>

        <c:if test="${isEdit && not empty approvalSteps}">
            <!-- Khối Tiến trình & Chuỗi cấp duyệt (KN-154 -> KN-161) -->
            <div class="card shadow-sm border mb-4 bg-white">
                <div class="card-header bg-light py-3 border-bottom d-flex flex-wrap justify-content-between align-items-center gap-2">
                    <div class="d-flex align-items-center gap-2">
                        <div class="logo-icon bg-primary text-white rounded p-1 px-2 d-inline-flex align-items-center justify-content-center" style="width: 32px; height: 32px;">
                            <i class="bi bi-diagram-3-fill"></i>
                        </div>
                        <div>
                            <h6 class="fw-bold text-dark mb-0">Tiến trình &amp; Chuỗi cấp duyệt Hồ sơ</h6>
                            <small class="text-muted">Theo dõi chuỗi 3 cấp phê duyệt &bull; Lịch sử chỉ đọc (Không thể sửa hoặc xóa)</small>
                        </div>
                    </div>
                    <div>
                        <span class="approval-lock-badge">
                            <i class="bi bi-shield-lock-fill text-warning"></i>
                            Bản ghi lịch sử đã khóa (Chỉ đọc)
                        </span>
                    </div>
                </div>
                <div class="card-body p-4">
                    <!-- Box Đang chờ ai (KN-157) -->
                    <div class="approval-waiting-box mb-4">
                        <div class="approval-waiting-icon">
                            <i class="bi bi-person-fill-exclamation"></i>
                        </div>
                        <div class="flex-grow-1">
                            <small class="text-muted d-block fw-semibold" style="font-size: 0.72rem;">TRẠNG THÁI HIỆN TẠI</small>
                            <div class="fw-bold text-dark fs-6">${nextApproverInfo.waitingForText}</div>
                        </div>
                    </div>

                    <!-- Stepper Timeline (KN-159) -->
                    <div class="approval-stepper px-2 py-3 bg-light rounded border mb-4">
                        <c:forEach var="s" items="${approvalSteps}">
                            <div class="approval-stepper-item ${s.status == 'APPROVED' ? 'completed' : (s.status == 'PENDING' ? 'active' : (s.status == 'REJECTED' ? 'rejected' : (s.status == 'CHANGES_REQUESTED' ? 'changes-requested' : 'waiting')))}">
                                <div class="approval-stepper-node">
                                    <c:choose>
                                        <c:when test="${s.status == 'APPROVED'}"><i class="bi bi-check-lg"></i></c:when>
                                        <c:when test="${s.status == 'PENDING'}"><i class="bi bi-hourglass-split"></i></c:when>
                                        <c:when test="${s.status == 'REJECTED'}"><i class="bi bi-x-lg"></i></c:when>
                                        <c:when test="${s.status == 'CHANGES_REQUESTED'}"><i class="bi bi-arrow-counterclockwise"></i></c:when>
                                        <c:otherwise><i class="bi bi-clock"></i></c:otherwise>
                                    </c:choose>
                                </div>
                                <div class="approval-stepper-title">${s.stepName}</div>
                                <div class="approval-stepper-desc">${s.statusLabel}</div>
                            </div>
                        </c:forEach>
                    </div>

                    <!-- Chi tiết từng cấp duyệt & Ý kiến (KN-160, KN-161) -->
                    <div class="row g-3">
                        <c:forEach var="s" items="${approvalSteps}">
                            <div class="col-12 col-md-4">
                                <div class="approval-step-card h-100 ${s.status == 'APPROVED' ? 'status-approved' : (s.status == 'PENDING' ? 'status-pending' : (s.status == 'REJECTED' ? 'status-rejected' : (s.status == 'CHANGES_REQUESTED' ? 'status-changes_requested' : 'status-waiting')))}">
                                    <div class="d-flex align-items-center justify-content-between mb-2">
                                        <span class="fw-bold text-dark small">${s.stepName}</span>
                                        <span class="badge ${s.statusBadgeClass}" style="font-size: 0.68rem;">${s.statusLabel}</span>
                                    </div>
                                    <div class="mb-2">
                                        <small class="text-muted d-block" style="font-size: 0.72rem;">Người duyệt:</small>
                                        <c:choose>
                                            <c:when test="${not empty s.approverName}">
                                                <div class="fw-semibold text-dark small">${s.approverName}</div>
                                                <small class="text-muted text-xs">${s.approverJobTitle}</small>
                                            </c:when>
                                            <c:when test="${s.status == 'PENDING'}">
                                                <div class="text-warning-emphasis small fw-semibold"><i class="bi bi-hourglass-split me-1"></i>Đang chờ cấp này duyệt</div>
                                            </c:when>
                                            <c:otherwise>
                                                <div class="text-muted small">Chờ cấp trước</div>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                    <div class="mb-2">
                                        <small class="text-muted d-block" style="font-size: 0.72rem;">Thời điểm:</small>
                                        <div class="small fw-semibold text-secondary">
                                            <c:choose>
                                                <c:when test="${not empty s.decisionDate}"><i class="bi bi-calendar3 me-1 text-primary"></i>${s.formattedDecisionDate}</c:when>
                                                <c:otherwise>--</c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>
                                    <div class="mt-2">
                                        <small class="text-muted d-block" style="font-size: 0.72rem;">Ý kiến người duyệt:</small>
                                        <c:choose>
                                            <c:when test="${not empty s.comment}">
                                                <div class="approval-comment-box ${s.status == 'APPROVED' ? 'comment-approved' : (s.status == 'REJECTED' ? 'comment-rejected' : (s.status == 'CHANGES_REQUESTED' ? 'comment-changes-requested' : ''))} py-1 px-2 mt-1" style="font-size: 0.78rem;">
                                                    "${s.comment}"
                                                </div>
                                            </c:when>
                                            <c:otherwise>
                                                <div class="text-muted fst-italic small" style="font-size: 0.75rem;">Chưa có ý kiến</div>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </div>
        </c:if>

        <c:if test="${reqItem.changesRequested || reqItem.status == 'CHANGES_REQUESTED'}">
            <div class="alert alert-warning border-warning d-flex align-items-center gap-3 p-3 mb-4 shadow-sm" role="alert">
                <div class="fs-3 text-warning"><i class="bi bi-exclamation-triangle-fill"></i></div>
                <div>
                    <h6 class="alert-heading fw-bold mb-1">Hồ sơ được yêu cầu bổ sung thông tin</h6>
                    <p class="mb-0 small">Hồ sơ này đã được cấp phê duyệt trả về để bổ sung hoặc hoàn thiện lại nội dung. Vui lòng xem nhận xét của người duyệt, cập nhật các trường thông tin cần thiết và bấm <strong>Gửi phê duyệt</strong> để tiếp tục quy trình.</p>
                </div>
            </div>
        </c:if>

        <form id="requisitionForm" method="post" action="${pageContext.request.contextPath}/recruitment-requests/save" novalidate>
            <input type="hidden" name="id" value="${reqItem.id}">
            <input type="hidden" name="code" value="${isEdit ? reqItem.code : nextCode}">
            <c:if test="${isCopy}">
                <input type="hidden" name="copyMode" value="true">
            </c:if>

            <div class="row g-4">
                <div class="col-12">
                    <!-- Khối 1: Khai báo Chức danh, Phòng ban, Định biên -->
                    <div class="card shadow-sm border mb-4">
                        <div class="card-header bg-white py-3 border-bottom">
                            <h6 class="fw-bold mb-0 text-dark">
                                <i class="bi bi-card-checklist text-primary me-2"></i>1. Thông tin Vị trí &amp; Định biên
                            </h6>
                        </div>
                        <div class="card-body p-4">
                            <div class="row g-3">
                                <!-- Mã yêu cầu -->
                                <div class="col-12 col-md-3">
                                    <label class="form-label text-muted small fw-semibold">Mã yêu cầu</label>
                                    <input type="text" class="form-control form-control-sm bg-light font-monospace fw-bold" 
                                           value="${isEdit ? reqItem.code : nextCode}" readonly>
                                </div>

                                <!-- Tiêu đề yêu cầu -->
                                <div class="col-12 col-md-9">
                                    <label class="form-label text-muted small fw-semibold">Tiêu đề phiếu yêu cầu</label>
                                    <input type="text" name="title" id="titleInput" class="form-control form-control-sm" 
                                           placeholder="VD: Tuyển dụng Kỹ sư Java Backend Senior"
                                           value="<c:out value='${reqItem.title}' />">
                                </div>

                                <!-- Chức danh cần tuyển -->
                                <div class="col-12 col-md-6">
                                    <label class="form-label small fw-bold text-dark">
                                        Chức danh cần tuyển <span class="text-danger">*</span>
                                    </label>
                                    <div class="input-group input-group-sm">
                                        <input type="text" name="positionTitle" id="positionTitleInput" class="form-control" 
                                               list="standardPositionsList"
                                               placeholder="Chọn hoặc nhập chức danh..." 
                                               value="${reqItem.positionTitle}" required>
                                        <button class="btn btn-outline-secondary dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                                            Danh mục chuẩn
                                        </button>
                                        <ul class="dropdown-menu dropdown-menu-end shadow-sm" style="max-height: 250px; overflow-y: auto;">
                                            <c:forEach var="sr" items="${salaryRanges}">
                                                <li>
                                                    <a class="dropdown-item small py-2" href="javascript:void(0)" 
                                                       onclick="selectPosition('${sr.positionTitle}', '${sr.departmentId}', '${sr.minSalary}', '${sr.maxSalary}')">
                                                        <div class="fw-semibold">${sr.positionTitle}</div>
                                                        <div class="text-xs text-muted">${sr.departmentName} &bull; Chuẩn: ${sr.formattedRange}</div>
                                                    </a>
                                                </li>
                                            </c:forEach>
                                        </ul>
                                    </div>
                                    <datalist id="standardPositionsList">
                                        <c:forEach var="sr" items="${salaryRanges}">
                                            <option value="${sr.positionTitle}">${sr.departmentName} (Chuẩn: ${sr.formattedRange})</option>
                                        </c:forEach>
                                    </datalist>
                                    <div class="form-text text-xs">Gõ hoặc chọn chức danh chuẩn để đối chiếu dải ngân sách lương.</div>
                                </div>

                                <!-- Phòng ban -->
                                <div class="col-12 col-md-6">
                                    <label class="form-label small fw-bold text-dark">
                                        Phòng ban phụ trách <span class="text-danger">*</span>
                                    </label>
                                    <select name="departmentId" id="departmentSelect" class="form-select form-select-sm" required>
                                        <option value="">-- Chọn phòng ban --</option>
                                        <c:forEach var="d" items="${departments}">
                                            <option value="${d.id}" ${(reqItem.departmentId == d.id) ? 'selected' : ''}>${d.name}</option>
                                        </c:forEach>
                                    </select>
                                </div>

                                <!-- Số lượng tuyển -->
                                <div class="col-12 col-md-4">
                                    <label class="form-label small fw-bold text-dark">
                                        Số lượng tuyển <span class="text-danger">*</span>
                                    </label>
                                    <div class="input-group input-group-sm">
                                        <input type="number" name="headcount" id="headcountInput" class="form-control text-center fw-bold" 
                                               min="1" max="100" value="${reqItem.headcount > 0 ? reqItem.headcount : 1}" required>
                                        <span class="input-group-text">Nhân sự</span>
                                    </div>
                                </div>

                                <!-- Lý do tuyển dụng -->
                                <div class="col-12 col-md-4">
                                    <label class="form-label small fw-bold text-dark">
                                        Lý do tuyển <span class="text-danger">*</span>
                                    </label>
                                    <select name="recruitmentReason" class="form-select form-select-sm" required>
                                        <option value="NEW_HEADCOUNT" ${reqItem.recruitmentReason != 'REPLACEMENT' ? 'selected' : ''}>Tăng mới (Mở rộng quy mô)</option>
                                        <option value="REPLACEMENT" ${reqItem.recruitmentReason == 'REPLACEMENT' ? 'selected' : ''}>Thay thế (Nhân sự nghỉ việc)</option>
                                    </select>
                                </div>

                                <!-- Ngày cần người (Deadline) -->
                                <div class="col-12 col-md-4">
                                    <label class="form-label small fw-bold text-dark">
                                        Ngày cần người <span class="text-danger">*</span>
                                    </label>
                                    <input type="date" name="deadline" id="deadlineInput" class="form-control form-control-sm" 
                                           min="${todayDate}" value="${reqItem.deadline}" required>
                                    <div class="form-text text-xs text-danger" id="deadlineHelp">
                                        <i class="bi bi-info-circle me-1"></i>Không được ở quá khứ
                                    </div>
                                </div>

                                <!-- BẢNG THEO DÕI ĐỊNH BIÊN & CẢNH BÁO VƯỢT CHỈ TIÊU (KN-195 -> KN-201) -->
                                <div class="col-12 mt-2" id="headcountBudgetSection">
                                    <div class="p-3 rounded border bg-light" id="budgetQuotaBox">
                                        <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-2">
                                            <div class="d-flex align-items-center gap-2">
                                                <i class="bi bi-wallet2 text-primary fs-5"></i>
                                                <span class="fw-bold small text-dark">Kiểm soát Định biên &amp; Ngân sách phòng ban:</span>
                                                <span class="badge bg-secondary-subtle text-secondary border" id="quotaStatusBadge">Chưa chọn phòng ban</span>
                                            </div>
                                            <div class="small text-muted" id="fiscalYearLabel"></div>
                                        </div>

                                        <!-- Thẻ thông tin Headcount & Ngân sách -->
                                        <div class="row g-2 text-center" id="quotaStatsRow" style="display: none;">
                                            <div class="col-6 col-md-3">
                                                <div class="border rounded bg-white p-2">
                                                    <div class="text-muted text-xs">Chỉ tiêu Headcount</div>
                                                    <div class="fw-bold text-dark fs-6" id="targetHeadcountDisplay">--</div>
                                                </div>
                                            </div>
                                            <div class="col-6 col-md-3">
                                                <div class="border rounded bg-white p-2">
                                                    <div class="text-muted text-xs">Đã sử dụng</div>
                                                    <div class="fw-bold text-warning-emphasis fs-6" id="usedHeadcountDisplay">--</div>
                                                </div>
                                            </div>
                                            <div class="col-6 col-md-3">
                                                <div class="border rounded bg-white p-2">
                                                    <div class="text-muted text-xs">Headcount còn lại</div>
                                                    <div class="fw-bold fs-6" id="remainingHeadcountDisplay">--</div>
                                                </div>
                                            </div>
                                            <div class="col-6 col-md-3">
                                                <div class="border rounded bg-white p-2">
                                                    <div class="text-muted text-xs">Ngân sách còn lại</div>
                                                    <div class="fw-bold fs-6" id="remainingBudgetDisplay">--</div>
                                                </div>
                                            </div>
                                        </div>

                                        <!-- Hộp cảnh báo vượt chỉ tiêu -->
                                        <div class="alert alert-danger d-flex align-items-start gap-2 mt-3 mb-0" id="quotaWarningAlert" style="display: none !important;">
                                            <i class="bi bi-exclamation-octagon-fill fs-5 mt-1 flex-shrink-0"></i>
                                            <div class="flex-grow-1">
                                                <h6 class="fw-bold mb-1" id="quotaWarningTitle">CẢNH BÁO CHẶN: Vượt chỉ tiêu định biên hoặc ngân sách!</h6>
                                                <p class="small mb-2" id="quotaWarningDesc"></p>
                                                
                                                <!-- Khu vực xác nhận ghi đè dành riêng cho Trưởng phòng Nhân sự -->
                                                <c:choose>
                                                    <c:when test="${currentUser.hasRole('ADMIN') || currentUser.hasRole('HR_MANAGER') || currentUser.hasPermission('budget.override')}">
                                                        <div class="p-3 bg-white border border-danger-subtle rounded mt-2">
                                                            <div class="form-check mb-2">
                                                                <input class="form-check-input" type="checkbox" name="isBudgetOverridden" id="isBudgetOverriddenCheck" value="true"
                                                                       ${reqItem.budgetOverridden ? 'checked' : ''} onchange="toggleOverrideReason(this.checked)">
                                                                <label class="form-check-label fw-bold text-danger small" for="isBudgetOverriddenCheck">
                                                                    <i class="bi bi-shield-lock-fill me-1"></i>Xác nhận ghi đè vượt chỉ tiêu (Quyền Trưởng phòng Nhân sự / Quản trị hệ thống)
                                                                </label>
                                                            </div>
                                                            <div id="overrideReasonContainer" style="${reqItem.budgetOverridden ? '' : 'display: none;'}">
                                                                <label class="form-label small fw-semibold text-dark">
                                                                    Lý do ghi đè chỉ tiêu <span class="text-danger">*</span>
                                                                </label>
                                                                <textarea name="budgetOverrideReason" id="budgetOverrideReasonInput" class="form-control form-control-sm"
                                                                          rows="2" placeholder="Bắt buộc nhập lý do đặc biệt cho phép vượt định biên / ngân sách..."><c:out value="${reqItem.budgetOverrideReason}" /></textarea>
                                                                <div class="text-xs text-muted mt-1">Thông tin xác nhận, thời gian và lý do sẽ được lưu vào nhật ký kiểm toán hệ thống.</div>
                                                            </div>
                                                        </div>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <div class="p-2 bg-white border border-danger-subtle rounded mt-2 text-danger small">
                                                            <i class="bi bi-shield-exclamation me-1"></i>
                                                            <strong>Lưu ý:</strong> Bạn không có thẩm quyền xác nhận ghi đè. Cần Trưởng phòng Nhân sự xác nhận và ghi rõ lý do trước khi gửi duyệt phiếu yêu cầu này.
                                                        </div>
                                                    </c:otherwise>
                                                </c:choose>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Khối 2: Dải lương đề xuất & Kiểm soát dải chuẩn -->
                    <div class="card shadow-sm border mb-4">
                        <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                            <h6 class="fw-bold mb-0 text-dark">
                                <i class="bi bi-cash-stack text-success me-2"></i>2. Dải lương đề xuất &amp; Ngân sách
                            </h6>
                            <span class="badge bg-light text-secondary border" id="benchmarkBadge">
                                Chưa xác định dải chuẩn
                            </span>
                        </div>
                        <div class="card-body p-4">
                            <!-- Hộp thông tin dải chuẩn của chức danh -->
                            <div class="p-3 mb-3 rounded border bg-light" id="standardRangeBox">
                                <div class="d-flex align-items-center justify-content-between">
                                    <div>
                                        <small class="text-muted text-uppercase fw-semibold" style="font-size: 11px;">Dải lương chuẩn của chức danh:</small>
                                        <div class="fw-bold fs-6 text-dark" id="standardRangeDisplay">
                                            (Chọn chức danh để nạp dải lương chuẩn theo chính sách)
                                        </div>
                                    </div>
                                    <i class="bi bi-shield-check text-success fs-3 opacity-75"></i>
                                </div>
                            </div>

                            <div class="row g-3">
                                <!-- Lương tối thiểu đề xuất -->
                                <div class="col-12 col-md-6">
                                    <label class="form-label small fw-bold text-dark">
                                        Lương tối thiểu đề xuất (VNĐ) <span class="text-danger">*</span>
                                    </label>
                                    <div class="input-group input-group-sm">
                                        <input type="number" name="minSalary" id="minSalaryInput" class="form-control fw-bold" 
                                               step="500000" placeholder="VD: 25000000" 
                                               value="${reqItem.minSalary != null ? reqItem.minSalary.toBigInteger() : ''}" required>
                                        <span class="input-group-text">VND</span>
                                    </div>
                                    <div class="text-xs text-muted mt-1" id="minSalaryFormatted"></div>
                                </div>

                                <!-- Lương tối đa đề xuất -->
                                <div class="col-12 col-md-6">
                                    <label class="form-label small fw-bold text-dark">
                                        Lương tối đa đề xuất (VNĐ) <span class="text-danger">*</span>
                                    </label>
                                    <div class="input-group input-group-sm">
                                        <input type="number" name="maxSalary" id="maxSalaryInput" class="form-control fw-bold" 
                                               step="500000" placeholder="VD: 45000000" 
                                               value="${reqItem.maxSalary != null ? reqItem.maxSalary.toBigInteger() : ''}" required>
                                        <span class="input-group-text">VND</span>
                                    </div>
                                    <div class="text-xs text-muted mt-1" id="maxSalaryFormatted"></div>
                                </div>
                            </div>

                            <!-- Alert Box cảnh báo vượt dải chuẩn -->
                            <div class="alert alert-warning border border-warning shadow-sm mt-3 mb-3 d-flex align-items-start gap-2" 
                                 id="salaryWarningBox" style="display: none;">
                                <i class="bi bi-exclamation-triangle-fill fs-5 text-warning flex-shrink-0 mt-1"></i>
                                <div>
                                    <strong class="d-block text-warning-emphasis">Cảnh báo: Dải lương đề xuất nằm ngoài dải chuẩn!</strong>
                                    <span class="small" id="salaryWarningText">
                                        Mức lương đề xuất không nằm trong khung chuẩn của chức danh. Theo quy định tuyển dụng, bạn <b>bắt buộc phải nhập văn bản giải trình lý do</b> bên dưới để trình Ban Giám đốc phê duyệt.
                                    </span>
                                </div>
                            </div>

                            <!-- Trường giải trình ngân sách -->
                            <div class="mt-3" id="explanationGroup">
                                <label class="form-label small fw-bold text-dark d-flex justify-content-between align-items-center">
                                    <span>
                                        Giải trình lý do vượt / nằm ngoài dải chuẩn 
                                        <span class="text-danger" id="explanationRequiredStar" style="display:none;">* (Bắt buộc)</span>
                                    </span>
                                    <span class="badge bg-secondary-subtle text-secondary" id="explanationStatusBadge">Tùy chọn</span>
                                </label>
                                <textarea name="salaryExplanation" id="salaryExplanationInput" class="form-control" rows="3" 
                                          placeholder="Nêu rõ lý do cần tuyển mức lương ngoài chuẩn (VD: Yêu cầu chuyên gia công nghệ hiếm, ứng viên có kinh nghiệm đặc thù, dự án trọng điểm cấp bách...)">${reqItem.salaryExplanation}</textarea>
                                <div class="form-text text-xs" id="explanationHelp">
                                    Bắt buộc điền nếu mức lương đề xuất thấp hơn mức sàn hoặc cao hơn mức trần của dải lương chuẩn.
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Khối 3: Soạn mô tả công việc (JD) -->
                    <div class="card shadow-sm border mb-4">
                        <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                            <h6 class="fw-bold mb-0 text-dark">
                                <i class="bi bi-file-text text-primary me-2"></i>3. Bản mô tả công việc (JD) <span class="text-danger">*</span>
                            </h6>
                            <button type="button" class="btn btn-sm btn-outline-primary py-1 px-3" onclick="insertJdTemplate()">
                                <i class="bi bi-magic me-1"></i>Mẫu gợi ý
                            </button>
                        </div>
                        <div class="card-body p-4">
                            <textarea name="jobDescription" id="jobDescriptionInput" class="form-control font-sans" rows="5" 
                                      placeholder="- Tham gia thiết kế và phát triển các hệ thống Backend...&#10;- Tối ưu hóa hiệu năng cơ sở dữ liệu và API...&#10;- Phối hợp cùng đội ngũ QA và Product Manager..."><c:out value="${reqItem.jobDescription}" /></textarea>
                            <div class="form-text text-xs">Mô tả rõ ràng trách nhiệm công việc, nhiệm vụ chính và mục tiêu cần đạt được.</div>
                        </div>
                    </div>

                    <!-- Khối 4: Soạn yêu cầu ứng viên -->
                    <div class="card shadow-sm border mb-4">
                        <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                            <h6 class="fw-bold mb-0 text-dark">
                                <i class="bi bi-person-check text-success me-2"></i>4. Yêu cầu ứng viên <span class="text-danger">*</span>
                            </h6>
                            <button type="button" class="btn btn-sm btn-outline-success py-1 px-3" onclick="insertReqTemplate()">
                                <i class="bi bi-magic me-1"></i>Mẫu gợi ý
                            </button>
                        </div>
                        <div class="card-body p-4">
                            <textarea name="jobRequirements" id="jobRequirementsInput" class="form-control font-sans" rows="5" 
                                      placeholder="- Tối thiểu 3 năm kinh nghiệm lập trình Java Servlet/Spring...&#10;- Thành thạo MySQL, tối ưu câu lệnh truy vấn...&#10;- Kỹ năng làm việc nhóm, tư duy giải quyết vấn đề tốt..."><c:out value="${reqItem.jobRequirements}" /></textarea>
                            <div class="form-text text-xs">Liệt kê kinh nghiệm, bằng cấp, kỹ năng chuyên môn và phẩm chất cần thiết.</div>
                        </div>
                    </div>

                    <!-- Khối 5: Thao tác nộp / Lưu nháp -->
                    <div class="card shadow-sm border bg-light mb-4">
                        <div class="card-body p-3 d-flex flex-wrap justify-content-between align-items-center gap-3">
                            <div>
                                <h6 class="fw-bold text-dark mb-1">Thao tác xử lý yêu cầu</h6>
                                <small class="text-muted">Bạn có thể lưu nháp để tiếp tục hoàn thiện sau, hoặc gửi duyệt để chuyển tiếp đến cấp quản lý.</small>
                            </div>
                            <div class="d-flex flex-wrap align-items-center gap-2">
                                <a href="${pageContext.request.contextPath}/recruitment-requests" class="btn btn-outline-secondary btn-sm px-3">
                                    <i class="bi bi-x-circle me-1"></i> Hủy bỏ
                                </a>
                                <button type="submit" name="action" value="draft" class="btn btn-outline-primary btn-sm px-3" id="draftBtn">
                                    <i class="bi bi-save me-1"></i> ${isCopy ? 'Tạo bản sao Nháp' : 'Lưu bản nháp'}
                                </button>
                                <c:if test="${not isCopy}">
                                    <button type="submit" name="action" value="submit" class="btn btn-primary btn-sm px-3 shadow-sm" id="submitBtn">
                                        <i class="bi bi-send-check-fill me-1"></i> Gửi phê duyệt yêu cầu
                                    </button>
                                </c:if>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </form>
    </div>

<jsp:include page="../common/footer.jsp" />

<!-- Dữ liệu dải chuẩn phía Client để tính toán tức thời -->
<script>
var salaryRangesData = [
    <c:forEach var="s" items="${salaryRanges}" varStatus="status">
    {
        positionTitle: "${s.positionTitle}",
        departmentId: "${s.departmentId}",
        minSalary: ${s.minSalary != null ? s.minSalary : 0},
        maxSalary: ${s.maxSalary != null ? s.maxSalary : 0},
        currency: "${s.currency}",
        formatted: "${s.formattedRange}"
    }${!status.last ? ',' : ''}
    </c:forEach>
];

var todayString = "${todayDate}";

function formatCurrencyVN(val) {
    if (!val || isNaN(val)) return "";
    return new Intl.NumberFormat('vi-VN').format(val) + ' VND';
}

function selectPosition(pos, deptId, minSal, maxSal) {
    document.getElementById('positionTitleInput').value = pos;
    if (deptId && !document.getElementById('departmentSelect').value) {
        document.getElementById('departmentSelect').value = deptId;
    }
    if (!document.getElementById('titleInput').value) {
        document.getElementById('titleInput').value = 'Tuyển dụng ' + pos;
    }
    if (!document.getElementById('minSalaryInput').value && minSal) {
        document.getElementById('minSalaryInput').value = minSal;
    }
    if (!document.getElementById('maxSalaryInput').value && maxSal) {
        document.getElementById('maxSalaryInput').value = maxSal;
    }
    checkSalaryBenchmark();
}

function findStandardBenchmark(positionTitle, departmentId) {
    if (!positionTitle) return null;
    var normPos = positionTitle.trim().toLowerCase();
    
    // Tìm khớp cả dept và position
    if (departmentId) {
        for (var i = 0; i < salaryRangesData.length; i++) {
            var item = salaryRangesData[i];
            if (item.positionTitle.trim().toLowerCase() === normPos && item.departmentId === departmentId) {
                return item;
            }
        }
    }
    // Fallback chỉ theo position
    for (var j = 0; j < salaryRangesData.length; j++) {
        var s = salaryRangesData[j];
        if (s.positionTitle.trim().toLowerCase() === normPos) {
            return s;
        }
    }
    return null;
}

function checkSalaryBenchmark() {
    var pos = document.getElementById('positionTitleInput').value;
    var dept = document.getElementById('departmentSelect').value;
    var minVal = parseFloat(document.getElementById('minSalaryInput').value) || 0;
    var maxVal = parseFloat(document.getElementById('maxSalaryInput').value) || 0;

    // Format helper labels
    document.getElementById('minSalaryFormatted').textContent = minVal > 0 ? formatCurrencyVN(minVal) : '';
    document.getElementById('maxSalaryFormatted').textContent = maxVal > 0 ? formatCurrencyVN(maxVal) : '';

    var benchmark = findStandardBenchmark(pos, dept);
    var warningBox = document.getElementById('salaryWarningBox');
    var warningText = document.getElementById('salaryWarningText');
    var badge = document.getElementById('benchmarkBadge');
    var display = document.getElementById('standardRangeDisplay');
    var expGroup = document.getElementById('explanationGroup');
    var expStar = document.getElementById('explanationRequiredStar');
    var expBadge = document.getElementById('explanationStatusBadge');
    var expInput = document.getElementById('salaryExplanationInput');

    if (benchmark) {
        badge.className = 'badge bg-success-subtle text-success border border-success-subtle';
        badge.textContent = 'Dải chuẩn: ' + benchmark.formatted;
        display.innerHTML = '<span class="text-success fw-bold">' + benchmark.formatted + '</span> ' +
                            '<span class="badge bg-secondary-subtle text-secondary ms-2">Chính sách ban hành</span>';

        var outOfRange = false;
        var reasons = [];

        if (minVal > 0 && benchmark.minSalary > 0 && minVal < benchmark.minSalary) {
            outOfRange = true;
            reasons.push('Lương tối thiểu (' + formatCurrencyVN(minVal) + ') thấp hơn sàn chuẩn (' + formatCurrencyVN(benchmark.minSalary) + ')');
        }
        if (maxVal > 0 && benchmark.maxSalary > 0 && maxVal > benchmark.maxSalary) {
            outOfRange = true;
            reasons.push('Lương tối đa (' + formatCurrencyVN(maxVal) + ') vượt mức trần chuẩn (' + formatCurrencyVN(benchmark.maxSalary) + ')');
        }

        if (outOfRange) {
            warningBox.style.display = 'flex';
            warningText.innerHTML = 'Dải lương đề xuất nằm ngoài khung chuẩn (' + benchmark.formatted + '): <b>' + reasons.join('; ') + '</b>.<br>Bạn <b>bắt buộc phải nhập giải trình ngân sách</b> bên dưới.';
            
            expStar.style.display = 'inline';
            expBadge.className = 'badge bg-danger text-white';
            expBadge.textContent = 'Bắt buộc nhập giải trình';
            expInput.classList.add('border-warning');
        } else {
            warningBox.style.display = 'none';
            expStar.style.display = 'none';
            expBadge.className = 'badge bg-secondary-subtle text-secondary';
            expBadge.textContent = 'Trong dải chuẩn (Tùy chọn)';
            expInput.classList.remove('border-warning');
        }
    } else {
        badge.className = 'badge bg-light text-secondary border';
        badge.textContent = 'Chưa có dải chuẩn';
        display.innerHTML = '<span class="text-muted">Chức danh chưa thiết lập khung chuẩn trong danh mục ngân sách</span>';
        warningBox.style.display = 'none';
        expStar.style.display = 'none';
        expBadge.className = 'badge bg-secondary-subtle text-secondary';
        expBadge.textContent = 'Tùy chọn';
        expInput.classList.remove('border-warning');
    }
}

// Bắt sự kiện thay đổi
document.getElementById('positionTitleInput').addEventListener('input', checkSalaryBenchmark);
document.getElementById('departmentSelect').addEventListener('change', checkSalaryBenchmark);
document.getElementById('minSalaryInput').addEventListener('input', checkSalaryBenchmark);
document.getElementById('maxSalaryInput').addEventListener('input', checkSalaryBenchmark);

// Kiểm tra ngày cần người không được ở quá khứ
document.getElementById('deadlineInput').addEventListener('change', function() {
    var selected = this.value;
    if (selected && selected < todayString) {
        alert('Cảnh báo: Ngày cần người không được ở quá khứ! Vui lòng chọn ngày từ hôm nay trở đi.');
        this.value = todayString;
    }
});

// Kiểm tra form trước khi Submit hoặc Draft
document.getElementById('requisitionForm').addEventListener('submit', function(e) {
    var action = document.activeElement ? document.activeElement.getAttribute('value') : 'submit';
    var isDraft = (action === 'draft');

    var pos = document.getElementById('positionTitleInput').value.trim();
    if (!pos) {
        alert('Vui lòng nhập hoặc chọn chức danh cần tuyển!');
        document.getElementById('positionTitleInput').focus();
        e.preventDefault();
        return false;
    }

    var deadlineVal = document.getElementById('deadlineInput').value;
    if (deadlineVal && deadlineVal < todayString) {
        alert('Lỗi: Ngày cần người không được ở quá khứ! Vui lòng chọn từ ngày ' + todayString + ' trở đi.');
        document.getElementById('deadlineInput').focus();
        e.preventDefault();
        return false;
    }

    var minVal = parseFloat(document.getElementById('minSalaryInput').value) || 0;
    var maxVal = parseFloat(document.getElementById('maxSalaryInput').value) || 0;
    if (minVal > 0 && maxVal > 0 && minVal > maxVal) {
        alert('Lỗi: Lương tối thiểu đề xuất không được lớn hơn lương tối đa!');
        document.getElementById('minSalaryInput').focus();
        e.preventDefault();
        return false;
    }

    // Nếu Gửi duyệt (không phải lưu nháp), kiểm tra nghiêm ngặt
    if (!isDraft) {
        var dept = document.getElementById('departmentSelect').value;
        if (!dept) {
            alert('Vui lòng chọn phòng ban phụ trách!');
            document.getElementById('departmentSelect').focus();
            e.preventDefault();
            return false;
        }

        if (!deadlineVal) {
            alert('Vui lòng chọn ngày cần người!');
            document.getElementById('deadlineInput').focus();
            e.preventDefault();
            return false;
        }

        var jd = document.getElementById('jobDescriptionInput').value.trim();
        if (!jd) {
            alert('Vui lòng soạn bản mô tả công việc (Job Description)!');
            document.getElementById('jobDescriptionInput').focus();
            e.preventDefault();
            return false;
        }

        var req = document.getElementById('jobRequirementsInput').value.trim();
        if (!req) {
            alert('Vui lòng soạn yêu cầu ứng viên (Candidate Requirements)!');
            document.getElementById('jobRequirementsInput').focus();
            e.preventDefault();
            return false;
        }

        // TIÊU CHÍ QUAN TRỌNG: Dải lương nằm ngoài dải chuẩn thì BẮT BUỘC nhập giải trình
        var benchmark = findStandardBenchmark(pos, dept);
        if (benchmark) {
            var outOfRange = false;
            if (minVal > 0 && benchmark.minSalary > 0 && minVal < benchmark.minSalary) outOfRange = true;
            if (maxVal > 0 && benchmark.maxSalary > 0 && maxVal > benchmark.maxSalary) outOfRange = true;

            if (outOfRange) {
                var expText = document.getElementById('salaryExplanationInput').value.trim();
                if (!expText) {
                    alert('YÊU CẦU BẮT BUỘC:\nDải lương đề xuất nằm ngoài khung dải chuẩn của chức danh (' + benchmark.formatted + ').\nBạn bắt buộc phải nhập nội dung giải trình ngân sách!');
                    document.getElementById('salaryExplanationInput').focus();
                    document.getElementById('salaryExplanationInput').classList.add('is-invalid');
                    e.preventDefault();
                    return false;
                }
            }
        }

        // TIÊU CHÍ QUAN TRỌNG (KN-195, KN-199): Kiểm tra vượt chỉ tiêu Định biên & Ngân sách phòng ban
        if (isQuotaExceeded) {
            var overrideCheck = document.getElementById('isBudgetOverriddenCheck');
            if (!overrideCheck || !overrideCheck.checked) {
                var errMsg = (currentQuotaData && currentQuotaData.message) 
                        ? currentQuotaData.message 
                        : 'Yêu cầu tuyển dụng vượt chỉ tiêu định biên hoặc ngân sách phòng ban!';
                alert(errMsg + '\n\nTheo quy định, cần Trưởng phòng Nhân sự xác nhận ghi đè và ghi rõ lý do trước khi gửi duyệt phiếu yêu cầu này.');
                document.getElementById('headcountBudgetSection').scrollIntoView({ behavior: 'smooth' });
                e.preventDefault();
                return false;
            }

            var reason = document.getElementById('budgetOverrideReasonInput').value.trim();
            if (!reason) {
                alert('YÊU CẦU BẮT BUỘC:\nTrường hợp vượt chỉ tiêu định biên/ngân sách, bạn bắt buộc phải nhập lý do xác nhận ghi đè!');
                document.getElementById('budgetOverrideReasonInput').focus();
                document.getElementById('budgetOverrideReasonInput').classList.add('is-invalid');
                e.preventDefault();
                return false;
            }
        }
    }
});

function insertJdTemplate() {
    var title = document.getElementById('positionTitleInput').value.trim() || 'nhân sự';
    var template = "- Chịu trách nhiệm thực hiện các nhiệm vụ chuyên môn liên quan đến vị trí " + title + ".\n" +
                   "- Phối hợp cùng các thành viên trong bộ phận để triển khai kế hoạch công việc theo quý/tháng.\n" +
                   "- Tham gia đánh giá, tối ưu quy trình và báo cáo tiến độ trực tiếp cho Quản lý bộ phận.\n" +
                   "- Thực hiện các nhiệm vụ chuyên trách khác theo yêu cầu của cấp trên.";
    document.getElementById('jobDescriptionInput').value = template;
}

function insertReqTemplate() {
    var title = document.getElementById('positionTitleInput').value.trim() || 'vị trí';
    var template = "- Tốt nghiệp Đại học/Cao đẳng chuyên ngành liên quan đến " + title + ".\n" +
                   "- Tối thiểu từ 2 - 3 năm kinh nghiệm làm việc thực tế ở vị trí tương đương.\n" +
                   "- Kỹ năng giao tiếp, làm việc nhóm và chủ động giải quyết vấn đề hiệu quả.\n" +
                   "- Tinh thần trách nhiệm cao, chịu được áp lực tiến độ công việc.";
    document.getElementById('jobRequirementsInput').value = template;
}

// =========================================================================
// XỬ LÝ KIỂM TRA ĐỊNH BIÊN & NGÂN SÁCH THỜI GIAN THỰC (KN-195 -> KN-201)
// =========================================================================
var isQuotaExceeded = false;
var currentQuotaData = null;

function toggleOverrideReason(checked) {
    var container = document.getElementById('overrideReasonContainer');
    if (container) {
        container.style.display = checked ? 'block' : 'none';
        if (checked) {
            document.getElementById('budgetOverrideReasonInput').focus();
        }
    }
}

function checkHeadcountBudget() {
    var deptId = document.getElementById('departmentSelect').value;
    var deadlineVal = document.getElementById('deadlineInput').value;
    var headcountVal = document.getElementById('headcountInput').value || 1;
    var maxSal = document.getElementById('maxSalaryInput').value || '';
    var minSal = document.getElementById('minSalaryInput').value || '';
    var salaryVal = maxSal || minSal;
    var year = deadlineVal ? new Date(deadlineVal).getFullYear() : new Date().getFullYear();
    var reqId = '${reqItem.id}' || '';

    var badge = document.getElementById('quotaStatusBadge');
    var statsRow = document.getElementById('quotaStatsRow');
    var warningAlert = document.getElementById('quotaWarningAlert');
    var warningTitle = document.getElementById('quotaWarningTitle');
    var warningDesc = document.getElementById('quotaWarningDesc');
    var yearLabel = document.getElementById('fiscalYearLabel');

    if (!deptId) {
        badge.className = 'badge bg-secondary-subtle text-secondary border';
        badge.innerText = 'Chưa chọn phòng ban';
        statsRow.style.display = 'none';
        warningAlert.style.setProperty('display', 'none', 'important');
        yearLabel.innerText = '';
        isQuotaExceeded = false;
        currentQuotaData = null;
        return;
    }

    yearLabel.innerText = 'Năm tài chính: ' + year;

    var url = '${pageContext.request.contextPath}/recruitment-requests/api/budget-check' +
              '?departmentId=' + encodeURIComponent(deptId) +
              '&deadline=' + encodeURIComponent(deadlineVal) +
              '&headcount=' + encodeURIComponent(headcountVal) +
              '&maxSalary=' + encodeURIComponent(maxSal) +
              '&minSalary=' + encodeURIComponent(minSal) +
              '&requisitionId=' + encodeURIComponent(reqId);

    fetch(url)
        .then(function(res) { return res.json(); })
        .then(function(data) {
            currentQuotaData = data;
            statsRow.style.display = 'flex';

            if (!data.configured) {
                // Chưa khai báo định biên: coi như cảnh báo chặn
                badge.className = 'badge bg-warning-subtle text-warning-emphasis border border-warning-subtle';
                badge.innerText = 'Chưa khai báo định biên';

                document.getElementById('targetHeadcountDisplay').innerText = '--';
                document.getElementById('usedHeadcountDisplay').innerText = '--';
                document.getElementById('remainingHeadcountDisplay').innerText = '--';
                document.getElementById('remainingBudgetDisplay').innerText = '--';

                warningTitle.innerText = 'CHƯA CÓ DỮ LIỆU ĐỊNH BIÊN & NGÂN SÁCH';
                warningDesc.innerText = data.message;
                warningAlert.style.setProperty('display', 'flex', 'important');
                isQuotaExceeded = true;

            } else {
                document.getElementById('targetHeadcountDisplay').innerText = data.targetHeadcount + ' người';
                document.getElementById('usedHeadcountDisplay').innerText = data.usedHeadcount + ' người';
                
                var remHcElem = document.getElementById('remainingHeadcountDisplay');
                remHcElem.innerText = data.remainingHeadcount + ' người';
                remHcElem.className = 'fw-bold fs-6 ' + (data.remainingHeadcount < 0 ? 'text-danger' : 'text-success');

                var remBgElem = document.getElementById('remainingBudgetDisplay');
                remBgElem.innerText = data.formattedRemainingBudget;
                remBgElem.className = 'fw-bold fs-6 ' + (data.remainingSalaryBudget < 0 ? 'text-danger' : 'text-success');

                if (data.exceeded) {
                    badge.className = 'badge bg-danger text-white';
                    badge.innerText = 'Vượt chỉ tiêu định biên/ngân sách';

                    warningTitle.innerText = 'CẢNH BÁO CHẶN: VƯỢT CHỈ TIÊU ĐỊNH BIÊN HOẶC NGÂN SÁCH!';
                    warningDesc.innerText = data.message;
                    warningAlert.style.setProperty('display', 'flex', 'important');
                    isQuotaExceeded = true;
                } else {
                    badge.className = 'badge bg-success-subtle text-success border border-success-subtle';
                    badge.innerText = 'Định biên hợp lệ (' + data.remainingHeadcount + ' HC còn lại)';
                    warningAlert.style.setProperty('display', 'none', 'important');
                    isQuotaExceeded = false;
                }
            }
        })
        .catch(function(err) {
            console.error('Lỗi kiểm tra định biên:', err);
        });
}

// Bắt sự kiện thay đổi kiểm tra định biên
document.getElementById('departmentSelect').addEventListener('change', checkHeadcountBudget);
document.getElementById('headcountInput').addEventListener('input', checkHeadcountBudget);
document.getElementById('minSalaryInput').addEventListener('input', checkHeadcountBudget);
document.getElementById('maxSalaryInput').addEventListener('input', checkHeadcountBudget);
document.getElementById('deadlineInput').addEventListener('change', checkHeadcountBudget);

// Chạy kiểm tra khởi tạo khi tải trang
window.addEventListener('DOMContentLoaded', function() {
    checkSalaryBenchmark();
    checkHeadcountBudget();
});
</script>

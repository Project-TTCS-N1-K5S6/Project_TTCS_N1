<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Cấu hình SLA Tuyển dụng" />
<c:set var="breadcrumb" value="Cấu hình SLA & Nhắc việc" />
<c:set var="activeMenu" value="sla-configs" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Tiêu đề trang & Thanh công cụ hành động -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <div class="d-flex align-items-center gap-2 mb-1">
                    <div class="rounded p-2 text-white" style="background: linear-gradient(135deg, #ef4444, #b91c1c);">
                        <i class="bi bi-stopwatch-fill fs-5"></i>
                    </div>
                    <h3 class="fw-bold text-dark mb-0">Cấu hình Ngưỡng SLA & Nhắc việc Quá hạn</h3>
                </div>
                <p class="text-muted small mb-0 ms-1">
                    Thiết lập thời gian giới hạn xử lý hồ sơ ứng viên theo từng giai đoạn tuyển dụng, tự động phát hiện vi phạm và gửi nhắc việc cho Recruiter mỗi ngày.
                </p>
            </div>
            <div class="d-flex align-items-center gap-2">
                <button type="button" class="btn btn-primary btn-sm px-3 shadow-sm" id="btnScanNow" onclick="triggerSlaScan()">
                    <i class="bi bi-arrow-repeat me-1" id="scanIcon"></i>
                    <span id="scanBtnText">Quét SLA ngay</span>
                </button>
                <a href="${pageContext.request.contextPath}/candidates" class="btn btn-outline-secondary btn-sm px-3">
                    <i class="bi bi-people me-1"></i> Danh sách ứng viên
                </a>
            </div>
        </div>

        <!-- 4 Thẻ KPI thống kê nhanh (RisApps Metric Cards) -->
        <div class="row g-3 mb-4">
            <div class="col-12 col-sm-6 col-xl-3">
                <div class="card border-0 shadow-sm rounded-3 h-100 p-3" style="border-left: 4px solid #3b82f6 !important; background: #ffffff;">
                    <div class="d-flex align-items-center justify-content-between">
                        <div>
                            <span class="text-secondary text-xs text-uppercase fw-semibold">Quy tắc giai đoạn</span>
                            <h3 class="fw-bold text-dark mt-1 mb-0">${stats.activeConfigs} / ${stats.totalConfigs}</h3>
                            <small class="text-muted" style="font-size: 0.75rem;">Đang kích hoạt áp dụng</small>
                        </div>
                        <div class="rounded-circle d-flex align-items-center justify-content-center" style="width: 44px; height: 44px; background: #eff6ff; color: #2563eb;">
                            <i class="bi bi-sliders fs-5"></i>
                        </div>
                    </div>
                </div>
            </div>

            <div class="col-12 col-sm-6 col-xl-3">
                <div class="card border-0 shadow-sm rounded-3 h-100 p-3" style="border-left: 4px solid #ef4444 !important; background: #ffffff;">
                    <div class="d-flex align-items-center justify-content-between">
                        <div>
                            <span class="text-secondary text-xs text-uppercase fw-semibold">Hồ sơ quá hạn SLA</span>
                            <h3 class="fw-bold text-danger mt-1 mb-0" id="statOverdueCount">${stats.overdueCandidatesCount}</h3>
                            <small class="text-muted" style="font-size: 0.75rem;">Cần xử lý chuyển vòng ngay</small>
                        </div>
                        <div class="rounded-circle d-flex align-items-center justify-content-center" style="width: 44px; height: 44px; background: #fef2f2; color: #dc2626;">
                            <i class="bi bi-exclamation-octagon-fill fs-5"></i>
                        </div>
                    </div>
                </div>
            </div>

            <div class="col-12 col-sm-6 col-xl-3">
                <div class="card border-0 shadow-sm rounded-3 h-100 p-3" style="border-left: 4px solid #10b981 !important; background: #ffffff;">
                    <div class="d-flex align-items-center justify-content-between">
                        <div>
                            <span class="text-secondary text-xs text-uppercase fw-semibold">Nhắc việc gửi hôm nay</span>
                            <h3 class="fw-bold text-success mt-1 mb-0" id="statRemindersSent">${stats.remindersSentToday}</h3>
                            <small class="text-muted" style="font-size: 0.75rem;">Chống gửi trùng trong ngày</small>
                        </div>
                        <div class="rounded-circle d-flex align-items-center justify-content-center" style="width: 44px; height: 44px; background: #ecfdf5; color: #059669;">
                            <i class="bi bi-bell-fill fs-5"></i>
                        </div>
                    </div>
                </div>
            </div>

            <div class="col-12 col-sm-6 col-xl-3">
                <div class="card border-0 shadow-sm rounded-3 h-100 p-3" style="border-left: 4px solid #8b5cf6 !important; background: #ffffff;">
                    <div class="d-flex align-items-center justify-content-between">
                        <div>
                            <span class="text-secondary text-xs text-uppercase fw-semibold">Lần quét định kỳ</span>
                            <div class="fw-bold text-dark mt-1" style="font-size: 0.95rem;" id="statLastScan">
                                <c:choose>
                                    <c:when test="${not empty stats.lastScanTimestamp}">
                                        <fmt:formatDate value="${stats.lastScanTimestamp}" pattern="HH:mm dd/MM/yyyy" />
                                    </c:when>
                                    <c:otherwise>Hôm nay (Tự động 24h)</c:otherwise>
                                </c:choose>
                            </div>
                            <small class="text-muted" style="font-size: 0.75rem;">Mỗi ngày 1 lần tự động</small>
                        </div>
                        <div class="rounded-circle d-flex align-items-center justify-content-center" style="width: 44px; height: 44px; background: #f5f3ff; color: #7c3aed;">
                            <i class="bi bi-clock-history fs-5"></i>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Khối Tab Điều Hướng Chính -->
        <div class="card border-0 shadow-sm rounded-3 mb-4">
            <div class="card-header bg-white border-bottom pt-3 pb-0 px-3">
                <ul class="nav nav-tabs card-header-tabs" id="slaMainTabs" role="tablist">
                    <li class="nav-item" role="presentation">
                        <button class="nav-link active fw-semibold" id="tab-config-btn" data-bs-toggle="tab" data-bs-target="#tab-config" type="button" role="tab" aria-selected="true">
                            <i class="bi bi-sliders me-1"></i> Cấu hình ngưỡng SLA các giai đoạn
                            <span class="badge bg-primary-subtle text-primary ms-1">${configs.size()}</span>
                        </button>
                    </li>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link fw-semibold text-danger" id="tab-overdue-btn" data-bs-toggle="tab" data-bs-target="#tab-overdue" type="button" role="tab" aria-selected="false">
                            <i class="bi bi-exclamation-triangle-fill me-1"></i> Hồ sơ đang quá hạn SLA
                            <span class="badge bg-danger ms-1" id="tabOverdueBadge">${overdueCandidates.size()}</span>
                        </button>
                    </li>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link fw-semibold text-secondary" id="tab-logs-btn" data-bs-toggle="tab" data-bs-target="#tab-logs" type="button" role="tab" aria-selected="false">
                            <i class="bi bi-journal-text me-1"></i> Lịch sử cảnh báo &amp; Lần gửi nhắc
                            <span class="badge bg-secondary-subtle text-secondary ms-1">${totalCount}</span>
                        </button>
                    </li>
                </ul>
            </div>

            <div class="card-body p-0">
                <div class="tab-content" id="slaMainTabsContent">
                    
                    <!-- ========================================== -->
                    <!-- TAB 1: CẤU HÌNH NGƯỠNG SLA GIAI ĐOẠN (KN-364, KN-366) -->
                    <!-- ========================================== -->
                    <div class="tab-pane fade show active p-3 p-md-4" id="tab-config" role="tabpanel">
                        <div class="d-flex flex-wrap align-items-center justify-content-between mb-3 gap-2">
                            <div>
                                <h5 class="fw-bold text-dark mb-1 fs-6">Quy định giới hạn thời gian (SLA) theo quy trình tuyển dụng</h5>
                                <p class="text-muted small mb-0">Hồ sơ lưu lại ở từng giai đoạn vượt quá số ngày quy định sẽ được tự động gắn cờ đỏ và kích hoạt nhắc việc.</p>
                            </div>
                        </div>

                        <div class="table-responsive">
                            <table class="table table-hover align-middle mb-0" id="slaConfigTable">
                                <thead class="table-light">
                                    <tr>
                                        <th style="min-width: 180px;">Giai đoạn tuyển dụng</th>
                                        <th style="min-width: 140px;">Ngưỡng tối đa (SLA)</th>
                                        <th style="min-width: 130px;">Cảnh báo cận hạn</th>
                                        <th style="min-width: 250px;">Yêu cầu hành động</th>
                                        <th style="min-width: 120px;">Trạng thái</th>
                                        <th style="min-width: 150px;">Cập nhật lần cuối</th>
                                        <th class="text-end pe-3" style="min-width: 110px;">Thao tác</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="cfg" items="${configs}">
                                        <tr>
                                            <td>
                                                <div class="d-flex align-items-center gap-2">
                                                    <span class="${cfg.stageBadgeClass} px-2 py-1" style="font-size: 0.75rem;">
                                                        ${cfg.stageCode}
                                                    </span>
                                                    <div>
                                                        <div class="fw-bold text-dark">${cfg.stageName}</div>
                                                        <small class="text-muted text-xs">Mã giai đoạn: <code>${cfg.stageCode}</code></small>
                                                    </div>
                                                </div>
                                            </td>
                                            <td>
                                                <span class="badge bg-primary-subtle text-primary border border-primary-subtle fs-6 px-3 py-1 fw-bold">
                                                    <i class="bi bi-clock me-1"></i> ${cfg.slaDays} ngày
                                                </span>
                                            </td>
                                            <td>
                                                <span class="text-secondary small">
                                                    <i class="bi bi-bell me-1 text-warning"></i> Trước <strong>${cfg.warningThresholdDays} ngày</strong>
                                                </span>
                                            </td>
                                            <td>
                                                <span class="text-muted small">${cfg.actionRequired != null ? cfg.actionRequired : 'Chưa có hướng dẫn hành động'}</span>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${cfg.active}">
                                                        <span class="badge bg-success-subtle text-success border border-success-subtle px-2 py-1">
                                                            <i class="bi bi-check-circle-fill me-1"></i> Đang áp dụng
                                                        </span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge bg-secondary-subtle text-secondary border px-2 py-1">
                                                            <i class="bi bi-pause-circle me-1"></i> Tạm dừng
                                                        </span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <div class="small text-secondary">
                                                    <fmt:formatDate value="${cfg.updatedAt}" pattern="dd/MM/yyyy HH:mm" />
                                                </div>
                                                <c:if test="${not empty cfg.updatedByName}">
                                                    <div class="text-xs text-muted">Bởi: ${cfg.updatedByName}</div>
                                                </c:if>
                                            </td>
                                            <td class="text-end pe-3">
                                                <button type="button" class="btn btn-outline-primary btn-sm rounded-pill px-3"
                                                        onclick="openEditSlaModal('${cfg.id}', '${cfg.stageCode}', '${cfg.stageName}', ${cfg.slaDays}, ${cfg.warningThresholdDays}, '${cfg.actionRequired}', ${cfg.active})">
                                                    <i class="bi bi-pencil-square me-1"></i> Sửa
                                                </button>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <!-- ========================================== -->
                    <!-- TAB 2: HỒ SƠ ĐANG QUÁ HẠN SLA (KN-363, KN-365) -->
                    <!-- ========================================== -->
                    <div class="tab-pane fade p-3 p-md-4" id="tab-overdue" role="tabpanel">
                        <div class="d-flex flex-wrap align-items-center justify-content-between mb-3 gap-2">
                            <div>
                                <h5 class="fw-bold text-dark mb-1 fs-6">Danh sách hồ sơ ứng viên vi phạm thời gian SLA</h5>
                                <p class="text-muted small mb-0">Hồ sơ được tự động gắn cờ đỏ khi số ngày ở giai đoạn hiện tại vượt quá ngưỡng SLA quy định.</p>
                            </div>
                            <c:if test="${not empty overdueCandidates}">
                                <span class="badge bg-danger fs-6 px-3 py-1">
                                    <i class="bi bi-exclamation-triangle-fill me-1"></i> Tổng cộng ${overdueCandidates.size()} hồ sơ quá hạn
                                </span>
                            </c:if>
                        </div>

                        <div class="table-responsive">
                            <table class="table table-hover align-middle mb-0" id="overdueCandidatesTable">
                                <thead class="table-light">
                                    <tr>
                                        <th style="min-width: 200px;">Ứng viên</th>
                                        <th style="min-width: 180px;">Vị trí tuyển dụng</th>
                                        <th style="min-width: 140px;">Giai đoạn</th>
                                        <th style="min-width: 160px;">Thời gian lưu lại</th>
                                        <th style="min-width: 140px;">Cờ cảnh báo</th>
                                        <th style="min-width: 180px;">Recruiter phụ trách</th>
                                        <th class="text-end pe-3" style="min-width: 140px;">Hành động</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="c" items="${overdueCandidates}">
                                        <tr>
                                            <td>
                                                <div class="d-flex align-items-center gap-2">
                                                    <div class="user-avatar" style="width: 36px; height: 36px; min-width: 36px; min-height: 36px; border-radius: 50%; background: linear-gradient(135deg, #ef4444, #f97316); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 0.85rem;">
                                                        ${c.fullName != null && c.fullName.length() > 0 ? c.fullName.substring(0, 1) : 'U'}
                                                    </div>
                                                    <div>
                                                        <div class="fw-bold text-dark">${c.fullName}</div>
                                                        <div class="text-muted text-xs">${c.email} • ${c.phone}</div>
                                                    </div>
                                                </div>
                                            </td>
                                            <td>
                                                <span class="fw-semibold text-secondary">${c.requisitionTitle != null ? c.requisitionTitle : 'Chưa gắn vị trí'}</span>
                                            </td>
                                            <td>
                                                <span class="${c.getStatusBadgeClass()} badge" style="font-size: 0.75rem;">
                                                    ${c.getStatusLabel()}
                                                </span>
                                            </td>
                                            <td>
                                                <div>
                                                    <strong class="text-danger fs-6">${c.daysInCurrentStage} ngày</strong>
                                                    <span class="text-muted small">/ Ngưỡng ${c.slaDaysThreshold} ngày</span>
                                                </div>
                                                <small class="text-muted text-xs">
                                                    Vào vòng: <fmt:formatDate value="${c.stageEnteredAt}" pattern="dd/MM/yyyy" />
                                                </small>
                                            </td>
                                            <td>
                                                <span class="badge bg-danger-subtle text-danger border border-danger-subtle px-2 py-1">
                                                    <i class="bi bi-flag-fill me-1"></i> Quá hạn ${c.getOverdueDays()} ngày
                                                </span>
                                                <c:if test="${not empty c.slaOverdueDetectedAt}">
                                                    <div class="text-muted text-xs mt-1" style="font-size: 0.7rem;">
                                                        Phát hiện: <fmt:formatDate value="${c.slaOverdueDetectedAt}" pattern="HH:mm dd/MM" />
                                                    </div>
                                                </c:if>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${not empty c.recruiterName}">
                                                        <div class="fw-semibold text-dark small">${c.recruiterName}</div>
                                                        <div class="text-muted text-xs">${c.recruiterEmail}</div>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="text-muted fst-italic small">Chưa chỉ định</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td class="text-end pe-3">
                                                <button type="button" class="btn btn-outline-danger btn-sm rounded-pill px-3"
                                                        onclick="sendSingleReminder('${c.id}', '${c.fullName}')" title="Gửi nhắc việc cho Recruiter">
                                                    <i class="bi bi-bell-fill me-1"></i> Nhắc việc
                                                </button>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty overdueCandidates}">
                                        <tr>
                                            <td colspan="7" class="text-center py-5 text-muted">
                                                <i class="bi bi-shield-check fs-1 text-success d-block mb-2"></i>
                                                <div class="fw-bold text-dark">Tuyệt vời! Không có hồ sơ nào bị quá hạn SLA.</div>
                                                <small class="text-muted">Tất cả các hồ sơ ứng viên đều đang được xử lý đúng cam kết thời gian.</small>
                                            </td>
                                        </tr>
                                    </c:if>
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <!-- ========================================== -->
                    <!-- TAB 3: LỊCH SỬ CẢNH BÁO & NHẮC VIỆC (KN-367, KN-368) -->
                    <!-- ========================================== -->
                    <div class="tab-pane fade p-3 p-md-4" id="tab-logs" role="tabpanel">
                        <div class="d-flex flex-wrap align-items-center justify-content-between mb-3 gap-2">
                            <div>
                                <h5 class="fw-bold text-dark mb-1 fs-6">Nhật ký cảnh báo SLA &amp; Lần gửi nhắc việc</h5>
                                <p class="text-muted small mb-0">Lưu vết toàn bộ thông báo gửi nhắc việc, thời điểm gửi, người nhận và lịch sử chống gửi trùng trong cùng ngày (KN-367, KN-368).</p>
                            </div>

                            <!-- Bộ lọc tìm kiếm -->
                            <form action="${pageContext.request.contextPath}/admin/sla-configs" method="GET" class="d-flex align-items-center gap-2">
                                <input type="date" name="date" class="form-control form-control-sm" value="${paramDate}" title="Lọc theo ngày" style="width: 140px;" />
                                <div class="ris-search-wrapper" style="min-width: 200px;">
                                    <input type="text" class="ris-search-input form-control-sm" name="search" value="${paramSearch}" placeholder="Tìm ứng viên, recruiter..." />
                                </div>
                                <button type="submit" class="btn btn-outline-secondary btn-sm">
                                    <i class="bi bi-funnel"></i> Lọc
                                </button>
                                <c:if test="${not empty paramSearch || not empty paramDate}">
                                    <a href="${pageContext.request.contextPath}/admin/sla-configs" class="btn btn-link btn-sm text-secondary p-0">Xóa lọc</a>
                                </c:if>
                            </form>
                        </div>

                        <div class="table-responsive">
                            <table class="table table-hover align-middle mb-0" id="slaReminderLogsTable">
                                <thead class="table-light">
                                    <tr>
                                        <th style="min-width: 150px;">Thời điểm</th>
                                        <th style="min-width: 180px;">Ứng viên</th>
                                        <th style="min-width: 160px;">Giai đoạn &amp; Vi phạm</th>
                                        <th style="min-width: 180px;">Recruiter nhận nhắc việc</th>
                                        <th style="min-width: 120px;">Kênh gửi</th>
                                        <th style="min-width: 140px;">Trạng thái</th>
                                        <th style="min-width: 220px;">Chi tiết ghi nhận</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="log" items="${reminderLogs}">
                                        <tr>
                                            <td>
                                                <div class="fw-semibold text-dark small">
                                                    <fmt:formatDate value="${log.sentAt}" pattern="dd/MM/yyyy HH:mm:ss" />
                                                </div>
                                                <div class="text-xs text-muted">Ngày: ${log.sentDate}</div>
                                            </td>
                                            <td>
                                                <div class="fw-bold text-dark">${log.candidateName}</div>
                                                <small class="text-muted text-xs">${log.requisitionTitle != null ? log.requisitionTitle : 'Vị trí tuyển dụng'}</small>
                                            </td>
                                            <td>
                                                <span class="badge bg-light text-dark border">
                                                    ${log.stageName}
                                                </span>
                                                <div class="small text-danger mt-1">
                                                    Đã lưu <strong>${log.daysInStage} ngày</strong> (SLA: ${log.slaDaysThreshold} ngày)
                                                </div>
                                            </td>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${not empty log.recruiterName}">
                                                        <div class="fw-semibold text-dark small">${log.recruiterName}</div>
                                                        <div class="text-muted text-xs">${log.recruiterEmail}</div>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="text-muted small">Ban tuyển dụng</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <span class="badge bg-info-subtle text-info border border-info-subtle px-2 py-1" style="font-size: 0.75rem;">
                                                    <i class="bi bi-envelope-at me-1"></i> App &amp; Email
                                                </span>
                                            </td>
                                            <td>
                                                <span class="${log.getStatusBadgeClass()} px-2 py-1">
                                                    ${log.getStatusLabel()}
                                                </span>
                                            </td>
                                            <td>
                                                <small class="text-muted">${log.message}</small>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty reminderLogs}">
                                        <tr>
                                            <td colspan="7" class="text-center py-5 text-muted">
                                                <i class="bi bi-clock-history fs-2 text-secondary d-block mb-2"></i>
                                                Chưa có lượt gửi nhắc việc nào được ghi nhận trong khoảng thời gian này.
                                            </td>
                                        </tr>
                                    </c:if>
                                </tbody>
                            </table>
                        </div>

                        <!-- Phân trang Log phía Server -->
                        <c:if test="${totalPages > 1}">
                            <div class="p-3 border-top d-flex flex-wrap justify-content-between align-items-center gap-2">
                                <small class="text-muted">Hiển thị trang <strong>${currentPage}</strong> / <strong>${totalPages}</strong> (Tổng cộng <strong>${totalCount}</strong> bản ghi log)</small>
                                <nav>
                                    <ul class="pagination pagination-sm mb-0">
                                        <li class="page-item ${currentPage <= 1 ? 'disabled' : ''}">
                                            <a class="page-link" href="${pageContext.request.contextPath}/admin/sla-configs?page=${currentPage - 1}&search=${paramSearch}&date=${paramDate}">
                                                <i class="bi bi-chevron-left"></i>
                                            </a>
                                        </li>
                                        <c:forEach begin="1" end="${totalPages}" var="p">
                                            <li class="page-item ${currentPage == p ? 'active' : ''}">
                                                <a class="page-link" href="${pageContext.request.contextPath}/admin/sla-configs?page=${p}&search=${paramSearch}&date=${paramDate}">${p}</a>
                                            </li>
                                        </c:forEach>
                                        <li class="page-item ${currentPage >= totalPages ? 'disabled' : ''}">
                                            <a class="page-link" href="${pageContext.request.contextPath}/admin/sla-configs?page=${currentPage + 1}&search=${paramSearch}&date=${paramDate}">
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
        </div>

    </div>
</main>

<!-- Modal Chỉnh Sửa Ngưỡng SLA (KN-366) -->
<div class="modal fade" id="editSlaModal" tabindex="-1" aria-labelledby="editSlaModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow-lg rounded-3">
            <div class="modal-header border-bottom py-3 px-4">
                <div class="d-flex align-items-center gap-2">
                    <div class="rounded-circle p-2 bg-primary-subtle text-primary">
                        <i class="bi bi-sliders fs-5"></i>
                    </div>
                    <h5 class="modal-title fw-bold text-dark mb-0" id="editSlaModalLabel">Chỉnh sửa ngưỡng SLA giai đoạn</h5>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
            </div>
            <form id="editSlaForm" onsubmit="submitSlaConfig(event)">
                <div class="modal-body p-4">
                    <input type="hidden" id="editSlaId" name="id">
                    <input type="hidden" id="editStageCode" name="stageCode">

                    <div class="mb-3">
                        <label class="form-label fw-semibold text-secondary small">Tên giai đoạn tuyển dụng</label>
                        <input type="text" class="form-control" id="editStageName" name="stageName" required readonly style="background-color: #f8fafc;">
                    </div>

                    <div class="row g-3 mb-3">
                        <div class="col-6">
                            <label class="form-label fw-semibold text-dark small">
                                Ngưỡng SLA tối đa (ngày) <span class="text-danger">*</span>
                            </label>
                            <div class="input-group">
                                <input type="number" class="form-control" id="editSlaDays" name="slaDays" min="1" max="90" required>
                                <span class="input-group-text text-muted">ngày</span>
                            </div>
                            <small class="text-muted" style="font-size: 0.72rem;">Số ngày tối đa xử lý hồ sơ</small>
                        </div>
                        <div class="col-6">
                            <label class="form-label fw-semibold text-dark small">
                                Cảnh báo cận hạn (ngày) <span class="text-danger">*</span>
                            </label>
                            <div class="input-group">
                                <input type="number" class="form-control" id="editWarningDays" name="warningThresholdDays" min="0" max="30" required>
                                <span class="input-group-text text-muted">ngày</span>
                            </div>
                            <small class="text-muted" style="font-size: 0.72rem;">Nhắc trước khi đến hạn</small>
                        </div>
                    </div>

                    <div class="mb-3">
                        <label class="form-label fw-semibold text-dark small">Yêu cầu hành động đối với Recruiter</label>
                        <textarea class="form-control" id="editActionRequired" name="actionRequired" rows="3" placeholder="Nhập hành động cần thực hiện khi hồ sơ ở giai đoạn này..."></textarea>
                    </div>

                    <div class="form-check form-switch mt-2">
                        <input class="form-check-input" type="checkbox" id="editIsActive" name="isActive" checked>
                        <label class="form-check-label fw-semibold text-dark" for="editIsActive">
                            Kích hoạt quy tắc SLA cho giai đoạn này
                        </label>
                        <div class="text-muted text-xs">Nếu tắt, hệ thống sẽ không quét hoặc cảnh báo quá hạn cho các hồ sơ ở giai đoạn này.</div>
                    </div>
                </div>
                <div class="modal-footer border-top py-2 px-4 bg-light">
                    <button type="button" class="btn btn-outline-secondary btn-sm px-3" data-bs-dismiss="modal">Hủy bỏ</button>
                    <button type="submit" class="btn btn-primary btn-sm px-4 fw-semibold" id="btnSaveSla">
                        <i class="bi bi-save me-1"></i> Lưu cấu hình
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
// Mở modal chỉnh sửa SLA
function openEditSlaModal(id, stageCode, stageName, slaDays, warningDays, actionRequired, isActive) {
    document.getElementById('editSlaId').value = id;
    document.getElementById('editStageCode').value = stageCode;
    document.getElementById('editStageName').value = stageName;
    document.getElementById('editSlaDays').value = slaDays;
    document.getElementById('editWarningDays').value = warningDays;
    document.getElementById('editActionRequired').value = actionRequired || '';
    document.getElementById('editIsActive').checked = (isActive === true || isActive === 'true');

    var modal = new bootstrap.Modal(document.getElementById('editSlaModal'));
    modal.show();
}

// Lưu cấu hình SLA qua AJAX
function submitSlaConfig(e) {
    e.preventDefault();
    var form = document.getElementById('editSlaForm');
    var formData = new FormData(form);
    var btn = document.getElementById('btnSaveSla');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang lưu...';

    fetch('${pageContext.request.contextPath}/admin/sla-configs/update', {
        method: 'POST',
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        },
        body: new URLSearchParams(formData)
    })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-save me-1"></i> Lưu cấu hình';
        if (data.success) {
            if (window.showToast) {
                window.showToast(data.message, 'success', 'Cấu hình SLA');
            }
            setTimeout(function() {
                window.location.reload();
            }, 600);
        } else {
            if (window.showToast) {
                window.showToast(data.message || 'Lỗi cập nhật', 'error', 'Lỗi');
            } else {
                alert(data.message);
            }
        }
    })
    .catch(function(err) {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-save me-1"></i> Lưu cấu hình';
        if (window.showToast) {
            window.showToast('Lỗi kết nối máy chủ: ' + err, 'error', 'Lỗi hệ thống');
        }
    });
}

// [KN-362]: Kích hoạt quét SLA thủ công ngay lập tức
function triggerSlaScan() {
    var btn = document.getElementById('btnScanNow');
    var icon = document.getElementById('scanIcon');
    var text = document.getElementById('scanBtnText');
    btn.disabled = true;
    icon.className = 'spinner-border spinner-border-sm me-1';
    text.innerText = 'Đang quét hồ sơ...';

    fetch('${pageContext.request.contextPath}/admin/sla-configs/scan-now', {
        method: 'POST',
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        }
    })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        btn.disabled = false;
        icon.className = 'bi bi-arrow-repeat me-1';
        text.innerText = 'Quét SLA ngay';

        if (data.success) {
            if (window.showToast) {
                window.showToast(data.message, 'success', 'Quét SLA thành công');
            } else {
                alert(data.message);
            }
            setTimeout(function() {
                window.location.reload();
            }, 1000);
        } else {
            if (window.showToast) {
                window.showToast(data.message || 'Lỗi khi quét', 'error', 'Lỗi');
            }
        }
    })
    .catch(function(err) {
        btn.disabled = false;
        icon.className = 'bi bi-arrow-repeat me-1';
        text.innerText = 'Quét SLA ngay';
        if (window.showToast) {
            window.showToast('Lỗi khi kích hoạt quét: ' + err, 'error', 'Lỗi');
        }
    });
}

// [KN-365]: Gửi nhắc việc cho 1 ứng viên
function sendSingleReminder(candidateId, candidateName) {
    if (!confirm('Bạn có chắc muốn gửi nhắc việc cho Recruiter phụ trách ứng viên "' + candidateName + '"?')) {
        return;
    }

    var params = new URLSearchParams();
    params.append('candidateId', candidateId);

    fetch('${pageContext.request.contextPath}/admin/sla-configs/send-reminder-now', {
        method: 'POST',
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        },
        body: params
    })
    .then(function(res) { return res.json(); })
    .then(function(data) {
        if (data.success) {
            if (window.showToast) {
                window.showToast(data.message, data.sent ? 'success' : 'warning', 'Nhắc việc SLA');
            } else {
                alert(data.message);
            }
            if (data.sent) {
                setTimeout(function() { window.location.reload(); }, 800);
            }
        } else {
            if (window.showToast) {
                window.showToast(data.message, 'error', 'Lỗi');
            } else {
                alert(data.message);
            }
        }
    })
    .catch(function(err) {
        if (window.showToast) {
            window.showToast('Lỗi gửi nhắc việc: ' + err, 'error', 'Lỗi');
        }
    });
}
</script>

<jsp:include page="../common/footer.jsp" />

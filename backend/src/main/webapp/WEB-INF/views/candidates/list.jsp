<%--
  ==============================================================================
  GIAO DIỆN QUẢN LÝ HỒ SƠ & PIPELINE ỨNG VIÊN (candidates/list.jsp)
  Chức năng:
  - Hiển thị danh sách hồ sơ ứng viên kèm mã tra cứu tracking code, vị trí ứng tuyển, nguồn nộp, trạng thái pipeline.
  - Phân tách dữ liệu: Chuyên viên tuyển dụng (Recruiter) chỉ xem ứng viên các vị trí mình phụ trách (US 5).
  - Tìm kiếm theo từ khóa họ tên/email, lọc theo trạng thái (APPLIED, SCREENING, INTERVIEWING, OFFER, HIRED, REJECTED, WITHDRAWN).
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
            <div class="d-flex align-items-center gap-2">
                <a href="${pageContext.request.contextPath}/candidates/duplicates" class="btn btn-outline-warning position-relative fw-semibold d-flex align-items-center gap-1 shadow-sm" style="border-radius: 8px;">
                    <i class="bi bi-exclamation-triangle-fill text-warning"></i>
                    <span>Hồ sơ nghi trùng</span>
                    <c:if test="${pendingDuplicateCount > 0}">
                        <span class="badge bg-danger rounded-pill ms-1">${pendingDuplicateCount}</span>
                    </c:if>
                </a>
                <button class="btn-ris-primary" data-bs-toggle="modal" data-bs-target="#addCandidateModal">
                    <i class="bi bi-person-plus-fill me-1"></i> Tiếp nhận ứng viên
                </button>
            </div>
        </div>

        <%-- Banner cảnh báo hồ sơ trùng lặp nếu có cảnh báo PENDING --%>
        <c:if test="${pendingDuplicateCount > 0}">
            <div class="alert alert-warning border border-warning-subtle shadow-sm d-flex flex-wrap align-items-center justify-content-between p-3 mb-4 rounded-3" style="background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);">
                <div class="d-flex align-items-center gap-3">
                    <div class="rounded-circle bg-warning bg-opacity-25 d-flex align-items-center justify-content-center flex-shrink-0" style="width: 42px; height: 42px;">
                        <i class="bi bi-exclamation-diamond-fill text-warning fs-5"></i>
                    </div>
                    <div>
                        <div class="fw-bold text-dark mb-0">Hệ thống phát hiện ${pendingDuplicateCount} cặp hồ sơ có nguy cơ trùng lặp!</div>
                        <div class="text-secondary small">Các ứng viên có trùng số điện thoại hoặc email liên hệ. Vui lòng xem xét đối chiếu và hợp nhất.</div>
                    </div>
                </div>
                <a href="${pageContext.request.contextPath}/candidates/duplicates" class="btn btn-warning btn-sm fw-bold px-3 py-2 text-dark shadow-sm rounded-pill text-nowrap mt-2 mt-md-0">
                    <i class="bi bi-arrow-right-circle me-1"></i> Xử lý hồ sơ trùng ngay
                </a>
            </div>
        </c:if>

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
                        <i class="bi bi-info-circle me-1"></i>Phạm vi hiển thị bị giới hạn
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
                                <c:when test="${paramStatus == 'WITHDRAWN'}">Ứng viên rút hồ sơ</c:when>
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
                            <li><a class="dropdown-item py-1" href="${pageContext.request.contextPath}/candidates?status=WITHDRAWN">Ứng viên rút hồ sơ</a></li>
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
                                <th class="ps-3" style="min-width: 180px; width: 22%;">Họ và tên</th>
                                <th style="min-width: 120px; width: 14%;">Phòng ban</th>
                                <th style="min-width: 170px; width: 18%;">Vị trí ứng tuyển &amp; CV</th>
                                <th style="min-width: 130px; width: 14%;">Nguồn / Giới thiệu</th>
                                <th style="min-width: 105px; width: 10%;">Thời gian</th>
                                <th style="min-width: 120px; width: 12%;">Trạng thái &amp; SLA</th>
                                <th class="text-end pe-3" style="min-width: 195px; width: 12%;">Thao tác &amp; Vòng tuyển</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="c" items="${candidates}">
                                <tr>
                                    <td class="ps-3">
                                        <div class="d-flex align-items-center gap-2">
                                            <div class="user-avatar" style="width: 32px; height: 32px; min-width: 32px; min-height: 32px; border-radius: 50%; background: linear-gradient(135deg, #2563eb, #8b5cf6); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 0.8rem; flex-shrink: 0;">
                                                ${c.fullName != null && c.fullName.length() > 0 ? c.fullName.substring(0, 1) : 'U'}
                                            </div>
                                            <div style="min-width: 0;">
                                                <a href="${pageContext.request.contextPath}/candidates/profile?id=${c.id}"
                                                   class="fw-bold text-dark text-decoration-none d-block text-nowrap"
                                                   title="Xem hồ sơ hợp nhất của ${c.fullName}"
                                                   style="font-size: 0.865rem; transition: color .15s;"
                                                   onmouseover="this.style.color='#4f46e5'" onmouseout="this.style.color=''">
                                                    ${c.fullName}
                                                </a>
                                                <div class="text-muted text-xs text-nowrap" style="max-width: 170px; overflow: hidden; text-overflow: ellipsis;" title="${c.email}">${c.email}</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <span class="text-dark fw-semibold" style="font-size: 0.825rem;">${c.departmentName != null ? c.departmentName : 'Đang cập nhật'}</span>
                                    </td>
                                    <td>
                                        <div class="text-secondary fw-medium" style="font-size: 0.825rem;">${c.requisitionTitle != null ? c.requisitionTitle : 'Chưa gắn vị trí'}</div>
                                        <c:if test="${not empty c.cvUrl}">
                                            <div class="d-flex align-items-center gap-1 mt-1 flex-wrap">
                                                <a href="${pageContext.request.contextPath}/candidates/cv-preview?id=${c.id}" target="_blank" class="text-primary text-xs text-decoration-none fw-semibold">
                                                    <i class="bi bi-file-earmark-pdf"></i> Xem CV
                                                </a>
                                                <c:choose>
                                                    <c:when test="${c.extractionStatus == 'CONFIRMED'}">
                                                        <span class="badge bg-success-subtle text-success border border-success-subtle" style="font-size: 0.68rem;" title="Recruiter đã đối chiếu & cập nhật thông tin vào hồ sơ">
                                                            <i class="bi bi-check-circle-fill me-1"></i>Đã đối chiếu
                                                        </span>
                                                    </c:when>
                                                    <c:when test="${c.extractionStatus == 'SUCCESS'}">
                                                        <span class="badge bg-info-subtle text-info border border-info-subtle" style="font-size: 0.68rem;" title="Hệ thống đã tự động bóc tách từ PDF">
                                                            <i class="bi bi-magic me-1"></i>Đã bóc tách CV
                                                        </span>
                                                    </c:when>
                                                    <c:when test="${c.extractionStatus == 'PARTIAL'}">
                                                        <span class="badge bg-warning-subtle text-warning border border-warning-subtle" style="font-size: 0.68rem;" title="Bóc tách được một phần thông tin">
                                                            <i class="bi bi-exclamation-circle me-1"></i>Bóc tách 1 phần
                                                        </span>
                                                    </c:when>
                                                    <c:when test="${c.extractionStatus == 'FAILED'}">
                                                        <span class="badge bg-danger-subtle text-danger border border-danger-subtle" style="font-size: 0.68rem;" title="Không thể bóc tách ký tự từ PDF này">
                                                            <i class="bi bi-x-circle me-1"></i>Lỗi bóc tách
                                                        </span>
                                                    </c:when>
                                                </c:choose>
                                            </div>
                                        </c:if>
                                    </td>
                                    <td>
                                        <div class="text-dark" style="font-size: 0.825rem;"><c:out value="${not empty c.applicationSourceName ? c.applicationSourceName : 'Chưa xác định'}" /></div>
                                        <c:if test="${not empty c.referredByUserName}">
                                            <div class="text-muted text-xs">Người giới thiệu: <c:out value="${c.referredByUserName}" /></div>
                                        </c:if>
                                    </td>
                                    <td>
                                        <div class="text-secondary font-monospace" style="font-size: 0.785rem;">
                                            <fmt:formatDate value="${c.createdAt}" pattern="dd/MM/yyyy HH:mm" />
                                        </div>
                                    </td>
                                    <td>
                                        <div class="d-flex flex-column gap-1">
                                            <span class="badge-pill-status ${c.status == 'HIRED' ? 'success' : (c.status == 'WITHDRAWN' ? 'secondary' : (c.status == 'REJECTED' ? 'danger' : (c.status == 'OFFER' ? 'info' : 'warning')))}">
                                                ${c.getStatusLabel()}
                                            </span>
                                            <c:if test="${c.slaOverdue}">
                                                <span class="badge bg-danger-subtle text-danger border border-danger-subtle d-inline-flex align-items-center gap-1 px-2 py-1" style="font-size: 0.72rem; width: fit-content;" title="Hồ sơ đã ở giai đoạn ${c.daysInCurrentStage} ngày (Ngưỡng SLA: ${c.slaDaysThreshold} ngày)">
                                                    <i class="bi bi-flag-fill text-danger"></i> Quá hạn SLA (${c.daysInCurrentStage}/${c.slaDaysThreshold} ngày)
                                                </span>
                                            </c:if>
                                        </div>
                                    </td>
                                    <td class="text-end pe-3">
                                        <div class="d-inline-flex align-items-center gap-1.5 justify-content-end">
                                            <c:if test="${not empty c.cvUrl}">
                                                <button type="button" class="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-1 py-1 px-2"
                                                        style="font-size: 0.72rem; border-radius: 6px; white-space: nowrap;"
                                                        data-bs-toggle="modal" data-bs-target="#reviewCVExtractionModal"
                                                        data-id="${c.id}"
                                                        data-fullname="<c:out value='${c.fullName}' />"
                                                        data-email="<c:out value='${c.email}' />"
                                                        data-phone="<c:out value='${c.phone}' />"
                                                        data-cvurl="<c:out value='${c.cvUrl}' />"
                                                        data-extracted-name="<c:out value='${c.extractedName}' />"
                                                        data-extracted-email="<c:out value='${c.extractedEmail}' />"
                                                        data-extracted-phone="<c:out value='${c.extractedPhone}' />"
                                                        data-status="<c:out value='${c.extractionStatus}' />"
                                                        data-status-label="<c:out value='${c.getExtractionStatusLabel()}' />"
                                                        data-badge-class="<c:out value='${c.getExtractionStatusBadgeClass()}' />"
                                                        onclick="populateExtractionModal(this)"
                                                        title="Đối chiếu thông tin trích xuất từ CV">
                                                    <i class="bi bi-file-earmark-diff"></i> Đối chiếu CV
                                                </button>
                                            </c:if>
                                            <form action="${pageContext.request.contextPath}/candidates/update-status" method="POST"
                                                  class="candidate-status-form d-inline-flex flex-column gap-1 align-items-end"
                                                  data-current-status="${c.status}" data-current-reason="${c.rejectionReasonId}">
                                                <input type="hidden" name="candidateId" value="${c.id}">
                                                <select class="form-select form-select-sm" name="status" onchange="toggleCandidateStatusForm(this)" style="width: 118px; font-size: 0.78rem; border-radius: 6px; padding: 0.25rem 0.45rem;">
                                                    <option value="APPLIED" ${c.status == 'APPLIED' ? 'selected' : ''}>Mới ứng tuyển</option>
                                                    <option value="SCREENING" ${c.status == 'SCREENING' ? 'selected' : ''}>Sàng lọc CV</option>
                                                    <option value="INTERVIEWING" ${c.status == 'INTERVIEWING' ? 'selected' : ''}>Phỏng vấn</option>
                                                    <option value="OFFER" ${c.status == 'OFFER' ? 'selected' : ''}>Gửi Offer</option>
                                                    <option value="HIRED" ${c.status == 'HIRED' ? 'selected' : ''}>Trúng tuyển</option>
                                                    <option value="REJECTED" ${c.status == 'REJECTED' ? 'selected' : ''}>Từ chối</option>
                                                </select>
                                                <div class="rejection-reason-field ${c.status == 'REJECTED' ? '' : 'd-none'}">
                                                    <select class="form-select form-select-sm" name="rejectionReasonId"
                                                            aria-label="Lý do loại hồ sơ" onchange="toggleCandidateStatusForm(this)" style="width: 118px; font-size: 0.72rem; padding: 0.2rem 0.4rem;">
                                                        <option value="">Lý do loại</option>
                                                        <c:forEach var="reason" items="${rejectionReasons}">
                                                            <option value="${reason.id}" ${c.rejectionReasonId == reason.id ? 'selected' : ''}>
                                                                <c:out value="${reason.value}" />
                                                            </option>
                                                        </c:forEach>
                                                    </select>
                                                </div>
                                                <button type="submit" class="btn btn-sm btn-primary candidate-status-submit py-0 px-2" style="font-size: 0.72rem;" hidden>
                                                    Lưu
                                                </button>
                                            </form>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty candidates}">
                                <tr>
                                    <td colspan="7" class="text-center py-5 text-muted">
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

    <!-- Modal Thêm Ứng Viên Thủ Công (KN-288) -->
    <div class="modal fade" id="addCandidateModal" tabindex="-1" aria-labelledby="addCandidateModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content shadow-lg border-0" style="border-radius: 12px; overflow: hidden;">
                <form id="manualCandidateForm" action="${pageContext.request.contextPath}/candidates/create" method="POST" enctype="multipart/form-data">
                    <input type="hidden" name="duplicateAction" id="manualDuplicateAction" value="">
                    
                    <div class="modal-header bg-light border-bottom py-3 px-4">
                        <div class="d-flex align-items-center gap-2">
                            <div class="rounded-circle bg-primary bg-opacity-10 d-flex align-items-center justify-content-center" style="width: 38px; height: 38px;">
                                <i class="bi bi-person-plus-fill text-primary fs-5"></i>
                            </div>
                            <div>
                                <h5 class="modal-title fw-bold text-dark mb-0" id="addCandidateModalLabel">
                                    Thêm ứng viên thủ công
                                </h5>
                                <small class="text-muted">Nhập tay thông tin và đính kèm CV (tự động phát hiện trùng lặp theo email &amp; SĐT)</small>
                            </div>
                        </div>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>

                    <div class="modal-body p-4">
                        <!-- Khung Cảnh báo Trùng lặp (KN-346, KN-351, KN-352) -->
                        <div id="manualDupAlert" class="alert alert-warning border border-warning shadow-sm mb-4 d-none" style="border-radius: 10px; background-color: #fff9ea;">
                            <div class="d-flex align-items-start gap-2 mb-2">
                                <i class="bi bi-exclamation-triangle-fill text-warning fs-5 flex-shrink-0 mt-1"></i>
                                <div>
                                    <h6 class="fw-bold text-dark mb-1">Phát hiện hồ sơ có khả năng trùng lặp!</h6>
                                    <p class="text-muted small mb-2" id="manualDupMsg">
                                        Email hoặc số điện thoại của ứng viên này trùng khớp với hồ sơ hiện có trong hệ thống:
                                    </p>
                                </div>
                            </div>
                            <div id="manualDupList" class="d-flex flex-column gap-2 mb-3"></div>
                            <div class="d-flex justify-content-end align-items-center gap-2 pt-2 border-top border-warning-subtle">
                                <button type="button" class="btn btn-outline-secondary btn-sm px-3" id="btnCancelDup">
                                    <i class="bi bi-pencil-square me-1"></i> Chỉnh sửa lại thông tin
                                </button>
                                <button type="button" class="btn btn-warning btn-sm px-3 fw-bold text-dark shadow-sm" id="btnOverrideDup">
                                    <i class="bi bi-check2-circle me-1"></i> Vẫn tạo hồ sơ mới
                                </button>
                            </div>
                        </div>

                        <!-- Khung Thông báo Lỗi chung -->
                        <div id="manualErrorAlert" class="alert alert-danger border border-danger-subtle shadow-sm mb-3 d-none" style="border-radius: 8px;">
                            <i class="bi bi-x-circle-fill me-1"></i> <span id="manualErrorMsg"></span>
                        </div>

                        <div class="row g-3">
                            <div class="col-12">
                                <label class="form-label small fw-semibold text-dark">Họ và tên ứng viên <span class="text-danger">*</span></label>
                                <div class="input-group">
                                    <span class="input-group-text bg-white"><i class="bi bi-person text-muted"></i></span>
                                    <input type="text" class="form-control" name="fullName" id="manualFullName" required maxlength="120" placeholder="VD: Nguyễn Văn A">
                                </div>
                            </div>
                            <div class="col-12 col-sm-6">
                                <label class="form-label small fw-semibold text-dark">Email liên hệ <span class="text-danger">*</span></label>
                                <div class="input-group">
                                    <span class="input-group-text bg-white"><i class="bi bi-envelope text-muted"></i></span>
                                    <input type="email" class="form-control" name="email" id="manualEmail" required maxlength="254" placeholder="candidate@email.com">
                                </div>
                            </div>
                            <div class="col-12 col-sm-6">
                                <label class="form-label small fw-semibold text-dark">Số điện thoại <span class="text-danger">*</span></label>
                                <div class="input-group">
                                    <span class="input-group-text bg-white"><i class="bi bi-telephone text-muted"></i></span>
                                    <input type="tel" class="form-control" name="phone" id="manualPhone" required maxlength="30" placeholder="0901234567">
                                </div>
                            </div>
                            
                            <!-- Container hiển thị cảnh báo nghi trùng thời gian thực -->
                            <div class="col-12" id="addCandDuplicateAlert" style="display: none;">
                                <div class="alert alert-warning border border-warning-subtle py-2 px-3 mb-0" style="background-color: #fffbeb; border-radius: 8px;">
                                    <div class="d-flex align-items-center gap-2 mb-1">
                                        <i class="bi bi-exclamation-triangle-fill text-warning fs-6"></i>
                                        <strong class="text-dark small" id="dupAlertTitle">Phát hiện hồ sơ nghi trùng khớp!</strong>
                                    </div>
                                    <div class="small text-secondary mb-2" id="dupAlertDesc">
                                        Email hoặc số điện thoại này đã tồn tại trong hệ thống.
                                    </div>
                                    <div id="dupMatchesList" class="d-flex flex-column gap-1"></div>
                                </div>
                            </div>
                            <div class="col-12 col-sm-6">
                                <label class="form-label small fw-semibold text-dark">Vị trí tuyển dụng</label>
                                <select class="form-select" name="requisitionId" id="manualRequisitionId">
                                    <option value="">-- Chưa gán vị trí tuyển dụng --</option>
                                    <c:forEach var="req" items="${requisitions}">
                                        <option value="${req.id}"><c:out value="${req.title}" /> (<c:out value="${req.code}" />)</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-12 col-sm-6">
                                <label class="form-label small fw-semibold text-dark">Nguồn ứng viên</label>
                                <select class="form-select" name="applicationSourceId" id="manualSourceId">
                                    <option value="">Chưa xác định</option>
                                    <c:forEach var="source" items="${applicationSources}">
                                        <option value="${source.id}"><c:out value="${source.value}" /></option>
                                    </c:forEach>
                                </select>
                            </div>

                            <!-- Khu vực Đính kèm & Xem trước CV (KN-349, KN-350) -->
                            <div class="col-12">
                                <label class="form-label small fw-semibold text-dark d-flex justify-content-between align-items-center mb-1">
                                    <span><i class="bi bi-file-earmark-arrow-up text-primary me-1"></i> Tệp CV ứng viên (.pdf, .doc, .docx - Tối đa 5MB)</span>
                                    <span class="text-muted text-xs">Không bắt buộc</span>
                                </label>
                                <input type="file" id="manualCvFileInput" name="cv" accept=".pdf,.doc,.docx" class="d-none">
                                <div id="manualCvDropzone" class="border border-2 border-dashed rounded p-3 text-center bg-light" style="cursor: pointer; transition: all 0.2s;">
                                    <div class="text-primary fs-3 mb-1"><i class="bi bi-cloud-arrow-up"></i></div>
                                    <div class="fw-semibold small text-dark" id="manualCvDropzoneText">Kéo thả CV vào đây hoặc bấm để chọn tệp</div>
                                    <div class="text-muted text-xs mt-1">Chấp nhận tệp .pdf, .doc, .docx dung lượng tối đa 5MB</div>
                                </div>

                                <!-- Khung Xem trước CV PDF trực tiếp (KN-349) -->
                                <div id="manualCvPreviewContainer" class="card mt-2 border border-info-subtle shadow-sm d-none" style="border-radius: 8px; overflow: hidden;">
                                    <div class="card-header bg-info-subtle py-2 px-3 d-flex justify-content-between align-items-center">
                                        <div class="small fw-semibold text-info-emphasis d-flex align-items-center gap-1">
                                            <i class="bi bi-file-earmark-pdf-fill text-danger fs-6"></i>
                                            <span id="manualCvPreviewName" class="text-truncate" style="max-width: 400px;">cv.pdf</span>
                                        </div>
                                        <button type="button" class="btn btn-outline-danger btn-sm py-0 px-2" style="font-size: 0.75rem;" id="btnRemoveCv">
                                            <i class="bi bi-trash me-1"></i> Bỏ tệp
                                        </button>
                                    </div>
                                    <div class="card-body p-0" id="manualCvPreviewBody">
                                        <iframe id="manualCvPreviewIframe" src="" style="width: 100%; height: 320px; border: none;"></iframe>
                                        <div id="manualCvPreviewDocNote" class="p-3 text-center text-muted small d-none">
                                            <i class="bi bi-info-circle text-primary me-1"></i> Trình duyệt không hỗ trợ xem trước tệp Word trực tiếp. Tệp sẽ được tải lên và lưu trữ an toàn trên máy chủ.
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <div class="col-12 col-sm-6">
                                <label class="form-label small fw-semibold text-dark">Trạng thái khởi tạo</label>
                                <select class="form-select" name="status" id="manualStatus">
                                    <option value="APPLIED">Mới ứng tuyển</option>
                                    <option value="SCREENING">Sàng lọc CV</option>
                                    <option value="INTERVIEWING">Phỏng vấn</option>
                                </select>
                            </div>
                            <div class="col-12 col-sm-6">
                                <label class="form-label small fw-semibold text-dark">Đường dẫn CV online (tùy chọn thay thế)</label>
                                <input type="url" class="form-control" name="cvUrl" id="manualCvUrl" placeholder="https://storage.cloud/cv.pdf">
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold text-dark">Ghi chú ban đầu</label>
                                <textarea class="form-control" name="notes" id="manualNotes" rows="2" placeholder="Ghi chú kinh nghiệm, nguồn ứng viên cụ thể, kỹ năng nổi bật..."></textarea>
                            </div>
                        </div>
                    </div>

                    <div class="modal-footer bg-light border-top py-2 px-4 d-flex justify-content-between">
                        <button type="button" class="btn btn-secondary btn-sm px-3" data-bs-dismiss="modal">Đóng</button>
                        <button type="submit" class="btn btn-primary btn-sm px-4 fw-semibold" id="btnSubmitManualCand">
                            <i class="bi bi-check-lg me-1"></i> Kiểm tra trùng &amp; Lưu hồ sơ
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>


    <!-- Modal Đối Chiếu & Xác Nhận Thông Tin Bóc Tách từ CV (KN-213) -->
    <div class="modal fade" id="reviewCVExtractionModal" tabindex="-1" aria-labelledby="reviewCVExtractionModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-xl">
            <div class="modal-content shadow-lg border-0" style="border-radius: 12px; overflow: hidden;">
                <div class="modal-header bg-light border-bottom py-3 px-4">
                    <div class="d-flex align-items-center gap-2">
                        <div class="rounded-circle bg-primary bg-opacity-10 d-flex align-items-center justify-content-center" style="width: 38px; height: 38px;">
                            <i class="bi bi-magic text-primary fs-5"></i>
                        </div>
                        <div>
                            <h5 class="modal-title fw-bold text-dark mb-0" id="reviewCVExtractionModalLabel">
                                Đối chiếu &amp; Xác nhận thông tin bóc tách từ CV
                            </h5>
                            <small class="text-muted">Tính năng tự động bóc tách thông tin từ CV PDF</small>
                        </div>
                    </div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <!-- Banner hướng dẫn & Quy tắc nghiệp vụ -->
                    <div class="alert alert-primary d-flex align-items-start gap-3 border border-primary-subtle rounded-3 py-2 px-3 mb-4" role="alert" style="background-color: #f0f7ff;">
                        <i class="bi bi-info-circle-fill text-primary fs-5 mt-1"></i>
                        <div class="small">
                            <strong>Nguyên tắc bảo vệ dữ liệu:</strong> Kết quả bóc tách tự động chỉ mang tính chất hỗ trợ và <strong>không tự ý ghi đè</strong> thông tin ứng viên đã nộp. Recruiter có thể đối chiếu, trực tiếp chỉnh sửa các trường trước khi xác nhận lưu vào hồ sơ chính thức. Bóc tách thất bại không ảnh hưởng đến hồ sơ nộp.
                        </div>
                    </div>

                    <div class="row g-4">
                        <!-- Cột 1: Thông tin ứng viên nộp ban đầu (Readonly) -->
                        <div class="col-12 col-lg-5">
                            <div class="card h-100 border shadow-none" style="background-color: #f8fafc; border-color: #e2e8f0 !important; border-radius: 10px;">
                                <div class="card-header bg-white border-bottom py-2 px-3 d-flex align-items-center justify-content-between">
                                    <span class="fw-bold text-secondary small text-uppercase">
                                        <i class="bi bi-person-fill me-1"></i> 1. Thông tin ban đầu (Gốc)
                                    </span>
                                    <span class="badge bg-secondary-subtle text-secondary border">Tham chiếu</span>
                                </div>
                                <div class="card-body p-3">
                                    <div class="mb-3">
                                        <label class="form-label text-muted small fw-semibold mb-1">Họ và tên ứng viên nộp</label>
                                        <input type="text" id="origFullName" class="form-control form-control-sm bg-white" readonly>
                                    </div>
                                    <div class="mb-3">
                                        <label class="form-label text-muted small fw-semibold mb-1">Email liên hệ nộp</label>
                                        <input type="text" id="origEmail" class="form-control form-control-sm bg-white" readonly>
                                    </div>
                                    <div class="mb-3">
                                        <label class="form-label text-muted small fw-semibold mb-1">Số điện thoại nộp</label>
                                        <input type="text" id="origPhone" class="form-control form-control-sm bg-white" readonly>
                                    </div>
                                    <div class="pt-2 border-top">
                                        <label class="form-label text-muted small fw-semibold mb-1">Tệp CV ứng tuyển</label>
                                        <a id="origCvLink" href="#" target="_blank" class="btn btn-sm btn-outline-secondary w-100 d-flex align-items-center justify-content-center gap-1">
                                            <i class="bi bi-file-earmark-pdf text-danger"></i> Mở xem tệp PDF gốc trong tab mới
                                        </a>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Cột 2: Thông tin bóc tách từ CV & Cho phép Recruiter sửa/xác nhận -->
                        <div class="col-12 col-lg-7">
                            <div class="card h-100 border shadow-none" style="background-color: #f0fdf4; border-color: #86efac !important; border-radius: 10px;">
                                <div class="card-header bg-white border-bottom py-2 px-3 d-flex align-items-center justify-content-between">
                                    <span class="fw-bold text-success small text-uppercase">
                                        <i class="bi bi-magic me-1"></i> 2. Thông tin bóc tách từ CV (Có thể sửa)
                                    </span>
                                    <span id="modalExtractionBadge" class="badge bg-info-subtle text-info border">Đang tải...</span>
                                </div>
                                <div class="card-body p-3">
                                    <form id="confirmExtractionForm" action="${pageContext.request.contextPath}/candidates/confirm-extraction" method="POST">
                                        <input type="hidden" name="candidateId" id="modalCandidateId">

                                        <div class="mb-3">
                                            <label class="form-label text-dark small fw-bold mb-1">
                                                Họ và tên trích xuất <span class="text-danger">*</span>
                                            </label>
                                            <div class="input-group input-group-sm">
                                                <span class="input-group-text bg-white"><i class="bi bi-person text-success"></i></span>
                                                <input type="text" class="form-control" name="confirmedName" id="confirmedName" required placeholder="Họ và tên ứng viên từ CV">
                                            </div>
                                            <div class="form-text text-xs text-muted">Recruiter có thể hiệu chỉnh lại dấu tiếng Việt hoặc cách viết hoa.</div>
                                        </div>

                                        <div class="mb-3">
                                            <label class="form-label text-dark small fw-bold mb-1">
                                                Email trích xuất <span class="text-danger">*</span>
                                            </label>
                                            <div class="input-group input-group-sm">
                                                <span class="input-group-text bg-white"><i class="bi bi-envelope text-success"></i></span>
                                                <input type="email" class="form-control" name="confirmedEmail" id="confirmedEmail" required placeholder="email@domain.com">
                                            </div>
                                            <div class="form-text text-xs text-muted">Hệ thống trích xuất theo định dạng email chuẩn từ nội dung văn bản PDF.</div>
                                        </div>

                                        <div class="mb-3">
                                            <label class="form-label text-dark small fw-bold mb-1">
                                                Số điện thoại trích xuất <span class="text-danger">*</span>
                                            </label>
                                            <div class="input-group input-group-sm">
                                                <span class="input-group-text bg-white"><i class="bi bi-telephone text-success"></i></span>
                                                <input type="tel" class="form-control" name="confirmedPhone" id="confirmedPhone" required placeholder="0912345678">
                                            </div>
                                            <div class="form-text text-xs text-muted">Định dạng số di động Việt Nam chuẩn (đầu 03, 05, 07, 08, 09 hoặc +84).</div>
                                        </div>

                                        <div class="alert alert-light border small text-muted py-2 px-3 mb-0" style="background: #ffffff; border-radius: 8px;">
                                            <i class="bi bi-shield-check text-success me-1"></i>
                                            Nhấn <strong>"Xác nhận & Cập nhật"</strong> sẽ lưu thông tin đã duyệt ở cột này vào hồ sơ ứng viên và chuyển trạng thái thành <code>Đã đối chiếu</code>.
                                        </div>
                                    </form>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer bg-light border-top py-2 px-4 d-flex justify-content-between">
                    <div>
                        <!-- Nút Bóc tách lại CV -->
                        <form id="reExtractForm" action="${pageContext.request.contextPath}/candidates/re-extract" method="POST" class="d-inline">
                            <input type="hidden" name="candidateId" id="reExtractCandidateId">
                            <button type="submit" class="btn btn-sm btn-outline-secondary" onclick="return confirm('Bạn có muốn hệ thống quét và bóc tách lại nội dung từ tệp CV này?');">
                                <i class="bi bi-arrow-repeat me-1"></i> Bóc tách lại CV
                            </button>
                        </form>
                    </div>
                    <div class="d-flex align-items-center gap-2">
                        <button type="button" class="btn btn-sm btn-outline-secondary px-3" data-bs-dismiss="modal">
                            Đóng (Giữ nguyên)
                        </button>
                        <button type="submit" form="confirmExtractionForm" class="btn btn-sm btn-primary px-3">
                            <i class="bi bi-check-circle-fill me-1"></i> Xác nhận &amp; Cập nhật vào hồ sơ
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>

<script>
function populateExtractionModal(button) {
    var id = button.getAttribute('data-id') || '';
    var fullName = button.getAttribute('data-fullname') || '';
    var email = button.getAttribute('data-email') || '';
    var phone = button.getAttribute('data-phone') || '';
    var cvUrl = button.getAttribute('data-cvurl') || '';
    var extractedName = button.getAttribute('data-extracted-name') || '';
    var extractedEmail = button.getAttribute('data-extracted-email') || '';
    var extractedPhone = button.getAttribute('data-extracted-phone') || '';
    var status = button.getAttribute('data-status') || '';
    var statusLabel = button.getAttribute('data-status-label') || 'Chưa bóc tách';
    var badgeClass = button.getAttribute('data-badge-class') || 'bg-secondary';

    document.getElementById('modalCandidateId').value = id;
    document.getElementById('reExtractCandidateId').value = id;

    // Gốc
    document.getElementById('origFullName').value = fullName;
    document.getElementById('origEmail').value = email;
    document.getElementById('origPhone').value = phone;

    var cvLink = document.getElementById('origCvLink');
    if (cvUrl) {
        cvLink.href = cvUrl;
        cvLink.classList.remove('disabled');
    } else {
        cvLink.href = '#';
        cvLink.classList.add('disabled');
    }

    // Giá trị bóc tách hiển thị ở form để recruiter xác nhận / sửa
    document.getElementById('confirmedName').value = extractedName ? extractedName : fullName;
    document.getElementById('confirmedEmail').value = extractedEmail ? extractedEmail : email;
    document.getElementById('confirmedPhone').value = extractedPhone ? extractedPhone : phone;

    var badgeElem = document.getElementById('modalExtractionBadge');
    badgeElem.className = 'badge ' + badgeClass;
    badgeElem.textContent = statusLabel;
}

function toggleCandidateStatusForm(control) {
    var form = control.closest('.candidate-status-form');
    if (!form) return;
    var status = form.querySelector('[name="status"]');
    var reasonBox = form.querySelector('.rejection-reason-field');
    var reason = form.querySelector('[name="rejectionReasonId"]');
    var submit = form.querySelector('.candidate-status-submit');
    var rejected = status.value === 'REJECTED';

    if (reasonBox) reasonBox.classList.toggle('d-none', !rejected);
    if (reason) {
        reason.required = rejected;
        if (!rejected) {
            reason.value = '';
        }
    }
    if (submit) {
        submit.hidden = status.value === form.dataset.currentStatus
            && (!rejected || reason.value === form.dataset.currentReason);
    }
}

document.querySelectorAll('.candidate-status-form').forEach(function (form) {
    toggleCandidateStatusForm(form.querySelector('[name="status"]'));
});

// =========================================================================
// [KN-288]: XỬ LÝ GIAO DIỆN THÊM ỨNG VIÊN THỦ CÔNG & XEM TRƯỚC CV & TRÙNG LẶP
// =========================================================================
(function () {
    var fileInput = document.getElementById('manualCvFileInput');
    var dropzone = document.getElementById('manualCvDropzone');
    var dropzoneText = document.getElementById('manualCvDropzoneText');
    var previewContainer = document.getElementById('manualCvPreviewContainer');
    var previewName = document.getElementById('manualCvPreviewName');
    var previewIframe = document.getElementById('manualCvPreviewIframe');
    var previewDocNote = document.getElementById('manualCvPreviewDocNote');
    var btnRemoveCv = document.getElementById('btnRemoveCv');
    var previewObjectUrl = null;

    var dupAlert = document.getElementById('manualDupAlert');
    var dupMsg = document.getElementById('manualDupMsg');
    var dupList = document.getElementById('manualDupList');
    var btnOverrideDup = document.getElementById('btnOverrideDup');
    var btnCancelDup = document.getElementById('btnCancelDup');

    var errorAlert = document.getElementById('manualErrorAlert');
    var errorMsg = document.getElementById('manualErrorMsg');

    var form = document.getElementById('manualCandidateForm');
    var btnSubmit = document.getElementById('btnSubmitManualCand');
    var dupActionInput = document.getElementById('manualDuplicateAction');

    if (!form) return;

    function resetCvPreview() {
        if (previewObjectUrl) {
            URL.revokeObjectURL(previewObjectUrl);
            previewObjectUrl = null;
        }
        if (previewIframe) previewIframe.src = '';
        if (previewContainer) previewContainer.classList.add('d-none');
        if (previewDocNote) previewDocNote.classList.add('d-none');
        if (dropzoneText) dropzoneText.textContent = 'Kéo thả CV vào đây hoặc bấm để chọn tệp';
        if (fileInput) fileInput.value = '';
    }

    function handleFileSelected(file) {
        if (!file) {
            resetCvPreview();
            return;
        }
        var ext = file.name.slice(file.name.lastIndexOf('.')).toLowerCase();
        var allowed = ['.pdf', '.doc', '.docx'];
        if (allowed.indexOf(ext) === -1) {
            alert('Định dạng tệp không được hỗ trợ. Vui lòng chọn tệp .pdf, .doc hoặc .docx');
            resetCvPreview();
            return;
        }
        if (file.size > 5 * 1024 * 1024) {
            alert('Dung lượng tệp vượt quá giới hạn 5MB cho phép.');
            resetCvPreview();
            return;
        }

        dropzoneText.textContent = 'Đã chọn: ' + file.name + ' (' + (file.size / 1024 / 1024).toFixed(2) + ' MB)';
        previewName.textContent = file.name + ' (' + (file.size / 1024 / 1024).toFixed(2) + ' MB)';
        previewContainer.classList.remove('d-none');

        if (ext === '.pdf') {
            if (previewObjectUrl) URL.revokeObjectURL(previewObjectUrl);
            previewObjectUrl = URL.createObjectURL(file);
            previewIframe.src = previewObjectUrl;
            previewIframe.classList.remove('d-none');
            previewDocNote.classList.add('d-none');
        } else {
            previewIframe.classList.add('d-none');
            previewDocNote.classList.remove('d-none');
        }
    }

    if (dropzone) {
        dropzone.addEventListener('click', function () {
            fileInput.click();
        });
        dropzone.addEventListener('dragover', function (e) {
            e.preventDefault();
            dropzone.classList.add('bg-white', 'border-primary');
        });
        dropzone.addEventListener('dragleave', function (e) {
            dropzone.classList.remove('bg-white', 'border-primary');
        });
        dropzone.addEventListener('drop', function (e) {
            e.preventDefault();
            dropzone.classList.remove('bg-white', 'border-primary');
            var files = e.dataTransfer.files;
            if (files && files.length > 0) {
                fileInput.files = files;
                handleFileSelected(files[0]);
            }
        });
    }

    if (fileInput) {
        fileInput.addEventListener('change', function () {
            if (fileInput.files && fileInput.files.length > 0) {
                handleFileSelected(fileInput.files[0]);
            } else {
                resetCvPreview();
            }
        });
    }

    if (btnRemoveCv) {
        btnRemoveCv.addEventListener('click', function (e) {
            e.preventDefault();
            resetCvPreview();
        });
    }

    if (btnCancelDup) {
        btnCancelDup.addEventListener('click', function () {
            dupAlert.classList.add('d-none');
            dupActionInput.value = '';
        });
    }

    if (btnOverrideDup) {
        btnOverrideDup.addEventListener('click', function () {
            dupActionInput.value = 'create_anyway';
            submitFormAjax();
        });
    }

    function renderDuplicateMatches(matches) {
        dupList.replaceChildren();
        if (!matches || !matches.length) return;

        var labels = { email: 'Trùng Email', phone: 'Trùng Số điện thoại' };

        matches.forEach(function (m) {
            var cand = m.candidate || {};
            var item = document.createElement('div');
            item.className = 'p-3 bg-white border border-warning rounded shadow-xs';

            var topRow = document.createElement('div');
            topRow.className = 'd-flex justify-content-between align-items-center mb-1';
            
            var nameSpan = document.createElement('strong');
            nameSpan.className = 'text-dark';
            nameSpan.textContent = cand.fullName || cand.name || 'Ứng viên';

            var tagsDiv = document.createElement('div');
            tagsDiv.className = 'd-flex gap-1 flex-wrap';

            (m.matchedFields || []).forEach(function (field) {
                var badge = document.createElement('span');
                badge.className = 'badge bg-danger-subtle text-danger border border-danger-subtle';
                badge.style.fontSize = '0.72rem';
                badge.innerHTML = '<i class="bi bi-shield-exclamation me-1"></i>' + (labels[field] || field);
                tagsDiv.appendChild(badge);
            });

            if (m.sameName) {
                var nameBadge = document.createElement('span');
                nameBadge.className = 'badge bg-warning-subtle text-warning-emphasis border border-warning-subtle';
                nameBadge.style.fontSize = '0.72rem';
                nameBadge.textContent = 'Cùng họ tên';
                tagsDiv.appendChild(nameBadge);
            }

            if (m.samePosition) {
                var posBadge = document.createElement('span');
                posBadge.className = 'badge bg-info-subtle text-info border border-info-subtle';
                posBadge.style.fontSize = '0.72rem';
                posBadge.textContent = 'Cùng vị trí';
                tagsDiv.appendChild(posBadge);
            }

            topRow.append(nameSpan, tagsDiv);

            var metaRow = document.createElement('div');
            metaRow.className = 'small text-muted d-flex gap-3 mt-1 flex-wrap';
            metaRow.innerHTML = '<span><i class="bi bi-envelope me-1"></i>' + (cand.email || '-') + '</span>' +
                                '<span><i class="bi bi-telephone me-1"></i>' + (cand.phone || '-') + '</span>' +
                                '<span><i class="bi bi-briefcase me-1"></i>' + (cand.position || 'Chưa gắn vị trí') + '</span>';

            item.append(topRow, metaRow);
            dupList.appendChild(item);
        });
    }

    function submitFormAjax() {
        errorAlert.classList.add('d-none');
        if (!form.reportValidity()) return;

        btnSubmit.disabled = true;
        btnSubmit.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span> Đang xử lý...';

        var formData = new FormData(form);

        fetch(form.action, {
            method: 'POST',
            body: formData,
            headers: {
                'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        }).then(function (response) {
            return response.json().then(function (data) {
                return { status: response.status, ok: response.ok, data: data };
            }).catch(function () {
                if (response.ok) {
                    window.location.reload();
                    return;
                }
                throw new Error('Máy chủ phản hồi trạng thái: ' + response.status);
            });
        }).then(function (res) {
            if (!res) return;
            if (res.status === 409) {
                // Phát hiện trùng hồ sơ (KN-346, KN-351)
                dupMsg.textContent = res.data.message || 'Phát hiện trùng khớp với hồ sơ hiện có trong hệ thống:';
                renderDuplicateMatches(res.data.matches);
                dupAlert.classList.remove('d-none');
                dupAlert.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            } else if (res.ok) {
                // Tạo mới thành công
                dupAlert.classList.add('d-none');
                window.location.reload();
            } else {
                errorMsg.textContent = res.data.message || 'Không thể lưu hồ sơ ứng viên.';
                errorAlert.classList.remove('d-none');
            }
        }).catch(function (err) {
            errorMsg.textContent = err.message || 'Lỗi mạng hoặc kết nối máy chủ.';
            errorAlert.classList.remove('d-none');
        }).finally(function () {
            btnSubmit.disabled = false;
            btnSubmit.innerHTML = '<i class="bi bi-check-lg me-1"></i> Kiểm tra trùng &amp; Lưu hồ sơ';
        });
    }

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        dupActionInput.value = '';
        submitFormAjax();
    });

    var modalElement = document.getElementById('addCandidateModal');
    if (modalElement) {
        modalElement.addEventListener('hidden.bs.modal', function () {
            form.reset();
            resetCvPreview();
            dupAlert.classList.add('d-none');
            errorAlert.classList.add('d-none');
            dupActionInput.value = '';
        });
    }
})();

// Real-time Duplicate Check in Add Candidate Modal (KN-298)
(function() {
    var emailInput = document.getElementById('manualEmail') || document.getElementById('addCandEmail');
    var phoneInput = document.getElementById('manualPhone') || document.getElementById('addCandPhone');
    var alertBox = document.getElementById('addCandDuplicateAlert');
    var matchesList = document.getElementById('dupMatchesList');
    var checkTimer = null;

    function checkDuplicates() {
        var email = emailInput ? emailInput.value.trim() : '';
        var phone = phoneInput ? phoneInput.value.trim() : '';

        if (!email && !phone) {
            if (alertBox) alertBox.style.display = 'none';
            return;
        }

        var url = '${pageContext.request.contextPath}/api/candidates/check-duplicate?email=' 
            + encodeURIComponent(email) + '&phone=' + encodeURIComponent(phone);

        fetch(url)
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (!alertBox || !matchesList) return;
                if (data.isDuplicate && data.matches && data.matches.length > 0) {
                    alertBox.style.display = 'block';
                    var html = '';
                    data.matches.forEach(function(m) {
                        html += '<div class="d-flex align-items-center justify-content-between p-2 bg-white rounded border border-warning-subtle small">';
                        html += '  <div>';
                        html += '    <strong class="text-dark">' + (m.fullName || 'Ứng viên') + '</strong> ';
                        html += '    <span class="text-muted">(' + (m.email || '') + ' - ' + (m.phone || 'N/A') + ')</span>';
                        if (m.requisitionTitle) {
                            html += '    <div class="text-xs text-muted">Vị trí: ' + m.requisitionTitle + '</div>';
                        }
                        html += '  </div>';
                        html += '  <a href="${pageContext.request.contextPath}/candidates/profile?id=' + m.id + '" target="_blank" class="btn btn-xs btn-outline-primary py-0 px-2 rounded-pill">Xem hồ sơ</a>';
                        html += '</div>';
                    });
                    matchesList.innerHTML = html;
                } else {
                    alertBox.style.display = 'none';
                    matchesList.innerHTML = '';
                }
            })
            .catch(function(err) {
                console.error('Error checking candidate duplicates:', err);
            });
    }

    function onInputChange() {
        clearTimeout(checkTimer);
        checkTimer = setTimeout(checkDuplicates, 400);
    }

    if (emailInput) emailInput.addEventListener('input', onInputChange);
    if (phoneInput) phoneInput.addEventListener('input', onInputChange);

    var modalElement = document.getElementById('addCandidateModal');
    if (modalElement) {
        modalElement.addEventListener('hidden.bs.modal', function () {
            if (alertBox) alertBox.style.display = 'none';
            if (matchesList) matchesList.innerHTML = '';
        });
    }
})();
</script>
<jsp:include page="../common/footer.jsp" />

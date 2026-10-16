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
                                <th class="ps-3">Họ và tên</th>
                                <th>Phòng ban</th>
                                <th>Vị trí ứng tuyển &amp; CV</th>
                                <th>Nguồn / Người giới thiệu</th>
                                <th>Thời gian</th>
                                <th>Trạng thái</th>
                                <th>Điểm &amp; Xếp hạng</th>
                                <th class="text-end pe-4" style="min-width: 220px;">Thao tác &amp; Chuyển vòng</th>
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
                                                <a href="${pageContext.request.contextPath}/candidates/profile?id=${c.id}"
                                                   class="fw-bold text-dark text-decoration-none d-block"
                                                   title="Xem hồ sơ hợp nhất của ${c.fullName}"
                                                   style="transition: color .15s;"
                                                   onmouseover="this.style.color='#4f46e5'" onmouseout="this.style.color=''">
                                                    ${c.fullName}
                                                </a>
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
                                        <div class="text-dark small"><c:out value="${not empty c.applicationSourceName ? c.applicationSourceName : 'Chưa xác định'}" /></div>
                                        <c:if test="${not empty c.referredByUserName}">
                                            <div class="text-muted small">Người giới thiệu: <c:out value="${c.referredByUserName}" /></div>
                                        </c:if>
                                    </td>
                                    <td>
                                        <div class="text-secondary small">
                                            <fmt:formatDate value="${c.createdAt}" pattern="dd/MM/yyyy HH:mm" />
                                        </div>
                                    </td>
                                    <td>
                                        <span class="badge-pill-status ${c.status == 'HIRED' ? 'success' : (c.status == 'WITHDRAWN' ? 'secondary' : (c.status == 'REJECTED' ? 'danger' : (c.status == 'OFFER' ? 'info' : 'warning')))}">
                                            ${c.getStatusLabel()}
                                        </span>
                                    </td>
                                    <%-- Cột điểm phù hợp & cờ tiêu chí bắt buộc --%>
                                    <td style="min-width:130px;">
                                        <c:choose>
                                            <c:when test="${c.matchScore != null}">
                                                <div class="d-flex flex-column gap-1 align-items-start">
                                                    <span class="score-badge ${c.scoreBadgeClass}" title="Điểm phù hợp (0-100)">
                                                        <i class="bi bi-star-half" style="font-size:.65rem;"></i>&nbsp;${c.matchScore}/100
                                                    </span>
                                                    <span class="${c.mandatoryFlagBadgeClass}" style="font-size:.66rem;">
                                                        <c:choose>
                                                            <c:when test="${c.mandatoryFlag == 'PASS'}"><i class="bi bi-check-circle-fill"></i></c:when>
                                                            <c:otherwise><i class="bi bi-x-circle-fill"></i></c:otherwise>
                                                        </c:choose>
                                                        ${c.mandatoryFlagLabel}
                                                    </span>
                                                </div>
                                            </c:when>
                                            <c:otherwise>
                                                <button type="button"
                                                        class="btn score-one-btn"
                                                        data-id="${c.id}"
                                                        title="Chấm điểm ngay cho ứng viên này (cần cấu hình tiêu chí trước)"
                                                        style="font-size:.68rem;border-radius:6px;padding:2px 8px;white-space:nowrap;background:#f1f5f9;border:1px solid #e2e8f0;color:#64748b;">
                                                    <i class="bi bi-lightning-charge-fill" style="color:#f59e0b;"></i> Chấm điểm
                                                </button>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="text-end pe-4">

                                        <div class="d-inline-flex align-items-center gap-2">
                                            <c:if test="${not empty c.cvUrl}">
                                                <button type="button" class="btn btn-sm btn-outline-primary d-inline-flex align-items-center gap-1 py-1 px-2"
                                                        style="font-size: 0.75rem; border-radius: 6px; white-space: nowrap;"
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
                                    <td colspan="8" class="text-center py-5 text-muted">
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

        <%-- CSS: Scoring badges – chấm điểm hồ sơ --%>
        <style>
        .score-badge {
            display: inline-flex; align-items: center; gap: 3px;
            min-width: 56px; padding: 3px 9px; border-radius: 20px;
            font-size: .78rem; font-weight: 700; border: 1px solid rgba(0,0,0,.08);
        }
        .score-high   { background:linear-gradient(135deg,#d1fae5,#a7f3d0); color:#065f46; }
        .score-medium { background:linear-gradient(135deg,#fef3c7,#fde68a); color:#92400e; }
        .score-low    { background:linear-gradient(135deg,#fee2e2,#fecaca); color:#991b1b; }
        .score-pending{ background:#f3f4f6; color:#9ca3af; border-color:#e5e7eb; }
        .badge-pass {
            display:inline-flex; align-items:center; gap:3px;
            background:linear-gradient(135deg,#d1fae5,#a7f3d0); color:#065f46;
            border:1px solid #6ee7b7; padding:2px 8px; border-radius:20px;
            font-size:.68rem; font-weight:700;
        }
        .badge-fail {
            display:inline-flex; align-items:center; gap:3px;
            background:linear-gradient(135deg,#fee2e2,#fecaca); color:#991b1b;
            border:1px solid #fca5a5; padding:2px 8px; border-radius:20px;
            font-size:.68rem; font-weight:700;
        }
        .badge-pending {
            display:inline-flex; align-items:center; gap:3px;
            background:#f3f4f6; color:#6b7280; border:1px solid #d1d5db;
            padding:2px 8px; border-radius:20px; font-size:.68rem; font-weight:600;
        }
        /* Toast thông báo chấm điểm */
        #scoreToast { min-width: 320px; z-index: 9999; }
        .score-result-bar { height: 8px; border-radius: 4px; }
        </style>
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
                            <small class="text-muted">Tính năng tự động bóc tách thông tin từ CV PDF &bull; User Story KN-213</small>
                        </div>
                    </div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <!-- Banner hướng dẫn & Quy tắc nghiệp vụ KN-213 -->
                    <div class="alert alert-primary d-flex align-items-start gap-3 border border-primary-subtle rounded-3 py-2 px-3 mb-4" role="alert" style="background-color: #f0f7ff;">
                        <i class="bi bi-info-circle-fill text-primary fs-5 mt-1"></i>
                        <div class="small">
                            <strong>Nguyên tắc bảo vệ dữ liệu (KN-213):</strong> Kết quả bóc tách tự động chỉ mang tính chất hỗ trợ và <strong>không tự ý ghi đè</strong> thông tin ứng viên đã nộp. Recruiter có thể đối chiếu, trực tiếp chỉnh sửa các trường trước khi xác nhận lưu vào hồ sơ chính thức. Bóc tách thất bại không ảnh hưởng đến hồ sơ nộp.
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

    <%-- Toast thông báo kết quả chấm điểm --%>
    <div class="position-fixed top-0 end-0 p-3" style="z-index:9999;">
        <div id="scoreToast" class="toast align-items-center shadow-lg" role="alert" data-bs-autohide="true" data-bs-delay="6000"
             style="border:0;border-radius:14px;overflow:hidden;min-width:320px;">
            <div id="scoreToastHeader" class="d-flex align-items-center gap-2 px-3 py-2" style="background:#1e293b;">
                <i class="bi bi-award-fill text-warning fs-5"></i>
                <strong class="me-auto text-white">Kết quả chấm điểm</strong>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast"></button>
            </div>
            <div id="scoreToastBody" class="toast-body bg-white"></div>
        </div>
    </div>

    <%-- Modal Chi tiết Điểm Chấm --%>
    <div class="modal fade" id="scoreDetailModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content" style="border-radius:16px;overflow:hidden;border:0;">
                <div class="modal-header" style="background:linear-gradient(135deg,#1e1b4b,#312e81);">
                    <div class="d-flex align-items-center gap-2">
                        <i class="bi bi-bar-chart-line-fill text-warning fs-4"></i>
                        <div>
                            <h5 class="modal-title fw-bold text-white mb-0">Báo cáo Chấm điểm Hồ sơ</h5>
                            <small class="text-indigo-200" style="color:#a5b4fc;">Phân tích độ phù hợp theo Tiêu chí Tuyển dụng</small>
                        </div>
                    </div>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body p-0" id="scoreDetailBody">
                    <div class="text-center py-5 text-muted">
                        <div class="spinner-border text-primary" role="status"></div>
                        <div class="mt-2">Đang chấm điểm...</div>
                    </div>
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Dóng</button>
                    <a href="${pageContext.request.contextPath}/screening-criteria" class="btn btn-outline-primary" style="border-radius:8px;">
                        <i class="bi bi-award me-1"></i>Cấu hình tiêu chí
                    </a>
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
</script>
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

// ===== XỬ LÝ NÚT CHẤM ĐIỂM ĐƠN LẺ (score-one-btn) =====
document.addEventListener('click', function(e) {
    var btn = e.target.closest('.score-one-btn');
    if (!btn) return;
    e.preventDefault();

    var candidateId = btn.getAttribute('data-id');
    if (!candidateId) return;

    // Hiện modal loading
    var modal = new bootstrap.Modal(document.getElementById('scoreDetailModal'));
    document.getElementById('scoreDetailBody').innerHTML =
        '<div class="text-center py-5"><div class="spinner-border text-primary" role="status"></div>' +
        '<div class="mt-2 text-muted">Đang phân tích hồ sơ và chấm điểm...</div></div>';
    modal.show();

    // Gọi API chấm điểm đơn lẻ
    fetch(contextPath + '/screening-criteria/score-one', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8' },
        body: 'candidateId=' + encodeURIComponent(candidateId)
    })
    .then(function(r) { return r.json(); })
    .then(function(data) {
        if (data.error) {
            document.getElementById('scoreDetailBody').innerHTML = buildErrorHtml(data.error);
            return;
        }
        // Cập nhật cell điểm inline (không reload trang)
        updateScoreCellInline(btn, data);
        // Hiển thị báo cáo chi tiết trong modal
        document.getElementById('scoreDetailBody').innerHTML = buildScoreReportHtml(data);
        // Show toast
        showScoreToast(data);
    })
    .catch(function(err) {
        document.getElementById('scoreDetailBody').innerHTML = buildErrorHtml('Không thể kết nối. Vui lòng thử lại.');
    });
});

/** Cập nhật ô điểm inline mà không reload trang */
function updateScoreCellInline(btn, data) {
    var td = btn.closest('td');
    if (!td) return;
    var scoreClass = data.scoreBadgeClass || 'score-pending';
    var flagClass  = data.mandatoryFlagBadgeClass || 'badge-pending';
    var flagIcon   = data.mandatoryFlag === 'PASS'
        ? '<i class="bi bi-check-circle-fill"></i>'
        : '<i class="bi bi-x-circle-fill"></i>';
    td.innerHTML =
        '<div class="d-flex flex-column gap-1 align-items-start">' +
        '<span class="score-badge ' + scoreClass + '" title="Điểm phù hợp (0-100)">' +
        '<i class="bi bi-star-half" style="font-size:.65rem;"></i>&nbsp;' + data.totalScore + '/100</span>' +
        '<span class="' + flagClass + '" style="font-size:.66rem;">' + flagIcon + ' ' + data.mandatoryFlagLabel + '</span>' +
        '</div>';
}

/** Tạo HTML báo cáo chi tiết điểm chấm */
function buildScoreReportHtml(d) {
    var flagBg  = d.mandatoryFlag === 'PASS' ? '#d1fae5' : '#fee2e2';
    var flagClr = d.mandatoryFlag === 'PASS' ? '#065f46' : '#991b1b';
    var recBadge = '';
    if (d.recommendation === 'HIGH') recBadge = '<span style="background:#4f46e5;color:#fff;border-radius:12px;padding:2px 12px;font-size:.75rem;font-weight:700;">🔵 Ưu tiên cao</span>';
    else if (d.recommendation === 'MEDIUM') recBadge = '<span style="background:#f59e0b;color:#fff;border-radius:12px;padding:2px 12px;font-size:.75rem;font-weight:700;">🟡 Ưu tiên trung bình</span>';
    else recBadge = '<span style="background:#e5e7eb;color:#374151;border-radius:12px;padding:2px 12px;font-size:.75rem;font-weight:700;">⚪ Xem xét thêm</span>';

    var scoreColor = d.totalScore >= 75 ? '#065f46' : (d.totalScore >= 50 ? '#92400e' : '#991b1b');
    var scoreBg    = d.totalScore >= 75 ? '#d1fae5' : (d.totalScore >= 50 ? '#fef3c7' : '#fee2e2');

    return '<div class="p-4">' +
        // Header tổng quan
        '<div class="d-flex align-items-center justify-content-between gap-3 mb-4 p-3 rounded-3" style="background:#f8fafc;border:1px solid #e2e8f0;">' +
            '<div>' +
                '<div class="fw-bold text-dark fs-6">' + escJ(d.candidateName) + '</div>' +
                '<div class="text-muted small">' + escJ(d.requisitionTitle || '') + '</div>' +
            '</div>' +
            '<div class="text-center">' +
                '<div style="font-size:2.2rem;font-weight:800;color:' + scoreColor + ';background:' + scoreBg + ';border-radius:50%;width:72px;height:72px;display:flex;align-items:center;justify-content:center;margin:0 auto;">' + d.totalScore + '</div>' +
                '<div class="small text-muted mt-1">/ 100 điểm</div>' +
            '</div>' +
        '</div>' +
        // Cờ bắt buộc & xếp hạng
        '<div class="d-flex gap-2 mb-4 flex-wrap">' +
            '<span style="background:' + flagBg + ';color:' + flagClr + ';border-radius:12px;padding:4px 14px;font-size:.8rem;font-weight:700;">' +
                (d.mandatoryFlag === 'PASS' ? '✅' : '❌') + ' ' + escJ(d.mandatoryFlagLabel) + '</span>' +
            recBadge +
        '</div>' +
        // Bảng chi tiết 3 hạng mục
        '<table class="table table-bordered table-sm" style="font-size:.82rem;">' +
            '<thead style="background:#f1f5f9;"><tr>' +
                '<th>Tiêu chí bắt buộc</th>' +
                '<th class="text-center" style="width:90px;">Kết quả</th>' +
                '<th class="text-center" style="width:90px;">Điểm</th>' +
            '</tr></thead><tbody>' +
            '<tr>' +
                '<td><strong>⏱ Kinh nghiệm</strong><div class="text-muted" style="font-size:.75rem;">' + escJ(d.experienceDetail || '') + '</div></td>' +
                '<td class="text-center">' + (d.expPassed ? '<span class="badge-pass">✓ Đạt</span>' : '<span class="badge-fail">✗ Chưa đạt</span>') + '</td>' +
                '<td class="text-center fw-bold">' + d.experienceScore + '</td>' +
            '</tr>' +
            '<tr>' +
                '<td><strong>🛠 Kỹ năng</strong><div class="text-muted" style="font-size:.75rem;">' + escJ(d.skillDetail || '') + '</div></td>' +
                '<td class="text-center">' + (d.skillsPassed ? '<span class="badge-pass">✓ Đạt</span>' : '<span class="badge-fail">✗ Thiếu</span>') + '</td>' +
                '<td class="text-center fw-bold">' + d.skillScore + '</td>' +
            '</tr>' +
            '<tr>' +
                '<td><strong>🎓 Bằng cấp</strong><div class="text-muted" style="font-size:.75rem;">' + escJ(d.degreeDetail || '') + '</div></td>' +
                '<td class="text-center">' + (d.degreePassed ? '<span class="badge-pass">✓ Đạt</span>' : '<span class="badge-fail">✗ Chưa đạt</span>') + '</td>' +
                '<td class="text-center fw-bold">' + d.educationScore + '</td>' +
            '</tr>' +
            '<tr style="background:#f8fafc;"><td><strong>TỔNG ĐIỂM PHÙ HỢP</strong></td><td></td><td class="text-center fw-bold fs-6" style="color:' + scoreColor + ';">' + d.totalScore + '/100</td></tr>' +
        '</tbody></table>' +
        // Thanh tiến trình
        '<div class="mt-2 mb-1 small text-muted fw-semibold">Phân bổ điểm thực tế</div>' +
        '<div style="display:flex;height:12px;border-radius:999px;overflow:hidden;gap:2px;">' +
            '<div style="width:' + d.experienceScore + '%;background:#4f46e5;" title="Kinh nghiệm: ' + d.experienceScore + '"></div>' +
            '<div style="width:' + d.skillScore + '%;background:#7c3aed;" title="Kỹ năng: ' + d.skillScore + '"></div>' +
            '<div style="width:' + d.educationScore + '%;background:#0ea5e9;" title="Bằng cấp: ' + d.educationScore + '"></div>' +
            '<div style="flex:1;background:#e5e7eb;"></div>' +
        '</div>' +
        '<div class="d-flex gap-3 mt-1" style="font-size:.72rem;color:#64748b;">' +
            '<span>● KN: ' + d.experienceScore + 'đ</span><span>● KĐ: ' + d.skillScore + 'đ</span><span>● BK: ' + d.educationScore + 'đ</span>' +
        '</div>' +
        '<div class="alert alert-info mt-3 py-2 small" style="border-radius:8px;">' +
            '<i class="bi bi-info-circle me-1"></i> Điểm số chỉ để <strong>gợi ý thứ tự ưu tiên</strong>. Nhà tuyển dụng quyết định cuối cùng.' +
        '</div>' +
    '</div>';
}

function buildErrorHtml(msg) {
    return '<div class="text-center py-5"><div class="text-danger fs-2"><i class="bi bi-exclamation-triangle"></i></div>' +
        '<div class="fw-semibold text-danger mt-2">Chưa thể chấm điểm</div>' +
        '<div class="text-muted small mt-1 px-4">' + escJ(msg) + '</div>' +
        '<a href="' + contextPath + '/screening-criteria" class="btn btn-primary btn-sm mt-3">' +
        '<i class="bi bi-award me-1"></i>Cấu hình tiêu chí ngay</a></div>';
}

function showScoreToast(d) {
    var body = document.getElementById('scoreToastBody');
    var scoreColor = d.totalScore >= 75 ? '#065f46' : (d.totalScore >= 50 ? '#92400e' : '#991b1b');
    body.innerHTML =
        '<div class="d-flex align-items-center gap-3 py-1">' +
            '<div style="font-size:1.6rem;font-weight:800;color:' + scoreColor + ';">' + d.totalScore + '<small style="font-size:.8rem;">/100</small></div>' +
            '<div>' +
                '<div class="fw-bold text-dark" style="font-size:.85rem;">' + escJ(d.candidateName) + '</div>' +
                '<div class="text-muted" style="font-size:.75rem;">' + escJ(d.mandatoryFlagLabel) + ' · ' + escJ(d.recommendationLabel) + '</div>' +
            '</div>' +
        '</div>';
    var toast = new bootstrap.Toast(document.getElementById('scoreToast'));
    toast.show();
}

function escJ(s) {
    if (!s) return '';
    return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

var contextPath = '${pageContext.request.contextPath}';
</script>
<jsp:include page="../common/footer.jsp" />

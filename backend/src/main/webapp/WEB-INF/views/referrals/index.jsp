<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Giới thiệu ứng viên nội bộ" />
<c:set var="breadcrumb" value="Giới thiệu ứng viên" />
<c:set var="activeMenu" value="referrals" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Breadcrumb & Header chuẩn phong cách IRMS / RisApps -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <nav aria-label="breadcrumb" class="mb-1">
                    <ol class="breadcrumb mb-0" style="font-size: 0.8rem;">
                        <li class="breadcrumb-item">
                            <a href="${pageContext.request.contextPath}/dashboard" class="text-decoration-none text-muted">
                                <i class="bi bi-house-door me-1"></i>Trang chủ
                            </a>
                        </li>
                        <li class="breadcrumb-item active text-primary fw-semibold">Giới thiệu ứng viên</li>
                    </ol>
                </nav>
                <h4 class="fw-bold mb-0">
                    <i class="bi bi-person-heart text-primary me-2"></i>Giới thiệu Ứng viên Nội bộ
                </h4>
                <p class="text-muted small mb-0 mt-1">
                    Chương trình giới thiệu nhân tài nội bộ: Nộp hồ sơ người quen và theo dõi trạng thái xử lý theo thời gian thực
                </p>
            </div>
            <div>
                <a href="#referralFormCard" class="btn btn-primary shadow-sm" onclick="document.getElementById('referralFullName').focus();">
                    <i class="bi bi-plus-circle me-1"></i> Gửi hồ sơ mới
                </a>
            </div>
        </div>

        <!-- Thống kê KPI Giới thiệu của bạn -->
        <c:set var="totalReferrals" value="${not empty referrals ? referrals.size() : 0}" />
        <c:set var="inProgressCount" value="0" />
        <c:set var="hiredCount" value="0" />
        <c:forEach var="ref" items="${referrals}">
            <c:if test="${ref.status == 'HIRED'}">
                <c:set var="hiredCount" value="${hiredCount + 1}" />
            </c:if>
            <c:if test="${ref.status == 'APPLIED' || ref.status == 'SCREENING' || ref.status == 'INTERVIEWING' || ref.status == 'OFFER'}">
                <c:set var="inProgressCount" value="${inProgressCount + 1}" />
            </c:if>
        </c:forEach>

        <div class="row g-3 mb-4">
            <div class="col-12 col-md-4">
                <div class="card border shadow-sm h-100" style="border-left: 4px solid #3b82f6 !important;">
                    <div class="card-body p-3">
                        <div class="d-flex align-items-center justify-content-between">
                            <div>
                                <div class="text-muted text-xs text-uppercase fw-bold">Tổng hồ sơ đã gửi</div>
                                <div class="fs-4 fw-bold text-dark mt-1">${totalReferrals}</div>
                            </div>
                            <div class="rounded-circle bg-primary bg-opacity-10 d-flex align-items-center justify-content-center" style="width: 44px; height: 44px;">
                                <i class="bi bi-people-fill text-primary fs-5"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-12 col-md-4">
                <div class="card border shadow-sm h-100" style="border-left: 4px solid #f59e0b !important;">
                    <div class="card-body p-3">
                        <div class="d-flex align-items-center justify-content-between">
                            <div>
                                <div class="text-muted text-xs text-uppercase fw-bold">Đang trong Pipeline xử lý</div>
                                <div class="fs-4 fw-bold text-warning mt-1">${inProgressCount}</div>
                            </div>
                            <div class="rounded-circle bg-warning bg-opacity-10 d-flex align-items-center justify-content-center" style="width: 44px; height: 44px;">
                                <i class="bi bi-hourglass-split text-warning fs-5"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-12 col-md-4">
                <div class="card border shadow-sm h-100" style="border-left: 4px solid #10b981 !important;">
                    <div class="card-body p-3">
                        <div class="d-flex align-items-center justify-content-between">
                            <div>
                                <div class="text-muted text-xs text-uppercase fw-bold">Ứng viên đã trúng tuyển</div>
                                <div class="fs-4 fw-bold text-success mt-1">${hiredCount}</div>
                            </div>
                            <div class="rounded-circle bg-success bg-opacity-10 d-flex align-items-center justify-content-center" style="width: 44px; height: 44px;">
                                <i class="bi bi-trophy-fill text-success fs-5"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Khung 1: Form Nộp hồ sơ giới thiệu -->
        <div class="card border shadow-sm mb-4" id="referralFormCard">
            <div class="card-header bg-white py-3 border-bottom d-flex align-items-center justify-content-between">
                <h6 class="fw-bold mb-0 text-dark">
                    <i class="bi bi-send-plus text-primary me-2"></i>Gửi hồ sơ giới thiệu ứng viên mới
                </h6>
                <span class="badge bg-primary-subtle text-primary border border-primary-subtle" style="font-size: 0.72rem;">
                    Nội bộ
                </span>
            </div>
            <div class="card-body p-4">
                <form action="${pageContext.request.contextPath}/referrals/create" method="POST" id="referralForm">
                    <div class="row g-3">
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold" for="referralFullName">
                                Họ và tên ứng viên <span class="text-danger">*</span>
                            </label>
                            <div class="input-group">
                                <span class="input-group-text bg-light text-muted"><i class="bi bi-person"></i></span>
                                <input id="referralFullName" type="text" class="form-control" name="fullName"
                                       placeholder="Ví dụ: Nguyễn Văn An" maxlength="255" required>
                            </div>
                        </div>

                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold" for="referralEmail">
                                Email ứng viên <span class="text-danger">*</span>
                            </label>
                            <div class="input-group">
                                <span class="input-group-text bg-light text-muted"><i class="bi bi-envelope"></i></span>
                                <input id="referralEmail" type="email" class="form-control" name="email"
                                       placeholder="ungvien@gmail.com" maxlength="255" required>
                            </div>
                        </div>

                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold" for="referralPhone">
                                Số điện thoại liên hệ
                            </label>
                            <div class="input-group">
                                <span class="input-group-text bg-light text-muted"><i class="bi bi-telephone"></i></span>
                                <input id="referralPhone" type="tel" class="form-control" name="phone"
                                       placeholder="0912345678" maxlength="50">
                            </div>
                        </div>

                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold" for="referralRequisition">
                                Vị trí tuyển dụng đề xuất
                            </label>
                            <div class="input-group">
                                <span class="input-group-text bg-light text-muted"><i class="bi bi-briefcase"></i></span>
                                <select id="referralRequisition" class="form-select" name="requisitionId">
                                    <option value="">-- Chưa chọn vị trí cụ thể (Để HR đề xuất) --</option>
                                    <c:forEach var="requisition" items="${openRequisitions}">
                                        <option value="${requisition.id}">
                                            <c:out value="${requisition.title}" /> (<c:out value="${requisition.code}" />)
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>

                        <div class="col-12">
                            <label class="form-label fw-semibold" for="referralCvUrl">
                                Đường dẫn liên kết CV (Link Google Drive / Dropbox / LinkedIn)
                            </label>
                            <div class="input-group">
                                <span class="input-group-text bg-light text-muted"><i class="bi bi-link-45deg"></i></span>
                                <input id="referralCvUrl" type="url" class="form-control" name="cvUrl"
                                       maxlength="2048" placeholder="https://drive.google.com/file/d/...">
                            </div>
                            <div class="form-text text-muted">Đường dẫn phải bắt đầu bằng <code>http://</code> hoặc <code>https://</code>. Có thể để trống nếu chưa có link CV điện tử.</div>
                        </div>

                        <div class="col-12">
                            <label class="form-label fw-semibold" for="referralNotes">
                                Ghi chú giới thiệu &amp; Đánh giá ứng viên <span class="text-danger">*</span>
                            </label>
                            <textarea id="referralNotes" class="form-control" name="notes" rows="3" maxlength="5000" required
                                      placeholder="Chia sẻ ngắn gọn về mối quan hệ, kinh nghiệm, kỹ năng thế mạnh hoặc lý do bạn đề xuất ứng viên này cho công ty..."></textarea>
                            <div class="form-text text-muted">Ghi chú này sẽ được chuyển trực tiếp tới chuyên viên tuyển dụng phụ trách vị trí.</div>
                        </div>

                        <div class="col-12 d-flex justify-content-end gap-2 pt-2">
                            <button type="reset" class="btn btn-outline-secondary px-3">
                                <i class="bi bi-arrow-counterclockwise me-1"></i> Làm lại
                            </button>
                            <button type="submit" class="btn btn-primary px-4 shadow-sm" id="btnSubmitReferral">
                                <i class="bi bi-send-fill me-1"></i> Gửi hồ sơ giới thiệu
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>

        <!-- Khung 2: Bảng theo dõi trạng thái các hồ sơ đã giới thiệu -->
        <div class="card border shadow-sm">
            <div class="card-header bg-white py-3 border-bottom d-flex align-items-center justify-content-between">
                <div>
                    <h6 class="fw-bold mb-0 text-dark">
                        <i class="bi bi-clock-history text-secondary me-2"></i>Danh sách hồ sơ bạn đã giới thiệu
                    </h6>
                    <small class="text-muted">Theo dõi trạng thái tiến độ xử lý của từng ứng viên theo thời gian thực</small>
                </div>
                <span class="badge bg-primary rounded-pill px-3 py-1.5" style="font-size: 0.78rem;">
                    ${totalReferrals} hồ sơ
                </span>
            </div>
            <div class="table-responsive">
                <table class="table table-custom table-hover align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-3" style="width: 26%; min-width: 180px;">Ứng viên</th>
                            <th style="width: 22%; min-width: 150px;">Vị trí đề xuất</th>
                            <th style="min-width: 180px;">Ghi chú của bạn</th>
                            <th style="width: 14%; min-width: 110px;">Thời gian gửi</th>
                            <th class="text-center pe-3" style="width: 16%; min-width: 130px;">Trạng thái tiến độ</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty referrals}">
                                <tr>
                                    <td colspan="5" class="text-center text-muted py-5">
                                        <div class="d-flex flex-column align-items-center">
                                            <div class="rounded-circle bg-light d-flex align-items-center justify-content-center mb-3" style="width: 56px; height: 56px;">
                                                <i class="bi bi-inbox text-secondary fs-3"></i>
                                            </div>
                                            <div class="fw-semibold text-secondary">Bạn chưa gửi hồ sơ giới thiệu nào</div>
                                            <small class="text-muted mt-1">Hãy sử dụng biểu mẫu phía trên để gửi hồ sơ người quen và nhận thưởng giới thiệu nội bộ!</small>
                                        </div>
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="referral" items="${referrals}">
                                    <tr>
                                        <td class="ps-3">
                                            <div class="d-flex align-items-center gap-2">
                                                <div class="rounded-circle bg-primary bg-opacity-10 text-primary d-flex align-items-center justify-content-center flex-shrink-0"
                                                     style="width: 32px; height: 32px; font-weight: 700; font-size: 0.8rem;">
                                                    <c:choose>
                                                        <c:when test="${not empty referral.fullName}">
                                                            ${referral.fullName.substring(0, 1).toUpperCase()}
                                                        </c:when>
                                                        <c:otherwise>U</c:otherwise>
                                                    </c:choose>
                                                </div>
                                                <div class="overflow-hidden" style="min-width: 0;">
                                                    <div class="fw-bold text-dark text-nowrap" style="font-size: 0.865rem;">
                                                        <c:out value="${referral.fullName}" />
                                                    </div>
                                                    <div class="text-muted text-xs text-nowrap" style="max-width: 180px; overflow: hidden; text-overflow: ellipsis;">
                                                        <c:out value="${referral.email}" />
                                                        <c:if test="${not empty referral.phone}"> &bull; <c:out value="${referral.phone}" /></c:if>
                                                    </div>
                                                </div>
                                            </div>
                                        </td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty referral.requisitionTitle}">
                                                    <div class="fw-semibold text-dark text-truncate" style="font-size: 0.85rem; max-width: 220px;" title="${referral.requisitionTitle}">
                                                        <c:out value="${referral.requisitionTitle}" />
                                                    </div>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-light text-muted border">Chưa chọn vị trí cụ thể</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <div class="text-muted text-sm text-truncate d-inline-block" style="max-width: 300px;" title="${referral.notes}">
                                                <c:out value="${referral.notes}" />
                                            </div>
                                            <c:if test="${not empty referral.cvUrl}">
                                                <div class="mt-1">
                                                    <a href="${referral.cvUrl}" target="_blank" rel="noopener noreferrer" class="btn btn-xs btn-outline-primary py-0 px-2" style="font-size: 0.72rem;">
                                                        <i class="bi bi-box-arrow-up-right me-1"></i> Xem CV
                                                    </a>
                                                </div>
                                            </c:if>
                                        </td>
                                        <td class="text-muted" style="font-size: 0.82rem;">
                                            <div><i class="bi bi-calendar-event me-1"></i><fmt:formatDate value="${referral.createdAt}" pattern="dd/MM/yyyy" /></div>
                                            <div class="text-xs text-muted mt-0.5"><fmt:formatDate value="${referral.createdAt}" pattern="HH:mm" /></div>
                                        </td>
                                        <td class="text-center pe-3">
                                            <c:choose>
                                                <c:when test="${referral.status == 'HIRED'}">
                                                    <span class="badge bg-success-subtle text-success border border-success-subtle px-2.5 py-1.5" style="font-size: 0.78rem;">
                                                        <i class="bi bi-check-circle-fill me-1"></i> Đã trúng tuyển
                                                    </span>
                                                </c:when>
                                                <c:when test="${referral.status == 'OFFER'}">
                                                    <span class="badge bg-info-subtle text-info border border-info-subtle px-2.5 py-1.5" style="font-size: 0.78rem;">
                                                        <i class="bi bi-envelope-paper-fill me-1"></i> Đã gửi Offer
                                                    </span>
                                                </c:when>
                                                <c:when test="${referral.status == 'INTERVIEWING'}">
                                                    <span class="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle px-2.5 py-1.5" style="font-size: 0.78rem;">
                                                        <i class="bi bi-chat-dots-fill me-1"></i> Đang phỏng vấn
                                                    </span>
                                                </c:when>
                                                <c:when test="${referral.status == 'SCREENING'}">
                                                    <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-2.5 py-1.5" style="font-size: 0.78rem;">
                                                        <i class="bi bi-file-earmark-person-fill me-1"></i> Sàng lọc CV
                                                    </span>
                                                </c:when>
                                                <c:when test="${referral.status == 'REJECTED'}">
                                                    <span class="badge bg-danger-subtle text-danger border border-danger-subtle px-2.5 py-1.5" style="font-size: 0.78rem;">
                                                        <i class="bi bi-x-circle-fill me-1"></i> Đã từ chối
                                                    </span>
                                                </c:when>
                                                <c:when test="${referral.status == 'WITHDRAWN'}">
                                                    <span class="badge bg-secondary-subtle text-secondary border px-2.5 py-1.5" style="font-size: 0.78rem;">
                                                        <i class="bi bi-dash-circle-fill me-1"></i> Đã rút hồ sơ
                                                    </span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge bg-primary px-2.5 py-1.5" style="font-size: 0.78rem;">
                                                        <i class="bi bi-clock-fill me-1"></i> Mới ứng tuyển
                                                    </span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

<jsp:include page="../common/footer.jsp" />

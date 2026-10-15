<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Giới thiệu ứng viên" />
<c:set var="breadcrumb" value="Giới thiệu ứng viên" />
<c:set var="activeMenu" value="referrals" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />
    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <div class="mb-4">
            <h3 class="fw-bold text-dark mb-1">Giới thiệu ứng viên nội bộ</h3>
            <div class="text-muted small">Gửi hồ sơ cho bộ phận tuyển dụng và theo dõi tiến độ xử lý.</div>
        </div>

        <div class="card border-0 shadow-sm mb-4">
            <div class="card-header bg-white py-3">
                <h5 class="fw-bold mb-0"><i class="bi bi-person-plus-fill text-primary me-2"></i>Gửi hồ sơ giới thiệu</h5>
            </div>
            <div class="card-body">
                <form action="${pageContext.request.contextPath}/referrals/create" method="POST">
                    <div class="row g-3">
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold" for="referralFullName">Họ và tên ứng viên <span class="text-danger">*</span></label>
                            <input id="referralFullName" type="text" class="form-control" name="fullName" maxlength="255" required>
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold" for="referralEmail">Email ứng viên <span class="text-danger">*</span></label>
                            <input id="referralEmail" type="email" class="form-control" name="email" maxlength="255" required>
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold" for="referralPhone">Số điện thoại</label>
                            <input id="referralPhone" type="tel" class="form-control" name="phone" maxlength="50">
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold" for="referralRequisition">Vị trí tuyển dụng</label>
                            <select id="referralRequisition" class="form-select" name="requisitionId">
                                <option value="">Chưa chọn vị trí</option>
                                <c:forEach var="requisition" items="${openRequisitions}">
                                    <option value="${requisition.id}">
                                        <c:out value="${requisition.title}" /> (<c:out value="${requisition.code}" />)
                                    </option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-12">
                            <label class="form-label fw-semibold" for="referralCvUrl">Liên kết CV</label>
                            <input id="referralCvUrl" type="url" class="form-control" name="cvUrl" maxlength="2048" placeholder="https://...">
                            <div class="form-text">Có thể để trống nếu chưa có CV điện tử.</div>
                        </div>
                        <div class="col-12">
                            <label class="form-label fw-semibold" for="referralNotes">Ghi chú giới thiệu <span class="text-danger">*</span></label>
                            <textarea id="referralNotes" class="form-control" name="notes" rows="3" maxlength="5000" required
                                      placeholder="Chia sẻ ngắn gọn về kinh nghiệm, kỹ năng hoặc lý do bạn đề xuất ứng viên."></textarea>
                        </div>
                        <div class="col-12 d-flex justify-content-end">
                            <button type="submit" class="btn-ris-primary">
                                <i class="bi bi-send-fill me-1"></i> Gửi giới thiệu
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>

        <div class="card border-0 shadow-sm">
            <div class="card-header bg-white py-3 d-flex align-items-center justify-content-between">
                <h5 class="fw-bold mb-0">Hồ sơ bạn đã giới thiệu</h5>
                <span class="badge bg-primary rounded-pill">${referrals.size()}</span>
            </div>
            <div class="table-responsive">
                <table class="table align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-4">Ứng viên</th>
                            <th>Vị trí</th>
                            <th>Ghi chú giới thiệu</th>
                            <th>Ngày gửi</th>
                            <th class="pe-4">Trạng thái</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty referrals}">
                                <tr>
                                    <td colspan="5" class="text-center text-muted py-5">
                                        Bạn chưa gửi hồ sơ giới thiệu nào.
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="referral" items="${referrals}">
                                    <tr>
                                        <td class="ps-4 fw-semibold"><c:out value="${referral.fullName}" /></td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${not empty referral.requisitionTitle}">
                                                    <c:out value="${referral.requisitionTitle}" />
                                                </c:when>
                                                <c:otherwise><span class="text-muted">Chưa chọn vị trí</span></c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="text-muted"><c:out value="${referral.notes}" /></td>
                                        <td class="text-muted">
                                            <fmt:formatDate value="${referral.createdAt}" pattern="dd/MM/yyyy HH:mm" />
                                        </td>
                                        <td class="pe-4">
                                            <span class="badge ${referral.getStatusBadgeClass()}">
                                                <c:out value="${referral.getStatusLabel()}" />
                                            </span>
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
</main>

<jsp:include page="../common/footer.jsp" />

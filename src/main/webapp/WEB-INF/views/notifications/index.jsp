<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Hộp thư Thông báo & Nhắc việc" />
<c:set var="breadcrumb" value="Thông báo" />
<c:set var="activeMenu" value="notifications" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <div class="d-flex align-items-center gap-2 mb-1">
                    <div class="rounded p-2 text-white" style="background: linear-gradient(135deg, #2563eb, #7c3aed);">
                        <i class="bi bi-bell-fill fs-5"></i>
                    </div>
                    <h3 class="fw-bold text-dark mb-0">Hộp thư Thông báo &amp; Nhắc việc</h3>
                </div>
                <p class="text-muted small mb-0 ms-1">
                    Tổng hợp toàn bộ thông báo hệ thống, cảnh báo quá hạn SLA và lịch phỏng vấn dành cho bạn.
                </p>
            </div>
            <div class="d-flex align-items-center gap-2">
                <form action="${pageContext.request.contextPath}/api/notifications/mark-all-read" method="POST" class="m-0">
                    <button type="submit" class="btn btn-outline-secondary btn-sm px-3">
                        <i class="bi bi-check2-all me-1"></i> Đánh dấu đã đọc tất cả
                    </button>
                </form>
                <c:if test="${currentUser.hasRole('ADMIN') || currentUser.hasRole('HR_MANAGER') || currentUser.hasPermission('candidates.manage')}">
                    <a href="${pageContext.request.contextPath}/admin/sla-configs" class="btn btn-outline-danger btn-sm px-3">
                        <i class="bi bi-stopwatch me-1"></i> Cấu hình ngưỡng SLA
                    </a>
                </c:if>
            </div>
        </div>

        <div class="card border-0 shadow-sm rounded-3">
            <div class="card-header bg-white border-bottom py-3 d-flex align-items-center justify-content-between">
                <div class="fw-bold text-dark">
                    Tất cả thông báo (${notifications.size()})
                </div>
                <c:if test="${unreadCount > 0}">
                    <span class="badge bg-danger rounded-pill px-3 py-1">${unreadCount} chưa đọc</span>
                </c:if>
            </div>
            <div class="card-body p-0">
                <div class="list-group list-group-flush">
                    <c:forEach var="item" items="${notifications}">
                        <div class="list-group-item p-3 ${!item.read ? 'bg-light' : ''} d-flex align-items-start gap-3">
                            <div class="rounded-circle p-2 d-flex align-items-center justify-content-center flex-shrink-0"
                                 style="width: 40px; height: 40px; ${item.type == 'SLA_OVERDUE' ? 'background: #fef2f2; color: #dc2626;' : 'background: #eff6ff; color: #2563eb;'}">
                                <i class="bi ${item.type == 'SLA_OVERDUE' ? 'bi-exclamation-triangle-fill' : 'bi-info-circle-fill'} fs-5"></i>
                            </div>
                            <div class="flex-grow-1">
                                <div class="d-flex align-items-center justify-content-between gap-2">
                                    <h6 class="fw-bold text-dark mb-1 ${!item.read ? 'text-primary' : ''}">${item.title}</h6>
                                    <small class="text-muted text-xs">
                                        <fmt:formatDate value="${item.createdAt}" pattern="HH:mm dd/MM/yyyy" />
                                    </small>
                                </div>
                                <p class="text-secondary small mb-2" style="font-size: 0.85rem; line-height: 1.5;">${item.content}</p>
                                <div class="d-flex align-items-center gap-2">
                                    <c:if test="${not empty item.referenceId}">
                                        <a href="${pageContext.request.contextPath}/candidates?search=${item.referenceId}" class="btn btn-sm btn-outline-primary py-0 px-2" style="font-size: 0.75rem;">
                                            <i class="bi bi-person-lines-fill me-1"></i> Xem ứng viên
                                        </a>
                                    </c:if>
                                    <c:if test="${!item.read}">
                                        <form action="${pageContext.request.contextPath}/api/notifications/mark-read" method="POST" class="d-inline m-0">
                                            <input type="hidden" name="id" value="${item.id}">
                                            <button type="submit" class="btn btn-sm btn-link text-secondary text-decoration-none py-0 px-1" style="font-size: 0.75rem;">
                                                Đánh dấu đã đọc
                                            </button>
                                        </form>
                                    </c:if>
                                </div>
                            </div>
                            <c:if test="${!item.read}">
                                <span class="rounded-circle bg-primary flex-shrink-0 mt-2" style="width: 8px; height: 8px;"></span>
                            </c:if>
                        </div>
                    </c:forEach>
                    <c:if test="${empty notifications}">
                        <div class="p-5 text-center text-muted">
                            <i class="bi bi-bell-slash fs-1 text-secondary d-block mb-2"></i>
                            <div class="fw-bold text-dark">Bạn chưa có thông báo nào.</div>
                            <small class="text-muted">Các thông báo quá hạn SLA và lịch trình tuyển dụng sẽ xuất hiện tại đây.</small>
                        </div>
                    </c:if>
                </div>
            </div>
        </div>

    </div>
</main>

<jsp:include page="../common/footer.jsp" />

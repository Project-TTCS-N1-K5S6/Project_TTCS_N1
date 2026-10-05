<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<c:set var="pageTitle" value="Danh mục vai trò" />
<c:set var="breadcrumb" value="Danh mục vai trò" />
<c:set var="activeMenu" value="roles" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <h4 class="fw-bold mb-1">Danh mục vai trò hệ thống</h4>
                <p class="text-muted small mb-0">Các vai trò kiểm soát quyền hạn phân quyền theo chuẩn RBAC trong hệ thống IRMS.</p>
            </div>
            <c:if test="${currentUser.hasPermission('permissions.view') || currentUser.hasRole('ADMIN')}">
                <a href="${pageContext.request.contextPath}/admin/permissions" class="btn btn-outline-primary">
                    <i class="bi bi-diagram-3-fill me-1"></i> Xem ma trận phân quyền
                </a>
            </c:if>
        </div>

        <div class="row g-3">
            <c:forEach var="r" items="${roles}">
                <div class="col-12 col-md-6 col-xl-4">
                    <div class="card h-100 mb-0 shadow-sm border">
                        <div class="card-body">
                            <div class="d-flex justify-content-between align-items-start mb-2">
                                <span class="badge badge-role font-monospace">${r.code}</span>
                                <span class="badge bg-light text-secondary border">
                                    <i class="bi bi-people me-1"></i> ${r.userCount} người dùng
                                </span>
                            </div>
                            <h5 class="card-title fw-bold text-dark mt-2 mb-2">${r.name}</h5>
                            <p class="text-muted small mb-3" style="min-height: 48px;">
                                ${r.description}
                            </p>
                            <div class="d-flex justify-content-between align-items-center pt-2 border-top">
                                <span class="badge bg-secondary-subtle text-secondary small">
                                    <c:choose>
                                        <c:when test="${r.systemRole}">Vai trò hệ thống</c:when>
                                        <c:otherwise>Vai trò tùy biến</c:otherwise>
                                    </c:choose>
                                </span>
                                <a href="${pageContext.request.contextPath}/admin/permissions" class="btn btn-sm btn-link text-decoration-none">
                                    Phân quyền <i class="bi bi-chevron-right"></i>
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>
    </div>

<jsp:include page="../common/footer.jsp" />

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<c:set var="pageTitle" value="${moduleName}" />
<c:set var="breadcrumb" value="${moduleName}" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <div class="card shadow-sm border p-5 text-center my-4">
            <div class="mb-3">
                <span class="display-3 text-primary"><i class="bi bi-gear-wide-connected"></i></span>
            </div>
            <h3 class="fw-bold text-dark mb-2">${moduleName}</h3>
            <p class="text-muted mx-auto" style="max-width: 600px;">
                Phân hệ này đã được thiết kế sẵn sàng trong ma trận phân quyền (RBAC Matrix) và cơ sở dữ liệu MySQL. Giao diện chức năng chi tiết đang trong lộ trình tích hợp tiếp theo.
            </p>
            <div class="d-inline-flex gap-2 justify-content-center mt-3">
                <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-3 py-2 font-monospace">
                    Route: ${requestUri}
                </span>
                <span class="badge bg-success-subtle text-success border border-success-subtle px-3 py-2">
                    <i class="bi bi-shield-check me-1"></i> RBAC Enforced
                </span>
            </div>
            <div class="mt-4">
                <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-outline-secondary">
                    <i class="bi bi-arrow-left me-1"></i> Quay về Bảng điều khiển
                </a>
            </div>
        </div>
    </div>

<jsp:include page="../common/footer.jsp" />

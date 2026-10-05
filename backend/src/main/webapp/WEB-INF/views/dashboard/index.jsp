<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Bảng điều khiển tổng quan" />
<c:set var="breadcrumb" value="Tổng quan hệ thống" />
<c:set var="activeMenu" value="dashboard" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Chào mừng & Nút tác vụ nhanh -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <h4 class="fw-bold mb-1">Xin chào, ${currentUser.fullName}! 👋</h4>
                <p class="text-muted small mb-0">Chào mừng bạn trở lại hệ thống quản lý tuyển dụng nội bộ IRMS.</p>
            </div>
            <div class="d-flex gap-2">
                <a href="${pageContext.request.contextPath}/candidates" class="btn btn-outline-primary btn-sm">
                    <i class="bi bi-person-plus-fill"></i> Quản lý ứng viên
                </a>
                <c:if test="${currentUser.hasPermission('users.view') || currentUser.hasRole('ADMIN')}">
                    <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-primary btn-sm">
                        <i class="bi bi-people-fill"></i> Quản trị tài khoản
                    </a>
                </c:if>
            </div>
        </div>

        <!-- 4 Thẻ chỉ số KPI chính -->
        <div class="row g-3 mb-4">
            <div class="col-12 col-sm-6 col-xl-3">
                <div class="stat-card">
                    <div class="stat-icon primary">
                        <i class="bi bi-people-fill"></i>
                    </div>
                    <div class="stat-content">
                        <div class="stat-title">Tổng người dùng</div>
                        <div class="stat-value">${totalUsers}</div>
                    </div>
                </div>
            </div>

            <div class="col-12 col-sm-6 col-xl-3">
                <div class="stat-card">
                    <div class="stat-icon success">
                        <i class="bi bi-person-lines-fill"></i>
                    </div>
                    <div class="stat-content">
                        <div class="stat-title">Hồ sơ ứng viên</div>
                        <div class="stat-value">${totalCandidates}</div>
                    </div>
                </div>
            </div>

            <div class="col-12 col-sm-6 col-xl-3">
                <div class="stat-card">
                    <div class="stat-icon info">
                        <i class="bi bi-building"></i>
                    </div>
                    <div class="stat-content">
                        <div class="stat-title">Cơ cấu phòng ban</div>
                        <div class="stat-value">${totalDepts}</div>
                    </div>
                </div>
            </div>

            <div class="col-12 col-sm-6 col-xl-3">
                <div class="stat-card">
                    <div class="stat-icon purple">
                        <i class="bi bi-shield-check"></i>
                    </div>
                    <div class="stat-content">
                        <div class="stat-title">Nhật ký bảo mật</div>
                        <div class="stat-value">${totalAuditLogs}</div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 2 Khối dữ liệu chi tiết: Ứng viên mới & Nhật ký gần nhất -->
        <div class="row g-4">
            <!-- Ứng viên mới nộp hồ sơ -->
            <div class="col-12 col-lg-7">
                <div class="card h-100 mb-0">
                    <div class="card-header">
                        <h5 class="card-title">
                            <i class="bi bi-person-vcard text-primary"></i> Ứng viên nộp hồ sơ gần đây
                        </h5>
                        <a href="${pageContext.request.contextPath}/candidates" class="btn btn-sm btn-link text-decoration-none">
                            Xem tất cả <i class="bi bi-chevron-right"></i>
                        </a>
                    </div>
                    <div class="card-body p-0">
                        <div class="table-responsive">
                            <table class="table table-custom">
                                <thead>
                                    <tr>
                                        <th>Ứng viên</th>
                                        <th>Vị trí tuyển dụng</th>
                                        <th>Trạng thái</th>
                                        <th>Ngày nộp</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="c" items="${recentCandidates}">
                                        <tr>
                                            <td>
                                                <div class="fw-semibold text-dark">${c.fullName}</div>
                                                <div class="text-muted text-xs">${c.email}</div>
                                            </td>
                                            <td>${c.requisitionTitle != null ? c.requisitionTitle : 'Kỹ sư Backend'}</td>
                                            <td>
                                                <span class="badge ${c.getStatusBadgeClass()}">${c.getStatusLabel()}</span>
                                            </td>
                                            <td class="text-muted text-xs">
                                                <fmt:formatDate value="${c.createdAt}" pattern="dd/MM/yyyy" />
                                            </td>
                                        </tr>
                                    </c:forEach>
                                    <c:if test="${empty recentCandidates}">
                                        <tr>
                                            <td colspan="4" class="text-center py-4 text-muted">Chưa có ứng viên nào trong hệ thống.</td>
                                        </tr>
                                    </c:if>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Nhật ký kiểm toán bảo mật gần nhất -->
            <div class="col-12 col-lg-5">
                <div class="card h-100 mb-0">
                    <div class="card-header">
                        <h5 class="card-title">
                            <i class="bi bi-clock-history text-info"></i> Nhật ký hoạt động bảo mật
                        </h5>
                        <c:if test="${currentUser.hasPermission('audit.view') || currentUser.hasRole('ADMIN')}">
                            <a href="${pageContext.request.contextPath}/admin/audit-logs" class="btn btn-sm btn-link text-decoration-none">
                                Chi tiết <i class="bi bi-chevron-right"></i>
                            </a>
                        </c:if>
                    </div>
                    <div class="card-body p-3">
                        <ul class="list-group list-group-flush">
                            <c:forEach var="log" items="${recentLogs}">
                                <li class="list-group-item px-0 py-2 border-bottom">
                                    <div class="d-flex justify-content-between align-items-center mb-1">
                                        <span class="badge ${log.getActionBadgeClass()} text-xs">${log.action}</span>
                                        <small class="text-muted text-xs">
                                            <fmt:formatDate value="${log.createdAt}" pattern="HH:mm dd/MM" />
                                        </small>
                                    </div>
                                    <div class="small text-dark">${log.description}</div>
                                    <div class="text-muted text-xs mt-1">
                                        <i class="bi bi-person"></i> ${log.userEmail != null ? log.userEmail : 'Hệ thống'} 
                                        &bull; <i class="bi bi-geo-alt"></i> ${log.ipAddress}
                                    </div>
                                </li>
                            </c:forEach>
                            <c:if test="${empty recentLogs}">
                                <li class="list-group-item text-center py-3 text-muted">Chưa có bản ghi nhật ký.</li>
                            </c:if>
                        </ul>
                    </div>
                </div>
            </div>
        </div>
    </div>

<jsp:include page="../common/footer.jsp" />

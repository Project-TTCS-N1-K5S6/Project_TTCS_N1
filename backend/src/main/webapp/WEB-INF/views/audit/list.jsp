<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Nhật ký kiểm toán" />
<c:set var="breadcrumb" value="Nhật ký kiểm toán" />
<c:set var="activeMenu" value="audit" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <h4 class="fw-bold mb-1">Nhật ký kiểm toán hệ thống (Audit Logs)</h4>
                <p class="text-muted small mb-0">Theo dõi toàn bộ lịch sử đăng nhập, thay đổi phân quyền, cập nhật dữ liệu và sự kiện bảo mật.</p>
            </div>
        </div>

        <!-- Bộ lọc hành động -->
        <div class="filter-bar">
            <form method="GET" action="${pageContext.request.contextPath}/admin/audit-logs" class="row g-2 align-items-center">
                <div class="col-12 col-md-5">
                    <div class="input-group">
                        <span class="input-group-text bg-white border-end-0 text-muted"><i class="bi bi-search"></i></span>
                        <input type="text" class="form-control border-start-0 ps-0" name="action" 
                               placeholder="Tìm theo mã hành động (LOGIN, USER, ROLE...)" value="${paramAction}">
                    </div>
                </div>

                <div class="col-12 col-md-3">
                    <button type="submit" class="btn btn-primary w-100">
                        <i class="bi bi-filter"></i> Lọc nhật ký
                    </button>
                </div>
            </form>
        </div>

        <!-- Bảng danh sách nhật ký -->
        <div class="card mb-4 shadow-sm border">
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-custom">
                        <thead>
                            <tr>
                                <th style="width: 170px;">Thời gian</th>
                                <th>Hành động</th>
                                <th>Đối tượng</th>
                                <th>Người thực hiện</th>
                                <th>Địa chỉ IP</th>
                                <th>Nội dung chi tiết</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="l" items="${logs}">
                                <tr>
                                    <td class="text-muted text-xs">
                                        <i class="bi bi-clock me-1"></i>
                                        <fmt:formatDate value="${l.createdAt}" pattern="yyyy-MM-dd HH:mm:ss" />
                                    </td>
                                    <td>
                                        <span class="badge ${l.getActionBadgeClass()}">${l.action}</span>
                                    </td>
                                    <td>
                                        <span class="badge bg-light text-secondary border font-monospace text-xs">${l.entityType}</span>
                                    </td>
                                    <td>
                                        <div class="fw-semibold text-dark">${l.userFullName != null ? l.userFullName : 'Hệ thống'}</div>
                                        <small class="text-muted text-xs">${l.userEmail != null ? l.userEmail : '-'}</small>
                                    </td>
                                    <td><span class="badge bg-light text-dark border font-monospace text-xs">${l.ipAddress}</span></td>
                                    <td class="small text-dark" style="max-width: 320px;">
                                        ${l.description}
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty logs}">
                                <tr>
                                    <td colspan="6" class="text-center py-5 text-muted">
                                        <i class="bi bi-inbox fs-2 d-block mb-2 text-secondary"></i>
                                        Không tìm thấy bản ghi nhật ký kiểm toán nào.
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <!-- Phân trang -->
                <c:if test="${totalPages > 1}">
                    <div class="p-3 border-top d-flex justify-content-between align-items-center">
                        <small class="text-muted">Tổng số <strong>${totalCount}</strong> sự kiện kiểm toán</small>
                        <nav>
                            <ul class="pagination pagination-sm mb-0">
                                <c:forEach begin="1" end="${totalPages}" var="p">
                                    <li class="page-item ${currentPage == p ? 'active' : ''}">
                                        <a class="page-link" href="${pageContext.request.contextPath}/admin/audit-logs?page=${p}&action=${paramAction}">${p}</a>
                                    </li>
                                </c:forEach>
                            </ul>
                        </nav>
                    </div>
                </c:if>
            </div>
        </div>
    </div>

<jsp:include page="../common/footer.jsp" />

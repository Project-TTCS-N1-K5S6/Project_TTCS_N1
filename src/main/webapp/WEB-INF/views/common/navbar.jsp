<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<header class="app-header">
    <div class="header-left">
        <button class="sidebar-toggle-btn" id="sidebar-toggle" type="button" aria-label="Toggle Sidebar">
            <i class="bi bi-list"></i>
        </button>
        <div class="page-breadcrumb">
            <span class="text-muted fs-6"><i class="bi bi-house-door me-1"></i> IRMS</span>
            <span class="text-muted mx-2">/</span>
            <span class="fw-semibold text-dark fs-6">${breadcrumb != null ? breadcrumb : 'Hệ Thống Tuyển Dụng'}</span>
        </div>
    </div>

    <div class="header-right">
        <!-- 
            ======================================================================
            [US 6 - Đạt]: HIỂN THỊ TÊN VÀ VAI TRÒ NGƯỜI ĐANG ĐĂNG NHẬP
            - Hiển thị avatar ký tự đầu, họ tên đầy đủ: ${currentUser.fullName}
            - Hiển thị danh sách vai trò: ${currentUser.getRolesDisplay()}
            ======================================================================
        -->
        <div class="dropdown">
            <a href="#" class="d-flex align-items-center gap-2 text-decoration-none dropdown-toggle" id="userMenuDropdown" data-bs-toggle="dropdown" aria-expanded="false">
                <div class="user-avatar">
                    <c:choose>
                        <c:when test="${not empty currentUser.fullName}">
                            ${currentUser.fullName.substring(0, 1)}
                        </c:when>
                        <c:otherwise>U</c:otherwise>
                    </c:choose>
                </div>
                <div class="d-none d-md-block text-start lh-1">
                    <div class="fw-semibold text-dark fs-6">${currentUser.fullName}</div>
                    <small class="text-muted">${currentUser.getRolesDisplay()}</small>
                </div>
            </a>
            <ul class="dropdown-menu dropdown-menu-end shadow-sm border-0 py-2" aria-labelledby="userMenuDropdown">
                <li class="px-3 py-2 border-bottom mb-1">
                    <div class="fw-bold">${currentUser.fullName}</div>
                    <div class="text-muted text-xs">${currentUser.email}</div>
                    <div class="mt-1"><span class="badge badge-role">${currentUser.getRolesDisplay()}</span></div>
                </li>
                <li>
                    <a class="dropdown-item py-2" href="${pageContext.request.contextPath}/profile">
                        <i class="bi bi-person-lines-fill me-2 text-primary"></i> Hồ sơ cá nhân
                    </a>
                </li>
                <li>
                    <a class="dropdown-item py-2" href="${pageContext.request.contextPath}/auth/change-password">
                        <i class="bi bi-key me-2 text-primary"></i> Đổi mật khẩu
                    </a>
                </li>
                <li><hr class="dropdown-divider"></li>
                <li>
                    <a class="dropdown-item py-2 text-danger" href="${pageContext.request.contextPath}/auth/logout">
                        <i class="bi bi-box-arrow-right me-2"></i> Đăng xuất
                    </a>
                </li>
            </ul>
        </div>
    </div>
</header>

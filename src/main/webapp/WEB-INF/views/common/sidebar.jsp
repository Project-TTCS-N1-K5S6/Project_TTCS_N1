<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<aside class="app-sidebar" id="app-sidebar">
    <div class="sidebar-header">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand-logo">
            <div class="logo-icon"><i class="bi bi-grid-fill"></i></div>
            <span>IRMS Platform</span>
        </a>
    </div>

    <!-- 
        ======================================================================
        MENU ĐIỀU HƯỚNG CHUẨN RISAPPS THEO HỆ THỐNG PHÂN QUYỀN VAI TRÒ
        - Hỗ trợ màn hình di động 320px - 430px qua drawer toggle
        - Ngôn ngữ: 100% tiếng Việt
        - Đồng bộ phong cách RisApps: icon mỏng, chữ gọn, trạng thái active nổi bật
        ======================================================================
    -->
    <div class="sidebar-menu">
        <div class="menu-header">BẢNG ĐIỀU KHIỂN</div>
        <ul class="nav flex-column mb-3">
            <li class="nav-item">
                <a class="nav-link ${activeMenu == 'dashboard' ? 'active' : ''}" href="${pageContext.request.contextPath}/dashboard">
                    <i class="bi bi-grid-fill"></i>
                    <span>Tổng quan</span>
                </a>
            </li>
            <c:if test="${currentUser.hasPermission('department.view') || currentUser.hasPermission('departments.view') || currentUser.hasRole('ADMIN') || currentUser.hasRole('HR_MANAGER')}">
                <li class="nav-item">
                    <a class="nav-link ${activeMenu == 'departments' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/departments">
                        <i class="bi bi-pie-chart-fill"></i>
                        <span>Phòng ban</span>
                    </a>
                </li>
            </c:if>
            <c:if test="${currentUser.hasPermission('candidates.view') || currentUser.hasRole('ADMIN')}">
                <li class="nav-item">
                    <a class="nav-link ${activeMenu == 'candidates' ? 'active' : ''}" href="${pageContext.request.contextPath}/candidates">
                        <i class="bi bi-people-fill"></i>
                        <span>Danh sách ứng viên</span>
                    </a>
                </li>
            </c:if>

        </ul>

        <div class="menu-header">QUY TRÌNH TUYỂN DỤNG</div>
        <ul class="nav flex-column mb-3">
            <!-- Menu Yêu cầu tuyển dụng -->
            <c:if test="${currentUser.hasPermission('requisitions.view') || currentUser.hasPermission('requisitions.create') || currentUser.hasPermission('requisitions.approve') || currentUser.hasRole('ADMIN')}">
                <li class="nav-item">
                    <a class="nav-link ${activeMenu == 'requisitions' ? 'active' : ''}" href="${pageContext.request.contextPath}/recruitment-requests">
                        <i class="bi bi-file-earmark-text-fill"></i>
                        <span>Yêu cầu tuyển dụng</span>
                    </a>
                </li>
            </c:if>
            <c:if test="${currentUser.hasPermission('job_postings.view') || currentUser.hasPermission('job_postings.manage') || currentUser.hasRole('ADMIN') || currentUser.hasRole('HR_MANAGER') || currentUser.hasRole('RECRUITER')}">
                <li class="nav-item">
                    <a class="nav-link ${activeMenu == 'post-publishing' ? 'active' : ''}" href="${pageContext.request.contextPath}/post-publishing">
                        <i class="bi bi-broadcast-pin"></i>
                        <span>Xuất bản bài đăng</span>
                    </a>
                </li>
            </c:if>
            <c:if test="${currentUser.hasRole('HR_MANAGER') || currentUser.hasRole('ADMIN') || currentUser.hasPermission('salary.view')}">
                <li class="nav-item">
                    <a class="nav-link ${activeMenu == 'salary' ? 'active' : ''}" href="${pageContext.request.contextPath}/salary-ranges">
                        <i class="bi bi-cash-stack"></i>
                        <span>Khai báo dải lương</span>
                    </a>
                </li>
            </c:if>
            <c:if test="${currentUser.hasRole('HR_MANAGER') || currentUser.hasRole('ADMIN') || currentUser.hasPermission('competencies.view') || currentUser.hasPermission('evaluations.manage')}">
                <li class="nav-item">
                    <a class="nav-link ${activeMenu == 'competencies' ? 'active' : ''}" href="${pageContext.request.contextPath}/competency-frameworks">
                        <i class="bi bi-award-fill"></i>
                        <span>Khung năng lực</span>
                    </a>
                </li>
            </c:if>
            <li class="nav-item">
                <a class="nav-link ${activeMenu == 'questions' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/questions">
                    <i class="bi bi-question-square-fill"></i>
                    <span>Ngân hàng câu hỏi</span>
                </a>
            </li>
        </ul>

        <c:if test="${currentUser.hasRole('ADMIN') || currentUser.hasRole('HR_MANAGER') || currentUser.hasPermission('users.view') || currentUser.hasPermission('roles.view') || currentUser.hasPermission('permissions.view') || currentUser.hasPermission('audit.view') || currentUser.hasPermission('company.view') || currentUser.hasPermission('company.manage')}">
            <div class="menu-header">QUẢN TRỊ HỆ THỐNG</div>
            <ul class="nav flex-column mb-3">
                <c:if test="${currentUser.hasPermission('users.view') || currentUser.hasRole('ADMIN')}">
                    <li class="nav-item">
                        <a class="nav-link ${activeMenu == 'users' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/users">
                            <i class="bi bi-person-badge-fill"></i>
                            <span>Tài khoản người dùng</span>
                        </a>
                    </li>
                </c:if>
                <c:if test="${currentUser.hasRole('ADMIN')}">
                    <li class="nav-item">
                        <a class="nav-link ${activeMenu == 'shared-catalogs' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/shared-catalogs">
                            <i class="bi bi-list-check"></i>
                            <span>Danh mục dùng chung</span>
                        </a>
                    </li>
                </c:if>
                <c:if test="${currentUser.hasPermission('roles.view') || currentUser.hasRole('ADMIN')}">
                    <li class="nav-item">
                        <a class="nav-link ${activeMenu == 'roles' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/roles">
                            <i class="bi bi-person-gear"></i>
                            <span>Danh mục vai trò</span>
                        </a>
                    </li>
                </c:if>
                <c:if test="${currentUser.hasPermission('permissions.view') || currentUser.hasRole('ADMIN')}">
                    <li class="nav-item">
                        <a class="nav-link ${activeMenu == 'permissions' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/permissions">
                            <i class="bi bi-diagram-3-fill"></i>
                            <span>Ma trận phân quyền</span>
                        </a>
                    </li>
                </c:if>
                <c:if test="${currentUser.hasPermission('approval_matrix.view') || currentUser.hasRole('ADMIN') || currentUser.hasRole('HR_MANAGER')}">
                    <li class="nav-item">
                        <a class="nav-link ${activeMenu == 'approval-matrix' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/approval-matrix">
                            <i class="bi bi-diagram-2-fill"></i>
                            <span>Ma trận phê duyệt</span>
                        </a>
                    </li>
                </c:if>
                <c:if test="${currentUser.hasPermission('audit.view') || currentUser.hasRole('ADMIN')}">
                    <li class="nav-item">
                        <a class="nav-link ${activeMenu == 'audit' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/audit-logs">
                            <i class="bi bi-clock-history"></i>
                            <span>Nhật ký kiểm toán</span>
                        </a>
                    </li>
                </c:if>
                <c:if test="${currentUser.hasRole('ADMIN') || currentUser.hasRole('HR_MANAGER') || currentUser.hasPermission('company.manage') || currentUser.hasPermission('company.view')}">
                    <li class="nav-item">
                        <a class="nav-link ${activeMenu == 'company-profile' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/company-profile">
                            <i class="bi bi-globe-americas"></i>
                            <span>Giới thiệu công ty</span>
                        </a>
                    </li>
                </c:if>
            </ul>
        </c:if>

        <!-- Thẻ hỗ trợ trực tuyến / Trợ lý CSKH theo thiết kế ảnh tham khảo -->
        <div class="sidebar-live-chat-card mt-3">
            <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Maria" alt="Maria" class="chat-avatar" />
            <div class="lh-1 overflow-hidden">
                <div class="text-xs text-muted">Hỗ trợ trực tuyến</div>
                <div class="fw-bold text-dark text-xs mt-1">Maria CSKH</div>
            </div>
            <span class="chat-status-dot" title="Đang hoạt động"></span>
        </div>

        <!-- Nút Đăng xuất đồng bộ ở cuối thanh điều hướng -->
        <ul class="nav flex-column mt-2 pt-2 border-top">

            <li class="nav-item">
                <a class="nav-link text-danger" href="${pageContext.request.contextPath}/auth/logout">
                    <i class="bi bi-box-arrow-right text-danger"></i>
                    <span>Đăng xuất</span>
                </a>
            </li>
        </ul>
    </div>

    <div class="sidebar-footer">
        <div class="d-flex align-items-center gap-2">
            <span class="badge bg-success-subtle text-success border border-success-subtle">
                <i class="bi bi-circle-fill" style="font-size: 6px;"></i> Hệ thống sẵn sàng
            </span>
            <span class="text-xs text-secondary ms-auto">v1.0.0</span>
        </div>
    </div>
</aside>

<div class="sidebar-backdrop" id="sidebar-backdrop"></div>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<aside class="app-sidebar" id="app-sidebar">
    <div class="sidebar-header">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand-logo">
            <div class="logo-icon"><i class="bi bi-shield-check"></i></div>
            <span>IRMS PLATFORM</span>
        </a>
    </div>

    <!-- 
        ======================================================================
        [US 6]: MENU ĐIỀU HƯỚNG THEO QUYỀN
        - Tiêu chí US 6: Mục menu không thuộc quyền thì không hiển thị.
        - Tiêu chí US 5: Đảm bảo Người phỏng vấn (INTERVIEWER) KHÔNG xem được dải lương.
        - Hỗ trợ màn hình 360px thông qua sidebar-toggle và co giãn CSS responsive.
        ======================================================================
    -->
    <div class="sidebar-menu">
        <div class="menu-header">TỔNG QUAN</div>
        <ul class="nav flex-column mb-3">
            <li class="nav-item">
                <a class="nav-link ${activeMenu == 'dashboard' ? 'active' : ''}" href="${pageContext.request.contextPath}/dashboard">
                    <i class="bi bi-grid-fill"></i>
                    <span>Bảng điều khiển</span>
                </a>
            </li>
        </ul>

        <div class="menu-header">QUY TRÌNH TUYỂN DỤNG</div>
        <ul class="nav flex-column mb-3">
            <!-- Menu Yêu cầu tuyển dụng -->
            <c:if test="${currentUser.hasPermission('requisitions.view') || currentUser.hasPermission('requisitions.create') || currentUser.hasRole('ADMIN')}">
                <li class="nav-item">
                    <a class="nav-link ${activeMenu == 'requisitions' ? 'active' : ''}" href="${pageContext.request.contextPath}/recruitment-requests">
                        <i class="bi bi-file-earmark-text-fill"></i>
                        <span>Yêu cầu tuyển dụng</span>
                    </a>
                </li>
            </c:if>
            <!-- [US 6 Tiêu chí 1]: Menu Hồ sơ ứng viên hiển thị cho người có quyền candidates.view hoặc ADMIN -->
            <c:if test="${currentUser.hasPermission('candidates.view') || currentUser.hasRole('ADMIN')}">
                <li class="nav-item">
                    <a class="nav-link ${activeMenu == 'candidates' ? 'active' : ''}" href="${pageContext.request.contextPath}/candidates">
                        <i class="bi bi-people-fill"></i>
                        <span>Hồ sơ & Pipeline</span>
                    </a>
                </li>
            </c:if>
            <!-- [KN-103]: CHỈ Trưởng phòng Nhân sự (HR_MANAGER) và Quản trị hệ thống (ADMIN) mới thấy menu Khai báo dải lương -->
            <c:if test="${currentUser.hasRole('HR_MANAGER') || currentUser.hasRole('ADMIN') || currentUser.hasPermission('salary.view')}">
                <li class="nav-item">
                    <a class="nav-link ${activeMenu == 'salary' ? 'active' : ''}" href="${pageContext.request.contextPath}/salary-ranges">
                        <i class="bi bi-cash-stack"></i>
                        <span>Khai báo dải lương</span>
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

        <!-- [US 6 Tiêu chí 1]: Menu Quản trị chỉ hiển thị khi tài khoản có ít nhất 1 quyền thuộc phân hệ Quản trị -->
        <c:if test="${currentUser.hasRole('ADMIN') || currentUser.hasPermission('users.view') || currentUser.hasPermission('department.view') || currentUser.hasPermission('roles.view') || currentUser.hasPermission('permissions.view') || currentUser.hasPermission('audit.view')}">
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
                <c:if test="${currentUser.hasPermission('department.view') || currentUser.hasRole('ADMIN')}">
                    <li class="nav-item">
                        <a class="nav-link ${activeMenu == 'departments' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/departments">
                            <i class="bi bi-building-fill"></i>
                            <span>Cơ cấu phòng ban</span>
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
                <c:if test="${currentUser.hasPermission('audit.view') || currentUser.hasRole('ADMIN')}">
                    <li class="nav-item">
                        <a class="nav-link ${activeMenu == 'audit' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/audit-logs">
                            <i class="bi bi-clock-history"></i>
                            <span>Nhật ký kiểm toán</span>
                        </a>
                    </li>
                </c:if>
            </ul>
        </c:if>

        <div class="menu-header">MỞ RỘNG (PHASE 2)</div>
        <ul class="nav flex-column mb-3">
            <li class="nav-item">
                <a class="nav-link text-muted" href="${pageContext.request.contextPath}/interviews">
                    <i class="bi bi-calendar-check"></i>
                    <span>Lịch phỏng vấn</span>
                    <span class="badge bg-secondary-subtle text-secondary ms-auto" style="font-size: 10px;">Soon</span>
                </a>
            </li>
            <li class="nav-item">
                <a class="nav-link text-muted" href="${pageContext.request.contextPath}/offers">
                    <i class="bi bi-envelope-paper"></i>
                    <span>Thư mời nhận việc</span>
                    <span class="badge bg-secondary-subtle text-secondary ms-auto" style="font-size: 10px;">Soon</span>
                </a>
            </li>
            <li class="nav-item">
                <a class="nav-link text-muted" href="${pageContext.request.contextPath}/onboarding">
                    <i class="bi bi-person-check"></i>
                    <span>Tiếp nhận Onboarding</span>
                    <span class="badge bg-secondary-subtle text-secondary ms-auto" style="font-size: 10px;">Soon</span>
                </a>
            </li>
            <li class="nav-item">
                <a class="nav-link text-muted" href="${pageContext.request.contextPath}/reports">
                    <i class="bi bi-bar-chart"></i>
                    <span>Báo cáo hiệu quả</span>
                    <span class="badge bg-secondary-subtle text-secondary ms-auto" style="font-size: 10px;">Soon</span>
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

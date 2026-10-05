<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Quản trị người dùng" />
<c:set var="breadcrumb" value="Quản trị người dùng" />
<c:set var="activeMenu" value="users" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <h4 class="fw-bold mb-1">Quản lý tài khoản người dùng</h4>
                <p class="text-muted small mb-0">Quản trị danh sách nhân viên, gán vai trò, khóa tài khoản và cấp lại mật khẩu.</p>
            </div>
            <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addUserModal">
                <i class="bi bi-person-plus-fill"></i> Thêm tài khoản mới
            </button>
        </div>

        <!-- Bộ lọc & Tìm kiếm -->
        <div class="filter-bar">
            <form method="GET" action="${pageContext.request.contextPath}/admin/users" class="row g-2 align-items-center">
                <div class="col-12 col-md-4">
                    <div class="input-group">
                        <span class="input-group-text bg-white border-end-0 text-muted"><i class="bi bi-search"></i></span>
                        <input type="text" class="form-control border-start-0 ps-0" name="search" 
                               placeholder="Tìm theo tên, email, mã nhân viên..." value="${paramSearch}">
                    </div>
                </div>

                <div class="col-12 col-sm-6 col-md-3">
                    <select class="form-select" name="deptId">
                        <option value="">-- Tất cả phòng ban --</option>
                        <c:forEach var="d" items="${departments}">
                            <option value="${d.id}" ${paramDeptId == d.id ? 'selected' : ''}>${d.name}</option>
                        </c:forEach>
                    </select>
                </div>

                <div class="col-12 col-sm-6 col-md-3">
                    <select class="form-select" name="status">
                        <option value="">-- Tất cả trạng thái --</option>
                        <option value="ACTIVE" ${paramStatus == 'ACTIVE' ? 'selected' : ''}>Đang hoạt động (ACTIVE)</option>
                        <option value="LOCKED" ${paramStatus == 'LOCKED' ? 'selected' : ''}>Đang bị khóa (LOCKED)</option>
                    </select>
                </div>

                <div class="col-12 col-md-2 d-flex gap-2">
                    <button type="submit" class="btn btn-primary flex-fill">
                        <i class="bi bi-funnel-fill"></i> Lọc
                    </button>
                    <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-outline-secondary" title="Đặt lại">
                        <i class="bi bi-arrow-counterclockwise"></i>
                    </a>
                </div>
            </form>
        </div>

        <!-- Bảng danh sách người dùng -->
        <div class="card mb-4">
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-custom">
                        <thead>
                            <tr>
                                <th>Mã NV</th>
                                <th>Họ và tên &amp; Email</th>
                                <th>Phòng ban &amp; Chức danh</th>
                                <th>Vai trò</th>
                                <th>Trạng thái</th>
                                <th>Lần đăng nhập cuối</th>
                                <th class="text-end pe-3">Hành động</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="u" items="${users}">
                                <tr>
                                    <td><span class="badge bg-light text-dark border font-monospace">${u.employeeCode}</span></td>
                                    <td>
                                        <div class="user-cell">
                                            <div class="avatar-sm">
                                                ${u.fullName != null ? u.fullName.substring(0, 1) : 'U'}
                                            </div>
                                            <div class="user-meta">
                                                <div class="user-name">${u.fullName}</div>
                                                <div class="user-email">${u.email}</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <div class="fw-semibold text-dark">${u.departmentName != null ? u.departmentName : 'Chưa phân bổ'}</div>
                                        <small class="text-muted">${u.jobTitle != null ? u.jobTitle : '-'}</small>
                                    </td>
                                    <td>
                                        <c:forEach var="r" items="${u.roles}">
                                            <span class="badge badge-role me-1 mb-1">${r.name}</span>
                                        </c:forEach>
                                        <c:if test="${empty u.roles}">
                                            <span class="text-muted text-xs">Chưa có vai trò</span>
                                        </c:if>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${u.status == 'ACTIVE'}">
                                                <span class="badge badge-active"><i class="bi bi-check-circle-fill me-1"></i> Hoạt động</span>
                                            </c:when>
                                            <c:when test="${u.status == 'LOCKED'}">
                                                <span class="badge badge-locked" title="Lý do: ${u.lockReason}"><i class="bi bi-lock-fill me-1"></i> Bị khóa</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge badge-inactive">${u.status}</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="text-muted text-xs">
                                        <c:choose>
                                            <c:when test="${not empty u.lastLoginAt}">
                                                <fmt:formatDate value="${u.lastLoginAt}" pattern="HH:mm dd/MM/yyyy" />
                                            </c:when>
                                            <c:otherwise>Chưa đăng nhập</c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="text-end pe-3">
                                        <div class="dropdown">
                                            <button class="btn btn-sm btn-outline-secondary dropdown-toggle" type="button" data-bs-toggle="dropdown">
                                                <i class="bi bi-three-dots-vertical"></i>
                                            </button>
                                            <ul class="dropdown-menu dropdown-menu-end shadow-sm">
                                                <li>
                                                    <a class="dropdown-item" href="javascript:void(0)" 
                                                       onclick="openEditModal('${u.id}', '${u.employeeCode}', '${u.fullName}', '${u.phone}', '${u.jobTitle}', '${u.departmentId}', '${u.status}', '${not empty u.roles ? u.roles[0].id : ''}')">
                                                        <i class="bi bi-pencil-square me-2 text-primary"></i> Sửa thông tin
                                                    </a>
                                                </li>
                                                <li>
                                                    <a class="dropdown-item" href="javascript:void(0)"
                                                       onclick="openResetPasswordModal('${u.id}', '${u.fullName}', '${u.email}')">
                                                        <i class="bi bi-key-fill me-2 text-warning"></i> Cấp lại mật khẩu
                                                    </a>
                                                </li>
                                                <li><hr class="dropdown-divider"></li>
                                                <c:choose>
                                                    <c:when test="${u.status == 'LOCKED'}">
                                                        <li>
                                                            <form action="${pageContext.request.contextPath}/admin/users/unlock" method="POST" style="margin:0;">
                                                                <input type="hidden" name="userId" value="${u.id}">
                                                                <button type="submit" class="dropdown-item text-success" onclick="return confirm('Mở khóa tài khoản này?')">
                                                                    <i class="bi bi-unlock-fill me-2"></i> Mở khóa tài khoản
                                                                </button>
                                                            </form>
                                                        </li>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <li>
                                                            <a class="dropdown-item text-danger" href="javascript:void(0)"
                                                               onclick="openLockModal('${u.id}', '${u.fullName}', '${u.email}')">
                                                                <i class="bi bi-lock-fill me-2"></i> Khóa tài khoản
                                                            </a>
                                                        </li>
                                                    </c:otherwise>
                                                </c:choose>
                                            </ul>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty users}">
                                <tr>
                                    <td colspan="7" class="text-center py-5 text-muted">
                                        <i class="bi bi-inbox fs-2 d-block mb-2 text-secondary"></i>
                                        Không tìm thấy người dùng nào phù hợp với điều kiện tìm kiếm.
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>

                <!-- Phân trang -->
                <c:if test="${totalPages > 1}">
                    <div class="p-3 border-top d-flex justify-content-between align-items-center">
                        <small class="text-muted">Tổng cộng <strong>${totalCount}</strong> người dùng</small>
                        <nav>
                            <ul class="pagination pagination-sm mb-0">
                                <c:forEach begin="1" end="${totalPages}" var="p">
                                    <li class="page-item ${currentPage == p ? 'active' : ''}">
                                        <a class="page-link" href="${pageContext.request.contextPath}/admin/users?page=${p}&search=${paramSearch}&deptId=${paramDeptId}&status=${paramStatus}">${p}</a>
                                    </li>
                                </c:forEach>
                            </ul>
                        </nav>
                    </div>
                </c:if>
            </div>
        </div>
    </div>

    <!-- Modal Thêm Người Dùng Mới -->
    <div class="modal fade" id="addUserModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/admin/users/create" method="POST">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold"><i class="bi bi-person-plus text-primary me-2"></i> Thêm tài khoản mới</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row g-3">
                            <div class="col-6">
                                <label class="form-label small fw-semibold">Mã nhân viên <span class="text-danger">*</span></label>
                                <input type="text" class="form-control" name="employeeCode" required placeholder="EMP008">
                            </div>
                            <div class="col-6">
                                <label class="form-label small fw-semibold">Vai trò chính <span class="text-danger">*</span></label>
                                <select class="form-select" name="roleId" required>
                                    <c:forEach var="r" items="${roles}">
                                        <option value="${r.id}">${r.name}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Họ và tên <span class="text-danger">*</span></label>
                                <input type="text" class="form-control" name="fullName" required placeholder="Nguyễn Văn A">
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Email công ty <span class="text-danger">*</span></label>
                                <input type="email" class="form-control" name="email" required placeholder="user@company.local">
                            </div>
                            <div class="col-6">
                                <label class="form-label small fw-semibold">Số điện thoại</label>
                                <input type="text" class="form-control" name="phone" placeholder="0901234567">
                            </div>
                            <div class="col-6">
                                <label class="form-label small fw-semibold">Chức vụ / Vị trí</label>
                                <input type="text" class="form-control" name="jobTitle" placeholder="Chuyên viên...">
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Phòng ban trực thuộc</label>
                                <select class="form-select" name="departmentId">
                                    <option value="">-- Chưa gán phòng ban --</option>
                                    <c:forEach var="d" items="${departments}">
                                        <option value="${d.id}">${d.name}</option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                        <div class="alert alert-info py-2 px-3 mt-3 mb-0 text-xs">
                            <i class="bi bi-info-circle me-1"></i> Mật khẩu khởi tạo mặc định: <strong>Admin@123456</strong>. Người dùng sẽ phải đổi mật khẩu khi đăng nhập lần đầu.
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-primary"><i class="bi bi-check-lg"></i> Tạo tài khoản</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Modal Sửa Người Dùng -->
    <div class="modal fade" id="editUserModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/admin/users/edit" method="POST">
                    <input type="hidden" name="userId" id="editUserId">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold"><i class="bi bi-pencil-square text-primary me-2"></i> Chỉnh sửa thông tin</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row g-3">
                            <div class="col-6">
                                <label class="form-label small fw-semibold">Mã nhân viên</label>
                                <input type="text" class="form-control" id="editEmployeeCode" disabled>
                            </div>
                            <div class="col-6">
                                <label class="form-label small fw-semibold">Vai trò</label>
                                <select class="form-select" name="roleId" id="editRoleId">
                                    <c:forEach var="r" items="${roles}">
                                        <option value="${r.id}">${r.name}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Họ và tên</label>
                                <input type="text" class="form-control" name="fullName" id="editFullName" required>
                            </div>
                            <div class="col-6">
                                <label class="form-label small fw-semibold">Số điện thoại</label>
                                <input type="text" class="form-control" name="phone" id="editPhone">
                            </div>
                            <div class="col-6">
                                <label class="form-label small fw-semibold">Chức vụ</label>
                                <input type="text" class="form-control" name="jobTitle" id="editJobTitle">
                            </div>
                            <div class="col-6">
                                <label class="form-label small fw-semibold">Phòng ban</label>
                                <select class="form-select" name="departmentId" id="editDepartmentId">
                                    <option value="">-- Chưa gán phòng ban --</option>
                                    <c:forEach var="d" items="${departments}">
                                        <option value="${d.id}">${d.name}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-6">
                                <label class="form-label small fw-semibold">Trạng thái</label>
                                <select class="form-select" name="status" id="editStatus">
                                    <option value="ACTIVE">Hoạt động (ACTIVE)</option>
                                    <option value="LOCKED">Bị khóa (LOCKED)</option>
                                </select>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-primary"><i class="bi bi-save"></i> Cập nhật</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Modal Khóa Người Dùng -->
    <div class="modal fade" id="lockUserModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/admin/users/lock" method="POST">
                    <input type="hidden" name="userId" id="lockUserId">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold text-danger"><i class="bi bi-lock-fill me-2"></i> Khóa tài khoản người dùng</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <p class="mb-2">Bạn có chắc chắn muốn khóa tài khoản: <strong id="lockUserName"></strong>?</p>
                        <p class="small text-muted mb-3">Người dùng sẽ bị thu hồi toàn bộ phiên đăng nhập hiện tại ngay lập tức.</p>
                        <div class="mb-3">
                            <label class="form-label small fw-semibold">Lý do khóa tài khoản <span class="text-danger">*</span></label>
                            <textarea class="form-control" name="reason" rows="3" required placeholder="Nhập lý do khóa tài khoản..."></textarea>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-danger"><i class="bi bi-lock-fill"></i> Xác nhận khóa</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Modal Cấp Lại Mật Khẩu -->
    <div class="modal fade" id="resetPasswordModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/admin/users/reset-password" method="POST">
                    <input type="hidden" name="userId" id="resetUserId">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold text-warning"><i class="bi bi-key-fill me-2"></i> Cấp lại mật khẩu tạm thời</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <p>Hệ thống sẽ sinh một mật khẩu ngẫu nhiên mới cho tài khoản: <strong id="resetUserName"></strong>.</p>
                        <p class="small text-muted mb-0">Người dùng sẽ phải đổi mật khẩu mới ở lần đăng nhập tiếp theo.</p>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-warning"><i class="bi bi-arrow-repeat"></i> Cấp lại mật khẩu</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

<script src="${pageContext.request.contextPath}/assets/js/users.js"></script>
<jsp:include page="../common/footer.jsp" />

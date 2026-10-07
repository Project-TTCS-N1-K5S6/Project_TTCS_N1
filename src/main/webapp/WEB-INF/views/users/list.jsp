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
            <div class="d-flex gap-2">
                <button class="btn btn-outline-success" data-bs-toggle="modal" data-bs-target="#importExcelModal">
                    <i class="bi bi-file-earmark-excel me-1"></i> Nhập từ Excel
                </button>
                <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addUserModal">
                    <i class="bi bi-person-plus-fill me-1"></i> Thêm tài khoản mới
                </button>
            </div>
        </div>

        <!-- 
            ======================================================================
            [US 8]: BỘ LỌC TÌM KIẾM TÀI KHOẢN NỘI BỘ
            - Tiêu chí US 8: Tìm theo tên, email, phòng ban; lọc theo vai trò và trạng thái.
            - Điểm đạt: Tìm từ khóa tên/email/mã NV, lọc phòng ban, lọc trạng thái.
            - Điểm cần bổ sung: Cần thêm dropdown lọc theo vai trò (roleId).
            ======================================================================
        -->
        <div class="filter-bar">
            <form method="GET" action="${pageContext.request.contextPath}/admin/users" class="row g-2 align-items-center">
                <div class="col-12 col-md-3 col-xl-3">
                    <div class="input-group">
                        <span class="input-group-text bg-white border-end-0 text-muted"><i class="bi bi-search"></i></span>
                        <input type="text" class="form-control border-start-0 ps-0" name="search" 
                               placeholder="Tìm tên, email, mã NV..." value="${paramSearch}">
                    </div>
                </div>

                <div class="col-6 col-md-2 col-xl-2">
                    <select class="form-select" name="deptId">
                        <option value="">-- Phòng ban --</option>
                        <c:forEach var="d" items="${departments}">
                            <option value="${d.id}" ${paramDeptId == d.id ? 'selected' : ''}>${d.name}</option>
                        </c:forEach>
                    </select>
                </div>

                <!-- [US 8 Tiêu chí 3]: Lọc theo vai trò người dùng -->
                <div class="col-6 col-md-2 col-xl-2">
                    <select class="form-select" name="roleId">
                        <option value="">-- Vai trò --</option>
                        <c:forEach var="r" items="${roles}">
                            <option value="${r.id}" ${paramRoleId == r.id ? 'selected' : ''}>${r.name}</option>
                        </c:forEach>
                    </select>
                </div>

                <div class="col-6 col-md-2 col-xl-2">
                    <select class="form-select" name="status">
                        <option value="">-- Trạng thái --</option>
                        <option value="ACTIVE" ${paramStatus == 'ACTIVE' ? 'selected' : ''}>Hoạt động</option>
                        <option value="LOCKED" ${paramStatus == 'LOCKED' ? 'selected' : ''}>Bị khóa</option>
                    </select>
                </div>

                <div class="col-6 col-md-3 col-xl-3 d-flex gap-2">
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
                    <table class="table table-custom table-fit">
                        <thead>
                            <tr>
                                <th class="d-none d-sm-table-cell" style="width: 85px;">Mã NV</th>
                                <th>Họ và tên &amp; Email</th>
                                <th class="d-none d-md-table-cell">Phòng ban &amp; Chức danh</th>
                                <th>Vai trò</th>
                                <th>Trạng thái</th>
                                <th class="d-none d-lg-table-cell">Đăng nhập cuối</th>
                                <th class="text-end pe-3" style="width: 70px;">Hành động</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="u" items="${users}">
                                <tr>
                                    <td class="d-none d-sm-table-cell"><span class="badge bg-light text-dark border font-monospace">${u.employeeCode}</span></td>
                                    <td>
                                        <div class="user-cell">
                                            <div class="avatar-sm">
                                                ${u.fullName != null ? u.fullName.substring(0, 1) : 'U'}
                                            </div>
                                            <div class="user-meta">
                                                <div class="user-name">${u.fullName}</div>
                                                <div class="user-email">${u.email}</div>
                                                <div class="d-sm-none text-muted text-xs font-monospace mt-1">${u.employeeCode}</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td class="d-none d-md-table-cell">
                                        <div class="fw-semibold text-dark">${u.departmentName != null ? u.departmentName : 'Chưa phân bổ'}</div>
                                        <small class="text-muted">${u.jobTitle != null ? u.jobTitle : '-'}</small>
                                    </td>
                                    <td>
                                        <div class="role-badges">
                                            <c:forEach var="r" items="${u.roles}">
                                                <span class="badge badge-role">${r.name}</span>
                                            </c:forEach>
                                            <c:if test="${empty u.roles}">
                                                <span class="text-muted text-xs">Chưa có</span>
                                            </c:if>
                                        </div>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${u.status == 'ACTIVE'}">
                                                <span class="badge badge-active"><i class="bi bi-check-circle-fill me-1"></i> Hoạt động</span>
                                            </c:when>
                                            <c:when test="${u.status == 'LOCKED'}">
                                                <span class="badge badge-locked" title="Lý do: ${u.lockReason}"><i class="bi bi-lock-fill me-1"></i> Khóa</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge badge-inactive">${u.status}</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="text-muted text-xs d-none d-lg-table-cell">
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
                                                    <c:set var="uRoleIds" value="" />
                                                    <c:forEach var="ur" items="${u.roles}" varStatus="rst">
                                                        <c:set var="uRoleIds" value="${uRoleIds}${ur.id}${rst.last ? '' : ','}" />
                                                    </c:forEach>
                                                    <a class="dropdown-item" href="javascript:void(0)" 
                                                       onclick="openEditModal('${u.id}', '${u.employeeCode}', '${u.fullName}', '${u.phone}', '${u.jobTitle}', '${u.departmentId}', '${u.status}', '${uRoleIds}')">
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
                                        <a class="page-link" href="${pageContext.request.contextPath}/admin/users?page=${p}&search=${paramSearch}&deptId=${paramDeptId}&roleId=${paramRoleId}&status=${paramStatus}">${p}</a>
                                    </li>
                                </c:forEach>
                            </ul>
                        </nav>
                    </div>
                </c:if>
            </div>
        </div>
    </div>

    <!-- 
        ======================================================================
        [US 8 & US 9]: MODAL THÊM TÀI KHOẢN NỘI BỘ
        - Tiêu chí US 8: Tạo tài khoản gửi email kích hoạt kèm mật khẩu tạm (mật khẩu mặc định hiện tại là Admin@123456).
        - Tiêu chí US 9: Một người dùng có thể giữ nhiều vai trò cùng lúc (hiện form đang dùng select 1 vai trò).
        ======================================================================
    -->
    <div class="modal fade" id="addUserModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/admin/users/create" method="POST">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold"><i class="bi bi-person-plus text-primary me-2"></i> Thêm tài khoản mới</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row g-3">
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Mã nhân viên <span class="text-danger">*</span></label>
                                <input type="text" class="form-control" name="employeeCode" required placeholder="EMP008">
                            </div>
                            <!-- [US 9 Tiêu chí 1]: Cho phép chọn một hoặc nhiều vai trò cùng lúc -->
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Vai trò phân quyền <span class="text-danger">*</span> (Có thể chọn nhiều vai trò)</label>
                                <div class="border rounded p-2 bg-light d-flex flex-wrap gap-3">
                                    <c:forEach var="r" items="${roles}">
                                        <div class="form-check">
                                            <input class="form-check-input add-user-role-cb" type="checkbox" name="roleIds" value="${r.id}" id="addRole_${r.id}">
                                            <label class="form-check-label small fw-medium" for="addRole_${r.id}">
                                                ${r.name}
                                            </label>
                                        </div>
                                    </c:forEach>
                                </div>
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Họ và tên <span class="text-danger">*</span></label>
                                <input type="text" class="form-control" name="fullName" required placeholder="Nguyễn Văn A">
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Email công ty <span class="text-danger">*</span></label>
                                <input type="email" class="form-control" name="email" required placeholder="user@company.local">
                            </div>
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Số điện thoại</label>
                                <input type="text" class="form-control" name="phone" placeholder="0901234567">
                            </div>
                            <div class="col-12 col-md-6">
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

    <!-- 
        ======================================================================
        [US 9]: MODAL CHỈNH SỬA THÔNG TIN & GÁN VAI TRÒ
        - Tiêu chí US 9:
          + Một người dùng có thể giữ nhiều vai trò cùng lúc.
          + Không thể tự thu hồi vai trò quản trị của chính mình.
          + Lưu ý: Cần nâng cấp UI sang checkbox danh sách vai trò thay vì select 1 vai trò.
        ======================================================================
    -->
    <div class="modal fade" id="editUserModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/admin/users/edit" method="POST">
                    <input type="hidden" name="userId" id="editUserId">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold"><i class="bi bi-pencil-square text-primary me-2"></i> Chỉnh sửa thông tin</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row g-3">
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Mã nhân viên</label>
                                <input type="text" class="form-control" id="editEmployeeCode" disabled>
                            </div>
                            <!-- [US 9 Tiêu chí 1]: Cập nhật nhiều vai trò cho tài khoản -->
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Vai trò phân quyền <span class="text-danger">*</span> (Có thể gán nhiều vai trò)</label>
                                <div class="border rounded p-2 bg-light d-flex flex-wrap gap-3">
                                    <c:forEach var="r" items="${roles}">
                                        <div class="form-check">
                                            <input class="form-check-input edit-user-role-cb" type="checkbox" name="roleIds" value="${r.id}" id="editRole_${r.id}">
                                            <label class="form-check-label small fw-medium" for="editRole_${r.id}">
                                                ${r.name}
                                            </label>
                                        </div>
                                    </c:forEach>
                                </div>
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Họ và tên</label>
                                <input type="text" class="form-control" name="fullName" id="editFullName" required>
                            </div>
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Số điện thoại</label>
                                <input type="text" class="form-control" name="phone" id="editPhone">
                            </div>
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Chức vụ</label>
                                <input type="text" class="form-control" name="jobTitle" id="editJobTitle">
                            </div>
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Phòng ban</label>
                                <select class="form-select" name="departmentId" id="editDepartmentId">
                                    <option value="">-- Chưa gán phòng ban --</option>
                                    <c:forEach var="d" items="${departments}">
                                        <option value="${d.id}">${d.name}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-12 col-md-6">
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

    <!-- 
        ======================================================================
        [US 10]: MODAL KHÓA TÀI KHOẢN NHÂN SỰ
        - Tiêu chí US 10:
          + Tài khoản bị khóa không đăng nhập được và bị thu hồi phiên đang mở.
          + Bắt buộc ghi lý do khóa (textarea required).
          + Vị trí tuyển dụng do người đó phụ trách được cảnh báo cần bàn giao.
        ======================================================================
    -->
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

    <!-- 
        ======================================================================
        MODAL NHẬP DANH SÁCH NHÂN SỰ TỪ FILE EXCEL
        - Luồng 3 bước: Chọn file -> Xem trước & Kiểm tra dữ liệu -> Kết quả nhập
        - Hỗ trợ kéo thả, kiểm tra 2 lớp trùng, báo lỗi theo dòng, nhập Partial Success
        ======================================================================
    -->
    <div class="modal fade" id="importExcelModal" tabindex="-1" aria-hidden="true" data-bs-backdrop="static">
        <div class="modal-dialog modal-dialog-centered modal-xl">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title fw-bold">
                        <i class="bi bi-file-earmark-excel text-success me-2"></i> Nhập danh sách nhân sự từ Excel
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close" id="btnCloseImportModal"></button>
                </div>

                <div class="modal-body p-4">
                    <!-- BƯỚC 1: CHỌN / KÉO THẢ FILE EXCEL -->
                    <div id="excelStepSelect">
                        <div class="alert alert-light border d-flex flex-wrap justify-content-between align-items-center gap-2 mb-3">
                            <div class="d-flex align-items-center">
                                <i class="bi bi-info-circle-fill text-primary fs-5 me-2"></i>
                                <div>
                                    <div class="fw-semibold text-dark">File mẫu Excel chính thức của hệ thống</div>
                                    <small class="text-muted">Vui lòng sử dụng đúng định dạng cột: Mã NV, Vai trò, Họ tên, Email, SĐT, Chức vụ, Phòng ban.</small>
                                </div>
                            </div>
                            <a href="${pageContext.request.contextPath}/admin/users/excel-template" class="btn btn-sm btn-outline-primary fw-medium">
                                <i class="bi bi-download me-1"></i> Tải file Excel mẫu
                            </a>
                        </div>

                        <!-- Vùng kéo thả file (Dropzone) -->
                        <div id="excelDropzone" class="border border-2 border-dashed rounded-3 p-5 text-center bg-light" style="cursor: pointer; transition: all 0.2s ease;">
                            <i class="bi bi-cloud-arrow-up text-primary" style="font-size: 3rem;"></i>
                            <h6 class="fw-bold mt-2 mb-1">Kéo và thả file Excel vào đây, hoặc <span class="text-primary text-decoration-underline">chọn từ máy tính</span></h6>
                            <p class="text-muted small mb-0">Chỉ chấp nhận file định dạng <strong>.xlsx</strong> (Dung lượng tối đa 5MB)</p>
                            <input type="file" id="excelFileInput" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" class="d-none">
                        </div>

                        <!-- Thông tin file đã chọn -->
                        <div id="selectedFileInfo" class="mt-3 p-3 border rounded-3 bg-white d-none">
                            <div class="d-flex align-items-center justify-content-between">
                                <div class="d-flex align-items-center gap-3">
                                    <div class="p-2 bg-success-subtle text-success rounded-3">
                                        <i class="bi bi-file-earmark-excel fs-4"></i>
                                    </div>
                                    <div>
                                        <div class="fw-bold text-dark" id="selectedFileName">ten_file.xlsx</div>
                                        <small class="text-muted" id="selectedFileSize">0 KB</small>
                                    </div>
                                </div>
                                <button type="button" class="btn btn-sm btn-outline-danger" id="btnRemoveFile">
                                    <i class="bi bi-trash3 me-1"></i> Bỏ chọn file
                                </button>
                            </div>
                        </div>

                        <!-- Thông báo lỗi chọn file -->
                        <div id="fileSelectAlert" class="alert alert-danger mt-3 d-none mb-0"></div>

                        <!-- Spinner Loading khi kiểm tra dữ liệu -->
                        <div id="excelLoading" class="text-center py-4 d-none">
                            <div class="spinner-border text-primary" role="status"></div>
                            <p class="mt-2 text-muted small fw-medium" id="excelLoadingText">Đang đọc và kiểm tra dữ liệu từ file Excel...</p>
                        </div>
                    </div>

                    <!-- BƯỚC 2: XEM TRƯỚC DỮ LIỆU & BÁO LỖI (PREVIEW) -->
                    <div id="excelStepPreview" class="d-none">
                        <div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-3 pb-2 border-bottom">
                            <div>
                                <h6 class="fw-bold mb-1 text-dark">Kết quả kiểm tra dữ liệu</h6>
                                <p class="text-muted small mb-0">Vui lòng rà soát lại thông tin trước khi xác nhận nhập vào hệ thống.</p>
                            </div>
                            <div class="d-flex gap-2">
                                <span class="badge bg-secondary px-3 py-2 fs-6">Tổng số: <strong id="previewTotal">0</strong></span>
                                <span class="badge bg-success px-3 py-2 fs-6">Hợp lệ: <strong id="previewValid">0</strong></span>
                                <span class="badge bg-danger px-3 py-2 fs-6">Lỗi: <strong id="previewErrors">0</strong></span>
                            </div>
                        </div>

                        <!-- Cảnh báo nếu có dòng lỗi -->
                        <div id="previewWarningAlert" class="alert alert-warning py-2 px-3 small mb-3 d-none">
                            <i class="bi bi-exclamation-triangle-fill me-1"></i>
                            <strong>Chú ý:</strong> Những dòng bị đánh dấu lỗi sẽ bị bỏ qua khi nhập dữ liệu. Hệ thống vẫn cho phép nhập các dòng hợp lệ (Partial Success).
                        </div>

                        <!-- Bảng xem trước dữ liệu chi tiết -->
                        <div class="table-responsive border rounded-3 mb-3" style="max-height: 420px; overflow-y: auto;">
                            <table class="table table-sm table-hover align-middle mb-0" id="previewTable">
                                <thead class="table-light sticky-top">
                                    <tr class="small text-muted">
                                        <th class="text-center" style="width: 50px;">STT</th>
                                        <th class="text-center" style="width: 60px;">Dòng</th>
                                        <th style="width: 100px;">Mã NV</th>
                                        <th>Họ và tên</th>
                                        <th>Email công ty</th>
                                        <th>Phòng ban</th>
                                        <th>Vai trò</th>
                                        <th class="text-center" style="width: 90px;">Trạng thái</th>
                                        <th style="min-width: 200px;">Chi tiết lỗi</th>
                                    </tr>
                                </thead>
                                <tbody id="previewTableBody" class="small">
                                    <!-- Render động qua JavaScript -->
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <!-- BƯỚC 3: KẾT QUẢ SAU KHI NHẬP -->
                    <div id="excelStepResult" class="d-none text-center py-3">
                        <div class="mb-3">
                            <i class="bi bi-check-circle-fill text-success" style="font-size: 3.5rem;"></i>
                            <h5 class="fw-bold mt-2">Hoàn tất xử lý nhập danh sách nhân sự!</h5>
                        </div>

                        <div class="row g-3 justify-content-center mb-4">
                            <div class="col-6 col-md-3">
                                <div class="border rounded-3 p-3 bg-light">
                                    <div class="text-muted small">Tổng số dòng</div>
                                    <div class="fs-4 fw-bold text-dark" id="resTotal">0</div>
                                </div>
                            </div>
                            <div class="col-6 col-md-3">
                                <div class="border rounded-3 p-3 bg-success-subtle text-success">
                                    <div class="small fw-semibold">Nhập thành công</div>
                                    <div class="fs-4 fw-bold" id="resSuccess">0</div>
                                </div>
                            </div>
                            <div class="col-6 col-md-3">
                                <div class="border rounded-3 p-3 bg-danger-subtle text-danger">
                                    <div class="small fw-semibold">Bỏ qua do lỗi</div>
                                    <div class="fs-4 fw-bold" id="resSkipped">0</div>
                                </div>
                            </div>
                        </div>

                        <!-- Danh sách dòng bị bỏ qua nếu có -->
                        <div id="resFailedContainer" class="text-start border rounded-3 p-3 bg-light d-none text-xs">
                            <div class="fw-bold text-danger mb-2">
                                <i class="bi bi-x-circle me-1"></i> Danh sách các dòng bị bỏ qua không tạo tài khoản:
                            </div>
                            <div id="resFailedList" style="max-height: 180px; overflow-y: auto;">
                                <!-- Render danh sách dòng bị bỏ qua -->
                            </div>
                        </div>
                    </div>
                </div>

                <div class="modal-footer bg-light">
                    <!-- Nút bấm cho Bước 1 -->
                    <div id="footerStepSelect" class="d-flex justify-content-end gap-2 w-100">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="button" class="btn btn-primary" id="btnValidateExcel" disabled>
                            <i class="bi bi-search me-1"></i> Kiểm tra dữ liệu
                        </button>
                    </div>

                    <!-- Nút bấm cho Bước 2 -->
                    <div id="footerStepPreview" class="d-none justify-content-between align-items-center w-100">
                        <button type="button" class="btn btn-outline-secondary" id="btnBackToSelect">
                            <i class="bi bi-arrow-left me-1"></i> Chọn lại file khác
                        </button>
                        <button type="button" class="btn btn-success" id="btnExecuteImport">
                            <i class="bi bi-cloud-arrow-up-fill me-1"></i> Xác nhận nhập dữ liệu (<span id="btnValidCount">0</span> dòng hợp lệ)
                        </button>
                    </div>

                    <!-- Nút bấm cho Bước 3 -->
                    <div id="footerStepResult" class="d-none justify-content-end w-100">
                        <button type="button" class="btn btn-primary" id="btnFinishReload">
                            <i class="bi bi-check-lg me-1"></i> Hoàn tất &amp; Cập nhật danh sách
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>

<script src="${pageContext.request.contextPath}/assets/js/users.js"></script>
<jsp:include page="../common/footer.jsp" />

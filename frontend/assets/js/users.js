/**
 * ==============================================================================
 * JAVASCRIPT QUẢN TRỊ TÀI KHOẢN NGƯỜI DÙNG (users.js)
 * ==============================================================================
 * Phục vụ các User Story & Subtask Jira:
 * - KN-79: Giao diện Danh sách Tài khoản & Phân trang (mặc định 20 dòng)
 * - KN-80: Thanh Tìm kiếm & Bộ lọc Đa năng (Tên/email/mã NV, Phòng ban, Vai trò, Trạng thái)
 * - KN-81: Màn hình/Modal Tạo mới & Sửa tài khoản (Gán nhiều vai trò)
 * - KN-82: Xử lý Luồng phản hồi người dùng (Toast, cảnh báo, khóa/mở khóa)
 * - KN-83: Gọi API Phân trang & Tìm kiếm Tài khoản
 * - KN-84: Kiểm tra Trùng lặp Email kèm thông báo chi tiết
 * - KN-85: Cơ chế Sinh Mật khẩu tạm & Gửi Email Kích hoạt
 * - KN-86: Gọi API Chỉnh sửa Thông tin Tài khoản
 * ==============================================================================
 */

let allDepartments = [];
let allRoles = [];
let currentPage = 1;
let pageSize = 20; // [KN-79 & US 8]: Mặc định 20 dòng
let currentUser = null;

document.addEventListener('DOMContentLoaded', async () => {
    initHeaderUser();
    await loadDepartments();
    await loadRoles();
    await loadUsers();
    await updateOutboxCount();
});

// 1. Khởi tạo thông tin User trên Header & Quyền đăng xuất
function initHeaderUser() {
    try {
        const raw = localStorage.getItem('irms_user');
        if (raw) {
            currentUser = JSON.parse(raw);
            const nameEl = document.getElementById('headerUserName');
            const roleEl = document.getElementById('headerUserRole');
            const avatarEl = document.getElementById('headerAvatar');

            if (nameEl) nameEl.innerText = currentUser.fullName || 'Quản trị viên';
            if (roleEl) roleEl.innerText = (currentUser.roles && currentUser.roles[0]) ? currentUser.roles[0].name : (currentUser.jobTitle || 'Quản trị hệ thống');
            if (avatarEl) avatarEl.innerText = (currentUser.fullName || 'A').charAt(0).toUpperCase();
        }
    } catch (e) {
        console.error('Lỗi đọc user session:', e);
    }
}

function handleLogout() {
    localStorage.removeItem('irms_user');
    localStorage.removeItem('irms_token');
    showToast('Đang đăng xuất khỏi hệ thống...', 'info');
    setTimeout(() => {
        window.location.href = 'index.html';
    }, 400);
}

// 2. Tải danh mục phòng ban và danh mục vai trò
async function loadDepartments() {
    try {
        const res = await fetch('/api/departments');
        const data = await res.json();
        if (data.success) {
            allDepartments = data.data;
            const filterDept = document.getElementById('filterDept');
            const addDept = document.getElementById('addDepartmentId');
            const editDept = document.getElementById('editDepartmentId');

            allDepartments.forEach(d => {
                const opt1 = new Option(d.name, d.id);
                const opt2 = new Option(d.name, d.id);
                const opt3 = new Option(d.name, d.id);
                if (filterDept) filterDept.add(opt1);
                if (addDept) addDept.add(opt2);
                if (editDept) editDept.add(opt3);
            });
        }
    } catch (e) {
        console.error('Lỗi nạp phòng ban:', e);
    }
}

async function loadRoles() {
    try {
        const res = await fetch('/api/roles');
        const data = await res.json();
        if (data.success) {
            allRoles = data.data;
            const filterRole = document.getElementById('filterRole');
            const addContainer = document.getElementById('addRoleCheckboxes');
            const editContainer = document.getElementById('editRoleCheckboxes');

            if (addContainer) addContainer.innerHTML = '';
            if (editContainer) editContainer.innerHTML = '';

            allRoles.forEach(r => {
                // Dropdown lọc
                if (filterRole) filterRole.add(new Option(r.name, r.id));

                // Checkbox cho Modal Thêm mới
                if (addContainer) {
                    const div = document.createElement('div');
                    div.className = 'form-check';
                    div.innerHTML = `
                        <input class="form-check-input add-role-cb" type="checkbox" value="${r.id}" id="add_role_${r.id}" ${r.id === 'role-003' ? 'checked' : ''}>
                        <label class="form-check-label small fw-medium text-dark" for="add_role_${r.id}">
                            ${r.name}
                        </label>
                    `;
                    addContainer.appendChild(div);
                }

                // Checkbox cho Modal Chỉnh sửa
                if (editContainer) {
                    const div = document.createElement('div');
                    div.className = 'form-check';
                    div.innerHTML = `
                        <input class="form-check-input edit-role-cb" type="checkbox" value="${r.id}" id="edit_role_${r.id}">
                        <label class="form-check-label small fw-medium text-dark" for="edit_role_${r.id}">
                            ${r.name}
                        </label>
                    `;
                    editContainer.appendChild(div);
                }
            });
        }
    } catch (e) {
        console.error('Lỗi nạp danh mục vai trò:', e);
    }
}

// 3. Tải danh sách người dùng phân trang & bộ lọc (KN-79, KN-80, KN-83)
async function loadUsers(page = currentPage) {
    currentPage = page;
    const search = document.getElementById('filterSearch').value.trim();
    const deptId = document.getElementById('filterDept').value;
    const roleId = document.getElementById('filterRole').value;
    const status = document.getElementById('filterStatus').value;

    const tbody = document.getElementById('userTableBody');
    tbody.innerHTML = `
        <tr>
            <td colspan="7" class="text-center py-5 text-muted">
                <div class="spinner-border spinner-border-sm text-primary me-2" role="status"></div>
                Đang nạp dữ liệu người dùng...
            </td>
        </tr>
    `;

    try {
        const query = new URLSearchParams({
            page: currentPage,
            limit: pageSize,
            search: search,
            deptId: deptId,
            roleId: roleId,
            status: status
        });

        const res = await fetch(`/api/users?${query.toString()}`);
        const data = await res.json();

        if (data.success) {
            renderUserTable(data.data);
            renderPagination(data.totalCount, data.totalPages, data.currentPage, data.pageSize);
        } else {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">${data.message}</td></tr>`;
        }
    } catch (e) {
        console.error('Lỗi nạp danh sách tài khoản:', e);
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">Lỗi kết nối máy chủ!</td></tr>`;
    }
}

// Render dữ liệu bảng người dùng
function renderUserTable(users) {
    const tbody = document.getElementById('userTableBody');
    if (!users || users.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center py-5 text-muted">
                    <i class="bi bi-inbox fs-2 d-block mb-2 text-secondary"></i>
                    Không tìm thấy tài khoản người dùng nào phù hợp với điều kiện tìm kiếm.
                </td>
            </tr>
        `;
        return;
    }

    let html = '';
    users.forEach(u => {
        const firstLetter = (u.fullName || 'U').charAt(0).toUpperCase();

        // Render vai trò
        let rolesHtml = '';
        if (u.roleObjects && u.roleObjects.length > 0) {
            rolesHtml = u.roleObjects.map(r => `<span class="badge badge-role me-1 mb-1">${r.name}</span>`).join('');
        } else {
            rolesHtml = '<span class="text-muted text-xs">Chưa gán</span>';
        }

        // Render trạng thái
        let statusHtml = '';
        if (u.status === 'ACTIVE') {
            statusHtml = `<span class="badge badge-active"><i class="bi bi-check-circle-fill me-1"></i> Hoạt động</span>`;
        } else if (u.status === 'LOCKED') {
            statusHtml = `<span class="badge badge-locked" title="Lý do: ${u.lockReason || 'Bị khóa'}"><i class="bi bi-lock-fill me-1"></i> Bị khóa</span>`;
        } else {
            statusHtml = `<span class="badge bg-secondary">${u.status}</span>`;
        }

        const roleIdsParam = JSON.stringify(u.roles || []).replace(/"/g, '&quot;');
        const safeName = (u.fullName || '').replace(/'/g, "\\'");
        const safeEmail = (u.email || '').replace(/'/g, "\\'");

        html += `
            <tr>
                <td><span class="badge bg-light text-dark border font-monospace">${u.employeeCode}</span></td>
                <td>
                    <div class="user-cell">
                        <div class="avatar-sm">${firstLetter}</div>
                        <div class="user-meta">
                            <div class="fw-semibold text-dark">${u.fullName}</div>
                            <div class="text-muted small">${u.email}</div>
                        </div>
                    </div>
                </td>
                <td>
                    <div class="fw-medium text-dark">${u.departmentName || 'Chưa phân bổ'}</div>
                    <small class="text-muted">${u.jobTitle || '-'}</small>
                </td>
                <td><div class="d-flex flex-wrap">${rolesHtml}</div></td>
                <td>${statusHtml}</td>
                <td class="small text-muted">${u.lastLoginAt ? u.lastLoginAt : 'Chưa đăng nhập'}</td>
                <td class="text-end pe-3">
                    <div class="dropdown">
                        <button class="btn btn-sm btn-outline-secondary dropdown-toggle" type="button" data-bs-toggle="dropdown">
                            <i class="bi bi-three-dots-vertical"></i>
                        </button>
                        <ul class="dropdown-menu dropdown-menu-end shadow-sm border-0 py-2">
                            <li>
                                <a class="dropdown-item py-2" href="javascript:void(0)" onclick="openEditModal('${u.id}', '${u.employeeCode}', '${safeName}', '${safeEmail}', '${u.phone || ''}', '${(u.jobTitle || '').replace(/'/g, "\\'")}', '${u.departmentId || ''}', '${u.status}', ${roleIdsParam})">
                                    <i class="bi bi-pencil-square me-2 text-primary"></i> Sửa thông tin
                                </a>
                            </li>
                            <li>
                                <a class="dropdown-item py-2" href="javascript:void(0)" onclick="openResetPasswordModal('${u.id}', '${safeName}', '${safeEmail}')">
                                    <i class="bi bi-key-fill me-2 text-warning"></i> Cấp lại mật khẩu
                                </a>
                            </li>
                            <li><hr class="dropdown-divider"></li>
                            ${u.status === 'LOCKED' ? `
                                <li>
                                    <a class="dropdown-item py-2 text-success" href="javascript:void(0)" onclick="openUnlockModal('${u.id}', '${safeName}', '${safeEmail}')">
                                        <i class="bi bi-unlock-fill me-2"></i> Mở khóa tài khoản
                                    </a>
                                </li>
                            ` : `
                                <li>
                                    <a class="dropdown-item py-2 text-danger" href="javascript:void(0)" onclick="openLockModal('${u.id}', '${safeName}', '${safeEmail}')">
                                        <i class="bi bi-lock-fill me-2"></i> Khóa tài khoản
                                    </a>
                                </li>
                            `}
                        </ul>
                    </div>
                </td>
            </tr>
        `;
    });

    tbody.innerHTML = html;
}

// Render phân trang (KN-79: "Danh sách phân trang, mặc định 20 dòng")
function renderPagination(totalCount, totalPages, curPage, curLimit) {
    const summaryEl = document.getElementById('pageSummary');
    const navEl = document.getElementById('paginationNav');

    if (totalCount === 0) {
        if (summaryEl) summaryEl.innerText = 'Không có bản ghi nào';
        if (navEl) navEl.innerHTML = '';
        return;
    }

    const start = (curPage - 1) * curLimit + 1;
    const end = Math.min(curPage * curLimit, totalCount);
    if (summaryEl) {
        summaryEl.innerHTML = `Hiển thị <strong>${start}</strong> - <strong>${end}</strong> trên tổng số <strong>${totalCount}</strong> tài khoản`;
    }

    let pagesHtml = '';

    // Nút Đầu & Trang trước
    pagesHtml += `
        <li class="page-item ${curPage === 1 ? 'disabled' : ''}">
            <a class="page-link" href="javascript:void(0)" onclick="loadUsers(1)" title="Trang đầu">&laquo;</a>
        </li>
        <li class="page-item ${curPage === 1 ? 'disabled' : ''}">
            <a class="page-link" href="javascript:void(0)" onclick="loadUsers(${curPage - 1})" title="Trang trước">&lsaquo;</a>
        </li>
    `;

    // Các trang số
    for (let p = 1; p <= totalPages; p++) {
        if (p === 1 || p === totalPages || (p >= curPage - 2 && p <= curPage + 2)) {
            pagesHtml += `
                <li class="page-item ${p === curPage ? 'active' : ''}">
                    <a class="page-link" href="javascript:void(0)" onclick="loadUsers(${p})">${p}</a>
                </li>
            `;
        } else if (p === curPage - 3 || p === curPage + 3) {
            pagesHtml += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
    }

    // Nút Trang sau & Trang cuối
    pagesHtml += `
        <li class="page-item ${curPage === totalPages ? 'disabled' : ''}">
            <a class="page-link" href="javascript:void(0)" onclick="loadUsers(${curPage + 1})" title="Trang sau">&rsaquo;</a>
        </li>
        <li class="page-item ${curPage === totalPages ? 'disabled' : ''}">
            <a class="page-link" href="javascript:void(0)" onclick="loadUsers(${totalPages})" title="Trang cuối">&raquo;</a>
        </li>
    `;

    if (navEl) navEl.innerHTML = pagesHtml;
}

// 4. Xử lý Bộ lọc & Kích thước trang
function handleFilterSubmit(e) {
    e.preventDefault();
    loadUsers(1);
}

function resetFilters() {
    document.getElementById('filterSearch').value = '';
    document.getElementById('filterDept').value = '';
    document.getElementById('filterRole').value = '';
    document.getElementById('filterStatus').value = '';
    loadUsers(1);
}

function handlePageSizeChange() {
    pageSize = parseInt(document.getElementById('pageSizeSelect').value, 10);
    loadUsers(1);
}

// 5. Thêm tài khoản mới (KN-81, KN-84, KN-85)
function openAddModal() {
    document.getElementById('addUserForm').reset();
    const alertBox = document.getElementById('addModalAlert');
    alertBox.className = 'alert d-none';
    alertBox.innerHTML = '';

    // Reset role checkboxes
    document.querySelectorAll('.add-role-cb').forEach(cb => {
        cb.checked = (cb.value === 'role-003');
    });

    const modal = new bootstrap.Modal(document.getElementById('addUserModal'));
    modal.show();
}

async function handleAddUserSubmit(e) {
    e.preventDefault();
    const employeeCode = document.getElementById('addEmployeeCode').value.trim();
    const fullName = document.getElementById('addFullName').value.trim();
    const email = document.getElementById('addEmail').value.trim();
    const phone = document.getElementById('addPhone').value.trim();
    const jobTitle = document.getElementById('addJobTitle').value.trim();
    const departmentId = document.getElementById('addDepartmentId').value;

    const selectedRoles = [];
    document.querySelectorAll('.add-role-cb:checked').forEach(cb => {
        selectedRoles.push(cb.value);
    });

    if (selectedRoles.length === 0) {
        showAddModalAlert('Vui lòng chọn ít nhất một vai trò phân quyền.', 'warning');
        return;
    }

    const btnSubmit = document.getElementById('btnAddSubmit');
    btnSubmit.disabled = true;
    btnSubmit.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Đang tạo...`;

    try {
        const res = await fetch('/api/users', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                employeeCode,
                fullName,
                email,
                phone,
                jobTitle,
                departmentId,
                roles: selectedRoles
            })
        });

        const data = await res.json();

        if (res.ok && data.success) {
            // Đóng modal
            bootstrap.Modal.getInstance(document.getElementById('addUserModal')).hide();

            // Hiển thị toast thông báo kèm mật khẩu tạm
            showToast(`
                <strong>Tạo tài khoản thành công!</strong><br>
                Mã NV: <b>${employeeCode}</b> - Email: <b>${email}</b><br>
                Mật khẩu tạm: <span class="badge bg-warning text-dark font-monospace">${data.temporaryPassword}</span><br>
                <small>Email kích hoạt đã được đưa vào hàng đợi.</small>
            `, 'success', 8000);

            await loadUsers(1);
            await updateOutboxCount();
        } else {
            // Thông báo lỗi cụ thể (ví dụ email trùng lặp: KN-84)
            showAddModalAlert(data.message || 'Lỗi khi tạo tài khoản người dùng.', 'danger');
        }
    } catch (err) {
        console.error('Lỗi API tạo user:', err);
        showAddModalAlert('Lỗi kết nối máy chủ khi tạo tài khoản.', 'danger');
    } finally {
        btnSubmit.disabled = false;
        btnSubmit.innerHTML = `<i class="bi bi-check-lg me-1"></i> Tạo tài khoản &amp; Gửi kích hoạt`;
    }
}

function showAddModalAlert(msg, typeClass) {
    const alertBox = document.getElementById('addModalAlert');
    alertBox.className = `alert alert-${typeClass} py-2 px-3 small rounded-3 mb-3 d-flex align-items-center gap-2`;
    alertBox.innerHTML = `<i class="bi bi-exclamation-triangle-fill flex-shrink-0"></i> <div>${msg}</div>`;
}

// 6. Chỉnh sửa tài khoản (KN-81, KN-86)
function openEditModal(id, code, name, email, phone, jobTitle, deptId, status, roles) {
    document.getElementById('editUserId').value = id;
    document.getElementById('editEmployeeCode').value = code;
    document.getElementById('editEmail').value = email;
    document.getElementById('editFullName').value = name;
    document.getElementById('editPhone').value = phone || '';
    document.getElementById('editJobTitle').value = jobTitle || '';
    document.getElementById('editDepartmentId').value = deptId || '';
    document.getElementById('editStatus').value = status || 'ACTIVE';

    const alertBox = document.getElementById('editModalAlert');
    alertBox.className = 'alert d-none';
    alertBox.innerHTML = '';

    const roleArr = Array.isArray(roles) ? roles : [];
    document.querySelectorAll('.edit-role-cb').forEach(cb => {
        cb.checked = roleArr.includes(cb.value);
    });

    const modal = new bootstrap.Modal(document.getElementById('editUserModal'));
    modal.show();
}

async function handleEditUserSubmit(e) {
    e.preventDefault();
    const userId = document.getElementById('editUserId').value;
    const fullName = document.getElementById('editFullName').value.trim();
    const phone = document.getElementById('editPhone').value.trim();
    const jobTitle = document.getElementById('editJobTitle').value.trim();
    const departmentId = document.getElementById('editDepartmentId').value;
    const status = document.getElementById('editStatus').value;

    const selectedRoles = [];
    document.querySelectorAll('.edit-role-cb:checked').forEach(cb => {
        selectedRoles.push(cb.value);
    });

    if (selectedRoles.length === 0) {
        showEditModalAlert('Vui lòng chọn ít nhất một vai trò phân quyền.', 'warning');
        return;
    }

    const btnSubmit = document.getElementById('btnEditSubmit');
    btnSubmit.disabled = true;
    btnSubmit.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Đang lưu...`;

    try {
        const currentAdminId = currentUser ? currentUser.id : 'usr-001';
        const res = await fetch(`/api/users/${userId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                fullName,
                phone,
                jobTitle,
                departmentId,
                status,
                roles: selectedRoles,
                currentAdminId
            })
        });

        const data = await res.json();

        if (res.ok && data.success) {
            bootstrap.Modal.getInstance(document.getElementById('editUserModal')).hide();
            showToast('Cập nhật thông tin tài khoản thành công!', 'success');
            await loadUsers();
        } else {
            showEditModalAlert(data.message || 'Lỗi khi cập nhật thông tin.', 'danger');
        }
    } catch (err) {
        console.error('Lỗi cập nhật tài khoản:', err);
        showEditModalAlert('Lỗi kết nối máy chủ.', 'danger');
    } finally {
        btnSubmit.disabled = false;
        btnSubmit.innerHTML = `<i class="bi bi-save me-1"></i> Lưu thay đổi`;
    }
}

function showEditModalAlert(msg, typeClass) {
    const alertBox = document.getElementById('editModalAlert');
    alertBox.className = `alert alert-${typeClass} py-2 px-3 small rounded-3 mb-3 d-flex align-items-center gap-2`;
    alertBox.innerHTML = `<i class="bi bi-exclamation-triangle-fill flex-shrink-0"></i> <div>${msg}</div>`;
}

// 7. Khóa tài khoản (Bắt buộc lý do: KN-82)
function openLockModal(id, name, email) {
    document.getElementById('lockUserId').value = id;
    document.getElementById('lockUserName').innerText = `${name} (${email})`;
    document.getElementById('lockReason').value = '';
    const modal = new bootstrap.Modal(document.getElementById('lockUserModal'));
    modal.show();
}

async function handleLockUserSubmit(e) {
    e.preventDefault();
    const userId = document.getElementById('lockUserId').value;
    const reason = document.getElementById('lockReason').value.trim();

    if (!reason) {
        alert('Bắt buộc phải nhập lý do khóa tài khoản.');
        return;
    }

    try {
        const currentAdminId = currentUser ? currentUser.id : 'usr-001';
        const res = await fetch(`/api/users/${userId}/lock`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ reason, currentAdminId })
        });
        const data = await res.json();

        if (res.ok && data.success) {
            bootstrap.Modal.getInstance(document.getElementById('lockUserModal')).hide();
            showToast(data.message || 'Đã khóa tài khoản thành công!', 'warning');
            await loadUsers();
        } else {
            alert(data.message || 'Lỗi khi khóa tài khoản.');
        }
    } catch (err) {
        console.error('Lỗi khóa user:', err);
        alert('Lỗi kết nối máy chủ.');
    }
}

// 8. Mở khóa tài khoản (KN-82)
function openUnlockModal(id, name, email) {
    document.getElementById('unlockUserId').value = id;
    document.getElementById('unlockUserName').innerText = `${name} (${email})`;
    const modal = new bootstrap.Modal(document.getElementById('unlockUserModal'));
    modal.show();
}

async function handleUnlockUserSubmit(e) {
    e.preventDefault();
    const userId = document.getElementById('unlockUserId').value;

    try {
        const res = await fetch(`/api/users/${userId}/unlock`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({})
        });
        const data = await res.json();

        if (res.ok && data.success) {
            bootstrap.Modal.getInstance(document.getElementById('unlockUserModal')).hide();
            showToast(data.message || 'Đã mở khóa tài khoản thành công!', 'success');
            await loadUsers();
        } else {
            alert(data.message || 'Lỗi khi mở khóa tài khoản.');
        }
    } catch (err) {
        console.error('Lỗi mở khóa user:', err);
        alert('Lỗi kết nối máy chủ.');
    }
}

// 9. Cấp lại mật khẩu (KN-85)
function openResetPasswordModal(id, name, email) {
    document.getElementById('resetUserId').value = id;
    document.getElementById('resetUserName').innerText = `${name} (${email})`;
    const modal = new bootstrap.Modal(document.getElementById('resetPasswordModal'));
    modal.show();
}

async function handleResetPasswordSubmit(e) {
    e.preventDefault();
    const userId = document.getElementById('resetUserId').value;

    try {
        const res = await fetch(`/api/users/${userId}/reset-password`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({})
        });
        const data = await res.json();

        if (res.ok && data.success) {
            bootstrap.Modal.getInstance(document.getElementById('resetPasswordModal')).hide();
            showToast(`
                <strong>Cấp lại mật khẩu thành công!</strong><br>
                Mật khẩu mới: <span class="badge bg-warning text-dark font-monospace">${data.temporaryPassword}</span><br>
                <small>Thông báo đã được gửi vào hòm thư hệ thống.</small>
            `, 'success', 8000);
            await updateOutboxCount();
        } else {
            alert(data.message || 'Lỗi khi cấp lại mật khẩu.');
        }
    } catch (err) {
        console.error('Lỗi cấp lại mật khẩu:', err);
        alert('Lỗi kết nối máy chủ.');
    }
}

// 10. Hòm thư kích hoạt / Mật khẩu tạm (Outbox Viewer)
async function updateOutboxCount() {
    try {
        const res = await fetch('/api/email-outbox');
        const data = await res.json();
        if (data.success) {
            const countEl = document.getElementById('outboxCount');
            if (countEl) countEl.innerText = data.data.length;
        }
    } catch (e) {}
}

async function openOutboxModal() {
    const tbody = document.getElementById('outboxTableBody');
    tbody.innerHTML = `<tr><td colspan="5" class="text-center py-3 text-muted">Đang tải hòm thư...</td></tr>`;

    const modal = new bootstrap.Modal(document.getElementById('viewOutboxModal'));
    modal.show();

    try {
        const res = await fetch('/api/email-outbox');
        const data = await res.json();
        if (data.success) {
            if (data.data.length === 0) {
                tbody.innerHTML = `<tr><td colspan="5" class="text-center py-3 text-muted">Chưa có email nào trong hòm thư.</td></tr>`;
                return;
            }

            let html = '';
            data.data.forEach(item => {
                html += `
                    <tr>
                        <td class="small text-muted">${item.sentAt || '-'}</td>
                        <td>
                            <div class="fw-semibold">${item.fullName || ''}</div>
                            <small class="text-muted">${item.recipientEmail}</small>
                        </td>
                        <td>${item.subject}</td>
                        <td>
                            <span class="badge bg-warning text-dark font-monospace fs-6 px-2 py-1">${item.temporaryPassword || 'Admin@123456'}</span>
                        </td>
                        <td>
                            <span class="badge bg-success-subtle text-success border border-success-subtle">
                                <i class="bi bi-check2-all me-1"></i> ${item.status}
                            </span>
                        </td>
                    </tr>
                `;
            });
            tbody.innerHTML = html;
        }
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center text-danger py-3">Lỗi tải hòm thư.</td></tr>`;
    }
}

// 11. Toast thông báo (KN-82)
function showToast(htmlContent, type = 'primary', duration = 5000) {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const id = 'toast_' + Date.now();
    const toast = document.createElement('div');
    toast.id = id;
    toast.className = `toast align-items-center text-bg-${type === 'warning' ? 'warning' : (type === 'danger' ? 'danger' : (type === 'success' ? 'success' : 'primary'))} border-0 shadow-lg mb-2`;
    toast.setAttribute('role', 'alert');
    toast.setAttribute('aria-live', 'assertive');
    toast.setAttribute('aria-atomic', 'true');

    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">
                ${htmlContent}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
        </div>
    `;

    container.appendChild(toast);
    const bsToast = new bootstrap.Toast(toast, { delay: duration });
    bsToast.show();

    toast.addEventListener('hidden.bs.toast', () => {
        toast.remove();
    });
}

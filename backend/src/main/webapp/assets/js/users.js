/**
 * ============================================================
 * IRMS - Client-Side JavaScript: User Management
 * File: src/main/webapp/assets/js/users.js
 * ============================================================
 */

function openEditModal(id, code, name, phone, jobTitle, deptId, status, roleId) {
    document.getElementById('editUserId').value = id;
    document.getElementById('editEmployeeCode').value = code;
    document.getElementById('editFullName').value = name;
    document.getElementById('editPhone').value = phone || '';
    document.getElementById('editJobTitle').value = jobTitle || '';
    document.getElementById('editDepartmentId').value = deptId || '';
    document.getElementById('editStatus').value = status;
    if (roleId) {
        document.getElementById('editRoleId').value = roleId;
    }
    const modal = new bootstrap.Modal(document.getElementById('editUserModal'));
    modal.show();
}

function openLockModal(id, name, email) {
    document.getElementById('lockUserId').value = id;
    document.getElementById('lockUserName').innerText = name + ' (' + email + ')';
    const modal = new bootstrap.Modal(document.getElementById('lockUserModal'));
    modal.show();
}

function openResetPasswordModal(id, name, email) {
    document.getElementById('resetUserId').value = id;
    document.getElementById('resetUserName').innerText = name + ' (' + email + ')';
    const modal = new bootstrap.Modal(document.getElementById('resetPasswordModal'));
    modal.show();
}

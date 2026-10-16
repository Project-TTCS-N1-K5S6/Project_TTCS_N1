/**
 * ==============================================================================
 * JAVASCRIPT ĐIỀU KHIỂN GIAO DIỆN QUẢN LÝ NGƯỜI DÙNG (users.js)
 * ==============================================================================
 * Phục vụ các User Story:
 * - US 8: Quản trị tài khoản, reset mật khẩu tạm thời.
 * - US 9: Gán/sửa vai trò người dùng (Lưu ý: hỗ trợ đa vai trò).
 * - US 10: Khóa tài khoản, thu hồi phiên làm việc tức thì.
 * ==============================================================================
 */

/**
 * [US 9]: Mở Modal chỉnh sửa thông tin người dùng và phân quyền đa vai trò
 * - Đổ dữ liệu từ hàng được chọn vào form sửa.
 * - Đánh dấu các checkbox vai trò mà người dùng đang sở hữu.
 */
function openEditModal(id, code, name, phone, jobTitle, deptId, status, roleIdsStr) {
    document.getElementById('editUserId').value = id;
    document.getElementById('editEmployeeCode').value = code;
    document.getElementById('editFullName').value = name;
    document.getElementById('editPhone').value = phone || '';
    document.getElementById('editJobTitle').value = jobTitle || '';
    document.getElementById('editDepartmentId').value = deptId || '';
    document.getElementById('editStatus').value = status;

    // Reset toàn bộ checkbox vai trò
    const cbs = document.querySelectorAll('.edit-user-role-cb');
    cbs.forEach(cb => cb.checked = false);

    // Kích hoạt các checkbox của những vai trò người dùng đang nắm giữ
    if (roleIdsStr) {
        const ids = roleIdsStr.split(',');
        ids.forEach(rid => {
            const cb = document.getElementById('editRole_' + rid.trim());
            if (cb) cb.checked = true;
        });
    }

    const modal = new bootstrap.Modal(document.getElementById('editUserModal'));
    modal.show();
}

/**
 * [US 10]: Mở Modal khóa tài khoản nhân sự
 * - Yêu cầu nhập lý do bắt buộc trước khi submit lên server.
 */
function openLockModal(id, name, email) {
    document.getElementById('lockUserId').value = id;
    document.getElementById('lockUserName').innerText = name + ' (' + email + ')';
    const modal = new bootstrap.Modal(document.getElementById('lockUserModal'));
    modal.show();
}

/**
 * [US 8]: Mở Modal cấp lại mật khẩu tạm thời cho nhân viên
 */
function openResetPasswordModal(id, name, email) {
    document.getElementById('resetUserId').value = id;
    document.getElementById('resetUserName').innerText = name + ' (' + email + ')';
    const modal = new bootstrap.Modal(document.getElementById('resetPasswordModal'));
    modal.show();
}


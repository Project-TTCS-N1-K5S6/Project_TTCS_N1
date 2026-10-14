/**
 * ============================================================
 * IRMS - Client-Side JavaScript: Permission Matrix
 * File: src/main/webapp/assets/js/matrix.js
 * ============================================================
 */

/**
 * Chức năng: Chọn hoặc bỏ chọn đồng loạt tất cả các checkbox quyền thuộc về một phân hệ (Module)
 *
 * @param {string} moduleName Tên phân hệ (users, roles, candidates...)
 * @param {HTMLInputElement} checkbox Ô checkbox tổng của module
 */
function toggleModuleCheckboxes(moduleName, checkbox) {
    const isChecked = checkbox.checked;
    const checkboxes = document.querySelectorAll(`input[data-module="${moduleName}"]`);
    checkboxes.forEach(cb => {
        cb.checked = isChecked;
    });
}

/**
 * Chức năng: Lưu cấu hình phân quyền của vai trò qua AJAX (POST), hiển thị trạng thái spinner và thông báo toast kết quả
 *
 * @param {string} roleId ID của vai trò cần gán quyền
 * @param {HTMLFormElement} formElement Biểu mẫu chứa danh sách quyền được chọn
 * @returns {boolean} Luôn trả về false để ngăn chặn reload trang mặc định của form
 */
function saveRolePermissions(roleId, formElement) {
    const formData = new FormData(formElement);
    const saveBtn = formElement.querySelector('button[type="submit"]');
    const originalText = saveBtn.innerHTML;
    saveBtn.disabled = true;
    saveBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang lưu...';

    fetch(formElement.action, {
        method: 'POST',
        body: new URLSearchParams(formData),
        headers: {
            'X-Requested-With': 'XMLHttpRequest'
        }
    })
    .then(res => res.json())
    .then(data => {
        saveBtn.disabled = false;
        saveBtn.innerHTML = originalText;
        if (data.success) {
            window.showToast(data.message || 'Cập nhật phân quyền thành công!', 'success');
        } else {
            window.showToast('Lỗi: ' + (data.message || 'Không thể lưu phân quyền'), 'error');
        }
    })
    .catch(err => {
        saveBtn.disabled = false;
        saveBtn.innerHTML = originalText;
        window.showToast('Lỗi kết nối máy chủ!', 'error');
    });

    return false;
}

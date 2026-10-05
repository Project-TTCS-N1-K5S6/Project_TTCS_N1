/**
 * ============================================================
 * IRMS - Client-Side JavaScript: Permission Matrix
 * File: src/main/webapp/assets/js/matrix.js
 * ============================================================
 */

function toggleModuleCheckboxes(moduleName, checkbox) {
    const isChecked = checkbox.checked;
    const checkboxes = document.querySelectorAll(`input[data-module="${moduleName}"]`);
    checkboxes.forEach(cb => {
        cb.checked = isChecked;
    });
}

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
            alert(data.message || 'Cập nhật phân quyền thành công!');
        } else {
            alert('Lỗi: ' + (data.message || 'Không thể lưu phân quyền'));
        }
    })
    .catch(err => {
        saveBtn.disabled = false;
        saveBtn.innerHTML = originalText;
        alert('Lỗi kết nối máy chủ!');
    });

    return false;
}

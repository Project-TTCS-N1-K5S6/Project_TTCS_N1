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

/**
 * ==============================================================================
 * LOGIC ĐIỀU KHIỂN MODAL NHẬP DANH SÁCH NHÂN SỰ TỪ FILE EXCEL
 * ==============================================================================
 */
document.addEventListener('DOMContentLoaded', function () {
    const dropzone = document.getElementById('excelDropzone');
    const fileInput = document.getElementById('excelFileInput');
    const selectedFileInfo = document.getElementById('selectedFileInfo');
    const selectedFileName = document.getElementById('selectedFileName');
    const selectedFileSize = document.getElementById('selectedFileSize');
    const btnRemoveFile = document.getElementById('btnRemoveFile');
    const btnValidateExcel = document.getElementById('btnValidateExcel');
    const fileSelectAlert = document.getElementById('fileSelectAlert');
    const excelLoading = document.getElementById('excelLoading');
    const excelLoadingText = document.getElementById('excelLoadingText');

    const stepSelect = document.getElementById('excelStepSelect');
    const stepPreview = document.getElementById('excelStepPreview');
    const stepResult = document.getElementById('excelStepResult');

    const footerSelect = document.getElementById('footerStepSelect');
    const footerPreview = document.getElementById('footerStepPreview');
    const footerResult = document.getElementById('footerStepResult');

    const btnBackToSelect = document.getElementById('btnBackToSelect');
    const btnExecuteImport = document.getElementById('btnExecuteImport');
    const btnFinishReload = document.getElementById('btnFinishReload');
    const btnCloseImportModal = document.getElementById('btnCloseImportModal');

    const previewTotal = document.getElementById('previewTotal');
    const previewValid = document.getElementById('previewValid');
    const previewErrors = document.getElementById('previewErrors');
    const btnValidCount = document.getElementById('btnValidCount');
    const previewTableBody = document.getElementById('previewTableBody');
    const previewWarningAlert = document.getElementById('previewWarningAlert');

    const resTotal = document.getElementById('resTotal');
    const resSuccess = document.getElementById('resSuccess');
    const resSkipped = document.getElementById('resSkipped');
    const resFailedContainer = document.getElementById('resFailedContainer');
    const resFailedList = document.getElementById('resFailedList');

    if (!dropzone || !fileInput) return;

    let selectedFile = null;

    // 1. Kéo thả file Excel
    dropzone.addEventListener('click', function () {
        fileInput.click();
    });

    dropzone.addEventListener('dragover', function (e) {
        e.preventDefault();
        dropzone.classList.add('border-primary', 'bg-primary-subtle');
    });

    dropzone.addEventListener('dragleave', function (e) {
        e.preventDefault();
        dropzone.classList.remove('border-primary', 'bg-primary-subtle');
    });

    dropzone.addEventListener('drop', function (e) {
        e.preventDefault();
        dropzone.classList.remove('border-primary', 'bg-primary-subtle');
        if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
            handleFile(e.dataTransfer.files[0]);
        }
    });

    fileInput.addEventListener('change', function () {
        if (fileInput.files && fileInput.files.length > 0) {
            handleFile(fileInput.files[0]);
        }
    });

    function handleFile(file) {
        fileSelectAlert.classList.add('d-none');
        fileSelectAlert.innerText = '';

        if (!file.name.toLowerCase().endsWith('.xlsx')) {
            showFileError('Định dạng file không hợp lệ! Vui lòng chỉ chọn file Excel (.xlsx).');
            resetFile();
            return;
        }

        const maxSizeBytes = 5 * 1024 * 1024; // 5MB
        if (file.size > maxSizeBytes) {
            showFileError('Dung lượng file vượt quá giới hạn 5MB (' + formatBytes(file.size) + ').');
            resetFile();
            return;
        }

        selectedFile = file;
        selectedFileName.innerText = file.name;
        selectedFileSize.innerText = formatBytes(file.size);
        selectedFileInfo.classList.remove('d-none');
        dropzone.classList.add('d-none');
        btnValidateExcel.disabled = false;
    }

    function resetFile() {
        selectedFile = null;
        fileInput.value = '';
        selectedFileInfo.classList.add('d-none');
        dropzone.classList.remove('d-none');
        btnValidateExcel.disabled = true;
    }

    if (btnRemoveFile) {
        btnRemoveFile.addEventListener('click', function () {
            resetFile();
        });
    }

    function showFileError(msg) {
        fileSelectAlert.innerText = msg;
        fileSelectAlert.classList.remove('d-none');
    }

    function formatBytes(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    function escapeHtml(text) {
        if (!text) return '';
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.replace(/[&<>"']/g, function (m) { return map[m]; });
    }

    // 2. Bước 1 -> Bước 2: Kiểm tra dữ liệu (Validate)
    if (btnValidateExcel) {
        btnValidateExcel.addEventListener('click', function () {
            if (!selectedFile) return;

            fileSelectAlert.classList.add('d-none');
            excelLoadingText.innerText = 'Đang đọc và kiểm tra dữ liệu từ file Excel...';
            excelLoading.classList.remove('d-none');
            btnValidateExcel.disabled = true;

            const formData = new FormData();
            formData.append('file', selectedFile);

            fetch(window.location.pathname.replace(/\/+$/, '') + '/validate-excel', {
                method: 'POST',
                body: formData
            })
            .then(async res => {
                const text = await res.text();
                try {
                    return JSON.parse(text);
                } catch (e) {
                    throw new Error('Máy chủ phản hồi không đúng định dạng JSON (Mã HTTP: ' + res.status + ')');
                }
            })
            .then(data => {
                excelLoading.classList.add('d-none');
                btnValidateExcel.disabled = false;

                if (!data.success) {
                    showFileError(data.message || 'Có lỗi xảy ra khi kiểm tra dữ liệu.');
                    return;
                }

                renderPreview(data.data);
            })
            .catch(err => {
                excelLoading.classList.add('d-none');
                btnValidateExcel.disabled = false;
                showFileError('Không thể xử lý yêu cầu: ' + err.message);
            });
        });
    }

    function renderPreview(previewData) {
        const total = previewData.totalRows || 0;
        const valid = previewData.validRows || 0;
        const errors = previewData.errorRows || 0;
        const rows = previewData.rows || [];

        previewTotal.innerText = total;
        previewValid.innerText = valid;
        previewErrors.innerText = errors;
        btnValidCount.innerText = valid;

        if (errors > 0) {
            previewWarningAlert.classList.remove('d-none');
        } else {
            previewWarningAlert.classList.add('d-none');
        }

        previewTableBody.innerHTML = '';
        rows.forEach((row, idx) => {
            const tr = document.createElement('tr');
            if (!row.valid) {
                tr.classList.add('table-danger');
            }

            let errorHtml = '';
            if (row.errors && row.errors.length > 0) {
                errorHtml = '<ul class="mb-0 ps-3 text-danger fw-medium">' +
                    row.errors.map(e => '<li>' + escapeHtml(e) + '</li>').join('') +
                    '</ul>';
            } else {
                errorHtml = '<span class="text-muted fst-italic">-</span>';
            }

            let statusBadge = row.valid
                ? '<span class="badge bg-success-subtle text-success border border-success px-2 py-1"><i class="bi bi-check-circle-fill me-1"></i>Hợp lệ</span>'
                : '<span class="badge bg-danger-subtle text-danger border border-danger px-2 py-1"><i class="bi bi-exclamation-circle-fill me-1"></i>Lỗi</span>';

            tr.innerHTML = `
                <td class="text-center text-muted">\${idx + 1}</td>
                <td class="text-center font-monospace fw-bold text-secondary">\${row.rowNumber}</td>
                <td class="font-monospace fw-bold">\${escapeHtml(row.employeeCode || '-')}</td>
                <td class="fw-semibold text-dark">\${escapeHtml(row.fullName || '-')}</td>
                <td>\${escapeHtml(row.email || '-')}</td>
                <td>\${escapeHtml(row.departmentName || 'Chưa gán')}</td>
                <td><span class="badge bg-light text-dark border">\${escapeHtml(row.rolesRaw || '-')}</span></td>
                <td class="text-center">\${statusBadge}</td>
                <td>\${errorHtml}</td>
            `;
            previewTableBody.appendChild(tr);
        });

        // Bật/tắt nút Import
        btnExecuteImport.disabled = (valid === 0);

        // Chuyển view
        stepSelect.classList.add('d-none');
        footerSelect.classList.add('d-none');
        footerSelect.classList.remove('d-flex');

        stepPreview.classList.remove('d-none');
        footerPreview.classList.remove('d-none');
        footerPreview.classList.add('d-flex');
    }

    // Quay lại bước chọn file
    if (btnBackToSelect) {
        btnBackToSelect.addEventListener('click', function () {
            stepPreview.classList.add('d-none');
            footerPreview.classList.add('d-none');
            footerPreview.classList.remove('d-flex');

            stepSelect.classList.remove('d-none');
            footerSelect.classList.remove('d-none');
            footerSelect.classList.add('d-flex');
        });
    }

    // 3. Bước 2 -> Bước 3: Xác nhận nhập dữ liệu (Execute Import)
    if (btnExecuteImport) {
        btnExecuteImport.addEventListener('click', function () {
            const originalText = btnExecuteImport.innerHTML;
            btnExecuteImport.disabled = true;
            btnExecuteImport.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span>Đang tạo tài khoản...';
            btnBackToSelect.disabled = true;

            fetch(window.location.pathname.replace(/\/+$/, '') + '/import-excel', {
                method: 'POST'
            })
            .then(async res => {
                const text = await res.text();
                try {
                    return JSON.parse(text);
                } catch (e) {
                    throw new Error('Máy chủ phản hồi không đúng định dạng JSON (Mã HTTP: ' + res.status + ')');
                }
            })
            .then(data => {
                btnExecuteImport.innerHTML = originalText;
                btnExecuteImport.disabled = false;
                btnBackToSelect.disabled = false;

                if (!data.success) {
                    alert('Lỗi nhập dữ liệu: ' + (data.message || 'Không xác định'));
                    return;
                }

                renderResult(data.data);
            })
            .catch(err => {
                btnExecuteImport.innerHTML = originalText;
                btnExecuteImport.disabled = false;
                btnBackToSelect.disabled = false;
                alert('Không thể hoàn tất nhập dữ liệu: ' + err.message);
            });
        });
    }

    function renderResult(resultData) {
        const total = resultData.totalRows || 0;
        const success = resultData.successCount || 0;
        const skipped = resultData.skippedCount || 0;
        const failedList = resultData.failedList || [];

        resTotal.innerText = total;
        resSuccess.innerText = success;
        resSkipped.innerText = skipped;

        if (failedList.length > 0) {
            resFailedContainer.classList.remove('d-none');
            resFailedList.innerHTML = '';
            failedList.forEach(item => {
                const div = document.createElement('div');
                div.className = 'border-bottom py-1';
                const errText = (item.errors && item.errors.length > 0) ? item.errors.join('; ') : 'Lỗi không xác định';
                div.innerHTML = `<strong>Dòng \${item.rowNumber}:</strong> \${escapeHtml(item.fullName || item.email || item.employeeCode || 'Không tên')} - <span class="text-danger">\${escapeHtml(errText)}</span>`;
                resFailedList.appendChild(div);
            });
        } else {
            resFailedContainer.classList.add('d-none');
        }

        // Chuyển view
        stepPreview.classList.add('d-none');
        footerPreview.classList.add('d-none');
        footerPreview.classList.remove('d-flex');

        stepResult.classList.remove('d-none');
        footerResult.classList.remove('d-none');
        footerResult.classList.add('d-flex');
    }

    // 4. Bước 3: Hoàn tất & Reload danh sách
    if (btnFinishReload) {
        btnFinishReload.addEventListener('click', function () {
            window.location.reload();
        });
    }

    // Reset modal khi đóng
    const importModalElement = document.getElementById('importExcelModal');
    if (importModalElement) {
        importModalElement.addEventListener('hidden.bs.modal', function () {
            resetFile();
            stepSelect.classList.remove('d-none');
            stepPreview.classList.add('d-none');
            stepResult.classList.add('d-none');

            footerSelect.classList.remove('d-none');
            footerSelect.classList.add('d-flex');
            footerPreview.classList.add('d-none');
            footerPreview.classList.remove('d-flex');
            footerResult.classList.add('d-none');
            footerResult.classList.remove('d-flex');
        });
    }
});



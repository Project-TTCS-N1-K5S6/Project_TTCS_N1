/**
 * ============================================================
 * IRMS - Candidate Pipeline Management
 * File: frontend/assets/js/candidates.js
 * ============================================================
 */

function updateCandidateStatus(candidateId, newStatus, selectElement) {
    const parentRow = selectElement.closest('tr');
    const badgeEl = parentRow ? parentRow.querySelector('.badge') : null;

    // Cập nhật giao diện tạm thời
    if (badgeEl) {
        badgeEl.className = 'badge bg-secondary';
        badgeEl.innerText = 'Đang lưu...';
    }

    // Gửi request cập nhật
    fetch('candidates/update-status', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-Requested-With': 'XMLHttpRequest'
        },
        body: new URLSearchParams({
            candidateId: candidateId,
            status: newStatus
        })
    })
    .then(response => {
        if (response.ok) {
            if (badgeEl) {
                const badgeClasses = {
                    'APPLIED': 'badge bg-primary',
                    'SCREENING': 'badge bg-info',
                    'INTERVIEWING': 'badge bg-warning text-dark',
                    'OFFER': 'badge bg-purple',
                    'HIRED': 'badge bg-success',
                    'REJECTED': 'badge bg-danger'
                };
                const labels = {
                    'APPLIED': 'Mới ứng tuyển',
                    'SCREENING': 'Sàng lọc CV',
                    'INTERVIEWING': 'Đang phỏng vấn',
                    'OFFER': 'Đã gửi Offer',
                    'HIRED': 'Đã trúng tuyển',
                    'REJECTED': 'Đã từ chối'
                };
                badgeEl.className = badgeClasses[newStatus] || 'badge bg-secondary';
                badgeEl.innerText = labels[newStatus] || newStatus;
            }
        } else {
            alert('Không thể cập nhật trạng thái ứng viên. Vui lòng thử lại!');
        }
    })
    .catch(() => {
        // Fallback tự submit nếu đang chạy form thuần
        if (selectElement.form) {
            selectElement.form.submit();
        }
    });
}

window.updateCandidateStatus = updateCandidateStatus;

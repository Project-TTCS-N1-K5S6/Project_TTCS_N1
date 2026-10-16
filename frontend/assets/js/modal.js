/**
 * ============================================================
 * IRMS - Reusable Modal Handler
 * File: frontend/assets/js/modal.js
 * ============================================================
 */

const ModalHandler = {
    /**
     * Mở modal theo ID
     */
    open(modalId) {
        const modalEl = document.getElementById(modalId);
        if (modalEl) {
            const bsModal = bootstrap.Modal.getOrCreateInstance(modalEl);
            bsModal.show();
        }
    },

    /**
     * Đóng modal theo ID
     */
    close(modalId) {
        const modalEl = document.getElementById(modalId);
        if (modalEl) {
            const bsModal = bootstrap.Modal.getInstance(modalEl);
            if (bsModal) bsModal.hide();
        }
    },

    /**
     * Xác nhận hành động nguy hiểm trước khi gửi form
     */
    confirmAction(message = 'Bạn có chắc chắn muốn thực hiện thao tác này?') {
        return window.confirm(message);
    }
};

window.ModalHandler = ModalHandler;

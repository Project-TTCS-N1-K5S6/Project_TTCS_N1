/**
 * ============================================================
 * IRMS - Client-Side Validation Module
 * File: frontend/assets/js/validation.js
 * ============================================================
 */

const Validator = {
    /**
     * Kiểm tra chuỗi rỗng
     */
    isRequired(value, fieldName = 'Trường này') {
        if (!value || value.trim() === '') {
            return `${fieldName} không được để trống.`;
        }
        return null;
    },

    /**
     * Kiểm tra định dạng email
     */
    isEmail(email) {
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!re.test(email)) {
            return 'Email không hợp lệ. Vui lòng nhập đúng định dạng (VD: name@company.com).';
        }
        return null;
    },

    /**
     * Kiểm tra độ dài tối thiểu của mật khẩu
     */
    isMinLength(value, min = 8, fieldName = 'Mật khẩu') {
        if (!value || value.length < min) {
            return `${fieldName} phải có độ dài tối thiểu ${min} ký tự.`;
        }
        return null;
    },

    /**
     * Kiểm tra mật khẩu khớp nhau
     */
    isMatched(val1, val2, errorMsg = 'Mật khẩu xác nhận không trùng khớp.') {
        if (val1 !== val2) {
            return errorMsg;
        }
        return null;
    },

    /**
     * Hiển thị lỗi bên dưới input
     */
    showError(inputElement, errorMessage) {
        inputElement.classList.add('is-invalid');
        inputElement.classList.remove('is-valid');
        let feedback = inputElement.nextElementSibling;
        if (!feedback || !feedback.classList.contains('invalid-feedback')) {
            feedback = document.createElement('div');
            feedback.className = 'invalid-feedback small';
            inputElement.parentNode.appendChild(feedback);
        }
        feedback.innerText = errorMessage;
    },

    /**
     * Xóa lỗi input
     */
    clearError(inputElement) {
        inputElement.classList.remove('is-invalid');
        inputElement.classList.add('is-valid');
        const feedback = inputElement.nextElementSibling;
        if (feedback && feedback.classList.contains('invalid-feedback')) {
            feedback.innerText = '';
        }
    }
};

window.Validator = Validator;

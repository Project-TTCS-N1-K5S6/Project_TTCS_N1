/**
 * ============================================================
 * IRMS - Client-Side Authentication Module
 * File: frontend/assets/js/auth.js
 * ============================================================
 */

document.addEventListener('DOMContentLoaded', () => {
    // 1. Bật/tắt hiển thị mật khẩu
    const toggleBtn = document.getElementById('togglePassword');
    const pwdInput = document.getElementById('password');
    const eyeIcon = document.getElementById('eyeIcon');

    if (toggleBtn && pwdInput) {
        toggleBtn.addEventListener('click', () => {
            const isPassword = pwdInput.type === 'password';
            pwdInput.type = isPassword ? 'text' : 'password';
            if (eyeIcon) {
                eyeIcon.className = isPassword ? 'bi bi-eye-slash' : 'bi bi-eye';
            }
        });
    }

    // 2. Client-side Form Validation cho Login
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', (e) => {
            const emailInput = document.getElementById('email');
            const passInput = document.getElementById('password');
            let hasError = false;

            if (emailInput) {
                const emailErr = Validator.isRequired(emailInput.value, 'Email') || Validator.isEmail(emailInput.value);
                if (emailErr) {
                    Validator.showError(emailInput, emailErr);
                    hasError = true;
                } else {
                    Validator.clearError(emailInput);
                }
            }

            if (passInput) {
                const passErr = Validator.isRequired(passInput.value, 'Mật khẩu');
                if (passErr) {
                    Validator.showError(passInput, passErr);
                    hasError = true;
                } else {
                    Validator.clearError(passInput);
                }
            }

            if (hasError) {
                e.preventDefault();
                return false;
            }
        });
    }

    // 3. Client-side Validation cho Đổi mật khẩu
    const changePwdForm = document.getElementById('changePasswordForm');
    if (changePwdForm) {
        changePwdForm.addEventListener('submit', (e) => {
            const newPass = document.getElementById('newPassword');
            const confirmPass = document.getElementById('confirmPassword');
            let hasError = false;

            if (newPass) {
                const err = Validator.isMinLength(newPass.value, 8, 'Mật khẩu mới');
                if (err) {
                    Validator.showError(newPass, err);
                    hasError = true;
                } else {
                    Validator.clearError(newPass);
                }
            }

            if (confirmPass && newPass) {
                const matchErr = Validator.isMatched(newPass.value, confirmPass.value);
                if (matchErr) {
                    Validator.showError(confirmPass, matchErr);
                    hasError = true;
                } else {
                    Validator.clearError(confirmPass);
                }
            }

            if (hasError) {
                e.preventDefault();
                return false;
            }
        });
    }
});

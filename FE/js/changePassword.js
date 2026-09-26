/**
 * Change Password Module handling KN-51 (Form UI), KN-52 (Validation), KN-53 (Response & State)
 */
document.addEventListener('DOMContentLoaded', () => {
  // Check auth status
  if (!Auth.isAuthenticated()) {
    window.location.href = 'login.html';
    return;
  }

  // Populate User Info in Header & Sidebar
  const currentUser = Auth.getUser();
  if (currentUser) {
    document.querySelectorAll('.user-name').forEach(el => el.textContent = currentUser.name);
    document.querySelectorAll('.user-role').forEach(el => el.textContent = currentUser.role);
    document.querySelectorAll('.user-avatar').forEach(el => el.src = currentUser.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150');
  }

  // DOM Elements
  const changePasswordForm = document.getElementById('changePasswordForm');
  const currentPasswordInput = document.getElementById('currentPassword');
  const newPasswordInput = document.getElementById('newPassword');
  const confirmPasswordInput = document.getElementById('confirmPassword');
  const revokeOtherSessionsCheckbox = document.getElementById('revokeOtherSessions');
  const submitBtn = document.getElementById('submitBtn');

  // Error Message Elements
  const currentPasswordError = document.getElementById('currentPasswordError');
  const newPasswordError = document.getElementById('newPasswordError');
  const confirmPasswordError = document.getElementById('confirmPasswordError');

  // Rules Checklist Elements
  const ruleMinLength = document.getElementById('ruleMinLength');
  const ruleHasLetter = document.getElementById('ruleHasLetter');
  const ruleHasDigit = document.getElementById('ruleHasDigit');
  const ruleDifferent = document.getElementById('ruleDifferent');

  // Strength Meter Elements
  const strengthMeterFill = document.getElementById('strengthMeterFill');
  const strengthText = document.getElementById('strengthText');

  // Eye Toggle Buttons
  document.querySelectorAll('.btn-toggle-eye').forEach(button => {
    button.addEventListener('click', (e) => {
      e.preventDefault();
      const targetId = button.getAttribute('data-target');
      const input = document.getElementById(targetId);
      if (input) {
        const isPassword = input.type === 'password';
        input.type = isPassword ? 'text' : 'password';
        button.innerHTML = isPassword 
          ? `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path><line x1="1" y1="1" x2="23" y2="23"></line></svg>`
          : `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path><circle cx="12" cy="12" r="3"></circle></svg>`;
      }
    });
  });

  // KN-52: Real-time Input Validation & Strength Meter calculation
  function validateInputs() {
    const currentVal = currentPasswordInput.value;
    const newVal = newPasswordInput.value;
    const confirmVal = confirmPasswordInput.value;

    let isValid = true;

    // 1. Current Password check
    if (currentVal.length === 0) {
      isValid = false;
    }

    // 2. New Password rules check
    const hasMinLength = newVal.length >= 8;
    const hasLetter = /[a-zA-Z]/.test(newVal);
    const hasDigit = /[0-9]/.test(newVal);
    const isDifferent = newVal !== '' && newVal !== currentVal;

    updateRuleState(ruleMinLength, hasMinLength);
    updateRuleState(ruleHasLetter, hasLetter);
    updateRuleState(ruleHasDigit, hasDigit);
    updateRuleState(ruleDifferent, isDifferent);

    // Update Strength Meter
    updateStrengthMeter(newVal, hasMinLength, hasLetter, hasDigit, isDifferent);

    if (!hasMinLength || !hasLetter || !hasDigit || !isDifferent) {
      isValid = false;
    }

    // 3. Confirm Password check
    if (confirmVal.length > 0) {
      if (confirmVal !== newVal) {
        showFieldError(confirmPasswordInput, confirmPasswordError, 'Mật khẩu xác nhận không trùng khớp.');
        isValid = false;
      } else {
        hideFieldError(confirmPasswordInput, confirmPasswordError);
      }
    } else {
      hideFieldError(confirmPasswordInput, confirmPasswordError);
      isValid = false;
    }

    // Toggle submit button state
    submitBtn.disabled = !isValid;
    return isValid;
  }

  function updateRuleState(element, isValid) {
    if (!element) return;
    if (isValid) {
      element.classList.add('valid');
      element.querySelector('.rule-icon').innerHTML = '✓';
    } else {
      element.classList.remove('valid');
      element.querySelector('.rule-icon').innerHTML = '○';
    }
  }

  function updateStrengthMeter(password, hasMinLength, hasLetter, hasDigit, isDifferent) {
    if (!password) {
      strengthMeterFill.style.width = '0%';
      strengthMeterFill.style.backgroundColor = 'transparent';
      strengthText.textContent = 'Mức độ bảo mật';
      return;
    }

    let score = 0;
    if (hasMinLength) score += 30;
    if (hasLetter) score += 25;
    if (hasDigit) score += 25;
    if (isDifferent) score += 10;
    if (password.length >= 12 && /[^a-zA-Z0-9]/.test(password)) score += 10; // Extra bonus for special chars

    if (score < 50) {
      strengthMeterFill.style.width = `${Math.max(score, 20)}%`;
      strengthMeterFill.style.backgroundColor = 'var(--danger)';
      strengthText.textContent = 'Mức độ: Yếu';
    } else if (score < 80) {
      strengthMeterFill.style.width = `${score}%`;
      strengthMeterFill.style.backgroundColor = 'var(--warning)';
      strengthText.textContent = 'Mức độ: Trung bình';
    } else {
      strengthMeterFill.style.width = `${score}%`;
      strengthMeterFill.style.backgroundColor = 'var(--success)';
      strengthText.textContent = 'Mức độ: Mạnh';
    }
  }

  function showFieldError(inputEl, errorEl, message) {
    inputEl.classList.add('is-invalid');
    inputEl.classList.remove('is-valid');
    errorEl.textContent = message;
    errorEl.classList.add('show');
  }

  function hideFieldError(inputEl, errorEl) {
    inputEl.classList.remove('is-invalid');
    errorEl.classList.remove('show');
  }

  // Attach input listeners for real-time validation
  [currentPasswordInput, newPasswordInput, confirmPasswordInput].forEach(input => {
    input.addEventListener('input', () => {
      // Clear field specific server errors on re-typing
      hideFieldError(currentPasswordInput, currentPasswordError);
      hideFieldError(newPasswordInput, newPasswordError);
      validateInputs();
    });
  });

  // KN-53 & KN-54: Form submission handler
  changePasswordForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    if (!validateInputs()) {
      showToast('Cảnh báo', 'Vui lòng kiểm tra và điền đầy đủ các thông tin hợp lệ.', 'warning');
      return;
    }

    const currentPassword = currentPasswordInput.value;
    const newPassword = newPasswordInput.value;
    const confirmPassword = confirmPasswordInput.value;
    const revokeOtherSessions = revokeOtherSessionsCheckbox.checked;

    // Set button loading state
    const originalBtnText = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = `<svg class="spinner" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48l2.83-2.83"></path></svg> Đang xử lý...`;

    try {
      // KN-54: Call Change Password API
      const result = await Auth.fetchWithAuth('/change-password', {
        method: 'POST',
        body: JSON.stringify({
          currentPassword,
          newPassword,
          confirmPassword,
          revokeOtherSessions
        })
      });

      if (!result) return; // User was logged out due to expired session

      if (result.ok && result.data.success) {
        // KN-53: Update token with new JWT token issued by BE
        if (result.data.newToken) {
          Auth.setToken(result.data.newToken);
        }

        // Display Success Toast (KN-53)
        showToast(
          'Đổi mật khẩu thành công!',
          result.data.message || 'Mật khẩu của bạn đã được cập nhật thành công.',
          'success',
          5000
        );

        // Reset Form & Validation States
        changePasswordForm.reset();
        revokeOtherSessionsCheckbox.checked = true; // Maintain default
        validateInputs();
        
        // Refresh User Info Profile if available
        if (result.data.user) {
          Auth.setUser(result.data.user);
        }
      } else {
        // Handle Server Validation Errors (KN-55)
        const errorData = result.data;
        if (errorData.field === 'currentPassword') {
          showFieldError(currentPasswordInput, currentPasswordError, errorData.message);
          currentPasswordInput.focus();
        } else if (errorData.field === 'newPassword') {
          showFieldError(newPasswordInput, newPasswordError, errorData.message);
          newPasswordInput.focus();
        } else if (errorData.field === 'confirmPassword') {
          showFieldError(confirmPasswordInput, confirmPasswordError, errorData.message);
          confirmPasswordInput.focus();
        } else {
          showToast('Đổi mật khẩu thất bại', errorData.message || 'Có lỗi xảy ra, vui lòng thử lại.', 'error');
        }
      }
    } catch (err) {
      console.error(err);
      showToast('Lỗi kết nối', 'Không thể kết nối đến máy chủ Backend.', 'error');
    } finally {
      submitBtn.innerHTML = originalBtnText;
      validateInputs();
    }
  });

  // Logout handler
  document.querySelectorAll('.btn-logout-icon').forEach(btn => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      Auth.logout();
    });
  });

  // Initial validation check
  validateInputs();
});

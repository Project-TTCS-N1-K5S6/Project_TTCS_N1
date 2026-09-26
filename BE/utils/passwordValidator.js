/**
 * Utility function to validate password complexity requirement (KN-55)
 * Requirements:
 * - Minimum 8 characters
 * - Must contain at least one letter (a-z or A-Z)
 * - Must contain at least one digit (0-9)
 */
function validatePasswordRules(password) {
  if (!password || typeof password !== 'string') {
    return {
      isValid: false,
      message: 'Mật khẩu không được để trống.'
    };
  }

  if (password.length < 8) {
    return {
      isValid: false,
      message: 'Mật khẩu phải chứa tối thiểu 8 ký tự.'
    };
  }

  const hasLetter = /[a-zA-Z]/.test(password);
  const hasDigit = /[0-9]/.test(password);

  if (!hasLetter || !hasDigit) {
    return {
      isValid: false,
      message: 'Mật khẩu mới phải chứa đầy đủ cả chữ cái và chữ số.'
    };
  }

  return {
    isValid: true,
    message: 'Mật khẩu hợp lệ.'
  };
}

module.exports = {
  validatePasswordRules
};

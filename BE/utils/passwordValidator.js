'use strict';

/**
 * Password complexity validator (shared between FE and BE)
 * Rules: min 8 chars, at least 1 letter, at least 1 digit
 */
function validatePasswordRules(password) {
  if (!password || typeof password !== 'string') {
    return { isValid: false, message: 'Mật khẩu không hợp lệ.' };
  }
  if (password.length < 8) {
    return { isValid: false, message: 'Mật khẩu phải có ít nhất 8 ký tự.' };
  }
  if (password.length > 128) {
    return { isValid: false, message: 'Mật khẩu không được vượt quá 128 ký tự.' };
  }
  if (!/[a-zA-Z]/.test(password)) {
    return { isValid: false, message: 'Mật khẩu phải chứa ít nhất 1 chữ cái (a-z, A-Z).' };
  }
  if (!/[0-9]/.test(password)) {
    return { isValid: false, message: 'Mật khẩu phải chứa ít nhất 1 chữ số (0-9).' };
  }
  return { isValid: true, message: 'Mật khẩu hợp lệ.' };
}

module.exports = { validatePasswordRules };

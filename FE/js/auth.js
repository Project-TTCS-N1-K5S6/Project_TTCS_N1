/**
 * Auth utilities for managing JWT tokens, API requests, and session revocation detection (KN-56)
 */
const API_BASE_URL = 'http://localhost:5000/api/v1/auth';

class Auth {
  static getToken() {
    return localStorage.getItem('jwtToken');
  }

  static setToken(token) {
    localStorage.setItem('jwtToken', token);
  }

  static getUser() {
    const userStr = localStorage.getItem('user');
    try {
      return userStr ? JSON.parse(userStr) : null;
    } catch (e) {
      return null;
    }
  }

  static setUser(user) {
    localStorage.setItem('user', JSON.stringify(user));
  }

  static isAuthenticated() {
    return !!this.getToken();
  }

  static logout(reason = null) {
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('user');
    if (reason) {
      sessionStorage.setItem('logoutReason', reason);
    }
    window.location.href = 'login.html';
  }

  /**
   * Helper wrapper for fetch with Bearer JWT token header
   */
  static async fetchWithAuth(endpoint, options = {}) {
    const token = this.getToken();
    if (!token) {
      this.logout('Bạn chưa đăng nhập. Vui lòng đăng nhập để tiếp tục.');
      return null;
    }

    const headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
      ...(options.headers || {})
    };

    try {
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers
      });

      const data = await response.json();

      // KN-56: Detect session revocation error from server
      if (response.status === 401 && data.code === 'SESSION_REVOKED') {
        this.logout('Phiên đăng nhập này đã bị thu hồi do tài khoản vừa đổi mật khẩu từ thiết bị khác.');
        return null;
      }

      if (response.status === 401 && (data.code === 'TOKEN_EXPIRED' || data.code === 'UNAUTHORIZED')) {
        this.logout('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
        return null;
      }

      return { status: response.status, ok: response.ok, data };
    } catch (error) {
      console.error('Fetch error:', error);
      throw error;
    }
  }
}

// Toast Notification Helper
function showToast(title, message, type = 'success', duration = 4000) {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;

  const iconSvg = type === 'success' 
    ? `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"></polyline></svg>`
    : `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>`;

  toast.innerHTML = `
    <div class="toast-icon">${iconSvg}</div>
    <div class="toast-content">
      <div class="toast-title">${title}</div>
      <div class="toast-message">${message}</div>
    </div>
    <button class="toast-close" onclick="this.parentElement.remove()">&times;</button>
  `;

  container.appendChild(toast);

  // Trigger smooth enter transition
  setTimeout(() => toast.classList.add('show'), 10);

  // Auto dismiss
  setTimeout(() => {
    toast.classList.remove('show');
    setTimeout(() => toast.remove(), 400);
  }, duration);
}

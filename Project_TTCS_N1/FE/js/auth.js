/**
 * ============================================================
 *  Auth Module - TTCS HR System
 *  Session-based authentication (NO localStorage for tokens)
 *  Session managed via HttpOnly cookies server-side
 * ============================================================
 */

const API_BASE = '/api/v1/auth';

const Auth = (() => {
  // ──────────────────────────────────────────────────────────
  // Session State (in-memory only, NOT localStorage)
  // ──────────────────────────────────────────────────────────
  let _user = null;  // Populated at login / page load via /api/v1/auth/me

  // ──────────────────────────────────────────────────────────
  // Get / Set user (non-sensitive info only, from sessionStorage)
  // ──────────────────────────────────────────────────────────
  function getUser() {
    if (_user) return _user;
    // Use sessionStorage (tab-scoped) for display info only - NOT for auth tokens
    try {
      const raw = sessionStorage.getItem('_ttcs_user');
      return raw ? JSON.parse(raw) : null;
    } catch { return null; }
  }

  function _setUser(user) {
    _user = user;
    // Store display info in sessionStorage (tab-scoped, cleared on tab close)
    // This does NOT contain tokens, session IDs, or passwords
    sessionStorage.setItem('_ttcs_user', JSON.stringify({
      id: user.id,
      employeeCode: user.employeeCode,
      name: user.name,
      email: user.email,
      role: user.role,
      avatarUrl: user.avatarUrl,
      mustChangePw: user.mustChangePw,
    }));
  }

  function _clearUser() {
    _user = null;
    sessionStorage.removeItem('_ttcs_user');
  }

  // ──────────────────────────────────────────────────────────
  // Check Authentication by calling /api/v1/auth/me
  // ──────────────────────────────────────────────────────────
  async function checkAuth() {
    try {
      const res = await fetch(`${API_BASE}/me`, {
        method: 'GET',
        credentials: 'include',  // Send session cookie
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
      });
      if (res.ok) {
        const data = await res.json();
        if (data.success) {
          _setUser(data.user);
          return data.user;
        }
      }
      return null;
    } catch (err) {
      console.warn('[Auth] checkAuth failed:', err.message);
      return null;
    }
  }

  // ──────────────────────────────────────────────────────────
  // Login
  // ──────────────────────────────────────────────────────────
  async function login(identifier, password, deviceName) {
    const res = await fetch(`${API_BASE}/login`, {
      method: 'POST',
      credentials: 'include',
      headers: { 'Content-Type': 'application/json', 'X-Requested-With': 'XMLHttpRequest' },
      body: JSON.stringify({ identifier, password, deviceName })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      _setUser(data.user);
    }
    return { ok: res.ok, status: res.status, data };
  }

  // ──────────────────────────────────────────────────────────
  // Logout (single device)
  // ──────────────────────────────────────────────────────────
  async function logout(reason = null) {
    SessionManager.destroy();
    _clearUser();
    try {
      await fetch(`${API_BASE}/logout`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' }
      });
    } catch (_) { /* Ignore network errors on logout */ }
    if (reason) sessionStorage.setItem('logoutReason', reason);
    window.location.href = '/login.html';
  }

  // ──────────────────────────────────────────────────────────
  // Logout All Devices
  // ──────────────────────────────────────────────────────────
  async function logoutAll() {
    SessionManager.destroy();
    _clearUser();
    try {
      await fetch(`${API_BASE}/logout-all`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' }
      });
    } catch (_) { }
    sessionStorage.setItem('logoutReason', 'Bạn đã đăng xuất khỏi tất cả thiết bị.');
    window.location.href = '/login.html';
  }

  // ──────────────────────────────────────────────────────────
  // Authenticated Fetch (with session cookie, handles expiry)
  // ──────────────────────────────────────────────────────────
  async function fetchWithAuth(url, options = {}) {
    const fullUrl = url.startsWith('/') ? url : `/api/v1${url}`;
    const response = await fetch(fullUrl, {
      ...options,
      credentials: 'include',  // Always send cookies
      headers: {
        'Content-Type': 'application/json',
        'X-Requested-With': 'XMLHttpRequest',
        ...(options.headers || {})
      }
    });

    // Update session expiry from response headers
    SessionManager.updateFromHeaders(response.headers);

    // Handle session expiry
    if (response.status === 401) {
      const data = await response.json().catch(() => ({}));
      const code = data.code || '';

      if (['SESSION_EXPIRED', 'SESSION_ABSOLUTE_EXPIRED', 'UNAUTHORIZED', 'SESSION_MISMATCH'].includes(code)) {
        SessionManager.destroy();
        _clearUser();
        const reason = code === 'SESSION_ABSOLUTE_EXPIRED'
          ? 'Phiên làm việc đã vượt quá thời gian tối đa cho phép. Vui lòng đăng nhập lại.'
          : 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.';
        sessionStorage.setItem('logoutReason', reason);
        window.location.href = '/login.html';
        return null;
      }

      if (code === 'SESSION_REVOKED') {
        SessionManager.destroy();
        _clearUser();
        sessionStorage.setItem('logoutReason', 'Phiên đăng nhập bị thu hồi do tài khoản đổi mật khẩu từ thiết bị khác.');
        window.location.href = '/login.html';
        return null;
      }
    }

    return { ok: response.ok, status: response.status, headers: response.headers, data: await response.json().catch(() => null) };
  }

  // ──────────────────────────────────────────────────────────
  // Role check helpers
  // ──────────────────────────────────────────────────────────
  function hasRole(...roles) {
    const user = getUser();
    return user ? roles.includes(user.role) : false;
  }

  return { login, logout, logoutAll, checkAuth, getUser, fetchWithAuth, hasRole };
})();

// ──────────────────────────────────────────────────────────
// Toast Notification Helper
// ──────────────────────────────────────────────────────────
function showToast(title, message, type = 'success', duration = 4500) {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const icons = {
    success: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>`,
    error: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>`,
    warning: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>`,
    info: `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>`,
  };

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <div class="toast-icon">${icons[type] || icons.info}</div>
    <div class="toast-body">
      <div class="toast-title">${title}</div>
      ${message ? `<div class="toast-msg">${message}</div>` : ''}
    </div>
    <button class="toast-close" onclick="this.closest('.toast').classList.remove('show'); setTimeout(()=>this.closest('.toast')?.remove(), 350)">×</button>
  `;

  container.appendChild(toast);
  requestAnimationFrame(() => toast.classList.add('show'));

  setTimeout(() => {
    toast.classList.remove('show');
    setTimeout(() => toast.remove(), 350);
  }, duration);
}

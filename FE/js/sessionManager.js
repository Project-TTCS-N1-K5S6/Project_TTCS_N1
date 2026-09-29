/**
 * ============================================================
 *  Session Manager - TTCS HR System
 *  Handles: idle timeout warning, auto-renewal, forced logout,
 *           activity tracking, and session expiry countdown
 * ============================================================
 */

const SessionManager = (() => {
  // ──────────────────────────────────────────────────────────
  // Configuration (populated from server at login)
  // ──────────────────────────────────────────────────────────
  let _config = {
    idleTimeoutSeconds: 1800,
    absoluteTimeoutSeconds: 28800,
    renewThresholdSeconds: 600,
    warningBeforeSeconds: 120,  // Show warning 2 min before idle expiry
  };

  let _idleExpiresAt = null;  // Date object
  let _absoluteExpiresAt = null;  // Date object
  let _warningShown = false;
  let _tickInterval = null;
  let _activityDebounce = null;
  let _isActive = false;

  // Activity events to listen for
  const ACTIVITY_EVENTS = ['mousemove', 'keydown', 'click', 'touchstart', 'scroll', 'focus'];

  // ──────────────────────────────────────────────────────────
  // Initialize with session info from server
  // ──────────────────────────────────────────────────────────
  function init(sessionInfo) {
    if (!sessionInfo) return;

    _config.idleTimeoutSeconds = sessionInfo.idleTimeoutSeconds || 1800;
    _config.absoluteTimeoutSeconds = sessionInfo.absoluteTimeoutSeconds || 28800;
    _config.renewThresholdSeconds = sessionInfo.renewThresholdSeconds || 600;

    // Calculate expiry times
    const now = Date.now();
    _idleExpiresAt = new Date(now + _config.idleTimeoutSeconds * 1000);
    _absoluteExpiresAt = new Date(now + _config.absoluteTimeoutSeconds * 1000);

    _isActive = true;
    _warningShown = false;

    // Register activity listeners
    ACTIVITY_EVENTS.forEach(event => {
      document.addEventListener(event, _onUserActivity, { passive: true });
    });

    // Start tick
    _startTick();
    console.log('[SessionManager] Initialized. Idle timeout:', _config.idleTimeoutSeconds, 's');
  }

  // ──────────────────────────────────────────────────────────
  // Update session expiry times (from server response headers)
  // ──────────────────────────────────────────────────────────
  function updateFromHeaders(headers) {
    const idleExpiry = headers.get('X-Session-Idle-Expires');
    const absoluteExpiry = headers.get('X-Session-Absolute-Expires');
    if (idleExpiry) _idleExpiresAt = new Date(idleExpiry);
    if (absoluteExpiry) _absoluteExpiresAt = new Date(absoluteExpiry);
    _warningShown = false;
  }

  // ──────────────────────────────────────────────────────────
  // User activity handler (debounced - sends keep-alive)
  // ──────────────────────────────────────────────────────────
  function _onUserActivity() {
    if (!_isActive) return;
    clearTimeout(_activityDebounce);
    _activityDebounce = setTimeout(_sendKeepAlive, 30 * 1000); // debounce 30s
  }

  async function _sendKeepAlive() {
    if (!_isActive) return;
    try {
      const res = await fetch('/api/v1/auth/refresh', {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' }
      });
      if (res.ok) {
        updateFromHeaders(res.headers);
        _warningShown = false;
        _hideWarning();
      } else if (res.status === 401) {
        _handleSessionExpired();
      }
    } catch (err) {
      console.warn('[SessionManager] Keep-alive failed (network error):', err.message);
    }
  }

  // ──────────────────────────────────────────────────────────
  // Tick every second: check timers, show warnings
  // ──────────────────────────────────────────────────────────
  function _startTick() {
    clearInterval(_tickInterval);
    _tickInterval = setInterval(_tick, 1000);
  }

  function _tick() {
    if (!_isActive || !_idleExpiresAt) return;

    const now = Date.now();
    const idleMs = _idleExpiresAt.getTime() - now;
    const absoluteMs = _absoluteExpiresAt ? _absoluteExpiresAt.getTime() - now : Infinity;
    const timeLeftMs = Math.min(idleMs, absoluteMs);

    // Session expired
    if (timeLeftMs <= 0) {
      _handleSessionExpired();
      return;
    }

    // Update countdown display
    _updateCountdown(timeLeftMs);

    // Show warning when within warningBeforeSeconds of idle expiry
    if (idleMs <= _config.warningBeforeSeconds * 1000 && !_warningShown) {
      _warningShown = true;
      _showWarning(idleMs);
    }

    // Update warning countdown if shown
    if (_warningShown) {
      _updateWarningCountdown(idleMs);
    }
  }

  // ──────────────────────────────────────────────────────────
  // Session Expired: save draft data, redirect to login
  // ──────────────────────────────────────────────────────────
  function _handleSessionExpired() {
    destroy();
    sessionStorage.setItem('logoutReason', 'Phiên làm việc đã hết hạn do không có hoạt động trong thời gian dài.');
    sessionStorage.setItem('sessionExpiredRedirect', 'true');
    window.location.href = '/login.html';
  }

  // ──────────────────────────────────────────────────────────
  // UI: Session warning modal
  // ──────────────────────────────────────────────────────────
  function _showWarning(msLeft) {
    const existing = document.getElementById('session-warning-modal');
    if (existing) return;

    const modal = document.createElement('div');
    modal.id = 'session-warning-modal';
    modal.style.cssText = `
      position: fixed; inset: 0; z-index: 99999; display: flex; align-items: center; justify-content: center;
      background: rgba(0,0,0,0.7); backdrop-filter: blur(4px); animation: fadeIn 0.3s ease;
    `;
    modal.innerHTML = `
      <div style="
        background: linear-gradient(145deg, #1e293b, #0f172a);
        border: 1px solid rgba(245, 158, 11, 0.4);
        border-radius: 20px; padding: 36px; max-width: 460px; width: 90%;
        box-shadow: 0 25px 60px rgba(0,0,0,0.6), 0 0 0 1px rgba(245,158,11,0.2);
        animation: slideUp 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
      ">
        <div style="display:flex; align-items:center; gap:14px; margin-bottom:20px;">
          <div style="
            width:52px; height:52px; border-radius:14px;
            background: rgba(245,158,11,0.15); border: 1px solid rgba(245,158,11,0.3);
            display:flex; align-items:center; justify-content:center; flex-shrink:0;
            font-size:1.5rem;
          ">⏱️</div>
          <div>
            <h3 style="color:#fbbf24; font-size:1.15rem; font-weight:700; margin:0;">Phiên sắp hết hạn</h3>
            <p style="color:#94a3b8; font-size:0.85rem; margin:4px 0 0;">Bạn sẽ bị đăng xuất tự động</p>
          </div>
        </div>
        <p style="color:#cbd5e1; font-size:0.95rem; line-height:1.6; margin-bottom:8px;">
          Phiên làm việc của bạn sẽ hết hạn sau <strong style="color:#fbbf24;" id="session-warning-countdown">--:--</strong>.
        </p>
        <p style="color:#64748b; font-size:0.85rem; margin-bottom:24px;">
          Mọi dữ liệu đang nhập sẽ được lưu tự động. Nhấn <strong style="color:#6366f1;">Tiếp tục phiên</strong> để ở lại.
        </p>
        <div style="display:flex; gap:12px;">
          <button id="session-logout-btn" style="
            flex:1; padding:12px; border-radius:10px; border:1px solid rgba(255,255,255,0.1);
            background:rgba(255,255,255,0.05); color:#94a3b8; font-size:0.9rem; font-weight:500;
            cursor:pointer; transition:all 0.2s;
          " onmouseover="this.style.background='rgba(239,68,68,0.15)'; this.style.color='#f87171'"
             onmouseout="this.style.background='rgba(255,255,255,0.05)'; this.style.color='#94a3b8'">
            Đăng xuất
          </button>
          <button id="session-continue-btn" style="
            flex:2; padding:12px; border-radius:10px; border:none;
            background:linear-gradient(135deg,#6366f1,#4f46e5); color:#fff;
            font-size:0.9rem; font-weight:600; cursor:pointer; transition:all 0.2s;
            box-shadow: 0 4px 15px rgba(99,102,241,0.4);
          " onmouseover="this.style.transform='translateY(-2px)'; this.style.boxShadow='0 8px 25px rgba(99,102,241,0.5)'"
             onmouseout="this.style.transform='translateY(0)'; this.style.boxShadow='0 4px 15px rgba(99,102,241,0.4)'">
            ✅ Tiếp tục phiên
          </button>
        </div>
      </div>
    `;

    document.body.appendChild(modal);

    // Button handlers
    document.getElementById('session-continue-btn').onclick = async () => {
      modal.remove();
      await _sendKeepAlive();
    };
    document.getElementById('session-logout-btn').onclick = async () => {
      modal.remove();
      await Auth.logout();
    };
  }

  function _hideWarning() {
    const modal = document.getElementById('session-warning-modal');
    if (modal) modal.remove();
  }

  function _updateWarningCountdown(msLeft) {
    const el = document.getElementById('session-warning-countdown');
    if (!el) return;
    const secs = Math.max(0, Math.ceil(msLeft / 1000));
    const m = Math.floor(secs / 60).toString().padStart(2, '0');
    const s = (secs % 60).toString().padStart(2, '0');
    el.textContent = `${m}:${s}`;
  }

  function _updateCountdown(msLeft) {
    const el = document.getElementById('session-countdown-display');
    if (!el) return;
    const secs = Math.max(0, Math.ceil(msLeft / 1000));
    const h = Math.floor(secs / 3600);
    const m = Math.floor((secs % 3600) / 60);
    const s = secs % 60;
    if (h > 0) {
      el.textContent = `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    } else {
      el.textContent = `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    }
    // Color coding
    if (msLeft < 120000) {
      el.style.color = '#ef4444';
    } else if (msLeft < 300000) {
      el.style.color = '#f59e0b';
    } else {
      el.style.color = '#10b981';
    }
  }

  // ──────────────────────────────────────────────────────────
  // Destroy session manager (on logout)
  // ──────────────────────────────────────────────────────────
  function destroy() {
    _isActive = false;
    clearInterval(_tickInterval);
    clearTimeout(_activityDebounce);
    ACTIVITY_EVENTS.forEach(event => {
      document.removeEventListener(event, _onUserActivity);
    });
    _hideWarning();
  }

  return { init, destroy, updateFromHeaders, sendKeepAlive: _sendKeepAlive };
})();

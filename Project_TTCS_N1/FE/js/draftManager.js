/**
 * ============================================================
 *  Auto-Save Draft Manager - TTCS HR System
 *  Saves evaluation form data to server (JSONB) at interval
 *  Restores data after session expiry / re-login
 * ============================================================
 */

const DraftManager = (() => {
  let _draftKey = null;
  let _formElement = null;
  let _saveInterval = null;
  let _lastSaved = null;
  let _isDirty = false;
  let _pendingOffline = null;  // Holds draft when network is offline
  const SAVE_INTERVAL_MS = 10000;  // Auto-save every 10 seconds
  const OFFLINE_KEY_PREFIX = '_ttcs_offline_draft_';

  /**
   * Initialize auto-save for a form
   * @param {HTMLFormElement} formEl - the form to watch
   * @param {string} draftKey - unique key for this draft (e.g. 'eval_Q3-2026_emp001')
   */
  async function init(formEl, draftKey) {
    if (!formEl || !draftKey) return;
    _formElement = formEl;
    _draftKey = draftKey;
    _isDirty = false;

    // Attach input listeners to track changes
    formEl.addEventListener('input', () => { _isDirty = true; });
    formEl.addEventListener('change', () => { _isDirty = true; });

    // Try to restore from server first
    const restored = await _restoreFromServer();
    if (!restored) {
      // Fall back to offline draft (from sessionStorage)
      _restoreFromOfflineStore();
    }

    // Start periodic save
    _saveInterval = setInterval(_autoSave, SAVE_INTERVAL_MS);

    // Save before page unload
    window.addEventListener('beforeunload', _syncBeforeUnload);

    // Save when going offline then back online
    window.addEventListener('offline', _onOffline);
    window.addEventListener('online', _onOnline);

    _updateStatusBadge('idle');
    console.log('[DraftManager] Initialized for key:', draftKey);
  }

  /**
   * Get current form data as plain object
   */
  function _collectFormData() {
    if (!_formElement) return {};
    const data = {};
    const inputs = _formElement.querySelectorAll('input, textarea, select');
    inputs.forEach(input => {
      if (!input.name && !input.id) return;
      const key = input.name || input.id;
      if (input.type === 'checkbox') {
        data[key] = input.checked;
      } else if (input.type === 'radio') {
        if (input.checked) data[key] = input.value;
      } else {
        data[key] = input.value;
      }
    });
    return data;
  }

  /**
   * Restore form data from an object
   */
  function _applyFormData(data) {
    if (!_formElement || !data) return;
    Object.entries(data).forEach(([key, value]) => {
      const input = _formElement.querySelector(`[name="${key}"], #${key}`);
      if (!input) return;
      if (input.type === 'checkbox') {
        input.checked = !!value;
      } else if (input.type === 'radio') {
        const radio = _formElement.querySelector(`[name="${key}"][value="${value}"]`);
        if (radio) radio.checked = true;
      } else {
        input.value = value ?? '';
      }
    });
  }

  /**
   * Auto-save: send to server if dirty
   */
  async function _autoSave() {
    if (!_isDirty) return;
    await saveNow();
  }

  /**
   * Save draft to server immediately
   */
  async function saveNow() {
    if (!_draftKey || !_formElement) return;
    const data = _collectFormData();

    // Save to offline store first (fallback)
    _saveToOfflineStore(data);

    _updateStatusBadge('saving');
    try {
      const result = await Auth.fetchWithAuth(`/api/v1/evaluations/drafts/${encodeURIComponent(_draftKey)}`, {
        method: 'POST',
        body: JSON.stringify({ data })
      });

      if (result && result.ok) {
        _isDirty = false;
        _lastSaved = new Date(result.data.savedAt);
        _updateStatusBadge('saved', _lastSaved);
        // Clear offline store after successful server save
        sessionStorage.removeItem(OFFLINE_KEY_PREFIX + _draftKey);
      } else {
        _updateStatusBadge('error');
      }
    } catch (err) {
      console.warn('[DraftManager] Save failed (offline?):', err.message);
      _updateStatusBadge('offline');
    }
  }

  /**
   * Restore draft from server
   * @returns {boolean} true if restored
   */
  async function _restoreFromServer() {
    if (!_draftKey) return false;
    try {
      const result = await Auth.fetchWithAuth(`/api/v1/evaluations/drafts/${encodeURIComponent(_draftKey)}`, {
        method: 'GET'
      });
      if (result && result.ok && result.data?.draft?.data) {
        _applyFormData(result.data.draft.data);
        _lastSaved = new Date(result.data.draft.savedAt);
        _updateStatusBadge('restored', _lastSaved);
        console.log('[DraftManager] Draft restored from server:', _draftKey);
        return true;
      }
    } catch (_) { }
    return false;
  }

  /**
   * Save to offline store (sessionStorage) as fallback
   */
  function _saveToOfflineStore(data) {
    try {
      sessionStorage.setItem(OFFLINE_KEY_PREFIX + _draftKey, JSON.stringify({
        data,
        savedAt: new Date().toISOString()
      }));
    } catch (_) { }
  }

  /**
   * Restore from offline store if server restore failed
   */
  function _restoreFromOfflineStore() {
    try {
      const raw = sessionStorage.getItem(OFFLINE_KEY_PREFIX + _draftKey);
      if (!raw) return false;
      const { data, savedAt } = JSON.parse(raw);
      if (data) {
        _applyFormData(data);
        _updateStatusBadge('restored', new Date(savedAt));
        showToast('Khôi phục bản nháp', 'Dữ liệu được khôi phục từ bộ nhớ tạm trên thiết bị.', 'info');
        return true;
      }
    } catch (_) { }
    return false;
  }

  function _syncBeforeUnload() {
    if (_isDirty) {
      const data = _collectFormData();
      _saveToOfflineStore(data);
    }
  }

  function _onOffline() {
    _updateStatusBadge('offline');
  }

  async function _onOnline() {
    // Attempt to sync any pending offline draft
    if (_isDirty) await saveNow();
  }

  /**
   * Update the auto-save status badge in the UI
   */
  function _updateStatusBadge(state, date = null) {
    const badge = document.getElementById('draft-save-status');
    if (!badge) return;

    const states = {
      idle: { text: 'Chưa có thay đổi', color: '#94a3b8', icon: '○' },
      saving: { text: 'Đang lưu...', color: '#f59e0b', icon: '⟳' },
      saved: { text: `Đã lưu ${date ? _formatTime(date) : ''}`, color: '#10b981', icon: '✓' },
      restored: { text: `Đã khôi phục ${date ? _formatTime(date) : ''}`, color: '#6366f1', icon: '↩' },
      error: { text: 'Lưu thất bại', color: '#ef4444', icon: '✕' },
      offline: { text: 'Đang ngoại tuyến (tạm lưu)', color: '#f59e0b', icon: '📶' },
    };
    const s = states[state] || states.idle;
    badge.innerHTML = `<span style="color:${s.color}">${s.icon} ${s.text}</span>`;
  }

  function _formatTime(date) {
    return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }

  /**
   * Destroy draft manager (on form submit / navigate away)
   */
  function destroy() {
    clearInterval(_saveInterval);
    window.removeEventListener('beforeunload', _syncBeforeUnload);
    window.removeEventListener('offline', _onOffline);
    window.removeEventListener('online', _onOnline);
  }

  /**
   * Delete draft from server after successful submission
   */
  async function deleteDraft() {
    if (!_draftKey) return;
    destroy();
    sessionStorage.removeItem(OFFLINE_KEY_PREFIX + _draftKey);
    await Auth.fetchWithAuth(`/api/v1/evaluations/drafts/${encodeURIComponent(_draftKey)}`, { method: 'DELETE' });
  }

  return { init, saveNow, deleteDraft, destroy };
})();

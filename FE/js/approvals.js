'use strict';

const STATUS_LABELS = {
  PENDING_APPROVAL: 'Đang chờ phê duyệt',
  NEED_MORE_INFO: 'Cần bổ sung',
  REJECTED: 'Đã từ chối',
  APPROVED: 'Đã duyệt',
};

const STEP_STATUS_LABELS = {
  PENDING: 'Chờ đến lượt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Đã từ chối',
  NEED_MORE_INFO: 'Yêu cầu bổ sung',
};

const ACTION_LABELS = {
  SUBMITTED: 'Đã gửi phê duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Đã từ chối',
  REQUEST_MORE_INFO: 'Yêu cầu bổ sung',
  RESUBMITTED: 'Người tạo đã gửi bổ sung',
};

const state = {
  user: null,
  reviewers: [],
  requests: [],
  selectedRequestId: null,
};

const elements = {
  notice: document.getElementById('notice'),
  requestList: document.getElementById('request-list'),
  createPanel: document.getElementById('create-panel'),
  createForm: document.getElementById('create-form'),
  reviewerSteps: document.getElementById('reviewer-steps'),
  detailPanel: document.getElementById('detail-panel'),
  requestDetail: document.getElementById('request-detail'),
};

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, (character) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
  })[character]);
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString('vi-VN') : '—';
}

function showNotice(message, type = 'success') {
  elements.notice.textContent = message;
  elements.notice.className = `notice notice-${type}`;
  elements.notice.hidden = false;
}

function clearNotice() {
  elements.notice.textContent = '';
  elements.notice.hidden = true;
}

async function apiRequest(path, options = {}) {
  const response = await fetch(`/api/v1/approvals${path}`, {
    ...options,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      'X-Requested-With': 'XMLHttpRequest',
      ...(options.headers || {}),
    },
  });
  const data = await response.json().catch(() => ({}));
  if (response.status === 401) {
    window.location.href = '/login.html';
    throw new Error('Phiên đăng nhập đã hết hạn.');
  }
  if (!response.ok || !data.success) {
    throw new Error(data.message || 'Không thể hoàn thành thao tác.');
  }
  return data;
}

function statusClass(status) {
  if (status === 'APPROVED') return 'status-approved';
  if (status === 'REJECTED') return 'status-rejected';
  if (status === 'NEED_MORE_INFO') return 'status-info';
  return 'status-pending';
}

function renderRequestList() {
  if (state.requests.length === 0) {
    elements.requestList.innerHTML = '<p class="empty-state">Chưa có hồ sơ phê duyệt nào.</p>';
    return;
  }

  elements.requestList.innerHTML = state.requests.map((request) => {
    const progress = request.status === 'APPROVED'
      ? 100
        : Math.round((request.approved_steps / request.total_steps) * 100);
    const currentOwner = request.is_creator
      ? 'Hồ sơ của bạn'
      : request.can_review
        ? 'Đang chờ bạn xử lý'
        : `Người tạo: ${request.creator_name}`;
    return `
      <button type="button" class="request-card" data-request-id="${escapeHtml(request.id)}">
        <div class="request-card-top">
          <h3>${escapeHtml(request.title)}</h3>
          <span class="status-badge ${statusClass(request.status)}">${STATUS_LABELS[request.status]}</span>
        </div>
        <div class="request-meta">
          <span>${escapeHtml(currentOwner)}</span>
          <span>Cấp ${request.current_step}/${request.total_steps}</span>
          <span>Cập nhật ${formatDate(request.updated_at)}</span>
        </div>
        <div class="progress-track" aria-label="Tiến độ ${progress}%">
          <div class="progress-value" style="width:${Math.min(100, Math.max(0, progress))}%"></div>
        </div>
      </button>`;
  }).join('');

  elements.requestList.querySelectorAll('[data-request-id]').forEach((button) => {
    button.addEventListener('click', () => openRequest(button.dataset.requestId));
  });
}

function addReviewerStep(selectedId = '') {
  if (elements.reviewerSteps.children.length >= 5) {
    showNotice('Mỗi hồ sơ hỗ trợ tối đa 5 cấp phê duyệt.', 'error');
    return;
  }
  const row = document.createElement('div');
  row.className = 'reviewer-row';
  const stepNumber = elements.reviewerSteps.children.length + 1;
  const options = state.reviewers.map((reviewer) => `
    <option value="${escapeHtml(reviewer.id)}" ${reviewer.id === selectedId ? 'selected' : ''}>
      ${escapeHtml(reviewer.full_name)} · ${escapeHtml(reviewer.employee_code)} (${escapeHtml(reviewer.role)})
    </option>`).join('');
  row.innerHTML = `
    <label for="reviewer-${stepNumber}">Cấp ${stepNumber}</label>
    <select id="reviewer-${stepNumber}" required>
      <option value="">Chọn người phê duyệt</option>
      ${options}
    </select>
    <button type="button" class="button button-muted remove-step" aria-label="Xóa cấp ${stepNumber}">Xóa</button>`;
  row.querySelector('.remove-step').addEventListener('click', () => {
    if (elements.reviewerSteps.children.length === 1) {
      showNotice('Hồ sơ cần ít nhất một cấp phê duyệt.', 'error');
      return;
    }
    row.remove();
    renumberReviewerSteps();
  });
  elements.reviewerSteps.appendChild(row);
}

function renumberReviewerSteps() {
  [...elements.reviewerSteps.children].forEach((row, index) => {
    const label = row.querySelector('label');
    const select = row.querySelector('select');
    const remove = row.querySelector('.remove-step');
    label.textContent = `Cấp ${index + 1}`;
    label.htmlFor = `reviewer-${index + 1}`;
    select.id = `reviewer-${index + 1}`;
    remove.setAttribute('aria-label', `Xóa cấp ${index + 1}`);
  });
}

function renderRequestDetail(request) {
  const steps = request.steps.map((step) => `
    <div class="step-line">
      <div class="step-line-main">
        <strong>Cấp ${step.step_number}: ${escapeHtml(step.approver_name)}</strong>
        <div class="request-meta">${escapeHtml(step.approver_employee_code)} · ${STEP_STATUS_LABELS[step.status]}</div>
        ${step.comment ? `<p class="request-meta">Ý kiến: ${escapeHtml(step.comment)}</p>` : ''}
      </div>
      <span class="status-badge ${step.status === 'APPROVED' ? 'status-approved' : step.status === 'REJECTED' ? 'status-rejected' : step.status === 'NEED_MORE_INFO' ? 'status-info' : 'status-pending'}">
        ${STEP_STATUS_LABELS[step.status]}
      </span>
    </div>`).join('');

  const history = request.history.map((entry) => `
    <div class="history-line">
      <div class="history-line-main">
        <strong>${ACTION_LABELS[entry.action]}${entry.step_number ? ` · Cấp ${entry.step_number}` : ''}</strong>
        <div class="request-meta">${escapeHtml(entry.actor_name || 'Tài khoản đã ngừng hoạt động')} · ${formatDate(entry.created_at)}</div>
        ${entry.comment ? `<p class="request-meta">${escapeHtml(entry.comment)}</p>` : ''}
      </div>
    </div>`).join('');

  const decisionActions = request.can_review ? `
    <div class="comment-box">
      <label class="field-label" for="decision-comment">Ý kiến (bắt buộc khi từ chối hoặc yêu cầu bổ sung)</label>
      <textarea id="decision-comment" rows="3" maxlength="4000" placeholder="Nhập ý kiến của bạn"></textarea>
    </div>
    <div class="detail-actions">
      <button type="button" class="button button-primary" data-decision="APPROVE">Duyệt</button>
      <button type="button" class="button button-danger" data-decision="REJECT">Từ chối</button>
      <button type="button" class="button button-warning" data-decision="REQUEST_MORE_INFO">Yêu cầu bổ sung</button>
    </div>` : '';

  const resubmitAction = request.is_creator && request.status === 'NEED_MORE_INFO' ? `
    <div class="comment-box">
      <label class="field-label" for="resubmit-response">Thông tin bổ sung gửi lại người phê duyệt</label>
      <textarea id="resubmit-response" rows="3" maxlength="4000" required placeholder="Nhập nội dung đã bổ sung"></textarea>
    </div>
    <div class="detail-actions">
      <button type="button" class="button button-primary" id="resubmit-button">Gửi bổ sung</button>
    </div>` : '';

  elements.requestDetail.innerHTML = `
    <div class="panel-heading">
      <div>
        <span class="status-badge ${statusClass(request.status)}">${STATUS_LABELS[request.status]}</span>
        <h2>${escapeHtml(request.title)}</h2>
        <p class="request-meta">Người tạo: ${escapeHtml(request.creator_name)} · Tạo lúc ${formatDate(request.created_at)}</p>
      </div>
      <button type="button" class="button button-muted" id="close-detail">Đóng</button>
    </div>
    <div>${escapeHtml(request.details).replace(/\n/g, '<br>')}</div>
    <div class="detail-section">
      <h3>Các bước phê duyệt</h3>
      <div class="step-list">${steps}</div>
    </div>
    ${decisionActions || resubmitAction ? `
      <div class="detail-section">
        <h3>Thao tác</h3>
        ${decisionActions || resubmitAction}
      </div>` : ''}
    <div class="detail-section">
      <h3>Lịch sử thao tác</h3>
      <div class="history-list">${history || '<p class="empty-state">Chưa có lịch sử.</p>'}</div>
    </div>`;

  elements.detailPanel.hidden = false;
  elements.requestDetail.querySelector('#close-detail').addEventListener('click', closeDetail);
  elements.requestDetail.querySelectorAll('[data-decision]').forEach((button) => {
    button.addEventListener('click', () => submitDecision(request.id, button.dataset.decision));
  });
  const resubmitButton = elements.requestDetail.querySelector('#resubmit-button');
  if (resubmitButton) resubmitButton.addEventListener('click', () => submitResubmission(request.id));
}

async function loadRequests() {
  const data = await apiRequest('/');
  state.requests = data.requests;
  renderRequestList();
  if (state.selectedRequestId) {
    const selected = state.requests.find((request) => request.id === state.selectedRequestId);
    if (selected) await openRequest(selected.id);
    else closeDetail();
  }
}

async function openRequest(requestId) {
  try {
    const data = await apiRequest(`/${encodeURIComponent(requestId)}`);
    state.selectedRequestId = requestId;
    renderRequestDetail(data.request);
    elements.detailPanel.scrollIntoView({ behavior: 'smooth', block: 'start' });
  } catch (err) {
    showNotice(err.message, 'error');
  }
}

function closeDetail() {
  state.selectedRequestId = null;
  elements.detailPanel.hidden = true;
  elements.requestDetail.replaceChildren();
}

async function submitDecision(requestId, action) {
  const comment = elements.requestDetail.querySelector('#decision-comment').value.trim();
  if (action !== 'APPROVE' && !comment) {
    showNotice('Vui lòng nhập ý kiến khi từ chối hoặc yêu cầu bổ sung.', 'error');
    return;
  }
  try {
    const data = await apiRequest(`/${encodeURIComponent(requestId)}/decisions`, {
      method: 'POST',
      body: JSON.stringify({ action, comment }),
    });
    showNotice(data.status === 'APPROVED'
      ? 'Hồ sơ đã được duyệt ở cấp cuối.'
      : action === 'APPROVE'
        ? 'Đã duyệt, hồ sơ tự chuyển sang cấp tiếp theo.'
        : action === 'REJECT'
          ? 'Đã từ chối hồ sơ.'
          : 'Đã yêu cầu bổ sung và chuyển hồ sơ về người tạo.');
    await loadRequests();
  } catch (err) {
    showNotice(err.message, 'error');
  }
}

async function submitResubmission(requestId) {
  const response = elements.requestDetail.querySelector('#resubmit-response').value.trim();
  if (!response) {
    showNotice('Vui lòng nhập nội dung đã bổ sung.', 'error');
    return;
  }
  try {
    await apiRequest(`/${encodeURIComponent(requestId)}/resubmit`, {
      method: 'POST',
      body: JSON.stringify({ response }),
    });
    showNotice('Đã gửi bổ sung; hồ sơ quay lại đúng cấp phê duyệt và lịch sử được giữ nguyên.');
    await loadRequests();
  } catch (err) {
    showNotice(err.message, 'error');
  }
}

async function submitNewRequest(event) {
  event.preventDefault();
  clearNotice();
  const reviewerIds = [...elements.reviewerSteps.querySelectorAll('select')].map((select) => select.value);
  if (reviewerIds.some((id) => !id) || new Set(reviewerIds).size !== reviewerIds.length) {
    showNotice('Chọn một người khác nhau cho mỗi cấp phê duyệt.', 'error');
    return;
  }
  const formData = new FormData(elements.createForm);
  try {
    await apiRequest('/', {
      method: 'POST',
      body: JSON.stringify({
        title: formData.get('title'),
        details: formData.get('details'),
        reviewerIds,
      }),
    });
    elements.createForm.reset();
    elements.reviewerSteps.replaceChildren();
    addReviewerStep();
    elements.createPanel.hidden = true;
    showNotice('Đã gửi hồ sơ vào luồng phê duyệt.');
    await loadRequests();
  } catch (err) {
    showNotice(err.message, 'error');
  }
}

async function initialize() {
  try {
    state.user = await Auth.checkAuth();
    if (!state.user) {
      window.location.href = '/login.html';
      return;
    }
    document.getElementById('current-user').textContent = `${state.user.name} · ${state.user.employeeCode}`;
    document.getElementById('logout-button').addEventListener('click', () => Auth.logout());
    const data = await apiRequest('/reviewers');
    state.reviewers = data.reviewers;
    addReviewerStep();
    document.getElementById('show-create').addEventListener('click', () => {
      clearNotice();
      elements.createPanel.hidden = false;
      elements.createPanel.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
    document.getElementById('cancel-create').addEventListener('click', () => {
      elements.createPanel.hidden = true;
    });
    document.getElementById('add-step').addEventListener('click', () => addReviewerStep());
    document.getElementById('refresh-list').addEventListener('click', () => {
      loadRequests().catch((err) => showNotice(err.message, 'error'));
    });
    elements.createForm.addEventListener('submit', submitNewRequest);
    await loadRequests();
  } catch (err) {
    showNotice(err.message, 'error');
  }
}

initialize();

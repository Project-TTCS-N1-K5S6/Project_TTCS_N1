document.addEventListener('DOMContentLoaded', () => {
  const navButtons = document.querySelectorAll('.nav-item');
  const dialog = document.querySelector('#copy-dialog');
  const form = document.querySelector('#copy-form');
  const sourceSelect = document.querySelector('#source-job');
  const titleInput = document.querySelector('#copy-title');
  const descriptionInput = document.querySelector('#copy-description');
  const requirementsInput = document.querySelector('#copy-requirements');
  const copyList = document.querySelector('#copy-list');
  const errorMessage = document.querySelector('#copy-error');
  const toast = document.querySelector('#copy-toast');
  const openDialogButton = document.querySelector('#open-copy-dialog');
  const submitButton = form.querySelector('[type="submit"]');
  let sourceJobs = [];
  let copies = [];
  let toastTimeout;

  navButtons.forEach((button) => {
    button.addEventListener('click', () => {
      navButtons.forEach((item) => item.classList.remove('selected', 'active'));
      button.classList.add('selected');
    });
  });

  function setError(message) {
    errorMessage.textContent = message;
    errorMessage.hidden = !message;
  }

  function showToast(message) {
    toast.textContent = message;
    toast.classList.add('visible');
    window.clearTimeout(toastTimeout);
    toastTimeout = window.setTimeout(() => toast.classList.remove('visible'), 3500);
  }

  async function requestJson(url, options) {
    const response = await fetch(url, options);
    let payload;
    try {
      payload = await response.json();
    } catch (error) {
      throw new Error('Máy chủ trả về dữ liệu không hợp lệ.');
    }

    if (!response.ok) {
      throw new Error(payload.error || `Yêu cầu thất bại (${response.status}).`);
    }
    return payload;
  }

  function appendText(parent, tagName, className, text) {
    const element = document.createElement(tagName);
    if (className) element.className = className;
    element.textContent = text;
    parent.append(element);
    return element;
  }

  function renderCopies() {
    copyList.replaceChildren();

    if (copies.length === 0) {
      appendText(copyList, 'p', 'empty-text', 'Chưa có bản sao nào. Hãy chọn một tin gốc để bắt đầu.');
      return;
    }

    copies.forEach((copy) => {
      const row = document.createElement('details');
      row.className = 'copy-item';

      const summary = document.createElement('summary');
      summary.className = 'copy-summary';
      appendText(summary, 'span', 'copy-name', copy.title);
      appendText(summary, 'span', 'draft-badge', copy.status);
      row.append(summary);

      const content = document.createElement('div');
      content.className = 'copy-content';
      appendText(content, 'p', 'copy-source', `Sao chép từ: ${copy.sourceTitle}`);
      appendText(content, 'h3', 'copy-field-title', 'Mô tả công việc');
      appendText(content, 'p', 'copy-field-text', copy.description);
      appendText(content, 'h3', 'copy-field-title', 'Yêu cầu ứng viên');
      appendText(content, 'p', 'copy-field-text', copy.requirements);
      row.append(content);
      copyList.append(row);
    });
  }

  function populateSources() {
    sourceSelect.replaceChildren(new Option('Chọn tin tuyển dụng', ''));
    sourceJobs.forEach((job) => {
      sourceSelect.add(new Option(`${job.title} — ${job.status}`, job.id));
    });
  }

  async function loadData() {
    openDialogButton.disabled = true;
    copyList.replaceChildren();
    appendText(copyList, 'p', 'empty-text', 'Đang tải dữ liệu tuyển dụng...');

    try {
      const [jobs, savedCopies] = await Promise.all([
        requestJson('/api/jobs'),
        requestJson('/api/job-copies')
      ]);
      if (!Array.isArray(jobs) || !Array.isArray(savedCopies)) {
        throw new Error('Máy chủ trả về danh sách tuyển dụng không hợp lệ.');
      }

      sourceJobs = jobs;
      copies = savedCopies;
      populateSources();
      renderCopies();
      openDialogButton.disabled = sourceJobs.length === 0;
    } catch (error) {
      copyList.replaceChildren();
      appendText(copyList, 'p', 'storage-error', `Không thể tải dữ liệu từ backend. ${error.message}`);
    }
  }

  openDialogButton.addEventListener('click', () => {
    form.reset();
    setError('');
    dialog.showModal();
    sourceSelect.focus();
  });

  document.querySelector('#close-copy-dialog').addEventListener('click', () => dialog.close());
  document.querySelector('#cancel-copy').addEventListener('click', () => dialog.close());

  sourceSelect.addEventListener('change', () => {
    const source = sourceJobs.find((job) => job.id === sourceSelect.value);
    if (!source) {
      titleInput.value = '';
      descriptionInput.value = '';
      requirementsInput.value = '';
      return;
    }

    titleInput.value = `Bản sao - ${source.title}`;
    descriptionInput.value = source.description;
    requirementsInput.value = source.requirements;
    setError('');
  });

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    setError('');

    if (!sourceJobs.some((job) => job.id === sourceSelect.value)) {
      setError('Vui lòng chọn tin tuyển dụng gốc.');
      sourceSelect.focus();
      return;
    }

    const newCopy = {
      sourceJobId: sourceSelect.value,
      title: titleInput.value.trim(),
      description: descriptionInput.value.trim(),
      requirements: requirementsInput.value.trim()
    };

    if (!newCopy.title || !newCopy.description || !newCopy.requirements) {
      setError('Vui lòng nhập đầy đủ tên tin, mô tả công việc và yêu cầu ứng viên.');
      return;
    }

    submitButton.disabled = true;
    try {
      const createdCopy = await requestJson('/api/job-copies', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(newCopy)
      });
      copies = [createdCopy, ...copies];
      renderCopies();
      dialog.close();
      showToast('Đã tạo bản sao tuyển dụng ở trạng thái Nháp.');
    } catch (error) {
      setError(`Không thể tạo bản sao tuyển dụng. ${error.message}`);
    } finally {
      submitButton.disabled = false;
    }
  });

  loadData();
});

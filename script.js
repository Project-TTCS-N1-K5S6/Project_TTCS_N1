document.addEventListener('DOMContentLoaded', () => {
  const sourceJobs = [
    {
      id: 'job-sales',
      title: 'Chuyên viên Kinh doanh',
      status: 'Đang tuyển',
      description:
        'Tìm kiếm và phát triển khách hàng mới. Tư vấn giải pháp phù hợp, duy trì quan hệ với khách hàng hiện tại và phối hợp với các phòng ban để đạt mục tiêu kinh doanh.',
      requirements:
        '- Tốt nghiệp Cao đẳng trở lên.\n- Có kỹ năng giao tiếp, đàm phán và chăm sóc khách hàng.\n- Ưu tiên ứng viên có ít nhất 1 năm kinh nghiệm kinh doanh.'
    },
    {
      id: 'job-java',
      title: 'Lập trình viên Java',
      status: 'Đã đóng',
      description:
        'Phát triển, kiểm thử và bảo trì các dịch vụ backend bằng Java. Phối hợp cùng nhóm sản phẩm để phân tích yêu cầu và cải thiện hiệu năng hệ thống.',
      requirements:
        '- Có kiến thức Java, OOP và SQL.\n- Biết sử dụng Git và làm việc theo quy trình nhóm.\n- Có kinh nghiệm với Spring Boot là một lợi thế.'
    },
    {
      id: 'job-hr-intern',
      title: 'Thực tập sinh Nhân sự',
      status: 'Bản nháp',
      description:
        'Hỗ trợ đăng tin tuyển dụng, sàng lọc hồ sơ, sắp xếp lịch phỏng vấn và cập nhật dữ liệu ứng viên.',
      requirements:
        '- Là sinh viên năm cuối hoặc mới tốt nghiệp ngành Quản trị nhân lực.\n- Cẩn thận, chủ động và giao tiếp tốt.\n- Có thể thực tập tối thiểu 3 ngày mỗi tuần.'
    }
  ];

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
  const copiesStorageKey = 'kn115-recruitment-copies';
  let copies = [];
  let storageReady = false;
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

  function readCopies() {
    const storedCopies = window.localStorage.getItem(copiesStorageKey);
    if (!storedCopies) return [];

    const parsedCopies = JSON.parse(storedCopies);
    if (!Array.isArray(parsedCopies)) {
      throw new Error('Dữ liệu bản sao tuyển dụng trong trình duyệt không hợp lệ.');
    }

    const validCopies = parsedCopies.every((copy) =>
      copy &&
      typeof copy.id === 'string' &&
      typeof copy.title === 'string' &&
      typeof copy.description === 'string' &&
      typeof copy.requirements === 'string' &&
      typeof copy.sourceTitle === 'string'
    );
    if (!validCopies) {
      throw new Error('Dữ liệu bản sao tuyển dụng trong trình duyệt không đầy đủ.');
    }

    return parsedCopies;
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
      appendText(summary, 'span', 'draft-badge', 'Nháp');
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
    sourceJobs.forEach((job) => {
      const option = document.createElement('option');
      option.value = job.id;
      option.textContent = `${job.title} — ${job.status}`;
      sourceSelect.append(option);
    });
  }

  document.querySelector('#open-copy-dialog').addEventListener('click', () => {
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

  form.addEventListener('submit', (event) => {
    event.preventDefault();
    setError('');

    const source = sourceJobs.find((job) => job.id === sourceSelect.value);
    if (!source) {
      setError('Vui lòng chọn tin tuyển dụng gốc.');
      sourceSelect.focus();
      return;
    }

    if (!storageReady) {
      setError('Không thể đọc dữ liệu đã lưu. Hãy tải lại trang trước khi tạo bản sao để tránh ghi đè dữ liệu.');
      return;
    }

    const newCopy = {
      id: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
      sourceTitle: source.title,
      title: titleInput.value.trim(),
      description: descriptionInput.value,
      requirements: requirementsInput.value,
      status: 'Nháp',
      createdAt: new Date().toISOString()
    };

    if (!newCopy.title || !newCopy.description.trim() || !newCopy.requirements.trim()) {
      setError('Vui lòng nhập đầy đủ tên tin, mô tả công việc và yêu cầu ứng viên.');
      return;
    }

    try {
      const updatedCopies = [newCopy, ...copies];
      window.localStorage.setItem(copiesStorageKey, JSON.stringify(updatedCopies));
      copies = updatedCopies;
    } catch (error) {
      setError(`Không thể lưu bản sao vào trình duyệt. ${error.message}`);
      return;
    }

    renderCopies();
    dialog.close();
    showToast('Đã tạo bản sao tuyển dụng ở trạng thái Nháp.');
  });

  try {
    populateSources();
    copies = readCopies();
    storageReady = true;
    renderCopies();
  } catch (error) {
    appendText(copyList, 'p', 'storage-error', `Không thể tải bản sao đã lưu. ${error.message}`);
  }
});

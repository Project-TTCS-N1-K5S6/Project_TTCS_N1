/**
 * ==============================================================================
 * QUẢN LÝ PIPELINE TUYỂN DỤNG DẠNG KANBAN (kanban.js)
 * ==============================================================================
 * Phục vụ Jira Issue KN-282 & các Subtasks:
 * - KN-300: Lấy dữ liệu Pipeline Kanban theo vị trí tuyển dụng
 * - KN-299: Bộ lọc vị trí tuyển dụng cho Pipeline Kanban
 * - KN-301: Xử lý cập nhật giai đoạn ứng viên bằng kéo thả
 * - KN-302: Giao diện bảng Kanban kéo thả mượt mà
 * - KN-303: Hiển thị số lượng ứng viên theo từng giai đoạn
 * - KN-304: Xác định phạm vi dữ liệu mặc định theo vị trí recruiter phụ trách
 * ==============================================================================
 */

(function () {
    'use strict';

    let currentConfig = {
        contextPath: '',
        initialRequisitionId: null,
        isRecruiterOnly: false
    };

    let pipelineData = null;
    let pendingRejection = null;
    let isUpdating = false;

    // Tên hiển thị tiếng Việt của 6 giai đoạn chuẩn
    const STAGE_NAMES = {
        'APPLIED': 'Hồ sơ mới',
        'SCREENING': 'Sàng lọc',
        'INTERVIEWING': 'Phỏng vấn',
        'OFFER': 'Đề xuất offer',
        'HIRED': 'Nhận việc',
        'REJECTED': 'Loại'
    };

    const STAGE_BADGE_CLASSES = {
        'APPLIED': 'bg-primary text-white',
        'SCREENING': 'bg-info text-white',
        'INTERVIEWING': 'bg-warning text-dark',
        'OFFER': 'bg-purple text-white',
        'HIRED': 'bg-success text-white',
        'REJECTED': 'bg-danger text-white'
    };

    /**
     * Khởi tạo bảng Kanban
     */
    function initKanban(config) {
        currentConfig = Object.assign(currentConfig, config);

        // Đăng ký sự kiện đổi vị trí tuyển dụng
        const reqSelect = document.getElementById('pipelineRequisitionSelect');
        if (reqSelect) {
            reqSelect.addEventListener('change', function () {
                const reqId = this.value;
                loadPipelineData(reqId);
            });
        }

        // Đăng ký sự kiện tìm kiếm nhanh
        const searchInput = document.getElementById('pipelineSearchInput');
        if (searchInput) {
            searchInput.addEventListener('input', function () {
                filterCardsClientSide(this.value);
            });
        }

        // Đăng ký sự kiện nút xóa tìm kiếm
        const clearSearchBtn = document.getElementById('pipelineClearSearchBtn');
        if (clearSearchBtn && searchInput) {
            clearSearchBtn.addEventListener('click', function () {
                searchInput.value = '';
                filterCardsClientSide('');
                searchInput.focus();
            });
        }

        // Đăng ký sự kiện nút reload
        const reloadBtn = document.getElementById('pipelineReloadBtn');
        if (reloadBtn) {
            reloadBtn.addEventListener('click', function () {
                const currentReqId = reqSelect ? reqSelect.value : currentConfig.initialRequisitionId;
                loadPipelineData(currentReqId);
            });
        }

        // Đăng ký sự kiện cho Modal chọn lý do loại
        initRejectionModal();

        // Tải dữ liệu ban đầu
        const initialReqId = reqSelect ? reqSelect.value : currentConfig.initialRequisitionId;
        loadPipelineData(initialReqId);
    }

    /**
     * Tải dữ liệu Pipeline từ API Backend
     */
    function loadPipelineData(requisitionId) {
        showLoadingState(true);

        let url = currentConfig.contextPath + '/api/candidates/pipeline';
        const params = new URLSearchParams();
        if (requisitionId && requisitionId !== 'ALL') {
            params.append('requisitionId', requisitionId);
        } else if (requisitionId === 'ALL') {
            params.append('requisitionId', 'ALL');
        }

        const queryString = params.toString();
        if (queryString) {
            url += '?' + queryString;
        }

        // Đồng bộ URL trình duyệt (history pushState)
        try {
            const newUrl = currentConfig.contextPath + '/candidates/pipeline' + (queryString ? '?' + queryString : '');
            window.history.replaceState({ requisitionId: requisitionId }, '', newUrl);
        } catch (e) {}

        fetch(url, {
            headers: {
                'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(response => {
            if (response.status === 401) {
                window.location.href = currentConfig.contextPath + '/auth/login';
                return null;
            }
            if (!response.ok) {
                return response.json().then(errData => {
                    throw new Error(errData.message || 'Lỗi tải dữ liệu Pipeline từ hệ thống (Mã ' + response.status + ').');
                }).catch(err => {
                    throw new Error(err.message || 'Lỗi tải dữ liệu Pipeline từ hệ thống (Mã ' + response.status + ').');
                });
            }
            return response.json();
        })
        .then(data => {
            showLoadingState(false);
            if (!data) return;

            if (data.success) {
                pipelineData = data;
                renderKanbanBoard(data);
                populateRejectionDropdown(data.rejectionReasons);
            } else {
                showToast(data.message || 'Không thể tải dữ liệu Pipeline.', 'danger');
            }
        })
        .catch(err => {
            showLoadingState(false);
            showToast(err.message || 'Lỗi kết nối khi tải dữ liệu Pipeline.', 'danger');
        });
    }

    /**
     * Hiển thị trạng thái đang tải (Loading overlay)
     */
    function showLoadingState(isLoading) {
        const overlay = document.getElementById('kanbanLoadingOverlay');
        if (overlay) {
            overlay.style.display = isLoading ? 'flex' : 'none';
        }
        const reloadBtn = document.getElementById('pipelineReloadBtn');
        if (reloadBtn) {
            const icon = reloadBtn.querySelector('i');
            if (icon) {
                if (isLoading) {
                    icon.classList.add('spin-animation');
                } else {
                    icon.classList.remove('spin-animation');
                }
            }
        }
    }

    /**
     * Render toàn bộ 6 cột của bảng Kanban từ dữ liệu API
     */
    function renderKanbanBoard(data) {
        const stages = data.stages || [];

        // Cập nhật thông tin tổng quan trên thanh công cụ
        updateSummaryHeader(data);

        // Render từng cột giai đoạn
        stages.forEach(stage => {
            const columnContainer = document.querySelector(`.kanban-column[data-stage="${stage.code}"]`);
            if (!columnContainer) return;

            // Cập nhật số lượng đếm trên header cột
            const badgeCount = columnContainer.querySelector('.kanban-column-count');
            if (badgeCount) {
                badgeCount.textContent = stage.count || 0;
            }

            // Danh sách thẻ trong cột
            const cardsContainer = columnContainer.querySelector('.kanban-cards-container');
            if (!cardsContainer) return;

            cardsContainer.innerHTML = '';

            const candidates = stage.candidates || [];
            if (candidates.length === 0) {
                cardsContainer.innerHTML = createEmptyStateHtml(stage.code);
            } else {
                candidates.forEach(cand => {
                    const cardEl = createCandidateCardElement(cand, stage.code);
                    cardsContainer.appendChild(cardEl);
                });
            }

            // Gán sự kiện kéo thả cho container của cột
            setupDropZone(cardsContainer, stage.code);
        });

        // Áp dụng bộ lọc tìm kiếm hiện tại nếu có
        const searchInput = document.getElementById('pipelineSearchInput');
        if (searchInput && searchInput.value.trim() !== '') {
            filterCardsClientSide(searchInput.value);
        }
    }

    /**
     * Cập nhật thông tin tóm tắt trên header
     */
    function updateSummaryHeader(data) {
        const totalCandidatesEl = document.getElementById('summaryTotalCandidates');
        if (totalCandidatesEl) {
            totalCandidatesEl.textContent = data.totalCandidates || 0;
        }

        const activeCandidatesEl = document.getElementById('summaryActiveCandidates');
        if (activeCandidatesEl) {
            activeCandidatesEl.textContent = data.totalActiveCandidates || 0;
        }

        const positionBadgeEl = document.getElementById('summaryPositionTitle');
        if (positionBadgeEl) {
            if (data.currentRequisition) {
                positionBadgeEl.innerHTML = '<i class="bi bi-briefcase-fill me-1"></i>' +
                    escapeHtml(data.currentRequisition.title) +
                    ' <span class="badge bg-light text-dark border ms-1">' + escapeHtml(data.currentRequisition.code) + '</span>';
            } else {
                positionBadgeEl.innerHTML = '<i class="bi bi-grid-fill me-1"></i>Tất cả vị trí tuyển dụng';
            }
        }
    }

    /**
     * Tạo phần tử HTML cho thẻ ứng viên (Kanban Card)
     */
    function createCandidateCardElement(cand, stageCode) {
        const card = document.createElement('div');
        card.className = 'kanban-card';
        card.setAttribute('draggable', 'true');
        card.setAttribute('data-candidate-id', cand.id);
        card.setAttribute('data-stage', stageCode);
        card.setAttribute('data-full-name', cand.fullName || '');
        card.setAttribute('data-email', cand.email || '');
        card.setAttribute('data-phone', cand.phone || '');
        card.setAttribute('data-position', cand.requisitionTitle || '');

        // Avatar chữ cái đầu
        const initials = getInitials(cand.fullName);

        // Tags
        let tagsHtml = '';
        if (cand.tagList && cand.tagList.length > 0) {
            cand.tagList.slice(0, 3).forEach(t => {
                tagsHtml += `<span class="badge bg-light text-secondary border me-1 mb-1 font-monospace text-xs">${escapeHtml(t)}</span>`;
            });
            if (cand.tagList.length > 3) {
                tagsHtml += `<span class="badge bg-light text-muted border text-xs">+${cand.tagList.length - 3}</span>`;
            }
        }

        // Cảnh báo quá hạn SLA
        let slaHtml = '';
        if (cand.isSlaOverdue) {
            slaHtml = `<span class="badge bg-danger-subtle text-danger border border-danger me-1 text-xs" title="Hồ sơ đã quá hạn SLA xử lý">
                <i class="bi bi-exclamation-triangle-fill me-1"></i>Quá hạn SLA
            </span>`;
        }

        // Số ngày ở giai đoạn hiện tại
        let daysInStageText = cand.daysInCurrentStage !== undefined ? cand.daysInCurrentStage : 0;
        let daysBadgeHtml = `<span class="text-muted text-xs d-flex align-items-center gap-1" title="Thời gian lưu lại ở giai đoạn này">
            <i class="bi bi-clock-history"></i> ${daysInStageText} ngày
        </span>`;

        // Xem CV
        let cvHtml = '';
        if (cand.cvUrl && cand.cvUrl.trim() !== '') {
            cvHtml = `<a href="${currentConfig.contextPath}/candidates/cv-preview?id=${encodeURIComponent(cand.id)}" target="_blank"
                        class="btn btn-xs btn-outline-secondary py-0 px-2 rounded-pill text-xs d-inline-flex align-items-center gap-1"
                        title="Xem trước CV ứng viên" onclick="event.stopPropagation();">
                <i class="bi bi-file-earmark-pdf-fill text-danger"></i> Xem CV
            </a>`;
        }

        card.innerHTML = `
            <div class="card-inner p-3">
                <div class="d-flex align-items-start justify-content-between gap-2 mb-2">
                    <div class="d-flex align-items-center gap-2">
                        <div class="cand-avatar-circle" title="${escapeHtml(cand.fullName)}">
                            ${initials}
                        </div>
                        <div>
                            <a href="${currentConfig.contextPath}/candidates/profile?id=${encodeURIComponent(cand.id)}" 
                               class="cand-name-link fw-bold text-dark text-decoration-none" 
                               title="Xem hồ sơ chi tiết">
                                ${escapeHtml(cand.fullName)}
                            </a>
                            ${cand.trackingCode ? `<div class="text-xs text-muted font-monospace">${escapeHtml(cand.trackingCode)}</div>` : ''}
                        </div>
                    </div>
                    <a href="${currentConfig.contextPath}/candidates/profile?id=${encodeURIComponent(cand.id)}" 
                       class="btn btn-sm btn-light p-1 rounded-circle shadow-none text-muted" 
                       title="Mở hồ sơ chi tiết">
                        <i class="bi bi-arrow-up-right"></i>
                    </a>
                </div>

                <div class="cand-position-text text-secondary small mb-2 d-flex align-items-center gap-1 text-truncate" title="${escapeHtml(cand.requisitionTitle)}">
                    <i class="bi bi-briefcase text-muted"></i>
                    <span class="text-truncate">${escapeHtml(cand.requisitionTitle)}</span>
                </div>

                <div class="cand-contact-info mb-2 small text-muted">
                    <div class="d-flex align-items-center gap-2 text-truncate mb-1" title="${escapeHtml(cand.email)}">
                        <i class="bi bi-envelope text-secondary"></i>
                        <span class="text-truncate">${escapeHtml(cand.email)}</span>
                    </div>
                    ${cand.phone ? `
                    <div class="d-flex align-items-center gap-2 text-truncate" title="${escapeHtml(cand.phone)}">
                        <i class="bi bi-telephone text-secondary"></i>
                        <span>${escapeHtml(cand.phone)}</span>
                    </div>` : ''}
                </div>

                ${tagsHtml ? `<div class="cand-tags mb-2">${tagsHtml}</div>` : ''}

                <div class="cand-footer pt-2 border-top d-flex align-items-center justify-content-between flex-wrap gap-1">
                    <div class="d-flex align-items-center gap-1">
                        ${slaHtml}
                        ${daysBadgeHtml}
                    </div>
                    <div>
                        ${cvHtml}
                    </div>
                </div>
            </div>
        `;

        // Đăng ký sự kiện Drag cho thẻ
        setupCardDragEvents(card);

        return card;
    }

    /**
     * Tạo thông báo cột trống
     */
    function createEmptyStateHtml(stageCode) {
        return `
            <div class="kanban-empty-state text-center py-4 px-2 text-muted">
                <i class="bi bi-inbox fs-3 opacity-50 mb-1 d-block"></i>
                <div class="text-xs">Không có ứng viên ở giai đoạn này</div>
                <div class="text-xs text-secondary mt-1">Kéo thả thẻ vào đây để chuyển</div>
            </div>
        `;
    }

    /**
     * Gán sự kiện kéo thả cho thẻ ứng viên
     */
    function setupCardDragEvents(card) {
        card.addEventListener('dragstart', function (e) {
            if (isUpdating) {
                e.preventDefault();
                return;
            }

            card.classList.add('is-dragging');
            const candidateId = card.getAttribute('data-candidate-id');
            const stage = card.getAttribute('data-stage');

            e.dataTransfer.effectAllowed = 'move';
            e.dataTransfer.setData('text/plain', candidateId);
            e.dataTransfer.setData('application/json', JSON.stringify({
                candidateId: candidateId,
                sourceStage: stage
            }));

            // Thêm class lên toàn bộ board để nhận diện đang drag
            document.querySelector('.kanban-board-container')?.classList.add('is-drag-active');
        });

        card.addEventListener('dragend', function () {
            card.classList.remove('is-dragging');
            document.querySelector('.kanban-board-container')?.classList.remove('is-drag-active');
            document.querySelectorAll('.kanban-cards-container').forEach(c => {
                c.classList.remove('drag-over');
            });
        });
    }

    /**
     * Gán sự kiện vùng thả (Drop zone) cho từng cột
     */
    function setupDropZone(cardsContainer, targetStageCode) {
        cardsContainer.addEventListener('dragover', function (e) {
            e.preventDefault();
            e.dataTransfer.dropEffect = 'move';
            if (!cardsContainer.classList.contains('drag-over')) {
                cardsContainer.classList.add('drag-over');
            }
        });

        cardsContainer.addEventListener('dragleave', function (e) {
            // Chỉ xóa nếu thực sự rời khỏi container
            if (!cardsContainer.contains(e.relatedTarget)) {
                cardsContainer.classList.remove('drag-over');
            }
        });

        cardsContainer.addEventListener('drop', function (e) {
            e.preventDefault();
            cardsContainer.classList.remove('drag-over');

            let candidateId = null;
            let sourceStage = null;

            try {
                const rawJson = e.dataTransfer.getData('application/json');
                if (rawJson) {
                    const parsed = JSON.parse(rawJson);
                    candidateId = parsed.candidateId;
                    sourceStage = parsed.sourceStage;
                }
            } catch (err) {}

            if (!candidateId) {
                candidateId = e.dataTransfer.getData('text/plain');
            }

            if (!candidateId) return;

            // Tìm thẻ ứng viên tương ứng trên DOM
            const cardEl = document.querySelector(`.kanban-card[data-candidate-id="${candidateId}"]`);
            if (!cardEl) return;

            if (!sourceStage) {
                sourceStage = cardEl.getAttribute('data-stage');
            }

            // Nếu kéo thả vào chính cột hiện tại thì bỏ qua
            if (sourceStage === targetStageCode) {
                return;
            }

            // [KN-282 & KN-283]: Nếu chuyển sang cột "Loại" (REJECTED), bắt buộc mở modal chọn lý do loại
            if (targetStageCode === 'REJECTED') {
                promptRejectionReason(candidateId, sourceStage, targetStageCode, cardEl);
                return;
            }

            // Chuyển giai đoạn thông thường
            executeStageChange(candidateId, sourceStage, targetStageCode, null, cardEl);
        });
    }

    /**
     * Mở modal yêu cầu chọn lý do loại khi kéo sang cột Loại (REJECTED)
     */
    function promptRejectionReason(candidateId, sourceStage, targetStageCode, cardEl) {
        const candidateName = cardEl.getAttribute('data-full-name') || 'Ứng viên';
        const positionTitle = cardEl.getAttribute('data-position') || '';

        const nameEl = document.getElementById('rejectModalCandidateName');
        if (nameEl) nameEl.textContent = candidateName;

        const posEl = document.getElementById('rejectModalPositionTitle');
        if (posEl) posEl.textContent = positionTitle;

        // Lưu thông tin chờ xử lý
        pendingRejection = {
            candidateId: candidateId,
            sourceStage: sourceStage,
            targetStage: targetStageCode,
            cardEl: cardEl
        };

        const modalEl = document.getElementById('rejectionReasonModal');
        if (modalEl && window.bootstrap && window.bootstrap.Modal) {
            const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
            modal.show();
        } else {
            // Fallback nếu không có bootstrap modal instance
            const reason = prompt(`Nhập lý do loại ứng viên "${candidateName}":`);
            if (reason) {
                executeStageChange(candidateId, sourceStage, targetStageCode, reason, cardEl);
            }
        }
    }

    /**
     * Khởi tạo Modal chọn lý do loại
     */
    function initRejectionModal() {
        const modalEl = document.getElementById('rejectionReasonModal');
        const confirmBtn = document.getElementById('confirmRejectBtn');
        const selectEl = document.getElementById('rejectionReasonSelect');

        if (confirmBtn && selectEl) {
            confirmBtn.addEventListener('click', function () {
                if (!pendingRejection) return;

                const reasonId = selectEl.value;
                if (!reasonId || reasonId.trim() === '') {
                    alert('Vui lòng chọn một lý do loại hợp lệ từ danh mục!');
                    selectEl.focus();
                    return;
                }

                const { candidateId, sourceStage, targetStage, cardEl } = pendingRejection;

                // Đóng modal
                if (modalEl && window.bootstrap && window.bootstrap.Modal) {
                    const modal = bootstrap.Modal.getInstance(modalEl);
                    if (modal) modal.hide();
                }

                pendingRejection = null;
                executeStageChange(candidateId, sourceStage, targetStage, reasonId, cardEl);
            });
        }

        // Khi đóng modal mà không xác nhận: giữ nguyên vị trí thẻ
        if (modalEl) {
            modalEl.addEventListener('hidden.bs.modal', function () {
                pendingRejection = null;
                if (selectEl) selectEl.selectedIndex = 0;
            });
        }
    }

    /**
     * Điền danh sách lý do loại vào dropdown
     */
    function populateRejectionDropdown(reasons) {
        const selectEl = document.getElementById('rejectionReasonSelect');
        if (!selectEl) return;

        selectEl.innerHTML = '<option value="">-- Chọn lý do loại hồ sơ --</option>';
        if (reasons && reasons.length > 0) {
            reasons.forEach(r => {
                const opt = document.createElement('option');
                opt.value = r.id;
                opt.textContent = r.value;
                selectEl.appendChild(opt);
            });
        }
    }

    /**
     * [KN-301]: Gửi yêu cầu cập nhật giai đoạn lên Backend và đồng bộ giao diện
     */
    function executeStageChange(candidateId, sourceStage, targetStage, rejectionReasonId, cardEl) {
        if (isUpdating) return;
        isUpdating = true;

        // Lưu thông tin vị trí cũ để rollback nếu thất bại
        const sourceContainer = document.querySelector(`.kanban-column[data-stage="${sourceStage}"] .kanban-cards-container`);
        const targetContainer = document.querySelector(`.kanban-column[data-stage="${targetStage}"] .kanban-cards-container`);

        if (!sourceContainer || !targetContainer || !cardEl) {
            isUpdating = false;
            return;
        }

        const candidateName = cardEl.getAttribute('data-full-name') || 'Ứng viên';

        // 1. Thao tác di chuyển tạm thời trên UI (Optimistic UI)
        // Xóa empty state ở target container nếu có
        const targetEmpty = targetContainer.querySelector('.kanban-empty-state');
        if (targetEmpty) targetEmpty.remove();

        // Đưa thẻ vào đầu target container
        targetContainer.insertBefore(cardEl, targetContainer.firstChild);
        cardEl.setAttribute('data-stage', targetStage);
        cardEl.classList.add('card-saving');

        // Cập nhật số lượng đếm tạm thời
        updateStageCountsOptimistic(sourceStage, targetStage);

        // 2. Gửi request POST lên Backend
        const payload = {
            candidateId: candidateId,
            targetStage: targetStage,
            rejectionReasonId: rejectionReasonId || ''
        };

        fetch(currentConfig.contextPath + '/api/candidates/pipeline/update-stage', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: JSON.stringify(payload)
        })
        .then(response => {
            if (response.status === 401) {
                window.location.href = currentConfig.contextPath + '/auth/login';
                return null;
            }
            return response.json().then(data => {
                if (!response.ok) {
                    throw new Error(data.message || 'Lỗi cập nhật giai đoạn trên máy chủ (Mã ' + response.status + ').');
                }
                return data;
            });
        })
        .then(data => {
            isUpdating = false;
            cardEl.classList.remove('card-saving');

            if (!data) return;

            if (data.success) {
                // Thành công: Hiển thị thông báo Toast xanh
                const stageName = STAGE_NAMES[targetStage] || targetStage;
                showToast(`Đã chuyển ứng viên <strong>${escapeHtml(candidateName)}</strong> sang giai đoạn <strong>${stageName}</strong> thành công!`, 'success');

                // Kiểm tra nếu source container trống thì hiển thị lại empty state
                checkAndInsertEmptyState(sourceContainer, sourceStage);

                // Cập nhật lại ngày vào vòng nếu cần
                const daysInStageEl = cardEl.querySelector('.cand-footer .text-muted');
                if (daysInStageEl) {
                    daysInStageEl.innerHTML = '<i class="bi bi-clock-history"></i> 0 ngày';
                }
            } else {
                // Thất bại từ API: Khôi phục lại vị trí ban đầu
                rollbackCardMove(cardEl, sourceContainer, targetContainer, sourceStage, targetStage, data.message || 'Cập nhật thất bại.');
            }
        })
        .catch(err => {
            isUpdating = false;
            cardEl.classList.remove('card-saving');
            // Thất bại kết nối hoặc lỗi server: Khôi phục lại thẻ về vị trí ban đầu
            rollbackCardMove(cardEl, sourceContainer, targetContainer, sourceStage, targetStage, err.message || 'Lỗi hệ thống khi cập nhật trạng thái.');
        });
    }

    /**
     * Khôi phục thẻ về vị trí ban đầu khi cập nhật thất bại (Rollback UI)
     */
    function rollbackCardMove(cardEl, sourceContainer, targetContainer, sourceStage, targetStage, errorMessage) {
        // Trả thẻ về cột cũ
        cardEl.setAttribute('data-stage', sourceStage);
        sourceContainer.appendChild(cardEl);

        // Khôi phục lại số đếm ở 2 cột
        updateStageCountsOptimistic(targetStage, sourceStage);

        // Kiểm tra empty state cho cả 2 container
        checkAndInsertEmptyState(sourceContainer, sourceStage);
        checkAndInsertEmptyState(targetContainer, targetStage);

        // Hiển thị thông báo lỗi rõ ràng bằng tiếng Việt
        showToast(errorMessage || 'Không thể chuyển giai đoạn ứng viên. Thẻ đã được khôi phục về vị trí ban đầu.', 'danger');
    }

    /**
     * Cập nhật số lượng đếm tức thì trên UI
     */
    function updateStageCountsOptimistic(fromStage, toStage) {
        const fromBadge = document.querySelector(`.kanban-column[data-stage="${fromStage}"] .kanban-column-count`);
        if (fromBadge) {
            let count = parseInt(fromBadge.textContent, 10) || 0;
            fromBadge.textContent = Math.max(0, count - 1);
        }

        const toBadge = document.querySelector(`.kanban-column[data-stage="${toStage}"] .kanban-column-count`);
        if (toBadge) {
            let count = parseInt(toBadge.textContent, 10) || 0;
            toBadge.textContent = count + 1;
        }
    }

    /**
     * Thêm empty state nếu container không còn thẻ nào
     */
    function checkAndInsertEmptyState(container, stageCode) {
        const cards = container.querySelectorAll('.kanban-card');
        const emptyState = container.querySelector('.kanban-empty-state');
        if (cards.length === 0 && !emptyState) {
            container.innerHTML = createEmptyStateHtml(stageCode);
        } else if (cards.length > 0 && emptyState) {
            emptyState.remove();
        }
    }

    /**
     * Lọc thẻ ứng viên nhanh từ thanh tìm kiếm
     */
    function filterCardsClientSide(query) {
        const q = (query || '').trim().toLowerCase();
        const allCards = document.querySelectorAll('.kanban-card');

        allCards.forEach(card => {
            const name = (card.getAttribute('data-full-name') || '').toLowerCase();
            const email = (card.getAttribute('data-email') || '').toLowerCase();
            const phone = (card.getAttribute('data-phone') || '').toLowerCase();
            const pos = (card.getAttribute('data-position') || '').toLowerCase();

            if (!q || name.includes(q) || email.includes(q) || phone.includes(q) || pos.includes(q)) {
                card.style.display = '';
            } else {
                card.style.display = 'none';
            }
        });

        // Cập nhật lại số đếm các thẻ đang hiển thị trên từng cột
        document.querySelectorAll('.kanban-column').forEach(col => {
            const visibleCards = col.querySelectorAll('.kanban-card:not([style*="display: none"])');
            const countBadge = col.querySelector('.kanban-column-count');
            if (countBadge) {
                countBadge.textContent = visibleCards.length;
            }
        });
    }

    /**
     * Hiển thị thông báo Toast đẹp mắt, tự động biến mất
     */
    function showToast(message, type) {
        let toastContainer = document.getElementById('kanbanToastContainer');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'kanbanToastContainer';
            toastContainer.className = 'toast-container position-fixed top-0 end-0 p-3';
            toastContainer.style.zIndex = '9999';
            document.body.appendChild(toastContainer);
        }

        const toastId = 'toast-' + Date.now();
        const iconClass = type === 'success' ? 'bi-check-circle-fill text-success' :
                          type === 'warning' ? 'bi-exclamation-triangle-fill text-warning' : 'bi-x-circle-fill text-danger';

        const toastEl = document.createElement('div');
        toastEl.className = 'toast align-items-center shadow-lg border-0 mb-2';
        toastEl.id = toastId;
        toastEl.setAttribute('role', 'alert');
        toastEl.setAttribute('aria-live', 'assertive');
        toastEl.setAttribute('aria-atomic', 'true');
        toastEl.style.minWidth = '300px';

        toastEl.innerHTML = `
            <div class="d-flex p-3 bg-white border border-${type || 'secondary'} rounded-3">
                <div class="fs-5 me-2 flex-shrink-0"><i class="bi ${iconClass}"></i></div>
                <div class="toast-body p-0 flex-grow-1 text-dark small">
                    ${message}
                </div>
                <button type="button" class="btn-close ms-2 me-0 shadow-none" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        `;

        toastContainer.appendChild(toastEl);

        if (window.bootstrap && window.bootstrap.Toast) {
            const toast = new bootstrap.Toast(toastEl, { delay: 4500 });
            toast.show();
            toastEl.addEventListener('hidden.bs.toast', function () {
                toastEl.remove();
            });
        } else {
            setTimeout(() => {
                toastEl.remove();
            }, 4500);
        }
    }

    /**
     * Tiện ích lấy chữ cái đầu
     */
    function getInitials(name) {
        if (!name) return 'U';
        const parts = name.trim().split(/\s+/);
        if (parts.length === 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    /**
     * Tiện ích escape HTML
     */
    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    // Xuất ra global scope
    window.IrmsKanban = {
        init: initKanban,
        reload: loadPipelineData
    };

})();

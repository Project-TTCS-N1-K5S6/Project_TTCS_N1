/**
 * ==============================================================================
 * JAVASCRIPT GIAO DIỆN CHUNG & TƯƠNG TÁC HỆ THỐNG (main.js)
 * ==============================================================================
 * Phục vụ các User Story:
 * - US 6: Điều hướng menu, hỗ trợ trải nghiệm di động responsive trên màn hình 360px.
 * - US 7: Tương tác thông báo và trải nghiệm người dùng.
 * ==============================================================================
 */

document.addEventListener('DOMContentLoaded', () => {
    // --------------------------------------------------------------------------
    // [US 6 - Đạt]: Sidebar Toggle trên thiết bị di động (hỗ trợ màn hình 360px)
    // Cho phép mở Sidebar dạng Drawer trượt và đóng lại khi chạm vào Backdrop.
    // --------------------------------------------------------------------------
    const toggleBtn = document.getElementById('sidebar-toggle');
    const sidebar = document.getElementById('app-sidebar');
    const backdrop = document.getElementById('sidebar-backdrop');

    if (toggleBtn && sidebar && backdrop) {
        toggleBtn.addEventListener('click', () => {
            sidebar.classList.toggle('show');
            backdrop.classList.toggle('show');
        });

        backdrop.addEventListener('click', () => {
            sidebar.classList.remove('show');
            backdrop.classList.remove('show');
        });
    }

    // 2. Tự động đánh dấu Menu Active dựa theo đường dẫn URL hiện tại
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('.sidebar-menu .nav-link');
    navLinks.forEach(link => {
        const href = link.getAttribute('href');
        if (href && currentPath.includes(href) && href !== '#') {
            link.classList.add('active');
        }
    });

    // 3. Khởi tạo toàn bộ Bootstrap Tooltip nếu có
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl));

    // 4. Khởi tạo các thông báo nổi (Toast) từ server render sẵn
    initServerRenderedToasts();
});

// ==============================================================================
// HỆ THỐNG THÔNG BÁO NỔI GÓC DƯỚI BÊN PHẢI (FLOATING TOAST NOTIFICATION SYSTEM)
// ==============================================================================

function getToastContainer() {
    let container = document.getElementById('toastNotificationContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastNotificationContainer';
        container.className = 'toast-notification-container';
        container.setAttribute('aria-live', 'polite');
        container.setAttribute('aria-atomic', 'true');
        document.body.appendChild(container);
    }
    return container;
}

/**
 * Hiển thị thông báo nổi ở góc dưới bên phải màn hình
 * @param {string} message Nội dung thông báo
 * @param {string} type Loại thông báo: 'success' | 'error' | 'danger' | 'warning' | 'info'
 * @param {string} [title] Tiêu đề thông báo
 * @param {number} [duration=5000] Thời gian tự đóng (ms)
 */
window.showToast = function(message, type = 'info', title = null, duration = 5000) {
    if (!message) return;
    const normType = (type === 'danger') ? 'error' : type.toLowerCase();
    const container = getToastContainer();

    const config = {
        success: {
            title: title || 'Thành công',
            icon: 'bi-check-circle-fill',
            class: 'toast-item-success'
        },
        error: {
            title: title || 'Đã xảy ra lỗi',
            icon: 'bi-exclamation-octagon-fill',
            class: 'toast-item-error'
        },
        warning: {
            title: title || 'Cảnh báo',
            icon: 'bi-exclamation-triangle-fill',
            class: 'toast-item-warning'
        },
        info: {
            title: title || 'Thông báo',
            icon: 'bi-info-circle-fill',
            class: 'toast-item-info'
        }
    };

    const cfg = config[normType] || config.info;

    const toastItem = document.createElement('div');
    toastItem.className = `toast-notification-item ${cfg.class}`;
    toastItem.innerHTML = `
        <div class="toast-icon-box"><i class="bi ${cfg.icon}"></i></div>
        <div class="toast-body-content">
            <div class="toast-header-title">${escapeToastHtml(cfg.title)}</div>
            <div class="toast-message-text">${escapeToastHtml(message)}</div>
        </div>
        <button type="button" class="toast-close-btn" aria-label="Đóng"><i class="bi bi-x-lg"></i></button>
        <div class="toast-progress-bar"></div>
    `;

    container.appendChild(toastItem);
    bindToastLifecycle(toastItem, duration);
};

window.dismissToast = function(toastItem) {
    if (!toastItem || toastItem.classList.contains('toast-closing')) return;
    toastItem.classList.add('toast-closing');
    setTimeout(() => {
        if (toastItem.parentNode) {
            toastItem.parentNode.removeChild(toastItem);
        }
    }, 300);
};

function bindToastLifecycle(toastItem, duration = 5000) {
    const closeBtn = toastItem.querySelector('.toast-close-btn');
    if (closeBtn) {
        closeBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            window.dismissToast(toastItem);
        });
    }

    const progressBar = toastItem.querySelector('.toast-progress-bar');
    let startTime = Date.now();
    let remaining = duration;
    let timerId = null;

    function startTimer() {
        if (progressBar) {
            progressBar.style.transition = `width ${remaining}ms linear`;
            progressBar.style.width = '0%';
        }
        startTime = Date.now();
        timerId = setTimeout(() => {
            window.dismissToast(toastItem);
        }, remaining);
    }

    function pauseTimer() {
        if (timerId) {
            clearTimeout(timerId);
            timerId = null;
        }
        if (progressBar) {
            const elapsed = Date.now() - startTime;
            remaining = Math.max(0, remaining - elapsed);
            const computedWidth = getComputedStyle(progressBar).width;
            progressBar.style.transition = 'none';
            progressBar.style.width = computedWidth;
        }
    }

    toastItem.addEventListener('mouseenter', pauseTimer);
    toastItem.addEventListener('mouseleave', () => {
        if (remaining > 0) startTimer();
    });

    // Bắt đầu đếm ngược
    setTimeout(startTimer, 50);
}

function initServerRenderedToasts() {
    const container = document.getElementById('toastNotificationContainer');
    if (!container) return;
    const items = container.querySelectorAll('.toast-notification-item');
    items.forEach(item => {
        const autoDismiss = parseInt(item.getAttribute('data-auto-dismiss') || '5000', 10);
        bindToastLifecycle(item, autoDismiss);
    });
}

function escapeToastHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

// ==============================================================================
// GHI ĐÈ BỘ BÁO ĐỘNG TRÌNH DUYỆT (OVERRIDE DEFAULT BROWSER ALERT)
// Tự động chuyển toàn bộ hộp thoại alert() truyền thống thành thông báo nổi góc dưới
// ==============================================================================
window.alert = function(message) {
    window.showToast(message, 'warning', 'Thông báo hệ thống', 6000);
};


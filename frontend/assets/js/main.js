/**
 * ============================================================
 * IRMS - Internal Recruitment Management System
 * Client-Side JavaScript: Core Layout & Interactivity
 * File: frontend/assets/js/main.js
 * ============================================================
 */

document.addEventListener('DOMContentLoaded', () => {
    // 1. Sidebar Toggle trên thiết bị di động / màn hình nhỏ
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

    // 3. Tự động ẩn các thông báo Flash Alert sau 5 giây
    const flashAlerts = document.querySelectorAll('.alert-dismissible');
    flashAlerts.forEach(alert => {
        setTimeout(() => {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            if (bsAlert) bsAlert.close();
        }, 5000);
    });

    // 4. Khởi tạo toàn bộ Bootstrap Tooltip nếu có
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl));

    // 5. Cập nhật thông tin User trên Header từ LocalStorage
    try {
        const raw = localStorage.getItem('irms_user');
        if (raw) {
            const u = JSON.parse(raw);
            const nameEl = document.getElementById('headerUserName');
            const roleEl = document.getElementById('headerUserRole');
            const avatarEl = document.getElementById('headerAvatar');

            if (nameEl) nameEl.innerText = u.fullName || 'Quản trị viên';
            if (roleEl) roleEl.innerText = (u.roles && u.roles[0]) ? u.roles[0].name : (u.jobTitle || 'Quản trị hệ thống');
            if (avatarEl) avatarEl.innerText = (u.fullName || 'A').charAt(0).toUpperCase();
        }
    } catch (e) {}
});

function handleLogout() {
    localStorage.removeItem('irms_user');
    localStorage.removeItem('irms_token');
    window.location.href = 'index.html';
}

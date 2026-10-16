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
});

/**
 * ============================================================
 * IRMS - Client-Side Search & Filter Handler
 * File: frontend/assets/js/search.js
 * ============================================================
 */

function debounce(func, wait = 300) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/**
 * Lọc trực tiếp các hàng trong bảng tại Client
 */
function filterTableRows(searchInputId, tableSelector) {
    const searchInput = document.getElementById(searchInputId);
    if (!searchInput) return;

    searchInput.addEventListener('input', debounce((e) => {
        const keyword = e.target.value.toLowerCase().trim();
        const rows = document.querySelectorAll(`${tableSelector} tbody tr`);

        rows.forEach(row => {
            const text = row.innerText.toLowerCase();
            if (text.includes(keyword)) {
                row.style.display = '';
            } else {
                row.style.display = 'none';
            }
        });
    }, 250));
}

window.filterTableRows = filterTableRows;

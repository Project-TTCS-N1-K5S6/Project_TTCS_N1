/**
 * ============================================================
 * IRMS - Client-Side Pagination Handler
 * File: frontend/assets/js/pagination.js
 * ============================================================
 */

function goToPage(pageNumber, formId = null) {
    if (formId) {
        const form = document.getElementById(formId);
        if (form) {
            let pageInput = form.querySelector('input[name="page"]');
            if (!pageInput) {
                pageInput = document.createElement('input');
                pageInput.type = 'hidden';
                pageInput.name = 'page';
                form.appendChild(pageInput);
            }
            pageInput.value = pageNumber;
            form.submit();
            return;
        }
    }

    const currentUrl = new URL(window.location.href);
    currentUrl.searchParams.set('page', pageNumber);
    window.location.href = currentUrl.toString();
}

window.goToPage = goToPage;

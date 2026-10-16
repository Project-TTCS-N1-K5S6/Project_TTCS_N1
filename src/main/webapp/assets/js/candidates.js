(() => {
    const tagsModal = document.getElementById('candidateTagsModal');
    const screeningModal = document.getElementById('candidateScreeningModal');

    if (tagsModal) {
        tagsModal.addEventListener('show.bs.modal', event => {
            const trigger = event.relatedTarget;
            const row = trigger.closest('tr');
            const selectedIds = new Set(
                Array.from(row.querySelectorAll('.js-current-candidate-tags [data-tag-id]'))
                    .map(tag => tag.dataset.tagId)
            );

            tagsModal.querySelector('[name="candidateId"]').value = trigger.dataset.candidateId;
            tagsModal.querySelector('.js-screening-candidate-name').textContent = trigger.dataset.candidateName;
            tagsModal.querySelectorAll('[name="tagIds"]').forEach(input => {
                input.checked = selectedIds.has(input.value);
            });
        });
    }

    if (screeningModal) {
        screeningModal.addEventListener('show.bs.modal', event => {
            const trigger = event.relatedTarget;
            const row = trigger.closest('tr');
            const form = screeningModal.querySelector('form');
            const ratingInput = form.querySelector('[name="rating"]');
            const noteInput = form.querySelector('[name="internalNote"]');
            const historyTarget = screeningModal.querySelector('.js-internal-note-history');
            const historySource = row.querySelector('[id^="candidateNoteHistory-"]');
            const rating = trigger.dataset.rating || '';

            form.querySelector('[name="candidateId"]').value = trigger.dataset.candidateId;
            screeningModal.querySelector('.js-screening-candidate-name').textContent = trigger.dataset.candidateName;
            ratingInput.value = rating;
            if (noteInput) noteInput.value = '';
            if (historyTarget) {
                historyTarget.innerHTML = historySource && historySource.innerHTML.trim()
                    ? historySource.innerHTML
                    : '<p class="text-muted mb-0">Chưa có ghi chú nội bộ.</p>';
            }

            screeningModal.querySelectorAll('.js-rating-star').forEach(star => {
                const selected = Number(star.dataset.rating) <= Number(rating);
                star.setAttribute('aria-pressed', String(selected));
                star.querySelector('i').className = selected ? 'bi bi-star-fill' : 'bi bi-star';
            });
        });

        screeningModal.querySelectorAll('.js-rating-star').forEach(star => {
            star.addEventListener('click', () => {
                const selectedRating = Number(star.dataset.rating);
                screeningModal.querySelector('[name="rating"]').value = selectedRating;
                screeningModal.querySelectorAll('.js-rating-star').forEach(item => {
                    const selected = Number(item.dataset.rating) <= selectedRating;
                    item.setAttribute('aria-pressed', String(selected));
                    item.querySelector('i').className = selected ? 'bi bi-star-fill' : 'bi bi-star';
                });
            });
        });

        screeningModal.querySelector('form').addEventListener('submit', event => {
            const rating = screeningModal.querySelector('[name="rating"]').value;
            const noteInput = screeningModal.querySelector('[name="internalNote"]');
            const note = noteInput ? noteInput.value.trim() : '';
            if (!rating && !note) {
                event.preventDefault();
                window.alert('Hãy chọn số sao hoặc nhập ghi chú nội bộ.');
            }
        });
    }
})();

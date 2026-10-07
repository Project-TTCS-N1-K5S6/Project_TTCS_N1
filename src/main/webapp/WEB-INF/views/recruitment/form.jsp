<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="${isEdit ? 'Chỉnh sửa Yêu cầu tuyển dụng' : 'Tạo mới Yêu cầu tuyển dụng'}" />
<c:set var="breadcrumb" value="Yêu cầu tuyển dụng" />
<c:set var="activeMenu" value="requisitions" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <div class="d-flex align-items-center gap-2 mb-1">
                    <a href="${pageContext.request.contextPath}/recruitment-requests" class="btn btn-sm btn-outline-secondary py-1 px-2" title="Quay lại">
                        <i class="bi bi-arrow-left"></i>
                    </a>
                    <h4 class="fw-bold mb-0">
                        <c:choose>
                            <c:when test="${isEdit}">
                                <i class="bi bi-pencil-square text-primary me-2"></i>Chỉnh sửa Yêu cầu tuyển dụng: <span class="text-primary font-monospace">${reqItem.code}</span>
                            </c:when>
                            <c:otherwise>
                                <i class="bi bi-file-earmark-plus-fill text-primary me-2"></i>Tạo mới Yêu cầu tuyển dụng
                            </c:otherwise>
                        </c:choose>
                    </h4>
                </div>
                <p class="text-muted small mb-0 ms-4 ps-2">
                    Điền đầy đủ thông tin vị trí, định biên, dải lương đề xuất và tiêu chuẩn tuyển dụng. Có thể lưu nháp để tiếp tục hoàn thiện sau.
                </p>
            </div>
            <div>
                <span class="badge ${reqItem.statusBadgeClass != null ? reqItem.statusBadgeClass : 'bg-secondary text-white'} px-3 py-2 fs-6">
                    Trạng thái: ${reqItem.statusLabel != null ? reqItem.statusLabel : 'Bản nháp mới'}
                </span>
            </div>
        </div>

        <form id="requisitionForm" method="post" action="${pageContext.request.contextPath}/recruitment-requests/save" novalidate>
            <input type="hidden" name="id" value="${reqItem.id}">
            <input type="hidden" name="code" value="${isEdit ? reqItem.code : nextCode}">

            <div class="row g-4">
                <!-- Cột trái: Thông tin vị trí & Ngân sách lương -->
                <div class="col-12 col-lg-7">
                    <!-- Khối 1: Khai báo Chức danh, Phòng ban, Định biên -->
                    <div class="card shadow-sm border mb-4">
                        <div class="card-header bg-white py-3 border-bottom">
                            <h6 class="fw-bold mb-0 text-dark">
                                <i class="bi bi-card-checklist text-primary me-2"></i>1. Thông tin Vị trí &amp; Định biên
                            </h6>
                        </div>
                        <div class="card-body">
                            <div class="row g-3">
                                <!-- Mã yêu cầu -->
                                <div class="col-md-4">
                                    <label class="form-label text-muted small fw-semibold">Mã yêu cầu</label>
                                    <input type="text" class="form-control form-control-sm bg-light font-monospace fw-bold" 
                                           value="${isEdit ? reqItem.code : nextCode}" readonly>
                                </div>

                                <!-- Tiêu đề yêu cầu -->
                                <div class="col-md-8">
                                    <label class="form-label text-muted small fw-semibold">Tiêu đề phiếu yêu cầu</label>
                                    <input type="text" name="title" id="titleInput" class="form-control form-control-sm" 
                                           placeholder="VD: Tuyển dụng Kỹ sư Java Backend Senior"
                                           value="${reqItem.title}">
                                </div>

                                <!-- Chức danh cần tuyển -->
                                <div class="col-md-7">
                                    <label class="form-label small fw-bold text-dark">
                                        Chức danh cần tuyển <span class="text-danger">*</span>
                                    </label>
                                    <div class="input-group input-group-sm">
                                        <input type="text" name="positionTitle" id="positionTitleInput" class="form-control" 
                                               list="standardPositionsList"
                                               placeholder="Chọn hoặc nhập chức danh..." 
                                               value="${reqItem.positionTitle}" required>
                                        <button class="btn btn-outline-secondary dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                                            Danh mục chuẩn
                                        </button>
                                        <ul class="dropdown-menu dropdown-menu-end shadow-sm" style="max-height: 250px; overflow-y: auto;">
                                            <c:forEach var="sr" items="${salaryRanges}">
                                                <li>
                                                    <a class="dropdown-item small py-2" href="javascript:void(0)" 
                                                       onclick="selectPosition('${sr.positionTitle}', '${sr.departmentId}', '${sr.minSalary}', '${sr.maxSalary}')">
                                                        <div class="fw-semibold">${sr.positionTitle}</div>
                                                        <div class="text-xs text-muted">${sr.departmentName} &bull; Chuẩn: ${sr.formattedRange}</div>
                                                    </a>
                                                </li>
                                            </c:forEach>
                                        </ul>
                                    </div>
                                    <datalist id="standardPositionsList">
                                        <c:forEach var="sr" items="${salaryRanges}">
                                            <option value="${sr.positionTitle}">${sr.departmentName} (Chuẩn: ${sr.formattedRange})</option>
                                        </c:forEach>
                                    </datalist>
                                    <div class="form-text text-xs">Gõ hoặc chọn chức danh chuẩn để đối chiếu dải ngân sách lương.</div>
                                </div>

                                <!-- Phòng ban -->
                                <div class="col-md-5">
                                    <label class="form-label small fw-bold text-dark">
                                        Phòng ban phụ trách <span class="text-danger">*</span>
                                    </label>
                                    <select name="departmentId" id="departmentSelect" class="form-select form-select-sm" required>
                                        <option value="">-- Chọn phòng ban --</option>
                                        <c:forEach var="d" items="${departments}">
                                            <option value="${d.id}" ${(reqItem.departmentId == d.id) ? 'selected' : ''}>${d.name}</option>
                                        </c:forEach>
                                    </select>
                                </div>

                                <!-- Số lượng tuyển -->
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-dark">
                                        Số lượng tuyển <span class="text-danger">*</span>
                                    </label>
                                    <div class="input-group input-group-sm">
                                        <input type="number" name="headcount" id="headcountInput" class="form-control text-center fw-bold" 
                                               min="1" max="100" value="${reqItem.headcount > 0 ? reqItem.headcount : 1}" required>
                                        <span class="input-group-text">Nhân sự</span>
                                    </div>
                                </div>

                                <!-- Lý do tuyển dụng -->
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-dark">
                                        Lý do tuyển <span class="text-danger">*</span>
                                    </label>
                                    <select name="recruitmentReason" class="form-select form-select-sm" required>
                                        <option value="NEW_HEADCOUNT" ${reqItem.recruitmentReason != 'REPLACEMENT' ? 'selected' : ''}>Tăng mới (Mở rộng quy mô)</option>
                                        <option value="REPLACEMENT" ${reqItem.recruitmentReason == 'REPLACEMENT' ? 'selected' : ''}>Thay thế (Nhân sự nghỉ việc)</option>
                                    </select>
                                </div>

                                <!-- Ngày cần người (Deadline) -->
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold text-dark">
                                        Ngày cần người <span class="text-danger">*</span>
                                    </label>
                                    <input type="date" name="deadline" id="deadlineInput" class="form-control form-control-sm" 
                                           min="${todayDate}" value="${reqItem.deadline}" required>
                                    <div class="form-text text-xs text-danger" id="deadlineHelp">
                                        <i class="bi bi-info-circle me-1"></i>Không được ở quá khứ
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Khối 2: Dải lương đề xuất & Kiểm soát dải chuẩn -->
                    <div class="card shadow-sm border mb-4">
                        <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                            <h6 class="fw-bold mb-0 text-dark">
                                <i class="bi bi-cash-stack text-success me-2"></i>2. Dải lương đề xuất &amp; Ngân sách
                            </h6>
                            <span class="badge bg-light text-secondary border" id="benchmarkBadge">
                                Chưa xác định dải chuẩn
                            </span>
                        </div>
                        <div class="card-body">
                            <!-- Hộp thông tin dải chuẩn của chức danh -->
                            <div class="p-3 mb-3 rounded border bg-light" id="standardRangeBox">
                                <div class="d-flex align-items-center justify-content-between">
                                    <div>
                                        <small class="text-muted text-uppercase fw-semibold" style="font-size: 11px;">Dải lương chuẩn của chức danh:</small>
                                        <div class="fw-bold fs-6 text-dark" id="standardRangeDisplay">
                                            (Chọn chức danh để nạp dải lương chuẩn theo chính sách)
                                        </div>
                                    </div>
                                    <i class="bi bi-shield-check text-success fs-3 opacity-75"></i>
                                </div>
                            </div>

                            <div class="row g-3">
                                <!-- Lương tối thiểu đề xuất -->
                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-dark">
                                        Lương tối thiểu đề xuất (VNĐ) <span class="text-danger">*</span>
                                    </label>
                                    <div class="input-group input-group-sm">
                                        <input type="number" name="minSalary" id="minSalaryInput" class="form-control fw-bold" 
                                               step="500000" placeholder="VD: 25000000" 
                                               value="${reqItem.minSalary != null ? reqItem.minSalary.toBigInteger() : ''}" required>
                                        <span class="input-group-text">VND</span>
                                    </div>
                                    <div class="text-xs text-muted mt-1" id="minSalaryFormatted"></div>
                                </div>

                                <!-- Lương tối đa đề xuất -->
                                <div class="col-md-6">
                                    <label class="form-label small fw-bold text-dark">
                                        Lương tối đa đề xuất (VNĐ) <span class="text-danger">*</span>
                                    </label>
                                    <div class="input-group input-group-sm">
                                        <input type="number" name="maxSalary" id="maxSalaryInput" class="form-control fw-bold" 
                                               step="500000" placeholder="VD: 45000000" 
                                               value="${reqItem.maxSalary != null ? reqItem.maxSalary.toBigInteger() : ''}" required>
                                        <span class="input-group-text">VND</span>
                                    </div>
                                    <div class="text-xs text-muted mt-1" id="maxSalaryFormatted"></div>
                                </div>
                            </div>

                            <!-- Alert Box cảnh báo vượt dải chuẩn -->
                            <div class="alert alert-warning border border-warning shadow-sm mt-3 mb-3 d-flex align-items-start gap-2" 
                                 id="salaryWarningBox" style="display: none;">
                                <i class="bi bi-exclamation-triangle-fill fs-5 text-warning flex-shrink-0 mt-1"></i>
                                <div>
                                    <strong class="d-block text-warning-emphasis">Cảnh báo: Dải lương đề xuất nằm ngoài dải chuẩn!</strong>
                                    <span class="small" id="salaryWarningText">
                                        Mức lương đề xuất không nằm trong khung chuẩn của chức danh. Theo quy định tuyển dụng, bạn <b>bắt buộc phải nhập văn bản giải trình lý do</b> bên dưới để trình Ban Giám đốc phê duyệt.
                                    </span>
                                </div>
                            </div>

                            <!-- Trường giải trình ngân sách -->
                            <div class="mt-3" id="explanationGroup">
                                <label class="form-label small fw-bold text-dark d-flex justify-content-between align-items-center">
                                    <span>
                                        Giải trình lý do vượt / nằm ngoài dải chuẩn 
                                        <span class="text-danger" id="explanationRequiredStar" style="display:none;">* (Bắt buộc)</span>
                                    </span>
                                    <span class="badge bg-secondary-subtle text-secondary" id="explanationStatusBadge">Tùy chọn</span>
                                </label>
                                <textarea name="salaryExplanation" id="salaryExplanationInput" class="form-control" rows="3" 
                                          placeholder="Nêu rõ lý do cần tuyển mức lương ngoài chuẩn (VD: Yêu cầu chuyên gia công nghệ hiếm, ứng viên có kinh nghiệm đặc thù, dự án trọng điểm cấp bách...)">${reqItem.salaryExplanation}</textarea>
                                <div class="form-text text-xs" id="explanationHelp">
                                    Bắt buộc điền nếu mức lương đề xuất thấp hơn mức sàn hoặc cao hơn mức trần của dải lương chuẩn.
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Cột phải: Soạn JD & Tiêu chuẩn ứng viên -->
                <div class="col-12 col-lg-5">
                    <!-- Khối 3: Soạn mô tả công việc (JD) -->
                    <div class="card shadow-sm border mb-4">
                        <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                            <h6 class="fw-bold mb-0 text-dark">
                                <i class="bi bi-file-text text-primary me-2"></i>3. Bản mô tả công việc (JD) <span class="text-danger">*</span>
                            </h6>
                            <button type="button" class="btn btn-xs btn-outline-primary py-0 px-2 text-xs" onclick="insertJdTemplate()">
                                <i class="bi bi-magic me-1"></i>Mẫu gợi ý
                            </button>
                        </div>
                        <div class="card-body">
                            <textarea name="jobDescription" id="jobDescriptionInput" class="form-control font-sans" rows="7" 
                                      placeholder="- Tham gia thiết kế và phát triển các hệ thống Backend...&#10;- Tối ưu hóa hiệu năng cơ sở dữ liệu và API...&#10;- Phối hợp cùng đội ngũ QA và Product Manager...">${reqItem.jobDescription}</textarea>
                            <div class="form-text text-xs">Mô tả rõ ràng trách nhiệm công việc, nhiệm vụ chính và mục tiêu cần đạt được.</div>
                        </div>
                    </div>

                    <!-- Khối 4: Soạn yêu cầu ứng viên -->
                    <div class="card shadow-sm border mb-4">
                        <div class="card-header bg-white py-3 border-bottom d-flex justify-content-between align-items-center">
                            <h6 class="fw-bold mb-0 text-dark">
                                <i class="bi bi-person-check text-success me-2"></i>4. Yêu cầu ứng viên <span class="text-danger">*</span>
                            </h6>
                            <button type="button" class="btn btn-xs btn-outline-success py-0 px-2 text-xs" onclick="insertReqTemplate()">
                                <i class="bi bi-magic me-1"></i>Mẫu gợi ý
                            </button>
                        </div>
                        <div class="card-body">
                            <textarea name="jobRequirements" id="jobRequirementsInput" class="form-control font-sans" rows="7" 
                                      placeholder="- Tối thiểu 3 năm kinh nghiệm lập trình Java Servlet/Spring...&#10;- Thành thạo MySQL, tối ưu câu lệnh truy vấn...&#10;- Kỹ năng làm việc nhóm, tư duy giải quyết vấn đề tốt...">${reqItem.jobRequirements}</textarea>
                            <div class="form-text text-xs">Liệt kê kinh nghiệm, bằng cấp, kỹ năng chuyên môn và phẩm chất cần thiết.</div>
                        </div>
                    </div>

                    <!-- Khối 5: Thao tác nộp / Lưu nháp -->
                    <div class="card shadow-sm border bg-light">
                        <div class="card-body p-3">
                            <h6 class="fw-bold text-dark mb-2">Thao tác xử lý yêu cầu</h6>
                            <p class="text-muted small mb-3">
                                Bạn có thể <b>Lưu nháp</b> để tiếp tục bổ sung thông tin sau mà không bị ràng buộc các trường chưa hoàn tất, hoặc <b>Gửi duyệt</b> để chuyển tiếp yêu cầu đến cấp quản lý.
                            </p>
                            <div class="d-grid gap-2">
                                <button type="submit" name="action" value="submit" class="btn btn-primary shadow-sm fw-semibold py-2" id="submitBtn">
                                    <i class="bi bi-send-check-fill me-1"></i> Gửi phê duyệt yêu cầu
                                </button>
                                <button type="submit" name="action" value="draft" class="btn btn-outline-secondary py-2" id="draftBtn">
                                    <i class="bi bi-save me-1"></i> Lưu bản nháp (Draft)
                                </button>
                                <a href="${pageContext.request.contextPath}/recruitment-requests" class="btn btn-link text-muted btn-sm text-decoration-none">
                                    Hủy bỏ và quay lại danh sách
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </form>
    </div>
</main>

<jsp:include page="../common/footer.jsp" />

<!-- Dữ liệu dải chuẩn phía Client để tính toán tức thời -->
<script>
var salaryRangesData = [
    <c:forEach var="s" items="${salaryRanges}" varStatus="status">
    {
        positionTitle: "${s.positionTitle}",
        departmentId: "${s.departmentId}",
        minSalary: ${s.minSalary != null ? s.minSalary : 0},
        maxSalary: ${s.maxSalary != null ? s.maxSalary : 0},
        currency: "${s.currency}",
        formatted: "${s.formattedRange}"
    }${!status.last ? ',' : ''}
    </c:forEach>
];

var todayString = "${todayDate}";

function formatCurrencyVN(val) {
    if (!val || isNaN(val)) return "";
    return new Intl.NumberFormat('vi-VN').format(val) + ' VND';
}

function selectPosition(pos, deptId, minSal, maxSal) {
    document.getElementById('positionTitleInput').value = pos;
    if (deptId && !document.getElementById('departmentSelect').value) {
        document.getElementById('departmentSelect').value = deptId;
    }
    if (!document.getElementById('titleInput').value) {
        document.getElementById('titleInput').value = 'Tuyển dụng ' + pos;
    }
    if (!document.getElementById('minSalaryInput').value && minSal) {
        document.getElementById('minSalaryInput').value = minSal;
    }
    if (!document.getElementById('maxSalaryInput').value && maxSal) {
        document.getElementById('maxSalaryInput').value = maxSal;
    }
    checkSalaryBenchmark();
}

function findStandardBenchmark(positionTitle, departmentId) {
    if (!positionTitle) return null;
    var normPos = positionTitle.trim().toLowerCase();
    
    // Tìm khớp cả dept và position
    if (departmentId) {
        for (var i = 0; i < salaryRangesData.length; i++) {
            var item = salaryRangesData[i];
            if (item.positionTitle.trim().toLowerCase() === normPos && item.departmentId === departmentId) {
                return item;
            }
        }
    }
    // Fallback chỉ theo position
    for (var j = 0; j < salaryRangesData.length; j++) {
        var s = salaryRangesData[j];
        if (s.positionTitle.trim().toLowerCase() === normPos) {
            return s;
        }
    }
    return null;
}

function checkSalaryBenchmark() {
    var pos = document.getElementById('positionTitleInput').value;
    var dept = document.getElementById('departmentSelect').value;
    var minVal = parseFloat(document.getElementById('minSalaryInput').value) || 0;
    var maxVal = parseFloat(document.getElementById('maxSalaryInput').value) || 0;

    // Format helper labels
    document.getElementById('minSalaryFormatted').textContent = minVal > 0 ? formatCurrencyVN(minVal) : '';
    document.getElementById('maxSalaryFormatted').textContent = maxVal > 0 ? formatCurrencyVN(maxVal) : '';

    var benchmark = findStandardBenchmark(pos, dept);
    var warningBox = document.getElementById('salaryWarningBox');
    var warningText = document.getElementById('salaryWarningText');
    var badge = document.getElementById('benchmarkBadge');
    var display = document.getElementById('standardRangeDisplay');
    var expGroup = document.getElementById('explanationGroup');
    var expStar = document.getElementById('explanationRequiredStar');
    var expBadge = document.getElementById('explanationStatusBadge');
    var expInput = document.getElementById('salaryExplanationInput');

    if (benchmark) {
        badge.className = 'badge bg-success-subtle text-success border border-success-subtle';
        badge.textContent = 'Dải chuẩn: ' + benchmark.formatted;
        display.innerHTML = '<span class="text-success fw-bold">' + benchmark.formatted + '</span> ' +
                            '<span class="badge bg-secondary-subtle text-secondary ms-2">Chính sách ban hành</span>';

        var outOfRange = false;
        var reasons = [];

        if (minVal > 0 && benchmark.minSalary > 0 && minVal < benchmark.minSalary) {
            outOfRange = true;
            reasons.push('Lương tối thiểu (' + formatCurrencyVN(minVal) + ') thấp hơn sàn chuẩn (' + formatCurrencyVN(benchmark.minSalary) + ')');
        }
        if (maxVal > 0 && benchmark.maxSalary > 0 && maxVal > benchmark.maxSalary) {
            outOfRange = true;
            reasons.push('Lương tối đa (' + formatCurrencyVN(maxVal) + ') vượt mức trần chuẩn (' + formatCurrencyVN(benchmark.maxSalary) + ')');
        }

        if (outOfRange) {
            warningBox.style.display = 'flex';
            warningText.innerHTML = 'Dải lương đề xuất nằm ngoài khung chuẩn (' + benchmark.formatted + '): <b>' + reasons.join('; ') + '</b>.<br>Bạn <b>bắt buộc phải nhập giải trình ngân sách</b> bên dưới.';
            
            expStar.style.display = 'inline';
            expBadge.className = 'badge bg-danger text-white';
            expBadge.textContent = 'Bắt buộc nhập giải trình';
            expInput.classList.add('border-warning');
        } else {
            warningBox.style.display = 'none';
            expStar.style.display = 'none';
            expBadge.className = 'badge bg-secondary-subtle text-secondary';
            expBadge.textContent = 'Trong dải chuẩn (Tùy chọn)';
            expInput.classList.remove('border-warning');
        }
    } else {
        badge.className = 'badge bg-light text-secondary border';
        badge.textContent = 'Chưa có dải chuẩn';
        display.innerHTML = '<span class="text-muted">Chức danh chưa thiết lập khung chuẩn trong danh mục ngân sách</span>';
        warningBox.style.display = 'none';
        expStar.style.display = 'none';
        expBadge.className = 'badge bg-secondary-subtle text-secondary';
        expBadge.textContent = 'Tùy chọn';
        expInput.classList.remove('border-warning');
    }
}

// Bắt sự kiện thay đổi
document.getElementById('positionTitleInput').addEventListener('input', checkSalaryBenchmark);
document.getElementById('departmentSelect').addEventListener('change', checkSalaryBenchmark);
document.getElementById('minSalaryInput').addEventListener('input', checkSalaryBenchmark);
document.getElementById('maxSalaryInput').addEventListener('input', checkSalaryBenchmark);

// Kiểm tra ngày cần người không được ở quá khứ
document.getElementById('deadlineInput').addEventListener('change', function() {
    var selected = this.value;
    if (selected && selected < todayString) {
        alert('Cảnh báo: Ngày cần người không được ở quá khứ! Vui lòng chọn ngày từ hôm nay trở đi.');
        this.value = todayString;
    }
});

// Kiểm tra form trước khi Submit hoặc Draft
document.getElementById('requisitionForm').addEventListener('submit', function(e) {
    var action = document.activeElement ? document.activeElement.getAttribute('value') : 'submit';
    var isDraft = (action === 'draft');

    var pos = document.getElementById('positionTitleInput').value.trim();
    if (!pos) {
        alert('Vui lòng nhập hoặc chọn chức danh cần tuyển!');
        document.getElementById('positionTitleInput').focus();
        e.preventDefault();
        return false;
    }

    var deadlineVal = document.getElementById('deadlineInput').value;
    if (deadlineVal && deadlineVal < todayString) {
        alert('Lỗi: Ngày cần người không được ở quá khứ! Vui lòng chọn từ ngày ' + todayString + ' trở đi.');
        document.getElementById('deadlineInput').focus();
        e.preventDefault();
        return false;
    }

    var minVal = parseFloat(document.getElementById('minSalaryInput').value) || 0;
    var maxVal = parseFloat(document.getElementById('maxSalaryInput').value) || 0;
    if (minVal > 0 && maxVal > 0 && minVal > maxVal) {
        alert('Lỗi: Lương tối thiểu đề xuất không được lớn hơn lương tối đa!');
        document.getElementById('minSalaryInput').focus();
        e.preventDefault();
        return false;
    }

    // Nếu Gửi duyệt (không phải lưu nháp), kiểm tra nghiêm ngặt
    if (!isDraft) {
        var dept = document.getElementById('departmentSelect').value;
        if (!dept) {
            alert('Vui lòng chọn phòng ban phụ trách!');
            document.getElementById('departmentSelect').focus();
            e.preventDefault();
            return false;
        }

        if (!deadlineVal) {
            alert('Vui lòng chọn ngày cần người!');
            document.getElementById('deadlineInput').focus();
            e.preventDefault();
            return false;
        }

        var jd = document.getElementById('jobDescriptionInput').value.trim();
        if (!jd) {
            alert('Vui lòng soạn bản mô tả công việc (Job Description)!');
            document.getElementById('jobDescriptionInput').focus();
            e.preventDefault();
            return false;
        }

        var req = document.getElementById('jobRequirementsInput').value.trim();
        if (!req) {
            alert('Vui lòng soạn yêu cầu ứng viên (Candidate Requirements)!');
            document.getElementById('jobRequirementsInput').focus();
            e.preventDefault();
            return false;
        }

        // TIÊU CHÍ QUAN TRỌNG: Dải lương nằm ngoài dải chuẩn thì BẮT BUỘC nhập giải trình
        var benchmark = findStandardBenchmark(pos, dept);
        if (benchmark) {
            var outOfRange = false;
            if (minVal > 0 && benchmark.minSalary > 0 && minVal < benchmark.minSalary) outOfRange = true;
            if (maxVal > 0 && benchmark.maxSalary > 0 && maxVal > benchmark.maxSalary) outOfRange = true;

            if (outOfRange) {
                var expText = document.getElementById('salaryExplanationInput').value.trim();
                if (!expText) {
                    alert('YÊU CẦU BẮT BUỘC:\nDải lương đề xuất nằm ngoài khung dải chuẩn của chức danh (' + benchmark.formatted + ').\nBạn bắt buộc phải nhập nội dung giải trình ngân sách!');
                    document.getElementById('salaryExplanationInput').focus();
                    document.getElementById('salaryExplanationInput').classList.add('is-invalid');
                    e.preventDefault();
                    return false;
                }
            }
        }
    }
});

function insertJdTemplate() {
    var title = document.getElementById('positionTitleInput').value.trim() || 'nhân sự';
    var template = "- Chịu trách nhiệm thực hiện các nhiệm vụ chuyên môn liên quan đến vị trí " + title + ".\n" +
                   "- Phối hợp cùng các thành viên trong bộ phận để triển khai kế hoạch công việc theo quý/tháng.\n" +
                   "- Tham gia đánh giá, tối ưu quy trình và báo cáo tiến độ trực tiếp cho Quản lý bộ phận.\n" +
                   "- Thực hiện các nhiệm vụ chuyên trách khác theo yêu cầu của cấp trên.";
    document.getElementById('jobDescriptionInput').value = template;
}

function insertReqTemplate() {
    var title = document.getElementById('positionTitleInput').value.trim() || 'vị trí';
    var template = "- Tốt nghiệp Đại học/Cao đẳng chuyên ngành liên quan đến " + title + ".\n" +
                   "- Tối thiểu từ 2 - 3 năm kinh nghiệm làm việc thực tế ở vị trí tương đương.\n" +
                   "- Kỹ năng giao tiếp, làm việc nhóm và chủ động giải quyết vấn đề hiệu quả.\n" +
                   "- Tinh thần trách nhiệm cao, chịu được áp lực tiến độ công việc.";
    document.getElementById('jobRequirementsInput').value = template;
}

// Chạy kiểm tra khởi tạo khi tải trang
window.addEventListener('DOMContentLoaded', checkSalaryBenchmark);
</script>

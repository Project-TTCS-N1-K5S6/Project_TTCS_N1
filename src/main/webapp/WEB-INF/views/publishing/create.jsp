<%--
  ==============================================================================
  BIỂU MẪU TẠO NHANH BÀI ĐĂNG TUYỂN DỤNG (publishing/create.jsp)
  Chức năng:
  - Cho phép chọn một Yêu cầu tuyển dụng đã được phê duyệt (APPROVED) để kế thừa toàn bộ nội dung (JD, Yêu cầu, Đãi ngộ, Dải lương, Hạn nộp).
  - Tùy chỉnh kênh đăng (Website nội bộ, LinkedIn, Facebook, VietnamWorks...), thiết lập hiển thị lương (công khai hoặc "Thỏa thuận").
  - Chức năng Xem trước bài đăng (Live Preview) mô phỏng chính xác giao diện ứng viên nhìn thấy trước khi xuất bản chính thức.
  ==============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Tạo nhanh Bài đăng Tuyển dụng" />
<c:set var="breadcrumb" value="Quản lý Xuất bản" />
<c:set var="activeMenu" value="post-publishing" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Tiêu đề trang & Điều hướng -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <div class="d-flex align-items-center gap-2 mb-1">
                    <a href="${pageContext.request.contextPath}/post-publishing" class="btn btn-sm btn-outline-secondary py-1 px-2" title="Quay lại danh sách">
                        <i class="bi bi-arrow-left"></i>
                    </a>
                    <h4 class="fw-bold mb-0">
                        <i class="bi bi-lightning-charge-fill text-warning me-2"></i>Tạo nhanh Bài đăng Tuyển dụng
                    </h4>
                    <span class="badge bg-success-subtle text-success border border-success-subtle px-2.5 py-1">
                        <i class="bi bi-shield-check me-1"></i>Từ yêu cầu đã duyệt
                    </span>
                </div>
                <p class="text-muted small mb-0 ms-4 ps-2">
                    Kế thừa thông tin từ yêu cầu tuyển dụng đã được phê duyệt, chuẩn hóa nội dung mô tả công việc và tùy chỉnh thông số đăng tuyển độc lập.
                </p>
            </div>
            <div>
                <a href="${pageContext.request.contextPath}/post-publishing" class="btn btn-outline-secondary btn-sm px-3">
                    <i class="bi bi-x-circle me-1"></i> Hủy thao tác
                </a>
            </div>
        </div>

        <!-- Hướng dẫn nghiệp vụ KN-204 & KN-208 -->
        <div class="alert alert-primary border-primary-subtle bg-primary-subtle text-primary-emphasis shadow-sm mb-4">
            <div class="d-flex align-items-start gap-2">
                <i class="bi bi-info-circle-fill fs-5 mt-0.5"></i>
                <div class="small">
                    <strong>Quy tắc nghiệp vụ:</strong>
                    Chỉ những yêu cầu tuyển dụng ở trạng thái <span class="badge bg-success text-white">Đã duyệt (APPROVED / OPEN)</span> mới được phép tạo bài đăng.
                    Nội dung mô tả công việc (JD) được kế thừa và chuẩn hóa tự động; mọi chỉnh sửa tại đây là độc lập và <strong>không làm thay đổi</strong> yêu cầu tuyển dụng gốc.
                </div>
            </div>
        </div>

        <form id="quickPostForm" method="POST" action="${pageContext.request.contextPath}/post-publishing/quick-create">
            <div class="row g-4">
                <!-- Cột trái: Thông tin chính -->
                <div class="col-12 col-lg-8">
                    <!-- 1. CHỌN YÊU CẦU NGUỒN -->
                    <div class="card border-0 shadow-sm mb-4">
                        <div class="card-header bg-white py-3 border-bottom">
                            <h6 class="fw-bold text-dark mb-0 d-flex align-items-center gap-2">
                                <span class="badge bg-primary rounded-circle p-1.5" style="width: 24px; height: 24px; display: inline-flex; align-items: center; justify-content: center;">1</span>
                                Yêu cầu tuyển dụng nguồn (Đã phê duyệt) <span class="text-danger">*</span>
                            </h6>
                        </div>
                        <div class="card-body p-4">
                            <div class="mb-3">
                                <label for="requisitionSelect" class="form-label fw-semibold text-dark text-xs text-uppercase">
                                    Chọn yêu cầu tuyển dụng đã duyệt:
                                </label>
                                <select class="form-select form-select-lg" id="requisitionSelect" name="requisitionId" required onchange="onRequisitionChanged(this.value)">
                                    <option value="">-- Chọn yêu cầu tuyển dụng đã được duyệt --</option>
                                    <c:forEach var="req" items="${approvedRequisitions}">
                                        <option value="${req.id}" ${selectedReq != null && selectedReq.id == req.id ? 'selected' : ''}>
                                            [${req.code}] ${req.positionTitle} - ${req.departmentName} (${req.statusLabel})
                                        </option>
                                    </c:forEach>
                                </select>
                                <div class="form-text small text-muted">
                                    Danh sách chỉ hiển thị các yêu cầu tuyển dụng đã được phê duyệt hợp lệ trong hệ thống.
                                </div>
                            </div>

                            <!-- Panel tóm tắt yêu cầu nguồn khi đã chọn -->
                            <div id="requisitionSummaryCard" class="p-3 bg-light rounded border ${selectedReq != null ? '' : 'd-none'}">
                                <div class="d-flex align-items-center justify-content-between mb-2">
                                    <span class="badge bg-success-subtle text-success border border-success-subtle">
                                        <i class="bi bi-check-circle-fill me-1"></i><span id="sumStatus">${selectedReq != null ? selectedReq.statusLabel : 'Đã duyệt'}</span>
                                    </span>
                                    <span class="text-xs text-muted font-monospace" id="sumCode">${selectedReq != null ? selectedReq.code : ''}</span>
                                </div>
                                <h6 class="fw-bold text-dark mb-2" id="sumPositionTitle">${selectedReq != null ? selectedReq.positionTitle : ''}</h6>
                                <div class="row g-2 text-xs text-secondary">
                                    <div class="col-sm-6">
                                        <i class="bi bi-building me-1 text-primary"></i>Phòng ban: <strong class="text-dark" id="sumDept">${selectedReq != null ? selectedReq.departmentName : ''}</strong>
                                    </div>
                                    <div class="col-sm-6">
                                        <i class="bi bi-people me-1 text-primary"></i>Chỉ tiêu tuyển: <strong class="text-dark" id="sumHeadcount">${selectedReq != null ? selectedReq.headcount : ''} người</strong>
                                    </div>
                                    <div class="col-sm-6">
                                        <i class="bi bi-cash me-1 text-primary"></i>Dải lương gốc: <strong class="text-dark" id="sumSalary">${selectedReq != null ? selectedReq.formattedSalaryRange : 'Thỏa thuận'}</strong>
                                    </div>
                                    <div class="col-sm-6">
                                        <i class="bi bi-calendar-event me-1 text-primary"></i>Hạn nhận gốc: <strong class="text-dark" id="sumDeadline">${selectedReq != null ? selectedReq.formattedDeadline : 'Chưa đặt'}</strong>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 2. NỘI DUNG BÀI ĐĂNG TUYỂN DỤNG -->
                    <div class="card border-0 shadow-sm mb-4">
                        <div class="card-header bg-white py-3 border-bottom d-flex align-items-center justify-content-between">
                            <h6 class="fw-bold text-dark mb-0 d-flex align-items-center gap-2">
                                <span class="badge bg-primary rounded-circle p-1.5" style="width: 24px; height: 24px; display: inline-flex; align-items: center; justify-content: center;">2</span>
                                Nội dung bài đăng tuyển dụng <span class="text-danger">*</span>
                            </h6>
                            <button type="button" class="btn btn-outline-primary btn-sm py-0.5 px-2" onclick="reNormalizeDescription()" title="Chuẩn hóa lại văn bản">
                                <i class="bi bi-magic me-1"></i>Chuẩn hóa lại JD
                            </button>
                        </div>
                        <div class="card-body p-4">
                            <!-- Tiêu đề bài đăng -->
                            <div class="mb-4">
                                <label for="postTitle" class="form-label fw-bold text-dark">
                                    Tiêu đề bài đăng tuyển dụng <span class="text-danger">*</span>
                                </label>
                                <input type="text" class="form-control form-control-lg fw-semibold" id="postTitle" name="title" 
                                       value="${draftPosting.title != null ? draftPosting.title : (selectedReq != null ? selectedReq.title : '')}" 
                                       placeholder="Ví dụ: Chuyên viên Phát triển Phần mềm Java (Senior)..." required />
                                <div class="form-text small text-muted">
                                    Tiêu đề rõ ràng, thu hút ứng viên và phản ánh chính xác vị trí tuyển dụng.
                                </div>
                            </div>

                            <!-- Mô tả công việc kế thừa và chuẩn hóa (KN-206) -->
                            <div class="mb-2">
                                <div class="d-flex align-items-center justify-content-between mb-1">
                                    <label for="postDescription" class="form-label fw-bold text-dark mb-0">
                                        Mô tả công việc &amp; Yêu cầu (JD) <span class="text-danger">*</span>
                                    </label>
                                    <span class="text-xs text-muted">
                                        <i class="bi bi-info-circle me-1"></i>Đã chuẩn hóa gạch đầu dòng &amp; khoảng trắng
                                    </span>
                                </div>
                                <textarea class="form-control font-monospace" id="postDescription" name="jobDescription" rows="12" 
                                          style="font-size: 0.9rem; line-height: 1.6;" required
                                          placeholder="Nội dung mô tả công việc sẽ tự động kế thừa từ yêu cầu tuyển dụng...">${draftPosting.jobDescription != null ? draftPosting.jobDescription : ''}</textarea>
                                <div class="d-flex justify-content-between align-items-center mt-1">
                                    <small class="text-muted">
                                        Nội dung chỉnh sửa tại đây là độc lập, không làm thay đổi dữ liệu yêu cầu tuyển dụng gốc.
                                    </small>
                                    <small id="jdCharCount" class="text-muted">0 ký tự</small>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Cột phải: Thiết lập bổ sung (Địa điểm, Hình thức, Hạn nhận, Lương) -->
                <div class="col-12 col-lg-4">
                    <!-- 3. ĐIỀU KIỆN LÀM VIỆC & THỜI HẠN -->
                    <div class="card border-0 shadow-sm mb-4">
                        <div class="card-header bg-white py-3 border-bottom">
                            <h6 class="fw-bold text-dark mb-0 d-flex align-items-center gap-2">
                                <span class="badge bg-primary rounded-circle p-1.5" style="width: 24px; height: 24px; display: inline-flex; align-items: center; justify-content: center;">3</span>
                                Thông số làm việc &amp; Thời hạn
                            </h6>
                        </div>
                        <div class="card-body p-4">
                            <!-- Địa điểm làm việc -->
                            <div class="mb-3">
                                <label for="workLocation" class="form-label fw-bold text-dark text-xs text-uppercase">
                                    Địa điểm làm việc <span class="text-danger">*</span>
                                </label>
                                <div class="input-group">
                                    <span class="input-group-text bg-light"><i class="bi bi-geo-alt"></i></span>
                                    <select class="form-select" id="workLocationSelect" onchange="onLocationSelectChanged(this.value)">
                                        <option value="">-- Chọn danh mục địa điểm --</option>
                                        <c:forEach var="loc" items="${workLocations}">
                                            <option value="${loc.value}">${loc.value}</option>
                                        </c:forEach>
                                        <option value="custom">-- Nhập địa điểm khác --</option>
                                    </select>
                                </div>
                                <input type="text" class="form-control mt-2" id="workLocationInput" name="workLocation" 
                                       value="${draftPosting.workLocation != null ? draftPosting.workLocation : (selectedReq != null ? selectedReq.workLocation : 'Hà Nội, Việt Nam')}" 
                                       placeholder="Ví dụ: Tầng 5, Tòa nhà Văn phòng, Hà Nội" required />
                                <div class="form-text small text-muted">Khai báo địa chỉ hoặc chi nhánh làm việc cụ thể.</div>
                            </div>

                            <!-- Hình thức làm việc -->
                            <div class="mb-3">
                                <label for="workMode" class="form-label fw-bold text-dark text-xs text-uppercase">
                                    Hình thức làm việc <span class="text-danger">*</span>
                                </label>
                                <div class="input-group">
                                    <span class="input-group-text bg-light"><i class="bi bi-briefcase"></i></span>
                                    <select class="form-select" id="workModeSelect" onchange="onModeSelectChanged(this.value)">
                                        <option value="">-- Chọn danh mục hình thức --</option>
                                        <c:forEach var="mode" items="${workModes}">
                                            <option value="${mode.value}">${mode.value}</option>
                                        </c:forEach>
                                        <option value="custom">-- Nhập hình thức khác --</option>
                                    </select>
                                </div>
                                <input type="text" class="form-control mt-2" id="workModeInput" name="workMode" 
                                       value="${draftPosting.workMode != null ? draftPosting.workMode : (selectedReq != null ? selectedReq.workMode : 'Toàn thời gian')} " 
                                       placeholder="Ví dụ: Toàn thời gian, Hybrid, Remote..." required />
                            </div>

                            <!-- Hạn nhận hồ sơ (KN-205: hợp lệ, không được trong quá khứ) -->
                            <div class="mb-3">
                                <label for="applicationDeadline" class="form-label fw-bold text-dark text-xs text-uppercase">
                                    Hạn nhận hồ sơ <span class="text-danger">*</span>
                                </label>
                                <div class="input-group">
                                    <span class="input-group-text bg-light"><i class="bi bi-calendar-event"></i></span>
                                    <input type="date" class="form-control" id="applicationDeadline" name="applicationDeadline" 
                                           value="${draftPosting.applicationDeadline != null ? draftPosting.applicationDeadline : (selectedReq != null ? selectedReq.deadline : '')}" required />
                                </div>
                                <div class="form-text small text-muted">Hạn nhận hồ sơ phải từ ngày hôm nay trở đi.</div>
                            </div>
                        </div>
                    </div>

                    <!-- 4. CẤU HÌNH HIỂN THỊ MỨC LƯƠNG (KN-204, KN-205) -->
                    <div class="card border-0 shadow-sm mb-4">
                        <div class="card-header bg-white py-3 border-bottom">
                            <h6 class="fw-bold text-dark mb-0 d-flex align-items-center gap-2">
                                <span class="badge bg-primary rounded-circle p-1.5" style="width: 24px; height: 24px; display: inline-flex; align-items: center; justify-content: center;">4</span>
                                Cấu hình Mức lương
                            </h6>
                        </div>
                        <div class="card-body p-4">
                            <!-- Toggle lựa chọn hiển thị mức lương -->
                            <div class="form-check form-switch mb-3">
                                <input class="form-check-input" type="checkbox" role="switch" id="salaryVisibleSwitch" name="salaryVisible" value="true"
                                       ${draftPosting.salaryVisible != null && draftPosting.salaryVisible ? 'checked' : (selectedReq != null && (selectedReq.minSalary != null || selectedReq.maxSalary != null) ? 'checked' : '')} 
                                       onchange="toggleSalaryVisibility(this.checked)">
                                <label class="form-check-label fw-semibold text-dark" for="salaryVisibleSwitch">
                                    Hiển thị mức lương trên bài đăng
                                </label>
                            </div>

                            <!-- Khoảng lương (chỉ yêu cầu khi bật hiển thị) -->
                            <div id="salaryRangeSection" class="${(draftPosting.salaryVisible != null && draftPosting.salaryVisible) || (selectedReq != null && (selectedReq.minSalary != null || selectedReq.maxSalary != null)) ? '' : 'd-none'}">
                                <div class="p-3 bg-light rounded border mb-2">
                                    <div class="mb-3">
                                        <label for="minSalary" class="form-label text-xs fw-bold text-muted text-uppercase">
                                            Lương tối thiểu (VND):
                                        </label>
                                        <input type="number" step="100000" min="0" class="form-control" id="minSalary" name="minSalary" 
                                               value="${draftPosting.minSalary != null ? draftPosting.minSalary.toBigInteger() : (selectedReq != null && selectedReq.minSalary != null ? selectedReq.minSalary.toBigInteger() : '')}" 
                                               placeholder="0" />
                                    </div>
                                    <div class="mb-3">
                                        <label for="maxSalary" class="form-label text-xs fw-bold text-muted text-uppercase">
                                            Lương tối đa (VND):
                                        </label>
                                        <input type="number" step="100000" min="0" class="form-control" id="maxSalary" name="maxSalary" 
                                               value="${draftPosting.maxSalary != null ? draftPosting.maxSalary.toBigInteger() : (selectedReq != null && selectedReq.maxSalary != null ? selectedReq.maxSalary.toBigInteger() : '')}" 
                                               placeholder="Không giới hạn" />
                                    </div>
                                    <div class="mb-0">
                                        <label for="currency" class="form-label text-xs fw-bold text-muted text-uppercase">Đơn vị tiền tệ:</label>
                                        <select class="form-select form-select-sm" id="currency" name="currency">
                                            <option value="VND" selected>VND (Việt Nam Đồng)</option>
                                            <option value="USD">USD (Đô la Mỹ)</option>
                                        </select>
                                    </div>
                                </div>
                                <div class="form-text small text-muted">
                                    Mức lương tối thiểu không được lớn hơn mức lương tối đa.
                                </div>
                            </div>

                            <div id="salaryHiddenNotice" class="alert alert-secondary py-2 px-3 small mb-0 ${(draftPosting.salaryVisible != null && draftPosting.salaryVisible) || (selectedReq != null && (selectedReq.minSalary != null || selectedReq.maxSalary != null)) ? 'd-none' : ''}">
                                <i class="bi bi-shield-lock me-1"></i>Mức lương sẽ được ẩn trên bài đăng và hiển thị <strong>"Thỏa thuận"</strong> đối với ứng viên.
                            </div>
                        </div>
                    </div>

                    <!-- 5. TÙY CHỌN XUẤT BẢN NGAY -->
                    <div class="card border-0 shadow-sm mb-4">
                        <div class="card-body p-4">
                            <div class="form-check form-switch">
                                <input class="form-check-input" type="checkbox" role="switch" id="publishImmediately" name="publishImmediately" value="true">
                                <label class="form-check-label fw-bold text-dark" for="publishImmediately">
                                    Xuất bản công khai ngay sau khi tạo
                                </label>
                            </div>
                            <small class="text-muted d-block mt-1">
                                Nếu không chọn, bài đăng sẽ được lưu ở trạng thái <strong>Bản nháp (DRAFT)</strong> để xem trước và xuất bản sau.
                            </small>
                        </div>
                    </div>

                    <!-- NÚT THAO TÁC HOÀN TẤT -->
                    <div class="card border-0 shadow-sm">
                        <div class="card-body p-3 d-flex flex-column gap-2">
                            <button type="submit" class="btn btn-success py-2.5 fw-bold shadow-sm d-flex align-items-center justify-content-center gap-2" id="submitBtn">
                                <i class="bi bi-check2-circle fs-5"></i>
                                <span>Tạo bài đăng tuyển dụng</span>
                            </button>
                            <a href="${pageContext.request.contextPath}/post-publishing" class="btn btn-outline-secondary py-2 text-center">
                                Quay lại danh sách
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </form>
    </div>
</main>

<jsp:include page="../common/footer.jsp" />

<script>
    var rawJobDescription = '';

    // Cập nhật số ký tự JD
    function updateCharCount() {
        var el = document.getElementById('postDescription');
        var cnt = document.getElementById('jdCharCount');
        if (el && cnt) {
            cnt.textContent = el.value.length + ' ký tự';
        }
    }

    document.getElementById('postDescription').addEventListener('input', updateCharCount);
    updateCharCount();

    // Toggle hiển thị khoảng lương
    function toggleSalaryVisibility(visible) {
        var sec = document.getElementById('salaryRangeSection');
        var notice = document.getElementById('salaryHiddenNotice');
        var minInput = document.getElementById('minSalary');
        var maxInput = document.getElementById('maxSalary');

        if (visible) {
            sec.classList.remove('d-none');
            notice.classList.add('d-none');
        } else {
            sec.classList.add('d-none');
            notice.classList.remove('d-none');
            minInput.value = '';
            maxInput.value = '';
        }
    }

    // Chọn địa điểm từ danh mục
    function onLocationSelectChanged(val) {
        var input = document.getElementById('workLocationInput');
        if (val && val !== 'custom') {
            input.value = val;
        } else if (val === 'custom') {
            input.focus();
        }
    }

    // Chọn hình thức làm việc từ danh mục
    function onModeSelectChanged(val) {
        var input = document.getElementById('workModeInput');
        if (val && val !== 'custom') {
            input.value = val;
        } else if (val === 'custom') {
            input.focus();
        }
    }

    // Chuẩn hóa lại mô tả bằng JS nếu người dùng chỉnh sửa
    function reNormalizeDescription() {
        var el = document.getElementById('postDescription');
        if (!el || !el.value) return;

        var text = el.value;
        // Chuẩn hóa các ký tự bullet point
        text = text.replace(/^[\s]*[•\*\+\–\—]\s+/gm, '- ');
        // Chuẩn hóa các dòng trống liên tiếp
        text = text.replace(/\n{3,}/g, '\n\n');
        el.value = text.trim();
        updateCharCount();
    }

    // Khi thay đổi yêu cầu tuyển dụng nguồn
    function onRequisitionChanged(reqId) {
        var summaryCard = document.getElementById('requisitionSummaryCard');
        if (!reqId) {
            summaryCard.classList.add('d-none');
            return;
        }

        fetch('${pageContext.request.contextPath}/post-publishing/requisition-data?id=' + encodeURIComponent(reqId))
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data.error) {
                    alert(data.error);
                    return;
                }

                summaryCard.classList.remove('d-none');
                document.getElementById('sumStatus').textContent = data.statusLabel || 'Đã duyệt';
                document.getElementById('sumCode').textContent = data.code || '';
                document.getElementById('sumPositionTitle').textContent = data.positionTitle || data.title || '';
                document.getElementById('sumDept').textContent = data.departmentName || '';
                document.getElementById('sumHeadcount').textContent = (data.headcount || 1) + ' người';
                document.getElementById('sumSalary').textContent = data.formattedSalaryRange || 'Thỏa thuận';
                document.getElementById('sumDeadline').textContent = data.deadlineFormatted || 'Chưa đặt';

                // Tự động điền tiêu đề
                var titleEl = document.getElementById('postTitle');
                if (!titleEl.value || titleEl.value.trim() === '') {
                    titleEl.value = data.title || data.positionTitle || '';
                }

                // Tự động kế thừa mô tả công việc đã chuẩn hóa (KN-206)
                var descEl = document.getElementById('postDescription');
                rawJobDescription = data.rawJobDescription || '';
                descEl.value = data.normalizedJobDescription || data.rawJobDescription || '';
                updateCharCount();

                // Hạn nhận hồ sơ
                if (data.deadline) {
                    document.getElementById('applicationDeadline').value = data.deadline;
                }

                // Địa điểm & hình thức
                if (data.workLocation) {
                    document.getElementById('workLocationInput').value = data.workLocation;
                }
                if (data.workMode) {
                    document.getElementById('workModeInput').value = data.workMode;
                }

                // Lương
                var salarySwitch = document.getElementById('salaryVisibleSwitch');
                if (data.salaryVisible && (data.minSalary || data.maxSalary)) {
                    salarySwitch.checked = true;
                    toggleSalaryVisibility(true);
                    if (data.minSalary) document.getElementById('minSalary').value = data.minSalary;
                    if (data.maxSalary) document.getElementById('maxSalary').value = data.maxSalary;
                } else {
                    salarySwitch.checked = false;
                    toggleSalaryVisibility(false);
                }
            })
            .catch(function(err) {
                console.error(err);
                if (window.showToast) {
                    window.showToast('Không thể tải thông tin yêu cầu tuyển dụng: ' + err, 'error', 'Lỗi tải dữ liệu');
                } else {
                    alert('Không thể tải thông tin yêu cầu tuyển dụng!');
                }
            });
    }

    // Client-side validation trước khi submit
    document.getElementById('quickPostForm').addEventListener('submit', function(e) {
        var reqSelect = document.getElementById('requisitionSelect');
        if (!reqSelect.value) {
            e.preventDefault();
            var msg = 'Vui lòng chọn yêu cầu tuyển dụng nguồn đã được phê duyệt!';
            if (window.showToast) window.showToast(msg, 'warning', 'Chưa chọn yêu cầu');
            else alert(msg);
            reqSelect.focus();
            return;
        }

        var title = document.getElementById('postTitle').value.trim();
        if (!title) {
            e.preventDefault();
            var msg = 'Vui lòng nhập tiêu đề bài đăng tuyển dụng!';
            if (window.showToast) window.showToast(msg, 'warning', 'Thiếu tiêu đề');
            else alert(msg);
            document.getElementById('postTitle').focus();
            return;
        }

        var jd = document.getElementById('postDescription').value.trim();
        if (!jd) {
            e.preventDefault();
            var msg = 'Vui lòng nhập nội dung mô tả công việc (JD)!';
            if (window.showToast) window.showToast(msg, 'warning', 'Thiếu mô tả');
            else alert(msg);
            document.getElementById('postDescription').focus();
            return;
        }

        var deadline = document.getElementById('applicationDeadline').value;
        if (deadline) {
            var selectedDate = new Date(deadline);
            var today = new Date();
            today.setHours(0, 0, 0, 0);
            if (selectedDate < today) {
                e.preventDefault();
                var msg = 'Hạn nhận hồ sơ không thể ở trong quá khứ!';
                if (window.showToast) window.showToast(msg, 'warning', 'Hạn nhận không hợp lệ');
                else alert(msg);
                document.getElementById('applicationDeadline').focus();
                return;
            }
        }

        var isSalaryVisible = document.getElementById('salaryVisibleSwitch').checked;
        if (isSalaryVisible) {
            var minSal = parseFloat(document.getElementById('minSalary').value);
            var maxSal = parseFloat(document.getElementById('maxSalary').value);

            if (!isNaN(minSal) && minSal < 0) {
                e.preventDefault();
                var msg = 'Mức lương tối thiểu không thể là số âm!';
                if (window.showToast) window.showToast(msg, 'warning', 'Lương không hợp lệ');
                else alert(msg);
                document.getElementById('minSalary').focus();
                return;
            }
            if (!isNaN(maxSal) && maxSal < 0) {
                e.preventDefault();
                var msg = 'Mức lương tối đa không thể là số âm!';
                if (window.showToast) window.showToast(msg, 'warning', 'Lương không hợp lệ');
                else alert(msg);
                document.getElementById('maxSalary').focus();
                return;
            }
            if (!isNaN(minSal) && !isNaN(maxSal) && minSal > maxSal) {
                e.preventDefault();
                var msg = 'Mức lương tối thiểu không được lớn hơn mức lương tối đa!';
                if (window.showToast) window.showToast(msg, 'warning', 'Dải lương không hợp lệ');
                else alert(msg);
                document.getElementById('minSalary').focus();
                return;
            }
        }

        // Chống double click (debouncing)
        var submitBtn = document.getElementById('submitBtn');
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Đang tạo bài đăng...';
    });

    // Thiết lập min date cho hạn nhận hồ sơ
    (function setMinDeadline() {
        var today = new Date().toISOString().split('T')[0];
        document.getElementById('applicationDeadline').setAttribute('min', today);
    })();
</script>

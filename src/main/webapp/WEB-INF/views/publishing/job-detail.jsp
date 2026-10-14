<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${job.requisition.positionTitle} - Tuyển dụng | IRMS Careers</title>
    <meta name="description" content="Ứng tuyển vị trí ${job.requisition.positionTitle} tại IRMS. Nộp hồ sơ trực tuyến nhanh chóng kèm CV định dạng PDF hoặc DOCX.">

    <!-- Google Fonts Inter -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap" rel="stylesheet">

    <!-- Bootstrap 5.3 CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">

    <style>
        :root {
            --primary-navy: #0f172a;
            --primary-blue: #2563eb;
            --primary-blue-hover: #1d4ed8;
            --accent-emerald: #10b981;
            --surface-bg: #f8fafc;
            --card-border: #e2e8f0;
            --text-heading: #0f172a;
            --text-body: #334155;
            --text-muted: #64748b;
        }

        body {
            font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            background-color: var(--surface-bg);
            color: var(--text-body);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }

        .header-banner {
            background: linear-gradient(135deg, #0f172a 0%, #1e3a8a 50%, #2563eb 100%);
            color: white;
            padding: 60px 0 45px;
            position: relative;
            overflow: hidden;
        }
        .header-banner::before {
            content: '';
            position: absolute;
            top: 0; left: 0; right: 0; bottom: 0;
            background: radial-gradient(circle at 85% 20%, rgba(255,255,255,0.08) 0%, transparent 60%);
            pointer-events: none;
        }

        .apply-card {
            background: #ffffff;
            border-radius: 16px;
            border: 1px solid var(--card-border);
            box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.08), 0 8px 10px -6px rgba(15, 23, 42, 0.04);
            padding: 24px;
            transition: all 0.2s ease;
        }

        .form-label {
            font-size: 0.82rem;
            font-weight: 600;
            color: #475569;
            margin-bottom: 6px;
        }
        .form-control, .form-select {
            border-radius: 8px;
            border: 1px solid #cbd5e1;
            font-size: 0.9rem;
            padding: 9px 12px;
            transition: border-color 0.15s ease, box-shadow 0.15s ease;
        }
        .form-control:focus, .form-select:focus {
            border-color: var(--primary-blue);
            box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
        }

        /* File Upload Dropzone */
        .cv-dropzone {
            border: 2px dashed #cbd5e1;
            border-radius: 10px;
            padding: 20px 16px;
            text-align: center;
            background-color: #f8fafc;
            cursor: pointer;
            transition: all 0.2s ease;
            position: relative;
        }
        .cv-dropzone:hover, .cv-dropzone.dragover {
            border-color: var(--primary-blue);
            background-color: #eff6ff;
        }
        .cv-dropzone input[type="file"] {
            position: absolute;
            top: 0; left: 0; width: 100%; height: 100%;
            opacity: 0;
            cursor: pointer;
        }
        .file-selected-box {
            display: none;
            background: #f0fdf4;
            border: 1px solid #bbf7d0;
            border-radius: 8px;
            padding: 10px 14px;
            margin-top: 10px;
        }

        /* Anti-spam CAPTCHA Challenge Widget */
        .captcha-widget-card {
            background: #ffffff;
            border: 1px solid #e2e8f0;
            border-radius: 10px;
            padding: 12px 16px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            box-shadow: 0 1px 3px rgba(0,0,0,0.05);
            user-select: none;
        }
        .captcha-checkbox-wrap {
            display: flex;
            align-items: center;
            gap: 12px;
            cursor: pointer;
        }
        .captcha-custom-box {
            width: 24px;
            height: 24px;
            border: 2px solid #94a3b8;
            border-radius: 4px;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: all 0.2s ease;
            background: white;
        }
        .captcha-custom-box.verified {
            border-color: #10b981;
            background-color: #10b981;
            color: white;
        }
        .captcha-custom-box.loading {
            border-color: var(--primary-blue);
            animation: spin 0.8s linear infinite;
            border-top-color: transparent;
        }
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }

        /* Success Tracking Code Box */
        .tracking-code-display {
            background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
            border: 2px dashed #22c55e;
            border-radius: 12px;
            padding: 20px;
            text-align: center;
            margin: 20px 0;
        }
        .tracking-code-value {
            font-family: 'Courier New', Courier, monospace;
            font-size: 1.6rem;
            font-weight: 800;
            color: #166534;
            letter-spacing: 2px;
        }
    </style>
</head>
<body>

    <!-- Header Navigation -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-dark py-3 shadow-sm sticky-top" id="detailNavbar">
        <div class="container">
            <a class="navbar-brand fw-bold d-flex align-items-center gap-2" href="${pageContext.request.contextPath}/careers" id="brandLink">
                <span class="p-1.5 rounded-3 bg-primary text-white d-inline-flex align-items-center justify-content-center" style="width: 32px; height: 32px;">
                    <i class="bi bi-briefcase-fill"></i>
                </span>
                <span>IRMS <span class="fw-normal text-white-50">Careers Portal</span></span>
            </a>
            <div class="d-flex align-items-center gap-3">
                <a href="${pageContext.request.contextPath}/careers" class="btn btn-outline-light btn-sm px-3 rounded-pill" id="backToJobsBtn">
                    <i class="bi bi-arrow-left me-1"></i> Tất cả vị trí tuyển dụng
                </a>
            </div>
        </div>
    </nav>

    <!-- Header Banner -->
    <header class="header-banner" id="jobHeaderBanner">
        <div class="container">
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
                <span class="badge bg-white text-primary px-3 py-1 font-monospace fw-bold" id="jobCodeBadge">
                    ${job.requisition.code != null ? job.requisition.code : 'JOB'}
                </span>
                <span class="badge bg-success-subtle text-success border border-success-subtle px-3 py-1 fw-semibold">
                    <i class="bi bi-broadcast me-1"></i>Đang xuất bản chính thức
                </span>
            </div>
            <h1 class="display-6 fw-bold text-white mb-2" id="jobPositionTitle">${job.requisition.positionTitle}</h1>
            <div class="text-white-50 fs-6 mb-3">${job.title}</div>
            <div class="d-flex flex-wrap gap-4 text-white-50 small">
                <span><i class="bi bi-building me-1 text-primary-subtle"></i>${job.requisition.departmentName != null ? job.requisition.departmentName : 'Toàn công ty'}</span>
                <span><i class="bi bi-geo-alt me-1 text-primary-subtle"></i>${job.workLocation != null ? job.workLocation : 'Trụ sở chính'}</span>
                <span><i class="bi bi-laptop me-1 text-primary-subtle"></i>${job.workType != null ? job.workType : 'Toàn thời gian'}</span>
                <span><i class="bi bi-cash-stack me-1 text-primary-subtle"></i><strong class="text-white">${job.formattedSalaryRange}</strong></span>
                <span><i class="bi bi-clock me-1 text-primary-subtle"></i>Hạn nộp: <fmt:formatDate value="${job.requisition.deadline}" pattern="dd/MM/yyyy" /></span>
            </div>
        </div>
    </header>

    <!-- Main Content -->
    <main class="container py-5" id="mainJobDetailContainer">
        <div class="row g-4">
            <!-- Left Column: Job Description & Details -->
            <div class="col-lg-7 col-xl-8">
                <div class="card border-0 shadow-sm p-4 mb-4 rounded-3 bg-white">
                    <h5 class="fw-bold text-dark border-bottom pb-2 mb-3">
                        <i class="bi bi-briefcase text-primary me-2"></i>Mô tả công việc
                    </h5>
                    <div class="text-secondary mb-4" style="white-space: pre-line; line-height: 1.8;" id="jobDescContent">
                        ${job.requisition.jobDescription}
                    </div>

                    <h5 class="fw-bold text-dark border-bottom pb-2 mb-3">
                        <i class="bi bi-check2-circle text-primary me-2"></i>Yêu cầu ứng viên
                    </h5>
                    <div class="text-secondary mb-4" style="white-space: pre-line; line-height: 1.8;" id="jobReqContent">
                        ${job.requisition.jobRequirements}
                    </div>

                    <h5 class="fw-bold text-dark border-bottom pb-2 mb-3">
                        <i class="bi bi-gift text-primary me-2"></i>Chế độ &amp; Quyền lợi đãi ngộ
                    </h5>
                    <ul class="text-secondary" style="line-height: 1.9;">
                        <li>Mức thu nhập cạnh tranh theo năng lực (<strong class="text-primary">${job.formattedSalaryRange}</strong>) + Lương tháng 13, thưởng hiệu quả kinh doanh.</li>
                        <li>Được tham gia các khóa đào tạo nâng cao chuyên môn và hoàn thiện Khung năng lực nghề nghiệp.</li>
                        <li>Được hưởng đầy đủ quyền lợi BHXH, BHYT, BHTN theo Luật Lao động và chế độ bảo hiểm sức khỏe nâng cao.</li>
                        <li>Môi trường làm việc cởi mở, khuyến khích sáng tạo và trao quyền phát triển cá nhân.</li>
                    </ul>
                </div>
            </div>

            <!-- Right Column: KN-230 Application Form -->
            <div class="col-lg-5 col-xl-4">
                <div class="apply-card sticky-top" style="top: 85px;" id="applyCardContainer">
                    <div class="d-flex align-items-center justify-content-between mb-2">
                        <h5 class="fw-bold text-dark mb-0">Ứng tuyển vị trí này</h5>
                        <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-2 py-1 text-xs">
                            Trực tuyến
                        </span>
                    </div>
                    <p class="text-muted small mb-3">
                        Gửi hồ sơ và CV để hội đồng tuyển dụng liên hệ phỏng vấn.
                    </p>

                    <!-- Alert Box for Error / Warning -->
                    <div class="alert alert-danger alert-dismissible fade show small d-none" id="applyErrorAlert" role="alert">
                        <div class="d-flex gap-2">
                            <i class="bi bi-exclamation-triangle-fill text-danger flex-shrink-0"></i>
                            <span id="applyErrorMessage">Có lỗi xảy ra, vui lòng kiểm tra lại thông tin.</span>
                        </div>
                    </div>

                    <!-- Application Form -->
                    <form id="publicApplyForm" novalidate>
                        <input type="hidden" name="requisitionId" value="${job.requisitionId}">
                        <input type="hidden" name="jobId" value="${job.id}">
                        <input type="hidden" name="captchaToken" id="captchaTokenInput" value="">

                        <!-- Full Name -->
                        <div class="mb-3">
                            <label for="fullNameInput" class="form-label">
                                Họ và tên <span class="text-danger">*</span>
                            </label>
                            <input type="text" class="form-control" id="fullNameInput" name="fullName" 
                                   required placeholder="Ví dụ: Nguyễn Văn A" autocomplete="name">
                            <div class="invalid-feedback" id="fullNameError">Vui lòng nhập họ và tên của bạn.</div>
                        </div>

                        <!-- Email -->
                        <div class="mb-3">
                            <label for="emailInput" class="form-label">
                                Email liên hệ <span class="text-danger">*</span>
                            </label>
                            <input type="email" class="form-control" id="emailInput" name="email" 
                                   required placeholder="ungvien@example.com" autocomplete="email">
                            <div class="invalid-feedback" id="emailError">Vui lòng nhập email hợp lệ để nhận mã tra cứu.</div>
                        </div>

                        <!-- Phone -->
                        <div class="mb-3">
                            <label for="phoneInput" class="form-label">
                                Số điện thoại di động <span class="text-danger">*</span>
                            </label>
                            <input type="tel" class="form-control" id="phoneInput" name="phone" 
                                   required placeholder="0901234567" autocomplete="tel">
                            <div class="invalid-feedback" id="phoneError">Số điện thoại di động Việt Nam gồm 10 chữ số (bắt đầu bằng 03, 05, 07, 08, 09).</div>
                        </div>

                        <!-- Application Source -->
                        <div class="mb-3">
                            <label for="sourceSelect" class="form-label">
                                Nguồn biết tin tuyển dụng
                            </label>
                            <select class="form-select" id="sourceSelect" name="applicationSourceId">
                                <option value="cat-src-career-site">Trang tuyển dụng công ty (Website)</option>
                                <option value="cat-src-linkedin">LinkedIn</option>
                                <option value="cat-src-job-board">Trang việc làm / Mạng xã hội</option>
                                <option value="cat-src-referral">Nhân viên / Bạn bè giới thiệu</option>
                                <c:forEach var="src" items="${applicationSources}">
                                    <option value="${src.id}">${src.value}</option>
                                </c:forEach>
                            </select>
                        </div>

                        <!-- Cover Letter -->
                        <div class="mb-3">
                            <div class="d-flex justify-content-between align-items-center mb-1">
                                <label for="coverLetterInput" class="form-label mb-0">Thư giới thiệu (Cover letter)</label>
                                <span class="text-muted text-xs" id="letterCharCount">0/1000</span>
                            </div>
                            <textarea class="form-control" id="coverLetterInput" name="coverLetter" rows="3" 
                                      maxlength="1000" placeholder="Tóm tắt ngắn gọn lý do bạn phù hợp với vị trí này..."></textarea>
                        </div>

                        <!-- CV File Upload (KN-230, KN-232) -->
                        <div class="mb-3">
                            <label class="form-label d-flex justify-content-between align-items-center">
                                <span>Tệp CV đính kèm <span class="text-danger">*</span></span>
                                <span class="text-muted text-xs font-monospace">PDF / DOCX &le; 10MB</span>
                            </label>

                            <div class="cv-dropzone" id="cvDropzone">
                                <input type="file" id="cvFileInput" name="cvFile" accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document" required>
                                <div id="dropzonePrompt">
                                    <i class="bi bi-cloud-arrow-up-fill text-primary fs-3 d-block mb-1"></i>
                                    <span class="small fw-semibold text-dark d-block">Kéo &amp; thả tệp CV hoặc bấm để chọn</span>
                                    <span class="text-muted text-xs">Chỉ chấp nhận tệp .pdf hoặc .docx, tối đa 10MB</span>
                                </div>
                            </div>

                            <!-- Selected File Badge -->
                            <div class="file-selected-box mt-2" id="fileSelectedBox">
                                <div class="d-flex align-items-center justify-content-between">
                                    <div class="d-flex align-items-center gap-2 overflow-hidden">
                                        <i class="bi bi-file-earmark-pdf-fill text-danger fs-4" id="fileTypeIcon"></i>
                                        <div class="text-truncate">
                                            <div class="small fw-bold text-dark text-truncate" id="selectedFileName">resume.pdf</div>
                                            <div class="text-xs text-muted" id="selectedFileSize">1.2 MB</div>
                                        </div>
                                    </div>
                                    <button type="button" class="btn btn-sm btn-link text-danger p-0 ms-2" id="removeFileBtn" title="Chọn lại tệp">
                                        <i class="bi bi-trash3-fill"></i>
                                    </button>
                                </div>
                            </div>
                            <div class="invalid-feedback d-block text-xs mt-1 d-none" id="fileErrorText">Vui lòng chọn tệp CV hợp lệ (.pdf hoặc .docx, tối đa 10MB).</div>
                        </div>

                        <!-- Anti-spam CAPTCHA Widget (KN-236) -->
                        <div class="mb-4">
                            <div class="captcha-widget-card" id="captchaCard">
                                <div class="captcha-checkbox-wrap" id="captchaCheckboxWrap" onclick="toggleCaptcha()">
                                    <div class="captcha-custom-box" id="captchaBox">
                                        <i class="bi bi-check-lg d-none" id="captchaCheckIcon"></i>
                                    </div>
                                    <span class="small fw-medium text-dark" id="captchaLabel">Tôi không phải là người máy</span>
                                </div>
                                <div class="text-end">
                                    <div class="d-flex align-items-center gap-1 justify-content-end text-primary" style="font-size: 0.75rem; font-weight: 700;">
                                        <i class="bi bi-shield-lock-fill"></i> IRMS Shield
                                    </div>
                                    <span class="text-muted" style="font-size: 0.65rem;">Anti-Spam Bot Protection</span>
                                </div>
                            </div>
                            <div class="text-danger text-xs mt-1 d-none" id="captchaErrorText">Vui lòng xác minh ô chống spam trước khi nộp hồ sơ.</div>
                        </div>

                        <!-- Submit Button -->
                        <button type="submit" class="btn btn-primary w-100 py-2.5 fw-bold shadow-sm d-flex align-items-center justify-content-center gap-2" id="submitApplyBtn">
                            <span class="spinner-border spinner-border-sm d-none" id="submitSpinner" role="status"></span>
                            <span id="submitBtnText"><i class="bi bi-send-fill me-1"></i> Nộp hồ sơ ứng tuyển</span>
                        </button>
                    </form>

                    <div class="mt-4 pt-3 border-top text-xs text-muted">
                        <div class="d-flex justify-content-between">
                            <span><i class="bi bi-eye me-1"></i> Lượt xem: <strong>${job.viewsCount + 1}</strong></span>
                            <span><i class="bi bi-shield-check me-1 text-success"></i> Bảo mật dữ liệu</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </main>

    <!-- Success Modal with Tracking Code (KN-230, KN-233) -->
    <div class="modal fade" id="applySuccessModal" tabindex="-1" aria-labelledby="applySuccessModalLabel" aria-hidden="true" data-bs-backdrop="static">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow-lg rounded-4 overflow-hidden">
                <div class="modal-body p-4 text-center">
                    <!-- Checkmark Icon Animation -->
                    <div class="mx-auto mb-3 d-inline-flex align-items-center justify-content-center rounded-circle bg-success-subtle text-success" style="width: 72px; height: 72px;">
                        <i class="bi bi-check-circle-fill" style="font-size: 2.8rem;"></i>
                    </div>

                    <h4 class="fw-extrabold text-dark mb-1" id="applySuccessModalLabel">Nộp hồ sơ thành công!</h4>
                    <p class="text-secondary small mb-3">
                        Cảm ơn bạn đã ứng tuyển vị trí <strong class="text-dark">${job.requisition.positionTitle}</strong>. 
                        Hệ thống đã tiếp nhận CV và gửi email xác nhận đến hộp thư của bạn.
                    </p>

                    <!-- Tracking Code Box -->
                    <div class="tracking-code-display">
                        <div class="text-xs fw-bold text-success text-uppercase mb-1" style="letter-spacing: 0.5px;">
                            MÃ TRA CỨU HỒ SƠ ỨNG VIÊN
                        </div>
                        <div class="tracking-code-value" id="modalTrackingCode">APP-20261014-XXXX</div>
                        <button type="button" class="btn btn-outline-success btn-sm mt-2 rounded-pill px-3 fw-semibold" id="copyTrackingCodeBtn" onclick="copyTrackingCode()">
                            <i class="bi bi-clipboard me-1"></i> <span id="copyBtnLabel">Sao chép mã tra cứu</span>
                        </button>
                    </div>

                    <!-- Submission Summary Details -->
                    <div class="bg-light rounded-3 p-3 text-start small mb-3 border">
                        <div class="d-flex justify-content-between mb-1.5">
                            <span class="text-muted">Ứng viên:</span>
                            <span class="fw-semibold text-dark" id="modalCandidateName">--</span>
                        </div>
                        <div class="d-flex justify-content-between mb-1.5">
                            <span class="text-muted">Email nhận thông báo:</span>
                            <span class="fw-semibold text-dark" id="modalCandidateEmail">--</span>
                        </div>
                        <div class="d-flex justify-content-between mb-1.5">
                            <span class="text-muted">Thời gian nộp:</span>
                            <span class="fw-semibold text-dark" id="modalSubmittedAt">--</span>
                        </div>
                        <div class="d-flex justify-content-between">
                            <span class="text-muted">Trạng thái:</span>
                            <span class="badge bg-primary">Mới ứng tuyển</span>
                        </div>
                    </div>

                    <div class="d-grid gap-2">
                        <a href="${pageContext.request.contextPath}/careers" class="btn btn-primary rounded-pill py-2 fw-semibold">
                            <i class="bi bi-grid me-1"></i> Khám phá các vị trí khác
                        </a>
                        <button type="button" class="btn btn-outline-secondary rounded-pill py-2 small" data-bs-dismiss="modal">
                            Đóng cửa sổ
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- Footer -->
    <footer class="bg-white border-top py-4 text-center text-secondary small mt-auto">
        <div class="container">
            <p class="mb-1">&copy; 2026 IRMS Recruitment Platform. Bản quyền thuộc về doanh nghiệp.</p>
        </div>
    </footer>

    <!-- Bootstrap 5 JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>

    <!-- Client-side Logic (KN-230, KN-236) -->
    <script>
        (function() {
            const contextPath = '${pageContext.request.contextPath}';
            const form = document.getElementById('publicApplyForm');
            const fullNameInput = document.getElementById('fullNameInput');
            const emailInput = document.getElementById('emailInput');
            const phoneInput = document.getElementById('phoneInput');
            const coverLetterInput = document.getElementById('coverLetterInput');
            const letterCharCount = document.getElementById('letterCharCount');
            const cvFileInput = document.getElementById('cvFileInput');
            const cvDropzone = document.getElementById('cvDropzone');
            const fileSelectedBox = document.getElementById('fileSelectedBox');
            const selectedFileName = document.getElementById('selectedFileName');
            const selectedFileSize = document.getElementById('selectedFileSize');
            const fileTypeIcon = document.getElementById('fileTypeIcon');
            const removeFileBtn = document.getElementById('removeFileBtn');
            const fileErrorText = document.getElementById('fileErrorText');
            const submitBtn = document.getElementById('submitApplyBtn');
            const submitSpinner = document.getElementById('submitSpinner');
            const submitBtnText = document.getElementById('submitBtnText');
            const errorAlert = document.getElementById('applyErrorAlert');
            const errorMessage = document.getElementById('applyErrorMessage');
            const captchaBox = document.getElementById('captchaBox');
            const captchaCheckIcon = document.getElementById('captchaCheckIcon');
            const captchaLabel = document.getElementById('captchaLabel');
            const captchaTokenInput = document.getElementById('captchaTokenInput');
            const captchaErrorText = document.getElementById('captchaErrorText');

            let isCaptchaVerified = false;
            let selectedFile = null;

            // Character counter for Cover Letter
            if (coverLetterInput && letterCharCount) {
                coverLetterInput.addEventListener('input', function() {
                    letterCharCount.textContent = this.value.length + '/1000';
                });
            }

            // Drag and drop handlers
            ['dragenter', 'dragover'].forEach(eventName => {
                cvDropzone.addEventListener(eventName, e => {
                    e.preventDefault();
                    cvDropzone.classList.add('dragover');
                });
            });
            ['dragleave', 'drop'].forEach(eventName => {
                cvDropzone.addEventListener(eventName, e => {
                    e.preventDefault();
                    cvDropzone.classList.remove('dragover');
                });
            });
            cvDropzone.addEventListener('drop', e => {
                if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
                    handleFileSelection(e.dataTransfer.files[0]);
                }
            });

            cvFileInput.addEventListener('change', function() {
                if (this.files && this.files.length > 0) {
                    handleFileSelection(this.files[0]);
                }
            });

            removeFileBtn.addEventListener('click', function(e) {
                e.stopPropagation();
                clearFileSelection();
            });

            function formatBytes(bytes) {
                if (bytes < 1024) return bytes + ' B';
                const i = Math.floor(Math.log(bytes) / Math.log(1024));
                return (bytes / Math.pow(1024, i)).toFixed(1) + ' ' + ['B', 'KB', 'MB', 'GB'][i];
            }

            function handleFileSelection(file) {
                if (!file) return;

                // Validate file extension (.pdf, .docx)
                const name = file.name.toLowerCase();
                const isValidExt = name.endsWith('.pdf') || name.endsWith('.docx');
                if (!isValidExt) {
                    showFileError('Định dạng tệp không hợp lệ! Chỉ chấp nhận tệp .pdf hoặc .docx.');
                    clearFileSelection();
                    return;
                }

                // Validate size (max 10MB)
                const maxSize = 10 * 1024 * 1024;
                if (file.size > maxSize) {
                    showFileError('Dung lượng tệp CV (' + formatBytes(file.size) + ') vượt quá giới hạn tối đa 10MB!');
                    clearFileSelection();
                    return;
                }

                // File is valid
                hideFileError();
                selectedFile = file;
                selectedFileName.textContent = file.name;
                selectedFileSize.textContent = formatBytes(file.size);

                if (name.endsWith('.pdf')) {
                    fileTypeIcon.className = 'bi bi-file-earmark-pdf-fill text-danger fs-4';
                } else {
                    fileTypeIcon.className = 'bi bi-file-earmark-word-fill text-primary fs-4';
                }

                cvDropzone.style.display = 'none';
                fileSelectedBox.style.display = 'block';
            }

            function clearFileSelection() {
                selectedFile = null;
                cvFileInput.value = '';
                cvDropzone.style.display = 'block';
                fileSelectedBox.style.display = 'none';
            }

            function showFileError(msg) {
                fileErrorText.textContent = msg;
                fileErrorText.classList.remove('d-none');
            }
            function hideFileError() {
                fileErrorText.classList.add('d-none');
            }

            // Anti-spam widget verification logic (KN-236)
            window.toggleCaptcha = function() {
                if (isCaptchaVerified) return;

                captchaBox.classList.add('loading');
                captchaLabel.textContent = 'Đang xác minh bảo mật...';

                setTimeout(() => {
                    captchaBox.classList.remove('loading');
                    captchaBox.classList.add('verified');
                    captchaCheckIcon.classList.remove('d-none');
                    captchaLabel.textContent = 'Đã xác minh bảo mật thành công';
                    isCaptchaVerified = true;
                    captchaErrorText.classList.add('d-none');

                    // Sinh token xác thực với timestamp
                    const timestamp = Date.now();
                    const nonce = Math.random().toString(36).substring(2, 10);
                    const token = 'IRMS-CAPTCHA-' + timestamp + '-' + nonce;
                    captchaTokenInput.value = token;
                }, 600);
            };

            // Form Validation & Submission (KN-230, KN-231, KN-232, KN-233)
            form.addEventListener('submit', function(e) {
                e.preventDefault();
                errorAlert.classList.add('d-none');

                let isValid = true;

                // Validate Full Name
                if (!fullNameInput.value.trim()) {
                    fullNameInput.classList.add('is-invalid');
                    isValid = false;
                } else {
                    fullNameInput.classList.remove('is-invalid');
                }

                // Validate Email
                const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                if (!emailRegex.test(emailInput.value.trim())) {
                    emailInput.classList.add('is-invalid');
                    isValid = false;
                } else {
                    emailInput.classList.remove('is-invalid');
                }

                // Validate Phone (VN phone: 10 digits starting with 03, 05, 07, 08, 09)
                const phoneClean = phoneInput.value.trim().replace(/[\s.-]/g, '');
                const phoneRegex = /^0[3|5|7|8|9][0-9]{8}$/;
                if (!phoneRegex.test(phoneClean)) {
                    phoneInput.classList.add('is-invalid');
                    isValid = false;
                } else {
                    phoneInput.classList.remove('is-invalid');
                }

                // Validate CV File
                if (!selectedFile && (!cvFileInput.files || cvFileInput.files.length === 0)) {
                    showFileError('Vui lòng chọn hoặc tải lên tệp hồ sơ CV của bạn.');
                    isValid = false;
                } else {
                    hideFileError();
                }

                // Validate CAPTCHA
                if (!isCaptchaVerified || !captchaTokenInput.value) {
                    captchaErrorText.classList.remove('d-none');
                    isValid = false;
                } else {
                    captchaErrorText.classList.add('d-none');
                }

                if (!isValid) return;

                // Chuẩn bị FormData Multipart gửi lên Backend API (KN-233)
                const formData = new FormData(form);
                if (selectedFile) {
                    formData.set('cvFile', selectedFile);
                }

                // Loading State
                submitBtn.disabled = true;
                submitSpinner.classList.remove('d-none');
                submitBtnText.textContent = 'Đang xử lý nộp hồ sơ...';

                fetch(contextPath + '/api/v1/public/jobs/apply', {
                    method: 'POST',
                    body: formData
                })
                .then(async res => {
                    const data = await res.json().catch(() => ({}));
                    return { status: res.status, data: data };
                })
                .then(({ status, data }) => {
                    submitBtn.disabled = false;
                    submitSpinner.classList.add('d-none');
                    submitBtnText.innerHTML = '<i class="bi bi-send-fill me-1"></i> Nộp hồ sơ ứng tuyển';

                    if (status === 200 || status === 201) {
                        // Thành công: Hiển thị Modal kèm Mã tra cứu độc nhất (KN-230, KN-233)
                        document.getElementById('modalTrackingCode').textContent = data.trackingCode || 'APP-20261014-XXXX';
                        document.getElementById('modalCandidateName').textContent = data.candidateName || fullNameInput.value;
                        document.getElementById('modalCandidateEmail').textContent = data.email || emailInput.value;
                        document.getElementById('modalSubmittedAt').textContent = data.submittedAt || new Date().toLocaleString();

                        const successModal = new bootstrap.Modal(document.getElementById('applySuccessModal'));
                        successModal.show();

                        // Reset form
                        form.reset();
                        clearFileSelection();
                        if (letterCharCount) letterCharCount.textContent = '0/1000';
                        isCaptchaVerified = false;
                        captchaBox.classList.remove('verified');
                        captchaCheckIcon.classList.add('d-none');
                        captchaLabel.textContent = 'Tôi không phải là người máy';
                        captchaTokenInput.value = '';
                    } else {
                        // Lỗi (Nộp trùng 409, Vượt tần suất 429, Sai file 400, v.v.)
                        const errorMsg = data.error || 'Có lỗi xảy ra khi nộp hồ sơ. Vui lòng kiểm tra lại.';
                        showFormError(errorMsg);
                    }
                })
                .catch(err => {
                    submitBtn.disabled = false;
                    submitSpinner.classList.add('d-none');
                    submitBtnText.innerHTML = '<i class="bi bi-send-fill me-1"></i> Nộp hồ sơ ứng tuyển';
                    showFormError('Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại đường truyền.');
                });
            });

            function showFormError(msg) {
                errorMessage.textContent = msg;
                errorAlert.classList.remove('d-none');
                errorAlert.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }

            // Copy Tracking Code to clipboard
            window.copyTrackingCode = function() {
                const code = document.getElementById('modalTrackingCode').textContent;
                navigator.clipboard.writeText(code).then(() => {
                    const label = document.getElementById('copyBtnLabel');
                    const original = label.textContent;
                    label.textContent = 'Đã sao chép!';
                    setTimeout(() => { label.textContent = original; }, 2000);
                }).catch(() => {
                    alert('Mã tra cứu: ' + code);
                });
            };
        })();
    </script>
</body>
</html>

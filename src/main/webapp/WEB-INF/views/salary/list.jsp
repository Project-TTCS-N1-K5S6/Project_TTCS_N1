<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<c:set var="pageTitle" value="Khai báo dải lương theo chức danh" />
<c:set var="breadcrumb" value="Khai báo dải lương" />
<c:set var="activeMenu" value="salary" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<style>
    /* Tối ưu bảng dải lương vừa khít 100% trang ngang, tuyệt đối không bị đè chữ */
    .table-salary {
        table-layout: fixed;
        width: 100%;
        margin-bottom: 0;
    }
    .table-salary th,
    .table-salary td {
        padding: 0.65rem 0.45rem !important;
        vertical-align: middle;
        overflow: hidden; /* Chống tràn đè chữ sang ô lân cận */
    }
    .table-salary thead th {
        white-space: nowrap !important;
        font-size: 0.72rem;
        font-weight: 700;
        letter-spacing: 0.03em;
        text-transform: uppercase;
        background-color: #f8fafc;
        border-bottom: 1px solid #e2e8f0;
        color: #475569;
    }
    .table-salary tbody td {
        font-size: 0.8125rem;
    }
    .table-salary .salary-badge {
        font-size: 0.76rem;
        font-weight: 600;
        letter-spacing: -0.2px;
        white-space: nowrap;
        display: inline-block;
        max-width: 100%;
        text-overflow: ellipsis;
        overflow: hidden;
    }
    .table-salary .note-text {
        font-size: 0.75rem;
        line-height: 1.35;
        color: #64748b;
        word-break: break-word;
    }
    .table-salary .btn-xs {
        padding: 0.25rem 0.45rem;
        font-size: 0.75rem;
        line-height: 1;
    }
    /* Màu sắc hiển thị sắc nét cho các cấp bậc */
    .bg-teal-subtle { background-color: #ccfbf1 !important; border-color: #99f6e4 !important; color: #0f766e !important; }
    .bg-indigo-subtle { background-color: #e0e7ff !important; border-color: #c7d2fe !important; color: #4338ca !important; }
    .bg-purple { background-color: #7c3aed !important; color: #ffffff !important; }

    @media (min-width: 992px) {
        .salary-table-responsive {
            overflow-x: hidden !important;
        }
    }
</style>

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Tiêu đề trang & Nút thao tác -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <h4 class="fw-bold mb-1">Khai báo dải lương theo chức danh</h4>
                <p class="text-muted small mb-0">
                    Mỗi chức danh có mã, tên, cấp bậc, khung lương tối thiểu &amp; tối đa. Dải lương được dùng làm hạn mức xét duyệt Offer tuyển dụng.
                </p>
            </div>
            <div class="d-flex gap-2">
                <a href="#offerCheckSection" class="btn btn-outline-primary">
                    <i class="bi bi-calculator me-1"></i> Đối soát hạn mức Offer
                </a>
                <button class="btn btn-primary shadow-sm" data-bs-toggle="modal" data-bs-target="#addSalaryModal">
                    <i class="bi bi-plus-circle me-1"></i> Khai báo chức danh mới
                </button>
            </div>
        </div>

        <!-- Thanh công cụ tìm kiếm và bộ lọc -->
        <div class="card mb-4 border shadow-sm">
            <div class="card-body p-3">
                <form action="${pageContext.request.contextPath}/salary-ranges" method="GET" class="row g-2 align-items-center">
                    <div class="col-12 col-md-4">
                        <div class="input-group">
                            <span class="input-group-text bg-light text-muted border-end-0"><i class="bi bi-search"></i></span>
                            <input type="text" name="search" class="form-control border-start-0" 
                                   placeholder="Tìm kiếm mã hoặc tên chức danh..." value="${paramSearch}">
                        </div>
                    </div>
                    <div class="col-12 col-sm-6 col-md-3">
                        <select class="form-select" name="deptId">
                            <option value="">-- Tất cả phòng ban --</option>
                            <c:forEach var="d" items="${departments}">
                                <option value="${d.id}" ${paramDeptId == d.id ? 'selected' : ''}>${d.name}</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-12 col-sm-6 col-md-3">
                        <select class="form-select" name="level">
                            <option value="">-- Tất cả cấp bậc --</option>
                            <option value="Intern" ${paramLevel == 'Intern' ? 'selected' : ''}>Intern (Thực tập)</option>
                            <option value="Fresher" ${paramLevel == 'Fresher' ? 'selected' : ''}>Fresher</option>
                            <option value="Junior" ${paramLevel == 'Junior' ? 'selected' : ''}>Junior</option>
                            <option value="Middle" ${paramLevel == 'Middle' ? 'selected' : ''}>Middle / Chuyên viên</option>
                            <option value="Senior" ${paramLevel == 'Senior' ? 'selected' : ''}>Senior</option>
                            <option value="Lead" ${paramLevel == 'Lead' ? 'selected' : ''}>Lead / Trưởng nhóm</option>
                            <option value="Manager" ${paramLevel == 'Manager' ? 'selected' : ''}>Manager / Trưởng phòng</option>
                            <option value="Director" ${paramLevel == 'Director' ? 'selected' : ''}>Director / Giám đốc</option>
                            <c:forEach var="lvl" items="${levels}">
                                <c:if test="${lvl != 'Intern' && lvl != 'Fresher' && lvl != 'Junior' && lvl != 'Middle' && lvl != 'Senior' && lvl != 'Lead' && lvl != 'Manager' && lvl != 'Director'}">
                                    <option value="${lvl}" ${paramLevel == lvl ? 'selected' : ''}>${lvl}</option>
                                </c:if>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-12 col-md-2 d-flex gap-2">
                        <button type="submit" class="btn btn-primary flex-fill">
                            <i class="bi bi-funnel me-1"></i> Lọc
                        </button>
                        <c:if test="${not empty paramSearch || not empty paramDeptId || not empty paramLevel}">
                            <a href="${pageContext.request.contextPath}/salary-ranges" class="btn btn-outline-secondary" title="Xóa bộ lọc">
                                <i class="bi bi-arrow-counterclockwise"></i>
                            </a>
                        </c:if>
                    </div>
                </form>
            </div>
        </div>

        <!-- Bảng danh sách Dải lương chức danh -->
        <div class="card mb-4 shadow-sm border">
            <div class="card-header bg-white py-3 d-flex justify-content-between align-items-center">
                <span class="fw-bold text-dark">
                    <i class="bi bi-table text-primary me-2"></i> Danh mục Chức danh & Dải lương ngân sách (${not empty salaryRanges ? fn:length(salaryRanges) : 0} bản ghi)
                </span>
                <span class="badge bg-light text-muted border">Đơn vị: VND</span>
            </div>
            <div class="card-body p-0">
                <div class="table-responsive salary-table-responsive">
                    <table class="table table-hover table-custom table-salary mb-0 align-middle">
                        <thead class="table-light">
                            <tr>
                                <th style="width: 9%;">Mã</th>
                                <th style="width: 19%;">Chức danh</th>
                                <th style="width: 10%;">Cấp bậc</th>
                                <th style="width: 16%;">Phòng ban</th>
                                <th style="width: 23%;">Dải lương (Min - Max)</th>
                                <th style="width: 14%;">Ghi chú / Hạn mức</th>
                                <th class="text-center" style="width: 9%; min-width: 85px;">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="s" items="${salaryRanges}">
                                <tr>
                                    <td>
                                        <code class="fw-bold px-1.5 py-0.5 bg-light text-primary border rounded text-xs text-break">
                                            ${s.positionCode != null ? s.positionCode : 'N/A'}
                                        </code>
                                    </td>
                                    <td>
                                        <div class="fw-semibold text-dark text-break" style="font-size: 0.8125rem;">${s.positionTitle}</div>
                                        <small class="text-muted d-block text-xs" style="font-size: 0.7rem;">
                                            Cập nhật: <fmt:formatDate value="${s.updatedAt}" pattern="dd/MM/yyyy HH:mm" />
                                        </small>
                                    </td>
                                    <td>
                                        <span class="badge ${s.getLevelBadgeClass()} px-2 py-1 text-xs text-nowrap">
                                            ${s.level != null ? s.level : 'N/A'}
                                        </span>
                                    </td>
                                    <td>
                                        <span class="text-muted text-xs text-break" style="font-size: 0.775rem;">
                                            <i class="bi bi-building me-1 text-secondary"></i>${s.departmentName != null ? s.departmentName : 'Toàn công ty'}
                                        </span>
                                    </td>
                                    <td>
                                        <span class="badge bg-success-subtle text-success border border-success-subtle px-2 py-1.5 salary-badge" title="${s.getFormattedRange()}">
                                            <i class="bi bi-cash-stack me-1"></i>${s.getFormattedRange()}
                                        </span>
                                    </td>
                                    <td>
                                        <div class="note-text" title="${s.note != null && !s.note.isEmpty() ? s.note : 'Hạn mức tiêu chuẩn'}">
                                            <c:out value="${s.note != null && !s.note.isEmpty() ? s.note : 'Hạn mức tiêu chuẩn'}" />
                                        </div>
                                    </td>
                                    <td class="text-center">
                                        <div class="btn-group btn-group-sm">
                                            <button type="button" class="btn btn-outline-primary btn-xs" 
                                                    title="Chỉnh sửa dải lương"
                                                    onclick="openEditModal('${s.id}')">
                                                <i class="bi bi-pencil-square"></i>
                                            </button>
                                            <button type="button" class="btn btn-outline-success btn-xs" 
                                                    title="Kiểm tra hạn mức offer"
                                                    onclick="selectPositionForCheck('${s.id}', '${s.positionTitle}', '${s.level}', '${s.minSalary}', '${s.maxSalary}')">
                                                <i class="bi bi-check2-circle"></i>
                                            </button>
                                            <button type="button" class="btn btn-outline-danger btn-xs" 
                                                    title="Xóa dải lương"
                                                    onclick="openDeleteModal('${s.id}', '${s.positionTitle} - ${s.level}')">
                                                <i class="bi bi-trash"></i>
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty salaryRanges}">
                                <tr>
                                    <td colspan="7" class="text-center py-5 text-muted">
                                        <i class="bi bi-cash-stack fs-1 text-secondary d-block mb-2"></i>
                                        Chưa có dải lương nào phù hợp với điều kiện tìm kiếm.<br>
                                        <button class="btn btn-sm btn-primary mt-3" data-bs-toggle="modal" data-bs-target="#addSalaryModal">
                                            <i class="bi bi-plus-circle me-1"></i> Khai báo chức danh đầu tiên
                                        </button>
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- Widget: Tra cứu & Đối soát Hạn mức duyệt Offer -->
        <div class="card mb-4 shadow-sm border border-primary border-opacity-25" id="offerCheckSection">
            <div class="card-header bg-primary bg-opacity-10 py-3 d-flex justify-content-between align-items-center">
                <div class="fw-bold text-primary">
                    <i class="bi bi-shield-check me-2"></i> Công cụ kiểm tra hạn mức duyệt Offer theo dải lương
                </div>
            </div>
            <div class="card-body">
                <p class="text-muted small mb-3">
                    Chọn chức danh đã khai báo và nhập mức lương đề xuất dành cho ứng viên để hệ thống tự động đối soát hạn mức phê duyệt Thư mời nhận việc (Offer Letter).
                </p>
                <div class="row g-3">
                    <div class="col-12 col-md-5">
                        <label class="form-label small fw-semibold">Chức danh & Cấp bậc xét duyệt <span class="text-danger">*</span></label>
                        <select class="form-select" id="checkSalaryRangeSelect">
                            <option value="">-- Chọn chức danh đối soát --</option>
                            <c:forEach var="s" items="${salaryRanges}">
                                <option value="${s.id}" 
                                        data-title="${s.positionTitle}" 
                                        data-level="${s.level}"
                                        data-min="${s.minSalary}" 
                                        data-max="${s.maxSalary}"
                                        data-formatted="${s.getFormattedRange()}">
                                    [${s.positionCode}] ${s.positionTitle} (${s.level}) - Khung: ${s.getFormattedRange()}
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-12 col-md-4">
                        <label class="form-label small fw-semibold">Mức lương đề xuất Offer (VND) <span class="text-danger">*</span></label>
                        <div class="input-group">
                            <input type="number" step="500000" class="form-control" id="checkOfferSalaryInput" placeholder="Ví dụ: 28000000">
                            <span class="input-group-text">VND</span>
                        </div>
                    </div>
                    <div class="col-12 col-md-3 d-flex align-items-end">
                        <button type="button" class="btn btn-primary w-100" onclick="performOfferLimitCheck()">
                            <i class="bi bi-search me-1"></i> Đối soát hạn mức
                        </button>
                    </div>
                </div>

                <!-- Kết quả đối soát trực quan -->
                <div id="checkResultContainer" class="mt-3" style="display: none;">
                    <div id="checkResultAlert" class="alert mb-0 d-flex align-items-center gap-3">
                        <i id="checkResultIcon" class="bi fs-3"></i>
                        <div class="flex-grow-1">
                            <div class="fw-bold" id="checkResultTitle">Kết quả đối soát</div>
                            <div class="small" id="checkResultMessage">Nội dung thông báo</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

    </div>

    <!-- Modal 1: Khai báo dải lương mới (KN-103) -->
    <div class="modal fade" id="addSalaryModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content border-0 shadow">
                <form action="${pageContext.request.contextPath}/salary-ranges/create" method="POST" onsubmit="return validateSalaryForm(this)">
                    <div class="modal-header bg-primary text-white">
                        <h5 class="modal-title fw-bold">
                            <i class="bi bi-cash-stack me-2"></i> Khai báo dải lương chức danh mới
                        </h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body p-4">
                        <div class="alert alert-info py-2 small mb-3">
                            <i class="bi bi-info-circle me-1"></i> 
                            Dải lương được khai báo sẽ làm căn cứ bắt buộc khi Trưởng phòng Nhân sự duyệt Offer cho ứng viên.
                        </div>
                        <div class="row g-3">
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Mã chức danh <span class="text-danger">*</span></label>
                                <input type="text" class="form-control text-uppercase" name="positionCode" required 
                                       placeholder="VD: DEV-SR, HR-REC, SALES-MGR">
                                <small class="text-muted text-xs">Mã định danh duy nhất của chức danh trong tổ chức</small>
                            </div>
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Tên chức danh <span class="text-danger">*</span></label>
                                <input type="text" class="form-control" name="positionTitle" required 
                                       placeholder="VD: Kỹ sư Java Backend, Chuyên viên Tuyển dụng">
                            </div>
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Cấp bậc chức danh <span class="text-danger">*</span></label>
                                <select class="form-select" name="level" required>
                                    <option value="">-- Chọn cấp bậc --</option>
                                    <option value="Intern">Intern (Thực tập sinh)</option>
                                    <option value="Fresher">Fresher</option>
                                    <option value="Junior">Junior</option>
                                    <option value="Middle">Middle (Chuyên viên)</option>
                                    <option value="Senior">Senior (Chuyên viên cao cấp)</option>
                                    <option value="Lead">Lead (Trưởng nhóm)</option>
                                    <option value="Manager">Manager (Trưởng phòng / Quản lý)</option>
                                    <option value="Director">Director (Giám đốc khối)</option>
                                </select>
                            </div>
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Phòng ban áp dụng</label>
                                <select class="form-select" name="departmentId">
                                    <option value="">-- Chung cho toàn công ty --</option>
                                    <c:forEach var="d" items="${departments}">
                                        <option value="${d.id}">${d.name}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Mức lương tối thiểu (VND) <span class="text-danger">*</span></label>
                                <input type="number" step="500000" min="0" class="form-control" name="minSalary" required placeholder="15000000">
                                <small class="text-muted text-xs">Hạn mức sàn ngân sách cho vị trí này</small>
                            </div>
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Mức lương tối đa (VND) <span class="text-danger">*</span></label>
                                <input type="number" step="500000" min="0" class="form-control" name="maxSalary" required placeholder="30000000">
                                <small class="text-muted text-xs">Hạn mức trần phê duyệt Offer không cần duyệt ngoại lệ</small>
                            </div>
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Đơn vị tiền tệ</label>
                                <input type="text" class="form-control" name="currency" value="VND" required>
                            </div>
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Quy định / Hạn mức duyệt Offer</label>
                                <input type="text" class="form-control" name="note" placeholder="VD: Offer trên mức trần cần TGĐ phê duyệt">
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer bg-light">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-primary px-4"><i class="bi bi-check-lg me-1"></i> Lưu khai báo dải lương</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Modal 2: Cập nhật dải lương chức danh -->
    <div class="modal fade" id="editSalaryModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content border-0 shadow">
                <form action="${pageContext.request.contextPath}/salary-ranges/edit" method="POST" onsubmit="return validateSalaryForm(this)">
                    <input type="hidden" name="id" id="editSalaryId">
                    <div class="modal-header bg-primary text-white">
                        <h5 class="modal-title fw-bold">
                            <i class="bi bi-pencil-square me-2"></i> Cập nhật dải lương chức danh
                        </h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body p-4">
                        <div class="row g-3">
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Mã chức danh <span class="text-danger">*</span></label>
                                <input type="text" class="form-control text-uppercase" name="positionCode" id="editPositionCode" required>
                            </div>
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Tên chức danh <span class="text-danger">*</span></label>
                                <input type="text" class="form-control" name="positionTitle" id="editPositionTitle" required>
                            </div>
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Cấp bậc chức danh <span class="text-danger">*</span></label>
                                <select class="form-select" name="level" id="editLevel" required>
                                    <option value="Intern">Intern (Thực tập sinh)</option>
                                    <option value="Fresher">Fresher</option>
                                    <option value="Junior">Junior</option>
                                    <option value="Middle">Middle (Chuyên viên)</option>
                                    <option value="Senior">Senior (Chuyên viên cao cấp)</option>
                                    <option value="Lead">Lead (Trưởng nhóm)</option>
                                    <option value="Manager">Manager (Trưởng phòng / Quản lý)</option>
                                    <option value="Director">Director (Giám đốc khối)</option>
                                </select>
                            </div>
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Phòng ban áp dụng</label>
                                <select class="form-select" name="departmentId" id="editDepartmentId">
                                    <option value="">-- Chung cho toàn công ty --</option>
                                    <c:forEach var="d" items="${departments}">
                                        <option value="${d.id}">${d.name}</option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Mức lương tối thiểu (VND) <span class="text-danger">*</span></label>
                                <input type="number" step="500000" min="0" class="form-control" name="minSalary" id="editMinSalary" required>
                            </div>
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Mức lương tối đa (VND) <span class="text-danger">*</span></label>
                                <input type="number" step="500000" min="0" class="form-control" name="maxSalary" id="editMaxSalary" required>
                            </div>
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Đơn vị tiền tệ</label>
                                <input type="text" class="form-control" name="currency" id="editCurrency" required>
                            </div>
                            <div class="col-12 col-md-6">
                                <label class="form-label small fw-semibold">Quy định / Hạn mức duyệt Offer</label>
                                <input type="text" class="form-control" name="note" id="editNote">
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer bg-light">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-primary px-4"><i class="bi bi-check-lg me-1"></i> Lưu thay đổi</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Modal 3: Xác nhận xóa dải lương -->
    <div class="modal fade" id="deleteSalaryModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow">
                <form action="${pageContext.request.contextPath}/salary-ranges/delete" method="POST">
                    <input type="hidden" name="id" id="deleteSalaryId">
                    <div class="modal-header bg-danger text-white">
                        <h5 class="modal-title fw-bold"><i class="bi bi-exclamation-triangle-fill me-2"></i> Xác nhận xóa dải lương</h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body p-4 text-center">
                        <p class="mb-2">Bạn có chắc chắn muốn xóa dải lương của chức danh:</p>
                        <h5 class="fw-bold text-danger mb-3" id="deletePositionTitleName"></h5>
                        <p class="small text-muted mb-0">Hành động này sẽ được ghi nhận vào Nhật ký kiểm toán bảo mật của hệ thống.</p>
                    </div>
                    <div class="modal-footer bg-light">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy bỏ</button>
                        <button type="submit" class="btn btn-danger px-4"><i class="bi bi-trash me-1"></i> Xác nhận xóa</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

<jsp:include page="../common/footer.jsp" />

<script>
    // Kiểm tra tính hợp lệ của Form trước khi gửi lên server
    function validateSalaryForm(form) {
        var minVal = parseFloat(form.minSalary.value);
        var maxVal = parseFloat(form.maxSalary.value);
        if (minVal < 0) {
            alert('Mức lương tối thiểu không được âm!');
            form.minSalary.focus();
            return false;
        }
        if (maxVal <= 0) {
            alert('Mức lương tối đa phải lớn hơn 0!');
            form.maxSalary.focus();
            return false;
        }
        if (maxVal < minVal) {
            alert('Mức lương tối đa phải lớn hơn hoặc bằng mức lương tối thiểu!');
            form.maxSalary.focus();
            return false;
        }
        return true;
    }

    // Mở modal sửa và nạp thông tin qua AJAX
    function openEditModal(id) {
        fetch('${pageContext.request.contextPath}/salary-ranges?action=detail&id=' + encodeURIComponent(id))
            .then(function(res) { return res.json(); })
            .then(function(data) {
                if (data.id) {
                    document.getElementById('editSalaryId').value = data.id;
                    document.getElementById('editPositionCode').value = data.positionCode || '';
                    document.getElementById('editPositionTitle').value = data.positionTitle || '';
                    document.getElementById('editLevel').value = data.level || 'Junior';
                    document.getElementById('editDepartmentId').value = data.departmentId || '';
                    document.getElementById('editMinSalary').value = data.minSalary || '';
                    document.getElementById('editMaxSalary').value = data.maxSalary || '';
                    document.getElementById('editCurrency').value = data.currency || 'VND';
                    document.getElementById('editNote').value = data.note || '';

                    var modal = new bootstrap.Modal(document.getElementById('editSalaryModal'));
                    modal.show();
                } else {
                    alert('Không tìm thấy thông tin dải lương.');
                }
            })
            .catch(function(err) {
                alert('Lỗi tải thông tin dải lương: ' + err);
            });
    }

    // Mở modal xóa
    function openDeleteModal(id, title) {
        document.getElementById('deleteSalaryId').value = id;
        document.getElementById('deletePositionTitleName').innerText = title;
        var modal = new bootstrap.Modal(document.getElementById('deleteSalaryModal'));
        modal.show();
    }

    // Chọn nhanh chức danh từ bảng vào Widget đối soát hạn mức Offer
    function selectPositionForCheck(id, title, level, min, max) {
        var select = document.getElementById('checkSalaryRangeSelect');
        select.value = id;
        document.getElementById('offerCheckSection').scrollIntoView({ behavior: 'smooth' });
        document.getElementById('checkOfferSalaryInput').focus();
    }

    // Thực hiện đối soát hạn mức duyệt Offer (Tiêu chí 2 KN-103)
    function performOfferLimitCheck() {
        var select = document.getElementById('checkSalaryRangeSelect');
        var rangeId = select.value;
        var offerSalaryVal = document.getElementById('checkOfferSalaryInput').value;

        if (!rangeId) {
            alert('Vui lòng chọn chức danh cần đối soát!');
            select.focus();
            return;
        }
        if (!offerSalaryVal || parseFloat(offerSalaryVal) <= 0) {
            alert('Vui lòng nhập mức lương offer đề xuất hợp lệ!');
            document.getElementById('checkOfferSalaryInput').focus();
            return;
        }

        var url = '${pageContext.request.contextPath}/salary-ranges?action=check-offer&rangeId=' 
                  + encodeURIComponent(rangeId) + '&offerSalary=' + encodeURIComponent(offerSalaryVal);

        fetch(url)
            .then(function(res) { return res.json(); })
            .then(function(data) {
                var container = document.getElementById('checkResultContainer');
                var alertBox = document.getElementById('checkResultAlert');
                var icon = document.getElementById('checkResultIcon');
                var title = document.getElementById('checkResultTitle');
                var message = document.getElementById('checkResultMessage');

                container.style.display = 'block';
                alertBox.className = 'alert mb-0 d-flex align-items-center gap-3 ';

                if (data.status === 'VALID') {
                    alertBox.className += 'alert-success border-success';
                    icon.className = 'bi bi-check-circle-fill text-success fs-3';
                    title.innerText = 'ĐẠT HẠN MỨC PHÊ DUYỆT OFFER';
                    title.className = 'fw-bold text-success';
                } else if (data.status === 'EXCEEDED_MAX') {
                    alertBox.className += 'alert-danger border-danger';
                    icon.className = 'bi bi-exclamation-triangle-fill text-danger fs-3';
                    title.innerText = 'CẢNH BÁO: VƯỢT HẠN MỨC DẢI LƯƠNG TỐI ĐA!';
                    title.className = 'fw-bold text-danger';
                } else if (data.status === 'BELOW_MIN') {
                    alertBox.className += 'alert-warning border-warning';
                    icon.className = 'bi bi-info-circle-fill text-warning fs-3';
                    title.innerText = 'LƯU Ý: THẤP HƠN HẠN MỨC SÀN QUY ĐỊNH';
                    title.className = 'fw-bold text-warning-emphasis';
                } else {
                    alertBox.className += 'alert-secondary';
                    icon.className = 'bi bi-question-circle-fill text-secondary fs-3';
                    title.innerText = 'KẾT QUẢ ĐỐI SOÁT';
                    title.className = 'fw-bold text-secondary';
                }

                message.innerText = data.message;
            })
            .catch(function(err) {
                alert('Lỗi khi đối soát hạn mức offer: ' + err);
            });
    }
</script>

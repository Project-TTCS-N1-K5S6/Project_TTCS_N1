<%--
  ==============================================================================
  BIỂU MẪU CẤU HÌNH MA TRẬN PHÊ DUYỆT ĐA CẤP (approval/form.jsp)
  Chức năng:
  - Thiết lập quy tắc phê duyệt: Mã quy tắc, Tên ma trận, Phòng ban áp dụng, Điều kiện dải lương hoặc ngưỡng ngân sách.
  - Cấu hình động các bước phê duyệt tuần tự (Step 1, Step 2, Step 3...): Chọn vai trò duyệt (APPROVER, HR_MANAGER, DEPT_HEAD), thời hạn SLA (giờ), quyền từ chối.
  - Hỗ trợ thêm/xóa/sắp xếp thứ tự các bước duyệt linh hoạt bằng JavaScript.
  ==============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="${isEdit ? 'Chỉnh sửa ma trận phê duyệt' : 'Thêm cấu hình ma trận phê duyệt'}" />
<c:set var="breadcrumb" value="${isEdit ? 'Chỉnh sửa ma trận' : 'Thêm ma trận phê duyệt'}" />
<c:set var="activeMenu" value="approval-matrix" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Tiêu đề & Điều hướng trên cùng -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <nav aria-label="breadcrumb">
                    <ol class="breadcrumb mb-1 text-xs">
                        <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/dashboard" class="text-decoration-none text-muted">Quản trị hệ thống</a></li>
                        <li class="breadcrumb-item"><a href="${pageContext.request.contextPath}/admin/approval-matrix" class="text-decoration-none text-muted">Ma trận phê duyệt</a></li>
                        <li class="breadcrumb-item active text-primary fw-semibold" aria-current="page">${isEdit ? 'Chỉnh sửa cấu hình' : 'Thêm cấu hình mới'}</li>
                    </ol>
                </nav>
                <h4 class="fw-bold mb-1 text-dark">
                    <i class="bi bi-diagram-2 text-primary me-2"></i>${isEdit ? 'Chỉnh sửa ma trận phê duyệt' : 'Thêm mới ma trận phê duyệt'}
                </h4>
                <p class="text-muted small mb-0">
                    Khai báo chuỗi cấp phê duyệt theo phòng ban, mức lương đề xuất và thiết lập quy tắc tự động bổ sung cấp khi vượt hạn mức.
                </p>
            </div>
            <div>
                <a href="${pageContext.request.contextPath}/admin/approval-matrix" class="btn btn-outline-secondary shadow-sm">
                    <i class="bi bi-arrow-left me-1"></i> Quay lại danh sách
                </a>
            </div>
        </div>

        <form id="matrixForm" method="post" action="${pageContext.request.contextPath}/admin/approval-matrix/save" class="needs-validation">
            <input type="hidden" name="id" value="${matrix.id}" />

            <!-- BỐ CỤC 1 CỘT (SINGLE COLUMN) - TẤT CẢ KHỐI XẾP DỌC, MỖI INPUT 1 HÀNG RIÊNG BIỆT -->
            <div class="mx-auto" style="max-width: 920px; width: 100%;">

                <!-- KHỐI 1: PHẠM VI VÀ ĐIỀU KIỆN ÁP DỤNG -->
                <div class="card shadow-sm border mb-4">
                    <div class="card-header bg-light py-3 border-bottom">
                        <h6 class="fw-bold mb-0 text-dark">
                            <i class="bi bi-sliders text-primary me-2"></i>1. Phạm vi và điều kiện áp dụng
                        </h6>
                    </div>
                    <div class="card-body p-4">
                        <!-- Mã cấu hình (1 dòng riêng) -->
                        <div class="mb-3">
                            <label class="form-label fw-semibold text-dark small">Mã cấu hình <span class="text-danger">*</span></label>
                            <input type="text" name="code" class="form-control text-uppercase font-monospace" 
                                   placeholder="VD: MTPD-KT, MTPD-KD, MTPD-CHUNG..." value="${matrix.code}" required maxlength="50" />
                            <div class="form-text text-xs">Mã định danh duy nhất của cấu hình ma trận phê duyệt trong toàn hệ thống.</div>
                        </div>

                        <!-- Tên cấu hình (1 dòng riêng) -->
                        <div class="mb-3">
                            <label class="form-label fw-semibold text-dark small">Tên cấu hình <span class="text-danger">*</span></label>
                            <input type="text" name="name" class="form-control" 
                                   placeholder="VD: Ma trận phê duyệt Khối Công nghệ Thông tin..." value="${matrix.name}" required maxlength="255" />
                        </div>

                        <!-- Phòng ban áp dụng (1 dòng riêng) -->
                        <div class="mb-3">
                            <label class="form-label fw-semibold text-dark small">Phòng ban áp dụng</label>
                            <select name="departmentId" id="departmentId" class="form-select" onchange="updateLiveSimulation()">
                                <option value="GLOBAL" ${empty matrix.departmentId ? 'selected' : ''}>Toàn công ty (Áp dụng chung)</option>
                                <c:forEach var="dept" items="${departments}">
                                    <option value="${dept.id}" ${matrix.departmentId == dept.id ? 'selected' : ''}>${dept.name} (${dept.code})</option>
                                </c:forEach>
                            </select>
                            <div class="form-text text-xs">Cấu hình cho phòng ban cụ thể sẽ được ưu tiên áp dụng trước quy tắc chung của toàn công ty.</div>
                        </div>

                        <!-- Quy tắc mức lương (1 dòng riêng) -->
                        <div class="mb-3">
                            <label class="form-label fw-semibold text-dark small">Quy tắc mức lương <span class="text-danger">*</span></label>
                            <select name="salaryCondition" id="salaryCondition" class="form-select" onchange="toggleSalaryInputs(); updateLiveSimulation();">
                                <option value="RANGE" ${matrix.salaryCondition == 'RANGE' ? 'selected' : ''}>Theo khoảng lương và ngưỡng chuyển cấp</option>
                                <option value="WITHIN_STANDARD" ${matrix.salaryCondition == 'WITHIN_STANDARD' ? 'selected' : ''}>Trong hạn mức thông thường</option>
                                <option value="EXCEED_STANDARD" ${matrix.salaryCondition == 'EXCEED_STANDARD' ? 'selected' : ''}>Vượt hạn mức thông thường (Vượt chuẩn)</option>
                            </select>
                        </div>

                        <!-- Lương tối thiểu (1 dòng riêng) -->
                        <div class="mb-3">
                            <label class="form-label fw-semibold text-dark small">Mức lương tối thiểu (VNĐ)</label>
                            <input type="text" name="minSalary" id="minSalary" class="form-control currency-input" 
                                   placeholder="0" value="<fmt:formatNumber value="${matrix.minSalary}" pattern="#,###" />" 
                                   oninput="formatCurrencyInput(this); updateLiveSimulation();" />
                            <div class="form-text text-xs">Mức lương sàn bắt đầu áp dụng ma trận phê duyệt này.</div>
                        </div>

                        <!-- Lương tối đa (1 dòng riêng) -->
                        <div class="mb-3">
                            <label class="form-label fw-semibold text-dark small">Mức lương tối đa (VNĐ)</label>
                            <input type="text" name="maxSalary" id="maxSalary" class="form-control currency-input" 
                                   placeholder="Không giới hạn" value="<fmt:formatNumber value="${matrix.maxSalary}" pattern="#,###" />" 
                                   oninput="formatCurrencyInput(this); updateLiveSimulation();" />
                            <div class="form-text text-xs">Mức lương trần áp dụng cấu hình (để trống nếu không giới hạn).</div>
                        </div>

                        <!-- Ngưỡng kích hoạt cấp vượt hạn mức (1 dòng riêng) -->
                        <div class="mb-3 p-3 bg-light rounded border">
                            <label class="form-label fw-semibold text-dark small d-flex align-items-center">
                                <i class="bi bi-arrow-up-circle-fill text-danger me-2"></i>
                                Ngưỡng kích hoạt cấp vượt hạn mức (VNĐ)
                            </label>
                            <input type="text" name="thresholdAmount" id="thresholdAmount" class="form-control currency-input bg-white" 
                                   placeholder="VD: 35.000.000" value="<fmt:formatNumber value="${matrix.thresholdAmount}" pattern="#,###" />" 
                                   oninput="formatCurrencyInput(this); updateLiveSimulation();" />
                            <div class="form-text text-danger text-xs mt-1">
                                Khi mức lương đề xuất lớn hơn ngưỡng này, hệ thống sẽ tự động bổ sung thêm các cấp phê duyệt vượt hạn mức.
                            </div>
                        </div>

                        <!-- Độ ưu tiên (1 dòng riêng) -->
                        <div class="mb-3">
                            <label class="form-label fw-semibold text-dark small">Độ ưu tiên áp dụng</label>
                            <input type="number" name="priority" class="form-control" value="${matrix.priority}" min="0" max="100" />
                            <div class="form-text text-xs">Giá trị số càng lớn thì cấu hình càng được ưu tiên đối chiếu trước.</div>
                        </div>

                        <!-- Trạng thái áp dụng (1 dòng riêng) -->
                        <div class="mb-3">
                            <label class="form-label fw-semibold text-dark small">Trạng thái áp dụng</label>
                            <select name="status" class="form-select">
                                <option value="ACTIVE" ${matrix.status == 'ACTIVE' ? 'selected' : ''}>Đang áp dụng</option>
                                <option value="INACTIVE" ${matrix.status == 'INACTIVE' ? 'selected' : ''}>Tạm dừng</option>
                            </select>
                        </div>

                        <!-- Mô tả / Ghi chú (1 dòng riêng) -->
                        <div class="mb-0">
                            <label class="form-label fw-semibold text-dark small">Mô tả và ghi chú quy tắc</label>
                            <textarea name="description" class="form-control" rows="3" placeholder="Nhập ghi chú chi tiết về quy tắc phê duyệt...">${matrix.description}</textarea>
                        </div>
                    </div>
                </div>

                <!-- KHỐI 2: CHUỖI CÁC CẤP PHÊ DUYỆT TUẦN TỰ -->
                <div class="card shadow-sm border mb-4">
                    <div class="card-header bg-light py-3 border-bottom d-flex flex-wrap align-items-center justify-content-between gap-2">
                        <div>
                            <h6 class="fw-bold mb-0 text-dark">
                                <i class="bi bi-diagram-3 text-primary me-2"></i>2. Chuỗi các cấp phê duyệt tuần tự
                            </h6>
                        </div>
                        <button type="button" class="btn btn-sm btn-primary" onclick="addNewStep()">
                            <i class="bi bi-plus-circle me-1"></i> Thêm cấp phê duyệt
                        </button>
                    </div>
                    <div class="card-body p-4">
                        <p class="text-muted small mb-3">
                            Khai báo thứ tự phê duyệt từ cấp cơ sở đến cấp quản lý cao nhất. Đánh dấu các cấp tự động bổ sung khi mức lương đề xuất vượt hạn mức.
                        </p>

                        <!-- Danh sách các bước động (Step Builder) -->
                        <div id="stepsContainer">
                            <c:choose>
                                <c:when test="${not empty matrix.steps}">
                                    <c:forEach var="s" items="${matrix.steps}" varStatus="status">
                                        <div class="step-card card border mb-3 p-3 ${s.escalationStep ? 'border-danger-subtle bg-danger-subtle bg-opacity-10' : 'bg-light bg-opacity-50'}">
                                            <!-- Thanh tiêu đề bước -->
                                            <div class="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom">
                                                <div class="d-flex align-items-center gap-2">
                                                    <span class="badge ${s.escalationStep ? 'bg-danger' : 'bg-primary'} rounded-pill step-order-badge px-3 py-1.5 fs-7">
                                                        Cấp ${s.stepOrder}
                                                    </span>
                                                    <span class="fw-bold text-dark step-title-preview">${s.stepName}</span>
                                                </div>
                                                <div class="d-flex align-items-center gap-1">
                                                    <button type="button" class="btn btn-sm btn-outline-secondary p-1 px-2" onclick="moveStep(this, -1)" title="Di chuyển lên">
                                                        <i class="bi bi-arrow-up"></i>
                                                    </button>
                                                    <button type="button" class="btn btn-sm btn-outline-secondary p-1 px-2" onclick="moveStep(this, 1)" title="Di chuyển xuống">
                                                        <i class="bi bi-arrow-down"></i>
                                                    </button>
                                                    <button type="button" class="btn btn-sm btn-outline-danger p-1 px-2" onclick="removeStep(this)" title="Xóa cấp này">
                                                        <i class="bi bi-trash"></i>
                                                    </button>
                                                </div>
                                            </div>

                                            <!-- CÁC THANH INPUT: MỖI CÁI Ở 1 DÒNG RIÊNG BIỆT (KHÔNG BỊ CHEN CHÚC HAY ĐÈ NHAU) -->
                                            <!-- Dòng 1: Tên cấp phê duyệt -->
                                            <div class="mb-3">
                                                <label class="form-label text-xs fw-semibold text-dark mb-1">Tên cấp phê duyệt <span class="text-danger">*</span></label>
                                                <input type="text" name="stepName[]" class="form-control" 
                                                       placeholder="VD: Trưởng bộ phận chuyên môn, Giám đốc khối..." value="${s.stepName}" required oninput="updateLiveSimulation()" />
                                            </div>

                                            <!-- Dòng 2: Loại người phê duyệt -->
                                            <div class="mb-3">
                                                <label class="form-label text-xs fw-semibold text-dark mb-1">Loại người phê duyệt <span class="text-danger">*</span></label>
                                                <select name="approverType[]" class="form-select" onchange="toggleApproverSelect(this); updateLiveSimulation();">
                                                    <option value="DEPT_MANAGER" ${s.approverType == 'DEPT_MANAGER' ? 'selected' : ''}>Trưởng phòng ban của yêu cầu tuyển dụng</option>
                                                    <option value="ROLE" ${s.approverType == 'ROLE' ? 'selected' : ''}>Theo vai trò phân quyền trong hệ thống</option>
                                                    <option value="SPECIFIC_USER" ${s.approverType == 'SPECIFIC_USER' ? 'selected' : ''}>Chỉ định đích danh tài khoản người duyệt</option>
                                                </select>
                                            </div>

                                            <!-- Dòng 3a: Chọn vai trò (hiển thị khi chọn Theo vai trò) -->
                                            <div class="mb-3 approver-role-col" style="${s.approverType == 'ROLE' ? '' : 'display:none;'}">
                                                <label class="form-label text-xs fw-semibold text-dark mb-1">Chọn vai trò phê duyệt <span class="text-danger">*</span></label>
                                                <select name="approverRoleId[]" class="form-select" onchange="updateLiveSimulation()">
                                                    <c:forEach var="r" items="${roles}">
                                                        <option value="${r.id}" ${s.approverRoleId == r.id ? 'selected' : ''}>${r.name}</option>
                                                    </c:forEach>
                                                </select>
                                            </div>

                                            <!-- Dòng 3b: Chọn người dùng (hiển thị khi chọn Chỉ định đích danh) -->
                                            <div class="mb-3 approver-user-col" style="${s.approverType == 'SPECIFIC_USER' ? '' : 'display:none;'}">
                                                <label class="form-label text-xs fw-semibold text-dark mb-1">Chọn tài khoản người duyệt <span class="text-danger">*</span></label>
                                                <select name="approverUserId[]" class="form-select" onchange="updateLiveSimulation()">
                                                    <c:forEach var="u" items="${potentialUsers}">
                                                        <option value="${u.id}" ${s.approverUserId == u.id ? 'selected' : ''}>${u.fullName} (${u.email})</option>
                                                    </c:forEach>
                                                </select>
                                            </div>

                                            <!-- Dòng 4: Cấp tự động bổ sung khi vượt hạn mức -->
                                            <div class="pt-2 border-top">
                                                <div class="form-check form-switch mb-2">
                                                    <input class="form-check-input" type="checkbox" role="switch" 
                                                           id="escCheck_${status.index}" ${s.escalationStep ? 'checked' : ''} 
                                                           onchange="toggleEscalation(this); updateLiveSimulation();" />
                                                    <label class="form-check-label text-xs fw-semibold ${s.escalationStep ? 'text-danger' : 'text-secondary'}" for="escCheck_${status.index}">
                                                        <i class="bi bi-arrow-up-circle me-1"></i> Đây là cấp tự động bổ sung khi mức lương đề xuất vượt hạn mức
                                                    </label>
                                                    <input type="hidden" name="isEscalationStep[]" value="${s.escalationStep ? 'true' : 'false'}" />
                                                </div>
                                                <div class="escalation-note-wrapper" style="${s.escalationStep ? '' : 'display:none;'}">
                                                    <input type="text" name="escalationCondition[]" class="form-control text-danger text-xs" 
                                                           placeholder="Ghi chú điều kiện bổ sung khi vượt hạn mức..." value="${s.escalationCondition}" />
                                                </div>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </c:when>
                            </c:choose>
                        </div>
                    </div>
                </div>

                <!-- KHỐI 3: MÔ PHỎNG LUỒNG PHÊ DUYỆT THỜI GIAN THỰC (XEM TRƯỚC) -->
                <div class="card bg-white border border-primary-subtle shadow-sm mb-4">
                    <div class="card-header bg-primary-subtle py-2 px-3 border-bottom border-primary-subtle">
                        <span class="fw-bold text-primary small">
                            <i class="bi bi-eye-fill me-1"></i> Mô phỏng chuỗi phê duyệt thực tế (Xem trước trực tiếp)
                        </span>
                    </div>
                    <div class="card-body p-3" id="liveSimulationBox">
                        <div class="mb-3">
                            <span class="badge bg-success-subtle text-success border border-success-subtle me-1">Trường hợp 1</span>
                            <strong class="small text-dark">Mức lương đề xuất trong hạn mức:</strong>
                            <div class="mt-2 ps-3 border-start border-3 border-success py-1" id="simNormalChain">
                                <span class="text-muted small">Đang tính toán...</span>
                            </div>
                        </div>
                        <div class="pt-2 border-top">
                            <span class="badge bg-danger-subtle text-danger border border-danger-subtle me-1">Trường hợp 2</span>
                            <strong class="small text-dark">Mức lương đề xuất vượt hạn mức:</strong>
                            <div class="mt-2 ps-3 border-start border-3 border-danger py-1" id="simEscalatedChain">
                                <span class="text-muted small">Đang tính toán...</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- NÚT LƯU VÀ HỦY THAO TÁC -->
                <div class="d-flex flex-wrap align-items-center justify-content-end gap-2 pt-2 mb-4">
                    <a href="${pageContext.request.contextPath}/admin/approval-matrix" class="btn btn-outline-secondary px-3">
                        Hủy bỏ
                    </a>
                    <button type="submit" id="submitBtn" class="btn btn-primary px-4 shadow-sm">
                        <i class="bi bi-check-circle-fill me-1"></i> ${isEdit ? 'Lưu thay đổi cấu hình' : 'Lưu cấu hình ma trận'}
                    </button>
                </div>

            </div>
        </form>
    </div>

<!-- Template mẫu để thêm cấp phê duyệt mới qua JavaScript -->
<div id="stepTemplate" style="display:none;">
    <div class="step-card card border mb-3 p-3 bg-light bg-opacity-50">
        <!-- Thanh tiêu đề bước -->
        <div class="d-flex align-items-center justify-content-between mb-3 pb-2 border-bottom">
            <div class="d-flex align-items-center gap-2">
                <span class="badge bg-primary rounded-pill step-order-badge px-3 py-1.5 fs-7">
                    Cấp {ORDER}
                </span>
                <span class="fw-bold text-dark step-title-preview">Cấp phê duyệt mới</span>
            </div>
            <div class="d-flex align-items-center gap-1">
                <button type="button" class="btn btn-sm btn-outline-secondary p-1 px-2" onclick="moveStep(this, -1)" title="Di chuyển lên">
                    <i class="bi bi-arrow-up"></i>
                </button>
                <button type="button" class="btn btn-sm btn-outline-secondary p-1 px-2" onclick="moveStep(this, 1)" title="Di chuyển xuống">
                    <i class="bi bi-arrow-down"></i>
                </button>
                <button type="button" class="btn btn-sm btn-outline-danger p-1 px-2" onclick="removeStep(this)" title="Xóa cấp này">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        </div>

        <!-- CÁC THANH INPUT: MỖI CÁI Ở 1 DÒNG RIÊNG BIỆT -->
        <!-- Dòng 1: Tên cấp phê duyệt -->
        <div class="mb-3">
            <label class="form-label text-xs fw-semibold text-dark mb-1">Tên cấp phê duyệt <span class="text-danger">*</span></label>
            <input type="text" name="stepName[]" class="form-control" 
                   placeholder="VD: Trưởng bộ phận chuyên môn, Giám đốc..." value="Cấp phê duyệt {ORDER}" required oninput="updateLiveSimulation()" />
        </div>

        <!-- Dòng 2: Loại người phê duyệt -->
        <div class="mb-3">
            <label class="form-label text-xs fw-semibold text-dark mb-1">Loại người phê duyệt <span class="text-danger">*</span></label>
            <select name="approverType[]" class="form-select" onchange="toggleApproverSelect(this); updateLiveSimulation();">
                <option value="DEPT_MANAGER">Trưởng phòng ban của yêu cầu tuyển dụng</option>
                <option value="ROLE">Theo vai trò phân quyền trong hệ thống</option>
                <option value="SPECIFIC_USER">Chỉ định đích danh tài khoản người duyệt</option>
            </select>
        </div>

        <!-- Dòng 3a: Chọn vai trò -->
        <div class="mb-3 approver-role-col" style="display:none;">
            <label class="form-label text-xs fw-semibold text-dark mb-1">Chọn vai trò phê duyệt <span class="text-danger">*</span></label>
            <select name="approverRoleId[]" class="form-select" onchange="updateLiveSimulation()">
                <c:forEach var="r" items="${roles}">
                    <option value="${r.id}">${r.name}</option>
                </c:forEach>
            </select>
        </div>

        <!-- Dòng 3b: Chọn người dùng -->
        <div class="mb-3 approver-user-col" style="display:none;">
            <label class="form-label text-xs fw-semibold text-dark mb-1">Chọn tài khoản người duyệt <span class="text-danger">*</span></label>
            <select name="approverUserId[]" class="form-select" onchange="updateLiveSimulation()">
                <c:forEach var="u" items="${potentialUsers}">
                    <option value="${u.id}">${u.fullName} (${u.email})</option>
                </c:forEach>
            </select>
        </div>

        <!-- Dòng 4: Cấp tự động bổ sung khi vượt hạn mức -->
        <div class="pt-2 border-top">
            <div class="form-check form-switch mb-2">
                <input class="form-check-input" type="checkbox" role="switch" onchange="toggleEscalation(this); updateLiveSimulation();" />
                <label class="form-check-label text-xs fw-semibold text-secondary">
                    <i class="bi bi-arrow-up-circle me-1"></i> Đây là cấp tự động bổ sung khi mức lương đề xuất vượt hạn mức
                </label>
                <input type="hidden" name="isEscalationStep[]" value="false" />
            </div>
            <div class="escalation-note-wrapper" style="display:none;">
                <input type="text" name="escalationCondition[]" class="form-control text-danger text-xs" 
                       placeholder="Ghi chú điều kiện bổ sung khi vượt hạn mức..." value="Tự động bổ sung khi mức lương đề xuất vượt hạn mức thông thường" />
            </div>
        </div>
    </div>
</div>

<script>
function formatCurrencyInput(input) {
    var val = input.value.replace(/\D/g, '');
    if (val === '') {
        input.value = '';
        return;
    }
    input.value = new Intl.NumberFormat('vi-VN').format(val);
}

function toggleSalaryInputs() {
    var cond = document.getElementById('salaryCondition').value;
    var threshInput = document.getElementById('thresholdAmount');
    if (cond === 'WITHIN_STANDARD' && !threshInput.value) {
        threshInput.placeholder = 'Nhập hạn mức chuẩn tối đa...';
    }
}

function toggleApproverSelect(selectElem) {
    var card = selectElem.closest('.step-card');
    var roleCol = card.querySelector('.approver-role-col');
    var userCol = card.querySelector('.approver-user-col');
    if (selectElem.value === 'ROLE') {
        roleCol.style.display = 'block';
        userCol.style.display = 'none';
    } else if (selectElem.value === 'SPECIFIC_USER') {
        roleCol.style.display = 'none';
        userCol.style.display = 'block';
    } else {
        roleCol.style.display = 'none';
        userCol.style.display = 'none';
    }
}

function toggleEscalation(checkbox) {
    var card = checkbox.closest('.step-card');
    var hidden = checkbox.parentElement.querySelector('input[type="hidden"]');
    var noteWrapper = card.querySelector('.escalation-note-wrapper');
    var badge = card.querySelector('.step-order-badge');
    var label = checkbox.parentElement.querySelector('label');

    if (checkbox.checked) {
        hidden.value = 'true';
        noteWrapper.style.display = 'block';
        card.classList.remove('bg-light');
        card.classList.add('border-danger-subtle', 'bg-danger-subtle', 'bg-opacity-10');
        badge.classList.remove('bg-primary');
        badge.classList.add('bg-danger');
        label.classList.remove('text-secondary');
        label.classList.add('text-danger');
    } else {
        hidden.value = 'false';
        noteWrapper.style.display = 'none';
        card.classList.remove('border-danger-subtle', 'bg-danger-subtle', 'bg-opacity-10');
        card.classList.add('bg-light');
        badge.classList.remove('bg-danger');
        badge.classList.add('bg-primary');
        label.classList.remove('text-danger');
        label.classList.add('text-secondary');
    }
}

function renumberSteps() {
    var container = document.getElementById('stepsContainer');
    var cards = container.querySelectorAll('.step-card');
    cards.forEach(function(card, index) {
        var order = index + 1;
        var badge = card.querySelector('.step-order-badge');
        badge.innerText = 'Cấp ' + order;
    });
}

function addNewStep() {
    var container = document.getElementById('stepsContainer');
    var count = container.querySelectorAll('.step-card').length + 1;
    var tpl = document.getElementById('stepTemplate').innerHTML;
    tpl = tpl.replace(/{ORDER}/g, count);
    var div = document.createElement('div');
    div.innerHTML = tpl;
    var newCard = div.firstElementChild;
    container.appendChild(newCard);
    renumberSteps();
    updateLiveSimulation();
}

function removeStep(btn) {
    var card = btn.closest('.step-card');
    var container = document.getElementById('stepsContainer');
    if (container.querySelectorAll('.step-card').length <= 1) {
        alert('Ma trận phê duyệt phải có ít nhất 1 cấp phê duyệt bắt buộc!');
        return;
    }
    card.remove();
    renumberSteps();
    updateLiveSimulation();
}

function moveStep(btn, direction) {
    var card = btn.closest('.step-card');
    var container = document.getElementById('stepsContainer');
    if (direction === -1 && card.previousElementSibling) {
        container.insertBefore(card, card.previousElementSibling);
    } else if (direction === 1 && card.nextElementSibling) {
        container.insertBefore(card.nextElementSibling, card);
    }
    renumberSteps();
    updateLiveSimulation();
}

function updateLiveSimulation() {
    var container = document.getElementById('stepsContainer');
    var cards = container.querySelectorAll('.step-card');
    var normalSteps = [];
    var escalatedSteps = [];

    cards.forEach(function(card, idx) {
        var nameInput = card.querySelector('input[name="stepName[]"]');
        var name = (nameInput && nameInput.value.trim()) ? nameInput.value.trim() : ('Cấp ' + (idx + 1));
        var isEsc = card.querySelector('input[name="isEscalationStep[]"]').value === 'true';
        if (isEsc) {
            escalatedSteps.push(name);
        } else {
            normalSteps.push(name);
        }
    });

    var normalHtml = '';
    if (normalSteps.length > 0) {
        normalHtml = normalSteps.map(function(s, i) {
            return '<span class="badge bg-light text-dark border me-1 px-2 py-1">Cấp ' + (i + 1) + ': ' + s + '</span>';
        }).join('<i class="bi bi-arrow-right text-muted me-1"></i>');
    } else {
        normalHtml = '<span class="text-danger small">Thiếu cấp phê duyệt thông thường!</span>';
    }
    document.getElementById('simNormalChain').innerHTML = normalHtml;

    var fullEscalated = normalSteps.concat(escalatedSteps);
    var escHtml = '';
    if (fullEscalated.length > 0) {
        escHtml = fullEscalated.map(function(s, i) {
            var isAdded = i >= normalSteps.length;
            var badgeClass = isAdded ? 'bg-danger text-white' : 'bg-light text-dark border';
            return '<span class="badge ' + badgeClass + ' me-1 px-2 py-1">Cấp ' + (i + 1) + ': ' + s + (isAdded ? ' (Vượt hạn mức)' : '') + '</span>';
        }).join('<i class="bi bi-arrow-right text-muted me-1"></i>');
    } else {
        escHtml = '<span class="text-muted small">Chưa cấu hình cấp vượt hạn mức.</span>';
    }
    document.getElementById('simEscalatedChain').innerHTML = escHtml;
}

// Chống click nhiều lần
document.getElementById('matrixForm').addEventListener('submit', function(e) {
    var btn = document.getElementById('submitBtn');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status"></span> Đang lưu cấu hình...';
});

// Khởi chạy khi tải trang
document.addEventListener('DOMContentLoaded', function() {
    updateLiveSimulation();
});
</script>

<jsp:include page="../common/footer.jsp" />

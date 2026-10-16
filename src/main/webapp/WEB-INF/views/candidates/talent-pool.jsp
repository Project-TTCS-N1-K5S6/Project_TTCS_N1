<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<c:set var="pageTitle" value="Kho ứng viên tiềm năng (Talent Pool)" />
<c:set var="breadcrumb" value="Kho ứng viên tiềm năng" />
<c:set var="activeMenu" value="talent-pool" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Header phân hệ -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <div class="d-flex align-items-center gap-2">
                    <h3 class="fw-bold text-dark mb-0">Kho ứng viên tiềm năng (Talent Pool)</h3>
                    <span class="badge bg-warning text-dark border border-warning-subtle fw-semibold px-2 py-1">
                        <i class="bi bi-stars me-1"></i>Talent Pool
                    </span>
                </div>
                <div class="text-muted small mt-1">
                    Lưu trữ, xếp hạng mức độ tiềm năng, phân loại kỹ năng chuyên môn và tái điều động ứng viên vào các vị trí tuyển dụng mới.
                </div>
            </div>
            <div class="d-flex gap-2">
                <a href="${pageContext.request.contextPath}/candidates" class="btn btn-outline-secondary">
                    <i class="bi bi-people me-1"></i> Danh sách ứng viên
                </a>
                <button type="button" class="btn-ris-primary" data-bs-toggle="modal" data-bs-target="#createPotentialModal">
                    <i class="bi bi-person-plus-fill me-1"></i> Thêm ứng viên tiềm năng
                </button>
            </div>
        </div>

        <!-- KPI Summary Cards -->
        <div class="row g-3 mb-4">
            <div class="col-12 col-sm-6 col-xl-3">
                <div class="card border-0 shadow-sm rounded-3 h-100">
                    <div class="card-body p-3 d-flex align-items-center">
                        <div class="rounded-3 p-3 bg-primary-subtle text-primary me-3 fs-3">
                            <i class="bi bi-collection-fill"></i>
                        </div>
                        <div>
                            <div class="text-muted small fw-semibold">Tổng hồ sơ trong kho</div>
                            <div class="fs-4 fw-bold text-dark">${stats[0]}</div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-12 col-sm-6 col-xl-3">
                <div class="card border-0 shadow-sm rounded-3 h-100">
                    <div class="card-body p-3 d-flex align-items-center">
                        <div class="rounded-3 p-3 bg-warning-subtle text-warning me-3 fs-3">
                            <i class="bi bi-star-fill"></i>
                        </div>
                        <div>
                            <div class="text-muted small fw-semibold">Tiềm năng cao (4-5 ⭐)</div>
                            <div class="fs-4 fw-bold text-dark">${stats[1]}</div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-12 col-sm-6 col-xl-3">
                <div class="card border-0 shadow-sm rounded-3 h-100">
                    <div class="card-body p-3 d-flex align-items-center">
                        <div class="rounded-3 p-3 bg-success-subtle text-success me-3 fs-3">
                            <i class="bi bi-person-check-fill"></i>
                        </div>
                        <div>
                            <div class="text-muted small fw-semibold">Sẵn sàng điều động</div>
                            <div class="fs-4 fw-bold text-dark">${stats[2]}</div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="col-12 col-sm-6 col-xl-3">
                <div class="card border-0 shadow-sm rounded-3 h-100">
                    <div class="card-body p-3 d-flex align-items-center">
                        <div class="rounded-3 p-3 bg-info-subtle text-info me-3 fs-3">
                            <i class="bi bi-clock-history"></i>
                        </div>
                        <div>
                            <div class="text-muted small fw-semibold">Thêm trong 30 ngày</div>
                            <div class="fs-4 fw-bold text-dark">${stats[3]}</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Bộ lọc và tìm kiếm -->
        <div class="card border-0 shadow-sm mb-4">
            <div class="card-body p-3">
                <form action="${pageContext.request.contextPath}/candidates/talent-pool" method="GET" class="row g-2 align-items-center">
                    <div class="col-12 col-md-3">
                        <div class="input-group">
                            <span class="input-group-text bg-white border-end-0 text-muted"><i class="bi bi-search"></i></span>
                            <input type="text" name="search" class="form-control border-start-0 ps-0"
                                   placeholder="Họ tên, email, SĐT..." value="<c:out value='${paramSearch}' />">
                        </div>
                    </div>
                    <div class="col-12 col-sm-6 col-md-2">
                        <div class="input-group">
                            <span class="input-group-text bg-white border-end-0 text-muted"><i class="bi bi-briefcase"></i></span>
                            <input type="text" name="targetPosition" class="form-control border-start-0 ps-0"
                                   placeholder="Chức danh quan tâm..." value="<c:out value='${paramPosition}' />">
                        </div>
                    </div>
                    <div class="col-12 col-sm-6 col-md-2">
                        <div class="input-group">
                            <span class="input-group-text bg-white border-end-0 text-muted"><i class="bi bi-tags"></i></span>
                            <input type="text" name="skill" class="form-control border-start-0 ps-0"
                                   placeholder="Kỹ năng..." value="<c:out value='${paramSkill}' />">
                        </div>
                    </div>
                    <div class="col-12 col-sm-6 col-md-2">
                        <select name="minRating" class="form-select">
                            <option value="">Tất cả đánh giá</option>
                            <option value="5" ${paramMinRating == 5 ? 'selected' : ''}>⭐⭐⭐⭐⭐ (5 sao)</option>
                            <option value="4" ${paramMinRating == 4 ? 'selected' : ''}>⭐⭐⭐⭐ (Từ 4 sao)</option>
                            <option value="3" ${paramMinRating == 3 ? 'selected' : ''}>⭐⭐⭐ (Từ 3 sao)</option>
                        </select>
                    </div>
                    <div class="col-12 col-sm-6 col-md-2">
                        <select name="departmentId" class="form-select">
                            <option value="">Phòng ban</option>
                            <c:forEach var="dept" items="${departments}">
                                <option value="${dept.id}" ${paramDepartmentId == dept.id ? 'selected' : ''}>
                                    <c:out value="${dept.name}" />
                                </option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-12 col-md-1 d-flex gap-1">
                        <button type="submit" class="btn btn-primary w-100 px-2" title="Tìm kiếm &amp; Lọc">
                            <i class="bi bi-funnel"></i>
                        </button>
                        <c:if test="${not empty paramSearch or not empty paramPosition or not empty paramMinRating or not empty paramDepartmentId or not empty paramSkill}">
                            <a href="${pageContext.request.contextPath}/candidates/talent-pool" class="btn btn-outline-secondary px-2" title="Đặt lại bộ lọc">
                                <i class="bi bi-arrow-counterclockwise"></i>
                            </a>
                        </c:if>
                    </div>
                    <!-- Quick Skill Pill Filters -->
                    <div class="col-12 mt-2 pt-2 border-top d-flex flex-wrap align-items-center gap-1">
                        <span class="small text-muted me-2"><i class="bi bi-tags me-1"></i>Kỹ năng nhanh:</span>
                        <c:forEach var="sk" items="${['Java', 'Spring', 'React', 'NodeJS', 'Python', 'DevOps', 'QA', 'UI/UX']}">
                            <a href="${pageContext.request.contextPath}/candidates/talent-pool?skill=${sk}"
                               class="badge ${paramSkill == sk ? 'bg-primary text-white' : 'bg-light text-dark border'} text-decoration-none px-2 py-1">
                                ${sk}
                            </a>
                        </c:forEach>
                    </div>
                </form>
            </div>
        </div>

        <!-- Danh sách Ứng viên trong Kho tiềm năng -->
        <div class="card border-0 shadow-sm">
            <div class="card-header bg-white py-3 d-flex align-items-center justify-content-between">
                <div class="d-flex align-items-center gap-2">
                    <h5 class="fw-bold mb-0">Danh sách nhân tài dự phòng</h5>
                    <span class="badge bg-primary rounded-pill">${totalCount} hồ sơ</span>
                </div>
            </div>
            <div class="table-responsive">
                <table class="table align-middle table-hover mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-4">Ứng viên</th>
                            <th>Mức tiềm năng</th>
                            <th>Kỹ năng nổi bật</th>
                            <th>Định hướng phù hợp</th>
                            <th>Ghi chú & Đánh giá</th>
                            <th>Ngày lưu kho</th>
                            <th class="pe-4 text-end">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:choose>
                            <c:when test="${empty talentPool}">
                                <tr>
                                    <td colspan="7" class="text-center py-5 text-muted">
                                        <div class="py-4">
                                            <i class="bi bi-inbox fs-1 text-muted d-block mb-2"></i>
                                            <div class="fw-semibold">Chưa có ứng viên nào trong Kho tiềm năng.</div>
                                            <div class="small">Bạn có thể thêm từ danh sách ứng viên thông thường hoặc tạo mới tại đây.</div>
                                        </div>
                                    </td>
                                </tr>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="cand" items="${talentPool}">
                                    <tr>
                                        <!-- Cột 1: Thông tin ứng viên -->
                                        <td class="ps-4">
                                            <div class="d-flex align-items-center">
                                                <div class="avatar-circle rounded-circle bg-primary-subtle text-primary fw-bold d-flex align-items-center justify-content-center me-3"
                                                     style="width: 42px; height: 42px; font-size: 1rem;">
                                                    <c:out value="${fn:substring(cand.fullName, 0, 1)}" />
                                                </div>
                                                <div>
                                                    <div class="fw-bold text-dark">
                                                        <c:out value="${cand.fullName}" />
                                                        <c:if test="${not empty cand.cvUrl}">
                                                            <a href="${cand.cvUrl}" target="_blank" class="ms-1 text-primary small text-decoration-none" title="Xem CV">
                                                                <i class="bi bi-file-earmark-pdf-fill"></i>
                                                            </a>
                                                        </c:if>
                                                    </div>
                                                    <div class="small text-muted"><i class="bi bi-envelope me-1"></i><c:out value="${cand.email}" /></div>
                                                    <c:if test="${not empty cand.phone}">
                                                        <div class="small text-muted"><i class="bi bi-telephone me-1"></i><c:out value="${cand.phone}" /></div>
                                                    </c:if>
                                                </div>
                                            </div>
                                        </td>

                                        <!-- Cột 2: Đánh giá tiềm năng -->
                                        <td>
                                            <div class="d-flex align-items-center">
                                                <div class="text-warning me-2">
                                                    <c:forEach begin="1" end="${cand.potentialRating}">
                                                        <i class="bi bi-star-fill"></i>
                                                    </c:forEach>
                                                    <c:forEach begin="${cand.potentialRating + 1}" end="5">
                                                        <i class="bi bi-star text-muted opacity-50"></i>
                                                    </c:forEach>
                                                </div>
                                                <span class="badge ${cand.getPotentialRatingBadgeClass()} font-monospace">
                                                    ${cand.potentialRating}/5
                                                </span>
                                            </div>
                                        </td>

                                        <!-- Cột 3: Kỹ năng nổi bật -->
                                        <td style="max-width: 200px;">
                                            <c:choose>
                                                <c:when test="${not empty cand.skillsArray}">
                                                    <div class="d-flex flex-wrap gap-1">
                                                        <c:forEach var="skillItem" items="${cand.skillsArray}">
                                                            <span class="badge bg-light text-dark border font-monospace" style="font-size: 0.72rem;">
                                                                <c:out value="${skillItem}" />
                                                            </span>
                                                        </c:forEach>
                                                    </div>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="text-muted small">Chưa gắn tag</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>

                                        <!-- Cột 4: Định hướng phù hợp -->
                                        <td>
                                            <div class="fw-semibold text-dark small">
                                                <c:out value="${not empty cand.targetPosition ? cand.targetPosition : 'Bất kỳ vị trí phù hợp'}" />
                                            </div>
                                            <div class="text-muted small">
                                                <i class="bi bi-building me-1"></i>
                                                <c:out value="${not empty cand.targetDepartmentName ? cand.targetDepartmentName : 'Toàn công ty'}" />
                                            </div>
                                        </td>

                                        <!-- Cột 5: Ghi chú đánh giá -->
                                        <td style="max-width: 220px;">
                                            <div class="text-truncate text-muted small" title="<c:out value='${cand.potentialNotes}' />">
                                                <c:out value="${cand.potentialNotes != null ? cand.potentialNotes : '—'}" />
                                            </div>
                                            <c:if test="${not empty cand.addedToPoolByName}">
                                                <div class="small text-muted opacity-75" style="font-size: 0.75rem;">
                                                    Bởi: <c:out value="${cand.addedToPoolByName}" />
                                                </div>
                                            </c:if>
                                        </td>

                                        <!-- Cột 6: Ngày lưu kho -->
                                        <td class="text-muted small">
                                            <fmt:formatDate value="${cand.addedToPoolAt}" pattern="dd/MM/yyyy HH:mm" />
                                        </td>

                                        <!-- Cột 7: Thao tác -->
                                        <td class="pe-4 text-end">
                                            <div class="btn-group">
                                                <!-- Nút Tái điều động (Gán vị trí) -->
                                                <button type="button" class="btn btn-sm btn-outline-success"
                                                        title="Tái điều động: Gán vào vị trí đang tuyển"
                                                        onclick="openAssignModal('${cand.id}', '${cand.fullName}')">
                                                    <i class="bi bi-arrow-right-circle-fill me-1"></i> Điều động
                                                </button>
                                                <!-- Nút Sửa thông tin tiềm năng -->
                                                <button type="button" class="btn btn-sm btn-outline-primary"
                                                        title="Sửa đánh giá & Kỹ năng"
                                                        onclick="openEditModal('${cand.id}', '${cand.fullName}', '${cand.potentialRating}', '${cand.potentialSkills}', '${cand.potentialNotes}', '${cand.targetDepartmentId}', '${cand.targetPosition}')">
                                                    <i class="bi bi-pencil-square"></i>
                                                </button>
                                                <!-- Nút Loại khỏi kho -->
                                                <form action="${pageContext.request.contextPath}/candidates/talent-pool/remove" method="POST"
                                                      onsubmit="return confirm('Bạn có chắc muốn loại ứng viên này khỏi Kho tiềm năng?');" class="d-inline">
                                                    <input type="hidden" name="candidateId" value="${cand.id}">
                                                    <button type="submit" class="btn btn-sm btn-outline-danger" title="Loại khỏi kho tiềm năng">
                                                        <i class="bi bi-trash"></i>
                                                    </button>
                                                </form>
                                            </div>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>
                    </tbody>
                </table>
            </div>

            <!-- Phân trang -->
            <c:if test="${totalPages > 1}">
                <div class="card-footer bg-white py-3 d-flex justify-content-between align-items-center">
                    <div class="small text-muted">
                        Trang <strong>${currentPage}</strong> trên <strong>${totalPages}</strong> (${totalCount} kết quả)
                    </div>
                    <nav>
                        <ul class="pagination pagination-sm mb-0">
                            <li class="page-item ${currentPage <= 1 ? 'disabled' : ''}">
                                <a class="page-link" href="?page=${currentPage - 1}&search=${paramSearch}&targetPosition=${paramPosition}&minRating=${paramMinRating}&departmentId=${paramDepartmentId}&skill=${paramSkill}">Trước</a>
                            </li>
                            <c:forEach begin="1" end="${totalPages}" var="p">
                                <li class="page-item ${p == currentPage ? 'active' : ''}">
                                    <a class="page-link" href="?page=${p}&search=${paramSearch}&targetPosition=${paramPosition}&minRating=${paramMinRating}&departmentId=${paramDepartmentId}&skill=${paramSkill}">${p}</a>
                                </li>
                            </c:forEach>
                            <li class="page-item ${currentPage >= totalPages ? 'disabled' : ''}">
                                <a class="page-link" href="?page=${currentPage + 1}&search=${paramSearch}&targetPosition=${paramPosition}&minRating=${paramMinRating}&departmentId=${paramDepartmentId}&skill=${paramSkill}">Sau</a>
                            </li>
                        </ul>
                    </nav>
                </div>
            </c:if>
        </div>
    </div>
</main>

<!-- ========================================================================= -->
<!-- MODAL 1: THÊM MỚI ỨNG VIÊN TIỀM NĂNG TRỰC TIẾP -->
<!-- ========================================================================= -->
<div class="modal fade" id="createPotentialModal" tabindex="-1" aria-labelledby="createPotentialModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content border-0 shadow">
            <form action="${pageContext.request.contextPath}/candidates/talent-pool/add" method="POST">
                <div class="modal-header bg-primary text-white">
                    <h5 class="modal-title fw-bold" id="createPotentialModalLabel">
                        <i class="bi bi-person-plus-fill me-2"></i>Thêm ứng viên vào Kho tiềm năng
                    </h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="row g-3">
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">Họ và tên ứng viên <span class="text-danger">*</span></label>
                            <input type="text" name="fullName" class="form-control" placeholder="Nguyễn Văn A" required>
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">Email liên hệ <span class="text-danger">*</span></label>
                            <input type="email" name="email" class="form-control" placeholder="nguyenvana@gmail.com" required>
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">Số điện thoại</label>
                            <input type="tel" name="phone" class="form-control" placeholder="0912345678">
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">Liên kết CV</label>
                            <input type="url" name="cvUrl" class="form-control" placeholder="https://...">
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">Đánh giá độ tiềm năng <span class="text-danger">*</span></label>
                            <select name="rating" class="form-select" required>
                                <option value="5">⭐⭐⭐⭐⭐ (5 sao - Xuất sắc / Ưu tiên số 1)</option>
                                <option value="4" selected>⭐⭐⭐⭐ (4 sao - Rất tiềm năng / Phù hợp cao)</option>
                                <option value="3">⭐⭐⭐ (3 sao - Tiềm năng / Đáp ứng cơ bản)</option>
                                <option value="2">⭐⭐ (2 sao - Cần đào tạo thêm)</option>
                                <option value="1">⭐ (1 sao - Khảo sát)</option>
                            </select>
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold">Phòng ban mục tiêu</label>
                            <select name="targetDepartmentId" class="form-select">
                                <option value="">Chưa xác định / Toàn công ty</option>
                                <c:forEach var="dept" items="${departments}">
                                    <option value="${dept.id}"><c:out value="${dept.name}" /></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-12">
                            <label class="form-label fw-semibold">Vị trí dự kiến phù hợp</label>
                            <input type="text" name="targetPosition" class="form-control" placeholder="VD: Senior Backend Java Developer, Tech Lead...">
                        </div>
                        <div class="col-12">
                            <label class="form-label fw-semibold">Kỹ năng nổi bật (Cách nhau bởi dấu phẩy)</label>
                            <input type="text" name="skills" class="form-control" placeholder="VD: Java, Spring Boot, Microservices, Redis, Docker">
                            <div class="form-text">Nhập các thẻ kỹ năng giúp tìm kiếm và phân loại nhanh.</div>
                        </div>
                        <div class="col-12">
                            <label class="form-label fw-semibold">Nhận xét & Ghi chú đánh giá tiềm năng</label>
                            <textarea name="notes" class="form-control" rows="3"
                                      placeholder="Lý do ứng viên được đánh giá tiềm năng, kinh nghiệm nổi bật, thái độ phỏng vấn..."></textarea>
                        </div>
                    </div>
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn-ris-primary">
                        <i class="bi bi-save me-1"></i> Lưu vào Kho tiềm năng
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- ========================================================================= -->
<!-- MODAL 2: TÁI ĐIỀU ĐỘNG / GÁN VÀO VỊ TRÍ TUYỂN DỤNG ĐANG MỞ -->
<!-- ========================================================================= -->
<div class="modal fade" id="assignRequisitionModal" tabindex="-1" aria-labelledby="assignModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content border-0 shadow">
            <form action="${pageContext.request.contextPath}/candidates/talent-pool/assign" method="POST">
                <input type="hidden" name="candidateId" id="assignCandidateId">
                <div class="modal-header bg-success text-white">
                    <h5 class="modal-title fw-bold" id="assignModalLabel">
                        <i class="bi bi-arrow-right-circle-fill me-2"></i>Tái điều động ứng viên vào vị trí mới
                    </h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="mb-3">
                        <label class="form-label fw-semibold text-muted small">ỨNG VIÊN ĐƯỢC CHỌN</label>
                        <div class="fs-5 fw-bold text-dark" id="assignCandidateName">...</div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold" for="assignRequisitionSelect">Vị trí tuyển dụng đang mở <span class="text-danger">*</span></label>
                        <select name="requisitionId" id="assignRequisitionSelect" class="form-select" required>
                            <option value="">-- Chọn yêu cầu tuyển dụng --</option>
                            <c:forEach var="req" items="${openRequisitions}">
                                <option value="${req.id}">
                                    <c:out value="${req.title}" /> (<c:out value="${req.code}" />) - <c:out value="${req.departmentName}" />
                                </option>
                            </c:forEach>
                        </select>
                        <div class="form-text">Ứng viên sẽ được đưa vào quy trình với trạng thái <strong>Sàng lọc CV (SCREENING)</strong>.</div>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold" for="assignNotesTextarea">Ghi chú điều động</label>
                        <textarea name="notes" id="assignNotesTextarea" class="form-control" rows="3"
                                  placeholder="Ghi chú lý do điều động, người đề xuất hoặc lưu ý đặc biệt cho chuyên viên tuyển dụng phụ trách..."></textarea>
                    </div>
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-success">
                        <i class="bi bi-check2-circle me-1"></i> Xác nhận điều động
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<!-- ========================================================================= -->
<!-- MODAL 3: CHỈNH SỬA THÔNG TIN ĐÁNH GIÁ & KỸ NĂNG TIỀM NĂNG -->
<!-- ========================================================================= -->
<div class="modal fade" id="editPotentialModal" tabindex="-1" aria-labelledby="editPotentialModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content border-0 shadow">
            <form action="${pageContext.request.contextPath}/candidates/talent-pool/update" method="POST">
                <input type="hidden" name="candidateId" id="editCandidateId">
                <div class="modal-header bg-primary text-white">
                    <h5 class="modal-title fw-bold" id="editPotentialModalLabel">
                        <i class="bi bi-pencil-square me-2"></i>Cập nhật đánh giá ứng viên tiềm năng
                    </h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-4">
                    <div class="mb-3">
                        <label class="form-label fw-semibold text-muted small">ỨNG VIÊN</label>
                        <div class="fs-5 fw-bold text-dark" id="editCandidateName">...</div>
                    </div>
                    <div class="row g-3">
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold" for="editRatingSelect">Mức độ tiềm năng <span class="text-danger">*</span></label>
                            <select name="rating" id="editRatingSelect" class="form-select" required>
                                <option value="5">⭐⭐⭐⭐⭐ (5 sao - Xuất sắc)</option>
                                <option value="4">⭐⭐⭐⭐ (4 sao - Rất tiềm năng)</option>
                                <option value="3">⭐⭐⭐ (3 sao - Tiềm năng)</option>
                                <option value="2">⭐⭐ (2 sao - Đạt yêu cầu)</option>
                                <option value="1">⭐ (1 sao - Dự phòng)</option>
                            </select>
                        </div>
                        <div class="col-12 col-md-6">
                            <label class="form-label fw-semibold" for="editDeptSelect">Phòng ban mục tiêu</label>
                            <select name="targetDepartmentId" id="editDeptSelect" class="form-select">
                                <option value="">Chưa xác định / Toàn công ty</option>
                                <c:forEach var="dept" items="${departments}">
                                    <option value="${dept.id}"><c:out value="${dept.name}" /></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-12">
                            <label class="form-label fw-semibold" for="editTargetPosition">Vị trí dự kiến phù hợp</label>
                            <input type="text" name="targetPosition" id="editTargetPosition" class="form-control" placeholder="VD: Senior React Native Developer">
                        </div>
                        <div class="col-12">
                            <label class="form-label fw-semibold" for="editSkills">Kỹ năng nổi bật (Cách nhau bởi dấu phẩy)</label>
                            <input type="text" name="skills" id="editSkills" class="form-control" placeholder="VD: Java, Spring Boot, Microservices">
                        </div>
                        <div class="col-12">
                            <label class="form-label fw-semibold" for="editNotes">Nhận xét & Ghi chú đánh giá</label>
                            <textarea name="notes" id="editNotes" class="form-control" rows="3"></textarea>
                        </div>
                    </div>
                </div>
                <div class="modal-footer bg-light">
                    <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn-ris-primary">
                        <i class="bi bi-save me-1"></i> Cập nhật
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
function openAssignModal(candId, candName) {
    document.getElementById('assignCandidateId').value = candId;
    document.getElementById('assignCandidateName').textContent = candName;
    new bootstrap.Modal(document.getElementById('assignRequisitionModal')).show();
}

function openEditModal(candId, candName, rating, skills, notes, deptId, targetPosition) {
    document.getElementById('editCandidateId').value = candId;
    document.getElementById('editCandidateName').textContent = candName;
    document.getElementById('editRatingSelect').value = rating || 3;
    document.getElementById('editSkills').value = skills || '';
    document.getElementById('editNotes').value = notes || '';
    document.getElementById('editDeptSelect').value = deptId || '';
    document.getElementById('editTargetPosition').value = targetPosition || '';
    new bootstrap.Modal(document.getElementById('editPotentialModal')).show();
}
</script>

<jsp:include page="../common/footer.jsp" />

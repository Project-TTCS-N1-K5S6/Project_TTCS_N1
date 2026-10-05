<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Dải lương ngân sách theo vị trí" />
<c:set var="breadcrumb" value="Dải lương ngân sách" />
<c:set var="activeMenu" value="salary" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <h4 class="fw-bold mb-1">Dải lương ngân sách theo vị trí</h4>
                <p class="text-muted small mb-0">Thông tin bảo mật mức lương tối thiểu - tối đa cho từng chức danh (Bị giới hạn với Người phỏng vấn).</p>
            </div>
            <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addSalaryModal">
                <i class="bi bi-plus-circle me-1"></i> Thiết lập dải lương
            </button>
        </div>

        <div class="card mb-4 shadow-sm border">
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-custom">
                        <thead>
                            <tr>
                                <th>Phòng ban</th>
                                <th>Vị trí / Chức danh</th>
                                <th>Dải lương ngân sách (Min - Max)</th>
                                <th>Đơn vị tiền tệ</th>
                                <th>Ngày cập nhật</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="s" items="${salaryRanges}">
                                <tr>
                                    <td><strong>${s.departmentName != null ? s.departmentName : 'Tất cả đơn vị'}</strong></td>
                                    <td><span class="fw-semibold text-dark">${s.positionTitle}</span></td>
                                    <td>
                                        <span class="badge bg-success-subtle text-success border border-success-subtle px-2 py-1 fs-6">
                                            ${s.getFormattedRange()}
                                        </span>
                                    </td>
                                    <td><span class="badge bg-light text-dark border font-monospace">${s.currency}</span></td>
                                    <td class="text-muted text-xs">
                                        <fmt:formatDate value="${s.updatedAt}" pattern="dd/MM/yyyy HH:mm" />
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty salaryRanges}">
                                <tr>
                                    <td colspan="5" class="text-center py-4 text-muted">Chưa có dữ liệu dải lương.</td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <!-- Modal Thêm Dải Lương -->
    <div class="modal fade" id="addSalaryModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/salary-ranges/create" method="POST">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold"><i class="bi bi-cash-stack text-primary me-2"></i> Thiết lập dải lương mới</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label small fw-semibold">Phòng ban</label>
                            <select class="form-select" name="departmentId">
                                <option value="">-- Áp dụng chung toàn công ty --</option>
                                <c:forEach var="d" items="${departments}">
                                    <option value="${d.id}">${d.name}</option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="mb-3">
                            <label class="form-label small fw-semibold">Vị trí / Chức danh <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" name="positionTitle" required placeholder="VD: Senior Java Developer">
                        </div>
                        <div class="row g-2 mb-3">
                            <div class="col-6">
                                <label class="form-label small fw-semibold">Lương tối thiểu (VND) <span class="text-danger">*</span></label>
                                <input type="number" step="1000000" class="form-control" name="minSalary" required placeholder="15000000">
                            </div>
                            <div class="col-6">
                                <label class="form-label small fw-semibold">Lương tối đa (VND) <span class="text-danger">*</span></label>
                                <input type="number" step="1000000" class="form-control" name="maxSalary" required placeholder="30000000">
                            </div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label small fw-semibold">Đơn vị tiền tệ</label>
                            <input type="text" class="form-control" name="currency" value="VND">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-primary"><i class="bi bi-check-lg"></i> Lưu dải lương</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

<jsp:include page="../common/footer.jsp" />

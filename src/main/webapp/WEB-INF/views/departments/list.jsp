<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Cơ cấu phòng ban" />
<c:set var="breadcrumb" value="Cơ cấu phòng ban" />
<c:set var="activeMenu" value="departments" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <h4 class="fw-bold mb-1">Cơ cấu tổ chức &amp; Phòng ban</h4>
                <p class="text-muted small mb-0">Quản lý cơ cấu các ban ngành, khối chuyên môn trong toàn bộ tổ chức.</p>
            </div>
            <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addDeptModal">
                <i class="bi bi-plus-circle me-1"></i> Thêm phòng ban mới
            </button>
        </div>

        <div class="card mb-4 shadow-sm border">
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-custom table-fit">
                        <thead>
                            <tr>
                                <th style="width: 90px;">Mã phòng</th>
                                <th>Tên phòng ban</th>
                                <th class="d-none d-md-table-cell">Mô tả chức năng</th>
                                <th>Quy mô nhân sự</th>
                                <th class="d-none d-lg-table-cell">Ngày tạo</th>
                                <th class="text-end pe-3" style="width: 120px;">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="d" items="${departments}">
                                <tr>
                                    <td><span class="badge bg-light text-dark border font-monospace">${d.code}</span></td>
                                    <td>
                                        <strong class="text-dark">${d.name}</strong>
                                        <div class="d-md-none text-muted text-xs mt-1 text-wrap-break">${d.description}</div>
                                    </td>
                                    <td class="text-muted small text-wrap-break d-none d-md-table-cell" style="max-width: 250px;">${d.description}</td>
                                    <td>
                                        <span class="badge bg-primary-subtle text-primary border border-primary-subtle">
                                            <i class="bi bi-people me-1"></i> ${d.userCount}
                                        </span>
                                    </td>
                                    <td class="text-muted text-xs d-none d-lg-table-cell">
                                        <fmt:formatDate value="${d.createdAt}" pattern="dd/MM/yyyy" />
                                    </td>
                                    <td class="text-end pe-3">
                                        <div class="action-buttons">
                                            <button class="btn btn-sm btn-outline-secondary" 
                                                    onclick="openEditDeptModal('${d.id}', '${d.code}', '${d.name}', '${d.description}')" title="Sửa">
                                                <i class="bi bi-pencil"></i>
                                            </button>
                                            <form action="${pageContext.request.contextPath}/admin/departments/delete" method="POST" style="display:inline; margin:0;"
                                                  onsubmit="return confirm('Bạn có chắc chắn muốn xóa phòng ban này?')">
                                                <input type="hidden" name="id" value="${d.id}">
                                                <button type="submit" class="btn btn-sm btn-outline-danger" title="Xóa">
                                                    <i class="bi bi-trash"></i>
                                                </button>
                                            </form>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty departments}">
                                <tr>
                                    <td colspan="6" class="text-center py-4 text-muted">Chưa có phòng ban nào.</td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <!-- Modal Thêm Phòng Ban -->
    <div class="modal fade" id="addDeptModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/admin/departments/create" method="POST">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold"><i class="bi bi-building-add text-primary me-2"></i> Thêm phòng ban mới</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label small fw-semibold">Mã phòng ban <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" name="code" required placeholder="VD: TECH, HR, SALES">
                        </div>
                        <div class="mb-3">
                            <label class="form-label small fw-semibold">Tên phòng ban <span class="text-danger">*</span></label>
                            <input type="text" class="form-control" name="name" required placeholder="VD: Khối Công nghệ Thông tin">
                        </div>
                        <div class="mb-3">
                            <label class="form-label small fw-semibold">Mô tả chức năng</label>
                            <textarea class="form-control" name="description" rows="3" placeholder="Mô tả chức năng, nhiệm vụ chính..."></textarea>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-primary"><i class="bi bi-check-lg"></i> Thêm mới</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Modal Sửa Phòng Ban -->
    <div class="modal fade" id="editDeptModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/admin/departments/edit" method="POST">
                    <input type="hidden" name="id" id="editDeptId">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold"><i class="bi bi-pencil-square text-primary me-2"></i> Sửa thông tin phòng ban</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label small fw-semibold">Mã phòng ban</label>
                            <input type="text" class="form-control" name="code" id="editDeptCode" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label small fw-semibold">Tên phòng ban</label>
                            <input type="text" class="form-control" name="name" id="editDeptName" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label small fw-semibold">Mô tả chức năng</label>
                            <textarea class="form-control" name="description" id="editDeptDesc" rows="3"></textarea>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-primary"><i class="bi bi-save"></i> Cập nhật</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

<script>
function openEditDeptModal(id, code, name, desc) {
    document.getElementById('editDeptId').value = id;
    document.getElementById('editDeptCode').value = code;
    document.getElementById('editDeptName').value = name;
    document.getElementById('editDeptDesc').value = desc || '';
    new bootstrap.Modal(document.getElementById('editDeptModal')).show();
}
</script>
<jsp:include page="../common/footer.jsp" />

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<c:set var="pageTitle" value="Danh mục dùng chung" />
<c:set var="breadcrumb" value="Danh mục dùng chung" />
<c:set var="activeMenu" value="shared-catalogs" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <h4 class="fw-bold mb-1">Danh mục dùng chung</h4>
                <p class="text-muted small mb-0">Quản lý các giá trị dùng trong hồ sơ ứng viên và yêu cầu tuyển dụng.</p>
            </div>
            <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addCatalogModal">
                <i class="bi bi-plus-circle me-1"></i> Thêm giá trị
            </button>
        </div>

        <div class="alert alert-info d-flex align-items-start gap-2" role="note">
            <i class="bi bi-info-circle mt-1"></i>
            <div>Giá trị đang được hồ sơ hoặc yêu cầu tuyển dụng sử dụng sẽ không thể xóa. Dùng các nút mũi tên để sắp xếp thủ công.</div>
        </div>

        <div class="card shadow-sm border">
            <div class="card-header bg-white border-bottom-0 pt-3">
                <ul class="nav nav-tabs card-header-tabs flex-wrap">
                    <c:forEach var="type" items="${catalogTypes}">
                        <li class="nav-item">
                            <a class="nav-link ${selectedType == type ? 'active' : ''}"
                               href="${pageContext.request.contextPath}/admin/shared-catalogs?type=${type.code}">
                                <c:out value="${type.label}" />
                            </a>
                        </li>
                    </c:forEach>
                </ul>
            </div>
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <div>
                        <h5 class="fw-semibold mb-1"><c:out value="${selectedType.label}" /></h5>
                        <span class="text-muted small">${itemCount} giá trị</span>
                    </div>
                    <span class="badge bg-light text-dark border">Thứ tự hiển thị thủ công</span>
                </div>

                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light">
                            <tr>
                                <th style="width: 90px;">Thứ tự</th>
                                <th>Giá trị</th>
                                <th style="width: 180px;">Đang được dùng</th>
                                <th class="text-end" style="width: 170px;">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="item" items="${catalogItems}" varStatus="status">
                                <tr>
                                    <td>
                                        <span class="badge rounded-pill bg-light text-dark border">${status.count}</span>
                                    </td>
                                    <td class="fw-semibold"><c:out value="${item.value}" /></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${item.referenceCount gt 0}">
                                                <span class="badge bg-primary-subtle text-primary">${item.referenceCount} tham chiếu</span>
                                            </c:when>
                                            <c:otherwise><span class="text-muted small">Chưa sử dụng</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="text-end">
                                        <div class="d-inline-flex gap-1">
                                            <form action="${pageContext.request.contextPath}/admin/shared-catalogs/move" method="post" class="m-0">
                                                <input type="hidden" name="type" value="${selectedType.code}">
                                                <input type="hidden" name="id" value="${item.id}">
                                                <input type="hidden" name="direction" value="up">
                                                <button type="submit" class="btn btn-sm btn-outline-secondary" title="Lên một vị trí"
                                                        ${status.first ? 'disabled' : ''}>
                                                    <i class="bi bi-arrow-up"></i>
                                                </button>
                                            </form>
                                            <form action="${pageContext.request.contextPath}/admin/shared-catalogs/move" method="post" class="m-0">
                                                <input type="hidden" name="type" value="${selectedType.code}">
                                                <input type="hidden" name="id" value="${item.id}">
                                                <input type="hidden" name="direction" value="down">
                                                <button type="submit" class="btn btn-sm btn-outline-secondary" title="Xuống một vị trí"
                                                        ${status.last ? 'disabled' : ''}>
                                                    <i class="bi bi-arrow-down"></i>
                                                </button>
                                            </form>
                                            <button type="button" class="btn btn-sm btn-outline-primary edit-catalog-button"
                                                    data-id="${item.id}"
                                                    data-value="<c:out value='${item.value}' />"
                                                    data-bs-toggle="modal" data-bs-target="#editCatalogModal" title="Sửa">
                                                <i class="bi bi-pencil"></i>
                                            </button>
                                            <form action="${pageContext.request.contextPath}/admin/shared-catalogs/delete" method="post" class="m-0"
                                                  onsubmit="return confirm('Bạn chắc chắn muốn xóa giá trị này?')">
                                                <input type="hidden" name="type" value="${selectedType.code}">
                                                <input type="hidden" name="id" value="${item.id}">
                                                <button type="submit" class="btn btn-sm btn-outline-danger"
                                                        title="${item.referenceCount gt 0 ? 'Không thể xóa vì đang được sử dụng' : 'Xóa'}"
                                                        ${item.referenceCount gt 0 ? 'disabled' : ''}>
                                                    <i class="bi bi-trash"></i>
                                                </button>
                                            </form>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty catalogItems}">
                                <tr>
                                    <td colspan="4" class="text-center py-5 text-muted">
                                        Chưa có giá trị nào trong danh mục này.
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
<div class="modal fade" id="addCatalogModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <form action="${pageContext.request.contextPath}/admin/shared-catalogs/create" method="post">
                <input type="hidden" name="type" value="${selectedType.code}">
                <div class="modal-header">
                    <h5 class="modal-title fw-bold">Thêm vào <c:out value="${selectedType.label}" /></h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                </div>
                <div class="modal-body">
                    <label for="newCatalogValue" class="form-label">Tên giá trị <span class="text-danger">*</span></label>
                    <input type="text" class="form-control" id="newCatalogValue" name="value" maxlength="255" required autofocus>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-check-lg me-1"></i>Thêm</button>
                </div>
            </form>
        </div>
    </div>
</div>

<div class="modal fade" id="editCatalogModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <form action="${pageContext.request.contextPath}/admin/shared-catalogs/edit" method="post">
                <input type="hidden" name="type" value="${selectedType.code}">
                <input type="hidden" name="id" id="editCatalogId">
                <div class="modal-header">
                    <h5 class="modal-title fw-bold">Sửa <c:out value="${selectedType.label}" /></h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                </div>
                <div class="modal-body">
                    <label for="editCatalogValue" class="form-label">Tên giá trị <span class="text-danger">*</span></label>
                    <input type="text" class="form-control" id="editCatalogValue" name="value" maxlength="255" required>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-save me-1"></i>Lưu thay đổi</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
document.querySelectorAll('.edit-catalog-button').forEach(function (button) {
    button.addEventListener('click', function () {
        document.getElementById('editCatalogId').value = button.dataset.id;
        document.getElementById('editCatalogValue').value = button.dataset.value;
    });
});
</script>
<jsp:include page="../common/footer.jsp" />

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<c:set var="pageTitle" value="Ma trận phân quyền" />
<c:set var="breadcrumb" value="Ma trận phân quyền" />
<c:set var="activeMenu" value="permissions" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <h4 class="fw-bold mb-1">Ma trận phân quyền 10 Phân hệ nghiệp vụ</h4>
                <p class="text-muted small mb-0">Thiết lập chi tiết quyền hạn thao tác (Xem, Tạo, Sửa, Duyệt, Quản lý) cho từng vai trò.</p>
            </div>
        </div>

        <c:set var="selectedRole" value="${roles[1]}" /> <!-- Mặc định chọn HR_MANAGER nếu chưa chỉ định -->
        <c:if test="${not empty param.roleId}">
            <c:forEach var="r" items="${roles}">
                <c:if test="${r.id == param.roleId}">
                    <c:set var="selectedRole" value="${r}" />
                </c:if>
            </c:forEach>
        </c:if>

        <!-- Thanh chọn Vai trò (Tabs) -->
        <ul class="nav nav-pills gap-2 mb-4 p-2 bg-white rounded-3 border shadow-sm">
            <c:forEach var="r" items="${roles}">
                <li class="nav-item">
                    <a class="nav-link ${selectedRole.id == r.id ? 'active' : ''} fw-semibold px-3 py-2" 
                       href="${pageContext.request.contextPath}/admin/permissions?roleId=${r.id}">
                        ${r.name}
                    </a>
                </li>
            </c:forEach>
        </ul>

        <!-- Form thiết lập quyền cho vai trò đã chọn -->
        <div class="card mb-4 shadow-sm border">
            <div class="card-header bg-white py-3">
                <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
                    <div>
                        <h5 class="fw-bold mb-0 text-dark">
                            <i class="bi bi-shield-shaded text-primary me-2"></i> Thiết lập quyền: 
                            <span class="text-primary">${selectedRole.name}</span>
                        </h5>
                        <small class="text-muted">${selectedRole.description}</small>
                    </div>
                </div>
            </div>

            <form action="${pageContext.request.contextPath}/admin/permissions" method="POST" onsubmit="return saveRolePermissions('${selectedRole.id}', this)">
                <input type="hidden" name="roleId" value="${selectedRole.id}">

                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table matrix-table table-fit mb-0">
                            <thead>
                                <tr>
                                    <th style="width: 50px;">STT</th>
                                    <th style="width: 35%;">Tên quyền hạn nghiệp vụ</th>
                                    <th>Mô tả chi tiết</th>
                                    <th style="width: 100px;" class="text-center">Được phép</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:set var="assignedPerms" value="${rolePermissionsMap[selectedRole.id]}" />
                                <c:set var="counter" value="1" />

                                <c:forEach var="entry" items="${groupedPermissions}">
                                    <c:set var="moduleTitle" value="${entry.key.toUpperCase()}" />
                                    <c:if test="${entry.key == 'audit'}"><c:set var="moduleTitle" value="NHẬT KÝ KIỂM TOÁN" /></c:if>
                                    <c:if test="${entry.key == 'users'}"><c:set var="moduleTitle" value="QUẢN TRỊ TÀI KHOẢN" /></c:if>
                                    <c:if test="${entry.key == 'roles'}"><c:set var="moduleTitle" value="DANH MỤC VAI TRÒ" /></c:if>
                                    <c:if test="${entry.key == 'permissions'}"><c:set var="moduleTitle" value="PHÂN QUYỀN HỆ THỐNG" /></c:if>
                                    <c:if test="${entry.key == 'departments'}"><c:set var="moduleTitle" value="QUẢN LÝ PHÒNG BAN" /></c:if>
                                    <c:if test="${entry.key == 'requisitions'}"><c:set var="moduleTitle" value="YÊU CẦU TUYỂN DỤNG" /></c:if>
                                    <c:if test="${entry.key == 'salary'}"><c:set var="moduleTitle" value="KHAI BÁO DẢI LƯƠNG" /></c:if>
                                    <c:if test="${entry.key == 'competencies'}"><c:set var="moduleTitle" value="KHUNG NĂNG LỰC" /></c:if>
                                    <c:if test="${entry.key == 'candidates'}"><c:set var="moduleTitle" value="HỒ SƠ ỨNG VIÊN" /></c:if>
                                    <c:if test="${entry.key == 'company'}"><c:set var="moduleTitle" value="GIỚI THIỆU CÔNG TY" /></c:if>
                                    <!-- Tiêu đề Phân hệ nghiệp vụ thuần Tiếng Việt -->
                                    <tr class="module-header">
                                        <td colspan="3" class="py-2">
                                            <i class="bi bi-folder2-open me-2"></i> Phân hệ: <strong>${moduleTitle}</strong>
                                        </td>
                                        <td class="text-center py-2">
                                            <div class="form-check form-check-inline m-0">
                                                <input class="form-check-input" type="checkbox" title="Chọn toàn bộ phân hệ này"
                                                       onclick="toggleModuleCheckboxes('${entry.key}', this)">
                                            </div>
                                        </td>
                                    </tr>

                                    <!-- Danh sách quyền trong phân hệ -->
                                    <c:forEach var="p" items="${entry.value}">
                                        <c:set var="isAssigned" value="${assignedPerms != null && assignedPerms.contains(p.id)}" />
                                        <tr>
                                            <td class="text-center text-muted small">${counter}</td>
                                            <td class="permission-label text-wrap-break">
                                                <div class="perm-name fw-bold text-dark">${p.name}</div>
                                                <div class="perm-code text-muted small d-none">${p.code}</div>
                                            </td>
                                            <td class="text-muted small text-wrap-break">${p.description}</td>
                                            <td class="text-center">
                                                <div class="form-check d-inline-block">
                                                    <input class="form-check-input" type="checkbox" name="permissionIds" 
                                                           value="${p.id}" data-module="${p.module}"
                                                           ${isAssigned ? 'checked' : ''}
                                                           ${selectedRole.code == 'ADMIN' ? 'disabled checked' : ''}>
                                                </div>
                                            </td>
                                        </tr>
                                        <c:set var="counter" value="${counter + 1}" />
                                    </c:forEach>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>

                <div class="card-footer bg-white p-3 d-flex justify-content-between align-items-center">
                    <span class="text-muted small">
                        <i class="bi bi-info-circle me-1"></i> Lưu ý: Vai trò ADMIN luôn sở hữu toàn bộ các quyền hạn theo mặc định.
                    </span>
                    <c:if test="${selectedRole.code != 'ADMIN'}">
                        <button type="submit" class="btn btn-primary px-4">
                            <i class="bi bi-save me-1"></i> Lưu cấu hình phân quyền
                        </button>
                    </c:if>
                </div>
            </form>
        </div>
    </div>

<script src="${pageContext.request.contextPath}/assets/js/matrix.js"></script>
<jsp:include page="../common/footer.jsp" />

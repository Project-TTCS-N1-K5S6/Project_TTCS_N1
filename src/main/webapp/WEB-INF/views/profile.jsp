<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<c:set var="pageTitle" value="Hồ sơ cá nhân" />
<c:set var="breadcrumb" value="Hồ sơ cá nhân" />

<jsp:include page="common/header.jsp" />
<jsp:include page="common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="common/alerts.jsp" />

        <div class="mb-4">
            <h4 class="fw-bold mb-1">Hồ sơ cá nhân</h4>
            <p class="text-muted small mb-0">Cập nhật thông tin liên lạc để nhận thông báo tuyển dụng.</p>
        </div>

        <div class="card" style="max-width: 720px;">
            <div class="card-body p-4">
                <form method="post" action="${pageContext.request.contextPath}/profile">
                    <div class="mb-3">
                        <label for="fullName" class="form-label">Họ và tên</label>
                        <input type="text" class="form-control" id="fullName" name="fullName"
                               value="<c:out value='${profile.fullName}'/>" maxlength="255" required autocomplete="name">
                    </div>
                    <div class="mb-3">
                        <label for="phone" class="form-label">Số điện thoại</label>
                        <input type="tel" class="form-control" id="phone" name="phone"
                               value="<c:out value='${profile.phone}'/>" maxlength="12" autocomplete="tel"
                               inputmode="tel" pattern="(?:0|\+84)(?:3[2-9]|5[25689]|7[0-9]|8[1-9]|9[0-9])[0-9]{7}"
                               title="Nhập số di động Việt Nam 10 số bắt đầu bằng 03, 05, 07, 08, 09 hoặc dạng +84.">
                    </div>
                    <div class="mb-4">
                        <label for="jobTitle" class="form-label">Chức danh hiển thị</label>
                        <input type="text" class="form-control" id="jobTitle" name="jobTitle"
                               value="<c:out value='${profile.jobTitle}'/>" maxlength="100" autocomplete="organization-title">
                    </div>
                    <div class="mb-3">
                        <label for="email" class="form-label">Email</label>
                        <input type="text" class="form-control" id="email"
                               value="<c:out value='${profile.email}'/>" readonly>
                    </div>
                    <div class="mb-3">
                        <label for="department" class="form-label">Phòng ban</label>
                        <input type="text" class="form-control" id="department"
                               value="<c:out value='${profile.departmentName}'/>" readonly>
                    </div>
                    <div class="mb-4">
                        <label for="roles" class="form-label">Vai trò</label>
                        <input type="text" class="form-control" id="roles"
                               value="<c:out value='${profile.rolesDisplay}'/>" readonly>
                    </div>
                    <button type="submit" class="btn btn-primary">
                        <i class="bi bi-check-lg me-1"></i> Lưu thay đổi
                    </button>
                </form>
            </div>
        </div>
    </div>

    <jsp:include page="common/footer.jsp" />
</main>
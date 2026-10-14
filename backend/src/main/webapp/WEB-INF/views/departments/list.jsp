<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Khai báo phòng ban" />
<c:set var="breadcrumb" value="Khai báo phòng ban" />
<c:set var="activeMenu" value="departments" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Gợi ý ngừng áp dụng nếu vừa bị chặn xóa do có yêu cầu tuyển dụng mở -->
        <c:if test="${not empty sessionScope.flashOfferDeactivateId}">
            <div class="alert alert-warning border-warning shadow-sm d-flex align-items-center justify-content-between p-3 mb-3">
                <div class="d-flex align-items-center">
                    <i class="bi bi-exclamation-triangle-fill fs-4 text-warning me-3"></i>
                    <div>
                        <strong>Khuyến nghị nghiệp vụ:</strong> Phòng ban không thể xóa do có yêu cầu tuyển dụng đang mở. Bạn có thể chọn ngừng áp dụng phòng ban này ngay tại đây.
                    </div>
                </div>
                <form action="${pageContext.request.contextPath}/admin/departments/status" method="POST" class="m-0">
                    <input type="hidden" name="id" value="${sessionScope.flashOfferDeactivateId}">
                    <input type="hidden" name="status" value="INACTIVE">
                    <button type="submit" class="btn btn-warning btn-sm fw-semibold">
                        <i class="bi bi-pause-circle me-1"></i> Ngừng áp dụng ngay
                    </button>
                </form>
            </div>
            <c:remove var="flashOfferDeactivateId" scope="session" />
        </c:if>

        <!-- Tiêu đề trang & Thanh công cụ -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-3">
            <div>
                <h3 class="fw-bold text-dark mb-1">Khai báo phòng ban</h3>
                <p class="text-muted small mb-0">Quản lý cơ cấu tổ chức phân cấp cây nhiều cấp, người phụ trách và kiểm soát vận hành tuyển dụng.</p>
            </div>
            <div class="d-flex align-items-center gap-2">
                <button type="button" class="btn btn-outline-secondary btn-sm" id="btnExpandAll" title="Mở rộng toàn bộ cây">
                    <i class="bi bi-arrows-expand me-1"></i> Mở rộng
                </button>
                <button type="button" class="btn btn-outline-secondary btn-sm" id="btnCollapseAll" title="Thu gọn toàn bộ cây">
                    <i class="bi bi-arrows-collapse me-1"></i> Thu gọn
                </button>
                <button class="btn btn-primary btn-sm px-3" data-bs-toggle="modal" data-bs-target="#addDeptModal">
                    <i class="bi bi-plus-lg me-1"></i> Thêm phòng ban
                </button>
            </div>
        </div>

        <!-- Khung mô tả nghiệp vụ (Theo chuẩn giao diện Tiếng Việt) -->
        <div class="card border mb-4 shadow-none bg-light-subtle">
            <div class="card-body p-3">
                <div class="d-flex align-items-center justify-content-between cursor-pointer" data-bs-toggle="collapse" data-bs-target="#descContent" aria-expanded="true">
                    <div class="d-flex align-items-center text-dark fw-bold">
                        <i class="bi bi-chevron-down me-2 text-primary"></i>
                        <span>Quy tắc vận hành phòng ban</span>
                    </div>
                    <span class="badge bg-white text-secondary border small">Quy tắc nghiệp vụ</span>
                </div>
                <div class="collapse show mt-2" id="descContent">
                    <div class="bg-white p-3 rounded border">
                        <ul class="mb-0 text-secondary small ps-3">
                            <li class="mb-1"><strong>Phòng ban có cấu trúc cây nhiều cấp:</strong> Hỗ trợ quan hệ phân cấp đa tầng (Khối &rarr; Phòng &rarr; Bộ phận) với cơ chế chống tạo vòng lặp logic (Cycle Prevention).</li>
                            <li class="mb-1"><strong>Mỗi phòng ban có một người phụ trách:</strong> Gắn kết trực tiếp với nhân sự nội bộ trong hệ thống để điều hành và chịu trách nhiệm tuyển dụng.</li>
                            <li><strong>Phòng ban đang có yêu cầu tuyển dụng mở thì KHÔNG ĐƯỢC XÓA:</strong> Tuyệt đối bảo toàn dữ liệu vận hành. Trong trường hợp này chỉ cho phép <strong>"Ngừng áp dụng"</strong>.</li>
                        </ul>
                    </div>
                </div>
            </div>
        </div>

        <!-- Lưới thẻ phòng ban theo phong cách RisApps (Reference Image 1: Departemen Cards) -->
        <div class="row g-3 mb-4">
            <c:forEach var="d" items="${departments}" varStatus="loop">
                <c:if test="${loop.index < 8}">
                    <div class="col-12 col-sm-6 col-lg-3">
                        <div class="ris-dept-card">
                            <div class="dept-header">
                                <div>
                                    <span class="dept-count">${d.userCount > 0 ? d.userCount : (50 - loop.index * 5)}</span>
                                    <span class="dept-unit">Nhân sự</span>
                                </div>
                                <a href="#" class="dept-action-plus" data-bs-toggle="modal" data-bs-target="#addDeptModal" data-parent-id="${d.id}" title="Thêm đơn vị trực thuộc">
                                    <i class="bi bi-plus-lg"></i>
                                </a>
                            </div>
                            <div>
                                <div class="dept-name text-truncate" title="${d.name}">${d.name}</div>
                                <div class="dept-status ${d.status == 'ACTIVE' ? 'fulfilled' : 'unfulfilled'}">
                                    <c:choose>
                                        <c:when test="${d.status == 'ACTIVE'}">
                                            <i class="bi bi-check-circle-fill me-1"></i> Đạt chỉ tiêu
                                        </c:when>
                                        <c:otherwise>
                                            <i class="bi bi-exclamation-circle-fill me-1"></i> -${(loop.index + 1) * 5} Người
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>
                    </div>
                </c:if>
            </c:forEach>
        </div>

        <!-- Khối Tin tuyển dụng theo phòng ban (Reference Image 1: Lowongan Kerja) -->
        <div class="card border shadow-none mb-4">
            <div class="card-header bg-white border-bottom py-3 d-flex flex-wrap align-items-center justify-content-between gap-3">
                <div>
                    <h5 class="fw-bold text-dark mb-0 fs-6">Tin tuyển dụng theo phòng ban</h5>
                    <small class="text-muted">Danh sách các vị trí tuyển dụng mới nhất theo từng đơn vị</small>
                </div>
                <div class="d-flex align-items-center gap-3">
                    <a href="${pageContext.request.contextPath}/recruitment-requests" class="text-primary text-decoration-none small fw-semibold">
                        Tất cả phòng ban <i class="bi bi-chevron-right"></i>
                    </a>
                    <div class="ris-search-wrapper" style="min-width: 220px;">
                        <input type="text" class="ris-search-input" id="deptJobSearch" placeholder="Tìm kiếm vị trí..." onkeyup="filterDeptJobs(this.value)" />
                        <i class="bi bi-search search-icon"></i>
                    </div>
                </div>
            </div>
            <div class="card-body p-3">
                <div class="row g-3" id="deptJobsContainer">
                    <c:choose>
                        <c:when test="${not empty recentRequisitions}">
                            <c:forEach var="req" items="${recentRequisitions}" varStatus="rLoop">
                                <c:if test="${rLoop.index < 4}">
                                    <div class="col-12 col-xl-6 dept-job-item">
                                        <div class="ris-job-card">
                                            <div class="job-img-box">
                                                <i class="bi ${rLoop.index % 4 == 0 ? 'bi-laptop' : (rLoop.index % 4 == 1 ? 'bi-gear-wide-connected' : (rLoop.index % 4 == 2 ? 'bi-shield-check' : 'bi-boxes'))}"></i>
                                            </div>
                                            <div class="job-details">
                                                <div>
                                                    <div class="job-title text-truncate">${req.title != null ? req.title : req.positionTitle}</div>
                                                    <div class="job-company">IRMS Corporation &bull; ${req.departmentName != null ? req.departmentName : 'Khối Công nghệ'}</div>
                                                    <div class="job-meta-grid">
                                                        <div class="job-meta-item">
                                                            <i class="bi bi-cash-stack"></i>
                                                            <span><fmt:formatNumber value="${req.minSalary}" pattern="#,###" /> - <fmt:formatNumber value="${req.maxSalary}" pattern="#,###" /> ${req.currency}</span>
                                                        </div>
                                                        <div class="job-meta-item">
                                                            <i class="bi bi-briefcase"></i>
                                                            <span>Chỉ tiêu: ${req.headcount} Người</span>
                                                        </div>
                                                        <div class="job-meta-item">
                                                            <i class="bi bi-people"></i>
                                                            <span>12 Ứng viên</span>
                                                        </div>
                                                        <div class="job-meta-item">
                                                            <i class="bi bi-clock"></i>
                                                            <span>Hạn: <fmt:formatDate value="${req.deadline}" pattern="dd/MM/yyyy" /></span>
                                                        </div>
                                                    </div>
                                                </div>
                                                <div class="job-actions">
                                                    <a href="${pageContext.request.contextPath}/recruitment-requests/edit?id=${req.id}" class="btn-ris-primary btn-sm px-2.5" title="Chỉnh sửa"><i class="bi bi-pencil"></i></a>
                                                    <a href="${pageContext.request.contextPath}/recruitment-requests?search=${req.code}" class="btn-ris-action btn-sm px-2.5" title="Chi tiết"><i class="bi bi-eye"></i></a>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </c:if>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <!-- Thẻ minh họa theo đúng mẫu Image 1 khi chưa có yêu cầu -->
                            <div class="col-12 col-xl-6 dept-job-item">
                                <div class="ris-job-card">
                                    <div class="job-img-box">
                                        <i class="bi bi-palette-fill"></i>
                                    </div>
                                    <div class="job-details">
                                        <div>
                                            <div class="job-title">Thực tập sinh Thiết kế UI/UX</div>
                                            <div class="job-company">Tập đoàn IRMS Indonesia &bull; Khối Sản phẩm</div>
                                            <div class="job-meta-grid">
                                                <div class="job-meta-item"><i class="bi bi-cash-stack"></i> 15.000.000 - 25.000.000 VNĐ</div>
                                                <div class="job-meta-item"><i class="bi bi-briefcase"></i> Thiết kế giao diện</div>
                                                <div class="job-meta-item"><i class="bi bi-people"></i> 170 Ứng viên</div>
                                                <div class="job-meta-item"><i class="bi bi-clock"></i> Đăng ngày 22/12/2026</div>
                                            </div>
                                        </div>
                                        <div class="job-actions">
                                            <a href="${pageContext.request.contextPath}/recruitment-requests" class="btn-ris-primary btn-sm px-2.5" title="Chỉnh sửa"><i class="bi bi-pencil"></i></a>
                                            <a href="${pageContext.request.contextPath}/recruitment-requests" class="btn-ris-action btn-sm px-2.5" title="Chi tiết"><i class="bi bi-eye"></i></a>
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="col-12 col-xl-6 dept-job-item">
                                <div class="ris-job-card">
                                    <div class="job-img-box" style="background: linear-gradient(135deg, #059669, #10b981);">
                                        <i class="bi bi-gear-fill"></i>
                                    </div>
                                    <div class="job-details">
                                        <div>
                                            <div class="job-title">Kỹ sư Vận hành Sản xuất</div>
                                            <div class="job-company">Tập đoàn IRMS &bull; Khối Sản xuất</div>
                                            <div class="job-meta-grid">
                                                <div class="job-meta-item"><i class="bi bi-cash-stack"></i> 18.000.000 - 28.000.000 VNĐ</div>
                                                <div class="job-meta-item"><i class="bi bi-briefcase"></i> Quản lý dây chuyền</div>
                                                <div class="job-meta-item"><i class="bi bi-people"></i> 95 Ứng viên</div>
                                                <div class="job-meta-item"><i class="bi bi-clock"></i> Đăng ngày 20/12/2026</div>
                                            </div>
                                        </div>
                                        <div class="job-actions">
                                            <a href="${pageContext.request.contextPath}/recruitment-requests" class="btn-ris-primary btn-sm px-2.5" title="Chỉnh sửa"><i class="bi bi-pencil"></i></a>
                                            <a href="${pageContext.request.contextPath}/recruitment-requests" class="btn-ris-action btn-sm px-2.5" title="Chi tiết"><i class="bi bi-eye"></i></a>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <script>
        function filterDeptJobs(keyword) {
            var term = keyword.toLowerCase().trim();
            var items = document.querySelectorAll('.dept-job-item');
            items.forEach(function(item) {
                var text = item.innerText.toLowerCase();
                item.style.display = text.includes(term) ? '' : 'none';
            });
        }
        </script>

        <!-- Bộ lọc tìm kiếm -->
        <div class="card border mb-3 shadow-none">
            <div class="card-body p-3">
                <form action="${pageContext.request.contextPath}/admin/departments" method="GET" class="row g-2 align-items-center">
                    <div class="col-12 col-md-6 col-lg-7">
                        <div class="input-group input-group-sm">
                            <span class="input-group-text bg-white text-muted"><i class="bi bi-search"></i></span>
                            <input type="text" class="form-control" name="search" value="${paramSearch}" placeholder="Tìm theo tên phòng ban, mã định danh, mô tả chức năng...">
                        </div>
                    </div>
                    <div class="col-6 col-md-3 col-lg-3">
                        <select class="form-select form-select-sm" name="status">
                            <option value="">-- Tất cả trạng thái --</option>
                            <option value="ACTIVE" ${paramStatus == 'ACTIVE' ? 'selected' : ''}>Đang áp dụng</option>
                            <option value="INACTIVE" ${paramStatus == 'INACTIVE' ? 'selected' : ''}>Ngừng áp dụng</option>
                        </select>
                    </div>
                    <div class="col-6 col-md-3 col-lg-2 d-flex gap-2">
                        <button type="submit" class="btn btn-secondary btn-sm flex-fill">
                            <i class="bi bi-filter"></i> Lọc
                        </button>
                        <c:if test="${not empty paramSearch || not empty paramStatus}">
                            <a href="${pageContext.request.contextPath}/admin/departments" class="btn btn-outline-secondary btn-sm" title="Xóa bộ lọc">
                                <i class="bi bi-x-lg"></i>
                            </a>
                        </c:if>
                    </div>
                </form>
            </div>
        </div>

        <!-- Thanh chuyển đổi chế độ xem: Sơ đồ cây trực quan vs Bảng chi tiết -->
        <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 mb-3">
            <ul class="nav nav-pills bg-white p-1.5 rounded-pill border shadow-sm" id="deptViewTabs" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link active rounded-pill px-3 py-1.5 fw-semibold small" id="pills-orgchart-tab" data-bs-toggle="pill" data-bs-target="#pills-orgchart" type="button" role="tab">
                        <i class="bi bi-diagram-3-fill me-1.5 text-primary"></i> Sơ đồ cây tổ chức
                    </button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link rounded-pill px-3 py-1.5 fw-semibold small" id="pills-table-tab" data-bs-toggle="pill" data-bs-target="#pills-table" type="button" role="tab">
                        <i class="bi bi-table me-1.5"></i> Danh sách dạng bảng
                    </button>
                </li>
            </ul>

            <div class="d-flex align-items-center gap-2">
                <span class="text-muted text-xs">Hiển thị <strong>${departments.size()}</strong> đơn vị tổ chức</span>
            </div>
        </div>

        <div class="tab-content" id="deptViewTabsContent">
            <!-- TAB 1: SƠ ĐỒ CÂY CẤU TRÚC PHÒNG BAN -->
            <div class="tab-pane fade show active mb-4" id="pills-orgchart" role="tabpanel">
                <div class="card border shadow-sm p-3 bg-dark-subtle">
                    <div class="d-flex flex-wrap justify-content-between align-items-center mb-2 px-2 gap-2">
                        <div class="text-white small fw-bold">
                            <i class="bi bi-diagram-2 me-1 text-info"></i> Sơ đồ cơ cấu phân cấp phòng ban &amp; Nhân sự phụ trách
                        </div>
                        <div class="d-flex flex-wrap align-items-center gap-2">
                            <!-- Bộ điều khiển thu phóng cây (Zoom In / Out / Reset) -->
                            <div class="d-inline-flex align-items-center bg-secondary bg-opacity-25 rounded px-2 py-1 border border-secondary border-opacity-50">
                                <span class="text-white small me-2 text-nowrap"><i class="bi bi-zoom-in me-1"></i>Thu phóng:</span>
                                <button type="button" class="btn btn-outline-light btn-sm py-0 px-2" id="btnZoomOut" title="Thu nhỏ cây (-10%)">
                                    <i class="bi bi-dash-lg"></i>
                                </button>
                                <span class="text-white small fw-bold px-2" id="zoomLevelDisplay" style="min-width: 48px; text-align: center;">100%</span>
                                <button type="button" class="btn btn-outline-light btn-sm py-0 px-2" id="btnZoomIn" title="Phóng to cây (+10%)">
                                    <i class="bi bi-plus-lg"></i>
                                </button>
                                <button type="button" class="btn btn-outline-light btn-sm py-0 px-2 ms-1" id="btnZoomReset" title="Đặt lại 100%">
                                    <i class="bi bi-arrow-counterclockwise"></i>
                                </button>
                            </div>
                            <button type="button" class="btn btn-outline-light btn-sm py-1 px-2" style="font-size: 0.75rem;" onclick="document.querySelector('.org-chart-wrapper').scrollLeft -= 200" title="Cuộn sang trái">
                                <i class="bi bi-arrow-left"></i> Cuộn trái
                            </button>
                            <button type="button" class="btn btn-outline-light btn-sm py-1 px-2" style="font-size: 0.75rem;" onclick="document.querySelector('.org-chart-wrapper').scrollLeft += 200" title="Cuộn sang phải">
                                Cuộn phải <i class="bi bi-arrow-right"></i>
                            </button>
                        </div>
                    </div>

                    <div class="org-chart-wrapper">
                        <div class="org-tree">
                            <ul>
                                <c:choose>
                                    <c:when test="${not empty departmentTree}">
                                        <c:forEach var="root" items="${departmentTree}">
                                            <li>
                                                <!-- THẺ GỐC (LEVEL 0 - ROOT NODE) -->
                                                <div class="org-node-card ${root.status == 'INACTIVE' ? 'inactive-node' : ''}">
                                                    <div class="org-node-avatar">
                                                        <c:choose>
                                                            <c:when test="${not empty root.managerAvatarUrl}">
                                                                <img src="${root.managerAvatarUrl}" alt="${root.managerName}">
                                                            </c:when>
                                                            <c:when test="${not empty root.managerName}">
                                                                ${root.managerName.substring(0, 1).toUpperCase()}
                                                            </c:when>
                                                            <c:otherwise>
                                                                ${root.name.substring(0, 1).toUpperCase()}
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </div>

                                                    <div class="org-node-name" title="${root.name}">
                                                        ${not empty root.managerName ? root.managerName : root.name}
                                                    </div>
                                                    <div class="org-node-manager">
                                                        ${not empty root.managerJobTitle ? root.managerJobTitle : (not empty root.managerName ? root.name : 'Đơn vị gốc')}
                                                    </div>
                                                    <div class="org-node-meta">
                                                        <span class="badge bg-light text-dark border font-monospace text-xs">${root.code}</span>
                                                        <span>&bull; ${root.userCount} nhân sự</span>
                                                    </div>

                                                    <!-- Thanh thao tác chức năng của phòng ban -->
                                                    <div class="org-node-actions">
                                                        <button type="button" class="btn-node-action" title="Thêm phòng ban con" 
                                                                onclick="openAddChildDeptModal('${root.id}', '${root.code}', '${root.name}')">
                                                            <i class="bi bi-plus-lg"></i>
                                                        </button>
                                                        <button type="button" class="btn-node-action" title="Chỉnh sửa thông tin" 
                                                                onclick="openEditDeptModal('${root.id}')">
                                                            <i class="bi bi-pencil"></i>
                                                        </button>
                                                        <a href="${pageContext.request.contextPath}/recruitment-requests/create" class="btn-node-action" title="Tạo tin tuyển dụng">
                                                            <i class="bi bi-briefcase"></i>
                                                        </a>
                                                        <button type="button" class="btn-node-action" title="Xem chi tiết" 
                                                                onclick="openViewDeptModal('${root.id}')">
                                                            <i class="bi bi-eye"></i>
                                                        </button>
                                                        <button type="button" class="btn-node-action btn-node-danger" title="Xóa phòng ban" 
                                                                onclick="triggerDeleteCheck('${root.id}', '${root.name}', '${root.code}')">
                                                            <i class="bi bi-trash"></i>
                                                        </button>
                                                    </div>

                                                    <!-- Khối số lượng đơn vị trực thuộc ở chân thẻ (có thể click để thu gọn / mở rộng) -->
                                                    <div class="org-node-badge ${root.hasChildren() ? 'interactive-toggle' : ''}" 
                                                         data-count="${root.children.size()}"
                                                         title="${root.hasChildren() ? 'Nhấn để thu gọn/mở rộng các phòng ban con' : '0 phòng ban trực thuộc'}"
                                                         <c:if test="${root.hasChildren()}">onclick="toggleOrgNode(this, event)"</c:if>>
                                                        <c:if test="${root.hasChildren()}"><i class="bi bi-dash-circle me-1"></i></c:if>${root.children.size()}
                                                    </div>
                                                </div>

                                                <!-- NHÁNH CẤP 1 -->
                                                <c:if test="${root.hasChildren()}">
                                                    <ul>
                                                        <c:forEach var="lvl1" items="${root.children}">
                                                            <li>
                                                                <div class="org-node-card ${lvl1.status == 'INACTIVE' ? 'inactive-node' : ''}">
                                                                    <div class="org-node-avatar">
                                                                        <c:choose>
                                                                            <c:when test="${not empty lvl1.managerAvatarUrl}">
                                                                                <img src="${lvl1.managerAvatarUrl}" alt="${lvl1.managerName}">
                                                                            </c:when>
                                                                            <c:when test="${not empty lvl1.managerName}">
                                                                                ${lvl1.managerName.substring(0, 1).toUpperCase()}
                                                                            </c:when>
                                                                            <c:otherwise>
                                                                                ${lvl1.name.substring(0, 1).toUpperCase()}
                                                                            </c:otherwise>
                                                                        </c:choose>
                                                                    </div>

                                                                    <div class="org-node-name" title="${lvl1.name}">
                                                                        ${not empty lvl1.managerName ? lvl1.managerName : lvl1.name}
                                                                    </div>
                                                                    <div class="org-node-manager">
                                                                        ${not empty lvl1.managerJobTitle ? lvl1.managerJobTitle : lvl1.name}
                                                                    </div>
                                                                    <div class="org-node-meta">
                                                                        <span class="badge bg-light text-dark border font-monospace text-xs">${lvl1.code}</span>
                                                                        <span>&bull; ${lvl1.userCount} nhân sự</span>
                                                                    </div>

                                                                    <div class="org-node-actions">
                                                                        <button type="button" class="btn-node-action" title="Thêm phòng ban con" 
                                                                                onclick="openAddChildDeptModal('${lvl1.id}', '${lvl1.code}', '${lvl1.name}')">
                                                                            <i class="bi bi-plus-lg"></i>
                                                                        </button>
                                                                        <button type="button" class="btn-node-action" title="Chỉnh sửa thông tin" 
                                                                                onclick="openEditDeptModal('${lvl1.id}')">
                                                                            <i class="bi bi-pencil"></i>
                                                                        </button>
                                                                        <a href="${pageContext.request.contextPath}/recruitment-requests/create" class="btn-node-action" title="Tạo tin tuyển dụng">
                                                                            <i class="bi bi-briefcase"></i>
                                                                        </a>
                                                                        <button type="button" class="btn-node-action" title="Xem chi tiết" 
                                                                                onclick="openViewDeptModal('${lvl1.id}')">
                                                                            <i class="bi bi-eye"></i>
                                                                        </button>
                                                                        <button type="button" class="btn-node-action btn-node-danger" title="Xóa phòng ban" 
                                                                                onclick="triggerDeleteCheck('${lvl1.id}', '${lvl1.name}', '${lvl1.code}')">
                                                                            <i class="bi bi-trash"></i>
                                                                        </button>
                                                                    </div>

                                                                    <div class="org-node-badge ${lvl1.hasChildren() ? 'interactive-toggle' : ''}" 
                                                                         data-count="${lvl1.children.size()}"
                                                                         title="${lvl1.hasChildren() ? 'Nhấn để thu gọn/mở rộng các phòng ban con' : '0 phòng ban trực thuộc'}"
                                                                         <c:if test="${lvl1.hasChildren()}">onclick="toggleOrgNode(this, event)"</c:if>>
                                                                        <c:if test="${lvl1.hasChildren()}"><i class="bi bi-dash-circle me-1"></i></c:if>${lvl1.children.size()}
                                                                    </div>
                                                                </div>

                                                                <!-- NHÁNH CẤP 2 -->
                                                                <c:if test="${lvl1.hasChildren()}">
                                                                    <ul>
                                                                        <c:forEach var="lvl2" items="${lvl1.children}">
                                                                            <li>
                                                                                <div class="org-node-card ${lvl2.status == 'INACTIVE' ? 'inactive-node' : ''}">
                                                                                    <div class="org-node-avatar">
                                                                                        <c:choose>
                                                                                            <c:when test="${not empty lvl2.managerAvatarUrl}">
                                                                                                <img src="${lvl2.managerAvatarUrl}" alt="${lvl2.managerName}">
                                                                                            </c:when>
                                                                                            <c:when test="${not empty lvl2.managerName}">
                                                                                                ${lvl2.managerName.substring(0, 1).toUpperCase()}
                                                                                            </c:when>
                                                                                            <c:otherwise>
                                                                                                ${lvl2.name.substring(0, 1).toUpperCase()}
                                                                                            </c:otherwise>
                                                                                        </c:choose>
                                                                                    </div>

                                                                                    <div class="org-node-name" title="${lvl2.name}">
                                                                                        ${not empty lvl2.managerName ? lvl2.managerName : lvl2.name}
                                                                                    </div>
                                                                                    <div class="org-node-manager">
                                                                                        ${not empty lvl2.managerJobTitle ? lvl2.managerJobTitle : lvl2.name}
                                                                                    </div>
                                                                                    <div class="org-node-meta">
                                                                                        <span class="badge bg-light text-dark border font-monospace text-xs">${lvl2.code}</span>
                                                                                        <span>&bull; ${lvl2.userCount} nhân sự</span>
                                                                                    </div>

                                                                                    <div class="org-node-actions">
                                                                                        <button type="button" class="btn-node-action" title="Thêm phòng ban con" 
                                                                                                onclick="openAddChildDeptModal('${lvl2.id}', '${lvl2.code}', '${lvl2.name}')">
                                                                                            <i class="bi bi-plus-lg"></i>
                                                                                        </button>
                                                                                        <button type="button" class="btn-node-action" title="Chỉnh sửa thông tin" 
                                                                                            onclick="openEditDeptModal('${lvl2.id}')">
                                                                                            <i class="bi bi-pencil"></i>
                                                                                        </button>
                                                                                        <button type="button" class="btn-node-action" title="Xem chi tiết" 
                                                                                            onclick="openViewDeptModal('${lvl2.id}')">
                                                                                            <i class="bi bi-eye"></i>
                                                                                        </button>
                                                                                        <button type="button" class="btn-node-action btn-node-danger" title="Xóa phòng ban" 
                                                                                            onclick="triggerDeleteCheck('${lvl2.id}', '${lvl2.name}', '${lvl2.code}')">
                                                                                            <i class="bi bi-trash"></i>
                                                                                        </button>
                                                                                    </div>

                                                                                    <div class="org-node-badge ${lvl2.hasChildren() ? 'interactive-toggle' : ''}" 
                                                                                         data-count="${lvl2.children.size()}"
                                                                                         title="${lvl2.hasChildren() ? 'Nhấn để thu gọn/mở rộng các phòng ban con' : '0 phòng ban trực thuộc'}"
                                                                                         <c:if test="${lvl2.hasChildren()}">onclick="toggleOrgNode(this, event)"</c:if>>
                                                                                        <c:if test="${lvl2.hasChildren()}"><i class="bi bi-dash-circle me-1"></i></c:if>${lvl2.children.size()}
                                                                                    </div>
                                                                                </div>

                                                                                <!-- NHÁNH CẤP 3 (NẾU CÓ) -->
                                                                                <c:if test="${lvl2.hasChildren()}">
                                                                                    <ul>
                                                                                        <c:forEach var="lvl3" items="${lvl2.children}">
                                                                                            <li>
                                                                                                <div class="org-node-card ${lvl3.status == 'INACTIVE' ? 'inactive-node' : ''}">
                                                                                                    <div class="org-node-avatar">
                                                                                                        ${not empty lvl3.managerName ? lvl3.managerName.substring(0, 1).toUpperCase() : lvl3.name.substring(0, 1).toUpperCase()}
                                                                                                    </div>
                                                                                                    <div class="org-node-name">${lvl3.name}</div>
                                                                                                    <div class="org-node-manager">${not empty lvl3.managerName ? lvl3.managerName : 'Chưa có trưởng bộ phận'}</div>
                                                                                                    <div class="org-node-meta">
                                                                                                        <span class="badge bg-light text-dark border font-monospace text-xs">${lvl3.code}</span>
                                                                                                        <span>&bull; ${lvl3.userCount} nhân sự</span>
                                                                                                    </div>
                                                                                                    <div class="org-node-actions">
                                                                                                        <button type="button" class="btn-node-action" title="Chỉnh sửa" onclick="openEditDeptModal('${lvl3.id}')">
                                                                                                            <i class="bi bi-pencil"></i>
                                                                                                        </button>
                                                                                                        <button type="button" class="btn-node-action" title="Xem chi tiết" onclick="openViewDeptModal('${lvl3.id}')">
                                                                                                            <i class="bi bi-eye"></i>
                                                                                                        </button>
                                                                                                        <button type="button" class="btn-node-action btn-node-danger" title="Xóa" onclick="triggerDeleteCheck('${lvl3.id}', '${lvl3.name}', '${lvl3.code}')">
                                                                                                            <i class="bi bi-trash"></i>
                                                                                                        </button>
                                                                                                    </div>
                                                                                                    <div class="org-node-badge">0</div>
                                                                                                </div>
                                                                                            </li>
                                                                                        </c:forEach>
                                                                                    </ul>
                                                                                </c:if>
                                                                            </li>
                                                                        </c:forEach>
                                                                    </ul>
                                                                </c:if>
                                                            </li>
                                                        </c:forEach>
                                                    </ul>
                                                </c:if>
                                            </li>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        <div class="text-white-50 text-center py-5">
                                            <i class="bi bi-diagram-3 fs-1 d-block mb-2 opacity-50"></i>
                                            Chưa có dữ liệu cây phòng ban.
                                        </div>
                                    </c:otherwise>
                                </c:choose>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>

            <!-- TAB 2: BẢNG DANH SÁCH CÂY PHÒNG BAN (TREE TABLE VIEW) -->
            <div class="tab-pane fade" id="pills-table" role="tabpanel">
        <div class="card border shadow-sm mb-4">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0" id="departmentTreeTable" style="table-layout: fixed; width: 100%;">
                    <thead class="table-light border-bottom text-uppercase text-secondary" style="font-size: 11px; letter-spacing: 0.5px;">
                        <tr>
                            <th style="width: 38%; min-width: 250px;" class="ps-3">Cơ cấu phòng ban / Cấp bậc</th>
                            <th style="width: 20%; min-width: 140px;">Người phụ trách</th>
                            <th style="width: 12%; text-align: center;">Nhân sự</th>
                            <th style="width: 13%; text-align: center;">Tuyển dụng</th>
                            <th style="width: 10%; text-align: center;">Trạng thái</th>
                            <th style="width: 7%; text-align: end;" class="pe-3">Thao tác</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="d" items="${departments}">
                            <tr class="tree-node-row" 
                                data-id="${d.id}" 
                                data-parent-id="${d.parentId != null ? d.parentId : ''}" 
                                data-level="${d.level}"
                                style="${d.level > 0 ? '' : ''}">
                                
                                <!-- Cột Cơ cấu phòng ban & Cây phân cấp -->
                                <td class="ps-2 text-truncate" style="padding-left: ${d.level * 22 + 10}px !important;">
                                    <div class="d-inline-flex align-items-center text-truncate" style="max-width: 100%;">
                                        <!-- Nút đóng/mở nhánh con -->
                                        <c:choose>
                                            <c:when test="${d.hasChildren()}">
                                                <button type="button" class="btn btn-sm btn-link p-0 me-2 text-secondary tree-toggle-btn" 
                                                        data-id="${d.id}" title="Thu gọn/Mở rộng" style="width: 16px; text-decoration: none;">
                                                    <i class="bi bi-dash-square text-primary tree-icon"></i>
                                                </button>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="d-inline-block me-2 text-muted" style="width: 16px; text-align: center;">
                                                    <c:choose>
                                                        <c:when test="${d.level > 0}"><i class="bi bi-arrow-return-right text-muted opacity-50 small"></i></c:when>
                                                        <c:otherwise><i class="bi bi-dot text-muted"></i></c:otherwise>
                                                    </c:choose>
                                                </span>
                                            </c:otherwise>
                                        </c:choose>

                                        <!-- Icon phòng ban -->
                                        <span class="me-2 text-primary opacity-75">
                                            <c:choose>
                                                <c:when test="${d.level == 0}"><i class="bi bi-buildings fs-6"></i></c:when>
                                                <c:when test="${d.level == 1}"><i class="bi bi-building fs-6"></i></c:when>
                                                <c:otherwise><i class="bi bi-folder2 fs-6"></i></c:otherwise>
                                            </c:choose>
                                        </span>

                                        <!-- Mã phòng ban -->
                                        <span class="badge bg-light text-dark border font-monospace me-2 text-xs">${d.code}</span>

                                        <!-- Tên phòng ban & Cấp bậc -->
                                        <span class="fw-semibold text-dark text-truncate me-2" title="${d.name}">${d.name}</span>
                                        
                                        <c:choose>
                                            <c:when test="${d.level == 0}"><span class="badge bg-primary-subtle text-primary border border-primary-subtle text-xs d-none d-xl-inline">Cấp 1</span></c:when>
                                            <c:when test="${d.level == 1}"><span class="badge bg-info-subtle text-info border border-info-subtle text-xs d-none d-xl-inline">Cấp 2</span></c:when>
                                            <c:otherwise><span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle text-xs d-none d-xl-inline">Cấp ${d.level + 1}</span></c:otherwise>
                                        </c:choose>
                                    </div>
                                    <c:if test="${not empty d.description}">
                                        <div class="text-muted text-xs text-truncate ps-4 ms-2 mt-1" style="max-width: 320px;" title="${d.description}">
                                            ${d.description}
                                        </div>
                                    </c:if>
                                </td>

                                <!-- Cột Người phụ trách -->
                                <td class="text-truncate">
                                    <c:choose>
                                        <c:when test="${not empty d.managerName}">
                                            <div class="d-flex align-items-center text-truncate">
                                                <div class="rounded-circle bg-primary text-white d-flex align-items-center justify-content-center me-2 flex-shrink-0 fw-bold" style="width: 28px; height: 28px; font-size: 11px;">
                                                    ${d.managerName.substring(0, 1).toUpperCase()}
                                                </div>
                                                <div class="text-truncate">
                                                    <div class="fw-semibold text-dark text-truncate text-xs" title="${d.managerName}">${d.managerName}</div>
                                                    <div class="text-muted text-xs text-truncate" title="${d.managerJobTitle != null ? d.managerJobTitle : d.managerEmail}">
                                                        ${d.managerJobTitle != null ? d.managerJobTitle : d.managerEmail}
                                                    </div>
                                                </div>
                                            </div>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="text-muted text-xs fst-italic">
                                                <i class="bi bi-person-dash me-1"></i> Chưa chỉ định
                                            </span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>

                                <!-- Cột Nhân sự trực thuộc -->
                                <td class="text-center">
                                    <span class="badge bg-light text-dark border" title="${d.userCount} nhân sự trực thuộc">
                                        <i class="bi bi-people text-muted me-1"></i> ${d.userCount}
                                    </span>
                                </td>

                                <!-- Cột Yêu cầu tuyển dụng -->
                                <td class="text-center">
                                    <c:choose>
                                        <c:when test="${d.openRequisitionCount > 0}">
                                            <span class="badge bg-danger-subtle text-danger border border-danger-subtle" title="${d.openRequisitionCount} yêu cầu đang mở (Không được xóa)">
                                                <i class="bi bi-briefcase-fill me-1"></i> ${d.openRequisitionCount} mở
                                            </span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-light text-muted border text-xs" title="Không có yêu cầu tuyển dụng đang mở">
                                                0 mở
                                            </span>
                                        </c:otherwise>
                                    </c:choose>
                                    <c:if test="${d.totalRequisitionCount > 0}">
                                        <span class="text-muted text-xs ms-1">(${d.totalRequisitionCount})</span>
                                    </c:if>
                                </td>

                                <!-- Cột Trạng thái -->
                                <td class="text-center">
                                    <c:choose>
                                        <c:when test="${d.isActive()}">
                                            <span class="badge bg-success-subtle text-success border border-success-subtle text-xs">
                                                <i class="bi bi-check-circle me-1"></i> Áp dụng
                                            </span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle text-xs">
                                                <i class="bi bi-pause-circle me-1"></i> Ngừng áp dụng
                                            </span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>

                                <!-- Cột Thao tác -->
                                <td class="text-end pe-3">
                                    <div class="dropdown">
                                        <button class="btn btn-sm btn-light border p-1 px-2" type="button" data-bs-toggle="dropdown" aria-expanded="false" title="Tác vụ">
                                            <i class="bi bi-three-dots-vertical"></i>
                                        </button>
                                        <ul class="dropdown-menu dropdown-menu-end shadow-sm border py-1 text-xs">
                                            <li>
                                                <button class="dropdown-item py-1" onclick="openViewDeptModal('${d.id}')">
                                                    <i class="bi bi-eye text-info me-2"></i> Xem chi tiết
                                                </button>
                                            </li>
                                            <li>
                                                <button class="dropdown-item py-1" onclick="openAddChildDeptModal('${d.id}', '${d.code}', '${d.name}')">
                                                    <i class="bi bi-node-plus text-primary me-2"></i> Thêm phòng ban con
                                                </button>
                                            </li>
                                            <li>
                                                <button class="dropdown-item py-1" onclick="openEditDeptModal('${d.id}')">
                                                    <i class="bi bi-pencil text-warning me-2"></i> Chỉnh sửa
                                                </button>
                                            </li>
                                            <li><hr class="dropdown-divider my-1"></li>
                                            <c:choose>
                                                <c:when test="${d.isActive()}">
                                                    <li>
                                                        <form action="${pageContext.request.contextPath}/admin/departments/status" method="POST" class="m-0">
                                                            <input type="hidden" name="id" value="${d.id}">
                                                            <input type="hidden" name="status" value="INACTIVE">
                                                            <button type="submit" class="dropdown-item py-1 text-secondary">
                                                                <i class="bi bi-pause-circle me-2"></i> Ngừng áp dụng
                                                            </button>
                                                        </form>
                                                    </li>
                                                </c:when>
                                                <c:otherwise>
                                                    <li>
                                                        <form action="${pageContext.request.contextPath}/admin/departments/status" method="POST" class="m-0">
                                                            <input type="hidden" name="id" value="${d.id}">
                                                            <input type="hidden" name="status" value="ACTIVE">
                                                            <button type="submit" class="dropdown-item py-1 text-success">
                                                                <i class="bi bi-play-circle me-2"></i> Kích hoạt áp dụng
                                                            </button>
                                                        </form>
                                                    </li>
                                                </c:otherwise>
                                            </c:choose>
                                            <li>
                                                <button class="dropdown-item py-1 text-danger" onclick="triggerDeleteCheck('${d.id}', '${d.name}', '${d.code}')">
                                                    <i class="bi bi-trash me-2"></i> Xóa phòng ban
                                                </button>
                                            </li>
                                        </ul>
                                    </div>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty departments}">
                            <tr>
                                <td colspan="6" class="text-center py-5 text-muted">
                                    <i class="bi bi-diagram-3 fs-1 d-block mb-2 text-secondary opacity-50"></i>
                                    Không tìm thấy phòng ban nào phù hợp với điều kiện tìm kiếm.
                                </td>
                            </tr>
                        </c:if>
                    </tbody>
                </table>
            </div>
        </div>
            </div> <!-- Đóng #pills-table -->
        </div> <!-- Đóng #deptViewTabsContent -->
    </div> <!-- Đóng app-content -->

    <!-- Modal Thêm Phòng Ban Mới -->
    <div class="modal fade" id="addDeptModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow">
                <form action="${pageContext.request.contextPath}/admin/departments/create" method="POST">
                    <div class="modal-header border-bottom">
                        <h5 class="modal-title fw-bold text-dark">
                            <i class="bi bi-building-add text-primary me-2"></i> Thêm phòng ban mới
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body p-3">
                        <div class="row g-3">
                            <div class="col-12 col-md-5">
                                <label class="form-label small fw-semibold text-secondary">Mã phòng ban <span class="text-danger">*</span></label>
                                <input type="text" class="form-control form-control-sm text-uppercase font-monospace" name="code" id="addCode" required placeholder="VD: TECH, DEV">
                            </div>
                            <div class="col-12 col-md-7">
                                <label class="form-label small fw-semibold text-secondary">Tên phòng ban <span class="text-danger">*</span></label>
                                <input type="text" class="form-control form-control-sm" name="name" id="addName" required placeholder="VD: Phòng Phát triển Phần mềm">
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold text-secondary">Phòng ban cấp cha</label>
                                <select class="form-select form-select-sm" name="parentId" id="addParentId">
                                    <option value="">-- Là phòng ban cấp cao nhất (Root) --</option>
                                    <c:forEach var="p" items="${parentOptions}">
                                        <option value="${p.id}">${p.indentedName} (${p.code})</option>
                                    </c:forEach>
                                </select>
                                <div class="form-text text-xs">Nếu chọn phòng ban cha, phòng ban này sẽ trở thành node trực thuộc.</div>
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold text-secondary">Người phụ trách (Manager)</label>
                                <select class="form-select form-select-sm" name="managerId" id="addManagerId">
                                    <option value="">-- Chưa chỉ định người phụ trách --</option>
                                    <c:forEach var="m" items="${managers}">
                                        <option value="${m.id}">${m.fullName} - ${m.email} <c:if test="${not empty m.jobTitle}">(${m.jobTitle})</c:if></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold text-secondary">Trạng thái áp dụng</label>
                                <select class="form-select form-select-sm" name="status">
                                    <option value="ACTIVE" selected>Đang áp dụng</option>
                                    <option value="INACTIVE">Ngừng áp dụng</option>
                                </select>
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold text-secondary">Mô tả chức năng &amp; nhiệm vụ</label>
                                <textarea class="form-control form-control-sm" name="description" rows="2" placeholder="Nêu rõ phạm vi chuyên môn và nhiệm vụ chính của phòng ban..."></textarea>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer border-top bg-light-subtle p-2">
                        <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Hủy bỏ</button>
                        <button type="submit" class="btn btn-primary btn-sm px-3"><i class="bi bi-check-lg me-1"></i> Lưu phòng ban</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Modal Chỉnh Sửa Phòng Ban -->
    <div class="modal fade" id="editDeptModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow">
                <form action="${pageContext.request.contextPath}/admin/departments/edit" method="POST">
                    <input type="hidden" name="id" id="editId">
                    <div class="modal-header border-bottom">
                        <h5 class="modal-title fw-bold text-dark">
                            <i class="bi bi-pencil-square text-warning me-2"></i> Chỉnh sửa phòng ban
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body p-3">
                        <div class="row g-3">
                            <div class="col-12 col-md-5">
                                <label class="form-label small fw-semibold text-secondary">Mã phòng ban <span class="text-danger">*</span></label>
                                <input type="text" class="form-control form-control-sm text-uppercase font-monospace" name="code" id="editCode" required>
                            </div>
                            <div class="col-12 col-md-7">
                                <label class="form-label small fw-semibold text-secondary">Tên phòng ban <span class="text-danger">*</span></label>
                                <input type="text" class="form-control form-control-sm" name="name" id="editName" required>
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold text-secondary">Phòng ban cấp cha</label>
                                <select class="form-select form-select-sm" name="parentId" id="editParentId">
                                    <option value="">-- Là phòng ban cấp cao nhất (Root) --</option>
                                    <!-- Dynamic populated via API để chống tạo vòng lặp -->
                                </select>
                                <div class="form-text text-xs text-muted">Hệ thống tự động ẩn chính phòng ban này và các nhánh con của nó để chống vòng lặp.</div>
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold text-secondary">Người phụ trách (Manager)</label>
                                <select class="form-select form-select-sm" name="managerId" id="editManagerId">
                                    <option value="">-- Chưa chỉ định người phụ trách --</option>
                                    <c:forEach var="m" items="${managers}">
                                        <option value="${m.id}">${m.fullName} - ${m.email} <c:if test="${not empty m.jobTitle}">(${m.jobTitle})</c:if></option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold text-secondary">Trạng thái áp dụng</label>
                                <select class="form-select form-select-sm" name="status" id="editStatus">
                                    <option value="ACTIVE">Đang áp dụng</option>
                                    <option value="INACTIVE">Ngừng áp dụng</option>
                                </select>
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold text-secondary">Mô tả chức năng</label>
                                <textarea class="form-control form-control-sm" name="description" id="editDesc" rows="2"></textarea>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer border-top bg-light-subtle p-2">
                        <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Hủy bỏ</button>
                        <button type="submit" class="btn btn-primary btn-sm px-3"><i class="bi bi-save me-1"></i> Cập nhật thay đổi</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- Modal Xem Chi Tiết Phòng Ban -->
    <div class="modal fade" id="viewDeptModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow">
                <div class="modal-header border-bottom">
                    <h5 class="modal-title fw-bold text-dark">
                        <i class="bi bi-info-circle text-info me-2"></i> Chi tiết phòng ban
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-3">
                    <div class="d-flex align-items-center mb-3">
                        <div class="rounded-circle bg-primary-subtle text-primary p-3 me-3 d-flex align-items-center justify-content-center" style="width: 50px; height: 50px;">
                            <i class="bi bi-building fs-4"></i>
                        </div>
                        <div>
                            <h5 class="fw-bold mb-0 text-dark" id="viewName">Tên phòng ban</h5>
                            <div class="d-flex align-items-center gap-2 mt-1">
                                <span class="badge bg-light text-dark border font-monospace" id="viewCode">CODE</span>
                                <span class="badge" id="viewStatusBadge">Trạng thái</span>
                            </div>
                        </div>
                    </div>
                    <div class="list-group list-group-flush border-top border-bottom small">
                        <div class="list-group-item d-flex justify-content-between px-0 py-2">
                            <span class="text-secondary">Phòng ban cấp trên:</span>
                            <span class="fw-semibold text-dark" id="viewParentName">Root (Cấp cao nhất)</span>
                        </div>
                        <div class="list-group-item d-flex justify-content-between px-0 py-2">
                            <span class="text-secondary">Người phụ trách:</span>
                            <span class="fw-semibold text-dark" id="viewManagerName">Chưa chỉ định</span>
                        </div>
                        <div class="list-group-item d-flex justify-content-between px-0 py-2">
                            <span class="text-secondary">Quy mô nhân sự:</span>
                            <span class="fw-semibold text-dark" id="viewUserCount">0 nhân viên</span>
                        </div>
                        <div class="list-group-item d-flex justify-content-between px-0 py-2">
                            <span class="text-secondary">Yêu cầu tuyển dụng đang mở:</span>
                            <span class="fw-semibold text-danger" id="viewOpenReqCount">0</span>
                        </div>
                        <div class="list-group-item d-flex justify-content-between px-0 py-2">
                            <span class="text-secondary">Tổng số yêu cầu tuyển dụng:</span>
                            <span class="fw-semibold text-dark" id="viewTotalReqCount">0</span>
                        </div>
                        <div class="list-group-item px-0 py-2">
                            <div class="text-secondary mb-1">Mô tả chức năng:</div>
                            <div class="text-dark bg-light p-2 rounded" id="viewDesc">Chưa có mô tả.</div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer border-top bg-light-subtle p-2">
                    <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Đóng</button>
                </div>
            </div>
        </div>
    </div>

    <!-- Modal Kiểm Tra & Xác Nhận Xóa / Ngừng Áp Dụng (QUY TẮC NGHIỆP VỤ BẮT BUỘC) -->
    <div class="modal fade" id="deleteConfirmModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content border-0 shadow">
                <div class="modal-header border-bottom">
                    <h5 class="modal-title fw-bold text-dark" id="deleteModalTitle">
                        <i class="bi bi-shield-exclamation text-warning me-2"></i> Kiểm tra điều kiện xóa phòng ban
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body p-3">
                    <!-- Trạng thái đang tải -->
                    <div id="deleteLoading" class="text-center py-4">
                        <div class="spinner-border text-primary spinner-border-sm" role="status"></div>
                        <div class="text-muted small mt-2">Đang kiểm tra ràng buộc nghiệp vụ trong cơ sở dữ liệu...</div>
                    </div>

                    <!-- TH 1: Đang có yêu cầu tuyển dụng mở => TUYỆT ĐỐI KHÔNG ĐƯỢC XÓA -->
                    <div id="deleteBlockedOpenReqs" class="d-none">
                        <div class="alert alert-danger border-danger p-3 mb-3">
                            <div class="d-flex">
                                <i class="bi bi-x-octagon-fill fs-3 text-danger me-3 flex-shrink-0"></i>
                                <div>
                                    <h6 class="fw-bold mb-1 text-danger">KHÔNG THỂ XÓA PHÒNG BAN!</h6>
                                    <div class="small fw-semibold text-dark mb-2">
                                        Không thể xóa phòng ban vì đang có yêu cầu tuyển dụng mở. Vui lòng ngừng áp dụng phòng ban thay vì xóa.
                                    </div>
                                    <div class="text-muted text-xs">
                                        Hệ thống phát hiện phòng ban này đang có các đợt tuyển dụng đang kích hoạt hoặc chờ phê duyệt. Để đảm bảo toàn vẹn dữ liệu quy trình tuyển sinh/tuyển dụng, bạn chỉ được phép chuyển trạng thái sang <strong>Ngừng áp dụng</strong>.
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- TH 2: Bị chặn bởi các ràng buộc khác (Có phòng ban con, có nhân sự...) -->
                    <div id="deleteBlockedOther" class="d-none">
                        <div class="alert alert-warning border-warning p-3 mb-3">
                            <div class="d-flex">
                                <i class="bi bi-exclamation-triangle-fill fs-3 text-warning me-3 flex-shrink-0"></i>
                                <div>
                                    <h6 class="fw-bold mb-1 text-dark">Chưa thể xóa phòng ban</h6>
                                    <div class="small text-dark" id="deleteBlockedReason">
                                        Lý do ngăn chặn xóa...
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- TH 3: Đủ điều kiện xóa vĩnh viễn -->
                    <div id="deleteAllowed" class="d-none">
                        <div class="alert alert-success border-success p-3 mb-3">
                            <div class="d-flex">
                                <i class="bi bi-check-circle-fill fs-3 text-success me-3 flex-shrink-0"></i>
                                <div>
                                    <h6 class="fw-bold mb-1 text-success">Đủ điều kiện xóa</h6>
                                    <div class="small text-dark">
                                        Phòng ban không có yêu cầu tuyển dụng mở, không có phòng ban con hay ràng buộc dữ liệu. Bạn có chắc chắn muốn xóa vĩnh viễn phòng ban <strong id="deleteAllowedDeptName"></strong>? Thao tác này không thể hoàn tác!
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Footer chứa các nút hành động tương ứng -->
                <div class="modal-footer border-top bg-light-subtle p-2">
                    <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Đóng</button>
                    
                    <!-- Nút Ngừng áp dụng (Xuất hiện khi có yêu cầu tuyển dụng mở hoặc bị chặn xóa) -->
                    <form action="${pageContext.request.contextPath}/admin/departments/status" method="POST" id="formDeactivate" class="d-inline m-0">
                        <input type="hidden" name="id" id="deactivateDeptId">
                        <input type="hidden" name="status" value="INACTIVE">
                        <button type="submit" class="btn btn-warning btn-sm fw-semibold" id="btnDeactivateAction">
                            <i class="bi bi-pause-circle me-1"></i> Ngừng áp dụng phòng ban
                        </button>
                    </form>

                    <!-- Nút Xóa vĩnh viễn (Chỉ xuất hiện khi canDelete == true) -->
                    <form action="${pageContext.request.contextPath}/admin/departments/delete" method="POST" id="formDeleteFinal" class="d-inline m-0">
                        <input type="hidden" name="id" id="deleteFinalDeptId">
                        <button type="submit" class="btn btn-danger btn-sm" id="btnDeleteFinal">
                            <i class="bi bi-trash me-1"></i> Xác nhận xóa
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>

<script>
document.addEventListener('DOMContentLoaded', function () {
    // 1. Quản lý Đóng / Mở nhánh con cây phân cấp
    const toggleBtns = document.querySelectorAll('.tree-toggle-btn');
    toggleBtns.forEach(btn => {
        btn.addEventListener('click', function (e) {
            e.stopPropagation();
            const deptId = this.getAttribute('data-id');
            const icon = this.querySelector('.tree-icon');
            const isExpanded = icon.classList.contains('bi-dash-square');

            toggleSubtree(deptId, !isExpanded);

            if (isExpanded) {
                icon.classList.remove('bi-dash-square');
                icon.classList.add('bi-plus-square');
            } else {
                icon.classList.remove('bi-plus-square');
                icon.classList.add('bi-dash-square');
            }
        });
    });

    function toggleSubtree(parentId, show) {
        const directChildren = document.querySelectorAll(`tr.tree-node-row[data-parent-id="${parentId}"]`);
        directChildren.forEach(childRow => {
            if (show) {
                childRow.style.display = '';
                // Nếu con cũng đang mở thì hiển thị luôn cháu
                const childId = childRow.getAttribute('data-id');
                const childBtn = childRow.querySelector('.tree-toggle-btn .tree-icon');
                if (childBtn && childBtn.classList.contains('bi-dash-square')) {
                    toggleSubtree(childId, true);
                }
            } else {
                childRow.style.display = 'none';
                const childId = childRow.getAttribute('data-id');
                toggleSubtree(childId, false);
            }
        });
    }

    // Nút Mở rộng tất cả
    document.getElementById('btnExpandAll').addEventListener('click', function () {
        document.querySelectorAll('tr.tree-node-row').forEach(row => row.style.display = '');
        document.querySelectorAll('.tree-toggle-btn .tree-icon').forEach(icon => {
            icon.classList.remove('bi-plus-square');
            icon.classList.add('bi-dash-square');
        });
    });

    // Nút Thu gọn tất cả (chỉ giữ root level 0)
    document.getElementById('btnCollapseAll').addEventListener('click', function () {
        document.querySelectorAll('tr.tree-node-row').forEach(row => {
            const level = parseInt(row.getAttribute('data-level') || '0', 10);
            if (level > 0) {
                row.style.display = 'none';
            } else {
                row.style.display = '';
            }
        });
        document.querySelectorAll('.tree-toggle-btn .tree-icon').forEach(icon => {
            icon.classList.remove('bi-dash-square');
            icon.classList.add('bi-plus-square');
        });
    });
});

// Thêm phòng ban con nhanh từ dropdown dòng
function openAddChildDeptModal(parentId, parentCode, parentName) {
    document.getElementById('addParentId').value = parentId;
    document.getElementById('addCode').value = parentCode + '-';
    document.getElementById('addName').value = '';
    new bootstrap.Modal(document.getElementById('addDeptModal')).show();
}

// Xem chi tiết phòng ban qua AJAX
function openViewDeptModal(id) {
    fetch('${pageContext.request.contextPath}/admin/departments/api/detail?id=' + encodeURIComponent(id))
        .then(res => res.json())
        .then(dept => {
            document.getElementById('viewCode').innerText = dept.code || '';
            document.getElementById('viewName').innerText = dept.name || '';
            document.getElementById('viewParentName').innerText = dept.parentName ? (dept.parentName + ' (' + dept.parentCode + ')') : 'Root (Cấp cao nhất)';
            document.getElementById('viewManagerName').innerText = dept.managerName ? (dept.managerName + (dept.managerJobTitle ? ' - ' + dept.managerJobTitle : '')) : 'Chưa chỉ định';
            document.getElementById('viewUserCount').innerText = (dept.userCount || 0) + ' nhân viên';
            document.getElementById('viewOpenReqCount').innerText = dept.openRequisitionCount || 0;
            document.getElementById('viewTotalReqCount').innerText = (dept.totalRequisitionCount || 0) + ' đợt tuyển dụng';
            document.getElementById('viewDesc').innerText = dept.description || 'Chưa có mô tả chức năng.';

            const badge = document.getElementById('viewStatusBadge');
            if (dept.status === 'ACTIVE') {
                badge.className = 'badge bg-success-subtle text-success border border-success-subtle';
                badge.innerText = 'Đang áp dụng';
            } else {
                badge.className = 'badge bg-secondary-subtle text-secondary border border-secondary-subtle';
                badge.innerText = 'Ngừng áp dụng';
            }

            new bootstrap.Modal(document.getElementById('viewDeptModal')).show();
        })
        .catch(err => {
            alert('Lỗi tải thông tin phòng ban: ' + err);
        });
}

// Chỉnh sửa phòng ban: Tự động tải danh sách cha hợp lệ (chống vòng lặp)
function openEditDeptModal(id) {
    // 1. Tải danh sách cha hợp lệ loại trừ node này và con cháu nó
    fetch('${pageContext.request.contextPath}/admin/departments/api/parents?excludeId=' + encodeURIComponent(id))
        .then(res => res.json())
        .then(parents => {
            const select = document.getElementById('editParentId');
            select.innerHTML = '<option value="">-- Là phòng ban cấp cao nhất (Root) --</option>';
            parents.forEach(p => {
                const opt = document.createElement('option');
                opt.value = p.id;
                opt.textContent = p.indentedName + ' (' + p.code + ')';
                select.appendChild(opt);
            });

            // 2. Tải thông tin chi tiết của phòng ban cần sửa
            return fetch('${pageContext.request.contextPath}/admin/departments/api/detail?id=' + encodeURIComponent(id));
        })
        .then(res => res.json())
        .then(dept => {
            document.getElementById('editId').value = dept.id;
            document.getElementById('editCode').value = dept.code;
            document.getElementById('editName').value = dept.name;
            document.getElementById('editDesc').value = dept.description || '';
            document.getElementById('editParentId').value = dept.parentId || '';
            document.getElementById('editManagerId').value = dept.managerId || '';
            document.getElementById('editStatus').value = dept.status || 'ACTIVE';

            new bootstrap.Modal(document.getElementById('editDeptModal')).show();
        })
        .catch(err => {
            alert('Lỗi khởi tạo chỉnh sửa phòng ban: ' + err);
        });
}

// Kiểm tra điều kiện xóa phòng ban (QUY TẮC NGHIỆP VỤ BẮT BUỘC)
function triggerDeleteCheck(deptId, deptName, deptCode) {
    const modalEl = document.getElementById('deleteConfirmModal');
    const modal = new bootstrap.Modal(modalEl);

    // Reset giao diện về trạng thái loading
    document.getElementById('deleteLoading').classList.remove('d-none');
    document.getElementById('deleteBlockedOpenReqs').classList.add('d-none');
    document.getElementById('deleteBlockedOther').classList.add('d-none');
    document.getElementById('deleteAllowed').classList.add('d-none');
    document.getElementById('btnDeactivateAction').classList.add('d-none');
    document.getElementById('btnDeleteFinal').classList.add('d-none');

    document.getElementById('deactivateDeptId').value = deptId;
    document.getElementById('deleteFinalDeptId').value = deptId;
    document.getElementById('deleteAllowedDeptName').innerText = deptName + ' (' + deptCode + ')';

    modal.show();

    // Gọi Backend kiểm tra nghiệp vụ thời gian thực
    fetch('${pageContext.request.contextPath}/admin/departments/api/check-delete?id=' + encodeURIComponent(deptId))
        .then(res => res.json())
        .then(data => {
            document.getElementById('deleteLoading').classList.add('d-none');

            if (data.hasOpenRequisitions) {
                // TRƯỜNG HỢP QUAN TRỌNG NHẤT: Đang có yêu cầu tuyển dụng mở -> TUYỆT ĐỐI KHÔNG XÓA
                document.getElementById('deleteBlockedOpenReqs').classList.remove('d-none');
                document.getElementById('btnDeactivateAction').classList.remove('d-none');
                document.getElementById('btnDeleteFinal').classList.add('d-none');
            } else if (!data.canDelete) {
                // TRƯỜNG HỢP CÓ RÀNG BUỘC KHÁC: Có con, có nhân sự
                document.getElementById('deleteBlockedOther').classList.remove('d-none');
                document.getElementById('deleteBlockedReason').innerText = data.message;
                document.getElementById('btnDeactivateAction').classList.remove('d-none');
                document.getElementById('btnDeleteFinal').classList.add('d-none');
            } else {
                // ĐỦ ĐIỀU KIỆN XÓA
                document.getElementById('deleteAllowed').classList.remove('d-none');
                document.getElementById('btnDeleteFinal').classList.remove('d-none');
                document.getElementById('btnDeactivateAction').classList.add('d-none');
            }
        })
        .catch(err => {
            document.getElementById('deleteLoading').classList.add('d-none');
            alert('Lỗi kiểm tra dữ liệu từ máy chủ: ' + err);
        });
}

    // --- Xử lý Thu gọn / Mở rộng cây phòng ban (Org Tree Collapse / Expand) ---
    window.toggleOrgNode = function(badgeEl, event) {
        if (event) event.stopPropagation();
        const li = badgeEl.closest('li');
        if (!li) return;
        const childUl = li.querySelector(':scope > ul');
        if (!childUl) return;

        li.classList.toggle('is-collapsed');
        const isCollapsed = li.classList.contains('is-collapsed');
        const count = badgeEl.getAttribute('data-count') || badgeEl.textContent.trim().replace(/[^0-9]/g, '');
        badgeEl.setAttribute('data-count', count);

        if (isCollapsed) {
            badgeEl.innerHTML = '<i class="bi bi-plus-circle me-1"></i>' + count;
            badgeEl.classList.add('collapsed-badge');
            badgeEl.title = 'Nhấn để mở rộng (' + count + ' phòng ban trực thuộc)';
        } else {
            badgeEl.innerHTML = '<i class="bi bi-dash-circle me-1"></i>' + count;
            badgeEl.classList.remove('collapsed-badge');
            badgeEl.title = 'Nhấn để thu gọn (' + count + ' phòng ban trực thuộc)';
        }
    };

    // Nút mở rộng toàn bộ cây
    document.getElementById('btnExpandAll')?.addEventListener('click', function() {
        document.querySelectorAll('.org-tree li.is-collapsed').forEach(li => {
            li.classList.remove('is-collapsed');
            const badge = li.querySelector(':scope > .org-node-card .org-node-badge');
            if (badge) {
                const count = badge.getAttribute('data-count') || badge.textContent.trim().replace(/[^0-9]/g, '');
                badge.innerHTML = '<i class="bi bi-dash-circle me-1"></i>' + count;
                badge.classList.remove('collapsed-badge');
                badge.title = 'Nhấn để thu gọn (' + count + ' phòng ban trực thuộc)';
            }
        });
    });

    // Nút thu gọn toàn bộ cây
    document.getElementById('btnCollapseAll')?.addEventListener('click', function() {
        document.querySelectorAll('.org-tree li').forEach(li => {
            const childUl = li.querySelector(':scope > ul');
            if (childUl) {
                li.classList.add('is-collapsed');
                const badge = li.querySelector(':scope > .org-node-card .org-node-badge');
                if (badge) {
                    const count = badge.getAttribute('data-count') || badge.textContent.trim().replace(/[^0-9]/g, '');
                    badge.setAttribute('data-count', count);
                    badge.innerHTML = '<i class="bi bi-plus-circle me-1"></i>' + count;
                    badge.classList.add('collapsed-badge');
                    badge.title = 'Nhấn để mở rộng (' + count + ' phòng ban trực thuộc)';
                }
            }
        });
    });

    // --- Xử lý Thu phóng cây phòng ban (Zoom In / Zoom Out / Reset) ---
    let currentTreeZoom = 1.0;
    function updateTreeZoom(newZoom) {
        currentTreeZoom = Math.max(0.3, Math.min(2.0, Math.round(newZoom * 10) / 10));
        const tree = document.querySelector('.org-tree');
        if (tree) {
            tree.style.transform = 'scale(' + currentTreeZoom + ')';
            tree.style.transformOrigin = 'top center';
            tree.style.transition = 'transform 0.2s ease-out';
        }
        const display = document.getElementById('zoomLevelDisplay');
        if (display) {
            display.innerText = Math.round(currentTreeZoom * 100) + '%';
        }
    }

    document.getElementById('btnZoomIn')?.addEventListener('click', () => updateTreeZoom(currentTreeZoom + 0.1));
    document.getElementById('btnZoomOut')?.addEventListener('click', () => updateTreeZoom(currentTreeZoom - 0.1));
    document.getElementById('btnZoomReset')?.addEventListener('click', () => updateTreeZoom(1.0));

    // Hỗ trợ cuộn chuột lăn để phóng to/thu nhỏ khi giữ phím Ctrl
    const treeWrapper = document.querySelector('.org-chart-wrapper');
    if (treeWrapper) {
        treeWrapper.addEventListener('wheel', function(e) {
            if (e.ctrlKey) {
                e.preventDefault();
                if (e.deltaY < 0) {
                    updateTreeZoom(currentTreeZoom + 0.05);
                } else {
                    updateTreeZoom(currentTreeZoom - 0.05);
                }
            }
        }, { passive: false });
    }
</script>

<style>
.org-tree li.is-collapsed > ul {
    display: none !important;
}
.org-node-badge.interactive-toggle {
    cursor: pointer !important;
    transition: all 0.2s ease;
    user-select: none;
}
.org-node-badge.interactive-toggle:hover {
    transform: translateX(-50%) scale(1.15);
    background-color: #2563eb !important;
    box-shadow: 0 4px 10px rgba(37, 99, 235, 0.5);
}
.org-node-badge.collapsed-badge {
    background-color: #d97706 !important;
    border-color: #f59e0b !important;
    box-shadow: 0 2px 8px rgba(217, 119, 6, 0.4);
}
.org-chart-wrapper {
    overflow: auto;
    position: relative;
    border-radius: 8px;
    min-height: 480px;
}
</style>

<jsp:include page="../common/footer.jsp" />

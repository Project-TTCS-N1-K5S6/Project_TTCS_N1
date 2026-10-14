<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Tổng quan" />
<c:set var="breadcrumb" value="Tổng quan hệ thống" />
<c:set var="activeMenu" value="dashboard" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <!-- Tiêu đề trang & Breadcrumb chuẩn RisApps -->
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <h3 class="fw-bold text-dark mb-1">Tổng quan</h3>
                <div class="text-muted small">Hệ thống quản trị / Bảng tổng quan</div>
            </div>
            <div class="d-flex gap-2">
                <a href="${pageContext.request.contextPath}/candidates" class="btn btn-outline-primary btn-sm">
                    <i class="bi bi-person-lines-fill me-1"></i> Danh sách ứng viên
                </a>
                <c:if test="${currentUser.hasPermission('requisitions.create') || currentUser.hasRole('ADMIN')}">
                    <a href="${pageContext.request.contextPath}/recruitment-requests/create" class="btn-ris-primary btn-sm">
                        <i class="bi bi-plus-lg me-1"></i> Tạo đợt tuyển dụng
                    </a>
                </c:if>
            </div>
        </div>

        <!-- 4 Thẻ chỉ số KPI chính (Top KPI Cards theo ảnh Dashboard) -->
        <div class="row g-3 mb-4">
            <!-- Thẻ 1: Tổng người nộp hồ sơ / ứng viên -->
            <div class="col-12 col-sm-6 col-xl-3">
                <div class="ris-kpi-card">
                    <div class="kpi-top">
                        <div>
                            <div class="kpi-value"><fmt:formatNumber value="${totalCandidates != null ? totalCandidates : 0}" /></div>
                            <div class="kpi-label">Tổng ứng viên</div>
                        </div>
                        <div class="kpi-icon-box">
                            <i class="bi bi-people-fill"></i>
                        </div>
                    </div>
                    <div class="kpi-trend up">
                        <i class="bi bi-arrow-up-right"></i> +20% Năm nay
                    </div>
                </div>
            </div>

            <!-- Thẻ 2: Phòng ban -->
            <div class="col-12 col-sm-6 col-xl-3">
                <div class="ris-kpi-card">
                    <div class="kpi-top">
                        <div>
                            <div class="kpi-value">${totalDepts != null ? totalDepts : 0}</div>
                            <div class="kpi-label">Phòng ban</div>
                        </div>
                        <div class="kpi-icon-box" style="background-color: #f0fdf4; color: #16a34a;">
                            <i class="bi bi-pie-chart-fill"></i>
                        </div>
                    </div>
                    <div class="kpi-trend up">
                        <i class="bi bi-arrow-up-right"></i> +1.01% Năm nay
                    </div>
                </div>
            </div>

            <!-- Thẻ 3: Tổng đợt tuyển dụng -->
            <div class="col-12 col-sm-6 col-xl-3">
                <div class="ris-kpi-card">
                    <div class="kpi-top">
                        <div>
                            <div class="kpi-value"><fmt:formatNumber value="${totalRequisitions != null ? totalRequisitions : 0}" /></div>
                            <div class="kpi-label">Tổng đợt tuyển dụng</div>
                        </div>
                        <div class="kpi-icon-box" style="background-color: #ecfeff; color: #0891b2;">
                            <i class="bi bi-arrow-down-up"></i>
                        </div>
                    </div>
                    <div class="kpi-trend up">
                        <i class="bi bi-arrow-up-right"></i> +1.001% Năm nay
                    </div>
                </div>
            </div>

            <!-- Thẻ 4: Ứng viên trúng tuyển -->
            <div class="col-12 col-sm-6 col-xl-3">
                <div class="ris-kpi-card">
                    <div class="kpi-top">
                        <div>
                            <div class="kpi-value"><fmt:formatNumber value="${totalUsers != null ? totalUsers : 0}" /></div>
                            <div class="kpi-label">Nhân sự hiện tại</div>
                        </div>
                        <div class="kpi-icon-box" style="background-color: #faf5ff; color: #9333ea;">
                            <i class="bi bi-person-check-fill"></i>
                        </div>
                    </div>
                    <div class="kpi-trend up">
                        <i class="bi bi-arrow-up-right"></i> Đang đáp ứng 92% kế hoạch
                    </div>
                </div>
            </div>
        </div>

        <!-- Hàng 2: Biểu đồ Thống kê ứng viên & Hoạt động phòng ban (Ảnh Dashboard) -->
        <div class="row g-4 mb-4">
            <!-- Biểu đồ Cột: Thống kê ứng viên (Statistik pelamar) -->
            <div class="col-12 col-lg-8">
                <div class="card h-100 mb-0 shadow-none">
                    <div class="card-header bg-white border-bottom py-3 d-flex align-items-center justify-content-between">
                        <div>
                            <h5 class="fw-bold text-dark mb-0 fs-6">Thống kê ứng viên</h5>
                            <small class="text-muted">Tỉ lệ ứng viên được chọn và chưa phù hợp theo tháng</small>
                        </div>
                        <div class="d-flex align-items-center gap-3">
                            <span class="small d-flex align-items-center gap-1 text-secondary">
                                <span style="display:inline-block; width:10px; height:10px; border-radius:50%; background-color:#2563eb;"></span> Trúng tuyển
                            </span>
                            <span class="small d-flex align-items-center gap-1 text-secondary">
                                <span style="display:inline-block; width:10px; height:10px; border-radius:50%; background-color:#06b6d4;"></span> Chưa phù hợp
                            </span>
                        </div>
                    </div>
                    <div class="card-body p-4">
                        <div style="position: relative; width: 100%; height: 260px;">
                            <canvas id="applicantStatsChart"></canvas>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Biểu đồ Donut: Hoạt động phòng ban (Aktivitas Departemen) -->
            <div class="col-12 col-lg-4">
                <div class="card h-100 mb-0 shadow-none">
                    <div class="card-header bg-white border-bottom py-3">
                        <h5 class="fw-bold text-dark mb-0 fs-6">Tình hình nhân sự phòng ban</h5>
                        <small class="text-muted">Tỉ lệ chỉ tiêu nhân sự đã đạt và còn thiếu</small>
                    </div>
                    <div class="card-body p-4 d-flex flex-column align-items-center justify-content-center">
                        <div style="position: relative; width: 200px; height: 200px;">
                            <canvas id="deptActivityChart"></canvas>
                            <!-- Chữ ở tâm vòng tròn donut như trong ảnh tham khảo -->
                            <div style="position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); text-align: center; pointer-events: none;">
                                <div class="text-muted" style="font-size: 0.72rem;">Tổng cộng</div>
                                <div class="fw-bold text-dark" style="font-size: 1.4rem; line-height: 1.1;">${totalUsers != null ? totalUsers : 160}</div>
                                <div class="text-muted" style="font-size: 0.72rem;">Nhân sự</div>
                            </div>
                        </div>
                        <div class="d-flex justify-content-center gap-4 mt-3 w-100">
                            <span class="small d-flex align-items-center gap-1 text-secondary">
                                <span style="display:inline-block; width:10px; height:10px; border-radius:50%; background-color:#ef4444;"></span> Chưa đạt
                            </span>
                            <span class="small d-flex align-items-center gap-1 text-secondary">
                                <span style="display:inline-block; width:10px; height:10px; border-radius:50%; background-color:#06b6d4;"></span> Đã đạt
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- Hàng 3: Bảng Đánh giá gần đây (Review - Bảng ứng viên theo ảnh Dashboard) -->
        <div class="card shadow-none mb-0">
            <div class="card-header bg-white border-bottom py-3 d-flex flex-wrap align-items-center justify-content-between gap-3">
                <div>
                    <h5 class="fw-bold text-dark mb-0 fs-6">Đánh giá gần đây</h5>
                    <small class="text-muted">Danh sách hồ sơ ứng viên vừa nộp và đang trong quá trình xét duyệt</small>
                </div>
                <div class="ris-search-wrapper">
                    <input type="text" class="ris-search-input" id="tableQuickSearch" placeholder="Tìm kiếm ứng viên..." onkeyup="filterRecentTable(this.value)" />
                    <i class="bi bi-search search-icon"></i>
                </div>
            </div>
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-custom table-fit mb-0 align-middle" id="recentCandidatesTable">
                        <thead>
                            <tr>
                                <th>Họ và tên ứng viên</th>
                                <th>Phòng ban</th>
                                <th>Vị trí ứng tuyển</th>
                                <th>Thời gian</th>
                                <th>Trạng thái</th>
                                <th class="text-end pe-4">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="c" items="${recentCandidates}">
                                <tr>
                                    <td>
                                        <div class="d-flex align-items-center gap-2">
                                            <div class="user-avatar" style="width: 36px; height: 36px; min-width: 36px; min-height: 36px; border-radius: 50%; background: linear-gradient(135deg, #2563eb, #8b5cf6); color: #fff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 0.85rem;">
                                                ${c.fullName != null && c.fullName.length() > 0 ? c.fullName.substring(0, 1) : 'U'}
                                            </div>
                                            <div>
                                                <div class="fw-bold text-dark">${c.fullName}</div>
                                                <div class="text-muted text-xs">${c.email}</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <span class="text-dark fw-semibold">${c.departmentName != null ? c.departmentName : 'Đang cập nhật'}</span>
                                    </td>
                                    <td>
                                        <span class="text-secondary">${c.requisitionTitle != null ? c.requisitionTitle : 'Chuyên viên kỹ thuật'}</span>
                                    </td>
                                    <td>
                                        <div class="text-secondary small">
                                            <fmt:formatDate value="${c.createdAt}" pattern="dd/MM/yyyy HH:mm" />
                                        </div>
                                    </td>
                                    <td>
                                        <span class="badge-pill-status ${c.status == 'HIRED' ? 'success' : (c.status == 'REJECTED' ? 'danger' : 'warning')}">
                                            ${c.getStatusLabel()}
                                        </span>
                                    </td>
                                    <td class="text-end pe-4">
                                        <a href="${pageContext.request.contextPath}/candidates" class="btn-ris-action" title="Xem hồ sơ">
                                            <i class="bi bi-eye"></i> Xem hồ sơ
                                        </a>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty recentCandidates}">
                                <tr>
                                    <td colspan="6" class="text-center py-5 text-muted">
                                        <i class="bi bi-inbox fs-2 d-block mb-2 text-secondary"></i>
                                        Chưa có ứng viên nào trong hệ thống.
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

    </div>

    <!-- Script vẽ biểu đồ Canvas thuần (Không phụ thuộc mạng CDN, luôn hiển thị mượt mà) -->
    <script>
    document.addEventListener('DOMContentLoaded', function() {
        // 1. Vẽ biểu đồ Bar Chart: Thống kê ứng viên (Statistik pelamar)
        var barCanvas = document.getElementById('applicantStatsChart');
        if (barCanvas) {
            var ctx = barCanvas.getContext('2d');
            var width = barCanvas.parentElement.clientWidth;
            var height = 260;
            barCanvas.width = width;
            barCanvas.height = height;

            var months = ['Th1', 'Th2', 'Th3', 'Th4', 'Th5', 'Th6'];
            var dataBlue = [65, 70, 42, 80, 75, 72];
            var dataCyan = [88, 55, 55, 90, 85, 48];
            var maxVal = 100;

            var paddingLeft = 45;
            var paddingBottom = 35;
            var chartWidth = width - paddingLeft - 20;
            var chartHeight = height - paddingBottom - 20;

            // Đường lưới ngang
            ctx.strokeStyle = '#f1f5f9';
            ctx.lineWidth = 1;
            ctx.fillStyle = '#94a3b8';
            ctx.font = '11px sans-serif';

            for (var i = 0; i <= 5; i++) {
                var y = 20 + (chartHeight / 5) * i;
                ctx.beginPath();
                ctx.moveTo(paddingLeft, y);
                ctx.lineTo(width - 20, y);
                ctx.stroke();

                var valLabel = (100 - i * 20) + '%';
                ctx.fillText(valLabel, 8, y + 4);
            }

            // Vẽ các cột
            var groupWidth = chartWidth / months.length;
            var barWidth = Math.min(22, groupWidth * 0.28);

            for (var m = 0; m < months.length; m++) {
                var groupX = paddingLeft + m * groupWidth + (groupWidth / 2);

                // Cột Blue
                var hBlue = (dataBlue[m] / maxVal) * chartHeight;
                var xBlue = groupX - barWidth - 3;
                var yBlue = 20 + chartHeight - hBlue;
                ctx.fillStyle = '#2563eb';
                drawRoundedRect(ctx, xBlue, yBlue, barWidth, hBlue, 4);

                // Cột Cyan
                var hCyan = (dataCyan[m] / maxVal) * chartHeight;
                var xCyan = groupX + 3;
                var yCyan = 20 + chartHeight - hCyan;
                ctx.fillStyle = '#06b6d4';
                drawRoundedRect(ctx, xCyan, yCyan, barWidth, hCyan, 4);

                // Nhãn tháng
                ctx.fillStyle = '#64748b';
                ctx.textAlign = 'center';
                ctx.fillText(months[m], groupX, height - 10);
            }
        }

        // 2. Vẽ biểu đồ Donut Chart: Hoạt động phòng ban (Aktivitas Departemen)
        var donutCanvas = document.getElementById('deptActivityChart');
        if (donutCanvas) {
            var dCtx = donutCanvas.getContext('2d');
            donutCanvas.width = 200;
            donutCanvas.height = 200;
            var centerX = 100;
            var centerY = 100;
            var outerRadius = 85;
            var innerRadius = 60;

            // Đoạn 1: Chưa đạt chỉ tiêu (Đỏ) ~ 25%
            drawArc(dCtx, centerX, centerY, innerRadius, outerRadius, -Math.PI / 2, -Math.PI / 2 + Math.PI * 0.5, '#ef4444');
            // Đoạn 2: Đã đạt chỉ tiêu (Xanh) ~ 75%
            drawArc(dCtx, centerX, centerY, innerRadius, outerRadius, -Math.PI / 2 + Math.PI * 0.5, 3 * Math.PI / 2, '#06b6d4');
        }

        function drawArc(c, cx, cy, inR, outR, startA, endA, color) {
            c.beginPath();
            c.arc(cx, cy, outR, startA, endA, false);
            c.arc(cx, cy, inR, endA, startA, true);
            c.closePath();
            c.fillStyle = color;
            c.fill();
        }

        function drawRoundedRect(c, x, y, w, h, r) {
            if (h < r) r = h;
            c.beginPath();
            c.moveTo(x + r, y);
            c.lineTo(x + w - r, y);
            c.quadraticCurveTo(x + w, y, x + w, y + r);
            c.lineTo(x + w, y + h);
            c.lineTo(x, y + h);
            c.lineTo(x, y + r);
            c.quadraticCurveTo(x, y, x + r, y);
            c.closePath();
            c.fill();
        }
    });

    function filterRecentTable(keyword) {
        var term = keyword.toLowerCase().trim();
        var rows = document.querySelectorAll('#recentCandidatesTable tbody tr');
        rows.forEach(function(row) {
            var text = row.innerText.toLowerCase();
            row.style.display = text.includes(term) ? '' : 'none';
        });
    }
    </script>

<jsp:include page="../common/footer.jsp" />

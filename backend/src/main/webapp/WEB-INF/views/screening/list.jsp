<%--
  ==============================================================================
  DANH SÁCH TIÊU CHÍ SÀNG LỌC & CHẤM ĐIỂM HỒ SƠ (screening/list.jsp)
  Chức năng:
  - Hiển thị danh sách các vị trí tuyển dụng đã cấu hình tiêu chí chấm điểm
  - Cho phép chấm điểm hàng loạt hoặc xoá tiêu chí
  ==============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Chấm điểm & Xếp hạng hồ sơ tự động" />
<c:set var="breadcrumb" value="Chấm điểm hồ sơ" />
<c:set var="activeMenu" value="screening" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<style>
/* ===== SCORING BADGE STYLES ===== */
.badge-pass {
    display: inline-flex; align-items: center; gap: 4px;
    background: linear-gradient(135deg, #d1fae5, #a7f3d0);
    color: #065f46; border: 1px solid #6ee7b7;
    padding: 3px 10px; border-radius: 20px; font-size: 0.72rem; font-weight: 700;
}
.badge-fail {
    display: inline-flex; align-items: center; gap: 4px;
    background: linear-gradient(135deg, #fee2e2, #fecaca);
    color: #991b1b; border: 1px solid #fca5a5;
    padding: 3px 10px; border-radius: 20px; font-size: 0.72rem; font-weight: 700;
}
.badge-pending {
    display: inline-flex; align-items: center; gap: 4px;
    background: #f3f4f6; color: #6b7280; border: 1px solid #d1d5db;
    padding: 3px 10px; border-radius: 20px; font-size: 0.72rem; font-weight: 600;
}
.score-high   { background: linear-gradient(135deg,#d1fae5,#a7f3d0); color:#065f46; }
.score-medium { background: linear-gradient(135deg,#fef3c7,#fde68a); color:#92400e; }
.score-low    { background: linear-gradient(135deg,#fee2e2,#fecaca); color:#991b1b; }
.score-pending{ background: #f3f4f6; color: #9ca3af; }
.score-badge {
    display: inline-flex; align-items: center; justify-content: center;
    min-width: 44px; padding: 3px 10px; border-radius: 20px;
    font-size: 0.8rem; font-weight: 700; border: 1px solid rgba(0,0,0,.08);
}
.criteria-card {
    background: #fff;
    border: 1px solid #e5e7eb;
    border-radius: 14px;
    padding: 20px 22px;
    transition: box-shadow .18s, transform .18s;
}
.criteria-card:hover {
    box-shadow: 0 8px 28px rgba(79,70,229,.10);
    transform: translateY(-2px);
}
.criteria-card .req-title {
    font-size: 1rem; font-weight: 700; color: #1e293b; margin-bottom: 4px;
}
.criteria-tag {
    display: inline-flex; align-items: center; gap: 4px;
    background: #eff6ff; color: #2563eb; border: 1px solid #bfdbfe;
    border-radius: 8px; padding: 2px 8px; font-size: 0.72rem; font-weight: 600;
    margin: 2px;
}
.criteria-tag.must { background:#fdf4ff; color:#7c3aed; border-color:#ddd6fe; }
.criteria-tag.nice { background:#f0fdf4; color:#16a34a; border-color:#bbf7d0; }
.badge-priority-high   { background:linear-gradient(135deg,#4f46e5,#7c3aed); color:#fff; border-radius:12px; padding:3px 10px; font-size:.72rem; font-weight:700; }
.badge-priority-medium { background:linear-gradient(135deg,#f59e0b,#d97706); color:#fff; border-radius:12px; padding:3px 10px; font-size:.72rem; font-weight:700; }
.badge-priority-review { background:#f3f4f6; color:#6b7280; border:1px solid #d1d5db; border-radius:12px; padding:3px 10px; font-size:.72rem; font-weight:700; }
.weight-bar {
    display: flex; height: 8px; border-radius: 999px; overflow: hidden; gap: 2px;
}
.weight-bar .w-exp  { background: #4f46e5; }
.weight-bar .w-skill{ background: #7c3aed; }
.weight-bar .w-edu  { background: #0ea5e9; }
</style>

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />
    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <%-- Tiêu đề trang --%>
        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <h3 class="fw-bold text-dark mb-1">
                    <i class="bi bi-award-fill me-2 text-indigo"></i>Chấm điểm & Xếp hạng hồ sơ tự động
                </h3>
                <div class="text-muted small">
                    Hệ thống quản trị / Chấm điểm hồ sơ &nbsp;·&nbsp;
                    Điểm chỉ để gợi ý xếp hạng, <strong>không tự động loại</strong> ứng viên
                </div>
            </div>
            <a href="${pageContext.request.contextPath}/screening-criteria/setup"
               class="btn-ris-primary" id="btn-add-criteria">
                <i class="bi bi-plus-circle-fill me-1"></i> Cấu hình tiêu chí mới
            </a>
        </div>

        <%-- Banner hướng dẫn --%>
        <div class="alert d-flex align-items-start gap-3 border shadow-sm mb-4"
             style="background:linear-gradient(135deg,#eff6ff,#dbeafe);border-color:#bfdbfe!important;border-radius:12px;">
            <div class="flex-shrink-0 mt-1">
                <div class="rounded-circle d-flex align-items-center justify-content-center"
                     style="width:38px;height:38px;background:rgba(79,70,229,.12);">
                    <i class="bi bi-info-circle-fill text-primary"></i>
                </div>
            </div>
            <div>
                <div class="fw-bold text-primary-emphasis mb-1">Cách hoạt động</div>
                <div class="small text-secondary">
                    <b>1. Khai báo tiêu chí</b> cho từng vị trí: số năm kinh nghiệm, bằng cấp, kỹ năng bắt buộc &amp; kỹ năng cộng điểm.<br>
                    <b>2. Chấm điểm hàng loạt</b> → Hệ thống tính điểm phù hợp (0–100) và gắn cờ <b>ĐẠT / KHÔNG ĐẠT</b> tiêu chí bắt buộc.<br>
                    <b>3. Xem kết quả</b> trong danh sách ứng viên → Sắp xếp theo điểm để ưu tiên xem xét.
                </div>
            </div>
        </div>

        <%-- Thang điểm minh họa --%>
        <div class="row g-3 mb-4">
            <div class="col-12 col-md-4">
                <div class="criteria-card text-center">
                    <div style="font-size:2rem; color:#4f46e5;" class="mb-1"><i class="bi bi-briefcase-fill"></i></div>
                    <div class="fw-bold text-dark">Kinh nghiệm</div>
                    <div style="font-size:2rem;font-weight:800;color:#4f46e5;">40</div>
                    <div class="text-muted small">điểm tối đa</div>
                </div>
            </div>
            <div class="col-12 col-md-4">
                <div class="criteria-card text-center">
                    <div style="font-size:2rem; color:#7c3aed;" class="mb-1"><i class="bi bi-tools"></i></div>
                    <div class="fw-bold text-dark">Kỹ năng chuyên môn</div>
                    <div style="font-size:2rem;font-weight:800;color:#7c3aed;">40</div>
                    <div class="text-muted small">điểm tối đa</div>
                </div>
            </div>
            <div class="col-12 col-md-4">
                <div class="criteria-card text-center">
                    <div style="font-size:2rem; color:#0ea5e9;" class="mb-1"><i class="bi bi-mortarboard-fill"></i></div>
                    <div class="fw-bold text-dark">Bằng cấp & Chứng chỉ</div>
                    <div style="font-size:2rem;font-weight:800;color:#0ea5e9;">20</div>
                    <div class="text-muted small">điểm tối đa</div>
                </div>
            </div>
        </div>

        <%-- Danh sách tiêu chí đã cấu hình --%>
        <c:choose>
            <c:when test="${empty criteriaList}">
                <div class="criteria-card text-center py-5">
                    <div style="font-size:3rem;color:#d1d5db;"><i class="bi bi-clipboard-x"></i></div>
                    <div class="fw-semibold text-muted mt-2">Chưa có tiêu chí sàng lọc nào được cấu hình</div>
                    <div class="small text-muted mb-3">Bấm "Cấu hình tiêu chí mới" để bắt đầu chấm điểm tự động</div>
                    <a href="${pageContext.request.contextPath}/screening-criteria/setup"
                       class="btn-ris-primary">
                        <i class="bi bi-plus-circle-fill me-1"></i> Cấu hình ngay
                    </a>
                </div>
            </c:when>
            <c:otherwise>
                <div class="row g-3">
                    <c:forEach var="sc" items="${criteriaList}">
                        <div class="col-12 col-lg-6">
                            <div class="criteria-card">
                                <%-- Header: tên vị trí --%>
                                <div class="d-flex align-items-start justify-content-between gap-2 mb-3">
                                    <div>
                                        <div class="req-title">
                                            <i class="bi bi-briefcase me-1 text-primary"></i>
                                            <c:out value="${sc.requisitionTitle != null ? sc.requisitionTitle : sc.requisitionId}" />
                                        </div>
                                        <div class="text-muted" style="font-size:.78rem;">
                                            Cập nhật: <fmt:formatDate value="${sc.updatedAt}" pattern="dd/MM/yyyy HH:mm" />
                                            <c:if test="${not empty sc.createdByName}">
                                                &nbsp;·&nbsp; <c:out value="${sc.createdByName}" />
                                            </c:if>
                                        </div>
                                    </div>
                                    <div class="d-flex gap-1 flex-shrink-0">
                                        <a href="${pageContext.request.contextPath}/screening-criteria/setup?requisitionId=${sc.requisitionId}"
                                           class="btn btn-sm btn-outline-secondary" title="Chỉnh sửa tiêu chí" style="border-radius:8px;">
                                            <i class="bi bi-pencil-fill"></i>
                                        </a>
                                        <a href="${pageContext.request.contextPath}/screening-criteria/delete?requisitionId=${sc.requisitionId}"
                                           class="btn btn-sm btn-outline-danger"
                                           onclick="return confirm('Xác nhận xoá tiêu chí sàng lọc cho vị trí này?')"
                                           title="Xoá tiêu chí" style="border-radius:8px;">
                                            <i class="bi bi-trash3-fill"></i>
                                        </a>
                                    </div>
                                </div>

                                <%-- Tiêu chí bắt buộc --%>
                                <div class="row g-2 mb-3">
                                    <div class="col-6">
                                        <div class="small text-muted mb-1"><i class="bi bi-calendar3 me-1"></i>Kinh nghiệm tối thiểu</div>
                                        <div class="fw-bold text-dark">
                                            <c:choose>
                                                <c:when test="${sc.minExperienceYears > 0}">${sc.minExperienceYears} năm</c:when>
                                                <c:otherwise><span class="text-muted">Không yêu cầu</span></c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>
                                    <div class="col-6">
                                        <div class="small text-muted mb-1"><i class="bi bi-mortarboard me-1"></i>Bằng cấp yêu cầu</div>
                                        <div class="fw-bold text-dark">
                                            <c:choose>
                                                <c:when test="${not empty sc.requiredDegree}"><c:out value="${sc.requiredDegree}" /></c:when>
                                                <c:otherwise><span class="text-muted">Không yêu cầu</span></c:otherwise>
                                            </c:choose>
                                        </div>
                                    </div>
                                </div>

                                <%-- Kỹ năng bắt buộc --%>
                                <c:if test="${not empty sc.mustHaveSkills}">
                                    <div class="small text-muted mb-1"><i class="bi bi-check2-circle me-1 text-danger"></i>Kỹ năng bắt buộc</div>
                                    <div class="mb-2">
                                        <c:forEach var="skill" items="${sc.mustHaveSkillList}">
                                            <span class="criteria-tag must"><i class="bi bi-asterisk" style="font-size:.6rem;"></i><c:out value="${skill}" /></span>
                                        </c:forEach>
                                    </div>
                                </c:if>

                                <%-- Kỹ năng cộng điểm --%>
                                <c:if test="${not empty sc.niceToHaveSkills}">
                                    <div class="small text-muted mb-1"><i class="bi bi-plus-circle me-1 text-success"></i>Kỹ năng cộng điểm</div>
                                    <div class="mb-3">
                                        <c:forEach var="skill" items="${sc.niceToHaveSkillList}">
                                            <span class="criteria-tag nice"><c:out value="${skill}" /></span>
                                        </c:forEach>
                                    </div>
                                </c:if>

                                <%-- Trọng số điểm --%>
                                <div class="mb-3">
                                    <div class="d-flex justify-content-between small text-muted mb-1">
                                        <span>Phân bổ điểm</span>
                                        <span>
                                            <span style="color:#4f46e5;">KN ${sc.weightExperience}</span> ·
                                            <span style="color:#7c3aed;">KN ${sc.weightSkills}</span> ·
                                            <span style="color:#0ea5e9;">BK ${sc.weightEducation}</span>
                                        </span>
                                    </div>
                                    <div class="weight-bar">
                                        <div class="w-exp"   style="width:${sc.weightExperience}%;"></div>
                                        <div class="w-skill" style="width:${sc.weightSkills}%;"></div>
                                        <div class="w-edu"   style="width:${sc.weightEducation}%;"></div>
                                    </div>
                                </div>

                                <%-- Nút hành động --%>
                                <div class="d-flex gap-2 flex-wrap">
                                    <form action="${pageContext.request.contextPath}/screening-criteria/score" method="POST"
                                          onsubmit="return confirm('Chấm điểm toàn bộ ứng viên của vị trí này? Điểm cũ sẽ bị ghi đè.')" style="display:inline;">
                                        <input type="hidden" name="requisitionId" value="${sc.requisitionId}">
                                        <button type="submit" class="btn btn-sm btn-primary" style="border-radius:8px;font-size:.8rem;">
                                            <i class="bi bi-lightning-charge-fill me-1"></i>Chấm điểm hàng loạt
                                        </button>
                                    </form>

                                    <a href="${pageContext.request.contextPath}/candidates?requisitionId=${sc.requisitionId}&sort=score"
                                       class="btn btn-sm btn-outline-primary" style="border-radius:8px;font-size:.8rem;">
                                        <i class="bi bi-sort-numeric-down me-1"></i>Xem kết quả xếp hạng
                                    </a>

                                    <form action="${pageContext.request.contextPath}/screening-criteria/reset" method="POST"
                                          onsubmit="return confirm('Đặt lại toàn bộ điểm chấm? Hành động này không thể hoàn tác.')" style="display:inline;">
                                        <input type="hidden" name="requisitionId" value="${sc.requisitionId}">
                                        <button type="submit" class="btn btn-sm btn-outline-secondary" style="border-radius:8px;font-size:.8rem;"
                                                title="Đặt lại điểm để chấm lại từ đầu">
                                            <i class="bi bi-arrow-counterclockwise"></i> Reset
                                        </button>
                                    </form>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div><%-- /app-content --%>
</main>

<jsp:include page="../common/footer.jsp" />

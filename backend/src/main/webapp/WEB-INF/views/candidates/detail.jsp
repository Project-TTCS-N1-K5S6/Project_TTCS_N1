<%--
  ==============================================================================
  HỒ SƠ ỨNG VIÊN HỢP NHẤT - KN-218 (candidates/detail.jsp)
  Chức năng:
  - Hiển thị đầy đủ thông tin cá nhân ứng viên (họ tên, email, SĐT, nguồn, mã tracking).
  - Xem trước CV ngay trong trang (iframe embedded PDF/Word preview).
  - Hiển thị toàn bộ lịch sử ứng tuyển các vị trí trước đây của cùng người này.
  - Quản lý ghi chú nội bộ (editable, chỉ Recruiter/HR_Manager/Admin).
  - Cập nhật trạng thái pipeline (APPLIED → SCREENING → INTERVIEWING → OFFER → HIRED/REJECTED).
  - Kiểm soát quyền: chỉ Recruiter phụ trách và Hiring Manager của vị trí đó mới xem được.
  ==============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Hồ sơ ứng viên - ${candidate.fullName}" />
<c:set var="breadcrumb" value="Hồ sơ ứng viên" />
<c:set var="activeMenu" value="candidates" />

<jsp:include page="../common/header.jsp" />

<style>
/* =====================================================================
   KN-218 — Trang Hồ sơ Ứng viên Hợp nhất
   ===================================================================== */
.profile-hero {
    background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
    border-radius: 16px;
    padding: 2rem 2.5rem;
    color: #fff;
    position: relative;
    overflow: hidden;
    margin-bottom: 1.75rem;
}
.profile-hero::before {
    content: '';
    position: absolute;
    top: -60px; right: -60px;
    width: 240px; height: 240px;
    background: radial-gradient(circle, rgba(99,102,241,0.25) 0%, transparent 70%);
    border-radius: 50%;
}
.profile-hero::after {
    content: '';
    position: absolute;
    bottom: -40px; left: 30%;
    width: 160px; height: 160px;
    background: radial-gradient(circle, rgba(16,185,129,0.15) 0%, transparent 70%);
    border-radius: 50%;
}
.profile-avatar-lg {
    width: 80px; height: 80px; min-width: 80px;
    border-radius: 50%;
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    display: flex; align-items: center; justify-content: center;
    font-size: 2rem; font-weight: 800; color: #fff;
    border: 3px solid rgba(255,255,255,0.2);
    box-shadow: 0 0 0 6px rgba(99,102,241,0.15);
    flex-shrink: 0;
}
.profile-name {
    font-size: 1.5rem; font-weight: 700; margin-bottom: 0.25rem;
    letter-spacing: -0.3px;
}
.profile-meta { opacity: 0.8; font-size: 0.875rem; }
.profile-badge {
    display: inline-flex; align-items: center; gap: 6px;
    background: rgba(255,255,255,0.12); border: 1px solid rgba(255,255,255,0.2);
    border-radius: 999px; padding: 4px 12px;
    font-size: 0.75rem; font-weight: 600; backdrop-filter: blur(4px);
}

/* --- Info Cards --- */
.info-card {
    background: #fff;
    border: 1px solid #e2e8f0;
    border-radius: 14px;
    overflow: hidden;
    box-shadow: 0 1px 3px rgba(0,0,0,0.06);
    margin-bottom: 1.5rem;
}
.info-card-header {
    padding: 1rem 1.25rem;
    border-bottom: 1px solid #f1f5f9;
    background: #f8fafc;
    display: flex; align-items: center; gap: 10px;
}
.info-card-header .card-icon {
    width: 32px; height: 32px; border-radius: 8px;
    display: flex; align-items: center; justify-content: center;
    font-size: 1rem;
}
.info-card-body { padding: 1.25rem; }

/* --- CV Preview --- */
.cv-preview-frame {
    width: 100%; height: 520px;
    border: none; border-radius: 8px;
    background: #f8fafc;
}
.cv-placeholder {
    width: 100%; height: 300px;
    background: linear-gradient(135deg, #f1f5f9, #e2e8f0);
    border-radius: 10px;
    display: flex; flex-direction: column;
    align-items: center; justify-content: center;
    color: #94a3b8; gap: 12px;
}

/* --- Application History Timeline --- */
.timeline { position: relative; padding-left: 24px; }
.timeline::before {
    content: ''; position: absolute;
    left: 10px; top: 0; bottom: 0; width: 2px;
    background: linear-gradient(to bottom, #6366f1, #e2e8f0);
    border-radius: 2px;
}
.timeline-item { position: relative; margin-bottom: 1.5rem; }
.timeline-dot {
    position: absolute; left: -30px; top: 4px;
    width: 16px; height: 16px; border-radius: 50%;
    border: 2px solid #fff;
    box-shadow: 0 0 0 2px #6366f1;
}
.timeline-dot.current { background: #6366f1; box-shadow: 0 0 0 3px rgba(99,102,241,0.25); }
.timeline-dot.success { background: #10b981; box-shadow: 0 0 0 3px rgba(16,185,129,0.25); }
.timeline-dot.danger  { background: #ef4444; box-shadow: 0 0 0 3px rgba(239,68,68,0.25); }
.timeline-dot.warning { background: #f59e0b; box-shadow: 0 0 0 3px rgba(245,158,11,0.25); }
.timeline-dot.info    { background: #3b82f6; box-shadow: 0 0 0 3px rgba(59,130,246,0.25); }
.timeline-dot.secondary { background: #94a3b8; box-shadow: 0 0 0 2px #94a3b8; }
.timeline-content {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 10px; padding: 1rem 1.125rem;
    transition: box-shadow .2s;
}
.timeline-content:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.timeline-content.is-current {
    background: linear-gradient(135deg, #eff6ff, #eef2ff);
    border-color: #a5b4fc;
}
.interaction-item { border-left: 3px solid #6366f1; padding: .75rem 1rem; background: #f8fafc; border-radius: 0 8px 8px 0; }
.interaction-item + .interaction-item { margin-top: .75rem; }
.interaction-item.interview { border-color: #f59e0b; }
.interaction-item.evaluation { border-color: #10b981; }
.interaction-item.email { border-color: #3b82f6; }
.interaction-item.application { border-color: #8b5cf6; }
.interaction-description { white-space: pre-wrap; overflow-wrap: anywhere; }

/* --- Notes Editor --- */
.notes-textarea {
    border: 1px solid #e2e8f0; border-radius: 10px;
    padding: 0.875rem 1rem; font-size: 0.875rem;
    resize: vertical; min-height: 100px; width: 100%;
    font-family: inherit; transition: border-color .2s, box-shadow .2s;
    outline: none;
}
.notes-textarea:focus {
    border-color: #6366f1;
    box-shadow: 0 0 0 3px rgba(99,102,241,0.1);
}

/* --- Status Badge Pill --- */
.status-pill {
    display: inline-flex; align-items: center; gap: 5px;
    padding: 3px 10px; border-radius: 999px;
    font-size: 0.72rem; font-weight: 600;
}
.status-pill.APPLIED    { background:#eff6ff; color:#2563eb; border:1px solid #bfdbfe; }
.status-pill.SCREENING  { background:#ecfeff; color:#0891b2; border:1px solid #a5f3fc; }
.status-pill.INTERVIEWING { background:#fffbeb; color:#d97706; border:1px solid #fde68a; }
.status-pill.OFFER      { background:#f5f3ff; color:#7c3aed; border:1px solid #ddd6fe; }
.status-pill.HIRED      { background:#ecfdf5; color:#059669; border:1px solid #a7f3d0; }
.status-pill.REJECTED   { background:#fef2f2; color:#dc2626; border:1px solid #fecaca; }

/* --- Quick Stats in header --- */
.stat-chip {
    display: inline-flex; align-items: center; gap: 6px;
    background: rgba(255,255,255,0.08);
    border: 1px solid rgba(255,255,255,0.15);
    border-radius: 10px; padding: 6px 14px;
    font-size: 0.8rem;
}
.stat-chip .stat-num { font-weight: 700; font-size: 1rem; }

/* --- Pipeline Status Stepper --- */
.pipeline-stepper {
    display: flex;
    align-items: center;
    gap: 0;
    flex-wrap: nowrap;
    overflow-x: auto;
    padding: 0.5rem 0;
}
.pipeline-step {
    display: flex; align-items: center; gap: 6px;
    padding: 6px 14px; border-radius: 999px;
    font-size: 0.76rem; font-weight: 600;
    border: 1.5px solid #e2e8f0;
    background: #f8fafc; color: #64748b;
    cursor: default; white-space: nowrap;
    transition: all .2s;
}
.pipeline-step.active-step {
    background: #6366f1; color: #fff;
    border-color: #6366f1;
    box-shadow: 0 2px 8px rgba(99,102,241,0.3);
}
.pipeline-step.done-step {
    background: #ecfdf5; color: #059669;
    border-color: #a7f3d0;
}
.pipeline-step.danger-step {
    background: #fef2f2; color: #dc2626;
    border-color: #fecaca;
}
.pipeline-connector {
    width: 24px; height: 2px; background: #e2e8f0;
    flex-shrink: 0;
}
.pipeline-connector.done-conn { background: #10b981; }

/* --- Quick Action Buttons --- */
.btn-pipeline {
    display: inline-flex; align-items: center; gap: 6px;
    padding: 6px 16px; border-radius: 8px;
    font-size: 0.8rem; font-weight: 600; border: none;
    cursor: pointer; transition: all .2s;
}
.btn-pipeline:hover { opacity: 0.85; transform: translateY(-1px); }
.btn-move-next {
    background: linear-gradient(135deg, #6366f1, #8b5cf6);
    color: #fff; box-shadow: 0 2px 8px rgba(99,102,241,0.3);
}
.btn-move-reject {
    background: #fef2f2; color: #dc2626; border: 1.5px solid #fecaca;
}
.btn-move-hire {
    background: linear-gradient(135deg, #059669, #10b981);
    color: #fff; box-shadow: 0 2px 8px rgba(16,185,129,0.3);
}
</style>

<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <%-- ========== BREADCRUMB + BACK BUTTON ========== --%>
        <div class="d-flex align-items-center justify-content-between mb-3">
            <nav aria-label="breadcrumb">
                <ol class="breadcrumb mb-0" style="font-size:0.82rem;">
                    <li class="breadcrumb-item">
                        <a href="${pageContext.request.contextPath}/candidates" class="text-decoration-none text-muted">
                            <i class="bi bi-people-fill me-1"></i>Danh sách ứng viên
                        </a>
                    </li>
                    <li class="breadcrumb-item active" aria-current="page">Hồ sơ hợp nhất</li>
                </ol>
            </nav>
            <a href="${pageContext.request.contextPath}/candidates"
               class="btn btn-sm btn-outline-secondary rounded-pill px-3">
                <i class="bi bi-arrow-left me-1"></i> Quay lại
            </a>
        </div>

        <%-- ========== HERO BANNER ========== --%>
        <div class="profile-hero">
            <div class="d-flex flex-wrap align-items-start gap-4" style="position:relative;z-index:1;">
                <div class="profile-avatar-lg">
                    ${candidate.fullName != null && candidate.fullName.length() > 0 ? candidate.fullName.substring(0,1) : 'U'}
                </div>
                <div class="flex-grow-1">
                    <div class="d-flex flex-wrap align-items-center gap-2 mb-1">
                        <div class="profile-name">${candidate.fullName}</div>
                        <span class="profile-badge">
                            <i class="bi bi-patch-check-fill" style="color:#34d399;"></i>
                            Hồ sơ hợp nhất
                        </span>
                    </div>
                    <div class="profile-meta mb-3">
                        <span class="me-3"><i class="bi bi-envelope me-1"></i>${candidate.email}</span>
                        <c:if test="${not empty candidate.phone}">
                            <span class="me-3"><i class="bi bi-telephone me-1"></i>${candidate.phone}</span>
                        </c:if>
                        <c:if test="${not empty candidate.trackingCode}">
                            <span class="me-3"><i class="bi bi-qr-code me-1"></i>${candidate.trackingCode}</span>
                        </c:if>
                    </div>
                    <div class="d-flex flex-wrap gap-2">
                        <div class="stat-chip">
                            <span class="stat-num">${applicationHistory.size()}</span>
                            <span>lần ứng tuyển</span>
                        </div>
                        <c:if test="${not empty candidate.applicationSourceName}">
                            <div class="stat-chip">
                                <i class="bi bi-funnel" style="opacity:.7;"></i>
                                <span>${candidate.applicationSourceName}</span>
                            </div>
                        </c:if>
                        <c:if test="${not empty candidate.departmentName}">
                            <div class="stat-chip">
                                <i class="bi bi-building" style="opacity:.7;"></i>
                                <span>${candidate.departmentName}</span>
                            </div>
                        </c:if>
                        <%-- Trạng thái hiện tại --%>
                        <div class="stat-chip">
                            <i class="bi bi-circle-fill" style="font-size:7px; color:
                                ${candidate.status == 'HIRED' ? '#34d399' :
                                  candidate.status == 'REJECTED' ? '#f87171' :
                                  candidate.status == 'OFFER' ? '#a78bfa' :
                                  candidate.status == 'INTERVIEWING' ? '#fbbf24' : '#60a5fa'};"></i>
                            ${candidate.getStatusLabel()}
                        </div>
                    </div>
                </div>
                <c:if test="${not empty candidate.cvUrl}">
                    <a href="${pageContext.request.contextPath}/candidates/cv-preview?id=${candidate.id}&download=true" class="btn btn-sm btn-light text-dark fw-semibold rounded-pill px-4"
                       style="white-space:nowrap; align-self:flex-start;">
                        <i class="bi bi-download me-1"></i> Tải CV
                    </a>
                </c:if>
            </div>
        </div>

        <%-- ========== PIPELINE STATUS STEPPER ========== --%>
        <c:if test="${canEdit && candidate.status != 'HIRED' && candidate.status != 'REJECTED'}">
        <div class="info-card mb-4">
            <div class="info-card-header">
                <div class="card-icon" style="background:rgba(99,102,241,0.1); color:#6366f1;">
                    <i class="bi bi-arrow-right-circle-fill"></i>
                </div>
                <div class="flex-grow-1">
                    <div class="fw-bold text-dark fs-6">Chuyển giai đoạn tuyển dụng</div>
                    <small class="text-muted">Pipeline: Ứng tuyển → Sàng lọc → Phỏng vấn → Gửi Offer → Trúng tuyển</small>
                </div>
            </div>
            <div class="info-card-body">
                <%-- Stepper hiển thị trực quan --%>
                <div class="pipeline-stepper mb-4">
                    <div class="pipeline-step ${candidate.status == 'APPLIED' ? 'active-step' : (candidate.status == 'SCREENING' || candidate.status == 'INTERVIEWING' || candidate.status == 'OFFER' ? 'done-step' : '')}">
                        <i class="bi bi-${candidate.status == 'APPLIED' ? 'record-circle' : 'check-circle-fill'}"></i>
                        Mới ứng tuyển
                    </div>
                    <div class="pipeline-connector ${candidate.status == 'SCREENING' || candidate.status == 'INTERVIEWING' || candidate.status == 'OFFER' ? 'done-conn' : ''}"></div>
                    <div class="pipeline-step ${candidate.status == 'SCREENING' ? 'active-step' : (candidate.status == 'INTERVIEWING' || candidate.status == 'OFFER' ? 'done-step' : '')}">
                        <i class="bi bi-${candidate.status == 'SCREENING' ? 'record-circle' : (candidate.status == 'INTERVIEWING' || candidate.status == 'OFFER' ? 'check-circle-fill' : 'circle')}"></i>
                        Sàng lọc CV
                    </div>
                    <div class="pipeline-connector ${candidate.status == 'INTERVIEWING' || candidate.status == 'OFFER' ? 'done-conn' : ''}"></div>
                    <div class="pipeline-step ${candidate.status == 'INTERVIEWING' ? 'active-step' : (candidate.status == 'OFFER' ? 'done-step' : '')}">
                        <i class="bi bi-${candidate.status == 'INTERVIEWING' ? 'record-circle' : (candidate.status == 'OFFER' ? 'check-circle-fill' : 'circle')}"></i>
                        Phỏng vấn
                    </div>
                    <div class="pipeline-connector ${candidate.status == 'OFFER' ? 'done-conn' : ''}"></div>
                    <div class="pipeline-step ${candidate.status == 'OFFER' ? 'active-step' : ''}">
                        <i class="bi bi-${candidate.status == 'OFFER' ? 'record-circle' : 'circle'}"></i>
                        Gửi Offer
                    </div>
                    <div class="pipeline-connector"></div>
                    <div class="pipeline-step">
                        <i class="bi bi-circle"></i>
                        Trúng tuyển
                    </div>
                </div>

                <%-- Action buttons --%>
                <div class="d-flex flex-wrap gap-2 align-items-center">
                    <%-- Nút chuyển sang giai đoạn tiếp theo --%>
                    <c:choose>
                        <c:when test="${candidate.status == 'APPLIED'}">
                            <form action="${pageContext.request.contextPath}/candidates/profile" method="POST" class="d-inline">
                                <input type="hidden" name="action" value="update-status">
                                <input type="hidden" name="candidateId" value="${candidate.id}">
                                <input type="hidden" name="status" value="SCREENING">
                                <button type="submit" class="btn-pipeline btn-move-next" id="btn-to-screening">
                                    <i class="bi bi-arrow-right-circle"></i> Chuyển sang Sàng lọc
                                </button>
                            </form>
                        </c:when>
                        <c:when test="${candidate.status == 'SCREENING'}">
                            <form action="${pageContext.request.contextPath}/candidates/profile" method="POST" class="d-inline">
                                <input type="hidden" name="action" value="update-status">
                                <input type="hidden" name="candidateId" value="${candidate.id}">
                                <input type="hidden" name="status" value="INTERVIEWING">
                                <button type="submit" class="btn-pipeline btn-move-next" id="btn-to-interviewing">
                                    <i class="bi bi-arrow-right-circle"></i> Chuyển sang Phỏng vấn
                                </button>
                            </form>
                        </c:when>
                        <c:when test="${candidate.status == 'INTERVIEWING'}">
                            <form action="${pageContext.request.contextPath}/candidates/profile" method="POST" class="d-inline">
                                <input type="hidden" name="action" value="update-status">
                                <input type="hidden" name="candidateId" value="${candidate.id}">
                                <input type="hidden" name="status" value="OFFER">
                                <button type="submit" class="btn-pipeline btn-move-next" id="btn-to-offer">
                                    <i class="bi bi-envelope-paper"></i> Gửi Offer
                                </button>
                            </form>
                        </c:when>
                        <c:when test="${candidate.status == 'OFFER'}">
                            <form action="${pageContext.request.contextPath}/candidates/profile" method="POST" class="d-inline">
                                <input type="hidden" name="action" value="update-status">
                                <input type="hidden" name="candidateId" value="${candidate.id}">
                                <input type="hidden" name="status" value="HIRED">
                                <button type="submit" class="btn-pipeline btn-move-hire" id="btn-to-hired">
                                    <i class="bi bi-award-fill"></i> Xác nhận Trúng tuyển
                                </button>
                            </form>
                        </c:when>
                    </c:choose>

                    <%-- Nút Từ chối (có modal chọn lý do) --%>
                    <button type="button" class="btn-pipeline btn-move-reject"
                            data-bs-toggle="modal" data-bs-target="#rejectModal" id="btn-open-reject">
                        <i class="bi bi-x-circle"></i> Từ chối hồ sơ
                    </button>
                </div>
            </div>
        </div>
        </c:if>

        <%-- Hiển thị thông báo nếu ứng viên đã kết thúc pipeline --%>
        <c:if test="${candidate.status == 'HIRED' || candidate.status == 'REJECTED'}">
        <div class="alert ${candidate.status == 'HIRED' ? 'alert-success' : 'alert-danger'} d-flex align-items-center gap-3 mb-4 rounded-3">
            <i class="bi bi-${candidate.status == 'HIRED' ? 'award-fill' : 'x-circle-fill'} fs-4"></i>
            <div>
                <div class="fw-bold">${candidate.status == 'HIRED' ? 'Ứng viên đã Trúng tuyển' : 'Hồ sơ đã bị Từ chối'}</div>
                <small>
                    <c:if test="${not empty candidate.rejectionReasonName}">
                        Lý do: ${candidate.rejectionReasonName}
                    </c:if>
                </small>
            </div>
            <c:if test="${canEdit}">
            <div class="ms-auto">
                <%-- Khôi phục về APPLIED --%>
                <form action="${pageContext.request.contextPath}/candidates/profile" method="POST" class="d-inline">
                    <input type="hidden" name="action" value="update-status">
                    <input type="hidden" name="candidateId" value="${candidate.id}">
                    <input type="hidden" name="status" value="APPLIED">
                    <button type="submit" class="btn btn-sm btn-outline-secondary rounded-pill"
                            onclick="return confirm('Đặt lại trạng thái về Mới ứng tuyển?')">
                        <i class="bi bi-arrow-counterclockwise me-1"></i>Đặt lại
                    </button>
                </form>
            </div>
            </c:if>
        </div>
        </c:if>

        <%-- ========== MAIN CONTENT: 2 cột ========== --%>
        <div class="row g-4">

            <%-- ===== CỘT TRÁI: CV preview + Thông tin cá nhân ===== --%>
            <div class="col-12 col-lg-7">

                <%-- 1. CV XEM TRƯỚC INLINE --%>
                <div class="info-card">
                    <div class="info-card-header">
                        <div class="card-icon bg-primary bg-opacity-10 text-primary">
                            <i class="bi bi-file-earmark-person-fill"></i>
                        </div>
                        <div>
                            <div class="fw-bold text-dark fs-6">Xem trước CV</div>
                            <small class="text-muted">Nội dung CV đính kèm của ứng viên</small>
                        </div>
                    </div>
                    <div class="info-card-body p-0" style="overflow:hidden; border-radius:0 0 14px 14px;">
                        <c:choose>
                            <c:when test="${not empty candidate.cvUrl}">
                                <%-- [KN-275]: Stream xem trước CV bảo mật qua API máy chủ --%>
                                <iframe src="${pageContext.request.contextPath}/candidates/cv-preview?id=${candidate.id}"
                                        class="cv-preview-frame"
                                        title="CV - ${candidate.fullName}"
                                        loading="lazy">
                                </iframe>
                                <div class="px-3 py-2 border-top bg-light d-flex align-items-center justify-content-between gap-2"
                                     style="font-size:.8rem;">
                                    <span class="text-muted"><i class="bi bi-shield-check text-success me-1"></i>
                                        Luồng xem trước CV an toàn (đã xác thực quyền)
                                    </span>
                                    <div class="d-flex align-items-center gap-2">
                                        <a href="${pageContext.request.contextPath}/candidates/cv-preview?id=${candidate.id}" target="_blank" class="text-decoration-none text-primary">
                                            <i class="bi bi-box-arrow-up-right me-1"></i> Mở tab mới
                                        </a>
                                        <span class="text-muted">|</span>
                                        <a href="${pageContext.request.contextPath}/candidates/cv-preview?id=${candidate.id}&download=true" class="btn btn-xs btn-outline-primary rounded-pill px-3 py-1" style="font-size:.75rem;">
                                            <i class="bi bi-download me-1"></i>Tải về
                                        </a>
                                    </div>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="cv-placeholder m-3" style="height:200px;">
                                    <i class="bi bi-file-earmark-x" style="font-size:2.5rem;"></i>
                                    <div class="fw-semibold">Chưa có CV đính kèm</div>
                                    <div style="font-size:.8rem;">Ứng viên chưa tải lên tệp CV</div>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <%-- 2. THÔNG TIN CÁ NHÂN --%>
                <div class="info-card">
                    <div class="info-card-header">
                        <div class="card-icon bg-indigo-100 text-indigo-600" style="background:rgba(99,102,241,0.1); color:#6366f1;">
                            <i class="bi bi-person-vcard-fill"></i>
                        </div>
                        <div>
                            <div class="fw-bold text-dark fs-6">Thông tin cá nhân</div>
                            <small class="text-muted">Chi tiết hồ sơ ứng viên</small>
                        </div>
                    </div>
                    <div class="info-card-body">
                        <div class="row g-3">
                            <div class="col-6">
                                <div class="text-muted small mb-1">Họ và tên</div>
                                <div class="fw-semibold text-dark">${candidate.fullName}</div>
                            </div>
                            <div class="col-6">
                                <div class="text-muted small mb-1">Email</div>
                                <div class="fw-semibold text-primary">${candidate.email}</div>
                            </div>
                            <div class="col-6">
                                <div class="text-muted small mb-1">Số điện thoại</div>
                                <div class="fw-semibold">${not empty candidate.phone ? candidate.phone : '—'}</div>
                            </div>
                            <div class="col-6">
                                <div class="text-muted small mb-1">Nguồn ứng tuyển</div>
                                <div class="fw-semibold">${not empty candidate.applicationSourceName ? candidate.applicationSourceName : 'Chưa xác định'}</div>
                            </div>
                            <div class="col-6">
                                <div class="text-muted small mb-1">Vị trí hiện tại</div>
                                <div class="fw-semibold">${not empty candidate.requisitionTitle ? candidate.requisitionTitle : 'Chưa gắn vị trí'}</div>
                            </div>
                            <div class="col-6">
                                <div class="text-muted small mb-1">Phòng ban</div>
                                <div class="fw-semibold">${not empty candidate.departmentName ? candidate.departmentName : '—'}</div>
                            </div>
                            <c:if test="${not empty candidate.trackingCode}">
                                <div class="col-6">
                                    <div class="text-muted small mb-1">Mã tra cứu</div>
                                    <div class="fw-semibold font-monospace text-success">${candidate.trackingCode}</div>
                                </div>
                            </c:if>
                            <div class="col-6">
                                <div class="text-muted small mb-1">Ngày nộp đơn</div>
                                <div class="fw-semibold">
                                    <fmt:formatDate value="${candidate.createdAt}" pattern="dd/MM/yyyy HH:mm" />
                                </div>
                            </div>
                            <c:if test="${not empty candidate.referredByUserName}">
                                <div class="col-12">
                                    <div class="text-muted small mb-1">Người giới thiệu</div>
                                    <div class="fw-semibold"><i class="bi bi-person-heart text-pink me-1" style="color:#ec4899;"></i>${candidate.referredByUserName}</div>
                                </div>
                            </c:if>
                            <c:if test="${not empty candidate.coverLetter}">
                                <div class="col-12">
                                    <div class="text-muted small mb-1">Thư xin việc</div>
                                    <div class="border rounded-3 bg-light p-3" style="font-size:.85rem; white-space:pre-wrap; max-height:140px; overflow-y:auto;">${candidate.coverLetter}</div>
                                </div>
                            </c:if>
                        </div>
                    </div>
                </div>

            </div><%-- end col-left --%>

            <%-- ===== CỘT PHẢI: Lịch sử ứng tuyển + Ghi chú + Nhãn ===== --%>
            <div class="col-12 col-lg-5">

                <%-- Nhật ký tương tác: dữ liệu theo đúng hồ sơ ứng viên hiện tại --%>
                <div class="info-card" id="interaction-log">
                    <div class="info-card-header">
                        <div class="card-icon" style="background:rgba(99,102,241,0.1); color:#4f46e5;">
                            <i class="bi bi-activity"></i>
                        </div>
                        <div class="flex-grow-1">
                            <div class="fw-bold text-dark fs-6">Nhật ký tương tác</div>
                            <small class="text-muted">Nộp hồ sơ, chuyển giai đoạn, email, phỏng vấn và đánh giá</small>
                        </div>
                        <span class="badge bg-primary rounded-pill">${interactionHistory.size()}</span>
                    </div>
                    <div class="info-card-body">
                        <c:choose>
                            <c:when test="${empty interactionHistory}">
                                <div class="text-center py-3 text-muted">
                                    Chưa có tương tác nào được ghi nhận cho hồ sơ này.
                                </div>
                            </c:when>
                            <c:otherwise>
                                <c:forEach var="event" items="${interactionHistory}">
                                    <div class="interaction-item
                                        ${event.type == 'INTERVIEW_SCHEDULED' ? 'interview' :
                                          event.type == 'EVALUATION_RECORDED' ? 'evaluation' :
                                          event.type == 'EMAIL_SENT' ? 'email' :
                                          event.type == 'APPLICATION' ? 'application' : ''}">
                                        <div class="d-flex justify-content-between align-items-start gap-2">
                                            <strong><c:out value="${event.title}" /></strong>
                                            <small class="text-muted flex-shrink-0">
                                                <fmt:formatDate value="${event.occurredAt}" pattern="dd/MM/yyyy HH:mm" />
                                            </small>
                                        </div>
                                        <div class="interaction-description small mt-1">
                                            <c:out value="${event.description}" />
                                        </div>
                                        <c:if test="${not empty event.actorName}">
                                            <small class="text-muted d-block mt-1">Người thực hiện: <c:out value="${event.actorName}" /></small>
                                        </c:if>
                                    </div>
                                </c:forEach>
                            </c:otherwise>
                        </c:choose>

                        <c:if test="${canEdit}">
                            <form action="${pageContext.request.contextPath}/candidates/profile#interaction-log"
                                  method="POST" class="border-top mt-3 pt-3" id="interactionForm">
                                <input type="hidden" name="action" value="record-interaction">
                                <input type="hidden" name="candidateId" value="${candidate.id}">
                                <label for="interactionType" class="form-label small fw-semibold">Ghi nhận tương tác</label>
                                <select class="form-select form-select-sm mb-2" id="interactionType" name="interactionType" required>
                                    <option value="INTERVIEW_SCHEDULED">Lịch phỏng vấn</option>
                                    <option value="EVALUATION_RECORDED">Đánh giá</option>
                                </select>
                                <div id="interviewScheduleField" class="mb-2">
                                    <label for="scheduledAt" class="form-label small">Thời gian phỏng vấn</label>
                                    <input type="datetime-local" class="form-control form-control-sm" id="scheduledAt" name="scheduledAt">
                                </div>
                                <label for="interactionDetails" class="form-label small">Chi tiết</label>
                                <textarea class="form-control form-control-sm" id="interactionDetails" name="details"
                                          rows="3" maxlength="2000" required
                                          placeholder="Địa điểm/người phỏng vấn hoặc kết quả và nhận xét đánh giá"></textarea>
                                <button type="submit" class="btn btn-sm btn-primary mt-2">
                                    <i class="bi bi-plus-lg me-1"></i>Ghi nhận
                                </button>
                            </form>
                            <script>
                                (function () {
                                    var type = document.getElementById('interactionType');
                                    var schedule = document.getElementById('interviewScheduleField');
                                    var date = document.getElementById('scheduledAt');
                                    function syncInteractionFields() {
                                        var interview = type.value === 'INTERVIEW_SCHEDULED';
                                        schedule.hidden = !interview;
                                        date.required = interview;
                                    }
                                    type.addEventListener('change', syncInteractionFields);
                                    syncInteractionFields();
                                }());
                            </script>
                        </c:if>
                    </div>
                </div>

                <%-- 3. LỊCH SỬ ỨNG TUYỂN --%>
                <div class="info-card">
                    <div class="info-card-header">
                        <div class="card-icon" style="background:rgba(16,185,129,0.1); color:#059669;">
                            <i class="bi bi-clock-history"></i>
                        </div>
                        <div class="flex-grow-1">
                            <div class="fw-bold text-dark fs-6">Lịch sử ứng tuyển</div>
                            <small class="text-muted">Tất cả lần ứng tuyển của <strong>${candidate.email}</strong></small>
                        </div>
                        <span class="badge bg-primary rounded-pill">${applicationHistory.size()}</span>
                    </div>
                    <div class="info-card-body">
                        <c:choose>
                            <c:when test="${empty applicationHistory}">
                                <div class="text-center py-4 text-muted">
                                    <i class="bi bi-person-x fs-2 d-block mb-2"></i>
                                    Không tìm thấy lịch sử ứng tuyển
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div class="timeline">
                                    <c:forEach var="app" items="${applicationHistory}" varStatus="loop">
                                        <div class="timeline-item">
                                            <%-- Dot màu theo trạng thái --%>
                                            <div class="timeline-dot
                                                ${app.id == candidate.id ? 'current' :
                                                  app.status == 'HIRED' ? 'success' :
                                                  app.status == 'REJECTED' ? 'danger' :
                                                  app.status == 'OFFER' ? 'info' :
                                                  app.status == 'INTERVIEWING' ? 'warning' : 'secondary'}">
                                            </div>
                                            <div class="timeline-content ${app.id == candidate.id ? 'is-current' : ''}">
                                                <div class="d-flex align-items-start justify-content-between gap-2 mb-1">
                                                    <div class="fw-bold text-dark" style="font-size:.88rem; line-height:1.3;">
                                                        <c:out value="${not empty app.requisitionTitle ? app.requisitionTitle : 'Chưa gắn vị trí'}" />
                                                        <c:if test="${app.id == candidate.id}">
                                                            <span class="badge bg-primary ms-1" style="font-size:.65rem;">Hiện tại</span>
                                                        </c:if>
                                                    </div>
                                                    <span class="status-pill ${app.status} flex-shrink-0">
                                                        ${app.getStatusLabel()}
                                                    </span>
                                                </div>
                                                <div class="d-flex flex-wrap gap-3" style="font-size:.78rem; color:#64748b;">
                                                    <c:if test="${not empty app.departmentName}">
                                                        <span><i class="bi bi-building me-1"></i>${app.departmentName}</span>
                                                    </c:if>
                                                    <span><i class="bi bi-calendar3 me-1"></i>
                                                        <fmt:formatDate value="${app.createdAt}" pattern="dd/MM/yyyy" />
                                                    </span>
                                                    <c:if test="${not empty app.trackingCode}">
                                                        <span class="font-monospace text-success"><i class="bi bi-qr-code me-1"></i>${app.trackingCode}</span>
                                                    </c:if>
                                                </div>
                                                <c:if test="${not empty app.rejectionReasonName}">
                                                    <div class="mt-1" style="font-size:.77rem; color:#ef4444;">
                                                        <i class="bi bi-x-circle me-1"></i>Lý do từ chối: <em><c:out value="${app.rejectionReasonName}" /></em>
                                                    </div>
                                                </c:if>
                                                <%-- Link xem hồ sơ vị trí khác (nếu có quyền) --%>
                                                <c:if test="${app.id != candidate.id}">
                                                    <a href="${pageContext.request.contextPath}/candidates/profile?id=${app.id}"
                                                       class="mt-1 d-inline-block text-decoration-none"
                                                       style="font-size:.75rem; color:#6366f1;">
                                                        <i class="bi bi-box-arrow-up-right me-1"></i>Xem hồ sơ lần này
                                                    </a>
                                                </c:if>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <%-- 4. GHI CHÚ NỘI BỘ --%>
                <div class="info-card">
                    <div class="info-card-header">
                        <div class="card-icon" style="background:rgba(245,158,11,0.1); color:#d97706;">
                            <i class="bi bi-pencil-square"></i>
                        </div>
                        <div>
                            <div class="fw-bold text-dark fs-6">Ghi chú nội bộ</div>
                            <small class="text-muted">Chỉ Recruiter, HR Manager và Admin xem được</small>
                        </div>
                    </div>
                    <div class="info-card-body">
                        <c:choose>
                            <c:when test="${canEdit}">
                                <form action="${pageContext.request.contextPath}/candidates/profile" method="POST" id="notesForm">
                                    <input type="hidden" name="action" value="update-notes">
                                    <input type="hidden" name="candidateId" value="${candidate.id}">
                                    <textarea class="notes-textarea" name="notes" id="candidateNotes"
                                              placeholder="Nhập ghi chú về ứng viên này... (kỹ năng nổi bật, ấn tượng phỏng vấn, lý do cân nhắc...)"
                                              rows="5">${candidate.notes}</textarea>
                                    <div class="d-flex align-items-center justify-content-between mt-3 gap-2">
                                        <small class="text-muted">
                                            <i class="bi bi-lock-fill me-1"></i>Chỉ nội bộ đọc được
                                        </small>
                                        <button type="submit" class="btn btn-sm btn-primary rounded-pill px-4" id="saveNotesBtn" disabled>
                                            <i class="bi bi-check2 me-1"></i> Lưu ghi chú
                                        </button>
                                    </div>
                                </form>
                                <script>
                                    (function(){
                                        var ta = document.getElementById('candidateNotes');
                                        var btn = document.getElementById('saveNotesBtn');
                                        var original = ta.value;
                                        ta.addEventListener('input', function(){
                                            btn.disabled = (ta.value === original);
                                        });
                                    })();
                                </script>
                            </c:when>
                            <c:otherwise>
                                <div class="notes-textarea bg-light text-secondary"
                                     style="min-height:80px; white-space:pre-wrap; cursor:default;">
                                    <c:choose>
                                        <c:when test="${not empty candidate.notes}"><c:out value="${candidate.notes}" /></c:when>
                                        <c:otherwise><span class="text-muted fst-italic">Chưa có ghi chú nội bộ.</span></c:otherwise>
                                    </c:choose>
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>

                <%-- 5. NHÃN / LABELS (KN-274) --%>
                <div class="info-card">
                    <div class="info-card-header d-flex align-items-center justify-content-between">
                        <div class="d-flex align-items-center gap-2">
                            <div class="card-icon" style="background:rgba(139,92,246,0.1); color:#7c3aed;">
                                <i class="bi bi-tags-fill"></i>
                            </div>
                            <div>
                                <div class="fw-bold text-dark fs-6">Nhãn phân loại</div>
                                <small class="text-muted">Gắn nhãn và tùy biến phân nhóm ứng viên</small>
                            </div>
                        </div>
                        <c:if test="${canEdit}">
                            <button type="button" class="btn btn-sm btn-outline-primary rounded-pill px-3"
                                    data-bs-toggle="modal" data-bs-target="#editTagsModal" id="btn-open-edit-tags">
                                <i class="bi bi-pencil me-1"></i> Sửa nhãn
                            </button>
                        </c:if>
                    </div>
                    <div class="info-card-body">
                        <%-- Nhãn tùy biến do Recruiter gắn --%>
                        <div class="mb-3">
                            <div class="text-muted small fw-semibold mb-2">Nhãn tùy biến:</div>
                            <div class="d-flex flex-wrap gap-2" id="customTagsContainer">
                                <c:choose>
                                    <c:when test="${not empty candidate.tags}">
                                        <c:forEach var="tag" items="${candidate.getTagList()}">
                                            <span class="badge rounded-pill bg-light text-dark border px-3 py-2 fw-semibold"
                                                  style="font-size:.78rem; display:inline-flex; align-items:center; gap:6px;">
                                                <i class="bi bi-tag-fill text-primary" style="font-size:.7rem;"></i>
                                                <c:out value="${tag}" />
                                            </span>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="text-muted small fst-italic">Chưa có nhãn tùy biến nào được gắn.</span>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>

                        <hr class="my-3 opacity-25">

                        <%-- Nhãn tự động theo hồ sơ --%>
                        <div>
                            <div class="text-muted small fw-semibold mb-2">Nhãn hệ thống:</div>
                            <div class="d-flex flex-wrap gap-2">
                                <c:if test="${candidate.status == 'HIRED' || candidate.status == 'OFFER'}">
                                    <span class="badge rounded-pill" style="background:#ecfdf5; color:#059669; border:1px solid #a7f3d0; font-size:.75rem; font-weight:600; padding:5px 12px;">
                                        <i class="bi bi-star-fill me-1"></i>Tiềm năng cao
                                    </span>
                                </c:if>
                                <c:if test="${applicationHistory.size() > 1}">
                                    <span class="badge rounded-pill" style="background:#eff6ff; color:#2563eb; border:1px solid #bfdbfe; font-size:.75rem; font-weight:600; padding:5px 12px;">
                                        <i class="bi bi-repeat me-1"></i>Ứng tuyển nhiều lần (${applicationHistory.size()})
                                    </span>
                                </c:if>
                                <c:if test="${not empty candidate.referredByUserId}">
                                    <span class="badge rounded-pill" style="background:#fdf4ff; color:#9333ea; border:1px solid #e9d5ff; font-size:.75rem; font-weight:600; padding:5px 12px;">
                                        <i class="bi bi-person-heart me-1"></i>Được giới thiệu nội bộ
                                    </span>
                                </c:if>
                                <c:if test="${not empty candidate.cvUrl}">
                                    <span class="badge rounded-pill" style="background:#fff7ed; color:#c2410c; border:1px solid #fed7aa; font-size:.75rem; font-weight:600; padding:5px 12px;">
                                        <i class="bi bi-file-earmark-pdf me-1"></i>Có tệp CV
                                    </span>
                                </c:if>
                                <c:if test="${candidate.status == 'INTERVIEWING'}">
                                    <span class="badge rounded-pill" style="background:#fffbeb; color:#d97706; border:1px solid #fde68a; font-size:.75rem; font-weight:600; padding:5px 12px;">
                                        <i class="bi bi-camera-video me-1"></i>Đang phỏng vấn
                                    </span>
                                </c:if>
                            </div>
                        </div>
                    </div>
                </div>

            </div><%-- end col-right --%>
        </div><%-- end row --%>
    </div>
    <jsp:include page="../common/footer.jsp" />
</main>

<%-- ========== MODAL TỪ CHỐI HỒ SƠ ========== --%>
<c:if test="${canEdit}">
<div class="modal fade" id="rejectModal" tabindex="-1" aria-labelledby="rejectModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content" style="border-radius:16px; overflow:hidden;">
            <form action="${pageContext.request.contextPath}/candidates/profile" method="POST" id="rejectForm">
                <input type="hidden" name="action" value="update-status">
                <input type="hidden" name="candidateId" value="${candidate.id}">
                <input type="hidden" name="status" value="REJECTED">
                <div class="modal-header border-0" style="background:linear-gradient(135deg,#fff1f2,#fef2f2); padding:1.25rem 1.5rem;">
                    <div class="d-flex align-items-center gap-3">
                        <div style="width:40px;height:40px;border-radius:10px;background:#fee2e2;display:flex;align-items:center;justify-content:center;">
                            <i class="bi bi-x-circle-fill text-danger fs-5"></i>
                        </div>
                        <div>
                            <h5 class="modal-title fw-bold mb-0" id="rejectModalLabel">Từ chối hồ sơ ứng viên</h5>
                            <small class="text-muted">${candidate.fullName} — ${candidate.email}</small>
                        </div>
                    </div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body px-4 py-3">
                    <div class="mb-3">
                        <label class="form-label fw-semibold small">Lý do từ chối <span class="text-danger">*</span></label>
                        <select class="form-select" name="rejectionReasonId" id="rejectionReasonId" required>
                            <option value="">-- Chọn lý do từ chối --</option>
                            <c:forEach var="r" items="${rejectionReasons}">
                                <option value="${r.id}">${r.value}</option>
                            </c:forEach>
                        </select>
                        <div class="form-text text-muted">Lý do từ chối sẽ được lưu trong hồ sơ và hiển thị trong lịch sử ứng tuyển.</div>
                    </div>
                    <div class="alert alert-warning d-flex align-items-start gap-2 py-2 px-3" style="font-size:.82rem; border-radius:8px;">
                        <i class="bi bi-exclamation-triangle-fill flex-shrink-0 mt-1"></i>
                        <span>Thao tác này sẽ kết thúc pipeline của ứng viên. Bạn có thể đặt lại trạng thái sau nếu cần.</span>
                    </div>
                </div>
                <div class="modal-footer border-0 px-4 pb-4">
                    <button type="button" class="btn btn-secondary rounded-pill px-4" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-danger rounded-pill px-4" id="confirmRejectBtn">
                        <i class="bi bi-x-circle me-1"></i>Xác nhận từ chối
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
// Validate form từ chối trước khi submit
document.getElementById('rejectForm').addEventListener('submit', function(e) {
    var reason = document.getElementById('rejectionReasonId').value;
    if (!reason) {
        e.preventDefault();
        document.getElementById('rejectionReasonId').classList.add('is-invalid');
        document.getElementById('rejectionReasonId').focus();
    }
});
document.getElementById('rejectionReasonId').addEventListener('change', function() {
    this.classList.remove('is-invalid');
});
</script>

<%-- ========== MODAL CHỈNH SỬA NHÃN (KN-274) ========== --%>
<div class="modal fade" id="editTagsModal" tabindex="-1" aria-labelledby="editTagsModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content" style="border-radius:16px; overflow:hidden;">
            <form action="${pageContext.request.contextPath}/candidates/profile" method="POST" id="tagsForm">
                <input type="hidden" name="action" value="update-tags">
                <input type="hidden" name="candidateId" value="${candidate.id}">
                <div class="modal-header border-0" style="background:linear-gradient(135deg,#f5f3ff,#ede9fe); padding:1.25rem 1.5rem;">
                    <div class="d-flex align-items-center gap-3">
                        <div style="width:40px;height:40px;border-radius:10px;background:#ddd6fe;display:flex;align-items:center;justify-content:center;">
                            <i class="bi bi-tags-fill text-purple fs-5" style="color:#7c3aed;"></i>
                        </div>
                        <div>
                            <h5 class="modal-title fw-bold mb-0" id="editTagsModalLabel">Chỉnh sửa nhãn phân loại</h5>
                            <small class="text-muted">${candidate.fullName}</small>
                        </div>
                    </div>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body px-4 py-3">
                    <div class="mb-3">
                        <label class="form-label fw-semibold small">Nhãn phân loại (cách nhau bởi dấu phẩy)</label>
                        <input type="text" class="form-control" name="tags" id="candidateTagsInput"
                               value="<c:out value="${candidate.tags}" />"
                               placeholder="VD: Java, Spring Boot, Senior, Tiềm năng cao">
                        <div class="form-text text-muted">Nhập các nhãn cách nhau bằng dấu phẩy để phân nhóm ứng viên.</div>
                    </div>
                    <div class="mb-2">
                        <small class="text-muted fw-semibold d-block mb-1">Gợi ý nhãn nhanh:</small>
                        <div class="d-flex flex-wrap gap-1">
                            <button type="button" class="btn btn-xs btn-outline-secondary rounded-pill py-0 px-2 tag-suggestion" data-tag="Java">Java</button>
                            <button type="button" class="btn btn-xs btn-outline-secondary rounded-pill py-0 px-2 tag-suggestion" data-tag="Frontend">Frontend</button>
                            <button type="button" class="btn btn-xs btn-outline-secondary rounded-pill py-0 px-2 tag-suggestion" data-tag="Senior">Senior</button>
                            <button type="button" class="btn btn-xs btn-outline-secondary rounded-pill py-0 px-2 tag-suggestion" data-tag="Junior">Junior</button>
                            <button type="button" class="btn btn-xs btn-outline-secondary rounded-pill py-0 px-2 tag-suggestion" data-tag="Tiềm năng">Tiềm năng</button>
                            <button type="button" class="btn btn-xs btn-outline-secondary rounded-pill py-0 px-2 tag-suggestion" data-tag="Pass vòng 1">Pass vòng 1</button>
                            <button type="button" class="btn btn-xs btn-outline-secondary rounded-pill py-0 px-2 tag-suggestion" data-tag="Yêu cầu lương cao">Yêu cầu lương cao</button>
                        </div>
                    </div>
                </div>
                <div class="modal-footer border-0 px-4 pb-4">
                    <button type="button" class="btn btn-secondary rounded-pill px-4" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn btn-primary rounded-pill px-4" id="confirmTagsBtn">
                        <i class="bi bi-check2 me-1"></i> Lưu nhãn
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
document.querySelectorAll('.tag-suggestion').forEach(function(btn) {
    btn.addEventListener('click', function() {
        var input = document.getElementById('candidateTagsInput');
        var tag = this.getAttribute('data-tag');
        var current = input.value.trim();
        if (!current) {
            input.value = tag;
        } else {
            var parts = current.split(',').map(function(s){ return s.trim(); });
            if (parts.indexOf(tag) === -1) {
                input.value = current + ', ' + tag;
            }
        }
    });
});
</script>
</c:if>

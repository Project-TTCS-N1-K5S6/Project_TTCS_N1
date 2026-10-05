<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Hồ sơ & Pipeline ứng viên" />
<c:set var="breadcrumb" value="Hồ sơ ứng viên" />
<c:set var="activeMenu" value="candidates" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <h4 class="fw-bold mb-1">Pipeline &amp; Hồ sơ Ứng viên</h4>
                <p class="text-muted small mb-0">Theo dõi tiến trình tuyển dụng, sàng lọc CV, phỏng vấn và gửi Offer.</p>
            </div>
            <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addCandidateModal">
                <i class="bi bi-person-plus-fill me-1"></i> Tiếp nhận ứng viên
            </button>
        </div>

        <!-- Bộ lọc trạng thái Pipeline -->
        <div class="d-flex flex-wrap gap-2 mb-3">
            <a href="${pageContext.request.contextPath}/candidates" 
               class="btn btn-sm ${empty paramStatus ? 'btn-dark' : 'btn-outline-secondary'}">
               Tất cả hồ sơ
            </a>
            <a href="${pageContext.request.contextPath}/candidates?status=APPLIED" 
               class="btn btn-sm ${paramStatus == 'APPLIED' ? 'btn-primary' : 'btn-outline-primary'}">
               Mới ứng tuyển
            </a>
            <a href="${pageContext.request.contextPath}/candidates?status=SCREENING" 
               class="btn btn-sm ${paramStatus == 'SCREENING' ? 'btn-info text-white' : 'btn-outline-info'}">
               Sàng lọc CV
            </a>
            <a href="${pageContext.request.contextPath}/candidates?status=INTERVIEWING" 
               class="btn btn-sm ${paramStatus == 'INTERVIEWING' ? 'btn-warning text-dark' : 'btn-outline-warning text-dark'}">
               Đang phỏng vấn
            </a>
            <a href="${pageContext.request.contextPath}/candidates?status=OFFER" 
               class="btn btn-sm ${paramStatus == 'OFFER' ? 'btn-purple' : 'btn-outline-secondary'}">
               Thư mời Offer
            </a>
            <a href="${pageContext.request.contextPath}/candidates?status=HIRED" 
               class="btn btn-sm ${paramStatus == 'HIRED' ? 'btn-success' : 'btn-outline-success'}">
               Trúng tuyển
            </a>
            <a href="${pageContext.request.contextPath}/candidates?status=REJECTED" 
               class="btn btn-sm ${paramStatus == 'REJECTED' ? 'btn-danger' : 'btn-outline-danger'}">
               Đã từ chối
            </a>
        </div>

        <!-- Bảng danh sách ứng viên -->
        <div class="card mb-4 shadow-sm border">
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="table table-custom">
                        <thead>
                            <tr>
                                <th>Ứng viên</th>
                                <th>Liên hệ</th>
                                <th>Vị trí ứng tuyển</th>
                                <th>CV đính kèm</th>
                                <th>Giai đoạn Pipeline</th>
                                <th>Ghi chú</th>
                                <th class="text-end pe-3">Chuyển vòng</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="c" items="${candidates}">
                                <tr>
                                    <td>
                                        <div class="fw-bold text-dark">${c.fullName}</div>
                                        <small class="text-muted">Nộp ngày: <fmt:formatDate value="${c.createdAt}" pattern="dd/MM/yyyy" /></small>
                                    </td>
                                    <td>
                                        <div class="small"><i class="bi bi-envelope text-muted me-1"></i> ${c.email}</div>
                                        <div class="small text-muted"><i class="bi bi-telephone text-muted me-1"></i> ${c.phone}</div>
                                    </td>
                                    <td>
                                        <span class="fw-semibold text-dark">${c.requisitionTitle != null ? c.requisitionTitle : 'Chưa gắn yêu cầu'}</span>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty c.cvUrl}">
                                                <a href="${c.cvUrl}" target="_blank" class="btn btn-xs btn-outline-primary py-1 px-2 text-xs">
                                                    <i class="bi bi-file-earmark-pdf"></i> Xem CV
                                                </a>
                                            </c:when>
                                            <c:otherwise><span class="text-muted text-xs">Không có file</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <span class="badge ${c.getStatusBadgeClass()}">${c.getStatusLabel()}</span>
                                    </td>
                                    <td class="small text-muted" style="max-width: 200px;">
                                        ${c.notes != null ? c.notes : '-'}
                                    </td>
                                    <td class="text-end pe-3">
                                        <form action="${pageContext.request.contextPath}/candidates/update-status" method="POST" class="d-inline-flex gap-1 align-items-center">
                                            <input type="hidden" name="candidateId" value="${c.id}">
                                            <select class="form-select form-select-sm" name="status" onchange="this.form.submit()" style="width: 140px;">
                                                <option value="APPLIED" ${c.status == 'APPLIED' ? 'selected' : ''}>Mới ứng tuyển</option>
                                                <option value="SCREENING" ${c.status == 'SCREENING' ? 'selected' : ''}>Sàng lọc CV</option>
                                                <option value="INTERVIEWING" ${c.status == 'INTERVIEWING' ? 'selected' : ''}>Phỏng vấn</option>
                                                <option value="OFFER" ${c.status == 'OFFER' ? 'selected' : ''}>Gửi Offer</option>
                                                <option value="HIRED" ${c.status == 'HIRED' ? 'selected' : ''}>Trúng tuyển</option>
                                                <option value="REJECTED" ${c.status == 'REJECTED' ? 'selected' : ''}>Từ chối</option>
                                            </select>
                                        </form>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty candidates}">
                                <tr>
                                    <td colspan="7" class="text-center py-5 text-muted">
                                        <i class="bi bi-person-x fs-2 d-block mb-2 text-secondary"></i>
                                        Không có ứng viên nào trong giai đoạn này.
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <!-- Modal Thêm Ứng Viên -->
    <div class="modal fade" id="addCandidateModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/candidates/create" method="POST">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold"><i class="bi bi-person-plus text-primary me-2"></i> Tiếp nhận hồ sơ ứng viên</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row g-3">
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Họ và tên ứng viên <span class="text-danger">*</span></label>
                                <input type="text" class="form-control" name="fullName" required placeholder="VD: Nguyễn Văn B">
                            </div>
                            <div class="col-6">
                                <label class="form-label small fw-semibold">Email liên hệ <span class="text-danger">*</span></label>
                                <input type="email" class="form-control" name="email" required placeholder="candidate@email.com">
                            </div>
                            <div class="col-6">
                                <label class="form-label small fw-semibold">Số điện thoại</label>
                                <input type="text" class="form-control" name="phone" placeholder="0901234567">
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Đường dẫn CV (Link PDF)</label>
                                <input type="url" class="form-control" name="cvUrl" placeholder="https://storage.cloud/cv.pdf">
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Trạng thái khởi tạo</label>
                                <select class="form-select" name="status">
                                    <option value="APPLIED">Mới ứng tuyển</option>
                                    <option value="SCREENING">Sàng lọc CV</option>
                                    <option value="INTERVIEWING">Phỏng vấn</option>
                                </select>
                            </div>
                            <div class="col-12">
                                <label class="form-label small fw-semibold">Ghi chú ban đầu</label>
                                <textarea class="form-control" name="notes" rows="2" placeholder="Ghi chú kinh nghiệm, kỹ năng..."></textarea>
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                        <button type="submit" class="btn btn-primary"><i class="bi bi-check-lg"></i> Thêm hồ sơ</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

<jsp:include page="../common/footer.jsp" />

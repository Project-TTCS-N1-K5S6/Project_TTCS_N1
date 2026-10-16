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
            <c:if test="${canManageCandidates}">
                <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addCandidateModal">
                    <i class="bi bi-person-plus-fill me-1"></i> Tiếp nhận ứng viên
                </button>
                <button class="btn btn-outline-secondary" data-bs-toggle="modal" data-bs-target="#candidateTagCatalogModal">
                    <i class="bi bi-tags me-1"></i> Danh mục nhãn
                </button>
            </c:if>
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
                    <table class="table table-custom table-fit">
                        <thead>
                            <tr>
                                <th>Ứng viên</th>
                                <th>Vị trí &amp; Hồ sơ</th>
                                <th class="d-none d-md-table-cell">Liên hệ</th>
                                <th>Giai đoạn Pipeline</th>
                                <th>Nhãn</th>
                                <th>Đánh giá nhanh</th>
                                <th class="d-none d-xl-table-cell">Ghi chú nội bộ</th>
                                <th class="d-none d-xl-table-cell">Ghi chú ban đầu</th>
                                <th class="text-end pe-3" style="min-width: 140px;">Chuyển vòng</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="c" items="${candidates}">
                                <tr>
                                    <td>
                                        <div class="fw-bold text-dark"><c:out value="${c.fullName}" /></div>
                                        <small class="text-muted d-block">Nộp: <fmt:formatDate value="${c.createdAt}" pattern="dd/MM/yyyy" /></small>
                                        <!-- Mobile contact info -->
                                        <div class="d-md-none text-muted small mt-1">
                                            <div><i class="bi bi-envelope me-1"></i> ${c.email}</div>
                                            <c:if test="${not empty c.phone}"><div><i class="bi bi-telephone me-1"></i> ${c.phone}</div></c:if>
                                        </div>
                                    </td>
                                    <td>
                                        <div class="fw-semibold text-dark">${c.requisitionTitle != null ? c.requisitionTitle : 'Chưa gắn yêu cầu'}</div>
                                        <c:choose>
                                            <c:when test="${not empty c.cvUrl}">
                                                <a href="${c.cvUrl}" target="_blank" class="btn btn-xs btn-outline-primary py-0 px-2 mt-1 text-xs d-inline-flex align-items-center gap-1">
                                                    <i class="bi bi-file-earmark-pdf"></i> Xem CV
                                                </a>
                                            </c:when>
                                            <c:otherwise><span class="text-muted text-xs">Chưa có CV</span></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="d-none d-md-table-cell">
                                        <div class="small"><i class="bi bi-envelope text-muted me-1"></i> ${c.email}</div>
                                        <div class="small text-muted"><i class="bi bi-telephone text-muted me-1"></i> ${c.phone != null ? c.phone : '-'}</div>
                                    </td>
                                    <td>
                                        <span class="badge ${c.getStatusBadgeClass()}">${c.getStatusLabel()}</span>
                                    </td>
                                    <td>
                                        <div class="d-flex flex-wrap gap-1 mb-1">
                                            <c:forEach var="tag" items="${c.tags}">
                                                <span class="badge rounded-pill text-bg-light border">
                                                    <c:out value="${tag.categoryName}" />:
                                                    <c:out value="${tag.name}" />
                                                </span>
                                            </c:forEach>
                                            <c:if test="${empty c.tags}">
                                                <span class="small text-muted">Chưa gắn nhãn</span>
                                            </c:if>
                                        </div>
                                        <c:if test="${canManageCandidates}">
                                            <button type="button" class="btn btn-sm btn-outline-primary js-edit-candidate-tags"
                                                    data-bs-toggle="modal" data-bs-target="#candidateTagsModal"
                                                    data-candidate-id="<c:out value='${c.id}' />"
                                                    data-candidate-name="<c:out value='${c.fullName}' />">
                                                <i class="bi bi-tags"></i> Gắn nhãn
                                            </button>
                                            <div class="d-none js-current-candidate-tags">
                                                <c:forEach var="tag" items="${c.tags}">
                                                    <span data-tag-id="<c:out value='${tag.id}' />"></span>
                                                </c:forEach>
                                            </div>
                                        </c:if>
                                    </td>
                                    <td>
                                        <div class="text-warning text-nowrap" aria-label="Điểm trung bình ${c.averageRating} trên 5">
                                            <c:choose>
                                                <c:when test="${c.ratingCount > 0}">
                                                    <i class="bi bi-star-fill"></i>
                                                    <span class="text-dark fw-semibold"><fmt:formatNumber value="${c.averageRating}" maxFractionDigits="1" /></span>
                                                    <small class="text-muted">(${c.ratingCount})</small>
                                                </c:when>
                                                <c:otherwise><span class="small text-muted">Chưa đánh giá</span></c:otherwise>
                                            </c:choose>
                                        </div>
                                        <c:if test="${canManageCandidates}">
                                            <button type="button" class="btn btn-sm btn-outline-warning mt-1 js-open-quick-screening"
                                                    data-bs-toggle="modal" data-bs-target="#candidateScreeningModal"
                                                    data-candidate-id="<c:out value='${c.id}' />"
                                                    data-candidate-name="<c:out value='${c.fullName}' />"
                                                    data-rating="<c:out value='${c.currentUserRating}' />">
                                                <i class="bi bi-star"></i> Chấm nhanh
                                            </button>
                                        </c:if>
                                    </td>
                                    <td class="d-none d-xl-table-cell">
                                        <c:if test="${canManageInternalNotes}">
                                            <c:choose>
                                                <c:when test="${not empty c.internalNotes}">
                                                    <button type="button" class="btn btn-sm btn-outline-secondary js-open-quick-screening"
                                                            data-bs-toggle="modal" data-bs-target="#candidateScreeningModal"
                                                            data-candidate-id="<c:out value='${c.id}' />"
                                                            data-candidate-name="<c:out value='${c.fullName}' />"
                                                            data-rating="<c:out value='${c.currentUserRating}' />">
                                                        <i class="bi bi-lock-fill"></i> Nội bộ (${c.internalNotes.size()})
                                                    </button>
                                                </c:when>
                                                <c:otherwise><span class="small text-muted">Chưa có ghi chú nội bộ</span></c:otherwise>
                                            </c:choose>
                                            <div class="d-none" id="candidateNoteHistory-${c.id}">
                                                <c:forEach var="note" items="${c.internalNotes}">
                                                    <article class="border rounded p-2 mb-2">
                                                        <div class="d-flex justify-content-between gap-2 small text-muted mb-1">
                                                            <strong class="text-dark"><c:out value="${note.authorName}" /></strong>
                                                            <fmt:formatDate value="${note.createdAt}" pattern="dd/MM/yyyy HH:mm" />
                                                        </div>
                                                        <div class="text-break"><c:out value="${note.content}" /></div>
                                                    </article>
                                                </c:forEach>
                                            </div>
                                        </c:if>
                                        <c:if test="${not canManageInternalNotes}">
                                            <span class="small text-muted">Chỉ dành cho recruiter</span>
                                        </c:if>
                                    </td>
                                    <td class="small text-muted text-wrap-break d-none d-xl-table-cell" style="max-width: 180px;">
                                        <c:out value="${not empty c.notes ? c.notes : '-'}" />
                                    </td>
                                    <td class="text-end pe-3">
                                        <c:if test="${canManageCandidates}">
                                            <form action="${pageContext.request.contextPath}/candidates/update-status" method="POST" class="d-inline-flex gap-1 align-items-center justify-content-end">
                                                <input type="hidden" name="candidateId" value="<c:out value='${c.id}' />">
                                                <select class="form-select form-select-sm" name="status" onchange="this.form.submit()" style="width: 130px; font-size: 0.8rem;">
                                                    <option value="APPLIED" ${c.status == 'APPLIED' ? 'selected' : ''}>Mới ứng tuyển</option>
                                                    <option value="SCREENING" ${c.status == 'SCREENING' ? 'selected' : ''}>Sàng lọc CV</option>
                                                    <option value="INTERVIEWING" ${c.status == 'INTERVIEWING' ? 'selected' : ''}>Phỏng vấn</option>
                                                    <option value="OFFER" ${c.status == 'OFFER' ? 'selected' : ''}>Gửi Offer</option>
                                                    <option value="HIRED" ${c.status == 'HIRED' ? 'selected' : ''}>Trúng tuyển</option>
                                                    <option value="REJECTED" ${c.status == 'REJECTED' ? 'selected' : ''}>Từ chối</option>
                                                </select>
                                            </form>
                                        </c:if>
                                        <c:if test="${not canManageCandidates}">
                                            <span class="badge ${c.getStatusBadgeClass()}"><c:out value="${c.getStatusLabel()}" /></span>
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty candidates}">
                                <tr>
                                    <td colspan="9" class="text-center py-5 text-muted">
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

    <c:if test="${canManageCandidates}">
        <div class="modal fade" id="candidateTagsModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-lg">
                <div class="modal-content">
                    <form action="${pageContext.request.contextPath}/candidates/update-tags" method="POST">
                        <div class="modal-header">
                            <div>
                                <h5 class="modal-title fw-bold"><i class="bi bi-tags text-primary me-2"></i>Gắn nhãn hồ sơ</h5>
                                <div class="small text-muted js-screening-candidate-name"></div>
                            </div>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                        </div>
                        <div class="modal-body">
                            <input type="hidden" name="candidateId">
                            <p class="small text-muted">Chọn một hoặc nhiều nhãn đã khai báo trong danh mục. Bỏ chọn toàn bộ để gỡ nhãn khỏi hồ sơ.</p>
                            <c:forEach var="category" items="${tagCategories}">
                                <fieldset class="mb-3">
                                    <legend class="fs-6 fw-semibold"><c:out value="${category.name}" /></legend>
                                    <c:choose>
                                        <c:when test="${not empty category.tags}">
                                            <div class="d-flex flex-wrap gap-2">
                                                <c:forEach var="tag" items="${category.tags}">
                                                    <label class="form-check form-check-inline border rounded px-3 py-2 mb-0">
                                                        <input class="form-check-input" type="checkbox" name="tagIds"
                                                               value="<c:out value='${tag.id}' />">
                                                        <span class="form-check-label"><c:out value="${tag.name}" /></span>
                                                    </label>
                                                </c:forEach>
                                            </div>
                                        </c:when>
                                        <c:otherwise><span class="small text-muted">Danh mục chưa có nhãn.</span></c:otherwise>
                                    </c:choose>
                                </fieldset>
                            </c:forEach>
                            <c:if test="${empty tagCategories}">
                                <div class="alert alert-info mb-0">Danh mục nhãn chưa có dữ liệu. Hãy tạo danh mục và nhãn trước.</div>
                            </c:if>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                            <button type="submit" class="btn btn-primary"><i class="bi bi-check-lg"></i> Lưu nhãn</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>

        <div class="modal fade" id="candidateScreeningModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-lg">
                <div class="modal-content">
                    <form action="${pageContext.request.contextPath}/candidates/save-screening" method="POST">
                        <div class="modal-header">
                            <div>
                                <h5 class="modal-title fw-bold"><i class="bi bi-stars text-warning me-2"></i>Sàng lọc nhanh</h5>
                                <div class="small text-muted js-screening-candidate-name"></div>
                            </div>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                        </div>
                        <div class="modal-body">
                            <input type="hidden" name="candidateId">
                            <label class="form-label fw-semibold">Đánh giá của bạn</label>
                            <input type="hidden" name="rating" value="">
                            <div class="js-rating-stars d-flex gap-2 mb-1" role="group" aria-label="Chọn điểm từ 1 đến 5 sao">
                                <c:forEach begin="1" end="5" var="star">
                                    <button class="btn btn-link p-0 text-warning fs-3 js-rating-star" type="button"
                                            data-rating="<c:out value='${star}' />" aria-label="<c:out value='${star}' /> sao"
                                            aria-pressed="false"><i class="bi bi-star"></i></button>
                                </c:forEach>
                            </div>
                            <div class="form-text mb-3">Điểm được lưu riêng theo từng nhân sự; điểm trung bình hiển thị trên hồ sơ.</div>
                            <c:if test="${canManageInternalNotes}">
                                <label class="form-label fw-semibold" for="candidateInternalNote">Ghi chú nội bộ dành cho recruiter</label>
                                <textarea class="form-control" id="candidateInternalNote" name="internalNote" rows="3"
                                          maxlength="5000" placeholder="Chỉ recruiter được phân quyền mới xem được."></textarea>
                                <div class="form-text">Ghi chú mới được lưu kèm tên người viết và thời điểm tạo; ứng viên và vai trò khác không thể truy cập.</div>
                                <hr>
                                <h6 class="fw-semibold"><i class="bi bi-lock-fill me-1"></i>Lịch sử ghi chú nội bộ</h6>
                                <div class="js-internal-note-history small">
                                    <p class="text-muted mb-0">Chưa có ghi chú nội bộ.</p>
                                </div>
                            </c:if>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Đóng</button>
                            <button type="submit" class="btn btn-primary"><i class="bi bi-check-lg"></i> Lưu đánh giá / ghi chú</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>

        <div class="modal fade" id="candidateTagCatalogModal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold"><i class="bi bi-tags text-primary me-2"></i>Danh mục nhãn sàng lọc</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Đóng"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row g-4">
                            <div class="col-md-6">
                                <h6 class="fw-semibold">Tạo danh mục</h6>
                                <form action="${pageContext.request.contextPath}/candidates/tag-categories/create" method="POST">
                                    <div class="input-group">
                                        <input class="form-control" name="name" maxlength="100" required placeholder="VD: Mức độ phù hợp">
                                        <button class="btn btn-outline-primary" type="submit">Thêm</button>
                                    </div>
                                </form>
                            </div>
                            <div class="col-md-6">
                                <h6 class="fw-semibold">Khai báo nhãn</h6>
                                <form action="${pageContext.request.contextPath}/candidates/tags/create" method="POST">
                                    <select class="form-select mb-2" name="categoryId" required>
                                        <option value="">Chọn danh mục</option>
                                        <c:forEach var="category" items="${tagCategories}">
                                            <option value="<c:out value='${category.id}' />"><c:out value="${category.name}" /></option>
                                        </c:forEach>
                                    </select>
                                    <div class="input-group">
                                        <input class="form-control" name="name" maxlength="100" required placeholder="VD: Phù hợp cao">
                                        <button class="btn btn-outline-primary" type="submit">Thêm</button>
                                    </div>
                                </form>
                            </div>
                        </div>
                        <hr>
                        <h6 class="fw-semibold">Nhãn hiện có</h6>
                        <c:forEach var="category" items="${tagCategories}">
                            <div class="mb-2">
                                <strong><c:out value="${category.name}" /></strong>
                                <div class="d-flex flex-wrap gap-1 mt-1">
                                    <c:forEach var="tag" items="${category.tags}">
                                        <span class="badge text-bg-light border"><c:out value="${tag.name}" /></span>
                                    </c:forEach>
                                    <c:if test="${empty category.tags}"><span class="small text-muted">Chưa có nhãn</span></c:if>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </div>
        </div>
    </c:if>

    <!-- Modal Thêm Ứng Viên -->
    <div class="modal fade" id="addCandidateModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
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
                            <div class="col-12 col-sm-6">
                                <label class="form-label small fw-semibold">Email liên hệ <span class="text-danger">*</span></label>
                                <input type="email" class="form-control" name="email" required placeholder="candidate@email.com">
                            </div>
                            <div class="col-12 col-sm-6">
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

<c:if test="${canManageCandidates}">
    <script src="${pageContext.request.contextPath}/assets/js/candidates.js"></script>
</c:if>

<jsp:include page="../common/footer.jsp" />

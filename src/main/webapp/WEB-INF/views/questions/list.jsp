<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:set var="pageTitle" value="Ngân hàng câu hỏi" />
<c:set var="breadcrumb" value="Ngân hàng câu hỏi" />
<c:set var="activeMenu" value="questions" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />

    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <div class="d-flex flex-wrap align-items-center justify-content-between gap-3 mb-4">
            <div>
                <h4 class="fw-bold mb-1">Quản lý Ngân hàng câu hỏi</h4>
                <p class="text-muted small mb-0">Quản lý các câu hỏi theo tiêu chí trong khung năng lực.</p>
            </div>
            <div>
                <button class="btn btn-secondary me-2" data-bs-toggle="modal" data-bs-target="#criteriaModal">
                    <i class="bi bi-list-check me-1"></i> Quản lý Tiêu chí
                </button>
                <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addQuestionModal">
                    <i class="bi bi-plus-circle me-1"></i> Thêm câu hỏi
                </button>
            </div>
        </div>

        <div class="card mb-4 shadow-sm border">
            <div class="card-body">
                <form action="${pageContext.request.contextPath}/admin/questions" method="GET" class="row g-3 mb-4 align-items-end">
                    <div class="col-md-4">
                        <label class="form-label small fw-semibold">Tìm theo chức danh</label>
                        <input type="text" class="form-control" name="jobTitle" value="${searchJobTitle}" placeholder="Nhập chức danh...">
                    </div>
                    <div class="col-md-4">
                        <label class="form-label small fw-semibold">Lọc theo tiêu chí năng lực</label>
                        <select class="form-select" name="criterionId">
                            <option value="">-- Tất cả --</option>
                            <c:forEach var="c" items="${criteria}">
                                <option value="${c.id}" ${c.id == searchCriterionId ? 'selected' : ''}>${c.name}</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="col-md-2">
                        <button type="submit" class="btn btn-primary w-100"><i class="bi bi-search"></i> Tìm kiếm</button>
                    </div>
                    <div class="col-md-2">
                        <a href="${pageContext.request.contextPath}/admin/questions" class="btn btn-outline-secondary w-100">Xóa lọc</a>
                    </div>
                </form>

                <div class="table-responsive">
                    <table class="table table-custom">
                        <thead>
                            <tr>
                                <th>Chức danh</th>
                                <th>Tiêu chí năng lực</th>
                                <th>Nội dung câu hỏi</th>
                                <th>Độ khó</th>
                                <th>Gợi ý trả lời</th>
                                <th class="text-end pe-3">Thao tác</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="q" items="${questions}">
                                <tr>
                                    <td><span class="badge bg-light text-dark border">${q.jobTitle}</span></td>
                                    <td><strong>${q.criterionName}</strong></td>
                                    <td>${q.content}</td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${q.difficultyLevel == 'EASY'}"><span class="badge bg-success">Dễ</span></c:when>
                                            <c:when test="${q.difficultyLevel == 'MEDIUM'}"><span class="badge bg-warning text-dark">Trung bình</span></c:when>
                                            <c:when test="${q.difficultyLevel == 'HARD'}"><span class="badge bg-danger">Khó</span></c:when>
                                        </c:choose>
                                    </td>
                                    <td class="text-muted small">${q.goodAnswerSuggestion}</td>
                                    <td class="text-end pe-3">
                                        <button class="btn btn-sm btn-outline-secondary me-1" 
                                                onclick="openEditQuestionModal('${q.id}', '${q.content.replace("'", "\\'")}', '${q.difficultyLevel}', '${q.goodAnswerSuggestion.replace("'", "\\'")}', '${q.criterionId}', '${q.jobTitle.replace("'", "\\'")}')">
                                            <i class="bi bi-pencil"></i> Sửa
                                        </button>
                                        <form action="${pageContext.request.contextPath}/admin/questions/delete" method="POST" style="display:inline;"
                                              onsubmit="return confirm('Bạn có chắc chắn muốn xóa câu hỏi này?')">
                                            <input type="hidden" name="id" value="${q.id}">
                                            <button type="submit" class="btn btn-sm btn-outline-danger">
                                                <i class="bi bi-trash"></i>
                                            </button>
                                        </form>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty questions}">
                                <tr>
                                    <td colspan="6" class="text-center py-4 text-muted">Không tìm thấy câu hỏi nào.</td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>

    <!-- Modal Thêm Câu hỏi -->
    <div class="modal fade" id="addQuestionModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/admin/questions/create" method="POST">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold"><i class="bi bi-plus-circle text-primary me-2"></i> Thêm câu hỏi mới</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row">
                            <div class="col-md-6 mb-3">
                                <label class="form-label small fw-semibold">Chức danh</label>
                                <input type="text" class="form-control" name="jobTitle" required placeholder="VD: Lập trình viên Java">
                            </div>
                            <div class="col-md-6 mb-3">
                                <label class="form-label small fw-semibold">Tiêu chí năng lực <span class="text-danger">*</span></label>
                                <select class="form-select" name="criterionId" required>
                                    <c:forEach var="c" items="${criteria}">
                                        <option value="${c.id}">${c.name}</option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label small fw-semibold">Nội dung câu hỏi <span class="text-danger">*</span></label>
                            <textarea class="form-control" name="content" rows="3" required></textarea>
                        </div>
                        <div class="mb-3">
                            <label class="form-label small fw-semibold">Độ khó <span class="text-danger">*</span></label>
                            <select class="form-select" name="difficultyLevel" required>
                                <option value="EASY">Dễ</option>
                                <option value="MEDIUM" selected>Trung bình</option>
                                <option value="HARD">Khó</option>
                            </select>
                        </div>
                        <div class="mb-3">
                            <label class="form-label small fw-semibold">Gợi ý trả lời tốt</label>
                            <textarea class="form-control" name="goodAnswerSuggestion" rows="4"></textarea>
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

    <!-- Modal Sửa Câu hỏi -->
    <div class="modal fade" id="editQuestionModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/admin/questions/edit" method="POST">
                    <input type="hidden" name="id" id="editQuestionId">
                    <div class="modal-header">
                        <h5 class="modal-title fw-bold"><i class="bi bi-pencil-square text-primary me-2"></i> Sửa câu hỏi</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row">
                            <div class="col-md-6 mb-3">
                                <label class="form-label small fw-semibold">Chức danh</label>
                                <input type="text" class="form-control" name="jobTitle" id="editQJobTitle" required>
                            </div>
                            <div class="col-md-6 mb-3">
                                <label class="form-label small fw-semibold">Tiêu chí năng lực <span class="text-danger">*</span></label>
                                <select class="form-select" name="criterionId" id="editQCriterionId" required>
                                    <c:forEach var="c" items="${criteria}">
                                        <option value="${c.id}">${c.name}</option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label small fw-semibold">Nội dung câu hỏi <span class="text-danger">*</span></label>
                            <textarea class="form-control" name="content" id="editQContent" rows="3" required></textarea>
                        </div>
                        <div class="mb-3">
                            <label class="form-label small fw-semibold">Độ khó <span class="text-danger">*</span></label>
                            <select class="form-select" name="difficultyLevel" id="editQDifficulty" required>
                                <option value="EASY">Dễ</option>
                                <option value="MEDIUM">Trung bình</option>
                                <option value="HARD">Khó</option>
                            </select>
                        </div>
                        <div class="mb-3">
                            <label class="form-label small fw-semibold">Gợi ý trả lời tốt</label>
                            <textarea class="form-control" name="goodAnswerSuggestion" id="editQGoodAnswer" rows="4"></textarea>
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
    
    <!-- Modal Quản lý Tiêu chí -->
    <div class="modal fade" id="criteriaModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title fw-bold">Quản lý Tiêu chí Khung Năng lực</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <form action="${pageContext.request.contextPath}/admin/criteria/create" method="POST" class="mb-4">
                        <div class="row g-2 align-items-end">
                            <div class="col-md-4">
                                <label class="form-label small fw-semibold">Tên tiêu chí <span class="text-danger">*</span></label>
                                <input type="text" class="form-control" name="name" required placeholder="VD: Khả năng làm việc nhóm">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label small fw-semibold">Mô tả</label>
                                <input type="text" class="form-control" name="description" placeholder="Mô tả tiêu chí">
                            </div>
                            <div class="col-md-2">
                                <button type="submit" class="btn btn-success w-100"><i class="bi bi-plus"></i> Thêm</button>
                            </div>
                        </div>
                    </form>
                    
                    <div class="table-responsive">
                        <table class="table table-sm table-bordered">
                            <thead>
                                <tr>
                                    <th>Tên tiêu chí</th>
                                    <th>Mô tả</th>
                                    <th style="width:100px" class="text-center">Xóa</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="c" items="${criteria}">
                                    <tr>
                                        <td>${c.name}</td>
                                        <td>${c.description}</td>
                                        <td class="text-center">
                                            <form action="${pageContext.request.contextPath}/admin/criteria/delete" method="POST" onsubmit="return confirm('Xóa tiêu chí sẽ xóa tất cả câu hỏi liên quan. Chắc chắn?')">
                                                <input type="hidden" name="id" value="${c.id}">
                                                <button type="submit" class="btn btn-sm btn-danger py-0 px-2"><i class="bi bi-trash"></i></button>
                                            </form>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    </div>

<script>
function openEditQuestionModal(id, content, diff, answer, critId, jobTitle) {
    document.getElementById('editQuestionId').value = id;
    document.getElementById('editQJobTitle').value = jobTitle;
    document.getElementById('editQCriterionId').value = critId;
    document.getElementById('editQContent').value = content;
    document.getElementById('editQDifficulty').value = diff;
    document.getElementById('editQGoodAnswer').value = answer || '';
    new bootstrap.Modal(document.getElementById('editQuestionModal')).show();
}
</script>
<jsp:include page="../common/footer.jsp" />

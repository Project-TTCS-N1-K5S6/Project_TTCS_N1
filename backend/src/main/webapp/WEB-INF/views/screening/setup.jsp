<%--
  ==============================================================================
  FORM CẤU HÌNH TIÊU CHÍ SÀNG LỌC & CHẤM ĐIỂM HỒ SƠ (screening/setup.jsp)
  Chức năng:
  - Khai báo tiêu chí bắt buộc: số năm kinh nghiệm, bằng cấp, kỹ năng bắt buộc
  - Khai báo kỹ năng cộng điểm (nice-to-have)
  - Cấu hình trọng số điểm (tổng = 100)
  ==============================================================================
--%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<c:set var="pageTitle" value="Cấu hình tiêu chí sàng lọc" />
<c:set var="breadcrumb" value="Chấm điểm / Cấu hình tiêu chí" />
<c:set var="activeMenu" value="screening" />

<jsp:include page="../common/header.jsp" />
<jsp:include page="../common/sidebar.jsp" />

<style>
.setup-card {
    background: #fff;
    border: 1px solid #e5e7eb;
    border-radius: 16px;
    padding: 28px 32px;
}
.section-label {
    font-size: .75rem; font-weight: 700; text-transform: uppercase;
    letter-spacing: .06em; color: #6b7280; margin-bottom: 10px;
}
.required-badge {
    background: linear-gradient(135deg,#fdf4ff,#ede9fe);
    color: #7c3aed; border: 1px solid #ddd6fe;
    border-radius: 999px; padding: 2px 10px; font-size: .68rem; font-weight: 700;
}
.optional-badge {
    background: #f0fdf4; color: #16a34a; border: 1px solid #bbf7d0;
    border-radius: 999px; padding: 2px 10px; font-size: .68rem; font-weight: 700;
}
.weight-preview {
    display: flex; height: 12px; border-radius: 999px; overflow: hidden; gap: 2px;
    margin-top: 6px;
}
.weight-preview .w-exp  { background: #4f46e5; transition: width .3s; }
.weight-preview .w-skill{ background: #7c3aed; transition: width .3s; }
.weight-preview .w-edu  { background: #0ea5e9; transition: width .3s; }
.skill-tag-input-area {
    border: 1.5px dashed #d1d5db;
    border-radius: 10px; padding: 10px 12px;
    background: #fafafa; min-height: 52px;
    cursor: text;
}
.skill-tag-input-area:focus-within {
    border-color: #4f46e5; background: #fff;
    box-shadow: 0 0 0 3px rgba(79,70,229,.08);
}
.skill-chip {
    display: inline-flex; align-items: center; gap: 5px;
    background: #eff6ff; color: #1d4ed8;
    border: 1px solid #bfdbfe; border-radius: 999px;
    padding: 3px 10px 3px 10px; font-size: .78rem; font-weight: 600;
    margin: 2px;
}
.skill-chip.must { background:#fdf4ff; color:#7c3aed; border-color:#ddd6fe; }
.skill-chip.nice { background:#f0fdf4; color:#16a34a; border-color:#bbf7d0; }
.skill-chip .remove-tag {
    cursor: pointer; opacity: .5; font-size: .8rem; line-height: 1;
    margin-left: 2px; transition: opacity .1s;
}
.skill-chip .remove-tag:hover { opacity: 1; }
.weight-total-ok   { color: #16a34a; font-weight: 700; }
.weight-total-warn { color: #dc2626; font-weight: 700; }

/* Hướng dẫn chấm điểm panel bên phải */
.scoring-guide {
    background: linear-gradient(160deg,#1e1b4b,#312e81);
    border-radius: 14px; padding: 22px 22px;
    color: #e0e7ff;
}
.scoring-guide h6 { color: #a5b4fc; font-size: .78rem; text-transform: uppercase; letter-spacing: .06em; }
.guide-item {
    display: flex; align-items: flex-start; gap: 10px; margin-bottom: 12px;
}
.guide-icon {
    width: 32px; height: 32px; border-radius: 8px;
    display: flex; align-items: center; justify-content: center;
    font-size: 1rem; flex-shrink: 0;
}
</style>

<main class="app-main">
    <jsp:include page="../common/navbar.jsp" />
    <div class="app-content">
        <jsp:include page="../common/alerts.jsp" />

        <%-- Tiêu đề --%>
        <div class="d-flex align-items-center gap-3 mb-4">
            <a href="${pageContext.request.contextPath}/screening-criteria"
               class="btn btn-outline-secondary btn-sm" style="border-radius:8px;">
                <i class="bi bi-arrow-left"></i>
            </a>
            <div>
                <h3 class="fw-bold text-dark mb-1">
                    <i class="bi bi-sliders me-2 text-primary"></i>
                    <c:choose>
                        <c:when test="${criteria != null}">Chỉnh sửa tiêu chí sàng lọc</c:when>
                        <c:otherwise>Cấu hình tiêu chí sàng lọc mới</c:otherwise>
                    </c:choose>
                </h3>
                <div class="text-muted small">Khai báo tiêu chí bắt buộc và kỹ năng để hệ thống tính điểm phù hợp (0–100)</div>
            </div>
        </div>

        <div class="row g-4">
            <%-- Cột chính: Form cấu hình --%>
            <div class="col-12 col-xl-8">
                <form action="${pageContext.request.contextPath}/screening-criteria/save" method="POST"
                      id="setupForm" novalidate>
                    <input type="hidden" name="requisitionId" id="hiddenRequisitionId"
                           value="${not empty selectedRequisitionId ? selectedRequisitionId : (criteria != null ? criteria.requisitionId : '')}">

                    <%-- BƯỚC 1: Chọn vị trí tuyển dụng --%>
                    <div class="setup-card mb-4">
                        <div class="section-label"><i class="bi bi-briefcase me-1"></i>Bước 1 — Vị trí tuyển dụng</div>
                        <label for="requisitionSelect" class="form-label fw-semibold">
                            Chọn vị trí áp dụng tiêu chí <span class="text-danger">*</span>
                        </label>
                        <select class="form-select" id="requisitionSelect" required
                                onchange="document.getElementById('hiddenRequisitionId').value = this.value;">
                            <option value="">— Chọn vị trí tuyển dụng —</option>
                            <c:forEach var="req" items="${requisitions}">
                                <option value="${req.id}"
                                    <c:if test="${(criteria != null && req.id == criteria.requisitionId) || req.id == selectedRequisitionId}">selected</c:if>>
                                    <c:out value="${req.code}" /> — <c:out value="${req.title}" />
                                </option>
                            </c:forEach>
                        </select>
                        <div class="form-text">Mỗi vị trí chỉ có một bộ tiêu chí. Nếu đã có, thao tác lưu sẽ ghi đè.</div>
                    </div>

                    <%-- BƯỚC 2: Tiêu chí bắt buộc --%>
                    <div class="setup-card mb-4">
                        <div class="section-label">
                            <i class="bi bi-shield-exclamation me-1"></i>Bước 2 — Tiêu chí bắt buộc (Mandatory)
                            <span class="required-badge ms-1">Bắt buộc</span>
                        </div>
                        <div class="alert alert-warning alert-sm border border-warning-subtle small py-2 mb-3" style="border-radius:8px;">
                            <i class="bi bi-exclamation-triangle-fill me-1"></i>
                            Ứng viên phải đáp ứng <strong>đầy đủ tất cả</strong> tiêu chí dưới đây để nhận cờ <strong>ĐẠT</strong>.
                            Thiếu bất kỳ tiêu chí nào → cờ <strong>KHÔNG ĐẠT</strong> (chỉ để phân loại, không tự động loại hồ sơ).
                        </div>

                        <div class="row g-3">
                            <div class="col-12 col-md-6">
                                <label class="form-label fw-semibold">
                                    <i class="bi bi-calendar3 me-1 text-primary"></i>Số năm kinh nghiệm tối thiểu
                                </label>
                                <div class="input-group">
                                    <input type="number" class="form-control" id="minExperienceYears"
                                           name="minExperienceYears" min="0" max="30"
                                           value="${criteria != null ? criteria.minExperienceYears : 0}"
                                           placeholder="0">
                                    <span class="input-group-text text-muted">năm</span>
                                </div>
                                <div class="form-text">Nhập 0 nếu không yêu cầu kinh nghiệm cụ thể.</div>
                            </div>

                            <div class="col-12 col-md-6">
                                <label class="form-label fw-semibold">
                                    <i class="bi bi-mortarboard me-1 text-primary"></i>Bằng cấp tối thiểu yêu cầu
                                </label>
                                <select class="form-select" name="requiredDegree" id="requiredDegree">
                                    <option value="">Không yêu cầu bằng cấp cụ thể</option>
                                    <option value="Trung cấp"   ${criteria != null && criteria.requiredDegree == 'Trung cấp'   ? 'selected' : ''}>Trung cấp</option>
                                    <option value="Cao đẳng"    ${criteria != null && criteria.requiredDegree == 'Cao đẳng'    ? 'selected' : ''}>Cao đẳng</option>
                                    <option value="Cử nhân"     ${criteria != null && criteria.requiredDegree == 'Cử nhân'     ? 'selected' : ''}>Cử nhân (Đại học)</option>
                                    <option value="Kỹ sư"       ${criteria != null && criteria.requiredDegree == 'Kỹ sư'       ? 'selected' : ''}>Kỹ sư</option>
                                    <option value="Thạc sĩ"     ${criteria != null && criteria.requiredDegree == 'Thạc sĩ'     ? 'selected' : ''}>Thạc sĩ</option>
                                    <option value="Tiến sĩ"     ${criteria != null && criteria.requiredDegree == 'Tiến sĩ'     ? 'selected' : ''}>Tiến sĩ</option>
                                </select>
                            </div>

                            <%-- Must-have skills --%>
                            <div class="col-12">
                                <label class="form-label fw-semibold">
                                    <i class="bi bi-check2-circle me-1 text-danger"></i>Kỹ năng bắt buộc (Must-have)
                                    <span class="required-badge ms-1">Bắt buộc phải có</span>
                                </label>
                                <div class="skill-tag-input-area" id="mustHaveArea"
                                     onclick="document.getElementById('mustHaveInput').focus();">
                                    <div id="mustHaveChips" class="d-inline"></div>
                                    <input type="text" id="mustHaveInput" placeholder="Gõ kỹ năng + Enter để thêm (VD: Java, SQL, Python...)"
                                           class="border-0 bg-transparent outline-0 ms-1" style="outline:none;min-width:220px;">
                                </div>
                                <input type="hidden" name="mustHaveSkills" id="mustHaveHidden"
                                       value="${criteria != null ? criteria.mustHaveSkills : ''}">
                                <div class="form-text">Mỗi kỹ năng nhấn <kbd>Enter</kbd> hoặc dấu phẩy để thêm. Hệ thống sẽ tìm từ khóa này trong CV.</div>
                            </div>
                        </div>
                    </div>

                    <%-- BƯỚC 3: Kỹ năng cộng điểm --%>
                    <div class="setup-card mb-4">
                        <div class="section-label">
                            <i class="bi bi-plus-circle me-1"></i>Bước 3 — Kỹ năng cộng điểm (Nice-to-have)
                            <span class="optional-badge ms-1">Tùy chọn</span>
                        </div>
                        <label class="form-label fw-semibold">
                            <i class="bi bi-star me-1 text-success"></i>Kỹ năng ưu tiên
                        </label>
                        <div class="skill-tag-input-area" id="niceArea"
                             onclick="document.getElementById('niceInput').focus();">
                            <div id="niceChips" class="d-inline"></div>
                            <input type="text" id="niceInput" placeholder="Gõ kỹ năng + Enter để thêm (VD: Docker, Kubernetes, Agile...)"
                                   class="border-0 bg-transparent ms-1" style="outline:none;min-width:220px;">
                        </div>
                        <input type="hidden" name="niceToHaveSkills" id="niceHidden"
                               value="${criteria != null ? criteria.niceToHaveSkills : ''}">
                        <div class="form-text">Ứng viên có các kỹ năng này sẽ nhận điểm cộng thêm (không bắt buộc).</div>
                    </div>

                    <%-- BƯỚC 4: Trọng số điểm --%>
                    <div class="setup-card mb-4">
                        <div class="section-label"><i class="bi bi-pie-chart me-1"></i>Bước 4 — Phân bổ trọng số điểm (Tổng phải = 100)</div>

                        <div class="row g-3 mb-3">
                            <div class="col-12 col-md-4">
                                <label class="form-label fw-semibold" style="color:#4f46e5;">
                                    <i class="bi bi-briefcase me-1"></i>Kinh nghiệm <small class="text-muted">(max 40)</small>
                                </label>
                                <div class="input-group">
                                    <input type="number" class="form-control" id="weightExperience" name="weightExperience"
                                           min="0" max="100" value="${criteria != null ? criteria.weightExperience : 40}"
                                           oninput="updateWeightPreview()">
                                    <span class="input-group-text">điểm</span>
                                </div>
                            </div>
                            <div class="col-12 col-md-4">
                                <label class="form-label fw-semibold" style="color:#7c3aed;">
                                    <i class="bi bi-tools me-1"></i>Kỹ năng <small class="text-muted">(max 40)</small>
                                </label>
                                <div class="input-group">
                                    <input type="number" class="form-control" id="weightSkills" name="weightSkills"
                                           min="0" max="100" value="${criteria != null ? criteria.weightSkills : 40}"
                                           oninput="updateWeightPreview()">
                                    <span class="input-group-text">điểm</span>
                                </div>
                            </div>
                            <div class="col-12 col-md-4">
                                <label class="form-label fw-semibold" style="color:#0ea5e9;">
                                    <i class="bi bi-mortarboard me-1"></i>Bằng cấp <small class="text-muted">(max 20)</small>
                                </label>
                                <div class="input-group">
                                    <input type="number" class="form-control" id="weightEducation" name="weightEducation"
                                           min="0" max="100" value="${criteria != null ? criteria.weightEducation : 20}"
                                           oninput="updateWeightPreview()">
                                    <span class="input-group-text">điểm</span>
                                </div>
                            </div>
                        </div>

                        <div class="weight-preview" id="weightPreviewBar">
                            <div class="w-exp" id="previewExp" style="width:40%;"></div>
                            <div class="w-skill" id="previewSkill" style="width:40%;"></div>
                            <div class="w-edu" id="previewEdu" style="width:20%;"></div>
                        </div>
                        <div class="d-flex justify-content-between align-items-center mt-2 small">
                            <div>
                                <span style="color:#4f46e5;">● Kinh nghiệm</span>&nbsp;
                                <span style="color:#7c3aed;">● Kỹ năng</span>&nbsp;
                                <span style="color:#0ea5e9;">● Bằng cấp</span>
                            </div>
                            <div>Tổng: <span id="weightTotal" class="weight-total-ok">100</span>/100 điểm</div>
                        </div>
                    </div>

                    <%-- Nút lưu --%>
                    <div class="d-flex gap-3 justify-content-end">
                        <a href="${pageContext.request.contextPath}/screening-criteria"
                           class="btn btn-outline-secondary" style="border-radius:10px;">Huỷ</a>
                        <button type="submit" class="btn btn-primary px-4" id="btnSave" style="border-radius:10px;">
                            <i class="bi bi-floppy-fill me-1"></i>
                            <c:choose>
                                <c:when test="${criteria != null}">Cập nhật tiêu chí</c:when>
                                <c:otherwise>Lưu tiêu chí</c:otherwise>
                            </c:choose>
                        </button>
                    </div>
                </form>
            </div>

            <%-- Cột phụ: Hướng dẫn chấm điểm --%>
            <div class="col-12 col-xl-4">
                <div class="scoring-guide mb-4">
                    <h6 class="mb-3"><i class="bi bi-lightbulb-fill me-1"></i>Hướng dẫn chấm điểm</h6>

                    <div class="guide-item">
                        <div class="guide-icon" style="background:rgba(79,70,229,.3);">
                            <i class="bi bi-briefcase text-indigo-300" style="color:#a5b4fc;"></i>
                        </div>
                        <div>
                            <div class="fw-semibold" style="font-size:.85rem;color:#e0e7ff;">Kinh nghiệm (max 40đ)</div>
                            <div style="font-size:.75rem;color:#a5b4fc;">Hệ thống tìm pattern "X năm" trong CV. Đủ/vượt yêu cầu → điểm tối đa. Thiếu → tỉ lệ tuyến tính.</div>
                        </div>
                    </div>

                    <div class="guide-item">
                        <div class="guide-icon" style="background:rgba(124,58,237,.3);">
                            <i class="bi bi-tools" style="color:#c4b5fd;"></i>
                        </div>
                        <div>
                            <div class="fw-semibold" style="font-size:.85rem;color:#e0e7ff;">Kỹ năng (max 40đ)</div>
                            <div style="font-size:.75rem;color:#a5b4fc;">Must-have: 70% điểm kỹ năng. Nice-to-have: 30% điểm cộng thêm. Tìm từ khóa khớp trong CV.</div>
                        </div>
                    </div>

                    <div class="guide-item">
                        <div class="guide-icon" style="background:rgba(14,165,233,.3);">
                            <i class="bi bi-mortarboard" style="color:#7dd3fc;"></i>
                        </div>
                        <div>
                            <div class="fw-semibold" style="font-size:.85rem;color:#e0e7ff;">Bằng cấp (max 20đ)</div>
                            <div style="font-size:.75rem;color:#a5b4fc;">Khớp bằng cấp yêu cầu hoặc bằng cấp cao hơn → điểm tối đa. Không khớp → 0 điểm.</div>
                        </div>
                    </div>

                    <hr style="border-color:rgba(255,255,255,.15);">

                    <h6 class="mb-2"><i class="bi bi-flag-fill me-1"></i>Cờ tiêu chí bắt buộc</h6>
                    <div style="font-size:.78rem;color:#c7d2fe;">
                        <div class="mb-1">✅ <b>ĐẠT</b> → Đáp ứng ĐẦY ĐỦ: kinh nghiệm + bằng cấp + must-have skills</div>
                        <div class="mb-1">❌ <b>KHÔNG ĐẠT</b> → Thiếu BẤT KỲ tiêu chí bắt buộc nào</div>
                        <div class="mt-2" style="color:#fbbf24;"><i class="bi bi-exclamation-triangle me-1"></i>Lưu ý: Cờ chỉ dùng để lọc/sắp xếp. KHÔNG tự động loại hồ sơ.</div>
                    </div>

                    <hr style="border-color:rgba(255,255,255,.15);">

                    <h6 class="mb-2"><i class="bi bi-bar-chart-line-fill me-1"></i>Gợi ý mức độ ưu tiên</h6>
                    <div style="font-size:.78rem;color:#c7d2fe;">
                        <div class="mb-1">🔵 <b>Ưu tiên cao</b>: Đạt bắt buộc + điểm ≥ 75</div>
                        <div class="mb-1">🟡 <b>Ưu tiên trung bình</b>: Điểm ≥ 50</div>
                        <div class="mb-1">⚪ <b>Xem xét thêm</b>: Điểm &lt; 50</div>
                    </div>
                </div>

                <%-- Ví dụ thực tế --%>
                <div class="setup-card">
                    <div class="section-label mb-2"><i class="bi bi-file-earmark-text me-1"></i>Ví dụ kỹ năng</div>
                    <div class="small text-muted mb-2">Kỹ năng bắt buộc (cách nhau bằng phẩy):</div>
                    <code style="font-size:.75rem;background:#f1f5f9;padding:6px 10px;border-radius:6px;display:block;">
                        Java, Spring Boot, MySQL, Git
                    </code>
                    <div class="small text-muted mt-2 mb-2">Kỹ năng cộng điểm:</div>
                    <code style="font-size:.75rem;background:#f1f5f9;padding:6px 10px;border-radius:6px;display:block;">
                        Docker, Kubernetes, Redis, Microservices
                    </code>
                </div>
            </div>
        </div>
    </div>
</main>

<script>
// ===== Tag input cho kỹ năng =====
function initTagInput(inputId, chipsId, hiddenId, chipClass) {
    const input    = document.getElementById(inputId);
    const chipsDiv = document.getElementById(chipsId);
    const hidden   = document.getElementById(hiddenId);
    let tags = hidden.value ? hidden.value.split(',').map(s => s.trim()).filter(Boolean) : [];

    function render() {
        chipsDiv.innerHTML = '';
        tags.forEach((tag, idx) => {
            const chip = document.createElement('span');
            chip.className = 'skill-chip ' + chipClass;
            chip.innerHTML = tag + ' <span class="remove-tag" onclick="removeTag_' + hiddenId + '(' + idx + ')">✕</span>';
            chipsDiv.appendChild(chip);
        });
        hidden.value = tags.join(',');
    }

    window['removeTag_' + hiddenId] = function(idx) {
        tags.splice(idx, 1); render();
    };

    input.addEventListener('keydown', function(e) {
        if ((e.key === 'Enter' || e.key === ',') && this.value.trim()) {
            e.preventDefault();
            const val = this.value.replace(',', '').trim();
            if (val && !tags.includes(val)) { tags.push(val); render(); }
            this.value = '';
        }
        if (e.key === 'Backspace' && !this.value && tags.length) {
            tags.pop(); render();
        }
    });

    render();
}

// ===== Cập nhật thanh trọng số =====
function updateWeightPreview() {
    const exp   = parseInt(document.getElementById('weightExperience').value) || 0;
    const skill = parseInt(document.getElementById('weightSkills').value)     || 0;
    const edu   = parseInt(document.getElementById('weightEducation').value)  || 0;
    const total = exp + skill + edu;

    document.getElementById('previewExp').style.width   = Math.min(exp, 100) + '%';
    document.getElementById('previewSkill').style.width = Math.min(skill, 100) + '%';
    document.getElementById('previewEdu').style.width   = Math.min(edu, 100) + '%';

    const el = document.getElementById('weightTotal');
    el.textContent = total;
    el.className = total === 100 ? 'weight-total-ok' : 'weight-total-warn';
}

// ===== Validate trước submit =====
document.getElementById('setupForm').addEventListener('submit', function(e) {
    const reqId = document.getElementById('hiddenRequisitionId').value;
    if (!reqId) {
        e.preventDefault();
        alert('Vui lòng chọn vị trí tuyển dụng.');
        return;
    }
    const total = parseInt(document.getElementById('weightExperience').value || 0)
                + parseInt(document.getElementById('weightSkills').value || 0)
                + parseInt(document.getElementById('weightEducation').value || 0);
    if (total !== 100) {
        e.preventDefault();
        alert('Tổng trọng số 3 hạng mục phải bằng 100 (hiện tại: ' + total + '). Vui lòng điều chỉnh lại.');
        return;
    }
});

// Khởi động
document.addEventListener('DOMContentLoaded', function() {
    initTagInput('mustHaveInput', 'mustHaveChips', 'mustHaveHidden', 'must');
    initTagInput('niceInput',     'niceChips',     'niceHidden',     'nice');
    updateWeightPreview();
});
</script>

<jsp:include page="../common/footer.jsp" />

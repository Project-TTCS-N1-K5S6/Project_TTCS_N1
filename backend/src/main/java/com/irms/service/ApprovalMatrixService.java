package com.irms.service;

import com.irms.dao.ApprovalMatrixDAO;
import com.irms.dao.DepartmentDAO;
import com.irms.dao.RoleDAO;
import com.irms.dao.SalaryRangeDAO;
import com.irms.dao.UserDAO;
import com.irms.model.*;
import com.irms.util.SecurityUtil;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Service xử lý toàn bộ nghiệp vụ Ma trận phê duyệt, tự động bổ sung cấp khi vượt hạn mức,
 * và bảo toàn luồng snapshot phê duyệt độc lập với các thay đổi cấu hình.
 */
public class ApprovalMatrixService {

    private final ApprovalMatrixDAO matrixDAO = new ApprovalMatrixDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final RoleDAO roleDAO = new RoleDAO();
    private final UserDAO userDAO = new UserDAO();
    private final SalaryRangeDAO salaryRangeDAO = new SalaryRangeDAO();
    private final AuditService auditService = new AuditService();

    // ------------------------------------------------------------------------
    // QUẢN LÝ CẤU HÌNH MA TRẬN PHÊ DUYỆT (CRUD & VALIDATION)
    // ------------------------------------------------------------------------

    public List<ApprovalMatrix> getMatrices(String search, String departmentId, String status, int page, int pageSize) {
        return matrixDAO.findAll(search, departmentId, status, page, pageSize);
    }

    public int countMatrices(String search, String departmentId, String status) {
        return matrixDAO.countAll(search, departmentId, status);
    }

    public ApprovalMatrix getMatrixById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        return matrixDAO.findById(id.trim());
    }

    public boolean createMatrix(ApprovalMatrix matrix, String actorId, String ip, String userAgent) {
        if (matrix.getId() == null || matrix.getId().trim().isEmpty()) {
            matrix.setId(SecurityUtil.generateUUID());
        }
        if (matrix.getCode() == null || matrix.getCode().trim().isEmpty()) {
            matrix.setCode("APM-" + System.currentTimeMillis() % 100000);
        } else {
            matrix.setCode(matrix.getCode().trim().toUpperCase());
        }

        matrix.setCreatedBy(actorId);
        matrix.setUpdatedBy(actorId);

        // Kiểm tra tính hợp lệ trước khi lưu (Yêu cầu 3.3)
        validateMatrix(matrix, true);

        boolean success = matrixDAO.insert(matrix);
        if (success) {
            String desc = "Tạo mới cấu hình ma trận phê duyệt: " + matrix.getCode() + " - " + matrix.getName();
            auditService.log(actorId, "APPROVAL_MATRIX_CREATED", "APPROVAL_MATRIX", matrix.getId(), desc, ip, userAgent);
        }
        return success;
    }

    public boolean updateMatrix(ApprovalMatrix matrix, String actorId, String ip, String userAgent) {
        if (matrix.getId() == null || matrix.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã ID ma trận phê duyệt không hợp lệ!");
        }

        matrix.setCode(matrix.getCode() != null ? matrix.getCode().trim().toUpperCase() : "");
        matrix.setUpdatedBy(actorId);

        // Kiểm tra tính hợp lệ trước khi cập nhật (Yêu cầu 3.3)
        validateMatrix(matrix, false);

        boolean success = matrixDAO.update(matrix);
        if (success) {
            String desc = "Cập nhật cấu hình ma trận phê duyệt: " + matrix.getCode() + " - " + matrix.getName();
            auditService.log(actorId, "APPROVAL_MATRIX_UPDATED", "APPROVAL_MATRIX", matrix.getId(), desc, ip, userAgent);
        }
        return success;
    }

    public boolean toggleMatrixStatus(String id, String actorId, String ip, String userAgent) {
        ApprovalMatrix current = matrixDAO.findById(id);
        if (current == null) {
            throw new IllegalArgumentException("Không tìm thấy ma trận phê duyệt cần đổi trạng thái!");
        }

        String newStatus = "ACTIVE".equalsIgnoreCase(current.getStatus()) ? "INACTIVE" : "ACTIVE";
        boolean success = matrixDAO.updateStatus(id, newStatus, actorId);
        if (success) {
            String desc = ("ACTIVE".equals(newStatus) ? "Kích hoạt" : "Tạm dừng") + 
                          " ma trận phê duyệt: " + current.getCode() + " - " + current.getName();
            auditService.log(actorId, "APPROVAL_MATRIX_STATUS_CHANGED", "APPROVAL_MATRIX", id, desc, ip, userAgent);
        }
        return success;
    }

    public boolean deleteMatrix(String id, String actorId, String ip, String userAgent) {
        ApprovalMatrix current = matrixDAO.findById(id);
        if (current == null) {
            throw new IllegalArgumentException("Không tìm thấy ma trận phê duyệt cần xóa!");
        }

        boolean success = matrixDAO.delete(id);
        if (success) {
            String desc = "Xóa ma trận phê duyệt: " + current.getCode() + " - " + current.getName();
            auditService.log(actorId, "APPROVAL_MATRIX_DELETED", "APPROVAL_MATRIX", id, desc, ip, userAgent);
        }
        return success;
    }

    /**
     * Xác thực tính toàn vẹn của Ma trận phê duyệt trước khi lưu (Yêu cầu 3.3)
     */
    public void validateMatrix(ApprovalMatrix matrix, boolean isNew) {
        if (matrix.getCode() == null || matrix.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã cấu hình không được để trống!");
        }
        if (matrix.getName() == null || matrix.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên cấu hình phê duyệt không được để trống!");
        }

        // Kiểm tra trùng lặp mã cấu hình
        ApprovalMatrix exist = matrixDAO.findByCode(matrix.getCode());
        if (exist != null && (isNew || !exist.getId().equals(matrix.getId()))) {
            throw new IllegalArgumentException("Mã cấu hình '" + matrix.getCode() + "' đã tồn tại trên hệ thống!");
        }

        // Kiểm tra phòng ban nếu có chọn
        if (matrix.getDepartmentId() != null && !matrix.getDepartmentId().trim().isEmpty()) {
            Department dept = departmentDAO.findById(matrix.getDepartmentId());
            if (dept == null) {
                throw new IllegalArgumentException("Phòng ban áp dụng không tồn tại hoặc đã bị xóa!");
            }
        }

        // Kiểm tra tính hợp lệ của khoảng lương
        if (matrix.getMinSalary() != null && matrix.getMinSalary().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Mức lương tối thiểu không được mang giá trị âm!");
        }
        if (matrix.getMaxSalary() != null && matrix.getMaxSalary().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Mức lương tối đa không được mang giá trị âm!");
        }
        if (matrix.getMinSalary() != null && matrix.getMaxSalary() != null 
                && matrix.getMinSalary().compareTo(matrix.getMaxSalary()) > 0) {
            throw new IllegalArgumentException("Mức lương tối thiểu (" + matrix.getMinSalary() + 
                ") không được vượt quá mức lương tối đa (" + matrix.getMaxSalary() + ")!");
        }
        if (matrix.getThresholdAmount() != null && matrix.getThresholdAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Ngưỡng lương chuyển cấp không được mang giá trị âm!");
        }

        // Kiểm tra các cấp phê duyệt
        List<ApprovalMatrixStep> steps = matrix.getSteps();
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Cấu hình ma trận phê duyệt phải có ít nhất 1 cấp phê duyệt bắt buộc!");
        }

        boolean hasStandardStep = false;
        int expectedOrder = 1;

        for (ApprovalMatrixStep step : steps) {
            if (step.getStepName() == null || step.getStepName().trim().isEmpty()) {
                throw new IllegalArgumentException("Tên cấp phê duyệt thứ " + step.getStepOrder() + " không được để trống!");
            }

            // Kiểm tra tính liên tục của thứ tự cấp
            if (step.getStepOrder() != expectedOrder) {
                step.setStepOrder(expectedOrder);
            }
            expectedOrder++;

            // Kiểm tra loại người phê duyệt
            String type = step.getApproverType();
            if (type == null || type.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn loại người phê duyệt cho cấp: " + step.getStepName());
            }

            if ("ROLE".equalsIgnoreCase(type)) {
                if (step.getApproverRoleId() == null || step.getApproverRoleId().trim().isEmpty()) {
                    throw new IllegalArgumentException("Vui lòng chọn vai trò phê duyệt cho cấp: " + step.getStepName());
                }
                Role role = roleDAO.findById(step.getApproverRoleId());
                if (role == null) {
                    throw new IllegalArgumentException("Vai trò được gán cho cấp '" + step.getStepName() + "' không tồn tại!");
                }
            } else if ("SPECIFIC_USER".equalsIgnoreCase(type)) {
                if (step.getApproverUserId() == null || step.getApproverUserId().trim().isEmpty()) {
                    throw new IllegalArgumentException("Vui lòng chọn người dùng phụ trách cho cấp: " + step.getStepName());
                }
                User u = userDAO.findById(step.getApproverUserId());
                if (u == null || !"ACTIVE".equalsIgnoreCase(u.getStatus())) {
                    throw new IllegalArgumentException("Người phê duyệt được gán cho cấp '" + step.getStepName() + "' không tồn tại hoặc đã bị khóa!");
                }
            } else if (!"DEPT_MANAGER".equalsIgnoreCase(type)) {
                throw new IllegalArgumentException("Loại người phê duyệt không hợp lệ: " + type);
            }

            if (!step.isEscalationStep()) {
                hasStandardStep = true;
            }
        }

        if (!hasStandardStep) {
            throw new IllegalArgumentException("Ma trận phê duyệt phải có ít nhất 1 cấp thông thường (không phải cấp vượt hạn mức)!");
        }
    }

    // ------------------------------------------------------------------------
    // TỰ ĐỘNG XÁC ĐỊNH CHUỖI PHÊ DUYỆT & BỔ SUNG CẤP VƯỢT HẠN MỨC (Yêu cầu 3.2)
    // ------------------------------------------------------------------------

    /**
     * Xác định chuỗi phê duyệt chuẩn xác cho một yêu cầu tuyển dụng.
     * Tự động bổ sung cấp cao hơn khi vượt hạn mức hoặc vượt dải lương chuẩn của vị trí.
     */
    public List<RequisitionApprovalStep> resolveApprovalSteps(RecruitmentRequest req) {
        if (req == null) return new ArrayList<>();

        String deptId = req.getDepartmentId();
        BigDecimal proposedSalary = (req.getMaxSalary() != null) ? req.getMaxSalary() : req.getMinSalary();

        // 1. Tìm các cấu hình ma trận phê duyệt đang áp dụng cho phòng ban này (hoặc toàn công ty)
        List<ApprovalMatrix> candidates = matrixDAO.findActiveByDepartment(deptId);
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(
                "Chưa có Ma trận phê duyệt nào được cấu hình cho phòng ban này. " +
                "Vui lòng liên hệ Quản trị viên vào 'Quản trị hệ thống -> Ma trận phê duyệt' để thiết lập trước khi gửi duyệt!"
            );
        }

        // 2. Chọn ma trận phù hợp nhất
        ApprovalMatrix selectedMatrix = null;
        for (ApprovalMatrix m : candidates) {
            // Khớp theo khoảng lương nếu có
            if (m.getMinSalary() != null && proposedSalary != null && proposedSalary.compareTo(m.getMinSalary()) < 0) {
                continue;
            }
            if (m.getMaxSalary() != null && proposedSalary != null && proposedSalary.compareTo(m.getMaxSalary()) > 0) {
                continue;
            }
            selectedMatrix = m;
            break;
        }

        // Nếu không có ma trận nào khớp với mức lương đề xuất -> Bị chặn lưu theo yêu cầu 'Cấu hình sai (không có cấp duyệt nào khớp) bị chặn lưu'
        if (selectedMatrix == null) {
            throw new IllegalArgumentException(
                "Cấu hình sai: Không có ma trận hoặc cấp duyệt nào khớp với mức lương đề xuất (" +
                formatMoney(proposedSalary) + ") của phòng ban này. Yêu cầu gửi duyệt bị chặn lưu!"
            );
        }

        // 3. Kiểm tra xem mức lương đề xuất có vượt hạn mức hay không
        boolean isExceeded = false;
        String exceedReason = null;

        // a. So sánh với ngưỡng của chính ma trận (threshold_amount)
        if (selectedMatrix.getThresholdAmount() != null && proposedSalary != null) {
            if (proposedSalary.compareTo(selectedMatrix.getThresholdAmount()) > 0) {
                isExceeded = true;
                exceedReason = "Mức lương đề xuất (" + formatMoney(proposedSalary) + 
                               ") vượt ngưỡng chuyển cấp (" + formatMoney(selectedMatrix.getThresholdAmount()) + ")";
            }
        }

        // b. So sánh với dải lương chuẩn của chức danh vị trí
        if (!isExceeded && proposedSalary != null && req.getPositionTitle() != null) {
            SalaryRange standard = salaryRangeDAO.findByDepartmentAndPosition(deptId, req.getPositionTitle());
            if (standard != null && standard.getMaxSalary() != null) {
                if (proposedSalary.compareTo(standard.getMaxSalary()) > 0) {
                    isExceeded = true;
                    exceedReason = "Mức lương đề xuất (" + formatMoney(proposedSalary) + 
                                   ") vượt mức trần chức danh chuẩn (" + formatMoney(standard.getMaxSalary()) + ")";
                }
            }
        }

        // c. Hoặc nếu yêu cầu tuyển dụng đã có bản giải trình vượt lương
        if (!isExceeded && req.getSalaryExplanation() != null && !req.getSalaryExplanation().trim().isEmpty()) {
            isExceeded = true;
            exceedReason = "Có nội dung giải trình ngân sách lương vượt chuẩn";
        }

        // 4. Xây dựng danh sách các bước phê duyệt được SNAPSHOT (Yêu cầu 3.2 & 3.4)
        List<RequisitionApprovalStep> resultSteps = new ArrayList<>();
        int currentOrder = 1;

        // a. Lấy các cấp thông thường trước (không bỏ qua cấp trung gian)
        for (ApprovalMatrixStep s : selectedMatrix.getSteps()) {
            if (!s.isEscalationStep()) {
                RequisitionApprovalStep step = createSnapshotStep(req, selectedMatrix.getId(), currentOrder++, s);
                resultSteps.add(step);
            }
        }

        // b. Nếu VƯỢT HẠN MỨC: Tự động bổ sung các cấp cao hơn theo đúng quy tắc
        if (isExceeded) {
            for (ApprovalMatrixStep s : selectedMatrix.getSteps()) {
                if (s.isEscalationStep()) {
                    RequisitionApprovalStep step = createSnapshotStep(req, selectedMatrix.getId(), currentOrder++, s);
                    if (exceedReason != null) {
                        step.setStepName(step.getStepName() + " (Tự động bổ sung do " + exceedReason + ")");
                    }
                    resultSteps.add(step);
                }
            }
        }

        if (resultSteps.isEmpty()) {
            throw new IllegalArgumentException("Không xác định được chuỗi phê duyệt hợp lệ từ cấu hình ma trận!");
        }

        return resultSteps;
    }

    /**
     * Tạo một bước snapshot độc lập gắn với yêu cầu tuyển dụng (Yêu cầu 3.4)
     */
    private RequisitionApprovalStep createSnapshotStep(RecruitmentRequest req, String matrixId, int order, ApprovalMatrixStep s) {
        RequisitionApprovalStep step = new RequisitionApprovalStep();
        step.setId(SecurityUtil.generateUUID());
        step.setRequisitionId(req.getId());
        step.setMatrixId(matrixId);
        step.setStepOrder(order);
        step.setStepName(s.getStepName());
        step.setApproverType(s.getApproverType());
        step.setApproverRoleId(s.getApproverRoleId());
        step.setApproverUserId(s.getApproverUserId());
        step.setStatus("PENDING");

        // Xác định tên người / vai trò đại diện tại thời điểm snapshot
        String assignedName = "Người phê duyệt";
        if ("DEPT_MANAGER".equalsIgnoreCase(s.getApproverType())) {
            Department dept = departmentDAO.findById(req.getDepartmentId());
            if (dept != null && dept.getManagerId() != null) {
                User mgr = userDAO.findById(dept.getManagerId());
                assignedName = (mgr != null) ? "Trưởng phòng: " + mgr.getFullName() : "Trưởng phòng " + dept.getName();
            } else if (dept != null) {
                assignedName = "Trưởng phòng " + dept.getName();
            } else {
                assignedName = "Trưởng bộ phận chuyên môn";
            }
        } else if ("ROLE".equalsIgnoreCase(s.getApproverType())) {
            Role r = roleDAO.findById(s.getApproverRoleId());
            assignedName = (r != null) ? r.getName() : "Vai trò hệ thống";
        } else if ("SPECIFIC_USER".equalsIgnoreCase(s.getApproverType())) {
            User u = userDAO.findById(s.getApproverUserId());
            assignedName = (u != null) ? u.getFullName() : "Người dùng chỉ định";
        }

        step.setAssignedApproverName(assignedName);
        return step;
    }

    /**
     * Lưu snapshot chuỗi phê duyệt xuống cơ sở dữ liệu khi gửi duyệt
     */
    public boolean saveRequisitionApprovalChain(RecruitmentRequest req) {
        List<RequisitionApprovalStep> steps = resolveApprovalSteps(req);
        // Xóa các bước cũ nếu có trước khi ghi mới
        matrixDAO.deleteRequisitionSteps(req.getId());
        return matrixDAO.saveRequisitionSteps(steps);
    }

    public List<RequisitionApprovalStep> getRequisitionApprovalSteps(String requisitionId) {
        return matrixDAO.getRequisitionSteps(requisitionId);
    }

    // ------------------------------------------------------------------------
    // XỬ LÝ PHÊ DUYỆT & TỪ CHỐI TỪNG CẤP TRONG LUỒNG ĐANG CHẠY (WORKFLOW ENGINE)
    // ------------------------------------------------------------------------

    public boolean approveRequisitionStep(String requisitionId, String stepId, String comment, User actor, String ip, String userAgent) {
        List<RequisitionApprovalStep> steps = matrixDAO.getRequisitionSteps(requisitionId);
        if (steps.isEmpty()) {
            throw new IllegalArgumentException("Yêu cầu này chưa được khởi tạo chuỗi cấp phê duyệt!");
        }

        RequisitionApprovalStep targetStep = null;
        int targetIndex = -1;
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getId().equals(stepId)) {
                targetStep = steps.get(i);
                targetIndex = i;
                break;
            }
        }

        if (targetStep == null) {
            throw new IllegalArgumentException("Không tìm thấy bước phê duyệt!");
        }

        // Kiểm tra thứ tự: không được duyệt vượt cấp nếu các cấp trước chưa duyệt xong
        for (int i = 0; i < targetIndex; i++) {
            if (!"APPROVED".equalsIgnoreCase(steps.get(i).getStatus())) {
                throw new IllegalArgumentException("Không thể phê duyệt cấp này khi cấp " + steps.get(i).getStepOrder() + " chưa được duyệt hoàn tất!");
            }
        }

        // Kiểm tra thẩm quyền phê duyệt
        validateApproverAuthority(targetStep, actor, requisitionId);

        boolean success = matrixDAO.updateRequisitionStepStatus(stepId, "APPROVED", comment, actor.getId());
        if (success) {
            // Kiểm tra xem tất cả các bước đã được duyệt hết chưa
            List<RequisitionApprovalStep> updatedSteps = matrixDAO.getRequisitionSteps(requisitionId);
            boolean allApproved = true;
            for (RequisitionApprovalStep s : updatedSteps) {
                if (!"APPROVED".equalsIgnoreCase(s.getStatus())) {
                    allApproved = false;
                    break;
                }
            }

            auditService.log(actor.getId(), "REQUISITION_STEP_APPROVED", "REQUISITION", requisitionId, 
                "Phê duyệt " + targetStep.getStepName() + (allApproved ? " (Đã hoàn tất toàn bộ quy trình phê duyệt)" : ""), 
                ip, userAgent);

            return allApproved;
        }
        return false;
    }

    public boolean rejectRequisitionStep(String requisitionId, String stepId, String reason, User actor, String ip, String userAgent) {
        List<RequisitionApprovalStep> steps = matrixDAO.getRequisitionSteps(requisitionId);
        RequisitionApprovalStep targetStep = null;
        for (RequisitionApprovalStep s : steps) {
            if (s.getId().equals(stepId)) {
                targetStep = s;
                break;
            }
        }

        if (targetStep == null) {
            throw new IllegalArgumentException("Không tìm thấy bước phê duyệt!");
        }

        validateApproverAuthority(targetStep, actor, requisitionId);

        boolean success = matrixDAO.updateRequisitionStepStatus(stepId, "REJECTED", reason, actor.getId());
        if (success) {
            auditService.log(actor.getId(), "REQUISITION_STEP_REJECTED", "REQUISITION", requisitionId, 
                "Từ chối tại " + targetStep.getStepName() + ". Lý do: " + reason, ip, userAgent);
        }
        return success;
    }

    private void validateApproverAuthority(RequisitionApprovalStep step, User actor, String requisitionId) {
        if (actor == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để thực hiện quyền phê duyệt!");
        }

        // Quản trị viên hệ thống có toàn quyền phê duyệt
        if (actor.hasRole("ADMIN")) return;

        if ("ROLE".equalsIgnoreCase(step.getApproverType())) {
            Role reqRole = roleDAO.findById(step.getApproverRoleId());
            if (reqRole != null && actor.hasRole(reqRole.getCode())) return;
            throw new IllegalArgumentException("Bạn không sở hữu vai trò '" + step.getAssignedApproverName() + "' để phê duyệt cấp này!");
        }

        if ("SPECIFIC_USER".equalsIgnoreCase(step.getApproverType())) {
            if (actor.getId().equals(step.getApproverUserId())) return;
            throw new IllegalArgumentException("Chỉ người dùng '" + step.getAssignedApproverName() + "' mới có quyền phê duyệt cấp này!");
        }

        if ("DEPT_MANAGER".equalsIgnoreCase(step.getApproverType())) {
            if (actor.hasRole("HR_MANAGER") || actor.hasRole("ADMIN")) return;
            // Kiểm tra xem actor có phải là manager của phòng ban trong yêu cầu hay không
            // Sẽ được cho phép nếu người dùng có quyền quản trị phòng ban
            return;
        }
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "0 đ";
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");
        return df.format(amount) + " đ";
    }
}

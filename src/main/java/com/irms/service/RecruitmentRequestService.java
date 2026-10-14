package com.irms.service;

import com.irms.dao.CandidateDAO;
import com.irms.dao.HeadcountBudgetDAO;
import com.irms.dao.JobPostingDAO;
import com.irms.dao.RecruitmentApprovalDAO;
import com.irms.dao.RecruitmentRequestDAO;
import com.irms.dao.SalaryRangeDAO;
import com.irms.dao.UserDAO;
import com.irms.model.DepartmentHeadcountBudget;
import com.irms.model.RecruitmentApprovalLog;
import com.irms.model.RecruitmentRequest;
import com.irms.model.SalaryRange;
import com.irms.model.RequisitionApprovalStep;
import com.irms.model.User;
import com.irms.util.SecurityUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Xử lý nghiệp vụ Yêu cầu tuyển dụng (Job Requisitions)
 */
public class RecruitmentRequestService {
    private final RecruitmentRequestDAO requestDAO = new RecruitmentRequestDAO();
    private final SalaryRangeDAO salaryRangeDAO = new SalaryRangeDAO();
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final JobPostingDAO jobPostingDAO = new JobPostingDAO();
    private final ApprovalMatrixService approvalMatrixService = new ApprovalMatrixService();
    private final RecruitmentApprovalDAO approvalDAO = new RecruitmentApprovalDAO();
    private final HeadcountBudgetDAO budgetDAO = new HeadcountBudgetDAO();
    private final UserDAO userDAO = new UserDAO();
    private final AuditService auditService = new AuditService();

    public CandidateDAO getCandidateDAO() { return candidateDAO; }
    public JobPostingDAO getJobPostingDAO() { return jobPostingDAO; }
    public RecruitmentApprovalDAO getApprovalDAO() { return approvalDAO; }
    public HeadcountBudgetDAO getBudgetDAO() { return budgetDAO; }

    public List<RecruitmentRequest> getAllRequests(String search, String status, String departmentId) {
        return requestDAO.findAll(search, status, departmentId);
    }

    public List<RecruitmentRequest> getRequests(String search, String status, String departmentId, int page, int pageSize) {
        int p = Math.max(1, page);
        int size = Math.max(1, pageSize);
        int offset = (p - 1) * size;
        return requestDAO.findAll(search, status, departmentId, offset, size);
    }

    public int countRequests(String search, String status, String departmentId) {
        return requestDAO.countAll(search, status, departmentId);
    }
    public RecruitmentRequest getRequestById(String id) {
        return requestDAO.findById(id);
    }

    public String getNextCode() {
        return requestDAO.generateNextCode();
    }

    public SalaryRange getStandardSalaryRange(String departmentId, String positionTitle) {
        return salaryRangeDAO.findByDepartmentAndPosition(departmentId, positionTitle);
    }

    /**
     * Kiểm tra xem dải lương đề xuất có nằm ngoài dải chuẩn của chức danh hay không
     */
    public boolean isSalaryOutOfStandardRange(String departmentId, String positionTitle, BigDecimal proposedMin, BigDecimal proposedMax) {
        if (positionTitle == null || positionTitle.trim().isEmpty()) {
            return false;
        }
        SalaryRange standard = salaryRangeDAO.findByDepartmentAndPosition(departmentId, positionTitle);
        if (standard == null) {
            return false;
        }

        boolean out = false;
        if (proposedMin != null && standard.getMinSalary() != null) {
            if (proposedMin.compareTo(standard.getMinSalary()) < 0) {
                out = true;
            }
        }
        if (proposedMax != null && standard.getMaxSalary() != null) {
            if (proposedMax.compareTo(standard.getMaxSalary()) > 0) {
                out = true;
            }
        }
        return out;
    }

    /**
     * Xác thực các tiêu chí theo yêu cầu nghiệp vụ
     */
    public void validateRequest(RecruitmentRequest req, boolean isDraft) {
        // 1. Chức danh không được để trống
        if (req.getPositionTitle() == null || req.getPositionTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn hoặc nhập chức danh cần tuyển!");
        }

        // Tự động sinh tiêu đề nếu người dùng chưa nhập
        if (req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            req.setTitle("Tuyển dụng " + req.getPositionTitle().trim());
        }

        // 2. Ngày cần người không được ở quá khứ
        if (req.getDeadline() != null) {
            LocalDate today = LocalDate.now();
            LocalDate reqDate = req.getDeadline().toLocalDate();
            if (reqDate.isBefore(today)) {
                throw new IllegalArgumentException("Ngày cần người không được ở quá khứ (phải từ hôm nay trở đi)!");
            }
        }

        // 3. Kiểm tra lương tối thiểu <= lương tối đa
        if (req.getMinSalary() != null && req.getMaxSalary() != null) {
            if (req.getMinSalary().compareTo(req.getMaxSalary()) > 0) {
                throw new IllegalArgumentException("Dải lương đề xuất không hợp lệ: Lương tối thiểu không được lớn hơn lương tối đa!");
            }
            if (req.getMinSalary().compareTo(BigDecimal.ZERO) < 0 || req.getMaxSalary().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Dải lương đề xuất không được là số âm!");
            }
        }

        // 4. Kiểm tra dải lương đề xuất so với dải chuẩn của chức danh
        SalaryRange standard = salaryRangeDAO.findByDepartmentAndPosition(req.getDepartmentId(), req.getPositionTitle());
        boolean outOfRange = false;
        if (standard != null) {
            if (req.getMinSalary() != null && standard.getMinSalary() != null && req.getMinSalary().compareTo(standard.getMinSalary()) < 0) {
                outOfRange = true;
            }
            if (req.getMaxSalary() != null && standard.getMaxSalary() != null && req.getMaxSalary().compareTo(standard.getMaxSalary()) > 0) {
                outOfRange = true;
            }
        }

        // Nếu ngoài dải chuẩn thì BẮT BUỘC nhập giải trình (ngay cả khi gửi duyệt, và khuyến khích khi lưu nháp)
        if (outOfRange && !isDraft) {
            if (req.getSalaryExplanation() == null || req.getSalaryExplanation().trim().isEmpty()) {
                String rangeText = (standard != null) ? standard.getFormattedRange() : "khung chuẩn";
                throw new IllegalArgumentException("Dải lương đề xuất nằm ngoài dải chuẩn của chức danh (" + rangeText + "). Bạn bắt buộc phải nhập nội dung giải trình ngân sách!");
            }
        }

        // 5. Nếu không phải lưu nháp (Gửi duyệt), kiểm tra nghiêm ngặt toàn bộ tiêu chí
        if (!isDraft) {
            if (req.getDepartmentId() == null || req.getDepartmentId().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn phòng ban cần tuyển!");
            }
            if (req.getHeadcount() <= 0) {
                throw new IllegalArgumentException("Số lượng tuyển dụng phải lớn hơn 0!");
            }
            if (req.getRecruitmentReason() == null || req.getRecruitmentReason().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn lý do tuyển dụng (Thay thế hoặc Tăng mới)!");
            }
            if (req.getDeadline() == null) {
                throw new IllegalArgumentException("Vui lòng nhập ngày cần người (không được ở quá khứ)!");
            }
            if (req.getJobDescription() == null || req.getJobDescription().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng soạn bản mô tả công việc (Job Description)!");
            }
            if (req.getJobRequirements() == null || req.getJobRequirements().trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng soạn yêu cầu ứng viên (Candidate Requirements)!");
            }
            if (req.getMinSalary() == null || req.getMaxSalary() == null) {
                throw new IllegalArgumentException("Vui lòng nhập đầy đủ dải lương đề xuất (từ - đến)!");
            }
        }
    }

    /**
     * Kiểm tra chỉ tiêu headcount và ngân sách lương của phòng ban (KN-195, KN-199)
     * Vượt chỉ tiêu là cảnh báo chặn, cần Trưởng phòng Nhân sự xác nhận ghi đè kèm lý do.
     */
    public void validateBudgetAndHeadcount(RecruitmentRequest req, String actorId) {
        if (req.getDepartmentId() == null || req.getDepartmentId().trim().isEmpty()) {
            return;
        }

        int fiscalYear = (req.getDeadline() != null)
                ? req.getDeadline().toLocalDate().getYear()
                : LocalDate.now().getYear();

        DepartmentHeadcountBudget budget = budgetDAO.getBudgetByDeptAndYear(req.getDepartmentId(), fiscalYear, req.getId());

        if (budget == null) {
            throw new IllegalArgumentException("Phòng ban chưa được khai báo chỉ tiêu định biên & ngân sách năm " 
                    + fiscalYear + "! Vui lòng liên hệ Trưởng phòng Nhân sự để khai báo trước khi gửi duyệt yêu cầu.");
        }

        int remainingHc = budget.getRemainingHeadcount();
        boolean hcExceeded = req.getHeadcount() > remainingHc;

        BigDecimal salaryPerPerson = req.getMaxSalary() != null ? req.getMaxSalary() 
                : (req.getMinSalary() != null ? req.getMinSalary() : BigDecimal.ZERO);
        BigDecimal proposedCost = salaryPerPerson.multiply(BigDecimal.valueOf(req.getHeadcount()));
        BigDecimal remainingBudget = budget.getRemainingSalaryBudget();
        boolean bgExceeded = (proposedCost.compareTo(BigDecimal.ZERO) > 0 && proposedCost.compareTo(remainingBudget) > 0);

        if (hcExceeded || bgExceeded) {
            if (!req.isBudgetOverridden()) {
                StringBuilder sb = new StringBuilder("CẢNH BÁO CHẶN: Yêu cầu tuyển dụng vượt chỉ tiêu của phòng ban năm ")
                        .append(fiscalYear).append("! ");
                if (hcExceeded) {
                    sb.append("Số lượng tuyển (").append(req.getHeadcount())
                      .append(" nhân sự) vượt quá headcount còn lại (").append(remainingHc).append(" nhân sự). ");
                }
                if (bgExceeded) {
                    sb.append("Chi phí lương đề xuất vượt quá ngân sách còn lại (Còn lại: ")
                      .append(budget.getFormattedRemainingBudget()).append("). ");
                }
                sb.append("Theo quy định, cần Trưởng phòng Nhân sự xác nhận ghi đè kèm lý do để tiếp tục gửi duyệt!");
                throw new IllegalArgumentException(sb.toString());
            }

            // Người dùng chọn xác nhận ghi đè: kiểm tra thẩm quyền tại backend (chỉ HR_MANAGER hoặc ADMIN)
            User actor = (actorId != null) ? userDAO.findById(actorId) : null;
            boolean canOverride = (actor != null && (actor.hasRole("ADMIN") || actor.hasRole("HR_MANAGER") || actor.hasPermission("budget.override")));
            if (!canOverride) {
                throw new IllegalArgumentException("BẠN KHÔNG CÓ THẨM QUYỀN: Thao tác xác nhận ghi đè vượt chỉ tiêu định biên/ngân sách chỉ dành cho Trưởng phòng Nhân sự hoặc Quản trị hệ thống!");
            }

            if (req.getBudgetOverrideReason() == null || req.getBudgetOverrideReason().trim().isEmpty()) {
                throw new IllegalArgumentException("Bắt buộc nhập lý do khi xác nhận ghi đè vượt chỉ tiêu định biên hoặc ngân sách!");
            }

            req.setBudgetOverridden(true);
            req.setBudgetOverriddenBy(actorId);
            req.setBudgetOverriddenAt(new java.sql.Timestamp(System.currentTimeMillis()));
            req.setBudgetOverrideReason(req.getBudgetOverrideReason().trim());
        }
    }

    public boolean createRequest(RecruitmentRequest req, boolean isDraft, String actorId, String ip, String userAgent) {
        if (req.getId() == null || req.getId().trim().isEmpty()) {
            req.setId(SecurityUtil.generateUUID());
        }
        if (req.getCode() == null || req.getCode().trim().isEmpty()) {
            req.setCode(requestDAO.generateNextCode());
        }
        req.setStatus(isDraft ? "DRAFT" : "PENDING_APPROVAL");
        req.setCreatedBy(actorId);

        validateRequest(req, isDraft);

        // Kiểm tra định biên & ngân sách phòng ban nếu gửi duyệt
        if (!isDraft) {
            validateBudgetAndHeadcount(req, actorId);
        }

        // Xác thực ma trận phê duyệt trước khi lưu nếu gửi duyệt
        if (!isDraft) {
            approvalMatrixService.resolveApprovalSteps(req);
        }

        boolean success = requestDAO.insert(req);
        if (success) {
            if (!isDraft) {
                approvalMatrixService.saveRequisitionApprovalChain(req);
                // Tự động khởi tạo chuỗi phê duyệt 3 cấp / nhật ký
                approvalDAO.initApprovalChain(req.getId(), req.getHiringManagerId());
            }
            String action = isDraft ? "REQUISITION_DRAFT_CREATED" : "REQUISITION_SUBMITTED";
            String desc = (isDraft ? "Lưu nháp yêu cầu tuyển dụng: " : "Gửi phê duyệt yêu cầu tuyển dụng: ") 
                          + req.getCode() + " - " + req.getTitle();
            if (req.isBudgetOverridden()) {
                desc += " [Xác nhận ghi đè vượt chỉ tiêu: " + req.getBudgetOverrideReason() + "]";
            }
            auditService.log(actorId, action, "REQUISITION", req.getId(), desc, ip, userAgent);
        }
        return success;
    }

    public boolean updateRequest(RecruitmentRequest req, boolean isDraft, String actorId, String ip, String userAgent) {
        RecruitmentRequest current = requestDAO.findById(req.getId());
        if (current == null) {
            throw new IllegalArgumentException("Không tìm thấy yêu cầu tuyển dụng có mã ID: " + req.getId());
        }

        req.setStatus(isDraft ? "DRAFT" : "PENDING_APPROVAL");
        validateRequest(req, isDraft);

        // Kiểm tra định biên & ngân sách phòng ban nếu gửi duyệt
        if (!isDraft) {
            validateBudgetAndHeadcount(req, actorId);
        }

        if (!isDraft) {
            approvalMatrixService.resolveApprovalSteps(req);
        }

        boolean success = requestDAO.update(req);
        if (success) {
            if (!isDraft) {
                approvalMatrixService.saveRequisitionApprovalChain(req);
                // Tự động khởi tạo chuỗi phê duyệt nếu chưa có
                if (approvalDAO.countSteps(req.getId()) == 0) {
                    approvalDAO.initApprovalChain(req.getId(), req.getHiringManagerId());
                }
            }
            String action = isDraft ? "REQUISITION_DRAFT_UPDATED" : "REQUISITION_SUBMITTED";
            String desc = "Cập nhật yêu cầu tuyển dụng: " + req.getCode() + " - " + req.getTitle();
            if (req.isBudgetOverridden()) {
                desc += " [Xác nhận ghi đè vượt chỉ tiêu: " + req.getBudgetOverrideReason() + "]";
            }
            auditService.log(actorId, action, "REQUISITION", req.getId(), desc, ip, userAgent);
        }
        return success;
    }

    public List<RequisitionApprovalStep> getApprovalSteps(String requisitionId) {
        return approvalMatrixService.getRequisitionApprovalSteps(requisitionId);
    }

    public boolean approveStep(String requisitionId, String stepId, String comment, User actor, String ip, String userAgent) {
        boolean allApproved = approvalMatrixService.approveRequisitionStep(requisitionId, stepId, comment, actor, ip, userAgent);
        if (allApproved) {
            requestDAO.updateStatus(requisitionId, "OPEN");
        }
        return true;
    }

    public boolean rejectStep(String requisitionId, String stepId, String reason, User actor, String ip, String userAgent) {
        boolean success = approvalMatrixService.rejectRequisitionStep(requisitionId, stepId, reason, actor, ip, userAgent);
        if (success) {
            requestDAO.updateStatus(requisitionId, "REJECTED");
        }
        return success;
    }

    public boolean deleteRequest(String id, String actorId, String ip, String userAgent) {
        RecruitmentRequest current = requestDAO.findById(id);
        if (current == null) return false;

        // KN-155: Khóa lịch sử phê duyệt không cho sửa hoặc xóa
        if (approvalDAO.countSteps(id) > 0) {
            List<RecruitmentApprovalLog> steps = approvalDAO.findByRequisitionId(id);
            for (RecruitmentApprovalLog s : steps) {
                if (s.isApproved() || s.isRejected()) {
                    throw new IllegalStateException("LỊCH SỬ PHÊ DUYỆT ĐÃ KHÓA: Yêu cầu này đã phát sinh nhật ký phê duyệt thực tế, không thể xóa!");
                }
            }
            approvalDAO.deleteApprovalChain(id);
        }

        boolean success = requestDAO.delete(id);
        if (success) {
            auditService.log(actorId, "REQUISITION_DELETED", "REQUISITION", id, 
                    "Xóa yêu cầu tuyển dụng: " + current.getCode(), ip, userAgent);
        }
        return success;
    }

    /**
     * Kiểm tra trạng thái ứng viên trong pipeline và tin tuyển dụng đang đăng trước khi đóng yêu cầu
     */
    public java.util.Map<String, Object> checkPipelineAndPostings(String id) {
        RecruitmentRequest req = requestDAO.findById(id);
        if (req == null) {
            throw new IllegalArgumentException("Không tìm thấy yêu cầu tuyển dụng có ID: " + id);
        }

        List<com.irms.model.Candidate> activeCandidates = candidateDAO.findActivePipelineCandidates(id);
        List<com.irms.model.JobPosting> publishedPostings = jobPostingDAO.findPublishedByRequisitionId(id);

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("requisitionId", req.getId());
        result.put("requisitionCode", req.getCode());
        result.put("requisitionTitle", req.getTitle());
        result.put("currentStatus", req.getStatus());
        result.put("activeCandidatesCount", activeCandidates.size());
        result.put("activeCandidates", activeCandidates);
        result.put("activePostingsCount", publishedPostings.size());
        result.put("activePostings", publishedPostings);
        return result;
    }

    /**
     * Đóng / Kết thúc yêu cầu tuyển dụng với một trong ba trạng thái:
     * - FULFILLED (Đã tuyển đủ)
     * - PAUSED (Tạm dừng)
     * - CANCELLED (Huỷ)
     *
     * Đồng thời:
     * 1. Tự động gỡ các tin tuyển dụng đang đăng (PUBLISHED -> UNPUBLISHED)
     * 2. Xử lý các ứng viên còn đang trong pipeline theo phương án người dùng chọn
     * 3. Ghi nhật ký kiểm toán hệ thống (Audit Log)
     */
    public java.util.Map<String, Object> closeRequest(String id, String terminalStatus, String closeReason, 
                                                      String closeNotes, String candidateAction, 
                                                      String actorId, String ip, String userAgent) {
        RecruitmentRequest req = requestDAO.findById(id);
        if (req == null) {
            throw new IllegalArgumentException("Không tìm thấy yêu cầu tuyển dụng có ID: " + id);
        }

        // Kiểm tra xem yêu cầu đã ở trạng thái kết thúc chưa
        if (req.isTerminal()) {
            throw new IllegalStateException("Yêu cầu tuyển dụng này đã ở trạng thái kết thúc (" + req.getStatusLabel() + ")!");
        }

        // Xác thực trạng thái kết thúc hợp lệ
        if (!"FULFILLED".equalsIgnoreCase(terminalStatus) &&
            !"PAUSED".equalsIgnoreCase(terminalStatus) &&
            !"CANCELLED".equalsIgnoreCase(terminalStatus) &&
            !"CLOSED".equalsIgnoreCase(terminalStatus)) {
            throw new IllegalArgumentException("Trạng thái kết thúc không hợp lệ! Chỉ chấp nhận: Đã tuyển đủ (FULFILLED), Tạm dừng (PAUSED), hoặc Huỷ (CANCELLED).");
        }

        // Chuẩn hóa tên trạng thái
        String normalizedStatus = terminalStatus.toUpperCase();

        // Kiểm tra lý do đóng
        if (closeReason == null || closeReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn hoặc nhập lý do kết thúc yêu cầu tuyển dụng!");
        }

        // Kiểm tra ứng viên trong pipeline
        int activeCandidatesCount = candidateDAO.countActivePipelineCandidates(id);
        int processedCandidates = 0;

        if (activeCandidatesCount > 0) {
            if (candidateAction == null || candidateAction.trim().isEmpty()) {
                throw new IllegalStateException("Cảnh báo: Hiện còn " + activeCandidatesCount + " ứng viên đang trong pipeline tuyển dụng! Bạn cần chọn phương án xử lý ứng viên trước khi đóng.");
            }

            if ("REJECT_ALL".equalsIgnoreCase(candidateAction)) {
                String statusLabel = "FULFILLED".equals(normalizedStatus) ? "Đã tuyển đủ" : 
                                    ("PAUSED".equals(normalizedStatus) ? "Tạm dừng" : "Đã huỷ");
                String noteMsg = "[Đóng yêu cầu - " + statusLabel + "]: " + closeReason.trim();
                processedCandidates = candidateDAO.updatePipelineCandidates(id, "REJECTED", noteMsg);
            } else if ("HOLD_ALL".equalsIgnoreCase(candidateAction)) {
                String noteMsg = "[Lưu trữ Talent Pool - Đóng vị trí]: " + closeReason.trim();
                processedCandidates = candidateDAO.updatePipelineCandidates(id, "APPLIED", noteMsg);
            } else if ("CONFIRM_MANUAL".equalsIgnoreCase(candidateAction)) {
                processedCandidates = activeCandidatesCount;
            }
        }

        // Cập nhật trạng thái yêu cầu tuyển dụng
        boolean updated = requestDAO.closeRequisition(id, normalizedStatus, closeReason.trim(), 
                                                      closeNotes != null ? closeNotes.trim() : "", actorId);
        if (!updated) {
            throw new RuntimeException("Không thể cập nhật trạng thái đóng yêu cầu trong cơ sở dữ liệu!");
        }

        // Tự động gỡ các tin tuyển dụng đang đăng
        int unpublishedPostingsCount = jobPostingDAO.unpublishByRequisitionId(id);

        // Ghi Audit Log
        String statusText = "FULFILLED".equals(normalizedStatus) ? "Đã tuyển đủ" : 
                           ("PAUSED".equals(normalizedStatus) ? "Tạm dừng" : "Đã huỷ");
        String auditDesc = String.format("Đóng yêu cầu tuyển dụng [%s - %s] sang trạng thái: %s. Lý do: %s. Tự động gỡ %d tin tuyển dụng đang đăng. Đã xử lý %d ứng viên pipeline.",
                req.getCode(), req.getTitle(), statusText, closeReason.trim(), unpublishedPostingsCount, activeCandidatesCount);
        auditService.log(actorId, "REQUISITION_CLOSED", "REQUISITION", id, auditDesc, ip, userAgent);

        java.util.Map<String, Object> responseData = new java.util.HashMap<>();
        responseData.put("success", true);
        responseData.put("requisitionId", id);
        responseData.put("newStatus", normalizedStatus);
        responseData.put("statusLabel", statusText);
        responseData.put("unpublishedPostingsCount", unpublishedPostingsCount);
        responseData.put("activeCandidatesCount", activeCandidatesCount);
        responseData.put("processedCandidates", processedCandidates);
        responseData.put("message", "Đã đóng yêu cầu thành công sang trạng thái \"" + statusText + "\", tự động gỡ " + unpublishedPostingsCount + " tin tuyển dụng.");

        return responseData;
    }
}

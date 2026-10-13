package com.irms.service;

import com.irms.dao.BaseDAO;
import com.irms.dao.RecruitmentApprovalDAO;
import com.irms.dao.RecruitmentRequestDAO;
import com.irms.model.RecruitmentApprovalLog;
import com.irms.model.RecruitmentRequest;
import com.irms.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service điều phối chuỗi phê duyệt đa cấp và nhật ký hồ sơ tuyển dụng
 * Đáp ứng toàn bộ các subtasks:
 * - KN-154: [BE] API ghi nhận kết quả phê duyệt theo từng cấp
 * - KN-155: [BE] Khóa lịch sử phê duyệt không cho sửa hoặc xoá
 * - KN-156: [BE] Thiết kế mô hình lưu nhật ký phê duyệt
 * - KN-157: [BE] Xác định người cần phê duyệt tiếp theo trong luồng nhiều cấp
 * - KN-158: [BE] API lấy chuỗi cấp duyệt và trạng thái hiện tại của hồ sơ
 */
public class RecruitmentApprovalService {
    private static final Logger logger = Logger.getLogger(RecruitmentApprovalService.class.getName());

    private final RecruitmentApprovalDAO approvalDAO = new RecruitmentApprovalDAO();
    private final RecruitmentRequestDAO requisitionDAO = new RecruitmentRequestDAO();
    private final AuditService auditService = new AuditService();

    /**
     * DTO lưu trữ thông tin Người cần phê duyệt tiếp theo (KN-157)
     */
    public static class NextApproverInfo {
        private boolean hasNextStep;
        private int stepOrder;
        private String stepName;
        private String requiredRole;
        private String requiredRoleName;
        private String waitingForText;
        private List<Map<String, String>> eligibleApprovers = new ArrayList<>();
        private boolean isFullyApproved;
        private boolean isRejected;

        public boolean isHasNextStep() { return hasNextStep; }
        public void setHasNextStep(boolean hasNextStep) { this.hasNextStep = hasNextStep; }

        public int getStepOrder() { return stepOrder; }
        public void setStepOrder(int stepOrder) { this.stepOrder = stepOrder; }

        public String getStepName() { return stepName; }
        public void setStepName(String stepName) { this.stepName = stepName; }

        public String getRequiredRole() { return requiredRole; }
        public void setRequiredRole(String requiredRole) { this.requiredRole = requiredRole; }

        public String getRequiredRoleName() { return requiredRoleName; }
        public void setRequiredRoleName(String requiredRoleName) { this.requiredRoleName = requiredRoleName; }

        public String getWaitingForText() { return waitingForText; }
        public void setWaitingForText(String waitingForText) { this.waitingForText = waitingForText; }

        public List<Map<String, String>> getEligibleApprovers() { return eligibleApprovers; }
        public void setEligibleApprovers(List<Map<String, String>> eligibleApprovers) { this.eligibleApprovers = eligibleApprovers; }

        public boolean isFullyApproved() { return isFullyApproved; }
        public void setFullyApproved(boolean fullyApproved) { isFullyApproved = fullyApproved; }

        public boolean isRejected() { return isRejected; }
        public void setRejected(boolean rejected) { isRejected = rejected; }
    }

    /**
     * Khởi tạo chuỗi duyệt nếu hồ sơ chưa có
     */
    public void ensureApprovalChain(RecruitmentRequest req) {
        if (req == null || "DRAFT".equalsIgnoreCase(req.getStatus())) {
            return;
        }
        if (approvalDAO.countSteps(req.getId()) == 0) {
            approvalDAO.initApprovalChain(req.getId(), req.getHiringManagerId());
        }
    }

    /**
     * Lấy toàn bộ danh sách các bước phê duyệt của hồ sơ (KN-158)
     */
    public List<RecruitmentApprovalLog> getApprovalChain(String requisitionId) {
        RecruitmentRequest req = requisitionDAO.findById(requisitionId);
        if (req != null && !"DRAFT".equalsIgnoreCase(req.getStatus())) {
            ensureApprovalChain(req);
        }
        return approvalDAO.findByRequisitionId(requisitionId);
    }

    /**
     * KN-157: Xác định người cần phê duyệt tiếp theo trong luồng nhiều cấp
     */
    public NextApproverInfo getNextApproverInfo(String requisitionId) {
        NextApproverInfo info = new NextApproverInfo();
        RecruitmentRequest req = requisitionDAO.findById(requisitionId);
        if (req == null) {
            info.setWaitingForText("Hồ sơ không tồn tại");
            return info;
        }

        if ("DRAFT".equalsIgnoreCase(req.getStatus())) {
            info.setWaitingForText("Hồ sơ đang ở trạng thái Bản nháp (chưa gửi phê duyệt)");
            return info;
        }

        if ("REJECTED".equalsIgnoreCase(req.getStatus())) {
            info.setRejected(true);
            info.setWaitingForText("Hồ sơ đã bị từ chối phê duyệt");
            return info;
        }

        if ("OPEN".equalsIgnoreCase(req.getStatus()) || "CLOSED".equalsIgnoreCase(req.getStatus())) {
            info.setFullyApproved(true);
            info.setWaitingForText("Hồ sơ đã hoàn tất phê duyệt qua tất cả các cấp");
            return info;
        }

        // Lấy bước hiện tại đang PENDING
        RecruitmentApprovalLog pendingStep = approvalDAO.getCurrentPendingStep(requisitionId);
        if (pendingStep == null) {
            // Kiểm tra xem có bước nào REJECTED không
            List<RecruitmentApprovalLog> steps = approvalDAO.findByRequisitionId(requisitionId);
            boolean rejected = false;
            for (RecruitmentApprovalLog s : steps) {
                if (s.isRejected()) {
                    rejected = true;
                    break;
                }
            }
            if (rejected) {
                info.setRejected(true);
                info.setWaitingForText("Hồ sơ đã bị từ chối ở một cấp trước đó");
            } else {
                info.setFullyApproved(true);
                info.setWaitingForText("Hồ sơ đã hoàn tất phê duyệt");
            }
            return info;
        }

        info.setHasNextStep(true);
        info.setStepOrder(pendingStep.getStepOrder());
        info.setStepName(pendingStep.getStepName());
        info.setRequiredRole(pendingStep.getRequiredRole());
        info.setRequiredRoleName(pendingStep.getRequiredRoleName());

        // Tìm danh sách người dùng có thể duyệt bước này
        List<Map<String, String>> eligible = findEligibleApprovers(pendingStep, req);
        info.setEligibleApprovers(eligible);

        StringBuilder waitingText = new StringBuilder();
        if (eligible.isEmpty()) {
            waitingText.append("Đang chờ: Người phụ trách vai trò ").append(pendingStep.getRequiredRoleName());
        } else {
            waitingText.append("Đang chờ: ");
            List<String> names = new ArrayList<>();
            for (Map<String, String> u : eligible) {
                String title = u.get("jobTitle") != null && !u.get("jobTitle").isEmpty() ? " (" + u.get("jobTitle") + ")" : "";
                names.add(u.get("fullName") + title);
            }
            waitingText.append(String.join(", ", names));
        }
        info.setWaitingForText(waitingText.toString());

        return info;
    }

    /**
     * Tìm danh sách nhân sự đủ điều kiện duyệt cấp hiện tại
     */
    public List<Map<String, String>> findEligibleApprovers(RecruitmentApprovalLog step, RecruitmentRequest req) {
        String hiringMgrId = (req != null) ? req.getHiringManagerId() : null;
        return approvalDAO.findEligibleApprovers(step.getRequiredRole(), step.getAssignedUserId(), hiringMgrId, step.getStepOrder());
    }

    /**
     * Kiểm tra xem người dùng hiện tại có thẩm quyền phê duyệt bước chờ duyệt hay không
     */
    public boolean canCurrentUserApprove(User user, String requisitionId) {
        if (user == null || requisitionId == null) return false;
        if (user.hasRole("ADMIN")) return true;

        RecruitmentApprovalLog pendingStep = approvalDAO.getCurrentPendingStep(requisitionId);
        if (pendingStep == null) return false;

        // Nếu bước chỉ định đúng user này
        if (user.getId().equals(pendingStep.getAssignedUserId())) {
            return true;
        }

        // Nếu user có vai trò khớp với vai trò yêu cầu
        if (user.hasRole(pendingStep.getRequiredRole())) {
            return true;
        }

        // Hoặc có quyền phê duyệt chung
        if (user.hasPermission("requisitions.approve") && user.hasRole(pendingStep.getRequiredRole())) {
            return true;
        }

        return false;
    }

    /**
     * KN-154 & KN-155: API ghi nhận kết quả phê duyệt theo từng cấp
     * Xử lý phê duyệt (APPROVE) hoặc từ chối (REJECT)
     */
    public synchronized boolean processApprovalDecision(String requisitionId, User actor, String action, String comment, String ip, String userAgent) {
        if (actor == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để thực hiện phê duyệt!");
        }

        RecruitmentRequest req = requisitionDAO.findById(requisitionId);
        if (req == null) {
            throw new IllegalArgumentException("Không tìm thấy yêu cầu tuyển dụng ID: " + requisitionId);
        }

        if (!"PENDING_APPROVAL".equalsIgnoreCase(req.getStatus())) {
            throw new IllegalStateException("Yêu cầu tuyển dụng hiện không ở trạng thái Chờ phê duyệt (Trạng thái hiện tại: " + req.getStatusLabel() + ")");
        }

        RecruitmentApprovalLog pendingStep = approvalDAO.getCurrentPendingStep(requisitionId);
        if (pendingStep == null) {
            throw new IllegalStateException("Không tìm thấy cấp duyệt nào đang chờ phê duyệt cho hồ sơ này!");
        }

        // Kiểm tra quyền hạn của người thực hiện
        if (!canCurrentUserApprove(actor, requisitionId)) {
            throw new SecurityException("Bạn không có quyền phê duyệt ở cấp hiện tại (" + pendingStep.getStepName() + ")!");
        }

        String normAction = (action != null) ? action.trim().toUpperCase() : "";
        if (!"APPROVE".equals(normAction) && !"REJECT".equals(normAction)) {
            throw new IllegalArgumentException("Hành động không hợp lệ: " + action + " (Chỉ chấp nhận APPROVE hoặc REJECT)");
        }

        // Nếu từ chối, bắt buộc nhập ý kiến lý do
        if ("REJECT".equals(normAction)) {
            if (comment == null || comment.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập lý do từ chối phê duyệt để phản hồi cho người lập phiếu!");
            }
        }

        String statusToRecord = "APPROVE".equals(normAction) ? "APPROVED" : "REJECTED";

        // Ghi nhận quyết định vào bước hiện tại (KN-154, KN-155: khóa lịch sử)
        boolean success = approvalDAO.recordApprovalDecision(pendingStep.getId(), actor.getId(), statusToRecord, comment);
        if (!success) {
            throw new RuntimeException("Ghi nhận quyết định phê duyệt thất bại!");
        }

        if ("APPROVE".equals(normAction)) {
            // Thử kích hoạt bước tiếp theo
            boolean nextActivated = approvalDAO.activateNextStep(requisitionId, pendingStep.getStepOrder());
            if (nextActivated) {
                // Vẫn còn bước tiếp theo -> Hồ sơ vẫn giữ PENDING_APPROVAL
                auditService.log(actor.getId(), "REQUISITION_STEP_APPROVED", "REQUISITION", requisitionId,
                        "Đã phê duyệt " + pendingStep.getStepName() + " của yêu cầu: " + req.getCode() + " - Ý kiến: " + (comment != null ? comment : "Đồng ý"),
                        ip, userAgent);
            } else {
                // Không còn bước tiếp theo -> ĐÃ HOÀN TẤT DUYỆT CẢ 3 CẤP!
                // Chuyển Requisition sang OPEN (Đang tuyển)
                req.setStatus("OPEN");
                requisitionDAO.update(req);

                auditService.log(actor.getId(), "REQUISITION_FULLY_APPROVED", "REQUISITION", requisitionId,
                        "Hoàn tất toàn bộ chuỗi phê duyệt yêu cầu tuyển dụng: " + req.getCode() + " -> Trạng thái chuyển thành ĐANG TUYỂN",
                        ip, userAgent);
            }
        } else {
            // TỪ CHỐI (REJECT) -> Bỏ qua toàn bộ các cấp sau
            approvalDAO.skipSubsequentSteps(requisitionId, pendingStep.getStepOrder());
            // Cập nhật Requisition sang REJECTED
            req.setStatus("REJECTED");
            requisitionDAO.update(req);

            auditService.log(actor.getId(), "REQUISITION_REJECTED", "REQUISITION", requisitionId,
                    "Từ chối tại " + pendingStep.getStepName() + " của yêu cầu: " + req.getCode() + " - Lý do: " + comment,
                    ip, userAgent);
        }

        return true;
    }

    /**
     * Lấy toàn bộ trạng thái chuỗi duyệt dưới dạng Map chuẩn bị cho JSON API (KN-158)
     */
    public Map<String, Object> getApprovalWorkflowState(String requisitionId, User currentUser) {
        Map<String, Object> state = new HashMap<>();
        RecruitmentRequest req = requisitionDAO.findById(requisitionId);
        if (req == null) {
            state.put("success", false);
            state.put("message", "Không tìm thấy hồ sơ yêu cầu tuyển dụng!");
            return state;
        }

        ensureApprovalChain(req);
        List<RecruitmentApprovalLog> steps = approvalDAO.findByRequisitionId(requisitionId);
        NextApproverInfo nextInfo = getNextApproverInfo(requisitionId);
        boolean canApprove = canCurrentUserApprove(currentUser, requisitionId);

        state.put("success", true);
        state.put("requisitionId", req.getId());
        state.put("code", req.getCode());
        state.put("title", req.getTitle());
        state.put("positionTitle", req.getPositionTitle());
        state.put("departmentName", req.getDepartmentName() != null ? req.getDepartmentName() : "Chưa gán");
        state.put("status", req.getStatus());
        state.put("statusLabel", req.getStatusLabel());
        state.put("statusBadgeClass", req.getStatusBadgeClass());
        state.put("isDraft", "DRAFT".equalsIgnoreCase(req.getStatus()));
        state.put("isPendingApproval", "PENDING_APPROVAL".equalsIgnoreCase(req.getStatus()));
        state.put("isOpen", "OPEN".equalsIgnoreCase(req.getStatus()));
        state.put("isRejected", "REJECTED".equalsIgnoreCase(req.getStatus()) || nextInfo.isRejected());
        state.put("isFullyApproved", nextInfo.isFullyApproved());
        state.put("currentStepOrder", nextInfo.getStepOrder());
        state.put("currentStepName", nextInfo.getStepName());
        state.put("waitingForText", nextInfo.getWaitingForText());
        state.put("canCurrentUserApprove", canApprove);

        List<Map<String, Object>> stepList = new ArrayList<>();
        for (RecruitmentApprovalLog s : steps) {
            Map<String, Object> stepMap = new HashMap<>();
            stepMap.put("id", s.getId());
            stepMap.put("stepOrder", s.getStepOrder());
            stepMap.put("stepName", s.getStepName());
            stepMap.put("requiredRole", s.getRequiredRole());
            stepMap.put("requiredRoleName", s.getRequiredRoleName());
            stepMap.put("status", s.getStatus());
            stepMap.put("statusLabel", s.getStatusLabel());
            stepMap.put("statusBadgeClass", s.getStatusBadgeClass());
            stepMap.put("statusIcon", s.getStatusIcon());
            stepMap.put("approverId", s.getApproverId());
            stepMap.put("approverName", s.getApproverName());
            stepMap.put("approverJobTitle", s.getApproverJobTitle());
            stepMap.put("approverEmail", s.getApproverEmail());
            stepMap.put("decisionDate", s.getDecisionDate() != null ? s.getFormattedDecisionDate() : null);
            stepMap.put("comment", s.getComment());
            stepMap.put("isLocked", s.isLocked()); // KN-155, KN-160: Đã khóa chỉ đọc
            stepMap.put("isApproved", s.isApproved());
            stepMap.put("isRejected", s.isRejected());
            stepMap.put("isPending", s.isPending());
            stepMap.put("isWaiting", s.isWaiting());
            stepMap.put("isSkipped", s.isSkipped());
            stepList.add(stepMap);
        }
        state.put("steps", stepList);

        return state;
    }
}

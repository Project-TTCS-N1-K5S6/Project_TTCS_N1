package com.irms.service;

import com.irms.dao.RecruiterAssignmentDAO;
import com.irms.dao.RecruitmentRequestDAO;
import com.irms.dao.UserDAO;
import com.irms.model.RecruiterAssignment;
import com.irms.model.RecruiterAssignmentHistory;
import com.irms.model.RecruitmentRequest;
import com.irms.model.User;
import com.irms.util.SecurityUtil;

import java.util.List;

/**
 * ==============================================================================
 * DỊCH VỤ NGHIỆP VỤ PHÂN BỔ HỒ SƠ TUYỂN DỤNG (KN-116)
 * ==============================================================================
 * Yêu cầu:
 * 1. Phân công một recruiter chính và nhiều recruiter hỗ trợ
 * 2. Recruiter chỉ nhìn thấy ứng viên của vị trí được giao
 * 3. Ghi lịch sử chuyển giao khi đổi người phụ trách chính
 * ==============================================================================
 */
public class RecruiterAssignmentService {

    private final RecruiterAssignmentDAO assignmentDAO = new RecruiterAssignmentDAO();
    private final RecruitmentRequestDAO requestDAO = new RecruitmentRequestDAO();
    private final UserDAO userDAO = new UserDAO();
    private final AuditService auditService = new AuditService();

    /**
     * Lấy danh sách recruiter (chính + hỗ trợ) đang phụ trách một vị trí
     */
    public List<RecruiterAssignment> getAssignmentsByRequisition(String requisitionId) {
        return assignmentDAO.findByRequisitionId(requisitionId);
    }

    /**
     * Lấy lịch sử chuyển giao recruiter chính của một vị trí
     */
    public List<RecruiterAssignmentHistory> getHistoryByRequisition(String requisitionId) {
        return assignmentDAO.findHistoryByRequisitionId(requisitionId);
    }

    /**
     * Lấy danh sách tất cả requisition mà một recruiter được giao
     * (dùng để kiểm soát quyền nhìn ứng viên - yêu cầu #2)
     */
    public List<String> getAssignedRequisitionIds(String recruiterId) {
        return assignmentDAO.findRequisitionIdsByRecruiterId(recruiterId);
    }

    /**
     * Kiểm tra recruiter có quyền xem ứng viên của một vị trí không
     * ADMIN và HR_MANAGER luôn có quyền.
     */
    public boolean canRecruiterViewCandidates(User recruiter, String requisitionId) {
        if (recruiter == null) return false;
        if (recruiter.hasRole("ADMIN") || recruiter.hasRole("HR_MANAGER")) return true;
        return assignmentDAO.isRecruiterAssigned(requisitionId, recruiter.getId());
    }

    /**
     * Phân công recruiter chính mới cho vị trí tuyển dụng.
     * Ghi lịch sử chuyển giao nếu recruiter thay đổi.
     *
     * @param requisitionId     ID vị trí tuyển dụng
     * @param newRecruiterId    ID recruiter chính mới
     * @param changeReason      Lý do thay đổi
     * @param actorId           ID người thực hiện
     * @param ip                IP người thực hiện
     * @param userAgent         User-Agent
     */
    public boolean assignPrimaryRecruiter(String requisitionId, String newRecruiterId,
                                          String changeReason, String actorId,
                                          String ip, String userAgent) {
        RecruitmentRequest req = requestDAO.findById(requisitionId);
        if (req == null) {
            throw new IllegalArgumentException("Không tìm thấy vị trí tuyển dụng: " + requisitionId);
        }

        String oldRecruiterId = req.getRecruiterId();
        String oldRecruiterName = req.getRecruiterName();

        // Không cần thay đổi nếu người cũ == người mới
        if (newRecruiterId != null && newRecruiterId.equals(oldRecruiterId)) {
            return true;
        }

        // Cập nhật recruiter chính trên bảng recruitment_requisitions
        req.setRecruiterId(newRecruiterId);

        // Lấy tên recruiter mới
        String newRecruiterName = null;
        if (newRecruiterId != null) {
            User newRec = userDAO.findById(newRecruiterId);
            if (newRec != null) newRecruiterName = newRec.getFullName();
        }

        boolean success = requestDAO.update(req);
        if (!success) return false;

        // Ghi lịch sử chuyển giao
        RecruiterAssignmentHistory history = new RecruiterAssignmentHistory();
        history.setId(SecurityUtil.generateUUID());
        history.setRequisitionId(requisitionId);
        history.setRequisitionCode(req.getCode());
        history.setOldRecruiterId(oldRecruiterId);
        history.setOldRecruiterName(oldRecruiterName);
        history.setNewRecruiterId(newRecruiterId);
        history.setNewRecruiterName(newRecruiterName);
        history.setChangeReason(changeReason);
        history.setChangedBy(actorId);

        // Lấy tên người thực hiện
        if (actorId != null) {
            User actor = userDAO.findById(actorId);
            if (actor != null) history.setChangedByName(actor.getFullName());
        }

        assignmentDAO.insertHistory(history);

        // Ghi audit log
        String desc = String.format("Chuyển giao recruiter chính vị trí [%s]: %s → %s. Lý do: %s",
                req.getCode(),
                oldRecruiterName != null ? oldRecruiterName : "(chưa có)",
                newRecruiterName != null ? newRecruiterName : "(chưa có)",
                changeReason != null ? changeReason : "—");
        auditService.log(actorId, "RECRUITER_PRIMARY_CHANGED", "REQUISITION", requisitionId, desc, ip, userAgent);

        return true;
    }

    /**
     * Cập nhật danh sách recruiter hỗ trợ của một vị trí.
     * Xóa toàn bộ danh sách cũ và thêm danh sách mới.
     *
     * @param requisitionId      ID vị trí tuyển dụng
     * @param supportRecruiterIds Danh sách ID recruiter hỗ trợ mới
     * @param actorId            ID người thực hiện
     * @param ip                 IP người thực hiện
     * @param userAgent          User-Agent
     */
    public boolean updateSupportRecruiters(String requisitionId, List<String> supportRecruiterIds,
                                           String actorId, String ip, String userAgent) {
        RecruitmentRequest req = requestDAO.findById(requisitionId);
        if (req == null) {
            throw new IllegalArgumentException("Không tìm thấy vị trí tuyển dụng: " + requisitionId);
        }

        // Xóa danh sách cũ
        assignmentDAO.clearSupportRecruiters(requisitionId);

        // Thêm danh sách mới
        if (supportRecruiterIds != null) {
            for (String rid : supportRecruiterIds) {
                if (rid != null && !rid.trim().isEmpty()) {
                    // Không cho thêm recruiter chính vào danh sách hỗ trợ
                    if (!rid.trim().equals(req.getRecruiterId())) {
                        assignmentDAO.addSupportRecruiter(requisitionId, rid.trim(), actorId);
                    }
                }
            }
        }

        auditService.log(actorId, "RECRUITER_SUPPORT_UPDATED", "REQUISITION", requisitionId,
                "Cập nhật danh sách recruiter hỗ trợ vị trí [" + req.getCode() + "]", ip, userAgent);

        return true;
    }

    /**
     * Phân công đồng thời recruiter chính và hỗ trợ (một lần gọi)
     */
    public boolean assignRecruiters(String requisitionId, String primaryRecruiterId,
                                    List<String> supportRecruiterIds, String changeReason,
                                    String actorId, String ip, String userAgent) {
        // Phân công recruiter chính (có ghi lịch sử nếu thay đổi)
        assignPrimaryRecruiter(requisitionId, primaryRecruiterId, changeReason, actorId, ip, userAgent);

        // Cập nhật danh sách hỗ trợ
        updateSupportRecruiters(requisitionId, supportRecruiterIds, actorId, ip, userAgent);

        return true;
    }
}

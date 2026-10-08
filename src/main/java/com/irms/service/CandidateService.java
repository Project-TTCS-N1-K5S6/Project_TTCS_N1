package com.irms.service;

import com.irms.dao.CandidateDAO;
import com.irms.dao.SharedCatalogDAO;
import com.irms.model.Candidate;
import com.irms.model.SharedCatalogType;
import com.irms.util.SecurityUtil;

import java.util.Arrays;
import java.util.List;

/**
 * Xử lý nghiệp vụ Hồ sơ Ứng viên & Pipeline tuyển dụng
 */
public class CandidateService {
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final SharedCatalogDAO catalogDAO = new SharedCatalogDAO();
    private static final List<String> CANDIDATE_STATUSES =
            Arrays.asList("APPLIED", "SCREENING", "INTERVIEWING", "OFFER", "HIRED", "REJECTED");

    public List<Candidate> getCandidates(String search, String status) {
        return candidateDAO.findAll(search, status, null);
    }

    /**
     * [US 5]: Lấy danh sách ứng viên có kiểm tra quyền hạn của Recruiter
     */
    public List<Candidate> getCandidates(String search, String status, String recruiterId) {
        return candidateDAO.findAll(search, status, recruiterId);
    }

    public boolean createCandidate(Candidate c) {
        if (!catalogDAO.isValueInType(c.getApplicationSourceId(), SharedCatalogType.APPLICATION_SOURCE)) {
            throw new IllegalArgumentException("Nguồn ứng viên không hợp lệ.");
        }
        c.setStatus(normalizeStatus(c.getStatus()));
        if ("REJECTED".equals(c.getStatus())) {
            validateRejectionReason(c.getRejectionReasonId());
        } else {
            c.setRejectionReasonId(null);
        }
        if (c.getRejectionReasonId() != null
                && !catalogDAO.isValueInType(c.getRejectionReasonId(), SharedCatalogType.REJECTION_REASON)) {
            throw new IllegalArgumentException("Lý do loại hồ sơ không hợp lệ.");
        }
        c.setId(SecurityUtil.generateUUID());
        return candidateDAO.insert(c);
    }

    public boolean updateStatus(String candidateId, String newStatus) {
        return updateStatus(candidateId, newStatus, null);
    }

    public boolean updateStatus(String candidateId, String newStatus, String rejectionReasonId) {
        if (candidateId == null || candidateId.trim().isEmpty()) {
            throw new IllegalArgumentException("Thiếu mã hồ sơ ứng viên.");
        }
        String normalizedStatus = normalizeStatus(newStatus);
        if ("REJECTED".equals(normalizedStatus)) {
            validateRejectionReason(rejectionReasonId);
        } else {
            rejectionReasonId = null;
        }
        return candidateDAO.updateStatus(candidateId.trim(), normalizedStatus, rejectionReasonId);
    }

    public int getTotalCandidates() {
        return candidateDAO.countTotal();
    }

    private String normalizeStatus(String status) {
        String normalized = status == null || status.trim().isEmpty() ? "APPLIED" : status.trim();
        if (!CANDIDATE_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Trạng thái hồ sơ không hợp lệ.");
        }
        return normalized;
    }

    private void validateRejectionReason(String rejectionReasonId) {
        if (rejectionReasonId == null || rejectionReasonId.trim().isEmpty()
                || !catalogDAO.isValueInType(rejectionReasonId, SharedCatalogType.REJECTION_REASON)) {
            throw new IllegalArgumentException("Hãy chọn lý do loại hồ sơ hợp lệ.");
        }
    }
}

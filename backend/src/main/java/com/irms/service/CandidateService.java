package com.irms.service;

import com.irms.dao.CandidateDAO;
import com.irms.model.Candidate;
import com.irms.util.SecurityUtil;

import java.util.List;

/**
 * Xử lý nghiệp vụ Hồ sơ Ứng viên & Pipeline tuyển dụng
 */
public class CandidateService {
    private final CandidateDAO candidateDAO = new CandidateDAO();

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
        c.setId(SecurityUtil.generateUUID());
        return candidateDAO.insert(c);
    }

    public boolean updateStatus(String candidateId, String newStatus) {
        return candidateDAO.updateStatus(candidateId, newStatus);
    }

    public int getTotalCandidates() {
        return candidateDAO.countTotal();
    }
}

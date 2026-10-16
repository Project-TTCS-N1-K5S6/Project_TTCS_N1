package com.irms.service;

import com.irms.dao.CandidateDAO;
import com.irms.dao.CandidateScreeningDAO;
import com.irms.model.Candidate;
import com.irms.model.CandidateTagCategory;
import com.irms.util.SecurityUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Xử lý nghiệp vụ Hồ sơ Ứng viên & Pipeline tuyển dụng
 */
public class CandidateService {
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final CandidateScreeningDAO screeningDAO = new CandidateScreeningDAO();

    public List<Candidate> getCandidates(String search, String status) {
        return candidateDAO.findAll(search, status, null);
    }

    /**
     * [US 5]: Lấy danh sách ứng viên có kiểm tra quyền hạn của Recruiter
     */
    public List<Candidate> getCandidates(String search, String status, String recruiterId) {
        return candidateDAO.findAll(search, status, recruiterId);
    }

    public List<Candidate> getCandidates(String search, String status, String recruiterId, String reviewerId) {
        return getCandidates(search, status, recruiterId, reviewerId, false);
    }

    public List<Candidate> getCandidates(String search, String status, String recruiterId,
                                         String reviewerId, boolean includeInternalNotes) {
        List<Candidate> candidates = candidateDAO.findAll(search, status, recruiterId);
        screeningDAO.attachScreeningDetails(candidates, reviewerId, includeInternalNotes);
        return candidates;
    }

    public List<CandidateTagCategory> getTagCatalog() {
        return screeningDAO.findTagCatalog();
    }

    public boolean canAccessCandidate(String candidateId, String recruiterId) {
        return candidateId != null && !candidateId.trim().isEmpty()
                && candidateDAO.existsForRecruiter(candidateId.trim(), recruiterId);
    }

    public void updateCandidateTags(String candidateId, String[] submittedTagIds, String assignedBy) {
        List<String> tagIds = normalizeTagIds(submittedTagIds);
        if (!screeningDAO.replaceCandidateTags(candidateId, tagIds, assignedBy)) {
            throw new IllegalArgumentException("Danh sách nhãn không hợp lệ hoặc không còn hoạt động.");
        }
    }

    public void saveQuickScreening(String candidateId, String reviewerId, String reviewerName,
                                   Integer rating, String internalNote) {
        validateRating(rating);
        String normalizedNote = normalizeInternalNote(internalNote);
        if (rating == null && normalizedNote == null) {
            throw new IllegalArgumentException("Hãy chọn số sao hoặc nhập ghi chú nội bộ.");
        }
        screeningDAO.saveQuickScreening(candidateId, reviewerId, reviewerName, rating, normalizedNote);
    }

    public void createTagCategory(String name) {
        String normalizedName = normalizeCatalogName(name, "danh mục");
        screeningDAO.createTagCategory(SecurityUtil.generateUUID(), normalizedName);
    }

    public void createTag(String categoryId, String name) {
        String normalizedCategoryId = categoryId == null ? "" : categoryId.trim();
        if (normalizedCategoryId.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn danh mục nhãn.");
        }
        String normalizedName = normalizeCatalogName(name, "nhãn");
        screeningDAO.createTag(SecurityUtil.generateUUID(), normalizedCategoryId, normalizedName);
    }

    static void validateRating(Integer rating) {
        if (rating != null && (rating < 1 || rating > 5)) {
            throw new IllegalArgumentException("Điểm đánh giá phải nằm trong khoảng từ 1 đến 5 sao.");
        }
    }

    static String normalizeInternalNote(String note) {
        if (note == null || note.trim().isEmpty()) return null;
        String normalized = note.trim();
        if (normalized.length() > 5000) {
            throw new IllegalArgumentException("Ghi chú nội bộ không được vượt quá 5.000 ký tự.");
        }
        return normalized;
    }

    private List<String> normalizeTagIds(String[] submittedTagIds) {
        Set<String> uniqueIds = new LinkedHashSet<>();
        if (submittedTagIds != null) {
            for (String tagId : submittedTagIds) {
                if (tagId == null || tagId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Danh sách nhãn không hợp lệ.");
                }
                uniqueIds.add(tagId.trim());
            }
        }
        return new ArrayList<>(uniqueIds);
    }

    private String normalizeCatalogName(String name, String type) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty() || normalized.length() > 100) {
            throw new IllegalArgumentException("Tên " + type + " phải có từ 1 đến 100 ký tự.");
        }
        return normalized;
    }

    public boolean createCandidate(Candidate c) {
        c.setId(SecurityUtil.generateUUID());
        return candidateDAO.insert(c);
    }

    public boolean updateStatus(String candidateId, String newStatus) {
        if (candidateId == null || candidateId.trim().isEmpty()
                || !Arrays.asList("APPLIED", "SCREENING", "INTERVIEWING", "OFFER", "HIRED", "REJECTED")
                .contains(newStatus)) {
            throw new IllegalArgumentException("Trạng thái hồ sơ ứng viên không hợp lệ.");
        }
        return candidateDAO.updateStatus(candidateId, newStatus);
    }

    public int getTotalCandidates() {
        return candidateDAO.countTotal();
    }
}

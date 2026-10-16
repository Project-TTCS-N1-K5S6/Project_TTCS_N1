package com.irms.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * DTO dữ liệu đối chiếu & so sánh chi tiết giữa hai hồ sơ ứng viên trước khi hợp nhất
 */
public class CandidateComparisonDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Candidate candidate1;
    private Candidate candidate2;
    private CandidateDuplicateWarning warning;
    private List<CandidateActivity> activities1 = new ArrayList<>();
    private List<CandidateActivity> activities2 = new ArrayList<>();

    public CandidateComparisonDTO() {}

    public CandidateComparisonDTO(Candidate candidate1, Candidate candidate2, CandidateDuplicateWarning warning) {
        this.candidate1 = candidate1;
        this.candidate2 = candidate2;
        this.warning = warning;
    }

    public Candidate getCandidate1() { return candidate1; }
    public void setCandidate1(Candidate candidate1) { this.candidate1 = candidate1; }

    public Candidate getCandidate2() { return candidate2; }
    public void setCandidate2(Candidate candidate2) { this.candidate2 = candidate2; }

    public CandidateDuplicateWarning getWarning() { return warning; }
    public void setWarning(CandidateDuplicateWarning warning) { this.warning = warning; }

    public List<CandidateActivity> getActivities1() { return activities1; }
    public void setActivities1(List<CandidateActivity> activities1) { this.activities1 = activities1; }

    public List<CandidateActivity> getActivities2() { return activities2; }
    public void setActivities2(List<CandidateActivity> activities2) { this.activities2 = activities2; }

    // Các hàm đối chiếu để highlight giao diện khác biệt
    public boolean isNameEqual() {
        if (candidate1 == null || candidate2 == null) return false;
        return Objects.equals(normalize(candidate1.getFullName()), normalize(candidate2.getFullName()));
    }

    public boolean isEmailEqual() {
        if (candidate1 == null || candidate2 == null) return false;
        return Objects.equals(normalize(candidate1.getEmail()), normalize(candidate2.getEmail()));
    }

    public boolean isPhoneEqual() {
        if (candidate1 == null || candidate2 == null) return false;
        String p1 = candidate1.getNormalizedPhone() != null ? candidate1.getNormalizedPhone() : normalizePhone(candidate1.getPhone());
        String p2 = candidate2.getNormalizedPhone() != null ? candidate2.getNormalizedPhone() : normalizePhone(candidate2.getPhone());
        return p1 != null && !p1.isEmpty() && p1.equals(p2);
    }

    public boolean isStatusEqual() {
        if (candidate1 == null || candidate2 == null) return false;
        return Objects.equals(candidate1.getStatus(), candidate2.getStatus());
    }

    public boolean isRequisitionEqual() {
        if (candidate1 == null || candidate2 == null) return false;
        return Objects.equals(candidate1.getRequisitionId(), candidate2.getRequisitionId());
    }

    public boolean isSourceEqual() {
        if (candidate1 == null || candidate2 == null) return false;
        return Objects.equals(candidate1.getApplicationSourceId(), candidate2.getApplicationSourceId());
    }

    public boolean isCvEqual() {
        if (candidate1 == null || candidate2 == null) return false;
        return Objects.equals(normalize(candidate1.getCvUrl()), normalize(candidate2.getCvUrl()));
    }

    public boolean isNotesEqual() {
        if (candidate1 == null || candidate2 == null) return false;
        return Objects.equals(normalize(candidate1.getNotes()), normalize(candidate2.getNotes()));
    }

    public boolean isTagsEqual() {
        if (candidate1 == null || candidate2 == null) return false;
        return Objects.equals(normalize(candidate1.getTags()), normalize(candidate2.getTags()));
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private String normalizePhone(String p) {
        if (p == null) return "";
        return p.replaceAll("[^0-9]", "");
    }
}

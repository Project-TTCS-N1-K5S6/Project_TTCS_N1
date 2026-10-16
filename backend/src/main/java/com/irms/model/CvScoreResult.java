package com.irms.model;

import java.io.Serializable;

/**
 * Kết quả chấm điểm hồ sơ ứng viên theo tiêu chí sàng lọc.
 * Đây là DTO tổng hợp, không tương ứng trực tiếp với một bảng DB.
 *
 * Cấu trúc thang điểm 100:
 *   - Điểm Kinh nghiệm (max 40): mức độ kinh nghiệm đáp ứng yêu cầu
 *   - Điểm Kỹ năng (max 40):     mức bao phủ kỹ năng bắt buộc + cộng điểm
 *   - Điểm Bằng cấp (max 20):    bằng cấp đúng chuyên ngành + chứng chỉ bổ sung
 */
public class CvScoreResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String candidateId;

    // ===== Điểm thành phần =====
    /** Điểm kinh nghiệm (0 - weightExperience, mặc định max 40) */
    private int experienceScore;

    /** Điểm kỹ năng (0 - weightSkills, mặc định max 40) */
    private int skillScore;

    /** Điểm bằng cấp (0 - weightEducation, mặc định max 20) */
    private int educationScore;

    /** Tổng điểm phù hợp (0-100) */
    private int totalScore;

    // ===== Cờ tiêu chí bắt buộc =====
    /** true = đáp ứng yêu cầu kinh nghiệm */
    private boolean experiencePassed;
    /** true = đáp ứng yêu cầu bằng cấp */
    private boolean degreePassed;
    /** true = đáp ứng tất cả kỹ năng bắt buộc */
    private boolean mustSkillsPassed;

    /** PASS nếu cả 3 tiêu chí bắt buộc đều đáp ứng, FAIL nếu thiếu bất kỳ */
    private String mandatoryFlag; // "PASS" | "FAIL"

    // ===== Chi tiết phân tích =====
    private String experienceDetail;
    private String degreeDetail;
    private String skillDetail;
    private String recommendation; // "HIGH" | "MEDIUM" | "REVIEW"
    private String recommendationLabel;

    // ===== Thông tin ứng viên =====
    private String candidateName;
    private String requisitionTitle;

    public CvScoreResult() {}

    // ========== Getters & Setters ==========

    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String candidateId) { this.candidateId = candidateId; }

    public int getExperienceScore() { return experienceScore; }
    public void setExperienceScore(int experienceScore) { this.experienceScore = experienceScore; }

    public int getSkillScore() { return skillScore; }
    public void setSkillScore(int skillScore) { this.skillScore = skillScore; }

    public int getEducationScore() { return educationScore; }
    public void setEducationScore(int educationScore) { this.educationScore = educationScore; }

    public int getTotalScore() { return totalScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }

    public boolean isExperiencePassed() { return experiencePassed; }
    public void setExperiencePassed(boolean experiencePassed) { this.experiencePassed = experiencePassed; }

    public boolean isDegreePassed() { return degreePassed; }
    public void setDegreePassed(boolean degreePassed) { this.degreePassed = degreePassed; }

    public boolean isMustSkillsPassed() { return mustSkillsPassed; }
    public void setMustSkillsPassed(boolean mustSkillsPassed) { this.mustSkillsPassed = mustSkillsPassed; }

    public String getMandatoryFlag() { return mandatoryFlag; }
    public void setMandatoryFlag(String mandatoryFlag) { this.mandatoryFlag = mandatoryFlag; }

    public String getExperienceDetail() { return experienceDetail; }
    public void setExperienceDetail(String experienceDetail) { this.experienceDetail = experienceDetail; }

    public String getDegreeDetail() { return degreeDetail; }
    public void setDegreeDetail(String degreeDetail) { this.degreeDetail = degreeDetail; }

    public String getSkillDetail() { return skillDetail; }
    public void setSkillDetail(String skillDetail) { this.skillDetail = skillDetail; }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
        if ("HIGH".equals(recommendation)) this.recommendationLabel = "Ưu tiên cao";
        else if ("MEDIUM".equals(recommendation)) this.recommendationLabel = "Ưu tiên trung bình";
        else this.recommendationLabel = "Xem xét thêm";
    }

    public String getRecommendationLabel() { return recommendationLabel; }

    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }

    public String getRequisitionTitle() { return requisitionTitle; }
    public void setRequisitionTitle(String requisitionTitle) { this.requisitionTitle = requisitionTitle; }

    /**
     * Badge CSS class cho cờ tiêu chí bắt buộc
     */
    public String getMandatoryFlagBadgeClass() {
        if ("PASS".equals(mandatoryFlag)) return "badge-pass";
        if ("FAIL".equals(mandatoryFlag)) return "badge-fail";
        return "badge-pending";
    }

    /**
     * Label hiển thị cho cờ bắt buộc
     */
    public String getMandatoryFlagLabel() {
        if ("PASS".equals(mandatoryFlag)) return "ĐẠT";
        if ("FAIL".equals(mandatoryFlag)) return "KHÔNG ĐẠT";
        return "Chưa đánh giá";
    }

    /**
     * Badge CSS class cho mức độ khuyến nghị
     */
    public String getRecommendationBadgeClass() {
        if ("HIGH".equals(recommendation)) return "badge-priority-high";
        if ("MEDIUM".equals(recommendation)) return "badge-priority-medium";
        return "badge-priority-review";
    }

    /**
     * Màu badge điểm số theo dải điểm
     */
    public String getScoreBadgeClass() {
        if (totalScore >= 75) return "score-high";
        if (totalScore >= 50) return "score-medium";
        return "score-low";
    }
}

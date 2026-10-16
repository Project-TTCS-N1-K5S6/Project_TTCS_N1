package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Đại diện thực thể Tiêu chí Sàng lọc & Chấm điểm hồ sơ (bảng job_screening_criteria)
 * Phục vụ nghiệp vụ: Chấm điểm & Xếp hạng hồ sơ tự động
 *
 * Thang điểm 100:
 *  - Kinh nghiệm làm việc: tối đa 40 điểm (weightExperience)
 *  - Kỹ năng chuyên môn:   tối đa 40 điểm (weightSkills)
 *  - Bằng cấp & Chứng chỉ: tối đa 20 điểm (weightEducation)
 */
public class ScreeningCriteria implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String requisitionId;
    private String requisitionTitle;

    /** Số năm kinh nghiệm tối thiểu bắt buộc */
    private int minExperienceYears;

    /** Bằng cấp bắt buộc (VD: "Cử nhân", "Thạc sĩ", "Cao đẳng") */
    private String requiredDegree;

    /** Kỹ năng bắt buộc (Must-have), phân cách bằng dấu phẩy */
    private String mustHaveSkills;

    /** Kỹ năng ưu tiên/cộng điểm (Nice-to-have), phân cách bằng dấu phẩy */
    private String niceToHaveSkills;

    /** Trọng số điểm Kinh nghiệm (mặc định 40) */
    private int weightExperience = 40;

    /** Trọng số điểm Kỹ năng (mặc định 40) */
    private int weightSkills = 40;

    /** Trọng số điểm Bằng cấp (mặc định 20) */
    private int weightEducation = 20;

    private String createdBy;
    private String createdByName;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public ScreeningCriteria() {}

    // ========== Getters & Setters ==========

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequisitionId() { return requisitionId; }
    public void setRequisitionId(String requisitionId) { this.requisitionId = requisitionId; }

    public String getRequisitionTitle() { return requisitionTitle; }
    public void setRequisitionTitle(String requisitionTitle) { this.requisitionTitle = requisitionTitle; }

    public int getMinExperienceYears() { return minExperienceYears; }
    public void setMinExperienceYears(int minExperienceYears) { this.minExperienceYears = minExperienceYears; }

    public String getRequiredDegree() { return requiredDegree; }
    public void setRequiredDegree(String requiredDegree) { this.requiredDegree = requiredDegree; }

    public String getMustHaveSkills() { return mustHaveSkills; }
    public void setMustHaveSkills(String mustHaveSkills) { this.mustHaveSkills = mustHaveSkills; }

    public String getNiceToHaveSkills() { return niceToHaveSkills; }
    public void setNiceToHaveSkills(String niceToHaveSkills) { this.niceToHaveSkills = niceToHaveSkills; }

    public int getWeightExperience() { return weightExperience; }
    public void setWeightExperience(int weightExperience) { this.weightExperience = weightExperience; }

    public int getWeightSkills() { return weightSkills; }
    public void setWeightSkills(int weightSkills) { this.weightSkills = weightSkills; }

    public int getWeightEducation() { return weightEducation; }
    public void setWeightEducation(int weightEducation) { this.weightEducation = weightEducation; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    // ========== Tiện ích ==========

    /**
     * Trả về danh sách kỹ năng bắt buộc đã tách từ chuỗi phân cách phẩy
     */
    public List<String> getMustHaveSkillList() {
        return parseSkills(mustHaveSkills);
    }

    /**
     * Trả về danh sách kỹ năng ưu tiên đã tách từ chuỗi phân cách phẩy
     */
    public List<String> getNiceToHaveSkillList() {
        return parseSkills(niceToHaveSkills);
    }

    private List<String> parseSkills(String raw) {
        if (raw == null || raw.trim().isEmpty()) return new ArrayList<>();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra xem tiêu chí đã được cấu hình đầy đủ chưa
     */
    public boolean isConfigured() {
        return (requiredDegree != null && !requiredDegree.trim().isEmpty())
                || (mustHaveSkills != null && !mustHaveSkills.trim().isEmpty())
                || minExperienceYears > 0;
    }
}

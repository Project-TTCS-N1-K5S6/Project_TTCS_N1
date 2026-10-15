package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Thực thể liên kết giữa Chức danh (bảng salary_ranges) và Khung năng lực (bảng competency_frameworks)
 * (Bảng position_competency_frameworks)
 */
public class PositionCompetencyFramework implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String positionId;
    private String positionCode;
    private String positionTitle;
    private String level;
    private String departmentId;
    private String departmentName;

    private String frameworkId;
    private String frameworkCode;
    private String frameworkName;

    private Timestamp assignedAt;
    private String assignedBy;
    private String assignedByName;

    public PositionCompetencyFramework() {}

    public PositionCompetencyFramework(String id, String positionId, String frameworkId) {
        this.id = id;
        this.positionId = positionId;
        this.frameworkId = frameworkId;
    }

    public PositionCompetencyFramework(String positionId, String frameworkId, String positionCode, String positionTitle, String level, String departmentName) {
        this.positionId = positionId;
        this.frameworkId = frameworkId;
        this.positionCode = positionCode;
        this.positionTitle = positionTitle;
        this.level = level;
        this.departmentName = departmentName;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPositionId() { return positionId; }
    public void setPositionId(String positionId) { this.positionId = positionId; }

    public String getPositionCode() { return positionCode; }
    public void setPositionCode(String positionCode) { this.positionCode = positionCode; }

    public String getPositionTitle() { return positionTitle; }
    public void setPositionTitle(String positionTitle) { this.positionTitle = positionTitle; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getFrameworkId() { return frameworkId; }
    public void setFrameworkId(String frameworkId) { this.frameworkId = frameworkId; }

    public String getFrameworkCode() { return frameworkCode; }
    public void setFrameworkCode(String frameworkCode) { this.frameworkCode = frameworkCode; }

    public String getFrameworkName() { return frameworkName; }
    public void setFrameworkName(String frameworkName) { this.frameworkName = frameworkName; }

    public Timestamp getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Timestamp assignedAt) { this.assignedAt = assignedAt; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public String getAssignedByName() { return assignedByName; }
    public void setAssignedByName(String assignedByName) { this.assignedByName = assignedByName; }
}

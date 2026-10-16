package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Đại diện thực thể Cấu hình ngưỡng SLA theo từng giai đoạn tuyển dụng (KN-364)
 */
public class SlaConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String stageCode; // APPLIED, SCREENING, INTERVIEWING, OFFER
    private String stageName; // Mới ứng tuyển, Sàng lọc CV, Phỏng vấn, Đề nghị tuyển dụng
    private int slaDays; // Số ngày tối đa cho phép
    private int warningThresholdDays; // Cảnh báo cận hạn (số ngày trước khi quá hạn)
    private String actionRequired; // Mô tả hành động cần làm
    private boolean active = true; // Trạng thái kích hoạt
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String updatedBy;
    private String updatedByName;

    public SlaConfig() {}

    public SlaConfig(String id, String stageCode, String stageName, int slaDays, int warningThresholdDays, String actionRequired, boolean active) {
        this.id = id;
        this.stageCode = stageCode;
        this.stageName = stageName;
        this.slaDays = slaDays;
        this.warningThresholdDays = warningThresholdDays;
        this.actionRequired = actionRequired;
        this.active = active;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStageCode() { return stageCode; }
    public void setStageCode(String stageCode) { this.stageCode = stageCode; }

    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }

    public int getSlaDays() { return slaDays; }
    public void setSlaDays(int slaDays) { this.slaDays = slaDays; }

    public int getWarningThresholdDays() { return warningThresholdDays; }
    public void setWarningThresholdDays(int warningThresholdDays) { this.warningThresholdDays = warningThresholdDays; }

    public String getActionRequired() { return actionRequired; }
    public void setActionRequired(String actionRequired) { this.actionRequired = actionRequired; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public String getUpdatedByName() { return updatedByName; }
    public void setUpdatedByName(String updatedByName) { this.updatedByName = updatedByName; }

    public String getStageBadgeClass() {
        if ("APPLIED".equalsIgnoreCase(stageCode)) return "badge bg-primary";
        if ("SCREENING".equalsIgnoreCase(stageCode)) return "badge bg-info text-dark";
        if ("INTERVIEWING".equalsIgnoreCase(stageCode)) return "badge bg-warning text-dark";
        if ("OFFER".equalsIgnoreCase(stageCode)) return "badge bg-purple text-white";
        return "badge bg-secondary";
    }
}

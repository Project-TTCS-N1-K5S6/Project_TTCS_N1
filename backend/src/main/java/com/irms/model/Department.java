package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Đại diện thực thể Phòng ban / Đơn vị tổ chức (bảng departments)
 * Hỗ trợ cấu trúc cây phân cấp nhiều cấp (Tree Hierarchy) và Người phụ trách
 */
public class Department implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    private String id;
    private String code;
    private String name;
    private String description;

    // Cấu trúc cây nhiều cấp (Self-reference)
    private String parentId;
    private String parentCode;
    private String parentName;

    // Người phụ trách phòng ban
    private String managerId;
    private String managerName;
    private String managerEmail;
    private String managerJobTitle;
    private String managerAvatarUrl;

    // Trạng thái áp dụng
    private String status = STATUS_ACTIVE;

    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String createdBy;
    private String createdByName;
    private String updatedBy;

    // Dữ liệu mở rộng tính toán cây & thống kê nghiệp vụ
    private int userCount = 0;
    private int childrenCount = 0;
    private int openRequisitionCount = 0; // Số yêu cầu tuyển dụng đang mở
    private int totalRequisitionCount = 0; // Tổng số yêu cầu tuyển dụng
    private int level = 0; // Độ sâu của cây (0 = Root, 1 = Cấp 1, 2 = Cấp 2...)
    private List<Department> children = new ArrayList<>();

    public Department() {}

    public Department(String id, String code, String name, String description) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public Department(String id, String code, String name, String description, String parentId, String managerId, String status) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.parentId = parentId;
        this.managerId = managerId;
        this.status = status != null ? status : STATUS_ACTIVE;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public String getParentCode() { return parentCode; }
    public void setParentCode(String parentCode) { this.parentCode = parentCode; }

    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }

    public String getManagerId() { return managerId; }
    public void setManagerId(String managerId) { this.managerId = managerId; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public String getManagerEmail() { return managerEmail; }
    public void setManagerEmail(String managerEmail) { this.managerEmail = managerEmail; }

    public String getManagerJobTitle() { return managerJobTitle; }
    public void setManagerJobTitle(String managerJobTitle) { this.managerJobTitle = managerJobTitle; }

    public String getManagerAvatarUrl() { return managerAvatarUrl; }
    public void setManagerAvatarUrl(String managerAvatarUrl) { this.managerAvatarUrl = managerAvatarUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) {
        this.status = (status != null && !status.trim().isEmpty()) ? status.trim().toUpperCase() : STATUS_ACTIVE;
    }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public int getUserCount() { return userCount; }
    public void setUserCount(int userCount) { this.userCount = userCount; }

    public int getChildrenCount() { return childrenCount; }
    public void setChildrenCount(int childrenCount) { this.childrenCount = childrenCount; }

    public int getOpenRequisitionCount() { return openRequisitionCount; }
    public void setOpenRequisitionCount(int openRequisitionCount) { this.openRequisitionCount = openRequisitionCount; }

    public int getTotalRequisitionCount() { return totalRequisitionCount; }
    public void setTotalRequisitionCount(int totalRequisitionCount) { this.totalRequisitionCount = totalRequisitionCount; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public List<Department> getChildren() { return children; }
    public void setChildren(List<Department> children) {
        this.children = children != null ? children : new ArrayList<>();
        this.childrenCount = this.children.size();
    }

    public void addChild(Department child) {
        if (this.children == null) this.children = new ArrayList<>();
        this.children.add(child);
        this.childrenCount = this.children.size();
    }

    // Helper methods
    public boolean isRoot() {
        return parentId == null || parentId.trim().isEmpty();
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equalsIgnoreCase(status);
    }

    public boolean isInactive() {
        return STATUS_INACTIVE.equalsIgnoreCase(status);
    }

    public String getStatusLabel() {
        return isActive() ? "Đang áp dụng" : "Ngừng áp dụng";
    }

    public String getStatusBadgeClass() {
        return isActive() ? "bg-success-subtle text-success border border-success-subtle"
                          : "bg-warning-subtle text-warning border border-warning-subtle";
    }

    /**
     * Kiểm tra phòng ban có thể xóa được hay không
     * Business rule: Nếu có yêu cầu tuyển dụng mở hoặc có phòng ban con -> KHÔNG ĐƯỢC XÓA
     */
    public boolean canDelete() {
        return openRequisitionCount == 0 && childrenCount == 0 && userCount == 0 && totalRequisitionCount == 0;
    }

    public String getIndentedName() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("　　");
        }
        if (level > 0) {
            sb.append("└── ");
        }
        sb.append(name).append(" (").append(code).append(")");
        return sb.toString();
    }
}

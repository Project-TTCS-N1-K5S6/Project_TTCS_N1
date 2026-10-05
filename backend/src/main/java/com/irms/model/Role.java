package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Đại diện thực thể Vai trò (bảng roles)
 */
public class Role implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String code;
    private String name;
    private String description;
    private boolean systemRole;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private int userCount;
    private List<Permission> permissions = new ArrayList<>();

    public Role() {}

    public Role(String id, String code, String name, String description, boolean systemRole) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.systemRole = systemRole;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isSystemRole() { return systemRole; }
    public void setSystemRole(boolean systemRole) { this.systemRole = systemRole; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public int getUserCount() { return userCount; }
    public void setUserCount(int userCount) { this.userCount = userCount; }

    public List<Permission> getPermissions() { return permissions; }
    public void setPermissions(List<Permission> permissions) { this.permissions = permissions; }
}

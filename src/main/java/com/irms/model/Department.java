package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Đại diện thực thể Phòng ban / Đơn vị tổ chức (bảng departments)
 */
public class Department implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String code;
    private String name;
    private String description;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private int userCount;

    public Department() {}

    public Department(String id, String code, String name, String description) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public int getUserCount() { return userCount; }
    public void setUserCount(int userCount) { this.userCount = userCount; }
}

package com.irms.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Đại diện thực thể Quyền hạn (bảng permissions)
 */
public class Permission implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String code;
    private String name;
    private String module;
    private String action;
    private String description;
    private Timestamp createdAt;

    public Permission() {}

    public Permission(String id, String code, String name, String module, String action, String description) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.module = module;
        this.action = action;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

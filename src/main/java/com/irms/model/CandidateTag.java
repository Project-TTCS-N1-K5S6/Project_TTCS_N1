package com.irms.model;

import java.io.Serializable;

public class CandidateTag implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String categoryId;
    private String categoryName;
    private String name;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

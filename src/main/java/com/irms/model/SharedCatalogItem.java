package com.irms.model;

import java.io.Serializable;

/**
 * Model đại diện cho một mục trong Danh mục dùng chung (bảng shared_catalog)
 * Quản lý loại danh mục (type), giá trị hiển thị (value), thứ tự sắp xếp (displayOrder) và số lượng bản ghi đang tham chiếu (referenceCount)
 */
public class SharedCatalogItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private SharedCatalogType type;
    private String value;
    private int displayOrder;
    private int referenceCount;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SharedCatalogType getType() {
        return type;
    }

    public void setType(SharedCatalogType type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public int getReferenceCount() {
        return referenceCount;
    }

    public void setReferenceCount(int referenceCount) {
        this.referenceCount = referenceCount;
    }
}

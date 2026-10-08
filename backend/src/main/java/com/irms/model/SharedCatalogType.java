package com.irms.model;

public enum SharedCatalogType {
    APPLICATION_SOURCE("APPLICATION_SOURCE", "Nguồn ứng viên"),
    REJECTION_REASON("REJECTION_REASON", "Lý do loại hồ sơ"),
    WORK_LOCATION("WORK_LOCATION", "Địa điểm làm việc"),
    WORK_MODE("WORK_MODE", "Hình thức làm việc");

    private final String code;
    private final String label;

    SharedCatalogType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static SharedCatalogType fromCode(String code) {
        if (code != null) {
            for (SharedCatalogType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException("Loại danh mục không hợp lệ.");
    }
}

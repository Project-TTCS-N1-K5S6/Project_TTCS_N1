package com.irms.model;

/**
 * Enum định nghĩa các phân loại Danh mục dùng chung trong hệ thống:
 * - APPLICATION_SOURCE: Nguồn ứng viên (Website, LinkedIn, Facebook...)
 * - REJECTION_REASON: Lý do loại hồ sơ (Không đủ kinh nghiệm, Thiếu kỹ năng...)
 * - WORK_LOCATION: Địa điểm làm việc (Hà Nội, TP.HCM, Đà Nẵng...)
 * - WORK_MODE: Hình thức làm việc (Full-time, Part-time, Remote, Hybrid...)
 */
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

    /**
     * Chức năng: Chuyển đổi từ chuỗi mã code sang đối tượng Enum SharedCatalogType tương ứng
     *
     * @param code Mã định danh của loại danh mục
     * @return Enum SharedCatalogType khớp với mã
     */
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

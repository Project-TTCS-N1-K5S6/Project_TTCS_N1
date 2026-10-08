package com.irms.service;

import com.irms.dao.SalaryRangeDAO;
import com.irms.model.SalaryRange;
import com.irms.util.SecurityUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Xử lý nghiệp vụ Khai báo Dải lương vị trí tuyển dụng & Kiểm soát Hạn mức duyệt Offer (KN-103)
 */
public class SalaryRangeService {
    private final SalaryRangeDAO salaryRangeDAO = new SalaryRangeDAO();

    public List<SalaryRange> getAllSalaryRanges() {
        return salaryRangeDAO.findAll();
    }

    public List<SalaryRange> getAllSalaryRanges(String search, String deptId, String level) {
        return salaryRangeDAO.findAll(search, deptId, level);
    }

    public SalaryRange getSalaryRangeById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        return salaryRangeDAO.findById(id.trim());
    }

    public SalaryRange getSalaryRangeByPositionCode(String code) {
        if (code == null || code.trim().isEmpty()) return null;
        return salaryRangeDAO.findByPositionCode(code.trim());
    }

    public List<String> getDistinctLevels() {
        return salaryRangeDAO.getDistinctLevels();
    }

    /**
     * Thêm mới dải lương chức danh với đầy đủ validation chặt chẽ
     */
    public boolean createSalaryRange(SalaryRange sr) {
        validateSalaryRange(sr, true);
        if (sr.getId() == null || sr.getId().trim().isEmpty()) {
            sr.setId(SecurityUtil.generateUUID());
        }
        return salaryRangeDAO.insert(sr);
    }

    /**
     * Cập nhật thông tin dải lương chức danh
     */
    public boolean updateSalaryRange(SalaryRange sr) {
        if (sr.getId() == null || sr.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("ID dải lương không hợp lệ!");
        }
        SalaryRange existing = salaryRangeDAO.findById(sr.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Không tìm thấy dải lương cần cập nhật!");
        }
        validateSalaryRange(sr, false);
        return salaryRangeDAO.update(sr);
    }

    /**
     * Xóa dải lương chức danh
     */
    public boolean deleteSalaryRange(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID dải lương không hợp lệ!");
        }
        return salaryRangeDAO.delete(id.trim());
    }

    /**
     * Kiểm tra tính hợp lệ dữ liệu dải lương
     */
    private void validateSalaryRange(SalaryRange sr, boolean isNew) {
        if (sr == null) {
            throw new IllegalArgumentException("Thông tin dải lương không được để trống!");
        }
        if (sr.getPositionCode() == null || sr.getPositionCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã chức danh không được để trống!");
        }
        sr.setPositionCode(sr.getPositionCode().trim().toUpperCase());

        // Kiểm tra trùng mã chức danh
        String excludeId = isNew ? null : sr.getId();
        if (salaryRangeDAO.existsByPositionCode(sr.getPositionCode(), excludeId)) {
            throw new IllegalArgumentException("Mã chức danh '" + sr.getPositionCode() + "' đã tồn tại trên hệ thống!");
        }

        if (sr.getPositionTitle() == null || sr.getPositionTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên chức danh không được để trống!");
        }
        sr.setPositionTitle(sr.getPositionTitle().trim());

        if (sr.getLevel() == null || sr.getLevel().trim().isEmpty()) {
            throw new IllegalArgumentException("Cấp bậc chức danh không được để trống!");
        }
        sr.setLevel(sr.getLevel().trim());

        if (sr.getMinSalary() == null || sr.getMinSalary().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Mức lương tối thiểu phải lớn hơn hoặc bằng 0!");
        }

        if (sr.getMaxSalary() == null || sr.getMaxSalary().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Mức lương tối đa phải lớn hơn 0!");
        }

        if (sr.getMaxSalary().compareTo(sr.getMinSalary()) < 0) {
            throw new IllegalArgumentException("Mức lương tối đa (" + sr.getMaxSalary() + ") không được nhỏ hơn mức lương tối thiểu (" + sr.getMinSalary() + ")!");
        }

        if (sr.getCurrency() == null || sr.getCurrency().trim().isEmpty()) {
            sr.setCurrency("VND");
        } else {
            sr.setCurrency(sr.getCurrency().trim().toUpperCase());
        }
    }

    /**
     * Tiêu chí KN-103: Dải lương dùng làm hạn mức duyệt offer về sau
     * Đối soát mức lương đề xuất với dải lương chức danh theo Range ID
     */
    public OfferLimitResult checkOfferLimit(String rangeId, BigDecimal offerSalary) {
        SalaryRange sr = getSalaryRangeById(rangeId);
        if (sr == null) {
            return new OfferLimitResult(false, "NOT_FOUND", "Không tìm thấy thông tin dải lương cho vị trí này.", null, null, offerSalary, BigDecimal.ZERO, 0);
        }
        return evaluateOfferAgainstRange(sr, offerSalary);
    }

    /**
     * Đối soát mức lương đề xuất theo tên chức danh và cấp bậc
     */
    public OfferLimitResult checkOfferLimitByPosition(String positionTitle, String level, BigDecimal offerSalary) {
        SalaryRange sr = salaryRangeDAO.findByPositionAndLevel(positionTitle, level);
        if (sr == null) {
            return new OfferLimitResult(false, "NOT_FOUND", "Chưa có quy định dải lương cho chức danh '" + positionTitle + "' cấp bậc '" + level + "'.", null, null, offerSalary, BigDecimal.ZERO, 0);
        }
        return evaluateOfferAgainstRange(sr, offerSalary);
    }

    private OfferLimitResult evaluateOfferAgainstRange(SalaryRange sr, BigDecimal offerSalary) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        if (offerSalary == null || offerSalary.compareTo(BigDecimal.ZERO) <= 0) {
            return new OfferLimitResult(false, "INVALID_SALARY", "Mức lương offer phải lớn hơn 0.", sr.getMinSalary(), sr.getMaxSalary(), offerSalary, BigDecimal.ZERO, 0);
        }

        // Trường hợp 1: Vượt trần dải lương tối đa
        if (offerSalary.compareTo(sr.getMaxSalary()) > 0) {
            BigDecimal diff = offerSalary.subtract(sr.getMaxSalary());
            double percent = (sr.getMaxSalary() != null && sr.getMaxSalary().compareTo(BigDecimal.ZERO) > 0)
                    ? diff.divide(sr.getMaxSalary(), 4, RoundingMode.HALF_UP).doubleValue() * 100 : 0.0;
            String msg = String.format("CẢNH BÁO: Mức offer %s %s đã VƯỢT HẠN MỨC TỐI ĐA (%s %s) của vị trí %s (%s). Vượt trần %s %s (+%.1f%%). Cần phê duyệt ngoại lệ!",
                    nf.format(offerSalary), sr.getCurrency(),
                    nf.format(sr.getMaxSalary()), sr.getCurrency(),
                    sr.getPositionTitle(), sr.getLevel(),
                    nf.format(diff), sr.getCurrency(), percent);
            return new OfferLimitResult(false, "EXCEEDED_MAX", msg, sr.getMinSalary(), sr.getMaxSalary(), offerSalary, diff, percent);
        }

        // Trường hợp 2: Dưới sàn dải lương tối thiểu
        if (offerSalary.compareTo(sr.getMinSalary()) < 0) {
            BigDecimal diff = sr.getMinSalary().subtract(offerSalary);
            double percent = (sr.getMinSalary() != null && sr.getMinSalary().compareTo(BigDecimal.ZERO) > 0)
                    ? diff.divide(sr.getMinSalary(), 4, RoundingMode.HALF_UP).doubleValue() * 100 : 0.0;
            String msg = String.format("LƯU Ý: Mức offer %s %s thấp hơn mức tối thiểu quy định (%s %s) của vị trí %s (%s). Chênh lệch -%s %s (-%.1f%%).",
                    nf.format(offerSalary), sr.getCurrency(),
                    nf.format(sr.getMinSalary()), sr.getCurrency(),
                    sr.getPositionTitle(), sr.getLevel(),
                    nf.format(diff), sr.getCurrency(), percent);
            return new OfferLimitResult(true, "BELOW_MIN", msg, sr.getMinSalary(), sr.getMaxSalary(), offerSalary, diff.negate(), percent);
        }

        // Trường hợp 3: Hợp lệ trong hạn mức
        String msg = String.format("HỢP LỆ: Mức offer %s %s nằm trong hạn mức ngân sách được duyệt (%s - %s %s) của vị trí %s (%s).",
                nf.format(offerSalary), sr.getCurrency(),
                nf.format(sr.getMinSalary()), nf.format(sr.getMaxSalary()), sr.getCurrency(),
                sr.getPositionTitle(), sr.getLevel());
        return new OfferLimitResult(true, "VALID", msg, sr.getMinSalary(), sr.getMaxSalary(), offerSalary, BigDecimal.ZERO, 0);
    }

    /**
     * DTO kết quả kiểm tra hạn mức duyệt Offer
     */
    public static class OfferLimitResult implements Serializable {
        private static final long serialVersionUID = 1L;

        private final boolean approved;
        private final String status;      // VALID, EXCEEDED_MAX, BELOW_MIN, NOT_FOUND, INVALID_SALARY
        private final String message;
        private final BigDecimal minSalary;
        private final BigDecimal maxSalary;
        private final BigDecimal offerSalary;
        private final BigDecimal diffAmount;
        private final double diffPercent;

        public OfferLimitResult(boolean approved, String status, String message, 
                                BigDecimal minSalary, BigDecimal maxSalary, BigDecimal offerSalary, 
                                BigDecimal diffAmount, double diffPercent) {
            this.approved = approved;
            this.status = status;
            this.message = message;
            this.minSalary = minSalary;
            this.maxSalary = maxSalary;
            this.offerSalary = offerSalary;
            this.diffAmount = diffAmount;
            this.diffPercent = diffPercent;
        }

        public boolean isApproved() { return approved; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public BigDecimal getMinSalary() { return minSalary; }
        public BigDecimal getMaxSalary() { return maxSalary; }
        public BigDecimal getOfferSalary() { return offerSalary; }
        public BigDecimal getDiffAmount() { return diffAmount; }
        public double getDiffPercent() { return diffPercent; }
    }
}

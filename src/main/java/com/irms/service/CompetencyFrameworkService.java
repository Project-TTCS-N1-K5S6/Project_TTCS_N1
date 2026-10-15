package com.irms.service;

import com.irms.dao.AuditDAO;
import com.irms.dao.CompetencyCriterionDAO;
import com.irms.dao.CompetencyFrameworkDAO;
import com.irms.model.AuditLog;
import com.irms.model.CompetencyFramework;
import com.irms.model.CompetencyFrameworkCriterion;
import com.irms.model.PositionCompetencyFramework;
import com.irms.util.SecurityUtil;

import java.math.BigDecimal;
import java.util.*;

/**
 * Nghiệp vụ Quản lý Khung năng lực & Validation Trọng số tiêu chí 100%
 */
public class CompetencyFrameworkService {

    private final CompetencyFrameworkDAO frameworkDAO = new CompetencyFrameworkDAO();
    private final CompetencyCriterionDAO criterionDAO = new CompetencyCriterionDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    public List<CompetencyFramework> getAllFrameworks(String search, String status) {
        return frameworkDAO.findAll(search, status);
    }

    public CompetencyFramework getFrameworkById(String id) {
        return frameworkDAO.findById(id);
    }

    public CompetencyFramework getFrameworkByCode(String code) {
        return frameworkDAO.findByCode(code);
    }

    public List<PositionCompetencyFramework> getAllPositionsWithFrameworkInfo() {
        return frameworkDAO.findAllPositionsWithFrameworkInfo();
    }

    /**
     * Tạo mới Khung năng lực kèm bộ tiêu chí và kiểm tra nghiêm ngặt quy tắc tổng trọng số = 100%
     */
    public CompetencyFramework createFramework(CompetencyFramework framework, List<CompetencyFrameworkCriterion> criteria,
                                               String userId, String ip, String userAgent) {
        validateFrameworkInfo(framework, null);
        validateCriteriaAndWeights(criteria, CompetencyFramework.STATUS_ACTIVE.equalsIgnoreCase(framework.getStatus()));

        if (framework.getId() == null || framework.getId().trim().isEmpty()) {
            framework.setId(SecurityUtil.generateUUID());
        }
        framework.setCreatedBy(userId);
        framework.setUpdatedBy(userId);

        boolean success = frameworkDAO.insert(framework, criteria);
        if (!success) {
            throw new RuntimeException("Lỗi hệ thống khi lưu khung năng lực vào cơ sở dữ liệu!");
        }

        // Ghi nhận nhật ký kiểm toán (Audit Log)
        logAudit(userId, "COMPETENCY_FRAMEWORK_CREATED", "COMPETENCY_FRAMEWORK", framework.getId(),
                "Tạo mới khung năng lực: " + framework.getCode() + " - " + framework.getName() +
                " (" + (criteria != null ? criteria.size() : 0) + " tiêu chí, Trạng thái: " + framework.getStatus() + ")",
                ip, userAgent);

        return frameworkDAO.findById(framework.getId());
    }

    /**
     * Cập nhật Khung năng lực và danh sách tiêu chí
     */
    public CompetencyFramework updateFramework(CompetencyFramework framework, List<CompetencyFrameworkCriterion> criteria,
                                               String userId, String ip, String userAgent) {
        if (framework.getId() == null || framework.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("ID khung năng lực không hợp lệ!");
        }

        CompetencyFramework existing = frameworkDAO.findById(framework.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Khung năng lực không tồn tại trên hệ thống!");
        }

        validateFrameworkInfo(framework, framework.getId());
        validateCriteriaAndWeights(criteria, CompetencyFramework.STATUS_ACTIVE.equalsIgnoreCase(framework.getStatus()));

        framework.setUpdatedBy(userId);

        boolean success = frameworkDAO.update(framework, criteria);
        if (!success) {
            throw new RuntimeException("Lỗi hệ thống khi cập nhật khung năng lực!");
        }

        // Ghi nhận nhật ký kiểm toán
        logAudit(userId, "COMPETENCY_FRAMEWORK_UPDATED", "COMPETENCY_FRAMEWORK", framework.getId(),
                "Cập nhật khung năng lực: " + framework.getCode() + " - " + framework.getName() +
                " (" + (criteria != null ? criteria.size() : 0) + " tiêu chí)",
                ip, userAgent);

        return frameworkDAO.findById(framework.getId());
    }

    /**
     * Thay đổi trạng thái Khung năng lực (Kích hoạt, Ngừng áp dụng, Chuyển về nháp)
     */
    public void changeStatus(String frameworkId, String newStatus, String userId, String ip, String userAgent) {
        CompetencyFramework cf = frameworkDAO.findById(frameworkId);
        if (cf == null) {
            throw new IllegalArgumentException("Không tìm thấy khung năng lực!");
        }

        newStatus = newStatus != null ? newStatus.trim().toUpperCase() : "";
        if (!CompetencyFramework.STATUS_ACTIVE.equals(newStatus) &&
            !CompetencyFramework.STATUS_INACTIVE.equals(newStatus) &&
            !CompetencyFramework.STATUS_DRAFT.equals(newStatus)) {
            throw new IllegalArgumentException("Trạng thái mới không hợp lệ!");
        }

        // Khi KÍCH HOẠT (ACTIVE): Phải có tiêu chí và tổng trọng số BẮT BUỘC bằng 100%
        if (CompetencyFramework.STATUS_ACTIVE.equals(newStatus)) {
            if (cf.getCriteria() == null || cf.getCriteria().isEmpty()) {
                throw new IllegalStateException("Không thể kích hoạt khung năng lực chưa có tiêu chí đánh giá!");
            }
            cf.calculateMetrics();
            if (!cf.isWeightValid()) {
                throw new IllegalStateException("Không thể kích hoạt! Tổng trọng số hiện tại là " +
                        cf.getTotalWeight() + "%, bắt buộc phải bằng đúng 100%!");
            }
        }

        boolean success = frameworkDAO.updateStatus(frameworkId, newStatus, userId);
        if (!success) {
            throw new RuntimeException("Lỗi cập nhật trạng thái khung năng lực!");
        }

        logAudit(userId, "COMPETENCY_FRAMEWORK_STATUS_CHANGED", "COMPETENCY_FRAMEWORK", frameworkId,
                "Chuyển trạng thái khung năng lực '" + cf.getCode() + "' sang: " + newStatus,
                ip, userAgent);
    }

    /**
     * Gán Khung năng lực cho một hoặc nhiều chức danh
     * Business Rule: Chỉ khung năng lực ACTIVE mới được gán cho chức danh!
     */
    public void assignPositions(String frameworkId, List<String> positionIds, String userId, String ip, String userAgent) {
        CompetencyFramework cf = frameworkDAO.findById(frameworkId);
        if (cf == null) {
            throw new IllegalArgumentException("Không tìm thấy khung năng lực!");
        }

        if (!cf.isActive()) {
            throw new IllegalStateException("Chỉ khung năng lực đang ở trạng thái 'Đang áp dụng' mới được gán cho chức danh!");
        }

        boolean success = frameworkDAO.assignPositionsToFramework(frameworkId, positionIds, userId);
        if (!success) {
            throw new RuntimeException("Lỗi lưu liên kết chức danh và khung năng lực!");
        }

        int count = positionIds != null ? positionIds.size() : 0;
        logAudit(userId, "COMPETENCY_FRAMEWORK_POSITIONS_ASSIGNED", "COMPETENCY_FRAMEWORK", frameworkId,
                "Gán khung năng lực '" + cf.getCode() + "' cho " + count + " chức danh",
                ip, userAgent);
    }

    /**
     * Xóa khung năng lực (Kiểm tra an toàn: Không cho xóa nếu đang có chức danh sử dụng)
     */
    public void deleteFramework(String frameworkId, String userId, String ip, String userAgent) {
        CompetencyFramework cf = frameworkDAO.findById(frameworkId);
        if (cf == null) {
            throw new IllegalArgumentException("Khung năng lực không tồn tại!");
        }

        if (cf.getAssignedPositionsCount() > 0) {
            throw new IllegalStateException("Không thể xóa khung năng lực đang được áp dụng cho " +
                    cf.getAssignedPositionsCount() + " chức danh! Vui lòng hủy gán chức danh trước.");
        }

        boolean success = frameworkDAO.delete(frameworkId);
        if (!success) {
            throw new RuntimeException("Lỗi khi xóa khung năng lực!");
        }

        logAudit(userId, "COMPETENCY_FRAMEWORK_DELETED", "COMPETENCY_FRAMEWORK", frameworkId,
                "Đã xóa khung năng lực: " + cf.getCode() + " - " + cf.getName(),
                ip, userAgent);
    }

    /**
     * Validate thông tin cơ bản của khung năng lực
     */
    public void validateFrameworkInfo(CompetencyFramework f, String excludeId) {
        if (f.getCode() == null || f.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Mã khung năng lực không được để trống!");
        }
        f.setCode(f.getCode().trim().toUpperCase());

        if (f.getName() == null || f.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Tên khung năng lực không được để trống!");
        }
        f.setName(f.getName().trim());

        if (frameworkDAO.existsByCode(f.getCode(), excludeId)) {
            throw new IllegalArgumentException("Mã khung năng lực '" + f.getCode() + "' đã tồn tại trên hệ thống!");
        }

        if (f.getStatus() == null || f.getStatus().trim().isEmpty()) {
            f.setStatus(CompetencyFramework.STATUS_DRAFT);
        } else {
            f.setStatus(f.getStatus().trim().toUpperCase());
        }
    }

    /**
     * VALIDATION TRỌNG SỐ - BUSINESS RULE BẮT BUỘC:
     * 1. Danh sách tiêu chí không được rỗng khi kích hoạt/áp dụng.
     * 2. Không có 2 tiêu chí trùng lặp trong cùng 1 khung.
     * 3. Mỗi trọng số > 0. Trọng số <= 0 -> báo lỗi.
     * 4. Tổng trọng số của toàn bộ tiêu chí PHẢI = 100%.
     *    Tổng < 100% -> Không cho lưu / áp dụng.
     *    Tổng > 100% -> Không cho lưu / áp dụng.
     */
    public void validateCriteriaAndWeights(List<CompetencyFrameworkCriterion> criteria, boolean requireActive) {
        if (criteria == null || criteria.isEmpty()) {
            throw new IllegalArgumentException("Khung năng lực phải có ít nhất 1 tiêu chí đánh giá!");
        }

        Set<String> seenCriterionIds = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;

        for (int i = 0; i < criteria.size(); i++) {
            CompetencyFrameworkCriterion item = criteria.get(i);
            if (item.getCriterionId() == null || item.getCriterionId().trim().isEmpty()) {
                throw new IllegalArgumentException("Tiêu chí tại dòng " + (i + 1) + " chưa được chọn!");
            }

            if (!seenCriterionIds.add(item.getCriterionId())) {
                throw new IllegalArgumentException("Tiêu chí không được trùng lặp trong cùng một khung năng lực!");
            }

            BigDecimal weight = item.getWeight();
            if (weight == null) {
                throw new IllegalArgumentException("Trọng số của tiêu chí không hợp lệ!");
            }

            if (weight.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Trọng số của từng tiêu chí phải lớn hơn 0%! (Dòng " + (i + 1) + ")");
            }

            if (weight.compareTo(new BigDecimal("100")) > 0) {
                throw new IllegalArgumentException("Trọng số của một tiêu chí không được vượt quá 100%!");
            }

            total = total.add(weight);
        }

        // Kiểm tra tổng trọng số = 100%
        if (total.compareTo(new BigDecimal("100.00")) < 0) {
            BigDecimal diff = new BigDecimal("100.00").subtract(total);
            throw new IllegalArgumentException("Tổng trọng số của khung năng lực chưa đủ 100% (Hiện tại: " +
                    formatBigDecimal(total) + "%, Còn thiếu: " + formatBigDecimal(diff) + "%). Vui lòng điều chỉnh lại!");
        }

        if (total.compareTo(new BigDecimal("100.00")) > 0) {
            BigDecimal diff = total.subtract(new BigDecimal("100.00"));
            throw new IllegalArgumentException("Tổng trọng số của khung năng lực đã vượt quá 100% (Hiện tại: " +
                    formatBigDecimal(total) + "%, Vượt mức: " + formatBigDecimal(diff) + "%). Vui lòng điều chỉnh lại!");
        }
    }

    private String formatBigDecimal(BigDecimal b) {
        if (b == null) return "0";
        if (b.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            return b.stripTrailingZeros().toPlainString();
        }
        return b.toString();
    }

    private void logAudit(String userId, String action, String entityType, String entityId, String desc, String ip, String ua) {
        try {
            AuditLog log = new AuditLog();
            log.setId(SecurityUtil.generateUUID());
            log.setUserId(userId);
            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setDescription(desc);
            log.setIpAddress(ip);
            log.setUserAgent(ua);
            auditDAO.insert(log);
        } catch (Exception ignored) {}
    }
}

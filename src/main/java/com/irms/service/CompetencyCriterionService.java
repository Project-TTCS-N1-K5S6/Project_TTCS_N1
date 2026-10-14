package com.irms.service;

import com.irms.dao.CompetencyCriterionDAO;
import com.irms.model.CompetencyCriterion;

import java.util.List;
import java.util.UUID;

/**
 * Tầng nghiệp vụ quản lý Tiêu chí Năng lực (Competency Criteria)
 * Cung cấp logic kiểm tra trùng mã, tạo mới, cập nhật hướng dẫn đánh giá và xóa tiêu chí năng lực
 */
public class CompetencyCriterionService {
    private final CompetencyCriterionDAO dao = new CompetencyCriterionDAO();

    /**
     * Chức năng: Lấy danh sách tất cả các tiêu chí năng lực trong hệ thống
     *
     * @return Danh sách toàn bộ tiêu chí năng lực
     */
    public List<CompetencyCriterion> getAllCriteria() {
        return dao.findAll();
    }

    /**
     * Chức năng: Lấy danh sách các tiêu chí năng lực đang có hiệu lực (trạng thái ACTIVE)
     *
     * @return Danh sách tiêu chí hoạt động
     */
    public List<CompetencyCriterion> getActiveCriteria() {
        return dao.findAllActive();
    }

    /**
     * Chức năng: Tìm kiếm chi tiết tiêu chí năng lực theo mã định danh ID
     *
     * @param id ID của tiêu chí
     * @return Thực thể CompetencyCriterion tương ứng
     */
    public CompetencyCriterion getCriterion(String id) {
        return dao.findById(id);
    }

    /**
     * Chức năng: Tìm kiếm tiêu chí năng lực theo mã code duy nhất
     *
     * @param code Mã code tiêu chí (ví dụ: CRIT-01)
     * @return Thực thể CompetencyCriterion tương ứng
     */
    public CompetencyCriterion getCriterionByCode(String code) {
        return dao.findByCode(code);
    }

    /**
     * Chức năng: Tạo mới tiêu chí năng lực dạng rút gọn (chỉ gồm tên và mô tả)
     *
     * @param name        Tên tiêu chí năng lực
     * @param description Mô tả chi tiết tiêu chí
     */
    public void createCriterion(String name, String description) {
        createCriterion(null, name, description, null);
    }

    /**
     * Chức năng: Tạo mới tiêu chí năng lực đầy đủ thuộc tính, kiểm tra tính hợp lệ và tự động sinh mã CRIT nếu không truyền
     *
     * @param code                Mã tiêu chí (ví dụ: CRIT-PYTHON)
     * @param name                Tên tiêu chí (bắt buộc)
     * @param description         Mô tả chi tiết năng lực
     * @param evaluationGuideline Hướng dẫn phỏng vấn/chấm điểm tiêu chí
     * @return Thực thể CompetencyCriterion vừa được tạo
     */
    public CompetencyCriterion createCriterion(String code, String name, String description, String evaluationGuideline) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên tiêu chí không được để trống!");
        }
        if (code != null && !code.trim().isEmpty()) {
            code = code.trim().toUpperCase();
            if (dao.existsByCode(code, null)) {
                throw new IllegalArgumentException("Mã tiêu chí '" + code + "' đã tồn tại!");
            }
        } else {
            code = "CRIT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        CompetencyCriterion c = new CompetencyCriterion();
        c.setId(UUID.randomUUID().toString());
        c.setCode(code);
        c.setName(name.trim());
        c.setDescription(description != null ? description.trim() : null);
        c.setEvaluationGuideline(evaluationGuideline != null ? evaluationGuideline.trim() : null);
        c.setStatus("ACTIVE");

        if (!dao.insert(c)) {
            throw new RuntimeException("Không thể thêm tiêu chí năng lực.");
        }
        return c;
    }

    /**
     * Chức năng: Cập nhật tên và mô tả của tiêu chí năng lực (giữ nguyên mã, hướng dẫn và trạng thái cũ)
     *
     * @param id          ID tiêu chí
     * @param name        Tên tiêu chí mới
     * @param description Mô tả mới
     */
    public void updateCriterion(String id, String name, String description) {
        CompetencyCriterion existing = dao.findById(id);
        String code = existing != null ? existing.getCode() : null;
        String guide = existing != null ? existing.getEvaluationGuideline() : null;
        String status = existing != null ? existing.getStatus() : "ACTIVE";
        updateCriterion(id, code, name, description, guide, status);
    }

    /**
     * Chức năng: Cập nhật toàn diện các thông tin của tiêu chí năng lực và kiểm tra trùng lặp mã code
     *
     * @param id                  ID tiêu chí cần cập nhật
     * @param code                Mã code tiêu chí mới
     * @param name                Tên tiêu chí mới
     * @param description         Mô tả tiêu chí
     * @param evaluationGuideline Hướng dẫn đánh giá
     * @param status              Trạng thái (ACTIVE / INACTIVE)
     */
    public void updateCriterion(String id, String code, String name, String description, String evaluationGuideline, String status) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên tiêu chí không được để trống!");
        }
        if (code != null && !code.trim().isEmpty()) {
            code = code.trim().toUpperCase();
            if (dao.existsByCode(code, id)) {
                throw new IllegalArgumentException("Mã tiêu chí '" + code + "' đã tồn tại!");
            }
        }

        CompetencyCriterion c = new CompetencyCriterion();
        c.setId(id);
        c.setCode(code);
        c.setName(name.trim());
        c.setDescription(description != null ? description.trim() : null);
        c.setEvaluationGuideline(evaluationGuideline != null ? evaluationGuideline.trim() : null);
        c.setStatus(status != null ? status : "ACTIVE");

        if (!dao.update(c)) {
            throw new RuntimeException("Không thể cập nhật tiêu chí năng lực.");
        }
    }

    /**
     * Chức năng: Xóa một tiêu chí năng lực khỏi hệ thống theo mã ID
     *
     * @param id ID tiêu chí cần xóa
     */
    public void deleteCriterion(String id) {
        if (!dao.delete(id)) {
            throw new RuntimeException("Không thể xóa tiêu chí năng lực.");
        }
    }
}

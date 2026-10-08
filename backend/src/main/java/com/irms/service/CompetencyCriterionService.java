package com.irms.service;

import com.irms.dao.CompetencyCriterionDAO;
import com.irms.model.CompetencyCriterion;

import java.util.List;
import java.util.UUID;

public class CompetencyCriterionService {
    private final CompetencyCriterionDAO dao = new CompetencyCriterionDAO();

    public List<CompetencyCriterion> getAllCriteria() {
        return dao.findAll();
    }

    public List<CompetencyCriterion> getActiveCriteria() {
        return dao.findAllActive();
    }

    public CompetencyCriterion getCriterion(String id) {
        return dao.findById(id);
    }

    public CompetencyCriterion getCriterionByCode(String code) {
        return dao.findByCode(code);
    }

    public void createCriterion(String name, String description) {
        createCriterion(null, name, description, null);
    }

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

    public void updateCriterion(String id, String name, String description) {
        CompetencyCriterion existing = dao.findById(id);
        String code = existing != null ? existing.getCode() : null;
        String guide = existing != null ? existing.getEvaluationGuideline() : null;
        String status = existing != null ? existing.getStatus() : "ACTIVE";
        updateCriterion(id, code, name, description, guide, status);
    }

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

    public void deleteCriterion(String id) {
        if (!dao.delete(id)) {
            throw new RuntimeException("Không thể xóa tiêu chí năng lực.");
        }
    }
}

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

    public CompetencyCriterion getCriterion(String id) {
        return dao.findById(id);
    }

    public void createCriterion(String name, String description) {
        CompetencyCriterion c = new CompetencyCriterion();
        c.setId(UUID.randomUUID().toString());
        c.setName(name);
        c.setDescription(description);
        
        if (!dao.insert(c)) {
            throw new RuntimeException("Không thể thêm tiêu chí năng lực.");
        }
    }

    public void updateCriterion(String id, String name, String description) {
        CompetencyCriterion c = new CompetencyCriterion();
        c.setId(id);
        c.setName(name);
        c.setDescription(description);
        
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

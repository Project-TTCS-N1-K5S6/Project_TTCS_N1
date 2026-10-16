package com.irms.service;

import com.irms.dao.SalaryRangeDAO;
import com.irms.model.SalaryRange;
import com.irms.util.SecurityUtil;

import java.util.List;

/**
 * Xử lý nghiệp vụ Dải lương vị trí tuyển dụng
 */
public class SalaryRangeService {
    private final SalaryRangeDAO salaryRangeDAO = new SalaryRangeDAO();

    public List<SalaryRange> getAllSalaryRanges() {
        return salaryRangeDAO.findAll();
    }

    public boolean createSalaryRange(SalaryRange sr) {
        sr.setId(SecurityUtil.generateUUID());
        return salaryRangeDAO.insert(sr);
    }
}

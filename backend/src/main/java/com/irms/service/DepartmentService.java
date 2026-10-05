package com.irms.service;

import com.irms.dao.DepartmentDAO;
import com.irms.model.Department;
import com.irms.util.SecurityUtil;

import java.util.List;

/**
 * Xử lý nghiệp vụ Danh mục cơ cấu tổ chức & phòng ban
 */
public class DepartmentService {
    private final DepartmentDAO departmentDAO = new DepartmentDAO();

    public List<Department> getAllDepartments() {
        return departmentDAO.findAll();
    }

    public Department getDepartmentById(String id) {
        return departmentDAO.findById(id);
    }

    public boolean createDepartment(String code, String name, String description) {
        Department d = new Department(SecurityUtil.generateUUID(), code, name, description);
        return departmentDAO.insert(d);
    }

    public boolean updateDepartment(String id, String code, String name, String description) {
        Department d = new Department(id, code, name, description);
        return departmentDAO.update(d);
    }

    public boolean deleteDepartment(String id) {
        return departmentDAO.delete(id);
    }
}

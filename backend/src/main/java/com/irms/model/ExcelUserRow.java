package com.irms.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Đối tượng biểu diễn dữ liệu của một dòng trong file Excel nhập danh sách nhân sự
 */
public class ExcelUserRow implements Serializable {
    private static final long serialVersionUID = 1L;

    private int rowNumber;
    private String employeeCode;
    private String fullName;
    private String email;
    private String phone;
    private String jobTitle;
    private String departmentName;
    private String departmentId;
    private String rolesRaw;
    private List<String> roleIds = new ArrayList<>();
    private List<String> roleNames = new ArrayList<>();
    private boolean valid = true;
    private List<String> errors = new ArrayList<>();

    public ExcelUserRow() {}

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getRolesRaw() {
        return rolesRaw;
    }

    public void setRolesRaw(String rolesRaw) {
        this.rolesRaw = rolesRaw;
    }

    public List<String> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<String> roleIds) {
        this.roleIds = roleIds;
    }

    public List<String> getRoleNames() {
        return roleNames;
    }

    public void setRoleNames(List<String> roleNames) {
        this.roleNames = roleNames;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
        if (errors != null && !errors.isEmpty()) {
            this.valid = false;
        }
    }

    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.valid = false;
    }
}

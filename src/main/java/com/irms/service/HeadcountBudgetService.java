package com.irms.service;

import com.irms.dao.DepartmentDAO;
import com.irms.dao.HeadcountBudgetDAO;
import com.irms.model.BudgetSubtask;
import com.irms.model.DepartmentHeadcountBudget;
import com.irms.model.User;
import com.irms.util.SecurityUtil;

import java.math.BigDecimal;
import java.util.*;

/**
 * Xử lý nghiệp vụ phân hệ Quản lý Định biên & Ngân sách Nhân sự (KN-195 -> KN-201)
 */
public class HeadcountBudgetService {

    private final HeadcountBudgetDAO budgetDAO = new HeadcountBudgetDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final AuditService auditService = new AuditService();

    public HeadcountBudgetDAO getBudgetDAO() {
        return budgetDAO;
    }

    public List<DepartmentHeadcountBudget> getBudgets(Integer year, String departmentId, String keyword) {
        return budgetDAO.getBudgets(year, departmentId, keyword);
    }

    public DepartmentHeadcountBudget getBudgetById(String id) {
        return budgetDAO.getBudgetById(id);
    }

    public DepartmentHeadcountBudget getBudgetByDeptAndYear(String departmentId, int year, String excludeRequisitionId) {
        return budgetDAO.getBudgetByDeptAndYear(departmentId, year, excludeRequisitionId);
    }

    public Map<String, Object> getCompanyKpiSummary(int fiscalYear) {
        return budgetDAO.getCompanyKpiSummary(fiscalYear);
    }

    /**
     * Khai báo hoặc cập nhật Định biên & Ngân sách theo Phòng ban / Năm
     */
    public boolean saveBudget(DepartmentHeadcountBudget budget, String actorId, String ip, String userAgent) {
        if (budget == null) {
            throw new IllegalArgumentException("Dữ liệu định biên & ngân sách không hợp lệ!");
        }

        // 1. Kiểm tra phòng ban
        if (budget.getDepartmentId() == null || budget.getDepartmentId().trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn phòng ban áp dụng định biên!");
        }

        // 2. Kiểm tra năm tài chính
        if (budget.getFiscalYear() < 2000 || budget.getFiscalYear() > 2100) {
            throw new IllegalArgumentException("Năm tài chính không hợp lệ (phải từ 2000 đến 2100)!");
        }

        // 3. Kiểm tra chỉ tiêu headcount
        if (budget.getTargetHeadcount() < 0) {
            throw new IllegalArgumentException("Chỉ tiêu headcount không được là số âm!");
        }

        // 4. Kiểm tra ngân sách lương
        if (budget.getTotalSalaryBudget() == null || budget.getTotalSalaryBudget().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Tổng ngân sách lương không được là số âm!");
        }

        boolean isCreate = (budget.getId() == null || budget.getId().trim().isEmpty());

        // 5. Ngăn tạo trùng lặp bản ghi định biên cho cùng một phòng ban và cùng một năm
        if (budgetDAO.existsByDeptAndYear(budget.getDepartmentId(), budget.getFiscalYear(), isCreate ? null : budget.getId())) {
            throw new IllegalArgumentException("Phòng ban này đã được khai báo định biên & ngân sách cho năm " 
                    + budget.getFiscalYear() + "! Không thể tạo bản ghi trùng lặp.");
        }

        if (isCreate) {
            budget.setId(SecurityUtil.generateUUID());
            budget.setCreatedBy(actorId);
            boolean ok = budgetDAO.insert(budget);
            if (ok) {
                auditService.log(actorId, "BUDGET_CREATED", "BUDGET", budget.getId(),
                        "Khai báo định biên năm " + budget.getFiscalYear() + " cho phòng ban: " + budget.getDepartmentId()
                                + " (Chỉ tiêu: " + budget.getTargetHeadcount() + " nhân sự, Ngân sách: " + budget.getFormattedTotalBudget() + ")",
                        ip, userAgent);
            }
            return ok;
        } else {
            budget.setUpdatedBy(actorId);
            boolean ok = budgetDAO.update(budget);
            if (ok) {
                auditService.log(actorId, "BUDGET_UPDATED", "BUDGET", budget.getId(),
                        "Cập nhật định biên năm " + budget.getFiscalYear() + " phòng ban: " + budget.getDepartmentId()
                                + " (Chỉ tiêu mới: " + budget.getTargetHeadcount() + " nhân sự, Ngân sách mới: " + budget.getFormattedTotalBudget() + ")",
                        ip, userAgent);
            }
            return ok;
        }
    }

    /**
     * Xóa bản ghi định biên
     */
    public boolean deleteBudget(String id, String actorId, String ip, String userAgent) {
        DepartmentHeadcountBudget b = budgetDAO.getBudgetById(id);
        if (b == null) return false;

        boolean ok = budgetDAO.delete(id);
        if (ok) {
            auditService.log(actorId, "BUDGET_DELETED", "BUDGET", id,
                    "Xóa định biên năm " + b.getFiscalYear() + " của phòng ban " + b.getDepartmentName(),
                    ip, userAgent);
        }
        return ok;
    }

    // =========================================================================
    // SUBTASKS QUẢN LÝ (KN-195 -> KN-201)
    // =========================================================================

    public List<BudgetSubtask> getAllSubtasks() {
        return budgetDAO.getAllSubtasks();
    }

    public BudgetSubtask getSubtaskById(String id) {
        return budgetDAO.getSubtaskById(id);
    }

    public boolean updateSubtaskStatus(String id, String status, String actorId, String ip, String userAgent) {
        BudgetSubtask subtask = budgetDAO.getSubtaskById(id);
        if (subtask == null) return false;

        boolean ok = budgetDAO.updateSubtaskStatus(id, status);
        if (ok) {
            auditService.log(actorId, "BUDGET_SUBTASK_STATUS_UPDATED", "SUBTASK", id,
                    "Cập nhật trạng thái Subtask " + subtask.getCode() + " sang " + status,
                    ip, userAgent);
        }
        return ok;
    }

    public boolean updateSubtask(BudgetSubtask subtask, String actorId, String ip, String userAgent) {
        if (subtask == null || subtask.getId() == null) return false;
        boolean ok = budgetDAO.updateSubtask(subtask);
        if (ok) {
            auditService.log(actorId, "BUDGET_SUBTASK_UPDATED", "SUBTASK", subtask.getId(),
                    "Cập nhật thông tin Subtask " + subtask.getCode(),
                    ip, userAgent);
        }
        return ok;
    }

    /**
     * Tính tỷ lệ hoàn thành Subtasks (% Done)
     */
    public double getSubtasksCompletionRate() {
        List<BudgetSubtask> list = budgetDAO.getAllSubtasks();
        if (list.isEmpty()) return 0.0;
        int doneCount = 0;
        for (BudgetSubtask s : list) {
            if ("DONE".equalsIgnoreCase(s.getStatus())) {
                doneCount++;
            }
        }
        double rate = ((double) doneCount / list.size()) * 100.0;
        return Math.round(rate * 10.0) / 10.0;
    }

    // =========================================================================
    // NGHIỆP VỤ ĐỐI SOÁT ĐỊNH BIÊN KHI TẠO / SỬA YÊU CẦU TUYỂN DỤNG (KN-195, KN-196)
    // =========================================================================

    /**
     * DTO kết quả đối soát định biên và ngân sách (QuotaCheckResult)
     */
    public static class QuotaCheckResult {
        private boolean configured;
        private boolean headcountExceeded;
        private boolean budgetExceeded;
        private boolean exceeded;
        private int headcountExcess;
        private String message;
        private DepartmentHeadcountBudget budget;
        private BigDecimal proposedCost = BigDecimal.ZERO;

        public boolean isConfigured() { return configured; }
        public void setConfigured(boolean configured) { this.configured = configured; }

        public boolean isHeadcountExceeded() { return headcountExceeded; }
        public void setHeadcountExceeded(boolean headcountExceeded) { this.headcountExceeded = headcountExceeded; }

        public boolean isBudgetExceeded() { return budgetExceeded; }
        public void setBudgetExceeded(boolean budgetExceeded) { this.budgetExceeded = budgetExceeded; }

        public boolean isExceeded() { return exceeded; }
        public void setExceeded(boolean exceeded) { this.exceeded = exceeded; }

        public int getHeadcountExcess() { return headcountExcess; }
        public void setHeadcountExcess(int headcountExcess) { this.headcountExcess = headcountExcess; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public DepartmentHeadcountBudget getBudget() { return budget; }
        public void setBudget(DepartmentHeadcountBudget budget) { this.budget = budget; }

        public BigDecimal getProposedCost() { return proposedCost; }
        public void setProposedCost(BigDecimal proposedCost) { this.proposedCost = proposedCost; }
    }

    /**
     * Kiểm tra đối soát định biên và ngân sách theo thời gian thực (KN-195, KN-196)
     */
    public QuotaCheckResult checkRequisitionQuota(String departmentId, int fiscalYear, int requestedHeadcount,
                                                 BigDecimal proposedSalary, String excludeRequisitionId) {
        QuotaCheckResult result = new QuotaCheckResult();
        DepartmentHeadcountBudget budget = budgetDAO.getBudgetByDeptAndYear(departmentId, fiscalYear, excludeRequisitionId);
        result.setBudget(budget);

        if (budget == null) {
            result.setConfigured(false);
            result.setExceeded(true);
            result.setMessage("Phòng ban chưa được khai báo chỉ tiêu định biên & ngân sách năm " + fiscalYear + "!");
            return result;
        }

        result.setConfigured(true);
        boolean hcExceeded = requestedHeadcount > budget.getRemainingHeadcount();
        result.setHeadcountExceeded(hcExceeded);
        if (hcExceeded) {
            result.setHeadcountExcess(requestedHeadcount - budget.getRemainingHeadcount());
        } else {
            result.setHeadcountExcess(0);
        }

        BigDecimal proposedCost = BigDecimal.ZERO;
        if (proposedSalary != null && proposedSalary.compareTo(BigDecimal.ZERO) > 0 && requestedHeadcount > 0) {
            proposedCost = proposedSalary.multiply(BigDecimal.valueOf(requestedHeadcount));
        }
        result.setProposedCost(proposedCost);

        boolean bgExceeded = false;
        if (proposedCost.compareTo(BigDecimal.ZERO) > 0) {
            bgExceeded = proposedCost.compareTo(budget.getRemainingSalaryBudget()) > 0;
        }
        result.setBudgetExceeded(bgExceeded);

        boolean exceeded = hcExceeded || bgExceeded;
        result.setExceeded(exceeded);

        StringBuilder msg = new StringBuilder();
        if (hcExceeded) {
            msg.append("Số lượng tuyển đề xuất (").append(requestedHeadcount)
                    .append(") vượt quá số Headcount còn lại của phòng ban (")
                    .append(budget.getRemainingHeadcount()).append(" nhân sự). ");
        }
        if (bgExceeded) {
            msg.append("Tổng chi phí lương đề xuất vượt quá ngân sách còn lại của phòng ban năm ")
                    .append(fiscalYear).append(" (Còn lại: ")
                    .append(budget.getFormattedRemainingBudget()).append("). ");
        }
        if (exceeded) {
            msg.append("Vượt chỉ tiêu là cảnh báo chặn: Cần Trưởng phòng Nhân sự xác nhận ghi đè kèm lý do!");
        } else {
            msg.append("Trong hạn mức định biên và ngân sách hợp lệ.");
        }
        result.setMessage(msg.toString());
        return result;
    }

    /**
     * Tra cứu và kiểm tra vượt chỉ tiêu định biên & ngân sách cho màn hình Requisition (trả về Map cho Ajax / Servlet)
     */
    public Map<String, Object> checkRequisitionBudget(String departmentId, Integer fiscalYear, int requestedHeadcount,
                                                      BigDecimal proposedSalary, String requisitionId, User actor) {
        Map<String, Object> result = new HashMap<>();

        if (fiscalYear == null || fiscalYear <= 0) {
            fiscalYear = Calendar.getInstance().get(Calendar.YEAR);
        }

        boolean canOverride = actor != null && (actor.hasRole("ADMIN") || actor.hasRole("HR_MANAGER") || actor.hasPermission("budget.override"));
        result.put("canOverride", canOverride);
        result.put("fiscalYear", fiscalYear);
        result.put("departmentId", departmentId);

        QuotaCheckResult check = checkRequisitionQuota(departmentId, fiscalYear, requestedHeadcount, proposedSalary, requisitionId);
        result.put("hasBudget", check.isConfigured());
        result.put("isExceeded", check.isExceeded());
        result.put("isHeadcountExceeded", check.isHeadcountExceeded());
        result.put("isBudgetExceeded", check.isBudgetExceeded());
        result.put("proposedCost", check.getProposedCost());
        result.put("warningMessage", check.getMessage());

        DepartmentHeadcountBudget budget = check.getBudget();
        if (budget == null) {
            result.put("targetHeadcount", 0);
            result.put("usedHeadcount", 0);
            result.put("remainingHeadcount", 0);
            result.put("totalSalaryBudget", BigDecimal.ZERO);
            result.put("committedSalaryBudget", BigDecimal.ZERO);
            result.put("remainingSalaryBudget", BigDecimal.ZERO);
            result.put("message", check.getMessage());
            return result;
        }

        result.put("departmentName", budget.getDepartmentName());
        result.put("targetHeadcount", budget.getTargetHeadcount());
        result.put("usedHeadcount", budget.getUsedHeadcount());
        result.put("remainingHeadcount", budget.getRemainingHeadcount());
        result.put("totalSalaryBudget", budget.getTotalSalaryBudget());
        result.put("committedSalaryBudget", budget.getCommittedSalaryBudget());
        result.put("remainingSalaryBudget", budget.getRemainingSalaryBudget());
        result.put("formattedTotalBudget", budget.getFormattedTotalBudget());
        result.put("formattedCommittedBudget", budget.getFormattedCommittedBudget());
        result.put("formattedRemainingBudget", budget.getFormattedRemainingBudget());

        return result;
    }
}

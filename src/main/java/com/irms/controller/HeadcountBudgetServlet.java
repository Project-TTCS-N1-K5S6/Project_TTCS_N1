package com.irms.controller;

import com.irms.model.BudgetSubtask;
import com.irms.model.Department;
import com.irms.model.DepartmentHeadcountBudget;
import com.irms.model.User;
import com.irms.service.DepartmentService;
import com.irms.service.HeadcountBudgetService;
import com.irms.util.JsonUtil;
import com.irms.util.SecurityUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller quản lý Định biên & Ngân sách Nhân sự (KN-195 -> KN-201)
 */
@WebServlet(name = "HeadcountBudgetServlet", urlPatterns = {
        "/headcount-budgets",
        "/headcount-budgets/save",
        "/headcount-budgets/delete",
        "/headcount-budgets/subtasks/update-status",
        "/headcount-budgets/subtasks/update",
        "/headcount-budgets/api/dept-budget",
        "/headcount-budgets/api/check"
})
public class HeadcountBudgetServlet extends HttpServlet {

    private final HeadcountBudgetService budgetService = new HeadcountBudgetService();
    private final DepartmentService departmentService = new DepartmentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        // AJAX API tra cứu định biên phòng ban
        if ("/headcount-budgets/api/dept-budget".equals(path) || "/headcount-budgets/api/check".equals(path)) {
            handleDeptBudgetApi(request, response);
            return;
        }

        // Màn hình chính Quản lý Định biên & Ngân sách Nhân sự
        showBudgetManagementPage(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String path = request.getServletPath();

        // Cập nhật nhanh trạng thái Subtask qua AJAX
        if ("/headcount-budgets/subtasks/update-status".equals(path)) {
            handleSubtaskUpdateStatusAjax(request, response);
            return;
        }

        // Cập nhật thông tin chi tiết Subtask
        if ("/headcount-budgets/subtasks/update".equals(path)) {
            handleSubtaskUpdate(request, response);
            return;
        }

        // Lưu / Cập nhật định biên phòng ban
        if ("/headcount-budgets/save".equals(path)) {
            handleSaveBudget(request, response);
            return;
        }

        // Xóa định biên phòng ban
        if ("/headcount-budgets/delete".equals(path)) {
            handleDeleteBudget(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/headcount-budgets");
    }

    private void showBudgetManagementPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        // Bộ lọc năm tài chính (Mặc định năm hiện tại 2026)
        int currentYear = LocalDate.now().getYear();
        String yearParam = request.getParameter("year");
        int fiscalYear = currentYear;
        if (yearParam != null && !yearParam.trim().isEmpty()) {
            try {
                fiscalYear = Integer.parseInt(yearParam.trim());
            } catch (NumberFormatException ignored) {}
        }

        String departmentId = request.getParameter("departmentId");
        String keyword = request.getParameter("keyword");

        List<DepartmentHeadcountBudget> budgets = budgetService.getBudgets(fiscalYear, departmentId, keyword);
        Map<String, Object> kpis = budgetService.getCompanyKpiSummary(fiscalYear);
        List<BudgetSubtask> subtasks = budgetService.getAllSubtasks();
        double completionRate = budgetService.getSubtasksCompletionRate();
        List<Department> departments = departmentService.getAllDepartments();

        boolean canManage = (currentUser != null && (currentUser.hasRole("ADMIN") || currentUser.hasRole("HR_MANAGER") || currentUser.hasPermission("budget.manage")));
        boolean canOverride = (currentUser != null && (currentUser.hasRole("ADMIN") || currentUser.hasRole("HR_MANAGER") || currentUser.hasPermission("budget.override")));

        request.setAttribute("budgets", budgets);
        request.setAttribute("kpis", kpis);
        request.setAttribute("subtasks", subtasks);
        request.setAttribute("completionRate", completionRate);
        request.setAttribute("departments", departments);
        request.setAttribute("fiscalYear", fiscalYear);
        request.setAttribute("selectedDept", departmentId);
        request.setAttribute("keyword", keyword);
        request.setAttribute("canManage", canManage);
        request.setAttribute("canOverride", canOverride);
        request.setAttribute("activeMenu", "budget");

        request.getRequestDispatcher("/WEB-INF/views/budget/list.jsp").forward(request, response);
    }

    private void handleSaveBudget(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String actorId = (currentUser != null) ? currentUser.getId() : null;
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        // Kiểm tra quyền khai báo
        boolean canManage = (currentUser != null && (currentUser.hasRole("ADMIN") || currentUser.hasRole("HR_MANAGER") || currentUser.hasPermission("budget.manage")));
        if (!canManage) {
            if (session != null) session.setAttribute("flashError", "Bạn không có quyền khai báo hoặc cập nhật định biên & ngân sách!");
            response.sendRedirect(request.getContextPath() + "/headcount-budgets");
            return;
        }

        String id = request.getParameter("id");
        String departmentId = request.getParameter("departmentId");
        String yearStr = request.getParameter("fiscalYear");
        String headcountStr = request.getParameter("targetHeadcount");
        String budgetStr = request.getParameter("totalSalaryBudget");
        String description = request.getParameter("description");
        String status = request.getParameter("status");

        DepartmentHeadcountBudget item = new DepartmentHeadcountBudget();
        if (id != null && !id.trim().isEmpty()) {
            item.setId(id.trim());
        }
        item.setDepartmentId(departmentId);

        try {
            int year = (yearStr != null && !yearStr.trim().isEmpty()) ? Integer.parseInt(yearStr.trim()) : LocalDate.now().getYear();
            item.setFiscalYear(year);

            int targetHc = (headcountStr != null && !headcountStr.trim().isEmpty()) ? Integer.parseInt(headcountStr.trim()) : 0;
            item.setTargetHeadcount(targetHc);

            BigDecimal totalBudget = BigDecimal.ZERO;
            if (budgetStr != null && !budgetStr.trim().isEmpty()) {
                String cleanBudget = budgetStr.trim().replace(".", "").replace(",", "");
                totalBudget = new BigDecimal(cleanBudget);
            }
            item.setTotalSalaryBudget(totalBudget);
            item.setDescription(description);
            item.setStatus((status != null && !status.trim().isEmpty()) ? status.trim() : "ACTIVE");

            boolean ok = budgetService.saveBudget(item, actorId, ip, userAgent);
            if (ok) {
                if (session != null) session.setAttribute("flashSuccess", "Lưu định biên & ngân sách phòng ban thành công!");
            } else {
                if (session != null) session.setAttribute("flashError", "Không thể lưu định biên & ngân sách. Vui lòng thử lại!");
            }
            response.sendRedirect(request.getContextPath() + "/headcount-budgets?year=" + year);

        } catch (IllegalArgumentException e) {
            if (session != null) session.setAttribute("flashError", e.getMessage());
            response.sendRedirect(request.getContextPath() + "/headcount-budgets?year=" + item.getFiscalYear());
        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", "Lỗi xử lý hệ thống: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/headcount-budgets");
        }
    }

    private void handleDeleteBudget(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String actorId = (currentUser != null) ? currentUser.getId() : null;
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        boolean canManage = (currentUser != null && (currentUser.hasRole("ADMIN") || currentUser.hasRole("HR_MANAGER") || currentUser.hasPermission("budget.manage")));
        if (!canManage) {
            if (session != null) session.setAttribute("flashError", "Bạn không có quyền xóa chỉ tiêu định biên & ngân sách!");
            response.sendRedirect(request.getContextPath() + "/headcount-budgets");
            return;
        }

        String id = request.getParameter("id");
        if (id != null && !id.trim().isEmpty()) {
            boolean ok = budgetService.deleteBudget(id.trim(), actorId, ip, userAgent);
            if (ok) {
                if (session != null) session.setAttribute("flashSuccess", "Đã xóa bản ghi định biên phòng ban!");
            } else {
                if (session != null) session.setAttribute("flashError", "Không thể xóa bản ghi định biên.");
            }
        }
        response.sendRedirect(request.getContextPath() + "/headcount-budgets");
    }

    private void handleSubtaskUpdateStatusAjax(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String actorId = (currentUser != null) ? currentUser.getId() : null;
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        String id = request.getParameter("id");
        String status = request.getParameter("status");

        Map<String, Object> res = new HashMap<>();
        if (id == null || status == null) {
            res.put("success", false);
            res.put("message", "Thiếu tham số ID hoặc trạng thái công việc!");
            JsonUtil.sendJsonResponse(response, 400, res);
            return;
        }

        boolean ok = budgetService.updateSubtaskStatus(id.trim(), status.trim().toUpperCase(), actorId, ip, userAgent);
        if (ok) {
            double rate = budgetService.getSubtasksCompletionRate();
            res.put("success", true);
            res.put("completionRate", rate);
            res.put("status", status.trim().toUpperCase());
            res.put("message", "Cập nhật trạng thái công việc thành công!");
            JsonUtil.sendJsonResponse(response, 200, res);
        } else {
            res.put("success", false);
            res.put("message", "Không thể cập nhật trạng thái công việc.");
            JsonUtil.sendJsonResponse(response, 500, res);
        }
    }

    private void handleSubtaskUpdate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String actorId = (currentUser != null) ? currentUser.getId() : null;
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        String id = request.getParameter("id");
        String priority = request.getParameter("priority");
        String assigneeId = request.getParameter("assigneeId");
        String assigneeName = request.getParameter("assigneeName");
        String status = request.getParameter("status");

        if (id != null && !id.trim().isEmpty()) {
            BudgetSubtask subtask = budgetService.getSubtaskById(id.trim());
            if (subtask != null) {
                if (priority != null && !priority.trim().isEmpty()) subtask.setPriority(priority.trim().toUpperCase());
                subtask.setAssigneeId(assigneeId);
                subtask.setAssigneeName((assigneeName != null && !assigneeName.trim().isEmpty()) ? assigneeName.trim() : "U");
                if (status != null && !status.trim().isEmpty()) subtask.setStatus(status.trim().toUpperCase());

                budgetService.updateSubtask(subtask, actorId, ip, userAgent);
                if (session != null) session.setAttribute("flashSuccess", "Cập nhật công việc " + subtask.getCode() + " thành công!");
            }
        }
        response.sendRedirect(request.getContextPath() + "/headcount-budgets");
    }

    private void handleDeptBudgetApi(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String departmentId = request.getParameter("departmentId");
        String yearStr = request.getParameter("year");
        String headcountStr = request.getParameter("headcount");
        String salaryStr = request.getParameter("salary");
        String requisitionId = request.getParameter("requisitionId");

        int year = LocalDate.now().getYear();
        try {
            if (yearStr != null && !yearStr.trim().isEmpty()) year = Integer.parseInt(yearStr.trim());
        } catch (Exception ignored) {}

        int hc = 1;
        try {
            if (headcountStr != null && !headcountStr.trim().isEmpty()) hc = Integer.parseInt(headcountStr.trim());
        } catch (Exception ignored) {}

        BigDecimal salary = BigDecimal.ZERO;
        try {
            if (salaryStr != null && !salaryStr.trim().isEmpty()) {
                salary = new BigDecimal(salaryStr.trim().replace(".", "").replace(",", ""));
            }
        } catch (Exception ignored) {}

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        Map<String, Object> result = budgetService.checkRequisitionBudget(departmentId, year, hc, salary, requisitionId, currentUser);
        JsonUtil.sendJsonResponse(response, 200, result);
    }
}

package com.irms.controller;

import com.irms.dao.DepartmentDAO;
import com.irms.dao.RoleDAO;
import com.irms.model.*;
import com.irms.service.ApprovalMatrixService;
import com.irms.service.DepartmentService;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller Quản trị Ma trận phê duyệt (Approval Matrix Management)
 * Xử lý danh sách, cấu hình đa cấp, kiểm tra điều kiện chuyển cấp và kiểm thử tính toàn vẹn.
 */
@WebServlet(name = "ApprovalMatrixServlet", urlPatterns = {
        "/admin/approval-matrix",
        "/admin/approval-matrix/create",
        "/admin/approval-matrix/edit",
        "/admin/approval-matrix/save",
        "/admin/approval-matrix/status",
        "/admin/approval-matrix/delete",
        "/admin/approval-matrix/detail",
        "/admin/approval-matrix/api/preview"
})
public class ApprovalMatrixServlet extends HttpServlet {

    private final ApprovalMatrixService matrixService = new ApprovalMatrixService();
    private final DepartmentService departmentService = new DepartmentService();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final RoleDAO roleDAO = new RoleDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        // 1. API xem trước chuỗi phê duyệt theo phòng ban và mức lương (Live Preview)
        if ("/admin/approval-matrix/api/preview".equals(path)) {
            handleApiPreview(request, response);
            return;
        }

        // 2. API lấy chi tiết cấu hình dạng JSON
        if ("/admin/approval-matrix/detail".equals(path)) {
            handleDetailJson(request, response);
            return;
        }

        // 3. Màn hình tạo mới ma trận phê duyệt
        if ("/admin/approval-matrix/create".equals(path)) {
            showCreateForm(request, response);
            return;
        }

        // 4. Màn hình chỉnh sửa ma trận phê duyệt
        if ("/admin/approval-matrix/edit".equals(path)) {
            showEditForm(request, response);
            return;
        }

        // 5. Màn hình danh sách ma trận phê duyệt
        showList(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        if ("/admin/approval-matrix/save".equals(path)) {
            handleSaveMatrix(request, response);
            return;
        }

        if ("/admin/approval-matrix/status".equals(path)) {
            handleToggleStatus(request, response);
            return;
        }

        if ("/admin/approval-matrix/delete".equals(path)) {
            handleDeleteMatrix(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/admin/approval-matrix");
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String search = request.getParameter("search");
        String departmentId = request.getParameter("departmentId");
        String status = request.getParameter("status");

        int page = 1;
        int pageSize = 10;
        try {
            if (request.getParameter("page") != null) {
                page = Integer.parseInt(request.getParameter("page"));
            }
        } catch (NumberFormatException ignored) {}

        List<ApprovalMatrix> list = matrixService.getMatrices(search, departmentId, status, page, pageSize);
        int total = matrixService.countMatrices(search, departmentId, status);
        int totalPages = (int) Math.ceil((double) total / pageSize);

        List<Department> departments = departmentService.getAllDepartments();

        request.setAttribute("matrices", list);
        request.setAttribute("departments", departments);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalCount", total);
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("paramSearch", search);
        request.setAttribute("paramDept", departmentId);
        request.setAttribute("paramStatus", status);

        request.getRequestDispatcher("/WEB-INF/views/approval/list.jsp").forward(request, response);
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ApprovalMatrix matrix = new ApprovalMatrix();
        matrix.setStatus("ACTIVE");
        matrix.setSalaryCondition("RANGE");

        // Khởi tạo sẵn 2 cấp mẫu để người dùng dễ hình dung
        List<ApprovalMatrixStep> defaultSteps = new ArrayList<>();
        defaultSteps.add(new ApprovalMatrixStep(1, "Trưởng bộ phận chuyên môn", "DEPT_MANAGER", null, null, false, null));
        defaultSteps.add(new ApprovalMatrixStep(2, "Ban Giám đốc Khối", "ROLE", "role-001", null, true, "Tự động bổ sung khi mức lương đề xuất vượt hạn mức thông thường"));
        matrix.setSteps(defaultSteps);

        prepareFormAttributes(request, matrix, false);
        request.getRequestDispatcher("/WEB-INF/views/approval/form.jsp").forward(request, response);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/admin/approval-matrix");
            return;
        }

        ApprovalMatrix matrix = matrixService.getMatrixById(id);
        if (matrix == null) {
            HttpSession session = request.getSession();
            session.setAttribute("flashError", "Không tìm thấy cấu hình ma trận phê duyệt cần sửa!");
            response.sendRedirect(request.getContextPath() + "/admin/approval-matrix");
            return;
        }

        prepareFormAttributes(request, matrix, true);
        request.getRequestDispatcher("/WEB-INF/views/approval/form.jsp").forward(request, response);
    }

    private void prepareFormAttributes(HttpServletRequest request, ApprovalMatrix matrix, boolean isEdit) {
        request.setAttribute("matrix", matrix);
        request.setAttribute("isEdit", isEdit);
        request.setAttribute("departments", departmentService.getAllDepartments());
        request.setAttribute("roles", roleDAO.findAll());
        request.setAttribute("potentialUsers", departmentDAO.findPotentialManagers());
    }

    private void handleSaveMatrix(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String actorId = (currentUser != null) ? currentUser.getId() : null;
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        String id = request.getParameter("id");
        boolean isEdit = (id != null && !id.trim().isEmpty());

        ApprovalMatrix matrix = new ApprovalMatrix();
        if (isEdit) {
            matrix.setId(id.trim());
        }

        try {
            matrix.setCode(request.getParameter("code"));
            matrix.setName(request.getParameter("name"));
            matrix.setDescription(request.getParameter("description"));

            String deptId = request.getParameter("departmentId");
            matrix.setDepartmentId((deptId != null && !deptId.trim().isEmpty() && !"GLOBAL".equalsIgnoreCase(deptId)) ? deptId.trim() : null);

            matrix.setSalaryCondition(request.getParameter("salaryCondition"));

            // Parse số tiền lương
            String minSalStr = request.getParameter("minSalary");
            String maxSalStr = request.getParameter("maxSalary");
            String thresholdStr = request.getParameter("thresholdAmount");

            matrix.setMinSalary(parseMoney(minSalStr));
            matrix.setMaxSalary(parseMoney(maxSalStr));
            matrix.setThresholdAmount(parseMoney(thresholdStr));

            String priorityStr = request.getParameter("priority");
            matrix.setPriority((priorityStr != null && !priorityStr.trim().isEmpty()) ? Integer.parseInt(priorityStr.trim()) : 0);

            matrix.setStatus(request.getParameter("status"));

            // Parse danh sách các cấp phê duyệt động
            List<ApprovalMatrixStep> steps = new ArrayList<>();
            String[] stepNames = request.getParameterValues("stepName[]");
            String[] approverTypes = request.getParameterValues("approverType[]");
            String[] roleIds = request.getParameterValues("approverRoleId[]");
            String[] userIds = request.getParameterValues("approverUserId[]");
            String[] escalationFlags = request.getParameterValues("isEscalationStep[]");
            String[] escalationConditions = request.getParameterValues("escalationCondition[]");

            if (stepNames != null) {
                for (int i = 0; i < stepNames.length; i++) {
                    if (stepNames[i] == null || stepNames[i].trim().isEmpty()) continue;
                    ApprovalMatrixStep s = new ApprovalMatrixStep();
                    s.setStepOrder(i + 1);
                    s.setStepName(stepNames[i].trim());
                    s.setApproverType(approverTypes != null && i < approverTypes.length ? approverTypes[i] : "DEPT_MANAGER");
                    s.setApproverRoleId(roleIds != null && i < roleIds.length ? roleIds[i] : null);
                    s.setApproverUserId(userIds != null && i < userIds.length ? userIds[i] : null);

                    boolean isEsc = (escalationFlags != null && i < escalationFlags.length && "true".equalsIgnoreCase(escalationFlags[i]));
                    s.setEscalationStep(isEsc);
                    s.setEscalationCondition(escalationConditions != null && i < escalationConditions.length ? escalationConditions[i] : null);
                    steps.add(s);
                }
            }
            matrix.setSteps(steps);

            if (isEdit) {
                matrixService.updateMatrix(matrix, actorId, ip, userAgent);
            } else {
                matrixService.createMatrix(matrix, actorId, ip, userAgent);
            }

            if (session != null) {
                session.setAttribute("flashSuccess", (isEdit ? "Cập nhật" : "Tạo mới") + " Ma trận phê duyệt thành công!");
            }
            response.sendRedirect(request.getContextPath() + "/admin/approval-matrix");

        } catch (IllegalArgumentException e) {
            // Giữ lại dữ liệu người dùng đã nhập để sửa, không làm mất form (Yêu cầu 3.3)
            if (session != null) session.setAttribute("flashError", e.getMessage());
            prepareFormAttributes(request, matrix, isEdit);
            request.getRequestDispatcher("/WEB-INF/views/approval/form.jsp").forward(request, response);

        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", "Lỗi xử lý hệ thống: " + e.getMessage());
            prepareFormAttributes(request, matrix, isEdit);
            request.getRequestDispatcher("/WEB-INF/views/approval/form.jsp").forward(request, response);
        }
    }

    private void handleToggleStatus(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String actorId = (currentUser != null) ? currentUser.getId() : null;
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        String id = request.getParameter("id");
        try {
            matrixService.toggleMatrixStatus(id, actorId, ip, userAgent);
            if (session != null) session.setAttribute("flashSuccess", "Đã cập nhật trạng thái ma trận phê duyệt!");
        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/admin/approval-matrix");
    }

    private void handleDeleteMatrix(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String actorId = (currentUser != null) ? currentUser.getId() : null;
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        String id = request.getParameter("id");
        try {
            matrixService.deleteMatrix(id, actorId, ip, userAgent);
            if (session != null) session.setAttribute("flashSuccess", "Đã xóa ma trận phê duyệt thành công!");
        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/admin/approval-matrix");
    }

    private void handleDetailJson(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String id = request.getParameter("id");
        ApprovalMatrix matrix = matrixService.getMatrixById(id);
        if (matrix != null) {
            JsonUtil.sendJsonResponse(response, 200, matrix);
        } else {
            JsonUtil.sendJsonResponse(response, 404, Collections.singletonMap("error", "Không tìm thấy cấu hình"));
        }
    }

    private void handleApiPreview(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String deptId = request.getParameter("departmentId");
        String posTitle = request.getParameter("positionTitle");
        String salaryStr = request.getParameter("proposedSalary");
        BigDecimal salary = parseMoney(salaryStr);

        RecruitmentRequest tempReq = new RecruitmentRequest();
        tempReq.setDepartmentId((deptId != null && !deptId.isEmpty() && !"GLOBAL".equalsIgnoreCase(deptId)) ? deptId : null);
        tempReq.setPositionTitle(posTitle != null ? posTitle : "Chức danh mô phỏng");
        tempReq.setMaxSalary(salary);
        tempReq.setMinSalary(salary);

        try {
            List<RequisitionApprovalStep> steps = matrixService.resolveApprovalSteps(tempReq);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("steps", steps);
            result.put("totalSteps", steps.size());
            JsonUtil.sendJsonResponse(response, 200, result);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", e.getMessage());
            JsonUtil.sendJsonResponse(response, 400, err);
        }
    }

    private BigDecimal parseMoney(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        try {
            String clean = str.trim().replace(".", "").replace(",", "").replace("đ", "").replace("VND", "").trim();
            return new BigDecimal(clean);
        } catch (Exception e) {
            return null;
        }
    }
}

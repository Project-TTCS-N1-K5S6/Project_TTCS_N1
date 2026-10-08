package com.irms.controller;

import com.irms.model.Department;
import com.irms.model.User;
import com.irms.service.DepartmentService;
import com.irms.service.DepartmentService.DeleteCheckResult;
import com.irms.util.JsonUtil;
import com.irms.util.SecurityUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller quản lý danh mục phòng ban và cơ cấu tổ chức phân cấp cây
 */
@WebServlet(name = "DepartmentServlet", urlPatterns = {
        "/admin/departments",
        "/admin/departments/create",
        "/admin/departments/edit",
        "/admin/departments/status",
        "/admin/departments/delete",
        "/admin/departments/api/check-delete",
        "/admin/departments/api/parents",
        "/admin/departments/api/detail"
})
public class DepartmentServlet extends HttpServlet {
    private final DepartmentService departmentService = new DepartmentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        String action = request.getParameter("action");

        // 1. AJAX API: Kiểm tra điều kiện xóa phòng ban
        if ("/admin/departments/api/check-delete".equals(path) || "check-delete".equalsIgnoreCase(action)) {
            handleCheckDeleteApi(request, response);
            return;
        }

        // 2. AJAX API: Lấy danh sách cha hợp lệ (đã loại trừ node hiện tại và con cháu của nó)
        if ("/admin/departments/api/parents".equals(path) || "parents".equalsIgnoreCase(action)) {
            handleParentsApi(request, response);
            return;
        }

        // 3. AJAX API: Lấy chi tiết phòng ban theo ID
        if ("/admin/departments/api/detail".equals(path) || "detail".equalsIgnoreCase(action)) {
            handleDetailApi(request, response);
            return;
        }

        // 4. Màn hình chính: Danh sách cơ cấu phòng ban dạng cây
        String search = request.getParameter("search");
        String status = request.getParameter("status");

        List<Department> tree = departmentService.getDepartmentTree(search, status);
        List<Department> flatTree = departmentService.getFlatTreeDepartments(search, status);
        List<Department> allFlat = departmentService.getFlatDepartmentsWithIndentation(null);
        List<User> managers = departmentService.getPotentialManagers();

        int totalCount = allFlat.size();
        long activeCount = allFlat.stream().filter(Department::isActive).count();
        long inactiveCount = allFlat.stream().filter(Department::isInactive).count();
        int maxLevel = allFlat.stream().mapToInt(Department::getLevel).max().orElse(0) + 1;

        request.setAttribute("departmentTree", tree);
        request.setAttribute("departments", flatTree); // Hiển thị trên bảng cây theo thứ tự DFS
        request.setAttribute("parentOptions", allFlat);
        request.setAttribute("managers", managers);
        request.setAttribute("totalCount", totalCount);
        request.setAttribute("activeCount", activeCount);
        request.setAttribute("inactiveCount", inactiveCount);
        request.setAttribute("maxLevel", maxLevel);
        request.setAttribute("paramSearch", search);
        request.setAttribute("paramStatus", status);

        request.getRequestDispatcher("/WEB-INF/views/departments/list.jsp").forward(request, response);
    }

    private void handleCheckDeleteApi(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("canDelete", false);
            err.put("message", "ID phòng ban không hợp lệ!");
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, err);
            return;
        }

        DeleteCheckResult res = departmentService.checkCanDelete(id);
        Map<String, Object> data = new HashMap<>();
        data.put("departmentId", id);
        data.put("canDelete", res.canDelete);
        data.put("hasOpenRequisitions", res.hasOpenRequisitions);
        data.put("hasChildren", res.hasChildren);
        data.put("hasUsers", res.hasUsers);
        data.put("hasSalaryRanges", res.hasSalaryRanges);
        data.put("hasHistoryRequisitions", res.hasHistoryRequisitions);
        data.put("message", res.message);
        JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, data);
    }

    private void handleParentsApi(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String excludeId = request.getParameter("excludeId");
        List<Department> flatParents = departmentService.getFlatDepartmentsWithIndentation(excludeId);
        JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, flatParents);
    }

    private void handleDetailApi(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String id = request.getParameter("id");
        Department dept = departmentService.getDepartmentById(id);
        if (dept == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Không tìm thấy phòng ban!");
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_NOT_FOUND, err);
            return;
        }
        JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, dept);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String userId = (currentUser != null) ? currentUser.getId() : "system";
        String clientIp = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            switch (path) {
                case "/admin/departments/create": {
                    Department dept = new Department();
                    dept.setCode(request.getParameter("code"));
                    dept.setName(request.getParameter("name"));
                    dept.setDescription(request.getParameter("description"));
                    dept.setParentId(request.getParameter("parentId"));
                    dept.setManagerId(request.getParameter("managerId"));
                    dept.setStatus(request.getParameter("status"));
                    departmentService.createDepartment(dept, userId, clientIp, userAgent);
                    if (session != null) {
                        session.setAttribute("flashSuccess", "Thêm mới phòng ban thành công!");
                    }
                    break;
                }
                case "/admin/departments/edit": {
                    Department dept = new Department();
                    dept.setId(request.getParameter("id"));
                    dept.setCode(request.getParameter("code"));
                    dept.setName(request.getParameter("name"));
                    dept.setDescription(request.getParameter("description"));
                    dept.setParentId(request.getParameter("parentId"));
                    dept.setManagerId(request.getParameter("managerId"));
                    dept.setStatus(request.getParameter("status"));
                    departmentService.updateDepartment(dept, userId, clientIp, userAgent);
                    if (session != null) {
                        session.setAttribute("flashSuccess", "Cập nhật thông tin phòng ban thành công!");
                    }
                    break;
                }
                case "/admin/departments/status": {
                    String id = request.getParameter("id");
                    String status = request.getParameter("status");
                    departmentService.changeStatus(id, status, userId, clientIp, userAgent);
                    if (session != null) {
                        String label = Department.STATUS_ACTIVE.equalsIgnoreCase(status) ? "kích hoạt" : "ngừng áp dụng";
                        session.setAttribute("flashSuccess", "Đã " + label + " phòng ban thành công!");
                    }
                    break;
                }
                case "/admin/departments/delete": {
                    String id = request.getParameter("id");
                    departmentService.deleteDepartment(id, userId, clientIp, userAgent);
                    if (session != null) {
                        session.setAttribute("flashSuccess", "Đã xóa phòng ban thành công!");
                    }
                    break;
                }
            }
        } catch (IllegalStateException e) {
            if (session != null) {
                session.setAttribute("flashError", e.getMessage());
                session.setAttribute("flashOfferDeactivateId", request.getParameter("id"));
            }
        } catch (Exception e) {
            if (session != null) {
                session.setAttribute("flashError", "Lỗi: " + e.getMessage());
            }
        }

        response.sendRedirect(request.getContextPath() + "/admin/departments");
    }
}

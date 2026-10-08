package com.irms.controller;

import com.irms.model.Department;
import com.irms.model.RecruitmentRequest;
import com.irms.model.SalaryRange;
import com.irms.model.User;
import com.irms.service.DepartmentService;
import com.irms.service.RecruitmentRequestService;
import com.irms.service.SalaryRangeService;
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
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller quản lý luồng Yêu cầu tuyển dụng (Job Requisitions)
 * Hỗ trợ tạo mới, lưu nháp, gửi duyệt, kiểm soát dải lương chuẩn và ngày cần người.
 */
@WebServlet(name = "RecruitmentRequestServlet", urlPatterns = {
        "/recruitment-requests",
        "/recruitment-requests/create",
        "/recruitment-requests/edit",
        "/recruitment-requests/save",
        "/recruitment-requests/api/salary-benchmark"
})
public class RecruitmentRequestServlet extends HttpServlet {

    private final RecruitmentRequestService requisitionService = new RecruitmentRequestService();
    private final DepartmentService departmentService = new DepartmentService();
    private final SalaryRangeService salaryRangeService = new SalaryRangeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        // 1. AJAX API: Lấy dải chuẩn theo vị trí & phòng ban để phản hồi tức thời
        if ("/recruitment-requests/api/salary-benchmark".equals(path)) {
            handleSalaryBenchmarkApi(request, response);
            return;
        }

        // 2. Màn hình Tạo mới yêu cầu tuyển dụng
        if ("/recruitment-requests/create".equals(path)) {
            showCreateForm(request, response);
            return;
        }

        // 3. Màn hình Chỉnh sửa bản nháp / yêu cầu tuyển dụng
        if ("/recruitment-requests/edit".equals(path)) {
            showEditForm(request, response);
            return;
        }

        // 4. Màn hình Danh sách yêu cầu tuyển dụng
        showList(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        if ("/recruitment-requests/save".equals(path)) {
            handleSaveRequisition(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/recruitment-requests");
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String search = request.getParameter("search");
        String status = request.getParameter("status");
        String departmentId = request.getParameter("departmentId");

        List<RecruitmentRequest> list = requisitionService.getAllRequests(search, status, departmentId);
        List<Department> departments = departmentService.getAllDepartments();

        request.setAttribute("requisitions", list);
        request.setAttribute("departments", departments);
        request.setAttribute("paramSearch", search);
        request.setAttribute("paramStatus", status);
        request.setAttribute("paramDept", departmentId);

        request.getRequestDispatcher("/WEB-INF/views/recruitment/list.jsp").forward(request, response);
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Department> departments = departmentService.getAllDepartments();
        List<SalaryRange> salaryRanges = salaryRangeService.getAllSalaryRanges();

        request.setAttribute("departments", departments);
        request.setAttribute("salaryRanges", salaryRanges);
        request.setAttribute("nextCode", requisitionService.getNextCode());
        request.setAttribute("todayDate", LocalDate.now().toString());
        request.setAttribute("isEdit", false);

        request.getRequestDispatcher("/WEB-INF/views/recruitment/form.jsp").forward(request, response);
    }

    private void showEditForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/recruitment-requests");
            return;
        }

        RecruitmentRequest reqItem = requisitionService.getRequestById(id);
        if (reqItem == null) {
            HttpSession session = request.getSession();
            session.setAttribute("flashError", "Không tìm thấy yêu cầu tuyển dụng cần chỉnh sửa!");
            response.sendRedirect(request.getContextPath() + "/recruitment-requests");
            return;
        }

        List<Department> departments = departmentService.getAllDepartments();
        List<SalaryRange> salaryRanges = salaryRangeService.getAllSalaryRanges();

        // Lấy dải lương chuẩn hiện tại
        SalaryRange standard = requisitionService.getStandardSalaryRange(reqItem.getDepartmentId(), reqItem.getPositionTitle());

        request.setAttribute("reqItem", reqItem);
        request.setAttribute("standardRange", standard);
        request.setAttribute("departments", departments);
        request.setAttribute("salaryRanges", salaryRanges);
        request.setAttribute("todayDate", LocalDate.now().toString());
        request.setAttribute("isEdit", true);

        request.getRequestDispatcher("/WEB-INF/views/recruitment/form.jsp").forward(request, response);
    }

    private void handleSaveRequisition(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String actorId = (currentUser != null) ? currentUser.getId() : null;
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        String action = request.getParameter("action");
        boolean isDraft = "draft".equalsIgnoreCase(action);

        String id = request.getParameter("id");
        boolean isEdit = (id != null && !id.trim().isEmpty());

        RecruitmentRequest req = isEdit ? requisitionService.getRequestById(id) : new RecruitmentRequest();
        if (req == null) {
            req = new RecruitmentRequest();
            isEdit = false;
        }

        try {
            req.setCode(request.getParameter("code"));
            req.setTitle(request.getParameter("title"));
            req.setPositionTitle(request.getParameter("positionTitle"));
            req.setDepartmentId(request.getParameter("departmentId"));

            String headcountStr = request.getParameter("headcount");
            if (headcountStr != null && !headcountStr.trim().isEmpty()) {
                req.setHeadcount(Integer.parseInt(headcountStr.trim()));
            } else if (!isDraft) {
                req.setHeadcount(1);
            }

            req.setRecruitmentReason(request.getParameter("recruitmentReason"));

            // Parse dải lương đề xuất
            String minSalStr = request.getParameter("minSalary");
            String maxSalStr = request.getParameter("maxSalary");
            if (minSalStr != null && !minSalStr.trim().isEmpty()) {
                req.setMinSalary(new BigDecimal(minSalStr.trim().replace(".", "").replace(",", "")));
            } else {
                req.setMinSalary(null);
            }
            if (maxSalStr != null && !maxSalStr.trim().isEmpty()) {
                req.setMaxSalary(new BigDecimal(maxSalStr.trim().replace(".", "").replace(",", "")));
            } else {
                req.setMaxSalary(null);
            }

            // Parse ngày cần người
            String deadlineStr = request.getParameter("deadline");
            if (deadlineStr != null && !deadlineStr.trim().isEmpty()) {
                req.setDeadline(Date.valueOf(deadlineStr.trim()));
            } else {
                req.setDeadline(null);
            }

            req.setJobDescription(request.getParameter("jobDescription"));
            req.setJobRequirements(request.getParameter("jobRequirements"));
            req.setSalaryExplanation(request.getParameter("salaryExplanation"));

            if (isEdit) {
                requisitionService.updateRequest(req, isDraft, actorId, ip, userAgent);
            } else {
                requisitionService.createRequest(req, isDraft, actorId, ip, userAgent);
            }

            String msg = isDraft ? "Đã lưu nháp yêu cầu tuyển dụng thành công!" : "Yêu cầu tuyển dụng đã được gửi duyệt thành công!";
            if (session != null) session.setAttribute("flashSuccess", msg);
            response.sendRedirect(request.getContextPath() + "/recruitment-requests");

        } catch (IllegalArgumentException e) {
            // Khi có lỗi nghiệp vụ (ví dụ ngày ở quá khứ, lương ngoài chuẩn thiếu giải trình...)
            if (session != null) session.setAttribute("flashError", e.getMessage());

            // Đưa lại dữ liệu vào request để người dùng không phải nhập lại
            request.setAttribute("reqItem", req);
            request.setAttribute("departments", departmentService.getAllDepartments());
            request.setAttribute("salaryRanges", salaryRangeService.getAllSalaryRanges());
            request.setAttribute("todayDate", LocalDate.now().toString());
            request.setAttribute("isEdit", isEdit);
            request.getRequestDispatcher("/WEB-INF/views/recruitment/form.jsp").forward(request, response);

        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", "Lỗi xử lý hệ thống: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/recruitment-requests");
        }
    }

    private void handleSalaryBenchmarkApi(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String departmentId = request.getParameter("departmentId");
        String positionTitle = request.getParameter("positionTitle");
        String minStr = request.getParameter("minSalary");
        String maxStr = request.getParameter("maxSalary");

        BigDecimal minSalary = null;
        BigDecimal maxSalary = null;
        try {
            if (minStr != null && !minStr.trim().isEmpty()) {
                minSalary = new BigDecimal(minStr.trim().replace(".", "").replace(",", ""));
            }
            if (maxStr != null && !maxStr.trim().isEmpty()) {
                maxSalary = new BigDecimal(maxStr.trim().replace(".", "").replace(",", ""));
            }
        } catch (Exception ignored) {}

        SalaryRange standard = requisitionService.getStandardSalaryRange(departmentId, positionTitle);

        Map<String, Object> result = new HashMap<>();
        if (standard != null) {
            result.put("hasStandard", true);
            result.put("positionTitle", standard.getPositionTitle());
            result.put("standardMin", standard.getMinSalary());
            result.put("standardMax", standard.getMaxSalary());
            result.put("formattedRange", standard.getFormattedRange());
            result.put("currency", standard.getCurrency());

            boolean isOutOfRange = false;
            StringBuilder warning = new StringBuilder();

            if (minSalary != null && standard.getMinSalary() != null && minSalary.compareTo(standard.getMinSalary()) < 0) {
                isOutOfRange = true;
                warning.append("Lương tối thiểu đề xuất thấp hơn mức sàn chuẩn. ");
            }
            if (maxSalary != null && standard.getMaxSalary() != null && maxSalary.compareTo(standard.getMaxSalary()) > 0) {
                isOutOfRange = true;
                warning.append("Lương tối đa đề xuất vượt mức trần chuẩn. ");
            }

            result.put("isOutOfRange", isOutOfRange);
            result.put("warningMessage", warning.toString());
        } else {
            result.put("hasStandard", false);
            result.put("isOutOfRange", false);
            result.put("warningMessage", "Chức danh này chưa thiết lập dải lương chuẩn trong danh mục.");
        }

        JsonUtil.sendJsonResponse(response, 200, result);
    }
}

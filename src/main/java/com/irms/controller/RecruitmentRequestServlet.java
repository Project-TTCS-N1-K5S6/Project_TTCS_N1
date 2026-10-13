package com.irms.controller;

import com.irms.model.Department;
import com.irms.model.RecruitmentRequest;
import com.irms.model.RequisitionApprovalStep;
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
        "/recruitment-requests/close",
        "/recruitment-requests/check-pipeline",
        "/recruitment-requests/api/salary-benchmark",
        "/recruitment-requests/api/approval-steps",
        "/recruitment-requests/approve-step",
        "/recruitment-requests/reject-step",
        "/recruitment-requests/api/approval-chain",
        "/recruitment-requests/api/approve"
})
public class RecruitmentRequestServlet extends HttpServlet {

    private final RecruitmentRequestService requisitionService = new RecruitmentRequestService();
    private final DepartmentService departmentService = new DepartmentService();
    private final SalaryRangeService salaryRangeService = new SalaryRangeService();
    private final com.irms.service.RecruitmentApprovalService approvalService = new com.irms.service.RecruitmentApprovalService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        // 1. AJAX API: Kiểm tra pipeline ứng viên và tin tuyển dụng trước khi đóng
        if ("/recruitment-requests/check-pipeline".equals(path)) {
            handleCheckPipeline(request, response);
            return;
        }

        // AJAX API: Lấy chuỗi cấp duyệt và trạng thái hồ sơ (KN-158)
        if ("/recruitment-requests/api/approval-chain".equals(path)) {
            handleApprovalChainApi(request, response);
            return;
        }

        // 2. AJAX API: Lấy dải chuẩn theo vị trí & phòng ban để phản hồi tức thời
        if ("/recruitment-requests/api/salary-benchmark".equals(path)) {
            handleSalaryBenchmarkApi(request, response);
            return;
        }

        // 3. AJAX API: Lấy danh sách các cấp phê duyệt đã đóng băng của yêu cầu
        if ("/recruitment-requests/api/approval-steps".equals(path)) {
            handleApprovalStepsApi(request, response);
            return;
        }

        // 4. Màn hình Tạo mới yêu cầu tuyển dụng
        if ("/recruitment-requests/create".equals(path)) {
            showCreateForm(request, response);
            return;
        }

        // 4. Màn hình Chỉnh sửa bản nháp / yêu cầu tuyển dụng
        if ("/recruitment-requests/edit".equals(path)) {
            showEditForm(request, response);
            return;
        }

        // 5. Màn hình Danh sách yêu cầu tuyển dụng
        showList(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        // AJAX API: Ghi nhận kết quả phê duyệt theo từng cấp (KN-154)
        if ("/recruitment-requests/api/approve".equals(path)) {
            handleApprovalDecisionApi(request, response);
            return;
        }

        if ("/recruitment-requests/save".equals(path)) {
            handleSaveRequisition(request, response);
            return;
        }

        if ("/recruitment-requests/close".equals(path)) {
            handleCloseRequisition(request, response);
            return;
        }

        if ("/recruitment-requests/approve-step".equals(path)) {
            handleApproveStep(request, response);
            return;
        }

        if ("/recruitment-requests/reject-step".equals(path)) {
            handleRejectStep(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/recruitment-requests");
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String search = request.getParameter("search");
        String status = request.getParameter("status");
        String departmentId = request.getParameter("departmentId");

        int page = 1;
        int pageSize = 10;
        try {
            if (request.getParameter("page") != null) {
                page = Integer.parseInt(request.getParameter("page"));
            }
        } catch (NumberFormatException ignored) {}

        List<RecruitmentRequest> list = requisitionService.getRequests(search, status, departmentId, page, pageSize);
        int totalRequests = requisitionService.countRequests(search, status, departmentId);
        int totalPages = (int) Math.ceil((double) totalRequests / pageSize);
        List<Department> departments = departmentService.getAllDepartments();

        request.setAttribute("requisitions", list);
        request.setAttribute("departments", departments);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalCount", totalRequests);
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("paramSearch", search);
        request.setAttribute("paramStatus", status);
        request.setAttribute("paramDept", departmentId);
        request.setAttribute("activeMenu", "requisitions");

        request.getRequestDispatcher("/WEB-INF/views/recruitment/list.jsp").forward(request, response);
    }

    private void showCreateForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Department> departments = departmentService.getAllDepartments();
        List<SalaryRange> salaryRanges = salaryRangeService.getAllSalaryRanges();
        String copyFrom = request.getParameter("copyFrom");

        if (copyFrom != null && !copyFrom.trim().isEmpty()) {
            RecruitmentRequest source = requisitionService.getRequestById(copyFrom.trim());
            if (source == null) {
                HttpSession session = request.getSession();
                session.setAttribute("flashError", "Không tìm thấy yêu cầu tuyển dụng cần sao chép!");
                response.sendRedirect(request.getContextPath() + "/recruitment-requests");
                return;
            }

            request.setAttribute("reqItem", createDraftCopy(source));
            request.setAttribute("isCopy", true);
        }

        request.setAttribute("departments", departments);
        request.setAttribute("salaryRanges", salaryRanges);
        request.setAttribute("nextCode", requisitionService.getNextCode());
        request.setAttribute("todayDate", LocalDate.now().toString());
        request.setAttribute("isEdit", false);
        request.setAttribute("activeMenu", "requisitions");

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
        request.setAttribute("activeMenu", "requisitions");

        // Chuỗi cấp duyệt và phân cấp phê duyệt (KN-158, KN-159)
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (!"DRAFT".equalsIgnoreCase(reqItem.getStatus())) {
            request.setAttribute("approvalSteps", approvalService.getApprovalChain(id));
            request.setAttribute("nextApproverInfo", approvalService.getNextApproverInfo(id));
            request.setAttribute("canCurrentUserApprove", approvalService.canCurrentUserApprove(currentUser, id));
        }

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
        boolean isCopy = "true".equalsIgnoreCase(request.getParameter("copyMode"));
        boolean isDraft = isCopy || "draft".equalsIgnoreCase(action);

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
                if (!requisitionService.updateRequest(req, isDraft, actorId, ip, userAgent)) {
                    throw new IllegalStateException("Không thể lưu yêu cầu tuyển dụng. Vui lòng thử lại.");
                }
            } else {
                if (!requisitionService.createRequest(req, isDraft, actorId, ip, userAgent)) {
                    throw new IllegalStateException("Không thể lưu yêu cầu tuyển dụng. Vui lòng thử lại.");
                }
            }

            String msg = isCopy
                ? "Đã tạo bản sao tuyển dụng ở trạng thái Nháp!"
                : isDraft
                    ? "Đã lưu nháp yêu cầu tuyển dụng thành công!"
                    : "Yêu cầu tuyển dụng đã được gửi duyệt thành công!";
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
            request.setAttribute("isCopy", isCopy);
            request.getRequestDispatcher("/WEB-INF/views/recruitment/form.jsp").forward(request, response);

        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", "Lỗi xử lý hệ thống: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/recruitment-requests");
        }
    }

    static RecruitmentRequest createDraftCopy(RecruitmentRequest source) {
        if (source == null) {
            throw new IllegalArgumentException("Tin tuyển dụng gốc không hợp lệ.");
        }

        RecruitmentRequest copy = new RecruitmentRequest();
        String title = "Bản sao - " + (source.getTitle() == null ? "" : source.getTitle());
        copy.setTitle(title.length() <= 255 ? title : title.substring(0, 255));
        copy.setPositionTitle(source.getPositionTitle());
        copy.setJobDescription(source.getJobDescription());
        copy.setJobRequirements(source.getJobRequirements());
        return copy;
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

    /**
     * KN-158: API lấy chuỗi cấp duyệt và trạng thái hiện tại của hồ sơ
     */
    private void handleApprovalChainApi(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            id = request.getParameter("requisitionId");
        }

        if (id == null || id.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Thiếu mã ID yêu cầu tuyển dụng cần tra cứu chuỗi cấp duyệt!");
            JsonUtil.sendJsonResponse(response, 400, err);
            return;
        }

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        Map<String, Object> state = approvalService.getApprovalWorkflowState(id.trim(), currentUser);
        JsonUtil.sendJsonResponse(response, 200, state);
    }

    /**
     * KN-154 & KN-155: API ghi nhận kết quả phê duyệt theo từng cấp
     */
    private void handleApprovalDecisionApi(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại!");
            JsonUtil.sendJsonResponse(response, 401, err);
            return;
        }

        String requisitionId = request.getParameter("requisitionId");
        String action = request.getParameter("action"); // APPROVE / REJECT
        String comment = request.getParameter("comment");
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        if (requisitionId == null || requisitionId.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Thiếu mã ID yêu cầu tuyển dụng!");
            JsonUtil.sendJsonResponse(response, 400, err);
            return;
        }

        try {
            boolean success = approvalService.processApprovalDecision(requisitionId.trim(), currentUser, action, comment, ip, userAgent);
            Map<String, Object> respData = new HashMap<>();
            respData.put("success", success);
            respData.put("message", "APPROVE".equalsIgnoreCase(action) ? "Đã ghi nhận phê duyệt thành công!" : "Đã ghi nhận từ chối yêu cầu tuyển dụng!");
            respData.put("state", approvalService.getApprovalWorkflowState(requisitionId.trim(), currentUser));
            JsonUtil.sendJsonResponse(response, 200, respData);
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            Map<String, Object> errData = new HashMap<>();
            errData.put("success", false);
            errData.put("message", e.getMessage());
            JsonUtil.sendJsonResponse(response, 400, errData);
        } catch (Exception e) {
            Map<String, Object> errData = new HashMap<>();
            errData.put("success", false);
            errData.put("message", "Lỗi xử lý hệ thống: " + e.getMessage());
            JsonUtil.sendJsonResponse(response, 500, errData);
        }
    }

    private void handleCheckPipeline(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Thiếu mã ID yêu cầu tuyển dụng!");
            JsonUtil.sendJsonResponse(response, 400, err);
            return;
        }

        try {
            Map<String, Object> data = requisitionService.checkPipelineAndPostings(id.trim());
            data.put("success", true);
            JsonUtil.sendJsonResponse(response, 200, data);
        } catch (IllegalArgumentException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", e.getMessage());
            JsonUtil.sendJsonResponse(response, 404, err);
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Lỗi kiểm tra pipeline: " + e.getMessage());
            JsonUtil.sendJsonResponse(response, 500, err);
        }
    }

    private void handleCloseRequisition(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String actorId = (currentUser != null) ? currentUser.getId() : null;
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        String id = request.getParameter("id");
        String terminalStatus = request.getParameter("terminalStatus");
        if (terminalStatus == null || terminalStatus.trim().isEmpty()) {
            terminalStatus = request.getParameter("status");
        }
        String closeReason = request.getParameter("closeReason");
        String closeNotes = request.getParameter("closeNotes");
        String candidateAction = request.getParameter("candidateAction");

        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"))
                || "json".equalsIgnoreCase(request.getParameter("format"));

        try {
            Map<String, Object> result = requisitionService.closeRequest(
                    id, terminalStatus, closeReason, closeNotes, candidateAction, actorId, ip, userAgent
            );

            if (isAjax) {
                JsonUtil.sendJsonResponse(response, 200, result);
            } else {
                if (session != null) {
                    session.setAttribute("flashSuccess", (String) result.get("message"));
                }
                response.sendRedirect(request.getContextPath() + "/recruitment-requests");
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            if (isAjax) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("error", e.getMessage());
                JsonUtil.sendJsonResponse(response, 400, err);
            } else {
                if (session != null) {
                    session.setAttribute("flashError", e.getMessage());
                }
                response.sendRedirect(request.getContextPath() + "/recruitment-requests");
            }
        } catch (Exception e) {
            if (isAjax) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("error", "Lỗi xử lý hệ thống: " + e.getMessage());
                JsonUtil.sendJsonResponse(response, 500, err);
            } else {
                if (session != null) {
                    session.setAttribute("flashError", "Lỗi xử lý hệ thống: " + e.getMessage());
                }
                response.sendRedirect(request.getContextPath() + "/recruitment-requests");
            }
        }
    }

    private void handleApprovalStepsApi(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String id = request.getParameter("id");
        List<RequisitionApprovalStep> steps = requisitionService.getApprovalSteps(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("steps", steps);
        JsonUtil.sendJsonResponse(response, 200, result);
    }

    private void handleApproveStep(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String requisitionId = request.getParameter("requisitionId");
        String stepId = request.getParameter("stepId");
        String comment = request.getParameter("comment");
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            requisitionService.approveStep(requisitionId, stepId, comment, currentUser, ip, userAgent);
            if (session != null) session.setAttribute("flashSuccess", "Đã phê duyệt cấp này thành công!");
        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/recruitment-requests");
    }

    private void handleRejectStep(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String requisitionId = request.getParameter("requisitionId");
        String stepId = request.getParameter("stepId");
        String reason = request.getParameter("reason");
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            requisitionService.rejectStep(requisitionId, stepId, reason, currentUser, ip, userAgent);
            if (session != null) session.setAttribute("flashSuccess", "Đã từ chối phê duyệt yêu cầu này!");
        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", e.getMessage());
        }
        response.sendRedirect(request.getContextPath() + "/recruitment-requests");
    }
}

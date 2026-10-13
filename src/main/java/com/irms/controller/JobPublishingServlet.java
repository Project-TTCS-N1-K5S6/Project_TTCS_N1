package com.irms.controller;

import com.google.gson.Gson;
import com.irms.model.Department;
import com.irms.model.JobPostPublication;
import com.irms.model.JobPosting;
import com.irms.model.RecruitmentRequest;
import com.irms.model.User;
import com.irms.service.DepartmentService;
import com.irms.service.JobPublishingService;
import com.irms.service.RecruitmentRequestService;
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
 * Controller xử lý Quản lý Xuất bản bài đăng (KN-120)
 * 1. Tin phải duyệt trước khi show công khai
 * 2. Xem trước đúng như giao diện khai báo trước khi xuất bản
 * 3. Ghi lại bản xuất và thời điểm
 */
@WebServlet(name = "JobPublishingServlet", urlPatterns = {
        "/post-publishing",
        "/post-publishing/preview",
        "/post-publishing/publish",
        "/post-publishing/unpublish",
        "/post-publishing/approve",
        "/post-publishing/history",
        "/post-publishing/version-detail",
        "/careers",
        "/careers/job"
})
public class JobPublishingServlet extends HttpServlet {

    private final JobPublishingService publishingService = new JobPublishingService();
    private final DepartmentService departmentService = new DepartmentService();
    private final RecruitmentRequestService requisitionService = new RecruitmentRequestService();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        // 1. Cổng tuyển dụng công khai cho ứng viên (Public Career Portal)
        if ("/careers".equals(path)) {
            showPublicCareerPortal(request, response);
            return;
        }

        if ("/careers/job".equals(path)) {
            showPublicJobDetail(request, response);
            return;
        }

        // 2. API lấy dữ liệu xem trước (Preview) đúng như khai báo trước khi xuất bản
        if ("/post-publishing/preview".equals(path)) {
            handlePreviewApi(request, response);
            return;
        }

        // 3. API lấy lịch sử các bản xuất và thời điểm
        if ("/post-publishing/history".equals(path)) {
            handleHistoryApi(request, response);
            return;
        }

        // 4. API lấy chi tiết snapshot của 1 bản xuất cụ thể trong lịch sử
        if ("/post-publishing/version-detail".equals(path)) {
            handleVersionDetailApi(request, response);
            return;
        }

        // 5. Màn hình Quản lý Xuất bản bài đăng chính
        showPublishingDashboard(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String path = request.getServletPath();

        if ("/post-publishing/publish".equals(path)) {
            handlePublish(request, response);
            return;
        }

        if ("/post-publishing/unpublish".equals(path)) {
            handleUnpublish(request, response);
            return;
        }

        if ("/post-publishing/approve".equals(path)) {
            handleApprove(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/post-publishing");
    }

    private void showPublishingDashboard(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String search = request.getParameter("search");
        String deptId = request.getParameter("departmentId");
        String approvalStatus = request.getParameter("approvalStatus");
        String publishStatus = request.getParameter("publishStatus");

        int page = 1;
        int pageSize = 10;
        try {
            if (request.getParameter("page") != null) {
                page = Integer.parseInt(request.getParameter("page"));
            }
        } catch (NumberFormatException ignored) {}

        List<JobPosting> postings = publishingService.getPostings(search, deptId, approvalStatus, publishStatus, page, pageSize);
        int totalPostings = publishingService.countPostings(search, deptId, approvalStatus, publishStatus);
        int totalPages = (int) Math.ceil((double) totalPostings / pageSize);

        List<Department> departments = departmentService.getAllDepartments();

        // Thống kê nhanh KPI
        int publishedCount = publishingService.countPostings(null, null, null, "PUBLISHED");
        int pendingApprovalCount = publishingService.countPostings(null, null, "PENDING", null);
        int draftCount = publishingService.countPostings(null, null, "DRAFT", null);

        request.setAttribute("postings", postings);
        request.setAttribute("departments", departments);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalCount", totalPostings);
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("paramSearch", search);
        request.setAttribute("paramDept", deptId);
        request.setAttribute("paramApproval", approvalStatus);
        request.setAttribute("paramPublish", publishStatus);

        request.setAttribute("kpiPublished", publishedCount);
        request.setAttribute("kpiPendingApproval", pendingApprovalCount);
        request.setAttribute("kpiDraft", draftCount);

        request.setAttribute("activeMenu", "post-publishing");

        request.getRequestDispatcher("/WEB-INF/views/publishing/index.jsp").forward(request, response);
    }

    /**
     * KN-120 (2): Xem trước đúng như giao diện khai báo trước khi xuất bản
     */
    private void handlePreviewApi(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String reqId = request.getParameter("id");
        if (reqId == null || reqId.trim().isEmpty()) {
            JsonUtil.sendJsonResponse(response, 400, Map.of("error", "Thiếu mã định danh yêu cầu tuyển dụng!"));
            return;
        }

        RecruitmentRequest req = requisitionService.getRequestById(reqId);
        if (req == null) {
            JsonUtil.sendJsonResponse(response, 404, Map.of("error", "Không tìm thấy tin tuyển dụng!"));
            return;
        }

        JobPosting posting = publishingService.getPostingByRequisitionId(reqId);
        boolean isApproved = publishingService.isEligibleForPublish(req);

        Map<String, Object> data = new HashMap<>();
        data.put("id", req.getId());
        data.put("code", req.getCode());
        data.put("title", req.getTitle());
        data.put("positionTitle", req.getPositionTitle());
        data.put("departmentName", req.getDepartmentName() != null ? req.getDepartmentName() : "Toàn công ty");
        data.put("headcount", req.getHeadcount());
        data.put("recruitmentReasonLabel", req.getRecruitmentReasonLabel());
        data.put("formattedSalaryRange", req.getFormattedSalaryRange());
        data.put("deadlineFormatted", req.getDeadline() != null ? new java.text.SimpleDateFormat("dd/MM/yyyy").format(req.getDeadline()) : "Không giới hạn");
        data.put("jobDescription", req.getJobDescription());
        data.put("jobRequirements", req.getJobRequirements());
        data.put("salaryExplanation", req.getSalaryExplanation());
        data.put("recruiterName", req.getRecruiterName() != null ? req.getRecruiterName() : "Bộ phận Tuyển dụng");
        data.put("status", req.getStatus());
        data.put("statusLabel", req.getStatusLabel());
        data.put("isApproved", isApproved);
        data.put("currentVersion", posting != null ? posting.getCurrentVersion() : 0);
        data.put("publishStatus", posting != null ? posting.getStatus() : "DRAFT");
        data.put("publishStatusLabel", posting != null ? posting.getStatusLabel() : "Chưa xuất bản");

        JsonUtil.sendJsonResponse(response, 200, data);
    }

    /**
     * KN-120 (1) + (3): Xử lý xuất bản bài đăng
     */
    private void handlePublish(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        String reqId = request.getParameter("requisitionId");
        String channel = request.getParameter("channel");
        String note = request.getParameter("note");

        if (reqId == null || reqId.trim().isEmpty()) {
            if (session != null) session.setAttribute("flashError", "Thiếu mã yêu cầu tuyển dụng cần xuất bản!");
            response.sendRedirect(request.getContextPath() + "/post-publishing");
            return;
        }

        try {
            JobPostPublication pub = publishingService.publishPost(reqId, channel, note, currentUser, ip, userAgent);
            if (session != null) {
                session.setAttribute("flashSuccess", "Xuất bản bài đăng thành công! Đã ghi nhận " 
                        + pub.getVersionLabel() + " vào lúc " + pub.getFormattedPublishedAt() + ".");
            }
        } catch (IllegalStateException e) {
            // Lỗi quy định: Tin phải duyệt trước khi show công khai
            if (session != null) {
                session.setAttribute("flashError", e.getMessage());
            }
        } catch (Exception e) {
            if (session != null) {
                session.setAttribute("flashError", "Lỗi trong quá trình xuất bản: " + e.getMessage());
            }
        }

        response.sendRedirect(request.getContextPath() + "/post-publishing");
    }

    /**
     * Xử lý gỡ bài đăng
     */
    private void handleUnpublish(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        String reqId = request.getParameter("requisitionId");
        String reason = request.getParameter("reason");

        if (reqId == null || reqId.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/post-publishing");
            return;
        }

        boolean ok = publishingService.unpublishPost(reqId, reason, currentUser, ip, userAgent);
        if (session != null) {
            if (ok) {
                session.setAttribute("flashSuccess", "Đã gỡ bài đăng tuyển dụng công khai thành công!");
            } else {
                session.setAttribute("flashError", "Không thể gỡ bài đăng tuyển dụng!");
            }
        }
        response.sendRedirect(request.getContextPath() + "/post-publishing");
    }

    /**
     * Phê duyệt yêu cầu tuyển dụng để đủ điều kiện xuất bản
     */
    private void handleApprove(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        String reqId = request.getParameter("requisitionId");
        if (reqId == null || reqId.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/post-publishing");
            return;
        }

        boolean ok = publishingService.approveRequisition(reqId, currentUser, ip, userAgent);
        if (session != null) {
            if (ok) {
                session.setAttribute("flashSuccess", "Phê duyệt tin tuyển dụng thành công! Tin hiện đã đủ điều kiện để xuất bản công khai.");
            } else {
                session.setAttribute("flashError", "Không thể phê duyệt tin tuyển dụng!");
            }
        }
        response.sendRedirect(request.getContextPath() + "/post-publishing");
    }

    /**
     * KN-120 (3): API trả về lịch sử các bản xuất và thời điểm
     */
    private void handleHistoryApi(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String reqId = request.getParameter("requisitionId");
        if (reqId == null || reqId.trim().isEmpty()) {
            JsonUtil.sendJsonResponse(response, 400, Map.of("error", "Thiếu mã yêu cầu tuyển dụng!"));
            return;
        }

        List<JobPostPublication> history = publishingService.getPublicationHistory(reqId);
        JsonUtil.sendJsonResponse(response, 200, history);
    }

    /**
     * Lấy chi tiết snapshot nội dung của bản xuất lịch sử
     */
    private void handleVersionDetailApi(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String pubId = request.getParameter("id");
        if (pubId == null || pubId.trim().isEmpty()) {
            JsonUtil.sendJsonResponse(response, 400, Map.of("error", "Thiếu ID bản xuất!"));
            return;
        }

        JobPostPublication pub = publishingService.getPublicationById(pubId);
        if (pub == null) {
            JsonUtil.sendJsonResponse(response, 404, Map.of("error", "Không tìm thấy bản xuất!"));
            return;
        }

        JsonUtil.sendJsonResponse(response, 200, pub);
    }

    /**
     * Cổng tuyển dụng công khai: CHỈ hiển thị tin ĐÃ DUYỆT và ĐANG XUẤT BẢN ("Tin phải duyệt trước khi show công khai")
     */
    private void showPublicCareerPortal(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String search = request.getParameter("search");
        String deptId = request.getParameter("deptId");

        int page = 1;
        int pageSize = 12;
        try {
            if (request.getParameter("page") != null) {
                page = Integer.parseInt(request.getParameter("page"));
            }
        } catch (NumberFormatException ignored) {}

        List<JobPosting> publicJobs = publishingService.getPublicPublishedPosts(search, deptId, page, pageSize);
        List<Department> departments = departmentService.getAllDepartments();

        request.setAttribute("publicJobs", publicJobs);
        request.setAttribute("departments", departments);
        request.setAttribute("paramSearch", search);
        request.setAttribute("paramDept", deptId);
        request.setAttribute("currentPage", page);

        request.getRequestDispatcher("/WEB-INF/views/publishing/careers.jsp").forward(request, response);
    }

    /**
     * Xem chi tiết 1 tin tuyển dụng công khai
     */
    private void showPublicJobDetail(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/careers");
            return;
        }

        JobPosting posting = publishingService.getPostingByRequisitionId(id);
        // BẮT BUỘC KIỂM TRA: Chỉ hiển thị nếu ĐÃ DUYỆT và ĐANG XUẤT BẢN
        if (posting == null || !posting.isApproved() || !posting.isPublished()) {
            request.setAttribute("errorMessage", "Tin tuyển dụng này chưa được phê duyệt hoặc chưa xuất bản công khai!");
            request.getRequestDispatcher("/WEB-INF/views/errors/404.jsp").forward(request, response);
            return;
        }

        // Tăng view count
        new com.irms.dao.JobPostingDAO().incrementViews(posting.getId());

        request.setAttribute("job", posting);
        request.getRequestDispatcher("/WEB-INF/views/publishing/job-detail.jsp").forward(request, response);
    }
}

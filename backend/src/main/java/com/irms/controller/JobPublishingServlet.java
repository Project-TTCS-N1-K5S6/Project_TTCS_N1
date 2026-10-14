package com.irms.controller;

import com.google.gson.Gson;
import com.irms.model.Department;
import com.irms.model.JobPostPublication;
import com.irms.model.JobPosting;
import com.irms.model.RecruitmentRequest;
import com.irms.model.SharedCatalogItem;
import com.irms.model.SharedCatalogType;
import com.irms.model.User;
import com.irms.service.DepartmentService;
import com.irms.service.JobPublishingService;
import com.irms.service.RecruitmentRequestService;
import com.irms.service.SharedCatalogService;
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
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller xử lý Quản lý Xuất bản bài đăng (KN-120) và Tạo nhanh Bài đăng Tuyển dụng (KN-204, KN-207)
 * 1. Tin phải duyệt trước khi show công khai (KN-208)
 * 2. Xem trước đúng như giao diện khai báo trước khi xuất bản
 * 3. Ghi lại bản xuất và thời điểm
 * 4. Tạo nhanh bài đăng từ yêu cầu tuyển dụng đã duyệt (KN-204, KN-205, KN-206, KN-207)
 */
@WebServlet(name = "JobPublishingServlet", urlPatterns = {
        "/post-publishing",
        "/post-publishing/preview",
        "/post-publishing/publish",
        "/post-publishing/unpublish",
        "/post-publishing/approve",
        "/post-publishing/history",
        "/post-publishing/version-detail",
        "/post-publishing/quick-create",
        "/post-publishing/create",
        "/post-publishing/requisition-data",
        "/post-publishing/approved-requisitions",
        "/job-postings",
        "/job-postings/create",
        "/job-postings/quick-create",
        "/careers",
        "/careers/job",
        "/jobs",
        "/jobs/detail"
})
public class JobPublishingServlet extends HttpServlet {

    private final JobPublishingService publishingService = new JobPublishingService();
    private final DepartmentService departmentService = new DepartmentService();
    private final RecruitmentRequestService requisitionService = new RecruitmentRequestService();
    private final com.irms.dao.SharedCatalogDAO sharedCatalogDAO = new com.irms.dao.SharedCatalogDAO();
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        // 0. Điều hướng URL /job-postings về /post-publishing
        if ("/job-postings".equals(path)) {
            response.sendRedirect(request.getContextPath() + "/post-publishing");
            return;
        }

        // 1. Cổng tuyển dụng công khai cho ứng viên (Public Career Portal)
        if ("/careers".equals(path) || "/jobs".equals(path)) {
            showPublicCareerPortal(request, response);
            return;
        }

        if ("/careers/job".equals(path) || "/jobs/detail".equals(path)) {
            showPublicJobDetail(request, response);
            return;
        }

        // KN-204: Màn hình tạo nhanh bài đăng tuyển dụng
        if ("/post-publishing/create".equals(path) || "/job-postings/create".equals(path)) {
            showCreateJobPostingPage(request, response);
            return;
        }

        // KN-206: API lấy thông tin yêu cầu tuyển dụng nguồn và mô tả chuẩn hóa
        if ("/post-publishing/requisition-data".equals(path)) {
            handleRequisitionDataApi(request, response);
            return;
        }

        // KN-208: API lấy danh sách yêu cầu tuyển dụng đã duyệt để tạo bài đăng
        if ("/post-publishing/approved-requisitions".equals(path)) {
            handleApprovedRequisitionsApi(request, response);
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

        // KN-207: API tạo bài đăng từ yêu cầu tuyển dụng đã duyệt
        if ("/post-publishing/quick-create".equals(path) || "/job-postings/quick-create".equals(path)
                || "/post-publishing/create".equals(path) || "/job-postings/create".equals(path)) {
            handleQuickCreate(request, response);
            return;
        }

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

        SharedCatalogService catalogService = new SharedCatalogService();
        List<SharedCatalogItem> workLocations = catalogService.getItems(SharedCatalogType.WORK_LOCATION);
        List<SharedCatalogItem> workModes = catalogService.getItems(SharedCatalogType.WORK_MODE);
        List<RecruitmentRequest> approvedRequisitions = publishingService.getApprovedRequisitions();

        request.setAttribute("postings", postings);
        request.setAttribute("departments", departments);
        request.setAttribute("workLocations", workLocations);
        request.setAttribute("workModes", workModes);
        request.setAttribute("approvedRequisitions", approvedRequisitions);
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
        String keyword = request.getParameter("q");
        if (keyword == null || keyword.trim().isEmpty()) {
            keyword = request.getParameter("keyword");
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            keyword = request.getParameter("search");
        }

        String deptId = request.getParameter("department_id");
        if (deptId == null || deptId.trim().isEmpty()) {
            deptId = request.getParameter("department");
        }
        if (deptId == null || deptId.trim().isEmpty()) {
            deptId = request.getParameter("deptId");
        }

        String locationId = request.getParameter("location_id");
        if (locationId == null || locationId.trim().isEmpty()) {
            locationId = request.getParameter("location");
        }

        String workType = request.getParameter("work_type");
        if (workType == null || workType.trim().isEmpty()) {
            workType = request.getParameter("work_mode");
        }

        String sortBy = request.getParameter("sort");

        int page = 1;
        int pageSize = 9;
        try {
            if (request.getParameter("page") != null) {
                page = Math.max(1, Integer.parseInt(request.getParameter("page")));
            }
        } catch (NumberFormatException ignored) {}

        try {
            if (request.getParameter("limit") != null) {
                pageSize = Math.max(1, Math.min(100, Integer.parseInt(request.getParameter("limit"))));
            }
        } catch (NumberFormatException ignored) {}

        List<JobPosting> publicJobs = publishingService.getPublicJobs(keyword, deptId, locationId, workType, sortBy, page, pageSize);
        int totalJobs = publishingService.countPublicJobs(keyword, deptId, locationId, workType);
        int totalPages = (totalJobs > 0) ? (int) Math.ceil((double) totalJobs / pageSize) : 1;

        List<Department> departments = departmentService.getAllDepartments();
        List<com.irms.model.SharedCatalogItem> locations = sharedCatalogDAO.findAll(com.irms.model.SharedCatalogType.WORK_LOCATION);
        List<com.irms.model.SharedCatalogItem> workTypes = sharedCatalogDAO.findAll(com.irms.model.SharedCatalogType.WORK_MODE);

        request.setAttribute("publicJobs", publicJobs);
        request.setAttribute("totalJobs", totalJobs);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("currentPage", page);
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("departments", departments);
        request.setAttribute("locations", locations);
        request.setAttribute("workTypes", workTypes);

        request.setAttribute("paramSearch", keyword != null ? keyword : "");
        request.setAttribute("paramKeyword", keyword != null ? keyword : "");
        request.setAttribute("paramDept", deptId != null ? deptId : "");
        request.setAttribute("paramLocation", locationId != null ? locationId : "");
        request.setAttribute("paramWorkType", workType != null ? workType : "");
        request.setAttribute("paramSort", sortBy != null ? sortBy : "newest");

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

    /**
     * KN-206: API lấy dữ liệu chi tiết của yêu cầu tuyển dụng đã duyệt
     * Chuẩn hóa mô tả công việc (gạch đầu dòng, khoảng trắng) nhưng giữ nguyên yêu cầu gốc.
     */
    private void handleRequisitionDataApi(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String reqId = request.getParameter("id");
        if (reqId == null || reqId.trim().isEmpty()) {
            reqId = request.getParameter("requisitionId");
        }
        if (reqId == null || reqId.trim().isEmpty()) {
            JsonUtil.sendJsonResponse(response, 400, Map.of("error", "Thiếu mã định danh yêu cầu tuyển dụng!"));
            return;
        }

        RecruitmentRequest req = requisitionService.getRequestById(reqId.trim());
        if (req == null) {
            JsonUtil.sendJsonResponse(response, 404, Map.of("error", "Không tìm thấy yêu cầu tuyển dụng!"));
            return;
        }

        // KN-208: Kiểm tra trạng thái đã duyệt
        boolean isApproved = publishingService.isEligibleForPublish(req);
        if (!isApproved) {
            JsonUtil.sendJsonResponse(response, 400, Map.of(
                    "error", "Yêu cầu tuyển dụng chưa ở trạng thái Đã duyệt (" + req.getStatusLabel() + "), không thể tạo bài đăng tuyển dụng!",
                    "isApproved", false,
                    "status", req.getStatus(),
                    "statusLabel", req.getStatusLabel()
            ));
            return;
        }

        // KN-206: Chuẩn hóa nội dung mô tả công việc
        String normalizedJd = publishingService.normalizeJobDescription(req.getJobDescription());

        Map<String, Object> data = new HashMap<>();
        data.put("id", req.getId());
        data.put("code", req.getCode());
        data.put("title", req.getTitle());
        data.put("positionTitle", req.getPositionTitle() != null ? req.getPositionTitle() : req.getTitle());
        data.put("departmentId", req.getDepartmentId());
        data.put("departmentName", req.getDepartmentName() != null ? req.getDepartmentName() : "Toàn công ty");
        data.put("headcount", req.getHeadcount());
        data.put("deadline", req.getDeadline() != null ? new SimpleDateFormat("yyyy-MM-dd").format(req.getDeadline()) : "");
        data.put("deadlineFormatted", req.getDeadline() != null ? new SimpleDateFormat("dd/MM/yyyy").format(req.getDeadline()) : "Chưa xác định");
        data.put("minSalary", req.getMinSalary());
        data.put("maxSalary", req.getMaxSalary());
        data.put("salaryVisible", req.getMinSalary() != null || req.getMaxSalary() != null);
        data.put("formattedSalaryRange", req.getFormattedSalaryRange());
        data.put("workLocation", req.getWorkLocation());
        data.put("workMode", req.getWorkMode());
        data.put("rawJobDescription", req.getJobDescription());
        data.put("normalizedJobDescription", normalizedJd);
        data.put("jobRequirements", req.getJobRequirements());
        data.put("status", req.getStatus());
        data.put("statusLabel", req.getStatusLabel());
        data.put("isApproved", true);

        JsonUtil.sendJsonResponse(response, 200, data);
    }

    /**
     * KN-208: API trả về danh sách các yêu cầu tuyển dụng đã duyệt để tạo bài đăng
     */
    private void handleApprovedRequisitionsApi(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        List<RecruitmentRequest> approvedList = publishingService.getApprovedRequisitions();
        JsonUtil.sendJsonResponse(response, 200, approvedList);
    }

    /**
     * KN-204: Màn hình tạo nhanh bài đăng tuyển dụng
     */
    private void showCreateJobPostingPage(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        // Phân quyền tạo bài đăng tuyển dụng
        if (!publishingService.canManageJobPostings(currentUser)) {
            request.setAttribute("errorMessage", "Bạn không có quyền tạo bài đăng tuyển dụng!");
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        String reqId = request.getParameter("requisitionId");
        if (reqId == null || reqId.trim().isEmpty()) {
            reqId = request.getParameter("id");
        }

        RecruitmentRequest selectedReq = null;
        JobPosting draftPosting = new JobPosting();

        if (reqId != null && !reqId.trim().isEmpty()) {
            selectedReq = requisitionService.getRequestById(reqId.trim());
            if (selectedReq == null) {
                if (session != null) {
                    session.setAttribute("flashError", "Không tìm thấy yêu cầu tuyển dụng nguồn!");
                }
                response.sendRedirect(request.getContextPath() + "/post-publishing");
                return;
            }

            // KN-208: Kiểm tra trạng thái đã duyệt
            if (!publishingService.isEligibleForPublish(selectedReq)) {
                if (session != null) {
                    session.setAttribute("flashError", "Chỉ những yêu cầu tuyển dụng có trạng thái Đã duyệt mới được phép tạo bài đăng tuyển dụng! (Trạng thái hiện tại: " + selectedReq.getStatusLabel() + ")");
                }
                response.sendRedirect(request.getContextPath() + "/post-publishing");
                return;
            }

            // Kế thừa và chuẩn hóa
            draftPosting.setRequisitionId(selectedReq.getId());
            draftPosting.setTitle(selectedReq.getTitle());
            draftPosting.setJobDescription(publishingService.normalizeJobDescription(selectedReq.getJobDescription()));
            draftPosting.setApplicationDeadline(selectedReq.getDeadline());
            draftPosting.setMinSalary(selectedReq.getMinSalary());
            draftPosting.setMaxSalary(selectedReq.getMaxSalary());
            draftPosting.setSalaryVisible(selectedReq.getMinSalary() != null || selectedReq.getMaxSalary() != null);
            draftPosting.setWorkLocation(selectedReq.getWorkLocation());
            draftPosting.setWorkMode(selectedReq.getWorkMode());
        }

        SharedCatalogService catalogService = new SharedCatalogService();
        List<SharedCatalogItem> workLocations = catalogService.getItems(SharedCatalogType.WORK_LOCATION);
        List<SharedCatalogItem> workModes = catalogService.getItems(SharedCatalogType.WORK_MODE);
        List<RecruitmentRequest> approvedRequisitions = publishingService.getApprovedRequisitions();

        request.setAttribute("selectedReq", selectedReq);
        request.setAttribute("draftPosting", draftPosting);
        request.setAttribute("approvedRequisitions", approvedRequisitions);
        request.setAttribute("workLocations", workLocations);
        request.setAttribute("workModes", workModes);
        request.setAttribute("activeMenu", "post-publishing");

        request.getRequestDispatcher("/WEB-INF/views/publishing/create.jsp").forward(request, response);
    }

    /**
     * KN-207: Xử lý tiếp nhận và tạo bài đăng tuyển dụng từ form / modal
     */
    private void handleQuickCreate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"))
                || "json".equalsIgnoreCase(request.getParameter("format"));

        // Phân quyền
        if (!publishingService.canManageJobPostings(currentUser)) {
            String errMsg = "Bạn không có quyền thực hiện thao tác tạo bài đăng tuyển dụng!";
            if (isAjax) {
                JsonUtil.sendJsonResponse(response, 403, Map.of("success", false, "error", errMsg));
            } else {
                if (session != null) session.setAttribute("flashError", errMsg);
                response.sendRedirect(request.getContextPath() + "/post-publishing");
            }
            return;
        }

        String reqId = request.getParameter("requisitionId");
        String title = request.getParameter("title");
        String jobDescription = request.getParameter("jobDescription");
        String workLocationId = request.getParameter("workLocationId");
        String workLocation = request.getParameter("workLocation");
        String workModeId = request.getParameter("workModeId");
        String workMode = request.getParameter("workMode");
        String deadlineStr = request.getParameter("applicationDeadline");
        String salaryVisibleStr = request.getParameter("salaryVisible");
        String minSalaryStr = request.getParameter("minSalary");
        String maxSalaryStr = request.getParameter("maxSalary");
        String currency = request.getParameter("currency");
        String publishImmediatelyStr = request.getParameter("publishImmediately");

        boolean salaryVisible = "true".equalsIgnoreCase(salaryVisibleStr) || "on".equalsIgnoreCase(salaryVisibleStr) || "1".equals(salaryVisibleStr);
        boolean publishImmediately = "true".equalsIgnoreCase(publishImmediatelyStr) || "on".equalsIgnoreCase(publishImmediatelyStr) || "1".equals(publishImmediatelyStr);

        JobPosting posting = new JobPosting();
        posting.setRequisitionId(reqId != null ? reqId.trim() : null);
        posting.setTitle(title != null ? title.trim() : null);
        posting.setJobDescription(jobDescription);
        posting.setWorkLocationId(workLocationId != null ? workLocationId.trim() : null);
        posting.setWorkLocation(workLocation != null ? workLocation.trim() : null);
        posting.setWorkModeId(workModeId != null ? workModeId.trim() : null);
        posting.setWorkMode(workMode != null ? workMode.trim() : null);
        posting.setSalaryVisible(salaryVisible);
        posting.setCurrency(currency != null && !currency.trim().isEmpty() ? currency.trim() : "VND");
        posting.setStatus(publishImmediately ? "PUBLISHED" : "DRAFT");

        // Parse deadline
        if (deadlineStr != null && !deadlineStr.trim().isEmpty()) {
            try {
                posting.setApplicationDeadline(Date.valueOf(deadlineStr.trim()));
            } catch (IllegalArgumentException e) {
                try {
                    java.util.Date parsed = new SimpleDateFormat("dd/MM/yyyy").parse(deadlineStr.trim());
                    posting.setApplicationDeadline(new Date(parsed.getTime()));
                } catch (Exception ex) {
                    String err = "Định dạng hạn nhận hồ sơ không hợp lệ (hỗ trợ yyyy-MM-dd hoặc dd/MM/yyyy)!";
                    if (isAjax) {
                        JsonUtil.sendJsonResponse(response, 400, Map.of("success", false, "error", err));
                    } else {
                        if (session != null) session.setAttribute("flashError", err);
                        response.sendRedirect(request.getContextPath() + "/post-publishing/create?requisitionId=" + (reqId != null ? reqId : ""));
                    }
                    return;
                }
            }
        }

        // Parse lương nếu salaryVisible == true
        if (salaryVisible) {
            if (minSalaryStr != null && !minSalaryStr.trim().isEmpty()) {
                try {
                    String cleanMin = minSalaryStr.replaceAll("[,\\s]", "");
                    posting.setMinSalary(new BigDecimal(cleanMin));
                } catch (NumberFormatException e) {
                    String err = "Mức lương tối thiểu không đúng định dạng số!";
                    if (isAjax) {
                        JsonUtil.sendJsonResponse(response, 400, Map.of("success", false, "error", err));
                    } else {
                        if (session != null) session.setAttribute("flashError", err);
                        response.sendRedirect(request.getContextPath() + "/post-publishing/create?requisitionId=" + (reqId != null ? reqId : ""));
                    }
                    return;
                }
            }
            if (maxSalaryStr != null && !maxSalaryStr.trim().isEmpty()) {
                try {
                    String cleanMax = maxSalaryStr.replaceAll("[,\\s]", "");
                    posting.setMaxSalary(new BigDecimal(cleanMax));
                } catch (NumberFormatException e) {
                    String err = "Mức lương tối đa không đúng định dạng số!";
                    if (isAjax) {
                        JsonUtil.sendJsonResponse(response, 400, Map.of("success", false, "error", err));
                    } else {
                        if (session != null) session.setAttribute("flashError", err);
                        response.sendRedirect(request.getContextPath() + "/post-publishing/create?requisitionId=" + (reqId != null ? reqId : ""));
                    }
                    return;
                }
            }
        } else {
            posting.setMinSalary(null);
            posting.setMaxSalary(null);
        }

        try {
            JobPosting created = publishingService.createQuickJobPosting(posting, currentUser, ip, userAgent);
            String successMsg = "Tạo nhanh bài đăng tuyển dụng thành công" + (publishImmediately ? " và đã xuất bản!" : " (Lưu nháp)!");
            if (isAjax) {
                Map<String, Object> respMap = new HashMap<>();
                respMap.put("success", true);
                respMap.put("message", successMsg);
                respMap.put("postingId", created.getId());
                respMap.put("requisitionId", created.getRequisitionId());
                respMap.put("status", created.getStatus());
                respMap.put("redirectUrl", request.getContextPath() + "/post-publishing");
                JsonUtil.sendJsonResponse(response, 200, respMap);
            } else {
                if (session != null) session.setAttribute("flashSuccess", successMsg);
                response.sendRedirect(request.getContextPath() + "/post-publishing");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            String errMsg = e.getMessage();
            if (isAjax) {
                JsonUtil.sendJsonResponse(response, 400, Map.of("success", false, "error", errMsg));
            } else {
                if (session != null) session.setAttribute("flashError", errMsg);
                response.sendRedirect(request.getContextPath() + "/post-publishing/create?requisitionId=" + (reqId != null ? reqId : ""));
            }
        } catch (Exception e) {
            String errMsg = "Lỗi trong quá trình tạo bài đăng: " + e.getMessage();
            if (isAjax) {
                JsonUtil.sendJsonResponse(response, 500, Map.of("success", false, "error", errMsg));
            } else {
                if (session != null) session.setAttribute("flashError", errMsg);
                response.sendRedirect(request.getContextPath() + "/post-publishing");
            }
        }
    }
}

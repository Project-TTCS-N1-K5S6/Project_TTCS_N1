package com.irms.controller;

import com.irms.dao.SharedCatalogDAO;
import com.irms.model.Department;
import com.irms.model.JobPosting;
import com.irms.model.SharedCatalogItem;
import com.irms.model.SharedCatalogType;
import com.irms.service.DepartmentService;
import com.irms.service.JobPublishingService;
import com.irms.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

/**
 * Controller API Công khai cho Danh sách việc làm (Public Job Portal API)
 * - KN-219: API Danh sách việc làm công khai (GET /api/v1/public/jobs)
 * - KN-222: Hỗ trợ tìm kiếm từ khóa (q / keyword)
 * - KN-223: Hỗ trợ bộ lọc phòng ban, địa điểm, hình thức làm việc
 * - Không yêu cầu đăng nhập (được kiểm soát bởi AuthFilter)
 */
@WebServlet(name = "PublicJobApiServlet", urlPatterns = {
        "/api/v1/public/jobs",
        "/api/public/jobs",
        "/api/v1/public/jobs/filters",
        "/api/public/jobs/filters"
})
public class PublicJobApiServlet extends HttpServlet {

    private final JobPublishingService publishingService = new JobPublishingService();
    private final DepartmentService departmentService = new DepartmentService();
    private final SharedCatalogDAO sharedCatalogDAO = new SharedCatalogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        // 1. Endpoint lấy dữ liệu tùy chọn lọc (Departments, Locations, Work Types)
        if (path.endsWith("/filters")) {
            handleGetFilters(request, response);
            return;
        }

        // 2. Endpoint lấy danh sách việc làm công khai kèm tìm kiếm, lọc, phân trang, sắp xếp
        handleGetPublicJobs(request, response);
    }

    private void handleGetPublicJobs(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // KN-222: Tìm kiếm từ khóa (hỗ trợ cả param 'q' lẫn 'keyword')
        String keyword = request.getParameter("q");
        if (keyword == null || keyword.trim().isEmpty()) {
            keyword = request.getParameter("keyword");
        }
        if (keyword != null) {
            keyword = keyword.trim();
        }

        // KN-223: Bộ lọc phòng ban (hỗ trợ 'department_id' lẫn 'department')
        String departmentId = request.getParameter("department_id");
        if (departmentId == null || departmentId.trim().isEmpty()) {
            departmentId = request.getParameter("department");
        }
        if (departmentId != null) {
            departmentId = departmentId.trim();
        }

        // KN-223: Bộ lọc địa điểm (hỗ trợ 'location_id' lẫn 'location')
        String locationId = request.getParameter("location_id");
        if (locationId == null || locationId.trim().isEmpty()) {
            locationId = request.getParameter("location");
        }
        if (locationId != null) {
            locationId = locationId.trim();
        }

        // KN-223: Bộ lọc hình thức làm việc (hỗ trợ 'work_type' lẫn 'work_mode')
        String workType = request.getParameter("work_type");
        if (workType == null || workType.trim().isEmpty()) {
            workType = request.getParameter("work_mode");
        }
        if (workType != null) {
            workType = workType.trim();
        }

        // KN-219: Sắp xếp (mặc định mới nhất lên đầu)
        String sortBy = request.getParameter("sort");
        if (sortBy == null || sortBy.trim().isEmpty()) {
            sortBy = "newest";
        }

        // KN-219: Phân trang (page, limit)
        int page = 1;
        try {
            String pageStr = request.getParameter("page");
            if (pageStr != null && !pageStr.trim().isEmpty()) {
                page = Math.max(1, Integer.parseInt(pageStr.trim()));
            }
        } catch (NumberFormatException ignored) {}

        int limit = 9; // Mặc định 9 bài/trang (phù hợp bố cục lưới 3 cột)
        try {
            String limitStr = request.getParameter("limit");
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                limit = Math.max(1, Math.min(100, Integer.parseInt(limitStr.trim())));
            }
        } catch (NumberFormatException ignored) {}

        // Truy vấn danh sách và tổng số lượng
        List<JobPosting> jobList = publishingService.getPublicJobs(keyword, departmentId, locationId, workType, sortBy, page, limit);
        int total = publishingService.countPublicJobs(keyword, departmentId, locationId, workType);
        int totalPages = (total > 0) ? (int) Math.ceil((double) total / limit) : 1;

        // Chuẩn hóa danh sách DTO trả về cho Client
        List<Map<String, Object>> items = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (JobPosting jp : jobList) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", jp.getId());
            item.put("requisitionId", jp.getRequisitionId());
            item.put("title", jp.getTitle());
            item.put("description", jp.getDescription() != null ? jp.getDescription() : jp.getJobDescription());
            item.put("departmentId", jp.getDepartmentId());
            item.put("departmentName", jp.getDepartmentName() != null ? jp.getDepartmentName() : "Toàn công ty");
            item.put("locationId", jp.getLocationId());
            item.put("location", jp.getWorkLocation() != null ? jp.getWorkLocation() : "Toàn quốc");
            item.put("workType", jp.getWorkType() != null ? jp.getWorkType() : "Toàn thời gian");
            item.put("workMode", jp.getWorkMode() != null ? jp.getWorkMode() : jp.getWorkType());
            item.put("status", jp.getStatus());
            item.put("publishedAt", jp.getPublishedAt());
            item.put("expiredAt", jp.getExpiredAt());
            item.put("formattedPublishedAt", jp.getFormattedPublishedAt());
            item.put("formattedExpiredAt", jp.getFormattedExpiredAt());
            item.put("formattedDeadline", jp.getFormattedDeadline());

            // Tính số ngày còn lại đến hạn nộp
            int daysRemaining = 0;
            Timestamp exp = jp.getExpiredAt();
            if (exp != null) {
                long diffMs = exp.getTime() - now;
                daysRemaining = (int) Math.ceil((double) diffMs / (1000 * 60 * 60 * 24));
            }
            item.put("daysRemaining", Math.max(0, daysRemaining));

            // Dải lương
            item.put("salaryVisible", jp.isSalaryVisible());
            item.put("minSalary", jp.getMinSalary());
            item.put("maxSalary", jp.getMaxSalary());
            item.put("currency", jp.getCurrency());
            item.put("formattedSalary", jp.getFormattedSalaryRange());

            // Số lượng tuyển & lượt xem
            item.put("headcount", jp.getRequisition() != null ? jp.getRequisition().getHeadcount() : 1);
            item.put("viewsCount", jp.getViewsCount());

            // Đường dẫn chi tiết công việc
            String targetId = (jp.getRequisitionId() != null && !jp.getRequisitionId().isEmpty())
                    ? jp.getRequisitionId() : jp.getId();
            item.put("detailUrl", request.getContextPath() + "/careers/job?id=" + targetId);

            items.add(item);
        }

        Map<String, Object> pagination = new LinkedHashMap<>();
        pagination.put("page", page);
        pagination.put("limit", limit);
        pagination.put("total", total);
        pagination.put("totalPages", totalPages);
        pagination.put("hasNext", page < totalPages);
        pagination.put("hasPrev", page > 1);

        Map<String, Object> responseData = new LinkedHashMap<>();
        responseData.put("success", true);
        responseData.put("data", items);
        responseData.put("pagination", pagination);

        JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
    }

    private void handleGetFilters(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Lấy danh sách phòng ban
        List<Department> rawDepts = departmentService.getAllDepartments();
        List<Map<String, Object>> depts = new ArrayList<>();
        if (rawDepts != null) {
            for (Department d : rawDepts) {
                if ("ACTIVE".equalsIgnoreCase(d.getStatus()) || d.getStatus() == null) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", d.getId());
                    m.put("name", d.getName());
                    m.put("code", d.getCode());
                    depts.add(m);
                }
            }
        }

        // Lấy danh mục địa điểm
        List<SharedCatalogItem> rawLocations = sharedCatalogDAO.findAll(SharedCatalogType.WORK_LOCATION);
        List<Map<String, Object>> locations = new ArrayList<>();
        if (rawLocations != null) {
            for (SharedCatalogItem item : rawLocations) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", item.getId());
                m.put("value", item.getValue());
                locations.add(m);
            }
        }

        // Lấy danh mục hình thức làm việc
        List<SharedCatalogItem> rawModes = sharedCatalogDAO.findAll(SharedCatalogType.WORK_MODE);
        List<Map<String, Object>> workTypes = new ArrayList<>();
        if (rawModes != null) {
            for (SharedCatalogItem item : rawModes) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", item.getId());
                m.put("value", item.getValue());
                workTypes.add(m);
            }
        }

        Map<String, Object> responseData = new LinkedHashMap<>();
        responseData.put("success", true);
        responseData.put("departments", depts);
        responseData.put("locations", locations);
        responseData.put("workTypes", workTypes);

        JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, responseData);
    }
}

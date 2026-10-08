package com.irms.controller;

import com.irms.dao.DepartmentDAO;
import com.irms.model.SalaryRange;
import com.irms.model.User;
import com.irms.service.AuditService;
import com.irms.service.SalaryRangeService;
import com.irms.service.SalaryRangeService.OfferLimitResult;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ==============================================================================
 * BỘ ĐIỀU KHIỂN KHAI BÁO DẢI LƯƠNG & HẠN MỨC DUYỆT OFFER (SalaryRangeServlet)
 * ==============================================================================
 * Đáp ứng các tiêu chí hoàn thành của Ticket Jira KN-103:
 * 1. Mỗi chức danh có mã, tên, cấp bậc, dải lương tối thiểu và tối đa.
 * 2. Dải lương dùng làm hạn mức duyệt offer về sau (cung cấp API đối soát & giao diện tra cứu).
 * 3. Chỉ Trưởng phòng Nhân sự (HR_MANAGER) và Quản trị viên (ADMIN) xem & quản lý được dải lương.
 * ==============================================================================
 */
@WebServlet(name = "SalaryRangeServlet", urlPatterns = {
        "/salary-ranges",
        "/salary-ranges/create",
        "/salary-ranges/edit",
        "/salary-ranges/delete",
        "/salary-ranges/detail",
        "/salary-ranges/check-offer"
})
public class SalaryRangeServlet extends HttpServlet {
    private final SalaryRangeService salaryRangeService = new SalaryRangeService();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final AuditService auditService = new AuditService();

    /**
     * Tiêu chí 3 [KN-103]: Kiểm tra chỉ Trưởng phòng Nhân sự (HR_MANAGER) hoặc Quản trị hệ thống (ADMIN) mới được truy cập
     */
    private boolean isAuthorized(User user) {
        if (user == null) return false;
        if (user.hasRole("ADMIN")) return true;
        if (user.hasRole("HR_MANAGER")) return true;
        if (user.hasPermission("salary.view")) return true;
        // Các vai trò khác (RECRUITER, HIRING_MANAGER, INTERVIEWER, APPROVER, CANDIDATE...) bị cấm
        return false;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        // [KN-103]: Chặn truy cập nếu không phải Trưởng phòng Nhân sự hoặc Admin
        if (!isAuthorized(currentUser)) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        String action = request.getParameter("action");
        String servletPath = request.getServletPath();

        // 1. API AJAX: Lấy chi tiết dải lương theo ID để điền form sửa
        if ("detail".equalsIgnoreCase(action) || "/salary-ranges/detail".equals(servletPath)) {
            String id = request.getParameter("id");
            SalaryRange sr = salaryRangeService.getSalaryRangeById(id);
            if (sr != null) {
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, sr);
            } else {
                Map<String, Object> err = new HashMap<>();
                err.put("error", "Không tìm thấy dải lương chức danh này");
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_NOT_FOUND, err);
            }
            return;
        }

        // 2. API AJAX: Đối soát mức lương đề xuất với dải lương (Tiêu chí: Hạn mức duyệt offer)
        if ("check-offer".equalsIgnoreCase(action) || "/salary-ranges/check-offer".equals(servletPath)) {
            String rangeId = request.getParameter("rangeId");
            String posTitle = request.getParameter("positionTitle");
            String level = request.getParameter("level");
            String offerStr = request.getParameter("offerSalary");

            try {
                BigDecimal offerSalary = parseSalary(offerStr);
                if (offerSalary == null) {
                    offerSalary = BigDecimal.ZERO;
                }
                OfferLimitResult result;
                if (rangeId != null && !rangeId.trim().isEmpty()) {
                    result = salaryRangeService.checkOfferLimit(rangeId, offerSalary);
                } else {
                    result = salaryRangeService.checkOfferLimitByPosition(posTitle, level, offerSalary);
                }
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, result);
            } catch (Exception e) {
                Map<String, Object> err = new HashMap<>();
                err.put("approved", false);
                err.put("error", "Dữ liệu mức offer không hợp lệ: " + e.getMessage());
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, err);
            }
            return;
        }

        // 3. Hiển thị danh sách và bộ lọc dải lương
        String search = request.getParameter("search");
        String deptId = request.getParameter("deptId");
        String level = request.getParameter("level");

        List<SalaryRange> list = salaryRangeService.getAllSalaryRanges(search, deptId, level);
        request.setAttribute("salaryRanges", list);
        request.setAttribute("departments", departmentDAO.findAll());
        request.setAttribute("levels", salaryRangeService.getDistinctLevels());
        request.setAttribute("paramSearch", search);
        request.setAttribute("paramDeptId", deptId);
        request.setAttribute("paramLevel", level);

        request.getRequestDispatcher("/WEB-INF/views/salary/list.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        // [KN-103]: Chặn thao tác nếu không có quyền
        if (!isAuthorized(currentUser)) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        String servletPath = request.getServletPath();
        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            if ("/salary-ranges/create".equals(servletPath)) action = "create";
            else if ("/salary-ranges/edit".equals(servletPath)) action = "edit";
            else if ("/salary-ranges/delete".equals(servletPath)) action = "delete";
            else action = "create";
        }

        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            if ("delete".equalsIgnoreCase(action)) {
                // Xóa dải lương
                String id = request.getParameter("id");
                SalaryRange sr = salaryRangeService.getSalaryRangeById(id);
                boolean success = salaryRangeService.deleteSalaryRange(id);
                if (success) {
                    auditService.log(currentUser.getId(), "SALARY_RANGE_DELETE", "SALARY_RANGE", id,
                            "Trưởng phòng HR đã xóa dải lương chức danh: " + (sr != null ? sr.getPositionTitle() + " (" + sr.getPositionCode() + ")" : id),
                            ip, userAgent);
                    if (session != null) session.setAttribute("flashSuccess", "Đã xóa dải lương chức danh thành công!");
                } else {
                    if (session != null) session.setAttribute("flashError", "Không thể xóa dải lương. Vui lòng thử lại!");
                }
            } else if ("edit".equalsIgnoreCase(action) || "update".equalsIgnoreCase(action)) {
                // Cập nhật dải lương
                SalaryRange sr = extractSalaryRangeFromRequest(request);
                sr.setId(request.getParameter("id"));

                salaryRangeService.updateSalaryRange(sr);
                auditService.log(currentUser.getId(), "SALARY_RANGE_UPDATE", "SALARY_RANGE", sr.getId(),
                        "Trưởng phòng HR đã cập nhật dải lương chức danh " + sr.getPositionTitle() + " (" + sr.getPositionCode() + ", cấp bậc " + sr.getLevel() + ")",
                        ip, userAgent);
                if (session != null) session.setAttribute("flashSuccess", "Cập nhật dải lương chức danh thành công!");
            } else {
                // Thêm mới dải lương chức danh
                SalaryRange sr = extractSalaryRangeFromRequest(request);

                salaryRangeService.createSalaryRange(sr);
                auditService.log(currentUser.getId(), "SALARY_RANGE_CREATE", "SALARY_RANGE", sr.getId(),
                        "Trưởng phòng HR đã khai báo dải lương mới cho chức danh " + sr.getPositionTitle() + " (" + sr.getPositionCode() + ", cấp bậc " + sr.getLevel() + ")",
                        ip, userAgent);
                if (session != null) session.setAttribute("flashSuccess", "Khai báo dải lương chức danh mới thành công!");
            }
        } catch (IllegalArgumentException e) {
            if (session != null) session.setAttribute("flashError", e.getMessage());
        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", "Lỗi xử lý dải lương: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/salary-ranges");
    }

    private SalaryRange extractSalaryRangeFromRequest(HttpServletRequest request) {
        SalaryRange sr = new SalaryRange();
        sr.setPositionCode(request.getParameter("positionCode"));
        sr.setPositionTitle(request.getParameter("positionTitle"));
        sr.setLevel(request.getParameter("level"));
        
        String deptId = request.getParameter("departmentId");
        sr.setDepartmentId((deptId != null && !deptId.trim().isEmpty()) ? deptId.trim() : null);

        sr.setMinSalary(parseSalary(request.getParameter("minSalary")));
        sr.setMaxSalary(parseSalary(request.getParameter("maxSalary")));

        String curr = request.getParameter("currency");
        sr.setCurrency((curr != null && !curr.trim().isEmpty()) ? curr.trim() : "VND");
        
        sr.setNote(request.getParameter("note"));
        return sr;
    }

    /**
     * Chuyển đổi chuỗi số tiền an toàn, hỗ trợ cả định dạng phân cách phần nghìn (25.000.000 / 25,000,000)
     * và định dạng số thực chuẩn (25000000 hoặc 25000000.00).
     */
    private BigDecimal parseSalary(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        String s = val.trim().replaceAll("\\s+", "");
        if (s.matches("^\\d+(\\.\\d+)?$")) {
            return new BigDecimal(s);
        }
        s = s.replaceAll("[,.]", "");
        if (s.matches("^\\d+$")) {
            return new BigDecimal(s);
        }
        throw new IllegalArgumentException("Định dạng số tiền không hợp lệ: " + val);
    }
}

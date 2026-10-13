package com.irms.controller;

import com.irms.model.RecruiterAssignment;
import com.irms.model.RecruiterAssignmentHistory;
import com.irms.model.RecruitmentRequest;
import com.irms.model.User;
import com.irms.service.RecruiterAssignmentService;
import com.irms.service.RecruitmentRequestService;
import com.irms.service.UserService;
import com.irms.util.JsonUtil;
import com.irms.util.SecurityUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;

/**
 * Controller KN-116: Quản lý Phân bổ Hồ sơ Tuyển dụng
 *
 * Endpoints:
 * GET  /recruitment-requests/assign?id={requisitionId}     → Form phân bổ recruiter
 * POST /recruitment-requests/assign                        → Lưu phân bổ
 * GET  /recruitment-requests/assign/history?id={id}        → Lịch sử chuyển giao (JSON)
 * GET  /recruitment-requests/assign/api/recruiters         → Danh sách recruiter khả dụng (JSON)
 */
@WebServlet(name = "RecruiterAssignmentServlet", urlPatterns = {
        "/recruitment-requests/assign",
        "/recruitment-requests/assign/history",
        "/recruitment-requests/assign/api/recruiters"
})
public class RecruiterAssignmentServlet extends HttpServlet {

    private final RecruiterAssignmentService assignmentService = new RecruiterAssignmentService();
    private final RecruitmentRequestService requestService = new RecruitmentRequestService();
    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        if ("/recruitment-requests/assign/history".equals(path)) {
            handleHistoryApi(request, response);
            return;
        }

        if ("/recruitment-requests/assign/api/recruiters".equals(path)) {
            handleRecruitersApi(request, response);
            return;
        }

        // Mặc định: trang form phân bổ
        showAssignForm(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();

        if ("/recruitment-requests/assign".equals(path)) {
            handleSaveAssignment(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/recruitment-requests");
    }

    // ================================================================
    // FORM PHÂN BỔ
    // ================================================================

    private void showAssignForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String requisitionId = request.getParameter("id");
        if (requisitionId == null || requisitionId.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/recruitment-requests");
            return;
        }

        RecruitmentRequest req = requestService.getRequestById(requisitionId);
        if (req == null) {
            HttpSession session = request.getSession();
            session.setAttribute("flashError", "Không tìm thấy vị trí tuyển dụng!");
            response.sendRedirect(request.getContextPath() + "/recruitment-requests");
            return;
        }

        // Danh sách recruiter đang phụ trách
        List<RecruiterAssignment> assignments = assignmentService.getAssignmentsByRequisition(requisitionId);

        // Lịch sử chuyển giao
        List<RecruiterAssignmentHistory> history = assignmentService.getHistoryByRequisition(requisitionId);

        // Danh sách user khả dụng làm recruiter (ACTIVE)
        List<User> availableRecruiters = userService.getUsers(null, null, "ACTIVE", 1, 500);

        request.setAttribute("reqItem", req);
        request.setAttribute("assignments", assignments);
        request.setAttribute("history", history);
        request.setAttribute("availableRecruiters", availableRecruiters);

        request.getRequestDispatcher("/WEB-INF/views/recruitment/assign.jsp").forward(request, response);
    }

    // ================================================================
    // LƯU PHÂN BỔ
    // ================================================================

    private void handleSaveAssignment(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        // Kiểm tra quyền: chỉ ADMIN và HR_MANAGER mới được phân bổ
        if (currentUser == null || (!currentUser.hasRole("ADMIN") && !currentUser.hasRole("HR_MANAGER")
                && !currentUser.hasPermission("requisitions.assign"))) {
            if (session != null) session.setAttribute("flashError", "Bạn không có quyền thực hiện phân bổ hồ sơ!");
            response.sendRedirect(request.getContextPath() + "/recruitment-requests");
            return;
        }

        String actorId = currentUser.getId();
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        String requisitionId = request.getParameter("requisitionId");
        String primaryRecruiterId = request.getParameter("primaryRecruiterId");
        String changeReason = request.getParameter("changeReason");
        String[] supportIds = request.getParameterValues("supportRecruiterIds");

        if (requisitionId == null || requisitionId.trim().isEmpty()) {
            if (session != null) session.setAttribute("flashError", "Thiếu thông tin vị trí tuyển dụng!");
            response.sendRedirect(request.getContextPath() + "/recruitment-requests");
            return;
        }

        try {
            List<String> supportList = new ArrayList<>();
            if (supportIds != null) {
                for (String sid : supportIds) {
                    if (sid != null && !sid.trim().isEmpty()) supportList.add(sid.trim());
                }
            }

            assignmentService.assignRecruiters(
                    requisitionId.trim(),
                    (primaryRecruiterId != null && !primaryRecruiterId.trim().isEmpty()) ? primaryRecruiterId.trim() : null,
                    supportList,
                    changeReason,
                    actorId, ip, userAgent
            );

            if (session != null) session.setAttribute("flashSuccess", "Phân bổ hồ sơ tuyển dụng thành công!");
        } catch (IllegalArgumentException e) {
            if (session != null) session.setAttribute("flashError", e.getMessage());
        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", "Lỗi hệ thống khi phân bổ: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/recruitment-requests/assign?id=" + requisitionId);
    }

    // ================================================================
    // API: LỊCH SỬ CHUYỂN GIAO (JSON)
    // ================================================================

    private void handleHistoryApi(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String requisitionId = request.getParameter("id");
        if (requisitionId == null || requisitionId.trim().isEmpty()) {
            Map<String, Object> err = new HashMap<>();
            err.put("error", "Thiếu tham số id");
            JsonUtil.sendJsonResponse(response, 400, err);
            return;
        }

        List<RecruiterAssignmentHistory> history = assignmentService.getHistoryByRequisition(requisitionId.trim());
        JsonUtil.sendJsonResponse(response, 200, history);
    }

    // ================================================================
    // API: DANH SÁCH RECRUITER KHẢ DỤNG (JSON)
    // ================================================================

    private void handleRecruitersApi(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String search = request.getParameter("search");
        List<User> users = userService.getUsers(search, null, "ACTIVE", 1, 200);

        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("fullName", u.getFullName());
            item.put("email", u.getEmail());
            item.put("jobTitle", u.getJobTitle());
            item.put("departmentName", u.getDepartmentName());
            result.add(item);
        }
        JsonUtil.sendJsonResponse(response, 200, result);
    }
}

package com.irms.controller;

import com.irms.model.Candidate;
import com.irms.model.SharedCatalogItem;
import com.irms.model.SharedCatalogType;
import com.irms.service.CandidateService;
import com.irms.service.SharedCatalogService;

import com.irms.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

/**
 * ==============================================================================
 * BỘ ĐIỀU KHIỂN HỒ SƠ ỨNG VIÊN & PIPELINE (CandidateServlet)
 * ==============================================================================
 * Phục vụ User Story:
 * - US 5: Đảm bảo Recruiter không xem được ứng viên của vị trí không thuộc mình.
 *   + Chỉ trả về ứng viên của các vị trí do chính Recruiter đó phụ trách nếu user không phải ADMIN hoặc HR_MANAGER.
 *   + Kiểm tra thẩm quyền thao tác sửa đổi ứng viên (candidates.manage) ở tầng Server.
 * ==============================================================================
 */
@WebServlet(name = "CandidateServlet", urlPatterns = {
        "/candidates",
        "/candidates/create",
        "/candidates/update-status"
})
public class CandidateServlet extends HttpServlet {
    private final CandidateService candidateService = new CandidateService();
    private final SharedCatalogService catalogService = new SharedCatalogService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        String search = request.getParameter("search");
        String status = request.getParameter("status");

        // [US 5]: Phân tách dữ liệu ứng viên theo vai trò:
        // Nếu là Chuyên viên tuyển dụng (RECRUITER) và không phải Quản trị viên/Trưởng phòng HR,
        // hệ thống chỉ tải các ứng viên nộp vào vị trí mà nhân sự này phụ trách.
        String recruiterId = null;
        if (currentUser != null && currentUser.hasRole("RECRUITER") && !currentUser.hasRole("ADMIN") && !currentUser.hasRole("HR_MANAGER")) {
            recruiterId = currentUser.getId();
        }

        List<Candidate> list = candidateService.getCandidates(search, status, recruiterId);
        request.setAttribute("candidates", list);
        request.setAttribute("paramSearch", search);
        request.setAttribute("paramStatus", status);
        List<SharedCatalogItem> applicationSources =
                catalogService.getItems(SharedCatalogType.APPLICATION_SOURCE);
        List<SharedCatalogItem> rejectionReasons =
                catalogService.getItems(SharedCatalogType.REJECTION_REASON);
        request.setAttribute("applicationSources", applicationSources);
        request.setAttribute("rejectionReasons", rejectionReasons);

        request.getRequestDispatcher("/WEB-INF/views/candidates/list.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        // [US 5]: Kiểm tra quyền thay đổi trạng thái và thêm mới ứng viên ở tầng Server
        if (currentUser == null || (!currentUser.hasPermission("candidates.manage") && !currentUser.hasRole("ADMIN"))) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        try {
            if ("/candidates/create".equals(path)) {
                Candidate c = new Candidate();
                c.setFullName(request.getParameter("fullName"));
                c.setEmail(request.getParameter("email"));
                c.setPhone(request.getParameter("phone"));
                c.setApplicationSourceId(request.getParameter("applicationSourceId"));
                c.setStatus(request.getParameter("status"));
                c.setCvUrl(request.getParameter("cvUrl"));
                c.setNotes(request.getParameter("notes"));
                if (!candidateService.createCandidate(c)) {
                    throw new IllegalStateException("Không thể thêm hồ sơ ứng viên.");
                }
                if (session != null) session.setAttribute("flashSuccess", "Thêm hồ sơ ứng viên thành công!");
            } else if ("/candidates/update-status".equals(path)) {
                String id = request.getParameter("candidateId");
                String newStatus = request.getParameter("status");
                if (!candidateService.updateStatus(id, newStatus, request.getParameter("rejectionReasonId"))) {
                    throw new IllegalStateException("Không thể cập nhật trạng thái hồ sơ ứng viên.");
                }
                if (session != null) session.setAttribute("flashSuccess", "Đã cập nhật trạng thái ứng viên thành công!");
            }
        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", "Lỗi: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/candidates");
    }
}

package com.irms.controller;

import com.irms.dao.RecruiterAssignmentDAO;
import com.irms.model.Candidate;
import com.irms.model.SharedCatalogItem;
import com.irms.model.SharedCatalogType;
import com.irms.model.User;
import com.irms.service.CandidateService;
import com.irms.service.SharedCatalogService;

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
 * HỒ SƠ ỨNG VIÊN HỢP NHẤT - KN-218 (CandidateProfileServlet)
 * ==============================================================================
 * Phục vụ User Story KN-218: Xem hồ sơ ứng viên hợp nhất
 *
 * Yêu cầu chức năng:
 * 1. Một trang gồm thông tin cá nhân, CV xem trước ngay trong trang,
 *    lịch sử ứng tuyển các vị trí, nhãn, ghi chú.
 * 2. Hiển thị đầy đủ các lần ứng tuyển trước đây của cùng người này (theo email).
 * 3. Chỉ recruiter phụ trách và hiring manager của vị trí đó mới mở được.
 *    Ngoại lệ: ADMIN và HR_MANAGER có thể xem toàn bộ.
 * 4. Cập nhật trạng thái pipeline (APPLIED → SCREENING → INTERVIEWING → OFFER → HIRED | REJECTED)
 *
 * URL: GET  /candidates/profile?id={candidateId}  — Xem hồ sơ
 *      POST /candidates/profile                    — Cập nhật ghi chú hoặc trạng thái
 * ==============================================================================
 */
@WebServlet(name = "CandidateProfileServlet", urlPatterns = {"/candidates/profile"})
public class CandidateProfileServlet extends HttpServlet {

    private final CandidateService candidateService = new CandidateService();
    private final RecruiterAssignmentDAO recruiterAssignmentDAO = new RecruiterAssignmentDAO();
    private final SharedCatalogService catalogService = new SharedCatalogService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        String candidateId = request.getParameter("id");
        if (candidateId == null || candidateId.trim().isEmpty()) {
            request.setAttribute("errorMessage", "Không tìm thấy mã hồ sơ ứng viên.");
            request.getRequestDispatcher("/WEB-INF/views/errors/404.jsp").forward(request, response);
            return;
        }

        // Load hồ sơ chính
        Candidate candidate = candidateService.getCandidateById(candidateId.trim());
        if (candidate == null) {
            request.setAttribute("errorMessage", "Hồ sơ ứng viên không tồn tại hoặc đã bị xóa.");
            request.getRequestDispatcher("/WEB-INF/views/errors/404.jsp").forward(request, response);
            return;
        }

        // [KN-218] Kiểm tra quyền truy cập:
        // - ADMIN và HR_MANAGER được xem toàn bộ
        // - RECRUITER: chỉ xem nếu là recruiter chính hoặc hỗ trợ của vị trí đó
        // - HIRING_MANAGER: chỉ xem nếu là người tạo/hiring manager của yêu cầu tuyển dụng
        boolean canView = false;

        if (currentUser.hasRole("ADMIN") || currentUser.hasRole("HR_MANAGER")) {
            canView = true;
        } else if (candidate.getRequisitionId() != null && !candidate.getRequisitionId().isEmpty()) {
            // Recruiter chính hoặc hỗ trợ
            if (currentUser.hasRole("RECRUITER")) {
                canView = recruiterAssignmentDAO.isRecruiterAssigned(
                        candidate.getRequisitionId(), currentUser.getId());
            }
            // Hiring Manager: kiểm tra qua recruitment_requisitions
            if (!canView && currentUser.hasRole("HIRING_MANAGER")) {
                canView = isHiringManagerOfRequisition(candidate.getRequisitionId(), currentUser.getId());
            }
        }

        if (!canView) {
            request.setAttribute("errorMessage",
                    "Bạn không có quyền xem hồ sơ ứng viên này. " +
                    "Chỉ Recruiter phụ trách và Hiring Manager của vị trí đó mới được phép truy cập.");
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        // [KN-218] Load lịch sử ứng tuyển (tất cả lần của cùng người theo email)
        List<Candidate> applicationHistory = candidateService.getApplicationHistory(candidate.getEmail());

        // Load danh sách lý do từ chối cho dropdown
        List<SharedCatalogItem> rejectionReasons = catalogService.getItems(SharedCatalogType.REJECTION_REASON);

        boolean canEdit = currentUser.hasRole("ADMIN")
                || currentUser.hasRole("HR_MANAGER")
                || currentUser.hasRole("RECRUITER");

        request.setAttribute("candidate", candidate);
        request.setAttribute("applicationHistory", applicationHistory);
        request.setAttribute("rejectionReasons", rejectionReasons);
        request.setAttribute("canEdit", canEdit);

        request.getRequestDispatcher("/WEB-INF/views/candidates/detail.jsp").forward(request, response);
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

        String action = request.getParameter("action");
        String candidateId = request.getParameter("candidateId");

        // [KN-218] Cập nhật ghi chú nội bộ
        if ("update-notes".equals(action) && candidateId != null && !candidateId.trim().isEmpty()) {
            if (!currentUser.hasRole("ADMIN") && !currentUser.hasRole("HR_MANAGER") && !currentUser.hasRole("RECRUITER")) {
                session.setAttribute("flashError", "Bạn không có quyền cập nhật ghi chú ứng viên.");
            } else {
                String notes = request.getParameter("notes");
                try {
                    candidateService.updateNotes(candidateId.trim(), notes);
                    session.setAttribute("flashSuccess", "Đã cập nhật ghi chú thành công!");
                } catch (Exception e) {
                    session.setAttribute("flashError", "Lỗi khi cập nhật ghi chú: " + e.getMessage());
                }
            }
            response.sendRedirect(request.getContextPath() + "/candidates/profile?id=" + candidateId.trim());
            return;
        }

        // [KN-218] Cập nhật trạng thái pipeline từ trang hồ sơ hợp nhất
        if ("update-status".equals(action) && candidateId != null && !candidateId.trim().isEmpty()) {
            // Kiểm tra quyền: chỉ ADMIN, HR_MANAGER, RECRUITER mới được đổi trạng thái
            if (!currentUser.hasRole("ADMIN") && !currentUser.hasRole("HR_MANAGER") && !currentUser.hasRole("RECRUITER")) {
                session.setAttribute("flashError", "Bạn không có quyền thay đổi trạng thái ứng viên.");
            } else {
                String newStatus = request.getParameter("status");
                String rejectionReasonId = request.getParameter("rejectionReasonId");
                try {
                    candidateService.updateStatus(candidateId.trim(), newStatus, rejectionReasonId);
                    session.setAttribute("flashSuccess", "Đã cập nhật trạng thái ứng viên thành công!");
                } catch (Exception e) {
                    session.setAttribute("flashError", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
                }
            }
            response.sendRedirect(request.getContextPath() + "/candidates/profile?id=" + candidateId.trim());
            return;
        }

        response.sendRedirect(request.getContextPath() + "/candidates");
    }

    /**
     * [KN-218]: Kiểm tra xem một user có phải là Hiring Manager của requisition không.
     * Tra vấn cột hiring_manager_id hoặc created_by trong bảng recruitment_requisitions.
     */
    private boolean isHiringManagerOfRequisition(String requisitionId, String userId) {
        if (requisitionId == null || userId == null) return false;
        java.sql.Connection conn = null;
        java.sql.PreparedStatement ps = null;
        java.sql.ResultSet rs = null;
        try {
            conn = com.irms.config.DBConnection.getConnection();
            String sql = "SELECT 1 FROM recruitment_requisitions " +
                         "WHERE id = ? AND (hiring_manager_id = ? OR created_by = ?) LIMIT 1";
            ps = conn.prepareStatement(sql);
            ps.setString(1, requisitionId);
            ps.setString(2, userId);
            ps.setString(3, userId);
            rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            return false;
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            try { if (ps != null) ps.close(); } catch (Exception ignored) {}
            try { if (conn != null) conn.close(); } catch (Exception ignored) {}
        }
    }
}

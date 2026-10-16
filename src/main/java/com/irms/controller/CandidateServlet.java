package com.irms.controller;

import com.irms.model.Candidate;
import com.irms.model.User;
import com.irms.service.AuditService;
import com.irms.service.CandidateService;
import com.irms.util.SecurityUtil;

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
        "/candidates/update-status",
        "/candidates/update-tags",
        "/candidates/save-screening",
        "/candidates/tag-categories/create",
        "/candidates/tags/create"
})
public class CandidateServlet extends HttpServlet {
    private final CandidateService candidateService = new CandidateService();
    private final AuditService auditService = new AuditService();

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

        boolean canManageCandidates = currentUser.hasPermission("candidates.manage") || currentUser.hasRole("ADMIN");
        boolean canManageInternalNotes = canManageCandidates && currentUser.hasRole("RECRUITER");
        List<Candidate> list = candidateService.getCandidates(
                search, status, recruiterId, currentUser.getId(), canManageInternalNotes);
        request.setAttribute("candidates", list);
        request.setAttribute("paramSearch", search);
        request.setAttribute("paramStatus", status);
        request.setAttribute("tagCategories", candidateService.getTagCatalog());
        request.setAttribute("canManageCandidates", canManageCandidates);
        request.setAttribute("canManageInternalNotes", canManageInternalNotes);

        request.getRequestDispatcher("/WEB-INF/views/candidates/list.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        // All screening mutations are restricted to internal users with candidate-management permission.
        if (currentUser == null || (!currentUser.hasPermission("candidates.manage") && !currentUser.hasRole("ADMIN"))) {
            sendForbidden(request, response);
            return;
        }

        try {
            if ("/candidates/create".equals(path)) {
                Candidate c = new Candidate();
                c.setFullName(request.getParameter("fullName"));
                c.setEmail(request.getParameter("email"));
                c.setPhone(request.getParameter("phone"));
                c.setStatus(request.getParameter("status"));
                c.setCvUrl(request.getParameter("cvUrl"));
                c.setNotes(request.getParameter("notes"));
                if (!candidateService.createCandidate(c)) {
                    throw new IllegalStateException("Không thể tạo hồ sơ ứng viên.");
                }
                if (session != null) session.setAttribute("flashSuccess", "Thêm hồ sơ ứng viên thành công!");
            } else if ("/candidates/update-status".equals(path)) {
                String id = request.getParameter("candidateId");
                String newStatus = request.getParameter("status");
                if (!canAccessCandidate(currentUser, id)) {
                    sendForbidden(request, response);
                    return;
                }
                if (!candidateService.updateStatus(id, newStatus)) {
                    throw new IllegalStateException("Không thể cập nhật trạng thái ứng viên.");
                }
                if (session != null) session.setAttribute("flashSuccess", "Đã cập nhật trạng thái ứng viên thành công!");
            } else if ("/candidates/update-tags".equals(path)) {
                String candidateId = request.getParameter("candidateId");
                if (!canAccessCandidate(currentUser, candidateId)) {
                    sendForbidden(request, response);
                    return;
                }
                candidateService.updateCandidateTags(candidateId, request.getParameterValues("tagIds"), currentUser.getId());
                audit(request, currentUser, "CANDIDATE_TAGS_UPDATED", candidateId,
                        "Updated screening labels for a candidate.");
                if (session != null) session.setAttribute("flashSuccess", "Đã cập nhật nhãn sàng lọc.");
            } else if ("/candidates/save-screening".equals(path)) {
                String candidateId = request.getParameter("candidateId");
                if (!canAccessCandidate(currentUser, candidateId)) {
                    sendForbidden(request, response);
                    return;
                }
                String ratingValue = request.getParameter("rating");
                Integer rating = parseRating(ratingValue);
                String note = request.getParameter("internalNote");
                if (note != null && !note.trim().isEmpty() && !currentUser.hasRole("RECRUITER")) {
                    sendForbidden(request, response);
                    return;
                }
                candidateService.saveQuickScreening(
                        candidateId, currentUser.getId(), currentUser.getFullName(), rating, note);
                if (rating != null) {
                    audit(request, currentUser, "CANDIDATE_QUICK_RATING_SAVED", candidateId,
                            "Saved a quick screening rating.");
                }
                if (note != null && !note.trim().isEmpty()) {
                    audit(request, currentUser, "CANDIDATE_INTERNAL_NOTE_ADDED", candidateId,
                            "Added an internal screening note.");
                }
                if (session != null) session.setAttribute("flashSuccess", "Đã lưu đánh giá nhanh hồ sơ.");
            } else if ("/candidates/tag-categories/create".equals(path)) {
                candidateService.createTagCategory(request.getParameter("name"));
                audit(request, currentUser, "CANDIDATE_TAG_CATEGORY_CREATED", null,
                        "Created a screening label category.");
                if (session != null) session.setAttribute("flashSuccess", "Đã tạo danh mục nhãn.");
            } else if ("/candidates/tags/create".equals(path)) {
                candidateService.createTag(request.getParameter("categoryId"), request.getParameter("name"));
                audit(request, currentUser, "CANDIDATE_TAG_CREATED", request.getParameter("categoryId"),
                        "Created a screening label.");
                if (session != null) session.setAttribute("flashSuccess", "Đã tạo nhãn sàng lọc.");
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            if (session != null) session.setAttribute("flashError", "Lỗi: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/candidates");
    }

    private boolean canAccessCandidate(User currentUser, String candidateId) {
        String recruiterId = currentUser.hasRole("RECRUITER")
                && !currentUser.hasRole("ADMIN") && !currentUser.hasRole("HR_MANAGER")
                ? currentUser.getId() : null;
        return candidateService.canAccessCandidate(candidateId, recruiterId);
    }

    private void audit(HttpServletRequest request, User user, String action, String candidateId, String description) {
        if (!auditService.log(user.getId(), action, "CANDIDATE_SCREENING", candidateId, description,
                SecurityUtil.getClientIp(request), request.getHeader("User-Agent"))) {
            throw new IllegalStateException("Không thể ghi nhật ký kiểm toán thao tác.");
        }
    }

    private Integer parseRating(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Điểm đánh giá phải là số nguyên từ 1 đến 5 sao.");
        }
    }

    private void sendForbidden(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
    }
}

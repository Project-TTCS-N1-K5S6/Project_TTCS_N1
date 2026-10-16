package com.irms.controller;

import com.irms.dao.DepartmentDAO;
import com.irms.dao.RecruitmentRequestDAO;
import com.irms.model.Candidate;
import com.irms.model.RecruitmentRequest;
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
    private final RecruitmentRequestDAO requisitionDAO = new RecruitmentRequestDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();

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

        int page = 1;
        int pageSize = 10;
        try {
            if (request.getParameter("page") != null) {
                page = Integer.parseInt(request.getParameter("page"));
            }
        } catch (NumberFormatException ignored) {}

        List<Candidate> list = candidateService.getCandidates(search, status, recruiterId, page, pageSize);
        int totalCandidates = candidateService.countCandidates(search, status, recruiterId);
        int totalPages = (int) Math.ceil((double) totalCandidates / pageSize);

        request.setAttribute("candidates", list);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalCount", totalCandidates);
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("paramSearch", search);
        request.setAttribute("paramStatus", status);
        List<SharedCatalogItem> applicationSources =
                catalogService.getItems(SharedCatalogType.APPLICATION_SOURCE);
        List<SharedCatalogItem> rejectionReasons =
                catalogService.getItems(SharedCatalogType.REJECTION_REASON);
        request.setAttribute("applicationSources", applicationSources);
        request.setAttribute("rejectionReasons", rejectionReasons);
        List<RecruitmentRequest> requisitions = requisitionDAO.findAll(null, null, null);
        request.setAttribute("requisitions", requisitions);
        request.setAttribute("departments", departmentDAO.findAll());

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
                String reqId = request.getParameter("requisitionId");
                if (reqId != null && !reqId.trim().isEmpty()) {
                    c.setRequisitionId(reqId.trim());
                }
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
                String rejectionReasonId = request.getParameter("rejectionReasonId");
                if (!candidateService.updateStatus(id, newStatus, rejectionReasonId)) {
                    throw new IllegalStateException("Không thể cập nhật trạng thái hồ sơ ứng viên.");
                }

                // [KN-286]: Ứng viên bị loại ở vòng cuối được đánh dấu đưa vào kho tiềm năng kèm lý do
                String saveToTalentPool = request.getParameter("saveToTalentPool");
                if ("REJECTED".equals(newStatus) && ("true".equalsIgnoreCase(saveToTalentPool) || "on".equalsIgnoreCase(saveToTalentPool) || "1".equals(saveToTalentPool))) {
                    int rating = 4;
                    try {
                        if (request.getParameter("potentialRating") != null && !request.getParameter("potentialRating").trim().isEmpty()) {
                            rating = Integer.parseInt(request.getParameter("potentialRating").trim());
                        }
                    } catch (NumberFormatException ignored) {}
                    String notes = request.getParameter("potentialNotes");
                    String skills = request.getParameter("potentialSkills");
                    String targetPosition = request.getParameter("targetPosition");
                    String targetDeptId = request.getParameter("targetDepartmentId");

                    if (notes == null || notes.trim().isEmpty()) {
                        notes = "Bị loại ở vòng tuyển dụng cuối, được đánh dấu lưu trữ vào kho tiềm năng để tái liên hệ.";
                    }
                    candidateService.addToTalentPool(id, rating, skills, notes, targetDeptId, targetPosition, currentUser.getId());
                    if (session != null) session.setAttribute("flashSuccess", "Đã cập nhật trạng thái Từ chối và lưu ứng viên vào Kho tiềm năng thành công!");
                } else {
                    if (session != null) session.setAttribute("flashSuccess", "Đã cập nhật trạng thái ứng viên thành công!");
                }
            }
        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", "Lỗi: " + e.getMessage());
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            response.sendRedirect(referer);
        } else {
            response.sendRedirect(request.getContextPath() + "/candidates");
        }
    }
}

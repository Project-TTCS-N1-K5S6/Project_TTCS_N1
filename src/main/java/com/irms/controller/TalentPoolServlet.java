package com.irms.controller;

import com.irms.dao.DepartmentDAO;
import com.irms.dao.RecruitmentRequestDAO;
import com.irms.model.Candidate;
import com.irms.model.Department;
import com.irms.model.RecruitmentRequest;
import com.irms.model.User;
import com.irms.service.CandidateService;

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
 * BỘ ĐIỀU KHIỂN QUẢN LÝ KHO ỨNG VIÊN TIỀM NĂNG (TalentPoolServlet)
 * ==============================================================================
 * Phục vụ phân hệ:
 * - Managing a pool of potential candidates (Quản lý kho ứng viên tiềm năng / Talent Pool)
 * - Tra cứu, phân loại ứng viên theo kỹ năng, độ tiềm năng (1-5 sao), phòng ban mục tiêu
 * - Thêm mới ứng viên vào kho tiềm năng, cập nhật đánh giá, loại khỏi kho
 * - Tái sử dụng ứng viên: Gán vào các yêu cầu tuyển dụng đang mở (Re-engage)
 * ==============================================================================
 */
@WebServlet(name = "TalentPoolServlet", urlPatterns = {
        "/candidates/talent-pool",
        "/candidates/talent-pool/add",
        "/candidates/talent-pool/update",
        "/candidates/talent-pool/remove",
        "/candidates/talent-pool/assign",
        "/candidates/talent-pool/suggested"
})
public class TalentPoolServlet extends HttpServlet {
    private final CandidateService candidateService = new CandidateService();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final RecruitmentRequestDAO requisitionDAO = new RecruitmentRequestDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        // Phân quyền: Cần quyền xem ứng viên
        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        // [KN-286]: API gợi ý ứng viên phù hợp từ kho tiềm năng khi mở vị trí mới
        if ("/candidates/talent-pool/suggested".equals(path)) {
            String reqId = request.getParameter("requisitionId");
            int limit = 15;
            try {
                if (request.getParameter("limit") != null) {
                    limit = Integer.parseInt(request.getParameter("limit").trim());
                }
            } catch (NumberFormatException ignored) {}

            List<Candidate> suggestions = candidateService.getSuggestedTalents(reqId, limit);
            response.setContentType("application/json;charset=UTF-8");
            com.google.gson.Gson gson = new com.google.gson.Gson();
            response.getWriter().write(gson.toJson(suggestions));
            return;
        }

        String search = request.getParameter("search");
        String targetPosition = request.getParameter("targetPosition");
        String skill = request.getParameter("skill");
        String departmentId = request.getParameter("departmentId");
        Integer minRating = null;
        try {
            if (request.getParameter("minRating") != null && !request.getParameter("minRating").trim().isEmpty()) {
                minRating = Integer.parseInt(request.getParameter("minRating").trim());
            }
        } catch (NumberFormatException ignored) {}

        String recruiterId = null;
        if (currentUser.hasRole("RECRUITER") && !currentUser.hasRole("ADMIN") && !currentUser.hasRole("HR_MANAGER")) {
            recruiterId = currentUser.getId();
        }

        int page = 1;
        int pageSize = 12;
        try {
            if (request.getParameter("page") != null) {
                page = Math.max(1, Integer.parseInt(request.getParameter("page")));
            }
        } catch (NumberFormatException ignored) {}

        List<Candidate> talentPool = candidateService.getTalentPool(search, targetPosition, skill, minRating, departmentId, recruiterId, page, pageSize);
        int totalCount = candidateService.countTalentPool(search, targetPosition, skill, minRating, departmentId, recruiterId);
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        int[] stats = candidateService.getTalentPoolStats(recruiterId);

        List<Department> departments = departmentDAO.findAll();
        List<RecruitmentRequest> openRequisitions = requisitionDAO.findOpenRequisitions();

        request.setAttribute("talentPool", talentPool);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalCount", totalCount);
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("paramSearch", search);
        request.setAttribute("paramPosition", targetPosition);
        request.setAttribute("paramSkill", skill);
        request.setAttribute("paramMinRating", minRating);
        request.setAttribute("paramDepartmentId", departmentId);
        request.setAttribute("stats", stats);
        request.setAttribute("departments", departments);
        request.setAttribute("openRequisitions", openRequisitions);

        request.getRequestDispatcher("/WEB-INF/views/candidates/talent-pool.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null || (!currentUser.hasPermission("candidates.manage") && !currentUser.hasRole("ADMIN") && !currentUser.hasRole("HR_MANAGER") && !currentUser.hasRole("RECRUITER"))) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        try {
            if ("/candidates/talent-pool/add".equals(path)) {
                String candidateId = request.getParameter("candidateId");
                String ratingStr = request.getParameter("rating");
                if (ratingStr == null || ratingStr.trim().isEmpty()) {
                    ratingStr = request.getParameter("potentialRating");
                }
                int rating = 3;
                try {
                    if (ratingStr != null) {
                        rating = Integer.parseInt(ratingStr.trim());
                    }
                } catch (NumberFormatException ignored) {}
                String skills = request.getParameter("skills");
                if (skills == null) {
                    skills = request.getParameter("potentialSkills");
                }
                String notes = request.getParameter("notes");
                if (notes == null) {
                    notes = request.getParameter("potentialNotes");
                }
                String targetDeptId = request.getParameter("targetDepartmentId");
                String targetPosition = request.getParameter("targetPosition");

                if (candidateId != null && !candidateId.trim().isEmpty()) {
                    // Thêm ứng viên có sẵn vào kho tiềm năng
                    boolean ok = candidateService.addToTalentPool(candidateId, rating, skills, notes, targetDeptId, targetPosition, currentUser.getId());
                    if (!ok) throw new IllegalStateException("Không thể đưa ứng viên vào kho tiềm năng.");
                    if (session != null) session.setAttribute("flashSuccess", "Đã thêm ứng viên vào Kho tiềm năng thành công!");
                } else {
                    // Tạo mới trực tiếp ứng viên tiềm năng
                    Candidate c = new Candidate();
                    c.setFullName(request.getParameter("fullName"));
                    c.setEmail(request.getParameter("email"));
                    c.setPhone(request.getParameter("phone"));
                    c.setCvUrl(request.getParameter("cvUrl"));
                    c.setPotentialRating(rating);
                    c.setPotentialSkills(skills);
                    c.setPotentialNotes(notes);
                    c.setTargetDepartmentId(targetDeptId);
                    c.setTargetPosition(targetPosition);

                    boolean ok = candidateService.createPotentialCandidate(c, currentUser.getId());
                    if (!ok) throw new IllegalStateException("Không thể lưu ứng viên mới vào kho tiềm năng.");
                    if (session != null) session.setAttribute("flashSuccess", "Thêm mới ứng viên vào Kho tiềm năng thành công!");
                }
            } else if ("/candidates/talent-pool/update".equals(path)) {
                String candidateId = request.getParameter("candidateId");
                int rating = 3;
                try {
                    if (request.getParameter("rating") != null) {
                        rating = Integer.parseInt(request.getParameter("rating").trim());
                    }
                } catch (NumberFormatException ignored) {}
                String skills = request.getParameter("skills");
                String notes = request.getParameter("notes");
                String targetDeptId = request.getParameter("targetDepartmentId");
                String targetPosition = request.getParameter("targetPosition");

                boolean ok = candidateService.updateTalentPool(candidateId, rating, skills, notes, targetDeptId, targetPosition);
                if (!ok) throw new IllegalStateException("Không thể cập nhật thông tin kho tiềm năng.");
                if (session != null) session.setAttribute("flashSuccess", "Cập nhật đánh giá và kỹ năng ứng viên tiềm năng thành công!");
            } else if ("/candidates/talent-pool/remove".equals(path)) {
                String candidateId = request.getParameter("candidateId");
                boolean ok = candidateService.removeFromTalentPool(candidateId);
                if (!ok) throw new IllegalStateException("Không thể loại bỏ ứng viên khỏi kho tiềm năng.");
                if (session != null) session.setAttribute("flashSuccess", "Đã đưa ứng viên ra khỏi Kho tiềm năng.");
            } else if ("/candidates/talent-pool/assign".equals(path)) {
                String candidateId = request.getParameter("candidateId");
                String requisitionId = request.getParameter("requisitionId");
                String notes = request.getParameter("notes");

                boolean ok = candidateService.assignCandidateToRequisition(candidateId, requisitionId, notes, currentUser.getId());
                if (!ok) throw new IllegalStateException("Không thể gán ứng viên vào yêu cầu tuyển dụng.");
                if (session != null) session.setAttribute("flashSuccess", "Tái điều động ứng viên vào vị trí tuyển dụng thành công! Hồ sơ chuyển sang vòng Sàng lọc.");
            }
        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", "Lỗi: " + e.getMessage());
        }

        String redirectUrl = request.getHeader("Referer");
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            response.sendRedirect(redirectUrl);
        } else {
            response.sendRedirect(request.getContextPath() + "/candidates/talent-pool");
        }
    }
}

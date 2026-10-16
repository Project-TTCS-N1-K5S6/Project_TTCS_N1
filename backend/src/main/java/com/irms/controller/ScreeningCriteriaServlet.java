package com.irms.controller;

import com.irms.dao.ScreeningCriteriaDAO;
import com.irms.dao.RecruitmentRequestDAO;
import com.irms.model.CvScoreResult;
import com.irms.model.RecruitmentRequest;
import com.irms.model.ScreeningCriteria;
import com.irms.model.User;
import com.irms.service.CvScoringService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * ==============================================================================
 * BỘ ĐIỀU KHIỂN CHẤM ĐIỂM & XẾP HẠNG HỒ SƠ TỰ ĐỘNG (ScreeningCriteriaServlet)
 * ==============================================================================
 * Phục vụ:
 * - GET  /screening-criteria          → Danh sách tiêu chí sàng lọc đã cấu hình
 * - GET  /screening-criteria/setup    → Form cấu hình tiêu chí cho một vị trí
 * - POST /screening-criteria/save     → Lưu/Cập nhật tiêu chí sàng lọc
 * - POST /screening-criteria/score    → Chấm điểm hàng loạt ứng viên của một vị trí
 * - POST /screening-criteria/score-one → Chấm điểm đơn lẻ một ứng viên
 * - POST /screening-criteria/reset    → Đặt lại điểm toàn bộ ứng viên của vị trí
 * - GET  /screening-criteria/delete   → Xoá tiêu chí sàng lọc
 * ==============================================================================
 */
@WebServlet(name = "ScreeningCriteriaServlet", urlPatterns = {
        "/screening-criteria",
        "/screening-criteria/setup",
        "/screening-criteria/save",
        "/screening-criteria/score",
        "/screening-criteria/score-one",
        "/screening-criteria/reset",
        "/screening-criteria/delete"
})
public class ScreeningCriteriaServlet extends HttpServlet {

    private final ScreeningCriteriaDAO criteriaDAO = new ScreeningCriteriaDAO();
    private final RecruitmentRequestDAO requisitionDAO = new RecruitmentRequestDAO();
    private final CvScoringService scoringService = new CvScoringService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        // Kiểm tra quyền: chỉ ADMIN, HR_MANAGER, RECRUITER mới truy cập được
        if (!currentUser.hasRole("ADMIN") && !currentUser.hasRole("HR_MANAGER")
                && !currentUser.hasRole("RECRUITER")) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        String path = request.getServletPath();

        // Form cấu hình tiêu chí cho một vị trí
        if ("/screening-criteria/setup".equals(path)) {
            String requisitionId = request.getParameter("requisitionId");
            List<RecruitmentRequest> requisitions = requisitionDAO.findAll(null, null, null);
            request.setAttribute("requisitions", requisitions);
            if (requisitionId != null && !requisitionId.trim().isEmpty()) {
                ScreeningCriteria existing = criteriaDAO.findByRequisitionId(requisitionId.trim());
                request.setAttribute("criteria", existing);
                request.setAttribute("selectedRequisitionId", requisitionId.trim());
            }
            request.getRequestDispatcher("/WEB-INF/views/screening/setup.jsp").forward(request, response);
            return;
        }

        // Xoá tiêu chí
        if ("/screening-criteria/delete".equals(path)) {
            String requisitionId = request.getParameter("requisitionId");
            if (requisitionId != null && !requisitionId.trim().isEmpty()) {
                criteriaDAO.deleteByRequisitionId(requisitionId.trim());
                if (session != null) session.setAttribute("flashSuccess", "Đã xoá tiêu chí sàng lọc thành công.");
            }
            response.sendRedirect(request.getContextPath() + "/screening-criteria");
            return;
        }

        // Trang danh sách tiêu chí đã cấu hình
        List<ScreeningCriteria> criteriaList = criteriaDAO.findAll();
        request.setAttribute("criteriaList", criteriaList);
        request.getRequestDispatcher("/WEB-INF/views/screening/list.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        // Kiểm tra quyền ghi
        if (!currentUser.hasRole("ADMIN") && !currentUser.hasRole("HR_MANAGER")
                && !currentUser.hasRole("RECRUITER")) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        String path = request.getServletPath();

        try {
            // =========================================================
            // POST /screening-criteria/save → Lưu/Cập nhật tiêu chí
            // =========================================================
            if ("/screening-criteria/save".equals(path)) {
                String requisitionId = request.getParameter("requisitionId");
                if (requisitionId == null || requisitionId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Vị trí tuyển dụng là bắt buộc.");
                }

                ScreeningCriteria sc = new ScreeningCriteria();
                sc.setRequisitionId(requisitionId.trim());
                sc.setMinExperienceYears(parseIntSafe(request.getParameter("minExperienceYears"), 0));
                sc.setRequiredDegree(trimOrNull(request.getParameter("requiredDegree")));
                sc.setMustHaveSkills(trimOrNull(request.getParameter("mustHaveSkills")));
                sc.setNiceToHaveSkills(trimOrNull(request.getParameter("niceToHaveSkills")));
                sc.setWeightExperience(parseIntSafe(request.getParameter("weightExperience"), 40));
                sc.setWeightSkills(parseIntSafe(request.getParameter("weightSkills"), 40));
                sc.setWeightEducation(parseIntSafe(request.getParameter("weightEducation"), 20));
                sc.setCreatedBy(currentUser.getId());
                sc.setCreatedByName(currentUser.getFullName());

                // Kiểm tra tổng trọng số = 100
                int totalWeight = sc.getWeightExperience() + sc.getWeightSkills() + sc.getWeightEducation();
                if (totalWeight != 100) {
                    throw new IllegalArgumentException("Tổng trọng số 3 hạng mục phải bằng 100 (hiện tại: " + totalWeight + ").");
                }

                boolean ok = criteriaDAO.saveOrUpdate(sc);
                if (!ok) throw new IllegalStateException("Không thể lưu tiêu chí sàng lọc.");

                if (session != null) session.setAttribute("flashSuccess",
                        "Đã lưu tiêu chí sàng lọc thành công! Ứng viên hiện tại có thể được chấm điểm.");
                response.sendRedirect(request.getContextPath() + "/screening-criteria");
                return;
            }

            // =========================================================
            // POST /screening-criteria/score → Chấm điểm hàng loạt
            // =========================================================
            if ("/screening-criteria/score".equals(path)) {
                String requisitionId = request.getParameter("requisitionId");
                if (requisitionId == null || requisitionId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Thiếu vị trí tuyển dụng.");
                }
                int count = scoringService.scoreAllCandidatesForRequisition(
                        requisitionId.trim(),
                        "USER:" + currentUser.getId()
                );
                if (session != null) session.setAttribute("flashSuccess",
                        "Đã chấm điểm & xếp hạng " + count + " hồ sơ ứng viên thành công!");
                response.sendRedirect(request.getContextPath() + "/candidates?requisitionId=" + requisitionId.trim());
                return;
            }

            // =========================================================
            // POST /screening-criteria/score-one → Chấm điểm đơn lẻ (JSON API)
            // =========================================================
            if ("/screening-criteria/score-one".equals(path)) {
                String candidateId = request.getParameter("candidateId");
                if (candidateId == null || candidateId.trim().isEmpty()) {
                    sendJsonError(response, 400, "Thiếu mã ứng viên (candidateId)");
                    return;
                }
                CvScoreResult result = scoringService.scoreOneCandidateAndSave(
                        candidateId.trim(),
                        "USER:" + currentUser.getId()
                );
                if (result == null) {
                    sendJsonError(response, 404, "Không tìm thấy ứng viên hoặc chưa cấu hình tiêu chí sàng lọc cho vị trí này.");
                    return;
                }
                sendJsonResult(response, result);
                return;
            }

            // =========================================================
            // POST /screening-criteria/reset → Reset điểm toàn bộ ứng viên
            // =========================================================
            if ("/screening-criteria/reset".equals(path)) {
                String requisitionId = request.getParameter("requisitionId");
                if (requisitionId == null || requisitionId.trim().isEmpty()) {
                    throw new IllegalArgumentException("Thiếu vị trí tuyển dụng.");
                }
                int count = criteriaDAO.resetScoresForRequisition(requisitionId.trim());
                if (session != null) session.setAttribute("flashSuccess",
                        "Đã đặt lại điểm chấm cho " + count + " hồ sơ. Vui lòng chấm lại khi cần.");
                response.sendRedirect(request.getContextPath() + "/screening-criteria");
                return;
            }

        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", "Lỗi: " + e.getMessage());
            response.sendRedirect(request.getContextPath() + "/screening-criteria");
        }
    }

    // ========== Private helpers ==========

    private int parseIntSafe(String val, int defaultVal) {
        if (val == null || val.trim().isEmpty()) return defaultVal;
        try { return Integer.parseInt(val.trim()); } catch (NumberFormatException e) { return defaultVal; }
    }

    private String trimOrNull(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        return val.trim();
    }

    private void sendJsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(status);
        response.getWriter().write("{\"error\":\"" + escapeJson(message) + "\"}");
    }

    private void sendJsonResult(HttpServletResponse response, CvScoreResult result) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.write(String.format(
            "{\"success\":true,\"candidateId\":\"%s\",\"candidateName\":\"%s\"," +
            "\"totalScore\":%d,\"experienceScore\":%d,\"skillScore\":%d,\"educationScore\":%d," +
            "\"mandatoryFlag\":\"%s\",\"mandatoryFlagLabel\":\"%s\"," +
            "\"recommendation\":\"%s\",\"recommendationLabel\":\"%s\"," +
            "\"experienceDetail\":\"%s\",\"skillDetail\":\"%s\",\"degreeDetail\":\"%s\"," +
            "\"scoreBadgeClass\":\"%s\",\"mandatoryFlagBadgeClass\":\"%s\"}",
            escapeJson(result.getCandidateId()),
            escapeJson(result.getCandidateName()),
            result.getTotalScore(), result.getExperienceScore(), result.getSkillScore(), result.getEducationScore(),
            escapeJson(result.getMandatoryFlag()),
            escapeJson(result.getMandatoryFlagLabel()),
            escapeJson(result.getRecommendation()),
            escapeJson(result.getRecommendationLabel()),
            escapeJson(result.getExperienceDetail()),
            escapeJson(result.getSkillDetail()),
            escapeJson(result.getDegreeDetail()),
            escapeJson(result.getScoreBadgeClass()),
            escapeJson(result.getMandatoryFlagBadgeClass())
        ));
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}

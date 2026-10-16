package com.irms.controller;

import com.irms.dao.RecruitmentRequestDAO;
import com.irms.model.Candidate;
import com.irms.model.CVExtractionResult;
import com.irms.model.RecruitmentRequest;
import com.irms.model.SharedCatalogItem;
import com.irms.model.SharedCatalogType;
import com.irms.service.CandidateService;
import com.irms.service.AuditService;
import com.irms.service.CandidateInteractionService;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ==============================================================================
 * BỘ ĐIỀU KHIỂN HỒ SƠ ỨNG VIÊN & PIPELINE (CandidateServlet)
 * ==============================================================================
 * Phục vụ User Story:
 * - US 5: Đảm bảo Recruiter không xem được ứng viên của vị trí không thuộc mình.
 *   + Chỉ trả về ứng viên của các vị trí do chính Recruiter đó phụ trách nếu user không phải ADMIN hoặc HR_MANAGER.
 *   + Kiểm tra thẩm quyền thao tác sửa đổi ứng viên (candidates.manage) ở tầng Server.
 * - KN-213: Trích xuất thông tin tự động từ CV:
 *   + Recruiter đối chiếu thông tin bóc tách (họ tên, email, SĐT) với thông tin ứng viên.
 *   + Recruiter xác nhận hoặc chỉnh sửa thông tin trước khi cập nhật chính thức vào hồ sơ.
 *   + Hỗ trợ nút bóc tách lại (re-extract) khi cần.
 * ==============================================================================
 */
@WebServlet(name = "CandidateServlet", urlPatterns = {
        "/candidates",
        "/candidates/create",
        "/candidates/update-status",
        "/candidates/confirm-extraction",
        "/candidates/re-extract",
        "/candidates/extraction-details"
})
public class CandidateServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(CandidateServlet.class.getName());
    private final CandidateService candidateService = new CandidateService();
    private final AuditService auditService = new AuditService();
    private final CandidateInteractionService interactionService = new CandidateInteractionService();
    private final SharedCatalogService catalogService = new SharedCatalogService();
    private final RecruitmentRequestDAO requisitionDAO = new RecruitmentRequestDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String servletPath = request.getServletPath();

        // [KN-213]: Endpoint API trả về chi tiết bóc tách CV phục vụ modal Recruiter
        if ("/candidates/extraction-details".equals(servletPath)) {
            handleExtractionDetails(request, response);
            return;
        }

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

        request.getRequestDispatcher("/WEB-INF/views/candidates/list.jsp").forward(request, response);
    }

    private void handleExtractionDetails(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        String id = request.getParameter("id");
        if (id == null || id.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Thiếu mã ứng viên (id)\"}");
            return;
        }

        Candidate c = candidateService.getCandidateById(id.trim());
        if (c == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\": \"Không tìm thấy ứng viên tương ứng\"}");
            return;
        }

        StringBuilder json = new StringBuilder("{");
        json.append("\"id\":\"").append(escapeJson(c.getId())).append("\",");
        json.append("\"fullName\":\"").append(escapeJson(c.getFullName())).append("\",");
        json.append("\"email\":\"").append(escapeJson(c.getEmail())).append("\",");
        json.append("\"phone\":\"").append(escapeJson(c.getPhone())).append("\",");
        json.append("\"cvUrl\":\"").append(escapeJson(c.getCvUrl())).append("\",");
        json.append("\"trackingCode\":\"").append(escapeJson(c.getTrackingCode())).append("\",");
        json.append("\"extractedName\":\"").append(escapeJson(c.getExtractedName())).append("\",");
        json.append("\"extractedEmail\":\"").append(escapeJson(c.getExtractedEmail())).append("\",");
        json.append("\"extractedPhone\":\"").append(escapeJson(c.getExtractedPhone())).append("\",");
        json.append("\"extractionStatus\":\"").append(escapeJson(c.getExtractionStatus())).append("\",");
        json.append("\"extractionStatusLabel\":\"").append(escapeJson(c.getExtractionStatusLabel())).append("\",");
        json.append("\"extractionStatusBadgeClass\":\"").append(escapeJson(c.getExtractionStatusBadgeClass())).append("\"");
        json.append("}");

        response.getWriter().write(json.toString());
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
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
                Candidate candidate = candidateService.getCandidateById(id);
                if (candidate == null) {
                    throw new IllegalArgumentException("Không tìm thấy hồ sơ ứng viên.");
                }
                if (!candidateService.updateStatus(id, newStatus, request.getParameter("rejectionReasonId"))) {
                    throw new IllegalStateException("Không thể cập nhật trạng thái hồ sơ ứng viên.");
                }
                if (newStatus != null && !newStatus.trim().equalsIgnoreCase(candidate.getStatus())) {
                    try {
                        auditService.log(currentUser.getId(), "UPDATE_CANDIDATE_STATUS", "CANDIDATE", id,
                                "Chuyển giai đoạn từ " + candidate.getStatus() + " sang "
                                        + newStatus.trim().toUpperCase() + " cho ứng viên " + candidate.getFullName(),
                                request.getRemoteAddr(), request.getHeader("User-Agent"));
                        interactionService.recordStageChange(id, candidate.getStatus(),
                                newStatus.trim().toUpperCase(), currentUser.getId(), candidate.getFullName());
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Đã đổi trạng thái nhưng không ghi được nhật ký cho ứng viên " + id, e);
                        throw new IllegalStateException("Trạng thái đã đổi nhưng không thể ghi nhật ký tương tác.", e);
                    }
                }
                if (session != null) session.setAttribute("flashSuccess", "Đã cập nhật trạng thái ứng viên thành công!");
            } else if ("/candidates/confirm-extraction".equals(path)) {
                // [KN-213]: Recruiter xác nhận hoặc sửa thông tin đã bóc tách từ CV và áp dụng vào hồ sơ
                String candidateId = request.getParameter("candidateId");
                String confirmedName = request.getParameter("confirmedName");
                String confirmedEmail = request.getParameter("confirmedEmail");
                String confirmedPhone = request.getParameter("confirmedPhone");

                boolean ok = candidateService.confirmAndApplyExtraction(candidateId, confirmedName, confirmedEmail, confirmedPhone);
                if (!ok) {
                    throw new IllegalStateException("Không thể xác nhận thông tin bóc tách từ CV.");
                }
                if (session != null) {
                    session.setAttribute("flashSuccess", "Đã xác nhận và cập nhật thông tin ứng viên từ kết quả trích xuất CV thành công!");
                }
            } else if ("/candidates/re-extract".equals(path)) {
                // [KN-213]: Cho phép bóc tách lại CV
                String candidateId = request.getParameter("candidateId");
                String uploadRoot = getServletContext().getRealPath("");
                if (uploadRoot == null || uploadRoot.trim().isEmpty()) {
                    uploadRoot = System.getProperty("user.dir");
                }
                CVExtractionResult result = candidateService.reExtractCVData(candidateId, uploadRoot);
                if (result == null || "FAILED".equals(result.getStatus())) {
                    if (session != null) {
                        session.setAttribute("flashError", "Không thể bóc tách thông tin từ CV (CV không tồn tại hoặc định dạng PDF không trích xuất được chữ).");
                    }
                } else {
                    if (session != null) {
                        session.setAttribute("flashSuccess", "Đã bóc tách lại thông tin từ CV thành công!");
                    }
                }
            }
        } catch (Exception e) {
            if (session != null) session.setAttribute("flashError", "Lỗi: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/candidates");
    }
}

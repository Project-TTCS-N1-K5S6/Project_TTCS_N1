package com.irms.controller;

import com.irms.dao.RecruitmentRequestDAO;
import com.irms.model.Candidate;
import com.irms.model.CVExtractionResult;
import com.irms.model.RecruitmentRequest;
import com.irms.model.SharedCatalogItem;
import com.irms.model.SharedCatalogType;
import com.irms.model.CandidateDuplicateWarning;
import com.irms.model.User;
import com.irms.service.CandidateDuplicateService;
import com.irms.service.CandidateService;
import com.irms.service.SharedCatalogService;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

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
 * - KN-288: Thêm ứng viên thủ công, kiểm tra trùng lặp và tải lên CV an toàn.
 * ==============================================================================
 */
@WebServlet(name = "CandidateServlet", urlPatterns = {
        "/candidates",
        "/candidates/create",
        "/candidates/update-status",
        "/candidates/confirm-extraction",
        "/candidates/re-extract",
        "/candidates/extraction-details",
        "/api/candidates",
        "/api/candidates/*"
})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 10 * 1024 * 1024,
        maxRequestSize = 12 * 1024 * 1024
)
public class CandidateServlet extends HttpServlet {
    private final CandidateService candidateService = new CandidateService();
    private final SharedCatalogService catalogService = new SharedCatalogService();
    private final RecruitmentRequestDAO requisitionDAO = new RecruitmentRequestDAO();
    private final CandidateDuplicateService duplicateService = new CandidateDuplicateService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String servletPath = request.getServletPath();

        // [KN-288 / KN-347]: Endpoint API danh sách ứng viên JSON
        if ("/api/candidates".equals(servletPath)) {
            String pathInfo = request.getPathInfo();
            if (pathInfo != null && pathInfo.endsWith("/cv")) {
                handleApiCandidateCv(request, response, pathInfo);
                return;
            }
            handleApiCandidatesList(request, response);
            return;
        }

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

        CandidateDuplicateService duplicateService = new CandidateDuplicateService();
        int pendingDuplicateCount = duplicateService.getPendingWarningsCount();
        request.setAttribute("pendingDuplicateCount", pendingDuplicateCount);

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

    private void handleApiCandidatesList(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        List<Candidate> list = candidateService.getCandidates(null, null, null, 1, 30);
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) json.append(",");
            Candidate c = list.get(i);
            json.append("{")
                .append("\"id\":\"").append(escapeJson(c.getId())).append("\",")
                .append("\"name\":\"").append(escapeJson(c.getFullName())).append("\",")
                .append("\"fullName\":\"").append(escapeJson(c.getFullName())).append("\",")
                .append("\"email\":\"").append(escapeJson(c.getEmail())).append("\",")
                .append("\"phone\":\"").append(escapeJson(c.getPhone())).append("\",")
                .append("\"position\":\"").append(escapeJson(c.getRequisitionTitle() != null ? c.getRequisitionTitle() : "")).append("\",")
                .append("\"source\":\"").append(escapeJson(c.getApplicationSourceName() != null ? c.getApplicationSourceName() : "")).append("\",")
                .append("\"cvUrl\":\"").append(escapeJson(c.getCvUrl() != null ? c.getCvUrl() : "")).append("\",")
                .append("\"cvName\":\"").append(escapeJson(c.getCvUrl() != null ? new File(c.getCvUrl()).getName() : "")).append("\"")
                .append("}");
        }
        json.append("]");
        response.getWriter().write(json.toString());
    }

    private void handleApiCandidateCv(HttpServletRequest request, HttpServletResponse response, String pathInfo) throws IOException {
        String candidateId = pathInfo.substring(1, pathInfo.length() - 3);
        if (candidateId.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Candidate c = candidateService.getCandidateById(candidateId);
        if (c == null || c.getCvUrl() == null || c.getCvUrl().trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String uploadRoot = getServletContext().getRealPath("");
        if (uploadRoot == null || uploadRoot.trim().isEmpty()) {
            uploadRoot = System.getProperty("user.dir");
        }
        String cvUrl = c.getCvUrl();
        File file;
        if (cvUrl.startsWith("/") || cvUrl.startsWith("\\")) {
            file = new File(uploadRoot, cvUrl.substring(1).replace("/", File.separator));
        } else {
            file = new File(uploadRoot, cvUrl.replace("/", File.separator));
        }
        if (!file.exists() || !file.isFile()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String ext = file.getName().toLowerCase(Locale.ROOT);
        boolean isPdf = ext.endsWith(".pdf");
        boolean inline = "1".equals(request.getParameter("inline")) && isPdf;
        response.setContentType(isPdf ? "application/pdf" : "application/octet-stream");
        response.setHeader("Content-Disposition", (inline ? "inline" : "attachment") + "; filename=\"" + file.getName() + "\"");
        response.setContentLengthLong(file.length());
        Files.copy(file.toPath(), response.getOutputStream());
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

        boolean isApi = "/api/candidates".equals(path);
        boolean isJson = isApi
                || "application/json".equalsIgnoreCase(request.getHeader("Accept"))
                || "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));

        // [US 5]: Kiểm tra quyền thay đổi trạng thái và thêm mới ứng viên ở tầng Server
        if (!isApi && (currentUser == null || (!currentUser.hasPermission("candidates.manage") && !currentUser.hasRole("ADMIN") && !currentUser.hasRole("HR_MANAGER") && !currentUser.hasRole("RECRUITER")))) {
            if (isJson) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"forbidden\",\"message\":\"Bạn không có quyền thực hiện thao tác này.\"}");
                return;
            }
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        try {
            if ("/candidates/create".equals(path) || "/api/candidates".equals(path)) {
                Candidate c = new Candidate();
                c.setFullName(request.getParameter("fullName"));
                c.setEmail(request.getParameter("email"));
                c.setPhone(request.getParameter("phone"));
                String reqId = request.getParameter("requisitionId");
                if (reqId != null && !reqId.trim().isEmpty()) {
                    c.setRequisitionId(reqId.trim());
                }
                String sourceId = request.getParameter("applicationSourceId");
                if (sourceId != null && !sourceId.trim().isEmpty()) {
                    c.setApplicationSourceId(sourceId.trim());
                } else {
                    String sourceText = request.getParameter("source");
                    if (sourceText != null && !sourceText.trim().isEmpty()) {
                        c.setApplicationSourceName(sourceText.trim());
                    }
                }
                String statusParam = request.getParameter("status");
                c.setStatus(statusParam != null && !statusParam.trim().isEmpty() ? statusParam.trim() : "APPLIED");
                String cvUrlParam = request.getParameter("cvUrl");
                if (cvUrlParam != null && !cvUrlParam.trim().isEmpty()) {
                    c.setCvUrl(cvUrlParam.trim());
                }
                c.setNotes(request.getParameter("notes"));

                Part cvPart = null;
                String reqContentType = request.getContentType();
                if (reqContentType != null && reqContentType.toLowerCase(Locale.ROOT).startsWith("multipart/form-data")) {
                    try {
                        cvPart = request.getPart("cv");
                    } catch (Exception ignored) {}
                }

                String uploadRoot = getServletContext().getRealPath("");
                if (uploadRoot == null || uploadRoot.trim().isEmpty()) {
                    uploadRoot = System.getProperty("user.dir");
                }

                String clientIp = request.getRemoteAddr();
                String userAgent = request.getHeader("User-Agent");
                boolean allowDuplicate = "create_anyway".equals(request.getParameter("duplicateAction"));

                try {
                    Candidate saved = candidateService.createManualCandidate(
                            c, cvPart, uploadRoot, clientIp, userAgent, currentUser, allowDuplicate);

                    if (isJson) {
                        response.setStatus(HttpServletResponse.SC_CREATED);
                        response.setContentType("application/json;charset=UTF-8");
                        StringBuilder sb = new StringBuilder("{");
                        sb.append("\"id\":\"").append(escapeJson(saved.getId())).append("\",");
                        sb.append("\"fullName\":\"").append(escapeJson(saved.getFullName())).append("\",");
                        sb.append("\"name\":\"").append(escapeJson(saved.getFullName())).append("\",");
                        sb.append("\"email\":\"").append(escapeJson(saved.getEmail())).append("\",");
                        sb.append("\"phone\":\"").append(escapeJson(saved.getPhone())).append("\",");
                        sb.append("\"trackingCode\":\"").append(escapeJson(saved.getTrackingCode())).append("\",");
                        sb.append("\"cvUrl\":\"").append(escapeJson(saved.getCvUrl() != null ? saved.getCvUrl() : "")).append("\",");
                        sb.append("\"status\":\"").append(escapeJson(saved.getStatus())).append("\"");
                        sb.append("}");
                        response.getWriter().write(sb.toString());
                        return;
                    }

                    if (session != null) {
                        List<CandidateDuplicateWarning> dups = duplicateService.detectAndRecordDuplicates(saved, currentUser);
                        if (!dups.isEmpty()) {
                            session.setAttribute("flashWarning", "Đã thêm hồ sơ ứng viên! Cảnh báo: Phát hiện " + dups.size() +
                                    " hồ sơ nghi trùng lặp theo Email hoặc Số điện thoại. <a href='" + request.getContextPath() +
                                    "/candidates/duplicates' class='alert-link fw-bold text-decoration-underline'>Xem & Đối chiếu hợp nhất ngay</a>");
                        } else {
                            session.setAttribute("flashSuccess", "Thêm hồ sơ ứng viên thành công! Mã tra cứu: " + saved.getTrackingCode());
                        }
                    }
                    response.sendRedirect(request.getContextPath() + "/candidates");
                    return;

                } catch (CandidateService.DuplicateCandidateException dupEx) {
                    if (isJson) {
                        response.setStatus(HttpServletResponse.SC_CONFLICT);
                        response.setContentType("application/json;charset=UTF-8");
                        StringBuilder sb = new StringBuilder("{");
                        sb.append("\"error\":\"duplicate_candidate\",");
                        sb.append("\"message\":\"").append(escapeJson(dupEx.getMessage())).append("\",");
                        sb.append("\"matches\":[");
                        List<CandidateService.DuplicateMatch> matches = dupEx.getMatches();
                        for (int i = 0; i < matches.size(); i++) {
                            if (i > 0) sb.append(",");
                            CandidateService.DuplicateMatch m = matches.get(i);
                            Candidate ex = m.getCandidate();
                            sb.append("{");
                            sb.append("\"candidate\":{");
                            sb.append("\"id\":\"").append(escapeJson(ex.getId())).append("\",");
                            sb.append("\"fullName\":\"").append(escapeJson(ex.getFullName())).append("\",");
                            sb.append("\"name\":\"").append(escapeJson(ex.getFullName())).append("\",");
                            sb.append("\"email\":\"").append(escapeJson(ex.getEmail())).append("\",");
                            sb.append("\"phone\":\"").append(escapeJson(ex.getPhone())).append("\",");
                            sb.append("\"position\":\"").append(escapeJson(ex.getRequisitionTitle() != null ? ex.getRequisitionTitle() : "")).append("\",");
                            sb.append("\"status\":\"").append(escapeJson(ex.getStatus())).append("\"");
                            sb.append("},");
                            sb.append("\"matchedFields\":[");
                            for (int f = 0; f < m.getMatchedFields().size(); f++) {
                                if (f > 0) sb.append(",");
                                sb.append("\"").append(escapeJson(m.getMatchedFields().get(f))).append("\"");
                            }
                            sb.append("],");
                            sb.append("\"sameName\":").append(m.isSameName()).append(",");
                            sb.append("\"samePosition\":").append(m.isSamePosition());
                            sb.append("}");
                        }
                        sb.append("]}");
                        response.getWriter().write(sb.toString());
                        return;
                    }

                    if (session != null) {
                        session.setAttribute("flashError", dupEx.getMessage());
                    }
                    response.sendRedirect(request.getContextPath() + "/candidates");
                    return;

                } catch (IllegalArgumentException valEx) {
                    if (isJson) {
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"error\":\"validation_error\",\"message\":\"" + escapeJson(valEx.getMessage()) + "\"}");
                        return;
                    }
                    if (session != null) session.setAttribute("flashError", valEx.getMessage());
                    response.sendRedirect(request.getContextPath() + "/candidates");
                    return;
                }
            } else if ("/candidates/update-status".equals(path)) {
                String id = request.getParameter("candidateId");
                String newStatus = request.getParameter("status");
                if (!candidateService.updateStatus(id, newStatus, request.getParameter("rejectionReasonId"))) {
                    throw new IllegalStateException("Không thể cập nhật trạng thái hồ sơ ứng viên.");
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

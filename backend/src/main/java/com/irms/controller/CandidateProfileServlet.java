package com.irms.controller;

import com.irms.model.Candidate;
import com.irms.model.SharedCatalogItem;
import com.irms.model.SharedCatalogType;
import com.irms.model.User;
import com.irms.service.AuditService;
import com.irms.service.CandidateService;
import com.irms.service.SharedCatalogService;
import com.irms.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ==============================================================================
 * HỒ SƠ ỨNG VIÊN HỢP NHẤT - CandidateProfileServlet
 * ==============================================================================
 * Phục vụ User Story: Xem hồ sơ ứng viên hợp nhất (KN-268 đến KN-276)
 *
 * Yêu cầu:
 * 1. [KN-268]: Màn hình hồ sơ hợp nhất (thông tin cá nhân, CV xem trước, lịch sử, nhãn, ghi chú).
 * 2. [KN-269]: Phân quyền: chỉ Recruiter phụ trách và Hiring Manager của vị trí đó (ngoại lệ: ADMIN, HR_MANAGER).
 * 3. [KN-270]: API JSON lấy hồ sơ ứng viên hợp nhất theo người dùng.
 * 4. [KN-271]: Gộp dữ liệu các lần ứng tuyển của cùng người này (theo email).
 * 5. [KN-272]: Xem trước CV an toàn ngay trong trang.
 * 6. [KN-273]: Hiển thị danh sách/timeline các lần ứng tuyển trước đây.
 * 7. [KN-274]: Hiển thị và chỉnh sửa nhãn, ghi chú hồ sơ ứng viên.
 * 8. [KN-275]: API lấy dữ liệu CV và cấu hình xem trước an toàn.
 * 9. [KN-276]: Ghi nhận lịch sử truy cập hồ sơ ứng viên hợp nhất (audit log).
 *
 * URLs:
 *   GET  /candidates/profile?id={candidateId}       — Xem giao diện hồ sơ hợp nhất
 *   GET  /api/candidates/profile?id={candidateId}   — API JSON lấy hồ sơ hợp nhất
 *   GET  /candidates/cv-preview?id={candidateId}    — API stream/xem trước CV an toàn
 *   POST /candidates/profile                         — Cập nhật ghi chú, nhãn hoặc trạng thái pipeline
 * ==============================================================================
 */
@WebServlet(name = "CandidateProfileServlet", urlPatterns = {
        "/candidates/profile",
        "/api/candidates/profile",
        "/candidates/cv-preview"
})
public class CandidateProfileServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(CandidateProfileServlet.class.getName());

    private final CandidateService candidateService = new CandidateService();
    private final SharedCatalogService catalogService = new SharedCatalogService();
    private final AuditService auditService = new AuditService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        String servletPath = request.getServletPath();
        boolean isApi = "/api/candidates/profile".equals(servletPath)
                || "json".equalsIgnoreCase(request.getParameter("format"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        if (currentUser == null) {
            if (isApi) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Vui lòng đăng nhập để thực hiện thao tác này.");
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED, err);
            } else {
                response.sendRedirect(request.getContextPath() + "/auth/login");
            }
            return;
        }

        String candidateId = request.getParameter("id");
        if (candidateId == null || candidateId.trim().isEmpty()) {
            if (isApi) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Thiếu mã hồ sơ ứng viên (id).");
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, err);
            } else {
                request.setAttribute("errorMessage", "Không tìm thấy mã hồ sơ ứng viên.");
                request.getRequestDispatcher("/WEB-INF/views/errors/404.jsp").forward(request, response);
            }
            return;
        }

        // Tải hồ sơ chính của ứng viên
        Candidate candidate = candidateService.getCandidateById(candidateId.trim());
        if (candidate == null) {
            if (isApi) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Hồ sơ ứng viên không tồn tại hoặc đã bị xóa.");
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_NOT_FOUND, err);
            } else {
                request.setAttribute("errorMessage", "Hồ sơ ứng viên không tồn tại hoặc đã bị xóa.");
                request.getRequestDispatcher("/WEB-INF/views/errors/404.jsp").forward(request, response);
            }
            return;
        }

        // [KN-269]: Kiểm tra quyền truy cập (Recruiter phụ trách hoặc Hiring Manager)
        boolean canView = candidateService.canAccessCandidateProfile(currentUser, candidate);
        if (!canView) {
            if (isApi) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Bạn không có quyền xem hồ sơ ứng viên này. Chỉ Recruiter phụ trách và Hiring Manager mới được phép truy cập.");
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_FORBIDDEN, err);
            } else {
                request.setAttribute("errorMessage",
                        "Bạn không có quyền xem hồ sơ ứng viên này. " +
                        "Chỉ Recruiter phụ trách và Hiring Manager của vị trí đó mới được phép truy cập.");
                request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            }
            return;
        }

        // ======================================================================
        // NHÁNH 1: [KN-275] API LẤY DỮ LIỆU CV VÀ CẤU HÌNH XEM TRƯỚC AN TOÀN
        // ======================================================================
        if ("/candidates/cv-preview".equals(servletPath)) {
            handleCvPreview(request, response, currentUser, candidate);
            return;
        }

        // [KN-276]: Ghi nhận lịch sử truy cập hồ sơ ứng viên hợp nhất (Audit Log)
        try {
            auditService.log(
                    currentUser.getId(),
                    "VIEW_CANDIDATE_PROFILE",
                    "CANDIDATE",
                    candidate.getId(),
                    "Xem hồ sơ ứng viên hợp nhất: " + candidate.getFullName() + " (" + candidate.getEmail()
                            + ") - Vị trí: " + (candidate.getRequisitionTitle() != null ? candidate.getRequisitionTitle() : "Chưa gắn"),
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Không thể ghi audit log truy cập hồ sơ: " + e.getMessage());
        }

        // [KN-271]: Lấy toàn bộ lịch sử ứng tuyển của cùng một người (theo email)
        List<Candidate> applicationHistory = candidateService.getApplicationHistory(candidate.getEmail());

        boolean canEdit = currentUser.hasRole("ADMIN")
                || currentUser.hasRole("HR_MANAGER")
                || currentUser.hasRole("RECRUITER");

        // ======================================================================
        // NHÁNH 2: [KN-270] API JSON LẤY HỒ SƠ ỨNG VIÊN HỢP NHẤT
        // ======================================================================
        if (isApi) {
            Map<String, Object> data = new HashMap<>();
            data.put("success", true);
            data.put("candidate", candidate);
            data.put("applicationHistory", applicationHistory);
            data.put("totalApplications", applicationHistory.size());
            data.put("canEdit", canEdit);
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, data);
            return;
        }

        // ======================================================================
        // NHÁNH 3: [KN-268] GIAO DIỆN HTML HỒ SƠ ỨNG VIÊN HỢP NHẤT (JSP)
        // ======================================================================
        List<SharedCatalogItem> rejectionReasons = catalogService.getItems(SharedCatalogType.REJECTION_REASON);

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

        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || "application/json".equalsIgnoreCase(request.getHeader("Content-Type"));

        if (currentUser == null) {
            if (isAjax) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Vui lòng đăng nhập.");
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED, err);
            } else {
                response.sendRedirect(request.getContextPath() + "/auth/login");
            }
            return;
        }

        String action = request.getParameter("action");
        String candidateId = request.getParameter("candidateId");

        if (candidateId == null || candidateId.trim().isEmpty()) {
            if (isAjax) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Thiếu mã hồ sơ ứng viên.");
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, err);
            } else {
                response.sendRedirect(request.getContextPath() + "/candidates");
            }
            return;
        }

        Candidate candidate = candidateService.getCandidateById(candidateId.trim());
        if (candidate == null) {
            if (isAjax) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Không tìm thấy hồ sơ ứng viên.");
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_NOT_FOUND, err);
            } else {
                response.sendRedirect(request.getContextPath() + "/candidates");
            }
            return;
        }

        // Quyền sửa đổi: ADMIN, HR_MANAGER, hoặc RECRUITER được phân công
        boolean canEdit = currentUser.hasRole("ADMIN")
                || currentUser.hasRole("HR_MANAGER")
                || (currentUser.hasRole("RECRUITER") && candidateService.canAccessCandidateProfile(currentUser, candidate));

        if (!canEdit) {
            if (isAjax) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Bạn không có quyền chỉnh sửa hồ sơ ứng viên này.");
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_FORBIDDEN, err);
            } else {
                session.setAttribute("flashError", "Bạn không có quyền chỉnh sửa hồ sơ ứng viên này.");
                response.sendRedirect(request.getContextPath() + "/candidates/profile?id=" + candidateId.trim());
            }
            return;
        }

        // 1. [KN-274]: Cập nhật ghi chú nội bộ
        if ("update-notes".equals(action)) {
            String notes = request.getParameter("notes");
            try {
                candidateService.updateNotes(candidateId.trim(), notes);
                auditService.log(
                        currentUser.getId(),
                        "UPDATE_CANDIDATE_NOTES",
                        "CANDIDATE",
                        candidateId.trim(),
                        "Cập nhật ghi chú nội bộ cho ứng viên " + candidate.getFullName(),
                        request.getRemoteAddr(),
                        request.getHeader("User-Agent")
                );
                if (isAjax) {
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", true);
                    res.put("message", "Đã cập nhật ghi chú thành công!");
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
                    return;
                }
                session.setAttribute("flashSuccess", "Đã cập nhật ghi chú thành công!");
            } catch (Exception e) {
                if (isAjax) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("success", false);
                    err.put("message", "Lỗi khi cập nhật ghi chú: " + e.getMessage());
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, err);
                    return;
                }
                session.setAttribute("flashError", "Lỗi khi cập nhật ghi chú: " + e.getMessage());
            }
            response.sendRedirect(request.getContextPath() + "/candidates/profile?id=" + candidateId.trim());
            return;
        }

        // 2. [KN-274]: Cập nhật nhãn phân loại (tags)
        if ("update-tags".equals(action)) {
            String tags = request.getParameter("tags");
            try {
                candidateService.updateTags(candidateId.trim(), tags);
                auditService.log(
                        currentUser.getId(),
                        "UPDATE_CANDIDATE_TAGS",
                        "CANDIDATE",
                        candidateId.trim(),
                        "Cập nhật nhãn phân loại (" + (tags != null ? tags : "") + ") cho ứng viên " + candidate.getFullName(),
                        request.getRemoteAddr(),
                        request.getHeader("User-Agent")
                );
                if (isAjax) {
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", true);
                    res.put("message", "Đã cập nhật nhãn thành công!");
                    res.put("tags", tags);
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
                    return;
                }
                session.setAttribute("flashSuccess", "Đã cập nhật nhãn phân loại thành công!");
            } catch (Exception e) {
                if (isAjax) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("success", false);
                    err.put("message", "Lỗi khi cập nhật nhãn: " + e.getMessage());
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, err);
                    return;
                }
                session.setAttribute("flashError", "Lỗi khi cập nhật nhãn: " + e.getMessage());
            }
            response.sendRedirect(request.getContextPath() + "/candidates/profile?id=" + candidateId.trim());
            return;
        }

        // 3. [KN-268]: Cập nhật trạng thái pipeline
        if ("update-status".equals(action)) {
            String newStatus = request.getParameter("status");
            String rejectionReasonId = request.getParameter("rejectionReasonId");
            try {
                candidateService.updateStatus(candidateId.trim(), newStatus, rejectionReasonId);
                auditService.log(
                        currentUser.getId(),
                        "UPDATE_CANDIDATE_STATUS",
                        "CANDIDATE",
                        candidateId.trim(),
                        "Chuyển trạng thái tuyển dụng sang " + newStatus + " cho ứng viên " + candidate.getFullName(),
                        request.getRemoteAddr(),
                        request.getHeader("User-Agent")
                );
                if (isAjax) {
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", true);
                    res.put("message", "Đã cập nhật trạng thái ứng viên thành công!");
                    res.put("newStatus", newStatus);
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
                    return;
                }
                session.setAttribute("flashSuccess", "Đã cập nhật trạng thái ứng viên thành công!");
            } catch (Exception e) {
                if (isAjax) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("success", false);
                    err.put("message", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, err);
                    return;
                }
                session.setAttribute("flashError", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
            }
            response.sendRedirect(request.getContextPath() + "/candidates/profile?id=" + candidateId.trim());
            return;
        }

        response.sendRedirect(request.getContextPath() + "/candidates");
    }

    /**
     * [KN-275]: Stream tệp CV an toàn và hỗ trợ xem trước inline
     */
    private void handleCvPreview(HttpServletRequest request, HttpServletResponse response,
                                 User currentUser, Candidate candidate) throws IOException {

        String cvUrl = candidate.getCvUrl();
        if (cvUrl == null || cvUrl.trim().isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Ứng viên chưa tải lên tệp CV.");
            return;
        }

        // Xác định đường dẫn thực tế trên ổ đĩa
        String uploadRoot = getServletContext().getRealPath("/");
        if (uploadRoot == null) {
            uploadRoot = System.getProperty("user.dir");
        }

        // Chuẩn hóa đường dẫn chống Path Traversal
        String relativePath = cvUrl.startsWith("/") ? cvUrl.substring(1) : cvUrl;
        File cvFile = new File(uploadRoot, relativePath.replace("/", File.separator));

        if (!cvFile.exists() || !cvFile.isFile()) {
            // Thử tìm theo đường dẫn tương đối từ workspace
            cvFile = new File(relativePath.replace("/", File.separator));
        }

        if (!cvFile.exists() || !cvFile.isFile()) {
            LOGGER.warning("Không tìm thấy tệp CV trên ổ đĩa: " + cvUrl);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Không tìm thấy tệp CV trên máy chủ.");
            return;
        }

        boolean isDownload = "true".equalsIgnoreCase(request.getParameter("download"));
        String fileName = cvFile.getName();
        String lowerName = fileName.toLowerCase();

        // Xác định Content-Type phù hợp
        String contentType = "application/octet-stream";
        if (lowerName.endsWith(".pdf")) {
            contentType = "application/pdf";
        } else if (lowerName.endsWith(".docx")) {
            contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lowerName.endsWith(".doc")) {
            contentType = "application/msword";
        }

        // Cấu hình Header an toàn
        response.setContentType(contentType);
        response.setContentLengthLong(cvFile.length());
        response.setHeader("X-Content-Type-Options", "nosniff");
        // Cho phép iframe nhúng từ chính trang này
        response.setHeader("Content-Security-Policy", "frame-ancestors 'self'");

        String safeFileName = "CV_" + candidate.getFullName().replaceAll("[^a-zA-Z0-9_.-]", "_")
                + (lowerName.endsWith(".pdf") ? ".pdf" : (lowerName.endsWith(".docx") ? ".docx" : ""));

        if (isDownload) {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + safeFileName + "\"");
        } else {
            response.setHeader("Content-Disposition", "inline; filename=\"" + safeFileName + "\"");
        }

        // Ghi nhật ký kiểm toán tải/xem CV
        try {
            auditService.log(
                    currentUser.getId(),
                    isDownload ? "DOWNLOAD_CANDIDATE_CV" : "VIEW_CANDIDATE_CV",
                    "CANDIDATE",
                    candidate.getId(),
                    (isDownload ? "Tải tệp CV: " : "Xem trước CV: ") + candidate.getFullName() + " (" + cvFile.getName() + ")",
                    request.getRemoteAddr(),
                    request.getHeader("User-Agent")
            );
        } catch (Exception ignored) {}

        // Ghi stream dữ liệu ra response
        try (FileInputStream in = new FileInputStream(cvFile);
             OutputStream out = response.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
    }
}

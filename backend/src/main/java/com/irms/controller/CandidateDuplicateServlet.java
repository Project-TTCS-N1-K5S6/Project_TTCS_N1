package com.irms.controller;

import com.irms.model.Candidate;
import com.irms.model.CandidateComparisonDTO;
import com.irms.model.CandidateDuplicateWarning;
import com.irms.model.User;
import com.irms.service.CandidateDuplicateService;
import com.irms.util.JsonUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ==============================================================================
 * BỘ ĐIỀU KHIỂN CẢNH BÁO, PHÁT HIỆN & HỢP NHẤT HỒ SƠ TRÙNG LẶP (CandidateDuplicateServlet)
 * ==============================================================================
 * Cung cấp đầy đủ các chức năng:
 * 1. Tự động kiểm tra trùng qua API thời gian thực (/api/candidates/check-duplicate)
 * 2. Xem danh sách cảnh báo trùng lặp và số lượng hồ sơ nghi trùng (/candidates/duplicates)
 * 3. Xem và so sánh đối chiếu chi tiết 2 hồ sơ (/candidates/duplicates/compare)
 * 4. Cho phép chọn lọc trường dữ liệu cần giữ lại và hợp nhất an toàn (/candidates/duplicates/merge)
 * 5. Tạm hoãn nhắc lại cảnh báo theo chu kỳ ngày (Snooze) (/candidates/duplicates/snooze)
 * 6. Bỏ qua cảnh báo nếu xác nhận là ứng viên khác nhau (/candidates/duplicates/dismiss)
 * ==============================================================================
 */
@WebServlet(name = "CandidateDuplicateServlet", urlPatterns = {
        "/candidates/duplicates",
        "/candidates/duplicates/compare",
        "/candidates/duplicates/merge",
        "/candidates/duplicates/undo",
        "/candidates/duplicates/snooze",
        "/candidates/duplicates/dismiss",
        "/api/candidates/check-duplicate",
        "/api/candidates/duplicates",
        "/api/candidates/compare",
        "/api/candidates/merge",
        "/api/candidates/merge/undo"
})
public class CandidateDuplicateServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(CandidateDuplicateServlet.class.getName());

    private final CandidateDuplicateService duplicateService = new CandidateDuplicateService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        boolean isApi = path.startsWith("/api/")
                || "json".equalsIgnoreCase(request.getParameter("format"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        // ======================================================================
        // NHÁNH 1: API KIỂM TRA TRÙNG LẶP THỜI GIAN THỰC (/api/candidates/check-duplicate)
        // ======================================================================
        if ("/api/candidates/check-duplicate".equals(path)) {
            String email = request.getParameter("email");
            String phone = request.getParameter("phone");
            String excludeId = request.getParameter("excludeId");

            List<Candidate> duplicates = duplicateService.checkForDuplicates(email, phone, excludeId);
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("isDuplicate", !duplicates.isEmpty());
            res.put("hasDuplicates", !duplicates.isEmpty());
            res.put("count", duplicates.size());
            res.put("matches", duplicates);
            res.put("duplicates", duplicates);
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
            return;
        }

        // Kiểm tra phân quyền truy cập: Cần đăng nhập và có quyền xem ứng viên
        if (currentUser == null) {
            if (isApi) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Vui lòng đăng nhập.");
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED, err);
            } else {
                response.sendRedirect(request.getContextPath() + "/auth/login");
            }
            return;
        }

        boolean canManage = currentUser.hasRole("ADMIN")
                || currentUser.hasRole("HR_MANAGER")
                || currentUser.hasRole("RECRUITER")
                || currentUser.hasPermission("candidates.manage");

        // ======================================================================
        // NHÁNH 2: SO SÁNH & ĐỐI CHIẾU CHI TIẾT HAI HỒ SƠ (/candidates/duplicates/compare)
        // ======================================================================
        if ("/candidates/duplicates/compare".equals(path) || "/api/candidates/compare".equals(path)) {
            String c1 = request.getParameter("c1");
            String c2 = request.getParameter("c2");
            String warningId = request.getParameter("warningId");

            if ((c1 == null || c2 == null) && warningId != null && !warningId.trim().isEmpty()) {
                CandidateDuplicateWarning w = duplicateService.getWarningById(warningId.trim());
                if (w != null) {
                    c1 = w.getCandidateId1();
                    c2 = w.getCandidateId2();
                }
            }

            if (c1 == null || c2 == null) {
                if (isApi) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("success", false);
                    err.put("message", "Thiếu mã hai hồ sơ để so sánh.");
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, err);
                } else {
                    session.setAttribute("flashError", "Thiếu thông tin hồ sơ để so sánh.");
                    response.sendRedirect(request.getContextPath() + "/candidates/duplicates");
                }
                return;
            }

            try {
                CandidateComparisonDTO comparison = duplicateService.getComparison(c1, c2);
                if (isApi) {
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", true);
                    res.put("comparison", comparison);
                    res.put("canManage", canManage);
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
                    return;
                }

                request.setAttribute("comparison", comparison);
                request.setAttribute("warningId", warningId);
                request.setAttribute("canManage", canManage);
                request.getRequestDispatcher("/WEB-INF/views/candidates/compare.jsp").forward(request, response);
                return;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Lỗi đối chiếu hồ sơ: " + e.getMessage(), e);
                if (isApi) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("success", false);
                    err.put("message", "Lỗi: " + e.getMessage());
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, err);
                    return;
                }
                session.setAttribute("flashError", "Lỗi: " + e.getMessage());
                response.sendRedirect(request.getContextPath() + "/candidates/duplicates");
                return;
            }
        }

        // ======================================================================
        // NHÁNH 3: DANH SÁCH CẢNH BÁO TRÙNG LẶP (/candidates/duplicates)
        // ======================================================================
        String search = request.getParameter("search");
        String status = request.getParameter("status");
        if (status == null || status.trim().isEmpty()) {
            status = "PENDING"; // Mặc định hiển thị các cảnh báo đang chờ xử lý
        }

        int page = 1;
        int pageSize = 10;
        try {
            if (request.getParameter("page") != null) {
                page = Integer.parseInt(request.getParameter("page"));
            }
        } catch (NumberFormatException ignored) {}

        List<CandidateDuplicateWarning> list = duplicateService.getWarnings(search, status, page, pageSize);
        int totalWarnings = duplicateService.countWarnings(search, status);
        int totalPages = (int) Math.ceil((double) totalWarnings / pageSize);
        int pendingCount = duplicateService.getPendingWarningsCount();

        if (isApi) {
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("warnings", list);
            res.put("currentPage", page);
            res.put("totalPages", totalPages);
            res.put("totalCount", totalWarnings);
            res.put("pendingCount", pendingCount);
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
            return;
        }

        request.setAttribute("warnings", list);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalCount", totalWarnings);
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("pendingCount", pendingCount);
        request.setAttribute("paramSearch", search);
        request.setAttribute("paramStatus", status);
        request.setAttribute("canManage", canManage);

        request.getRequestDispatcher("/WEB-INF/views/candidates/duplicates.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (request.getContentType() != null && request.getContentType().contains("application/json"))
                || path.startsWith("/api/");

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

        // Kiểm tra thẩm quyền thao tác xử lý trùng / hợp nhất hồ sơ
        boolean canManage = currentUser.hasRole("ADMIN")
                || currentUser.hasRole("HR_MANAGER")
                || currentUser.hasRole("RECRUITER")
                || currentUser.hasPermission("candidates.manage");

        if (!canManage) {
            if (isAjax) {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("message", "Bạn không có quyền thực hiện thao tác hợp nhất hoặc quản lý cảnh báo trùng lặp.");
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_FORBIDDEN, err);
            } else {
                request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            }
            return;
        }

        // ======================================================================
        // THAO TÁC 1: XÁC NHẬN HỢP NHẤT HỒ SƠ (/candidates/duplicates/merge)
        // ======================================================================
        if ("/candidates/duplicates/merge".equals(path) || "/api/candidates/merge".equals(path)) {
            String primaryId = request.getParameter("primaryId");
            String secondaryId = request.getParameter("secondaryId");
            String mergeNotes = request.getParameter("mergeNotes");

            Map<String, String> fieldSelections = new HashMap<>();
            fieldSelections.put("fullName", request.getParameter("fullNameChoice"));
            fieldSelections.put("email", request.getParameter("emailChoice"));
            fieldSelections.put("phone", request.getParameter("phoneChoice"));
            fieldSelections.put("cvUrl", request.getParameter("cvChoice"));
            fieldSelections.put("status", request.getParameter("statusChoice"));
            fieldSelections.put("requisitionId", request.getParameter("requisitionChoice"));
            fieldSelections.put("applicationSourceId", request.getParameter("sourceChoice"));
            fieldSelections.put("notes", request.getParameter("notesChoice"));

            try {
                boolean success = duplicateService.mergeCandidates(primaryId, secondaryId, fieldSelections, mergeNotes, currentUser);
                if (success) {
                    if (isAjax) {
                        Map<String, Object> res = new HashMap<>();
                        res.put("success", true);
                        res.put("message", "Hợp nhất hai hồ sơ thành công!");
                        res.put("primaryId", primaryId);
                        JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
                        return;
                    }
                    if (session != null) {
                        session.setAttribute("flashSuccess", "Hợp nhất hồ sơ thành công! Toàn bộ lịch sử đã được bảo toàn.");
                    }
                    response.sendRedirect(request.getContextPath() + "/candidates/profile?id=" + primaryId);
                    return;
                } else {
                    throw new IllegalStateException("Không thể hoàn tất hợp nhất hồ sơ.");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Lỗi khi hợp nhất hồ sơ: " + e.getMessage(), e);
                if (isAjax) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("success", false);
                    err.put("message", e.getMessage());
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, err);
                    return;
                }
                if (session != null) {
                    session.setAttribute("flashError", "Lỗi hợp nhất: " + e.getMessage());
                }
                response.sendRedirect(request.getContextPath() + "/candidates/duplicates/compare?c1=" + primaryId + "&c2=" + secondaryId);
                return;
            }
        }

        // ======================================================================
        // THAO TÁC 2: TẠM HOÃN NHẮC LẠI CẢNH BÁO (/candidates/duplicates/snooze)
        // ======================================================================
        if ("/candidates/duplicates/snooze".equals(path)) {
            String warningId = request.getParameter("warningId");
            int days = 3;
            try {
                if (request.getParameter("days") != null) {
                    days = Integer.parseInt(request.getParameter("days"));
                }
            } catch (NumberFormatException ignored) {}

            try {
                boolean ok = duplicateService.snoozeWarning(warningId, days, currentUser);
                if (isAjax) {
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", ok);
                    res.put("message", ok ? "Đã tạm hoãn cảnh báo và sẽ nhắc lại sau " + days + " ngày." : "Không thể tạm hoãn cảnh báo.");
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
                    return;
                }
                if (session != null) {
                    session.setAttribute("flashSuccess", "Đã hoãn nhắc lại cảnh báo trùng lặp trong " + days + " ngày.");
                }
            } catch (Exception e) {
                if (isAjax) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("success", false);
                    err.put("message", e.getMessage());
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, err);
                    return;
                }
                if (session != null) {
                    session.setAttribute("flashError", e.getMessage());
                }
            }
            response.sendRedirect(request.getContextPath() + "/candidates/duplicates");
            return;
        }

        // ======================================================================
        // THAO TÁC 3: BỎ QUA CẢNH BÁO TRÙNG LẶP (/candidates/duplicates/dismiss)
        // ======================================================================
        if ("/candidates/duplicates/dismiss".equals(path)) {
            String warningId = request.getParameter("warningId");
            String notes = request.getParameter("notes");
            try {
                boolean ok = duplicateService.dismissWarning(warningId, notes, currentUser);
                if (isAjax) {
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", ok);
                    res.put("message", ok ? "Đã bỏ qua cảnh báo trùng lặp thành công." : "Không thể bỏ qua cảnh báo.");
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
                    return;
                }
                if (session != null) {
                    session.setAttribute("flashSuccess", "Đã xác nhận bỏ qua cảnh báo trùng lặp.");
                }
            } catch (Exception e) {
                if (isAjax) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("success", false);
                    err.put("message", e.getMessage());
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, err);
                    return;
                }
                if (session != null) {
                    session.setAttribute("flashError", e.getMessage());
                }
            }
            response.sendRedirect(request.getContextPath() + "/candidates/duplicates");
            return;
        }

        // ======================================================================
        // THAO TÁC 4: HOÀN TÁC HỢP NHẤT HỒ SƠ TRONG 24 GIỜ (/candidates/duplicates/undo)
        // ======================================================================
        if ("/candidates/duplicates/undo".equals(path) || "/api/candidates/merge/undo".equals(path)) {
            String historyId = request.getParameter("historyId");
            String undoNotes = request.getParameter("undoNotes");

            try {
                boolean success = duplicateService.undoMerge(historyId, undoNotes, currentUser);
                if (success) {
                    if (isAjax) {
                        Map<String, Object> res = new HashMap<>();
                        res.put("success", true);
                        res.put("message", "Hoàn tác hợp nhất hồ sơ thành công! Cả hai hồ sơ đã được khôi phục nguyên trạng.");
                        JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
                        return;
                    }
                    if (session != null) {
                        session.setAttribute("flashSuccess", "Hoàn tác hợp nhất hồ sơ thành công! Cả hai hồ sơ và lịch sử ứng tuyển đã được khôi phục.");
                    }
                } else {
                    throw new IllegalStateException("Không thể hoàn tác hợp nhất hồ sơ.");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Lỗi khi hoàn tác hợp nhất hồ sơ: " + e.getMessage(), e);
                if (isAjax) {
                    Map<String, Object> err = new HashMap<>();
                    err.put("success", false);
                    err.put("message", e.getMessage());
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, err);
                    return;
                }
                if (session != null) {
                    session.setAttribute("flashError", "Lỗi hoàn tác: " + e.getMessage());
                }
            }

            String redirectUrl = request.getParameter("redirectUrl");
            if (redirectUrl != null && !redirectUrl.trim().isEmpty() && !redirectUrl.contains("\n") && !redirectUrl.contains("\r")) {
                response.sendRedirect(redirectUrl);
            } else {
                response.sendRedirect(request.getContextPath() + "/candidates/duplicates?status=MERGED");
            }
            return;
        }

        response.sendRedirect(request.getContextPath() + "/candidates/duplicates");
    }
}

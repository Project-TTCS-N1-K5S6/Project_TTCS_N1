package com.irms.controller;

import com.irms.dao.CandidateDAO;
import com.irms.model.Candidate;
import com.irms.model.SlaConfig;
import com.irms.model.SlaReminderLog;
import com.irms.model.User;
import com.irms.service.AuditService;
import com.irms.service.SlaService;
import com.irms.util.JsonUtil;
import com.irms.util.SecurityUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ==============================================================================
 * BỘ ĐIỀU KHIỂN CẤU HÌNH NGƯỠNG SLA & QUẢN LÝ CẢNH BÁO (KN-364, KN-366)
 * ==============================================================================
 */
@WebServlet(name = "SlaServlet", urlPatterns = {
        "/admin/sla-configs",
        "/admin/sla-configs/update",
        "/admin/sla-configs/scan-now",
        "/admin/sla-configs/send-reminder-now"
})
public class SlaServlet extends HttpServlet {

    private final SlaService slaService = new SlaService();
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final AuditService auditService = new AuditService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        // Phân quyền: ADMIN hoặc HR_MANAGER hoặc có quyền candidates.manage
        if (currentUser == null || (!currentUser.hasRole("ADMIN") && !currentUser.hasRole("HR_MANAGER") && !currentUser.hasPermission("candidates.manage"))) {
            request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            return;
        }

        // 1. Danh sách cấu hình ngưỡng SLA (KN-364)
        List<SlaConfig> configs = slaService.getAllConfigs();

        // 2. Danh sách hồ sơ ứng viên đang bị quá hạn SLA (KN-363)
        List<Candidate> overdueCandidates = candidateDAO.findOverdueCandidates(null);

        // 3. Danh sách nhật ký cảnh báo và nhắc việc (KN-368)
        String search = request.getParameter("search");
        String dateStr = request.getParameter("date");
        Date filterDate = null;
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                filterDate = Date.valueOf(dateStr.trim());
            } catch (Exception ignored) {}
        }

        int page = 1;
        int pageSize = 10;
        try {
            if (request.getParameter("page") != null) {
                page = Integer.parseInt(request.getParameter("page"));
            }
        } catch (NumberFormatException ignored) {}

        List<SlaReminderLog> reminderLogs = slaService.getReminderLogs(search, filterDate, page, pageSize);
        int totalLogs = slaService.countReminderLogs(search, filterDate);
        int totalPages = (int) Math.ceil((double) totalLogs / pageSize);

        // 4. Các chỉ số thống kê tổng quan (KPIs)
        Map<String, Object> stats = slaService.getOverviewStats();

        request.setAttribute("configs", configs);
        request.setAttribute("overdueCandidates", overdueCandidates);
        request.setAttribute("reminderLogs", reminderLogs);
        request.setAttribute("stats", stats);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalCount", totalLogs);
        request.setAttribute("paramSearch", search);
        request.setAttribute("paramDate", dateStr);
        request.setAttribute("activeMenu", "sla-configs");

        request.getRequestDispatcher("/WEB-INF/views/sla/config.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null || (!currentUser.hasRole("ADMIN") && !currentUser.hasRole("HR_MANAGER") && !currentUser.hasPermission("candidates.manage"))) {
            if (isAjax(request)) {
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_FORBIDDEN, Map.of("success", false, "message", "Bạn không có quyền thực hiện thao tác này."));
            } else {
                request.getRequestDispatcher("/WEB-INF/views/errors/403.jsp").forward(request, response);
            }
            return;
        }

        String path = request.getServletPath();

        try {
            if ("/admin/sla-configs/update".equals(path)) {
                // Cập nhật cấu hình ngưỡng SLA (KN-364 & KN-366)
                String id = request.getParameter("id");
                String stageCode = request.getParameter("stageCode");
                String stageName = request.getParameter("stageName");
                int slaDays = Integer.parseInt(request.getParameter("slaDays"));
                int warningDays = Integer.parseInt(request.getParameter("warningThresholdDays"));
                String actionRequired = request.getParameter("actionRequired");
                boolean isActive = "true".equalsIgnoreCase(request.getParameter("isActive"))
                        || "on".equalsIgnoreCase(request.getParameter("isActive"))
                        || "1".equals(request.getParameter("isActive"));

                SlaConfig cfg = new SlaConfig(id, stageCode, stageName, slaDays, warningDays, actionRequired, isActive);
                boolean ok = slaService.updateConfig(cfg, currentUser.getId());

                if (ok) {
                    auditService.log(currentUser.getId(), "SLA_CONFIG_UPDATE", "sla_configurations", id,
                            "Cập nhật ngưỡng SLA giai đoạn " + stageName + ": " + slaDays + " ngày, kích hoạt: " + isActive,
                            SecurityUtil.getClientIp(request), request.getHeader("User-Agent"));
                }

                if (isAjax(request)) {
                    Map<String, Object> res = new HashMap<>();
                    res.put("success", ok);
                    res.put("message", ok ? "Cập nhật cấu hình SLA thành công!" : "Không thể cập nhật cấu hình.");
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
                    return;
                } else {
                    if (session != null) session.setAttribute("flashSuccess", "Cập nhật cấu hình ngưỡng SLA thành công!");
                }

            } else if ("/admin/sla-configs/scan-now".equals(path)) {
                // [KN-362]: Kích hoạt quét tức thì qua giao diện quản trị
                SlaService.ScanSummary summary = slaService.scanAndProcessOverdueCandidates();

                auditService.log(currentUser.getId(), "SLA_MANUAL_SCAN", "sla_configurations", null,
                        String.format("Kích hoạt quét SLA thủ công: Quét %d hồ sơ, phát hiện %d quá hạn, gửi %d nhắc việc.",
                                summary.getTotalScanned(), summary.getOverdueCount(), summary.getRemindersSent()),
                        SecurityUtil.getClientIp(request), request.getHeader("User-Agent"));

                Map<String, Object> res = new HashMap<>();
                res.put("success", true);
                res.put("totalScanned", summary.getTotalScanned());
                res.put("overdueCount", summary.getOverdueCount());
                res.put("remindersSent", summary.getRemindersSent());
                res.put("remindersSkipped", summary.getRemindersSkipped());
                res.put("message", String.format("Đã hoàn tất quét: Phát hiện %d hồ sơ quá hạn, gửi %d thông báo nhắc việc (bỏ qua %d trùng lặp).",
                        summary.getOverdueCount(), summary.getRemindersSent(), summary.getRemindersSkipped()));
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
                return;

            } else if ("/admin/sla-configs/send-reminder-now".equals(path)) {
                // [KN-365]: Gửi nhắc việc thủ công cho 1 ứng viên
                String candidateId = request.getParameter("candidateId");
                Candidate c = candidateDAO.findById(candidateId);
                if (c == null) {
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_NOT_FOUND, Map.of("success", false, "message", "Không tìm thấy hồ sơ ứng viên."));
                    return;
                }

                SlaConfig cfg = slaService.getConfigByStage(c.getStatus());
                if (cfg == null) {
                    JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, Map.of("success", false, "message", "Giai đoạn hồ sơ này không có cấu hình SLA."));
                    return;
                }

                boolean sent = slaService.sendOverdueReminder(c, cfg, c.getDaysInCurrentStage(), new Date(System.currentTimeMillis()));
                Map<String, Object> res = new HashMap<>();
                res.put("success", true);
                res.put("sent", sent);
                res.put("message", sent ? "Đã gửi thông báo & email nhắc việc thành công!" : "Hồ sơ này đã được gửi nhắc việc trong ngày hôm nay rồi (chống gửi trùng).");
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
                return;
            }
        } catch (Exception e) {
            if (isAjax(request)) {
                JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_BAD_REQUEST, Map.of("success", false, "message", "Lỗi: " + e.getMessage()));
                return;
            } else {
                if (session != null) session.setAttribute("flashError", "Lỗi: " + e.getMessage());
            }
        }

        response.sendRedirect(request.getContextPath() + "/admin/sla-configs");
    }

    private boolean isAjax(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith) || (accept != null && accept.contains("application/json"));
    }
}

package com.irms.controller;

import com.irms.model.AuditLog;
import com.irms.service.AuditService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Controller tra cứu Nhật ký kiểm toán bảo mật (Audit Logs)
 */
@WebServlet(name = "AuditLogServlet", urlPatterns = {"/admin/audit-logs"})
public class AuditLogServlet extends HttpServlet {
    private final AuditService auditService = new AuditService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        int page = 1;
        int pageSize = 15;
        try {
            if (request.getParameter("page") != null) {
                page = Integer.parseInt(request.getParameter("page"));
            }
        } catch (NumberFormatException ignored) {}

        List<AuditLog> logs = auditService.getLogs(action, page, pageSize);
        int totalLogs = auditService.countLogs(action);
        int totalPages = (int) Math.ceil((double) totalLogs / pageSize);

        request.setAttribute("logs", logs);
        request.setAttribute("currentPage", page);
        request.setAttribute("totalPages", totalPages);
        request.setAttribute("totalCount", totalLogs);
        request.setAttribute("paramAction", action);

        request.getRequestDispatcher("/WEB-INF/views/audit/list.jsp").forward(request, response);
    }
}

package com.irms.controller;

import com.irms.model.InAppNotification;
import com.irms.model.User;
import com.irms.service.NotificationService;
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

/**
 * Bộ điều khiển Thông báo & Nhắc việc hệ thống (KN-365)
 */
@WebServlet(name = "NotificationServlet", urlPatterns = {
        "/notifications",
        "/api/notifications/unread-count",
        "/api/notifications/list",
        "/api/notifications/mark-read",
        "/api/notifications/mark-all-read"
})
public class NotificationServlet extends HttpServlet {

    private final NotificationService notificationService = new NotificationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            response.sendRedirect(request.getContextPath() + "/auth/login");
            return;
        }

        String path = request.getServletPath();

        if ("/api/notifications/unread-count".equals(path)) {
            int unreadCount = notificationService.getUnreadCount(currentUser.getId());
            Map<String, Object> res = new HashMap<>();
            res.put("unreadCount", unreadCount);
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
            return;
        }

        if ("/api/notifications/list".equals(path)) {
            List<InAppNotification> list = notificationService.getNotificationsForUser(currentUser.getId(), 20);
            int unreadCount = notificationService.getUnreadCount(currentUser.getId());
            Map<String, Object> res = new HashMap<>();
            res.put("notifications", list);
            res.put("unreadCount", unreadCount);
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
            return;
        }

        // Màn hình trang danh sách thông báo
        List<InAppNotification> list = notificationService.getNotificationsForUser(currentUser.getId(), 50);
        int unreadCount = notificationService.getUnreadCount(currentUser.getId());

        request.setAttribute("notifications", list);
        request.setAttribute("unreadCount", unreadCount);
        request.setAttribute("activeMenu", "notifications");

        request.getRequestDispatcher("/WEB-INF/views/notifications/index.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;

        if (currentUser == null) {
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_UNAUTHORIZED, Map.of("success", false));
            return;
        }

        String path = request.getServletPath();

        if ("/api/notifications/mark-read".equals(path)) {
            String id = request.getParameter("id");
            boolean ok = notificationService.markAsRead(id, currentUser.getId());
            int unreadCount = notificationService.getUnreadCount(currentUser.getId());
            Map<String, Object> res = new HashMap<>();
            res.put("success", ok);
            res.put("unreadCount", unreadCount);
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
            return;
        }

        if ("/api/notifications/mark-all-read".equals(path)) {
            boolean ok = notificationService.markAllAsRead(currentUser.getId());
            Map<String, Object> res = new HashMap<>();
            res.put("success", ok);
            res.put("unreadCount", 0);
            JsonUtil.sendJsonResponse(response, HttpServletResponse.SC_OK, res);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/notifications");
    }
}

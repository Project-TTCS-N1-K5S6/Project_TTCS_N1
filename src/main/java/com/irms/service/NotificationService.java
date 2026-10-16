package com.irms.service;

import com.irms.dao.NotificationDAO;
import com.irms.model.InAppNotification;

import java.util.List;

/**
 * Quản lý nghiệp vụ Thông báo nội bộ hệ thống (KN-365)
 */
public class NotificationService {
    private final NotificationDAO notificationDAO = new NotificationDAO();

    public boolean createNotification(InAppNotification notif) {
        if (notif == null || notif.getUserId() == null || notif.getUserId().trim().isEmpty()) {
            return false;
        }
        return notificationDAO.insert(notif);
    }

    public List<InAppNotification> getNotificationsForUser(String userId, int limit) {
        if (userId == null || userId.trim().isEmpty()) return java.util.Collections.emptyList();
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return notificationDAO.findByUserId(userId.trim(), safeLimit);
    }

    public int getUnreadCount(String userId) {
        if (userId == null || userId.trim().isEmpty()) return 0;
        return notificationDAO.countUnreadByUserId(userId.trim());
    }

    public boolean markAsRead(String id, String userId) {
        if (id == null || userId == null) return false;
        return notificationDAO.markAsRead(id.trim(), userId.trim());
    }

    public boolean markAllAsRead(String userId) {
        if (userId == null || userId.trim().isEmpty()) return false;
        return notificationDAO.markAllAsRead(userId.trim());
    }
}

package com.irms.service;

import com.irms.dao.AuditDAO;
import com.irms.model.AuditLog;
import com.irms.util.SecurityUtil;

import java.util.List;

/**
 * Xử lý nghiệp vụ Nhật ký kiểm toán bảo mật (Audit Logs)
 */
public class AuditService {
    private final AuditDAO auditDAO = new AuditDAO();

    public List<AuditLog> getLogs(String action, int page, int pageSize) {
        int offset = Math.max(0, (page - 1) * pageSize);
        return auditDAO.findAll(action, offset, pageSize);
    }

    public int countLogs(String action) {
        return auditDAO.countAll(action);
    }

    public void log(String userId, String action, String entityType, String entityId, String description, String ip, String userAgent) {
        AuditLog log = new AuditLog();
        log.setId(SecurityUtil.generateUUID());
        log.setUserId(userId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDescription(description);
        log.setIpAddress(ip);
        log.setUserAgent(userAgent);
        auditDAO.insert(log);
    }
}

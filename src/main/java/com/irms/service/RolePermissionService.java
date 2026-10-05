package com.irms.service;

import com.irms.dao.AuditDAO;
import com.irms.dao.PermissionDAO;
import com.irms.dao.RoleDAO;
import com.irms.model.AuditLog;
import com.irms.model.Permission;
import com.irms.model.Role;
import com.irms.util.SecurityUtil;

import java.util.*;

/**
 * Xử lý nghiệp vụ Vai trò & Ma trận phân quyền 10 phân hệ
 */
public class RolePermissionService {
    private final RoleDAO roleDAO = new RoleDAO();
    private final PermissionDAO permissionDAO = new PermissionDAO();
    private final AuditDAO auditDAO = new AuditDAO();

    public List<Role> getAllRoles() {
        return roleDAO.findAll();
    }

    public List<Permission> getAllPermissions() {
        return permissionDAO.findAll();
    }

    public Map<String, List<Permission>> getPermissionsGroupedByModule() {
        List<Permission> all = permissionDAO.findAll();
        Map<String, List<Permission>> grouped = new LinkedHashMap<>();
        for (Permission p : all) {
            grouped.computeIfAbsent(p.getModule(), k -> new ArrayList<>()).add(p);
        }
        return grouped;
    }

    public Map<String, Set<String>> getRolePermissionsMatrix() {
        return permissionDAO.getRolePermissionsMap();
    }

    public boolean updateRolePermissions(String roleId, List<String> permissionIds, String adminId, String ip, String userAgent) {
        boolean success = permissionDAO.updateRolePermissions(roleId, permissionIds);
        if (success) {
            AuditLog log = new AuditLog();
            log.setId(SecurityUtil.generateUUID());
            log.setUserId(adminId);
            log.setAction("ROLE_PERMISSIONS_UPDATED");
            log.setEntityType("ROLE");
            log.setEntityId(roleId);
            log.setDescription("Cập nhật ma trận phân quyền cho vai trò ID: " + roleId + " (Số quyền: " + (permissionIds != null ? permissionIds.size() : 0) + ")");
            log.setIpAddress(ip);
            log.setUserAgent(userAgent);
            auditDAO.insert(log);
        }
        return success;
    }
}

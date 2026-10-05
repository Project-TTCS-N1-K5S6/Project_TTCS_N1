package com.irms.controller;

import com.irms.model.Permission;
import com.irms.model.Role;
import com.irms.model.User;
import com.irms.service.RolePermissionService;
import com.irms.util.JsonUtil;
import com.irms.util.SecurityUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;

/**
 * Controller quản lý Ma trận Phân quyền 10 Phân hệ nghiệp vụ IRMS
 */
@WebServlet(name = "PermissionMatrixServlet", urlPatterns = {"/admin/permissions"})
public class PermissionMatrixServlet extends HttpServlet {
    private final RolePermissionService rolePermissionService = new RolePermissionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        List<Role> roles = rolePermissionService.getAllRoles();
        Map<String, List<Permission>> groupedPermissions = rolePermissionService.getPermissionsGroupedByModule();
        Map<String, Set<String>> rolePermissionsMap = rolePermissionService.getRolePermissionsMatrix();

        request.setAttribute("roles", roles);
        request.setAttribute("groupedPermissions", groupedPermissions);
        request.setAttribute("rolePermissionsMap", rolePermissionsMap);

        request.getRequestDispatcher("/WEB-INF/views/permissions/matrix.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        String adminId = (currentUser != null) ? currentUser.getId() : "SYSTEM";
        String ip = SecurityUtil.getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        String roleId = request.getParameter("roleId");
        String[] permissionIds = request.getParameterValues("permissionIds");

        List<String> permList = permissionIds != null ? Arrays.asList(permissionIds) : Collections.<String>emptyList();
        boolean success = rolePermissionService.updateRolePermissions(roleId, permList, adminId, ip, userAgent);

        // Hỗ trợ cả AJAX và Form Submit truyền thống
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            Map<String, Object> resMap = new HashMap<>();
            resMap.put("success", success);
            resMap.put("message", success ? "Đã lưu cấu hình ma trận phân quyền thành công!" : "Lỗi lưu ma trận phân quyền.");
            JsonUtil.sendJsonResponse(response, success ? 200 : 500, resMap);
        } else {
            session.setAttribute("flashSuccess", "Đã cập nhật phân quyền cho vai trò thành công!");
            response.sendRedirect(request.getContextPath() + "/admin/permissions");
        }
    }
}

'use strict';
const PermissionModel = require('../models/permissionModel');
const PermissionService = require('../services/permissionService');

/**
 * RbacController - Permission management API
 * All endpoints require 'permission.manage' permission (quan_tri only)
 */
class RbacController {

  /**
   * GET /api/v1/rbac/roles
   * List all roles with their permissions
   */
  static async getRoles(req, res) {
    try {
      const roles = await PermissionModel.getAllRolesWithPermissions();
      return res.status(200).json({
        success: true,
        roles
      });
    } catch (err) {
      console.error('[RbacController.getRoles] Error:', err);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi tải danh sách vai trò.'
      });
    }
  }

  /**
   * GET /api/v1/rbac/permissions
   * List all available permissions
   */
  static async getPermissions(req, res) {
    try {
      const permissions = await PermissionModel.getAllPermissions();
      return res.status(200).json({
        success: true,
        permissions
      });
    } catch (err) {
      console.error('[RbacController.getPermissions] Error:', err);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi tải danh sách quyền.'
      });
    }
  }

  /**
   * GET /api/v1/rbac/roles/:roleCode/permissions
   * Get permissions for a specific role
   */
  static async getRolePermissions(req, res) {
    try {
      const { roleCode } = req.params;
      const perms = await PermissionService.getPermissionsForRole(roleCode);
      return res.status(200).json({
        success: true,
        roleCode,
        permissions: perms
      });
    } catch (err) {
      console.error('[RbacController.getRolePermissions] Error:', err);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi tải quyền của vai trò.'
      });
    }
  }

  /**
   * PUT /api/v1/rbac/roles/:roleCode/permissions
   * Replace all permissions for a role (bulk update)
   * Body: { permissions: ['candidate.view', 'salary.view', ...] }
   */
  static async setRolePermissions(req, res) {
    try {
      const { roleCode } = req.params;
      const { permissions } = req.body;

      if (!Array.isArray(permissions)) {
        return res.status(400).json({
          success: false,
          message: 'Danh sách quyền phải là một mảng.'
        });
      }

      // Prevent removing own manage permission (safety guard)
      if (roleCode === 'quan_tri' && !permissions.includes('permission.manage')) {
        return res.status(400).json({
          success: false,
          message: 'Không thể xóa quyền "Quản lý phân quyền" khỏi vai trò Quản trị hệ thống.'
        });
      }

      await PermissionModel.setPermissionsForRole(roleCode, permissions, req.user.id);

      console.log(`[RBAC] Permissions updated for role=${roleCode} by user=${req.user.id}`);

      return res.status(200).json({
        success: true,
        message: `Đã cập nhật quyền cho vai trò '${roleCode}' thành công.`,
        roleCode,
        permissions
      });

    } catch (err) {
      console.error('[RbacController.setRolePermissions] Error:', err);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi cập nhật phân quyền.'
      });
    }
  }

  /**
   * POST /api/v1/rbac/roles/:roleCode/permissions/:permCode
   * Grant a single permission to a role
   */
  static async grantPermission(req, res) {
    try {
      const { roleCode, permCode } = req.params;
      await PermissionModel.grantPermission(roleCode, permCode, req.user.id);

      return res.status(200).json({
        success: true,
        message: `Đã cấp quyền '${permCode}' cho vai trò '${roleCode}'.`
      });

    } catch (err) {
      console.error('[RbacController.grantPermission] Error:', err);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi cấp quyền.'
      });
    }
  }

  /**
   * DELETE /api/v1/rbac/roles/:roleCode/permissions/:permCode
   * Revoke a single permission from a role
   */
  static async revokePermission(req, res) {
    try {
      const { roleCode, permCode } = req.params;

      // Safety: don't let admin remove their own permission management
      if (roleCode === 'quan_tri' && permCode === 'permission.manage') {
        return res.status(400).json({
          success: false,
          message: 'Không thể thu hồi quyền "Quản lý phân quyền" của vai trò Quản trị hệ thống.'
        });
      }

      await PermissionModel.revokePermission(roleCode, permCode);

      return res.status(200).json({
        success: true,
        message: `Đã thu hồi quyền '${permCode}' khỏi vai trò '${roleCode}'.`
      });

    } catch (err) {
      console.error('[RbacController.revokePermission] Error:', err);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi thu hồi quyền.'
      });
    }
  }

  /**
   * GET /api/v1/rbac/my-permissions
   * Get current user's permissions (used by FE to render UI)
   */
  static async getMyPermissions(req, res) {
    try {
      const permissions = await PermissionService.getPermissionsForRole(req.user.role);
      return res.status(200).json({
        success: true,
        role: req.user.role,
        permissions,
        meta: {
          canViewSalary: permissions.includes('salary.view'),
          canManagePermissions: permissions.includes('permission.manage'),
          hasRecruiterScope: PermissionService.hasRecruiterScope(req.user.role),
        }
      });
    } catch (err) {
      console.error('[RbacController.getMyPermissions] Error:', err);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi tải quyền người dùng.'
      });
    }
  }
}

module.exports = RbacController;

'use strict';
const PermissionModel = require('../models/permissionModel');

/**
 * PermissionService - Core RBAC logic
 *
 * Centralized service for checking permissions.
 * All authorization decisions go through this service.
 *
 * Default deny: if role/permission not found -> deny
 */
class PermissionService {

  /**
   * Check if a role has a specific permission
   * @param {string} roleCode - e.g. 'nguoi_phong_van'
   * @param {string} permissionCode - e.g. 'salary.view'
   * @returns {Promise<boolean>}
   */
  static async hasPermission(roleCode, permissionCode) {
    if (!roleCode || !permissionCode) return false;

    try {
      const perms = await PermissionModel.getPermissionsForRole(roleCode);
      return perms.has(permissionCode);
    } catch (err) {
      console.error('[PermissionService.hasPermission] Error:', err.message);
      return false; // Default deny on error
    }
  }

  /**
   * Check if a role has ANY of the given permissions
   * @param {string} roleCode
   * @param {string[]} permissionCodes
   * @returns {Promise<boolean>}
   */
  static async hasAnyPermission(roleCode, permissionCodes) {
    if (!roleCode || !permissionCodes?.length) return false;

    try {
      const perms = await PermissionModel.getPermissionsForRole(roleCode);
      return permissionCodes.some(code => perms.has(code));
    } catch (err) {
      console.error('[PermissionService.hasAnyPermission] Error:', err.message);
      return false;
    }
  }

  /**
   * Check if a role has ALL of the given permissions
   * @param {string} roleCode
   * @param {string[]} permissionCodes
   * @returns {Promise<boolean>}
   */
  static async hasAllPermissions(roleCode, permissionCodes) {
    if (!roleCode || !permissionCodes?.length) return false;

    try {
      const perms = await PermissionModel.getPermissionsForRole(roleCode);
      return permissionCodes.every(code => perms.has(code));
    } catch (err) {
      console.error('[PermissionService.hasAllPermissions] Error:', err.message);
      return false;
    }
  }

  /**
   * Check if a user can view salary information
   * @param {string} roleCode
   * @returns {Promise<boolean>}
   */
  static async canViewSalary(roleCode) {
    return PermissionService.hasPermission(roleCode, 'salary.view');
  }

  /**
   * Check if a user can manage system permissions
   * @param {string} roleCode
   * @returns {Promise<boolean>}
   */
  static async canManagePermissions(roleCode) {
    return PermissionService.hasPermission(roleCode, 'permission.manage');
  }

  /**
   * Check if this role has recruiter data scope restriction
   * Only 'chuyen_vien_tuyen_dung' has this restriction
   * @param {string} roleCode
   * @returns {boolean}
   */
  static hasRecruiterScope(roleCode) {
    return roleCode === 'chuyen_vien_tuyen_dung';
  }

  /**
   * Get all permissions for a role (for API response)
   * @param {string} roleCode
   * @returns {Promise<string[]>}
   */
  static async getPermissionsForRole(roleCode) {
    try {
      const perms = await PermissionModel.getPermissionsForRole(roleCode);
      return Array.from(perms);
    } catch (err) {
      console.error('[PermissionService.getPermissionsForRole] Error:', err.message);
      return [];
    }
  }

  /**
   * Build user permission context (attached to req.user.permissions)
   * @param {string} roleCode
   * @returns {Promise<Object>}
   */
  static async buildUserContext(roleCode) {
    const permissions = await PermissionService.getPermissionsForRole(roleCode);
    return {
      permissions,
      canViewSalary: permissions.includes('salary.view'),
      canManagePermissions: permissions.includes('permission.manage'),
      hasRecruiterScope: PermissionService.hasRecruiterScope(roleCode),
    };
  }
}

module.exports = PermissionService;

'use strict';
const PermissionService = require('../services/permissionService');

/**
 * RBAC Authorization Middleware
 *
 * Principles:
 * 1. Default DENY - if permission not found, deny.
 * 2. Check at SERVER level - never trust client headers.
 * 3. All error messages in Vietnamese.
 * 4. Log technical details server-side, show friendly messages to users.
 */

/**
 * Middleware factory: require a specific permission
 *
 * Usage:
 *   router.get('/candidates', authMiddleware, requirePermission('candidate.view'), controller)
 *
 * @param {string} permissionCode - e.g. 'candidate.view', 'salary.view'
 * @param {string} [customMessage] - custom 403 message (optional)
 */
function requirePermission(permissionCode, customMessage) {
  return async (req, res, next) => {
    // Must be authenticated first
    if (!req.user) {
      return res.status(401).json({
        success: false,
        code: 'UNAUTHORIZED',
        message: 'Bạn chưa đăng nhập. Vui lòng đăng nhập để tiếp tục.'
      });
    }

    const userRole = req.user.role;

    // Deny if no role
    if (!userRole) {
      console.warn(`[RBAC] User ${req.user.id} has no role - denying access to ${permissionCode}`);
      return res.status(403).json({
        success: false,
        code: 'FORBIDDEN',
        message: 'Tài khoản của bạn chưa được gán vai trò. Vui lòng liên hệ quản trị viên.'
      });
    }

    try {
      const allowed = await PermissionService.hasPermission(userRole, permissionCode);

      if (!allowed) {
        // Log technical details
        console.warn(`[RBAC] DENIED: user=${req.user.id} role=${userRole} permission=${permissionCode} path=${req.path}`);

        // Return friendly Vietnamese message
        const message = customMessage || getDefaultDeniedMessage(permissionCode);
        return res.status(403).json({
          success: false,
          code: 'FORBIDDEN',
          message
        });
      }

      next();
    } catch (err) {
      console.error('[RBAC.requirePermission] Error checking permission:', err.message);
      return res.status(500).json({
        success: false,
        code: 'SERVER_ERROR',
        message: 'Lỗi hệ thống khi kiểm tra quyền truy cập.'
      });
    }
  };
}

/**
 * Middleware factory: require ANY of the given permissions
 * @param {string[]} permissionCodes
 */
function requireAnyPermission(permissionCodes) {
  return async (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({
        success: false,
        code: 'UNAUTHORIZED',
        message: 'Bạn chưa đăng nhập. Vui lòng đăng nhập để tiếp tục.'
      });
    }

    const userRole = req.user.role;
    if (!userRole) {
      return res.status(403).json({
        success: false,
        code: 'FORBIDDEN',
        message: 'Tài khoản của bạn chưa được gán vai trò.'
      });
    }

    try {
      const allowed = await PermissionService.hasAnyPermission(userRole, permissionCodes);
      if (!allowed) {
        console.warn(`[RBAC] DENIED: user=${req.user.id} role=${userRole} requires ANY of [${permissionCodes.join(',')}] path=${req.path}`);
        return res.status(403).json({
          success: false,
          code: 'FORBIDDEN',
          message: 'Bạn không có quyền thực hiện chức năng này.'
        });
      }
      next();
    } catch (err) {
      console.error('[RBAC.requireAnyPermission] Error:', err.message);
      return res.status(500).json({
        success: false,
        code: 'SERVER_ERROR',
        message: 'Lỗi hệ thống khi kiểm tra quyền truy cập.'
      });
    }
  };
}

/**
 * Middleware: attach user's permissions to req.user
 * Use this before routes that need to filter data based on permissions.
 *
 * Attaches:
 *   req.user.permissions    -> string[]
 *   req.user.canViewSalary  -> boolean
 *   req.user.canManagePermissions -> boolean
 *   req.user.hasRecruiterScope   -> boolean
 */
async function attachPermissions(req, res, next) {
  if (!req.user) {
    return next();
  }

  try {
    const ctx = await PermissionService.buildUserContext(req.user.role);
    req.user = { ...req.user, ...ctx };
    next();
  } catch (err) {
    console.error('[RBAC.attachPermissions] Error:', err.message);
    // Non-fatal: continue without permissions attached
    req.user.permissions = [];
    req.user.canViewSalary = false;
    req.user.canManagePermissions = false;
    req.user.hasRecruiterScope = false;
    next();
  }
}

/**
 * Get default Vietnamese error message for a permission code
 */
function getDefaultDeniedMessage(permissionCode) {
  const messages = {
    'salary.view':            'Bạn không có quyền xem thông tin dải lương.',
    'permission.manage':      'Bạn không có quyền quản lý phân quyền hệ thống.',
    'candidate.view':         'Bạn không có quyền xem danh sách ứng viên.',
    'candidate.create':       'Bạn không có quyền tạo hồ sơ ứng viên.',
    'candidate.update':       'Bạn không có quyền cập nhật hồ sơ ứng viên.',
    'candidate.delete':       'Bạn không có quyền xóa hồ sơ ứng viên.',
    'candidate.approve':      'Bạn không có quyền phê duyệt ứng viên.',
    'candidate.export':       'Bạn không có quyền xuất danh sách ứng viên.',
    'job.view':               'Bạn không có quyền xem vị trí tuyển dụng.',
    'job.create':             'Bạn không có quyền tạo vị trí tuyển dụng.',
    'job.update':             'Bạn không có quyền cập nhật vị trí tuyển dụng.',
    'job.delete':             'Bạn không có quyền xóa vị trí tuyển dụng.',
    'interview.view':         'Bạn không có quyền xem lịch phỏng vấn.',
    'interview.create':       'Bạn không có quyền tạo lịch phỏng vấn.',
    'evaluation.view':        'Bạn không có quyền xem phiếu đánh giá.',
    'evaluation.create':      'Bạn không có quyền tạo phiếu đánh giá.',
  };

  return messages[permissionCode] || 'Bạn không có quyền thực hiện chức năng này.';
}

module.exports = {
  requirePermission,
  requireAnyPermission,
  attachPermissions,
};

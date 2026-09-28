'use strict';
const config = require('../config/config');
const SessionModel = require('../models/sessionModel');

/**
 * Session Authentication Middleware
 *
 * Validates server-side session stored in PostgreSQL.
 * Handles:
 * - Missing/invalid session
 * - Idle timeout detection
 * - Absolute timeout detection
 * - Automatic session renewal when within threshold
 * - Session fixation protection
 */
async function authMiddleware(req, res, next) {
  // Check if user data exists in express-session
  if (!req.session || !req.session.userId || !req.session.sessionId) {
    return res.status(401).json({
      success: false,
      code: 'UNAUTHORIZED',
      message: 'Yêu cầu xác thực. Vui lòng đăng nhập.'
    });
  }

  try {
    // Validate the session record in our user_sessions table
    const sessionRecord = await SessionModel.findValid(req.session.sessionId);

    if (!sessionRecord) {
      // Session is invalid, expired, or revoked
      req.session.destroy(() => { });
      return res.status(401).json({
        success: false,
        code: 'SESSION_EXPIRED',
        message: 'Phiên làm việc đã hết hạn hoặc bị thu hồi. Vui lòng đăng nhập lại.'
      });
    }

    // Verify session belongs to the authenticated user
    if (sessionRecord.user_id !== req.session.userId) {
      req.session.destroy(() => { });
      return res.status(401).json({
        success: false,
        code: 'SESSION_MISMATCH',
        message: 'Phiên đăng nhập không hợp lệ.'
      });
    }

    // Check absolute timeout (hard limit even if user is active)
    const now = Date.now();
    const absoluteExpiry = new Date(sessionRecord.absolute_expires_at).getTime();
    if (now >= absoluteExpiry) {
      await SessionModel.revoke(req.session.sessionId);
      req.session.destroy(() => { });
      return res.status(401).json({
        success: false,
        code: 'SESSION_ABSOLUTE_EXPIRED',
        message: 'Phiên làm việc đã vượt quá thời gian tối đa cho phép. Vui lòng đăng nhập lại.'
      });
    }

    // Auto-renew idle timeout when within renewal threshold
    const idleExpiry = new Date(sessionRecord.expires_at).getTime();
    const timeLeft = idleExpiry - now;

    if (timeLeft < config.SESSION_RENEW_THRESHOLD_MS) {
      await SessionModel.renew(req.session.sessionId, config.SESSION_IDLE_TIMEOUT_MS);
      // Inform frontend that session was renewed
      res.set('X-Session-Renewed', 'true');
    }

    // Attach user info to request
    req.user = {
      id: sessionRecord.user_id,
      employeeCode: sessionRecord.employee_code || req.session.employeeCode,
      name: req.session.userName,
      email: req.session.userEmail,
      role: req.session.userRole,
      sessionId: req.session.sessionId,
    };

    // Expose remaining session times in response headers for frontend timer
    res.set('X-Session-Idle-Expires', new Date(sessionRecord.expires_at).toISOString());
    res.set('X-Session-Absolute-Expires', new Date(sessionRecord.absolute_expires_at).toISOString());

    next();
  } catch (err) {
    console.error('[AuthMiddleware] Error validating session:', err.message);
    return res.status(500).json({
      success: false,
      code: 'SERVER_ERROR',
      message: 'Lỗi hệ thống khi xác thực phiên làm việc.'
    });
  }
}

/**
 * Role-based Authorization Middleware Factory
 * @param {...string} allowedRoles - roles allowed to access the route
 */
function requireRole(...allowedRoles) {
  return (req, res, next) => {
    if (!req.user) {
      return res.status(401).json({ success: false, code: 'UNAUTHORIZED', message: 'Chưa xác thực.' });
    }
    if (!allowedRoles.includes(req.user.role)) {
      return res.status(403).json({
        success: false,
        code: 'FORBIDDEN',
        message: `Bạn không có quyền thực hiện hành động này. Yêu cầu vai trò: ${allowedRoles.join(', ')}.`
      });
    }
    next();
  };
}

module.exports = { authMiddleware, requireRole };

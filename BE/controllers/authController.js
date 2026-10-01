'use strict';
const bcrypt = require('bcryptjs');
const { v4: uuidv4 } = require('uuid');
const UserModel = require('../models/userModel');
const SessionModel = require('../models/sessionModel');
const EvaluationDraftModel = require('../models/evaluationDraftModel');
const config = require('../config/config');
const { validatePasswordRules } = require('../utils/passwordValidator');
const crypto = require('crypto');
const PasswordResetModel = require('../models/passwordResetModel');
const { sendResetPasswordEmail } = require('../utils/emailService');

/**
 * AuthController - Handles all authentication flows with server-side sessions
 */
class AuthController {

  // ──────────────────────────────────────────────────────────
  // POST /api/v1/auth/login
  // ──────────────────────────────────────────────────────────
  static async login(req, res) {
    try {
      const { identifier, password, deviceName } = req.body;

      if (!identifier || !password) {
        return res.status(400).json({
          success: false,
          message: 'Vui lòng nhập Email/Mã nhân sự và Mật khẩu.'
        });
      }

      // Find user by email OR employee_code
      const isEmail = identifier.includes('@');
      const user = isEmail
        ? await UserModel.findByEmail(identifier)
        : await UserModel.findByEmployeeCode(identifier);

      if (!user) {
        return res.status(401).json({
          success: false,
          message: 'Email/Mã nhân sự hoặc mật khẩu không chính xác.'
        });
      }

      // Verify password
      const isPasswordValid = await bcrypt.compare(password, user.password_hash);
      if (!isPasswordValid) {
        return res.status(401).json({
          success: false,
          message: 'Email/Mã nhân sự hoặc mật khẩu không chính xác.'
        });
      }

      // CRITICAL: Regenerate session ID to prevent session fixation attacks
      await new Promise((resolve, reject) => {
        req.session.regenerate((err) => err ? reject(err) : resolve());
      });

      // Generate a unique server-side session tracking ID
      const sessionId = uuidv4();

      // Store minimal, non-sensitive user info in session
      req.session.userId = user.id;
      req.session.userEmail = user.email;
      req.session.userName = user.full_name;
      req.session.userRole = user.role;
      req.session.employeeCode = user.employee_code;
      req.session.sessionId = sessionId;
      req.session.loginTime = new Date().toISOString();

      // Save session to express-session store (PostgreSQL)
      await new Promise((resolve, reject) => {
        req.session.save((err) => err ? reject(err) : resolve());
      });

      // Create server-side session record in user_sessions table
      await SessionModel.create({
        sessionId,
        userId: user.id,
        deviceName: deviceName || req.headers['user-agent']?.substring(0, 100) || 'Trình duyệt Web',
        ipAddress: req.ip || req.connection.remoteAddress,
        userAgent: req.headers['user-agent'],
        idleTimeoutMs: config.SESSION_IDLE_TIMEOUT_MS,
        absoluteTimeoutMs: config.SESSION_ABSOLUTE_TIMEOUT_MS,
      });

      // Restore draft data if user had pending evaluation drafts
      const pendingDrafts = await EvaluationDraftModel.findAllForUser(user.id);

      return res.status(200).json({
        success: true,
        message: 'Đăng nhập thành công!',
        user: {
          id: user.id,
          employeeCode: user.employee_code,
          name: user.full_name,
          email: user.email,
          role: user.role,
          avatarUrl: user.avatar_url,
          mustChangePw: user.must_change_pw,
        },
        session: {
          idleTimeoutSeconds: config.SESSION_IDLE_TIMEOUT_SECONDS,
          absoluteTimeoutSeconds: config.SESSION_ABSOLUTE_TIMEOUT_SECONDS,
          renewThresholdSeconds: config.SESSION_RENEW_THRESHOLD_SECONDS,
        },
        hasPendingDrafts: pendingDrafts.length > 0,
        pendingDraftsCount: pendingDrafts.length,
      });

    } catch (err) {
      console.error('[AuthController.login] Error:', err);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi đăng nhập.'
      });
    }
  }

  // ──────────────────────────────────────────────────────────
  // POST /api/v1/auth/logout
  // ──────────────────────────────────────────────────────────
  static async logout(req, res) {
    try {
      const sessionId = req.session?.sessionId;
      const userId = req.session?.userId;

      if (sessionId) {
        // Revoke session in user_sessions table
        await SessionModel.revoke(sessionId);
      }

      // Destroy express-session (removes from pg store)
      await new Promise((resolve) => {
        req.session.destroy((err) => {
          if (err) console.error('[AuthController.logout] Session destroy error:', err);
          resolve();
        });
      });

      // Clear the session cookie
      res.clearCookie('ttcs.sid', {
        httpOnly: true,
        secure: config.IS_PRODUCTION,
        sameSite: config.IS_PRODUCTION ? 'Strict' : 'Lax',
        path: '/',
      });

      return res.status(200).json({
        success: true,
        message: 'Đăng xuất thành công.'
      });

    } catch (err) {
      console.error('[AuthController.logout] Error:', err);
      return res.status(500).json({ success: false, message: 'Lỗi hệ thống khi đăng xuất.' });
    }
  }

  // ──────────────────────────────────────────────────────────
  // POST /api/v1/auth/logout-all
  // ──────────────────────────────────────────────────────────
  static async logoutAll(req, res) {
    try {
      const userId = req.user.id;
      const sessionId = req.session?.sessionId;

      // Revoke ALL server-side sessions for this user
      await SessionModel.revokeAllForUser(userId);

      // Destroy the current express-session
      await new Promise((resolve) => req.session.destroy(resolve));

      res.clearCookie('ttcs.sid', {
        httpOnly: true,
        secure: config.IS_PRODUCTION,
        sameSite: config.IS_PRODUCTION ? 'Strict' : 'Lax',
        path: '/',
      });

      return res.status(200).json({
        success: true,
        message: 'Đã đăng xuất khỏi tất cả thiết bị thành công.'
      });

    } catch (err) {
      console.error('[AuthController.logoutAll] Error:', err);
      return res.status(500).json({ success: false, message: 'Lỗi hệ thống khi đăng xuất tất cả thiết bị.' });
    }
  }

  // ──────────────────────────────────────────────────────────
  // POST /api/v1/auth/refresh  (Session keep-alive ping)
  // ──────────────────────────────────────────────────────────
  static async refreshSession(req, res) {
    try {
      // authMiddleware already renewed if within threshold
      // Just return the current session state
      const sessionRecord = await SessionModel.findValid(req.session.sessionId);
      if (!sessionRecord) {
        return res.status(401).json({
          success: false,
          code: 'SESSION_EXPIRED',
          message: 'Phiên đã hết hạn.'
        });
      }

      return res.status(200).json({
        success: true,
        message: 'Phiên đang hoạt động.',
        session: {
          idleExpiresAt: sessionRecord.expires_at,
          absoluteExpiresAt: sessionRecord.absolute_expires_at,
          idleTimeoutSeconds: config.SESSION_IDLE_TIMEOUT_SECONDS,
          absoluteTimeoutSeconds: config.SESSION_ABSOLUTE_TIMEOUT_SECONDS,
          renewThresholdSeconds: config.SESSION_RENEW_THRESHOLD_SECONDS,
        }
      });

    } catch (err) {
      console.error('[AuthController.refreshSession] Error:', err);
      return res.status(500).json({ success: false, message: 'Lỗi hệ thống.' });
    }
  }

  // ──────────────────────────────────────────────────────────
  // GET /api/v1/auth/me
  // ──────────────────────────────────────────────────────────
  static async getProfile(req, res) {
    try {
      const user = await UserModel.findById(req.user.id);
      if (!user) {
        return res.status(404).json({ success: false, message: 'Không tìm thấy tài khoản.' });
      }

      const activeSessionCount = await SessionModel.getActiveSessions(user.id);

      return res.status(200).json({
        success: true,
        user: {
          id: user.id,
          employeeCode: user.employee_code,
          name: user.full_name,
          email: user.email,
          role: user.role,
          avatarUrl: user.avatar_url,
          mustChangePw: user.must_change_pw,
          activeSessions: activeSessionCount.length,
        }
      });

    } catch (err) {
      console.error('[AuthController.getProfile] Error:', err);
      return res.status(500).json({ success: false, message: 'Lỗi hệ thống.' });
    }
  }

  // ──────────────────────────────────────────────────────────
  // GET /api/v1/auth/sessions
  // ──────────────────────────────────────────────────────────
  static async getSessions(req, res) {
    try {
      const sessions = await SessionModel.getActiveSessions(req.user.id);
      return res.status(200).json({
        success: true,
        sessions: sessions.map(s => ({
          id: s.id,
          deviceName: s.device_name,
          ipAddress: s.ip_address,
          createdAt: s.created_at,
          lastActivity: s.last_activity,
          expiresAt: s.expires_at,
          isCurrent: s.session_id === req.session.sessionId,
        }))
      });
    } catch (err) {
      console.error('[AuthController.getSessions] Error:', err);
      return res.status(500).json({ success: false, message: 'Lỗi hệ thống.' });
    }
  }

  // ──────────────────────────────────────────────────────────
  // DELETE /api/v1/auth/sessions/:id
  // ──────────────────────────────────────────────────────────
  static async revokeSession(req, res) {
    try {
      const sessionId = req.params.id;
      const revoked = await SessionModel.revokeById(req.user.id, sessionId);
      if (!revoked) {
        return res.status(404).json({ success: false, message: 'Không tìm thấy phiên làm việc.' });
      }

      // If user revoked their own current session, destroy session store & clear cookie
      if (revoked.session_id === req.session.sessionId) {
        await new Promise((resolve) => req.session.destroy(() => resolve()));
        res.clearCookie('ttcs.sid');
      }

      return res.status(200).json({
        success: true,
        message: 'Đã thu hồi phiên đăng nhập thành công.'
      });
    } catch (err) {
      console.error('[AuthController.revokeSession] Error:', err);
      return res.status(500).json({ success: false, message: 'Lỗi hệ thống.' });
    }
  }

  // ──────────────────────────────────────────────────────────
  // POST /api/v1/auth/change-password
  // ──────────────────────────────────────────────────────────
  static async changePassword(req, res) {
    try {
      const userId = req.user.id;
      const currentSessionId = req.session.sessionId;
      const { currentPassword, newPassword, confirmPassword, revokeOtherSessions = true } = req.body;

      // Validate required fields
      if (!currentPassword) {
        return res.status(400).json({ success: false, field: 'currentPassword', message: 'Bắt buộc phải nhập mật khẩu hiện tại.' });
      }
      if (!newPassword) {
        return res.status(400).json({ success: false, field: 'newPassword', message: 'Mật khẩu mới không được để trống.' });
      }
      if (!confirmPassword) {
        return res.status(400).json({ success: false, field: 'confirmPassword', message: 'Vui lòng xác nhận lại mật khẩu mới.' });
      }
      if (newPassword !== confirmPassword) {
        return res.status(400).json({ success: false, field: 'confirmPassword', message: 'Mật khẩu xác nhận không trùng khớp.' });
      }
      if (currentPassword === newPassword) {
        return res.status(400).json({ success: false, field: 'newPassword', message: 'Mật khẩu mới không được trùng với mật khẩu hiện tại.' });
      }

      // Server-side password complexity validation
      const ruleCheck = validatePasswordRules(newPassword);
      if (!ruleCheck.isValid) {
        return res.status(400).json({ success: false, field: 'newPassword', message: ruleCheck.message });
      }

      // Fetch user with password hash for verification
      const userRes = await require('../database/db').query(
        `SELECT id, password_hash FROM users WHERE id = $1 LIMIT 1`, [userId]
      );
      const user = userRes.rows[0];
      if (!user) {
        return res.status(404).json({ success: false, message: 'Không tìm thấy tài khoản.' });
      }

      // Verify current password
      const isCurrentValid = await bcrypt.compare(currentPassword, user.password_hash);
      if (!isCurrentValid) {
        return res.status(400).json({ success: false, field: 'currentPassword', message: 'Mật khẩu hiện tại không chính xác.' });
      }

      // Hash new password with bcrypt (cost factor 12)
      const hashedNewPassword = await bcrypt.hash(newPassword, 12);

      // Update password and revoke sessions in a transaction
      const updatedUser = await UserModel.updatePassword(userId, hashedNewPassword, revokeOtherSessions, currentSessionId);

      // Update must_change_pw flag
      await require('../database/db').query(
        `UPDATE users SET must_change_pw = FALSE WHERE id = $1`, [userId]
      );

      return res.status(200).json({
        success: true,
        message: revokeOtherSessions
          ? 'Đổi mật khẩu thành công! Tất cả phiên đăng nhập ở thiết bị khác đã bị thu hồi.'
          : 'Đổi mật khẩu thành công!',
        user: {
          id: updatedUser.id,
          employeeCode: updatedUser.employee_code,
          name: updatedUser.full_name,
          email: updatedUser.email,
          role: updatedUser.role,
          avatarUrl: updatedUser.avatar_url,
        }
      });

    } catch (err) {
      console.error('[AuthController.changePassword] Error:', err);
      return res.status(500).json({ success: false, message: 'Lỗi hệ thống khi đổi mật khẩu.' });
    }
  }

  // ──────────────────────────────────────────────────────────
  // POST /api/v1/auth/forgot-password
  // ──────────────────────────────────────────────────────────
  static async forgotPassword(req, res) {
    try {
      const { email } = req.body;
      if (!email) {
        return res.status(400).json({ success: false, message: 'Vui lòng nhập email.' });
      }

      // Check if user exists
      const user = await UserModel.findByEmail(email);
      if (!user) {
        // Requirement: Email không tồn tại vẫn hiển thị cùng một thông báo
        return res.status(200).json({ success: true, message: 'Nếu email tồn tại trong hệ thống, bạn sẽ nhận được một liên kết đặt lại mật khẩu.' });
      }

      // Generate reset token (UUID or random string)
      const resetToken = crypto.randomBytes(32).toString('hex');
      
      // Save token to database with 30 min expiration
      await PasswordResetModel.create(user.id, resetToken);

      // Send email (Mock or actual implementation)
      // Requirement: Nhập email nhận được liên kết đặt lại có hiệu lực 30 phút
      if (typeof sendResetPasswordEmail === 'function') {
        await sendResetPasswordEmail(user.email, resetToken);
      }

      return res.status(200).json({
        success: true,
        message: 'Nếu email tồn tại trong hệ thống, bạn sẽ nhận được một liên kết đặt lại mật khẩu.'
      });

    } catch (err) {
      console.error('[AuthController.forgotPassword] Error:', err);
      return res.status(500).json({ success: false, message: 'Lỗi hệ thống.' });
    }
  }

  // ──────────────────────────────────────────────────────────
  // POST /api/v1/auth/reset-password
  // ──────────────────────────────────────────────────────────
  static async resetPassword(req, res) {
    try {
      const { token, newPassword } = req.body;
      if (!token || !newPassword) {
        return res.status(400).json({ success: false, message: 'Dữ liệu không hợp lệ.' });
      }

      // Validate password rules
      const ruleCheck = validatePasswordRules(newPassword);
      if (!ruleCheck.isValid) {
        return res.status(400).json({ success: false, message: ruleCheck.message });
      }

      // Verify token
      const resetRecord = await PasswordResetModel.findByToken(token);
      if (!resetRecord) {
        return res.status(400).json({ success: false, message: 'Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.' });
      }

      // Hash new password
      const hashedNewPassword = await bcrypt.hash(newPassword, 12);

      // Update password
      await UserModel.updatePassword(resetRecord.user_id, hashedNewPassword, true, null);

      // Mark token as used
      await PasswordResetModel.markAsUsed(resetRecord.id);

      return res.status(200).json({
        success: true,
        message: 'Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.'
      });

    } catch (err) {
      console.error('[AuthController.resetPassword] Error:', err);
      return res.status(500).json({ success: false, message: 'Lỗi hệ thống.' });
    }
  }

}

module.exports = AuthController;

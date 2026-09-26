const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const config = require('../config/config');
const UserModel = require('../models/userModel');
const { validatePasswordRules } = require('../utils/passwordValidator');

/**
 * Controller handling Login, Change Password, and User Sessions
 */
class AuthController {
  /**
   * POST /api/v1/auth/login
   */
  static async login(req, res) {
    try {
      const { email, password, deviceName } = req.body;

      if (!email || !password) {
        return res.status(400).json({
          success: false,
          message: 'Vui lòng nhập đầy đủ Email và Mật khẩu.'
        });
      }

      const user = UserModel.findByEmail(email);
      if (!user) {
        return res.status(401).json({
          success: false,
          message: 'Email hoặc mật khẩu không chính xác.'
        });
      }

      const isPasswordValid = bcrypt.compareSync(password, user.password);
      if (!isPasswordValid) {
        return res.status(401).json({
          success: false,
          message: 'Email hoặc mật khẩu không chính xác.'
        });
      }

      // Generate a unique sessionId for this login
      const sessionId = 'sess_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6);

      // Track active session
      const sessionInfo = {
        sessionId,
        deviceName: deviceName || 'Trình duyệt Web',
        loginTime: new Date().toISOString(),
        ipAddress: req.ip || '127.0.0.1'
      };
      UserModel.addSession(user.id, sessionInfo);

      // Generate JWT Token including tokenVersion and sessionId
      const token = jwt.sign(
        {
          id: user.id,
          email: user.email,
          role: user.role,
          tokenVersion: user.tokenVersion,
          sessionId
        },
        config.JWT_SECRET,
        { expiresIn: config.JWT_EXPIRES_IN }
      );

      return res.status(200).json({
        success: true,
        message: 'Đăng nhập thành công!',
        token,
        user: {
          id: user.id,
          name: user.name,
          email: user.email,
          role: user.role,
          avatar: user.avatar
        },
        sessionId
      });
    } catch (error) {
      console.error('Login error:', error);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi đăng nhập.'
      });
    }
  }

  /**
   * POST /api/v1/auth/change-password
   * Implements KN-54 (API), KN-55 (Validation), KN-56 (Revoke other sessions)
   */
  static async changePassword(req, res) {
    try {
      const userId = req.user.id;
      const currentSessionId = req.user.sessionId;
      const { currentPassword, newPassword, confirmPassword, revokeOtherSessions = true } = req.body;

      // KN-52 & KN-55: Validate missing fields
      if (!currentPassword) {
        return res.status(400).json({
          success: false,
          field: 'currentPassword',
          message: 'Bắt buộc phải nhập mật khẩu hiện tại.'
        });
      }

      if (!newPassword) {
        return res.status(400).json({
          success: false,
          field: 'newPassword',
          message: 'Mật khẩu mới không được để trống.'
        });
      }

      if (!confirmPassword) {
        return res.status(400).json({
          success: false,
          field: 'confirmPassword',
          message: 'Vui lòng xác nhận lại mật khẩu mới.'
        });
      }

      // Check confirm password match
      if (newPassword !== confirmPassword) {
        return res.status(400).json({
          success: false,
          field: 'confirmPassword',
          message: 'Mật khẩu xác nhận không trùng khớp với mật khẩu mới.'
        });
      }

      // Check new password differs from current
      if (currentPassword === newPassword) {
        return res.status(400).json({
          success: false,
          field: 'newPassword',
          message: 'Mật khẩu mới không được trùng với mật khẩu hiện tại.'
        });
      }

      // KN-55: Server-side validation of password complexity rules
      const ruleCheck = validatePasswordRules(newPassword);
      if (!ruleCheck.isValid) {
        return res.status(400).json({
          success: false,
          field: 'newPassword',
          message: ruleCheck.message
        });
      }

      // Fetch user from DB
      const user = UserModel.findById(userId);
      if (!user) {
        return res.status(404).json({
          success: false,
          message: 'Không tìm thấy tài khoản người dùng.'
        });
      }

      // KN-55: Verify current password
      const isCurrentValid = bcrypt.compareSync(currentPassword, user.password);
      if (!isCurrentValid) {
        return res.status(400).json({
          success: false,
          field: 'currentPassword',
          message: 'Mật khẩu hiện tại không chính xác.'
        });
      }

      // Hash new password
      const hashedNewPassword = bcrypt.hashSync(newPassword, 10);

      // KN-56: Update password in DB & Revoke other sessions (increments tokenVersion)
      const updatedUser = UserModel.updatePassword(
        userId,
        hashedNewPassword,
        revokeOtherSessions,
        currentSessionId
      );

      // Generate NEW JWT token for the CURRENT session with updated tokenVersion (KN-53 & KN-56)
      const newToken = jwt.sign(
        {
          id: updatedUser.id,
          email: updatedUser.email,
          role: updatedUser.role,
          tokenVersion: updatedUser.tokenVersion,
          sessionId: currentSessionId
        },
        config.JWT_SECRET,
        { expiresIn: config.JWT_EXPIRES_IN }
      );

      return res.status(200).json({
        success: true,
        message: revokeOtherSessions
          ? 'Đổi mật khẩu thành công! Tất cả các phiên đăng nhập ở thiết bị khác đã bị thu hồi.'
          : 'Đổi mật khẩu thành công!',
        newToken,
        tokenVersion: updatedUser.tokenVersion,
        user: {
          id: updatedUser.id,
          name: updatedUser.name,
          email: updatedUser.email,
          role: updatedUser.role,
          avatar: updatedUser.avatar
        }
      });

    } catch (error) {
      console.error('Change password error:', error);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi thực hiện đổi mật khẩu.'
      });
    }
  }

  /**
   * GET /api/v1/auth/me
   */
  static async getProfile(req, res) {
    const user = UserModel.findById(req.user.id);
    if (!user) {
      return res.status(404).json({ success: false, message: 'User not found' });
    }
    return res.status(200).json({
      success: true,
      user: {
        id: user.id,
        name: user.name,
        email: user.email,
        role: user.role,
        avatar: user.avatar,
        tokenVersion: user.tokenVersion,
        activeSessionsCount: user.activeSessions.length
      }
    });
  }
}

module.exports = AuthController;

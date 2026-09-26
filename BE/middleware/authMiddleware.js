const jwt = require('jsonwebtoken');
const config = require('../config/config');
const UserModel = require('../models/userModel');

/**
 * Authentication middleware to verify JWT token and check tokenVersion for revoked sessions (KN-56)
 */
function authMiddleware(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({
      success: false,
      code: 'UNAUTHORIZED',
      message: 'Yêu cầu xác thực. Vui lòng đăng nhập.'
    });
  }

  const token = authHeader.split(' ')[1];

  try {
    const decoded = jwt.verify(token, config.JWT_SECRET);
    
    // Verify user exists and token version is valid (KN-56)
    const isValid = UserModel.isTokenValid(decoded.id, decoded.tokenVersion, decoded.sessionId);
    if (!isValid) {
      return res.status(401).json({
        success: false,
        code: 'SESSION_REVOKED',
        message: 'Phiên đăng nhập này đã bị thu hồi do tài khoản đã đổi mật khẩu từ thiết bị khác. Vui lòng đăng nhập lại.'
      });
    }

    const user = UserModel.findById(decoded.id);
    if (!user) {
      return res.status(401).json({
        success: false,
        code: 'USER_NOT_FOUND',
        message: 'Tài khoản người dùng không tồn tại.'
      });
    }

    // Attach user information to request
    req.user = {
      id: user.id,
      email: user.email,
      name: user.name,
      role: user.role,
      tokenVersion: user.tokenVersion,
      sessionId: decoded.sessionId
    };

    next();
  } catch (error) {
    if (error.name === 'TokenExpiredError') {
      return res.status(401).json({
        success: false,
        code: 'TOKEN_EXPIRED',
        message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'
      });
    }

    return res.status(401).json({
      success: false,
      code: 'INVALID_TOKEN',
      message: 'Token xác thực không hợp lệ.'
    });
  }
}

module.exports = authMiddleware;

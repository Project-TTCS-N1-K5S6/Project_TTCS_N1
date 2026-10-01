// middlewares/authMiddleware.js
const jwt = require('jsonwebtoken');

/**
 * @desc    Middleware xác thực JWT Token và bảo vệ các đường dẫn API (KN-71)
 */
const verifyToken = (req, res, next) => {
  try {
    // Lấy token từ header Authorization (dạng "Bearer <token>")
    const authHeader = req.headers.authorization || req.headers.Authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        success: false,
        message: 'Quyền truy cập bị từ chối. Token không hợp lệ hoặc không tồn tại!'
      });
    }

    const token = authHeader.split(' ')[1];

    // Xác thực token bằng secret key
    const secretKey = process.env.JWT_SECRET || 'your_default_jwt_secret_key';
    const decoded = jwt.verify(token, secretKey);

    // Gán thông tin user đã giải mã vào req.user để các Controller phía sau sử dụng
    req.user = decoded;
    next();

  } catch (error) {
    return res.status(403).json({
      success: false,
      message: 'Token đã hết hạn hoặc không hợp lệ!',
      error: error.message
    });
  }
};

/**
 * @desc    Middleware phân quyền người dùng (VD: chỉ Admin được truy cập)
 */
const authorizeRoles = (...allowedRoles) => {
  return (req, res, next) => {
    if (!req.user || !allowedRoles.includes(req.user.role)) {
      return res.status(403).json({
        success: false,
        message: 'Bạn không có quyền thực hiện thao tác này!'
      });
    }
    next();
  };
};

module.exports = {
  verifyToken,
  authorizeRoles
}:
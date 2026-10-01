'use strict';
const express = require('express');
const router = express.Router();
const AuthController = require('../controllers/authController');
const { authMiddleware } = require('../middleware/authMiddleware');
const AccountController = require('../controllers/accountController');
const { body, validationResult } = require('express-validator');
const rateLimit = require('express-rate-limit');
const config = require('../config/config');

// ──────────────────────────────────────────────────────
// Rate Limiter for Login (brute-force protection)
// ──────────────────────────────────────────────────────
const loginLimiter = rateLimit({
  windowMs: config.RATE_LIMIT_WINDOW_MS,
  max: config.LOGIN_RATE_LIMIT_MAX,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    code: 'TOO_MANY_REQUESTS',
    message: `Quá nhiều lần đăng nhập thất bại. Vui lòng thử lại sau ${config.RATE_LIMIT_WINDOW_MS / 60000} phút.`
  }
});

// ──────────────────────────────────────────────────────
// Input Validation Schemas
// ──────────────────────────────────────────────────────
const loginValidation = [
  body('identifier').trim().notEmpty().withMessage('Vui lòng nhập Email hoặc Mã nhân sự.').isLength({ max: 255 }),
  body('password').notEmpty().withMessage('Vui lòng nhập mật khẩu.').isLength({ max: 128 }),
];

const registerValidation = [
  body('fullName')
    .trim()
    .notEmpty().withMessage('Vui lòng nhập họ và tên.')
    .isLength({ min: 2, max: 150 }).withMessage('Họ và tên phải có độ dài từ 2 đến 150 ký tự.'),
  body('email')
    .trim()
    .notEmpty().withMessage('Vui lòng nhập email.')
    .isEmail().withMessage('Email không đúng định dạng.')
    .normalizeEmail(),
  body('password')
    .notEmpty().withMessage('Vui lòng nhập mật khẩu.')
    .isLength({ min: 8, max: 128 }).withMessage('Mật khẩu tối thiểu 8 ký tự.'),
  body('confirmPassword')
    .optional({ checkFalsy: true })
    .custom((value, { req }) => {
      if (req.body.confirmPassword && value !== req.body.password) {
        throw new Error('Mật khẩu xác nhận không khớp.');
      }
      return true;
    }),
];

const changePasswordValidation = [
  body('currentPassword').notEmpty().withMessage('Vui lòng nhập mật khẩu hiện tại.'),
  body('newPassword').notEmpty().isLength({ min: 8, max: 128 }).withMessage('Mật khẩu mới tối thiểu 8 ký tự.'),
  body('confirmPassword').notEmpty().withMessage('Vui lòng xác nhận mật khẩu mới.'),
];

// Validation error handler middleware
function handleValidationErrors(req, res, next) {
  const errors = validationResult(req);
  if (!errors.isEmpty()) {
    const first = errors.array()[0];
    return res.status(400).json({
      success: false,
      field: first.path,
      message: first.msg,
    });
  }
  next();
}

// ──────────────────────────────────────────────────────
// Routes
// ──────────────────────────────────────────────────────

// Public routes
router.post('/login', loginLimiter, loginValidation, handleValidationErrors, AuthController.login);
router.post('/register', loginLimiter, registerValidation, handleValidationErrors, AuthController.register);
router.post('/logout', AuthController.logout);  // Can be called even without valid session

// Protected routes (require valid session)
router.post('/logout-all', authMiddleware, AuthController.logoutAll);
router.post('/refresh', authMiddleware, AuthController.refreshSession);
router.get('/me', authMiddleware, AuthController.getProfile);
router.get('/sessions', authMiddleware, AuthController.getSessions);
router.delete('/sessions/:id', authMiddleware, AuthController.revokeSession);
router.post('/change-password', authMiddleware, changePasswordValidation, handleValidationErrors, AuthController.changePassword);

// Internal account administration is restricted by server-side RBAC.
router.get('/accounts', authMiddleware, AccountController.requireAccountPermission, AccountController.list);
router.post('/accounts', authMiddleware, AccountController.requireAccountPermission, AccountController.create);
router.patch('/accounts/:id', authMiddleware, AccountController.requireAccountPermission, AccountController.update);

module.exports = router;

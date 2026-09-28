const express = require('express');
const router = express.Router();
const AuthController = require('../controllers/authController');
const authMiddleware = require('../middleware/authMiddleware');
const AccountController = require('../controllers/accountController');

// Public route
router.post('/login', AuthController.login);

// Protected routes (requires valid JWT token)
router.post('/change-password', authMiddleware, AuthController.changePassword);
router.get('/me', authMiddleware, AuthController.getProfile);

// Internal account administration (administrator access only)
router.get('/accounts', authMiddleware, AccountController.requireAdmin, AccountController.list);
router.post('/accounts', authMiddleware, AccountController.requireAdmin, AccountController.create);
router.patch('/accounts/:id', authMiddleware, AccountController.requireAdmin, AccountController.update);

module.exports = router;

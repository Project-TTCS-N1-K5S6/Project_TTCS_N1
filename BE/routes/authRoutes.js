const express = require('express');
const router = express.Router();
const AuthController = require('../controllers/authController');
const authMiddleware = require('../middleware/authMiddleware');

// Public route
router.post('/login', AuthController.login);

// Protected routes (requires valid JWT token)
router.post('/change-password', authMiddleware, AuthController.changePassword);
router.get('/me', authMiddleware, AuthController.getProfile);

module.exports = router;

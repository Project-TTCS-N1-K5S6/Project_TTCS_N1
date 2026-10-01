const express = require('express');
const router = express.Router();
const { getUserProfile } = require('../controllers/userController');
const { verifyToken } = require('../middlewares/authMiddleware'); // Middleware bảo vệ API

// Route lấy thông tin profile người dùng
router.get('/profile', verifyToken, getUserProfile);

module.exports = router;
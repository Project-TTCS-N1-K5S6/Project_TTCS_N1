const express = require('express');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
require('dotenv').config();
const config = require('./config/config');
const authRoutes = require('./routes/authRoutes');

const app = express();
const frontendDist = path.join(__dirname, '../FE/dist');
const frontendRoot = fs.existsSync(path.join(frontendDist, 'index.html'))
  ? frontendDist
  : path.join(__dirname, '../FE');

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Serve static frontend files if accessed via backend
app.use(express.static(frontendRoot));

// API Routes
app.use('/api/v1/auth', authRoutes);

// Health check endpoint
app.get('/api/health', (req, res) => {
  res.status(200).json({ status: 'OK', system: 'Hệ thống Tuyển dụng Nội bộ - TTCS', time: new Date().toISOString() });
});

// Fallback to FE index.html
app.use((req, res) => {
  res.sendFile(path.join(frontendRoot, 'index.html'));
});

// Start server
app.listen(config.PORT, () => {
  console.log(`===================================================`);
  console.log(`🚀 Backend Server running on http://localhost:${config.PORT}`);
  console.log(`🔑 Auth Endpoint: http://localhost:${config.PORT}/api/v1/auth`);
  console.log(`===================================================`);
});

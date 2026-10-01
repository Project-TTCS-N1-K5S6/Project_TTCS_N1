'use strict';
require('dotenv').config();

const express = require('express');
const session = require('express-session');
const pgSession = require('connect-pg-simple')(session);
const helmet = require('helmet');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const path = require('path');
const config = require('./config/config');
const { pool } = require('./database/db');
const SessionModel = require('./models/sessionModel');

// Import Routes
const authRoutes = require('./routes/authRoutes');
const evaluationRoutes = require('./routes/evaluationRoutes');

const app = express();

// ============================================================
// 1. SECURITY HEADERS (helmet)
// ============================================================
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'", "'unsafe-inline'", 'cdn.jsdelivr.net', 'fonts.googleapis.com'],
      styleSrc: ["'self'", "'unsafe-inline'", 'cdn.jsdelivr.net', 'fonts.googleapis.com', 'fonts.gstatic.com'],
      fontSrc: ["'self'", 'fonts.googleapis.com', 'fonts.gstatic.com', 'data:'],
      imgSrc: ["'self'", 'data:', 'https://images.unsplash.com'],
      connectSrc: ["'self'"],
      frameSrc: ["'none'"],
      objectSrc: ["'none'"],
    }
  },
  crossOriginEmbedderPolicy: false,
}));

// ============================================================
// 2. CORS
// ============================================================
app.use(cors({
  origin: config.CORS_ORIGIN,
  credentials: true,  // Required for cookies to be sent cross-origin
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'X-Requested-With'],
}));

// ============================================================
// 3. REQUEST PARSING
// ============================================================
app.use(express.json({ limit: '2mb' }));
app.use(express.urlencoded({ extended: true, limit: '2mb' }));

// ============================================================
// 4. GLOBAL RATE LIMITER
// ============================================================
app.use('/api/', rateLimit({
  windowMs: config.RATE_LIMIT_WINDOW_MS,
  max: config.RATE_LIMIT_MAX,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    code: 'TOO_MANY_REQUESTS',
    message: 'Quá nhiều yêu cầu. Vui lòng thử lại sau.'
  }
}));

// ============================================================
// 5. SESSION STORE (PostgreSQL via connect-pg-simple)
// ============================================================
const sessionStore = new pgSession({
  pool,
  tableName: 'session',
  createTableIfMissing: false, // Table created via migration script
  ttl: config.SESSION_IDLE_TIMEOUT_SECONDS,
  disableTouch: false,
  pruneSessionInterval: 60 * 60, // Prune expired sessions every hour
});

app.use(session({
  name: 'ttcs.sid',             // Non-revealing cookie name
  store: sessionStore,
  secret: config.SESSION_SECRET,
  resave: false,                  // Don't resave session if not modified
  saveUninitialized: false,       // Don't create session until logged in
  rolling: false,                 // Renewal handled manually in middleware
  cookie: {
    httpOnly: true,               // Prevent XSS access to cookie
    secure: config.IS_PRODUCTION,  // HTTPS only in production
    sameSite: config.IS_PRODUCTION ? 'Strict' : 'Lax',
    maxAge: config.SESSION_IDLE_TIMEOUT_MS,
    path: '/',
  }
}));

// ============================================================
// 6. SERVE STATIC FRONTEND FILES
// ============================================================
app.use(express.static(path.join(__dirname, '../FE')));

// ============================================================
// 7. API ROUTES
// ============================================================
app.use('/api/v1/auth', authRoutes);
app.use('/api/v1/evaluations', evaluationRoutes);

// Health check
app.get('/api/health', async (req, res) => {
  try {
    await pool.query('SELECT 1');
    res.status(200).json({
      status: 'OK',
      system: 'Hệ thống Nhân Sự Nội Bộ - TTCS',
      time: new Date().toISOString(),
      db: 'connected',
      session: req.session?.userId ? 'authenticated' : 'unauthenticated',
    });
  } catch (e) {
    res.status(503).json({ status: 'ERROR', db: 'disconnected' });
  }
});

// ============================================================
// 8. SPA FALLBACK (serve FE index.html for non-API routes)
// ============================================================
app.use((req, res, next) => {
  if (req.path.startsWith('/api/')) return next();
  res.sendFile(path.join(__dirname, '../FE/index.html'));
});

// ============================================================
// 9. GLOBAL ERROR HANDLER
// ============================================================
app.use((err, req, res, next) => {
  console.error('[Server] Unhandled error:', err);
  res.status(500).json({
    success: false,
    message: config.IS_PRODUCTION ? 'Lỗi hệ thống.' : err.message,
  });
});

// ============================================================
// 10. START SERVER + PERIODIC SESSION CLEANUP
// ============================================================
app.listen(config.PORT, async () => {
  console.log('════════════════════════════════════════════════════');
  console.log(`🚀 TTCS HR System Backend`);
  console.log(`   Env  : ${config.NODE_ENV}`);
  console.log(`   Port : http://localhost:${config.PORT}`);
  console.log(`   DB   : ${process.env.DB_NAME}@${process.env.DB_HOST}:${process.env.DB_PORT}`);
  console.log(`   Auth : Session-based (PostgreSQL store)`);
  console.log(`   Idle : ${config.SESSION_IDLE_TIMEOUT_SECONDS}s | Absolute: ${config.SESSION_ABSOLUTE_TIMEOUT_SECONDS}s`);
  console.log('════════════════════════════════════════════════════');

  // Cleanup expired sessions every 30 minutes
  setInterval(async () => {
    try {
      const cleaned = await SessionModel.cleanupExpired();
      if (cleaned > 0) console.log(`[SessionCleanup] Cleaned up ${cleaned} expired session(s).`);
    } catch (err) {
      console.error('[SessionCleanup] Error:', err.message);
    }
  }, 30 * 60 * 1000);
});

module.exports = app;

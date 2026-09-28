'use strict';
require('dotenv').config();

const SESSION_IDLE_TIMEOUT_SECONDS = parseInt(process.env.SESSION_IDLE_TIMEOUT_SECONDS || '1800', 10);
const SESSION_ABSOLUTE_TIMEOUT_SECONDS = parseInt(process.env.SESSION_ABSOLUTE_TIMEOUT_SECONDS || '28800', 10);
const SESSION_RENEW_THRESHOLD_SECONDS = parseInt(process.env.SESSION_RENEW_THRESHOLD_SECONDS || '600', 10);

module.exports = {
  // Server
  PORT: parseInt(process.env.PORT || '5000', 10),
  NODE_ENV: process.env.NODE_ENV || 'development',
  IS_PRODUCTION: process.env.NODE_ENV === 'production',

  // Session
  SESSION_SECRET: process.env.SESSION_SECRET || 'ttcs_fallback_secret_change_me',
  SESSION_IDLE_TIMEOUT_MS: SESSION_IDLE_TIMEOUT_SECONDS * 1000,
  SESSION_ABSOLUTE_TIMEOUT_MS: SESSION_ABSOLUTE_TIMEOUT_SECONDS * 1000,
  SESSION_RENEW_THRESHOLD_MS: SESSION_RENEW_THRESHOLD_SECONDS * 1000,
  SESSION_IDLE_TIMEOUT_SECONDS,
  SESSION_ABSOLUTE_TIMEOUT_SECONDS,
  SESSION_RENEW_THRESHOLD_SECONDS,

  // CORS
  CORS_ORIGIN: process.env.CORS_ORIGIN || 'http://localhost:5173',

  // Rate limiting
  RATE_LIMIT_WINDOW_MS: parseInt(process.env.RATE_LIMIT_WINDOW_MS || '900000', 10),
  RATE_LIMIT_MAX: parseInt(process.env.RATE_LIMIT_MAX || '100', 10),
  LOGIN_RATE_LIMIT_MAX: parseInt(process.env.LOGIN_RATE_LIMIT_MAX || '10', 10),
};

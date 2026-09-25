import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '../../../.env') });
dotenv.config(); // fallback

export const config = {
  env: process.env.NODE_ENV || 'development',
  port: parseInt(process.env.PORT || '5000', 10),
  corsOrigin: process.env.CORS_ORIGIN || 'http://localhost:5173',

  jwt: {
    secret: process.env.JWT_ACCESS_SECRET || 'irms_jwt_access_super_secret_key_2026_min_32_chars',
    expiresInSeconds: parseInt(process.env.JWT_ACCESS_EXPIRATION_SECONDS || '900', 10), // 15 mins
    refreshExpirationDays: parseInt(process.env.REFRESH_TOKEN_EXPIRATION_DAYS || '7', 10), // 7 days
  },

  security: {
    maxFailedAttempts: parseInt(process.env.MAX_FAILED_LOGIN_ATTEMPTS || '5', 10),
    lockoutMinutes: parseInt(process.env.ACCOUNT_LOCK_MINUTES || '15', 10),
    resetTokenExpirationMinutes: parseInt(process.env.PASSWORD_RESET_EXPIRATION_MINUTES || '30', 10),
  },

  mail: {
    host: process.env.SMTP_HOST || 'localhost',
    port: parseInt(process.env.SMTP_PORT || '1025', 10),
    user: process.env.SMTP_USER || '',
    pass: process.env.SMTP_PASSWORD || '',
    secure: process.env.SMTP_SECURE === 'true',
    from: process.env.EMAIL_FROM || '"IRMS Recruitment System" <noreply@company.local>',
    appUrl: process.env.APP_URL || 'http://localhost:5173',
  },
};

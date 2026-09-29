import express, { Request, Response } from 'express';
import cors from 'cors';
import helmet from 'helmet';
import cookieParser from 'cookie-parser';
import swaggerUi from 'swagger-ui-express';
import { config } from './config/env';
import { swaggerSpec } from './swagger/swagger';
import { errorHandler } from './middlewares/error.middleware';
import { NotFoundException } from './common/exceptions';

// Route modules
import authRoutes from './modules/auth/auth.routes';
import usersRoutes from './modules/users/users.routes';
import rolesRoutes from './modules/roles/roles.routes';
import permissionsRoutes from './modules/permissions/permissions.routes';
import departmentsRoutes from './modules/departments/departments.routes';
import auditRoutes from './modules/audit/audit.routes';
import emailRoutes from './modules/email/email.routes';

export const app = express();

// Security Headers
app.use(
  helmet({
    contentSecurityPolicy: false, // Allow Swagger UI inline scripts
    crossOriginEmbedderPolicy: false,
  })
);

// CORS configuration
app.use(
  cors({
    origin: config.corsOrigin,
    credentials: true,
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'X-Requested-With'],
  })
);

// Body and Cookie Parsers
app.use(express.json({ limit: '5mb' }));
app.use(express.urlencoded({ extended: true, limit: '5mb' }));
app.use(cookieParser());

// Structured Request Logger (Safe: never logs password, tokens or sensitive payloads)
app.use((req: Request, res: Response, next) => {
  const start = Date.now();
  res.on('finish', () => {
    const duration = Date.now() - start;
    if (process.env.NODE_ENV !== 'test') {
      console.log(
        `[${new Date().toISOString()}] ${req.method} ${req.originalUrl} ${res.statusCode} - ${duration}ms`
      );
    }
  });
  next();
});

// Swagger Documentation
app.use('/api/docs', swaggerUi.serve, swaggerUi.setup(swaggerSpec));
app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerSpec));

// Health check endpoint
app.get('/api/health', (req: Request, res: Response) => {
  res.status(200).json({
    status: 'UP',
    system: 'IRMS Backend',
    timestamp: new Date().toISOString(),
  });
});

// Mount Module APIs
app.use('/api/auth', authRoutes);
app.use('/api/users', usersRoutes);
app.use('/api/roles', rolesRoutes);
app.use('/api/permissions', permissionsRoutes);
app.use('/api/departments', departmentsRoutes);
app.use('/api/audit-logs', auditRoutes);
app.use('/api/email-outbox', emailRoutes);

// Catch 404
app.use((req: Request, res: Response, next) => {
  next(new NotFoundException(`Không tìm thấy đường dẫn: ${req.method} ${req.originalUrl}`));
});

// Global Error Handler
app.use(errorHandler);

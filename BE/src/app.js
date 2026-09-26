import express from 'express';
import cors from 'cors';
import apiRoutes from './routes/apiRoutes.js';
import { globalErrorHandler, notFoundHandler } from './middlewares/errorHandler.js';

const app = express();

app.use(cors());
app.use(express.json());

// API Endpoints
app.use('/api/v1', apiRoutes);

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'UP', service: 'TTCS Recruitment Backend API', timestamp: new Date() });
});

// Route Not Found Handler (404)
app.use(notFoundHandler);

// Global Error Handler Middleware
app.use(globalErrorHandler);

export default app;

import { Request, Response, NextFunction } from 'express';
import { HttpException } from '../common/exceptions';
import { errorResponse } from '../common/response';
import { ZodError } from 'zod';

export function errorHandler(
  err: Error | HttpException | ZodError,
  req: Request,
  res: Response,
  next: NextFunction
): void {
  // If headers already sent, delegate to default express handler
  if (res.headersSent) {
    return next(err);
  }

  // Handle Zod Validation Error
  if (err instanceof ZodError) {
    const formattedErrors = err.errors.map((e) => ({
      field: e.path.join('.'),
      message: e.message,
    }));
    res.status(422).json(errorResponse('Dữ liệu không hợp lệ.', formattedErrors));
    return;
  }

  // Handle Custom HttpException
  if (err instanceof HttpException) {
    res.status(err.statusCode).json(errorResponse(err.message, err.errors));
    return;
  }

  // Handle unexpected server error (Log privately, never leak stack trace or SQL)
  console.error('[Unhandled Internal Error]', {
    message: err.message,
    stack: err.stack,
    path: req.path,
    method: req.method,
    timestamp: new Date().toISOString(),
  });

  res.status(500).json(errorResponse('Đã có lỗi xảy ra trên hệ thống. Vui lòng liên hệ quản trị viên.'));
}

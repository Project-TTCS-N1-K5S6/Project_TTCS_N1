/**
 * Sub-task KN-78: [BE] Chuẩn hóa cấu trúc phản hồi lỗi
 * Global Error Handler Middleware để capture và format mọi ngoại lệ thành JSON chuẩn
 */

import { HttpStatus, ErrorCode, ErrorHints } from '../constants/httpStatus.js';
import { AppException } from '../exceptions/customExceptions.js';

export const globalErrorHandler = (err, req, res, next) => {
  const timestamp = new Date().toISOString();
  const path = req.originalUrl || req.url;

  // Nếu là ngoại lệ nghiệp vụ do hệ thống chủ động ném (AppException)
  if (err instanceof AppException) {
    return res.status(err.status).json({
      success: false,
      status: err.status,
      errorCode: err.errorCode,
      message: err.message,
      hint: err.hint,
      path: path,
      timestamp: timestamp,
      details: err.details
    });
  }

  // Xử lý lỗi hệ thống chưa được dự đoán trước (Unhandled System / Syntax / Runtime Error)
  console.error(`[Unhandled Error] ${timestamp} | Path: ${path}`, err);

  const status = err.status || HttpStatus.INTERNAL_SERVER_ERROR;
  const errorCode = err.code || ErrorCode.INTERNAL_SERVER_ERROR;

  return res.status(status).json({
    success: false,
    status: status,
    errorCode: errorCode,
    message: err.message || 'Đã xảy ra lỗi không xác định trên máy chủ.',
    hint: ErrorHints[status] || 'Mẹo: Vui lòng liên hệ quản trị viên hoặc quay lại trang chủ.',
    path: path,
    timestamp: timestamp,
    details: process.env.NODE_ENV === 'development' ? [err.stack] : []
  });
};

/**
 * Middleware xử lý route không tồn tại (404 Not Found) ở Backend
 */
export const notFoundHandler = (req, res, next) => {
  const path = req.originalUrl || req.url;
  return res.status(HttpStatus.NOT_FOUND).json({
    success: false,
    status: HttpStatus.NOT_FOUND,
    errorCode: ErrorCode.NOT_FOUND,
    message: `Đường dẫn API '${path}' không tồn tại trên hệ thống tuyển dụng.`,
    hint: 'Mẹo: Kiểm tra lại địa chỉ URL API hoặc tham khảo tài liệu Swagger/Postman.',
    path: path,
    timestamp: new Date().toISOString(),
    details: []
  });
};

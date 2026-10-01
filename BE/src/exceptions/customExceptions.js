/**
 * Sub-task KN-77: [BE] Tập tin ngoại lệ xử lý
 * Các lớp Ngoại lệ (Custom Exception Classes) chuẩn hóa
 */

import { HttpStatus, ErrorCode, ErrorHints } from '../constants/httpStatus.js';

export class AppException extends Error {
  constructor(message, status = HttpStatus.INTERNAL_SERVER_ERROR, errorCode = ErrorCode.INTERNAL_SERVER_ERROR, details = null, hint = null) {
    super(message);
    this.name = this.constructor.name;
    this.status = status;
    this.errorCode = errorCode;
    this.details = details || [];
    this.hint = hint || ErrorHints[status] || 'Mẹo: Thử tải lại trang hoặc liên hệ bộ phận hỗ trợ kỹ thuật.';
    Error.captureStackTrace(this, this.constructor);
  }
}

export class BadRequestException extends AppException {
  constructor(message = 'Yêu cầu không hợp lệ.', details = null, hint = null) {
    super(message, HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, details, hint);
  }
}

export class UnauthorizedException extends AppException {
  constructor(message = 'Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn.', details = null, hint = null) {
    super(message, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, details, hint);
  }
}

export class ForbiddenException extends AppException {
  constructor(message = 'Bạn không có quyền truy cập vào chức năng hoặc tài nguyên này.', details = null, hint = null) {
    super(message, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, details, hint);
  }
}

export class NotFoundException extends AppException {
  constructor(message = 'Tài nguyên hoặc đường dẫn không tìm thấy.', details = null, hint = null) {
    super(message, HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, details, hint);
  }
}

export class ValidationException extends AppException {
  constructor(message = 'Dữ liệu không đáp ứng quy tắc kiểm tra.', details = null, hint = null) {
    super(message, HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.VALIDATION_FAILED, details, hint);
  }
}

export class InternalServerException extends AppException {
  constructor(message = 'Đã xảy ra lỗi hệ thống nội bộ.', details = null, hint = null) {
    super(message, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR, details, hint);
  }
}

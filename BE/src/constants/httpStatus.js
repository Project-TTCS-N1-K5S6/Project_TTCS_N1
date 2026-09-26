/**
 * Sub-task KN-76: [BE] Chuẩn hóa HTTP trạng thái
 * Danh mục mã trạng thái HTTP tiêu chuẩn và Mã lỗi nghiệp vụ (Error Codes)
 */

export const HttpStatus = {
  OK: 200,
  CREATED: 201,
  NO_CONTENT: 204,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  CONFLICT: 409,
  UNPROCESSABLE_ENTITY: 422,
  TOO_MANY_REQUESTS: 429,
  INTERNAL_SERVER_ERROR: 500,
  SERVICE_UNAVAILABLE: 503
};

export const ErrorCode = {
  BAD_REQUEST: 'BAD_REQUEST',
  UNAUTHORIZED: 'UNAUTHORIZED_ACCESS',
  FORBIDDEN: 'FORBIDDEN_RESOURCE',
  NOT_FOUND: 'RESOURCE_NOT_FOUND',
  VALIDATION_FAILED: 'VALIDATION_FAILED',
  INTERNAL_SERVER_ERROR: 'INTERNAL_SERVER_ERROR',
  SERVICE_UNAVAILABLE: 'SERVICE_UNAVAILABLE',
  DATABASE_ERROR: 'DATABASE_ERROR'
};

/**
 * Gợi ý mẹo khắc phục lỗi mặc định cho từng loại lỗi
 */
export const ErrorHints = {
  [HttpStatus.BAD_REQUEST]: 'Mẹo: Kiểm tra lại các tham số dữ liệu gửi lên trong yêu cầu.',
  [HttpStatus.UNAUTHORIZED]: 'Mẹo: Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại để tiếp tục.',
  [HttpStatus.FORBIDDEN]: 'Mẹo: Liên hệ Quản trị viên HR (Admin) để xin cấp quyền hoặc quay lại Trang chủ.',
  [HttpStatus.NOT_FOUND]: 'Mẹo: Vị trí tuyển dụng hoặc hồ sơ ứng viên này có thể đã bị xoá hoặc sai đường dẫn.',
  [HttpStatus.UNPROCESSABLE_ENTITY]: 'Mẹo: Đảm bảo các định dạng Email, Số điện thoại và Ngày tháng chuẩn DD/MM/YYYY.',
  [HttpStatus.INTERNAL_SERVER_ERROR]: 'Mẹo: Hệ thống gặp sự cố đột xuất. Hãy thử lại sau vài phút hoặc báo cho IT Support.',
  [HttpStatus.SERVICE_UNAVAILABLE]: 'Mẹo: Hệ thống tuyển dụng đang bảo trì định kỳ. Vui lòng quay lại sau.'
};

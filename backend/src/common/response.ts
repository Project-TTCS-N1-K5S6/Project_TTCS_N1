export interface ApiResponse<T = any> {
  success: boolean;
  data: T | null;
  message: string | null;
  errors?: Array<{ field?: string; message: string }>;
}

export function successResponse<T>(data: T, message: string | null = 'Thao tác thành công'): ApiResponse<T> {
  return {
    success: true,
    data,
    message,
  };
}

export function errorResponse(
  message: string = 'Đã xảy ra lỗi, vui lòng thử lại.',
  errors: Array<{ field?: string; message: string }> = []
): ApiResponse<null> {
  return {
    success: false,
    data: null,
    message,
    errors,
  };
}

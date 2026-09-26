/**
 * Sub-task KN-75: [FE] Bộ Bắt lỗi Tập trung & Điều hướng Client
 * Service Client-side API Interceptor & Central Error Handler
 */

export async function apiRequest(endpoint, options = {}) {
  const defaultHeaders = {
    'Content-Type': 'application/json',
    'x-user-role': localStorage.getItem('userRole') || 'EMPLOYEE'
  };

  const config = {
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers
    }
  };

  try {
    const response = await fetch(endpoint, config);

    // Parse JSON payload
    let data;
    try {
      data = await response.json();
    } catch (e) {
      data = null;
    }

    if (!response.ok) {
      // Backend returned non-2xx status code
      const errorObj = {
        status: response.status,
        errorCode: data?.errorCode || getErrorCodeFromStatus(response.status),
        message: data?.message || getErrorMessageFromStatus(response.status),
        hint: data?.hint || getErrorHintFromStatus(response.status),
        path: data?.path || endpoint,
        timestamp: data?.timestamp || new Date().toISOString(),
        details: data?.details || []
      };

      // Create rich error object
      const error = new Error(errorObj.message);
      error.errorPayload = errorObj;
      throw error;
    }

    return data;
  } catch (error) {
    if (error.errorPayload) {
      throw error; // Re-throw structured API error
    }

    // Network error / offline failure
    const networkErrorObj = {
      status: 503,
      errorCode: 'NETWORK_ERROR',
      message: 'Không thể kết nối đến máy chủ tuyển dụng. Vui lòng kiểm tra lại đường truyền Internet.',
      hint: 'Mẹo: Kiểm tra Wi-Fi/LAN của bạn hoặc thử làm mới trang.',
      path: endpoint,
      timestamp: new Date().toISOString(),
      details: [error.message]
    };

    const err = new Error(networkErrorObj.message);
    err.errorPayload = networkErrorObj;
    throw err;
  }
}

function getErrorCodeFromStatus(status) {
  switch (status) {
    case 401: return 'UNAUTHORIZED_ACCESS';
    case 403: return 'FORBIDDEN_RESOURCE';
    case 404: return 'RESOURCE_NOT_FOUND';
    case 500: return 'INTERNAL_SERVER_ERROR';
    case 503: return 'SERVICE_UNAVAILABLE';
    default: return 'HTTP_ERROR_' + status;
  }
}

function getErrorMessageFromStatus(status) {
  switch (status) {
    case 401: return 'Phiên đăng nhập của bạn đã hết hạn.';
    case 403: return 'Bạn không có quyền truy cập vào tài nguyên này.';
    case 404: return 'Trang hoặc tài nguyên bạn tìm kiếm không tồn tại.';
    case 500: return 'Lỗi xử lý nội bộ tại máy chủ tuyển dụng.';
    case 503: return 'Hệ thống tuyển dụng hiện đang bảo trì.';
    default: return 'Đã xảy ra lỗi không xác định (' + status + ').';
  }
}

function getErrorHintFromStatus(status) {
  switch (status) {
    case 401: return 'Mẹo: Nhấn nút "Đăng nhập lại" để làm mới phiên làm việc.';
    case 403: return 'Mẹo: Liên hệ Quản trị viên HR (Admin) để xin cấp quyền hoặc quay lại Trang chủ.';
    case 404: return 'Mẹo: Kiểm tra lại đường dẫn URL hoặc sử dụng tìm kiếm ứng viên.';
    case 500: return 'Mẹo: Báo sự cố cho bộ phận IT hoặc thử lại sau vài phút.';
    default: return 'Mẹo: Thử làm mới trang hoặc liên hệ hỗ trợ.';
  }
}

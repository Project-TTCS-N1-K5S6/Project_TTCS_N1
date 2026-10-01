import React from 'react';
import { CommonErrorPage } from './CommonErrorPage';

/**
 * Sub-task KN-73: [FE] Các trang lỗi Đặc thù
 * Bộ sưu tập các trang lỗi chuyên biệt với thông điệp và ngữ cảnh ứng dụng tuyển dụng nội bộ
 */

export function Error404Page({ onRetry }) {
  const payload = {
    status: 404,
    errorCode: 'RESOURCE_NOT_FOUND',
    message: 'Hồ sơ ứng viên hoặc trang tuyển dụng bạn truy cập không tìm thấy trên hệ thống.',
    hint: 'Mẹo: Vui lòng kiểm tra lại Mã UV (Ví dụ: UV-2026-001) hoặc tìm theo danh sách Vị trí Tuyển dụng đang Mở.',
    path: window.location.pathname,
    timestamp: new Date().toISOString(),
    details: ['Resource path resolution returned 0 records']
  };

  return <CommonErrorPage errorPayload={payload} onRetry={onRetry} />;
}

export function Error403Page({ onRetry }) {
  const payload = {
    status: 403,
    errorCode: 'FORBIDDEN_RESOURCE',
    message: 'Bạn không có quyền xem thông tin báo cáo ngân sách lương & định biên tuyển dụng.',
    hint: 'Mẹo: Bạn thuộc vai trò "Nhân sự Sàng lọc". Chức năng này dành cho "HR_ADMIN". Hãy nhấn nút bên dưới để gửi yêu cầu nâng quyền.',
    path: '/api/v1/recruitment/admin/salary-reports',
    timestamp: new Date().toISOString(),
    details: ['Role required: HR_ADMIN', 'Current role: EMPLOYEE']
  };

  return <CommonErrorPage errorPayload={payload} onRetry={onRetry} />;
}

export function Error401Page({ onRetry }) {
  const payload = {
    status: 401,
    errorCode: 'UNAUTHORIZED_ACCESS',
    message: 'Phiên làm việc nội bộ đã hết hạn sau 30 phút không hoạt động.',
    hint: 'Mẹo: Đăng nhập lại thông qua Cổng SSO doanh nghiệp để tiếp tục cập nhật trạng thái phỏng vấn.',
    path: '/api/v1/auth/verify-session',
    timestamp: new Date().toISOString(),
    details: ['JWT Token expired at 2026-09-26T07:45:00Z']
  };

  return <CommonErrorPage errorPayload={payload} onRetry={onRetry} />;
}

export function Error500Page({ onRetry }) {
  const payload = {
    status: 500,
    errorCode: 'INTERNAL_SERVER_ERROR',
    message: 'Lỗi truy vấn Cơ sở dữ liệu Postgres trong quá trình kết xuất báo cáo phỏng vấn.',
    hint: 'Mẹo: Đã ghi nhận trace lỗi hệ thống #ERR-9082. IT Support đang tiến hành xử lý.',
    path: '/api/v1/system/crash-test',
    timestamp: new Date().toISOString(),
    details: ['PostgreSQL connection timeout: 5000ms', 'Database host: db-primary.local']
  };

  return <CommonErrorPage errorPayload={payload} onRetry={onRetry} />;
}

export function Error503Page({ onRetry }) {
  const payload = {
    status: 503,
    errorCode: 'SERVICE_UNAVAILABLE',
    message: 'Hệ thống tuyển dụng nội bộ đang bảo trì định kỳ kỳ T9/2026 (K5S6).',
    hint: 'Mẹo: Thời gian bảo trì dự kiến hoàn tất trong 15 phút. Bạn vui lòng quay lại sau.',
    path: '/api/v1/system/maintenance',
    timestamp: new Date().toISOString(),
    details: ['Maintenance window: 07:30 - 08:00 AM']
  };

  return <CommonErrorPage errorPayload={payload} onRetry={onRetry} />;
}

/**
 * Recruitment Controller với ví dụ kiểm thử cho các ngoại lệ chuẩn hóa
 */

import {
  NotFoundException,
  ForbiddenException,
  UnauthorizedException,
  ValidationException,
  InternalServerException
} from '../exceptions/customExceptions.js';

// Dữ liệu giả lập Ứng viên & Vị trí tuyển dụng
const mockCandidates = [
  { id: 'UV-2026-001', name: 'Nguyễn Văn A', position: 'Senior React Developer', department: 'Công nghệ thông tin', status: 'Phỏng vấn V2', email: 'nva@gmail.com' },
  { id: 'UV-2026-002', name: 'Trần Thị B', position: 'HR Business Partner', department: 'Nhân sự', status: 'Sàng lọc', email: 'ttb@gmail.com' },
  { id: 'UV-2026-003', name: 'Lê Hoàng C', position: 'Backend Specialist (Node.js)', department: 'Công nghệ thông tin', status: 'Offer', email: 'lhc@gmail.com' }
];

const mockVacancies = [
  { code: 'VT-01', title: 'Senior Frontend Engineer', department: 'Công nghệ thông tin', openCount: 3, deadline: '30/10/2026', status: 'Mở' },
  { code: 'VT-02', title: 'Chuyên viên Tuyển dụng Nội bộ', department: 'Nhân sự', openCount: 1, deadline: '15/10/2026', status: 'Mở' }
];

export const getCandidates = (req, res) => {
  res.json({
    success: true,
    data: mockCandidates,
    message: 'Lấy danh sách ứng viên thành công.'
  });
};

export const getCandidateById = (req, res) => {
  const { id } = req.params;
  const candidate = mockCandidates.find(c => c.id === id);

  if (!candidate) {
    throw new NotFoundException(
      `Không tìm thấy ứng viên có mã '${id}'.`,
      [{ field: 'id', value: id }],
      'Mẹo: Vui lòng kiểm tra lại Mã UV hoặc tra cứu danh sách ứng viên mới nhất.'
    );
  }

  res.json({
    success: true,
    data: candidate,
    message: 'Chi tiết ứng viên.'
  });
};

export const getVacancies = (req, res) => {
  res.json({
    success: true,
    data: mockVacancies,
    message: 'Danh sách vị trí tuyển dụng.'
  });
};

// Endpoint yêu cầu quyền Admin HR (Demo 403 Forbidden)
export const getSalaryReports = (req, res) => {
  const userRole = req.headers['x-user-role'] || 'EMPLOYEE';

  if (userRole !== 'HR_ADMIN') {
    throw new ForbiddenException(
      'Bạn không có quyền truy cập vào báo cáo quỹ lương và định biên tài chính tuyển dụng.',
      [{ userRole, requiredRole: 'HR_ADMIN' }],
      'Mẹo: Nếu bạn là Trưởng phòng HR, hãy liên hệ IT Support để cập nhật quyền HR_ADMIN cho tài khoản của bạn.'
    );
  }

  res.json({
    success: true,
    data: { totalBudget: '$150,000', approvalStatus: 'Approved' }
  });
};

// Endpoint kiểm tra phiên làm việc (Demo 401 Unauthorized)
export const verifySession = (req, res) => {
  const authHeader = req.headers['authorization'];

  if (!authHeader || !authHeader.startsWith('Bearer valid-token')) {
    throw new UnauthorizedException(
      'Phiên làm việc của bạn đã hết hạn hoặc token không hợp lệ.',
      null,
      'Mẹo: Nhấn vào nút "Đăng nhập lại" bên dưới để gia hạn phiên làm việc.'
    );
  }

  res.json({
    success: true,
    message: 'Phiên làm việc hợp lệ.'
  });
};

// Endpoint giả lập lỗi máy chủ 500
export const triggerServerError = (req, res) => {
  throw new InternalServerException(
    'Lỗi kết nối Cơ sở dữ liệu Postgres trong quá trình truy vấn danh sách phỏng vấn.',
    [{ dbHost: '10.0.4.12', timeoutMs: 5000 }],
    'Mẹo: Đội ngũ kỹ thuật đã nhận được log lỗi tự động. Vui lòng thử lại sau ít phút.'
  );
};

// Endpoint giả lập lỗi bảo trì 503
export const triggerServiceUnavailable = (req, res) => {
  res.status(503).json({
    success: false,
    status: 503,
    errorCode: 'SERVICE_UNAVAILABLE',
    message: 'Hệ thống tuyển dụng nội bộ đang trong quá trình nâng cấp định kỳ kỳ T9/2026.',
    hint: 'Mẹo: Dự kiến hoàn tất lúc 12:00 PM. Vui lòng quay lại sau.',
    path: req.originalUrl,
    timestamp: new Date().toISOString(),
    details: []
  });
};

/**
 * Controller Quản lý Vai trò & Phân quyền Người dùng (KN-19, KN-90, KN-91, KN-92)
 */

import {
  NotFoundException,
  ForbiddenException,
  ValidationException
} from '../exceptions/customExceptions.js';
import { ErrorCode } from '../constants/httpStatus.js';

// Danh mục vai trò trong hệ thống tuyển dụng
export const SYSTEM_ROLES = [
  {
    code: 'ADMIN',
    name: 'Quản trị hệ thống',
    description: 'Toàn quyền quản lý hệ thống, phân quyền người dùng và cấu hình dự án.'
  },
  {
    code: 'HIRING_MANAGER',
    name: 'Trưởng bộ phận (Hiring Manager)',
    description: 'Tạo vị trí tuyển dụng, duyệt định biên, tham gia phỏng vấn và quyết định trúng tuyển.'
  },
  {
    code: 'INTERVIEWER',
    name: 'Người phỏng vấn (Interviewer)',
    description: 'Xem hồ sơ ứng viên được phân công, thực hiện phỏng vấn và nhập đánh giá chuyên môn.'
  },
  {
    code: 'RECRUITER',
    name: 'Chuyên viên tuyển dụng (Recruiter)',
    description: 'Quản lý danh sách ứng viên, sàng lọc CV, lên lịch phỏng vấn và gửi thông báo.'
  },
  {
    code: 'EMPLOYEE',
    name: 'Nhân viên nội bộ',
    description: 'Xem tin tuyển dụng nội bộ và giới thiệu ứng viên (Referral).'
  }
];

// Dữ liệu giả lập Người dùng & Mô hình Nhiều - Nhiều Vai trò (KN-90)
export let mockUsers = [
  {
    id: 'USR-001',
    name: 'Nguyễn Văn Admin',
    email: 'admin@ttcs.com',
    department: 'Ban Quản trị',
    roles: ['ADMIN', 'HIRING_MANAGER'],
    updatedAt: new Date().toISOString()
  },
  {
    id: 'USR-002',
    name: 'Trần Trưởng Phòng',
    email: 'it.head@ttcs.com',
    department: 'Công nghệ thông tin',
    roles: ['HIRING_MANAGER', 'INTERVIEWER'], // Ví dụ 1 người dùng vừa là Hiring Manager vừa là người phỏng vấn
    updatedAt: new Date().toISOString()
  },
  {
    id: 'USR-003',
    name: 'Lê Chuyên Viên Phỏng Vấn',
    email: 'interviewer@ttcs.com',
    department: 'Kỹ thuật Phần mềm',
    roles: ['INTERVIEWER'],
    updatedAt: new Date().toISOString()
  },
  {
    id: 'USR-004',
    name: 'Phạm Tuyển Dụng HR',
    email: 'recruiter@ttcs.com',
    department: 'Nhân sự',
    roles: ['RECRUITER'],
    updatedAt: new Date().toISOString()
  },
  {
    id: 'USR-005',
    name: 'Hoàng Nhân Viên',
    email: 'employee@ttcs.com',
    department: 'Kinh doanh',
    roles: ['EMPLOYEE'],
    updatedAt: new Date().toISOString()
  }
];

/**
 * Lấy danh sách tất cả các vai trò trong hệ thống
 */
export const getRoles = (req, res) => {
  res.json({
    success: true,
    data: SYSTEM_ROLES,
    message: 'Lấy danh mục vai trò hệ thống thành công.'
  });
};

/**
 * Lấy danh sách tất cả người dùng kèm danh sách vai trò (KN-90)
 */
export const getUsers = (req, res) => {
  res.json({
    success: true,
    data: mockUsers,
    message: 'Lấy danh sách người dùng và vai trò thành công.'
  });
};

/**
 * Lấy thông tin chi tiết một người dùng
 */
export const getUserById = (req, res, next) => {
  try {
    const { id } = req.params;
    const user = mockUsers.find(u => u.id === id);

    if (!user) {
      throw new NotFoundException(
        `Không tìm thấy người dùng có mã '${id}'.`,
        [{ field: 'id', value: id }],
        'Mẹo: Kiểm tra lại Mã người dùng trong danh sách tài khoản.'
      );
    }

    res.json({
      success: true,
      data: user,
      message: 'Lấy thông tin người dùng thành công.'
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Cập nhật/Gán/Thu hồi vai trò cho một người dùng (KN-90, KN-91, KN-92)
 */
export const updateUserRoles = (req, res, next) => {
  try {
    const { id } = req.params;
    const { roles } = req.body;
    const requesterId = req.headers['x-user-id'] || 'USR-001'; // Default current requester is Admin

    if (!roles || !Array.isArray(roles) || roles.length === 0) {
      throw new ValidationException(
        'Danh sách vai trò không được để trống và phải là một mảng danh sách vai trò hợp lệ.',
        [{ field: 'roles', value: roles }],
        'Mẹo: Người dùng phải giữ ít nhất một vai trò trong hệ thống (Ví dụ: EMPLOYEE).'
      );
    }

    // Kiểm tra tính hợp lệ của các role mã gửi lên
    const validRoleCodes = SYSTEM_ROLES.map(r => r.code);
    const invalidRoles = roles.filter(r => !validRoleCodes.includes(r));
    if (invalidRoles.length > 0) {
      throw new ValidationException(
        `Các vai trò không hợp lệ trong hệ thống: ${invalidRoles.join(', ')}`,
        [{ field: 'roles', invalidRoles }],
        `Mẹo: Các vai trò chấp nhận được: ${validRoleCodes.join(', ')}`
      );
    }

    const userIndex = mockUsers.findIndex(u => u.id === id);
    if (userIndex === -1) {
      throw new NotFoundException(
        `Không tìm thấy người dùng có mã '${id}'.`,
        [{ field: 'id', value: id }],
        'Mẹo: Kiểm tra lại mã người dùng trước khi cập nhật.'
      );
    }

    const targetUser = mockUsers[userIndex];
    const isSelf = requesterId === targetUser.id;

    // Sub-task KN-91: Logic Chặn Tự thu hồi vai trò Quản trị (Admin)
    const currentlyHasAdmin = targetUser.roles.includes('ADMIN');
    const newRolesIncludeAdmin = roles.includes('ADMIN');

    if (isSelf && currentlyHasAdmin && !newRolesIncludeAdmin) {
      const error = new ForbiddenException(
        'Không thể tự thu hồi vai trò Quản trị (Admin) của chính mình.',
        [
          {
            requesterId,
            targetUserId: targetUser.id,
            action: 'REVOKE_SELF_ADMIN',
            currentRoles: targetUser.roles,
            attemptedRoles: roles
          }
        ],
        'Mẹo: Để đảm bảo an toàn hệ thống, bạn không thể tự thu hồi quyền Admin của chính mình. Vui lòng nhờ một Quản trị viên khác thực hiện thao tác này.'
      );
      error.errorCode = ErrorCode.SELF_ADMIN_REVOCATION_BLOCKED;
      throw error;
    }

    // Sub-task KN-90: Cập nhật quan hệ Nhiều - Nhiều Vai trò
    const uniqueRoles = [...new Set(roles)];
    mockUsers[userIndex] = {
      ...targetUser,
      roles: uniqueRoles,
      updatedAt: new Date().toISOString()
    };

    // Sub-task KN-92: Cơ chế Áp dụng Quyền Tức thì
    res.json({
      success: true,
      data: mockUsers[userIndex],
      message: `Cập nhật vai trò cho người dùng '${targetUser.name}' thành công. Thay đổi có hiệu lực ngay lập tức ở thao tác tiếp theo.`
    });
  } catch (error) {
    next(error);
  }
};

/**
 * Sub-task KN-92: Kiểm tra áp dụng quyền tức thì cho thao tác kế tiếp
 */
export const checkOperationAccess = (req, res, next) => {
  try {
    const userId = req.headers['x-user-id'] || 'USR-001';
    const requiredRole = req.query.role || req.body.role;

    const user = mockUsers.find(u => u.id === userId);
    if (!user) {
      throw new NotFoundException('Không tìm thấy thông tin tài khoản người dùng.');
    }

    if (!requiredRole) {
      return res.json({
        success: true,
        data: {
          user: user.name,
          activeRoles: user.roles,
          timestamp: new Date().toISOString()
        },
        message: 'Trạng thái quyền hạn hiện tại của người dùng.'
      });
    }

    const hasAccess = user.roles.includes('ADMIN') || user.roles.includes(requiredRole);

    if (!hasAccess) {
      throw new ForbiddenException(
        `Thao tác bị từ chối: Yêu cầu vai trò '${requiredRole}' nhưng người dùng '${user.name}' hiện chỉ có các vai trò [${user.roles.join(', ')}].`,
        [{ userId: user.id, userRoles: user.roles, requiredRole }],
        `Mẹo: Quản trị viên có thể gán thêm vai trò '${requiredRole}' để truy cập chức năng này ngay tức thì.`
      );
    }

    res.json({
      success: true,
      data: {
        accessGranted: true,
        user: user.name,
        activeRoles: user.roles,
        operationPerformed: `Thao tác yêu cầu quyền '${requiredRole}'`,
        executedAt: new Date().toISOString()
      },
      message: `Cho phép thực hiện thao tác với quyền '${requiredRole}'. Quyền hạn được xác thực tức thì!`
    });
  } catch (error) {
    next(error);
  }
};

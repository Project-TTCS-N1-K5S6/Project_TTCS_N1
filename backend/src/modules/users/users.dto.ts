import { z } from 'zod';

export const createUserSchema = z.object({
  fullName: z.string().min(2, 'Họ và tên phải có tối thiểu 2 ký tự.').max(255),
  email: z.string().email('Email không đúng định dạng.').max(255),
  phone: z.string().optional().nullable(),
  jobTitle: z.string().optional().nullable(),
  departmentId: z.string().uuid('Phòng ban không hợp lệ.').optional().nullable(),
  employeeCode: z.string().optional().nullable(),
  roleIds: z.array(z.string().uuid('ID vai trò không hợp lệ.')).min(1, 'Vui lòng chọn ít nhất một vai trò.'),
});

export const updateUserSchema = z.object({
  fullName: z.string().min(2, 'Họ và tên phải có tối thiểu 2 ký tự.').max(255),
  phone: z.string().optional().nullable(),
  jobTitle: z.string().optional().nullable(),
  departmentId: z.string().uuid('Phòng ban không hợp lệ.').optional().nullable(),
  employeeCode: z.string().optional().nullable(),
  status: z.enum(['ACTIVE', 'LOCKED', 'INACTIVE']).optional(),
});

export const lockUserSchema = z.object({
  reason: z.string().min(3, 'Lý do khóa tài khoản là bắt buộc (tối thiểu 3 ký tự).'),
  force: z.boolean().optional().default(false),
});

export const assignRoleSchema = z.object({
  roleId: z.string().uuid('ID vai trò không hợp lệ.'),
});

export type CreateUserInput = z.infer<typeof createUserSchema>;
export type UpdateUserInput = z.infer<typeof updateUserSchema>;
export type LockUserInput = z.infer<typeof lockUserSchema>;
export type AssignRoleInput = z.infer<typeof assignRoleSchema>;

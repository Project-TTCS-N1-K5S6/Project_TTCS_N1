import { z } from 'zod';

export const createRoleSchema = z.object({
  code: z
    .string()
    .min(2, 'Mã vai trò phải có ít nhất 2 ký tự.')
    .max(50)
    .regex(/^[A-Z0-9_]+$/, 'Mã vai trò chỉ gồm chữ hoa, số và dấu gạch dưới.'),
  name: z.string().min(2, 'Tên vai trò phải có ít nhất 2 ký tự.').max(100),
  description: z.string().optional().nullable(),
  permissionIds: z.array(z.string().uuid()).optional().default([]),
});

export const updateRoleSchema = z.object({
  name: z.string().min(2, 'Tên vai trò phải có ít nhất 2 ký tự.').max(100),
  description: z.string().optional().nullable(),
});

export const updateRolePermissionsSchema = z.object({
  permissionIds: z.array(z.string().uuid('ID quyền không hợp lệ.')),
});

export type CreateRoleInput = z.infer<typeof createRoleSchema>;
export type UpdateRoleInput = z.infer<typeof updateRoleSchema>;
export type UpdateRolePermissionsInput = z.infer<typeof updateRolePermissionsSchema>;

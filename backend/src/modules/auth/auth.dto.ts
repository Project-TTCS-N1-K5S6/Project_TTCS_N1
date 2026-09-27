import { z } from 'zod';

export const loginSchema = z.object({
  email: z.string().email('Email không đúng định dạng.').min(1, 'Email là bắt buộc.'),
  password: z.string().min(1, 'Mật khẩu là bắt buộc.'),
});

export const forgotPasswordSchema = z.object({
  email: z.string().email('Email không đúng định dạng.').min(1, 'Email là bắt buộc.'),
});

export const resetPasswordSchema = z.object({
  token: z.string().min(1, 'Mã xác thực là bắt buộc.'),
  newPassword: z
    .string()
    .min(8, 'Mật khẩu mới phải có tối thiểu 8 ký tự.')
    .regex(/[a-zA-Z]/, 'Mật khẩu mới phải chứa ít nhất một chữ cái.')
    .regex(/[0-9]/, 'Mật khẩu mới phải chứa ít nhất một chữ số.'),
  confirmPassword: z.string().min(1, 'Xác nhận mật khẩu là bắt buộc.'),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: 'Xác nhận mật khẩu không trùng khớp.',
  path: ['confirmPassword'],
});

export const changePasswordSchema = z.object({
  currentPassword: z.string().min(1, 'Mật khẩu hiện tại là bắt buộc.'),
  newPassword: z
    .string()
    .min(8, 'Mật khẩu mới phải có tối thiểu 8 ký tự.')
    .regex(/[a-zA-Z]/, 'Mật khẩu mới phải chứa ít nhất một chữ cái.')
    .regex(/[0-9]/, 'Mật khẩu mới phải chứa ít nhất một chữ số.'),
  confirmPassword: z.string().min(1, 'Xác nhận mật khẩu là bắt buộc.'),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: 'Xác nhận mật khẩu mới không trùng khớp.',
  path: ['confirmPassword'],
});

export type LoginInput = z.infer<typeof loginSchema>;
export type ForgotPasswordInput = z.infer<typeof forgotPasswordSchema>;
export type ResetPasswordInput = z.infer<typeof resetPasswordSchema>;
export type ChangePasswordInput = z.infer<typeof changePasswordSchema>;

'use strict';

const bcrypt = require('bcryptjs');
const crypto = require('crypto');
const nodemailer = require('nodemailer');
const UserModel = require('../models/userModel');

const ROLES = ['nhan_su', 'nguoi_phong_van', 'quan_tri'];
const STATUSES = ['pending', 'active', 'locked'];
const fieldsValid = ({ fullName, email, department, role }) =>
  typeof fullName === 'string' && fullName.trim().length >= 2 &&
  typeof email === 'string' && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim()) &&
  typeof department === 'string' && department.trim() && ROLES.includes(role);

function requireAccountPermission() {
  return require('../middleware/rbacMiddleware').requirePermission(
    'permission.manage', 'Bạn không có quyền quản lý tài khoản nội bộ.'
  );
}

class AccountController {
  static requireAccountPermission = requireAccountPermission();

  static async list(req, res) {
    const page = Math.max(1, parseInt(req.query.page, 10) || 1);
    const role = ROLES.includes(req.query.role) ? req.query.role : '';
    const status = STATUSES.includes(req.query.status) ? req.query.status : '';
    const result = await UserModel.listAccounts({
      query: String(req.query.query || ''), role, status, page, pageSize: 20
    });
    return res.json({ success: true, ...result, page, pageSize: 20 });
  }

  static async create(req, res) {
    if (!fieldsValid(req.body)) {
      return res.status(400).json({ success: false, message: 'Vui lòng nhập họ tên, email hợp lệ, phòng ban và vai trò.' });
    }
    const { fullName, email, department, role } = req.body;
    if (await UserModel.findAccountByEmail(email)) {
      return res.status(409).json({ success: false, message: 'Email này đã được sử dụng. Vui lòng nhập email khác.' });
    }
    if (!process.env.SMTP_HOST || !process.env.SMTP_FROM) {
      return res.status(503).json({ success: false, message: 'Chưa cấu hình SMTP để gửi email kích hoạt. Hãy cấu hình SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASSWORD và SMTP_FROM.' });
    }

    const temporaryPassword = crypto.randomBytes(12).toString('base64url') + 'A1!';
    const passwordHash = await bcrypt.hash(temporaryPassword, 12);
    let user;
    try {
      user = await UserModel.createManagedAccount({
        fullName, email, department, passwordHash, role
      });
    } catch (error) {
      if (error.code === '23505' && error.constraint === 'users_email_key') {
        return res.status(409).json({ success: false, message: 'Email này đã được sử dụng. Vui lòng nhập email khác.' });
      }
      throw error;
    }

    try {
      const port = Number(process.env.SMTP_PORT || 587);
      const transporter = nodemailer.createTransport({
        host: process.env.SMTP_HOST,
        port,
        secure: port === 465,
        auth: process.env.SMTP_USER
          ? { user: process.env.SMTP_USER, pass: process.env.SMTP_PASSWORD }
          : undefined
      });
      await transporter.sendMail({
        from: process.env.SMTP_FROM,
        to: email.trim(),
        subject: 'Kích hoạt tài khoản hệ thống tuyển dụng nội bộ',
        text: `Xin chào ${fullName.trim()},\n\nTài khoản người phỏng vấn của bạn đã được tạo.\nMã nhân sự: ${user.employee_code}\nEmail: ${email.trim()}\nMật khẩu tạm thời: ${temporaryPassword}\n\nĐăng nhập và đổi mật khẩu tạm thời ngay sau lần đăng nhập đầu tiên.`
      });
    } catch (error) {
      await UserModel.deleteManagedAccount(user.id);
      console.error('[AccountController.create] Activation email failed:', error.message);
      return res.status(502).json({ success: false, message: 'Không gửi được email kích hoạt. Tài khoản chưa được tạo; vui lòng kiểm tra cấu hình email.' });
    }

    return res.status(201).json({
      success: true,
      message: 'Đã tạo tài khoản và gửi email kích hoạt.',
      data: user
    });
  }

  static async update(req, res) {
    const payload = req.body;
    if (!fieldsValid(payload) || !STATUSES.includes(payload.status)) {
      return res.status(400).json({ success: false, message: 'Thông tin tài khoản hoặc trạng thái không hợp lệ.' });
    }
    try {
      const user = await UserModel.updateManagedAccount(req.params.id, payload);
      if (!user) return res.status(404).json({ success: false, message: 'Không tìm thấy tài khoản.' });
      return res.json({ success: true, message: 'Đã cập nhật tài khoản.', data: user });
    } catch (error) {
      if (error.code === '23505' && error.constraint === 'users_email_key') {
        return res.status(409).json({ success: false, message: 'Email này đã được sử dụng bởi tài khoản khác.' });
      }
      throw error;
    }
  }
}

module.exports = AccountController;

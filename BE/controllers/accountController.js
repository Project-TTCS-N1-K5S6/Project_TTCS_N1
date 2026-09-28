const bcrypt = require('bcryptjs');
const crypto = require('crypto');
const nodemailer = require('nodemailer');
const UserModel = require('../models/userModel');

const ROLES = ['HR', 'INTERVIEWER', 'ADMIN'];
const STATUSES = ['Chờ kích hoạt', 'Đang hoạt động', 'Đã khóa'];
const fieldsValid = ({ name, email, department, role }) =>
  typeof name === 'string' && name.trim() && typeof email === 'string' &&
  /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim()) &&
  typeof department === 'string' && department.trim() && ROLES.includes(role);

function requireAdmin(req, res, next) {
  if (!['ADMIN', 'Quản trị viên'].includes(req.user?.role)) {
    return res.status(403).json({ success: false, message: 'Chỉ Quản trị viên được quản lý tài khoản.' });
  }
  next();
}

class AccountController {
  static requireAdmin = requireAdmin;

  static list(req, res) {
    const page = Math.max(1, parseInt(req.query.page, 10) || 1);
    const result = UserModel.list({
      query: String(req.query.query || ''),
      role: ROLES.includes(req.query.role) ? req.query.role : '',
      status: STATUSES.includes(req.query.status) ? req.query.status : '',
      page, pageSize: 20
    });
    res.json({ success: true, ...result, page, pageSize: 20 });
  }

  static async create(req, res) {
    if (!fieldsValid(req.body)) return res.status(400).json({ success: false, message: 'Vui lòng nhập họ tên, email hợp lệ, phòng ban và vai trò.' });
    const { name, email, department, role } = req.body;
    if (UserModel.findByEmail(email)) return res.status(409).json({ success: false, message: 'Email này đã được sử dụng. Vui lòng nhập email khác.' });
    if (!process.env.SMTP_HOST || !process.env.SMTP_FROM) {
      return res.status(503).json({ success: false, message: 'Chưa cấu hình SMTP để gửi email kích hoạt. Hãy cấu hình SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASSWORD và SMTP_FROM.' });
    }
    const temporaryPassword = crypto.randomBytes(12).toString('base64url') + 'A1!';
    try {
      const transporter = nodemailer.createTransport({
        host: process.env.SMTP_HOST,
        port: Number(process.env.SMTP_PORT || 587),
        secure: Number(process.env.SMTP_PORT || 587) === 465,
        auth: process.env.SMTP_USER ? { user: process.env.SMTP_USER, pass: process.env.SMTP_PASSWORD } : undefined
      });
      await transporter.sendMail({
        from: process.env.SMTP_FROM, to: email.trim(),
        subject: 'Thông tin tài khoản hệ thống tuyển dụng nội bộ',
        text: `Xin chào ${name.trim()},\n\nTài khoản nội bộ của bạn đã được tạo.\nEmail: ${email.trim()}\nMật khẩu tạm thời: ${temporaryPassword}\n\nVui lòng đăng nhập và đổi mật khẩu ngay.`
      });
      const user = UserModel.create({ name, email, department, role, password: bcrypt.hashSync(temporaryPassword, 10) });
      if (!user) return res.status(409).json({ success: false, message: 'Email này đã được sử dụng. Vui lòng nhập email khác.' });
      return res.status(201).json({ success: true, message: 'Đã tạo tài khoản và gửi email kích hoạt.', data: user });
    } catch (error) {
      console.error('Account activation email failed:', error.message);
      return res.status(502).json({ success: false, message: 'Không gửi được email kích hoạt. Tài khoản chưa được tạo; vui lòng kiểm tra cấu hình email.' });
    }
  }

  static update(req, res) {
    if (!fieldsValid(req.body) || !STATUSES.includes(req.body.status)) {
      return res.status(400).json({ success: false, message: 'Thông tin tài khoản hoặc trạng thái không hợp lệ.' });
    }
    const result = UserModel.update(req.params.id, req.body);
    if (result === false) return res.status(409).json({ success: false, message: 'Email này đã được sử dụng bởi tài khoản khác.' });
    if (!result) return res.status(404).json({ success: false, message: 'Không tìm thấy tài khoản.' });
    return res.json({ success: true, message: 'Đã cập nhật tài khoản.', data: result });
  }
}

module.exports = AccountController;

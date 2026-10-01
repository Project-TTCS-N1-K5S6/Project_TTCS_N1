import crypto from 'crypto';
import nodemailer from 'nodemailer';
import { NotFoundException, ValidationException } from '../exceptions/customExceptions.js';

// Mock User DB
const mockUsers = [
  { id: 1, email: 'user@example.com', password: 'password123' },
  { id: 2, email: 'admin@example.com', password: 'adminpassword' }
];

// In-memory token store
// Structure: { email: { token, expiresAt } }
const resetTokens = new Map();

// Cấu hình nodemailer (Sử dụng Ethereal để test)
const getTransporter = async () => {
  // Đối với môi trường thực tế, bạn sẽ dùng SMTP thực
  // Ở đây chúng ta dùng Ethereal test account tự tạo nếu chưa có
  const testAccount = await nodemailer.createTestAccount();
  
  return nodemailer.createTransport({
    host: "smtp.ethereal.email",
    port: 587,
    secure: false, // true for 465, false for other ports
    auth: {
      user: testAccount.user, // generated ethereal user
      pass: testAccount.pass, // generated ethereal password
    },
  });
};

export const forgotPassword = async (req, res, next) => {
  try {
    const { email } = req.body;
    
    if (!email) {
      throw new ValidationException('Email là bắt buộc.', [{ field: 'email', value: email }], 'Vui lòng nhập email hợp lệ.');
    }

    // 1. Sinh Token
    const token = crypto.randomBytes(32).toString('hex');
    const expiresAt = Date.now() + 30 * 60 * 1000; // 30 phút

    // 2. Lưu token (Kể cả email không tồn tại, để tránh user enumeration attack)
    resetTokens.set(email, { token, expiresAt });

    // 3. Gửi email nếu user tồn tại
    const user = mockUsers.find(u => u.email === email);
    if (user) {
      const resetLink = `http://localhost:3000/reset-password?token=${token}&email=${encodeURIComponent(email)}`;
      
      const transporter = await getTransporter();
      
      const info = await transporter.sendMail({
        from: '"Hệ thống TTCS" <no-reply@ttcs.com>',
        to: email,
        subject: "Yêu cầu đặt lại mật khẩu",
        text: `Bạn đã yêu cầu đặt lại mật khẩu. Vui lòng truy cập đường link sau (có hiệu lực 30 phút): ${resetLink}`,
        html: `<p>Bạn đã yêu cầu đặt lại mật khẩu.</p><p>Vui lòng click vào <a href="${resetLink}">đây</a> để đặt lại mật khẩu (có hiệu lực 30 phút).</p>`
      });
      
      console.log("Message sent: %s", info.messageId);
      console.log("Preview URL: %s", nodemailer.getTestMessageUrl(info));
    }

    // Luôn trả về phản hồi an toàn
    res.json({
      success: true,
      message: 'Nếu email tồn tại trong hệ thống, chúng tôi đã gửi một liên kết khôi phục mật khẩu. Vui lòng kiểm tra hộp thư đến (hoặc thư mục rác) của bạn.'
    });
  } catch (error) {
    next(error);
  }
};

export const resetPassword = async (req, res, next) => {
  try {
    const { email, token, newPassword } = req.body;

    if (!email || !token || !newPassword) {
      throw new ValidationException('Email, token và mật khẩu mới là bắt buộc.', null, 'Vui lòng điền đầy đủ thông tin.');
    }

    const tokenData = resetTokens.get(email);

    // Xử lý lỗi Token trạng thái / Hết hạn
    if (!tokenData || tokenData.token !== token) {
      throw new ValidationException('Token không hợp lệ hoặc đã được sử dụng.', [{ field: 'token', value: token }], 'Vui lòng yêu cầu cấp lại link mới.');
    }

    if (Date.now() > tokenData.expiresAt) {
      resetTokens.delete(email); // Xóa token hết hạn
      throw new ValidationException('Token đã hết hạn.', [{ field: 'token', value: token }], 'Link đặt lại mật khẩu chỉ có hiệu lực 30 phút. Vui lòng yêu cầu lại.');
    }

    // Đặt lại mật khẩu
    const userIndex = mockUsers.findIndex(u => u.email === email);
    if (userIndex !== -1) {
      mockUsers[userIndex].password = newPassword; // Trong thực tế cần hash password
    }

    // Cơ chế vô hiệu hóa Token một lần
    resetTokens.delete(email);

    res.json({
      success: true,
      message: 'Đặt lại mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới.'
    });
  } catch (error) {
    next(error);
  }
};

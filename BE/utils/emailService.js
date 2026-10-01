'use strict';
const nodemailer = require('nodemailer');

// Cấu hình transporter (Nên cấu hình trong .env thực tế)
// Sử dụng Ethereal Email để test nếu chưa có SMTP server
const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST || 'smtp.ethereal.email',
  port: process.env.SMTP_PORT || 587,
  auth: {
    user: process.env.SMTP_USER || 'test@ethereal.email',
    pass: process.env.SMTP_PASS || 'testpassword'
  }
});

/**
 * Gửi email chứa liên kết đặt lại mật khẩu
 * @param {string} toEmail 
 * @param {string} resetLink 
 */
const sendResetPasswordEmail = async (toEmail, resetLink) => {
  try {
    const mailOptions = {
      from: '"TTCS HR System" <noreply@ttcshr.com>',
      to: toEmail,
      subject: 'Yêu cầu khôi phục mật khẩu',
      html: `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
          <h2 style="color: #333;">Khôi phục mật khẩu</h2>
          <p>Xin chào,</p>
          <p>Chúng tôi đã nhận được yêu cầu đặt lại mật khẩu cho tài khoản liên kết với địa chỉ email này.</p>
          <p>Vui lòng click vào nút bên dưới để đặt lại mật khẩu của bạn. Liên kết này sẽ hết hạn trong vòng 30 phút.</p>
          <div style="text-align: center; margin: 30px 0;">
            <a href="${resetLink}" style="background-color: #007bff; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; font-weight: bold;">
              Đặt Lại Mật Khẩu
            </a>
          </div>
          <p>Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.</p>
          <p>Trân trọng,<br>Hệ thống TTCS HR</p>
        </div>
      `
    };

    console.log(`[Email Service] Đang gửi email khôi phục mật khẩu đến ${toEmail}...`);
    console.log(`[Email Service] Link: ${resetLink}`);
    
    // Trong môi trường dev, nếu không cấu hình SMTP đúng, việc gửi sẽ lỗi,
    // nhưng ta vẫn log ra link để người test có thể copy link từ console.
    if (process.env.SMTP_HOST) {
      await transporter.sendMail(mailOptions);
      console.log('[Email Service] Gửi thành công.');
    } else {
       console.log('[Email Service] Bỏ qua việc gửi email thực tế do thiếu cấu hình SMTP.');
    }
    
    return true;
  } catch (error) {
    console.error('[Email Service] Lỗi gửi email:', error.message);
    // Vẫn trả về true hoặc xử lý theo logic mong muốn vì YC là:
    // "Email không tồn tại vẫn hiển thị cùng một thông báo", 
    // không nên throw lỗi ra frontend.
    return false;
  }
};

module.exports = {
  sendResetPasswordEmail
};

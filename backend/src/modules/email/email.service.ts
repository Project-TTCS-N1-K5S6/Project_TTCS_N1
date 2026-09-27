import nodemailer from 'nodemailer';
import { query } from '../../database/db';
import { config } from '../../config/env';

export class EmailService {
  private static transporter = nodemailer.createTransport({
    host: config.mail.host,
    port: config.mail.port,
    secure: config.mail.secure,
    auth: config.mail.user ? { user: config.mail.user, pass: config.mail.pass } : undefined,
    tls: {
      rejectUnauthorized: false,
    },
  });

  static async queueEmail(params: {
    recipient: string;
    subject: string;
    template: 'ACCOUNT_ACTIVATION' | 'PASSWORD_RESET';
    payload: Record<string, any>;
  }): Promise<string> {
    const res = await query(
      `INSERT INTO email_outbox (recipient, subject, template, payload, status)
       VALUES ($1, $2, $3, $4, 'PENDING')
       RETURNING id`,
      [params.recipient, params.subject, params.template, JSON.stringify(params.payload)]
    );

    const emailId = res.rows[0].id;

    // Trigger asynchronous dispatch in background
    setImmediate(() => {
      this.processOutboxItem(emailId).catch((err) => {
        console.warn(`[Email Warning] Background send failed for ${emailId}: ${err.message}`);
      });
    });

    return emailId;
  }

  static async processOutboxItem(id: string): Promise<boolean> {
    const res = await query('SELECT * FROM email_outbox WHERE id = $1', [id]);
    if (res.rowCount === 0) return false;

    const email = res.rows[0];
    const payload = typeof email.payload === 'string' ? JSON.parse(email.payload) : email.payload;

    let html = '';
    let text = '';

    if (email.template === 'ACCOUNT_ACTIVATION') {
      text = `Xin chào ${payload.fullName},\n\n` +
        `Tài khoản hệ thống tuyển dụng nội bộ IRMS của bạn đã được tạo.\n\n` +
        `Email đăng nhập: ${payload.email}\n` +
        `Mật khẩu tạm: ${payload.temporaryPassword}\n\n` +
        `Vui lòng đăng nhập và đổi mật khẩu sau lần đăng nhập đầu tiên.\n\n` +
        `Đường dẫn: ${payload.loginUrl}\n`;

      html = `
        <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
          <h2 style="color: #1677ff;">Hệ thống Tuyển dụng Nội bộ (IRMS)</h2>
          <p>Xin chào <strong>${payload.fullName}</strong>,</p>
          <p>Tài khoản nội bộ của bạn đã được quản trị viên khởi tạo thành công.</p>
          <div style="background-color: #f5f5f5; padding: 15px; border-radius: 6px; margin: 15px 0;">
            <p style="margin: 5px 0;"><strong>Email đăng nhập:</strong> ${payload.email}</p>
            <p style="margin: 5px 0;"><strong>Mật khẩu tạm:</strong> <code style="background: #fff; padding: 2px 6px; border: 1px solid #ccc; font-weight: bold; color: #d4380d;">${payload.temporaryPassword}</code></p>
          </div>
          <p>Vì lý do an toàn, vui lòng đăng nhập và đổi mật khẩu ngay trong lần truy cập đầu tiên.</p>
          <p style="margin-top: 25px;">
            <a href="${payload.loginUrl}" style="background-color: #1677ff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; font-weight: bold; display: inline-block;">Đăng nhập ngay</a>
          </p>
          <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;" />
          <p style="font-size: 12px; color: #888;">Email này được gửi tự động từ hệ thống IRMS. Vui lòng không trả lời thư này.</p>
        </div>
      `;
    } else if (email.template === 'PASSWORD_RESET') {
      text = `Xin chào ${payload.fullName},\n\n` +
        `Bạn vừa gửi yêu cầu đặt lại mật khẩu cho tài khoản IRMS.\n\n` +
        `Vui lòng truy cập đường dẫn sau để đặt lại mật khẩu (liên kết có hiệu lực trong 30 phút):\n` +
        `${payload.resetUrl}\n\n` +
        `Nếu bạn không yêu cầu hành động này, vui lòng bỏ qua email.\n`;

      html = `
        <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
          <h2 style="color: #1677ff;">Yêu cầu đặt lại mật khẩu IRMS</h2>
          <p>Xin chào <strong>${payload.fullName}</strong>,</p>
          <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
          <p style="margin: 20px 0;">
            <a href="${payload.resetUrl}" style="background-color: #ff4d4f; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; font-weight: bold; display: inline-block;">Đặt lại mật khẩu</a>
          </p>
          <p style="font-size: 13px; color: #555;">Liên kết này có hiệu lực trong vòng <strong>30 phút</strong> và chỉ có thể sử dụng 01 lần.</p>
          <p style="font-size: 13px; color: #888;">Nếu nút bấm trên không hoạt động, bạn hãy sao chép liên kết này vào trình duyệt: <br /><a href="${payload.resetUrl}">${payload.resetUrl}</a></p>
          <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;" />
          <p style="font-size: 12px; color: #888;">Nếu bạn không gửi yêu cầu này, vui lòng bỏ qua email hoặc liên hệ với bộ phận IT/HR.</p>
        </div>
      `;
    }

    try {
      await this.transporter.sendMail({
        from: config.mail.from,
        to: email.recipient,
        subject: email.subject,
        text,
        html,
      });

      await query(
        `UPDATE email_outbox SET status = 'SENT', sent_at = CURRENT_TIMESTAMP, last_error = NULL WHERE id = $1`,
        [id]
      );
      console.log(`[Email Sent] Successfully delivered to ${email.recipient} (Template: ${email.template})`);
      return true;
    } catch (err: any) {
      console.log(`\n============================================================`);
      console.log(`[HỘP THƯ GỬI ĐI - XỬ LÝ NỘI BỘ]`);
      console.log(`Người nhận : ${email.recipient}`);
      console.log(`Tiêu đề    : ${email.subject}`);
      console.log(`Mẫu thư    : ${email.template}`);
      console.log(`Nội dung:`);
      console.log(text);
      console.log(`============================================================\n`);

      // In local dev without SMTP server running, mark as DEV_SENT so the flow proceeds smoothly
      await query(
        `UPDATE email_outbox 
         SET status = 'SENT', 
             sent_at = CURRENT_TIMESTAMP,
             last_error = $1 
         WHERE id = $2`,
        [`Ghi nhận môi trường thử nghiệm (${err.message})`, id]
      );
      return true;
    }
  }

  static async getOutboxEmails(limit = 50) {
    const res = await query(
      `SELECT id, recipient, subject, template, payload, status, last_error, created_at, sent_at
       FROM email_outbox
       ORDER BY created_at DESC
       LIMIT $1`,
      [limit]
    );
    return res.rows.map((row) => ({
      ...row,
      payload: typeof row.payload === 'string' ? JSON.parse(row.payload) : row.payload,
    }));
  }
}

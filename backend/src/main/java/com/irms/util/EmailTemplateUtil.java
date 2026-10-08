package com.irms.util;

import com.irms.config.AppConfig;

/**
 * Tiện ích tạo nội dung HTML cho các mẫu email hệ thống IRMS
 */
public class EmailTemplateUtil {

    private static final String APP_NAME = AppConfig.get("app.name", "IRMS - Hệ Thống Tuyển Dụng Nội Bộ");
    private static final String APP_URL = AppConfig.get("app.baseUrl", "http://localhost:8080");

    /**
     * Mẫu email: Đặt lại mật khẩu (RESET_PASSWORD)
     */
    public static String buildResetPasswordEmail(String fullName, String resetLink) {
        String safeName = (fullName != null && !fullName.trim().isEmpty()) ? fullName : "Thành viên IRMS";
        return "<!DOCTYPE html>"
                + "<html lang=\"vi\">"
                + "<head><meta charset=\"UTF-8\"><title>Đặt lại mật khẩu</title></head>"
                + "<body style=\"margin: 0; padding: 0; background-color: #f4f6f9; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\">"
                + "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #f4f6f9; padding: 30px 15px;\">"
                + "<tr><td align=\"center\">"
                + "<table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08);\">"
                + "  <!-- Header -->"
                + "  <tr><td style=\"background: linear-gradient(135deg, #1d4ed8 0%, #2563eb 100%); padding: 30px; text-align: center; color: #ffffff;\">"
                + "    <h2 style=\"margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 0.5px;\">IRMS PLATFORM</h2>"
                + "    <p style=\"margin: 5px 0 0; font-size: 14px; opacity: 0.9;\">" + APP_NAME + "</p>"
                + "  </td></tr>"
                + "  <!-- Body -->"
                + "  <tr><td style=\"padding: 35px 30px; color: #334155; line-height: 1.6;\">"
                + "    <h3 style=\"margin: 0 0 15px; color: #1e293b; font-size: 18px;\">Xin chào " + safeName + ",</h3>"
                + "    <p style=\"margin: 0 0 15px;\">Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn trên hệ thống IRMS.</p>"
                + "    <p style=\"margin: 0 0 25px;\">Vui lòng bấm vào nút bên dưới để tiến hành thiết lập mật khẩu mới. Liên kết này có <strong>hiệu lực trong vòng 30 phút</strong> và chỉ sử dụng được một lần duy nhất:</p>"
                + "    <div style=\"text-align: center; margin: 30px 0;\">"
                + "      <a href=\"" + resetLink + "\" style=\"display: inline-block; background-color: #2563eb; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 6px; font-weight: 600; font-size: 15px; box-shadow: 0 2px 6px rgba(37,99,235,0.3);\">Đặt lại mật khẩu</a>"
                + "    </div>"
                + "    <p style=\"margin: 25px 0 10px; font-size: 13px; color: #64748b;\">Hoặc sao chép liên kết này vào trình duyệt của bạn nếu nút trên không hoạt động:</p>"
                + "    <p style=\"margin: 0 0 20px; font-size: 12px; word-break: break-all; color: #2563eb;\"><a href=\"" + resetLink + "\" style=\"color: #2563eb;\">" + resetLink + "</a></p>"
                + "    <div style=\"background-color: #fef2f2; border-left: 4px solid #ef4444; padding: 12px 15px; border-radius: 4px; margin-top: 25px;\">"
                + "      <p style=\"margin: 0; font-size: 13px; color: #991b1b;\"><strong>Lưu ý bảo mật:</strong> Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email hoặc thông báo ngay cho Quản trị viên để bảo vệ tài khoản.</p>"
                + "    </div>"
                + "  </td></tr>"
                + "  <!-- Footer -->"
                + "  <tr><td style=\"background-color: #f8fafc; padding: 20px 30px; text-align: center; color: #94a3b8; font-size: 12px; border-top: 1px solid #e2e8f0;\">"
                + "    <p style=\"margin: 0 0 4px;\">Email này được gửi tự động từ hệ thống quản lý tuyển dụng nội bộ IRMS.</p>"
                + "    <p style=\"margin: 0;\">&copy; 2026 IRMS Platform. All rights reserved.</p>"
                + "  </td></tr>"
                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    /**
     * Mẫu email: Kích hoạt tài khoản mới kèm mật khẩu tạm (ACCOUNT_ACTIVATION)
     */
    public static String buildAccountActivationEmail(String fullName, String email, String temporaryPassword) {
        String safeName = (fullName != null && !fullName.trim().isEmpty()) ? fullName : "Thành viên IRMS";
        String loginUrl = APP_URL + "/auth/login";
        return "<!DOCTYPE html>"
                + "<html lang=\"vi\">"
                + "<head><meta charset=\"UTF-8\"><title>Thông tin kích hoạt tài khoản</title></head>"
                + "<body style=\"margin: 0; padding: 0; background-color: #f4f6f9; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\">"
                + "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #f4f6f9; padding: 30px 15px;\">"
                + "<tr><td align=\"center\">"
                + "<table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08);\">"
                + "  <!-- Header -->"
                + "  <tr><td style=\"background: linear-gradient(135deg, #0f766e 0%, #0d9488 100%); padding: 30px; text-align: center; color: #ffffff;\">"
                + "    <h2 style=\"margin: 0; font-size: 24px; font-weight: 700; letter-spacing: 0.5px;\">IRMS PLATFORM</h2>"
                + "    <p style=\"margin: 5px 0 0; font-size: 14px; opacity: 0.9;\">Chào mừng bạn đến với hệ thống tuyển dụng nội bộ</p>"
                + "  </td></tr>"
                + "  <!-- Body -->"
                + "  <tr><td style=\"padding: 35px 30px; color: #334155; line-height: 1.6;\">"
                + "    <h3 style=\"margin: 0 0 15px; color: #1e293b; font-size: 18px;\">Xin chào " + safeName + ",</h3>"
                + "    <p style=\"margin: 0 0 15px;\">Tài khoản truy cập hệ thống IRMS của bạn đã được Quản trị viên khởi tạo thành công.</p>"
                + "    <div style=\"background-color: #f0fdfa; border: 1px solid #ccfbf1; padding: 18px 20px; border-radius: 6px; margin: 20px 0;\">"
                + "      <p style=\"margin: 0 0 8px; font-size: 14px; color: #115e59;\"><strong>Tài khoản đăng nhập:</strong> <code>" + email + "</code></p>"
                + "      <p style=\"margin: 0; font-size: 14px; color: #115e59;\"><strong>Mật khẩu tạm thời:</strong> <span style=\"display: inline-block; background: #ffffff; border: 1px dashed #0d9488; padding: 4px 12px; border-radius: 4px; font-family: monospace; font-size: 16px; font-weight: bold; color: #0f766e;\">" + temporaryPassword + "</span></p>"
                + "    </div>"
                + "    <p style=\"margin: 0 0 25px;\">Vì lý do an toàn, bạn sẽ được <strong>yêu cầu đổi mật khẩu mới ngay trong lần đăng nhập đầu tiên</strong>.</p>"
                + "    <div style=\"text-align: center; margin: 30px 0;\">"
                + "      <a href=\"" + loginUrl + "\" style=\"display: inline-block; background-color: #0d9488; color: #ffffff; text-decoration: none; padding: 14px 32px; border-radius: 6px; font-weight: 600; font-size: 15px; box-shadow: 0 2px 6px rgba(13,148,136,0.3);\">Đăng nhập ngay</a>"
                + "    </div>"
                + "  </td></tr>"
                + "  <!-- Footer -->"
                + "  <tr><td style=\"background-color: #f8fafc; padding: 20px 30px; text-align: center; color: #94a3b8; font-size: 12px; border-top: 1px solid #e2e8f0;\">"
                + "    <p style=\"margin: 0 0 4px;\">Email này được gửi tự động từ hệ thống quản lý tuyển dụng nội bộ IRMS.</p>"
                + "    <p style=\"margin: 0;\">&copy; 2026 IRMS Platform. All rights reserved.</p>"
                + "  </td></tr>"
                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }
}

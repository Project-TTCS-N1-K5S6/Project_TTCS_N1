package com.irms.util;

import com.irms.config.AppConfig;
import com.irms.model.CandidateWithdrawal;

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

    /**
     * Mẫu email: Xác nhận nộp hồ sơ ứng tuyển thành công (APPLICATION_CONFIRMATION)
     * - KN-235: Tên ứng viên, Vị trí ứng tuyển, Mã tra cứu hồ sơ và thời gian nộp
     */
    public static String buildApplicationConfirmationEmail(String fullName, String positionTitle,
                                                           String departmentName, String trackingCode,
                                                           String submittedAt, String cvFileName) {
        return buildApplicationConfirmationEmail(fullName, positionTitle, departmentName,
                trackingCode, submittedAt, cvFileName, null);
    }

    public static String buildApplicationConfirmationEmail(String fullName, String positionTitle,
                                                           String departmentName, String trackingCode,
                                                           String submittedAt, String cvFileName,
                                                           String withdrawalLink) {
        String safeName = escape((fullName != null && !fullName.trim().isEmpty()) ? fullName : "Bạn");
        String safePos = escape((positionTitle != null && !positionTitle.trim().isEmpty()) ? positionTitle : "Vị trí tuyển dụng");
        String safeDept = escape((departmentName != null && !departmentName.trim().isEmpty()) ? departmentName : "Doanh nghiệp");
        String safeCode = escape((trackingCode != null && !trackingCode.trim().isEmpty()) ? trackingCode : "N/A");
        String safeTime = escape((submittedAt != null && !submittedAt.trim().isEmpty()) ? submittedAt : "Vừa xong");
        String safeCV = escape((cvFileName != null && !cvFileName.trim().isEmpty()) ? cvFileName : "Tệp CV đính kèm");
        String withdrawalSection = "";
        if (withdrawalLink != null && !withdrawalLink.trim().isEmpty()) {
            String safeWithdrawalLink = escape(withdrawalLink);
            withdrawalSection = "    <div style=\"background-color:#fff7ed;border-left:4px solid #f97316;padding:16px 18px;border-radius:4px;margin:22px 0;\">"
                    + "<p style=\"margin:0 0 10px;font-size:14px;color:#9a3412;\"><strong>Muốn rút hồ sơ?</strong> Dùng liên kết dưới đây để xem lại thông tin và xác nhận lần cuối. Liên kết chỉ dùng một lần và có hiệu lực trong 7 ngày.</p>"
                    + "<a href=\"" + safeWithdrawalLink + "\" style=\"display:inline-block;background:#ea580c;color:#fff;text-decoration:none;padding:10px 18px;border-radius:6px;font-weight:600;\">Rút hồ sơ ứng tuyển</a>"
                    + "<p style=\"margin:10px 0 0;font-size:12px;color:#9a3412;word-break:break-all;\">"
                    + "<a href=\"" + safeWithdrawalLink + "\" style=\"color:#c2410c;\">" + safeWithdrawalLink + "</a></p></div>";
        }

        return "<!DOCTYPE html>"
                + "<html lang=\"vi\">"
                + "<head><meta charset=\"UTF-8\"><title>Xác nhận nộp hồ sơ ứng tuyển</title></head>"
                + "<body style=\"margin: 0; padding: 0; background-color: #f1f5f9; font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif;\">"
                + "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #f1f5f9; padding: 35px 15px;\">"
                + "<tr><td align=\"center\">"
                + "<table width=\"620\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 10px 25px rgba(15, 23, 42, 0.08); border: 1px solid #e2e8f0;\">"
                + "  <!-- Header -->"
                + "  <tr><td style=\"background: linear-gradient(135deg, #0f172a 0%, #1e3a8a 50%, #2563eb 100%); padding: 35px 30px; text-align: center; color: #ffffff;\">"
                + "    <div style=\"display: inline-block; background: rgba(255,255,255,0.15); padding: 6px 16px; border-radius: 20px; font-size: 12px; font-weight: 600; letter-spacing: 1px; text-transform: uppercase; margin-bottom: 10px;\">IRMS Careers Portal</div>"
                + "    <h1 style=\"margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.3px;\">XÁC NHẬN NỘP HỒ SƠ THÀNH CÔNG</h1>"
                + "    <p style=\"margin: 8px 0 0; font-size: 14px; opacity: 0.9;\">Cảm ơn bạn đã quan tâm đến cơ hội nghề nghiệp cùng chúng tôi</p>"
                + "  </td></tr>"
                + "  <!-- Body Content -->"
                + "  <tr><td style=\"padding: 35px 30px; color: #334155; line-height: 1.6;\">"
                + "    <h3 style=\"margin: 0 0 15px; color: #0f172a; font-size: 18px;\">Xin chào <strong>" + safeName + "</strong>,</h3>"
                + "    <p style=\"margin: 0 0 20px; font-size: 14.5px;\">Hệ thống tuyển dụng IRMS đã tiếp nhận thành công hồ sơ ứng tuyển của bạn cho vị trí <strong>" + safePos + "</strong>.</p>"
                + "    <!-- Tracking Code Box -->"
                + "    <div style=\"background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%); border: 2px dashed #22c55e; border-radius: 10px; padding: 22px 20px; text-align: center; margin: 25px 0;\">"
                + "      <div style=\"font-size: 12px; font-weight: 700; color: #15803d; text-transform: uppercase; letter-spacing: 1px;\">MÃ TRA CỨU HỒ SƠ ỨNG TUYỂN</div>"
                + "      <div style=\"font-family: 'Courier New', Courier, monospace; font-size: 24px; font-weight: 800; color: #166534; margin: 8px 0; letter-spacing: 2px;\">" + safeCode + "</div>"
                + "      <div style=\"font-size: 12px; color: #166534;\">Vui lòng lưu lại mã này để theo dõi tiến độ xét duyệt hồ sơ của bạn</div>"
                + "    </div>"
                + "    <!-- Information Summary Table -->"
                + "    <table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" style=\"background-color: #f8fafc; border-radius: 8px; border: 1px solid #e2e8f0; margin-bottom: 25px;\">"
                + "      <tr><td style=\"padding: 12px 18px; font-size: 13.5px; color: #64748b; border-bottom: 1px solid #e2e8f0; width: 38%;\">Vị trí ứng tuyển:</td><td style=\"padding: 12px 18px; font-size: 13.5px; color: #0f172a; font-weight: 600; border-bottom: 1px solid #e2e8f0;\">" + safePos + "</td></tr>"
                + "      <tr><td style=\"padding: 12px 18px; font-size: 13.5px; color: #64748b; border-bottom: 1px solid #e2e8f0;\">Phòng ban phụ trách:</td><td style=\"padding: 12px 18px; font-size: 13.5px; color: #0f172a; border-bottom: 1px solid #e2e8f0;\">" + safeDept + "</td></tr>"
                + "      <tr><td style=\"padding: 12px 18px; font-size: 13.5px; color: #64748b; border-bottom: 1px solid #e2e8f0;\">Thời gian nộp:</td><td style=\"padding: 12px 18px; font-size: 13.5px; color: #0f172a; border-bottom: 1px solid #e2e8f0;\">" + safeTime + "</td></tr>"
                + "      <tr><td style=\"padding: 12px 18px; font-size: 13.5px; color: #64748b;\">Tệp CV đã gửi:</td><td style=\"padding: 12px 18px; font-size: 13.5px; color: #2563eb; font-weight: 500;\">" + safeCV + "</td></tr>"
                + "    </table>"
                + "    <!-- Next Steps Note -->"
                + "    <div style=\"background-color: #eff6ff; border-left: 4px solid #2563eb; padding: 14px 18px; border-radius: 4px; margin-bottom: 20px;\">"
                + "      <p style=\"margin: 0; font-size: 13px; color: #1e40af; line-height: 1.5;\"><strong>Quy trình tiếp theo:</strong> Đội ngũ Tuyển dụng sẽ sàng lọc hồ sơ và liên hệ với bạn trong vòng 3-5 ngày làm việc nếu hồ sơ đáp ứng yêu cầu của vị trí.</p>"
                + "    </div>"
                + withdrawalSection
                + "    <p style=\"margin: 0; font-size: 14px; color: #475569;\">Chúc bạn có một trải nghiệm tuyển dụng tuyệt vời cùng IRMS!</p>"
                + "  </td></tr>"
                + "  <!-- Footer -->"
                + "  <tr><td style=\"background-color: #f8fafc; padding: 22px 30px; text-align: center; color: #94a3b8; font-size: 12px; border-top: 1px solid #e2e8f0;\">"
                + "    <p style=\"margin: 0 0 5px;\">Thư xác nhận được gửi tự động từ Hệ thống Quản lý Tuyển dụng IRMS.</p>"
                + "    <p style=\"margin: 0;\">&copy; 2026 IRMS Recruitment Platform. Mọi quyền được bảo lưu.</p>"
                + "  </td></tr>"
                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    public static String buildCandidateWithdrawalNotificationEmail(CandidateWithdrawal withdrawal) {
        String candidateName = escape(withdrawal.getFullName());
        String candidateEmail = escape(withdrawal.getEmail());
        String trackingCode = escape(withdrawal.getTrackingCode());
        String position = escape(withdrawal.getRequisitionTitle() == null
                ? "Vị trí tuyển dụng" : withdrawal.getRequisitionTitle());
        String oldStatus = escape(withdrawal.getStatusBeforeWithdrawal());
        String recruiterName = escape(withdrawal.getRecruiterName() == null
                ? "Recruiter" : withdrawal.getRecruiterName());
        return "<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\"></head>"
                + "<body style=\"font-family:Arial,sans-serif;color:#334155;background:#f8fafc;padding:24px;\">"
                + "<div style=\"max-width:600px;margin:auto;background:#fff;border:1px solid #e2e8f0;border-radius:10px;padding:28px;\">"
                + "<h2 style=\"color:#0f172a;margin-top:0;\">Ứng viên đã rút hồ sơ</h2>"
                + "<p>Xin chào " + recruiterName + ",</p>"
                + "<p>Ứng viên xác nhận rút hồ sơ khỏi quy trình tuyển dụng. Hồ sơ đã được chuyển khỏi pipeline đang hoạt động.</p>"
                + "<table style=\"width:100%;border-collapse:collapse;\">"
                + "<tr><td style=\"padding:8px;border-bottom:1px solid #e2e8f0;\">Ứng viên</td><td style=\"padding:8px;border-bottom:1px solid #e2e8f0;\">" + candidateName + " (" + candidateEmail + ")</td></tr>"
                + "<tr><td style=\"padding:8px;border-bottom:1px solid #e2e8f0;\">Vị trí</td><td style=\"padding:8px;border-bottom:1px solid #e2e8f0;\">" + position + "</td></tr>"
                + "<tr><td style=\"padding:8px;border-bottom:1px solid #e2e8f0;\">Mã hồ sơ</td><td style=\"padding:8px;border-bottom:1px solid #e2e8f0;\">" + trackingCode + "</td></tr>"
                + "<tr><td style=\"padding:8px;\">Trạng thái</td><td style=\"padding:8px;\">" + oldStatus + " → WITHDRAWN</td></tr>"
                + "</table><p style=\"font-size:12px;color:#64748b;margin-top:22px;\">Thông báo tự động từ IRMS Careers.</p>"
                + "</div></body></html>";
    }

    private static String escape(String value) {
        return com.irms.util.SecurityUtil.escapeHtml(value == null ? "" : value);
    }
}

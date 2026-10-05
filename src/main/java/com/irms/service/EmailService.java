package com.irms.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.irms.config.AppConfig;
import com.irms.dao.PasswordResetDAO;
import com.irms.model.EmailOutboxItem;
import com.irms.util.EmailTemplateUtil;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ==============================================================================
 * DỊCH VỤ GỬI EMAIL THÔNG MINH - HỖ TRỢ SONG SONG MAILHOG & GMAIL SMTP
 * ==============================================================================
 * - Chế độ 'auto': Tự động chọn Gmail nếu có điền user & App Password;
 *                  ngược lại tự động chọn MailHog (localhost:1025).
 * - Chế độ 'mailhog': Luôn dùng MailHog cục bộ (xem tại http://localhost:8025).
 * - Chế độ 'gmail': Gửi thư thật qua Gmail SMTP (smtp.gmail.com:587).
 * - Tự động quét và xử lý hàng đợi email_outbox bất đồng bộ không làm chậm web.
 * ==============================================================================
 */
public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final PasswordResetDAO passwordResetDAO = new PasswordResetDAO();
    private static boolean isWorkerStarted = false;

    /**
     * Khởi động background worker quét hàng đợi email định kỳ mỗi 30 giây
     */
    public static synchronized void startScheduledWorker() {
        if (isWorkerStarted) return;
        isWorkerStarted = true;
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                processPendingEmails();
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "[EmailService] Lỗi quét outbox định kỳ", t);
            }
        }, 5, 30, TimeUnit.SECONDS);
        LOGGER.info("[EmailService] Đã kích hoạt Email Outbox Worker (chu kỳ quét 30s).");
    }

    /**
     * Kích hoạt xử lý hàng đợi email_outbox ngay lập tức trong luồng nền
     */
    public static void triggerOutboxProcessing() {
        scheduler.submit(() -> {
            try {
                processPendingEmails();
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "[EmailService] Lỗi khi xử lý hàng đợi email outbox", t);
            }
        });
    }

    /**
     * Quét và gửi các email đang ở trạng thái PENDING hoặc FAILED (retry < 3)
     */
    public static void processPendingEmails() {
        List<EmailOutboxItem> pendingList = passwordResetDAO.findPendingEmails(10);
        if (pendingList == null || pendingList.isEmpty()) {
            return;
        }

        LOGGER.info("[EmailService] Tìm thấy " + pendingList.size() + " thư trong hàng đợi cần gửi...");
        for (EmailOutboxItem item : pendingList) {
            try {
                String htmlBody = buildHtmlForTemplate(item.getTemplate(), item.getRecipient(), item.getPayload());
                boolean success = sendHtmlEmail(item.getRecipient(), item.getSubject(), htmlBody);
                if (success) {
                    passwordResetDAO.updateEmailStatus(item.getId(), "SENT", null);
                    LOGGER.info("[EmailService] Đã gửi thành công email ID: " + item.getId() + " đến: " + item.getRecipient());
                } else {
                    passwordResetDAO.updateEmailStatus(item.getId(), "FAILED", "Không thể kết nối hoặc gửi thư tới máy chủ SMTP.");
                }
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : e.toString();
                passwordResetDAO.updateEmailStatus(item.getId(), "FAILED", errorMsg);
                LOGGER.log(Level.SEVERE, "[EmailService] Lỗi gửi thư outbox ID: " + item.getId(), e);
            }
        }
    }

    /**
     * Render HTML theo mẫu dựa trên template name và JSON payload
     */
    private static String buildHtmlForTemplate(String template, String recipient, String payloadJson) {
        String fullName = "";
        String resetLink = "";
        String tempPass = "";

        if (payloadJson != null && !payloadJson.trim().isEmpty()) {
            try {
                JsonObject json = JsonParser.parseString(payloadJson).getAsJsonObject();
                if (json.has("fullName")) fullName = json.get("fullName").getAsString();
                if (json.has("resetLink")) resetLink = json.get("resetLink").getAsString();
                if (json.has("temporaryPassword")) tempPass = json.get("temporaryPassword").getAsString();
            } catch (Exception e) {
                LOGGER.warning("[EmailService] Lỗi parse payload JSON: " + e.getMessage());
            }
        }

        if ("RESET_PASSWORD".equalsIgnoreCase(template)) {
            return EmailTemplateUtil.buildResetPasswordEmail(fullName, resetLink);
        } else if ("ACCOUNT_ACTIVATION".equalsIgnoreCase(template)) {
            return EmailTemplateUtil.buildAccountActivationEmail(fullName, recipient, tempPass);
        }

        // Mặc định nếu không trùng template
        return "<p>" + payloadJson + "</p>";
    }

    /**
     * Gửi email HTML qua MailHog, Gmail SMTP hoặc in ra Console (chế độ Dev không cần Docker)
     */
    public static boolean sendHtmlEmail(String to, String subject, String htmlContent) {
        String provider = AppConfig.get("mail.provider", "auto").trim().toLowerCase();
        String smtpUser = AppConfig.get("mail.smtp.username", "").trim();
        String smtpPass = AppConfig.get("mail.smtp.password", "").trim();

        // 1. Chế độ CONSOLE / MOCK: In trực tiếp ra màn hình Terminal (không cần cài Docker hay Mail server)
        if ("console".equals(provider) || "mock".equals(provider)) {
            printConsoleEmail(to, subject, htmlContent);
            return true;
        }

        boolean useGmail = "gmail".equals(provider) || ("auto".equals(provider) && !smtpUser.isEmpty() && !smtpPass.isEmpty());
        final String cleanPass = smtpPass.replace(" ", "");

        Properties props = new Properties();
        Session session;
        String from = AppConfig.get("mail.from", "IRMS Recruitment Platform <noreply@company.local>");

        if (useGmail) {
            // Cấu hình gửi qua Gmail SMTP (TLS 587)
            String host = AppConfig.get("mail.smtp.host", "smtp.gmail.com");
            String port = AppConfig.get("mail.smtp.port", "587");
            if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)) {
                host = "smtp.gmail.com";
                port = "587";
            }
            if (from.contains("@company.local") && smtpUser.contains("@")) {
                from = "IRMS Recruitment Platform <" + smtpUser + ">";
            }

            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "10000");

            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpUser, cleanPass);
                }
            });
            LOGGER.info("[EmailService] Đang gửi thư qua Gmail SMTP (" + host + ":" + port + ") từ: " + from + " đến: " + to);
        } else {
            // Cấu hình gửi qua MailHog nội bộ (Port 1025)
            String host = AppConfig.get("mail.mailhog.host", "localhost");
            String port = AppConfig.get("mail.mailhog.port", "1025");

            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", port);
            props.put("mail.smtp.auth", "false");
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");

            session = Session.getInstance(props);
            LOGGER.info("[EmailService] Đang gửi thư qua MailHog (" + host + ":" + port + ") đến: " + to);
        }

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject, "UTF-8");
            message.setContent(htmlContent, "text/html; charset=UTF-8");

            Transport.send(message);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "[EmailService] Máy chủ SMTP chưa sẵn sàng (" + e.getMessage() + "). Tự động kích hoạt hiển thị nội dung trên Terminal Console dự phòng.");
            printConsoleEmail(to, subject, htmlContent);
            return true;
        }
    }

    /**
     * In nội dung email ra màn hình Terminal phục vụ kiểm thử nhanh
     */
    private static void printConsoleEmail(String to, String subject, String htmlContent) {
        System.out.println("\n" + "=".repeat(75));
        System.out.println("📧 [IRMS DEV EMAIL CONSOLE] - EMAIL MỚI ĐÃ ĐƯỢC TẠO RA");
        System.out.println("-".repeat(75));
        System.out.println("Gửi đến:  " + to);
        System.out.println("Tiêu đề:  " + subject);
        
        // Trích xuất link hoặc mật khẩu từ HTML để tiện copy
        if (htmlContent != null) {
            if (htmlContent.contains("/auth/reset-password?token=")) {
                int startIdx = htmlContent.indexOf("http://");
                if (startIdx < 0) startIdx = htmlContent.indexOf("https://");
                if (startIdx >= 0) {
                    int endIdx = htmlContent.indexOf("\"", startIdx);
                    if (endIdx > startIdx) {
                        String link = htmlContent.substring(startIdx, endIdx);
                        System.out.println("🔗 LIÊN KẾT ĐẶT LẠI MẬT KHẨU: " + link);
                    }
                }
            }
            if (htmlContent.contains("Mật khẩu tạm thời:")) {
                int start = htmlContent.indexOf("Temp@");
                if (start >= 0) {
                    int end = htmlContent.indexOf("<", start);
                    if (end > start) {
                        System.out.println("🔑 MẬT KHẨU TẠM THỜI: " + htmlContent.substring(start, end));
                    }
                }
            }
        }
        System.out.println("=".repeat(75) + "\n");
    }

    /**
     * Dừng background worker khi ứng dụng tắt
     */
    public static void shutdown() {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
            LOGGER.info("[EmailService] Đã dừng Email Outbox Worker.");
        }
    }
}

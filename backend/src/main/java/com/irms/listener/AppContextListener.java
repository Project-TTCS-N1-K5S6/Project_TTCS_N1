package com.irms.listener;

import com.irms.config.DBConnection;
import com.irms.service.EmailService;
import com.irms.service.JobExpirationService;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.logging.Logger;

/**
 * Quản lý vòng đời ứng dụng IRMS:
 * - Khởi động Email Outbox Worker và tác vụ gỡ tin quá hạn
 * - Dọn dẹp Worker và DB Connection Pool khi ứng dụng tắt
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(AppContextListener.class.getName());

    /**
     * Chức năng: Xử lý sự kiện khi ứng dụng Web bắt đầu khởi động, kích hoạt worker gửi email ngầm (Email Outbox Worker)
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("================================================================");
        LOGGER.info("🚀 HỆ THỐNG IRMS ĐANG KHỞI CHẠY - BẬT EMAIL OUTBOX VÀ TÁC VỤ GỠ TIN QUÁ HẠN");
        LOGGER.info("================================================================");
        EmailService.startScheduledWorker();
        JobExpirationService.startDailyExpirationTask();
    }

    /**
     * Chức năng: Dừng các tác vụ nền và giải phóng HikariCP connection pool khi ứng dụng tắt
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Đang tắt hệ thống IRMS: dọn dẹp Email Worker và DB Pool...");
        JobExpirationService.shutdown();
        EmailService.shutdown();
        DBConnection.closePool();
    }
}

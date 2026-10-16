package com.irms.listener;

import com.irms.config.DBConnection;
import com.irms.service.EmailService;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.logging.Logger;

/**
 * Quản lý vòng đời ứng dụng IRMS:
 * - Khởi động Email Outbox Worker khi ứng dụng khởi chạy
 * - Dọn dẹp tài nguyên (Worker, DB Connection Pool) khi ứng dụng tắt
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("================================================================");
        LOGGER.info("🚀 HỆ THỐNG IRMS ĐANG KHỞI CHẠY - BẬT DỊCH VỤ EMAIL OUTBOX");
        LOGGER.info("================================================================");
        EmailService.startScheduledWorker();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Đang tắt hệ thống IRMS: dọn dẹp Email Worker và DB Pool...");
        EmailService.shutdown();
        DBConnection.closePool();
    }
}

package com.irms.service;

import com.irms.dao.JobPostingDAO;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tác vụ tự động gỡ bài đăng khi đã quá hạn nhận hồ sơ.
 */
public final class JobExpirationService {

    private static final Logger LOGGER = Logger.getLogger(JobExpirationService.class.getName());
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static boolean started;

    private JobExpirationService() {}

    public static synchronized void startDailyExpirationTask() {
        if (started) return;
        started = true;
        scheduler.scheduleWithFixedDelay(JobExpirationService::unpublishExpiredPosts, 0, 1, TimeUnit.DAYS);
        LOGGER.info("[JobExpirationService] Đã khởi động tác vụ gỡ tin quá hạn hằng ngày.");
    }

    private static void unpublishExpiredPosts() {
        try {
            int count = new JobPostingDAO().unpublishExpiredPosts();
            if (count > 0) {
                LOGGER.info("[JobExpirationService] Đã tự động gỡ " + count + " tin tuyển dụng quá hạn.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "[JobExpirationService] Không thể tự động gỡ các tin tuyển dụng quá hạn.", e);
        }
    }

    public static synchronized void shutdown() {
        scheduler.shutdownNow();
    }
}

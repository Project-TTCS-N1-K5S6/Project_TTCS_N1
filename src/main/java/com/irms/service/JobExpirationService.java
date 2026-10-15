package com.irms.service;

import com.irms.dao.JobPostingDAO;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * [KN-265]: Tác vụ định kỳ hằng ngày tự động ẩn tin quá hạn nhận hồ sơ.
 * - Tự động quét và gỡ các tin tuyển dụng có hạn nộp trong quá khứ sang UNPUBLISHED.
 * - Chạy ngay một lần khi server khởi động để bắt kịp các tin hết hạn trong thời gian tắt server.
 * - Sau đó lặp lại định kỳ vào lúc 00:01 mỗi ngày.
 */
public final class JobExpirationService {

    private static final Logger LOGGER = Logger.getLogger(JobExpirationService.class.getName());
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static boolean started;

    private JobExpirationService() {}

    /**
     * Khởi động tác vụ quét tự động tin quá hạn định kỳ hằng ngày
     */
    public static synchronized void startDailyExpirationTask() {
        if (started) return;
        started = true;
        // 1. Quét ngay khi khởi động
        scheduler.execute(JobExpirationService::unpublishExpiredPosts);

        // 2. Lên lịch chạy cố định vào 00:01 mỗi ngày
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.toLocalDate().plusDays(1).atTime(LocalTime.of(0, 1));
        long initialDelay = Duration.between(now, nextRun).getSeconds();
        scheduler.scheduleAtFixedRate(JobExpirationService::unpublishExpiredPosts,
                initialDelay, TimeUnit.DAYS.toSeconds(1), TimeUnit.SECONDS);
        LOGGER.info("[JobExpirationService] Đã khởi động tác vụ tự động ẩn tin quá hạn nhận hồ sơ hằng ngày (KN-265).");
    }

    /**
     * Thực hiện gỡ các tin tuyển dụng đã quá hạn nhận hồ sơ
     */
    private static void unpublishExpiredPosts() {
        try {
            int count = new JobPostingDAO().unpublishExpiredPosts();
            if (count > 0) {
                LOGGER.info("[JobExpirationService] Đã tự động gỡ " + count + " tin tuyển dụng quá hạn nhận hồ sơ.");
            }
        } catch (SQLException | RuntimeException e) {
            LOGGER.log(Level.SEVERE, "[JobExpirationService] Lỗi khi tự động gỡ các tin tuyển dụng quá hạn.", e);
        }
    }

    /**
     * Dọn dẹp scheduler khi tắt server
     */
    public static synchronized void shutdown() {
        scheduler.shutdownNow();
    }
}

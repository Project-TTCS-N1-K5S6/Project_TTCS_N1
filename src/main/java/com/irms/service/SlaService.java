package com.irms.service;

import com.google.gson.JsonObject;
import com.irms.config.AppConfig;
import com.irms.dao.CandidateDAO;
import com.irms.dao.PasswordResetDAO;
import com.irms.dao.SlaDAO;
import com.irms.dao.UserDAO;
import com.irms.model.Candidate;
import com.irms.model.InAppNotification;
import com.irms.model.SlaConfig;
import com.irms.model.SlaReminderLog;
import com.irms.model.User;
import com.irms.util.SecurityUtil;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ==============================================================================
 * DỊCH VỤ QUẢN LÝ SLA, TÁC VỤ ĐỊNH KỲ QUÉT QUÁ HẠN & NHẮC VIỆC (KN-362 -> KN-368)
 * ==============================================================================
 */
public class SlaService {
    private static final Logger LOGGER = Logger.getLogger(SlaService.class.getName());

    private final SlaDAO slaDAO = new SlaDAO();
    private final CandidateDAO candidateDAO = new CandidateDAO();
    private final PasswordResetDAO passwordResetDAO = new PasswordResetDAO();
    private final NotificationService notificationService = new NotificationService();
    private final UserDAO userDAO = new UserDAO();

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static boolean isSchedulerStarted = false;
    private static Timestamp lastScanTimestamp = null;

    /**
     * [KN-362]: Khởi động tác vụ định kỳ quét hồ sơ quá hạn SLA mỗi ngày một lần (24 giờ)
     */
    public static synchronized void startDailyScheduler() {
        if (isSchedulerStarted) return;
        isSchedulerStarted = true;

        // Chạy lần đầu sau 15 giây khởi động, sau đó lặp lại mỗi 24 giờ (1 ngày)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LOGGER.info("[SlaService] Bắt đầu tác vụ định kỳ tự động quét hồ sơ quá hạn SLA...");
                SlaService service = new SlaService();
                service.scanAndProcessOverdueCandidates();
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "[SlaService] Lỗi khi chạy tác vụ định kỳ quét SLA", t);
            }
        }, 15, 24 * 60 * 60, TimeUnit.SECONDS);

        LOGGER.info("[SlaService] Đã kích hoạt tác vụ định kỳ quét hồ sơ quá hạn SLA (chu kỳ 24h).");
    }

    public static void shutdown() {
        try {
            scheduler.shutdownNow();
            LOGGER.info("[SlaService] Đã tắt tác vụ định kỳ quét SLA an toàn.");
        } catch (Exception ignored) {}
    }

    /**
     * [KN-364]: Lấy toàn bộ danh sách cấu hình ngưỡng SLA
     */
    public List<SlaConfig> getAllConfigs() {
        return slaDAO.findAllConfigs();
    }

    /**
     * [KN-364]: Lấy cấu hình ngưỡng SLA của 1 giai đoạn
     */
    public SlaConfig getConfigByStage(String stageCode) {
        if (stageCode == null || stageCode.trim().isEmpty()) return null;
        return slaDAO.findConfigByStage(stageCode.trim());
    }

    public static void validateConfig(SlaConfig config) {
        if (config == null || config.getId() == null) {
            throw new IllegalArgumentException("Dữ liệu cấu hình không hợp lệ.");
        }
        if (config.getSlaDays() < 1 || config.getSlaDays() > 90) {
            throw new IllegalArgumentException("Ngưỡng SLA phải từ 1 đến 90 ngày.");
        }
        if (config.getWarningThresholdDays() < 0 || config.getWarningThresholdDays() >= config.getSlaDays()) {
            throw new IllegalArgumentException("Ngưỡng cảnh báo cận hạn phải nhỏ hơn ngưỡng SLA.");
        }
    }

    /**
     * [KN-364]: Cập nhật cấu hình ngưỡng SLA cho một giai đoạn tuyển dụng
     */
    public boolean updateConfig(SlaConfig config, String updatedBy) {
        validateConfig(config);
        config.setUpdatedBy(updatedBy);
        return slaDAO.updateConfig(config);
    }

    /**
     * [KN-362 & KN-363 & KN-365 & KN-367 & KN-368]:
     * Thực hiện quét toàn bộ hồ sơ ứng viên đang hoạt động đối chiếu ngưỡng SLA.
     * Trả về kết quả tóm tắt quá trình quét.
     */
    public ScanSummary scanAndProcessOverdueCandidates() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        lastScanTimestamp = now;
        Date today = new Date(System.currentTimeMillis());

        ScanSummary summary = new ScanSummary();

        // 1. Tải bản đồ cấu hình SLA đang kích hoạt
        List<SlaConfig> configs = slaDAO.findAllConfigs();
        Map<String, SlaConfig> activeConfigMap = new HashMap<>();
        for (SlaConfig cfg : configs) {
            if (cfg.isActive()) {
                activeConfigMap.put(cfg.getStageCode(), cfg);
            }
        }

        // 2. Lấy danh sách hồ sơ cần quét
        List<Candidate> candidates = slaDAO.findActiveCandidatesForScan();
        summary.setTotalScanned(candidates.size());

        for (Candidate c : candidates) {
            SlaConfig cfg = activeConfigMap.get(c.getStatus());
            if (cfg == null) {
                // Giai đoạn không cấu hình SLA hoặc cấu hình đang tắt
                continue;
            }

            int daysInStage = c.getDaysInCurrentStage();
            // Nếu ngày tính toán nhỏ hơn hoặc bằng 0, tính thủ công từ stageEnteredAt hoặc createdAt
            if (daysInStage <= 0) {
                Timestamp refTime = c.getStageEnteredAt() != null ? c.getStageEnteredAt() : c.getCreatedAt();
                if (refTime != null) {
                    long diffMillis = now.getTime() - refTime.getTime();
                    daysInStage = (int) (diffMillis / (1000L * 60 * 60 * 24));
                }
            }

            // Kiểm tra xem có vượt quá ngưỡng SLA của giai đoạn hay không
            if (daysInStage >= cfg.getSlaDays()) {
                summary.incrementOverdueCount();

                // [KN-363]: Gắn cờ hồ sơ quá hạn và lưu thời điểm phát hiện SLA
                candidateDAO.markOverdue(c.getId(), now);
                c.setSlaOverdue(true);
                c.setSlaOverdueDetectedAt(now);

                // [KN-365 & KN-367 & KN-368]: Gửi nhắc việc có kiểm tra chống gửi trùng
                boolean reminderSent = sendOverdueReminder(c, cfg, daysInStage, today);
                if (reminderSent) {
                    summary.incrementRemindersSent();
                } else {
                    summary.incrementRemindersSkipped();
                }
            }
        }

        LOGGER.info(String.format("[SlaService] Hoàn thành quét SLA: Quét %d hồ sơ, phát hiện %d quá hạn, gửi %d nhắc việc, bỏ qua trùng %d.",
                summary.getTotalScanned(), summary.getOverdueCount(), summary.getRemindersSent(), summary.getRemindersSkipped()));
        return summary;
    }

    /**
     * [KN-365 & KN-367 & KN-368]:
     * Gửi nhắc việc cho Recruiter phụ trách khi hồ sơ quá hạn SLA,
     * đồng thời chống gửi trùng trong cùng ngày (KN-367) và ghi log lịch sử (KN-368).
     */
    public boolean sendOverdueReminder(Candidate candidate, SlaConfig config, int daysInStage, Date sentDate) {
        if (sentDate == null) {
            sentDate = new Date(System.currentTimeMillis());
        }

        // [KN-367]: Kiểm tra chống gửi nhắc trùng trong cùng ngày cho cùng hồ sơ
        boolean alreadySent = slaDAO.hasReminderBeenSentToday(candidate.getId(), config.getStageCode(), sentDate);
        if (alreadySent) {
            LOGGER.info("[SlaService] Bỏ qua gửi nhắc cho ứng viên " + candidate.getFullName() + " (ID: " + candidate.getId() + "): Đã gửi trong ngày " + sentDate);
            // Ghi log trạng thái SKIPPED_DUPLICATE
            SlaReminderLog skipLog = new SlaReminderLog();
            skipLog.setId(SecurityUtil.generateUUID());
            skipLog.setCandidateId(candidate.getId());
            skipLog.setCandidateName(candidate.getFullName());
            skipLog.setRequisitionId(candidate.getRequisitionId());
            skipLog.setRequisitionTitle(candidate.getRequisitionTitle());
            skipLog.setStageCode(config.getStageCode());
            skipLog.setSlaDaysThreshold(config.getSlaDays());
            skipLog.setDaysInStage(daysInStage);
            skipLog.setRecruiterId(candidate.getRecruiterId());
            skipLog.setRecruiterName(candidate.getRecruiterName());
            skipLog.setRecruiterEmail(candidate.getRecruiterEmail());
            skipLog.setChannel("IN_APP_AND_EMAIL");
            skipLog.setStatus("SKIPPED_DUPLICATE");
            skipLog.setMessage("Bỏ qua do đã gửi nhắc việc cho hồ sơ này trong ngày hôm nay.");
            skipLog.setSentDate(sentDate);
            slaDAO.insertReminderLog(skipLog);
            return false;
        }

        // [KN-365]: Xác định người nhận nhắc việc (Recruiter phụ trách vị trí)
        String recruiterId = candidate.getRecruiterId();
        String recruiterName = candidate.getRecruiterName();
        String recruiterEmail = candidate.getRecruiterEmail();

        // Nếu vị trí chưa gán Recruiter, tìm kiếm một Quản trị viên hoặc HR Manager để nhận cảnh báo
        if (recruiterId == null || recruiterId.trim().isEmpty()) {
            try {
                List<User> admins = userDAO.findAll(null, null, "ACTIVE", null, 0, 5);
                if (admins != null && !admins.isEmpty()) {
                    User primaryAdmin = admins.get(0);
                    recruiterId = primaryAdmin.getId();
                    recruiterName = primaryAdmin.getFullName();
                    recruiterEmail = primaryAdmin.getEmail();
                }
            } catch (Exception ignored) {}
        }

        int overdueDays = Math.max(0, daysInStage - config.getSlaDays());
        String stageLabel = config.getStageName() != null ? config.getStageName() : config.getStageCode();

        // 1. Tạo thông báo in-app (Chuông thông báo nội bộ)
        if (recruiterId != null && !recruiterId.trim().isEmpty()) {
            InAppNotification notif = new InAppNotification();
            notif.setId(SecurityUtil.generateUUID());
            notif.setUserId(recruiterId);
            notif.setTitle("⚠️ Quá hạn SLA: Hồ sơ " + candidate.getFullName());
            notif.setContent("Hồ sơ ứng viên " + candidate.getFullName() +
                    " tại giai đoạn '" + stageLabel + "' đã ở quá hạn " + daysInStage +
                    " ngày (Ngưỡng SLA: " + config.getSlaDays() + " ngày). Vui lòng xử lý ngay.");
            notif.setType("SLA_OVERDUE");
            notif.setReferenceId(candidate.getId());
            notificationService.createNotification(notif);
        }

        // 2. Đưa email nhắc việc vào hàng đợi outbox (nếu có email hợp lệ)
        if (recruiterEmail != null && recruiterEmail.contains("@")) {
            JsonObject payload = new JsonObject();
            payload.addProperty("recruiterName", recruiterName != null ? recruiterName : "Chuyên viên tuyển dụng");
            payload.addProperty("candidateName", candidate.getFullName());
            payload.addProperty("candidateEmail", candidate.getEmail() != null ? candidate.getEmail() : "");
            payload.addProperty("candidatePhone", candidate.getPhone() != null ? candidate.getPhone() : "");
            payload.addProperty("positionTitle", candidate.getRequisitionTitle() != null ? candidate.getRequisitionTitle() : "Vị trí tuyển dụng");
            payload.addProperty("stageName", stageLabel);
            payload.addProperty("daysInStage", daysInStage);
            payload.addProperty("slaThreshold", config.getSlaDays());
            payload.addProperty("actionUrl", AppConfig.get("app.baseUrl", "http://localhost:8088") + "/candidates");

            String subject = "[IRMS - Nhắc việc SLA] Cảnh báo hồ sơ '" + candidate.getFullName() + "' quá hạn " + overdueDays + " ngày";
            passwordResetDAO.queueEmail(recruiterEmail, subject, "SLA_REMINDER", payload.toString());
            EmailService.triggerOutboxProcessing();
        }

        // 3. [KN-368]: Ghi log lịch sử cảnh báo SLA và lần gửi nhắc
        SlaReminderLog log = new SlaReminderLog();
        log.setId(SecurityUtil.generateUUID());
        log.setCandidateId(candidate.getId());
        log.setCandidateName(candidate.getFullName());
        log.setRequisitionId(candidate.getRequisitionId());
        log.setRequisitionTitle(candidate.getRequisitionTitle());
        log.setStageCode(config.getStageCode());
        log.setSlaDaysThreshold(config.getSlaDays());
        log.setDaysInStage(daysInStage);
        log.setRecruiterId(recruiterId);
        log.setRecruiterName(recruiterName);
        log.setRecruiterEmail(recruiterEmail);
        log.setChannel("IN_APP_AND_EMAIL");
        log.setStatus("SENT");
        log.setMessage("Đã gửi cảnh báo in-app và email nhắc việc cho Recruiter.");
        log.setSentDate(sentDate);
        slaDAO.insertReminderLog(log);

        LOGGER.info("[SlaService] Đã gửi nhắc việc quá hạn SLA cho hồ sơ: " + candidate.getFullName() + " tới recruiter: " + recruiterEmail);
        return true;
    }

    /**
     * [KN-368]: Tra cứu danh sách lịch sử nhắc việc có phân trang
     */
    public List<SlaReminderLog> getReminderLogs(String search, Date date, int page, int pageSize) {
        int p = Math.max(1, page);
        int size = Math.max(1, pageSize);
        int offset = (p - 1) * size;
        return slaDAO.findReminderLogs(search, date, offset, size);
    }

    public int countReminderLogs(String search, Date date) {
        return slaDAO.countReminderLogs(search, date);
    }

    /**
     * Lấy các chỉ số thống kê SLA tổng quan
     */
    public Map<String, Object> getOverviewStats() {
        Map<String, Object> stats = new HashMap<>();
        List<SlaConfig> configs = slaDAO.findAllConfigs();
        int activeConfigCount = 0;
        for (SlaConfig c : configs) {
            if (c.isActive()) activeConfigCount++;
        }

        stats.put("totalConfigs", configs.size());
        stats.put("activeConfigs", activeConfigCount);
        stats.put("overdueCandidatesCount", slaDAO.countOverdueCandidates());
        stats.put("remindersSentToday", slaDAO.countRemindersSentToday());
        stats.put("lastScanTimestamp", lastScanTimestamp);
        return stats;
    }

    /**
     * Lớp dữ liệu tóm tắt kết quả quét
     */
    public static class ScanSummary {
        private int totalScanned = 0;
        private int overdueCount = 0;
        private int remindersSent = 0;
        private int remindersSkipped = 0;

        public int getTotalScanned() { return totalScanned; }
        public void setTotalScanned(int totalScanned) { this.totalScanned = totalScanned; }

        public int getOverdueCount() { return overdueCount; }
        public void incrementOverdueCount() { this.overdueCount++; }

        public int getRemindersSent() { return remindersSent; }
        public void incrementRemindersSent() { this.remindersSent++; }

        public int getRemindersSkipped() { return remindersSkipped; }
        public void incrementRemindersSkipped() { this.remindersSkipped++; }
    }
}

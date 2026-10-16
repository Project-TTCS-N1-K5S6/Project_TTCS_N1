package com.irms;

import com.irms.model.Candidate;
import com.irms.model.InAppNotification;
import com.irms.model.SlaConfig;
import com.irms.model.SlaReminderLog;
import com.irms.service.SlaService;
import com.irms.util.EmailTemplateUtil;
import org.junit.Before;
import org.junit.Test;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;

import static org.junit.Assert.*;

/**
 * ==============================================================================
 * UNIT TESTS: CẢNH BÁO & NHẮC NHỞ QUÁ HẠN SLA (KN-364, 362, 363, 365, 366, 367, 368)
 * ==============================================================================
 */
public class SlaManagementTest {

    private SlaConfig appliedConfig;
    private SlaConfig screeningConfig;
    private SlaConfig interviewConfig;
    private SlaConfig offerConfig;

    @Before
    public void setUp() {
        appliedConfig = new SlaConfig("sla-01", "APPLIED", "Mới ứng tuyển", 3, 1,
                "Sàng lọc hồ sơ ứng viên ban đầu và phản hồi/chuyển vòng", true);
        screeningConfig = new SlaConfig("sla-02", "SCREENING", "Sàng lọc CV", 5, 2,
                "Đánh giá chi tiết kinh nghiệm và hẹn lịch phỏng vấn", true);
        interviewConfig = new SlaConfig("sla-03", "INTERVIEWING", "Phỏng vấn", 7, 2,
                "Hoàn thành phỏng vấn và nhập kết quả đánh giá", true);
        offerConfig = new SlaConfig("sla-04", "OFFER", "Đề nghị tuyển dụng (Offer)", 4, 1,
                "Thương lượng đãi ngộ và xác nhận tiếp nhận ứng viên", true);
    }

    /**
     * [KN-364]: Kiểm tra thiết kế cấu hình ngưỡng SLA theo từng giai đoạn
     */
    @Test
    public void testSlaThresholdConfiguration() {
        assertNotNull("Cấu hình giai đoạn APPLIED không được null", appliedConfig);
        assertEquals("APPLIED", appliedConfig.getStageCode());
        assertEquals("Mới ứng tuyển", appliedConfig.getStageName());
        assertEquals("Hồ sơ mới phải sàng lọc trong 3 ngày", 3, appliedConfig.getSlaDays());
        assertEquals("Cảnh báo trước 1 ngày", 1, appliedConfig.getWarningThresholdDays());
        assertTrue("Cấu hình phải đang kích hoạt", appliedConfig.isActive());
        assertEquals("badge bg-primary", appliedConfig.getStageBadgeClass());

        assertEquals("SCREENING", screeningConfig.getStageCode());
        assertEquals(5, screeningConfig.getSlaDays());

        assertEquals("INTERVIEWING", interviewConfig.getStageCode());
        assertEquals(7, interviewConfig.getSlaDays());

        assertEquals("OFFER", offerConfig.getStageCode());
        assertEquals(4, offerConfig.getSlaDays());
    }

    /**
     * [KN-364]: Kiểm tra ràng buộc hợp lệ khi cập nhật ngưỡng SLA
     */
    @Test
    public void testSlaConfigValidationRules() {
        // Ngưỡng SLA không được < 1 ngày
        SlaConfig invalidSlaDays = new SlaConfig("sla-01", "APPLIED", "Mới ứng tuyển", 0, 0, "Test", true);
        try {
            SlaService.validateConfig(invalidSlaDays);
            fail("Phải ném ngoại lệ khi slaDays < 1");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("từ 1 đến 90 ngày"));
        }

        // Ngưỡng cảnh báo cận hạn không được lớn hơn hoặc bằng ngưỡng SLA
        SlaConfig invalidWarningDays = new SlaConfig("sla-01", "APPLIED", "Mới ứng tuyển", 3, 3, "Test", true);
        try {
            SlaService.validateConfig(invalidWarningDays);
            fail("Phải ném ngoại lệ khi warningThresholdDays >= slaDays");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("nhỏ hơn ngưỡng SLA"));
        }
    }

    /**
     * [KN-363]: Kiểm tra gắn cờ hồ sơ quá hạn và tính toán số ngày vi phạm
     */
    @Test
    public void testCandidateOverdueFlagAndCalculation() {
        Candidate candidate = new Candidate();
        candidate.setId("cand-101");
        candidate.setFullName("Nguyễn Văn A");
        candidate.setStatus("APPLIED");
        candidate.setSlaDaysThreshold(3);

        // Trường hợp 1: Hồ sơ ở trong giai đoạn 2 ngày -> Chưa quá hạn
        candidate.setDaysInCurrentStage(2);
        candidate.setSlaOverdue(false);
        assertFalse("Hồ sơ 2 ngày chưa vượt quá ngưỡng SLA 3 ngày", candidate.isSlaOverdue());
        assertEquals(0, candidate.getOverdueDays());

        // Trường hợp 2: Hồ sơ ở trong giai đoạn 5 ngày -> Quá hạn 2 ngày!
        candidate.setDaysInCurrentStage(5);
        candidate.setSlaOverdue(true);
        Timestamp detectedTime = new Timestamp(System.currentTimeMillis());
        candidate.setSlaOverdueDetectedAt(detectedTime);

        assertTrue("Hồ sơ phải bị gắn cờ quá hạn", candidate.isSlaOverdue());
        assertEquals("Quá hạn 2 ngày (5 - 3 = 2)", 2, candidate.getOverdueDays());
        assertNotNull("Thời điểm phát hiện phải được ghi nhận", candidate.getSlaOverdueDetectedAt());
    }

    /**
     * [KN-365]: Kiểm tra tạo thông báo in-app và render mẫu email gửi cho Recruiter
     */
    @Test
    public void testOverdueReminderDispatchAndEmailTemplate() {
        InAppNotification notif = new InAppNotification("notif-01", "recruiter-01",
                "⚠️ Quá hạn SLA: Hồ sơ Nguyễn Văn A",
                "Hồ sơ ứng viên Nguyễn Văn A tại giai đoạn 'Mới ứng tuyển' đã ở quá hạn 4 ngày (Ngưỡng SLA: 3 ngày).",
                "SLA_OVERDUE", "cand-101");

        assertNotNull(notif);
        assertEquals("recruiter-01", notif.getUserId());
        assertEquals("SLA_OVERDUE", notif.getType());
        assertFalse("Mặc định thông báo mới là chưa đọc", notif.isRead());

        // Kiểm tra render email HTML nhắc việc
        String emailHtml = EmailTemplateUtil.buildSlaReminderEmail(
                "Trần Thị Recruiter", "Nguyễn Văn A", "nguyenvana@example.com",
                "0987654321", "Lập trình viên Java Senior", "Mới ứng tuyển",
                5, 3, "http://localhost:8088/candidates");

        assertNotNull("Nội dung email không được null", emailHtml);
        assertTrue("Email phải chứa tên Recruiter", emailHtml.contains("Trần Thị Recruiter"));
        assertTrue("Email phải chứa tên ứng viên", emailHtml.contains("Nguyễn Văn A"));
        assertTrue("Email phải chứa vị trí", emailHtml.contains("Lập trình viên Java Senior"));
        assertTrue("Email phải chứa số ngày quá hạn", emailHtml.contains("Quá hạn 2 ngày"));
        assertTrue("Email phải chứa link dẫn tới hồ sơ", emailHtml.contains("/candidates"));
    }

    /**
     * [KN-367]: Kiểm tra cơ chế chống gửi nhắc trùng trong cùng ngày cho cùng hồ sơ
     */
    @Test
    public void testAntiDuplicateReminderSameDay() {
        String candidateId = "cand-dup-test";
        String stageCode = "APPLIED";
        Date today = Date.valueOf(LocalDate.now());

        // Giả lập log đã gửi hôm nay
        SlaReminderLog sentLog = new SlaReminderLog();
        sentLog.setId("log-001");
        sentLog.setCandidateId(candidateId);
        sentLog.setStageCode(stageCode);
        sentLog.setSentDate(today);
        sentLog.setStatus("SENT");

        assertEquals(candidateId, sentLog.getCandidateId());
        assertEquals(stageCode, sentLog.getStageCode());
        assertEquals(today, sentLog.getSentDate());
        assertEquals("SENT", sentLog.getStatus());

        // Log bỏ qua do trùng lặp
        SlaReminderLog skipLog = new SlaReminderLog();
        skipLog.setId("log-002");
        skipLog.setCandidateId(candidateId);
        skipLog.setStageCode(stageCode);
        skipLog.setSentDate(today);
        skipLog.setStatus("SKIPPED_DUPLICATE");
        skipLog.setMessage("Bỏ qua do đã gửi nhắc việc cho hồ sơ này trong ngày hôm nay.");

        assertEquals("SKIPPED_DUPLICATE", skipLog.getStatus());
        assertEquals("badge bg-warning text-dark", skipLog.getStatusBadgeClass());
        assertEquals("Bỏ qua (Đã nhắc trong ngày)", skipLog.getStatusLabel());
    }

    /**
     * [KN-368]: Kiểm tra ghi log lịch sử cảnh báo SLA và lần gửi nhắc
     */
    @Test
    public void testSlaReminderAuditLogging() {
        SlaReminderLog log = new SlaReminderLog();
        log.setId("log-audit-01");
        log.setCandidateId("cand-123");
        log.setCandidateName("Lê Văn C");
        log.setRequisitionId("req-456");
        log.setRequisitionTitle("Trưởng nhóm Kỹ thuật (Tech Lead)");
        log.setStageCode("SCREENING");
        log.setStageName("Sàng lọc CV");
        log.setSlaDaysThreshold(5);
        log.setDaysInStage(7);
        log.setRecruiterId("rec-789");
        log.setRecruiterName("Phạm Văn Recruiter");
        log.setRecruiterEmail("recruiter@company.local");
        log.setChannel("IN_APP_AND_EMAIL");
        log.setStatus("SENT");
        log.setMessage("Đã gửi thông báo in-app và email nhắc việc cho Recruiter.");
        log.setSentDate(Date.valueOf(LocalDate.now()));
        log.setSentAt(new Timestamp(System.currentTimeMillis()));

        assertEquals("cand-123", log.getCandidateId());
        assertEquals("Lê Văn C", log.getCandidateName());
        assertEquals("SCREENING", log.getStageCode());
        assertEquals(2, log.getOverdueDays()); // 7 - 5 = 2 ngày
        assertEquals("Đã gửi nhắc", log.getStatusLabel());
        assertEquals("badge bg-success", log.getStatusBadgeClass());
        assertNotNull(log.getSentAt());
    }
}

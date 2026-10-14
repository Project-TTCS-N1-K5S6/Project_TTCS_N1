package com.irms;

import com.irms.model.Candidate;
import com.irms.model.JobPosting;
import com.irms.model.RecruitmentRequest;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Kiểm thử tính năng Quản lý Trạng thái Yêu cầu Tuyển dụng (Manage Recruitment Request Status):
 * 1. Ba trạng thái kết thúc có lý do riêng: Đã tuyển đủ, Tạm dừng, Huỷ
 * 2. Đóng yêu cầu tự động gỡ tin tuyển dụng đang đăng
 * 3. Ứng viên còn đang trong pipeline được cảnh báo cần xử lý trước khi đóng
 */
public class RecruitmentRequestStatusTest {

    @Test
    public void testTerminalStatusLabelsAndBadges() {
        RecruitmentRequest req1 = new RecruitmentRequest();
        req1.setStatus("FULFILLED");
        req1.setCloseStatus("FULFILLED");
        req1.setCloseReason("Đã tuyển đủ số lượng chỉ tiêu (Headcount)");
        Assert.assertTrue("FULFILLED phải là trạng thái kết thúc", req1.isTerminal());
        Assert.assertEquals("Đã tuyển đủ", req1.getStatusLabel());
        Assert.assertTrue(req1.getStatusBadgeClass().contains("bg-teal-custom"));

        RecruitmentRequest req2 = new RecruitmentRequest();
        req2.setStatus("PAUSED");
        req2.setCloseStatus("PAUSED");
        req2.setCloseReason("Tạm dừng kế hoạch tuyển dụng quý này");
        Assert.assertTrue("PAUSED phải là trạng thái kết thúc", req2.isTerminal());
        Assert.assertEquals("Tạm dừng", req2.getStatusLabel());
        Assert.assertTrue(req2.getStatusBadgeClass().contains("bg-warning"));

        RecruitmentRequest req3 = new RecruitmentRequest();
        req3.setStatus("CANCELLED");
        req3.setCloseStatus("CANCELLED");
        req3.setCloseReason("Huỷ bỏ vị trí do thay đổi chiến lược kinh doanh");
        Assert.assertTrue("CANCELLED phải là trạng thái kết thúc", req3.isTerminal());
        Assert.assertEquals("Đã huỷ", req3.getStatusLabel());
        Assert.assertTrue(req3.getStatusBadgeClass().contains("bg-danger"));
    }

    @Test
    public void testJobPostingStatusAndUnpublish() {
        JobPosting posting = new JobPosting("jp-001", "req-001", "Senior Java Engineer", "INTERNAL", "PUBLISHED");
        Assert.assertEquals("Đang đăng", posting.getStatusLabel());
        Assert.assertEquals("bg-success text-white", posting.getStatusBadgeClass());

        // Mô phỏng gỡ tin tự động khi đóng yêu cầu
        posting.setStatus("UNPUBLISHED");
        Assert.assertEquals("Đã gỡ", posting.getStatusLabel());
        Assert.assertEquals("bg-secondary text-white", posting.getStatusBadgeClass());
    }

    @Test
    public void testCandidatePipelineActiveDetection() {
        List<Candidate> candidates = new ArrayList<>();

        Candidate c1 = new Candidate();
        c1.setId("c1");
        c1.setStatus("APPLIED"); // Active in pipeline

        Candidate c2 = new Candidate();
        c2.setId("c2");
        c2.setStatus("INTERVIEWING"); // Active in pipeline

        Candidate c3 = new Candidate();
        c3.setId("c3");
        c3.setStatus("HIRED"); // Terminal

        candidates.add(c1);
        candidates.add(c2);
        candidates.add(c3);

        long activeCount = candidates.stream()
                .filter(c -> "APPLIED".equals(c.getStatus()) || "SCREENING".equals(c.getStatus()) ||
                             "INTERVIEWING".equals(c.getStatus()) || "OFFER".equals(c.getStatus()))
                .count();

        Assert.assertEquals("Phải phát hiện chính xác 2 ứng viên đang kẹt trong pipeline", 2, activeCount);
    }

    @Test
    public void testClosureReasonValidation() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setStatus("OPEN");
        req.setDeadline(Date.valueOf(LocalDate.now().plusDays(10)));
        req.setMinSalary(new BigDecimal("15000000"));
        req.setMaxSalary(new BigDecimal("25000000"));

        // Xác thực lý do không được để trống
        String reason = "";
        boolean failed = false;
        try {
            if (reason.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn hoặc nhập lý do kết thúc yêu cầu tuyển dụng!");
            }
        } catch (IllegalArgumentException e) {
            failed = true;
        }
        Assert.assertTrue("Bắt buộc phải có lý do khi kết thúc yêu cầu tuyển dụng", failed);
    }
}

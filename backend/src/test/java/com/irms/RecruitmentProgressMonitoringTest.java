package com.irms;

import com.irms.model.RecruitmentRequest;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * Kiểm thử tính năng Giám sát & Cảnh báo Tiến độ Tuyển dụng [KN-109 / KN-118]:
 * 1. Lọc theo trạng thái, phòng ban, recruiter phụ trách, khoảng thời gian.
 * 2. Mỗi dòng hiển thị số ngày mở và số ngày còn lại tới ngày cần người.
 * 3. Yêu cầu quá ngày cần người được đánh dấu nổi bật.
 */
public class RecruitmentProgressMonitoringTest {

    @Test
    public void testDaysOpenCalculation() {
        RecruitmentRequest req = new RecruitmentRequest();
        
        // 1. Yêu cầu tạo cách đây 12 ngày, chưa đóng -> số ngày mở = 12
        LocalDate twelveDaysAgo = LocalDate.now().minusDays(12);
        req.setCreatedAt(Timestamp.valueOf(twelveDaysAgo.atStartOfDay()));
        req.setStatus("OPEN");
        Assert.assertEquals("Số ngày mở phải là 12", 12L, req.getDaysOpen());

        // 2. Yêu cầu đã đóng (kết thúc) cách đây 4 ngày -> số ngày mở tính đến closedAt = 8 ngày
        LocalDate fourDaysAgo = LocalDate.now().minusDays(4);
        req.setClosedAt(Timestamp.valueOf(fourDaysAgo.atStartOfDay()));
        req.setStatus("FULFILLED");
        Assert.assertEquals("Yêu cầu đã đóng tính số ngày mở đến closedAt", 8L, req.getDaysOpen());

        // 3. Trường hợp createdAt null -> trả về 0
        req.setCreatedAt(null);
        Assert.assertEquals("createdAt null trả về 0 ngày", 0L, req.getDaysOpen());
    }

    @Test
    public void testDaysRemainingFutureDeadline() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setStatus("OPEN");

        // Ngày cần người còn 15 ngày nữa
        LocalDate futureDate = LocalDate.now().plusDays(15);
        req.setDeadline(Date.valueOf(futureDate));

        Assert.assertNotNull(req.getDaysRemaining());
        Assert.assertEquals("Còn 15 ngày tới hạn", Long.valueOf(15), req.getDaysRemaining());
        Assert.assertFalse("Chưa quá hạn", req.isOverdue());
        Assert.assertFalse("Còn 15 ngày (>7) không thuộc diện sắp đến hạn gấp", req.isDueSoon());
    }

    @Test
    public void testDaysRemainingDueSoonBoundary() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setStatus("OPEN");

        // Còn đúng 5 ngày
        LocalDate fiveDaysAhead = LocalDate.now().plusDays(5);
        req.setDeadline(Date.valueOf(fiveDaysAhead));

        Assert.assertEquals(Long.valueOf(5), req.getDaysRemaining());
        Assert.assertFalse("Chưa quá hạn", req.isOverdue());
        Assert.assertTrue("Còn 5 ngày (<=7) phải cảnh báo sắp đến hạn (dueSoon)", req.isDueSoon());

        // Hạn chót chính là hôm nay (0 ngày)
        req.setDeadline(Date.valueOf(LocalDate.now()));
        Assert.assertEquals(Long.valueOf(0), req.getDaysRemaining());
        Assert.assertFalse("Hôm nay là ngày đến hạn, chưa bị tính là đã quá hạn", req.isOverdue());
        Assert.assertTrue("Hôm nay đến hạn phải đánh dấu dueSoon", req.isDueSoon());
    }

    @Test
    public void testOverdueDetectionAndHighlight() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setStatus("OPEN");

        // Quá hạn 4 ngày
        LocalDate pastDate = LocalDate.now().minusDays(4);
        req.setDeadline(Date.valueOf(pastDate));

        Assert.assertEquals(Long.valueOf(-4), req.getDaysRemaining());
        Assert.assertTrue("Vị trí OPEN quá ngày cần người phải được đánh dấu OVERDUE", req.isOverdue());
        Assert.assertEquals("Số ngày quá hạn là 4", Long.valueOf(4), req.getDaysOverdue());
    }

    @Test
    public void testOverdueIgnoredForTerminalStatuses() {
        // Nếu yêu cầu đã đóng (Đã tuyển đủ, Tạm dừng, Đã huỷ), dù ngày deadline trong quá khứ cũng không báo Overdue
        RecruitmentRequest reqFulfilled = new RecruitmentRequest();
        reqFulfilled.setStatus("FULFILLED");
        reqFulfilled.setDeadline(Date.valueOf(LocalDate.now().minusDays(10)));
        Assert.assertFalse("Yêu cầu đã tuyển đủ không cảnh báo quá hạn", reqFulfilled.isOverdue());

        RecruitmentRequest reqPaused = new RecruitmentRequest();
        reqPaused.setStatus("PAUSED");
        reqPaused.setDeadline(Date.valueOf(LocalDate.now().minusDays(10)));
        Assert.assertFalse("Yêu cầu tạm dừng không cảnh báo quá hạn", reqPaused.isOverdue());

        RecruitmentRequest reqCancelled = new RecruitmentRequest();
        reqCancelled.setStatus("CANCELLED");
        reqCancelled.setDeadline(Date.valueOf(LocalDate.now().minusDays(10)));
        Assert.assertFalse("Yêu cầu đã huỷ không cảnh báo quá hạn", reqCancelled.isOverdue());
    }
}

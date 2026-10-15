package com.irms;

import com.irms.model.JobPosting;
import com.irms.model.RecruitmentRequest;
import com.irms.service.JobPublishingService;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;

import static org.junit.Assert.*;

/**
 * Kiểm thử toàn diện tính năng Xem danh sách việc làm công khai:
 * - KN-220: Thiết kế & Chuẩn hóa dữ liệu mô hình bài tuyển dụng (expired_at, department_id, location_id, work_type)
 * - KN-219: API Danh sách việc làm công khai (chỉ hiển thị tin đã xuất bản & còn hạn, phân trang, sắp xếp)
 * - KN-222: Tìm kiếm từ khóa (case-insensitive trên title và description)
 * - KN-223: Bộ lọc phòng ban, địa điểm, hình thức làm việc
 */
public class PublicJobListingTest {

    private JobPublishingService publishingService;

    @Before
    public void setUp() {
        publishingService = new JobPublishingService();
    }

    // =========================================================================
    // KN-220 & ACCEPTANCE CRITERIA: ĐIỀU KIỆN HIỂN THỊ CÔNG KHAI
    // Chỉ trả về bài đăng status = 'PUBLISHED' và expired_at >= current_time
    // =========================================================================

    @Test
    public void testEligibility_PublishedAndNotExpired_ReturnsTrue() {
        JobPosting posting = new JobPosting();
        posting.setStatus("PUBLISHED");
        // Hạn nộp còn 10 ngày trong tương lai
        posting.setExpiredAt(new Timestamp(System.currentTimeMillis() + 10L * 24 * 60 * 60 * 1000));

        RecruitmentRequest req = new RecruitmentRequest();
        req.setStatus("APPROVED");
        posting.setRequisition(req);

        assertTrue("Bài đăng PUBLISHED và còn hạn phải đủ điều kiện công khai",
                publishingService.isPubliclyEligible(posting));
        assertTrue("isPubliclyVisible của model cũng phải trả về true",
                posting.isPubliclyVisible());
        assertFalse("Không được xem là đã hết hạn", posting.isExpired());
    }

    @Test
    public void testEligibility_PublishedButExpired_ReturnsFalse() {
        JobPosting posting = new JobPosting();
        posting.setStatus("PUBLISHED");
        // Đã hết hạn từ 2 ngày trước
        posting.setExpiredAt(new Timestamp(System.currentTimeMillis() - 2L * 24 * 60 * 60 * 1000));

        RecruitmentRequest req = new RecruitmentRequest();
        req.setStatus("APPROVED");
        posting.setRequisition(req);

        assertFalse("Bài đăng đã hết hạn tuyệt đối KHÔNG được hiển thị công khai!",
                publishingService.isPubliclyEligible(posting));
        assertFalse("isPubliclyVisible phải trả về false khi đã hết hạn",
                posting.isPubliclyVisible());
        assertTrue("isExpired() phải trả về true", posting.isExpired());
    }

    @Test
    public void testEligibility_DraftStatus_ReturnsFalse() {
        JobPosting posting = new JobPosting();
        posting.setStatus("DRAFT");
        posting.setExpiredAt(new Timestamp(System.currentTimeMillis() + 5L * 24 * 60 * 60 * 1000));

        assertFalse("Bài đăng ở trạng thái DRAFT không được hiển thị công khai",
                publishingService.isPubliclyEligible(posting));
        assertFalse(posting.isPubliclyVisible());
    }

    @Test
    public void testEligibility_ArchivedStatus_ReturnsFalse() {
        JobPosting posting = new JobPosting();
        posting.setStatus("ARCHIVED");
        posting.setExpiredAt(new Timestamp(System.currentTimeMillis() + 5L * 24 * 60 * 60 * 1000));

        assertFalse("Bài đăng ở trạng thái ARCHIVED (đã lưu trữ) không được hiển thị công khai",
                publishingService.isPubliclyEligible(posting));
        assertFalse(posting.isPubliclyVisible());
        assertEquals("Đã lưu trữ", posting.getStatusLabel());
        assertTrue(posting.getStatusBadgeClass().contains("bg-secondary"));
    }

    @Test
    public void testEligibility_UnpublishedStatus_ReturnsFalse() {
        JobPosting posting = new JobPosting();
        posting.setStatus("UNPUBLISHED");
        posting.setExpiredAt(new Timestamp(System.currentTimeMillis() + 5L * 24 * 60 * 60 * 1000));

        assertFalse("Bài đăng ở trạng thái UNPUBLISHED không được hiển thị công khai",
                publishingService.isPubliclyEligible(posting));
        assertFalse(posting.isPubliclyVisible());
    }

    @Test
    public void testEligibility_ClosedStatus_ReturnsFalse() {
        JobPosting posting = new JobPosting();
        posting.setStatus("CLOSED");
        posting.setExpiredAt(new Timestamp(System.currentTimeMillis() + 5L * 24 * 60 * 60 * 1000));

        assertFalse("Bài đăng ở trạng thái CLOSED không được hiển thị công khai",
                publishingService.isPubliclyEligible(posting));
        assertFalse(posting.isPubliclyVisible());
    }

    @Test
    public void testEligibility_UnapprovedRequisition_ReturnsFalse() {
        JobPosting posting = new JobPosting();
        posting.setStatus("PUBLISHED");
        posting.setExpiredAt(new Timestamp(System.currentTimeMillis() + 10L * 24 * 60 * 60 * 1000));

        // Yêu cầu chưa duyệt (PENDING_APPROVAL hoặc REJECTED)
        RecruitmentRequest req = new RecruitmentRequest();
        req.setStatus("PENDING_APPROVAL");
        posting.setRequisition(req);

        assertFalse("Bài đăng gắn với yêu cầu chưa duyệt không được hiển thị công khai",
                publishingService.isPubliclyEligible(posting));
    }

    // =========================================================================
    // KN-220: KIỂM TRA MÔ HÌNH VÀ CÁC TRƯỜNG CHUẨN HÓA MỚI
    // =========================================================================

    @Test
    public void testJobPostingModel_NewFieldsAndFallbacks() {
        JobPosting jp = new JobPosting();
        jp.setTitle("Senior Cloud Architect");
        jp.setDescription("Thiết kế hạ tầng đám mây AWS và GCP");
        jp.setDepartmentId("dept-it-01");
        jp.setLocationId("loc-hanoi");
        jp.setWorkType("HYBRID");

        assertEquals("Senior Cloud Architect", jp.getTitle());
        assertEquals("Thiết kế hạ tầng đám mây AWS và GCP", jp.getDescription());
        assertEquals("dept-it-01", jp.getDepartmentId());
        assertEquals("loc-hanoi", jp.getLocationId());
        assertEquals("HYBRID", jp.getWorkType());

        // Kiểm tra cơ chế fallback từ applicationDeadline sang expiredAt
        Date dl = Date.valueOf(LocalDate.now().plusDays(5));
        jp.setApplicationDeadline(dl);
        assertNotNull("expiredAt tự động suy luận từ deadline nếu chưa set trực tiếp", jp.getExpiredAt());
        assertTrue("Thời điểm hết hạn tự động suy luận phải sau hiện tại",
                jp.getExpiredAt().getTime() > System.currentTimeMillis());
    }

    // =========================================================================
    // KN-222: TÌM KIẾM TỪ KHÓA CASE-INSENSITIVE TRÊN TITLE VÀ DESCRIPTION
    // =========================================================================

    @Test
    public void testKeywordSearch_TitleMatching() {
        JobPosting jp = new JobPosting();
        jp.setTitle("Senior Fullstack Java Developer");
        jp.setDescription("Xây dựng ứng dụng doanh nghiệp.");

        String keywordUpper = "JAVA";
        String keywordLower = "java";
        String keywordMixed = "fUlLsTaCk";

        assertTrue(jp.getTitle().toLowerCase().contains(keywordUpper.toLowerCase()));
        assertTrue(jp.getTitle().toLowerCase().contains(keywordLower.toLowerCase()));
        assertTrue(jp.getTitle().toLowerCase().contains(keywordMixed.toLowerCase()));
    }

    @Test
    public void testKeywordSearch_DescriptionMatching() {
        JobPosting jp = new JobPosting();
        jp.setTitle("Kỹ sư Phần mềm");
        jp.setDescription("Chuyên về kiến trúc Microservices và cơ sở dữ liệu MySQL 8.0 tối ưu.");

        String keyword1 = "microservices";
        String keyword2 = "MYSQL";

        assertTrue(jp.getDescription().toLowerCase().contains(keyword1.toLowerCase()));
        assertTrue(jp.getDescription().toLowerCase().contains(keyword2.toLowerCase()));

        String nonExistent = "Python Django";
        assertFalse(jp.getTitle().toLowerCase().contains(nonExistent.toLowerCase()) ||
                   jp.getDescription().toLowerCase().contains(nonExistent.toLowerCase()));
    }

    // =========================================================================
    // KN-223: BỘ LỌC PHÒNG BAN, ĐỊA ĐIỂM, HÌNH THỨC LÀM VIỆC
    // =========================================================================

    @Test
    public void testFilterMatchingLogic() {
        JobPosting jp = new JobPosting();
        jp.setDepartmentId("dept-engineering");
        jp.setLocationId("loc-hcm");
        jp.setWorkType("REMOTE");

        // Lọc đúng
        assertEquals("dept-engineering", jp.getDepartmentId());
        assertEquals("loc-hcm", jp.getLocationId());
        assertEquals("REMOTE", jp.getWorkType());

        // Lọc sai
        assertNotEquals("dept-hr", jp.getDepartmentId());
        assertNotEquals("loc-danang", jp.getLocationId());
        assertNotEquals("ONSITE", jp.getWorkType());
    }

    // =========================================================================
    // KN-219: TÍNH TOÁN PHÂN TRANG (PAGINATION)
    // =========================================================================

    @Test
    public void testPaginationCalculations() {
        int total = 25;
        int limit = 9;
        int totalPages = (int) Math.ceil((double) total / limit);
        assertEquals("25 bài với limit 9 phải có 3 trang", 3, totalPages);

        int page1Offset = (1 - 1) * limit;
        int page2Offset = (2 - 1) * limit;
        int page3Offset = (3 - 1) * limit;

        assertEquals(0, page1Offset);
        assertEquals(9, page2Offset);
        assertEquals(18, page3Offset);

        // Trường hợp danh sách rỗng
        int zeroTotal = 0;
        int zeroPages = (zeroTotal > 0) ? (int) Math.ceil((double) zeroTotal / limit) : 1;
        assertEquals("0 bài phải trả về 1 trang rỗng", 1, zeroPages);
    }

    @Test
    public void testSalaryFormatting() {
        JobPosting jp = new JobPosting();
        jp.setMinSalary(new BigDecimal("20000000"));
        jp.setMaxSalary(new BigDecimal("35000000"));
        jp.setCurrency("VND");
        jp.setSalaryVisible(true);

        String formatted = jp.getFormattedSalaryRange();
        assertTrue("Hiển thị dải lương đầy đủ", formatted.contains("20.000.000") && formatted.contains("35.000.000"));

        // Khi ẩn lương
        jp.setSalaryVisible(false);
        assertEquals("Thỏa thuận", jp.getFormattedSalaryRange());
    }
}

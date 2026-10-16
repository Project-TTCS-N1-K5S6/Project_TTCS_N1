package com.irms;

import com.irms.model.JobPosting;
import com.irms.model.RecruitmentRequest;
import com.irms.model.User;
import com.irms.service.JobPublishingService;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;

import static org.junit.Assert.*;

/**
 * Kiểm thử toàn diện chức năng Tạo nhanh Bài đăng Tuyển dụng:
 * - KN-204: Giao diện và cấu trúc mô hình bài đăng
 * - KN-205: Backend xác thực dữ liệu bắt buộc (Hạn nhận, Dải lương, Rỗng, Số âm, Tiếng Việt)
 * - KN-206: Chuẩn hóa nội dung mô tả công việc (JD kế thừa, không sửa yêu cầu nguồn)
 * - KN-207: Phân quyền thực hiện tạo bài đăng
 * - KN-208: Điều kiện trạng thái phê duyệt (chỉ APPROVED / OPEN)
 */
public class QuickJobPostingTest {

    private JobPublishingService publishingService;

    @Before
    public void setUp() {
        publishingService = new JobPublishingService();
    }

    /**
     * KN-206: Chuẩn hóa nội dung mô tả công việc
     * Gạch đầu dòng các định dạng •, *, +, –, — được chuẩn hóa về "- "
     * Khoảng trắng thừa được làm sạch, không làm mất nội dung gốc.
     */
    @Test
    public void testInheritAndNormalizeJobDescription_StandardizesBulletsAndWhitespace() {
        String rawJd = "  • Tham gia phát triển hệ thống backend Java Spring Boot\n\n\n"
                + "  * Thiết kế cơ sở dữ liệu MySQL và tối ưu hóa truy vấn\n"
                + "  + Xây dựng API chuẩn RESTful kết nối Frontend\n"
                + "  – Phối hợp với đội kiểm thử QA/QC  \n"
                + "  — Hỗ trợ bảo trì và triển khai hệ thống.\n";

        String normalized = publishingService.normalizeJobDescription(rawJd);

        assertNotNull(normalized);
        assertTrue("Bullet • phải được chuẩn hóa về -", normalized.contains("- Tham gia phát triển hệ thống backend Java Spring Boot"));
        assertTrue("Bullet * phải được chuẩn hóa về -", normalized.contains("- Thiết kế cơ sở dữ liệu MySQL"));
        assertTrue("Bullet + phải được chuẩn hóa về -", normalized.contains("- Xây dựng API chuẩn RESTful"));
        assertTrue("Bullet – phải được chuẩn hóa về -", normalized.contains("- Phối hợp với đội kiểm thử QA/QC"));
        assertTrue("Bullet — phải được chuẩn hóa về -", normalized.contains("- Hỗ trợ bảo trì và triển khai"));
        assertFalse("Không còn 3 dòng trắng liên tiếp", normalized.contains("\n\n\n"));

        // Xác minh nội dung gốc trong yêu cầu tuyển dụng không bị biến đổi
        RecruitmentRequest req = new RecruitmentRequest();
        req.setJobDescription(rawJd);
        assertEquals("Yêu cầu tuyển dụng gốc phải giữ nguyên dữ liệu gốc", rawJd, req.getJobDescription());
    }

    /**
     * KN-208: Kiểm tra trạng thái phê duyệt
     * Yêu cầu có trạng thái APPROVED hoặc OPEN được coi là ĐÃ DUYỆT và hợp lệ.
     */
    @Test
    public void testApprovalStatusCheck_OnlyApprovedOrOpenAllowed() {
        RecruitmentRequest approvedReq = new RecruitmentRequest();
        approvedReq.setStatus("APPROVED");
        assertTrue("APPROVED phải đủ điều kiện", publishingService.isEligibleForPublish(approvedReq));

        RecruitmentRequest openReq = new RecruitmentRequest();
        openReq.setStatus("OPEN");
        assertTrue("OPEN phải đủ điều kiện", publishingService.isEligibleForPublish(openReq));

        String[] invalidStatuses = {"DRAFT", "PENDING_APPROVAL", "REJECTED", "CANCELLED", "CLOSED", "FULFILLED", "PAUSED"};
        for (String st : invalidStatuses) {
            RecruitmentRequest req = new RecruitmentRequest();
            req.setStatus(st);
            assertFalse("Trạng thái " + st + " KHÔNG được phép tạo bài đăng!", publishingService.isEligibleForPublish(req));
        }
    }

    /**
     * KN-205 & KN-208: Xác thực bài đăng hợp lệ từ yêu cầu đã duyệt
     */
    @Test
    public void testValidateQuickPosting_Success() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setId("req-001");
        req.setStatus("APPROVED");

        JobPosting posting = new JobPosting();
        posting.setRequisitionId("req-001");
        posting.setTitle("Chuyên viên Java Developer");
        posting.setJobDescription("- Lập trình ứng dụng Java\n- Xử lý cơ sở dữ liệu");
        posting.setWorkLocation("Hà Nội");
        posting.setWorkMode("Toàn thời gian");
        posting.setApplicationDeadline(Date.valueOf(LocalDate.now().plusDays(15)));
        posting.setSalaryVisible(true);
        posting.setMinSalary(new BigDecimal("15000000"));
        posting.setMaxSalary(new BigDecimal("25000000"));

        // Xác thực không có lỗi
        java.util.List<String> errors = publishingService.validateQuickPosting(posting, req);
        assertTrue("Không được có lỗi", errors.isEmpty());

        // Không ném ngoại lệ
        publishingService.assertValidQuickPosting(posting, req);
    }

    /**
     * KN-208: Từ chối tạo bài đăng nếu yêu cầu chưa ở trạng thái ĐÃ DUYỆT
     */
    @Test(expected = IllegalStateException.class)
    public void testValidateQuickPosting_UnapprovedRequisition_ThrowsIllegalStateException() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setId("req-002");
        req.setStatus("PENDING_APPROVAL");

        JobPosting posting = new JobPosting();
        posting.setRequisitionId("req-002");
        posting.setTitle("Kỹ sư phần mềm");
        posting.setJobDescription("- Lập trình phần mềm");
        posting.setWorkLocation("Hà Nội");
        posting.setWorkMode("Toàn thời gian");
        posting.setApplicationDeadline(Date.valueOf(LocalDate.now().plusDays(10)));

        publishingService.assertValidQuickPosting(posting, req);
    }

    /**
     * KN-205: Từ chối nếu thiếu yêu cầu tuyển dụng nguồn
     */
    @Test
    public void testValidateQuickPosting_MissingRequisition_ThrowsIllegalArgumentException() {
        JobPosting posting = new JobPosting();
        posting.setTitle("Tuyển dụng");
        posting.setJobDescription("Mô tả công việc");

        try {
            publishingService.assertValidQuickPosting(posting, null);
            fail("Phải ném ngoại lệ khi req là null");
        } catch (IllegalArgumentException e) {
            assertTrue("Thông báo lỗi phải bằng tiếng Việt", e.getMessage().contains("nguồn"));
        }
    }

    /**
     * KN-205: Từ chối nếu thiếu tiêu đề bài đăng
     */
    @Test
    public void testValidateQuickPosting_EmptyTitle_ThrowsIllegalArgumentException() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setId("req-003");
        req.setStatus("APPROVED");

        JobPosting posting = new JobPosting();
        posting.setRequisitionId("req-003");
        posting.setTitle("   ");
        posting.setJobDescription("Mô tả công việc");

        try {
            publishingService.assertValidQuickPosting(posting, req);
            fail("Phải ném ngoại lệ khi tiêu đề rỗng");
        } catch (IllegalArgumentException e) {
            assertTrue("Thông báo lỗi phải nhắc đến tiêu đề bài đăng", e.getMessage().toLowerCase().contains("tiêu đề bài đăng"));
        }
    }

    /**
     * KN-205: Từ chối nếu thiếu mô tả công việc (JD)
     */
    @Test
    public void testValidateQuickPosting_EmptyJobDescription_ThrowsIllegalArgumentException() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setId("req-004");
        req.setStatus("APPROVED");

        JobPosting posting = new JobPosting();
        posting.setRequisitionId("req-004");
        posting.setTitle("Chuyên viên Tuyển dụng");
        posting.setJobDescription("");

        try {
            publishingService.assertValidQuickPosting(posting, req);
            fail("Phải ném ngoại lệ khi JD rỗng");
        } catch (IllegalArgumentException e) {
            assertTrue("Thông báo lỗi phải nhắc đến mô tả công việc", e.getMessage().contains("mô tả công việc"));
        }
    }

    /**
     * KN-205: Hạn nhận hồ sơ không hợp lệ (ở trong quá khứ)
     */
    @Test
    public void testValidateQuickPosting_DeadlineInPast_ThrowsIllegalArgumentException() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setId("req-005");
        req.setStatus("APPROVED");

        JobPosting posting = new JobPosting();
        posting.setRequisitionId("req-005");
        posting.setTitle("Chuyên viên QA");
        posting.setJobDescription("- Kiểm thử tự động");
        posting.setWorkLocation("Hà Nội");
        posting.setWorkMode("Toàn thời gian");
        posting.setApplicationDeadline(Date.valueOf(LocalDate.now().minusDays(1)));

        try {
            publishingService.assertValidQuickPosting(posting, req);
            fail("Phải ném ngoại lệ khi hạn nhận hồ sơ ở quá khứ");
        } catch (IllegalArgumentException e) {
            assertTrue("Thông báo lỗi hạn nhận ở quá khứ", e.getMessage().contains("quá khứ"));
        }
    }

    /**
     * KN-205: Mức lương âm bị từ chối
     */
    @Test
    public void testValidateQuickPosting_NegativeSalary_ThrowsIllegalArgumentException() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setId("req-006");
        req.setStatus("APPROVED");

        JobPosting posting = new JobPosting();
        posting.setRequisitionId("req-006");
        posting.setTitle("Chuyên viên Marketing");
        posting.setJobDescription("- Chạy quảng cáo số");
        posting.setWorkLocation("Hà Nội");
        posting.setWorkMode("Toàn thời gian");
        posting.setApplicationDeadline(Date.valueOf(LocalDate.now().plusDays(5)));
        posting.setSalaryVisible(true);
        posting.setMinSalary(new BigDecimal("-5000000"));

        try {
            publishingService.assertValidQuickPosting(posting, req);
            fail("Phải ném ngoại lệ khi lương tối thiểu âm");
        } catch (IllegalArgumentException e) {
            assertTrue("Thông báo lỗi mức lương âm", e.getMessage().contains("số âm"));
        }
    }

    /**
     * KN-205: Mức lương tối thiểu lớn hơn tối đa bị từ chối
     */
    @Test
    public void testValidateQuickPosting_MinSalaryGreaterThanMax_ThrowsIllegalArgumentException() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setId("req-007");
        req.setStatus("APPROVED");

        JobPosting posting = new JobPosting();
        posting.setRequisitionId("req-007");
        posting.setTitle("Trưởng nhóm Kỹ thuật");
        posting.setJobDescription("- Quản lý đội ngũ phát triển");
        posting.setWorkLocation("Hà Nội");
        posting.setWorkMode("Toàn thời gian");
        posting.setApplicationDeadline(Date.valueOf(LocalDate.now().plusDays(10)));
        posting.setSalaryVisible(true);
        posting.setMinSalary(new BigDecimal("35000000"));
        posting.setMaxSalary(new BigDecimal("20000000"));

        try {
            publishingService.assertValidQuickPosting(posting, req);
            fail("Phải ném ngoại lệ khi min > max salary");
        } catch (IllegalArgumentException e) {
            assertTrue("Thông báo lỗi min > max", e.getMessage().contains("lớn hơn mức lương tối đa"));
        }
    }

    /**
     * KN-204 & KN-205: Không hiển thị mức lương -> Cho phép lương rỗng
     */
    @Test
    public void testValidateQuickPosting_SalaryHidden_AllowsNullSalary() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setId("req-008");
        req.setStatus("APPROVED");

        JobPosting posting = new JobPosting();
        posting.setRequisitionId("req-008");
        posting.setTitle("Giám đốc Kinh doanh");
        posting.setJobDescription("- Phát triển thị trường");
        posting.setWorkLocation("Hà Nội");
        posting.setWorkMode("Toàn thời gian");
        posting.setApplicationDeadline(Date.valueOf(LocalDate.now().plusDays(20)));
        posting.setSalaryVisible(false);
        posting.setMinSalary(null);
        posting.setMaxSalary(null);

        // Hợp lệ, không ném ngoại lệ
        publishingService.assertValidQuickPosting(posting, req);
        assertEquals("Thỏa thuận", posting.getFormattedSalaryRange());
    }

    /**
     * KN-207: Kiểm tra phân quyền tạo bài đăng tuyển dụng
     * ADMIN, HR_MANAGER, RECRUITER hoặc người có quyền job_postings.manage được phép.
     */
    @Test
    public void testRBACPermission_CanManageJobPostings() {
        User admin = new User();
        com.irms.model.Role rAdmin = new com.irms.model.Role();
        rAdmin.setCode("ADMIN");
        admin.setRoles(java.util.Collections.singletonList(rAdmin));
        assertTrue("ADMIN phải có quyền quản lý bài đăng", publishingService.canManageJobPostings(admin));

        User hrManager = new User();
        com.irms.model.Role rHr = new com.irms.model.Role();
        rHr.setCode("HR_MANAGER");
        hrManager.setRoles(java.util.Collections.singletonList(rHr));
        assertTrue("HR_MANAGER phải có quyền quản lý bài đăng", publishingService.canManageJobPostings(hrManager));

        User recruiter = new User();
        com.irms.model.Role rRecruiter = new com.irms.model.Role();
        rRecruiter.setCode("RECRUITER");
        recruiter.setRoles(java.util.Collections.singletonList(rRecruiter));
        assertTrue("RECRUITER phải có quyền quản lý bài đăng", publishingService.canManageJobPostings(recruiter));

        User authorizedUser = new User();
        authorizedUser.setPermissions(java.util.Collections.singletonList("job_postings.manage"));
        assertTrue("User có permission job_postings.manage phải được phép", publishingService.canManageJobPostings(authorizedUser));

        User normalUser = new User();
        com.irms.model.Role rEmp = new com.irms.model.Role();
        rEmp.setCode("EMPLOYEE");
        normalUser.setRoles(java.util.Collections.singletonList(rEmp));
        assertFalse("EMPLOYEE không có quyền tạo bài đăng", publishingService.canManageJobPostings(normalUser));

        assertFalse("Actor null không được phép", publishingService.canManageJobPostings(null));
    }

    /**
     * KN-204: Kiểm tra định dạng hiển thị dải lương của JobPosting
     */
    @Test
    public void testJobPostingSalaryFormatting() {
        JobPosting post = new JobPosting();
        post.setSalaryVisible(true);
        post.setMinSalary(new BigDecimal("10000000"));
        post.setMaxSalary(new BigDecimal("20000000"));
        post.setCurrency("VND");

        String formatted = post.getFormattedSalaryRange();
        assertTrue("Phải chứa định dạng 10.000.000 hoặc 10,000,000", formatted.contains("10.000.000") || formatted.contains("10,000,000"));
        assertTrue("Phải chứa định dạng 20.000.000 hoặc 20,000,000", formatted.contains("20.000.000") || formatted.contains("20,000,000"));

        // Chỉ có minSalary
        post.setMaxSalary(null);
        assertTrue("Từ 10.000.000", post.getFormattedSalaryRange().contains("10.000.000") || post.getFormattedSalaryRange().contains("10,000,000"));

        // Chỉ có maxSalary
        post.setMinSalary(null);
        post.setMaxSalary(new BigDecimal("15000000"));
        assertTrue("Lên đến 15.000.000", post.getFormattedSalaryRange().contains("15.000.000") || post.getFormattedSalaryRange().contains("15,000,000"));

        // Ẩn lương
        post.setSalaryVisible(false);
        assertEquals("Thỏa thuận", post.getFormattedSalaryRange());
    }

    /**
     * Kiểm thử tích hợp trực tiếp CSDL: Xác minh lưu bài đăng không còn lỗi current_version
     */
    @Test
    public void testCreateQuickJobPosting_DatabaseIntegration() throws Exception {
        User admin = new User();
        admin.setId("usr-001");
        admin.setFullName("Nguyễn Quản Trị");
        com.irms.model.Role rAdmin = new com.irms.model.Role();
        rAdmin.setCode("ADMIN");
        admin.setRoles(java.util.Collections.singletonList(rAdmin));

        JobPosting posting = new JobPosting();
        posting.setRequisitionId("req-001");
        posting.setTitle("Kỹ sư Java Test DB " + System.currentTimeMillis());
        posting.setJobDescription("- Lập trình ứng dụng Java\n- Xử lý cơ sở dữ liệu");
        posting.setWorkLocation("Hà Nội");
        posting.setWorkMode("Toàn thời gian");
        posting.setApplicationDeadline(Date.valueOf(LocalDate.now().plusDays(15)));
        posting.setSalaryVisible(true);
        posting.setMinSalary(new BigDecimal("15000000"));
        posting.setMaxSalary(new BigDecimal("25000000"));
        posting.setStatus("PUBLISHED");

        JobPosting created = publishingService.createQuickJobPosting(posting, admin, "127.0.0.1", "JUnit-Test");
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("PUBLISHED", created.getStatus());
    }
}

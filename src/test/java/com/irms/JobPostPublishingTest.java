package com.irms;

import com.google.gson.Gson;
import com.irms.model.JobPostPublication;
import com.irms.model.JobPosting;
import com.irms.model.RecruitmentRequest;
import com.irms.service.JobPublishingService;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit Test cho Phân hệ Quản lý Xuất bản bài đăng (Ticket KN-120)
 * Kiểm tra 3 tiêu chí nghiệp vụ chính:
 * 1. "Tin phải duyệt trước khi show công khai"
 * 2. "Xem trước đúng như giao diện khai báo trước khi xuất bản"
 * 3. "Ghi lại bản xuất và thời điểm"
 */
public class JobPostPublishingTest {

    private JobPublishingService publishingService;
    private Gson gson;

    @Before
    public void setUp() {
        publishingService = new JobPublishingService();
        gson = new Gson();
    }

    /**
     * Tiêu chí 1: Tin phải duyệt trước khi show công khai
     * Khi tin chưa duyệt (DRAFT, PENDING_APPROVAL, REJECTED), hệ thống phải từ chối xuất bản.
     */
    @Test
    public void test1_CannotPublishUnapprovedRequisition_Draft() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setCode("REQ-2026-001");
        req.setTitle("Tuyển dụng Senior Java Developer");
        req.setPositionTitle("Senior Java Developer");
        req.setStatus("DRAFT"); // Bản nháp

        assertFalse("Tin ở trạng thái DRAFT không được phép xuất bản!", publishingService.isEligibleForPublish(req));

        try {
            publishingService.validateCanPublish(req);
            fail("Phải ném ngoại lệ IllegalStateException khi cố xuất bản tin DRAFT!");
        } catch (IllegalStateException e) {
            assertTrue("Thông báo lỗi phải nhấn mạnh 'Tin phải duyệt trước khi show công khai'",
                    e.getMessage().contains("Tin phải duyệt trước khi show công khai"));
        }
    }

    @Test
    public void test1_CannotPublishUnapprovedRequisition_PendingApproval() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setCode("REQ-2026-002");
        req.setTitle("Tuyển dụng Kế toán trưởng");
        req.setPositionTitle("Kế toán trưởng");
        req.setStatus("PENDING_APPROVAL"); // Chờ phê duyệt

        assertFalse("Tin ở trạng thái PENDING_APPROVAL không được phép xuất bản!", publishingService.isEligibleForPublish(req));

        try {
            publishingService.validateCanPublish(req);
            fail("Phải ném ngoại lệ khi cố xuất bản tin đang chờ duyệt!");
        } catch (IllegalStateException e) {
            assertTrue("Thông báo lỗi phải chứa thông điệp quy định KN-120",
                    e.getMessage().contains("Tin phải duyệt trước khi show công khai"));
        }
    }

    @Test
    public void test1_CannotPublishUnapprovedRequisition_Rejected() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setCode("REQ-2026-003");
        req.setTitle("Tuyển dụng Chuyên viên Pháp chế");
        req.setPositionTitle("Chuyên viên Pháp chế");
        req.setStatus("REJECTED"); // Từ chối

        assertFalse("Tin bị từ chối không được phép xuất bản!", publishingService.isEligibleForPublish(req));

        try {
            publishingService.validateCanPublish(req);
            fail("Phải ném ngoại lệ khi cố xuất bản tin bị từ chối!");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Tin phải duyệt trước khi show công khai"));
        }
    }

    /**
     * Tiêu chí 1: Tin đã duyệt được phép xuất bản
     * Khi tin ở trạng thái APPROVED hoặc OPEN, hệ thống cho phép xuất bản.
     */
    @Test
    public void test1_CanPublishApprovedOrOpenRequisitions() {
        RecruitmentRequest reqApproved = new RecruitmentRequest();
        reqApproved.setCode("REQ-2026-004");
        reqApproved.setTitle("Tuyển dụng Product Manager");
        reqApproved.setPositionTitle("Product Manager");
        reqApproved.setStatus("APPROVED"); // Đã duyệt

        assertTrue("Tin đã duyệt (APPROVED) phải đủ điều kiện xuất bản!", publishingService.isEligibleForPublish(reqApproved));
        // Không ném ngoại lệ
        publishingService.validateCanPublish(reqApproved);

        RecruitmentRequest reqOpen = new RecruitmentRequest();
        reqOpen.setCode("REQ-2026-005");
        reqOpen.setTitle("Tuyển dụng DevOps Engineer");
        reqOpen.setPositionTitle("DevOps Engineer");
        reqOpen.setStatus("OPEN"); // Đang tuyển (Đã duyệt)

        assertTrue("Tin OPEN phải đủ điều kiện xuất bản!", publishingService.isEligibleForPublish(reqOpen));
        publishingService.validateCanPublish(reqOpen);
    }

    /**
     * Tiêu chí 2: Xem trước đúng như giao diện khai báo trước khi xuất bản
     * Snapshot JSON tạo ra phải chứa đầy đủ và chuẩn xác các trường dữ liệu khai báo.
     */
    @Test
    public void test2_PreviewContainsExactDeclaredFields() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setId("req-uuid-123");
        req.setCode("REQ-2026-010");
        req.setTitle("Tuyển dụng Chuyên viên Tuyển dụng Cao cấp");
        req.setPositionTitle("Chuyên viên Tuyển dụng");
        req.setDepartmentId("dept-hr-01");
        req.setDepartmentName("Phòng Nhân sự");
        req.setHeadcount(3);
        req.setRecruitmentReason("NEW_HEADCOUNT");
        req.setMinSalary(new BigDecimal("18000000"));
        req.setMaxSalary(new BigDecimal("25000000"));
        req.setCurrency("VND");
        req.setDeadline(Date.valueOf(LocalDate.now().plusDays(30)));
        req.setJobDescription("Lập kế hoạch tuyển dụng, tìm nguồn ứng viên tiềm năng, phỏng vấn sơ loại...");
        req.setJobRequirements("Tối thiểu 3 năm kinh nghiệm trong ngành nhân sự, kỹ năng giao tiếp xuất sắc...");
        req.setSalaryExplanation("Lương phù hợp khung định biên mở rộng năm 2026");
        req.setRecruiterName("Nguyễn Văn Quản Lý");

        String snapshotJson = publishingService.createSnapshotJson(req);
        assertNotNull("Snapshot JSON không được rỗng!", snapshotJson);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = gson.fromJson(snapshotJson, Map.class);

        assertEquals("Mã yêu cầu phải khớp chính xác!", "REQ-2026-010", map.get("code"));
        assertEquals("Chức danh tuyển dụng phải khớp chính xác!", "Chuyên viên Tuyển dụng", map.get("positionTitle"));
        assertEquals("Phòng ban phải khớp chính xác!", "Phòng Nhân sự", map.get("departmentName"));
        assertEquals("Số lượng tuyển dụng phải khớp!", 3.0, (Double) map.get("headcount"), 0.001);
        assertEquals("Lý do tuyển dụng nhãn tiếng Việt phải là Tăng mới!", "Tăng mới", map.get("recruitmentReasonLabel"));
        assertTrue("Dải lương phải chứa số tiền định dạng chuẩn!", map.get("formattedSalaryRange").toString().contains("18.000.000"));
        assertEquals("Mô tả công việc phải giữ nguyên vẹn!", req.getJobDescription(), map.get("jobDescription"));
        assertEquals("Yêu cầu ứng viên phải giữ nguyên vẹn!", req.getJobRequirements(), map.get("jobRequirements"));
        assertEquals("Giải trình lương phải giữ nguyên vẹn!", req.getSalaryExplanation(), map.get("salaryExplanation"));
    }

    /**
     * Tiêu chí 3: Ghi lại bản xuất và thời điểm
     * Kiểm tra thực thể JobPostPublication lưu trữ số phiên bản, nhãn Bản xuất #N,
     * thời điểm xuất bản (Timestamp), người xuất bản và snapshot.
     */
    @Test
    public void test3_GhiLaiBanXuatVaThoiDiem_ModelAndVersioning() {
        JobPostPublication pub1 = new JobPostPublication();
        pub1.setId("pub-uuid-001");
        pub1.setRequisitionId("req-uuid-123");
        pub1.setVersionNumber(1);
        pub1.setChannel("CAREER_PORTAL");
        pub1.setStatus("PUBLISHED");

        Timestamp now = new Timestamp(System.currentTimeMillis());
        pub1.setPublishedAt(now);
        pub1.setPublishedBy("user-hr-01");
        pub1.setPublishedByName("Trần Thị HR");
        pub1.setSnapshotTitle("Tuyển dụng Senior Java Developer");
        pub1.setSnapshotData("{\"code\":\"REQ-2026-001\"}");
        pub1.setChangeSummary("Xuất bản lần đầu lên Website tuyển dụng công khai");

        assertEquals("Bản xuất số 1 phải có nhãn 'Bản xuất #1'", "Bản xuất #1", pub1.getVersionLabel());
        assertEquals("Người xuất bản phải khớp", "Trần Thị HR", pub1.getPublishedByName());
        assertEquals("Kênh xuất bản phải khớp", "CAREER_PORTAL", pub1.getChannel());
        assertEquals("Đang áp dụng", pub1.getStatusLabel());
        assertNotNull("Thời điểm xuất bản được định dạng chuỗi dd/MM/yyyy HH:mm:ss", pub1.getFormattedPublishedAt());
        assertFalse("Chuỗi thời điểm xuất bản không được là rỗng hoặc --", "--".equals(pub1.getFormattedPublishedAt()));

        // Kiểm tra Bản xuất #2 khi xuất bản lại
        JobPostPublication pub2 = new JobPostPublication();
        pub2.setVersionNumber(2);
        assertEquals("Bản xuất số 2 phải có nhãn 'Bản xuất #2'", "Bản xuất #2", pub2.getVersionLabel());
    }

    /**
     * Kiểm tra liên kết logic của JobPosting
     */
    @Test
    public void test3_JobPostingApprovalAndPublishStatus() {
        JobPosting jp = new JobPosting();
        jp.setId("jp-001");
        jp.setRequisitionId("req-001");
        jp.setTitle("Bài đăng tuyển dụng Backend");

        RecruitmentRequest req = new RecruitmentRequest();
        req.setStatus("OPEN"); // Đã duyệt
        jp.setRequisition(req);

        assertTrue("JobPosting phải nhận diện đúng tin đã duyệt!", jp.isApproved());

        jp.setStatus("PUBLISHED");
        assertTrue("JobPosting phải nhận diện đúng trạng thái đang xuất bản!", jp.isPublished());
        assertEquals("Đang xuất bản", jp.getStatusLabel());

        jp.setStatus("UNPUBLISHED");
        assertFalse("JobPosting đã gỡ không được coi là đang xuất bản!", jp.isPublished());
        assertEquals("Đã gỡ bài", jp.getStatusLabel());
    }
}

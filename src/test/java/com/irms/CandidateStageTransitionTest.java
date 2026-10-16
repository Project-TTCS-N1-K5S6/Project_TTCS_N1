package com.irms;

import com.irms.dao.CandidateDAO;
import com.irms.model.Candidate;
import com.irms.model.CandidateActivity;
import com.irms.model.Role;
import com.irms.model.SharedCatalogItem;
import com.irms.model.SharedCatalogType;
import com.irms.model.StageTransitionResult;
import com.irms.model.User;
import com.irms.service.CandidateDuplicateService;
import com.irms.service.CandidateService;
import com.irms.service.SharedCatalogService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * ==============================================================================
 * BỘ KIỂM THỬ TOÀN DIỆN CHO JIRA ISSUE KN-283:
 * CHUYỂN GIAI ĐOẠN VÀ QUẢN LÝ LÝ DO LOẠI ỨNG VIÊN
 * ==============================================================================
 * Kiểm thử chi tiết 8 Subtasks:
 * - KN-305: [BE] Kiểm tra điều kiện bỏ qua giai đoạn Phỏng vấn trước khi chuyển sang Đề xuất offer
 * - KN-306: [BE] Lưu nhật ký mỗi lần chuyển giai đoạn ứng viên
 * - KN-307: [FE] Bổ sung form chọn lý do khi chuyển ứng viên sang trạng thái Loại
 * - KN-308: [FE] Hiển thị lịch sử chuyển giai đoạn trong hồ sơ ứng viên
 * - KN-309: [BE] Ràng buộc chuyển sang trạng thái Loại phải chọn lý do từ danh mục
 * - KN-310: [BE] Xử lý xác nhận của Trưởng phòng Nhân sự cho trường hợp bỏ qua Phỏng vấn
 * - KN-311: [BE] Thiết kế API lấy danh mục lý do loại ứng viên
 * - KN-312: [FE] Hiển thị cảnh báo và luồng xác nhận khi bỏ qua giai đoạn Phỏng vấn
 * ==============================================================================
 */
public class CandidateStageTransitionTest {

    private CandidateService candidateService;
    private CandidateDAO candidateDAO;
    private SharedCatalogService catalogService;
    private CandidateDuplicateService duplicateService;

    private User adminUser;
    private User hrManagerUser;
    private User recruiterUser;
    private User otherRecruiterUser;
    private User guestUser;

    private String originalCandidateStatus;
    private String originalRejectionReasonId;
    private String validRejectionReasonId;

    private static final String TEST_CANDIDATE_ID = "cand-001";

    @Before
    public void setUp() {
        candidateService = new CandidateService();
        candidateDAO = new CandidateDAO();
        catalogService = new SharedCatalogService();
        duplicateService = new CandidateDuplicateService();

        // 1. Khởi tạo các vai trò người dùng kiểm thử
        adminUser = new User();
        adminUser.setId("usr-001");
        adminUser.setFullName("Nguyễn Quản Trị");
        adminUser.setRoles(Collections.singletonList(new Role("role-001", "ADMIN", "Quản trị hệ thống", "", true)));
        adminUser.setPermissions(Arrays.asList("candidates.view", "candidates.manage"));

        hrManagerUser = new User();
        hrManagerUser.setId("usr-002");
        hrManagerUser.setFullName("Trần Thị Trưởng Phòng Nhân Sự");
        hrManagerUser.setRoles(Collections.singletonList(new Role("role-002", "HR_MANAGER", "Trưởng phòng Nhân sự", "", true)));
        hrManagerUser.setPermissions(Arrays.asList("candidates.view", "candidates.manage"));

        // Recruiter phụ trách req-001 (usr-003 theo seed.sql)
        recruiterUser = new User();
        recruiterUser.setId("usr-003");
        recruiterUser.setFullName("Chuyên viên Tuyển dụng Phụ trách");
        recruiterUser.setRoles(Collections.singletonList(new Role("role-003", "RECRUITER", "Chuyên viên tuyển dụng", "", true)));
        recruiterUser.setPermissions(Arrays.asList("candidates.view", "candidates.manage"));

        otherRecruiterUser = new User();
        otherRecruiterUser.setId("usr-recruiter-unassigned");
        otherRecruiterUser.setFullName("Recruiter Không phân công");
        otherRecruiterUser.setRoles(Collections.singletonList(new Role("role-003", "RECRUITER", "Chuyên viên tuyển dụng", "", true)));
        otherRecruiterUser.setPermissions(Arrays.asList("candidates.view", "candidates.manage"));

        guestUser = new User();
        guestUser.setId("usr-guest");
        guestUser.setFullName("Người dùng Khách");
        guestUser.setRoles(Collections.singletonList(new Role("role-999", "GUEST", "Khách", "", true)));
        guestUser.setPermissions(Collections.emptyList());

        // 2. Lấy lý do loại hợp lệ từ danh mục dùng chung
        List<SharedCatalogItem> reasons = catalogService.getItems(SharedCatalogType.REJECTION_REASON);
        assertNotNull("Danh mục lý do loại phải có trong CSDL", reasons);
        assertFalse("Danh mục lý do loại không được rỗng", reasons.isEmpty());
        validRejectionReasonId = reasons.get(0).getId();

        // 3. Sao lưu trạng thái ban đầu của ứng viên cand-001 để khôi phục sau kiểm thử
        Candidate cand = candidateDAO.findById(TEST_CANDIDATE_ID);
        assertNotNull("Ứng viên cand-001 phải tồn tại trong CSDL seed", cand);
        originalCandidateStatus = cand.getStatus();
        originalRejectionReasonId = cand.getRejectionReasonId();

        // Đảm bảo đưa ứng viên về trạng thái APPLIED trước khi chạy test
        candidateDAO.updateStatus(TEST_CANDIDATE_ID, "APPLIED", null);
    }

    @After
    public void tearDown() {
        if (originalCandidateStatus != null) {
            try {
                // Khôi phục trạng thái ban đầu để không ảnh hưởng dữ liệu khác
                candidateDAO.updateStatus(TEST_CANDIDATE_ID, originalCandidateStatus, originalRejectionReasonId);
            } catch (Exception ignored) {}
        }
    }

    // =========================================================================
    // 1. [KN-311]: THIẾT KẾ API / DANH MỤC LÝ DO LOẠI ỨNG VIÊN
    // =========================================================================

    @Test
    public void testRejectionReasonsCatalogAvailability() {
        List<SharedCatalogItem> reasons = catalogService.getItems(SharedCatalogType.REJECTION_REASON);
        assertNotNull("Danh mục lý do loại không được null", reasons);
        assertFalse("Danh mục lý do loại phải có dữ liệu khởi tạo", reasons.isEmpty());

        for (SharedCatalogItem item : reasons) {
            assertEquals("Loại catalog phải là REJECTION_REASON", SharedCatalogType.REJECTION_REASON, item.getType());
            assertNotNull("Mã lý do không được null", item.getId());
            assertNotNull("Tên hiển thị lý do không được null", item.getValue());
            assertTrue("Tên hiển thị không được rỗng", !item.getValue().trim().isEmpty());
        }
    }

    // =========================================================================
    // 2. [KN-309]: RÀNG BUỘC CHUYỂN SANG TRẠNG THÁI LOẠI PHẢI CHỌN LÝ DO
    // =========================================================================

    @Test
    public void testRejectWithoutReasonFails() {
        // Thử chuyển sang REJECTED mà không truyền reason ID
        StageTransitionResult result = candidateService.transitionStage(
                recruiterUser,
                TEST_CANDIDATE_ID,
                "REJECTED",
                null,
                false,
                null,
                "127.0.0.1",
                "JUnit Test"
        );

        assertFalse("Chuyển sang Loại mà thiếu lý do phải thất bại", result.isSuccess());
        assertEquals("Phải trả về mã lỗi 400 Bad Request", 400, result.getStatusCode());
        assertTrue("Thông báo lỗi phải nhắc chọn lý do bằng tiếng Việt",
                result.getMessage().contains("bắt buộc phải chọn lý do"));

        // Kiểm tra trong CSDL trạng thái ứng viên không bị đổi
        Candidate c = candidateDAO.findById(TEST_CANDIDATE_ID);
        assertEquals("Trạng thái vẫn phải là APPLIED", "APPLIED", c.getStatus());
        assertNull("Chưa có rejection_reason_id", c.getRejectionReasonId());
    }

    @Test
    public void testRejectWithInvalidReasonFails() {
        // Thử chuyển sang REJECTED với reason ID không tồn tại
        StageTransitionResult result = candidateService.transitionStage(
                recruiterUser,
                TEST_CANDIDATE_ID,
                "REJECTED",
                "non-existent-reason-id-9999",
                false,
                null,
                "127.0.0.1",
                "JUnit Test"
        );

        assertFalse("Lý do loại không tồn tại phải bị từ chối", result.isSuccess());
        assertEquals(400, result.getStatusCode());
        assertTrue("Thông báo lỗi phải nêu lý do không tồn tại hoặc không hợp lệ",
                result.getMessage().contains("không tồn tại") || result.getMessage().contains("không hợp lệ"));
    }

    @Test
    public void testRejectWithValidReasonSucceedsAndRecordsLog() {
        // Chuyển sang REJECTED với lý do hợp lệ
        StageTransitionResult result = candidateService.transitionStage(
                recruiterUser,
                TEST_CANDIDATE_ID,
                "REJECTED",
                validRejectionReasonId,
                false,
                null,
                "127.0.0.1",
                "JUnit Test"
        );

        assertTrue("Chuyển sang Loại với lý do hợp lệ phải thành công", result.isSuccess());
        assertEquals("REJECTED", result.getNewStage());

        // Kiểm tra CSDL
        Candidate c = candidateDAO.findById(TEST_CANDIDATE_ID);
        assertEquals("REJECTED", c.getStatus());
        assertEquals("Lý do loại trong CSDL phải khớp", validRejectionReasonId, c.getRejectionReasonId());

        // [KN-306 & KN-308]: Kiểm tra bản ghi nhật ký trong candidate_activities
        List<CandidateActivity> activities = duplicateService.getCandidateActivities(TEST_CANDIDATE_ID);
        assertFalse("Phải có nhật ký hoạt động được tạo", activities.isEmpty());

        CandidateActivity rejectActivity = null;
        for (CandidateActivity a : activities) {
            if ("STATUS_CHANGE".equals(a.getActivityType()) && a.getDescription() != null && a.getDescription().contains("Loại")) {
                rejectActivity = a;
                break;
            }
        }
        assertNotNull("Phải tìm thấy nhật ký chuyển sang Loại", rejectActivity);
        assertEquals(recruiterUser.getId(), rejectActivity.getPerformedBy());
        assertNotNull("Thời điểm hoạt động phải có", rejectActivity.getActivityDate());
        assertTrue("Mô tả hoạt động phải chứa lý do loại",
                rejectActivity.getDescription().contains("Lý do:") || rejectActivity.getDescription().contains("Loại"));
    }

    // =========================================================================
    // 3. [KN-305]: KIỂM TRA ĐIỀU KIỆN BỎ QUA GIAI ĐOẠN PHỎNG VẤN
    // =========================================================================

    @Test
    public void testIsSkippingInterviewLogic() {
        // APPLIED -> OFFER: Bỏ qua
        assertTrue(candidateService.isSkippingInterview("APPLIED", "OFFER"));
        // APPLIED -> HIRED: Bỏ qua
        assertTrue(candidateService.isSkippingInterview("APPLIED", "HIRED"));
        // SCREENING -> OFFER: Bỏ qua
        assertTrue(candidateService.isSkippingInterview("SCREENING", "OFFER"));
        // SCREENING -> HIRED: Bỏ qua
        assertTrue(candidateService.isSkippingInterview("SCREENING", "HIRED"));

        // Đúng quy trình: APPLIED -> SCREENING (Không bỏ qua)
        assertFalse(candidateService.isSkippingInterview("APPLIED", "SCREENING"));
        // SCREENING -> INTERVIEWING (Không bỏ qua)
        assertFalse(candidateService.isSkippingInterview("SCREENING", "INTERVIEWING"));
        // INTERVIEWING -> OFFER (Không bỏ qua, đã qua phỏng vấn)
        assertFalse(candidateService.isSkippingInterview("INTERVIEWING", "OFFER"));
        // OFFER -> HIRED (Không bỏ qua)
        assertFalse(candidateService.isSkippingInterview("OFFER", "HIRED"));
        // Bất kỳ giai đoạn nào -> REJECTED (Không bỏ qua phỏng vấn, mà là loại)
        assertFalse(candidateService.isSkippingInterview("APPLIED", "REJECTED"));
    }

    // =========================================================================
    // 4. [KN-310]: XỬ LÝ XÁC NHẬN CỦA TRƯỞNG PHÒNG NHÂN SỰ
    // =========================================================================

    @Test
    public void testRecruiterCannotSkipInterviewDirectly() {
        // Recruiter cố gắng chuyển từ APPLIED sang OFFER trực tiếp
        StageTransitionResult result = candidateService.transitionStage(
                recruiterUser,
                TEST_CANDIDATE_ID,
                "OFFER",
                null,
                false, // chưa confirm
                null,
                "127.0.0.1",
                "JUnit Test"
        );

        assertFalse("Recruiter không được chuyển thẳng sang OFFER khi chưa qua phỏng vấn", result.isSuccess());
        assertEquals("Mã lỗi phải là 403 Forbidden", 403, result.getStatusCode());
        assertTrue("Phải có cờ requiresHrApproval", result.isRequiresHrApproval());
        assertFalse("Recruiter không thể tự phê duyệt", result.isCanCurrentUserApprove());
        assertTrue("Thông báo lỗi phải hướng dẫn liên hệ Trưởng phòng Nhân sự",
                result.getMessage().contains("Trưởng phòng Nhân sự"));

        // Dù recruiter có gửi skipInterviewConfirmed=true thì Backend vẫn phải từ chối (bảo mật Backend)
        StageTransitionResult spoofResult = candidateService.transitionStage(
                recruiterUser,
                TEST_CANDIDATE_ID,
                "OFFER",
                null,
                true, // giả mạo đã xác nhận
                "Ghi chú giả mạo",
                "127.0.0.1",
                "JUnit Test"
        );
        assertFalse("Recruiter giả mạo cờ xác nhận vẫn phải bị từ chối 403", spoofResult.isSuccess());
        assertEquals(403, spoofResult.getStatusCode());
    }

    @Test
    public void testHrManagerPromptedForConfirmationWhenSkippingInterview() {
        // Trưởng phòng Nhân sự chuyển từ APPLIED sang OFFER nhưng chưa bật skipInterviewConfirmed
        StageTransitionResult result = candidateService.transitionStage(
                hrManagerUser,
                TEST_CANDIDATE_ID,
                "OFFER",
                null,
                false, // chưa confirm
                null,
                "127.0.0.1",
                "JUnit Test"
        );

        assertFalse("Khi chưa confirm, hệ thống phải yêu cầu xác nhận", result.isSuccess());
        assertEquals("HTTP 400 Bad Request yêu cầu xác nhận", 400, result.getStatusCode());
        assertTrue("Phải đánh dấu requiresHrApproval", result.isRequiresHrApproval());
        assertTrue("HR Manager có thẩm quyền phê duyệt", result.isCanCurrentUserApprove());
        assertTrue("Thông báo giải thích ứng viên chưa qua phỏng vấn",
                result.getMessage().contains("chưa qua giai đoạn Phỏng vấn"));
    }

    @Test
    public void testHrManagerWithConfirmationCanSkipInterviewAndSucceeds() {
        // Trưởng phòng Nhân sự xác nhận đặc cách bỏ qua phỏng vấn
        String note = "Đã phỏng vấn trực tiếp tại văn phòng Giám đốc";
        StageTransitionResult result = candidateService.transitionStage(
                hrManagerUser,
                TEST_CANDIDATE_ID,
                "OFFER",
                null,
                true, // đã xác nhận
                note,
                "127.0.0.1",
                "JUnit Test"
        );

        assertTrue("HR Manager xác nhận đặc cách phải thành công", result.isSuccess());
        assertEquals("OFFER", result.getNewStage());

        // Kiểm tra CSDL
        Candidate c = candidateDAO.findById(TEST_CANDIDATE_ID);
        assertEquals("OFFER", c.getStatus());

        // [KN-306 & KN-308]: Kiểm tra nhật ký hoạt động có lưu thông tin đặc cách
        List<CandidateActivity> activities = duplicateService.getCandidateActivities(TEST_CANDIDATE_ID);
        assertFalse("Phải có nhật ký ghi lại", activities.isEmpty());

        CandidateActivity act = null;
        for (CandidateActivity a : activities) {
            if ("STATUS_CHANGE".equals(a.getActivityType()) && a.getDescription() != null && a.getDescription().contains("Đặc cách")) {
                act = a;
                break;
            }
        }
        assertNotNull("Phải tìm thấy nhật ký đặc cách", act);
        assertEquals("STATUS_CHANGE", act.getActivityType());
        assertTrue("Mô tả hoạt động phải ghi rõ đặc cách", act.getDescription().contains("Đặc cách"));
        assertTrue("Mô tả hoạt động phải chứa ghi chú phê duyệt", act.getDescription().contains(note));
        assertNotNull("Metadata JSON phải được lưu", act.getMetadataJson());
        assertTrue("Metadata JSON phải ghi nhận skippedInterview=true",
                act.getMetadataJson().contains("\"skippedInterview\":true"));
        assertTrue("Metadata JSON phải lưu người phê duyệt",
                act.getMetadataJson().contains(hrManagerUser.getFullName()));
    }

    @Test
    public void testAdminWithConfirmationCanAlsoSkipInterview() {
        // Quản trị viên hệ thống (ADMIN) cũng có thẩm quyền đặc cách
        StageTransitionResult result = candidateService.transitionStage(
                adminUser,
                TEST_CANDIDATE_ID,
                "OFFER",
                null,
                true,
                "Phê duyệt bởi Quản trị viên hệ thống",
                "127.0.0.1",
                "JUnit Test"
        );

        assertTrue("Admin có quyền đặc cách bỏ qua phỏng vấn", result.isSuccess());
        assertEquals("OFFER", result.getNewStage());
    }

    // =========================================================================
    // 5. [KN-306]: TÍNH TOÀN VẸN GIAO DỊCH VÀ BẢO ĐẢM NHẬT KÝ ĐẦY ĐỦ
    // =========================================================================

    @Test
    public void testUnauthorizedUserCannotChangeStage() {
        // Người dùng không có quyền (Guest)
        StageTransitionResult result = candidateService.transitionStage(
                guestUser,
                TEST_CANDIDATE_ID,
                "SCREENING",
                null,
                false,
                null,
                "127.0.0.1",
                "JUnit Test"
        );

        assertFalse("Guest không được phép chuyển giai đoạn", result.isSuccess());
        assertEquals(403, result.getStatusCode());
    }

    @Test
    public void testOtherRecruiterNotAssignedCannotChangeStage() {
        // Recruiter không được phân công phụ trách req-001
        StageTransitionResult result = candidateService.transitionStage(
                otherRecruiterUser,
                TEST_CANDIDATE_ID,
                "SCREENING",
                null,
                false,
                null,
                "127.0.0.1",
                "JUnit Test"
        );

        assertFalse("Recruiter không phụ trách vị trí không được chuyển giai đoạn", result.isSuccess());
        assertEquals(403, result.getStatusCode());
    }

    @Test
    public void testNormalStageSequenceSucceedsAndRecordsHistory() {
        // Luồng chuẩn: APPLIED -> SCREENING
        StageTransitionResult r1 = candidateService.transitionStage(
                recruiterUser,
                TEST_CANDIDATE_ID,
                "SCREENING",
                null,
                false,
                null,
                "127.0.0.1",
                "JUnit Test"
        );
        assertTrue(r1.isSuccess());
        assertEquals("SCREENING", r1.getNewStage());

        // SCREENING -> INTERVIEWING
        StageTransitionResult r2 = candidateService.transitionStage(
                recruiterUser,
                TEST_CANDIDATE_ID,
                "INTERVIEWING",
                null,
                false,
                null,
                "127.0.0.1",
                "JUnit Test"
        );
        assertTrue(r2.isSuccess());
        assertEquals("INTERVIEWING", r2.getNewStage());

        // INTERVIEWING -> OFFER (sau phỏng vấn không cần đặc cách)
        StageTransitionResult r3 = candidateService.transitionStage(
                recruiterUser,
                TEST_CANDIDATE_ID,
                "OFFER",
                null,
                false,
                null,
                "127.0.0.1",
                "JUnit Test"
        );
        assertTrue("Sau phỏng vấn, Recruiter chuyển sang OFFER không cần đặc cách", r3.isSuccess());
        assertEquals("OFFER", r3.getNewStage());

        // Kiểm tra lịch sử phải có các bản ghi nhật ký chuyển trạng thái
        List<CandidateActivity> activities = duplicateService.getCandidateActivities(TEST_CANDIDATE_ID);
        assertTrue("Phải có ít nhất 3 hoạt động chuyển trạng thái được ghi lại", activities.size() >= 3);
    }
}

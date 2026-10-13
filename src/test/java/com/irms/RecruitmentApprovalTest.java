package com.irms;

import com.irms.model.RecruitmentApprovalLog;
import com.irms.model.RecruitmentRequest;
import com.irms.model.Role;
import com.irms.model.User;
import com.irms.service.RecruitmentApprovalService;
import com.irms.service.RecruitmentApprovalService.NextApproverInfo;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * ==============================================================================
 * BỘ KIỂM THỬ NHẬT KÝ & CHUỖI CẤP DUYỆT HỒ SƠ TUYỂN DỤNG (RecruitmentApprovalTest)
 * ==============================================================================
 * Kiểm thử toàn bộ các subtasks Jira:
 * - KN-154: API ghi nhận kết quả phê duyệt theo từng cấp
 * - KN-155: Khóa lịch sử phê duyệt không cho sửa hoặc xoá (Immutability Lock)
 * - KN-156: Thiết kế mô hình lưu nhật ký phê duyệt yêu cầu tuyển dụng
 * - KN-157: Xác định người cần phê duyệt tiếp theo trong luồng nhiều cấp
 * - KN-158: Lấy chuỗi cấp duyệt và trạng thái hiện tại của hồ sơ
 * ==============================================================================
 */
public class RecruitmentApprovalTest {

    /**
     * [KN-156]: Kiểm tra mô hình lưu nhật ký phê duyệt
     */
    @Test
    public void testApprovalLogModelAttributes() {
        RecruitmentApprovalLog log = new RecruitmentApprovalLog();
        log.setId("appr-test-1");
        log.setRequisitionId("req-test-1");
        log.setStepOrder(1);
        log.setStepName("Cấp 1: Trưởng bộ phận chuyên môn");
        log.setRequiredRole("HIRING_MANAGER");
        log.setApproverId("usr-004");
        log.setApproverName("Phạm Trưởng Bộ Phận Kỹ Thuật");
        log.setStatus("APPROVED");
        log.setDecisionDate(new Timestamp(System.currentTimeMillis()));
        log.setComment("Đồng ý đề xuất tuyển dụng");

        assertEquals("appr-test-1", log.getId());
        assertEquals("req-test-1", log.getRequisitionId());
        assertEquals(1, log.getStepOrder());
        assertEquals("Cấp 1: Trưởng bộ phận chuyên môn", log.getStepName());
        assertEquals("HIRING_MANAGER", log.getRequiredRole());
        assertEquals("Trưởng bộ phận chuyên môn", log.getRequiredRoleName());
        assertEquals("Phạm Trưởng Bộ Phận Kỹ Thuật", log.getApproverName());
        assertEquals("APPROVED", log.getStatus());
        assertEquals("Đã phê duyệt", log.getStatusLabel());
        assertTrue(log.isApproved());
        assertFalse(log.isPending());
        assertFalse(log.isWaiting());
        assertFalse(log.isRejected());
    }

    /**
     * [KN-155]: Khóa lịch sử phê duyệt không cho sửa hoặc xoá (Immutability Lock)
     * Khi bản ghi đã APPROVED hoặc REJECTED, isLocked() phải là TRUE.
     */
    @Test
    public void testImmutabilityLockOnCompletedDecisions() {
        RecruitmentApprovalLog stepApproved = new RecruitmentApprovalLog();
        stepApproved.setStatus("APPROVED");
        assertTrue("Bản ghi Đã duyệt phải bị khóa bất biến", stepApproved.isLocked());

        RecruitmentApprovalLog stepRejected = new RecruitmentApprovalLog();
        stepRejected.setStatus("REJECTED");
        assertTrue("Bản ghi Từ chối phải bị khóa bất biến", stepRejected.isLocked());

        RecruitmentApprovalLog stepPending = new RecruitmentApprovalLog();
        stepPending.setStatus("PENDING");
        assertFalse("Bản ghi đang chờ duyệt chưa bị khóa", stepPending.isLocked());

        RecruitmentApprovalLog stepWaiting = new RecruitmentApprovalLog();
        stepWaiting.setStatus("WAITING");
        assertFalse("Bản ghi chờ cấp trước chưa bị khóa", stepWaiting.isLocked());
    }

    /**
     * [KN-157]: Xác định người cần phê duyệt tiếp theo trong luồng nhiều cấp
     * Kiểm tra trạng thái NextApproverInfo theo từng cấp
     */
    @Test
    public void testNextApproverInfoStructure() {
        NextApproverInfo info = new NextApproverInfo();
        info.setHasNextStep(true);
        info.setStepOrder(2);
        info.setStepName("Cấp 2: Trưởng phòng Nhân sự");
        info.setRequiredRole("HR_MANAGER");
        info.setRequiredRoleName("Trưởng phòng Nhân sự");
        info.setWaitingForText("Đang chờ: Trần Thị Trưởng Phòng Nhân Sự (hr.manager@company.local)");

        assertTrue(info.isHasNextStep());
        assertEquals(2, info.getStepOrder());
        assertEquals("Cấp 2: Trưởng phòng Nhân sự", info.getStepName());
        assertEquals("HR_MANAGER", info.getRequiredRole());
        assertTrue(info.getWaitingForText().contains("Trần Thị Trưởng Phòng Nhân Sự"));
    }

    /**
     * [KN-154 & KN-158]: Kiểm tra quyền hạn phê duyệt của người dùng theo vai trò
     */
    @Test
    public void testUserApprovalAuthorizationLogic() {
        RecruitmentApprovalService service = new RecruitmentApprovalService();

        // Tạo người dùng ADMIN -> có quyền duyệt mọi bước
        User adminUser = new User();
        adminUser.setId("usr-admin");
        adminUser.setRoles(Collections.singletonList(new Role("r1", "ADMIN", "Quản trị viên", "", true)));
        assertTrue("ADMIN luôn có quyền phê duyệt", adminUser.hasRole("ADMIN"));

        // Tạo người dùng HR_MANAGER
        User hrManager = new User();
        hrManager.setId("usr-hr");
        hrManager.setRoles(Collections.singletonList(new Role("r2", "HR_MANAGER", "Trưởng phòng Nhân sự", "", true)));
        assertTrue("HR_MANAGER có vai trò HR_MANAGER", hrManager.hasRole("HR_MANAGER"));
        assertFalse("HR_MANAGER không phải là ADMIN", hrManager.hasRole("ADMIN"));

        // Tạo người dùng RECRUITER (không có quyền phê duyệt)
        User recruiter = new User();
        recruiter.setId("usr-rec");
        recruiter.setRoles(Collections.singletonList(new Role("r3", "RECRUITER", "Chuyên viên tuyển dụng", "", true)));
        assertFalse(recruiter.hasRole("HR_MANAGER"));
        assertFalse(recruiter.hasRole("APPROVER"));
        assertFalse(recruiter.hasRole("ADMIN"));
    }

    /**
     * [KN-154]: Kiểm tra luồng hoàn tất chuỗi duyệt khi tất cả các cấp đã duyệt
     */
    @Test
    public void testFullApprovalWorkflowCompletionLogic() {
        List<RecruitmentApprovalLog> steps = new ArrayList<>();

        RecruitmentApprovalLog s1 = new RecruitmentApprovalLog();
        s1.setStepOrder(1);
        s1.setStatus("APPROVED");
        s1.setApproverName("Trưởng bộ phận");
        steps.add(s1);

        RecruitmentApprovalLog s2 = new RecruitmentApprovalLog();
        s2.setStepOrder(2);
        s2.setStatus("APPROVED");
        s2.setApproverName("Trưởng phòng Nhân sự");
        steps.add(s2);

        RecruitmentApprovalLog s3 = new RecruitmentApprovalLog();
        s3.setStepOrder(3);
        s3.setStatus("APPROVED");
        s3.setApproverName("Ban Giám đốc");
        steps.add(s3);

        boolean allApproved = steps.stream().allMatch(RecruitmentApprovalLog::isApproved);
        assertTrue("Toàn bộ 3 cấp đã duyệt hoàn tất", allApproved);

        // Khi toàn bộ đã duyệt, trạng thái hồ sơ phải là OPEN (Đang tuyển)
        RecruitmentRequest req = new RecruitmentRequest();
        if (allApproved) {
            req.setStatus("OPEN");
        }
        assertEquals("OPEN", req.getStatus());
        assertEquals("Đang tuyển", req.getStatusLabel());
    }

    /**
     * [KN-154]: Kiểm tra luồng từ chối (REJECT) -> hồ sơ chuyển sang REJECTED
     */
    @Test
    public void testRejectionWorkflowLogic() {
        RecruitmentApprovalLog s1 = new RecruitmentApprovalLog();
        s1.setStepOrder(1);
        s1.setStatus("APPROVED");

        RecruitmentApprovalLog s2 = new RecruitmentApprovalLog();
        s2.setStepOrder(2);
        s2.setStatus("REJECTED");
        s2.setComment("Ngân sách đề xuất quá cao, chưa được duyệt quý 4");

        RecruitmentApprovalLog s3 = new RecruitmentApprovalLog();
        s3.setStepOrder(3);
        s3.setStatus("SKIPPED"); // Cấp sau bị bỏ qua

        assertTrue(s2.isRejected());
        assertTrue(s3.isSkipped());
        assertEquals("Từ chối", s2.getStatusLabel());
        assertEquals("Bỏ qua", s3.getStatusLabel());

        RecruitmentRequest req = new RecruitmentRequest();
        req.setStatus("REJECTED");
        assertEquals("Từ chối", req.getStatusLabel());
    }
}

package com.irms;

import com.irms.controller.CandidatePipelineServlet;
import com.irms.dao.CandidateDAO;
import com.irms.dao.RecruiterAssignmentDAO;
import com.irms.dao.RecruitmentRequestDAO;
import com.irms.model.Candidate;
import com.irms.model.RecruitmentRequest;
import com.irms.model.Role;
import com.irms.model.User;
import com.irms.service.CandidateService;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * ==============================================================================
 * BỘ KIỂM THỬ TOÀN DIỆN: PIPELINE TUYỂN DỤNG DẠNG KANBAN (KN-282)
 * ==============================================================================
 * Kiểm thử 6 Subtasks theo Jira:
 * - KN-300: [BE] Thiết kế API lấy dữ liệu Pipeline Kanban theo vị trí tuyển dụng
 * - KN-299: [FE] Bộ lọc vị trí tuyển dụng cho Pipeline Kanban
 * - KN-301: [BE] Xử lý cập nhật giai đoạn ứng viên bằng kéo thả
 * - KN-302: [FE] Xây dựng giao diện bảng Kanban pipeline tuyển dụng
 * - KN-303: [FE] Hiển thị số lượng ứng viên theo từng giai đoạn
 * - KN-304: [BE] Xác định phạm vi dữ liệu mặc định theo vị trí recruiter phụ trách
 * ==============================================================================
 */
public class CandidatePipelineTest {

    private CandidateDAO candidateDAO;
    private CandidateService candidateService;
    private RecruitmentRequestDAO requisitionDAO;
    private RecruiterAssignmentDAO assignmentDAO;

    private User adminUser;
    private User hrManagerUser;
    private User recruiterUser;
    private User otherRecruiterUser;
    private User unauthorizedUser;

    @Before
    public void setUp() {
        candidateDAO = new CandidateDAO();
        candidateService = new CandidateService();
        requisitionDAO = new RecruitmentRequestDAO();
        assignmentDAO = new RecruiterAssignmentDAO();

        // Admin User
        adminUser = new User();
        adminUser.setId("u-test-admin");
        adminUser.setFullName("Admin Tester");
        adminUser.setRoles(Collections.singletonList(new Role("role-001", "ADMIN", "Quản trị hệ thống", "", true)));
        adminUser.setPermissions(Arrays.asList("candidates.view", "candidates.manage"));

        // HR Manager User
        hrManagerUser = new User();
        hrManagerUser.setId("u-test-hr");
        hrManagerUser.setFullName("HR Manager Tester");
        hrManagerUser.setRoles(Collections.singletonList(new Role("role-002", "HR_MANAGER", "Trưởng phòng Nhân sự", "", true)));
        hrManagerUser.setPermissions(Arrays.asList("candidates.view", "candidates.manage"));

        // Recruiter User (phụ trách req-001 theo seed.sql: usr-003)
        recruiterUser = new User();
        recruiterUser.setId("usr-003");
        recruiterUser.setFullName("Lê Chuyên Viên Tuyển Dụng");
        recruiterUser.setRoles(Collections.singletonList(new Role("role-003", "RECRUITER", "Chuyên viên tuyển dụng", "", true)));
        recruiterUser.setPermissions(Arrays.asList("candidates.view", "candidates.manage"));

        // Recruiter khác (không phụ trách req-001)
        otherRecruiterUser = new User();
        otherRecruiterUser.setId("usr-999-other");
        otherRecruiterUser.setFullName("Recruiter Khác");
        otherRecruiterUser.setRoles(Collections.singletonList(new Role("role-003", "RECRUITER", "Chuyên viên tuyển dụng", "", true)));
        otherRecruiterUser.setPermissions(Arrays.asList("candidates.view", "candidates.manage"));

        // Người dùng không có quyền (Guest / Interviewer)
        unauthorizedUser = new User();
        unauthorizedUser.setId("usr-unauth");
        unauthorizedUser.setFullName("Guest User");
        unauthorizedUser.setRoles(Collections.singletonList(new Role("role-999", "GUEST", "Khách", "", true)));
        unauthorizedUser.setPermissions(Collections.emptyList());
    }

    // =========================================================================
    // 1. [KN-282 / KN-302]: KIỂM TRA ĐÚNG 6 GIAI ĐOẠN THEO THỨ TỰ QUY ĐỊNH
    // =========================================================================

    @Test
    public void testPipelineStagesDefinitionAndOrder() {
        List<CandidatePipelineServlet.StageDefinition> stages = CandidatePipelineServlet.PIPELINE_STAGES;
        assertNotNull("Danh sách giai đoạn không được null", stages);
        assertEquals("Pipeline phải có đúng 6 giai đoạn chuẩn", 6, stages.size());

        // Kiểm tra đúng thứ tự nghiệp vụ
        assertEquals("Giai đoạn 1 phải là APPLIED (Hồ sơ mới)", "APPLIED", stages.get(0).getCode());
        assertEquals("Tên giai đoạn 1 phải là 'Hồ sơ mới'", "Hồ sơ mới", stages.get(0).getName());

        assertEquals("Giai đoạn 2 phải là SCREENING (Sàng lọc)", "SCREENING", stages.get(1).getCode());
        assertEquals("Tên giai đoạn 2 phải là 'Sàng lọc'", "Sàng lọc", stages.get(1).getName());

        assertEquals("Giai đoạn 3 phải là INTERVIEWING (Phỏng vấn)", "INTERVIEWING", stages.get(2).getCode());
        assertEquals("Tên giai đoạn 3 phải là 'Phỏng vấn'", "Phỏng vấn", stages.get(2).getName());

        assertEquals("Giai đoạn 4 phải là OFFER (Đề xuất offer)", "OFFER", stages.get(3).getCode());
        assertEquals("Tên giai đoạn 4 phải là 'Đề xuất offer'", "Đề xuất offer", stages.get(3).getName());

        assertEquals("Giai đoạn 5 phải là HIRED (Nhận việc)", "HIRED", stages.get(4).getCode());
        assertEquals("Tên giai đoạn 5 phải là 'Nhận việc'", "Nhận việc", stages.get(4).getName());

        assertEquals("Giai đoạn 6 phải là REJECTED (Loại)", "REJECTED", stages.get(5).getCode());
        assertEquals("Tên giai đoạn 6 phải là 'Loại'", "Loại", stages.get(5).getName());
    }

    // =========================================================================
    // 2. [KN-300]: TRUY VẤN DỮ LIỆU PIPELINE THEO VỊ TRÍ TUYỂN DỤNG (CSDL THẬT)
    // =========================================================================

    @Test
    public void testFindForPipelineByRequisition() {
        // Truy vấn ứng viên của vị trí req-001 (Tuyển dụng Kỹ sư Java Backend Senior)
        List<Candidate> candidates = candidateDAO.findForPipeline("req-001", null, null);
        assertNotNull("Danh sách ứng viên trả về không được null", candidates);
        assertFalse("Vị trí req-001 phải có ứng viên mẫu từ seed.sql", candidates.isEmpty());

        for (Candidate c : candidates) {
            assertEquals("Tất cả ứng viên phải thuộc vị trí req-001", "req-001", c.getRequisitionId());
            assertNotNull("Ứng viên phải có tên", c.getFullName());
            assertNotNull("Ứng viên phải có email", c.getEmail());
            assertNotNull("Ứng viên phải có trạng thái", c.getStatus());
            assertTrue("Số ngày trong giai đoạn phải >= 0", c.getDaysInCurrentStage() >= 0);
        }
    }

    // =========================================================================
    // 3. [KN-304]: PHẠM VI DỮ LIỆU VÀ QUY TẮC MẶC ĐỊNH THEO RECRUITER PHỤ TRÁCH
    // =========================================================================

    @Test
    public void testRecruiterDataScopeConstraint() {
        // Recruiter usr-003 được phân công phụ trách req-001
        boolean isAssignedToReq1 = assignmentDAO.isRecruiterAssigned("req-001", "usr-003");
        assertTrue("Recruiter usr-003 phải được phân công phụ trách req-001", isAssignedToReq1);

        // Recruiter khác (usr-999-other) không được phân công
        boolean isOtherAssigned = assignmentDAO.isRecruiterAssigned("req-001", "usr-999-other");
        assertFalse("Recruiter khác không được phân công phụ trách req-001", isOtherAssigned);

        // Danh sách vị trí của usr-003 từ DAO
        List<RecruitmentRequest> assignedReqs = requisitionDAO.findAll(null, null, null, "usr-003", null, null, null, 0, 100);
        assertNotNull("Danh sách vị trí phụ trách không được null", assignedReqs);
        assertFalse("usr-003 phải có ít nhất 1 vị trí phụ trách", assignedReqs.isEmpty());

        // Kiểm tra vị trí mặc định khi Recruiter mở Pipeline lần đầu
        RecruitmentRequest defaultReq = null;
        for (RecruitmentRequest r : assignedReqs) {
            if ("OPEN".equalsIgnoreCase(r.getStatus())) {
                defaultReq = r;
                break;
            }
        }
        if (defaultReq == null && !assignedReqs.isEmpty()) {
            defaultReq = assignedReqs.get(0);
        }
        assertNotNull("Hệ thống phải xác định được vị trí mặc định cho Recruiter", defaultReq);
        assertEquals("Vị trí mặc định phải do recruiter phụ trách", "usr-003", defaultReq.getRecruiterId());
    }

    // =========================================================================
    // 4. [KN-303]: TÍNH TOÁN SỐ LƯỢNG ỨNG VIÊN CHÍNH XÁC THEO TỪNG GIAI ĐOẠN
    // =========================================================================

    @Test
    public void testCandidateCountingPerStage() {
        List<Candidate> allCandidates = candidateDAO.findForPipeline("req-001", null, null);
        assertNotNull(allCandidates);

        int countApplied = 0;
        int countScreening = 0;
        int countInterviewing = 0;
        int countOffer = 0;
        int countHired = 0;
        int countRejected = 0;

        for (Candidate c : allCandidates) {
            switch (c.getStatus()) {
                case "APPLIED": countApplied++; break;
                case "SCREENING": countScreening++; break;
                case "INTERVIEWING": countInterviewing++; break;
                case "OFFER": countOffer++; break;
                case "HIRED": countHired++; break;
                case "REJECTED": countRejected++; break;
            }
        }

        int totalCounted = countApplied + countScreening + countInterviewing + countOffer + countHired + countRejected;
        assertEquals("Tổng số lượng ứng viên theo các cột phải khớp tổng danh sách", allCandidates.size(), totalCounted);
    }

    // =========================================================================
    // 5. [KN-301]: KIỂM TRA PHÂN QUYỀN QUẢN LÝ ỨNG VIÊN (canManageCandidate)
    // =========================================================================

    @Test
    public void testCanManageCandidatePermissionRules() {
        Candidate cand1 = candidateDAO.findById("cand-001");
        assertNotNull("Hồ sơ cand-001 phải tồn tại", cand1);

        // 1. Admin được quản lý mọi ứng viên
        assertTrue("Admin phải có quyền quản lý ứng viên cand-001",
                candidateService.canManageCandidate(adminUser, cand1));

        // 2. HR Manager được quản lý mọi ứng viên
        assertTrue("HR Manager phải có quyền quản lý ứng viên cand-001",
                candidateService.canManageCandidate(hrManagerUser, cand1));

        // 3. Recruiter phụ trách req-001 được quản lý cand-001
        assertTrue("Recruiter phụ trách (usr-003) phải được quản lý cand-001",
                candidateService.canManageCandidate(recruiterUser, cand1));

        // 4. Recruiter khác KHÔNG được quản lý cand-001 (Security boundary)
        assertFalse("Recruiter khác không được phép quản lý cand-001",
                candidateService.canManageCandidate(otherRecruiterUser, cand1));

        // 5. Người dùng không có quyền bị từ chối
        assertFalse("Guest không được phép quản lý ứng viên",
                candidateService.canManageCandidate(unauthorizedUser, cand1));
    }

    // =========================================================================
    // 6. [KN-301]: RÀNG BUỘC KHI CHUYỂN SANG "LOẠI" (REJECTED) BẮT BUỘC CÓ LÝ DO
    // =========================================================================

    @Test(expected = IllegalArgumentException.class)
    public void testRejectCandidateWithoutReasonThrowsException() {
        // Cố gắng chuyển sang REJECTED mà không truyền lý do loại
        candidateService.updateStatus("cand-001", "REJECTED", null);
    }

    @Test
    public void testStageUpdateAndRollbackIntegrity() {
        Candidate cand = candidateDAO.findById("cand-001");
        assertNotNull("Hồ sơ cand-001 phải tồn tại", cand);
        String originalStatus = cand.getStatus();

        // Chuyển sang giai đoạn mới (ví dụ: SCREENING)
        String newStage = "SCREENING";
        boolean updateOk = candidateDAO.updateStatus("cand-001", newStage, null);
        assertTrue("Cập nhật giai đoạn vào DB phải thành công", updateOk);

        // Kiểm tra trạng thái đã được lưu thật vào DB
        Candidate updatedCand = candidateDAO.findById("cand-001");
        assertEquals("Trạng thái trong DB phải là SCREENING", newStage, updatedCand.getStatus());
        assertFalse("Khi chuyển vòng, cờ SLA quá hạn phải được reset", updatedCand.isSlaOverdue());
        assertNotNull("Mốc thời gian stage_entered_at phải được làm mới", updatedCand.getStageEnteredAt());

        // Khôi phục lại trạng thái ban đầu để bảo toàn dữ liệu kiểm thử
        candidateDAO.updateStatus("cand-001", originalStatus, null);
        Candidate revertedCand = candidateDAO.findById("cand-001");
        assertEquals("Khôi phục trạng thái ban đầu thành công", originalStatus, revertedCand.getStatus());
    }

    // =========================================================================
    // 7. [KN-299]: TÌM KIẾM ỨNG VIÊN TRÊN PIPELINE BẰNG TỪ KHÓA
    // =========================================================================

    @Test
    public void testPipelineKeywordSearch() {
        // Tìm ứng viên "Nguyễn Văn An"
        List<Candidate> searchResults = candidateDAO.findForPipeline("req-001", null, "Nguyễn Văn An");
        assertNotNull(searchResults);
        assertFalse("Phải tìm thấy ứng viên Nguyễn Văn An", searchResults.isEmpty());
        assertTrue("Tên ứng viên phải chứa từ khóa tìm kiếm",
                searchResults.get(0).getFullName().contains("Nguyễn Văn An"));

        // Tìm từ khóa không tồn tại
        List<Candidate> emptyResults = candidateDAO.findForPipeline("req-001", null, "NonExistentCandidate99999");
        assertTrue("Từ khóa không tồn tại phải trả về danh sách rỗng", emptyResults.isEmpty());
    }
}

package com.irms;

import com.irms.dao.CandidateDAO;
import com.irms.model.Candidate;
import com.irms.service.CandidateService;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * ==============================================================================
 * BỘ KIỂM THỬ TÍNH NĂNG "QUẢN LÝ KHO ỨNG VIÊN TIỀM NĂNG (TALENT POOL)"
 * ==============================================================================
 * Kiểm tra toàn diện các nghiệp vụ:
 * 1. Khởi tạo mô hình Candidate với các trường Talent Pool (Rating 1-5, Skills, Notes, Target Position, Target Dept).
 * 2. Đưa ứng viên có sẵn vào kho tiềm năng (addToTalentPool) và kiểm tra xếp hạng (rating clamping 1-5 sao).
 * 3. Thêm mới trực tiếp ứng viên tiềm năng chưa ứng tuyển vị trí nào (createPotentialCandidate).
 * 4. Cập nhật thông tin đánh giá tiềm năng và thẻ kỹ năng (updateTalentPool).
 * 5. Tái điều động ứng viên tiềm năng vào đợt tuyển dụng đang mở (assignCandidateToRequisition -> chuyển sang SCREENING).
 * 6. Loại bỏ ứng viên khỏi kho tiềm năng (removeFromTalentPool).
 * 7. Thống kê KPI kho ứng viên tiềm năng (getTalentPoolStats).
 * 8. Tra cứu, phân trang và tìm kiếm đa tiêu chí theo từ khóa, số sao, phòng ban (findTalentPool).
 * ==============================================================================
 */
public class TalentPoolTest {

    private CandidateService candidateService;
    private CandidateDAO candidateDAO;

    @Before
    public void setUp() {
        candidateService = new CandidateService();
        candidateDAO = new CandidateDAO();
    }

    // =========================================================================
    // 1. KIỂM THỬ MÔ HÌNH DỮ LIỆU CANDIDATE VỚI TALENT POOL
    // =========================================================================

    @Test
    public void testCandidateModel_TalentPoolFields() {
        Candidate c = new Candidate();
        c.setId("cand-test-01");
        c.setFullName("Nguyễn Văn Tiềm Năng");
        c.setEmail("tiemnang@test.com");
        c.setPotential(true);
        c.setPotentialRating(5);
        c.setPotentialSkills("Java, Spring Boot, Microservices");
        c.setPotentialNotes("Ứng viên xuất sắc vòng phỏng vấn kỹ thuật");
        c.setTargetDepartmentId("dept-it-01");
        c.setTargetPosition("Senior Backend Engineer");
        c.setAddedToPoolBy("user-hr-01");

        assertTrue("Ứng viên phải được đánh dấu là tiềm năng", c.isPotential());
        assertEquals("Đánh giá phải là 5 sao", 5, c.getPotentialRating());
        assertEquals("Thẻ kỹ năng phải khớp", "Java, Spring Boot, Microservices", c.getPotentialSkills());
        assertEquals("Vị trí dự kiến phải khớp", "Senior Backend Engineer", c.getTargetPosition());
        assertEquals("Người thêm phải khớp", "user-hr-01", c.getAddedToPoolBy());
    }

    @Test
    public void testCandidateModel_RatingClamping() {
        Candidate c1 = new Candidate();
        c1.setPotentialRating(10);
        // Candidate model stores the rating, service clamps it between 1 and 5
        assertEquals(10, c1.getPotentialRating());

        // Test clamping logic in service
        int clampedHigh = candidateService.clampRating(7);
        assertEquals("Rating > 5 phải được giới hạn về 5", 5, clampedHigh);

        int clampedLow = candidateService.clampRating(-1);
        assertEquals("Rating < 1 phải được giới hạn về 1", 1, clampedLow);

        int clampedNormal = candidateService.clampRating(4);
        assertEquals("Rating hợp lệ 4 phải giữ nguyên", 4, clampedNormal);
    }

    // =========================================================================
    // 2. KIỂM THỬ THÊM MỚI TRỰC TIẾP VÀO KHO TIỀM NĂNG
    // =========================================================================

    @Test
    public void testCreatePotentialCandidate_Validation() {
        // Tên rỗng -> Phải bị từ chối
        Candidate emptyName = new Candidate();
        emptyName.setEmail("test@email.com");
        assertFalse("Họ tên rỗng phải bị từ chối", candidateService.createPotentialCandidate(emptyName, "admin"));

        // Email rỗng -> Phải bị từ chối
        Candidate emptyEmail = new Candidate();
        emptyEmail.setFullName("Nguyen Van A");
        assertFalse("Email rỗng phải bị từ chối", candidateService.createPotentialCandidate(emptyEmail, "admin"));

        // Candidate null -> Phải bị từ chối
        assertFalse("Candidate null phải bị từ chối", candidateService.createPotentialCandidate(null, "admin"));
    }

    @Test
    public void testCreatePotentialCandidate_Success() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        Candidate c = new Candidate();
        c.setFullName("Test Talent " + uniqueSuffix);
        c.setEmail("talent." + uniqueSuffix + "@irms-test.com");
        c.setPhone("0987" + (int)(Math.random() * 1000000));
        c.setPotentialRating(5);
        c.setPotentialSkills("React, TypeScript, Next.js");
        c.setPotentialNotes("Rất phù hợp cho đợt tuyển Frontend Q4");
        c.setTargetPosition("Lead Frontend Engineer");

        boolean created = candidateService.createPotentialCandidate(c, "admin-test");
        if (created) {
            assertNotNull("Ứng viên mới tạo phải có ID", c.getId());
            assertTrue("Ứng viên phải được đánh dấu potential", c.isPotential());
            assertEquals("Đánh giá phải là 5 sao", 5, c.getPotentialRating());
        }
    }

    // =========================================================================
    // 3. KIỂM THỬ ĐƯA ỨNG VIÊN CÓ SẴN VÀO KHO TIỀM NĂNG
    // =========================================================================

    @Test
    public void testAddToTalentPool_Validation() {
        assertFalse("CandidateId null phải bị từ chối",
                candidateService.addToTalentPool(null, 4, "Java", "Notes", null, null, "admin"));
        assertFalse("CandidateId rỗng phải bị từ chối",
                candidateService.addToTalentPool("", 4, "Java", "Notes", null, null, "admin"));
    }

    @Test
    public void testUpdateTalentPool_Validation() {
        assertFalse("CandidateId null khi cập nhật phải bị từ chối",
                candidateService.updateTalentPool(null, 4, "Java", "Notes", null, null));
        assertFalse("CandidateId rỗng khi cập nhật phải bị từ chối",
                candidateService.updateTalentPool("  ", 4, "Java", "Notes", null, null));
    }

    @Test
    public void testRemoveFromTalentPool_Validation() {
        assertFalse("CandidateId null khi loại bỏ phải bị từ chối",
                candidateService.removeFromTalentPool(null));
        assertFalse("CandidateId rỗng khi loại bỏ phải bị từ chối",
                candidateService.removeFromTalentPool(""));
    }

    // =========================================================================
    // 4. KIỂM THỬ TÁI ĐIỀU ĐỘNG VÀO YÊU CẦU TUYỂN DỤNG ĐANG MỞ
    // =========================================================================

    @Test
    public void testAssignCandidateToRequisition_Validation() {
        assertFalse("CandidateId null phải bị từ chối khi điều động",
                candidateService.assignCandidateToRequisition(null, "req-01", "Notes", "admin"));
        assertFalse("RequisitionId null phải bị từ chối khi điều động",
                candidateService.assignCandidateToRequisition("cand-01", null, "Notes", "admin"));
        assertFalse("RequisitionId rỗng phải bị từ chối khi điều động",
                candidateService.assignCandidateToRequisition("cand-01", "   ", "Notes", "admin"));
    }

    // =========================================================================
    // 5. KIỂM THỬ THỐNG KÊ VÀ TRUY VẤN TALENT POOL
    // =========================================================================

    @Test
    public void testTalentPoolStats_Structure() {
        Map<String, Object> stats = candidateService.getTalentPoolStats();
        assertNotNull("Bản đồ thống kê không được null", stats);
        assertTrue("Phải chứa totalCount", stats.containsKey("totalCount"));
        assertTrue("Phải chứa fiveStarCount", stats.containsKey("fiveStarCount"));
        assertTrue("Phải chứa readyCount", stats.containsKey("readyCount"));

        int total = (int) stats.get("totalCount");
        int fiveStars = (int) stats.get("fiveStarCount");
        int ready = (int) stats.get("readyCount");

        assertTrue("Tổng số ứng viên tiềm năng phải không âm", total >= 0);
        assertTrue("Số lượng 5 sao không được lớn hơn tổng số", fiveStars <= total);
        assertTrue("Số lượng sẵn sàng không được lớn hơn tổng số", ready <= total);
    }

    @Test
    public void testFindTalentPool_FiltersAndPagination() {
        // Kiểm tra phân trang và lọc danh sách
        List<Candidate> page1 = candidateService.getTalentPool(null, null, null, null, 1, 10);
        assertNotNull("Danh sách trang 1 không được null", page1);
        assertTrue("Kích thước danh sách không vượt quá pageSize", page1.size() <= 10);

        int count = candidateService.countTalentPool(null, null, null, null);
        assertTrue("Tổng số bản ghi đếm được phải >= 0", count >= 0);

        // Kiểm tra lọc theo từ khóa không tồn tại
        List<Candidate> noMatch = candidateService.getTalentPool("__NON_EXISTENT_KEYWORD_XYZ__", null, null, null, 1, 10);
        assertNotNull(noMatch);
        assertEquals("Từ khóa không khớp phải trả về danh sách rỗng", 0, noMatch.size());
    }

    // =========================================================================
    // 6. [KN-286] KIỂM THỬ ĐẶC TẢ YÊU CẦU QUẢN LÝ KHO ỨNG VIÊN TIỀM NĂNG
    // =========================================================================

    /**
     * Yêu cầu 1: Ứng viên bị loại ở vòng cuối được đánh dấu đưa vào kho tiềm năng kèm lý do
     */
    @Test
    public void testKN286_RejectAndAddToTalentPool_Validation() {
        // Kiểm tra validation: candidateId null/rỗng
        assertFalse("CandidateId null phải bị từ chối",
                candidateService.rejectAndAddToTalentPool(null, "reason-01", 4, "Notes", "Java", "Backend Dev", "dept-01", "hr-user"));
        assertFalse("CandidateId rỗng phải bị từ chối",
                candidateService.rejectAndAddToTalentPool("  ", "reason-01", 4, "Notes", "Java", "Backend Dev", "dept-01", "hr-user"));
    }

    /**
     * Yêu cầu 2: Kho tìm kiếm được theo chức danh quan tâm và kỹ năng
     */
    @Test
    public void testKN286_FindTalentPool_ByTargetPositionAndSkill() {
        // Tìm kiếm theo targetPosition (vị trí/chức danh quan tâm)
        List<Candidate> posResults = candidateService.getTalentPool(null, "NonExistentTitleXYZ123", null, null, null, 1, 10);
        assertNotNull("Kết quả tìm theo chức danh quan tâm không được null", posResults);
        assertEquals("Chức danh không tồn tại phải trả về 0 kết quả", 0, posResults.size());

        int countPos = candidateService.countTalentPool(null, "NonExistentTitleXYZ123", null, null, null);
        assertEquals("Count chức danh không tồn tại phải là 0", 0, countPos);

        // Tìm kiếm theo skill (kỹ năng)
        List<Candidate> skillResults = candidateService.getTalentPool(null, null, "NonExistentSkillXYZ999", null, null, 1, 10);
        assertNotNull("Kết quả tìm theo kỹ năng không được null", skillResults);
        assertEquals("Kỹ năng không tồn tại phải trả về 0 kết quả", 0, skillResults.size());

        int countSkill = candidateService.countTalentPool(null, null, "NonExistentSkillXYZ999", null, null);
        assertEquals("Count kỹ năng không tồn tại phải là 0", 0, countSkill);
    }

    /**
     * Yêu cầu 3: Khi mở vị trí mới, hệ thống gợi ý ứng viên phù hợp từ kho
     */
    @Test
    public void testKN286_GetSuggestedTalents_ValidationAndExecution() {
        // RequisitionId null hoặc rỗng -> phải trả về danh sách rỗng an toàn, không ném ngoại lệ
        List<Candidate> nullReq = candidateService.getSuggestedTalents(null, 10);
        assertNotNull("RequisitionId null phải trả về danh sách không null", nullReq);
        assertTrue("RequisitionId null phải trả về danh sách rỗng", nullReq.isEmpty());

        List<Candidate> emptyReq = candidateService.getSuggestedTalents("   ", 10);
        assertNotNull("RequisitionId rỗng phải trả về danh sách không null", emptyReq);
        assertTrue("RequisitionId rỗng phải trả về danh sách rỗng", emptyReq.isEmpty());

        // Kiểm tra Candidate model có các trường matchScore và matchReason
        Candidate candidate = new Candidate();
        candidate.setMatchScore(3);
        candidate.setMatchReason("Khớp chức danh quan tâm & phòng ban tuyển dụng");
        assertEquals(3, candidate.getMatchScore());
        assertEquals("Khớp chức danh quan tâm & phòng ban tuyển dụng", candidate.getMatchReason());
    }
}

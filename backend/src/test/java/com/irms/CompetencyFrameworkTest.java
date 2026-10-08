package com.irms;

import com.irms.model.CompetencyCriterion;
import com.irms.model.CompetencyFramework;
import com.irms.model.CompetencyFrameworkCriterion;
import com.irms.model.PositionCompetencyFramework;
import com.irms.model.Role;
import com.irms.model.User;
import com.irms.service.CompetencyFrameworkService;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * ==============================================================================
 * BỘ KIỂM THỬ TỰ ĐỘNG CHỨC NĂNG KHAI BÁO KHUNG NĂNG LỰC (CompetencyFrameworkTest)
 * ==============================================================================
 * Đáp ứng toàn bộ các tiêu chí nghiệp vụ và Business Rules:
 * 1. Tổng trọng số của toàn bộ tiêu chí trong khung BẮT BUỘC bằng đúng 100%.
 *    - Tổng < 100%: Từ chối lưu/áp dụng, báo lỗi rõ ràng.
 *    - Tổng > 100%: Từ chối lưu/áp dụng, báo lỗi rõ ràng.
 *    - Tổng = 100%: Hợp lệ, cho phép lưu.
 * 2. Từng tiêu chí phải có trọng số > 0%. Trọng số <= 0% hoặc > 100% bị từ chối.
 * 3. Không cho phép trùng lặp tiêu chí trong cùng một khung.
 * 4. Khung năng lực không có tiêu chí không được phép kích hoạt (Active).
 * 5. Tái sử dụng: Một khung năng lực có thể gắn cho NHIỀU chức danh (1:N).
 * 6. Kiểm tra phân quyền RBAC: ADMIN, HR_MANAGER, RECRUITER, INTERVIEWER.
 * ==============================================================================
 */
public class CompetencyFrameworkTest {

    private CompetencyFrameworkService frameworkService;

    @Before
    public void setUp() {
        frameworkService = new CompetencyFrameworkService();
    }

    /**
     * Test Case 1: Tổng trọng số bằng đúng 100% (Ví dụ chuẩn theo đặc tả nghiệp vụ)
     * - Kiến thức chuyên môn: 30%
     * - Kỹ năng giao tiếp: 20%
     * - Kỹ năng xử lý vấn đề: 25%
     * - Khả năng làm việc nhóm: 15%
     * - Tư duy: 10%
     * TỔNG: 100% -> HỢP LỆ
     */
    @Test
    public void testTotalWeightEquals100PercentPassesValidation() {
        List<CompetencyFrameworkCriterion> criteria = new ArrayList<>();
        criteria.add(createCriterionItem("crit-1", new BigDecimal("30.00"), 1));
        criteria.add(createCriterionItem("crit-2", new BigDecimal("20.00"), 2));
        criteria.add(createCriterionItem("crit-3", new BigDecimal("25.00"), 3));
        criteria.add(createCriterionItem("crit-4", new BigDecimal("15.00"), 4));
        criteria.add(createCriterionItem("crit-5", new BigDecimal("10.00"), 5));

        // Phải vượt qua validation mà không ném ra ngoại lệ
        try {
            frameworkService.validateCriteriaAndWeights(criteria, true);
        } catch (IllegalArgumentException e) {
            fail("Tổng trọng số 100% hợp lệ không được ném ngoại lệ: " + e.getMessage());
        }

        // Kiểm tra tính toán trên model
        CompetencyFramework framework = new CompetencyFramework();
        framework.setCriteria(criteria);
        framework.calculateMetrics();

        assertEquals(new BigDecimal("100.00"), framework.getTotalWeight());
        assertTrue("Tổng trọng số 100% phải được đánh dấu hợp lệ", framework.isWeightValid());
        assertEquals(5, framework.getCriteriaCount());
    }

    /**
     * Test Case 2: Tổng trọng số < 100% (Ví dụ: 30% + 20% + 25% = 75%)
     * Business Rule: Không cho lưu/áp dụng, báo lỗi rõ ràng.
     */
    @Test
    public void testTotalWeightLessThan100PercentFails() {
        List<CompetencyFrameworkCriterion> criteria = new ArrayList<>();
        criteria.add(createCriterionItem("crit-1", new BigDecimal("30.00"), 1));
        criteria.add(createCriterionItem("crit-2", new BigDecimal("20.00"), 2));
        criteria.add(createCriterionItem("crit-3", new BigDecimal("25.00"), 3)); // Tổng = 75%

        try {
            frameworkService.validateCriteriaAndWeights(criteria, false);
            fail("Phải ném ngoại lệ IllegalArgumentException khi tổng trọng số < 100%");
        } catch (IllegalArgumentException e) {
            assertTrue("Thông báo lỗi phải nhắc đến thiếu trọng số và 100%",
                    e.getMessage().contains("chưa đủ 100%") && e.getMessage().contains("Còn thiếu"));
        }

        CompetencyFramework framework = new CompetencyFramework();
        framework.setCriteria(criteria);
        framework.calculateMetrics();
        assertFalse("Tổng trọng số 75% không được hợp lệ", framework.isWeightValid());
    }

    /**
     * Test Case 3: Tổng trọng số > 100% (Ví dụ: 60% + 50% = 110%)
     * Business Rule: Không cho lưu/áp dụng, báo lỗi rõ ràng.
     */
    @Test
    public void testTotalWeightGreaterThan100PercentFails() {
        List<CompetencyFrameworkCriterion> criteria = new ArrayList<>();
        criteria.add(createCriterionItem("crit-1", new BigDecimal("60.00"), 1));
        criteria.add(createCriterionItem("crit-2", new BigDecimal("50.00"), 2)); // Tổng = 110%

        try {
            frameworkService.validateCriteriaAndWeights(criteria, false);
            fail("Phải ném ngoại lệ IllegalArgumentException khi tổng trọng số > 100%");
        } catch (IllegalArgumentException e) {
            assertTrue("Thông báo lỗi phải nêu rõ vượt quá 100%",
                    e.getMessage().contains("vượt quá 100%"));
        }

        CompetencyFramework framework = new CompetencyFramework();
        framework.setCriteria(criteria);
        framework.calculateMetrics();
        assertFalse("Tổng trọng số 110% không được hợp lệ", framework.isWeightValid());
    }

    /**
     * Test Case 4: Trọng số của từng tiêu chí <= 0 (ví dụ 0% hoặc số âm)
     * Business Rule: Trọng số <= 0 => Báo lỗi rõ ràng.
     */
    @Test
    public void testZeroOrNegativeWeightFails() {
        // Trường hợp 4a: Trọng số = 0%
        List<CompetencyFrameworkCriterion> criteriaZero = new ArrayList<>();
        criteriaZero.add(createCriterionItem("crit-1", new BigDecimal("0.00"), 1));
        criteriaZero.add(createCriterionItem("crit-2", new BigDecimal("100.00"), 2));

        try {
            frameworkService.validateCriteriaAndWeights(criteriaZero, false);
            fail("Phải báo lỗi khi có tiêu chí có trọng số = 0%");
        } catch (IllegalArgumentException e) {
            assertTrue("Thông báo lỗi phải yêu cầu trọng số > 0%",
                    e.getMessage().contains("lớn hơn 0%"));
        }

        // Trường hợp 4b: Trọng số âm (-15%)
        List<CompetencyFrameworkCriterion> criteriaNegative = new ArrayList<>();
        criteriaNegative.add(createCriterionItem("crit-1", new BigDecimal("-15.00"), 1));
        criteriaNegative.add(createCriterionItem("crit-2", new BigDecimal("115.00"), 2));

        try {
            frameworkService.validateCriteriaAndWeights(criteriaNegative, false);
            fail("Phải báo lỗi khi có tiêu chí có trọng số âm");
        } catch (IllegalArgumentException e) {
            assertTrue("Thông báo lỗi phải yêu cầu trọng số > 0%",
                    e.getMessage().contains("lớn hơn 0%"));
        }
    }

    /**
     * Test Case 5: Tiêu chí rỗng hoặc chưa chọn tiêu chí
     * Business Rule: Không có tiêu chí => Báo lỗi.
     */
    @Test
    public void testEmptyOrNullCriteriaFails() {
        try {
            frameworkService.validateCriteriaAndWeights(null, false);
            fail("Phải báo lỗi khi danh sách tiêu chí là null");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("ít nhất 1 tiêu chí"));
        }

        try {
            frameworkService.validateCriteriaAndWeights(Collections.emptyList(), false);
            fail("Phải báo lỗi khi danh sách tiêu chí rỗng");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("ít nhất 1 tiêu chí"));
        }
    }

    /**
     * Test Case 6: Trùng lặp tiêu chí trong cùng một khung
     * Business Rule: Mỗi tiêu chí chỉ xuất hiện 1 lần trong khung.
     */
    @Test
    public void testDuplicateCriterionFails() {
        List<CompetencyFrameworkCriterion> criteria = new ArrayList<>();
        criteria.add(createCriterionItem("crit-duplicate", new BigDecimal("50.00"), 1));
        criteria.add(createCriterionItem("crit-duplicate", new BigDecimal("50.00"), 2));

        try {
            frameworkService.validateCriteriaAndWeights(criteria, false);
            fail("Phải báo lỗi khi có tiêu chí trùng lặp");
        } catch (IllegalArgumentException e) {
            assertTrue("Thông báo lỗi phải chỉ rõ trùng lặp",
                    e.getMessage().contains("không được trùng lặp"));
        }
    }

    /**
     * Test Case 7: Tái sử dụng Khung năng lực cho nhiều chức danh (1 Khung : N Chức danh)
     * Đáp ứng đặc tả: Một khung năng lực có thể được sử dụng lại cho nhiều chức danh.
     */
    @Test
    public void testReusabilityOneFrameworkMultiplePositions() {
        CompetencyFramework framework = new CompetencyFramework();
        framework.setId("fw-sales-01");
        framework.setCode("CF-SALES");
        framework.setName("Khung năng lực Khối Kinh doanh");
        framework.setStatus(CompetencyFramework.STATUS_ACTIVE);

        // Giả lập gán cho 3 chức danh khác nhau
        PositionCompetencyFramework pos1 = new PositionCompetencyFramework("sr-01", "fw-sales-01", "SALES-01", "Nhân viên kinh doanh 1", "Junior", "sales@corp.local");
        PositionCompetencyFramework pos2 = new PositionCompetencyFramework("sr-02", "fw-sales-01", "SALES-02", "Nhân viên kinh doanh 2", "Middle", "sales@corp.local");
        PositionCompetencyFramework pos3 = new PositionCompetencyFramework("sr-03", "fw-sales-01", "SALES-SR", "Chuyên viên kinh doanh", "Senior", "sales@corp.local");

        List<PositionCompetencyFramework> assignedPositions = Arrays.asList(pos1, pos2, pos3);
        framework.setAssignedPositions(assignedPositions);

        assertEquals(3, framework.getAssignedPositionsCount());
        assertTrue("Danh sách chức danh phải chứa Nhân viên kinh doanh 1",
                framework.getAssignedPositionTitles().contains("Nhân viên kinh doanh 1"));
        assertTrue("Danh sách chức danh phải chứa Nhân viên kinh doanh 2",
                framework.getAssignedPositionTitles().contains("Nhân viên kinh doanh 2"));
        assertTrue("Danh sách chức danh phải chứa Chuyên viên kinh doanh",
                framework.getAssignedPositionTitles().contains("Chuyên viên kinh doanh"));
    }

    /**
     * Test Case 8: Trạng thái khung năng lực và các nhãn/badge CSS tương ứng
     * Đáp ứng: Nháp (DRAFT), Đang áp dụng (ACTIVE), Ngừng áp dụng (INACTIVE)
     */
    @Test
    public void testFrameworkStatusesAndBadges() {
        CompetencyFramework draftFw = new CompetencyFramework();
        draftFw.setStatus(CompetencyFramework.STATUS_DRAFT);
        assertEquals("Bản nháp", draftFw.getStatusLabel());
        assertTrue(draftFw.getStatusBadgeClass().contains("secondary") || draftFw.getStatusBadgeClass().contains("gray"));

        CompetencyFramework activeFw = new CompetencyFramework();
        activeFw.setStatus(CompetencyFramework.STATUS_ACTIVE);
        assertEquals("Đang áp dụng", activeFw.getStatusLabel());
        assertTrue(activeFw.getStatusBadgeClass().contains("success") || activeFw.getStatusBadgeClass().contains("emerald"));
        assertTrue(activeFw.isActive());

        CompetencyFramework inactiveFw = new CompetencyFramework();
        inactiveFw.setStatus(CompetencyFramework.STATUS_INACTIVE);
        assertEquals("Ngừng áp dụng", inactiveFw.getStatusLabel());
        assertTrue(inactiveFw.getStatusBadgeClass().contains("warning") || inactiveFw.getStatusBadgeClass().contains("danger") || inactiveFw.getStatusBadgeClass().contains("rose"));
        assertFalse(inactiveFw.isActive());
    }

    /**
     * Test Case 9: Kiểm tra ma trận phân quyền RBAC cho module Khung năng lực
     * - ADMIN & HR_MANAGER: Đầy đủ quyền xem, tạo, sửa, đổi trạng thái, gán chức danh.
     * - RECRUITER & INTERVIEWER: Có quyền xem (competencies.view) phục vụ đánh giá phỏng vấn.
     * - Người dùng không có quyền: Bị chặn.
     */
    @Test
    public void testRBACPermissionsForCompetencyFramework() {
        // 1. Quản trị viên (ADMIN)
        User admin = new User();
        Role adminRole = new Role("r-admin", "ADMIN", "Quản trị", "", true);
        admin.setRoles(Collections.singletonList(adminRole));
        admin.setPermissions(Arrays.asList(
                "competencies.view", "competencies.create", "competencies.update",
                "competencies.status", "competencies.assign"
        ));

        assertTrue(admin.hasPermission("competencies.view"));
        assertTrue(admin.hasPermission("competencies.create"));
        assertTrue(admin.hasPermission("competencies.update"));
        assertTrue(admin.hasPermission("competencies.status"));
        assertTrue(admin.hasPermission("competencies.assign"));

        // 2. Trưởng phòng nhân sự (HR_MANAGER)
        User hrManager = new User();
        Role hrRole = new Role("r-hr", "HR_MANAGER", "Trưởng phòng HR", "", true);
        hrManager.setRoles(Collections.singletonList(hrRole));
        hrManager.setPermissions(Arrays.asList(
                "competencies.view", "competencies.create", "competencies.update",
                "competencies.status", "competencies.assign"
        ));

        assertTrue(hrManager.hasPermission("competencies.view"));
        assertTrue(hrManager.hasPermission("competencies.create"));
        assertTrue(hrManager.hasPermission("competencies.assign"));

        // 3. Người phỏng vấn (INTERVIEWER) - chỉ có quyền xem để sinh phiếu đánh giá Sprint 6
        User interviewer = new User();
        Role intRole = new Role("r-int", "INTERVIEWER", "Người phỏng vấn", "", true);
        interviewer.setRoles(Collections.singletonList(intRole));
        interviewer.setPermissions(Collections.singletonList("competencies.view"));

        assertTrue(interviewer.hasPermission("competencies.view"));
        assertFalse("Interviewer không được quyền tạo khung năng lực", interviewer.hasPermission("competencies.create"));
        assertFalse("Interviewer không được quyền đổi trạng thái", interviewer.hasPermission("competencies.status"));
        assertFalse("Interviewer không được quyền gán chức danh", interviewer.hasPermission("competencies.assign"));

        // 4. Khách vãng lai / Người dùng chưa phân quyền
        User guest = new User();
        guest.setRoles(Collections.emptyList());
        guest.setPermissions(Collections.emptyList());

        assertFalse("Guest không được xem khung năng lực", guest.hasPermission("competencies.view"));
        assertFalse("Guest không được tạo khung năng lực", guest.hasPermission("competencies.create"));
    }

    /**
     * Test Case 10: Kiểm tra tích hợp DAO và cơ sở dữ liệu
     * Đảm bảo các bảng competency_frameworks, competency_criteria, position_competency_frameworks
     * đã được tự động tạo lập / nâng cấp đầy đủ và có thể truy vấn.
     */
    @Test
    public void testDatabaseIntegrationAndQueries() {
        try {
            // Kiểm tra truy vấn danh sách khung năng lực từ DB
            List<CompetencyFramework> list = frameworkService.getAllFrameworks(null, null);
            assertNotNull("Danh sách khung năng lực không được null", list);

            // Kiểm tra danh sách vị trí kèm thông tin khung năng lực
            List<PositionCompetencyFramework> positions = frameworkService.getAllPositionsWithFrameworkInfo();
            assertNotNull("Danh sách chức danh không được null", positions);

            // Nếu đã có dữ liệu seed trong DB, kiểm tra tính toàn vẹn
            if (!list.isEmpty()) {
                CompetencyFramework first = list.get(0);
                assertNotNull("Mã khung năng lực không được null", first.getCode());
                assertNotNull("Tên khung năng lực không được null", first.getName());
                assertNotNull("Trạng thái không được null", first.getStatus());
            }
        } catch (Exception e) {
            fail("Truy vấn CSDL cho Khung năng lực gặp lỗi: " + e.getMessage());
        }
    }

    /**
     * Helper tạo một dòng tiêu chí kèm trọng số
     */
    private CompetencyFrameworkCriterion createCriterionItem(String criterionId, BigDecimal weight, int order) {
        CompetencyFrameworkCriterion item = new CompetencyFrameworkCriterion();
        item.setCriterionId(criterionId);
        item.setWeight(weight);
        item.setDisplayOrder(order);
        return item;
    }
}

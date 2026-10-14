package com.irms;

import com.irms.config.DBConnection;
import com.irms.dao.HeadcountBudgetDAO;
import com.irms.model.BudgetSubtask;
import com.irms.model.DepartmentHeadcountBudget;
import com.irms.model.RecruitmentRequest;
import com.irms.service.HeadcountBudgetService;
import com.irms.service.HeadcountBudgetService.QuotaCheckResult;
import com.irms.service.RecruitmentRequestService;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;

public class HeadcountBudgetTest {

    private HeadcountBudgetDAO budgetDAO;
    private HeadcountBudgetService budgetService;
    private RecruitmentRequestService requisitionService;

    @Before
    public void setUp() {
        budgetDAO = new HeadcountBudgetDAO();
        budgetService = new HeadcountBudgetService();
        requisitionService = new RecruitmentRequestService();
    }

    @Test
    public void testTablesAndColumnsExist() throws Exception {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Kiểm tra bảng department_headcount_budgets
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM department_headcount_budgets")) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) >= 0);
            }

            // Kiểm tra bảng budget_subtasks
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM budget_subtasks")) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) >= 7); // Ít nhất 7 subtasks KN-195 -> KN-201
            }

            // Kiểm tra các cột xác nhận ghi đè trong recruitment_requisitions
            try (ResultSet rs = stmt.executeQuery("SELECT is_budget_overridden, budget_override_reason, budget_overridden_by, budget_overridden_at FROM recruitment_requisitions LIMIT 1")) {
                assertNotNull(rs);
            }
        }
    }

    @Test
    public void testSubtasksKN195ToKN201Exist() {
        List<BudgetSubtask> subtasks = budgetService.getAllSubtasks();
        assertNotNull(subtasks);
        assertTrue(subtasks.size() >= 7);

        String[] expectedCodes = {"KN-195", "KN-196", "KN-197", "KN-198", "KN-199", "KN-200", "KN-201"};
        for (String code : expectedCodes) {
            boolean found = subtasks.stream().anyMatch(s -> code.equalsIgnoreCase(s.getCode()));
            assertTrue("Subtask " + code + " phải tồn tại trong CSDL!", found);
        }
    }

    @Test
    public void testBudgetCrudAndUniqueConstraint() {
        String testDeptId = "dept-005"; // MKT
        int testYear = 2030;

        // Xóa sạch nếu đã có trước đó
        DepartmentHeadcountBudget existing = budgetDAO.getBudgetByDeptAndYear(testDeptId, testYear, null);
        if (existing != null) {
            budgetDAO.delete(existing.getId());
        }

        // 1. Tạo mới định biên
        DepartmentHeadcountBudget budget = new DepartmentHeadcountBudget();
        budget.setDepartmentId(testDeptId);
        budget.setFiscalYear(testYear);
        budget.setTargetHeadcount(10);
        budget.setTotalSalaryBudget(new BigDecimal("300000000.00"));
        budget.setDescription("Định biên thử nghiệm năm 2030");
        budget.setStatus("ACTIVE");

        boolean created = budgetService.saveBudget(budget, "usr-001", "127.0.0.1", "JUnit");
        assertTrue("Tạo mới định biên phải thành công", created);
        assertNotNull("ID phải được tự động sinh", budget.getId());

        // 2. Tra cứu lại
        DepartmentHeadcountBudget loaded = budgetService.getBudgetById(budget.getId());
        assertNotNull("Bản ghi vừa tạo phải tra cứu được", loaded);
        assertEquals(10, loaded.getTargetHeadcount());
        assertEquals(new BigDecimal("300000000.00"), loaded.getTotalSalaryBudget());

        // 3. Kiểm tra chống tạo bản ghi trùng cho cùng phòng ban và năm
        DepartmentHeadcountBudget duplicate = new DepartmentHeadcountBudget();
        duplicate.setDepartmentId(testDeptId);
        duplicate.setFiscalYear(testYear);
        duplicate.setTargetHeadcount(20);

        try {
            budgetService.saveBudget(duplicate, "usr-001", "127.0.0.1", "JUnit");
            fail("Phải ném ngoại lệ khi tạo bản ghi định biên trùng phòng ban và năm!");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("đã được khai báo định biên"));
        }

        // 4. Cập nhật bản ghi
        loaded.setTargetHeadcount(12);
        loaded.setTotalSalaryBudget(new BigDecimal("350000000.00"));
        boolean updated = budgetService.saveBudget(loaded, "usr-001", "127.0.0.1", "JUnit");
        assertTrue("Cập nhật định biên phải thành công", updated);

        DepartmentHeadcountBudget updatedLoaded = budgetService.getBudgetById(loaded.getId());
        assertEquals(12, updatedLoaded.getTargetHeadcount());

        // 5. Dọn dẹp
        boolean deleted = budgetService.deleteBudget(loaded.getId(), "usr-001", "127.0.0.1", "JUnit");
        assertTrue("Xóa định biên thử nghiệm phải thành công", deleted);
    }

    @Test
    public void testRealtimeQuotaCheckCalculations() {
        // Kiểm tra phòng ban IT (dept-003) năm 2026 đã seed định biên 15 HC, 650.000.000 VND
        DepartmentHeadcountBudget itBudget = budgetDAO.getBudgetByDeptAndYear("dept-003", 2026, null);
        assertNotNull("Phòng ban TECH năm 2026 phải có định biên", itBudget);
        assertEquals(15, itBudget.getTargetHeadcount());
        assertTrue("Used headcount phải >= 0", itBudget.getUsedHeadcount() >= 0);
        assertEquals("Headcount còn lại = chỉ tiêu - đã dùng", 
                itBudget.getTargetHeadcount() - itBudget.getUsedHeadcount(), 
                itBudget.getRemainingHeadcount());

        // Test 1: Kiểm tra yêu cầu trong hạn mức
        QuotaCheckResult okResult = budgetService.checkRequisitionQuota("dept-003", 2026, 1, new BigDecimal("25000000"), null);
        assertTrue(okResult.isConfigured());
        assertFalse("1 headcount trong hạn mức không được coi là vượt", okResult.isHeadcountExceeded());

        // Test 2: Kiểm tra yêu cầu vượt quá Headcount còn lại
        int excessiveHc = itBudget.getRemainingHeadcount() + 10;
        QuotaCheckResult hcExcessResult = budgetService.checkRequisitionQuota("dept-003", 2026, excessiveHc, new BigDecimal("25000000"), null);
        assertTrue(hcExcessResult.isConfigured());
        assertTrue("Phải cảnh báo vượt headcount", hcExcessResult.isHeadcountExceeded());
        assertTrue("Phải đánh dấu isExceeded = true", hcExcessResult.isExceeded());
        assertEquals(10, hcExcessResult.getHeadcountExcess());

        // Test 3: Kiểm tra yêu cầu vượt Ngân sách còn lại
        BigDecimal excessiveSalary = new BigDecimal("2000000000.00"); // 2 tỷ
        QuotaCheckResult bgExcessResult = budgetService.checkRequisitionQuota("dept-003", 2026, 1, excessiveSalary, null);
        assertTrue(bgExcessResult.isConfigured());
        assertTrue("Phải cảnh báo vượt ngân sách", bgExcessResult.isBudgetExceeded());
        assertTrue(bgExcessResult.isExceeded());

        // Test 4: Kiểm tra phòng ban chưa khai báo định biên
        QuotaCheckResult unconfigResult = budgetService.checkRequisitionQuota("dept-001", 2099, 1, new BigDecimal("20000000"), null);
        assertFalse("Chưa khai báo thì isConfigured = false", unconfigResult.isConfigured());
        assertTrue("Chưa khai báo phải là cảnh báo chặn (isExceeded = true)", unconfigResult.isExceeded());
        assertTrue(unconfigResult.getMessage().contains("chưa được khai báo chỉ tiêu"));
    }

    @Test
    public void testRequisitionOverridePermissionCheck() {
        String testReqId = "test-req-" + UUID.randomUUID();
        RecruitmentRequest req = new RecruitmentRequest();
        req.setId(testReqId);
        req.setCode("TEST-REQ-" + System.currentTimeMillis() % 100000);
        req.setTitle("Yêu cầu thử nghiệm kiểm tra ghi đè");
        req.setPositionTitle("Kỹ sư Kiểm thử QA");
        req.setDepartmentId("dept-003"); // TECH
        req.setHeadcount(999); // Chắc chắn vượt headcount
        req.setRecruitmentReason("NEW_HEADCOUNT");
        req.setMinSalary(new BigDecimal("30000000"));
        req.setMaxSalary(new BigDecimal("40000000"));
        req.setDeadline(Date.valueOf(LocalDate.now().plusMonths(1)));
        req.setJobDescription("Mô tả công việc kiểm thử tự động");
        req.setJobRequirements("Yêu cầu kinh nghiệm kiểm thử");

        // Test 1: Vượt chỉ tiêu nhưng KHÔNG chọn ghi đè -> Phải ném ngoại lệ cảnh báo chặn
        req.setBudgetOverridden(false);
        try {
            requisitionService.validateBudgetAndHeadcount(req, "usr-002");
            fail("Phải chặn khi vượt chỉ tiêu mà không có xác nhận ghi đè!");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("CẢNH BÁO CHẶN"));
        }

        // Test 2: Chọn ghi đè nhưng người thực hiện KHÔNG phải Trưởng phòng Nhân sự / Admin (ví dụ usr-003 là RECRUITER)
        req.setBudgetOverridden(true);
        req.setBudgetOverrideReason("Cần tuyển gấp phục vụ dự án trọng điểm");
        try {
            requisitionService.validateBudgetAndHeadcount(req, "usr-003"); // usr-003 là RECRUITER
            fail("Chuyên viên tuyển dụng không được phép tự xác nhận ghi đè!");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("BẠN KHÔNG CÓ THẨM QUYỀN"));
        }

        // Test 3: Trưởng phòng Nhân sự (usr-002) xác nhận nhưng QUÊN nhập lý do
        req.setBudgetOverridden(true);
        req.setBudgetOverrideReason("");
        try {
            requisitionService.validateBudgetAndHeadcount(req, "usr-002");
            fail("Bắt buộc phải nhập lý do khi ghi đè!");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Bắt buộc nhập lý do"));
        }

        // Test 4: Trưởng phòng Nhân sự (usr-002) xác nhận hợp lệ kèm lý do
        req.setBudgetOverridden(true);
        req.setBudgetOverrideReason("Dự án chiến lược được Tổng giám đốc phê duyệt bổ sung ngân sách.");
        requisitionService.validateBudgetAndHeadcount(req, "usr-002");
        assertTrue("Xác nhận ghi đè thành công", req.isBudgetOverridden());
        assertEquals("usr-002", req.getBudgetOverriddenBy());
        assertNotNull(req.getBudgetOverriddenAt());
    }

    @Test
    public void testCompanyKpiSummary() {
        Map<String, Object> kpis = budgetService.getCompanyKpiSummary(2026);
        assertNotNull(kpis);
        assertEquals(2026, kpis.get("fiscalYear"));
        assertTrue((Integer) kpis.get("totalDepartmentsWithBudget") > 0);
        assertTrue((Integer) kpis.get("totalTargetHeadcount") > 0);
        assertTrue(((BigDecimal) kpis.get("totalApprovedBudget")).compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    public void testSubtaskStatusUpdateAndCompletionRate() {
        List<BudgetSubtask> subtasks = budgetService.getAllSubtasks();
        assertNotNull(subtasks);
        assertFalse(subtasks.isEmpty());
        BudgetSubtask first = subtasks.get(0);
        String origStatus = first.getStatus();

        boolean updated = budgetService.updateSubtaskStatus(first.getId(), "DONE", "usr-001", "127.0.0.1", "JUnit");
        assertTrue("Cập nhật trạng thái Subtask thành DONE phải thành công", updated);

        double rateAfterDone = budgetService.getSubtasksCompletionRate();
        assertTrue("Tỷ lệ hoàn thành phải > 0%", rateAfterDone > 0.0);

        budgetService.updateSubtaskStatus(first.getId(), origStatus != null ? origStatus : "TO_DO", "usr-001", "127.0.0.1", "JUnit");
    }
}

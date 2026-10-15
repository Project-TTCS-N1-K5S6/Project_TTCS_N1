package com.irms;

import com.irms.model.*;
import com.irms.service.ApprovalMatrixService;
import com.irms.service.RecruitmentRequestService;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * ==============================================================================
 * BỘ KIỂM THỬ TOÀN DIỆN MA TRẬN PHÊ DUYỆT (ApprovalMatrixTest)
 * ==============================================================================
 * Đáp ứng đầy đủ các tiêu chí hoàn thành của Prompt Antigravity:
 * 1. Khai báo chuỗi cấp phê duyệt theo phòng ban, mức lương.
 * 2. Tự động xác định và bổ sung cấp phê duyệt cao hơn khi vượt hạn mức.
 * 3. Chặn lưu cấu hình sai (thiếu cấp, lương sai, người duyệt sai, thứ tự sai).
 * 4. Bảo toàn luồng đang chạy (Snapshot) khi cấu hình ma trận thay đổi.
 * ==============================================================================
 */
public class ApprovalMatrixTest {

    private ApprovalMatrixService matrixService;
    private RecruitmentRequestService requisitionService;

    @Before
    public void setUp() {
        matrixService = new ApprovalMatrixService();
        requisitionService = new RecruitmentRequestService();
    }

    /**
     * Tiêu chí 1: Tạo cấu hình Ma trận phê duyệt hợp lệ với đầy đủ cấp và điều kiện
     */
    @Test
    public void testCreateValidApprovalMatrix() {
        ApprovalMatrix matrix = new ApprovalMatrix();
        matrix.setCode("APM-TEST-VALID-" + System.currentTimeMillis() % 10000);
        matrix.setName("Ma trận kiểm thử tự động");
        matrix.setDepartmentId("dept-003"); // Khối CNTT
        matrix.setSalaryCondition("RANGE");
        matrix.setMinSalary(new BigDecimal("10000000"));
        matrix.setMaxSalary(new BigDecimal("50000000"));
        matrix.setThresholdAmount(new BigDecimal("30000000"));
        matrix.setPriority(10);
        matrix.setStatus("ACTIVE");

        List<ApprovalMatrixStep> steps = new ArrayList<>();
        steps.add(new ApprovalMatrixStep(1, "Trưởng bộ phận", "DEPT_MANAGER", null, null, false, null));
        steps.add(new ApprovalMatrixStep(2, "Giám đốc Khối", "ROLE", "role-001", null, true, "Bổ sung khi vượt 30M"));
        matrix.setSteps(steps);

        boolean created = matrixService.createMatrix(matrix, "usr-001", "127.0.0.1", "JUnit-Test");
        assertTrue("Tạo cấu hình hợp lệ phải thành công", created);

        ApprovalMatrix fetched = matrixService.getMatrixById(matrix.getId());
        assertNotNull("Phải tìm thấy ma trận vừa tạo trong DB", fetched);
        assertEquals("Mã cấu hình phải khớp", matrix.getCode(), fetched.getCode());
        assertEquals("Số cấp phê duyệt phải là 2", 2, fetched.getSteps().size());

        // Dọn dẹp
        matrixService.deleteMatrix(matrix.getId(), "usr-001", "127.0.0.1", "JUnit-Test");
    }

    /**
     * Tiêu chí 2: Chặn lưu cấu hình không có cấp phê duyệt nào
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRejectMatrixWithoutSteps() {
        ApprovalMatrix matrix = new ApprovalMatrix();
        matrix.setCode("APM-NO-STEPS-" + System.currentTimeMillis() % 10000);
        matrix.setName("Ma trận thiếu cấp");
        matrix.setSteps(new ArrayList<>()); // Rỗng

        matrixService.createMatrix(matrix, "usr-001", "127.0.0.1", "JUnit-Test");
    }

    /**
     * Tiêu chí 3: Chặn lưu cấu hình có khoảng lương không hợp lệ (minSalary > maxSalary)
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRejectInvalidSalaryRange() {
        ApprovalMatrix matrix = new ApprovalMatrix();
        matrix.setCode("APM-INV-SAL-" + System.currentTimeMillis() % 10000);
        matrix.setName("Ma trận lương sai");
        matrix.setMinSalary(new BigDecimal("50000000"));
        matrix.setMaxSalary(new BigDecimal("20000000")); // Nhỏ hơn minSalary

        List<ApprovalMatrixStep> steps = new ArrayList<>();
        steps.add(new ApprovalMatrixStep(1, "Trưởng phòng", "DEPT_MANAGER", null, null, false, null));
        matrix.setSteps(steps);

        matrixService.createMatrix(matrix, "usr-001", "127.0.0.1", "JUnit-Test");
    }

    /**
     * Tiêu chí 4: Chặn lưu cấu hình có lương âm hoặc ngưỡng âm
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRejectNegativeSalary() {
        ApprovalMatrix matrix = new ApprovalMatrix();
        matrix.setCode("APM-NEG-SAL-" + System.currentTimeMillis() % 10000);
        matrix.setName("Ma trận lương âm");
        matrix.setMinSalary(new BigDecimal("-1000000"));

        List<ApprovalMatrixStep> steps = new ArrayList<>();
        steps.add(new ApprovalMatrixStep(1, "Trưởng phòng", "DEPT_MANAGER", null, null, false, null));
        matrix.setSteps(steps);

        matrixService.createMatrix(matrix, "usr-001", "127.0.0.1", "JUnit-Test");
    }

    /**
     * Tiêu chí 5 [TRỌNG TÂM]: Xác định đúng chuỗi phê duyệt thông thường khi mức lương TRONG HẠN MỨC
     * Ví dụ Khối CNTT (dept-003): Ngưỡng 35M. Lương đề xuất 25M -> Chỉ kích hoạt Cấp 1.
     */
    @Test
    public void testResolveChainWithinThreshold() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setId("test-req-normal");
        req.setCode("REQ-TEST-001");
        req.setDepartmentId("dept-003"); // Khối CNTT
        req.setPositionTitle("Lập trình viên Backend");
        req.setMinSalary(new BigDecimal("20000000"));
        req.setMaxSalary(new BigDecimal("25000000")); // 25M <= 35M ngưỡng

        List<RequisitionApprovalStep> steps = matrixService.resolveApprovalSteps(req);
        assertNotNull("Chuỗi phê duyệt không được null", steps);
        assertFalse("Chuỗi phê duyệt không được rỗng", steps.isEmpty());

        // Khi lương trong hạn mức, cấp bổ sung vượt hạn mức KHÔNG được kích hoạt
        assertEquals("Chuỗi thông thường chỉ có 1 cấp (Trưởng bộ phận)", 1, steps.size());
        assertEquals("Cấp 1 phải có step_order là 1", 1, steps.get(0).getStepOrder());
        assertEquals("Trưởng bộ phận Kỹ thuật", steps.get(0).getStepName());
    }

    /**
     * Tiêu chí 6 [TRỌNG TÂM]: TỰ ĐỘNG BỔ SUNG CẤP CAO HƠN KHI VƯỢT HẠN MỨC
     * Ví dụ Khối CNTT: Ngưỡng 35M. Lương đề xuất 45M (> 35M) -> Tự động bổ sung Cấp 2: Giám đốc Khối CNTT!
     */
    @Test
    public void testAutoEscalateWhenSalaryExceedsThreshold() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setId("test-req-over");
        req.setCode("REQ-TEST-002");
        req.setDepartmentId("dept-003"); // Khối CNTT
        req.setPositionTitle("Chuyên gia Kiến trúc phần mềm");
        req.setMinSalary(new BigDecimal("35000000"));
        req.setMaxSalary(new BigDecimal("45000000")); // 45M > 35M ngưỡng vượt

        List<RequisitionApprovalStep> steps = matrixService.resolveApprovalSteps(req);
        assertNotNull("Chuỗi phê duyệt không được null", steps);

        // Phải có đủ 2 cấp: Cấp 1 (Trưởng bộ phận) VÀ Cấp 2 tự động bổ sung (Giám đốc Khối)
        assertEquals("Chuỗi vượt hạn mức phải tự động bổ sung thành 2 cấp", 2, steps.size());
        assertEquals("Thứ tự cấp 1", 1, steps.get(0).getStepOrder());
        assertEquals("Thứ tự cấp 2", 2, steps.get(1).getStepOrder());
        assertTrue("Cấp 2 phải là cấp bổ sung do vượt hạn mức", 
                steps.get(1).getStepName().contains("Giám đốc") || steps.get(1).getStepName().contains("vượt"));
    }

    /**
     * Tiêu chí 7: Tính liên tục của thứ tự cấp và không tạo cấp trùng lặp
     */
    @Test
    public void testStepOrderSequentialAndNoDuplicates() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setId("test-req-seq");
        req.setDepartmentId("dept-004"); // Khối Kinh doanh
        req.setPositionTitle("Trưởng phòng Kinh doanh");
        req.setMaxSalary(new BigDecimal("40000000")); // Vượt ngưỡng 30M

        List<RequisitionApprovalStep> steps = matrixService.resolveApprovalSteps(req);
        for (int i = 0; i < steps.size(); i++) {
            assertEquals("Thứ tự cấp phải liên tục từ 1..N", i + 1, steps.get(i).getStepOrder());
            for (int j = i + 1; j < steps.size(); j++) {
                assertNotEquals("Không được trùng lặp số thứ tự cấp", 
                        steps.get(i).getStepOrder(), steps.get(j).getStepOrder());
            }
        }
    }

    /**
     * Tiêu chí 8 [BẢO TOÀN LUỒNG ĐANG CHẠY]: Thay đổi cấu hình Ma trận KHÔNG làm thay đổi luồng snapshot đã chạy
     */
    @Test
    public void testRunningWorkflowSnapshotImmutability() {
        // 1. Tạo một yêu cầu tuyển dụng và lưu snapshot chuỗi phê duyệt
        RecruitmentRequest req = new RecruitmentRequest();
        req.setId("test-snapshot-req-" + System.currentTimeMillis() % 10000);
        req.setCode("REQ-SNAP-01");
        req.setTitle("Yêu cầu test snapshot");
        req.setPositionTitle("Kỹ sư phần mềm");
        req.setDepartmentId("dept-003");
        req.setMaxSalary(new BigDecimal("20000000")); // 20M: 1 cấp

        boolean saved = matrixService.saveRequisitionApprovalChain(req);
        assertTrue("Lưu snapshot chuỗi phê duyệt phải thành công", saved);

        List<RequisitionApprovalStep> initialSteps = matrixService.getRequisitionApprovalSteps(req.getId());
        assertEquals("Ban đầu có 1 cấp", 1, initialSteps.size());
        String initialStepId = initialSteps.get(0).getId();

        // 2. Người quản trị chỉnh sửa hoặc tạo ma trận mới trong hệ thống
        ApprovalMatrix newMatrix = new ApprovalMatrix();
        newMatrix.setCode("APM-TEMP-" + System.currentTimeMillis() % 10000);
        newMatrix.setName("Ma trận sửa đổi tạm");
        newMatrix.setDepartmentId("dept-003");
        List<ApprovalMatrixStep> newSteps = new ArrayList<>();
        newSteps.add(new ApprovalMatrixStep(1, "Cấp mới 1", "DEPT_MANAGER", null, null, false, null));
        newSteps.add(new ApprovalMatrixStep(2, "Cấp mới 2", "ROLE", "role-001", null, false, null));
        newSteps.add(new ApprovalMatrixStep(3, "Cấp mới 3", "ROLE", "role-002", null, false, null));
        newMatrix.setSteps(newSteps);
        matrixService.createMatrix(newMatrix, "usr-001", "127.0.0.1", "JUnit-Test");

        // 3. Kiểm tra lại chuỗi phê duyệt của yêu cầu đang chạy -> Phải KHÔNG ĐỔI!
        List<RequisitionApprovalStep> stepsAfterMatrixChange = matrixService.getRequisitionApprovalSteps(req.getId());
        assertEquals("Chuỗi đã snapshot không được phép thay đổi số cấp", 1, stepsAfterMatrixChange.size());
        assertEquals("ID bước đã snapshot phải giữ nguyên vẹn", initialStepId, stepsAfterMatrixChange.get(0).getId());

        // Dọn dẹp
        matrixService.deleteMatrix(newMatrix.getId(), "usr-001", "127.0.0.1", "JUnit-Test");
    }

    /**
     * Tiêu chí 9: Chặn lưu cấu hình khi vai trò của người duyệt không hợp lệ
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRejectInvalidApproverRole() {
        ApprovalMatrix matrix = new ApprovalMatrix();
        matrix.setCode("APM-INV-ROLE-" + System.currentTimeMillis() % 10000);
        matrix.setName("Ma trận vai trò ma");

        List<ApprovalMatrixStep> steps = new ArrayList<>();
        steps.add(new ApprovalMatrixStep(1, "Cấp 1", "ROLE", "role-non-existent-9999", null, false, null));
        matrix.setSteps(steps);

        matrixService.createMatrix(matrix, "usr-001", "127.0.0.1", "JUnit-Test");
    }

    /**
     * Tiêu chí 10: Xử lý phê duyệt tuần tự và hoàn tất quy trình
     */
    @Test
    public void testApproveStepFlow() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setId("test-flow-" + System.currentTimeMillis() % 10000);
        req.setDepartmentId("dept-003");
        req.setMaxSalary(new BigDecimal("20000000"));

        matrixService.saveRequisitionApprovalChain(req);
        List<RequisitionApprovalStep> steps = matrixService.getRequisitionApprovalSteps(req.getId());
        assertFalse("Phải có bước phê duyệt", steps.isEmpty());

        User adminUser = new User();
        adminUser.setId("usr-001");
        Role adminRole = new Role();
        adminRole.setCode("ADMIN");
        adminUser.getRoles().add(adminRole);

        // Phê duyệt cấp 1
        boolean allDone = matrixService.approveRequisitionStep(req.getId(), steps.get(0).getId(), "Đồng ý phê duyệt", adminUser, "127.0.0.1", "JUnit");
        assertTrue("Vì yêu cầu chỉ có 1 cấp nên allDone phải là true", allDone);

        List<RequisitionApprovalStep> updated = matrixService.getRequisitionApprovalSteps(req.getId());
        assertEquals("Trạng thái bước phải là APPROVED", "APPROVED", updated.get(0).getStatus());
    }

    /**
     * Tiêu chí 11: Cấu hình sai (không có cấp duyệt nào khớp) bị chặn lưu
     * Khi mức lương đề xuất nằm ngoài mọi ma trận phê duyệt đã cấu hình của phòng ban, yêu cầu phải bị chặn lưu.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRejectWhenNoApprovalMatrixMatchesSalaryRange() {
        RecruitmentRequest req = new RecruitmentRequest();
        req.setId("test-no-match-sal");
        req.setDepartmentId("dept-003"); // Khối CNTT (max trong seed là 70M)
        req.setMaxSalary(new BigDecimal("150000000")); // 150M vượt ngoài dải cấu hình tối đa
        req.setPositionTitle("Giám đốc Chuyển đổi số");

        // Phải bị chặn ném IllegalArgumentException
        matrixService.resolveApprovalSteps(req);
    }
}

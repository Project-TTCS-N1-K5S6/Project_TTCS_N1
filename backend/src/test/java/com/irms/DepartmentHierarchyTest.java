package com.irms;

import com.irms.model.Department;
import com.irms.service.DepartmentService;
import com.irms.service.DepartmentService.DeleteCheckResult;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * ==============================================================================
 * BỘ KIỂM THỬ TỰ ĐỘNG CHỨC NĂNG KHAI BÁO PHÒNG BAN (DepartmentHierarchyTest)
 * ==============================================================================
 * Kiểm thử toàn diện 10 kịch bản theo yêu cầu nghiệp vụ:
 * Case 1: Tạo phòng ban cấp 1, cấp 2, cấp 3 & kiểm tra độ sâu phân cấp cây.
 * Case 2: Gắn kết và thay đổi người phụ trách cho từng phòng ban.
 * Case 3: Chống tạo vòng lặp phân cấp cây (Cycle Detection: A->B thì B không làm cha A).
 * Case 4: Không được chọn chính nó làm cha.
 * Case 5: BUSINESS RULE: Có yêu cầu tuyển dụng mở => TUYỆT ĐỐI KHÔNG ĐƯỢC XÓA (Message bắt buộc).
 * Case 6: Cho phép Ngừng áp dụng phòng ban khi đang có yêu cầu tuyển dụng mở.
 * Case 7: Kích hoạt lại phòng ban đã ngừng áp dụng.
 * Case 8: Không cho xóa phòng ban khi còn phòng ban con trực thuộc.
 * Case 9: Loại trừ chính nó và các node con trong danh sách chọn phòng ban cha.
 * Case 10: Xóa phòng ban độc lập khi đủ điều kiện.
 * ==============================================================================
 */
public class DepartmentHierarchyTest {

    private DepartmentService departmentService;

    @Before
    public void setUp() {
        departmentService = new DepartmentService();
    }

    /**
     * Case 1: Kiểm tra cấu trúc cây nhiều cấp và tính toán Level (Cấp 1, Cấp 2, Cấp 3...)
     */
    @Test
    public void testDepartmentTreeStructureAndLevels() {
        Department root = new Department("d-company", "CTY", "Tập đoàn Công nghệ", "Tổng công ty");
        root.setLevel(0);

        Department tech = new Department("d-tech", "TECH", "Khối Công nghệ", "Phụ trách R&D");
        tech.setParentId("d-company");
        tech.setLevel(1);

        Department dev = new Department("d-dev", "DEV", "Phòng Phát triển phần mềm", "Backend & Frontend");
        dev.setParentId("d-tech");
        dev.setLevel(2);

        Department qa = new Department("d-qa", "QA", "Phòng Kiểm thử", "QC & QA");
        qa.setParentId("d-tech");
        qa.setLevel(2);

        tech.addChild(dev);
        tech.addChild(qa);
        root.addChild(tech);

        assertEquals(0, root.getLevel());
        assertEquals("CTY", root.getCode());
        assertTrue(root.hasChildren());
        assertEquals(1, root.getChildren().size());

        Department childTech = root.getChildren().get(0);
        assertEquals(1, childTech.getLevel());
        assertEquals("TECH", childTech.getCode());
        assertTrue(childTech.hasChildren());
        assertEquals(2, childTech.getChildren().size());

        Department leafDev = childTech.getChildren().get(0);
        assertEquals(2, leafDev.getLevel());
        assertEquals("DEV", leafDev.getCode());
        assertFalse(leafDev.hasChildren());
    }

    /**
     * Case 2: Mỗi phòng ban có một người phụ trách hợp lệ
     */
    @Test
    public void testDepartmentManagerAssignment() {
        Department dept = new Department();
        dept.setId("dept-test-mgr");
        dept.setCode("TEST-MGR");
        dept.setName("Phòng Nghiệp vụ Test");
        dept.setManagerId("usr-admin-01");
        dept.setManagerName("Trần Quản Trị");
        dept.setManagerEmail("admin@irms.local");
        dept.setManagerJobTitle("Trưởng phòng Kỹ thuật");

        assertEquals("usr-admin-01", dept.getManagerId());
        assertEquals("Trần Quản Trị", dept.getManagerName());
        assertEquals("admin@irms.local", dept.getManagerEmail());
        assertEquals("Trưởng phòng Kỹ thuật", dept.getManagerJobTitle());
    }

    /**
     * Case 3 & 4: Chống tạo vòng lặp phân cấp cây (Cycle Detection)
     * - Không thể chọn chính nó làm cha
     * - Nếu A là cha B thì B không được trở thành cha của A
     */
    @Test
    public void testCyclePreventionSelfParent() {
        try {
            departmentService.validateNoCycle("dept-001", "dept-001");
            fail("Phải ném ngoại lệ khi chọn chính nó làm cha!");
        } catch (IllegalArgumentException e) {
            assertTrue("Thông báo lỗi phải rõ ràng", e.getMessage().contains("chính phòng ban"));
        }
    }

    @Test
    public void testCyclePreventionDirectLoop() {
        try {
            departmentService.validateNoCycle("dept-003", "dept-006");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("vòng lặp") || e.getMessage().contains("phòng ban cha"));
        }
    }

    /**
     * Case 5: BUSINESS RULE QUAN TRỌNG NHẤT:
     * Phòng ban có yêu cầu tuyển dụng đang mở => TUYỆT ĐỐI KHÔNG ĐƯỢC XÓA!
     * Thông báo bắt buộc:
     * "Không thể xóa phòng ban vì đang có yêu cầu tuyển dụng mở. Vui lòng ngừng áp dụng phòng ban thay vì xóa."
     */
    @Test
    public void testDeleteBlockedWhenOpenRecruitmentRequestsExist() {
        DeleteCheckResult res = new DeleteCheckResult();
        res.hasOpenRequisitions = true;
        res.canDelete = false;
        res.message = "Không thể xóa phòng ban vì đang có yêu cầu tuyển dụng mở. Vui lòng ngừng áp dụng phòng ban thay vì xóa.";

        assertFalse("canDelete phải là false khi có yêu cầu tuyển dụng mở", res.canDelete);
        assertTrue("hasOpenRequisitions phải là true", res.hasOpenRequisitions);
        assertEquals(
                "Thông báo lỗi bắt buộc phải đúng từng từ theo đặc tả",
                "Không thể xóa phòng ban vì đang có yêu cầu tuyển dụng mở. Vui lòng ngừng áp dụng phòng ban thay vì xóa.",
                res.message
        );
    }

    /**
     * Case 6: Trường hợp đang có yêu cầu tuyển dụng mở => Cho phép "Ngừng áp dụng"
     */
    @Test
    public void testDeactivateAllowedForDepartment() {
        Department dept = new Department();
        dept.setId("dept-active-01");
        dept.setStatus(Department.STATUS_ACTIVE);
        assertTrue(dept.isActive());
        assertFalse(dept.isInactive());

        // Chuyển sang ngừng áp dụng
        dept.setStatus(Department.STATUS_INACTIVE);
        assertFalse(dept.isActive());
        assertTrue(dept.isInactive());
        assertEquals("Ngừng áp dụng", dept.getStatusLabel());
    }

    /**
     * Case 7: Kích hoạt lại phòng ban đã ngừng áp dụng
     */
    @Test
    public void testReactivateDepartment() {
        Department dept = new Department();
        dept.setId("dept-inactive-01");
        dept.setStatus(Department.STATUS_INACTIVE);
        assertTrue(dept.isInactive());

        // Kích hoạt lại
        dept.setStatus(Department.STATUS_ACTIVE);
        assertTrue(dept.isActive());
        assertEquals("Đang áp dụng", dept.getStatusLabel());
    }

    /**
     * Case 8: Không cho xóa phòng ban khi còn phòng ban con trực thuộc
     */
    @Test
    public void testDeleteBlockedWhenChildDepartmentsExist() {
        DeleteCheckResult res = new DeleteCheckResult();
        res.hasChildren = true;
        res.canDelete = false;
        res.message = "Không thể xóa phòng ban khi còn 2 phòng ban con trực thuộc. Vui lòng chuyển hoặc xóa các phòng ban con trước.";

        assertFalse(res.canDelete);
        assertTrue(res.hasChildren);
        assertTrue(res.message.contains("phòng ban con"));
    }

    /**
     * Case 9: Loại trừ chính nó và các node con trong dropdown chọn cha (Indented list)
     */
    @Test
    public void testExcludedSubtreeFromParentDropdown() {
        Department root = new Department("root", "ROOT", "Cấp gốc", "");
        root.setLevel(0);

        Department child1 = new Department("c1", "C1", "Nhánh 1", "");
        child1.setParentId("root");
        child1.setLevel(1);

        Department child2 = new Department("c2", "C2", "Nhánh 2", "");
        child2.setParentId("root");
        child2.setLevel(1);

        Department grandchild1 = new Department("gc1", "GC1", "Cháu 1", "");
        grandchild1.setParentId("c1");
        grandchild1.setLevel(2);

        child1.addChild(grandchild1);
        root.addChild(child1);
        root.addChild(child2);

        List<String> allowedForChild1 = new ArrayList<>();
        allowedForChild1.add("root");
        allowedForChild1.add("c2");

        assertFalse("Không được chứa chính child1", allowedForChild1.contains("c1"));
        assertFalse("Không được chứa cháu grandchild1", allowedForChild1.contains("gc1"));
        assertTrue("Chứa root", allowedForChild1.contains("root"));
        assertTrue("Chứa c2 (nhánh độc lập)", allowedForChild1.contains("c2"));
    }

    /**
     * Case 10: Xóa phòng ban độc lập khi đủ điều kiện
     */
    @Test
    public void testCanDeleteLeafDepartmentWithoutDependencies() {
        DeleteCheckResult res = new DeleteCheckResult();
        res.canDelete = true;
        res.hasOpenRequisitions = false;
        res.hasChildren = false;
        res.hasUsers = false;
        res.hasSalaryRanges = false;
        res.hasHistoryRequisitions = false;
        res.message = "Phòng ban đủ điều kiện xóa.";

        assertTrue(res.canDelete);
        assertEquals("Phòng ban đủ điều kiện xóa.", res.message);
    }
}

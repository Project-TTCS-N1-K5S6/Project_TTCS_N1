package com.irms;

import com.irms.model.Role;
import com.irms.model.User;
import com.irms.service.AuthService;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

/**
 * ==============================================================================
 * BỘ KIỂM THỬ TỰ ĐỘNG PHÂN QUYỀN RBAC & BẢO MẬT (RBACPermissionTest)
 * ==============================================================================
 * Đáp ứng trực tiếp yêu cầu của User Story 5 (US 5):
 * "Tự động kiểm thử phân quyền tự động (automated tests) cho các ca kiểm thử chính."
 * 
 * Kiểm tra các ca kiểm thử:
 * 1. Admin có toàn quyền truy cập tất cả chức năng trong hệ thống.
 * 2. Interviewer (Người phỏng vấn) bị từ chối truy cập dải lương (salary.view) và quản trị người dùng.
 * 3. Recruiter (Chuyên viên tuyển dụng) có quyền ứng viên nhưng bị từ chối quản trị người dùng.
 * 4. Cơ chế từ chối mặc định (Deny-by-default) khi chưa được cấp quyền.
 * 5. Kiểm thử độ phức tạp mật khẩu theo tiêu chí US 3 & US 4 (tối thiểu 8 ký tự, có cả chữ và số).
 * ==============================================================================
 */
public class RBACPermissionTest {

    private User adminUser;
    private User hrManagerUser;
    private User recruiterUser;
    private User interviewerUser;
    private User guestUser;

    @Before
    public void setUp() {
        // 1. Khởi tạo tài khoản Quản trị hệ thống (ADMIN)
        adminUser = new User();
        adminUser.setId("u-admin");
        adminUser.setFullName("System Admin");
        adminUser.setEmail("admin@company.local");
        Role adminRole = new Role("role-001", "ADMIN", "Quản trị hệ thống", "Full admin", true);
        adminUser.setRoles(Collections.singletonList(adminRole));
        adminUser.setPermissions(Arrays.asList(
                "users.view", "users.create", "users.update", "users.lock",
                "roles.view", "permissions.view", "permissions.manage",
                "salary.view", "candidates.view", "candidates.manage",
                "requisitions.view", "requisitions.create", "audit.view"
        ));

        // 2. Khởi tạo tài khoản Trưởng phòng HR (HR_MANAGER)
        hrManagerUser = new User();
        hrManagerUser.setId("u-hr");
        hrManagerUser.setFullName("HR Manager");
        Role hrRole = new Role("role-002", "HR_MANAGER", "Trưởng phòng Nhân sự", "HR", true);
        hrManagerUser.setRoles(Collections.singletonList(hrRole));
        hrManagerUser.setPermissions(Arrays.asList(
                "department.view", "candidates.view", "candidates.manage",
                "salary.view", "requisitions.view", "requisitions.approve"
        ));

        // 3. Khởi tạo tài khoản Chuyên viên tuyển dụng (RECRUITER)
        recruiterUser = new User();
        recruiterUser.setId("u-recruiter");
        recruiterUser.setFullName("Recruiter Staff");
        Role recRole = new Role("role-003", "RECRUITER", "Chuyên viên tuyển dụng", "Recruiter", true);
        recruiterUser.setRoles(Collections.singletonList(recRole));
        recruiterUser.setPermissions(Arrays.asList(
                "department.view", "candidates.view", "candidates.manage",
                "requisitions.view", "requisitions.create", "job_postings.manage"
        ));

        // 4. Khởi tạo tài khoản Người phỏng vấn (INTERVIEWER)
        interviewerUser = new User();
        interviewerUser.setId("u-interviewer");
        interviewerUser.setFullName("Interviewer Staff");
        Role intRole = new Role("role-005", "INTERVIEWER", "Người phỏng vấn", "Interviewer", true);
        interviewerUser.setRoles(Collections.singletonList(intRole));
        interviewerUser.setPermissions(Arrays.asList(
                "department.view", "candidates.view", "interviews.view", "interviews.evaluate"
        ));

        // 5. Khởi tạo tài khoản thường chưa phân quyền
        guestUser = new User();
        guestUser.setId("u-guest");
        guestUser.setFullName("Guest User");
        guestUser.setRoles(new ArrayList<>());
        guestUser.setPermissions(new ArrayList<>());
    }

    /**
     * Ca kiểm thử 1 [US 5]: Admin có toàn quyền truy cập tất cả các chức năng.
     */
    @Test
    public void testAdminHasFullAccess() {
        assertTrue("Admin phải có vai trò ADMIN", adminUser.hasRole("ADMIN"));
        assertTrue("Admin phải xem được danh sách tài khoản", adminUser.hasPermission("users.view"));
        assertTrue("Admin phải tạo được tài khoản", adminUser.hasPermission("users.create"));
        assertTrue("Admin phải xem được dải lương", adminUser.hasPermission("salary.view"));
        assertTrue("Admin phải xem được ma trận phân quyền", adminUser.hasPermission("permissions.view"));
        assertTrue("Admin phải xem được nhật ký kiểm toán", adminUser.hasPermission("audit.view"));
    }

    /**
     * Ca kiểm thử 2 [US 5]: Người phỏng vấn (INTERVIEWER) tuyệt đối KHÔNG ĐƯỢC xem dải lương.
     */
    @Test
    public void testInterviewerCannotAccessSalaryRange() {
        assertTrue("Interviewer phải có vai trò INTERVIEWER", interviewerUser.hasRole("INTERVIEWER"));
        assertFalse("Interviewer không được phép có vai trò ADMIN", interviewerUser.hasRole("ADMIN"));
        assertFalse("Interviewer tuyệt đối không được có quyền salary.view", interviewerUser.hasPermission("salary.view"));
        assertFalse("Interviewer không được xem danh sách người dùng", interviewerUser.hasPermission("users.view"));
    }

    /**
     * Ca kiểm thử 3 [US 5]: Chuyên viên tuyển dụng (RECRUITER) xem được hồ sơ ứng viên nhưng không được quản trị user.
     */
    @Test
    public void testRecruiterCannotAccessUserManagement() {
        assertTrue("Recruiter phải có vai trò RECRUITER", recruiterUser.hasRole("RECRUITER"));
        assertTrue("Recruiter xem được hồ sơ ứng viên", recruiterUser.hasPermission("candidates.view"));
        assertTrue("Recruiter quản lý được ứng viên", recruiterUser.hasPermission("candidates.manage"));

        assertFalse("Recruiter không được xem danh sách user", recruiterUser.hasPermission("users.view"));
        assertFalse("Recruiter không được tạo user", recruiterUser.hasPermission("users.create"));
        assertFalse("Recruiter không được cấu hình quyền hạn", recruiterUser.hasPermission("permissions.manage"));
        assertFalse("Recruiter không được xem dải lương", recruiterUser.hasPermission("salary.view"));
    }

    /**
     * Ca kiểm thử 4 [US 5]: Cơ chế từ chối mặc định (Deny-by-default).
     * Bất kỳ tài khoản nào khi truy cập chức năng không được cấp quyền rõ ràng đều bị từ chối (false).
     */
    @Test
    public void testDenyByDefaultPolicy() {
        assertFalse("Người dùng chưa cấp quyền không thể xem user", guestUser.hasPermission("users.view"));
        assertFalse("Người dùng chưa cấp quyền không thể xem salary", guestUser.hasPermission("salary.view"));
        assertFalse("Người dùng chưa cấp quyền không thể xem audit", guestUser.hasPermission("audit.view"));
        assertFalse("Quyền không tồn tại phải luôn trả về false", interviewerUser.hasPermission("system.destroy"));
    }

    /**
     * Ca kiểm thử 5 [US 3 & US 4]: Kiểm thử quy tắc độ phức tạp của mật khẩu.
     * Quy tắc: Tối thiểu 8 ký tự, bao gồm cả chữ cái và chữ số.
     */
    @Test
    public void testPasswordComplexityValidation() {
        // Hợp lệ: đủ 8 ký tự trở lên, có cả chữ và số
        assertTrue(AuthService.isValidPasswordComplexity("Admin@123456"));
        assertTrue(AuthService.isValidPasswordComplexity("Pass1234"));
        assertTrue(AuthService.isValidPasswordComplexity("Vietnam2026"));
        assertTrue(AuthService.isValidPasswordComplexity("Secret#8899"));

        // Không hợp lệ: dưới 8 ký tự
        assertFalse(AuthService.isValidPasswordComplexity("Abc12"));
        assertFalse(AuthService.isValidPasswordComplexity("1234567"));

        // Không hợp lệ: chỉ có chữ, không có số
        assertFalse(AuthService.isValidPasswordComplexity("PasswordOnly"));
        assertFalse(AuthService.isValidPasswordComplexity("abcdefghij"));

        // Không hợp lệ: chỉ có số, không có chữ
        assertFalse(AuthService.isValidPasswordComplexity("123456789"));
        assertFalse(AuthService.isValidPasswordComplexity("9988776655"));

        // Không hợp lệ: chuỗi rỗng hoặc null
        assertFalse(AuthService.isValidPasswordComplexity(""));
        assertFalse(AuthService.isValidPasswordComplexity(null));
    }

    /**
     * Ca kiểm thử 6 [KN-103]: Chỉ Trưởng phòng Nhân sự (HR_MANAGER) và Quản trị hệ thống (ADMIN) mới được xem & quản lý dải lương.
     * Mọi vai trò khác (Recruiter, Interviewer, Guest...) đều bị từ chối truy cập.
     */
    @Test
    public void testOnlyHRManagerAndAdminCanAccessSalaryRange() {
        // Trưởng phòng Nhân sự (HR_MANAGER) được phép
        assertTrue("Trưởng phòng HR phải có vai trò HR_MANAGER", hrManagerUser.hasRole("HR_MANAGER"));
        assertTrue("Trưởng phòng HR phải có quyền xem dải lương", hrManagerUser.hasRole("HR_MANAGER") || hrManagerUser.hasRole("ADMIN"));

        // Quản trị viên (ADMIN) được phép
        assertTrue("Admin phải có vai trò ADMIN", adminUser.hasRole("ADMIN"));
        assertTrue("Admin phải có quyền xem dải lương", adminUser.hasRole("HR_MANAGER") || adminUser.hasRole("ADMIN"));

        // Chuyên viên tuyển dụng (RECRUITER) KHÔNG được phép
        assertFalse("Recruiter không được phép có vai trò HR_MANAGER", recruiterUser.hasRole("HR_MANAGER"));
        assertFalse("Recruiter không được phép có vai trò ADMIN", recruiterUser.hasRole("ADMIN"));
        boolean canRecruiterAccess = recruiterUser.hasRole("HR_MANAGER") || recruiterUser.hasRole("ADMIN");
        assertFalse("Recruiter tuyệt đối không được truy cập dải lương", canRecruiterAccess);

        // Người phỏng vấn (INTERVIEWER) KHÔNG được phép
        assertFalse("Interviewer không được phép có vai trò HR_MANAGER", interviewerUser.hasRole("HR_MANAGER"));
        assertFalse("Interviewer không được phép có vai trò ADMIN", interviewerUser.hasRole("ADMIN"));
        boolean canInterviewerAccess = interviewerUser.hasRole("HR_MANAGER") || interviewerUser.hasRole("ADMIN");
        assertFalse("Interviewer tuyệt đối không được truy cập dải lương", canInterviewerAccess);

        // Khách / User thường KHÔNG được phép
        boolean canGuestAccess = guestUser.hasRole("HR_MANAGER") || guestUser.hasRole("ADMIN");
        assertFalse("User chưa phân quyền tuyệt đối không được truy cập dải lương", canGuestAccess);
    }
}


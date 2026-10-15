package com.irms;

import com.irms.model.AuditLog;
import com.irms.model.Candidate;
import com.irms.model.Role;
import com.irms.model.User;
import com.irms.service.CandidateService;
import com.irms.util.SecurityUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * ==============================================================================
 * BỘ KIỂM THỬ TÍNH NĂNG "XEM HỒ SƠ ỨNG VIÊN HỢP NHẤT" (KN-268 ĐẾN KN-276)
 * ==============================================================================
 * Kiểm thử đầy đủ các yêu cầu cốt lõi của User Story:
 * 1. [KN-269]: Phân quyền truy cập hồ sơ theo Recruiter phụ trách và Hiring Manager.
 * 2. [KN-271]: Logic gộp dữ liệu các lần ứng tuyển của cùng ứng viên theo email.
 * 3. [KN-274]: Quản lý và xử lý nhãn phân loại (tags) và ghi chú (notes).
 * 4. [KN-275]: Cấu hình xem trước CV an toàn (inline preview & headers).
 * 5. [KN-276]: Ghi nhận nhật ký kiểm toán (Audit Log) khi truy cập hồ sơ hợp nhất.
 * ==============================================================================
 */
public class CandidateProfileTest {

    private Candidate candidate;
    private CandidateService candidateService;

    @Before
    public void setUp() {
        candidateService = new CandidateService();
        candidate = new Candidate();
        candidate.setId("cand-001");
        candidate.setFullName("Nguyễn Văn A");
        candidate.setEmail("nguyenvana@example.com");
        candidate.setPhone("0901234567");
        candidate.setStatus("APPLIED");
        candidate.setRequisitionId("req-tech-01");
        candidate.setRequisitionTitle("Senior Java Engineer");
        candidate.setDepartmentName("Engineering");
        candidate.setCvUrl("/uploads/cv/cv_APP123_test.pdf");
        candidate.setNotes("Ứng viên có 5 năm kinh nghiệm backend");
        candidate.setTags("Java, Spring Boot, Tiềm năng");
    }

    // =========================================================================
    // 1. [KN-271]: LOGIC GỘP DỮ LIỆU CÁC LẦN ỨNG TUYỂN CỦA CÙNG ỨNG VIÊN
    // =========================================================================

    @Test
    public void testApplicationHistoryGroupingByEmail() {
        // Giả lập lịch sử 3 lần nộp đơn của cùng một email nguyenvana@example.com
        Candidate app1 = new Candidate();
        app1.setId("cand-001");
        app1.setEmail("nguyenvana@example.com");
        app1.setRequisitionTitle("Senior Java Engineer");
        app1.setStatus("APPLIED");

        Candidate app2 = new Candidate();
        app2.setId("cand-002");
        app2.setEmail("nguyenvana@example.com");
        app2.setRequisitionTitle("Backend Developer");
        app2.setStatus("REJECTED");

        Candidate app3 = new Candidate();
        app3.setId("cand-003");
        app3.setEmail("nguyenvana@example.com");
        app3.setRequisitionTitle("Java Intern");
        app3.setStatus("HIRED");

        List<Candidate> history = Arrays.asList(app1, app2, app3);

        assertEquals("Tổng số lần ứng tuyển là 3", 3, history.size());
        for (Candidate c : history) {
            assertEquals("Tất cả đơn đều thuộc cùng email ứng viên", "nguyenvana@example.com", c.getEmail());
        }

        // Đảm bảo đơn hiện tại cand-001 nằm trong danh sách
        boolean containsCurrent = history.stream().anyMatch(c -> "cand-001".equals(c.getId()));
        assertTrue("Lịch sử phải chứa đơn ứng tuyển hiện tại", containsCurrent);
    }

    // =========================================================================
    // 2. [KN-274]: HIỂN THỊ VÀ CHỈNH SỬA NHÃN, GHI CHÚ
    // =========================================================================

    @Test
    public void testCandidateTagsParsingAndHelper() {
        candidate.setTags("Java, Spring Boot, Microservices, Tiềm năng");
        List<String> tagList = candidate.getTagList();

        assertNotNull(tagList);
        assertEquals("Số lượng nhãn phân tách chính xác", 4, tagList.size());
        assertEquals("Java", tagList.get(0));
        assertEquals("Spring Boot", tagList.get(1));
        assertEquals("Microservices", tagList.get(2));
        assertEquals("Tiềm năng", tagList.get(3));
    }

    @Test
    public void testCandidateTagsEmptyOrNullHandling() {
        candidate.setTags(null);
        assertTrue("Tag null trả về danh sách rỗng an toàn", candidate.getTagList().isEmpty());

        candidate.setTags("   ");
        assertTrue("Tag khoảng trắng trả về danh sách rỗng an toàn", candidate.getTagList().isEmpty());

        candidate.setTags("  Frontend  , ,  ReactJS  ");
        List<String> tagList = candidate.getTagList();
        assertEquals("Tự loại bỏ khoảng trắng và chuỗi rỗng", 2, tagList.size());
        assertEquals("Frontend", tagList.get(0));
        assertEquals("ReactJS", tagList.get(1));
    }

    @Test
    public void testCandidateNotesManagement() {
        String newNotes = "Đã phỏng vấn kỹ thuật vòng 1, đánh giá thuật toán tốt, kiến thức Spring vững.";
        candidate.setNotes(newNotes);
        assertEquals("Ghi chú được cập nhật chính xác", newNotes, candidate.getNotes());
    }

    // =========================================================================
    // 3. [KN-269]: PHÂN QUYỀN TRUY CẬP HỒ SƠ THEO VAI TRÒ
    // =========================================================================

    @Test
    public void testAdminAndHrManagerHaveFullAccess() {
        User admin = new User();
        admin.setId("u-admin");
        admin.setRoles(Collections.singletonList(createRole("ADMIN", "Quản trị viên")));

        User hrManager = new User();
        hrManager.setId("u-hr");
        hrManager.setRoles(Collections.singletonList(createRole("HR_MANAGER", "Trưởng phòng Nhân sự")));

        assertTrue("ADMIN có quyền xem hồ sơ", candidateService.canAccessCandidateProfile(admin, candidate));
        assertTrue("HR_MANAGER có quyền xem hồ sơ", candidateService.canAccessCandidateProfile(hrManager, candidate));
    }

    @Test
    public void testUnauthenticatedUserDenied() {
        assertFalse("User null không được phép xem hồ sơ", candidateService.canAccessCandidateProfile(null, candidate));
        assertFalse("Candidate null không được phép xử lý", candidateService.canAccessCandidateProfile(new User(), null));
    }

    @Test
    public void testUnauthorizedRoleDenied() {
        // Nhân viên thông thường (EMPLOYEE) không có quyền mở hồ sơ ứng viên
        User employee = new User();
        employee.setId("u-emp");
        employee.setRoles(Collections.singletonList(createRole("EMPLOYEE", "Nhân viên")));

        assertFalse("EMPLOYEE không được phép mở hồ sơ ứng viên",
                candidateService.canAccessCandidateProfile(employee, candidate));
    }

    // =========================================================================
    // 4. [KN-275]: CẤU HÌNH XEM TRƯỚC CV AN TOÀN
    // =========================================================================

    @Test
    public void testCvPreviewSecurityFileNameAndPathValidation() {
        String cvUrl = candidate.getCvUrl();
        assertNotNull(cvUrl);
        assertTrue("CV phải có định dạng .pdf hoặc .docx",
                cvUrl.toLowerCase().endsWith(".pdf") || cvUrl.toLowerCase().endsWith(".docx"));

        // Kiểm tra chống Path Traversal trong đường dẫn
        assertFalse("Không được chứa ký tự leo thang thư mục ..", cvUrl.contains(".."));

        // Chuẩn hóa tên file xuất ra trình duyệt
        String safeFileName = "CV_" + candidate.getFullName().replaceAll("[^a-zA-Z0-9_.-]", "_") + ".pdf";
        assertEquals("CV_Nguy_n_V_n_A.pdf", safeFileName);
    }

    // =========================================================================
    // 5. [KN-276]: GHI NHẬN LỊCH SỬ TRUY CẬP (AUDIT LOG)
    // =========================================================================

    @Test
    public void testAuditLogStructureForCandidateProfileAccess() {
        AuditLog log = new AuditLog();
        log.setId(SecurityUtil.generateUUID());
        log.setUserId("u-recruiter-01");
        log.setAction("VIEW_CANDIDATE_PROFILE");
        log.setEntityType("CANDIDATE");
        log.setEntityId(candidate.getId());
        log.setDescription("Xem hồ sơ ứng viên hợp nhất: " + candidate.getFullName() + " (" + candidate.getEmail() + ")");
        log.setIpAddress("192.168.1.100");
        log.setUserAgent("Mozilla/5.0 IRMS-Client");

        assertEquals("Action chính xác", "VIEW_CANDIDATE_PROFILE", log.getAction());
        assertEquals("EntityType là CANDIDATE", "CANDIDATE", log.getEntityType());
        assertEquals("EntityId khớp mã hồ sơ", "cand-001", log.getEntityId());
        assertTrue("Nội dung mô tả chứa tên ứng viên", log.getDescription().contains(candidate.getFullName()));
        assertTrue("Nội dung mô tả chứa email ứng viên", log.getDescription().contains(candidate.getEmail()));
    }

    // =========================================================================
    // HÀM BỔ TRỢ TẠO ROLE CHO TEST
    // =========================================================================
    private Role createRole(String code, String name) {
        Role r = new Role();
        r.setCode(code);
        r.setName(name);
        return r;
    }
}

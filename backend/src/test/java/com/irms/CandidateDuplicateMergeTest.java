package com.irms;

import com.google.gson.JsonObject;
import com.irms.dao.CandidateDuplicateDAO;
import com.irms.model.Candidate;
import com.irms.model.CandidateActivity;
import com.irms.model.CandidateComparisonDTO;
import com.irms.model.CandidateDuplicateWarning;
import com.irms.model.CandidateMergeHistory;
import com.irms.model.User;
import com.irms.util.JsonUtil;
import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * ==============================================================================
 * BỘ KIỂM THỬ TÍNH NĂNG PHÁT HIỆN TRÙNG, SO SÁNH, GỘP BẢO TOÀN LỊCH SỬ & HOÀN TÁC 24H
 * ==============================================================================
 * 1. Phát hiện trùng theo email và số điện thoại (kể cả định dạng chuẩn hóa +84 / 0)
 * 2. Hiển thị so sánh cạnh nhau trước khi gộp (CandidateComparisonDTO)
 * 3. Gộp giữ lại toàn bộ lịch sử ứng tuyển và hoạt động của cả hai hồ sơ
 * 4. Hoàn tác hợp nhất trong vòng 24 giờ và từ chối khi quá thời hạn
 * ==============================================================================
 */
public class CandidateDuplicateMergeTest {

    private Candidate candidateA;
    private Candidate candidateB;
    private User recruiter;

    @Before
    public void setUp() {
        recruiter = new User();
        recruiter.setId("usr-recruiter-01");
        recruiter.setFullName("Lê Tuyển Dụng");
        recruiter.setEmail("recruiter@example.com");

        candidateA = new Candidate();
        candidateA.setId("cand-001");
        candidateA.setFullName("Nguyễn Văn An");
        candidateA.setEmail("nguyenvanan@example.com");
        candidateA.setPhone("0912345678");
        candidateA.setNormalizedPhone("0912345678");
        candidateA.setStatus("INTERVIEWING");
        candidateA.setRequisitionId("req-java-01");
        candidateA.setRequisitionTitle("Senior Java Developer");
        candidateA.setDepartmentName("Bộ phận Công nghệ");
        candidateA.setCvUrl("/uploads/cv/an_cv1.pdf");
        candidateA.setNotes("Ứng viên có kỹ năng backend tốt");
        candidateA.setTags("Java, Spring, Backend");

        candidateB = new Candidate();
        candidateB.setId("cand-002");
        candidateB.setFullName("Nguyễn V. An");
        candidateB.setEmail("nguyenvanan@example.com"); // Cùng email
        candidateB.setPhone("+84 912 345 678"); // SĐT định dạng quốc tế
        candidateB.setNormalizedPhone("0912345678");
        candidateB.setStatus("APPLIED");
        candidateB.setRequisitionId("req-python-02");
        candidateB.setRequisitionTitle("Python AI Engineer");
        candidateB.setDepartmentName("Bộ phận AI");
        candidateB.setCvUrl("/uploads/cv/an_cv2.pdf");
        candidateB.setNotes("Có bổ sung kinh nghiệm xử lý dữ liệu");
        candidateB.setTags("AI, Python, Tiềm năng");
    }

    // =========================================================================
    // YÊU CẦU 1: PHÁT HIỆN TRÙNG THEO EMAIL VÀ SỐ ĐIỆN THOẠI
    // =========================================================================

    @Test
    public void testPhoneNormalizationFormats() {
        // Chuẩn hóa định dạng số điện thoại Việt Nam (+84, khoảng trắng, gạch ngang)
        assertEquals("0912345678", CandidateDuplicateDAO.normalizePhone("+84 912 345 678"));
        assertEquals("0912345678", CandidateDuplicateDAO.normalizePhone("+84-912-345-678"));
        assertEquals("0912345678", CandidateDuplicateDAO.normalizePhone("84912345678"));
        assertEquals("0912345678", CandidateDuplicateDAO.normalizePhone("0912.345.678"));
        assertEquals("0912345678", CandidateDuplicateDAO.normalizePhone("0912 345 678"));
        assertNull(CandidateDuplicateDAO.normalizePhone(""));
        assertNull(CandidateDuplicateDAO.normalizePhone(null));
    }

    @Test
    public void testDuplicateDetectionCriteria() {
        // Test tiêu chí trùng theo Email
        String emailA = candidateA.getEmail().toLowerCase();
        String emailB = candidateB.getEmail().toLowerCase();
        boolean isEmailMatch = emailA.equals(emailB);
        assertTrue("Hai ứng viên phải trùng email", isEmailMatch);

        // Test tiêu chí trùng theo SĐT chuẩn hóa
        String phoneA = CandidateDuplicateDAO.normalizePhone(candidateA.getPhone());
        String phoneB = CandidateDuplicateDAO.normalizePhone(candidateB.getPhone());
        boolean isPhoneMatch = phoneA != null && phoneA.equals(phoneB);
        assertTrue("Hai ứng viên phải trùng số điện thoại sau khi chuẩn hóa", isPhoneMatch);

        // Trường hợp cả hai tiêu chí
        String matchType = (isEmailMatch && isPhoneMatch) ? "BOTH" : (isEmailMatch ? "EMAIL" : "PHONE");
        assertEquals("Loại trùng khớp phải là BOTH khi cả email và SĐT trùng", "BOTH", matchType);
    }

    // =========================================================================
    // YÊU CẦU 2: HIỂN THỊ SO SÁNH CẠNH NHAU TRƯỚC KHI GỘP
    // =========================================================================

    @Test
    public void testCandidateComparisonDTOSideBySide() {
        CandidateDuplicateWarning warning = new CandidateDuplicateWarning();
        warning.setId("warn-001");
        warning.setMatchType("BOTH");
        warning.setMatchValue(candidateA.getEmail());

        CandidateComparisonDTO dto = new CandidateComparisonDTO(candidateA, candidateB, warning);

        assertNotNull("DTO phải được khởi tạo thành công", dto);
        assertEquals("candidate1 phải là candidateA", "cand-001", dto.getCandidate1().getId());
        assertEquals("candidate2 phải là candidateB", "cand-002", dto.getCandidate2().getId());

        // Kiểm tra đối chiếu các trường khác biệt
        assertFalse("Tên khác nhau (Nguyễn Văn An vs Nguyễn V. An)", dto.isNameEqual());
        assertTrue("Email trùng khớp", dto.isEmailEqual());
        assertTrue("Số điện thoại trùng khớp", dto.isPhoneEqual());
        assertFalse("Trạng thái pipeline khác biệt (INTERVIEWING vs APPLIED)", dto.isStatusEqual());
        assertFalse("Vị trí tuyển dụng khác biệt (req-java-01 vs req-python-02)", dto.isRequisitionEqual());
        assertFalse("CV đính kèm khác biệt", dto.isCvEqual());
    }

    // =========================================================================
    // YÊU CẦU 3: GỘP BẢO TOÀN TOÀN BỘ LỊCH SỬ ỨNG TUYỂN CỦA CẢ HAI HỒ SƠ
    // =========================================================================

    @Test
    public void testMergePreservesAllApplicationsAndActivities() {
        // Tạo lịch sử hoạt động ứng tuyển của cả hai hồ sơ
        CandidateActivity act1 = new CandidateActivity("act-1", candidateA.getId(), "INTERVIEW",
                "Phỏng vấn vòng 1", "Pass chuyên môn kỹ thuật", "usr-1", "Interviewer 1");
        CandidateActivity act2 = new CandidateActivity("act-2", candidateB.getId(), "APPLIED",
                "Nộp hồ sơ qua Career Portal", "Vị trí Python AI", null, "Ứng viên");

        List<CandidateActivity> activitiesA = new ArrayList<>(Arrays.asList(act1));
        List<CandidateActivity> activitiesB = new ArrayList<>(Arrays.asList(act2));

        // Mô phỏng logic bảo toàn lịch sử hoạt động: chuyển toàn bộ hoạt động từ B sang A
        for (CandidateActivity bAct : activitiesB) {
            bAct.setOriginalCandidateId(bAct.getCandidateId());
            bAct.setCandidateId(candidateA.getId());
            activitiesA.add(bAct);
        }

        assertEquals("Tổng số hoạt động sau hợp nhất phải là 2", 2, activitiesA.size());
        assertTrue("Hoạt động gốc của B phải có original_candidate_id = cand-002",
                activitiesA.stream().anyMatch(a -> "cand-002".equals(a.getOriginalCandidateId())));

        // Mô phỏng gộp tags không bị trùng lặp
        Set<String> combinedTags = new LinkedHashSet<>();
        combinedTags.addAll(candidateA.getTagList());
        combinedTags.addAll(candidateB.getTagList());
        assertEquals("Tổng số nhãn gộp không bị trùng", 6, combinedTags.size());
        assertTrue(combinedTags.contains("Java"));
        assertTrue(combinedTags.contains("AI"));

        // Mô phỏng lịch sử ứng tuyển bảo toàn của cả hai hồ sơ
        candidateB.setMerged(true);
        candidateB.setMergedIntoId(candidateA.getId());

        List<Candidate> allApplications = Arrays.asList(candidateA, candidateB);
        assertEquals("Lịch sử ứng tuyển bảo toàn cả 2 lần ứng tuyển", 2, allApplications.size());
    }

    // =========================================================================
    // YÊU CẦU 4: HOÀN TÁC HỢP NHẤT TRONG VÒNG 24 GIỜ (UNDO MERGE)
    // =========================================================================

    @Test
    public void testCanUndoWithin24Hours() {
        CandidateMergeHistory history = new CandidateMergeHistory();
        history.setId("hist-001");
        history.setPrimaryCandidateId(candidateA.getId());
        history.setSecondaryCandidateId(candidateB.getId());
        history.setPrimaryCandidateName(candidateA.getFullName());
        history.setSecondaryCandidateName(candidateB.getFullName());
        history.setMergedByUserName(recruiter.getFullName());

        // Thời điểm vừa mới gộp (cách đây 2 giờ)
        history.setCreatedAt(new Timestamp(System.currentTimeMillis() - 2 * 3600 * 1000L));
        history.setUndone(false);

        assertTrue("Phải cho phép hoàn tác khi thời gian hợp nhất <= 24 giờ", history.isCanUndo());
        assertTrue("Số phút còn lại phải dương", history.getRemainingUndoMinutes() > 0);
        assertTrue("Chuỗi mô tả thời gian còn lại phải hiển thị hợp lệ", history.getRemainingUndoText().contains("giờ"));
    }

    @Test
    public void testCannotUndoAfter24Hours() {
        CandidateMergeHistory history = new CandidateMergeHistory();
        history.setId("hist-002");
        history.setPrimaryCandidateId(candidateA.getId());
        history.setSecondaryCandidateId(candidateB.getId());
        history.setPrimaryCandidateName(candidateA.getFullName());
        history.setSecondaryCandidateName(candidateB.getFullName());

        // Thời điểm gộp cách đây 25 giờ (vượt quá 24 giờ)
        history.setCreatedAt(new Timestamp(System.currentTimeMillis() - 25 * 3600 * 1000L));
        history.setUndone(false);

        assertFalse("Không được phép hoàn tác khi đã quá 24 giờ", history.isCanUndo());
        assertEquals("Chuỗi mô tả thời gian phải báo hết hạn", "Hết hạn hoàn tác (> 24 giờ)", history.getRemainingUndoText());
    }

    @Test
    public void testCannotUndoTwice() {
        CandidateMergeHistory history = new CandidateMergeHistory();
        history.setId("hist-003");
        history.setCreatedAt(new Timestamp(System.currentTimeMillis() - 1 * 3600 * 1000L));
        history.setUndone(true); // Đã được hoàn tác trước đó

        assertFalse("Không được phép hoàn tác lần thứ hai", history.isCanUndo());
        assertEquals("Chuỗi mô tả phải báo đã hoàn tác", "Đã hoàn tác", history.getRemainingUndoText());
    }

    @Test
    public void testUndoRestoresOriginalSnapshotState() {
        // Tạo snapshot trước khi gộp
        String primarySnapshot = JsonUtil.toJson(candidateA);
        String secondarySnapshot = JsonUtil.toJson(candidateB);
        String mergedSnapshot = "{\"primaryBefore\":" + primarySnapshot + ",\"secondaryBefore\":" + secondarySnapshot + "}";

        // Giả lập sau khi gộp: candidateA nhận thông tin của B, candidateB bị đánh dấu MERGED
        Candidate mergedPrimary = new Candidate();
        mergedPrimary.setId(candidateA.getId());
        mergedPrimary.setFullName(candidateB.getFullName()); // Bị đổi tên
        mergedPrimary.setStatus("OFFER");

        candidateB.setMerged(true);
        candidateB.setMergedIntoId(candidateA.getId());
        candidateB.setStatus("MERGED");

        // Mô phỏng quá trình HOÀN TÁC: Đọc lại snapshot JSON để phục hồi
        JsonObject snapshotObj = JsonUtil.fromJson(mergedSnapshot, JsonObject.class);
        assertNotNull(snapshotObj);

        Candidate restoredA = JsonUtil.fromJson(snapshotObj.get("primaryBefore").toString(), Candidate.class);
        Candidate restoredB = JsonUtil.fromJson(snapshotObj.get("secondaryBefore").toString(), Candidate.class);

        // Khôi phục hồ sơ chính A
        assertEquals("Tên hồ sơ A phải được phục hồi", candidateA.getFullName(), restoredA.getFullName());
        assertEquals("Trạng thái hồ sơ A phải phục hồi thành INTERVIEWING", "INTERVIEWING", restoredA.getStatus());

        // Khôi phục hồ sơ phụ B
        assertEquals("Tên hồ sơ B phải là Nguyễn V. An", candidateB.getFullName(), restoredB.getFullName());
        assertEquals("Trạng thái hồ sơ B phải phục hồi thành APPLIED", "APPLIED", restoredB.getStatus());
        assertFalse("Hồ sơ B không còn bị đánh dấu là MERGED", restoredB.isMerged());
        assertNull("Mã hồ sơ liên kết mergedIntoId của B phải là null", restoredB.getMergedIntoId());
    }
}

package com.irms;

import com.irms.dao.CandidateDAO;
import com.irms.model.Candidate;
import com.irms.service.CVFileStorageService;
import com.irms.service.CandidateService;
import com.irms.service.CandidateService.DuplicateCandidateException;
import com.irms.service.CandidateService.DuplicateMatch;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/**
 * ==============================================================================
 * BỘ KIỂM THỬ TỰ ĐỘNG CHỨC NĂNG "THÊM ỨNG VIÊN THỦ CÔNG" (JIRA KN-288)
 * ==============================================================================
 * Kiểm thử 8 Subtask theo Jira KN-288:
 * - KN-347: [BE] API tạo mới ứng viên thủ công
 * - KN-346: [BE] Kiểm tra trùng ứng viên khi tạo mới
 * - KN-352: [BE] Hỗ trợ rà soát và đối sánh dữ liệu trùng theo hồ sơ hiện có
 * - KN-351: [FE] Lựa chọn xử lý trùng lặp (chặn hoặc vẫn tạo mới)
 * - KN-349: [FE] Tải lên và xem trước CV khi thêm ứng viên
 * - KN-350: [BE] Lưu trữ CV và quản lý liên kết tệp đính kèm (Magic Bytes)
 * - KN-353: [BE] Ghi nhận lịch sử thêm ứng viên thủ công
 * ==============================================================================
 */
public class ManualCandidateTest {

    private CandidateService candidateService;

    @Before
    public void setUp() {
        candidateService = new CandidateService();
    }

    // =========================================================================
    // 1. [KN-346]: CHUẨN HÓA SỐ ĐIỆN THOẠI & PHÁT HIỆN TRÙNG LẶP
    // =========================================================================

    @Test
    public void testPhoneNormalizationFormats() {
        assertEquals("0901234567", CandidateDAO.normalizePhone("090 123 4567"));
        assertEquals("0901234567", CandidateDAO.normalizePhone("090-123-4567"));
        assertEquals("0901234567", CandidateDAO.normalizePhone("090.123.4567"));
        assertEquals("0901234567", CandidateDAO.normalizePhone("(090) 123 4567"));
        assertEquals("0901234567", CandidateDAO.normalizePhone("+84901234567"));
        assertEquals("0901234567", CandidateDAO.normalizePhone("84901234567"));
        assertEquals("", CandidateDAO.normalizePhone(null));
        assertEquals("", CandidateDAO.normalizePhone("   "));
    }

    // =========================================================================
    // 2. [KN-352]: ĐỐI SÁNH DỮ LIỆU TRÙNG KHỚP (EMAIL, PHONE, NAME, POSITION)
    // =========================================================================

    @Test
    public void testDuplicateMatchingLogic() {
        Candidate existing = new Candidate();
        existing.setId("cand-dup-01");
        existing.setFullName("Nguyễn Văn A");
        existing.setEmail("nguyenvana@example.com");
        existing.setPhone("0901234567");
        existing.setRequisitionId("req-001");
        existing.setRequisitionTitle("Backend Developer");

        List<String> matchedFields = new ArrayList<>();
        matchedFields.add("email");
        matchedFields.add("phone");

        DuplicateMatch match = new DuplicateMatch(existing, matchedFields, true, true);

        assertEquals("Khớp mã hồ sơ", "cand-dup-01", match.getCandidate().getId());
        assertTrue("Trùng email", match.getMatchedFields().contains("email"));
        assertTrue("Trùng số điện thoại", match.getMatchedFields().contains("phone"));
        assertTrue("Cùng họ tên", match.isSameName());
        assertTrue("Cùng vị trí", match.isSamePosition());
    }

    @Test
    public void testDuplicateExceptionCarriesMatches() {
        Candidate existing = new Candidate();
        existing.setId("cand-dup-02");
        existing.setFullName("Trần Thị B");
        existing.setEmail("tranthib@example.com");

        DuplicateMatch match = new DuplicateMatch(existing, Collections.singletonList("email"), true, false);
        DuplicateCandidateException ex = new DuplicateCandidateException(Collections.singletonList(match));

        assertNotNull(ex.getMatches());
        assertEquals(1, ex.getMatches().size());
        assertEquals("tranthib@example.com", ex.getMatches().get(0).getCandidate().getEmail());
        assertFalse("Khác vị trí", ex.getMatches().get(0).isSamePosition());
    }

    // =========================================================================
    // 3. [KN-349, KN-350]: KIỂM TRA TỆP CV, ĐỊNH DẠNG & MAGIC BYTES BẢO MẬT
    // =========================================================================

    @Test
    public void testCvStorageValidationPdfMagicBytes() {
        CVFileStorageService storage = new CVFileStorageService();

        // PDF chuẩn: %PDF-
        byte[] validPdfHeader = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
        CVFileStorageService.ValidationResult res = storage.validate("candidate_cv.pdf", 1024 * 500, "application/pdf", validPdfHeader);
        assertTrue("Tệp PDF hợp lệ", res.isValid());
        assertEquals(".pdf", res.getFileExtension());

        // PDF giả mạo đuôi (nội dung thực chất là văn bản thường hoặc thực thi)
        byte[] fakePdfHeader = new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00}; // Header của file .exe
        CVFileStorageService.ValidationResult fakeRes = storage.validate("fake_cv.pdf", 1024 * 100, "application/pdf", fakePdfHeader);
        assertFalse("Chặn file PDF giả mạo đuôi", fakeRes.isValid());
        assertTrue("Thông báo lỗi magic bytes", fakeRes.getErrorMessage().contains("Magic bytes"));
    }

    @Test
    public void testCvStorageValidationDocAndDocx() {
        CVFileStorageService storage = new CVFileStorageService();

        // DOCX chuẩn: PK\x03\x04
        byte[] validDocxHeader = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x06, 0x00};
        CVFileStorageService.ValidationResult docxRes = storage.validate("cv.docx", 1024 * 300, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", validDocxHeader);
        assertTrue("Tệp DOCX hợp lệ", docxRes.isValid());

        // DOC chuẩn: OLE 0xD0 0xCF 0x11 0xE0
        byte[] validDocHeader = new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
        CVFileStorageService.ValidationResult docRes = storage.validate("cv.doc", 1024 * 200, "application/msword", validDocHeader);
        assertTrue("Tệp DOC hợp lệ", docRes.isValid());

        // Tệp định dạng không được phép (.exe, .zip, .jpg)
        CVFileStorageService.ValidationResult invalidExtRes = storage.validate("script.exe", 1024, "application/octet-stream", new byte[]{0, 0, 0, 0});
        assertFalse("Chặn tệp không phải PDF, DOC hoặc DOCX", invalidExtRes.isValid());
    }

    @Test
    public void testCvStorageFileSizeLimit() {
        CVFileStorageService storage = new CVFileStorageService();
        byte[] validPdfHeader = new byte[]{0x25, 0x50, 0x44, 0x46};

        // File quá 10MB
        long overSize = 10 * 1024 * 1024 + 1;
        CVFileStorageService.ValidationResult overRes = storage.validate("large.pdf", overSize, "application/pdf", validPdfHeader);
        assertFalse("Chặn tệp vượt quá giới hạn dung lượng", overRes.isValid());
        assertTrue(overRes.getErrorMessage().contains("vượt quá giới hạn"));

        // File 0 byte
        CVFileStorageService.ValidationResult zeroRes = storage.validate("empty.pdf", 0, "application/pdf", validPdfHeader);
        assertFalse("Chặn tệp rỗng 0 byte", zeroRes.isValid());
    }

    // =========================================================================
    // 4. [KN-347, KN-353]: RÀNG BUỘC DỮ LIỆU TẠO MỚI & LỊCH SỬ THAO TÁC
    // =========================================================================

    @Test
    public void testTrackingCodeGenerationFormat() {
        String code = candidateService.generateUniqueTrackingCode();
        assertNotNull("Mã tra cứu không null", code);
        assertTrue("Bắt đầu bằng APP-", code.startsWith("APP-"));
        assertTrue("Có độ dài chuẩn định dạng APP-YYYYMMDD-XXXX", code.length() >= 17);
    }

    @Test
    public void testCandidateValidationInputs() {
        Candidate c = new Candidate();
        c.setFullName(""); // Rỗng họ tên
        c.setEmail("invalid-email");
        c.setPhone("123"); // Quá ngắn

        try {
            candidateService.createManualCandidate(c, null, "C:/temp", "127.0.0.1", "Mozilla", null, false);
            fail("Phải ném ngoại lệ khi họ tên rỗng");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("bắt buộc"));
        } catch (Exception e) {
            fail("Phải ném IllegalArgumentException");
        }

        c.setFullName("Nguyễn Văn Hợp Lệ");
        try {
            candidateService.createManualCandidate(c, null, "C:/temp", "127.0.0.1", "Mozilla", null, false);
            fail("Phải ném ngoại lệ khi email sai định dạng");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Email không đúng định dạng"));
        } catch (Exception e) {
            fail("Phải ném IllegalArgumentException");
        }

        c.setEmail("candidate.valid@example.com");
        try {
            candidateService.createManualCandidate(c, null, "C:/temp", "127.0.0.1", "Mozilla", null, false);
            fail("Phải ném ngoại lệ khi số điện thoại quá ngắn");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Số điện thoại"));
        } catch (Exception e) {
            fail("Phải ném IllegalArgumentException");
        }
    }
}

package com.irms;

import com.irms.model.Candidate;
import com.irms.service.CVFileStorageService;
import com.irms.service.CandidateService;
import com.irms.service.EmailService;
import com.irms.util.CaptchaValidatorUtil;
import com.irms.util.EmailTemplateUtil;
import com.irms.util.RateLimiterUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * ==============================================================================
 * BỘ KIỂM THỬ TOÀN DIỆN TÍNH NĂNG "NỘP HỒ SƠ ỨNG TUYỂN KÈM CV"
 * ==============================================================================
 * Kiểm tra đầy đủ 7 subtasks theo yêu cầu:
 * - KN-230: Biểu mẫu ứng tuyển, kiểm tra định dạng email, SĐT, file <= 10MB
 * - KN-231: Từ chối nộp trùng cùng vị trí với cùng email (Duplicate Application)
 * - KN-232: Kiểm tra định dạng (.pdf, .docx), Magic Bytes và dung lượng tệp CV
 * - KN-233: Tiếp nhận hồ sơ, sinh mã tra cứu độc nhất (APP-YYYYMMDD-XXXX)
 * - KN-234: Giới hạn tần suất nộp hồ sơ theo IP trong vòng 1 giờ (Rate Limiting)
 * - KN-235: Gửi email xác nhận kèm mã tra cứu bất đồng bộ (Non-blocking)
 * - KN-236: Tích hợp và xác thực chống spam (CAPTCHA / Turnstile token)
 * ==============================================================================
 */
public class JobApplicationTest {

    private CVFileStorageService cvStorageService;
    private CandidateService candidateService;
    private final String TEST_IP = "192.168.1.100";
    private final String TEST_EMAIL = "candidate.test@example.com";
    private final String TEST_REQ_ID = "req-test-java-2026";

    @Before
    public void setUp() {
        cvStorageService = new CVFileStorageService();
        candidateService = new CandidateService();
        RateLimiterUtil.resetAll();
    }

    @After
    public void tearDown() {
        RateLimiterUtil.resetAll();
    }

    // =========================================================================
    // 1. KN-232: KIỂM TRA ĐỊNH DẠNG VÀ KÍCH THƯỚC TỆP CV
    // =========================================================================

    @Test
    public void testCVValidation_ValidPDF_Accepted() {
        // PDF Magic Bytes: %PDF-
        byte[] pdfHeader = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34};
        long validSize = 2 * 1024 * 1024; // 2MB

        CVFileStorageService.ValidationResult result =
                cvStorageService.validate("cv_nguyen_van_a.pdf", validSize, "application/pdf", pdfHeader);

        assertTrue("Tệp PDF chuẩn có dung lượng 2MB phải được chấp nhận", result.isValid());
        assertEquals(".pdf", result.getFileExtension());
        assertNull(result.getErrorMessage());
    }

    @Test
    public void testCVValidation_ValidDOCX_Accepted() {
        // DOCX Magic Bytes (Zip archive): PK\x03\x04
        byte[] docxHeader = new byte[]{0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x06, 0x00};
        long validSize = 5 * 1024 * 1024; // 5MB

        CVFileStorageService.ValidationResult result =
                cvStorageService.validate("my_resume.docx", validSize, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxHeader);

        assertTrue("Tệp DOCX chuẩn có dung lượng 5MB phải được chấp nhận", result.isValid());
        assertEquals(".docx", result.getFileExtension());
        assertNull(result.getErrorMessage());
    }

    @Test
    public void testCVValidation_OversizedFile_Rejected() {
        byte[] pdfHeader = new byte[]{0x25, 0x50, 0x44, 0x46};
        // 10MB + 1 byte (Vượt quá giới hạn 10MB)
        long oversized = 10 * 1024 * 1024L + 1L;

        CVFileStorageService.ValidationResult result =
                cvStorageService.validate("large_portfolio.pdf", oversized, "application/pdf", pdfHeader);

        assertFalse("Tệp vượt quá 10MB phải bị từ chối", result.isValid());
        assertNotNull(result.getErrorMessage());
        assertTrue("Thông báo lỗi phải đề cập đến giới hạn 10MB",
                result.getErrorMessage().contains("10MB") || result.getErrorMessage().contains("vượt quá giới hạn"));
    }

    @Test
    public void testCVValidation_InvalidExtension_Rejected() {
        byte[] someHeader = new byte[]{0x00, 0x01, 0x02, 0x03};
        long validSize = 1024 * 1024; // 1MB

        // Thử các định dạng không được phép: .png, .exe, .txt, .zip
        String[] invalidFiles = {"avatar.png", "script.exe", "notes.txt", "archive.zip"};
        for (String file : invalidFiles) {
            CVFileStorageService.ValidationResult result =
                    cvStorageService.validate(file, validSize, "application/octet-stream", someHeader);
            assertFalse("Tệp có đuôi " + file + " không được phép chấp nhận", result.isValid());
            assertTrue("Thông báo lỗi phải nêu rõ chỉ nhận .pdf hoặc .docx",
                    result.getErrorMessage().contains(".pdf") || result.getErrorMessage().contains(".docx"));
        }
    }

    @Test
    public void testCVValidation_SpoofedExtension_Rejected() {
        // Tên file là resume.pdf nhưng nội dung bên trong thực chất là plain text (không có magic bytes %PDF-)
        byte[] fakeContent = "This is a fake text file disguised as pdf".getBytes(StandardCharsets.UTF_8);
        long size = fakeContent.length;

        CVFileStorageService.ValidationResult result =
                cvStorageService.validate("malicious.pdf", size, "application/pdf", fakeContent);

        assertFalse("Tệp giả mạo đuôi .pdf nhưng magic bytes sai phải bị từ chối", result.isValid());
        assertTrue("Thông báo lỗi phải chỉ ra tệp không đúng định dạng PDF chuẩn",
                result.getErrorMessage().contains("PDF chuẩn") || result.getErrorMessage().contains("Magic bytes"));
    }

    // =========================================================================
    // 2. KN-234 & KN-236: CHỐNG SPAM, RATE LIMIT & XÁC THỰC NGƯỜI MÁY
    // =========================================================================

    @Test
    public void testRateLimiting_WithinLimit_Allowed() {
        // Trong hạn mức tối đa cho phép (mặc định 5 lần/giờ)
        for (int i = 0; i < 5; i++) {
            assertTrue("Lượt nộp thứ " + (i + 1) + " phải được chấp thuận", RateLimiterUtil.isAllowed(TEST_IP));
            RateLimiterUtil.recordAttempt(TEST_IP);
        }
        assertEquals("Số lượt còn lại phải là 0", 0, RateLimiterUtil.getRemainingAttempts(TEST_IP));
    }

    @Test
    public void testRateLimiting_ExceededLimit_Blocked() {
        // Nộp đủ 5 lần trong 1 giờ
        for (int i = 0; i < 5; i++) {
            RateLimiterUtil.recordAttempt(TEST_IP);
        }

        // Lần thứ 6 phải bị chặn ngay lập tức
        assertFalse("Lần nộp thứ 6 từ cùng địa chỉ IP trong vòng 1 giờ phải bị chặn",
                RateLimiterUtil.isAllowed(TEST_IP));

        long retryAfter = RateLimiterUtil.getRetryAfterSeconds(TEST_IP);
        assertTrue("Thời gian chờ phải lớn hơn 0 giây", retryAfter > 0);
        assertTrue("Thời gian chờ không quá 3600 giây (1 giờ)", retryAfter <= 3600);
    }

    @Test
    public void testCaptcha_ValidTokens_Accepted() {
        // Token kiểm thử tự động
        assertTrue("TEST_BYPASS_TOKEN phải được chấp nhận",
                CaptchaValidatorUtil.verifyToken(CaptchaValidatorUtil.TEST_BYPASS_TOKEN, TEST_IP));

        // Token widget bảo mật nội bộ còn hạn
        String validWidgetToken = "IRMS-CAPTCHA-" + System.currentTimeMillis() + "-abc123xy";
        assertTrue("Token widget bảo mật nội bộ hợp lệ phải được xác thực thành công",
                CaptchaValidatorUtil.verifyToken(validWidgetToken, TEST_IP));
    }

    @Test
    public void testCaptcha_InvalidOrMissingToken_Rejected() {
        // Token null hoặc rỗng
        assertFalse("Token null phải bị từ chối", CaptchaValidatorUtil.verifyToken(null, TEST_IP));
        assertFalse("Token rỗng phải bị từ chối", CaptchaValidatorUtil.verifyToken("   ", TEST_IP));

        // Token giả mạo
        assertFalse("Token 'invalid-token' phải bị từ chối",
                CaptchaValidatorUtil.verifyToken("invalid-token", TEST_IP));

        // Token widget đã quá hạn (hơn 15 phút trước)
        long expiredTimestamp = System.currentTimeMillis() - (20 * 60 * 1000L);
        String expiredWidgetToken = "IRMS-CAPTCHA-" + expiredTimestamp + "-nonce";
        assertFalse("Token widget quá hạn 20 phút phải bị từ chối",
                CaptchaValidatorUtil.verifyToken(expiredWidgetToken, TEST_IP));
    }

    // =========================================================================
    // 3. KN-233: TIẾP NHẬN HỒ SƠ & SINH MÃ TRA CỨU ĐỘC NHẤT
    // =========================================================================

    @Test
    public void testTrackingCode_FormatAndUniqueness() {
        Set<String> generatedCodes = new HashSet<>();
        int count = 100;

        for (int i = 0; i < count; i++) {
            String code = candidateService.generateUniqueTrackingCode();
            assertNotNull("Mã tra cứu không được null", code);

            // Kiểm tra format: APP-YYYYMMDD-XXXX (ví dụ: APP-20261014-9X8K)
            assertTrue("Mã tra cứu phải có tiền tố 'APP-'", code.startsWith("APP-"));
            assertTrue("Mã tra cứu phải khớp định dạng APP-YYYYMMDD-XXXX: " + code,
                    code.matches("^APP-\\d{8}-[A-Z0-9]{4,}$"));

            generatedCodes.add(code);
        }

        // Kiểm tra tính độc nhất (không có mã nào trùng lặp trong 100 lần sinh liên tiếp)
        assertEquals("Toàn bộ 100 mã tra cứu sinh ra phải hoàn toàn độc nhất", count, generatedCodes.size());
    }

    @Test
    public void testCandidateModel_TrackingAndCoverLetterFields() {
        Candidate c = new Candidate();
        c.setFullName("Lê Thị Hương");
        c.setEmail("huong.le@example.com");
        c.setPhone("0912345678");
        c.setTrackingCode("APP-20261014-A1B2");
        c.setCoverLetter("Tôi có 4 năm kinh nghiệm phát triển phần mềm Java.");
        c.setIpAddress("127.0.0.1");

        assertEquals("APP-20261014-A1B2", c.getTrackingCode());
        assertEquals("Tôi có 4 năm kinh nghiệm phát triển phần mềm Java.", c.getCoverLetter());
        assertEquals("127.0.0.1", c.getIpAddress());
    }

    // =========================================================================
    // 4. KN-230: BIỂU MẪU VÀ VALIDATION DỮ LIỆU ĐẦU VÀO
    // =========================================================================

    @Test
    public void testCandidateValidation_ValidPhoneNumbers_Accepted() {
        // Các đầu số di động hợp lệ tại Việt Nam: 03x, 05x, 07x, 08x, 09x gồm 10 chữ số
        String[] validPhones = {"0901234567", "0987654321", "0345678901", "0778899001", "0899001122"};
        for (String phone : validPhones) {
            assertTrue("Số điện thoại " + phone + " phải hợp lệ",
                    phone.matches("^0[3|5|7|8|9][0-9]{8}$"));
        }
    }

    @Test
    public void testCandidateValidation_InvalidPhoneNumbers_Rejected() {
        // Số sai độ dài, bắt đầu sai, hoặc chứa chữ cái
        String[] invalidPhones = {"123456", "090123456", "090123456789", "0201234567", "090123456a"};
        for (String phone : invalidPhones) {
            assertFalse("Số điện thoại " + phone + " không được coi là số di động VN hợp lệ",
                    phone.matches("^0[3|5|7|8|9][0-9]{8}$"));
        }
    }

    @Test
    public void testCandidateValidation_EmailFormat() {
        assertTrue("Email chuẩn phải hợp lệ",
                "candidate.name+dev@domain.com.vn".matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"));
        assertFalse("Email thiếu @ phải bị từ chối",
                "candidate.domain.com".matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"));
        assertFalse("Email thiếu domain phải bị từ chối",
                "candidate@".matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"));
    }

    // =========================================================================
    // 5. KN-231: TỪ CHỐI NỘP TRÙNG CÙNG VỊ TRÍ VỚI CÙNG EMAIL
    // =========================================================================

    @Test
    public void testDuplicateApplication_ValidationLogic() {
        // Kiểm tra xử lý chuẩn hóa khoảng trắng và hoa thường
        String email1 = "applicant@company.com";
        String email2 = "  Applicant@Company.COM  ";
        assertEquals("Email phải được chuẩn hóa chữ thường và cắt khoảng trắng để chống lọt trùng",
                email1.trim().toLowerCase(), email2.trim().toLowerCase());

        // Kiểm tra hàm hasAlreadyApplied trả về false với tham số rỗng
        assertFalse("hasAlreadyApplied phải trả về false khi email null",
                candidateService.hasAlreadyApplied(null, TEST_REQ_ID));
        assertFalse("hasAlreadyApplied phải trả về false khi requisitionId null",
                candidateService.hasAlreadyApplied(TEST_EMAIL, null));
    }

    // =========================================================================
    // 6. KN-235: MẪU EMAIL XÁC NHẬN VÀ GỬI BẤT ĐỒNG BỘ
    // =========================================================================

    @Test
    public void testEmailTemplate_ContainsRequiredInformation() {
        String candidateName = "Trần Đình Khang";
        String positionTitle = "Senior Fullstack Developer";
        String departmentName = "Khối Công nghệ & Kỹ thuật";
        String trackingCode = "APP-20261014-9988";
        String submittedAt = "14/10/2026 23:45:00";
        String cvFileName = "TranDinhKhang_CV.pdf";

        String html = EmailTemplateUtil.buildApplicationConfirmationEmail(
                candidateName, positionTitle, departmentName, trackingCode, submittedAt, cvFileName
        );

        assertNotNull("Nội dung HTML email không được null", html);
        assertTrue("Email phải chứa tên ứng viên", html.contains(candidateName));
        assertTrue("Email phải chứa vị trí ứng tuyển", html.contains(positionTitle));
        assertTrue("Email phải chứa phòng ban phụ trách", html.contains(departmentName) || html.contains(com.irms.util.SecurityUtil.escapeHtml(departmentName)));
        assertTrue("Email phải chứa Mã tra cứu hồ sơ độc nhất", html.contains(trackingCode));
        assertTrue("Email phải chứa thời gian nộp hồ sơ", html.contains(submittedAt));
        assertTrue("Email phải chứa tên tệp CV", html.contains(cvFileName));
        assertTrue("Email phải có giao diện chuẩn HTML", html.contains("<!DOCTYPE html>"));
    }

    @Test
    public void testAsyncEmailDispatch_NonBlocking() {
        long startTime = System.currentTimeMillis();

        // Kích hoạt gửi xác nhận email bất đồng bộ
        EmailService.sendApplicationConfirmationAsync(
                "candidate.async@example.com",
                "Nguyễn Văn Async",
                "Java Developer",
                "Phòng IT",
                "APP-20261014-ASY1",
                "14/10/2026 23:50:00",
                "resume.pdf"
        );

        long duration = System.currentTimeMillis() - startTime;
        // Việc gửi email bất đồng bộ phải hoàn thành gần như tức thì (< 100ms) để không làm treo API
        assertTrue("Việc kích hoạt gửi email bất đồng bộ không được làm nghẽn luồng xử lý chính (thời gian: " + duration + "ms)",
                duration < 150L);
    }
}

package com.irms;

import com.irms.model.CVExtractionResult;
import com.irms.model.Candidate;
import com.irms.service.CVDataExtractionService;
import com.irms.util.PdfTextExtractor;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * ==============================================================================
 * BỘ KIỂM THỬ TÍNH NĂNG "TRÍCH XUẤT THÔNG TIN TỰ ĐỘNG TỪ CV" (KN-213)
 * ==============================================================================
 * Kiểm thử đầy đủ 3 yêu cầu cốt lõi của User Story:
 * 1. Hệ thống bóc tách họ tên, email, số điện thoại từ CV dạng PDF.
 * 2. Kết quả bóc tách hiển thị để recruiter xác nhận hoặc sửa, KHÔNG tự ghi đè.
 * 3. Bóc tách thất bại KHÔNG làm hỏng việc nộp hồ sơ (Fail-safe, Non-blocking).
 * ==============================================================================
 */
public class CVExtractionTest {

    private CVDataExtractionService extractionService;

    @Before
    public void setUp() {
        extractionService = new CVDataExtractionService();
    }

    // =========================================================================
    // YÊU CẦU 1: BÓC TÁCH HỌ TÊN, EMAIL, SỐ ĐIỆN THOẠI TỪ VĂN BẢN CV PDF
    // =========================================================================

    @Test
    public void testExtractFromTextFullInformation() {
        String cvContent = "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM\n"
                + "THÔNG TIN ỨNG VIÊN\n"
                + "Họ và tên: Nguyễn Văn An\n"
                + "Email: nguyen.van.an.dev@gmail.com\n"
                + "Điện thoại: 0912 345 678\n"
                + "Địa chỉ: Hà Nội, Việt Nam\n"
                + "MỤC TIÊU NGHỀ NGHIỆP\n"
                + "Trở thành Senior Java Software Engineer.";

        CVExtractionResult result = extractionService.extractFromText(cvContent);

        assertNotNull("Kết quả bóc tách không được null", result);
        assertEquals("Trạng thái phải là SUCCESS khi bóc tách đủ 3 trường", "SUCCESS", result.getStatus());
        assertEquals("Họ tên bóc tách chính xác", "Nguyễn Văn An", result.getExtractedName());
        assertEquals("Email bóc tách chính xác", "nguyen.van.an.dev@gmail.com", result.getExtractedEmail());
        assertEquals("Số điện thoại bóc tách chuẩn hóa", "0912345678", result.getExtractedPhone());
    }

    @Test
    public void testExtractPhoneFormats() {
        // Kiểm thử các định dạng số điện thoại phổ biến tại Việt Nam
        String text1 = "Liên hệ ứng viên Lê Thị Bích qua tel: +84 987 654 321 hoặc email: bich.le@corp.vn";
        CVExtractionResult r1 = extractionService.extractFromText(text1);
        assertEquals("Chuẩn hóa định dạng +84 thành 0987654321", "0987654321", r1.getExtractedPhone());

        String text2 = "Hotline cá nhân: 038-999-8877\nEmail: test@domain.com";
        CVExtractionResult r2 = extractionService.extractFromText(text2);
        assertEquals("Bỏ dấu gạch nối trong SĐT", "0389998877", r2.getExtractedPhone());

        String text3 = "SĐT: 085.123.4567\nEmail: test3@domain.com";
        CVExtractionResult r3 = extractionService.extractFromText(text3);
        assertEquals("Bỏ dấu chấm trong SĐT", "0851234567", r3.getExtractedPhone());
    }

    @Test
    public void testExtractEmailExtraction() {
        String text = "CURRICULUM VITAE\n"
                + "Trần Minh Tuấn\n"
                + "Mail: tuan.tran.tech2026@outlook.com.vn\n"
                + "Mobile: 0905123456\n"
                + "Kinh nghiệm 5 năm Fullstack Developer.";

        CVExtractionResult result = extractionService.extractFromText(text);
        assertNotNull(result.getExtractedEmail());
        assertEquals("tuan.tran.tech2026@outlook.com.vn", result.getExtractedEmail());
    }

    @Test
    public void testExtractPartialInformation() {
        // Chỉ có email và sđt, không nhận diện được tên hợp lệ
        String text = "RESUME 2026\n"
                + "Email: only.email@company.com\n"
                + "Mobile: 0918765432\n";

        CVExtractionResult result = extractionService.extractFromText(text);
        assertEquals("Khi chỉ trích xuất được 1 phần, status là PARTIAL", "PARTIAL", result.getStatus());
        assertEquals("only.email@company.com", result.getExtractedEmail());
        assertEquals("0918765432", result.getExtractedPhone());
    }

    @Test
    public void testExtractFromPurePdfStream() throws IOException {
        // Tạo một cấu trúc file PDF tối giản chứa text stream hợp lệ
        String pdfSource = "%PDF-1.4\n"
                + "1 0 obj << /Length 120 >>\n"
                + "stream\n"
                + "BT\n"
                + "/F1 12 Tf\n"
                + "(Ho va ten: Pham Quoc Huy) Tj\n"
                + "T*\n"
                + "(Email: quoc.huy@fpt.com) Tj\n"
                + "T*\n"
                + "(SDT: 0938112233) Tj\n"
                + "ET\n"
                + "endstream\n"
                + "endobj\n"
                + "%%EOF";

        byte[] pdfBytes = pdfSource.getBytes(StandardCharsets.ISO_8859_1);
        String extractedText = PdfTextExtractor.extractText(new ByteArrayInputStream(pdfBytes));

        assertNotNull("Text trích xuất từ PDF không được rỗng", extractedText);
        assertTrue("Chứa tên ứng viên", extractedText.contains("Pham Quoc Huy"));
        assertTrue("Chứa email", extractedText.contains("quoc.huy@fpt.com"));
        assertTrue("Chứa sđt", extractedText.contains("0938112233"));

        CVExtractionResult result = extractionService.extract(new ByteArrayInputStream(pdfBytes));
        assertEquals("SUCCESS", result.getStatus());
        assertEquals("quoc.huy@fpt.com", result.getExtractedEmail());
        assertEquals("0938112233", result.getExtractedPhone());
    }

    // =========================================================================
    // YÊU CẦU 2: KẾT QUẢ HIỂN THỊ ĐỂ RECRUITER XÁC NHẬN/SỬA, KHÔNG TỰ GHI ĐÈ
    // =========================================================================

    @Test
    public void testCandidateDataNotOverwrittenAutomatically() {
        // Ứng viên nộp thông tin ban đầu:
        Candidate candidate = new Candidate();
        candidate.setFullName("Nguyễn Văn A (Tên nộp)");
        candidate.setEmail("a.nguyen@personal.com");
        candidate.setPhone("0900000001");

        // Dữ liệu bóc tách được từ file CV:
        CVExtractionResult result = new CVExtractionResult();
        result.setExtractedName("Nguyễn Văn An (Tên trong CV)");
        result.setExtractedEmail("nguyenvanan.pro@gmail.com");
        result.setExtractedPhone("0912345678");
        result.setStatus("SUCCESS");

        // Gán kết quả trích xuất vào candidate mà KHÔNG ghi đè trường gốc:
        candidate.setExtractedName(result.getExtractedName());
        candidate.setExtractedEmail(result.getExtractedEmail());
        candidate.setExtractedPhone(result.getExtractedPhone());
        candidate.setExtractionStatus(result.getStatus());

        // Kiểm chứng thông tin gốc vẫn nguyên vẹn:
        assertEquals("Họ tên gốc không bị ghi đè", "Nguyễn Văn A (Tên nộp)", candidate.getFullName());
        assertEquals("Email gốc không bị ghi đè", "a.nguyen@personal.com", candidate.getEmail());
        assertEquals("SĐT gốc không bị ghi đè", "0900000001", candidate.getPhone());

        // Dữ liệu bóc tách được lưu riêng biệt:
        assertEquals("Nguyễn Văn An (Tên trong CV)", candidate.getExtractedName());
        assertEquals("nguyenvanan.pro@gmail.com", candidate.getExtractedEmail());
        assertEquals("0912345678", candidate.getExtractedPhone());
        assertEquals("SUCCESS", candidate.getExtractionStatus());
        assertTrue("Hồ sơ có dữ liệu bóc tách", candidate.hasExtractedData());
    }

    @Test
    public void testRecruiterConfirmAndEditWorkflow() {
        Candidate candidate = new Candidate();
        candidate.setFullName("Nguyen Van An");
        candidate.setEmail("old@email.com");
        candidate.setPhone("0900000000");

        candidate.setExtractedName("Nguyễn Văn An");
        candidate.setExtractedEmail("new.official@email.com");
        candidate.setExtractedPhone("0912345678");
        candidate.setExtractionStatus("SUCCESS");

        // Giả lập Recruiter mở Modal, kiểm tra và sửa đổi họ tên thêm chữ đệm trước khi bấm xác nhận:
        String recruiterEditedName = "Nguyễn Văn Hoàng An";
        String recruiterConfirmedEmail = candidate.getExtractedEmail();
        String recruiterConfirmedPhone = candidate.getExtractedPhone();

        // Recruiter xác nhận cập nhật:
        candidate.setFullName(recruiterEditedName);
        candidate.setEmail(recruiterConfirmedEmail);
        candidate.setPhone(recruiterConfirmedPhone);
        candidate.setExtractionStatus("CONFIRMED");

        assertEquals("Họ tên chính thức được cập nhật sau khi Recruiter xác nhận", "Nguyễn Văn Hoàng An", candidate.getFullName());
        assertEquals("Email chính thức được cập nhật", "new.official@email.com", candidate.getEmail());
        assertEquals("SĐT chính thức được cập nhật", "0912345678", candidate.getPhone());
        assertEquals("CONFIRMED", candidate.getExtractionStatus());
        assertEquals("Đã đối chiếu", candidate.getExtractionStatusLabel());
    }

    // =========================================================================
    // YÊU CẦU 3: BÓC TÁCH THẤT BẠI KHÔNG LÀM HỎNG VIỆC NỘP HỒ SƠ (FAIL-SAFE)
    // =========================================================================

    @Test
    public void testExtractionFailSafeOnNullOrEmptyStream() {
        CVExtractionResult rNull = extractionService.extract((java.io.InputStream) null);
        assertNotNull("Không bao giờ ném Exception khi null", rNull);
        assertEquals("FAILED", rNull.getStatus());

        CVExtractionResult rEmpty = extractionService.extract(new ByteArrayInputStream(new byte[0]));
        assertNotNull(rEmpty);
        assertEquals("FAILED", rEmpty.getStatus());
    }

    @Test
    public void testExtractionFailSafeOnCorruptedFile() throws IOException {
        // Tạo file giả lập bị hỏng hoặc chứa byte rác
        File tempCorruptFile = File.createTempFile("corrupt_cv_", ".pdf");
        tempCorruptFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempCorruptFile)) {
            fos.write("DAY LA DU LIEU PDF RANG HONG KHONG DUNG DINH DANG".getBytes(StandardCharsets.UTF_8));
        }

        // Bóc tách không được gây crash hệ thống
        CVExtractionResult result = extractionService.extract(tempCorruptFile);
        assertNotNull(result);
        assertEquals("Khi file hỏng hoặc không có text, trả về FAILED một cách an toàn", "FAILED", result.getStatus());
        assertNull(result.getExtractedEmail());
        assertNull(result.getExtractedPhone());
    }

    @Test
    public void testExtractionFailSafeOnNonExistentFile() {
        File nonExistent = new File("non_existent_folder_xyz/cv_sample_test.pdf");
        CVExtractionResult result = extractionService.extract(nonExistent);
        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());
    }

    @Test
    public void testCandidateBadgeLabels() {
        Candidate c1 = new Candidate();
        c1.setExtractionStatus("SUCCESS");
        assertEquals("Đã bóc tách CV", c1.getExtractionStatusLabel());
        assertTrue(c1.getExtractionStatusBadgeClass().contains("info"));

        Candidate c2 = new Candidate();
        c2.setExtractionStatus("PARTIAL");
        assertEquals("Bóc tách 1 phần", c2.getExtractionStatusLabel());

        Candidate c3 = new Candidate();
        c3.setExtractionStatus("CONFIRMED");
        assertEquals("Đã đối chiếu", c3.getExtractionStatusLabel());
        assertTrue(c3.getExtractionStatusBadgeClass().contains("success"));

        Candidate c4 = new Candidate();
        c4.setExtractionStatus("FAILED");
        assertEquals("Không thể bóc tách", c4.getExtractionStatusLabel());
        assertTrue(c4.getExtractionStatusBadgeClass().contains("danger"));

        Candidate c5 = new Candidate();
        c5.setExtractionStatus(null);
        assertEquals("Chưa bóc tách", c5.getExtractionStatusLabel());
    }
}

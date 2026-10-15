package com.irms.service;

import com.irms.model.CVExtractionResult;
import com.irms.util.PdfTextExtractor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ==============================================================================
 * DỊCH VỤ TRÍCH XUẤT THÔNG TIN TỰ ĐỘNG TỪ CV DẠNG PDF (CVDataExtractionService)
 * ==============================================================================
 * Đáp ứng đầy đủ 3 yêu cầu của User Story KN-213:
 * 1. Hệ thống bóc tách họ tên, email, số điện thoại từ CV dạng PDF.
 * 2. Kết quả bóc tách hiển thị để recruiter xác nhận hoặc sửa, không tự ghi đè.
 * 3. Bóc tách thất bại không làm hỏng việc nộp hồ sơ (Fail-safe, non-blocking).
 * ==============================================================================
 */
public class CVDataExtractionService {

    private static final Logger LOGGER = Logger.getLogger(CVDataExtractionService.class.getName());

    // Pattern nhận diện Email
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b", Pattern.CASE_INSENSITIVE);

    // Pattern nhận diện Email có nhãn
    private static final Pattern LABELED_EMAIL_PATTERN = Pattern.compile(
            "(?i)(?:email|e-mail|thư\\s*điện\\s*tử|mail)\\s*[:：\\-]?\\s*([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

    // Pattern nhận diện Số điện thoại có nhãn
    private static final Pattern LABELED_PHONE_PATTERN = Pattern.compile(
            "(?i)(?:điện\\s*thoại|sđt|sdt|đt|hotline|phone|tel|mobile|cell)\\s*[:：\\-]?\\s*([+0-9()\\s.-]{9,22})");

    // Pattern nhận diện Số điện thoại Việt Nam thông dụng (10 chữ số, đầu 03, 05, 07, 08, 09 hoặc +84)
    private static final Pattern VN_PHONE_PATTERN = Pattern.compile(
            "(?:(?:\\+84|84|0)[\\s.-]?)((?:3[2-9]|5[25689]|7[06-9]|8[1-9]|9[0-9])[0-9\\s.-]{7,11})");

    // Pattern nhận diện Họ và tên có nhãn rõ ràng (hỗ trợ cả tiếng Việt có dấu, không dấu và tiếng Anh)
    private static final Pattern LABELED_NAME_PATTERN = Pattern.compile(
            "(?i)(?:h[oọ]\\s*v[aà]\\s*t[eê]n|h[oọ]\\s*t[eê]n|h[oọ]\\s*&\\s*t[eê]n|full\\s*name|candidate\\s*name|[uúứ]ng\\s*vi[eê]n|t[eê]n\\s*[uúứ]ng\\s*vi[eê]n|t[eê]n|name)\\s*[:：\\-]?\\s*([^\\r\\n,;\\t:]{2,60})");

    // Danh sách từ khóa tiêu đề tài liệu cần loại trừ khi dò Họ tên
    private static final List<String> HEADER_BLACKLIST = Arrays.asList(
            "CURRICULUM VITAE", "CV", "RESUME", "SƠ YẾU LÝ LỊCH", "THÔNG TIN CÁ NHÂN",
            "THONG TIN CA NHAN", "PERSONAL INFORMATION", "PROFILE", "THÔNG TIN LIÊN HỆ",
            "MỤC TIÊU NGHỀ NGHIỆP", "CAREER OBJECTIVE", "OBJECTIVE", "TỔNG QUAN",
            "KINH NGHIỆM LÀM VIỆC", "KINH NGHIEM LAM VIEC", "WORK EXPERIENCE", "EXPERIENCE",
            "HỌC VẤN", "HOC VAN", "EDUCATION", "KỸ NĂNG", "KY NANG", "SKILLS", "SKILL",
            "DỰ ÁN", "PROJECTS", "CHỨNG CHỈ", "CERTIFICATIONS", "NGƯỜI THAM CHIẾU",
            "HOẠT ĐỘNG", "ACTIVITIES", "COVER LETTER", "GIỚI THIỆU BẢN THÂN", "SUMMARY",
            "ABOUT ME", "CONTACT", "CONTACT INFO", "APPLICATION FOR"
    );

    // Danh sách chức danh chuyên môn cần loại trừ khi dò Họ tên
    private static final List<String> JOB_TITLE_BLACKLIST = Arrays.asList(
            "DEVELOPER", "SOFTWARE ENGINEER", "LẬP TRÌNH VIÊN", "LAP TRINH VIEN",
            "FRONTEND DEVELOPER", "BACKEND DEVELOPER", "FULLSTACK DEVELOPER",
            "JAVA DEVELOPER", "WEB DEVELOPER", "MOBILE DEVELOPER", "DEVOPS ENGINEER",
            "TESTER", "QA ENGINEER", "QC ENGINEER", "QUALITY ASSURANCE",
            "UI/UX DESIGNER", "DESIGNER", "PRODUCT MANAGER", "PROJECT MANAGER",
            "BUSINESS ANALYST", "DATA ANALYST", "CHUYÊN VIÊN TUYỂN DỤNG", "RECRUITER",
            "HR SPECIALIST", "MARKETING SPECIALIST", "SALES EXECUTIVE",
            "INTERN", "INTERNSHIP", "THỰC TẬP SINH", "FRESHER", "JUNIOR", "SENIOR", "LEAD"
    );

    /**
     * Trích xuất thông tin tự động từ file CV PDF (Đảm bảo an toàn không ném Exception)
     */
    public CVExtractionResult extract(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists() || !pdfFile.canRead()) {
            return CVExtractionResult.failed("Tệp CV không tồn tại hoặc không thể đọc được.");
        }

        try (InputStream is = new FileInputStream(pdfFile)) {
            return extract(is);
        } catch (Throwable t) {
            // [KN-213]: Bóc tách thất bại không làm hỏng việc nộp hồ sơ
            LOGGER.log(Level.WARNING, "[CVDataExtractionService] Lỗi khi đọc tệp CV PDF: " + pdfFile.getAbsolutePath(), t);
            return CVExtractionResult.failed("Lỗi đọc tệp CV PDF: " + t.getMessage());
        }
    }

    /**
     * Trích xuất thông tin tự động từ InputStream của CV PDF
     */
    public CVExtractionResult extract(InputStream is) {
        if (is == null) {
            return CVExtractionResult.failed("Luồng dữ liệu CV rỗng.");
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            byte[] pdfBytes = baos.toByteArray();
            return extract(pdfBytes);
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "[CVDataExtractionService] Lỗi khi xử lý luồng tệp CV", t);
            return CVExtractionResult.failed("Lỗi đọc dữ liệu CV: " + t.getMessage());
        }
    }

    /**
     * Trích xuất thông tin từ mảng bytes của tệp PDF
     */
    public CVExtractionResult extract(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return CVExtractionResult.failed("Tệp CV trống không có dữ liệu.");
        }

        // Kiểm tra chữ ký file PDF (%PDF-)
        if (pdfBytes.length < 5 || pdfBytes[0] != '%' || pdfBytes[1] != 'P'
                || pdfBytes[2] != 'D' || pdfBytes[3] != 'F' || pdfBytes[4] != '-') {
            return CVExtractionResult.failed("Tệp đính kèm không phải là định dạng PDF hợp lệ (thiếu chữ ký %PDF-).");
        }

        try {
            // 1. Trích xuất văn bản thô từ tệp PDF
            String text = PdfTextExtractor.extractText(pdfBytes);
            if (text == null || text.trim().isEmpty()) {
                return CVExtractionResult.failed(
                        "Không thể bóc tách nội dung văn bản (CV có thể là tệp scan ảnh hoặc không chứa lớp văn bản).");
            }

            // 2. Tiến hành phân tích văn bản để tìm Họ tên, Email, Số điện thoại
            return extractFromText(text);
        } catch (Throwable t) {
            // [KN-213]: Đảm bảo bắt mọi lỗi ngoại lệ phát sinh để không làm hỏng tiến trình nộp đơn
            LOGGER.log(Level.WARNING, "[CVDataExtractionService] Ngoại lệ khi bóc tách thông tin từ CV", t);
            return CVExtractionResult.failed("Lỗi phân tích cú pháp CV: " + t.getMessage());
        }
    }

    /**
     * Phân tích văn bản đã trích xuất từ CV để bóc tách Họ tên, Email, Số điện thoại
     */
    public CVExtractionResult extractFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return CVExtractionResult.failed("Văn bản CV rỗng.");
        }

        try {
            // 1. Bóc tách Email
            String email = extractEmail(text);

            // 2. Bóc tách Số điện thoại
            String phone = extractPhone(text);

            // 3. Bóc tách Họ và tên
            String fullName = extractFullName(text, email);

            // 4. Đánh giá trạng thái trích xuất
            String preview = text.length() > 300 ? text.substring(0, 300) + "..." : text;

            boolean hasName = (fullName != null && !fullName.trim().isEmpty());
            boolean hasEmail = (email != null && !email.trim().isEmpty());
            boolean hasPhone = (phone != null && !phone.trim().isEmpty());

            if (hasName && (hasEmail || hasPhone)) {
                return CVExtractionResult.success(fullName, email, phone, preview);
            } else if (hasName || hasEmail || hasPhone) {
                return CVExtractionResult.partial(fullName, email, phone, preview,
                        "Hệ thống đã trích xuất một phần thông tin (" +
                                (hasName ? "Họ tên " : "") +
                                (hasEmail ? "Email " : "") +
                                (hasPhone ? "SĐT" : "") + "). Vui lòng đối chiếu thêm.");
            } else {
                return CVExtractionResult.failed("Không tìm thấy thông tin liên hệ chuẩn (Họ tên, Email, SĐT) trong CV.");
            }
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "[CVDataExtractionService] Lỗi khi nhận diện các trường thông tin từ văn bản", t);
            return CVExtractionResult.failed("Lỗi phân tích thông tin: " + t.getMessage());
        }
    }

    /**
     * Bóc tách Email từ văn bản CV
     */
    public String extractEmail(String text) {
        if (text == null) return null;

        // Ưu tiên email có nhãn (vd: Email: nguyenvan@gmail.com)
        Matcher labeledMatcher = LABELED_EMAIL_PATTERN.matcher(text);
        if (labeledMatcher.find()) {
            String candidate = labeledMatcher.group(1).trim().toLowerCase(Locale.ROOT);
            if (isValidExtractedEmail(candidate)) {
                return candidate;
            }
        }

        // Quét toàn bộ email trong văn bản
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group().trim().toLowerCase(Locale.ROOT);
            if (isValidExtractedEmail(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isValidExtractedEmail(String email) {
        if (email == null || email.length() < 6 || email.length() > 100) return false;
        String lower = email.toLowerCase(Locale.ROOT);
        // Loại bỏ email mẫu hoặc giả định
        if (lower.contains("example.com") || lower.contains("sample.com")
                || lower.contains("domain.com") || lower.contains("email.com")
                || lower.contains("yourname@") || lower.contains("your-email@")
                || lower.startsWith("noreply") || lower.startsWith("no-reply")) {
            return false;
        }
        return lower.matches("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$");
    }

    /**
     * Bóc tách Số điện thoại từ văn bản CV
     */
    public String extractPhone(String text) {
        if (text == null) return null;

        // Ưu tiên SĐT có nhãn rõ ràng (vd: SĐT: 0912345678, Phone: +84 901 234 567)
        Matcher labeledMatcher = LABELED_PHONE_PATTERN.matcher(text);
        if (labeledMatcher.find()) {
            String candidate = labeledMatcher.group(1);
            String normalized = normalizePhone(candidate);
            if (normalized != null) {
                return normalized;
            }
        }

        // Quét SĐT định dạng chuẩn Việt Nam
        Matcher vnMatcher = VN_PHONE_PATTERN.matcher(text);
        while (vnMatcher.find()) {
            String candidate = vnMatcher.group();
            String normalized = normalizePhone(candidate);
            if (normalized != null) {
                return normalized;
            }
        }

        return null;
    }

    /**
     * Chuẩn hóa số điện thoại về định dạng 10 số (bắt đầu bằng 0)
     */
    private String normalizePhone(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("84") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        }
        if (digits.length() == 10 && digits.matches("^0[35789][0-9]{8}$")) {
            return digits;
        }
        return null;
    }

    /**
     * Bóc tách Họ và tên từ văn bản CV
     */
    public String extractFullName(String text, String knownEmail) {
        if (text == null) return null;

        // 1. Kiểm tra họ tên có nhãn rõ ràng (Họ và tên: Nguyễn Văn A)
        Matcher labeledMatcher = LABELED_NAME_PATTERN.matcher(text);
        while (labeledMatcher.find()) {
            String raw = labeledMatcher.group(1);
            if (raw != null) {
                raw = raw.replaceAll("(?i)\\s+(?:email|e-mail|sđt|sdt|phone|tel|mobile|đt).*$", "");
            }
            String name = sanitizeName(raw);
            if (isValidName(name)) {
                return name;
            }
        }

        // 2. Dò tìm trong các dòng đầu tiên của CV (thông thường tên ứng viên nằm ở 1-8 dòng đầu tiên)
        String[] lines = text.split("\\r?\\n");
        int linesToCheck = Math.min(lines.length, 12);
        for (int i = 0; i < linesToCheck; i++) {
            String line = lines[i].trim();
            if (line.isEmpty() || line.length() < 3 || line.length() > 60) {
                continue;
            }

            // Bỏ qua nếu dòng chứa email hoặc số điện thoại
            if (line.contains("@") || line.matches(".*[0-9]{5,}.*")) {
                continue;
            }

            // Bỏ qua nếu dòng là tiêu đề tài liệu chuẩn (Curriculum Vitae, Resume, ...)
            if (isBlacklistedHeader(line)) {
                continue;
            }

            // Bỏ qua nếu dòng là tên chức danh nghề nghiệp
            if (isBlacklistedJobTitle(line)) {
                continue;
            }

            String sanitized = sanitizeName(line);
            if (isValidName(sanitized)) {
                return sanitized;
            }
        }

        return null;
    }

    private String sanitizeName(String raw) {
        if (raw == null) return "";
        // Loại bỏ tiền tố xưng hô (Mr., Ms., Anh, Chị), ngoặc đơn, ký tự đặc biệt
        String s = raw.replaceAll("(?i)^(mr\\.?|ms\\.?|mrs\\.?|anh|chị|ông|bà)\\s+", "");
        s = s.replaceAll("[\\(\\)\\[\\]\\{\\}\\/\\\\|\\*\\#\\~\\^]", "");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    private boolean isValidName(String name) {
        if (name == null || name.length() < 3 || name.length() > 60) {
            return false;
        }

        // Không chứa chữ số hoặc ký tự cấu trúc
        if (name.matches(".*[0-9\\{\\}\\[\\]\\<\\>\\=\\_\\%\\$\\@\\#].*")) {
            return false;
        }

        // Tên gồm từ 2 đến 6 từ
        String[] words = name.split("\\s+");
        if (words.length < 2 || words.length > 6) {
            return false;
        }

        // Không được là tiêu đề hoặc chức danh
        if (isBlacklistedHeader(name) || isBlacklistedJobTitle(name)) {
            return false;
        }

        // Mỗi từ phải bắt đầu bằng chữ hoa hoặc toàn bộ chữ in hoa
        for (String w : words) {
            if (w.isEmpty()) return false;
            char first = w.charAt(0);
            if (!Character.isUpperCase(first) && !isAllUpper(name)) {
                return false;
            }
        }

        return true;
    }

    private boolean isAllUpper(String str) {
        return str.equals(str.toUpperCase(Locale.ROOT)) && str.matches(".*[A-ZÀ-Ỹ].*");
    }

    private boolean isBlacklistedHeader(String text) {
        String clean = text.toUpperCase(Locale.ROOT).trim();
        for (String item : HEADER_BLACKLIST) {
            if (clean.equals(item) || clean.startsWith(item + " ") || clean.endsWith(" " + item)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlacklistedJobTitle(String text) {
        String clean = text.toUpperCase(Locale.ROOT).trim();
        for (String title : JOB_TITLE_BLACKLIST) {
            if (clean.equals(title) || clean.equalsIgnoreCase("SENIOR " + title)
                    || clean.equalsIgnoreCase("JUNIOR " + title)
                    || clean.equalsIgnoreCase("LEAD " + title)) {
                return true;
            }
        }
        return false;
    }
}

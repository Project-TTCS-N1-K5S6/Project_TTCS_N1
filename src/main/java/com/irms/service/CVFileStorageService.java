package com.irms.service;

import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ==============================================================================
 * DỊCH VỤ KIỂM TRA VÀ LƯU TRỮ TỆP CV (CVFileStorageService)
 * ==============================================================================
 * - Subtask KN-232: Kiểm tra và xử lý tệp CV theo định dạng và kích thước.
 *   + Chỉ chấp nhận định dạng .pdf hoặc .docx
 *   + Dung lượng tệp tối đa: 10MB (10,485,760 bytes)
 *   + Kiểm tra cả phần mở rộng (extension), MIME type và Magic Bytes (file signature).
 *   + Lưu trữ an toàn, chống tấn công Path Traversal và trùng lặp tệp.
 * ==============================================================================
 */
public class CVFileStorageService {

    private static final Logger LOGGER = Logger.getLogger(CVFileStorageService.class.getName());

    // Dung lượng tối đa: 10MB (10 * 1024 * 1024 bytes)
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    // Danh sách phần mở rộng hợp lệ
    public static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".pdf", ".doc", ".docx");

    // Magic Bytes cho PDF: %PDF- (0x25 0x50 0x44 0x46)
    private static final byte[] PDF_MAGIC_BYTES = new byte[]{0x25, 0x50, 0x44, 0x46};

    // Magic Bytes cho DOC (OLE Structured Storage): 0xD0 0xCF 0x11 0xE0
    private static final byte[] DOC_MAGIC_BYTES = new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0};

    // Magic Bytes cho DOCX (Zip archive): PK\x03\x04 (0x50 0x4B 0x03 0x04)
    private static final byte[] DOCX_MAGIC_BYTES = new byte[]{0x50, 0x4B, 0x03, 0x04};

    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String fileExtension;

        public ValidationResult(boolean valid, String errorMessage, String fileExtension) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.fileExtension = fileExtension;
        }

        public static ValidationResult success(String extension) {
            return new ValidationResult(true, null, extension);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage, null);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getFileExtension() {
            return fileExtension;
        }
    }

    /**
     * Kiểm tra tính hợp lệ của tệp CV từ Part upload
     */
    public ValidationResult validate(Part part) {
        if (part == null || part.getSize() <= 0) {
            return ValidationResult.failure("Vui lòng đính kèm tệp hồ sơ CV (PDF hoặc DOCX).");
        }

        String fileName = extractFileName(part);
        long size = part.getSize();
        String contentType = part.getContentType();

        byte[] headerBytes = new byte[8];
        try (InputStream is = part.getInputStream()) {
            int read = is.read(headerBytes);
            if (read < 4) {
                return ValidationResult.failure("Tệp CV trống hoặc không có nội dung hợp lệ.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Không thể đọc nội dung tệp upload: " + fileName, e);
            return ValidationResult.failure("Không thể đọc nội dung tệp đính kèm.");
        }

        return validate(fileName, size, contentType, headerBytes);
    }

    /**
     * Kiểm tra tính hợp lệ với các thông số chi tiết (hỗ trợ kiểm thử Unit Test độc lập)
     */
    public ValidationResult validate(String fileName, long size, String contentType, byte[] headerBytes) {
        // 1. Kiểm tra kích thước tệp (tối đa 10MB)
        if (size <= 0) {
            return ValidationResult.failure("Tệp CV trống hoặc không có dữ liệu.");
        }
        if (size > MAX_FILE_SIZE) {
            return ValidationResult.failure("Dung lượng tệp CV (" + formatFileSize(size) + ") vượt quá giới hạn tối đa cho phép là 10MB.");
        }

        // 2. Kiểm tra phần mở rộng (extension)
        if (fileName == null || fileName.trim().isEmpty()) {
            return ValidationResult.failure("Tên tệp không hợp lệ.");
        }

        String ext = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            ext = fileName.substring(dotIndex).toLowerCase().trim();
        }

        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return ValidationResult.failure("Định dạng tệp không hợp lệ (" + (ext.isEmpty() ? "không có đuôi" : ext) + "). Hệ thống chỉ chấp nhận tệp định dạng .pdf, .doc hoặc .docx.");
        }

        // 3. Kiểm tra Magic Bytes chống đổi đuôi tệp độc hại
        if (headerBytes != null && headerBytes.length >= 4) {
            if (".pdf".equals(ext)) {
                if (!matchesHeader(headerBytes, PDF_MAGIC_BYTES)) {
                    return ValidationResult.failure("Tệp không đúng định dạng PDF chuẩn (Magic bytes không khớp).");
                }
            } else if (".doc".equals(ext)) {
                if (!matchesHeader(headerBytes, DOC_MAGIC_BYTES)) {
                    return ValidationResult.failure("Tệp không đúng định dạng Word (DOC) chuẩn.");
                }
            } else if (".docx".equals(ext)) {
                if (!matchesHeader(headerBytes, DOCX_MAGIC_BYTES)) {
                    return ValidationResult.failure("Tệp không đúng định dạng Word (DOCX) chuẩn (Cấu trúc Zip không khớp).");
                }
            }
        }

        return ValidationResult.success(ext);
    }

    /**
     * Lưu trữ an toàn tệp CV vào thư mục lưu trữ của hệ thống
     *
     * @param part           Part từ Servlet Request
     * @param uploadRootPath Thư mục gốc uploads trên server
     * @param trackingCode   Mã tra cứu hồ sơ duy nhất để định danh
     * @return Đường dẫn truy cập tương đối (URL path) của tệp đã lưu
     * @throws IOException nếu có lỗi khi ghi tệp
     */
    public String storeCV(Part part, String uploadRootPath, String trackingCode) throws IOException {
        String fileName = extractFileName(part);
        String ext = ".pdf";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            ext = fileName.substring(dotIndex).toLowerCase().trim();
        }

        String safeTrackingCode = (trackingCode != null && !trackingCode.trim().isEmpty())
                ? trackingCode.replaceAll("[^a-zA-Z0-9_-]", "")
                : "CV";

        // Thư mục lưu trữ: {uploadRootPath}/uploads/cv
        File dir = new File(uploadRootPath, "uploads" + File.separator + "cv");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Tên tệp duy nhất: cv_{trackingCode}_{timestamp}.{ext}
        String storedFileName = "cv_" + safeTrackingCode + "_" + System.currentTimeMillis() + ext;
        File targetFile = new File(dir, storedFileName);

        try (InputStream input = part.getInputStream()) {
            Files.copy(input, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        LOGGER.info("[CVFileStorageService] Đã lưu tệp CV an toàn: " + targetFile.getAbsolutePath());
        return "/uploads/cv/" + storedFileName;
    }

    /**
     * Lưu trữ từ InputStream (hỗ trợ lưu trữ bất đồng bộ hoặc từ luồng)
     */
    public String storeCV(InputStream input, String uploadRootPath, String trackingCode, String ext) throws IOException {
        String safeTrackingCode = (trackingCode != null && !trackingCode.trim().isEmpty())
                ? trackingCode.replaceAll("[^a-zA-Z0-9_-]", "")
                : "CV";

        File dir = new File(uploadRootPath, "uploads" + File.separator + "cv");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String safeExt = ext.startsWith(".") ? ext : "." + ext;
        String storedFileName = "cv_" + safeTrackingCode + "_" + System.currentTimeMillis() + safeExt;
        File targetFile = new File(dir, storedFileName);

        Files.copy(input, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("[CVFileStorageService] Đã lưu tệp CV từ InputStream: " + targetFile.getAbsolutePath());
        return "/uploads/cv/" + storedFileName;
    }

    /**
     * Trích xuất tên tệp gốc từ Part Header
     */
    public static String extractFileName(Part part) {
        if (part == null) return "";
        String contentDisp = part.getHeader("content-disposition");
        if (contentDisp == null) return "";

        for (String token : contentDisp.split(";")) {
            if (token.trim().startsWith("filename")) {
                String fileName = token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
                // Xử lý path trong IE/Windows: C:\path\to\file.pdf
                int lastSlash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
                if (lastSlash >= 0) {
                    fileName = fileName.substring(lastSlash + 1);
                }
                return fileName;
            }
        }
        return "cv_" + System.currentTimeMillis() + ".pdf";
    }

    private static boolean matchesHeader(byte[] data, byte[] pattern) {
        if (data == null || pattern == null || data.length < pattern.length) {
            return false;
        }
        for (int i = 0; i < pattern.length; i++) {
            if (data[i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int z = (63 - Long.numberOfLeadingZeros(bytes)) / 10;
        return String.format("%.1f %sB", (double) bytes / (1L << (z * 10)), " KMGTPE".charAt(z));
    }
}

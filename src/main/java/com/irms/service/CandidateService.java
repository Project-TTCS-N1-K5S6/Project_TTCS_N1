package com.irms.service;

import com.irms.dao.CandidateDAO;
import com.irms.model.Candidate;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;


public class CandidateService {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Set<String> SOURCES = new HashSet<String>(Arrays.asList(
            "Giới thiệu", "Mạng xã hội", "Sự kiện tuyển dụng", "Nguồn khác"));

    private final CandidateDAO candidateDAO;

    public CandidateService(CandidateDAO candidateDAO) {
        this.candidateDAO = candidateDAO;
    }

    public List<Candidate> findRecent() throws SQLException {
        return candidateDAO.findRecent(20);
    }

    public Candidate create(String name, String email, String phone, String position, String source,
                            String cvName, String storageName, String contentType, String clientIp,
                            boolean allowDuplicate)
            throws SQLException, ValidationException, DuplicateCandidateException {
        String normalizedName = required(name, "Họ và tên", 120);
        String normalizedEmail = required(email, "Email", 254).toLowerCase(Locale.ROOT);
        String displayPhone = required(phone, "Số điện thoại", 30);
        String normalizedPhone = normalizePhone(displayPhone);
        String normalizedPosition = required(position, "Vị trí ứng tuyển", 120);
        String normalizedSource = source == null ? "" : source.trim();

        if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new ValidationException("Email không đúng định dạng.");
        }
        if (normalizedPhone.length() < 8 || normalizedPhone.length() > 15) {
            throw new ValidationException("Số điện thoại phải có từ 8 đến 15 chữ số.");
        }
        if (!SOURCES.contains(normalizedSource)) {
            throw new ValidationException("Nguồn ứng viên không hợp lệ.");
        }

        List<Candidate> duplicates = allowDuplicate
                ? new ArrayList<Candidate>()
                : candidateDAO.findDuplicates(normalizedEmail, normalizedPhone);
        if (!duplicates.isEmpty()) {
            recordHistory("DUPLICATE_BLOCKED", null, normalizedName, normalizedEmail, displayPhone,
                    normalizedSource, cvName != null, "Trùng với " + duplicates.size() + " hồ sơ hiện có.", clientIp);
            throw new DuplicateCandidateException(buildMatches(duplicates, normalizedName, normalizedEmail,
                    normalizedPhone, normalizedPosition));
        }

        Candidate candidate = new Candidate(UUID.randomUUID().toString(), normalizedName,
                normalizedEmail, displayPhone, normalizedPosition,
                normalizedSource, cvName, null);
        try {
            candidateDAO.insert(candidate, normalizedEmail, normalizedPhone, storageName, contentType);
        } catch (SQLIntegrityConstraintViolationException exception) {
            duplicates = candidateDAO.findDuplicates(normalizedEmail, normalizedPhone);
            if (!duplicates.isEmpty()) {
                recordHistory("DUPLICATE_BLOCKED", null, normalizedName, normalizedEmail, displayPhone,
                        normalizedSource, cvName != null, "Trùng khi ghi dữ liệu.", clientIp);
                throw new DuplicateCandidateException(buildMatches(duplicates, normalizedName,
                        normalizedEmail, normalizedPhone, normalizedPosition));
            }
            throw exception;
        }
        recordHistory(allowDuplicate ? "CREATED_DESPITE_DUPLICATE" : "MANUAL_CREATED", candidate.getId(),
                normalizedName, normalizedEmail, displayPhone, normalizedSource, cvName != null,
                allowDuplicate ? "Người dùng xác nhận tạo mới dù có thể trùng." : "Thêm ứng viên thủ công.",
                clientIp);
        return candidate;
    }

    private List<DuplicateMatch> buildMatches(List<Candidate> duplicates, String name, String email,
                                              String normalizedPhone, String position) {
        List<DuplicateMatch> matches = new ArrayList<DuplicateMatch>();
        for (Candidate existing : duplicates) {
            List<String> fields = new ArrayList<String>();
            if (existing.getEmail().equalsIgnoreCase(email)) {
                fields.add("email");
            }
            if (normalizePhone(existing.getPhone()).equals(normalizedPhone)) {
                fields.add("phone");
            }
            matches.add(new DuplicateMatch(existing, fields,
                    existing.getName().equalsIgnoreCase(name),
                    existing.getPosition().equalsIgnoreCase(position)));
        }
        return matches;
    }

    /**
     * Xuất danh sách ứng viên ra file Excel (.xlsx) theo bộ lọc đang áp dụng.
     * <p>
     * Tệp KHÔNG bao gồm các trường nhạy cảm nội bộ:
     * id, email_normalized, phone_normalized, cv_storage_name, cv_content_type.
     *
     * @param position vị trí ứng tuyển (lọc chứa, null = bỏ qua)
     * @param source   nguồn ứng viên (lọc chính xác, null = bỏ qua)
     * @param fromDate ngày bắt đầu YYYY-MM-DD (null = bỏ qua)
     * @param toDate   ngày kết thúc YYYY-MM-DD (null = bỏ qua)
     * @param out      OutputStream để ghi workbook vào response
     */
    public void exportToExcel(String position, String source,
                              String fromDate, String toDate,
                              OutputStream out) throws SQLException, IOException {
        List<Candidate> candidates = candidateDAO.findWithFilters(position, source, fromDate, toDate);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh sách ứng viên");

            // --- Styles ---
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle altStyle = workbook.createCellStyle();
            altStyle.cloneStyleFrom(dataStyle);
            altStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // --- Header row ---
            String[] headers = {
                "STT", "Họ và tên", "Email", "Số điện thoại",
                "Vị trí ứng tuyển", "Nguồn ứng viên", "Tên CV", "Ngày nộp"
            };
            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerStyle);
            }

            // --- Data rows ---
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
            for (int i = 0; i < candidates.size(); i++) {
                Candidate c = candidates.get(i);
                Row row = sheet.createRow(i + 1);
                CellStyle rowStyle = (i % 2 == 1) ? altStyle : dataStyle;

                Cell[] cells = {
                    row.createCell(0), row.createCell(1), row.createCell(2),
                    row.createCell(3), row.createCell(4), row.createCell(5),
                    row.createCell(6), row.createCell(7)
                };
                cells[0].setCellValue(i + 1);
                cells[1].setCellValue(c.getName());
                cells[2].setCellValue(c.getEmail());
                cells[3].setCellValue(c.getPhone());
                cells[4].setCellValue(c.getPosition());
                cells[5].setCellValue(c.getSource());
                cells[6].setCellValue(c.getCvName() != null ? c.getCvName() : "");
                cells[7].setCellValue(c.getCreatedAt() != null
                        ? sdf.format(new Date(c.getCreatedAt().getTime())) : "");
                for (Cell cell : cells) {
                    cell.setCellStyle(rowStyle);
                }
            }

            // --- Auto-size columns ---
            int[] colWidths = { 2000, 8000, 8000, 5000, 9000, 6000, 9000, 7000 };
            for (int col = 0; col < colWidths.length; col++) {
                sheet.setColumnWidth(col, colWidths[col]);
            }

            workbook.write(out);
        }
    }

    /**
     * Tạo tên tệp xuất Excel theo định dạng:
     * candidates_&lt;mãVịTrí&gt;_&lt;ngàyXuất&gt;.xlsx
     * Ví dụ: candidates_ChuyenVienKinhDoanh_20261016.xlsx
     *
     * @param position vị trí ứng tuyển (null hoặc rỗng → "TatCaViTri")
     */
    public static String buildExportFileName(String position) {
        String posCode = (position == null || position.trim().isEmpty())
                ? "TatCaViTri"
                : position.trim()
                          .replaceAll("[^\\p{L}\\p{N}]", "")   // bỏ ký tự đặc biệt
                          .replaceAll("\\s+", "");
        if (posCode.isEmpty()) posCode = "TatCaViTri";
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        return "candidates_" + posCode + "_" + date + ".xlsx";
    }

    private void recordHistory(String action, String candidateId, String name, String email, String phone,
                               String source, boolean hasCv, String details, String clientIp) {
        try {
            candidateDAO.insertHistory(action, candidateId, name, email, phone, source, hasCv, details, clientIp);
        } catch (SQLException exception) {
            // Lịch sử không được làm hỏng luồng nghiệp vụ chính.
            System.err.println("Failed to record candidate history: " + exception.getMessage());
        }
    }

    private String required(String value, String label, int maxLength) throws ValidationException {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new ValidationException(label + " là bắt buộc.");
        }
        if (normalized.length() > maxLength) {
            throw new ValidationException(label + " không được vượt quá " + maxLength + " ký tự.");
        }
        return normalized;
    }

    private String normalizePhone(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }

    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }

    public static class DuplicateMatch {
        private final Candidate candidate;
        private final List<String> matchedFields;
        private final boolean sameName;
        private final boolean samePosition;

        public DuplicateMatch(Candidate candidate, List<String> matchedFields,
                              boolean sameName, boolean samePosition) {
            this.candidate = candidate;
            this.matchedFields = matchedFields;
            this.sameName = sameName;
            this.samePosition = samePosition;
        }

        public Candidate getCandidate() { return candidate; }
        public List<String> getMatchedFields() { return matchedFields; }
        public boolean isSameName() { return sameName; }
        public boolean isSamePosition() { return samePosition; }
    }

    public static class DuplicateCandidateException extends Exception {
        private final List<DuplicateMatch> matches;

        public DuplicateCandidateException(List<DuplicateMatch> matches) {
            super("Đã tồn tại hồ sơ có cùng email hoặc số điện thoại.");
            this.matches = matches;
        }

        public List<DuplicateMatch> getMatches() {
            return matches;
        }
    }
}

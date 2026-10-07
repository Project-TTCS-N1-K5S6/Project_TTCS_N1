package com.irms.service;

import com.irms.dao.DepartmentDAO;
import com.irms.dao.RoleDAO;
import com.irms.dao.UserDAO;
import com.irms.model.Department;
import com.irms.model.ExcelUserRow;
import com.irms.model.Role;
import com.irms.model.User;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * ==============================================================================
 * DỊCH VỤ XỬ LÝ NHẬP DANH SÁCH NHÂN SỰ TỪ FILE EXCEL (UserExcelService)
 * ==============================================================================
 * Tuân thủ nghiêm ngặt các nguyên tắc:
 * 1. Đọc và tạo đúng chuẩn file Excel .xlsx gồm 2 sheet: DanhSachNhanSu & HuongDan.
 * 2. Validate 2 lớp: trùng lặp trong file và trùng lặp trong cơ sở dữ liệu.
 * 3. Đối chiếu danh mục vai trò và phòng ban với database; hỗ trợ nhiều vai trò (phân tách bởi ',' hoặc ';').
 * 4. Không tự tạo role/department mới nếu không tồn tại.
 * 5. Báo lỗi chi tiết theo từng dòng; cho phép Import Partial Success (nhập các dòng hợp lệ, bỏ qua dòng lỗi).
 * 6. Tái sử dụng 100% logic userService.createUser(...) để đảm bảo tạo tài khoản, sinh mật khẩu tạm,
 *    gửi email kích hoạt và ghi nhật ký kiểm toán giống hệt tạo thủ công.
 * ==============================================================================
 */
public class UserExcelService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private final UserService userService = new UserService();
    private final UserDAO userDAO = new UserDAO();
    private final RoleDAO roleDAO = new RoleDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();

    /**
     * Tạo file Excel mẫu chính thức (.xlsx) phục vụ tải về
     * Gồm Sheet 1 "DanhSachNhanSu" và Sheet 2 "HuongDan"
     */
    public void generateTemplate(OutputStream out) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            // ==========================================
            // SHEET 1: DANH SÁCH NHÂN SỰ CẦN NHẬP
            // ==========================================
            Sheet sheet = workbook.createSheet("DanhSachNhanSu");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Data Style (Text format cho Mã NV và SĐT để bảo toàn số 0 ở đầu)
            DataFormat dataFormat = workbook.createDataFormat();
            CellStyle textStyle = workbook.createCellStyle();
            textStyle.setDataFormat(dataFormat.getFormat("@"));
            textStyle.setBorderTop(BorderStyle.THIN);
            textStyle.setBorderBottom(BorderStyle.THIN);
            textStyle.setBorderLeft(BorderStyle.THIN);
            textStyle.setBorderRight(BorderStyle.THIN);

            CellStyle normalStyle = workbook.createCellStyle();
            normalStyle.setBorderTop(BorderStyle.THIN);
            normalStyle.setBorderBottom(BorderStyle.THIN);
            normalStyle.setBorderLeft(BorderStyle.THIN);
            normalStyle.setBorderRight(BorderStyle.THIN);

            // Dòng Tiêu đề (Header)
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(28);

            String[] headers = {
                    "Mã NV",
                    "Vai trò phân quyền",
                    "Họ và tên",
                    "Email công ty",
                    "Số điện thoại",
                    "Chức vụ / Vị trí",
                    "Phòng ban trực thuộc"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Dữ liệu mẫu tham khảo
            String[][] sampleData = {
                    {
                            "EMP020",
                            "Chuyên viên tuyển dụng",
                            "Nguyễn Văn An",
                            "an.nv@company.local",
                            "0901000020",
                            "Chuyên viên tuyển dụng",
                            "Ban Nhân sự & Tuyển dụng"
                    },
                    {
                            "EMP021",
                            "Người phỏng vấn",
                            "Trần Minh Châu",
                            "chau.tm@company.local",
                            "0901000021",
                            "Chuyên viên chuyên môn",
                            "Khối Công nghệ Thông tin"
                    },
                    {
                            "EMP022",
                            "Chuyên viên tuyển dụng, Người phỏng vấn",
                            "Bùi Ngọc Ánh",
                            "anh.bn@company.local",
                            "0901000022",
                            "Senior Recruiter & Interviewer",
                            "Ban Nhân sự & Tuyển dụng"
                    },
                    {
                            "EMP023",
                            "Ứng viên",
                            "Đỗ Minh Khang",
                            "khang.dm@company.local",
                            "0901000023",
                            "Ứng viên",
                            ""
                    }
            };

            for (int r = 0; r < sampleData.length; r++) {
                Row row = sheet.createRow(r + 1);
                row.setHeightInPoints(20);
                for (int c = 0; c < sampleData[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(sampleData[r][c]);
                    if (c == 0 || c == 4) {
                        cell.setCellStyle(textStyle);
                    } else {
                        cell.setCellStyle(normalStyle);
                    }
                }
            }

            int[] colWidths = {16, 38, 25, 32, 18, 30, 32};
            for (int i = 0; i < colWidths.length; i++) {
                sheet.setColumnWidth(i, colWidths[i] * 256);
            }

            // ==========================================
            // SHEET 2: HƯỚNG DẪN VÀ DANH MỤC THAM CHIẾU
            // ==========================================
            Sheet guideSheet = workbook.createSheet("HuongDan");

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);
            titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFont(titleFont);

            CellStyle boldStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldStyle.setFont(boldFont);

            int gRow = 0;
            Row rTitle = guideSheet.createRow(gRow++);
            Cell cTitle = rTitle.createCell(0);
            cTitle.setCellValue("HƯỚNG DẪN ĐIỀN THÔNG TIN TẬP TIN NHẬP NHÂN SỰ");
            cTitle.setCellStyle(titleStyle);

            gRow++; // Blank row

            Row rNote1 = guideSheet.createRow(gRow++);
            rNote1.createCell(0).setCellValue("1. QUY TẮC CÁC CỘT DỮ LIỆU:");
            rNote1.getCell(0).setCellStyle(boldStyle);

            String[][] colRules = {
                    {"Mã NV", "Bắt buộc. Định dạng chuỗi (ví dụ: EMP010). Không được trùng trong file và trong hệ thống."},
                    {"Vai trò phân quyền", "Bắt buộc. Tên hoặc Mã vai trò. Có thể gán nhiều vai trò bằng cách ngăn cách bởi dấu phẩy (,) hoặc chấm phẩy (;)."},
                    {"Họ và tên", "Bắt buộc. Họ và tên đầy đủ của nhân sự."},
                    {"Email công ty", "Bắt buộc. Định dạng email hợp lệ (ví dụ: user@company.local). Không được trùng lặp."},
                    {"Số điện thoại", "Tùy chọn. Chuỗi số điện thoại liên hệ."},
                    {"Chức vụ / Vị trí", "Tùy chọn. Tên chức vụ chuyên môn (ví dụ: Trưởng nhóm, Recruiter)."},
                    {"Phòng ban trực thuộc", "Tùy chọn (với ứng viên có thể để trống). Tên hoặc Mã phòng ban hợp lệ trong hệ thống."}
            };

            for (String[] cr : colRules) {
                Row rRule = guideSheet.createRow(gRow++);
                rRule.createCell(0).setCellValue("- " + cr[0] + ":");
                rRule.getCell(0).setCellStyle(boldStyle);
                rRule.createCell(1).setCellValue(cr[1]);
            }

            gRow++; // Blank row

            Row rNote2 = guideSheet.createRow(gRow++);
            rNote2.createCell(0).setCellValue("2. DANH MỤC VAI TRÒ HỢP LỆ TRONG HỆ THỐNG:");
            rNote2.getCell(0).setCellStyle(boldStyle);

            List<Role> dbRoles = roleDAO.findAll();
            for (Role role : dbRoles) {
                Row rR = guideSheet.createRow(gRow++);
                rR.createCell(0).setCellValue("  • " + role.getName() + " (" + role.getCode() + ")");
                rR.createCell(1).setCellValue(role.getDescription() != null ? role.getDescription() : "");
            }

            gRow++; // Blank row

            Row rNote3 = guideSheet.createRow(gRow++);
            rNote3.createCell(0).setCellValue("3. DANH MỤC PHÒNG BAN HỢP LỆ TRONG HỆ THỐNG:");
            rNote3.getCell(0).setCellStyle(boldStyle);

            List<Department> dbDepts = departmentDAO.findAll();
            for (Department dept : dbDepts) {
                Row rD = guideSheet.createRow(gRow++);
                rD.createCell(0).setCellValue("  • " + dept.getName() + " (" + dept.getCode() + ")");
                rD.createCell(1).setCellValue(dept.getDescription() != null ? dept.getDescription() : "");
            }

            guideSheet.setColumnWidth(0, 42 * 256);
            guideSheet.setColumnWidth(1, 65 * 256);

            workbook.write(out);
        }
    }

    /**
     * Đọc file Excel, validate 2 lớp và trả về danh sách kết quả Preview chi tiết
     */
    public Map<String, Object> parseAndValidate(InputStream inputStream) throws Exception {
        List<ExcelUserRow> resultRows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        // 1. Tải danh mục Role và Department từ DB vào RAM để tra cứu nhanh
        List<Role> allRoles = roleDAO.findAll();
        List<Department> allDepts = departmentDAO.findAll();

        Map<String, Role> roleByNameMap = new HashMap<>();
        Map<String, Role> roleByCodeMap = new HashMap<>();
        for (Role r : allRoles) {
            if (r.getName() != null) roleByNameMap.put(normalize(r.getName()), r);
            if (r.getCode() != null) roleByCodeMap.put(normalize(r.getCode()), r);
        }

        Map<String, Department> deptByNameMap = new HashMap<>();
        Map<String, Department> deptByCodeMap = new HashMap<>();
        for (Department d : allDepts) {
            if (d.getName() != null) deptByNameMap.put(normalize(d.getName()), d);
            if (d.getCode() != null) deptByCodeMap.put(normalize(d.getCode()), d);
        }

        // 2. Tải toàn bộ Mã NV và Email hiện có trong DB (gom kiểm tra trong RAM)
        Set<String> dbEmployeeCodes = userDAO.getAllEmployeeCodes();
        Set<String> dbEmails = userDAO.getAllEmails();

        // 3. Set phát hiện trùng lặp ngay trong file Excel
        Set<String> fileEmployeeCodes = new HashSet<>();
        Set<String> fileEmails = new HashSet<>();

        // 4. Mở và đọc workbook
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            // Tìm sheet "DanhSachNhanSu" hoặc sheet đầu tiên phù hợp
            Sheet sheet = workbook.getSheet("DanhSachNhanSu");
            if (sheet == null) {
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    String sName = workbook.getSheetName(i).toLowerCase();
                    if (sName.contains("nhansu") || sName.contains("danhsach") || sName.contains("nhân sự")) {
                        sheet = workbook.getSheetAt(i);
                        break;
                    }
                }
            }
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                throw new Exception("File Excel không có dữ liệu.");
            }

            // Kiểm tra dòng Header (dòng 0)
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new Exception("File Excel thiếu dòng tiêu đề (header). Vui lòng sử dụng file mẫu chuẩn.");
            }

            int lastRowNum = sheet.getLastRowNum();
            for (int r = 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String empCode = getCellString(row.getCell(0), formatter);
                String rolesRaw = getCellString(row.getCell(1), formatter);
                String fullName = getCellString(row.getCell(2), formatter);
                String email = getCellString(row.getCell(3), formatter);
                String phone = getCellString(row.getCell(4), formatter);
                String jobTitle = getCellString(row.getCell(5), formatter);
                String deptName = getCellString(row.getCell(6), formatter);

                // Chuẩn hóa định dạng số điện thoại nếu bị mất số 0 ở đầu
                if (phone.matches("^[1-9][0-9]{8}$")) {
                    phone = "0" + phone;
                }

                // Nếu dòng hoàn toàn trống thì bỏ qua
                if (empCode.isEmpty() && rolesRaw.isEmpty() && fullName.isEmpty() &&
                        email.isEmpty() && phone.isEmpty() && jobTitle.isEmpty() && deptName.isEmpty()) {
                    continue;
                }

                ExcelUserRow rowData = new ExcelUserRow();
                rowData.setRowNumber(r + 1); // 1-indexed trong Excel
                rowData.setEmployeeCode(empCode);
                rowData.setRolesRaw(rolesRaw);
                rowData.setFullName(fullName);
                rowData.setEmail(email);
                rowData.setPhone(phone);
                rowData.setJobTitle(jobTitle);
                rowData.setDepartmentName(deptName);

                // --- VALIDATION TỪNG TRƯỜNG ---

                // 1. Mã NV
                if (empCode.isEmpty()) {
                    rowData.addError("Thiếu Mã NV");
                } else {
                    String normCode = empCode.toUpperCase();
                    if (fileEmployeeCodes.contains(normCode)) {
                        rowData.addError("Mã nhân viên '" + empCode + "' bị trùng lặp trong file");
                    } else {
                        fileEmployeeCodes.add(normCode);
                    }

                    if (dbEmployeeCodes.contains(normCode)) {
                        rowData.addError("Mã nhân viên '" + empCode + "' đã tồn tại trong hệ thống");
                    }
                }

                // 2. Họ và tên
                if (fullName.isEmpty()) {
                    rowData.addError("Thiếu Họ và tên");
                }

                // 3. Email công ty
                if (email.isEmpty()) {
                    rowData.addError("Thiếu Email công ty");
                } else {
                    if (!EMAIL_PATTERN.matcher(email).matches()) {
                        rowData.addError("Email không hợp lệ");
                    }

                    String normEmail = email.toLowerCase();
                    if (fileEmails.contains(normEmail)) {
                        rowData.addError("Email '" + email + "' bị trùng lặp trong file");
                    } else {
                        fileEmails.add(normEmail);
                    }

                    if (dbEmails.contains(normEmail)) {
                        rowData.addError("Email '" + email + "' đã tồn tại trong hệ thống");
                    }
                }

                // 4. Vai trò phân quyền (Hỗ trợ nhiều vai trò phân tách bởi ',' hoặc ';')
                if (rolesRaw.isEmpty()) {
                    rowData.addError("Thiếu Vai trò phân quyền");
                } else {
                    // Hỗ trợ cả dấu phẩy, chấm phẩy và xuống dòng
                    String[] roleParts = rolesRaw.split("[,;\\n/]+");
                    List<String> matchedRoleIds = new ArrayList<>();
                    List<String> matchedRoleNames = new ArrayList<>();
                    boolean allRolesFound = true;

                    for (String part : roleParts) {
                        String cleanPart = part != null ? part.trim() : "";
                        if (cleanPart.isEmpty()) continue;

                        String normRole = normalize(cleanPart);
                        Role matchedRole = roleByNameMap.get(normRole);
                        if (matchedRole == null) {
                            matchedRole = roleByCodeMap.get(normRole);
                        }

                        if (matchedRole == null) {
                            rowData.addError("Vai trò '" + cleanPart + "' không tồn tại trong hệ thống");
                            allRolesFound = false;
                        } else {
                            if (!matchedRoleIds.contains(matchedRole.getId())) {
                                matchedRoleIds.add(matchedRole.getId());
                                matchedRoleNames.add(matchedRole.getName());
                            }
                        }
                    }

                    if (matchedRoleIds.isEmpty() && allRolesFound) {
                        rowData.addError("Thiếu Vai trò phân quyền");
                    } else {
                        rowData.setRoleIds(matchedRoleIds);
                        rowData.setRoleNames(matchedRoleNames);
                    }
                }

                // 5. Phòng ban trực thuộc
                if (!deptName.isEmpty()) {
                    String normDept = normalize(deptName);
                    Department matchedDept = deptByNameMap.get(normDept);
                    if (matchedDept == null) {
                        matchedDept = deptByCodeMap.get(normDept);
                    }

                    if (matchedDept == null) {
                        rowData.addError("Phòng ban '" + deptName + "' không tồn tại trong hệ thống");
                    } else {
                        rowData.setDepartmentId(matchedDept.getId());
                        rowData.setDepartmentName(matchedDept.getName());
                    }
                } else {
                    rowData.setDepartmentId(null);
                }

                resultRows.add(rowData);
            }
        }

        int totalRows = resultRows.size();
        int validRows = 0;
        int errorRows = 0;
        for (ExcelUserRow r : resultRows) {
            if (r.isValid()) {
                validRows++;
            } else {
                errorRows++;
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalRows", totalRows);
        response.put("validRows", validRows);
        response.put("errorRows", errorRows);
        response.put("rows", resultRows);
        return response;
    }

    /**
     * Thực hiện Import các dòng hợp lệ (Partial Success)
     * Gọi lại chính xác userService.createUser(...) để đảm bảo tạo dữ liệu,
     * mật khẩu tạm, email kích hoạt và nhật ký kiểm toán giống hệt tạo thủ công.
     */
    public Map<String, Object> executeImport(List<ExcelUserRow> rows, String adminId, String ip, String userAgent) {
        int totalRows = rows != null ? rows.size() : 0;
        int successCount = 0;
        int skippedCount = 0;
        List<Map<String, Object>> failedList = new ArrayList<>();

        if (rows != null) {
            for (ExcelUserRow row : rows) {
                if (!row.isValid()) {
                    skippedCount++;
                    Map<String, Object> item = new HashMap<>();
                    item.put("rowNumber", row.getRowNumber());
                    item.put("employeeCode", row.getEmployeeCode());
                    item.put("fullName", row.getFullName());
                    item.put("email", row.getEmail());
                    item.put("errors", row.getErrors());
                    failedList.add(item);
                    continue;
                }

                try {
                    User u = new User();
                    String empCode = row.getEmployeeCode() != null ? row.getEmployeeCode().trim() : null;
                    if (empCode != null && empCode.isEmpty()) empCode = null;
                    u.setEmployeeCode(empCode);

                    u.setFullName(row.getFullName() != null ? row.getFullName().trim() : "");
                    u.setEmail(row.getEmail() != null ? row.getEmail().trim() : "");

                    String phone = row.getPhone() != null ? row.getPhone().trim() : null;
                    if (phone != null && phone.isEmpty()) phone = null;
                    if (phone != null && phone.matches("^[1-9][0-9]{8}$")) {
                        phone = "0" + phone;
                    }
                    u.setPhone(phone);

                    String jobTitle = row.getJobTitle() != null ? row.getJobTitle().trim() : null;
                    if (jobTitle != null && jobTitle.isEmpty()) jobTitle = null;
                    u.setJobTitle(jobTitle);

                    u.setDepartmentId(row.getDepartmentId());
                    u.setStatus("ACTIVE");

                    boolean ok = userService.createUser(u, row.getRoleIds(), adminId, ip, userAgent);
                    if (ok) {
                        successCount++;
                    } else {
                        skippedCount++;
                        Map<String, Object> item = new HashMap<>();
                        item.put("rowNumber", row.getRowNumber());
                        item.put("employeeCode", row.getEmployeeCode());
                        item.put("fullName", row.getFullName());
                        item.put("email", row.getEmail());
                        item.put("errors", Collections.singletonList("Lỗi ghi dữ liệu vào CSDL"));
                        failedList.add(item);
                    }
                } catch (Exception e) {
                    skippedCount++;
                    Map<String, Object> item = new HashMap<>();
                    item.put("rowNumber", row.getRowNumber());
                    item.put("employeeCode", row.getEmployeeCode());
                    item.put("fullName", row.getFullName());
                    item.put("email", row.getEmail());
                    item.put("errors", Collections.singletonList(e.getMessage()));
                    failedList.add(item);
                }
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRows", totalRows);
        summary.put("successCount", successCount);
        summary.put("skippedCount", skippedCount);
        summary.put("failedList", failedList);
        return summary;
    }

    private String getCellString(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        String val = formatter.formatCellValue(cell);
        return val != null ? val.trim() : "";
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replace('\u00A0', ' ')
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");
    }
}

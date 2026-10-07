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
 * 1. Đọc và tạo đúng chuẩn file Excel .xlsx.
 * 2. Validate 2 lớp: trùng lặp trong file và trùng lặp trong cơ sở dữ liệu.
 * 3. Đối chiếu danh mục vai trò và phòng ban với database; hỗ trợ nhiều vai trò (cách nhau bởi ';').
 * 4. Không tạo role/department mới nếu không tồn tại.
 * 5. Báo lỗi chi tiết theo từng dòng; cho phép Import Partial Success (nhập các dòng hợp lệ, bỏ qua dòng lỗi).
 * 6. Tái sử dụng 100% logic userService.createUser(...) hiện tại để tạo tài khoản, sinh mật khẩu tạm,
 *    gửi email kích hoạt và ghi nhật ký kiểm toán.
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
     */
    public void generateTemplate(OutputStream out) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("DanhSachNhanSu");

            // Tạo Header Style
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

            // Tạo Data Style (Text format cho Mã NV và Phone để không mất số 0)
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

            // 1. Dòng Header
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

            // 2. Dòng dữ liệu mẫu minh họa
            String[][] sampleData = {
                    {
                            "EMP010",
                            "Quản trị hệ thống",
                            "Nguyễn Văn An",
                            "an.nv@company.local",
                            "0901000010",
                            "Chuyên viên Quản trị Hệ thống",
                            "Khối Công nghệ Thông tin"
                    },
                    {
                            "EMP011",
                            "Chuyên viên tuyển dụng;Người phỏng vấn",
                            "Trần Minh Châu",
                            "chau.tm@company.local",
                            "0901000011",
                            "Senior IT Recruiter",
                            "Ban Nhân sự & Tuyển dụng"
                    },
                    {
                            "EMP012",
                            "Trưởng bộ phận chuyên môn",
                            "Lê Hoàng Long",
                            "long.lh@company.local",
                            "0901000012",
                            "Tech Lead / Solution Architect",
                            "Khối Công nghệ Thông tin"
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

            // Thiết lập độ rộng cột hợp lý
            int[] colWidths = {15, 38, 25, 30, 18, 30, 32};
            for (int i = 0; i < colWidths.length; i++) {
                sheet.setColumnWidth(i, colWidths[i] * 256);
            }

            workbook.write(out);
        }
    }

    /**
     * Đọc file Excel, validate 2 lớp và trả về danh sách kết quả Preview
     */
    public Map<String, Object> parseAndValidate(InputStream inputStream) throws Exception {
        List<ExcelUserRow> resultRows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        // 1. Tải trước danh mục Role và Department từ DB vào RAM
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

        // 2. Tải toàn bộ Mã NV và Email hiện có trong DB (đáp ứng Mục 14: gom kiểm tra trong RAM)
        Set<String> dbEmployeeCodes = userDAO.getAllEmployeeCodes();
        Set<String> dbEmails = userDAO.getAllEmails();

        // 3. Set lưu trữ để phát hiện trùng lặp ngay trong file Excel
        Set<String> fileEmployeeCodes = new HashSet<>();
        Set<String> fileEmails = new HashSet<>();

        // 4. Mở và đọc workbook
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
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

                // 4. Vai trò phân quyền (Hỗ trợ nhiều vai trò: 'Vai trò 1;Vai trò 2')
                if (rolesRaw.isEmpty()) {
                    rowData.addError("Thiếu Vai trò phân quyền");
                } else {
                    String[] roleParts = rolesRaw.split(";");
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
                    u.setEmployeeCode(row.getEmployeeCode());
                    u.setFullName(row.getFullName());
                    u.setEmail(row.getEmail());
                    u.setPhone(row.getPhone());
                    u.setJobTitle(row.getJobTitle());
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
        return s.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}

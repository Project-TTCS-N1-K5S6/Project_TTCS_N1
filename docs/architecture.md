# KIẾN TRÚC HỆ THỐNG IRMS (JAVA SERVLET, MYSQL & BOOTSTRAP 5 SASS)

## 1. TỔNG QUAN KIẾN TRÚC

Hệ thống Tuyển dụng Nội bộ (IRMS) áp dụng mô hình **MVC (Model - View - Controller)** kinh điển kết hợp **Layered Architecture** chuẩn mực của hệ sinh thái Java Enterprise:

```
+-------------------------------------------------------------------------+
|                           CLIENT / PRESENTATION                         |
|             HTML5 + CSS3 + Bootstrap 5.3 + JavaScript + SASS            |
|    - Giao diện Responsive chuẩn doanh nghiệp (Mobile drawer đến Desktop)|
|    - SASS Design Tokens (_variables, _layout, _components, _tables)     |
|    - AJAX Fetch API tương tác mượt mà không tải lại trang               |
|    - Dynamic Modal: Thêm người dùng, Khóa tài khoản, Phân quyền         |
+-------------------------------------------------------------------------+
                                   |  HTTP Request / Session / Cookie
                                   v
+-------------------------------------------------------------------------+
|                       FILTER & SECURITY LAYER (JAVA)                    |
|  - EncodingFilter: Ép buộc UTF-8 request & response                     |
|  - AuthFilter: Xác thực Session & Phân quyền truy cập 10 phân hệ        |
|  - MustChangePasswordGuard: Chặn truy cập nếu chưa đổi mật khẩu khởi tạo|
|  - InterviewerRestriction: Cấm người phỏng vấn xem dải lương ngân sách  |
+-------------------------------------------------------------------------+
                                   |
                                   v
+-------------------------------------------------------------------------+
|                      CONTROLLER LAYER (JAVA SERVLET)                    |
|  - DashboardServlet: Thống kê số liệu KPI tổng quan (/dashboard)        |
|  - AuthServlet: Đăng nhập, đăng xuất, đổi & cấp lại mật khẩu (/auth/*)  |
|  - UserServlet: Quản trị tài khoản, khóa & mở khóa (/admin/users)       |
|  - RoleServlet & PermissionMatrixServlet: Ma trận phân quyền RBAC       |
|  - DepartmentServlet: Quản lý danh mục phòng ban (/admin/departments)   |
|  - CandidateServlet: Quản lý pipeline ứng viên (/candidates)            |
|  - SalaryRangeServlet: Quản lý dải lương ngân sách (/salary-ranges)     |
|  - AuditLogServlet: Tra cứu nhật ký kiểm toán (/admin/audit-logs)       |
+-------------------------------------------------------------------------+
                                   |
                                   v
+-------------------------------------------------------------------------+
|                       SERVICE LAYER (BUSINESS LOGIC)                    |
|  - AuthService: Xác thực BCrypt, chống Brute-force (khóa 15p sau 5 lần) |
|  - UserService: Xử lý nghiệp vụ người dùng, mã nhân viên                |
|  - RolePermissionService: Xử lý ma trận phân quyền 10 phân hệ           |
|  - CandidateService: Quản trị pipeline và giai đoạn ứng tuyển           |
|  - AuditService: Ghi nhận nhật ký kiểm toán hệ thống                    |
+-------------------------------------------------------------------------+
                                   |
                                   v
+-------------------------------------------------------------------------+
|                       DATA ACCESS LAYER (DAO / JDBC)                    |
|  - UserDAO, RoleDAO, PermissionDAO, DepartmentDAO, CandidateDAO...      |
|  - BaseDAO: Quản lý vòng đời kết nối và giải phóng tài nguyên           |
|  - HikariCP Connection Pool: Tối ưu hiệu năng kết nối cơ sở dữ liệu     |
|  - MySQL 8.0 Database (ttcs_db): Charset utf8mb4_unicode_ci             |
+-------------------------------------------------------------------------+
```

---

## 2. CƠ CHẾ BẢO MẬT & QUẢN LÝ PHIÊN (SECURITY ARCHITECTURE)

### A. Quản lý Phiên làm việc (Session Management)
1. **HttpOnly Cookie & Session Timeout:**
   - Phiên làm việc sử dụng `HttpSession` của Java Servlet, lưu ID phiên qua Cookie `JSESSIONID` với cờ `HttpOnly`, ngăn chặn mã độc JavaScript đánh cắp session.
   - Thời gian sống mặc định của phiên: **60 phút** (cấu hình trong `web.xml`).
2. **Cơ chế Vô hiệu hóa tức thời (Session Invalidation on Security Event):**
   - Bảng `users` có cột `session_version INT NOT NULL DEFAULT 0`.
   - Mỗi khi đổi mật khẩu, cấp lại mật khẩu hoặc khóa tài khoản: `session_version = session_version + 1`.

### B. Kiểm soát Đăng nhập & Chống dò mật khẩu (Brute-force Protection)
- Mỗi lần người dùng nhập sai mật khẩu:
  - Cột `failed_login_attempts` trong bảng `users` tự động tăng 1.
  - Ghi sự kiện `LOGIN_FAILED` vào bảng `audit_logs`.
  - Thông báo số lần thử còn lại cho người dùng.
- Nếu nhập sai liên tiếp **5 lần**:
  - Đặt `locked_until = NOW() + INTERVAL 15 MINUTE`.
  - Đặt `status = 'LOCKED'`.
  - Người dùng bị khóa trong 15 phút, từ chối đăng nhập với thông báo rõ thời gian mở lại.
- Khi đăng nhập thành công:
  - Tự động reset `failed_login_attempts = 0`, `locked_until = NULL`.
  - Cập nhật `last_login_at = NOW()`.
  - Ghi sự kiện `LOGIN_SUCCESS` vào `audit_logs`.

### C. Đổi mật khẩu bắt buộc (Must Change Password Flow)
- Áp dụng khi Quản trị viên tạo mới tài khoản hoặc Cấp lại mật khẩu tạm thời (`must_change_password = true`).
- `AuthFilter` tự động phát hiện và chặn người dùng truy cập mọi trang chức năng khác, ép buộc chuyển hướng tới `/auth/change-password?required=true`.
- Sau khi đổi thành công sang mật khẩu mới: `must_change_password` chuyển về `false`.

### D. Ma trận phân quyền 10 Phân hệ chuẩn (RBAC Matrix)
- 10 Phân hệ chuẩn nghiệp vụ:
  1. `departments`: Tổ chức & vị trí
  2. `requisitions`: Yêu cầu tuyển dụng
  3. `job_postings`: Tin tuyển dụng
  4. `candidates`: Hồ sơ ứng viên & Pipeline
  5. `interviews`: Lịch phỏng vấn
  6. `evaluations`: Phiếu đánh giá
  7. `offers`: Thư mời nhận việc
  8. `notifications`: Email & Thông báo
  9. `reports`: Báo cáo & Thống kê
  10. `users / roles / permissions / audit`: Quản trị hệ thống
- `AuthFilter` kiểm tra quyền theo phương thức `currentUser.hasPermission("...")` trước khi cho phép vào Servlet xử lý.

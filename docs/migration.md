# BÁO CÁO CHI TIẾT CHUYỂN ĐỔI TOÀN BỘ DỰ ÁN (MIGRATION REPORT)
**Dự án:** Hệ thống Tuyển dụng Nội bộ (IRMS - Internal Recruitment Management System)  
**Thời gian thực hiện:** Tháng 10/2026  
**Kiến trúc sư & Kỹ sư thực hiện:** Senior Software Architect & Senior Full-Stack Engineer  

---

## 1. CÔNG NGHỆ CŨ VS CÔNG NGHỆ MỚI

| Thành phần | Công nghệ cũ (Trước Migration) | Công nghệ mới (Sau Migration) | Lý do & Lợi ích kiến trúc |
| :--- | :--- | :--- | :--- |
| **Frontend Framework** | React 18, Vite, TypeScript | **HTML5, CSS3, JavaScript thuần (Modular ES6+)** | Loại bỏ hoàn toàn bundle nặng, mã nguồn rõ ràng, tương thích tối đa với Servlet/JSP container. |
| **Frontend Styling** | Tailwind CSS | **Bootstrap 5.3.3 + SASS/SCSS (Cấu trúc 7-1)** | Quản lý biến và theme tập trung (`_variables.scss`), responsive linh hoạt, không phụ thuộc bộ build phức tạp. |
| **Icons** | Lucide React | **Bootstrap Icons (v1.11.3)** | Tương thích hoàn hảo với hệ thống CSS Bootstrap, tải trực tiếp hoặc CDN. |
| **Backend Runtime** | Node.js (v18+) | **Java (JDK 8 / 11 / 17 / 21)** | Nền tảng doanh nghiệp bền vững, type-safety, đa luồng hiệu năng cao. |
| **Backend Architecture**| Express, TypeScript | **Java Servlet (3.1 / 4.0), JSP & JSTL 1.2** | Tuân thủ tuyệt đối mô hình MVC chuẩn Java EE, phân tách Controller - Service - DAO - Model. |
| **Database Connector** | node-postgres (`pg`) / Raw SQL | **JDBC thuần kết hợp HikariCP Connection Pool** | Hiệu năng tối đa, kiểm soát chặt chẽ từng câu lệnh SQL, chống nghẽn kết nối. |
| **Database Engine** | PostgreSQL 15 | **MySQL 8.0+ (InnoDB Engine)** | Định dạng chuẩn phổ biến, hỗ trợ đầy đủ Foreign Key, Unique, ACID Transactions, Charset `utf8mb4`. |
| **Cơ chế Xác thực** | JWT (JSON Web Token) trong Cookie | **Java HttpSession (Cookie `JSESSIONID` HttpOnly)** | Quản lý phiên tập trung phía server, an toàn tuyệt đối trước tấn công XSS đánh cắp token. |
| **Mã hóa Mật khẩu** | `bcryptjs` (Node.js) | **`jBCrypt` (0.4 - Java)** | Thuật toán băm BCrypt chuẩn công nghiệp với Salt 10 vòng lặp an toàn. |
| **Quản lý Thư viện** | npm / package.json | **Apache Maven (`pom.xml`)** | Chuẩn đóng gói `.war` deploy trực tiếp lên Apache Tomcat Server. |

---

## 2. NHỮNG TẬP TIN ĐÃ THAY ĐỔI, TẠO MỚI VÀ LOẠI BỎ

### 2.1. Tập tin đã xóa bỏ hoàn toàn (100% Old Code Removal)
- Đã xóa sạch toàn bộ mã nguồn TypeScript/React ở Frontend:
  - `frontend/src/**/*.tsx`
  - `frontend/src/**/*.ts`
  - `frontend/src/**/*.css`
  - `frontend/vite.config.ts`, `frontend/tsconfig.json`, `frontend/tsconfig.node.json`
- Đã xóa sạch toàn bộ mã nguồn Node.js/TypeScript ở Backend:
  - `backend/src/**/*.ts`
  - `backend/tsconfig.json`
  - `backend/vitest.config.ts`
  - Toàn bộ các file kiểm thử `.test.ts`, `.spec.ts` chạy trên môi trường Node.js.

### 2.2. Tập tin Database đã tạo mới (`database/`)
- [database/schema.sql](file:///e:/ttcs_n1/database/schema.sql): Định nghĩa 13 bảng MySQL với engine InnoDB, charset `utf8mb4_unicode_ci`, thiết lập ràng buộc khóa chính, khóa ngoại, unique key và đánh chỉ mục tối ưu.
- [database/seed.sql](file:///e:/ttcs_n1/database/seed.sql): Khởi tạo 30 quyền hạn chuẩn thuộc 10 phân hệ, 7 vai trò hệ thống, ma trận quyền ban đầu, 7 tài khoản người dùng mẫu với mật khẩu băm BCrypt (`Admin@123456`), cơ cấu phòng ban, hồ sơ ứng viên và dải lương mẫu.
- [database/README.md](file:///e:/ttcs_n1/database/README.md): Hướng dẫn cài đặt CSDL và tài liệu sơ đồ quan hệ thực thể (ERD).

### 2.3. Tập tin Backend Java Servlet đã tạo mới (`backend/src/main/` & `src/main/`)
1. **Cấu hình & Deployment:**
   - [pom.xml](file:///e:/ttcs_n1/backend/pom.xml): Cấu hình Maven WAR, nạp thư viện `javax.servlet-api:4.0.1`, `jsp-api:2.3.3`, `jstl:1.2`, `mysql-connector-java:8.0.33`, `HikariCP:3.4.5`, `jbcrypt:0.4`, `gson:2.10.1`, và plugin `tomcat7-maven-plugin`.
   - [web.xml](file:///e:/ttcs_n1/backend/src/main/webapp/WEB-INF/web.xml): Cấu hình Filter chuỗi, timeout phiên 60 phút, trang báo lỗi tùy biến (403, 404, 500).
   - [application.properties](file:///e:/ttcs_n1/backend/src/main/resources/application.properties): Thông số kết nối MySQL, kích thước Hikari Pool và cấu hình bảo mật.
2. **Lớp Kết nối & Cấu hình:**
   - [DBConnection.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/config/DBConnection.java): Khởi tạo Singleton HikariDataSource kết nối MySQL.
   - [AppConfig.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/config/AppConfig.java): Nạp và truy xuất biến cấu hình tập trung.
3. **Lớp Mô hình Thực thể (Models):**
   - [User.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/model/User.java): Chứa đầy đủ thông tin tài khoản, danh sách vai trò, danh sách quyền và các phương thức nghiệp vụ `hasRole()`, `hasPermission()`.
   - [Role.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/model/Role.java), [Permission.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/model/Permission.java), [Department.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/model/Department.java), [Candidate.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/model/Candidate.java), [SalaryRange.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/model/SalaryRange.java), [AuditLog.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/model/AuditLog.java).
4. **Lớp Truy xuất Dữ liệu (DAOs):**
   - [BaseDAO.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/dao/BaseDAO.java): Cung cấp tiện ích lấy kết nối và giải phóng tài nguyên JDBC an toàn.
   - [UserDAO.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/dao/UserDAO.java): Xử lý CRUD tài khoản, tìm kiếm, lọc theo phòng ban, khóa/mở khóa, tăng session version, cập nhật số lần thử sai.
   - [RoleDAO.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/dao/RoleDAO.java), [PermissionDAO.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/dao/PermissionDAO.java), [DepartmentDAO.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/dao/DepartmentDAO.java), [CandidateDAO.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/dao/CandidateDAO.java), [SalaryRangeDAO.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/dao/SalaryRangeDAO.java), [AuditDAO.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/dao/AuditDAO.java).
5. **Lớp Nghiệp vụ (Services):**
   - [AuthService.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/service/AuthService.java): Xác thực BCrypt, cơ chế chống Brute-force (khóa 15 phút sau 5 lần sai), xử lý đổi mật khẩu và vô hiệu hóa phiên.
   - [UserService.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/service/UserService.java), [RolePermissionService.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/service/RolePermissionService.java), [DepartmentService.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/service/DepartmentService.java), [CandidateService.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/service/CandidateService.java), [SalaryRangeService.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/service/SalaryRangeService.java), [AuditService.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/service/AuditService.java).
6. **Lớp Điều phối & Bộ lọc (Filters & Controllers):**
   - [EncodingFilter.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/filter/EncodingFilter.java): Ép buộc UTF-8 cho mọi luồng request/response.
   - [AuthFilter.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/filter/AuthFilter.java): Bảo vệ các đường dẫn nhạy cảm, chặn truy cập khi chưa đổi mật khẩu khởi tạo, kiểm tra RBAC và ngăn vai trò `INTERVIEWER` xem thông tin lương.
   - [DashboardServlet.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/controller/DashboardServlet.java), [AuthServlet.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/controller/AuthServlet.java), [UserServlet.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/controller/UserServlet.java), [RoleServlet.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/controller/RoleServlet.java), [PermissionMatrixServlet.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/controller/PermissionMatrixServlet.java), [DepartmentServlet.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/controller/DepartmentServlet.java), [CandidateServlet.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/controller/CandidateServlet.java), [SalaryRangeServlet.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/controller/SalaryRangeServlet.java), [AuditLogServlet.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/controller/AuditLogServlet.java), [PlaceholderServlet.java](file:///e:/ttcs_n1/backend/src/main/java/com/irms/controller/PlaceholderServlet.java).
7. **Lớp Giao diện Server-Side (JSP Views):**
   - Layouts: `header.jsp`, `sidebar.jsp`, `navbar.jsp`, `alerts.jsp`, `footer.jsp`.
   - Chức năng: `auth/login.jsp`, `auth/forgot-password.jsp`, `auth/change-password.jsp`, `dashboard/index.jsp`, `users/list.jsp`, `roles/list.jsp`, `permissions/matrix.jsp`, `departments/list.jsp`, `candidates/list.jsp`, `salary/list.jsp`, `audit/list.jsp`, `placeholder/index.jsp`.
   - Báo lỗi: `errors/403.jsp`, `errors/404.jsp`, `errors/500.jsp`.

### 2.4. Tập tin Frontend đã tạo mới (`frontend/` & `webapp/assets/`)
1. **Kiến trúc SASS 7-1 Pattern (`assets/scss/`):**
   - `abstracts/`: `_variables.scss`, `_mixins.scss`, `_functions.scss` (Định nghĩa bảng màu Indigo/Slate hiện đại, kích thước, đổ bóng, breakpoint).
   - `base/`: `_reset.scss`, `_typography.scss`, `_global.scss`.
   - `components/`: `_buttons.scss`, `_forms.scss`, `_cards.scss`, `_tables.scss`, `_navbar.scss`, `_badges.scss`.
   - `layouts/`: `_header.scss`, `_sidebar.scss`, `_footer.scss`, `_taskbar.scss`.
   - `pages/`: `_login.scss`, `_dashboard.scss`, `_matrix.scss`.
   - `main.scss`: Điểm tích hợp biên dịch SASS sang `assets/css/main.css`.
2. **Mã nguồn JavaScript Mô-đun (`assets/js/`):**
   - `main.js`: Khởi tạo chung, điều khiển đóng mở Sidebar Responsive trên Mobile.
   - `validation.js`: Xác thực dữ liệu biểu mẫu Client-side.
   - `auth.js`: Xử lý đăng nhập, đếm ngược Brute-force lockout.
   - `modal.js`: Tiện ích đóng/mở Dynamic Modal của Bootstrap.
   - `search.js` & `pagination.js`: Tìm kiếm và phân trang bảng dữ liệu động.
   - `candidates.js`: Xử lý chuyển giai đoạn hồ sơ ứng viên (Pipeline Kanban/Table).
   - `matrix.js`: Xử lý cập nhật ma trận phân quyền tức thời qua AJAX.
   - `users.js`: Điều khiển các thao tác khóa tài khoản, mở khóa, cấp lại mật khẩu.
3. **Màn hình HTML Độc lập (Standalone HTML Prototypes):**
   - `index.html` (Màn hình đăng nhập)
   - `dashboard.html` (Bảng điều khiển KPI)
   - `users.html` (Quản lý tài khoản)
   - `roles.html` (Danh mục vai trò)
   - `permissions.html` (Ma trận phân quyền)
   - `departments.html` (Cơ cấu phòng ban)
   - `candidates.html` (Pipeline ứng viên)
   - `salary.html` (Dải lương ngân sách)
   - `audit.html` (Nhật ký kiểm toán)
   - `change-password.html` & `forgot-password.html`

---

## 3. BẢO TOÀN TOÀN BỘ CHỨC NĂNG NGHIỆP VỤ (100% PRESERVED)

Không có bất kỳ tính năng nào bị xóa bỏ hay đơn giản hóa. Mọi quy tắc nghiệp vụ đều được ánh xạ chính xác:

### 3.1. Phân hệ Xác thực & Quản lý phiên (Authentication & Session)
- **Đăng nhập an toàn:** Xác thực mật khẩu với BCrypt.
- **Phòng chống dò mật khẩu (Brute-Force Protection):** Khi nhập sai mật khẩu, hệ thống tăng `failed_login_attempts` và cảnh báo số lần thử còn lại. Nếu sai liên tiếp **5 lần**, tài khoản bị khóa tạm trong **15 phút** (`locked_until = NOW() + INTERVAL 15 MINUTE`).
- **Đổi mật khẩu bắt buộc:** Tài khoản mới tạo hoặc được cấp lại mật khẩu tạm thời có cờ `must_change_password = true`. `AuthFilter` sẽ bắt buộc người dùng đổi mật khẩu trước khi được truy cập các phân hệ khác.
- **Vô hiệu hóa phiên tức thời (`session_version`):** Khi tài khoản bị khóa, đổi mật khẩu hoặc cấp lại mật khẩu, cột `session_version` tự động tăng 1, ngay lập tức vô hiệu hóa phiên làm việc của người dùng trên toàn hệ thống.

### 3.2. Quản trị Người dùng (User Management)
- Xem danh sách người dùng có tìm kiếm theo họ tên, email, mã nhân viên, lọc theo phòng ban.
- Thêm mới tài khoản với mã nhân viên tự động hoặc tùy chỉnh, mật khẩu khởi tạo mã hóa BCrypt.
- Cập nhật thông tin: Họ tên, số điện thoại, chức danh, phòng ban.
- Khóa tài khoản có ghi nhận lý do và mở khóa tài khoản.
- Cấp lại mật khẩu tạm thời cho người dùng.

### 3.3. Vai trò & Ma trận phân quyền (RBAC Matrix)
- 7 vai trò hệ thống: `ADMIN`, `HR_MANAGER`, `RECRUITER`, `HIRING_MANAGER`, `INTERVIEWER`, `APPROVER`, `CANDIDATE`.
- 30 quyền hạn được gom nhóm theo 10 phân hệ nghiệp vụ.
- Ma trận phân quyền tương tác trực quan: Cho phép bật/tắt quyền hạn của từng vai trò và lưu trực tiếp qua AJAX.

### 3.4. Cơ cấu Phòng ban (Department Structure)
- Danh mục phòng ban kèm mã code duy nhất (BGD, HR, TECH, SALES...).
- Thống kê số lượng nhân sự trực thuộc từng phòng ban.

### 3.5. Quy trình Hồ sơ Ứng viên & Pipeline Tuyển dụng
- Quản lý hồ sơ ứng viên gắn liền với yêu cầu tuyển dụng.
- Chuyển tiếp trạng thái ứng viên qua các vòng tuyển dụng: `APPLIED` (Ứng tuyển) -> `SCREENING` (Lọc hồ sơ) -> `INTERVIEWING` (Phỏng vấn) -> `OFFER` (Thư mời) -> `HIRED` (Đã tuyển dụng) hoặc `REJECTED` (Từ chối).

### 3.6. Dải lương ngân sách (Salary Ranges)
- Quản lý hạn mức lương tối thiểu - tối đa theo từng vị trí tuyển dụng.
- **Ràng buộc nghiệp vụ bảo mật:** Phân quyền chặn hoàn toàn người dùng có vai trò `INTERVIEWER` (Người phỏng vấn) không được phép truy cập hay xem dữ liệu dải lương ngân sách.

### 3.7. Nhật ký kiểm toán an ninh (Audit Logs)
- Ghi nhận tự động các sự kiện: Đăng nhập thành công, đăng nhập thất bại, khóa tài khoản, mở khóa, đổi mật khẩu, phân quyền.
- Lưu trữ đầy đủ: Mã sự kiện, người thực hiện, thời điểm, địa chỉ IP và trình duyệt thực hiện.

### 3.8. Phân hệ mở rộng (Phase 2 Placeholders)
- [PlaceholderServlet](file:///e:/ttcs_n1/backend/src/main/java/com/irms/controller/PlaceholderServlet.java) xử lý điều hướng thông minh cho các chức năng mở rộng tiếp theo:
  - `/recruitment-requests/*` (Yêu cầu tuyển dụng)
  - `/job-postings/*` (Tin tuyển dụng)
  - `/interviews/*` (Lịch phỏng vấn & Phiếu đánh giá)
  - `/offers/*` (Thư mời nhận việc)
  - `/onboarding/*` (Quy trình tiếp nhận nhân sự)
  - `/notifications/*` (Hộp thư thông báo)
  - `/reports/*` (Báo cáo thống kê)

---

## 4. CHI TIẾT CHUYỂN ĐỔI CƠ SỞ DỮ LIỆU (DATABASE MIGRATION)

### 4.1. Bảng ánh xạ kiểu dữ liệu (PostgreSQL -> MySQL 8.0)
| Kiểu dữ liệu PostgreSQL | Kiểu dữ liệu MySQL 8.0 | Ghi chú xử lý |
| :--- | :--- | :--- |
| `UUID PRIMARY KEY` | `VARCHAR(36) PRIMARY KEY` | Lưu trữ chuỗi UUID chuẩn 36 ký tự |
| `VARCHAR(n)` | `VARCHAR(n)` | Giữ nguyên độ dài |
| `TEXT` | `TEXT` | Giữ nguyên |
| `BOOLEAN` | `BOOLEAN` (hoặc `TINYINT(1)`) | Tương thích chuẩn SQL |
| `TIMESTAMP WITH TIME ZONE` | `TIMESTAMP` | Thiết lập default `CURRENT_TIMESTAMP` |
| `NOW()` | `NOW()` hoặc `CURRENT_TIMESTAMP` | Giữ nguyên hàm thời gian thực |
| `SERIAL` / `BIGSERIAL` | `INT AUTO_INCREMENT` | Cho các bảng phụ có khóa số |

### 4.2. Khóa ngoại và Toàn vẹn tham chiếu
- Toàn bộ 13 bảng đều sử dụng động cơ lưu trữ `ENGINE=InnoDB` để hỗ trợ khóa ngoại (Foreign Key Constraints) và giao dịch ACID.
- Bảng trung gian `user_roles` và `role_permissions` áp dụng `ON DELETE CASCADE` để tự động dọn dẹp khi xóa người dùng hoặc quyền.
- Bảng `users` liên kết `department_id` áp dụng `ON DELETE SET NULL` để không làm mất tài khoản khi phòng ban bị giải thể.

---

## 5. HƯỚNG DẪN KHỞI CHẠY VÀ VẬN HÀNH DỰ ÁN

### 5.1. Yêu cầu môi trường
- **Java:** JDK 8, 11, 17 hoặc 21 (Hệ thống hiện tại đã cài đặt Java 8).
- **Maven:** Phiên bản 3.6 trở lên.
- **MySQL:** Phiên bản 8.0 trở lên.
- **Tomcat:** Phiên bản 8.5, 9.0 hoặc 10.1 (chạy qua plugin maven hoặc standalone server).

### 5.2. Khởi tạo Cơ sở dữ liệu MySQL
1. Mở MySQL Workbench hoặc terminal MySQL:
   ```bash
   mysql -u root -p
   ```
2. Thực thi tuần tự hai tập tin cấu trúc và dữ liệu mẫu:
   ```sql
   SOURCE e:/ttcs_n1/database/schema.sql;
   SOURCE e:/ttcs_n1/database/seed.sql;
   ```
   *(Hoặc sử dụng Docker Compose có sẵn trong dự án: `docker compose up -d` để tự động nạp database `ttcs_db` trên port 3306)*.

### 5.3. Cấu hình kết nối Backend
Kiểm tra tập tin `backend/src/main/resources/application.properties` (hoặc biến môi trường `.env`):
```properties
db.url=jdbc:mysql://localhost:3306/ttcs_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
db.user=root
db.password=rootpassword
db.pool.size=10
```

### 5.4. Biên dịch và Chạy Backend
Tại thư mục `backend/` (hoặc thư mục gốc dự án):
```bash
mvn clean package
```
Chạy thử nghiệm nhanh với Tomcat Plugin tích hợp sẵn trong `pom.xml`:
```bash
mvn tomcat7:run
```
Truy cập hệ thống tại:
```
http://localhost:8080/
```

### 5.5. Xem giao diện Frontend trực tiếp
Có thể mở trực tiếp tập tin [frontend/index.html](file:///e:/ttcs_n1/frontend/index.html) hoặc [frontend/dashboard.html](file:///e:/ttcs_n1/frontend/dashboard.html) trên bất kỳ trình duyệt web hiện đại nào (Chrome, Firefox, Edge, Safari).

### 5.6. Tài khoản kiểm thử được cấp sẵn
| Email | Mật khẩu | Vai trò | Phân quyền đặc thù |
| :--- | :--- | :--- | :--- |
| `admin@company.local` | `Admin@123456` | `ADMIN` | Toàn quyền quản trị hệ thống, người dùng, ma trận vai trò |
| `hr.manager@company.local` | `Admin@123456` | `HR_MANAGER` | Quản lý tuyển dụng, phòng ban, ứng viên, xem dải lương |
| `recruiter@company.local` | `Admin@123456` | `RECRUITER` | Lọc hồ sơ ứng viên, điều phối phỏng vấn (không thấy dải lương) |
| `hiring.mgr@company.local` | `Admin@123456` | `HIRING_MANAGER` | Đề xuất tuyển dụng, phỏng vấn chuyên môn, phê duyệt offer |
| `interviewer@company.local` | `Admin@123456` | `INTERVIEWER` | Đánh giá phỏng vấn (**Bị chặn hoàn toàn xem dải lương**) |
| `approver@company.local` | `Admin@123456` | `APPROVER` | Phê duyệt ngân sách và thư mời nhận việc |
| `locked.user@company.local` | `Admin@123456` | `LOCKED` | Tài khoản mô phỏng trạng thái bị khóa do vi phạm |

---

## 6. MIGRATION CHECKLIST (BẢNG KIỂM TRA HOÀN THÀNH)

- [x] **Frontend Migrated:** Hoàn thành 100% bằng HTML5, CSS3, JavaScript mô-đun, Bootstrap 5.3 và SASS 7-1 pattern.
- [x] **Old Code Removal:** Xóa sạch 100% mã nguồn React, TypeScript, Tailwind, Node.js (0 file .ts / .tsx tồn đọng).
- [x] **Backend Migrated:** Hoàn thành kiến trúc MVC mở rộng trên Java Servlet + JSP + JSTL + JDBC thuần (HikariCP).
- [x] **Database Migrated:** Chuyển đổi toàn bộ sang MySQL 8.0 InnoDB (`schema.sql`, `seed.sql`), đầy đủ PK, FK, Unique, Index.
- [x] **Authentication Migrated:** Đăng nhập BCrypt, chống Brute-Force (khóa 15 phút sau 5 lần), đăng xuất, đổi mật khẩu bắt buộc.
- [x] **Session Version Invalidation:** Tăng `session_version` để thu hồi phiên ngay lập tức khi đổi mật khẩu hoặc khóa tài khoản.
- [x] **Authorization / RBAC Migrated:** 7 vai trò, 30 quyền hạn chi tiết, kiểm soát cả Frontend và Filter Backend (`AuthFilter`).
- [x] **CRUD Operations Migrated:** Người dùng, Phòng ban, Ứng viên, Dải lương, Phân quyền.
- [x] **Search, Filter, Sort, Pagination:** Triển khai cả Client-side JavaScript và Database Query qua DAO.
- [x] **Interviewer Restriction:** Chặn vai trò `INTERVIEWER` xem thông tin dải lương ngân sách theo đúng quy tắc an ninh.
- [x] **Audit Logging:** Tự động ghi vết toàn bộ hoạt động nhạy cảm vào CSDL.
- [x] **Phase 2 Expansion:** Triển khai `PlaceholderServlet` cho 7 phân hệ mở rộng theo lộ trình tiếp theo.
- [x] **Documentation Completed:** Cập nhật đầy đủ `README.md`, `docs/architecture.md`, `docs/database.md`, `docs/api.md`, `docs/migration.md`.

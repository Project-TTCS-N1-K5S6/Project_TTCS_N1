# HỆ THỐNG QUẢN TRỊ TUYỂN DỤNG NỘI BỘ (IRMS)
### Dự án Thực tập Cơ sở (TTCS) & Tuyển dụng Doanh nghiệp
**Công nghệ:** Backend Java Servlet &amp; JSP &bull; Cơ sở dữ liệu MySQL 8.0 &bull; Frontend Bootstrap 5, HTML5, CSS3, JS &amp; SASS

---

## 📌 1. Giới thiệu Dự án
**IRMS (Internal Recruitment Management System)** là giải pháp phần mềm quản trị quy trình tuyển dụng nội bộ toàn diện cho doanh nghiệp:
- **Kiến trúc:** Mô hình MVC (Model - View - Controller) kinh điển trong Java Enterprise Web.
- **Bảo mật:** Phân quyền theo vai trò (RBAC) trên 10 phân hệ nghiệp vụ, mã hóa mật khẩu một chiều bằng thuật toán BCrypt, tự động khóa tài khoản sau 5 lần đăng nhập thất bại trong 15 phút, chống lộ diện người dùng (User Enumeration Protection), và kiểm toán toàn diện (Audit Logging).
- **Giao diện:** Thiết kế hiện đại, chuyên nghiệp với **Bootstrap 5.3**, phối hợp hệ thống biến **SASS (SCSS)**, hỗ trợ linh hoạt từ màn hình máy tính tới thiết bị di động (Responsive Layout & Mobile Drawer).

---

## 🛠️ 2. Công nghệ sử dụng trong dự án

| Thành phần | Công nghệ / Thư viện | Vai trò |
| :--- | :--- | :--- |
| **Backend** | **Java 8+ / Java Servlet 4.0 (hoặc 3.1)** | Tầng xử lý logic Controller điều phối HTTP Request/Response |
| **View Engine** | **JSP (Java Server Pages) &amp; JSTL 1.2** | Render giao diện web động từ phía máy chủ (Server-Side Rendering) |
| **Database** | **MySQL 8.0+** | Hệ quản trị cơ sở dữ liệu quan hệ lưu trữ dữ liệu ACID an toàn |
| **Connection Pool** | **HikariCP 3.4.5** | Quản lý và tái sử dụng kết nối JDBC với hiệu năng tối ưu |
| **Bảo mật mật khẩu** | **jBCrypt 0.4** | Băm và đối chiếu mật khẩu an toàn theo tiêu chuẩn BCrypt |
| **Xử lý JSON / AJAX**| **Google Gson 2.10.1** | Chuyển đổi dữ liệu đối tượng Java sang JSON cho các API/AJAX |
| **Frontend Styling** | **Bootstrap 5.3.3 &amp; SASS (SCSS)** | Khung giao diện UI hiện đại, phối màu doanh nghiệp |
| **Frontend Logic** | **Vanilla JavaScript (ES6+)** | Điều khiển tương tác modal, lọc tìm kiếm, validation và gọi AJAX |
| **Build &amp; Dependency**| **Apache Maven** | Quản lý thư viện và đóng gói thành file `war` triển khai lên Tomcat |

---

## 📁 3. Cấu trúc thư mục Dự án

```
ttcs_n1/
├── pom.xml                                   <- File cấu hình Maven (Dependencies & Plugins)
├── package.json                              <- Quản lý script biên dịch SASS & Docker MySQL
├── docker-compose.yml                        <- Khởi chạy MySQL 8.0 & MailHog bằng Docker
├── .env / .env.example                       <- Biến môi trường hệ thống
│
├── database/                                 <- Kịch bản Cơ sở dữ liệu MySQL
│   ├── schema.sql                            <- DDL tạo bảng, chỉ mục và khóa ngoại MySQL
│   └── seed.sql                              <- DML nạp dữ liệu mẫu ban đầu (Users, Roles, Perms)
│
├── src/main/
│   ├── java/com/irms/                        <- Mã nguồn Java Backend
│   │   ├── config/                           <- Cấu hình hệ thống & kết nối DB
│   │   │   ├── AppConfig.java                <- Đọc cấu hình từ application.properties
│   │   │   └── DBConnection.java             <- HikariCP Connection Pool kết nối MySQL
│   │   ├── model/                            <- JavaBeans / Thực thể dữ liệu (Models)
│   │   │   ├── User.java, Role.java, Permission.java, Department.java
│   │   │   ├── Candidate.java, SalaryRange.java, AuditLog.java
│   │   ├── dao/                              <- Tầng truy vấn CSDL JDBC (Data Access Objects)
│   │   │   ├── BaseDAO.java, UserDAO.java, RoleDAO.java, PermissionDAO.java
│   │   │   ├── DepartmentDAO.java, CandidateDAO.java, SalaryRangeDAO.java, AuditDAO.java
│   │   ├── service/                          <- Tầng nghiệp vụ (Business Logic Layer)
│   │   │   ├── AuthService.java, UserService.java, RolePermissionService.java
│   │   │   ├── DepartmentService.java, CandidateService.java, SalaryRangeService.java, AuditService.java
│   │   ├── filter/                           <- Bộ lọc kiểm soát bảo mật (Filters)
│   │   │   ├── EncodingFilter.java           <- Ép buộc UTF-8 cho toàn hệ thống
│   │   │   └── AuthFilter.java               <- Kiểm tra đăng nhập, đổi mật khẩu và phân quyền RBAC
│   │   ├── controller/                       <- Tầng điều khiển Servlet (HTTP Handlers)
│   │   │   ├── DashboardServlet.java         <- Bảng điều khiển tổng quan (/dashboard)
│   │   │   ├── AuthServlet.java              <- Đăng nhập, đăng xuất, đổi mật khẩu (/auth/*)
│   │   │   ├── UserServlet.java              <- Quản trị người dùng (/admin/users)
│   │   │   ├── RoleServlet.java              <- Danh mục vai trò (/admin/roles)
│   │   │   ├── PermissionMatrixServlet.java  <- Ma trận phân quyền 10 phân hệ (/admin/permissions)
│   │   │   ├── DepartmentServlet.java        <- Quản lý phòng ban (/admin/departments)
│   │   │   ├── CandidateServlet.java         <- Hồ sơ & pipeline ứng viên (/candidates)
│   │   │   ├── SalaryRangeServlet.java       <- Dải lương vị trí (/salary-ranges)
│   │   │   └── AuditLogServlet.java          <- Tra cứu nhật ký kiểm toán (/admin/audit-logs)
│   │   └── util/                             <- Tiện ích bảo mật và xử lý dữ liệu
│   │       ├── PasswordUtil.java             <- Mã hóa BCrypt
│   │       ├── JsonUtil.java                 <- Google Gson cho AJAX
│   │       └── SecurityUtil.java             <- Lấy IP Client, chống XSS, sinh UUID
│   │
│   ├── resources/
│   │   └── application.properties            <- Cấu hình tham số MySQL, Pool & Security
│   │
│   └── webapp/                               <- Tầng giao diện người dùng (Web Application)
│       ├── WEB-INF/
│       │   ├── web.xml                       <- Bộ định tuyến và mô tả triển khai Web
│       │   └── views/                        <- Giao diện JSP (Server-Side Rendering)
│       │       ├── common/                   <- Layout dùng chung: header, footer, sidebar, navbar, alerts
│       │       ├── auth/                     <- Đăng nhập, quên mật khẩu, đổi mật khẩu
│       │       ├── dashboard/                <- Giao diện thống kê tổng quan
│       │       ├── users/                    <- Quản lý người dùng, modal thêm/sửa/khóa
│       │       ├── roles/                    <- Danh mục vai trò hệ thống
│       │       ├── permissions/              <- Giao diện Ma trận phân quyền 10 phân hệ
│       │       ├── departments/              <- Quản lý cơ cấu phòng ban
│       │       ├── candidates/               <- Quản lý hồ sơ & pipeline ứng viên
│       │       ├── salary/                   <- Quản lý dải lương ngân sách
│       │       ├── audit/                    <- Tra cứu nhật ký kiểm toán
│       │       └── errors/                   <- Trang thông báo lỗi 403, 404, 500
│       └── assets/                           <- Tài nguyên tĩnh (Static Assets)
│           ├── scss/                         <- Các tệp nguồn SASS (_variables, _layout, _components, _tables, _auth, main.scss)
│           ├── css/                          <- Tệp CSS biên dịch hoàn chỉnh (main.css)
│           └── js/                           <- Mã JavaScript giao diện (main.js, users.js, matrix.js)
│
├── frontend/                                 <- Bản xem trước giao diện HTML + Bootstrap + SASS độc lập
│   ├── index.html, dashboard.html, users.html, candidates.html, permissions.html
│
└── docs/                                     <- Tài liệu kiến trúc và cơ sở dữ liệu
    ├── architecture.md                       <- Tài liệu kiến trúc MVC & Bảo mật
    └── database.md                           <- Lược đồ CSDL MySQL chi tiết
```

---

## 🚀 4. Hướng dẫn Cài đặt & Vận hành

### Bước 1: Khởi động Cơ sở dữ liệu MySQL
Có thể sử dụng Docker Compose hoặc cài đặt MySQL Server trực tiếp trên máy:

**Cách 1: Khởi chạy bằng Docker Compose (Khuyến nghị, nhanh nhất):**
```bash
# Khởi chạy MySQL 8.0 (Tự động nạp schema.sql và seed.sql)
docker compose up -d mysql
```

**Cách 2: Sử dụng MySQL Server cài sẵn trên máy:**
1. Mở MySQL Workbench hoặc dòng lệnh `mysql -u root -p`.
2. Chạy lần lượt các tập lệnh:
   ```sql
   source database/schema.sql;
   source database/seed.sql;
   ```
3. Mở file `src/main/resources/application.properties`, kiểm tra thông số kết nối:
   ```properties
   db.url=jdbc:mysql://localhost:3306/ttcs_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8
   db.user=root
   db.password=root
   ```

### Bước 2: Chạy ứng dụng Java Servlet

**Cách 1: Chạy trong IntelliJ IDEA / Eclipse:**
1. Mở thư mục dự án `ttcs_n1` trong IntelliJ IDEA hoặc Eclipse.
2. Chọn **Import as Maven Project**.
3. Cấu hình máy chủ **Apache Tomcat 8.5 hoặc 9.0**:
   - Application context: `/` (hoặc `/irms`).
   - Chọn Deployment: `irms:war exploded`.
4. Nhấn **Run** hoặc **Debug**.

**Cách 2: Chạy trực tiếp bằng Maven Tomcat Plugin (Dòng lệnh):**
```bash
mvn tomcat7:run
```
Ứng dụng sẽ khởi chạy tại: **http://localhost:8080**

**Chạy database IRMS cục bộ trên Windows (nếu đã được chuẩn bị):**
Chạy `run-local.bat` để dùng database riêng `irms_local_dev_20261008` trên MySQL tại máy hiện tại. Database này được nạp từ `database/schema.sql` và `database/seed.sql`, không ghi vào database `irms_db` cũ hoặc DB chung. Đăng nhập mẫu: `admin@company.local` / `Admin@123456`. Chạy `run.bat` không có tham số để dùng cấu hình DB trong `.env`.

### Bước 3: Xem giao diện Frontend độc lập (Tùy chọn)
Nếu muốn xem trước các màn hình HTML/CSS/JS/Bootstrap tĩnh mà không cần bật máy chủ Java:
- Mở thư mục `frontend/` và nhấp đúp vào `index.html` hoặc chạy với Visual Studio Code **Live Server**.

---

## 🔑 5. Danh sách Tài khoản Kiểm thử Mặc định
Mật khẩu chung cho tất cả tài khoản mẫu: **`Admin@123456`**

| Vai trò | Email đăng nhập | Quyền hạn nổi bật |
| :--- | :--- | :--- |
| **Quản trị hệ thống (Admin)** | `admin@company.local` | Toàn quyền quản trị tài khoản, vai trò, ma trận phân quyền và kiểm toán |
| **Trưởng phòng Nhân sự (HR Manager)** | `hr.manager@company.local` | Quản lý danh mục phòng ban, ứng viên, xem người dùng và dải lương |
| **Chuyên viên Tuyển dụng (Recruiter)** | `recruiter@company.local` | Tiếp nhận ứng viên, điều phối pipeline (Bị giới hạn không xem dải lương) |
| **Trưởng bộ phận (Hiring Manager)** | `hiring.mgr@company.local` | Theo dõi ứng viên phỏng vấn chuyên môn |
| **Người phỏng vấn (Interviewer)** | `interviewer@company.local` | Chấm điểm phỏng vấn (Cấm tuyệt đối truy cập dải lương) |
| **Tài khoản bị khóa (Locked)** | `locked.user@company.local` | Mô phỏng trường hợp tài khoản bị khóa do vi phạm |

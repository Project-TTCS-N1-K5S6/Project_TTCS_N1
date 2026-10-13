# Tái sử dụng mô tả và yêu cầu tuyển dụng

Ứng dụng web cho phép chọn một tin tuyển dụng có sẵn, chỉnh sửa nội dung và tạo tin mới ở trạng thái **Nháp**. Giao diện HTML/CSS/JavaScript gọi API Java Servlet; dữ liệu tin gốc và bản sao được lưu trong MySQL, không lưu bằng `localStorage`.

## Công nghệ

- Java 8+, Java Servlet 4.0, Apache Tomcat 9
- MySQL 8+
- JDBC với HikariCP
- Maven, Gson
- HTML, CSS và JavaScript thuần

## Chức năng backend

| Phương thức | Endpoint | Mô tả |
| --- | --- | --- |
| `GET` | `/api/jobs` | Lấy danh sách tin tuyển dụng gốc |
| `GET` | `/api/job-copies` | Lấy các bản sao đã tạo, mới nhất trước |
| `POST` | `/api/job-copies` | Tạo bản sao mới ở trạng thái `Nháp` |

Ví dụ dữ liệu gửi tới `POST /api/job-copies`:

```json
{
  "sourceJobId": "job-sales",
  "title": "Bản sao - Chuyên viên Kinh doanh",
  "description": "Mô tả công việc",
  "requirements": "Yêu cầu ứng viên"
}
```

## Chạy ứng dụng trên Windows

Yêu cầu đã cài JDK 8 trở lên, Maven, MySQL 8 và Apache Tomcat 9.

1. Tạo cơ sở dữ liệu và bảng:

   ```powershell
   Get-Content -Raw database\schema.sql | mysql -u root -p
   ```

2. Nạp dữ liệu tin tuyển dụng mẫu:

   ```powershell
   Get-Content -Raw database\seed.sql | mysql -u root -p ttcs_db
   ```

3. Cấu hình kết nối MySQL bằng biến môi trường. Thay thông tin bên dưới bằng thông tin MySQL trên máy:

   ```powershell
   $env:DB_URL = "jdbc:mysql://localhost:3306/ttcs_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8"
   $env:DB_USER = "root"
   $env:DB_PASSWORD = "mat-khau-mysql"
   ```

4. Đóng gói ứng dụng:

   ```powershell
   mvn clean package
   ```

5. Chép `target\ROOT.war` vào thư mục `webapps` của Tomcat 9, sau đó khởi động Tomcat. Mở [http://localhost:8080](http://localhost:8080).

Tomcat phải được khởi động từ cùng cửa sổ PowerShell đã cấu hình các biến môi trường ở bước 3. Ứng dụng và API được phục vụ cùng origin; nếu MySQL chưa chạy hoặc cấu hình sai, giao diện sẽ hiển thị lỗi thay vì âm thầm dùng dữ liệu mẫu.

## Cấu trúc chính

```text
database\
  schema.sql
  seed.sql
src\main\java\com\irms\
  config\Database.java
  config\DatabaseLifecycle.java
  controller\RecruitmentServlet.java
  dao\RecruitmentRepository.java
pom.xml
index.html
script.js
styles.css
```

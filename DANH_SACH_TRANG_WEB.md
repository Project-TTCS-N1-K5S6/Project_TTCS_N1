# 🌐 TỔNG HỢP DANH SÁCH CÁC TRANG & ĐƯỜNG DẪN TRUY CẬP (IRMS)

Tài liệu này tổng hợp toàn bộ các đường dẫn (URL), phân loại trang **Công khai (Không cần đăng nhập)** và **Nội bộ (Yêu cầu đăng nhập)**, cùng thông tin tài khoản mẫu và hướng dẫn truy cập hệ thống.

---

## 🚀 1. Hướng dẫn Khởi chạy Hệ thống

1. **Khởi động Server:**
   - Mở cửa sổ dòng lệnh (Terminal / CMD / PowerShell) tại thư mục gốc của dự án `e:\ttcs_n1`.
   - Chạy file thực thi:
     ```cmd
     .\run.bat
     ```
   - Server Tomcat tích hợp sẽ lắng nghe tại cổng: `http://localhost:8088`
2. **Địa chỉ gốc:**
   - Nếu truy cập thẳng `http://localhost:8088` khi chưa đăng nhập, hệ thống sẽ tự động chuyển hướng tới trang Đăng nhập (`/auth/login`).

---

## 🌍 2. CÁC TRANG CÔNG KHAI (KHÔNG CẦN ĐĂNG NHẬP)

Những trang này dành cho ứng viên vãng lai, người tìm việc và khách thăm quan, được miễn trừ xác thực phiên đăng nhập trong `AuthFilter`:

| Tên Trang | Đường dẫn (URL) | Mô tả & Chức năng |
| :--- | :--- | :--- |
| **Trang Tuyển dụng công khai** | [`http://localhost:8088/careers`](http://localhost:8088/careers)<br>*(hoặc `/jobs`)* | Xem danh sách các bài đăng tuyển dụng đang mở (`status = published`, chưa hết hạn). Tìm kiếm theo tiêu đề/mô tả, lọc theo Phòng ban, Địa điểm làm việc, Hình thức làm việc. |
| **Trang Chi tiết việc làm & Nộp CV** | `http://localhost:8088/careers/job?id={requisitionId}`<br>*(hoặc `/jobs/detail?id={id}`)* | Xem đầy đủ JD (Mô tả, Yêu cầu, Đãi ngộ, Hạn nộp). Tích hợp form nộp hồ sơ kéo thả CV (`.pdf`, `.docx`), chống bot spam, chống nộp trùng và nhận mã tra cứu `APP-YYYYMMDD-XXXX`. |
| **Trang Giới thiệu công ty (About Us)** | [`http://localhost:8088/about-us`](http://localhost:8088/about-us)<br>*(hoặc `/company/about`)* | Xem thông tin công ty, văn hóa doanh nghiệp, giá trị cốt lõi và các chế độ phúc lợi. |
| **Trang Đăng nhập hệ thống** | [`http://localhost:8088/auth/login`](http://localhost:8088/auth/login) | Màn hình đăng nhập tài khoản nhân viên nội bộ. |
| **Trang Quên mật khẩu** | [`http://localhost:8088/auth/forgot-password`](http://localhost:8088/auth/forgot-password) | Yêu cầu gửi email liên kết đặt lại mật khẩu. |
| **Trang Đặt lại mật khẩu** | `http://localhost:8088/auth/reset-password?token={token}` | Thiết lập mật khẩu mới thông qua mã token xác thực từ email. |

### 📄 Xem trước giao diện tĩnh (Frontend Standalone Preview)
Nếu chưa khởi động server Tomcat, bạn có thể mở trực tiếp file giao diện HTML độc lập bằng trình duyệt:
- Danh sách việc làm: `file:///e:/ttcs_n1/frontend/careers.html`
- Chi tiết việc làm: `file:///e:/ttcs_n1/frontend/job-detail.html`

### 🔌 Danh sách Public & Internal API tiêu biểu
- **`GET /api/v1/public/jobs`**: Lấy danh sách việc làm dạng JSON (hỗ trợ các query params: `keyword`, `department`, `location`, `workType`, `page`, `pageSize`).
- **`GET /api/v1/public/jobs/detail?id={id}`**: Lấy chi tiết tin tuyển dụng công khai kèm trạng thái còn hạn/hết hạn dạng JSON (KN-227).
- **`POST /api/v1/public/jobs/apply`**: Nộp hồ sơ ứng tuyển dạng `multipart/form-data` (họ tên, email, sđt, cv file, token chống spam...).
- **`GET /api/v1/public/jobs/apply?trackingCode=APP-YYYYMMDD-XXXX`**: Tra cứu trạng thái hồ sơ đã nộp bằng mã tracking.
- **`GET /api/candidates/profile?id={id}`**: API lấy dữ liệu hồ sơ ứng viên hợp nhất (thông tin cá nhân, nhãn, ghi chú, lịch sử ứng tuyển) có kiểm tra phân quyền RBAC.
- **`GET /candidates/cv-preview?id={id}`**: API stream xem trước CV an toàn (bảo vệ inline preview, chống path traversal, kiểm tra quyền truy cập).

---

## 🔒 3. CÁC TRANG QUẢN TRỊ NỘI BỘ (YÊU CẦU ĐĂNG NHẬP)

Để truy cập các trang này, người dùng **phải đăng nhập** tài khoản có vai trò và quyền hạn tương ứng:

| Phân hệ / Tính năng | Đường dẫn (URL) | Vai trò được phép |
| :--- | :--- | :--- |
| **Bảng điều khiển (Dashboard)** | [`http://localhost:8088/dashboard`](http://localhost:8088/dashboard) | ADMIN, HR_MANAGER, HIRING_MANAGER, APPROVER |
| **Quản trị Người dùng** | [`http://localhost:8088/admin/users`](http://localhost:8088/admin/users) | ADMIN (hoặc quyền `users.view`) |
| **Quản lý Vai trò** | [`http://localhost:8088/admin/roles`](http://localhost:8088/admin/roles) | ADMIN (hoặc quyền `roles.view`) |
| **Ma trận phân quyền (RBAC)** | [`http://localhost:8088/admin/permissions`](http://localhost:8088/admin/permissions) | ADMIN (hoặc quyền `permissions.view`) |
| **Cấu hình Ma trận phê duyệt** | [`http://localhost:8088/admin/approval-matrix`](http://localhost:8088/admin/approval-matrix) | ADMIN, HR_MANAGER |
| **Nhật ký kiểm toán (Audit Logs)** | [`http://localhost:8088/admin/audit-logs`](http://localhost:8088/admin/audit-logs) | ADMIN (hoặc quyền `audit.view`) |
| **Cơ cấu tổ chức Phòng ban** | [`http://localhost:8088/departments`](http://localhost:8088/departments) | ADMIN, HR_MANAGER, HIRING_MANAGER |
| **Yêu cầu tuyển dụng** | [`http://localhost:8088/recruitment-requests`](http://localhost:8088/recruitment-requests) | ADMIN, HR_MANAGER, HIRING_MANAGER, RECRUITER |
| **Trung tâm Phê duyệt Tuyển dụng** | [`http://localhost:8088/recruitment-requests/approvals`](http://localhost:8088/recruitment-requests/approvals) | APPROVER, HR_MANAGER, ADMIN |
| **Quản lý Xuất bản Tin tuyển dụng** | [`http://localhost:8088/post-publishing`](http://localhost:8088/post-publishing) | ADMIN, HR_MANAGER, RECRUITER |
| **Quản lý Hồ sơ ứng viên** | [`http://localhost:8088/candidates`](http://localhost:8088/candidates) | RECRUITER, HR_MANAGER, ADMIN, INTERVIEWER |
| **Hồ sơ ứng viên hợp nhất** | `http://localhost:8088/candidates/profile?id={candidateId}` | ADMIN, HR_MANAGER, RECRUITER phụ trách, HIRING_MANAGER của vị trí |
| **Định biên & Ngân sách Nhân sự** | [`http://localhost:8088/headcount-budgets`](http://localhost:8088/headcount-budgets) | ADMIN, HR_MANAGER, HIRING_MANAGER |
| **Quản lý Khung năng lực** | [`http://localhost:8088/competency-frameworks`](http://localhost:8088/competency-frameworks) | ADMIN, HR_MANAGER |
| **Quản lý Dải lương tuyển dụng** | [`http://localhost:8088/salary-ranges`](http://localhost:8088/salary-ranges) | ADMIN, HR_MANAGER *(Bảo mật riêng)* |
| **Danh mục dùng chung** | [`http://localhost:8088/shared-catalogs`](http://localhost:8088/shared-catalogs) | ADMIN, HR_MANAGER |
| **Hồ sơ cá nhân & Đổi mật khẩu** | [`http://localhost:8088/profile`](http://localhost:8088/profile) | Tất cả người dùng đã đăng nhập |
| **Giới thiệu ứng viên nội bộ** | [`http://localhost:8088/referrals`](http://localhost:8088/referrals) | Tất cả nhân viên nội bộ đã đăng nhập |
| **⭐ Chấm điểm & Xếp hạng hồ sơ tự động** | [`http://localhost:8088/screening-criteria`](http://localhost:8088/screening-criteria) | ADMIN, HR_MANAGER, RECRUITER |
| **Cấu hình tiêu chí sàng lọc** | `http://localhost:8088/screening-criteria/setup?requisitionId={id}` | ADMIN, HR_MANAGER, RECRUITER |

---

## 🔑 4. Danh sách Tài khoản Kiểm thử Mặc định

*Mật khẩu mặc định cho tất cả các tài khoản nội bộ là:* **`Admin@123456`**

| Vai trò | Email đăng nhập | Tên người dùng | Phạm vi quyền hạn |
| :--- | :--- | :--- | :--- |
| **Quản trị hệ thống (ADMIN)** | `admin@company.local` | Nguyễn Quản Trị | Toàn quyền kiểm soát tất cả phân hệ, người dùng, phân quyền RBAC và cấu hình. |
| **Trưởng phòng Nhân sự (HR_MANAGER)** | `hr.manager@company.local` | Trần Thị Trưởng Phòng Nhân Sự | Quản lý quy trình nhân sự, phê duyệt, dải lương, định biên và xuất bản tin. |
| **Chuyên viên tuyển dụng (RECRUITER)** | `recruiter@company.local` | Lê Chuyên Viên Tuyển Dụng | Quản lý pipeline ứng viên, bài đăng tin, điều phối phỏng vấn (không xem dải lương). |
| **Trưởng bộ phận (HIRING_MANAGER)** | `hiring.mgr@company.local` | Phạm Trưởng Bộ Phận Kỹ Thuật | Tạo đề xuất tuyển dụng theo phòng ban, xem định biên headcount kỹ thuật. |
| **Người phỏng vấn (INTERVIEWER)** | `interviewer@company.local` | Hoàng Chuyên Gia Phỏng Vấn | Xem danh sách ứng viên và chấm điểm đánh giá phỏng vấn được phân công. |
| **Người phê duyệt (APPROVER)** | `approver@company.local` | Vũ Giám Đốc Điều Hành | Phê duyệt/từ chối các yêu cầu tuyển dụng trong luồng ma trận phê duyệt. |

---

## 💡 5. Mẹo Nhanh Khi Trải Nghiệm

1. **Xem trang ứng viên nộp hồ sơ:**
   - Mở cửa sổ ẩn danh (Incognito) hoặc trình duyệt phụ.
   - Truy cập ngay `http://localhost:8088/careers`.
   - Chọn một công việc $\rightarrow$ Bấm **"Ứng tuyển ngay"** $\rightarrow$ Điền thông tin và đính kèm CV (`.pdf` hoặc `.docx`).
   - Nhận mã tra cứu (VD: `APP-20261014-9988`).
2. **Xem hồ sơ vừa nộp trong trang quản trị:**
   - Mở trình duyệt chính, đăng nhập bằng tài khoản `recruiter@company.local` hoặc `hr.manager@company.local` với mật khẩu `Admin@123456`.
   - Vào menu **"Hồ sơ ứng viên"** (`/candidates`) để kiểm tra hồ sơ ứng viên và file CV vừa được tải lên.
3. **Thử tính năng Chấm điểm & Xếp hạng hồ sơ tự động:**
   - Đăng nhập bằng `recruiter@company.local` hoặc `hr.manager@company.local`.
   - Vào menu **"Chấm điểm hồ sơ"** (`/screening-criteria`).
   - Bấm **"Cấu hình tiêu chí mới"** → Chọn vị trí tuyển dụng → Nhập yêu cầu kinh nghiệm, bằng cấp, kỹ năng bắt buộc → **Lưu**.
   - Bấm **"Chấm điểm hàng loạt"** → Hệ thống tính điểm (0–100) và gắn cờ ĐẠT/KHÔNG ĐẠT cho từng hồ sơ.
   - Vào **"Danh sách ứng viên"** → Thấy cột **Điểm & Xếp hạng** hiển thị inline.
   - Bấm nút ⚡ **"Chấm điểm"** trên từng hồ sơ để xem báo cáo chi tiết ngay lập tức.

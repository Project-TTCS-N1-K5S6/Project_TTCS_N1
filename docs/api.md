# DANH MỤC SERVLET & API ENDPOINTS — IRMS (JAVA SERVLET & MYSQL)

Tài liệu chi tiết toàn bộ các đường dẫn URL, phương thức HTTP, phân quyền bảo mật và định dạng dữ liệu (HTML SSR & AJAX JSON) của hệ thống Quản lý Tuyển dụng Nội bộ (IRMS).

---

## 1. QUY CHUẨN XỬ LÝ YÊU CẦU & PHẢN HỒI

Hệ thống hỗ trợ song song hai hình thức xử lý qua Java Servlet:
1. **Server-Side Rendering (JSP View):** Với các request duyệt trang thông thường (Accept: `text/html`), Servlet chuyển tiếp (forward) dữ liệu sang tập tin JSP tương ứng trong `/WEB-INF/views/`.
2. **AJAX / JSON API (Accept: `application/json`):** Trả về chuỗi JSON theo cấu trúc chuẩn:
   - **Thành công (200, 201):**
     ```json
     {
       "success": true,
       "message": "Thông điệp xử lý thành công",
       "data": { ... }
     }
     ```
   - **Thất bại / Lỗi nghiệp vụ (400, 401, 403, 404, 500):**
     ```json
     {
       "success": false,
       "message": "Mô tả nguyên nhân lỗi cụ thể",
       "data": null
     }
     ```

---

## 2. PHÂN HỆ XÁC THỰC & BẢO MẬT PHIÊN (`/auth/*`)

| Phương thức | Đường dẫn URL | Quyền yêu cầu | Mô tả chức năng |
| :--- | :--- | :--- | :--- |
| `GET` | `/auth/login` | Công khai | Hiển thị màn hình đăng nhập |
| `POST` | `/auth/login` | Công khai | Tiếp nhận email & mật khẩu, xác thực BCrypt, thiết lập HttpSession |
| `POST` | `/auth/logout` | Đã đăng nhập | Hủy phiên làm việc `session.invalidate()` và chuyển hướng về đăng nhập |
| `GET` | `/auth/forgot-password` | Công khai | Hiển thị form yêu cầu cấp lại mật khẩu qua email |
| `POST` | `/auth/forgot-password` | Công khai | Tiếp nhận email, kiểm tra tồn tại và tạo token đặt lại |
| `GET` | `/auth/change-password` | Đã đăng nhập | Hiển thị form đổi mật khẩu cá nhân / bắt buộc |
| `POST` | `/auth/change-password` | Đã đăng nhập | Kiểm tra mật khẩu cũ, băm mật khẩu mới, cập nhật `session_version` |

### Tham số POST `/auth/login`:
- `email` (String, required): Địa chỉ email nhân viên
- `password` (String, required): Mật khẩu truy cập
- **Xử lý Brute-Force:** Đăng nhập sai quá 5 lần sẽ khóa tài khoản 15 phút (`locked_until = NOW() + INTERVAL 15 MINUTE`).

---

## 3. BẢNG ĐIỀU KHIỂN TỔNG QUAN (`/dashboard`)

| Phương thức | Đường dẫn URL | Quyền yêu cầu | Mô tả chức năng |
| :--- | :--- | :--- | :--- |
| `GET` | `/dashboard` | Đã đăng nhập | Hiển thị các chỉ số KPI: Tổng nhân sự, Ứng viên đang xử lý, Dải lương tuyển dụng |

---

## 4. QUẢN TRỊ TÀI KHOẢN NGƯỜI DÙNG (`/admin/users`)

| Phương thức | Đường dẫn URL | Quyền yêu cầu | Mô tả chức năng |
| :--- | :--- | :--- | :--- |
| `GET` | `/admin/users` | `users.view` | Danh sách tài khoản có tìm kiếm theo tên/email, lọc theo phòng ban |
| `POST` | `/admin/users` (action: create) | `users.create` | Tạo tài khoản nhân sự mới, băm mật khẩu BCrypt, kích hoạt flag `must_change_password` |
| `POST` | `/admin/users` (action: update) | `users.update` | Cập nhật họ tên, phòng ban, chức danh, số điện thoại |
| `POST` | `/admin/users` (action: lock) | `users.lock` | Khóa tài khoản, tăng `session_version` để thu hồi phiên ngay lập tức |
| `POST` | `/admin/users` (action: unlock) | `users.unlock` | Mở khóa tài khoản, reset bộ đếm `failed_login_attempts` |
| `POST` | `/admin/users` (action: reset_pwd) | `users.reset-password`| Cấp mật khẩu ngẫu nhiên tạm thời, ép đổi mật khẩu ở lần vào tiếp theo |

---

## 5. VAI TRÒ & MA TRẬN PHÂN QUYỀN (`/admin/roles` & `/admin/permissions`)

| Phương thức | Đường dẫn URL | Quyền yêu cầu | Mô tả chức năng |
| :--- | :--- | :--- | :--- |
| `GET` | `/admin/roles` | `roles.view` | Xem danh sách 7 vai trò hệ thống và số lượng người dùng |
| `GET` | `/admin/permissions` | `permissions.view` | Ma trận quyền hạn 30 quyền gom nhóm theo 10 phân hệ |
| `POST` | `/admin/permissions` | `permissions.manage` | Cập nhật danh sách quyền cho vai trò (xử lý qua AJAX JSON) |

---

## 6. QUẢN LÝ PHÒNG BAN (`/admin/departments`)

| Phương thức | Đường dẫn URL | Quyền yêu cầu | Mô tả chức năng |
| :--- | :--- | :--- | :--- |
| `GET` | `/admin/departments` | Đã đăng nhập | Xem cơ cấu phòng ban và số lượng nhân sự |
| `POST` | `/admin/departments` (action: create) | `departments.manage` | Thêm phòng ban tổ chức mới |
| `POST` | `/admin/departments` (action: update) | `departments.manage` | Cập nhật tên và mô tả phòng ban |

---

## 7. QUY TRÌNH HỒ SƠ ỨNG VIÊN & PIPELINE (`/candidates`)

| Phương thức | Đường dẫn URL | Quyền yêu cầu | Mô tả chức năng |
| :--- | :--- | :--- | :--- |
| `GET` | `/candidates` | `candidates.view` | Danh sách hồ sơ ứng viên, lọc theo trạng thái vòng tuyển dụng |
| `POST` | `/candidates` (action: create) | `candidates.create` | Tiếp nhận hồ sơ ứng viên mới vào hệ thống |
| `POST` | `/candidates` (action: update_status)| `candidates.manage` | Chuyển giai đoạn ứng viên (SCREENING -> INTERVIEWING -> OFFER -> HIRED/REJECTED) |

---

## 8. QUẢN LÝ DẢI LƯƠNG NGÂN SÁCH (`/salary-ranges`)

| Phương thức | Đường dẫn URL | Quyền yêu cầu | Mô tả chức năng |
| :--- | :--- | :--- | :--- |
| `GET` | `/salary-ranges` | `salary.view` | Danh mục dải lương ngân sách theo vị trí (Chặn hoàn toàn role INTERVIEWER) |
| `POST` | `/salary-ranges` (action: create) | `salary.manage` | Thêm hạn mức lương tối thiểu - tối đa |
| `POST` | `/salary-ranges` (action: update) | `salary.manage` | Điều chỉnh dải lương |

---

## 9. NHẬT KÝ KIỂM TOÁN AN NINH (`/admin/audit-logs`)

| Phương thức | Đường dẫn URL | Quyền yêu cầu | Mô tả chức năng |
| :--- | :--- | :--- | :--- |
| `GET` | `/admin/audit-logs` | `audit.view` | Tra cứu lịch sử đăng nhập, khóa/mở tài khoản, phân quyền kèm IP và User-Agent |

---

## 10. PHÂN HỆ MỞ RỘNG (PHASE 2 PLACEHOLDERS)

Các URL sau được điều phối bởi [PlaceholderServlet](file:///e:/ttcs_n1/backend/src/main/java/com/irms/controller/PlaceholderServlet.java), sẵn sàng cho các giai đoạn nâng cấp tiếp theo:
- `/recruitment-requests/*` - Quản lý Yêu cầu tuyển dụng
- `/job-postings/*` - Quản lý Tin tuyển dụng nội bộ và công khai
- `/interviews/*` - Điều phối Lịch phỏng vấn & Phiếu đánh giá
- `/offers/*` - Soạn thảo & Phê duyệt Thư mời nhận việc (Offer Letter)
- `/onboarding/*` - Thủ tục Tiếp nhận nhân sự mới
- `/notifications/*` - Hộp thư & Thông báo tự động
- `/reports/*` - Báo cáo chỉ số và hiệu quả tuyển dụng

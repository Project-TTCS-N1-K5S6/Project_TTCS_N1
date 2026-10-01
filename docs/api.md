# DANH MỤC REST API IRMS — SPRINT 1

Quy chuẩn phản hồi:
- **Thành công (200, 201):** `{ "success": true, "data": ..., "message": "..." }`
- **Thất bại (400, 401, 403, 404, 409, 500):** `{ "success": false, "data": null, "message": "...", "errors": [] }`
- **Lỗi xác thực dữ liệu (422):** `{ "success": false, "data": null, "message": "Dữ liệu không hợp lệ.", "errors": [{ "field": "...", "message": "..." }] }`

---

## 1. AUTHENTICATION & SESSION (`/api/auth`)

| Phương thức | Đường dẫn | Quyền yêu cầu | Mô tả |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Công khai | Đăng nhập tài khoản công ty, nhận Access Token & HttpOnly Cookie Refresh Token. Khóa tạm 15 phút nếu sai 5 lần. |
| `POST` | `/api/auth/refresh` | Công khai / Cookie | Đổi Refresh Token lấy Access Token mới (Token Rotation). |
| `POST` | `/api/auth/logout` | Đã đăng nhập | Thu hồi Refresh Token phía server và xóa cookie. |
| `POST` | `/api/auth/forgot-password`| Công khai | Gửi yêu cầu đặt lại mật khẩu. Trả về phản hồi generic an toàn. |
| `POST` | `/api/auth/reset-password` | Công khai | Đặt lại mật khẩu với token 30 phút, một lần dùng. |
| `POST` | `/api/auth/change-password`| Đã đăng nhập | Đổi mật khẩu chủ động. Thu hồi các phiên đăng nhập khác. |
| `GET` | `/api/auth/me` | Đã đăng nhập | Lấy thông tin tài khoản, danh sách vai trò và quyền hạn hiện tại. |

---

## 2. QUẢN TRỊ NGƯỜI DÙNG (`/api/users`)

| Phương thức | Đường dẫn | Quyền yêu cầu | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/users` | `users.view` | Lấy danh sách người dùng có phân trang (default 20), tìm kiếm, lọc theo phòng ban, vai trò, trạng thái. |
| `POST` | `/api/users` | `users.create` | Tạo người dùng mới, tự sinh mật khẩu tạm, gửi email kích hoạt. |
| `GET` | `/api/users/:id` | `users.view` | Xem chi tiết thông tin tài khoản, số lượng vị trí tuyển dụng phụ trách. |
| `PUT` | `/api/users/:id` | `users.update` | Cập nhật thông tin tài khoản (Họ tên, SĐT, Chức danh, Phòng ban, Trạng thái). |
| `POST` | `/api/users/:id/lock` | `users.lock` | Khóa tài khoản (bắt buộc lý do). Kiểm tra cảnh báo bàn giao nếu đang phụ trách vị trí OPEN. Thu hồi tất cả phiên làm việc. |
| `POST` | `/api/users/:id/unlock` | `users.unlock` | Mở khóa tài khoản cho phép đăng nhập lại. |
| `GET` | `/api/users/:id/roles` | `roles.view` | Lấy danh sách vai trò được gán cho người dùng. |
| `POST` | `/api/users/:id/roles` | `roles.assign` | Gán thêm vai trò cho người dùng (Hỗ trợ đa vai trò). |
| `DELETE` | `/api/users/:id/roles/:roleId` | `roles.revoke` | Thu hồi vai trò. Ngăn cản Admin tự thu hồi quyền Admin của chính mình. |

---

## 3. VAI TRÒ & PHÂN QUYỀN (`/api/roles` & `/api/permissions`)

| Phương thức | Đường dẫn | Quyền yêu cầu | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/roles` | `roles.view` | Xem danh sách 7 vai trò hệ thống, số lượng quyền và số người dùng mỗi vai trò. |
| `GET` | `/api/roles/:id` | `roles.view` | Xem chi tiết vai trò kèm danh sách quyền được cấp. |
| `POST` | `/api/roles` | `roles.create` | Tạo vai trò mới. |
| `PUT` | `/api/roles/:id` | `roles.update` | Cập nhật thông tin vai trò. |
| `GET` | `/api/roles/:id/permissions`| `roles.view` | Lấy danh sách ID quyền được gán cho vai trò. |
| `PUT` | `/api/roles/:id/permissions`| `permissions.manage`| Cập nhật ma trận quyền hạn cho vai trò. |
| `GET` | `/api/permissions` | `permissions.view` | Lấy toàn bộ quyền hệ thống gom nhóm theo module. |

---

## 4. PHÒNG BAN & NHẬT KÝ (`/api/departments` & `/api/audit-logs`)

| Phương thức | Đường dẫn | Quyền yêu cầu | Mô tả |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/departments` | Đã đăng nhập | Lấy danh mục phòng ban tổ chức. |
| `GET` | `/api/audit-logs` | `audit.view` | Lấy danh sách nhật ký an ninh, lịch sử đăng nhập và thao tác quản trị. |

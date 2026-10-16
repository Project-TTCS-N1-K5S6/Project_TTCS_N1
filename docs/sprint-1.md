# BÁO CÁO HOÀN THÀNH SPRINT 1 — HỆ THỐNG TUYỂN DỤNG NỘI BỘ (IRMS)

**Tên dự án:** Internal Recruitment Management System (IRMS)  
**Epic:** EP-01 — Tài khoản, Phân quyền & Quản trị người dùng  
**Sprint Point:** 42 point  
**Số lượng User Story:** 10/10  
**Tình trạng:** **HOÀN THÀNH 100% (DONE)**  
**Kiểm thử tự động:** **29/29 Integration Tests PASSED (100%)**  

---

## 1. MỤC TIÊU SPRINT (SPRINT GOAL)

> *"Quản trị viên tạo được tài khoản cho toàn bộ nhân sự tham gia tuyển dụng, mỗi vai trò đăng nhập vào chỉ nhìn thấy đúng phần menu thuộc quyền của mình."*

---

## 2. BẢNG TIẾN ĐỘ 10 USER STORY

| Mã Story | Tên User Story | Trọng số (SP) | Trạng thái | Kiểm thử tự động |
| :--- | :--- | :---: | :---: | :---: |
| **S1-01** | Đăng nhập hệ thống | 5 | **DONE** | Test 1, 2, 3, 4 |
| **S1-02** | Duy trì phiên và đăng xuất an toàn | 5 | **DONE** | Test 5, 6, 7, 8 |
| **S1-03** | Quên mật khẩu & Reset Token | 4 | **DONE** | Test 9, 10 |
| **S1-04** | Đổi mật khẩu chủ động | 3 | **DONE** | Test 11 |
| **S1-05** | Phân quyền theo vai trò (RBAC) | 5 | **DONE** | Test 12, 13, 14, 15 |
| **S1-06** | Menu điều hướng theo quyền | 4 | **DONE** | UI & Guard Verify |
| **S1-07** | Trang lỗi / Access Denied (403) | 3 | **DONE** | UI & Guard Verify |
| **S1-08** | Quản trị tài khoản nội bộ (CRUD, Phân trang) | 6 | **DONE** | Test 16, 17, 18, 19 |
| **S1-09** | Gán và thu hồi đa vai trò (Multi-role) | 4 | **DONE** | Test 20, 21, 22 |
| **S1-10** | Khóa/Mở khóa & Cảnh báo bàn giao | 3 | **DONE** | Test 23, 24, 25 |

---

## 3. CHI TIẾT KẾT QUẢ NGHIỆP VỤ TỪNG USER STORY

### S1-01 — Đăng nhập
- **Hiện thực:** `POST /api/auth/login`
- **Acceptance Criteria đạt:**
  1. Đăng nhập đúng trả về JWT Access Token + HttpOnly Refresh Token, điều hướng người dùng tới Dashboard tương ứng.
  2. Sai mật khẩu hoặc email không tồn tại trả về thông báo chung: *"Email hoặc mật khẩu không chính xác."*. Tuyệt đối không tiết lộ tài khoản có tồn tại hay không.
  3. Sau 5 lần nhập sai liên tiếp, tài khoản bị tạm khóa 15 phút (`locked_until = now() + 15m`). Trong thời gian này, mọi nỗ lực đăng nhập đều bị từ chối với thông báo: *"Tài khoản đang tạm khóa. Vui lòng thử lại sau."*.
  4. Đăng nhập thành công thiết lập lại `failed_login_attempts = 0` và `locked_until = NULL`.

### S1-02 — Duy trì phiên và đăng xuất an toàn
- **Hiện thực:** `POST /api/auth/refresh`, `POST /api/auth/logout`, `GET /api/auth/me`
- **Acceptance Criteria đạt:**
  1. Access Token ngắn hạn (15 phút). Refresh Token dài hạn (7 ngày) được mã hóa hash SHA-256 lưu trong bảng `refresh_tokens`.
  2. Hỗ trợ xoay vòng Refresh Token (Token Rotation) khi gọi `/api/auth/refresh`.
  3. Đăng xuất lập tức thu hồi phiên trên server (`revoked_at = CURRENT_TIMESTAMP`), xóa Cookie phía client.
  4. Axios interceptor trên Frontend tự động bắt mã 401 để refresh token ngầm; nếu refresh thất bại, xóa trạng thái đăng nhập và đưa người dùng về `/login` với thông báo: *"Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại."*.

### S1-03 — Quên mật khẩu / Reset Password
- **Hiện thực:** `POST /api/auth/forgot-password`, `POST /api/auth/reset-password`
- **Acceptance Criteria đạt:**
  1. Người dùng nhập email công ty. Hệ thống luôn phản hồi generic: *"Nếu email tồn tại trong hệ thống, chúng tôi sẽ gửi hướng dẫn đặt lại mật khẩu."* (Không để lộ email tồn tại).
  2. Nếu email có trong DB: Sinh token ngẫu nhiên 32 bytes, lưu hash SHA-256 vào `password_reset_tokens` với hiệu lực đúng 30 phút.
  3. Gửi email chứa liên kết đặt lại mật khẩu qua hàng đợi `email_outbox`.
  4. Đặt lại mật khẩu thành công sẽ đánh dấu `used_at = CURRENT_TIMESTAMP` (chỉ sử dụng được 1 lần) và thu hồi toàn bộ phiên đăng nhập cũ trên mọi thiết bị.

### S1-04 — Đổi mật khẩu
- **Hiện thực:** `POST /api/auth/change-password`
- **Acceptance Criteria đạt:**
  1. Bắt buộc nhập mật khẩu hiện tại và kiểm tra qua BCrypt so sánh với DB.
  2. Chính sách mật khẩu mới: Tối thiểu 8 ký tự, bắt buộc có ít nhất 1 chữ cái và 1 chữ số.
  3. Khi đổi mật khẩu thành công, thu hồi tất cả phiên đăng nhập trên các thiết bị khác.

### S1-05 — Phân quyền theo vai trò (RBAC cấp Permission)
- **Hiện thực:** Middleware `requirePermission(code)` kết hợp kiến trúc phân quyền động trong DB (`roles`, `permissions`, `user_roles`, `role_permissions`).
- **Acceptance Criteria đạt:**
  1. Hỗ trợ đúng 7 vai trò: `CANDIDATE`, `RECRUITER`, `HIRING_MANAGER`, `INTERVIEWER`, `HR_MANAGER`, `APPROVER`, `ADMIN`.
  2. Cơ chế **Deny by default**: Nếu người dùng không sở hữu quyền tương ứng trong DB -> trả về HTTP 403 Forbidden với message: *"Bạn không có quyền thực hiện thao tác này."*.
  3. Role `ADMIN` sở hữu toàn quyền hệ thống.
  4. Đã có automated test kiểm chứng tối thiểu 4 role: `ADMIN` (200), `HR_MANAGER` (200), `RECRUITER` (403), `INTERVIEWER` (403).

### S1-06 — Menu điều hướng theo quyền
- **Hiện thực:** Component `AppLayout` lọc menu động qua hàm `hasPermission`.
- **Acceptance Criteria đạt:**
  1. Menu không thuộc quyền sẽ bị ẩn hoàn toàn khỏi Sidebar và Header.
  2. Header hiển thị tên đầy đủ của người dùng, huy hiệu vai trò (`Quản trị hệ thống`, `Chuyên viên tuyển dụng`, v.v.), avatar và dropdown thông tin.
  3. Giao diện đáp ứng mượt mà (responsive) từ màn hình di động 360px: Sidebar tự động chuyển thành Drawer trượt, Header co giãn thông minh.
  4. Bảo mật 2 lớp: Nếu người dùng cố tình gõ URL trực tiếp (ví dụ `/admin/users`), Route Guard chặn lại và Backend trả về 403.

### S1-07 — Trang lỗi / Access Denied (403 & 404)
- **Hiện thực:** `src/pages/errors/ForbiddenPage.tsx`, `NotFoundPage.tsx`
- **Acceptance Criteria đạt:**
  1. Giao diện 403 chuyên nghiệp với icon minh họa, tiêu đề và giải thích rõ ràng.
  2. Cung cấp 2 nút điều hướng: *"Quay lại trang trước"* và *"Về trang chủ"*.
  3. Tuyệt đối không để lộ stack trace, thông tin SQL hay connection string kỹ thuật.

### S1-08 — Quản trị tài khoản nội bộ
- **Hiện thực:** `GET /api/users`, `POST /api/users`, `GET /api/users/:id`, `PUT /api/users/:id`
- **Acceptance Criteria đạt:**
  1. Quản trị viên tạo tài khoản mới: Tự động sinh mật khẩu tạm ngẫu nhiên, mã hóa BCrypt, xếp lịch gửi email kích hoạt vào `email_outbox`.
  2. Từ chối email trùng lặp với mã lỗi HTTP 409 Conflict.
  3. Tìm kiếm theo tên, email, số điện thoại, mã nhân viên.
  4. Lọc đa điều kiện: Phòng ban, Vai trò, Trạng thái (`ACTIVE`, `LOCKED`, `INACTIVE`).
  5. Phân trang server-side chuẩn 20 dòng/trang mặc định (có thể chọn 10, 20, 50).

### S1-09 — Gán và thu hồi đa vai trò
- **Hiện thực:** `POST /api/users/:id/roles`, `DELETE /api/users/:id/roles/:roleId`
- **Acceptance Criteria đạt:**
  1. Một tài khoản có thể sở hữu đồng thời nhiều vai trò (ví dụ: vừa là Hiring Manager vừa là Interviewer như tài khoản `leader.tech@company.local`).
  2. Quản trị viên có thể gán thêm vai trò hoặc thu hồi vai trò bất kỳ lúc nào, có hiệu lực ngay ở request tiếp theo.
  3. **Quy tắc bảo vệ tối cao:** Backend kiểm tra và từ chối nghiêm ngặt trường hợp Admin tự thu hồi quyền Admin của chính mình (`targetUser == currentUser AND role == 'ADMIN'` -> 400 Bad Request: *"Bạn không thể tự thu hồi quyền quản trị của chính mình."*).

### S1-10 — Khóa và mở khóa tài khoản & Cảnh báo bàn giao
- **Hiện thực:** `POST /api/users/:id/lock`, `POST /api/users/:id/unlock`
- **Acceptance Criteria đạt:**
  1. Khóa tài khoản thủ công bắt buộc phải nhập lý do (tối thiểu 3 ký tự).
  2. Khi khóa: Trạng thái chuyển thành `LOCKED`, lưu thời gian khóa, người thực hiện khóa, và **thu hồi toàn bộ Refresh Token** đang mở của người đó.
  3. **Kiểm tra bàn giao (Handover Check):** Nếu người dùng đang phụ trách vị trí tuyển dụng có trạng thái `OPEN` (vai trò Recruiter hoặc Hiring Manager), backend trả về cảnh báo `requiresHandoverWarning: true` kèm số lượng vị trí. Frontend hiển thị modal cảnh báo yêu cầu Admin xác nhận bàn giao trước khi tiếp tục khóa.
  4. Mở khóa tài khoản: Reset trạng thái về `ACTIVE`, xóa lý do khóa, xóa đếm số lần sai mật khẩu, cho phép đăng nhập bình thường.

---

## 4. KẾT QUẢ KIỂM THỬ TỰ ĐỘNG (AUTOMATED TEST SUITE)

```bash
✓ tests/api.test.ts (29 tests) 1556ms
  S1-01 & S1-02: Authentication & Sessions
    ✓ 1. Đăng nhập thành công với tài khoản Admin
    ✓ 2. Đăng nhập sai mật khẩu trả về generic error và không tiết lộ email
    ✓ 3. Đăng nhập với email không tồn tại trả về generic error giống hệt
    ✓ 4. Nhập sai 5 lần liên tiếp sẽ tạm khóa tài khoản 15 phút (S1-01 AC4)
    ✓ 5. Làm mới Access Token thông qua Refresh Token thành công (S1-02)
    ✓ 6. Refresh Token đã bị thu hồi hoặc không hợp lệ sẽ trả về 401
    ✓ 7. Đăng xuất làm mất hiệu lực Refresh Token phía server (S1-02 AC2)
    ✓ 8. Lấy thông tin phiên hiện tại qua /api/auth/me
  S1-03 & S1-04: Password Management
    ✓ 9. Quên mật khẩu trả về generic response cho cả email tồn tại và không tồn tại
    ✓ 10. Đặt lại mật khẩu thành công với token hợp lệ và vô hiệu hóa sau 1 lần dùng
    ✓ 11. Đổi mật khẩu thành công và kiểm tra chính sách mật khẩu (S1-04)
  S1-05 & S1-06: Role-Based Access Control for >= 3 Roles
    ✓ 12. Role 1 - ADMIN truy cập GET /api/users thành công (200)
    ✓ 13. Role 2 - HR_MANAGER truy cập GET /api/users thành công vì có permission users.view (200)
    ✓ 14. Role 3 - RECRUITER truy cập GET /api/users bị từ chối với 403 Forbidden
    ✓ 15. Role 4 - INTERVIEWER truy cập GET /api/users bị từ chối với 403 Forbidden
  S1-08: Internal User Management
    ✓ 16. Admin tạo tài khoản người dùng mới thành công và sinh mật khẩu tạm
    ✓ 17. Tạo tài khoản trùng email bị từ chối với 409 Conflict
    ✓ 18. Tìm kiếm, lọc và phân trang 20 dòng mặc định
    ✓ 19. Cập nhật thông tin tài khoản người dùng thành công
  S1-09: Assign & Revoke Roles
    ✓ 20. Gán thêm vai trò cho người dùng (Một người nhiều vai trò)
    ✓ 21. Thu hồi vai trò thành công
    ✓ 22. Admin KHÔNG ĐƯỢC tự thu hồi quyền ADMIN của chính mình (S1-09 AC5)
  S1-10: Account Lock & Handover Warning
    ✓ 23. Khóa tài khoản người dùng đang phụ trách vị trí OPEN sẽ hiển thị cảnh báo bàn giao
    ✓ 24. Khóa tài khoản khi xác nhận (force: true) hoặc khi không có vị trí phụ trách
    ✓ 25. Mở khóa tài khoản thành công (S1-10)
  Roles & Permissions Matrix APIs
    ✓ 26. Lấy danh sách 7 vai trò hệ thống
    ✓ 27. Lấy danh sách quyền hệ thống gom nhóm theo module
    ✓ 28. Cập nhật ma trận phân quyền cho vai trò
  Audit Logs APIs
    ✓ 29. Xem nhật ký hệ thống với quyền audit.view

Test Files  1 passed (1)
Tests       29 passed (29)
Duration    100% Success
```

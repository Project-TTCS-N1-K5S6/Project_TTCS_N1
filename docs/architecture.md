# KIẾN TRÚC HỆ THỐNG IRMS — SPRINT 1

## 1. TỔNG QUAN KIẾN TRÚC

Hệ thống Tuyển dụng Nội bộ (IRMS) áp dụng mô hình **Clean Layered Architecture** tách biệt hoàn toàn giữa Frontend (React/TypeScript) và Backend (Node.js/Express/TypeScript) với cơ sở dữ liệu quan hệ PostgreSQL chuẩn enterprise.

```
+-------------------------------------------------------------------------+
|                           CLIENT TẦNG FRONTEND                          |
|             React 18 + Vite + Ant Design 5 + React Router 6             |
|    - Permission-aware Sidebar & Header (Mobile Drawer down to 360px)    |
|    - Silent Axios JWT Auto-Refresh Interceptor                          |
|    - Client-side Route Guard (ProtectedRoute, PermissionRoute)          |
+-------------------------------------------------------------------------+
                                   |  HTTPS / JSON / HttpOnly Cookie
                                   v
+-------------------------------------------------------------------------+
|                        API GATEWAY / SECURITY LAYER                     |
|  - Helmet Security Headers                                              |
|  - CORS Origin Validation                                               |
|  - Structured Safe Logger (Never logs passwords/tokens)                 |
|  - Global Exception Handler (No stack trace/SQL leak)                   |
|  - JWT Bearer Authentication Middleware                                 |
|  - RBAC Permission Enforcement Middleware (Deny-by-default)             |
+-------------------------------------------------------------------------+
                                   |
                                   v
+-------------------------------------------------------------------------+
|                      APPLICATION / CONTROLLER LAYER                     |
|  - AuthController, UsersController, RolesController, PermissionsCtrl    |
|  - Zod DTO Request Body & Query Validation                              |
+-------------------------------------------------------------------------+
                                   |
                                   v
+-------------------------------------------------------------------------+
|                        DOMAIN / SERVICE LAYER                           |
|  - AuthService: Lockout (5 attempts/15m), Token Rotation, Revocation    |
|  - UsersService: CRUD, Server-side pagination, Handover Requisition Chk |
|  - RolesService: Multi-role Assignment, Self-admin Revoke Protection    |
|  - AuditService: Security Activity Logs                                 |
|  - EmailService: Async Outbox Dispatcher (Activation, Password Reset)   |
+-------------------------------------------------------------------------+
                                   |
                                   v
+-------------------------------------------------------------------------+
|                        DATA ACCESS & PERSISTENCE                        |
|  - PostgreSQL 16+ (Database: ttcs_db)                                   |
|  - Connection Pooling with pg.Pool                                      |
|  - Flyway-style SQL Schema Migrations & Automated Seeding               |
|  - Transactional Isolation (BEGIN ... COMMIT / ROLLBACK)                |
+-------------------------------------------------------------------------+
```

---

## 2. BẢO MẬT & QUẢN LÝ PHIÊN (SECURITY ARCHITECTURE)

### A. Mô hình Token Kép (Access Token + Refresh Token)
1. **Access Token:**
   - Thời lượng ngắn (15 phút).
   - Chứa định danh `userId`, `email`, danh sách mã `roles`, danh sách `permissions`.
   - Được gửi qua HTTP Header: `Authorization: Bearer <accessToken>`.
2. **Refresh Token:**
   - Thời lượng dài hơn (7 ngày).
   - Được mã hóa một chiều qua thuật toán **SHA-256** trước khi lưu vào bảng `refresh_tokens`. Tuyệt đối không lưu plaintext token trong cơ sở dữ liệu.
   - Truyền tải qua **HttpOnly Cookie** (`irms_refresh_token`) với cờ `sameSite: 'lax'` và `path: '/api/auth'`, ngăn ngừa triệt để tấn công XSS trộm token.
   - Hỗ trợ **Token Rotation**: Mỗi lần đổi token mới, token cũ bị thu hồi và liên kết tới token mới (`replaced_by_token_id`).
   - Hỗ trợ thu hồi phiên từ phía server (`revoked_at`): Khi người dùng đăng xuất, đổi mật khẩu, hoặc tài khoản bị Admin khóa, các token đều lập tức bị vô hiệu hóa trong cơ sở dữ liệu.

### B. Kiểm soát Thử đăng nhập sai & Tạm khóa tài khoản (Brute-force Protection)
- Mỗi lần người dùng nhập sai mật khẩu, `failed_login_attempts` tăng thêm 1.
- Nếu đạt 5 lần liên tiếp:
  - Thiết lập `locked_until = now() + 15 minutes`.
  - Ghi nhật ký audit `ACCOUNT_TEMP_LOCKED`.
  - Trả về mã HTTP 403 Forbidden: *"Tài khoản đang tạm khóa. Vui lòng thử lại sau."*.
- Đăng nhập thành công: Tự động reset `failed_login_attempts = 0` và `locked_until = NULL`.

### C. Quên & Đặt lại mật khẩu an toàn
- Phản hồi **Generic Response**: Bất kể email có tồn tại hay không, API luôn trả về: *"Nếu email tồn tại trong hệ thống, chúng tôi sẽ gửi hướng dẫn đặt lại mật khẩu."*.
- Token đặt lại mật khẩu sinh ngẫu nhiên 32 bytes qua `crypto.randomBytes(32)`, lưu hash SHA-256 vào `password_reset_tokens`.
- Hiệu lực chính xác 30 phút, chỉ được sử dụng tối đa 01 lần duy nhất (`used_at IS NOT NULL`).

---

## 3. MÔ HÌNH PHÂN QUYỀN (PERMISSION-BASED RBAC)

Hệ thống áp dụng phân quyền dựa trên quyền hạn (Permission-based Authorization):
- **User N-N Role** thông qua bảng `user_roles`.
- **Role N-N Permission** thông qua bảng `role_permissions`.
- Định dạng quyền: `MODULE.ACTION` (Ví dụ: `users.view`, `users.create`, `users.lock`, `roles.assign`, `audit.view`, v.v.).
- Quy tắc **Deny by default**: Nếu người dùng không sở hữu quyền cụ thể hoặc vai trò của họ chưa được cấp quyền đó trong database, request lập tức bị từ chối với HTTP 403.
- Vai trò `ADMIN` được trang bị cơ chế Superuser bypass để luôn có toàn quyền quản trị hệ thống.

---

## 4. TÍNH SẴN SÀNG CHO SPRINT 2 - 8
Kiến trúc này được thiết kế để tiếp tục mở rộng cho các Sprint tiếp theo (Danh mục vị trí, Yêu cầu tuyển dụng, Đăng tin, Hồ sơ ứng viên, Phỏng vấn, Offer & Onboarding):
- Bảng `recruitment_requisitions` đã được chuẩn bị sẵn sàng và tích hợp kiểm tra ràng buộc bàn giao ở Sprint 1.
- Danh mục permissions đã bao gồm sẵn các module mở rộng (`requisitions.*`, `candidates.*`, `interviews.*`, `salary.*`).
- Frontend sử dụng cấu trúc module hóa theo tính năng (`features/` & `pages/`), giúp việc bổ sung trang mới không gây ảnh hưởng tới kiến trúc cốt lõi.

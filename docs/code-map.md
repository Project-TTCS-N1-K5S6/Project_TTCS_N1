# IRMS CODE MAP — SPRINT 1

Tài liệu ánh xạ toàn bộ tập tin mã nguồn (Code Map) cho Backend và Frontend của dự án IRMS.
Quy chuẩn bảng: **Hàm / Phương thức | Mục đích | Input | Output | Quyền yêu cầu | Ghi chú**.

---

# PHẦN 1: BACKEND (`backend/src`)

## 1. Core & Server

### `backend/src/server.ts`
| Hàm / Khối | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `startServer()` | Khởi động server HTTP Express & kiểm tra kết nối DB | Không | `Promise<void>` | Công khai | Lắng nghe trên cổng `PORT` (mặc định 5000) |

### `backend/src/app.ts`
| Hàm / Khối | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `createApp()` | Khởi tạo Express app, cấu hình CORS, Helmet, CookieParser, Swagger, Routes & Error Handler | Không | `Express Application` | Công khai | Đăng ký toàn bộ middleware và tiền tố `/api` |

---

## 2. Common & Config

### `backend/src/common/response.ts`
| Hàm | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `sendSuccess()` | Chuẩn hóa phản hồi thành công (HTTP 200/201) | `res, data, message, statusCode` | `Response` | Công khai | Định dạng `{ success: true, data, message }` |
| `sendError()` | Chuẩn hóa phản hồi thất bại | `res, message, statusCode, errors, data` | `Response` | Công khai | Định dạng `{ success: false, data, message, errors }` |

### `backend/src/common/exceptions.ts`
| Lớp / Hàm | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `HttpException` | Lớp lỗi cơ sở hỗ trợ statusCode, message, errors và data kèm theo | `statusCode, message, errors, data` | `Instance` | Công khai | Kế thừa `Error` |
| `BadRequestException` | Ném lỗi 400 Bad Request | `message, errors, data` | `Instance` | Công khai | Hỗ trợ meta data (ví dụ `remainingAttempts`) |
| `UnauthorizedException` | Ném lỗi 401 Unauthorized | `message` | `Instance` | Công khai | Dùng khi token hết hạn/bị thu hồi |
| `ForbiddenException` | Ném lỗi 403 Forbidden | `message, data` | `Instance` | Công khai | Dùng khi thiếu quyền hoặc tài khoản bị khóa |
| `NotFoundException` | Ném lỗi 404 Not Found | `message` | `Instance` | Công khai | Không tìm thấy bản ghi |
| `ConflictException` | Ném lỗi 409 Conflict | `message` | `Instance` | Công khai | Trùng lặp email, mã NV |
| `ValidationException` | Ném lỗi 422 Unprocessable Entity | `errors` | `Instance` | Công khai | Dành cho lỗi Zod schema validation |

### `backend/src/config/env.ts`
| Biến / Hàm | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `env` | Đọc, validate và export toàn bộ biến môi trường an toàn | `process.env` | `EnvConfig object` | Công khai | Sử dụng Zod schema để kiểm tra kiểu dữ liệu khi start app |

---

## 3. Database

### `backend/src/database/db.ts`
| Khối | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `pool` | Khởi tạo PostgreSQL connection pool qua thư viện `pg` | Chuỗi cấu hình DB | `pg.Pool` | Internal | Quản lý kết nối DB tối ưu |
| `query()` | Thực thi câu lệnh SQL qua pool | `text, params` | `Promise<QueryResult>` | Internal | Helper truy vấn nhanh |

### `backend/src/database/migrate.ts`
| Hàm | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `runMigrations()` | Quét thư mục `migrations/`, thực thi các file SQL theo thứ tự phiên bản | Không | `Promise<void>` | CLI / Admin | Bảng `schema_migrations` lưu lịch sử |

### `backend/src/database/seed.ts`
| Hàm | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `seedData()` | Khởi tạo dữ liệu mẫu cho hệ thống (Phòng ban, 7 Roles, Permissions, Admin & Users mặc định) | Không | `Promise<void>` | CLI / Admin | Idempotent, sử dụng `ON CONFLICT DO NOTHING` |

---

## 4. Middlewares

### `backend/src/middlewares/auth.middleware.ts`
| Hàm | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `authenticate()` | Xác thực JWT Bearer Token, kiểm tra trạng thái tài khoản và đối chiếu `sessionVersion` với DB | `req, res, next` | `void` | Công khai | Ném 401 nếu token sai/hết hạn hoặc `sessionVersion != user.session_version` |

### `backend/src/middlewares/permission.middleware.ts`
| Hàm | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `requirePermission()` | Kiểm tra quyền hạn trực tiếp từ DB cho từng request (Deny by default) | `permissionCode: string` | Express Middleware | Đã đăng nhập | ADMIN luôn có quyền; ném 403 nếu thiếu quyền |
| `requireAnyPermission()` | Cho phép truy cập nếu sở hữu ít nhất một trong các quyền chỉ định | `permissionCodes: string[]` | Express Middleware | Đã đăng nhập | Phục vụ các endpoint dùng chung |

### `backend/src/middlewares/error.middleware.ts`
| Hàm | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `errorHandler()` | Bắt toàn bộ lỗi chưa xử lý, không rò rỉ stack trace/SQL ra production | `err, req, res, next` | `Response` | Công khai | Ghi log chi tiết ở server, trả về phản hồi chuẩn hóa |

---

## 5. Modules

### Module `auth` (`backend/src/modules/auth/`)

#### `auth.service.ts`
| Hàm | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `getUserRolesAndPermissions()` | Lấy danh sách mã vai trò và danh sách quyền hợp nhất (Union) từ DB | `userId: string` | `{ roles: string[], permissions: string[] }` | Internal | Truy vấn DB thực tế mỗi lần gọi |
| `generateAccessToken()` | Tạo chuỗi JWT Access Token (15 phút) chứa thông tin người dùng và `sessionVersion` | `user, roles, permissions, sessionVersion` | `string` | Internal | Ký bằng `JWT_SECRET` |
| `createRefreshToken()` | Tạo Refresh Token (7 ngày), băm SHA-256 lưu DB, trả về plaintext token | `userId, ip, userAgent` | `Promise<string>` | Internal | Token rotation |
| `login()` | Xác thực đăng nhập, kiểm tra lockout (5 lần/15 phút), trả Access Token & Refresh Token | `LoginDto, ip, userAgent` | `Promise<LoginResult>` | Công khai | Trả `remainingAttempts` nếu tài khoản tồn tại |
| `refreshToken()` | Đổi Refresh Token lấy Access Token mới và xoay vòng Refresh Token | `rawRefreshToken, ip, userAgent` | `Promise<RefreshResult>` | Cookie / Body | Ném 401 nếu token bị thu hồi hoặc đã sử dụng |
| `logout()` | Thu hồi Refresh Token phía server | `rawRefreshToken` | `Promise<void>` | Đã đăng nhập | Cập nhật `revoked_at = NOW()` |
| `forgotPassword()` | Gửi email hướng dẫn đặt lại mật khẩu với token ngẫu nhiên 32 bytes | `ForgotPasswordDto, ip, userAgent` | `Promise<void>` | Công khai | Generic response an toàn, không lộ thông tin |
| `resetPassword()` | Đặt lại mật khẩu với token 30 phút, tăng `session_version`, thu hồi toàn bộ session | `ResetPasswordDto, ip, userAgent` | `Promise<void>` | Công khai | Gửi email xác nhận `PASSWORD_RESET_SUCCESS` |
| `changePassword()` | Người dùng tự đổi mật khẩu, tăng `session_version`, thu hồi tất cả session | `userId, ChangePasswordDto, ip, userAgent` | `Promise<void>` | Đã đăng nhập | Gửi email xác nhận `PASSWORD_CHANGED_SUCCESS` |
| `getCurrentUser()` | Lấy thông tin tài khoản đang đăng nhập, vai trò và quyền hạn | `userId: string` | `Promise<UserProfile>` | Đã đăng nhập | Kèm trạng thái `mustChangePassword` |

#### `auth.controller.ts`
| Phương thức | Mục đích | Endpoint | HTTP Method | Permission | Ghi chú |
|---|---|---|---|---|---|
| `login` | Xử lý request đăng nhập, set HttpOnly Cookie | `/api/auth/login` | POST | Công khai | Gọi `AuthService.login` |
| `refresh` | Xử lý request làm mới token | `/api/auth/refresh` | POST | Cookie | Gọi `AuthService.refreshToken` |
| `logout` | Xử lý request đăng xuất, xóa cookie | `/api/auth/logout` | POST | Đã đăng nhập | Gọi `AuthService.logout` |
| `forgotPassword` | Xử lý quên mật khẩu | `/api/auth/forgot-password` | POST | Công khai | Gọi `AuthService.forgotPassword` |
| `resetPassword` | Xử lý đặt lại mật khẩu | `/api/auth/reset-password` | POST | Công khai | Gọi `AuthService.resetPassword` |
| `changePassword` | Xử lý đổi mật khẩu chủ động | `/api/auth/change-password` | POST | Đã đăng nhập | Gọi `AuthService.changePassword` |
| `me` | Lấy thông tin phiên hiện tại | `/api/auth/me` | GET | Đã đăng nhập | Gọi `AuthService.getCurrentUser` |

---

### Module `users` (`backend/src/modules/users/`)

#### `users.service.ts`
| Hàm | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `getUsers()` | Lấy danh sách người dùng phân trang, tìm kiếm, lọc phòng ban/vai trò/trạng thái | `GetUsersQuery` | `Promise<PaginatedUsers>` | `users.view` | Phân trang server-side |
| `getUserById()` | Lấy chi tiết thông tin người dùng và số vị trí tuyển dụng đang phụ trách | `userId: string` | `Promise<UserDetails>` | `users.view` | Ném 404 nếu không tồn tại |
| `createUser()` | Tạo người dùng mới, sinh mật khẩu tạm, gửi email kích hoạt | `CreateUserDto, actorId, ip, userAgent` | `Promise<User>` | `users.create` | Gán `must_change_password = true` |
| `updateUser()` | Cập nhật thông tin họ tên, chức danh, phòng ban của người dùng | `userId, UpdateUserDto, actorId, ip, userAgent` | `Promise<User>` | `users.update` | Ghi audit log `USER_UPDATED` |
| `lockUser()` | Khóa tài khoản (kèm lý do), kiểm tra cảnh báo bàn giao, tăng `session_version` | `userId, LockUserDto, actorId, ip, userAgent` | `Promise<void>` | `users.lock` | Thu hồi toàn bộ session của tài khoản |
| `unlockUser()` | Mở khóa tài khoản người dùng | `userId, actorId, ip, userAgent` | `Promise<void>` | `users.unlock` | Reset `failed_login_attempts = 0` |
| `resetPassword()` | Admin cấp lại mật khẩu tạm thời, băm BCrypt, tăng `session_version`, gửi email | `userId, actorId, ip, userAgent` | `Promise<{ tempPassword?: string }>` | `users.reset-password` | Chỉ lộ `tempPassword` khi `EXPOSE_TEMP_PASSWORD_IN_DEV=true` |
| `getUserRoles()` | Lấy danh sách vai trò của người dùng | `userId: string` | `Promise<Role[]>` | `roles.view` | Truy vấn bảng `user_roles` |
| `assignRole()` | Gán vai trò cho người dùng (hỗ trợ đa vai trò) | `userId, roleId, actorId, ip, userAgent` | `Promise<void>` | `roles.assign` | Ghi audit log `ROLE_ASSIGNED` |
| `revokeRole()` | Thu hồi vai trò, bảo vệ Admin không tự thu hồi vai trò ADMIN của chính mình | `userId, roleId, actorId, ip, userAgent` | `Promise<void>` | `roles.revoke` | Chặn thu hồi nếu vi phạm self-admin protection |

#### `users.controller.ts`
| Phương thức | Mục đích | Endpoint | HTTP Method | Permission | Ghi chú |
|---|---|---|---|---|---|
| `getUsers` | Lấy danh sách người dùng | `/api/users` | GET | `users.view` | Hỗ trợ pagination & filter |
| `getUserById` | Lấy chi tiết người dùng | `/api/users/:id` | GET | `users.view` | Trả kèm vai trò và vị trí phụ trách |
| `createUser` | Tạo tài khoản người dùng mới | `/api/users` | POST | `users.create` | Gửi email kích hoạt |
| `updateUser` | Cập nhật thông tin tài khoản | `/api/users/:id` | PUT | `users.update` | Xác thực Zod schema |
| `lockUser` | Khóa tài khoản người dùng | `/api/users/:id/lock` | POST | `users.lock` | Yêu cầu `reason` |
| `unlockUser` | Mở khóa tài khoản | `/api/users/:id/unlock` | POST | `users.unlock` | Phục hồi quyền truy cập |
| `resetPassword` | Admin cấp lại mật khẩu tạm thời | `/api/users/:id/reset-password` | POST | `users.reset-password` | Vô hiệu hóa phiên cũ tức thời |
| `getUserRoles` | Lấy vai trò của người dùng | `/api/users/:id/roles` | GET | `roles.view` | Trả về danh sách roles |
| `assignRole` | Gán vai trò | `/api/users/:id/roles` | POST | `roles.assign` | Yêu cầu `roleId` |
| `revokeRole` | Thu hồi vai trò | `/api/users/:id/roles/:roleId` | DELETE | `roles.revoke` | Kiểm tra tự bảo vệ Admin |

---

### Module `roles` & `permissions` (`backend/src/modules/roles/`, `permissions/`)

#### `roles.service.ts`
| Hàm | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `getRoles()` | Lấy danh sách 7 vai trò hệ thống kèm số quyền và số người dùng | Không | `Promise<RoleSummary[]>` | `roles.view` | Sắp xếp theo thứ tự ưu tiên |
| `getRoleById()` | Lấy chi tiết vai trò và danh sách quyền hạn | `roleId: string` | `Promise<RoleDetail>` | `roles.view` | Ném 404 nếu không tìm thấy |
| `createRole()` | Tạo vai trò mới | `CreateRoleDto, actorId` | `Promise<Role>` | `roles.create` | Kiểm tra trùng lặp code |
| `updateRole()` | Chỉnh sửa thông tin vai trò | `roleId, UpdateRoleDto, actorId` | `Promise<Role>` | `roles.update` | Không cho đổi code vai trò hệ thống |
| `getRolePermissions()` | Lấy danh sách ID quyền được gán cho vai trò | `roleId: string` | `Promise<string[]>` | `roles.view` | Phục vụ trang ma trận phân quyền |
| `updateRolePermissions()` | Cập nhật toàn bộ quyền cho vai trò (Ma trận phân quyền) | `roleId, permissionIds, actorId, ip, userAgent` | `Promise<void>` | `permissions.manage` | Thực hiện trong Database Transaction |

#### `permissions.service.ts`
| Hàm | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `getAllPermissions()` | Lấy tất cả quyền hệ thống, gom nhóm theo module | Không | `Promise<{ all: Permission[], byModule: Record<string, Permission[]> }>` | `permissions.view` | Phục vụ hiển thị ma trận phân quyền |

---

### Module `email` (`backend/src/modules/email/`)

#### `email.provider.ts`
| Interface / Lớp | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `EmailProvider` | Interface trừu tượng định nghĩa hàm gửi email | `options: SendEmailOptions` | `Promise<SendEmailResult>` | Internal | Chuẩn hóa provider |
| `MockEmailProvider` | In nội dung email ra console cho môi trường Local / Dev | `options` | `Promise<SendEmailResult>` | Internal | Kích hoạt khi `EMAIL_PROVIDER=mock` |
| `HttpEmailProvider` | Gửi email qua HTTP REST API (SendGrid, Mailgun, Brevo, SES) | `options` | `Promise<SendEmailResult>` | Internal | Kích hoạt khi `EMAIL_PROVIDER=http` |
| `SmtpEmailProvider` | Gửi email qua máy chủ SMTP Nodemailer | `options` | `Promise<SendEmailResult>` | Internal | Kích hoạt khi `EMAIL_PROVIDER=smtp` |

#### `email.service.ts`
| Hàm | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `queueEmail()` | Đưa thư vào hàng đợi `email_outbox` và kích hoạt gửi bất đồng bộ | `recipient, subject, template, payload` | `Promise<string>` | Internal | Trạng thái ban đầu `PENDING` |
| `processOutboxItem()` | Thực thi gửi thư qua `EmailProvider`, cập nhật `SENT` hoặc `FAILED` | `outboxId: string` | `Promise<void>` | Internal | Tăng `retry_count` và lưu `last_error` nếu thất bại |
| `getOutboxEmails()` | Lấy danh sách thư trong outbox phục vụ kiểm tra | `limit?: number` | `Promise<OutboxItem[]>` | `audit.view` | Hỗ trợ debug email outbox |

#### `email.templates.ts`
| Hàm Template | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `renderAccountActivation()` | Tạo mẫu email cấp tài khoản mới kèm mật khẩu tạm | `data` | `{ subject, html, text }` | Internal | Tiếng Việt, HTML responsive |
| `renderPasswordReset()` | Tạo mẫu email gửi liên kết đặt lại mật khẩu 30 phút | `data` | `{ subject, html, text }` | Internal | Tiếng Việt, chứa link reset |
| `renderPasswordResetSuccess()` | Tạo mẫu email thông báo đặt lại mật khẩu thành công | `data` | `{ subject, html, text }` | Internal | Cảnh báo bảo mật |
| `renderPasswordChangedSuccess()`| Tạo mẫu email thông báo đổi mật khẩu thành công | `data` | `{ subject, html, text }` | Internal | Thông báo đăng xuất toàn bộ phiên |
| `renderAdminTempPassword()` | Tạo mẫu email Admin cấp lại mật khẩu tạm thời | `data` | `{ subject, html, text }` | Internal | Chứa mật khẩu tạm thời |

---

### Module `audit` (`backend/src/modules/audit/`)

#### `audit.service.ts`
| Hàm | Mục đích | Input | Output | Permission | Ghi chú |
|---|---|---|---|---|---|
| `log()` | Ghi nhật ký hành động an ninh và quản trị vào DB | `AuditLogData` | `Promise<void>` | Internal | Tuyệt đối không lưu plaintext password/token/key |
| `getLogs()` | Lấy danh sách nhật ký an ninh phân trang, lọc theo action/user | `GetAuditLogsQuery` | `Promise<PaginatedAuditLogs>` | `audit.view` | Phục vụ trang Nhật ký hệ thống |

---

# PHẦN 2: FRONTEND (`frontend/src`)

## 1. App Core, Context & Router

### `frontend/src/app/context/AuthContext.tsx`
| Hàm / Hook | Mục đích | Input | Output | Quyền UI | Ghi chú |
|---|---|---|---|---|---|
| `useAuth()` | Custom hook truy xuất AuthContext | Không | `AuthContextType` | Mọi component | Ném lỗi nếu dùng ngoài Provider |
| `AuthProvider` | Quản lý state đăng nhập, access token, thông tin user, roles, permissions | `children` | JSX | Toàn app | Lắng nghe event `session_expired` để tự đăng xuất |
| `hasPermission()` | Kiểm tra xem người dùng hiện tại có quyền cụ thể hay không | `permission: string` | `boolean` | Client | ADMIN luôn trả về `true` |
| `hasRole()` | Kiểm tra người dùng có vai trò cụ thể hay không | `role: string` | `boolean` | Client | Hỗ trợ đa vai trò |
| `login()` | Gọi API login, lưu access token, nạp thông tin profile | `credentials` | `Promise<void>` | Công khai | Bắt lỗi trả về `remainingAttempts` |
| `logout()` | Gọi API logout, xóa token, chuyển hướng về `/login` | Không | `Promise<void>` | Đã đăng nhập | Xóa cookie và token state |
| `refreshProfile()` | Gọi lại `/api/auth/me` để cập nhật quyền mới nhất | Không | `Promise<void>` | Đã đăng nhập | Đồng bộ quyền sau khi admin cập nhật |

### `frontend/src/app/router/AppRouter.tsx`
| Thành phần | Mục đích | Đường dẫn | Component | Quyền yêu cầu | Ghi chú |
|---|---|---|---|---|---|
| `AppRouter` | Định nghĩa toàn bộ cây định tuyến (Routes) của ứng dụng | Toàn app | JSX | - | Quản lý công khai/bảo vệ/phân quyền |
| Route `/login` | Trang đăng nhập hệ thống | `/login` | `LoginPage` | Công khai | Tự chuyển `/dashboard` nếu đã login |
| Route `/forgot-password` | Trang yêu cầu đặt lại mật khẩu | `/forgot-password` | `ForgotPasswordPage` | Công khai | - |
| Route `/reset-password` | Trang đặt lại mật khẩu mới với token | `/reset-password` | `ResetPasswordPage` | Công khai | - |
| Route `/dashboard` | Trang tổng quan hệ thống | `/dashboard` | `DashboardPage` | Đã đăng nhập | - |
| Route `/admin/users` | Trang quản lý tài khoản người dùng | `/admin/users` | `UsersPage` | `users.view` | Chặn nếu thiếu quyền |
| Route `/admin/roles` | Trang quản lý vai trò | `/admin/roles` | `RolesPage` | `roles.view` | Chặn nếu thiếu quyền |
| Route `/admin/permission-matrix` | Trang ma trận phân quyền hệ thống | `/admin/permission-matrix` | `PermissionMatrixPage` | `roles.view` hoặc `permissions.view` | Chặn nếu thiếu quyền |
| Route `/admin/role-descriptions` | Trang mô tả nhiệm vụ 7 vai trò | `/admin/role-descriptions` | `RoleDescriptionsPage` | `roles.view` | Chặn nếu thiếu quyền |
| Route `/admin/audit-logs` | Trang nhật ký an ninh hệ thống | `/admin/audit-logs` | `AuditLogsPage` | `audit.view` | Chặn nếu thiếu quyền |
| Route `/change-password` | Trang đổi mật khẩu tài khoản | `/change-password` | `ChangePasswordPage` | Đã đăng nhập | Hỗ trợ chế độ bắt buộc đổi |

---

## 2. Components & Guards

### `frontend/src/components/common/RouteGuards.tsx`
| Component | Mục đích | Điều kiện pass | Hành động khi fail | Ghi chú |
|---|---|---|---|---|
| `ProtectedRoute` | Bảo vệ các route yêu cầu đăng nhập | Đã xác thực (`isAuthenticated`) | Chuyển hướng `/login` kèm `from` state | - |
| `PermissionRoute` | Bảo vệ các route yêu cầu quyền hạn cụ thể | Người dùng có `permission` tương ứng | Chuyển hướng `/403` (Truy cập bị từ chối) | Không chỉ ẩn link mà chặn cả URL trực tiếp |
| `PublicOnlyRoute` | Chặn người dùng đã đăng nhập vào lại trang login/public | Chưa xác thực | Chuyển hướng `/dashboard` | - |

### `frontend/src/components/auth/MustChangePasswordGuard.tsx`
| Component | Mục đích | Điều kiện kiểm tra | Hành động khi kích hoạt | Ghi chú |
|---|---|---|---|---|
| `MustChangePasswordGuard` | Ép người dùng đổi mật khẩu khi có cờ `must_change_password` | `user.mustChangePassword === true` | Chuyển hướng bắt buộc sang `/change-password?required=true` | Cho phép truy cập `/change-password` và đăng xuất |

### `frontend/src/components/layout/AppLayout.tsx`
| Thành phần | Mục đích | Input | Output | Quyền UI | Ghi chú |
|---|---|---|---|---|---|
| `AppLayout` | Khung giao diện chính (Header, Sidebar cố định, Mobile Drawer, Breadcrumb) | `children` | JSX | Đã đăng nhập | Tích hợp menu điều hướng thông minh |
| `getVisibleMenuItems()` | Lọc danh sách menu hiển thị dựa trên quyền hạn thực tế | `permissions, user` | `ItemType[]` | Client | Không hard-code role; kiểm tra `hasPermission()` tập trung |

### `frontend/src/components/email/EmailOutboxDrawer.tsx`
| Component | Mục đích | Input | Output | Quyền UI | Ghi chú |
|---|---|---|---|---|---|
| `EmailOutboxDrawer` | Drawer tiện ích xem nhanh các email trong hàng đợi Outbox (Dev/Test) | `visible, onClose` | JSX | `audit.view` | Hiển thị template, trạng thái, payload |

---

## 3. Pages

### `frontend/src/pages/auth/LoginPage.tsx`
| Hàm / Khối | Mục đích | Input | Output | Quyền UI | Ghi chú |
|---|---|---|---|---|---|
| `handleSubmit()` | Gửi form đăng nhập, hiển thị cảnh báo số lần thử còn lại (`remainingAttempts`) | Form values | `Promise<void>` | Công khai | Sử dụng `Alert` của Ant Design, nhãn tiếng Việt 100% |
| `handleQuickFill()` | Nút hỗ trợ điền nhanh tài khoản demo 7 vai trò bằng tiếng Việt | `roleKey` | `void` | Dev/Demo | Đồng bộ nhãn tiếng Việt |

### `frontend/src/pages/auth/ChangePasswordPage.tsx`
| Hàm / Khối | Mục đích | Input | Output | Quyền UI | Ghi chú |
|---|---|---|---|---|---|
| `handleSubmit()` | Gửi yêu cầu đổi mật khẩu, đăng xuất toàn bộ phiên và điều hướng về `/login` | Form values | `Promise<void>` | Đã đăng nhập | Hiển thị thông báo yêu cầu bắt buộc nếu `required=true` |

### `frontend/src/pages/users/UsersPage.tsx`
| Hàm / Khối | Mục đích | Input | Output | Quyền UI | Ghi chú |
|---|---|---|---|---|---|
| `fetchUsers()` | Nạp danh sách người dùng từ API có phân trang, lọc theo phòng ban và vai trò | Query params | `Promise<void>` | `users.view` | Ant Design Table |
| `handleLockUser()` | Khóa tài khoản người dùng kèm cảnh báo bàn giao yêu cầu tuyển dụng | `userId, reason` | `Promise<void>` | `users.lock` | Modal xác nhận |
| `handleUnlockUser()` | Mở khóa tài khoản người dùng | `userId` | `Promise<void>` | `users.unlock` | Thông báo thành công |
| `handleResetPassword()` | Admin cấp lại mật khẩu tạm thời cho tài khoản, hiển thị modal thông báo kết quả | `userId` | `Promise<void>` | `users.reset-password` | Nút "Cấp lại mật khẩu tạm", icon `KeyOutlined` |
| `handleAssignRoles()` | Cập nhật vai trò cho người dùng qua Drawer | `userId, roleIds` | `Promise<void>` | `roles.assign` | Hỗ trợ đa vai trò |

### `frontend/src/pages/permissions/PermissionMatrixPage.tsx`
| Hàm / Khối | Mục đích | Input | Output | Quyền UI | Ghi chú |
|---|---|---|---|---|---|
| `fetchMatrixData()` | Nạp danh sách quyền theo module và quyền của từng vai trò | Không | `Promise<void>` | `roles.view` / `permissions.view` | Matrix Table đầy đủ |
| `handleTogglePermission()` | Bật/tắt quyền hạn của vai trò trong ma trận | `roleId, permId` | `void` | `permissions.manage` | Đánh dấu trạng thái "Có thay đổi chưa lưu" |
| `handleToggleModule()` | Chọn tất cả / Bỏ chọn tất cả quyền của một module | `roleId, module, checked` | `void` | `permissions.manage` | Thao tác hàng loạt nhanh |
| `handleSaveMatrix()` | Lưu ma trận quyền đã chỉnh sửa lên backend | Không | `Promise<void>` | `permissions.manage` | Gọi `PUT /api/roles/:id/permissions` trong transaction |
| `handleResetChanges()` | Khôi phục lại trạng thái ban đầu khi chưa lưu | Không | `void` | `roles.view` | Hủy các thay đổi tạm thời |

### `frontend/src/pages/roles/RoleDescriptionsPage.tsx`
| Component | Mục đích | Input | Output | Quyền UI | Ghi chú |
|---|---|---|---|---|---|
| `RoleDescriptionsPage` | Hiển thị chi tiết mục tiêu, nhiệm vụ chính, thao tác được phép/không được phép của 7 vai trò | Không | JSX | `roles.view` | Ant Design Tabs / Card, 100% tiếng Việt |

### `frontend/src/pages/roles/RolesPage.tsx`
| Hàm / Khối | Mục đích | Input | Output | Quyền UI | Ghi chú |
|---|---|---|---|---|---|
| `fetchRoles()` | Nạp danh sách 7 vai trò hệ thống, số quyền và số tài khoản | Không | `Promise<void>` | `roles.view` | Cảnh báo vai trò Quản trị hệ thống |
| `handleEditRole()` | Mở modal cập nhật thông tin vai trò | `role` | `void` | `roles.update` | Không cho đổi code vai trò hệ thống |

### `frontend/src/pages/audit/AuditLogsPage.tsx`
| Hàm / Khối | Mục đích | Input | Output | Quyền UI | Ghi chú |
|---|---|---|---|---|---|
| `fetchLogs()` | Nạp danh sách nhật ký an ninh có phân trang, lọc theo hành động | Query params | `Promise<void>` | `audit.view` | Hiển thị mã hành động, mô tả, IP, thời gian |

---

## 4. Services

### `frontend/src/services/api.ts`
| Biến / Hàm | Mục đích | Input | Output | Quyền UI | Ghi chú |
|---|---|---|---|---|---|
| `apiClient` | Axios instance cấu hình `baseURL`, `withCredentials: true` | Request config | AxiosInstance | Toàn frontend | Interceptor tự động gắn `Authorization: Bearer` |
| Response Interceptor | Bắt mã lỗi 401: Phân biệt `SESSION_INVALIDATED` để xóa token và dispatch logout | Axios error | `Promise<rejected>` | Client | Tự động xử lý Refresh Token Rotation mà không loop |

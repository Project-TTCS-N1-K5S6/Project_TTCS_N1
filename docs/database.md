# TÀI LIỆU CƠ SỞ DỮ LIỆU IRMS — SPRINT 1

Hệ quản trị cơ sở dữ liệu: **PostgreSQL 16+**  
Tên Database: **ttcs_db**  
Múi giờ lưu trữ: **UTC / TIMESTAMP WITH TIME ZONE (Hiển thị Asia/Ho_Chi_Minh)**  

---

## 1. DANH SÁCH BẢNG (TABLES)

### 1. `departments` (Phòng ban / Đơn vị tổ chức)
| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Định danh phòng ban |
| `code` | VARCHAR(50) | UNIQUE, NOT NULL | Mã phòng ban (HR, TECH, SALES, MKT) |
| `name` | VARCHAR(255) | NOT NULL | Tên phòng ban |
| `description` | TEXT | NULL | Mô tả chức năng |
| `created_at` | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Thời gian tạo |
| `updated_at` | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Thời gian cập nhật |

### 2. `roles` (Vai trò nghiệp vụ)
| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Định danh vai trò |
| `code` | VARCHAR(50) | UNIQUE, NOT NULL | Mã vai trò (ADMIN, RECRUITER, ...) |
| `name` | VARCHAR(100) | NOT NULL | Tên hiển thị vai trò |
| `description` | TEXT | NULL | Mô tả vai trò |
| `is_system_role`| BOOLEAN | DEFAULT true | Vai trò hệ thống định sẵn |
| `created_at` | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Thời gian tạo |
| `updated_at` | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Thời gian cập nhật |

### 3. `permissions` (Quyền hạn hệ thống)
| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Định danh quyền |
| `code` | VARCHAR(100) | UNIQUE, NOT NULL | Mã quyền dạng MODULE.ACTION |
| `name` | VARCHAR(255) | NOT NULL | Tên hiển thị quyền |
| `module` | VARCHAR(100) | NOT NULL | Tên phân hệ (users, roles, ...) |
| `action` | VARCHAR(50) | NOT NULL | Hành động (view, create, lock, ...) |
| `description` | TEXT | NULL | Mô tả chi tiết quyền hạn |
| `created_at` | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Thời gian tạo |

### 4. `users` (Tài khoản người dùng nội bộ)
| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Định danh người dùng |
| `employee_code` | VARCHAR(50) | UNIQUE | Mã số nhân viên |
| `full_name` | VARCHAR(255) | NOT NULL | Họ và tên |
| `email` | VARCHAR(255) | UNIQUE, NOT NULL | Email công ty |
| `phone` | VARCHAR(50) | NULL | Số điện thoại |
| `job_title` | VARCHAR(100) | NULL | Chức danh công việc |
| `department_id` | UUID | REFERENCES departments(id) | Phòng ban trực thuộc |
| `password_hash` | VARCHAR(255) | NOT NULL | Mật khẩu băm bằng BCrypt |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE, LOCKED, INACTIVE |
| `failed_login_attempts` | INT | DEFAULT 0 | Đếm số lần đăng nhập sai |
| `locked_until` | TIMESTAMPTZ | NULL | Thời điểm hết hạn tạm khóa 15p |
| `locked_at` | TIMESTAMPTZ | NULL | Thời điểm bị Admin khóa thủ công |
| `lock_reason` | TEXT | NULL | Lý do khóa tài khoản |
| `locked_by` | UUID | REFERENCES users(id) | Admin thực hiện khóa |
| `must_change_password`| BOOLEAN | DEFAULT false | Yêu cầu đổi mật khẩu lần đầu |
| `last_login_at` | TIMESTAMPTZ | NULL | Thời điểm đăng nhập gần nhất |
| `created_at` | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Ngày tạo tài khoản |
| `updated_at` | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Ngày cập nhật |
| `created_by` | UUID | REFERENCES users(id) | Người tạo |
| `updated_by` | UUID | REFERENCES users(id) | Người cập nhật |

### 5. `user_roles` (Liên kết N-N Người dùng - Vai trò)
| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `user_id` | UUID | REFERENCES users(id) ON DELETE CASCADE | ID người dùng |
| `role_id` | UUID | REFERENCES roles(id) ON DELETE CASCADE | ID vai trò |
| `created_at` | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Thời điểm gán |
| *Khóa chính:* | (user_id, role_id) | | Không trùng lặp vai trò |

### 6. `role_permissions` (Liên kết N-N Vai trò - Quyền hạn)
| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `role_id` | UUID | REFERENCES roles(id) ON DELETE CASCADE | ID vai trò |
| `permission_id` | UUID | REFERENCES permissions(id) ON DELETE CASCADE | ID quyền |
| `created_at` | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Thời điểm gán |
| *Khóa chính:* | (role_id, permission_id) | | Không trùng lặp quyền |

### 7. `refresh_tokens` (Phiên làm việc & Token xoay vòng)
| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Định danh bản ghi token |
| `user_id` | UUID | REFERENCES users(id) ON DELETE CASCADE | Chủ sở hữu token |
| `token_hash` | VARCHAR(255) | UNIQUE, NOT NULL | Giá trị băm SHA-256 của token |
| `expires_at` | TIMESTAMPTZ | NOT NULL | Thời điểm hết hạn (7 ngày) |
| `revoked_at` | TIMESTAMPTZ | NULL | Thời điểm bị thu hồi |
| `created_at` | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Thời điểm tạo |
| `replaced_by_token_id` | UUID | REFERENCES refresh_tokens(id) | Token xoay vòng thay thế |
| `ip_address` | VARCHAR(45) | NULL | Địa chỉ IP đăng nhập |
| `user_agent` | TEXT | NULL | Trình duyệt / thiết bị |

### 8. `password_reset_tokens` (Token đặt lại mật khẩu)
| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Định danh reset token |
| `user_id` | UUID | REFERENCES users(id) ON DELETE CASCADE | Người yêu cầu |
| `token_hash` | VARCHAR(255) | UNIQUE, NOT NULL | Băm SHA-256 của reset token |
| `expires_at` | TIMESTAMPTZ | NOT NULL | Thời điểm hết hạn (30 phút) |
| `used_at` | TIMESTAMPTZ | NULL | Thời điểm đã sử dụng |
| `created_at` | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Thời điểm tạo |

### 9. `audit_logs` (Nhật ký kiểm toán an ninh)
| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Định danh audit log |
| `user_id` | UUID | REFERENCES users(id) ON DELETE SET NULL | Người thực hiện hành động |
| `action` | VARCHAR(100) | NOT NULL | Hành động (LOGIN_SUCCESS, USER_LOCKED, ...) |
| `entity_type` | VARCHAR(100) | NOT NULL | Loại đối tượng (USER, AUTH, ROLE, ...) |
| `entity_id` | VARCHAR(100) | NULL | ID đối tượng bị tác động |
| `description` | TEXT | NULL | Mô tả chi tiết hành động |
| `ip_address` | VARCHAR(45) | NULL | Địa chỉ IP của client |
| `user_agent` | TEXT | NULL | Thông tin trình duyệt/OS |
| `created_at` | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Thời gian ghi nhận |

### 10. `email_outbox` (Hàng đợi email gửi đi)
| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Định danh thư |
| `recipient` | VARCHAR(255) | NOT NULL | Địa chỉ email người nhận |
| `subject` | VARCHAR(255) | NOT NULL | Tiêu đề thư |
| `template` | VARCHAR(100) | NOT NULL | Loại mẫu thư (ACCOUNT_ACTIVATION, ...) |
| `payload` | JSONB | NOT NULL | Dữ liệu tham số nội dung thư |
| `status` | VARCHAR(20) | DEFAULT 'PENDING' | PENDING, SENT, FAILED |
| `retry_count` | INT | DEFAULT 0 | Số lần thử lại |
| `last_error` | TEXT | NULL | Chi tiết lỗi gửi thư nếu có |
| `created_at` | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Thời điểm xếp hàng đợi |
| `sent_at` | TIMESTAMPTZ | NULL | Thời điểm gửi thành công |

### 11. `recruitment_requisitions` (Yêu cầu tuyển dụng - Phục vụ cảnh báo bàn giao S1-10)
| Cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PRIMARY KEY, DEFAULT gen_random_uuid() | Định danh yêu cầu |
| `code` | VARCHAR(50) | UNIQUE, NOT NULL | Mã yêu cầu (REQ-2026-001) |
| `title` | VARCHAR(255) | NOT NULL | Tiêu đề vị trí tuyển dụng |
| `department_id` | UUID | REFERENCES departments(id) | Phòng ban yêu cầu |
| `recruiter_id` | UUID | REFERENCES users(id) | Chuyên viên tuyển dụng phụ trách |
| `hiring_manager_id`| UUID | REFERENCES users(id) | Trưởng bộ phận tuyển dụng |
| `status` | VARCHAR(50) | NOT NULL, DEFAULT 'OPEN' | OPEN, CLOSED, DRAFT |
| `created_at` | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Ngày tạo |
| `updated_at` | TIMESTAMPTZ | DEFAULT CURRENT_TIMESTAMP | Ngày cập nhật |

# THIẾT KẾ CƠ SỞ DỮ LIỆU IRMS (MYSQL 8.0)

## 1. THÔNG SỐ CƠ BẢN
- **Hệ quản trị CSDL:** MySQL 8.0+
- **Database Name:** `ttcs_db`
- **Charset:** `utf8mb4`
- **Collation:** `utf8mb4_unicode_ci`
- **Động cơ lưu trữ (Engine):** InnoDB (Hỗ trợ Giao dịch ACID và Ràng buộc Khóa ngoại)
- **Tập tin khởi tạo:**
  - `database/schema.sql`: Định nghĩa cấu trúc bảng (DDL), chỉ mục (Indexes) và ràng buộc toàn vẹn.
  - `database/seed.sql`: Nạp dữ liệu mẫu ban đầu (DML): vai trò, 10 phân hệ quyền hạn, phòng ban, người dùng mẫu, ứng viên.

---

## 2. DANH SÁCH BẢNG DỮ LIỆU CHÍNH

### 1. `departments` (Phòng ban / Đơn vị tổ chức)
| Cột | Kiểu | Mô tả |
| :--- | :--- | :--- |
| `id` | VARCHAR(36) PK | Khóa chính (UUID) |
| `code` | VARCHAR(50) UNIQUE | Mã phòng ban (BGD, HR, TECH, SALES...) |
| `name` | VARCHAR(255) | Tên phòng ban đầy đủ |
| `description` | TEXT | Chức năng nhiệm vụ |
| `created_at` | TIMESTAMP | Thời gian tạo |
| `updated_at` | TIMESTAMP | Thời gian cập nhật |

### 2. `roles` (Danh mục vai trò)
| Cột | Kiểu | Mô tả |
| :--- | :--- | :--- |
| `id` | VARCHAR(36) PK | Khóa chính (UUID) |
| `code` | VARCHAR(50) UNIQUE | Mã vai trò (ADMIN, HR_MANAGER, RECRUITER...) |
| `name` | VARCHAR(100) | Tên vai trò hiển thị |
| `description` | TEXT | Mô tả quyền hạn của vai trò |
| `is_system_role`| BOOLEAN | Cờ đánh dấu vai trò mặc định của hệ thống |

### 3. `permissions` (Danh mục quyền hạn 10 phân hệ)
| Cột | Kiểu | Mô tả |
| :--- | :--- | :--- |
| `id` | VARCHAR(36) PK | Khóa chính (UUID) |
| `code` | VARCHAR(100) UNIQUE | Mã quyền (VD: users.view, candidates.manage) |
| `name` | VARCHAR(255) | Tên quyền hiển thị |
| `module` | VARCHAR(100) | Tên phân hệ (users, candidates, interviews...) |
| `action` | VARCHAR(50) | Hành động (view, create, update, manage...) |
| `description` | TEXT | Giải thích chức năng quyền hạn |

### 4. `users` (Tài khoản người dùng)
| Cột | Kiểu | Mô tả |
| :--- | :--- | :--- |
| `id` | VARCHAR(36) PK | Khóa chính (UUID) |
| `employee_code` | VARCHAR(50) UNIQUE | Mã nhân viên (EMP001...) |
| `full_name` | VARCHAR(255) | Họ và tên nhân viên |
| `email` | VARCHAR(255) UNIQUE | Địa chỉ email đăng nhập |
| `phone` | VARCHAR(50) | Số điện thoại liên lạc |
| `job_title` | VARCHAR(100) | Chức danh công việc |
| `department_id` | VARCHAR(36) FK | Phòng ban trực thuộc |
| `password_hash` | VARCHAR(255) | Mật khẩu mã hóa BCrypt |
| `status` | VARCHAR(20) | Trạng thái (ACTIVE, LOCKED, INACTIVE) |
| `failed_login_attempts` | INT | Số lần đăng nhập sai liên tiếp |
| `locked_until` | TIMESTAMP NULL | Thời điểm hết hạn tạm khóa 15 phút |
| `must_change_password` | BOOLEAN | Cờ bắt buộc đổi mật khẩu khi đăng nhập |
| `session_version` | INT | Phiên bản bảo mật để vô hiệu hóa phiên cũ |

### 5. `user_roles` (Quan hệ Người dùng - Vai trò: N-N)
- `user_id` VARCHAR(36) FK
- `role_id` VARCHAR(36) FK
- Khóa chính kết hợp: `(user_id, role_id)`

### 6. `role_permissions` (Quan hệ Vai trò - Quyền hạn: N-N)
- `role_id` VARCHAR(36) FK
- `permission_id` VARCHAR(36) FK
- Khóa chính kết hợp: `(role_id, permission_id)`

### 7. `candidates` (Hồ sơ ứng viên)
| Cột | Kiểu | Mô tả |
| :--- | :--- | :--- |
| `id` | VARCHAR(36) PK | Khóa chính (UUID) |
| `requisition_id`| VARCHAR(36) FK | Yêu cầu tuyển dụng gắn kèm |
| `full_name` | VARCHAR(255) | Họ tên ứng viên |
| `email` | VARCHAR(255) | Email ứng viên |
| `phone` | VARCHAR(50) | Số điện thoại |
| `status` | VARCHAR(50) | Vòng tuyển dụng: APPLIED, SCREENING, INTERVIEWING, OFFER, HIRED, REJECTED |
| `cv_url` | TEXT | Đường dẫn file CV ứng viên |
| `notes` | TEXT | Ghi chú đánh giá |

### 8. `salary_ranges` (Dải lương theo vị trí)
| Cột | Kiểu | Mô tả |
| :--- | :--- | :--- |
| `id` | VARCHAR(36) PK | Khóa chính (UUID) |
| `department_id` | VARCHAR(36) FK | Đơn vị áp dụng |
| `position_title`| VARCHAR(255) | Tên vị trí tuyển dụng |
| `min_salary` | DECIMAL(15, 2) | Mức lương tối thiểu |
| `max_salary` | DECIMAL(15, 2) | Mức lương tối đa |
| `currency` | VARCHAR(10) | Đơn vị tiền tệ (mặc định VND) |

### 9. `audit_logs` (Nhật ký kiểm toán bảo mật)
| Cột | Kiểu | Mô tả |
| :--- | :--- | :--- |
| `id` | VARCHAR(36) PK | Khóa chính (UUID) |
| `user_id` | VARCHAR(36) FK | Người thực hiện thao tác |
| `action` | VARCHAR(100) | Mã hành động (LOGIN_SUCCESS, USER_LOCKED...) |
| `entity_type` | VARCHAR(100) | Đối tượng thao tác |
| `entity_id` | VARCHAR(100) | ID đối tượng |
| `description` | TEXT | Mô tả chi tiết hành động |
| `ip_address` | VARCHAR(45) | Địa chỉ IP của Client |
| `user_agent` | TEXT | Thông tin trình duyệt/thiết bị |
| `created_at` | TIMESTAMP | Thời gian thực hiện |

---

## 3. CÁC TÀI KHOẢN MẪU KHỞI TẠO (SEED ACCOUNTS)
Mật khẩu chung cho tất cả tài khoản mặc định: `Admin@123456`
- **Quản trị hệ thống (Admin):** `admin@company.local` (Sở hữu toàn bộ quyền hạn)
- **Trưởng phòng Nhân sự (HR Manager):** `hr.manager@company.local`
- **Chuyên viên Tuyển dụng (Recruiter):** `recruiter@company.local`
- **Trưởng bộ phận kỹ thuật (Hiring Manager):** `hiring.mgr@company.local`
- **Người phỏng vấn (Interviewer):** `interviewer@company.local`
- **Người phê duyệt (Approver):** `approver@company.local`
- **Tài khoản đang bị khóa (Locked):** `locked.user@company.local`

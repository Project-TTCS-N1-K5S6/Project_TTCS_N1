# HƯỚNG DẪN CƠ SỞ DỮ LIỆU MYSQL 8.0 - IRMS

## 1. THÔNG TIN CHUNG
- **Hệ quản trị CSDL:** MySQL 8.0+
- **Database Name:** `ttcs_db`
- **Charset:** `utf8mb4`
- **Collation:** `utf8mb4_unicode_ci`
- **Storage Engine:** `InnoDB` (Hỗ trợ ACID Transactions và Foreign Key Constraints)

---

## 2. CẤU TRÚC TẬP TIN

```text
database/
├── schema.sql    <- DDL: Lược đồ tạo Database, 13 bảng quan hệ, ràng buộc FK và Index
├── seed.sql      <- DML: Dữ liệu mẫu ban đầu (30 quyền hạn, 7 vai trò, người dùng, dải lương)
└── README.md     <- Tài liệu hướng dẫn cài đặt & lược đồ dữ liệu
```

---

## 3. LƯỢC ĐỒ QUAN HỆ CƠ SỞ DỮ LIỆU (ERD)

```text
[departments] 1 ────< N [users] 1 ────< N [user_roles] >──── 1 [roles]
   │                       │                                     │
   │ 1                     │ 1                                   │ 1
   │                       │                                     │
   v N                     v N                                   v N
[salary_ranges]       [audit_logs]                       [role_permissions]
                           │                                     │
                           │ 1                                   │ N
                           v N                                   v 1
                      [refresh_tokens]                     [permissions]
```

### Chi tiết các bảng:
1. **`departments`**: Cơ cấu tổ chức & phòng ban (`id`, `code`, `name`, `description`, `created_at`, `updated_at`).
2. **`roles`**: Danh mục vai trò (`id`, `code`, `name`, `description`, `is_system_role`).
3. **`permissions`**: 30 quyền hạn thuộc 10 phân hệ nghiệp vụ chuẩn (`id`, `code`, `name`, `module`, `action`, `description`).
4. **`users`**: Tài khoản người dùng nội bộ (`id`, `employee_code`, `full_name`, `email`, `phone`, `job_title`, `department_id`, `password_hash`, `status`, `failed_login_attempts`, `locked_until`, `must_change_password`, `session_version`...).
5. **`user_roles`**: Quan hệ N-N giữa người dùng và vai trò (`user_id`, `role_id`).
6. **`role_permissions`**: Quan hệ N-N giữa vai trò và quyền hạn ma trận phân quyền (`role_id`, `permission_id`).
7. **`refresh_tokens`**: Quản lý phiên token (`id`, `user_id`, `token_hash`, `expires_at`, `revoked_at`...).
8. **`password_reset_tokens`**: Token cấp lại mật khẩu tạm thời (`id`, `user_id`, `token_hash`, `expires_at`, `used_at`).
9. **`audit_logs`**: Nhật ký kiểm toán bảo mật truy vết mọi thao tác (`id`, `user_id`, `action`, `entity_type`, `entity_id`, `description`, `ip_address`, `user_agent`, `created_at`).
10. **`email_outbox`**: Hòm thư lưu vết các email hệ thống gửi đi (`id`, `recipient`, `subject`, `template`, `payload`, `status`).
11. **`recruitment_requisitions`**: Yêu cầu tuyển dụng nhân sự (`id`, `code`, `title`, `department_id`, `recruiter_id`, `hiring_manager_id`, `status`).
12. **`candidates`**: Hồ sơ ứng viên & Pipeline (`id`, `requisition_id`, `full_name`, `email`, `phone`, `status`, `cv_url`, `notes`).
13. **`salary_ranges`**: Dải lương ngân sách theo vị trí (`id`, `department_id`, `position_title`, `min_salary`, `max_salary`, `currency`).

---

## 4. HƯỚNG DẪN KHỞI TẠO CSDL

### Cách 1: Sử dụng Docker Compose (Tự động nạp schema & seed)
Từ thư mục gốc dự án:
```bash
docker compose up -d mysql
```
Container `irms-mysql` sẽ tự động:
1. Tạo database `ttcs_db` với charset `utf8mb4`.
2. Thực thi `schema.sql` để tạo toàn bộ 13 bảng và chỉ mục.
3. Thực thi `seed.sql` để nạp dữ liệu mẫu ban đầu.

### Cách 2: Sử dụng MySQL Server cục bộ (MySQL Workbench / Command Line)
Mở terminal hoặc MySQL Client và chạy:
```sql
CREATE DATABASE IF NOT EXISTS ttcs_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ttcs_db;
SOURCE database/schema.sql;
SOURCE database/seed.sql;
```

---

## 5. TÀI KHOẢN MẪU KHỞI TẠO (SEED DATA)
Mật khẩu mặc định của tất cả các tài khoản: **`Admin@123456`**

| Email | Họ và tên | Vai trò (Role) | Mô tả quyền hạn |
| :--- | :--- | :--- | :--- |
| `admin@company.local` | Nguyễn Quản Trị | `ADMIN` | Toàn quyền quản trị tài khoản, vai trò, ma trận phân quyền và kiểm toán |
| `hr.manager@company.local` | Trần Thị Trưởng Phòng | `HR_MANAGER` | Quản lý quy trình nhân sự, ứng viên, phòng ban, dải lương |
| `recruiter@company.local` | Lê Chuyên Viên | `RECRUITER` | Quản lý pipeline ứng viên (Bị chặn xem dải lương) |
| `hiring.mgr@company.local` | Phạm Trưởng Kỹ Thuật | `HIRING_MANAGER` | Theo dõi ứng viên phỏng vấn chuyên môn |
| `interviewer@company.local`| Hoàng Chuyên Gia | `INTERVIEWER` | Đánh giá phỏng vấn (Bị cấm xem dải lương) |
| `approver@company.local` | Vũ Giám Đốc | `APPROVER` | Phê duyệt yêu cầu và offer |
| `locked.user@company.local`| Đặng Nhân Viên Khóa | `LOCKED` | Tài khoản mẫu kiểm thử trường hợp bị khóa |

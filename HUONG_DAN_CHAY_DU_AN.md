# 🚀 HƯỚNG DẪN CÀI ĐẶT VÀ CHẠY DỰ ÁN TTCS HR SYSTEM

Tài liệu này hướng dẫn chi tiết từng bước để cài đặt, cấu hình cơ sở dữ liệu và khởi chạy ứng dụng **TTCS HR System** (Backend Node.js Express & Frontend Vite/Vanilla JS).

---

## 📋 1. Môi trường cần chuẩn bị

Trước khi khởi chạy dự án, hãy đảm bảo máy tính của bạn đã được cài đặt:
1. **Node.js** (Phiên bản 18.x trở lên). Kiểm tra bằng lệnh: `node -v`
2. **PostgreSQL** (Phiên bản 14.x trở lên). Dịch vụ PostgreSQL đang chạy ở port `5432`.

---

## 🛠️ 2. Cấu hình Cơ sở dữ liệu PostgreSQL

### Bước 2.1: Tạo Database
Mở **pgAdmin** hoặc dòng lệnh **psql** và thực thi câu lệnh SQL:
```sql
CREATE DATABASE ttcs_hr_db;
```

### Bước 2.2: Tạo file cấu hình môi trường `.env`
Trong thư mục `BE/`, tạo một tệp mới tên là `.env` với nội dung mẫu sau:

```env
# Server Config
PORT=5000
NODE_ENV=development

# PostgreSQL Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ttcs_hr_db
DB_USER=postgres
DB_PASSWORD=thay_bang_mat_khau_postgres_cua_ban
DB_SSL=false

# Session & Security
SESSION_SECRET=ttcs_session_super_secret_key_2026_change_in_production_min_32_chars
SESSION_IDLE_TIMEOUT_SECONDS=1800
SESSION_ABSOLUTE_TIMEOUT_SECONDS=28800
SESSION_RENEW_THRESHOLD_SECONDS=600

# CORS
CORS_ORIGIN=http://localhost:3000
```
> ⚠️ **Chú ý:** Thay `thay_bang_mat_khau_postgres_cua_ban` thành mật khẩu thực tế của tài khoản `postgres` trên máy của bạn.

---

## 💻 3. Các bước Khởi chạy Dự án

### ⚠️ Lưu ý quan trọng cho Windows PowerShell
Nếu dùng PowerShell trong VS Code / IDE và gặp lỗi *"running scripts is disabled"*, hãy chạy lệnh này trước trong Terminal:
```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

---

### Bước 3.1: Cài đặt và Chạy Backend (`BE`)

1. Mở cửa sổ Terminal thứ nhất:
   ```powershell
   # Di chuyển vào thư mục BE
   cd BE

   # Cài đặt các thư viện (chỉ cần chạy lần đầu)
   npm install

   # Chạy Migration để tạo các bảng dữ liệu & tài khoản mẫu
   npm run migrate

   # Khởi chạy Backend Server
   npm run dev
   ```

👉 Backend Server sẽ lắng nghe tại: **`http://localhost:5000`**

---

### Bước 3.2: Cài đặt và Chạy Frontend (`FE`)

1. Mở một cửa sở Terminal thứ hai (Tab PowerShell mới):
   ```powershell
   # Mở khóa quyền thi hành script
   Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

   # Di chuyển vào thư mục FE
   cd FE

   # Cài đặt thư viện Frontend
   npm install

   # Khởi chạy máy chủ Vite
   npm run dev
   ```

👉 Mở trình duyệt và truy cập vào đường dẫn được hiển thị trên Terminal Frontend (Thường là **`http://localhost:3000`** hoặc **`http://localhost:5173`**).

---

## 🔑 4. Danh sách Tài khoản Mặc định

Tất cả tài khoản thử nghiệm có mật khẩu mặc định là: **`TempPassword123`**

| Họ và tên | Mã nhân sự | Email | Vai trò | Chức năng thử nghiệm |
| :--- | :--- | :--- | :--- | :--- |
| **Hoàng Tiến Anh** | `NS001` | `hoang.ta@company.com` | `nhan_su` | Chấm phiếu, tự động lưu bản nháp, đổi mật khẩu |
| **Sìn Văn Cương** | `QT001` | `cuong.sv@company.com` | `quan_tri` | Quản trị hệ thống, quản lý danh sách phiên |
| **Nguyễn Thị Mai** | `TP001` | `mai.nt@company.com` | `truong_phong` | Trưởng phòng đánh giá nhân sự |

*(Lưu ý: Tại giao diện `login.html`, có sẵn các nút đăng nhập 1-click điền tự động dữ liệu mẫu).*

---

## ❓ 5. Xử lý các sự cố thường gặp (Troubleshooting)

### ❌ Lỗi 1: `npm : File ... npm.ps1 cannot be loaded because running scripts is disabled`
* **Nguyên nhân:** Windows khóa quyền thực thi script của PowerShell.
* **Cách khắc phục:** Gõ lệnh sau vào Terminal trước khi chạy npm:
  ```powershell
  Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
  ```

### ❌ Lỗi 2: `Error: Cannot find module 'dotenv'` hoặc `MODULE_NOT_FOUND`
* **Nguyên nhân:** Chưa chạy `npm install` bên trong thư mục `BE` hoặc `FE`.
* **Cách khắc phục:** Cần `cd BE` (hoặc `cd FE`) rồi chạy `npm install`.

### ❌ Lỗi 3: `ECONNREFUSED` khi chạy `npm run migrate`
* **Nguyên nhân:** Dịch vụ PostgreSQL chưa khởi động hoặc sai cổng/mật khẩu trong file `.env`.
* **Cách khắc phục:**
  1. Kiểm tra dịch vụ PostgreSQL Service trong `services.msc` đã chọn **Running** chưa.
  2. Kiểm tra lại thông số `DB_USER` và `DB_PASSWORD` trong file `BE/.env`.

# HỆ THỐNG QUẢN LÝ ĐĂNG NHẬP, PHIÊN LÀM VIỆC & CHẤM PHIẾU ĐÁNH GIÁ NHÂN SỰ (TTCS HR)

> **Kiến trúc:** Session-based Authentication (HttpOnly Cookie + PostgreSQL Store) + RESTful API + Vanilla JS/Bootstrap 5 SPA + Auto-Save Drafts (JSONB).

---

## 🎯 Mục Tiêu & Tính Năng Đạt Được

1 **Đăng nhập an toàn:**
   - Hỗ trợ đăng nhập linh hoạt bằng **Email** hoặc **Mã nhân sự** kết hợp mật khẩu.
   - Băm mật khẩu với **bcrypt salt rounds = 12**, chống tấn công từ điển và rainbow table.
   - Phòng chống brute-force bằng **express-rate-limit** (tối đa 10 lần thử/15 phút).
   - Chống Session Fixation bằng **session.regenerate()** ngay sau khi xác thực thành công.

2. **Quản lý Phiên làm việc (Session Management) cấp Enterprise:**
   - **PostgreSQL Session Store (`connect-pg-simple`):** Lưu trữ tập trung, không dùng MemoryStore trong production.
   - **Cookie HttpOnly, SameSite, Secure (Production):** Tuyệt đối không lưu token hay mật khẩu trong `localStorage`.
   - **Idle Timeout (30 phút):** Tự động gia hạn (rolling renewal) khi người dùng còn hoạt động. Không gia hạn vô hạn nếu người dùng vắng mặt.
   - **Absolute Timeout (8 giờ):** Giới hạn phiên tối đa, bắt buộc đăng nhập lại sau 8 tiếng dù có hoạt động liên tục.
   - **Cảnh báo trước khi hết hạn:** Modal đếm ngược (60s) với nút *"Tiếp tục phiên"* để gia hạn chủ động.
   - **Đăng xuất an toàn:** Vô hiệu hóa session ngay lập tức ở server (`is_active = FALSE` & xóa session record).
   - **Đăng xuất tất cả thiết bị (`/api/v1/auth/logout-all`):** Thu hồi toàn bộ session đang mở của tài khoản trên mọi máy tính và thiết bị di động.

3. **Chấm phiếu đánh giá & Tự động lưu bản nháp (Draft Manager):**
   - 6 tiêu chí đánh giá nhân sự: Kỷ luật & Tác phong, Chất lượng công việc, Tiến độ & Hiệu quả, Kỹ năng làm việc nhóm, Tinh thần chủ động, Giao tiếp.
   - Tự động tính tổng điểm (thang 60) và xếp hạng thời gian thực (🏆 Xuất sắc, ⭐ Tốt, ✅ Đạt yêu cầu, ⚠️ Cần cải thiện).
   - **Auto-save định kỳ (30s):** Tự động lưu bản nháp vào PostgreSQL dưới định dạng `JSONB` (`evaluation_drafts`).
   - **Chống mất dữ liệu:** Lưu trữ fallback tại `sessionStorage` khi mạng gián đoạn, tự động khôi phục toàn bộ form khi đăng nhập lại hoặc tải lại trang.

4. **Đổi mật khẩu & Thu hồi phiên (`change-password.html`):**
   - Thanh đo độ mạnh mật khẩu (Password Strength Meter) theo thời gian thực.
   - Kiểm tra 4 điều kiện nghiệp vụ: Tối thiểu 8 ký tự, có chữ cái, có chữ số, khác mật khẩu hiện tại.
   - Tùy chọn thu hồi phiên trên toàn bộ thiết bị khác sau khi đổi mật khẩu thành công.
   - Cập nhật cờ `must_change_pw = FALSE`.

5. **Quản lý phiên đăng nhập trực quan (`sessions.html`):**
   - Danh sách thiết bị, địa chỉ IP, thời gian đăng nhập và hoạt động gần nhất.
   - Phân biệt rõ ràng phiên hiện tại (Current Session) và phiên trên các thiết bị khác.
   - Nút thu hồi phiên từ xa cho từng thiết bị cụ thể.

---

## 🏗️ Kiến Trúc Công Nghệ

### Backend (`/BE`)
- **Runtime:** Node.js (Express.js 5.x)
- **Cơ sở dữ liệu:** PostgreSQL (Driver `pg` + Pool connection)
- **Session Engine:** `express-session` + `connect-pg-simple`
- **Bảo mật:** `helmet`, `cors`, `express-rate-limit`, `bcryptjs`, `uuid`
- **Validation:** `express-validator`

### Frontend (`/FE`)
- **Ngôn ngữ:** HTML5, CSS3, JavaScript ES Modules / Vanilla JS
- **UI Framework:** Bootstrap 5.3 + Custom Luxury Dark Glassmorphism CSS
- **Typography & Icons:** Font Inter (Google Fonts), SVG Icons tối ưu hóa
- **Giao tiếp:** Fetch API (`credentials: 'include'` cho cookie session)
- **Tuân thủ:** Không React/Vue/Angular, không `localStorage` cho token hay mật khẩu.

---

## 📁 Cấu Trúc Thư Mục

```text
Project_TTCS_N1/
├── BE/                               # Backend Node.js Express
│   ├── config/
│   │   └── config.js                 # Cấu hình biến môi trường và timeout
│   ├── controllers/
│   │   ├── authController.js         # Xử lý login, logout, refresh, đổi MK, quản lý session
│   │   └── evaluationController.js   # Xử lý chấm điểm, nộp phiếu, lưu draft JSONB
│   ├── database/
│   │   ├── db.js                     # PostgreSQL connection pool & transaction helper
│   │   ├── migrate.js                # Script chạy migration tự động
│   │   └── migrations/
│   │       └── 001_init.sql          # DDL tạo bảng users, sessions, drafts, forms + Seed data
│   ├── middleware/
│   │   └── authMiddleware.js         # Kiểm tra phiên hợp lệ, idle & absolute timeout
│   ├── models/
│   │   ├── userModel.js              # Truy vấn dữ liệu người dùng
│   │   ├── sessionModel.js           # Truy vấn và quản lý bảng user_sessions
│   │   └── evaluationDraftModel.js   # Quản lý JSONB draft trong evaluation_drafts
│   ├── routes/
│   │   ├── authRoutes.js             # Endpoints /api/v1/auth/*
│   │   └── evaluationRoutes.js       # Endpoints /api/v1/evaluations/*
│   ├── utils/
│   │   ├── generateHash.js           # Tiện ích sinh hash mật khẩu bcrypt cost 12
│   │   └── passwordValidator.js      # Kiểm tra quy tắc độ phức tạp mật khẩu
│   ├── .env.example                  # Mẫu cấu hình môi trường
│   ├── package.json
│   └── server.js                     # Điểm khởi động Express server
│
├── FE/                               # Frontend Single Page / Multi-Page App
│   ├── css/
│   │   ├── style.css                 # Hệ thống design tokens, layout và dark glassmorphism
│   │   └── toast.css                 # Hiệu ứng thông báo Toast
│   ├── js/
│   │   ├── auth.js                   # Module xác thực phiên, guard, fetchWithAuth
│   │   ├── sessionManager.js         # Quản lý idle timeout, cảnh báo countdown, auto-renew
│   │   ├── draftManager.js           # Auto-save draft định kỳ (JSONB) & khôi phục dữ liệu
│   │   └── changePassword.js         # Realtime password validation & strength meter
│   ├── login.html                    # Trang đăng nhập với danh sách tài khoản demo
│   ├── index.html                    # Dashboard tổng quan, thống kê phiên & hành động nhanh
│   ├── evaluation.html               # Trang chấm phiếu đánh giá nhân sự 6 tiêu chí
│   ├── change-password.html          # Trang đổi mật khẩu bảo mật
│   └── sessions.html                 # Trang quản lý các phiên & thiết bị đăng nhập
└── README.md
```

---

## 🚀 Hướng Dẫn Cài Đặt & Chạy Ứng Dụng

### 1. Chuẩn bị Cơ sở dữ liệu PostgreSQL

Tạo database trong PostgreSQL:
```sql
CREATE DATABASE ttcs_hr_db;
```

Cấu hình file `BE/.env` (tham khảo `BE/.env.example`):
```env
PORT=5000
NODE_ENV=development

DB_HOST=localhost
DB_PORT=5432
DB_NAME=ttcs_hr_db
DB_USER=postgres
DB_PASSWORD=your_postgres_password
DB_SSL=false

SESSION_SECRET=ttcs_session_super_secret_key_2026_change_in_production_min_32_chars
SESSION_IDLE_TIMEOUT_SECONDS=1800
SESSION_ABSOLUTE_TIMEOUT_SECONDS=28800
SESSION_RENEW_THRESHOLD_SECONDS=600

CORS_ORIGIN=http://localhost:5000
```

### 2. Cài đặt Dependencies & Chạy Migration

```bash
cd BE
npm install
npm run migrate
```

Lệnh `npm run migrate` sẽ tự động tạo cấu trúc các bảng:
- `users`: Thông tin nhân viên, vai trò, hash mật khẩu bcrypt.
- `user_sessions`: Theo dõi phiên đăng nhập chi tiết, thiết bị, IP, thời gian hết hạn.
- `session`: Bảng lưu session store của `connect-pg-simple`.
- `evaluation_forms`: Dữ liệu phiếu đánh giá sau khi hoàn tất nộp.
- `evaluation_drafts`: Bản nháp tự động lưu dạng JSONB theo từng nhân viên.
- Seed sẵn 3 tài khoản mẫu với mật khẩu chuẩn bcrypt cost 12.

### 3. Khởi Động Server

```bash
npm run dev
# hoặc: npm start
```

Server sẽ phục vụ cả API và static frontend tại:
👉 **`http://localhost:5000/login.html`**

---

## 🔑 Tài Khoản Thử Nghiệm Mặc Định

Tất cả tài khoản mẫu có mật khẩu ban đầu là: `TempPassword123`

| Họ và tên | Mã nhân sự | Email | Vai trò | Chức năng kiểm thử |
| :--- | :--- | :--- | :--- | :--- |
| **Hoàng Tiến Anh** | `NS001` | `hoang.ta@company.com` | `nhan_su` | Chấm phiếu, lưu draft, đổi mật khẩu |
| **Sìn Văn Cương** | `QT001` | `cuong.sv@company.com` | `quan_tri` | Quản trị viên, quản lý nhiều phiên |
| **Nguyễn Thị Mai** | `TP001` | `mai.nt@company.com` | `truong_phong` | Trưởng phòng đánh giá |

*(Trên giao diện `login.html`, có sẵn các nút bấm 1-click điền thông tin nhanh cho các tài khoản trên)*.

---

## 📡 Danh Sách RESTful API Endpoints

### Xác thực & Phiên (`/api/v1/auth`)

| Method | Endpoint | Xác thực | Mô tả |
| :--- | :--- | :---: | :--- |
| `POST` | `/api/v1/auth/login` | ❌ | Đăng nhập (Email/Mã NV + Mật khẩu), tạo session cookie |
| `POST` | `/api/v1/auth/logout` | ❌ | Đăng xuất thiết bị hiện tại, revoke session |
| `POST` | `/api/v1/auth/logout-all` | ✅ | Đăng xuất toàn bộ các thiết bị đang đăng nhập |
| `POST` | `/api/v1/auth/refresh` | ✅ | Gia hạn phiên (Keep-alive) nếu đạt ngưỡng quy định |
| `GET` | `/api/v1/auth/me` | ✅ | Lấy thông tin tài khoản người dùng hiện tại |
| `GET` | `/api/v1/auth/sessions` | ✅ | Lấy danh sách toàn bộ phiên đang hoạt động |
| `DELETE`| `/api/v1/auth/sessions/:id` | ✅ | Thu hồi một phiên cụ thể từ xa |
| `POST` | `/api/v1/auth/change-password` | ✅ | Đổi mật khẩu, tùy chọn thu hồi phiên thiết bị khác |

### Đánh giá & Bản nháp (`/api/v1/evaluations`)

| Method | Endpoint | Xác thực | Mô tả |
| :--- | :--- | :---: | :--- |
| `POST` | `/api/v1/evaluations/drafts/:draftKey` | ✅ | Tự động lưu bản nháp dạng JSONB |
| `GET` | `/api/v1/evaluations/drafts/:draftKey` | ✅ | Lấy dữ liệu bản nháp để khôi phục vào form |
| `GET` | `/api/v1/evaluations/drafts` | ✅ | Lấy danh sách tất cả bản nháp của user |
| `DELETE`| `/api/v1/evaluations/drafts/:draftKey` | ✅ | Xóa bản nháp sau khi đã nộp phiếu |
| `POST` | `/api/v1/evaluations/submit` | ✅ | Nộp phiếu đánh giá hoàn chỉnh vào database |
| `GET` | `/api/v1/evaluations/history` | ✅ | Xem lịch sử các phiếu đã đánh giá |

---

## 🛡️ Cam Kết Bảo Mật (Security Checklist)

- [x] **No localStorage Secrets:** Không lưu password, access token hay session ID trong `localStorage`.
- [x] **HttpOnly Cookie:** Cookie phiên `ttcs.sid` được đặt cờ `HttpOnly` ngăn chặn triệt để tấn công XSS trộm phiên.
- [x] **SameSite Protection:** Giảm thiểu nguy cơ CSRF.
- [x] **Session Fixation Prevention:** Tái tạo lại Session ID (`session.regenerate`) ngay sau khi đăng nhập.
- [x] **Immediate Revocation:** Đăng xuất hoặc đổi mật khẩu sẽ đánh dấu session mất hiệu lực ngay trong DB, chặn đứng các request giả mạo tiếp theo.
- [x] **Bcrypt Salt 12:** Thuật toán băm mật khẩu bảo mật cao.
- [x] **Draft Resilience:** Tự động bảo vệ dữ liệu nhập dở, ngăn chặn mất dữ liệu do rớt mạng hoặc hết hạn phiên đột ngột.

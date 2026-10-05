# MA TRẬN PHÂN QUYỀN HỆ THỐNG IRMS (7 VAI TRÒ)

Quy ước:
- **F (Full):** Toàn quyền (Xem, Thêm, Sửa, Xóa/Khóa)
- **W (Write):** Ghi / Tạo mới / Cập nhật
- **R (Read):** Chỉ xem
- **W\* / R\*:** Chỉ thao tác trên dữ liệu thuộc phạm vi phụ trách của mình
- **-:** Không có quyền truy cập (Bị chặn 403 / Ẩn menu)

---

## 1. BẢNG MA TRẬN PHÂN QUYỀN THEO MODULE

| Phân hệ nghiệp vụ | Candidate | Interviewer | Hiring Manager | Recruiter | Approver | HR Manager | Admin |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Danh mục tổ chức & vị trí** | - | R | R | R | R | F | **F** |
| **Yêu cầu tuyển dụng (Requisitions)** | - | - | W* | W | W* | F | **F** |
| **Tin tuyển dụng (Job Postings)** | R | - | R | W | R | F | **F** |
| **Hồ sơ ứng viên & Pipeline** | R* | R* | R* | F | R | F | **F** |
| **Lịch phỏng vấn (Interviews)** | R* | R* | R* | F | - | F | **F** |
| **Phiếu đánh giá phỏng vấn** | - | W* | R* | R | R | F | **F** |
| **Offer & Onboarding** | R* | - | R* | W | W* | F | **F** |
| **Email & Thông báo** | R* | R* | R* | F | - | F | **F** |
| **Báo cáo & Dashboard** | - | - | R* | R* | R | F | **F** |
| **Quản lý người dùng (Users)** | - | - | - | - | - | R | **F** |
| **Nhật ký hệ thống (Audit Logs)** | - | - | - | - | - | R | **F** |
| **Vai trò & Cấu hình quyền (Roles)** | - | - | - | - | - | R | **F** |

---

## 2. ÁNH XẠ MÃ QUYỀN (PERMISSION CODES) CHI TIẾT

### Module `users`
- `users.view`: Xem danh sách và chi tiết người dùng
- `users.create`: Tạo tài khoản người dùng mới
- `users.update`: Chỉnh sửa thông tin người dùng
- `users.lock`: Khóa tài khoản người dùng
- `users.unlock`: Mở khóa tài khoản người dùng
- `users.reset-password`: Cấp lại mật khẩu tạm thời cho tài khoản (Chỉ dành cho ADMIN)

### Module `roles`
- `roles.view`: Xem danh sách và chi tiết vai trò
- `roles.create`: Tạo vai trò mới
- `roles.update`: Chỉnh sửa thông tin vai trò
- `roles.assign`: Gán vai trò cho người dùng
- `roles.revoke`: Thu hồi vai trò của người dùng

### Module `permissions`
- `permissions.view`: Xem danh sách quyền hệ thống / ma trận phân quyền
- `permissions.manage`: Cấu hình ma trận quyền hạn cho vai trò

### Module `audit`
- `audit.view`: Xem nhật ký an ninh và lịch sử thao tác quản trị

### Module `departments`
- `department.view`: Xem danh mục các phòng ban tổ chức

### Module nền tảng cho Sprint 2 - 8 (Extensible)
- `requisitions.view`: Xem yêu cầu tuyển dụng
- `requisitions.create`: Tạo yêu cầu tuyển dụng
- `requisitions.update`: Cập nhật yêu cầu tuyển dụng
- `requisitions.approve`: Phê duyệt yêu cầu tuyển dụng
- `candidates.view`: Xem ứng viên
- `interviews.view`: Xem lịch phỏng vấn
- `interviews.evaluate`: Đánh giá phỏng vấn
- `salary.view`: Xem dải lương

---

## 3. MA TRẬN BASELINE PERMISSION MAPPING (SEED)

| Mã Quyền | ADMIN | HR_MANAGER | RECRUITER | HIRING_MANAGER | INTERVIEWER | APPROVER | CANDIDATE |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| `users.view` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `users.create` | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `users.update` | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `users.lock` | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `users.unlock` | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `users.reset-password` | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `roles.view` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `roles.create` | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `roles.update` | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `roles.assign` | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `roles.revoke` | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `permissions.view` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `permissions.manage` | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `audit.view` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| `department.view` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| `requisitions.view` | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ | ❌ |
| `requisitions.create` | ✅ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ |
| `requisitions.update` | ✅ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ |
| `requisitions.approve` | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ | ❌ |
| `candidates.view` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| `interviews.view` | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ |
| `interviews.evaluate` | ✅ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ |
| `salary.view` | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Tổng số quyền** | **23** | **10** | **6** | **5** | **4** | **4** | **0** |

---

## 4. NGUYÊN TẮC BẢO MẬT & THI KỶ
1. **Deny By Default:** Mọi endpoint đều mặc định từ chối truy cập nếu người dùng không có permission tương ứng trong database.
2. **Ẩn menu ở frontend chỉ phục vụ UX:** Backend luôn là chốt chặn cuối cùng kiểm tra quyền hạn thực tế.
3. **Quyền hạn quản trị tối cao:** Admin không thể tự tước đoạt quyền ADMIN của chính mình.
4. **Quyền hiệu lực:** Khi người dùng có nhiều vai trò, quyền hiệu lực = Union toàn bộ quyền của các vai trò (không trùng lặp).
5. **Hiệu lực tức thì:** Khi Admin cập nhật vai trò hoặc phân quyền, quyền mới có hiệu lực ở request tiếp theo do backend query DB trực tiếp.

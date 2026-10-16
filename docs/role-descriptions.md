# MÔ TẢ NHIỆM VỤ & QUYỀN HẠN 7 VAI TRÒ HỆ THỐNG IRMS

Tài liệu chi tiết về mục tiêu, nhiệm vụ chính, phạm vi chức năng, quyền hạn được cấp và các quy tắc nghiệp vụ cho 7 vai trò trong Hệ thống Tuyển dụng Nội bộ IRMS.

---

## 1. QUẢN TRỊ HỆ THỐNG (ADMIN)
- **Mã vai trò:** `ADMIN`
- **Tên hiển thị:** Quản trị hệ thống
- **Mục tiêu của vai trò:** Vận hành toàn bộ nền tảng, thiết lập cấu hình bảo mật, quản trị người dùng, phân quyền truy cập và kiểm tra nhật ký hệ thống.
- **Nhiệm vụ chính:**
  1. Quản lý toàn bộ danh sách tài khoản nhân sự trong tổ chức.
  2. Khóa/mở khóa tài khoản nhân sự có cảnh báo bàn giao nghiệp vụ.
  3. Cấp lại mật khẩu tạm thời cho người dùng khi được yêu cầu.
  4. Quản lý danh sách vai trò và cấu hình ma trận phân quyền hệ thống.
  5. Giám sát an ninh qua nhật ký hệ thống (Audit Logs).
  6. Khởi tạo và thiết lập các danh mục dùng chung (Phòng ban, Vị trí tuyển dụng).
- **Phạm vi chức năng:** Toàn quyền trên tất cả các module (`users`, `roles`, `permissions`, `audit`, `department`, `requisitions`, `candidates`, `interviews`, `salary`).
- **Các quyền (Permissions) được cấp:** Toàn bộ 23 quyền hệ thống (bao gồm quyền đặc biệt `users.reset-password` và `permissions.manage`).
- **Thao tác được phép:** Tạo/Sửa/Khóa/Mở khóa người dùng; Gán/Thu hồi vai trò; Chỉnh sửa ma trận quyền; Cấp mật khẩu tạm thời; Xem Audit logs.
- **Thao tác KHÔNG được phép:** Tự thu hồi vai trò ADMIN của chính mình; Tự hạ quyền quản trị khi chưa bàn giao.
- **Ghi chú nghiệp vụ:** Đây là vai trò quản trị hệ thống cao nhất, mặc định có toàn quyền.

---

## 2. TRƯỞNG PHÒNG NHÂN SỰ (HR_MANAGER)
- **Mã vai trò:** `HR_MANAGER`
- **Tên hiển thị:** Trưởng phòng Nhân sự
- **Mục tiêu của vai trò:** Lãnh đạo toàn diện công tác nhân sự, hoạch định tuyển dụng, phê duyệt các yêu cầu tuyển dụng và giám sát tiến độ tuyển dụng toàn công ty.
- **Nhiệm vụ chính:**
  1. Theo dõi tổng quan nhân sự và danh sách tài khoản nhân viên.
  2. Xem cơ cấu phòng ban và vai trò hiện hành.
  3. Xem ma trận phân quyền để đối chiếu thẩm quyền nhân sự.
  4. Xem xét và phê duyệt các yêu cầu tuyển dụng từ các phòng ban.
  5. Giám sát tiến độ phỏng vấn, hồ sơ ứng viên và dải lương thị trường/nội bộ.
  6. Theo dõi nhật ký an ninh hệ thống.
- **Phạm vi chức năng:** Module xem thông tin (`users.view`, `roles.view`, `permissions.view`, `audit.view`, `department.view`), phê duyệt yêu cầu tuyển dụng (`requisitions.view`, `requisitions.approve`), giám sát ứng viên & dải lương (`candidates.view`, `interviews.view`, `salary.view`).
- **Các quyền (Permissions) được cấp:** 10 quyền baseline:
  - `users.view`, `roles.view`, `permissions.view`, `audit.view`, `department.view`
  - `requisitions.view`, `requisitions.approve`, `candidates.view`, `interviews.view`, `salary.view`
- **Thao tác được phép:** Xem danh sách nhân sự; Phê duyệt yêu cầu tuyển dụng; Xem ứng viên, lịch phỏng vấn và dải lương; Xem nhật ký audit.
- **Thao tác KHÔNG được phép:** Chỉnh sửa ma trận quyền; Cấp lại mật khẩu tạm; Khóa/Mở khóa tài khoản người dùng; Tạo mới người dùng trực tiếp.
- **Ghi chú nghiệp vụ:** Không can thiệp vào cấu hình kỹ thuật của hệ thống.

---

## 3. CHUYÊN VIÊN TUYỂN DỤNG (RECRUITER)
- **Mã vai trò:** `RECRUITER`
- **Tên hiển thị:** Chuyên viên tuyển dụng
- **Mục tiêu của vai trò:** Trực tiếp thực thi quy trình tìm kiếm, sàng lọc hồ sơ, điều phối lịch phỏng vấn và quản lý ứng viên.
- **Nhiệm vụ chính:**
  1. Khởi tạo và cập nhật các yêu cầu tuyển dụng (Job Requisitions).
  2. Đăng tin tuyển dụng và tìm nguồn ứng viên phù hợp.
  3. Tiếp nhận hồ sơ, sàng lọc CV và cập nhật trạng thái ứng viên.
  4. Sắp xếp lịch phỏng vấn giữa ứng viên và người phỏng vấn.
  5. Gửi thư mời phỏng vấn và thông báo kết quả cho ứng viên.
- **Phạm vi chức năng:** Các phân hệ tuyển dụng và ứng viên (`department.view`, `requisitions.view`, `requisitions.create`, `requisitions.update`, `candidates.view`, `interviews.view`).
- **Các quyền (Permissions) được cấp:** 6 quyền baseline:
  - `department.view`, `requisitions.view`, `requisitions.create`, `requisitions.update`, `candidates.view`, `interviews.view`
- **Thao tác được phép:** Tạo/chỉnh sửa yêu cầu tuyển dụng; Xem/cập nhật hồ sơ ứng viên; Điều phối lịch phỏng vấn.
- **Thao tác KHÔNG được phép:** Phê duyệt yêu cầu tuyển dụng; Xem dải lương bảo mật; Quản trị người dùng hoặc vai trò.
- **Ghi chú nghiệp vụ:** Phải phối hợp chặt chẽ với Hiring Manager và Interviewer trong suốt pipeline.

---

## 4. TRƯỞNG BỘ PHẬN TUYỂN DỤNG (HIRING_MANAGER)
- **Mã vai trò:** `HIRING_MANAGER`
- **Tên hiển thị:** Trưởng bộ phận tuyển dụng
- **Mục tiêu của vai trò:** Đại diện cho đơn vị chuyên môn cần tuyển thêm nhân sự, xác định yêu cầu công việc và đánh giá chuyên môn của ứng viên.
- **Nhiệm vụ chính:**
  1. Tạo yêu cầu bổ sung nhân sự cho phòng ban của mình.
  2. Xác định tiêu chuẩn tuyển chọn, JD và tiêu chí đánh giá năng lực.
  3. Theo dõi tiến độ tiếp nhận hồ sơ ứng viên cho các vị trí trực thuộc.
  4. Tham gia phỏng vấn các vòng chuyên môn hoặc quản lý.
- **Phạm vi chức năng:** Quản lý yêu cầu tuyển dụng cấp phòng ban (`department.view`, `requisitions.view`, `requisitions.create`, `candidates.view`, `interviews.view`).
- **Các quyền (Permissions) được cấp:** 5 quyền baseline:
  - `department.view`, `requisitions.view`, `requisitions.create`, `candidates.view`, `interviews.view`
- **Thao tác được phép:** Tạo yêu cầu tuyển dụng mới; Xem ứng viên ứng tuyển vào vị trí phụ trách; Xem lịch phỏng vấn.
- **Thao tác KHÔNG được phép:** Phê duyệt yêu cầu tuyển dụng; Chỉnh sửa thông tin tài khoản người dùng; Quản trị hệ thống.
- **Ghi chú nghiệp vụ:** Chỉ thao tác trên các yêu cầu và ứng viên thuộc thẩm quyền bộ phận mình.

---

## 5. NGƯỜI PHỎNG VẤN (INTERVIEWER)
- **Mã vai trò:** `INTERVIEWER`
- **Tên hiển thị:** Người phỏng vấn
- **Mục tiêu của vai trò:** Đánh giá chuyên môn, kỹ năng và mức độ phù hợp văn hóa của ứng viên trong các buổi phỏng vấn.
- **Nhiệm vụ chính:**
  1. Xem lịch phỏng vấn được phân công.
  2. Xem thông tin CV và hồ sơ kinh nghiệm của ứng viên cần phỏng vấn.
  3. Điền phiếu đánh giá và cho điểm phỏng vấn sau mỗi buổi gặp.
  4. Đề xuất quyết định tuyển dụng/từ chối dựa trên tiêu chuẩn kỹ thuật.
- **Phạm vi chức năng:** Phỏng vấn và đánh giá (`department.view`, `interviews.view`, `interviews.evaluate`, `candidates.view`).
- **Các quyền (Permissions) được cấp:** 4 quyền baseline:
  - `department.view`, `interviews.view`, `interviews.evaluate`, `candidates.view`
- **Thao tác được phép:** Xem lịch phỏng vấn được gán; Xem CV ứng viên liên quan; Điền phiếu đánh giá phỏng vấn.
- **Thao tác KHÔNG được phép:** Tạo/sửa yêu cầu tuyển dụng; Phê duyệt offer; Xem dải lương ứng viên; Quản trị người dùng.
- **Ghi chú nghiệp vụ:** Không được truy cập các module quản trị hoặc dữ liệu tài chính/lương của ứng viên.

---

## 6. NGƯỜI PHÊ DUYỆT (APPROVER)
- **Mã vai trò:** `APPROVER`
- **Tên hiển thị:** Người phê duyệt
- **Mục tiêu của vai trò:** Đại diện ban giám đốc/lãnh đạo khối xem xét và quyết định thông qua ngân sách, định biên và yêu cầu tuyển dụng.
- **Nhiệm vụ chính:**
  1. Xem xét các yêu cầu tuyển dụng được gửi lên chờ duyệt.
  2. Đánh giá tính cấp thiết, sự phù hợp với ngân sách và chiến lược công ty.
  3. Ra quyết định phê duyệt (Approve) hoặc từ chối (Reject) kèm lý do.
  4. Xem thông tin hồ sơ ứng viên ở vòng duyệt cuối.
- **Phạm vi chức năng:** Xem xét và phê duyệt (`department.view`, `requisitions.view`, `requisitions.approve`, `candidates.view`).
- **Các quyền (Permissions) được cấp:** 4 quyền baseline:
  - `department.view`, `requisitions.view`, `requisitions.approve`, `candidates.view`
- **Thao tác được phép:** Xem yêu cầu tuyển dụng; Phê duyệt hoặc từ chối yêu cầu; Xem hồ sơ ứng viên liên quan.
- **Thao tác KHÔNG được phép:** Trực tiếp tạo mới/sửa yêu cầu tuyển dụng; Can thiệp kỹ thuật hệ thống; Quản lý tài khoản.
- **Ghi chú nghiệp vụ:** Quyết định phê duyệt là căn cứ chính thức để mở tuyển dụng công khai.

---

## 7. ỨNG VIÊN (CANDIDATE)
- **Mã vai trò:** `CANDIDATE`
- **Tên hiển thị:** Ứng viên
- **Mục tiêu của vai trò:** Ứng viên bên ngoài hoặc nội bộ tham gia nộp hồ sơ, theo dõi trạng thái ứng tuyển và tham gia phỏng vấn.
- **Nhiệm vụ chính:**
  1. Tạo và cập nhật hồ sơ cá nhân, CV.
  2. Nộp đơn vào các vị trí tuyển dụng đang mở.
  3. Theo dõi tiến trình tuyển dụng và phản hồi thư mời phỏng vấn.
  4. Xem offer tuyển dụng và gửi phản hồi.
- **Phạm vi chức năng:** Cổng thông tin ứng viên (Portal Ứng viên - Sprint tiếp theo).
- **Các quyền (Permissions) được cấp:** 0 quyền quản trị nội bộ trong Sprint 1 (`[]`).
- **Thao tác được phép:** Đăng nhập hệ thống; Tự đổi mật khẩu; Xem trang cá nhân.
- **Thao tác KHÔNG được phép:** Mọi hành động quản trị hệ thống, phòng ban, phân quyền, người dùng, audit logs. Truy cập các API nội bộ sẽ nhận mã lỗi 403 Forbidden.
- **Ghi chú nghiệp vụ:** Tuân thủ nguyên tắc Deny By Default chặt chẽ.

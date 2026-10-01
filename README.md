# HỆ THỐNG TUYỂN DỤNG NỘI BỘ (ATS - APPLICANT TRACKING SYSTEM)
### Dự án thực tập · Nhóm 2 · Thời lượng 8 tuần · Tổng Story Points: 350 PT (76 User Stories)

[![Sprint 1 Status](https://img.shields.io/badge/Sprint%201-100%25%20Completed-success.svg)](docs/sprint-1.md)
[![Tests Passing](https://img.shields.io/badge/Integration%20Tests-29%2F29%20Passed-brightgreen.svg)](backend/tests/api.test.ts)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.0-blue.svg)](https://www.typescriptlang.org/)
[![React](https://img.shields.io/badge/React-18-61dafb.svg)](https://react.dev/)
[![Node.js](https://img.shields.io/badge/Node.js-24-green.svg)](https://nodejs.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791.svg)](https://www.postgresql.org/)

---

## 📌 1. Tổng quan Dự án (Product Overview)

### 1.1. Bối cảnh & Vấn đề Nghiệp vụ
Bộ phận nhân sự hiện đang quản lý quy trình tuyển dụng thủ công qua Excel, Gmail và Google Drive. Vấn đề này dẫn tới các hệ quả:
- **Thiếu minh bạch tiến độ:** Không ai biết một vị trí đang tắc ở đâu; phải lội hộp thư để tổng hợp báo cáo.
- **Thiếu kiểm soát ngân sách:** Duyệt headcount qua chat/email, không lưu vết phê duyệt và mức lương, gây lệch ngân sách cuối năm.
- **Lãng phí ứng viên tiềm năng:** Bỏ quên ứng viên giỏi từng trượt vòng cuối khi mở lại vị trí tương tự sau 6 tháng.
- **Nhận xét phỏng vấn thiếu tiêu chí:** Đánh giá dựa trên cảm tính cá nhân, thiếu khung năng lực thống nhất.
- **Thương hiệu tuyển dụng bị ảnh hưởng:** Ứng viên trượt không nhận được phản hồi tự động.

### 1.2. Tầm nhìn Sản phẩm (Product Vision)
- **Dành cho:** Bộ phận nhân sự và các trưởng bộ phận đang tuyển người bằng Excel, Gmail và Google Drive.
- **ATS là gì:** Hệ thống quản lý tuyển dụng nội bộ trên nền web (Internal Recruitment Management System).
- **Giá trị cốt lõi:** Giúp theo dõi trọn vòng đời tuyển dụng trên một nguồn dữ liệu duy nhất, từ yêu cầu headcount tới ngày nhận việc.
- **Điểm khác biệt:** Mọi quyết định — duyệt headcount, loại ứng viên, chốt offer — đều có dấu vết, có tiêu chí và có thể truy vết kiểm toán toàn diện (audit trail).

### 1.3. Thông số Dự án
- **Thời gian thực hiện:** 8 tuần (2 tháng) — 8 Sprints × 1 tuần/sprint
- **Đội ngũ:** 5 lập trình viên Fullstack full-time (40 giờ/tuần/người)
- **Quy đổi Effort:** 1 point ≈ 4 giờ công công nghệ (tương đương ~170 giờ hữu ích/tuần sau khi trừ họp và code review)
- **Velocity mục tiêu:** 42 – 45 points / sprint
- **Tổng quy mô:** 350 Story Points | 76 User Stories | 9 Epics

### 1.4. Mục tiêu & Thước đo Thành công (KPIs)

| Mục tiêu | Thước đo thành công | Phương thức đo lường |
| :--- | :--- | :--- |
| **Chạy trọn 1 vị trí pilot** | 1 vị trí đi hết chu trình: Yêu cầu → Duyệt → Đăng tin → Ứng tuyển → Phỏng vấn → Offer → Onboarding | Demo nghiệm thu cuối Sprint 8 |
| **Rút ngắn thời gian sàng lọc** | Recruiter xử lý 20 CV mới trong $\le$ 15 phút nhờ pipeline & nhãn sàng lọc | Bấm giờ thực tế trên môi trường Staging |
| **Minh bạch 100% Headcount** | 100% yêu cầu tuyển dụng trong dữ liệu pilot có lịch sử duyệt đầy đủ | Đối soát nhật ký hệ thống (Audit log) |
| **Quyết định tuyển chuẩn hóa** | Mỗi ứng viên vào vòng cuối có $\ge$ 2 phiếu đánh giá theo khung năng lực | Kiểm thử nghiệp vụ |
| **Bàn giao sản phẩm chất lượng** | Deploy Staging tự động, $\ge$ 60% coverage tầng service, 0 lỗi Critical | Báo cáo kiểm thử tự động CI/CD |

### 1.5. Phạm vi Dự án (Scope)

| IN-SCOPE (Có làm) | OUT-OF-SCOPE (Không làm) |
| :--- | :--- |
| • Xác thực, phân quyền theo vai trò (RBAC) & Audit Log | • Bóc tách CV bằng AI / Matching ngữ nghĩa JD |
| • Sơ đồ tổ chức, danh mục chức danh, khung năng lực | • Tự động đăng tin sang TopCV, VietnamWorks, LinkedIn |
| • Yêu cầu tuyển dụng & Luồng phê duyệt nhiều cấp | • Đồng bộ 2 chiều Google Calendar / Outlook Calendar |
| • Quản lý ngân sách Headcount theo phòng ban | • Phỏng vấn video trực tuyến tích hợp trong ứng dụng |
| • Soạn & xuất bản tin tuyển dụng, Cổng ứng tuyển web | • Bài kiểm tra năng lực/trắc nghiệm trực tuyến tự động |
| • Nộp CV, tra cứu trạng thái, Giới thiệu nội bộ | • Ký số hợp đồng lao động |
| • Hồ sơ hợp nhất, gộp trùng, Kho ứng viên tiềm năng | • Quản lý nhân sự sau onboarding (chấm công, lương, KPI) |
| • Pipeline tuyển dụng Kanban theo giai đoạn | • Ứng dụng di động Native App (iOS/Android) |
| • Đặt lịch phỏng vấn, chống trùng lịch, thư mời | |
| • Phiếu đánh giá khung năng lực & So sánh ứng viên | |
| • Đề xuất Offer, duyệt hạn mức, Checklist Onboarding | |
| • Email tự động, thông báo in-app, Dashboard & Báo cáo | |

---

## 🛠 2. Kiến trúc & Nền tảng Kỹ thuật (Technical Architecture)

### 2.1. Tech Stack
- **Frontend:** React 18 + TypeScript, Ant Design v5, Vite (Tối ưu hóa tiếng Việt, responsive hoàn toàn trên màn hình từ 360px).
- **Backend:** Node.js + Express (Clean Layered Architecture), TypeScript, Zod Validation, Helmet, Swagger OpenAPI.
- **Database:** PostgreSQL (Đảm bảo ràng buộc toàn vẹn dữ liệu cho luồng duyệt, khóa ngoại và audit logs).
- **Authentication:** JWT Access Token (15 phút) + HttpOnly Refresh Token (7 ngày, mã hóa hash SHA-256 lưu DB).
- **File Storage:** Dịch vụ Object Storage (Lưu trữ CV/Tài liệu, trừu tượng hóa qua Service Interface).
- **Email Service:** SMTP nội bộ + Outbox Queue (Gửi mail bất đồng bộ, tự động retry, theo dõi trực quan trên UI).

### 2.2. Yêu cầu Phi chức năng (Non-Functional Requirements)
- **Hiệu năng:** Kết quả danh sách ứng viên trả về $< 1.5$ giây với $20.000$ hồ sơ và $200$ vị trí. Dashboard tải hoàn tất $< 2$ giây.
- **Quy mô:** Hỗ trợ 400 người dùng nội bộ; Cổng ứng tuyển công khai chịu được 100 lượt nộp CV/giờ.
- **Bảo vệ dữ liệu (Data Privacy):** Hồ sơ ứng viên chỉ hiển thị cho Recruiter phụ trách và Hiring Manager của vị trí đó. Mọi truy cập được ghi log. Hỗ trợ xóa hồ sơ theo yêu cầu riêng tư.
- **Bảo mật:** Mật khẩu băm bằng BCrypt (Salt rounds = 10). Tích hợp Rate-limiting và Anti-spam tại cổng ứng tuyển. Bắt buộc kiểm tra phân quyền ở tầng Server (Server-side RBAC) cho mọi Endpoint với nguyên tắc **Deny by Default**.
- **Localization:** Giao diện tiếng Việt 100%, Múi giờ `Asia/Ho_Chi_Minh`, Đơn vị tiền tệ VND.
- **Sao lưu (Backup):** Sao lưu Database và tệp CV hằng ngày, lưu trữ 7 bản gần nhất.

---

## 👤 3. Mô hình Vai trò & Ma trận Phân quyền (User Roles & RBAC)

### 3.1. 7 Vai trò Người dùng
1. **Ứng viên (Candidate):** Người nộp hồ sơ từ bên ngoài, không có tài khoản nội bộ.
2. **Nhân viên tuyển dụng (Recruiter):** Người vận hành tuyển dụng hằng ngày (Sàng lọc, điều phối pipeline, đặt lịch, soạn offer).
3. **Trưởng bộ phận (Hiring Manager):** Người có nhu cầu tuyển dụng (Tạo yêu cầu, xem CV vị trí thuộc mình, quyết định tuyển).
4. **Người phỏng vấn (Interviewer):** Nhân sự tham gia hội đồng phỏng vấn (Xem lịch, xem CV, chấm phiếu đánh giá).
5. **Trưởng phòng Nhân sự (HR Manager):** Chủ sở hữu toàn bộ hoạt động tuyển dụng, phân công Recruiter, theo dõi ngân sách headcount và báo cáo.
6. **Người duyệt (Approver):** Ban giám đốc hoặc cấp duyệt theo hạn mức lương/headcount.
7. **Quản trị hệ thống (Admin):** Vận hành ứng dụng, quản lý tài khoản, phân quyền, danh mục và xem audit log.

### 3.2. Ma trận Phân quyền (Access Control Matrix)
*Ký hiệu:* **F** = Toàn quyền | **W** = Ghi/Sửa trong phạm vi phụ trách | **R** = Chỉ xem | **–** = Không truy cập  
*\* Ràng buộc kiểm tra nghiêm ngặt tại Server-side.*

| Phân hệ nghiệp vụ | Candidate | Interviewer | Hiring Manager | Recruiter | Approver | HR Manager | Admin |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Danh mục Tổ chức & Vị trí** | – | R | R | R | R | F | **F** |
| **Yêu cầu Tuyển dụng** | – | – | W* | W | W* | F | **F** |
| **Tin Tuyển dụng** | R | – | R | W | R | F | **F** |
| **Hồ sơ Ứng viên & Pipeline** | R* | R* | R* | F | R | F | **F** |
| **Lịch Phỏng vấn** | R* | R* | R* | F | – | F | **F** |
| **Phiếu Đánh giá** | – | W* | R* | R | R | F | **F** |
| **Offer & Onboarding** | R* | – | R* | W | W* | F | **F** |
| **Email & Thông báo** | R* | R* | R* | F | – | F | **F** |
| **Báo cáo & Dashboard** | – | – | R* | R* | R | F | **F** |
| **Người dùng & Audit Log** | – | – | – | – | – | R | **F** |

---

## 📦 4. Danh sách Epic (Epic Overview)

| Mã Epic | Tên Epic | Mục tiêu & Phạm vi | Points | % Tổng | Sprint | Số Story |
| :--- | :--- | :--- | :---: | :---: | :---: | :---: |
| **EP-01** | Tài khoản, Phân quyền & Hồ sơ | Đăng nhập, khôi phục mật khẩu, RBAC, quản trị người dùng, hồ sơ cá nhân. | 52 | 14.9% | 1–2 | 13 |
| **EP-02** | Danh mục Tổ chức & Vị trí | Sơ đồ phòng ban, chức danh, dải lương, khung năng lực, ngân hàng câu hỏi. | 25 | 7.1% | 2 | 5 |
| **EP-03** | Yêu cầu tuyển dụng & Duyệt | Requisition, luồng duyệt đa cấp, ngân sách headcount, phân công Recruiter. | 44 | 12.6% | 2–3 | 9 |
| **EP-04** | Đăng tin & Cổng ứng tuyển | Xuất bản tin tuyển dụng, portal công khai, nộp CV, tra cứu, referral nội bộ. | 47 | 13.4% | 2–4 | 11 |
| **EP-05** | Hồ sơ ứng viên & Pipeline | Hồ sơ hợp nhất, gộp trùng, Kanban pipeline, sàng lọc, kho ứng viên tiềm năng. | 53 | 15.1% | 4–6 | 11 |
| **EP-06** | Phỏng vấn & Đánh giá | Đặt lịch, chống trùng lịch, thư mời, phiếu đánh giá khung năng lực, so sánh. | 51 | 14.6% | 6–7 | 11 |
| **EP-07** | Offer & Onboarding | Đề xuất offer, duyệt hạn mức lương, thư nhận việc, checklist onboarding. | 28 | 8.0% | 7 | 5 |
| **EP-08** | Thông báo & Email tự động | Mẫu email, thư từ chối hàng loạt, thông báo in-app, cảnh báo SLA. | 22 | 6.3% | 5, 7, 8 | 6 |
| **EP-09** | Báo cáo & Dashboard | Dashboard tổng quan, báo cáo phễu, Time-to-hire, Cost-per-hire, hiệu quả nguồn. | 28 | 8.0% | 8 | 5 |
| **TỔNG** | | | **350** | **100%** | **1–8** | **76** |

---

## 🗓 5. Lộ trình Sprint & Product Backlog Chi tiết

### 5.1. Tổng quan Kế hoạch 8 Sprints

| Sprint | Chủ đề Sprint | Mục tiêu Demo cuối Sprint | Story | Point | Lũy kế | Còn lại |
| :---: | :--- | :--- | :---: | :---: | :---: | :---: |
| **1** | Tài khoản & Phân quyền | 7 vai trò đăng nhập thành công và chỉ thấy đúng menu thuộc quyền. *(ĐÃ HOÀN THÀNH)* | 10 | 42 | 42 | 308 |
| **2** | Danh mục & Yêu cầu TD | Sơ đồ tổ chức, khung năng lực và khởi tạo Yêu cầu tuyển dụng đầu tiên. | 10 | 45 | 87 | 263 |
| **3** | Phê duyệt & Đăng tin | 1 Headcount đi trọn luồng duyệt đa cấp và xuất bản thành tin tuyển dụng. | 10 | 44 | 131 | 219 |
| **4** | Cổng ứng tuyển & CV | Ứng viên nộp CV thành công trên di động, tra cứu được trạng thái. | 9 | 45 | 176 | 174 |
| **5** | Pipeline & Sàng lọc | Recruiter kéo-thả điều phối 20 ứng viên trên bảng Kanban. | 10 | 45 | 221 | 129 |
| **6** | Phỏng vấn & Đánh giá | Đặt lịch phỏng vấn không trùng, gửi mail mời và nộp phiếu đánh giá. | 9 | 43 | 264 | 86 |
| **7** | Offer & Onboarding | 1 Offer được duyệt hạn mức, gửi đi, ứng viên chấp nhận và sinh checklist. | 9 | 42 | 306 | 44 |
| **8** | Email & Báo cáo | Dashboard tuyển dụng, báo cáo phễu chuyển đổi và Time-to-hire. | 9 | 44 | 350 | 0 |

---

### 5.2. Chi tiết Product Backlog (76 User Stories)

#### 🚀 SPRINT 1: Tài khoản, Phân quyền & Quản trị Người dùng (10 stories · 42 points) — *HOÀN THÀNH 100%*
> **Goal:** Admin tạo tài khoản cho nhân sự; phân quyền chuẩn xác theo vai trò; hiển thị đúng Menu.

| Mã ID | Epic | Vai trò | User Story | Tiêu chí Chấp nhận (Acceptance Criteria) | Pt | Ưu tiên | Trạng thái |
| :---: | :---: | :---: | :--- | :--- | :---: | :---: | :---: |
| **S1-01** | EP-01 | Internal | Đăng nhập bằng Email & Mật khẩu | Đăng nhập đúng vào trang chủ tương ứng vai trò. Sai thông tin báo lỗi generic. Khóa 15 phút nếu sai 5 lần liên tiếp. | 5 | Must | ✅ DONE |
| **S1-02** | EP-01 | Internal | Duy trì phiên & Đăng xuất an toàn | Gia hạn tự động khi active qua refresh token. Đăng xuất thu hồi token phía Server ngay lập tức. | 3 | Must | ✅ DONE |
| **S1-03** | EP-01 | Internal | Quên mật khẩu qua Email | Gửi link reset password có hiệu lực 30 phút, mã hash SHA-256, dùng 1 lần duy nhất. | 5 | Must | ✅ DONE |
| **S1-04** | EP-01 | Internal | Chủ động Đổi mật khẩu | Bắt buộc xác thực mật khẩu cũ; mật khẩu mới $\ge 8$ ký tự (có chữ & số). Thu hồi phiên đăng nhập khác. | 2 | Must | ✅ DONE |
| **S1-05** | EP-01 | Admin | Phân quyền RBAC toàn hệ thống | Phân quyền 7 vai trò. Server-side check mặc định deny. Báo lỗi tiếng Việt. Có Automated test cho các roles. | 8 | Must | ✅ DONE |
| **S1-06** | EP-01 | Internal | Hiển thị Menu theo phân quyền | Nổi/ẩn menu theo đúng quyền hạn. Bố cục responsive chuẩn từ màn hình 360px. | 5 | Must | ✅ DONE |
| **S1-07** | EP-01 | Internal | Trang thông báo lỗi truy cập | Báo lỗi 403/404 đồng bộ giao diện, gợi ý hành động quay lại hoặc về trang chủ. | 1 | Should | ✅ DONE |
| **S1-08** | EP-01 | Admin | Quản lý Tài khoản nội bộ | Tạo tài khoản gửi mail kích hoạt + MK tạm. Lọc/tìm kiếm theo tên, email, phòng ban, phân trang 20 dòng. | 8 | Must | ✅ DONE |
| **S1-09** | EP-01 | Admin | Gán / Thu hồi Vai trò | 1 người dùng có thể giữ nhiều vai trò. Hiệu lực tức thì. Chặn tự thu hồi quyền Admin của chính mình. | 3 | Must | ✅ DONE |
| **S1-10** | EP-01 | Admin | Khóa / Mở khóa Tài khoản | Khóa tài khoản sẽ hủy ngay phiên làm việc, bắt buộc ghi lý do. Cảnh báo bàn giao vị trí đang phụ trách. | 2 | Must | ✅ DONE |

---

#### 🏛️ SPRINT 2: Bộ khung Tổ chức, Khung Năng lực & Yêu cầu Tuyển dụng (10 stories · 45 points)
> **Goal:** HR khai báo sơ đồ tổ chức, khung năng lực và Hiring Manager tạo Yêu cầu tuyển dụng đầu tiên.

| Mã ID | Epic | Vai trò | User Story | Tiêu chí Chấp nhận (Acceptance Criteria) | Pt | Ưu tiên |
| :---: | :---: | :---: | :--- | :--- | :---: | :---: |
| **S2-01** | EP-01 | Admin | Import Tài khoản hàng loạt từ Excel | Tải file mẫu, preview báo lỗi từng dòng, nhập dòng hợp lệ, xuất báo cáo kết quả. | 5 | Should |
| **S2-02** | EP-01 | Internal | Xem & Cập nhật Hồ sơ cá nhân | Sửa họ tên, SĐT (validate SĐT VN), chức danh. Không tự sửa Email/Phòng ban/Role. | 3 | Must |
| **S2-03** | EP-01 | Internal | Upload Avatar cá nhân | Chấp nhận JPG/PNG $\le$ 2MB. Tự động crop vuông và thumbnail. | 2 | Could |
| **S2-04** | EP-02 | HR Mgr | Khai báo Sơ đồ Tổ chức | Cấu trúc cây đa cấp, có Trưởng phòng. Phòng ban đang tuyển không cho xóa (chỉ Inactive). | 5 | Must |
| **S2-05** | EP-02 | HR Mgr | Khai báo Chức danh & Dải lương | Mã, tên, cấp bậc, dải lương Min-Max. Chỉ HR Manager xem được dải lương. | 5 | Must |
| **S2-06** | EP-02 | HR Mgr | Khai báo Khung năng lực | Gắn bộ tiêu chí + trọng số ($\sum = 100\%$). Reusable cho nhiều chức danh. Sinh phiếu phỏng vấn ở S6. | 5 | Must |
| **S2-07** | EP-02 | HR Mgr | Ngân hàng Câu hỏi Phỏng vấn | Gắn câu hỏi với tiêu chí khung năng lực, độ khó, gợi ý câu trả lời chuẩn. | 5 | Should |
| **S2-08** | EP-02 | HR Mgr | Danh mục dùng chung | Nguồn ứng viên, lý do loại, địa điểm, hình thức làm việc. Ràng buộc không xóa khi đang tham chiếu. | 5 | Must |
| **S2-09** | EP-04 | HR Mgr | Cấu hình Trang công ty Portal | Soạn văn bản giới thiệu, logo, hình ảnh, xem trước đúng giao diện public. | 2 | Could |
| **S2-10** | EP-03 | Hiring Mgr | Tạo Yêu cầu Tuyển dụng (Requisition) | Khai báo vị trí, số lượng, lý do, dải lương đề xuất, JD, ngày cần người. Vượt dải lương bắt buộc giải trình. | 8 | Must |

---

#### 🔄 SPRINT 3: Luồng Phê duyệt Headcount & Đăng tin Tuyển dụng (10 stories · 44 points)
> **Goal:** Headcount đi trọn luồng duyệt nhiều cấp, kiểm soát ngân sách và chuyển thành tin tuyển dụng công khai.

| Mã ID | Epic | Vai trò | User Story | Tiêu chí Chấp nhận (Acceptance Criteria) | Pt | Ưu tiên |
| :---: | :---: | :---: | :--- | :--- | :---: | :---: |
| **S3-01** | EP-03 | HR Mgr | Cấu hình Luồng phê duyệt đa cấp | Cấu hình chuỗi cấp duyệt theo phòng ban/lương. Tự động thêm cấp duyệt cao hơn khi vượt hạn mức. | 8 | Must |
| **S3-02** | EP-03 | Approver | Duyệt / Từ chối / Bổ sung Yêu cầu | Duyệt/Từ chối/Yêu cầu bổ sung. 2 hành động sau bắt buộc nhập ý kiến. Lưu vết lịch sử đầy đủ. | 5 | Must |
| **S3-03** | EP-03 | Hiring Mgr | Theo dõi Tiến độ Phê duyệt | Màn hình visual chuỗi cấp duyệt (đã duyệt, đang chờ ai), thời điểm và ý kiến người duyệt. Không sửa/xóa log. | 3 | Must |
| **S3-04** | EP-03 | HR Mgr | Quản lý Ngân sách Headcount | Khai báo chỉ tiêu headcount/ngân sách theo năm. Cảnh báo chặn khi vượt chỉ tiêu (cần HR Mgr override). | 5 | Must |
| **S3-05** | EP-03 | Hiring Mgr | Clone Yêu cầu tuyển dụng | Sao chép mô tả công việc/yêu cầu từ yêu cầu cũ. Bản sao bắt đầu ở trạng thái Nháp. | 2 | Should |
| **S3-06** | EP-03 | HR Mgr | Phân công Recruiter phụ trách | Phân công 1 Recruiter chính + nhiều Recruiter hỗ trợ. Ghi log khi thay đổi người phụ trách. | 3 | Must |
| **S3-07** | EP-03 | HR Mgr | Tạm dừng / Đóng / Hủy Yêu cầu | Đóng yêu cầu tự động ẩn tin tuyển dụng đang đăng. Cảnh báo ứng viên đang trong pipeline. | 5 | Must |
| **S3-08** | EP-03 | HR Mgr | Danh sách Yêu cầu & Cảnh báo trễ | Bộ lọc đa điều kiện. Hiển thị số ngày mở, đếm ngược ngày cần người. Highlight vị trí quá hạn. | 5 | Must |
| **S3-09** | EP-04 | Recruiter | Soạn Tin tuyển dụng từ Yêu cầu | Kế thừa JD từ Requisition đã duyệt, chỉnh sửa ngôn từ, chọn hạn nộp, cấu hình hiển thị lương. | 5 | Must |
| **S3-10** | EP-04 | HR Mgr | Phê duyệt & Xuất bản Tin đăng | Phê duyệt tin đăng trước khi public, preview chuẩn giao diện, ghi nhận thời điểm xuất bản. | 3 | Must |

---

#### 📱 SPRINT 4: Cổng Ứng tuyển & Hồ sơ Ứng viên (9 stories · 45 points)
> **Goal:** Ứng viên xem job, nộp CV trên di động, tra cứu trạng thái; Recruiter xem hồ sơ hợp nhất.

| Mã ID | Epic | Vai trò | User Story | Tiêu chí Chấp nhận (Acceptance Criteria) | Pt | Ưu tiên |
| :---: | :---: | :---: | :--- | :--- | :---: | :---: |
| **S4-01** | EP-04 | Candidate | Xem Danh sách Việc làm Public | Không cần đăng nhập. Lọc theo phòng ban, địa điểm, từ khóa. Chỉ hiện tin active/còn hạn. | 5 | Must |
| **S4-02** | EP-04 | Candidate | Xem Chi tiết Tin tuyển dụng | Hiển thị JD, Yêu cầu, Quyền lợi. Nút Ứng tuyển tối ưu di động. Tin hết hạn ẩn biểu mẫu nộp. | 5 | Must |
| **S4-03** | EP-04 | Candidate | Nộp CV ứng tuyển trực tuyến | Form gồm Họ tên, Email, SĐT, CV (PDF/DOCX $\le 10$MB). Rate-limiting, anti-spam, sinh mã tra cứu + mail xác nhận. | 8 | Must |
| **S4-04** | EP-04 | Recruiter | Auto-parsing thông tin từ CV | Bóc tách tự động Name/Email/Phone từ PDF làm gợi ý (không tự ghi đè). Lỗi parsing không hỏng luồng nộp. | 5 | Should |
| **S4-05** | EP-04 | Candidate | Tra cứu Trạng thái Hồ sơ | Tra cứu bằng Mã tra cứu + Email. Chỉ hiện trạng thái chung (Đã nhận, Đang xem, Mời PV, Kết thúc). Ẩn ghi chú nội bộ. | 5 | Must |
| **S4-06** | EP-04 | Candidate | Rút Hồ sơ Ứng tuyển | Rút hồ sơ qua link email token. Cập nhật trạng thái "Ứng viên rút", thông báo cho Recruiter phụ trách. | 2 | Should |
| **S4-07** | EP-04 | Internal | Giới thiệu Ứng viên (Referral) | Nhân viên nộp CV giúp người quen. Gắn nhãn "Giới thiệu nội bộ" + Tên người giới thiệu. | 5 | Should |
| **S4-08** | EP-04 | Recruiter | Gỡ tin / Tự động ẩn tin quá hạn | Gỡ thủ công lập tức; Cronjob hằng ngày tự động ẩn các tin quá hạn nhận hồ sơ. | 2 | Must |
| **S4-09** | EP-05 | Recruiter | Xem Hồ sơ Ứng viên Hợp nhất | 1 màn hình chứa: Thông tin cá nhân, Preview CV online, Lịch sử ứng tuyển các lần trước, Tag, Notes. Server check permission. | 8 | Must |

---

#### 📊 SPRINT 5: Pipeline Tuyển dụng & Sàng lọc Hồ sơ (10 stories · 45 points)
> **Goal:** Quản lý ứng viên dạng Kanban kéo-thả, gộp trùng hồ sơ, chấm điểm sàng lọc, nhắc SLA quá hạn.

| Mã ID | Epic | Vai trò | User Story | Tiêu chí Chấp nhận (Acceptance Criteria) | Pt | Ưu tiên |
| :---: | :---: | :---: | :--- | :--- | :---: | :---: |
| **S5-01** | EP-05 | Recruiter | Cảnh báo & Gộp hồ sơ trùng | Tự động phát hiện trùng Email/SĐT. Xem so sánh side-by-side trước khi gộp. Giữ toàn bộ history. Undo trong 24h. | 5 | Must |
| **S5-02** | EP-05 | Recruiter | Điều phối Pipeline dạng Kanban | Các cột stage chuẩn. Drag-and-drop chuyển giai đoạn, hiển thị count từng cột. Filter theo vị trí. | 8 | Must |
| **S5-03** | EP-05 | Recruiter | Chuyển stage & Ghi lý do Loại | Chuyển sang "Loại" bắt buộc chọn lý do. Ghi audit log. Chặn nhảy cóc qua vòng PV tới Offer (trừ khi HR Mgr duyệt). | 5 | Must |
| **S5-04** | EP-05 | Recruiter | Đánh giá nhanh, Gắn Tag & Note | Gắn tag danh mục, đánh giá 1–5 sao, Note nội bộ (ẩn hoàn toàn với ứng viên). | 5 | Must |
| **S5-05** | EP-05 | Recruiter | Bộ lọc & Tìm kiếm Ứng viên Advanced | Lọc đa tiêu chí (Stage, nguồn, tag, thời gian, sao). Tìm kiếm full-text. Cho phép lưu bộ lọc hay dùng. | 5 | Must |
| **S5-06** | EP-05 | HR Mgr | Quản lý Kho Ứng viên Tiềm năng | Đánh giá đưa ứng viên trượt vòng cuối vào Kho tiềm năng. Tự động gợi ý ứng viên phù hợp khi mở vị trí mới. | 5 | Should |
| **S5-07** | EP-05 | Recruiter | Xem Nhật ký Tương tác (Timeline) | Timeline sắp xếp mới nhất: Nộp CV, Chuyển stage, Mail đã gửi, Lịch PV, Phiếu đánh giá. | 2 | Must |
| **S5-08** | EP-05 | Recruiter | Thêm Hồ sơ thủ công (Sourcing) | Import tay ứng viên từ LinkedIn/Headhunter. Đi qua đúng luồng check trùng. | 2 | Must |
| **S5-09** | EP-05 | Recruiter | Chấm điểm Sàng lọc Tự động | Khai báo tiêu chí bắt buộc (số năm kn, bằng cấp). Gắn điểm match + flag Đạt/Không đạt để gợi ý sort. | 5 | Should |
| **S5-10** | EP-08 | Recruiter | Cảnh báo Quá hạn SLA Sàng lọc | Cấu hình SLA theo stage (ví dụ 3 ngày cho CV mới). Nổi cờ cảnh báo & gửi thông báo nhắc việc hằng ngày. | 3 | Should |

---

#### 🗓️ SPRINT 6: Đặt lịch Phỏng vấn & Phiếu Đánh giá (9 stories · 43 points)
> **Goal:** Đặt lịch PV đơn/hàng loạt không trùng, gửi thư mời xác nhận online, chấm điểm theo khung năng lực.

| Mã ID | Epic | Vai trò | User Story | Tiêu chí Chấp nhận (Acceptance Criteria) | Pt | Ưu tiên |
| :---: | :---: | :---: | :--- | :--- | :---: | :---: |
| **S6-01** | EP-05 | Recruiter | Xuất Excel Danh sách Ứng viên | Export theo bộ lọc hiện tại, loại bỏ note nhạy cảm nội bộ. Tên file chứa Mã vị trí + Ngày export. | 3 | Should |
| **S6-02** | EP-06 | Recruiter | Đặt lịch Phỏng vấn đơn lẻ | Chọn vòng, ngày giờ, phòng/link online, hội đồng PV. Chặn đặt quá khứ. Đồng bộ hồ sơ & calendar nội bộ. | 8 | Must |
| **S6-03** | EP-06 | Recruiter | Đặt lịch Phỏng vấn hàng loạt | Chọn nhiều ứng viên + 1 khung giờ -> Tự động chẻ time-slot liên tiếp. Preview slot trước khi gửi mail. | 5 | Should |
| **S6-04** | EP-06 | Recruiter | Cảnh báo Trùng lịch Phỏng vấn | Phát hiện trùng lịch người phỏng vấn hoặc phòng họp. Chặn lưu (trừ khi Recruiter chọn Override + ghi lý do). | 5 | Must |
| **S6-05** | EP-06 | Candidate | Nhận Mail Mời & Xác nhận Tham dự | Email chứa thông tin PV. Candidate bấm nút "Xác nhận" / "Xin đổi lịch" qua link token. Cập nhật ngay cho Recruiter. | 5 | Must |
| **S6-06** | EP-06 | Recruiter | Dời / Hủy lịch Phỏng vấn | Đổi thời gian/địa điểm hoặc Hủy (bắt buộc nhập lý do). Tự động gửi mail thông báo các bên. Khóa chỉnh sửa nếu đã có phiếu PV. | 3 | Must |
| **S6-07** | EP-06 | HR Mgr | Sinh Phiếu Đánh giá Khung năng lực | Phiếu PV tự động lấy tiêu chí & trọng số từ EP-02. Chấm điểm 1–5 + comment. Tự tính điểm tổng theo trọng số. | 8 | Must |
| **S6-08** | EP-06 | Interviewer | Nộp Phiếu Đánh giá Phỏng vấn | Bắt buộc chọn Kết luận (Nên tuyển / Cân nhắc / Không tuyển). Khóa sửa sau nộp (trừ khi Recruiter re-open trong 24h). Blind review trước khi nộp. | 3 | Must |
| **S6-09** | EP-06 | Hiring Mgr | So sánh Ứng viên vòng cuối | Bảng so sánh matrix điểm từng tiêu chí của các ứng viên cùng vị trí. Sort theo tổng điểm weighted. | 3 | Should |

---

#### 💼 SPRINT 7: Quyết định Tuyển, Offer & Onboarding (9 stories · 42 points)
> **Goal:** Ra quyết định tuyển, duyệt Offer theo hạn mức lương, gửi thư nhận việc, khởi tạo Checklist Onboarding.

| Mã ID | Epic | Vai trò | User Story | Tiêu chí Chấp nhận (Acceptance Criteria) | Pt | Ưu tiên |
| :---: | :---: | :---: | :--- | :--- | :---: | :---: |
| **S7-01** | EP-06 | Interviewer | Xem Lịch Phỏng vấn Cá nhân | Dashboard cá nhân hiển thị các buổi PV sắp tới, link CV, highlight buổi chưa nộp phiếu đánh giá. | 3 | Must |
| **S7-02** | EP-06 | Interviewer | Gợi ý Câu hỏi PV trong màn hình | Hiển thị ngân hàng câu hỏi theo tiêu chí trong phiếu PV. Đánh dấu câu đã hỏi ở các vòng trước. | 3 | Should |
| **S7-03** | EP-06 | Hiring Mgr | Ra Quyết định Tuyển dụng cuối cùng | Tổng hợp phiếu PV mọi vòng. Chọn: "Đề xuất Offer" hoặc "Loại (kèm lý do)". Ghi log người quyết định. | 5 | Must |
| **S7-04** | EP-07 | Recruiter | Soạn Đề xuất Offer | Vị trí, Lương, Phụ cấp, Ngày nhận việc, Hạn phản hồi. Vượt dải lương bắt buộc giải trình. 1 Candidate có 1 active offer. | 8 | Must |
| **S7-05** | EP-07 | Approver | Phê duyệt Offer theo Hạn mức | Lương chuẩn = HR Mgr duyệt; Vượt dải lương = Bổ sung Cấp duyệt cao hơn. Duyệt/Từ chối/Yêu cầu sửa (kèm ý kiến). | 5 | Must |
| **S7-06** | EP-07 | Candidate | Nhận Thư Offer & Phản hồi Online | Mail chứa nội dung Offer + Link token. Chọn Chấp nhận/Từ chối + Ý kiến. Tự động chuyển Hết hạn khi quá time. | 5 | Must |
| **S7-07** | EP-07 | Recruiter | Xử lý Từ chối Offer / Thương lượng | Ghi lý do từ chối. Cho phép tạo Offer sửa đổi (Offer cũ đổi thành Superseded). Gợi ý lưu kho tiềm năng. | 5 | Must |
| **S7-08** | EP-07 | HR Mgr | Khởi tạo Checklist Onboarding | Sinh checklist theo template phòng ban (Tài khoản, Thiết bị, Chỗ ngồi). Assign người phụ trách & deadline. Đạt 100% -> Onboarded. | 5 | Must |
| **S7-09** | EP-08 | Recruiter | Gửi Email Cảm ơn / Từ chối hàng loạt | Chọn gửi hàng loạt ứng viên trượt. Preview data thật từng người trước khi bấm gửi. Chặn gửi trùng thư từ chối. | 3 | Should |

---

#### 📈 SPRINT 8: Email Tự động, Thông báo & Báo cáo Tuyển dụng (9 stories · 44 points)
> **Goal:** Cấu hình mẫu email tự động, thông báo in-app, Dashboard tổng quan và các báo cáo phễu/chi phí.

| Mã ID | Epic | Vai trò | User Story | Tiêu chí Chấp nhận (Acceptance Criteria) | Pt | Ưu tiên |
| :---: | :---: | :---: | :--- | :--- | :---: | :---: |
| **S8-01** | EP-08 | HR Mgr | Cấu hình Mẫu Email Tự động | Template cho các stage (Xác nhận nộp, Mời PV, Từ chối, Offer). Chèn placeholder name/job/time. Toggle On/Off. | 5 | Must |
| **S8-02** | EP-08 | Recruiter | Nhật ký Email & Resend mail lỗi | Log thời điểm, template, trạng thái thành công/thất bại. Nút Resend mail lỗi. Gửi bất đồng bộ qua Queue. | 3 | Must |
| **S8-03** | EP-08 | Internal | Thông báo Trong Ứng dụng (In-app) | Notification bell hiển thị số chưa đọc cho các event (Chờ duyệt, PV mới, Candidate phản hồi). Bất đồng bộ. | 5 | Must |
| **S8-04** | EP-08 | Internal | Cấu hình Bật/Tắt Kênh Thông báo | Cho phép nhân sự bật/tắt nhận thông báo qua Email hoặc In-app. Khóa tắt các thông báo bắt buộc (Pending approval). | 3 | Could |
| **S8-05** | EP-09 | HR Mgr | Dashboard Báo cáo Tổng quan ATS | KPI cards (Vị trí mở, CV mới, PV sắp tới, Offer chờ). Biểu đồ CV theo tuần & Job closed. Highlight vị trí trễ hạn. Load $< 2s$. | 8 | Must |
| **S8-06** | EP-09 | Hiring Mgr | Dashboard Tiến độ Vị trí thuộc mình | Hiển thị phễu ứng viên của riêng vị trí thuộc Hiring Mgr, lịch PV sắp tới, đếm ngược tới ngày cần người. | 5 | Should |
| **S8-07** | EP-09 | HR Mgr | Báo cáo Phễu & Tỷ lệ Chuyển đổi | Conversion rate qua từng stage, filter vị trí/phòng ban/thời gian. Chỉ ra bottleneck rơi rụng hồ sơ. | 5 | Must |
| **S8-08** | EP-09 | HR Mgr | Báo cáo Time-to-hire & Cost-per-hire | Tính avg Time-to-hire (từ Requisition duyệt -> Onboarded) & Cost-per-hire (Chi phí đăng tin + Referral bonus). Export Excel. | 5 | Should |
| **S8-09** | EP-09 | HR Mgr | Báo cáo Hiệu quả Nguồn Ứng viên | Thống kê số CV, % qua sàng lọc, số Onboarded theo từng nguồn (LinkedIn, Referral, Job portal). Export Excel. | 5 | Should |

---

## ✅ 6. Quy chuẩn Chất lượng (DoR & DoD)

### 6.1. Definition of Ready (DoR) — Tiêu chí để Story đưa vào Sprint
- User story được viết đúng định dạng: `Là <vai trò>, tôi muốn <hành động>, để <giá trị>`.
- Có đầy đủ tiêu chí chấp nhận (Acceptance Criteria - AC) có thể kiểm chứng được, không mơ hồ.
- Đã được ước lượng Point (Planning Poker) và toàn bộ đội ngũ có cùng hiểu biết.
- Không còn phụ thuộc kỹ thuật (Technical dependencies) chưa xử lý.
- Đã có Wireframe / Phác thảo giao diện (nếu story có UI).
- Kích thước Story $\le 8$ Points; nếu bằng 8 points phải chẻ nhỏ thành các phần API + UI.

### 6.2. Definition of Done (DoD) — Tiêu chí để Story hoàn thành
- Tất cả các tiêu chí chấp nhận (AC) được kiểm thử đạt 100%.
- Code đã qua Code Review bởi ít nhất 1 thành viên khác và được merge vào nhánh `main`.
- Có Unit / Integration Test cho tầng Service, độ phủ nhánh mới (Branch Coverage) $\ge 60\%$.
- Tiến trình CI Build, Lint, Test pass thành công 100%.
- Đã deploy thành công lên môi trường Staging và hoạt động bình thường.
- Quyền truy cập được kiểm tra nghiêm ngặt tại tầng Server (Server-side RBAC).
- Giao diện đáp ứng tốt (Responsive) trên màn hình kích thước từ 360px.
- Không còn tồn đọng lỗi thuộc mức Major trở lên.
- Product Owner (PO) nghiệm thu trực tiếp trên môi trường chạy thực nghiệm.

---

## ⚠️ 7. Quản trị Rủi ro & Giả định (Risks & Assumptions)

### 7.1. Các Giả định cần PO Xác nhận
- **G1:** Không tích hợp API đăng tin tự động ra các trang bên ngoài (TopCV, VietnamWorks, LinkedIn).
- **G2:** Không đồng bộ 2 chiều với Google Calendar hay Outlook Calendar (Người phỏng vấn xem lịch trên hệ thống).
- **G3:** Luồng phê duyệt tối đa 3 cấp tuần tự (Không xử lý duyệt song song hoặc ma trận phức tạp).
- **G4:** Hạn mức duyệt Offer tính theo dải lương chuẩn của chức danh.
- **G5:** Không lưu trữ hợp đồng lao động và không ký số trong ứng dụng (Dừng lại ở Thư mời nhận việc - Offer Letter).
- **G6:** Ứng viên ngoài không có tài khoản đăng nhập portal (Chỉ tra cứu qua Mã tra cứu bí mật + Email).

### 7.2. Bảng Quản trị Rủi ro (Risk Management)

| # | Rủi ro nhận diện | Mức độ | Tác động | Phương án Ứng phó & Xử lý |
| :-: | :--- | :---: | :---: | :--- |
| **1** | Nợ kỹ thuật (Tech Debt) bùng nổ ở Sprint 6–7 do bỏ quên việc refactor. | Cao | Cao | Lập danh sách Kỹ thuật riêng ở Sprint 1; bảo vệ 15% năng suất mỗi Sprint cho công việc hạ tầng. |
| **2** | Velocity thực tế 2 Sprint đầu thấp hơn 42 Points. | Cao | Cao | Đánh giá lại Velocity sau Sprint 2; sẵn sàng cắt bỏ các story thuộc nhóm Could và Should. |
| **3** | Luồng duyệt đa cấp (S3-01) là State Machine phức tạp quá ước lượng. | Cao | Cao | Làm Spike kỹ thuật ở Sprint 2. Nếu quá thời gian, hạ cấp xuống luồng duyệt 2 cấp cố định. |
| **4** | Parsing CV tiếng Việt (S4-04) cho kết quả kém với định dạng lạ. | Cao | Thấp | Thiết kế dạng gợi ý không tự ghi đè. Nếu chất lượng kém, hủy story và thu hồi 5 points. |
| **5** | Sprint 1 tuần khiến story 8 points không đóng kịp trong 1 Sprint. | Cao | Trung bình | Chẻ nhỏ tất cả story 8 points thành 2 sub-tasks (API & UI) ngay tại buổi Sprint Planning. |
| **6** | Rò rỉ dữ liệu cá nhân ứng viên do thiếu hụt phân quyền. | Trung bình | Cao | Bắt buộc tích hợp kiểm tra RBAC tại Server-side từ Sprint 1; dành riêng Story S8-05 rà soát bảo mật. |
| **7** | Không có vị trí tuyển dụng thực tế để chạy thử nghiệm Pilot. | Thấp | Cao | Chuẩn bị sẵn bộ dữ liệu mẫu (Seed Data) hoàn chỉnh với các phòng ban, chức danh và vị trí tuyển dụng tiêu chuẩn để chạy kịch bản thử nghiệm end-to-end từ Sprint 1. |

---

## 🚀 8. Hướng dẫn Cài đặt & Khởi chạy Nhanh (Sprint 1 Live Environment)

### 8.1. Yêu cầu hệ thống
- **Node.js:** v18+ hoặc v20+ (Khuyến nghị v20 LTS hoặc v24)
- **PostgreSQL:** v14+ (Cổng mặc định `5432`)
- **Docker & Docker Compose** (Tùy chọn nếu muốn chạy PostgreSQL và MailHog container)

### 8.2. Cài đặt các gói phụ thuộc
```bash
# 1. Cài đặt toàn bộ dependencies (Root, Backend, Frontend)
npm install
npm run install:all
```

### 8.3. Khởi tạo Cơ sở Dữ liệu & Seed Data
```bash
# 2. Chạy Migration và Nạp dữ liệu mẫu
npm run db:migrate
npm run db:seed
```

### 8.4. Khởi chạy Ứng dụng
```bash
# 3. Chạy đồng thời cả Backend (5000) và Frontend (5173)
npm run dev
```

- **Frontend Web UI:** [http://localhost:5173](http://localhost:5173)
- **Backend API:** [http://localhost:5000/api](http://localhost:5000/api)
- **Tài liệu Swagger OpenAPI:** [http://localhost:5000/api/docs](http://localhost:5000/api/docs)

### 8.5. Tài khoản Mẫu Đăng nhập Thử nghiệm

| Vai trò | Email đăng nhập | Mật khẩu mẫu | Phạm vi quyền hạn |
| :--- | :--- | :--- | :--- |
| **Quản trị hệ thống (Admin)** | `admin@company.local` | `Admin@123456` | Toàn quyền quản trị tài khoản, vai trò, ma trận quyền và audit log |
| **Trưởng phòng Nhân sự (HR Mgr)** | `hr.manager@company.local` | `Admin@123456` | Quản trị tuyển dụng, phê duyệt, phân công và xem danh sách nhân sự |
| **Chuyên viên Tuyển dụng (Recruiter)**| `recruiter@company.local` | `Admin@123456` | Quản lý tin đăng, ứng viên, pipeline Kanban, xếp lịch phỏng vấn |
| **Trưởng bộ phận (Hiring Manager)** | `hiring.manager@company.local` | `Admin@123456` | Tạo yêu cầu tuyển dụng, xem CV vị trí phụ trách, duyệt ứng viên |
| **Người phỏng vấn (Interviewer)** | `interviewer@company.local` | `Admin@123456` | Xem lịch phỏng vấn cá nhân, chấm phiếu đánh giá năng lực |
| **Cấp phê duyệt (Approver)** | `approver@company.local` | `Admin@123456` | Phê duyệt yêu cầu tuyển dụng và hạn mức offer |
| **Đa vai trò (Tech Lead)** | `leader.tech@company.local` | `Admin@123456` | Kết hợp đồng thời Hiring Manager và Interviewer |

### 8.6. Chạy Bộ Kiểm thử Tự động (Integration Tests)
```bash
cd backend
npm test
```
*Kết quả:* **29/29 tests đạt 100% (Passed)** bao gồm xác thực, khóa tài khoản tự động sau 5 lần nhập sai, xoay vòng refresh token, thu hồi phiên, phân quyền RBAC và kiểm tra bàn giao tuyển dụng.

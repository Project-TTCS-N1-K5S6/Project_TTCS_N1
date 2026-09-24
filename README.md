HỆ THỐNG TUYỂN DỤNG NỘI BỘ - TTCS T9/2026 (K5S6)

Tài liệu hướng dẫn chi tiết về cấu trúc, quy trình vận hành và nguyên tắc quản lý dữ liệu cho file HỆ THỐNG TUYỂN DỤNG NỘI BỘ-TTCS_T926_K5S6.xlsx.

📌 1. Tổng quan Dự án (Project Overview)

Tên file dữ liệu: HỆ THỐNG TUYỂN DỤNG NỘI BỘ-TTCS_T926_K5S6.xlsx

Mục đích: Quản lý tập trung toàn bộ quy trình tuyển dụng nội bộ, thông tin ứng viên, lịch phỏng vấn và đo lường hiệu quả tuyển dụng theo thời gian thực.

Phạm vi áp dụng: Bộ phận HR / Tuyển dụng, Trưởng bộ phận chuyên môn và Ban quản lý dự án TTCS (Kỳ T9/2026 - K5S6).

🎯 2. Mục tiêu Hệ thống

Chuẩn hóa dữ liệu: Quản lý tập trung thông tin ứng viên và định biên các vị trí tuyển dụng trên một nền tảng thống nhất.

Theo dõi tiến độ realtime: Nắm bắt trạng thái từng ứng viên trong quy trình: Sàng lọc → Phỏng vấn → Offer → Onboarding.

Phân tích & Báo cáo: Cung cấp chỉ số đo lường hiệu suất tuyển dụng (Time-to-Hire, Pass Rate, Nguồn tuyển dụng hiệu quả).

📊 3. Cấu trúc Bảng tính (Sheet Structure)

3.1. Dashboard - Báo cáo & Thống kê

Chức năng: Tổng hợp trực quan các chỉ số tuyển dụng chính (KPIs).

Các chỉ số chính:

Tổng số Hồ sơ/CV tiếp nhận.

Số lượng vị trí đang tuyển (Open Vacancies).

Tỷ lệ chuyển đổi qua các vòng (Screening Pass Rate, Interview Pass Rate).

Tỷ lệ chốt Offer thành công.

3.2. Danh_Sach_Ung_Vien - Cơ sở dữ liệu Ứng viên

Chức năng: Lưu trữ thông tin chi tiết của toàn bộ ứng viên.

Các trường dữ liệu chính:

Mã UV: Mã định danh duy nhất (Ví dụ: UV-2026-001).

Họ và Tên, Email, Số điện thoại.

Vị trí ứng tuyển, Phòng ban.

Nguồn ứng viên: Nội bộ, Referral, Website, LinkedIn,...

Trạng thái Hồ sơ: Tiếp nhận → Sàng lọc → Phỏng vấn V1 → Phỏng vấn V2 → Trúng tuyển / Từ chối.

Ghi chú & Đánh giá: Nhận xét từ HR và Trưởng bộ phận.

3.3. Vi_Tri_Tuyen_Dung - Quản lý Vị trí Tuyển dụng

Chức năng: Quản lý định biên và nhu cầu tuyển dụng từ các phòng ban.

Các trường dữ liệu chính:

Mã Vị trí, Tên Chức danh, Phòng ban yêu cầu.

Số lượng cần tuyển, Hạn chót tuyển dụng (Deadline).

Trạng thái Vị trí: Mở (Open) / Tạm dừng (On-hold) / Đã đóng (Closed).

3.4. Lich_Phong_Van - Lịch trình & Đánh giá

Chức năng: Theo dõi thời gian, người phỏng vấn và kết quả chi tiết từng vòng.

Các trường dữ liệu chính:

Thời gian phỏng vấn: Ngày / Giờ.

Người phỏng vấn (Interviewer).

Hình thức: Online / Directly.

Kết quả & Phản hồi chi tiết.

🔄 4. Quy trình Vận hành (Recruitment Workflow)

flowchart LR
    A[1. Tiếp nhận nhu cầu] --> B[2. Cập nhật vị trí]
    B --> C[3. Nhập hồ sơ UV]
    C --> D[4. Sàng lọc & Phỏng vấn]
    D --> E[5. Đánh giá & Offer]
    E --> F[6. Onboarding & Lưu trữ]


Bước 1: Tiếp nhận nhu cầu từ phòng ban → Cập nhật thông tin vào sheet Vi_Tri_Tuyen_Dung.

Bước 2: Tiếp nhận CV → Nhập thông tin ứng viên mới vào sheet Danh_Sach_Ung_Vien.

Bước 3: Sàng lọc CV → Lên lịch phỏng vấn và cập nhật vào sheet Lich_Phong_Van.

Bước 4: Cập nhật kết quả phỏng vấn và chuyển trạng thái hồ sơ tương ứng.

Bước 5: Tổng hợp dữ liệu báo cáo định kỳ trên sheet Dashboard.

🛠️ 5. Hướng dẫn & Quy định Nhập liệu

Định dạng dữ liệu:

Ngày tháng: Sử dụng định dạng chuẩn DD/MM/YYYY.

Trường danh mục: Các cột như Trạng thái, Nguồn ứng viên, Phòng ban sử dụng Data Validation (Dropdown list) để chọn, không gõ tự do nhằm tránh lỗi báo cáo.

Bảo mật thông tin:

File chứa thông tin cá nhân ứng viên và dữ liệu tuyển dụng nội bộ. Vui lòng chỉ chia sẻ cho nhân sự có thẩm quyền.

Sao lưu dữ liệu:

Thực hiện backup định kỳ theo cấu trúc tên file: HTTD_TTCS_YYYYMMDD.xlsx.

📞 6. Thông tin Quản trị

Dự án: Thực tập cơ sở (TTCS) - Kỳ T9/2026 (K5S6)

Phiên bản: 1.0 (GitHub Ready)

Loại tài liệu: README / Document Manual

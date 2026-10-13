USE ttcs_db;

INSERT IGNORE INTO recruitment_jobs (id, title, status, description, requirements) VALUES
  (
    'job-sales',
    'Chuyên viên Kinh doanh',
    'Đang tuyển',
    'Tìm kiếm và phát triển khách hàng mới. Tư vấn giải pháp phù hợp, duy trì quan hệ với khách hàng hiện tại và phối hợp với các phòng ban để đạt mục tiêu kinh doanh.',
    '- Tốt nghiệp Cao đẳng trở lên.\n- Có kỹ năng giao tiếp, đàm phán và chăm sóc khách hàng.\n- Ưu tiên ứng viên có ít nhất 1 năm kinh nghiệm kinh doanh.'
  ),
  (
    'job-java',
    'Lập trình viên Java',
    'Đã đóng',
    'Phát triển, kiểm thử và bảo trì các dịch vụ backend bằng Java. Phối hợp cùng nhóm sản phẩm để phân tích yêu cầu và cải thiện hiệu năng hệ thống.',
    '- Có kiến thức Java, OOP và SQL.\n- Biết sử dụng Git và làm việc theo quy trình nhóm.\n- Có kinh nghiệm với Spring Boot là một lợi thế.'
  ),
  (
    'job-hr-intern',
    'Thực tập sinh Nhân sự',
    'Bản nháp',
    'Hỗ trợ đăng tin tuyển dụng, sàng lọc hồ sơ, sắp xếp lịch phỏng vấn và cập nhật dữ liệu ứng viên.',
    '- Là sinh viên năm cuối hoặc mới tốt nghiệp ngành Quản trị nhân lực.\n- Cẩn thận, chủ động và giao tiếp tốt.\n- Có thể thực tập tối thiểu 3 ngày mỗi tuần.'
  );

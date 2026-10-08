-- ============================================================
-- IRMS (Internal Recruitment Management System)
-- Dữ liệu khởi tạo (Seed Data): MySQL 8.0+
-- File: database/seed.sql
-- Mật khẩu mặc định toàn bộ tài khoản: Admin@123456
-- ============================================================

USE test;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Khởi tạo danh mục phòng ban (Departments)
DELETE FROM departments;
INSERT INTO departments (id, code, name, description) VALUES
('dept-001', 'BGD', 'Ban Giám đốc', 'Hội đồng quản trị và Ban Tổng giám đốc điều hành'),
('dept-002', 'HR', 'Ban Nhân sự & Tuyển dụng', 'Quản trị nhân lực, tuyển dụng và chế độ đãi ngộ nội bộ'),
('dept-003', 'TECH', 'Khối Công nghệ Thông tin', 'Phát triển phần mềm, hạ tầng hệ thống và bảo mật thông tin'),
('dept-004', 'SALES', 'Khối Kinh doanh & Thị trường', 'Phát triển khách hàng doanh nghiệp và đối tác chiến lược'),
('dept-005', 'MKT', 'Phòng Truyền thông & Marketing', 'Xây dựng thương hiệu nhà tuyển dụng và truyền thông nội bộ');

-- 1.1. Danh mục dùng chung
DELETE FROM shared_catalogs;
INSERT INTO shared_catalogs (id, type_code, value, display_order) VALUES
('cat-src-referral', 'APPLICATION_SOURCE', 'Nhân viên giới thiệu', 0),
('cat-src-linkedin', 'APPLICATION_SOURCE', 'LinkedIn', 1),
('cat-src-career-site', 'APPLICATION_SOURCE', 'Trang tuyển dụng công ty', 2),
('cat-src-job-board', 'APPLICATION_SOURCE', 'Trang việc làm', 3),
('cat-rej-experience', 'REJECTION_REASON', 'Kinh nghiệm chưa phù hợp', 0),
('cat-rej-skill', 'REJECTION_REASON', 'Kỹ năng chưa đáp ứng yêu cầu', 1),
('cat-rej-salary', 'REJECTION_REASON', 'Mức lương chưa phù hợp', 2),
('cat-rej-other', 'REJECTION_REASON', 'Lý do khác', 3),
('cat-loc-hanoi', 'WORK_LOCATION', 'Hà Nội', 0),
('cat-loc-hcm', 'WORK_LOCATION', 'Thành phố Hồ Chí Minh', 1),
('cat-loc-danang', 'WORK_LOCATION', 'Đà Nẵng', 2),
('cat-mode-office', 'WORK_MODE', 'Làm việc tại văn phòng', 0),
('cat-mode-hybrid', 'WORK_MODE', 'Làm việc kết hợp', 1),
('cat-mode-remote', 'WORK_MODE', 'Làm việc từ xa', 2);

-- 2. Khởi tạo danh mục vai trò chuẩn (Roles)
DELETE FROM roles;
INSERT INTO roles (id, code, name, description, is_system_role) VALUES
('role-001', 'ADMIN', 'Quản trị hệ thống', 'Quản trị toàn bộ người dùng, vai trò, ma trận phân quyền, bảo mật và nhật ký hệ thống.', TRUE),
('role-002', 'HR_MANAGER', 'Trưởng phòng Nhân sự', 'Quản lý toàn bộ quy trình nhân sự, phê duyệt và giám sát hoạt động tuyển dụng.', TRUE),
('role-003', 'RECRUITER', 'Chuyên viên tuyển dụng', 'Thực thi các chiến dịch tuyển dụng, quản lý hồ sơ ứng viên và điều phối phỏng vấn.', TRUE),
('role-004', 'HIRING_MANAGER', 'Trưởng bộ phận chuyên môn', 'Đề xuất định biên, xem xét hồ sơ kỹ thuật và phối hợp đánh giá ứng viên.', TRUE),
('role-005', 'INTERVIEWER', 'Người phỏng vấn', 'Thực hiện phỏng vấn và chấm điểm đánh giá chuyên môn theo lịch được phân công.', TRUE),
('role-006', 'APPROVER', 'Người phê duyệt', 'Phê duyệt các yêu cầu tuyển dụng và đề xuất tuyển dụng theo thẩm quyền.', TRUE),
('role-007', 'CANDIDATE', 'Ứng viên', 'Tài khoản ứng viên nộp hồ sơ, không có quyền truy cập quản trị nội bộ.', TRUE);

-- 3. Khởi tạo danh mục quyền hạn 10 phân hệ (Permissions)
DELETE FROM permissions;
INSERT INTO permissions (id, code, name, module, action, description) VALUES
-- 1. Tổ chức & vị trí
('p-001', 'department.view', 'Xem phòng ban & tổ chức', 'departments', 'view', 'Xem danh sách và cơ cấu các phòng ban trong tổ chức'),
('p-002', 'departments.manage', 'Quản lý tổ chức & vị trí', 'departments', 'manage', 'Thiết lập danh mục cơ cấu tổ chức, phòng ban và chức danh'),

-- 2. Yêu cầu tuyển dụng
('p-003', 'requisitions.view', 'Xem yêu cầu tuyển dụng', 'requisitions', 'view', 'Xem danh sách các phiếu yêu cầu tuyển dụng'),
('p-004', 'requisitions.create', 'Tạo yêu cầu tuyển dụng', 'requisitions', 'create', 'Lập phiếu yêu cầu tuyển dụng nhân sự mới'),
('p-005', 'requisitions.update', 'Cập nhật yêu cầu tuyển dụng', 'requisitions', 'update', 'Chỉnh sửa thông tin yêu cầu tuyển dụng'),
('p-006', 'requisitions.approve', 'Phê duyệt yêu cầu tuyển dụng', 'requisitions', 'approve', 'Duyệt hoặc từ chối phiếu yêu cầu tuyển dụng'),

-- 3. Tin tuyển dụng
('p-007', 'job_postings.view', 'Xem tin tuyển dụng', 'job_postings', 'view', 'Xem danh sách các tin tuyển dụng nội bộ và công khai'),
('p-008', 'job_postings.manage', 'Quản lý tin tuyển dụng', 'job_postings', 'manage', 'Tạo, biên tập và đăng tải tin tuyển dụng lên cổng việc làm'),

-- 4. Hồ sơ ứng viên & Pipeline
('p-009', 'candidates.view', 'Xem hồ sơ ứng viên', 'candidates', 'view', 'Tra cứu danh sách và chi tiết hồ sơ ứng viên'),
('p-010', 'candidates.manage', 'Quản lý ứng viên & Pipeline', 'candidates', 'manage', 'Thêm mới, chuyển đổi vòng tuyển dụng và cập nhật ứng viên'),

-- 5. Lịch phỏng vấn
('p-011', 'interviews.view', 'Xem lịch phỏng vấn', 'interviews', 'view', 'Xem lịch phỏng vấn cá nhân hoặc toàn đơn vị'),
('p-012', 'interviews.manage', 'Điều phối lịch phỏng vấn', 'interviews', 'manage', 'Tạo mới, dời lịch và gửi thư mời phỏng vấn'),

-- 6. Đánh giá & Chấm điểm
('p-013', 'interviews.evaluate', 'Chấm điểm phỏng vấn', 'evaluations', 'evaluate', 'Nhập điểm và nhận xét kết quả phỏng vấn ứng viên'),
('p-014', 'evaluations.manage', 'Quản lý biểu mẫu đánh giá', 'evaluations', 'manage', 'Thiết lập tiêu chí đánh giá cho từng vị trí'),

-- 7. Thư mời nhận việc & Tiếp nhận
('p-015', 'offers.view', 'Xem thư mời nhận việc', 'offers', 'view', 'Xem danh sách thư mời nhận việc và tiến độ phản hồi'),
('p-016', 'offers.manage', 'Quản lý offer & tiếp nhận', 'offers', 'manage', 'Lập offer letter và điều phối quy trình tiếp nhận nhân sự mới'),

-- 8. Email & Thông báo
('p-017', 'notifications.view', 'Xem lịch sử thông báo', 'notifications', 'view', 'Xem danh sách email thông báo đã gửi'),
('p-018', 'notifications.send', 'Kích hoạt gửi thông báo', 'notifications', 'send', 'Gửi email tự động tới ứng viên hoặc hội đồng phỏng vấn'),

-- 9. Báo cáo & Thống kê
('p-019', 'reports.view', 'Xem báo cáo & Dashboard', 'reports', 'view', 'Xem các biểu đồ KPI và báo cáo hiệu quả tuyển dụng'),
('p-020', 'reports.export', 'Xuất báo cáo dữ liệu', 'reports', 'export', 'Xuất dữ liệu báo cáo ra file Excel hoặc PDF'),

-- 10. Quản trị hệ thống (Users, Roles, Permissions, Audit, Salary)
('p-021', 'users.view', 'Xem danh sách tài khoản', 'users', 'view', 'Xem danh sách tài khoản nhân viên nội bộ'),
('p-022', 'users.create', 'Tạo mới tài khoản', 'users', 'create', 'Thêm mới tài khoản người dùng vào hệ thống'),
('p-023', 'users.update', 'Cập nhật tài khoản', 'users', 'update', 'Chỉnh sửa thông tin hồ sơ tài khoản nhân sự'),
('p-024', 'users.lock', 'Khóa & Mở khóa tài khoản', 'users', 'lock', 'Tạm khóa hoặc mở khóa tài khoản người dùng'),
('p-025', 'users.reset-password', 'Cấp lại mật khẩu tạm thời', 'users', 'reset-password', 'Reset mật khẩu ngẫu nhiên cho tài khoản'),
('p-026', 'roles.view', 'Xem danh mục vai trò', 'roles', 'view', 'Xem danh sách vai trò phân quyền'),
('p-027', 'permissions.view', 'Xem ma trận phân quyền', 'permissions', 'view', 'Xem ma trận quyền hạn các vai trò'),
('p-028', 'permissions.manage', 'Cấu hình ma trận quyền hạn', 'permissions', 'manage', 'Cập nhật phân quyền cho từng vai trò'),
('p-029', 'audit.view', 'Xem nhật ký kiểm toán', 'audit', 'view', 'Xem lịch sử các thao tác bảo mật và biến động dữ liệu'),
('p-030', 'salary.view', 'Xem dải lương cơ bản', 'salary', 'view', 'Xem thông tin dải ngân sách lương vị trí');

-- 4. Gán quyền hạn cho từng vai trò (Role Permissions)
DELETE FROM role_permissions;

-- ADMIN: Sở hữu tất cả quyền hạn
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-001', id FROM permissions;

-- HR_MANAGER: Sở hữu hầu hết các quyền nghiệp vụ và xem quản trị
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-002', id FROM permissions
WHERE code IN (
    'department.view', 'departments.manage',
    'requisitions.view', 'requisitions.create', 'requisitions.update', 'requisitions.approve',
    'job_postings.view', 'job_postings.manage',
    'candidates.view', 'candidates.manage',
    'interviews.view', 'interviews.manage',
    'interviews.evaluate', 'evaluations.manage',
    'offers.view', 'offers.manage',
    'notifications.view', 'notifications.send',
    'reports.view', 'reports.export',
    'users.view', 'roles.view', 'permissions.view', 'audit.view', 'salary.view'
);

-- RECRUITER: Chuyên sâu pipeline tuyển dụng, KHÔNG có salary.view và quản trị người dùng
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-003', id FROM permissions
WHERE code IN (
    'department.view',
    'requisitions.view', 'requisitions.create', 'requisitions.update',
    'job_postings.view', 'job_postings.manage',
    'candidates.view', 'candidates.manage',
    'interviews.view', 'interviews.manage',
    'interviews.evaluate',
    'offers.view', 'offers.manage',
    'notifications.view', 'notifications.send',
    'reports.view'
);

-- HIRING_MANAGER: Yêu cầu tuyển dụng, xem ứng viên của mình, đánh giá phỏng vấn
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-004', id FROM permissions
WHERE code IN (
    'department.view',
    'requisitions.view', 'requisitions.create',
    'job_postings.view',
    'candidates.view',
    'interviews.view',
    'interviews.evaluate',
    'offers.view',
    'notifications.view',
    'reports.view'
);

-- INTERVIEWER: Chỉ xem phòng ban, xem lịch phỏng vấn được giao và chấm điểm
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-005', id FROM permissions
WHERE code IN (
    'department.view',
    'candidates.view',
    'interviews.view',
    'interviews.evaluate', 'evaluations.manage',
    'notifications.view'
);

-- APPROVER: Phê duyệt yêu cầu và offer
INSERT INTO role_permissions (role_id, permission_id)
SELECT 'role-006', id FROM permissions
WHERE code IN (
    'department.view',
    'requisitions.view', 'requisitions.approve',
    'job_postings.view',
    'candidates.view',
    'offers.view', 'offers.manage',
    'reports.view'
);

-- 5. Khởi tạo danh sách người dùng mẫu (Users)
-- Hash BCrypt chuẩn cho "Admin@123456": $2a$10$PHoFdRDuZxvp05By2bX13OQH1..M.qgemFjjlTM4PoXbqHlMSL9Ke
DELETE FROM users;
INSERT INTO users (id, employee_code, full_name, email, phone, job_title, department_id, password_hash, status, failed_login_attempts, must_change_password) VALUES
('usr-001', 'EMP001', 'Nguyễn Quản Trị', 'admin@company.local', '0901234567', 'Trưởng ban Công nghệ & Quản trị', 'dept-003', '$2a$10$PHoFdRDuZxvp05By2bX13OQH1..M.qgemFjjlTM4PoXbqHlMSL9Ke', 'ACTIVE', 0, FALSE),
('usr-002', 'EMP002', 'Trần Thị Trưởng Phòng Nhân Sự', 'hr.manager@company.local', '0902345678', 'Trưởng phòng Nhân sự', 'dept-002', '$2a$10$PHoFdRDuZxvp05By2bX13OQH1..M.qgemFjjlTM4PoXbqHlMSL9Ke', 'ACTIVE', 0, FALSE),
('usr-003', 'EMP003', 'Lê Chuyên Viên Tuyển Dụng', 'recruiter@company.local', '0903456789', 'Senior IT Recruiter', 'dept-002', '$2a$10$PHoFdRDuZxvp05By2bX13OQH1..M.qgemFjjlTM4PoXbqHlMSL9Ke', 'ACTIVE', 0, FALSE),
('usr-004', 'EMP004', 'Phạm Trưởng Bộ Phận Kỹ Thuật', 'hiring.mgr@company.local', '0904567890', 'Tech Lead / Hiring Manager', 'dept-003', '$2a$10$PHoFdRDuZxvp05By2bX13OQH1..M.qgemFjjlTM4PoXbqHlMSL9Ke', 'ACTIVE', 0, FALSE),
('usr-005', 'EMP005', 'Hoàng Chuyên Gia Phỏng Vấn', 'interviewer@company.local', '0905678901', 'Principal Architect', 'dept-003', '$2a$10$PHoFdRDuZxvp05By2bX13OQH1..M.qgemFjjlTM4PoXbqHlMSL9Ke', 'ACTIVE', 0, FALSE),
('usr-006', 'EMP006', 'Vũ Giám Đốc Điều Hành', 'approver@company.local', '0906789012', 'Phó Tổng Giám Đốc (Approver)', 'dept-001', '$2a$10$PHoFdRDuZxvp05By2bX13OQH1..M.qgemFjjlTM4PoXbqHlMSL9Ke', 'ACTIVE', 0, FALSE),
('usr-007', 'EMP007', 'Đặng Nhân Viên Bị Khóa', 'locked.user@company.local', '0907890123', 'Nhân viên thử việc', 'dept-004', '$2a$10$PHoFdRDuZxvp05By2bX13OQH1..M.qgemFjjlTM4PoXbqHlMSL9Ke', 'LOCKED', 5, FALSE);

-- 6. Gán vai trò cho người dùng (User Roles)
DELETE FROM user_roles;
INSERT INTO user_roles (user_id, role_id) VALUES
('usr-001', 'role-001'), -- admin -> ADMIN
('usr-002', 'role-002'), -- hr.manager -> HR_MANAGER
('usr-003', 'role-003'), -- recruiter -> RECRUITER
('usr-004', 'role-004'), -- hiring.mgr -> HIRING_MANAGER
('usr-005', 'role-005'), -- interviewer -> INTERVIEWER
('usr-006', 'role-006'), -- approver -> APPROVER
('usr-007', 'role-003'); -- locked.user -> RECRUITER

-- 7. Khởi tạo một số yêu cầu tuyển dụng mẫu (Recruitment Requisitions)
DELETE FROM recruitment_requisitions;
INSERT INTO recruitment_requisitions (id, code, title, position_title, department_id, headcount, recruitment_reason, min_salary, max_salary, currency, deadline, job_description, job_requirements, salary_explanation, status, recruiter_id, hiring_manager_id, work_location_id, work_mode_id, created_by) VALUES
('req-001', 'REQ-2026-001', 'Tuyển dụng Kỹ sư Java Backend Senior', 'Kỹ sư Java Backend (Senior)', 'dept-003', 2, 'NEW_HEADCOUNT', 25000000.00, 45000000.00, 'VND', '2026-11-30', '- Thiết kế kiến trúc dịch vụ và RESTful APIs hiệu năng cao.\n- Quản trị CSDL MySQL, tối ưu truy vấn phân tán.\n- Hướng dẫn lập trình viên junior và review code.', '- Tối thiểu 4 năm kinh nghiệm với Java Core & Servlet/Spring Boot.\n- Hiểu sâu về MySQL, Transaction, HikariCP, Indexing.\n- Khả năng đọc hiểu tài liệu tiếng Anh tốt.', NULL, 'OPEN', 'usr-003', 'usr-004', 'cat-loc-hanoi', 'cat-mode-hybrid', 'usr-004'),
('req-002', 'REQ-2026-002', 'Tuyển dụng Chuyên viên Tuyển dụng Nhân sự', 'Chuyên viên Tuyển dụng (Recruiter)', 'dept-002', 1, 'REPLACEMENT', 10000000.00, 18000000.00, 'VND', '2026-11-15', '- Tìm kiếm nguồn ứng viên tiềm năng cho các vị trí công nghệ.\n- Sàng lọc CV, điều phối lịch phỏng vấn và gửi Offer Letter.\n- Xây dựng thương hiệu tuyển dụng trên LinkedIn, TopCV.', '- Tối thiểu 1 năm kinh nghiệm tuyển dụng IT/Tech.\n- Kỹ năng giao tiếp xuất sắc, chủ động và khéo léo.', NULL, 'OPEN', 'usr-003', 'usr-002', 'cat-loc-hanoi', 'cat-mode-office', 'usr-002'),
('req-003', 'REQ-2026-003', 'Tuyển dụng Trưởng nhóm Kinh doanh B2B', 'Trưởng nhóm Kinh doanh B2B', 'dept-004', 1, 'REPLACEMENT', 25000000.00, 42000000.00, 'VND', '2026-12-01', '- Quản lý đội ngũ kinh doanh 8 nhân sự, phụ trách KPI doanh số.\n- Tiếp cận khách hàng doanh nghiệp và đối tác chiến lược.\n- Đàm phán và ký kết hợp đồng dịch vụ lớn.', '- Tối thiểu 3 năm kinh nghiệm Sales B2B phần mềm/SaaS.\n- Khả năng thương thuyết và dẫn dắt đội ngũ tốt.', 'Đề xuất mức lương 25 - 42M vượt dải chuẩn (20 - 35M) do yêu cầu chuyên gia có sẵn mạng lưới khách hàng doanh nghiệp Enterprise cao cấp.', 'PENDING_APPROVAL', 'usr-003', 'usr-006', 'cat-loc-hcm', 'cat-mode-office', 'usr-006'),
('req-004', 'REQ-2026-004', 'Tuyển dụng Kỹ sư Java Backend Junior (Bản nháp)', 'Kỹ sư Java Backend (Junior)', 'dept-003', 3, 'NEW_HEADCOUNT', 12000000.00, 18000000.00, 'VND', '2026-12-15', '- Tham gia phát triển các tính năng phân hệ người dùng theo tài liệu thiết kế.\n- Viết unit test và sửa lỗi hệ thống.', '- Sinh viên mới tốt nghiệp hoặc có dưới 1 năm kinh nghiệm Java.\n- Nắm vững kiến thức OOP, cấu trúc dữ liệu và giải thuật.', NULL, 'DRAFT', 'usr-003', 'usr-004', 'cat-loc-hanoi', 'cat-mode-office', 'usr-004');

-- 8. Khởi tạo danh sách ứng viên mẫu (Candidates)
DELETE FROM candidates;
INSERT INTO candidates (id, requisition_id, application_source_id, full_name, email, phone, status, cv_url, notes) VALUES
('cand-001', 'req-001', 'cat-src-linkedin', 'Nguyễn Văn An', 'an.nguyen@email.test', '0912345671', 'INTERVIEWING', 'https://cv.storage/an_nguyen_java.pdf', 'Ứng viên có 4 năm kinh nghiệm Java Servlet & Spring Boot'),
('cand-002', 'req-001', 'cat-src-referral', 'Trần Bảo Bình', 'binh.tran@email.test', '0912345672', 'SCREENING', 'https://cv.storage/binh_tran_backend.pdf', 'Điểm đồ án tốt nghiệp xuất sắc, tiếng Anh TOEIC 850'),
('cand-003', 'req-001', 'cat-src-career-site', 'Lê Hữu Cường', 'cuong.le@email.test', '0912345673', 'OFFER', 'https://cv.storage/cuong_le_senior.pdf', 'Đã pass vòng phỏng vấn kỹ thuật, đề xuất mức lương 32M'),
('cand-004', 'req-002', 'cat-src-job-board', 'Phạm Minh Dung', 'dung.pham@email.test', '0912345674', 'APPLIED', 'https://cv.storage/dung_pham_hr.pdf', 'Kinh nghiệm 2 năm tuyển dụng IT & Headhunter'),
('cand-005', 'req-003', 'cat-src-referral', 'Hoàng Gia Long', 'long.hoang@email.test', '0912345675', 'HIRED', 'https://cv.storage/long_hoang_sales.pdf', 'Đã nhận việc từ ngày 01/10/2026');

-- 9. Khởi tạo dải lương chuẩn theo vị trí (Salary Ranges)
DELETE FROM salary_ranges;
INSERT INTO salary_ranges (id, position_code, position_title, level, department_id, min_salary, max_salary, currency, note) VALUES
('sal-001', 'DEV-BE-JR', 'Kỹ sư Java Backend', 'Junior', 'dept-003', 12000000.00, 18000000.00, 'VND', 'Hạn mức duyệt offer cho lập trình viên Java dưới 2 năm kinh nghiệm'),
('sal-002', 'DEV-BE-SR', 'Kỹ sư Java Backend', 'Senior', 'dept-003', 25000000.00, 45000000.00, 'VND', 'Hạn mức duyệt offer cho kỹ sư Java Senior từ 4 năm kinh nghiệm'),
('sal-003', 'HR-REC-01', 'Chuyên viên Tuyển dụng', 'Chuyên viên', 'dept-002', 10000000.00, 18000000.00, 'VND', 'Hạn mức cho vị trí Recruiter phụ trách mảng IT & kỹ thuật'),
('sal-004', 'HR-MGR-01', 'Trưởng phòng Nhân sự', 'Trưởng phòng', 'dept-002', 30000000.00, 50000000.00, 'VND', 'Hạn mức ngân sách vị trí Trưởng phòng Nhân sự cấp quản lý'),
('sal-005', 'SALES-LEAD-01', 'Trưởng nhóm Kinh doanh B2B', 'Trưởng nhóm', 'dept-004', 20000000.00, 35000000.00, 'VND', 'Hạn mức lương cứng cho vị trí Team Lead B2B Sales');

-- 10. Khởi tạo một số nhật ký kiểm toán ban đầu (Audit Logs)
DELETE FROM audit_logs;
INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, description, ip_address, user_agent) VALUES
('aud-001', 'usr-001', 'SYSTEM_INIT', 'DATABASE', 'ttcs_db', 'Khởi tạo cấu trúc và nạp dữ liệu mẫu hệ thống IRMS thành công', '127.0.0.1', 'System Bootstrap'),
('aud-002', 'usr-001', 'LOGIN_SUCCESS', 'AUTH', 'usr-001', 'Đăng nhập thành công tài khoản Quản trị hệ thống', '127.0.0.1', 'Mozilla/5.0'),
('aud-003', 'usr-001', 'ROLE_PERMISSIONS_UPDATE', 'ROLE', 'role-002', 'Cập nhật danh sách phân quyền cho vai trò HR_MANAGER', '127.0.0.1', 'Mozilla/5.0');

-- 11. Khởi tạo danh mục tiêu chí năng lực (Competency Criteria)
DELETE FROM competency_criteria;
INSERT INTO competency_criteria (id, name, description) VALUES
('crit-001', 'Kỹ năng lập trình Java', 'Khả năng sử dụng Java, Spring Boot, Hibernate...'),
('crit-002', 'Tư duy logic thuật toán', 'Khả năng giải quyết vấn đề, cấu trúc dữ liệu và thuật toán'),
('crit-003', 'Kỹ năng làm việc nhóm', 'Khả năng giao tiếp, phối hợp với các thành viên khác'),
('crit-004', 'Giải quyết vấn đề', 'Khả năng phân tích và đưa ra giải pháp trong tình huống khó khăn');

-- 12. Khởi tạo Ngân hàng câu hỏi (Questions)
DELETE FROM questions;
INSERT INTO questions (id, content, difficulty_level, good_answer_suggestion, criterion_id, job_title) VALUES
('q-001', 'Nêu sự khác biệt giữa ArrayList và LinkedList trong Java?', 'MEDIUM', 'Ứng viên cần giải thích được sự khác biệt về cấu trúc dữ liệu bên dưới (mảng động vs danh sách liên kết kép) và hiệu năng khi truy cập, chèn, xóa.', 'crit-001', 'Lập trình viên Java'),
('q-002', 'Hãy mô tả một tình huống bạn phải giải quyết xung đột ý kiến với đồng nghiệp. Bạn đã xử lý thế nào?', 'MEDIUM', 'Ứng viên thể hiện sự lắng nghe, tôn trọng ý kiến người khác, tìm kiếm điểm chung và hướng tới mục tiêu chung của dự án.', 'crit-003', 'Tất cả chức danh'),
('q-003', 'Bạn làm thế nào để tối ưu hóa hiệu năng của một ứng dụng Spring Boot bị chậm?', 'HARD', 'Nhắc đến các yếu tố như: Database index, N+1 query problem, Caching (Redis), Connection Pool, JVM Tuning, Profiling tools.', 'crit-001', 'Lập trình viên Java'),
('q-004', 'Cho một mảng chưa sắp xếp, hãy tìm phần tử lớn thứ K trong mảng với độ phức tạp tối ưu nhất.', 'HARD', 'Sử dụng Min-Heap (O(N log K)) hoặc thuật toán QuickSelect (O(N) trung bình).', 'crit-002', 'Lập trình viên Java'),
('q-005', 'Nếu hệ thống đang chạy bị lỗi dẫn đến ngắt dịch vụ, bạn sẽ thực hiện các bước xử lý nào?', 'HARD', '1. Tái thiết lập dịch vụ/Rollback để giảm ảnh hưởng. 2. Thu thập log. 3. Phân tích nguyên nhân gốc (Root Cause). 4. Cập nhật bản vá và viết post-mortem.', 'crit-004', 'DevOps / Backend Senior');

SET FOREIGN_KEY_CHECKS = 1;

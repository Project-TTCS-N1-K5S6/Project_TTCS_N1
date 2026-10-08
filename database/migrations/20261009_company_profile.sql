-- ============================================================
-- Migration: Cấu hình trang giới thiệu công ty (Company Profile / About Us)
-- File: database/migrations/20261009_company_profile.sql
-- ============================================================

USE test;

-- 1. Tạo bảng lưu trữ cấu hình trang giới thiệu công ty
CREATE TABLE IF NOT EXISTS company_profile (
    id VARCHAR(36) PRIMARY KEY,
    company_name VARCHAR(255) NOT NULL,
    brand_name VARCHAR(100),
    slogan VARCHAR(255),
    tagline VARCHAR(255),
    logo_url TEXT,
    banner_url TEXT,
    overview TEXT,
    history_story TEXT,
    mission TEXT,
    vision TEXT,
    core_values TEXT, -- JSON danh sách giá trị cốt lõi
    culture_desc TEXT,
    gallery_urls TEXT, -- JSON hoặc danh sách URL ảnh hoạt động
    perks TEXT, -- JSON danh sách chế độ đãi ngộ
    headquarters_address VARCHAR(255),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    website_url VARCHAR(255),
    facebook_url VARCHAR(255),
    linkedin_url VARCHAR(255),
    youtube_url VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED', -- 'PUBLISHED', 'DRAFT'
    updated_by VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_company_profile_user FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bổ sung quyền hạn (permissions) cho phân hệ Cấu hình trang giới thiệu công ty
INSERT IGNORE INTO permissions (id, code, name, module, action, description) VALUES
('perm-company-view', 'company.view', 'Xem cấu hình trang giới thiệu công ty', 'COMPANY_PROFILE', 'VIEW', 'Cho phép xem trang giới thiệu và cấu hình công ty'),
('perm-company-manage', 'company.manage', 'Quản lý cấu hình trang giới thiệu công ty', 'COMPANY_PROFILE', 'MANAGE', 'Cho phép soạn thảo, tải ảnh/logo và lưu cấu hình trang giới thiệu công ty');

-- 3. Phân quyền cho ADMIN và HR_MANAGER
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('ADMIN', 'HR_MANAGER') 
  AND p.code IN ('company.view', 'company.manage');

-- 4. Tạo bản ghi cấu hình giới thiệu mẫu nếu bảng chưa có dữ liệu
INSERT IGNORE INTO company_profile (
    id, company_name, brand_name, slogan, tagline,
    logo_url, banner_url, overview, history_story,
    mission, vision, core_values, culture_desc, gallery_urls, perks,
    headquarters_address, contact_email, contact_phone, website_url,
    facebook_url, linkedin_url, youtube_url, status
) VALUES (
    'comp-default-01',
    'Tập đoàn Công nghệ & Tuyển dụng IRMS',
    'IRMS Platform',
    'Tiên phong công nghệ - Kiến tạo tương lai nghề nghiệp',
    'Nơi tài năng hội tụ và phát triển vượt bậc',
    '/assets/img/logo-default.png',
    '/assets/img/banner-hero-default.jpg',
    'IRMS là nền tảng quản trị và tuyển dụng nhân tài nội bộ hàng đầu, kết nối nguồn nhân lực chất lượng cao với các dự án công nghệ đột phá. Chúng tôi không ngừng đổi mới để mang lại giá trị bền vững cho nhân sự và khách hàng.',
    'Thành lập từ năm 2020, IRMS đã trải qua hơn 6 năm phát triển mạnh mẽ với quy mô hơn 500 nhân sự chất lượng cao và văn phòng tại các thành phố lớn.',
    'Xây dựng hệ sinh thái công nghệ nhân sự thông minh, minh bạch và gắn kết dài lâu.',
    'Trở thành biểu tượng xuất sắc trong chuyển đổi số nhân tài tại khu vực Đông Nam Á.',
    '[{"icon":"bi-star","title":"Chất lượng vượt trội","desc":"Luôn đặt chuẩn mực cao nhất trong từng sản phẩm và dịch vụ."},{"icon":"bi-lightbulb","title":"Đổi mới sáng tạo","desc":"Khuyến khích tư duy mới, không ngừng thử nghiệm và bứt phá."},{"icon":"bi-shield-check","title":"Chính trực & Minh bạch","desc":"Minh bạch trong mọi quy trình và tôn trọng cam kết với nhân sự."},{"icon":"bi-people","title":"Đồng đội gắn kết","desc":"Cùng nhau vượt qua thử thách và chia sẻ thành công."}]',
    'Tại IRMS, chúng tôi xây dựng một môi trường làm việc cởi mở, tôn trọng sự đa dạng và khuyến khích mỗi cá nhân làm chủ công việc. Văn hóa học hỏi liên tục và cân bằng giữa công việc - cuộc sống là giá trị cốt lõi giúp đội ngũ gắn bó.',
    '["/assets/img/gallery-1.jpg","/assets/img/gallery-2.jpg","/assets/img/gallery-3.jpg"]',
    '[{"icon":"bi-cash-coin","title":"Lương & Thưởng hấp dẫn","desc":"Thu nhập cạnh tranh, đánh giá tăng lương 2 lần/năm và thưởng dự án xuất sắc."},{"icon":"bi-heart-pulse","title":"Bảo hiểm sức khỏe cao cấp","desc":"Gói bảo hiểm sức khỏe toàn diện cho nhân viên và người thân."},{"icon":"bi-laptop","title":"Trang thiết bị hiện đại","desc":"Cấp Macbook/Laptop hiệu năng cao và hỗ trợ làm việc linh hoạt."},{"icon":"bi-mortarboard","title":"Đào tạo & Chứng chỉ","desc":"Tài trợ 100% học phí các khóa đào tạo chuyên sâu và thi chứng chỉ quốc tế."}]',
    'Tòa nhà IRMS Tower, Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội',
    'career@irms.local',
    '(+84) 24 3869 1234',
    'https://irms.local',
    'https://facebook.com/irms.career',
    'https://linkedin.com/company/irms',
    'https://youtube.com/@irms-careers',
    'PUBLISHED'
);

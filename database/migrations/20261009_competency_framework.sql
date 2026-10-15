-- ============================================================
-- IRMS - Khai báo khung năng lực (Competency Framework)
-- Migration Script: database/migrations/20261009_competency_framework.sql
-- ============================================================

USE test;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Nâng cấp bảng tiêu chí năng lực (competency_criteria) bổ sung code, evaluation_guideline, status
SET @col_code = (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competency_criteria' AND COLUMN_NAME = 'code');
SET @sql_code = IF(@col_code = 0, 'ALTER TABLE competency_criteria ADD COLUMN code VARCHAR(50) NULL UNIQUE AFTER id', 'SELECT 1');
PREPARE stmt_code FROM @sql_code;
EXECUTE stmt_code;
DEALLOCATE PREPARE stmt_code;

SET @col_guide = (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competency_criteria' AND COLUMN_NAME = 'evaluation_guideline');
SET @sql_guide = IF(@col_guide = 0, 'ALTER TABLE competency_criteria ADD COLUMN evaluation_guideline TEXT NULL AFTER description', 'SELECT 1');
PREPARE stmt_guide FROM @sql_guide;
EXECUTE stmt_guide;
DEALLOCATE PREPARE stmt_guide;

SET @col_status = (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competency_criteria' AND COLUMN_NAME = 'status');
SET @sql_status = IF(@col_status = 0, "ALTER TABLE competency_criteria ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER evaluation_guideline", 'SELECT 1');
PREPARE stmt_status FROM @sql_status;
EXECUTE stmt_status;
DEALLOCATE PREPARE stmt_status;

-- Cập nhật mã chuẩn cho các tiêu chí mẫu nếu chưa có
UPDATE competency_criteria SET code = 'CRIT-JAVA' WHERE id = 'crit-001' AND (code IS NULL OR code = '');
UPDATE competency_criteria SET code = 'CRIT-LOGIC' WHERE id = 'crit-002' AND (code IS NULL OR code = '');
UPDATE competency_criteria SET code = 'CRIT-TEAM' WHERE id = 'crit-003' AND (code IS NULL OR code = '');
UPDATE competency_criteria SET code = 'CRIT-PROB' WHERE id = 'crit-004' AND (code IS NULL OR code = '');

-- 2. Bảng Khung năng lực (competency_frameworks)
CREATE TABLE IF NOT EXISTS competency_frameworks (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- 'DRAFT', 'ACTIVE', 'INACTIVE'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36) NULL,
    updated_by VARCHAR(36) NULL,
    CONSTRAINT fk_cf_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_cf_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_cf_code (code),
    INDEX idx_cf_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Bảng liên kết Khung năng lực - Tiêu chí đánh giá (competency_framework_criteria)
CREATE TABLE IF NOT EXISTS competency_framework_criteria (
    id VARCHAR(36) PRIMARY KEY,
    framework_id VARCHAR(36) NOT NULL,
    criterion_id VARCHAR(36) NOT NULL,
    weight DECIMAL(5, 2) NOT NULL,
    display_order INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cfc_framework FOREIGN KEY (framework_id) REFERENCES competency_frameworks(id) ON DELETE CASCADE,
    CONSTRAINT fk_cfc_criterion FOREIGN KEY (criterion_id) REFERENCES competency_criteria(id) ON DELETE RESTRICT,
    CONSTRAINT uq_cfc_framework_criterion UNIQUE (framework_id, criterion_id),
    INDEX idx_cfc_framework (framework_id),
    INDEX idx_cfc_criterion (criterion_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Bảng liên kết Chức danh - Khung năng lực (position_competency_frameworks)
-- Mỗi chức danh (từ salary_ranges) chỉ gắn 1 khung năng lực tại 1 thời điểm
CREATE TABLE IF NOT EXISTS position_competency_frameworks (
    id VARCHAR(36) PRIMARY KEY,
    position_id VARCHAR(36) NOT NULL UNIQUE,
    framework_id VARCHAR(36) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(36) NULL,
    CONSTRAINT fk_pcf_position FOREIGN KEY (position_id) REFERENCES salary_ranges(id) ON DELETE CASCADE,
    CONSTRAINT fk_pcf_framework FOREIGN KEY (framework_id) REFERENCES competency_frameworks(id) ON DELETE CASCADE,
    CONSTRAINT fk_pcf_user FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_pcf_framework (framework_id),
    INDEX idx_pcf_position (position_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Bổ sung các quyền hạn mới vào bảng permissions
INSERT INTO permissions (id, code, name, module, action, description) VALUES
('p-031', 'competencies.view', 'Xem khung năng lực', 'competencies', 'view', 'Tra cứu danh sách, chi tiết và chức danh sử dụng khung năng lực')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO permissions (id, code, name, module, action, description) VALUES
('p-032', 'competencies.create', 'Tạo khung năng lực', 'competencies', 'create', 'Tạo mới bộ khung năng lực đánh giá nhân sự')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO permissions (id, code, name, module, action, description) VALUES
('p-033', 'competencies.update', 'Chỉnh sửa khung năng lực', 'competencies', 'update', 'Cập nhật nội dung tiêu chí và cơ cấu trọng số khung năng lực')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO permissions (id, code, name, module, action, description) VALUES
('p-034', 'competencies.status', 'Kích hoạt / Ngừng áp dụng', 'competencies', 'status', 'Kích hoạt hoặc ngừng áp dụng khung năng lực')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO permissions (id, code, name, module, action, description) VALUES
('p-035', 'competencies.assign', 'Gán khung cho chức danh', 'competencies', 'assign', 'Thiết lập khung năng lực áp dụng cho từng chức danh vị trí')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- Gán toàn bộ quyền module competencies cho ADMIN (role-001)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 'role-001', id FROM permissions WHERE module = 'competencies';

-- Gán toàn bộ quyền module competencies cho HR_MANAGER (role-002)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 'role-002', id FROM permissions WHERE module = 'competencies';

-- Gán quyền xem cho HIRING_MANAGER, RECRUITER, INTERVIEWER
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 'role-003', id FROM permissions WHERE code = 'competencies.view';
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 'role-004', id FROM permissions WHERE code = 'competencies.view';
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 'role-005', id FROM permissions WHERE code = 'competencies.view';

-- Nạp thêm một số tiêu chí năng lực đa dạng vào ngân hàng
INSERT IGNORE INTO competency_criteria (id, code, name, description, evaluationGuideline, status) VALUES
('crit-005', 'CRIT-COMM', 'Kỹ năng giao tiếp chuyên nghiệp', 'Khả năng truyền đạt thông tin rõ ràng, thuyết phục và lắng nghe tích cực.', 'Đánh giá qua cách ứng viên trả lời câu hỏi tình huống, sự tự tin và diễn đạt mạch lạc.', 'ACTIVE'),
('crit-006', 'CRIT-LEAD', 'Năng lực lãnh đạo & Quản lý', 'Khả năng định hướng mục tiêu, truyền cảm hứng và phân công công việc hiệu quả.', 'Đánh giá qua kinh nghiệm quản lý đội ngũ và xử lý bất đồng nội bộ.', 'ACTIVE'),
('crit-007', 'CRIT-SALES', 'Kỹ năng đàm phán & Bán hàng', 'Năng lực thấu hiểu nhu cầu khách hàng, xử lý từ chối và chốt hợp đồng B2B.', 'Đánh giá qua số liệu thành tích bán hàng trong quá khứ và cách xử lý tình huống phản biện của khách hàng.', 'ACTIVE'),
('crit-008', 'CRIT-ADAPT', 'Khả năng thích ứng & Học hỏi', 'Sự nhanh nhạy trong tiếp thu công nghệ mới và linh hoạt trước thay đổi.', 'Đánh giá qua các dự án ứng viên tự học kỹ năng mới trong thời gian ngắn.', 'ACTIVE');

-- Khởi tạo mẫu 2 Khung năng lực chuẩn với tổng trọng số đúng 100%
INSERT IGNORE INTO competency_frameworks (id, code, name, description, status, created_by) VALUES
('cf-001', 'KNL-DEV-SR', 'Khung năng lực Kỹ sư Backend Senior', 'Bộ tiêu chí đánh giá kỹ năng chuyên môn và phối hợp dành cho lập trình viên Backend cấp cao', 'ACTIVE', 'usr-001'),
('cf-002', 'KNL-SALES-B2B', 'Khung năng lực Chuyên viên Kinh doanh B2B', 'Bộ tiêu chí đánh giá năng lực đàm phán, giao tiếp và kỹ năng giải quyết vấn đề khách hàng', 'ACTIVE', 'usr-001');

-- Gán tiêu chí cho KNL-DEV-SR (Tổng: 30 + 25 + 25 + 20 = 100%)
INSERT IGNORE INTO competency_framework_criteria (id, framework_id, criterion_id, weight, display_order) VALUES
('cfc-001', 'cf-001', 'crit-001', 30.00, 1), -- Kỹ năng lập trình Java: 30%
('cfc-002', 'cf-001', 'crit-002', 25.00, 2), -- Tư duy logic thuật toán: 25%
('cfc-003', 'cf-001', 'crit-004', 25.00, 3), -- Giải quyết vấn đề: 25%
('cfc-004', 'cf-001', 'crit-003', 20.00, 4); -- Kỹ năng làm việc nhóm: 20%

-- Gán tiêu chí cho KNL-SALES-B2B (Tổng: 35 + 25 + 20 + 20 = 100%)
INSERT IGNORE INTO competency_framework_criteria (id, framework_id, criterion_id, weight, display_order) VALUES
('cfc-005', 'cf-002', 'crit-007', 35.00, 1), -- Đàm phán & Bán hàng: 35%
('cfc-006', 'cf-002', 'crit-005', 25.00, 2), -- Giao tiếp chuyên nghiệp: 25%
('cfc-007', 'cf-002', 'crit-004', 20.00, 3), -- Giải quyết vấn đề: 20%
('cfc-008', 'cf-002', 'crit-003', 20.00, 4); -- Kỹ năng làm việc nhóm: 20%

-- Gán khung năng lực cf-001 cho chức danh sal-002 (DEV-BE-SR - Kỹ sư Java Backend Senior)
INSERT IGNORE INTO position_competency_frameworks (id, position_id, framework_id, assigned_by) VALUES
('pcf-001', 'sal-002', 'cf-001', 'usr-001');

-- Gán khung năng lực cf-002 cho chức danh sal-005 (SALES-LEAD-01 - Trưởng nhóm Kinh doanh B2B)
INSERT IGNORE INTO position_competency_frameworks (id, position_id, framework_id, assigned_by) VALUES
('pcf-002', 'sal-005', 'cf-002', 'usr-001');

SET FOREIGN_KEY_CHECKS = 1;

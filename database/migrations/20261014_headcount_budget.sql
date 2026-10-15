-- ============================================================
-- IRMS - Quản lý Định biên & Ngân sách Nhân sự
-- Migration Script: database/migrations/20261014_headcount_budget.sql
-- ============================================================

USE test;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Bảng Khai báo Định biên & Ngân sách theo Phòng ban và Năm (department_headcount_budgets)
CREATE TABLE IF NOT EXISTS department_headcount_budgets (
    id VARCHAR(36) PRIMARY KEY,
    department_id VARCHAR(36) NOT NULL,
    fiscal_year INT NOT NULL,
    target_headcount INT NOT NULL DEFAULT 0,
    total_salary_budget DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    description TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'INACTIVE', 'LOCKED'
    created_by VARCHAR(36) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(36) NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_dhb_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE,
    CONSTRAINT fk_dhb_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_dhb_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE KEY uk_dept_fiscal_year (department_id, fiscal_year),
    INDEX idx_dhb_year (fiscal_year),
    INDEX idx_dhb_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bảng Danh mục Subtasks công việc (budget_subtasks: KN-195 đến KN-201)
CREATE TABLE IF NOT EXISTS budget_subtasks (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'
    assignee_id VARCHAR(36) NULL,
    assignee_name VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO', -- 'TODO', 'IN_PROGRESS', 'DONE'
    module_type VARCHAR(10) NOT NULL DEFAULT 'BE', -- 'BE', 'FE'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bs_assignee FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_bs_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Bổ sung các trường xác nhận ghi đè vào bảng Yêu cầu tuyển dụng (recruitment_requisitions)
SET @col_exist_override = (
    SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() 
      AND TABLE_NAME = 'recruitment_requisitions' 
      AND COLUMN_NAME = 'is_budget_overridden'
);

SET @stmt_override = IF(@col_exist_override = 0,
    'ALTER TABLE recruitment_requisitions 
        ADD COLUMN is_budget_overridden BOOLEAN NOT NULL DEFAULT FALSE,
        ADD COLUMN budget_override_reason TEXT NULL,
        ADD COLUMN budget_overridden_by VARCHAR(36) NULL,
        ADD COLUMN budget_overridden_at TIMESTAMP NULL,
        ADD CONSTRAINT fk_req_overridden_by FOREIGN KEY (budget_overridden_by) REFERENCES users(id) ON DELETE SET NULL;',
    'SELECT 1;'
);
PREPARE stmt_exec FROM @stmt_override;
EXECUTE stmt_exec;
DEALLOCATE PREPARE stmt_exec;

-- 4. Bổ sung quyền hạn phân hệ Định biên & Ngân sách (Permissions)
INSERT IGNORE INTO permissions (id, code, name, module, action, description) VALUES
('p-037', 'budget.view', 'Xem định biên & ngân sách', 'budget', 'view', 'Xem danh sách và báo cáo định biên headcount và ngân sách nhân sự theo phòng ban'),
('p-038', 'budget.manage', 'Khai báo định biên & ngân sách', 'budget', 'manage', 'Khai báo và cập nhật chỉ tiêu định biên và ngân sách lương theo phòng ban'),
('p-039', 'budget.override', 'Xác nhận ghi đè vượt chỉ tiêu', 'budget', 'override', 'Xác nhận ghi đè khi tạo yêu cầu tuyển dụng vượt chỉ tiêu định biên hoặc ngân sách (Dành cho Trưởng phòng Nhân sự)');

-- Gán quyền cho ADMIN (role-001)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 'role-001', id FROM permissions WHERE code IN ('budget.view', 'budget.manage', 'budget.override');

-- Gán quyền cho HR_MANAGER (role-002)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 'role-002', id FROM permissions WHERE code IN ('budget.view', 'budget.manage', 'budget.override');

-- Gán quyền xem cho HIRING_MANAGER (role-004)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 'role-004', id FROM permissions WHERE code IN ('budget.view');

-- 5. Dữ liệu khởi tạo Subtasks (KN-195 -> KN-201) theo đúng tham chiếu
INSERT INTO budget_subtasks (id, code, title, priority, assignee_id, assignee_name, status, module_type) VALUES
('st-195', 'KN-195', '[BE] Tính toán headcount còn lại và kiểm tra vượt chỉ tiêu khi tạo yêu cầu tuyển dụng.', 'HIGH', 'usr-004', 'Phạm Trưởng Bộ Phận Kỹ Thuật', 'DONE', 'BE'),
('st-196', 'KN-196', '[BE] API tra cứu ngân sách còn lại theo phòng ban để phục vụ màn hình requisition.', 'MEDIUM', 'usr-004', 'Phạm Trưởng Bộ Phận Kỹ Thuật', 'DONE', 'BE'),
('st-197', 'KN-197', '[BE] Khai báo/cập nhật định biên và ngân sách nhân sự theo phòng ban.', 'HIGH', 'usr-002', 'Trần Thị Trưởng Phòng Nhân Sự', 'DONE', 'BE'),
('st-198', 'KN-198', '[BE] Thiết kế mô hình lưu chỉ tiêu headcount và ngân sách lương theo phòng ban/năm.', 'CRITICAL', 'usr-004', 'Phạm Trưởng Bộ Phận Kỹ Thuật', 'DONE', 'BE'),
('st-199', 'KN-199', '[BE] Cơ chế xác nhận ghi đè khi vượt chỉ tiêu bởi Trưởng phòng Nhân sự.', 'HIGH', 'usr-002', 'Trần Thị Trưởng Phòng Nhân Sự', 'DONE', 'BE'),
('st-200', 'KN-200', '[FE] Màn hình khai báo định biên và ngân sách nhân sự theo phòng ban.', 'HIGH', 'usr-003', 'Lê Chuyên Viên Tuyển Dụng', 'DONE', 'FE'),
('st-201', 'KN-201', '[FE] Hiển thị cảnh báo vượt chỉ tiêu và luồng xác nhận ghi đè trên form yêu cầu tuyển dụng.', 'CRITICAL', 'usr-003', 'Lê Chuyên Viên Tuyển Dụng', 'DONE', 'FE')
ON DUPLICATE KEY UPDATE 
    title = VALUES(title),
    priority = VALUES(priority),
    assignee_id = VALUES(assignee_id),
    assignee_name = VALUES(assignee_name),
    status = VALUES(status),
    module_type = VALUES(module_type);

-- 6. Khởi tạo Định biên & Ngân sách mẫu năm 2026 cho các phòng ban cốt lõi
INSERT INTO department_headcount_budgets (id, department_id, fiscal_year, target_headcount, total_salary_budget, currency, description, status, created_by) VALUES
('dhb-tech-2026', 'dept-003', 2026, 15, 650000000.00, 'VND', 'Định biên Khối Công nghệ Thông tin năm tài chính 2026', 'ACTIVE', 'usr-001'),
('dhb-hr-2026', 'dept-002', 2026, 8, 250000000.00, 'VND', 'Định biên Ban Nhân sự & Tuyển dụng năm 2026', 'ACTIVE', 'usr-001'),
('dhb-sales-2026', 'dept-004', 2026, 20, 500000000.00, 'VND', 'Định biên Khối Kinh doanh & Thị trường năm 2026', 'ACTIVE', 'usr-001'),
('dhb-mkt-2026', 'dept-005', 2026, 6, 180000000.00, 'VND', 'Định biên Phòng Truyền thông & Marketing năm 2026', 'ACTIVE', 'usr-001'),
('dhb-bgd-2026', 'dept-001', 2026, 5, 300000000.00, 'VND', 'Định biên Ban Giám đốc năm 2026', 'ACTIVE', 'usr-001')
ON DUPLICATE KEY UPDATE
    target_headcount = VALUES(target_headcount),
    total_salary_budget = VALUES(total_salary_budget),
    description = VALUES(description);

SET FOREIGN_KEY_CHECKS = 1;

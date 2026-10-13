-- ============================================================
-- IRMS - Ma trận phê duyệt đa cấp & Phân cấp phê duyệt lương
-- Migration Script: database/migrations/20261012_approval_matrix.sql
-- ============================================================

USE test;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Bảng Cấu hình Ma trận phê duyệt (approval_matrices)
CREATE TABLE IF NOT EXISTS approval_matrices (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    department_id VARCHAR(36) NULL,
    salary_condition VARCHAR(50) NOT NULL DEFAULT 'RANGE', -- 'RANGE', 'WITHIN_STANDARD', 'EXCEED_STANDARD'
    min_salary DECIMAL(15, 2) NULL,
    max_salary DECIMAL(15, 2) NULL,
    threshold_amount DECIMAL(15, 2) NULL,
    priority INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'INACTIVE'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36) NULL,
    updated_by VARCHAR(36) NULL,
    CONSTRAINT fk_apm_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,
    CONSTRAINT fk_apm_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_apm_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_apm_dept (department_id),
    INDEX idx_apm_status (status),
    INDEX idx_apm_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bảng Các cấp phê duyệt trong ma trận (approval_matrix_steps)
CREATE TABLE IF NOT EXISTS approval_matrix_steps (
    id VARCHAR(36) PRIMARY KEY,
    matrix_id VARCHAR(36) NOT NULL,
    step_order INT NOT NULL,
    step_name VARCHAR(255) NOT NULL,
    approver_type VARCHAR(50) NOT NULL, -- 'DEPT_MANAGER', 'ROLE', 'SPECIFIC_USER'
    approver_role_id VARCHAR(36) NULL,
    approver_user_id VARCHAR(36) NULL,
    is_escalation_step BOOLEAN NOT NULL DEFAULT FALSE,
    escalation_condition VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_apms_matrix FOREIGN KEY (matrix_id) REFERENCES approval_matrices(id) ON DELETE CASCADE,
    CONSTRAINT fk_apms_role FOREIGN KEY (approver_role_id) REFERENCES roles(id) ON DELETE SET NULL,
    CONSTRAINT fk_apms_user FOREIGN KEY (approver_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_apms_matrix (matrix_id),
    INDEX idx_apms_order (matrix_id, step_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Bảng Lưu vết chuỗi phê duyệt của từng yêu cầu tuyển dụng (Snapshot & Running Workflow)
CREATE TABLE IF NOT EXISTS requisition_approval_steps (
    id VARCHAR(36) PRIMARY KEY,
    requisition_id VARCHAR(36) NOT NULL,
    matrix_id VARCHAR(36) NULL,
    step_order INT NOT NULL,
    step_name VARCHAR(255) NOT NULL,
    approver_type VARCHAR(50) NOT NULL,
    approver_role_id VARCHAR(36) NULL,
    approver_user_id VARCHAR(36) NULL,
    assigned_approver_name VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED'
    comment TEXT NULL,
    acted_by VARCHAR(36) NULL,
    acted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ras_req FOREIGN KEY (requisition_id) REFERENCES recruitment_requisitions(id) ON DELETE CASCADE,
    CONSTRAINT fk_ras_acted_by FOREIGN KEY (acted_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_ras_req (requisition_id),
    INDEX idx_ras_status (requisition_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Bổ sung quyền hạn Ma trận phê duyệt vào bảng permissions
INSERT IGNORE INTO permissions (id, code, name, module, action, description) VALUES
('p-apm-view', 'approval_matrix.view', 'Xem ma trận phê duyệt', 'approval_matrix', 'view', 'Xem danh sách và chi tiết các quy tắc ma trận phê duyệt'),
('p-apm-manage', 'approval_matrix.manage', 'Quản lý ma trận phê duyệt', 'approval_matrix', 'manage', 'Thêm, sửa, đổi trạng thái và cấu hình các cấp phê duyệt');

-- Gán quyền cho ADMIN và HR_MANAGER
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 'role-001', id FROM permissions WHERE code LIKE 'approval_matrix.%';

INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 'role-002', id FROM permissions WHERE code LIKE 'approval_matrix.%';

-- 5. Seed dữ liệu mẫu thực tế cho Ma trận phê duyệt
-- Ma trận 1: Khối Công nghệ thông tin (TECH - dept-003)
INSERT IGNORE INTO approval_matrices (id, code, name, description, department_id, salary_condition, min_salary, max_salary, threshold_amount, priority, status)
VALUES ('apm-001', 'APM-TECH', 'Ma trận phê duyệt Khối Công nghệ Thông tin', 'Áp dụng cho mọi vị trí công nghệ: Cấp 1 Trưởng bộ phận, tự động bổ sung Cấp 2 Giám đốc Khối khi lương > 35M', 'dept-003', 'RANGE', 0.00, 70000000.00, 35000000.00, 10, 'ACTIVE');

INSERT IGNORE INTO approval_matrix_steps (id, matrix_id, step_order, step_name, approver_type, approver_role_id, approver_user_id, is_escalation_step, escalation_condition)
VALUES 
('step-001-1', 'apm-001', 1, 'Trưởng bộ phận Kỹ thuật', 'DEPT_MANAGER', NULL, NULL, FALSE, NULL),
('step-001-2', 'apm-001', 2, 'Giám đốc Khối CNTT', 'ROLE', 'role-001', NULL, TRUE, 'Tự động bổ sung khi mức lương đề xuất vượt hạn mức 35,000,000 VNĐ');

-- Ma trận 2: Khối Kinh doanh & Thị trường (SALES - dept-004)
INSERT IGNORE INTO approval_matrices (id, code, name, description, department_id, salary_condition, min_salary, max_salary, threshold_amount, priority, status)
VALUES ('apm-002', 'APM-SALES', 'Ma trận phê duyệt Khối Kinh doanh & Thị trường', 'Áp dụng cho bộ phận kinh doanh: Cấp 1 Trưởng phòng kinh doanh, tự động bổ sung Cấp 2 Ban Giám đốc khi lương > 30M', 'dept-004', 'RANGE', 0.00, 60000000.00, 30000000.00, 10, 'ACTIVE');

INSERT IGNORE INTO approval_matrix_steps (id, matrix_id, step_order, step_name, approver_type, approver_role_id, approver_user_id, is_escalation_step, escalation_condition)
VALUES 
('step-002-1', 'apm-002', 1, 'Trưởng phòng Kinh doanh', 'DEPT_MANAGER', NULL, NULL, FALSE, NULL),
('step-002-2', 'apm-002', 2, 'Ban Giám đốc Điều hành', 'ROLE', 'role-001', NULL, TRUE, 'Tự động bổ sung khi mức lương đề xuất vượt hạn mức 30,000,000 VNĐ');

-- Ma trận 3: Quy định chung toàn công ty (Toàn bộ phòng ban khác)
INSERT IGNORE INTO approval_matrices (id, code, name, description, department_id, salary_condition, min_salary, max_salary, threshold_amount, priority, status)
VALUES ('apm-003', 'APM-GENERAL', 'Ma trận phê duyệt tiêu chuẩn toàn Công ty', 'Quy tắc phê duyệt chung: Cấp 1 Trưởng phòng, Cấp 2 Trưởng phòng Nhân sự, tự động bổ sung Cấp 3 Tổng Giám đốc khi lương > 25M', NULL, 'RANGE', 0.00, 50000000.00, 25000000.00, 1, 'ACTIVE');

INSERT IGNORE INTO approval_matrix_steps (id, matrix_id, step_order, step_name, approver_type, approver_role_id, approver_user_id, is_escalation_step, escalation_condition)
VALUES 
('step-003-1', 'apm-003', 1, 'Trưởng phòng ban của yêu cầu', 'DEPT_MANAGER', NULL, NULL, FALSE, NULL),
('step-003-2', 'apm-003', 2, 'Trưởng phòng Nhân sự', 'ROLE', 'role-002', NULL, FALSE, NULL),
('step-003-3', 'apm-003', 3, 'Ban Tổng Giám đốc', 'ROLE', 'role-001', NULL, TRUE, 'Tự động bổ sung khi mức lương đề xuất vượt hạn mức 25,000,000 VNĐ');

SET FOREIGN_KEY_CHECKS = 1;

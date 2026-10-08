-- ============================================================
-- IRMS - Khai báo phòng ban (Department Hierarchy & Management)
-- Migration Script: database/migrations/20261009_department_hierarchy.sql
-- ============================================================

USE test;

SET FOREIGN_KEY_CHECKS = 0;

-- 1. Mở rộng bảng departments để hỗ trợ cây nhiều cấp và người phụ trách
-- Cột parent_id: Phòng ban cha (cấu trúc cây nhiều cấp)
SET @col_parent = (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'departments' AND COLUMN_NAME = 'parent_id');
SET @sql_parent = IF(@col_parent = 0, 'ALTER TABLE departments ADD COLUMN parent_id VARCHAR(36) NULL AFTER description', 'SELECT 1');
PREPARE stmt_parent FROM @sql_parent;
EXECUTE stmt_parent;
DEALLOCATE PREPARE stmt_parent;

-- Cột manager_id: Người phụ trách phòng ban (FK trỏ tới users)
SET @col_mgr = (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'departments' AND COLUMN_NAME = 'manager_id');
SET @sql_mgr = IF(@col_mgr = 0, 'ALTER TABLE departments ADD COLUMN manager_id VARCHAR(36) NULL AFTER parent_id', 'SELECT 1');
PREPARE stmt_mgr FROM @sql_mgr;
EXECUTE stmt_mgr;
DEALLOCATE PREPARE stmt_mgr;

-- Cột status: Trạng thái áp dụng ('ACTIVE', 'INACTIVE')
SET @col_status = (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'departments' AND COLUMN_NAME = 'status');
SET @sql_status = IF(@col_status = 0, "ALTER TABLE departments ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER manager_id", 'SELECT 1');
PREPARE stmt_status FROM @sql_status;
EXECUTE stmt_status;
DEALLOCATE PREPARE stmt_status;

-- Cột created_by & updated_by
SET @col_cb = (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'departments' AND COLUMN_NAME = 'created_by');
SET @sql_cb = IF(@col_cb = 0, 'ALTER TABLE departments ADD COLUMN created_by VARCHAR(36) NULL AFTER updated_at', 'SELECT 1');
PREPARE stmt_cb FROM @sql_cb;
EXECUTE stmt_cb;
DEALLOCATE PREPARE stmt_cb;

SET @col_ub = (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'departments' AND COLUMN_NAME = 'updated_by');
SET @sql_ub = IF(@col_ub = 0, 'ALTER TABLE departments ADD COLUMN updated_by VARCHAR(36) NULL AFTER created_by', 'SELECT 1');
PREPARE stmt_ub FROM @sql_ub;
EXECUTE stmt_ub;
DEALLOCATE PREPARE stmt_ub;

-- Tạo index hỗ trợ tìm kiếm và duyệt cây
CREATE INDEX idx_dept_parent ON departments(parent_id);
CREATE INDEX idx_dept_manager ON departments(manager_id);
CREATE INDEX idx_dept_status ON departments(status);

-- 2. Bổ sung quyền hạn quản lý phòng ban vào hệ thống permissions
INSERT INTO permissions (id, code, name, module, action, description) VALUES
('p-dept-view', 'departments.view', 'Xem cơ cấu phòng ban', 'departments', 'view', 'Xem danh sách và sơ đồ cây cơ cấu tổ chức phòng ban'),
('p-dept-create', 'departments.create', 'Tạo mới phòng ban', 'departments', 'create', 'Thêm mới phòng ban và đơn vị trực thuộc'),
('p-dept-update', 'departments.update', 'Chỉnh sửa phòng ban', 'departments', 'update', 'Cập nhật thông tin, thay đổi cấp bậc và người phụ trách phòng ban'),
('p-dept-delete', 'departments.delete', 'Xóa phòng ban', 'departments', 'delete', 'Xóa phòng ban khi không có ràng buộc yêu cầu tuyển dụng mở hoặc con'),
('p-dept-status', 'departments.status', 'Đổi trạng thái phòng ban', 'departments', 'status', 'Kích hoạt hoặc ngừng áp dụng phòng ban')
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

-- Gán quyền cho vai trò ADMIN và HR_MANAGER
INSERT IGNORE INTO role_permissions (role_id, permission_id) VALUES
('role-001', 'p-dept-view'),
('role-001', 'p-dept-create'),
('role-001', 'p-dept-update'),
('role-001', 'p-dept-delete'),
('role-001', 'p-dept-status'),
('role-002', 'p-dept-view'),
('role-002', 'p-dept-create'),
('role-002', 'p-dept-update'),
('role-002', 'p-dept-delete'),
('role-002', 'p-dept-status');

-- 3. Cập nhật dữ liệu mẫu phân cấp cây nhiều cấp (Hierarchical Departments)
-- Cấp 1: Ban Giám đốc (Root)
UPDATE departments SET parent_id = NULL, manager_id = 'u-admin', status = 'ACTIVE' WHERE id = 'dept-001';

-- Cấp 2: Khối trực thuộc Ban Giám đốc
UPDATE departments SET parent_id = 'dept-001', manager_id = 'u-hr', status = 'ACTIVE' WHERE id = 'dept-002'; -- HR
UPDATE departments SET parent_id = 'dept-001', manager_id = 'u-admin', status = 'ACTIVE' WHERE id = 'dept-003'; -- TECH
UPDATE departments SET parent_id = 'dept-001', manager_id = 'u-admin', status = 'ACTIVE' WHERE id = 'dept-004'; -- SALES
UPDATE departments SET parent_id = 'dept-001', manager_id = 'u-admin', status = 'ACTIVE' WHERE id = 'dept-005'; -- MKT

-- Cấp 3: Các phòng ban con trực thuộc Khối Công nghệ (TECH)
INSERT INTO departments (id, code, name, description, parent_id, manager_id, status) VALUES
('dept-006', 'TECH-DEV', 'Phòng Phát triển phần mềm', 'Nghiên cứu và phát triển các sản phẩm phần mềm nội bộ', 'dept-003', 'u-admin', 'ACTIVE'),
('dept-007', 'TECH-QA', 'Phòng Kiểm thử & Đảm bảo chất lượng', 'Kiểm thử chức năng, tự động hóa và đảm bảo chất lượng phần mềm', 'dept-003', 'u-admin', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), parent_id = VALUES(parent_id);

-- Cấp 3: Các phòng ban con trực thuộc Khối Kinh doanh (SALES)
INSERT INTO departments (id, code, name, description, parent_id, manager_id, status) VALUES
('dept-008', 'SALES-MB', 'Phòng Kinh doanh miền Bắc', 'Phụ trách doanh số và khách hàng khu vực miền Bắc', 'dept-004', 'u-admin', 'ACTIVE'),
('dept-009', 'SALES-MN', 'Phòng Kinh doanh miền Nam', 'Phụ trách thị trường và đối tác chiến lược miền Nam', 'dept-004', 'u-admin', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), parent_id = VALUES(parent_id);

-- Cấp 3: Phòng ban con trực thuộc Ban Nhân sự (HR)
INSERT INTO departments (id, code, name, description, parent_id, manager_id, status) VALUES
('dept-010', 'HR-REC', 'Phòng Tuyển dụng & Thu hút nhân tài', 'Chuyên trách tìm nguồn, sàn lọc và tiếp nhận nhân sự', 'dept-002', 'u-hr', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), parent_id = VALUES(parent_id);

SET FOREIGN_KEY_CHECKS = 1;

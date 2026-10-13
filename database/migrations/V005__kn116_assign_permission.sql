-- ============================================================
-- KN-116: Thêm quyền Phân bổ Hồ sơ Tuyển dụng
-- Migration: V005__kn116_assign_permission.sql
-- Áp dụng cho DB đang chạy (không cần chạy lại seed.sql)
-- ============================================================

USE test;

-- Thêm quyền requisitions.assign nếu chưa tồn tại
INSERT IGNORE INTO permissions (id, code, name, module, action, description)
VALUES ('p-036', 'requisitions.assign',
        'Phân bổ Recruiter cho vị trí',
        'requisitions', 'assign',
        'Gán recruiter chính và recruiter hỗ trợ cho từng vị trí tuyển dụng, ghi lịch sử chuyển giao (KN-116)');

-- Gán quyền này cho ADMIN (role-001) nếu chưa có
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 'role-001', id FROM permissions WHERE code = 'requisitions.assign';

-- Gán quyền này cho HR_MANAGER (role-002) nếu chưa có
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 'role-002', id FROM permissions WHERE code = 'requisitions.assign';

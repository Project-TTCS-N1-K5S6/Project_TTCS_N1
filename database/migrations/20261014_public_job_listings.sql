-- ==============================================================================
-- Migration: Chuẩn hóa dữ liệu và Tối ưu hóa tính năng "Xem danh sách việc làm công khai"
-- Ticket: KN-220, KN-219, KN-222, KN-223
-- ==============================================================================

USE test;

-- 1. Bổ sung các cột chuẩn hóa cho bảng job_postings nếu chưa có
ALTER TABLE job_postings ADD COLUMN IF NOT EXISTS expired_at TIMESTAMP NULL AFTER published_at;
ALTER TABLE job_postings ADD COLUMN IF NOT EXISTS department_id VARCHAR(36) NULL AFTER requisition_id;
ALTER TABLE job_postings ADD COLUMN IF NOT EXISTS location_id VARCHAR(36) NULL AFTER work_location_id;
ALTER TABLE job_postings ADD COLUMN IF NOT EXISTS work_type VARCHAR(100) NULL AFTER work_mode;
ALTER TABLE job_postings ADD COLUMN IF NOT EXISTS description MEDIUMTEXT NULL AFTER title;

-- 2. Đồng bộ dữ liệu cũ (Backfill)
-- Đồng bộ department_id từ recruitment_requisitions
UPDATE job_postings jp
JOIN recruitment_requisitions r ON jp.requisition_id = r.id
SET jp.department_id = r.department_id
WHERE jp.department_id IS NULL AND r.department_id IS NOT NULL;

-- Đồng bộ location_id từ work_location_id
UPDATE job_postings
SET location_id = work_location_id
WHERE location_id IS NULL AND work_location_id IS NOT NULL;

-- Đồng bộ work_type từ work_mode
UPDATE job_postings
SET work_type = work_mode
WHERE work_type IS NULL AND work_mode IS NOT NULL;

-- Đồng bộ description từ job_description
UPDATE job_postings
SET description = job_description
WHERE description IS NULL AND job_description IS NOT NULL;

-- Đồng bộ expired_at từ application_deadline (cuối ngày 23:59:59)
UPDATE job_postings
SET expired_at = TIMESTAMP(CONCAT(application_deadline, ' 23:59:59'))
WHERE expired_at IS NULL AND application_deadline IS NOT NULL;

-- 3. Tạo chỉ mục (Indexes) tối ưu hóa truy vấn công khai
-- Chỉ mục trên status
CREATE INDEX IF NOT EXISTS idx_jp_status ON job_postings(status);

-- Chỉ mục trên expired_at
CREATE INDEX IF NOT EXISTS idx_jp_expired_at ON job_postings(expired_at);

-- Chỉ mục trên department_id
CREATE INDEX IF NOT EXISTS idx_jp_dept_id ON job_postings(department_id);

-- Chỉ mục trên location_id
CREATE INDEX IF NOT EXISTS idx_jp_loc_id ON job_postings(location_id);

-- Chỉ mục trên work_type
CREATE INDEX IF NOT EXISTS idx_jp_work_type ON job_postings(work_type);

-- Chỉ mục tổ hợp cho điều kiện lọc tin hợp lệ (status + expired_at)
CREATE INDEX IF NOT EXISTS idx_jp_pub_exp ON job_postings(status, expired_at);

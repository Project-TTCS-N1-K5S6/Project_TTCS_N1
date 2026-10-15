-- ==============================================================================
-- Migration: Bổ sung tính năng "Trích xuất thông tin tự động từ CV (KN-213)"
-- ==============================================================================
-- 1. Bóc tách họ tên, email, số điện thoại từ CV dạng PDF
-- 2. Kết quả bóc tách hiển thị để recruiter xác nhận hoặc sửa, không tự ghi đè
-- 3. Bóc tách thất bại không làm hỏng việc nộp hồ sơ
-- ==============================================================================

USE test;

-- 1. Thêm các cột lưu trữ kết quả bóc tách tự động từ CV (độc lập với thông tin nộp của ứng viên)
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS extracted_name VARCHAR(255) NULL AFTER cv_url;
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS extracted_email VARCHAR(255) NULL AFTER extracted_name;
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS extracted_phone VARCHAR(50) NULL AFTER extracted_email;
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS extraction_status VARCHAR(50) DEFAULT 'NONE' AFTER extracted_phone; -- 'SUCCESS', 'PARTIAL', 'FAILED', 'CONFIRMED', 'NONE'
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS cv_extracted_at TIMESTAMP NULL AFTER extraction_status;

-- 2. Chỉ mục tối ưu tra cứu trạng thái bóc tách CV
CREATE INDEX IF NOT EXISTS idx_cand_extract_status ON candidates(extraction_status);

-- ==============================================================================
-- Migration: Bổ sung tính năng "Nộp hồ sơ ứng tuyển kèm CV"
-- Ticket: KN-230, KN-231, KN-232, KN-233, KN-234, KN-235, KN-236
-- ==============================================================================

USE test;

-- 1. Bổ sung các cột phục vụ nộp hồ sơ công khai & tra cứu
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS tracking_code VARCHAR(50) NULL UNIQUE AFTER status;
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS cover_letter MEDIUMTEXT NULL AFTER notes;
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS ip_address VARCHAR(45) NULL AFTER cover_letter;

-- 2. Tạo chỉ mục tổ hợp để kiểm tra trùng lặp email theo từng vị trí (KN-231)
CREATE INDEX IF NOT EXISTS idx_cand_email_req ON candidates(email, requisition_id);

-- 3. Tạo chỉ mục trên tracking_code để tra cứu nhanh hồ sơ (KN-233)
CREATE INDEX IF NOT EXISTS idx_cand_tracking ON candidates(tracking_code);

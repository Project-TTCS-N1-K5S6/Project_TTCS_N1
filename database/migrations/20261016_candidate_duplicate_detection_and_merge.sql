-- ==============================================================================
-- Migration: Cảnh báo và phát hiện hồ sơ trùng lặp & Hợp nhất hồ sơ ứng viên
-- File: database/migrations/20261016_candidate_duplicate_detection_and_merge.sql
-- ==============================================================================

USE test;

-- 1. Bổ sung các cột vào bảng candidates để hỗ trợ hợp nhất và chuẩn hóa số điện thoại
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS merged_into_id VARCHAR(36) NULL AFTER status;
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS is_merged BOOLEAN DEFAULT FALSE AFTER merged_into_id;
ALTER TABLE candidates ADD COLUMN IF NOT EXISTS normalized_phone VARCHAR(50) NULL AFTER phone;

-- Chuẩn hóa số điện thoại hiện có trong bảng candidates
UPDATE candidates
SET normalized_phone = REGEXP_REPLACE(phone, '[^0-9]', '')
WHERE phone IS NOT NULL AND (normalized_phone IS NULL OR normalized_phone = '');

-- Chỉ mục cho candidates
CREATE INDEX IF NOT EXISTS idx_cand_merged_into ON candidates(merged_into_id);
CREATE INDEX IF NOT EXISTS idx_cand_is_merged ON candidates(is_merged);
CREATE INDEX IF NOT EXISTS idx_cand_norm_phone ON candidates(normalized_phone);

-- 2. Bảng cảnh báo hồ sơ nghi trùng lặp (candidate_duplicate_warnings)
CREATE TABLE IF NOT EXISTS candidate_duplicate_warnings (
    id VARCHAR(36) PRIMARY KEY,
    candidate_id_1 VARCHAR(36) NOT NULL,
    candidate_id_2 VARCHAR(36) NOT NULL,
    match_type VARCHAR(20) NOT NULL, -- 'EMAIL', 'PHONE', 'BOTH'
    match_value VARCHAR(255) NOT NULL, -- Giá trị trùng (email hoặc số điện thoại)
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'DISMISSED', 'MERGED', 'SNOOZED'
    snooze_until TIMESTAMP NULL, -- Thời điểm nhắc lại cảnh báo
    reminder_count INT NOT NULL DEFAULT 0, -- Số lần nhắc lại
    reminder_interval_days INT NOT NULL DEFAULT 3, -- Chu kỳ nhắc lại theo cấu hình (mặc định 3 ngày)
    notes TEXT NULL,
    resolved_by VARCHAR(36) NULL,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cdw_cand1 FOREIGN KEY (candidate_id_1) REFERENCES candidates(id) ON DELETE CASCADE,
    CONSTRAINT fk_cdw_cand2 FOREIGN KEY (candidate_id_2) REFERENCES candidates(id) ON DELETE CASCADE,
    CONSTRAINT fk_cdw_resolver FOREIGN KEY (resolved_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_cdw_pair UNIQUE (candidate_id_1, candidate_id_2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_cdw_status ON candidate_duplicate_warnings(status);
CREATE INDEX IF NOT EXISTS idx_cdw_snooze ON candidate_duplicate_warnings(snooze_until);
CREATE INDEX IF NOT EXISTS idx_cdw_cand1 ON candidate_duplicate_warnings(candidate_id_1);
CREATE INDEX IF NOT EXISTS idx_cdw_cand2 ON candidate_duplicate_warnings(candidate_id_2);

-- 3. Bảng lịch sử hợp nhất hồ sơ (candidate_merge_history)
CREATE TABLE IF NOT EXISTS candidate_merge_history (
    id VARCHAR(36) PRIMARY KEY,
    warning_id VARCHAR(36) NULL,
    primary_candidate_id VARCHAR(36) NOT NULL,
    secondary_candidate_id VARCHAR(36) NOT NULL,
    primary_candidate_name VARCHAR(255) NOT NULL,
    secondary_candidate_name VARCHAR(255) NOT NULL,
    merged_by_user_id VARCHAR(36) NULL,
    merged_by_user_name VARCHAR(255) NULL,
    field_selection_details LONGTEXT NULL, -- JSON cấu hình các trường được giữ lại
    merged_snapshot LONGTEXT NULL, -- JSON snapshot đầy đủ dữ liệu cả 2 hồ sơ trước khi gộp
    merge_notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cmh_warning FOREIGN KEY (warning_id) REFERENCES candidate_duplicate_warnings(id) ON DELETE SET NULL,
    CONSTRAINT fk_cmh_primary FOREIGN KEY (primary_candidate_id) REFERENCES candidates(id) ON DELETE CASCADE,
    CONSTRAINT fk_cmh_user FOREIGN KEY (merged_by_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_cmh_primary ON candidate_merge_history(primary_candidate_id);
CREATE INDEX IF NOT EXISTS idx_cmh_secondary ON candidate_merge_history(secondary_candidate_id);
CREATE INDEX IF NOT EXISTS idx_cmh_created ON candidate_merge_history(created_at);

-- 4. Bảng bảo toàn toàn bộ lịch sử ứng tuyển, phỏng vấn, đánh giá, ghi chú và hoạt động (candidate_activities)
CREATE TABLE IF NOT EXISTS candidate_activities (
    id VARCHAR(36) PRIMARY KEY,
    candidate_id VARCHAR(36) NOT NULL,
    activity_type VARCHAR(50) NOT NULL, -- 'APPLICATION', 'INTERVIEW', 'ASSESSMENT', 'NOTE', 'STATUS_CHANGE', 'MERGE', 'WARNING'
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    performed_by VARCHAR(36) NULL,
    performed_by_name VARCHAR(255) NULL,
    original_candidate_id VARCHAR(36) NULL, -- Lưu vết nếu hoạt động này chuyển từ hồ sơ khác sang khi hợp nhất
    metadata_json LONGTEXT NULL,
    activity_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ca_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE,
    CONSTRAINT fk_ca_user FOREIGN KEY (performed_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_ca_candidate ON candidate_activities(candidate_id);
CREATE INDEX IF NOT EXISTS idx_ca_type ON candidate_activities(activity_type);
CREATE INDEX IF NOT EXISTS idx_ca_date ON candidate_activities(activity_date);

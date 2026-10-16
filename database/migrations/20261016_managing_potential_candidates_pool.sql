-- ==============================================================================
-- MIGRATION: QUẢN LÝ KHO ỨNG VIÊN TIỀM NĂNG (MANAGING A POOL OF POTENTIAL CANDIDATES)
-- Bổ sung các trường lưu trữ trạng thái Kho tài năng (Talent Pool), phân loại kỹ năng,
-- đánh giá tiềm năng và định hướng vị trí/phòng ban phù hợp trong tương lai.
-- ==============================================================================

USE ttcs_db;

-- 1. Thêm các cột quản lý Kho ứng viên tiềm năng vào bảng candidates
ALTER TABLE candidates
    ADD COLUMN is_potential BOOLEAN NOT NULL DEFAULT FALSE AFTER status,
    ADD COLUMN potential_rating INT NOT NULL DEFAULT 3 AFTER is_potential,
    ADD COLUMN potential_skills VARCHAR(500) NULL AFTER potential_rating,
    ADD COLUMN potential_notes TEXT NULL AFTER potential_skills,
    ADD COLUMN target_department_id VARCHAR(36) NULL AFTER potential_notes,
    ADD COLUMN target_position VARCHAR(255) NULL AFTER target_department_id,
    ADD COLUMN added_to_pool_at TIMESTAMP NULL AFTER target_position,
    ADD COLUMN added_to_pool_by VARCHAR(36) NULL AFTER added_to_pool_at;

-- 2. Khóa ngoại liên kết phòng ban mục tiêu và người đưa vào kho tiềm năng
ALTER TABLE candidates
    ADD CONSTRAINT fk_cand_target_dept
        FOREIGN KEY (target_department_id) REFERENCES departments(id) ON DELETE SET NULL,
    ADD CONSTRAINT fk_cand_pool_by
        FOREIGN KEY (added_to_pool_by) REFERENCES users(id) ON DELETE SET NULL;

-- 3. Chỉ mục tối ưu hóa tốc độ tìm kiếm và lọc trong Kho ứng viên tiềm năng
CREATE INDEX idx_cand_potential ON candidates(is_potential, potential_rating);
CREATE INDEX idx_cand_target_dept ON candidates(target_department_id);

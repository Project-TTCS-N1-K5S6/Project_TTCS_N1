-- ============================================================
-- MIGRATION: Chấm điểm & Xếp hạng hồ sơ tự động (CV Scoring & Ranking)
-- Ngày: 2026-10-16
-- Mô tả: Thêm bảng tiêu chí sàng lọc và các cột điểm phù hợp vào bảng candidates
-- ============================================================

USE test;
SET FOREIGN_KEY_CHECKS = 0;

-- Bảng tiêu chí sàng lọc theo vị trí tuyển dụng (job_screening_criteria)
CREATE TABLE IF NOT EXISTS job_screening_criteria (
    id VARCHAR(36) PRIMARY KEY,
    requisition_id VARCHAR(36) NOT NULL COMMENT 'Liên kết vị trí tuyển dụng',
    min_experience_years INT NOT NULL DEFAULT 0 COMMENT 'Số năm kinh nghiệm tối thiểu bắt buộc',
    required_degree VARCHAR(100) NULL COMMENT 'Bằng cấp bắt buộc (VD: Cử nhân, Thạc sĩ)',
    must_have_skills TEXT NULL COMMENT 'Kỹ năng bắt buộc, phân cách bằng dấu phẩy',
    nice_to_have_skills TEXT NULL COMMENT 'Kỹ năng cộng điểm, phân cách bằng dấu phẩy',
    weight_experience INT NOT NULL DEFAULT 40 COMMENT 'Trọng số điểm Kinh nghiệm (max 40)',
    weight_skills INT NOT NULL DEFAULT 40 COMMENT 'Trọng số điểm Kỹ năng (max 40)',
    weight_education INT NOT NULL DEFAULT 20 COMMENT 'Trọng số điểm Bằng cấp (max 20)',
    created_by VARCHAR(36) NULL,
    created_by_name VARCHAR(255) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_jsc_requisition FOREIGN KEY (requisition_id) REFERENCES recruitment_requisitions(id) ON DELETE CASCADE,
    CONSTRAINT fk_jsc_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE KEY uk_jsc_requisition (requisition_id),
    INDEX idx_jsc_requisition (requisition_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tiêu chí sàng lọc và chấm điểm hồ sơ tự động theo vị trí tuyển dụng';

-- Thêm cột điểm phù hợp và cờ trạng thái bắt buộc vào bảng candidates
ALTER TABLE candidates
    ADD COLUMN IF NOT EXISTS match_score INT DEFAULT NULL COMMENT 'Điểm phù hợp (0-100), NULL = chưa chấm',
    ADD COLUMN IF NOT EXISTS mandatory_flag VARCHAR(20) DEFAULT NULL COMMENT 'Cờ tiêu chí bắt buộc: PASS | FAIL | NULL=chưa đánh giá',
    ADD COLUMN IF NOT EXISTS score_breakdown JSON DEFAULT NULL COMMENT 'Chi tiết điểm từng hạng mục (JSON)',
    ADD COLUMN IF NOT EXISTS scored_at TIMESTAMP NULL COMMENT 'Thời điểm chấm điểm gần nhất',
    ADD COLUMN IF NOT EXISTS scored_by VARCHAR(36) NULL COMMENT 'Người hoặc hệ thống thực hiện chấm điểm';

-- Index hỗ trợ sắp xếp theo điểm
CREATE INDEX IF NOT EXISTS idx_cand_match_score ON candidates(match_score);
CREATE INDEX IF NOT EXISTS idx_cand_mandatory_flag ON candidates(mandatory_flag);

SET FOREIGN_KEY_CHECKS = 1;

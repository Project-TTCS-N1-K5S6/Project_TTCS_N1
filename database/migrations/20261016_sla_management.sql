-- ============================================================
-- Migration: 20261016_sla_management.sql
-- Quản lý cấu hình ngưỡng SLA, Cảnh báo & Nhắc việc quá hạn
-- ============================================================

USE test;

-- 1. Bảng cấu hình ngưỡng SLA theo từng giai đoạn (KN-364)
CREATE TABLE IF NOT EXISTS sla_configurations (
    id VARCHAR(36) PRIMARY KEY,
    stage_code VARCHAR(50) NOT NULL UNIQUE, -- APPLIED, SCREENING, INTERVIEWING, OFFER
    stage_name VARCHAR(100) NOT NULL,
    sla_days INT NOT NULL DEFAULT 3, -- Số ngày tối đa cho phép ở giai đoạn này
    warning_threshold_days INT NOT NULL DEFAULT 1, -- Cảnh báo trước khi hết hạn (ngày)
    action_required VARCHAR(255) NULL, -- Yêu cầu hành động
    is_active BOOLEAN NOT NULL DEFAULT TRUE, -- Kích hoạt cấu hình
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(36) NULL,
    CONSTRAINT fk_sla_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_sla_stage (stage_code),
    INDEX idx_sla_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Nạp cấu hình mặc định ban đầu nếu chưa có
INSERT INTO sla_configurations (id, stage_code, stage_name, sla_days, warning_threshold_days, action_required, is_active)
VALUES 
    ('sla-001', 'APPLIED', 'Mới ứng tuyển', 3, 1, 'Sàng lọc hồ sơ ứng viên ban đầu và phản hồi/chuyển vòng', TRUE),
    ('sla-002', 'SCREENING', 'Sàng lọc CV', 5, 2, 'Đánh giá chi tiết kinh nghiệm, kỹ năng và hẹn lịch phỏng vấn', TRUE),
    ('sla-003', 'INTERVIEWING', 'Phỏng vấn', 7, 2, 'Hoàn thành buổi phỏng vấn chuyên môn và nhập kết quả đánh giá', TRUE),
    ('sla-004', 'OFFER', 'Đề nghị tuyển dụng (Offer)', 4, 1, 'Thương lượng đãi ngộ và xác nhận phản hồi nhận việc của ứng viên', TRUE)
ON DUPLICATE KEY UPDATE 
    stage_name = VALUES(stage_name),
    action_required = VALUES(action_required);

-- 2. Thêm cột gắn cờ và thời điểm phát hiện quá hạn vào bảng candidates (KN-363)
-- (Sử dụng cú pháp kiểm tra an toàn)
ALTER TABLE candidates 
    ADD COLUMN IF NOT EXISTS is_sla_overdue BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS sla_overdue_detected_at TIMESTAMP NULL,
    ADD COLUMN IF NOT EXISTS stage_entered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Cập nhật stage_entered_at cho các hồ sơ hiện có nếu đang null
UPDATE candidates SET stage_entered_at = COALESCE(created_at, CURRENT_TIMESTAMP) WHERE stage_entered_at IS NULL;

-- Tạo chỉ mục tìm kiếm nhanh hồ sơ quá hạn SLA
CREATE INDEX idx_cand_sla_overdue ON candidates(is_sla_overdue);
CREATE INDEX idx_cand_stage_entered ON candidates(stage_entered_at);

-- 3. Bảng Thông báo nội bộ cho Recruiter / Quản trị viên (KN-365)
CREATE TABLE IF NOT EXISTS in_app_notifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'SLA_OVERDUE', -- 'SLA_OVERDUE', 'SYSTEM', 'REMINDER'
    reference_id VARCHAR(36) NULL, -- ID ứng viên hoặc ID liên quan
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_user_read (user_id, is_read),
    INDEX idx_notif_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Bảng Nhật ký cảnh báo SLA & Lần gửi nhắc việc (KN-367, KN-368)
CREATE TABLE IF NOT EXISTS sla_reminder_logs (
    id VARCHAR(36) PRIMARY KEY,
    candidate_id VARCHAR(36) NOT NULL,
    candidate_name VARCHAR(255) NOT NULL,
    requisition_id VARCHAR(36) NULL,
    requisition_title VARCHAR(255) NULL,
    stage_code VARCHAR(50) NOT NULL,
    sla_days_threshold INT NOT NULL,
    days_in_stage INT NOT NULL,
    recruiter_id VARCHAR(36) NULL,
    recruiter_name VARCHAR(255) NULL,
    recruiter_email VARCHAR(255) NULL,
    channel VARCHAR(50) NOT NULL DEFAULT 'IN_APP_AND_EMAIL', -- 'IN_APP', 'EMAIL', 'IN_APP_AND_EMAIL'
    status VARCHAR(50) NOT NULL DEFAULT 'SENT', -- 'SENT', 'SKIPPED_DUPLICATE', 'FAILED'
    message TEXT NULL,
    sent_date DATE NOT NULL, -- Dùng để chống gửi trùng trong cùng ngày (KN-367)
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sla_log_cand FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE,
    CONSTRAINT fk_sla_log_recruiter FOREIGN KEY (recruiter_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_sla_remind_dedup (candidate_id, stage_code, sent_date),
    INDEX idx_sla_remind_date (sent_date),
    INDEX idx_sla_remind_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

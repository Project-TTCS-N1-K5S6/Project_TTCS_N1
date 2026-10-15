-- ============================================================
-- KN-116: Quản lý Phân bổ Hồ sơ Tuyển dụng
-- Migration: V004__recruiter_assignment.sql
-- ============================================================

USE test;

SET FOREIGN_KEY_CHECKS = 0;

-- Bảng phân công recruiter hỗ trợ cho từng vị trí tuyển dụng
-- recruiter_id trong recruitment_requisitions = recruiter CHÍNH
-- Bảng này lưu thêm các recruiter HỖ TRỢ
DROP TABLE IF EXISTS requisition_recruiters;
CREATE TABLE requisition_recruiters (
    id VARCHAR(36) PRIMARY KEY,
    requisition_id VARCHAR(36) NOT NULL,
    recruiter_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'SUPPORT', -- 'PRIMARY' hoặc 'SUPPORT'
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(36) NULL,
    CONSTRAINT fk_rr_requisition FOREIGN KEY (requisition_id) REFERENCES recruitment_requisitions(id) ON DELETE CASCADE,
    CONSTRAINT fk_rr_recruiter FOREIGN KEY (recruiter_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_rr_assigned_by FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_rr_req_recruiter UNIQUE (requisition_id, recruiter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_rr_requisition ON requisition_recruiters(requisition_id);
CREATE INDEX idx_rr_recruiter ON requisition_recruiters(recruiter_id);

-- Bảng lịch sử chuyển giao người phụ trách chính (recruiter_id trên requisition)
DROP TABLE IF EXISTS recruiter_assignment_history;
CREATE TABLE recruiter_assignment_history (
    id VARCHAR(36) PRIMARY KEY,
    requisition_id VARCHAR(36) NOT NULL,
    requisition_code VARCHAR(50) NULL,
    old_recruiter_id VARCHAR(36) NULL,
    old_recruiter_name VARCHAR(255) NULL,
    new_recruiter_id VARCHAR(36) NULL,
    new_recruiter_name VARCHAR(255) NULL,
    change_reason TEXT NULL,
    changed_by VARCHAR(36) NULL,
    changed_by_name VARCHAR(255) NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rah_requisition FOREIGN KEY (requisition_id) REFERENCES recruitment_requisitions(id) ON DELETE CASCADE,
    CONSTRAINT fk_rah_old_recruiter FOREIGN KEY (old_recruiter_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_rah_new_recruiter FOREIGN KEY (new_recruiter_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_rah_changed_by FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_rah_requisition ON recruiter_assignment_history(requisition_id);
CREATE INDEX idx_rah_changed_at ON recruiter_assignment_history(changed_at);

SET FOREIGN_KEY_CHECKS = 1;

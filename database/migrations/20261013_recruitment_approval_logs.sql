-- ============================================================
-- Migration: Bảng Nhật ký & Chuỗi cấp duyệt Yêu cầu tuyển dụng
-- File: database/migrations/20261013_recruitment_approval_logs.sql
-- Subtasks: KN-154, KN-155, KN-156, KN-157, KN-158, KN-159, KN-160, KN-161
-- ============================================================

CREATE TABLE IF NOT EXISTS recruitment_approval_logs (
    id VARCHAR(36) PRIMARY KEY,
    requisition_id VARCHAR(36) NOT NULL,
    step_order INT NOT NULL, -- 1: Cấp 1, 2: Cấp 2, 3: Cấp 3
    step_name VARCHAR(150) NOT NULL, -- vd: "Cấp 1: Trưởng bộ phận chuyên môn", "Cấp 2: Trưởng phòng Nhân sự", "Cấp 3: Ban Giám đốc"
    required_role VARCHAR(50) NOT NULL, -- 'HIRING_MANAGER', 'HR_MANAGER', 'APPROVER'
    assigned_user_id VARCHAR(36) NULL, -- Người được gán phụ trách duyệt cụ thể (nếu có)
    approver_id VARCHAR(36) NULL, -- Người thực tế đã phê duyệt / từ chối
    status VARCHAR(50) NOT NULL DEFAULT 'WAITING', -- 'WAITING', 'PENDING', 'APPROVED', 'REJECTED', 'SKIPPED'
    decision_date TIMESTAMP NULL, -- Thời điểm phê duyệt
    comment TEXT NULL, -- Ý kiến đóng góp / lý do phê duyệt hoặc từ chối
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appr_req FOREIGN KEY (requisition_id) REFERENCES recruitment_requisitions(id) ON DELETE CASCADE,
    CONSTRAINT fk_appr_user FOREIGN KEY (approver_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_appr_assigned FOREIGN KEY (assigned_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_appr_req_order ON recruitment_approval_logs(requisition_id, step_order);
CREATE INDEX idx_appr_status ON recruitment_approval_logs(status);

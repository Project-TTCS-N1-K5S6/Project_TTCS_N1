-- ============================================================
-- Migration: Trung tâm Phê duyệt Tuyển dụng
-- File: database/migrations/20261014_recruitment_approval_center.sql
-- Subtasks: KN-181, KN-182, KN-183, KN-184, KN-185, KN-186, KN-187, KN-188
-- ============================================================

-- Bổ sung và chuẩn hóa các trạng thái cho recruitment_requisitions:
-- 'APPROVED': Đã hoàn tất phê duyệt tại cấp duyệt cuối cùng (KN-185)
-- 'CHANGES_REQUESTED': Yêu cầu bổ sung, trả hồ sơ về cho người tạo (KN-181)

-- recruitment_approval_logs hỗ trợ trạng thái:
-- 'CHANGES_REQUESTED': Yêu cầu bổ sung kèm ý kiến giải trình bắt buộc (KN-182, KN-183, KN-187)

-- Đảm bảo chỉ mục tra cứu nhanh theo trạng thái và thời gian tạo
CREATE INDEX IF NOT EXISTS idx_req_status_created ON recruitment_requisitions(status, created_at);
CREATE INDEX IF NOT EXISTS idx_appr_req_status ON recruitment_approval_logs(requisition_id, status);

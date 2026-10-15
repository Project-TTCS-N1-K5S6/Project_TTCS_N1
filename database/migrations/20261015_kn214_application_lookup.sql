-- ==============================================================================
-- Migration: KN-214 - Tra cứu trạng thái hồ sơ bằng mã
-- Sprint: KN Sprint 4
-- Subtasks: KN-243, KN-244, KN-245, KN-246, KN-247, KN-248, KN-249
-- Ngày tạo: 2026-10-15
-- ==============================================================================
-- Mục đích:
--   Hỗ trợ tính năng tra cứu trạng thái hồ sơ công khai.
--   Ứng viên dùng candidateId (UUID) + email để tra cứu.
--   Không cần thêm cột mới vì candidates.id đã là UUID dùng làm mã tra cứu.
--   Migration này thêm index tối ưu hóa và comment làm rõ mục đích.
-- ==============================================================================

-- [KN-245]: Đảm bảo index kép (id, email) để tra cứu nhanh và xác thực quyền truy cập
-- Lưu ý: id đã là PRIMARY KEY (indexed), email đã có idx_cand_email
-- Thêm composite index tối ưu hóa câu query xác thực kép: WHERE id = ? AND email = ?
-- (MySQL dùng PRIMARY KEY cho id, composite sẽ giúp trong trường hợp dữ liệu lớn)

-- Drop nếu đã tồn tại (idempotent)
DROP INDEX IF EXISTS idx_cand_lookup ON candidates;

-- [KN-245] [KN-247]: Composite index cho tra cứu nhanh theo id + email
CREATE INDEX idx_cand_lookup ON candidates(id, email);

-- ==============================================================================
-- [KN-246]: Đảm bảo cột notes KHÔNG bị expose qua API/View tra cứu công khai
-- (Kiểm soát ở tầng DAO/Service, không cần thay đổi cấu trúc bảng)
-- ==============================================================================

-- [KN-249]: Không cần thay đổi cấu trúc bảng cho xử lý edge case -
--           được xử lý ở tầng application (Servlet + Service + DAO).

-- ==============================================================================
-- Ghi chú kỹ thuật:
-- * URL tra cứu công khai: GET /application-lookup (không yêu cầu đăng nhập)
-- * Mã tra cứu = candidates.id (UUID, được gửi email cho ứng viên sau khi nộp)
-- * Trạng thái ánh xạ (KN-243):
--     APPLIED, SCREENING   -> "Đã nhận"
--     INTERVIEWING         -> "Mời phỏng vấn"
--     OFFER, HIRED         -> "Kết thúc" (thành công)
--     REJECTED             -> "Kết thúc"
-- * KHÔNG trả về: notes, interviewer name, rejection reason chi tiết
-- ==============================================================================

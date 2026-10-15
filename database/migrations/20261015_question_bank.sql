-- ============================================================
-- IRMS - Ngân hàng câu hỏi phỏng vấn (Question Bank)
-- Migration Script: database/migrations/20261015_question_bank.sql
-- Tạo bảng questions, liên kết với competency_criteria
-- ============================================================

USE test;

SET FOREIGN_KEY_CHECKS = 0;

-- Tạo bảng ngân hàng câu hỏi phỏng vấn
CREATE TABLE IF NOT EXISTS questions (
    id          VARCHAR(36)   PRIMARY KEY,
    content     TEXT          NOT NULL COMMENT 'Nội dung câu hỏi phỏng vấn',
    difficulty_level VARCHAR(10) NOT NULL DEFAULT 'MEDIUM'
                              COMMENT 'Độ khó: EASY | MEDIUM | HARD',
    good_answer_suggestion TEXT NULL COMMENT 'Gợi ý câu trả lời tốt dành cho người phỏng vấn',
    criterion_id VARCHAR(36) NULL COMMENT 'Tiêu chí năng lực gắn liền',
    job_title   VARCHAR(255) NULL COMMENT 'Chức danh vị trí áp dụng câu hỏi',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_q_criterion FOREIGN KEY (criterion_id)
        REFERENCES competency_criteria(id) ON DELETE SET NULL,
    INDEX idx_q_criterion (criterion_id),
    INDEX idx_q_job_title (job_title(100)),
    INDEX idx_q_difficulty (difficulty_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Ngân hàng câu hỏi phỏng vấn theo tiêu chí năng lực';

-- Khởi tạo dữ liệu mẫu nếu chưa có
INSERT IGNORE INTO questions (id, content, difficulty_level, good_answer_suggestion, criterion_id, job_title) VALUES
-- Câu hỏi Java (crit-001)
('q-001', 'Hãy giải thích sự khác nhau giữa Interface và Abstract Class trong Java, và khi nào nên dùng mỗi loại?',
 'HARD',
 'Interface định nghĩa contract thuần tuý (từ Java 8 có thể có default method). Abstract Class cho phép chia sẻ code và trạng thái. Dùng Interface khi muốn đa kế thừa kiểu; dùng Abstract Class khi có logic chung cần kế thừa.',
 'crit-001', 'Lập trình viên Java'),

('q-002', 'Bạn xử lý NullPointerException như thế nào trong code Java của mình?',
 'MEDIUM',
 'Sử dụng Optional<T>, kiểm tra null trước khi truy cập, dùng Objects.requireNonNull(), hoặc dùng annotation @NonNull/@Nullable. Tránh trả về null, thay bằng Optional hoặc Collections rỗng.',
 'crit-001', 'Lập trình viên Java'),

-- Câu hỏi tư duy logic (crit-002)
('q-003', 'Trình bày giải thuật sắp xếp nhanh (QuickSort) và phân tích độ phức tạp thời gian.',
 'HARD',
 'QuickSort chọn pivot, chia mảng thành 2 phần nhỏ hơn và lớn hơn pivot, đệ quy trên 2 phần. Average O(n log n), worst case O(n²) khi pivot tệ. Cải thiện bằng cách chọn pivot ngẫu nhiên hoặc median-of-three.',
 'crit-002', 'Lập trình viên Java'),

('q-004', 'Cho danh sách số nguyên, hãy tìm cặp số có tổng bằng K trong O(n).',
 'MEDIUM',
 'Dùng HashSet: lặp qua từng phần tử x, kiểm tra xem (K-x) đã có trong set chưa; nếu có thì tìm thấy cặp, ngược lại thêm x vào set. Độ phức tạp O(n) thời gian, O(n) không gian.',
 'crit-002', 'Lập trình viên Java'),

-- Câu hỏi làm việc nhóm (crit-003)
('q-005', 'Kể về một lần bạn phải xử lý mâu thuẫn với thành viên trong nhóm. Bạn đã làm gì?',
 'MEDIUM',
 'Kỳ vọng ứng viên mô tả: chủ động tiếp cận riêng, lắng nghe quan điểm đối phương, tìm điểm chung, đề xuất giải pháp thỏa hiệp, và học hỏi từ tình huống đó.',
 'crit-003', 'Tất cả vị trí'),

('q-006', 'Bạn ưu tiên như thế nào khi có nhiều deadline cùng lúc trong dự án nhóm?',
 'EASY',
 'Ứng viên nên đề cập: phân loại ưu tiên theo tác động (urgent/important matrix), trao đổi với team lead, chia nhỏ task, cập nhật tiến độ định kỳ, và nhờ hỗ trợ khi cần thiết.',
 'crit-003', 'Tất cả vị trí'),

-- Câu hỏi giải quyết vấn đề (crit-004)
('q-007', 'Mô tả quy trình debug khi hệ thống production bị lỗi nghiêm trọng (outage).',
 'HARD',
 'Quy trình: 1) Xác nhận phạm vi ảnh hưởng, 2) Rollback nếu cần, 3) Thu thập log/metrics, 4) Tái hiện vấn đề, 5) Root cause analysis, 6) Fix + test + deploy, 7) Postmortem. Luôn ưu tiên khôi phục dịch vụ trước.',
 'crit-004', 'Lập trình viên Java'),

('q-008', 'Hệ thống bạn phụ trách đột ngột chậm 10x. Bạn sẽ điều tra từ đâu?',
 'HARD',
 'Kiểm tra theo thứ tự: infrastructure (CPU, RAM, disk, network), database (slow query log, explain plan, lock wait), application (thread dump, GC log, profiler), external services (timeout, latency). Dùng APM tools như Datadog, Jaeger để trace.',
 'crit-004', 'Lập trình viên Java'),

-- Câu hỏi giao tiếp (crit-005)
('q-009', 'Làm thế nào bạn giải thích một vấn đề kỹ thuật phức tạp cho người không biết IT?',
 'EASY',
 'Dùng ngôn ngữ đơn giản, ví dụ thực tế từ cuộc sống, tránh jargon kỹ thuật. Hỏi lại để đảm bảo họ hiểu. Sử dụng hình ảnh/sơ đồ nếu cần.',
 'crit-005', 'Tất cả vị trí');

SET FOREIGN_KEY_CHECKS = 1;

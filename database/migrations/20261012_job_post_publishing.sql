-- ============================================================
-- Migration: KN-120 - Quản lý Xuất bản bài đăng
-- Yêu cầu:
-- 1. Tin phải duyệt trước khi show công khai
-- 2. Xem trước đúng như giao diện khai báo trước khi xuất bản
-- 3. Ghi lại bản xuất và thời điểm
-- ============================================================

USE test;

-- 1. Bổ sung bảng job_postings nếu chưa có
CREATE TABLE IF NOT EXISTS job_postings (
    id VARCHAR(36) PRIMARY KEY,
    requisition_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    platform VARCHAR(100) DEFAULT 'CAREER_PORTAL',
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- 'DRAFT', 'PUBLISHED', 'UNPUBLISHED'
    current_version INT NOT NULL DEFAULT 1,
    published_at TIMESTAMP NULL,
    published_by VARCHAR(36) NULL,
    unpublished_at TIMESTAMP NULL,
    unpublished_by VARCHAR(36) NULL,
    public_url VARCHAR(255) NULL,
    views_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_jp_requisition FOREIGN KEY (requisition_id) REFERENCES recruitment_requisitions(id) ON DELETE CASCADE,
    CONSTRAINT fk_jp_pub_user FOREIGN KEY (published_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_jp_unpub_user FOREIGN KEY (unpublished_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bảng ghi lại các bản xuất và thời điểm xuất bản (job_post_publications)
CREATE TABLE IF NOT EXISTS job_post_publications (
    id VARCHAR(36) PRIMARY KEY,
    requisition_id VARCHAR(36) NOT NULL,
    job_posting_id VARCHAR(36) NULL,
    version_number INT NOT NULL DEFAULT 1,
    version_label VARCHAR(50) NOT NULL,
    channel VARCHAR(100) NOT NULL DEFAULT 'CAREER_PORTAL',
    status VARCHAR(50) NOT NULL DEFAULT 'PUBLISHED', -- 'PUBLISHED', 'ARCHIVED', 'UNPUBLISHED'
    published_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_by VARCHAR(36) NULL,
    published_by_name VARCHAR(255) NULL,
    unpublished_at TIMESTAMP NULL,
    unpublished_by VARCHAR(36) NULL,
    snapshot_title VARCHAR(255) NOT NULL,
    snapshot_data LONGTEXT NOT NULL,
    change_summary TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_jpp_requisition FOREIGN KEY (requisition_id) REFERENCES recruitment_requisitions(id) ON DELETE CASCADE,
    CONSTRAINT fk_jpp_posting FOREIGN KEY (job_posting_id) REFERENCES job_postings(id) ON DELETE SET NULL,
    CONSTRAINT fk_jpp_user FOREIGN KEY (published_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_jp_req ON job_postings(requisition_id);
CREATE INDEX idx_jp_status ON job_postings(status);
CREATE INDEX idx_jpp_req ON job_post_publications(requisition_id);
CREATE INDEX idx_jpp_pub_at ON job_post_publications(published_at);
CREATE INDEX idx_jpp_version ON job_post_publications(requisition_id, version_number);

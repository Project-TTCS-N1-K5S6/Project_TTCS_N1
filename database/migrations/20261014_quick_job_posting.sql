-- Migration: Hỗ trợ chức năng Tạo nhanh Bài đăng Tuyển dụng (KN-204, KN-205, KN-206, KN-207, KN-208)
-- Bổ sung các cột mở rộng cho bảng job_postings nếu chưa có

ALTER TABLE job_postings ADD COLUMN IF NOT EXISTS job_description MEDIUMTEXT NULL AFTER title;
ALTER TABLE job_postings ADD COLUMN IF NOT EXISTS work_location_id VARCHAR(36) NULL AFTER job_description;
ALTER TABLE job_postings ADD COLUMN IF NOT EXISTS work_location VARCHAR(255) NULL AFTER work_location_id;
ALTER TABLE job_postings ADD COLUMN IF NOT EXISTS work_mode_id VARCHAR(36) NULL AFTER work_location;
ALTER TABLE job_postings ADD COLUMN IF NOT EXISTS work_mode VARCHAR(100) NULL AFTER work_mode_id;
ALTER TABLE job_postings ADD COLUMN IF NOT EXISTS application_deadline DATE NULL AFTER work_mode;
ALTER TABLE job_postings ADD COLUMN IF NOT EXISTS salary_visible BOOLEAN DEFAULT TRUE AFTER application_deadline;
ALTER TABLE job_postings ADD COLUMN IF NOT EXISTS min_salary DECIMAL(15,2) NULL AFTER salary_visible;
ALTER TABLE job_postings ADD COLUMN IF NOT EXISTS max_salary DECIMAL(15,2) NULL AFTER min_salary;
ALTER TABLE job_postings ADD COLUMN IF NOT EXISTS currency VARCHAR(10) DEFAULT 'VND' AFTER max_salary;
ALTER TABLE job_postings ADD COLUMN IF NOT EXISTS views_count INT DEFAULT 0 AFTER status;

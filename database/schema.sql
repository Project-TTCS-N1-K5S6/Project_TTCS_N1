-- ============================================================
-- IRMS (Internal Recruitment Management System)
-- Cơ sở dữ liệu: MySQL 8.0+
-- File: database/schema.sql
-- ============================================================

USE test;

-- Tắt kiểm tra khóa ngoại tạm thời khi khởi tạo bảng
SET FOREIGN_KEY_CHECKS = 0;

-- 1. Bảng phòng ban (departments)
DROP TABLE IF EXISTS departments;
CREATE TABLE departments (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parent_id VARCHAR(36) NULL,
    manager_id VARCHAR(36) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'INACTIVE'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36) NULL,
    updated_by VARCHAR(36) NULL,
    CONSTRAINT fk_dept_parent FOREIGN KEY (parent_id) REFERENCES departments(id) ON DELETE RESTRICT,
    CONSTRAINT fk_dept_manager FOREIGN KEY (manager_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_dept_parent (parent_id),
    INDEX idx_dept_manager (manager_id),
    INDEX idx_dept_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bảng vai trò (roles)
DROP TABLE IF EXISTS roles;
CREATE TABLE roles (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_system_role BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Bảng quyền hạn (permissions)
DROP TABLE IF EXISTS permissions;
CREATE TABLE permissions (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    module VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Bảng người dùng (users)
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    employee_code VARCHAR(50) UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(50),
    job_title VARCHAR(100),
    avatar_url VARCHAR(255),
    department_id VARCHAR(36),
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'LOCKED', 'INACTIVE'
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP NULL,
    locked_at TIMESTAMP NULL,
    lock_reason TEXT NULL,
    locked_by VARCHAR(36) NULL,
    must_change_password BOOLEAN DEFAULT FALSE,
    session_version INT NOT NULL DEFAULT 0,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36) NULL,
    updated_by VARCHAR(36) NULL,
    CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,
    CONSTRAINT fk_users_locked_by FOREIGN KEY (locked_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_users_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_users_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Bảng quan hệ Người dùng - Vai trò (user_roles: N-N)
DROP TABLE IF EXISTS user_roles;
CREATE TABLE user_roles (
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Bảng quan hệ Vai trò - Quyền hạn (role_permissions: N-N)
DROP TABLE IF EXISTS role_permissions;
CREATE TABLE role_permissions (
    role_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Bảng Refresh Token (quản lý phiên đăng nhập)
DROP TABLE IF EXISTS refresh_tokens;
CREATE TABLE refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    replaced_by_token_id VARCHAR(36) NULL,
    user_agent TEXT,
    ip_address VARCHAR(45),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_rt_replaced FOREIGN KEY (replaced_by_token_id) REFERENCES refresh_tokens(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Bảng Password Reset Token (quên mật khẩu)
DROP TABLE IF EXISTS password_reset_tokens;
CREATE TABLE password_reset_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Bảng Nhật ký kiểm toán (audit_logs)
DROP TABLE IF EXISTS audit_logs;
CREATE TABLE audit_logs (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    description TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Bảng Hòm thư gửi email (email_outbox)
DROP TABLE IF EXISTS email_outbox;
CREATE TABLE email_outbox (
    id VARCHAR(36) PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    template VARCHAR(100) NOT NULL,
    payload JSON,
    status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'SENT', 'FAILED'
    retry_count INT DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Danh mục dùng chung cho các biểu mẫu tuyển dụng
DROP TABLE IF EXISTS shared_catalogs;
CREATE TABLE shared_catalogs (
    id VARCHAR(36) PRIMARY KEY,
    type_code ENUM('APPLICATION_SOURCE', 'REJECTION_REASON', 'WORK_LOCATION', 'WORK_MODE') NOT NULL,
    value VARCHAR(255) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_shared_catalog_type_value UNIQUE (type_code, value),
    INDEX idx_shared_catalog_order (type_code, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Bảng Yêu cầu tuyển dụng (recruitment_requisitions)
DROP TABLE IF EXISTS recruitment_requisitions;
CREATE TABLE recruitment_requisitions (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    position_title VARCHAR(255) NOT NULL,
    department_id VARCHAR(36) NULL,
    headcount INT NOT NULL DEFAULT 1,
    recruitment_reason VARCHAR(50) NOT NULL DEFAULT 'NEW_HEADCOUNT', -- 'REPLACEMENT' (Thay thế), 'NEW_HEADCOUNT' (Tăng mới)
    min_salary DECIMAL(15, 2) NULL,
    max_salary DECIMAL(15, 2) NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    deadline DATE NOT NULL, -- Ngày cần người (không được ở quá khứ)
    job_description LONGTEXT NULL, -- Soạn mô tả công việc
    job_requirements LONGTEXT NULL, -- Soạn yêu cầu ứng viên
    salary_explanation TEXT NULL, -- Bắt buộc nếu dải lương đề xuất nằm ngoài dải chuẩn của chức danh
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- 'DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'CHANGES_REQUESTED', 'OPEN', 'FULFILLED', 'PAUSED', 'CANCELLED', 'CLOSED', 'REJECTED'
    close_status VARCHAR(50) NULL, -- 'FULFILLED', 'PAUSED', 'CANCELLED'
    close_reason VARCHAR(255) NULL, -- Lý do riêng biệt tương ứng từng trạng thái
    close_notes TEXT NULL, -- Ghi chú chi tiết lý do đóng
    closed_at TIMESTAMP NULL, -- Thời điểm đóng
    closed_by VARCHAR(36) NULL, -- Người thực hiện đóng
    recruiter_id VARCHAR(36) NULL,
    hiring_manager_id VARCHAR(36) NULL,
    work_location_id VARCHAR(36) NULL,
    work_mode_id VARCHAR(36) NULL,
    created_by VARCHAR(36) NULL,
    is_budget_overridden BOOLEAN NOT NULL DEFAULT FALSE,
    budget_override_reason TEXT NULL,
    budget_overridden_by VARCHAR(36) NULL,
    budget_overridden_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_req_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL,
    CONSTRAINT fk_req_recruiter FOREIGN KEY (recruiter_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_req_manager FOREIGN KEY (hiring_manager_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_req_work_location FOREIGN KEY (work_location_id) REFERENCES shared_catalogs(id) ON DELETE RESTRICT,
    CONSTRAINT fk_req_work_mode FOREIGN KEY (work_mode_id) REFERENCES shared_catalogs(id) ON DELETE RESTRICT,
    CONSTRAINT fk_req_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_req_closed_by FOREIGN KEY (closed_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_req_overridden_by FOREIGN KEY (budget_overridden_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Bảng Tin tuyển dụng (job_postings)
DROP TABLE IF EXISTS job_postings;
CREATE TABLE job_postings (
    id VARCHAR(36) PRIMARY KEY,
    requisition_id VARCHAR(36) NOT NULL,
    department_id VARCHAR(36) NULL,
    title VARCHAR(255) NOT NULL,
    description MEDIUMTEXT NULL,
    job_description MEDIUMTEXT NULL,
    work_location_id VARCHAR(36) NULL,
    location_id VARCHAR(36) NULL,
    work_location VARCHAR(255) NULL,
    work_mode_id VARCHAR(36) NULL,
    work_mode VARCHAR(100) NULL,
    work_type VARCHAR(100) NULL,
    application_deadline DATE NULL,
    expired_at TIMESTAMP NULL,
    salary_visible BOOLEAN DEFAULT TRUE,
    min_salary DECIMAL(15,2) NULL,
    max_salary DECIMAL(15,2) NULL,
    currency VARCHAR(10) DEFAULT 'VND',
    platform VARCHAR(100) DEFAULT 'INTERNAL', -- 'INTERNAL', 'CAREER_SITE', 'LINKEDIN', 'TOPCV'
    status VARCHAR(50) NOT NULL DEFAULT 'PUBLISHED', -- 'PUBLISHED', 'UNPUBLISHED', 'CLOSED', 'DRAFT', 'ARCHIVED'
    current_version INT NOT NULL DEFAULT 1,
    views_count INT DEFAULT 0,
    posted_url VARCHAR(255) NULL,
    public_url VARCHAR(255) NULL,
    published_at TIMESTAMP NULL,
    published_by VARCHAR(36) NULL,
    unpublished_at TIMESTAMP NULL,
    unpublished_by VARCHAR(36) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_jp_requisition FOREIGN KEY (requisition_id) REFERENCES recruitment_requisitions(id) ON DELETE CASCADE,
    CONSTRAINT fk_jp_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Bảng Ứng viên (candidates)
DROP TABLE IF EXISTS candidates;
CREATE TABLE candidates (
    id VARCHAR(36) PRIMARY KEY,
    requisition_id VARCHAR(36) NULL,
    application_source_id VARCHAR(36) NULL,
    referred_by_user_id VARCHAR(36) NULL,
    rejection_reason_id VARCHAR(36) NULL,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    normalized_phone VARCHAR(50) NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'APPLIED', -- 'APPLIED', 'SCREENING', 'INTERVIEWING', 'OFFER', 'HIRED', 'REJECTED', 'MERGED'
    merged_into_id VARCHAR(36) NULL,
    is_merged BOOLEAN DEFAULT FALSE,
    tracking_code VARCHAR(50) NULL UNIQUE,
    cv_url TEXT,
    extracted_name VARCHAR(255) NULL,
    extracted_email VARCHAR(255) NULL,
    extracted_phone VARCHAR(50) NULL,
    extraction_status VARCHAR(50) DEFAULT 'NONE', -- 'SUCCESS', 'PARTIAL', 'FAILED', 'CONFIRMED', 'NONE'
    cv_extracted_at TIMESTAMP NULL,
    notes TEXT,
    tags TEXT NULL,
    cover_letter MEDIUMTEXT NULL,
    ip_address VARCHAR(45) NULL,
    is_sla_overdue BOOLEAN NOT NULL DEFAULT FALSE,
    sla_overdue_detected_at TIMESTAMP NULL,
    stage_entered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cand_requisition FOREIGN KEY (requisition_id) REFERENCES recruitment_requisitions(id) ON DELETE SET NULL,
    CONSTRAINT fk_cand_application_source FOREIGN KEY (application_source_id) REFERENCES shared_catalogs(id) ON DELETE RESTRICT,
    CONSTRAINT fk_cand_referred_by FOREIGN KEY (referred_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_cand_rejection_reason FOREIGN KEY (rejection_reason_id) REFERENCES shared_catalogs(id) ON DELETE RESTRICT,
    CONSTRAINT fk_cand_merged_into FOREIGN KEY (merged_into_id) REFERENCES candidates(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_cand_referrer ON candidates(referred_by_user_id);
CREATE INDEX idx_cand_extract_status ON candidates(extraction_status);
CREATE INDEX idx_cand_email ON candidates(email);
CREATE INDEX idx_cand_phone ON candidates(phone);

-- 13.1. Bảng Lịch sử thêm ứng viên & xử lý trùng lặp (candidate_history - KN-353)
DROP TABLE IF EXISTS candidate_history;
CREATE TABLE candidate_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    candidate_id VARCHAR(36) NULL,
    full_name VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    phone VARCHAR(50) NULL,
    source_name VARCHAR(100) NULL,
    has_cv BOOLEAN NOT NULL DEFAULT FALSE,
    details VARCHAR(500) NULL,
    client_ip VARCHAR(45) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_history_candidate (candidate_id),
    INDEX idx_history_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Token rút hồ sơ công khai, chỉ lưu SHA-256 và chỉ dùng một lần
CREATE TABLE candidate_withdrawal_tokens (
    id VARCHAR(36) PRIMARY KEY,
    candidate_id VARCHAR(36) NOT NULL UNIQUE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cwt_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. Bảng Dải lương theo vị trí (salary_ranges)
DROP TABLE IF EXISTS salary_ranges;
CREATE TABLE salary_ranges (
    id VARCHAR(36) PRIMARY KEY,
    position_code VARCHAR(50) NOT NULL UNIQUE,
    position_title VARCHAR(255) NOT NULL,
    level VARCHAR(50) NOT NULL,
    department_id VARCHAR(36) NULL,
    min_salary DECIMAL(15, 2) NOT NULL,
    max_salary DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    note TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sr_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Chỉ mục tối ưu hóa hiệu năng (Indexes)
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_dept ON users(department_id);
CREATE INDEX idx_users_session_ver ON users(id, session_version);

CREATE INDEX idx_ur_user ON user_roles(user_id);
CREATE INDEX idx_ur_role ON user_roles(role_id);
CREATE INDEX idx_rp_role ON role_permissions(role_id);
CREATE INDEX idx_rp_perm ON role_permissions(permission_id);

CREATE INDEX idx_rt_user ON refresh_tokens(user_id);
CREATE INDEX idx_rt_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_prt_user ON password_reset_tokens(user_id);
CREATE INDEX idx_prt_hash ON password_reset_tokens(token_hash);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_created ON audit_logs(created_at);

CREATE INDEX idx_cand_req ON candidates(requisition_id);
CREATE INDEX idx_cand_email ON candidates(email);
CREATE INDEX idx_cand_status ON candidates(status);
CREATE INDEX idx_cand_email_req ON candidates(email, requisition_id);
CREATE INDEX idx_cand_tracking ON candidates(tracking_code);

CREATE INDEX idx_sr_dept ON salary_ranges(department_id);

-- 14. Bảng Tiêu chí năng lực (competency_criteria)
DROP TABLE IF EXISTS competency_criteria;
CREATE TABLE competency_criteria (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(50) NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    evaluation_guideline TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. Bảng Ngân hàng câu hỏi (questions)
DROP TABLE IF EXISTS questions;
CREATE TABLE questions (
    id VARCHAR(36) PRIMARY KEY,
    content TEXT NOT NULL,
    difficulty_level VARCHAR(20) NOT NULL, -- EASY, MEDIUM, HARD
    good_answer_suggestion TEXT,
    criterion_id VARCHAR(36) NOT NULL,
    job_title VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_q_criterion FOREIGN KEY (criterion_id) REFERENCES competency_criteria(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_q_criterion ON questions(criterion_id);
CREATE INDEX idx_q_job_title ON questions(job_title);

-- 16. Bảng Khung năng lực (competency_frameworks)
DROP TABLE IF EXISTS competency_frameworks;
CREATE TABLE competency_frameworks (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- 'DRAFT', 'ACTIVE', 'INACTIVE'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(36) NULL,
    updated_by VARCHAR(36) NULL,
    CONSTRAINT fk_cf_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_cf_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_cf_code ON competency_frameworks(code);
CREATE INDEX idx_cf_status ON competency_frameworks(status);

-- 17. Bảng liên kết Khung năng lực - Tiêu chí (competency_framework_criteria)
DROP TABLE IF EXISTS competency_framework_criteria;
CREATE TABLE competency_framework_criteria (
    id VARCHAR(36) PRIMARY KEY,
    framework_id VARCHAR(36) NOT NULL,
    criterion_id VARCHAR(36) NOT NULL,
    weight DECIMAL(5, 2) NOT NULL,
    display_order INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cfc_framework FOREIGN KEY (framework_id) REFERENCES competency_frameworks(id) ON DELETE CASCADE,
    CONSTRAINT fk_cfc_criterion FOREIGN KEY (criterion_id) REFERENCES competency_criteria(id) ON DELETE RESTRICT,
    CONSTRAINT uq_cfc_framework_criterion UNIQUE (framework_id, criterion_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_cfc_framework ON competency_framework_criteria(framework_id);
CREATE INDEX idx_cfc_criterion ON competency_framework_criteria(criterion_id);

-- 18. Bảng liên kết Chức danh - Khung năng lực (position_competency_frameworks)
DROP TABLE IF EXISTS position_competency_frameworks;
CREATE TABLE position_competency_frameworks (
    id VARCHAR(36) PRIMARY KEY,
    position_id VARCHAR(36) NOT NULL UNIQUE,
    framework_id VARCHAR(36) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(36) NULL,
    CONSTRAINT fk_pcf_position FOREIGN KEY (position_id) REFERENCES salary_ranges(id) ON DELETE CASCADE,
    CONSTRAINT fk_pcf_framework FOREIGN KEY (framework_id) REFERENCES competency_frameworks(id) ON DELETE CASCADE,
    CONSTRAINT fk_pcf_user FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pcf_framework ON position_competency_frameworks(framework_id);
<<<<<<< HEAD
CREATE INDEX idx_pcf_position ON position_competency_frameworks(position_id);
CREATE INDEX idx_jp_req ON job_postings(requisition_id);
CREATE INDEX idx_jp_status ON job_postings(status);
CREATE INDEX idx_jp_expired_at ON job_postings(expired_at);
CREATE INDEX idx_jp_dept_id ON job_postings(department_id);
CREATE INDEX idx_jp_loc_id ON job_postings(location_id);
CREATE INDEX idx_jp_work_type ON job_postings(work_type);
CREATE INDEX idx_jp_pub_exp ON job_postings(status, expired_at);

-- 19. Bảng Nhật ký & Chuỗi cấp duyệt yêu cầu tuyển dụng (recruitment_approval_logs)
DROP TABLE IF EXISTS recruitment_approval_logs;
CREATE TABLE recruitment_approval_logs (
    id VARCHAR(36) PRIMARY KEY,
    requisition_id VARCHAR(36) NOT NULL,
    step_order INT NOT NULL, -- 1: Cấp 1, 2: Cấp 2, 3: Cấp 3
    step_name VARCHAR(150) NOT NULL, -- "Cấp 1: Trưởng bộ phận chuyên môn", "Cấp 2: Trưởng phòng Nhân sự", "Cấp 3: Ban Giám đốc"
    required_role VARCHAR(50) NOT NULL, -- 'HIRING_MANAGER', 'HR_MANAGER', 'APPROVER'
    assigned_user_id VARCHAR(36) NULL, -- Người được gán phụ trách duyệt cụ thể (nếu có)
    approver_id VARCHAR(36) NULL, -- Người thực tế đã phê duyệt / từ chối
    status VARCHAR(50) NOT NULL DEFAULT 'WAITING', -- 'WAITING', 'PENDING', 'APPROVED', 'REJECTED', 'CHANGES_REQUESTED', 'SKIPPED'
    decision_date TIMESTAMP NULL, -- Thời điểm phê duyệt
    comment TEXT NULL, -- Ý kiến đóng góp / lý do phê duyệt hoặc từ chối
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appr_req FOREIGN KEY (requisition_id) REFERENCES recruitment_requisitions(id) ON DELETE CASCADE,
    CONSTRAINT fk_appr_user FOREIGN KEY (approver_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_appr_assigned FOREIGN KEY (assigned_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_appr_req_order ON recruitment_approval_logs(requisition_id, step_order);
CREATE INDEX idx_appr_status ON recruitment_approval_logs(status);

-- 20. Bảng Phân công Recruiter hỗ trợ cho Vị trí tuyển dụng (KN-116)
DROP TABLE IF EXISTS requisition_recruiters;
CREATE TABLE requisition_recruiters (
    id VARCHAR(36) PRIMARY KEY,
    requisition_id VARCHAR(36) NOT NULL,
    recruiter_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'SUPPORT', -- 'PRIMARY', 'SUPPORT'
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(36) NULL,
    CONSTRAINT fk_rr_req FOREIGN KEY (requisition_id) REFERENCES recruitment_requisitions(id) ON DELETE CASCADE,
    CONSTRAINT fk_rr_rec FOREIGN KEY (recruiter_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_rr_req_rec UNIQUE (requisition_id, recruiter_id),
    INDEX idx_rr_requisition (requisition_id),
    INDEX idx_rr_recruiter (recruiter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 21. Bảng Lịch sử chuyển giao Recruiter chính (KN-116)
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
    CONSTRAINT fk_rah_req FOREIGN KEY (requisition_id) REFERENCES recruitment_requisitions(id) ON DELETE CASCADE,
    INDEX idx_rah_requisition (requisition_id),
    INDEX idx_rah_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 22. Bảng Khai báo Định biên & Ngân sách Nhân sự theo phòng ban/năm (KN-197, KN-198)
DROP TABLE IF EXISTS department_headcount_budgets;
CREATE TABLE department_headcount_budgets (
    id VARCHAR(36) PRIMARY KEY,
    department_id VARCHAR(36) NOT NULL,
    fiscal_year INT NOT NULL,
    target_headcount INT NOT NULL DEFAULT 0,
    total_salary_budget DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    description TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'INACTIVE', 'LOCKED'
    created_by VARCHAR(36) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(36) NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_dhb_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE CASCADE,
    CONSTRAINT fk_dhb_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_dhb_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE KEY uk_dept_fiscal_year (department_id, fiscal_year),
    INDEX idx_dhb_year (fiscal_year),
    INDEX idx_dhb_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 23. Bảng Danh mục Subtasks công việc (budget_subtasks: KN-195 -> KN-201)
DROP TABLE IF EXISTS budget_subtasks;
CREATE TABLE budget_subtasks (
    id VARCHAR(36) PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', -- 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'
    assignee_id VARCHAR(36) NULL,
    assignee_name VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'TODO', -- 'TODO', 'IN_PROGRESS', 'DONE'
    module_type VARCHAR(10) NOT NULL DEFAULT 'BE', -- 'BE', 'FE'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_bs_assignee FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_bs_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 24. Bảng Ngân hàng câu hỏi phỏng vấn (questions)
DROP TABLE IF EXISTS questions;
CREATE TABLE questions (
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

-- 25. Bảng Cấu hình ngưỡng SLA theo giai đoạn (sla_configurations)
DROP TABLE IF EXISTS sla_configurations;
CREATE TABLE sla_configurations (
    id VARCHAR(36) PRIMARY KEY,
    stage_code VARCHAR(50) NOT NULL UNIQUE,
    stage_name VARCHAR(100) NOT NULL,
    sla_days INT NOT NULL DEFAULT 3,
    warning_threshold_days INT NOT NULL DEFAULT 1,
    action_required VARCHAR(255) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(36) NULL,
    CONSTRAINT fk_sla_updated_by FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_sla_stage (stage_code),
    INDEX idx_sla_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 26. Bảng Thông báo nội bộ (in_app_notifications)
DROP TABLE IF EXISTS in_app_notifications;
CREATE TABLE in_app_notifications (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'SLA_OVERDUE',
    reference_id VARCHAR(36) NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_user_read (user_id, is_read),
    INDEX idx_notif_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 27. Bảng Nhật ký cảnh báo & Nhắc việc SLA (sla_reminder_logs)
DROP TABLE IF EXISTS sla_reminder_logs;
CREATE TABLE sla_reminder_logs (
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
    channel VARCHAR(50) NOT NULL DEFAULT 'IN_APP_AND_EMAIL',
    status VARCHAR(50) NOT NULL DEFAULT 'SENT',
    message TEXT NULL,
    sent_date DATE NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sla_log_cand FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE,
    CONSTRAINT fk_sla_log_recruiter FOREIGN KEY (recruiter_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_sla_remind_dedup (candidate_id, stage_code, sent_date),
    INDEX idx_sla_remind_date (sent_date),
    INDEX idx_sla_remind_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 28. Bảng cảnh báo hồ sơ nghi trùng lặp (candidate_duplicate_warnings)
DROP TABLE IF EXISTS candidate_duplicate_warnings;
CREATE TABLE candidate_duplicate_warnings (
    id VARCHAR(36) PRIMARY KEY,
    candidate_id_1 VARCHAR(36) NOT NULL,
    candidate_id_2 VARCHAR(36) NOT NULL,
    match_type VARCHAR(20) NOT NULL, -- 'EMAIL', 'PHONE', 'BOTH'
    match_value VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'DISMISSED', 'MERGED', 'SNOOZED'
    snooze_until TIMESTAMP NULL,
    reminder_count INT NOT NULL DEFAULT 0,
    reminder_interval_days INT NOT NULL DEFAULT 3,
    notes TEXT NULL,
    resolved_by VARCHAR(36) NULL,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cdw_cand1 FOREIGN KEY (candidate_id_1) REFERENCES candidates(id) ON DELETE CASCADE,
    CONSTRAINT fk_cdw_cand2 FOREIGN KEY (candidate_id_2) REFERENCES candidates(id) ON DELETE CASCADE,
    CONSTRAINT fk_cdw_resolver FOREIGN KEY (resolved_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_cdw_pair UNIQUE (candidate_id_1, candidate_id_2),
    INDEX idx_cdw_status (status),
    INDEX idx_cdw_snooze (snooze_until),
    INDEX idx_cdw_cand1 (candidate_id_1),
    INDEX idx_cdw_cand2 (candidate_id_2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 29. Bảng lịch sử hợp nhất hồ sơ (candidate_merge_history)
DROP TABLE IF EXISTS candidate_merge_history;
CREATE TABLE candidate_merge_history (
    id VARCHAR(36) PRIMARY KEY,
    warning_id VARCHAR(36) NULL,
    primary_candidate_id VARCHAR(36) NOT NULL,
    secondary_candidate_id VARCHAR(36) NOT NULL,
    primary_candidate_name VARCHAR(255) NOT NULL,
    secondary_candidate_name VARCHAR(255) NOT NULL,
    merged_by_user_id VARCHAR(36) NULL,
    merged_by_user_name VARCHAR(255) NULL,
    field_selection_details LONGTEXT NULL,
    merged_snapshot LONGTEXT NULL,
    merge_notes TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cmh_warning FOREIGN KEY (warning_id) REFERENCES candidate_duplicate_warnings(id) ON DELETE SET NULL,
    CONSTRAINT fk_cmh_primary FOREIGN KEY (primary_candidate_id) REFERENCES candidates(id) ON DELETE CASCADE,
    CONSTRAINT fk_cmh_user FOREIGN KEY (merged_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_cmh_primary (primary_candidate_id),
    INDEX idx_cmh_secondary (secondary_candidate_id),
    INDEX idx_cmh_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 30. Bảng hoạt động & bảo toàn lịch sử ứng viên (candidate_activities)
DROP TABLE IF EXISTS candidate_activities;
CREATE TABLE candidate_activities (
    id VARCHAR(36) PRIMARY KEY,
    candidate_id VARCHAR(36) NOT NULL,
    activity_type VARCHAR(50) NOT NULL, -- 'APPLICATION', 'INTERVIEW', 'ASSESSMENT', 'NOTE', 'STATUS_CHANGE', 'MERGE', 'WARNING'
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    performed_by VARCHAR(36) NULL,
    performed_by_name VARCHAR(255) NULL,
    original_candidate_id VARCHAR(36) NULL,
    metadata_json LONGTEXT NULL,
    activity_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ca_candidate FOREIGN KEY (candidate_id) REFERENCES candidates(id) ON DELETE CASCADE,
    CONSTRAINT fk_ca_user FOREIGN KEY (performed_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_ca_candidate (candidate_id),
    INDEX idx_ca_type (activity_type),
    INDEX idx_ca_date (activity_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

USE test;

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

INSERT INTO shared_catalogs (id, type_code, value, display_order) VALUES
('cat-src-referral', 'APPLICATION_SOURCE', 'Nhân viên giới thiệu', 0),
('cat-src-linkedin', 'APPLICATION_SOURCE', 'LinkedIn', 1),
('cat-src-career-site', 'APPLICATION_SOURCE', 'Trang tuyển dụng công ty', 2),
('cat-src-job-board', 'APPLICATION_SOURCE', 'Trang việc làm', 3),
('cat-rej-experience', 'REJECTION_REASON', 'Kinh nghiệm chưa phù hợp', 0),
('cat-rej-skill', 'REJECTION_REASON', 'Kỹ năng chưa đáp ứng yêu cầu', 1),
('cat-rej-salary', 'REJECTION_REASON', 'Mức lương chưa phù hợp', 2),
('cat-rej-other', 'REJECTION_REASON', 'Lý do khác', 3),
('cat-loc-hanoi', 'WORK_LOCATION', 'Hà Nội', 0),
('cat-loc-hcm', 'WORK_LOCATION', 'Thành phố Hồ Chí Minh', 1),
('cat-loc-danang', 'WORK_LOCATION', 'Đà Nẵng', 2),
('cat-mode-office', 'WORK_MODE', 'Làm việc tại văn phòng', 0),
('cat-mode-hybrid', 'WORK_MODE', 'Làm việc kết hợp', 1),
('cat-mode-remote', 'WORK_MODE', 'Làm việc từ xa', 2);

ALTER TABLE recruitment_requisitions
    ADD COLUMN work_location_id VARCHAR(36) NULL,
    ADD COLUMN work_mode_id VARCHAR(36) NULL,
    ADD CONSTRAINT fk_req_work_location FOREIGN KEY (work_location_id)
        REFERENCES shared_catalogs(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_req_work_mode FOREIGN KEY (work_mode_id)
        REFERENCES shared_catalogs(id) ON DELETE RESTRICT;

ALTER TABLE candidates
    ADD COLUMN application_source_id VARCHAR(36) NULL,
    ADD COLUMN rejection_reason_id VARCHAR(36) NULL,
    ADD CONSTRAINT fk_cand_application_source FOREIGN KEY (application_source_id)
        REFERENCES shared_catalogs(id) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_cand_rejection_reason FOREIGN KEY (rejection_reason_id)
        REFERENCES shared_catalogs(id) ON DELETE RESTRICT;

CREATE DATABASE IF NOT EXISTS ttcs_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE ttcs_db;

CREATE TABLE IF NOT EXISTS recruitment_jobs (
  id VARCHAR(64) NOT NULL,
  title VARCHAR(160) NOT NULL,
  status VARCHAR(40) NOT NULL,
  description TEXT NOT NULL,
  requirements TEXT NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS recruitment_copies (
  id CHAR(36) NOT NULL,
  source_job_id VARCHAR(64) NOT NULL,
  source_title VARCHAR(160) NOT NULL,
  title VARCHAR(160) NOT NULL,
  description TEXT NOT NULL,
  requirements TEXT NOT NULL,
  status VARCHAR(40) NOT NULL DEFAULT 'Nháp',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX idx_recruitment_copies_created_at (created_at),
  CONSTRAINT fk_recruitment_copies_source_job
    FOREIGN KEY (source_job_id) REFERENCES recruitment_jobs (id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS ttcs_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE ttcs_db;

CREATE TABLE IF NOT EXISTS candidates (
  id CHAR(36) NOT NULL,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(254) NOT NULL,
  email_normalized VARCHAR(254) NOT NULL,
  phone VARCHAR(30) NOT NULL,
  phone_normalized VARCHAR(15) NOT NULL,
  position_applied VARCHAR(120) NOT NULL,
  source_name VARCHAR(40) NOT NULL,
  cv_original_name VARCHAR(255) NULL,
  cv_storage_name CHAR(40) NULL,
  cv_content_type VARCHAR(100) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_candidates_email_normalized (email_normalized),
  KEY idx_candidates_phone_normalized (phone_normalized),
  KEY idx_candidates_created_at (created_at)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS candidate_history (
  id BIGINT NOT NULL AUTO_INCREMENT,
  action VARCHAR(30) NOT NULL,
  candidate_id CHAR(36) NULL,
  full_name VARCHAR(120) NULL,
  email VARCHAR(254) NULL,
  phone VARCHAR(30) NULL,
  source_name VARCHAR(40) NULL,
  has_cv BOOLEAN NOT NULL DEFAULT FALSE,
  details VARCHAR(500) NULL,
  client_ip VARCHAR(45) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_history_candidate (candidate_id),
  KEY idx_history_created_at (created_at)
) ENGINE=InnoDB
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

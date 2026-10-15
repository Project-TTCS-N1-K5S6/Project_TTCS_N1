-- Token rút hồ sơ công khai: chỉ lưu SHA-256, một lần dùng, hết hạn sau 7 ngày.
CREATE TABLE IF NOT EXISTS candidate_withdrawal_tokens (
    id VARCHAR(36) PRIMARY KEY,
    candidate_id VARCHAR(36) NOT NULL UNIQUE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cwt_candidate FOREIGN KEY (candidate_id)
        REFERENCES candidates(id) ON DELETE CASCADE
);

USE test;

ALTER TABLE candidates
    ADD COLUMN referred_by_user_id VARCHAR(36) NULL AFTER application_source_id,
    ADD CONSTRAINT fk_cand_referred_by
        FOREIGN KEY (referred_by_user_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_cand_referrer ON candidates(referred_by_user_id);

UPDATE shared_catalogs
SET value = 'Giới thiệu nội bộ'
WHERE id = 'cat-src-referral' AND type_code = 'APPLICATION_SOURCE';

CREATE TABLE IF NOT EXISTS candidate_interaction_events (
    id VARCHAR(36) PRIMARY KEY,
    candidate_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    actor_user_id VARCHAR(36) NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cie_candidate FOREIGN KEY (candidate_id)
        REFERENCES candidates(id) ON DELETE CASCADE,
    CONSTRAINT fk_cie_actor FOREIGN KEY (actor_user_id)
        REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_cie_candidate_time ON candidate_interaction_events(candidate_id, occurred_at);

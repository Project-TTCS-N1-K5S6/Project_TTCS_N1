CREATE DATABASE IF NOT EXISTS ttcs_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ttcs_db;

CREATE TABLE IF NOT EXISTS candidate_tag_categories (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS candidate_tags (
    id VARCHAR(36) PRIMARY KEY,
    category_id VARCHAR(36) NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_candidate_tag_category_name UNIQUE (category_id, name),
    CONSTRAINT fk_candidate_tag_category FOREIGN KEY (category_id)
        REFERENCES candidate_tag_categories(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS candidate_tag_assignments (
    candidate_id VARCHAR(36) NOT NULL,
    tag_id VARCHAR(36) NOT NULL,
    assigned_by VARCHAR(36) NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (candidate_id, tag_id),
    KEY idx_candidate_tag_assignments_tag (tag_id),
    CONSTRAINT fk_candidate_tag_assignment_candidate FOREIGN KEY (candidate_id)
        REFERENCES candidates(id) ON DELETE CASCADE,
    CONSTRAINT fk_candidate_tag_assignment_tag FOREIGN KEY (tag_id)
        REFERENCES candidate_tags(id) ON DELETE CASCADE,
    CONSTRAINT fk_candidate_tag_assignment_user FOREIGN KEY (assigned_by)
        REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS candidate_quick_ratings (
    candidate_id VARCHAR(36) NOT NULL,
    reviewer_id VARCHAR(36) NOT NULL,
    rating TINYINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (candidate_id, reviewer_id),
    KEY idx_candidate_quick_ratings_reviewer (reviewer_id),
    CONSTRAINT chk_candidate_quick_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_candidate_quick_rating_candidate FOREIGN KEY (candidate_id)
        REFERENCES candidates(id) ON DELETE CASCADE,
    CONSTRAINT fk_candidate_quick_rating_reviewer FOREIGN KEY (reviewer_id)
        REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS candidate_internal_notes (
    id VARCHAR(36) PRIMARY KEY,
    candidate_id VARCHAR(36) NOT NULL,
    author_id VARCHAR(36) NULL,
    author_name VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_candidate_internal_notes_candidate_created (candidate_id, created_at),
    CONSTRAINT fk_candidate_internal_note_candidate FOREIGN KEY (candidate_id)
        REFERENCES candidates(id) ON DELETE CASCADE,
    CONSTRAINT fk_candidate_internal_note_author FOREIGN KEY (author_id)
        REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO candidate_tag_categories (id, name) VALUES
('tag-category-fit', 'Mức độ phù hợp'),
('tag-category-follow-up', 'Theo dõi');

INSERT IGNORE INTO candidate_tags (id, category_id, name) VALUES
('candidate-tag-strong-fit', 'tag-category-fit', 'Phù hợp cao'),
('candidate-tag-potential-fit', 'tag-category-fit', 'Có tiềm năng'),
('candidate-tag-not-fit', 'tag-category-fit', 'Chưa phù hợp'),
('candidate-tag-contact', 'tag-category-follow-up', 'Cần liên hệ'),
('candidate-tag-priority', 'tag-category-follow-up', 'Ưu tiên xử lý');

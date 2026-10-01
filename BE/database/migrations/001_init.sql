-- ============================================================
--  TTCS HR System - Database Migration Script
--  Run this script to set up all required tables
-- ============================================================

-- Enable pgcrypto extension for UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- 1. USERS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  employee_code   VARCHAR(20) UNIQUE NOT NULL,
  full_name       VARCHAR(150) NOT NULL,
  email           VARCHAR(255) UNIQUE NOT NULL,
  password_hash   VARCHAR(255) NOT NULL,
  role            VARCHAR(50) NOT NULL DEFAULT 'nhan_su'
                    CHECK (role IN ('nhan_su', 'quan_tri', 'truong_phong', 'giam_doc')),
  avatar_url      TEXT,
  is_active       BOOLEAN NOT NULL DEFAULT TRUE,
  must_change_pw  BOOLEAN NOT NULL DEFAULT TRUE,
  token_version   INTEGER NOT NULL DEFAULT 1,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_email         ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_employee_code ON users (employee_code);
CREATE INDEX IF NOT EXISTS idx_users_role          ON users (role);

-- ============================================================
-- 2. USER SESSIONS TABLE (Server-side session tracking)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_sessions (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  session_id      VARCHAR(255) UNIQUE NOT NULL,
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  device_name     VARCHAR(200) DEFAULT 'Trình duyệt Web',
  ip_address      INET,
  user_agent      TEXT,
  is_active       BOOLEAN NOT NULL DEFAULT TRUE,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  last_activity   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expires_at      TIMESTAMPTZ NOT NULL,
  absolute_expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_sessions_session_id    ON user_sessions (session_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_user_id       ON user_sessions (user_id);
CREATE INDEX IF NOT EXISTS idx_user_sessions_is_active     ON user_sessions (is_active);
CREATE INDEX IF NOT EXISTS idx_user_sessions_last_activity ON user_sessions (last_activity);

-- ============================================================
-- 3. EXPRESS-SESSION STORE TABLE (connect-pg-simple)
-- ============================================================
CREATE TABLE IF NOT EXISTS session (
  sid     VARCHAR NOT NULL COLLATE "default",
  sess    JSON NOT NULL,
  expire  TIMESTAMP(6) NOT NULL
);

DO $$ BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'session_pkey'
      AND conrelid = 'session'::regclass
  ) THEN
    ALTER TABLE session
      ADD CONSTRAINT session_pkey PRIMARY KEY (sid) NOT DEFERRABLE INITIALLY IMMEDIATE;
  END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_session_expire ON session (expire);

-- ============================================================
-- 4. EVALUATION FORMS TABLE (Phiếu đánh giá nhân sự)
-- ============================================================
CREATE TABLE IF NOT EXISTS evaluation_forms (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  evaluator_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  evaluatee_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  period          VARCHAR(50) NOT NULL,         -- e.g., 'Q3-2026'
  status          VARCHAR(30) NOT NULL DEFAULT 'draft'
                    CHECK (status IN ('draft', 'submitted', 'approved', 'rejected')),
  scores          JSONB DEFAULT '{}',
  comments        TEXT,
  total_score     NUMERIC(5,2) DEFAULT 0,
  submitted_at    TIMESTAMPTZ,
  approved_at     TIMESTAMPTZ,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_eval_evaluator ON evaluation_forms (evaluator_id);
CREATE INDEX IF NOT EXISTS idx_eval_evaluatee ON evaluation_forms (evaluatee_id);
CREATE INDEX IF NOT EXISTS idx_eval_status    ON evaluation_forms (status);

-- ============================================================
-- 5. EVALUATION DRAFTS TABLE (Auto-save JSONB bản nháp)
-- ============================================================
CREATE TABLE IF NOT EXISTS evaluation_drafts (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  form_id         UUID REFERENCES evaluation_forms(id) ON DELETE CASCADE,
  draft_key       VARCHAR(255) NOT NULL,  -- unique key e.g. 'eval_draft_Q3-2026_usr_001'
  draft_data      JSONB NOT NULL DEFAULT '{}',
  saved_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (user_id, draft_key)
);

CREATE INDEX IF NOT EXISTS idx_drafts_user_id   ON evaluation_drafts (user_id);
CREATE INDEX IF NOT EXISTS idx_drafts_draft_key ON evaluation_drafts (draft_key);
CREATE INDEX IF NOT EXISTS idx_drafts_saved_at  ON evaluation_drafts (saved_at);
-- GIN index for JSONB search
CREATE INDEX IF NOT EXISTS idx_drafts_data_gin ON evaluation_drafts USING GIN (draft_data);

-- ============================================================
-- 6. TRIGGER: auto-update updated_at
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_users_updated_at') THEN
    CREATE TRIGGER trg_users_updated_at
      BEFORE UPDATE ON users
      FOR EACH ROW EXECUTE FUNCTION update_updated_at();
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_eval_updated_at') THEN
    CREATE TRIGGER trg_eval_updated_at
      BEFORE UPDATE ON evaluation_forms
      FOR EACH ROW EXECUTE FUNCTION update_updated_at();
  END IF;
END $$;

-- ============================================================
-- 7. SEED DATA (Initial users for development)
-- ============================================================

-- Password for all seed users: TempPassword123
-- Hash (cost 12): $2b$12$I7F49olS0n9I0CbcoyA45espI1BpaVbC.1Ze5.LHlQw56vG0hPHjm

INSERT INTO users (id, employee_code, full_name, email, password_hash, role, avatar_url, must_change_pw, token_version)
VALUES
  (
    '00000000-0000-0000-0000-000000000001',
    'NS001',
    'Hoàng Tiến Anh',
    'hoang.ta@company.com',
    '$2b$12$I7F49olS0n9I0CbcoyA45espI1BpaVbC.1Ze5.LHlQw56vG0hPHjm',
    'nhan_su',
    'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150',
    false,
    1
  ),
  (
    '00000000-0000-0000-0000-000000000002',
    'QT001',
    'Sìn Văn Cương',
    'cuong.sv@company.com',
    '$2b$12$I7F49olS0n9I0CbcoyA45espI1BpaVbC.1Ze5.LHlQw56vG0hPHjm',
    'quan_tri',
    'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150',
    false,
    1
  ),
  (
    '00000000-0000-0000-0000-000000000003',
    'TP001',
    'Nguyễn Thị Mai',
    'mai.nt@company.com',
    '$2b$12$I7F49olS0n9I0CbcoyA45espI1BpaVbC.1Ze5.LHlQw56vG0hPHjm',
    'truong_phong',
    'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=150',
    false,
    1
  )
ON CONFLICT (email) DO NOTHING;

-- Seed a sample evaluation form + draft
INSERT INTO evaluation_forms (id, evaluator_id, evaluatee_id, period, status)
VALUES (
  '10000000-0000-0000-0000-000000000001',
  '00000000-0000-0000-0000-000000000002',
  '00000000-0000-0000-0000-000000000001',
  'Q3-2026',
  'draft'
) ON CONFLICT DO NOTHING;

INSERT INTO evaluation_drafts (user_id, form_id, draft_key, draft_data)
VALUES (
  '00000000-0000-0000-0000-000000000002',
  '10000000-0000-0000-0000-000000000001',
  'eval_Q3-2026_NS001',
  '{"criteria":{"performance":8,"teamwork":7,"initiative":9},"comments":"Nhân viên có tinh thần làm việc tốt","overall":"Đạt yêu cầu"}'
) ON CONFLICT (user_id, draft_key) DO NOTHING;

SELECT 'Migration completed successfully!' AS result;

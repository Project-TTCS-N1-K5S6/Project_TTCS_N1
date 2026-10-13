CREATE TABLE IF NOT EXISTS recruitment_approval_requests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title VARCHAR(200) NOT NULL,
  details TEXT NOT NULL,
  created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING_APPROVAL'
    CHECK (status IN ('PENDING_APPROVAL', 'NEED_MORE_INFO', 'REJECTED', 'APPROVED')),
  current_step INTEGER NOT NULL DEFAULT 1 CHECK (current_step > 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS recruitment_approval_steps (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  request_id UUID NOT NULL REFERENCES recruitment_approval_requests(id) ON DELETE CASCADE,
  step_number INTEGER NOT NULL CHECK (step_number > 0),
  approver_user_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'NEED_MORE_INFO')),
  comment TEXT,
  acted_by UUID REFERENCES users(id) ON DELETE SET NULL,
  acted_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (request_id, step_number)
);

CREATE TABLE IF NOT EXISTS recruitment_approval_history (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  request_id UUID NOT NULL REFERENCES recruitment_approval_requests(id) ON DELETE CASCADE,
  step_number INTEGER,
  action VARCHAR(30) NOT NULL
    CHECK (action IN ('SUBMITTED', 'APPROVED', 'REJECTED', 'REQUEST_MORE_INFO', 'RESUBMITTED')),
  actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
  comment TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_recruitment_approval_creator
  ON recruitment_approval_requests (created_by, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_recruitment_approval_status
  ON recruitment_approval_requests (status, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_recruitment_approval_step_assignee
  ON recruitment_approval_steps (approver_user_id, status);
CREATE INDEX IF NOT EXISTS idx_recruitment_approval_history_request
  ON recruitment_approval_history (request_id, created_at, id);

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_approval_requests_updated_at') THEN
    CREATE TRIGGER trg_approval_requests_updated_at
      BEFORE UPDATE ON recruitment_approval_requests
      FOR EACH ROW EXECUTE FUNCTION update_updated_at();
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_approval_steps_updated_at') THEN
    CREATE TRIGGER trg_approval_steps_updated_at
      BEFORE UPDATE ON recruitment_approval_steps
      FOR EACH ROW EXECUTE FUNCTION update_updated_at();
  END IF;
END $$;

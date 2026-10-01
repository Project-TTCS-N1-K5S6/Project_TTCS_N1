-- Keep historical references while removing a member from the active directory.
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_users_active_directory
  ON users (created_at DESC, full_name ASC)
  WHERE deleted_at IS NULL;

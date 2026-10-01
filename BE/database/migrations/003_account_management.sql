-- Fields used by the internal account administration screen.
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS department VARCHAR(150) NOT NULL DEFAULT '',
  ADD COLUMN IF NOT EXISTS account_status VARCHAR(20) NOT NULL DEFAULT 'active';

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_account_status_check;
ALTER TABLE users
  ADD CONSTRAINT users_account_status_check
  CHECK (account_status IN ('pending', 'active', 'locked'));

UPDATE users
SET account_status = CASE WHEN is_active THEN 'active' ELSE 'locked' END
WHERE account_status IS NULL OR account_status NOT IN ('pending', 'active', 'locked');

CREATE INDEX IF NOT EXISTS idx_users_account_status ON users (account_status);

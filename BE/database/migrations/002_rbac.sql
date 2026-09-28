-- ============================================================
--  TTCS HR System - Migration 002: RBAC (Role-Based Access Control)
--  Adds 7 business roles and permission management
-- ============================================================

-- ============================================================
-- 1. Extend users.role CHECK constraint to include 7 roles
-- ============================================================
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users
  ADD CONSTRAINT users_role_check
    CHECK (role IN (
      'nhan_su',                    -- HR Staff
      'quan_tri',                   -- System Administrator
      'truong_phong',               -- Department Manager
      'giam_doc',                   -- Director
      'nguoi_phong_van',            -- Interviewer (NEW)
      'chuyen_vien_tuyen_dung',     -- Recruiter (NEW) - has data scope
      'quan_ly_tuyen_dung'          -- Recruitment Manager (NEW)
    ));

-- ============================================================
-- 2. ROLES TABLE (lookup for display names and metadata)
-- ============================================================
CREATE TABLE IF NOT EXISTS roles (
  id          SERIAL PRIMARY KEY,
  code        VARCHAR(50) UNIQUE NOT NULL,   -- matches users.role
  display_name VARCHAR(150) NOT NULL,
  description TEXT,
  is_system   BOOLEAN NOT NULL DEFAULT FALSE, -- system roles cannot be deleted
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 3. PERMISSIONS TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS permissions (
  id          SERIAL PRIMARY KEY,
  resource    VARCHAR(50) NOT NULL,   -- CANDIDATE, JOB, SALARY, INTERVIEW, PERMISSION
  action      VARCHAR(50) NOT NULL,   -- VIEW, CREATE, UPDATE, DELETE, APPROVE, EXPORT, VIEW_SALARY, MANAGE
  code        VARCHAR(100) UNIQUE NOT NULL,  -- e.g. 'candidate.view'
  display_name VARCHAR(200) NOT NULL,
  description TEXT,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (resource, action)
);

CREATE INDEX IF NOT EXISTS idx_permissions_resource ON permissions (resource);
CREATE INDEX IF NOT EXISTS idx_permissions_code ON permissions (code);

-- ============================================================
-- 4. ROLE_PERMISSIONS TABLE (many-to-many)
-- ============================================================
CREATE TABLE IF NOT EXISTS role_permissions (
  id            SERIAL PRIMARY KEY,
  role_code     VARCHAR(50) NOT NULL REFERENCES roles(code) ON DELETE CASCADE,
  permission_id INTEGER NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
  granted_by    UUID REFERENCES users(id),
  granted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (role_code, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_role_permissions_role ON role_permissions (role_code);
CREATE INDEX IF NOT EXISTS idx_role_permissions_perm ON role_permissions (permission_id);

-- ============================================================
-- 5. JOBS TABLE (for Recruiter data scope)
-- ============================================================
CREATE TABLE IF NOT EXISTS jobs (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code            VARCHAR(50) UNIQUE NOT NULL,
  title           VARCHAR(200) NOT NULL,
  department      VARCHAR(150),
  description     TEXT,
  salary_min      NUMERIC(15,2),
  salary_max      NUMERIC(15,2),
  open_count      INTEGER DEFAULT 1,
  deadline        DATE,
  status          VARCHAR(30) NOT NULL DEFAULT 'open'
                    CHECK (status IN ('open', 'closed', 'draft', 'cancelled')),
  recruiter_id    UUID REFERENCES users(id),
  manager_id      UUID REFERENCES users(id),
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_jobs_recruiter   ON jobs (recruiter_id);
CREATE INDEX IF NOT EXISTS idx_jobs_status      ON jobs (status);
CREATE INDEX IF NOT EXISTS idx_jobs_code        ON jobs (code);

-- ============================================================
-- 6. CANDIDATES TABLE
-- ============================================================
CREATE TABLE IF NOT EXISTS candidates (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code            VARCHAR(50) UNIQUE NOT NULL,
  full_name       VARCHAR(200) NOT NULL,
  email           VARCHAR(255),
  phone           VARCHAR(30),
  job_id          UUID REFERENCES jobs(id) ON DELETE SET NULL,
  position        VARCHAR(200),
  department      VARCHAR(150),
  salary_expected NUMERIC(15,2),
  status          VARCHAR(50) NOT NULL DEFAULT 'screening'
                    CHECK (status IN ('screening', 'interview', 'offer', 'hired', 'rejected')),
  notes           TEXT,
  created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_candidates_job_id ON candidates (job_id);
CREATE INDEX IF NOT EXISTS idx_candidates_status ON candidates (status);

-- Auto-update triggers
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_jobs_updated_at') THEN
    CREATE TRIGGER trg_jobs_updated_at
      BEFORE UPDATE ON jobs
      FOR EACH ROW EXECUTE FUNCTION update_updated_at();
  END IF;
END $$;

DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_candidates_updated_at') THEN
    CREATE TRIGGER trg_candidates_updated_at
      BEFORE UPDATE ON candidates
      FOR EACH ROW EXECUTE FUNCTION update_updated_at();
  END IF;
END $$;

-- ============================================================
-- 7. SEED: 7 Roles
-- ============================================================
INSERT INTO roles (code, display_name, description, is_system) VALUES
  ('quan_tri',                'Quan tri he thong',        'Toan quyen quan tri he thong va phan quyen', TRUE),
  ('quan_ly_tuyen_dung',      'Quan ly tuyen dung',       'Quan ly toan bo quy trinh va du lieu tuyen dung', TRUE),
  ('chuyen_vien_tuyen_dung',  'Chuyen vien tuyen dung',   'Tuyen dung - chi xem ung vien thuoc vi tri minh phu trach', TRUE),
  ('nguoi_phong_van',         'Nguoi phong van',           'Xem ho so ung vien, khong duoc xem dai luong', TRUE),
  ('truong_phong',            'Truong phong',              'Phe duyet ung vien, xem bao cao phong ban', TRUE),
  ('giam_doc',                'Giam doc',                  'Xem toan bo bao cao, phe duyet cap cao', TRUE),
  ('nhan_su',                 'Nhan su noi bo',            'Quan ly ho so nhan vien va danh gia', TRUE)
ON CONFLICT (code) DO UPDATE SET
  display_name = EXCLUDED.display_name,
  description  = EXCLUDED.description;

-- ============================================================
-- 8. SEED: Permissions
-- ============================================================
INSERT INTO permissions (resource, action, code, display_name, description) VALUES
  ('CANDIDATE', 'VIEW',    'candidate.view',    'Xem danh sach ung vien',    'Xem danh sach va chi tiet ung vien'),
  ('CANDIDATE', 'CREATE',  'candidate.create',  'Tao ho so ung vien',        'Tao moi ho so ung vien'),
  ('CANDIDATE', 'UPDATE',  'candidate.update',  'Cap nhat ho so ung vien',   'Chinh sua thong tin ung vien'),
  ('CANDIDATE', 'DELETE',  'candidate.delete',  'Xoa ho so ung vien',        'Xoa ho so ung vien khoi he thong'),
  ('CANDIDATE', 'APPROVE', 'candidate.approve', 'Phe duyet ung vien',        'Phe duyet/tu choi ung vien'),
  ('CANDIDATE', 'EXPORT',  'candidate.export',  'Xuat danh sach ung vien',   'Xuat file danh sach ung vien'),
  ('JOB',       'VIEW',    'job.view',          'Xem vi tri tuyen dung',     'Xem danh sach vi tri tuyen dung'),
  ('JOB',       'CREATE',  'job.create',        'Tao vi tri tuyen dung',     'Tao moi vi tri tuyen dung'),
  ('JOB',       'UPDATE',  'job.update',        'Cap nhat vi tri tuyen dung','Chinh sua thong tin vi tri tuyen dung'),
  ('JOB',       'DELETE',  'job.delete',        'Xoa vi tri tuyen dung',     'Xoa vi tri tuyen dung'),
  ('SALARY',    'VIEW',    'salary.view',       'Xem dai luong',             'Xem thong tin luong va dai luong ung vien'),
  ('INTERVIEW', 'VIEW',    'interview.view',    'Xem lich phong van',        'Xem lich va ket qua phong van'),
  ('INTERVIEW', 'CREATE',  'interview.create',  'Tao lich phong van',        'Len lich phong van'),
  ('INTERVIEW', 'UPDATE',  'interview.update',  'Cap nhat lich phong van',   'Chinh sua lich phong van'),
  ('EVALUATION','VIEW',    'evaluation.view',   'Xem phieu danh gia',        'Xem ket qua danh gia nhan su'),
  ('EVALUATION','CREATE',  'evaluation.create', 'Tao phieu danh gia',        'Tao phieu danh gia nhan su'),
  ('PERMISSION','MANAGE',  'permission.manage', 'Quan ly phan quyen',        'Cau hinh quyen cho cac vai tro')
ON CONFLICT (resource, action) DO NOTHING;

-- ============================================================
-- 9. SEED: Role Permissions
-- ============================================================

-- quan_tri: all permissions
INSERT INTO role_permissions (role_code, permission_id)
SELECT 'quan_tri', id FROM permissions
ON CONFLICT (role_code, permission_id) DO NOTHING;

-- quan_ly_tuyen_dung: all except permission.manage, candidate.delete, job.delete
INSERT INTO role_permissions (role_code, permission_id)
SELECT 'quan_ly_tuyen_dung', id FROM permissions
WHERE code NOT IN ('permission.manage', 'candidate.delete', 'job.delete')
ON CONFLICT (role_code, permission_id) DO NOTHING;

-- chuyen_vien_tuyen_dung: own candidates + salary + interviews (data scope enforced in backend)
INSERT INTO role_permissions (role_code, permission_id)
SELECT 'chuyen_vien_tuyen_dung', id FROM permissions
WHERE code IN (
  'candidate.view', 'candidate.create', 'candidate.update',
  'job.view', 'salary.view',
  'interview.view', 'interview.create', 'interview.update',
  'evaluation.view', 'evaluation.create'
)
ON CONFLICT (role_code, permission_id) DO NOTHING;

-- nguoi_phong_van: view candidates only, NO salary
INSERT INTO role_permissions (role_code, permission_id)
SELECT 'nguoi_phong_van', id FROM permissions
WHERE code IN (
  'candidate.view', 'job.view',
  'interview.view', 'evaluation.view', 'evaluation.create'
)
ON CONFLICT (role_code, permission_id) DO NOTHING;

-- truong_phong: view + approve + salary
INSERT INTO role_permissions (role_code, permission_id)
SELECT 'truong_phong', id FROM permissions
WHERE code IN (
  'candidate.view', 'candidate.approve', 'candidate.export',
  'job.view', 'salary.view',
  'interview.view', 'evaluation.view', 'evaluation.create'
)
ON CONFLICT (role_code, permission_id) DO NOTHING;

-- giam_doc: view + approve + salary + export
INSERT INTO role_permissions (role_code, permission_id)
SELECT 'giam_doc', id FROM permissions
WHERE code IN (
  'candidate.view', 'candidate.approve', 'candidate.export',
  'job.view', 'salary.view',
  'interview.view', 'evaluation.view'
)
ON CONFLICT (role_code, permission_id) DO NOTHING;

-- nhan_su: manage candidates/jobs, no salary, no permission management
INSERT INTO role_permissions (role_code, permission_id)
SELECT 'nhan_su', id FROM permissions
WHERE code IN (
  'candidate.view', 'candidate.create', 'candidate.update',
  'job.view', 'job.create', 'job.update',
  'interview.view', 'interview.create', 'interview.update',
  'evaluation.view', 'evaluation.create'
)
ON CONFLICT (role_code, permission_id) DO NOTHING;

-- ============================================================
-- 10. SEED: Sample Jobs & Candidates (for testing)
-- ============================================================
INSERT INTO users (id, employee_code, full_name, email, password_hash, role, must_change_pw, token_version)
VALUES
  (
    '00000000-0000-0000-0000-000000000004',
    'PV001',
    'Pham Van Dung',
    'dung.pv@company.com',
    '$2b$12$I7F49olS0n9I0CbcoyA45espI1BpaVbC.1Ze5.LHlQw56vG0hPHjm',
    'nguoi_phong_van',
    false, 1
  ),
  (
    '00000000-0000-0000-0000-000000000005',
    'TV001',
    'Tran Van Em',
    'em.tv@company.com',
    '$2b$12$I7F49olS0n9I0CbcoyA45espI1BpaVbC.1Ze5.LHlQw56vG0hPHjm',
    'chuyen_vien_tuyen_dung',
    false, 1
  ),
  (
    '00000000-0000-0000-0000-000000000006',
    'QL001',
    'Nguyen Thi Fuong',
    'fuong.nt@company.com',
    '$2b$12$I7F49olS0n9I0CbcoyA45espI1BpaVbC.1Ze5.LHlQw56vG0hPHjm',
    'quan_ly_tuyen_dung',
    false, 1
  )
ON CONFLICT (email) DO NOTHING;

INSERT INTO jobs (id, code, title, department, salary_min, salary_max, open_count, status, recruiter_id, deadline)
VALUES
  (
    'a0000000-0000-0000-0000-000000000001',
    'VT-2026-001',
    'Senior React Developer',
    'Cong nghe thong tin',
    25000000, 45000000, 3, 'open',
    '00000000-0000-0000-0000-000000000005',
    '2026-10-30'
  ),
  (
    'a0000000-0000-0000-0000-000000000002',
    'VT-2026-002',
    'Chuyen vien Tuyen dung Noi bo',
    'Nhan su',
    15000000, 25000000, 1, 'open',
    '00000000-0000-0000-0000-000000000001',
    '2026-10-15'
  )
ON CONFLICT (code) DO NOTHING;

INSERT INTO candidates (id, code, full_name, email, job_id, position, department, salary_expected, status)
VALUES
  (
    'b0000000-0000-0000-0000-000000000001',
    'UV-2026-001',
    'Nguyen Van An',
    'nva@gmail.com',
    'a0000000-0000-0000-0000-000000000001',
    'Senior React Developer',
    'Cong nghe thong tin',
    38000000,
    'interview'
  ),
  (
    'b0000000-0000-0000-0000-000000000002',
    'UV-2026-002',
    'Tran Thi Binh',
    'ttb@gmail.com',
    'a0000000-0000-0000-0000-000000000001',
    'Senior React Developer',
    'Cong nghe thong tin',
    32000000,
    'screening'
  ),
  (
    'b0000000-0000-0000-0000-000000000003',
    'UV-2026-003',
    'Le Hoang Cuong',
    'lhc@gmail.com',
    'a0000000-0000-0000-0000-000000000002',
    'Chuyen vien Tuyen dung Noi bo',
    'Nhan su',
    20000000,
    'offer'
  )
ON CONFLICT (code) DO NOTHING;

SELECT 'Migration 002 RBAC completed!' AS result;

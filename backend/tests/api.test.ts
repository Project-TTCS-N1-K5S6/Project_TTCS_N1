import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import request from 'supertest';
import { app } from '../src/app';
import { pool } from '../src/database/db';
import { seedData } from '../src/database/seed';

describe('IRMS SPRINT 1 - Comprehensive Test Suite', () => {
  let adminToken: string;
  let hrToken: string;
  let recruiterToken: string;
  let interviewerToken: string;
  let adminUserId: string;
  let adminRoleId: string;
  let testCreatedUserId: string;

  beforeAll(async () => {
    // Delete any previous test users and outbox
    await pool.query("DELETE FROM email_outbox");
    await pool.query("DELETE FROM users WHERE email LIKE '%@company.local' AND email NOT IN ('admin@company.local', 'hr.manager@company.local', 'recruiter@company.local', 'hiring.manager@company.local', 'interviewer@company.local', 'approver@company.local', 'leader.tech@company.local')");
    // Re-seed to guarantee clean initial state
    await seedData();

    // Query admin user and role id
    const adminUserRes = await pool.query("SELECT id FROM users WHERE email = 'admin@company.local'");
    adminUserId = adminUserRes.rows[0].id;

    const adminRoleRes = await pool.query("SELECT id FROM roles WHERE code = 'ADMIN'");
    adminRoleId = adminRoleRes.rows[0].id;
  });

  afterAll(async () => {
    await pool.end();
  });

  // ============================================================
  // S1-01 & S1-02: AUTHENTICATION, LOCKOUT & SESSION REFRESH
  // ============================================================
  describe('S1-01 & S1-02: Authentication & Sessions', () => {
    it('1. Đăng nhập thành công với tài khoản Admin', async () => {
      const res = await request(app)
        .post('/api/auth/login')
        .send({ email: 'admin@company.local', password: 'Admin@123456' });

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data.accessToken).toBeDefined();
      expect(res.body.data.user.email).toBe('admin@company.local');
      expect(res.body.data.user.roles).toContain('ADMIN');
      adminToken = res.body.data.accessToken;

      // Verify cookie
      const cookies = res.headers['set-cookie'];
      expect(cookies).toBeDefined();
      expect(cookies[0]).toContain('irms_refresh_token');
    });

    it('2. Đăng nhập sai mật khẩu trả về generic error và không tiết lộ email', async () => {
      const res = await request(app)
        .post('/api/auth/login')
        .send({ email: 'admin@company.local', password: 'WrongPassword123' });

      expect(res.status).toBe(400);
      expect(res.body.success).toBe(false);
      expect(res.body.message).toBe('Email hoặc mật khẩu không chính xác.');
    });

    it('3. Đăng nhập với email không tồn tại trả về generic error giống hệt', async () => {
      const res = await request(app)
        .post('/api/auth/login')
        .send({ email: 'nonexistent@company.local', password: 'AnyPassword123' });

      expect(res.status).toBe(400);
      expect(res.body.success).toBe(false);
      expect(res.body.message).toBe('Email hoặc mật khẩu không chính xác.');
    });

    it('4. Nhập sai 5 lần liên tiếp sẽ tạm khóa tài khoản 15 phút (S1-01 AC4)', async () => {
      // Create a test user for lockout test
      const testEmail = 'lockout.test@company.local';
      await pool.query(
        `INSERT INTO users (employee_code, full_name, email, password_hash, status)
         VALUES ('EMP999', 'Lockout Test', $1, '$2a$10$wT0l0/iS/eK4QdFqI8pQveC7w3rT9J9N4LhM1j/Yt4v7qO2t5qLq2', 'ACTIVE')
         ON CONFLICT (email) DO UPDATE SET failed_login_attempts = 0, locked_until = NULL`,
        [testEmail]
      );

      // Attempt 1 to 4: should fail with 400
      for (let i = 1; i <= 4; i++) {
        const res = await request(app)
          .post('/api/auth/login')
          .send({ email: testEmail, password: 'WrongPassword!' });
        expect(res.status).toBe(400);
      }

      // Attempt 5: should trigger temporary lockout (403)
      const res5 = await request(app)
        .post('/api/auth/login')
        .send({ email: testEmail, password: 'WrongPassword!' });

      expect(res5.status).toBe(403);
      expect(res5.body.message).toBe('Tài khoản đang tạm khóa. Vui lòng thử lại sau.');

      // Subsequent attempt while locked: should also be rejected with 403
      const resLocked = await request(app)
        .post('/api/auth/login')
        .send({ email: testEmail, password: 'WrongPassword!' });

      expect(resLocked.status).toBe(403);
      expect(resLocked.body.message).toBe('Tài khoản đang tạm khóa. Vui lòng thử lại sau.');
    });

    it('5. Làm mới Access Token thông qua Refresh Token thành công (S1-02)', async () => {
      // First login to get refresh token cookie
      const loginRes = await request(app)
        .post('/api/auth/login')
        .send({ email: 'admin@company.local', password: 'Admin@123456' });

      const cookie = loginRes.headers['set-cookie'];

      const refreshRes = await request(app)
        .post('/api/auth/refresh')
        .set('Cookie', cookie)
        .send({});

      expect(refreshRes.status).toBe(200);
      expect(refreshRes.body.success).toBe(true);
      expect(refreshRes.body.data.accessToken).toBeDefined();
    });

    it('6. Refresh Token đã bị thu hồi hoặc không hợp lệ sẽ trả về 401', async () => {
      const res = await request(app)
        .post('/api/auth/refresh')
        .send({ refreshToken: 'invalid_or_revoked_token_12345' });

      expect(res.status).toBe(401);
      expect(res.body.message).toBe('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
    });

    it('7. Đăng xuất làm mất hiệu lực Refresh Token phía server (S1-02 AC2)', async () => {
      const loginRes = await request(app)
        .post('/api/auth/login')
        .send({ email: 'admin@company.local', password: 'Admin@123456' });

      const rawRefreshToken = loginRes.body.data.refreshToken;
      const cookie = loginRes.headers['set-cookie'];

      const logoutRes = await request(app)
        .post('/api/auth/logout')
        .set('Cookie', cookie)
        .send({ refreshToken: rawRefreshToken });

      expect(logoutRes.status).toBe(200);

      // Attempting to refresh with the logged out token must fail with 401
      const retryRefresh = await request(app)
        .post('/api/auth/refresh')
        .send({ refreshToken: rawRefreshToken });

      expect(retryRefresh.status).toBe(401);
    });

    it('8. Lấy thông tin phiên hiện tại qua /api/auth/me', async () => {
      const res = await request(app)
        .get('/api/auth/me')
        .set('Authorization', `Bearer ${adminToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data.email).toBe('admin@company.local');
      expect(res.body.data.roles).toContain('ADMIN');
      expect(res.body.data.permissions).toBeInstanceOf(Array);
    });
  });

  // ============================================================
  // S1-03 & S1-04: FORGOT / RESET / CHANGE PASSWORD
  // ============================================================
  describe('S1-03 & S1-04: Password Management', () => {
    it('9. Quên mật khẩu trả về generic response cho cả email tồn tại và không tồn tại', async () => {
      // Existing email
      const res1 = await request(app)
        .post('/api/auth/forgot-password')
        .send({ email: 'admin@company.local' });

      expect(res1.status).toBe(200);
      expect(res1.body.message).toBe(
        'Nếu email tồn tại trong hệ thống, chúng tôi sẽ gửi hướng dẫn đặt lại mật khẩu.'
      );

      // Non-existing email
      const res2 = await request(app)
        .post('/api/auth/forgot-password')
        .send({ email: 'unknown_person@company.local' });

      expect(res2.status).toBe(200);
      expect(res2.body.message).toBe(
        'Nếu email tồn tại trong hệ thống, chúng tôi sẽ gửi hướng dẫn đặt lại mật khẩu.'
      );
    });

    it('10. Đặt lại mật khẩu thành công với token hợp lệ và vô hiệu hóa sau 1 lần dùng', async () => {
      // Trigger forgot password for hr.manager
      await request(app)
        .post('/api/auth/forgot-password')
        .send({ email: 'hr.manager@company.local' });

      // Retrieve the queued email payload to get the raw token
      const outboxRes = await pool.query(
        "SELECT payload FROM email_outbox WHERE recipient = 'hr.manager@company.local' ORDER BY created_at DESC LIMIT 1"
      );
      expect(outboxRes.rowCount).toBe(1);
      const payload = outboxRes.rows[0].payload;
      const resetUrl = typeof payload === 'string' ? JSON.parse(payload).resetUrl : payload.resetUrl;
      const token = new URL(resetUrl).searchParams.get('token');
      expect(token).toBeDefined();

      // Reset password with new valid password
      const resetRes = await request(app)
        .post('/api/auth/reset-password')
        .send({
          token,
          newPassword: 'NewPassword@2026',
          confirmPassword: 'NewPassword@2026',
        });

      expect(resetRes.status).toBe(200);
      expect(resetRes.body.message).toContain('Đặt lại mật khẩu thành công');

      // Attempting to reuse the same token must fail
      const reuseRes = await request(app)
        .post('/api/auth/reset-password')
        .send({
          token,
          newPassword: 'AnotherPassword@2026',
          confirmPassword: 'AnotherPassword@2026',
        });

      expect(reuseRes.status).toBe(400);
      expect(reuseRes.body.message).toContain('đã được sử dụng');
    });

    it('11. Đổi mật khẩu thành công và kiểm tra chính sách mật khẩu (S1-04)', async () => {
      // Login with hr.manager with new password
      const loginRes = await request(app)
        .post('/api/auth/login')
        .send({ email: 'hr.manager@company.local', password: 'NewPassword@2026' });

      const hrAuthToken = loginRes.body.data.accessToken;

      // Fail if new password has no number or < 8 chars
      const weakRes = await request(app)
        .post('/api/auth/change-password')
        .set('Authorization', `Bearer ${hrAuthToken}`)
        .send({
          currentPassword: 'NewPassword@2026',
          newPassword: 'short',
          confirmPassword: 'short',
        });

      expect(weakRes.status).toBe(422);

      // Change password with valid password
      const changeRes = await request(app)
        .post('/api/auth/change-password')
        .set('Authorization', `Bearer ${hrAuthToken}`)
        .send({
          currentPassword: 'NewPassword@2026',
          newPassword: 'Admin@123456',
          confirmPassword: 'Admin@123456',
        });

      expect(changeRes.status).toBe(200);
      expect(changeRes.body.message).toBe('Đổi mật khẩu thành công.');
    });
  });

  // ============================================================
  // S1-05 & S1-06: RBAC & PERMISSION-BASED AUTHORIZATION (>= 3 ROLES)
  // ============================================================
  describe('S1-05 & S1-06: Role-Based Access Control for >= 3 Roles', () => {
    beforeAll(async () => {
      // Obtain tokens for HR_MANAGER, RECRUITER, INTERVIEWER
      const hrLogin = await request(app)
        .post('/api/auth/login')
        .send({ email: 'hr.manager@company.local', password: 'Admin@123456' });
      hrToken = hrLogin.body.data.accessToken;

      const recruiterLogin = await request(app)
        .post('/api/auth/login')
        .send({ email: 'recruiter@company.local', password: 'Admin@123456' });
      recruiterToken = recruiterLogin.body.data.accessToken;

      const interviewerLogin = await request(app)
        .post('/api/auth/login')
        .send({ email: 'interviewer@company.local', password: 'Admin@123456' });
      interviewerToken = interviewerLogin.body.data.accessToken;
    });

    it('12. Role 1 - ADMIN truy cập GET /api/users thành công (200)', async () => {
      const res = await request(app)
        .get('/api/users')
        .set('Authorization', `Bearer ${adminToken}`);

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
      expect(res.body.data.items).toBeInstanceOf(Array);
    });

    it('13. Role 2 - HR_MANAGER truy cập GET /api/users thành công vì có permission users.view (200)', async () => {
      const res = await request(app)
        .get('/api/users')
        .set('Authorization', `Bearer ${hrToken}`);

      expect(res.status).toBe(200);
      expect(res.body.success).toBe(true);
    });

    it('14. Role 3 - RECRUITER truy cập GET /api/users bị từ chối với 403 Forbidden', async () => {
      const res = await request(app)
        .get('/api/users')
        .set('Authorization', `Bearer ${recruiterToken}`);

      expect(res.status).toBe(403);
      expect(res.body.success).toBe(false);
      expect(res.body.message).toBe('Bạn không có quyền thực hiện thao tác này.');
    });

    it('15. Role 4 - INTERVIEWER truy cập GET /api/users bị từ chối với 403 Forbidden', async () => {
      const res = await request(app)
        .get('/api/users')
        .set('Authorization', `Bearer ${interviewerToken}`);

      expect(res.status).toBe(403);
      expect(res.body.success).toBe(false);
      expect(res.body.message).toBe('Bạn không có quyền thực hiện thao tác này.');
    });
  });

  // ============================================================
  // S1-08: USER MANAGEMENT (CRUD, PAGINATION, SEARCH, FILTER)
  // ============================================================
  describe('S1-08: Internal User Management', () => {
    it('16. Admin tạo tài khoản người dùng mới thành công và sinh mật khẩu tạm', async () => {
      const rolesRes = await pool.query("SELECT id FROM roles WHERE code = 'RECRUITER'");
      const recruiterRoleId = rolesRes.rows[0].id;

      const deptRes = await pool.query("SELECT id FROM departments WHERE code = 'HR'");
      const deptId = deptRes.rows[0].id;

      const res = await request(app)
        .post('/api/users')
        .set('Authorization', `Bearer ${adminToken}`)
        .send({
          fullName: 'Vũ Thị Minh Anh',
          email: 'minhanh.vu@company.local',
          phone: '0988776655',
          jobTitle: 'Talent Acquisition Executive',
          departmentId: deptId,
          roleIds: [recruiterRoleId],
        });

      expect(res.status).toBe(201);
      expect(res.body.success).toBe(true);
      expect(res.body.data.user.email).toBe('minhanh.vu@company.local');
      expect(res.body.data.temporaryPassword).toBeDefined();

      testCreatedUserId = res.body.data.user.id;

      // Verify email outbox received activation email
      const outbox = await pool.query(
        "SELECT * FROM email_outbox WHERE recipient = 'minhanh.vu@company.local' AND template = 'ACCOUNT_ACTIVATION'"
      );
      expect(outbox.rowCount).toBe(1);
    });

    it('17. Tạo tài khoản trùng email bị từ chối với 409 Conflict', async () => {
      const rolesRes = await pool.query("SELECT id FROM roles WHERE code = 'RECRUITER'");
      const res = await request(app)
        .post('/api/users')
        .set('Authorization', `Bearer ${adminToken}`)
        .send({
          fullName: 'Trùng Email',
          email: 'minhanh.vu@company.local',
          roleIds: [rolesRes.rows[0].id],
        });

      expect(res.status).toBe(409);
      expect(res.body.message).toBe('Email đã tồn tại trong hệ thống.');
    });

    it('18. Tìm kiếm, lọc và phân trang 20 dòng mặc định', async () => {
      const res = await request(app)
        .get('/api/users?page=1&pageSize=20&search=minhanh')
        .set('Authorization', `Bearer ${adminToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data.pageSize).toBe(20);
      expect(res.body.data.items.length).toBeGreaterThanOrEqual(1);
      expect(res.body.data.items[0].email).toBe('minhanh.vu@company.local');
    });

    it('19. Cập nhật thông tin tài khoản người dùng thành công', async () => {
      const res = await request(app)
        .put(`/api/users/${testCreatedUserId}`)
        .set('Authorization', `Bearer ${adminToken}`)
        .send({
          fullName: 'Vũ Thị Minh Anh (Đã cập nhật)',
          phone: '0911223344',
          jobTitle: 'Lead Talent Acquisition',
        });

      expect(res.status).toBe(200);
      expect(res.body.data.fullName).toBe('Vũ Thị Minh Anh (Đã cập nhật)');
    });
  });

  // ============================================================
  // S1-09: ASSIGN & REVOKE ROLES (AND SELF-ADMIN REVOKE RULE)
  // ============================================================
  describe('S1-09: Assign & Revoke Roles', () => {
    it('20. Gán thêm vai trò cho người dùng (Một người nhiều vai trò)', async () => {
      const interviewerRole = await pool.query("SELECT id FROM roles WHERE code = 'INTERVIEWER'");
      const roleId = interviewerRole.rows[0].id;

      const res = await request(app)
        .post(`/api/users/${testCreatedUserId}/roles`)
        .set('Authorization', `Bearer ${adminToken}`)
        .send({ roleId });

      expect(res.status).toBe(200);
      expect(res.body.message).toBe('Gán vai trò thành công.');

      // Verify user now has both roles
      const userRes = await request(app)
        .get(`/api/users/${testCreatedUserId}`)
        .set('Authorization', `Bearer ${adminToken}`);

      const roles = userRes.body.data.roles.map((r: any) => r.code);
      expect(roles).toContain('RECRUITER');
      expect(roles).toContain('INTERVIEWER');
    });

    it('21. Thu hồi vai trò thành công', async () => {
      const interviewerRole = await pool.query("SELECT id FROM roles WHERE code = 'INTERVIEWER'");
      const roleId = interviewerRole.rows[0].id;

      const res = await request(app)
        .delete(`/api/users/${testCreatedUserId}/roles/${roleId}`)
        .set('Authorization', `Bearer ${adminToken}`);

      expect(res.status).toBe(200);
      expect(res.body.message).toBe('Thu hồi vai trò thành công.');
    });

    it('22. Admin KHÔNG ĐƯỢC tự thu hồi quyền ADMIN của chính mình (S1-09 AC5)', async () => {
      const res = await request(app)
        .delete(`/api/users/${adminUserId}/roles/${adminRoleId}`)
        .set('Authorization', `Bearer ${adminToken}`);

      expect(res.status).toBe(400);
      expect(res.body.message).toBe('Bạn không thể tự thu hồi quyền quản trị của chính mình.');
    });
  });

  // ============================================================
  // S1-10: LOCK & UNLOCK ACCOUNT (HANDOVER WARNING CHECK)
  // ============================================================
  describe('S1-10: Account Lock & Handover Warning', () => {
    it('23. Khóa tài khoản người dùng đang phụ trách vị trí OPEN sẽ hiển thị cảnh báo bàn giao', async () => {
      // recruiter@company.local is assigned to OPEN requisitions
      const recruiterUser = await pool.query("SELECT id FROM users WHERE email = 'recruiter@company.local'");
      const recruiterId = recruiterUser.rows[0].id;

      const res = await request(app)
        .post(`/api/users/${recruiterId}/lock`)
        .set('Authorization', `Bearer ${adminToken}`)
        .send({ reason: 'Nhân sự chuẩn bị nghỉ việc' });

      expect(res.status).toBe(200);
      expect(res.body.data.requiresHandoverWarning).toBe(true);
      expect(res.body.data.activeRequisitionCount).toBeGreaterThan(0);
      expect(res.body.data.message).toContain('vị trí tuyển dụng');
    });

    it('24. Khóa tài khoản khi xác nhận (force: true) hoặc khi không có vị trí phụ trách', async () => {
      const res = await request(app)
        .post(`/api/users/${testCreatedUserId}/lock`)
        .set('Authorization', `Bearer ${adminToken}`)
        .send({ reason: 'Nghỉ việc theo quy định công ty' });

      expect(res.status).toBe(200);
      expect(res.body.data.requiresHandoverWarning).toBe(false);
      expect(res.body.message).toContain('Đã khóa tài khoản thành công');

      // Verify user cannot login anymore
      const loginAttempt = await request(app)
        .post('/api/auth/login')
        .send({ email: 'minhanh.vu@company.local', password: 'AnyPassword123' });

      expect(loginAttempt.status).toBe(403);
      expect(loginAttempt.body.message).toContain('đã bị khóa');
    });

    it('25. Mở khóa tài khoản thành công (S1-10)', async () => {
      const res = await request(app)
        .post(`/api/users/${testCreatedUserId}/unlock`)
        .set('Authorization', `Bearer ${adminToken}`);

      expect(res.status).toBe(200);
      expect(res.body.message).toBe('Đã mở khóa tài khoản thành công.');

      // Check status in DB is ACTIVE
      const checkRes = await pool.query('SELECT status FROM users WHERE id = $1', [testCreatedUserId]);
      expect(checkRes.rows[0].status).toBe('ACTIVE');
    });
  });

  // ============================================================
  // ROLES & PERMISSIONS MANAGEMENT
  // ============================================================
  describe('Roles & Permissions Matrix APIs', () => {
    it('26. Lấy danh sách 7 vai trò hệ thống', async () => {
      const res = await request(app)
        .get('/api/roles')
        .set('Authorization', `Bearer ${adminToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data.length).toBeGreaterThanOrEqual(7);
    });

    it('27. Lấy danh sách quyền hệ thống gom nhóm theo module', async () => {
      const res = await request(app)
        .get('/api/permissions')
        .set('Authorization', `Bearer ${adminToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data.all).toBeInstanceOf(Array);
      expect(res.body.data.byModule).toBeDefined();
    });

    it('28. Cập nhật ma trận phân quyền cho vai trò', async () => {
      const recruiterRole = await pool.query("SELECT id FROM roles WHERE code = 'RECRUITER'");
      const roleId = recruiterRole.rows[0].id;

      const permsRes = await pool.query("SELECT id FROM permissions WHERE code IN ('requisitions.view', 'candidates.view')");
      const permIds = permsRes.rows.map((p) => p.id);

      const res = await request(app)
        .put(`/api/roles/${roleId}/permissions`)
        .set('Authorization', `Bearer ${adminToken}`)
        .send({ permissionIds: permIds });

      expect(res.status).toBe(200);
      expect(res.body.message).toBe('Cập nhật phân quyền cho vai trò thành công.');
    });
  });

  // ============================================================
  // AUDIT LOGS
  // ============================================================
  describe('Audit Logs APIs', () => {
    it('29. Xem nhật ký hệ thống với quyền audit.view', async () => {
      const res = await request(app)
        .get('/api/audit-logs?page=1&pageSize=20')
        .set('Authorization', `Bearer ${adminToken}`);

      expect(res.status).toBe(200);
      expect(res.body.data.items).toBeInstanceOf(Array);
      expect(res.body.data.total).toBeGreaterThan(0);
    });
  });
});

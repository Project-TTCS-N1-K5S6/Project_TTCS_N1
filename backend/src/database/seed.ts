import bcrypt from 'bcryptjs';
import { pool, withTransaction } from './db';

async function seedData() {
  console.log('[Seed] Starting database seed...');

  try {
    await withTransaction(async (client) => {
      // 1. Seed Departments
      console.log('[Seed] Seeding departments...');
      const deptInserts = [
        { code: 'HR', name: 'Ban Nhân sự', description: 'Quản lý nhân sự và tuyển dụng nội bộ' },
        { code: 'TECH', name: 'Phòng Kỹ thuật', description: 'Phát triển phần mềm và hạ tầng công nghệ' },
        { code: 'SALES', name: 'Phòng Kinh doanh', description: 'Kinh doanh và phát triển thị trường' },
        { code: 'MKT', name: 'Phòng Marketing', description: 'Truyền thông và marketing thương hiệu' },
      ];

      const deptMap: Record<string, string> = {};
      for (const d of deptInserts) {
        const res = await client.query(
          `INSERT INTO departments (code, name, description)
           VALUES ($1, $2, $3)
           ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description
           RETURNING id, code`,
          [d.code, d.name, d.description]
        );
        deptMap[res.rows[0].code] = res.rows[0].id;
      }

      // 2. Seed Roles
      console.log('[Seed] Seeding roles...');
      const rolesData = [
        { code: 'ADMIN', name: 'Quản trị hệ thống', description: 'Toàn quyền cấu hình và quản trị hệ thống IRMS' },
        { code: 'HR_MANAGER', name: 'Trưởng phòng Nhân sự', description: 'Quản lý chiến lược nhân sự, xem danh sách tài khoản, nhật ký và báo cáo' },
        { code: 'RECRUITER', name: 'Chuyên viên tuyển dụng', description: 'Tạo yêu cầu, quản lý hồ sơ ứng viên và điều phối tuyển dụng' },
        { code: 'HIRING_MANAGER', name: 'Trưởng bộ phận tuyển dụng', description: 'Đề xuất yêu cầu tuyển dụng, tham gia phỏng vấn và đánh giá ứng viên' },
        { code: 'INTERVIEWER', name: 'Người phỏng vấn', description: 'Xem thông tin ứng viên được phân công và nhập phiếu đánh giá phỏng vấn' },
        { code: 'APPROVER', name: 'Người phê duyệt', description: 'Phê duyệt yêu cầu tuyển dụng và đề xuất tuyển dụng (offer)' },
        { code: 'CANDIDATE', name: 'Ứng viên', description: 'Tài khoản ứng viên nộp hồ sơ ứng tuyển ngoài' },
      ];

      const roleMap: Record<string, string> = {};
      for (const r of rolesData) {
        const res = await client.query(
          `INSERT INTO roles (code, name, description, is_system_role)
           VALUES ($1, $2, $3, true)
           ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description
           RETURNING id, code`,
          [r.code, r.name, r.description]
        );
        roleMap[res.rows[0].code] = res.rows[0].id;
      }

      // 3. Seed Permissions
      console.log('[Seed] Seeding permissions...');
      const permissionsData = [
        // Users
        { code: 'users.view', name: 'Xem người dùng', module: 'users', action: 'view', description: 'Xem danh sách và chi tiết tài khoản nội bộ' },
        { code: 'users.create', name: 'Tạo người dùng', module: 'users', action: 'create', description: 'Tạo tài khoản nội bộ mới và cấp mật khẩu tạm' },
        { code: 'users.update', name: 'Cập nhật người dùng', module: 'users', action: 'update', description: 'Chỉnh sửa thông tin hồ sơ tài khoản' },
        { code: 'users.lock', name: 'Khóa tài khoản', module: 'users', action: 'lock', description: 'Khóa tài khoản nhân sự nghỉ việc hoặc có lý do' },
        { code: 'users.unlock', name: 'Mở khóa tài khoản', module: 'users', action: 'unlock', description: 'Mở khóa tài khoản cho phép đăng nhập lại' },

        // Roles
        { code: 'roles.view', name: 'Xem vai trò', module: 'roles', action: 'view', description: 'Xem danh sách và chi tiết các vai trò' },
        { code: 'roles.create', name: 'Tạo vai trò', module: 'roles', action: 'create', description: 'Tạo mới vai trò người dùng' },
        { code: 'roles.update', name: 'Cập nhật vai trò', module: 'roles', action: 'update', description: 'Cập nhật thông tin vai trò' },
        { code: 'roles.assign', name: 'Gán vai trò', module: 'roles', action: 'assign', description: 'Gán một hoặc nhiều vai trò cho tài khoản' },
        { code: 'roles.revoke', name: 'Thu hồi vai trò', module: 'roles', action: 'revoke', description: 'Thu hồi vai trò của người dùng' },

        // Permissions
        { code: 'permissions.view', name: 'Xem quyền', module: 'permissions', action: 'view', description: 'Xem ma trận phân quyền hệ thống' },
        { code: 'permissions.manage', name: 'Cấu hình quyền', module: 'permissions', action: 'manage', description: 'Gán hoặc bỏ quyền của vai trò' },

        // Audit Logs
        { code: 'audit.view', name: 'Xem nhật ký hệ thống', module: 'audit', action: 'view', description: 'Xem lịch sử truy cập và thao tác quản trị' },

        // Departments
        { code: 'department.view', name: 'Xem phòng ban', module: 'departments', action: 'view', description: 'Xem danh sách các phòng ban tổ chức' },

        // Extensible permissions for future sprints
        { code: 'requisitions.view', name: 'Xem yêu cầu tuyển dụng', module: 'requisitions', action: 'view', description: 'Xem danh sách yêu cầu tuyển dụng' },
        { code: 'requisitions.create', name: 'Tạo yêu cầu tuyển dụng', module: 'requisitions', action: 'create', description: 'Đề xuất yêu cầu tuyển dụng mới' },
        { code: 'requisitions.update', name: 'Cập nhật yêu cầu tuyển dụng', module: 'requisitions', action: 'update', description: 'Cập nhật yêu cầu tuyển dụng' },
        { code: 'requisitions.approve', name: 'Phê duyệt yêu cầu tuyển dụng', module: 'requisitions', action: 'approve', description: 'Phê duyệt hoặc từ chối yêu cầu tuyển dụng' },

        { code: 'candidates.view', name: 'Xem ứng viên', module: 'candidates', action: 'view', description: 'Xem thông tin và hồ sơ ứng viên' },
        { code: 'interviews.view', name: 'Xem lịch phỏng vấn', module: 'interviews', action: 'view', description: 'Xem lịch phỏng vấn được phân công' },
        { code: 'interviews.evaluate', name: 'Đánh giá phỏng vấn', module: 'interviews', action: 'evaluate', description: 'Chấm điểm và điền phiếu đánh giá phỏng vấn' },
        { code: 'salary.view', name: 'Xem dải lương', module: 'salary', action: 'view', description: 'Xem mức lương dự kiến và dải lương' },
      ];

      const permMap: Record<string, string> = {};
      for (const p of permissionsData) {
        const res = await client.query(
          `INSERT INTO permissions (code, name, module, action, description)
           VALUES ($1, $2, $3, $4, $5)
           ON CONFLICT (code) DO UPDATE SET name = EXCLUDED.name, description = EXCLUDED.description
           RETURNING id, code`,
          [p.code, p.name, p.module, p.action, p.description]
        );
        permMap[res.rows[0].code] = res.rows[0].id;
      }

      // 4. Map Role to Permissions
      console.log('[Seed] Mapping role permissions...');
      const rolePermMappings: Record<string, string[]> = {
        ADMIN: Object.keys(permMap), // Admin has ALL permissions
        HR_MANAGER: [
          'users.view',
          'roles.view',
          'permissions.view',
          'audit.view',
          'department.view',
          'requisitions.view',
          'requisitions.approve',
          'candidates.view',
          'interviews.view',
          'salary.view',
        ],
        RECRUITER: [
          'department.view',
          'requisitions.view',
          'requisitions.create',
          'requisitions.update',
          'candidates.view',
          'interviews.view',
        ],
        HIRING_MANAGER: [
          'department.view',
          'requisitions.view',
          'requisitions.create',
          'candidates.view',
          'interviews.view',
        ],
        INTERVIEWER: [
          'department.view',
          'interviews.view',
          'interviews.evaluate',
          'candidates.view',
        ],
        APPROVER: [
          'department.view',
          'requisitions.view',
          'requisitions.approve',
          'candidates.view',
        ],
        CANDIDATE: [],
      };

      for (const [roleCode, permCodes] of Object.entries(rolePermMappings)) {
        const roleId = roleMap[roleCode];
        if (!roleId) continue;
        for (const pCode of permCodes) {
          const permId = permMap[pCode];
          if (!permId) continue;
          await client.query(
            `INSERT INTO role_permissions (role_id, permission_id)
             VALUES ($1, $2)
             ON CONFLICT (role_id, permission_id) DO NOTHING`,
            [roleId, permId]
          );
        }
      }

      // 5. Seed Users
      console.log('[Seed] Seeding default users...');
      const defaultPasswordHash = await bcrypt.hash('Admin@123456', 10);

      const usersToSeed = [
        {
          employeeCode: 'EMP001',
          fullName: 'Nguyễn Quản Trị',
          email: 'admin@company.local',
          phone: '0901234567',
          jobTitle: 'Trưởng ban Công nghệ & Quản trị',
          deptCode: 'TECH',
          roles: ['ADMIN'],
        },
        {
          employeeCode: 'EMP002',
          fullName: 'Trần Thị Trưởng Phòng Nhân Sự',
          email: 'hr.manager@company.local',
          phone: '0902345678',
          jobTitle: 'Trưởng phòng Nhân sự',
          deptCode: 'HR',
          roles: ['HR_MANAGER'],
        },
        {
          employeeCode: 'EMP003',
          fullName: 'Lê Chuyên Viên Tuyển Dụng',
          email: 'recruiter@company.local',
          phone: '0903456789',
          jobTitle: 'Senior Recruiter',
          deptCode: 'HR',
          roles: ['RECRUITER'],
        },
        {
          employeeCode: 'EMP004',
          fullName: 'Phạm Trưởng Bộ Phận Kỹ Thuật',
          email: 'hiring.manager@company.local',
          phone: '0904567890',
          jobTitle: 'Engineering Manager',
          deptCode: 'TECH',
          roles: ['HIRING_MANAGER'],
        },
        {
          employeeCode: 'EMP005',
          fullName: 'Hoàng Chuyên Gia Phỏng Vấn',
          email: 'interviewer@company.local',
          phone: '0905678901',
          jobTitle: 'Principal Software Engineer',
          deptCode: 'TECH',
          roles: ['INTERVIEWER'],
        },
        {
          employeeCode: 'EMP006',
          fullName: 'Đỗ Giám Đốc Khối Phê Duyệt',
          email: 'approver@company.local',
          phone: '0906789012',
          jobTitle: 'Chief Operating Officer (COO)',
          deptCode: 'HR',
          roles: ['APPROVER'],
        },
        {
          employeeCode: 'EMP007',
          fullName: 'Nguyễn Văn Đa Năng',
          email: 'leader.tech@company.local',
          phone: '0907890123',
          jobTitle: 'Tech Lead & Hiring Head',
          deptCode: 'TECH',
          roles: ['HIRING_MANAGER', 'INTERVIEWER'], // Demo S1-09 multi-role
        },
      ];

      const userMap: Record<string, string> = {};
      for (const u of usersToSeed) {
        const deptId = deptMap[u.deptCode];
        const res = await client.query(
          `INSERT INTO users (employee_code, full_name, email, phone, job_title, department_id, password_hash, status)
           VALUES ($1, $2, $3, $4, $5, $6, $7, 'ACTIVE')
           ON CONFLICT (email) DO UPDATE SET
             full_name = EXCLUDED.full_name,
             phone = EXCLUDED.phone,
             job_title = EXCLUDED.job_title,
             department_id = EXCLUDED.department_id,
             password_hash = EXCLUDED.password_hash,
             status = 'ACTIVE',
             failed_login_attempts = 0,
             locked_until = NULL,
             locked_at = NULL,
             lock_reason = NULL
           RETURNING id, email`,
          [u.employeeCode, u.fullName, u.email, u.phone, u.jobTitle, deptId, defaultPasswordHash]
        );
        const userId = res.rows[0].id;
        userMap[u.email] = userId;

        // Assign user roles
        for (const roleCode of u.roles) {
          const roleId = roleMap[roleCode];
          if (roleId) {
            await client.query(
              `INSERT INTO user_roles (user_id, role_id)
               VALUES ($1, $2)
               ON CONFLICT (user_id, role_id) DO NOTHING`,
              [userId, roleId]
            );
          }
        }
      }

      // 6. Seed Recruitment Requisitions for Handover Warning Demonstration (S1-10)
      console.log('[Seed] Seeding sample open recruitment requisitions...');
      const recruiterId = userMap['recruiter@company.local'];
      const hiringManagerId = userMap['hiring.manager@company.local'];
      const techDeptId = deptMap['TECH'];

      if (recruiterId && hiringManagerId && techDeptId) {
        await client.query(
          `INSERT INTO recruitment_requisitions (code, title, department_id, recruiter_id, hiring_manager_id, status)
           VALUES 
             ('REQ-2026-001', 'Senior Backend Engineer (NodeJS/Java)', $1, $2, $3, 'OPEN'),
             ('REQ-2026-002', 'DevOps / Cloud Architect', $1, $2, $3, 'OPEN')
           ON CONFLICT (code) DO NOTHING`,
          [techDeptId, recruiterId, hiringManagerId]
        );
      }
    });

    console.log('[Seed] Seed completed successfully!');
  } catch (error) {
    console.error('[Seed Error]', error);
    throw error;
  }
}

if (require.main === module) {
  seedData()
    .catch((err) => {
      console.error(err);
      process.exit(1);
    })
    .finally(async () => {
      await pool.end();
    });
}

export { seedData };

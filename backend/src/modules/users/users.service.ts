import crypto from 'crypto';
import bcrypt from 'bcryptjs';
import { query, withTransaction } from '../../database/db';
import { config } from '../../config/env';
import {
  BadRequestException,
  ConflictException,
  NotFoundException,
} from '../../common/exceptions';
import { AuditService } from '../audit/audit.service';
import { EmailService } from '../email/email.service';
import {
  CreateUserInput,
  UpdateUserInput,
  LockUserInput,
} from './users.dto';

export class UsersService {
  static async getUsers(params: {
    page?: number;
    pageSize?: number;
    search?: string;
    departmentId?: string;
    role?: string;
    status?: string;
    sortBy?: string;
    sortDirection?: string;
  }) {
    const page = Math.max(1, params.page || 1);
    const pageSize = Math.max(1, Math.min(100, params.pageSize || 20)); // default 20
    const offset = (page - 1) * pageSize;

    const conditions: string[] = [];
    const values: any[] = [];
    let paramIndex = 1;

    if (params.search && params.search.trim()) {
      const term = `%${params.search.trim().toLowerCase()}%`;
      conditions.push(
        `(LOWER(u.full_name) LIKE $${paramIndex} OR LOWER(u.email) LIKE $${paramIndex} OR LOWER(COALESCE(u.phone, '')) LIKE $${paramIndex} OR LOWER(COALESCE(u.employee_code, '')) LIKE $${paramIndex})`
      );
      values.push(term);
      paramIndex++;
    }

    if (params.departmentId && params.departmentId.trim()) {
      conditions.push(`u.department_id = $${paramIndex++}`);
      values.push(params.departmentId.trim());
    }

    if (params.status && params.status.trim()) {
      conditions.push(`u.status = $${paramIndex++}`);
      values.push(params.status.trim().toUpperCase());
    }

    if (params.role && params.role.trim()) {
      conditions.push(
        `EXISTS (SELECT 1 FROM user_roles ur INNER JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = u.id AND r.code = $${paramIndex++})`
      );
      values.push(params.role.trim().toUpperCase());
    }

    const whereClause = conditions.length > 0 ? `WHERE ${conditions.join(' AND ')}` : '';

    // Sort column safe mapping
    const allowedSortColumns: Record<string, string> = {
      createdAt: 'u.created_at',
      created_at: 'u.created_at',
      fullName: 'u.full_name',
      full_name: 'u.full_name',
      email: 'u.email',
      status: 'u.status',
    };
    const sortCol = allowedSortColumns[params.sortBy || 'created_at'] || 'u.created_at';
    const sortDir = params.sortDirection?.toLowerCase() === 'asc' ? 'ASC' : 'DESC';

    // Count
    const countRes = await query(
      `SELECT count(*) FROM users u ${whereClause}`,
      values
    );
    const total = parseInt(countRes.rows[0].count, 10);

    // Items
    const itemsRes = await query(
      `SELECT 
         u.id, u.employee_code, u.full_name, u.email, u.phone, u.job_title,
         u.status, u.failed_login_attempts, u.locked_until, u.locked_at, u.lock_reason,
         u.last_login_at, u.created_at, u.updated_at,
         d.id as department_id, d.name as department_name, d.code as department_code,
         COALESCE(
           json_agg(
             json_build_object('id', r.id, 'code', r.code, 'name', r.name)
           ) FILTER (WHERE r.id IS NOT NULL), '[]'
         ) as roles
       FROM users u
       LEFT JOIN departments d ON d.id = u.department_id
       LEFT JOIN user_roles ur ON ur.user_id = u.id
       LEFT JOIN roles r ON r.id = ur.role_id
       ${whereClause}
       GROUP BY u.id, d.id
       ORDER BY ${sortCol} ${sortDir}
       LIMIT $${paramIndex++} OFFSET $${paramIndex++}`,
      [...values, pageSize, offset]
    );

    return {
      items: itemsRes.rows.map((row) => ({
        id: row.id,
        employeeCode: row.employee_code,
        fullName: row.full_name,
        email: row.email,
        phone: row.phone,
        jobTitle: row.job_title,
        status: row.status,
        failedLoginAttempts: row.failed_login_attempts,
        lockedUntil: row.locked_until,
        lockedAt: row.locked_at,
        lockReason: row.lock_reason,
        lastLoginAt: row.last_login_at,
        createdAt: row.created_at,
        updatedAt: row.updated_at,
        department: row.department_id
          ? { id: row.department_id, code: row.department_code, name: row.department_name }
          : null,
        roles: row.roles,
      })),
      page,
      pageSize,
      total,
      totalPages: Math.ceil(total / pageSize),
    };
  }

  static async getUserById(userId: string) {
    const userRes = await query(
      `SELECT 
         u.id, u.employee_code, u.full_name, u.email, u.phone, u.job_title,
         u.status, u.failed_login_attempts, u.locked_until, u.locked_at, u.lock_reason,
         u.last_login_at, u.created_at, u.updated_at,
         d.id as department_id, d.name as department_name, d.code as department_code,
         COALESCE(
           json_agg(
             json_build_object('id', r.id, 'code', r.code, 'name', r.name)
           ) FILTER (WHERE r.id IS NOT NULL), '[]'
         ) as roles
       FROM users u
       LEFT JOIN departments d ON d.id = u.department_id
       LEFT JOIN user_roles ur ON ur.user_id = u.id
       LEFT JOIN roles r ON r.id = ur.role_id
       WHERE u.id = $1
       GROUP BY u.id, d.id`,
      [userId]
    );

    const row = userRes.rows[0];
    if (!row) {
      throw new NotFoundException('Không tìm thấy tài khoản người dùng.');
    }

    // Check open requisition count
    const reqRes = await query(
      `SELECT count(*) FROM recruitment_requisitions 
       WHERE status = 'OPEN' AND (recruiter_id = $1 OR hiring_manager_id = $1)`,
      [userId]
    );
    const activeRequisitionCount = parseInt(reqRes.rows[0].count, 10);

    return {
      id: row.id,
      employeeCode: row.employee_code,
      fullName: row.full_name,
      email: row.email,
      phone: row.phone,
      jobTitle: row.job_title,
      status: row.status,
      failedLoginAttempts: row.failed_login_attempts,
      lockedUntil: row.locked_until,
      lockedAt: row.locked_at,
      lockReason: row.lock_reason,
      lastLoginAt: row.last_login_at,
      createdAt: row.created_at,
      updatedAt: row.updated_at,
      activeRequisitionCount,
      department: row.department_id
        ? { id: row.department_id, code: row.department_code, name: row.department_name }
        : null,
      roles: row.roles,
    };
  }

  static async createUser(
    input: CreateUserInput,
    actorUserId?: string,
    ipAddress?: string,
    userAgent?: string
  ) {
    // 1. Check duplicate email
    const existing = await query('SELECT 1 FROM users WHERE LOWER(email) = LOWER($1)', [input.email.trim()]);
    if (existing.rowCount && existing.rowCount > 0) {
      throw new ConflictException('Email đã tồn tại trong hệ thống.');
    }

    // 2. Generate random temporary password (e.g. Temp@123Abc)
    const randomHex = crypto.randomBytes(4).toString('hex');
    const tempPassword = `Temp@${randomHex.charAt(0).toUpperCase()}${randomHex.slice(1)}1`;
    const passwordHash = await bcrypt.hash(tempPassword, 10);

    // Auto-generate employee code if missing
    const employeeCode =
      input.employeeCode?.trim() ||
      `EMP${Math.floor(100000 + Math.random() * 900000)}`;

    const newUser = await withTransaction(async (client) => {
      // Insert user
      const userRes = await client.query(
        `INSERT INTO users (
           employee_code, full_name, email, phone, job_title, department_id,
           password_hash, status, must_change_password, created_by
         )
         VALUES ($1, $2, $3, $4, $5, $6, $7, 'ACTIVE', true, $8)
         RETURNING id, employee_code, full_name, email, phone, job_title, status, created_at`,
        [
          employeeCode,
          input.fullName.trim(),
          input.email.trim().toLowerCase(),
          input.phone?.trim() || null,
          input.jobTitle?.trim() || null,
          input.departmentId || null,
          passwordHash,
          actorUserId || null,
        ]
      );

      const createdUser = userRes.rows[0];

      // Assign initial roles
      for (const roleId of input.roleIds) {
        await client.query(
          `INSERT INTO user_roles (user_id, role_id)
           VALUES ($1, $2)
           ON CONFLICT DO NOTHING`,
          [createdUser.id, roleId]
        );
      }

      return createdUser;
    });

    // 3. Queue activation email
    await EmailService.queueEmail({
      recipient: newUser.email,
      subject: '[IRMS] Thông báo tài khoản tuyển dụng nội bộ mới',
      template: 'ACCOUNT_ACTIVATION',
      payload: {
        fullName: newUser.full_name,
        email: newUser.email,
        temporaryPassword: tempPassword,
        loginUrl: `${config.mail.appUrl}/login`,
      },
    });

    // 4. Audit Log
    await AuditService.log({
      userId: actorUserId,
      action: 'USER_CREATED',
      entityType: 'USER',
      entityId: newUser.id,
      description: `Quản trị viên tạo tài khoản mới cho ${newUser.full_name} (${newUser.email})`,
      ipAddress,
      userAgent,
    });

    return {
      user: newUser,
      temporaryPassword: tempPassword, // returned for Admin display convenience in development
    };
  }

  static async updateUser(
    userId: string,
    input: UpdateUserInput,
    actorUserId?: string,
    ipAddress?: string,
    userAgent?: string
  ) {
    const existing = await query('SELECT * FROM users WHERE id = $1', [userId]);
    if (existing.rowCount === 0) {
      throw new NotFoundException('Không tìm thấy tài khoản người dùng.');
    }

    const updated = await query(
      `UPDATE users
       SET full_name = $1,
           phone = $2,
           job_title = $3,
           department_id = $4,
           employee_code = COALESCE($5, employee_code),
           status = COALESCE($6, status),
           updated_at = CURRENT_TIMESTAMP,
           updated_by = $7
       WHERE id = $8
       RETURNING id, employee_code, full_name, email, phone, job_title, status, updated_at`,
      [
        input.fullName.trim(),
        input.phone?.trim() || null,
        input.jobTitle?.trim() || null,
        input.departmentId || null,
        input.employeeCode?.trim() || null,
        input.status || null,
        actorUserId || null,
        userId,
      ]
    );

    await AuditService.log({
      userId: actorUserId,
      action: 'USER_UPDATED',
      entityType: 'USER',
      entityId: userId,
      description: `Cập nhật thông tin tài khoản người dùng: ${input.fullName}`,
      ipAddress,
      userAgent,
    });

    const row = updated.rows[0];
    return {
      id: row.id,
      employeeCode: row.employee_code,
      fullName: row.full_name,
      email: row.email,
      phone: row.phone,
      jobTitle: row.job_title,
      status: row.status,
      updatedAt: row.updated_at,
    };
  }

  static async lockUser(
    userId: string,
    input: LockUserInput,
    actorUserId?: string,
    ipAddress?: string,
    userAgent?: string
  ) {
    const userRes = await query('SELECT * FROM users WHERE id = $1', [userId]);
    const targetUser = userRes.rows[0];

    if (!targetUser) {
      throw new NotFoundException('Không tìm thấy tài khoản người dùng.');
    }

    if (targetUser.status === 'LOCKED') {
      throw new BadRequestException('Tài khoản này hiện đang ở trạng thái bị khóa.');
    }

    // Check open recruitment requisitions (S1-10 Handover Check)
    const reqRes = await query(
      `SELECT count(*) FROM recruitment_requisitions 
       WHERE status = 'OPEN' AND (recruiter_id = $1 OR hiring_manager_id = $1)`,
      [userId]
    );
    const activeRequisitionCount = parseInt(reqRes.rows[0].count, 10);

    // If active requisitions exist and force is not confirmed: return warning
    if (activeRequisitionCount > 0 && !input.force) {
      return {
        requiresHandoverWarning: true,
        activeRequisitionCount,
        message: `Người dùng đang phụ trách ${activeRequisitionCount} vị trí tuyển dụng. Vui lòng kiểm tra và bàn giao trước khi khóa tài khoản.`,
      };
    }

    // Perform Lock
    await withTransaction(async (client) => {
      await client.query(
        `UPDATE users
         SET status = 'LOCKED',
             lock_reason = $1,
             locked_at = CURRENT_TIMESTAMP,
             locked_by = $2,
             updated_at = CURRENT_TIMESTAMP
         WHERE id = $3`,
        [input.reason.trim(), actorUserId || null, userId]
      );

      // S1-10: Revoke all active sessions immediately
      await client.query(
        'UPDATE refresh_tokens SET revoked_at = CURRENT_TIMESTAMP WHERE user_id = $1 AND revoked_at IS NULL',
        [userId]
      );
    });

    await AuditService.log({
      userId: actorUserId,
      action: 'USER_LOCKED',
      entityType: 'USER',
      entityId: userId,
      description: `Khóa tài khoản ${targetUser.email}. Lý do: ${input.reason}. (Bàn giao: ${activeRequisitionCount} vị trí)`,
      ipAddress,
      userAgent,
    });

    return {
      requiresHandoverWarning: false,
      activeRequisitionCount,
      message: 'Đã khóa tài khoản thành công và thu hồi tất cả phiên đăng nhập.',
    };
  }

  static async unlockUser(
    userId: string,
    actorUserId?: string,
    ipAddress?: string,
    userAgent?: string
  ) {
    const userRes = await query('SELECT * FROM users WHERE id = $1', [userId]);
    const targetUser = userRes.rows[0];

    if (!targetUser) {
      throw new NotFoundException('Không tìm thấy tài khoản người dùng.');
    }

    await query(
      `UPDATE users
       SET status = 'ACTIVE',
           lock_reason = NULL,
           locked_at = NULL,
           locked_by = NULL,
           failed_login_attempts = 0,
           locked_until = NULL,
           updated_at = CURRENT_TIMESTAMP
       WHERE id = $1`,
      [userId]
    );

    await AuditService.log({
      userId: actorUserId,
      action: 'USER_UNLOCKED',
      entityType: 'USER',
      entityId: userId,
      description: `Mở khóa tài khoản ${targetUser.email}`,
      ipAddress,
      userAgent,
    });

    return { message: 'Đã mở khóa tài khoản thành công.' };
  }

  static async getUserRoles(userId: string) {
    const res = await query(
      `SELECT r.id, r.code, r.name, r.description, ur.created_at as assigned_at
       FROM roles r
       INNER JOIN user_roles ur ON ur.role_id = r.id
       WHERE ur.user_id = $1
       ORDER BY r.code`,
      [userId]
    );
    return res.rows;
  }

  static async assignRole(
    userId: string,
    roleId: string,
    actorUserId?: string,
    ipAddress?: string,
    userAgent?: string
  ) {
    const userRes = await query('SELECT * FROM users WHERE id = $1', [userId]);
    if (userRes.rowCount === 0) {
      throw new NotFoundException('Không tìm thấy tài khoản người dùng.');
    }

    const roleRes = await query('SELECT * FROM roles WHERE id = $1', [roleId]);
    if (roleRes.rowCount === 0) {
      throw new NotFoundException('Không tìm thấy vai trò.');
    }

    await query(
      `INSERT INTO user_roles (user_id, role_id)
       VALUES ($1, $2)
       ON CONFLICT (user_id, role_id) DO NOTHING`,
      [userId, roleId]
    );

    await AuditService.log({
      userId: actorUserId,
      action: 'ROLE_ASSIGNED',
      entityType: 'ROLE',
      entityId: roleId,
      description: `Gán vai trò [${roleRes.rows[0].name}] cho người dùng [${userRes.rows[0].email}]`,
      ipAddress,
      userAgent,
    });

    return { message: 'Gán vai trò thành công.' };
  }

  static async revokeRole(
    userId: string,
    roleId: string,
    actorUserId?: string,
    ipAddress?: string,
    userAgent?: string
  ) {
    const userRes = await query('SELECT * FROM users WHERE id = $1', [userId]);
    if (userRes.rowCount === 0) {
      throw new NotFoundException('Không tìm thấy tài khoản người dùng.');
    }

    const roleRes = await query('SELECT * FROM roles WHERE id = $1', [roleId]);
    if (roleRes.rowCount === 0) {
      throw new NotFoundException('Không tìm thấy vai trò.');
    }

    const role = roleRes.rows[0];

    // S1-09 Rule: Target user cannot be current user IF role is ADMIN
    if (userId === actorUserId && role.code === 'ADMIN') {
      throw new BadRequestException('Bạn không thể tự thu hồi quyền quản trị của chính mình.');
    }

    await query(
      'DELETE FROM user_roles WHERE user_id = $1 AND role_id = $2',
      [userId, roleId]
    );

    await AuditService.log({
      userId: actorUserId,
      action: 'ROLE_REVOKED',
      entityType: 'ROLE',
      entityId: roleId,
      description: `Thu hồi vai trò [${role.name}] khỏi người dùng [${userRes.rows[0].email}]`,
      ipAddress,
      userAgent,
    });

    return { message: 'Thu hồi vai trò thành công.' };
  }
}

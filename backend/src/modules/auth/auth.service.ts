import crypto from 'crypto';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { query, withTransaction } from '../../database/db';
import { config } from '../../config/env';
import {
  BadRequestException,
  ForbiddenException,
  NotFoundException,
  UnauthorizedException,
} from '../../common/exceptions';
import { AuditService } from '../audit/audit.service';
import { EmailService } from '../email/email.service';
import {
  LoginInput,
  ForgotPasswordInput,
  ResetPasswordInput,
  ChangePasswordInput,
} from './auth.dto';

export interface UserSessionPayload {
  userId: string;
  email: string;
  roles: string[];
  permissions: string[];
}

export class AuthService {
  private static hashToken(token: string): string {
    return crypto.createHash('sha256').update(token).digest('hex');
  }

  static async getUserRolesAndPermissions(userId: string): Promise<{ roles: string[]; permissions: string[] }> {
    // 1. Roles
    const rolesRes = await query(
      `SELECT r.code FROM roles r
       INNER JOIN user_roles ur ON ur.role_id = r.id
       WHERE ur.user_id = $1`,
      [userId]
    );
    const roles = rolesRes.rows.map((r) => r.code);

    // 2. Distinct Permissions from all assigned roles
    const permsRes = await query(
      `SELECT DISTINCT p.code FROM permissions p
       INNER JOIN role_permissions rp ON rp.permission_id = p.id
       INNER JOIN user_roles ur ON ur.role_id = rp.role_id
       WHERE ur.user_id = $1`,
      [userId]
    );
    const permissions = permsRes.rows.map((p) => p.code);

    return { roles, permissions };
  }

  static generateAccessToken(payload: UserSessionPayload): string {
    return jwt.sign(payload, config.jwt.secret, {
      expiresIn: config.jwt.expiresInSeconds,
    });
  }

  static async createRefreshToken(
    userId: string,
    ipAddress?: string,
    userAgent?: string
  ): Promise<{ rawToken: string; expiresAt: Date; tokenId: string }> {
    const rawToken = crypto.randomBytes(40).toString('hex');
    const tokenHash = this.hashToken(rawToken);
    const expiresAt = new Date(Date.now() + config.jwt.refreshExpirationDays * 24 * 60 * 60 * 1000);

    const res = await query(
      `INSERT INTO refresh_tokens (user_id, token_hash, expires_at, ip_address, user_agent)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING id`,
      [userId, tokenHash, expiresAt, ipAddress || null, userAgent ? userAgent.substring(0, 500) : null]
    );

    return { rawToken, expiresAt, tokenId: res.rows[0].id };
  }

  static async login(
    input: LoginInput,
    ipAddress?: string,
    userAgent?: string
  ) {
    const userRes = await query(
      `SELECT u.*, d.name as department_name, d.code as department_code
       FROM users u
       LEFT JOIN departments d ON d.id = u.department_id
       WHERE LOWER(u.email) = LOWER($1)`,
      [input.email.trim()]
    );

    const user = userRes.rows[0];

    // Generic error constant - strictly required: do not reveal email existence
    const genericLoginError = 'Email hoặc mật khẩu không chính xác.';

    if (!user) {
      await AuditService.log({
        action: 'LOGIN_FAILED',
        entityType: 'AUTH',
        description: `Đăng nhập thất bại cho email: ${input.email} (Email không tồn tại)`,
        ipAddress,
        userAgent,
      });
      throw new BadRequestException(genericLoginError);
    }

    // Check if account is permanently/manually locked by Admin
    if (user.status === 'LOCKED') {
      await AuditService.log({
        userId: user.id,
        action: 'LOGIN_BLOCKED',
        entityType: 'AUTH',
        entityId: user.id,
        description: `Chặn đăng nhập: Tài khoản đã bị quản trị viên khóa. Lý do: ${user.lock_reason || 'Không có'}`,
        ipAddress,
        userAgent,
      });
      throw new ForbiddenException('Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.');
    }

    // Check if account is inactive
    if (user.status === 'INACTIVE') {
      throw new ForbiddenException('Tài khoản chưa được kích hoạt hoặc đã ngừng hoạt động.');
    }

    // Check temporary lockout from 5 failed attempts
    const now = new Date();
    if (user.locked_until && new Date(user.locked_until) > now) {
      await AuditService.log({
        userId: user.id,
        action: 'LOGIN_BLOCKED_TEMP',
        entityType: 'AUTH',
        entityId: user.id,
        description: `Chặn đăng nhập do tài khoản đang bị tạm khóa 15 phút đến: ${user.locked_until}`,
        ipAddress,
        userAgent,
      });
      throw new ForbiddenException('Tài khoản đang tạm khóa. Vui lòng thử lại sau.');
    }

    // Verify Password
    const isPasswordValid = await bcrypt.compare(input.password, user.password_hash);

    if (!isPasswordValid) {
      const newAttempts = (user.failed_login_attempts || 0) + 1;

      if (newAttempts >= config.security.maxFailedAttempts) {
        const lockedUntil = new Date(Date.now() + config.security.lockoutMinutes * 60 * 1000);
        await query(
          `UPDATE users 
           SET failed_login_attempts = $1, locked_until = $2, updated_at = CURRENT_TIMESTAMP 
           WHERE id = $3`,
          [newAttempts, lockedUntil, user.id]
        );

        await AuditService.log({
          userId: user.id,
          action: 'ACCOUNT_TEMP_LOCKED',
          entityType: 'USER',
          entityId: user.id,
          description: `Tài khoản bị tạm khóa 15 phút do nhập sai mật khẩu 5 lần liên tiếp`,
          ipAddress,
          userAgent,
        });

        throw new ForbiddenException('Tài khoản đang tạm khóa. Vui lòng thử lại sau.');
      } else {
        await query(
          `UPDATE users 
           SET failed_login_attempts = $1, updated_at = CURRENT_TIMESTAMP 
           WHERE id = $2`,
          [newAttempts, user.id]
        );

        await AuditService.log({
          userId: user.id,
          action: 'LOGIN_FAILED',
          entityType: 'AUTH',
          entityId: user.id,
          description: `Đăng nhập sai mật khẩu lần thứ ${newAttempts}`,
          ipAddress,
          userAgent,
        });

        throw new BadRequestException(genericLoginError);
      }
    }

    // Password is valid -> reset failed attempts and temp lock
    await query(
      `UPDATE users 
       SET failed_login_attempts = 0, locked_until = NULL, last_login_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP 
       WHERE id = $1`,
      [user.id]
    );

    // Fetch roles & permissions
    const { roles, permissions } = await this.getUserRolesAndPermissions(user.id);

    // Generate tokens
    const accessToken = this.generateAccessToken({
      userId: user.id,
      email: user.email,
      roles,
      permissions,
    });

    const { rawToken: refreshToken, expiresAt: refreshExpiresAt, tokenId: refreshTokenId } =
      await this.createRefreshToken(user.id, ipAddress, userAgent);

    await AuditService.log({
      userId: user.id,
      action: 'LOGIN_SUCCESS',
      entityType: 'AUTH',
      entityId: user.id,
      description: `Đăng nhập thành công với vai trò: ${roles.join(', ')}`,
      ipAddress,
      userAgent,
    });

    return {
      accessToken,
      refreshToken,
      refreshExpiresAt,
      refreshTokenId,
      user: {
        id: user.id,
        employeeCode: user.employee_code,
        fullName: user.full_name,
        email: user.email,
        phone: user.phone,
        jobTitle: user.job_title,
        status: user.status,
        mustChangePassword: user.must_change_password,
        department: user.department_id
          ? { id: user.department_id, code: user.department_code, name: user.department_name }
          : null,
        roles,
        permissions,
      },
    };
  }

  static async refreshToken(
    rawRefreshToken: string,
    ipAddress?: string,
    userAgent?: string
  ) {
    if (!rawRefreshToken) {
      throw new UnauthorizedException('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
    }

    const tokenHash = this.hashToken(rawRefreshToken);

    const tokenRes = await query(
      `SELECT rt.*, u.status as user_status, u.email as user_email
       FROM refresh_tokens rt
       INNER JOIN users u ON u.id = rt.user_id
       WHERE rt.token_hash = $1`,
      [tokenHash]
    );

    const tokenRecord = tokenRes.rows[0];

    if (!tokenRecord) {
      throw new UnauthorizedException('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
    }

    // Check if token was already revoked or expired
    if (tokenRecord.revoked_at || new Date(tokenRecord.expires_at) <= new Date()) {
      throw new UnauthorizedException('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
    }

    // Check if user is locked or inactive
    if (tokenRecord.user_status !== 'ACTIVE') {
      await query('UPDATE refresh_tokens SET revoked_at = CURRENT_TIMESTAMP WHERE id = $1', [tokenRecord.id]);
      throw new ForbiddenException('Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.');
    }

    // Fetch user details, roles and permissions
    const { roles, permissions } = await this.getUserRolesAndPermissions(tokenRecord.user_id);

    // Rotate refresh token
    const newRawToken = crypto.randomBytes(40).toString('hex');
    const newTokenHash = this.hashToken(newRawToken);
    const newExpiresAt = new Date(Date.now() + config.jwt.refreshExpirationDays * 24 * 60 * 60 * 1000);

    const newRtRes = await query(
      `INSERT INTO refresh_tokens (user_id, token_hash, expires_at, ip_address, user_agent)
       VALUES ($1, $2, $3, $4, $5)
       RETURNING id`,
      [tokenRecord.user_id, newTokenHash, newExpiresAt, ipAddress || null, userAgent ? userAgent.substring(0, 500) : null]
    );

    const newTokenId = newRtRes.rows[0].id;

    // Revoke old token and link to new one
    await query(
      `UPDATE refresh_tokens 
       SET revoked_at = CURRENT_TIMESTAMP, replaced_by_token_id = $1 
       WHERE id = $2`,
      [newTokenId, tokenRecord.id]
    );

    // Issue new access token
    const accessToken = this.generateAccessToken({
      userId: tokenRecord.user_id,
      email: tokenRecord.user_email,
      roles,
      permissions,
    });

    return {
      accessToken,
      refreshToken: newRawToken,
      refreshExpiresAt: newExpiresAt,
      userId: tokenRecord.user_id,
      roles,
      permissions,
    };
  }

  static async logout(rawRefreshToken?: string, userId?: string, ipAddress?: string, userAgent?: string) {
    if (rawRefreshToken) {
      const tokenHash = this.hashToken(rawRefreshToken);
      await query(
        `UPDATE refresh_tokens 
         SET revoked_at = CURRENT_TIMESTAMP 
         WHERE token_hash = $1 AND revoked_at IS NULL`,
        [tokenHash]
      );
    }

    if (userId) {
      await AuditService.log({
        userId,
        action: 'LOGOUT',
        entityType: 'AUTH',
        entityId: userId,
        description: 'Đăng xuất khỏi hệ thống',
        ipAddress,
        userAgent,
      });
    }

    return { success: true, message: 'Đăng xuất thành công.' };
  }

  static async forgotPassword(input: ForgotPasswordInput, ipAddress?: string, userAgent?: string) {
    const genericResponse: { message: string; devResetUrl?: string } = {
      message: 'Nếu email tồn tại trong hệ thống, chúng tôi sẽ gửi hướng dẫn đặt lại mật khẩu.',
    };

    const userRes = await query('SELECT * FROM users WHERE LOWER(email) = LOWER($1)', [input.email.trim()]);
    const user = userRes.rows[0];

    // Generic response regardless of whether email exists or not
    if (!user || user.status !== 'ACTIVE') {
      return genericResponse;
    }

    // Generate random 32-byte token
    const rawResetToken = crypto.randomBytes(32).toString('hex');
    const tokenHash = this.hashToken(rawResetToken);
    const expiresAt = new Date(Date.now() + config.security.resetTokenExpirationMinutes * 60 * 1000);

    // Invalidate existing unused reset tokens for this user
    await query(
      `UPDATE password_reset_tokens 
       SET used_at = CURRENT_TIMESTAMP 
       WHERE user_id = $1 AND used_at IS NULL`,
      [user.id]
    );

    // Store hashed token
    await query(
      `INSERT INTO password_reset_tokens (user_id, token_hash, expires_at)
       VALUES ($1, $2, $3)`,
      [user.id, tokenHash, expiresAt]
    );

    // Queue email
    const resetUrl = `${config.mail.appUrl}/reset-password?token=${rawResetToken}`;
    await EmailService.queueEmail({
      recipient: user.email,
      subject: '[IRMS] Yêu cầu đặt lại mật khẩu',
      template: 'PASSWORD_RESET',
      payload: {
        fullName: user.full_name,
        resetUrl,
      },
    });

    await AuditService.log({
      userId: user.id,
      action: 'PASSWORD_RESET_REQUESTED',
      entityType: 'AUTH',
      entityId: user.id,
      description: `Yêu cầu đặt lại mật khẩu gửi tới ${user.email}`,
      ipAddress,
      userAgent,
    });

    return {
      message: genericResponse.message,
      devResetUrl: config.env === 'development' ? resetUrl : undefined,
    };
  }

  static async resetPassword(input: ResetPasswordInput, ipAddress?: string, userAgent?: string) {
    const tokenHash = this.hashToken(input.token.trim());

    const tokenRes = await query(
      `SELECT prt.*, u.status as user_status, u.email as user_email
       FROM password_reset_tokens prt
       INNER JOIN users u ON u.id = prt.user_id
       WHERE prt.token_hash = $1`,
      [tokenHash]
    );

    const tokenRecord = tokenRes.rows[0];

    if (!tokenRecord) {
      throw new BadRequestException('Liên kết đặt lại mật khẩu không hợp lệ.');
    }

    if (tokenRecord.used_at) {
      throw new BadRequestException('Liên kết đặt lại mật khẩu đã được sử dụng.');
    }

    if (new Date(tokenRecord.expires_at) < new Date()) {
      throw new BadRequestException('Liên kết đặt lại mật khẩu đã hết hạn (quá 30 phút).');
    }

    if (tokenRecord.user_status !== 'ACTIVE') {
      throw new ForbiddenException('Tài khoản đã bị khóa hoặc ngừng hoạt động.');
    }

    // Hash new password with BCrypt
    const newPasswordHash = await bcrypt.hash(input.newPassword, 10);

    await withTransaction(async (client) => {
      // 1. Mark reset token as used
      await client.query(
        'UPDATE password_reset_tokens SET used_at = CURRENT_TIMESTAMP WHERE id = $1',
        [tokenRecord.id]
      );

      // 2. Update user's password and clear any failed login lock
      await client.query(
        `UPDATE users 
         SET password_hash = $1, 
             failed_login_attempts = 0, 
             locked_until = NULL, 
             must_change_password = false,
             updated_at = CURRENT_TIMESTAMP 
         WHERE id = $2`,
        [newPasswordHash, tokenRecord.user_id]
      );

      // 3. Revoke all active refresh tokens to force re-login on all devices
      await client.query(
        'UPDATE refresh_tokens SET revoked_at = CURRENT_TIMESTAMP WHERE user_id = $1 AND revoked_at IS NULL',
        [tokenRecord.user_id]
      );
    });

    await AuditService.log({
      userId: tokenRecord.user_id,
      action: 'PASSWORD_RESET_COMPLETED',
      entityType: 'AUTH',
      entityId: tokenRecord.user_id,
      description: 'Đặt lại mật khẩu thành công qua email',
      ipAddress,
      userAgent,
    });

    return { message: 'Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới.' };
  }

  static async changePassword(
    userId: string,
    input: ChangePasswordInput,
    currentRawRefreshToken?: string,
    ipAddress?: string,
    userAgent?: string
  ) {
    const userRes = await query('SELECT * FROM users WHERE id = $1', [userId]);
    const user = userRes.rows[0];

    if (!user) {
      throw new NotFoundException('Không tìm thấy tài khoản người dùng.');
    }

    // Verify current password
    const isCurrentValid = await bcrypt.compare(input.currentPassword, user.password_hash);
    if (!isCurrentValid) {
      throw new BadRequestException('Mật khẩu hiện tại không chính xác.');
    }

    // Hash new password
    const newPasswordHash = await bcrypt.hash(input.newPassword, 10);

    await withTransaction(async (client) => {
      // Update password
      await client.query(
        `UPDATE users 
         SET password_hash = $1, 
             must_change_password = false, 
             updated_at = CURRENT_TIMESTAMP 
         WHERE id = $2`,
        [newPasswordHash, userId]
      );

      // S1-04: Revoke other sessions.
      // If current raw refresh token is provided, keep it and revoke others.
      if (currentRawRefreshToken) {
        const currentHash = this.hashToken(currentRawRefreshToken);
        await client.query(
          `UPDATE refresh_tokens 
           SET revoked_at = CURRENT_TIMESTAMP 
           WHERE user_id = $1 AND token_hash != $2 AND revoked_at IS NULL`,
          [userId, currentHash]
        );
      } else {
        await client.query(
          `UPDATE refresh_tokens 
           SET revoked_at = CURRENT_TIMESTAMP 
           WHERE user_id = $1 AND revoked_at IS NULL`,
          [userId]
        );
      }
    });

    await AuditService.log({
      userId,
      action: 'PASSWORD_CHANGED',
      entityType: 'AUTH',
      entityId: userId,
      description: 'Đổi mật khẩu thành công và thu hồi các phiên đăng nhập khác',
      ipAddress,
      userAgent,
    });

    return { message: 'Đổi mật khẩu thành công.' };
  }

  static async getCurrentUser(userId: string) {
    const userRes = await query(
      `SELECT u.*, d.name as department_name, d.code as department_code
       FROM users u
       LEFT JOIN departments d ON d.id = u.department_id
       WHERE u.id = $1`,
      [userId]
    );

    const user = userRes.rows[0];
    if (!user) {
      throw new NotFoundException('Người dùng không tồn tại.');
    }

    if (user.status === 'LOCKED') {
      throw new ForbiddenException('Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.');
    }

    const { roles, permissions } = await this.getUserRolesAndPermissions(user.id);

    return {
      id: user.id,
      employeeCode: user.employee_code,
      fullName: user.full_name,
      email: user.email,
      phone: user.phone,
      jobTitle: user.job_title,
      status: user.status,
      mustChangePassword: user.must_change_password,
      department: user.department_id
        ? { id: user.department_id, code: user.department_code, name: user.department_name }
        : null,
      roles,
      permissions,
    };
  }
}

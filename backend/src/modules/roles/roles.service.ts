import { query, withTransaction } from '../../database/db';
import {
  BadRequestException,
  ConflictException,
  NotFoundException,
} from '../../common/exceptions';
import { AuditService } from '../audit/audit.service';
import {
  CreateRoleInput,
  UpdateRoleInput,
  UpdateRolePermissionsInput,
} from './roles.dto';

export class RolesService {
  static async getRoles() {
    const res = await query(
      `SELECT 
         r.id, r.code, r.name, r.description, r.is_system_role, r.created_at, r.updated_at,
         COUNT(DISTINCT rp.permission_id)::int as permissions_count,
         COUNT(DISTINCT ur.user_id)::int as users_count
       FROM roles r
       LEFT JOIN role_permissions rp ON rp.role_id = r.id
       LEFT JOIN user_roles ur ON ur.role_id = r.id
       GROUP BY r.id
       ORDER BY 
         CASE WHEN r.code = 'ADMIN' THEN 1
              WHEN r.code = 'HR_MANAGER' THEN 2
              WHEN r.code = 'RECRUITER' THEN 3
              WHEN r.code = 'HIRING_MANAGER' THEN 4
              WHEN r.code = 'INTERVIEWER' THEN 5
              WHEN r.code = 'APPROVER' THEN 6
              WHEN r.code = 'CANDIDATE' THEN 7
              ELSE 8 END,
         r.name ASC`
    );
    return res.rows;
  }

  static async getRoleById(roleId: string) {
    const roleRes = await query(
      `SELECT r.id, r.code, r.name, r.description, r.is_system_role, r.created_at, r.updated_at
       FROM roles r
       WHERE r.id = $1`,
      [roleId]
    );

    const role = roleRes.rows[0];
    if (!role) {
      throw new NotFoundException('Không tìm thấy vai trò.');
    }

    // Fetch assigned permissions
    const permsRes = await query(
      `SELECT p.id, p.code, p.name, p.module, p.action, p.description
       FROM permissions p
       INNER JOIN role_permissions rp ON rp.permission_id = p.id
       WHERE rp.role_id = $1
       ORDER BY p.module, p.code`,
      [roleId]
    );

    return {
      ...role,
      permissions: permsRes.rows,
    };
  }

  static async createRole(
    input: CreateRoleInput,
    actorUserId?: string,
    ipAddress?: string,
    userAgent?: string
  ) {
    const existing = await query('SELECT 1 FROM roles WHERE code = $1', [input.code]);
    if (existing.rowCount && existing.rowCount > 0) {
      throw new ConflictException(`Mã vai trò [${input.code}] đã tồn tại.`);
    }

    const newRole = await withTransaction(async (client) => {
      const res = await client.query(
        `INSERT INTO roles (code, name, description, is_system_role)
         VALUES ($1, $2, $3, false)
         RETURNING id, code, name, description, is_system_role, created_at`,
        [input.code, input.name.trim(), input.description?.trim() || null]
      );
      const role = res.rows[0];

      if (input.permissionIds && input.permissionIds.length > 0) {
        for (const pId of input.permissionIds) {
          await client.query(
            `INSERT INTO role_permissions (role_id, permission_id)
             VALUES ($1, $2)
             ON CONFLICT DO NOTHING`,
            [role.id, pId]
          );
        }
      }

      return role;
    });

    await AuditService.log({
      userId: actorUserId,
      action: 'ROLE_CREATED',
      entityType: 'ROLE',
      entityId: newRole.id,
      description: `Tạo mới vai trò: ${newRole.name} (${newRole.code})`,
      ipAddress,
      userAgent,
    });

    return newRole;
  }

  static async updateRole(
    roleId: string,
    input: UpdateRoleInput,
    actorUserId?: string,
    ipAddress?: string,
    userAgent?: string
  ) {
    const roleRes = await query('SELECT * FROM roles WHERE id = $1', [roleId]);
    const role = roleRes.rows[0];
    if (!role) {
      throw new NotFoundException('Không tìm thấy vai trò.');
    }

    const updated = await query(
      `UPDATE roles
       SET name = $1,
           description = $2,
           updated_at = CURRENT_TIMESTAMP
       WHERE id = $3
       RETURNING id, code, name, description, is_system_role, updated_at`,
      [input.name.trim(), input.description?.trim() || null, roleId]
    );

    await AuditService.log({
      userId: actorUserId,
      action: 'ROLE_UPDATED',
      entityType: 'ROLE',
      entityId: roleId,
      description: `Cập nhật thông tin vai trò: ${input.name}`,
      ipAddress,
      userAgent,
    });

    return updated.rows[0];
  }

  static async getRolePermissions(roleId: string) {
    const roleRes = await query('SELECT id, code, name FROM roles WHERE id = $1', [roleId]);
    if (roleRes.rowCount === 0) {
      throw new NotFoundException('Không tìm thấy vai trò.');
    }

    const permsRes = await query(
      `SELECT p.id, p.code, p.name, p.module, p.action, p.description
       FROM permissions p
       INNER JOIN role_permissions rp ON rp.permission_id = p.id
       WHERE rp.role_id = $1
       ORDER BY p.module, p.code`,
      [roleId]
    );

    return {
      role: roleRes.rows[0],
      permissions: permsRes.rows,
      permissionIds: permsRes.rows.map((p) => p.id),
    };
  }

  static async updateRolePermissions(
    roleId: string,
    input: UpdateRolePermissionsInput,
    actorUserId?: string,
    ipAddress?: string,
    userAgent?: string
  ) {
    const roleRes = await query('SELECT * FROM roles WHERE id = $1', [roleId]);
    const role = roleRes.rows[0];
    if (!role) {
      throw new NotFoundException('Không tìm thấy vai trò.');
    }

    await withTransaction(async (client) => {
      // Clear existing permissions for this role
      await client.query('DELETE FROM role_permissions WHERE role_id = $1', [roleId]);

      // Insert new permissions
      if (input.permissionIds && input.permissionIds.length > 0) {
        for (const pId of input.permissionIds) {
          await client.query(
            `INSERT INTO role_permissions (role_id, permission_id)
             VALUES ($1, $2)
             ON CONFLICT DO NOTHING`,
            [roleId, pId]
          );
        }
      }
    });

    await AuditService.log({
      userId: actorUserId,
      action: 'ROLE_PERMISSION_UPDATED',
      entityType: 'ROLE',
      entityId: roleId,
      description: `Cập nhật ma trận quyền cho vai trò: [${role.name}]. Số quyền mới: ${input.permissionIds.length}`,
      ipAddress,
      userAgent,
    });

    return { message: 'Cập nhật phân quyền cho vai trò thành công.' };
  }
}

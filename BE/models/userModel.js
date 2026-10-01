'use strict';
const { query, withTransaction } = require('../database/db');

/**
 * UserModel - PostgreSQL-backed user operations
 */
class UserModel {
  /**
   * Find user by email (case-insensitive)
   */
  static async findByEmail(email) {
    const res = await query(
      `SELECT id, employee_code, full_name, email, password_hash, role, avatar_url,
              is_active, must_change_pw, token_version, account_status, created_at, updated_at
       FROM users
       WHERE LOWER(email) = LOWER($1) AND is_active = TRUE AND account_status = 'active'
       LIMIT 1`,
      [email]
    );
    return res.rows[0] || null;
  }

  /**
   * Find user by employee code (case-insensitive)
   */
  static async findByEmployeeCode(code) {
    const res = await query(
      `SELECT id, employee_code, full_name, email, password_hash, role, avatar_url,
              is_active, must_change_pw, token_version, account_status, created_at, updated_at
       FROM users
       WHERE UPPER(employee_code) = UPPER($1) AND is_active = TRUE AND account_status = 'active'
       LIMIT 1`,
      [code]
    );
    return res.rows[0] || null;
  }

  /**
   * Find user by ID
   */
  static async findById(id) {
    const res = await query(
      `SELECT id, employee_code, full_name, email, role, avatar_url,
              is_active, must_change_pw, token_version, account_status, created_at, updated_at
       FROM users
       WHERE id = $1 AND is_active = TRUE AND account_status = 'active'
       LIMIT 1`,
      [id]
    );
    return res.rows[0] || null;
  }

  /**
   * Update password and optionally revoke all other sessions
   */
  static async updatePassword(userId, newHashedPassword, revokeOthers = true, currentSessionId = null) {
    return withTransaction(async (client) => {
      // Increment token_version to invalidate all existing JWT (if any) and server sessions
      const res = await client.query(
        `UPDATE users
         SET password_hash = $1,
             token_version  = CASE WHEN $2 THEN token_version + 1 ELSE token_version END,
             updated_at     = NOW()
         WHERE id = $3
         RETURNING id, employee_code, full_name, email, role, avatar_url, token_version, must_change_pw`,
        [newHashedPassword, revokeOthers, userId]
      );

      const updatedUser = res.rows[0];

      if (revokeOthers) {
        // Revoke all server-side sessions except the current one
        if (currentSessionId) {
          await client.query(
            `UPDATE user_sessions
             SET is_active = FALSE
             WHERE user_id = $1 AND session_id != $2`,
            [userId, currentSessionId]
          );
        } else {
          await client.query(
            `UPDATE user_sessions SET is_active = FALSE WHERE user_id = $1`,
            [userId]
          );
        }
      }

      return updatedUser;
    });
  }

  /**
   * Get active session count for a user
   */
  static async getActiveSessionCount(userId) {
    const res = await query(
      `SELECT COUNT(*) AS count
       FROM user_sessions
       WHERE user_id = $1
         AND is_active = TRUE
         AND expires_at > NOW()
         AND absolute_expires_at > NOW()`,
      [userId]
    );
    return parseInt(res.rows[0].count, 10);
  }

  /**
   * Sinh mã nhân viên tự động tăng dạng PV001, PV002,...
   * Bóc tách phần số MAX rồi ép kiểu INTEGER, tránh lỗi sắp xếp chuỗi (Phương án 2)
   * @param {string} prefix Tiền tố mã nhân viên (mặc định 'PV')
   * @param {object} client PostgreSQL client nếu đang chạy transaction
   * @returns {Promise<string>} Mã nhân sự mới, ví dụ: 'PV002'
   */
  static async generateNextEmployeeCode(prefix = 'PV', client = null) {
    const sql = `
      SELECT MAX(SUBSTRING(employee_code FROM (length($1) + 1))::INTEGER) AS max_num
      FROM users
      WHERE employee_code ~ ('^' || $1 || '[0-9]+$')
    `;
    const res = client ? await client.query(sql, [prefix]) : await query(sql, [prefix]);
    const maxNum = res.rows[0]?.max_num || 0;
    const nextNum = maxNum + 1;
    return `${prefix}${String(nextNum).padStart(3, '0')}`;
  }

  /**
   * Tạo tài khoản người dùng mới kèm mã nhân viên tự tăng và cơ chế Retry chống Race Condition
   */
  static async create({ fullName, email, passwordHash, role = 'nguoi_phong_van', avatarUrl = null, prefix = 'PV' }) {
    const maxRetries = 3;
    let attempt = 0;

    while (attempt < maxRetries) {
      attempt++;
      try {
        const employeeCode = await UserModel.generateNextEmployeeCode(prefix);

        const res = await query(
          `INSERT INTO users (employee_code, full_name, email, password_hash, role, avatar_url, must_change_pw)
           VALUES ($1, $2, $3, $4, $5, $6, FALSE)
           RETURNING id, employee_code, full_name, email, role, avatar_url, must_change_pw, created_at`,
          [employeeCode, fullName.trim(), email.toLowerCase().trim(), passwordHash, role, avatarUrl]
        );

        return res.rows[0];
      } catch (err) {
        // Mã lỗi PostgreSQL 23505: unique_violation trên employee_code do 2 request cùng lúc
        if (err.code === '23505' && err.constraint === 'users_employee_code_key' && attempt < maxRetries) {
          console.warn(`[UserModel.create] Trùng mã nhân viên trong lúc tranh chấp, đang thử lại lần ${attempt}...`);
          continue;
        }
        throw err;
      }
    }
  }

  static async findAccountByEmail(email) {
    const res = await query(
      `SELECT id FROM users WHERE LOWER(email) = LOWER($1) LIMIT 1`,
      [email.trim()]
    );
    return res.rows[0] || null;
  }

  static async listAccounts({ query: search = '', role = '', status = '', page = 1, pageSize = 20 } = {}) {
    const clauses = [];
    const params = [];
    if (search.trim()) {
      params.push(`%${search.trim()}%`);
      clauses.push(`(full_name ILIKE $${params.length} OR email ILIKE $${params.length} OR department ILIKE $${params.length})`);
    }
    if (role) { params.push(role); clauses.push(`role = $${params.length}`); }
    if (status) { params.push(status); clauses.push(`account_status = $${params.length}`); }
    const where = clauses.length ? `WHERE ${clauses.join(' AND ')}` : '';
    const countResult = await query(`SELECT COUNT(*)::INTEGER AS total FROM users ${where}`, params);
    const total = countResult.rows[0].total;
    const offset = (page - 1) * pageSize;
    const rows = await query(
      `SELECT id, employee_code, full_name AS name, email, department, role,
              account_status AS status, created_at
       FROM users ${where}
       ORDER BY created_at DESC, full_name ASC
       LIMIT $${params.length + 1} OFFSET $${params.length + 2}`,
      [...params, pageSize, offset]
    );
    return { data: rows.rows, total };
  }

  static async createManagedAccount({ fullName, email, department, passwordHash, role }) {
    const prefixByRole = { nguoi_phong_van: 'PV', nhan_su: 'NS', quan_tri: 'QT' };
    const prefix = prefixByRole[role];
    if (!prefix) throw new Error('Vai trò tài khoản không hợp lệ.');
    for (let attempt = 1; attempt <= 3; attempt++) {
      try {
        return await withTransaction(async (client) => {
          const employeeCode = await UserModel.generateNextEmployeeCode(prefix, client);
          const result = await client.query(
            `INSERT INTO users (employee_code, full_name, email, password_hash, role,
                                department, account_status, is_active, must_change_pw)
             VALUES ($1, $2, LOWER($3), $4, $5, $6, 'active', TRUE, TRUE)
             RETURNING id, employee_code, full_name AS name, email, department, role,
                       account_status AS status, created_at`,
            [employeeCode, fullName.trim(), email.trim(), passwordHash, role, department.trim()]
          );
          return result.rows[0];
        });
      } catch (error) {
        if (error.code === '23505' && error.constraint === 'users_employee_code_key' && attempt < 3) continue;
        throw error;
      }
    }
  }

  static async deleteManagedAccount(id) {
    await query('DELETE FROM users WHERE id = $1', [id]);
  }

  static async updateManagedAccount(id, { fullName, email, department, role, status }) {
    return withTransaction(async (client) => {
      const result = await client.query(
        `UPDATE users
         SET full_name = $1, email = LOWER($2), department = $3, role = $4,
             account_status = $5, is_active = ($5 = 'active'),
             token_version = token_version + 1, updated_at = NOW()
         WHERE id = $6
         RETURNING id, employee_code, full_name AS name, email, department, role,
                   account_status AS status, created_at`,
        [fullName.trim(), email.trim(), department.trim(), role, status, id]
      );
      if (!result.rows[0]) return null;
      await client.query('UPDATE user_sessions SET is_active = FALSE WHERE user_id = $1', [id]);
      return result.rows[0];
    });
  }
}

module.exports = UserModel;

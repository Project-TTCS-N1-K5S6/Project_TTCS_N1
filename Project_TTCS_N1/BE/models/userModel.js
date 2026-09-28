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
              is_active, must_change_pw, token_version, created_at, updated_at
       FROM users
       WHERE LOWER(email) = LOWER($1) AND is_active = TRUE
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
              is_active, must_change_pw, token_version, created_at, updated_at
       FROM users
       WHERE UPPER(employee_code) = UPPER($1) AND is_active = TRUE
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
              is_active, must_change_pw, token_version, created_at, updated_at
       FROM users
       WHERE id = $1 AND is_active = TRUE
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
}

module.exports = UserModel;

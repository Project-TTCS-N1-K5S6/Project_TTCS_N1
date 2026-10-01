'use strict';
const { query } = require('../database/db');

class PasswordResetModel {
  /**
   * Tạo một token reset password mới
   */
  static async createToken(userId, token, expiresInMinutes = 30) {
    const expiresAt = new Date();
    expiresAt.setMinutes(expiresAt.getMinutes() + expiresInMinutes);

    const res = await query(
      `INSERT INTO password_reset_tokens (user_id, token, expires_at)
       VALUES ($1, $2, $3)
       RETURNING id, token, expires_at`,
      [userId, token, expiresAt]
    );
    return res.rows[0];
  }

  /**
   * Tìm token hợp lệ chưa được sử dụng và chưa hết hạn
   */
  static async findValidToken(token) {
    const res = await query(
      `SELECT prt.*, u.email, u.id as user_id 
       FROM password_reset_tokens prt
       JOIN users u ON prt.user_id = u.id
       WHERE prt.token = $1 
         AND prt.used = FALSE 
         AND prt.expires_at > NOW()`,
      [token]
    );
    return res.rows[0] || null;
  }

  /**
   * Đánh dấu token đã được sử dụng
   */
  static async markAsUsed(tokenId) {
    await query(
      `UPDATE password_reset_tokens 
       SET used = TRUE 
       WHERE id = $1`,
      [tokenId]
    );
  }

  /**
   * Vô hiệu hóa tất cả các token trước đó của user (nếu có yêu cầu mới)
   */
  static async invalidateAllUserTokens(userId) {
    await query(
      `UPDATE password_reset_tokens
       SET used = TRUE
       WHERE user_id = $1 AND used = FALSE`,
      [userId]
    );
  }
}

module.exports = PasswordResetModel;

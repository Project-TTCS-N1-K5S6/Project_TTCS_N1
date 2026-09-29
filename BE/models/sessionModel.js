'use strict';
const { query, withTransaction } = require('../database/db');

/**
 * SessionModel - Server-side session management with PostgreSQL
 * Handles idle timeout, absolute timeout, revocation, and renewal
 */
class SessionModel {
  /**
   * Create a new server-side session record
   * @param {object} data
   */
  static async create({ sessionId, userId, deviceName, ipAddress, userAgent, idleTimeoutMs, absoluteTimeoutMs }) {
    const now = new Date();
    const idleExpiry = new Date(now.getTime() + idleTimeoutMs);
    const absoluteExpiry = new Date(now.getTime() + absoluteTimeoutMs);

    await query(
      `INSERT INTO user_sessions
         (session_id, user_id, device_name, ip_address, user_agent, is_active, created_at, last_activity, expires_at, absolute_expires_at)
       VALUES ($1, $2, $3, $4, $5, TRUE, $6, $6, $7, $8)
       ON CONFLICT (session_id) DO UPDATE
         SET is_active = TRUE,
             last_activity = $6,
             expires_at = $7,
             absolute_expires_at = $8`,
      [sessionId, userId, deviceName || 'Trình duyệt Web', ipAddress, userAgent, now, idleExpiry, absoluteExpiry]
    );
  }

  /**
   * Check if a session is valid (active, not idle-expired, not absolute-expired)
   * @returns {object|null} session record or null if invalid
   */
  static async findValid(sessionId) {
    const res = await query(
      `SELECT us.*, u.token_version AS user_token_version, u.is_active AS user_is_active
       FROM user_sessions us
       JOIN users u ON u.id = us.user_id
       WHERE us.session_id = $1
         AND us.is_active = TRUE
         AND us.expires_at > NOW()
         AND us.absolute_expires_at > NOW()
         AND u.is_active = TRUE
       LIMIT 1`,
      [sessionId]
    );
    return res.rows[0] || null;
  }

  /**
   * Renew idle timeout for an active session
   * @param {string} sessionId
   * @param {number} idleTimeoutMs
   */
  static async renew(sessionId, idleTimeoutMs) {
    const newExpiry = new Date(Date.now() + idleTimeoutMs);
    await query(
      `UPDATE user_sessions
       SET last_activity = NOW(),
           expires_at = LEAST($2, absolute_expires_at)
       WHERE session_id = $1 AND is_active = TRUE`,
      [sessionId, newExpiry]
    );
  }

  /**
   * Revoke a specific session (logout)
   */
  static async revoke(sessionId) {
    await query(
      `UPDATE user_sessions
       SET is_active = FALSE
       WHERE session_id = $1`,
      [sessionId]
    );
  }

  /**
   * Revoke ALL sessions for a user (logout all devices)
   */
  static async revokeAllForUser(userId) {
    await query(
      `UPDATE user_sessions SET is_active = FALSE WHERE user_id = $1`,
      [userId]
    );
  }

  /**
   * Revoke all sessions for a user EXCEPT the current one
   */
  static async revokeOtherSessions(userId, currentSessionId) {
    await query(
      `UPDATE user_sessions
       SET is_active = FALSE
       WHERE user_id = $1 AND session_id != $2`,
      [userId, currentSessionId]
    );
  }

  /**
   * Get all active sessions for a user (for display / management)
   */
  static async getActiveSessions(userId) {
    const res = await query(
      `SELECT id, session_id, device_name, ip_address,
              created_at, last_activity, expires_at, absolute_expires_at
       FROM user_sessions
       WHERE user_id = $1
         AND is_active = TRUE
         AND expires_at > NOW()
         AND absolute_expires_at > NOW()
       ORDER BY last_activity DESC`,
      [userId]
    );
    return res.rows;
  }

  /**
   * Cleanup expired sessions (should be called periodically)
   */
  static async cleanupExpired() {
    const res = await query(
      `UPDATE user_sessions
       SET is_active = FALSE
       WHERE is_active = TRUE
         AND (expires_at < NOW() OR absolute_expires_at < NOW())
       RETURNING session_id`,
      []
    );
    return res.rowCount;
  }

  /**
   * Revoke a specific session by record ID for a user
   */
  static async revokeById(userId, recordId) {
    const res = await query(
      `UPDATE user_sessions
       SET is_active = FALSE
       WHERE id = $1 AND user_id = $2
       RETURNING session_id`,
      [recordId, userId]
    );
    return res.rows[0] || null;
  }
}

module.exports = SessionModel;

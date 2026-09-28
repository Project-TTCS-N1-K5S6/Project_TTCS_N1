'use strict';
const { query } = require('../database/db');

/**
 * EvaluationDraftModel - Auto-save evaluation form drafts in JSONB
 */
class EvaluationDraftModel {
  /**
   * Save or update a draft (upsert by user_id + draft_key)
   */
  static async upsert(userId, draftKey, draftData) {
    const res = await query(
      `INSERT INTO evaluation_drafts (user_id, draft_key, draft_data, saved_at)
       VALUES ($1, $2, $3::jsonb, NOW())
       ON CONFLICT (user_id, draft_key)
       DO UPDATE SET
         draft_data = $3::jsonb,
         saved_at   = NOW()
       RETURNING *`,
      [userId, draftKey, JSON.stringify(draftData)]
    );
    return res.rows[0];
  }

  /**
   * Get a specific draft by user_id + draft_key
   */
  static async findByKey(userId, draftKey) {
    const res = await query(
      `SELECT * FROM evaluation_drafts
       WHERE user_id = $1 AND draft_key = $2
       LIMIT 1`,
      [userId, draftKey]
    );
    return res.rows[0] || null;
  }

  /**
   * Get all drafts for a user
   */
  static async findAllForUser(userId) {
    const res = await query(
      `SELECT id, draft_key, saved_at, jsonb_array_length(
          CASE WHEN jsonb_typeof(draft_data) = 'array' THEN draft_data ELSE '[]'::jsonb END
        ) AS field_count
       FROM evaluation_drafts
       WHERE user_id = $1
       ORDER BY saved_at DESC`,
      [userId]
    );
    return res.rows;
  }

  /**
   * Delete a specific draft
   */
  static async delete(userId, draftKey) {
    await query(
      `DELETE FROM evaluation_drafts WHERE user_id = $1 AND draft_key = $2`,
      [userId, draftKey]
    );
  }
}

module.exports = EvaluationDraftModel;

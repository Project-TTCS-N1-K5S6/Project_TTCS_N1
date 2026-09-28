'use strict';
const EvaluationDraftModel = require('../models/evaluationDraftModel');

/**
 * EvaluationController - Handles evaluation scoring and draft auto-save
 */
class EvaluationController {

  // POST /api/v1/evaluations/drafts/:draftKey
  static async saveDraft(req, res) {
    try {
      const userId = req.user.id;
      const draftKey = req.params.draftKey;
      const draftData = req.body.data;

      if (!draftKey || typeof draftData === 'undefined') {
        return res.status(400).json({ success: false, message: 'Thiếu draftKey hoặc dữ liệu bản nháp.' });
      }

      // Sanitize draft key to prevent injection
      const safeDraftKey = draftKey.replace(/[^a-zA-Z0-9_\-\.]/g, '').substring(0, 255);
      if (!safeDraftKey) {
        return res.status(400).json({ success: false, message: 'Draft key không hợp lệ.' });
      }

      const draft = await EvaluationDraftModel.upsert(userId, safeDraftKey, draftData);

      return res.status(200).json({
        success: true,
        message: 'Đã lưu bản nháp tự động.',
        savedAt: draft.saved_at,
        draftKey: safeDraftKey,
      });

    } catch (err) {
      console.error('[EvaluationController.saveDraft] Error:', err);
      return res.status(500).json({ success: false, message: 'Lỗi hệ thống khi lưu bản nháp.' });
    }
  }

  // GET /api/v1/evaluations/drafts/:draftKey
  static async getDraft(req, res) {
    try {
      const userId = req.user.id;
      const draftKey = req.params.draftKey;

      const safeDraftKey = draftKey.replace(/[^a-zA-Z0-9_\-\.]/g, '').substring(0, 255);
      const draft = await EvaluationDraftModel.findByKey(userId, safeDraftKey);

      if (!draft) {
        return res.status(404).json({ success: false, message: 'Không tìm thấy bản nháp.' });
      }

      return res.status(200).json({
        success: true,
        draft: {
          draftKey: draft.draft_key,
          data: draft.draft_data,
          savedAt: draft.saved_at,
        }
      });

    } catch (err) {
      console.error('[EvaluationController.getDraft] Error:', err);
      return res.status(500).json({ success: false, message: 'Lỗi hệ thống.' });
    }
  }

  // GET /api/v1/evaluations/drafts
  static async listDrafts(req, res) {
    try {
      const drafts = await EvaluationDraftModel.findAllForUser(req.user.id);
      return res.status(200).json({ success: true, drafts });
    } catch (err) {
      console.error('[EvaluationController.listDrafts] Error:', err);
      return res.status(500).json({ success: false, message: 'Lỗi hệ thống.' });
    }
  }

  // DELETE /api/v1/evaluations/drafts/:draftKey
  static async deleteDraft(req, res) {
    try {
      const userId = req.user.id;
      const draftKey = req.params.draftKey.replace(/[^a-zA-Z0-9_\-\.]/g, '').substring(0, 255);
      await EvaluationDraftModel.delete(userId, draftKey);
      return res.status(200).json({ success: true, message: 'Đã xóa bản nháp.' });
    } catch (err) {
      console.error('[EvaluationController.deleteDraft] Error:', err);
      return res.status(500).json({ success: false, message: 'Lỗi hệ thống.' });
    }
  }

  // POST /api/v1/evaluations/submit
  static async submitEvaluation(req, res) {
    try {
      const evaluatorId = req.user.id;
      const { evaluateeId, period = 'Q3-2026', scores = {}, comments = '', totalScore = 0, draftKey } = req.body;

      // Validate required score fields
      if (typeof scores !== 'object' || Object.keys(scores).length === 0) {
        return res.status(400).json({ success: false, message: 'Vui lòng nhập điểm đánh giá.' });
      }

      const { query, withTransaction } = require('../database/db');

      const result = await withTransaction(async (client) => {
        // Fallback target user if evaluateeId not provided
        let targetUserId = evaluateeId;
        if (!targetUserId) {
          const userRes = await client.query('SELECT id FROM users WHERE id != $1 LIMIT 1', [evaluatorId]);
          targetUserId = userRes.rows[0]?.id || evaluatorId;
        }

        // Insert completed evaluation
        const insertRes = await client.query(
          `INSERT INTO evaluation_forms
             (evaluator_id, evaluatee_id, period, status, scores, comments, total_score, submitted_at)
           VALUES ($1, $2, $3, 'submitted', $4, $5, $6, NOW())
           RETURNING id, evaluator_id, evaluatee_id, period, status, scores, comments, total_score, submitted_at`,
          [evaluatorId, targetUserId, period, JSON.stringify(scores), comments, totalScore]
        );

        // Delete draft if draftKey was supplied
        if (draftKey) {
          const safeKey = draftKey.replace(/[^a-zA-Z0-9_\-\.]/g, '').substring(0, 255);
          await client.query(
            `DELETE FROM evaluation_drafts WHERE user_id = $1 AND draft_key = $2`,
            [evaluatorId, safeKey]
          );
        }

        return insertRes.rows[0];
      });

      return res.status(201).json({
        success: true,
        message: 'Đã nộp phiếu đánh giá thành công!',
        evaluation: result
      });

    } catch (err) {
      console.error('[EvaluationController.submitEvaluation] Error:', err);
      return res.status(500).json({ success: false, message: 'Lỗi hệ thống khi nộp phiếu đánh giá.' });
    }
  }

  // GET /api/v1/evaluations/history
  static async listEvaluations(req, res) {
    try {
      const { query } = require('../database/db');
      const result = await query(
        `SELECT ef.*,
                u.full_name AS evaluatee_name,
                u.employee_code AS evaluatee_code
         FROM evaluation_forms ef
         LEFT JOIN users u ON u.id = ef.evaluatee_id
         WHERE ef.evaluator_id = $1
         ORDER BY ef.created_at DESC`,
        [req.user.id]
      );

      return res.status(200).json({
        success: true,
        evaluations: result.rows
      });
    } catch (err) {
      console.error('[EvaluationController.listEvaluations] Error:', err);
      return res.status(500).json({ success: false, message: 'Lỗi hệ thống.' });
    }
  }
}

module.exports = EvaluationController;

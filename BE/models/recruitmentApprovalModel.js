'use strict';
const { query, withTransaction } = require('../database/db');

class RecruitmentApprovalModel {
  static async listReviewers(excludedUserId) {
    const result = await query(
      `SELECT id, employee_code, full_name, role
       FROM users
       WHERE is_active = TRUE AND id <> $1
       ORDER BY full_name`,
      [excludedUserId]
    );
    return result.rows;
  }

  static async create({ title, details, createdBy, reviewerIds }) {
    return withTransaction(async (client) => {
      const reviewersResult = await client.query(
        `SELECT id
         FROM users
         WHERE is_active = TRUE AND id = ANY($1::uuid[])`,
        [reviewerIds]
      );
      if (reviewersResult.rowCount !== reviewerIds.length
        || reviewerIds.includes(createdBy)) {
        return { error: 'INVALID_REVIEWERS' };
      }

      const requestResult = await client.query(
        `INSERT INTO recruitment_approval_requests (title, details, created_by)
         VALUES ($1, $2, $3)
         RETURNING id, title, details, created_by, status, current_step, created_at, updated_at`,
        [title, details, createdBy]
      );
      const request = requestResult.rows[0];

      for (const [index, reviewerId] of reviewerIds.entries()) {
        await client.query(
          `INSERT INTO recruitment_approval_steps (request_id, step_number, approver_user_id)
           VALUES ($1, $2, $3)`,
          [request.id, index + 1, reviewerId]
        );
      }

      await client.query(
        `INSERT INTO recruitment_approval_history (request_id, step_number, action, actor_id)
         VALUES ($1, 1, 'SUBMITTED', $2)`,
        [request.id, createdBy]
      );
      return request;
    });
  }

  static async listForUser(userId) {
    const result = await query(
      `SELECT r.id, r.title, r.details, r.created_by, r.status, r.current_step,
              r.created_at, r.updated_at, creator.full_name AS creator_name,
              current_step.approver_user_id AS current_approver_id,
              approver.full_name AS current_approver_name,
              (r.created_by = $1) AS is_creator,
              (current_step.approver_user_id = $1) AS can_review,
              (SELECT COUNT(*) FROM recruitment_approval_steps all_steps
               WHERE all_steps.request_id = r.id) AS total_steps,
              (SELECT COUNT(*) FROM recruitment_approval_steps approved_steps
               WHERE approved_steps.request_id = r.id
                 AND approved_steps.status = 'APPROVED') AS approved_steps
       FROM recruitment_approval_requests r
       JOIN users creator ON creator.id = r.created_by
       LEFT JOIN recruitment_approval_steps current_step
         ON current_step.request_id = r.id AND current_step.step_number = r.current_step
       LEFT JOIN users approver ON approver.id = current_step.approver_user_id
       WHERE r.created_by = $1 OR EXISTS (
         SELECT 1 FROM recruitment_approval_steps assigned
         WHERE assigned.request_id = r.id AND assigned.approver_user_id = $1
       )
       ORDER BY r.updated_at DESC, r.created_at DESC`,
      [userId]
    );
    return result.rows;
  }

  static async getForUser(requestId, userId) {
    const requestResult = await query(
      `SELECT r.id, r.title, r.details, r.created_by, r.status, r.current_step,
              r.created_at, r.updated_at, creator.full_name AS creator_name,
              (r.created_by = $2) AS is_creator,
              (current_step.approver_user_id = $2 AND current_step.status = 'PENDING'
                AND r.status = 'PENDING_APPROVAL') AS can_review
       FROM recruitment_approval_requests r
       JOIN users creator ON creator.id = r.created_by
       LEFT JOIN recruitment_approval_steps current_step
         ON current_step.request_id = r.id AND current_step.step_number = r.current_step
       WHERE r.id = $1
         AND (r.created_by = $2 OR EXISTS (
           SELECT 1 FROM recruitment_approval_steps assigned
           WHERE assigned.request_id = r.id AND assigned.approver_user_id = $2
         ))
       LIMIT 1`,
      [requestId, userId]
    );
    const request = requestResult.rows[0];
    if (!request) return null;

    const [stepsResult, historyResult] = await Promise.all([
      query(
        `SELECT s.step_number, s.status, s.comment, s.acted_at,
                reviewer.id AS approver_id, reviewer.full_name AS approver_name,
                reviewer.employee_code AS approver_employee_code,
                actor.full_name AS acted_by_name
         FROM recruitment_approval_steps s
         JOIN users reviewer ON reviewer.id = s.approver_user_id
         LEFT JOIN users actor ON actor.id = s.acted_by
         WHERE s.request_id = $1
         ORDER BY s.step_number`,
        [requestId]
      ),
      query(
        `SELECT h.id, h.step_number, h.action, h.comment, h.created_at,
                actor.full_name AS actor_name
         FROM recruitment_approval_history h
         LEFT JOIN users actor ON actor.id = h.actor_id
         WHERE h.request_id = $1
         ORDER BY h.created_at, h.id`,
        [requestId]
      )
    ]);
    return { ...request, steps: stepsResult.rows, history: historyResult.rows };
  }

  static async decide({ requestId, userId, action, comment }) {
    return withTransaction(async (client) => {
      const requestResult = await client.query(
        `SELECT id, status, current_step
         FROM recruitment_approval_requests
         WHERE id = $1
         FOR UPDATE`,
        [requestId]
      );
      const request = requestResult.rows[0];
      if (!request) return { error: 'NOT_FOUND' };
      if (request.status !== 'PENDING_APPROVAL') return { error: 'NOT_PENDING' };

      const stepResult = await client.query(
        `SELECT id, step_number, approver_user_id, status
         FROM recruitment_approval_steps
         WHERE request_id = $1 AND step_number = $2
         FOR UPDATE`,
        [requestId, request.current_step]
      );
      const step = stepResult.rows[0];
      if (!step || step.approver_user_id !== userId) return { error: 'FORBIDDEN' };
      if (step.status !== 'PENDING') return { error: 'NOT_PENDING' };

      const stepStatus = action === 'APPROVE' ? 'APPROVED'
        : action === 'REJECT' ? 'REJECTED' : 'NEED_MORE_INFO';
      const historyAction = action === 'APPROVE' ? 'APPROVED'
        : action === 'REJECT' ? 'REJECTED' : 'REQUEST_MORE_INFO';

      await client.query(
        `UPDATE recruitment_approval_steps
         SET status = $1, comment = $2, acted_by = $3, acted_at = NOW()
         WHERE id = $4`,
        [stepStatus, comment || null, userId, step.id]
      );

      let newStatus = request.status;
      let currentStep = request.current_step;
      if (action === 'REJECT') {
        newStatus = 'REJECTED';
      } else if (action === 'REQUEST_MORE_INFO') {
        newStatus = 'NEED_MORE_INFO';
      } else {
        const countResult = await client.query(
          'SELECT COUNT(*)::int AS total FROM recruitment_approval_steps WHERE request_id = $1',
          [requestId]
        );
        if (request.current_step >= countResult.rows[0].total) {
          newStatus = 'APPROVED';
        } else {
          currentStep += 1;
        }
      }

      await client.query(
        `UPDATE recruitment_approval_requests
         SET status = $1, current_step = $2
         WHERE id = $3`,
        [newStatus, currentStep, requestId]
      );
      await client.query(
        `INSERT INTO recruitment_approval_history
           (request_id, step_number, action, actor_id, comment)
         VALUES ($1, $2, $3, $4, $5)`,
        [requestId, step.step_number, historyAction, userId, comment || null]
      );
      return { status: newStatus, currentStep };
    });
  }

  static async resubmit({ requestId, userId, response }) {
    return withTransaction(async (client) => {
      const requestResult = await client.query(
        `SELECT id, created_by, status, current_step
         FROM recruitment_approval_requests
         WHERE id = $1
         FOR UPDATE`,
        [requestId]
      );
      const request = requestResult.rows[0];
      if (!request) return { error: 'NOT_FOUND' };
      if (request.created_by !== userId) return { error: 'FORBIDDEN' };
      if (request.status !== 'NEED_MORE_INFO') return { error: 'NOT_NEED_MORE_INFO' };

      const stepResult = await client.query(
        `SELECT id, step_number
         FROM recruitment_approval_steps
         WHERE request_id = $1 AND step_number = $2
         FOR UPDATE`,
        [requestId, request.current_step]
      );
      const step = stepResult.rows[0];
      if (!step) return { error: 'STEP_NOT_FOUND' };

      await client.query(
        `UPDATE recruitment_approval_steps
         SET status = 'PENDING', comment = NULL, acted_by = NULL, acted_at = NULL
         WHERE id = $1`,
        [step.id]
      );
      await client.query(
        `UPDATE recruitment_approval_requests
         SET status = 'PENDING_APPROVAL'
         WHERE id = $1`,
        [requestId]
      );
      await client.query(
        `INSERT INTO recruitment_approval_history
           (request_id, step_number, action, actor_id, comment)
         VALUES ($1, $2, 'RESUBMITTED', $3, $4)`,
        [requestId, step.step_number, userId, response]
      );
      return { status: 'PENDING_APPROVAL', currentStep: request.current_step };
    });
  }
}

module.exports = RecruitmentApprovalModel;

'use strict';
const { query } = require('../database/db');
const DataScopeService = require('../services/dataScopeService');

/**
 * CandidateController - RBAC-protected candidate operations
 *
 * Security guarantees:
 * - All routes require authMiddleware + requirePermission
 * - Data scope enforced at DB query level for Recruiter
 * - Salary fields stripped at data layer for unauthorized roles
 * - IDOR prevention: check scope before returning individual candidate
 */
class CandidateController {

  /**
   * GET /api/v1/candidates
   * List candidates with data scope filtering
   */
  static async list(req, res) {
    try {
      const { canViewSalary, hasRecruiterScope } = req.user;

      // Build scope filter for Recruiter
      const scopeFilter = await DataScopeService.buildCandidateScopeFilter(req.user);

      // Select salary fields based on permission
      const salarySelect = canViewSalary
        ? ', c.salary_expected'
        : '';  // DO NOT return salary if no permission

      const sql = `
        SELECT
          c.id, c.code, c.full_name, c.email, c.phone,
          c.position, c.department, c.status, c.notes,
          c.created_at, c.updated_at,
          j.title AS job_title, j.code AS job_code,
          j.id AS job_id
          ${salarySelect}
        FROM candidates c
        LEFT JOIN jobs j ON j.id = c.job_id
        WHERE 1=1
        ${scopeFilter.whereClause}
        ORDER BY c.created_at DESC
      `;

      const res2 = await query(sql, scopeFilter.params);

      return res.status(200).json({
        success: true,
        total: res2.rows.length,
        candidates: res2.rows,
        meta: {
          scopeRestricted: scopeFilter.hasRecruiterScope || hasRecruiterScope,
          salaryVisible: canViewSalary,
        }
      });

    } catch (err) {
      console.error('[CandidateController.list] Error:', err);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi tải danh sách ứng viên.'
      });
    }
  }

  /**
   * GET /api/v1/candidates/:id
   * Get candidate detail - with IDOR prevention
   */
  static async getById(req, res) {
    try {
      const candidateId = req.params.id;
      const { canViewSalary } = req.user;

      // Recruiter scope check: prevent IDOR
      if (req.user.role === 'chuyen_vien_tuyen_dung') {
        const canAccess = await DataScopeService.recruiterCanAccessCandidate(
          req.user.id,
          candidateId
        );
        if (!canAccess) {
          console.warn(`[RBAC] IDOR attempt: recruiter=${req.user.id} tried to access candidate=${candidateId}`);
          return res.status(403).json({
            success: false,
            code: 'FORBIDDEN',
            message: 'Bạn không có quyền truy cập ứng viên thuộc vị trí này.'
          });
        }
      }

      // Build salary select
      const salarySelect = canViewSalary ? ', c.salary_expected' : '';

      const result = await query(
        `SELECT
          c.id, c.code, c.full_name, c.email, c.phone,
          c.position, c.department, c.status, c.notes,
          c.created_at, c.updated_at,
          j.title AS job_title, j.code AS job_code,
          j.id AS job_id
          ${salarySelect}
         FROM candidates c
         LEFT JOIN jobs j ON j.id = c.job_id
         WHERE c.id = $1
         LIMIT 1`,
        [candidateId]
      );

      if (!result.rows[0]) {
        return res.status(404).json({
          success: false,
          message: `Không tìm thấy ứng viên có mã '${candidateId}'.`
        });
      }

      return res.status(200).json({
        success: true,
        candidate: result.rows[0]
      });

    } catch (err) {
      console.error('[CandidateController.getById] Error:', err);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi tải thông tin ứng viên.'
      });
    }
  }

  /**
   * POST /api/v1/candidates
   * Create new candidate
   */
  static async create(req, res) {
    try {
      const { full_name, email, phone, job_id, position, department, salary_expected, status, notes } = req.body;

      if (!full_name) {
        return res.status(400).json({ success: false, message: 'Họ tên ứng viên là bắt buộc.' });
      }

      // Generate candidate code
      const countRes = await query('SELECT COUNT(*) FROM candidates');
      const count = parseInt(countRes.rows[0].count) + 1;
      const year = new Date().getFullYear();
      const code = `UV-${year}-${String(count).padStart(3, '0')}`;

      const result = await query(
        `INSERT INTO candidates
           (code, full_name, email, phone, job_id, position, department, salary_expected, status, notes)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
         RETURNING id, code, full_name, email, phone, job_id, position, department, status, created_at`,
        [code, full_name, email, phone, job_id, position, department, salary_expected, status || 'screening', notes]
      );

      return res.status(201).json({
        success: true,
        message: 'Tạo hồ sơ ứng viên thành công.',
        candidate: result.rows[0]
      });

    } catch (err) {
      console.error('[CandidateController.create] Error:', err);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi tạo hồ sơ ứng viên.'
      });
    }
  }

  /**
   * PUT /api/v1/candidates/:id
   * Update candidate
   */
  static async update(req, res) {
    try {
      const candidateId = req.params.id;

      // Recruiter scope check for update
      if (req.user.role === 'chuyen_vien_tuyen_dung') {
        const canAccess = await DataScopeService.recruiterCanAccessCandidate(
          req.user.id,
          candidateId
        );
        if (!canAccess) {
          return res.status(403).json({
            success: false,
            code: 'FORBIDDEN',
            message: 'Bạn không có quyền cập nhật ứng viên thuộc vị trí này.'
          });
        }
      }

      const { full_name, email, phone, position, department, status, notes } = req.body;

      const result = await query(
        `UPDATE candidates
         SET full_name = COALESCE($1, full_name),
             email     = COALESCE($2, email),
             phone     = COALESCE($3, phone),
             position  = COALESCE($4, position),
             department = COALESCE($5, department),
             status    = COALESCE($6, status),
             notes     = COALESCE($7, notes),
             updated_at = NOW()
         WHERE id = $8
         RETURNING id, code, full_name, email, status, updated_at`,
        [full_name, email, phone, position, department, status, notes, candidateId]
      );

      if (!result.rows[0]) {
        return res.status(404).json({ success: false, message: 'Không tìm thấy ứng viên.' });
      }

      return res.status(200).json({
        success: true,
        message: 'Cập nhật hồ sơ ứng viên thành công.',
        candidate: result.rows[0]
      });

    } catch (err) {
      console.error('[CandidateController.update] Error:', err);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi cập nhật hồ sơ ứng viên.'
      });
    }
  }

  /**
   * DELETE /api/v1/candidates/:id
   */
  static async delete(req, res) {
    try {
      const candidateId = req.params.id;

      const result = await query(
        `DELETE FROM candidates WHERE id = $1 RETURNING id, code, full_name`,
        [candidateId]
      );

      if (!result.rows[0]) {
        return res.status(404).json({ success: false, message: 'Không tìm thấy ứng viên.' });
      }

      return res.status(200).json({
        success: true,
        message: 'Đã xóa hồ sơ ứng viên thành công.',
        deleted: result.rows[0]
      });

    } catch (err) {
      console.error('[CandidateController.delete] Error:', err);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi xóa hồ sơ ứng viên.'
      });
    }
  }
}

module.exports = CandidateController;

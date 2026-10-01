'use strict';
const { query } = require('../database/db');
const PermissionService = require('../services/permissionService');

/**
 * JobController - RBAC-protected job/vacancy operations
 *
 * Salary fields (salary_min, salary_max) are only returned
 * when the user has 'salary.view' permission.
 * This is enforced at the DB query/controller layer, NOT CSS hiding.
 */
class JobController {

  /**
   * GET /api/v1/jobs
   * List all jobs - salary fields conditionally included
   */
  static async list(req, res) {
    try {
      const canViewSalary = req.user.canViewSalary;

      // SECURITY: Do NOT select salary fields if no permission
      const salaryFields = canViewSalary
        ? ', j.salary_min, j.salary_max'
        : '';

      const result = await query(
        `SELECT
          j.id, j.code, j.title, j.department, j.open_count,
          j.deadline, j.status, j.created_at,
          u.full_name AS recruiter_name
          ${salaryFields}
         FROM jobs j
         LEFT JOIN users u ON u.id = j.recruiter_id
         WHERE j.status != 'cancelled'
         ORDER BY j.created_at DESC`,
        []
      );

      return res.status(200).json({
        success: true,
        total: result.rows.length,
        jobs: result.rows,
        meta: { salaryVisible: canViewSalary }
      });

    } catch (err) {
      console.error('[JobController.list] Error:', err);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi tải danh sách vị trí tuyển dụng.'
      });
    }
  }

  /**
   * GET /api/v1/jobs/:id
   * Get job detail - salary conditionally included
   */
  static async getById(req, res) {
    try {
      const jobId = req.params.id;
      const canViewSalary = req.user.canViewSalary;

      const salaryFields = canViewSalary
        ? ', j.salary_min, j.salary_max'
        : '';

      const result = await query(
        `SELECT
          j.id, j.code, j.title, j.department, j.description,
          j.open_count, j.deadline, j.status, j.created_at, j.updated_at,
          u.full_name AS recruiter_name, u.id AS recruiter_id
          ${salaryFields}
         FROM jobs j
         LEFT JOIN users u ON u.id = j.recruiter_id
         WHERE j.id = $1
         LIMIT 1`,
        [jobId]
      );

      if (!result.rows[0]) {
        return res.status(404).json({
          success: false,
          message: `Không tìm thấy vị trí tuyển dụng '${jobId}'.`
        });
      }

      return res.status(200).json({
        success: true,
        job: result.rows[0],
        meta: { salaryVisible: canViewSalary }
      });

    } catch (err) {
      console.error('[JobController.getById] Error:', err);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi tải thông tin vị trí tuyển dụng.'
      });
    }
  }

  /**
   * POST /api/v1/jobs
   */
  static async create(req, res) {
    try {
      const { code, title, department, description, salary_min, salary_max, open_count, deadline, recruiter_id } = req.body;

      if (!title) {
        return res.status(400).json({ success: false, message: 'Tên vị trí tuyển dụng là bắt buộc.' });
      }

      const jobCode = code || `VT-${new Date().getFullYear()}-${Date.now().toString().slice(-4)}`;

      const result = await query(
        `INSERT INTO jobs
           (code, title, department, description, salary_min, salary_max, open_count, deadline, recruiter_id)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
         RETURNING id, code, title, department, open_count, status, created_at`,
        [jobCode, title, department, description, salary_min, salary_max, open_count || 1, deadline, recruiter_id]
      );

      return res.status(201).json({
        success: true,
        message: 'Tạo vị trí tuyển dụng thành công.',
        job: result.rows[0]
      });

    } catch (err) {
      console.error('[JobController.create] Error:', err);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi tạo vị trí tuyển dụng.'
      });
    }
  }

  /**
   * PUT /api/v1/jobs/:id
   */
  static async update(req, res) {
    try {
      const jobId = req.params.id;
      const { title, department, description, salary_min, salary_max, open_count, deadline, status } = req.body;

      const result = await query(
        `UPDATE jobs
         SET title      = COALESCE($1, title),
             department = COALESCE($2, department),
             description = COALESCE($3, description),
             salary_min  = COALESCE($4, salary_min),
             salary_max  = COALESCE($5, salary_max),
             open_count  = COALESCE($6, open_count),
             deadline    = COALESCE($7, deadline),
             status      = COALESCE($8, status),
             updated_at  = NOW()
         WHERE id = $9
         RETURNING id, code, title, department, status, updated_at`,
        [title, department, description, salary_min, salary_max, open_count, deadline, status, jobId]
      );

      if (!result.rows[0]) {
        return res.status(404).json({ success: false, message: 'Không tìm thấy vị trí tuyển dụng.' });
      }

      return res.status(200).json({
        success: true,
        message: 'Cập nhật vị trí tuyển dụng thành công.',
        job: result.rows[0]
      });

    } catch (err) {
      console.error('[JobController.update] Error:', err);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi cập nhật vị trí tuyển dụng.'
      });
    }
  }

  /**
   * DELETE /api/v1/jobs/:id
   */
  static async delete(req, res) {
    try {
      const jobId = req.params.id;

      const result = await query(
        `DELETE FROM jobs WHERE id = $1 RETURNING id, code, title`,
        [jobId]
      );

      if (!result.rows[0]) {
        return res.status(404).json({ success: false, message: 'Không tìm thấy vị trí tuyển dụng.' });
      }

      return res.status(200).json({
        success: true,
        message: 'Đã xóa vị trí tuyển dụng thành công.',
        deleted: result.rows[0]
      });

    } catch (err) {
      console.error('[JobController.delete] Error:', err);
      return res.status(500).json({
        success: false,
        message: 'Lỗi hệ thống khi xóa vị trí tuyển dụng.'
      });
    }
  }
}

module.exports = JobController;

'use strict';
const { query } = require('../database/db');

/**
 * DataScopeService - Enforces data-level authorization for Recruiter role
 *
 * Key rule: chuyen_vien_tuyen_dung can ONLY access candidates
 * belonging to jobs where they are the assigned recruiter.
 *
 * Security principle: Filter at DB query level, never at application/FE level.
 */
class DataScopeService {

  /**
   * Get the list of job IDs that a recruiter is responsible for.
   * @param {string} recruiterId - UUID of the recruiter user
   * @returns {Promise<string[]>} Array of job IDs (UUIDs)
   */
  static async getRecruiterJobIds(recruiterId) {
    const res = await query(
      `SELECT id FROM jobs
       WHERE recruiter_id = $1 AND status != 'cancelled'`,
      [recruiterId]
    );
    return res.rows.map(r => r.id);
  }

  /**
   * Build a scoped candidate WHERE clause for Recruiter.
   * Returns SQL fragment and params to inject into queries.
   *
   * @param {object} user - req.user
   * @returns {Promise<{whereClause: string, params: any[], offset: number}>}
   */
  static async buildCandidateScopeFilter(user) {
    // System Admin, Manager, Director, HR Staff => no scope restriction
    const NO_SCOPE_ROLES = [
      'quan_tri',
      'quan_ly_tuyen_dung',
      'truong_phong',
      'giam_doc',
      'nhan_su',
    ];

    if (NO_SCOPE_ROLES.includes(user.role)) {
      return { whereClause: '', params: [], offset: 0 };
    }

    // Recruiter: scope by assigned jobs
    if (user.role === 'chuyen_vien_tuyen_dung') {
      const jobIds = await DataScopeService.getRecruiterJobIds(user.id);
      if (jobIds.length === 0) {
        // Recruiter has no assigned jobs -> return empty
        return {
          whereClause: 'AND FALSE',
          params: [],
          offset: 0,
          hasNoJobs: true
        };
      }
      return {
        whereClause: `AND c.job_id = ANY($1)`,
        params: [jobIds],
        offset: 1
      };
    }

    // Interviewer: can view all candidates (read-only, no salary)
    if (user.role === 'nguoi_phong_van') {
      return { whereClause: '', params: [], offset: 0 };
    }

    // Unknown role -> deny all (default deny)
    return {
      whereClause: 'AND FALSE',
      params: [],
      offset: 0
    };
  }

  /**
   * Check if a recruiter can access a specific candidate.
   * Used for GET /candidates/:id to prevent IDOR.
   *
   * @param {string} userId - recruiter's user ID
   * @param {string} candidateId - UUID of candidate
   * @returns {Promise<boolean>}
   */
  static async recruiterCanAccessCandidate(userId, candidateId) {
    const res = await query(
      `SELECT c.id
       FROM candidates c
       JOIN jobs j ON j.id = c.job_id
       WHERE c.id = $1
         AND j.recruiter_id = $2`,
      [candidateId, userId]
    );
    return res.rows.length > 0;
  }

  /**
   * Scope a salary fields based on role permission
   * Removes salary_expected and related fields for unauthorized roles
   *
   * @param {object|array} data - candidate data
   * @param {boolean} canViewSalary - whether user can view salary
   * @returns {object|array} - sanitized data
   */
  static stripSalaryFields(data, canViewSalary) {
    if (canViewSalary) return data;

    const SALARY_FIELDS = ['salary_expected', 'salary_min', 'salary_max', 'salaryExpected', 'salaryMin', 'salaryMax'];

    const strip = (obj) => {
      if (!obj || typeof obj !== 'object') return obj;
      const result = { ...obj };
      for (const field of SALARY_FIELDS) {
        delete result[field];
      }
      return result;
    };

    if (Array.isArray(data)) {
      return data.map(strip);
    }
    return strip(data);
  }
}

module.exports = DataScopeService;

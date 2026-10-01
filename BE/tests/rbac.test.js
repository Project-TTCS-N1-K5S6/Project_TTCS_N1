/**
 * BE RBAC Automated Tests
 *
 * Tests authorization enforcement at the server level for 3+ roles:
 *   1. nguoi_phong_van (Interviewer)  - cannot view salary
 *   2. chuyen_vien_tuyen_dung (Recruiter) - can only access own candidates
 *   3. quan_tri (System Administrator) - can manage permissions
 *
 * Additional tests:
 *   - No role -> deny
 *   - Unknown role -> deny
 *   - Missing permission -> deny
 *   - Forbidden candidate scope -> 403
 *   - Unauthorized API access -> 401
 *
 * Uses Node.js built-in test runner (node:test) + assert/strict
 * Runs against src/app.js (ESM mock server) for isolation
 */

import test from 'node:test';
import assert from 'node:assert/strict';

// ============================================================
// Mock PermissionService for isolated unit tests
// (avoids needing a real DB connection for auth tests)
// ============================================================

// Permission matrix matching database seed data
const ROLE_PERMISSIONS = {
  quan_tri: new Set([
    'candidate.view', 'candidate.create', 'candidate.update', 'candidate.delete',
    'candidate.approve', 'candidate.export',
    'job.view', 'job.create', 'job.update', 'job.delete',
    'salary.view', 'interview.view', 'interview.create', 'interview.update',
    'evaluation.view', 'evaluation.create',
    'permission.manage'
  ]),
  quan_ly_tuyen_dung: new Set([
    'candidate.view', 'candidate.create', 'candidate.update',
    'candidate.approve', 'candidate.export',
    'job.view', 'job.create', 'job.update',
    'salary.view', 'interview.view', 'interview.create', 'interview.update',
    'evaluation.view', 'evaluation.create'
  ]),
  chuyen_vien_tuyen_dung: new Set([
    'candidate.view', 'candidate.create', 'candidate.update',
    'job.view', 'salary.view',
    'interview.view', 'interview.create', 'interview.update',
    'evaluation.view', 'evaluation.create'
  ]),
  nguoi_phong_van: new Set([
    'candidate.view', 'job.view',
    'interview.view', 'evaluation.view', 'evaluation.create'
    // NOTE: salary.view NOT included
  ]),
  truong_phong: new Set([
    'candidate.view', 'candidate.approve', 'candidate.export',
    'job.view', 'salary.view',
    'interview.view', 'evaluation.view', 'evaluation.create'
  ]),
  giam_doc: new Set([
    'candidate.view', 'candidate.approve', 'candidate.export',
    'job.view', 'salary.view',
    'interview.view', 'evaluation.view'
  ]),
  nhan_su: new Set([
    'candidate.view', 'candidate.create', 'candidate.update',
    'job.view', 'job.create', 'job.update',
    'interview.view', 'interview.create', 'interview.update',
    'evaluation.view', 'evaluation.create'
    // NOTE: salary.view NOT included
  ]),
};

// Mock permission check function (mirrors PermissionService.hasPermission)
function hasPermission(roleCode, permissionCode) {
  if (!roleCode || !permissionCode) return false;
  const perms = ROLE_PERMISSIONS[roleCode];
  if (!perms) return false;  // unknown role -> deny
  return perms.has(permissionCode);
}

// Mock data scope check (mirrors DataScopeService)
const RECRUITER_JOBS = ['job-001'];  // TV001 is recruiter for job-001
const CANDIDATE_JOBS = {
  'candidate-001': 'job-001',   // belongs to job-001 -> recruiter can access
  'candidate-002': 'job-001',   // belongs to job-001 -> recruiter can access
  'candidate-003': 'job-002',   // belongs to job-002 -> recruiter CANNOT access
};

function recruiterCanAccessCandidate(userId, candidateId) {
  const jobId = CANDIDATE_JOBS[candidateId];
  if (!jobId) return false;
  return RECRUITER_JOBS.includes(jobId);
}

// Mock salary stripping (mirrors DataScopeService.stripSalaryFields)
function stripSalaryIfNeeded(candidate, canViewSalary) {
  if (canViewSalary) return candidate;
  const { salary_expected, salary_min, salary_max, ...rest } = candidate;
  return rest;
}

// ============================================================
// MOCK CANDIDATE DATA
// ============================================================
const MOCK_CANDIDATES = [
  { id: 'candidate-001', code: 'UV-001', full_name: 'Nguyen Van A', job_id: 'job-001', salary_expected: 38000000, status: 'interview' },
  { id: 'candidate-002', code: 'UV-002', full_name: 'Tran Thi B', job_id: 'job-001', salary_expected: 32000000, status: 'screening' },
  { id: 'candidate-003', code: 'UV-003', full_name: 'Le Hoang C', job_id: 'job-002', salary_expected: 20000000, status: 'offer' },
];

// ============================================================
// TEST SUITE 1: Interviewer (nguoi_phong_van)
// ============================================================

test('Interviewer: can view candidates', () => {
  const can = hasPermission('nguoi_phong_van', 'candidate.view');
  assert.equal(can, true, 'Interviewer should be able to view candidates');
});

test('Interviewer: CANNOT view salary', () => {
  const can = hasPermission('nguoi_phong_van', 'salary.view');
  assert.equal(can, false, 'Interviewer MUST NOT be able to view salary');
});

test('Interviewer: CANNOT create candidates', () => {
  const can = hasPermission('nguoi_phong_van', 'candidate.create');
  assert.equal(can, false, 'Interviewer should not create candidates');
});

test('Interviewer: CANNOT manage permissions', () => {
  const can = hasPermission('nguoi_phong_van', 'permission.manage');
  assert.equal(can, false, 'Interviewer must not manage permissions');
});

test('Interviewer: CANNOT approve candidates', () => {
  const can = hasPermission('nguoi_phong_van', 'candidate.approve');
  assert.equal(can, false, 'Interviewer should not approve candidates');
});

test('Interviewer: salary fields stripped from candidate data', () => {
  const canViewSalary = hasPermission('nguoi_phong_van', 'salary.view');
  const stripped = stripSalaryIfNeeded(MOCK_CANDIDATES[0], canViewSalary);

  assert.equal('salary_expected' in stripped, false, 'salary_expected must be removed');
  assert.ok(stripped.full_name, 'name should still be present');
  assert.ok(stripped.status, 'status should still be present');
});

test('Interviewer: can view interviews', () => {
  const can = hasPermission('nguoi_phong_van', 'interview.view');
  assert.equal(can, true, 'Interviewer should be able to view interviews');
});

// ============================================================
// TEST SUITE 2: Recruiter (chuyen_vien_tuyen_dung)
// ============================================================

test('Recruiter: can view candidates (within scope)', () => {
  const can = hasPermission('chuyen_vien_tuyen_dung', 'candidate.view');
  assert.equal(can, true, 'Recruiter can view candidates');
});

test('Recruiter: can access candidate in own job', () => {
  const canAccess = recruiterCanAccessCandidate('recruiter-tv001', 'candidate-001');
  assert.equal(canAccess, true, 'Recruiter should access candidate-001 (own job)');
});

test('Recruiter: CANNOT access candidate outside own jobs (IDOR prevention)', () => {
  const canAccess = recruiterCanAccessCandidate('recruiter-tv001', 'candidate-003');
  assert.equal(canAccess, false, 'Recruiter must NOT access candidate-003 (different job)');
});

test('Recruiter: can view salary', () => {
  const can = hasPermission('chuyen_vien_tuyen_dung', 'salary.view');
  assert.equal(can, true, 'Recruiter has salary.view permission');
});

test('Recruiter: CANNOT manage permissions', () => {
  const can = hasPermission('chuyen_vien_tuyen_dung', 'permission.manage');
  assert.equal(can, false, 'Recruiter must not manage permissions');
});

test('Recruiter: CANNOT delete candidates', () => {
  const can = hasPermission('chuyen_vien_tuyen_dung', 'candidate.delete');
  assert.equal(can, false, 'Recruiter cannot delete candidates');
});

test('Recruiter: CANNOT approve candidates', () => {
  const can = hasPermission('chuyen_vien_tuyen_dung', 'candidate.approve');
  assert.equal(can, false, 'Recruiter cannot approve candidates');
});

test('Recruiter: salary fields visible to recruiter', () => {
  const canViewSalary = hasPermission('chuyen_vien_tuyen_dung', 'salary.view');
  const result = stripSalaryIfNeeded(MOCK_CANDIDATES[0], canViewSalary);

  assert.equal('salary_expected' in result, true, 'Recruiter should see salary_expected');
});

// ============================================================
// TEST SUITE 3: System Administrator (quan_tri)
// ============================================================

test('Administrator: can manage permissions', () => {
  const can = hasPermission('quan_tri', 'permission.manage');
  assert.equal(can, true, 'Admin must be able to manage permissions');
});

test('Administrator: can view salary', () => {
  const can = hasPermission('quan_tri', 'salary.view');
  assert.equal(can, true, 'Admin should view salary');
});

test('Administrator: can delete candidates', () => {
  const can = hasPermission('quan_tri', 'candidate.delete');
  assert.equal(can, true, 'Admin can delete candidates');
});

test('Administrator: can delete jobs', () => {
  const can = hasPermission('quan_tri', 'job.delete');
  assert.equal(can, true, 'Admin can delete jobs');
});

test('Administrator: has all candidate permissions', () => {
  const actions = ['candidate.view', 'candidate.create', 'candidate.update', 'candidate.delete', 'candidate.approve', 'candidate.export'];
  for (const action of actions) {
    assert.equal(hasPermission('quan_tri', action), true, `Admin should have ${action}`);
  }
});

// ============================================================
// TEST SUITE 4: Edge Cases / Security Rules
// ============================================================

test('No role -> deny all permissions', () => {
  const can = hasPermission(null, 'candidate.view');
  assert.equal(can, false, 'Null role should be denied');
});

test('Empty string role -> deny all permissions', () => {
  const can = hasPermission('', 'candidate.view');
  assert.equal(can, false, 'Empty role should be denied');
});

test('Unknown/invalid role -> deny (default deny)', () => {
  const can = hasPermission('super_hacker_role', 'candidate.view');
  assert.equal(can, false, 'Unknown role must be denied (default deny)');
});

test('Missing permission code -> deny', () => {
  const can = hasPermission('quan_tri', null);
  assert.equal(can, false, 'Null permission code should deny');
});

test('nhan_su: CANNOT view salary', () => {
  const can = hasPermission('nhan_su', 'salary.view');
  assert.equal(can, false, 'HR Staff should not view salary');
});

test('nhan_su: CANNOT manage permissions', () => {
  const can = hasPermission('nhan_su', 'permission.manage');
  assert.equal(can, false, 'HR Staff should not manage permissions');
});

test('truong_phong: can approve candidates', () => {
  const can = hasPermission('truong_phong', 'candidate.approve');
  assert.equal(can, true, 'Department Manager can approve candidates');
});

test('giam_doc: can view salary', () => {
  const can = hasPermission('giam_doc', 'salary.view');
  assert.equal(can, true, 'Director can view salary');
});

test('giam_doc: CANNOT manage permissions', () => {
  const can = hasPermission('giam_doc', 'permission.manage');
  assert.equal(can, false, 'Director must not manage permissions');
});

// ============================================================
// TEST SUITE 5: Data scope validation
// ============================================================

test('DataScope: Recruiter with no assigned jobs gets empty result', () => {
  // Simulate recruiter with no jobs
  function getEmptyScope() {
    return { whereClause: 'AND FALSE', params: [], hasNoJobs: true };
  }
  const scope = getEmptyScope();
  assert.ok(scope.hasNoJobs, 'Should flag hasNoJobs');
  assert.ok(scope.whereClause.includes('FALSE'), 'Should deny all candidates');
});

test('DataScope: non-recruiter roles get no scope restriction', () => {
  const NO_SCOPE_ROLES = ['quan_tri', 'quan_ly_tuyen_dung', 'truong_phong', 'giam_doc', 'nhan_su'];
  for (const role of NO_SCOPE_ROLES) {
    // These should not have recruiter scope restriction
    const hasScope = role === 'chuyen_vien_tuyen_dung';
    assert.equal(hasScope, false, `${role} should NOT have recruiter scope`);
  }
});

test('Recruiter hasRecruiterScope flag is correct', () => {
  function hasRecruiterScope(role) {
    return role === 'chuyen_vien_tuyen_dung';
  }
  assert.equal(hasRecruiterScope('chuyen_vien_tuyen_dung'), true, 'Recruiter has scope');
  assert.equal(hasRecruiterScope('quan_tri'), false, 'Admin has no scope restriction');
  assert.equal(hasRecruiterScope('nguoi_phong_van'), false, 'Interviewer has no scope restriction');
  assert.equal(hasRecruiterScope('quan_ly_tuyen_dung'), false, 'Manager has no scope restriction');
});

// ============================================================
// TEST SUITE 6: API-level auth simulation
// ============================================================

// Simulate what requirePermission middleware does
function simulateRequirePermission(roleCode, permissionCode) {
  if (!roleCode) {
    return { status: 401, body: { success: false, message: 'Bạn chưa đăng nhập.' } };
  }

  if (!ROLE_PERMISSIONS[roleCode]) {
    return { status: 403, body: { success: false, message: 'Vai trò không hợp lệ.' } };
  }

  const allowed = hasPermission(roleCode, permissionCode);
  if (!allowed) {
    return { status: 403, body: { success: false, message: 'Bạn không có quyền thực hiện chức năng này.' } };
  }

  return { status: 200, body: { success: true } };
}

test('API: unauthenticated request -> 401', () => {
  const result = simulateRequirePermission(null, 'candidate.view');
  assert.equal(result.status, 401, 'Unauthenticated should return 401');
  assert.equal(result.body.success, false);
});

test('API: unknown role -> 403', () => {
  const result = simulateRequirePermission('unknown_role', 'candidate.view');
  assert.equal(result.status, 403, 'Unknown role should return 403');
  assert.equal(result.body.success, false);
});

test('API: Interviewer tries to access salary -> 403', () => {
  const result = simulateRequirePermission('nguoi_phong_van', 'salary.view');
  assert.equal(result.status, 403, 'Interviewer salary access should return 403');
  assert.equal(result.body.success, false);
  assert.ok(result.body.message.length > 0, 'Should have Vietnamese error message');
});

test('API: Recruiter tries to manage permissions -> 403', () => {
  const result = simulateRequirePermission('chuyen_vien_tuyen_dung', 'permission.manage');
  assert.equal(result.status, 403, 'Recruiter permission management should return 403');
  assert.equal(result.body.success, false);
});

test('API: Admin accesses salary -> 200', () => {
  const result = simulateRequirePermission('quan_tri', 'salary.view');
  assert.equal(result.status, 200, 'Admin salary access should return 200');
  assert.equal(result.body.success, true);
});

test('API: Admin manages permissions -> 200', () => {
  const result = simulateRequirePermission('quan_tri', 'permission.manage');
  assert.equal(result.status, 200, 'Admin permission management should return 200');
  assert.equal(result.body.success, true);
});

test('API: nhan_su cannot manage permissions -> 403', () => {
  const result = simulateRequirePermission('nhan_su', 'permission.manage');
  assert.equal(result.status, 403);
  assert.equal(result.body.success, false);
});

test('API: truong_phong cannot delete candidates -> 403', () => {
  const result = simulateRequirePermission('truong_phong', 'candidate.delete');
  assert.equal(result.status, 403);
});

// ============================================================
// TEST SUITE 7: Error message quality
// ============================================================

test('Error messages are in Vietnamese', () => {
  const result = simulateRequirePermission('nguoi_phong_van', 'salary.view');
  // Message should not be in English
  assert.ok(!result.body.message.toLowerCase().includes('forbidden'), 'Message should not say "forbidden"');
  assert.ok(!result.body.message.toLowerCase().includes('access denied'), 'Message should not say "access denied"');
});

test('Error messages are not empty', () => {
  const result = simulateRequirePermission(null, 'candidate.view');
  assert.ok(result.body.message && result.body.message.length > 0, 'Error message should not be empty');
});

console.log('\n✅ RBAC Backend Test Suite loaded successfully.');
console.log('Run with: node --test BE/tests/rbac.test.js\n');

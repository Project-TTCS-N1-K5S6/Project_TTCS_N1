import test from 'node:test';
import assert from 'node:assert/strict';
import app from '../src/app.js';
import { HttpStatus, ErrorCode } from '../src/constants/httpStatus.js';

// Simple HTTP request helper using native fetch
const request = async (path, options = {}) => {
  const server = app.listen(0);
  const port = server.address().port;
  try {
    const res = await fetch(`http://localhost:${port}${path}`, options);
    const data = await res.json();
    return { status: res.status, body: data };
  } finally {
    server.close();
  }
};

test('KN-76 & KN-78: Should return standardized 404 error when API route does not exist', async () => {
  const res = await request('/api/v1/non-existent-endpoint');
  assert.equal(res.status, HttpStatus.NOT_FOUND);
  assert.equal(res.body.success, false);
  assert.equal(res.body.status, 404);
  assert.equal(res.body.errorCode, ErrorCode.NOT_FOUND);
  assert.ok(res.body.message);
  assert.ok(res.body.hint);
  assert.ok(res.body.timestamp);
});

test('KN-76, KN-77, KN-78: Should return standardized 403 Forbidden when standard user accesses salary reports', async () => {
  const res = await request('/api/v1/recruitment/admin/salary-reports', {
    headers: { 'x-user-role': 'EMPLOYEE' }
  });
  assert.equal(res.status, HttpStatus.FORBIDDEN);
  assert.equal(res.body.success, false);
  assert.equal(res.body.errorCode, ErrorCode.FORBIDDEN);
  assert.match(res.body.hint, /HR_ADMIN|Quản trị viên/);
});

test('KN-76, KN-77, KN-78: Should return 200 OK when HR_ADMIN accesses salary reports', async () => {
  const res = await request('/api/v1/recruitment/admin/salary-reports', {
    headers: { 'x-user-role': 'HR_ADMIN' }
  });
  assert.equal(res.status, HttpStatus.OK);
  assert.equal(res.body.success, true);
  assert.ok(res.body.data);
});

test('KN-77 & KN-78: Should return 404 NotFoundException for unknown candidate ID', async () => {
  const res = await request('/api/v1/recruitment/candidates/UV-9999');
  assert.equal(res.status, HttpStatus.NOT_FOUND);
  assert.equal(res.body.errorCode, ErrorCode.NOT_FOUND);
  assert.match(res.body.message, /UV-9999/);
});

test('KN-77 & KN-78: Should handle server errors with 500 status and standardized structure', async () => {
  const res = await request('/api/v1/system/crash-test');
  assert.equal(res.status, HttpStatus.INTERNAL_SERVER_ERROR);
  assert.equal(res.body.errorCode, ErrorCode.INTERNAL_SERVER_ERROR);
  assert.ok(res.body.hint);
});

import test from 'node:test';
import assert from 'node:assert/strict';
import app from '../src/app.js';
import { HttpStatus, ErrorCode } from '../src/constants/httpStatus.js';

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

test('KN-90: Should list system roles and users with multiple assigned roles', async () => {
  // Fetch roles
  const rolesRes = await request('/api/v1/roles');
  assert.equal(rolesRes.status, HttpStatus.OK);
  assert.equal(rolesRes.body.success, true);
  assert.ok(Array.isArray(rolesRes.body.data));
  assert.ok(rolesRes.body.data.some(r => r.code === 'HIRING_MANAGER'));
  assert.ok(rolesRes.body.data.some(r => r.code === 'INTERVIEWER'));

  // Fetch users
  const usersRes = await request('/api/v1/users');
  assert.equal(usersRes.status, HttpStatus.OK);
  assert.equal(usersRes.body.success, true);

  // Verify user USR-002 holds both HIRING_MANAGER and INTERVIEWER simultaneously
  const headUser = usersRes.body.data.find(u => u.id === 'USR-002');
  assert.ok(headUser);
  assert.deepEqual(headUser.roles, ['HIRING_MANAGER', 'INTERVIEWER']);
});

test('KN-90: Should update multiple roles for a user (Assigning & Revoking roles)', async () => {
  const res = await request('/api/v1/users/USR-003/roles', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'x-user-id': 'USR-001'
    },
    body: JSON.stringify({
      roles: ['INTERVIEWER', 'HIRING_MANAGER']
    })
  });

  assert.equal(res.status, HttpStatus.OK);
  assert.equal(res.body.success, true);
  assert.deepEqual(res.body.data.roles, ['INTERVIEWER', 'HIRING_MANAGER']);
});

test('KN-91: Should block self-revocation of Admin role for the logged-in admin user', async () => {
  const res = await request('/api/v1/users/USR-001/roles', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'x-user-id': 'USR-001' // Self update
    },
    body: JSON.stringify({
      roles: ['HIRING_MANAGER'] // Removing ADMIN role!
    })
  });

  assert.equal(res.status, HttpStatus.FORBIDDEN);
  assert.equal(res.body.success, false);
  assert.equal(res.body.errorCode, ErrorCode.SELF_ADMIN_REVOCATION_BLOCKED);
  assert.match(res.body.message, /Không thể tự thu hồi vai trò Quản trị/);
  assert.ok(res.body.hint);
});

test('KN-91: Should allow another Admin to modify roles of user USR-001', async () => {
  const res = await request('/api/v1/users/USR-001/roles', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'x-user-id': 'USR-999' // Different admin requester
    },
    body: JSON.stringify({
      roles: ['ADMIN', 'HIRING_MANAGER', 'INTERVIEWER']
    })
  });

  assert.equal(res.status, HttpStatus.OK);
  assert.equal(res.body.success, true);
});

test('KN-92: Should apply updated permissions immediately on the very next operation', async () => {
  // Step 1: Set user USR-005 to EMPLOYEE only
  await request('/api/v1/users/USR-005/roles', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', 'x-user-id': 'USR-001' },
    body: JSON.stringify({ roles: ['EMPLOYEE'] })
  });

  // Check access for HIRING_MANAGER -> Should fail immediately
  const checkFail = await request('/api/v1/users/check-access?role=HIRING_MANAGER', {
    headers: { 'x-user-id': 'USR-005' }
  });
  assert.equal(checkFail.status, HttpStatus.FORBIDDEN);
  assert.equal(checkFail.body.success, false);

  // Step 2: Dynamically add HIRING_MANAGER role to USR-005
  await request('/api/v1/users/USR-005/roles', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', 'x-user-id': 'USR-001' },
    body: JSON.stringify({ roles: ['EMPLOYEE', 'HIRING_MANAGER'] })
  });

  // Step 3: Check access for HIRING_MANAGER on immediate next operation -> Should succeed!
  const checkSuccess = await request('/api/v1/users/check-access?role=HIRING_MANAGER', {
    headers: { 'x-user-id': 'USR-005' }
  });
  assert.equal(checkSuccess.status, HttpStatus.OK);
  assert.equal(checkSuccess.body.success, true);
  assert.equal(checkSuccess.body.data.accessGranted, true);
});

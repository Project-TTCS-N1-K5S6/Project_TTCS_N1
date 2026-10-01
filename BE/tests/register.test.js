import test from 'node:test';
import assert from 'node:assert/strict';
import app from '../src/app.js';
import { HttpStatus } from '../src/constants/httpStatus.js';

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

test('Register API: Should reject request with missing fields', async () => {
  const res = await request('/api/v1/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'test@company.com' })
  });

  assert.equal(res.status, HttpStatus.UNPROCESSABLE_ENTITY);
  assert.equal(res.body.success, false);
});

test('Register API: Should automatically generate incremental PV employee code (e.g. PV002, PV003)', async () => {
  // First registration
  const user1 = await request('/api/v1/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      fullName: 'Nguyen Van Test 1',
      email: 'test1@company.com',
      password: 'Password123'
    })
  });

  assert.equal(user1.status, 201);
  assert.equal(user1.body.success, true);
  assert.ok(user1.body.user.employeeCode.startsWith('PV'));
  const firstCode = user1.body.user.employeeCode;

  // Second registration
  const user2 = await request('/api/v1/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      fullName: 'Tran Thi Test 2',
      email: 'test2@company.com',
      password: 'Password123'
    })
  });

  assert.equal(user2.status, 201);
  assert.equal(user2.body.success, true);
  const secondCode = user2.body.user.employeeCode;

  // Verify incremental behavior
  const num1 = parseInt(firstCode.replace('PV', ''), 10);
  const num2 = parseInt(secondCode.replace('PV', ''), 10);
  assert.equal(num2, num1 + 1, 'Mã nhân sự kế tiếp phải tăng dần đúng 1 đơn vị');
});

test('Register API: Should reject duplicate email', async () => {
  const res = await request('/api/v1/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      fullName: 'Duplicate Email User',
      email: 'test1@company.com', // Already registered above
      password: 'Password123'
    })
  });

  assert.equal(res.status, HttpStatus.UNPROCESSABLE_ENTITY);
  assert.equal(res.body.success, false);
});

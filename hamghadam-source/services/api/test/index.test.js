import test from 'node:test';
import assert from 'node:assert/strict';

import handler from '../index.js';

function mockRes() {
  return {
    statusCode: 0,
    headers: {},
    body: '',
    setHeader(name, value) {
      this.headers[String(name).toLowerCase()] = value;
    },
    end(chunk) {
      this.body += chunk === undefined || chunk === null ? '' : String(chunk);
    },
  };
}

test('root route describes the service without secrets', async () => {
  const res = mockRes();
  await handler({ method: 'GET', url: '/', headers: {} }, res);

  assert.equal(res.statusCode, 200);
  const body = JSON.parse(res.body);
  assert.equal(body.service, 'hamghadam-api');
  assert.equal(JSON.stringify(body).includes('process.env'), false);
});

test('GET /api/health reaches the health handler', async () => {
  const res = mockRes();
  await handler({ method: 'GET', url: '/api/health', headers: {} }, res);

  assert.equal(res.statusCode, 200);
  const body = JSON.parse(res.body);
  assert.equal(body.status, 'ok');
  assert.equal(typeof body.capabilities.agent, 'boolean');
});

test('trailing slashes and query strings still resolve', async () => {
  const res = mockRes();
  await handler({ method: 'GET', url: '/api/health/?probe=1', headers: {} }, res);
  assert.equal(res.statusCode, 200);
  assert.equal(JSON.parse(res.body).status, 'ok');
});

test('POST /api/agent without a token is a 401, not a 404', async () => {
  const res = mockRes();
  await handler({ method: 'POST', url: '/api/agent', headers: {}, body: { message: 'سلام' } }, res);

  assert.equal(res.statusCode, 401);
  assert.equal(JSON.parse(res.body).error, 'missing_token');
});

test('unknown routes are a JSON 404', async () => {
  const res = mockRes();
  await handler({ method: 'GET', url: '/api/unknown', headers: {} }, res);

  assert.equal(res.statusCode, 404);
  assert.equal(JSON.parse(res.body).error, 'not_found');
});

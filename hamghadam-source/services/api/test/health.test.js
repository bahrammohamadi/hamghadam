import test from 'node:test';
import assert from 'node:assert/strict';

import { createHealthHandler } from '../api/health.js';

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

test('GET /api/health reports ok with capability booleans only', () => {
  const handler = createHealthHandler({
    env: () => ({ SUPABASE_URL: 'https://demo.supabase.co', SUPABASE_ANON_KEY: 'public-anon' }),
    now: () => new Date('2026-01-02T03:04:05.000Z'),
  });
  const res = mockRes();
  handler({ method: 'GET', headers: {} }, res);

  assert.equal(res.statusCode, 200);
  assert.match(res.headers['content-type'], /application\/json/);
  assert.equal(res.headers['cache-control'], 'no-store');

  const body = JSON.parse(res.body);
  assert.equal(body.status, 'ok');
  assert.equal(body.time, '2026-01-02T03:04:05.000Z');
  assert.equal(body.capabilities.auth, true);
  assert.equal(body.capabilities.agent, false);
  assert.equal(body.capabilities.audit, false);

  // The health payload must never echo configured secrets.
  assert.equal(res.body.includes('public-anon'), false);
});

test('POST /api/health is rejected with 405', () => {
  const handler = createHealthHandler({ env: () => ({}) });
  const res = mockRes();
  handler({ method: 'POST', headers: {} }, res);
  assert.equal(res.statusCode, 405);
  assert.equal(JSON.parse(res.body).error, 'method_not_allowed');
});

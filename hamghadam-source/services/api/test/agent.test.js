import test from 'node:test';
import assert from 'node:assert/strict';

import { createAgentHandler } from '../api/agent.js';

const USER_ID = '11111111-1111-1111-1111-111111111111';

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

function jsonResponse(payload, status = 200) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { 'content-type': 'application/json' },
  });
}

function fakeFetch({ user = { id: USER_ID, email: 'user@example.com' }, authStatus = 200, completion } = {}) {
  const calls = [];
  const impl = async (url, init = {}) => {
    const target = String(url);
    calls.push({ url: target, init });
    if (target.endsWith('/auth/v1/user')) return jsonResponse(user, authStatus);
    if (target.endsWith('/chat/completions')) return jsonResponse(completion || {});
    if (target.includes('/rest/v1/agent_runs')) return new Response('', { status: 201 });
    throw new Error(`unexpected url: ${target}`);
  };
  impl.calls = calls;
  return impl;
}

const baseEnv = {
  SUPABASE_URL: 'https://demo.supabase.co',
  SUPABASE_ANON_KEY: 'anon-key',
  AI_API_KEY: 'server-side-key',
  AI_MODEL: 'test-model',
};

function post(handler, { token = 'valid-token', origin, body } = {}) {
  const headers = {};
  if (token !== null) headers.authorization = `Bearer ${token}`;
  if (origin) headers.origin = origin;
  const res = mockRes();
  const promise = handler({ method: 'POST', headers, body: body === undefined ? { message: 'سلام' } : body }, res);
  return { res, promise };
}

test('missing token is rejected before any upstream call', async () => {
  const fetchImpl = fakeFetch();
  const handler = createAgentHandler({ fetchImpl, env: () => baseEnv });
  const { res, promise } = post(handler, { token: null });
  await promise;

  assert.equal(res.statusCode, 401);
  assert.equal(JSON.parse(res.body).error, 'missing_token');
  assert.equal(fetchImpl.calls.length, 0);
});

test('token without configured Supabase returns 503, not a crash', async () => {
  const fetchImpl = fakeFetch();
  const handler = createAgentHandler({ fetchImpl, env: () => ({}) });
  const { res, promise } = post(handler);
  await promise;

  assert.equal(res.statusCode, 503);
  assert.equal(JSON.parse(res.body).error, 'auth_not_configured');
});

test('invalid Supabase token maps to 401 invalid_token', async () => {
  const fetchImpl = fakeFetch({ authStatus: 401, user: { message: 'bad jwt' } });
  const handler = createAgentHandler({ fetchImpl, env: () => baseEnv });
  const { res, promise } = post(handler);
  await promise;

  assert.equal(res.statusCode, 401);
  assert.equal(JSON.parse(res.body).error, 'invalid_token');
});

test('browser origins are rejected unless explicitly allowed', async () => {
  const fetchImpl = fakeFetch();
  const handler = createAgentHandler({ fetchImpl, env: () => baseEnv });
  const { res, promise } = post(handler, { origin: 'https://evil.example' });
  await promise;

  assert.equal(res.statusCode, 403);
  assert.equal(JSON.parse(res.body).error, 'origin_not_allowed');
  assert.equal(fetchImpl.calls.length, 0);
});

test('authenticated request without AI key reports ai_not_configured', async () => {
  const fetchImpl = fakeFetch();
  const handler = createAgentHandler({ fetchImpl, env: () => ({ ...baseEnv, AI_API_KEY: '' }) });
  const { res, promise } = post(handler);
  await promise;

  assert.equal(res.statusCode, 503);
  assert.equal(JSON.parse(res.body).error, 'ai_not_configured');
});

test('happy path returns proposals only, never executions', async () => {
  const fetchImpl = fakeFetch({
    completion: {
      choices: [{
        message: {
          content: 'آماده شد؛ تأیید کنید.',
          tool_calls: [
            { type: 'function', function: { name: 'task.create', arguments: '{"title":"تماس با مادر","dueAt":"2026-11-02"}' } },
            { type: 'function', function: { name: 'task.delete', arguments: '{"id":"task_999"}' } },
            { type: 'function', function: { name: 'task.update', arguments: '{"title":"بی‌شناسه"}' } },
          ],
        },
      }],
    },
  });
  const handler = createAgentHandler({ fetchImpl, env: () => baseEnv });
  const { res, promise } = post(handler, { body: { message: 'فردا با مادر تماس بگیر', locale: 'fa', tasks: [] } });
  await promise;

  assert.equal(res.statusCode, 200);
  const payload = JSON.parse(res.body);
  assert.equal(payload.model, 'test-model');
  assert.equal(payload.reply, 'آماده شد؛ تأیید کنید.');
  assert.equal(payload.actions.length, 2);
  assert.equal(payload.actions[0].tool, 'task.create');
  assert.equal(payload.actions[1].sensitive, true);
  assert.equal(payload.rejected.length, 1);

  const chatCall = fetchImpl.calls.find((call) => call.url.endsWith('/chat/completions'));
  const sent = JSON.parse(chatCall.init.body);
  assert.equal(sent.tool_choice, 'auto');
  assert.equal(sent.stream, undefined);
  assert.equal(sent.messages.length, 2);
  // The AI key is used server-side only and never appears in the response.
  assert.equal(res.body.includes('server-side-key'), false);
});

test('requests over the body cap are rejected with 413', async () => {
  const fetchImpl = fakeFetch({ completion: { choices: [] } });
  const handler = createAgentHandler({ fetchImpl, env: () => baseEnv });
  const { res, promise } = post(handler, { body: { message: 'x'.repeat(4001) } });
  await promise;

  assert.equal(res.statusCode, 413);
  assert.equal(JSON.parse(res.body).error, 'message_too_long');
});

test('audit row stores a digest, not the raw prompt', async () => {
  const fetchImpl = fakeFetch({ completion: { choices: [{ message: { content: 'ok', tool_calls: [] } }] } });
  const handler = createAgentHandler({
    fetchImpl,
    env: () => ({ ...baseEnv, SUPABASE_SERVICE_ROLE_KEY: 'service-role' }),
  });
  const secretPrompt = 'برنامهٔ خصوصی من فردا ساعت ۹';
  const { res, promise } = post(handler, { body: { message: secretPrompt } });
  await promise;

  assert.equal(res.statusCode, 200);
  const auditCall = fetchImpl.calls.find((call) => call.url.includes('/rest/v1/agent_runs'));
  assert.ok(auditCall, 'audit write should happen when a service key is configured');
  const row = JSON.parse(auditCall.init.body);
  assert.equal(row.user_id, USER_ID);
  assert.match(row.prompt_digest, /^[a-f0-9]{64}$/);
  assert.equal(JSON.stringify(row).includes(secretPrompt), false);
  assert.equal(auditCall.init.headers.Authorization.includes('service-role'), true);
});

test('per-user rate limit returns 429 with Retry-After', async () => {
  const fetchImpl = fakeFetch({ completion: { choices: [{ message: { content: 'ok', tool_calls: [] } }] } });
  const handler = createAgentHandler({ fetchImpl, env: () => baseEnv, now: () => 1_000_000 });

  let last;
  for (let index = 0; index < 21; index += 1) {
    last = post(handler);
    await last.promise;
  }

  assert.equal(last.res.statusCode, 429);
  assert.equal(JSON.parse(last.res.body).error, 'rate_limited');
  assert.ok(Number(last.res.headers['retry-after']) > 0);
});

test('upstream provider failure is a safe 502', async () => {
  const fetchImpl = async (url) => {
    const target = String(url);
    if (target.endsWith('/auth/v1/user')) return jsonResponse({ id: USER_ID });
    return new Response('upstream boom', { status: 500 });
  };
  const handler = createAgentHandler({ fetchImpl, env: () => baseEnv });
  const { res, promise } = post(handler);
  await promise;

  assert.equal(res.statusCode, 502);
  assert.equal(JSON.parse(res.body).error, 'ai_error');
  assert.equal(res.body.includes('upstream boom'), false);
});

test('GET on /api/agent is a 405', async () => {
  const handler = createAgentHandler({ fetchImpl: fakeFetch(), env: () => baseEnv });
  const res = mockRes();
  await handler({ method: 'GET', headers: {} }, res);
  assert.equal(res.statusCode, 405);
});

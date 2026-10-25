import test from 'node:test';
import assert from 'node:assert/strict';

import {
  MAX_ACTIONS,
  SENSITIVE_TOOLS,
  buildMessages,
  sanitizeArgs,
  toActions,
  toOpenAiTools,
} from '../lib/agent.js';

test('sanitizeArgs keeps declared fields, clamps text and drops unknown keys', () => {
  const result = sanitizeArgs('task.create', {
    title: `  ${'ب'.repeat(260)}  `,
    priority: 2,
    evil: 'rm -rf',
    dueAt: '2026-11-02',
  });

  assert.equal(result.ok, true);
  assert.equal(result.args.title.length, 200);
  assert.equal(result.args.priority, 2);
  assert.equal(result.args.dueAt, '2026-11-02');
  assert.equal('evil' in result.args, false);
});

test('sanitizeArgs fails when a required field is missing', () => {
  const result = sanitizeArgs('task.create', { dueAt: '2026-11-02' });
  assert.equal(result.ok, false);
  assert.deepEqual(result.missing, ['title']);
});

test('sanitizeArgs drops an invalid optional field but keeps the action', () => {
  const result = sanitizeArgs('task.create', { title: 'خرید', priority: 9, dueAt: 'فردا' });
  assert.equal(result.ok, true);
  assert.deepEqual(result.args, { title: 'خرید' });
  assert.deepEqual(result.dropped.sort(), ['dueAt', 'priority']);
});

test('sanitizeArgs rejects non-ISO dates and unknown tools', () => {
  assert.equal(sanitizeArgs('reminder.schedule', { taskId: 'abc123', at: 'tomorrow' }).ok, false);
  assert.equal(sanitizeArgs('task.create', { title: 'x' }).ok, true);
  assert.equal(sanitizeArgs('rm.everything', { title: 'x' }).ok, false);
});

test('toActions validates tool calls and flags destructive tools', () => {
  const { actions, rejected } = toActions([
    { type: 'function', function: { name: 'task.create', arguments: '{"title":"تماس با مادر"}' } },
    { type: 'function', function: { name: 'task.delete', arguments: '{"id":"task_123"}' } },
    { type: 'function', function: { name: 'task.update', arguments: '{"title":"بدون شناسه"}' } },
    { type: 'function', function: { name: 'shell.exec', arguments: '{}' } },
    { type: 'function', function: { name: 'task.create', arguments: '{not json' } },
  ]);

  assert.equal(actions.length, 2);
  assert.equal(actions[0].tool, 'task.create');
  assert.equal(actions[1].tool, 'task.delete');
  assert.equal(actions[1].sensitive, true);
  assert.equal(SENSITIVE_TOOLS.has('task.delete'), true);
  for (const action of actions) assert.equal(action.requiresConfirmation, true);

  assert.equal(rejected.length, 3);
  assert.deepEqual(rejected.map((entry) => entry.reason).sort(), [
    'invalid_arguments_json',
    'missing_or_invalid_arguments',
    'unknown_tool',
  ]);
});

test('toActions caps how many proposals reach the device', () => {
  const many = Array.from({ length: MAX_ACTIONS + 4 }, () => ({
    type: 'function',
    function: { name: 'task.complete', arguments: '{"id":"task_1"}' },
  }));
  const { actions } = toActions(many);
  assert.equal(actions.length, MAX_ACTIONS);
});

test('toOpenAiTools exposes a strict JSON schema with no extra properties', () => {
  const tools = toOpenAiTools();
  assert.equal(tools.length, 7);
  for (const tool of tools) {
    const schema = tool.function.parameters;
    assert.equal(schema.type, 'object');
    assert.equal(schema.additionalProperties, false);
    for (const field of Object.keys(schema.properties)) {
      assert.equal(Object.prototype.hasOwnProperty.call(schema.properties, field), true);
    }
    for (const required of schema.required) {
      assert.equal(typeof schema.properties[required], 'object');
    }
  }
});

test('buildMessages trims and caps the task context sent to the provider', () => {
  const tasks = Array.from({ length: 80 }, (_, index) => ({
    id: `task_${index}`,
    title: `t${index}`,
    dueAt: '2026-11-02T10:00:00Z',
    status: 'open',
    starred: false,
    secret: 'should not leak',
  }));
  tasks.push({ bad: true });

  const messages = buildMessages({ message: 'فردا یادم بنداز', locale: 'fa', tasks });
  assert.equal(messages.length, 2);
  assert.equal(messages[0].role, 'system');
  assert.match(messages[0].content, /Never claim/);

  const payload = JSON.parse(messages[1].content);
  assert.equal(payload.tasks.length, 50);
  assert.equal(payload.timezone, 'Asia/Tehran');
  assert.equal(JSON.stringify(payload.tasks).includes('should not leak'), false);
});

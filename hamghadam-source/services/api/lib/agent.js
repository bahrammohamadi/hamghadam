// Tool contract for the optional planning agent.
//
// Design rule (see docs/12): the model never executes anything. It only *proposes*
// actions; the Android app renders a preview, the user confirms, the app runs the
// tool locally, and the real result is written to the audit trail. That keeps the
// offline-first core authoritative and makes every AI write reversible/visible.

export const MAX_MESSAGE = 4000;
export const MAX_CONTEXT_TASKS = 50;
export const MAX_ACTIONS = 8;

export const TOOL_SPECS = {
  'task.create': {
    title: { kind: 'text', max: 200, required: true },
    dueAt: { kind: 'iso' },
    priority: { kind: 'int', min: 0, max: 3 },
    project: { kind: 'text', max: 80 },
    notes: { kind: 'text', max: 2000 },
    subtasks: { kind: 'textList', maxItems: 50, max: 120 },
  },
  'task.update': {
    id: { kind: 'id', required: true },
    title: { kind: 'text', max: 200 },
    dueAt: { kind: 'iso' },
    priority: { kind: 'int', min: 0, max: 3 },
    starred: { kind: 'bool' },
    notes: { kind: 'text', max: 2000 },
  },
  'task.complete': { id: { kind: 'id', required: true } },
  'task.delete': { id: { kind: 'id', required: true } },
  'event.create': {
    title: { kind: 'text', max: 200, required: true },
    startAt: { kind: 'iso', required: true },
    endAt: { kind: 'iso' },
    allDay: { kind: 'bool' },
    location: { kind: 'text', max: 200 },
  },
  'reminder.schedule': {
    taskId: { kind: 'id', required: true },
    at: { kind: 'iso', required: true },
    channel: { kind: 'enum', values: ['notification', 'alarm'] },
  },
  'notification.schedule': {
    title: { kind: 'text', max: 120, required: true },
    body: { kind: 'text', max: 500 },
    at: { kind: 'iso', required: true },
  },
};

/** Tools that destroy or overwrite data: the UI must show an explicit warning. */
export const SENSITIVE_TOOLS = new Set([
  'task.delete',
  'task.complete',
  'task.update',
  'event.create',
  'reminder.schedule',
  'notification.schedule',
]);

const ISO_RE = /^\d{4}-\d{2}-\d{2}([T ]\d{2}:\d{2}(:\d{2})?(\.\d{1,9})?(Z|[+-]\d{2}:?\d{2})?)?$/;
const ID_RE = /^[A-Za-z0-9_-]{3,64}$/;

function isValid(kind, value, spec) {
  switch (kind) {
    case 'text':
      return typeof value === 'string' && value.trim().length > 0;
    case 'id':
      return typeof value === 'string' && ID_RE.test(value);
    case 'iso':
      return typeof value === 'string' && value.length <= 40 && ISO_RE.test(value) &&
        !Number.isNaN(Date.parse(value));
    case 'int':
      return Number.isInteger(value) && value >= spec.min && value <= spec.max;
    case 'bool':
      return typeof value === 'boolean';
    case 'enum':
      return typeof value === 'string' && spec.values.includes(value);
    case 'textList':
      return Array.isArray(value) && value.length > 0 && value.every((item) => typeof item === 'string');
    default:
      return false;
  }
}

function coerce(kind, value, spec) {
  if (kind === 'text') return value.trim().slice(0, spec.max);
  if (kind === 'textList') {
    return value
      .slice(0, spec.maxItems)
      .map((item) => item.trim().slice(0, spec.max))
      .filter((item) => item.length > 0);
  }
  return value;
}

/**
 * Keeps only declared fields, clamps lengths and drops invalid optional fields.
 * Anything required that is missing/invalid fails the whole action.
 */
export function sanitizeArgs(tool, input) {
  const spec = TOOL_SPECS[tool];
  if (!spec) return { ok: false, args: {}, missing: [], dropped: [] };
  const source = input && typeof input === 'object' && !Array.isArray(input) ? input : {};
  const args = {};
  const missing = [];
  const dropped = [];

  for (const [field, fieldSpec] of Object.entries(spec)) {
    const present = Object.prototype.hasOwnProperty.call(source, field);
    if (!present || source[field] === null || source[field] === undefined) {
      if (fieldSpec.required) missing.push(field);
      continue;
    }
    if (!isValid(fieldSpec.kind, source[field], fieldSpec)) {
      if (fieldSpec.required) missing.push(field);
      else dropped.push(field);
      continue;
    }
    args[field] = coerce(fieldSpec.kind, source[field], fieldSpec);
  }

  // Unknown keys are silently discarded: they can never reach the device.
  return { ok: missing.length === 0, args, missing, dropped };
}

export function toActions(toolCalls) {
  const actions = [];
  const rejected = [];
  const calls = Array.isArray(toolCalls) ? toolCalls.slice(0, MAX_ACTIONS) : [];

  for (const call of calls) {
    const name = call && call.function ? call.function.name : undefined;
    const rawArgs = call && call.function ? call.function.arguments : undefined;
    let parsed = {};
    try {
      if (typeof rawArgs === 'string' && rawArgs.trim() !== '') parsed = JSON.parse(rawArgs);
      else if (rawArgs && typeof rawArgs === 'object') parsed = rawArgs;
    } catch {
      rejected.push({ tool: String(name || 'unknown').slice(0, 64), reason: 'invalid_arguments_json' });
      continue;
    }
    if (typeof name !== 'string' || !TOOL_SPECS[name]) {
      rejected.push({ tool: String(name || 'unknown').slice(0, 64), reason: 'unknown_tool' });
      continue;
    }
    const result = sanitizeArgs(name, parsed);
    if (!result.ok) {
      rejected.push({
        tool: name,
        reason: 'missing_or_invalid_arguments',
        fields: [...result.missing, ...result.dropped].slice(0, 8),
      });
      continue;
    }
    actions.push({
      tool: name,
      args: result.args,
      droppedFields: result.dropped,
      sensitive: SENSITIVE_TOOLS.has(name),
      requiresConfirmation: true,
    });
  }
  return { actions, rejected };
}

export function toOpenAiTools() {
  const description = {
    'task.create': 'Create one task in the user\'s local task list.',
    'task.update': 'Change fields of an existing task. Never invent an id.',
    'task.complete': 'Mark an existing task as done.',
    'task.delete': 'Delete an existing task. Destructive; the user must confirm.',
    'event.create': 'Add a calendar event for the user.',
    'reminder.schedule': 'Schedule a reminder for an existing task.',
    'notification.schedule': 'Schedule a one-off local notification.',
  };
  const properties = {
    title: { type: 'string' },
    id: { type: 'string' },
    taskId: { type: 'string' },
    dueAt: { type: 'string', description: 'ISO-8601 date or date-time (Asia/Tehran local time allowed).' },
    startAt: { type: 'string' },
    endAt: { type: 'string' },
    at: { type: 'string' },
    allDay: { type: 'boolean' },
    starred: { type: 'boolean' },
    priority: { type: 'integer', minimum: 0, maximum: 3 },
    channel: { type: 'string', enum: ['notification', 'alarm'] },
    project: { type: 'string' },
    notes: { type: 'string' },
    body: { type: 'string' },
    location: { type: 'string' },
    subtasks: { type: 'array', items: { type: 'string' } },
  };
  return Object.entries(TOOL_SPECS).map(([name, spec]) => ({
    type: 'function',
    function: {
      name,
      description: description[name],
      parameters: {
        type: 'object',
        additionalProperties: false,
        required: Object.entries(spec).filter(([, f]) => f.required).map(([field]) => field),
        properties: Object.fromEntries(
          Object.keys(spec).filter((field) => properties[field]).map((field) => [field, properties[field]]),
        ),
      },
    },
  }));
}

export function systemPrompt(locale) {
  const persian = locale !== 'en';
  return [
    'You are the planning agent of Hamghadam, a Persian (Jalali) task and calendar app.',
    'You cannot execute actions. You only propose them through tool calls; the app shows a preview and the user decides.',
    'Rules:',
    '1. Never claim that something was created, changed, deleted or scheduled. Say you prepared it for confirmation.',
    '2. Only reference task ids that appear in the supplied context. Never invent ids.',
    '3. If the request is ambiguous (missing date, missing task), ask one short clarifying question instead of guessing.',
    '4. Prefer one or two precise actions over many speculative ones.',
    '5. Destructive intents (delete, wipe, complete in bulk) must be proposed as a single explicit action and described as irreversible.',
    persian
      ? '6. Answer in Persian, keep it short (max 3 sentences), and keep quoted titles in their original script.'
      : '6. Answer in English, keep it short (max 3 sentences).',
    '7. The user timezone is Asia/Tehran (Jalali calendar); when the user says "فردا" interpret it in that timezone.',
  ].join('\n');
}

export function buildMessages({ message, locale = 'fa', tasks = [] }) {
  const context = (Array.isArray(tasks) ? tasks : [])
    .slice(0, MAX_CONTEXT_TASKS)
    .filter((task) => task && typeof task.id === 'string' && typeof task.title === 'string')
    .map((task) => ({
      id: String(task.id).slice(0, 64),
      title: String(task.title).slice(0, 200),
      dueAt: typeof task.dueAt === 'string' ? task.dueAt.slice(0, 40) : null,
      status: typeof task.status === 'string' ? task.status.slice(0, 24) : 'open',
      starred: task.starred === true,
    }));

  return [
    { role: 'system', content: systemPrompt(locale) },
    {
      role: 'user',
      content: JSON.stringify({ now: new Date().toISOString(), timezone: 'Asia/Tehran', message, tasks: context }),
    },
  ];
}

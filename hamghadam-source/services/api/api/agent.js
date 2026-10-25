import { createHash } from 'node:crypto';
import { HttpError, header, readJson, requireMethod, sendError, sendJson } from '../lib/http.js';
import { requireUser, supabaseConfig } from '../lib/auth.js';
import { MAX_MESSAGE, buildMessages, toActions, toOpenAiTools } from '../lib/agent.js';

const RATE_WINDOW_MS = 10 * 60 * 1000;
const RATE_MAX_REQUESTS = 20;
const UPSTREAM_TIMEOUT_MS = 20_000;
const AI_PATH = '/chat/completions';

/**
 * POST /api/agent
 * Body: { message: string, locale?: 'fa'|'en', tasks?: Array<{id,title,dueAt?,status?,starred?}> }
 * Header: Authorization: Bearer <supabase access token>
 * Returns: { reply, actions[], rejected[], model, latencyMs }
 */
export function createAgentHandler({
  fetchImpl = fetch,
  env = () => process.env,
  now = () => Date.now(),
} = {}) {
  // Best-effort per-instance limiter. Serverless instances do not share memory,
  // so this is abuse friction, not a hard quota (documented in docs/12).
  const buckets = new Map();

  function rateLimit(userId) {
    const current = now();
    const bucket = buckets.get(userId);
    if (!bucket || current - bucket.startedAt >= RATE_WINDOW_MS) {
      buckets.set(userId, { startedAt: current, count: 1 });
      return { allowed: true, retryAfterSeconds: 0 };
    }
    bucket.count += 1;
    if (bucket.count > RATE_MAX_REQUESTS) {
      const retryAfterSeconds = Math.max(1, Math.ceil((bucket.startedAt + RATE_WINDOW_MS - current) / 1000));
      return { allowed: false, retryAfterSeconds };
    }
    return { allowed: true, retryAfterSeconds: 0 };
  }

  async function audit(config, user, entry) {
    const key = config.SUPABASE_SERVICE_ROLE_KEY;
    const { base } = supabaseConfig(config);
    if (!base || !key) return; // auditing is optional; the request must not fail because of it
    const promptDigest = createHash('sha256').update(String(entry.message)).digest('hex');
    await fetchImpl(`${base}/rest/v1/agent_runs`, {
      method: 'POST',
      headers: {
        apikey: key,
        Authorization: `Bearer ${key}`,
        'Content-Type': 'application/json',
        Prefer: 'return=minimal',
      },
      body: JSON.stringify({
        user_id: user.id,
        model: entry.model,
        prompt_digest: promptDigest,
        action_count: entry.actions.length,
        tool_names: entry.actions.map((action) => action.tool),
        rejected_count: entry.rejected.length,
      }),
      signal: AbortSignal.timeout(5000),
    });
  }

  return async function handler(req, res) {
    const startedAt = now();
    try {
      requireMethod(req, ['POST']);
      const config = env();

      const origin = header(req, 'origin');
      if (origin) {
        const allowed = String(config.ALLOWED_ORIGINS || '')
          .split(',')
          .map((value) => value.trim())
          .filter(Boolean);
        if (!allowed.includes(String(origin))) {
          throw new HttpError(403, 'origin_not_allowed', 'This endpoint is for the app, not browsers');
        }
      }

      const user = await requireUser(req, { fetchImpl, env: config });

      const body = await readJson(req);
      const message = typeof body.message === 'string' ? body.message.trim() : '';
      if (!message) throw new HttpError(400, 'missing_message', 'message is required');
      if (message.length > MAX_MESSAGE) {
        throw new HttpError(413, 'message_too_long', `message must be <= ${MAX_MESSAGE} characters`);
      }
      const locale = body.locale === 'en' ? 'en' : 'fa';
      const tasks = Array.isArray(body.tasks) ? body.tasks : [];

      const limit = rateLimit(user.id);
      if (!limit.allowed) {
        return sendJson(
          res,
          429,
          { error: 'rate_limited', retryAfterSeconds: limit.retryAfterSeconds },
          { 'Retry-After': String(limit.retryAfterSeconds) },
        );
      }

      if (!config.AI_API_KEY) {
        throw new HttpError(503, 'ai_not_configured', 'The assistant is not enabled on this deployment');
      }

      const base = String(config.AI_BASE_URL || 'https://api.openai.com/v1').replace(/\/+$/, '');
      const model = String(config.AI_MODEL || 'gpt-4o-mini');

      let upstream;
      try {
        upstream = await fetchImpl(`${base}${AI_PATH}`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${config.AI_API_KEY}`,
          },
          body: JSON.stringify({
            model,
            temperature: 0.2,
            max_tokens: 700,
            messages: buildMessages({ message, locale, tasks }),
            tools: toOpenAiTools(),
            tool_choice: 'auto',
          }),
          signal: AbortSignal.timeout(UPSTREAM_TIMEOUT_MS),
        });
      } catch {
        throw new HttpError(502, 'ai_unreachable', 'The assistant provider did not answer');
      }

      if (!upstream.ok) {
        throw new HttpError(502, 'ai_error', `The assistant provider returned ${upstream.status}`);
      }

      const completion = await upstream.json().catch(() => null);
      const choice = completion && Array.isArray(completion.choices) ? completion.choices[0] : null;
      const reply = choice && choice.message && typeof choice.message.content === 'string'
        ? choice.message.content.trim().slice(0, 2000)
        : '';
      const { actions, rejected } = toActions(choice && choice.message ? choice.message.tool_calls : []);

      await audit(config, user, { model, message, actions, rejected }).catch(() => {});

      return sendJson(res, 200, {
        reply,
        actions,
        rejected,
        model,
        latencyMs: Math.max(0, now() - startedAt),
      });
    } catch (error) {
      return sendError(res, error);
    }
  };
}

export default createAgentHandler();

import { requireMethod, sendError, sendJson } from '../lib/http.js';

/** GET /api/health — public, no secrets: only booleans about what is configured. */
export function createHealthHandler({ env = () => process.env, now = () => new Date() } = {}) {
  return function handler(req, res) {
    try {
      requireMethod(req, ['GET', 'HEAD']);
      const config = env();
      sendJson(res, 200, {
        status: 'ok',
        service: 'hamghadam-api',
        version: '0.2.0',
        time: now().toISOString(),
        capabilities: {
          auth: Boolean(config.SUPABASE_URL && (config.SUPABASE_ANON_KEY || config.SUPABASE_PUBLISHABLE_KEY)),
          agent: Boolean(config.AI_API_KEY),
          audit: Boolean(config.SUPABASE_SERVICE_ROLE_KEY),
        },
      });
    } catch (error) {
      sendError(res, error);
    }
  };
}

export default createHealthHandler();

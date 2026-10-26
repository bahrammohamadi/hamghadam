// Entrypoint for the Vercel "Node" framework preset.
//
// Why this file exists: Vercel's Node preset needs a single server entrypoint
// (index.js / server.js / app.js). The zero-config `api/*.js` functions still
// work for the "Other" preset, but that preset cannot be selected through the
// deployment API without interactive auto-detection confirmation. This router
// makes the same handlers deployable under both presets — no duplicated logic.

import { createAgentHandler } from './api/agent.js';
import { createHealthHandler } from './api/health.js';

const health = createHealthHandler();
const agent = createAgentHandler();

const ROUTES = new Map([
  ['/api/health', health],
  ['/health', health],
  ['/api/agent', agent],
  ['/agent', agent],
]);

function normalize(url) {
  const path = String(url || '/').split('?')[0].split('#')[0];
  if (path === '' || path === '/') return '/';
  return path.replace(/\/+$/, '') || '/';
}

export default async function handler(req, res) {
  const route = ROUTES.get(normalize(req && req.url));
  if (route) {
    // Forward only what the handlers need; the wrapper stays stateless.
    return route({ method: req.method, headers: req.headers, body: req.body, on: req.on, destroy: req.destroy }, res);
  }

  if (normalize(req && req.url) === '/') {
    res.statusCode = 200;
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    res.setHeader('Cache-Control', 'no-store');
    res.end(JSON.stringify({ service: 'hamghadam-api', docs: 'GET /api/health' }));
    return;
  }

  res.statusCode = 404;
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.setHeader('Cache-Control', 'no-store');
  res.end(JSON.stringify({ error: 'not_found', message: 'Unknown route' }));
}

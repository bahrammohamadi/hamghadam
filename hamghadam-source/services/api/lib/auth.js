// Supabase JWT check. The app signs in with Supabase and sends the access token;
// we validate it by asking Supabase who the token belongs to.
//
// Why not verify the JWT locally? Supabase signs with rotating keys; asking
// /auth/v1/user with the project anon key is the smallest, dependency-free way
// to get a trustworthy user id without vendoring a JWT library.

import { HttpError, header } from './http.js';

const AUTH_TIMEOUT_MS = 8000;

export function bearerToken(req) {
  const raw = String(header(req, 'authorization') || '').trim();
  const match = /^Bearer\s+(.+)$/i.exec(raw);
  return match ? match[1].trim() : '';
}

export function supabaseConfig(env) {
  const base = String(env.SUPABASE_URL || '').replace(/\/+$/, '');
  const apikey = env.SUPABASE_ANON_KEY || env.SUPABASE_PUBLISHABLE_KEY || '';
  return { base, apikey };
}

export async function requireUser(req, { fetchImpl = fetch, env = process.env } = {}) {
  const token = bearerToken(req);
  if (!token) {
    throw new HttpError(401, 'missing_token', 'Send Authorization: Bearer <supabase access token>');
  }
  const { base, apikey } = supabaseConfig(env);
  if (!base || !apikey) {
    throw new HttpError(503, 'auth_not_configured', 'Server authentication is not configured');
  }

  let response;
  try {
    response = await fetchImpl(`${base}/auth/v1/user`, {
      method: 'GET',
      headers: { apikey, Authorization: `Bearer ${token}` },
      signal: AbortSignal.timeout(AUTH_TIMEOUT_MS),
    });
  } catch {
    throw new HttpError(503, 'auth_unreachable', 'Authentication service is unreachable');
  }

  if (!response.ok) throw new HttpError(401, 'invalid_token', 'Invalid or expired token');

  const user = await response.json().catch(() => null);
  if (!user || typeof user.id !== 'string' || user.id.length < 8) {
    throw new HttpError(401, 'invalid_token', 'Invalid or expired token');
  }
  return { id: user.id, email: typeof user.email === 'string' ? user.email : null };
}

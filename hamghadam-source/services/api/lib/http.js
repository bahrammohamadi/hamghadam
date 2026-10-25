// HTTP helpers for the optional Hamghadam API (Vercel Node runtime).
// No third-party dependencies: the serverless bundle stays small and auditable.

export const MAX_BODY_BYTES = 64 * 1024;

export class HttpError extends Error {
  constructor(status, code, message) {
    super(message || code);
    this.status = status;
    this.code = code;
  }
}

/** Case-insensitive header reader that works for Node IncomingMessage and Fetch Request. */
export function header(req, name) {
  const headers = req && req.headers;
  if (!headers) return undefined;
  if (typeof headers.get === 'function') return headers.get(name) ?? undefined;
  const key = Object.keys(headers).find((k) => k.toLowerCase() === String(name).toLowerCase());
  return key ? headers[key] : undefined;
}

/** Writes a JSON response using the Node ServerResponse API (what Vercel hands us). */
export function sendJson(res, status, payload, extraHeaders = {}) {
  const body = JSON.stringify(payload);
  res.statusCode = status;
  if (typeof res.setHeader === 'function') {
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    res.setHeader('Cache-Control', 'no-store');
    for (const [key, value] of Object.entries(extraHeaders)) res.setHeader(key, value);
  }
  res.end(body);
}

export function requireMethod(req, methods) {
  const method = String((req && req.method) || '').toUpperCase();
  if (!methods.includes(method)) {
    throw new HttpError(405, 'method_not_allowed', `Allowed methods: ${methods.join(', ')}`);
  }
}

function readStream(req) {
  return new Promise((resolve, reject) => {
    if (typeof req.on !== 'function') return resolve(undefined);
    const chunks = [];
    let size = 0;
    req.on('data', (chunk) => {
      size += chunk.length;
      if (size > MAX_BODY_BYTES) {
        reject(new HttpError(413, 'body_too_large', `Body must be <= ${MAX_BODY_BYTES} bytes`));
        if (typeof req.destroy === 'function') req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
    req.on('error', reject);
  });
}

/** Parses a JSON body, accepting Vercel's pre-parsed body as well as a raw stream. */
export async function readJson(req) {
  let raw = req ? req.body : undefined;
  if (raw === undefined || raw === null) raw = await readStream(req);
  if (Buffer.isBuffer(raw)) raw = raw.toString('utf8');
  if (raw && typeof raw === 'object') {
    const serialized = JSON.stringify(raw);
    if (Buffer.byteLength(serialized, 'utf8') > MAX_BODY_BYTES) {
      throw new HttpError(413, 'body_too_large', `Body must be <= ${MAX_BODY_BYTES} bytes`);
    }
    return raw;
  }
  if (typeof raw !== 'string' || raw.trim() === '') {
    throw new HttpError(400, 'invalid_json', 'Request body must be a JSON object');
  }
  if (Buffer.byteLength(raw, 'utf8') > MAX_BODY_BYTES) {
    throw new HttpError(413, 'body_too_large', `Body must be <= ${MAX_BODY_BYTES} bytes`);
  }
  try {
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      throw new Error('not an object');
    }
    return parsed;
  } catch {
    throw new HttpError(400, 'invalid_json', 'Request body must be a JSON object');
  }
}

export function clientIp(req) {
  const forwarded = header(req, 'x-forwarded-for');
  if (forwarded) return String(forwarded).split(',')[0].trim();
  return String(header(req, 'x-real-ip') || 'unknown');
}

/** Single place where an error becomes a response; never leaks internals or secrets. */
export function sendError(res, error) {
  if (error instanceof HttpError) {
    return sendJson(res, error.status, { error: error.code, message: error.message });
  }
  return sendJson(res, 500, { error: 'internal_error', message: 'Unexpected server error' });
}

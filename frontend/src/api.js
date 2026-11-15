const API_BASE = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '');

export class ApiError extends Error {
  constructor(message, status, payload) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.payload = payload;
  }
}

export async function apiRequest(path, { method = 'GET', body, token, signal } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
  });

  const raw = await response.text();
  let payload = null;
  if (raw) {
    try {
      payload = JSON.parse(raw);
    } catch {
      payload = { message: raw };
    }
  }

  if (!response.ok) {
    const message = payload?.message || payload?.detail || `Request failed (${response.status})`;
    throw new ApiError(message, response.status, payload);
  }
  return payload;
}

export function extractToken(payload) {
  const keys = ['accessToken', 'access_token', 'token', 'jwt'];
  const queue = [payload];
  const seen = new Set();
  while (queue.length) {
    const current = queue.shift();
    if (!current || typeof current !== 'object' || seen.has(current)) continue;
    seen.add(current);
    for (const key of keys) {
      if (typeof current[key] === 'string' && current[key].length > 20) return current[key];
    }
    for (const value of Object.values(current)) {
      if (value && typeof value === 'object') queue.push(value);
    }
  }
  return null;
}

export function decodeJwt(token) {
  try {
    const part = token.split('.')[1];
    const normalized = part.replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(normalized));
  } catch {
    return {};
  }
}

export function readRoles(value) {
  const roles = [];
  const walk = (item, depth = 0) => {
    if (depth > 5 || item == null) return;
    if (typeof item === 'string') {
      if (/^(ROLE_)?(ADMIN|TEACHER|GUARDIAN|STUDENT)$/i.test(item)) roles.push(item.replace(/^ROLE_/i, '').toUpperCase());
      return;
    }
    if (Array.isArray(item)) {
      item.forEach((entry) => walk(entry, depth + 1));
      return;
    }
    if (typeof item === 'object') {
      for (const nested of Object.values(item)) walk(nested, depth + 1);
    }
  };
  walk(value);
  return [...new Set(roles)];
}

export function rowsFrom(payload) {
  if (Array.isArray(payload)) return payload;
  if (!payload || typeof payload !== 'object') return [];
  for (const key of ['content', 'items', 'results', 'records']) {
    if (Array.isArray(payload[key])) return payload[key];
  }
  if (payload.data) return rowsFrom(payload.data);
  return [];
}

export function displayDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
}

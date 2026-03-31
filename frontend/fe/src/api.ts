import type { ApiEnvelope, ApiResult, AuthUser, HttpMethod } from './types';

export const API_BASE_URL = '';
const TOKEN_KEY = 'library_token';
const USER_KEY = 'library_user';

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? '';
}

export function setToken(token: string): void {
  if (!token) {
    localStorage.removeItem(TOKEN_KEY);
    return;
  }

  localStorage.setItem(TOKEN_KEY, token);
}

export function getStoredUser(): AuthUser | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as AuthUser;
  } catch {
    localStorage.removeItem(USER_KEY);
    return null;
  }
}

export function setStoredUser(user: AuthUser | null): void {
  if (!user) {
    localStorage.removeItem(USER_KEY);
    return;
  }

  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearAuth(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export async function request(
  endpoint: string,
  method: HttpMethod,
  body?: unknown,
): Promise<ApiResult<unknown>> {
  const token = getToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  };

  if (token && !endpoint.startsWith('/auth/')) {
    headers.Authorization = `Bearer ${token}`;
  }

  try {
    return await performRequest(endpoint, method, headers, body);
  } catch (error) {
    return {
      ok: false,
      status: 0,
      body: null,
      data: null,
      message: error instanceof Error ? error.message : 'Network request failed.',
    };
  }
}

async function performRequest(
  endpoint: string,
  method: HttpMethod,
  headers: HeadersInit,
  body?: unknown,
): Promise<ApiResult<unknown>> {
  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  const payload = await parseResponseBody(response);
  const envelope = isApiEnvelope(payload) ? payload : null;

  return {
    ok: response.ok,
    status: response.status,
    body: payload,
    data: envelope?.data ?? null,
    message: envelope?.message ?? (typeof payload === 'string' ? payload : ''),
  };
}

async function parseResponseBody(response: Response): Promise<unknown> {
  const raw = await response.text();
  if (!raw) {
    return raw;
  }

  try {
    return JSON.parse(raw);
  } catch {
    return raw;
  }
}

export async function typedRequest<T>(
  endpoint: string,
  method: HttpMethod,
  body?: unknown,
): Promise<ApiResult<T>> {
  return (await request(endpoint, method, body)) as ApiResult<T>;
}

export async function hydrateCurrentUser(): Promise<AuthUser | null> {
  const token = getToken();
  if (!token) {
    setStoredUser(null);
    return null;
  }

  const cachedUser = getStoredUser();
  if (cachedUser) {
    return cachedUser;
  }

  const response = await typedRequest<AuthUser>('/api/users/me', 'GET');
  if (!response.ok || !response.data) {
    clearAuth();
    return null;
  }

  setStoredUser(response.data);
  return response.data;
}

function isApiEnvelope(value: unknown): value is ApiEnvelope<unknown> {
  if (typeof value !== 'object' || value === null) {
    return false;
  }

  return 'code' in value && 'message' in value && 'data' in value;
}

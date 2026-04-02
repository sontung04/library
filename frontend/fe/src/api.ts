import type { ApiEnvelope, ApiResult, AuthUser, HttpMethod, LoginPayload } from './types';

export const API_BASE_URL = '';
const ACCESS_TOKEN_KEY = 'library_access_token';
const REFRESH_TOKEN_KEY = 'library_refresh_token';
const ACCESS_EXPIRY_KEY = 'library_access_expiry';
const USER_KEY = 'library_user';

/** Returns the stored access token (may be expired). */
export function getToken(): string {
  return localStorage.getItem(ACCESS_TOKEN_KEY) ?? '';
}

export function getRefreshToken(): string {
  return localStorage.getItem(REFRESH_TOKEN_KEY) ?? '';
}

export function setTokens(payload: Pick<LoginPayload, 'accessToken' | 'refreshToken' | 'accessExpiresIn'>): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, payload.accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, payload.refreshToken);
  // Store absolute expiry timestamp so we can refresh proactively
  const expiresAt = Date.now() + payload.accessExpiresIn * 1000;
  localStorage.setItem(ACCESS_EXPIRY_KEY, String(expiresAt));
}

/** True if the access token will expire within the next 60 seconds. */
function isAccessTokenExpiringSoon(): boolean {
  const raw = localStorage.getItem(ACCESS_EXPIRY_KEY);
  if (!raw) return true;
  return Date.now() >= Number(raw) - 60_000;
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
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(ACCESS_EXPIRY_KEY);
  localStorage.removeItem(USER_KEY);
}

let refreshInFlight: Promise<boolean> | null = null;

async function tryRefreshTokens(): Promise<boolean> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return false;

  try {
    const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });

    if (!response.ok) {
      clearAuth();
      return false;
    }

    const payload = await response.json() as { data: LoginPayload };
    const data = payload.data;
    setTokens({
      accessToken: data.accessToken,
      refreshToken: data.refreshToken,
      accessExpiresIn: data.accessExpiresIn,
    });
    return true;
  } catch {
    clearAuth();
    return false;
  }
}

export async function callLogout(): Promise<void> {
  const refreshToken = getRefreshToken();
  const accessToken = getToken();
  if (refreshToken) {
    try {
      await fetch(`${API_BASE_URL}/auth/logout`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        },
        body: JSON.stringify({ refreshToken }),
      });
    } catch {
      // best-effort
    }
  }
  clearAuth();
}

export async function request(
  endpoint: string,
  method: HttpMethod,
  body?: unknown,
): Promise<ApiResult<unknown>> {
  const isAuthEndpoint = endpoint.startsWith('/auth/');

  // Proactively refresh before the access token expires
  if (!isAuthEndpoint && getToken() && isAccessTokenExpiringSoon()) {
    refreshInFlight ??= tryRefreshTokens().finally(() => { refreshInFlight = null; });
    await refreshInFlight;
  }

  const headers: HeadersInit = { 'Content-Type': 'application/json' };
  const token = getToken();
  if (token && !isAuthEndpoint) {
    headers.Authorization = `Bearer ${token}`;
  }

  try {
    const result = await performRequest(endpoint, method, headers, body);

    // If 401 and we have a refresh token, try once to refresh and replay
    if (result.status === 401 && !isAuthEndpoint) {
      refreshInFlight ??= tryRefreshTokens().finally(() => { refreshInFlight = null; });
      const refreshed = await refreshInFlight;
      if (refreshed) {
        const retryHeaders: HeadersInit = {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${getToken()}`,
        };
        return performRequest(endpoint, method, retryHeaders, body);
      }
    }

    return result;
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
  const refreshToken = getRefreshToken();

  if (!token && !refreshToken) {
    setStoredUser(null);
    return null;
  }

  // If access token is expiring soon but we have a refresh token, refresh first
  if (refreshToken && isAccessTokenExpiringSoon()) {
    refreshInFlight ??= tryRefreshTokens().finally(() => { refreshInFlight = null; });
    const refreshed = await refreshInFlight;
    if (!refreshed) {
      setStoredUser(null);
      return null;
    }
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

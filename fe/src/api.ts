import type { HttpMethod, ApiResponse } from './types';

export const API_BASE_URL = 'http://localhost:8080';
const TOKEN_KEY = 'library_token';

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

export async function request(
  endpoint: string,
  method: HttpMethod,
  body?: unknown,
): Promise<ApiResponse> {
  const token = getToken();
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  };

  // Send token for all requests except login and register
  if (token && !endpoint.startsWith('/auth/')) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  const raw = await response.text();
  let data: unknown = raw;

  if (raw) {
    try {
      data = JSON.parse(raw);
    } catch {
      data = raw;
    }
  }

  return {
    ok: response.ok,
    status: response.status,
    data,
  };
}

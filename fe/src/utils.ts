import type { ApiResponse } from './types';
import { setToken } from './api';

export function prettyJson(value: unknown): string {
  if (typeof value === 'string') {
    return value;
  }

  return JSON.stringify(value, null, 2);
}

export function unwrapResponseData(value: unknown): Record<string, unknown> | null {
  if (typeof value !== 'object' || value === null) {
    return null;
  }

  const responseBody = value as Record<string, unknown>;
  const nestedData = responseBody.data;

  if (typeof nestedData === 'object' && nestedData !== null) {
    return nestedData as Record<string, unknown>;
  }

  return responseBody;
}

export function setApiOutput(targetId: string, response: ApiResponse): void {
  const target = document.getElementById(targetId);
  if (!target) {
    return;
  }

  target.textContent = `Status: ${response.status}\n\n${prettyJson(response.data)}`;
  target.className = response.ok ? 'api-output success' : 'api-output error';
}

export function setMessage(targetId: string, message: string, isError = false): void {
  const target = document.getElementById(targetId);
  if (!target) {
    return;
  }

  target.textContent = message;
  target.className = isError ? 'inline-message error' : 'inline-message success';
}

export function getInputValue(id: string): string {
  const input = document.getElementById(id) as HTMLInputElement | null;
  return input?.value.trim() ?? '';
}

export function navigate(path: string): void {
  window.history.pushState({}, '', path);
  // Re-render will be called from main render loop
  const event = new CustomEvent('navigate', { detail: { path } });
  window.dispatchEvent(event);
}

export function bindNavigationLinks(): void {
  document.querySelectorAll<HTMLAnchorElement>('a[data-link]').forEach((anchor) => {
    anchor.addEventListener('click', (event) => {
      event.preventDefault();
      const href = anchor.getAttribute('href');
      if (href) {
        navigate(href);
      }
    });
  });

  const clearToken = document.getElementById('clear-token');
  clearToken?.addEventListener('click', () => {
    setToken('');
    alert('Token cleared');
  });
}

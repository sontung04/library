import { callLogout, getStoredUser } from './api';
import type { ApiResult, AuthUser, FlashMessage } from './types';

const FLASH_KEY = 'library_flash';
let isGlobalNavBindingInstalled = false;

export function escapeHtml(value: string): string {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

export function setMessage(targetId: string, message: string, isError = false): void {
  const target = document.getElementById(targetId);
  if (!target) {
    return;
  }

  target.textContent = message;
  target.className = isError ? 'inline-message error' : 'inline-message success';
}

export function setRequestFeedback(targetId: string, response: ApiResult<unknown>, fallback: string): void {
  setMessage(targetId, response.message || fallback, !response.ok);
}

export function getInputValue(id: string): string {
  const input = document.getElementById(id) as HTMLInputElement | null;
  return input?.value.trim() ?? '';
}

export function getNumberValue(id: string): number {
  return Number(getInputValue(id));
}

export function navigate(path: string): void {
  globalThis.history.pushState({}, '', path);
  const event = new CustomEvent('navigate', { detail: { path } });
  globalThis.dispatchEvent(event);
}

export function bindNavigationLinks(): void {
  document.querySelectorAll<HTMLAnchorElement>('a[data-link]').forEach((anchor) => {
    if (anchor.dataset.navBound === 'true') {
      return;
    }

    anchor.addEventListener('click', (event) => {
      event.preventDefault();
      const href = anchor.getAttribute('href');
      if (href) {
        navigate(href);
      }
    });

    anchor.dataset.navBound = 'true';
  });

  const logoutButton = document.getElementById('logout-button');
  if (logoutButton && logoutButton.dataset.navBound !== 'true') {
    logoutButton.addEventListener('click', () => {
      void callLogout().finally(() => {
        pushFlash({ tone: 'success', text: 'You have been logged out.' });
        navigate('/');
      });
    });
    logoutButton.dataset.navBound = 'true';
  }

  const menu = document.getElementById('user-menu');
  const toggle = document.getElementById('user-menu-toggle');

  if (toggle && toggle.dataset.navBound !== 'true') {
    toggle.addEventListener('click', (event) => {
      event.stopPropagation();
      menu?.classList.toggle('open');
    });
    toggle.dataset.navBound = 'true';
  }

  if (menu && menu.dataset.navBound !== 'true') {
    menu.addEventListener('click', (event) => {
      event.stopPropagation();
    });
    menu.dataset.navBound = 'true';
  }

  if (!isGlobalNavBindingInstalled) {
    document.addEventListener('click', (event) => {
      const target = event.target;
      if (!(target instanceof Node)) {
        return;
      }

      const activeMenu = document.getElementById('user-menu');
      const activeToggle = document.getElementById('user-menu-toggle');
      if (activeMenu?.contains(target) || activeToggle?.contains(target)) {
        return;
      }

      activeMenu?.classList.remove('open');
    });
    isGlobalNavBindingInstalled = true;
  }
}

export function getCheckedValues(name: string): string[] {
  return Array.from(document.querySelectorAll<HTMLInputElement>(`input[name="${name}"]:checked`)).map(
    (input) => input.value,
  );
}

export function pushFlash(message: FlashMessage): void {
  sessionStorage.setItem(FLASH_KEY, JSON.stringify(message));
}

export function consumeFlash(): FlashMessage | null {
  const raw = sessionStorage.getItem(FLASH_KEY);
  if (!raw) {
    return null;
  }

  sessionStorage.removeItem(FLASH_KEY);
  try {
    return JSON.parse(raw) as FlashMessage;
  } catch {
    return null;
  }
}

export function hasRole(user: AuthUser | null, role: string): boolean {
  return Boolean(user?.roles.includes(role));
}

export function getInitials(user: AuthUser | null): string {
  if (!user?.username) {
    return 'U';
  }

  return user.username.slice(0, 1).toUpperCase();
}

export function formatDate(value: string | null): string {
  if (!value) {
    return 'Not returned';
  }

  return new Date(value).toLocaleDateString();
}

export function getCurrentUserLabel(): string {
  const user = getStoredUser();
  if (!user) {
    return 'Guest';
  }

  if (!user.email) {
    return user.username;
  }

  return `${user.username} • ${user.email}`;
}

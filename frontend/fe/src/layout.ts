import { getStoredUser } from './api';
import { consumeFlash, escapeHtml, getCurrentUserLabel, getInitials } from './utils';

type NavLink = {
  href: string;
  label: string;
};

export function layout(title: string, content: string, currentPath: string): string {
  const user = getStoredUser();
  const flash = consumeFlash();
  const links = getPrimaryLinks();
  const userRoles = user?.roles.length ? user.roles.join(' • ').replaceAll('ROLE_', '') : 'Guest access';
  const adminMenuLink = user?.roles.includes('ROLE_ADMIN') ? '<a href="/admin/users" data-link>Users</a>' : '';
  const loanHistoryMenuLink = user?.roles.includes('ROLE_USER') || user?.roles.includes('ROLE_LIBRARIAN') ? '<a href="/loans/history" data-link>Loan history</a>' : '';
  const authControls = user
    ? `
        <div class="topbar-actions topbar-actions-authenticated">
          <div class="user-summary-card">
            <span class="user-summary-eyebrow">Signed in</span>
            <strong>${escapeHtml(user.username)}</strong>
            <span class="hint">${escapeHtml(userRoles)}</span>
          </div>
          <div class="user-menu-shell">
            <button id="user-menu-toggle" class="user-menu-toggle" type="button" aria-label="User menu">${escapeHtml(getInitials(user))}</button>
            <div id="user-menu" class="user-menu">
              <p class="user-menu-label">${escapeHtml(getCurrentUserLabel())}</p>
              <a href="/users/me" data-link>User info</a>
              ${adminMenuLink}
              ${loanHistoryMenuLink}
              <button id="logout-button" type="button" class="menu-danger">Log out</button>
            </div>
          </div>
        </div>
      `
    : `
        <div class="topbar-actions">
          <a href="/auth/login" data-link class="ghost-button">Log in</a>
          <a href="/auth/register" data-link class="solid-button">Register</a>
        </div>
      `;

  return `
    <div class="shell">
      <header class="topbar">
        <div class="brand-block">
          <a href="/" data-link class="brand-mark">Library</a>
          <div>
            <h1>${escapeHtml(title)}</h1>
          </div>
        </div>
        ${authControls}
      </header>
      <section class="shell-status-row">
        <div class="status-copy">
          <span class="status-dot"></span>
          <span>${user ? `Session ready for ${escapeHtml(user.username)}` : 'Browsing as a guest'}</span>
        </div>
        <div class="role-badge-row">
          ${(user?.roles.length ? user.roles : ['ROLE_GUEST'])
            .map((role) => `<span class="role-badge">${escapeHtml(role.replace('ROLE_', ''))}</span>`)
            .join('')}
        </div>
      </section>
      ${flash ? `<div class="flash-banner ${flash.tone}">${escapeHtml(flash.text)}</div>` : ''}
      ${links.length > 0 ? renderPrimaryNav(links, currentPath) : ''}
      <main class="content">${content}</main>
    </div>
  `;
}

function getPrimaryLinks(): NavLink[] {
  const user = getStoredUser();
  const isAdmin = user?.roles.includes('ROLE_ADMIN');
  const canBorrow = user?.roles.includes('ROLE_USER');
  const isLibrarian = user?.roles.includes('ROLE_LIBRARIAN');

  if (isAdmin) {
    return [{ href: '/admin/users', label: 'Users' }];
  }

  return [
    { href: '/', label: 'Catalog' },
    ...(canBorrow
      ? [
          { href: '/loans/borrow', label: 'Borrow' },
          { href: '/loans/history', label: 'Loans' },
        ]
      : []),
    ...(isLibrarian ? [{ href: '/books/new', label: 'Create Book' }] : []),
  ];
}

function renderPrimaryNav(links: NavLink[], currentPath: string): string {
  return `
    <nav class="shell-nav" aria-label="Primary">
      ${links
        .map((link) => {
          const isActive = currentPath === link.href || (link.href !== '/' && currentPath.startsWith(link.href));
          return `
            <a href="${link.href}" data-link class="nav-link ${isActive ? 'active' : ''}">
              <span class="nav-link-label">${escapeHtml(link.label)}</span>
            </a>
          `;
        })
        .join('')}
    </nav>
  `;
}

import { getStoredUser, typedRequest } from '../api';
import type { AuthUser, Page } from '../types';
import { bindNavigationLinks, escapeHtml, getCheckedValues, getInputValue, hasRole, navigate, pushFlash, setMessage } from '../utils';

export function myProfilePage(): Page {
  return {
    title: 'My profile',
    html: `
      <section class="panel">
        <h2>Account summary</h2>
        <p class="hint">This page shows the authenticated user information returned by the gateway and user service.</p>
        <div id="profile-fields" class="details-grid"></div>
        <p id="profile-message" class="inline-message"></p>
      </section>
    `,
    setup: async () => {
      const fields = document.getElementById('profile-fields');

      if (!fields) {
        return;
      }

      const response = await typedRequest<AuthUser>('/api/users/me', 'GET');
      if (!response.ok || !response.data) {
        setMessage('profile-message', response.message || 'Failed to load profile.', true);
        return;
      }

      const user = response.data;
      fields.innerHTML = `
        <article class="detail-item"><span class="detail-label">User ID</span><strong>${user.id}</strong></article>
        <article class="detail-item"><span class="detail-label">Username</span><strong>${escapeHtml(user.username)}</strong></article>
        <article class="detail-item"><span class="detail-label">Email</span><strong>${escapeHtml(user.email || 'Not provided')}</strong></article>
        <article class="detail-item"><span class="detail-label">Roles</span><strong>${escapeHtml(user.roles.join(', '))}</strong></article>
      `;
    },
  };
}

export function adminUsersPage(): Page {
  return {
    title: 'Users',
    html: `
      <section class="panel">
        <div class="row-between">
          <div>
            <h2>User management</h2>
            <p class="hint">Create, update, and remove accounts from the admin workspace.</p>
          </div>
          <a href="/admin/users/new" data-link class="solid-button">Create</a>
        </div>
        <div id="admin-users-list" class="list"></div>
        <p id="admin-users-message" class="inline-message"></p>
      </section>
    `,
    setup: async () => {
      if (!ensureAdmin('admin-users-message')) {
        return;
      }

      const response = await typedRequest<AuthUser[]>('/api/admin/users', 'GET');
      const list = document.getElementById('admin-users-list');
      if (!list) {
        return;
      }

      if (!response.ok || !response.data) {
        setMessage('admin-users-message', response.message || 'Failed to load users.', true);
        return;
      }

      if (response.data.length === 0) {
        list.innerHTML = '<article class="empty-state"><h3>No users found</h3><p>Create the first account from the action above.</p></article>';
        bindNavigationLinks();
        return;
      }

      list.innerHTML = response.data
        .map(
          (user) => `
            <article class="card user-card">
              <div class="user-card-info">
                <strong>${escapeHtml(user.username)}</strong>
                <span class="hint">${escapeHtml(user.email)}</span>
                <span class="hint">${escapeHtml(user.roles.join(', '))}</span>
              </div>
              <div class="user-card-actions">
                <a href="/admin/users/${user.id}/edit" data-link class="ghost-link">Modify</a>
                <a href="/admin/users/${user.id}/delete" data-link class="ghost-link">Delete</a>
              </div>
            </article>
          `,
        )
        .join('');
      bindNavigationLinks();
    },
  };
}

export function adminUserCreatePage(): Page {
  return {
    title: 'Create user',
    html: renderAdminUserForm('Create user', 'Create account', {
      includePasswordField: true,
      includePasswordHelp: true,
    }),
    setup: () => {
      if (!ensureAdmin('admin-user-form-message')) {
        return;
      }

      bindAdminUserForm('/api/admin/users/new', 'POST', 'User created successfully.');
    },
  };
}

export function adminUserUpdatePage(userId: string): Page {
  return {
    title: 'Modify user',
    html: renderAdminUserForm('Modify user', 'Save changes', {
      includePasswordField: false,
      includePasswordHelp: false,
    }),
    setup: async () => {
      if (!ensureAdmin('admin-user-form-message')) {
        return;
      }

      const response = await typedRequest<AuthUser>(`/api/admin/users/${userId}`, 'GET');
      if (!response.ok || !response.data) {
        setMessage('admin-user-form-message', response.message || 'Failed to load user.', true);
        return;
      }

      fillAdminUserForm(response.data);
      bindAdminUserForm(`/api/admin/users/${userId}`, 'PUT', 'User updated successfully.');
    },
  };
}

export function adminUserDeletePage(userId: string): Page {
  return {
    title: 'Delete user',
    html: `
      <section class="panel">
        <h2>Delete user</h2>
        <div id="admin-user-delete-summary" class="panel-subtle"></div>
        <div class="form-actions">
          <button id="confirm-delete-user" type="button" class="danger">Delete user</button>
          <a href="/admin/users" data-link class="ghost-link">Cancel</a>
        </div>
        <p id="admin-user-delete-message" class="inline-message"></p>
      </section>
    `,
    setup: async () => {
      if (!ensureAdmin('admin-user-delete-message')) {
        return;
      }

      const response = await typedRequest<AuthUser>(`/api/admin/users/${userId}`, 'GET');
      const summary = document.getElementById('admin-user-delete-summary');
      const button = document.getElementById('confirm-delete-user');
      if (!summary || !button) {
        return;
      }

      if (!response.ok || !response.data) {
        setMessage('admin-user-delete-message', response.message || 'Failed to load user.', true);
        return;
      }

      const user = response.data;
      summary.innerHTML = `
        <h3>${escapeHtml(user.username)}</h3>
        <p class="hint">${escapeHtml(user.email)}</p>
        <p class="hint">Roles: ${escapeHtml(user.roles.join(', '))}</p>
      `;

      button.addEventListener('click', async () => {
        const deleteResponse = await typedRequest<null>(`/api/admin/users/${userId}`, 'DELETE');
        if (!deleteResponse.ok && deleteResponse.status !== 204) {
          setMessage('admin-user-delete-message', deleteResponse.message || 'Failed to delete user.', true);
          return;
        }

        pushFlash({ tone: 'success', text: `User ${user.username} deleted.` });
        navigate('/admin/users');
      });

      bindNavigationLinks();
    },
  };
}

function renderAdminUserForm(
  title: string,
  submitLabel: string,
  options: { includePasswordField: boolean; includePasswordHelp: boolean },
): string {
  return `
    <section class="panel auth-panel admin-user-form-panel">
      <h2>${title}</h2>
      <p class="hint">Admins can assign roles directly when creating or updating an account.</p>
      <form id="admin-user-form" class="form-grid">
        <label>Username <input id="admin-user-username" type="text" required /></label>
        <label>Email <input id="admin-user-email" type="email" required /></label>
        ${options.includePasswordField ? '<label>Password <input id="admin-user-password" type="password" required /></label>' : ''}
        <fieldset class="roles-fieldset">
          <legend>Roles</legend>
          <div class="roles-checkboxes">
            ${renderRoleCheckbox('ROLE_USER', 'Reader')}
            ${renderRoleCheckbox('ROLE_LIBRARIAN', 'Librarian')}
            ${renderRoleCheckbox('ROLE_ADMIN', 'Admin')}
          </div>
        </fieldset>
        ${options.includePasswordHelp ? '<p class="hint">Use a temporary password if the account owner will change it later.</p>' : ''}
        <div class="form-actions">
          <button type="submit">${submitLabel}</button>
          <a href="/admin/users" data-link class="ghost-link">Back</a>
        </div>
      </form>
      <p id="admin-user-form-message" class="inline-message"></p>
    </section>
  `;
}

function bindAdminUserForm(endpoint: string, method: 'POST' | 'PUT', successMessage: string): void {
  const form = document.getElementById('admin-user-form') as HTMLFormElement | null;
  if (!form || form.dataset.bound === 'true') {
    bindNavigationLinks();
    return;
  }

  form.addEventListener('submit', async (event) => {
    event.preventDefault();

    const roles = getCheckedValues('admin-user-role');
    if (roles.length === 0) {
      setMessage('admin-user-form-message', 'Select at least one role.', true);
      return;
    }

    const response = await typedRequest<AuthUser>(endpoint, method, {
      username: getInputValue('admin-user-username'),
      email: getInputValue('admin-user-email'),
      password: getOptionalInputValue('admin-user-password'),
      roles,
    });

    if (!response.ok) {
      setMessage('admin-user-form-message', response.message || 'Failed to save user.', true);
      return;
    }

    pushFlash({ tone: 'success', text: successMessage });
    navigate('/admin/users');
  });

  form.dataset.bound = 'true';
  bindNavigationLinks();
}

function fillAdminUserForm(user: AuthUser): void {
  const usernameInput = document.getElementById('admin-user-username') as HTMLInputElement | null;
  const emailInput = document.getElementById('admin-user-email') as HTMLInputElement | null;
  const passwordInput = document.getElementById('admin-user-password') as HTMLInputElement | null;
  if (!usernameInput || !emailInput) {
    return;
  }

  usernameInput.value = user.username;
  emailInput.value = user.email;
  if (passwordInput) {
    passwordInput.value = '';
  }
  document.querySelectorAll<HTMLInputElement>('input[name="admin-user-role"]').forEach((input) => {
    input.checked = user.roles.includes(input.value);
  });
}

function getOptionalInputValue(id: string): string | null {
  const input = document.getElementById(id) as HTMLInputElement | null;
  if (!input) {
    return null;
  }

  const value = input.value.trim();
  return value || null;
}

function renderRoleCheckbox(value: string, label: string): string {
  return `<label class="checkbox-label"><input type="checkbox" name="admin-user-role" value="${value}" /> ${label}</label>`;
}

function ensureAdmin(messageTargetId: string): boolean {
  const user = getStoredUser();
  if (hasRole(user, 'ROLE_ADMIN')) {
    return true;
  }

  setMessage(messageTargetId, 'Only admins can access this page.', true);
  return false;
}

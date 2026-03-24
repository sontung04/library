import type { Page } from '../types';
import { request } from '../api';
import { getInputValue, setApiOutput, setMessage, navigate, unwrapResponseData } from '../utils';

const ALL_ROLES = ['ROLE_USER', 'ROLE_LIBRARIAN', 'ROLE_ADMIN'] as const;

export function createUserPage(): Page {
  return {
    title: 'Create User',
    html: `
      <section class="panel">
        <h2>POST /api/admin/users/new</h2>
        <form id="create-user-form" class="form-grid">
          <label>Username <input id="create-username" type="text" required /></label>
          <label>Email <input id="create-email" type="email" required /></label>
          <label>Password <input id="create-password" type="password" required /></label>
          <fieldset class="roles-fieldset">
            <legend>Roles</legend>
            <div class="roles-checkboxes">
              ${ALL_ROLES.map((role) => `
                <label class="checkbox-label">
                  <input type="checkbox" name="role" value="${role}" />
                  ${role}
                </label>
              `).join('')}
            </div>
          </fieldset>
          <button type="submit">Create User</button>
        </form>
        <p id="create-user-message" class="inline-message"></p>
        <pre id="create-user-output" class="api-output"></pre>
      </section>
    `,
    setup: () => {
      const form = document.getElementById('create-user-form') as HTMLFormElement | null;
      if (!form) {
        return;
      }

      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const username = getInputValue('create-username');
        const email = getInputValue('create-email');
        const password = getInputValue('create-password');
        const roles = Array.from(
          form.querySelectorAll<HTMLInputElement>('input[name="role"]:checked'),
        ).map((cb) => cb.value);

        const response = await request('/api/admin/users/new', 'POST', { username, email, password, roles });
        setApiOutput('create-user-output', response);
        setMessage(
          'create-user-message',
          response.ok ? 'User created successfully.' : 'Failed to create user.',
          !response.ok,
        );
      });
    },
  };
}

export function usersListPage(): Page {
  return {
    title: 'Users',
    html: `
      <section class="panel">
        <div class="row-between">
          <h2>GET /api/admin/users</h2>
          <div>
            <button id="create-user-btn" type="button">Create User</button>
            <button id="reload-users" type="button">Reload</button>
          </div>
        </div>
        <div id="users-message" class="inline-message"></div>
        <div id="users-list" class="list"></div>
        <pre id="users-output" class="api-output"></pre>
      </section>
    `,
    setup: () => {
      const list = document.getElementById('users-list');

      const deleteUser = async (userId: string) => {
        if (!confirm(`Delete user ${userId}?`)) {
          return;
        }
        const response = await request(`/api/admin/users/${userId}`, 'DELETE');
        setApiOutput('users-output', response);
        if (response.ok) {
          setMessage('users-message', `User ${userId} deleted.`);
          void loadUsers();
        } else {
          setMessage('users-message', `Failed to delete user ${userId}.`, true);
        }
      };

      const renderUsers = (users: unknown[]) => {
        if (!list) {
          return;
        }
        if (users.length === 0) {
          list.innerHTML = '<p class="hint">No users found.</p>';
          return;
        }
        list.innerHTML = users
          .map((item) => {
            if (typeof item !== 'object' || item === null) {
              return '';
            }
            const user = item as Record<string, unknown>;
            const id = String(user.id ?? '');
            const username = String(user.username ?? 'N/A');
            const email = String(user.email ?? 'N/A');
            const rolesValue = user.roles;
            const roles = Array.isArray(rolesValue)
              ? rolesValue.map(String).join(', ')
              : 'N/A';
            return `
              <article class="card user-card">
                <div class="user-card-info">
                  <strong>${username}</strong>
                  <span class="hint">${email}</span>
                  <span class="hint">ID: ${id}</span>
                  <span class="hint">Roles: ${roles}</span>
                </div>
                <div class="user-card-actions">
                  <a href="/users/${id}/edit" data-link class="btn-link">Modify</a>
                  <button class="danger" data-delete-id="${id}" type="button">Delete</button>
                </div>
              </article>
            `;
          })
          .join('');

        list.querySelectorAll<HTMLButtonElement>('button[data-delete-id]').forEach((btn) => {
          btn.addEventListener('click', () => {
            const userId = btn.getAttribute('data-delete-id') ?? '';
            void deleteUser(userId);
          });
        });

        list.querySelectorAll<HTMLAnchorElement>('a[data-link]').forEach((anchor) => {
          anchor.addEventListener('click', (event) => {
            event.preventDefault();
            const href = anchor.getAttribute('href');
            if (href) {
              navigate(href);
            }
          });
        });
      };

      const loadUsers = async () => {
        const response = await request('/api/admin/users', 'GET');
        setApiOutput('users-output', response);
        // Response shape: { code, message, data: [{id, username, email, roles}] }
        const body = unwrapResponseData(response.data);
        const items: unknown[] = Array.isArray(body) ? (body as unknown as unknown[]) : [];
        renderUsers(items);
      };

      document.getElementById('reload-users')?.addEventListener('click', loadUsers);
      document.getElementById('create-user-btn')?.addEventListener('click', () => navigate('/users/create'));
      void loadUsers();
    },
  };
}

export function editUserPage(userId: string): Page {
  return {
    title: `Edit User #${userId}`,
    html: `
      <section class="panel">
        <h2>PUT /api/admin/users/${userId}</h2>
        <form id="edit-user-form" class="form-grid">
          <label>Username <input id="edit-username" type="text" required /></label>
          <label>Email <input id="edit-email" type="email" required /></label>
          <fieldset class="roles-fieldset">
            <legend>Roles</legend>
            <div class="roles-checkboxes">
              ${ALL_ROLES.map((role) => `
                <label class="checkbox-label">
                  <input type="checkbox" name="role" value="${role}" />
                  ${role}
                </label>
              `).join('')}
            </div>
          </fieldset>
          <button type="submit">Save Changes</button>
        </form>
        <p id="edit-user-message" class="inline-message"></p>
        <pre id="edit-user-output" class="api-output"></pre>
      </section>
    `,
    setup: () => {
      const form = document.getElementById('edit-user-form') as HTMLFormElement | null;
      if (!form) {
        return;
      }

      const prefill = async () => {
        const response = await request(`/api/admin/users/${userId}`, 'GET');
        setApiOutput('edit-user-output', response);
        if (!response.ok) {
          setMessage('edit-user-message', 'Failed to load user data.', true);
          return;
        }
        const user = unwrapResponseData(response.data);
        if (!user) {
          return;
        }

        const usernameInput = document.getElementById('edit-username') as HTMLInputElement | null;
        const emailInput = document.getElementById('edit-email') as HTMLInputElement | null;
        if (usernameInput) {
          usernameInput.value = String(user.username ?? '');
        }
        if (emailInput) {
          emailInput.value = String(user.email ?? '');
        }

        const rolesValue = user.roles;
        const currentRoles = Array.isArray(rolesValue) ? rolesValue.map(String) : [];
        form.querySelectorAll<HTMLInputElement>('input[name="role"]').forEach((checkbox) => {
          checkbox.checked = currentRoles.includes(checkbox.value);
        });
      };

      void prefill();

      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const username = getInputValue('edit-username');
        const email = getInputValue('edit-email');
        const roles = Array.from(
          form.querySelectorAll<HTMLInputElement>('input[name="role"]:checked'),
        ).map((cb) => cb.value);

        const response = await request(`/api/admin/users/${userId}`, 'PUT', { username, email, roles });
        setApiOutput('edit-user-output', response);
        setMessage(
          'edit-user-message',
          response.ok ? 'User updated successfully.' : 'Failed to update user.',
          !response.ok,
        );
      });
    },
  };
}

export function myProfilePage(): Page {
  return {
    title: 'My Profile',
    html: `
      <section class="panel">
        <div class="row-between">
          <h2>GET /api/users/me</h2>
          <button id="reload-profile" type="button">Reload</button>
        </div>
        <div id="profile-fields" class="details-grid"></div>
        <pre id="profile-output" class="api-output"></pre>
      </section>
    `,
    setup: () => {
      const fields = document.getElementById('profile-fields');

      const renderFields = (user: Record<string, unknown> | null) => {
        if (!fields) {
          return;
        }

        if (!user) {
          fields.innerHTML = '<p class="hint">No user details available from this response format.</p>';
          return;
        }

        const rolesValue = user.roles;
        const roles = Array.isArray(rolesValue) ? rolesValue.map((role) => {
          const roleText = String(role);
          return roleText;
        }) : [];

        fields.innerHTML = `
          <article class="detail-item">
            <span class="detail-label">ID</span>
            <strong>${String(user.id ?? '') || 'N/A'}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-label">Username</span>
            <strong>${String(user.username ?? '') || 'N/A'}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-label">Email</span>
            <strong>${String(user.email ?? '') || 'N/A'}</strong>
          </article>
          <article class="detail-item">
            <span class="detail-label">Roles</span>
            <strong>${roles.length > 0 ? roles.join(', ') : 'N/A'}</strong>
          </article>
        `;
      };

      const loadProfile = async () => {
        const response = await request('/api/users/me', 'GET');
        setApiOutput('profile-output', response);
        renderFields(response.ok ? unwrapResponseData(response.data) : null);
      };

      const reloadButton = document.getElementById('reload-profile');
      reloadButton?.addEventListener('click', loadProfile);
      void loadProfile();
    },
  };
}

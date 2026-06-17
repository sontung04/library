import { setStoredUser, setTokens, typedRequest } from '../api';
import type { AuthUser, LoginPayload, Page } from '../types';
import { getInputValue, navigate, pushFlash, setMessage } from '../utils';

export function loginPage(): Page {
  return {
    title: 'Login',
    html: `
      <section class="panel auth-panel">
        <h2>Log in</h2>
        <p class="hint">Use your library account to borrow books or manage circulation.</p>
        <form id="login-form" class="form-grid">
          <label>Username <input id="login-username" type="text" required /></label>
          <label>Password <input id="login-password" type="password" required /></label>
          <button type="submit">Login</button>
        </form>
        <p id="login-message" class="inline-message"></p>
      </section>
    `,
    setup: () => {
      const form = document.getElementById('login-form') as HTMLFormElement | null;
      if (!form) {
        return;
      }

      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const username = getInputValue('login-username');
        const password = getInputValue('login-password');

        const response = await typedRequest<LoginPayload>('/auth/login', 'POST', { username, password });

        if (response.ok && response.data) {
          setTokens({
            accessToken: response.data.accessToken,
            refreshToken: response.data.refreshToken,
            accessExpiresIn: response.data.accessExpiresIn,
          });
          const profileResponse = await typedRequest<AuthUser>('/api/users/me', 'GET');

          if (profileResponse.ok && profileResponse.data) {
            setStoredUser(profileResponse.data);
          } else {
            setStoredUser({
              id: response.data.userId,
              username: response.data.username,
              email: '',
              roles: response.data.roles,
            });
          }

          pushFlash({ tone: 'success', text: 'Login successful.' });
          navigate('/');
          return;
        }

        setMessage('login-message', response.message || 'Login failed.', true);
      });
    },
  };
}

export function registerPage(): Page {
  return {
    title: 'Register',
    html: `
      <section class="panel auth-panel">
        <h2>Create an account</h2>
        <p class="hint">New accounts are created as readers by default.</p>
        <form id="register-form" class="form-grid">
          <label>Username <input id="register-username" type="text" required /></label>
          <label>Email <input id="register-email" type="email" required /></label>
          <label>Password <input id="register-password" type="password" required /></label>
          <button type="submit">Register</button>
        </form>
        <p id="register-message" class="inline-message"></p>
      </section>
    `,
    setup: () => {
      const form = document.getElementById('register-form') as HTMLFormElement | null;
      if (!form) {
        return;
      }

      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const username = getInputValue('register-username');
        const email = getInputValue('register-email');
        const password = getInputValue('register-password');

        const response = await typedRequest<null>('/auth/register', 'POST', { username, email, password });
        if (response.ok) {
          pushFlash({ tone: 'success', text: 'Registration successful. You can log in now.' });
          navigate('/auth/login');
          return;
        }

        setMessage('register-message', response.message || 'Register failed.', true);
      });
    },
  };
}

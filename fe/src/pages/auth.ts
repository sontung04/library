import type { Page } from '../types';
import { request, setToken } from '../api';
import { getInputValue, setApiOutput, setMessage, unwrapResponseData } from '../utils';

export function loginPage(): Page {
  return {
    title: 'Login',
    html: `
      <section class="panel">
        <h2>POST /auth/login</h2>
        <form id="login-form" class="form-grid">
          <label>Username <input id="login-username" type="text" required /></label>
          <label>Password <input id="login-password" type="password" required /></label>
          <button type="submit">Login</button>
        </form>
        <p id="login-message" class="inline-message"></p>
        <pre id="login-output" class="api-output"></pre>
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

        const response = await request('/auth/login', 'POST', { username, password });
        setApiOutput('login-output', response);

        if (response.ok) {
          const responseData = unwrapResponseData(response.data);
          const token = responseData?.token;

          if (typeof token === 'string') {
            setToken(token);
            setMessage('login-message', 'Login successful. Token saved in localStorage.');
          } else {
            setMessage('login-message', 'Login successful. No data.token field found in response.');
          }
          return;
        }

        setMessage('login-message', 'Login failed.', true);
      });
    },
  };
}

export function registerPage(): Page {
  return {
    title: 'Register',
    html: `
      <section class="panel">
        <h2>POST /auth/register</h2>
        <form id="register-form" class="form-grid">
          <label>Username <input id="register-username" type="text" required /></label>
          <label>Email <input id="register-email" type="email" required /></label>
          <label>Password <input id="register-password" type="password" required /></label>
          <button type="submit">Register</button>
        </form>
        <p id="register-message" class="inline-message"></p>
        <pre id="register-output" class="api-output"></pre>
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

        const response = await request('/auth/register', 'POST', { username, email, password });
        setApiOutput('register-output', response);
        setMessage(
          'register-message',
          response.ok ? 'Register successful.' : 'Register failed.',
          !response.ok,
        );
      });
    },
  };
}

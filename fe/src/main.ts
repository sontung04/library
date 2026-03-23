import './style.css';

type HttpMethod = 'GET' | 'POST';

type ApiResponse = {
  ok: boolean;
  status: number;
  data: unknown;
};

type Page = {
  title: string;
  html: string;
  setup?: () => void;
};

const API_BASE_URL = 'http://localhost:8080';
const TOKEN_KEY = 'library_token';

const app = document.querySelector<HTMLDivElement>('#app');

if (!app) {
  throw new Error('App root was not found');
}

const appRoot: HTMLDivElement = app;

function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? '';
}

function setToken(token: string): void {
  if (!token) {
    localStorage.removeItem(TOKEN_KEY);
    return;
  }

  localStorage.setItem(TOKEN_KEY, token);
}

async function request(endpoint: string, method: HttpMethod, body?: unknown): Promise<ApiResponse> {
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

function prettyJson(value: unknown): string {
  if (typeof value === 'string') {
    return value;
  }

  return JSON.stringify(value, null, 2);
}

function setApiOutput(targetId: string, response: ApiResponse): void {
  const target = document.getElementById(targetId);
  if (!target) {
    return;
  }

  target.textContent = `Status: ${response.status}\n\n${prettyJson(response.data)}`;
  target.className = response.ok ? 'api-output success' : 'api-output error';
}

function setMessage(targetId: string, message: string, isError = false): void {
  const target = document.getElementById(targetId);
  if (!target) {
    return;
  }

  target.textContent = message;
  target.className = isError ? 'inline-message error' : 'inline-message success';
}

function getInputValue(id: string): string {
  const input = document.getElementById(id) as HTMLInputElement | null;
  return input?.value.trim() ?? '';
}

function navigate(path: string): void {
  window.history.pushState({}, '', path);
  render();
}

function layout(title: string, content: string): string {
  return `
    <div class="shell">
      <header class="topbar">
        <div>
          <p class="eyebrow">Library Frontend</p>
          <h1>${title}</h1>
        </div>
        <div class="topbar-actions">
          <button id="clear-token" class="secondary" type="button">Clear Token</button>
        </div>
      </header>
      <nav class="nav-grid">
        <a href="/auth/login" data-link>Login</a>
        <a href="/auth/register" data-link>Register</a>
        <a href="/books" data-link>Books</a>
        <a href="/books/create" data-link>Create Book</a>
        <a href="/loans/create" data-link>Create Loan</a>
        <a href="/loans/my" data-link>My Loans</a>
      </nav>
      <main class="content">${content}</main>
    </div>
  `;
}

function loginPage(): Page {
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

        if (response.ok && typeof response.data === 'object' && response.data !== null) {
          const responseBody = response.data as Record<string, unknown>;
          const responseData = responseBody.data;
          const token =
            typeof responseData === 'object' && responseData !== null
              ? (responseData as Record<string, unknown>).token
              : undefined;

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

function registerPage(): Page {
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

function createBookPage(): Page {
  return {
    title: 'Create Book',
    html: `
      <section class="panel">
        <h2>POST /api/books</h2>
        <form id="book-form" class="form-grid two-columns">
          <label>Title <input id="book-title" type="text" required /></label>
          <label>Author <input id="book-author" type="text" required /></label>
          <label>Category <input id="book-category" type="text" required /></label>
          <label>ISBN <input id="book-isbn" type="text" required /></label>
          <label>Published Year <input id="book-year" type="number" required /></label>
          <label>Quantity <input id="book-quantity" type="number" required /></label>
          <button type="submit">Create Book</button>
        </form>
        <pre id="book-output" class="api-output"></pre>
      </section>
    `,
    setup: () => {
      const form = document.getElementById('book-form') as HTMLFormElement | null;
      if (!form) {
        return;
      }

      form.addEventListener('submit', async (event) => {
        event.preventDefault();

        const payload = {
          title: getInputValue('book-title'),
          author: getInputValue('book-author'),
          category: getInputValue('book-category'),
          isbn: getInputValue('book-isbn'),
          publishedYear: Number(getInputValue('book-year')),
          quantity: Number(getInputValue('book-quantity')),
        };

        const response = await request('/api/books', 'POST', payload);
        setApiOutput('book-output', response);
      });
    },
  };
}

function booksPage(): Page {
  return {
    title: 'Books',
    html: `
      <section class="panel">
        <div class="row-between">
          <h2>GET /api/books</h2>
          <button id="reload-books" type="button">Reload</button>
        </div>
        <div id="books-list" class="list"></div>
        <pre id="books-output" class="api-output"></pre>
      </section>
    `,
    setup: () => {
      const outputId = 'books-output';
      const list = document.getElementById('books-list');

      const loadBooks = async () => {
        const response = await request('/api/books', 'GET');
        setApiOutput(outputId, response);

        if (!list) {
          return;
        }

        if (!response.ok || !Array.isArray(response.data)) {
          list.innerHTML = '<p class="hint">No list available from this response format.</p>';
          return;
        }

        if (response.data.length === 0) {
          list.innerHTML = '<p class="hint">No books found.</p>';
          return;
        }

        list.innerHTML = response.data
          .map((item) => {
            if (typeof item !== 'object' || item === null) {
              return '';
            }

            const book = item as Record<string, unknown>;
            const id = String(book.id ?? book.bookId ?? '');
            const title = String(book.title ?? 'Untitled');
            const author = String(book.author ?? 'Unknown author');

            if (!id) {
              return `<article class="card"><h3>${title}</h3><p>${author}</p><p class="hint">No id found in item.</p></article>`;
            }

            return `
              <article class="card">
                <h3>${title}</h3>
                <p>${author}</p>
                <a href="/books/${id}" data-link>View details</a>
              </article>
            `;
          })
          .join('');
      };

      const reloadButton = document.getElementById('reload-books');
      reloadButton?.addEventListener('click', loadBooks);
      void loadBooks();
    },
  };
}

function bookDetailsPage(bookId: string): Page {
  return {
    title: `Book Details #${bookId}`,
    html: `
      <section class="panel">
        <h2>GET /api/books/${bookId}</h2>
        <pre id="book-details-output" class="api-output"></pre>
      </section>
    `,
    setup: () => {
      const loadDetails = async () => {
        const response = await request(`/books/${bookId}`, 'GET');
        setApiOutput('book-details-output', response);
      };

      void loadDetails();
    },
  };
}

function createLoanPage(): Page {
  return {
    title: 'Create Loan',
    html: `
      <section class="panel">
        <h2>POST /api/loans</h2>
        <form id="loan-form" class="form-grid two-columns">
          <label>User ID <input id="loan-user-id" type="text" required /></label>
          <label>Book ID <input id="loan-book-id" type="text" required /></label>
          <label>Loan Date <input id="loan-date" type="date" required /></label>
          <label>Due Date <input id="loan-due-date" type="date" required /></label>
          <button type="submit">Create Loan</button>
        </form>
        <pre id="loan-output" class="api-output"></pre>
      </section>
    `,
    setup: () => {
      const form = document.getElementById('loan-form') as HTMLFormElement | null;
      if (!form) {
        return;
      }

      form.addEventListener('submit', async (event) => {
        event.preventDefault();

        const payload = {
          userId: getInputValue('loan-user-id'),
          bookId: getInputValue('loan-book-id'),
          loanDate: getInputValue('loan-date'),
          dueDate: getInputValue('loan-due-date'),
        };

        const response = await request('/api/loans', 'POST', payload);
        setApiOutput('loan-output', response);
      });
    },
  };
}

function myLoansPage(): Page {
  return {
    title: 'My Loans',
    html: `
      <section class="panel">
        <h2>GET /api/loans/my?userId=...</h2>
        <form id="my-loans-form" class="form-grid inline">
          <label>User ID <input id="my-loans-user-id" type="text" required /></label>
          <button type="submit">Fetch Loans</button>
        </form>
        <pre id="my-loans-output" class="api-output"></pre>
      </section>
    `,
    setup: () => {
      const form = document.getElementById('my-loans-form') as HTMLFormElement | null;
      if (!form) {
        return;
      }

      form.addEventListener('submit', async (event) => {
        event.preventDefault();
        const userId = getInputValue('my-loans-user-id');
        const response = await request(`/api/loans/my?userId=${encodeURIComponent(userId)}`, 'GET');
        setApiOutput('my-loans-output', response);
      });
    },
  };
}

function homePage(): Page {
  return {
    title: 'Library API Tester',
    html: `
      <section class="panel">
        <h2>Backend</h2>
        <p>This frontend calls <strong>${API_BASE_URL}</strong>.</p>
        <p>Use the navigation links above to test authentication, books, and loans endpoints.</p>
      </section>
    `,
  };
}

function notFoundPage(path: string): Page {
  return {
    title: 'Not Found',
    html: `
      <section class="panel">
        <h2>Route not found</h2>
        <p>No page configured for <strong>${path}</strong>.</p>
      </section>
    `,
  };
}

function getPage(pathname: string): Page {
  if (pathname === '/') {
    return homePage();
  }

  if (pathname === '/auth/login') {
    return loginPage();
  }

  if (pathname === '/auth/register') {
    return registerPage();
  }

  if (pathname === '/books') {
    return booksPage();
  }

  if (pathname === '/books/create') {
    return createBookPage();
  }

  const bookDetailMatch = pathname.match(/^\/books\/([^/]+)$/);
  if (bookDetailMatch) {
    return bookDetailsPage(bookDetailMatch[1]);
  }

  if (pathname === '/loans/create') {
    return createLoanPage();
  }

  if (pathname === '/loans/my') {
    return myLoansPage();
  }

  return notFoundPage(pathname);
}

function bindCommonActions(): void {
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

function render(): void {
  const page = getPage(window.location.pathname);
  appRoot.innerHTML = layout(page.title, page.html);
  bindCommonActions();
  page.setup?.();
}

window.addEventListener('popstate', render);
render();

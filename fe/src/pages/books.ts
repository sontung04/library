import type { Page } from '../types';
import { request } from '../api';
import { getInputValue, setApiOutput, navigate } from '../utils';

export function createBookPage(): Page {
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
          copies: Number(getInputValue('book-quantity')),
        };

        const response = await request('/api/books', 'POST', payload);
        setApiOutput('book-output', response);
      });
    },
  };
}

export function booksPage(): Page {
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

      const reloadButton = document.getElementById('reload-books');
      reloadButton?.addEventListener('click', loadBooks);
      void loadBooks();
    },
  };
}

export function bookDetailsPage(bookId: string): Page {
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

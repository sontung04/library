import type { Page } from '../types';
import { request } from '../api';
import { getInputValue, setApiOutput, setMessage, navigate, unwrapResponseData } from '../utils';

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
          <label>Copies <input id="book-copies" type="number" required /></label>
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
          copies: Number(getInputValue('book-copies')),
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
      const list = document.getElementById('books-list');

      const deleteBook = async (bookId: string) => {
        if (!confirm(`Delete book ${bookId}?`)) {
          return;
        }
        const response = await request(`/api/books/${bookId}`, 'DELETE');
        setApiOutput('books-output', response);
        if (response.ok) {
          void loadBooks();
        } else {
          setMessage('books-output', 'Failed to delete book.', true);
        }
      };

      const loadBooks = async () => {
        const response = await request('/api/books', 'GET');
        setApiOutput('books-output', response);

        if (!list) {
          return;
        }

        // Response: { code, message, data: [{id, title, author, category, isbn, availableCopies, totalCopies}] }
        const body = unwrapResponseData(response.data);
        const books: unknown[] = Array.isArray(body) ? (body as unknown[]) : [];

        if (!response.ok) {
          list.innerHTML = '<p class="hint">Failed to load books.</p>';
          return;
        }

        if (books.length === 0) {
          list.innerHTML = '<p class="hint">No books found.</p>';
          return;
        }

        list.innerHTML = books
          .map((item) => {
            if (typeof item !== 'object' || item === null) {
              return '';
            }

            const book = item as Record<string, unknown>;
            const id = String(book.id ?? '');
            const title = String(book.title ?? 'Untitled');
            const author = String(book.author ?? 'Unknown author');
            const category = String(book.category ?? '');
            const available = String(book.availableCopies ?? '?');
            const total = String(book.totalCopies ?? '?');

            if (!id) {
              return `<article class="card"><h3>${title}</h3><p>${author}</p><p class="hint">No id found.</p></article>`;
            }

            return `
              <article class="card">
                <h3>${title}</h3>
                <p>${author}</p>
                ${category ? `<p class="hint">${category}</p>` : ''}
                <p class="hint">Copies: ${available} / ${total}</p>
                <div class="card-actions">
                  <a href="/books/${id}" data-link>View</a>
                  <a href="/books/${id}/edit" data-link class="btn-link">Edit</a>
                  <button class="danger" data-delete-id="${id}" type="button">Delete</button>
                </div>
              </article>
            `;
          })
          .join('');

        list.querySelectorAll<HTMLButtonElement>('button[data-delete-id]').forEach((btn) => {
          btn.addEventListener('click', () => {
            void deleteBook(btn.getAttribute('data-delete-id') ?? '');
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
        <div class="row-between">
          <h2>GET /api/books/${bookId}</h2>
          <div>
            <a href="/books/${bookId}/edit" data-link class="btn-link">Edit</a>
            <button id="delete-book" class="danger" type="button">Delete</button>
          </div>
        </div>
        <div id="book-fields" class="details-grid"></div>
        <p id="book-details-message" class="inline-message"></p>
        <pre id="book-details-output" class="api-output"></pre>
      </section>
    `,
    setup: () => {
      const fields = document.getElementById('book-fields');

      const loadDetails = async () => {
        const response = await request(`/api/books/${bookId}`, 'GET');
        setApiOutput('book-details-output', response);

        if (!fields || !response.ok) {
          return;
        }

        const book = unwrapResponseData(response.data);
        if (!book) {
          return;
        }

        fields.innerHTML = `
          <article class="detail-item"><span class="detail-label">ID</span><strong>${String(book.id ?? '') || 'N/A'}</strong></article>
          <article class="detail-item"><span class="detail-label">Title</span><strong>${String(book.title ?? '') || 'N/A'}</strong></article>
          <article class="detail-item"><span class="detail-label">Author</span><strong>${String(book.author ?? '') || 'N/A'}</strong></article>
          <article class="detail-item"><span class="detail-label">Category</span><strong>${String(book.category ?? '') || 'N/A'}</strong></article>
          <article class="detail-item"><span class="detail-label">ISBN</span><strong>${String(book.isbn ?? '') || 'N/A'}</strong></article>
          <article class="detail-item"><span class="detail-label">Available Copies</span><strong>${String(book.availableCopies ?? '') || 'N/A'}</strong></article>
          <article class="detail-item"><span class="detail-label">Total Copies</span><strong>${String(book.totalCopies ?? '') || 'N/A'}</strong></article>
        `;
      };

      document.getElementById('delete-book')?.addEventListener('click', async () => {
        if (!confirm(`Delete book ${bookId}?`)) {
          return;
        }
        const response = await request(`/api/books/${bookId}`, 'DELETE');
        setApiOutput('book-details-output', response);
        if (response.ok) {
          setMessage('book-details-message', 'Book deleted.');
          navigate('/books');
        } else {
          setMessage('book-details-message', 'Failed to delete book.', true);
        }
      });

      void loadDetails();
    },
  };
}

export function editBookPage(bookId: string): Page {
  return {
    title: `Edit Book #${bookId}`,
    html: `
      <section class="panel">
        <h2>PUT /api/books/${bookId}</h2>
        <form id="edit-book-form" class="form-grid two-columns">
          <label>Title <input id="edit-book-title" type="text" required /></label>
          <label>Author <input id="edit-book-author" type="text" required /></label>
          <label>Category <input id="edit-book-category" type="text" required /></label>
          <label>ISBN <input id="edit-book-isbn" type="text" required /></label>
          <label>Available Copies <input id="edit-book-available" type="number" required /></label>
          <label>Total Copies <input id="edit-book-total" type="number" required /></label>
          <button type="submit">Save Changes</button>
        </form>
        <p id="edit-book-message" class="inline-message"></p>
        <pre id="edit-book-output" class="api-output"></pre>
      </section>
    `,
    setup: () => {
      const form = document.getElementById('edit-book-form') as HTMLFormElement | null;
      if (!form) {
        return;
      }

      const prefill = async () => {
        const response = await request(`/api/books/${bookId}`, 'GET');
        setApiOutput('edit-book-output', response);
        if (!response.ok) {
          setMessage('edit-book-message', 'Failed to load book data.', true);
          return;
        }
        const book = unwrapResponseData(response.data);
        if (!book) {
          return;
        }

        (document.getElementById('edit-book-title') as HTMLInputElement).value = String(book.title ?? '');
        (document.getElementById('edit-book-author') as HTMLInputElement).value = String(book.author ?? '');
        (document.getElementById('edit-book-category') as HTMLInputElement).value = String(book.category ?? '');
        (document.getElementById('edit-book-isbn') as HTMLInputElement).value = String(book.isbn ?? '');
        (document.getElementById('edit-book-available') as HTMLInputElement).value = String(book.availableCopies ?? '');
        (document.getElementById('edit-book-total') as HTMLInputElement).value = String(book.totalCopies ?? '');
      };

      void prefill();

      form.addEventListener('submit', async (event) => {
        event.preventDefault();

        const payload = {
          title: getInputValue('edit-book-title'),
          author: getInputValue('edit-book-author'),
          category: getInputValue('edit-book-category'),
          isbn: getInputValue('edit-book-isbn'),
          availableCopies: Number(getInputValue('edit-book-available')),
          totalCopies: Number(getInputValue('edit-book-total')),
        };

        const response = await request(`/api/books/${bookId}`, 'PUT', payload);
        setApiOutput('edit-book-output', response);
        setMessage(
          'edit-book-message',
          response.ok ? 'Book updated successfully.' : 'Failed to update book.',
          !response.ok,
        );
      });
    },
  };
}

import { getStoredUser, typedRequest } from '../api';
import type { Book, Page } from '../types';
import {
  bindNavigationLinks,
  escapeHtml,
  getInputValue,
  getNumberValue,
  hasRole,
  navigate,
  pushFlash,
  setMessage,
} from '../utils';

export function createBookPage(): Page {
  return {
    title: 'Create Book',
    html: `
      <section class="panel">
        <h2>Create a new catalog entry</h2>
        <p class="hint">Librarians can add a title and its initial stock in one step.</p>
        <form id="book-form" class="form-grid two-columns">
          <label>Title <input id="book-title" type="text" required /></label>
          <label>Author <input id="book-author" type="text" required /></label>
          <label>Category <input id="book-category" type="text" required /></label>
          <label>ISBN <input id="book-isbn" type="text" required /></label>
          <label>Initial Copies <input id="book-copies" type="number" min="1" required /></label>
          <div class="form-actions">
            <button type="submit">Create book</button>
          </div>
        </form>
        <p id="book-message" class="inline-message"></p>
      </section>
    `,
    setup: () => {
      const user = getStoredUser();
      if (!hasRole(user, 'ROLE_LIBRARIAN')) {
        setMessage('book-message', 'Only librarians can create books.', true);
        return;
      }

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
          copies: getNumberValue('book-copies'),
        };

        const response = await typedRequest<Book>('/api/books', 'POST', payload);
        if (!response.ok || !response.data) {
          setMessage('book-message', response.message || 'Failed to create book.', true);
          return;
        }

        pushFlash({ tone: 'success', text: `Book "${response.data.title}" created.` });
        navigate(`/books/${response.data.id}`);
      });
    },
  };
}

export function bookDetailsPage(bookId: string): Page {
  return {
    title: 'Book details',
    html: `
      <section class="panel">
        <div class="row-between">
          <div>
            <p class="eyebrow">Catalog item</p>
            <h2 id="book-heading">Loading book</h2>
          </div>
          <div id="book-actions" class="table-actions"></div>
        </div>
        <div id="book-fields" class="details-grid"></div>
        <div id="book-meta" class="panel-subtle"></div>
        <p id="book-details-message" class="inline-message"></p>
      </section>
    `,
    setup: async () => {
      const user = getStoredUser();
      const isLibrarian = hasRole(user, 'ROLE_LIBRARIAN');
      const heading = document.getElementById('book-heading');
      const fields = document.getElementById('book-fields');
      const actions = document.getElementById('book-actions');
      const meta = document.getElementById('book-meta');

      const response = await typedRequest<Book>(`/api/books/${bookId}`, 'GET');
      if (!response.ok || !response.data || !fields || !heading || !actions || !meta) {
        setMessage('book-details-message', response.message || 'Failed to load book.', true);
        return;
      }

      const book = response.data;
      heading.textContent = book.title;
      meta.innerHTML = `<p class="hint">${escapeHtml(book.author)} • ${escapeHtml(book.category || 'General')} • ISBN ${escapeHtml(book.isbn)}</p>`;
      fields.innerHTML = `
        <article class="detail-item"><span class="detail-label">Available copies</span><strong>${book.availableCopies}</strong></article>
        <article class="detail-item"><span class="detail-label">Total copies</span><strong>${book.totalCopies}</strong></article>
        <article class="detail-item"><span class="detail-label">Category</span><strong>${escapeHtml(book.category || 'General')}</strong></article>
        <article class="detail-item"><span class="detail-label">ISBN</span><strong>${escapeHtml(book.isbn)}</strong></article>
      `;

      actions.innerHTML = isLibrarian
        ? `
            <a href="/books/${book.id}/edit" data-link class="ghost-link">Update book</a>
            <a href="/books/${book.id}/stock" data-link class="ghost-link">Increase stock</a>
            <button id="delete-book" class="danger" type="button">Delete</button>
          `
        : '<a href="/loans/borrow" data-link class="ghost-link">Borrow flow</a>';
      bindNavigationLinks();

      const deleteButton = document.getElementById('delete-book');
      deleteButton?.addEventListener('click', async () => {
        if (!globalThis.confirm(`Delete "${book.title}"?`)) {
          return;
        }

        const deleteResponse = await typedRequest<null>(`/api/books/${book.id}`, 'DELETE');
        if (!deleteResponse.ok) {
          setMessage('book-details-message', deleteResponse.message || 'Failed to delete book.', true);
          return;
        }

        pushFlash({ tone: 'success', text: `Book "${book.title}" deleted.` });
        navigate('/');
      });
    },
  };
}

export function editBookPage(bookId: string): Page {
  return {
    title: 'Update book',
    html: `
      <section class="panel">
        <h2>Edit book details</h2>
        <p class="hint">Use this form to update metadata and the declared total number of copies.</p>
        <form id="edit-book-form" class="form-grid two-columns">
          <label>Title <input id="edit-book-title" type="text" required /></label>
          <label>Author <input id="edit-book-author" type="text" required /></label>
          <label>Category <input id="edit-book-category" type="text" required /></label>
          <label>ISBN <input id="edit-book-isbn" type="text" required /></label>
          <label>Total Copies <input id="edit-book-total" type="number" min="1" required /></label>
          <div class="form-note" id="edit-book-current"></div>
          <div class="form-actions">
            <button type="submit">Save changes</button>
          </div>
        </form>
        <p id="edit-book-message" class="inline-message"></p>
      </section>
    `,
    setup: async () => {
      const user = getStoredUser();
      if (!hasRole(user, 'ROLE_LIBRARIAN')) {
        setMessage('edit-book-message', 'Only librarians can edit books.', true);
        return;
      }

      const response = await typedRequest<Book>(`/api/books/${bookId}`, 'GET');
      if (!response.ok || !response.data) {
        setMessage('edit-book-message', response.message || 'Failed to load book data.', true);
        return;
      }

      const book = response.data;
      const titleInput = document.getElementById('edit-book-title') as HTMLInputElement | null;
      const authorInput = document.getElementById('edit-book-author') as HTMLInputElement | null;
      const categoryInput = document.getElementById('edit-book-category') as HTMLInputElement | null;
      const isbnInput = document.getElementById('edit-book-isbn') as HTMLInputElement | null;
      const totalInput = document.getElementById('edit-book-total') as HTMLInputElement | null;
      const currentBlock = document.getElementById('edit-book-current');

      if (!titleInput || !authorInput || !categoryInput || !isbnInput || !totalInput || !currentBlock) {
        return;
      }

      titleInput.value = book.title;
      authorInput.value = book.author;
      categoryInput.value = book.category;
      isbnInput.value = book.isbn;
      totalInput.value = String(book.totalCopies);
      currentBlock.innerHTML = `
        <p class="hint">Currently available: <strong>${book.availableCopies}</strong></p>
        <p class="hint">If you need to add physical copies, use the stock increase page instead.</p>
      `;

      const form = document.getElementById('edit-book-form') as HTMLFormElement | null;
      form?.addEventListener('submit', async (event) => {
        event.preventDefault();

        const updateResponse = await typedRequest<Book>(`/api/books/${bookId}`, 'PUT', {
          title: getInputValue('edit-book-title'),
          author: getInputValue('edit-book-author'),
          category: getInputValue('edit-book-category'),
          isbn: getInputValue('edit-book-isbn'),
          totalCopies: getNumberValue('edit-book-total'),
        });

        if (!updateResponse.ok || !updateResponse.data) {
          setMessage('edit-book-message', updateResponse.message || 'Failed to update book.', true);
          return;
        }

        pushFlash({ tone: 'success', text: `Book "${updateResponse.data.title}" updated.` });
        navigate(`/books/${bookId}`);
      });
    },
  };
}

export function increaseStockPage(bookId: string): Page {
  return {
    title: 'Increase stock',
    html: `
      <section class="panel">
        <h2>Increase available copies</h2>
        <p class="hint">This action adds new copies to both the available and total stock count.</p>
        <div id="stock-book-summary" class="panel-subtle"></div>
        <form id="stock-form" class="form-grid inline">
          <label>Additional copies <input id="stock-additional" type="number" min="1" value="1" required /></label>
          <button type="submit">Apply stock update</button>
        </form>
        <p id="stock-message" class="inline-message"></p>
      </section>
    `,
    setup: async () => {
      const user = getStoredUser();
      if (!hasRole(user, 'ROLE_LIBRARIAN')) {
        setMessage('stock-message', 'Only librarians can increase stock.', true);
        return;
      }

      const response = await typedRequest<Book>(`/api/books/${bookId}`, 'GET');
      if (!response.ok || !response.data) {
        setMessage('stock-message', response.message || 'Failed to load book.', true);
        return;
      }

      const book = response.data;
      const summary = document.getElementById('stock-book-summary');
      if (summary) {
        summary.innerHTML = `
          <h3>${escapeHtml(book.title)}</h3>
          <p class="hint">Current stock: ${book.availableCopies} available of ${book.totalCopies} total copies.</p>
        `;
      }

      const form = document.getElementById('stock-form') as HTMLFormElement | null;
      form?.addEventListener('submit', async (event) => {
        event.preventDefault();

        const stockResponse = await typedRequest<Book>(`/api/books/${bookId}/stock`, 'PATCH', {
          additionalCopies: getNumberValue('stock-additional'),
        });

        if (!stockResponse.ok || !stockResponse.data) {
          setMessage('stock-message', stockResponse.message || 'Failed to update stock.', true);
          return;
        }

        pushFlash({ tone: 'success', text: `Stock updated for "${stockResponse.data.title}".` });
        navigate(`/books/${bookId}`);
      });
    },
  };
}

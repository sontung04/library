import { getStoredUser, typedRequest } from '../api';
import type { Book, Page } from '../types';
import { bindNavigationLinks, escapeHtml, hasRole, navigate, setRequestFeedback } from '../utils';

export function homePage(): Page {
  return {
    title: 'Home',
    html: `
      <section class="panel">
        <div id="home-actions" class="row-between section-gap"></div>
        <div id="home-books" class="list"></div>
        <p id="home-message" class="inline-message"></p>
      </section>
    `,
    setup: async () => {
      const user = getStoredUser();
      const actions = document.getElementById('home-actions');
      const booksContainer = document.getElementById('home-books');

      if (!actions || !booksContainer) {
        return;
      }

      const canBorrow = hasRole(user, 'ROLE_USER');
      const canManageBooks = hasRole(user, 'ROLE_LIBRARIAN');

      actions.innerHTML = `
        <div>
          <h3>Catalog</h3>
          <p class="hint">${user ? 'Use the table below to continue your task.' : 'Log in to borrow or manage books.'}</p>
        </div>
        <div class="action-row">
          ${canBorrow ? '<a href="/loans/borrow" data-link class="solid-button">Borrow a book</a>' : ''}
          ${canManageBooks ? '<a href="/books/new" data-link class="solid-button">Create a book</a>' : ''}
        </div>
      `;
      bindNavigationLinks();

      if (!user || (!canBorrow && !canManageBooks)) {
        booksContainer.innerHTML = `
          <article class="empty-state">
            <h3>Welcome to the library portal</h3>
            <p>Sign in as a user to borrow books or as a librarian to manage the catalog and loan activity.</p>
          </article>
        `;
        return;
      }

      const response = await typedRequest<Book[]>('/api/books', 'GET');
      if (response.ok && response.status === 204) {
        booksContainer.innerHTML = renderBookTable([], canBorrow, canManageBooks);
        setRequestFeedback('home-message', { ...response, message: '' }, '');
        return;
      }

      if (!response.ok || !response.data) {
        setRequestFeedback('home-message', response, 'Failed to load books.');
        booksContainer.innerHTML = '';
        return;
      }

      booksContainer.innerHTML = renderBookTable(response.data, canBorrow, canManageBooks);
      bindNavigationLinks();
      booksContainer.querySelectorAll<HTMLButtonElement>('[data-borrow-id]').forEach((button) => {
        button.addEventListener('click', () => {
          const bookId = button.dataset.borrowId;
          if (bookId) {
            navigate(`/loans/confirm/${bookId}`);
          }
        });
      });
    },
  };
}

export function notFoundPage(path: string): Page {
  return {
    title: 'Not Found',
    html: `
      <section class="panel">
        <h2>Route not found</h2>
        <p>No page configured for <strong>${escapeHtml(path)}</strong>.</p>
      </section>
    `,
  };
}

function renderBookTable(books: Book[], canBorrow: boolean, canManageBooks: boolean): string {
  if (books.length === 0) {
    return '<article class="empty-state"><h3>No books available</h3><p>Add a book to get started.</p></article>';
  }

  return `
    <div class="table-card">
      <table class="resource-table">
        <thead>
          <tr>
            <th>Title</th>
            <th>Author</th>
            <th>Category</th>
            <th>ISBN</th>
            <th>Available</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          ${books
            .map(
              (book) => `
                <tr>
                  <td><a href="/books/${book.id}" data-link>${escapeHtml(book.title)}</a></td>
                  <td>${escapeHtml(book.author)}</td>
                  <td>${escapeHtml(book.category || 'General')}</td>
                  <td>${escapeHtml(book.isbn)}</td>
                  <td>${book.availableCopies} / ${book.totalCopies}</td>
                  <td>
                    <div class="table-actions">
                      <a href="/books/${book.id}" data-link class="ghost-link">Details</a>
                      ${canBorrow && book.availableCopies > 0 ? `<button type="button" data-borrow-id="${book.id}">Borrow</button>` : ''}
                      ${canManageBooks ? `<a href="/books/${book.id}/edit" data-link class="ghost-link">Update book</a>` : ''}
                      ${canManageBooks ? `<a href="/books/${book.id}/stock" data-link class="ghost-link">Update availability</a>` : ''}
                    </div>
                  </td>
                </tr>
              `,
            )
            .join('')}
        </tbody>
      </table>
    </div>
  `;
}

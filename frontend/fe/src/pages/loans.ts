import { getStoredUser, typedRequest } from '../api';
import type { Book, Loan, LoanHistoryMode, Page } from '../types';
import {
  bindNavigationLinks,
  escapeHtml,
  formatDate,
  getInputValue,
  hasRole,
  navigate,
  pushFlash,
  setMessage,
} from '../utils';

type LoanStatusFilter = 'all' | 'active' | 'returned';

export function borrowBookPage(): Page {
  return {
    title: 'Borrow a book',
    html: `
      <section class="panel">
        <div class="row-between">
          <div>
            <h2>Choose a book to borrow</h2>
            <p class="hint">Readers borrow one title at a time. Librarians can also use this flow to verify the user experience.</p>
          </div>
          <a href="/" data-link class="ghost-link">Back to home</a>
        </div>
        <div id="borrow-books" class="list"></div>
        <p id="borrow-message" class="inline-message"></p>
      </section>
    `,
    setup: async () => {
      const user = getStoredUser();
      if (!user || (!hasRole(user, 'ROLE_USER') && !hasRole(user, 'ROLE_LIBRARIAN'))) {
        setMessage('borrow-message', 'You need a user or librarian role to borrow books.', true);
        return;
      }

      const response = await typedRequest<Book[]>('/api/books', 'GET');
      if (response.ok && response.status === 204) {
        const container = document.getElementById('borrow-books');
        if (container) {
          container.innerHTML = '<article class="empty-state"><h3>No books available</h3><p>All books are currently on loan or the catalog is still empty.</p></article>';
        }
        return;
      }

      if (!response.ok || !response.data) {
        setMessage('borrow-message', response.message || 'Failed to load books.', true);
        return;
      }

      const availableBooks = response.data.filter((book) => book.availableCopies > 0);
      const container = document.getElementById('borrow-books');
      if (!container) {
        return;
      }

      if (availableBooks.length === 0) {
        container.innerHTML = '<article class="empty-state"><h3>No books available</h3><p>All books are currently on loan.</p></article>';
        return;
      }

      container.innerHTML = availableBooks
        .map(
          (book) => `
            <article class="catalog-card">
              <div>
                <p class="eyebrow">${escapeHtml(book.category || 'General')}</p>
                <h3>${escapeHtml(book.title)}</h3>
                <p class="hint">${escapeHtml(book.author)} • ISBN ${escapeHtml(book.isbn)}</p>
                <p class="hint">${book.availableCopies} of ${book.totalCopies} copies available</p>
              </div>
              <button type="button" data-borrow-book-id="${book.id}">Continue</button>
            </article>
          `,
        )
        .join('');

      container.querySelectorAll<HTMLButtonElement>('[data-borrow-book-id]').forEach((button) => {
        button.addEventListener('click', () => {
          const bookId = button.dataset.borrowBookId;
          if (bookId) {
            navigate(`/loans/confirm/${bookId}`);
          }
        });
      });
      bindNavigationLinks();
    },
  };
}

export function confirmBorrowPage(bookId: string): Page {
  return {
    title: 'Confirm borrowing',
    html: `
      <section class="panel">
        <h2>Confirm loan</h2>
        <div id="confirm-book" class="panel-subtle"></div>
        <form id="confirm-loan-form" class="form-grid inline">
          <label>Due date <input id="confirm-due-date" type="date" required /></label>
          <button type="submit">Confirm borrow</button>
        </form>
        <p id="confirm-loan-message" class="inline-message"></p>
      </section>
    `,
    setup: async () => {
      const user = getStoredUser();
      if (!user || (!hasRole(user, 'ROLE_USER') && !hasRole(user, 'ROLE_LIBRARIAN'))) {
        setMessage('confirm-loan-message', 'You need permission to borrow books.', true);
        return;
      }

      const response = await typedRequest<Book>(`/api/books/${bookId}`, 'GET');
      if (!response.ok || !response.data) {
        setMessage('confirm-loan-message', response.message || 'Failed to load book.', true);
        return;
      }

      const book = response.data;
      const summary = document.getElementById('confirm-book');
      const dueDateInput = document.getElementById('confirm-due-date') as HTMLInputElement | null;
      if (!summary || !dueDateInput) {
        return;
      }

      summary.innerHTML = `
        <h3>${escapeHtml(book.title)}</h3>
        <p class="hint">${escapeHtml(book.author)} • ${book.availableCopies} copies still available</p>
      `;

      const defaultDueDate = new Date();
      defaultDueDate.setDate(defaultDueDate.getDate() + 14);
      dueDateInput.value = defaultDueDate.toISOString().split('T')[0];

      const form = document.getElementById('confirm-loan-form') as HTMLFormElement | null;
      form?.addEventListener('submit', async (event) => {
        event.preventDefault();

        const loanResponse = await typedRequest<Loan>('/api/loans', 'POST', {
          bookId: Number(bookId),
          dueDate: getInputValue('confirm-due-date'),
        });

        if (!loanResponse.ok) {
          setMessage('confirm-loan-message', loanResponse.message || 'Failed to create loan.', true);
          return;
        }

        pushFlash({ tone: 'success', text: `Loan created for "${book.title}".` });
        navigate('/loans/history');
      });
    },
  };
}

export function loanHistoryPage(): Page {
  return {
    title: 'Loan history',
    html: `
      <section class="panel">
        <div class="row-between">
          <div>
            <h2>Loan history</h2>
            <p id="loan-history-subtitle" class="hint"></p>
          </div>
          <div id="loan-history-actions" class="table-actions"></div>
        </div>
        <div id="loan-history-controls"></div>
        <div id="loan-history-list" class="list"></div>
        <p id="loan-history-message" class="inline-message"></p>
      </section>
    `,
    setup: async () => {
      const user = getStoredUser();
      const isReader = hasRole(user, 'ROLE_USER');
      const isLibrarian = hasRole(user, 'ROLE_LIBRARIAN');
      const subtitle = document.getElementById('loan-history-subtitle');
      const actions = document.getElementById('loan-history-actions');
      const controls = document.getElementById('loan-history-controls');

      if (!user || (!isReader && !isLibrarian) || !subtitle || !actions || !controls) {
        setMessage('loan-history-message', 'No loan history available for the current account.', true);
        return;
      }

      let mode: LoanHistoryMode = isLibrarian ? 'all' : 'mine';
      let statusFilter: LoanStatusFilter = 'all';
      let activeUserId = '';
      let loadedLoans: Loan[] = [];
      let currentEmptyText = 'No loan records found.';

      const applyStatusFilter = (loans: Loan[]): Loan[] => {
        if (statusFilter === 'active') {
          return loans.filter((loan) => loan.status === 'ACTIVE');
        }

        if (statusFilter === 'returned') {
          return loans.filter((loan) => loan.status === 'RETURNED');
        }

        return loans;
      };

      const getEmptyText = (): string => {
        if (statusFilter === 'all') {
          return currentEmptyText;
        }

        if (mode === 'user' && activeUserId) {
          return statusFilter === 'active'
            ? `User ${activeUserId} has no active loans.`
            : `User ${activeUserId} has no returned loans.`;
        }

        if (mode === 'mine') {
          return statusFilter === 'active' ? 'You have no active loans.' : 'You have no returned loans.';
        }

        return statusFilter === 'active'
          ? 'No active loan records found.'
          : 'No returned loan records found.';
      };

      const refreshVisibleLoans = () => {
        setMessage('loan-history-message', '');
        renderLoans(applyStatusFilter(loadedLoans), getEmptyText(), isLibrarian, () => {
          void setMode(mode);
        });
      };

      const loadLoans = async (endpoint: string, emptyText: string) => {
        const response = await typedRequest<Loan[]>(endpoint, 'GET');
        if (!response.ok || !response.data) {
          setMessage('loan-history-message', response.message || 'Failed to load loans.', true);
          loadedLoans = [];
          currentEmptyText = emptyText;
          renderLoans([]);
          return;
        }

        loadedLoans = response.data;
        currentEmptyText = emptyText;
        refreshVisibleLoans();
      };

      const renderMode = () => {
        subtitle.textContent = isLibrarian
          ? 'Librarians can inspect all loans, search by user id, filter returned status, and mark returns.'
          : 'Your account history and return actions are shown here.';
        actions.innerHTML = isLibrarian
          ? '<button id="load-all-loans" type="button" class="secondary">All users</button>'
          : '<button id="load-my-loans" type="button">Load my loans</button>';
        controls.innerHTML = isLibrarian
          ? `
            <form id="loan-history-search-form" class="form-grid inline">
              <label>
                User ID
                <input id="loan-history-user-id" type="number" min="1" step="1" placeholder="Find a user history" />
              </label>
              <label>
                Status
                <select id="loan-history-status-filter">
                  <option value="all">All loans</option>
                  <option value="active">Not returned</option>
                  <option value="returned">Returned</option>
                </select>
              </label>
              <button type="submit">Find user</button>
            </form>
          `
          : '';
        bindLoanHistoryActions(setMode);

        if (isLibrarian) {
          const statusSelect = document.getElementById('loan-history-status-filter') as HTMLSelectElement | null;
          const userIdInput = document.getElementById('loan-history-user-id') as HTMLInputElement | null;
          const searchForm = document.getElementById('loan-history-search-form') as HTMLFormElement | null;

          if (statusSelect) {
            statusSelect.value = statusFilter;
            statusSelect.addEventListener('change', () => {
              statusFilter = statusSelect.value as LoanStatusFilter;
              refreshVisibleLoans();
            });
          }

          if (userIdInput) {
            userIdInput.value = activeUserId;
          }

          searchForm?.addEventListener('submit', (event) => {
            event.preventDefault();

            const nextUserId = getInputValue('loan-history-user-id');
            const parsedUserId = Number(nextUserId);
            if (!nextUserId || !Number.isInteger(parsedUserId) || parsedUserId <= 0) {
              setMessage('loan-history-message', 'Enter a valid user id to load that history.', true);
              return;
            }

            activeUserId = String(parsedUserId);
            void setMode('user');
          });
        }
      };

      const setMode = async (nextMode: LoanHistoryMode) => {
        mode = nextMode;

        if (mode === 'mine') {
          await loadLoans('/api/loans/my', 'You have no recorded loans.');
          return;
        }

        if (mode === 'all') {
          activeUserId = '';
          const userIdInput = document.getElementById('loan-history-user-id') as HTMLInputElement | null;
          if (userIdInput) {
            userIdInput.value = '';
          }
          await loadLoans('/api/loans', 'No loan records found for any user.');
          return;
        }

        if (mode === 'user') {
          if (!activeUserId) {
            loadedLoans = [];
            currentEmptyText = 'Enter a user id to load loan history.';
            setMessage('loan-history-message', currentEmptyText, true);
            renderLoans([]);
            return;
          }

          await loadLoans(`/api/loans/users/${activeUserId}`, `No loan records found for user ${activeUserId}.`);
          return;
        }

        const list = document.getElementById('loan-history-list');
        if (list) {
          list.innerHTML = '';
        }
      };

      renderMode();
      if (isLibrarian) {
        void setMode('all');
        return;
      }

      if (isReader) {
        void setMode('mine');
        return;
      }

      setMessage('loan-history-message', 'Choose a view above to load loan history.');
    },
  };
}

function renderLoans(
  loans: Loan[],
  emptyText = 'No loan records found.',
  isLibrarian = false,
  onLoanReturned?: () => void,
): void {
  const container = document.getElementById('loan-history-list');
  if (!container) {
    return;
  }

  if (loans.length === 0) {
    container.innerHTML = `<article class="empty-state"><h3>No loans found</h3><p>${escapeHtml(emptyText)}</p></article>`;
    return;
  }

  const returnActionLabel = 'Mark returned';

  container.innerHTML = loans
    .map(
      (loan) => `
        <article class="loan-card">
          <div class="row-between loan-card-header">
            <div>
              <p class="eyebrow">Loan #${loan.id}</p>
              <h3>User ${loan.userId}</h3>
            </div>
            <span class="status-pill ${loan.status.toLowerCase()}">${escapeHtml(loan.status)}</span>
          </div>
          <div class="loan-card-body">
            <p class="hint">Borrowed ${formatDate(loan.loanDate)} • Due ${formatDate(loan.dueDate)}</p>
            <p class="hint">Returned ${formatDate(loan.returnDate)}</p>
            <div class="loan-item-list">
              ${loan.items
                .map(
                  (item) => `
                    <div class="loan-item-row">
                      <strong>${escapeHtml(item.book?.title ?? `Book ${item.bookId}`)}</strong>
                      <span class="hint">${escapeHtml(item.book?.author ?? 'Unknown author')} • Qty ${item.amount}</span>
                    </div>
                  `,
                )
                .join('')}
            </div>
          </div>
          ${isLibrarian && loan.status === 'ACTIVE' ? `<div class="table-actions"><button type="button" data-return-loan-id="${loan.id}">${returnActionLabel}</button></div>` : ''}
        </article>
      `,
    )
    .join('');

  container.querySelectorAll<HTMLButtonElement>('[data-return-loan-id]').forEach((button) => {
    button.addEventListener('click', async () => {
      const loanId = button.dataset.returnLoanId;
      if (!loanId) {
        return;
      }

      const response = await typedRequest<Loan>(`/api/loans/${loanId}/return`, 'PATCH');
      if (!response.ok) {
        setMessage('loan-history-message', response.message || 'Failed to return loan.', true);
        return;
      }

      pushFlash({ tone: 'success', text: `Loan #${loanId} marked as returned.` });
      if (onLoanReturned) {
        onLoanReturned();
        return;
      }

      navigate('/loans/history');
    });
  });
}

function bindLoanHistoryActions(
  setMode: (mode: LoanHistoryMode) => void | Promise<void>,
): void {
  document.getElementById('load-my-loans')?.addEventListener('click', () => {
    void setMode('mine');
  });

  document.getElementById('load-all-loans')?.addEventListener('click', () => {
    void setMode('all');
  });
}

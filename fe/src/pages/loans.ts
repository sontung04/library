import type { Page } from '../types';
import { request } from '../api';
import { getInputValue, setApiOutput } from '../utils';

export function createLoanPage(): Page {
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

export function myLoansPage(): Page {
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

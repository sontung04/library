import type { Page } from '../types';
import { API_BASE_URL } from '../api';

export function homePage(): Page {
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

export function notFoundPage(path: string): Page {
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

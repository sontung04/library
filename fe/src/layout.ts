export function layout(title: string, content: string): string {
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
        <a href="/users/me" data-link>My Profile</a>
        <a href="/users" data-link>Users</a>
        <a href="/books" data-link>Books</a>
        <a href="/books/create" data-link>Create Book</a>
        <a href="/loans/create" data-link>Create Loan</a>
        <a href="/loans/my" data-link>My Loans</a>
      </nav>
      <main class="content">${content}</main>
    </div>
  `;
}

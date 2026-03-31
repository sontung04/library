import './style.css';
import { hydrateCurrentUser } from './api';
import { layout } from './layout';
import { getPage } from './router';
import { bindNavigationLinks } from './utils';

const app = document.querySelector<HTMLDivElement>('#app');

if (!app) {
  throw new Error('App root was not found');
}

const appRoot: HTMLDivElement = app;

async function render(): Promise<void> {
  await hydrateCurrentUser();
  const page = getPage(globalThis.location.pathname);
  appRoot.innerHTML = layout(page.title, page.html, globalThis.location.pathname);
  bindNavigationLinks();
  await page.setup?.();
}

// Handle browser back/forward
globalThis.addEventListener('popstate', () => {
  void render();
});

// Handle programmatic navigation
globalThis.addEventListener('navigate', () => {
  void render();
});

// Initial render
await render();

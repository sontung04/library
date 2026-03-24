import './style.css';
import { layout } from './layout';
import { getPage } from './router';
import { bindNavigationLinks } from './utils';

const app = document.querySelector<HTMLDivElement>('#app');

if (!app) {
  throw new Error('App root was not found');
}

const appRoot: HTMLDivElement = app;

function render(): void {
  const page = getPage(window.location.pathname);
  appRoot.innerHTML = layout(page.title, page.html);
  bindNavigationLinks();
  page.setup?.();
}

// Handle browser back/forward
window.addEventListener('popstate', render);

// Handle programmatic navigation
window.addEventListener('navigate', () => {
  render();
});

// Initial render
render();

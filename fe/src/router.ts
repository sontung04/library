import type { Page } from './types';
import { loginPage, registerPage } from './pages/auth';
import { createBookPage, booksPage, bookDetailsPage, editBookPage } from './pages/books';
import { createLoanPage, myLoansPage } from './pages/loans';
import { usersListPage, editUserPage, myProfilePage, createUserPage } from './pages/users';
import { homePage, notFoundPage } from './pages/home';

export function getPage(pathname: string): Page {
  if (pathname === '/') {
    return homePage();
  }

  if (pathname === '/auth/login') {
    return loginPage();
  }

  if (pathname === '/auth/register') {
    return registerPage();
  }

  if (pathname === '/books') {
    return booksPage();
  }

  if (pathname === '/books/create') {
    return createBookPage();
  }

  const bookDetailMatch = pathname.match(/^\/books\/([^/]+)$/);
  if (bookDetailMatch) {
    return bookDetailsPage(bookDetailMatch[1]);
  }

  const editBookMatch = pathname.match(/^\/books\/([^/]+)\/edit$/);
  if (editBookMatch) {
    return editBookPage(editBookMatch[1]);
  }

  if (pathname === '/loans/create') {
    return createLoanPage();
  }

  if (pathname === '/loans/my') {
    return myLoansPage();
  }

  if (pathname === '/users/me') {
    return myProfilePage();
  }

  if (pathname === '/users/create') {
    return createUserPage();
  }

  if (pathname === '/users') {
    return usersListPage();
  }

  const editUserMatch = pathname.match(/^\/users\/([^/]+)\/edit$/);
  if (editUserMatch) {
    return editUserPage(editUserMatch[1]);
  }

  return notFoundPage(pathname);
}

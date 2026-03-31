import type { Page } from './types';
import { loginPage, registerPage } from './pages/auth';
import { createBookPage, bookDetailsPage, editBookPage, increaseStockPage } from './pages/books';
import { borrowBookPage, confirmBorrowPage, loanHistoryPage } from './pages/loans';
import { adminUserCreatePage, adminUserDeletePage, adminUsersPage, adminUserUpdatePage, myProfilePage } from './pages/users';
import { homePage, notFoundPage } from './pages/home';

const BOOK_DETAILS_ROUTE = /^\/books\/([^/]+)$/;
const BOOK_EDIT_ROUTE = /^\/books\/([^/]+)\/edit$/;
const BOOK_STOCK_ROUTE = /^\/books\/([^/]+)\/stock$/;
const LOAN_CONFIRM_ROUTE = /^\/loans\/confirm\/([^/]+)$/;
const ADMIN_USER_EDIT_ROUTE = /^\/admin\/users\/([^/]+)\/edit$/;
const ADMIN_USER_DELETE_ROUTE = /^\/admin\/users\/([^/]+)\/delete$/;

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

  if (pathname === '/books/new') {
    return createBookPage();
  }

  const bookDetailMatch = BOOK_DETAILS_ROUTE.exec(pathname);
  if (bookDetailMatch) {
    return bookDetailsPage(bookDetailMatch[1]);
  }

  const editBookMatch = BOOK_EDIT_ROUTE.exec(pathname);
  if (editBookMatch) {
    return editBookPage(editBookMatch[1]);
  }

  const stockBookMatch = BOOK_STOCK_ROUTE.exec(pathname);
  if (stockBookMatch) {
    return increaseStockPage(stockBookMatch[1]);
  }

  if (pathname === '/loans/borrow') {
    return borrowBookPage();
  }

  const confirmBorrowMatch = LOAN_CONFIRM_ROUTE.exec(pathname);
  if (confirmBorrowMatch) {
    return confirmBorrowPage(confirmBorrowMatch[1]);
  }

  if (pathname === '/loans/history') {
    return loanHistoryPage();
  }

  if (pathname === '/users/me') {
    return myProfilePage();
  }

  if (pathname === '/admin/users') {
    return adminUsersPage();
  }

  if (pathname === '/admin/users/new') {
    return adminUserCreatePage();
  }

  const adminUserEditMatch = ADMIN_USER_EDIT_ROUTE.exec(pathname);
  if (adminUserEditMatch) {
    return adminUserUpdatePage(adminUserEditMatch[1]);
  }

  const adminUserDeleteMatch = ADMIN_USER_DELETE_ROUTE.exec(pathname);
  if (adminUserDeleteMatch) {
    return adminUserDeletePage(adminUserDeleteMatch[1]);
  }

  return notFoundPage(pathname);
}

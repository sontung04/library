export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

export type ApiEnvelope<T> = {
  code: number;
  message: string;
  data: T | null;
};

export type ApiResult<T> = {
  ok: boolean;
  status: number;
  body: unknown;
  data: T | null;
  message: string;
};

export type Page = {
  title: string;
  html: string;
  setup?: () => void | Promise<void>;
};

export type AuthUser = {
  id: number;
  username: string;
  email: string;
  roles: string[];
};

export type LoginPayload = {
  token: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  username: string;
  roles: string[];
};

export type Book = {
  id: number;
  title: string;
  author: string;
  category: string;
  isbn: string;
  availableCopies: number;
  totalCopies: number;
};

export type LoanItem = {
  bookId: number;
  amount: number;
  book: Book | null;
};

export type Loan = {
  id: number;
  userId: number;
  items: LoanItem[];
  loanDate: string;
  dueDate: string;
  returnDate: string | null;
  status: string;
};

export type LoanHistoryMode = 'mine' | 'all' | 'user';

export type FlashMessage = {
  tone: 'success' | 'error';
  text: string;
};

import { getStoredUser } from './api';

export function isLoggedIn(): boolean {
  return Boolean(getStoredUser());
}
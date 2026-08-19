const REFRESH_TOKEN_KEY = 'careerforge_refresh_token';
const USER_KEY = 'careerforge_user';

export const storage = {
  getRefreshToken(): string | null {
    try {
      return localStorage.getItem(REFRESH_TOKEN_KEY);
    } catch {
      return null;
    }
  },

  setRefreshToken(token: string): void {
    try {
      localStorage.setItem(REFRESH_TOKEN_KEY, token);
    } catch {
      // Ignored
    }
  },

  removeRefreshToken(): void {
    try {
      localStorage.removeItem(REFRESH_TOKEN_KEY);
    } catch {
      // Ignored
    }
  },

  getUser<T>(): T | null {
    try {
      const data = localStorage.getItem(USER_KEY);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  },

  setUser<T>(user: T): void {
    try {
      localStorage.setItem(USER_KEY, JSON.stringify(user));
    } catch {
      // Ignored
    }
  },

  removeUser(): void {
    try {
      localStorage.removeItem(USER_KEY);
    } catch {
      // Ignored
    }
  },

  clearAuth(): void {
    this.removeRefreshToken();
    this.removeUser();
  },
};

import { create } from 'zustand';
import { UserSummary } from '@/types/auth.types';
import { storage } from '@/lib/storage';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserSummary | null;
  isAuthenticated: boolean;
  isLoading: boolean;

  setAuth: (accessToken: string, refreshToken: string, user: UserSummary) => void;
  setTokens: (accessToken: string, refreshToken: string) => void;
  setUser: (user: UserSummary) => void;
  logout: () => void;
  initSession: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: storage.getRefreshToken(),
  user: storage.getUser<UserSummary>(),
  isAuthenticated: !!storage.getRefreshToken(),
  isLoading: true,

  setAuth: (accessToken, refreshToken, user) => {
    storage.setRefreshToken(refreshToken);
    storage.setUser(user);
    set({
      accessToken,
      refreshToken,
      user,
      isAuthenticated: true,
      isLoading: false,
    });
  },

  setTokens: (accessToken, refreshToken) => {
    storage.setRefreshToken(refreshToken);
    set({
      accessToken,
      refreshToken,
      isAuthenticated: true,
    });
  },

  setUser: (user) => {
    storage.setUser(user);
    set({ user });
  },

  logout: () => {
    storage.clearAuth();
    set({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
      isLoading: false,
    });
  },

  initSession: () => {
    const refreshToken = storage.getRefreshToken();
    const user = storage.getUser<UserSummary>();
    set({
      refreshToken,
      user,
      isAuthenticated: !!refreshToken,
      isLoading: false,
    });
  },
}));

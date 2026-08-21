import { create } from 'zustand';
import axios from 'axios';
import { UserSummary } from '@/types/auth.types';
import { storage } from '@/lib/storage';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserSummary | null;
  isAuthenticated: boolean;
  isLoading: boolean;

  setAuth: (
    accessToken: string,
    refreshToken: string,
    user: UserSummary
  ) => void;

  setTokens: (
    accessToken: string,
    refreshToken: string
  ) => void;

  setUser: (user: UserSummary) => void;

  logout: () => void;

  initSession: () => Promise<void>;
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

    set({
      user,
    });
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

  initSession: async () => {
    const refreshToken = storage.getRefreshToken();
    const user = storage.getUser<UserSummary>();

    // No existing session
    if (!refreshToken) {
      set({
        accessToken: null,
        refreshToken: null,
        user,
        isAuthenticated: false,
        isLoading: false,
      });

      return;
    }

    try {
      const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

      const response = await axios.post(
        `${baseURL}/auth/refresh`,
        {
          refreshToken,
        }
      );

      const {
        accessToken,
        refreshToken: newRefreshToken,
      } = response.data.data;

      storage.setRefreshToken(newRefreshToken);

      set({
        accessToken,
        refreshToken: newRefreshToken,
        user,
        isAuthenticated: true,
        isLoading: false,
      });

      console.log('CareerForge session restored successfully');
    } catch (error) {
      console.error('CareerForge session restoration failed:', error);

      storage.clearAuth();

      set({
        accessToken: null,
        refreshToken: null,
        user: null,
        isAuthenticated: false,
        isLoading: false,
      });
    }
  },
}));
import { create } from 'zustand';
import axios from 'axios';
import { UserSummary } from '@/types/auth.types';
import { storage } from '@/lib/storage';
import { queryClient } from '@/lib/queryClient';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: UserSummary | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  isInitialized: boolean;
  isAccountDisabled: boolean;
  disabledMessage: string | null;

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

  setDisabled: (disabled: boolean, message?: string) => void;

  initSession: () => Promise<void>;
}

const initialRefreshToken = storage.getRefreshToken();
const initialUser = storage.getUser<UserSummary>();

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  refreshToken: initialRefreshToken,
  user: initialUser,
  isAuthenticated: false,
  isLoading: !!initialRefreshToken,
  isInitialized: !initialRefreshToken,
  isAccountDisabled: false,
  disabledMessage: null,

  setAuth: (accessToken, refreshToken, user) => {
    queryClient.clear();
    storage.setRefreshToken(refreshToken);
    storage.setUser(user);

    set({
      accessToken,
      refreshToken,
      user,
      isAuthenticated: true,
      isLoading: false,
      isInitialized: true,
      isAccountDisabled: false,
      disabledMessage: null,
    });
  },

  setDisabled: (disabled, message) => {
    if (disabled) {
      storage.clearAuth();
      queryClient.clear();
      set({
        accessToken: null,
        refreshToken: null,
        user: null,
        isAuthenticated: false,
        isLoading: false,
        isInitialized: true,
        isAccountDisabled: true,
        disabledMessage: message || 'Your account has been disabled by an administrator. Please contact support for assistance.',
      });
    } else {
      set({
        isAccountDisabled: false,
        disabledMessage: null,
      });
    }
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
    queryClient.clear();

    set({
      accessToken: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,
      isLoading: false,
      isInitialized: true,
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
        isInitialized: true,
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
        isInitialized: true,
      });

      console.log('CareerForge session restored successfully');
    } catch (error) {
      console.error('CareerForge session restoration failed:', error);

      storage.clearAuth();
      queryClient.clear();

      set({
        accessToken: null,
        refreshToken: null,
        user: null,
        isAuthenticated: false,
        isLoading: false,
        isInitialized: true,
      });
    }
  },
}));
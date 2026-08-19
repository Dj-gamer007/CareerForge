import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore } from '@/features/auth/authStore';

describe('Zustand AuthStore', () => {
  beforeEach(() => {
    useAuthStore.getState().logout();
  });

  it('sets authentication tokens and user state on login', () => {
    const user = { id: 1, email: 'student@careerforge.local', role: 'ROLE_STUDENT' as const };
    useAuthStore.getState().setAuth('mock-access-token', 'mock-refresh-token', user);

    const state = useAuthStore.getState();
    expect(state.isAuthenticated).toBe(true);
    expect(state.accessToken).toBe('mock-access-token');
    expect(state.refreshToken).toBe('mock-refresh-token');
    expect(state.user?.email).toBe('student@careerforge.local');
  });

  it('updates token pair on silent refresh', () => {
    useAuthStore.getState().setTokens('new-access-token', 'new-refresh-token');

    const state = useAuthStore.getState();
    expect(state.accessToken).toBe('new-access-token');
    expect(state.refreshToken).toBe('new-refresh-token');
  });

  it('clears all state on logout', () => {
    const user = { id: 1, email: 'student@careerforge.local', role: 'ROLE_STUDENT' as const };
    useAuthStore.getState().setAuth('mock-access-token', 'mock-refresh-token', user);

    useAuthStore.getState().logout();

    const state = useAuthStore.getState();
    expect(state.isAuthenticated).toBe(false);
    expect(state.accessToken).toBeNull();
    expect(state.refreshToken).toBeNull();
    expect(state.user).toBeNull();
  });
});

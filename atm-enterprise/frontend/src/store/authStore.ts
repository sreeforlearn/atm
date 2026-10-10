import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { LoginResponse, User, Card } from '@/types';

/**
 * Auth state stored in Zustand.
 * Persisted to localStorage so a page refresh keeps the session until the
 * token is rejected by the API (handled by the Axios response interceptor).
 */
export interface AuthState {
  user: User | null;
  card: Card | null;
  token: string | null;
  isAuthenticated: boolean;
  /** Hydrate auth state after a successful login. */
  login: (payload: LoginResponse) => void;
  /** Replace token (e.g. after refresh). */
  setToken: (token: string) => void;
  /** Clear all auth state. */
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      card: null,
      token: null,
      isAuthenticated: false,

      login: (payload) =>
        set({
          user: payload.user,
          card: payload.card ?? null,
          token: payload.token,
          isAuthenticated: true,
        }),

      setToken: (token) => set({ token }),

      logout: () =>
        set({
          user: null,
          card: null,
          token: null,
          isAuthenticated: false,
        }),
    }),
    {
      name: 'atm-auth',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        user: state.user,
        card: state.card,
        token: state.token,
        isAuthenticated: state.isAuthenticated,
      }),
    },
  ),
);

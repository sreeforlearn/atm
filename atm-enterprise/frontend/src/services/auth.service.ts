import api from './api';
import type {
  ApiResponse,
  LoginRequest,
  LoginResponse,
  PinChangeRequest,
  User,
} from '@/types';
import { useAuthStore } from '@/store/authStore';

/**
 * Authentication service — wraps `/api/v1/auth/*` endpoints.
 */
export const authService = {
  /**
   * Authenticate a customer with their card number + PIN.
   * @param payload card number (16 digits) and 4-digit PIN.
   * @returns login envelope containing JWT + user profile.
   */
  async login(payload: LoginRequest): Promise<LoginResponse> {
    const { data } = await api.post<ApiResponse<LoginResponse>>(
      '/auth/login',
      payload,
    );
    return data.data;
  },

  /**
   * Fetch the current authenticated user's profile.
   */
  async getProfile(): Promise<User> {
    const { data } = await api.get<ApiResponse<User>>('/auth/me');
    return data.data;
  },

  /**
   * Change the PIN for the authenticated card.
   * @param payload cardId, oldPin, newPin.
   */
  async changePin(payload: PinChangeRequest): Promise<void> {
    await api.put<ApiResponse<null>>('/auth/pin', payload);
  },

  /**
   * Revoke the current JWT (server-side blacklist) and clear local state.
   * Always clears local state even if the network call fails.
   */
  async logout(): Promise<void> {
    try {
      await api.post('/auth/logout');
    } finally {
      useAuthStore.getState().logout();
    }
  },
};

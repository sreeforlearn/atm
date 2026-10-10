import axios, { AxiosError, type AxiosInstance } from 'axios';
import { toast } from 'react-toastify';
import type { ApiResponse } from '@/types';
import { useAuthStore } from '@/store/authStore';

/**
 * Pre-configured Axios instance used by all service modules.
 *
 * - baseURL is `/api` so Vite's dev proxy and the nginx upstream both forward
 *   the request to the API Gateway running on port 8080.
 * - Request interceptor injects the `Authorization: Bearer <token>` header
 *   from the Zustand auth store.
 * - Response interceptor:
 *     • 401 → log the user out, navigate to /login, toast.
 *     • Other errors → surface the gateway's `message` via toast.
 */
const api: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token;
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status;
    const message =
      error.response?.data?.message ??
      error.message ??
      'Something went wrong. Please try again.';

    if (status === 401) {
      // Token rejected — wipe local session and bounce to login.
      useAuthStore.getState().logout();
      toast.error('Your session has expired. Please sign in again.');
      // Avoid full reload when already on a public route.
      if (window.location.pathname !== '/login') {
        window.location.assign('/login');
      }
    } else if (status === 403) {
      toast.error('You are not authorised to perform this action.');
    } else if (status && status >= 500) {
      toast.error('Server error. Please try again in a moment.');
    } else {
      toast.error(message);
    }

    return Promise.reject(error);
  },
);

export default api;

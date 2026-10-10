import { useMutation, useQuery } from '@tanstack/react-query';
import { toast } from 'react-toastify';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import { authService } from '@/services/auth.service';
import type { LoginRequest, PinChangeRequest } from '@/types';

/**
 * Login mutation. On success:
 *   1. persists token+user via Zustand store,
 *   2. shows a success toast,
 *   3. navigates to /dashboard.
 */
export function useLogin() {
  const login = useAuthStore((s) => s.login);
  const navigate = useNavigate();

  return useMutation({
    mutationKey: ['auth', 'login'],
    mutationFn: (payload: LoginRequest) => authService.login(payload),
    onSuccess: (data) => {
      login(data);
      toast.success(`Welcome back, ${data.user.firstName}!`);
      navigate('/dashboard', { replace: true });
    },
    onError: () => {
      // Toast handled in Axios interceptor; nothing extra to do.
    },
  });
}

/**
 * Logout mutation. Calls the server, then clears local state and redirects.
 */
export function useLogout() {
  const logout = useAuthStore((s) => s.logout);
  const navigate = useNavigate();

  return useMutation({
    mutationKey: ['auth', 'logout'],
    mutationFn: () => authService.logout(),
    onSettled: () => {
      logout();
      navigate('/login', { replace: true });
    },
  });
}

/**
 * PIN change mutation. Shows toast on success/failure and resets the form.
 */
export function useChangePin() {
  return useMutation({
    mutationKey: ['auth', 'change-pin'],
    mutationFn: (payload: PinChangeRequest) => authService.changePin(payload),
    onSuccess: () => {
      toast.success('PIN changed successfully. Please use the new PIN next time.');
    },
  });
}

/**
 * Optional — fetch the authenticated user's profile (e.g. for the Topbar).
 */
export function useProfile() {
  return useQuery({
    queryKey: ['auth', 'profile'],
    queryFn: () => authService.getProfile(),
    enabled: useAuthStore.getState().isAuthenticated,
    staleTime: 5 * 60 * 1000,
  });
}

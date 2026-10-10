import { Navigate, useLocation } from 'react-router-dom';
import { type ReactNode } from 'react';
import { useAuthStore } from '@/store/authStore';

/**
 * Route guard — if the user is not authenticated, redirect to /login and
 * remember where they were heading (`from`) so we can bounce them back.
 */
export default function ProtectedRoute({ children }: { children: ReactNode }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const token = useAuthStore((s) => s.token);
  const location = useLocation();

  if (!isAuthenticated || !token) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <>{children}</>;
}

import { Navigate, Route, Routes } from 'react-router-dom';
import { Suspense, lazy } from 'react';
import ProtectedRoute from '@/components/Layout/ProtectedRoute';
import DashboardLayout from '@/components/Layout/DashboardLayout';
import Loader from '@/components/ui/Loader';

// Lazy-loaded pages for code splitting
const Login = lazy(() => import('@/pages/Login'));
const Dashboard = lazy(() => import('@/pages/Dashboard'));
const Balance = lazy(() => import('@/pages/Balance'));
const Withdraw = lazy(() => import('@/pages/Withdraw'));
const Deposit = lazy(() => import('@/pages/Deposit'));
const Transfer = lazy(() => import('@/pages/Transfer'));
const Transactions = lazy(() => import('@/pages/Transactions'));
const PinChange = lazy(() => import('@/pages/PinChange'));
const NotFound = lazy(() => import('@/pages/NotFound'));

/**
 * Root application routes.
 * - `/login` is public.
 * - All customer routes are wrapped in `<ProtectedRoute>` and share `<DashboardLayout>`.
 * - `/logout` is a sentinel handled inside Topbar; redirects to `/login`.
 */
export default function App() {
  return (
    <Suspense fallback={<Loader full />}>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/logout" element={<Navigate to="/login" replace />} />

        <Route
          element={
            <ProtectedRoute>
              <DashboardLayout />
            </ProtectedRoute>
          }
        >
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/balance" element={<Balance />} />
          <Route path="/withdraw" element={<Withdraw />} />
          <Route path="/deposit" element={<Deposit />} />
          <Route path="/transfer" element={<Transfer />} />
          <Route path="/transactions" element={<Transactions />} />
          <Route path="/pin-change" element={<PinChange />} />
        </Route>

        <Route path="/" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </Suspense>
  );
}

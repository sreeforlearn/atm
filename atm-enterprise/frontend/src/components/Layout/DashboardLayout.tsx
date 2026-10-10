import { useState } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import {
  LayoutDashboard,
  Wallet,
  ArrowDownToLine,
  ArrowUpFromLine,
  Send,
  Receipt,
  Lock,
  X,
} from 'lucide-react';
import Topbar from './Topbar';
import Sidebar from './Sidebar';

const mobileItems = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/balance', label: 'Balance', icon: Wallet },
  { to: '/withdraw', label: 'Withdraw', icon: ArrowDownToLine },
  { to: '/deposit', label: 'Deposit', icon: ArrowUpFromLine },
  { to: '/transfer', label: 'Transfer', icon: Send },
  { to: '/transactions', label: 'Transactions', icon: Receipt },
  { to: '/pin-change', label: 'Change PIN', icon: Lock },
];

/**
 * Layout shell for authenticated pages. Renders the sidebar (desktop) plus a
 * slide-over drawer for mobile, with a sticky topbar. Child routes render into
 * the `<Outlet />` inside the main scroll area.
 */
export default function DashboardLayout() {
  const [drawerOpen, setDrawerOpen] = useState(false);

  return (
    <div className="flex min-h-screen">
      <Sidebar />

      {/* Mobile drawer */}
      {drawerOpen && (
        <div className="fixed inset-0 z-40 lg:hidden" role="dialog" aria-modal="true">
          <div
            className="absolute inset-0 bg-slate-900/60 backdrop-blur-sm"
            onClick={() => setDrawerOpen(false)}
            aria-hidden="true"
          />
          <div className="absolute left-0 top-0 h-full w-64 bg-primary-950/95 p-4 text-white animate-fade-in-fast">
            <div className="flex items-center justify-between mb-6">
              <span className="font-bold">Menu</span>
              <button
                type="button"
                aria-label="Close menu"
                onClick={() => setDrawerOpen(false)}
                className="rounded-lg p-1.5 hover:bg-white/10"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <nav className="space-y-1">
              {mobileItems.map(({ to, label, icon: Icon }) => (
                <NavLink
                  key={to}
                  to={to}
                  className="nav-link"
                  onClick={() => setDrawerOpen(false)}
                >
                  <Icon className="h-5 w-5" aria-hidden="true" />
                  <span>{label}</span>
                </NavLink>
              ))}
            </nav>
          </div>
        </div>
      )}

      {/* Main column */}
      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar onMenu={() => setDrawerOpen(true)} />
        <main className="flex-1 overflow-y-auto">
          <div className="page">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}

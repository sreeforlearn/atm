import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Wallet,
  ArrowDownToLine,
  ArrowUpFromLine,
  Send,
  Receipt,
  Lock,
} from 'lucide-react';

interface NavItem {
  to: string;
  label: string;
  icon: typeof LayoutDashboard;
}

/** Sidebar navigation. Each link uses NavLink's `aria-current=page` styling. */
const items: NavItem[] = [
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/balance', label: 'Balance', icon: Wallet },
  { to: '/withdraw', label: 'Withdraw', icon: ArrowDownToLine },
  { to: '/deposit', label: 'Deposit', icon: ArrowUpFromLine },
  { to: '/transfer', label: 'Transfer', icon: Send },
  { to: '/transactions', label: 'Transactions', icon: Receipt },
  { to: '/pin-change', label: 'Change PIN', icon: Lock },
];

export default function Sidebar() {
  return (
    <aside
      className="hidden lg:flex flex-col w-64 shrink-0 bg-primary-950/80 text-white"
      aria-label="Primary navigation"
    >
      <div className="flex items-center gap-2 px-6 py-6">
        <div className="grid h-9 w-9 place-items-center rounded-xl bg-accent-500 text-primary-950 font-bold">
          ATM
        </div>
        <div>
          <p className="text-sm font-bold leading-tight">ATM Enterprise</p>
          <p className="text-xs text-white/60">Customer Portal</p>
        </div>
      </div>

      <nav className="flex-1 px-3 py-2 space-y-1">
        {items.map((item) => {
          const Icon = item.icon;
          return (
            <NavLink key={item.to} to={item.to} className="nav-link">
              <Icon className="h-5 w-5" aria-hidden="true" />
              <span>{item.label}</span>
            </NavLink>
          );
        })}
      </nav>

      <footer className="px-6 py-4 text-xs text-white/50">
        &copy; {new Date().getFullYear()} ATM Enterprise
      </footer>
    </aside>
  );
}

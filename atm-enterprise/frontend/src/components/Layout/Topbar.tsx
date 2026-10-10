import { useQuery } from '@tanstack/react-query';
import { LogOut, Menu, UserCircle } from 'lucide-react';
import { toast } from 'react-toastify';
import { useAuthStore } from '@/store/authStore';
import { useLogout } from '@/hooks/useAuth';
import { accountService } from '@/services/account.service';
import { formatCurrency, maskAccountNumber } from '@/utils/format';
import Button from '@/components/ui/Button';

/**
 * Topbar — shows the brand on mobile, the user name, a balance pill, and a
 * logout button. The hamburger toggles the mobile drawer (controlled by parent).
 */
export default function Topbar({ onMenu }: { onMenu: () => void }) {
  const user = useAuthStore((s) => s.user);
  const card = useAuthStore((s) => s.card);
  const logout = useLogout();

  // Pull the primary account for the balance pill.
  const { data: account } = useQuery({
    queryKey: ['accounts', 'card'],
    queryFn: () => accountService.getAccountByCard(),
    staleTime: 30_000,
  });

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center justify-between gap-4 border-b border-white/10 bg-primary-950/70 px-4 text-white backdrop-blur-md lg:px-8">
      <div className="flex items-center gap-3">
        <button
          type="button"
          className="rounded-lg p-1.5 hover:bg-white/10 lg:hidden"
          aria-label="Open menu"
          onClick={onMenu}
        >
          <Menu className="h-5 w-5" />
        </button>
        <div className="lg:hidden flex items-center gap-2">
          <div className="grid h-8 w-8 place-items-center rounded-lg bg-accent-500 text-primary-950 text-xs font-bold">
            ATM
          </div>
          <span className="font-semibold">ATM Enterprise</span>
        </div>
        <span className="hidden lg:block text-sm text-white/70">
          Welcome, <span className="font-semibold text-white">{user?.firstName ?? 'Customer'}</span>
        </span>
      </div>

      <div className="flex items-center gap-3">
        {account && (
          <div className="hidden sm:flex flex-col items-end leading-tight">
            <span className="text-[10px] uppercase tracking-wider text-white/60">
              {maskAccountNumber(account.accountNumber)}
            </span>
            <span className="text-sm font-semibold">
              {formatCurrency(account.availableBalance ?? account.balance)}
            </span>
          </div>
        )}

        <div className="hidden sm:flex items-center gap-2 rounded-full bg-white/10 px-3 py-1.5">
          <UserCircle className="h-5 w-5 text-accent-300" />
          <span className="text-sm">
            {user ? `${user.firstName} ${user.lastName}` : 'Customer'}
          </span>
        </div>

        <Button
          variant="ghost"
          size="sm"
          className="text-white hover:bg-white/10"
          leftIcon={<LogOut className="h-4 w-4" />}
          loading={logout.isPending}
          onClick={() => {
            logout.mutate();
            toast.info('You have been logged out.');
          }}
        >
          <span className="hidden sm:inline">Logout</span>
        </Button>
      </div>

      {/* Stash the card holder name for screen-readers */}
      <span className="sr-only">{card?.cardHolderName}</span>
    </header>
  );
}

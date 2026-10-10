import { CreditCard, Eye, EyeOff } from 'lucide-react';
import { useState } from 'react';
import { formatCurrency, maskAccountNumber, maskCardNumber } from '@/utils/format';
import type { Account, Card as CardType } from '@/types';

export interface BalanceCardProps {
  account: Account | null;
  card?: CardType | null;
  /** Hide account balance (mask it) when the user toggles visibility off. */
  initialVisible?: boolean;
}

/**
 * Gradient ATM-card visual showing account number, balance and (optionally)
 * the linked card's masked number. Includes a show/hide balance toggle.
 */
export default function BalanceCard({
  account,
  card,
  initialVisible = true,
}: BalanceCardProps) {
  const [visible, setVisible] = useState(initialVisible);

  return (
    <article
      className="atm-card-gradient relative overflow-hidden rounded-2xl p-6 text-white shadow-atm min-h-[200px]"
      aria-label="Account balance card"
    >
      {/* Decorative circles */}
      <span className="pointer-events-none absolute -right-10 -top-10 h-40 w-40 rounded-full bg-white/10" />
      <span className="pointer-events-none absolute -bottom-12 -left-8 h-40 w-40 rounded-full bg-white/5" />

      <header className="flex items-start justify-between">
        <div className="flex items-center gap-2">
          <CreditCard className="h-5 w-5 text-accent-300" />
          <span className="text-xs font-medium uppercase tracking-wider text-white/80">
            {account?.accountType ?? 'Savings'} Account
          </span>
        </div>
        <button
          type="button"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? 'Hide balance' : 'Show balance'}
          className="rounded-lg p-1 text-white/80 hover:bg-white/10 hover:text-white"
        >
          {visible ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
        </button>
      </header>

      <div className="mt-4">
        <p className="text-xs uppercase tracking-wider text-white/70">
          Available Balance
        </p>
        <p className="mt-1 text-4xl font-bold tracking-tight">
          {visible ? formatCurrency(account?.availableBalance ?? account?.balance) : '₹ ••••••'}
        </p>
      </div>

      <footer className="mt-6 flex items-end justify-between text-sm">
        <div>
          <p className="text-white/70 text-[10px] uppercase tracking-wider">
            Account No.
          </p>
          <p className="font-mono text-base tracking-wide">
            {maskAccountNumber(account?.accountNumber)}
          </p>
        </div>
        {card && (
          <div className="text-right">
            <p className="text-white/70 text-[10px] uppercase tracking-wider">
              Card
            </p>
            <p className="font-mono text-sm tracking-wide">
              {maskCardNumber(card.cardNumber)}
            </p>
          </div>
        )}
      </footer>
    </article>
  );
}

import { useQuery } from '@tanstack/react-query';
import { RefreshCw, Wallet, Landmark } from 'lucide-react';
import type { ReactNode } from 'react';
import { accountService } from '@/services/account.service';
import { useAuthStore } from '@/store/authStore';
import { formatCurrency, formatDateTime, maskAccountNumber } from '@/utils/format';
import Card from '@/components/ui/Card';
import Loader from '@/components/ui/Loader';
import Button from '@/components/ui/Button';

/**
 * Balance page — shows current balance, available balance, last updated, and
 * basic account metadata (type, bank, branch, IFSC). Click "refresh" to force
 * a refetch.
 */
export default function Balance() {
  const card = useAuthStore((s) => s.card);
  const { data: account, isLoading, isFetching, refetch } = useQuery({
    queryKey: ['accounts', 'card'],
    queryFn: () => accountService.getAccountByCard(),
    staleTime: 30_000,
  });

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-white">Account Balance</h1>
        <p className="text-sm text-white/70">Real-time available balance for your linked account.</p>
      </header>

      {isLoading ? (
        <div className="glass-card p-8 flex items-center justify-center">
          <Loader label="Fetching balance…" />
        </div>
      ) : account ? (
        <div className="grid gap-6 md:grid-cols-2">
          {/* Hero */}
          <Card padded>
            <div className="flex items-center gap-3">
              <div className="grid h-12 w-12 place-items-center rounded-xl bg-primary-900 text-white">
                <Wallet className="h-6 w-6" />
              </div>
              <div>
                <p className="text-sm text-slate-500">Current Balance</p>
                <p className="text-3xl font-bold text-slate-900">
                  {formatCurrency(account.balance)}
                </p>
              </div>
            </div>

            <dl className="mt-6 space-y-3 text-sm">
              <Row label="Available balance" value={formatCurrency(account.availableBalance ?? account.balance)} />
              <Row label="Account number" value={maskAccountNumber(account.accountNumber)} mono />
              <Row label="Currency" value={account.currency} />
              <Row
                label="Last updated"
                value={
                  isFetching
                    ? 'Updating…'
                    : formatDateTime(new Date().toISOString())
                }
              />
            </dl>

            <div className="mt-6">
              <Button
                variant="secondary"
                onClick={() => refetch()}
                loading={isFetching}
                leftIcon={<RefreshCw className="h-4 w-4" />}
              >
                Refresh
              </Button>
            </div>
          </Card>

          {/* Account metadata */}
          <Card title="Account details" padded>
            <dl className="space-y-3 text-sm">
              <Row label="Account type" value={account.accountType} />
              <Row label="Bank" value={account.bankName} icon={<Landmark className="h-4 w-4" />} />
              <Row label="Branch" value={account.branchName ?? '—'} />
              <Row label="IFSC code" value={account.ifscCode ?? '—'} mono />
              <Row label="Status" value={<StatusBadge status={account.status} />} />
              {card && (
                <Row
                  label="Linked card"
                  value={`${card.cardHolderName} · ${card.type}`}
                />
              )}
            </dl>
          </Card>
        </div>
      ) : (
        <div className="glass-card p-8 text-center text-sm text-slate-500">
          No account linked to your card.
        </div>
      )}
    </div>
  );
}

function Row({
  label,
  value,
  mono = false,
  icon,
}: {
  label: string;
  value: ReactNode;
  mono?: boolean;
  icon?: ReactNode;
}) {
  return (
    <div className="flex items-center justify-between gap-4 border-b border-slate-100 pb-2 last:border-0">
      <dt className="flex items-center gap-2 text-slate-500">
        {icon}
        {label}
      </dt>
      <dd className={`font-semibold text-slate-900 ${mono ? 'font-mono text-sm' : ''}`}>
        {value}
      </dd>
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const tone =
    status === 'ACTIVE'
      ? 'bg-success-100 text-success-700'
      : status === 'BLOCKED'
        ? 'bg-danger-100 text-danger-700'
        : 'bg-slate-100 text-slate-700';
  return <span className={`badge ${tone}`}>{status}</span>;
}

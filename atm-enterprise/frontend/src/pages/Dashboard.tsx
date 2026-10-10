import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import {
  ArrowDownToLine,
  ArrowUpFromLine,
  Send,
  Wallet,
  Receipt,
  ChevronRight,
} from 'lucide-react';
import { accountService } from '@/services/account.service';
import { transactionService } from '@/services/transaction.service';
import { useAuthStore } from '@/store/authStore';
import { useTransactions } from '@/hooks/useTransactions';
import { formatCurrency, formatDate } from '@/utils/format';
import BalanceCard from '@/components/ui/BalanceCard';
import Card from '@/components/ui/Card';
import Loader from '@/components/ui/Loader';
import { TransactionType } from '@/types';

interface QuickAction {
  to: string;
  label: string;
  description: string;
  icon: typeof ArrowDownToLine;
  accent: string;
}

const quickActions: QuickAction[] = [
  {
    to: '/withdraw',
    label: 'Withdraw',
    description: 'Get cash from your account',
    icon: ArrowDownToLine,
    accent: 'bg-danger-100 text-danger-700',
  },
  {
    to: '/deposit',
    label: 'Deposit',
    description: 'Add cash or cheque',
    icon: ArrowUpFromLine,
    accent: 'bg-success-100 text-success-700',
  },
  {
    to: '/transfer',
    label: 'Transfer',
    description: 'Send money to anyone',
    icon: Send,
    accent: 'bg-primary-100 text-primary-700',
  },
  {
    to: '/balance',
    label: 'Balance',
    description: 'Check current balance',
    icon: Wallet,
    accent: 'bg-accent-100 text-accent-700',
  },
];

/**
 * Customer dashboard.
 *
 * Pulls the linked account and the last week of transactions, renders an
 * ATM-card hero, four quick-action tiles, a 7-day debit bar chart, and a
 * preview of the 5 most recent transactions.
 */
export default function Dashboard() {
  const card = useAuthStore((s) => s.card);
  const user = useAuthStore((s) => s.user);

  const { data: account, isLoading: accountLoading } = useQuery({
    queryKey: ['accounts', 'card'],
    queryFn: () => accountService.getAccountByCard(),
    staleTime: 30_000,
  });

  // Recent 5 transactions (page 0, size 5).
  const txQuery = useTransactions(account?.id, 0, 5);

  // Last-7-days debit chart.
  const chartData = useChartData(account?.id);

  return (
    <div className="space-y-8">
      {/* Hero */}
      <section>
        <header className="mb-4 flex items-end justify-between">
          <div>
            <h1 className="text-2xl font-bold text-white">
              Hi {user?.firstName ?? 'there'},
            </h1>
            <p className="text-sm text-white/70">Here's your account summary</p>
          </div>
          <Link to="/balance" className="text-sm text-white/80 hover:text-white underline-offset-4 hover:underline">
            View details →
          </Link>
        </header>
        {accountLoading ? (
          <div className="glass-card h-[200px] animate-pulse" />
        ) : (
          <BalanceCard account={account ?? null} card={card} />
        )}
      </section>

      {/* Quick actions */}
      <section className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        {quickActions.map((q) => {
          const Icon = q.icon;
          return (
            <Link
              key={q.to}
              to={q.to}
              className="glass-card p-4 hover:shadow-atm transition-all duration-200 group"
            >
              <div className={`grid h-10 w-10 place-items-center rounded-xl ${q.accent}`}>
                <Icon className="h-5 w-5" />
              </div>
              <p className="mt-3 text-sm font-semibold text-slate-900">{q.label}</p>
              <p className="text-xs text-slate-500">{q.description}</p>
            </Link>
          );
        })}
      </section>

      {/* Chart + recent transactions */}
      <section className="grid gap-6 lg:grid-cols-3">
        <Card title="Last 7 days" description="Daily debit total" className="lg:col-span-2">
          {chartData.isLoading ? (
            <Loader label="Crunching numbers…" className="h-56 items-center justify-center" />
          ) : (
            <div className="h-56">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData.data} margin={{ top: 10, right: 8, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" vertical={false} />
                  <XAxis
                    dataKey="label"
                    tick={{ fill: '#64748b', fontSize: 12 }}
                    tickLine={false}
                    axisLine={false}
                  />
                  <YAxis
                    tick={{ fill: '#64748b', fontSize: 12 }}
                    tickLine={false}
                    axisLine={false}
                    tickFormatter={(v) => `₹${v}`}
                    width={60}
                  />
                  <Tooltip
                    formatter={(value: number) => formatCurrency(value)}
                    labelFormatter={(label) => `Date: ${label}`}
                    contentStyle={{ borderRadius: 12, border: '1px solid #e2e8f0' }}
                  />
                  <Bar dataKey="amount" fill="#1e3a8a" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </Card>

        <Card
          title="Recent activity"
          actions={
            <Link to="/transactions" className="text-xs text-primary-700 hover:underline">
              View all
            </Link>
          }
        >
          {txQuery.isLoading ? (
            <Loader label="Loading…" className="py-6" />
          ) : txQuery.data && txQuery.data.content.length > 0 ? (
            <ul className="space-y-3">
              {txQuery.data.content.map((tx) => (
                <li key={tx.id} className="flex items-center justify-between">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-slate-800">
                      {tx.description ?? tx.type}
                    </p>
                    <p className="text-xs text-slate-500">{formatDate(tx.createdAt)}</p>
                  </div>
                  <p
                    className={`text-sm font-semibold ${
                      tx.direction === 'CR' ? 'text-success-700' : 'text-danger-700'
                    }`}
                  >
                    {tx.direction === 'CR' ? '+' : '−'} {formatCurrency(tx.amount)}
                  </p>
                </li>
              ))}
            </ul>
          ) : (
            <div className="py-6 text-center text-sm text-slate-500">
              No recent transactions.
            </div>
          )}
        </Card>
      </section>

      <Link
        to="/transactions"
        className="flex items-center justify-between glass-card p-4 hover:shadow-atm transition-all"
      >
        <div className="flex items-center gap-3">
          <div className="grid h-10 w-10 place-items-center rounded-xl bg-primary-100 text-primary-700">
            <Receipt className="h-5 w-5" />
          </div>
          <div>
            <p className="text-sm font-semibold text-slate-900">Transaction history</p>
            <p className="text-xs text-slate-500">View, filter and export all your transactions</p>
          </div>
        </div>
        <ChevronRight className="h-5 w-5 text-slate-400" />
      </Link>
    </div>
  );
}

/**
 * Build the last-7-days debit summary chart for an account. We fetch up to
 * 100 recent transactions and bucket debits by day client-side.
 */
function useChartData(accountId: string | undefined) {
  const { data, isLoading } = useTransactions(accountId, 0, 100, {
    type: TransactionType.WITHDRAW,
  });

  const buckets = useMemo(() => {
    const today = new Date();
    const days: { key: string; label: string; amount: number }[] = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date(today);
      d.setDate(d.getDate() - i);
      days.push({
        key: d.toISOString().slice(0, 10),
        label: d.toLocaleDateString('en-IN', { weekday: 'short' }),
        amount: 0,
      });
    }
    const idx = new Map(days.map((d) => [d.key, d]));
    (data?.content ?? []).forEach((tx) => {
      const key = new Date(tx.createdAt).toISOString().slice(0, 10);
      const day = idx.get(key);
      if (day) day.amount += tx.amount;
    });
    return days;
  }, [data]);

  return { data: buckets, isLoading };
}

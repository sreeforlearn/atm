import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Download, Filter } from 'lucide-react';
import { accountService } from '@/services/account.service';
import { useTransactions } from '@/hooks/useTransactions';
import { formatDate, toDateInputValue } from '@/utils/format';
import Card from '@/components/ui/Card';
import Loader from '@/components/ui/Loader';
import Button from '@/components/ui/Button';
import TransactionTable from '@/components/Transaction/TransactionTable';
import { TransactionType, type Transaction } from '@/types';

const PAGE_SIZE = 10;

/** All transaction-type filter options rendered in the dropdown. */
const TYPE_FILTERS: { label: string; value: TransactionType | '' }[] = [
  { label: 'All', value: '' },
  { label: 'Withdraw', value: TransactionType.WITHDRAW },
  { label: 'Deposit', value: TransactionType.DEPOSIT },
  { label: 'Transfer', value: TransactionType.TRANSFER },
  { label: 'Refund', value: TransactionType.REFUND },
];

/**
 * Full transaction history page.
 *
 * - Date range (from / to) and type filter.
 * - Paginated table (uses {@link useTransactions}).
 * - Export current page to CSV.
 */
export default function Transactions() {
  const { data: accounts, isLoading: accountsLoading } = useQuery({
    queryKey: ['accounts'],
    queryFn: () => accountService.getAccounts(),
  });
  const accountId = accounts?.[0]?.id;

  const [page, setPage] = useState(0);
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [type, setType] = useState<TransactionType | ''>('');

  const filters = useMemo(
    () => ({ from: from || undefined, to: to || undefined, type: type || undefined }),
    [from, to, type],
  );

  const txQuery = useTransactions(accountId, page, PAGE_SIZE, filters);

  const handleExportCsv = () => {
    if (!txQuery.data) return;
    const rows = txQuery.data.content;
    exportCsv(rows);
  };

  return (
    <div className="space-y-6">
      <header className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Transactions</h1>
          <p className="text-sm text-white/70">Full statement with filters and CSV export.</p>
        </div>
        <Button
          variant="secondary"
          size="sm"
          leftIcon={<Download className="h-4 w-4" />}
          disabled={!txQuery.data || txQuery.data.content.length === 0}
          onClick={handleExportCsv}
        >
          Export CSV
        </Button>
      </header>

      <Card title="Filters" description="Narrow down by date range or transaction type" padded>
        <form className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4" onSubmit={(e) => e.preventDefault()}>
          <div>
            <label htmlFor="from" className="block text-sm font-medium text-slate-700 mb-1.5">
              From
            </label>
            <input
              id="from"
              type="date"
              value={from}
              max={to || toDateInputValue(new Date())}
              onChange={(e) => {
                setFrom(e.target.value);
                setPage(0);
              }}
              className="form-input"
            />
          </div>
          <div>
            <label htmlFor="to" className="block text-sm font-medium text-slate-700 mb-1.5">
              To
            </label>
            <input
              id="to"
              type="date"
              value={to}
              min={from}
              max={toDateInputValue(new Date())}
              onChange={(e) => {
                setTo(e.target.value);
                setPage(0);
              }}
              className="form-input"
            />
          </div>
          <div>
            <label htmlFor="type" className="block text-sm font-medium text-slate-700 mb-1.5">
              Type
            </label>
            <select
              id="type"
              value={type}
              onChange={(e) => {
                setType(e.target.value as TransactionType | '');
                setPage(0);
              }}
              className="form-input"
            >
              {TYPE_FILTERS.map((t) => (
                <option key={t.label} value={t.value}>
                  {t.label}
                </option>
              ))}
            </select>
          </div>
          <div className="flex items-end">
            <Button
              type="button"
              variant="ghost"
              size="md"
              leftIcon={<Filter className="h-4 w-4" />}
              onClick={() => {
                setFrom('');
                setTo('');
                setType('');
                setPage(0);
              }}
            >
              Reset
            </Button>
          </div>
        </form>
      </Card>

      <Card padded={false} className="p-3">
        {accountsLoading ? (
          <Loader label="Loading accounts…" className="py-10" />
        ) : (
          <TransactionTable
            data={txQuery.data}
            loading={txQuery.isLoading || txQuery.isFetching}
            page={page}
            size={PAGE_SIZE}
            onPageChange={setPage}
          />
        )}
      </Card>
    </div>
  );
}

/**
 * Serialize the current page's transactions to a CSV blob and trigger a
 * browser download. Handles amount formatting and quoting.
 */
function exportCsv(rows: Transaction[]) {
  const header = ['Reference', 'Date', 'Type', 'Direction', 'Amount', 'Remark', 'Status'];
  const lines = rows.map((tx) =>
    [
      tx.referenceNumber,
      new Date(tx.createdAt).toISOString(),
      tx.type,
      tx.direction,
      tx.amount.toFixed(2),
      (tx.remark ?? tx.description ?? '').replace(/"/g, '""'),
      tx.status,
    ]
      .map((v) => `"${String(v)}"`)
      .join(','),
  );
  const csv = [header.join(','), ...lines].join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `transactions-${formatDate(new Date())}.csv`;
  link.click();
  URL.revokeObjectURL(url);
}

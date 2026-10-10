import { ArrowDownLeft, ArrowUpRight, ChevronLeft, ChevronRight } from 'lucide-react';
import type { PaginatedResponse, Transaction } from '@/types';
import { formatCurrency, formatDateTime } from '@/utils/format';
import Loader from '@/components/ui/Loader';

export interface TransactionTableProps {
  data?: PaginatedResponse<Transaction>;
  loading?: boolean;
  page: number;
  size: number;
  onPageChange: (page: number) => void;
  /** Hide pagination controls (e.g. on the dashboard mini-list). */
  paginate?: boolean;
}

const directionBadge = {
  CR: { label: 'CR', className: 'badge-cr' },
  DR: { label: 'DR', className: 'badge-dr' },
} as const;

/**
 * Transaction history table with type/direction badges, amount column and
 * pagination controls. Reused by the dashboard (top 5) and the Transactions page.
 */
export default function TransactionTable({
  data,
  loading,
  page,
  size,
  onPageChange,
  paginate = true,
}: TransactionTableProps) {
  const rows = data?.content ?? [];
  const totalPages = data?.totalPages ?? 1;

  return (
    <div className="overflow-hidden">
      {loading ? (
        <div className="flex items-center justify-center py-12">
          <Loader label="Loading transactions…" />
        </div>
      ) : rows.length === 0 ? (
        <div className="py-12 text-center text-sm text-slate-500">
          No transactions to display.
        </div>
      ) : (
        <div className="-mx-2 overflow-x-auto">
          <table className="w-full min-w-[600px] text-sm">
            <thead>
              <tr className="text-left text-xs uppercase tracking-wider text-slate-500">
                <th className="px-3 py-3 font-medium">Date</th>
                <th className="px-3 py-3 font-medium">Reference</th>
                <th className="px-3 py-3 font-medium">Type</th>
                <th className="px-3 py-3 font-medium">Direction</th>
                <th className="px-3 py-3 font-medium text-right">Amount</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {rows.map((tx) => {
                const isCredit = tx.direction === 'CR';
                const badge = directionBadge[tx.direction];
                return (
                  <tr key={tx.id} className="hover:bg-slate-50/70">
                    <td className="px-3 py-3 text-slate-600 whitespace-nowrap">
                      {formatDateTime(tx.createdAt)}
                    </td>
                    <td className="px-3 py-3 font-mono text-xs text-slate-700">
                      {tx.referenceNumber}
                    </td>
                    <td className="px-3 py-3 text-slate-700">
                      <span className="badge bg-slate-100 text-slate-700">
                        {tx.type}
                      </span>
                    </td>
                    <td className="px-3 py-3">
                      <span className={badge.className}>
                        <span className="mr-1 inline-flex">
                          {isCredit ? (
                            <ArrowDownLeft className="h-3 w-3" />
                          ) : (
                            <ArrowUpRight className="h-3 w-3" />
                          )}
                        </span>
                        {badge.label}
                      </span>
                    </td>
                    <td
                      className={`px-3 py-3 text-right font-semibold ${
                        isCredit ? 'text-success-700' : 'text-danger-700'
                      }`}
                    >
                      {isCredit ? '+' : '−'} {formatCurrency(tx.amount)}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {paginate && rows.length > 0 && (
        <nav className="mt-4 flex items-center justify-between text-sm" aria-label="Pagination">
          <p className="text-slate-500">
            Page <span className="font-semibold text-slate-700">{page + 1}</span> of{' '}
            <span className="font-semibold text-slate-700">{totalPages}</span>
            <span className="ml-2 text-slate-400">
              ({data?.totalElements ?? 0} transactions)
            </span>
          </p>
          <div className="flex gap-1">
            <button
              type="button"
              onClick={() => onPageChange(Math.max(0, page - 1))}
              disabled={page === 0}
              className="btn-secondary btn-sm disabled:opacity-50"
              aria-label="Previous page"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <button
              type="button"
              onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))}
              disabled={page >= totalPages - 1}
              className="btn-secondary btn-sm disabled:opacity-50"
              aria-label="Next page"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </nav>
      )}
    </div>
  );
}

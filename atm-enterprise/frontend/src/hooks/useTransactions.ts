import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';
import { transactionService } from '@/services/transaction.service';
import type {
  DepositRequest,
  TransactionType,
  TransferRequest,
  WithdrawRequest,
} from '@/types';

/** React Query key factories keep cache-key strings DRY. */
export const txKeys = {
  all: ['transactions'] as const,
  list: (
    accountId: string,
    page: number,
    size: number,
    filters?: { from?: string; to?: string; type?: TransactionType | '' },
  ) =>
    [
      'transactions',
      'list',
      accountId,
      page,
      size,
      filters?.from ?? '',
      filters?.to ?? '',
      filters?.type ?? '',
    ] as const,
  detail: (id: string) => ['transactions', 'detail', id] as const,
};

/**
 * Paginated transaction history for an account.
 */
export function useTransactions(
  accountId: string | undefined,
  page: number,
  size: number,
  filters?: { from?: string; to?: string; type?: TransactionType | '' },
) {
  return useQuery({
    queryKey: txKeys.list(accountId ?? '', page, size, filters),
    queryFn: () =>
      transactionService.getTransactions(accountId!, page, size, filters),
    enabled: Boolean(accountId),
    keepPreviousData: true,
  });
}

/** Invalidate every list & balance cache for an account. */
function invalidateAccount(queryClient: ReturnType<typeof useQueryClient>, accountId?: string) {
  queryClient.invalidateQueries({ queryKey: ['transactions'] });
  queryClient.invalidateQueries({ queryKey: ['accounts'] });
  if (accountId) {
    queryClient.invalidateQueries({ queryKey: ['accounts', accountId, 'balance'] });
  }
}

/**
 * Withdraw mutation — invalidates balance + transaction list on success.
 */
export function useWithdraw(accountId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationKey: ['transactions', 'withdraw'],
    mutationFn: (payload: WithdrawRequest) => transactionService.withdraw(payload),
    onSuccess: (tx) => {
      toast.success(`₹${tx.amount.toLocaleString('en-IN')} withdrawn. Ref: ${tx.referenceNumber}`);
      invalidateAccount(queryClient, accountId);
    },
  });
}

/**
 * Deposit mutation.
 */
export function useDeposit(accountId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationKey: ['transactions', 'deposit'],
    mutationFn: (payload: DepositRequest) => transactionService.deposit(payload),
    onSuccess: (tx) => {
      toast.success(`₹${tx.amount.toLocaleString('en-IN')} deposited. Ref: ${tx.referenceNumber}`);
      invalidateAccount(queryClient, accountId);
    },
  });
}

/**
 * Transfer mutation.
 */
export function useTransfer(fromAccountId?: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationKey: ['transactions', 'transfer'],
    mutationFn: (payload: TransferRequest) => transactionService.transfer(payload),
    onSuccess: (tx) => {
      toast.success(`₹${tx.amount.toLocaleString('en-IN')} transferred. Ref: ${tx.referenceNumber}`);
      invalidateAccount(queryClient, fromAccountId);
    },
  });
}
